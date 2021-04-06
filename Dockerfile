FROM lsiobase/ubuntu:bionic AS builder

RUN \
  apt-get update && \
  apt-get install -y \
    git \
    openjdk-8-jdk && \
  git clone \
    https://github.com/Griefed/ServerPackCreator.git \
      /tmp/serverpackcreator && \
  chmod +x /tmp/serverpackcreator/gradlew* && \
  cd /tmp/serverpackcreator && \
  rm -Rf /tmp/serverpackcreator/src/test && \
  ./gradlew build && \
  ls -ahl ./build/libs/

FROM lsiobase/alpine:3.12

ENV S6_BEHAVIOUR_IF_STAGE2_FAILS=2

LABEL maintainer="Griefed <griefed@griefed.de>"

RUN \
  echo "**** install dependencies and build tools and stuff ****" && \
  apk add --no-cache \
    openjdk8-jre \
    curl && \
  mkdir -p \
    /app/serverpackcreator && \
  echo "**** Cleanup ****" && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY --from=builder tmp/serverpackcreator/build/libs/ServerPackCreator.jar /app/serverpackcreator/serverpackcreator.jar
COPY src/main/resources/server_files/ /defaults/server_files
COPY root/ /

VOLUME /data