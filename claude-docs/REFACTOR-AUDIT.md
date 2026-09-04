> **This is the single home for audit reports.** Newer sections are appended at the end, so the file reads
> chronologically. It was split for a while: three June audits lived here while fifteen later ones
> accumulated in a `REFACTOR-AUDIT.md` at the repository root, which is where the audit command writes by
> default. The root copy was merged in below on 2026-08-21 and deleted; nothing was dropped.
>
> Findings here are **closed unless a section says otherwise** — the fixes are in `git log`, and the
> durable conclusions are in `REFACTOR-LOG.md` and the module `CLAUDE.md` files. This file is the
> *evidence*: mutation results, measurements, and the "verified clean, do not re-litigate" lists.

# Audit — branch `claude-workflow-audit` (2026-06-25)

**Scope:** `git log develop..HEAD` — **0 commits.** The branch's work (a security/correctness audit of
the six pre-existing CI workflows + its remediation) is **entirely uncommitted** in the working tree:
`.github/workflows/{devbuild,github-prerelease,github_release,test,update_readme,virustotal}.yml`
modified, `claude-docs/WORKFLOW-AUDIT.md` new. Source was **not** modified by this audit (report only).

**Applicability:** these are **GitHub Actions YAML** changes, not application-code refactoring. The
code-centric conventions — characterization-tests-before-refactor, Strangler-Fig, module boundary /
plugin-API, Kotlin idioms — are **N/A** (no Kotlin/JVM source touched). What *can* be judged is commit
hygiene (one concern per commit, bug-in-its-own-commit, boy-scout/scope) — and since nothing is
committed yet, those are **forward-looking guidance for the pending commit(s)**, not violations in
history. The substantive findings + remediation detail live in `claude-docs/WORKFLOW-AUDIT.md`; this
entry only assesses convention-compliance.

## HIGH — none
No application behavior changed; no module boundary or plugin-API touched (no code in scope).

## MEDIUM (commit-hygiene guidance for when this is committed)

- **MED-1 — Bugfixes bundled with hardening; would violate "one concern per commit".** The working tree
  mixes three distinct concerns that the conventions say to separate:
  (a) the **audit report** (`claude-docs/WORKFLOW-AUDIT.md`);
  (b) **correctness bugfixes** to existing workflows — **M1** `github_release.yml:261` (added
  `needs: [preparations]`; the job was building docs with `-Pversion=""`) and **M2**
  `github_release.yml:167` (removed a dead `steps.preinfo` reference);
  (c) **security hardening** (H1 tj-actions removal, H2 permissions, H3 SHA-pinning, M4/M5/M6, L2/L3).
  The "surface a bug in its own commit" rule specifically wants (b) isolated. **Recommend** committing
  as ≥3 commits: report → correctness fixes → hardening.

- **MED-2 — A behaviour change rides along with the mechanical hardening (M5), on an untestable path.**
  `update_readme.yml:70-71` rewrites the GitLab push from `git push https://user:token@host` to
  `git -c http.extraheader=… push https://host HEAD:refs/heads/main`. That is a real **behaviour
  change** to the push mechanism (new refspec/auth path), not a pure security tweak, and it is bundled
  with the SHA-pins. Per the "don't mix behaviour-change with mechanical edits" spirit it should be its
  own commit and **verified on the next run** (these workflows only fire on tag-push/release/dispatch,
  so none of this is locally testable).

## LOW

- **LOW-1 — Permission tightening may under-scope actions that relied on the default broad token.**
  `update_readme.yml` now has `permissions: contents: read`; the sponsor/contributor actions previously
  ran with the repo-default token. If either needed a scope beyond `contents: read`, it will now fail.
  Verify on next run. (Same caution applies generally to the new least-privilege blocks — intended, but
  unverifiable offline.)
- **LOW-2 — One self-introduced YAML bug, already fixed, leaves no trace.** The `run: echo "Version: …"`
  added to the `preparations` jobs was an unquoted scalar whose `: ` parsed as a mapping key; fixed by
  switching to a block scalar before any commit. Noted only for completeness (caught by YAML lint).

## Not findings / positives

- **Scope discipline:** all changes confined to `.github/workflows/` + the audit doc — no sprawl into
  unrelated files (boy-scout rule respected).
- **M3 correctly left unchanged** (`if: always()` on release jobs) per maintainer intent — *not* a
  silent work-around; a clarifying comment was added and it's recorded in `WORKFLOW-AUDIT.md`.
- All ten workflows YAML-validate; all third-party actions SHA-pinned; permissions least-privilege.

---

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

---

<!-- Merged in from the repository root on 2026-08-21. Everything below was written there. -->

# Refactor audit — `claude-perf-network-startup`

**Range:** the 6 commits of `claude-perf-network-startup`, `7abd7c85c` to `docs: record the network/startup work and backlog its follow-ups`
**Date:** 2026-08-17 · **Mode:** READ-ONLY. No source modified. Verification used a throwaway git
worktree under the scratchpad, plus hybrid checkouts (fix-commit production + pin-commit tests) to test
the red→green chain independently rather than trusting commit messages.

| Commit | Type | Verdict |
|---|---|---|
| `pin that HTTP calls give up instead of hanging forever` | test(api) | red claim **verified**; chain to its fix **broken** (F1) |
| `bound every HTTP call with configurable timeouts` | fix(api) | central claim **overstated** (F2, F3); everything else verified |
| `extract manifest refreshing into ManifestUpdater` | refactor(api) | verbatim **verified**; visibility widened undisclosed (F4, F5) |
| `pin what a manifest check costs` | test(api) | red claims **verified exactly** |
| `halve startup requests and skip unchanged manifests` | fix(api) | red→green chain **verified clean**; figures internally consistent |
| `record the network/startup work and backlog its follow-ups` | docs | one inaccurate completeness claim (F3) |

Commit scoping is clean throughout: test-only, fix, refactor, test-only, fix, docs. No refactor mixes a
behaviour change, no `refactor:` label is misapplied, no module boundary is crossed, and no plugin-API
contract is broken by signature change.

---

## HIGH

### F2 — An unbounded network call survives on the startup path, in the same class the branch set out to fix

**`serverpackcreator-api/src/main/kotlin/de/griefed/serverpackcreator/api/settings/UpdateConfig.kt:117`**

```kotlin
updateUrl.openStream().use {          // no connect-timeout, no read-timeout
```

Reached from `ApiProperties.updateFallback()` → `UpdateConfig.updateFallback()`, called at
**`ApiProperties.kt:1025`** inside `loadProperties`, which **`ApiProperties`' own `init` block calls**
(`ApiProperties.kt:1337-1340`). So constructing `ApiProperties` — the first thing `stageOne` does — makes
an untimed HTTPS request to the configured update URL (GitHub).

Not theoretical: the GUI run captured during this work logged it twice —
`INFO (ApiProperties.kt:1026) - Fallback lists updated.`

This is precisely the defect `test(api): pin that HTTP calls give up instead of hanging forever` was written to pin and `fix(api): bound every HTTP call with configurable timeouts` claims to have eliminated. A
host that DROPs rather than REJECTs blocks here exactly as it did in `VersionMeta`, and it blocks
*earlier* — before `stageTwo` is even reached, so before the manifest checks that were fixed.

Rule broken: *"If you find a bug while refactoring, surface it explicitly and propose a fix in its own
commit. Do not silently work around it or defer it as out of scope."* The branch neither fixed nor
recorded it; the commit message asserts the opposite.

**Proposed fix (own commit, `fix(api)`):** route it through `WebUtilities.openTimedStream`. `UpdateConfig`
does not currently hold a `WebUtilities`, so this needs either injection or moving the fetch behind an
existing collaborator — worth deciding deliberately rather than by reflex, since `UpdateConfig` is
constructed early in `ApiProperties`' property-declaration order (see the declaration-order landmine).

### F3 — "Every outbound call" is asserted in a commit message and in a durable landmine, and is false

`fix(api): bound every HTTP call with configurable timeouts` states: *"Every outbound call now goes through one opener."*
`serverpackcreator-api/CLAUDE.md:208-211` states the rule and adds: *"until 2026-08-17 that was every
single call site."*

Measured at `fix(api): bound every HTTP call with configurable timeouts` and still true at branch tip — sites **not** routed:

| Site | Network? | Status |
|---|---|---|
| `settings/UpdateConfig.kt:117` | **yes**, GitHub | untimed — see F1/F2 above |
| `serverpackcreator-app/.../updater/versionchecker/VersionChecker.kt:328` | **yes**, GitHub/GitLab | untimed `openConnection()` |
| `plugins/ServerPackCreatorPlugin.kt:68` | no — `jar:` URL, reads `plugin.toml` from the plugin's own jar | benign, but contradicts the wording |
| `utilities/common/ClassUtilities.kt:60` | no — `JarURLConnection` on a classpath resource | benign, same |

`VersionChecker.getResponse` is the update checker, and the captured GUI log shows it running at startup
(`GitHubChecker` → `All versions: [...]`), so it is a second real unbounded call on a user-facing path.

A durable landmine that overstates its own coverage is worse than none: the next reader will assume the
invariant holds and will not check. Rule broken: documentation must stay truthful as code changes.

**Proposed fix (own commit, `docs` + `fix`):** correct the landmine to state what is actually routed and
name the two exceptions with the reason each is benign or outstanding; route `VersionChecker.getResponse`
(it already builds its own `HttpURLConnection`, so this is two setter lines and needs no new dependency).

---

## MEDIUM

### F1 — The first red pin does not go green under its own fix; the guard was strengthened mid-stream

Verified empirically, not inferred:

- At `test(api): pin that HTTP calls give up instead of hanging forever`: `WebUtilitiesTimeoutTest` — 2 tests, **both FAILED** ("did not return within 15s").
  The red claim is genuine, and caused by the real defect (production consulted no timeout at all).
- Hybrid (production from `fix(api): bound every HTTP call with configurable timeouts`, test file exactly as committed at `test(api): pin that HTTP calls give up instead of hanging forever`): **both still
  FAILED**, same message.

Cause: the pin used `mockk<ApiProperties>(relaxed = true)`, which answers `0` for an `Int`, and `0` *is*
the JDK's "wait forever". Once the fix made production read those properties, the fixture supplied the
defect itself. `fix(api): bound every HTTP call with configurable timeouts` therefore had to edit the already-committed test (+`timedProperties()`, two
call-site swaps) to turn it green.

So checking out `test(api): pin that HTTP calls give up instead of hanging forever` and applying the fix on top shows **red → red**, not red → green. The assertions and the
15s bound are unchanged — only the fixture — and `fix(api): bound every HTTP call with configurable timeouts`'s message discloses this in full ("both
stall-guards failed against the *fixed* code until these stubs were added"), which is why this is MEDIUM
and not HIGH. But the conventions' evidence chain is the point of committing the pin separately, and here
it does not hold. The same trap is now documented as a landmine, which is the right outcome.

For contrast, the second pair is clean and was verified the same way: `test(api): pin what a manifest check costs` red on exactly the three
stated tests (2 requests vs 1, twice; `If-Modified-Since` absent), and the **unedited** pin from
`test(api): pin what a manifest check costs` passes all six against `fix(api): halve startup requests and skip unchanged manifests`'s production code.

### F4 — The "verbatim move" widened published API surface, and its own commit message does not say so

`refactor(api): extract manifest refreshing into ManifestUpdater` moved `checkManifest` from `private fun` in `VersionMeta` to **`fun`** (public) in a **public**
`class ManifestUpdater`. Diffed and normalised, the moved logic is otherwise byte-identical — including the
`var countOldFile/countNewFile` accumulators, the LegacyFabric equal-count nudge, and both `updateManifest`
overloads, which correctly stayed `private`.

`serverpackcreator-api` is published to Maven Central and its public surface is a stated
plugin-compatibility constraint. A pure-refactor commit is the wrong place to add exported surface
silently. The later docs commit does describe `versionmeta.ManifestUpdater` as "new exported", so it is
disclosed on the branch — just not where the change happens.

**Also:** `public` is broader than required. `ManifestUpdaterTest` is in the **same package and module**
(`de.griefed.serverpackcreator.api.versionmeta`), so `internal` — or no modifier at all for the same
package — would have satisfied the test without committing to a compatibility promise.

**Proposed fix:** decide whether `ManifestUpdater` is intended as plugin-facing. If not, narrow
`checkManifest` (and consider the class) to `internal` in its own `refactor(api)` commit, and drop the
"new exported" phrasing from the behaviour-change row.

### F5 — A test was added inside a `fix:` commit

`fix(api): halve startup requests and skip unchanged manifests` adds `anUnreachableHostLeavesThePresentManifestIntact` (+24 lines in
`ManifestUpdaterTest.kt`) alongside the behaviour change.

The convention keeps "add tests" and "change behaviour" in separate commits. The guard covers a behaviour
the same commit deliberately *preserved* (offline stays a WARN, not twelve ERRORs), so bundling is
defensible and the message explains it — but it is still a mix, and it means that guard has no red
ancestor. Nothing verifies it would have failed had the WARN path been written differently.

---

## LOW

### F6 — `ModpackZipInspector`-style testability seam absent here, so one claim rests on the message alone

`fix(api): bound every HTTP call with configurable timeouts` asserts warning counts ("21 before, 21 after") and suite counts. Counts were re-verified at
branch tip (21 for `-api`), but per-commit warning counts are not reproducible from the repository alone.
No action needed — noted only so a future reader knows which figures in these messages are re-checkable
and which are testimony.

### F7 — Verified-correct claims, recorded so they are not re-litigated

Checked and **holding**, each independently:

- No `setConnectTimeout`/`setReadTimeout` anywhere before the branch (`git grep` at the commit before `test(api): pin that HTTP calls give up instead of hanging forever`: 0 hits).
- `URLConnection` (not `HttpURLConnection`) as the opener's return type is load-bearing: the 4-arg
  `JarUtilities.copyFileFromJar` chain resolves `getResourceAsStream("/$fileToCopy")` — an **absolute**
  path — so the `VersionMeta::class.java` → `ManifestUpdater::class.java` swap in `refactor(api): extract manifest refreshing into ManifestUpdater` is genuinely
  equivalent (same classloader). Claim verified rather than assumed.
- `NetworkConfig` defaults are 5 000 / 15 000 / 60 000 and match the root `CLAUDE.md` row; `0` is passed
  through (`sanitise` rejects only `timeout < 0`), so the documented escape hatch exists.
- `getResponseAsString` / `getResponseCode` genuinely had no callers in main source.
- The byte figures are internally consistent: 206,986 + 56,270 + 9,381 + 2,516 = **275,153**, and
  489,038 − 275,153 = **213,885**. B30 in `BACKLOG.md` repeats the same numbers without drift.
- All three named test classes exist.

---

## Summary

Two real defects of the branch's own stated kind survive it (**F2**, and the `VersionChecker` half of
**F3**), and the branch asserts in a durable landmine that they do not. That is the finding worth acting
on: the fix is sound as far as it reaches, but its claim of completeness is wrong, and the wrong claim is
now written where future readers will trust it.

The process findings are smaller. The second red→green pair is exemplary and independently reproducible;
the first is not, for a reason the author documented rather than hid. The extraction is a genuine verbatim
move that quietly widened published surface.

Recommended order, each in its own commit:
1. `fix(api)` — bound `UpdateConfig.updateFallback`'s request (F2). Highest value: earliest startup path.
2. `fix(app)` — bound `VersionChecker.getResponse` (F3).
3. `docs` — correct the landmine's completeness claim; name the benign `jar:` exceptions (F3).
4. `refactor(api)` — narrow `ManifestUpdater.checkManifest` to `internal` unless it is meant to be
   plugin-facing (F4).

Stopping here for go-ahead, as instructed. No source modified.

---

# Refactor audit — `claude-perf-gui`

**Range:** the 7 commits of `claude-perf-gui`, up to `docs: record the GUI typing-path work` (7 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Verified the same way: throwaway worktree, plus hybrid checkouts (fix-commit production + pin-commit
tests, unedited) to test each red→green chain independently.

| Commit | Type | Verdict |
|---|---|---|
| `move the check-timer's server probe and pack-name read behind the view model` | refactor(app) | clean; behaviour-preserving move, dead param removed |
| `make the launcher-manifest candidates askable` | refactor(api) | adds **published API** with no compatibility-table row (G3) |
| `pin how often the check-timer consults the network and the disk` | test(app) | red **verified** (3 of 13) |
| `stop the check-timer re-probing the network and re-parsing the manifest` | fix(app) | chain **verified clean** red→green, pin unedited; but ships NUL bytes (G1) |
| `pin that the autocomplete list is parsed once, not per keystroke` | test(app) | red, but the guard is **wrong** — it can never pass (G2) |
| `parse the autocomplete list once, and stop reinstalling the LAF per keystroke` | fix(app) | rewrote that guard's assertion; disclosed (G2) |
| `record the GUI typing-path work` | docs | accurate; figures are testimony, noted |

## MEDIUM

### G1 — A Kotlin source file contains NUL bytes, so git records it as binary

**`serverpackcreator-app/.../configs/ConfigEditorViewModel.kt:107`**

```
val triple = "$minecraftVersion\x00$modloader\x00$modloaderVersion"
```

Two `0x00` bytes at offsets 5786 and 5797, where spaces were intended. `git show --stat` for
`fix(app): stop the check-timer re-probing the network and re-parsing the manifest` reports `Bin 5266 -> 8399 bytes` — the diff of that commit is **unreviewable**, and every
future diff of this file will be too. The file is still valid UTF-8 and compiles.

Not a runtime defect: the separator is used consistently when writing and reading the key, and NUL cannot
appear in a version string, so lookups are correct and collision-proof. It is a hygiene and reviewability
defect — an invisible control character in source that no one wrote deliberately, which will confuse the
IDE, Qodana and any human reviewer.

**Fix:** drop the string key entirely and use Kotlin's `Triple` as the map key — collision-free by
construction, no separator to choose, and the intent is visible.

### G2 — The autocomplete pin's *assertion* was rewritten by its own fix, so the committed guard can never pass

`test(app): pin that the autocomplete list is parsed once, not per keystroke` commits `anUnchangedSuggestionListIsParsedOnce` asserting
`verify(exactly = 1) { guiProps.getGuiProperty("autocomplete.clientmods") }`.

`fix(app): parse the autocomplete list once, and stop reinstalling the LAF per keystroke` changes that same guard to `verify(exactly = 20)` and swaps `assertEquals(size)` for
`assertSame(identity)`.

This is the conventions' explicit stop-and-flag signal — a changed *expectation*, not a fixture tweak —
and it is worse than the equivalent finding on the previous branch (F1). There, the pin was correct and
merely defeated by its fixture. Here the pin was **incorrect**: it demanded that the property be read once
per query, which the design deliberately does not do (the memo is *keyed* on the property value, so the
cheap read must happen every time). Anyone checking out `test(app): pin that the autocomplete list is parsed once, not per keystroke` sees a guard that no correct
implementation can satisfy.

Mitigating, and the reason this is MEDIUM: `fix(app): parse the autocomplete list once, and stop reinstalling the LAF per keystroke`'s message states all of this plainly, and the
replacement assertion was verified to have teeth by deliberately defeating the cache and observing it fail
on identity while contents matched. The lesson is already recorded as a landmine. No code fix is needed —
the guard is correct now — but the branch's red commit is a misleading artifact.

### G3 — New published API added in a `refactor:` commit, with no compatibility-table row

`refactor(api): make the launcher-manifest candidates askable` adds two public methods to a module published to Maven Central:

- `ModpackManifestParser.manifestCandidates(destination): List<File>` (`:67`)
- `ConfigurationHandler.manifestCandidates(destination): List<File>` (`:682`)

Neither appears in the root `CLAUDE.md` behaviour-change/compatibility table (`grep`: 0 hits); the only
mention anywhere is in `serverpackcreator-app/CLAUDE.md`, i.e. the consumer's notes rather than the
publisher's contract. The API compatibility policy makes the exported surface a stated constraint, so a
new exported member belongs in that table even when nothing breaks.

The `refactor:` label is defensible — behaviour genuinely is preserved, and `checkManifests` consumes the
same list it now exposes — but "refactor" understates a commit that widens a published contract.

**Fix:** add the row. (Label left alone: rewriting history for a type-label on an otherwise honest commit
is not worth it, and the row is what a future reader actually needs.)

## LOW

### G4 — Verified-correct, recorded so it is not re-litigated

- **The `SuggestionProvider` cache is EDT-confined, so its plain `var`s are safe.** Checked rather than
  assumed: `allSuggestions()` is reached only from `ConfigEditor.saveCurrentConfiguration` (`:547`, a
  user-initiated save) and `InclusionsEditor.saveSuggestions`; `parsedSuggestions()` only from the document
  listener, which launches on `Dispatchers.Swing`. The `ConfigCheckTimer`'s `parallelStream` validate path
  does **not** touch suggestions (`validateExclusions`/`validateWhitelist` contain no suggestion access).
  Had it, this would have been a data race.
- `ConfigEditorViewModel`'s two caches are correctly `ConcurrentHashMap`-backed, which *is* required — that
  object is reached from the timer's `parallelStream`.
- `refactor(app): move the check-timer's server probe and pack-name read behind the view model` removes `ConfigCheckTimer`'s now-unused `apiWrapper` parameter and `java.io.File` import, and
  the throwaway `PackConfig` per tick. Boy-Scout, within scope, no sprawl.
- Chain `test(app): pin how often the check-timer consults the network and the disk` → `fix(app): stop the check-timer re-probing the network and re-parsing the manifest` verified by hybrid checkout: 3 of 13 red before, all 13 green after with
  the pin **unedited**. This is the pattern the other chains should follow.
- The performance figures in `fix(app): stop the check-timer re-probing the network and re-parsing the manifest` and `docs: record the GUI typing-path work` (4.70 ms vs 0.021 ms parse/fingerprint on the
  2,715,835-byte fixture; popup `56x85` → `54x34` px) are measurements taken during the work and are not
  reproducible from the repository alone. Testimony, not verifiable — flagged only so a reader knows which
  is which.

## Summary

One code defect (**G1**, NUL bytes making a source file binary to git) and two process/documentation
findings (**G2**, a red pin that asserted the wrong thing; **G3**, published API without its table row).
Nothing behavioural is wrong: the caches are correct, EDT-confinement was verified rather than assumed, and
the one clean red→green chain on this branch is exemplary.

---

# Refactor audit — `claude-perf-generation`

**Range:** the 8 commits of `claude-perf-generation`, up to `docs: record the generation-throughput work, including the corrected estimates` (8 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Both red→green chains verified by hybrid checkout (fix production + pin tests, unedited).

| Commit | Type | Verdict |
|---|---|---|
| `pin the cost of the clientside-exclusion loop` | test(api) | red **verified** (3/3) |
| `stop a bad regex aborting the mod-list, and hoist the loop invariants` | fix(api) | chain **verified clean** red→green, pin unedited |
| `pin how often a modpack archive's central directory is read` | test(api) | contains **production code** (H1) |
| `read a modpack archive once per inspection, and index the Quilt merge` | fix(api) | **three concerns in one commit** (H2); one undisclosed error-path change (H4) |
| `pin that the regex mod-lists are not shared between reads` | test(api) | red **verified** (1/12) |
| `hand out a fresh regex mod-list per read` | fix(api) | chain **verified clean** red→green, pin unedited |
| `compile the Forge annotation-scanner's regexes once` | refactor(api) | published property changes shape, no compat row (H3) |
| `record the generation-throughput work, including the corrected estimates` | docs | accurate, including the corrected estimates |

This is the strongest branch of the four on evidence: both pins were genuinely red on exactly the stated
tests, and both go green with the pin **untouched**. It is the weakest on commit hygiene.

## MEDIUM

### H1 — A `test(api):` commit ships production code

`test(api): pin how often a modpack archive's central directory is read` adds `ModpackZipInspector`'s defaulted `openZip: (File) -> ZipFile` constructor parameter
(+18 lines of production) alongside its guard.

The convention keeps "add tests" and "change production" apart. The message discloses it and the reasoning
is real — open-counts are invisible from outside, so the guard cannot exist without the seam, and the
parameter is inert until the next commit — but the label says `test` and the diff says otherwise. Either
the seam belonged in its own `refactor(api):` commit first, or the commit should have been labelled for
what it contained.

### H2 — A `fix(api):` commit bundles three unrelated changes

`fix(api): read a modpack archive once per inspection, and index the Quilt merge` contains:

1. `ModpackZipInspector` — one archive read per inspection (the actual fix, ~80 ms per site on a
   10,000-entry archive);
2. `ServerPackFileGatherer` — hoisting `File(source).absolutePath` out of the walk loop (3.0 → 0.5 ms at
   50,000 files);
3. `QuiltPackScanner` — replacing a nested `find` with a map (4.71 → 0.14 ms at 500 mods).

Three files, three unrelated concerns, one commit. The message describes each honestly and labels 2 and 3
as rounding errors, which is why this is MEDIUM rather than HIGH — but a reader bisecting a zip regression
gets the scanner change too, and reverting one means reverting all three. They share only "I measured them
in the same sitting".

### H3 — A published property changed shape with no compatibility row

`refactor(api): compile the Forge annotation-scanner's regexes once` turns `ForgeAnnotationScanner.dependencyCheck` / `dependencyReplace` from
`val x: Regex get() = "…".toRegex()` into `val x: Regex = "…".toRegex()`.

Source-compatible, and the right change — the getter recompiled the pattern on every read inside
per-dependency loops. But these are **public** members of a published module, and the semantics change in
exactly the way this project's compatibility table already records twice for other members: the value is
no longer freshly created per read, so every caller now shares one instance. A caller comparing by identity
sees different behaviour. `grep` finds no row for either property (0 hits in `CLAUDE.md`).

The same omission as G3 on the previous branch, which suggests the reflex to record additive or
shape-only API changes is not yet automatic.

## LOW

### H4 — An error-path behaviour change went undisclosed

`fix(api): read a modpack archive once per inspection, and index the Quilt merge` reduces `getAllFilesAndDirectoriesInModpackZip` from **two** `catch` blocks to **one**
(verified: 2 occurrences of "Could not acquire file or directory" before, 1 after).

Before, directories and files were fetched by separate calls, each with its own `try`/`catch`, so a failure
in one still returned the other's results plus one logged error. Now a single failure loses both and logs
once. The commit message describes the single-pass optimisation but not this consequence.

Barely reachable in practice — both old calls opened the *same* archive, so a failure in one would almost
certainly fail the other — which is why it is LOW rather than MEDIUM. Worth recording because "we now lose
partial results on failure" is the kind of change that surprises someone reading the error log later.

### H5 — Verified-correct, recorded so it is not re-litigated

- Both chains verified by hybrid checkout: `test(api): pin the cost of the clientside-exclusion loop` red on exactly its three stated tests → all green
  under `fix(api): stop a bad regex aborting the mod-list, and hoist the loop invariants` with the pin unedited; `test(api): pin that the regex mod-lists are not shared between reads` red on its one test → green under `fix(api): hand out a fresh regex mod-list per read`,
  likewise unedited. This is the discipline the earlier branches' first pins lacked.
- `fix(api): read a modpack archive once per inspection, and index the Quilt merge` uses `putIfAbsent` rather than `associateBy` in the Quilt merge, preserving `find`'s
  first-match-wins. Correct, and the message explains why — a real distinction, not pedantry.
- `checkZipArchive`'s `val foldersInModpackZip` is assigned inside the `use` block and read after it;
  definite-assignment holds because the early-return path precedes the assignment. Compiler-enforced,
  checked rather than assumed.
- The docs commit records that the branch's headline estimate was **wrong** (the `exclusionFilter` read is
  worth ~3 ms, not a substantial win) rather than quietly dropping it. That is the behaviour the
  conventions ask for.

---

# Refactor audit — `claude-perf-web`

**Range:** the 8 commits of `claude-perf-web`, up to `docs: record the web query-shape and DBRef-flattening work` (8 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Both red→green chains verified by hybrid checkout (fix production + pin tests, unedited).

| Commit | Type | Verdict |
|---|---|---|
| `pin that the stats endpoint counts instead of scanning` | test(app) | red **verified** (1/2) |
| `count the stats totals instead of scanning three collections` | fix(app) | chain **verified clean**, pin unedited |
| `extract the upload duplicate-check from saveUploadedFile` | refactor(app) | clean, genuinely behaviour-preserving |
| `pin that an upload's duplicate-check does not scan the collection` | test(app) | ships **production code** (W2) |
| `look an upload's hash up by index instead of scanning every modpack` | fix(app) | chain **verified clean** (3/3 red → 3/3 green), pin unedited |
| `embed the run-configuration mod lists instead of joining three collections` | fix(app) | 18 files, big-bang; **not deployable on its own** (W3, W4) |
| `migrate stored run-configurations to embedded mod-lists` | feat(app) | migration **runner is untested** (W1) |
| `record the web query-shape and DBRef-flattening work` | docs | accurate |

## HIGH

### W1 — The component that rewrites persisted data has no test

**`serverpackcreator-app/.../web/migration/RunConfigurationListMigrationRunner.kt`** — `grep` across
`src/test`: **no test references it at all.**

`RunConfigurationListMigrationTest` covers only `RunConfigurationListMigration`, the pure per-document
transformation. Every decision that makes the migration *safe* lives in the untested runner:

| Line | Untested decision |
|---|---|
| `:77` | iterating `collection.find()` while writing inside the loop |
| `:83` | `replaceOne` targeting `_id` |
| `:86` | skip the drop when nothing was rewritten |
| `:91` | drop the orphaned collections only after a fully successful pass |
| `:92` | swallow every exception so startup continues |
| `:104-111` | per-collection drop failures logged and skipped |

This is the one component on all four branches that **mutates a user's persisted data**, and it is the
only substantial one with no coverage. The convention is explicit: never refactor untested code blind, and
missing characterization tests are a reportable defect. The transformation being well tested makes this
easy to overlook, which is precisely why it is worth stating.

Note the ordering rule is genuinely load-bearing: if the drop ran before the rewrite, the referenced ids
would be gone and the configurations unrecoverable. Nothing verifies that ordering today.

**Proposed fix:** test the runner against an in-memory Mongo double. The project has no such harness, so
the cheapest honest option is to extract the loop's decisions behind a small seam the way
`ModpackZipInspector`'s `openZip` was done — then the ordering, the skip and the swallow can be asserted
without a database.

## MEDIUM

### W2 — A `test(app):` commit ships production code

`test(app): pin that an upload's duplicate-check does not scan the collection` adds `@Indexed` to `ModPack.sha256` (+3) and `findBySha256` to `ModPackRepository` (+9).
Identical in kind to H1 on the previous branch, disclosed the same way, and wrong the same way: the label
says `test`, the diff includes production. Twice on one stack means the pattern, not the slip, is the
finding — when a guard needs new surface to exist, that surface belongs in a preceding
`refactor:`/`feat:` commit.

### W3 — `fix(app): embed the run-configuration mod lists instead of joining three collections` is a big-bang change across 18 files

One commit deletes three `@Document` classes and four repositories, retypes three entity fields, rewrites
a service, changes a derived query, rewrites a test (deleting four cases), and changes five frontend
files including two Vitest fixtures.

The convention asks for incremental change behind stable interfaces. The counter-argument is real and
stated in the message: the field type *is* the change, so nothing compiles between the halves, and the
frontend consumes the same JSON contract. But "it cannot be split" is not quite true — the frontend could
have moved in its own commit after the backend, since the SPA is built and deployed from the same tree but
is not compiled against Kotlin.

### W4 — An intermediate commit leaves the application unable to read its own data

`fix(app): embed the run-configuration mod lists instead of joining three collections` changes the persisted shape; the migration arrives only in `feat(app): migrate stored run-configurations to embedded mod-lists`. Deploying or bisecting
to `fix(app): embed the run-configuration mod lists instead of joining three collections` gives an application whose mapped type cannot read existing `runConfiguration` documents.

Disclosed in the message ("this one alone would leave a deployed instance unable to read its own
run-configurations"), and harmless if the branch merges as a unit — but it means the branch has no
bisectable-safe midpoint, which matters for exactly the kind of bug a data migration causes.

## LOW

### W5 — "The next start retries" is true of the rewrite, not the drop

`RunConfigurationListMigrationRunner.kt:94-96` promises a retry on failure. Accurate for the rewrite, whose
check is per document and idempotent. Not accurate for `dropOrphanedCollections`: it runs only when
`rewritten > 0`, so if the rewrite succeeded and a drop failed, the next start rewrites nothing, returns
early at `:86`, and never retries the drop. The collections linger.

Consequence is nil — the code already documents leaving them as harmless — but the comment overstates its
guarantee, which is the same species as F3 on the first branch.

### W6 — Verified-correct, recorded so it is not re-litigated

- Both chains verified by hybrid checkout: `test(app): pin that the stats endpoint counts instead of scanning` red on its one stated test → green under
  `fix(app): count the stats totals instead of scanning three collections`; `test(app): pin that an upload's duplicate-check does not scan the collection` red on all three → green under `fix(app): look an upload's hash up by index instead of scanning every modpack`. Pins unedited in both cases.
- **Writing inside a `find()` cursor is safe here**, though only because the rewrite is idempotent: a
  document returned twice by a moving cursor fails `needsRewrite` on the second visit and is skipped.
  Worth recording, since the same loop would be unsafe if the transformation were not idempotent.
- `replaceOne` is handed a `migrated` document that still carries its original `_id`, so the replace is a
  true in-place update rather than an insert.
- The `In`-means-contains-any bug fixed in `fix(app): embed the run-configuration mod lists instead of joining three collections` is real and was found while reading, not by a test —
  surfaced explicitly in the message rather than silently corrected, which is what the conventions ask.
- Four tests were **deleted** in `fix(app): embed the run-configuration mod lists instead of joining three collections` because the behaviour they pinned ceased to exist, and the
  comma-splitting coverage two of them also carried was preserved under new names. Checked: no coverage was
  silently dropped.

---

# Second pass — auditing the remediation

The fixes above are themselves code, so they were audited the same way. Findings from this pass:

### X1 — `createHasteBinFromString` was a third way of applying timeouts (fixed)

`WebUtilities.kt:235` opened its connection with a bare `openConnection()` and then set the two timeouts by
hand, because it needs `HttpsURLConnection` for its POST. Bounded, so not a hang — but the landmine had
just been rewritten to say only **two** routes exist, and this was a third. It now goes through
`openTimedConnection` and narrows the result, so the module contains exactly one `openConnection()` call:
the shared opener itself.

### X2 — Verified clean on the final tip

- **NUL bytes:** 1,531 tracked source and documentation files scanned, **none** contain one. (The first
  attempt at this check used `grep -qU $'\000'`, which degrades to an empty pattern and "found" 539
  matches including every PNG — the scan was redone in python. Worth recording: a check that reports
  everything is broken, not thorough.)
- **Unbounded network calls:** none. The only surviving `openStream`/`openConnection` outside the opener
  are `ServerPackCreatorPlugin.kt:68` and `ClassUtilities.kt:60`, both reading a `jar:` URL, both now
  named in the landmine as deliberate exceptions.
- **New guards have teeth**, verified by deliberately breaking the code rather than assumed:
  dropping before the rewrite fails 4 of the 7 runner guards; dropping when nothing was rewritten fails 2.
- **Stack integrity:** 12 / 8 / 8 / 8 commits per branch after rebasing, no duplicated subjects, and
  `./gradlew build` green at the tip.

### X3 — A mistake made during remediation, recorded because it nearly lost work

Rebasing the stack, `git rebase --onto claude-perf-generation <parent of `docs: record the web query-shape and DBRef-flattening work`> claude-perf-web` used the wrong
upstream and **dropped seven of web's eight commits**. Caught immediately by inspecting the branch, and
recovered from the reflog. The correct form for a stacked rebase is `--onto <new-base> <old-base>`, where
`<old-base>` is the parent branch's *pre-rebase* tip — not `HEAD^`.

---

## Status of every finding

| # | Finding | Severity | Status |
|---|---|---|---|
| F1 | First timeout pin does not go red→green (fixture defeated it) | MEDIUM | Recorded as a landmine; later pins written to avoid it |
| F2 | `UpdateConfig.updateFallback` unbounded, on the earliest startup path | **HIGH** | **Fixed** + pinned |
| F3 | "Every outbound call" false; `VersionChecker` unbounded; landmine overstated | **HIGH** | **Fixed** + pinned + landmine corrected |
| F4 | "Verbatim move" widened published API undisclosed | MEDIUM | **Fixed** — narrowed to `internal`, table row corrected |
| F5 | Test added inside a `fix:` commit | MEDIUM | Recorded; history not rewritten |
| F6/F7 | Testimony vs re-checkable figures; verified-correct claims | LOW | Recorded |
| G1 | NUL bytes made a Kotlin file binary to git | MEDIUM | **Fixed** — keyed on `Triple` |
| G2 | Autocomplete pin's assertion rewritten by its own fix | MEDIUM | Recorded; assertion in tree is correct and has teeth |
| G3 | `manifestCandidates` published without a compat row | MEDIUM | **Fixed** — row added |
| G4 | EDT-confinement verified, not assumed | LOW | Recorded |
| H1 | `test(api):` commit shipped production code | MEDIUM | Recorded; pattern noted |
| H2 | `fix(api):` bundled three concerns | MEDIUM | Recorded |
| H3 | Published property changed shape, no compat row | MEDIUM | **Fixed** — row added |
| H4 | Undisclosed error-path change in the zip rewrite | LOW | **Fixed** — row added |
| H5 | Chains verified clean | LOW | Recorded |
| W1 | Migration runner — mutates persisted data, **no test** | **HIGH** | **Fixed** — `MigrationStore` seam + 7 guards, teeth verified |
| W2 | `test(app):` commit shipped production code | MEDIUM | Recorded (second instance of H1) |
| W3 | 18-file big-bang commit | MEDIUM | Recorded |
| W4 | Intermediate commit not deployable (schema without migration) | MEDIUM | Recorded; branch merges as a unit |
| W5 | "Next start retries" true of the rewrite, not the drop | LOW | **Fixed** — comment and notes corrected |
| W6 | Cursor-write safety, `In` bug, deleted tests | LOW | Recorded |
| X1 | Third way of applying timeouts | LOW | **Fixed** |

**Three HIGH findings, all fixed and pinned.** Every remaining open item is a property of the commit
history — labels and commit boundaries — which cannot be corrected without rewriting shared history and is
recorded here instead. No behavioural defect is left open.

---

# Audit — `claude-performance-improvements`, iteration 1

**Range:** `7abd7c85c..HEAD` (45 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
This branch merges the four performance branches and has had one round of remediation, including
history restructuring. Re-verified from scratch. **Every finding below was introduced by that
remediation** — the pre-existing findings it set out to fix are confirmed fixed.

## What the restructuring fixed, verified

| Old finding | Verified now |
|---|---|
| F1 — first timeout pin went red→red | **Fixed.** Settings group lands first, so the pin references real properties and is red because nothing routes them; green under the routing commit, unedited. |
| F5 — test bundled into a fix | **Fixed.** `test(api): pin that an unreachable host leaves the present manifest intact` is its own commit. |
| G2 — autocomplete pin asserted the wrong thing | **Fixed.** `parsedSuggestions()` extracted first, pin holds the identity assertion, red on `@186d20a3` vs `@74ab779f` with equal contents (Java identity hashes from the failure message, not commits). |
| H1 — `test:` commit shipped the `openZip` seam | **Fixed.** Seam is now its own `refactor(api)` commit. |
| H2 — one `fix:` bundled three concerns | **Fixed.** Split into the archive fix, the gatherer hoist, and the Quilt index. |
| W2 — `test:` commit shipped `findBySha256` + index | **Fixed.** Now a preceding `feat(app)` commit. |
| W4 — undeployable midpoint | **Fixed.** Schema change and migration are one commit; no intermediate state cannot read its own data. |

Structural checks pass: no `fix:`/`refactor:` commit alters a previously committed test's *expectations*
(the three that touch pre-existing test files add guards, update constructor arguments, or delete cases
whose behaviour ceased to exist), and a replayed chain was spot-verified end to end after the rewrite.

## MEDIUM

### N1 — 54 dead commit-hash references across the documentation

The rewrite changed every hash. The docs still cite the old ones:

```
CLAUDE.md, serverpackcreator-api/CLAUDE.md, serverpackcreator-app/CLAUDE.md,
claude-docs/REFACTOR-LOG.md, REFACTOR-AUDIT.md
  -> 54 hashes not reachable from this branch
```

Examples: `REFACTOR-LOG.md` cites `fix(api): stop a bad regex aborting the mod-list, and hoist the loop invariants`, `fix(api): read a modpack archive once per inspection, and index the Quilt merge`, `fix(api): hand out a fresh regex mod-list per read`, `refactor(api): compile the Forge annotation-scanner's regexes once` for the generation work
and `test(app): pin that the stats endpoint counts instead of scanning`, `fix(app): count the stats totals instead of scanning three collections`, `refactor(app): extract the upload duplicate-check from saveUploadedFile`, `fix(app): look an upload's hash up by index instead of scanning every modpack`, `fix(app): embed the run-configuration mod lists instead of joining three collections`, `feat(app): migrate stored run-configurations to embedded mod-lists` for the web work — the entire
blow-by-blow. `REFACTOR-AUDIT.md` is a report about commits that no longer exist on the branch.

They resolve **today** only because the four superseded branches still hold them locally. Delete those and
every reference dies. The refactor log exists to be read "when you need the *why* of a past decision"; a
citation that resolves to nothing defeats exactly that.

(Note `61f97194` and `6afc2700` in `REFACTOR-LOG.md:1263` are *not* commits — they are Java
object-identity hashes quoted inside a test-failure message from earlier, unrelated work. Correctly
excluded rather than "fixed".)

### N2 — A landmine now documents a defect that has been fixed, and advises against the fixed pattern

`serverpackcreator-api/CLAUDE.md:245-248`:

> Consequence worth knowing before you trust that pair as an example: because the fixture had to change,
> checking out that pin and applying `fix(api): bound every HTTP call with configurable timeouts` shows **red → red**, not red → green. The later timeout pins
> […] were written against the *existing* signatures precisely so their fixes turn them green untouched —
> **copy those, not the first one.**

Two problems. The hashes are dead (N1), and the claim is **no longer true**: the F1 remediation reordered
that pair so the pin *does* go red → green untouched. A future reader is told to avoid copying the
`WebUtilitiesTimeoutTest` pattern, which is now the correct one and the most thorough example on the
branch.

The underlying lesson — a relaxed mockk answers `0`, which is the JDK's "wait forever" — is still valuable
and must survive; only the "and therefore this pair is a bad example" conclusion is stale.

### N3 — The remediation created a third instance of the violation it was fixing

`test(app): cover the migration runner's safety decisions, behind a MigrationStore seam` ships
**two main-source files** (`MigrationStore.kt`, and the runner rewired onto it).

This is precisely H1/W2 — a `test:`-labelled commit containing production code — committed *while* those
two were being split apart for the same reason. Applying a standard to inherited work and not to one's own
is the worse failure of the two.

Fix: split into `refactor(app): put the migration's database access behind MigrationStore`
(production, behaviour-preserving) followed by the guards.

## LOW

### N4 — Two new guards ride along in a `fix:` commit

`fix(api): route every outbound call through one timed opener` adds
`openedConnectionsCarryTheConfiguredTimeouts` and `aNonHttpUrlCanStillBeDownloaded` to the
already-committed `WebUtilitiesTimeoutTest`.

Both genuinely require the API the same commit introduces, so they cannot precede it — but F5 was split for
the same shape of reason, so the branch is inconsistent with itself. Lower severity than N3 because neither
guard could have been written earlier: `openTimedConnection` does not exist until this commit.

### N5 — Verified-correct, recorded so it is not re-litigated

- The replayed chain `pin what a manifest check costs` → `halve startup requests` still shows 3 red → all
  green with the pin unedited, so cherry-picking 40-odd commits did not silently alter intermediate trees.
- Final tree is byte-identical to the pre-rewrite snapshot (`git diff --quiet` against
  `perf-safety-snapshot`), so the restructuring changed only history, never content.
- `./gradlew build` green; api 338 (1 skip), app 135, clientside 88, frontend 31.
- 45 commits, no duplicated subjects.

## Summary

The restructuring did what it was meant to: all seven inherited commit-boundary findings are fixed and
verified. It introduced three of its own, all documentation-or-hygiene: **54 stale hash citations**, a
**landmine that now misdescribes its own repository**, and **one new `test:` commit carrying production
code** — the same rule it was in the middle of enforcing.

Nothing behavioural is wrong. Recommended order:
1. `docs` — replace hash citations with commit *subjects*, which survive a rewrite (N1), and correct the
   landmine to keep the mockk lesson while dropping the stale conclusion (N2).
2. `refactor(app)` + `test(app)` — split the `MigrationStore` seam out of its guard commit (N3).
3. Leave N4, with a note: the guards cannot precede the API they exercise.

---

# Audit — `claude-performance-improvements`, iteration 2

**Range:** `7abd7c85c..HEAD` (47 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: what earlier passes had *not* examined — Kotlin idiom, the internal quality of the units this
branch adds, and doc/code drift. Previous iteration's fixes re-verified first.

## Iteration-1 fixes confirmed

| Finding | Verified |
|---|---|
| N1 — 54 dead hash citations | **0 remain** in the durable docs (the two object-identity hashes at `REFACTOR-LOG.md:1263` correctly left alone) |
| N2 — stale red→red landmine | Corrected; the mockk lesson survives, the wrong conclusion is gone |
| N3 — `test:` commit shipping production | No `test:`-labelled commit on the branch touches `src/main` |

## MEDIUM

### P1 — The refactor-state table understates the suites by 9 tests

`CLAUDE.md` "Current status" table versus reality:

| Module | Table | Actual |
|---|---|---|
| api | 337 (1 skip) | **338** (1 skip) |
| app | 127 | **135** |
| clientside | 88 | 88 ✓ |

The gap is exactly the audit remediation: `UpdateConfigTimeoutTest` (+1, api),
`VersionCheckerTimeoutTest` (+1, app) and `RunConfigurationListMigrationRunnerTest` (+7, app). Each
was committed with a correct per-commit count in its message, but the branch's own summary table was
never brought forward.

That table is described in this repo's own words as the "durable, *current-state* context" a session
reads before touching code — so a stale count there is worse than a stale count anywhere else. It is
also the third instance of the same species on this branch (dead hashes, stale landmine, stale
counts): **documentation that quotes a snapshot goes out of date exactly when the code improves.**

## LOW

### P2 — `NetworkConfig`'s setters store the sanitised value but keep and log the raw one

`serverpackcreator-api/.../settings/NetworkConfig.kt:109-113` (and the two identical siblings):

```kotlin
set(value) {
    store.setInt(CONNECT_TIMEOUT_KEY, sanitise(value, fallbackConnectTimeout, CONNECT_TIMEOUT_KEY))
    field = value                                   // the RAW value
    log.info("Connect-timeout set to: $field ms")   // reports the RAW value
}
```

Assigning `connectTimeout = -5` stores **5000** (correct) but leaves `field` at `-5` and logs
`"Connect-timeout set to: -5 ms"`. No wrong value can be *read* — the getter recomputes from the store
— so this cannot reach a connection. It is a lying log line and a field transiently holding a value the
class has just rejected.

No test covers a negative *assignment* (`settersWriteBackToTheStore` uses a valid value), which is why
it survived.

**Fix:** sanitise once into a local, use it for the store, the field and the log. Add a guard for the
negative-assignment path.

### P3 — A line citation this branch's own fix invalidated

`serverpackcreator-app/CLAUDE.md:236` cites `ConfigEditor.kt:80` for the debounce trigger. Line 80 is
now a MigLayout column spec; `validationChangeListener` moved to **:84** when the view-model
constructor gained two arguments.

The sibling citation in the same sentence, `TabbedConfigsTab.kt:229` → `timer.restart()`, is still
accurate.

Same root cause as N1, one level down: a reference to a *position* rather than to a *name*. Symbols
survive edits; line numbers do not. Worth fixing in kind rather than by bumping the number.

### P4 — Pre-existing drift, out of this branch's scope

`CLAUDE.md` cites `PathsConfig.kt:586`–`:643` for the eight computed script-template properties; line
586 is a bare `}`. Nothing on this branch touches `PathsConfig.kt` or that paragraph, so this is
inherited debt, recorded rather than fixed — flagged because the same scan produced it and a future
reader should not mistake it for new.

### P5 — Verified clean

- **No new `!!`** anywhere: all 38 changed main-source files compared against their pre-branch
  versions, none has a higher count.
- **Every `var` justified:** three are `NetworkConfig` settings properties following the established
  group pattern, two are the verbatim-moved accumulators in `ManifestUpdater`, one is a loop counter in
  the migration runner. No `var` where a `val` would do.
- **All 15 test classes named in the docs exist**, and all 15 line citations across the three
  `CLAUDE.md` files are in range (only the one above points at the wrong content).
- `MigrationStore` is an interface with a single implementation and four methods, each one operation —
  no util-dumping, and the seam is the narrowest that makes the runner's decisions observable.
- `RunConfigurationListMigration.MIGRATED_FIELDS` is an immutable `listOf`, not a mutable companion
  collection.

## Summary

No behavioural defect. One real code defect (**P2**, a setter that stores one value and logs another)
and two documentation-drift findings (**P1**, **P3**), plus one inherited (**P4**).

The pattern across three iterations is now unmistakable and worth stating rather than fixing one more
time: every finding in this pass and the last is a **snapshot quoted in prose** — hashes, line numbers,
test counts — going stale the moment the code moved. Recommended fixes:
1. `fix(api)` — sanitise once in the `NetworkConfig` setters, and guard the negative-assignment path (P2).
2. `docs` — refresh the counts, replace the positional citation with a symbol, and record the
   snapshot-citation hazard itself so the next reader stops creating them (P1, P3).

---

# Audit — `claude-performance-improvements`, iteration 3 (final)

**Range:** `7abd7c85c..HEAD` (48 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: what three structural passes could not reach — **whether the guards actually bite**. Done by
mutation: break the production code, and see whether the suite notices. This found the most substantive
defects of any iteration.

## Iteration-2 fixes confirmed

`NetworkConfig`'s setters sanitise once (P2); the refactor-state table reads 339 / 135 / 88, matching the
suites (P1); the positional citation is now a symbol reference (P3).

## MEDIUM

Three guards pass for the wrong reason. Each was written *alongside* its production code, so none ever
had a red state — exactly the population this project's own conventions warn about ("a guard whose teeth
were never checked has repeatedly turned out to assert nothing"). Mutation found them; nothing else could.

### Q1 — `aFailedProbeIsRetried` cannot distinguish a retry from a wrongly-cached success

`ConfigEditorViewModelTest.aFailedProbeIsRetried` stubs the probe `false`, asserts `false`, re-stubs it
`true`, then asserts `true`.

**Mutation:** make `isServerDownloadable` cache failures as well as successes
(`downloadableTriples.add(triple)` unconditionally). **Result: the whole class still passes.**

Why: with a failure cached, the second call short-circuits at
`if (downloadableTriples.contains(triple)) return true` — so it returns `true` without probing, and
`assertTrue` is satisfied. The assertion cannot tell "re-probed and got true" from "remembered a failure
and wrongly reported true". The guard's stated intent — *the failure must not be remembered* — is
unverified, and the asymmetric caching policy that intent describes is the whole design decision.

**Fix:** assert the probe happened again — `verify(exactly = 2) { serverPackHandler.serverDownloadable(…) }`.

### Q2 — The zip single-pass guard sorts away the ordering it claims to compare

`ModpackZipInspectorOpenCountTest.theSinglePassAgreesWithTheDedicatedMethods` compares
`(getDirectoriesInModpackZip + getFilesInModpackZip).sorted()` against
`getAllFilesAndDirectoriesInModpackZip(...).sorted()`.

**Mutation:** invert the partition (`partition { !it.isDirectory }`), so files come before directories.
**Result: every zip test still passes.**

Both sides are sorted, so order is discarded — yet the production comment asserts "Directories still come
first, as they did when they were two separate calls", and the commit message repeats it. That guarantee
has no guard.

Mitigating, and why this is MEDIUM not HIGH: no production code consumes the order —
`ConfigurationHandler.kt:865` is the only caller and it returns the list onward untouched. So the claim is
**decorative**, which is itself the finding: either it matters and must be asserted, or it does not and
should not be promised.

**Fix:** either drop `.sorted()` and pin the order, or stop claiming it in prose. Pinning is cheaper and
keeps the promise honest.

### Q3 — `aNullHashReportsNoDuplicate` asserts its own stub

`ModPackDuplicateCheckTest.aNullHashReportsNoDuplicate` stubs `findBySha256(null)` to return
`Optional.empty()` and asserts the result is empty.

**Mutation:** remove the null short-circuit from `existingUploadOf`, so a null hash goes to the
repository. **Result: the test still passes** — because the stub answers `empty()` for `null`.

The guard verifies the mock, not the code. Its intent is "a hash-less upload must not even ask the
database", precisely because Mongo's own `{sha256: null}` *would* match documents whose field is unset —
the reason the short-circuit exists at all.

**Fix:** `verify(exactly = 0) { modpackRepository.findBySha256(any()) }`.

## LOW

### Q4 — Guards confirmed to bite

The same mutation method, applied to the rest of the never-red population, found these sound:

| Mutation | Result |
|---|---|
| migration rewrite stops converting DBRefs | 2 of 5 fail |
| manifest candidate order swapped | 1 of 3 fails |
| `DEFAULT_READ_TIMEOUT` 15 000 → 14 000 | 1 of 7 fails |
| `allSuggestions` leaks the cached instance | 1 of 4 fails |
| manifest fingerprint ignores size and mtime | 1 of 13 fails |

Together with the earlier runner mutations (drop-before-rewrite → 4 fail; drop-on-empty-pass → 2 fail),
every other new guard on this branch is demonstrably load-bearing.

### Q5 — Module boundaries intact

`-api` imports no Spring and no FlatLaf. Its four `javax.swing` imports (`ApiPlugins`,
`ExtensionConfigPanel`, `TabExtension`, `ExtensionTab`) are all **pre-existing** at the branch point —
verified against `7abd7c85c` — and are the documented plugin-GUI extension points, not new leakage. No
dependency points outward.

### Q6 — The new units hold up

- `MigrationStore` — four methods, one operation each, single implementation, exists solely to make an
  unobservable decision observable. Correctly sized.
- `FilterMatcher` — private nested class, constructed once per generation, holds only what is invariant
  across the loop it serves.
- `URL.timedConnection` — one function, the single place that knows *how* a timeout is applied, with two
  documented call routes and a stated reason for each.
- `NetworkConfig` — follows the established settings-group pattern exactly; defaults are constants read by
  the `fallback*` members, so the shipped values exist once.

## Summary

Three guards that assert nothing meaningful (**Q1**, **Q2**, **Q3**) — all found by mutation, none
findable by inspection, and all in the population the project's own conventions single out as highest risk:
tests written after the code they cover. Every other new guard bites, module boundaries are intact, and
the new units are correctly sized.

Recommended: fix all three, and mutation-test them afterwards rather than trusting the fix.

---

# Audit — `claude-performance-improvements`, iteration 4

**Range:** `7abd7c85c..HEAD` (49 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: the two things three structural passes and one mutation pass could not reach — **whether the
persistence claims are true of the runtime**, and **whether the contracts outside this repo's own
source tree still match the code**. Both turned out to hide defects; the mutation pass could not have
found either, because both are true of code that no test exercises.

## Iteration-3 fixes confirmed

`test: make three guards that passed for the wrong reason actually bite` closes Q1, Q2 and Q3 exactly as recommended: `aFailedProbeIsRetried` now
`verify(exactly = 2)`s the probe, `theSinglePassAgreesWithTheDedicatedMethods` dropped both `.sorted()`
calls so the directories-first order is pinned rather than promised in prose, and
`aNullHashReportsNoDuplicate` now `verify(exactly = 0)`s the repository. Each carries a comment saying
why the assertion above it is insufficient on its own.

**Also verified, since it gates the finding below:** `perf-safety-snapshot` is fully superseded by this
branch. `git diff HEAD perf-safety-snapshot` is 10 files, 439 deletions against 36 insertions, with no
file added or removed on either side; every one of those 36 insertions is text HEAD later replaced
(the stale hash citations, and suite counts of 337 / 127). All 11 of its patch-id-unique commits have
subject counterparts on HEAD, some split in two. It needs no merge, and deleting it is safe — which is
precisely what makes **R4** urgent rather than cosmetic.

## HIGH

### R1 — The `@Indexed` that the upload duplicate-check is built on creates no index

`fix(app): look an upload's hash up by index instead of scanning every modpack` replaced a
`findAll()`-and-compare loop with `ModPackRepository.findBySha256`, and `ModPack.sha256` carries
`@Indexed`. The repository's KDoc states the consequence as fact: *"`ModPack.sha256` carries
`@Indexed`, so this is a single indexed lookup rather than a scan"*.

**No index is ever created.** Spring Data MongoDB stopped creating annotation-declared indexes
automatically in 3.0. Verified from the resolved artifacts rather than from memory:

| Evidence | Result |
|---|---|
| `javap -c` on `MongoMappingContext` (spring-data-mongodb 5.1.0) | the no-arg constructor emits `iconst_0; putfield autoIndexCreation:Z` — the default is **false** |
| `javap -c` on `DataMongoConfiguration.mongoMappingContext` (spring-boot-data-mongodb 4.1.0) | `PropertyMapper.from(properties.isAutoIndexCreation()).to(context::setAutoIndexCreation)` — `PropertyMapper` skips a null source, so an absent property leaves the constructor default standing |
| `spring-configuration-metadata.json` in that jar | `spring.data.mongodb.auto-index-creation` exists with **no default value** |
| `grep -rn 'auto-index-creation\|autoIndexCreation'` across the repo | **no hits** — no property, and no `MongoMappingContext` bean or `AbstractMongoClientConfiguration` subclass anywhere |

So `findBySha256` is a `COLLSCAN`. This is `@Indexed`'s only occurrence in the project and it is **new on
this branch** (`git grep @Indexed 7abd7c85c` is empty), so nothing pre-existing masks it.

Graded HIGH on this report's own precedent: **F3** was graded HIGH for exactly this shape — a claim
asserted in a commit message and in durable prose, and false.

**Scoped honestly: the commit is still a real improvement, just not the one it claims.** It stops
loading every document into the JVM and stops dragging the eager `@DBRef` graph behind each one, which
was the dominant cost. What it does not do is let the *server* skip documents.

**Fix:** enable `spring.data.mongodb.auto-index-creation=true` in the app's own
`application.properties`. That keeps the annotation as the single declaration — creating the index in
code as well would duplicate a decision into two places that can drift.

### R2 — A duplicate hash now yields an uncaught 500 where it used to yield a handled 400

`ModPackRepository.findBySha256` is declared `fun findBySha256(sha256: String?): Optional<ModPack>`. A
Spring Data derived query returning `Optional<T>` throws `IncorrectResultSizeDataAccessException` when
more than one document matches.

The code it replaced could not do that. It iterated `findAll()` and threw `StorageException` on the
**first** match, tolerating any number of duplicates:

```kotlin
val availableModpacks = modpackRepository.findAll()
for (available in availableModpacks) {
    if (available.sha256 == modpack.sha256) { throw StorageException(...) }
}
```

`ModPackController` catches `StorageException` and answers with a populated error body.
`IncorrectResultSizeDataAccessException` is not a `StorageException`, so it escapes to the container as
a 500 — and it does so on *every* subsequent upload of that hash, not once.

Two ways two documents come to share a hash: an existing database predating the duplicate check, and a
race between two concurrent uploads of the same file (both scan, both find nothing, both `save`). The
old code absorbed both; the new code turns the second upload into a server error. Undisclosed — neither
the commit message nor `serverpackcreator-app/CLAUDE.md` mentions the multiplicity contract.

**Fix:** `findFirstBySha256`. Spring Data's `First` keyword limits the query to one result, so it can
never throw, and "first match wins" is exactly the pre-branch semantics.

### R3 — A published, versioned REST contract changed shape; its published spec still describes the old one

`fix(app): embed the run-configuration mod lists, and migrate what is stored` changed
`RunConfiguration.startArgs` / `clientMods` / `whitelistedMods` from `MutableList<StartArgument>` etc. to
`MutableList<String>`, and deleted the three classes. `RunConfigurationController` returns that entity
directly — `ResponseEntity<List<RunConfiguration>>` and `ResponseEntity<RunConfiguration>` under
`@RequestMapping("/api/v2/runconfigs")` — so the response body went from

```json
"clientMods": [{"id": 1, "mod": "3dskinlayers-"}]
```

to `"clientMods": ["3dskinlayers-"]`, on a path that carries an explicit API version.

Neither published description was updated:

- `serverpackcreator-help/Writerside/api-docs.yaml` still `$ref`s
  `#/components/schemas/StartArgument`, `/ClientMod` and `/WhitelistedMod`, and still defines all three
  — schemas for classes that no longer exist. `RunConfiguration` is embedded in `ServerPackView`, so
  every response carrying a server pack is described wrongly too.
- `serverpackcreator-help/Writerside/topics/Run-Configs.md` shows four response samples with
  `{"id": …, "mod": …}` objects.

`serverpackcreator-app/CLAUDE.md` does say *"The JSON shape is part of this contract"* — but names only
the two SPA consumers and `types/api.ts`. The obligation it states is "change those in the same
commit", which is precisely what was done; the documented external contract was simply not in the list.

**Caveat, so the fix is not over-claimed:** that spec is a stale generated snapshot independent of this
branch — it types `id` as `integer/int32` while the entities use `@MongoId(FieldType.STRING)`, and
`springdoc` is commented out in `serverpackcreator-app/build.gradle.kts`, so nothing regenerates it.
This branch widened pre-existing drift rather than creating it. What is new, and this branch's own, is
that the spec now references deleted types.

**Fix:** inline `type: string` into the three properties, delete the three orphaned schema definitions
(referenced from nowhere else — verified), and correct the four samples in `Run-Configs.md`. Note that
`serverpackcreator-help` is not in the Gradle build, so no test can guard this; it is prose, and it
stays correct only by being edited alongside the entity.

## MEDIUM

### R4 — The audit report itself cites 41 hashes that are not on the branch

Of the 42 hash-shaped tokens cited in `REFACTOR-AUDIT.md`, one is the branch base `7abd7c85c` (on
`develop`, so stable), two are not commits at all — `61f97194` and `6afc2700` are Java version strings,
as the report itself notes elsewhere — and **39 are dead commit hashes**. Every one of those 39 resolves
only because `perf-safety-snapshot` still points at it: the branch this same iteration just established
is safe to delete. Delete it, and the report's evidence base becomes unreachable and
garbage-collectable. Two further identity hashes (`@186d20a3`, `@74ab779f`) are Java `hashCode` output
quoted from a failure message, not refs.

This is the defect class **N1** recorded and `docs: cite commit subjects instead of hashes` fixed — in
`claude-docs/REFACTOR-LOG.md` and `serverpackcreator-api/CLAUDE.md`. The audit report that *recorded*
the convention was left violating it, and it is a committed, root-level document like the others.

**Fix:** replace the hash citations with commit subjects, per the convention the branch adopted.
Verdict tables keep one column of identity — the subject — because that is what survives a rebase.

## LOW

### R5 — The migration's collection name is a string copy of a mapping decision, and a wrong one fails silently

`RunConfigurationListMigrationRunner.COLLECTION` is `"runConfiguration"`, commented as *"The collection
Spring Data maps `RunConfiguration` to."* That is correct today — Spring Data decapitalises the simple
class name — and the same is true of the three `ORPHANED_COLLECTIONS`, which have to be literals
because their classes are gone.

The live one is different from the three dead ones: if `RunConfiguration` is ever renamed or gains
`@Document("…")`, `store.findAll(COLLECTION)` reads a collection that does not exist, returns an empty
list, rewrites nothing, drops nothing, logs no failure, and the migration reports success. Every
guard in `RunConfigurationListMigrationRunnerTest` still passes, because they all drive the store
through the same constant.

**Fix:** pin it. A standalone `MongoMappingContext` resolves the mapped name with no database, so the
constant can be asserted against the mapping it claims to mirror.

### R6 — `REFACTOR-AUDIT.md` rides along in five code commits

`test(api): pin that the fallback-list refresh gives up instead of hanging` (`test(api)`), `fix(app): key the installer-probe memo on a Triple instead of NUL-delimited text` and `fix(api): sanitise a network timeout once per assignment` (`fix(api)`/`fix(app)`), `fix(api): route the HasteBin POST through the shared opener too`
(`fix(api)`) and `test: make three guards that passed for the wrong reason actually bite` (`test`) each bundle the regenerated audit report — twice together with
`CLAUDE.md` edits — while the branch elsewhere keeps five separate `docs:` commits. Reporting it for
consistency, not proposing a fix: the report is arguably the fix's own evidence, and rewriting five
commits' history to relocate a markdown file is disproportionate to the rule it bends.

### R7 — Verified clean, recorded so it is not re-litigated

- **`./gradlew build` green**, and the documented counts are the real ones, read from the JUnit XML:
  api 339 (1 skip), app 135, clientside 88, grinder 233 (19 skip). The refactor-state table matches.
- **No new compiler warnings.** A forced recompile (`--rerun-tasks`) of `-api` and `-app` main sources
  emits 35, and every one blames to a commit reachable from `develop`. The only two in files this
  branch touched — the delicate-API call in `ConfigEditor` and the unchecked `javaFileListFlavor` cast
  in `TabbedConfigsTab` — blame to `refactor(gui): own ConfigEditor coroutines via a component-scoped
  CoroutineScope` and the drag-and-drop feature commit respectively.
- **No new `!!`** anywhere in the branch's added lines. Every added `var` is a settings property, a memo
  cache, a loop counter or a mapped entity field.
- **The NUL-byte defect is genuinely gone.** `fix(app): stop the check-timer re-probing the network and re-parsing the manifest` committed two literal NULs into
  `ConfigEditorViewModel.kt`, which is why git recorded that one diff as binary; the blob at
  `fix(app): key the installer-probe memo on a Triple instead of NUL-delimited text` and at `HEAD` contains none, and `file` reports UTF-8 text. The historical commit stays
  un-diffable, which is not worth rewriting history over.

## Summary

Three HIGH findings, all in the same blind spot: **claims about a runtime this suite never starts, and
contracts that live outside `src/`.** The declared index does not exist (**R1**), the query that
replaced a tolerant scan is intolerant of the duplicates the scan existed to report (**R2**), and a
versioned REST response changed shape while its published spec kept describing types the branch
deleted (**R3**). None was reachable by inspection of the diff, by the test suite, or by mutation —
the first two need a real MongoDB, the third needs to look outside the module.

Also: the report you are reading cites 41 hashes that die with `perf-safety-snapshot` (**R4**), and the
migration's collection name can go wrong silently (**R5**).

Recommended: fix R1–R5, and delete `perf-safety-snapshot` once R4 has landed.

---

# Audit — `claude-performance-improvements`, iteration 5

**Range:** `7abd7c85c..HEAD` (57 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: **iteration 4's own remediation, adversarially.** Iteration 4 found defects by asking what is
true of a runtime the suite never starts — so the first thing to ask of its fixes is whether *they*
were checked against that runtime. One was not, and it is the worst regression on the branch.

## Iteration-4 fixes confirmed

R2 (`findFirstBySha256`, limiting), R3 (the spec and the 30 doc samples, with the three orphaned schemas
gone and the YAML still parsing), R4 (39 dead hashes replaced by subjects; five hash-shaped tokens
deliberately kept and each annotated) and R5 (the collection-name guard, mutation-verified) all hold.

**R1 does not.** See S1.

## HIGH

### S1 — The index fix makes the web application refuse to start without a database

`fix(app): create the indexes the web module declares` set
`spring.data.mongodb.auto-index-creation=true`. That property does not merely *permit* index creation;
it makes `MongoTemplate`'s own bean creation perform it, during context refresh, against a server that
must be reachable **then**.

Measured, both directions, same context and same absent MongoDB, one variable changed:

| `spring.data.mongodb.auto-index-creation` | Result |
|---|---|
| `false` | context starts, `ModPackService` resolves — *"started fine"* |
| `true` | `Waiting for server to become available for operation createIndexes with ID 3. Remaining time: 29997 ms`, then `MongoTimeoutException` → `mongoTemplate` fails → **refresh cancelled**, context dead |

So the fix turned "web mode starts, logs a connection error, and works once the database appears" into
"web mode hangs 30 seconds and then dies if the database is not up yet". That is not a corner case:
`docker/docker-compose.yml` starts the app and the `db` service together, so losing the race is the
normal first boot.

It also contradicts a design decision this module already made and documented. From
`serverpackcreator-app/CLAUDE.md`, on the migration runner: *"applies it on `ApplicationReadyEvent`, not
during context startup, so an unreachable database delays the migration instead of blocking the boot."*
The index fix did precisely what that sentence forbids, one directory away from the sentence.

**Why iteration 4 did not catch it:** `WebServiceContextTest` boots the real context with no database
and would have failed instantly — but the property lives in `src/main/resources/application.properties`,
and `src/test/resources/application.properties` shadows it on the test classpath. The suite was green
because the shipped setting never reached the context under test. Iteration 4 noticed the shadowing —
it is why `ModPackIndexCreationTest` has to enumerate classpath URLs to read the shipped file at all —
and treated it as a test-plumbing detail rather than as the reason its own change was unverified. See
**S2**.

**Fix:** revert the property and create the declared indexes on `ApplicationReadyEvent` instead,
failing soft, exactly as the migration runner does. `@Indexed` stays the single declaration: the
definitions are resolved from the mapping context with Spring Data's own
`MongoPersistentEntityIndexResolver`, so nothing is restated in code and startup gains no database
dependency.

## MEDIUM

### S2 — No test exercises the shipped web configuration, because the test copy shadows it

`serverpackcreator-app/src/test/resources/application.properties` (720 bytes) shadows
`src/main/resources/application.properties` (1,159 bytes) on the test classpath. Everything Spring
reads under test therefore comes from the test copy, so **any** change to the shipped file is invisible
to the suite — `WebServiceContextTest` boots "the real web application context" over a configuration
that is not the one shipped.

This is what let S1 through, and it is not specific to S1: the same hole covers every future edit to
that file. It is also pre-existing, and the test copy exists for good reasons (it points
`spring.config.import` at test fixtures), so the fix is not "delete it".

**Fix:** narrow the exposure where it bit. The startup-safety property of the shipped configuration is
now asserted directly — the context is booted with no database *and* with the shipped file's
index-creation setting applied explicitly, so the combination that broke is the combination under test.
Recorded here rather than solved wholesale, because making the whole shipped file authoritative under
test is a larger change than this branch should carry.

## LOW

### S3 — Verified clean

- `./gradlew build` green at iteration 4's tip: api 339 (1 skip), app 140, clientside 88, grinder 233
  (19 skip), and the refactor-state table carries 140 rather than being left at 135.
- The three iteration-4 guards each bite. `RunConfigurationCollectionNameTest` was mutation-verified in
  its own commit; `ModPackHashQueryTest` went red on `findBySha256` and green on `findFirstBySha256`;
  `ModPackIndexCreationTest`'s entity half fails if `@Indexed` is removed, since it resolves through
  `MongoPersistentEntityIndexResolver` rather than reading the annotation.
- The doc rewrite is mechanical and complete: no `"mod":` or `"argument":` element survives in
  `serverpackcreator-help/Writerside/topics/`, and no reference to the three deleted schemas survives
  in `api-docs.yaml`.

## Summary

One HIGH (**S1**), and it is iteration 4's own: enabling annotation-driven index creation made a
reachable MongoDB a *startup requirement*, breaking the first boot of the shipped docker-compose and
contradicting the very design note the module wrote about not blocking startup on the database. It was
verified by measurement in both directions.

The lesson is **S2**, and it generalises past this branch: a guard that reads a shipped file is not the
same as a test that *runs* with it. The suite has booted the real context all along and would have
caught this in one second, had the configuration under test been the configuration shipped.

Recommended: fix S1 by moving index creation to `ApplicationReadyEvent` behind a seam, and close S2 for
the property that bit by booting the context with it.

---

# Audit — `claude-performance-improvements`, iteration 6

**Range:** `7abd7c85c..HEAD` (60 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: the **generalisation** of the last two iterations' findings. R1 and S1 were both the same shape —
something *declared* that was never actually in effect, unit-tested by construction and never checked
against the thing that runs it. So: what else does this branch declare that nothing verifies is live?
Two answers, one in each module the branch touched.

## Iteration-5 fixes confirmed

`DeclaredIndexCreator` creates the declared indexes on `ApplicationReadyEvent`, failures logged and
swallowed; the property is reverted and left commented with its reason; `DeclaredIndexStartupTest` boots a
real context with no database and asserts the shipped file does not re-enable refresh-time creation, and it
shares `WebServiceContextTest`'s context rather than paying a second boot. Both landmines are in
`serverpackcreator-app/CLAUDE.md`. `./gradlew build` green — api 339 (1 skip), app 143, clientside 88,
grinder 233 (19 skip).

## MEDIUM

### T1 — Five components this branch added, none asserted to be wired

The branch adds five `@Component`s: `RunConfigurationListMigration`, `RunConfigurationListMigrationRunner`,
`MigrationStore`/`MongoMigrationStore`, `IndexStore`/`MongoIndexStore` and `DeclaredIndexCreator`. Every one
is well unit-tested — and every one of those tests **constructs the class directly**, so all of them pass
whether or not Spring ever creates the bean.

Nothing asserts registration. `WebServiceContextTest` is the module's wiring guard and stops at the
controllers and four services; neither `@EventListener` is covered by anything. So the failure mode is
silent in both cases, and unequal in cost:

| If inert | Consequence |
|---|---|
| `DeclaredIndexCreator` | the index is never created — R1 again, and its own guard would still pass |
| `RunConfigurationListMigrationRunner` | **persisted data is never migrated**, and the mapped type cannot read the old shape, so reads fail on real data while the whole suite is green |

Not a live defect — the boot logged in iteration 5 shows both listeners firing (`createIndexes` and then
`find`, each waiting on the absent server). It is a **guard gap**, and precisely the one that let R1 and S1
through: a component verified by construction is not verified as reachable.

**Fix:** assert the five beans and the two listeners in the context test that already owns wiring.

### T2 — The frontend guard for the shape change asserts pass-through, not rendering

The DBRef-to-embedded change made `startArgs` / `clientMods` / `whitelistedMods` arrays of strings, and
`RunConfigurationCard.vue` renders them with `.join(', ')`. Its test asserts
`expect(wrapper.vm.clientMods).toEqual(['optifine'])` — the value it just passed in, via a component that
assigns `this.clientMods = runConfig.clientMods` unchanged. The assertion holds for *any* element type.

**Mutation:** revert the component to the pre-branch object shape — `clientMods.map(m => m.mod).join(', ')`,
at both render sites. **Result: all 31 frontend tests pass.** The card renders `undefined, undefined` in a
browser and nothing notices.

**`SubmitModPackForm.test.ts` is *not* the same shape, and the first draft of this finding said it was.**
Its `expect(wrapper.vm.clientMods).toBe('optifine')` looks like the same pass-through, but the form's
`clientMods` is a *derived* value — `selectedRunConfiguration` does `config.clientMods.join(', ')` into a
string field — so the assertion is shape-sensitive. Checked rather than assumed: mutating that line to
`config.clientMods.map(m => m.mod).join(', ')` **fails** the guard. Its three tooltip `.join(', ')` render
sites are still unasserted, but the consumer that matters is covered. Corrected here because an
overstated finding is the same defect as an overstated commit message.

This is iteration 3's Q1/Q2/Q3 defect class — a guard that verifies its own input rather than the code —
reappearing in the module iteration 3 did not cover. It matters more here than usual because the frontend is
the *only* consumer the shape change was verified against: `serverpackcreator-app/CLAUDE.md` names these two
components as the contract's consumers, and the guard on them cannot see the shape.

**Fix:** assert the rendered text, not the passed-through prop. `wrapper.text()` containing the joined
strings fails under the mutation above, because `undefined` is what gets rendered.

## LOW

### T3 — Verified clean

- **Component scanning genuinely reaches the new package.** `WebService` is `@SpringBootApplication` in
  `de.griefed.serverpackcreator.app.web`, so `web.index` and `web.migration` are below it. Worth stating
  because T1 is otherwise easy to misread as "the beans are missing" — they are not, they are unasserted.
- **No annotation the branch added is inert.** The added set is exactly five `@Component`, two
  `@EventListener(ApplicationReadyEvent)`, one `@Indexed` and four `@Throws`. The `@Indexed` was R1 and is
  now consumed explicitly by `DeclaredIndexCreator`; the rest are live. No `@Transactional` was added — which
  is as well, since MongoDB transactions need a replica set and the shipped compose file runs a single node.
- **The two ready-event listeners are order-independent.** Neither carries `@Order` and their execution
  order is therefore unspecified, which is fine: the index is non-unique, so creating it before or after the
  migration rewrites documents changes nothing.

## Summary

Both findings are the same defect as the previous two iterations', one level up: **a declaration verified by
the test that constructs it, rather than by the thing that runs it.** Five components verified by
construction and never as beans (**T1**); one frontend guard that asserts the prop it passed in and stays
green when the component consumes the wrong element shape entirely (**T2**, proven by mutation — and scoped
down mid-finding, because the second test it accused turned out to bite).

Neither is a live defect today. Both are the reason a live defect went unnoticed twice in this session, so
they are worth closing on that basis rather than on their current impact.

---

# Audit — `claude-performance-improvements`, iteration 7

**Range:** `7abd7c85c..HEAD` (63 commits) · **Date:** 2026-08-18 · **Mode:** READ-ONLY
Focus: **equivalence with `develop`.** Iterations 1–6 verified claims, guards and declarations — all
inward-looking. None of them asked the only question that matters for a performance branch: *does it still
do what it did before?* This iteration answers that by measurement, three ways, and then by running the
real application against a real database.

## The result first

**490 pre-existing guards, run unmodified against HEAD's production code, zero failures.**

| Module | develop's own test tree, HEAD's `src/main` | Result |
|---|---|---|
| `-api` | 309 (1 skip) | **0 failures** |
| `-clientside` | 88 | **0 failures** |
| `-app` | 93 (19 of 21 develop-era files) | **0 failures** |

Method: a detached worktree at HEAD with each module's `src/test` replaced by `git checkout develop --`.
Nothing else changed, so any failure would be HEAD's production code behaving differently under develop's
expectations.

Only two develop-era files could not compile, and both are the branch's two deliberate shape changes:

| File | Why it cannot compile | Verdict |
|---|---|---|
| `ConfigEditorViewModelTest` | `ConfigEditorViewModel` gained `configurationHandler` + `serverPackHandler` | **adapted and run** — the constructor took two relaxed mocks, *every assertion byte-identical* (diffed to prove it), and all **7** guards pass |
| `RunConfigurationServiceTest` | references `ClientMod` / `StartArgument` / `WhitelistedMod`, their four repositories, `findByArgument` / `findByMod`, and the 3-arg constructor — all deleted | **not adaptable, and correctly so**: it asserts the join behaviour the branch removed by design |

The second one deserves the detail, since "the test was deleted" is how regressions hide. develop had 10
guards, HEAD has 9. The three that went — `aKnownStartArgumentIsReplacedByTheStoredEntry`,
`aKnownClientModIsReplacedByTheStoredEntry`, `aKnownWhitelistedModIsReplacedByTheStoredEntry` — each pinned
"look up the existing document and reuse it", which cannot exist once the values are embedded strings. HEAD
adds `clientModsAreSplitOnCommas`, `whitelistedModsAreSplitOnCommas` and
`buildingAConfigurationCostsTwoRepositoryCalls` in their place, so parsing coverage is **broader** than
develop's, and `anExistingRunConfigurationIsReturnedInsteadOfSavingADuplicate` — the dedup guard — survived.

### What E1 covers, and what it cannot

Coverage was measured, not assumed: every changed production class was checked against develop's test
sources for whether anything names or exercises it.

- **Exercised, therefore proven equivalent:** `GenerationConfig` (34 references), `ApiProperties` (21),
  `WebUtilities` (19), `VersionMeta` (15), `ConfigurationHandler` (9), `ModListCompiler` (7),
  `ModpackZipInspector` (5), `ModpackManifestParser` (4), `ServerPackFileGatherer` (2), and in `-app`
  `RunConfiguration`, `RunConfigurationService`, `ModPack`, `ModPackService`, `VersionChecker`,
  `UpdateChecker`, `ConfigEditorViewModel`, `AmountStatsService`.
- **Exercised *indirectly*, which the class-name check initially missed:** `QuiltPackScanner` — the one
  genuinely algorithmic rewrite (search-per-entry → index-by-jar) — is driven by `ModScannerSidenessTest`
  (30 `quilt` references), `ModScannerTest` and `ModListCompilerTest` (15), all green.
  `ForgeAnnotationScanner` likewise, via `ModScannerDispatchTest`/`ModScannerTest` and the `1.12` fixtures.
- **The `FilterMatcher` rewrite is covered across every mode**, not just the common one: develop's
  `ModListCompilerTest` exercises `CONTAIN` (5), `START` (3), `END`, `REGEX` and `EITHER`, and it passed
  unedited.
- **Genuinely outside E1's reach:** `SuggestionProvider` and `ConfigCheckTimer`/`TabbedConfigsTab` —
  develop's tests never mention them (`Suggestion`, `CheckTimer`: zero hits). The first is covered by
  HEAD's own `SuggestionProviderTest`, which pins content and not just call counts (size 550, an exact
  changed set, per-caller copies). The Swing glue in the other two has no automated coverage on **either**
  side; that is pre-existing, and hand-verification is the only evidence there has ever been.
- **`AmountStatsService`**: `findAll().size` → `count().toInt()`. Equivalent by definition — both are "how
  many documents" — so no differential test was written; `count()` is additionally *more* robust, since it
  cannot trip over a dangling `@DBRef` the way loading every document can.

## Verified against a real MongoDB and the real jar

E1 proves the *tested* behaviour is preserved. It says nothing about the three changes whose whole point is
what a real database does. Those were run for real: MongoDB 8.0.5 in Docker (the version
`docker/docker-compose.yml` pins), the actual `bootJar` in `-web` mode, seeded with **pre-branch shaped
documents** — DBRef arrays plus the three id-only collections, exactly what an upgrading installation holds.

| Change | Result |
|---|---|
| `DeclaredIndexCreator` (S1's fix) | `Ensured index 'sha256' on 'modPack'` — and `getIndexes()` confirms `sha256({"sha256":1})` really exists |
| Index creation must not gate startup | With **no** MongoDB running: `Tomcat started` and `Started ServerPackCreatorKt in 2.083 seconds` **first**, then the failed `createIndexes` attempt. The app served throughout. S1's fix confirmed in production form, not just in a test context |
| `RunConfigurationListMigration` on real data | `Migrated 1 of 2 run-configurations` — the legacy document rewritten to `["OptiFine","Sodium"]` / `["-Xmx4G","-Xms2G"]` / `["Ping-Wheel-"]`, every other field intact (`mc=1.20.1 loader=Forge ver=47.2.0`), and the already-embedded document **untouched**. Idempotency demonstrated on real documents rather than argued |
| Orphan collections | All three dropped, after the rewrite: only `modPack` and `runConfiguration` remain |
| R3's REST contract | `GET /api/v2/runconfigs/all` on the running app returns `"clientMods": ["OptiFine","Sodium"]` and `"id": "rc-legacy-1"` — a **string**, which also confirms U1's id-type correction empirically |

That is the strongest evidence on this branch, and it is the evidence the branch previously had none of:
before this iteration the migration that rewrites persisted data had **never been run against a database**.

## HIGH

### V1 — The web application ignores its configured database and uses MongoDB's default `test`

Found while setting the above up, and the most consequential finding of the session — **but not this
branch's**, which is why it is reported rather than fixed.

Launching the built `bootJar` in `-web` mode, the client is created with
`clusterSettings={hosts=[localhost:27017]}`, **`credential=null`**, and every write lands in the database
named **`test`**. Reproduced three times, including with a hand-written clean
`spring.data.mongodb.uri=mongodb://localhost:27017/serverpackcreatordb` in the home's
`serverpackcreator.properties`. Proof it is not the seed data: the sha256 index was created on
`test.modPack`, while a `serverpackcreatordb` seeded with the identical legacy documents was left
completely untouched — and the migration reported `No run-configurations needed migrating (0 inspected)`.

Two contributing observations, neither conclusive on its own:

- A freshly generated home writes the URI **triple-escaped** —
  `spring.data.mongodb.uri=mongodb\\\://user\\\:password@...` — which reads back as
  `mongodb\://user\:password@...`, with *literal* backslashes. `WebserviceConfig.FALLBACK_DATABASE_URI` is
  the Kotlin literal `"mongodb\\://user\\:password@localhost\\:27017/serverpackcreatordb"`, so the
  backslashes are in the value before `Properties.store` escapes them again.
- The packaged `application.properties` sets
  `spring.config.import=classpath:/application.properties,classpath:/serverpackcreator.properties,…` — it
  **imports itself**, and the second entry is not marked `optional:` yet no such resource exists in the jar.

**Not caused by the performance work, and provably so:** `WebserviceConfig.kt` has a *zero* diff against
develop, and neither the import chain nor the fallback constant was touched. `credential=null` also means
this cannot be what a working authenticated deployment does, so the docker path presumably supplies the URI
by a route this local launch does not — which is exactly why the mechanism needs someone who owns that
plumbing, not a guess from here.

Consequences worth stating plainly: a self-hosted instance may be writing to `test`; and the DBRef→embedded
migration, run against a correctly-configured database, would find `0 inspected` and silently leave data in
the old shape — which the mapped type can no longer read.

## LOW

### V2 — U3's dead end has the same cause, so it is one finding rather than two

Iteration 5 recorded that shortening the driver's server-selection timeout in test resources "does not
work". A second attempt via `@SpringBootTest(properties = …)` also failed — and a probe showed the URI
*does* reach the environment (`Inlined Test Properties -> …?serverSelectionTimeoutMS=250`) while the client
still used `serverSelectionTimeout='30000 ms'`. Together with **V1**, the pattern is one thing, not two:
**`spring.data.mongodb.uri` does not reach the MongoClient in this application**, in tests or at runtime.
The exact mechanism is unidentified and deliberately not chased further. Both attempts are reverted; the
~30 s per no-database context boot stands as an accepted cost, now with a stated cause to investigate
rather than two mysteries.

### V3 — Findings closed this iteration

- **U1** — the spec's id types. Wider than R3's caveat implied: **10 path parameters** and **10 properties**,
  including `ServerPack.fileID` typed `int64`. Corrected against the entities, which all use
  `@MongoId(FieldType.STRING)`; `size`/`downloads`/`confirmedWorking` left as integers because they are.
  `ServerPackView.id` and `ModPackView.id` left alone and reported: those schemas describe classes that no
  longer exist anywhere, so a type for them would be invention.
- **U2** — decided rather than half-done. The render-vs-prop rule is recorded in the frontend's `CLAUDE.md`
  with its mutation evidence; `SubmitModPackForm`'s three tooltip sites stay untested by the same call
  already made for the tables (two layers of lazy Quasar rendering, display-only duplicates), and the
  reasoning is written down so the gap is a decision.
- **R6** — restated as won't-fix. Relocating a markdown file across five commits' history is the "make it
  pretty" tail, and the branch is local-only either way.

## Summary

**Nothing is broken relative to `develop`.** 490 pre-existing guards pass unmodified against HEAD's
production code; the one adaptable exception was adapted without touching a single assertion and passes; the
one non-adaptable exception asserts behaviour the branch deliberately removed, and its replacement covers
strictly more. Every algorithmic rewrite — the Quilt index, the zip single pass, the exclusion-filter
matcher across all five modes, the Forge regex hoist, the gatherer hoist — is exercised by develop-era tests
that were never edited.

Beyond equivalence, the three changes that only a real database can exercise were exercised by one: the
index is really created, the migration really converts legacy documents and really leaves migrated ones
alone, and the REST response really carries the shape the corrected spec describes.

The one thing to act on is **V1**, and it is not this branch's: the web mode appears to use MongoDB's
default `test` database rather than the configured one. It needs whoever owns the config plumbing, and it
is worth treating as urgent, because it would also make this branch's migration a silent no-op on a
correctly-configured instance.

---

# Audit — the Forgejo CI migration, iteration 1

**Range:** `8195c413c..claude-forgejo-ci` (1 commit, `ci: move CI/CD to Forgejo, and retire GitLab CI`)
**Date:** 2026-08-21 · **Mode:** READ-ONLY
Focus: **faithfulness and completeness.** Workflows have no test suite, so the conventions' TDD rules have
nothing to bite on here; what can go wrong instead is a job silently lost, a reference that cannot resolve,
or a credential that is the right *name* on the wrong *forge*. All three happened.

## Verified clean first

- **All 20 GitLab jobs are accounted for** — `Build Test`, `Qodana`(+`Post`), `Docker Test`,
  `Generate Release`, `Build Release`, `Sign Java Publication`, the four `Publish *`, both
  `Build Docker *`, `Writerside Build`, the three `Writerside Docker *`, `pages`,
  `Update README:on-schedule`, `release_job`. One case within them is not — see **C4**.
- **No dangling internal references.** Every `needs:` names a real job, every
  `needs.<job>.outputs.<x>` is declared by that job, and every `steps.<id>.outputs.*` has a step with
  that id. Checked programmatically across all 13 workflow files, not by eye.
- **All 13 workflow files parse as YAML**, and `./gradlew build` is green with the retargeted Maven
  repository — all four `publishMavenJavaPublicationTo*Repository` tasks still generate, so the names CI
  invokes still exist.

## HIGH

### C1 — The PGP signing key is passed as a command-line property, and it cannot survive that

`release-build.yml`'s `maven` job builds
`COMMON="-Pversion=$V -PsigningKey=${{ secrets.SIGNING_KEY }} -PsigningPassword=…"`.

An armoured PGP private key is **multi-line**. Splicing it into a shell variable that is then word-split
into `./gradlew` arguments cannot work: the first newline ends the argument, and what reaches
`useInMemoryPgpKeys` is a truncated fragment. Signing then fails — or worse, produces something and fails
later at OSSRH validation, after three other repositories have already been published to.

GitLab never did this. It set the value as a CI variable and `findProperty("signingKey")`
(`publishing-conventions.gradle.kts:100`) picked it up, because Gradle resolves `findProperty` from
`ORG_GRADLE_PROJECT_<name>` environment variables as well as from `-P`. That is the mechanism this must
use, and it is also the one that keeps a private key out of the process argument list — where `-P` would
expose it to anything that can read `/proc/<pid>/cmdline`.

**Fix:** `ORG_GRADLE_PROJECT_signingKey` / `ORG_GRADLE_PROJECT_signingPassword` in the job's `env`, and
drop both from `COMMON`.

### C2 — Two GitHub-API actions are handed the Forgejo token

`update-readme.yml:38` and `:44` pass `${{ secrets.GITHUB_TOKEN }}` to
`JamesIves/github-sponsors-readme-action` and `actions-cool/contributor-helper`. On Forgejo,
`secrets.GITHUB_TOKEN` is the **automatically provided Forgejo token** — a credential for
`git.griefed.de`, handed to two actions that query *GitHub's* GraphQL and REST APIs for sponsors and
contributors.

The name is right and the forge is wrong, which is the worst shape for this kind of bug: nothing fails to
parse, and the likely outcome is an empty sponsor list quietly committed over a populated one. The commit
introduced `GH_ACTOR`/`GH_TOKEN` precisely to keep GitHub credentials distinguishable, then did not use
them here.

**Fix:** `${{ secrets.GH_TOKEN }}` at both call sites.

### C3 — The `continuous` tag is moved on the wrong forge, by an action that cannot resolve

`devbuild.yml:319–324` keeps GitLab-era `richardsimko/update-tag` with
`GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}` to move the rolling `continuous` tag. Two defects in one step:

- the reference is **bare**, so Forgejo resolves it against `DEFAULT_ACTIONS_URL`
  (`https://data.forgejo.org`), which does not host it — see **C5**;
- even if it resolved, it is a GitHub action moving a GitHub tag, and the rolling release now lives on
  Forgejo.

This one is not cosmetic. The new "Refresh the Forgejo continuous release" step creates the release with
`target_commitish: develop` against tag `continuous`; if that tag never moves, every dev build publishes
assets under a tag pointing at an old commit, and the source archives fetched from
`/archive/continuous.zip` are of that old commit too — assets and source silently disagreeing.

**Fix:** move the tag on Forgejo with git (`git tag -f continuous && git push --force origin continuous`)
using `FORGEJO_TOKEN`, and delete the action.

## MEDIUM

### C4 — Docs images are no longer built for non-release commits

GitLab had *three* `Writerside Docker*` jobs, and the plain one carried the inverse rules —
`$CI_COMMIT_TAG !~ /release patterns/` — so it published `serverpackcreator-help:<short-sha>` on
**ordinary pipelines**, not tags. `docs.yml` triggers only on `push: tags: ['*.*.*']`, so that case is
gone: help images now exist for releases only.

That may well be what you want (a per-commit docs image is the same noise `docker-test.yml` deliberately
stopped publishing), but the commit did not say so — it presented three jobs collapsing into one as pure
simplification. Either restore a non-tag trigger or record the drop as deliberate.

### C5 — Action qualification is inconsistent, and rests on a setting nobody verified

Four references are fully qualified (`https://github.com/luangong/setup-install4j`,
`…/JamesIves/…`, `…/actions-cool/…`, `…/release-kit/…`) and **four are bare third-party**:
`jmgilman/actions-generate-checksum`, `richardsimko/update-tag`, `tiyee/action-ssh`,
`nogsantos/scp-deploy` (all in `devbuild.yml`).

A bare reference resolves against `DEFAULT_ACTIONS_URL`, which the Forgejo docs give as
`https://data.forgejo.org` — a mirror of common actions, not of arbitrary GitHub repositories. So these
four resolve only if this instance's `DEFAULT_ACTIONS_URL` points at github.com. The commit's own comment
argues the `actions/...@<github-sha>` references are "proven to resolve on this instance", which is sound
for `actions/*` and `docker/*`; extending that confidence to four one-person repositories is not the same
claim, and the Forgejo docs explicitly recommend qualifying.

**Fix:** fully qualify all four. It costs nothing and removes the dependency on an instance setting.

## LOW

### C6 — Two GitLab capabilities dropped without being named

Recorded so they are decisions rather than omissions:

- `Build Release` uploaded the app jar to GitLab's **generic package registry** and then created a
  release *asset link* pointing at it. Forgejo release assets are uploaded directly and get stable URLs,
  so the two-step dance is genuinely superseded — but a consumer with a hard-coded
  `/packages/generic/...` URL loses it.
- `release_job` created a GitLab release whose description linked the changelog on three forges. The
  Forgejo release now carries the changelog section itself, which is better, and the mirror job creates
  the downstream entries.

## Summary

Three HIGH, and they share a shape: **a credential or a reference that is syntactically fine and points
at the wrong system.** C1 would fail signing (or half-publish), C2 would quietly empty the README's
sponsor list, C3 would publish dev assets under a stale tag. None of them would be caught by YAML
validation, which is all this migration has had so far — the reason to look at them by hand.

Recommended: fix C1–C3 and C5 as one commit each, decide C4, record C6.

---

# Audit — the Forgejo CI migration, iteration 2

**Date:** 2026-08-21 · **Mode:** READ-ONLY
Focus: **does the shell and API logic actually work?** Iteration 1 read the workflows; this one *ran* the
steps — extracted each `run:` block from the YAML, substituted the expressions, and executed it against
real inputs (the repository's own `CHANGELOG.md`, a synthetic build tree). That is the only way to check a
workflow that cannot be run on the runner from here, and it found a defect no amount of reading had.

## Iteration-1 fixes confirmed

Signing credentials travel as `ORG_GRADLE_PROJECT_*`; both README actions use `GH_TOKEN`; the `continuous`
tag is moved on Forgejo with git; all four remaining third-party actions are fully qualified; the non-tag
docs image is restored. Re-validated: no dangling references, no bare third-party actions, no
`secrets.GITHUB_*` outside a comment.

## HIGH

### D1 — A final release's notes contain every prerelease's notes as well

`release-build.yml`'s *Extract changelog section* uses:

```awk
$0 ~ "^#+ +\\[?" ver "\\]?" { found=1; next }
found && /^#+ +\[?[0-9]+\.[0-9]+\.[0-9]+/ { exit }
```

The closing bracket is **optional**, so for `ver=8.1.1` the start pattern also matches
`## [8.1.1-beta.2](…)`. That rule runs first and ends in `next`, so the exit rule never sees those
headings — collection simply continues through them.

Measured against the repository's own `CHANGELOG.md`, comparing the extraction with the true section
boundaries:

| requested version | extracted | true section | |
|---|---|---|---|
| `8.1.1` | 5,685 chars | 2,834 | **wrong** — absorbs `8.1.1-beta.2` and `-beta.1` |
| `8.1.0` | 28,807 chars | 14,377 | **wrong** — absorbs three betas |
| `8.1.1-beta.2` | 673 chars | 672 | correct (nothing nests under a prerelease) |

So every *final* release would publish its own changelog followed by the changelogs of the prereleases
that led to it — duplicated content, and for `8.1.0` roughly double the intended notes. Prereleases are
unaffected, which is exactly why this would have shipped: the first Forgejo release cut will be an alpha.

**Fix:** require the closing bracket (`\[` … `\]` — every heading in this file is `## [x.y.z](url)`), so a
prerelease heading falls through to the exit rule. Escape the dots too, so `8.1.1` cannot match `8x1y1`.

## MEDIUM

### D2 — A failed asset upload leaves an incomplete release and a green run

Five `run:` steps that call `curl` in a loop or in sequence have no `set -e`:

| workflow | job | step |
|---|---|---|
| `release-build.yml` | `release` | Create release and upload assets **(loops)** |
| `release-build.yml` | `mirror` | Fetch release notes from Forgejo |
| `release-build.yml` | `mirror` | Mirror to GitHub **(loops)** |
| `release-build.yml` | `mirror` | Mirror to GitLab.com |
| `qodana.yml` | `notify` | Post to Discord |

`curl -sf` returns non-zero on an HTTP error, but without `set -e` the loop continues and the step exits
with the status of its *last* command. So one asset failing to upload — a network blip, a size limit, a
name collision — produces a release that is missing a file while the run reports success. For a release
pipeline that is the wrong failure mode: a loud failure can be re-run, a silently incomplete release gets
downloaded.

The three steps written for this migration in `devbuild.yml`, and both VirusTotal steps, already have
`set -eu`. This is inconsistency, not oversight-by-design.

**Fix:** `set -eu` in all five. Discord stays tolerant on purpose — a missing webhook is already handled
explicitly — but it should not silently swallow a *failed* post.

## LOW

### D3 — Verified working, by execution rather than inspection

Recorded so a future reader does not re-derive it:

- **Tag classification** (`prepare`): accepts `9.0.0`, `9.0.0-alpha.6`, `9.0.0-beta.1`; **refuses**
  `continuous`, `9.0`, `v9.0.0`, `9.0.0-rc.1` with a non-zero exit and a named error. The `continuous`
  refusal matters — the dev-build tag must never start a release.
- **Docs image tags**: `tag/9.0.0` → version + `latest`; `tag/9.0.0-alpha.6` → version only;
  `branch/develop` → 8-char short SHA. All three GitLab jobs' behaviour, from one computation.
- **Asset collection**, against a synthetic build tree containing the traps: 13 assets, with
  `serverpackcreator-app-<v>-plain.jar` and `media/output.txt` correctly excluded, the app jar renamed to
  `ServerPackCreator-<v>.jar`, the dokka zip included, and `checksum.txt` covering 12 files **without
  listing itself** — the ordering fix holds.
- **Forgejo release payload**: built from a real 27-line changelog section containing backticks, brackets,
  parentheses and quotes. Valid JSON, and the body round-trips byte-identically. The `$BODY` expansion is
  safe because parameter expansion does not re-interpret the backslashes JSON encoding introduces.
- **GitLab mirror payload**: valid JSON. Its nested-quoting construction is fragile to read but correct
  for fixed content; left alone rather than rewritten for taste.

## Summary

One HIGH that only execution could find (**D1** — final releases publish their prereleases' changelogs
too, roughly doubling the notes), and one MEDIUM about failure modes (**D2** — an incomplete release that
reports success). Everything else executed correctly against real inputs, including the two payload paths
most likely to break on quoting.

Recommended: fix D1 and D2; leave D3 as the record.

---

# Audit — the Forgejo CI migration, iteration 3

**Date:** 2026-08-21 · **Mode:** READ-ONLY
Focus: **the first real run, and the second.** Iterations 1 and 2 checked that the workflows are internally
correct. This one asks what happens when the world does not cooperate — a mirror that has not caught up, a
job that failed halfway, two pushes landing at once, a re-run. Every finding here is invisible to YAML
validation *and* to executing the steps in isolation, because each is about ordering between jobs.

## Iteration-2 fixes confirmed

Changelog extraction re-verified by execution against five versions: every one now matches the true
section boundaries exactly, and an absent version still falls back. `set -eu` present in all five
previously-silent steps, with Discord's guard still exiting cleanly on a missing webhook.

## HIGH

### E1 — The GitHub mirror can create the tag itself, pointing at the wrong commit

`mirror` POSTs to `…/releases` with `tag_name` and no `target_commitish`. GitHub's documented behaviour is
that **if the tag does not exist, it is created from `target_commitish`, which defaults to the
repository's default branch.**

Mirroring git refs to GitHub is asynchronous — and, for a push-mirror, may not have happened at all when
this job runs seconds after the Forgejo release. So the likely first-run outcome is GitHub creating a
`9.0.0` tag pointing at whatever `main` happened to be, not at the release commit. That is a wrong tag on
a public forge, it will disagree with Forgejo's, and it will then *block* the correct tag when mirroring
does catch up.

**Fix:** pass `target_commitish: ${{ github.sha }}` — the commit the tag was cut from. If GitHub has to
create the tag, it then creates the right one.

## MEDIUM

### E2 — The GitLab mirror fails until the tag has propagated

Same root cause, opposite symptom. GitLab's `POST /projects/:id/releases` requires the tag to exist
already unless a `ref` is supplied; without one it answers `404 Tag Not Found`. Now that the step has
`set -eu` (iteration 2), that is a hard failure of the mirror job on every release where mirroring has not
yet caught up — which is most of them.

**Fix:** pass `ref: ${{ github.sha }}`, which tells GitLab to create the tag at that commit if needed.

### E3 — `devbuild` has no concurrency guard, and this migration made that dangerous

Two pushes to `develop` in quick succession run two devbuilds concurrently. The old GitHub workflow also
had no guard, but it used `ncipollo/release-action` with `allowUpdates`/`replacesArtifacts` — an in-place
update, which overlapping runs survive untidily.

This migration replaced that with **delete-then-recreate** (deliberately, to be sure stale assets are
gone). That is the right shape for one run and the wrong shape for two: run A deletes the release, run B
deletes nothing, A creates, B's create then collides or B attaches its assets to A's release. There is also
a window in which the `continuous` release does not exist at all — on the download page users are pointed
at.

So the guard is not a pre-existing omission being tidied up; the change raised the stakes and should have
brought it along.

**Fix:** `concurrency: {group: devbuild, cancel-in-progress: true}`.

### E4 — A re-run cannot repair a partly-failed release

`release` creates the release unconditionally. Re-running the workflow for the same tag — the ordinary
response to `docker` or `maven` failing — now hits `set -eu` and dies at release creation, because a
release for that tag already exists. `maven` and `docker` would re-run (they do not depend on `release`),
but `mirror` and `virustotal` are skipped, so the exact path that failed cannot be retried.

**Fix:** look the release up by tag first and reuse its id; create only if absent. That makes the job
idempotent and a re-run a repair rather than a second failure.

### E5 — The mirrored release never carries the VirusTotal section, and the mirror does not run last

Two ordering problems with one fix:

- `virustotal` PATCHes the **Forgejo** release body with the scan permalinks, but `mirror` reads that body
  in parallel — both depend only on `release`. So GitHub's copy is written before the section exists and
  never gets it. The workflow this replaced set `update_release_body: true` on the GitHub release, so this
  is a capability lost in the move.
- `mirror` does not depend on `maven` or `docker`, so GitHub can advertise a release whose Maven artifacts
  and container images do not exist yet — or never will, if those jobs fail. The approved plan said the
  mirror "runs last and only on success"; the graph does not honour it.

**Fix:** `prepare → assets → {release, maven, docker} → virustotal → mirror`.

## LOW

### E6 — `update-readme` has no concurrency guard

Its schedule and a manual dispatch can overlap, and both commit and push to `main`. The loser fails on a
non-fast-forward, which is noisy rather than harmful, but the guard costs one line.

### E7 — Verified clean

- Every other workflow has a concurrency group; `release-generate` and `release-build` correctly use
  `cancel-in-progress: false`, so a release in flight is never cancelled by a following push.
- `release-build`'s trigger glob `'*.*.*'` does match prerelease tags such as `9.0.0-alpha.6`, and
  `prepare` refuses anything that is not one of the two release shapes — so a stray tag cannot start a
  release build.
- `virustotal` and `mirror` both take their assets from the same artifact the `release` job published, so
  what is scanned and what is mirrored is what was released, not a rebuild.

## Summary

One HIGH and four MEDIUM, all of them about *when* things happen rather than what they do — the class that
neither YAML validation nor step-level execution can see. E1 and E2 are the same root cause seen from two
sides: the mirror assumes a tag that may not be there yet. E3 and E4 are about the second run rather than
the first. E5 is a capability quietly lost and a dependency the plan specified but the graph did not.

Recommended: fix all six; they are one commit's work and every one of them would show up on the first real
release.

---

# Audit — post-CI-migration commits, iteration 8 (2026-08-21)

**Scope:** `eda82e20f^..HEAD` on `develop` — the eight non-merge commits landed after the Forgejo CI
migration's iteration-3 audit: the audit-file consolidation, the two Forgejo credential fixes, the CI
secrets reference, Griefed's install4j 13 bump, the Qodana de-Clouding, and two documentation
corrections.

## HIGH

**H1 — `2c8de2ce6` breaks every Gradle invocation.** `gradle/libs.versions.toml`, the `install4j`
version ref.

`./gradlew help` — any task at all — fails at `:buildSrc:compilePluginsBlocks`:

```
install4j-gradle-13.1.jar!/META-INF/…kotlin_module Module was compiled with an incompatible
version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.0.0.
```

`install4j-gradle:13.1` is built with Kotlin 2.3. buildSrc's precompiled script plugins are compiled
by **Gradle's embedded** Kotlin — 2.0.x on the wrapper's Gradle 8.14.4 — and the plugin marker is on
that compile classpath (`buildSrc/build.gradle.kts`, `libs.plugins.install4j.marker()`), so the
embedded compiler has to read metadata three minor versions ahead of itself and refuses.

This is the exact failure shape the root `CLAUDE.md` already documents for the coroutines bump
("*binary version of its metadata is X, expected Y*"), with one difference that matters: the catalog's
`kotlin = "2.4.10"` cannot help here. That version governs how the **modules** compile. Build-logic
compilation is bound to whatever Kotlin Gradle embeds, which is why this is a Gradle-version problem
wearing a plugin-version costume.

Two ways out, and they are not equivalent:

- **Gradle 9.x**, which embeds Kotlin 2.3.10–2.3.21 and can read the metadata. A major upgrade against
  a build that still has 20 configuration-cache problems and third-party plugins (jk1 license report,
  siouan frontend, Spring Boot, pf4j) to re-verify. The real fix, and not a drive-by.
- **Keep the Gradle plugin at 12.0.2** while install4j the *tool* and the license stay at 13. Restores
  a known-good build immediately. Unverified risk: a v12 plugin driving a v13 installation against a
  `spc.install4j` now stamped `version="13.1"`. The `media` task needs install4j installed locally and
  is not part of the dev loop, so this cannot be settled from here — it settles on the first release
  build.

## MEDIUM

**M1 — `2c8de2ce6` records no measurement, and the missing measurement is exactly what would have
caught H1.** The project's own convention for build-logic changes is explicit: buildSrc has no test
harness by deliberate choice, so "measure the behaviour before and after, and record both numbers in
the commit message." The commit records nothing. The measurement in this case is one command that
takes four seconds and fails.

**M2 — `2c8de2ce6` collapses two version refs that were deliberately distinct.** `install4j` (the
Gradle plugin) was 12.0.2 and `install4jRuntime` (`com.install4j:install4j-runtime`, `compileOnly` in
`-app`) was 12.0.4 — different because the two artifacts release independently. Both are now 13.1.
Checked: `install4j-runtime:13.1` **does** exist on Maven Central (12.0–12.0.5, 13.0–13.1 published),
so the coordinate is real and this is not a second break. But nothing records that the two are now
expected to move together, and the next bump will have to rediscover whether they must.

## LOW

**L1 — `20cd6edcc` carries three changes under one `fix(ci)`.** The auth-mechanism change (URL →
`extraheader`) is the stated concern; the `GIT_USER`/`GIT_MAIL` move into `env:` and the added
`set -eu` are Boy-Scout cleanup riding along. All three are in the same step and all three are
disclosed in the message, so this is scope, not deception — but the commit could have been two.

**L2 — `5eeb4a7a3` quoted a version in prose and went stale within a day.** `CI-SECRETS.md` said the
install4j license is "used with install4j `12.0.2`"; `2c8de2ce6` moved it to 13.1 the same day. Fixed
by `9832ace60`, which replaced the number with a pointer to where it is declared. Recorded because it
is a recurrence of the defect class the "cite names, not snapshots" convention exists for, committed
by the pass that had just been enforcing it. **Closed.**

## Not findings — verified clean, do not re-litigate

- **`eda82e20f`** — both merged halves diff byte-for-byte against their pre-merge blobs (440 and 1802
  lines, 18 sections). No audit content lost.
- **`710e8ea8f`** — zero `secrets.FORGEJO_` references remain, all eight workflows parse, no
  `GITHUB_`/`GITEA_` reference introduced. Env-var names left as `FORGEJO_*` on purpose, so
  `publishing-conventions`' `System.getenv` calls are untouched.
- **`d4af07b8d`** — the premise was checked, not assumed: a Qodana token is required only for the paid
  linters and is optional for the Community linters, and this job runs `qodana-jvm-community`. The
  job's other Cloud coupling was already gone.
- **`6f7e95c55`** — B25's closure was verified against the shipped resources (`latest.release` has a
  matching `mcserver/` file; 659 files against the 643 the entry described), not inferred from prose.
- **`2c8de2ce6`'s `spc.install4j` regeneration** — structurally identical across the bump: 2 mediaSets
  and 3 launchers before and after, only the stamped version and formatting differ. The 2093/2093
  line churn is a reformat, not a content change.
- **No `-api`/`-app`/`-clientside` source is touched by any commit in scope**, so no
  characterization-test obligation arises, no module boundary moved, and the plugin-facing API is
  untouched. The one-concern-per-commit and test-first rules have nothing to bite on outside H1's
  build change.

---

# Audit — post-CI-migration commits, iteration 9 (2026-08-21)

**Scope:** the same range as iteration 8 plus the four commits that answered it — the iteration-8
report, the install4j marker fix, the corrected marker rule, and the regenerated license agreement.

## HIGH — none

Iteration 8's H1 is closed. `./gradlew build` SUCCESSFUL in 6m 8s with api 354 (1 skipped), app 149,
clientside 88, grinder 233 (19 skipped), plugin-example 3, zero failures — and the plugin is genuinely
applied, not silently dropped: `tasks --all` still lists `install4j` and `media`.

## MEDIUM

**M1 — `20cd6edcc` writes the Forgejo credential with `git config --global`.**
`.forgejo/workflows/release-generate.yml`, the "Authenticate origin" step.

The commit's own stated purpose was getting the token out of a place that hands it back out. It
succeeded against `git remote -v` and push-error text, but `--global` writes it to the runner's home
config, where every later step in the job can read it and where it applies to *every* repository on
that runner, not just this checkout. `update-readme.yml` — the workflow this one was aligned to — uses
`git -c http.extraheader=...` inline, which is never written to disk at all.

`git -c` is not available here, because semantic-release spawns its own git rather than running through
this step. But `--local` is: it writes to the checkout's own `.git/config`, semantic-release runs with
that repository as its working directory, and the blast radius drops from the runner to one clone. The
finding is that the commit reached for the widest scope that worked instead of the narrowest.

**M2 — `CLAUDE.md`'s refactor-state table understates the api suite by 11 tests.** It says
`api | 343 (1 skip)`; the suite reports **354 (1 skipped)**. Checked that this is real rather than
inflated by leftovers: all 59 result XML files in `serverpackcreator-api/build/test-results/test` were
written by the last build, none older. The other five rows are accurate (app 149, clientside 88,
grinder 233/19, plugin-example 3). This is the defect the "cite names, not snapshots" convention names
outright — "suite counts left behind by the tests that were just added" — and it has drifted more than
once in this branch's own commit messages, which said 312 and 313 while the table said 343.

## LOW

**L1 — `696931357` shipped a rule that was wrong, and `0138f8799` had to correct it.** The landmine
generalised from one case to "a plugin needs the marker only when a precompiled script plugin applies
it", which is a natural reading of the evidence and false: buildSrc also needs a plugin when its own
Kotlin source compiles against that plugin's API, which is exactly the situation `licenseReport` is in.
The series self-corrected within one commit and the failed experiment is now recorded as
do-not-re-litigate, so the cost was one broken `:buildSrc:compileKotlin`. Recorded because the pattern
is worth noticing: the fix was verified by measurement, but the *generalisation drawn from it* was not,
and a documented rule is acted on later by someone with less context.

## Not findings — verified clean, do not re-litigate

- **install4j versions are consistent across the whole repo.** No `12.0.2`/`12.0.4` reference survives
  anywhere outside the audit log; the catalog's `install4j` and `install4jRuntime` and both workflows'
  `setup-install4j` steps all read 13.1.
- **`1c9046577` is scoped, not swept.** The regenerated agreement contains exactly one version change
  (`install4j-runtime` 12.0.4 → 13.1) with no other Group/Name/Version line touched, the dependency
  count is 44 either side, and the two tracked copies are byte-identical to each other again.
- **The install4j alias conversion's premise was checked directly, not by analogy.** `buildSrc/src`
  contains no install4j reference at all, and its only third-party imports are `com.github.jk1.license*`
  — which is precisely why the same conversion fails for `licenseReport` and works for install4j.
- **No `-api`/`-app`/`-clientside`/`-grinder` production source was touched by any commit in either
  iteration's scope.** Module boundaries, the plugin-facing API and the characterization-test obligation
  are all untouched; the only executable change in nine commits is which classpath a Gradle plugin sits
  on, and the full suite covers that.

---

# Audit — context/documentation commits, iteration 10 (2026-08-21)

**Scope:** `52a192c13^..HEAD` — the mantra addition, the `.claude/rules` migration, the gitignore
rules, and the BUILD.md CI pointers. All four are documentation or build-hygiene; nothing executable
was touched, so the characterization-test, module-boundary and plugin-API rules have nothing to bite on.

## HIGH

**H1 — `8dd6af0e0` states a context saving it never verified, and a known open bug would nullify it.**
**→ RESOLVED, see iteration 10a below: the mechanism was tested and works. The finding stood for about
an hour; the reasoning below is kept because the *method* it argues for is what settled it.**
`CLAUDE.md`, `.claude/rules/build-layout.md`, `.claude/rules/ci-workflows.md`.

The commit claims "~10,051 → ~6,810 est. tokens resident per session". That number is a **character
count of files on disk divided by four**. It is not a measurement of what actually loads. The whole
value of the change rests on `paths:` frontmatter causing a rule file to load only when a matching file
is touched — and that was asserted from a tool description, never checked against Claude Code's
behaviour or documentation.

Checking it now:
[anthropics/claude-code#16299](https://github.com/anthropics/claude-code/issues/16299) —
*"Path-scoped rules in `.claude/rules/` load into context globally regardless of `paths:` frontmatter"* —
is **open**, reported 2026-01-05 against 2.0.76, labelled `bug` / `area:core` / `has repro` /
`perf:memory`, with no maintainer response and no known workaround. If that regression is still live on
2.1.239, the migration saved **nothing**: the text still loads every session, from a different file,
1,798 characters larger than before.

This is precisely the rule the conventions already state — *"what only a real runtime can answer, ask a
real runtime"* — applied to a claim that looked arithmetic and therefore safe. A char count answers
"how big is this file", never "does this load".

**The correctness risk is smaller than the accounting risk, and worth stating separately.** The two
known bugs bracket the outcome rather than straddling it: #16299 makes path-scoped rules load
*globally* (benign here — the landmines stay always-on, we just gain nothing), and
[#22170](https://github.com/anthropics/claude-code/issues/22170) makes them load *never*, but only for
`~/.claude/rules/`; ours are project-level, which is that issue's documented workaround. Neither
failure mode silently drops project rules. So the landmines are not at risk of vanishing — only the
saving is at risk of being fictional.

Resolvable only outside this session: `/memory` in a **fresh** session lists what actually loaded.
This session cannot answer it — the rule files were created mid-session, so its context snapshot
predates them.

## MEDIUM

**M1 — `8dd6af0e0` carries an unrelated whitespace repair.** The migration touches `CLAUDE.md` and the
two new rule files; it also dedents two lines of `serverpackcreator-api/CLAUDE.md`, which has nothing
to do with lazy loading. One concern per commit says these are two commits. **Mitigating and recorded
as such:** Griefed explicitly asked for the three working-tree items to be committed together, and the
message discloses the dedent in its own paragraph. Noted so the bundling is not read later as an
accident.

## LOW

**L1 — `882f96725` bundles a correction with an addition.** Adding BUILD.md's two pointers and
correcting the "four issue-driven `clientside-*` workflows" claim in
`.claude/rules/ci-workflows.md` are separate concerns in separate files. Disclosed in the message; the
correction was discovered *while* verifying the addition, which is the honest reason they travelled
together.

**L2 — the inaccurate workflow claim still survives in one place.**
`claude-docs/REFACTOR-LOG.md` still says "the four issue-driven `clientside-*` workflows". Verified
against the triggers: three are `issues:`-driven and `clientside-report-reusable.yml` is a
`workflow_call:` helper the others invoke. `882f96725` fixed BUILD.md and the rule file but not the log.
The claim originated in the root `CLAUDE.md` and propagated to three files before anyone checked it —
which is the "cite names, not snapshots" defect class in its copy-paste form.

## Not findings — verified clean, do not re-litigate

- **`f32c820c8`** is correct and well-scoped. Verified with `git check-ignore -v`: the two rule files
  match no ignore rule, `.claude/settings.local.json` matches at `.gitignore:465`. The comment stating
  that `.claude/rules/` and `.claude/skills/` are checked in *on purpose* is load-bearing — without it
  the obvious tidy-up is `.claude/` wholesale, which would un-share the landmines.
- **Every glob in both rule files points at something real.** `.releaserc.yml` exists; `buildSrc` holds
  16 matching files, `.forgejo/workflows` 8, and there are 8 `build.gradle.kts` plus the catalog. Both
  frontmatter blocks parse as YAML with `description` + `paths`.
- **Nothing was lost in the migration.** The three files total 1,798 characters *more* than the original
  single file, that being frontmatter plus the pointers left at both cut sites; heading count is 12
  before and after.
- **The api-docs note was deliberately not migrated.** It was glued to the tail of the CI bullet but is
  about springdoc regeneration, so CI-scoped paths would have stopped it loading when someone edits a
  controller — which is how it drifted to 25 of 44 endpoints before. It stays resident.
- **`52a192c13` grew an always-loaded file by ~1.8k chars while its size was the session's problem**,
  but this is not a finding: it was explicitly requested, and `8dd6af0e0` names it as one of the two
  additions that tripped the threshold rather than pretending otherwise.
- **Root `CLAUDE.md` is 27,243 chars**, comfortably under the ~40,000 floor, whatever H1 resolves to.


---

# Audit — iteration 10a: H1 resolved (2026-08-21)

**H1 is closed, in favour of the migration.** `paths:` scoping works on 2.1.239.

**The test.** Reading `gradle/libs.versions.toml` — which matches `build-layout.md`'s `gradle/*.toml`
glob — caused Claude Code to inject that rule file's entire contents into the session mid-turn, having
demonstrably not been present before. One file read, one observation, conclusive.

That rules out **both** known failure modes at once, which is what no amount of reading could do:

| Failure mode | Would look like | Observed |
|---|---|---|
| [#16299](https://github.com/anthropics/claude-code/issues/16299) loads globally | present before touching anything | absent before the read |
| [#22170](https://github.com/anthropics/claude-code/issues/22170) never loads | absent after touching a match | present immediately after |

So the ~3.6k est. tokens per session is real, and the landmines do reach a session that edits a build
file. The `docs: load the build and CI landmines only when they apply` claim was correct; what was wrong
was calling a char count a measurement.

**The part worth keeping — the verification I prescribed was the wrong one.** H1 said to run `/memory`
in a fresh session. That test cannot distinguish success from failure: a correctly-scoped rule is
*supposed* to be absent from a clean session, so absence reads identically to broken. Griefed ran it,
reported "I don't see them", and that was the **pass** condition being mistaken for the fail condition.
The discriminating test is to touch a matching file *first*, then look — which is now written at the top
of both rule files so the next person does not repeat the round trip.

Two lessons, both about the shape of the error rather than the fact:

- **"Ask a real runtime" has to name a test whose outcomes differ.** Iteration 10 correctly refused a
  disk-based char count as evidence and then prescribed an observation that is identical under both
  hypotheses. Demanding verification is only half the discipline; the other half is checking the test
  can fail.
- **The absence of a signal was nearly read as a defect.** The same trap the check-1 rules in `/doctor`
  describe for passive components — "a zero is the absence of logging, not evidence of disuse" — applied
  here to a memory file, and the audit walked into it one iteration after writing about it.


---

# Iteration 12 — 2026-08-22 — the systemd home-resolution branch (`claude-fix-service-home-resolution`, merged by `8008c4160`)

Scope: the seven commits `refactor(api): inject PathsConfig's working directory` … `docs: record the systemd
home-resolution trap and the ordering it depends on`. Self-audit of a bugfix branch, not a refactor branch: the
reported failure was the grinder dying as a systemd service on `java.io.FileNotFoundException: /log4j2.xml`.
Reproduced before the fix and re-run after it by launching the installed distribution from `/`, which is where
`systemd` starts a unit that does not say `WorkingDirectory=`.

## HIGH

### L1 — The new startup check can kill a perfectly writable home, because the write probe races

`b4704961c` — `serverpackcreator-api/src/main/kotlin/de/griefed/serverpackcreator/api/ApiProperties.kt:1394`
(`requireUsableHomeDirectory`), via `utilities/common/FileUtilities.kt:551` (`testFileWrite`).

The probe writes a **fixed filename**: `File(this, "poke")`, then asserts `file.exists()`, then deletes it. Two
SPC processes probing the *same* home interleave — A writes `poke`, B writes `poke`, A sees it and deletes it, B's
`exists()` returns **false** — and the loser concludes the directory is unwritable. Before this commit that verdict
only made a GUI file-chooser refuse a directory (`GlobalSettings.kt:63`, `:96`, `WebserviceSettings.kt:70`, `:89`),
which is recoverable and visible. This commit put the same probe on the **construction path of every
`ApiProperties`**, behind a `throw`, so the loser of that race now dies at startup with
`IllegalStateException: … home directory is not usable` naming a home that is fine.

Concurrent SPC processes on one host are not hypothetical here — they are the documented normal condition, and the
reason the `Preferences` node was split per host in the first place (grinder daemon + `:serverpackcreator-api:test`
+ the developer's GUI; see the grinder `CLAUDE.md` landmine and its measured 2026-07-30 incident). The grinder
itself constructs two `ApiProperties` per start (log4j's `ConfigurationFactory` instance, then `ApiWrapper`'s), so
the probe now runs twice per start against the daemon's shared home.

Fix: probe with a name that cannot collide — `Files.createTempFile(dir, ".spc-write-probe", null)`, deleted in a
`finally` — keeping `testFileWrite`'s "false on failure, never throw except on a non-directory" contract. Fixing
the utility rather than the call site also removes the race from the four GUI call sites.

## MEDIUM

### L2 — The ordering guard scans past `main`, so it can pass vacuously

`4624a572f` (guard) / `d4eec4bff` (the assertions it grew) —
`serverpackcreator-grinder/src/test/kotlin/…/GrinderSpcEnvironmentTest.kt`,
`theSpcEnvironmentIsClaimedBeforeTheFirstLogStatement`.

`body` is `readText().substringAfter("fun main(args: Array<String>) {")` — `main`'s body **plus every declaration
below it**. `indexOf("log.")` and `indexOf("pinSpcHomeDirectory(")` therefore search text that is not `main`:
today the first match of each is the intended one only because `main` happens to precede the helpers in the file.
Move `pinSpcHomeDirectory`'s declaration above `main`, or remove `main`'s logging, and the guard compares positions
of things it was not asserting about and goes green while the property it pins is broken.

This is exactly the failure class this file has recorded twice ("a guard whose teeth were never checked has
repeatedly turned out to assert nothing"; "a test that only asserts shape is not a pin"). The guard is a source
assertion by necessity — a JVM whose logging is already initialised cannot observe the ordering — but its window
has to be `main` itself.

Fix: extract `main`'s body by brace-matching from its opening brace, and assert inside that window only.

## LOW

### L3 — Pinning SPC's home to the daemon base makes the properties file load twice

`d4eec4bff` — `serverpackcreator-grinder/src/main/kotlin/…/GrinderApplication.kt`. SPC's home is now the same
directory the daemon hands `ApiWrapper.api()` its properties file from, so `PropertyStore` loads
`<base>/serverpackcreator.properties` as both the explicit file and the home candidate, logging `Loaded properties
from …` twice per start (visible in the post-fix run at 23:07:02,357). **Verified harmless:** `PropertyStore.save`
collects into a `TreeSet<File>` (`PropertyStore.kt:216`), so the duplicate collapses and the file is written once.
Recorded so the doubled journal line is not mistaken for a defect.

### L4 — An added side effect went unmentioned in the fix commit

`d4eec4bff` — `GrinderApplication.kt`, `base` gained `.absoluteFile.apply { mkdirs() }`. Harmless and arguably
required now that the home is pinned to it, but it is a behaviour addition the commit message does not state.

### L5 — The root `CLAUDE.md` snapshot header was not moved

`bb6545b18` — `CLAUDE.md:305` still reads **Current status (2026-08-21)** while the table below it was edited
(api 354 → 356, grinder 233 → 237, plus the systemd note). The block is defined by that file as the current
snapshot.

### L6 — The test-side half of the injection landed in the wrong commit

`ada74768d` — `PathsConfigTest.kt`, the `pathsConfig()` helper's new `workingDirectory` parameter is the
counterpart of `3c068ce3a`'s constructor injection and belonged in that refactor commit. Reference-only: verified
that no pre-existing assertion, argument or expected value changed, so the `refactor:` label on `3c068ce3a` and
the `test:` label on `ada74768d` are both still honest.

## Not findings — verified clean, do not re-litigate

- **`3c068ce3a` is behaviour-preserving despite moving `File("").absoluteFile` from per-access to
  construction-time.** Probed directly: the JVM resolves an empty path against the working directory it was
  *launched* with and ignores a later `user.dir` (setting the property mid-process does not move
  `File("").absolutePath`), so the value cannot change during a process's life. Per-access and once-at-construction
  are the same value by construction.
- **No pre-existing assertion changed anywhere on the branch.** `git diff 7780da54a..HEAD -- "*/src/test/*"` is two
  new files plus `PathsConfigTest`, whose only added assertion lines belong to the new test. The stop-and-flag
  signal for a mislabelled refactor does not fire.
- **Logging from inside the log4j-instantiated `ApiProperties` is safe, the new `log.error` in `setLoggingLevel`
  included.** `ApiProperties` *is* log4j's `ConfigurationFactory` (`@Plugin`, `ApiProperties.kt:57`), so log4j
  builds one while configuring itself; the post-fix run from `/` shows that instance's own `loadProperties` and
  `printSettings` INFO lines appearing normally (23:07:02,356–,423), with no recursion, no stall and no lost
  output.
- **SPC's home and the grinder's state can share one directory.** The subdirectories do not collide — SPC uses
  `work/temp` and `work/installers`, the grinder `work/install` and `work/verify` — and nothing in `-api` deletes
  `work/` wholesale (grepped: the only recursive delete near it is `BootWorkspaceReaper`, which sweeps `verify`
  only).
- **A root-owned `/opt` install is not the next failure of this class.** `installLocationXml` — `log4j2.xml` beside
  the jar — is only ever *read* (`ApiProperties.kt:1502`–`1506`), never written, so an unwritable jar folder costs
  nothing.
- **The stored home preference stays authoritative even when unwritable.** Deliberate: a loud, actionable error
  beats silently relocating a configured home and leaving its configs behind. Flagged to Griefed with its
  alternative (skip unwritable stored values too, which would self-heal a stale `/` without any error).

## Resolution (same day, on Griefed's instruction to fix all findings)

- **L1 — fixed.** `test(api): pin that the writability probe cannot be defeated by a name collision` landed the
  guards red (34 of 64 concurrent probes false, plus the deterministic name-collision case), then
  `fix(api): probe writability with a name nothing else can hold` switched `testFileWrite` to
  `Files.createTempFile(dir, ".spc-write-probe", null)` with removal in a `finally`. api and app suites green —
  app matters here, since the four GUI file-chooser call sites live there. Behaviour row appended to
  `claude-docs/API-BEHAVIOUR-CHANGES.md`.
- **L2 — fixed.** The guard's window is now `main`'s own body, cut by brace-matching, and it asserts its own
  boundedness (fails if the window reaches the declarations below `main`). **Teeth checked by mutation:** moving
  both claims back below `main`'s first log statement turns it red with the intended message; reverting turns it
  green again. That check is the whole point of the finding — the previous window would have gone green either way
  once the file was reordered.
- **L3 — documented, not changed.** Removing the duplicate load would mean changing `PropertyStore`'s candidate
  list, which is out of proportion to a log line that costs nothing (the write is already deduplicated by
  `TreeSet`). Recorded in the grinder `CLAUDE.md` beside the existing "expected and harmless" note about the
  dist's own properties copy, so the doubled line is not chased twice.
- **L4 — fixed.** `GrinderApplication` now says why `base` is created at that point: SPC's writability check runs
  against it before anything else of ours would have created it.
- **L5 — fixed.** Root `CLAUDE.md`'s snapshot header moved to 2026-08-22, with the table it heads.
- **L6 — accepted, not fixed.** The misplaced test-helper parameter is commit hygiene in already-merged history;
  rewriting the merge to move two lines is not worth it, and an earlier rewrite on this branch is precisely what
  swept an unrelated untracked file into a commit. Recorded instead.

---

# Iteration 13 — 2026-08-23 — the report bind-address branch (`claude-grinder-report-bind-host`, merged by `030036580`)

Scope: `git log 97f487e0e..HEAD`, 17 commits. Thirteen of them were already audited as iteration 12 and its
resolutions; their conclusions are re-affirmed below rather than re-litigated. Fresh scrutiny falls on the four
new commits — `test(grinder): pin the report's bind address and its wiring`, `feat(grinder): make the report's
bind address configurable`, `docs: record the grinder's report bind address in the status table`, and the merge.

Self-audit of a feature branch, not a refactor branch. The reported failure was a reverse proxy 502ing against
the grinder's report while the report answered fine on the box itself; the cause was `main` never passing
`ReportServer`'s `host`, so it took the loopback default.

## HIGH — none

No behaviour change is mixed into a `refactor:` commit (there are no refactor commits on the branch), no module
boundary moved, and nothing here is on the plugin-facing API — the grinder is not published to Maven.

## MEDIUM

### M1 — `claude-docs/REFACTOR-LOG.md` has no entry for this branch, nor for the one before it

Definition of done, item 4: "Append the blow-by-blow to `claude-docs/REFACTOR-LOG.md`." The file's last entry is
`## 2026-08-17 — web query shapes and the DBRef flattening`. Both the 2026-08-22 systemd home-resolution branch
(iteration 12) and this one landed without one, so the log is two branches stale. Iteration 12 did not catch this
about itself.

### M2 — `7b8e762d2` bundles a test refactor with the new guards

One concern per commit. The commit adds `ReportBindWiringTest` and `ReportServerBindAddressTest` *and* lifts
`mainBody()` out of `GrinderSpcEnvironmentTest` into `GrindTestFixtures` as `grinderMainBody()`. The move is
reference-only — every assertion byte-identical, which the conventions' carve-out explicitly permits to stay a
`refactor:` — but that is an argument for it being its own `refactor(grinder):` commit *before* the guards, not
for merging it into a `test:` one.

## LOW

### L1 — a `0.0.0.0` bind logs a URL nobody can open

`f91cedfdb` — `GrinderApplication.kt`, the `reportUrl` line.

Replacing the hardcoded `localhost` with `$bindHost` is right for a concrete address and wrong for the wildcard:
verified against the JDK's `HttpServer`, `0.0.0.0` binds fine and the line now prints `http://0.0.0.0:56442/`,
which is not a browsable URL. The old hardcoded text was correct for exactly this case. Regression, narrow.

### L2 — an IPv6 bind produces a malformed URL

Same line. Verified: `::1` binds (`hostString` comes back `0:0:0:0:0:0:0:1`) and the log reads
`http://::1:56443/`. IPv6 literals need brackets — `http://[::1]:56443/` — or the URL is unparseable.

### L3 — four new `!!` assertions

Kotlin conventions, "no **new** `!!` in refactored code": `ReportServerBindAddressTest` has `address!!` twice,
`ReportBindWiringTest` has `read!!` and `construction!!`. All four exist only because `Assumptions.assumeTrue`
and `Assertions.assertNotNull` do not smart-cast. Both have null-safe spellings that read better.

### L4 — `assertThrows(ConnectException)` is tighter than the fact it is pinning

`ReportServerBindAddressTest.theDefaultIsReachableOnLoopbackOnly`. The claim is "not reachable"; the assertion is
"refused with this exact exception". A host that DROPs rather than REJECTs yields `HttpConnectTimeoutException`
after the 5 s timeout, and the guard then *fails* on a box where the property it guards actually holds.

### L5 — the ephemeral port is chosen on loopback and assumed free on the other interface

Same test. `requestedPort = 0` allocates a free port *for 127.0.0.1*; the guard then connects to
`nonLoopbackIp:thatPort`. A process bound specifically to that address and port would make the connection succeed
and the guard fail. Rare, not impossible; recorded rather than fixed, since every fix costs more than the flake.

### L6 — the README's "fails loudly at startup" was written before it was checked

§5 *Exposing the report* asserts that a stale bridge subnet makes the bind fail at startup "rather than silently
falling back". True — verified after the fact: an unassigned address gives `BindException: Can't assign requested
address` and a typo'd hostname `SocketException: Unresolved address`. The convention is "document what you
verify"; here the order was reversed.

## Not findings — verified clean, do not re-litigate

- **The guards landed red, in their own commit, for the right reason.** `7b8e762d2` predates any
  `SPC_GRINDER_HOST` in the source, so `theConfiguredBindHostReachesTheReportServer` failed on "main() no longer
  reads SPC_GRINDER_HOST" — the intended assertion, not an accident of its own regex. This is the boundary the
  2026-07-31 audit found collapsed in eight commits; it holds here.
- **The regex widening inside `f91cedfdb` is not the stop-and-flag signal.** It changes how broadly a
  one-commit-old guard *searches*, not what it expects; no expectation, argument or expected value moved. The
  signal is about existing assertions changing under a `refactor:` label, and this is a `feat:`.
- **No pre-existing assertion changed anywhere on the branch.** `git diff 3e873af88..HEAD -- "*/src/test/*"` is
  two new files plus the reference-only helper move.
- **`0.0.0.0` is not the recommended value and the docs say so.** README §5 recommends the bridge gateway and
  states why (the report is unauthenticated end to end — `/`, `/status` and `/export.csv` all answer
  unconditionally, no auth anywhere in `ReportServer.start()`).
- **No `API-BEHAVIOUR-CHANGES.md` row is owed.** The grinder is not published and not plugin-facing; the changed
  surface is a log line and an environment variable.
- **Iteration 12's resolutions stand.** `testFileWrite` still probes via `Files.createTempFile` with removal in a
  `finally`, and its behaviour row is still in `claude-docs/API-BEHAVIOUR-CHANGES.md`.

## Summary

| Severity | Count | Fixable in place |
|---|---|---|
| HIGH | 0 | — |
| MEDIUM | 2 | M1 yes; M2 no — already-merged history |
| LOW | 6 | L1–L4 yes; L5 recorded; L6 already verified, wording only |

## Resolution (same day, on Griefed's instruction to fix all findings)

- **M1 — fixed.** `claude-docs/REFACTOR-LOG.md` backfilled with both missing entries: the 2026-08-22 systemd
  home-resolution branch (iteration 12's subject, which had none either) and this one.
- **M2 — accepted, not fixed.** Splitting `7b8e762d2` means rewriting history already merged into `develop`. The
  same call iteration 12 made for its L6, and for the same reason: an earlier rewrite on this project is exactly
  what swept an unrelated untracked file into a commit. The lesson was applied going forward instead — the L1/L2
  fix below landed as four commits (extract, pin red, fix, clean) rather than one.
- **L1 and L2 — fixed.** `refactor(grinder): extract the report URL from main()` made the line testable,
  `test(grinder): pin the logged report URL for wildcard and IPv6 binds` landed red on three of four cases, and
  `fix(grinder): make the logged report URL openable for wildcard and IPv6 binds` turned them green. A wildcard
  is reported as the loopback the report is certainly answering on; IPv6 literals are bracketed.
  `concreteIpv4AddressesAreLeftAlone` was green before *and* after, so the ordinary path is demonstrably
  untouched.
- **L3 and L4 — fixed.** `refactor(grinder): drop the bind guards' !! and over-tight exception type`. All four
  `!!` are gone: `nonLoopbackIpv4()` returns `String` and throws `TestAbortedException` itself (JUnit reports the
  same skip the assumption did), and the two regex lookups use elvis into `Assertions.fail`, which returns
  `Nothing`. The unreachability assertion widened from `ConnectException` to `IOException`, so a host that DROPs
  rather than REJECTs no longer fails a guard whose property holds.
- **L5 — recorded, not fixed.** Standing: every available fix costs more than the flake it prevents.
- **L6 — fixed.** README §5 now names what was actually observed — `BindException: Can't assign requested
  address` for an unowned address, `SocketException: Unresolved address` for a name that does not resolve — so a
  reader can recognise either rather than take the claim on faith.

Suite after the resolutions: grinder **245**, zero failures.

---

# Iteration 14 — 2026-08-23 — the deployment files (`feat(grinder): ship an example systemd unit and an installer`)

Scope: the two commits adding `serverpackcreator-grinder/deploy/` and its guard, audited immediately after
landing. Shell and unit files, so the "measure it rather than test it" ceiling from the conventions applies —
every finding below was reproduced against a real shell before being written down.

## HIGH — none

## MEDIUM

### M1 — the installer's overridable paths can silently disagree with the unit that has to run them

`install-grinder.sh` honours `PREFIX`, `SERVICE_USER` and `SERVICE_HOME` from the environment; the shipped unit
hardcodes `ExecStart=/opt/spc-grinder/…`, `User=grinder` and `WorkingDirectory=/home/grinder`. Override any one
and the install still reports success, having produced a deployment the unit cannot start. The failure surfaces
later as a systemd start error with no connection back to the override.

## LOW

### L1 — `${PREFIX:?}` guards emptiness and nothing else

`rm -rf "${PREFIX:?}/lib"` is protected against an *unset* PREFIX, which is not the dangerous case. `PREFIX=/`
makes it `rm -rf /lib`; `PREFIX=/usr` makes it `rm -rf /usr/lib`. Both were reachable.

### L2 — "one sudo prompt up front" is a claim the timestamp cannot keep

`sudo -v` caches for ~15 minutes by default. A cold `docker build` of the runtime image plus a Gradle
`installDist` routinely exceeds that, so the comment promising a single prompt was wrong in exactly the case it
was written for.

### L3 — the header told you to run it from a directory it does not care about

"Run it from the repository root" — but `repo_root` is derived from `BASH_SOURCE`, so the working directory is
irrelevant. A doc line contradicting the code beside it.

## Not findings — verified clean, do not re-litigate

- **`cp -a bin lib "$PREFIX/"` merges on a re-run rather than nesting `bin/bin`.** The obvious suspicion about
  re-running the installer; reproduced in a scratch tree instead of reasoned about — a changed file was
  overwritten (`v1` → `v2`), a new file appeared, and no nested `bin/bin` was created.
- **The exec bit survives git.** `git ls-files -s` reports `100755` for `install-grinder.sh`.
- **The `deploy/` gitignore exception works.** `git check-ignore -v` exits 1 for both files. The rule it escapes
  is the JDeveloper/IDEA template's "default output directories" block, which swallowed the first commit
  attempt silently — the commit reported success having added nothing.
- **The unit's three hardcoded values agree with the installer's three defaults.** Parsed out of the unit and
  compared: `grinder`, `/home/grinder`, `/opt/spc-grinder/bin/serverpackcreator-grinder`. M1 is about overrides,
  not about the shipped defaults.
- **`SystemdUnitConfigurationTest` has teeth.** Four mutations, four distinct failures, unit restored
  byte-identical afterwards — recorded in that commit's message rather than assumed from a green run.

## Resolution (same day)

- **M1 — fixed.** The installer now parses `User=`, `WorkingDirectory=` and `ExecStart=` out of the unit and
  compares them against the values it is installing with, printing exactly what to change. A warning rather than
  a hard failure: an operator who has already edited their own copy is doing nothing wrong.
- **L1 — fixed.** `PREFIX` must now be absolute and at least two components deep. Verified: `/` and `/usr` are
  both rejected by name, a relative path is rejected as non-absolute, and `/opt/spc-grinder` passes.
- **L2 — fixed.** `sudo -v` is refreshed immediately before the privileged block, and the comment now says what
  actually happens instead of promising something the timestamp cannot deliver.
- **L3 — fixed.** The header says the working directory does not matter and why.

Suite after the resolutions: grinder **249**, zero failures, 39 classes.

---

# Iteration 15 — 2026-08-23 — third pass over `97f487e0e..HEAD`, auditing iterations 13 and 14's own fixes

Scope: the full 25-commit range, with the eight commits produced by iterations 13 and 14 getting the scrutiny —
a fix is a change like any other, and this pass exists because the first two passes had not been audited by
anything. Two of the three findings below are defects the earlier *fixes* introduced.

## HIGH — none

## MEDIUM

### M1 — `ad7aff574` is labelled `refactor:` while changing an existing assertion

`refactor(grinder): drop the bind guards' !! and over-tight exception type` changed
`assertThrows(ConnectException::class.java, …)` to `assertThrows(IOException::class.java, …)`. The conventions
are explicit: "If an **existing** test's *assertion, argument or expected value* has to change, the label is
already wrong — that is the stop-and-flag signal, not a formality." The reference-only carve-out does not apply,
because no symbol moved; the expectation genuinely widened. `fix(test)` or `test:` was the honest label.

Recorded rather than corrected: the commit is on `develop`, and the project's standing decision — iteration 12's
L6, iteration 13's M2 — is that rewriting merged history to relabel a commit costs more than it returns. The
body describes the change accurately; only the type lies. Same disposition as `358675fbf`, already in this file.

## LOW

### L1 — the iteration-13 URL fix double-brackets an already-bracketed IPv6 literal

`reportUrl` bracketed anything containing a colon, so `SPC_GRINDER_HOST=[::1]` produced
`http://[[::1]]:8757`. The bracketed form is not a user error — verified against `HttpServer`, which binds
`[::1]` and reports `0:0:0:0:0:0:0:1`. Fixed, pinned red first.

### L2 — the iteration-14 header fix silently truncated `--help`

L3 of iteration 14 rewrote the script's header comment from three lines to four. `--help` printed a fixed
`sed -n '2,21p'` range, so the extra line pushed `--skip-image` out of the output entirely — the flag stopped
being documented by the very commit that improved the documentation above it. Fixed structurally with an `awk`
that prints the contiguous comment block, rather than by bumping the number to 22 and waiting for it to rot.

This is the "cite names, not snapshots" rule from `CLAUDE.md` reappearing in a shell script: a line-number
citation went stale inside one commit.

## Not findings — verified clean, do not re-litigate

- **`reportUrl` handles hostnames correctly.** A name like `grinder.internal` contains no colon, so it is not
  bracketed. Only literals and the two wildcards take a branch.
- **The wildcard substitution is complete.** `0.0.0.0`, `::` and the JDK's expanded `0:0:0:0:0:0:0:0` are all
  mapped; a bind to any of them is reported on the loopback it is certainly answering on.
- **Iteration 14's four fixes hold.** The `PREFIX` shape guard rejects `/`, `/usr` and a relative path by name
  and passes `/opt/spc-grinder`; the unit/installer consistency check parses `grinder`, `/home/grinder` and
  `/opt/spc-grinder/bin/serverpackcreator-grinder` out of the shipped unit and agrees with all three defaults.
- **Iteration 13's L3 and L4 fixes hold.** No `!!` remains in either bind guard, and both still pass with the
  interface up.

## Summary

| Severity | Count | Disposition |
|---|---|---|
| HIGH | 0 | — |
| MEDIUM | 1 | M1 recorded — merged history, body honest, type wrong |
| LOW | 2 | both fixed, both introduced by the two preceding iterations' fixes |

Suite: grinder **250**, zero failures.

---

# Iteration 16 — 2026-08-23 — targeted audit of `deploy/install-grinder.sh` and `deploy/spc-grinder.service`

Scope: the two deployment files only, on request. Not a commit-convention pass — a review of what the script and
the unit actually do on a real Linux host. Every finding below was reproduced against a real runtime (shellcheck,
`systemd-analyze verify`, and `useradd` in Debian containers) rather than reasoned about, per the conventions'
"what only a real runtime can answer, ask a real runtime".

## HIGH

### H1 — nothing provides or checks Java, and the launcher cannot start without it

Neither file mentions `java` or `JAVA_HOME` — `grep -c` returns 0 for both. The Gradle launcher requires one or
the other and dies otherwise:

    ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

systemd gives a unit a minimal `PATH` (`/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin`) and no
`JAVA_HOME` at all. A distro-packaged JDK lands in `/usr/bin/java` and works by luck; a Temurin tarball under
`/opt`, SDKMAN, asdf or a JDK in the *installing user's* profile all produce a service that never starts. The
installer builds with Gradle, so Java is on the *operator's* PATH — which is exactly what makes this invisible
until the first `systemctl start`. This is the most likely first-run failure of the whole deployment and neither
file says a word about it.

### H2 — an existing account is silently granted root-equivalent privilege

`install-grinder.sh:128–148`. When `$SERVICE_USER` already exists the script prints "leaving it alone" and then
runs `usermod -aG docker` on it anyway. Docker group membership is root-equivalent — `docker run -v /:/host`
hands out the whole filesystem — so pointing `SERVICE_USER` at an existing human account escalates that account
to effective root, reported as the single line "added X to the docker group". The "leaving it alone" message
immediately above makes it read as though nothing was changed.

## MEDIUM

### M1 — `Group=grinder` may name a group that was never created

Reproduced in `debian:stable`. With the stock `/etc/login.defs` the group exists:

    uid=999(grinder) gid=999(grinder) groups=999(grinder)      group grinder EXISTS

With `USERGROUPS_ENAB no` — a supported setting, and the default on some hardened images — it does not:

    uid=998(g2) gid=100(users) groups=100(users)               group g2 MISSING

The unit's `Group=grinder` then refers to nothing and the service fails at start. The script never verifies the
group exists, and `install-grinder.sh:139`'s `chown "$SERVICE_USER:$SERVICE_USER"` would fail for the same
reason if it were reached.

### M2 — a failed upgrade leaves the service stopped

`install-grinder.sh:100–105` stops a running service, and `set -e` means any later failure — `cp`, `chown`,
`useradd`, a full disk — exits the script before `:179` restarts it. The upgrade path therefore converts a
transient error into an outage that persists until someone notices. Nothing unwinds the stop.

### M3 — `--install-unit` installs a unit the script has just warned does not match

The consistency check (`:150–168`) runs *before* the install block (`:171–177`), so with an override in play the
script prints "WARNING: the unit does not match this install" and then installs that unit regardless. The
ordering makes the warning read as though it had been acted on.

## LOW

### L1 — `set -E` is inert

`set -Eeuo pipefail` at `:24`. `-E` only propagates an `ERR` trap into functions and subshells, and no `trap …
ERR` is ever installed. Harmless, but it advertises error handling the script does not have — and an ERR trap
reporting the failing line is also the natural fix for M2.

### L2 — `chmod -R a+rX` adds permissions and never removes any

`:122`. `cp -a` preserves the build tree's modes, which come from the operator's umask. A umask of `002` or `000`
carries group- or world-writable modes into `/opt/spc-grinder`, and anyone who can write there controls what the
service executes. `a+rX` cannot undo that; `go-w` would.

### L3 — `docker build` without `--pull`

`:85`. A cached base image silently persists, so "rebuild the image" may not pick up a new base. A tradeoff
rather than a defect — `--pull` costs a registry round trip on every run — but it is currently an unstated one.

### L4 — no `SyslogIdentifier=`

Journal lines are tagged with the launcher's name rather than `spc-grinder`. `journalctl -fu spc-grinder` works
regardless, since that selects by unit; this only affects how lines read when grepping the whole journal.

### L5 — `Documentation=` points at the mirror

The unit cites `github.com/Griefed/ServerPackCreator` while `origin` is `git.griefed.de`, which `CLAUDE.md` names
as canonical. Defensible — GitHub is the public-facing one — but it should be a decision rather than a default.

## Not findings — verified clean, do not re-litigate

- **shellcheck is clean at `-S style`.** Exit 0, no output, run via `koalaman/shellcheck:stable`. No quoting,
  word-splitting, expansion or subshell defects — including the `$(…)` inside the `echo` at `:113`.
- **`systemd-analyze verify` reports no unit defects.** Run in `fedora:latest` with systemd installed. The only
  two lines are `Unit docker.service not found` and `Command /opt/spc-grinder/bin/serverpackcreator-grinder is
  not executable` — both artefacts of verifying inside a container that has neither. No syntax errors, unknown
  directives or deprecated options.
- **The state directory is private by default.** `useradd --create-home` produced `drwx------ grinder grinder
  /home/grinder`.
- **`ProtectSystem=full` does not restrict anything the service needs.** It covers `/usr`, `/boot` and `/etc`,
  leaving `/opt` (binaries), `/home` (state) and `/run` (the docker socket) writable.
- **The restart limiter never trips, deliberately.** `RestartSec=30s` against systemd's default
  `StartLimitIntervalSec=10s`/`StartLimitBurst=5` means two restarts never fall inside one window, so a
  permanently broken service retries forever instead of being given up on. Correct for a daemon meant to survive
  a reboot loop; noted so nobody "fixes" it by accident.
- **Iteration 14 and 15's fixes hold.** `cp -a` merges rather than nesting, the exec bit survives git as
  `100755`, the `PREFIX` shape guards reject `/`, `/usr` and relative paths, and `--help` prints the whole header.

## Summary

| Severity | Count | Note |
|---|---|---|
| HIGH | 2 | H1 breaks the first start on most non-distro JDKs; H2 is a silent privilege grant |
| MEDIUM | 3 | M1 host-config dependent, M2 turns an error into an outage, M3 an ordering bug |
| LOW | 5 | L1–L2 worth fixing, L3–L5 are judgement calls |

Not fixed — reported for a go-ahead, per the audit's read-only rule.

## Resolution (2026-08-23, on Griefed's instruction to fix all findings)

Every finding fixed. Verified on real runtimes — Debian and Fedora containers — rather than by inspection, since
all ten are about what happens on a host this machine is not.

- **H1 — fixed**, guard first (`test(grinder): pin that the unit tells the operator how to provide Java`, red on
  "the unit does not mention JAVA_HOME"). The unit gained a JVM section quoting the exact launcher error and
  systemd's real `PATH`, plus `JAVA_HOME`, `JAVA_OPTS` and `SERVERPACKCREATOR_GRINDER_OPTS`; those three are
  read by the Gradle launcher rather than any Kotlin, so the phantom-variable guard whitelists them as
  launcher-read. The installer checks with `env -i PATH=<systemd's>`, deliberately *not* the caller's
  environment. Observed firing in a JDK-less container with the full warning text.
- **H2 — fixed.** An account the script creates is still added to `docker` automatically; one that already
  existed now needs `--grant-docker`. Verified both ways against a pre-existing account: refused with
  `id -nG grinder` still reading `grinder`, then granted to `grinder docker` with the flag.
- **M1 — fixed.** The script reads `Group=` out of the unit and creates that group when missing. Verified on the
  host shape that produced the finding — `USERGROUPS_ENAB no`, where `useradd` gave `gid=100(users)` and no
  group — after which `getent group grinder` resolves and the unit's `Group=` is valid.
- **M2 and L1 — fixed together.** `set -E` now has something to propagate: an `ERR` trap reporting the failing
  line, and an `EXIT` trap that restarts the service when the install died after stopping it. The inert flag and
  the silent outage were the same omission.
- **M3 — fixed.** The consistency check moved into preflight, before anything is built or changed, and is fatal
  when `--install-unit` would install the mismatched unit. Verified: `SERVICE_USER=someoneelse … --install-unit`
  now dies at "Checking prerequisites" having touched nothing.
- **L2 — fixed.** `chmod -R go-w` before `a+rX`. Verified `755`, `root`-owned.
- **L3 — fixed.** `--pull` by default, `--no-pull` for an offline rebuild.
- **L4, L5 — fixed.** `SyslogIdentifier=spc-grinder`; `Documentation=` lists `git.griefed.de` before the GitHub
  mirror.

Re-verified after the changes: `shellcheck -S style` exit 0, `systemd-analyze verify` reporting only the two
container artefacts, grinder suite **251**, zero failures.

---

# Audit iteration 17 — 2026-08-23 — the container-identity branch (`claude-grinder-uid-browser-properties`)

Scope: `git log develop..HEAD`, ten commits. Three unrelated production defects found from a live grinder run
plus one new endpoint. Base for the comparison is `develop` at `845fb6381`.

**Method.** Read every commit's diff against the conventions; re-read the four new units in full rather than
their diffs; ran `:serverpackcreator-grinder:test` (268, 19 skipped) and `:serverpackcreator-clientside:test`
(90, 0 skipped), both green. Counts re-derived from `build/test-results/test/*.xml` of that run.

## HIGH

None. No behaviour change is hidden inside a `refactor:`, no module boundary is crossed (`-grinder` and
`-clientside` gained nothing pointing outward; the new endpoint reads `ApiWrapper` in the composition root only),
and nothing `serverpackcreator-api` exports changed shape or behaviour.

## MEDIUM

- **M1 — `docs(grinder): close the deployment gaps this outage ran into` carries a production signature
  change despite its `docs` label.**
  `container/ContainerUser.kt:60` loses its default argument (`override: String? = System.getenv(ENV_KEY)` →
  `override: String?`) and the read moves to `GrinderApplication.kt:88`. Behaviour is preserved, so this is a
  *pure refactor* mislabelled as documentation, not a behaviour change in disguise — but it is exactly the
  "one concern per commit" violation the conventions single out, and the file's own precedent (`358675fbf`) is
  explicit that the remedy is cheap before a merge and unfixable after. The branch is unpushed, so it is still
  cheap. **Fix:** split the two-file code change out of the docs commit.

- **M2 — `9df60fca8` changes three behaviours and pins one.** `BrowserDownloader.kt` swallows the download
  abort (pinned by `isDownloadAbort`), *and* switches both navigations from Playwright's default `load` to
  `DOMCONTENTLOADED`, *and* raises the 30s default to a configurable 60s. The latter two have no guard of any
  kind. Grouping related behaviour changes is allowed; leaving two of them unpinned is not. The repo already
  has the technique for a join no test can execute — `ReportBindWiringTest` asserts against `main`'s own
  source text — and the same applies here, since exercising the options needs a live Chromium.

- **M3 — `6be42c479` leaves the endpoint's production wiring unpinned.** `ReportServerTest` supplies its own
  `fallbackLists` lambda, so nothing asserts that `GrinderApplication.kt:186` hands the endpoint SPC's *real*
  `clientsideMods`/`modsWhitelist`. This is the identical gap `ReportBindWiringTest` was written to close for
  `SPC_GRINDER_HOST`: the endpoint could be wired to an empty list and every test would stay green while every
  polling client silently received nothing.

- **M4 — a comma inside an entry silently corrupts the published list.**
  `report/FallbackPropertiesRenderer.kt:124` passes character 44 through verbatim (it is inside the printable
  range), and the consumer — `UpdateConfig.updateFallback` — does `newBlacklist.split(",")`. One stem
  containing a comma therefore arrives at every client as two bogus entries, each of which is a `startsWith`
  matcher against real mod filenames. Filenames may legally contain commas, and `FilenameStemDeriver` derives
  stems straight from them, so this is reachable without anything unusual happening. Silent at both ends.

- **M5 — a malformed `SPC_GRINDER_CONTAINER_USER` is discarded without a word.**
  `container/ContainerUser.kt:63` requires `\d+:\d+`, and anything else (`1000`, `grinder:grinder`, a stray
  quote) falls through to the directory owner. That is the right *behaviour* — a nonsense identity must not
  reach Docker — but an operator who deliberately set the variable gets no signal that it was ignored, on the
  one knob whose whole purpose is overriding a resolution that has already gone wrong once.

## LOW

- **L1 — `report/FallbackPropertiesRenderer.kt:118` is clever where it should be plain.**
  `appendLine("…$separator\\".removeSuffix(if (index == entries.lastIndex) "\\" else ""))` appends a
  continuation backslash and then removes it again for the last entry. Correct, but the reader has to simulate
  it; the same `index == entries.lastIndex` test is asked twice in one expression.

- **L2 — the charset branch added to `ReportServer.respond` cannot be reached.**
  `report/ReportServer.kt:141` picks ISO-8859-1 when the content-type says so, but
  `FallbackPropertiesRenderer.escape` maps every character outside 32..126 to `\uXXXX`, so the rendered
  document is pure ASCII and both encodings produce identical bytes. The *declared* charset in the header is
  load-bearing and must stay; the branching is a mechanism that can never do anything, in a helper every
  endpoint shares.

- **L3 — `report/FallbackPropertiesRenderer.kt:79,96` normalise the same collections twice**, once for the
  header's counts and once to render. Harmless at this size, but it means two sources of truth for "how many
  entries are we publishing".

- **L4 — `loader/InstallFailureDiagnosisTest.kt:60` uses `!!`** after an `assertNotNull`. Test code, but the
  convention says no new non-null assertions, and `assertNotNull` returns the narrowed value.

- **L5 — fully-qualified names where an import belongs.**
  `loader/InstallFailureDiagnosisTest.kt:66` writes `de.griefed.serverpackcreator.grinder.container.ContainerUser.ENV_KEY`
  inline, and `report/ReportServerTest.kt` constructs `java.util.Properties()` the same way.

## Verified clean — do not re-litigate

- **Pin-before-fix boundaries hold on all four units.** `29644191d`, `5ecf8cdc9`, `a43573563` and `47f991719`
  each land red on their own and are followed by the change; each red state was observed (compile failure on
  the missing unit) before committing.
- **`de20741de` is correctly labelled `fix:`,** and its `ContainerEngine.kt` edit is documentation of the
  parameter it changes the meaning of — within the commit's stated scope, not sprawl.
- **`8ec7440f5` does not guess.** `InstallFailureDiagnosis.of` returns `null` for a console it cannot explain,
  and that is pinned, so the raw tail stays the fallback rather than being replaced by a confident invention.
- **The confidence floor on `/as-properties` is pinned in both directions** — HIGH published,
  MEDIUM/LOW/INCONCLUSIVE and a null `suggestedEntry` excluded — and the document is verified by *parsing* it
  with `java.util.Properties`, which is what the consumer does, rather than by asserting on its shape.
- **No `-api` behaviour changed,** so `claude-docs/API-BEHAVIOUR-CHANGES.md` correctly gains no row. `-grinder`
  and `-clientside` are unpublished, so their signature changes carry no compatibility obligation.

---

# Audit iteration 18 — 2026-08-23 — second pass over the same branch, after iteration 17's fixes

Scope: `git log develop..HEAD`, now 24 commits. Iteration 17's ten findings are all closed; this pass
re-reads the branch as a whole rather than commit-by-commit, and pushes on the two things iteration 17 asserted
without executing.

Suites at the time of writing: grinder 275 (19 skipped), clientside 93. Both green, counts re-derived from
`build/test-results/test/*.xml`.

## Iteration 17 findings — closed

- **M1** split: `c14e750e0 refactor(grinder): read the container-user override in the entry point` carries the
  code, `31e696797 docs(grinder)` carries the documentation.
- **M2/M3** pinned: `navigationOptions()`/`downloadOptions()` extracted and asserted by *building* them
  (`723394f62`, `02d8a3916`), and `FallbackListWiringTest` asserts the endpoint's production wiring against
  `main`'s source. Teeth verified by breaking the join — it fails on "the published clientside list must come
  from SPC's own property".
- **M4/M5** fixed with red pins first (`93bbab2ed` → `3b4dc45a5`).
- **L1–L5** cleaned in `72dc9d2cd`, existing assertions untouched.

## What this pass added

- **The endpoint is now verified against its real consumer, not a model of it.**
  `FallbackPropertiesConsumerTest` (`c37…`, commit `test(grinder): drive SPC's real updater…`) points a real
  `UpdateConfig.updateFallback` at a running `ReportServer` over an ephemeral loopback port and asserts the
  entries land in `GenerationConfig.clientsideMods`. Everything else on this endpoint asserts against
  `java.util.Properties`, which is my model of the consumer; this is the consumer. It needs neither Docker nor
  internet, so it is a plain test rather than a gated IT. **Teeth verified:** removing the continuation
  backslash collapses the whole list to `[, entityculling-]` and both cases go red.

## MEDIUM

- **P2-M1 — the published base list is only as fresh as this daemon's own SPC, and that was undocumented.**
  `UpdateConfig` *replaces* a client's lists with whatever it is served, so a grinder running an old build — or
  one that could not reach the repository at its own startup — hands every client a **staler** list than they
  had. The endpoint is a mechanism for distributing this daemon's opinion, and that opinion has an age. Fixed
  in this pass: README §5 and the module landmine now state it.

- **P2-M2 — the container-user fix is still unverified against a real daemon, and this host cannot verify it.**
  Attempted, with `docker:29.7.2` and a *named volume* rather than a bind mount, specifically so the
  permissions would be real Linux ones inside the VM. The result is inconclusive for an instructive reason:
  with the volume root chowned to `1001:1001`, a container run as `--user 0:0` reads it back as `1001:1001`,
  while a container run as `--user 1001:1001` reads the same directory as `0:0` and cannot write. Root and
  non-root containers disagree about the same inode, which is Docker Desktop's own id remapping, not kernel
  DAC — so **neither the bug nor the fix reproduces faithfully here**, and the run proves nothing either way.

  The bug itself is not in doubt: the production console on 2026-08-23 shows three `Permission denied` lines
  against the mounted pack, and the fix is the standard remedy. But the convention is explicit that a real
  runtime answers this class of question, so it stays open until run on the Linux host:

  ```
  sudo -u grinder mkdir -p /tmp/spc-uid-check
  docker run --rm -v /tmp/spc-uid-check:/pack -w /pack --user "$(id -u grinder):$(id -g grinder)" \
      spc-grinder-runtime:latest bash -c 'touch user_jvm_args.txt && echo WRITABLE'
  ```

  Expected: `WRITABLE`. The same command with `--user 1000:1000` should fail wherever `id -u grinder` is not
  1000 — that pair is the actual proof, since it shows the two identities behaving differently on one directory.

## LOW

- **P2-L1 — `@JvmStatic` on `BrowserDownloader.isDownloadAbort`** for a helper with no Java callers. Removed.
- **P2-L2 — the root `CLAUDE.md` counts were stale again** (268/90, written before iteration 17's own tests
  landed). A count in prose goes stale by being *correct at the time*, which is the failure mode the
  "cite names, not snapshots" convention exists for; the column already says how to re-derive it, and the
  numbers were re-derived rather than adjusted by hand.

## Verified clean — do not re-litigate

- **`ContainerUser` resolves after `workDir` is created** (`GrinderApplication.kt`: `workDir` is
  `.apply { mkdirs() }` at declaration, the resolution follows the `workers` read), so `ownerOf` never reads a
  path that does not exist and never silently falls back to the image default for that reason.
- **Running as an id with no matching entry in the image's `/etc/passwd` is not a new risk.** The rootfs is
  read-only and only `/tmp` is a tmpfs, so nothing could write to a home directory under the old uid either;
  the JDKs under `/opt` are world-readable.
- **The comma filter applies to both lists and to grinder findings**, not just the shipped list — checked
  against `normalise` being the single funnel every published entry passes through.
- **No endpoint other than `/as-properties` changed behaviour.** `respond` now always encodes UTF-8, which is
  what it did before this branch; the ISO-8859-1 branch existed only within this branch's own history.

---

# Audit iteration 19 — 2026-08-23 — third pass, and the equivalence check

Scope: `git log develop..HEAD`, 27 commits. This pass stops re-reading commit boundaries — iterations 17 and 18
covered those — and does the two things that had not been done: run the base branch's tests against this
branch's code, and read the finished units rather than their diffs.

## Equivalence against the base — clean

`develop`'s unmodified test tree, checked out over this branch's production code in a detached worktree:

```
git worktree add --detach <tmp> HEAD
cd <tmp> && rm -rf serverpackcreator-{grinder,clientside}/src/test
git checkout develop -- serverpackcreator-{grinder,clientside}/src/test
./gradlew :serverpackcreator-grinder:test :serverpackcreator-clientside:test --continue
```

**339 pre-existing guards (grinder 251 with 19 skipped, clientside 88), zero failures, zero compile errors.**
No file needed adapting, which is itself the finding: every signature this branch changed gained a *defaulted*
parameter (`ContainerServerRunner`, `DockerLoaderInstaller`, `ContainerCandidateVerifier`, `ReportServer`,
`BrowserDownloader`), and the one signature that lost a default — `ContainerUser.forDirectory` — is new on this
branch and has no base-tree callers. Nothing existing changed shape.

## LOW

- **P3-L1 — `unrepresentable()` counted duplicates while `normalise()` de-duplicated.**
  `report/FallbackPropertiesRenderer.kt` — one mod dropped on three loaders was reported in the document as
  three omissions, sending a reader hunting two entries that never existed. Two functions filtering the same
  collection by the same predicate should agree on what "an entry" is. Fixed.
- **P3-L2 — the DOMCONTENTLOADED rationale existed twice**, inline at the first `page.navigate` and in
  `navigationOptions()`' KDoc, after the extraction moved the decision. Two copies of a reason is one copy that
  goes stale. Inline copy removed. Fixed.
- **P3-L3 — the root `CLAUDE.md` counts were stale for the third time in one session** (268/90 → 276/93). Not
  a new defect each time, but worth stating as a pattern: any count written before the last test lands is
  wrong by the time it is committed, and this session generated three chances to get it wrong. Re-derived from
  the run that produced them.

## Considered and deliberately not changed

- **`/as-properties` re-renders on every request** rather than caching. The full list is a few hundred KB and
  the consumer polls at *startup*, so a cache would add invalidation to save nothing measurable. Recorded so
  the next reader does not re-derive it — and so that if polling ever becomes frequent, the decision is known
  to have been made under the startup-only assumption.
- **Running as a uid with no `/etc/passwd` entry in the image.** `$HOME` is unset for such a uid, but the
  rootfs is read-only with only `/tmp` writable, so nothing could write to a home directory under the old uid
  either. No regression; see iteration 18's clean list.
- **`InstallFailureDiagnosis` recognises exactly one cause.** Adding speculative patterns would restore the
  problem it was written to fix — a confident diagnosis pointing at the wrong subsystem. It returns `null` and
  falls back to the raw tail for anything else, and that is pinned.

## Still open

- **P2-M2 — the container-user fix has no real-runtime verification** and cannot get one on this workstation
  (see iteration 18 for why Docker Desktop's id remapping makes the local run prove nothing). The two-command
  check for the Linux host is recorded there. This is the one claim on the branch resting on reasoning plus
  production logs rather than on an executed check, and it should be closed on Yggdrasil before the fix is
  trusted in the release notes.

---

# Audit iteration 20 — 2026-08-23 — the graceful-shutdown branch

Scope: the three commits merged as `c22e54a40` — `312745b33` (pins), `25541a8d8` (implementation),
`067ebc31f` (documentation). Already on `develop`, so the fixes land as a follow-on branch rather than by
rewriting merged history.

Suites: grinder 282 (22 skipped), plus 6/6 of the Docker-gated `DockerJavaContainerEngineIT` against
docker 29.7.2.

## HIGH

- **H1 — the branch's central guarantee has a reachable window in which it silently does not hold.**
  `Grinder.kt`, `GrindPool.grindAll`: the worker threads are **started inside the `map`** and the field the
  shutdown path reads is assigned only afterwards.

  ```kotlin
  val running = (1..workerCount).map {
      Thread { … }.apply { name = "grind-worker-$it"; start() }   // running
  }
  workers = running                                              // …only now visible to awaitStop
  ```

  A SIGTERM arriving between the first `start()` and that assignment finds `workers == emptyList()`. `awaitStop`
  then interrupts nobody, joins nothing, and — because `emptyList().none { it.isAlive }` is `true` — **reports a
  clean stop**. The hook logs no warning, the JVM exits, and workers are still running. It is the exact failure
  the branch was written to prevent, wearing a success message.

  The window is small (thread construction for N workers) but it is entered on *every* pass, and a daemon that
  restarts on a schedule enters it often. Publish the list before starting the threads.

  Severity mapped deliberately: the rubric's HIGH covers behaviour-change-inside-a-refactor, broken boundaries
  and plugin-API contracts, none of which this is. Calling it MEDIUM would understate a defect that makes the
  feature's promise conditional on a race.

## MEDIUM

- **M2 — nothing pins the shutdown hook's wiring**, which is the same gap `FallbackListWiringTest` was written
  to close two audits ago, left unapplied to a hook that cannot be executed (it builds an `ApiWrapper` and a
  Docker client). Nothing asserts that `main` calls `awaitStop` at all, that it calls `reapOrphans` at startup,
  or that `engine.close()` precedes `awaitStop` — and the ordering is load-bearing: `close()` is what sets the
  closed flag, so reversing the two re-opens the create-behind-the-sweep hole this branch closed.

- **M3 — the 15-second window is unpinned.** `SHUTDOWN_GRACE` is stated in the unit, in the README and in the
  operator contract, and no test fails if someone changes it. It is a number an operator was promised.

- **M4 — `TimeoutStopSec` and `SHUTDOWN_GRACE` are coupled with nothing enforcing it.** The unit's own comment
  says lowering the timeout below the window "is the one change that actively causes the leak", and
  `SystemdUnitConfigurationTest` checks environment knobs only. The relationship is arithmetic and therefore
  checkable: the stop timeout must exceed `ceil(workers / 8) × grace` with margin.

- **M5 — the grace window is a hard-coded top-level `val` the engine reads directly**, so no test can vary it.
  Two consequences: the value itself is untestable (M3), and one IT case burns 15.6 s of real wall-clock
  waiting out a window it cannot shorten. A constructor parameter defaulting to the constant fixes both
  without changing production behaviour.

## LOW

- **L1 — `DockerJavaContainerEngineIT.waitForContainer(engine)` ignores its parameter**; it polls the daemon
  globally by label. The signature claims a scoping that does not exist.
- **L2 — `reapsALabelledOrphanLeftByAPreviousProcess` leaves its `orphanEngine` open** and its worker thread
  running against a container the reap has removed. Harmless in a gated IT, but it is the one test in the file
  that does not clean up after itself.
- **L3 — every shutdown with a boot in flight now logs a spurious WARN.** `close()` removes the container,
  then `run()`'s own `finally` tries again and the 404 surfaces as
  `Could not remove container <id>: …`. Expected, harmless, and indistinguishable in the journal from a
  removal that genuinely failed — which is precisely the kind of noise that made the 2026-08-23 install
  diagnosis take three rounds.

## Verified clean — do not re-litigate

- **The pins landed red and separately** (`312745b33` before `25541a8d8`), and the container half was verified
  against a live daemon rather than reasoned about — including a trapped SIGTERM proving the signal arrives
  before removal.
- **`close()` sets `closed` before it sweeps, and `run()` re-checks after adding to the tracking set.** Either
  the sweep sees the container or the creator sees the flag; there is no third outcome.
- **Concurrency in `close()` is bounded** at 8 and the pool is shut down in a `finally`.
- **`reapOrphans` runs before the staging reaper and after the engine exists**, and at that point this process
  owns no containers, so everything wearing the label is by definition inherited.

---

# Audit iteration 21 — 2026-08-23 — second pass over the shutdown work

Scope: the same three merged commits plus iteration 20's two fix commits. Grinder suite 288 (22 skipped),
Docker-gated IT 6/6 against docker 29.7.2.

## Iteration 20 findings — closed

H1 fixed (workers published before they start) and its invariant pinned; M2/M3/M4 pinned by
`ShutdownWiringTest`, **teeth verified on all three** by breaking each in turn; M5 injected; L1–L3 cleaned.
Recorded honestly in the test's own doc: H1's guards were never observed red, because the interleaving could
not be provoked at 8 or 64 workers.

## HIGH

- **P21-H1 — the one-shot run's workers are never signalled.** `GrinderApplication.kt:190`:

  ```kotlin
  GrindPool(grinder, workers).grindAll(candidates)   // one-shot: no crawl cursor to advance
  ```

  The pool is constructed inline and **never stored in `activePool`**, which is the only handle the shutdown
  hook has. So on Ctrl-C during a one-shot run the hook calls `requestStop()` and `awaitStop()` on `null`,
  both silently no-op via `?.`, and the workers are neither signalled nor waited for. The engine still closes,
  so containers are stopped and the boots collapse — which is why this looks like it works — but the worker
  half of the contract does not run at all, and the `false`-means-warn branch cannot fire either.

  The hook's own comment claims otherwise: *"registered before any boot can start so it covers the one-shot
  path too"*. That was true of the hook and stopped being true of what the hook can reach. One-shot is the
  end-to-end verification path, and Ctrl-C is how it is always ended.

## MEDIUM

- **P21-M1 — the workers get a second full window, not the remainder of the first.** The hook passes
  `awaitStop(SHUTDOWN_GRACE)` *after* `engine.close()` may already have spent the entire 15 s, so the real
  worst case is **30 s**, not 15. Three places say otherwise: the hook's log line ("15s for containers and
  workers to quit"), the comment directly above the call ("whatever is left of the window"), and README §5
  step 4 ("gives the workers what is left of that window"). The requested contract was one shared window, the
  documentation describes one shared window, and the code implements two sequential ones.

  It also quietly undercuts `ShutdownWiringTest.theUnitAllowsEnoughTimeForTheCleanupItDependsOn`, whose
  arithmetic (`ceil(workers / 8) * grace`) assumes the worker wait overlaps the container wait rather than
  following it.

## LOW

- **P21-L1 — `activePool.get()` is read twice in the hook**, once for `requestStop` and once for `awaitStop`,
  so the two calls can in principle land on different pools: `main` sets a new one per pass, and the hook runs
  concurrently with it. Capture it once.
- **P21-L2 — `SHUTDOWN_GRACE` lives in the `container` package but now governs workers too.** Its KDoc says
  so, and moving it would churn imports for little gain, but the home is no longer quite right — noted so the
  next reader does not assume the worker timeout is a container concern.

## Verified clean — do not re-litigate

- **The hook's ordering is correct and now guarded**: `close()` (which sets the closed flag) strictly precedes
  `awaitStop`, verified red by swapping them.
- **`requestStop()` before `close()` is deliberate**, not redundant with `awaitStop`'s own flag set: it stops a
  worker that finishes during the sweep from picking up another candidate.
- **A null `activePool` is handled correctly** for the *continuous* path — it is null only between passes,
  when there are no workers to signal. P21-H1 is about the one-shot path never setting it at all.

---

# Audit iteration 22 — 2026-08-23 — third pass, and the equivalence check

Scope: the shutdown work plus iterations 20 and 21's fixes. Grinder suite 288 (22 skipped). Docker-gated IT
6/6 on docker 29.7.2.

## Equivalence against the base — clean

The pre-shutdown test tree (`develop~1`) checked out over the current production code in a detached worktree:
**276 pre-existing guards, zero failures, zero compile errors, nothing needing adaptation.** Every signature
this work changed either is new (`awaitStop`, `reapOrphans`, `trackedWorkerCount`) or gained a defaulted
parameter (`DockerJavaContainerEngine(shutdownGrace = …)`).

## MEDIUM

- **P22-M1 — the "one 15-second window" is only true up to eight in-flight containers.**
  `DockerJavaContainerEngine.MAX_PARALLEL_STOPS = 8`, so with more containers than that the stops run in
  batches and the container phase alone costs `ceil(n / 8) × 15 s`. At `SPC_GRINDER_WORKERS=10` — the value
  actually deployed — that is **30 s before the workers get anything**, and iteration 21's deadline then hands
  `awaitStop` zero milliseconds.

  So iteration 21 fixed the *sequencing* of the two windows and left the multiplication in place. The unit's
  comment and `ShutdownWiringTest` both already encode `ceil(workers / 8)`, which means the arithmetic is
  honest — but it is honest about a number that did not need to be larger than one in the first place. The cap
  was chosen defensively ("so a large worker count cannot flood the daemon"); a `docker stop` is an HTTP call
  that spends its time waiting, and the realistic ceiling on concurrent boots is memory-bound at ~20. Raising
  the cap well above any real worker count makes the promised single window true, and collapses the arithmetic
  in three documents to `1 × grace`.

- **P22-M2 — a container that burns the whole window leaves the workers exactly zero.**
  With `remaining` clamped at 0, `awaitStop` interrupts and then joins nothing, so the "did not stop within
  15s" warning is *guaranteed* rather than informative — the workers were never given a chance to observe the
  interrupt they were just sent. A small floor (a second) makes the warning mean what it says, at a worst case
  of 16 s against a 60 s stop timeout.

## LOW

- **P22-L1 — `theWorkersGetTheRemainderOfTheWindowRatherThanASecondOne` asserts an absence.**
  `!body.contains("awaitStop(SHUTDOWN_GRACE)")` passes for any spelling that is not that exact string, so a
  future rewrite that reintroduces the second window under a different name slips through. A positive
  assertion — that a deadline is computed and its remainder passed — is what the guard means.
- **P22-L2 — the one-shot path never clears `activePool`.** Harmless (a finished pool tracks no workers, so
  `awaitStop` returns immediately) and noted only so it is not read as an oversight later.

## Verified clean — do not re-litigate

- **Iterations 20 and 21's guards all have verified teeth**, each broken in turn: `TimeoutStopSec=20`, the
  removed reap call, the swapped `close`/`awaitStop` ordering, the unregistered one-shot pool, and the
  full-window `awaitStop`. The one exception is stated in its own test doc — H1's invariant guards were never
  observed red because the interleaving could not be provoked.
- **The `activePool.set(` counting mistake is fixed and worth remembering**: the pass loop clears the
  reference with `activePool.set(null)`, so counting occurrences made an unregistered pool pass. Caught only
  because an expected red did not arrive.
- **The three documents now agree with the code** on ordering and on the shared window (subject to P22-M1's
  batching), and each is guarded rather than merely written.

---

# Audit — 2026-08-23, `claude-grinder-cpu-limit` (iteration 23)

Scope: `git log develop..HEAD` — four commits (`test(grinder): pin the per-container CPU cap and its wiring`,
`feat(grinder): express the container CPU cap in cores, against a stated period` = `ContainerResources.forCpus`
+ `cpuPeriod`, `feat(grinder): make the per-container CPU cap configurable` = `main` wiring + operator docs +
the gated IT, `docs: record the CPU-cap knob and the quota-without-period finding` = context files and the
log). Read-only pass; every number below was produced by a command, not recalled.

## HIGH

**H1 — a positive CPU cap can silently become *no* cap.**
`serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/container/ContainerEngine.kt:93`
(the `forCpus` commit). `forCpus` uses the *computed* quota as its "uncapped" sentinel:

```kotlin
val requested = Math.round(cpus * base.cpuPeriod)
cpuQuota = if (requested == 0L) 0L else maxOf(MINIMUM_QUOTA_MICROSECONDS, requested)
```

`requested` is 0 for any `cpus < 5e-6`, so the branch cannot distinguish "the operator asked for uncapped"
from "the operator's value rounded away to nothing" — and quota `0` is *no limit*, verified against the
daemon: `docker run --cpu-quota=0 --cpu-period=100000 busybox cat /sys/fs/cgroup/cpu.max` → `max 100000`.
The KDoc two lines above promises the opposite ("anything positive but smaller than the daemon's floor is
raised to it"), and the direction of the failure is the hardening-off one: a request for the smallest
possible cap yields none at all. The trigger needs an absurd value, but this is a security-posture knob and
the guarantee is written down, so it is graded on the failure, not the likelihood. `ContainerResourcesTest`
misses it because its floor case (`forCpus(0.0001)`) is two orders of magnitude above the boundary.
Fix: branch on the *input* (`cpus == 0.0`), and reject non-finite input while there — `"Infinity".toDouble()`
parses, and `Math.round(Double.POSITIVE_INFINITY * 100_000)` is `Long.MAX_VALUE`.

## MEDIUM

**M1 — the new README section was inserted into the middle of the previous one.**
`serverpackcreator-grinder/README.md`'s `### Capping CPU` (added by the configurable-cap commit) landed before the **Keep the host
awake** paragraph, which is about suspends and `caffeinate` and belongs to *Sizing the worker count* — it now
reads as the closing advice of the CPU section. Same commit also leaves §5's sizing opener ("the worker count
is a memory question rather than a CPU one") without the pointer it now needs. Boy-Scout scope was respected;
the placement is simply wrong.

**M2 — the startup line reports the derived numbers, not the knob.**
`GrinderApplication`'s startup line (from the configurable-cap commit) logs `cpuQuota=200000/100000`. Every other knob is logged in the
operator's own unit (`workers=2`, `port=8757`, `containerUser=…`), and this is the one whose whole point is
that the operator thinks in cores. Worse at the documented escape hatch: `SPC_GRINDER_CPUS=0` prints
`cpuQuota=0/100000`, which reads as "zero CPU" when it means "uncapped" — the value an operator is most
likely to double-check in the log is the one the log states most misleadingly.

**M3 — a guard shipped in the same commit as the code it guards.**
The configurable-cap commit adds `DockerJavaContainerEngineIT.theCpuCapReachesTheKernelWithItsPeriod` alongside the wiring,
and the behaviour that guard actually pins — `withCpuPeriod` in `hostConfigFor` — landed one commit earlier
in the `forCpus` commit, bundled with the new API. Nobody can check out a commit and watch that pin go red. This is
the exact boundary CLAUDE.md's "Pin first means *commit* first" entry was written about after the 2026-07-31
audit found eight commits doing it. Mitigating evidence, recorded because it is real: the teeth *were*
checked in-session by removing `.withCpuPeriod` and re-running, which produced `saw: [75000 100000]` for a
requested 1.5 cores at a 50 ms period. Unlike the 2026-07-31 case the branch is **local and unpushed**, so
the honest remedy is available: split the history rather than write an apology into a convention file.

**M4 — `deploy/install-grinder.sh:313` still says "Three worth a decision rather than a default".**
It names `SPC_GRINDER_WORKERS` as the throughput lever and never mentions its CPU twin, so the installer —
the operator's first surface, and the one no guard test covers — is now the only place the knob is invisible.
`SystemdUnitConfigurationTest`/`ReadmeConfigurationTest` cannot catch this; the script is not scanned by
either.

## LOW

**L1 — `Math.round(...)`** at `ContainerEngine.kt:92` is a Java-ism where `kotlin.math.roundToLong()` is the
idiom ("Don't port Java patterns 1:1").

**L2 — `ContainerResourcesTest.theDefaultIsUnchangedByTheKnobExisting`** is not a sentence; the assertion it
makes (default ≡ `forCpus(2.0)`) deserves a name that says so.

**L3 — the docs imply docker's `--cpus` validation applies, and it does not.** `forCpus`'s KDoc and README
§5 both say "the same arithmetic as docker's own `--cpus`", which is true of the arithmetic and false of the
guard rails: the CLI's `--cpus` is bounded by the host's CPU count, while the raw cfs path we use is not.
Measured on a 16-core host: `docker run --cpu-quota=100000000 --cpu-period=100000` (1000 cores) is accepted
and the container's cgroup reports `100000000 100000`. Consequence for the fix list: an over-large value
needs **no** clamp — it silently means "effectively uncapped", which is worth one sentence rather than code.

**L4 — `CpuLimitWiringTest.construction()`** requires the call's closing paren on its own line
(`"""$type\((.*?)\n\s*\)"""`). Reformatting a call to one line makes the guard fail with "main() no longer
constructs a …" rather than pass silently, so the failure mode is loud and acceptable; noted so the next
reader does not mistake it for a real regression.

## Verified clean — do not re-litigate

- **No positional `ContainerResources(...)` callers exist**, so inserting `cpuPeriod` as the third parameter
  cannot have silently rebound anyone's `pidsLimit`. Checked on both trees: `git grep "ContainerResources("`
  over `develop` and HEAD finds only `ContainerResources()`, named-argument, and default-value uses.
- **The daemon's 1 ms floor is real and the quoted error is verbatim.** `docker run --cpu-quota=500
  --cpu-period=100000 busybox true` → `Error response from daemon: CPU cfs quota can not be less than 1ms
  (i.e. 1000)`. `MINIMUM_QUOTA_MICROSECONDS` and its comment are accurate.
- **The `withCpuPeriod` fix is verified in the kernel, not in the request.**
  `theCpuCapReachesTheKernelWithItsPeriod` reads `/sys/fs/cgroup/cpu.max` from inside the container and uses
  a non-default 50 ms period so the assertion cannot pass with the period unsent — confirmed by removing the
  production line and watching it fail. Docker 29.7.2, full gated IT 7/7.
- **`0` really is uncapped end to end**, in the daemon (`max 100000`, above) and through our own path.
- **`test(grinder): pin the per-container CPU cap and its wiring` is a legitimate red-first test commit**; its red is a compile error naming the missing
  API, which is the only form available for a not-yet-existing symbol.
- **The `ContainerResources` KDoc reshape** (one parameter per line was already the shape) added `cpuPeriod`
  with docs and left names, types, order and defaults of the existing parameters untouched.

## Equivalence against the base — clean

`develop`'s unmodified test tree against the branch's production code, per CLAUDE.md's recipe:

```
git worktree add --detach <tmp> HEAD
cd <tmp> && rm -rf serverpackcreator-grinder/src/test && git checkout develop -- serverpackcreator-grinder/src/test
./gradlew :serverpackcreator-grinder:test --continue
```

**290 pre-existing guards, 0 failures, 22 skipped, zero compile errors** — no signature changed, so nothing
had to be adapted. Branch's own suite: 298 / 0 / 22.

## Resolution — 2026-08-23, same session

| Finding | Status | Where |
|---|---|---|
| H1 positive cap → uncapped | **fixed** | guard `test(grinder): pin that a positive CPU cap stays a cap` (red: `expected: <1000> but was: <0>`), fix `fix(grinder): decide "uncapped" from the request, not from the arithmetic` |
| M1 README section split | **fixed** | `fix(grinder): report the CPU cap the way the operator set it` |
| M2 startup line in µs | **fixed** | guard + `cpuCapDescription()`, same commit; teeth checked by restoring the old line |
| M3 guard bundled with its code | **accepted, not rewritten** — see below |
| M4 installer's "three worth a decision" | **fixed** | same commit as M1 |
| L1 `Math.round` | **fixed** | `roundToLong()` |
| L2 test name | **fixed** | `theShippedDefaultIsExactlyTwoCores` |
| L3 `--cpus` validation implied | **fixed** | KDoc now states the raw cfs path is *not* host-bounded, with the measurement |
| L4 brittle-but-loud wiring regex | **no change** | fails loudly rather than passing silently; the new startup-line matcher documents the lazy-match trap it hit |

**M3 is deliberately not remedied by rewriting history, and that is a judgement call worth stating.** The
convention's purpose is that someone can check out a parent and watch the pin go red. Buying that here costs
a rebase of the whole branch, and the rebase would invalidate every commit hash this very report cites —
precisely the failure mode CLAUDE.md's "cite names, not snapshots" entry exists for (54 hashes killed by a
rebase). What is bought is thin: the guard in question is `theCpuCapReachesTheKernelWithItsPeriod`, a gated
IT that no CI run will ever execute, whose teeth were checked in-session by removing the production line
(`saw: [75000 100000]`) and whose red is recorded in three places. The two subsequent fixes on this branch
were landed guard-first in their own commits, so the discipline is demonstrated where it is cheap. If a
future reader disagrees, the remedy is a rewrite *before* the merge; after it, it is unfixable.

**Re-verified after the fixes:** branch suite **303 tests, 0 failures** (16 skipped with `GRINDER_DOCKER_IT=1`,
23 without — the gated Docker IT grew a case). Gated `DockerJavaContainerEngineIT` 7/7 against Docker 29.7.2.

---

# Audit — 2026-08-23, `claude-clientside-recheck-diversity` (iteration 24)

Scope: `git log develop..HEAD` — eight commits, six of code in strict test-then-fix pairs
(`bcc802174`/`14c8539a0` Modrinth primary files, `1f4dea431`/`39d340340` diversified crash re-check,
`638cbf0a0`/`82c0c9c39` per-attempt staging identity) and two of documentation (`7a7d9e952`,
`5c4fcfa31`). Read-only pass; every number below was produced by a command, and the HIGH was
reproduced by executing the two functions involved rather than by reading them.

## HIGH

**H1 — a clean boot under one loader is published as another loader's `bootResult`, and then disproves
a *third* loader's crash on evidence that does not support it.**
`serverpackcreator-clientside/src/main/kotlin/de/griefed/serverpackcreator/clientside/BootVerifier.kt`
(`reconcileOtherVersionRecheck`, survivor branch) together with
`ClientsideVerifier.loaderDisprovingTheCrash` / `supersededByLoader` — introduced by `39d340340`.

Before this branch, every attempt folded into a verdict was a boot of *that verdict's own loader*, so
`survivor.outcome.copy(...)` returning the survivor's `result` was sound. `39d340340` made the re-check
sample span loaders without changing that fold, so the survivor may now be a different loader's boot.
Two consequences, both reproduced by calling the real functions:

```
PROBE1 result=SURVIVED
        reconcileOtherVersionRecheck(NeoForge CRASHED,
            [OtherVersionAttempt("sodium-fabric-0.5.jar (Fabric, Minecraft 1.21.11)", SURVIVED)])
PROBE2 disproving=NeoForge
PROBE2 note=Forge 48.1.0 / MC 1.20.2 -> CRASHED (exit 1) Crashed, but NeoForge booted a server with
        the same entry 'embeddium-' — the crash belongs to that build, not to the mod's sideness.
```

1. **The report states something untrue.** NeoForge never booted a server; Fabric did. The module's own
   rule for this note is that it is *rebuilt* rather than appended to, "because bolting a correction onto
   a false sentence is how prose goes stale" — this produces the false sentence directly.
   `ClientsideReportRenderer.kt:66` renders `verdict.bootResult` as that loader's row, so the per-loader
   table shows `SURVIVED` for a loader that crashed.
2. **`loaderDisprovingTheCrash`'s stated invariant is no longer satisfied.** It exists so a published
   entry cannot strip a build proven to boot, and it tests that by comparing the two verdicts' *entries*.
   With a cross-loader survivor the build that actually booted belongs to a third loader whose entry may
   differ — `sodium-fabric-0.5.jar` does not start with `embeddium-`, the very Fabric/NeoForge naming
   split `FilenameStemDeriver.deriveStems` documents. The guard fires; nothing it protects is at risk.

Direction of harm is a false *negative* (a crash cleared that should have stood), which is the
conservative side for the fallback list — but the branch's whole subject is the honesty of this evidence
chain, and it currently publishes a sentence it cannot support. Severity HIGH because a behaviour change
in `39d340340` silently altered the meaning of an input an *existing* guard depends on.

Suggested remedy, in the branch's own idiom: record on `BootOutcome` which loader produced the decisive
boot, carry it to `LoaderVerdict`, and require in `loaderDisprovingTheCrash` that a disproving verdict's
decisive boot was its **own** loader's. That keeps the guard's invariant exactly as documented, keeps the
note true, and leaves the within-loader and cross-loader re-checks untouched.

## MEDIUM

**M1 — the reaper's scope now rests on an agreement the code elsewhere warns can fail.**
`serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/ContainerCandidateVerifier.kt`
(`reaper.reap(candidate.platform, candidate.slug)`, `82c0c9c39`). Staging is named from the *resolved
report's* `ProjectFiles.platform`/`slug`; reaping is keyed on the *candidate's*. `Grinder.kt:82-88`
explicitly logs `"Platform mismatch for …: candidate says 'X', resolved report says 'Y'"`, i.e. the
codebase already knows the two can disagree. On disagreement the reap matches nothing and that
candidate's staging survives until the next startup `reapAll()` — bounded, but it re-opens the disk-growth
class `BootWorkspaceReaper` exists for (98 GB / 1750 directories, 2026-07-30).

Not newly invented: `reap(candidate.slug)` versus a directory named from `project.slug` had the same
coupling before this branch. The change *extends* it from one field to two, which is the moment to close
it rather than inherit it. Remedy: reap on the resolved report's identity when the verification produced
one, falling back to the candidate's when it threw.

**M2 — a landmine claim in the module context file is false, and the branch edits the line it sits on.**
`serverpackcreator-clientside/CLAUDE.md:266` — *"`MetadataScannerTest` is the only one needing a
resource"*. Measured: on `develop` three test classes already build an `ApiWrapper`
(`MetadataScannerTest`, `BootVerifierSelectionTest`, `LoaderVersionResolverTest`); this branch adds
`AttemptStagingIsolationTest`, making four. The claim was stale before the branch, but `7a7d9e952`
rewrites the test count on that same bullet, so the Boy-Scout rule puts it in scope — and this project
treats stale prose as its own defect class ("cite names, not snapshots").

## LOW

**L1 — a log line that no longer describes what it does.**
`BootVerifier.kt:226` (`39d340340`): `"re-checking ${candidates.size} other version(s) before trusting
the crash"`. The sample now spans loaders, so "version" is only half of what is being tried; the
per-attempt labels were updated for exactly this reason and this line was not.

**L2 — a fixture that no longer illustrates the shape it stands for.**
`BootVerifierCrashRecheckTest`'s synthetic labels keep the pre-change
`"ironchest-1.20.1.jar (Minecraft 1.20.1)"` form while production now emits
`"… (Forge, Minecraft 1.20.1)"`. Harmless — they are opaque strings to the function under test — but the
file is where a reader goes to learn what a label looks like.

## Verified clean — do not re-litigate

- **Commit hygiene is exact.** Every one of the six code commits touches a single source set:
  `bcc802174`, `1f4dea431`, `638cbf0a0` → `src/test` only; `14c8539a0`, `39d340340`, `82c0c9c39` →
  `src/main` only. No commit mixes a test with the change it pins, which is the boundary CLAUDE.md
  records eight commits collapsing on 2026-07-31.
- **Both test commits were verified red before their fix**, and the two signature-change commits are red
  as compile failures — the honest shape for a signature change, stated as such in their messages.
- **Teeth checked on all five new behavioural pins**, each by restoring the pre-fix line and observing the
  named test fail: `reapingOnePlatformLeavesTheSameSlugOnAnotherPlatformAlone`,
  `theJarScanOfTwoPlatformsSharingASlugDownloadsIntoSeparateDirectories`,
  `theSameSlugOnTwoPlatformsStagesIntoSeparateDirectories`, `nonPrimaryFilesAreNotModFiles`,
  `aSourceJarDoesNotPoisonTheDerivedListEntry`.
- **No new compiler warnings** in either touched module (`compileKotlin` + `compileTestKotlin`,
  `--rerun-tasks`, filtered to `clientside`/`grinder`: none).
- **Module boundaries intact.** `AttemptDirectory` lives in `-clientside` and is consumed by `-grinder`,
  which already depends on it. Nothing points inward; no Spring, Swing or frontend reach was added.
- **No plugin-API contract touched.** Every changed signature (`pickRecheckCandidates`,
  `BootWorkspaceReaper.reap`, `stageBootPack`, `OtherVersionAttempt.label` semantics) is in
  `-clientside` or `-grinder`, neither of which is published to Maven Central. `-api` is untouched by the
  branch.
- **`14c8539a0` is a genuine fix, not a filter dressed as one.** The `primaries.ifEmpty { files }`
  fallback is load-bearing: 3 of `creativecore`'s 300 live versions flag no primary, and dropping them
  would lose real builds. Verified against the live API, not assumed.

## Equivalence against the base — clean

`develop`'s unmodified test tree run against this branch's production code, per CLAUDE.md's procedure:

```
git worktree add --detach /tmp/spc-base HEAD
rm -rf <clientside|grinder>/src/test && git checkout develop -- <both>/src/test
./gradlew :serverpackcreator-clientside:test :serverpackcreator-grinder:test --continue
```

**415 pre-existing guards, 0 failures** (clientside 114, grinder 301), with exactly **two** files
uncompilable, both enumerated rather than worked around:

| File | Why it cannot compile | Deliberate? |
|---|---|---|
| `BootCandidateSelectorTest` | `pickRecheckCandidates` gained a loader per candidate and gates on `(loader, mc)` | yes — re-pinned by `1f4dea431` |
| `BootWorkspaceReaperTest` | `reap` gained the platform half of its scope | yes — re-pinned by `638cbf0a0` |

Both are the branch's two intended contract changes, and both are the subject of a red test commit. No
*other* guard on `develop` changed meaning: the remaining 415 were run unmodified and passed.

## Resolution — 2026-08-23, same session

All five findings fixed, each pinned red first in its own `test(...)` commit.

| Finding | Outcome |
|---|---|
| H1 cross-loader survivor disproving a third loader's crash | **fixed** — `BootOutcome.bootedLoader` stamped by `runPrepared`, carried to `LoaderVerdict.bootedLoader`; `loaderDisprovingTheCrash` now requires `other.bootedLoader == other.loader`; the Markdown Boot cell reads `SURVIVED (via NeoForge)` when they differ |
| M1 reap keyed on the candidate's identity | **fixed** — `ContainerCandidateVerifier.reapTarget` prefers the resolved report's `(platform, slug)` and falls back to the candidate only when the verification threw |
| M2 stale "only one test needs a resource" | **fixed** — names all four and states the `grep` that re-derives them, rather than leaving another number to trust |
| L1 "other version(s)" log line | **fixed** — says "combination(s)" and lists each `<loader> / Minecraft <version>` |
| L2 fixture labels in the pre-change format | **fixed** — labels carry their loader; every assertion byte-identical |

**H1's remedy was chosen over two cheaper ones and it is worth saying why.** Making
`reconcileOtherVersionRecheck` return the *crashing* loader's outcome with a patched result would have kept
`bootResult` honest per row but thrown away which build actually booted, so the report could no longer say
what cleared the crash. Suppressing cross-loader survivors from the fold entirely would have undone the fix
the branch exists for. Recording the loader costs one nullable field and makes both the guard and the
rendered table state exactly what happened.

**Teeth checked on both fixes**: restoring `loaderDisprovingTheCrash`'s two-condition form fails
`aSurvivalBorrowedFromAnotherLoaderDisprovesNothing` while `theSameSurvivalOnItsOwnLoaderStillDisproves`
stays green (so the guard narrowed rather than closed); `reapTarget` returning the candidate unconditionally
fails `theResolvedReportsIdentityIsWhatGetsReaped`.

**Re-verified after the fixes and the two features that followed:** clientside **136 tests, 0 failures**;
grinder **325 tests, 0 failures** (23 skipped — the gated Docker IT and the two bind-address guards that
need a real non-loopback IPv4).

---

# Audit — 2026-08-23, unpushed `develop` (iteration 25)

Scope: `git log origin/develop..HEAD` — everything not yet pushed, i.e. the two merges landed this session
(`f2cf95dc4` the creativecore work, `23e8efc46` the re-grind queue) and their branches. Read-only pass. The
MEDIUM below was confirmed against the **production log** rather than reasoned about.

## HIGH

**H1 — a shutdown landing in the re-grind drain is ignored, and a fresh catalog pass starts anyway.**
`serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/GrinderApplication.kt`, the
continuous pass loop (`2376d75d6`).

The drain introduced a **second `GrindPool` per pass**, and the shutdown hook holds exactly one handle:

```kotlin
val requeued = requeue.drain()
if (requeued.isNotEmpty()) {
    GrindPool(grinder, workers).also { activePool.set(it) }.grindAll(requeued, force = true)
}
val batch = crawler.nextBatch()                                   // <-- no running.get() between
val pool = GrindPool(grinder, workers).also { activePool.set(it) }
```

The hook reads `activePool` **once** — deliberately, and the comment says why ("reading it twice could signal
one pool and wait on another"). So a stop during a drain signals the requeue pool, closes the engine, awaits
the workers, and reports a clean stop; `main` then falls through to `crawler.nextBatch()` and starts a *new*
pool, grinding new candidates and creating new containers **after the hook has finished**, with systemd's
`TimeoutStopSec` already counting down. The loop's only `running.get()` checks are at the top of the iteration
and after the catalog pass — neither is between the two pools.

Before this branch the invariant held by construction: one pool per iteration, set immediately before use, so
the hook's single read always named the pool that was running. The drain broke it. Severity HIGH because the
grinder's shutdown path is load-bearing — containers live in the docker daemon's cgroup, not the unit's, so
the hook is the only thing that can stop them, and this branch's own module doc says so.

## MEDIUM

**M1 — `/status` under-reports, and goes stale, for the whole drain.**
`status.beginPass(pass, batch.candidates.size)` is called *after* the requeue pool and counts only the catalog
slice. So while a drain of 300 forced re-grinds is running, `/status` still shows the **previous** pass's
number and size while `active` shows workers grinding candidates that belong to neither. The endpoint exists
to answer "what is it doing right now?", and during the one operation an operator is most likely to be
watching, it answers wrongly.

**M2 — a queued re-grind can wait six hours, which is not what "immediate" or the README promise.**
The inter-pass pause is a single uninterruptible `Thread.sleep(pause.toMillis())`, and
`GrindPacing.pauseAfterPass` returns `betweenSweeps` — default `SPC_GRINDER_INTERVAL` = 21 600 s — whenever a
completed sweep verified nothing. Queue work into a daemon that has just gone to sleep and nothing happens for
up to six hours. The README says "a running daemon takes them at the start of its next pass" without saying
how far away that can be, which reads as *soon*. The queue's whole justification is not waiting out a 30-day
TTL; trading it for a 6-hour one is better but still not what was built.

**M3 — pre-existing, found while auditing: the pass-completion log prints a whole `GrindPass`, not the pass
number.** `val pass = pool.grindAll(batch.candidates)` shadows the `var pass` counter, so
`log.info("Pass #$pass complete: …")` interpolates the data class. Present on `origin/develop`, so not this
branch's doing — surfaced here because the conventions require it rather than deferring it.

Confirmed against `~/.spc-grinder/grinder.log`, 14 such lines, e.g.:

```
Pass #GrindPass(reached=[GrindCandidate(projectUrl=https://modrinth.com/mod/lambdynamiclights,
slug=lambdynamiclights, popularity=49644693, platform=Modrin… complete: …
```

A multi-kilobyte line, dumping every candidate's URL and popularity, where a two-digit number belongs — in
the one line an operator greps to see pass progress.

## LOW

**L1 — `JsonRequeueStore.pending()` is not `@Synchronized` while `add` and `drain` are.** It only reads, and
`read()` degrades to empty on any failure, so the exposure is a momentarily stale count on `/status` rather
than corruption. But the inconsistency invites the next reader to conclude the annotation is decorative.

**L2 — the README's re-grind section does not say when "next pass" is.** Same fact as M2, stated where an
operator reads it; worth fixing in the same breath as M2 rather than separately.

## Verified clean — do not re-litigate

- **Commit hygiene across all 21 unpushed commits**: every code commit touches a single source set, test
  commits precede their fixes and were red, and the two merges carry the reasoning rather than a file list.
- **The re-grind queue's own contract** is pinned where it matters and the pins have teeth: the force
  (`aForcedGrindReVerifiesEvenAFreshVerdict` asserts both directions in one test), persistence across
  processes, identity by project id over slug, and the platform split.
- **The CLI path's placement** — before the SPC claims, printing to stdout — is guarded on *both* halves by
  `theRequeuePathRunsBeforeTheClaimsAndNeverLogs`, which reads the helper's own source because `main`'s body
  cannot see a log call made inside it. Verified in the built artefact too: a real
  `:serverpackcreator-grinder:run --requeue-before …` against a store sliced from the live 875-verdict file
  left only `requeue.json` behind, no `logs/`, i.e. no `ApiProperties` was constructed.
- **Not an HTTP endpoint** is the right call and is documented as such: the report server is unauthenticated
  by design, so a write endpoint would let anyone who can reach the page schedule unbounded container work.
- **`CrashLogStore`'s traversal guard** checks the name before touching the filesystem and confirms
  canonically afterwards; both the store-level and the served-over-HTTP cases are pinned, and a refusal is
  deliberately indistinguishable from an absent log.

## Resolution — iteration 25, same session

| Finding | Outcome |
|---|---|
| H1 shutdown gap between the two pools | **fixed** — `running` re-checked between them, landmined; guard keys on the two `GrindPool` constructions |
| M1 `/status` stale and under-reporting during a drain | **fixed** — announced before either pool, counting `requeued.size + batch.candidates.size` |
| M2 a queued re-grind waiting out a 6-hour pause | **fixed** — `GrindPacing.pollInterval`, wait served in 15 s slices, ends early when work is queued |
| M3 pre-existing shadowed pass counter | **fixed** — renamed to `catalogPass`, own commit pair |
| L1 `pending()` not `@Synchronized` | **fixed** |
| L2 README silent on how far "next pass" is | **fixed** with M2 |

**Both source guards were re-targeted before they went green, and that is worth stating plainly** rather than
leaving it to look like a moved goalpost. They originally keyed on `crawler.nextBatch()` and on
`requeue.drain()`; the fix moved both, so the guards asserted about landmarks that no longer carried the
meaning. The *intent* is unchanged — no new pool after a signalled stop, and a pass size that includes the
drain — and they now key on the two `GrindPool` constructions and on `beginPass`'s argument list. Teeth
re-checked **after** re-targeting: removing either fix fails its guard.

---

# Audit — 2026-08-23, unpushed `develop` (iteration 26)

Scope: as iteration 25, plus that iteration's own fixes (`d20fc2c8f`, `447acafe7`, `96701a362`,
`59f7d7130`). A fresh pass, weighted toward what the last one changed — which is where two of the three
findings are.

## HIGH

**H1 — the sliced inter-pass wait can throw and take the daemon's pass loop with it.**
`GrinderApplication.kt`, the inter-pass wait (`447acafe7`), with `GrindPacing.pollInterval`:

```kotlin
while (running.get() && System.currentTimeMillis() < wakeAt) {
    if (requeue.pending() > 0) { … break }
    val remaining = Duration.ofMillis(wakeAt - System.currentTimeMillis())
    Thread.sleep(GrindPacing.pollInterval(remaining).toMillis())
}
```

The loop condition guarantees `remaining > 0` *when it is evaluated*, but `requeue.pending()` runs between
that test and the subtraction — a synchronized read that stats and parses a JSON file. On the **final** slice,
where `remaining` is by construction somewhere in `(0, 15 s]`, an I/O stall longer than the remainder makes
`remaining` negative. `pollInterval` passes a negative duration straight through (`-5ms < 15s` is true), and
`Thread.sleep(-5)` throws:

```
PROBE Thread.sleep(-5) -> java.lang.IllegalArgumentException: timeout value is negative
```

`IllegalArgumentException` is not `InterruptedException`, so it escapes the `catch` around the wait, escapes
`while (running.get())`, and ends `main`. A fire-and-forget daemon stops grinding — no crash banner an
operator would notice, just a service that quietly does nothing until somebody looks. Once per pause there is
a window; across a six-hour pause there are 1 440 iterations and exactly one of them is the risky last, so the
expected time to hit it is passes, not years.

## MEDIUM

**M1 — `CrashLogStore.keep` reads an entire console into memory to keep 2 MiB of it.**
`CrashLogStore.kt`: `val text = console.readText()` precedes the `MAX_BYTES` check, so the cap bounds what is
*written*, not what is *read*. Boot consoles are streamed to disk uncapped and bounded only by the 15-minute
boot timeout, so a chatty mod can leave hundreds of megabytes — and `readText()` inflates that to roughly
double as a UTF-16 `String`. The `runCatching` turns an `OutOfMemoryError`… into nothing, because
`runCatching` catches `Throwable`: the daemon would swallow an OOM and carry on in an unknown heap state. The
truncation test passes because it plants a file just over the cap; nothing exercises the case the guard was
written for.

**M2 — `--requeue` accepts a link no platform can resolve and queues it anyway.**
`enqueueAndExit` maps every argument through `ModPlatforms.ofUrl`, which answers `Unknown` for anything that
is neither Modrinth nor CurseForge. The entry is queued, reported as `Queued 1 of 1`, and then fails in the
daemon hours later when `ClientsideVerifier.report` throws "No supported platform" — one line in a log the
operator is not watching. A typo in a URL should be refused by the command that reads it, which is the only
moment anybody is looking.

## LOW

**L1 — the catalog batch is now fetched before a drain that may run for hours.** Moving `crawler.nextBatch()`
above the drain (needed so `/status` can announce both sizes) means a long drain grinds a slice whose
popularity ordering is hours stale, and two catalog API calls are spent even when the pass then breaks for
shutdown. Neither is a correctness problem — the cursor is only advanced by `commit`, so an abandoned batch is
re-handed — and the alternative costs a mutable status API. Recorded so the next reader does not re-derive it.

## Verified clean — do not re-litigate

- **Cursor safety across the new `break`.** Breaking between the drain and the catalog pass leaves `batch`
  uncommitted; `nextBatch()` only *reads* cursors and `commit` is what persists an advance, so the slice is
  re-handed on the next start rather than skipped. This is the documented intent ("an interrupted pass
  re-hands the rest next time"), and the new early exit inherits it correctly.
- **The re-targeted guards have teeth after re-targeting**, re-checked by removing each fix.
- **`pollInterval`'s slice size** — 15 s across a six-hour pause is 1 440 wake-ups that each stat one small
  file, against a daemon that boots Minecraft servers in containers when it is awake. Not worth tuning.
- **`thePassCounterIsNotShadowed`** is narrow enough not to cry wolf: it asserts `var pass = 0` exists and no
  `val pass =` shadows it, which is exactly the defect and nothing else.

## Resolution — iteration 26, same session

| Finding | Outcome |
|---|---|
| H1 negative slice → `IllegalArgumentException` ends `main` | **fixed** — `pollInterval` clamps a negative remainder to zero, landmined |
| M1 crash console read whole before the cap | **fixed** — `tailOf` seeks; pinned **by measurement** on a 64 MiB console |
| M2 an unresolvable link queued anyway | **fixed** — `RequeueSelection.fromLinks` refuses and names it back |
| L1 catalog batch fetched before a long drain | **accepted, recorded** — cursor only advances on `commit`, so an abandoned batch is re-handed |

**M2's fix consolidates rather than patches, deliberately.** The one-shot path carried the *identical*
expression — `args.map { GrindCandidate(it, slugFromUrl(it), 0, ModPlatforms.ofUrl(it)) }` — so fixing only
the queue would have left the same defect one call site away, which is how a fixed bug comes back. Both now
resolve through `fromLinks`, and `slugFromUrl` moved with it.

**M1 is pinned by measurement, not by reading the code**, because that is the only formulation that separates
a bounded read from a lucky one: the existing truncation test plants a file just over the cap and passes
either way. The new guard writes a 64 MiB console and requires heap growth across `keep` to stay under the
file size. (The first cut of that guard asserted `length() > 64 MiB` against a file of exactly 64 MiB and
failed on its own fixture — caught because the expected red arrived for the wrong reason.)

**Teeth re-checked on both fixes** by restoring the old line: `readText()` fails
`anOversizedConsoleIsNeverReadWholeIntoMemory`, the two-branch `pollInterval` fails
`aRemainderThatHasAlreadyElapsedSlicesToZero`.

**Verified in the built artefact**, since the rejection is an operator-facing path:

```
$ spc-grinder --requeue …/creativecore https://modrint.com/mod/typo …/mc-mods/jei
Not a Modrinth or CurseForge project link, ignoring: https://modrint.com/mod/typo
Queued 2 of 2 project(s) for immediate re-grinding (0 already waiting); 2 now pending.
queued: [('Modrinth', 'creativecore'), ('CurseForge', 'jei')]
```

**Suite after both iterations: grinder 336 → 344, 0 failures** (23 skipped).

---

# Audit — 2026-08-23, `claude-grinder-favicon-hostname-forge` (iteration 27)

Scope: the 13 commits of `develop..HEAD` — a bundled favicon, container name resolution, the Forge
launch path, two classifier rungs and a scan-date column. Six code commits, each preceded by its own
red `test(...)` commit; no commit carries a `refactor:` label, so the behaviour-preservation rule is
not in play.

## HIGH

**H1 — `Error: could not open` is broad enough to destroy a true clientside HIGH, and it is checked
above the marker that produces one.**
`BootLogClassifier.kt`, `launchFailureMarkers` (`c54637df7`):

```kotlin
"|Error: could not open)", RegexOption.IGNORE_CASE
```

`classify` tests `launchFailureMarkers` at rung four and `clientOnlyClassMarker` at rung seven, so a
console that matches the former never reaches the latter. The pattern is unanchored and
case-insensitive, so *any* line containing the substring matches — including a mod's own log line:

```
[19:41:26] [main/ERROR] [polytone/]: Error: could not open assets/polytone/foo.json
java.lang.NoClassDefFoundError: net/minecraft/client/multiplayer/ClientLevel
```

That console is a textbook clientside crash and would now be scored **INCONCLUSIVE** — a true positive
silently dropped, which is the one failure mode this classifier's whole guard ladder is arranged to
avoid. The commit's own KDoc claims the opposite ("Matched with the launcher's own `Error: ` prefix so
a mod logging 'could not open' about one of its own files is not excused along with it"), which is
false: a log line can contain `Error: could not open` anywhere in it. So the defect ships with a
comment asserting it is absent — the stale-prose failure class this file's conventions single out.

The JVM launcher emits it as the **entire line**, with no timestamp or level prefix, while every mod
line carries one. Anchoring the alternative to line start (`^Error: could not open`) is exact — the
classifier already matches per line, so `^` means "the launcher said it" and nothing else.

## MEDIUM

**M1 — the new `Scanned` cell is the only table cell not HTML-escaped, and the doc says otherwise.**
`VerdictReportRenderer.kt`, `rowHtml` (`3b1392f1c`): every other cell goes through `esc(...)`;
`ScanDate.of(verdict.verifiedAt)` does not. No injection is reachable today — the value comes from a
fixed `yyyy/MM/dd` formatter over an `Instant`, so it can only be digits and slashes. What is broken is
the *invariant*, and the same commit edited that function's KDoc to read "and every cell is
HTML-escaped", which is now untrue. The uniform discipline is what makes the next cell safe to add;
one exception costs a character to remove and is otherwise a trap for whoever adds the cell after it.

## LOW

**L1 — `ContainerSpec.hostName` is unvalidated.** A blank one produces `withExtraHosts(":127.0.0.1")`
and the daemon refuses *every* create with a message about extra hosts rather than about the spec.
**Accepted, recorded:** both construction sites (`ContainerServerRunner`, `DockerLoaderInstaller`) take
the default constant, and unlike `ContainerResources` — which validates precisely because
`SPC_GRINDER_CPUS`/`SPC_GRINDER_MEMORY_GIB` reach it from the environment — nothing plumbs a value in.
A `require` here would guard an input that cannot currently exist.

**L2 — the icon is served without `Cache-Control`,** so a browser re-fetches 4 160 bytes per page load
of a loopback service. Accepted; not worth a header.

## Equivalence against the base — clean

`develop`'s unmodified test tree against this branch's production code (worktree at `HEAD`,
`src/test` replaced from `develop`, `--continue`):

| Module | Base guards | Failures | Compile errors |
|---|---|---|---|
| clientside | 136 | **0** | none |
| grinder | 344 | 2 | none |

Both grinder failures are `VerdictCsvExporterTest.emitsHeaderAndOrdersHighestConfidenceFirst` and
`emptyVerdictsStillEmitTheHeader` — the two exact `assertEquals` on the CSV header, changed
deliberately by the `Scanned` column and enumerated in that column's own red-test commit. **Zero
compile errors** is the load-bearing half: `ContainerSpec` gained a parameter and `respond` changed
shape, and no base guard's signature broke. Notably the base `PackVariablesTest`, `ReportServerTest`
and the whole clientside classifier suite pass untouched, so `USE_SSJ=false`, the two new favicon
contexts and the two new INCONCLUSIVE rungs regressed nothing that was already pinned.

## Verified clean — do not re-litigate

- **The favicon really ships in the artefact**, not only on the test classpath. Measured, since the
  guard resolves the resource from `build/resources/main` and would stay green if packaging dropped it,
  and `buildSrc`/packaging has no test harness by decision:
  `unzip -l serverpackcreator-grinder-dev.jar` → `de/griefed/serverpackcreator/grinder/report/favicon.png`,
  **4160 bytes**, byte-identical to `img/config.png`.
- **`CLEANUP` cannot delete the argfile the Forge boot now depends on.** `cleanServerFiles` runs only on
  `--cleanup` or when `.previousrun` shows a changed version, and `.previousrun` is in the install
  snapshot's runtime-state denylist — so a freshly staged grinder pack has none and never cleans.
  The `libraries/` tree the argfile lives in therefore survives the offline boot.
- **The extra host does not disturb the *networked* install container.** Measured on the bridge default:
  `/etc/hosts` carries `127.0.0.1 spc-grinder` ahead of the daemon's own `172.17.0.3 spc-grinder`, so
  the container's own name resolves to loopback — harmless for a container that only makes outbound
  calls — and outbound DNS is unaffected (`nslookup maven.neoforged.net` answers).
- **Matching the message and not the module** in `loaderBootstrapFailureMarkers` is deliberate and
  proven, not a shortcut: the production log named `java.base` read by `net.minecraftforge.eventbus`,
  the local reproduction of the identical launch named `java.management.rmi` read by `JarJarMetadata`.
- **`loaderBootstrapFailureMarkers`' own breadth.** `Could not find parent layer for module`,
  `Failed to find run file at` and `Failed to find startup arguments using run script path` are all
  distinctive upstream sentences with no plausible mod-log collision — unlike H1's, none of them is a
  generic verb phrase. Checked against the guard-ordering hazard H1 describes and cleared.
- **The `/tmp` `noexec` finding is recorded, not deferred.** JNA cannot map a native library out of a
  `noexec` tmpfs, which is the `com.sun.jna.Native` / `oshi` noise in `Modrinth-polytone-NeoForge.log`.
  It cost nothing there (crash-report diagnostics only, and the real client-class crash was still
  detected) and it must not be "fixed" in the classifier: those markers appear *inside* polytone's
  correct HIGH, so excusing them would destroy the verdict. The only real fix is `exec` on `/tmp`,
  which is a deliberate weakening of the untrusted-mod posture and therefore an owner's decision, not
  a cleanup. Landmined in `grinder/container/CLAUDE.md`.
- **Commit hygiene.** Every one of the six code commits is preceded by its own `test(...)` commit that
  was observed red, with the red output quoted in the message; tests and behaviour changes are never in
  the same commit; the single `docs:` commit touches only files documenting this branch's work.

## Resolution — iteration 27, same session

| Finding | Outcome |
|---|---|
| H1 `Error: could not open` suppresses a true clientside HIGH | **fixed** — anchored to line start, both directions pinned |
| M1 the `Scanned` cell was the only unescaped one | **fixed** — `esc(...)`, byte-identical output, labelled `refactor:` |
| L1 `hostName` unvalidated | **accepted, recorded** — no caller can supply one; unlike `ContainerResources`, nothing plumbs it from the environment |
| L2 no `Cache-Control` on the icon | **accepted, recorded** |

**H1's teeth were checked in the only way that proves them**: the guard was committed red
(`expected: <CRASHED> but was: <INCONCLUSIVE>` on a console holding a mod's `Error: could not open`
line *and* `NoClassDefFoundError: net/minecraft/client/multiplayer/ClientLevel`) and is green with the
anchor. The pre-existing `aJvmThatNeverLaunchedIsInconclusive` case — the launcher's own whole-line
message — stays green, so the anchor narrowed the guard without disarming it.

**M1 is deliberately labelled `refactor:` and deliberately carries no new test.** `esc` rewrites only
`& < > " '`; a `yyyy/MM/dd` string from a fixed formatter over an `Instant` contains none, so output is
byte-identical and every existing assertion stays as it was — which is what the label claims and what
makes a new guard pointless. A test would have to assert the *shape* of the call, which this repo's
conventions rule out.

---

# Audit — 2026-08-23, `claude-grinder-favicon-hostname-forge` (iteration 28)

Scope: as iteration 27, plus that iteration's own fixes (`ff8dc823d`, `b88f502d4`, `4e4cd96ca`).
Weighted toward the guard-ordering hazard H1 exposed — the natural follow-through is whether any
*sibling* marker has the same shape — and toward the two lists this branch incremented.

## MEDIUM

**M1 — nothing couples the table's header count to its cell count, and this branch changed both.**
`VerdictReportRenderer.kt`: `columns` (now 8 entries) and `rowHtml`'s cell list (now 8) are two
hand-maintained lists kept in step only by a reader noticing. Add a header without its cell and the
page still renders — every column past the gap displays its neighbour's data, and `sortBy(index)`,
wired from the header's *position*, sorts by the wrong column. Every existing guard looks for one
value somewhere in the page, so none of them notices a shifted table. Incrementing both at once, which
this branch did twice (crash-log cell earlier, `Scanned` here), is exactly when the two drift.

## Examined and cleared — do not re-litigate

- **`setupAbortMarkers` has H1's shape and is deliberately left broad.** `is not available for
  Minecraft` is a phrase a mod could plausibly log about its own feature gating, and it sits at rung
  three — one *above* the rung H1 was fixed at, so the same suppression is reachable. It is
  nevertheless correct as-is: that guard exists to prevent a **false HIGH** on a loader with no build
  for a new Minecraft, and for that job losing a true positive is the cheaper error — the engine's
  stated policy is to claim CRASHED only when sure. H1 was the opposite case: a newly added generic
  phrase whose own KDoc claimed a narrowness it did not have. Recorded so a later pass does not
  "fix" a breadth that is chosen. (`crashServer` does echo its message as a whole line, so anchoring
  is *available* there — it is simply not wanted.)
- **The `^` anchor versus docker's log framing.** `DockerJavaContainerEngine` splits each frame's
  payload on `\n`, so a line spanning two frames would arrive as two fragments and the anchor would
  see the second fragment's start rather than the line's. Not reachable: the log driver frames at
  write boundaries (and 16 KB), while the launcher's ~80-byte message is the process's first write —
  and a *substring* match would fail on a split line just as surely. Accepted.
- **`loaderBootstrapFailureMarkers` re-checked against H1's hazard.** All three alternatives are
  distinctive upstream sentences, not generic verb phrases, and none has a plausible mod-log
  collision. Cleared a second time, deliberately, because it is the guard H1's sibling review would
  otherwise have to revisit.
- **Dokka is clean for both touched modules** (`dokkaGenerateHtml`, no warnings), including the
  `[ScanDate]` reference from `VerdictCsvExporter`'s public KDoc to an `internal` object — which is
  the one new cross-visibility doc link on the branch.

## Resolution — iteration 28, same session

| Finding | Outcome |
|---|---|
| M1 headers and cells uncoupled | **fixed** — `everyHeaderHasACellBeneathIt`, teeth checked both ways |

**A characterization test, so it passes as written — which is precisely why its teeth had to be shown
rather than assumed.** Broken deliberately in both directions:

| Injected defect | Guard says |
|---|---|
| a ninth header, no cell | `expected: <9> but was: <8>` |
| a ninth cell, no header | `expected: <8> but was: <9>` |

Counted off the rendered page rather than off the two source lists, so it pins the consequence (a
misaligned table) and not the implementation that currently produces it.

---

# Audit — 2026-08-23, `claude-grinder-favicon-hostname-forge` (iteration 29)

Scope: the template work Griefed asked for mid-session (`c9106d0b1`, `a39285749`, `84a6d58fb`) — which
iterations 27 and 28 predate — plus its docs. This is the pass that mattered most, because the change
touches three shell templates and only one of them can be executed by the suite.

## HIGH

**H1 — the new guard's fail-safe polarity was inverted, and its own KDoc said otherwise.**
`forgeNeedsItsOwnArgfile` in all three templates. The doc read "anything unreadable falls through to a
bypass, which is the safe direction"; the code fell through to the **ServerStarterJar**:

```
Minecraft 26w05a was launched via the ServerStarterJar; expected Forge's argfile
```

The bypass is the safe direction precisely because the argfile path works for every Forge from 1.17 on
while the starter jar has a known failure — so a version nobody can parse must not be handed to the
latter. Exactly the polarity the Java-24 guard already gets right, and exactly the class of defect this
file records for that guard ("a guard inverted the wrong way still parses, still runs, and silently
reinstates the crash").

**H2 — comparing an unscreened version component is not harmless, and one call site was pre-existing.**
Found by the guard written for H1. Per shell:

| shell | comparing `26w05a` |
|---|---|
| bash | prints `bash: 26w05a: value too great for base` at the operator |
| fish | the comparison is an error |
| PowerShell | `[int]` **throws** (`RuntimeException`) — a snapshot-shaped version takes the whole start script down |

The PowerShell case is a live defect in the **launcher-era** check (`[int]$Semantics[0] -eq 1 -And …`),
which predates this branch. Both call sites are screened now, in one commit, because it is one concern.
The era check still falls to the modern era for an unreadable version, which is where anything not
plainly 1.x-and-old belongs, so its pinned behaviour is unchanged.

## MEDIUM

**M1 — the fish and PowerShell *callers* had no execution evidence, only a parse check.** The suite
executes bash's `setupForge` and asserts source fragments for the other two — the repo's documented
compromise, since neither shell installs everywhere. That covers the *helper* but not the caller wiring
the branch restructured (`SSJ_REFUSAL`, the `elif`, the folded argfile block). Closed by measurement
rather than by a new test, which would skip on every machine without those shells: the whole
`setupForge` was extracted and driven in a container. **fish agrees with bash on all eight versions
tried** (`1.17.1/1.20.1/1.20.2/1.20.3/1.20.4/1.21.1/26.2/26.20.2` → SSJ/SSJ/ARGFILE/ARGFILE/SSJ/SSJ/SSJ/SSJ).
The PowerShell whole-`SetupForge` drive did not complete — the amd64 image runs under QEMU on this host
and aborts or stalls on the file-writing parts — so **PowerShell's caller wiring rests on its parser plus
the helper matrix, and that residual gap is stated rather than papered over.**

**M2 — a suite count went stale inside the same session that changed it.** REFACTOR-LOG said grinder
**350**; the test-result XML says **351**. The count moved twice on one branch (an alignment guard added,
two `PackVariables` guards replaced by one). Corrected in `142f53db6`, and the corrected line now names
where the number comes from.

## Verified clean — do not re-litigate

- **All three shells agree on all eleven versions after the fix**, verified by *executing* the extracted
  helper, not by reading it:
  `1.17.1/1.19.2/1.20/1.20.1 → SSJ`, `1.20.2/1.20.3 → BYPASS`, `1.20.4/1.21.1/26.2/26.20.2 → SSJ`,
  `26w05a → BYPASS`. No shell emits a complaint, and both non-bash templates pass their own parser
  (`fish -n`, `Parser::ParseFile`).
- **The Java-24 guard's literal text survived the restructure**, so
  `allTemplatesResolveJavaAfterTheChecksAndFailSafeWhenItIsUnknown` still pins what it always did — checked
  by running it, not by eyeballing the diff.
- **`26.20.2` is the case that earns the major test.** It matches 1.20.2 component for component below the
  major; without the major test every modern pack would lose the starter jar.
- **The `USE_SSJ=false` branch of `setupForge` is untouched** — `develop`'s own template tests pass.
- **`HELP.md` under `src/main/resources` is generated and gitignored**; the root `HELP.md` is the source and
  `shipRootDocuments` copies it. The first edit went to the generated copy and was reverted.
- **The grinder's own `USE_SSJ=false` was removed rather than left as belt-and-braces**, deliberately: it
  disabled the starter jar for every version, so the grinder would have stopped exercising the path most
  user packs take — the path whose breakage it is the only thing that noticed.

## Resolution — iteration 29, same session

| Finding | Outcome |
|---|---|
| H1 inverted fail-safe polarity, doc claiming the opposite | **fixed** — unreadable ⇒ bypass, pinned red-first |
| H2 unscreened component compared (one site pre-existing, ps1 throws) | **fixed** — both sites screened, all three shells |
| M1 fish/ps1 caller unexecuted | **fish closed by measurement; PowerShell's caller gap stated** |
| M2 stale suite count | **fixed** — 351, with the source of the number named |

**Suites after all three iterations: api 356 → 361, clientside 136 → 139, grinder 344 → 351, zero
failures** (24 skipped in the grinder — the docker ITs and the two bind-address guards needing a
non-loopback IPv4). Read back from `<module>/build/test-results/test/*.xml`.

---

# Audit — 2026-08-30, unpushed `develop` (iteration 30)

Scope: `09bb6f262..develop` — **108 commits, 98 non-merge** (31 `test`, 25 `docs`, 22 `feat`, 17 `fix`,
3 `refactor`). That is everything since iteration 29's branch was merged; iterations 1–29 stand and are not
re-litigated. Requested against the Phase 0 baseline `69a587b6a`, which is 899 commits back — the earlier
sections already cover that ground, so this section audits only what no iteration has seen.

Suites at audit time: **api 376, clientside 228, grinder 410, app 149, plugin-example 3 — zero failures**
(1 skip in api, 28 in the grinder: the docker ITs, the live catalog ITs, the template matrix, and the two
bind-address guards that need a non-loopback IPv4). Read back from `<module>/build/test-results/test/*.xml`.

## HIGH

**H1 — `6de769d05 refactor(grinder): extract the configuration and the sweep out of main` deleted five
executing shutdown guards and replaced them with nothing.**
`serverpackcreator-grinder/src/test/.../GrindPoolShutdownTest.kt` — the commit removed 239 lines from it and
today the file is **an empty class**: a licence header, ten imports, a KDoc that still says *"Pins what
`systemctl stop` must do to the workers"*, and `{ }`.

Eight `fun`s went in. Three were re-homed to the `GrindLoopTest.kt` added by the same commit
(`theCatalogPassIsNotStartedWhenAStopArrivedDuringTheDrain`, `theLivePassCountsTheRequeuedCandidatesToo`, and
`thePassCounterIsNotShadowed` as `everyCompletedPassIsCounted`). **Five did not, and exist nowhere in the
repository:**

| Deleted guard | What it held |
|---|---|
| `interruptsAWorkerParkedInABootRatherThanWaitingOutItsBudget` | the interrupt, not the flag, is what wakes a worker inside a 15-minute boot |
| `givesUpAfterTheGraceWindowWhenAWorkerWillNotQuit` | the grace window is enforced against a worker that does not cooperate |
| `stopsWorkersTakingFurtherCandidates` | `requestStop` stops the queue being drained further |
| `tracksEveryWorkerBeforeAnyOfThemCanRun` | no worker starts untracked |
| `neverReportsACleanStopWhileAWorkerIsStillRunning` | `awaitStop`'s return value cannot lie |

Verified: `GrindPool.awaitStop`'s grace window is **no longer executed by any test**. The only surviving
references are source-greps of `main`'s body in `ShutdownWiringTest` (`body.indexOf("awaitStop(")`,
`body.contains("awaitStop(")`), which assert that a call is *written*, not that it *behaves*.

Why HIGH rather than MEDIUM, on three counts. The label claims behaviour preservation while behavioural
coverage was removed — the conventions' explicit stop-and-flag signal, and the carve-out does not apply
because these are not reference updates. The path is one this module's own `CLAUDE.md` calls load-bearing:
*"containers belong to the docker daemon's control group, not the unit's, so the shutdown hook is the only
thing that can stop them"*, with `TimeoutStopSec` sized against exactly the window these guards held. And the
loss is **silent**: a class with no `@Test` produces no `TEST-*.xml` at all, so the grinder's total went
344 → 351 → 410 across the range with nobody able to notice five guards leaving.

## MEDIUM

**M2 — seven change commits bundled their guard instead of pinning it red first.** A repeat of the finding
already recorded for 2026-07-31, and the conventions state the rule as *"Pin first means **commit** first, not
just write first"*. The dominant pattern in this range is correct — most of the 98 commits run `test(...)` →
`fix(...)`/`feat(...)` in adjacent pairs — but these seven land production and guard together, so nobody can
check out `<commit>^` and watch the pin go red:

`a05a25929`, `b76a6c9ee`, `edd962786`, `18f59b4bf`, `1714da922`, `41be7a2ea`, `d41c37e49`.

**M3 — `18f59b4bf feat(clientside): stage the dependencies a jar manifest declares alone` added an abstract
`ModPlatform.name` without updating the two anonymous implementations in the test tree, and nothing noticed
for eleven days.** Fixed on 2026-08-30 by `b3d7e65a5`, so the defect is closed; what is **not** closed is the
cause. Gradle's incremental compilation never recompiled `AttemptStagingIsolationTest` or
`BootVerifierSelectionTest`, so every green build in between — including the full builds this work was merged
on — was green without ever compiling those two files. `--rerun-tasks` is what exposed it, and nothing in the
build or CI runs that. A green build is not evidence that the test tree compiles.

## LOW

**L1 — the emptied `GrindPoolShutdownTest.kt` keeps ten imports it no longer uses** (`CountDownLatch`,
`AtomicBoolean`, `AtomicInteger`, `Timeout`, `Duration`, …). Kotlin does not warn on unused imports, so this
does not breach "no new compiler warnings"; it is dead code that makes the file look inhabited. Resolved
either way by whatever fixes H1.

**L2 — `acfed4b45 docs(app): finish the dokka backlog` touches 74 files in one commit.** Verified harmless
(see below), but a 74-file commit is not reviewable in the sense the one-concern rule is aiming at.

## Verified clean — do not re-litigate

- **No new `!!` anywhere in `src/main` across all 108 commits.** Checked by diffing the whole range and
  grepping the added lines; zero hits.
- **`6ec7dc042 refactor(grinder): rename CrashLogStore to BootLogStore` is a textbook reference-only update
  and is correctly labelled.** Every changed assertion line differs only in the receiver
  (`CrashLogStore.MAX_BYTES` → `BootLogStore.MAX_BYTES`); no expected value moved. This is precisely the
  carve-out the conventions added so a legitimate Strangler-Fig move is not cried wolf over.
- **`c3fe991e8 refactor(clientside): route every boot attempt through one BootVerifier.boot` changed no test
  at all** — one production file, existing assertions green. Correct.
- **Every `docs:` commit is comment-only or a sanctioned constructor reshape.** Checked by stripping comment,
  blank and KDoc-punctuation lines from each diff: `f81c4ba6f` and `428c6e74a` have **zero** non-comment
  changes; `acfed4b45` has 9 and `9000e99f1` has 21, and all of them are the
  one-parameter-per-line reshape the conventions explicitly permit (`AmountPerDate`, `TaskDetail`,
  `Prepared.Failed`, `Entry`, `RunResult.NotStarted`, `RunResult.Completed`), plus one within-file move of
  `trackedWorkerCount`. Parameter names, types and order survive untouched in every case. No `docs:` commit
  hides a behaviour change.
- **No `docs:`-only commit touches a non-source file it should not**, and no `feat:`/`fix:` commit sprawls:
  the widest are `f249e18cf` (15), `a05a25929` (15) and `41be7a2ea` (13), each confined to its stated feature.
- **The base-vs-branch equivalence proof was run and recorded** (`REFACTOR-LOG.md`, 2026-08-29): base
  `57d22b57c`'s unmodified test tree against this work's production code — four files uncompilable from three
  enumerated signature changes, the other 48 giving 323 tests with 16 deltas and **zero regressions**.
- **B6's merge gate was run and recorded** (`REFACTOR-LOG.md`, 2026-08-30): HIGH 8 → 4, three controls held,
  zero regressions, on six pre-registered candidates.

## Resolution — iteration 30, same session

| Finding | Outcome |
|---|---|
| H1 five shutdown guards deleted by a `refactor:` commit | **fixed** — restored, teeth checked by mutation |
| M2 seven commits bundled guard and change | **not fixable** — merged and pushed; process finding only |
| M3 "nothing runs a clean compile" | **wrong, and corrected** — CI does, it fired, the red went unactioned |
| L1 ten unused imports in the emptied file | **fixed** with H1 — all ten are used again |
| L2 74-file docs commit | no action — verified comment-only, noted for reviewability |

**H1 — closed.** The five guards are restored from `6de769d05^` verbatim, minus the three that legitimately
moved to `GrindLoopTest` and minus nothing else. Verified after the fix: the file holds **5 `fun`s**, emits a
`TEST-*.xml` again (it emitted none while empty, which is why the loss was invisible), and executes
`pool.awaitStop(` at **5 call sites** rather than grepping `main`'s text for it. All five pass unchanged
against today's `GrindPool`, so the deletion cost coverage but concealed no regression.

**Teeth checked by mutation, and the first mutation was the wrong one.** Making `requestStop()` a no-op
changed nothing — `awaitStop` sets `stopRequested` itself, so that mutation is vacuous rather than the guards
being weak. Mutating `awaitStop` to neither flag the stop nor interrupt fails exactly the two that depend on
it (`interruptsAWorkerParkedInABootRatherThanWaitingOutItsBudget`,
`stopsWorkersTakingFurtherCandidates`); the other three hold mechanisms that mutation does not reach — the
grace window expiring against a stubborn worker, worker tracking, and `awaitStop`'s return value not lying.
Recorded because "the guards survived a mutation" is worthless until you check the mutation was meaningful.

**M3 — the finding was wrong, and checking it was worth more than the fix would have been.** `test.yml`
triggers on `push:` and `pull_request:` and runs `./gradlew build` on a fresh runner, so the clean-compile
gate exists. It also **fired**: Forgejo `test.yml` runs **#301 (`592f1ce21`)** and **#306 (`388b6e3a2`)** both
concluded **failure** on `develop` and stayed unactioned for about a week. Verified locally rather than
inferred — a worktree at `388b6e3a2` fails `:serverpackcreator-clientside:compileTestKotlin --rerun-tasks`
with the two `ModPlatform` fixtures, so the red was real, not flaky.

The half worth keeping is *why it stayed invisible locally*: a plain build of that same commit reports
**`BUILD SUCCESSFUL in 5s`** off the build cache (`org.gradle.caching=true`), against **`BUILD FAILED in 21s`**
with `--rerun-tasks`. Every local full build agreed with itself and disagreed with CI. Recorded in the root
`CLAUDE.md`.

**No workflow change was made, deliberately.** Adding a second clean-compile step cannot help when the first
one's failure is not read, and `.claude/rules/ci-workflows.md` makes clear that `.forgejo/workflows` edits are
not free. The remedy is to check the pipeline after a push.

**Suites after the fixes: api 376, clientside 228, grinder 415 (was 410 — the five restored), app 149,
plugin-example 3. 1171 total, zero failures, 29 skipped.** Read from
`<module>/build/test-results/test/*.xml` after a full `./gradlew build`.

---

# Audit — 2026-08-30, unpushed `develop` (iteration 31)

Scope: iteration 30's range plus its own three fixes (`3ef3930fe`, `f68041c4a`, `9568120e1`). A re-audit of
the fixes themselves, per the convention that a fix pass is audited like any other.

## HIGH

None.

## MEDIUM

**M1 — `M2` from iteration 30 stands and cannot be closed.** Seven change commits bundled their guard with
the change (`a05a25929`, `b76a6c9ee`, `edd962786`, `18f59b4bf`, `1714da922`, `41be7a2ea`, `d41c37e49`). They
are merged and pushed; rewriting published history costs more than the evidence is worth. Carried forward as
a standing process finding rather than re-reported each iteration.

## LOW

**L1 — `GrindPoolShutdownTest`'s restored KDoc describes the file's *original* eight-guard scope.** It reads
*"Pins what `systemctl stop` must do to the workers"*, which is true of the five it now holds; the three that
moved to `GrindLoopTest` are not mentioned in either file's doc as having moved. Harmless — no claim is false
— but a reader tracing the sweep guards has nothing pointing them across. Not fixed: editing it would touch a
file this iteration just restored verbatim, and the value is small against the cost of another diff over
recovered code.

## Verified clean — do not re-litigate

- **The restoration is verbatim, not a rewrite.** `3ef3930fe` adds exactly the five `fun`s that
  `6de769d05` removed, with their original bodies and KDoc; the only omissions are the three that moved to
  `GrindLoopTest` and the closing brace adjustment. Diffing the restored file against `6de769d05^`'s copy
  shows no assertion, no expected value and no fixture changed.
- **The guards execute rather than grep.** Five `pool.awaitStop(` call sites in the restored file; the
  source-text assertions in `ShutdownWiringTest` are untouched and still cover the *wiring* inside `main`,
  which is a different question and still cannot be executed.
- **The loss is now detectable.** The file emits a `TEST-*.xml` again. While empty it emitted none, so the
  grinder's totals moved 344 → 351 → 410 across iteration 30's range with five guards leaving and no count
  changing — the mechanism that made H1 silent, and the reason a count is not a coverage check.
- **L1 from iteration 30 is closed by the same commit**: all ten imports are used again, and a
  `--rerun-tasks` compile of the module emits zero warnings from that file.
- **No source outside the audit's scope was touched by the fix pass.** `3ef3930fe` is one test file;
  `f68041c4a` and `9568120e1` are `CLAUDE.md` only.
- **The full suite is green after the fixes**: 1171 tests, 0 failed, 29 skipped.

# Audit — 2026-09-01, unpushed `develop` (iteration 32)

**Scope:** `git log 03047a7c0..HEAD` — **17 commits** (6 merges, 11 working commits) across `-api`,
`-clientside`, `-grinder` and the grinder's deploy scripts. All were produced in one session responding to
live-grinder defect reports: a Quilt scan dropping Fabric descriptors, INCONCLUSIVE verdicts that discarded a
survived boot, a deploy script that only warned about the headless browser, a `/status` dashboard, and Fabric
API's module ids going unresolved.

**Method:** commit-by-commit hygiene review; `--rerun-tasks` clean compile of all three changed modules to
catch warnings and the incremental-compilation blind spot this repository has already paid for; suite counts
re-derived from `build/test-results/test/*.xml`; guard teeth checked by deliberate breakage.

## HIGH — none

No behaviour regression found. Module boundaries intact (`-clientside` gained nothing pointing outward,
`-grinder`'s report still has no Spring). No new `!!` in production code. The one change to the **published**
`-api` surface (`fix(api): keep a Fabric jar's declaration when scanning a Quilt pack` — `ScannedMod.descriptorRead`
+ `QuiltPackScanner`'s Fabric-descriptor fallback)
**did** get its `claude-docs/API-BEHAVIOUR-CHANGES.md` row — checked, because a silent behaviour change on the
Maven-published module is the highest-cost miss available in this repository.

## MEDIUM

- **MED-1 — `fix(clientside): count a survived boot, and say why a locked file would not download` bundles a
  pure refactor with the behaviour change it was labelled for.** The
  behaviour change is ~5 lines (a `SURVIVED -> Confidence.LOW` branch in `aggregate`). The commit carries
  **90 changed lines** in `ClientsideVerifier.kt`, because it also moved `aggregate` into the companion,
  renamed it `aggregateFor`, and re-wrapped three lines the move pushed past the column limit. The
  conventions ask that "pure refactor (no behavior change)" and "change behavior" be separate commits, and
  the cost here is the usual one: the fold's history now shows a wholesale rewrite, so a later reader cannot
  see the one-line ranking decision without diffing by eye. The move was *necessary* (the test calls it
  statically) — it simply belonged in its own commit ahead of the fix.

- **MED-2 — Two red-committed pins contained bugs of their own, fixed in the implementation commit.** The
  pin-first rule exists so someone can check out `<fix>^` and watch the guard go red for the stated reason.
  Twice in this range the red was partly self-inflicted:
  - `test(grinder): pin a self-contained live dashboard for /status` never wired `consoleRules` in its
    fixture, so `bootRules.source|ruleCount|undecidedVerdict|errors` could not resolve **even with a correct
    implementation**; the following `feat(grinder)` commit edited the test to add it.
  - `test(clientside): pin that Fabric API's modules resolve to Fabric API` wrote
    `"fabric-resource-loader-v${'$'}version"`, an escaped dollar producing the literal `...-v$version`; the
    following `fix(clientside)` commit corrected it.

  Both were caught immediately and the guards are sound now, but the committed red state is not the clean
  "implementation missing" signal the rule is for. Checking the fixture resolves against a *real* server
  before committing the pin would have caught the first; running the pin once before committing catches both.

- **MED-3 — Root `CLAUDE.md`'s api suite count went stale in this very range. FIXED in this audit.** It read
  `381 (1 skip)`; re-derived from the test XML it is **382 (1 skip)**. `test(api): pin that a Quilt pack scan
keeps a Fabric jar's dependencies` added one test to
  `ModScannerSidenessTest` and no commit updated the row. This is precisely the defect the *Cite names, not
  snapshots* convention names — "suite counts left behind by the tests that were just added" — and it is the
  fourth consecutive audit to find an instance of that class. The clientside (263) and grinder (434) rows,
  updated in the same session, were correct.

## LOW

- **LOW-1 — New tests introduced inside implementation commits rather than pinned first.**
  `StatusDashboardScriptTest` (172 lines) landed entirely in `feat(grinder): a live, framework-free dashboard
  for /status`, and the Fabric-module collapse guard (30 lines) in `fix(clientside): resolve Fabric API's
  module ids to Fabric API`. Both are defensible as characterization — the collapse guard pins
  behaviour that already worked, and the script test's teeth *were* verified in-session by reinstating the
  bug — but neither has a red commit, so the verification leaves no evidence, which is the same gap the
  conventions record for the eight commits of 2026-07-31.

- **LOW-2 — Three new `!!`, all the same line.** `val nodeBinary = node!!` appears verbatim in each of
  `StatusDashboardScriptTest`'s three tests, after an `Assumptions.assumeTrue(node != null, …)` that Kotlin
  cannot smart-cast through. The convention is "no new `!!`"; the honest fix is to have the helper return the
  path and skip inside itself, or to bind once in a `@BeforeEach`. Test-only, so the blast radius is nil, but
  it is duplication as well as an idiom violation.

- **LOW-3 — `StatusDashboardRenderer.PAGE` has no doc comment.** Every other member of the object has one,
  and the conventions extend the requirement to unexported constants explicitly. Its neighbour `toHtml()`
  carries the explanation, which is presumably why it was missed.

- **LOW-4 — The dashboard's failure text over-claims on the first poll.** `feed(false, "unreachable (…) —
  showing the last successful poll")` is accurate on any poll but the first; when the *first* fetch fails
  there is no previous poll and every panel is empty, so the message describes a state the page is not in.

## Observation — an open item, not a defect in this range

- **OBS-1 — the Fabric API module fix is asymmetric, and the Quilt mirror is untouched.** `KnownModIds` now
  resolves `fabric-<something>-v<digits>` to Fabric API, closing the case where a descriptor names a *module*
  of a project neither platform publishes separately. The same structural shape exists for Quilt: QSL also
  ships as many modules, and this codebase maps only `quilted_fabric_api`/`qsl`, while
  `BootVerifier.environmentProvidedIds` excuses only `quilt_loader` and `quilt_base`. Any other QSL module id
  a Quilt descriptor declares therefore falls through to Modrinth as a slug guess and goes unmapped —
  unstaged, exactly as `fabric-resource-loader-v0` did. **Unverified:** the exact QSL module ids (the
  repository groups them by category rather than by published id), so confirm the id shape against a real
  `quilt.mod.json` before adding a rule; guessing at it would rebuild the un-pinned table `KnownModIds`
  exists to avoid.

## Not findings / positives (verified — do not re-litigate)

- **Pin-first done properly twice.** The `test(api)`/`fix(api)` pair for the Quilt pack scan, and the
  `test(clientside)`/`fix(clientside)` pair for the survived boot, each land the
  guard red in its own commit and the fix touches **no test file** — confirmed with `git show --stat <fix> --
  '*Test.kt'`.
- **Guard teeth checked by deliberate breakage, three times.** Renaming the string-literal key `"loaderCache"`
  in `statusJson()` made `everyFieldTheDashboardReadsExistsInTheStatusDocument` the **only** failure in all
  434 grinder tests; reinstating `safeHref`'s base URL turned `theLinkHelperOnlyAcceptsAbsoluteHttpUrls` red;
  renaming the Kotlin property `passRunningSeconds` failed at *compile* time via the pre-existing
  `GrinderStatusTest`, which is what established that `READ_FIELDS` covers the untyped half rather than
  duplicating the compiler.
- **No new compiler warnings.** `--rerun-tasks` clean compile of `:api`, `:clientside` and `:grinder` test
  source sets: every `w:` line is a pre-existing Java-deprecation in a file this range did not touch, and the
  one touched file that carries such warnings (`ForgeTomlScanner.kt`) received a two-line change unrelated to
  them.
- **Measurements recorded rather than asserted**, per the *cite names* convention: the module rule measured at
  44 of 46 `FabricMC/fabric` directories with the two misses named and explained; Playwright's pinned Chromium
  revision read out of `driver-1.62.0.jar`'s `browsers.json` rather than assumed; the installer's unit-file
  bug demonstrated by executing the resolution block against all three states.
- **Deploy-script changes carry no test harness, and that ceiling is stated rather than quietly accepted** —
  the same position `buildSrc` holds. Verification went in the commit messages (`bash -n`, `--help` rendering,
  `--nonsense` still rejected, the CLI main class resolving off the real installed dist).

## Iteration 32 — resolution (2026-09-01)

Every finding actioned, on branch `claude-audit-32-followups`. Status of each:

| Finding | Outcome |
|---|---|
| MED-1 refactor bundled with behaviour change (the survived-boot `fix(clientside)`) | **Closed by the rebuild below** |
| MED-2 red pins carrying their own bugs | **Closed forward** — new convention in root `CLAUDE.md` |
| MED-3 stale api suite count | **Fixed** in the iteration-32 audit commit (381 → 382) |
| LOW-1 guards added inside implementation commits | **Accepted, not rewritten** — see below |
| LOW-2 three `!!` in `StatusDashboardScriptTest` | **Fixed** — `requireNode()` returns non-null |
| LOW-3 `StatusDashboardRenderer.PAGE` undocumented | **Fixed** |
| LOW-4 dashboard over-claims on the first failed poll | **Fixed** — `everLoaded` gates the wording |
| OBS-1 QSL mirror of the Fabric API module gap | **Verified and closed** — see below |

**MED-1 and LOW-1 are accepted rather than rewritten, deliberately.** Both are commit-shape defects in
history that is already merged into `develop`. Fixing them means rebasing 17 commits including six merges to
re-cut two of them, and the repository has already decided this trade once: the `358675fbf` entry records
that a mislabelled commit found after merging is remedied by *the audit entry*, because the alternative is
rewriting shared history. The forward-looking half is what has value, and MED-2's convention is it.

**MED-2 is closed as a rule, since the instances cannot be.** Root `CLAUDE.md` now carries *"Run the pin
before you commit it red, and read why it failed"*, with both 2026-09-01 instances as its evidence — the
fixture that omitted `consoleRules` and the `${'$'}` escape that produced a literal `...-v$version`. It sits
directly above the existing *pin first means commit first* rule, because it is the failure mode that rule
does not catch on its own: a red commit proves nothing if the red is the guard's own bug.

**OBS-1 verified and closed.** The observation was recorded unverified because the QSL repository groups
modules by category rather than by published id. Resolved by reading **all 47 `quilt.mod.json` files** in
`QuiltMC/quilt-standard-libraries` (branch 1.21.5) and collecting their `depends` entries: **33 distinct
`quilt_*` module ids**, with `quilt_resource_loader_testmod` declaring `["quilt_loader",
"quilt_resource_loader"]`. The gap was real and identical in shape to the Fabric one.

- The rule is **not** a copy of Fabric's: QSL ids are underscored and carry **no** API-version suffix, so
  `fabric-<x>-v<digits>` matches none of them. `^quilt_[a-z0-9_]+$` → `qsl` / `634179`.
- `notQsl` holds `quilt_loader` — the loader, not a module. It is already dropped before staging, so mapping
  it would change nothing observable; it is excluded because a lookup table other code trusts should not
  record a false fact merely because the falsehood is unreachable today.
- **One sub-gap raised, then closed on Griefed's instruction (2026-09-01).** `quilt_base` *is* a QSL module — `library/core/qsl_base` exists
  and `quilt_base_testmod` depends on it — yet `-api`'s `QuiltScanner.dependencyExclusions` strips it at scan
  time as "the platform", and `BootVerifier.environmentProvidedIds` repeats that. So it never reaches
  staging. Closing it would mean changing existing assertions in the **published** module, which the
  conventions treat as a stop-and-flag rather than a fix to make unilaterally, and the reachable case is
  narrow: a mod whose *only* QSL dependency is `quilt_base`, since any other module now pulls QSL in. Raised
  rather than changed unilaterally — and Griefed then said to change it, which is the flag doing its job.
  **Now fixed:** `QuiltScanner.dependencyExclusions` drops only `(quilt_loader|java|minecraft)` and
  `environmentProvidedIds` no longer lists `quilt_base`; four guards pinned red first across both modules,
  two existing expectations updated under a `fix:` label, one `API-BEHAVIOUR-CHANGES.md` row for the extra
  `ModDependency` a Quilt scan now returns. Final suites: api **383** (1 skip), clientside **267**, grinder
  434 (29 skip), app 149.

Suites after the follow-ups, re-derived from `build/test-results/test/*.xml`: api 382 (1 skip), clientside
**266** (was 263), grinder 434 (29 skip), app 149. All green.

## Iteration 32 — MED-1, LOW-1 and MED-2's instances closed by rebuilding the history (2026-09-01)

The resolution above recorded MED-1 and LOW-1 as *accepted, not rewritten*, on the `358675fbf` precedent
that a mis-shaped commit found after merging is remedied by the audit entry. Griefed overrode that and the
history was re-cut, taking MED-2's two instances with it — those had been closed only as a *rule*, because
the bad red commits were thought immovable.

**Correction, recorded because it is the more useful half of this entry.** The rebuild was proposed on the
stated ground that *nothing had been pushed*. That was false and it was never checked: `origin/develop`
already held the 26 commits, so `358675fbf`'s precedent applied in full rather than being inapplicable. The
rewrite therefore needed a **force-push of a shared branch**, which Griefed did on 2026-09-01 after being
shown the divergence. Nothing was lost — the tip trees are byte-identical — but the decision was taken on a
premise nobody had verified. **Check `origin/<branch>` before proposing a history rewrite;** the cost of
being wrong is borne by everyone who has already pulled.

**Method.** `git rebase -i` is unavailable in this environment, so the range was replayed explicitly: each
feature branch re-created from `03047a7c0`, its commits re-made, and each `--no-ff` merge restored with its
original message. **All nine merges survive** and the resulting tree is byte-identical to the pre-rebase tip
(`git diff --stat` empty against the `backup-pre-rebase-20260901` ref). 26 commits became 29, and 30 once
this entry landed.

**MED-1 — the survived-boot `fix(clientside)` split, and the refactor moved ahead of the pin.** It is now
`refactor(clientside): move the confidence fold into the companion` → `test(...)` → `fix(...)`. Putting the
refactor *first* is what makes it honest: `aggregate` becomes `aggregateFor` in the companion while nothing
yet references it, so that commit is **green** — verified by running the clientside suite at it. The
confidence ladder was diffed against its previous form to confirm not one branch changed. The behaviour
change is then a five-line commit instead of a ninety-line one.

**LOW-1 — the two guards buried in implementation commits now have their own.**

- `StatusDashboardScriptTest` was written after the renderer and found the `safeHref` bug, so the honest
  shape is four commits, and that is what history now says: `test` (pin) → `feat` (dashboard, carrying the
  base-URL bug) → `test(grinder): execute the dashboard's script under node` → `fix(grinder): the dashboard
  links only absolute http(s) URLs`. Verified by checking out each: the feat commit is green, the node guard
  is **red on `theLinkHelperOnlyAcceptsAbsoluteHttpUrls`**, the fix is green. The bug and its discovery are
  now legible from the log instead of being invisible inside one commit.
- The Fabric-module collapse guard moved into the pin commit, where it belongs: it is **red before the
  fix** — `theManyModulesOfFabricApiCollapseToOneDependency` fails there — because without the registry
  change the module ids do not resolve to `fabric-api` and so are not filtered.

**MED-2's instances, not just its rule.** The dashboard pin's fixture now wires **every** optional
collaborator of `ReportServer`, so that commit is red for exactly one reason — `Unresolved reference
'StatusDashboardRenderer'` — rather than also for four field paths that no implementation could have
resolved. The Fabric pin carries the corrected `v${version}` interpolation rather than the literal that was
fixed a commit later. Both were checked by reading the failures at those commits, which is what the
convention added yesterday asks for.

Suites at the rebuilt tip: api 383 (1 skip), clientside 267, grinder 434 (29 skip), app 149 — all green.
Every iteration-32 finding is now closed; none remain accepted-with-reason.

## Iteration 32 — the citations this file's own rebase killed (2026-09-02)

Rewriting the history above orphaned **13 short commit hashes cited in this file**, across 26 occurrences.
Eight were killed by the 2026-09-01 rebase; the other five had already been orphaned by an earlier one and
are now reachable from **no ref at all** — `git for-each-ref --contains` finds nothing for the CPU-cap
series (`test(grinder): pin the per-container CPU cap and its wiring`, `feat(grinder): express the container
CPU cap in cores, against a stated period`, `feat(grinder): make the per-container CPU cap configurable`,
`docs: record the CPU-cap knob and the quota-without-period finding`) or for `docs(grinder): close the
deployment gaps this outage ran into`. They survive only in the object store until gc, after which their
hashes would not even resolve — which is why they are named here by subject rather than by the hashes that
are about to become meaningless.

All 26 now name the **commit subject** instead, which is what the *Cite names, not snapshots* convention
asks for and what survives rebase, cherry-pick and squash. Verified by sweeping **every tracked `.md`** for
hex tokens that `git cat-file -t` resolves to a commit and testing each against
`git merge-base --is-ancestor <hash> develop`: this file is clean.

Two other files carry non-develop citations and are deliberately left alone. `.claude/rules/ci-workflows.md`
names `50fd50f37`, which is a real release commit on `origin/alpha` — outside `develop` by design, not
orphaned. `CHANGELOG.md` names ~31 hashes that resolve to no branch at all, but it is **generated by
semantic-release** and rewritten on every release, so hand-editing it would be both futile and wrong.

The lesson is not that a rebase is dangerous. It is that **this file is the one place in the repository that
cites hashes at volume**, so it is the one guaranteed casualty of any history rewrite — and it had already
been hit once (the "54 commit hashes killed by a rebase" incident the root `CLAUDE.md` records) before being
hit again here. Write subjects the first time.


# Audit — 2026-09-02, unpushed `develop` (iteration 33)

**Scope:** the twelve commits of 2026-09-02 — two audit-citation repairs, a Chromium launch probe for the
installer, a circuit breaker for the headless-browser download route, and the removal of that route and all
of Playwright. **Every finding was actioned, so this is the report and its resolution together: acting on it
changed the history it described.**

Commits are named by **subject, not hash**. Acting on the findings re-cut six of them, which would have
orphaned every hash cited here — the defect iteration 32 found twice and the root `CLAUDE.md` convention
warns about. Written that way the first time.

## HIGH — none

No module boundary crossed (the removal *reduced* `-clientside`'s dependencies). No `-api` change, no
plugin-API contract touched. The one breaking change is labelled `feat(…)!` with its app-user consequence in
the body.

## MEDIUM

- **MED-1 — the removal shipped with stale operator documentation. FIXED.**
  `serverpackcreator-clientside/README.md` listed "Browser system libraries" as a prerequisite, described the
  headless-browser fallback over a dozen lines, instructed `npx --yes playwright install-deps chromium`, and
  offered that command in two troubleshooting rows — telling operators to install a capability that had just
  been deleted, in the user-facing README of that very module.
  **Root cause, mechanical:** the completeness sweep grepped `"Playwright\|BrowserDownloader"`
  **case-sensitively**, and the file writes the tool lowercase inside `npx --yes playwright install-deps` —
  0 matches where `grep -i` finds 3. An earlier sweep *had* listed the file; it was dropped on the strength
  of the case-sensitive re-check. **Verify a removal with `grep -i`, or the check confirms only what it can
  see.** Now rewritten to state the limit honestly, with a historical note so a reader of the old version
  knows why the prerequisite vanished; every tracked `.md` re-swept case-insensitively, leaving two hits that
  are both historical prose.

- **MED-2 — the removal was one 23-file commit where several would each have compiled. FIXED by re-cutting,
  possible because none of it was pushed** — `origin/develop` sat at the launch-probe merge, checked *before*
  touching anything, which is the lesson from yesterday's rebase on a false premise. It is now four commits:
  the guard (red) → *stop using and delete the route* (code + tests) → *drop the dependency* (build, CI,
  installer) → *the documentation*. Each compiles; the second onward are green.
  The re-cut also **relocated the evidence**: 274.7 MB → 77.8 MB is now measured either side of the build
  commit that causes it, rather than quoted as a whole-session figure in a commit that also moved code. That
  was the audit's actual objection, and splitting fixed it instead of merely reporting it.

- **MED-3 — a feature was built and deleted inside 34 minutes. The artifact is gone from history; the lesson
  is a convention.** `BrowserRouteBreaker` was pinned (189 lines of guards), implemented, wired through two
  modules and documented, then removed when Griefed asked whether Playwright was needed at all. Because it
  was created *and* deleted inside the unpushed range, the re-cut replays the work without it ever existing,
  so the record shows the removal rather than the detour.
  Every commit in the original sequence was correctly shaped, which is precisely why shaping did not save it.
  Root `CLAUDE.md` now carries **"Question the requirement before you optimise the cost of meeting it"**: the
  breaker bounded the *cost* of a route whose *existence* had not been questioned, with the evidence to ask
  the prior question already in hand.

## LOW

- **LOW-1 — a `fix:` commit grouped the breaker with a report-text change. Dissolved by MED-3's re-cut**; the
  commit no longer exists, nor does the code it carried.
- **LOW-2 — two existing expectations changed inside the implementation commit. Correct as-is, no action.**
  `aLockedFileSaysWhyItCouldNotBeDownloaded` stopped asserting the message names a "browser" and now asserts
  it names Modrinth and mentions neither browser nor Playwright; `BootVerifierSelectionTest` lost a
  constructor argument with no expectation moved (the reference-only carve-out). The first is a genuine
  expectation change — correct for `feat!` rather than `refactor`, and unsplittable without a red
  intermediate. Recorded rather than left implicit.

## Not findings / positives (verified — do not re-litigate)

- **Both red pins were run before being committed and each failed only for its stated reason**, per the
  convention added 2026-09-01. The routing guard failed on exactly its two absence assertions.
- **The removal is provably complete in code**: `JarDownloaderRoutingTest` asserts `BrowserDownloader` is
  absent from the classpath, and `playwright` appears in **0** runtime-classpath entries for `-clientside`,
  `-grinder` and `-app`. MED-1 was a documentation gap, never a code one.
- **A documented landmine was re-triggered and caught pre-commit** — the breaker's constructor parameter
  first went in after `bootArtifactSink`, which this module's `CLAUDE.md` warns re-binds trailing-lambda call
  sites. Disclosed rather than quietly fixed, and now moot.
- **The launch probe is measurement-verified to the stated ceiling for deploy scripts** (no harness exists,
  none added): `bash -n`, `--help`, unknown-flag rejection, and the real CLI invocation against the installed
  dist. It produced the fact that shaped everything after it — headless Chromium is a *separate* binary,
  `chromium_headless_shell-1234`.
- **Diagnoses were falsified, not defended.** The opening hypothesis (missing Chromium/OS libraries) was
  disproved by the probe's own output and abandoned; its replacement (Cloudflare) was tested by direct fetch,
  403 with challenge markers on two user agents, with the caveat stated that curl's 403 does not by itself
  prove Chromium is blocked.
- **Help circumventing the bot challenge was declined**, and what shipped removes the workaround rather than
  hardening it — consistent with the platform's and the authors' opt-out.
- Every measurement quoted was re-run for this audit: 192.9 MB `driver-bundle`, 3.0 MB `driver`, 274.7 →
  77.8 MB app jar, suites 383 / 262 / 434 / 149 / 3.

---

## Iteration 34 — the result-system redesign, plus the JEI and optional-dependency fixes (2026-09-04)

**Scope:** the commits of 2026-09-03 and 2026-09-04 across three branches — `claude-verdict-redesign`
(17 commits, `develop..HEAD`), `claude-optional-dependencies` (3), and the JEI work merged into `develop`
(4). Commits are named by **subject**, not hash, per the repo's own convention.

**Method:** commit-by-commit against the refactoring conventions; the pin boundary verified by *checking
out each commit in a scratch worktree and running the suites*, not by trusting the commit messages; every
"never ran" code path traced by hand against `Verdict.ERROR`'s stated meaning.

**Method note worth keeping — the first pass produced a false result.** Detecting failure by grepping the
Gradle output for `FAILED` reported **every** commit red, including ones known green. The grinder's own test
fixtures log `Done Modrinth/mod50 → FAILED after 0s`, so the marker appears in passing runs. Re-run on the
build's **exit status**, the picture was clean. A detector that cannot tell its subject from its subject's
log output is worse than no detector, because it produces confident nonsense.

### HIGH

- **HIGH-1 — `packPostProcessor` failure is reported as INCONCLUSIVE, re-creating the exact conflation the
  redesign exists to remove.** `BootVerifier.runPrepared` returns
  `BootOutcome(BootResult.INCONCLUSIVE, null, "Pack post-processing failed: …")` without
  `stagingPrevented = true`. The post-processor is the grinder's `overlayLoaderInstall`: when it throws, **no
  container ever runs**, so this is a grind that could not be performed — `Verdict.ERROR` by the definition
  committed in *"pin the four-state verdict, and that a prevented grind is an Error"*. It currently publishes
  as "the boot ran and taught us nothing".
  **Why this is the worst possible site for the bug:** the overlay is a *loader-cache* operation, so it fails
  exactly when the host is broken — the same class of event as the missing-runtime-image outage, whose whole
  lesson was that a host defect must not be published as thousands of verdicts about mods. The redesign fixed
  the staging refusal and left this one.

- **HIGH-2 — `RunResult.NotStarted` is reported as INCONCLUSIVE for the same reason.**
  `BootVerifier.outcomeFor` maps it to a plain INCONCLUSIVE. `NotStarted` means the runner never started the
  server at all (`ServerRunner.kt:98`: *"No start.sh in the generated server pack."*), which is definitionally
  a prevented grind.
  The stage-1 pin `aStagedGrindWithNoObservationIsAnError` looks like it covers this, and does not: it
  asserts on `boot == null`, whereas `NotStarted` yields a **non-null** outcome carrying INCONCLUSIVE, so
  `verdictOf` never reaches that branch. **A guard that appears to cover a case it cannot reach is worse than
  an absent one**, because it stops anyone looking.

### MEDIUM

- **MED-1 — the `Confidence` deletion is one 33-file commit where three would each have compiled.**
  *"retire Confidence and aggregateFor"* changes 11 main and 22 test files at once, spanning two modules. It
  was separable with no red intermediate: (1) `-clientside` (delete the enum, move the note into `verdictOf`,
  migrate `supersededByLoader`), (2) `-grinder` (drop the field, migrate the fixtures), (3) the stale-prose
  and `GrinderAuditIT` sweep. The conventions ask for incremental change behind stable interfaces.
  **Honest counter-argument, recorded so this is not re-litigated as clear-cut:** deleting a type that two
  modules reference cannot leave a compiling intermediate *unless* the field is removed last, which is what
  the split above does — so the objection stands, but it is about reviewability rather than about the result.

- **MED-2 — `GrinderAuditIT`'s repair is a behaviour fix buried in a deletion commit.** The same 33-file
  commit changes the IT from reading the CSV's `Confidence` column and `HIGH` value to `Verdict`/`CONFIRMED`.
  That is not part of retiring a type — it is fixing a test that would have **failed against a live daemon
  while compiling perfectly**, which is precisely the "surface it explicitly, in its own commit" case. It was
  surfaced in the message, so this is a labelling failure, not a hidden one.

### LOW

- **LOW-1 — `DefaultBootRules` declares `private val bundled` and `fun bundled()`.** Legal Kotlin, but a
  property and a function of the same name in one object reads as a typo at the call site and gives no hint
  which is being invoked. `cached` / `bundled()` would say what each is.

### Not findings / positives (verified — do not re-litigate)

- **The pin boundary holds for all six `test(…)` commits on the verdict branch**, verified by checkout in a
  scratch worktree: `pin the four-state verdict`, `pin that extracting the ladder into rules changes no
  verdict`, `pin that metadata sideness is decided by rules too`, `pin the fold that replaces aggregateFor`,
  `pin that only a confirmation is published`, `pin that the report and CSV speak the four verdicts` — all
  RED at their own commit. All ten answering `feat`/`fix`/`refactor` commits are GREEN at theirs. The JEI and
  optional-dependency branches were verified the same way when they landed.
- **Both `refactor:` labels are honest.** *"the ladder reads its patterns from the rules file"* touches one
  main file and zero tests, with the 46 pre-existing classifier guards green — the equivalence evidence the
  extraction needed. *"one type for what a boot did, not two"* changes two test files, and its **entire** test
  diff is one method **rename**; no assertion, argument or expected value moved. That is the documented
  reference-only carve-out.
- **No new `!!` and no new `var`** in `Verdict.kt` or `BootRule.kt`.
- **The duplicated-`noteFor` hazard did not survive.** An editing slip inserted it twice and removed two
  neighbouring helpers; both were caught by the compiler and repaired before the commit. Exactly one
  definition exists.
- **Metadata cannot decide.** `noMetadataRuleCarriesAVerdict` fails the build if a `RuleSource.METADATA` rule
  ever carries a `verdict`, which is the guard that keeps stage 3's original short-circuit from returning.
- **`ERROR` never publishes**, pinned across twenty rows rather than one, because the failure mode is a flood.

### Recommendation

HIGH-1 and HIGH-2 are the same defect in two places and should be fixed together, in one `fix:` commit
preceded by its own red pin — the pin matters more than usual here, because HIGH-2 shows an existing guard
that *looks* like it covers the case. MED-1 and MED-2 are recorded for judgment, not repair: the branch is
unpushed, so re-cutting is available, but the result is correct and the messages are honest. LOW-1 is a
rename.

---

## Iteration 35 — re-audit after the iteration-34 fixes (2026-09-04)

**Scope:** the two commits answering iteration 34 — *"pin the two prevented-grind paths the redesign missed"*
and *"a grind that never ran is an Error, on every path"* — plus a re-check of the branch for anything
iteration 34 missed.

**Method:** every `BootOutcome` construction in `BootVerifier` traced by hand against `Verdict.ERROR`'s
definition; the grinder's thrown-verification path read end to end; full tree re-run with `--rerun-tasks`.

### HIGH — none

Both iteration-34 HIGHs are closed and verified structurally, not just by their own tests. `BootVerifier`
now has **five** `BootOutcome` constructions: four never-ran paths, all carrying `stagingPrevented = true`
(staging refusal, other-version re-stage refusal, thrown post-processor, `RunResult.NotStarted`), and one
for `RunResult.Completed` — a boot that actually ran — correctly not marked. There is no sixth.

### MEDIUM

- **MED-1 — the fix left two of its own references stale, which is the defect class the conventions single
  out.** Both were introduced *by* the fix commit:
  - `serverpackcreator-clientside/CLAUDE.md` still says `stagingPrevented` "is set at the staging-refusal
    sites". That was true before the fix and is now wrong in the direction that matters: a reader adding a
    new never-ran path would conclude the field is not their concern, which is exactly how HIGH-1 and
    HIGH-2 came to exist in the first place.
  - `BootVerifierRunPreparedTest.aThrownPostProcessorIsInconclusiveAndSkipsTheBoot` still asserts
    `BootResult.INCONCLUSIVE` (correct — the *result* is unchanged) but its **name** now describes the old
    verdict semantics. It passes, so nothing fails; a reader looking for "does a thrown hook produce an
    error?" would search this name and conclude the opposite of the truth.

### LOW — none new

LOW-1 is closed: `DefaultBootRules`' `private val bundled` is now `cached`, so the property no longer
shadows `fun bundled()`.

### Not findings / positives (verified — do not re-litigate)

- **A thrown verification publishes nothing at all.** `Grinder.grind` wraps `verifier.verify(candidate)` in
  `runCatching`, and a failure returns `GrindOutcome.FAILED` *before* any `GrindVerdict` is constructed — so
  no row reaches the store and nothing can reach `/as-properties`. This is the correct shape and is the
  reason the thrown path needed no `stagingPrevented` equivalent.
- **A prevented grind keeps no artifacts, and that is right rather than a contradiction of
  `Verdict.ERROR.keepsLogs`.** Both prevented paths return before `bootArtifactSink` fires, but there is no
  console to keep — nothing ran. `keepsLogs` governs whether evidence is *retained when it exists*; the
  operator-facing reason lives in `BootOutcome.detail`, which both paths set.
- **Retention stays consistent across the two independent expressions of one rule.**
  `BootArtifacts.worthKeeping(INCONCLUSIVE)` is `true` and `Verdict.ERROR.keepsLogs` is `true`, so a
  prevented grind that *did* produce output would keep it. `VerdictColumnTest.attemptRetentionAgreesWithVerdictRetention`
  is the drift guard.
- **The fix did not over-reach.** `aRealBootThatFailedIsNotMarkedPrevented` passed *before* the fix and
  still passes: a container that ran and crashed on a client-only class remains evidence. Trading a false
  INCONCLUSIVE for a lost true positive would have been the worse bargain, since those crashes are what the
  engine exists to find.
- **MED-1 and MED-2 of iteration 34 stand as recorded, not repaired.** The 33-file `Confidence` deletion and
  the `GrinderAuditIT` repair inside it are reviewability faults with a correct result and honest commit
  messages. Re-cutting is available (the branch is unpushed) but touches 33 files to change no behaviour,
  and the audit trail already carries the objection.

### Recommendation

MED-1's two stale references are a five-minute fix and should be taken: the module-doc line is the one that
actively misleads the next person to add a never-ran path, and that is precisely how this defect arose.

---

## Iteration 36 — third pass; the documentation contradicts itself (2026-09-04)

**Scope:** the branch after iterations 34 and 35 were actioned. **Method:** re-grep for every reference the
two fix rounds could have invalidated; re-derive the documented suite counts from
`build/test-results` rather than trusting them.

### HIGH — none

### MEDIUM

- **MED-1 — the root `CLAUDE.md` clientside row states two contradictory rules, one of which the redesign
  deliberately reversed.** The row carries, from 2026-09-01:

  > SURVIVED now yields LOW, ranked below `metadataClient` so a clean boot still cannot overturn a
  > client-only declaration

  and, from 2026-09-04, a few sentences later:

  > The console decides and the metadata only declares … a mod claiming **server** whose console reaches a
  > client-only class is CONFIRMED **client**

  The second sentence *replaced* the first — `aClientOnlyDeclarationNoLongerOutranksACleanBoot` pins that a
  clean boot with a CLIENT declaration is now `CLEAR`, the exact case the older sentence says is impossible.
  A reader reaching the older text first gets the pre-redesign precedence and no signal it is historical.
  **This is worse than an ordinary stale line**, because the file is loaded into every session and the two
  claims sit in the same table cell: whichever is read first looks current.

### LOW

- **LOW-1 — the documented clientside suite count is stale.** The row says 309; the tree reports **313**
  (the four `PreventedGrindTest` guards added by the iteration-34 fix). The convention the count itself
  carries — *re-derive it from `build/test-results`, do not trust the sentence* — is what caught it, and
  it went stale within one commit of being written.

### Not findings / positives (verified — do not re-litigate)

- No source or test still references `staging-refusal sites` or the old
  `aThrownPostProcessorIsInconclusive…` name; iteration 35's MED-1 is fully closed.
- The grinder row's count (455) is accurate.
- The clientside row's older entries that the redesign did **not** invalidate are correct as written and
  should stay: the fair-run principle, "an excuse may never outrank decisive client-only evidence" (now
  enforced by file order rather than by rung order, which the newer text says), the `clientOnlyClassMarker`
  Fabric-intermediary gap, and the JEI descriptor-gate entry.

### Recommendation

Both are documentation-only. MED-1 should be fixed by marking the superseded sentence as history rather than
deleting it — the measured rows behind it (`better-stats`, `tcdcommons`, `yacl`) are still the evidence for
why a clean boot is worth recording at all, and that reasoning survives the change in what it is recorded
*as*.

---

## Iteration 37 — the four fixes that followed the redesign (2026-09-04)

**Scope:** everything since iteration 36 — the LWJGL/invalid-dist promotion, the bundled-dependency fix,
client-only proof crossing loaders, and the version-metadata race. **Method:** code read rather than commit
messages; pin boundaries verified by checkout; lifetimes and concurrency reasoned through against the
grinder's actual runtime (a daemon running for weeks, not a CLI invocation).

### HIGH

- **HIGH-1 — `BundledJars` leaks a JVM-lifetime registration per nested jar, per scan.**
  `idsIn` spools each declared nested jar with
  `File.createTempFile("spc-nested-", ".jar").apply { deleteOnExit() }` and deletes it in a `finally`. The
  **`deleteOnExit()` is the leak**: it adds the path to `java.io.DeleteOnExitHook`'s static `LinkedHashSet`,
  which never shrinks — deleting the file does not deregister it. The `finally` frees the disk and nothing
  frees the set.
  **Why it matters here specifically:** this is called from `stageManifestDependencies`, i.e. per staged jar,
  per boot attempt (three per candidate), for every candidate in a catalog sweep. `sodium` alone declares
  nine nested jars. A daemon designed to run for weeks accumulates one dead `String` per nested jar per
  attempt, plus a shutdown hook that eventually walks tens of thousands of already-deleted paths.
  **And the temp file is not needed at all.** Only the nested descriptor is read; a `ZipInputStream` over the
  entry's stream gets it without touching disk, which removes both the leak and the I/O.

### MEDIUM

- **MED-1 — `propagateClientOnlyProof` overwrites `Verdict.ERROR`, erasing an operator signal.** It copies
  `verdict = Verdict.CONFIRMED` onto *every* non-proof verdict, including a loader whose grind was
  **prevented** — no runtime image, a staging refusal, a failed overlay. Publishing that loader's entry is
  right (the mod is client-only, and the entry comes from platform metadata rather than from the boot), but
  the ERROR disappears from the report, so a host defect stops being visible on exactly the projects where a
  proof happens to exist. `ERROR` exists to be actionable; it should survive in the note even when the
  verdict is superseded.

- **MED-2 — `VersionMeta.update()` is unsynchronised, so two refreshes can interleave field generations.**
  The snapshot fix makes each field internally consistent, but nothing serialises `update()` itself:
  `refreshManifests()` runs on `refreshScope`, and `update()` is public and callable by the app and the
  grinder. Two overlapping runs can leave `releases` from generation A beside `meta` from generation B.
  Each is a complete list, so nothing tears — but a lookup can miss a version the release list contains.
  Strictly narrower than the bug just fixed, and the same class.

### LOW

- **LOW-1 — the immutability pin can pass vacuously.** `theReleaseListHandedToCallersIsNotLiveState` and
  `noMetaHandsOutLiveState` both guard the assertion with `if (asMutable != null)`. Today the cast always
  succeeds (`Collections.unmodifiableList` presents as `MutableList` to Kotlin), so the guard never skips —
  but if an accessor ever returned something that failed the cast, the test would report success while
  asserting nothing. A guard that can silently assert nothing is the defect class iteration 34 already found
  once.

### Not findings / positives (verified — do not re-litigate)

- **Concern separation is clean across all six non-merge commits**: every one is either wholly test or
  wholly main, with no mixed commit in the range.
- **All three pin boundaries hold**, verified by checking each `test(...)` commit out and running its
  module: `pin that client-only proof is about the mod`, `pin that version metadata is not handed out as
  live state`, and `pin that no version meta hands out live state` are each RED at their own commit.
- **The `iron-chests` guard survived the client-only change.** An unexplained crash is still disprovable by
  another loader's clean boot; only `provesClientOnly` rungs are exempt, and
  `anUnexplainedCrashIsStillDisprovedByAnotherLoader` pins it.
- **`OPERATOR_RULE` correctly does not propagate** despite being `decisive` — a rule reaching CRASHED states
  that *this console* is a crash, not that the mod is client-only.
- **Only declared nested jars count** in `BundledJars`; a stray file under `META-INF/jars/` is not treated
  as bundled, which is the direction where generosity would skip staging something genuinely needed.
- **The three narrowed published signatures are recorded** in `API-BEHAVIOUR-CHANGES.md` with the reason.

### Recommendation

HIGH-1 first, and fix it by removing the temp file rather than by removing `deleteOnExit` — the spool is
avoidable work on the hot staging path. MED-1 is a two-line change to preserve the error text. MED-2 wants a
lock around `update()`. LOW-1 is a one-line strengthening.

---

## Iteration 38 — auditing iteration 37's own fixes (2026-09-04)

**Scope:** the commit answering iteration 37, plus the four merges it repaired. **Method:** each fix traced
to the *path that motivated it* rather than to the symbol it changed; the streamed reader verified against a
real nine-nested-jar artifact.

### HIGH

- **HIGH-1 — MED-2's fix does not cover the case it was written for.** `VersionMeta.update()` was marked
  `@Synchronized`, but the background refresh does **not** go through it: `refreshManifests()` calls
  `minecraft.update()`, `fabric.update()`, `forge.update()` and the rest **directly**. So the lock guards the
  public caller and leaves the coroutine — the path the finding was about — entirely unguarded, and two
  refreshes can still interleave field generations.
  This is iteration 34's HIGH-2 shape exactly: a guard that looks like it covers a case and cannot reach it.
  Both are instance methods of `VersionMeta`, so marking `refreshManifests()` `@Synchronized` puts them on
  the same monitor and actually serialises them.

### MEDIUM

- **MED-1 — none of the last four fixes is documented in a module `CLAUDE.md`.** `BundledJars`,
  `BootDecision.provesClientOnly`, the `lwjgl-on-a-dedicated-server` / `fml-invalid-dist` defaults, and the
  version-metadata snapshot rule appear in commit messages and nowhere a session will load. Four landmines
  a reader is expected to respect — *only declared nested jars count*, *client-only proof crosses loaders*,
  *the parser is not the bug*, *never hand out live metadata* — exist only in history. The repo's own
  convention is that durable facts live in the module files precisely because commit messages are not read
  before touching code.

### LOW

- **LOW-1 — two new `!!` in `VersionMetaRefreshRaceTest`.** Introduced while removing the vacuous-pass
  guard: `assertNotNull(asMutable)` followed by `asMutable!!.clear()`. Correct, but the conventions ask for
  no new `!!`, and `requireNotNull` returns the narrowed value in one step.

### Not findings / positives (verified — do not re-litigate)

- **The streamed nested-jar reader works on a real multi-nested artifact.** Run against the live
  `sodium` Fabric jar, `BundledJars.idsIn` returns all **nine** declared ids —
  `fabric-api-base`, `fabric-block-getter-api-v2`, `fabric-lifecycle-events-v1`, `fabric-renderer-api-v1`,
  `fabric-rendering-fluids-v1`, `fabric-rendering-v1`, `fabric-resource-loader-v0`,
  `fabric-resource-loader-v1`, `fabric-transitive-access-wideners-v1`. `ZipInputStream` positioned at an
  entry bounds the read correctly, so `readTree` does not run past it. The temp file and its
  `deleteOnExit` registration are gone.
- **That result also shows the bundled-dependency fix has real breadth**: those nine are exactly the Fabric
  API modules this repo documents as the most-commonly-missing dependency class, so a mod shipping its own
  copies no longer drags the whole of Fabric API into staging.
- **MED-1 of iteration 37 is correctly narrow** — a superseded `ERROR` keeps its reason in the note and
  still publishes its entry, which is right: the entry comes from platform metadata, not from the boot.
- **No mixed-concern commit** in the range; the fix commit is main-only and its pins pre-date it.

### Recommendation

HIGH-1 is one annotation and must be taken — the finding it answers is otherwise still open while looking
closed, which is worse than never having fixed it. MED-1 is the documentation pass the four fixes never got.
LOW-1 is two lines.
