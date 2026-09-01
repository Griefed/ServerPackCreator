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
#   --clear          DELETE the service's data directory before installing, so the daemon starts with a
#                    clean slate: verdicts, crawl cursors, the re-grind queue, kept boot logs AND the
#                    loader cache. Everything the grind has learned. The binaries are unaffected; this
#                    is about state, not code. Nothing else in this script destroys data, so it is opt-in
#                    and says exactly what it removed.
#   --skip-browser   skip both halves of the headless-browser setup that distribution-locked CurseForge
#                    files need: the ~170 MB Chromium download and the OS libraries it links against.
#                    A Modrinth-only or offline host can do without. Note the daemon downloads the
#                    browser itself on first use anyway — what skipping really costs is the libraries,
#                    without which every locked-file navigation times out.
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
clear_home=false
skip_image=false
no_pull=false
grant_docker=false
skip_browser=false
was_active=false
for arg in "$@"; do
    case "$arg" in
        --install-unit) install_unit=true ;;
        --clear)        clear_home=true ;;
        --skip-image)   skip_image=true ;;
        --no-pull)      no_pull=true ;;
        --grant-docker) grant_docker=true ;;
        --skip-browser) skip_browser=true ;;
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

# The daemon's data directory, resolved the same way the daemon resolves it: SPC_GRINDER_HOME if the
# operator sets one, else `$HOME/.spc-grinder` (GrinderConfiguration.defaultHome). Resolved even when
# --clear was not given, so the line below always tells the operator where the state lives.
SERVICE_DATA="${SPC_GRINDER_HOME:-$SERVICE_HOME/.spc-grinder}"

# Guarded on the SHAPE, exactly as PREFIX is above, because --clear rm -rf's this. Absolute and at least
# two components deep, and never the account's whole home or the install prefix -- a SPC_GRINDER_HOME of
# `/home/grinder` would otherwise take the account's dotfiles, its ~/.gradle and its Playwright browsers
# with it, and one of `/opt/spc-grinder` would delete the binaries this script just installed.
if [[ "$clear_home" == true ]]; then
    [[ "$SERVICE_DATA" == /* ]]              || die "SPC_GRINDER_HOME must be an absolute path, got '$SERVICE_DATA'"
    [[ "$SERVICE_DATA" =~ ^/[^/]+/[^/]+ ]]   || die "SPC_GRINDER_HOME looks too close to the root to rm -rf: '$SERVICE_DATA'"
    [[ "$SERVICE_DATA" != "$SERVICE_HOME" ]] || die "--clear would delete the whole home of $SERVICE_USER ($SERVICE_HOME), not just its data"
    [[ "$SERVICE_DATA" != "$PREFIX" ]]       || die "--clear would delete the install prefix ($PREFIX)"
fi

echo "repository:   $repo_root"
echo "install to:   $PREFIX"
echo "service user: $SERVICE_USER (home $SERVICE_HOME)"
echo "service data: $SERVICE_DATA$([[ "$clear_home" == true ]] && echo '  ** WILL BE DELETED (--clear) **')"
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

# --- Optional: a clean slate ----------------------------------------------------------------------
# Deliberately here, AFTER the service was stopped in step 3, and the ordering is load-bearing: the
# daemon coalesces verdict writes and flushes the store on shutdown, so clearing a *running* service
# just gets it written back out of memory as the service stops. It is also after the account exists, so
# the directory it names is the one the daemon will actually use.
if [[ "$clear_home" == true ]]; then
    step "Clearing $SERVICE_DATA (--clear)"

    if [[ -d "$SERVICE_DATA" ]]; then
        size="$(sudo du -sh "$SERVICE_DATA" 2>/dev/null | cut -f1 || echo '?')"
        echo "removing $size of accumulated state:"
        # Named individually rather than as one line, because "the grinder's data" is four different
        # kinds of loss and the operator should see which ones they are asking for. `if` rather than
        # `[[ ]] &&` so a missing last entry does not fail the loop under `set -e`.
        for artefact in verdicts.json cursors.json requeue.json boot-logs crash-logs cache work; do
            if [[ -e "$SERVICE_DATA/$artefact" ]]; then
                echo "  - $artefact"
            fi
        done
        sudo rm -rf "${SERVICE_DATA:?}"
        echo "cleared — the daemon starts with no verdicts, at the head of the crawl, and re-downloads"
        echo "every loader install it needs (~150 MB per loader/Minecraft tuple, the expensive part)."
    else
        echo "$SERVICE_DATA does not exist — nothing to clear"
    fi
fi

# --- The JVM the service will actually see -----------------------------------------------------------
# The launcher needs JAVA_HOME or a `java` on PATH. The operator has both — they just ran Gradle — and
# the service has neither unless the host provides them, which is why this is checked against systemd's
# PATH rather than the caller's. `env -i` drops the caller's environment entirely, so the answer is the
# service's, not a flattering version of it.
step "Checking the JVM the service will see"

systemd_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
unit_java_home="$(sed -n 's/^Environment=JAVA_HOME=//p' "$script_dir/$UNIT_NAME" | head -1)"

# Captured, not just reported: the browser install below runs a Java main class and must use the same
# JVM the service will, and re-deriving it there is how the two answers drift apart.
service_java=""
if [[ -n "$unit_java_home" ]]; then
    if [[ -x "$unit_java_home/bin/java" ]]; then
        echo "unit sets JAVA_HOME=$unit_java_home, and $unit_java_home/bin/java is executable"
        service_java="$unit_java_home/bin/java"
    else
        die "the unit sets JAVA_HOME=$unit_java_home but $unit_java_home/bin/java is not executable"
    fi
elif env -i PATH="$systemd_path" sh -c 'command -v java' >/dev/null 2>&1; then
    service_java="$(env -i PATH="$systemd_path" sh -c 'command -v java')"
    echo "java found on systemd's PATH at $service_java"
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

# --- The headless browser CurseForge needs -----------------------------------------------------------
# Distribution-locked CurseForge files (`allowModDistribution=false`) carry no API download-URL at all
# and can only be fetched by driving the site with a headless Chromium, on the host, as the service
# account. Modrinth never needs it.
#
# **The browser is not the part that is usually missing.** Playwright's Java binding downloads browsers
# itself on the first `Playwright.create()` — `DriverJar.installBrowsers()`, which is also why
# `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD` exists — so a host that never ran an install still gets one. What
# it does NOT do is install the OS libraries Chromium links against, and on a headless server those are
# absent by default: Chromium then launches and every navigation times out, which reads as CurseForge
# being slow rather than as a missing dependency. The CI job for the same code path installs exactly
# those and nothing else (`clientside-report-reusable.yml` runs `install-deps chromium`), which is the
# shape of the real gap. This script previously only *warned* about both halves.
#
# So both are done here, and pre-installing the browser is worth it even though it self-installs: the
# lazy download happens mid-grind, needs network at an arbitrary later moment, and when it fails the
# operator sees a staging failure on some mod rather than an error about a browser. Doing it at deploy
# time makes it fail where somebody is watching.
#
# Installed with Playwright's OWN CLI, out of the jars we just installed — NOT with `npx playwright
# install`. Playwright pins one Chromium build per release and looks for that exact directory: 1.62.0
# wants `chromium-1234` (Chrome for Testing 151.0.7922.34, read from driver-1.62.0.jar's
# browsers.json). `npx` installs whatever revision the *npm* package pins, which for any other version
# lands beside it as `chromium-<other>` and leaves the Java binding still downloading its own — while a
# check for `chromium-*` happily reports it present, which is what the check here used to do. Driving
# the CLI from our own classpath makes the version match by construction: the same driver jar that will
# run the download decides what to fetch.
#
# It is also idempotent — already-installed is a no-op — so this re-runs on every upgrade rather than
# being guarded by a check that would have to replicate Playwright's own path logic to be correct. That
# is what keeps a Playwright version bump, which moves the pinned revision, from silently going stale.
if [[ "$skip_browser" == true ]]; then
    step "Skipping the headless browser (--skip-browser)"
    echo "the daemon will download it lazily on the first locked CurseForge file instead"
elif [[ -z "$service_java" ]]; then
    step "WARNING: cannot install the headless browser without a JVM"
    echo "resolve the JVM warning above, then re-run this script to install it"
else
    step "Installing the headless browser for locked CurseForge files"

    # As the service account and with -H, so the browser lands in the HOME the daemon will actually
    # search. The operator's own cache proves nothing about the service's.
    # NOT fatal, for the same reason it is worth doing at all: the daemon can still install this
    # itself on first use, so a transient download failure here must not take a deployment down with
    # it. Warn, and let the operator decide whether to care.
    if sudo -u "$SERVICE_USER" -H "$service_java" -cp "$PREFIX/lib/*" \
            com.microsoft.playwright.CLI install chromium; then
        echo "chromium installed for $SERVICE_USER"
    else
        step "WARNING: could not pre-install chromium for $SERVICE_USER"
        echo "the daemon will retry on its first locked CurseForge file — but it will do so mid-grind,"
        echo "and if it fails there the verdict reads as a staging failure rather than as a browser one"
    fi

    # The OS libraries Chromium links against, which need root and are a separate step. Playwright only
    # knows how to do this on Debian/Ubuntu, so a failure here is NOT fatal: on any other distribution
    # the operator installs them by hand, and killing a deployment over it would be wrong. Say what is
    # left undone instead, because without the libraries Chromium launches and then every navigation
    # times out — which reads as CurseForge being slow rather than as a missing dependency.
    if sudo "$service_java" -cp "$PREFIX/lib/*" com.microsoft.playwright.CLI install-deps chromium; then
        echo "system libraries for chromium are present"
    else
        step "WARNING: could not install chromium's system libraries"
        cat <<DEPS
Playwright can only install these automatically on Debian/Ubuntu. Chromium itself is downloaded, but
on a headless host without its libraries every navigation times out, so a locked CurseForge file
fails slowly instead of quickly. Install the equivalent packages for your distribution by hand — the
list Playwright would have installed is printed above — and then re-run just this step:

  sudo $service_java -cp "$PREFIX/lib/*" com.microsoft.playwright.CLI install-deps chromium

DEPS
    fi
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
