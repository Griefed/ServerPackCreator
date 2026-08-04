# Refactor Audit — `claude-ci-qodana-jbr-cache` (`develop..HEAD`)

**Range audited:** `develop..HEAD` — **3 commits**, `fd8171288` … `f3a60df80`
**Base:** `develop` (`255af9eeb`) — verified ancestor (`git merge-base --is-ancestor`), clean linear range
**Files touched:** `.gitlab-ci.yml` only (52 insertions, 4 deletions). No Kotlin, no build script, no resources.
**Mode:** READ-ONLY. No source modified.
**Suites:** not re-run and not affected — no source, `buildSrc`, or resource file is in the range.

> Seventh report at this path. Supersedes the `claude-kdoc-coverage` audit (0 HIGH / 1 MEDIUM / 3 LOW, all
> remediated), whose text remains in git history at this path.

**Findings: 1 HIGH · 1 MEDIUM · 3 LOW.**

**Status 2026-08-04 — all five addressed, on `claude-ci-audit-fixes`.** **H-1 could not be fixed as recommended:**
between the audit and the remediation the three commits were merged into `origin/develop` (`f16dff7ff`) and
`origin/alpha`, so amending the label would mean force-pushing two shared branches. Recorded in `CLAUDE.md`'s
mislabelled-commit list instead — the same remedy this project already applied to `5f138ef8a` and `7815d5960` — with
the added lesson that the rule is cheap before a merge and unfixable after it. **M-1** closed by
`claude-docs/BACKLOG.md` §2026-08-04 (B26–B29; numbering continues from B25 because IDs still cited from the
`CLAUDE.md` files were not carried over when that file was emptied, which the new section now says out loud).
**L-1 and L-2** fixed in `afadcb90f`, which also found and fixed a **third defect introduced by the first attempt at
L-1**: probing every JBR with a hard `-version` let a stale tree fail the pipeline, and the `[ -x ]` assert written
to replace it was unsound — a Docker Desktop mount reports `0755` for a host-side `0644` file and answers `[ -x ]`
"executable" while `execve` still fails. The probe now classifies EACCES (fail) from anything else (warn); all five
paths verified against the block extracted from the YAML itself. **L-3** closed by the `REFACTOR-LOG.md` entry for
2026-08-04, now that Griefed has confirmed a green pipeline. The out-of-range note below is partly closed too: B25
and B4/B5/B11/B21/B22 are documented as dangling citations in `BACKLOG.md`.

Rules checked and **not** applicable, stated so they are not re-flagged: module boundaries, plugin-API
contract, Kotlin idioms (`val`/`!!`/Java-isms), Strangler-Fig incrementality, big-bang rewrite — the range
contains no Kotlin and no exported surface. Characterization tests: `CLAUDE.md` sets the ceiling for CI and
build-logic changes at *measurement recorded in the commit message*, deliberately, and two of the three
commits carry one (`fd8171288`: a three-step local reproduction; `358675fbf`: 20/20 → 7/20 jobs). `f3a60df80`
adds a diagnostic that produces a measurement and so has nothing to measure itself. That is the documented
ceiling, not a gap.

---

## HIGH

### H-1 · `358675fbf` — labelled `refactor(ci)`, but it changes behaviour

`.gitlab-ci.yml:16-25` (`.dockerized` template) plus the seven `extends:` sites.

**Rule broken:** *"`refactor:` is a claim about behaviour, not about intent. Use it only when behaviour is
preserved; label a behaviour change `fix:` or `feat:` however tidy it looks."* (`CLAUDE.md`, Refactor
discipline.)

Thirteen of the twenty jobs no longer start a container that they previously started, and each stops paying
the ~34 s service health-check wait measured in the 2026-08-03 log. The commit message **quantifies that
delta itself** — "before: 20/20, after: 7/20" — so the commit both claims behaviour preservation in its type
and reports a behavioural change in its body. `CLAUDE.md` already records two commits with exactly this
defect (`5f138ef8a`, `7815d5960`: *"Both described the change honestly in the body — only the type lied"*),
which makes this a repeat, not a first offence.

**The strongest defence, and why it does not hold.** The removed service was provably non-functional —
`dockerd` exits at startup (`can't create unix socket /var/run/docker.sock: device or resource busy`) — so
arguably nothing that *worked* was removed and the pipeline's outputs are byte-identical. Two things defeat
this. First, the rule is about the observable behaviour of what was changed, and a job that stops launching a
container and finishes 34 s sooner has changed. Second, the commit is internally inconsistent with its own
defence: it *retains* `.dockerized` on seven jobs precisely to avoid changing their behaviour. If service
presence is behaviour worth preserving for those seven, it was behaviour for the other thirteen too.

**Not a code defect.** The job selection is correct (verified below) and the commit is single-concern and
well-scoped. Only the type is wrong. Remedy: re-word to `fix(ci)` or `perf(ci)`; no code change.

**Job selection verified, so it is not re-flagged.** Every job body was parsed and cross-checked against the
`extends:` list. Exactly seven need a daemon and exactly seven have it. `Update README:on-schedule` contains
no literal `docker` token and trips a naive grep as a false positive — it runs `act -v` against
`catthehacker/ubuntu:act-*` images written into `~/.actrc` (`.gitlab-ci.yml:447-463`), so it does need one and
correctly has it. `Generate Release` runs `npx semantic-release` with every `@semantic-release/exec` block
commented out (`.releaserc.yml:129-134`), so it needs none. No Gradle job needs one: the Docker-dependent
grinder tests are gated behind `GRINDER_DOCKER_IT` / `GRINDER_TEMPLATE_IT`, unset in CI.

---

## MEDIUM

### M-1 · Branch-wide — three follow-ups created, none recorded in `claude-docs/BACKLOG.md`

`claude-docs/BACKLOG.md` currently reads **"## Empty — every recorded item has landed"**, and its own
instruction is *"Add the next item under a dated section, with the reason it waited and enough context to pick
it up cold."* This branch creates deferred work and records none of it there:

1. **Remove the diagnostic** added by `f3a60df80` (`.gitlab-ci.yml:164-174`). It is self-documented as
   temporary — *"Remove this block once the answer is recorded"* — which is honest, but a comment in a release
   pipeline is not a tracked item. Nothing outside this session's memory will surface it.
2. **Decide `.dockerized`'s fate** once that diagnostic answers host-socket vs. `tcp://docker`. Until then
   seven jobs keep launching a service that demonstrably cannot start.
3. **Fix or accept the dind socket collision itself.** `358675fbf` surfaces it explicitly rather than working
   around it silently — which satisfies the "surface bugs you find" rule — but its root cause is the runner's
   volume config, outside this repo, and there is no record of that anywhere a future session would look.

The `CLAUDE.md` "Refactor state" snapshot is likewise untouched. That is defensible for a CI fix (it is not an
architectural step, which is the condition the Definition of Done attaches) — see L-3.

---

## LOW

### L-1 · `fd8171288` — the evidence probe takes the *first* JBR, the chmod takes all of them

`.gitlab-ci.yml:114`

```
java_bin="$(find "$jbr" -type f -path '*/bin/java' 2>/dev/null | head -n1 || true)"
```

`chmod -R +x "$jbr"` (`:120`) repairs every cached JBR, so the **fix** is correct regardless. The `ls -l` and
`-version` lines, however, describe whichever copy `find` happened to return first. If a future Qodana bump
leaves two JBR trees in the cache, the log's "before/after" evidence — the whole reason those lines exist, per
the commit message — may describe a tree Qodana never execs, while the one it does exec goes unreported.
Cosmetic today (one tree, verified locally: `qodana-jbrsdk-25.0.2-…`), and it degrades the diagnostic rather
than the repair.

### L-2 · `fd8171288` — a JBR tree with no `bin/java` skips the chmod and prints a misleading message

`.gitlab-ci.yml:114-116`

The guard keys on the *java binary*, not on the JBR *tree*. If `qodana-jbr/` is restored but incomplete — no
`bin/java` — `java_bin` is empty, the `chmod` is skipped entirely, and the log states `**** No cached JBR in
$jbr - Qodana will download one ****`. That claim is only true if Qodana re-downloads on a partial tree, which
was not verified; if it instead accepts the directory as present, the original EACCES returns underneath a
message asserting the opposite. Low likelihood, but this is precisely a cache that already loses file
metadata, so a partial restore is not far-fetched. A tree-level guard (`[ -d "$jbr" ]`) with the binary lookup
inside it would report honestly in both cases.

### L-3 · Branch-wide — no `claude-docs/REFACTOR-LOG.md` entry

`CLAUDE.md`'s Definition of Done ties both the "Refactor state" update and the `REFACTOR-LOG.md` append to
*"when an architectural step lands."* A CI fix is not one, so this may be correctly *premature* rather than
missing — the natural moment is when the branch's follow-ups close after the next pipeline. Recorded because
the branch does produce durable, non-derivable knowledge that currently lives only in commit messages: that
Qodana ≥ 2026.2 necessarily places an executable inside the GitLab-cached directory with no opt-out, and that
the dind service in this pipeline has never worked. The first is captured well at the call site
(`.gitlab-ci.yml:104-111`); the second is not captured anywhere durable (see M-1.3).

---

## Verified clean — recorded so the next audit does not re-derive it

- **The fix is not inert.** `before_script` runs in `step_script`, which GitLab starts *after* `restore_cache`
  and `download_artifacts` — confirmed against the failing log's own section markers (`restore_cache` ends
  20:23:18, `step_script` begins 20:23:35). Had the chmod run before the restore it would have done nothing.
- **One concern per commit, in the right order.** `fd8171288` fix → `358675fbf` service scoping →
  `f3a60df80` diagnostic. No commit mixes them; the second and third touch disjoint regions of the file.
- **No scope sprawl.** `358675fbf` edits eight job definitions, every one of them forced by removing the
  top-level `services:` block. Nothing unrelated was tidied along the way.
- **No secret exposure in the new diagnostic** (`f3a60df80`), which matters because it was inserted into the
  one job that handles `DOCKERHUB_TOKEN` and `GITHUB_TOKEN`. It prints two named variables, an `ls -l`, and
  `docker info --format 'Name=… ServerVersion=…'` — no `env`, no unfiltered `docker info`, no credential
  path. Every line ends in `|| true`, so it cannot fail the job it diagnoses.
- **The `refactor:`/behaviour rule's carve-out was considered and rejected** for H-1: the exemption covers
  reference-only updates (moving a symbol, imports following), not the removal of a runtime component.
- **`chmod -R +x` breadth is deliberate, not sloppy.** Measured: 6 of 133 files in the JBR carry an x bit, two
  of them (`lib/jexec`, `lib/jspawnhelper`) outside `bin/`, which is why a `bin/`-only predicate was rejected.
  No file anywhere else in the cache directory is executable, which is why the chmod is scoped to
  `qodana-jbr/` rather than the whole cache.

## Out of range — noted, not counted

`CLAUDE.md`'s 2026-07-31 paragraph ends *"Remaining backlog: B4, B5, B11 (deliberate) plus B21, B22"*, while
`claude-docs/BACKLOG.md` records every item as landed. The sentence sits inside an explicitly dated paragraph,
so it reads as a statement of that date rather than of now — not flagged as a defect. It does mean a reader
looking for open backlog items gets contradictory answers from the two files, and the shorter fix is a
present-tense pointer in `CLAUDE.md` to whatever `BACKLOG.md` currently says. Pre-existing on `develop`;
outside this range.

---

**Recommendation.** H-1 is a one-word commit-message change (`refactor(ci)` → `fix(ci)`) and, since nothing is
pushed, can be amended in place. M-1 wants a dated `BACKLOG.md` section with the three follow-ups. L-1 and L-2
are a small edit to the same guard and could ride one `fix(ci)` commit. L-3 is best left until the pipeline has
run. Awaiting go-ahead — no source modified.
