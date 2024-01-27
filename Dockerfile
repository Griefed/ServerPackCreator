FROM eclipse-temurin:21-jdk-jammy as builder

ARG VERSION=dev

RUN mkdir -p /tmp/serverpackcreator/java
WORKDIR /tmp/serverpackcreator
COPY / /tmp/serverpackcreator

RUN \
  apt-get update && apt-get install dos2unix -y && \
  dos2unix gradlew && chmod +x gradlew && \
  sh gradlew -Pversion="$VERSION" \
    build --info --full-stacktrace \
    -x :serverpackcreator-api:jvmTest -x :serverpackcreator-web:test && \
  wget -O zulu21.tar.gz https://cdn.azul.com/zulu/bin/zulu21.30.15-ca-jdk21.0.1-linux_x64.tar.gz && \
  tar -xvf zulu21.tar.gz -C /tmp/serverpackcreator/java --strip-components=1

FROM ghcr.io/linuxserver/baseimage-ubuntu:jammy-version-d74de700

ARG VERSION=dev

ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

LABEL maintainer="Griefed <griefed@griefed.de>"
LABEL source="https://github.com/Griefed/ServerPackCreator"
LABEL license="GNU LGPLv2.1"
LABEL issues="https://github.com/Griefed/ServerPackCreator/issues"
LABEL help="https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help"
LABEL support="https://discord.griefed.de"
LABEL homepage="https://serverpackcreator.de"
LABEL version=$VERSION
LABEL description="Create a server pack from a Minecraft Forge, NeoForge, Fabric, LegacyFabric or Quilt modpack!"

RUN \
  echo "**** Bring system up to date ****" && \
  apt-get update && apt-get upgrade -y && \
  apt-get install -y \
    libatomic1 && \
  echo "**** Creating our folder(s) ****" && \
  mkdir -p \
    /app/serverpackcreator/java && \
  echo "**** Cleanup ****" && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY root/ /
COPY --from=builder /tmp/serverpackcreator/java/ /app/serverpackcreator/java
COPY --from=builder /tmp/serverpackcreator/serverpackcreator-app/build/libs/serverpackcreator-app-$VERSION.jar /app/serverpackcreator/serverpackcreator.jar

EXPOSE 8080