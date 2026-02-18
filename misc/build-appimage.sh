#!/bin/bash

# Build-Script for Java 21 apps as AppImages
# Builds a gradle-based application into an AppImage and includes a Java 21 JDK in it
# Proof of concept script written using Claude Sonnet 4.5 and classic manual writing.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Script-Dir: $SCRIPT_DIR"
echo -e "${GREEN}Project-Root: $PROJECT_ROOT"
echo ""

if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Usage: $0 [VERSION]"
    echo ""
    echo "Builds an AppImage for the native architecture of the build-system."
    echo "Supports: x86_64, aarch64 (ARM64)"
    echo ""
    echo "Examples:"
    echo "  $0           # Builds with version 'dev'"
    echo "  $0 1.0.0     # Builds with version '1.0.0'"
    echo "  $0 2.1.3     # Builds with version '2.1.3'"
    echo ""
    exit 0
fi

OS="$(uname -s)"
case "${OS}" in
    Linux*)     MACHINE=Linux;;
    Darwin*)    MACHINE=Mac;;
    *)          MACHINE="UNKNOWN:${OS}"
esac

if [[ -z "$BUILD_ARCH" ]]; then
  ARCH="$(uname -m)"
  case "${ARCH}" in
      x86_64)     BUILD_ARCH=x86_64;;
      aarch64)    BUILD_ARCH=aarch64;;  # Linux ARM64
      arm64)      BUILD_ARCH=aarch64;;  # macOS ARM64
      *)
          echo -e "${RED}Unknown architecture: ${ARCH}${NC}"
          exit 1
          ;;
  esac
fi

echo -e "${GREEN}Detected OS: $MACHINE${NC}"
echo -e "${GREEN}Detected arch: $BUILD_ARCH${NC}"

APP_NAME="ServerPackCreator"
APP_VERSION="${1:-dev}"
APP_DIR="${APP_NAME}.AppDir"
APP_COMMENT="Create server packs from Minecraft Forge, NeoForge, Fabric, Quilt or LegacyFabric modpacks."
APP_CATEGORIES="Utility;FileTools;Java;"
APP_ARGS="-Dfile.encoding=UTF-8 -Dlog4j2.formatMsgNoLookups=true -DServerPackCreator -Dname=ServerPackCreator -Dspring.application.name=ServerPackCreator -Dcom.apple.mrj.application.apple.menu.about.name=ServerPackCreator"
APP_MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
GRADLE_TASK="build"
GRADLE_ARGS="--info --full-stacktrace --warning-mode all -x :serverpackcreator-api:test -x :serverpackcreator-app:test"
BUILD_DIR="serverpackcreator-app/build/libs"
JDK_VERSION="21"

if [ "$BUILD_ARCH" = "x86_64" ]; then
    JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
    JDK_DIR="jdk-${JDK_VERSION}-${BUILD_ARCH}"
    APPIMAGE_ARCH=${BUILD_ARCH}
elif [ "$BUILD_ARCH" = "aarch64" ]; then
    JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/aarch64/jdk/hotspot/normal/eclipse"
    JDK_DIR="jdk-${JDK_VERSION}-${BUILD_ARCH}"
    APPIMAGE_ARCH=${BUILD_ARCH}
fi

cleanup() {
  echo -e "${YELLOW}Cleaning up temporary files...${NC}"
  rm -rf "$APP_DIR"
  rm -f Dockerfile.appimage "${APP_DIR}.tar.gz"
  echo -e "${GREEN}Cleanup done.${NC}"
}

trap cleanup EXIT

echo -e "${GREEN}Build-Version: ${APP_VERSION}${NC}"
echo ""
echo -e "${GREEN}=== Build-Process started ===${NC}"

echo -e "${YELLOW}Checking Dependencies...${NC}"

if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Gradle-Wrapper (gradlew) not found!${NC}"
    echo -e "${YELLOW}Ensure it's present in the root-dir of the project.${NC}"
    exit 1
fi

chmod +x ./gradlew

if [ "$MACHINE" = "Linux" ]; then
    if ! command -v appimagetool &> /dev/null; then
        echo -e "${YELLOW}appimagetool not found. Downloading...${NC}"

        if [ "$BUILD_ARCH" = "x86_64" ]; then
            APPIMAGETOOL_URL="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
        elif [ "$BUILD_ARCH" = "aarch64" ]; then
            APPIMAGETOOL_URL="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-aarch64.AppImage"
        fi

        if command -v wget &> /dev/null; then
            wget -O appimagetool "$APPIMAGETOOL_URL"
        elif command -v curl &> /dev/null; then
            curl -L -o appimagetool "$APPIMAGETOOL_URL"
        else
            echo -e "${RED}Neither wget nor curl was found. Please supply either one.${NC}"
            exit 1
        fi

        chmod +x appimagetool
        APPIMAGETOOL="./appimagetool"
    else
        APPIMAGETOOL="appimagetool"
    fi
elif [ "$MACHINE" = "Mac" ]; then
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Docker not found!${NC}"
        echo -e "${YELLOW}Docker is needed on MacOS in order to build the AppImage.${NC}"
        exit 1
    fi
    echo -e "${GREEN}Docker found - Will be used for AppImage-build.${NC}"

    if [ "$BUILD_ARCH" = "aarch64" ]; then
        echo -e "${YELLOW}Building for aarch64(arm64).${NC}"
    fi
fi

echo -e "${YELLOW}Checking Java 21 JDK...${NC}"
if [ ! -d "$JDK_DIR" ]; then
    echo -e "${YELLOW}Downloading Java 21 JDK...${NC}"

    if command -v wget &> /dev/null; then
        wget -O jdk.tar.gz "$JDK_URL"
    elif command -v curl &> /dev/null; then
        curl -L -o jdk.tar.gz "$JDK_URL"
    else
        echo -e "${RED}Neither wget nor curl was found. Please supply either one.${NC}"
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

echo -e "${YELLOW}Checking if JAR already exists...${NC}"
EXISTING_JAR=$(find "$BUILD_DIR" -name "*.jar" ! -name "*-javadoc.jar" ! -name "*-sources.jar" ! -name "*-plain.jar" 2>/dev/null | head -n 1)

if [ -n "$EXISTING_JAR" ]; then
    echo -e "${GREEN}JAR already present: $EXISTING_JAR${NC}"
    echo -e "${YELLOW}Skipping Gradle-Build. For rebuild, delete $BUILD_DIR/*.jar${NC}"
    JAR_FILE="$EXISTING_JAR"
else
    echo -e "${YELLOW}No JAR found. Building using Gradle Wrapper...${NC}"
    ./gradlew clean --info --full-stacktrace
    ./gradlew $GRADLE_TASK -Pversion=$APP_VERSION $GRADLE_ARGS --info --full-stacktrace

    JAR_FILE=$(find "$BUILD_DIR" -name "*.jar" ! -name "*-javadoc.jar" ! -name "*-sources.jar" ! -name "*-plain.jar" | head -n 1)

    if [ -z "$JAR_FILE" ]; then
        echo -e "${RED}No JAR-file found in $BUILD_DIR${NC}"
        exit 1
    fi

    echo -e "${GREEN}JAR created: $JAR_FILE${NC}"
fi

echo -e "${GREEN}JAR found: $JAR_FILE${NC}"

echo -e "${YELLOW}Creating AppImage-Structure...${NC}"
mkdir -p "$APP_DIR/usr/bin"
mkdir -p "$APP_DIR/usr/lib"
mkdir -p "$APP_DIR/usr/share/applications"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/512x512/apps"
mkdir -p "$APP_DIR/usr/share/icons/hicolor/scalable/apps"
mkdir -p "$APP_DIR/usr/share/metainfo"

echo -e "${YELLOW}Copying Java JDK into AppImage...${NC}"
cp -rp "$JDK_DIR" "$APP_DIR/usr/lib/jdk"
chmod +x "$APP_DIR/usr/lib/jdk/bin/"* 2>/dev/null || true
echo -e "${GREEN}JDK copied (Size: $(du -sh "$APP_DIR/usr/lib/jdk" | cut -f1))${NC}"

if [ ! -f "$APP_DIR/usr/lib/jdk/bin/java" ]; then
    echo -e "${RED}ERROR: JDK was not correctly copied!${NC}"
    echo -e "${YELLOW}Check $APP_DIR/usr/lib/${NC}"
    ls -la "$APP_DIR/usr/lib/" 2>&1 || true
    exit 1
fi
echo -e "${GREEN}✓ JDK exists in APP_DIR${NC}"

cp "$JAR_FILE" "$APP_DIR/usr/lib/${APP_NAME}.jar"

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

# Function to check Java version
check_java_version() {
    local java_cmd="\$1"
    if [ ! -x "\$java_cmd" ]; then
        return 1
    fi

    # Get Java version
    local version_output=\$("\$java_cmd" -version 2>&1)
    local java_version=\$(echo "\$version_output" | grep -oP '(?<=version ")([0-9]+)' | head -1)

    if [ -z "\$java_version" ]; then
        # Try alternative format (OpenJDK)
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

# Try bundled JDK first
JAVA=""
if [ -f "\$BUNDLED_JAVA" ] && [ -x "\$BUNDLED_JAVA" ]; then
    if check_java_version "\$BUNDLED_JAVA"; then
        JAVA="\$BUNDLED_JAVA"
        if [ "\$DEBUG" = "1" ]; then
            echo "✓ Using bundled JDK: \$JAVA"
        fi
    fi
elif [ -f "\$BUNDLED_JAVA" ] && [ ! -x "\$BUNDLED_JAVA" ]; then
    # Try to fix permissions
    chmod +x "\$BUNDLED_JAVA" 2>/dev/null || true
    if [ -x "\$BUNDLED_JAVA" ] && check_java_version "\$BUNDLED_JAVA"; then
        JAVA="\$BUNDLED_JAVA"
        if [ "\$DEBUG" = "1" ]; then
            echo "✓ Using bundled JDK (fixed permissions): \$JAVA"
        fi
    fi
fi

# Fallback to system Java if bundled JDK not available
if [ -z "\$JAVA" ]; then
    if [ "\$DEBUG" = "1" ]; then
        echo "⚠ Bundled JDK not available, checking for system Java..."
    fi

    # Check if java is in PATH
    if command -v java &> /dev/null; then
        SYSTEM_JAVA="\$(command -v java)"
        if check_java_version "\$SYSTEM_JAVA"; then
            JAVA="\$SYSTEM_JAVA"
            echo "⚠ Warning: Using system Java instead of bundled JDK"
            echo "  Location: \$JAVA"
            echo -n "  Version: "
            \$JAVA -version 2>&1 | head -1
        else
            echo "✗ Error: System Java found but version is too old (requires Java \$REQUIRED_JAVA_VERSION+)"
            echo -n "  Found: "
            \$SYSTEM_JAVA -version 2>&1 | head -1
        fi
    fi
fi

# Final check - no compatible Java found
if [ -z "\$JAVA" ]; then
    echo "========================================"
    echo "ERROR: No compatible Java installation found!"
    echo "========================================"
    echo ""
    echo "This application requires Java \$REQUIRED_JAVA_VERSION or higher."
    echo ""
    echo "Bundled JDK Status:"
    if [ -d "\$APPDIR/usr/lib/jdk" ]; then
        echo "  ✗ JDK directory exists but java binary not found/executable"
        echo "    Expected: \$BUNDLED_JAVA"
        if [ -f "\$BUNDLED_JAVA" ]; then
            echo "    File exists: Yes"
            echo "    Permissions: \$(ls -l "\$BUNDLED_JAVA" | awk '{print \$1}')"
        else
            echo "    File exists: No"
        fi
    else
        echo "  ✗ JDK directory does not exist: \$APPDIR/usr/lib/jdk"
    fi
    echo ""
    echo "System Java Status:"
    if command -v java &> /dev/null; then
        echo "  ✗ Found: \$(command -v java)"
        echo -n "    Version: "
        java -version 2>&1 | head -1
        echo "    (Too old - requires Java \$REQUIRED_JAVA_VERSION+)"
    else
        echo "  ✗ No 'java' command found in PATH"
    fi
    echo ""
    echo "Solutions:"
    echo "  1. Install Java \$REQUIRED_JAVA_VERSION or higher:"
    echo "     Ubuntu/Debian: sudo apt install openjdk-21-jre"
    echo "     Fedora:        sudo dnf install java-21-openjdk"
    echo "     Arch:          sudo pacman -S jre-openjdk"
    echo "  2. Re-download the AppImage (bundled JDK may be corrupted)"
    echo "  3. Run with DEBUG=1 for more information"
    echo ""
    exit 1
fi

# Launch application
exec "\$JAVA" ${APP_ARGS} -jar "\$APPDIR/usr/lib/${APP_NAME}.jar" "\$@"
LAUNCHER_EOF

chmod +x "$APP_DIR/usr/bin/${APP_NAME}"

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

cp img/app_256x256.png  "$APP_DIR/usr/share/icons/hicolor/256x256/apps/${APP_NAME}.png"
cp img/app.png          "$APP_DIR/usr/share/icons/hicolor/512x512/apps/${APP_NAME}.png"
cp img/app.svg          "$APP_DIR/usr/share/icons/hicolor/scalable/apps/${APP_NAME}.svg"
cp "$APP_DIR/usr/share/icons/hicolor/256x256/apps/${APP_NAME}.png" "$APP_DIR/${APP_NAME}.png" 2>/dev/null || true
cp "$APP_DIR/usr/share/applications/${APP_NAME}.desktop" "$APP_DIR/${APP_NAME}.desktop"
cp misc/appdata.xml "$APP_DIR/usr/share/metainfo/de.griefed.ServerPackCreator.appdata.xml"

cat > "$APP_DIR/AppRun" << EOF
#!/bin/bash
APPDIR="\$(cd "\$(dirname "\$(readlink -f "\$0")")" && pwd)"

# Debug: If APPDIR is empty
if [ -z "\$APPDIR" ]; then
    echo "ERROR: APPDIR could not be computed"
    exit 1
fi

# Debug-Mode
if [ "\$DEBUG" = "1" ]; then
    echo "DEBUG AppRun:"
    echo "  \$0 = \$0"
    echo "  readlink = \$(readlink -f "\$0")"
    echo "  dirname = \$(dirname "\$(readlink -f "\$0")")"
    echo "  APPDIR = \$APPDIR"
    echo "  Target = \$APPDIR/usr/bin/${APP_NAME}"
fi

export PATH="\$APPDIR/usr/bin:\$PATH"
export LD_LIBRARY_PATH="\$APPDIR/usr/lib:\$LD_LIBRARY_PATH"

# Check if launcher exists
if [ ! -f "\$APPDIR/usr/bin/${APP_NAME}" ]; then
    echo "ERROR: Launcher not found: \$APPDIR/usr/bin/${APP_NAME}"
    echo "APPDIR contents:"
    ls -la "\$APPDIR" 2>&1 || true
    exit 1
fi

exec "\$APPDIR/usr/bin/${APP_NAME}" "\$@"
EOF

chmod +x "$APP_DIR/AppRun"

echo -e "${YELLOW}Creating AppImage for ${APPIMAGE_ARCH} (this can take a while)...${NC}"
rm -f ${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage

if [[ "$BUILD_ARCH" = "aarch64" || "$BUILD_ARCH" = "arm64" ]]; then
    DOCKER_PLATFORM="linux/arm64"
    APPIMAGETOOL_ARCH="aarch64"
    echo -e "${YELLOW}Building for aarch64 (ARM64)...${NC}"
else
    DOCKER_PLATFORM="linux/amd64"
    APPIMAGETOOL_ARCH="x86_64"
    echo -e "${YELLOW}Building for x86_64 (AMD64)...${NC}"
fi

cat > Dockerfile.appimage << DOCKERFILE_EOF
FROM --platform=${DOCKER_PLATFORM} ubuntu:22.04
RUN apt-get update && apt-get install -y \
  wget file libglib2.0-0 libfuse2 libcairo2 \
  libpango-1.0-0 libgdk-pixbuf2.0-0 desktop-file-utils \
  && rm -rf /var/lib/apt/lists/*
RUN wget -O /usr/local/bin/appimagetool https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-${APPIMAGETOOL_ARCH}.AppImage && \
  chmod +x /usr/local/bin/appimagetool && \
  cd /usr/local/bin && \
  ./appimagetool --appimage-extract && \
  mv squashfs-root appimagetool-dir && \
  rm appimagetool && \
  ln -s /usr/local/bin/appimagetool-dir/AppRun /usr/local/bin/appimagetool
WORKDIR /work
DOCKERFILE_EOF

DOCKER_IMAGE_NAME="appimage-builder-${BUILD_ARCH}"
if ! docker images | grep -q "$DOCKER_IMAGE_NAME"; then
    echo -e "${YELLOW}Building Docker image for AppImage creation (${BUILD_ARCH})...${NC}"
    docker build --platform=${DOCKER_PLATFORM} -t $DOCKER_IMAGE_NAME -f Dockerfile.appimage .
else
    echo -e "${GREEN}Using cached Docker image: $DOCKER_IMAGE_NAME${NC}"
fi

echo -e "${YELLOW}Packing APP_DIR for Docker-Transfer...${NC}"
echo -e "${YELLOW}APP_DIR contents before packaging:${NC}"
ls -lh "$APP_DIR/usr/lib/" | head -5

tar -czf "${APP_DIR}.tar.gz" "$APP_DIR"
echo -e "${GREEN}Archive created: $(du -sh "${APP_DIR}.tar.gz" | cut -f1)${NC}"

echo -e "${YELLOW}Checking if JDK is present in archive...${NC}"
if tar -tzf "${APP_DIR}.tar.gz" | grep -q "jdk/bin/java"; then
    echo -e "${GREEN}✓ JDK is present in tar-archive${NC}"
else
    echo -e "${RED}✗ ERROR: JDK is NOT present in tar-archive!${NC}"
    exit 1
fi

echo -e "${YELLOW}Running appimagetool in Docker container...${NC}"
docker run --rm --platform=${DOCKER_PLATFORM} \
    -v "$(pwd)/${APP_DIR}.tar.gz:/work/appdir.tar.gz" \
    -v "$(pwd):/output" \
    $DOCKER_IMAGE_NAME \
    sh -c "cd /work && tar -xzf appdir.tar.gz && cd /output && ARCH=${APPIMAGE_ARCH} appimagetool /work/${APP_DIR} ${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage"

rm -f "${APP_DIR}.tar.gz"

echo -e "${YELLOW}Verifying AppImage-Contents...${NC}"
if [ -f "${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage" ]; then
    ./${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage --appimage-extract >/dev/null 2>&1
    if [ -f "squashfs-root/usr/lib/jdk/bin/java" ]; then
        echo -e "${GREEN}✓ JDK is present in tar-archive${NC}"
    else
        echo -e "${RED}✗ ERROR: JDK is NOT present in tar-archive!${NC}"
        echo -e "${YELLOW}Checking squashfs-root/usr/lib/${NC}"
        ls -la squashfs-root/usr/lib/ 2>&1 || true
    fi
    rm -rf squashfs-root
else
    echo -e "${RED}✗ AppImage was not created!${NC}"
    exit 1
fi

echo -e "${GREEN}=== Done! ===${NC}"
echo -e "${GREEN}AppImage created: ${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage${NC}"
APPIMAGE_SIZE=$(du -h "${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage" | cut -f1)
echo -e "${GREEN}Site: ${APPIMAGE_SIZE}${NC}"
echo -e "${GREEN}Architecture: ${APPIMAGE_ARCH}${NC}"
echo -e "${YELLOW}Test with: ./${APP_NAME}-${APP_VERSION}-${APPIMAGE_ARCH}.AppImage${NC}"
echo -e "${YELLOW}FYI: This AppImage contains Java 21 and does not need a separate Java installation!${NC}"
