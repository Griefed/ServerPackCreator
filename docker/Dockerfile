FROM eclipse-temurin:21-jdk-jammy AS builder

ARG VERSION=dev

RUN mkdir -p /tmp/serverpackcreator/java
WORKDIR /tmp/serverpackcreator
COPY / /tmp/serverpackcreator

RUN \
  apt-get update && apt-get install dos2unix -y && \
  dos2unix gradlew && chmod +x gradlew && \
  sh gradlew -Pversion="$VERSION" \
    build --info --full-stacktrace \
    -x :serverpackcreator-api:test -x :serverpackcreator-app:test

FROM ghcr.io/linuxserver/baseimage-ubuntu:noble

ARG VERSION=dev

ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

LABEL maintainer="Griefed <griefed@griefed.de>"
LABEL source="https://github.com/Griefed/ServerPackCreator"
LABEL license="GNU LGPLv2.1"
LABEL issues="https://github.com/Griefed/ServerPackCreator/issues"
LABEL help="https://help.serverpackcreator.de/help-topic.html"
LABEL support="https://discord.griefed.de"
LABEL homepage="https://serverpackcreator.de"
LABEL version=$VERSION
LABEL description="Create a server pack from a Minecraft Forge, NeoForge, Fabric, LegacyFabric or Quilt modpack!"

RUN \
  echo "**** Bring system up to date ****" && \
  apt-get update && apt-get upgrade -y && \
  apt-get install -y \
    libatomic1 \
    openjdk-21-jdk-headless && \
  echo "**** Creating our folder(s) ****" && \
  mkdir -p \
    /app/serverpackcreator/java && \
  echo "**** Cleanup ****" && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY --chmod=777 docker/root /
COPY --from=builder --chmod=777 /tmp/serverpackcreator/serverpackcreator-app/build/libs/serverpackcreator-app-$VERSION.jar /app/serverpackcreator/serverpackcreator.jar

EXPOSE 8080