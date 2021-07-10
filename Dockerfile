FROM griefed/baseimage-ubuntu-jdk-8:1.0.5 AS builder

RUN \
  git clone \
    https://github.com/Griefed/ServerPackCreator.git \
      /tmp/serverpackcreator && \
  chmod +x /tmp/serverpackcreator/gradlew* && \
  cd /tmp/serverpackcreator && \
  rm -Rf /tmp/serverpackcreator/src/test && \
  ./gradlew build && \
  ls -ahl ./build/libs/

FROM griefed/baseimage-ubuntu-jdk-8:1.0.5

ENV S6_BEHAVIOUR_IF_STAGE2_FAILS=2

LABEL maintainer="Griefed <griefed@griefed.de>"

RUN \
  echo "**** install dependencies and build tools and stuff ****" && \
  mkdir -p \
    /app/serverpackcreator && \
  echo "**** Cleanup ****" && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY --from=builder tmp/serverpackcreator/build/libs/serverpackcreator.jar /app/serverpackcreator/serverpackcreator.jar
COPY src/main/resources/de/griefed/resources/server_files /defaults/server_files
COPY root/ /

VOLUME /data
