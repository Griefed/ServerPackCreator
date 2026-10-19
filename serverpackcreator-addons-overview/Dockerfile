FROM griefed/gitlab-ci-cd:2.2.12 AS builder

ARG BRANCH_OR_TAG=main
ARG HOSTER=git.griefed.de

RUN \
  git clone \
    -b $BRANCH_OR_TAG \
      https://$HOSTER/Griefed/ServerPackCreator-Addons-Overview.git \
      /tmp/ServerPackCreator-Addons-Overview && \
  cd /tmp/ServerPackCreator-Addons-Overview && \
  npm install && \
  quasar build

FROM ghcr.io/linuxserver/nginx:1.22.1

LABEL maintainer="Griefed <griefed@griefed.de>"

RUN \
  rm -rf \
    /config/www && \
  echo "**** Cleanup ****" && \
    rm -rf \
      /root/.cache \
      /tmp/*

COPY --from=builder /tmp/ServerPackCreator-Addons-Overview/dist/spa/ /config/www

EXPOSE 80 443
