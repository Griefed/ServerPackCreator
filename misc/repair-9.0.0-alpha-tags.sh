#!/usr/bin/env bash
# Repair the six 9.0.0-alpha.* tags and give the survivors their semantic-release
# channel notes.
#
#   ./repair-9.0.0-alpha-tags.sh            # check only, writes nothing, exits 1 on any problem
#   FJ_TOKEN=... ./repair-9.0.0-alpha-tags.sh --apply
#   FJ_TOKEN=... ./repair-9.0.0-alpha-tags.sh --apply --with-github   # also retarget on GitHub
#
# Written for bash 3.2, because that is what macOS ships and this runs on the
# operator's machine: no associative arrays, no mapfile.
#
# WHAT IS WRONG
#   All six 9.0.0-alpha.* tags point at 6cd6e9af3 ("RELEASE: 8.1.2", main's tip) on
#   origin, on GitHub, AND locally. The Forgejo remote also carries zero refs/notes/*,
#   so semantic-release cannot tell which channel any prerelease tag belongs to,
#   discards every alpha tag, resolves the last STABLE tag instead, and proposes
#   9.0.0-alpha.1 six alphas into the line.
#
# WHAT THIS DOES
#   1. Retargets 9.0.0-alpha.1 .. .5 onto the commits that actually produced them.
#   2. Deletes the ghost 9.0.0-alpha.6 (no RELEASE commit for it exists anywhere).
#   3. Writes {"channels":["alpha"]} for the five survivors and pushes the notes.
#
# ABOUT 9.0.0-alpha.6
#   It is not a mystery: GitHub's copy was created 2026-06-10 by a human account, two
#   MONTHS before 9.0.0-alpha.1 .. .5 (2026-08-02 .. 08-14, by github-actions[bot]).
#   It is the leftover of an earlier, abandoned 9.0.0 alpha line — no RELEASE commit
#   for it survives, and CHANGELOG.md on origin/alpha ends at .5. Forgejo's copy
#   (2026-08-17, empty target_commitish, i.e. created when the tag already existed) is
#   a later sync of that old GitHub release.
#   CONSEQUENCE WORTH KNOWING: after this repair the next release is computed as
#   9.0.0-alpha.6 again, so a version number that was published once in June is
#   republished with different contents. That is fine if the old one is deleted
#   everywhere — which this does — but anyone who downloaded the June assets has
#   different bits under the same version.
#
# WHERE THE TRUE TARGETS COME FROM
#   The commit graph, NOT the local tags. The local tags are wrong too: a
#   `git fetch origin --tags --force` overwrote the correct local tags with the
#   remote's broken ones, so a "force-push what you already have" repair no longer
#   works. Each target is found by its `RELEASE: 9.0.0-alpha.N` subject and then
#   cross-checked against the SHAs recorded in the repair document — if the graph and
#   that list ever disagree, this stops rather than guessing.
#
# WHY THE NOTES ARE WRITTEN AGAINST `^{commit}`
#   semantic-release reads notes with `git log --notes=refs/notes/semantic-release*`,
#   which resolves notes BY COMMIT. `git notes add <tag>` resolves <tag> with
#   rev-parse, which for an ANNOTATED tag is the tag object — so the note lands where
#   git log never looks, while `git notes show <tag>` still prints it and makes the
#   obvious verification pass on a broken repair. These five tags are lightweight
#   today (checked below); peeling is a no-op there and keeps this correct if that
#   ever changes.
#
# WHY THE GITHUB TAG PUSH IS OPT-IN
#   semantic-release only ever reads origin, so fixing Forgejo is the whole repair.
#   Pushing the five tags to GitHub is cosmetic — and not free. A tag push to GitHub
#   runs the workflows in the TREE OF THE PUSHED COMMIT, and the true targets
#   (050a50a5 ..) still carry .github/workflows/github-prerelease.yml, the GitLab-era
#   workflow that triggers on `*.*.*-alpha.N` tags, builds install4j media, republishes
#   the GitHub prerelease through softprops/action-gh-release and SCPs the assets to
#   the download host. Retargeting all five would set five of those running at once.
#   (Those same runs, as `github-actions[bot]`, are what created the GitHub releases
#   for .1 .. .5 on 2026-08-02 .. 08-14.) So: Forgejo by default; --with-github when
#   you have decided you want those builds, or after disabling Actions on the GitHub
#   repository. Deleting the ghost TAG on GitHub is still done either way — a deletion
#   raises a `delete` event, not a `push`, so no workflow fires for it.
#
# SAFETY
#   • Nothing is written without --apply; the check pass makes no network writes.
#   • Force is used ONLY on the five tag refs, and only after both the graph and the
#     document agree on every target.
#   • The ghost's Forgejo release id is re-read at run time, never trusted from notes.
#   • A rejected force-push is reported as tag protection rather than retried.
set -eu

APPLY=false
PUSH_GITHUB=false
while [ $# -gt 0 ]; do
  case "$1" in
    --apply)       APPLY=true ;;
    --with-github) PUSH_GITHUB=true ;;
    *) echo "usage: $0 [--apply] [--with-github]" >&2; exit 2 ;;
  esac
  shift
done

NOTES_REF="semantic-release"
CHANNEL='{"channels":["alpha"]}'
GHOST="9.0.0-alpha.6"
TAGS="9.0.0-alpha.1 9.0.0-alpha.2 9.0.0-alpha.3 9.0.0-alpha.4 9.0.0-alpha.5"
TAG_COUNT=5

problems=0
say()  { printf '%s\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$*"; problems=$((problems + 1)); }
note() { printf '  \033[33m·\033[0m %s\n' "$*"; }
die()  { printf '\033[31mSTOP:\033[0m %s\n' "$*" >&2; exit 1; }

# expected_sha is the repair document's list, used ONLY to cross-check the graph.
# It is never pushed directly.
expected_sha() {
  case "$1" in
    9.0.0-alpha.1) echo 050a50a51b3cda1d10a9120b86ddc954ad4ff951 ;;
    9.0.0-alpha.2) echo 5c00ee702cc95c87fe233337afd101451fa3ef59 ;;
    9.0.0-alpha.3) echo e3ab298844105bfd3b8268fce4ff6d88cf02e9a0 ;;
    9.0.0-alpha.4) echo 6878287949e0f1c641b38afefa140b4187bab5cb ;;
    9.0.0-alpha.5) echo dc5aed60354bd49187fa411acfe8b569fe986a69 ;;
    *)             echo "" ;;
  esac
}

# RESOLVED accumulates "<tag> <sha>" lines; target_of reads one back. This is the
# bash-3.2 stand-in for an associative array.
RESOLVED=""
target_of() {
  printf '%s\n' "$RESOLVED" | awk -v tag="$1" '$1 == tag { print $2; exit }'
}

# escape_dots makes a version safe inside a regex, so alpha.1 cannot match alpha.10.
escape_dots() { printf '%s' "$1" | sed 's/\./\\./g'; }

# remote_tag_sha prints the object a remote has for a tag, skipping the peeled
# `^{}` line so an annotated tag is compared like for like.
remote_tag_sha() {
  git ls-remote --tags "$1" "refs/tags/$2" 2>/dev/null | awk '$2 !~ /\^\{\}$/ { print $1; exit }'
}

# ── 0. Preconditions ────────────────────────────────────────────────────────────
say "── Preconditions"

git rev-parse --git-dir >/dev/null 2>&1 || die "not a git repository"
for remote in origin GitHub; do
  git remote get-url "$remote" >/dev/null 2>&1 || die "no '$remote' remote in this clone"
done
ok "origin  = $(git remote get-url origin)"
ok "GitHub  = $(git remote get-url GitHub)"

# Forgejo's URL path case need not match the API's, so this is overridable.
API_REPO="${API_REPO:-Griefed/ServerPackCreator}"
API="https://git.griefed.de/api/v1/repos/${API_REPO}"
ok "Forgejo API = $API   (override with API_REPO=owner/name)"

if [ -n "${FJ_TOKEN:-}" ]; then
  ok "FJ_TOKEN is set"
elif $APPLY; then
  die "FJ_TOKEN is not set — needed to delete the ghost release"
else
  note "FJ_TOKEN unset — fine for a check run"
fi

# Read-only, and makes every comparison below meaningful. Tags are fetched WITHOUT
# --force so this cannot itself overwrite a local ref; notes are fetched because
# their absence is half the bug.
git fetch --quiet origin || die "fetching origin failed"
git fetch --quiet GitHub || die "fetching GitHub failed"
git fetch --quiet origin 'refs/notes/*:refs/notes/*' 2>/dev/null || true
ok "fetched origin and GitHub (refs read, nothing overwritten)"

# ── 1. Resolve the true targets from the graph ──────────────────────────────────
say ""
say "── True targets, from the commit graph"

for tag in $TAGS; do
  subject_re="^RELEASE: $(escape_dots "$tag")\$"
  found=$(git log --all --grep="$subject_re" --format='%H')
  count=$(printf '%s' "$found" | grep -c . || true)
  if [ "$count" != "1" ]; then
    bad "$tag: found $count commits with subject 'RELEASE: $tag', need exactly 1"
    continue
  fi
  want=$(expected_sha "$tag")
  if [ "$found" != "$want" ]; then
    bad "$tag: graph says $(echo "$found" | cut -c1-9), document says $(echo "$want" | cut -c1-9) — resolve by hand"
    continue
  fi
  if ! git merge-base --is-ancestor "$found" origin/alpha; then
    bad "$tag: $(echo "$found" | cut -c1-9) is not an ancestor of origin/alpha"
    continue
  fi
  RESOLVED="$RESOLVED$tag $found
"
  ok "$tag -> $(echo "$found" | cut -c1-9)  (ancestor of origin/alpha)"
done

# The ghost must have no release commit anywhere; that is what makes deleting it safe.
if git log --all --grep="^RELEASE: $(escape_dots "$GHOST")\$" --format='%H' | grep -q .; then
  bad "$GHOST HAS a RELEASE commit — it is not a ghost, do NOT delete it"
else
  ok "$GHOST has no RELEASE commit anywhere (ghost confirmed)"
fi

# ── 2. Current state of every ref this touches ──────────────────────────────────
say ""
say "── Current state"

for tag in $TAGS $GHOST; do
  local_sha=$(git rev-parse --verify --quiet "refs/tags/$tag^{commit}" || echo '-')
  o_sha=$(remote_tag_sha origin "$tag"); [ -n "$o_sha" ] || o_sha='-'
  g_sha=$(remote_tag_sha GitHub "$tag"); [ -n "$g_sha" ] || g_sha='-'
  want=$(target_of "$tag"); [ -n "$want" ] || want='DELETE'
  printf '  %-16s local=%-10s origin=%-10s GitHub=%-10s want=%s\n' \
    "$tag" "$(echo "$local_sha" | cut -c1-9)" "$(echo "$o_sha" | cut -c1-9)" \
    "$(echo "$g_sha" | cut -c1-9)" "$(echo "$want" | cut -c1-9)"
done

# Lightweight vs annotated decides where a note has to be attached. Peeling handles
# both; this only reports what is there.
for tag in $TAGS; do
  if git rev-parse --verify --quiet "refs/tags/$tag" >/dev/null; then
    [ "$(git cat-file -t "$tag")" = "commit" ] || \
      note "$tag is an ANNOTATED tag — the peeled note below handles it"
  fi
done

# Anything else out of step, so this repair is not hiding a second problem.
# `continuous` is the rolling dev tag and is meant to have moved.
say ""
say "── Every other tag: local vs origin (expect none)"
mismatches=0
git ls-remote --tags origin | while read -r sha ref; do
  case "$ref" in *'^{}') continue ;; esac
  tag="${ref#refs/tags/}"
  case "$tag" in continuous|9.0.0-alpha.*) continue ;; esac
  local_sha=$(git rev-parse --verify --quiet "refs/tags/$tag^{commit}" || true)
  remote_commit=$(git rev-parse --verify --quiet "$sha^{commit}" 2>/dev/null || true)
  if [ -n "$local_sha" ] && [ -n "$remote_commit" ] && [ "$local_sha" != "$remote_commit" ]; then
    printf '  \033[33m·\033[0m MISMATCH %s: local %s vs origin %s\n' \
      "$tag" "$(echo "$local_sha" | cut -c1-9)" "$(echo "$remote_commit" | cut -c1-9)"
  fi
done > /tmp/spc-tag-mismatches.$$ || true
if [ -s /tmp/spc-tag-mismatches.$$ ]; then
  cat /tmp/spc-tag-mismatches.$$
  mismatches=$(grep -c MISMATCH /tmp/spc-tag-mismatches.$$ || true)
  bad "$mismatches other tag(s) differ — investigate before applying"
else
  ok "no other tag differs"
fi
rm -f /tmp/spc-tag-mismatches.$$

# ── 3. The ghost's Forgejo release, and any GitHub twin ─────────────────────────
say ""
say "── The ghost release"

if [ -n "${FJ_TOKEN:-}" ]; then
  ghost_json=$(curl -sS -H "Authorization: token $FJ_TOKEN" "$API/releases/tags/$GHOST" || true)
else
  ghost_json=$(curl -sS "$API/releases/tags/$GHOST" || true)
fi
GHOST_ID=$(printf '%s' "$ghost_json" | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin).get("id") or "")
except Exception:
    print("")' 2>/dev/null || true)
if [ -n "$GHOST_ID" ]; then
  printf '%s' "$ghost_json" | python3 -c 'import json,sys
d = json.load(sys.stdin)
print("  · Forgejo release id=%s  assets=%d  target_commitish=%r  created=%s"
      % (d["id"], len(d.get("assets") or []), d.get("target_commitish"), d.get("created_at")))'
  note "those assets are destroyed by the delete; the next 9.0.0-alpha.6 build replaces them"
else
  ok "no Forgejo release for $GHOST visible (already gone, or needs a token to see)"
fi

# A GitHub release for the ghost keeps repoman trying to sync it into Forgejo on
# every run. It cannot fabricate the tag any more, but it logs a deferral forever.
# NOT a blocker: it does not affect what semantic-release computes.
#
# Captured into a variable rather than piped into `grep -q`, which exits on the first
# match and SIGPIPEs curl ("curl: (56) Failure writing output to destination").
GH_GHOST_ID=""
gh_ghost_json="$(curl -sS "https://api.github.com/repos/Griefed/ServerPackCreator/releases/tags/$GHOST" || true)"
case "$gh_ghost_json" in
  *'"tag_name"'*)
    GH_GHOST_ID="$(printf '%s' "$gh_ghost_json" | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin).get("id") or "")
except Exception:
    print("")' 2>/dev/null || true)"
    printf '%s' "$gh_ghost_json" | python3 -c 'import json,sys
d = json.load(sys.stdin)
print("  · GitHub release id=%s  created=%s  author=%s  assets=%d"
      % (d.get("id"), d.get("created_at"), (d.get("author") or {}).get("login"),
         len(d.get("assets") or [])))'
    if [ -n "${GH_TOKEN:-}" ]; then
      note "GH_TOKEN is set — --apply will delete it"
    else
      note "set GH_TOKEN to have this deleted too, or afterwards:"
      note "  curl -X DELETE -H \"Authorization: token \$GH_TOKEN\" \\"
      note "    https://api.github.com/repos/Griefed/ServerPackCreator/releases/$GH_GHOST_ID"
    fi
    ;;
  *)
    ok "GitHub has no $GHOST release"
    ;;
esac

# ── 4. Verdict / apply ─────────────────────────────────────────────────────────
say ""
resolved_count=$(printf '%s' "$RESOLVED" | grep -c . || true)
if [ "$problems" -gt 0 ]; then
  die "$problems problem(s) above. Nothing was changed."
fi
if [ "$resolved_count" != "$TAG_COUNT" ]; then
  die "resolved $resolved_count of $TAG_COUNT targets. Nothing was changed."
fi

if ! $APPLY; then
  say "── Check passed. This is what --apply would run:"
  for tag in $TAGS; do
    say "  git tag -f $tag $(target_of "$tag" | cut -c1-9)"
  done
  refspecs=""
  for tag in $TAGS; do refspecs="$refspecs refs/tags/$tag"; done
  say "  git push --force origin$refspecs"
  if $PUSH_GITHUB; then
    say "  git push --force GitHub$refspecs   # will trigger github-prerelease.yml x5"
  else
    say "  (GitHub tag push SKIPPED — pass --with-github if you want it; see the header)"
  fi
  [ -n "$GHOST_ID" ] && say "  curl -X DELETE $API/releases/$GHOST_ID"
  say "  git push origin :refs/tags/$GHOST ; git push GitHub :refs/tags/$GHOST ; git tag -d $GHOST"
  for tag in $TAGS; do
    say "  git notes --ref=$NOTES_REF add -f -m '$CHANNEL' '$tag^{commit}'"
  done
  say "  git push origin refs/notes/$NOTES_REF"
  say ""
  say "Re-run with --apply when ready. FJ_TOKEN must be set."
  exit 0
fi

# ---- from here on, everything writes ----
say "── Applying"

# The release goes before the tag: Forgejo will not leave a release whose tag is
# gone, and deleting the tag underneath one produces a draft-shaped orphan.
if [ -n "$GHOST_ID" ]; then
  curl -fsS -X DELETE -H "Authorization: token $FJ_TOKEN" "$API/releases/$GHOST_ID" >/dev/null
  ok "deleted Forgejo release $GHOST_ID ($GHOST)"
fi
if [ -n "$GH_GHOST_ID" ] && [ -n "${GH_TOKEN:-}" ]; then
  if curl -fsS -X DELETE -H "Authorization: token $GH_TOKEN" \
       "https://api.github.com/repos/Griefed/ServerPackCreator/releases/$GH_GHOST_ID" >/dev/null; then
    ok "deleted GitHub release $GH_GHOST_ID ($GHOST)"
  else
    bad "could not delete the GitHub release $GH_GHOST_ID — do it by hand"
  fi
fi
for remote in origin GitHub; do
  if [ -n "$(remote_tag_sha "$remote" "$GHOST")" ]; then
    if git push "$remote" ":refs/tags/$GHOST" >/dev/null 2>&1; then
      ok "deleted $GHOST on $remote"
    else
      bad "could not delete $GHOST on $remote"
    fi
  else
    ok "$GHOST already absent on $remote"
  fi
done
if git tag -d "$GHOST" >/dev/null 2>&1; then ok "deleted local $GHOST"; else note "no local $GHOST"; fi

# Local first, so the pushes below push verified refs rather than whatever the last
# fetch left behind.
for tag in $TAGS; do
  git tag -f "$tag" "$(target_of "$tag")" >/dev/null
done
ok "retargeted the five tags locally"

refspecs=""
for tag in $TAGS; do refspecs="$refspecs refs/tags/$tag"; done
push_targets="origin"
$PUSH_GITHUB && push_targets="origin GitHub"
for remote in $push_targets; do
  # shellcheck disable=SC2086 -- refspecs is a deliberate word-split list of refs
  if git push --force "$remote" $refspecs >/dev/null 2>&1; then
    ok "force-pushed the five tags to $remote"
  else
    bad "force-push to $remote REJECTED — most likely tag protection on '9.0.0-*'. Relax it, re-run, restore it."
  fi
done
$PUSH_GITHUB || note "GitHub tags left as they are (no --with-github); semantic-release reads origin only"

# Peeled, per the header. -f so a re-run is idempotent.
for tag in $TAGS; do
  git notes --ref="$NOTES_REF" add -f -m "$CHANNEL" "$tag^{commit}" >/dev/null
done
ok "wrote channel notes for the five tags"
if git push origin "refs/notes/$NOTES_REF" >/dev/null 2>&1; then
  ok "pushed refs/notes/$NOTES_REF to origin"
else
  bad "pushing the notes to origin failed — without them semantic-release still regresses to 8.1.2"
fi

# ── 5. Verify the way semantic-release actually reads it ────────────────────────
say ""
say "── Verification"

for tag in $TAGS; do
  o_sha=$(remote_tag_sha origin "$tag")
  want=$(target_of "$tag")
  if [ "$o_sha" = "$want" ]; then
    ok "origin $tag -> $(echo "$want" | cut -c1-9)"
  else
    bad "origin $tag -> $(echo "$o_sha" | cut -c1-9), want $(echo "$want" | cut -c1-9)"
  fi
done
[ -z "$(remote_tag_sha origin "$GHOST")" ] && ok "$GHOST gone from origin" || bad "$GHOST still on origin"
[ -z "$(remote_tag_sha GitHub "$GHOST")" ] && ok "$GHOST gone from GitHub" || bad "$GHOST still on GitHub"

# THE check that matters: not `git notes show` (which passes even when the note is
# attached where git log never looks) but the query getTagsNotes actually runs.
say ""
say "  what semantic-release sees (git log --notes=refs/notes/$NOTES_REF*):"
git log --tags='*' --decorate-refs='refs/tags/*' --no-walk \
        --format='%d%x09%N' --notes="refs/notes/${NOTES_REF}*" \
  | grep -E '9\.0\.0-alpha' || note "no alpha tags in that output at all — that is a failure"

say ""
if [ "$problems" -gt 0 ]; then
  die "$problems problem(s) during apply — read them before running a release."
fi
cat <<'NEXT'
  ✓ Applied.

  Now prove it from a fresh clone, which is what the runner gets:

    rm -rf /tmp/spc-verify
    git clone https://git.griefed.de/Griefed/ServerPackCreator.git /tmp/spc-verify
    cd /tmp/spc-verify && git checkout alpha
    CI=true npx --yes -p semantic-release@24 -p @semantic-release/changelog@7 \
      -p @semantic-release/git@11 -p conventional-changelog-conventionalcommits@9 \
      semantic-release --dry-run

  Expect: "Found git tag 9.0.0-alpha.5 ... on branch alpha" and
          "The next release version is 9.0.0-alpha.6".
  8.1.2 reappearing means the notes did not reach origin.

  Two things this deliberately did NOT do:
    • the 9.0.0-alpha.1 .. .5 release pages keep the assets they were built with
      (from the 8.1.2 tree), and Forgejo may keep their old target_commitish;
    • the notes went to origin only. If anything ever runs semantic-release against
      the GitHub copy, it will regress there.
NEXT
