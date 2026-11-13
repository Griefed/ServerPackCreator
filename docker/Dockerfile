FROM eclipse-temurin:25-jdk-jammy@sha256:5ba254809ef542504d40d47271c7d6c4f240fe40ca1386259806d431f13b6b4f AS builder

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

FROM ghcr.io/linuxserver/baseimage-ubuntu:noble@sha256:7150f35f13406e645b208ccebe38e45e15cf5c7f263616a4fc186e4f95834264

ARG VERSION=dev

ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

LABEL maintainer="Griefed <griefed@griefed.de>"
LABEL source="https://github.com/Griefed/ServerPackCreator"
LABEL license="GNU LGPLv2.1"
LABEL issues="https://github.com/Griefed/ServerPackCreator/issues"
LABEL help="https://serverpackcreator.de/help/latest/help-topic.html"
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