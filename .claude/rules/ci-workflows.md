---
description: How CI is wired on Forgejo, and the all-or-nothing landmine that governs .forgejo/workflows
paths:
  - ".forgejo/**"
  - ".github/**"
  - ".releaserc.yml"
---

> **History and evidence** (incidents, run numbers, measurement and snapshot tables, the gitlab.com mirror
> story) live in `claude-docs/CI-WORKFLOWS-HISTORY.md` — read it only when re-checking a rule's evidence.

# CI / release pipeline

Operator-facing secret detail lives in `claude-docs/CI-SECRETS.md`.

> **The `paths:` mechanism is verified** — reading a file matching a rule's globs injects that rule into
> context mid-session.
>
> **A bare `/memory` in a clean session proves nothing about it**: a correctly-scoped rule is *supposed*
> to be absent until a matching file is touched, so absence and breakage look identical. Touch
> `.forgejo/workflows/…` first, then look.

**CI lives in `.forgejo/workflows`. Forgejo (`git.griefed.de`) is the canonical CI and the origin of
every release.** `.gitlab-ci.yml` is gone. **LANDMINE:** `.forgejo/workflows` is *all-or-nothing* — once
it exists, Forgejo ignores `.github/workflows` entirely
([forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203)), so anything Forgejo must do belongs
there and nowhere else. `.github/workflows` keeps a **smoke test**, the three `clientside-*` workflows
(GitHub-native: two `issues:`-triggered, one `workflow_call:` helper, with their logic in
`.github/scripts/` so Forgejo's `test.yml` can execute it), and **`pages.yml`**. Releases are created on
Forgejo and mirrored outward by `release-build.yml`'s `mirror` job, because Forgejo push-mirrors replicate
refs but **not** releases. **GitHub is the only outward release mirror** (gitlab.com was dropped — see the
archive).

## Actions are referenced by VERSION TAG, not by commit SHA (Griefed's call, 2026-10-03)

**`uses: actions/checkout@v6`, in both directories** — a major tag, no SHA, no `# vX.Y.Z` comment.
Local `./.github/workflows/...` reusable-workflow references take no version at all.

**The cost was accepted, not overlooked:** a tag is mutable, so upstream can repoint `v6` with no commit
here to blame, and in `.forgejo/workflows` a bare `owner/repo` resolves through the runner's default
action URL, `https://data.forgejo.org`, so it also trusts that *mirror*. SHA pins nobody re-visits had
frozen actions at old releases; immutability nobody re-visits is staleness.

**Before adding a NEW bare reference under `.forgejo/workflows`, confirm the tag exists there with
`git ls-remote --tags https://data.forgejo.org/<owner>/<repo>`.** That host is in no file here (the runner
logs show it), and it mirrors common actions only — `actions/github-script` and
`JetBrains/writerside-github-action` are absent, fine only because both are used solely in
`.github/workflows`.

**Four references are not a bare major, each checked:**

| Reference | Why |
|---|---|
| `tiyee/action-ssh@v1.0.1` | publishes no `v1` tag *or* branch; `v1.0.1` is the only tag, and it is the commit that was pinned |
| `nogsantos/scp-deploy@v1.3.0` | same shape — tags are `v1.0.0`…`v1.3.0`, no `v1` |
| `luangong/setup-install4j@v1` | `v1` is a **branch**, not a tag, currently at exactly the pinned commit |
| `jmgilman/actions-generate-checksum@v1` | same — `refs/heads/v1`, no `v1` tag |

The last two are more mutable than a tag and are still what each action's README tells you to use. If one
of them ever matters more, the fix is a fork, not a SHA — a SHA there means nobody ever updates it.

**Dependabot watches `github-actions` at `/`, which on GitHub means `.github/workflows` only.** Everything
under `.forgejo/workflows` is watched by nothing; with tags it at least picks up patch releases on its own.

**The criterion for `.github/workflows` is "GitHub is the only place this CAN happen", not "this is
convenient here".** `pages.yml` passes despite running Writerside and Dokka (what `test.yml`'s header
warns against) because GitHub Pages deploys only from GitHub and Forgejo has no Pages; it takes no secrets
and gates nothing. The `clientside-*` three pass because GitHub Issues are the trigger. Anything that
fails this test belongs in `.forgejo/workflows`, where Forgejo can actually see it.

**`pages.yml` and `docs.yml` build the same bundle for two different hosts, and four things must move
together:** the `INSTANCE`/`ARTIFACT` pair, the pinned `jetbrains/writerside-builder` version, the
*Stage documents and images* step (the seven root documents are generated into `Writerside/topics/` and a
fresh checkout has none of them), and the `api/` path the Dokka tree is merged into.

**`spch.tree`'s Dokka entry is an ABSOLUTE URL, and it has to be.** The help viewer navigates for real
only when the href carries a scheme (`isExternal || /^(?:[a-z]+:)?\/\//`); a relative or root-absolute
`api/index.html` is client-side routed to `/api/`, whose `config.json`/`HelpTOC.json`/
`api-object-digest.json` 404, and the page dies on *"TOC data error"*.

**It is also PER-VERSION, and only `docs.yml` rewrites it.** The literal is the GitHub Pages URL, which
`pages.yml` wants as is. `docs.yml` replaces it with `https://serverpackcreator.de/api/<version>/index.html`
(that site serves a tree per version), and the step FAILS if the literal is absent, since `sed` matching
nothing exits 0. A second step reads `HelpTOC.json` back out of the built zip, because Writerside
rewrites hrefs; it SKIPS, loudly, where neither `unzip` nor `python3` exists — `unzip` is not a given
here, which is also why the dokka job ships a tar.gz.
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
and then every later step is skipped. Tool images such as `jetbrains/qodana-jvm-community` ship no node.

Run the tool as a `docker run` from an ordinary `runs-on: ubuntu-latest` job instead: the runner image
(`ghcr.io/catthehacker/ubuntu:act-22.04`) has node, and the tool image needs to supply nothing but the
tool. Two things to carry over each time — the tool image's own path conventions (Qodana wants
`/data/project`, `/data/cache`, `/data/results`, `/data/report`; its `WorkingDir` is `/data/project`),
and ownership: these images run as uid 0, so `chown -R "$(id -u):$(id -g)"` the outputs or
upload-artifact cannot read them when the runner is not root.

**`permissions:` does nothing on Forgejo, so there is none in this directory.** The runner warns
*"has a permissions field, which is not supported in Forgejo and will be ignored"* once per job.
Scoping there is per-job **Authorized Integrations**, set on the instance; a `permissions:` block here
only *looks* like hardening. `.github/workflows` keeps its blocks, because GitHub honours them.

**Creating a branch fires `push`, not just `create` — and GitHub's guard against it is a silent no-op
here.** `services/repository/push.go` dispatches `CreateRef` **and then** `PushCommits` for a new ref,
with the branch's last ten commits in the payload though none is new. So creating `alpha` or `beta`
starts every `push:` workflow on a branch that is byte-for-byte `main` — including `release-generate`,
and **semantic-release can mint a tag** there.

`create` *does* exist, contrary to the [Actions reference](https://forgejo.org/docs/latest/user/actions/reference/),
which omits it: `modules/actions/workflows.go` matches `HookEventCreate` (no activity types, so a bare
`on: create` works) and `services/actions/notifier.go` dispatches `CreatePayload{ref, sha, ref_type, …}`.
It is an opt-*in* trigger, so it is no help in opting *out*.

**LANDMINE — `github.event.created` does not exist on Forgejo.** Its `PushPayload`
(`modules/structs/hook.go`) has no `created`/`deleted`/`forced`, so `if: github.event.created == false`
is **true on every event**: no failure, no warning, the guard is simply inert.

What works is `Before`, which is `opts.OldCommitID` passed through unmutated; on a new ref it is the
all-zero object id. The guard is repeated verbatim in every job that needs it — there is no
workflow-level `if:`, and `qodana`'s `notify` needs its own copy because `always()` runs it even when the
job it `needs` was skipped:

```yaml
if: ${{ github.event_name != 'push' || !(startsWith(github.event.before, '0000000000000000')
        && (github.ref_name == 'alpha' || github.ref_name == 'beta')) }}
```

`startsWith` rather than `== '0000…'` on purpose: an all-zero string coerces to the number 0, and so does
an absent field, so the equality form would misfire on every non-push event. The 16-zero prefix also
covers a SHA-256 repository, whose empty id is 64 zeros.

**Verify a new guard by evaluation, not by reading:** run it through Forgejo's own act fork
(`code.forgejo.org/forgejo/act`, `pkg/exprparser`) over every event shape it must handle — creation of
`alpha`/`beta`, other branches, a normal push, `workflow_dispatch`, `schedule`, `pull_request`, and any
existing clause such as `release-generate`'s `RELEASE:` check.

**Corollary: a workflow that merely *parses* is not a workflow that runs.** Only a real run on the
instance shows whether a step executes.

## A step output written from PowerShell needs UTF-8 with no BOM

**LANDMINE — `Out-File` to `$env:GITHUB_OUTPUT` on a Windows runner yields an EMPTY output, silently.**
Windows PowerShell 5.1 — the only PowerShell a Windows host ships — defaults `Out-File` and the `>`/`>>`
operators to **UTF-16LE**, and its `-Encoding utf8`, the workaround GitHub's own documentation gives,
emits **UTF-8 with a byte-order mark**.

The Forgejo runner's `ParseEnvFile` (`act/container/parse_env_file.go`) splits each line on its first `=`
with **no encoding and no BOM handling** (upstream nektos/act strips a BOM; this fork does not), so the
key becomes `\xff\xfep\x00a\x00t\x00h\x00` or `\xef\xbb\xbfpath` instead of `path`, and the consuming
step reads an empty string.

**The step stays GREEN either way.** `runStepExecutor` (`act/runner/step.go`) records the conclusion from
the script's exit code and only *then* runs the file commands, whose error never touches it. UTF-8+BOM
reports nothing at all; UTF-16LE surfaces only as `[runner]: invalid format '', expected a line with '='
or '<<'` under a green step — the `''` is a stray NUL byte, not an empty line.

Write the file through .NET, which is the one spelling with no BOM in either PowerShell:

```powershell
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::AppendAllText($env:GITHUB_OUTPUT, "path=$jar`n", $utf8NoBom)
```

**Better still, pass nothing.** No step in this repository hands a value between two PowerShell steps:
`artifacts-reusable.yml`'s Windows job takes no `-JarPath`, and the value that cannot be wrong is the one
nobody passes. The rule above is for the next step that genuinely needs a hand-off, and applies equally
to `GITHUB_ENV`, `GITHUB_STATE` and `GITHUB_PATH`, which that one parser serves for all four.

## A workflow that lives only on a branch cannot be dispatched from the UI

**LANDMINE — the dispatch menu is built from the DEFAULT BRANCH, so a new workflow is unrunnable
until it is merged.** `routers/web/repo/actions/actions.go` resolves the list with
`ctx.Repo.GitRepo.GetBranchCommit(ctx.Repo.Repository.DefaultBranch)` and hands that commit to
`actions.ListWorkflows`; the branch being viewed never enters into it, and there is no fallback. A
`workflow_dispatch`-only workflow pushed on a feature branch therefore appears nowhere.

**The REST API has no such restriction and is the way out.** `DispatchWorkflow`
(`routers/api/v1/repo/action.go`) calls `GetWorkflowFromCommit(ctx.Repo().GitRepo, opt.Ref, name)`,
and that function expands **the ref the caller passed** and lists the workflows of *that* commit:

```
curl -X POST -H "Authorization: token $TOKEN" -H "Content-Type: application/json" \
  https://git.griefed.de/api/v1/repos/Griefed/ServerPackCreator/actions/workflows/<file>.yml/dispatches \
  -d '{"ref":"<branch>","inputs":{"key":"value"},"return_run_info":true}'
```

`ref` is required (`400 ref is empty` without it), the workflow must declare `on: workflow_dispatch`,
and a `type: boolean` input is converted from its string form before expressions see it
(`services/actions/workflows.go`, `resolveDispatchInput`). `return_run_info` swaps the `204` for a
`201` carrying the run id and its jobs.

**The third way, and the cheapest when the branch is already pushed, is to add a `push:` trigger
scoped with `branches:` and `paths:`** so only a commit touching the thing under test runs it. That
is what the Phase 0 probes do. It needs no token and no merge.

The consequence to plan around: **a workflow cannot be rehearsed by dispatch before it exists on the
default branch**, so anything whose first real run would otherwise be a release needs one of the
three routes above arranged in advance, not discovered at the point of use.

## The shared base: one definition per thing, dev and release differ only by inputs

**Why it is a rule:** with two copies of a job, only the one run on every push is rehearsed; the release
copy breaks unseen while a same-named job looks covered.

So: an artifact has exactly one definition, and the dev and release pipelines differ only in the values
they pass it. A difference that has to exist is an **input**, never a second copy.

**`misc/ci-duplication-check.py` enforces it**, with `misc/ci-duplication-allowlist.yml` holding the
repeats that genuinely cannot be shared, each with its reason. The allowlist is keyed by a hash of the
step's normalised body, so editing an exempted step turns the check red and the exemption is re-argued
rather than inherited. It is a gate in `test.yml`'s *Verify the workflow conventions* step, sharing one
pyyaml install with `ci-env-reference-check.py`, before the long build. It fails on an unexplained
duplicate and on an allowlist entry matching no step, both checked by exit code.

**That keying has one failure mode worth planning for: a sweep across every workflow re-keys every
entry it touches, and the check then reports each affected repeat TWICE** — once as a stale entry and
once as an unexplained duplicate, which reads like two findings and is one. Re-key in the same commit as
the sweep.

### What Forgejo will and will not do, all of it checked rather than assumed

- **`on.workflow_call.secrets` is a schema violation here.** A callee simply reads `secrets.X`, and the
  caller passes `jobs.<id>.secrets: inherit`. This **differs from `.github/workflows/`**, where
  `clientside-report-reusable.yml` declares its secrets and should keep doing so. `inherit` is
  all-or-nothing, so a shared artifact workflow has every publishing credential in scope — say so
  wherever one is written, or someone will add a publish step there *because the token is right here*.
- **A calling job must omit `runs-on:`**, or Forgejo disables workflow expansion.
- **A multi-job reusable workflow reports SUCCESS when an inner job failed below Runner v12.1.2**
  ([forgejo#6657](https://codeberg.org/forgejo/forgejo/issues/6657), fixed by
  [runner#1081](https://code.forgejo.org/forgejo/runner/pulls/1081)) — and then a broken artifact build
  reports green and the release ships on it. On this instance a failing inner job (Linux or Windows)
  propagates to the calling job. `needs.<job>.result` is populated on a calling job; expansion, inputs,
  outputs and `secrets: inherit` all work, and a `windows-latest` job inside a called workflow reaches
  the Windows runner.
- **The runner version is in the first line of every job log** — `Runner <uuid> (version:v13.2.0)` —
  and a Linux job also gets `FORGEJO_ACTIONS_RUNNER_VERSION`, so neither the admin page nor a host
  login is needed to check the floor.
- **The Windows runner reports `version:dev`, so the floor cannot be checked there at all.**
  `misc/windows-runner/provision.ps1` builds it from `main` with a bare `go build`, which stamps no
  version in. **The Linux runners can be checked by version, the Windows one only by behaviour** —
  measure that host directly rather than generalising from Linux; `winimage` lives inside the shared
  workflow.
- **A composite action may call other actions.** The runner's own fixture
  `act/runner/testdata/act-composite-env-test/action1/action.yml` does `uses: ./…/action2`.

### What cannot go inside a composite action

- **`actions/checkout`.** A local action's own files arrive *with* the checkout, so the checkout can
  never be the thing inside one. Forgejo's reference calls forgetting this the most common mistake.
- **The `secrets` context.** Pass secrets as inputs; they stay masked, because masking applies to the
  value wherever it appears.
- **A job-level `if:`.** It decides whether a job is scheduled; a composite runs inside one that
  already was.
- A composite `run:` step **must declare `shell:`**, where a job step defaults to bash.

### Where the pieces live

`.forgejo/actions/` — `setup-build` (JDK, Gradle, `chmod +x gradlew`; `gradle: "false"` for the Windows
jobs, which run jpackage and never resolve a dependency), `resolve-version` (`tag` mode refuses a tag
that is neither `X.Y.Z` nor `X.Y.Z-(alpha|beta).N`, `branch` mode derives one), `registry-cache` (the
pull-through-cache probe **and** the builder it configures, as one unit, because the config is useless
unless the next builder is handed it), `registry-login` (and the fact worth having in one place: ghcr's
username is `DOCKERHUB_USER`, because the accounts share a name and ghcr authenticates the token).

## A run has two numbers, and the web routes disagree about which one they take

**LANDMINE — `/actions/runs/{run}` takes the per-repo index everywhere except the artifact download,
which takes the instance-wide id.** Every handler under that path resolves the run with
`GetRunByIndex` — the run page, the job view, the logs, the artifact *listing* — while
`ArtifactsDownloadView` calls `getRunByID` (`routers/web/repo/actions/view.go`). One path, two
identifiers, no redirect between them: `/actions/runs/<index>/artifacts/<name>` 404s and
`/actions/runs/<id>/` 404s.

The two values reach a workflow under names that do not hint at the difference
(`services/actions/context.go`): **`github.run_number` is `run.Index`** (the number in a run's URL)
and **`github.run_id` is `run.ID`** (what the API and the artifact route want). The REST API takes the
id too, which is why `/api/v1/repos/{owner}/{repo}/actions/runs/222` 404s while the run's page is
`/actions/runs/222`; `index_in_repo` in a listing is the bridge between them.

So the upload action's own `artifact-url` output (built from `github.context.runId`) is the correct
link. **When a forge hands you two identifiers for one object, probe the route rather than reasoning
about which one is "the" run number** — `curl -o /dev/null -w '%{http_code}'` answers it anonymously in
a second.

## One workflow per area, and the one that cannot join them

**`grinder-container-it.yml` stays separate, and the reason is structural rather than preference.**
Its concurrency group has **no ref in it** and `cancel-in-progress: false`, so one run happens at a
time across the whole repository; `test.yml`'s is per-ref and cancellable. One workflow can hold only
one of those, because **Forgejo has no job-level `concurrency`** — read from its source, not assumed:
`ConfigureActionRunConcurrency` evaluates `workflow.RawConcurrency` onto the `ActionRun`, and
`models/actions/run_job.go` mentions concurrency nowhere at all.

So merging it costs one of two things: dropping the repo-wide lock re-opens cross-run interference
(a *different* test of the same class failing each time), and taking the lock for all of `test.yml`
serialises every push — considered and rejected (see the `concurrency` section below). Leave it where it is until Forgejo
grows job-level concurrency.

## `concurrency` is per-ref here, and two refs build the same commit on every push

**Forgejo implements `concurrency` (since v14), and its `cancel-in-progress: false` QUEUES rather than
doing nothing** (ordering not guaranteed). Search summaries claim the opposite; read the
[reference](https://forgejo.org/docs/v15.0/user/actions/reference/), not a summary.

**The group in this directory is `<workflow>-${{ github.ref }}`, which is deliberate, and it lets two
different branches run side by side** — two jobs, one Docker daemon, one filesystem. Fine for everything
that only reads; not fine for anything that asks the daemon a global question or performs a global side
effect. The signature of that interference is a *different* test of the same class failing each run; a
defect fails the same test every time. `pull_request:` is deliberately absent from the push-triggered
workflows, because every PR here is opened from a branch `push:` already builds — the cost is that the
merge result is not built separately and fork PRs get no Forgejo CI.

**A push also saturates the shared host, and that breaks every time-boxed wait in the suite at once.**
**Size every CI deadline against the pathological host, not the healthy one**, or replace it with a
host-independent assertion. When several unrelated guards go red together, compare durations across the
whole run list before diagnosing any of them. The remaining lever on the cause is the runner's own job
concurrency, which is host configuration — see `claude-docs/RUNNER-REGISTRY-CACHE.md`'s *Runner hygiene*
section.

`grinder-container-it.yml` is the answer where exclusivity is genuinely required — a group with **no ref in
it**, so one run at a time repository-wide, and `cancel-in-progress: false` so a second one queues. It holds
only the two classes that need it, because the lock is only cheap while the job is short; serialising
`test.yml` itself would have worked and cost every push a second full 35-minute run in series.

## The mirror can only be as current as the repository it mirrors into

**A mirror step's precondition is the mirror, not the API call.** Creating a release on a forge needs the
tag or the commit it names to exist there already. Before adding or restoring a mirror target, check it
actually has the ref: `curl -s https://<forge>/api/.../repository/commits?per_page=1` (anonymous is enough
for a public repo).

**LANDMINE — GitHub's `422` naming `tag_name is not a valid tag`, `Published releases must have a valid
tag` and an invalid `target_commitish` together is ONE cause: GitHub does not have the release commit.**
It sends you to the tag, the changelog and the payload, which are all fine. `release-build.yml` therefore
probes `GET /commits/${{ github.sha }}` before the POST (`422 No commit found for SHA` means the push
mirror has not delivered it), and treats `401`/`403` as the credential rather than waiting out the poll to
blame the wrong thing.

**A red `mirror` job does not mean an incomplete GitHub release.** It can fail after every asset was
uploaded. Check the GitHub release (assets, sizes, body, `target_commitish`) before repairing one, or you
will "fix" something that was never broken. Identical-looking red `mirror` jobs can have unrelated
causes, so read the job log — `GET /api/v1/repos/{owner}/{repo}/actions/jobs/{job_id}/logs` works
anonymously on a public repo — and distinguish them by exit code (`1` is the `::error::` the GitHub steps
emit). The API's run id is **not** the number in the run's URL (see *A run has two numbers*).

### Waiting for a push-mirror was the wrong half of the fix — trigger it

**`Sync when new commits are pushed` is an opt-in checkbox on a Forgejo push-mirror**; without it the
mirror is periodic on an interval that [defaults to 8h](https://forgejo.org/docs/v15.0/user/repo-mirror/),
so polling for it is a bet on someone else's schedule.

**The fix is `POST /repos/{owner}/{repo}/push_mirrors-sync`, and it is synchronous.** `repoPushMirrorSync`
(`routers/api/v1/repo/mirror.go`) loops the repository's push mirrors calling `SyncPushMirror` **inline**
rather than putting it on the mirror queue, and that runs
`git.Push(…, Force: true, Mirror: true)` — a `git push --mirror --force`. So a `200` means every mirror
holds the release commit *and* its tag by the time the call returns, which also demotes `target_commitish`
in the POST below from load-bearing to belt-and-braces. One call covers every mirror the repository has,
so **a forge added later needs no workflow change** — that is the whole reason to prefer it over probing
each target.

Three constraints that decide how the step is written:

- **It needs repo ADMIN, not write.** The route is `reqAdmin(), reqToken()`. `FJ_TOKEN` carries
  `write:repository` and its owner owns the repo, so nothing new is needed — but a token minted for a
  non-admin would get a `403` here while succeeding at every other `FJ_TOKEN` call in the pipeline.
- **One dead mirror can mask a healthy one.** The handler `return`s a 500 on the **first** mirror whose
  push fails and stops iterating, and the API offers no per-mirror sync (`GET`/`DELETE /push_mirrors/{name}`
  only) to route around it. So the step is best-effort and the **per-forge probe stays the verdict**; and
  the operational rule for a second mirror is that it gets repaired or removed, never left configured
  and broken, because a broken one taxes every release.
- **`last_error` is the thing to print.** `GET /push_mirrors` returns `remote_name`, `remote_address`,
  `sync_on_commit`, `interval`, `last_update` and `last_error` per mirror, and `last_error` is the exact
  text the web UI shows under Settings → Mirror Settings — which is what the old failure message sent an
  operator to go and read by hand. `remote_address` is stored credential-stripped (`util.SanitizeURL` at
  creation), so it is safe in a job log.

The poll that follows is sized for GitHub indexing a push it has already received, not for a mirror that
may not have run: 8 attempts at 15 s. **A poll is only honest about what it is waiting for.**

When a push mirror lags (gitlab.com still receives refs from one, partially), do not invent the
mechanism: read `last_error`, which the job prints on every release, before changing anything.

**The `news` job announces the GITHUB release, and asks GitHub rather than trusting `mirror`'s
colour.** Griefed's call, 2026-10-19: downloads are to come from GitHub, because the twelve assets are
the bulk of what this project serves and GitHub's bandwidth is free where git.griefed.de's is not. An
announcement sending people somewhere therefore must not precede the release being there.

**So `mirror` is in `needs:` for ORDERING ONLY and is absent from the job's `if:`.** The `if:` is
`always()` plus explicit `success` checks on `prepare`, `release`, `maven` and `docker` — the four whose
failure really does mean there is nothing worth announcing. `mirror`'s own result is not one of them
because a red `mirror` does not mean an incomplete GitHub release (and `push_mirrors-sync` is
best-effort), and because a **skipped** job cannot be re-run on its own, so a plain `needs: mirror` would
make the per-job repair — fix the mirror, re-run that job alone — leave the announcement permanently
unsendable.

**The guard was evaluated, not read**, against Forgejo's own act fork (`code.forgejo.org/forgejo/act`,
`pkg/exprparser`, which declares its module path as `github.com/nektos/act` and therefore needs a
`replace` directive to resolve). It is true when `mirror` failed or was skipped and the other four
succeeded, false when any of the four failed or everything was cancelled — and false on an **empty
`needs` context**: an inert context fails the guard CLOSED, so the failure direction is a skipped
announcement rather than an announcement of nothing. Re-evaluate it the same way after any edit.

The verdict is the job's first step, `Require the release to be live on GitHub`: one
`GET /releases/tags/<version>`, failing if the release is absent or has zero assets. Deliberately no
poll — `mirror` creates the release synchronously, so a 404 means never created. The step also yields
the release's `html_url`, so the message never links a URL assembled from a version string.

**LANDMINE — a step's `env:` key and the name its script reads are two separate things, and a
mismatch fails GREEN.** Renaming the secret and the shell reads but not the `env:` **key** leaves the
variable unconditionally empty, and a tolerant "not configured — skipping" branch exits 0 forever. So an
absent `NEWS_WEBHOOK_URL` **fails** the job: a secret that is supposed to be configured and is not is an
operator error, not a supported configuration. `misc/ci-env-reference-check.py` pins the defect by its
own shape across both workflow directories.

**Repairing one is per-job, not per-workflow.** A whole-run re-run re-executes `maven` and `docker` for a
published version, and Sonatype plus three registries reject a re-published version; only `release` is
idempotent by design. Repair the mirror, then re-run `mirror` alone (Forgejo 16.0.3+ can).

Two things generalise:

- **Waiting on an asynchronous dependency is a worse answer than triggering it, whenever a trigger
  exists.** Ask what *starts* the thing being waited for before choosing how long to wait for it.
- **`curl -sf` in CI is how a failure becomes unreadable.** `-f` sets exit 22 and `-s` throws away the body —
  which is the only place these APIs say what is wrong. Every call whose failure is not deliberately
  tolerated captures the status with `-o file -w '%{http_code}'` and prints the body before exiting —
  `mirror`, the `release` job's create/patch/upload, the VirusTotal release-body update and the Discord
  post. The two `-sf` calls left in `release-build.yml` are VirusTotal's own submissions, whose failures
  are tolerated on purpose (`|| true`, then a guard on the empty id) because a scan that does not come
  back must not fail a release that is already published.

## The release notes outgrew two limits nobody declared

**LANDMINE — a release body never travels as a curl argument.** Linux caps a *single* argv entry at
`MAX_ARG_STRLEN` = `32 * PAGE_SIZE` = **131,072 bytes**, independently of `ARG_MAX`, and `execve`
returns `E2BIG` past it: `/usr/bin/curl: Argument list too long`, followed by a python `JSONDecodeError`
traceback, which is the *consequence* — curl never ran, so the next stage read an empty pipe. **Read past
the traceback: the parser is never the bug when the line above it says a binary could not be exec'd.**

The size jump is structural: a prerelease section covers one increment, but a **beta or final** section
aggregates every prerelease since the last stable tag, because that is what semantic-release writes. So
every POST builds its payload with python and passes `-d @<file>`.

**LANDMINE — GitHub caps a release body at 125,000 characters; Forgejo does not cap it at all.**
Getting the body out of argv only moves the wall: GitHub answers `422 body is too long (maximum is
125000 characters)`. Forgejo's `Release.Note` is a `TEXT` column that Forgejo never truncates — only
`Title`, at 255 (`models/repo/release.go`) — and this instance is not database-limited either. So the
**canonical Forgejo release keeps every character and only the mirrored copy is cut**, in `Fetch release
notes from Forgejo` (the step that exists solely to produce the GitHub-bound copy), on a line boundary so
no markdown link is severed, with a pointer back to the Forgejo release.

**Forgejo does not enforce release-asset name uniqueness, so an unguarded re-run silently doubles the
assets.** `CreateReleaseAttachment` (`routers/api/v1/repo/release_attachment.go`) hands the name
straight to `UploadAttachment` with no existence check, so re-running `release` (the ordinary repair)
attaches a second copy of every asset and reports success. Both asset loops skip names the release
already carries (`GET /releases/{id}/assets`) — keep that guard.

**The general rule: a payload assembled from repository content has no size you control.** Ask where
it lands — argv, a database column, someone else's API — and put it in a file before it gets there.

## A Gradle task without a project path runs in every project

**LANDMINE — the release's `maven` job is the only place in this repo that could fan a task out over
every project, so it is the only place an unpublished module can break a release.** An *unqualified*
task name means "in every project"; an unpublished module's `dokkaGeneratePublicationJavadoc` failing
(for example on a missing `module.md`, which `serverpackcreator.dokka-conventions` includes in every
source set) stops the publish. The job runs `:serverpackcreator-api:dokkaJavadocJar`, and the convention
plugin refuses to configure a module that has no `module.md`, so that mistake fails on the next
`./gradlew` instead.

**Check the log for a `publish*` line before assuming a failed `maven` job is re-runnable.** If the
failure precedes every `publishMavenJavaPublicationTo*`, `publishToSonatype` and
`closeAndReleaseSonatypeStagingRepository` line, nothing reached a registry and re-running `maven` alone
is safe; otherwise it is **not** (see *Repairing one is per-job* above: those targets reject a
re-published version). A failed `maven` also skips `mirror` and `news`.

**The general rule: in a release job, name the project.** Everywhere else in this directory Dokka is
already scoped — `:serverpackcreator-api:dokkaGenerateHtml` in `assets` — and `-api` is the only
published module, so nothing about a release ever wanted the other six. A bare task name silently
recruits every module that applies the same convention plugin, including ones added months later by
someone who never reads this workflow.

## The release pipeline's two silent killers

Neither is visible in the workflow YAML, and neither produces a wrong-looking config — only a
wrong-looking release.

**LANDMINE — semantic-release keeps a prerelease tag's channel in git notes, not in the tag.** The
channel of every release is written to `refs/notes/semantic-release`; `lib/branches/get-tags.js` reads
it and falls back to `channels = [null]` when a tag has no note. `lib/get-last-release.js` then filters
a **prerelease** branch's tags down to those whose channels match the branch channel:

```js
(branch.type === "prerelease" && tag.channels.some((c) => isSameChannel(branch.channel, c)) && ...)
  || !semver.prerelease(tag.version)
```

So on a remote with no `refs/notes/*`, **every `X-alpha.N` tag is invisible** and only the newest
non-prerelease tag survives the filter. The symptom is a log line that looks almost right —
`Found git tag 8.1.2 ... on branch alpha` where `9.0.0-alpha.6` was expected — followed by a version
that restarts the prerelease counter at `.1`. The repair is
`git notes --ref=semantic-release add -m '{"channels":["alpha"]}' <tag>`, pushed to origin.

Two consequences worth knowing:

- **`actions/checkout` does not need changing.** semantic-release runs `fetchNotes` itself before
  resolving branches. The notes must exist **on origin**; nothing about the checkout step has to fetch
  them.
- **A forge migration loses them.** `refs/notes/*` is not a branch and not a tag, so a mirror, an
  import or a `git push --all --tags` carries none of it. `claude-docs/RELEASE-TAG-REPAIR.md` has the
  repair.

**LANDMINE — creating a release through the forge API mints the tag at the target branch, not at the
release commit.** Forgejo (like GitHub) creates a missing tag at `target_commitish` when a release is
created, so passing a branch there puts the tag on that branch's tip, potentially hundreds of commits
away from the code it names. Tags pushed by git are fine; tags minted by the API are not.
`release-build.yml`'s Forgejo call posts `{"tag_name":"$V", ...}` with no `target_commitish`, which is
correct because the tag already exists when the release is created.

The rule: **push the tag with git first, then create the release against a tag that already exists.**
The mirror job passes `target_commitish: ${{ github.sha }}` for the GitHub side because the tag has
usually not mirrored across yet — which covers an absent **tag** only; the commit must still be present,
hence the commit probe (see above).

## ghcr's 429 is a burst limiter on the *host*, and authenticating does not lift it

**LANDMINE — a registry 429 that asks for a sub-millisecond wait is not a quota you can buy your way out
of, and buildkit will not wait for it.** The error reads
`toomanyrequests: retry-after: 933.17µs, allowed: 44000/minute`, and it can hit a manifest resolve or a
blob fetch, so anything touching the registry is exposed. 44,000/minute is not an allowance one build can
exhaust, and a sub-millisecond retry-after is a token bucket keyed on the **requesting host** — so an
account-scoped credential is the wrong axis, and a pull that logged `Login Succeeded` still gets the 429.
The `Log in to ghcr.io when credentials are available` step is kept because it costs one second and is
right for Docker Hub's genuine per-account quota, but it is not what keeps the job green.

**What works is waiting, and the only retry a `uses:` step admits is `continue-on-error` plus a second
copy gated on `if: steps.<id>.outcome == 'failure'`** (`nick-fields/retry` and kin run shell commands,
not JavaScript actions). In the forgejo act fork, `continue-on-error` sets `outcome` to `failure` while
turning `conclusion` back to `success` (`pkg/runner/step.go`). Both `test.yml`'s `docker` job and
`release-build.yml` do this; the latter matters most, since a 429 there costs the release its images and
takes `mirror` (`needs: docker`) down with it.

**One retry, not a loop** — a host still limiting a minute later should fail loudly. In `test.yml` the
retry is nearly free (buildkit reuses what it fetched); in `release-build.yml`, `no-cache: true` makes
attempt two rebuild the whole Gradle stage, and pushing the same tags twice is idempotent.

**The cause the retry does not remove** is many pulls from one address (every job pulls
`ghcr.io/catthehacker/ubuntu:runner-latest`; per-ref concurrency runs builds side by side). Caching on the
runner host removes it — **`claude-docs/RUNNER-REGISTRY-CACHE.md`** has the configuration, why Docker
Engine's `registry-mirrors` (Docker-Hub-only) is not BuildKit's (any registry, falls back to upstream),
and the bridge-gateway/`INPUT policy DROP` trap that makes a mirror look like it does not work.

**The workflow half is fail-soft.** `test.yml` and `release-build.yml` probe each cache with `GET /v2/`
and mirror only what answered, so a host with no caches builds against upstream exactly as before. The
caches are reached by **fixed IP, not by name**, because jobs run inside a `docker:dind` daemon whose
embedded DNS does not know the host daemon's containers. Do not re-add `driver-opts: network=…` — a
missing network (`network runners_default not found`) kills the build outright. Section 0 of that file
has the topology; do not re-add a service name here.
