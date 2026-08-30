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
# The build account needs docker group membership, once:
#
#   usermod -aG docker <account>
#
# It does NOT need permanent sudo. If it cannot already sudo without a password, this script grants it
# for the duration of the run and removes the grant on exit, which it can do because it is root
# already. That is not a privilege escalation worth agonising over: the docker group is
# root-equivalent (`docker run -v /:/host` owns the box), so an account that can reach the daemon
# could take root whenever it liked.
#
# WHY A GRANT RATHER THAN A PASSWORD PROMPT: root running `sudo -u <account>` needs no password, but
# the installer's own sudo — the account going back to root — does. A service account created with
# `useradd --system` has no password at all, so that prompt is unanswerable by anyone, with or without
# a TTY. Prompting is available with --no-temp-sudo for an account that does have a password.
#
# ---------------------------------------------------------------------------------------------------
# FIRST INSTALL vs UPDATE, and what --bootstrap is for
#
# Without --bootstrap this is an *update* script and presumes what a first install leaves behind. On a
# host that has never run the grinder it stops at the first thing it cannot satisfy, in this order:
#
#   1. no such account: <build user>   -- and this is the chicken-and-egg. The account is created by
#      install-grinder.sh step 4, which this script cannot reach because it dies first.
#   2. <build user> is not in the docker group -- needs Docker installed and the account added.
#   3. Docker itself -- install-grinder.sh dies on `docker not found`, an unreachable daemon, or a host
#      with no `docker` group at all.
#   4. a build JDK -- ./gradlew needs JAVA_HOME or a java on PATH *for the build account*. The JVM check
#      install-grinder.sh performs is about the SERVICE's java on systemd's PATH, which is a different
#      question; a host with no JDK used to surface as a bare Gradle error.
#   5. the unit -- not installed unless --install-unit is passed, and even then explicitly "not enabled,
#      not started". A first run otherwise leaves binaries and no service.
#
# --bootstrap folds 1, 2 and 5 into this script: it creates the build account when missing, puts it in
# the docker group, passes --install-unit, and enables and starts the service at the end.
#
# It deliberately does NOT install packages. Docker and a JDK must already be there; 3 and 4 are checked
# and named with the command to fix them, but apt/dnf/pacman differ, Docker's own convenience script is a
# separate decision, and silently installing a container runtime is a bigger step than an update script
# should take unattended. On Debian/Ubuntu that one-time step is:
#
#   apt install -y docker.io git openjdk-21-jdk && systemctl enable --now docker
#
# After a bootstrap, plain `sudo ./update-grinder.sh` is the upgrade path and --clear is the fresh start.
# ---------------------------------------------------------------------------------------------------
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
#   --no-temp-sudo      never touch /etc/sudoers.d. The account must then either already have
#                       passwordless sudo, or a password and a TTY to type it at.
#   --bootstrap         prepare a host that has never run the grinder: create the build account if it is
#                       missing, add it to the docker group, install the systemd unit, and enable and
#                       start the service at the end. See the block above for what it does not do.
#   --clear             hand install-grinder.sh --clear, which DELETES the daemon's data directory
#                       before installing: verdicts, crawl cursors, the re-grind queue, kept boot logs
#                       and the loader cache. A fresh start, not an update.
#
# --clear is not the same as the checkout this script always wipes. That is build input and costs a
# clone to replace; the data directory is everything the grind has learned and costs weeks of boots.
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
BUILD_USER="${BUILD_USER:-grinder}"
REPO_URL="${REPO_URL:-https://git.griefed.de/griefed/serverpackcreator}"
temp_sudo=true
bootstrap=false
PREFIX="${PREFIX:-/opt/spc-grinder}"
SRC="${SRC:-/opt/spc-grinder-src}"

installer_args=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --branch)     BRANCH="${2:?--branch needs a value}"; shift 2 ;;
        --build-user) BUILD_USER="${2:?--build-user needs a value}"; shift 2 ;;
        --repo)       REPO_URL="${2:?--repo needs a value}"; shift 2 ;;
        --no-temp-sudo) temp_sudo=false; shift ;;
        --bootstrap)    bootstrap=true; shift ;;
        # Forwarded rather than acted on here: the installer is the half that knows where the data lives,
        # and -- crucially -- the half that has already stopped the service by the time it clears.
        --clear)      installer_args+=("--clear"); shift ;;
        --)           shift; installer_args+=("$@"); break ;;
        # The header block, however long it grows — the same trick install-grinder.sh uses, and for the
        # same reason: a fixed line range rots the moment the text above it moves.
        -h|--help)    awk 'NR>1 && /^#/ {sub(/^# ?/,""); print; next} NR>1 {exit}' "$0"; exit 0 ;;
        *)            echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
    esac
done

# A first install needs the unit on disk before anything can enable it. Added rather than assumed, and
# skipped when the caller already passed it after `--`, so it is never handed over twice.
if [[ "$bootstrap" == true && " ${installer_args[*]-} " != *" --install-unit "* ]]; then
    installer_args+=("--install-unit")
fi

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31merror: %s\033[0m\n' "$1" >&2; exit 1; }

# `set -E` is what makes this fire inside functions and subshells; without a trap it is an inert flag.
trap 'printf "\033[31mfailed at line %s\033[0m\n" "$LINENO" >&2' ERR

# Set the moment the file is created, so the EXIT trap can remove it even if the very next line fails.
TEMP_SUDOERS=""

# Remove the temporary grant, whatever happened. Registered on EXIT rather than on the success path
# because the whole point is that a build which dies at Gradle, or is Ctrl-C'd, does not leave an
# account with permanent passwordless root behind it.
drop_temp_sudo() {
    [[ -n "$TEMP_SUDOERS" && -e "$TEMP_SUDOERS" ]] || return 0
    rm -f "$TEMP_SUDOERS"
    printf 'removed the temporary sudo grant for %s\n' "$BUILD_USER"
}
trap drop_temp_sudo EXIT

# Grant [BUILD_USER] passwordless sudo for this run only.
#
# Validated with `visudo -c` before it counts: a malformed drop-in does not break only this script, it
# breaks sudo for the whole host, including the root shell that would have to repair it. Written 0440
# because sudo refuses to read a group- or world-writable file and says so unhelpfully.
grant_temp_sudo() {
    local dropin="/etc/sudoers.d/99-spc-grinder-update"
    [[ ! -e "$dropin" ]] || die "$dropin already exists — an earlier run may have been killed; remove it by hand"

    TEMP_SUDOERS="$dropin"
    printf '%s ALL=(ALL) NOPASSWD:ALL\n' "$BUILD_USER" >"$dropin"
    chmod 440 "$dropin"

    if command -v visudo >/dev/null; then
        visudo -cf "$dropin" >/dev/null || die "the sudoers drop-in this script wrote is invalid; it has been removed"
    fi

    # Proven rather than assumed: NSS, sudo's own config and a mistyped account name can all make an
    # apparently-correct drop-in do nothing, and the failure would otherwise surface much later as the
    # installer hanging on a prompt.
    sudo -u "$BUILD_USER" -H sudo -n true 2>/dev/null ||
        die "granted $BUILD_USER passwordless sudo via $dropin, but it still cannot sudo"

    echo "granted $BUILD_USER passwordless sudo for this run only ($dropin, removed on exit)"
}

# --- Preflight ------------------------------------------------------------------------------------
# Everything is checked before the first destructive act. `rm -rf $SRC` happens below, and an update
# that fails after it has deleted the previous checkout but before it has a new one is a worse state
# than the one it started in.
step "Checking prerequisites"

[[ "$(id -u)" -eq 0 ]] || die "run this as root — it drops to $BUILD_USER to build"

command -v git >/dev/null || die "git not found on PATH"
command -v sudo >/dev/null || die "sudo not found; this script cannot drop privileges without it"

# Tracked so the docker-group decision below can tell a purpose-made account from one that was already
# here, exactly as install-grinder.sh does for the service account.
created_account=false
if ! id -u "$BUILD_USER" >/dev/null 2>&1; then
    [[ "$bootstrap" == true ]] ||
        die "no such account: $BUILD_USER (create one, pass --build-user, or run with --bootstrap)"
    # Created the way install-grinder.sh creates the service account, including the nologin shell: this
    # script never needs a login shell, because it runs `bash -lc` explicitly rather than `sudo -i`.
    nologin_shell="$(command -v nologin || echo /usr/sbin/nologin)"
    useradd --system --create-home --home-dir "/home/$BUILD_USER" --shell "$nologin_shell" "$BUILD_USER"
    created_account=true
    echo "created $BUILD_USER (system account, home /home/$BUILD_USER, shell $nologin_shell)"
fi
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
# Checked here rather than left to the installer, which only reaches its own docker checks after this
# script has already wiped the checkout and cloned again — a slow way to learn the daemon is down.
command -v docker >/dev/null ||
    die "docker is not installed. --bootstrap does not install packages; on Debian/Ubuntu:
  apt install -y docker.io && systemctl enable --now docker"
docker info >/dev/null 2>&1 ||
    die "the Docker daemon is not reachable. Is it running?
  systemctl enable --now docker"
getent group docker >/dev/null ||
    die "this host has no 'docker' group, so no account could ever reach the socket"

if id -nG "$BUILD_USER" | tr ' ' '\n' | grep -qx docker; then
    echo "$BUILD_USER is in the docker group"
elif [[ "$bootstrap" == true && "$created_account" == true ]]; then
    # Only for an account this script just made. The docker group is root-equivalent -- `docker run
    # -v /:/host` owns the box -- so install-grinder.sh refuses to grant it to an account it did not
    # create, and bootstrapping is not a reason to be less careful than the script it calls.
    usermod -aG docker "$BUILD_USER"
    echo "added $BUILD_USER to the docker group (root-equivalent; it is a purpose-made service account)"
else
    die "$BUILD_USER is not in the docker group, and it already existed before this run.
Adding it there grants root-equivalent privilege -- anyone who can run docker can mount / and own the
host -- so this script will not do that to an account it did not create. Grant it deliberately:
  usermod -aG docker $BUILD_USER"
fi

# The build JDK, which nothing else checks: install-grinder.sh's JVM step asks whether *systemd* will
# find a java for the service, which is a different question and answered much later. Without this, a
# host with no JDK fails inside Gradle with a message about JAVA_HOME and nothing about how it got there.
sudo -u "$BUILD_USER" -H bash -lc 'command -v java >/dev/null || [[ -n "${JAVA_HOME:-}" ]]' 2>/dev/null ||
    die "$BUILD_USER can see no java, and the Gradle build needs one. --bootstrap does not install
packages; on Debian/Ubuntu:
  apt install -y openjdk-21-jdk"
if sudo -u "$BUILD_USER" -H sudo -n true 2>/dev/null; then
    echo "$BUILD_USER can already sudo without a password"
elif [[ "$temp_sudo" == true ]]; then
    grant_temp_sudo
elif [[ -t 0 ]]; then
    # --no-temp-sudo with a terminal: let the installer's own sudo prompt. Only works if the account
    # HAS a password, which a `useradd --system` service account does not — hence the warning rather
    # than a cheerful "you will be prompted".
    step "WARNING: $BUILD_USER has no passwordless sudo"
    cat <<PROMPT
--no-temp-sudo was given, so nothing under /etc/sudoers.d will be touched. The installer's sudo calls
will prompt for ${BUILD_USER}'s password on this terminal.

If $BUILD_USER is a service account created by install-grinder.sh, it has no password — \`useradd
--system\` locks it — and no password can be typed that will work. Drop --no-temp-sudo, or build as an
account that has one.
PROMPT
else
    die "$BUILD_USER cannot sudo without a password, --no-temp-sudo was given, and there is no TTY here
to answer a prompt. Drop --no-temp-sudo to let this script grant it for the run, or grant it yourself:
  printf '$BUILD_USER ALL=(ALL) NOPASSWD:ALL\\n' >/etc/sudoers.d/$BUILD_USER && chmod 440 /etc/sudoers.d/$BUILD_USER"
fi

echo "branch:       $BRANCH"
echo "repository:   $REPO_URL"
echo "build as:     $BUILD_USER$([[ "$bootstrap" == true ]] && echo '  (--bootstrap)')"
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

if [[ "$bootstrap" == true ]]; then
    step "Enabling and starting spc-grinder"
    # `enable --now` rather than `start`: a bootstrapped host should come back up after a reboot, which
    # is the whole point of installing it as a service. Idempotent, so re-running --bootstrap is safe.
    systemctl enable --now spc-grinder
    systemctl --no-pager --lines=0 status spc-grinder || true
fi

step "Done"
cat <<NEXT
$BRANCH is installed. The installer restarted the service if it was running; if this was a first
install it will have printed the enable/start commands.

  systemctl status spc-grinder
  journalctl -fu spc-grinder

The checkout is left at $SRC/repo for debugging and is wiped at the start of the next run.
NEXT

if [[ "$bootstrap" == true ]]; then
    cat <<NEXT
Bootstrapped. Review the unit before trusting the defaults — every variable is listed in it, commented
out, with its default:

  /etc/systemd/system/spc-grinder.service

SPC_GRINDER_HOST is the one to decide first: the report has no authentication and binds loopback.
NEXT
fi
