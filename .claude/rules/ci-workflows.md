---
description: How CI is wired on Forgejo, and the all-or-nothing landmine that governs .forgejo/workflows
paths:
  - ".forgejo/**"
  - ".github/**"
  - ".releaserc.yml"
---

# CI / release pipeline

Moved out of the root `CLAUDE.md` on 2026-08-21 so it loads when you touch a workflow rather than in
every session. Operator-facing secret detail lives in `claude-docs/CI-SECRETS.md`.

> **The `paths:` mechanism is verified** — reading a file matching a rule's globs injects that rule into
> context mid-session, confirmed 2026-08-21 against `.claude/rules/build-layout.md` and
> `gradle/libs.versions.toml`. This file's own globs were not exercised directly, but they are the same
> mechanism.
>
> **A bare `/memory` in a clean session proves nothing about it**: a correctly-scoped rule is *supposed*
> to be absent until a matching file is touched, so absence and breakage look identical. Touch
> `.forgejo/workflows/…` first, then look.

**CI lives in `.forgejo/workflows`. Forgejo (`git.griefed.de`) is the canonical CI and the origin of
every release.** `.gitlab-ci.yml` is gone. **LANDMINE:** `.forgejo/workflows` is *all-or-nothing* — once
it exists, Forgejo ignores `.github/workflows` entirely
([forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203)), so anything Forgejo must do belongs
there and nowhere else. `.github/workflows` keeps a **smoke test** plus the four
`clientside-*` workflows, which are GitHub-native (three `issues:`-triggered, one `workflow_call:`
helper); releases are created on Forgejo and mirrored outward
by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate refs but **not** releases.
**GitHub is the only outward mirror.** gitlab.com was one too until 2026-08-23 — see *The mirror can only be
as current as the repository it mirrors into* below.
Two GitLab capabilities were **deliberately not carried over**: `Build Release` uploaded the app jar to
GitLab's *generic package registry* and then created a release asset *link* to it (Forgejo attaches
assets to the release directly, so a consumer with a hard-coded `/packages/generic/...` URL loses it),
and `release_job` created a release whose description merely linked changelogs on three forges (the
Forgejo release now carries the changelog section itself).

## Two Forgejo behaviours that GitHub-shaped workflows get wrong

**LANDMINE — never give a job a `container:` of a tool image.** Forgejo Actions is act-based, and act
runs every *JavaScript* action by exec'ing `node` **inside the job container**. GitHub's hosted runners
inject a node binary into container jobs; act does not. So `actions/checkout`, `actions/cache`,
`actions/upload-artifact` and `actions/download-artifact` all die with
`exec: "node": executable file not found in $PATH`, exit 127 ([nektos/act#107](https://github.com/nektos/act/issues/107)),
and then every later step is skipped. Verified: `command -v node` in
`jetbrains/qodana-jvm-community:2026.2` returns nothing. Both jobs that did this failed in the wild —
qodana.yml run 134 and docs.yml run 133.

Run the tool as a `docker run` from an ordinary `runs-on: ubuntu-latest` job instead: the runner image
(`ghcr.io/catthehacker/ubuntu:act-22.04`) has node, and the tool image needs to supply nothing but the
tool. Two things to carry over each time — the tool image's own path conventions (Qodana wants
`/data/project`, `/data/cache`, `/data/results`, `/data/report`; its `WorkingDir` is `/data/project`),
and ownership: these images run as uid 0, so `chown -R "$(id -u):$(id -g)"` the outputs or
upload-artifact cannot read them when the runner is not root.

**`permissions:` does nothing on Forgejo, so there is none in this directory.** The runner warns
*"has a permissions field, which is not supported in Forgejo and will be ignored"* once per job.
Capability scoping there is per-job **Authorized Integrations**, configured on the instance, not in the
workflow. This matters beyond noise: `claude-docs/WORKFLOW-AUDIT.md`'s H2 finding was "every workflow
now has a top-level `permissions: contents: read`", and the migration was reported as carrying that
hardening across. It did not — the blocks were inert from the first commit. `.github/workflows` keeps
its blocks, because GitHub honours them.

**Corollary worth stating: a workflow that merely *parses* is not a workflow that runs.** Both defects
above survived three audit iterations that validated YAML, checked action pinning, matched globs and
verified secret names. None of that touches whether the runner can execute a step. The only test that
finds these is a real run on the real instance.

## The mirror can only be as current as the repository it mirrors into

**gitlab.com was dropped as an outward mirror on 2026-08-23, and no release-API change could have saved it.**
The `Mirror to GitLab.com` step reported a bare `⚙️ [runner]: exitcode '22': failure` in 0 s — curl's
`--fail`, meaning HTTP ≥ 400, with the body discarded by `-sf`. It had never been able to succeed since it
was written, because its precondition was already false when the migration added it. The cause was not in
the workflow:

```
gitlab.com/Griefed/ServerPackCreator  newest commit  071e55402  2024-04-27  "RELEASE: 5.2.1"
                                      newest tag     5.2.1        (1397 commits behind main)
                                      releases       5, latest 5.2.1
```

The git push-mirror to gitlab.com died with the GitLab→Forgejo migration, so that repository has not received
a commit since April 2024. GitLab's `POST /releases` needs either an existing `tag_name` or a `ref` commit to
mint the tag from, and **that repository has neither** — the tag names a version four major lines newer than
anything there, and the SHA has never existed there. The step's own comment anticipated the 404 and added `ref` to fix it, which was the
right fix for a mirror that is merely *behind* and useless for one that is *stopped*.

**The concrete instance is run `222` (`9.0.0-alpha.6`), job `Mirror release outward`, and its log is the
proof — plus a trap worth knowing.** That job mirrored **all twelve assets to GitHub successfully**, and
only then died:

```
10:02:09Z  GitHub <- updates.xml          <- the last of 12, all fine
10:02:11Z  ⚙️ [runner]: exitcode '22': failure
```

`22` is curl's `--fail`, and the step that produced it was `Mirror to GitLab.com`, present at that tag
(`git show 9.0.0-alpha.6:.forgejo/workflows/release-build.yml` has it) and deleted two and a half hours
later by *fix(ci): drop the gitlab.com release mirror, and stop curl hiding why*. So **alpha.6 needs no
repair and the failure cannot recur** — the step is gone.

**The trap: a red `mirror` job does not mean an incomplete GitHub release.** Verified for alpha.6 against
both APIs — 12/12 assets present with matching sizes, an identical 41,275-char body including the
VirusTotal section, `target_commitish` `f7ebba4e`, `prerelease: true`. Red CI, complete release. Check the
release before repairing one, or you will "fix" something that was never broken. It also means the run
list alone cannot tell these two failures apart: **`9.0.0-alpha.6` and `9.0.0-alpha.7` both show exactly
one red job, `Mirror release outward`, attempt 1, and they failed for completely unrelated reasons** — the
deleted GitLab step versus the stalled mirror below. Read the job log (`GET
/api/v1/repos/{owner}/{repo}/actions/jobs/{job_id}/logs`, which works anonymously on a public repo and is
the fastest way in) and distinguish them by exit code: `22` is the old GitLab step, `1` is the `::error::`
the GitHub steps now emit. Note the API's run id is **not** the number in the run's URL — that is
`index_in_repo` (`222` → id `252`, `272` → id `311`), so `/actions/runs/222` 404s.

**It then happened to GitHub, on 9.0.0-alpha.7 (2026-08-23, run `272`) — so this is a class, not a
GitLab story. This is a DIFFERENT failure from alpha.6's above, despite looking identical in the run
list.**
The `mirror` job died on a GitHub `422` naming three fields at once: `tag_name is not a valid tag`,
`Published releases must have a valid tag`, and an invalid `target_commitish`. All three are one cause with
three symptoms. GitHub did not have the release commit: `GET /commits/50fd50f37` answered `422 No commit
found for SHA`, GitHub's `alpha` still sat on `f7ebba4e` (`RELEASE: 9.0.0-alpha.6`, 69 commits behind), and
`pushed_at` was five hours older than the tag. With no commit there is nothing to mint the tag from, so
`target_commitish` is rejected, and a release with no tag is rejected in turn. **Nothing was wrong with the
workflow** — the Forgejo release was complete and correct (id 1730, 12 assets, the VirusTotal section
present, the tag on the right commit), and `9.0.0-alpha.6` is standing proof the same code works when the
mirror is current: its GitHub release carries `target_commitish` `f7ebba4e`, where `.1` through `.5` carry
`main`.

Note what this costs in reading time: GitHub's 422 is about `tag_name`, so it sends you to the tag, the
changelog and the release payload — three places that were all fine. `release-build.yml` now probes
`GET /commits/${{ github.sha }}` before the POST and polls for five minutes, because a push-mirror's
`Sync when new commits are pushed` is an **opt-in** checkbox and without it the mirror is periodic on an
interval that [defaults to 8h](https://forgejo.org/docs/v15.0/user/repo-mirror/); it then fails naming the
mirror, and treats `401`/`403` as the credential rather than waiting five minutes to blame the wrong thing.

**Nothing else in the release should be gated on the mirror.** The `news` job — the Discord
announcement recreated from `main`'s `github_release.yml`/`github-prerelease.yml` — needs
`[prepare, release, maven, docker]` and pointedly **not** `mirror`, because it announces the Forgejo
release, which is complete and correct in both incidents above. Gating it on the mirror would have
silenced the announcement of two perfectly good releases.

**Repairing one is per-job, not per-workflow.** Re-running the whole run would re-execute `maven` and
`docker` for a version already published — `closeAndReleaseSonatypeStagingRepository` plus three registries
that reject a re-published version. Only `release` is idempotent by design. Forgejo 16.0.3 can re-run a
single job, so repair the mirror, then re-run `mirror` alone.

Two things generalise beyond GitLab:

- **A mirror step's precondition is the mirror, not the API call.** Before adding or restoring one, check the
  target actually has the ref: `curl -s https://<forge>/api/.../repository/commits?per_page=1`. Anonymous is
  enough for a public repo, and it takes seconds.
- **`curl -sf` in CI is how a failure becomes unreadable.** `-f` sets exit 22 and `-s` throws away the body —
  which is the only place these APIs say what is wrong. The failing run said nothing else whatsoever.
  The `mirror` job now captures the status with `-o file -w '%{http_code}'` and prints the body before
  exiting; **the `release`, `virustotal` and release-body-update steps still use `curl -sf`** and have the
  same blindness waiting for them.

## The release pipeline's two silent killers

Both bit the same run on 2026-08-22 (`release-generate.yml`, branch `alpha`). Neither is visible in the
workflow YAML, and neither produces a wrong-looking config — only a wrong-looking release.

**LANDMINE — semantic-release keeps a prerelease tag's channel in git notes, not in the tag.** The
channel of every release is written to `refs/notes/semantic-release`; `lib/branches/get-tags.js` reads
it and falls back to `channels = [null]` when a tag has no note. `lib/get-last-release.js` then filters
a **prerelease** branch's tags down to those whose channels match the branch channel:

```js
(branch.type === "prerelease" && tag.channels.some((c) => isSameChannel(branch.channel, c)) && ...)
  || !semver.prerelease(tag.version)
```

So on a remote with no `refs/notes/*`, **every `X-alpha.N` tag is invisible** and only the newest
non-prerelease tag survives the filter. The observed symptom is a log line that looks almost right —
`Found git tag 8.1.2 ... on branch alpha` where `9.0.0-alpha.6` was expected — followed by a version
that restarts the prerelease counter at `.1`. Reproduced in a scratch repo both ways: no notes gives
`8.1.2` / `9.0.0-alpha.1`, and `git notes --ref=semantic-release add -m '{"channels":["alpha"]}'` on
`9.0.0-alpha.6` gives `9.0.0-alpha.6` / `9.0.0-alpha.7`.

Two consequences worth knowing:

- **`actions/checkout` does not need changing.** semantic-release runs `fetchNotes` itself before
  resolving branches, verified against a fresh clone whose only copy of the notes was on the remote.
  The notes must exist **on origin**; nothing about the checkout step has to fetch them.
- **A forge migration loses them.** `refs/notes/*` is not a branch and not a tag, so a mirror, an
  import or a `git push --all --tags` carries none of it. That is exactly how the Forgejo remote ended
  up with 375 tags and zero notes. `claude-docs/RELEASE-TAG-REPAIR.md` has the repair.

**LANDMINE — creating a release through the forge API mints the tag at the target branch, not at the
release commit.** Forgejo (like GitHub) creates a missing tag at `target_commitish` when a release is
created. The releases API still shows `"target_commitish": "main"` on `9.0.0-alpha.1` through `.5`, and
all five tags sit on `main`'s tip — the `RELEASE: 8.1.2` commit `6cd6e9af3` — while the real
`RELEASE: 9.0.0-alpha.N` commits sit on `alpha`. Comparing every remote tag against its local
counterpart bounds the damage exactly: of 375 tags, those five mismatch, `9.0.0-alpha.6` is a ghost
(see below), `continuous` legitimately moves because it is the rolling dev tag, and **the other 368
match byte for byte**. Tags pushed by git are fine; tags minted by the API are not.

`release-build.yml` is **not** the culprit and needs no change: its Forgejo call posts
`{"tag_name":"$V", ...}` with no `target_commitish`, which is why `8.1.2` and `9.0.0-alpha.6` both come
back from the API with an empty target — the tag already existed when the release was created. How
`9.0.0-alpha.6`'s tag came to be on `6cd6e9af3` anyway is **not** recoverable from what the remote still
holds; don't invent a mechanism for it. What *is* on the record: there is no `RELEASE: 9.0.0-alpha.6`
commit in the history and `CHANGELOG.md` on `origin/alpha` ends at `.5`, so that release's
`@semantic-release/git` commit never landed.

The rule that follows regardless: **push the tag with git first, then create the release against a tag
that already exists.** Passing a branch as `target_commitish` for a release that has a real commit is
how five tags ended up 300-odd commits away from the code they name. Note the mirror job already gets
this right for the GitHub side — it passes `target_commitish: ${{ github.sha }}` precisely because the
tag has usually not mirrored across yet. That covers an absent **tag** only: the commit it names still has
to be present, which is why the job now probes for it first (see above).
