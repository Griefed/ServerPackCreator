#!/usr/bin/env bash
#
# Build, install and deploy the ServerPackCreator Grinder as a systemd service.
#
# ONE SCRIPT, TWO MODES — AND THE MODE IS YOUR UID, NOT A FLAG
#
#   BUILD MODE  (you are NOT root) — build THIS checkout and install it:
#     1. builds the runtime image the boots run in
#     2. runs :serverpackcreator-grinder:installDist
#     3. installs the distribution to /opt/spc-grinder
#     4. creates the service account, with a home directory and docker group membership
#
#   DEPLOY MODE (you ARE root) — deploy a branch unattended:
#     1. clones the requested branch into a build directory owned by an unprivileged build account
#     2. re-runs THIS script from that clone, as that account, which lands in build mode
#
# There is no --mode flag because there is no choice to make. A Gradle build must not run as root or
# it leaves root-owned files in build/ that your next ordinary build cannot overwrite; and dropping to
# an unprivileged build account requires being root to begin with. The uid therefore determines the
# mode completely, and a flag could only ever agree with it or lie.
#
# Re-running is safe in both modes: it IS the upgrade path. An already-running service is stopped
# before its jars are replaced and started again afterwards, and nothing under the service's home is
# touched unless you ask for --clear.
#
# Usage:
#   ./install-grinder.sh [options]           build this checkout and install it
#   sudo ./install-grinder.sh --bootstrap    first install on a host that has never run the grinder
#   sudo ./install-grinder.sh [options]      update an installed service from a fresh clone
#
# Options accepted in BOTH modes. Deploy mode does not act on these itself — it forwards them to the
# build-mode run inside the clone, which is the half that knows what they mean:
#
#   --install-unit   also copy spc-grinder.service to /etc/systemd/system and daemon-reload.
#                    It is NOT enabled or started for you; the commands are printed at the end.
#                    --bootstrap implies it.
#   --skip-image     leave the runtime image alone. It changes far less often than the code, and
#                    skipping it is the difference between a two-minute update and a ten-minute one.
#   --no-pull        build the image without refreshing its base (for an offline rebuild).
#   --clear          DELETE the service's data directory before installing, so the daemon starts with a
#                    clean slate: verdicts, crawl cursors, the re-grind queue, kept boot logs AND the
#                    loader cache. Everything the grind has learned. The binaries are unaffected; this
#                    is about state, not code. Nothing else in this script destroys data, so it is opt-in
#                    and says exactly what it removed.
#
#                    NOT the same as the checkout deploy mode wipes on every run: that is build input
#                    and costs a clone to replace, while the data directory costs weeks of boots.
#   --grant-docker   allow adding an ALREADY-EXISTING account to the docker group. That group is
#                    root-equivalent — `docker run -v /:/host` owns the box — so granting it to an
#                    account this script did not create is refused unless you ask for it explicitly.
#                    An account the script creates is a purpose-made service account and is added
#                    without this flag.
#
# Options that only mean something in DEPLOY MODE. Passing one as a normal user is refused rather than
# ignored, because an ignored --branch silently installs something other than what you asked for:
#
#   --branch NAME       branch to deploy (default: develop)
#   --build-user NAME   unprivileged account to build as (default: $BUILD_USER, else grinder)
#   --repo URL          clone from somewhere else (default: the canonical Forgejo remote)
#   --no-temp-sudo      never touch /etc/sudoers.d. The account must then either already have
#                       passwordless sudo, or a password and a TTY to type it at.
#   --bootstrap         prepare a host that has never run the grinder: create the build account if it is
#                       missing, add it to the docker group, install the systemd unit, and enable and
#                       start the service at the end. See FIRST INSTALL below for what it does not do.
#
# `--` is accepted and ignored, so the older `sudo ./install-grinder.sh -- --skip-image` spelling still
# works. There is one flag namespace now, so there is nothing left to pass through.
#
# Overridable by environment: PREFIX, SERVICE_USER, SERVICE_HOME, IMAGE (build mode); BUILD_USER,
# REPO_URL, SRC (deploy mode). PREFIX is read by both, so deploy mode's safety checks agree with the
# build-mode run they protect.
#
# ---------------------------------------------------------------------------------------------------
# DEPLOY MODE: WHY A BUILD ACCOUNT, and not the service account
#
# The service account is created by build mode step 4 as a nologin, non-sudo system account, which is
# what a service account should be. The build needs neither of those things — it needs a shell and
# sudo, because it calls sudo about thirty times for systemctl, useradd and everything under the
# install prefix. So `sudo -u grinder -i` cannot work twice over: -i runs the account's login shell,
# which is /usr/sbin/nologin, and the account could not sudo even if it had one.
#
# The build account needs docker group membership, once:
#
#   usermod -aG docker <account>
#
# It does NOT need permanent sudo. If it cannot already sudo without a password, deploy mode grants it
# for the duration of the run and removes the grant on exit, which it can do because it is root
# already. That is not a privilege escalation worth agonising over: the docker group is root-equivalent
# (`docker run -v /:/host` owns the box), so an account that can reach the daemon could take root
# whenever it liked.
#
# WHY A GRANT RATHER THAN A PASSWORD PROMPT: root running `sudo -u <account>` needs no password, but
# the build-mode sudo — the account going back to root — does. A service account created with
# `useradd --system` has no password at all, so that prompt is unanswerable by anyone, with or without
# a TTY. Prompting is available with --no-temp-sudo for an account that does have a password.
#
# ---------------------------------------------------------------------------------------------------
# FIRST INSTALL vs UPDATE, and what --bootstrap is for
#
# Without --bootstrap, deploy mode is an *update* path and presumes what a first install leaves behind.
# On a host that has never run the grinder it stops at the first thing it cannot satisfy, in this order:
#
#   1. no such account: <build user>   -- and this is the chicken-and-egg. The account is created by
#      build mode step 4, which deploy mode cannot reach because it dies first.
#   2. <build user> is not in the docker group -- needs Docker installed and the account added.
#   3. Docker itself -- `docker not found`, an unreachable daemon, or a host with no `docker` group.
#   4. a build JDK -- ./gradlew needs JAVA_HOME or a java on PATH *for the build account*. The JVM check
#      build mode performs is about the SERVICE's java on systemd's PATH, which is a different question;
#      a host with no JDK used to surface as a bare Gradle error.
#   5. the unit -- not installed unless --install-unit is passed, and even then explicitly "not enabled,
#      not started". A first run otherwise leaves binaries and no service.
#
# --bootstrap folds 1, 2 and 5 in: it creates the build account when missing, puts it in the docker
# group, implies --install-unit, and enables and starts the service at the end.
#
# It deliberately does NOT install packages. Docker and a JDK must already be there; 3 and 4 are checked
# and named with the command to fix them, but apt/dnf/pacman differ, Docker's own convenience script is a
# separate decision, and silently installing a container runtime is a bigger step than a deploy script
# should take unattended. On Debian/Ubuntu that one-time step is:
#
#   apt install -y docker.io git openjdk-21-jdk && systemctl enable --now docker
#
# After a bootstrap, plain `sudo ./install-grinder.sh` is the upgrade path and --clear is the fresh start.
#
# ---------------------------------------------------------------------------------------------------
# DEPLOY MODE RUNS THE CLONE'S COPY OF THIS SCRIPT, NOT THIS ONE.
#
# That is the entire point of cloning: the branch being deployed carries its own installer, and running
# the copy you happened to launch would install the branch's jars under the *old* script's logic — an
# installer that does not know about a step the branch's build now needs. The child runs as an
# unprivileged account, so it lands in build mode; there is no recursion to guard against, because only
# root reaches the clone-and-hand-off path at all.
#
# The service is NOT stopped or started by deploy mode. Build mode stops it, records that it was
# running, replaces the jars and starts it again — and the EXIT trap restarts it if the install dies
# halfway. Stopping it first would make that bookkeeping false and defeat the safety net.
#
# The source tree is left behind for debugging and wiped at the start of the next run. It holds a full
# Gradle build, so it is not small; SRC deliberately lives outside PREFIX, because build mode chowns
# PREFIX to root:root and would otherwise take the build tree — and the build account's ~/.gradle
# ownership — with it.

set -Eeuo pipefail

# --- Configuration --------------------------------------------------------------------------------
# Build mode.
PREFIX="${PREFIX:-/opt/spc-grinder}"
SERVICE_USER="${SERVICE_USER:-grinder}"
SERVICE_HOME="${SERVICE_HOME:-/home/grinder}"
IMAGE="${IMAGE:-spc-grinder-runtime:latest}"
UNIT_NAME="spc-grinder.service"

# Deploy mode.
BRANCH="develop"
BUILD_USER="${BUILD_USER:-grinder}"
REPO_URL="${REPO_URL:-https://git.griefed.de/griefed/serverpackcreator}"
SRC="${SRC:-/opt/spc-grinder-src}"

# The mode, decided once and never again. See the header for why this is a uid and not a flag.
if [[ "$(id -u)" -eq 0 ]]; then mode=deploy; else mode=build; fi

install_unit=false
clear_home=false
skip_image=false
no_pull=false
grant_docker=false
temp_sudo=true
bootstrap=false

# Set by build mode once it has stopped a running service; read by the EXIT trap.
was_active=false
# Set by deploy mode the moment the sudoers drop-in exists, so the EXIT trap can remove it even if the
# very next line fails; read by the EXIT trap.
TEMP_SUDOERS=""

# The both-modes flags actually given, to hand to the clone's copy. Accumulated while parsing rather
# than reconstructed afterwards, so a new flag cannot be added in one place and forgotten in the other.
forward_args=()

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31merror: %s\033[0m\n' "$1" >&2; exit 1; }

# Refuse a deploy-mode flag given to a build-mode run. Refusing rather than ignoring, because every one
# of them changes *what* gets installed, and an ignored --branch installs the wrong thing quietly.
deploy_only() {
    [[ "$mode" == deploy ]] ||
        die "$1 only means something when deploying as root; as $(id -un) this script builds the checkout it lives in.
Either drop $1, or run it as root:  sudo $0 $1 ..."
}

# --- Options --------------------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --install-unit) install_unit=true;  forward_args+=("$1"); shift ;;
        --clear)        clear_home=true;    forward_args+=("$1"); shift ;;
        --skip-image)   skip_image=true;    forward_args+=("$1"); shift ;;
        --no-pull)      no_pull=true;       forward_args+=("$1"); shift ;;
        --grant-docker) grant_docker=true;  forward_args+=("$1"); shift ;;
        --branch)       deploy_only "$1"; BRANCH="${2:?--branch needs a value}";         shift 2 ;;
        --build-user)   deploy_only "$1"; BUILD_USER="${2:?--build-user needs a value}"; shift 2 ;;
        --repo)         deploy_only "$1"; REPO_URL="${2:?--repo needs a value}";         shift 2 ;;
        --no-temp-sudo) deploy_only "$1"; temp_sudo=false; shift ;;
        --bootstrap)    deploy_only "$1"; bootstrap=true;  shift ;;
        # Kept as an accepted no-op so the pre-merge spelling still works; there is nothing to pass
        # through any more, because there is only one script and one flag namespace.
        --)             shift ;;
        # The header block, however long it grows. A fixed line range was wrong within one commit of
        # being written — the range is the kind of citation that rots the moment the text above moves.
        -h|--help)      awk 'NR>1 && /^#/ {sub(/^# ?/,""); print; next} NR>1 {exit}' "$0"; exit 0 ;;
        *)              echo "unknown option: $1 (try --help)" >&2; exit 2 ;;
    esac
done

# A first install needs the unit on disk before anything can enable it. Set rather than assumed, and
# only when the caller did not already pass it, so it is never forwarded twice.
if [[ "$bootstrap" == true && "$install_unit" != true ]]; then
    install_unit=true
    forward_args+=("--install-unit")
fi

# --- Traps ----------------------------------------------------------------------------------------
# `set -E` above is what makes this fire inside functions and subshells too; without a trap it was an
# inert flag. Reporting the line matters here because most of what follows is a one-line sudo call.
trap 'printf "\033[31mfailed at line %s\033[0m\n" "$LINENO" >&2' ERR

# An upgrade stops the service before replacing its jars. Without this, any later failure — a full
# disk, a bad sudo, an interrupted copy — exits under `set -e` and leaves the service DOWN, turning a
# transient error into an outage nobody is told about.
restore_service() {
    [[ "${1:-0}" -ne 0 && "$was_active" == true ]] || return 0
    printf '\033[31m\nthe install failed after stopping %s — starting it again\033[0m\n' "$UNIT_NAME" >&2
    sudo systemctl start "$UNIT_NAME" || printf '\033[31mcould not restart it; do so by hand\033[0m\n' >&2
}

# Remove the temporary grant, whatever happened. On EXIT rather than on the success path because the
# whole point is that a build which dies at Gradle, or is Ctrl-C'd, does not leave an account with
# permanent passwordless root behind it.
drop_temp_sudo() {
    [[ -n "$TEMP_SUDOERS" && -e "$TEMP_SUDOERS" ]] || return 0
    rm -f "$TEMP_SUDOERS"
    printf 'removed the temporary sudo grant for %s\n' "$BUILD_USER"
}

# One EXIT handler for both modes. Neither half needs to ask which mode it is in: was_active is only
# ever set by build mode and TEMP_SUDOERS only by deploy mode, so each is already a no-op in the other,
# and a mode branch here would be one more thing to keep in sync. $? is captured first because
# everything below it clobbers the status the handler is supposed to react to.
on_exit() {
    local status=$?
    drop_temp_sudo
    restore_service "$status"
}
trap on_exit EXIT

# --- Shared helpers -------------------------------------------------------------------------------

# Guard a path this script is about to rm -rf. A `${path:?}` expansion catches an empty value and
# nothing else, so what is checked here is the SHAPE: absolute, and at least two components deep, so a
# value of "/", "/usr" or "/opt" cannot turn a cleanup into something catastrophic.
require_deletable() {
    local label="$1" path="$2"
    [[ "$path" == /* ]]            || die "$label must be an absolute path, got '$path'"
    [[ "$path" =~ ^/[^/]+/[^/]+ ]] || die "$label looks too close to the root to rm -rf under: '$path'"
}

# Both modes need a reachable daemon and an existing docker group, and neither can do anything useful
# without them. Checked up front in each because both failures otherwise surface much later as
# something else — in deploy mode, only after the checkout has already been wiped and re-cloned.
require_docker() {
    command -v docker >/dev/null || die "docker is not installed. On Debian/Ubuntu:
  apt install -y docker.io && systemctl enable --now docker"
    docker info >/dev/null 2>&1 || die "cannot reach the Docker daemon — is it running, and is this account in the docker group?
  systemctl enable --now docker"
    getent group docker >/dev/null || die "this host has no 'docker' group, so no account could ever reach the socket"
}

# Create a nologin, passwordless system account: the service account in build mode, the build account
# in deploy mode. Neither ever needs a login shell, because deploy mode runs `bash -lc` explicitly
# rather than `sudo -i`, and the service is started by systemd.
create_system_account() {
    local user="$1" home="$2" nologin_shell
    nologin_shell="$(command -v nologin || echo /usr/sbin/nologin)"
    sudo useradd --system --create-home --home-dir "$home" --shell "$nologin_shell" "$user"
    echo "created $user (system account, home $home, shell $nologin_shell)"
}

# Put an account in the docker group, or refuse to. That group is root-equivalent — `docker run
# -v /:/host` yields the whole filesystem — so granting it to an account this script created is the
# point of that account, while granting it to one that already existed (a human login, say) is a
# privilege escalation the operator has to ask for. [remedy] is the mode's own way of asking.
ensure_docker_group() {
    local user="$1" created="$2" allowed="$3" remedy="$4"
    if id -nG "$user" | tr ' ' '\n' | grep -qx docker; then
        echo "$user is already in the docker group"
    elif [[ "$created" == true || "$allowed" == true ]]; then
        sudo usermod -aG docker "$user"
        echo "added $user to the docker group (root-equivalent; it is a purpose-made service account)"
    else
        die "$user already existed and is not in the docker group.
Adding it there grants root-equivalent privilege — anyone who can run docker can mount / and own the
host — so this script will not do that to an account it did not create. $remedy"
    fi
}

# ====================================================================================================
# BUILD MODE — build this checkout and install it
# ====================================================================================================
run_build_mode() {
    local script_dir repo_root dist_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    repo_root="$(cd "$script_dir/../.." && pwd)"
    dist_dir="$repo_root/serverpackcreator-grinder/build/install/serverpackcreator-grinder"

    # --- Preflight ----------------------------------------------------------------------------
    step "Checking prerequisites"

    [[ -f "$repo_root/settings.gradle.kts" ]] || die "not a ServerPackCreator checkout: $repo_root"

    # This mode rm -rf's $PREFIX/lib, and --clear rm -rf's the data directory.
    require_deletable PREFIX "$PREFIX"

    require_docker
    command -v sudo >/dev/null || die "sudo not found; the privileged steps cannot run"

    # Checked here, before anything is built or changed: PREFIX, SERVICE_USER and SERVICE_HOME are
    # overridable while the shipped unit hardcodes all three, and an install that disagrees with the unit
    # only fails later, at `systemctl start`, with nothing pointing back at the override.
    #
    # **Which copy of the unit is authoritative is not obvious, and reading the wrong one is a real bug we
    # shipped.** Every knob in the shipped unit is commented out, JAVA_HOME included, and the operator
    # uncomments what they need in the INSTALLED copy under /etc/systemd/system — that is the one systemd
    # executes. Reading the shipped copy therefore reports the defaults rather than the configuration, and
    # deploy mode makes it worse: it `rm -rf`s its checkout and re-clones every run, so the shipped copy is
    # pristine every single time and the operator's edits are invisible by construction. The consequence
    # was not cosmetic — a host whose java comes from JAVA_HOME in the installed unit, and not from
    # systemd's bare PATH, resolved no JVM here, which printed a "the service will not find a JVM" warning
    # at a service that starts fine.
    #
    # So: the shipped copy when we are about to install it (it is what will be in effect), otherwise the
    # installed copy if there is one, otherwise the shipped copy as a preview of a first install.
    local installed_unit unit_file unit_mismatch unit_group
    installed_unit="/etc/systemd/system/$UNIT_NAME"
    if [[ "$install_unit" == true || ! -f "$installed_unit" ]]; then
        unit_file="$script_dir/$UNIT_NAME"
    else
        unit_file="$installed_unit"
    fi
    unit_value() { sed -n "s/^$1=//p" "$unit_file" | head -1; }
    unit_mismatch=false
    if [[ -f "$unit_file" ]]; then
        [[ "$(unit_value User)" == "$SERVICE_USER" ]] ||
            { echo "  unit has User=$(unit_value User), installing for $SERVICE_USER"; unit_mismatch=true; }
        [[ "$(unit_value WorkingDirectory)" == "$SERVICE_HOME" ]] ||
            { echo "  unit has WorkingDirectory=$(unit_value WorkingDirectory), home is $SERVICE_HOME"; unit_mismatch=true; }
        [[ "$(unit_value ExecStart)" == "$PREFIX/bin/serverpackcreator-grinder" ]] ||
            { echo "  unit has ExecStart=$(unit_value ExecStart), installed to $PREFIX"; unit_mismatch=true; }
        if [[ "$unit_mismatch" == true ]]; then
            # Refuse rather than warn when we would be installing it: a unit that cannot start is not
            # something to hand systemd on the strength of a message that scrolled past.
            [[ "$install_unit" != true ]] ||
                die "--install-unit would install a unit that does not match this install (see above); edit $unit_file first"
            echo "  (not installing the unit, so this is yours to reconcile before starting the service)"
        fi
    fi

    # The daemon's data directory, resolved the same way the daemon resolves it: SPC_GRINDER_HOME if the
    # operator sets one, else `$HOME/.spc-grinder` (GrinderConfiguration.defaultHome). Resolved even when
    # --clear was not given, so the line below always tells the operator where the state lives.
    local service_data
    service_data="${SPC_GRINDER_HOME:-$SERVICE_HOME/.spc-grinder}"

    # Guarded on the SHAPE, exactly as PREFIX is above, because --clear rm -rf's this. Absolute and at
    # least two components deep, and never the account's whole home or the install prefix -- a
    # SPC_GRINDER_HOME of `/home/grinder` would otherwise take the account's dotfiles and its ~/.gradle
    # with it, and one of `/opt/spc-grinder` would delete the binaries this script just installed.
    if [[ "$clear_home" == true ]]; then
        require_deletable SPC_GRINDER_HOME "$service_data"
        [[ "$service_data" != "$SERVICE_HOME" ]] || die "--clear would delete the whole home of $SERVICE_USER ($SERVICE_HOME), not just its data"
        [[ "$service_data" != "$PREFIX" ]]       || die "--clear would delete the install prefix ($PREFIX)"
    fi

    echo "mode:          build (running as $(id -un))"
    echo "repository:    $repo_root"
    echo "unit read:     $unit_file$([[ "$unit_file" == "$installed_unit" ]] && echo '  (installed — the one systemd runs)' || echo '  (shipped copy)')"
    echo "install to:    $PREFIX"
    echo "service user:  $SERVICE_USER (home $SERVICE_HOME)"
    echo "service data:  $service_data$([[ "$clear_home" == true ]] && echo '  ** WILL BE DELETED (--clear) **')"
    echo "runtime image: $IMAGE"

    # Ask once up front rather than surprising you four steps in. The timestamp only lasts ~15 minutes,
    # which a cold image build plus a Gradle build can outlive, so it is refreshed again before the
    # privileged steps rather than assumed to still be valid.
    sudo -v

    # --- 1. The runtime image -----------------------------------------------------------------
    if [[ "$skip_image" == true ]]; then
        step "Skipping the runtime image (--skip-image)"
        docker image inspect "$IMAGE" >/dev/null 2>&1 || die "$IMAGE does not exist, so it cannot be skipped"
    else
        step "1/4  Building the runtime image: $IMAGE"
        # --pull by default: without it a cached base silently survives a rebuild, so "rebuild the image
        # for a newer JDK" can quietly do nothing. --no-pull is the escape for an offline rebuild.
        local pull_flag=(--pull)
        [[ "$no_pull" != true ]] || pull_flag=()
        docker build "${pull_flag[@]}" -t "$IMAGE" "$repo_root/serverpackcreator-grinder/docker"
    fi

    # --- 2. The distribution ------------------------------------------------------------------
    step "2/4  Building the distribution (installDist)"
    ( cd "$repo_root" && ./gradlew :serverpackcreator-grinder:installDist )
    [[ -x "$dist_dir/bin/serverpackcreator-grinder" ]] || die "installDist produced no launcher at $dist_dir/bin/"

    # --- 3. Install it ------------------------------------------------------------------------
    step "3/4  Installing to $PREFIX"

    sudo -v

    # Stop first if it is running: replacing jars under a live JVM is how you get a class-loading failure
    # hours later, on a lazily-loaded class, with nothing in the log to connect it to this script.
    if command -v systemctl >/dev/null && systemctl is-active --quiet "$UNIT_NAME" 2>/dev/null; then
        was_active=true
        echo "stopping $UNIT_NAME (it will be started again at the end)"
        sudo systemctl stop "$UNIT_NAME"
    fi

    sudo mkdir -p "$PREFIX"

    # lib/ is replaced wholesale rather than merged. The launcher pins an explicit jar list, so a stale
    # jar is never *loaded* — but a version bump renames one, and merging would silently accumulate every
    # version ever installed.
    if [[ -d "$PREFIX/lib" ]]; then
        echo "replacing $PREFIX/lib ($(find "$PREFIX/lib" -name '*.jar' | wc -l | tr -d ' ') jars currently installed)"
        sudo rm -rf "${PREFIX:?}/lib"
    fi

    sudo cp -a "$dist_dir/bin" "$dist_dir/lib" "$PREFIX/"

    # Root-owned and world-readable on purpose: the service reads its own binaries and has no business
    # writing them. Everything it *does* write lives under its home.
    sudo chown -R root:root "$PREFIX"
    # go-w before a+rX: cp -a preserved the build tree's modes, which came from the operator's umask, and
    # a permissive one (002, 000) would leave the service's own binaries group- or world-writable. Adding
    # read cannot undo that; removing write can.
    sudo chmod -R go-w "$PREFIX"
    sudo chmod -R a+rX "$PREFIX"
    echo "installed $(find "$PREFIX/lib" -name '*.jar' | wc -l | tr -d ' ') jars; launcher at $PREFIX/bin/serverpackcreator-grinder"

    # --- 4. The service account ---------------------------------------------------------------
    step "4/4  Service account: $SERVICE_USER"

    local created_account=false
    if id -u "$SERVICE_USER" >/dev/null 2>&1; then
        echo "user $SERVICE_USER already exists — not modifying the account itself"
    else
        create_system_account "$SERVICE_USER" "$SERVICE_HOME"
        created_account=true
    fi

    # --create-home is a no-op for an account that already existed without one.
    if [[ ! -d "$SERVICE_HOME" ]]; then
        sudo mkdir -p "$SERVICE_HOME"
        sudo chown "$SERVICE_USER" "$SERVICE_HOME"
        echo "created missing home $SERVICE_HOME"
    fi

    # The unit says Group=<user>, and that group is NOT guaranteed to exist: with USERGROUPS_ENAB no in
    # /etc/login.defs — supported, and the default on some hardened images — useradd puts the account on a
    # shared group instead (observed: gid=100(users)) and creates nothing. systemd then fails to start the
    # unit on a group that does not resolve, which reads as a permissions problem and is not one.
    unit_group="$(unit_value Group)"
    unit_group="${unit_group:-$SERVICE_USER}"
    if getent group "$unit_group" >/dev/null; then
        echo "group $unit_group exists"
    else
        sudo groupadd --system "$unit_group"
        sudo usermod -aG "$unit_group" "$SERVICE_USER"
        echo "created group $unit_group and added $SERVICE_USER to it (the unit names it as Group=)"
    fi

    ensure_docker_group "$SERVICE_USER" "$created_account" "$grant_docker" \
        "Either use a dedicated account, or re-run with --grant-docker if $SERVICE_USER really is meant to be one."

    # --- Optional: a clean slate --------------------------------------------------------------
    # Deliberately here, AFTER the service was stopped in step 3, and the ordering is load-bearing: the
    # daemon coalesces verdict writes and flushes the store on shutdown, so clearing a *running* service
    # just gets it written back out of memory as the service stops. It is also after the account exists, so
    # the directory it names is the one the daemon will actually use.
    if [[ "$clear_home" == true ]]; then
        step "Clearing $service_data (--clear)"

        if [[ -d "$service_data" ]]; then
            local size artefact
            size="$(sudo du -sh "$service_data" 2>/dev/null | cut -f1 || echo '?')"
            echo "removing $size of accumulated state:"
            # Named individually rather than as one line, because "the grinder's data" is four different
            # kinds of loss and the operator should see which ones they are asking for. `if` rather than
            # `[[ ]] &&` so a missing last entry does not fail the loop under `set -e`.
            for artefact in verdicts.json cursors.json requeue.json boot-logs crash-logs cache work; do
                if [[ -e "$service_data/$artefact" ]]; then
                    echo "  - $artefact"
                fi
            done
            sudo rm -rf "${service_data:?}"
            echo "cleared — the daemon starts with no verdicts, at the head of the crawl, and re-downloads"
            echo "every loader install it needs (~150 MB per loader/Minecraft tuple, the expensive part)."
        else
            echo "$service_data does not exist — nothing to clear"
        fi
    fi

    # --- The JVM the service will actually see ------------------------------------------------
    # The launcher needs JAVA_HOME or a `java` on PATH. The operator has both — they just ran Gradle — and
    # the service has neither unless the host provides them, which is why this is checked against systemd's
    # PATH rather than the caller's. `env -i` drops the caller's environment entirely, so the answer is the
    # service's, not a flattering version of it.
    step "Checking the JVM the service will see"

    local systemd_path unit_java_home
    systemd_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    # Not via unit_value: that keeps the FIRST `Environment=` line, and the unit has several — the
    # JAVA_HOME one is not necessarily it. Read from $unit_file, which is the copy resolved in the
    # preflight as the one actually in effect, not the pristine shipped one.
    unit_java_home="$(sed -n 's/^Environment=JAVA_HOME=//p' "$unit_file" | head -1)"

    if [[ -n "$unit_java_home" ]]; then
        if [[ -x "$unit_java_home/bin/java" ]]; then
            echo "unit sets JAVA_HOME=$unit_java_home, and $unit_java_home/bin/java is executable"
        else
            die "the unit sets JAVA_HOME=$unit_java_home but $unit_java_home/bin/java is not executable"
        fi
    elif env -i PATH="$systemd_path" sh -c 'command -v java' >/dev/null 2>&1; then
        echo "java found on systemd's PATH at $(env -i PATH="$systemd_path" sh -c 'command -v java')"
    else
        step "WARNING: the service will not find a JVM"
        cat <<'JVM'
The launcher needs JAVA_HOME or a `java` on PATH, and there is no java on the PATH systemd gives a unit:

  /usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

Your own java does not count — you have it from a profile the service never reads. Uncomment and set
JAVA_HOME in the unit, or the first `systemctl start` fails with:

  ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
JVM
    fi

    # --- Optional: the unit -------------------------------------------------------------------
    if [[ "$install_unit" == true ]]; then
        step "Installing $UNIT_NAME"
        command -v systemctl >/dev/null || die "--install-unit given, but this host has no systemctl"
        sudo install -m 644 "$script_dir/$UNIT_NAME" "/etc/systemd/system/$UNIT_NAME"
        sudo systemctl daemon-reload
        echo "installed /etc/systemd/system/$UNIT_NAME (not enabled, not started)"
    fi

    if [[ "$was_active" == true ]]; then
        step "Restarting $UNIT_NAME"
        sudo systemctl start "$UNIT_NAME"
    fi

    # --- What is left for you -----------------------------------------------------------------
    step "Done"

    # Point at the INSTALLED unit once there is one. Editing the checkout's copy is a trap: it is not what
    # systemd reads, and deploy mode rm -rf's its checkout at the start of every run, so an operator who
    # configures there loses the configuration on the next update and cannot see why.
    local edit_unit edit_note
    if [[ -f "$installed_unit" ]]; then
        edit_unit="$installed_unit"
        edit_note="(the installed copy — the one systemd reads; a daemon-reload + restart applies an edit)"
    else
        edit_unit="$script_dir/$UNIT_NAME"
        edit_note="(not installed yet — install it first, then edit /etc/systemd/system/$UNIT_NAME, NOT this checkout)"
    fi

    cat <<NEXT
Review the unit's configuration before the first start — every variable is listed in it, commented
out, with its default:

  $edit_unit
  $edit_note

Four worth a decision rather than a default:
  SPC_GRINDER_WORKERS  budget 3 GiB of Docker-available memory each; the default of 2 is conservative
  SPC_GRINDER_CPUS     cores per container, so workers x cpus is what the boots can occupy (4 by
                       default). A unit-level CPUQuota= cannot reach them -- containers belong to the
                       Docker daemon's control group, not this service's
  SPC_GRINDER_HOST     loopback unless a reverse proxy needs it; the report has NO authentication
  SPC_GRINDER_CONTAINER_USER  defaults to the owner of the work directory, which is almost always right;
                       a wrong value makes every install fail with Permission denied inside the pack

NEXT

    if [[ "$install_unit" != true ]]; then
        cat <<NEXT
Then install and start it:

  sudo install -m 644 $script_dir/$UNIT_NAME /etc/systemd/system/$UNIT_NAME
  sudo systemctl daemon-reload
  sudo systemctl enable --now spc-grinder
  journalctl -fu spc-grinder
NEXT
    elif [[ "$was_active" != true ]]; then
        cat <<NEXT
Then start it:

  sudo systemctl enable --now spc-grinder
  journalctl -fu spc-grinder
NEXT
    fi
}

# ====================================================================================================
# DEPLOY MODE — clone a branch and hand off to build mode inside it
# ====================================================================================================

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
    # build hanging on a prompt.
    sudo -u "$BUILD_USER" -H sudo -n true 2>/dev/null ||
        die "granted $BUILD_USER passwordless sudo via $dropin, but it still cannot sudo"

    echo "granted $BUILD_USER passwordless sudo for this run only ($dropin, removed on exit)"
}

run_deploy_mode() {
    # --- Preflight ----------------------------------------------------------------------------
    # Everything is checked before the first destructive act. `rm -rf $SRC` happens below, and an update
    # that fails after it has deleted the previous checkout but before it has a new one is a worse state
    # than the one it started in.
    step "Checking prerequisites"

    command -v git  >/dev/null || die "git not found on PATH"
    command -v sudo >/dev/null || die "sudo not found; this script cannot drop privileges without it"

    # Tracked so the docker-group decision below can tell a purpose-made account from one that was already
    # here, exactly as build mode does for the service account.
    local created_account=false
    if ! id -u "$BUILD_USER" >/dev/null 2>&1; then
        [[ "$bootstrap" == true ]] ||
            die "no such account: $BUILD_USER (create one, pass --build-user, or run with --bootstrap)"
        create_system_account "$BUILD_USER" "/home/$BUILD_USER"
        created_account=true
    fi
    [[ "$(id -u "$BUILD_USER")" -ne 0 ]] || die "$BUILD_USER is root, and the build half refuses to run as root"

    require_deletable SRC "$SRC"

    # The one that is easy to get wrong and expensive to debug. Build mode ends with
    # `chown -R root:root $PREFIX`, so a source tree underneath it becomes root-owned — and the *next*
    # run's build, as $BUILD_USER, then fails on a tree it cannot write. Refusing is cheaper than the
    # half-hour spent reading Gradle permission errors.
    case "$SRC/" in
        "$PREFIX"/*) die "SRC ($SRC) is inside PREFIX ($PREFIX); the install chowns PREFIX to root:root and would take the build tree with it" ;;
    esac

    # Checked here rather than left to build mode, which only reaches its own docker checks after this
    # half has already wiped the checkout and cloned again — a slow way to learn the daemon is down.
    require_docker

    # Only for an account this script just made, and --bootstrap is not a reason to be less careful:
    # build mode refuses the same grant to an account it did not create, so passing `false` here keeps
    # the two halves aligned rather than making --bootstrap the loophole.
    ensure_docker_group "$BUILD_USER" "$created_account" false \
        "Grant it deliberately:
  usermod -aG docker $BUILD_USER"

    # The build JDK, which nothing else checks: build mode's JVM step asks whether *systemd* will find a
    # java for the service, which is a different question and answered much later. Without this, a host
    # with no JDK fails inside Gradle with a message about JAVA_HOME and nothing about how it got there.
    sudo -u "$BUILD_USER" -H bash -lc 'command -v java >/dev/null || [[ -n "${JAVA_HOME:-}" ]]' 2>/dev/null ||
        die "$BUILD_USER can see no java, and the Gradle build needs one. --bootstrap does not install
packages; on Debian/Ubuntu:
  apt install -y openjdk-21-jdk"

    if sudo -u "$BUILD_USER" -H sudo -n true 2>/dev/null; then
        echo "$BUILD_USER can already sudo without a password"
    elif [[ "$temp_sudo" == true ]]; then
        grant_temp_sudo
    elif [[ -t 0 ]]; then
        # --no-temp-sudo with a terminal: let the build half's own sudo prompt. Only works if the account
        # HAS a password, which a `useradd --system` service account does not — hence the warning rather
        # than a cheerful "you will be prompted".
        step "WARNING: $BUILD_USER has no passwordless sudo"
        cat <<PROMPT
--no-temp-sudo was given, so nothing under /etc/sudoers.d will be touched. The build's sudo calls will
prompt for ${BUILD_USER}'s password on this terminal.

If $BUILD_USER is a service account this script created, it has no password — \`useradd --system\`
locks it — and no password can be typed that will work. Drop --no-temp-sudo, or build as an account
that has one.
PROMPT
    else
        die "$BUILD_USER cannot sudo without a password, --no-temp-sudo was given, and there is no TTY here
to answer a prompt. Drop --no-temp-sudo to let this script grant it for the run, or grant it yourself:
  printf '$BUILD_USER ALL=(ALL) NOPASSWD:ALL\\n' >/etc/sudoers.d/$BUILD_USER && chmod 440 /etc/sudoers.d/$BUILD_USER"
    fi

    echo "mode:         deploy (running as root)"
    echo "branch:       $BRANCH"
    echo "repository:   $REPO_URL"
    echo "build as:     $BUILD_USER$([[ "$bootstrap" == true ]] && echo '  (--bootstrap)')"
    echo "source in:    $SRC"
    echo "install to:   $PREFIX"
    [[ ${#forward_args[@]} -eq 0 ]] || echo "build flags:  ${forward_args[*]}"

    # --- 1. A fresh checkout ------------------------------------------------------------------
    step "1/2  Cloning $BRANCH"

    rm -rf "${SRC:?}"
    # install -d rather than mkdir + chown: one call, and the ownership is right from the moment the
    # directory exists rather than for the window between the two.
    install -d -o "$BUILD_USER" -g "$BUILD_USER" "$SRC"

    # Cloned AS the build account, so every file in the tree is already owned by the account that has to
    # build it. --depth 1 because nothing here reads history; it is a deploy, not a working copy.
    sudo -u "$BUILD_USER" -H git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$SRC/repo" ||
        die "could not clone $BRANCH from $REPO_URL — does that branch exist?"

    # THE CLONE'S copy of this script, deliberately, not "$0" — see the header. The branch carries its own
    # installer, and re-running the launched copy would install the branch's jars under whatever logic the
    # copy you happened to have on disk implements.
    #
    # Spelled out rather than derived from "$0": the launched copy is not necessarily named this. The
    # bootstrap one-liner curls this file to wherever it likes, and `basename "$0"` would then look for
    # that name inside the clone and die on a path that never existed.
    local deploy_dir installer
    deploy_dir="$SRC/repo/serverpackcreator-grinder/deploy"
    installer="$deploy_dir/install-grinder.sh"
    [[ -x "$installer" ]] || die "no executable installer at $installer"

    # --- 2. Build and install -----------------------------------------------------------------
    step "2/2  Building and installing $BRANCH as $BUILD_USER"

    # %q each argument rather than interpolating the array: `bash -lc` takes ONE string, so an argument
    # containing a space or a quote would otherwise be re-split by the child's parser into something the
    # caller never wrote. None of today's flags can trigger it; the next one to take a value would.
    local quoted_args="" arg
    if [[ ${#forward_args[@]} -gt 0 ]]; then
        for arg in "${forward_args[@]}"; do quoted_args+=" $(printf '%q' "$arg")"; done
    fi

    # -H sets HOME to the build account's, which Gradle needs for ~/.gradle; without it Gradle writes into
    # root's home as an unprivileged user and fails on permissions.
    #
    # `bash -lc`, not `-i`: -i runs the account's login shell, which for a service-style account is
    # /usr/sbin/nologin. Naming bash explicitly means sudo never consults the shell field, and -l reads the
    # profile that puts JAVA_HOME and the toolchain on PATH.
    sudo -u "$BUILD_USER" -H bash -lc \
        "cd $(printf '%q' "$deploy_dir") && ./install-grinder.sh$quoted_args"

    if [[ "$bootstrap" == true ]]; then
        step "Enabling and starting spc-grinder"
        # `enable --now` rather than `start`: a bootstrapped host should come back up after a reboot, which
        # is the whole point of installing it as a service. Idempotent, so re-running --bootstrap is safe.
        systemctl enable --now spc-grinder
        systemctl --no-pager --lines=0 status spc-grinder || true
    fi

    step "Done"
    cat <<NEXT
$BRANCH is installed. The build half restarted the service if it was running; if this was a first
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
}

# ====================================================================================================
if [[ "$mode" == deploy ]]; then
    run_deploy_mode
else
    run_build_mode
fi
