# Refactor audit — `claude-parallelmap-thread-leak` (second pass, post-remediation)

**Base:** `a7717e8a9` (`develop`) · **Head:** `1cc6c4b1a` · **Commits:** 6 · **Date:** 2026-08-16
**Supersedes** the first pass, which covered a 7-commit history and raised **2 HIGH, 4 MEDIUM,
3 LOW**. That history has been rewritten; the first pass's commit hashes no longer exist.
**Pre-rewrite history preserved at branch `backup-pre-rewrite` (`b1f831dad`).**

**Verdict: no HIGH, no MEDIUM. Two deliberate deviations retained and justified below.**

---

## Rewritten history

| # | Commit | Subject | State |
|---|---|---|---|
| 1 | `e55ba8947` | `test(api): pin that parallelMap neither leaks a thread nor serialises` | **RED** (intentional) |
| 2 | `ae18f5657` | `fix(api): stop parallelMap leaking a thread per call, and make it parallel` | green |
| 3 | `e55ddfe8e` | `build: bump third-party library versions in the catalog` | **RED** (intentional) |
| 4 | `3ab1abed6` | `fix(build): import Boot's BOM as a platform so the catalog wins` | green |
| 5 | `e7320796a` | `fix(build): declare mockk explicitly in -app so it matches -api` | green |
| 6 | `1cc6c4b1a` | `docs: record the parallelMap fix, the coroutines floor and the BOM landmine` | green |

The rewrite is content-preserving. `git diff backup-pre-rewrite HEAD` touches exactly three files —
`ListUtilities.kt` (L3), `ListUtilitiesTest.kt` (M2) and `serverpackcreator-api/CLAUDE.md` (the M2
lesson). Every other byte of the branch's work is identical; only the history was reorganised.

---

## First-pass findings — disposition

| ID | Sev | Finding | Status |
|---|---|---|---|
| H1 | HIGH | `58ba8ced8` bundled five concerns, two of them behaviour changes | **FIXED** — split into commits 3, 4, 5 and 6 |
| H2 | HIGH | Branch not bisectable; catalog bump landed after its own fix | **FIXED** — catalog bump is now commit 3, its fix commit 4 |
| M1 | MED | mockk bug fixed inline instead of its own commit | **FIXED** — commit 5 |
| M2 | MED | Leak guard could silently stop guarding | **FIXED** — count-based, commit 1 |
| M3 | MED | Two commits cancelled out; two more documented a dead state | **FIXED** — the `ext[]` attempt and its docs are gone |
| M4 | MED | Published-API behaviour change without a prior-contract pin | **ACCEPTED** — see below |
| L1 | LOW | Cross-module comment edit outside commit scope | **FIXED** — moved into commit 5, where it becomes true |
| L2 | LOW | Unrelated test-count drift corrected in a scoped docs commit | **ACCEPTED** — disclosed in the commit body |
| L3 | LOW | Wildcard `import kotlinx.coroutines.*` retained | **FIXED** — narrowed in commit 2 |

### H2 — how it was fixed, and the evidence

The whole point was that the failure had to be reproducible from the commit that causes it. It now
is. Measured **at commit 3**, before its fix exists:

```
:serverpackcreator-app:dependencyInsight --dependency kotlinx-coroutines-core
  testRuntimeClasspath  1.10.2          (catalog asks for 1.11.0)
:serverpackcreator-app:test             108 tests, 16 failed
  java.lang.NoSuchMethodError: kotlinx.coroutines.BuildersKt.runBlockingK(...)
```

and **at commit 4**: `runtimeClasspath` differing coordinates `13 of 79` → **`0 of 79`**;
`:serverpackcreator-app:test` `16 failed` → **`0 failed`**. Both commit messages carry the numbers
measured on their own tree.

### M4 — accepted, not fixed

`parallelMap`'s behavioural contract changed on published API (single-thread confinement → shared
pool). This is **Griefed's explicit decision**, taken before the work started: presented as a choice
between fixing in place, deprecating with `ReplaceWith`, and deleting outright, and answered "fix in
place, keep the signature". It is recorded in the root `CLAUDE.md` API-compatibility table per the
project's own "source-compatible is not behaviour-compatible" policy, with the race exposure spelled
out. Signature and source compatibility are unchanged; the function has zero in-repo callers.

Left in the report rather than dropped because the risk itself does not go away by being approved:
an embedder whose lambda mutated shared state without synchronisation was previously serialised by
accident and can now race.

---

## Deliberate deviations retained

### D1 — two commits are intentionally red

`e55ba8947` (failing guards) and `e55ddfe8e` (catalog bump) do not pass their suites.

- The first is mandated by the project's own convention: *"Pin first means commit first… The failing
  test lands in its own `test(...)` commit, red, and the fix follows in the next one."*
- The second is the same discipline applied to a build change. The bump **cannot** be green — Boot's
  BOM forces coroutines back to 1.10.2 while `-api` compiles against 1.11.0 — so the only
  alternatives were to squash it into its fix (recreating H1) or to leave the fix's measurement
  unreproducible (recreating H2).

**Cost, stated plainly:** `git bisect` across this branch will land on a red commit twice, and a
rebase-merge puts two red commits on `develop`. A squash-merge does not. If that trade is unwanted,
the remedy is to squash 3 into 4 and 1 into 2 at merge time — which reintroduces H1/H2 in the
history but keeps them out of `develop`.

### D2 — one commit spans two modules

`e7320796a` edits both `serverpackcreator-app/build.gradle.kts` (the fix) and
`serverpackcreator-api/build.gradle.kts` (a comment). The comment asserted that mockk is
single-versioned across the build; that statement was false until this commit and true after it, so
the two belong together. Splitting them would produce a commit whose only content is a comment that
is wrong at the moment it is written.

---

## Clean — verified this pass

- **One concern per commit.** Version bumps, build mechanism, the mockk defect, the API fix and the
  documentation are now five separate commits.
- **No commit is mislabelled `refactor:`.** All are `test:`, `build:`, `fix:` or `docs:`; every
  behaviour change is labelled `fix:` or `build:`.
- **No existing test's assertion, argument or expected value was modified.** The only test change on
  the branch is additive.
- **Both guards were watched failing before their fix**, and the leak guard was then hardened after
  an audit found it could pass while leaking — being red once is necessary, not sufficient, and that
  lesson is now in `serverpackcreator-api/CLAUDE.md`.
- **Bugs surfaced, not worked around.** Three were found and each got its own commit or an explicit
  record: the thread leak, the BOM downgrade, the mockk split.
- **Module boundaries intact.** No Swing, Spring-web or frontend dependency reached `-api`.
- **Kotlin idioms.** No new `!!`, no `var` where `val` suffices; every new and changed declaration
  carries a doc comment; the touched file's wildcard import was narrowed.

---

## Verification at `1cc6c4b1a`

| Module | Tests | Failures | Skipped |
|---|---|---|---|
| api | 309 | 0 | 1 |
| clientside | 88 | 0 | 0 |
| app | 108 | 0 | 0 |
| plugin-example | 3 | 0 | 0 |
| grinder | 233 | 0 | 19 |
| **Total** | **741** | **0** | **20** |

`:serverpackcreator-app:bootJar` packages successfully. `-api` vs `-app` resolved
`runtimeClasspath`: **0 of 79** shared coordinates differ. `testRuntimeClasspath`: 3 of 102 differ,
all cases of `-app` resolving *higher* (byte-buddy 1.18.10, asm 9.7.1) from test dependencies `-api`
does not have — correct conflict resolution, not drift.

---

## Outstanding — not addressed by this branch, no action taken

- `springGradle` (4.0.2) still lags `springBoot` (4.1.0). Harmless now that the BOM comes from the
  catalog rather than the plugin, but the two naming the same product at different versions is a
  latent trap. Griefed's call.
- The IDE's Gradle project model is stale — `.idea/libraries` is empty and three modules are absent
  — which is what produced the phantom "unresolved `CoroutineContext`" errors that opened this work.
  Not a repository problem; a Gradle reload in IntelliJ fixes it.
