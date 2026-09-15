# Repairing the 9.0.0-alpha tags and the missing semantic-release notes

**Status:** procedure written 2026-08-22, **not executed** — every step touches a remote, and this
repository's rule is that Claude never pushes. Run it yourself, in order, and check the verification
block at the end.

**Why this exists:** the `release-generate.yml` run on 2026-08-22 announced
`Found git tag 8.1.2 ... on branch alpha` and proposed `9.0.0-alpha.1`, six alphas into the 9.0.0 line.
Two independent defects, both diagnosed in `.claude/rules/ci-workflows.md` under *The release
pipeline's two silent killers*:

1. The Forgejo remote carries **375 tags and zero `refs/notes/*`**, so semantic-release cannot tell
   which channel any prerelease tag belongs to and discards all of them on a prerelease branch.
2. The six `9.0.0-alpha.*` tags on the remote all point at `6cd6e9af3` — the `RELEASE: 8.1.2` commit,
   which is `main`'s tip. For `.1` through `.5` the cause is on the record: the releases API still
   reports `"target_commitish": "main"` for them, and a forge mints a missing tag at its target. For
   `.6` the target is empty, meaning the tag already existed when its release was created; how that tag
   came to be on `6cd6e9af3` cannot be reconstructed from what the remote still holds, and this
   document does not guess. `release-build.yml` is not implicated either way — it posts no
   `target_commitish` to Forgejo at all.

A third fact drives the choice below: **`9.0.0-alpha.6` is a ghost.** There is no
`RELEASE: 9.0.0-alpha.6` commit anywhere in the repository and `CHANGELOG.md` on `origin/alpha` ends at
`9.0.0-alpha.5`, yet Forgejo has a published `9.0.0-alpha.6` release with 14 assets whose body contains
real `9.0.0-alpha.5...9.0.0-alpha.6` notes. The run that generated those notes never landed its
`@semantic-release/git` changelog commit, so as far as the git history is concerned that release does
not exist.

**Chosen remedy — honest repair.** Point `9.0.0-alpha.1` … `.5` at the commits that actually produced
them, delete the ghost `9.0.0-alpha.6` release and tag, and give the five surviving tags their channel
notes. The next release then comes out as **`9.0.0-alpha.6`**, computed from `dc5aed603`, with release
notes covering only the commits since `9.0.0-alpha.5` — which is exactly what `CHANGELOG.md` is missing.

## The true targets

| tag             | current target (both remotes) | true target — subject                      |
|-----------------|-------------------------------|--------------------------------------------|
| `9.0.0-alpha.1` | `6cd6e9af3`                   | `050a50a51b3cda1d10a9120b86ddc954ad4ff951`  |
| `9.0.0-alpha.2` | `6cd6e9af3`                   | `5c00ee702cc95c87fe233337afd101451fa3ef59`  |
| `9.0.0-alpha.3` | `6cd6e9af3`                   | `e3ab298844105bfd3b8268fce4ff6d88cf02e9a0`  |
| `9.0.0-alpha.4` | `6cd6e9af3`                   | `6878287949e0f1c641b38afefa140b4187bab5cb`  |
| `9.0.0-alpha.5` | `6cd6e9af3`                   | `dc5aed60354bd49187fa411acfe8b569fe986a69`  |
| `9.0.0-alpha.6` | `6cd6e9af3`                   | *none — delete*                             |

Each of those five commits has the subject `RELEASE: 9.0.0-alpha.N` and every one is an ancestor of
`origin/alpha`, both verified. Your **local** tags already point at them; only the remotes are wrong,
except for the local `9.0.0-alpha.6`, which was fetched from the broken remote.

Comparing all 375 remote tags against local finds exactly six differences: these five, plus
`continuous`, which is the rolling dev tag and is *supposed* to have moved. Nothing else needs
touching — re-run that comparison rather than trusting this paragraph:

```bash
git ls-remote --tags origin | grep -v '\^{}' | while read sha ref; do
  t=${ref#refs/tags/}
  [ "$(git rev-parse "$t^{commit}" 2>/dev/null)" = "$(git rev-parse "$sha^{commit}" 2>/dev/null)" ] \
    || echo "MISMATCH $t"
done
```

## Step 1 — delete the ghost release, then its tag

The release must go first: Forgejo will not leave a release pointing at a tag that no longer exists,
and deleting the tag underneath it produces a draft-shaped orphan. Assets are lost with it — that is
intended, the next `9.0.0-alpha.6` build replaces them.

```bash
# Confirm the release id. It was 1723 when this was written (created 2026-08-17T18:17:52+02:00);
# re-read it rather than trusting the number, since a delete is not undoable.
curl -sS "https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator/releases/tags/9.0.0-alpha.6" \
  | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d["id"], d["tag_name"], len(d["assets"]), "assets")'

# Delete it (substitute the id you just read)
curl -sS -X DELETE -H "Authorization: token $FJ_TOKEN" \
  "https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator/releases/<ID>"

# Then the tag, on both remotes and locally
git push origin :refs/tags/9.0.0-alpha.6
git push GitHub :refs/tags/9.0.0-alpha.6
git tag -d 9.0.0-alpha.6
```

## Step 2 — retarget alpha.1 … alpha.5 on the remotes

Your local tags are already correct, so this is a force-push of what you have. **If Forgejo has
protected tags configured for `9.0.0-*`, this is where it fails** — relax the rule, push, restore it.

```bash
git push --force origin \
  refs/tags/9.0.0-alpha.1 refs/tags/9.0.0-alpha.2 refs/tags/9.0.0-alpha.3 \
  refs/tags/9.0.0-alpha.4 refs/tags/9.0.0-alpha.5

git push --force GitHub \
  refs/tags/9.0.0-alpha.1 refs/tags/9.0.0-alpha.2 refs/tags/9.0.0-alpha.3 \
  refs/tags/9.0.0-alpha.4 refs/tags/9.0.0-alpha.5
```

A Forgejo push-mirror replicates refs, so the `GitHub` push may be redundant — do it anyway and let the
verification block below decide, because a mirror that has never been asked to force-update a tag is
not evidence that it will.

## Step 3 — write the channel notes and push them

This is the part that makes semantic-release see the tags at all. The payload is the same JSON
`addNote` writes itself: `{"channels":["<branch channel>"]}`, and for a prerelease branch the channel
defaults to the branch name.

```bash
for tag in 9.0.0-alpha.1 9.0.0-alpha.2 9.0.0-alpha.3 9.0.0-alpha.4 9.0.0-alpha.5; do
  git notes --ref=semantic-release add -f -m '{"channels":["alpha"]}' "$tag"
done
git push origin refs/notes/semantic-release
```

Nothing needs a note on `main`: `get-last-release.js` accepts any tag that is **not** a prerelease
regardless of its channels, which is why `8.1.2` was found in the first place. Verified in a scratch
repo — with `alpha` merged into `main` and only the alpha tags noted, `main` still resolves
`Found git tag 8.1.2 ... on branch main` and proposes `9.0.0`.

From here on the pipeline maintains the notes on its own: `index.js` calls `addNote` + `pushNotes`
after every release, and that runs in the core flow, not in a plugin, so `publish: false` does not
disable it.

## Step 4 — verify before letting a release run

```bash
git fetch origin --tags --force
git fetch origin 'refs/notes/*:refs/notes/*'

# every tag on its real commit
for t in 9.0.0-alpha.1 9.0.0-alpha.2 9.0.0-alpha.3 9.0.0-alpha.4 9.0.0-alpha.5; do
  printf '%-16s %s  %s\n' "$t" "$(git rev-parse --short "$t^{commit}")" "$(git log -1 --format=%s "$t^{commit}")"
done
# expect: five distinct commits, subjects RELEASE: 9.0.0-alpha.1 .. .5

# the ghost is gone from both remotes
git ls-remote --tags origin  | grep -c '9\.0\.0-alpha\.6'   # expect 0
git ls-remote --tags GitHub  | grep -c '9\.0\.0-alpha\.6'   # expect 0

# the notes exist on origin
git ls-remote origin 'refs/notes/*'                          # expect refs/notes/semantic-release
for t in 9.0.0-alpha.1 9.0.0-alpha.5; do git notes --ref=semantic-release show "$t"; done
# expect: {"channels":["alpha"]}
```

Then the real check — a dry run of the actual pipeline command from a **fresh clone**, because a clone
is what the runner gets and it proves the notes are reachable from the remote rather than from your
working copy:

```bash
git clone https://git.griefed.de/Griefed/ServerPackCreator.git /tmp/spc-verify
cd /tmp/spc-verify && git checkout alpha
CI=true npx --yes \
  -p semantic-release@24 \
  -p @semantic-release/changelog@7 \
  -p @semantic-release/git@11 \
  -p conventional-changelog-conventionalcommits@9 \
  semantic-release --dry-run
```

**Expect `Found git tag 9.0.0-alpha.5 associated with version 9.0.0-alpha.5 on branch alpha` and
`The next release version is 9.0.0-alpha.6`**, with release notes that start at
`9.0.0-alpha.5...9.0.0-alpha.6` rather than reaching back to `8.1.2`. Anything else — especially
`8.1.2` reappearing — means the notes did not reach origin.

## The rehearsal, and what it measured

The whole procedure was executed against a **local bare clone of this repository standing in for
origin**, so the numbers below come from the real commit graph and the real `.releaserc.yml`, not from
a toy repo. Nothing was pushed anywhere.

| state                                                     | last release found | commits since | next version    |
|-----------------------------------------------------------|--------------------|---------------|-----------------|
| broken (alpha.1-5 forced onto `6cd6e9af3`, no notes)        | `8.1.2`            | **624**       | `9.0.0-alpha.1` |
| repaired (tags retargeted, alpha.6 deleted, notes pushed)   | `9.0.0-alpha.5`    | **229**       | `9.0.0-alpha.6` |

The broken row reproduces the failing CI run exactly, down to the commit count — that is what makes the
diagnosis a finding rather than a theory. The repaired row was produced from a **fresh clone** of the
stand-in origin, whose only copy of the notes was on the remote side, which is the same position the
runner is in.

`generateNotes` completed with zero errors under the pinned `conventional-changelog-conventionalcommits@9`,
rendering all 229 commits under this repo's custom section titles (`🦊 CI/CD`, `✂️ Refactor`,
`🚀 Features`, `🛠 Fixes`, `📔 Docs`, `📦 Other`, `🧪 Tests`, and `⚠ BREAKING CHANGES`).

**One false alarm worth writing down so nobody re-chases it.** In the dry-run *log*, the heading prints
as `## 9.0.0-alpha.6 (https://.../compare/...) (2026-08-22)` with the markdown link flattened and the
bullets indented four spaces — it looks like the changelog format regressed. It has not: that is
semantic-release's signale logger rendering multi-line output. Calling `generateNotes` directly and
writing the string to a file gives
`## [9.0.0-alpha.6](https://git.griefed.de/Griefed/ServerPackCreator/compare/9.0.0-alpha.5...9.0.0-alpha.6) (2026-08-22)`
— byte-identical in shape to the existing `CHANGELOG.md` entries, and identical between preset 8 and
preset 9. Judge the format from the written file, never from the log.

## What this does not fix

The `9.0.0-alpha.1` … `.5` **release pages** on Forgejo keep whatever assets they were built with, and
those assets were built from `6cd6e9af3` (the 8.1.2 tree): `release-build.yml` triggers on the tag push
and its jobs use a bare `actions/checkout` with no `ref:`, so they build whatever the tag points at.
Retargeting the tags does not rebuild them. If those five alpha downloads matter,
they need rebuilding; if they do not, leave them and let `9.0.0-alpha.6` be the first alpha whose
assets match its tag.
