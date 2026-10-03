#!/bin/bash

# Build-Script for Java 21 apps as AppImages
# Builds a gradle-based application into an AppImage and includes a Java 21 JDK in it
# Targets the host architecture by default. --arch cross-packages for the other architecture with no
# Docker and no emulation: everything that ends up *inside* the AppImage is downloaded rather than
# executed (the JDK is unpacked, the JAR is already built), so the only arch-specific thing that has
# to run is appimagetool -- and that runs natively while ARCH tells it which runtime to embed.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
APP_VERSION="dev"
TARGET_ARCH=""
# Which GitHub release AppImageUpdate should look in, or empty for no update information at all.
# Empty is the right default: a local build is not published anywhere, so an AppImage claiming it can
# update itself would be claiming something about a file nobody can fetch.
UPDATE_CHANNEL=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            echo "Usage: $0 [OPTIONS] [VERSION]"
            echo ""
            echo "Builds an AppImage for the host architecture, or for the one given by --arch."
            echo ""
            echo "Options:"
            echo "  -h, --help         Show this help message"
            echo "  -a, --arch ARCH    Target architecture: x86_64 or aarch64 (default: the host's)"
            echo "                     A foreign target cross-packages -- the bundled JDK and the"
            echo "                     embedded runtime are the target's, appimagetool stays native."
            echo "  -u, --update-channel CHANNEL"
            echo "                     Embed update information for AppImageUpdate and write the"
            echo "                     matching .zsync beside the AppImage. CHANNEL is the GitHub"
            echo "                     release to look in: 'latest' (stable releases) or 'continuous'"
            echo "                     (the rolling dev build). Omit it for a local build."
            echo "                     Needs no extra tool: appimagetool bundles zsyncmake."
            echo ""
            echo "Arguments:"
            echo "  VERSION            Version string (default: 'dev')"
            echo ""
            echo "Examples:"
            echo "  $0                 # Build for host architecture, version 'dev'"
            echo "  $0 1.0.0           # Build for host architecture, version '1.0.0'"
            echo "  $0 --arch aarch64 1.0.0   # Build an aarch64 AppImage on any supported host"
            echo "  $0 --update-channel latest 1.0.0   # ... and make it self-updating"
            echo ""
            exit 0
            ;;
        -a|--arch)
            if [ -z "${2:-}" ]; then
                echo -e "${RED}--arch requires a value: x86_64 or aarch64${NC}"
                exit 1
            fi
            TARGET_ARCH="$2"
            shift 2
            ;;
        -u|--update-channel)
            if [ -z "${2:-}" ]; then
                echo -e "${RED}--update-channel requires a value: latest or continuous${NC}"
                exit 1
            fi
            case "$2" in
                latest|continuous) ;;
                *)
                    echo -e "${RED}--update-channel must be 'latest' or 'continuous', got '$2'${NC}"
                    exit 1
                    ;;
            esac
            UPDATE_CHANNEL="$2"
            shift 2
            ;;
        -*)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
        *)
            APP_VERSION="$1"
            shift
            ;;
    esac
done

echo -e "${GREEN}Script-Dir: $SCRIPT_DIR"
echo -e "${GREEN}Project-Root: $PROJECT_ROOT"
echo ""

# Two architectures matter here and they are not always the same one. The host's decides which
# appimagetool binary is downloaded, because that is the one process that has to execute. The
# target's decides which JDK is bundled, which runtime appimagetool embeds, and what the output is
# called.
HOST_ARCH="$(uname -m)"
case "${HOST_ARCH}" in
    x86_64|amd64)
        HOST_APPIMAGE_ARCH=x86_64
        ;;
    aarch64|arm64)
        HOST_APPIMAGE_ARCH=aarch64
        ;;
    *)
        echo -e "${RED}Unsupported host architecture: ${HOST_ARCH}${NC}"
        echo -e "${YELLOW}Supported architectures: x86_64, aarch64${NC}"
        exit 1
        ;;
esac

# No --arch means target the host, so an invocation without it behaves exactly as it did before.
if [ -z "$TARGET_ARCH" ]; then
    TARGET_ARCH="$HOST_APPIMAGE_ARCH"
fi
case "${TARGET_ARCH}" in
    x86_64|amd64)
        BUILD_ARCH=x86_64
        JDK_ARCH=x64
        ;;
    aarch64|arm64)
        BUILD_ARCH=aarch64
        JDK_ARCH=aarch64
        ;;
    *)
        echo -e "${RED}Unsupported target architecture: ${TARGET_ARCH}${NC}"
        echo -e "${YELLOW}Supported architectures: x86_64, aarch64${NC}"
        exit 1
        ;;
esac

APPIMAGETOOL_ARCH="$HOST_APPIMAGE_ARCH"
if [ "$BUILD_ARCH" = "$HOST_APPIMAGE_ARCH" ]; then
    CROSS_PACKAGING=false
else
    CROSS_PACKAGING=true
fi

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Linux*)  MACHINE=Linux;;
    Darwin*) MACHINE=Mac;;
    *)       MACHINE="UNKNOWN:${OS}"
esac

echo -e "${GREEN}Host OS: $MACHINE ($HOST_ARCH)${NC}"
echo -e "${GREEN}Build Architecture: $BUILD_ARCH${NC}"
if [ "$CROSS_PACKAGING" = true ]; then
    echo -e "${YELLOW}Cross-packaging: appimagetool runs as ${APPIMAGETOOL_ARCH}, output targets ${BUILD_ARCH}${NC}"
fi
echo -e "${GREEN}Build Version: ${APP_VERSION}${NC}"
echo ""

if [ "$MACHINE" != "Linux" ]; then
    echo -e "${RED}AppImages can only be built on Linux!${NC}"
    echo -e "${YELLOW}Please run this script on a Linux system (e.g. a GitHub Actions runner).${NC}"
    exit 1
fi

# Configuration
APP_NAME="ServerPackCreator"
APP_DIR="${APP_NAME}.AppDir"
APP_COMMENT="Create server packs from Minecraft Forge, NeoForge, Fabric, Quilt or LegacyFabric modpacks."
APP_CATEGORIES="Utility;FileTools;Java;"
APP_ARGS="-Dfile.encoding=UTF-8 -Dlog4j2.formatMsgNoLookups=true -DServerPackCreator -Dname=ServerPackCreator -Dspring.application.name=ServerPackCreator -Dcom.apple.mrj.application.apple.menu.about.name=ServerPackCreator"
APP_MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
GRADLE_TASK="build"
GRADLE_ARGS="--info --full-stacktrace --warning-mode all -x :serverpackcreator-api:test -x :serverpackcreator-app:test"
BUILD_DIR="serverpackcreator-app/build/libs"
JDK_VERSION="21"
# PINNED, not `latest/<major>/ga`. The bundled JDK is the only thing in the AppImage that links against
# the host C library, so it alone decides which systems the artifact runs on -- and an unpinned URL moves
# that floor on somebody else's release schedule, with no commit here to blame when it does. The release
# name is URL-encoded: Adoptium's `+` build separator has to arrive as %2B.
# Bump this and JDK_MAX_GLIBC together, and read what the guard below says before accepting the new value.
JDK_RELEASE="jdk-21.0.12.1+1"
JDK_URL="https://api.adoptium.net/v3/binary/version/${JDK_RELEASE//+/%2B}/linux/${JDK_ARCH}/jdk/hotspot/normal/eclipse"
JDK_DIR="jdk-${JDK_VERSION}-${BUILD_ARCH}"
# The highest glibc symbol version the bundled JDK may reference, i.e. the oldest system this AppImage
# runs on. Measured for jdk-21.0.12.1+1: GLIBC_2.15, set by lib/libjava.so and lib/server/libjvm.so,
# which is a March 2012 floor. The AppImage runtime itself contributes nothing -- it is static-pie -- and
# the JDK ships no libc, libstdc++ or libgcc of its own, so this one number IS the artifact's floor.
JDK_MAX_GLIBC="2.15"
# Where AppImageUpdate looks. GitHub rather than git.griefed.de because the AppImage spec's only
# release-aware transport is `gh-releases-zsync`: a plain `zsync|<url>` needs a URL that stays stable
# across versions, and Forgejo serves no such route -- `/releases/latest/download/<asset>` answers 404,
# and `/releases/latest` is a redirect to the tag PAGE. The GitHub mirror carries every release asset,
# which is what `release-build.yml`'s `mirror` job is for, and devbuild.yml publishes the rolling
# `continuous` release there itself.
UPDATE_OWNER="Griefed"
UPDATE_REPO="ServerPackCreator"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${APPIMAGETOOL_ARCH}.AppImage"
APPIMAGETOOL_BIN="./appimagetool-${APPIMAGETOOL_ARCH}.AppImage"

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up temporary files...${NC}"
    rm -rf "$APP_DIR"
    rm -rf squashfs-root
    echo -e "${GREEN}Cleanup done.${NC}"
}

trap cleanup EXIT

echo -e "${GREEN}=== Build-Process started ===${NC}"

# Check Dependencies
echo -e "${YELLOW}Checking Dependencies...${NC}"

if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Gradle-Wrapper (gradlew) not found!${NC}"
    echo -e "${YELLOW}Ensure it's present in the root directory of the project.${NC}"
    exit 1
fi
chmod +x ./gradlew

for cmd in wget curl tar file; do
    if ! command -v "$cmd" &> /dev/null; then
        echo -e "${YELLOW}Warning: '$cmd' not found.${NC}"
    fi
done

# Download appimagetool if not present
echo -e "${YELLOW}Checking appimagetool for ${APPIMAGETOOL_ARCH} (the host)...${NC}"
if [ ! -f "$APPIMAGETOOL_BIN" ]; then
    echo -e "${YELLOW}Downloading appimagetool...${NC}"
    if command -v wget &> /dev/null; then
        wget -O "$APPIMAGETOOL_BIN" "$APPIMAGETOOL_URL"
    elif command -v curl &> /dev/null; then
        curl -L -o "$APPIMAGETOOL_BIN" "$APPIMAGETOOL_URL"
    else
        echo -e "${RED}Neither wget nor curl found. Please install one of them.${NC}"
        exit 1
    fi
    chmod +x "$APPIMAGETOOL_BIN"
    echo -e "${GREEN}appimagetool downloaded.${NC}"
else
    echo -e "${GREEN}appimagetool already present.${NC}"
fi

# Extract appimagetool (AppImages can't run directly without FUSE; extract and use directly)
echo -e "${YELLOW}Extracting appimagetool...${NC}"
"$APPIMAGETOOL_BIN" --appimage-extract > /dev/null
APPIMAGETOOL="$(pwd)/squashfs-root/AppRun"
echo -e "${GREEN}appimagetool ready.${NC}"

# Download JDK
echo -e "${YELLOW}Checking Java ${JDK_RELEASE} for ${BUILD_ARCH}...${NC}"
if [ ! -d "$JDK_DIR" ]; then
    echo -e "${YELLOW}Downloading Java ${JDK_RELEASE} for ${BUILD_ARCH}...${NC}"
    if command -v wget &> /dev/null; then
        wget -O jdk.tar.gz "$JDK_URL"
    elif command -v curl &> /dev/null; then
        curl -L -o jdk.tar.gz "$JDK_URL"
    else
        echo -e "${RED}Neither wget nor curl found. Please install one of them.${NC}"
        exit 1
    fi
    echo -e "${YELLOW}Extracting JDK...${NC}"
    mkdir -p "$JDK_DIR"
    tar -xzf jdk.tar.gz -C "$JDK_DIR" --strip-components=1
    rm jdk.tar.gz
    echo -e "${GREEN}JDK downloaded and extracted.${NC}"
else
    echo -e "${GREEN}JDK already present.${NC}"
fi

# Check for existing JAR
echo -e "${YELLOW}Checking if JAR already exists...${NC}"
EXISTING_JAR=$(find "$BUILD_DIR" -name "*.jar" ! -name "*-javadoc.jar" ! -name "*-sources.jar" ! -name "*-plain.jar" 2>/dev/null | head -n 1)

if [ -n "$EXISTING_JAR" ]; then
    echo -e "${GREEN}JAR already present: $EXISTING_JAR${NC}"
    echo -e "${YELLOW}Skipping Gradle-Build. To rebuild, delete $BUILD_DIR/*.jar${NC}"
    JAR_FILE="$EXISTING_JAR"
else
    echo -e "${YELLOW}No JAR found. Building using Gradle Wrapper...${NC}"
    ./gradlew clean --info --full-stacktrace
    ./gradlew $GRADLE_TASK -Pversion=$APP_VERSION $GRADLE_ARGS

    JAR_FILE=$(find "$BUILD_DIR" -name "*.jar" ! -name "*-javadoc.jar" ! -name "*-sources.jar" ! -name "*-plain.jar" | head -n 1)

    if [ -z "$JAR_FILE" ]; then
        echo -e "${RED}No JAR file found in $BUILD_DIR${NC}"
        exit 1
    fi
    echo -e "${GREEN}JAR created: $JAR_FILE${NC}"
fi

echo -e "${GREEN}Using JAR: $JAR_FILE${NC}"

# Create AppImage structure
echo -e "${YELLOW}Creating AppImage structure...${NC}"
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/usr/bin"
mkdir -p "$APP_DIR/usr/lib"
mkdir -p "$APP_DIR/usr/share/applications"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/512x512/apps"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/scalable/apps"
mkdir -p "$APP_DIR/usr/share/metainfo"

# Copy JDK
echo -e "${YELLOW}Copying Java JDK into AppImage...${NC}"
cp -rp "$JDK_DIR" "$APP_DIR/usr/lib/jdk"
chmod +x "$APP_DIR/usr/lib/jdk/bin/"* 2>/dev/null || true
echo -e "${GREEN}JDK copied (Size: $(du -sh "$APP_DIR/usr/lib/jdk" | cut -f1))${NC}"

if [ ! -f "$APP_DIR/usr/lib/jdk/bin/java" ]; then
    echo -e "${RED}ERROR: JDK was not correctly copied!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ JDK exists in APP_DIR${NC}"

# Assert the portability floor, against the copy in the AppDir rather than the download, so it is the
# shipped bytes that are measured. Without this a JDK bump moves which systems the artifact runs on and
# nothing says so: the build stays green, the AppImage still launches on the builder, and the first
# report comes from a user on an older distro.
echo -e "${YELLOW}Checking the bundled JDK's glibc floor (max ${JDK_MAX_GLIBC})...${NC}"
HIGHEST_GLIBC="$(grep -rhao 'GLIBC_[0-9][0-9.]*' "$APP_DIR/usr/lib/jdk" 2>/dev/null \
    | sed 's/^GLIBC_//' | sort -V -u | tail -n 1)"
if [ -z "$HIGHEST_GLIBC" ]; then
    # A check that cannot run must fail rather than pass. Every JDK references versioned glibc symbols,
    # so reading none of them means the scan is broken, not that the JDK is independent of glibc.
    echo -e "${RED}ERROR: could not read a single GLIBC_ symbol version from the bundled JDK.${NC}"
    echo -e "${YELLOW}The floor check cannot answer, so it is not answering 'fine'.${NC}"
    exit 1
fi
if [ "$(printf '%s\n%s\n' "$JDK_MAX_GLIBC" "$HIGHEST_GLIBC" | sort -V | tail -n 1)" != "$JDK_MAX_GLIBC" ]; then
    echo -e "${RED}ERROR: the bundled JDK needs glibc ${HIGHEST_GLIBC}, above the ${JDK_MAX_GLIBC} floor.${NC}"
    echo -e "${YELLOW}Raised by:${NC}"
    grep -rlao "GLIBC_${HIGHEST_GLIBC}" "$APP_DIR/usr/lib/jdk" 2>/dev/null | sed 's|^|  |'
    echo -e "${YELLOW}This AppImage would stop running on systems it used to run on. Either pin a JDK${NC}"
    echo -e "${YELLOW}that keeps the floor, or raise JDK_MAX_GLIBC deliberately and say so in the commit.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Bundled JDK needs glibc ${HIGHEST_GLIBC} at most (floor ${JDK_MAX_GLIBC})${NC}"

# Copy JAR
cp "$JAR_FILE" "$APP_DIR/usr/lib/${APP_NAME}.jar"

# Create Launcher Script
cat > "$APP_DIR/usr/bin/${APP_NAME}" << LAUNCHER_EOF
#!/bin/bash
SCRIPT_PATH="\$(readlink -f "\$0")"
APPDIR="\${SCRIPT_PATH%/usr/bin/*}"
BUNDLED_JAVA="\$APPDIR/usr/lib/jdk/bin/java"
REQUIRED_JAVA_VERSION=$JDK_VERSION

if [ "\$DEBUG" = "1" ]; then
    echo "DEBUG Launcher:"
    echo "  SCRIPT_PATH=\$SCRIPT_PATH"
    echo "  APPDIR=\$APPDIR"
    echo "  BUNDLED_JAVA=\$BUNDLED_JAVA"
fi

# Whatever the last probed java printed, kept so a refusal can say WHY rather than only that it
# refused. "The bundled JDK did not work" is unactionable; its own error message usually is not.
JAVA_PROBE_OUTPUT=""

# Major version last read, or empty. A GLOBAL and not a return value, like JAVA_PROBE_OUTPUT beside it:
# `x=\$(probe_java ...)` would run the function in a subshell and neither global would reach the caller,
# so a refusal could never quote what the JDK actually said.
JAVA_MAJOR=""

# Run `\$1 -version`, leaving its output in JAVA_PROBE_OUTPUT and its major version in JAVA_MAJOR.
#
# Both patterns are ANCHORED on what a JDK actually prints, never on "the first digits in the line".
# POSIX `sed`/`awk` throughout, never `grep -oP`: -P is a GNU extension, and where it is absent the
# probe fails for a reason that has nothing to do with the JDK, silently discarding a good runtime.
probe_java() {
    local java_cmd="\$1"
    JAVA_MAJOR=""
    JAVA_PROBE_OUTPUT=\$("\$java_cmd" -version 2>&1)
    local first_line
    first_line=\$(printf '%s\n' "\$JAVA_PROBE_OUTPUT" | head -n 1)
    # `openjdk version "21.0.12.1" 2026-07-15` -- what every current JDK prints.
    JAVA_MAJOR=\$(printf '%s\n' "\$first_line" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p')
    if [ -z "\$JAVA_MAJOR" ]; then
        # `openjdk 21.0.1 2026-01-01` -- the unquoted form some distributions use. The second field
        # only, and only when it starts with a version number: anything looser reads a digit out of a
        # FAILURE message and reports it as a version, which is how an unusable JDK gets exec'd.
        JAVA_MAJOR=\$(printf '%s\n' "\$first_line" \
            | awk '\$2 ~ /^[0-9]+([.]|\$)/ { split(\$2, parts, "."); print parts[1]; exit }')
    fi
    [ -n "\$JAVA_MAJOR" ]
}

# Whether \$1 is a runnable Java of at least \$REQUIRED_JAVA_VERSION.
check_java_version() {
    local java_cmd="\$1"
    [ -x "\$java_cmd" ] || return 1
    probe_java "\$java_cmd" || return 1
    [ "\$JAVA_MAJOR" -ge "\$REQUIRED_JAVA_VERSION" ]
}

JAVA=""
if [ -f "\$BUNDLED_JAVA" ]; then
    # An AppImage is a read-only squashfs, so this only ever matters for an extracted AppDir.
    [ -x "\$BUNDLED_JAVA" ] || chmod +x "\$BUNDLED_JAVA" 2>/dev/null || true
    if check_java_version "\$BUNDLED_JAVA"; then
        JAVA="\$BUNDLED_JAVA"
        [ "\$DEBUG" = "1" ] && echo "✓ Using bundled JDK: \$JAVA"
    fi
fi

# A bundled JDK that will not run is a BROKEN ARTIFACT, and quietly continuing on the system's Java is
# how that ships unnoticed: this AppImage carries a whole JDK precisely so no system Java is needed, the
# old warning went to stdout, and a .desktop launch has no stdout to read it from. So it is an error
# now, it says what the JDK itself reported, and the old behaviour is opt-in for anyone who needs it.
if [ -z "\$JAVA" ] && [ -f "\$BUNDLED_JAVA" ]; then
    echo "ERROR: the JDK bundled in this AppImage could not be used." >&2
    echo "  Tried: \$BUNDLED_JAVA" >&2
    if [ -n "\$JAVA_PROBE_OUTPUT" ]; then
        echo "  It reported:" >&2
        printf '%s\n' "\$JAVA_PROBE_OUTPUT" | sed 's/^/    /' >&2
    else
        echo "  It produced no output at all, which usually means the loader refused it:" >&2
        echo "  this build needs glibc ${JDK_MAX_GLIBC} or newer and will not run on a musl system." >&2
    fi
    echo "" >&2
    echo "  Please report this with the lines above -- a self-contained build should not need" >&2
    echo "  your system's Java. To run on it anyway, set SPC_ALLOW_SYSTEM_JAVA=1." >&2
    if [ "\$SPC_ALLOW_SYSTEM_JAVA" != "1" ]; then
        exit 1
    fi
fi

if [ -z "\$JAVA" ]; then
    if command -v java > /dev/null 2>&1; then
        SYSTEM_JAVA="\$(command -v java)"
        if check_java_version "\$SYSTEM_JAVA"; then
            JAVA="\$SYSTEM_JAVA"
            echo "⚠ Using system Java instead of the bundled JDK: \$JAVA" >&2
            printf '%s\n' "\$JAVA_PROBE_OUTPUT" | head -n 1 >&2
        fi
    fi
fi

if [ -z "\$JAVA" ]; then
    echo "ERROR: No compatible Java \$REQUIRED_JAVA_VERSION+ found!" >&2
    echo "Install Java or re-download the AppImage." >&2
    exit 1
fi

exec "\$JAVA" ${APP_ARGS} -jar "\$APPDIR/usr/lib/${APP_NAME}.jar" "\$@"
LAUNCHER_EOF

chmod +x "$APP_DIR/usr/bin/${APP_NAME}"

# Create Desktop Entry
cat > "$APP_DIR/usr/share/applications/${APP_NAME}.desktop" << EOF
[Desktop Entry]
Type=Application
Name=${APP_NAME}
Comment=${APP_COMMENT}
Exec=${APP_NAME}
Icon=${APP_NAME}
Categories=${APP_CATEGORIES}
Terminal=false
StartupWMClass=${APP_MAIN_CLASS}
EOF

# Copy icons and metadata
cp img/app_256x256.png  "$APP_DIR/usr/share/icons/hicolor/256x256/apps/${APP_NAME}.png"
cp img/app.png          "$APP_DIR/usr/share/icons/hicolor/512x512/apps/${APP_NAME}.png"
cp img/app.svg          "$APP_DIR/usr/share/icons/hicolor/scalable/apps/${APP_NAME}.svg"
cp "$APP_DIR/usr/share/icons/hicolor/256x256/apps/${APP_NAME}.png" "$APP_DIR/${APP_NAME}.png" 2>/dev/null || true
cp "$APP_DIR/usr/share/applications/${APP_NAME}.desktop" "$APP_DIR/${APP_NAME}.desktop"
cp misc/appdata.xml "$APP_DIR/usr/share/metainfo/de.griefed.${APP_NAME}.appdata.xml"

# Create AppRun
cat > "$APP_DIR/AppRun" << EOF
#!/bin/bash
APPDIR="\$(cd "\$(dirname "\$(readlink -f "\$0")")" && pwd)"
[ -z "\$APPDIR" ] && { echo "ERROR: APPDIR could not be computed"; exit 1; }

if [ "\$DEBUG" = "1" ]; then
    echo "DEBUG AppRun: APPDIR=\$APPDIR"
fi

export PATH="\$APPDIR/usr/bin:\$PATH"
export LD_LIBRARY_PATH="\$APPDIR/usr/lib:\$LD_LIBRARY_PATH"

if [ ! -f "\$APPDIR/usr/bin/${APP_NAME}" ]; then
    echo "ERROR: Launcher not found: \$APPDIR/usr/bin/${APP_NAME}"
    exit 1
fi

exec "\$APPDIR/usr/bin/${APP_NAME}" "\$@"
EOF

chmod +x "$APP_DIR/AppRun"

# Build the AppImage. ARCH is what makes a foreign target work: appimagetool supplies the runtime for
# the architecture named there rather than for its own, so a natively-running tool emits a foreign
# AppImage. Verified 2026-08-22 by running the aarch64 appimagetool with ARCH=x86_64 -- `file` reported
# the output as "ELF 64-bit LSB pie executable, x86-64", and identically with an explicit
# --runtime-file, so the flag is not needed. ARCH is also not optional: without it appimagetool guesses
# from the ELFs in the AppDir, and that guess is the bundled JDK's architecture only by luck.
if [ "$CROSS_PACKAGING" = true ]; then
    echo -e "${YELLOW}Building ${BUILD_ARCH} AppImage on a ${HOST_APPIMAGE_ARCH} host...${NC}"
else
    echo -e "${YELLOW}Building AppImage natively for ${BUILD_ARCH}...${NC}"
fi
# `_experimental` is part of the name on purpose, and this is the ONE place that decides it: the
# AppImage is far newer than the install4j installers, ships a JDK the project does not otherwise
# distribute, and the aarch64 one is cross-packaged rather than built on the architecture it targets.
# A user picking a download should be able to see that from the filename alone. Nothing else hardcodes
# the name -- the workflows glob `ServerPackCreator-*.AppImage` precisely so this stays a single
# decision (a bare `*.AppImage` would also match the `appimagetool-*.AppImage` downloaded beside it).
OUTPUT_APPIMAGE="${APP_NAME}-${APP_VERSION}-${BUILD_ARCH}_experimental.AppImage"
rm -f "$OUTPUT_APPIMAGE" "${OUTPUT_APPIMAGE}.zsync"

# Update information, when a channel was asked for. The glob is derived from the name above rather than
# written out again, and it is ARCH-SPECIFIC on purpose: a bare `ServerPackCreator-*.AppImage.zsync`
# would let an x86_64 install update itself to the aarch64 build.
APPIMAGETOOL_ARGS=()
if [ -n "$UPDATE_CHANNEL" ]; then
    # appimagetool writes the .zsync itself, with the zsyncmake it BUNDLES -- measured against the
    # continuous build (git 8c8c91f) in a container with no `zsync` package installed: it reported
    # "zsyncmake is available" and produced the file. Do not add an apt install for it.
    #
    # What makes that worth guarding anyway is the failure shape: without a zsyncmake it can reach,
    # appimagetool embeds the update information regardless and only skips the .zsync, on stderr, with
    # exit 0. The AppImage then advertises an update route whose .zsync nobody wrote, and that fails on
    # a user's machine rather than here -- so the check after the build reads back BOTH halves.
    UPDATE_INFORMATION="gh-releases-zsync|${UPDATE_OWNER}|${UPDATE_REPO}|${UPDATE_CHANNEL}|${APP_NAME}-*-${BUILD_ARCH}_experimental.AppImage.zsync"
    echo -e "${YELLOW}Update information: ${UPDATE_INFORMATION}${NC}"
    APPIMAGETOOL_ARGS+=(-u "$UPDATE_INFORMATION")
fi

ARCH=${BUILD_ARCH} "$APPIMAGETOOL" "${APPIMAGETOOL_ARGS[@]}" "$APP_DIR" "$OUTPUT_APPIMAGE"

# Verify the update information actually landed, in BOTH halves. appimagetool exits 0 having done only
# the first half when zsyncmake is missing, and the guard above is what prevents that -- this one
# catches every other way it could go wrong, by reading back what was written rather than trusting the
# exit code. The string is plain text at a known ELF section, so `grep -a` finds it.
if [ -n "$UPDATE_CHANNEL" ]; then
    echo -e "${YELLOW}Verifying the update information and its .zsync...${NC}"
    if ! grep -aqF "$UPDATE_INFORMATION" "$OUTPUT_APPIMAGE"; then
        echo -e "${RED}ERROR: the AppImage does not carry the update information it was built with.${NC}"
        echo -e "${YELLOW}Expected: ${UPDATE_INFORMATION}${NC}"
        exit 1
    fi
    if [ ! -s "${OUTPUT_APPIMAGE}.zsync" ]; then
        echo -e "${RED}ERROR: no ${OUTPUT_APPIMAGE}.zsync was produced.${NC}"
        echo -e "${YELLOW}An AppImage advertising updates without its .zsync published beside it fails${NC}"
        echo -e "${YELLOW}on the user's machine, not here.${NC}"
        exit 1
    fi
    # zsyncmake writes the AppImage's own name as the URL, relative to wherever the .zsync is served
    # from -- which is what makes one release asset resolve the other.
    if ! grep -qxF "URL: ${OUTPUT_APPIMAGE}" "${OUTPUT_APPIMAGE}.zsync"; then
        echo -e "${RED}ERROR: ${OUTPUT_APPIMAGE}.zsync does not point at ${OUTPUT_APPIMAGE}.${NC}"
        grep -i '^URL:' "${OUTPUT_APPIMAGE}.zsync" || echo "  it carries no URL header at all"
        exit 1
    fi
    echo -e "${GREEN}✓ Update information embedded, and ${OUTPUT_APPIMAGE}.zsync written${NC}"
fi

# Verify AppImage
if [ -f "$OUTPUT_APPIMAGE" ]; then
    APPIMAGE_SIZE=$(du -h "$OUTPUT_APPIMAGE" | cut -f1)
    echo -e "${GREEN}=== Success! ===${NC}"
    echo -e "${GREEN}AppImage: ${OUTPUT_APPIMAGE}${NC}"
    echo -e "${GREEN}Size: ${APPIMAGE_SIZE}${NC}"
    echo -e "${GREEN}Architecture: ${BUILD_ARCH}${NC}"
    if [ -n "$UPDATE_CHANNEL" ]; then
        echo -e "${GREEN}Update channel: ${UPDATE_CHANNEL} (publish ${OUTPUT_APPIMAGE}.zsync beside it)${NC}"
    fi
    echo -e "${YELLOW}Test with: ./${OUTPUT_APPIMAGE}${NC}"
else
    echo -e "${RED}✗ ERROR: AppImage was not created!${NC}"
    exit 1
fi