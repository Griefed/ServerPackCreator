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
there and nowhere else. `.github/workflows` keeps a **smoke test**, the four
`clientside-*` workflows, which are GitHub-native (three `issues:`-triggered, one `workflow_call:`
helper), and **`pages.yml`**; releases are created on Forgejo and mirrored outward
by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate refs but **not** releases.
**GitHub is the only outward mirror.** gitlab.com was one too until 2026-08-23 — see *The mirror can only be
as current as the repository it mirrors into* below.

## Actions are referenced by VERSION TAG, not by commit SHA (Griefed's call, 2026-10-03)

**`uses: actions/checkout@v6`, in both directories.** Of the **72** `uses:` lines in the two workflow
directories, **63** were a 40-character SHA with a `# vX.Y.Z` comment beside it and are now the tag, the
comment gone with them because the ref says what the comment said. The other nine were never SHAs: seven
in the Windows app-image jobs, written as tags from the start, and two local
`./.github/workflows/...` reusable-workflow references, which take no version at all. Re-derive the
total with `grep -rhE '^[[:space:]]*uses:' .forgejo/workflows/ .github/workflows/ | wc -l`.

**The cost is real and was accepted rather than overlooked:** a tag is mutable, so upstream can repoint
`v6` and the next push runs different code with no commit here to blame — and in `.forgejo/workflows` a
bare `owner/repo` resolves through the runner's default action URL, `https://data.forgejo.org`, so it also
trusts a *mirror* to stay in step. The measurement that decided it points the other way: **every major tag
was already AHEAD of the SHA it replaced**, so the pins had silently frozen eight actions at older releases
— `checkout` v6 at `d23441a4` against a pin of `9f698171`, and the same for `setup-java`, `gradle/actions`,
`cache`, `setup-node` and all three `docker/*`. Immutability nobody re-visits is staleness.

**Where the resolution host comes from, since it is in no file here:** the runners' own logs, which print
`☁️ git fetch 'https://data.forgejo.org/actions/checkout'`. The major tags were then confirmed present
there with `git ls-remote --tags` before anything was converted — do that again before adding a *new* bare
reference, because `data.forgejo.org` mirrors common actions and not arbitrary GitHub repositories
(`actions/github-script` and `JetBrains/writerside-github-action`, for instance, are absent from it; both
are used only in `.github/workflows`, where GitHub resolves them).

**Four references are not a bare major, each checked:**

| Reference | Why |
|---|---|
| `tiyee/action-ssh@v1.0.1` | publishes no `v1` tag *or* branch; `v1.0.1` is the only tag, and it is the commit that was pinned |
| `nogsantos/scp-deploy@v1.3.0` | same shape — tags are `v1.0.0`…`v1.3.0`, no `v1` |
| `luangong/setup-install4j@v1` | `v1` is a **branch**, not a tag, currently at exactly the pinned commit |
| `jmgilman/actions-generate-checksum@v1` | same — `refs/heads/v1`, no `v1` tag |

The last two are more mutable than a tag and are still what each action's README tells you to use. If one
of them ever matters more, the fix is a fork, not a SHA — a SHA there means nobody ever updates it.

**Dependabot watches `github-actions` at `/`, which on GitHub means `.github/workflows` only.** That
reaches 15 of the 72 `uses:` lines; the **57** under `.forgejo/workflows` are watched by nothing, before
and after this change. With tags they at least pick up patch releases on their own.

**The criterion for `.github/workflows` is "GitHub is the only place this CAN happen", not "this is
convenient here".** `pages.yml` stretches it the furthest and still passes: it runs the Writerside builder
*and* a Gradle Dokka build, which looks exactly like the second CI `test.yml`'s header warns against — but
GitHub Pages can only be deployed from GitHub, Forgejo has no Pages, and hosting the rendered help site
stopped being possible when GitLab Pages went away. It also takes no secrets and gates nothing, so a red run
costs a stale docs site. The `clientside-*` four pass for the same reason (GitHub Issues are the trigger).
Anything that fails this test belongs in `.forgejo/workflows`, where Forgejo can actually see it.

**`pages.yml` and `docs.yml` build the same bundle for two different hosts, and four things must move
together:** the `INSTANCE`/`ARTIFACT` pair, the pinned `jetbrains/writerside-builder` version, the
*Stage documents and images* step (the seven root documents are generated into `Writerside/topics/` and a
fresh checkout has none of them), and the `api/` path the Dokka tree is merged into.

**`spch.tree`'s Dokka entry is an ABSOLUTE URL, and it has to be.** The published help is a single-page
app whose viewer navigates for real only when the href carries a scheme — `isExternal || /^(?:[a-z]+:)?\/\//`
— so a relative `api/index.html` is client-side routed instead: the app pushes the URL to `/api/` and
re-bootstraps there, asking for `api/config.json`, `api/HelpTOC.json` and `api/api-object-digest.json`,
which exist only at the site root. All three 404 and the page dies on *"TOC data error"*. A root-absolute
`/ServerPackCreator/api/index.html` fails the same test. The cost is that the link leaves a self-hosted
container for the public site; `/api/` is still served in both layouts for anyone addressing it directly.
**`pages.yml` needs Settings → Pages → Source set to "GitHub Actions"**; while it is still "Deploy from a
branch" the workflow goes green and publishes nothing.

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

**Creating a branch fires `push`, not just `create` — and GitHub's guard against it is a silent no-op
here.** `services/repository/push.go` takes the `IsNewRef()` branch and calls `notify_service.CreateRef`
**and then** `notify_service.PushCommits`, so both events are dispatched; worse, it fills the push payload
with `newCommit.CommitsBeforeLimit(10)`, advertising the branch's last ten commits though not one of them
is new. Creating `alpha` or `beta` therefore started five workflows — `test`, `qodana`, `docker-test`,
`docs` and `release-generate` — against a branch that is byte-for-byte `main`. The last of those is the
one that matters: **semantic-release can mint a tag**, and it was running on a branch nobody had pushed
work to yet.

`create` *does* exist, contrary to the [Actions reference](https://forgejo.org/docs/latest/user/actions/reference/),
which omits it: `modules/actions/workflows.go` matches `HookEventCreate` (no activity types, so a bare
`on: create` works) and `services/actions/notifier.go` dispatches `CreatePayload{ref, sha, ref_type, …}`.
It is an opt-*in* trigger, so it is no help in opting *out*.

**LANDMINE — `github.event.created` does not exist on Forgejo.** Its `PushPayload`
(`modules/structs/hook.go`) is `Ref, Before, After, CompareURL, Commits, TotalCommits, HeadCommit, Repo,
Pusher, Sender` — GitHub's `created`/`deleted`/`forced` are absent. So `if: github.event.created == false`
evaluates **true on every event**, meaning the job always runs and the guard silently does nothing. It
does not fail, warn, or skip; it is simply inert, which is the worst of the three.

What works is `Before`, which is `opts.OldCommitID` passed through unmutated — the local `oldCommitID`
rewriting further down `pushUpdates` feeds only the compare URL. On a new ref it is the all-zero object
id. The guard, **repeated verbatim in six jobs** across those five workflows (there is no workflow-level
`if:`, and `qodana`'s `notify` needs its own copy because `always()` runs it even when the job it
`needs` was skipped):

```yaml
if: ${{ github.event_name != 'push' || !(startsWith(github.event.before, '0000000000000000')
        && (github.ref_name == 'alpha' || github.ref_name == 'beta')) }}
```

`startsWith` rather than `== '0000…'` on purpose: an all-zero string coerces to the number 0, and so does
an absent field, so the equality form would misfire on every non-push event. The 16-zero prefix also
covers a SHA-256 repository, whose empty id is 64 zeros.

**Verified by evaluation, not by reading.** Forgejo's own act fork (`code.forgejo.org/forgejo/act`
v1.37.0, `pkg/exprparser`) was run over all six guards: creation of `alpha`/`beta` → `false`; creation of
any other branch, every normal push, `workflow_dispatch`, `schedule` and `pull_request` → `true`;
`release-generate`'s existing `RELEASE:` clause still returns `false` for a release commit. The same
harness is what showed `github.event.created == false` returning `true` in all nine cases.

**Corollary worth stating: a workflow that merely *parses* is not a workflow that runs.** Both defects
above survived three audit iterations that validated YAML, checked action pinning, matched globs and
verified secret names. None of that touches whether the runner can execute a step. The only test that
finds these is a real run on the real instance.

## A step output written from PowerShell needs UTF-8 with no BOM

**LANDMINE — `Out-File` to `$env:GITHUB_OUTPUT` on a Windows runner yields an EMPTY output, silently.**
Windows PowerShell 5.1 — the only PowerShell a Windows host ships — defaults `Out-File` and the `>`/`>>`
operators to **UTF-16LE**, and its `-Encoding utf8`, the workaround GitHub's own documentation gives,
emits **UTF-8 with a byte-order mark**.

The Forgejo runner vendors act as `act/container/parse_env_file.go` (module
`code.forgejo.org/forgejo/runner/v13`), and `ParseEnvFile` splits each line on its first `=` with **no
encoding and no BOM handling at all** — unlike upstream nektos/act, which strips a UTF-8 BOM from the
first line. So UTF-16LE gives the key `\xff\xfep\x00a\x00t\x00h\x00` and `-Encoding utf8` gives
`\xef\xbb\xbfpath`, where the step meant `path`. The consuming step reads an empty string.

**The step stays GREEN either way, which is the part that costs the time.** `runStepExecutor`
(`act/runner/step.go`) records `stepResult.Conclusion` from the script's own exit code and logs
`✅ Success`, and only *then* runs the file commands, whose error never touches `stepResult`. UTF-8+BOM
parses as a junk key and reports nothing at all. UTF-16LE *does* error — the `\x0a\x00` line
terminator leaves a trailing `\x00` byte as a line of its own, which has neither `=` nor `<<` — but it
surfaces only as a log line reading `[runner]: invalid format '', expected a line with '=' or '<<'`,
under a step the UI has already marked green. The `''` is not an empty line: it is the NUL byte, which
renders as nothing.

Write the file through .NET, which is the one spelling with no BOM in either PowerShell:

```powershell
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::AppendAllText($env:GITHUB_OUTPUT, "path=$jar`n", $utf8NoBom)
```

`release-build.yml`'s `winimage` job is the only place in this repository that passes a value between
two PowerShell steps, and it is where this is enforced. The same applies to `GITHUB_ENV`,
`GITHUB_STATE` and `GITHUB_PATH`, which that one parser serves for all four.

## A run has two numbers, and the web routes disagree about which one they take

**LANDMINE — `/actions/runs/{run}` takes the per-repo index everywhere except the artifact download,
which takes the instance-wide id.** Every handler under that path resolves the run with
`GetRunByIndex` — the run page, the job view, the logs, the artifact *listing* — while
`ArtifactsDownloadView` calls `getRunByID` (`routers/web/repo/actions/view.go`). One path, two
identifiers, no redirect between them. Probed against this instance for one Qodana scan of
`ad5269302`, which is run index 644 and run id 936:

```
/actions/runs/644/artifacts/qodana-report -> 404
/actions/runs/936/artifacts/qodana-report -> 200, 2515070 bytes
/actions/runs/644/                        -> 307 to the job view
/actions/runs/936/                        -> 404
```

The two values reach a workflow under names that do not hint at the difference
(`services/actions/context.go`): **`github.run_number` is `run.Index`** (the number in a run's URL)
and **`github.run_id` is `run.ID`** (what the API and the artifact route want). The REST API takes the
id too, which is why `/api/v1/repos/{owner}/{repo}/actions/runs/222` 404s while the run's page is
`/actions/runs/222`; `index_in_repo` in a listing is the bridge between them.

This shipped a dead link in `qodana.yml`'s Discord message until 2026-09-26, and the comment that
caused it had the premise right and the conclusion inverted: it refused the upload action's own
`artifact-url` output *because* that output is built from `github.context.runId`. That is exactly the
identifier the route wants — the action logged `.../actions/runs/936/artifacts/879` in the same job,
and it was correct. **When a forge hands you two identifiers for one object, probe the route rather
than reasoning about which one is "the" run number.** `curl -o /dev/null -w '%{http_code}'` answers it
anonymously in a second.

## `concurrency` is per-ref here, and two refs build the same commit on every push

**Forgejo implements `concurrency` (since v14), and its `cancel-in-progress: false` QUEUES rather than
doing nothing** — *"any previous invocation of any workflow in the repository with the same concurrency
group will be executed before the newer invocation"*, with exact ordering documented as not guaranteed.
Worth stating because a search summary of the same feature says the opposite ("no concurrency management
will occur"), and because GitHub and Forgejo do not describe it identically. Read the
[reference](https://forgejo.org/docs/v15.0/user/actions/reference/), not a summary.

**The group in this directory is `<workflow>-${{ github.ref }}`, which is deliberate and used to have a
cost worth knowing: a push to `develop` also built PR #678 (`develop` → `beta`) at the same commit, in
parallel, on the same runner.** That particular doubling is gone — `pull_request:` was dropped from
`test.yml`, `docker-test.yml` and `grinder-container-it.yml` on 2026-10-02, because every PR here is
opened from a branch `push:` already builds — but the per-ref key still lets **two different branches**
run side by side, which is the same collision with a rarer trigger. Two jobs, one Docker daemon, one
filesystem. That is fine for everything that only reads
— and it is not fine for anything that asks the daemon a global question or performs a global side effect.
It cost `DockerJavaContainerEngineIT` three red runs in a row on 2026-09-27 (947, 954, 956), each failing a
*different* test of the same class, which is the signature of interference rather than of a defect: a defect
fails the same test in both jobs.

**The same push also saturates the host, and that breaks every time-boxed wait in the suite at once.**
On 2026-10-01 one push to `develop` queued **nine** runs at `17:03:00`; the runner started three heavy
Gradle/npm builds together and the whole batch ran 2–5x its own previous day's time:

| workflow | typical | 2026-10-01 |
|---|---|---|
| `docker-test.yml` | 13–15 min | **41.6 min** (829, 830 — buildkit's Gradle stage 616s → 1913s, npm 447s → 817s) |
| `devbuild.yml` | ~20 min | **96.6 min** (828) |
| `docs.yml` | ~25–31 min | **63.4 min** (832) |
| `grinder-container-it.yml` | 5.3–8.8 min | **18.1, 23.3 min** (831, 834 — both red) |
| `test.yml` | 12–22 min | 836 red |

Nothing in the repository changed to cause it — `921d46938` touched two workflow files — and the slow
stages are CPU-bound, not registry-bound, so this is capacity, not network. **What it breaks is
anything with a deadline in it**: `grinder-container-it` lost four tests to 90-second fixture waits and
one to a wall-clock assertion, and `test.yml` lost all fourteen database-backed tests because
flapdoodle's embedded `mongod` default is a 30-second start budget. All of those budgets have since
been raised or replaced with host-independent assertions, which is the right repository-side answer —
a CI deadline must be sized against the pathological host, not the healthy one — but it treats the
symptom. The cause is how much this runner is asked to do at once. **One of the two levers has been
pulled**: `pull_request:` is gone from the three workflows that carried it, halving the batch, at the
cost of no longer building the merge result separately and of fork PRs getting no Forgejo CI. The other
is the runner's own job concurrency, which is host configuration; see
`claude-docs/RUNNER-REGISTRY-CACHE.md`'s *Runner hygiene* section.

`grinder-container-it.yml` is the answer where exclusivity is genuinely required — a group with **no ref in
it**, so one run at a time repository-wide, and `cancel-in-progress: false` so a second one queues. It holds
only the two classes that need it, because the lock is only cheap while the job is short; serialising
`test.yml` itself would have worked and cost every push a second full 35-minute run in series.

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
  Every call whose failure is not deliberately tolerated now captures the status with
  `-o file -w '%{http_code}'` and prints the body before exiting — `mirror` first, and since
  *fix(ci): stop curl -sf hiding why a release step failed* also the `release` job's create/patch/upload,
  the VirusTotal release-body update and the Discord post. The two `-sf` calls left in `release-build.yml`
  are VirusTotal's own submissions, whose failures are tolerated on purpose (`|| true`, then a guard on
  the empty id) because a scan that does not come back must not fail a release that is already published.

## The release notes outgrew two limits nobody declared

**LANDMINE — a release body never travels as a curl argument.** Linux caps a *single* argv entry at
`MAX_ARG_STRLEN` = `32 * PAGE_SIZE` = **131,072 bytes**, independently of `ARG_MAX`, and `execve`
returns `E2BIG` past it. `release-build.yml` assembled the whole release JSON — changelog section
included — into one `-d "{...}"` word in both the `release` and `mirror` jobs, so `9.0.0-beta.1`
(run `536`, job `Forgejo release`) died with

```
/var/run/act/workflow/create.sh: line 21: /usr/bin/curl: Argument list too long
```

and then a python `JSONDecodeError` traceback, which is the *consequence* — curl never ran, so the
next stage in the pipeline read an empty pipe. **Read past the traceback: the parser is never the
bug when the line above it says a binary could not be exec'd.**

The jump is structural, not bad luck. A prerelease section covers one increment; a **beta or final**
section aggregates every prerelease since the last stable tag, because that is what semantic-release
writes:

```
9.0.0-alpha.8   changelog section  74,454 B   -d argument ~75,9xx B   released fine
9.0.0-beta.1    changelog section 200,185 B   -d argument  201,610 B  E2BIG
```

So `9.0.0` final will be at least as large. Both POST branches now build the payload with python and
pass `-d @<file>`; the PATCH branch had always done this, which is why only creation ever failed.
Reproduced both shapes against the real notes in `alpine:3.20` — inlined gives exit 126 `Argument
list too long`, `-d @file` execs curl and reaches the network with a 201,620-byte payload.

**LANDMINE — GitHub caps a release body at 125,000 characters; Forgejo does not cap it at all.**
Getting the body out of argv only moves the wall: GitHub answers `422 body is too long (maximum is
125000 characters)`. Forgejo's `Release.Note` is a `TEXT` column that Forgejo never truncates — only
`Title`, at 255 (`models/repo/release.go`) — and this instance is not database-limited either, since
`9.0.0-alpha.8`'s stored body is 75,918 bytes, past MySQL `TEXT`'s 65,535. So the **canonical Forgejo
release keeps every character and only the mirrored copy is cut**, in `Fetch release notes from
Forgejo` (the step that exists solely to produce the GitHub-bound copy), on a line boundary so no
markdown link is severed, with a pointer back to the Forgejo release. Measured on the real
`9.0.0-beta.1` notes: 200,185 → 124,935 characters.

**Forgejo does not enforce release-asset name uniqueness, so an unguarded re-run silently doubles the
assets.** `CreateReleaseAttachment` (`routers/api/v1/repo/release_attachment.go`) hands the name
straight to `UploadAttachment` with no existence check. Re-running `release` — which its own comment
names as the ordinary repair — therefore did not fail on a duplicate, which would at least be
visible; it attached a second copy of all twelve and reported success. Both asset loops now skip
names the release already carries (`GET /releases/{id}/assets`).

**The general rule: a payload assembled from repository content has no size you control.** Ask where
it lands — argv, a database column, someone else's API — and put it in a file before it gets there.
Both defects were latent from the first commit of this workflow and invisible for eight releases,
because the input only crossed the threshold when the release channel changed.

## A Gradle task without a project path runs in every project

**LANDMINE — the release's `maven` job is the only place in this repo that fans a task out over all
seven projects, so it is the only place an unpublished module can break a release.** `Publish Maven`
ran `./gradlew dokkaJavadocJar :serverpackcreator-api:signMavenJavaPublication`, and the *unqualified*
first task means "in every project". On `9.0.0-alpha.8` (run `472`, job `Publish Maven`)
`serverpackcreator-plugin-grinder` had no `module.md` — which `serverpackcreator.dokka-conventions`
includes as a File in every source set — so its `dokkaGeneratePublicationJavadoc` died with
`.../serverpackcreator-plugin-grinder/module.md (No such file or directory)` and the build stopped
there. The job is now `:serverpackcreator-api:dokkaJavadocJar`, and the convention plugin refuses to
configure a module that has no `module.md`, so the same mistake fails on the next `./gradlew` instead.

**What the incident cost, and what it did not.** `maven` is the `needs:` of both `mirror` and `news`,
so one unpublished module's docs skipped the GitHub mirror *and* the Discord announcement of a release
that was otherwise complete. It cost nothing to repair: the log contains **no** `publishMavenJavaPublicationTo*`,
`publishToSonatype` or `closeAndReleaseSonatypeStagingRepository` line at all, because the failure was in
the job's *first* `./gradlew` invocation. Nothing reached Sonatype, GitHub Packages, GitLab or the Forgejo
registry, so re-running `maven` alone is safe here — which is **not** the general case (see *Repairing one
is per-job* above: those four targets reject a re-published version). Check the log for a `publish*` line
before assuming a failed `maven` job is re-runnable.

**The general rule: in a release job, name the project.** Everywhere else in this directory Dokka is
already scoped — `:serverpackcreator-api:dokkaGenerateHtml` in `assets` — and `-api` is the only
published module, so nothing about a release ever wanted the other six. A bare task name silently
recruits every module that applies the same convention plugin, including ones added months later by
someone who never reads this workflow.

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

## ghcr's 429 is a burst limiter on the *host*, and authenticating does not lift it

**LANDMINE — a registry 429 that asks for a sub-millisecond wait is not a quota you can buy your way out
of, and buildkit will not wait for it.** `docker-test.yml` has lost three runs to the pull of
`ghcr.io/linuxserver/baseimage-ubuntu:noble` — 676 and 680 on 2026-09-27, 739 on 2026-09-28 — and all
three end the same way:

```
#9 ERROR: failed to copy: httpReadSeeker: failed open: unexpected status from GET request to
https://ghcr.io/v2/linuxserver/baseimage-ubuntu/blobs/sha256:5d9a14c0…: 429 Too Many Requests
::error::buildx failed with: toomanyrequests: retry-after: 933.17µs, allowed: 44000/minute
```

The phase varies — 676 died resolving the *manifest*, 680 and 739 fetching a *blob* — so anything that
touches the registry is exposed, not one request.

**The first fix was wrong, and the evidence that it was wrong is in the run it shipped in.** The `Log in
to ghcr.io when credentials are available` step was added on the premise that ghcr throttles *anonymous*
pulls per source address and an authenticated pull is counted against the account instead. Run 739 carried
that step, logged `Login Succeeded` and `Authenticated to ghcr.io as ***`, buildkit emitted its `[auth]
… token for ghcr.io` vertex (absent from job 1712, which had no credentials) — and the blob GET came back
429 anyway. Two numbers in the error say why: **44,000/minute is not an allowance one build can exhaust**,
and **the retry-after is 933 µs**. That is a token bucket keyed on the requesting host, refilling in under
a millisecond; an account-scoped credential is the wrong axis entirely. The login is kept — it costs one
second and is the right thing for Docker Hub's genuine per-account quota — but it is not why the job is
green.

**What works is waiting, and the only retry a `uses:` step admits is `continue-on-error` plus a second
copy of it.** `nick-fields/retry` and its kin run shell commands, not JavaScript actions. Verified against
the runner's own engine rather than assumed: `pkg/runner/step.go` in `code.forgejo.org/forgejo/act` sets
`stepResult.Outcome` to `failure` while `isContinueOnError` turns `Conclusion` back to `success`, and
`StepResult` (`pkg/model/step_result.go`) marshals `outcome` as exactly the string an
`if: steps.<id>.outcome == 'failure'` compares against. Both `docker-test.yml` and `release-build.yml`
now do this; the second is the one that matters, because a 429 there costs the release its images and
takes the `mirror` job (`needs: docker`) down with it.

**One retry, not a loop.** The limiter resets in under a millisecond, so a host still limiting a minute
later is a condition worth failing loudly on rather than grinding against. In `docker-test.yml` the retry
is nearly free — the builder container outlives the step, so buildkit reuses whatever it already fetched.
In `release-build.yml` it is not: `no-cache: true` means attempt two rebuilds the whole Gradle stage,
about fifteen minutes. Pushing the same tags twice is idempotent, so that is the price of the insurance.

**The standing exposure the retry does not remove:** every job on this instance pulls
`ghcr.io/catthehacker/ubuntu:runner-latest`, the grinder pulls its own images, and `concurrency` is keyed
per-ref, so one push routinely runs two builds of the same commit side by side against one registry from
one address. Caching in front of the registries on the runner host is the fix that removes the cause
rather than absorbing it — **`claude-docs/RUNNER-REGISTRY-CACHE.md`** has the configuration, the two
constraints that decide its shape (Docker Engine's `registry-mirrors` is Docker-Hub-only; BuildKit's is
not, and falls back to upstream — verified from `util/resolver/resolver.go`, which the buildkitd
reference does not state), and the bridge-gateway/`INPUT policy DROP` trap that makes a mirror look like
it simply does not work.

**The workflow half has landed and no longer has to wait for the host half.** `docker-test.yml` and
`release-build.yml` probe each cache with `GET /v2/` and mirror only what answered, so a host with no
caches builds against upstream exactly as before. The caches are reached by **fixed IP, not by name**,
because jobs run inside a `docker:dind` daemon whose embedded DNS does not know the host daemon's
containers — measured both ways on 2026-10-02 against a reproduction of the topology. `driver-opts:
network=…` went with it, which also removes the only way that step could kill a build outright
(`network runners_default not found`, run 797). Section 0 of that file has the topology; do not re-add
a service name here.
