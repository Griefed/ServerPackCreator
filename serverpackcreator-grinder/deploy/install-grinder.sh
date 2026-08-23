#!/usr/bin/env bash
#
# Build and deploy the ServerPackCreator Grinder as a system service.
#
#   1. builds the runtime image the boots run in
#   2. runs :serverpackcreator-grinder:installDist
#   3. installs the distribution to /opt/spc-grinder
#   4. creates the service account, with a home directory and docker group membership
#
# Run it as your normal user — NOT as root. The build must not run as root or it leaves root-owned
# files in build/ that your next ordinary build cannot overwrite. The four privileged steps call sudo
# themselves and are the only things that do. The working directory does not matter: the repository
# root is derived from this script's own location.
#
# Re-running is safe: it is the upgrade path. An already-running service is stopped before its jars
# are replaced and restarted afterwards, and nothing under the service's home is touched.
#
# Usage:  ./serverpackcreator-grinder/deploy/install-grinder.sh [--install-unit] [--skip-image]
#
#   --install-unit   also copy spc-grinder.service to /etc/systemd/system and daemon-reload.
#                    It is NOT enabled or started for you; the commands are printed at the end.
#   --skip-image     leave the runtime image alone (it changes far less often than the code).
#   --no-pull        build the image without refreshing its base (for an offline rebuild).
#   --grant-docker   allow adding an ALREADY-EXISTING account to the docker group. That group is
#                    root-equivalent — `docker run -v /:/host` owns the box — so granting it to an
#                    account this script did not create is refused unless you ask for it explicitly.
#                    An account the script creates is a purpose-made service account and is added
#                    without this flag.

set -Eeuo pipefail

PREFIX="${PREFIX:-/opt/spc-grinder}"
SERVICE_USER="${SERVICE_USER:-grinder}"
SERVICE_HOME="${SERVICE_HOME:-/home/grinder}"
IMAGE="${IMAGE:-spc-grinder-runtime:latest}"
UNIT_NAME="spc-grinder.service"

install_unit=false
skip_image=false
no_pull=false
grant_docker=false
was_active=false
for arg in "$@"; do
    case "$arg" in
        --install-unit) install_unit=true ;;
        --skip-image)   skip_image=true ;;
        --no-pull)      no_pull=true ;;
        --grant-docker) grant_docker=true ;;
        # The header block, however long it grows. A fixed line range was wrong within one commit of
        # being written — the range is the kind of citation that rots the moment the text above moves.
        -h|--help)      awk 'NR>1 && /^#/ {sub(/^# ?/,""); print; next} NR>1 {exit}' "$0"; exit 0 ;;
        *)              echo "unknown option: $arg (try --help)" >&2; exit 2 ;;
    esac
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
dist_dir="$repo_root/serverpackcreator-grinder/build/install/serverpackcreator-grinder"

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31merror: %s\033[0m\n' "$1" >&2; exit 1; }

# `set -E` above is what makes this fire inside functions and subshells too; without a trap it was an
# inert flag. Reporting the line matters here because most of what follows is a one-line sudo call.
trap 'printf "\033[31mfailed at line %s\033[0m\n" "$LINENO" >&2' ERR

# An upgrade stops the service before replacing its jars. Without this, any later failure — a full
# disk, a bad sudo, an interrupted copy — exits under `set -e` and leaves the service DOWN, turning a
# transient error into an outage nobody is told about.
restore_service() {
    local status=$?
    if [[ $status -ne 0 && "$was_active" == true ]]; then
        printf '\033[31m\nthe install failed after stopping %s — starting it again\033[0m\n' "$UNIT_NAME" >&2
        sudo systemctl start "$UNIT_NAME" || printf '\033[31mcould not restart it; do so by hand\033[0m\n' >&2
    fi
}
trap restore_service EXIT

# --- Preflight ------------------------------------------------------------------------------------
step "Checking prerequisites"

[[ -f "$repo_root/settings.gradle.kts" ]] || die "not a ServerPackCreator checkout: $repo_root"

# This script rm -rf's $PREFIX/lib. ${PREFIX:?} below catches an empty value and nothing else, so the
# shape of the path is checked here instead: absolute, and at least two components deep, so that a
# PREFIX of "/" or "/usr" cannot turn the cleanup into something catastrophic.
[[ "$PREFIX" == /* ]]              || die "PREFIX must be an absolute path, got '$PREFIX'"
[[ "$PREFIX" =~ ^/[^/]+/[^/]+ ]]   || die "PREFIX looks too close to the root to rm -rf under: '$PREFIX'"
[[ "$(id -u)" -ne 0 ]] || die "do not run this as root — the Gradle build would leave root-owned files in build/"

command -v docker >/dev/null || die "docker not found on PATH"
docker info >/dev/null 2>&1 || die "cannot reach the Docker daemon — is it running, and are you in the docker group?"
command -v sudo  >/dev/null || die "sudo not found; the privileged steps cannot run"
getent group docker >/dev/null || die "no 'docker' group on this host — the service account could never reach the socket"

# Checked here, before anything is built or changed: PREFIX, SERVICE_USER and SERVICE_HOME are
# overridable while the shipped unit hardcodes all three, and an install that disagrees with the unit
# only fails later, at `systemctl start`, with nothing pointing back at the override.
unit_file="$script_dir/$UNIT_NAME"
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

echo "repository:   $repo_root"
echo "install to:   $PREFIX"
echo "service user: $SERVICE_USER (home $SERVICE_HOME)"
echo "runtime image: $IMAGE"

# Ask once up front rather than surprising you four steps in. The timestamp only lasts ~15 minutes,
# which a cold image build plus a Gradle build can outlive, so it is refreshed again before the
# privileged steps rather than assumed to still be valid.
sudo -v

# --- 1. The runtime image -------------------------------------------------------------------------
if [[ "$skip_image" == true ]]; then
    step "Skipping the runtime image (--skip-image)"
    docker image inspect "$IMAGE" >/dev/null 2>&1 || die "$IMAGE does not exist, so it cannot be skipped"
else
    step "1/4  Building the runtime image: $IMAGE"
    # --pull by default: without it a cached base silently survives a rebuild, so "rebuild the image
    # for a newer JDK" can quietly do nothing. --no-pull is the escape for an offline rebuild.
    pull_flag=(--pull)
    [[ "$no_pull" != true ]] || pull_flag=()
    docker build "${pull_flag[@]}" -t "$IMAGE" "$repo_root/serverpackcreator-grinder/docker"
fi

# --- 2. The distribution --------------------------------------------------------------------------
step "2/4  Building the distribution (installDist)"
( cd "$repo_root" && ./gradlew :serverpackcreator-grinder:installDist )
[[ -x "$dist_dir/bin/serverpackcreator-grinder" ]] || die "installDist produced no launcher at $dist_dir/bin/"

# --- 3. Install it --------------------------------------------------------------------------------
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

# --- 4. The service account -----------------------------------------------------------------------
step "4/4  Service account: $SERVICE_USER"

created_account=false
if id -u "$SERVICE_USER" >/dev/null 2>&1; then
    echo "user $SERVICE_USER already exists — not modifying the account itself"
else
    nologin_shell="$(command -v nologin || echo /usr/sbin/nologin)"
    sudo useradd --system --create-home --home-dir "$SERVICE_HOME" --shell "$nologin_shell" "$SERVICE_USER"
    created_account=true
    echo "created $SERVICE_USER with home $SERVICE_HOME and shell $nologin_shell"
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

# Docker group membership is root-equivalent: `docker run -v /:/host` yields the whole filesystem.
# Granting it to an account this script created is the point of that account. Granting it to one that
# already existed — a human login, say — is a privilege escalation the operator has to ask for.
if id -nG "$SERVICE_USER" | tr ' ' '\n' | grep -qx docker; then
    echo "$SERVICE_USER is already in the docker group"
elif [[ "$created_account" == true || "$grant_docker" == true ]]; then
    sudo usermod -aG docker "$SERVICE_USER"
    echo "added $SERVICE_USER to the docker group (root-equivalent; it is a service account)"
else
    die "$SERVICE_USER already existed and is not in the docker group.
Adding it there grants root-equivalent privilege — anyone who can run docker can mount / and own the
host — so this script will not do that to an account it did not create. Either use a dedicated
account, or re-run with --grant-docker if $SERVICE_USER really is meant to be one."
fi

# --- The JVM the service will actually see -----------------------------------------------------------
# The launcher needs JAVA_HOME or a `java` on PATH. The operator has both — they just ran Gradle — and
# the service has neither unless the host provides them, which is why this is checked against systemd's
# PATH rather than the caller's. `env -i` drops the caller's environment entirely, so the answer is the
# service's, not a flattering version of it.
step "Checking the JVM the service will see"

systemd_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
unit_java_home="$(sed -n 's/^Environment=JAVA_HOME=//p' "$script_dir/$UNIT_NAME" | head -1)"

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

# --- Optional: the unit ---------------------------------------------------------------------------
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

# --- What is left for you -------------------------------------------------------------------------
step "Done"
cat <<NEXT
Review the unit's configuration before the first start — every variable is listed in it, commented
out, with its default:

  $script_dir/$UNIT_NAME

Two worth a decision rather than a default:
  SPC_GRINDER_WORKERS  budget 3 GiB of Docker-available memory each; the default of 2 is conservative
  SPC_GRINDER_HOST     loopback unless a reverse proxy needs it; the report has NO authentication

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
