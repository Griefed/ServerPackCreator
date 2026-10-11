FROM griefed/gitlab-ci-cd:2.8.0 AS builder

WORKDIR /tmp/serverpackcreator.de

# The sources are taken from the build context instead of being cloned back out
# of the forge. The clone was a GitLab-era workaround: it built whatever the
# remote branch happened to point at rather than the commit under test, and it
# only worked for a branch that was already pushed and anonymously readable.
# `.dockerignore` keeps the host's node_modules, a stale dist and the CI
# definitions out of the context.
COPY package.json package-lock.json ./
RUN npm install

COPY . .
RUN quasar build

FROM ghcr.io/linuxserver/nginx:1.22.1

LABEL maintainer="Griefed <griefed@griefed.de>"

RUN \
  rm -rf \
    /config/www && \
  echo "**** Cleanup ****" && \
  rm -rf \
    /root/.cache \
    /tmp/*

COPY --from=builder /tmp/serverpackcreator.de/dist/spa/ /config/www

EXPOSE 80 443
