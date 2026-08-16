# Refactor audit — `claude-coroutines-1.11-fallout` (fourth pass, post-remediation)

**Base:** `a7717e8a9` (`develop`) · **Head:** `57ba3f256` · **Commits:** 16 · **Date:** 2026-08-16
**Supersedes** the third pass, which raised **4 MEDIUM, 2 LOW** against a 12-commit history. Those
commits no longer exist; the three mixed build commits have been split. Prior history is preserved at
`backup-pre-split` (`0b1589559`), `backup-pre-kover-fix` (`a9dbbf3d8`) and `backup-pre-rewrite`
(`b1f831dad`).

**Verdict: no HIGH, no MEDIUM, no LOW open.** The single LOW (L3, pre-existing) was fixed on this
branch at Griefed's request — see below. Every
commit builds under the full `./gradlew build` except the two that are deliberately red, each for a
reason stated in its own message.

---

## History

| # | Commit | Subject | Build |
|---|---|---|---|
| 1 | `e55ba8947` | `test(api): pin that parallelMap neither leaks a thread nor serialises` | **RED** (intentional) |
| 2 | `ae18f5657` | `fix(api): stop parallelMap leaking a thread per call, and make it parallel` | green |
| 3 | `e55ddfe8e` | `build: bump third-party library versions in the catalog` | **RED** (intentional) |
| 4 | `3ab1abed6` | `fix(build): import Boot's BOM as a platform so the catalog wins` | green |
| 5 | `e7320796a` | `fix(build): declare mockk explicitly in -app so it matches -api` | green |
| 6 | `1cc6c4b1a` | `docs: record the parallelMap fix, the coroutines floor and the BOM landmine` | green |
| 7 | `b33da601d` | `docs: audit the parallelMap/coroutines branch, post-remediation pass` | green |
| 8 | `f4dec992f` | `build: align springGradle with springBoot at 4.1.0` | green |
| 9 | `b4e6fe977` | `build: route nekodetector and the Boot BOM through the catalog` | green — **pure** |
| 10 | `66c77c053` | `build: drop the orphaned io.spring.dependency-management plugin` | green — behaviour |
| 11 | `a8f158865` | `build: add a [plugins] catalog section and alias it from the build scripts` | green — **pure** |
| 12 | `6325735c9` | `build: consume plugin markers in buildSrc instead of implementation artifacts` | green — behaviour |
| 13 | `96cb68461` | `build: bump Kover to 0.9.9` | green — behaviour |
| 14 | `f0bf0034e` | `build: bump the Kotlin compiler to 2.4.10` | green — behaviour |
| 15 | `2be03f8d0` | `build: collapse the four Kotlin entries onto one version ref` | green — **pure** |
| 16 | `57ba3f256` | `docs: record the catalog, [plugins] and Kotlin-unification work` | green |

Commits 9–16 replace the former 9–12. The split is content-preserving: the tree at `2be03f8d0` is
byte-identical to `backup-pre-split`, excluding only the two generated LICENSE artifacts and this
document.

---

## Third-pass findings — disposition

| ID | Sev | Finding | Status |
|---|---|---|---|
| M1 | MED | Kotlin commit mixed a compiler upgrade with a pure ref collapse | **FIXED** — `f0bf0034e` (bump) + `2be03f8d0` (collapse) |
| M2 | MED | `[plugins]` commit mixed alias conversion with a buildSrc classpath change | **FIXED** — `a8f158865` (pure) + `6325735c9` (behaviour) |
| M3 | MED | "Three leftovers" bundled two pure changes with one behavioural | **FIXED** — `b4e6fe977` (pure) + `66c77c053` (behaviour) |
| M4 | MED | Committed audit was stale | **FIXED** — this pass, committed alongside the history it describes |
| L1 | LOW | Documentation bundling inconsistent | **FIXED** — documentation for 9–15 collected into `57ba3f256`, matching commits 6–7 |
| L2 | LOW | Frontend/Kover never exercised; `./gradlew build` never run | **FIXED — and it had already bitten** (below) |

### On M1–M3: what the split actually bought

Each pair isolates the risky half, which is concrete rather than cosmetic:

- `6325735c9` alone carries the buildSrc classpath move (23 → 31 modules, dropping
  `org.jetbrains.dokka:javadoc-plugin`). A bisect landing on a dokka problem now lands on the single
  commit that touched dokka's classpath, not one that also renamed three plugin references.
- `f0bf0034e` alone carries the compiler upgrade. `2be03f8d0` is provably inert: every version value
  unchanged, only the number of places declaring it, with `kotlin-stdlib` still resolving 2.4.10.
- `66c77c053` alone carries the `dependency-management` removal, so "it was applied nowhere" is
  checkable against one diff.

All eight rebuilt commits were verified with a **full `./gradlew build`** — 91 tasks — not a subset.

### On L2: the finding that proved itself within the hour

Filed in pass three as a LOW coverage gap. Griefed then ran `./gradlew build` and it failed at
task-graph time, before anything compiled:

```
Could not determine the dependencies of task ':serverpackcreator-api:koverGenerateArtifactJvm'.
> Could not get unknown property 'compileKotlinTask' for compilation 'main' (target  (jvm))
```

Kover 0.9.1 reads `compileKotlinTask` by reflection; KGP 2.4.10 no longer exposes it, and
`kotlin-conventions` applies Kover to every module — so the Kotlin bump broke the whole build while
every task in the curated verification list still passed. Fixed by `96cb68461`, deliberately placed
*before* the compiler bump, because Kover 0.9.9 supports both compilers and that keeps each commit
green.

**The reusable lesson:** task-level verification systematically under-tests build-plugin
interactions, because a plugin that fails at *configuration* time is invisible to any task list that
omits it. On the Kotlin/Gradle-plugin axis, `./gradlew build` is the minimum bar — not a curated set,
however thorough it reads in a commit message.

---

## LOW (fixed)

### L3 — `dokkaGeneratePublicationHtml` had an undeclared task dependency (PRE-EXISTING)

Running `dokkaGeneratePublicationHtml` alongside the javadoc publication from a wiped `build/dokka`
fails deterministically (3 of 3 attempts):

```
Task ':serverpackcreator-api:dokkaGeneratePublicationHtml' uses this output of task
':serverpackcreator-api:compileJava' without declaring an explicit or implicit dependency.
```

`serverpackcreator-api/build.gradle.kts` declares `dependsOn(generateI18n4kFiles,
fixMissingResources)` on that task, but not the Java compilations whose `build/generated` output it
reads.

**Confirmed pre-existing and unrelated to this branch:** the identical failure reproduces on
untouched `develop` (`a7717e8a9`) in a clean worktree. It never surfaced in normal use because
`build` runs only the javadoc publication (via `finalizedBy`), never the HTML one.

**FIXED** at Griefed's request by `dbcb80caf`, in `dokka-conventions` so every module applying the
convention benefits. The Javadoc publication already carried exactly this `dependsOn` — only the HTML
half lacked it, making this the second occurrence of one bug, so the two are now configured together
rather than side by side. Measured on the previously-failing command from a wiped `build/dokka`:
**3 of 3 FAILED before, 3 of 3 SUCCESSFUL after**, 185 `index.html` generated.

---

## Clean — verified this pass

- **One concern per commit.** Behaviour and pure-structure changes are separated throughout, and each
  pure commit states what must not move and demonstrates it did not.
- **No commit is labelled `refactor:`.** All 16 are `test:`, `build:`, `fix:` or `docs:`.
- **No existing test's assertion, argument or expected value was modified.** The only test file
  touched on the branch is `ListUtilitiesTest.kt`, additively.
- **Every build commit carries its measurement**, re-measured on its own tree during the split rather
  than copied forward.
- **Bugs surfaced, not worked around** — six on the branch: the thread leak, the BOM downgrade, the
  mockk split, the hardcoded nekodetector coordinate, the orphaned dependency-management plugin, and
  the Kover/KGP incompatibility. Plus L3, reported rather than quietly patched.
- **Module boundaries intact**; no Swing, Spring-web or frontend dependency reached `-api`.
- **Catalog hygiene:** 49 libraries, 12 plugins, zero unused aliases, zero unreferenced `[versions]`
  entries, zero `FAILED` markers across every configuration in every module.

## Carried forward — accepted, unchanged

- **Two commits are intentionally red** (`e55ba8947`, `e55ddfe8e`), so a failing guard and a breaking
  bump are checkable from the commit that causes them. They are now 15 and 13 commits deep; a
  `git bisect` hits a red commit twice, and a rebase-merge puts both on `develop`. A squash-merge does
  not. Still Griefed's trade to make.
- **`parallelMap`'s published behavioural contract change** — Griefed's explicit decision, recorded in
  the API-compatibility table. An embedder whose lambda mutated shared state without synchronisation
  was previously serialised by accident and can now race.

---

## Verification at `57ba3f256`

`./gradlew build` — **BUILD SUCCESSFUL, 91 tasks.** That graph covers 741 JVM tests (api 309,
clientside 88, app 108, plugin-example 3, grinder 233; 0 failures, 20 skipped), every Kover report,
`bootJar`, both dokka publications, `sourcesJar`, `generateLicenseReport`, and the frontend —
`installFrontend` / `assembleFrontend` / `checkFrontend`, the last running Vitest via `npm run test`.

Only install4j's `media` task remains unexercised; it needs a local install4j installation and is
documented as outside the development loop.

---

## Remaining decisions for Griefed

1. **Merge strategy.** A squash-merge keeps the two deliberate red commits off `develop`; a
   merge commit or rebase does not.

Nothing else is outstanding, and no remediation is proposed for this branch.
