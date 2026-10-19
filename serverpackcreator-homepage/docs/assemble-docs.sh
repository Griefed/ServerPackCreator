#!/usr/bin/env bash
#
# Assemble the documentation tree the site serves.
#
#   SPC_WEBHELP_ZIP=<webHelpSPCH2-all.zip> docs/assemble-docs.sh <output-root> [versions.json]
#
# produces:
#
#   <output-root>/help/latest/      the Writerside webhelp -- ONE copy, this build's
#   <output-root>/api/<version>/    the Dokka HTML API reference, one per version in the manifest
#   <output-root>/api/latest        a symlink to the newest release's reference
#
# A SCRIPT RATHER THAN DOCKERFILE STEPS, because this is the part worth testing: it can be run
# against a scratch directory and the result served and clicked, which a `RUN` buried in a
# multi-stage build cannot. `assemble-docs.test.sh` does exactly that. The Dockerfile calls it and
# does nothing else with the docs.
#
# THE TWO HALVES ARE VERSIONED DIFFERENTLY, and that asymmetry is the point.
#
# The API reference is per-version, because it is a deliverable tied to a compile: somebody writing
# against `serverpackcreator-api` 9.1.0 needs 9.1.0's reference, and every release publishes one as
# a `serverpackcreator-api-<version>-dokka-html.zip` asset. `/api/<version>/` is the address that
# reference is linked and bookmarked by, so every version the manifest lists has to keep answering.
#
# The webhelp is NOT per-version. It is HELP.md and its sibling topics rendered as a browsable site
# -- user documentation, not an artifact tied to a compile -- and the site links exactly one entry
# point to it, `/help/latest`. So it is taken from the build that is running, and no archive of
# older renderings is kept anywhere.
set -euo pipefail

OUT="${1:?usage: SPC_WEBHELP_ZIP=<zip> assemble-docs.sh <output-root> [versions.json]}"
MANIFEST="${2:-$(dirname "$0")/versions.json}"
# The Writerside bundle, handed over by whoever built it -- the `writerside` job's artifact in CI, a
# local build or a downloaded one by hand. REQUIRED: the site's own Dockerfile refuses to build
# without a help tree, and failing here names the cause rather than the symptom.
WEBHELP_ZIP="${SPC_WEBHELP_ZIP:?SPC_WEBHELP_ZIP must point at the Writerside webhelp zip}"
# The release being published, which is never in the checked-in manifest -- that file is edited
# by hand and the tag is cut afterwards. Empty on an ordinary build.
RELEASE_VERSION="${SPC_RELEASE_VERSION:-}"
# Whether that release should become `/api/latest`. The CALLER decides, because `resolve-version`
# already answers it for the whole pipeline and a regex here would be a second copy of one question.
# Defaults to false, which is the conservative direction: a standalone run adds the version to the
# site without promoting it over whatever the manifest names.
RELEASE_IS_LATEST="${SPC_RELEASE_IS_LATEST:-false}"
RELEASES="${SPC_RELEASE_BASE:-https://git.griefed.de/Griefed/ServerPackCreator/releases/download}"

command -v curl >/dev/null || { echo "assemble-docs: curl is required" >&2; exit 1; }
command -v node >/dev/null || { echo "assemble-docs: node is required" >&2; exit 1; }

# `unzip` is NOT a given on the Forgejo runner image -- it is why the dokka job ships a tar.gz.
# python3 is the same fallback `docs.yml` already uses to read HelpTOC.json out of the webhelp zip,
# so this is the established pattern rather than a new one.
if command -v unzip >/dev/null; then
  extract() { unzip -qq -o "$1" -d "$2"; }
elif command -v python3 >/dev/null; then
  extract() { python3 -c 'import sys,zipfile; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])' "$1" "$2"; }
else
  echo "assemble-docs: neither unzip nor python3 is available to unpack an archive" >&2
  exit 1
fi

# Parsed with node, not python: node is already a hard dependency of this build (it is what
# compiles the SPA), so using it adds nothing, and `mapfile`/`read -a` are bash 4 builtins that a
# developer on macOS -- bash 3.2 -- does not have. A script that only runs in CI cannot be tested.
read_manifest() {
  node -e '
    const m = require(process.argv[1]);
    const released = process.argv[2] || "";
    const releasedIsLatest = process.argv[3] === "true";
    if (!m.latest || !Array.isArray(m.versions)) {
      console.error("assemble-docs: manifest needs a `latest` and a `versions` array");
      process.exit(1);
    }
    if (!m.versions.includes(m.latest)) {
      console.error(`assemble-docs: latest ${m.latest} is not in versions [${m.versions}]`);
      process.exit(1);
    }
    let latest = m.latest;
    let versions = m.versions.slice();
    if (released) {
      if (!versions.includes(released)) versions.unshift(released);
      // A prerelease is browsable but never `latest`, for the same reason it never becomes the
      // `:latest` image tag: `/api/latest` is what a reader reaches without asking for a version.
      // Which it is was decided by `resolve-version` and handed down, not re-derived here.
      if (releasedIsLatest) latest = released;
    }
    console.log(latest);
    console.log(versions.join(" "));
  ' "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")" "$2" "$3"
}

MANIFEST_OUT="$(read_manifest "$MANIFEST" "$RELEASE_VERSION" "$RELEASE_IS_LATEST")"
LATEST="$(printf '%s\n' "$MANIFEST_OUT" | sed -n 1p)"
VERSIONS="$(printf '%s\n' "$MANIFEST_OUT" | sed -n 2p)"

echo "assemble-docs: latest=$LATEST versions=$VERSIONS"
mkdir -p "$OUT/help" "$OUT/api"

fetch() {
  # $1 url, $2 destination file. A missing asset must stop the build: a site that silently omits a
  # version it advertises is worse than one that fails to build.
  local url="$1" dest="$2" code
  code="$(curl -sSL -o "$dest" -w '%{http_code}' --retry 3 --retry-delay 2 --max-time 300 "$url")"
  if [ "$code" != "200" ] || [ ! -s "$dest" ]; then
    echo "assemble-docs: $url answered HTTP $code" >&2
    return 1
  fi
}

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# The help, from this build's own bundle. A real directory rather than a symlink into a versioned
# one, because there is no versioned one: `/help/latest` is the only help URL the site offers.
test -s "$WEBHELP_ZIP" || { echo "assemble-docs: $WEBHELP_ZIP is missing or empty" >&2; exit 1; }
rm -rf "${OUT:?}/help/latest"; mkdir -p "$OUT/help/latest"
extract "$WEBHELP_ZIP" "$OUT/help/latest"
# `config.json` is the webhelp's own manifest. If it is absent the bundle layout moved and every
# page below it will 404 at runtime, which is exactly the failure worth catching at build time.
test -s "$OUT/help/latest/config.json"
echo "assemble-docs: help/latest from $(basename "$WEBHELP_ZIP") -- $(find "$OUT/help/latest" -type f | wc -l | tr -d ' ') files"

# Unquoted on purpose: this is the one place word splitting is wanted.
# shellcheck disable=SC2086
for V in $VERSIONS; do
  # An already-populated `api/<version>/` WINS over the release asset, and that is how the version
  # being released gets its reference at all: `release-build.yml` uploads the asset from a different
  # workflow on the same tag, so at this moment it may not exist yet. The caller unpacks `docs.yml`'s
  # own `dokka` artifact there instead -- the same tree, built from the same commit, one job earlier.
  # Older versions have no such artifact and are fetched.
  if [ -s "$OUT/api/$V/index.html" ]; then
    echo "assemble-docs: api/$V supplied by the caller, not fetching"
    continue
  fi
  echo "assemble-docs: api/$V"
  # The zip unpacks FLAT -- index.html sits at its root, there is no wrapper directory -- so it
  # unpacks straight into the versioned path.
  fetch "$RELEASES/$V/serverpackcreator-api-$V-dokka-html.zip" "$TMP/api-$V.zip"
  rm -rf "${OUT:?}/api/$V"; mkdir -p "$OUT/api/$V"
  extract "$TMP/api-$V.zip" "$OUT/api/$V"
  test -s "$OUT/api/$V/index.html"
done

# `/api/latest` is a SYMLINK, not an nginx redirect, and that is the whole reason this needs no nginx
# config at all. The site's base image declares `/config` a VOLUME and seeds its site-conf onto it
# ONCE, so a conf baked into the image is silently ignored by any deployment that already has one --
# the redirect would work on a fresh volume and quietly not work on the live host. A symlink is part
# of the content, so it ships wherever the content ships. nginx follows it by default
# (`disable_symlinks` is off), and `try_files $uri $uri/` resolves through it. `/help/latest` needs
# none of this: it is a real directory, because there is no versioned help for it to point at.
ln -sfn "$LATEST" "$OUT/api/latest"
test -s "$OUT/api/latest/index.html"

printf '%s\n' "$LATEST" > "$OUT/.docs-latest"
echo "assemble-docs: done -- $(du -sh "$OUT" | cut -f1)"
