#!/usr/bin/env bash
#
# Executable guard for `assemble-docs.sh`.
#
#   docs/assemble-docs.test.sh
#
# RUNS THE SCRIPT rather than reading it, because the thing worth pinning is the tree it produces --
# what lands where, that a stale tree is replaced rather than merged into, and that every way of
# handing it a bad archive fails loudly. Those are the parts a one-character edit breaks silently
# and a site visitor finds.
#
# It needs no network, no Docker daemon and no credentials, which is the same claim the script makes
# for itself -- so a run of this test is also the evidence for that claim.
#
# Written in bash 3.2 dialect -- no `mapfile`, no associative arrays -- because a script that only
# runs on the CI image cannot be tested by the person changing it.
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

# Assert a file's exact contents -- used to prove which archive a tree came from, which `test -e`
# cannot distinguish.
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
trap 'rm -rf "$WORK"' EXIT

# ---------------------------------------------------------------------------------------------
# Fixtures. The real webhelp is 64 files and the real Dokka tree 1,411; what the script asserts on
# is one sentinel in each, so the fixtures carry that plus enough to prove the WHOLE archive is
# unpacked rather than just the file being looked for.
# ---------------------------------------------------------------------------------------------
mkdir -p "$WORK/webhelp" "$WORK/dokka"
printf '{"productVersion":"fixture"}\n' > "$WORK/webhelp/config.json"
printf 'webhelp index\n'                > "$WORK/webhelp/index.html"
printf 'webhelp topic\n'                > "$WORK/webhelp/HELP.html"
(cd "$WORK/webhelp" && zip -qr "$WORK/webHelpSPCH2-all.zip" .)
WEBHELP_ZIP="$WORK/webHelpSPCH2-all.zip"

printf 'dokka index\n'                  > "$WORK/dokka/index.html"
mkdir -p "$WORK/dokka/serverpackcreator-api"
printf 'dokka package page\n'           > "$WORK/dokka/serverpackcreator-api/index.html"
tar -czf "$WORK/dokka-html.tar.gz" -C "$WORK/dokka" .
DOKKA_TARGZ="$WORK/dokka-html.tar.gz"

run() { SPC_WEBHELP_ZIP="$1" SPC_DOKKA_TARGZ="$2" "$SCRIPT" "$3" > "$3.log" 2>&1; }

# ---------------------------------------------------------------------------------------------
echo "case 1: an ordinary build"
# ---------------------------------------------------------------------------------------------
OUT="$WORK/out1"
run "$WEBHELP_ZIP" "$DOKKA_TARGZ" "$OUT"
check "the script succeeds"                        test "$?" = "0"
check "the help lands at help/latest"              test -s "$OUT/help/latest/config.json"
check "the whole webhelp is unpacked"              test -s "$OUT/help/latest/HELP.html"
check "the reference lands at api/latest"          test -s "$OUT/api/latest/index.html"
check "the whole reference is unpacked"            test -s "$OUT/api/latest/serverpackcreator-api/index.html"
check_contents "api/latest is the dokka tree, not the webhelp" \
  "$OUT/api/latest/index.html" "dokka index"
# Both halves of the site Dockerfile's own gate, asserted here so a break is named by this test
# rather than by a `docker build` four minutes in.
check "the Dockerfile's help gate is satisfied"    test -e "$OUT/help/latest/config.json"
check "the Dockerfile's api gate is satisfied"     test -e "$OUT/api/latest/index.html"

# Nothing is versioned and nothing is archived: one image, one release, one copy of each.
check "no versioned help path is produced"         test ! -e "$OUT/help/9.2.1"
check "no versioned api path is produced"          test ! -e "$OUT/api/9.2.1"
check "neither latest is a symlink"                test ! -L "$OUT/api/latest"

# ---------------------------------------------------------------------------------------------
echo "case 2: a rebuild replaces the previous tree rather than merging into it"
# ---------------------------------------------------------------------------------------------
# A left-over page from an earlier version would otherwise be served forever, which is exactly the
# staleness this one-version-per-image design exists to make impossible.
printf 'from an older release\n' > "$OUT/help/latest/GONE.html"
printf 'from an older release\n' > "$OUT/api/latest/GONE.html"
run "$WEBHELP_ZIP" "$DOKKA_TARGZ" "$OUT"
check "the script succeeds"                        test "$?" = "0"
check "a stale help page does not survive"         test ! -e "$OUT/help/latest/GONE.html"
check "a stale api page does not survive"          test ! -e "$OUT/api/latest/GONE.html"
check "the help is still there afterwards"         test -s "$OUT/help/latest/config.json"

# ---------------------------------------------------------------------------------------------
echo "case 3: the failure directions"
# ---------------------------------------------------------------------------------------------
# Each archive must be REQUIRED, not defaulted: the site's Dockerfile fails on a missing tree
# anyway, and failing here names which input was not handed over.
OUT="$WORK/out3"; SPC_DOKKA_TARGZ="$DOKKA_TARGZ" "$SCRIPT" "$OUT" > "$OUT.log" 2>&1
check "an absent SPC_WEBHELP_ZIP fails the build"  test "$?" != "0"

OUT="$WORK/out4"; SPC_WEBHELP_ZIP="$WEBHELP_ZIP" "$SCRIPT" "$OUT" > "$OUT.log" 2>&1
check "an absent SPC_DOKKA_TARGZ fails the build"  test "$?" != "0"

OUT="$WORK/out5"; run "$WORK/nope.zip" "$DOKKA_TARGZ" "$OUT"
check "a missing webhelp zip fails the build"      test "$?" != "0"

OUT="$WORK/out6"; run "$WEBHELP_ZIP" "$WORK/nope.tar.gz" "$OUT"
check "a missing dokka tar.gz fails the build"     test "$?" != "0"

# An archive that unpacks WITHOUT its sentinel is the silent one: the extraction succeeds, the
# directory is non-empty, and the site ships 404s behind its own navigation.
mkdir -p "$WORK/wrong"
printf 'not a webhelp\n' > "$WORK/wrong/readme.txt"
(cd "$WORK/wrong" && zip -qr "$WORK/wrong-webhelp.zip" .)
tar -czf "$WORK/wrong-dokka.tar.gz" -C "$WORK/wrong" .

OUT="$WORK/out7"; run "$WORK/wrong-webhelp.zip" "$DOKKA_TARGZ" "$OUT"
check "a webhelp without config.json fails the build" test "$?" != "0"

OUT="$WORK/out8"; run "$WEBHELP_ZIP" "$WORK/wrong-dokka.tar.gz" "$OUT"
check "a reference without index.html fails the build" test "$?" != "0"

echo
echo "assemble-docs.test: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
