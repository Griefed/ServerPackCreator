# Refactor Audit — `claude-backlog-followups` (`develop..HEAD`)

**Range audited:** `develop..HEAD` — **13 commits**, `21f9d1ea8` … `7a8a52a4b`
**Base:** `develop` — verified ancestor, clean linear range
**Mode:** READ-ONLY. No source modified.
**Suite at HEAD:** api 278 (1 skip) · clientside 87 · grinder 233 (19 skip) · app 76 — all green

> Fifth report at this path. Supersedes the `claude-audit-backlog-cleanup` audit (0 HIGH / 4 MEDIUM / 3 LOW, all
> remediated), whose text is in git at `fce979685`. Its still-open item is carried forward below.

**Findings: 1 HIGH · 2 MEDIUM · 2 LOW.**

**Status 2026-07-31 — all five remediated.** **H-1** removed from `7a8a52a4b` by amend (`826822dbd`): the change is
absent from the commit *and* from branch history, and Griefed's edit is back in the working tree, byte-identical and
uncommitted, for him to decide on. **M-1** tabled (`b9609860b`). **L-2** resolved by correcting a false claim rather
than deleting the API (`553671a18`) — the finding was "never called from production", and inspecting why revealed the
real harm: a test comment asserting `hasVerdictFor` drives the skip check, which it does not. **M-2** and **L-1**
closed as a recorded decision (`fa898071f`): build logic has no test harness here, so the standard is an explicit
before/after measurement in the commit message, which both flagged commits already contained.

The six backlog items each landed as a red `test(...)` commit followed by its fix — `a758dae64`→`0dacd5a58`,
`bde5d16fc`→`150d66214`, `6b122bb36`→`5ce44e3e0`, `4eb556ec1`→`7ed63978c`, `5c1a3ce3f`→`7a8a52a4b` — with the observed
failure quoted in each message, and four guards additionally verified by breaking the fix. No new `!!`, no debug
left behind. The HIGH is not a design failure but a staging one, and it is the most serious finding in five audits
because it put someone else's unreviewed work into a commit of mine.

---

## HIGH

### H-1 — `7a8a52a4b` commits an unrelated, uninspected build change swept up by `git add -A`
**Commit:** `7a8a52a4b` *"feat(grinder): dedup verdicts by project identity instead of slug"*
**File:** `buildSrc/src/main/kotlin/serverpackcreator.quasar-conventions.gradle.kts:26`
**Rule broken:** *One concern per commit* / *Boy Scout rule — stay within the commit's stated scope.*
**Severity:** HIGH — a behaviour change to the frontend build, smuggled into a grinder feature commit whose message
never mentions it, and **not authored by the committing session**.

The commit staged with `git add -A` after twelve commits of explicit path staging. It swept in:

```diff
 tasks.register("installQuasar", RunNpmTaskType::class) {
-    dependsOn("installCorepackLatest")
+    //dependsOn("installCorepackLatest")
```

Three things make this worse than ordinary sprawl:

1. **It is not this session's change.** Nothing in these 13 commits touches the frontend build; the file was open in
   Griefed's IDE at the time. This is his working-tree edit, committed under a message about verdict dedup and
   attributed to a co-authored grinder change.
2. **It is functional, and plausibly breaking.** `installCorepackLatest` exists *because* of an upstream Corepack
   bug — it is backlog **B11**, the item that survived this whole cleanup. Commenting out the `dependsOn` removes the
   workaround from every frontend build. Whether that is a deliberate experiment or a stray keystroke is not knowable
   from here, which is precisely the problem.
3. **The grinder suites cannot catch it.** The four Kotlin suites that were run to green this branch do not build the
   frontend, so "all four suites green" is true and irrelevant to this change.

**Remediation is available and cheap** — the branch is unpushed. Revert that one file out of `7a8a52a4b` (or split it
into its own commit) and hand the edit back to the working tree, so Griefed decides whether it is wanted and it lands
with a message that says what it does. **Do not simply keep it**: an unreviewed build change riding in someone else's
commit is how a frontend break gets bisected to a grinder feature three months from now.

**Rule for future work:** stage by explicit path. `git add -A` is unsafe in a shared working tree, and the twelve
commits before this one demonstrate the alternative costs nothing.

---

## MEDIUM

### M-1 — `0dacd5a58` changes what eight exported properties return, with no compatibility-table row
**Commit:** `0dacd5a58` *"fix(api): derive the shipped template paths on access, not at construction"*
**File:** `serverpackcreator-api/src/main/kotlin/.../settings/PathsConfig.kt:586`, `:594`, `:603`, `:611`, `:619`,
`:627`, `:635`, `:643`
**Rule broken:** the project's own policy — *"a change that keeps every signature but alters what an exported call
returns is still a contract change for embedders"* (`CLAUDE.md:87-95`).

The eight template properties went from `val x = File(…)` to `val x: File get() = File(…)`. For a plugin reading
`apiProperties.defaultShellScriptTemplate`, a previously constant value can now change between two calls — the point
of the fix, and exactly the kind of thing the table exists to record. The table gained a row for **B23** in
`150d66214`, two commits later, for a *narrower* change to the same module. B21 got none.

Third recurrence of this class: `variables.txt` (HIGH-B, third audit), `MinecraftServer` logging (M-2, fourth audit),
this. **The pattern is the finding**, not the individual omission.

Graded MEDIUM rather than HIGH deliberately, and the reasoning matters so this does not read as going soft: unlike
`variables.txt`, which changed the content *every* installation gets, this value is identical for a stable home and
differs only when the home changes mid-process. Narrow trigger, real contract change. **Remediation:** one table row.

Verified while auditing, and worth recording as a positive: all eight `ApiProperties` facades (`:801`–`:836`) are
computed getters, so the fix genuinely reaches embedders rather than being masked by a caching facade.

### M-2 — `8b87057cf` changes build behaviour with no automated guard
**Commit:** `8b87057cf` *"fix(build): spare the version-manifest cache when wiping the test home"*
**File:** `buildSrc/src/main/kotlin/serverpackcreator.java-conventions.gradle.kts` (`cleanup()`)
**Rule broken:** *Ensure characterization tests exist … never refactor untested code blind.*

The only commit in the range with a production change and no accompanying pin — the pairing table above shows it
standing alone between two test/fix pairs. It was verified empirically and the measurement is in the message (a
planted marker plus a cached manifest; the cache went 643 → 0 before, survived after), which is why this is MEDIUM
rather than worse. But nothing stops the exclusion being dropped, and the symptom is invisible on any machine whose
home is already warm.

`ShippedManifestSnapshotTest` (`4eb556ec1`) partially covers the *consequence* — a snapshot falling behind — but
nothing asserts that a test run preserves `manifests/`. The repo has no Gradle TestKit harness, so a real guard means
introducing one; that is a legitimate reason to defer, not a reason to call it pinned.

---

## LOW

### L-1 — `15111f757` retargets a Copy task with nothing asserting the new source
**Commit:** `15111f757` · **File:** `serverpackcreator-api/build.gradle.kts` (`updateManifests`)

Correct and well-evidenced in the message (app home 643 files vs api home 659, the difference being exactly the 16
missing releases), but a future edit could point it back at a directory that never holds fresh data and no test would
notice. Same TestKit gap as M-2; LOW because the failure is loud the moment someone runs the refresh and the snapshot
does not move.

### L-2 — `7a8a52a4b` extends an API that production never calls
**Commit:** `7a8a52a4b` · **File:** `.../grinder/report/VerdictStore.kt:46`

`hasVerdictFor` gained the `projectId` parameter, but no production code calls it — only tests. The behaviour that
actually matters flows through `newestVerification`, which *is* wired (`Grinder.kt:62`). Pre-existing dead surface,
now slightly larger. Either wire it or drop it; leaving it invites a future reader to assume the crawl's
already-ground check goes through it.

---

## Carried forward

| Finding | Severity | Status at this HEAD |
|---|---|---|
| **H-A** `origin/develop` published with a failing `ConfigEditorViewModelTest` | HIGH | **STILL OPEN, five audits deep.** The fix `34464832e` is unpushed behind 131 commits on `develop`, plus these 13. The only finding across all five that cannot be closed locally. |
| Fourth audit: M-1…M-4, L-1…L-3 | — | All remediated on the previous branch (`fce979685`), or recorded as historical. |
| B11 Corepack workaround | — | Still the sole backlog entry — and see **H-1**, which touched exactly that mechanism without meaning to. |

---

## What this branch got right

- **A garbled test was corrected instead of the implementation being bent to match it.**
  `theSameSlugOnDifferentPlatformsStaysSeparate` asserted `assertFalse(hasVerdictFor(…).not())`, a double negative
  that failed against *correct* behaviour. The fix was to the test, and its RED commit was amended rather than a
  green-looking patch layered on top.
- **Two entries were corrected rather than executed as written.** B22 claimed the working directory had been ruled
  out (it had not — that conclusion rested on an `lsof` reading that did not mean what it was taken to mean), and B24
  claimed the manifests were not shipped (they are). Both were re-derived from evidence before any code changed.
- **A migration was designed away rather than written.** B5 makes the id nullable with slug fallback *and* supersedes
  the id-less row on record, so ~870 live verdicts converge as projects are re-ground — no schema step, no rewrite of
  a running store.
- **Guards verified by breaking them** on B21, B23, B22 and B5, each time confirming the edit applied before trusting
  the result — the failure mode that produced two false "passes" earlier in this work.

---

## Recommended order of action

1. **H-1 — get the quasar-conventions change out of `7a8a52a4b`** and back into the working tree. Cheap now,
   archaeology later. Then confirm with Griefed whether disabling `installCorepackLatest` is wanted at all.
2. **M-1 — add the compatibility-table row** for the eight template properties. One row, and it closes the third
   recurrence of a class this project has a written policy for.
3. **Push** (**H-A**). Five audits, 144 commits.
4. **M-2 / L-1** — decide whether a Gradle TestKit harness is worth introducing for build-logic guards; if not,
   record that as the deliberate ceiling so the gap stops being re-flagged.
5. **L-2** — wire or drop `hasVerdictFor`.

---

**Report only — no source modified. Awaiting go-ahead.**
