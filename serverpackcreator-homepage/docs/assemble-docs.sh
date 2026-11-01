#!/usr/bin/env bash
#
# Assemble the documentation tree the site serves.
#
#   SPC_WEBHELP_ZIP=<webHelpSPCH2-all.zip> SPC_DOKKA_TARGZ=<dokka-html.tar.gz> \
#     docs/assemble-docs.sh <output-root>
#
# produces:
#
#   <output-root>/help/latest/   the Writerside webhelp
#   <output-root>/api/latest/    the Dokka HTML API reference
#
# ONE VERSION, THE ONE THIS BUILD IS FOR. The site image is tagged per release, so an image already
# answers "which version is this?" and the documentation inside it is that version's by construction.
# Serving a reader anything older would mean an image carrying documentation for releases it is not.
#
# The site therefore holds no archive and fetches nothing. Both halves are handed over by whoever
# built them -- `docs.yml`'s `writerside` and `dokka` artifacts in CI -- which is what lets this run
# on a developer machine with no network, no Docker daemon and no credentials.
#
# A SCRIPT RATHER THAN DOCKERFILE STEPS, because this is the part worth testing: it can be run
# against a scratch directory and the result served and clicked, which a `RUN` buried in a
# multi-stage build cannot. `assemble-docs.test.sh` does exactly that.
set -euo pipefail

OUT="${1:?usage: SPC_WEBHELP_ZIP=<zip> SPC_DOKKA_TARGZ=<tar.gz> assemble-docs.sh <output-root>}"
# Both REQUIRED, and `:?` rather than a default on purpose: the site's own Dockerfile refuses to
# build without either tree, and failing here names the cause rather than the symptom.
WEBHELP_ZIP="${SPC_WEBHELP_ZIP:?SPC_WEBHELP_ZIP must point at the Writerside webhelp zip}"
DOKKA_TARGZ="${SPC_DOKKA_TARGZ:?SPC_DOKKA_TARGZ must point at the Dokka HTML tar.gz}"

# `unzip` is NOT a given on the Forgejo runner image -- it is why the dokka artifact is a tar.gz in
# the first place. python3 is the same fallback `docs.yml` already uses to read HelpTOC.json out of
# the webhelp zip, so this is the established pattern rather than a new one.
if command -v unzip >/dev/null; then
  extract_zip() { unzip -qq -o "$1" -d "$2"; }
elif command -v python3 >/dev/null; then
  extract_zip() { python3 -c 'import sys,zipfile; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])' "$1" "$2"; }
else
  echo "assemble-docs: neither unzip nor python3 is available to unpack the webhelp" >&2
  exit 1
fi

# Unpack one archive into a path of its own, replacing whatever was there. $1 archive, $2 target,
# $3 the file whose absence means the archive's layout moved -- asserted, because `COPY docs-build/`
# would happily ship a directory that unpacked empty and leave the site 404ing behind its own links.
unpack() {
  local archive="$1" target="$2" sentinel="$3"
  test -s "$archive" || { echo "assemble-docs: $archive is missing or empty" >&2; exit 1; }
  rm -rf "${target:?}"; mkdir -p "$target"
  case "$archive" in
    *.tar.gz|*.tgz) tar -xzf "$archive" -C "$target" ;;
    *)              extract_zip "$archive" "$target" ;;
  esac
  test -s "$target/$sentinel" || {
    echo "assemble-docs: $archive unpacked without $sentinel -- its layout moved" >&2
    exit 1
  }
  echo "assemble-docs: ${target#"$OUT"/} from $(basename "$archive") -- $(find "$target" -type f | wc -l | tr -d ' ') files"
}

mkdir -p "$OUT"
# `config.json` is the webhelp's own manifest; without it every page below it 404s at runtime.
unpack "$WEBHELP_ZIP"  "$OUT/help/latest" "config.json"
unpack "$DOKKA_TARGZ"  "$OUT/api/latest"  "index.html"

echo "assemble-docs: done -- $(du -sh "$OUT" | cut -f1)"
