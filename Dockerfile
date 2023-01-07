FROM ghcr.io/griefed/baseimage-ubuntu-jdk17-kotlin:1.0.0

ARG VERSION=dev

ENV S6_BEHAVIOUR_IF_STAGE2_FAILS=2
ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

LABEL maintainer="Griefed <griefed@griefed.de>"
LABEL source="https://github.com/Griefed/ServerPackCreator"
LABEL license="GNU LGPLv2.1"
LABEL issues="https://github.com/Griefed/ServerPackCreator/issues"
LABEL help="https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help"
LABEL support="https://discord.griefed.de"
LABEL homepage="https://griefed.de/#/serverpackcreator"
LABEL version=$VERSION
LABEL description="Create a server pack from a Minecraft Forge, Fabric or Quilt modpack!"

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

COPY root/ /
COPY --chown=grfd:grfd serverpackcreator-app/build/libs/serverpackcreator-app-${VERSION}.jar /app/serverpackcreator/serverpackcreator.jar
COPY --chown=grfd:grfd serverpackcreator-api/src/jvmMain/resources/de/griefed/resources/server_files /defaults/server_files
COPY --chown=grfd:grfd serverpackcreator-api/src/jvmMain/resources/de/griefed/resources/serverpackcreator.conf /defaults/serverpackcreator.conf

VOLUME /data /server-packs

EXPOSE 8080