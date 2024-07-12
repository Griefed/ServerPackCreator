#!/bin/bash

SOURCE=${BASH_SOURCE[0]}
while [ -L "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
  SOURCE=$(readlink "$SOURCE")
  [[ $SOURCE != /* ]] && SOURCE=$DIR/$SOURCE # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
cd "${DIR}" >/dev/null 2>&1 || exit

VERSION=$1
VERSION="${VERSION:-dev}"
YEAR=$(date +%Y)
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
BASE=appimage
INPUT=${BASE}/input
DEST=${BASE}/dist
TEMP=${BASE}/temp
APPDIR=${BASE}/ServerPackCreator.AppDir

rm -rf ${BASE}/*
mkdir -p \
  ${INPUT} \
  ${DEST} \
  ${TEMP} \
  ${APPDIR}/usr/share/applications \
  ${APPDIR}/usr/icons/hicolor/256x256/apps \
  ${APPDIR}/usr/icons/hicolor/512x512/apps \
  ${APPDIR}/usr/icons/hicolor/scalable/apps \
  ${APPDIR}/usr/share/applications \
  ${APPDIR}/usr/share/metainfo

goback() {
  cd "${DIR}"
}

trap goback EXIT

cp -f \
  serverpackcreator-app/build/libs/serverpackcreator-app-${VERSION}.jar \
  ${INPUT}/serverpackcreator.jar

#
# CREATE IMAGE
#
jpackage \
  --app-version "${VERSION}" \
  --name "ServerPackCreator" \
  --copyright "Copyright (C) ${YEAR} Griefed" \
  --description "Create server packs from Minecraft Forge, NeoForge, Fabric, Quilt or LegacyFabric modpacks." \
  --vendor "Griefed" \
  --icon img/icon.png \
  --dest ${DEST} \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "-Dlog4j2.formatMsgNoLookups=true" \
  --java-options "-DServerPackCreator" \
  --java-options "-Dname=ServerPackCreator" \
  --java-options "-Dspring.application.name=ServerPackCreator" \
  --java-options "-Dcom.apple.mrj.application.apple.menu.about.name=ServerPackCreator" \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --main-jar "serverpackcreator.jar" \
  --input "${INPUT}/" \
  --runtime-image "${JAVA_HOME}" \
  --temp "${TEMP}" \
  --type "app-image" \
  --verbose

#
# CREATE APPIMAGE
#
{
  echo "#!/usr/bin/env xdg-open"
  echo "[Desktop Entry]"
  echo "Name=ServerPackCreator"
  echo "Name[en]=ServerPackCreator"
  echo "Comment=Create server packs from Minecraft modpacks."
  echo "Exec=ServerPackCreator"
  echo "Icon=ServerPackCreator"
  echo "Type=Application"
  echo "Categories=Utility;FileTools;Java;"
  echo "StartupWMClass=org.springframework.boot.loader.launch.JarLauncher"
} >>${APPDIR}/usr/share/applications/de.griefed.ServerPackCreator.desktop

cp -rf \
  ${DEST}/ServerPackCreator/* \
  ${APPDIR}

cp -f \
  misc/appdata.xml \
  ${APPDIR}/usr/share/metainfo/de.griefed.ServerPackCreator.appdata.xml

cp -f \
  img/app_256x256.png \
  ${APPDIR}/usr/icons/hicolor/256x256/apps/ServerPackCreator.png

cp -f \
  img/app.png \
  ${APPDIR}/usr/icons/hicolor/512x512/apps/ServerPackCreator.png

cp -f \
  img/app.svg \
  ${APPDIR}/usr/icons/hicolor/scalable/apps/ServerPackCreator.svg

cd ${APPDIR}

ln -s \
  bin/ServerPackCreator \
  AppRun

ln -s \
  usr/share/applications/de.griefed.ServerPackCreator.desktop \
  de.griefed.ServerPackCreator.desktop

ln -s \
  usr/icons/hicolor/512x512/apps/ServerPackCreator.png \
  ServerPackCreator.png

ln -s \
  usr/icons/hicolor/scalable/apps/ServerPackCreator.svg \
  ServerPackCreator.svg

cd ..

wget -c https://github.com/$(wget -q https://github.com/probonopd/go-appimage/releases/expanded_assets/continuous -O - | grep "appimagetool-.*-x86_64.AppImage" | head -n 1 | cut -d '"' -f 2)

mv \
  appimagetool-*.AppImage \
  appimagetool.AppImage

chmod +x \
  appimagetool.AppImage

export ARCH=x86_64
export PATH=./squashfs-root/usr/bin:${PATH}
export VERSION="${VERSION:-dev}"

./appimagetool.AppImage \
  --standalone \
  ./ServerPackCreator.AppDir

ls -hat

mv \
  ServerPackCreator*.AppImage \
  ServerPackCreator-${VERSION}-x86_64.AppImage || echo "Move not necessary."