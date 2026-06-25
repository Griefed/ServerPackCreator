# Audit — branch `claude-clientside-verify` (2026-06-25)

**Scope:** `git log develop..HEAD` — **1 commit**, `13d8c4fbf`
(*feat(app): automate clientside-mod request verification & acceptance*). Audited against the
Refactoring Conventions. **Source was NOT modified** (this report only).

**Remediation status (2026-06-25, after maintainer go-ahead):** **L1 fixed**, **L2 confirmed-covered**,
**M2 addressed** (pure logic extracted + tested; remainder accepted as integration-only), **M1 accepted**.
Remediation is staged in the working tree to land as a **separate follow-up commit** (kept apart from
the feature commit, per one-concern-per-commit). Details inline below.

**Nature of the branch:** this is an **additive feature**, not a refactor. No existing tested unit was
restructured. Changes to pre-existing files are additive only — `Mode.kt` (+24/-0),
`CommandlineParser.kt` (+81/-0), `InteractiveCommandLine.kt` (+8/-0), `build.gradle.kts` (+3/-0), and
`ServerPackCreator.kt` (+33/-1, the single deletion being the `when (mode)` header line extended
in place with the new modes — **no existing mode's behavior changed**). All new logic lives in
`serverpackcreator-app` (`clientside/` package + 4 CLI verbs) plus 3 new `clientside-*` workflows.

## HIGH — none

- **Module boundary intact:** the domain core `serverpackcreator-api` is **untouched**; no Swing /
  Spring-web / frontend inward dependency was introduced; the new `playwright` + `java.net.http`
  dependencies are confined to `-app`. ✅
- **Plugin-API contract unchanged** (no `-api` change). ✅
- **No behavior change mixed into a refactor** — there is no refactor; existing-file edits are purely
  additive and existing modes dispatch unchanged. ✅

## MEDIUM

- **M1 — One concern per commit. → ACCEPTED.** `13d8c4fbf` (whole commit, ~3.5k LOC) bundles three
  distinct phases (metadata signal / server-boot signal / `accepted`→PR), all their tests, **and** an
  in-development bugfix into a single commit. The conventions call for one concern per commit and
  separating "add tests" from feature work; ideally this would be ≥3 commits. **Accepted by the
  maintainer** — the single feature commit was intentional; the change is purely additive (no existing
  behavior altered), so bisection risk is contained to the new feature.
- **M2 — Test gap on integration seams. → ADDRESSED (remainder accepted).** *Done:* the pure
  file/version-selection logic was extracted from `BootVerifier` into `BootCandidateSelector`
  (behavior-preserving move) and unit-tested (`BootCandidateSelectorTest` — Minecraft ordering,
  bootable-candidate fallback, dependency-file selection); the `BrowserDownloader` no-page-URL
  short-circuit is now tested without launching Chromium (`BrowserDownloaderTest`). *Accepted as
  integration-only:* the live boot (`BootVerifier.boot` generate + `start.sh`), the live Playwright
  download, and the `ServerPackCreator.kt` dispatch wiring remain uncovered — booting a Minecraft
  server / launching a browser is too heavy and network-dependent for the offline unit suite, and is
  exercised by the `clientside-boot` workflow instead.

## LOW

- **L1 — New `!!` non-null assertion. → FIXED.** `clientside/ClientsideListEditor.kt:69` previously
  used `kotlinEntryLine.find(lines[first])!!.groupValues[1]`; replaced with
  `…?.groupValues?.get(1) ?: "<12-space indent>"`. No `!!` remains in the new main source.
- **L2 — In-development bug folded into the feature commit (observational). → COVERED.** The
  `serverpackcreator.properties` last-entry `,\` continuation-corruption bug (would bleed into the next
  property) was found during local validation and fixed in `ClientsideListEditor.addToProperties`. It
  is pinned by the regression test
  `ClientsideListEditorTest.appendingPropertiesEntryGivesPreviousLastABackslashButNotTheNewLast`. The
  "surface a bug in its own commit" rule targets *pre-existing* bugs found during refactor; this was a
  defect in new, not-yet-committed code (strictly N/A) — the regression test is the durable safeguard.

## N/A — refactor-specific conventions (no refactor in scope)

- **Characterization-before-refactor:** no existing tested unit was refactored. The one pre-existing
  tested unit touched (`CommandlineParser`) already had `CommandlineParserTest`; new branches were
  added **with** new tests in the same commit, and the existing assertions were left **unchanged**
  (diff is additive-only — verified, no deletions).
- **Pure-refactor-green-with-existing-assertions:** no pure-refactor commit exists. All prior
  `CommandlineParserTest` assertions still pass unmodified.
- **Strangler-Fig / no big-bang:** N/A — this is a new feature, not a module rewrite. (It was *built*
  in phases but *landed* as one commit; see M1.)

## Not findings (explicitly cleared)

- `private var` on picocli `@CommandLine.Option` fields (`ScanCommand`, `ClientsideReportCommand`,
  `VerifyClientsideCommand`, `ClientsideApplyCommand`) is **required** by picocli's field injection —
  not a var-over-val violation.
- `private var playwright/browser` in `BrowserDownloader` is legitimately mutable (lazy creation, reset
  to `null` on `close()`).
- Boy-Scout / scope sprawl: none — all changes confined to `-app` + the 3 clientside workflows; no
  unrelated files touched.

---

# Refactor audit — full range (Phase 0 → HEAD)

**Scope:** `git log 69a587b6..develop` — 66 commits, base `69a587b6a` (*Phase 0: baseline*, the
first claude-refactored commit), tip `41b17afc9` (*Merge branch 'claude-fix-settings-dirty-icon'
into develop*). Audit against the Refactoring Conventions. **Source was untouched except for the M3
remediation** (a unit test added for `ComponentCoroutineScope`; see M3 *Remediation*).

All 15 `claude-`prefixed branches are merged into `develop` (verified: each tip is an ancestor of
`develop`); nothing was left unmerged. The only unmerged branch in the repo is `backup-pre-rewrite`,
a pre-rewrite safety snapshot (17 ahead / 38 behind) — deliberately **not** merged.

This audit supersedes the previous `REFACTOR-AUDIT.md`, which covered only `69a587b6..37bd79184`
(48 commits, through the Phase 4 frontend merge). That analysis is retained verbatim below; the
**§ Post-37bd79184 commits** section adds the 18 commits since (Phase 4e frontend coverage, the GUI
structured-concurrency refactor, and the settings dirty-icon fix). The Phase 4 branch findings are
under **§ Phase 4 (already remediated)**; §HIGH/§MEDIUM/§LOW cover Phases 1–3 (api / app /
plugin-example).

**Phase map of the range:**

| Phase  | Commits                             | Theme                                              | Verdict                         |
|--------|-------------------------------------|----------------------------------------------------|---------------------------------|
| 0      | `69a587b6`                          | baseline + Kover                                   | clean                           |
| 1a     | `c2b708e60` `df2eab16e` `6cfaaab0b` | characterization tests + 3 bugfixes                | **2 mixed-concern**             |
| 1b     | `a6884f593`…`bea19210d` (+merges)   | ApiProperties → 8 settings-groups                  | clean (model Strangler-Fig)     |
| 1c     | `a35f3cda6` `50165a44f`             | ConfigurationHandler → validators/parser/inspector | **1 bugfix-in-refactor (HIGH)** |
| 1d     | `b0dc98131`                         | ServerPackHandler → pipeline steps                 | clean                           |
| 1e     | `3e4b26d3f` `ae69b89b2`             | regex consolidation, reach-back removal            | LOW (multi-concern)             |
| 2a/2b  | `5d571b973`…`aef54fe1f`             | app tests, ConfigEditor view-models                | clean (model deferral)          |
| 3      | `9fe67c232`                         | plugin-example test layout                         | clean                           |
| 4a–d   | `7c2e98d69`…`308dbb267`             | frontend Vitest/TS/harness                         | see §Phase 4 (remediated)       |
| 4e     | `704e0f9f7`…`94def0c78`             | broadened frontend component coverage              | **clean (model discipline)**    |
| GUI-SC | `40e2aab26`…`93ae56765`             | GlobalScope → ComponentCoroutineScope (8 commits)  | **1 test-gap (MEDIUM)**         |
| fix    | `bef3c1ee2` `585ecb2b9`             | settings dirty-icon stuck-on bugfix                | clean (standalone fix)          |

No pure-refactor commit in Phases 1b/1c/1d/1e/3 modified a **pre-existing test file's assertions** —
verified mechanically (`git show --name-status`). That is the strongest signal in the range: the
extractions kept the existing suite green unchanged, exactly as the convention requires.

---

## HIGH

### H1 — Behavior bugfix folded into an extraction refactor (`50165a44f`, Phase 1c)
**File:** `serverpackcreator-api/.../config/ConfigurationHandler.kt` — `isZip()`, the
`server.properties` branch (now `ConfigurationHandler.kt:490`).
**Rule broken:** "If you find a bug while refactoring, surface it explicitly and propose a fix in
**its own commit**." + "Never mix a refactor with a feature or bugfix." (rubric: *behavior change
mixed into a refactor* → HIGH).

The commit's stated job is a pure extraction — pulling `ModloaderValidator`,
`InclusionsValidator` and `ModpackDirectoryValidator` out of `ConfigurationHandler` (217-line
production diff, behind facades). Inside that same commit it also flips a real runtime bug:

```
-            packConfig.serverIconPath = file.absolutePath
+            packConfig.serverPropertiesPath = file.absolutePath
```

A `server.properties` discovered in an extracted modpack was being assigned to `serverIconPath`,
so the icon got overwritten and the properties file was never picked up. This is an **observable
behavior change** riding inside a commit labelled (and otherwise genuinely) a pure refactor — a
bisect of the validator extraction cannot be separated from "changed which field server.properties
lands in."

**Mitigation:** the bug **was surfaced** in the commit message (not silently worked around), which
is the convention's most important requirement. The defect is in the *current* tree (correct now).

**What the convention wanted:** land the `isZip` field-swap as its own `fix:` commit — ideally
preceded by a characterization test asserting the old buggy mapping, then flipped — before or after
the extraction. Process-only; no code change requested.

---

## MEDIUM

### M1 — API-visible bugfix mixed into an "add tests" commit; the "characterization" test pins the *fixed* behavior (`c2b708e60`, Phase 1a)
**Files:** `ConfigurationHandler.kt` `getModLoaderCase()` (logic now in
`ModpackManifestParser.kt:439`); test `ConfigurationHandlerCharacterizationTest.kt`.
**Rules broken:** "One concern per commit" (add-tests vs change-behavior) **and** "ensure
characterization tests exist that pin its **current** behavior" — the test here pins *new* behavior.

The commit adds 30 characterization tests (75 → 105) **and** rewrites `getModLoaderCase`'s branch
order, fixing two bugs: `"legacyfabric"` was detected as `Fabric` (the `contains("fabric")` branch
matched first), and the NeoForge branch used `contains("NeoForge")` on an already-`lowercase()`d
string so it could never match. After the fix, `getModLoaderCase("legacyfabric")` returns
`"LegacyFabric"` instead of `"Fabric"` — and the new test asserts exactly that *post-fix* value:

```
assertEquals("LegacyFabric", configurationHandler.getModLoaderCase("legacyfabric"))   // new behavior
```

So the test does not *characterize* the legacy behavior — it locks in the corrected behavior, in
the same commit that changes it. The output of a **public API method** changed.

**Why MEDIUM, not HIGH (borderline):** `getModLoaderCase` is part of the exported API surface, so
strictly this is a changed-API-contract change (which the rubric flags HIGH). It is held at MEDIUM
because (a) it is a pure correctness fix to a self-evidently broken branch — no plausible plugin
depends on `"legacyfabric" → "Fabric"`, and (b) the fix was openly surfaced in the message. The
real defect is process: bugfix + tests in one commit, and a "characterization" test that asserts
new rather than current behavior.

### M2 — Two web-contract bugfixes mixed into an "add tests" commit (`6cfaaab0b`, Phase 1a)
**Files:** `app/web/stats/StatsController.kt:141` (route), `app/web/SettingsController.kt:79-83`
(`@get:JsonProperty`).
**Rule broken:** "One concern per commit." + surface/fix a bug in its own commit.

This commit adds the standalone-MockMvc suites for six controllers (5 → 39 app tests) **and** ships
two behavior/contract changes:
- **Route change:** the server-pack download-history endpoint was mapped to
  `/downloads/modpacks/{id}`, colliding with the modpack-history route and unreachable; it is moved
  to `/downloads/serverpacks/{id}`. This is an **HTTP-contract change**.
- **Serialization change:** `@get:JsonProperty("isZipFileExclusionEnabled")` /
  `("isAutoExcludingModsEnabled")` restore the `is`-prefixed JSON field names Jackson had stripped,
  which the frontend `setting-store` reads. This is a **wire-format/contract change** the SPA
  depends on.

Both were surfaced in the message (good) and both are correctness fixes, but they are public
behavioral contract changes folded into a commit whose stated concern is "add characterization
tests." Like M1, the new controller tests assert the *post-fix* routes/field-names, so they pin the
corrected contract rather than characterizing the broken one. Held at MEDIUM (web contract, not the
plugin API; openly disclosed; bundled with tests rather than with a refactor).

### M3 — Behavior-affecting GUI change labeled `refactor`, shipped with no characterization tests (`40e2aab26`…`93ae56765`, GUI structured-concurrency)
**Files:** all 8 GUI commits; new core `app/gui/utilities/ComponentCoroutineScope.kt`.
**Rules broken:** "ensure characterization tests exist that pin its current behavior … never refactor
untested code blind" **and** "keep refactor (no behavior change) and change behavior in separate
commits" (the `refactor(gui)` label vs. an admitted behavior change).

The 8-commit series replaces all 26 `GlobalScope.launch` sites with launches on a lifecycle-owned
`ComponentCoroutineScope` cancelled from `removeNotify()`. This is the right fix for the
structured-concurrency anti-pattern, and it is **commendably partitioned** — one component cluster
per commit (ConfigEditor, ScrollTextArea, IconPreview/SuggestionProvider, inclusions, TabbedConfigsTab,
dialogs, check-timers, ControlPanel), each keeping its original dispatcher and `CoroutineStart`. Two
caveats keep it at MEDIUM:

1. **It is a behavior change wearing a `refactor` label.** Each message says so plainly
   ("Behavior-affecting; needs GUI runtime verification") — coroutines that previously leaked on
   `GlobalScope` now get **cancelled** when the component leaves the screen. That is an observable
   lifetime change, not behavior-preserving; the convention reserves the `refactor` label for
   no-behavior-change commits. (Not HIGH: it is the deliberate, openly-disclosed *point* of the
   change, not a fix smuggled in behind an unrelated extraction.)
2. **No tests pin it.** Swing view code needs a GUI runtime, so the leak/cancel behavior was verified
   manually (per `serverpackcreator-app/CLAUDE.md`) with the app suite green — acceptable for the
   view classes. But `ComponentCoroutineScope` itself is **plain, runtime-free Kotlin** (scope
   re-creation after `cancel`, `isActive` gating, `@Synchronized` access) and was shipped in
   `40e2aab26` with **no unit test**, even though it is trivially unit-testable and is now the single
   point all 26 sites depend on. That is the one genuinely-testable unit in the series left uncovered.

**What the convention wanted:** a unit test for `ComponentCoroutineScope` (cancel → next `scope()`
yields an active scope; cancelled children stop) committed before/with its introduction, and the
view migrations tagged as the behavior change they are. Process + one missing small test; the tree is
correct and GUI-verified.

**Remediation (resolved):** the missing test now exists —
`serverpackcreator-app/src/test/kotlin/.../gui/utilities/ComponentCoroutineScopeTest.kt` pins all
four documented behaviours (scope stability while active, cancellation of in-flight work,
re-creation+usability after `cancel`, and `SupervisorJob` sibling-independence) with no GUI runtime,
app suite green. Caveat 2 is closed. Caveat 1 (the `refactor` label on a behavior-affecting,
already-merged series) is a past commit-message/granularity matter; it is **accepted as-is** —
rewriting merged history to relabel commits is not worth it, and every message already disclosed the
behavior change in prose.

---

## LOW

### L1 — Multiple structural concerns in one commit (`3e4b26d3f`, Phase 1e)
The commit does three separable structural things: (a) introduce `SupportedModloaders` as the single
source of truth for the five loader-regexes and re-point five consumers at it; (b) remove
service-locator reach-backs into the `ApiWrapper` singleton (`ServerPackManifest` self-derives the
version; `PackConfig.save` gains an injected overload with the old one kept as a `@Deprecated`
facade); (c) deprecate `ReticulatingSplines` (later reverted in `ae69b89b2` per Griefed). All three
are behavior-preserving and done behind source-compatible facades (textbook Strangler-Fig), and
they are thematically related ("finish decoupling the API core"), so this is a commit-granularity
nit, not a correctness issue. Ideally (a) and (b) would have been two commits.

### L2 — `ae69b89b2` reverts a same-phase decision (Phase 1e)
The `ReticulatingSplines` deprecation from `3e4b26d3f` is reverted one commit later per Griefed's
direction. Clean and correctly surfaced; noted only as churn that would have been avoided had the
deprecation not been bundled into L1's commit in the first place.

---

## What was done well (not violations)

- **Phase 1b is the model Strangler-Fig.** `ApiProperties` (3,007 → 1,372 lines) was split into
  eight settings-groups across `a6884f593`…`bea19210d`, each commit: write group-tests first → move
  get/set logic verbatim → `ApiProperties` keeps a thin delegating facade → run api+app suites. No
  existing assertion was touched; the facade kept every internal call-site unchanged.
- **Deferred behavior change handled exactly right (`17d636aca` → `d5c8493c1`, Phase 2b).** The
  Phase 2b dirty-check extraction *found* that `InclusionSpecification` has no value-equality
  (so the dirty-check over-reports). Rather than fix it inline, `17d636aca` **surfaced it as a
  pinned characterization finding and explicitly deferred** the fix; the fix then landed in its own
  dedicated behavior-change commit `d5c8493c1`, which (correctly, being a behavior change and not a
  refactor) **flips the quirk-test** to the corrected by-value comparison, adds 4 equality tests,
  and documents the "verified no hash-based collections exist" safety check. This is the textbook
  counter-example to H1 — the same situation, handled by the book.
- **`b0dc98131` (Phase 1d)** split `ServerPackHandler` (1,466 → 490 lines) into
  `ModListCompiler`/`ServerPackFileGatherer`/`ServerPackProvisioner` behind facades, verified by the
  five end-to-end generation tests + Phase 1a characterization tests, no test edits — a clean
  big-unit decomposition done incrementally.
- **`a35f3cda6` (Phase 1c)** removed genuinely dead code (two identical, unreachable
  `mmcPrismPack` `when`-branches) as disclosed Boy-Scout cleanup inside its extraction — behavior-
  preserving, in scope.
- **`0fcd7aac8`** ("Fix overbroad .gitignore that silently dropped `ConfigEditorViewModel`") is a
  standalone, single-concern `fix:` commit — the right shape.
- **Plugin-API stability held (`9fe67c232`, Phase 3):** the example plugin needed **no** production
  changes after the Phase 1 API refactor; the jar was rebuilt and confirmed pf4j-loadable, and the
  API's `ApiPluginsTest` still discovers all six extension points. The compatibility policy was
  honoured.
- **Module boundaries intact:** no inward dependency from `-api` onto Swing/Spring/frontend was
  introduced anywhere in the range; the app/frontend remained adapters around the core.
- **Phase 4e is the discipline H1/M1/M2 lacked — done right.** Characterization tests landed first
  in their own commits (`704e0f9f7`, `4396adace`; suite 12 → 23), *then* the `DrawerLink`
  `colour="accent"` → `color` bug — surfaced while writing that component's test — landed as its own
  dedicated `fix:` commit (`05450f15e`), with the test pinning *rendering* (not colour) so it stays
  green across the fix. Textbook tests-first → isolated-fix sequencing.
- **`bef3c1ee2` (dirty-icon fix) is a clean standalone `fix:`** — one file, root-cause disclosed
  (normalizing getters vs. raw widget values after save), explicitly noted as a pre-existing bug
  "independent of the GUI coroutine-scope refactor," app suite green. Correct commit shape.
- **GUI structured-concurrency partitioning (despite M3):** the 26-site `GlobalScope` removal was
  split into 8 reviewable per-component commits behind one small shared helper, each preserving
  per-site dispatcher/`CoroutineStart` — the *granularity* the convention asks for, even though the
  series carries the M3 label/test-gap caveat.

---

## § Phase 4 (web-frontend) — already audited & remediated

The Phase 4 commits (`7c2e98d69`…`308dbb267`) were audited separately on the
`claude-phase4-frontend` branch and **all findings were remediated before merge** (the cited
pre-rewrite hashes no longer exist; history was rebuilt and verified byte-identical via
`backup-pre-rewrite`). Summary of that audit, for the full-range record:

| Was      | Finding                                                            | Status in current history                                                                                    |
|----------|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| H (4c-6) | `onRejected` bugfix mixed into the TS-conversion commit            | **RESOLVED** — split into `118701e24` (fix) + `caa6bc8af` (refactor); pinned by `87a7cc859` test             |
| M (4a)   | Vitest + dead-code + `refresh()` contract change in one commit     | **RESOLVED** — split into `7c2e98d69` (refactor) + `2d20acf34` (tests)                                       |
| M        | SFCs converted to TS with no characterization tests first          | **RESOLVED** — `SubmitModPackForm` + both download pages now tested (`feca50a25`, `87a7cc859`); suite 6 → 12 |
| L1–L5    | disclosed behavior-equivalent edits inside "convert to TS" commits | left as-is (correct in tree, several now test-covered)                                                       |

The standing cross-module item that audit noted — `ConfigEditor`'s `GlobalScope.launch`
anti-pattern (app GUI) — has since been **resolved** by the GUI structured-concurrency series below
(see M3).

---

## § Post-37bd79184 commits (Phase 4e, GUI structured-concurrency, dirty-icon fix)

The 18 commits merged after the earlier audit's tip. Mechanically verified: in this sub-range test
files were **only added, never modified** (`git log --name-status … -- '*test*'` shows all `A`,
no `M`) — so no pure-refactor commit touched a pre-existing assertion. The three merge commits
(`12bddf10a`, `76a3938eb`, `41b17afc9`) introduce no diff beyond the sum of their branch commits
(develop did not advance between branches).

| Commits                 | Theme                                        | Verdict                                                                |
|-------------------------|----------------------------------------------|------------------------------------------------------------------------|
| `704e0f9f7` `4396adace` | frontend characterization tests (12 → 23)    | clean — tests-first, own commits                                       |
| `05450f15e`             | `DrawerLink` `colour`→`color` fix            | clean — dedicated `fix:`, test stays green                             |
| `94def0c78`             | Phase 4e docs                                | clean — docs only                                                      |
| `40e2aab26`…`a56314d4e` | 8× `GlobalScope` → `ComponentCoroutineScope` | **MEDIUM (M3)** — behavior change as `refactor`; test gap **resolved** |
| `93ae56765`             | GUI structured-concurrency docs              | clean — docs only                                                      |
| `bef3c1ee2`             | settings dirty-icon stuck-on fix             | clean — standalone `fix:`                                              |
| `585ecb2b9`             | dirty-check landmine docs                    | clean — docs only                                                      |

Only new finding: **M3** (above). Everything else in this sub-range is clean and, in the Phase 4e
case, is the model the earlier bugfix-bundling phases should have followed.

---

## Summary

| Severity | Count | Items                                                                                                                                                                                                                                                                                                         |
|----------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| HIGH     | 1     | H1 — `isZip` bugfix folded into the Phase 1c validator extraction (`50165a44f`)                                                                                                                                                                                                                               |
| MEDIUM   | 3     | M1 — loader-detection fix + tests, test pins post-fix behavior (`c2b708e60`); M2 — two web-contract fixes bundled with controller tests (`6cfaaab0b`); M3 — GUI `GlobalScope` removal labeled `refactor` + `ComponentCoroutineScope` shipped untested (`40e2aab26`…`93ae56765`) — **test gap since resolved** |
| LOW      | 2     | L1 — multi-concern Phase 1e commit (`3e4b26d3f`); L2 — same-phase revert (`ae69b89b2`)                                                                                                                                                                                                                        |

**Dominant theme: process, not correctness — and a discipline that visibly improved over time.**
The recurring early pattern is the *first* commit of a phase (1a) and the *first* extraction of a
cluster (1c) bundling a freshly-discovered bugfix in with tests or with the extraction, and — in the
test cases — writing the new test against the *fixed* behavior so it documents the new state rather
than characterizing the old one. The later GUI series adds a different flavour (M3): a deliberate,
disclosed behavior change carrying the `refactor` label with its one unit-testable unit left
uncovered. Across the whole range every such change was **openly surfaced** in its commit message
(none buried), every fix is **correct in the current tree**, and the trajectory is clearly upward —
Phase 2b's `InclusionSpecification` handling (deferred finding → dedicated behaviour-change commit →
flipped test) and **Phase 4e's tests-first → isolated-fix sequencing** (`05450f15e`) are the model
the earlier bugfixes should have followed, and the GUI work nailed the *commit granularity* even
where it slipped on labeling/tests (the latter, the M3 `ComponentCoroutineScope` test gap, has since
been closed — see M3 *Remediation*). No broken module boundary and no silently-changed plugin-API
contract anywhere in the range.

---

*Originally a read-only audit (no source modified). Following sign-off, the one mechanically-fixable
finding — M3's missing `ComponentCoroutineScope` unit test — was remediated: `ComponentCoroutineScopeTest`
added, app suite green. No other source was touched; the remaining findings are accepted-as-is
process/labeling matters on already-merged history.*
