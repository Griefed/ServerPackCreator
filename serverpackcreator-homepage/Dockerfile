# The Quasar CLI comes from this tree's own lockfile, so the builder only has to supply Node. The
# version floor is real and is a hard exit, not a warning: @quasar/app-vite's node-version-check
# compares against 22.22.0 and calls process.exit(1), so a base image that moves backwards breaks
# this build with an error naming Node rather than Quasar.
FROM node:22-bookworm-slim@sha256:de170b39439eb0e5a3444ad7d60b015614a381e10fecae2e30ec9e735122f38d AS builder

WORKDIR /tmp/serverpackcreator.de

# The sources are taken from the build context instead of being cloned back out
# of the forge. The clone was a GitLab-era workaround: it built whatever the
# remote branch happened to point at rather than the commit under test, and it
# only worked for a branch that was already pushed and anonymously readable.
# `.dockerignore` keeps the host's node_modules, a stale dist and the CI
# definitions out of the context.
COPY package.json package-lock.json ./
# `ci`, not `install`: the lockfile is copied right above precisely so the build is reproducible.
RUN npm ci

COPY . .
# `npm run build`, not a bare `quasar build`: the script collects the download counts into
# src/generated/ before invoking Quasar, and npm puts node_modules/.bin first on PATH, so the CLI
# is the lockfile's own rather than whatever the base image happens to carry.
RUN npm run build

# This tag served the help site for a while in an image of its own, so the `/config/www` webroot this
# image writes to is proven on it, not assumed. Nothing here ships an nginx site-conf (`/api/latest`
# is a symlink, deliberately, because /config is a VOLUME seeded once), so the site-conf path move
# between LSIO generations does not reach this image.
FROM ghcr.io/linuxserver/nginx:1.30.4@sha256:c6c300bd5959e340d8742359f59604f3ad21426ea905cc77e75b01345034c8c6

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
