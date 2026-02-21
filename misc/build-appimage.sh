#!/bin/bash

# Build-Script for Java 21 apps as AppImages
# Builds a gradle-based application into an AppImage and includes a Java 21 JDK in it
# Builds natively for the host architecture (no Docker, no cross-compilation)

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

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            echo "Usage: $0 [OPTIONS] [VERSION]"
            echo ""
            echo "Builds an AppImage natively for the host architecture."
            echo ""
            echo "Options:"
            echo "  -h, --help         Show this help message"
            echo ""
            echo "Arguments:"
            echo "  VERSION            Version string (default: 'dev')"
            echo ""
            echo "Examples:"
            echo "  $0                 # Build for host architecture, version 'dev'"
            echo "  $0 1.0.0           # Build for host architecture, version '1.0.0'"
            echo ""
            exit 0
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

# Determine host architecture and map to AppImage arch names
HOST_ARCH="$(uname -m)"
case "${HOST_ARCH}" in
    x86_64|amd64)
        BUILD_ARCH=x86_64
        JDK_ARCH=x64
        APPIMAGETOOL_ARCH=x86_64
        ;;
    aarch64|arm64)
        BUILD_ARCH=aarch64
        JDK_ARCH=aarch64
        APPIMAGETOOL_ARCH=aarch64
        ;;
    *)
        echo -e "${RED}Unsupported host architecture: ${HOST_ARCH}${NC}"
        echo -e "${YELLOW}Supported architectures: x86_64, aarch64${NC}"
        exit 1
        ;;
esac

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Linux*)  MACHINE=Linux;;
    Darwin*) MACHINE=Mac;;
    *)       MACHINE="UNKNOWN:${OS}"
esac

echo -e "${GREEN}Host OS: $MACHINE ($HOST_ARCH)${NC}"
echo -e "${GREEN}Build Architecture: $BUILD_ARCH${NC}"
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
JDK_URL="https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/linux/${JDK_ARCH}/jdk/hotspot/normal/eclipse"
JDK_DIR="jdk-${JDK_VERSION}-${BUILD_ARCH}"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${APPIMAGETOOL_ARCH}.AppImage"
APPIMAGETOOL_BIN="./appimagetool-${BUILD_ARCH}.AppImage"

# Cleanup function
cleanup() {
    echo -e "${YELLOW}Cleaning up temporary files...${NC}"
    rm -rf "$APP_DIR"
    rm -f squashfs-root
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
echo -e "${YELLOW}Checking appimagetool for ${BUILD_ARCH}...${NC}"
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
echo -e "${YELLOW}Checking Java ${JDK_VERSION} JDK for ${BUILD_ARCH}...${NC}"
if [ ! -d "$JDK_DIR" ]; then
    echo -e "${YELLOW}Downloading Java ${JDK_VERSION} JDK for ${BUILD_ARCH}...${NC}"
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

check_java_version() {
    local java_cmd="\$1"
    if [ ! -x "\$java_cmd" ]; then
        return 1
    fi
    local version_output=\$("\$java_cmd" -version 2>&1)
    local java_version=\$(echo "\$version_output" | grep -oP '(?<=version ")([0-9]+)' | head -1)
    if [ -z "\$java_version" ]; then
        java_version=\$(echo "\$version_output" | grep -oP '(?<=openjdk )([0-9]+)' | head -1)
    fi
    if [ -z "\$java_version" ]; then
        return 1
    fi
    if [ "\$java_version" -ge "\$REQUIRED_JAVA_VERSION" ]; then
        return 0
    else
        return 1
    fi
}

JAVA=""
if [ -f "\$BUNDLED_JAVA" ] && [ -x "\$BUNDLED_JAVA" ]; then
    if check_java_version "\$BUNDLED_JAVA"; then
        JAVA="\$BUNDLED_JAVA"
        [ "\$DEBUG" = "1" ] && echo "✓ Using bundled JDK: \$JAVA"
    fi
elif [ -f "\$BUNDLED_JAVA" ] && [ ! -x "\$BUNDLED_JAVA" ]; then
    chmod +x "\$BUNDLED_JAVA" 2>/dev/null || true
    if [ -x "\$BUNDLED_JAVA" ] && check_java_version "\$BUNDLED_JAVA"; then
        JAVA="\$BUNDLED_JAVA"
        [ "\$DEBUG" = "1" ] && echo "✓ Using bundled JDK (fixed permissions): \$JAVA"
    fi
fi

if [ -z "\$JAVA" ]; then
    [ "\$DEBUG" = "1" ] && echo "⚠ Bundled JDK not available, checking for system Java..."
    if command -v java &> /dev/null; then
        SYSTEM_JAVA="\$(command -v java)"
        if check_java_version "\$SYSTEM_JAVA"; then
            JAVA="\$SYSTEM_JAVA"
            echo "⚠ Warning: Using system Java instead of bundled JDK"
            echo "  Location: \$JAVA"
            \$JAVA -version 2>&1 | head -1
        fi
    fi
fi

if [ -z "\$JAVA" ]; then
    echo "ERROR: No compatible Java \$REQUIRED_JAVA_VERSION+ found!"
    echo "Install Java or re-download the AppImage."
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
cp misc/appdata.xml "$APP_DIR/usr/share/metainfo/${APP_NAME}.appdata.xml"

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

# Build AppImage natively
echo -e "${YELLOW}Building AppImage natively for ${BUILD_ARCH}...${NC}"
OUTPUT_APPIMAGE="${APP_NAME}-${APP_VERSION}-${BUILD_ARCH}.AppImage"
rm -f "$OUTPUT_APPIMAGE"

ARCH=${BUILD_ARCH} "$APPIMAGETOOL" "$APP_DIR" "$OUTPUT_APPIMAGE"

# Verify AppImage
if [ -f "$OUTPUT_APPIMAGE" ]; then
    APPIMAGE_SIZE=$(du -h "$OUTPUT_APPIMAGE" | cut -f1)
    echo -e "${GREEN}=== Success! ===${NC}"
    echo -e "${GREEN}AppImage: ${OUTPUT_APPIMAGE}${NC}"
    echo -e "${GREEN}Size: ${APPIMAGE_SIZE}${NC}"
    echo -e "${GREEN}Architecture: ${BUILD_ARCH}${NC}"
    echo -e "${YELLOW}Test with: ./${OUTPUT_APPIMAGE}${NC}"
else
    echo -e "${RED}✗ ERROR: AppImage was not created!${NC}"
    exit 1
fi