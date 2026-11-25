# The JDK here must be the Gradle toolchain's major (gradle/libs.versions.toml `java`): the launcher JVM serves as
# the toolchain only when the majors match, and otherwise Gradle downloads one from foojay on every uncached build.
# auto-download=false below turns that drift into a build failure instead.
FROM eclipse-temurin:21-jdk-jammy@sha256:b2711c65970c1227197c29c46e669bfdbb137524e9da0e664703a140ec25a040 AS builder

ARG VERSION=dev

RUN mkdir -p /tmp/serverpackcreator/java
WORKDIR /tmp/serverpackcreator
COPY / /tmp/serverpackcreator

RUN \
  apt-get update && apt-get install dos2unix -y && \
  dos2unix gradlew && chmod +x gradlew && \
  sh gradlew -Pversion="$VERSION" -Porg.gradle.java.installations.auto-download=false \
    build --info --full-stacktrace \
    -x :serverpackcreator-api:test -x :serverpackcreator-app:test

FROM ghcr.io/linuxserver/baseimage-ubuntu:noble@sha256:3f3f186526ab9d1619234f30221e80ceb9ec37139d64d77afb42350cf8fb9732

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