FROM ghcr.io/griefed/baseimage-ubuntu-jdk-8:2.0.11 AS builder

ARG BRANCH_OR_TAG=webservice
ARG HOSTER=git.griefed.de
ARG VERSION=dev

RUN \
  apt-get update && apt-get upgrade -y && \
  apt-get install -y \
    libatomic1 && \
  git clone \
    -b $BRANCH_OR_TAG \
      https://$HOSTER/Griefed/ServerPackCreator.git \
        /tmp/serverpackcreator && \
  chmod +x /tmp/serverpackcreator/gradlew* && \
  cd /tmp/serverpackcreator && \
  rm -Rf /tmp/serverpackcreator/backend/test && \
  ./gradlew about installQuasar cleanFrontend assembleFrontend copyDist build -Pversion=$VERSION --info --full-stacktrace -x test && \
  ls -ahl ./build/libs/ && \
  mv \
    ./build/libs/serverpackcreator-$VERSION.jar \
    ./build/libs/serverpackcreator.jar && \
  ls -ahl ./build/libs/

FROM ghcr.io/griefed/baseimage-ubuntu-jdk-8:2.0.11

ARG VERSION=dev

ENV S6_BEHAVIOUR_IF_STAGE2_FAILS=2
ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

LABEL maintainer="Griefed <griefed@griefed.de>"
LABEL version=$VERSION
LABEL description="An app to create server packs from a given Minecraft Forge or Fabric modpack."

RUN \
  echo "**** Bring system up to date ****" && \
  apt-get update && apt-get upgrade -y && \
  apt-get install -y \
    libatomic1 && \
  echo "**** Creating our folder(s) ****" && \
  mkdir -p \
    /app/serverpackcreator && \
  mkdir \
    /server-packs && \
  echo "**** Cleanup ****" && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY --from=builder tmp/serverpackcreator/build/libs/serverpackcreator.jar /app/serverpackcreator/serverpackcreator.jar

COPY backend/main/resources/de/griefed/resources/server_files /defaults/server_files

COPY root/ /

VOLUME /data /server-packs

EXPOSE 8080