#!/usr/bin/env bash
#
# Update an installed Grinder service from a fresh checkout.
#
#   1. clones the requested branch into a build directory owned by the build account
#   2. runs install-grinder.sh as that account, which builds, installs and restarts the service
#
# Run it as ROOT — the inverse of install-grinder.sh, which refuses root. This script is privileged
# only so that it can drop to an unprivileged account; it never builds anything itself.
#
# WHY A BUILD ACCOUNT, and not the service account: the service account is created by the installer
# as a nologin, non-sudo system account, which is what a service account should be. The installer
# needs neither of those things — it needs a shell and sudo, because it calls sudo about thirty times
# for systemctl, useradd and everything under the install prefix. So `sudo -u grinder -i` cannot work
# twice over: -i runs the account's login shell, which is /usr/sbin/nologin, and the account could not
# sudo even if it had one.
#
# The build account needs, once:
#
#   usermod -aG docker <account>
#   printf '<account> ALL=(ALL) NOPASSWD:ALL\n' >/etc/sudoers.d/<account> && chmod 440 /etc/sudoers.d/<account>
#
# Docker because the installer builds the runtime image and probes the daemon; passwordless sudo
# because there is no TTY here to answer a prompt. Both are checked before anything is cloned.
#
# The service is NOT stopped or started here. install-grinder.sh stops it, records that it was
# running, replaces the jars and starts it again — and its EXIT trap restarts it if the install dies
# halfway. Stopping it first would make that bookkeeping false and defeat the safety net.
#
# The source tree is left behind for debugging and wiped at the start of the next run. It holds a full
# Gradle build, so it is not small; SRC deliberately lives outside PREFIX, because the installer chowns
# PREFIX to root:root and would otherwise take the build tree — and the build account's ~/.gradle
# ownership — with it.
#
# Usage:  sudo ./serverpackcreator-grinder/deploy/update-grinder.sh [options] [-- installer options]
#
#   --branch NAME       branch to deploy (default: develop)
#   --build-user NAME   unprivileged account to build as (default: $BUILD_USER, else spcbuild)
#   --repo URL          clone from somewhere else (default: the canonical Forgejo remote)
#
# Anything after `--` is passed straight to install-grinder.sh. The two worth knowing:
#
#   --skip-image        leave the runtime image alone. It changes far less often than the code, and
#                       skipping it is the difference between a two-minute update and a ten-minute one.
#   --install-unit      also refresh /etc/systemd/system/spc-grinder.service and daemon-reload.
#
# Overridable by environment: PREFIX, SERVICE_USER, SRC — the first two only so that this script's
# safety checks agree with an installer you are already overriding.

set -Eeuo pipefail

BRANCH="develop"
BUILD_USER="${BUILD_USER:-spcbuild}"
REPO_URL="${REPO_URL:-https://git.griefed.de/griefed/serverpackcreator}"
PREFIX="${PREFIX:-/opt/spc-grinder}"
SRC="${SRC:-/opt/spc-grinder-src}"

installer_args=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --branch)     BRANCH="${2:?--branch needs a value}"; shift 2 ;;
        --build-user) BUILD_USER="${2:?--build-user needs a value}"; shift 2 ;;
        --repo)       REPO_URL="${2:?--repo needs a value}"; shift 2 ;;
        --)           shift; installer_args+=("$@"); break ;;
        # The header block, however long it grows — the same trick install-grinder.sh uses, and for the
        # same reason: a fixed line range rots the moment the text above it moves.
        -h|--help)    awk 'NR>1 && /^#/ {sub(/^# ?/,""); print; next} NR>1 {exit}' "$0"; exit 0 ;;
        *)            echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
    esac
done

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31merror: %s\033[0m\n' "$1" >&2; exit 1; }

# `set -E` is what makes this fire inside functions and subshells; without a trap it is an inert flag.
trap 'printf "\033[31mfailed at line %s\033[0m\n" "$LINENO" >&2' ERR

# --- Preflight ------------------------------------------------------------------------------------
# Everything is checked before the first destructive act. `rm -rf $SRC` happens below, and an update
# that fails after it has deleted the previous checkout but before it has a new one is a worse state
# than the one it started in.
step "Checking prerequisites"

[[ "$(id -u)" -eq 0 ]] || die "run this as root — it drops to $BUILD_USER to build"

command -v git >/dev/null || die "git not found on PATH"
command -v sudo >/dev/null || die "sudo not found; this script cannot drop privileges without it"

id -u "$BUILD_USER" >/dev/null 2>&1 || die "no such account: $BUILD_USER (create one, or pass --build-user)"
[[ "$(id -u "$BUILD_USER")" -ne 0 ]] || die "$BUILD_USER is root; install-grinder.sh refuses to run as root"

# This script rm -rf's $SRC. ${SRC:?} alone catches an empty value and nothing else, so the shape is
# checked here: absolute, and at least two components deep, so an SRC of "/" or "/opt" cannot turn the
# cleanup into something catastrophic. Lifted from install-grinder.sh's identical guard on PREFIX.
[[ "$SRC" == /* ]]            || die "SRC must be an absolute path, got '$SRC'"
[[ "$SRC" =~ ^/[^/]+/[^/]+ ]] || die "SRC looks too close to the root to rm -rf: '$SRC'"

# The one that is easy to get wrong and expensive to debug. The installer ends with
# `chown -R root:root $PREFIX`, so a source tree underneath it becomes root-owned — and the *next*
# run's build, as $BUILD_USER, then fails on a tree it cannot write. Refusing is cheaper than the
# half-hour spent reading Gradle permission errors.
case "$SRC/" in
    "$PREFIX"/*) die "SRC ($SRC) is inside PREFIX ($PREFIX); the installer chowns PREFIX to root:root and would take the build tree with it" ;;
esac

# Checked rather than assumed, because both failures surface deep inside the installer as something
# else: without docker it dies on `docker info` reading as a stopped daemon, and without passwordless
# sudo it blocks on a password prompt against a stdin that will never answer.
id -nG "$BUILD_USER" | tr ' ' '\n' | grep -qx docker ||
    die "$BUILD_USER is not in the docker group — the installer builds an image and probes the daemon.
  usermod -aG docker $BUILD_USER"
sudo -u "$BUILD_USER" -H sudo -n true 2>/dev/null ||
    die "$BUILD_USER cannot sudo without a password, and there is no TTY here to answer a prompt.
  printf '$BUILD_USER ALL=(ALL) NOPASSWD:ALL\\n' >/etc/sudoers.d/$BUILD_USER && chmod 440 /etc/sudoers.d/$BUILD_USER"

echo "branch:       $BRANCH"
echo "repository:   $REPO_URL"
echo "build as:     $BUILD_USER"
echo "source in:    $SRC"
echo "install to:   $PREFIX (install-grinder.sh's own default unless PREFIX is overridden)"
[[ ${#installer_args[@]} -eq 0 ]] || echo "installer args: ${installer_args[*]}"

# --- 1. A fresh checkout --------------------------------------------------------------------------
step "1/2  Cloning $BRANCH"

rm -rf "${SRC:?}"
# install -d rather than mkdir + chown: one call, and the ownership is right from the moment the
# directory exists rather than for the window between the two.
install -d -o "$BUILD_USER" -g "$BUILD_USER" "$SRC"

# Cloned AS the build account, so every file in the tree is already owned by the account that has to
# build it. --depth 1 because nothing here reads history; it is a deploy, not a working copy.
sudo -u "$BUILD_USER" -H git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$SRC/repo" ||
    die "could not clone $BRANCH from $REPO_URL — does that branch exist?"

installer="$SRC/repo/serverpackcreator-grinder/deploy/install-grinder.sh"
[[ -x "$installer" ]] || die "no executable installer at $installer"

# --- 2. Build and install -------------------------------------------------------------------------
step "2/2  Running install-grinder.sh as $BUILD_USER"

# -H sets HOME to the build account's, which Gradle needs for ~/.gradle; without it Gradle writes into
# root's home as an unprivileged user and fails on permissions.
#
# `bash -lc`, not `-i`: -i runs the account's login shell, which for a service-style account is
# /usr/sbin/nologin. Naming bash explicitly means sudo never consults the shell field, and -l reads the
# profile that puts JAVA_HOME and the toolchain on PATH.
sudo -u "$BUILD_USER" -H bash -lc \
    "cd '$SRC/repo/serverpackcreator-grinder/deploy' && ./install-grinder.sh ${installer_args[*]:-}"

step "Done"
cat <<NEXT
$BRANCH is installed. The installer restarted the service if it was running; if this was a first
install it will have printed the enable/start commands.

  systemctl status spc-grinder
  journalctl -fu spc-grinder

The checkout is left at $SRC/repo for debugging and is wiped at the start of the next run.
NEXT
