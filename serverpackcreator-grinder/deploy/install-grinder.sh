#!/usr/bin/env bash
#
# Build and deploy the ServerPackCreator Grinder as a system service.
#
#   1. builds the runtime image the boots run in
#   2. runs :serverpackcreator-grinder:installDist
#   3. installs the distribution to /opt/spc-grinder
#   4. creates the service account, with a home directory and docker group membership
#
# Run it from the repository root, as your normal user — NOT as root. The build must not run as root
# or it leaves root-owned files in build/ that your next ordinary build cannot overwrite. The four
# privileged steps call sudo themselves and are the only things that do.
#
# Re-running is safe: it is the upgrade path. An already-running service is stopped before its jars
# are replaced and restarted afterwards, and nothing under the service's home is touched.
#
# Usage:  ./serverpackcreator-grinder/deploy/install-grinder.sh [--install-unit] [--skip-image]
#
#   --install-unit   also copy spc-grinder.service to /etc/systemd/system and daemon-reload.
#                    It is NOT enabled or started for you; the commands are printed at the end.
#   --skip-image     leave the runtime image alone (it changes far less often than the code).

set -Eeuo pipefail

PREFIX="${PREFIX:-/opt/spc-grinder}"
SERVICE_USER="${SERVICE_USER:-grinder}"
SERVICE_HOME="${SERVICE_HOME:-/home/grinder}"
IMAGE="${IMAGE:-spc-grinder-runtime:latest}"
UNIT_NAME="spc-grinder.service"

install_unit=false
skip_image=false
for arg in "$@"; do
    case "$arg" in
        --install-unit) install_unit=true ;;
        --skip-image)   skip_image=true ;;
        -h|--help)      sed -n '2,21p' "$0"; exit 0 ;;
        *)              echo "unknown option: $arg (try --help)" >&2; exit 2 ;;
    esac
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
dist_dir="$repo_root/serverpackcreator-grinder/build/install/serverpackcreator-grinder"

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31merror: %s\033[0m\n' "$1" >&2; exit 1; }

# --- Preflight ------------------------------------------------------------------------------------
step "Checking prerequisites"

[[ -f "$repo_root/settings.gradle.kts" ]] || die "not a ServerPackCreator checkout: $repo_root"
[[ "$(id -u)" -ne 0 ]] || die "do not run this as root — the Gradle build would leave root-owned files in build/"

command -v docker >/dev/null || die "docker not found on PATH"
docker info >/dev/null 2>&1 || die "cannot reach the Docker daemon — is it running, and are you in the docker group?"
command -v sudo  >/dev/null || die "sudo not found; the privileged steps cannot run"
getent group docker >/dev/null || die "no 'docker' group on this host — the service account could never reach the socket"

echo "repository:   $repo_root"
echo "install to:   $PREFIX"
echo "service user: $SERVICE_USER (home $SERVICE_HOME)"
echo "runtime image: $IMAGE"

# One sudo prompt up front rather than four scattered through a long build.
sudo -v

# --- 1. The runtime image -------------------------------------------------------------------------
if [[ "$skip_image" == true ]]; then
    step "Skipping the runtime image (--skip-image)"
    docker image inspect "$IMAGE" >/dev/null 2>&1 || die "$IMAGE does not exist, so it cannot be skipped"
else
    step "1/4  Building the runtime image: $IMAGE"
    docker build -t "$IMAGE" "$repo_root/serverpackcreator-grinder/docker"
fi

# --- 2. The distribution --------------------------------------------------------------------------
step "2/4  Building the distribution (installDist)"
( cd "$repo_root" && ./gradlew :serverpackcreator-grinder:installDist )
[[ -x "$dist_dir/bin/serverpackcreator-grinder" ]] || die "installDist produced no launcher at $dist_dir/bin/"

# --- 3. Install it --------------------------------------------------------------------------------
step "3/4  Installing to $PREFIX"

# Stop first if it is running: replacing jars under a live JVM is how you get a class-loading failure
# hours later, on a lazily-loaded class, with nothing in the log to connect it to this script.
was_active=false
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
sudo chmod -R a+rX "$PREFIX"
echo "installed $(find "$PREFIX/lib" -name '*.jar' | wc -l | tr -d ' ') jars; launcher at $PREFIX/bin/serverpackcreator-grinder"

# --- 4. The service account -----------------------------------------------------------------------
step "4/4  Service account: $SERVICE_USER"

if id -u "$SERVICE_USER" >/dev/null 2>&1; then
    echo "user $SERVICE_USER already exists — leaving it alone"
else
    nologin_shell="$(command -v nologin || echo /usr/sbin/nologin)"
    sudo useradd --system --create-home --home-dir "$SERVICE_HOME" --shell "$nologin_shell" "$SERVICE_USER"
    echo "created $SERVICE_USER with home $SERVICE_HOME and shell $nologin_shell"
fi

# --create-home is a no-op for an account that already existed without one.
if [[ ! -d "$SERVICE_HOME" ]]; then
    sudo mkdir -p "$SERVICE_HOME"
    sudo chown "$SERVICE_USER:$SERVICE_USER" "$SERVICE_HOME"
    echo "created missing home $SERVICE_HOME"
fi

if id -nG "$SERVICE_USER" | tr ' ' '\n' | grep -qx docker; then
    echo "$SERVICE_USER is already in the docker group"
else
    sudo usermod -aG docker "$SERVICE_USER"
    echo "added $SERVICE_USER to the docker group"
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
