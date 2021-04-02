FROM openjdk:8-jdk-alpine3.9 AS builder

RUN \
  apk add --no-cache \
    git && \
  git clone \
    https://github.com/Griefed/ServerPackCreator.git \
      /tmp/serverpackcreator && \
  chmod +x /tmp/serverpackcreator/gradlew* && \
  cd /tmp/serverpackcreator && \
  ./gradlew build && \
  ls -ahl ./build/libs/

FROM lsiobase/alpine:3.12

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

COPY --from=builder ./build/libs/ServerPackCreator.jar /app/serverpackcreator/serverpackcreator.jar
COPY src/main/resources/server_files/ /defaults/server_files
COPY root/ /

VOLUME /data