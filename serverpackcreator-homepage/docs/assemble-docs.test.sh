#!/usr/bin/env bash
#
# Executable guard for `assemble-docs.sh`.
#
#   docs/assemble-docs.test.sh
#
# RUNS THE SCRIPT rather than reading it, because the thing worth pinning is the tree it produces --
# which version lands where, which `latest` points at whom, and that a caller-supplied reference wins
# over the download. Those are the parts a one-character edit breaks silently and a site visitor finds.
#
# It needs no network and no Docker daemon: the release assets are served by a throwaway
# `http.server` on a loopback port, and the webhelp is a four-file zip built here. Everything lives in
# one scratch directory that the trap removes.
#
# Written in bash 3.2 dialect -- no `mapfile`, no associative arrays -- for the same reason
# `assemble-docs.sh` parses its manifest with node: a script that only runs on the CI image cannot be
# tested by the person changing it.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SCRIPT="$HERE/assemble-docs.sh"

PASSED=0
FAILED=0

# Report one assertion. Collects rather than exits, so a run names every broken expectation at once
# instead of one per invocation.
check() {
  local what="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    PASSED=$((PASSED + 1))
    printf '  ok   %s\n' "$what"
  else
    FAILED=$((FAILED + 1))
    printf '  FAIL %s\n' "$what"
  fi
}

# Assert that a file's contents are exactly what was expected -- used to prove a caller-supplied tree
# was kept rather than silently replaced by a download of the same shape.
check_contents() {
  local what="$1" file="$2" expected="$3"
  local actual
  actual="$(cat "$file" 2>/dev/null)"
  if [ "$actual" = "$expected" ]; then
    PASSED=$((PASSED + 1))
    printf '  ok   %s\n' "$what"
  else
    FAILED=$((FAILED + 1))
    printf '  FAIL %s (expected %s, got %s)\n' "$what" "$expected" "$actual"
  fi
}

WORK="$(mktemp -d)"
SERVER_PID=""
# The server is backgrounded, so it outlives a failed assertion unless something reaps it.
cleanup() {
  # `wait` inside the redirection as well as `kill`: without it the shell prints its own
  # "Terminated" job-control line after the summary, which reads like a failure and is not one.
  if [ -n "$SERVER_PID" ]; then
    { kill "$SERVER_PID" && wait "$SERVER_PID"; } 2>/dev/null
  fi
  rm -rf "$WORK"
}
trap cleanup EXIT

# ---------------------------------------------------------------------------------------------
# Fixtures: a webhelp zip and a release server carrying one API reference per version.
# ---------------------------------------------------------------------------------------------

# The real bundle is 62 files; what the script asserts on is `config.json`, so the fixture carries
# that plus enough to prove the whole archive is unpacked and not just the one file.
mkdir -p "$WORK/webhelp"
printf '{"productVersion":"fixture"}\n' > "$WORK/webhelp/config.json"
printf 'webhelp index\n'                > "$WORK/webhelp/index.html"
printf 'webhelp topic\n'                > "$WORK/webhelp/HELP.html"
(cd "$WORK/webhelp" && zip -qr "$WORK/webHelpSPCH2-all.zip" .)
WEBHELP_ZIP="$WORK/webHelpSPCH2-all.zip"

# The release layout the script fetches from: <base>/<version>/serverpackcreator-api-<version>-dokka-html.zip
make_release_asset() {
  local version="$1"
  mkdir -p "$WORK/releases/$version" "$WORK/api-$version"
  printf 'api %s\n' "$version" > "$WORK/api-$version/index.html"
  (cd "$WORK/api-$version" && zip -qr "$WORK/releases/$version/serverpackcreator-api-$version-dokka-html.zip" .)
}
make_release_asset 9.2.1
make_release_asset 9.1.0

# Port 0 lets the kernel choose, so two runs of this test cannot collide and no port is hard-coded.
python3 - "$WORK/releases" "$WORK/port" 2> "$WORK/server.log" <<'PYEOF' &
import sys, http.server, functools, socketserver
root, portfile = sys.argv[1], sys.argv[2]
handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=root)
class Quiet(socketserver.TCPServer):
    allow_reuse_address = True
with Quiet(("127.0.0.1", 0), handler) as httpd:
    with open(portfile, "w") as f:
        f.write(str(httpd.server_address[1]))
    httpd.serve_forever()
PYEOF
SERVER_PID=$!

# Waiting for the port file rather than sleeping a fixed amount: a CI host under load is exactly where
# a fixed sleep turns a working test into a flaky one.
for _ in $(seq 1 50); do
  [ -s "$WORK/port" ] && break
  sleep 0.1
done
if [ ! -s "$WORK/port" ]; then
  echo "assemble-docs.test: the fixture server never reported a port" >&2
  exit 1
fi
BASE="http://127.0.0.1:$(cat "$WORK/port")"

cat > "$WORK/versions.json" <<'JSONEOF'
{ "latest": "9.1.0", "versions": ["9.2.1", "9.1.0"] }
JSONEOF

# ---------------------------------------------------------------------------------------------
echo "case 1: an ordinary build -- no release in flight"
# ---------------------------------------------------------------------------------------------
OUT="$WORK/out1"
SPC_WEBHELP_ZIP="$WEBHELP_ZIP" SPC_RELEASE_BASE="$BASE" \
  "$SCRIPT" "$OUT" "$WORK/versions.json" > "$WORK/out1.log" 2>&1
check "the script succeeds"                       test "$?" = "0"
check "the webhelp is served once, at help/latest" test -s "$OUT/help/latest/config.json"
check "the whole bundle is unpacked, not one file" test -s "$OUT/help/latest/HELP.html"
check "api/9.2.1 is fetched"                      test -s "$OUT/api/9.2.1/index.html"
check "api/9.1.0 is fetched"                      test -s "$OUT/api/9.1.0/index.html"
check "api/latest resolves to the manifest latest" test -s "$OUT/api/latest/index.html"
check_contents "api/latest is 9.1.0" "$OUT/api/latest/index.html" "api 9.1.0"
# The help is no longer archived per version: the container image that used to hold each version's
# copy is gone, and the site links only /help/latest.
check "no per-version help tree is produced"      test ! -e "$OUT/help/9.1.0"
check "no per-version help tree for the other"    test ! -e "$OUT/help/9.2.1"

# ---------------------------------------------------------------------------------------------
echo "case 2: a release build -- the version being released is folded in and promoted"
# ---------------------------------------------------------------------------------------------
OUT="$WORK/out2"
# Seeded exactly as `homepage-image` seeds it from the dokka artifact: the release asset for the
# version being published does not exist yet, so this is the only source.
mkdir -p "$OUT/api/9.3.2"
printf 'seeded by the caller\n' > "$OUT/api/9.3.2/index.html"
SPC_WEBHELP_ZIP="$WEBHELP_ZIP" SPC_RELEASE_BASE="$BASE" \
SPC_RELEASE_VERSION=9.3.2 SPC_RELEASE_IS_LATEST=true \
  "$SCRIPT" "$OUT" "$WORK/versions.json" > "$WORK/out2.log" 2>&1
check "the script succeeds"                       test "$?" = "0"
check_contents "a caller-supplied api/<v> is kept, not refetched" \
  "$OUT/api/9.3.2/index.html" "seeded by the caller"
check_contents "api/latest follows the released version" \
  "$OUT/api/latest/index.html" "seeded by the caller"
check "the manifest versions are still served"    test -s "$OUT/api/9.1.0/index.html"
check "help/latest is this build's webhelp"       test -s "$OUT/help/latest/config.json"

# ---------------------------------------------------------------------------------------------
echo "case 3: a prerelease is browsable but never becomes latest"
# ---------------------------------------------------------------------------------------------
OUT="$WORK/out3"
mkdir -p "$OUT/api/9.4.0-alpha.1"
printf 'prerelease\n' > "$OUT/api/9.4.0-alpha.1/index.html"
SPC_WEBHELP_ZIP="$WEBHELP_ZIP" SPC_RELEASE_BASE="$BASE" \
SPC_RELEASE_VERSION=9.4.0-alpha.1 SPC_RELEASE_IS_LATEST=false \
  "$SCRIPT" "$OUT" "$WORK/versions.json" > "$WORK/out3.log" 2>&1
check "the script succeeds"                       test "$?" = "0"
check "the prerelease is browsable"               test -s "$OUT/api/9.4.0-alpha.1/index.html"
check_contents "api/latest still names the manifest latest" \
  "$OUT/api/latest/index.html" "api 9.1.0"

# ---------------------------------------------------------------------------------------------
echo "case 4: the failure directions"
# ---------------------------------------------------------------------------------------------
# A webhelp that was never handed over must stop the build. The site's own Dockerfile fails on a
# missing help/latest anyway, but failing here names the cause instead of the symptom.
OUT="$WORK/out4"
SPC_RELEASE_BASE="$BASE" "$SCRIPT" "$OUT" "$WORK/versions.json" > "$WORK/out4.log" 2>&1
check "an absent SPC_WEBHELP_ZIP fails the build"  test "$?" != "0"

OUT="$WORK/out5"
SPC_WEBHELP_ZIP="$WORK/nope.zip" SPC_RELEASE_BASE="$BASE" \
  "$SCRIPT" "$OUT" "$WORK/versions.json" > "$WORK/out5.log" 2>&1
check "a missing webhelp zip fails the build"      test "$?" != "0"

# `latest` naming a version the site does not serve is the manifest mistake worth catching early --
# it would otherwise ship a dangling /api/latest.
cat > "$WORK/bad.json" <<'JSONEOF'
{ "latest": "9.9.9", "versions": ["9.1.0"] }
JSONEOF
OUT="$WORK/out6"
SPC_WEBHELP_ZIP="$WEBHELP_ZIP" SPC_RELEASE_BASE="$BASE" \
  "$SCRIPT" "$OUT" "$WORK/bad.json" > "$WORK/out6.log" 2>&1
check "a latest outside versions[] fails the build" test "$?" != "0"

# A version whose release asset does not exist must fail loudly: a site that silently omits a version
# it advertises is worse than one that fails to build.
cat > "$WORK/missing.json" <<'JSONEOF'
{ "latest": "9.1.0", "versions": ["9.1.0", "0.0.0"] }
JSONEOF
OUT="$WORK/out7"
SPC_WEBHELP_ZIP="$WEBHELP_ZIP" SPC_RELEASE_BASE="$BASE" \
  "$SCRIPT" "$OUT" "$WORK/missing.json" > "$WORK/out7.log" 2>&1
check "an unpublished version fails the build"     test "$?" != "0"

echo
echo "assemble-docs.test: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
