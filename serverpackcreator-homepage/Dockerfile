# A Node + Quasar toolchain image. The name is a leftover from the GitLab era and is misleading --
# nothing about this build is GitLab CI -- but it is the image that has the toolchain, so it stays
# until something replaces it rather than being renamed in place.
FROM griefed/gitlab-ci-cd:2.8.0@sha256:1461ae150d5803774398cc89af820c5e4b4cca92ebd74e8a17171e386de7b56d AS builder

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

# This tag served the help site for a while in an image of its own, so the `/config/www` webroot this
# image writes to is proven on it, not assumed. Nothing here ships an nginx site-conf (`/api/latest`
# is a symlink, deliberately, because /config is a VOLUME seeded once), so the site-conf path move
# between LSIO generations does not reach this image.
FROM ghcr.io/linuxserver/nginx:1.30.4@sha256:3486bc71c5983edc9e7c80713b5d762f317c4c77fb31842a40a1ce26cce9f1de

LABEL maintainer="Griefed <griefed@griefed.de>"

RUN \
  rm -rf \
    /config/www && \
  echo "**** Cleanup ****" && \
  rm -rf \
    /root/.cache \
    /tmp/*

COPY --from=builder /tmp/serverpackcreator.de/dist/spa/ /config/www

# The documentation, assembled BEFORE this build by `docs/assemble-docs.sh`.
#
# Done in a script rather than a RUN here so it can be executed against a scratch directory and the
# result served and clicked -- which is what `docs/assemble-docs.test.sh` and `npm test` do, and
# what a RUN buried in a multi-stage build could not. The serving stage also has no `unzip`. So the
# script runs in the job, writes `docs-build/`, and this copies the result. `docs-build/.gitkeep` is
# committed so the COPY resolves even on a checkout where the script has not been run.
COPY docs-build/ /config/www/

# Fail HERE rather than serve a site whose /help and /api are 404s. A docs-less build is what you get
# by running `docker build` without assembling first, and the silent version of that is a site that
# advertises documentation it does not have.
RUN \
  if [ ! -e /config/www/help/latest/config.json ] || [ ! -e /config/www/api/latest/index.html ]; then \
    echo "No documentation in the image. Run docs/assemble-docs.sh docs-build before building." >&2; \
    exit 1; \
  fi

EXPOSE 80 443
