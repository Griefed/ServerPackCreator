# Analysis audit — accumulating evidence log

> Appended to, never overwritten. Each section is dated. Measurements, mutation results and
> "verified clean, do not re-litigate" lists stay valid and must survive.

---

## 2026-09-05 — test depth & coverage: `-clientside` (368) and `-grinder` (490)

Read-only. Both modules on `develop` with the day's audit fixes merged. Lens requested: guards asserting a
unit no caller reaches, tests constructing the value under test, and partially-pinned ordering invariants.

**No HIGH findings.** The two defects of that class found earlier today (`LoaderCache.isInstalled`
unreachable, `bundledPattern` failing silently) are fixed and pinned; the sweep below found no third.

### MEDIUM

**M-1 — `Grinder.grind`'s 18-field mapping is asserted five fields deep.**
`serverpackcreator-grinder/src/main/kotlin/.../Grinder.kt` (the `store.record(GrindVerdict(...))` block)
copies **18** fields out of `LoaderVerdict` by hand. `GrinderTest` — the only test that drives `grind` and
inspects what was recorded — asserts `bootedLoader`, `declaredClientSide`, `declaredServerSide`, `jarScan`
and `suggestedEntry`. Unasserted through the mapping: **`verdict`, `declared`, `firedRule`, `decidedBy`,
`stagedDependencies`, `filenamePattern`, `detail`**.

Every report, CSV, query and filter test builds its `GrindVerdict` directly through the `grindVerdict(...)`
helper, so none of them can see a producer that fills a field wrongly. This is exactly the shape that let
`unsatisfiedLabel` pass its tests while both platforms fed it the wrong `slug`.

*Failure scenario:* write `filenamePattern = verdict.suggestedEntry`, or swap `declared` for
`declaredServerSide`, and the whole suite stays green while `/as-properties` and the report table publish
the wrong column. `verdict` is the worst of them — it is the field the publication gate reads.

*Suggested test:* a fake `CandidateVerifier` returning one `ClientsideReport` whose `LoaderVerdict` carries
a **distinct sentinel in all 18 fields**; run `Grinder.grind`; assert the recorded `GrindVerdict` carries
each sentinel in the matching field. The technique already exists one layer up in
`everyColumnRendersTheValueItsHeaderNames`, which gives each column its own `SENTINELx` precisely so an
off-by-one shows up — the same argument applies to the mapping that feeds it.

**M-2 — `pickDependencyFile`'s arms 2 and 3 are unpinned against each other.**
`serverpackcreator-clientside/src/main/kotlin/.../BootCandidateSelector.kt`, the four-arm preference:

    pickFrom(satisfying.filterNot { locked })   // 1
      ?: pickFrom(files.filterNot { locked })   // 2  obtainable, constraint violated
      ?: pickFrom(satisfying)                   // 3  locked, constraint satisfied
      ?: pickFrom(files)                        // 4

Covered today: locked-vs-obtainable *without* a constraint (arms 1/2 over 3/4), obtainability over the
exact loader match, and all-locked still returning a file (arm 4). **The interaction is not covered** —
no test has a locked file that *satisfies* the constraint competing with an unlocked file that *violates*
it, which is the only case separating arm 2 from arm 3.

*Failure scenario:* swap arms 2 and 3 and the suite stays green, while the selector again prefers an
unobtainable file — reintroducing the `306612` / Fabric-API refusal fixed on 2026-09-04, whose symptom is
an `ERROR` verdict overwriting a decisive one.

*Suggested test:* `lockedFile("lib-1.5.jar", …)` satisfying `>=1.0` beside an obtainable `lib-0.9.jar`;
assert `lib-0.9.jar` is picked, with the reason in the message.

### LOW

**L-1 — `FilenameStemDeriver.deriveStems` (plural) has no production caller anywhere in the repo.**
`serverpackcreator-clientside/src/main/kotlin/.../FilenameStemDeriver.kt:75`. Production uses the singular
`deriveStem` at three sites (`ClientsideVerifier` twice, `BootVerifier` once). One test exercises the
plural form. `ClientsideVerifier`'s KDoc then *cites* it — "`embeddium-` (Forge/NeoForge) versus
`sodium-fabric-` … is the one `FilenameStemDeriver.deriveStems` documents" — so a landmine explanation
points at code nothing runs.

Same species as the grinder's `FallbackPropertiesRenderer.decisive()` removed today: dead surface that
reads as load-bearing because a comment vouches for it. Either delete it and retarget the citation to
`deriveStem`, or wire it if the per-loader map is genuinely wanted.

**L-2 — one of 21 commits mixed `src/main` with `src/test`.**
`0e6eb1b33 fix(grinder): fall back when a knob is unusable…` also edited
`GrinderConfigurationTest.everyVariableReadIsDeclaredAsAKnob`. Disclosed in the commit body. The edit added
three names to a **source-scanning guard's alphabet** — mechanism, not expectation, with every assertion
unchanged — so it sits inside the "reference-only update is not the stop-and-flag signal" carve-out, but it
is the kind of thing that should be stated rather than assumed. The other 20 commits keep test, fix, refactor
and docs strictly separate, with every `fix:` preceded by its own red `test:`.

### Verified clean — do not re-litigate

- **CurseForge API key never leaves the header.** It appears only as a constructor parameter, in two KDoc
  `@param` lines, and in the `headers` map. It is never interpolated into a URL, an exception message or a
  log statement. The `key` matches inside log lines are `partition.key` (a crawl token) and `attemptKey`.
- **No unused-import warnings** in either module (`compileKotlin --rerun-tasks`).
- **Zero dangling KDoc blocks** in both modules — 6 fixed in `-grinder`, 7 in `-clientside`, detector re-run
  to zero. Two adjacent blocks mean Kotlin binds only the second, so this class of defect is silent.
- **`VerdictStore.hasVerdictFor` is not a gap.** 12 test references and no production caller, which matches
  the `isInstalled` shape — but its own KDoc states that `Grinder.grind` uses `newestVerification` instead,
  and that path *is* separately pinned: cross-platform isolation, the freshest-across-loaders case, the
  project-rename identity case, and survival across a store reopen. The predicate is an additional readable
  convenience, not a substitute. Checked and dismissed.
- **`missingRuleIds()` and `trackedWorkerCount()`** are documented test seams, not orphans.
- **Ladder ordering is complete and has teeth**: all 16 rungs pinned in `theGuardOrderIsPinnedAsAWhole`,
  mutation-verified today (hoisting `mixin-apply-failure` above `client-only-class` fails).
- **`BootLogStore.isInsideStore`** rejects traversal by name shape, `.`/`..`, and canonical parent.
  **`VerdictReportRenderer.esc`** escapes `&` first, then `<>"'`, on every cell including the `href`.

### Method note

The orphan sweep was run **repo-wide**, not per module: a first pass scoped to one module's `src/main`
reported 17 false positives in `-clientside` alone, because `ClientsideListEditor`, `BootArtifacts` and
`AttemptDirectory` are called from `-app` and `-grinder`, and because method references (`::current`,
`cache::evictUnusedSince`) are not call syntax. Counting calls, `::` references and `override` declarations
across every module's `src/main` reduced 30 candidates to 4, of which 2 were documented seams and 1 was
dismissed above.

### Resolution — 2026-09-05, same day

All four findings closed. Branch `claude-audit-followups`.

**M-1 — closed.** `RecordedVerdictMappingTest` drives `Grinder.grind` with a **distinct sentinel in every
one of the eighteen mapped fields** and asserts each arrives in the recorded `GrindVerdict`. Green when
written, so mutation-verified rather than trusted — both predicted mis-wirings now fail:

    filenamePattern = verdict.suggestedEntry        -> "'SENTINEL_FILENAME' was dropped by the mapping"
    declaredClientSide = verdict.declaredServerSide -> "expected: <REQUIRED> but was: <UNSUPPORTED>"

Distinctness is the mechanism, not decoration: equal values cannot detect a swap, so the two
`DeclaredSupport` fields deliberately take different constants and no two enum sentinels share a name.

**M-2 — closed.** Two tests in `BootCandidateSelectorTest` separate preference arms 2 and 3.
`anObtainableFileBeatsALockedOneThatSatisfiesTheConstraint` pins that obtainability outranks the version
constraint; `betweenTwoObtainableFilesTheConstraintStillDecides` is the counterweight that keeps
obtainability a *preference* rather than an override. Mutation-verified: swapping the two arms fails with
`expected: <lib-0.9.0.jar> but was: <lib-1.5.0.jar>`.

**L-1 — closed.** `FilenameStemDeriver.deriveStems` deleted along with its one test. Its KDoc carried the
`sodium-fabric-` versus `embeddium-` example that two other files cite as authoritative, so that moved onto
`deriveStem` — the function that actually produces those stems — with the consequence now stated outright:
the divergence is *why* `loaderDisprovingTheCrash` compares entries rather than loaders. Both citations
retargeted; no `deriveStems` reference remains in the repo.

**L-2 — nothing to fix.** A historical commit-hygiene note, disclosed in the offending commit's own body at
the time. Recorded, not actionable.

### `REFACTOR-AUDIT.md` — swept the same day, four candidates, all already closed

Checked against the code rather than from memory, so they are not re-litigated:

| Item | Status |
|---|---|
| iter 33 OBS-1 — QSL module ids unmapped | **closed** — `ModIdRegistry.qslModulePattern = ^quilt_[a-z0-9_]+$` → `qsl`/`634179` |
| iter 38 LOW-1 — two new `!!` in `VersionMetaRefreshRaceTest` | **closed** — zero `!!` in that file |
| iter 39 LOW-1 — two Minecraft snapshot accessors unpinned | **closed** — `noMetaHandsOutLiveState` lists `clientSnapshots` and `serverSnapshots`, with a comment saying the set is deliberately *every* list accessor |
| iter 40 HIGH-1 — the step-down pin never reached the change | **closed** — source-level wiring guard added and mutation-verified |

Iteration 34's MED-1/MED-2 (the 33-file `Confidence` deletion commit's shape) remain as recorded history:
that commit was later re-cut, and the audit entry is the remedy the conventions prescribe for a shape found
after the fact.

Suites from clean (`--rerun-tasks`): api **405** (1 skipped), clientside **369**, grinder **495**
(29 skipped), app **149**.

# Analysis — 2026-09-06, `claude-grinder-plugin`

Scope: `serverpackcreator-plugin-grinder` (all of it), `VerdictsJsonEndpointTest` + the `/verdicts.json`
route, and `ExtensionScopingTest` + the `ApiPlugins` fix. Test depth, edge cases, latent bugs, security.
Companion to the same day's entry in `REFACTOR-AUDIT.md`; findings shared by both are cross-referenced
rather than restated.

## Coverage map — what has a guard and what does not

| Unit | Guards | Verdict |
|---|---|---|
| `GrinderUrl` | 5 | good — normalisation, scheme assumption, rejection, endpoint derivation |
| `GrinderClient` | 8 | good on failure modes, **blind to the request path** (A-4) |
| `SelectionStore` | 8 | good — including a damaged config and the shipped `config.toml` itself |
| `ClientsideEntryInjector` | 7 | good — the hardest-pinned unit, correctly so |
| `GrinderPreGenExtension` | 5 | good — pins the real `run` signature, incl. idempotence |
| `VerdictTableModel` | 10 | good |
| **`GrinderTab.onSelectionChanged`** | **0** | **logic, not rendering — and wrong (A-1)** |
| **`VerdictListPane.shownEntries`** | **0** | feeds A-1's decision; untested |
| **`DashboardPane.duration` / `text`** | **0** | pure functions with real boundaries |
| `GrinderTabExtension` | 0 | metadata + factory; `GrinderPreGenExtension` has `identifiesItself`, this does not |
| `SettingsPane` | 0 | wiring, acceptable — except dead `isUsable` |

## Potential bugs

- **A-1 (MEDIUM) — `gui/GrinderTab.kt:181`, `onSelectionChanged` mis-files every stale entry.**
  Full analysis in `REFACTOR-AUDIT.md` MED-1. In short: `partition { it in shownInOther }` sends an entry
  shown in *neither* pane to `CONFIRMED`, and the module's own deliberate never-prune rule guarantees such
  entries exist. Every tick migrates the user's at-your-own-risk selections into the proven list. Server
  packs are unaffected (`allSelected()` is the union); the risk record is destroyed.
  *Suggested tests:* an entry in neither pane keeps its stored pane; an entry shown in Other is filed under
  Other; an entry shown in Confirmed is filed under Confirmed; *Select all* on an empty pane changes nothing.

- **A-2 (MEDIUM, security) — grinder text reaches HTML-interpreting Swing components.**
  Full analysis in `REFACTOR-AUDIT.md` MED-2, with the measured three-row table. `slug` and `detail` come
  from mod metadata; the daemon is unauthenticated. Swing's HTML subset fetches remote images, so a crafted
  mod name makes an SPC GUI issue outbound requests. *Suggested tests:* a verdict whose `slug` begins with
  `<html>` renders as literal text (assert the renderer installs no HTML view, i.e. the `"html"` client
  property stays null); same for a `/status` rule-error string.

- **A-3 (MEDIUM) — `core/GrinderClient.kt:141`, a wrong-shaped 200 reads as "no verdicts".**
  `readVerdicts` returns `emptyList()` when the document is neither an array nor an object carrying a
  `verdicts` array. A proxy or a future daemon answering `{"error":"…"}` with a 200 therefore produces
  `FetchResult.Ok(emptyList())`, and the tab reports *"0 confirmed, 0 other verdicts"* — indistinguishable
  from a grinder that has genuinely ground nothing. The existing guard
  `reportsAnUnparseableBodyRatherThanReturningNothingFound` names exactly this hazard but only covers
  non-JSON, so the case it is named for is the case it misses. *Suggested test:* a 200 carrying
  `{"error":"nope"}` yields `Failed`.

- **A-4 (MEDIUM) — `core/GrinderClientTest.kt:44`, the fixture answers every path.**
  See `REFACTOR-AUDIT.md` MED-4. No guard observes which URL the client requests. *Suggested test:* record
  `exchange.requestURI.path` in the fixture and assert `/verdicts.json` and `/status`.

- **A-5 — WITHDRAWN, not a bug.** The claim was that `ClientsideEntryInjector`'s `lowercase()` is
  locale-sensitive. It is not: Kotlin's `lowercase()` exists (since 1.5) *because* `toLowerCase()` is
  locale-sensitive, and compiles to `toLowerCase(Locale.ROOT)`. Measured under a Turkish default locale —
  Java `toLowerCase()` → `ıceberg-`, Kotlin `lowercase()` → `iceberg-`. The guard written for it was green
  on its first run, which is how the finding was caught; it is kept as a regression pin against the
  property being lost to `lowercase(Locale.getDefault())` or to Java interop. Full note in the same day's
  `REFACTOR-AUDIT.md` entry.

- **A-6 (LOW) — `gui/GrinderTab.kt:78`, the dashboard `Timer` has no owner.** Polls for the life of the
  JVM regardless of whether the tab is on screen. See LOW-6.

## Missing edge cases in existing guards

- `SelectionStoreTest.boundsThePollInterval` covers `0` and `9_999` but **not a negative** value, which is
  the one that would reach `javax.swing.Timer` and throw `IllegalArgumentException` if the clamp were
  removed from one side only.
- `VerdictsJsonEndpointTest` asserts `page`/`pages` only for the unpaged case. Nothing exercises
  `?size=1&page=2` on the JSON route, so the paging metadata is pinned only where it is trivially `1`.
- `VerdictTableModelTest` does not assert `getColumnClass` for a non-tick column (it pins only the Boolean
  one), so a change making every column Boolean would pass.
- `GrinderClientTest` has no guard for a body that is a bare **array** — a shape `readVerdicts` explicitly
  supports and documents. The branch is unreachable from the tests.
- `ExtensionScopingTest` does not pin the extension-run multiplication. See MED-3.

## Shallow or redundant

- Nothing redundant found. `GrinderUrlTest.derivesTheEndpointsFromTheBase` is *shallow in isolation* —
  string equality on a helper — but that is only a weakness because A-4 leaves it unjoined to the client.

## Error handling

Good, and deliberately so: `GrinderClient` converts every failure into `FetchResult.Failed`, restores the
interrupt flag (a `SwingWorker` cancels by interrupting), and `SelectionStore` tolerates every damaged
value because it is constructed on the generation path. `GrinderPreGenExtension` no-ops on an absent
config rather than aborting a server pack. The one gap is A-3, where a failure is reported as a success.

## Security

- **A-2 is the finding.** Everything else checked clean:
- `VerdictListPane.applyFilter` wraps free text in `Pattern.quote`, so a filter cannot be a regex injection
  or a `PatternSyntaxException`.
- No secret is read, written, or logged. `config.toml` holds a URL and two string lists.
- The plugin issues only GETs, to a URL the user typed, restricted to `http`/`https` by `GrinderUrl`
  (`file:`, `ftp:` and a schemeless-but-colon-bearing string are all rejected).
- Fetched entries become exclusion patterns, and a malformed one is already inert —
  `ModListCompiler.FilterMatcher` compiles up front and logs-and-skips. Nothing here executes grinder text.
- The Settings pane states in the UI that the report server is unauthenticated, which is the honest
  mitigation for a design the daemon owns.

## Consistency with existing patterns

Followed correctly: loopback-`HttpServer` guards over mocks (the grinder's own `ReportServerTest` idiom);
tables' rendering left untested while their model is not (the frontend's stated stance); the shipped
`config.toml` parsed by SPC's own `TomlParser` in its own guard; `pluginArtifact` consumable configuration
rather than reaching into another project.
One inconsistency: `GrinderPreGenExtension` has an `identifiesItself` guard, `GrinderTabExtension` has none.

## Documentation & imports

- `gui/GrinderTab.kt` imports `com.fasterxml.jackson.databind.JsonNode` and never uses it. Sole unused
  import in the module (checked all 13 files).
- `gui/SettingsPane.kt:124` `isUsable` is dead code.
- Doc comments are present on every declaration including private ones, per the project convention; spot
  checks found none restating its signature.

## Suites at the time of this analysis

api **407** (1 skipped), grinder **501** (29 skipped), plugin-grinder **44**, app **149**,
clientside **369**, plugin-example **3**. Zero failures.

## Resolution

A-1, A-2 and A-3 fixed; A-4 and A-6 fixed; A-5 withdrawn. Every "missing edge case" listed above now has
a guard: the negative poll interval, the JSON route's paging and its clamp past the end, a non-tick
column's class, a bare-array response, an empty-but-valid response, and `GrinderTabExtension`'s identity.

The three units the coverage map called untested are now extracted and pinned: `SelectionAttribution`
(7), `PlainTextRendering` (4), `StatusFormatting` (7). plugin-grinder 44 → **69** tests.

`GrinderTab`, `VerdictListPane`, `DashboardPane` and `SettingsPane` remain without guards **by design** —
what is left in them after the extractions is layout and wiring, which is this project's standing stance
on view code. What was *not* layout has been moved out of them, which was the finding.


---

# 2026-09-08 — test depth over `300a4aae6^1..HEAD`: the backtrack, the storm fix, and one latent repeat

Scope: the code changed by the 2026-09-06 field reports (`LoaderCompatibility`, the Connector-placeholder
redirect, `DependencyBacktrack`) and by the 2026-09-07/08 fixes to what the third of those did live
(`VersionConstraint.readableVersion`, `UnmetReason` + `refuseForMissingDependencies` + `backtrackReason`,
`BundledJars.versionsIn`, `BootVerifier.nestedVersions`). Companion to the same-day section in
`REFACTOR-AUDIT.md`, which carries the per-commit red/green verification; this one is about depth, edges and
bugs. READ-ONLY pass.

## HIGH

**A-1 — `numbersOf` still turns a value it cannot represent into `0`, and `readableVersion` now waves it
through.** This is the defect just fixed (`Balm 26.2.0.7` → `[0, 2, 0, 7]`) reached by a second door:

```kotlin
.map { component -> component.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
```

`readableVersion` admits any component that is **all digits**, but `toIntOrNull` returns `null` above
`Int.MAX_VALUE` (10 digits), and `?: 0` then makes that component **zero** rather than unreadable. A version
carrying a date or a CI counter — `1.20260908120000`, `2026.9.8.120000` — therefore compares as though its
largest component were `0`, i.e. below almost any bound, which is exactly the shape that manufactured 47
verdicts. It is latent rather than live: no version in the current corpus was observed hitting it, and the
failure mode is a *demotion*, not a wrong sideness verdict.

The direction of the fix is the one already chosen for this class: a component that is all digits but does
not parse is **not readable**, so `satisfies` accepts and nothing is judged on a number we could not hold.
That is a one-predicate change in `readableVersion` plus a boundary test; widening `numbersOf` to `Long`
would only move the ceiling.

## MEDIUM

**A-2 — the two ambiguity rules introduced by `293998273` are asserted nowhere.** `BundledJars.versionsIn`
drops an id one jar bundles at two versions; `BootVerifier.nestedVersions` drops one that two staged jars
bundle differently. Both are documented as deliberate "fail toward proceeding" safety properties, and
`grep -rl "versionsIn\|nestedVersions" src/test` matches **nothing** — only the end-to-end happy path
exercises `versionsIn` at all, implicitly. Mutating either fold to keep the first value seen would pass the
whole suite.

**A-3 — the precedence rule is mutation-invisible.** `nestedVersions(stagedJars) + scanned…toMap()` relies on
`Map.plus` letting the right operand win, so a top-level jar beats a bundled copy of the same id. Swapping
the operands changes which build a demotion acts on and **no test fails**. The rule is stated in a comment
and in the module CLAUDE.md; it needs a guard, because the comment is the only thing holding it.

**A-4 — `backtrackReason` has no direct test.** The predicate that separates "this project publishes nothing
usable" from "we excluded all of it" is reachable only through two staging paths, and only one of its three
branches (`excluded` empty → short-circuit; excluded but a pick still exists; excluded and the pick only
existed unfiltered) is exercised. It is a pure function taking a `ProjectFiles` and a `Set<String>` — unit
territory, and this module's own preference is to pin pure decisions directly.

**A-5 — two of the five `UnmetReason` values never travel the staging path.** `DISTRIBUTION_LOCKED` and
`UNRESOLVED` are asserted only where the refusal string is *rendered* (`DependencyLabelTest`), not where the
reason is *chosen*. The choice sites are one `if (dependencyFile.locked)` and one `?: UnmetReason.UNRESOLVED`
in `downloadWithDependencies`; inverting either is invisible to the suite. `UnmetDependencyReasonTest` covers
the three that were previously indistinguishable, which is where the value was — but the vocabulary is now
five wide and only three deep.

## LOW

**A-6 — `explain()` returns `String?` and is interpolated straight into two log lines.** In
`downloadWithDependencies` and `stageManifestDependencies` the reason is logged as
`"…: ${reason.explain(platform.name)}."`. Only `UNRESOLVED` returns `null`, and neither site can reach it
today — but the sites are one refactor away from printing the literal `null.`, and the compiler will not say
so. Either make the log sites take the non-null branch explicitly or give `UNRESOLVED` a string of its own.

**A-7 — a shallow assertion in a new test.** `NestedDependencyConflictTest.aSatisfiedNestedRequirementDemotesNothing`
asserts `stagedMods(workDir).contains("create-6.0.10.jar")`. Its sibling asserts the exact set, which is the
stronger form and the one that would catch an unrelated jar going missing.

**A-8 — `nestedVersions` re-opens every staged jar on every backtrack round.** Up to `MAX_BACKTRACKS` (10)
rounds × every staged jar, each a `ZipFile` open plus a nested-entry read. Noted, **not** recommended for
optimisation: each round already re-downloads the whole pack, so this is far from the cost centre, and this
module's standing rule is to measure before changing a bound. Recorded so the next reader does not have to
re-derive that it was considered.

## Not findings (verified — do not re-litigate)

- **`backtrackReason` ignoring `requirement.versionConstraint` in the manifest path is correct.**
  `pickDependencyFile` treats a constraint as a preference with a fall-back arm over the whole set, so its
  *null-ness* is identical with and without one. Checked arm by arm; the reason classification cannot differ.
- **`idsIn` is behaviour-identical after the `nestedDescriptorsOf` extraction.** Both failure directions
  match the original: an absent/unreadable outer descriptor yields nothing (early return), and a nested jar
  that cannot be parsed is dropped rather than failing the jar (`mapNotNull` where the original used
  `getOrDefault(emptySet())`). `BundledJarDependencyTest`'s eight assertions are unchanged and green.
- **No new compiler warnings, no unused imports.** `--rerun-tasks` on `:serverpackcreator-clientside:
  compileKotlin` and `compileTestKotlin` emits nothing for this module; every warning in the build belongs to
  `-api` (`Locale` constructors, nightconfig `valueMap`) and predates this range.
- **No security surface.** No new I/O sink, no string-built query, no secret handled in production code. The
  operator probe `misc/cf-dependency-probe.sh` reads `CURSEFORGE_API_KEY` from the environment, passes it only
  as a header, and prints its *length* rather than its value; it is untracked by design.
- **No new concurrency.** Everything added is pure or per-attempt, inside the existing
  `(platform, slug, loader)` staging directory ownership; nothing new is shared between grind workers.
- **Verdict-store impact is bounded and self-healing.** Nothing here writes verdicts; the 47 wrong `ERROR`
  rows are overwritten as their 30-day TTL turns them over, and `ERROR` does not reach `/as-properties`.

## Suggested tests (specific)

1. `readableVersion` boundary table: `v2.1`, `V2.1`, `v` alone, `1..2`, `1.`, `1.0.0+build-1`, `1.20.1-rc1`,
   and an over-`Int.MAX_VALUE` component — the last one red until A-1 is fixed.
2. `BundledJars.versionsIn`: version read from a nested descriptor; a nested mod with no `version` omitted
   while `idsIn` still reports it; the same id at two versions in one jar dropped; the same id at the *same*
   version kept; the Quilt spelling (`quilt_loader.version`); an unreadable jar yielding an empty map.
3. `nestedVersions` precedence: a top-level staged jar's version wins over a bundled copy of the same id
   (kills the operand swap), and two jars bundling different versions of one id produce no conflict.
4. `backtrackReason`: all three branches, directly.
5. Staging-level `DISTRIBUTION_LOCKED` (a dependency file with `downloadUrl = null`) and `UNRESOLVED`
   (a ref the platform does not carry) reaching the published refusal with the right reason.
6. Tighten `aSatisfiedNestedRequirementDemotesNothing` to an exact-set assertion.

## Suites at the time of this analysis

clientside **419** (0 failures), grinder **503** (0 failures, 29 skipped), app **149** (0 failures) — the
latter two re-measured at 2026-09-08 07:31/07:32, i.e. *after* `293998273`, because the merge message had
quoted figures taken before it (REFACTOR-AUDIT L-4). They hold.

## Resolution — every finding closed the same day (2026-09-08)

| Finding | Closed by | How |
|---|---|---|
| A-1 | `9fbfffa75` (red), `60a0e77c8` (fix) | `readableVersion` asks `toIntOrNull() != null` rather than "all digits" — the question `numbersOf` actually needs answered. Chosen over widening to `Long`, which moves the ceiling instead of closing the gap |
| A-2 | `89ae3d8ca`, `838f35ad5` | `BundledVersionTest`: both ambiguity levels, the `provides` alias, the no-version split, the Quilt spelling, unreadable input |
| A-3 | `89ae3d8ca` | The precedence guard, **mutation-verified**: swapping `nestedVersions(...) + scanned…` makes it fail with `create-6.0.8.jar` in place of `create-6.0.10.jar`, and nothing else in the suite notices |
| A-4 | `89ae3d8ca` | `backtrackReason`'s three branches asserted directly, with its precondition stated — one first draft asked about a state the function is never called in, which is recorded in the test rather than silently dropped |
| A-5 | `89ae3d8ca` | DISTRIBUTION_LOCKED and UNRESOLVED reached through real staging. Writing it exposed a **faithless fake**: the test downloader ignored `downloadUrl`, so a locked file "downloaded" and the case came back as a backtrack. It now mirrors `HttpJarDownloader`'s first line |
| A-6 | `9b58349f4` | `explain()` always returns a sentence; whether to append it is `worthAppending`, in the renderer. Published strings byte-identical |
| A-7 | `89ae3d8ca` | Exact-set assertion, and the duplicated jar builder in that file replaced by one parameterised harness |
| A-8 | noted, deliberately not changed | Re-opening each staged jar per backtrack round is far from the cost centre — each round already re-downloads the pack — and this module measures before changing a bound |

**Two of the new guards were red first, and both times the fault was in the test.** That is the argument for
the convention that says run a pin before committing it: a fake more capable than the real thing, and a
question the function is never asked, both look exactly like a code defect until the failure text is read.

Suites at close: clientside **438**, grinder **503** (29 skipped), app **149**, zero failures, all
re-derived from `build/test-results` with result files timestamped after the last code commit.

---

## 2026-09-09 — test depth & coverage: the LOCKED/UNVERIFIABLE batch

Read-only. Scope is the eleven commits merged as *"Merge branch 'claude-locked-unverifiable-verdicts' into
develop"*: two new verdicts with a typed `PreventionCause`, and three dependency-resolution fixes
(provided-ids dedupe, multi-ref learned map, patch-version fallback). Commits are cited by **subject** —
this file accumulates and a history rewrite kills hashes in it, which is how thirteen of them died here on
2026-09-01. Suites at analysis time: clientside **515**, grinder **512**
(29 skipped), plugin-grinder **73**, api **412** (1 skipped), app **149**.

Lens: what the *relaxed* rule now lets through, functions that are not total, and stated orderings nothing
asserts.

**No HIGH findings.** No module boundary crossed (`-clientside` gained no dependency; `-api` untouched, so
the published surface and the plugin contract are unchanged), no behaviour change hidden inside a
`refactor:`, and no verdict can now be *published* that could not be published before — `/as-properties`
still gates on `CONFIRMED`, and neither new verdict is reachable from a boot that ran.

### MEDIUM

**M-1 — the patch-version fallback has no pre-boot gate on the dependency's *own* declared Minecraft range.**
`serverpackcreator-clientside/.../BootCandidateSelector.kt` `pickDependencyFile` / `preferenceLadder`, and
`BootVerifier.dependencyToDemote`.

`pickDependencyFile` now stages a dependency built for another patch release of the same line. Nothing
afterwards asks whether that jar's descriptor accepts the version being booted:

- `refuseForSelfDeclaration` reads the **candidate's** jar only.
- `DependencyBacktrack.conflicts` matches *mod-id → version* requirements. It never looks at a staged jar's
  `minecraftConstraint`, and `dependencyToDemote` does not pass one, although it already holds the
  `ScannedMod` for every staged jar and that field is right there on it.

So `cobblemon`'s Fabric 1.21.1 build now stages into a 1.21.11 pack, the loader refuses the pack at runtime,
and the candidate wears an `INCONCLUSIVE` — which *overwrites a decisive verdict in the store*, the harm
shape this module keeps guards for. It cannot reach a false `CONFIRMED` (a wrong-Minecraft library produces
none of the four decisive rungs), so this is a wasted container plus a downgraded verdict, not a wrong
publication.

*What makes it this batch's finding rather than a pre-existing one:* the exact-Minecraft rule **was** the
protection in the version dimension, and *"test(clientside): pin the patch-version dependency fallback"*
quotes the reasoning it relaxed.
The loader dimension already had a gate (`JarSelfDeclaration`) and the version dimension now has none.

*Fix:* have `dependencyToDemote` also demote a **dependency** whose `minecraftConstraint` positively excludes
the pack's Minecraft version. It reuses the existing `excluded`/`MAX_BACKTRACKS` machinery, is symmetric with
`refuseForSelfDeclaration`'s treatment of the candidate, and keeps the fail-toward-proceed rule for free
because `VersionConstraint.satisfies` accepts anything it cannot read. It also closes the same exposure in
the *cross-loader* and *untagged* fallbacks, which predate this batch.

**M-2 — `BootVerifier.preventionCauseFor` is not total: it throws on an empty map.**
`serverpackcreator-clientside/.../BootVerifier.kt`, `PreventionCause.entries.first { it in causes }`.

`causes` is empty for an empty `unsatisfied`, and `first {}` then throws `NoSuchElementException`. The only
call site guards it (`refuseForMissingDependencies` returns `null` before reaching it), so it is unreachable
today — which is exactly the shape this repository has paid for twice: `UnmetReason.explain` returned `null`
for a value no caller could produce, and two log sites would have printed the literal `null` after some later
edit. An `internal` helper with no `require` and no doc saying "never empty" is a landmine, and its name
reads total.

*Failure scenario:* a second caller folds a set it has not proved non-empty — e.g. a future refusal that
collects unmet dependencies and reports even when none were found — and a grind worker dies on an exception
whose message names an enum, not the dependency.

*Fix:* return `PreventionCause.HOST` for an empty set (the same "loudest reading" default every other
prevention site takes) and pin it.

**M-3 — neither new verdict is covered by the publication guard.**
`serverpackcreator-grinder/src/test/.../report/VerdictPublicationTest.kt`.

That test pins "a broken host does not publish" across twenty `ERROR` rows, deliberately, because the failure
mode is a flood. `LOCKED` and `UNVERIFIABLE` are new members of exactly the population it exists to protect
against and appear in no assertion. `FallbackPropertiesRenderer` filters `== Verdict.CONFIRMED`, so it is
correct today by construction — but the guard's whole point is that construction is not what it trusts.

*Fix:* drive the renderer over one row of **every** `Verdict.entries` value and assert only the `CONFIRMED`
one is served. Derived from `entries`, so a seventh verdict is covered without an edit.

**M-4 — `attemptRetentionAgreesWithVerdictRetention` asserts a hand-written list, so it no longer covers
every verdict.** `serverpackcreator-grinder/src/test/.../report/VerdictColumnTest.kt`.

It iterates `listOf(Verdict.CONFIRMED, Verdict.INCONCLUSIVE, Verdict.ERROR)` and asserts `keepsLogs`. Its own
doc calls itself a *drift guard* between `BootArtifacts.worthKeeping` and `Verdict.keepsLogs` — and it now
covers four of six verdicts, silently. `LOCKED`/`UNVERIFIABLE` were given `keepsLogs = false` by judgment
(no container ran, so there is nothing to keep); nothing asserts that judgment, and nothing would notice a
seventh verdict added with the wrong value.

*Fix:* partition `Verdict.entries` on `grindRan` and assert the two groups, so the guard is stated as the
rule rather than as a list.

**M-5 — `LearnedModIds`' thread-safety claim is asserted nowhere, and the value type just became mutable.**
`serverpackcreator-clientside/.../LearnedModIds.kt`.

The class doc ends *"Thread-safe: the grinder shares one instance across its grind workers, which is where
the compounding comes from"*, and `GrindPool` really does share one across N workers. Until this batch the
value was an immutable `String` behind `putIfAbsent`; it is now a `CopyOnWriteArrayList` mutated by
`addIfAbsent` after a `computeIfAbsent`. That composition is correct — `computeIfAbsent` is atomic and
`addIfAbsent` is synchronised — but it is now the kind of correctness worth pinning, and no test in either
module starts a second thread.

*Fix:* concurrent `learn` of the same id from several threads, asserting every distinct ref survives exactly
once and `refFor` is stable. Cheap, deterministic enough with a latch, and it is the property the grinder
depends on.

### LOW

**L-1 — the version-over-constraint half of `preferenceLadder`'s stated ordering is unpinned.**
`DependencyPatchVersionTest` pins obtainability-over-version (`anObtainableNeighbourBeatsALockedExactMatch`)
and version-over-nothing, and `BootCandidateSelectorTest.aConstraintNarrowsTheChoiceButNeverEmptiesIt` pins
the constraint as a preference. Nothing asserts the middle rung: an **exact** file the constraint rejects
beats a **neighbour** it accepts. The ladder does behave that way (verified by reading the yield order), and
the KDoc claims all three, so two of three are load-bearing prose.

**L-2 — `LearnedModIds.restore` can create an empty entry that then round-trips as `"id": []`.**
`computeIfAbsent` runs before the refs are filtered, so an id whose value contributes nothing usable — a
legacy document holding a JSON `null`, a number, or an empty array — leaves an empty `CopyOnWriteArrayList`
behind. Harmless to read (`refFor` returns `null`), but `snapshot()` serialises it, so the file accumulates
entries that assert nothing. `learn` cannot do this (its `ref` is non-blank-guarded).

**L-3 — `mappingsFor` rebuilds the learned ref list inside its own filter.**
`orElse(modId).takeIf { it.ref != null && it.ref !in learned.map { alias -> alias.ref } }` maps `learned` a
second time on every call and nests two `it`-shadowing lambdas. `learned.none { it.ref == … }` says the same
thing once. Style only; the behaviour is pinned by `theRegistryDoesNotRepeatALearnedRef`.

**L-4 — the patch distance inside a line is unbounded, and the `1.21` line is unusually wide.**
`minecraftLine` is the first two components, so `1.21` … `1.21.11` is one line and a dependency eleven patch
releases away is eligible. This is Griefed's stated rule ("if the only difference is a patch version, try the
others"), nearest-first mitigates it, and M-1's fix is the real protection — recorded here so the next reader
knows it was considered rather than overlooked. Bounding the distance is available if M-1's gate turns out to
fire often.

**L-5 — `provided` is complete only for the jar whose staging finishes last.**
A dependency's *own* manifest requirement is planned during its recursion, so an id a **later sibling** will
provide is not yet in `provided` and gets resolved separately. The candidate — the case the ten live rows
were — is fully covered, because its manifest pass runs after every platform dependency is in. Pre-existing
shape of the recursion, not introduced here; noted because the fix's guarantee is narrower than its name
suggests.

**L-6 — `serverpackcreator-clientside/CLAUDE.md`'s testing section is stale in two numbers.**
It says *"257 tests, all offline"* (now 515) and *"**Four** need a resource"* against
`grep -rl "ApiWrapper.api(" src/test`, which now answers **11** — it was already wrong before this batch
(8) and three of the new files added to it. The file's own instruction is to state the count as a command
rather than a number to trust; the sentence should follow its own advice.

### Not findings (verified — do not re-litigate)

- **No unused imports** in any of the fourteen changed files. `ScannedMod` (new in `BootVerifier`),
  `CopyOnWriteArrayList` (new in `LearnedModIds`) and every test import are used.
- **No new compiler warnings.** The one pre-existing warning in `ClientsideVerifier.kt:245` (unnecessary safe
  call on `BootDecision`) is untouched and predates this batch; `BootVerifier`'s "no cast needed" was removed
  in *"fix(clientside): a dependency in the pack cannot refuse its own boot"* while the surrounding function
  was being changed. Also flagged: `ClientsideVerifier.kt:245` is a **line number**, which the conventions
  ask prose to avoid — the symbol is the `confirmedByRule` fold in `verdictOf`.
- **No security surface.** No new input reaches a shell, a path, a query or a log format string. The new
  verdict names are enum constants; the report escapes every cell through `esc()` as before; nothing reads a
  credential.
- **`VerdictPolicy.decide`'s `when (staging.cause)` is exhaustive with no `else`**, so a seventh cause is a
  compile error rather than a silent `ERROR`. Deliberate and verified.
- **`patchOf` handles every malformed shape by returning `null`**, including a non-numeric patch
  (`1.21.4-pre3`), a snapshot (`22w24a`), a trailing dot, and a component above `Int.MAX_VALUE` — the same
  `toIntOrNull()` discipline `readableVersion` was corrected to on 2026-09-08. Pinned by
  `anUnreadablePatchComponentIsNotANeighbour`.
- **The three red commits that could not go red on their signatures say so in their own messages**, and each
  pins the behaviour through a path that compiles against the pre-fix code. Checked against the convention
  rather than assumed: an added enum constant or a new parameter cannot fail an assertion, only a
  compilation.
- **`PreventedGrindBlameTest` does not construct the cause it asserts on** — two guards drive the real
  `prepareBootPack`, two the real `refuseForMissingDependencies` — and it is mutation-verified: forcing
  either cause site to `HOST` fails exactly the three "not our failure" guards and leaves the counterweight
  green.
- **Equivalence against `develop`'s unmodified test tree: 475 guards, 0 failures**, with exactly three files
  uncompilable, each an enumerated signature change adapted by argument only. Re-run recipe is in the root
  `CLAUDE.md`.

### Suggested tests (specific)

1. `aDependencyWhoseDescriptorExcludesThePacksMinecraftIsDemoted` — drive `prepareBootPack` with a staged
   dependency declaring `"minecraft": "~1.21.1"` into a 1.21.11 pack, assert it is dropped and an older build
   staged. (M-1; the guard that makes the relaxation safe.)
2. `aDependencyDeclaringNoMinecraftRangeIsLeftAlone` and `anUnreadableRangeIsLeftAlone` — the
   fail-toward-proceed counterweights for M-1, without which the gate becomes a mass-refusal.
3. `anEmptyUnmetSetIsOurProblem` — `preventionCauseFor(emptyMap())` is `HOST`, not an exception. (M-2)
4. `onlyConfirmedIsEverPublished` over `Verdict.entries` in `VerdictPublicationTest`. (M-3)
5. `retentionFollowsWhetherAGrindRan` — partition `Verdict.entries` on `grindRan` in `VerdictColumnTest`.
   (M-4)
6. `concurrentLearnersKeepEveryRefExactlyOnce` — N threads, one id, distinct refs, latch-started. (M-5)
7. `anExactVersionTheConstraintRejectsBeatsANeighbourItAccepts`. (L-1)
8. `aLegacyDocumentWithNothingUsableForAnIdLeavesNoEntry`, plus a mixed-shape document (one id a string, one
   a list) for `JsonLearnedModIds`. (L-2)

### Suites at the time of this analysis

clientside 515 · grinder 512 (29 skipped) · plugin-grinder 73 · api 412 (1 skipped) · app 149 (needs a local
MongoDB on 27017; green against `mongo:8.0.5` in Docker). All re-derived from `build/test-results`.

### Resolution — every finding closed the same day (2026-09-09)

Fixed on `develop` in the commits below, each cited by subject. Two needed a red pin first; four were
coverage over code that was already correct and landed green; three were hygiene.

| Finding | Commit (subject) | How |
|---|---|---|
| **M-1** | `test(clientside): pin that a wrong-Minecraft dependency is dropped pre-boot` → `fix(clientside): drop a dependency whose descriptor excludes the pack's Minecraft` | `outsideThePacksMinecraft` in `dependencyToDemote`, asked before the version conflicts, candidate excluded, everything uncertain accepting |
| **M-2** | `test(clientside): pin that folding no unmet dependencies answers, not throws` → `fix(clientside): make preventionCauseFor total` | `firstOrNull … ?: HOST`; red with the real `NoSuchElementException` |
| **M-3** | `test: close the coverage gaps the analysis found (M-3, M-4, M-5, L-1)` | `onlyConfirmedIsEverPublished` driven over `Verdict.entries` |
| **M-4** | same | `everyVerdictIsClassifiedForRetention` asserts the partition of `Verdict.entries`, so an unclassified verdict fails the build |
| **M-5** | same | sixteen writers off one latch; mutation `addIfAbsent` → `add` fails it |
| **L-1** | same | `anExactVersionTheConstraintRejectsBeatsANeighbourItAccepts`; mutation hoisting the constraint tier fails it |
| **L-2** | `test(grinder): pin that a restored id with no usable ref leaves no entry` → `fix(clientside): do not remember an id a restore had no ref for` | filter before claiming the entry; fixture is a mixed-shape document |
| **L-3** | `refactor(clientside): ask the learned refs once in mappingsFor` | compare against the ref list directly |
| **L-4** | — | no code change; M-1's gate is the protection, and the reasoning stays recorded above |
| **L-5** | — | no code change; pre-existing shape of the recursion, and the candidate case is fully covered |
| **L-6** | `docs: the audit findings, and the gate that keeps the patch fallback safe` | both counts restated as commands, with the entry's own two-time error recorded |

**One finding was mine and wrong, and the correction is the useful part.** M-4's first implementation
asserted retention as a partition on `Verdict.grindRan` — *"a grind that never ran has nothing to keep"* —
and went **red against correct code**: `ERROR.keepsLogs` is `true` *despite* nothing having run, because an
admin has to diagnose the host and that is the one bucket they can act on. The guard was asserting a rule
that had been invented for it rather than the rule that exists. Reading *why* it failed is what caught it;
the shipped guard states the actual classification with a per-verdict reason and fails the build on an
unclassified verdict, instead of on a tidy-looking predicate.

**One under-reported item, found while fixing M-4.** `VerdictPublicationTest.everyVerdictButClearKeepsItsLogs`
had the same defect as `VerdictColumnTest` — a hand-written list of three — *and* a name this batch made
actively false, since `LOCKED` and `UNVERIFIABLE` discard as well. M-4 named only `VerdictColumnTest`. Both
are now derived from `Verdict.entries`.

**Suites after the fixes:** clientside **523**, grinder **514** (29 skipped), plugin-grinder **73**, api
**412** (1 skipped), app **149** (needs a local MongoDB on 27017). All re-derived from `build/test-results`.


## 2026-09-11 — test depth & coverage: the UNVERIFIABLE pass (21 commits, `86d3d441b..develop`, iteration 1)

Suites at the time of this analysis: api **413** (1 skipped), clientside **554**, grinder **514** (29 skipped),
app **149**, plugin-grinder **73**, plugin-example **3** — 0 failures, and `./gradlew build` green including the
frontend's Vitest suite.

Two facts were measured for this pass rather than reasoned about. **`fabric-api-0.116.17+1.21.1.jar`, read
from the live artifact:** its top-level descriptor is `id = "fabric-api"`, `provides = ["fabric"]`, and it
ships 49 nested jars — `fabric-resource-loader-v0` exists **only** as
`META-INF/jars/fabric-resource-loader-v0-0.116.17.jar`. And **every pin of the range was re-run at its own
commit** (table in `REFACTOR-AUDIT.md`), which is what turned up the one that does not compile.

### HIGH — none

No new code path can reach a published verdict on evidence it does not have: the two staging fallbacks added
here (`alternativeFor`, `acrossPlatforms`) can only *add* a staged jar, never promote a rung, and the
loader re-selection leaves `bootedLoader` different from the verdict's loader, which
`loaderDisprovingTheCrash` already refuses to accept as a disproof.

### MEDIUM

| # | Where | Finding |
|---|---|---|
| A-1 | `BootVerifier.kt:1699` (`1443d9f46`) | **Bug — the `unless` drop arm cannot fire for the case its own comment names.** It tests `providedIds` only; the alternative `fabric-resource-loader-v0` is a **nested** jar of Fabric API and therefore lands in `bundledIds` (measured above). So a pack that already contains Fabric API does not drop the requirement: it goes the expensive way through `alternativeFor`, re-resolves the project and re-downloads a jar already in `mods/`. No wrong verdict (`injected` dedupes by file name, so `MAX_INJECTED_DEPENDENCIES` is safe), but the cheap path is dead and the comment is false. The arm one line above it does consult `bundledIds`. Same finding as `REFACTOR-AUDIT` M-2. |
| A-2 | `CurseForgePlatform.kt:231`, `ClientsideModels.kt` (`eb277c8c9`) | **The CurseForge half of the release-channel rule is asserted nowhere.** `ReleaseChannel.fromCurseForge` has exactly one call site and no test names a channel on the CurseForge path — the two CF fixtures that mention `releaseType` set it to `1` incidentally and assert nothing about it. Unexercised: `2`→BETA, `3`→ALPHA, and the fail-toward-RELEASE for an absent or unknown `releaseType`. `ReleaseChannelPreferenceTest` drives `ModrinthPlatform` only. CurseForge is the larger catalog *and* the one with no sideness field, i.e. where booting an unrepresentative build costs most. |
| A-3 | `BootCandidateSelector.kt:74` (`eb277c8c9`) | **"The channel preference never overrides `loaderVersionAvailable`" is unpinned.** Every `pick()` in `ReleaseChannelPreferenceTest` passes `{ true }`. The implementation is right — the availability gate sits *inside* `pickBootableCandidateFrom`, so a release for an unbootable Minecraft falls through to the beta pass — but nothing holds it there: hoisting the channel filter above the gate would pass all five current guards while making a project whose only release targets an unsupported Minecraft unverifiable. |
| A-4 | `BootVerifier.kt:1399` (`355d322ef`) | **`loaderToVerifyUnder`'s tie-break is unpinned.** It is `internal` and reached only through staging with a *single* declared loader, so neither documented rule is asserted: "prefer a loader the platform also tagged" and "otherwise alphabetical, purely for determinism". A future edit could reverse the preference, or make the choice depend on set iteration order, with the suite green. |
| A-5 | `QuiltScanner.kt:138` (`1443d9f46`) | **Two of `readUnless`'s three shapes are unexercised.** It handles a bare string, an object carrying `id`, and an array of either; `grep unlessProvided` over both test trees returns nothing and the one integration guard writes `"unless":"<id>"`. Parsing, in the published module, where a wrong branch yields a plausible value rather than an error — the class this repo requires a first-written test for. |

### LOW

| # | Where | Finding |
|---|---|---|
| A-6 | `ModrinthPlatform.kt:138` (`37e2d2797`) | **Bug — a *failed* pin lookup is not memoised.** `?: return null` precedes the cache write, so a `version_id` that 404s is re-fetched once per version node that pins it, with a WARN each time. `resolve` walks a project's whole version list, so the failure path costs exactly what the memo was added to prevent. `thePinIsLookedUpOnce` pins only the success path. |
| A-7 | `ModIdRegistry.kt` (`6ee12d4f8`) | **The new fall-through is unpinned on the side it exists for.** A table entry with no ref for a platform now yields that platform's slug `Guess` instead of `None`; only the Modrinth side of `tacz` is asserted. Nothing pins `mappingFor("tacz", "CurseForge") == Guess("tacz")`, which is precisely the behaviour the deliberately-`null` CurseForge ref was chosen to preserve. |
| A-8 | `LoaderDescriptors.kt` (`3d61e97c6`) | An unreadable Minecraft version is pinned only through `scannerFor`. The gate's own entry point — `descriptorsFor("NeoForge", "")`, which is what `JarSelfDeclaration` actually calls — is unasserted, and it is the one whose answer decides whether a jar is refused. |
| A-9 | `serverpackcreator-clientside/CLAUDE.md` (`f8239995a`) | **Documentation scope.** The channel rule is stated unscoped, but it applies to `pickBootableCandidate` **only**: `pickDependencyFile` and `pickRecheckCandidates` are channel-blind. Both are defensible on purpose — a dependency merely has to load, and the re-check wants *diversity* — but a reader will assume otherwise and "fix" one of them. |
| A-10 | module docs (`ab188dff4`, `cf78607ab`) | Stale citations: `filenamePattern` and `theFilenamePatternIsNotWhatGetsPublished` no longer exist, and "`resolveDependency` stays single-page on purpose" is now half true. Detail in `REFACTOR-AUDIT` M-7/M-8. |
| A-11 | `CurseForgePlatform.filesUrl` | **Informational (security).** The Minecraft version is interpolated into the query string unencoded. Not reachable today — both sources are SPC's own release list — but `ModFile.minecraftVersions` holds author-influenced CurseForge `gameVersions` strings and is one refactor away from that parameter. Nothing in the range logs or embeds a credential; `CurseForgePlatform("test-key", …)` is a test literal. |

### Verified clean — do not re-litigate

- **The neighbour ordering is thoroughly pinned already**, by the pre-existing `DependencyPatchVersionTest`:
  nearest-first, equidistant→newer, never crosses the Minecraft line, does not widen the loader rule, an
  unreadable patch component is not a neighbour, and obtainable-vs-locked in both directions. Extracting
  `patchNeighboursIn` did not weaken any of it, and the *new* caller's cost is asserted end-to-end by
  `CurseForgeDependencyLineTest.theExactVersionIsAskedForFirst` (exact request sequence).
- **All three string-replacement fixtures in `ReleaseChannelPreferenceTest` have teeth.** If a replacement
  silently failed to apply, the expected file changes, so the guard fails rather than asserting nothing —
  checked case by case, because that construction is exactly how a fixture stops testing anything.
- **Asking for a version twice is impossible:** `alsoVersions.filter { it != minecraftVersion }.distinct()`
  on the way out and `distinctBy { it.fileName }` on the way back.
- **The pin memo is race-free** — `ConcurrentHashMap`, and each candidate gets its own platform instance.
- **No unused imports** anywhere in the range; `ModScanner.kt` correctly dropped `Comparison` and
  `SemanticVersionComparator` when the era predicates moved out.
- **Every new unit carries KDoc**, and there is no new `!!`, no new `var`, and no `TODO`/`FIXME` in any
  main-source addition across the 21 commits.

### Suggested tests (specific)

1. `QuiltUnlessClauseTest.anUnlessAlternativeAlreadyBundledDropsTheRequirement` — Quilt candidate declaring
   `quilt_resource_loader unless fabric-resource-loader-v0`, with a staged jar that *bundles*
   `fabric-resource-loader-v0` under `META-INF/jars/`; assert through a **recording** downloader that nothing
   is fetched for it (closes A-1, and fails today).
2. `ReleaseChannelPreferenceTest` (or a CurseForge sibling) — drive the real `CurseForgePlatform` over canned
   JSON with `releaseType` 1/2/3 and one file with the field absent; assert the `ModFile.channel` of each, and
   that a `releaseType:1` file on an older Minecraft is picked over a `releaseType:2` file on a newer one
   (closes A-2).
3. `…theChannelPreferenceNeverOverridesLoaderAvailability` — the release is tagged for a Minecraft whose
   `loaderVersionAvailable` answers `false`, the beta for one that answers `true`; assert the beta is picked
   (closes A-3; fails if the channel filter is ever hoisted above the gate).
4. `LoaderReselectionTest` — three direct `loaderToVerifyUnder` guards: a platform-tagged declared loader wins
   over an untagged one; with neither tagged the choice is alphabetical and stable; with none bootable the
   answer is `null` (closes A-4).
5. `QuiltScannerTest` in `-api` — one jar per `unless` shape (bare string, `{"id": …}`, array of both, plus a
   blank id) asserting `ModDependency.unlessProvided` exactly (closes A-5, and is the first assertion of that
   field anywhere).
6. `PinnedDependencyRefTest.aFailedPinLookupIsAttemptedOnce` — a fetcher that throws for `/version/{id}` and
   counts; two versions pinning it must produce **one** attempt (closes A-6, fails today).
7. `ModIdRegistryTest` — `mappingFor("tacz", "CurseForge")` is `Guess("tacz")`, and `mappingFor("tacz",
   "Modrinth")` is `Alias("timeless-and-classics-guns")` (closes A-7).
8. `LoaderDescriptorsTest` (or `ModScannerDispatchTest`) — `descriptorsFor("NeoForge", "")` and
   `descriptorsFor("NeoForge", "26")` answer the modern set rather than throwing (closes A-8).

### Resolution — iteration 1 (2026-09-11)

Every finding above is closed; the detail and the mutation results are in `REFACTOR-AUDIT.md`'s matching
resolution section rather than duplicated here. Two of them changed what the code does — the `unless` arm now
sees a bundled alternative (A-1) and a dead pin is asked once (A-6) — and one changed where a rule lives
(the retry order, M-4). The other eight were guards for behaviour that was already correct, which is why
each was **mutation-verified** rather than trusted: A-2 (forcing `fromCurseForge` to RELEASE fails both new
CurseForge guards), A-3 (hoisting the channel filter above the availability gate fails its guard), A-4
(dropping the platform-tagged preference fails its guard).

**One process note worth keeping, because it nearly cost a false conclusion.** The first run of the A-2
mutation reported the *wrong* failing test: a regex edit had left an orphaned `when` body, so that build
never compiled, and the parser happily read the **previous** run's `test-results` XML. A mutation check that
cannot compile says nothing, and its output looks exactly like a result. Clear `build/test-results` before a
mutation run, and treat "no results" as a distinct outcome from "zero red".

## 2026-09-11 — test depth & coverage: iteration 2

Suites entering iteration 2: api **421** (1 skipped), clientside **566**, grinder **514** (29 skipped),
app **149**, plugin-grinder **73** — 1,723 tests, 0 failures. Equivalence for iteration 1's production code
against `develop`'s unmodified test tree: **api 413 / 0, clientside 554 / 0, no compile errors.**

### MEDIUM

| # | Where | Finding |
|---|---|---|
| B-1 | `FilenamePatternTest` (whole class) | Six guards whose **stated subject no longer exists** — the "narrower entry derived from the sampled file" became the file's verbatim name on 2026-09-10. They still pass because they call `FilenameStemDeriver.deriveStem` directly, so what has rotted is the *why*, not the *what*: single-file `deriveStem` is now load-bearing only for `Prepared.Ready.candidateStem`, i.e. telling the candidate's stack frames from a dependency's when a crash is blamed. A reader looking for the guard on the Filename column lands here and learns the wrong thing about what that column holds. |
| B-2 | `ClientsideVerifier.kt:168` | The **producer** of that column is unguarded. Every existing assertion about it either injects the value into a fixture or pins the mapping one layer downstream, which is the arrangement `DependencySlugTest` exists to warn about. The regression it cannot see is the exact code that was removed: re-deriving a stem from the sampled file. |
| B-3 | `LoaderReselectionTest` | Iteration 1's retry-order fix is asserted as **data** (two fields of one refusal) and not as **behaviour** (the version retry firing when the loader retry cannot, and the re-staged attempt clearing the descriptor gate). |

### LOW

| # | Where | Finding |
|---|---|---|
| B-4 | `serverpackcreator-clientside/CLAUDE.md:1101` | Iteration 1's own doc fix names `LoaderVerdict.fileName`, a property that does not exist — see `REFACTOR-AUDIT` I2-1. |

### Verified clean — do not re-litigate

- **Iteration 1's three production changes are equivalent to `develop` for every pre-existing guard**
  (413 + 554, zero failures, zero compile errors), so none of them moved a signature or a behaviour the old
  tree could see. The behaviour each one *did* change is covered by its own new guard.
- **`unresolvablePins` cannot grow without bound**: it is per platform instance, and a platform instance is
  built per candidate, so its ceiling is the distinct `version_id`s pinned by one project's version list.
- **`fromCurseForge`, `loaderToVerifyUnder` and the channel-inside-the-gate ordering are now all
  mutation-verified** (iteration 1's resolution section lists which mutation kills which guard).

### Suggested tests (specific)

1. Rename `FilenamePatternTest` to what it pins — single-file stem derivation, as used by `candidateStem` —
   and correct its doc; keep every assertion (closes B-1 without losing coverage).
2. A `-clientside` guard that `LoaderVerdict.sampleFile` is the sampled `ModFile.fileName` **verbatim**,
   extension and all, driven through the real `ClientsideVerifier` rather than a constructed verdict
   (closes B-2; fails if anyone re-derives a stem there).
3. `LoaderReselectionTest.theVersionRetryFiresWhenTheLoaderRetryCannot` — a `mods.toml`-only jar requested
   as NeoForge at a version ≥1.20.5 whose own range accepts an older release, with Forge withheld so the
   loader retry cannot fire; assert two staging attempts, both NeoForge, and that the second clears the
   descriptor gate because `mods.toml` names NeoForge below 1.20.5 (closes B-3).

### Resolution — iteration 2 (2026-09-11)

All four closed; detail in `REFACTOR-AUDIT.md`'s iteration-2 resolution. The one worth repeating here is
B-3, because it inverted a finding: the behavioural guard demanded by the analysis went **red against code
that was already supposed to be fixed**, which is how iteration 1's M-4 was exposed as a false finding. A
guard that asserts shape can be green and prove nothing; a guard that asserts the consequence can be red and
prove the *finding* wrong. Both directions are worth having, and only the second one catches a bad diagnosis.

## 2026-09-11 — test depth & coverage: iteration 3

Suites entering iteration 3: api **421**, clientside **568**, grinder **514**, app **149**,
plugin-grinder **73**.

The third pass deliberately looked where the first two had not: at the **consumers** of the value the range
changed from a derived stem to a verbatim, author-controlled filename, and at the seam the cross-platform
fallback is wired through. Both came back clean, and the reasoning is recorded in `REFACTOR-AUDIT.md`'s
iteration-3 "verified clean" list so a fourth pass does not re-derive it: HTML/CSV/JSON escaping, path
traversal on `/boot-log`, the `!==` identity the alternate-platform filter depends on, and the exclusion set
crossing platforms.

### LOW

| # | Where | Finding |
|---|---|---|
| C-1 | `VerdictReportRenderer.kt:200` | The Project cell's `href` takes any scheme. Escaping stops markup, not `javascript:`. Not attacker-reachable today (operator-queued URLs and CurseForge's own `websiteUrl`), and the server is loopback by default — but it is one `SPC_GRINDER_HOST` away from being served, so the cheap allowlist is worth having. Needs a guard asserting an untrusted scheme renders as text. |
| C-2 | `ClientsideVerifier.kt:245` | An unnecessary safe call on a smart-cast non-null receiver — a compiler warning in a file this range touched. |

### Suggested tests (specific)

1. `VerdictReportRendererTest.aProjectUrlWithAnUntrustedSchemeIsNotLinked` — a verdict whose `projectUrl` is
   `javascript:alert(1)` renders as escaped text with no `<a href`, while an `https://` one still renders as
   a link (closes C-1).

### Resolution — iteration 3 (2026-09-11)

Both closed, both mutation-verified; detail in `REFACTOR-AUDIT.md`. The pass's real output is its
"verified clean" list: five consumer-side questions an author-controlled string reaching three renderers
raises, answered once and written down, so the next pass starts from a shorter list instead of the same one.

---

## 2026-09-20 — test depth and defect analysis of `develop` after the server-pack-update merge

Scope: `develop` at `710c2f78c`, with attention on the update mechanism merged today
(`ServerPackUpdater`, `ServerPackHandler.run`, `ServerPackProvisioner`, `ServerPackFileGatherer`).
Read-only; the two probes written to verify findings were deleted and the tree is clean.

### HIGH

**H1 — a first generation with updating enabled never writes the *local* `variables.txt`, so the
user's configured Java path is missing from the pack they are about to run.**
`serverpackcreator-api/.../ServerPackHandler.kt:325` writes `manifest.json`, and
`ServerPackUpdater.isUpdateRun` (`ServerPackUpdater.kt:57`) decides an update by asking whether that
file exists. Both `createServerRunFiles` calls happen **after** the write (`:359` zipped variant,
`:375` local variant), so `isUpdateRun` flips from `false` to `true` *in the middle of the run*. On a
first generation the sequence is:

1. `:359` writes the zipped `variables.txt` — `preserve` is false, because the file does not exist yet.
2. `:375` should overwrite it with the local variant, which is the only one carrying the user's
   `SPC_JAVA_SPC` path — but `variables.txt` now exists, is protected, and `isUpdateRun` is now true,
   so `preserve` returns **true** and the local variant is never written.

The pack keeps the archive's copy, where `SPC_JAVA_SPC` was replaced with the literal `"java"`
(`ServerPackProvisioner.kt:500-503`).

*Verified, not inferred.* A probe generated the same config twice into `@TempDir`s, differing only in
the toggle, with `SPC_JAVA_SPC = /opt/my-very-distinctive-jdk/bin/java`:

| run | `variables.txt` contains the configured Java path |
|---|---|
| updating **enabled**, first generation, zip desired | **false** |
| updating disabled (control) | **true** |

Reachable in the default configuration of the feature that just shipped: overwrite defaults on,
zip-creation defaults on, and the user ticks "Update Server Packs". On a genuine *update* the
behaviour is correct and wanted — the operator's file must survive — so the fix has to distinguish
"existed before this run began" from "this run just wrote it". Snapshotting `isUpdateRun` (and the
set of protected files that already existed) **once, before any file is written**, does that; the
`preserve` lambda then closes over the snapshot instead of re-deriving it. That also removes the
surprise that a value read twice in one run can disagree with itself.

No guard covers this: `anUpdateKeepsTheOperatorsVariables` exercises the *second* run only, and no
test asserts anything about `variables.txt` on a first generation.

### MEDIUM

**M1 — `lazy_mode` bypasses update protection entirely, and silently degrades the update.**
`ServerPackFileGatherer.kt:86-98` returns before the copy loop, so the `isProtected` predicate is
never consulted: the whole modpack is written with `copyRecursively(..., overwrite = true)`. Two
consequences, one pre-existing and one new:
- Anything in the modpack that collides with a protected path (a shipped `world`, a `server.properties`)
  overwrites the operator's copy, which is exactly what the feature promises not to do.
- The branch returns an **empty** `copiedFiles`, so on an update `files.isEmpty()` is true and the
  prune is skipped (fail-safe, but it means a lazy-mode pack never converges), and the manifest records
  only the provisioned run-files rather than the pack. The emptiness predates this work; its new
  significance does not.

`lazy_mode` is referenced by `ServerPackFileGathererTest` and `ValidatorsTest`, but by **no** test in
combination with updating.

**M2 — the prune's keep-set is compared case-insensitively, which is wrong on a case-sensitive
filesystem.** `ServerPackUpdater.kt:125,129` builds `keep` from `produced` lowercased and tests
`keep.contains(relative.lowercase())`. On Linux `Mods/Alpha.jar` and `mods/alpha.jar` are two files;
folding them together means an old manifest entry is treated as "still produced" and is never pruned,
so a stale file survives indefinitely. The failure is safe (it under-prunes rather than deleting
something live), which is why nothing catches it. Note the case-insensitivity of `protects()` is a
*deliberate* and defensible choice — protection should be liberal — so only the keep-set comparison is
in question.

**M3 — four `lowercase()` calls with no locale.** `ServerPackUpdater.kt:125,129` and
`ServerPackHandler.kt:328,341`. Kotlin's no-arg `lowercase()` uses the **default locale**, so under
`tr`/`az` an `I` folds to `ı`. Both sides of each comparison fold identically, so a match still
matches; the residual risk is two distinct paths colliding (again under-pruning). `Locale.ROOT` is the
correct argument for anything comparing identifiers or paths, and the only other case-folding in the
API (`SystemUtilities.kt:41`) does pass a locale explicitly.

**M4 — test depth: the update mechanism has 25 guards and none of them exercise these.**
Concretely missing, in the order I would add them:
1. **First generation + updating enabled writes the local `variables.txt`** — the H1 regression guard.
2. **A manifest written on the other platform.** `protects()` is unit-tested against backslashes, but
   `prune()` is never given a manifest whose entries are `mods\alpha.jar`; that is the path a pack
   generated on Windows and updated on Linux actually takes.
3. **`lazy_mode` + updating** — assert what happens to a protected path, and to the manifest.
4. **An absolute path in an old manifest** — `File(serverPack, "/abs/path")` resolves *under* the pack
   on Unix; assert nothing outside the pack is deleted.
5. **Update + `customDestination`** — every update guard uses `customDestination`, so the derived
   `getServerPackDestination` path is never exercised with updating on.
6. **A protected *file* whose name prefixes a real directory** (`world` the file vs `world/` the tree)
   and the converse — `protects` is prefix-based and the boundary is untested in `prune`.
7. **`zipBuilder` with `isZipFileExclusionEnabled = false`** — the filter is now installed
   unconditionally (a behaviour change recorded in `API-BEHAVIOUR-CHANGES.md`), and no test covers the
   disabled branch. `GenerationConfigTest` only round-trips the flag through the store.
8. **`readManifest` on an unreadable (not merely unparseable) file** — the permission path.

### LOW

**L1 — `${{ }}` interpolated into `run:` shell in `devbuild.yml:141,215`.** The value is a filename
this workflow's own previous step produced from `ls`, so it is machine-generated rather than
attacker-supplied, and the pattern predates these commits (`release-build.yml:31` interpolates
`github.ref_name` the same way, from `832eb4684`). Reading it through `env:` rather than expression
interpolation is the hardened form. Flagged for completeness, not as an exploitable finding.

**L2 — `ServerPackUpdater.protects` is called once per file on an update.** Looks like the
hot-loop-invariant problem `ModListCompiler` already paid for; it is not. See the measurement below.

### Verified clean — do not re-litigate

- **Hoisting the protected-paths list out of the per-file loop buys nothing.** Measured on this machine,
  20,000 paths: `protects()` **12.0 ms**, of which the `updateProtectedPaths` getter (which builds a
  fresh `TreeSet` and reads `java.util.Properties`) is **2.5 ms**; the same matching with the list
  hoisted once is **12.7 ms**, i.e. indistinguishable. The cost is the 13-entry scan per path, not the
  getter, and 12 ms across a whole update is not worth a cache. This is the B30 lesson again — measure
  before optimising, and record the negative result so nobody re-opens it.
- **No unused imports** in any of the five changed `api/serverpack` files.
- **No new compiler warnings** in the touched files (both modules, `--rerun-tasks`).
- **No dead code in `run()`** — `isProtected` ×4, `isOperatorData` ×2, `preserve` ×5, `producedPaths` ×2.
- **Module boundary intact** — `ServerPackUpdater` imports only Jackson, `ApiProperties`, one file
  utility and log4j; no Swing, Spring-web or frontend type anywhere in `api/serverpack`.
- **No secrets, no injection surface in the Kotlin changes.** Nothing in the new code interpolates into
  a shell, builds SQL, or touches credentials.
- **Error handling is present on every new I/O path** — `readManifest` catches and reports, returning
  `null` so a manifest that cannot be understood prunes nothing; `relativize` catches
  `IllegalArgumentException` and returns `null` rather than recording a `../` path;
  `directory.list()?.isEmpty() == true` treats an unreadable directory as not-empty and leaves it.
- **Documentation** — every member of `ServerPackUpdater` bar its logger carries a doc comment, matching
  its siblings.

### Recommendation

H1 is a user-visible regression in the feature that just shipped and should be fixed before this reaches
a release, with the guard written red first. M1–M3 are correctness gaps that under-deliver rather than
destroy, and can follow. M4's list is the test debt that let H1 through: every existing guard exercises
the *second* run, and none asserts what a *first* run produces while updating is enabled.

#### Second pass, same day — findings closed, and one of them retracted

`develop` at `5bc4e2743`. Full build green, **1,904 tests, 0 failures** across six modules.

**H1 closed** (`5bc4e2743`, red-pinned first in `b57efd7e2`). `ServerPackHandler.run` now snapshots
which provisioned files were already present *before it writes anything*, and the icon,
`server.properties` and run-file decisions all read that one snapshot. The mid-run flip of
`isUpdateRun` can no longer make this run's own output look like the operator's.

**M1 closed** (`5bc4e2743`, red-pinned first). Lazy mode walks the modpack, skips a protected path
that already exists, and records what it copied.

**M3 closed** (`5bc4e2743`). Four comparisons now pass `Locale.ROOT`.

**M2 RETRACTED — the recommendation was wrong, and implementing it would have destroyed data.**
The report said case-insensitive comparison of the prune's keep-set is "wrong on a case-sensitive
filesystem". The reasoning was one-sided. On a **case-insensitive** filesystem — Windows, and macOS
by default — `Mods/Alpha.jar` and `mods/alpha.jar` are *one file*, and the prune runs **after** the
copy. An unfolded comparison there would fail to match the entry the copy had just written, conclude
it was stale, and **delete the file this very run produced**. Folding costs an occasional stale file
on Linux; not folding costs a freshly generated file on Windows. Under-pruning is the safe direction,
so the existing behaviour stands and is now commented with that reasoning rather than changed.

Recorded because the error is instructive: the finding named a real asymmetry and then proposed the
fix that resolves it in the destructive direction. A recommendation that changes what gets **deleted**
has to be argued on both filesystems before it is written down, not just the one that prompted it.

**M4 partially closed.** Items 1 and 3 of the suggested-tests list are now guards
(`aFirstGenerationWritesTheLocalVariablesEvenWhenUpdatingIsEnabled`,
`lazyModeStillRespectsProtectionOnAnUpdate`, `lazyModeRecordsWhatItCopied`). Items 2, 4, 5, 6, 7 and 8
remain open test debt; none of them guards a defect that is currently known to exist, which is why they
were not written speculatively.

**L1 stands** as recorded (not exploitable, pattern predates the commits). **L2 stands retracted** by
its own measurement.

### New in this pass — verified clean

- **The H1 defect class does not recur.** Every other `updater.*` call in `run()` was re-checked for a
  value that can change mid-run: `isUpdate` is captured once at `:219`; `relativize` and `protects` are
  pure; `prune` runs once. The one remaining per-call derivation is `isProtected`'s `candidate.exists()`
  at `:260`, and it is safe by construction — the copy checks a destination *before* writing it, and no
  `ServerPackFile` writes another's destination. Directory entries are created before their children are
  checked, but on a first generation `isUpdate` is false so nothing is protected, and on an update the
  children exist already. Same reasoning holds for the new lazy-mode walk, which descends into a skipped
  protected directory and checks each child individually.
- **Lazy mode writing into a destination nested inside the modpack** is a pre-existing hazard, not one
  this change introduces: `walkTopDown()` and `copyRecursively` are both depth-first walks of a tree
  being written into, so the risk is unchanged. It is unreachable with the default destination
  (`<home>/server-packs`, outside the modpack) and needs a `customDestination` pointing inside the
  modpack to trigger.
- **`preserved` is computed before `runPreGenExtensions`**, so a file a plugin writes during pre-gen is
  not mistaken for the operator's. That matches the previous behaviour, where no plugin-written file
  could be preserved either.

#### Third pass, same day — M4 fully closed

`develop` at `2f3795465`. Full build green: **1,910 tests, 0 failures, 30 skipped** (the 30 are
pre-existing skips in `grinder` and `api`).

All six remaining suggested tests are written (`2f3795465`). Every one **passes against today's
code** — they guard no present defect — so each was proved by mutation instead, and each failed
exactly the guard written for it:

| suggested test (M4 item) | mutation applied to production code | guards that failed |
|---|---|---|
| 2 — cross-platform manifest | `normalize()` stops converting `\\` to `/` | `aManifestWrittenOnTheOtherPlatformStillPrunes`, `protectionIgnoresSeparatorStyleAndCase` |
| 4 — absolute path in an old manifest | `prune` resolves entries as `File(relative)` | `anAbsolutePathInAnOldManifestCannotReachOutsideThePack` + 4 others |
| 5 — derived destination | *(integration guard, no single-line mutation)* | — |
| 6 — segment boundary | `startsWith("$protected/")` relaxed to `startsWith(protected)` | `protectionStopsAtTheSegmentBoundaryWhenPruning`, `protectionCoversAnEntryAndItsContents` |
| 7 — zip preference off | filter installed only when `isZipFileExclusionEnabled` | `theCallersExclusionAppliesEvenWithTheZipExclusionPreferenceOff` |
| 8 — unreadable manifest | `readManifest` stops catching | `anUnreadableManifestPrunesNothingEither`, `anUnreadableManifestPrunesNothing` |

Item 8 is guarded by `Assumptions` on both `setReadable(false)` succeeding and the file genuinely
being unreadable afterwards, so a filesystem that ignores the call — or a run as root — skips rather
than passes for the wrong reason. It ran, not skipped, on this machine.

Item 5 has no single-line mutation because what it pins is a *path through* `run()` rather than a
predicate: it drops `customDestination`, lets `getServerPackDestination` compute the directory from
the server-packs directory and the pack name, and asserts the second run updates that pack rather
than generating a second one beside it — which is what every other update guard, all using
`customDestination`, could not have caught.

**No findings remain open from the 2026-09-20 analysis.** H1, M1, M3 fixed; M2 retracted; M4 closed;
L1 stands as documented-not-exploitable; L2 stands retracted by measurement.


## 2026-09-23 — test depth and residual defects in the upload/check/storage pass (`4be89ab67..develop`)

Companion to the same-day section in `REFACTOR-AUDIT.md`, which covers convention compliance. This one
covers what the new suites actually assert, and what is still wrong in the changed code. Read-only.

### Method

- Every `.kt` touched in the range checked for unused imports by extracting each non-wildcard import's
  simple name and grepping the import-stripped body. **Result: none.** (Three apparent hits —
  `import Translations` in `ConfigurationHandler`, `ModpackManifestParser`, `ModpackZipInspector` — are
  the checker mis-splitting a dotless import, not real.)
- Each new guard read for whether its fixture still reproduces the condition it names, rather than for
  whether it passes.
- Changed response paths read for unescaped client-controlled data.

### HIGH

**A-1. `StorageSystemTest.anUploadThatCannotBeWrittenIsReportedAsAnEmptyResultRatherThanThrowing`
asserts nothing — the fix it guards stopped its own fixture from reproducing the condition.**
`StorageSystemTest.kt`, the third C1 guard. It uploads `"sub/dir/pack.zip"` and asserts
`assertDoesNotThrow { land(upload) }`. That was a real guard when written: the unsanitised name made
`transferTo` fail with `NoSuchFileException`, and it was measured red. But the fix (`57b172c77`) reduces
the name to its base name, so `"sub/dir/pack.zip"` now becomes `"pack.zip"` and the write simply
**succeeds**. The assertion passes because nothing went wrong, not because a failure was contained. Its
comment still claims it covers the unhandled-500 path.

This is the failure mode the root `CLAUDE.md` calls out by name — *ask why a guard **passed**, not only
why it failed* — and it is the second instance in this pass (the empty-repository sweep guard was caught
during development for the same reason; this one was not).
**Remedy:** make the write genuinely fail independently of the name — e.g. point `rootLocation` at a
path that is a regular file, the same technique `ModpackExtractionFailureTest` already uses — and drop
the now-false comment. The `IOException`/`IllegalStateException` catches in `StorageSystem.land` are
currently covered by no executing assertion at all.

**A-2. `ModPackController.kt:71` interpolates the client-supplied filename into a quoted
`Content-Disposition` header with no escaping.**

```kotlin
.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${modpack.get().name}\"")
```

`ModPack.name` is `SavedFile.originalName`, which is the upload's own filename — attacker-controlled,
and now *deliberately* preserved verbatim by `landingName` (only path separators are stripped). A name
containing `"` closes the quoted string early; one containing `";` can append a second parameter. The
endpoint has no authentication and `@CrossOrigin("*")`. Tomcat rejects CR/LF, so this is header-parameter
injection rather than response splitting, but it is unvalidated data in a response header on a public
route. `ServerPackController.kt:69` has the same shape, though its value is a generated name rather than
a user's.
**Notable because this line was edited in this pass** — `8bbd000ac` changed the body on the very next
line and left the header alone. Remedy: `ContentDisposition.attachment().filename(name, UTF_8).build()`,
which encodes per RFC 6266.

### MEDIUM

**A-3. The embedded mongod version is spelled three ways, one of them dead.**
`EmbeddedMongoAvailable.kt:64` declares `const val MONGOD_VERSION = "8.0.5"` and **never uses it**; the
probe at `:71` starts `Version.Main.V8_0` (whatever 8.0.x flapdoodle's `Main` currently points at), while
`WebServiceContextTest.kt:76` and `WebPersistenceIT.kt:60` each hard-code the string `8.0.5` in their
annotations. So the thing that decides *whether to skip* and the thing that decides *what to run* are
different values, and a dead constant sits between them looking like it ties them together. Precisely the
repo's own recorded lesson — *duplicated knowledge drifts toward whichever copy is easier to reach* — and
the failure it enables is the worst kind: the probe succeeds, the tests then try to download a version
the probe never validated, and the failure is attributed to the test rather than the version.
**Remedy:** one constant, consumed by both — the annotations can reference a `const val` since they need
a compile-time constant, so `"de.flapdoodle.mongodb.embedded.version=" + MONGOD_VERSION` works, and the
probe should resolve from the same string rather than `Version.Main`.

**A-4. Four coverage gaps, each one an assertion that could exist and does not.**

| Gap | Why it matters |
|---|---|
| Neither cron is ever run against a real database | `WebPersistenceIT` covers index, migration, round-trip and legacy rows, but the two sweeps — the only *destructive* code in the module — are still covered only by mocked repositories. The empty-repository refusal and the GridFS reclaim are asserted against doubles, never against data. |
| No test that a wrong content type is now refused | `8a9e337f3` added `consumes = MULTIPART_FORM_DATA_VALUE`, which makes Spring answer 415 instead of accepting. One `MockMvc` case would pin it. |
| No frontend test for the three SPA fixes | `SubmitModPackForm.test.ts` exists and the harness is set up; the `modPackId`/`modPackID` mismatch and the `error.data` → `error.response.data` fix went in unguarded. |
| `ArchiveStreamingTest` asserts the body's *type*, not its content | It proves `FileSystemResource` rather than `ByteArrayResource`, which is the point, but never checks that the bytes served match the archive or that `contentLength` is right. A streaming change that served the wrong file would pass. |

**A-5. A stale comment inside A-1's test.** It says *"application.properties ships
include-stacktrace=ALWAYS"*. `f5755f7d9` changed that to `NEVER` later in the same pass. The comment is
the justification for the guard, so it is wrong twice over.

**A-6. `FileCleanupSchedule.sweep` deletes an orphan twice when it has both an archive and an extracted
directory.** The loop iterates directory entries, and `<id>.zip` and `<id>/` both match
`storageIdPattern` after `removeSuffix(".zip")`. The first `deleteStored(id)` removes both plus the
GridFS document; the second repeats the whole call, costing a redundant Mongo round-trip per orphan.
Harmless — `delete` is idempotent — but it means the sweep's cost is per *entry* rather than per *pack*.

### LOW

**A-7. `TaskExecutionServiceImpl.reportFailure` persists whatever state the failed task had reached.**
`processTask` mutates `taskDetail.modpack` as it goes, so a task that throws midway is saved with its
partial mutations plus `status = ERROR`. Correct enough — the row is being marked failed — but the
saved `projectID`/`versionID`/`source` may be half-applied.

**A-8. The concurrent-upload race is documented but unpinned.** Two simultaneous uploads of one file
both land, both hash, both find no duplicate, and both store — two rows with one hash.
`findFirstBySha256`'s `First` exists precisely to tolerate that, and its KDoc says so, but nothing
asserts the tolerated outcome.

### Not findings / positives (verified — do not re-litigate)

- **No unused imports** in any of the 40+ `.kt` files changed or added in the range.
- **The `land`/`store` split is genuinely two-phase**: `ModPackUploadStorageTest` asserts on the storage
  root's *contents*, not on mock interactions, so it would catch a commit that reintroduced the write.
- **`WebPersistenceIT` tests outcomes against a real server**, including the two claims this pass most
  needed to prove rather than argue: `delete` reclaims both storage tiers, and a refused duplicate
  writes no GridFS document.
- **`DatabaseStorageServiceTest.springDataConvertsAnIdStringToAnObjectIdSoTheLookupMatches` asks Spring
  Data's own `QueryMapper`** rather than asserting a belief about it — the right shape for a claim about
  a framework, and the reason the "the GridFS query is broken" reading was caught before it was fixed.
- **Both `TestDatabaseTimeoutTest` and the two `DownloadRowMappingTest` guards were mutation-verified**,
  with the mutation quoted in the commit message.
- **No secrets, no injection in the changed query paths.** The only Mongo queries built from input are
  `findFirstBySha256` (a derived query, parameterised) and the GridFS `_id` criteria.

### Suggested tests, in the order they would pay

1. Replace A-1's fixture so `land` actually fails (root as a regular file) — restores a guard that
   currently protects nothing.
2. A `MockMvc` case for 415 on a non-multipart upload.
3. `WebPersistenceIT`: run both crons against seeded rows — one orphaned file, one orphaned row, one
   empty repository — which is the only destructive path still unproven against real data.
4. A frontend case pinning that a failed upload leaves the pickers untouched and does not throw.
5. Assert served bytes and `contentLength` in `ArchiveStreamingTest`.

### Resolution — closed the same day (2026-09-23)

| Finding | Closed by | Evidence |
|---|---|---|
| A-1 | `test(app): repair a guard that stopped reproducing its own condition` | The failure is now produced by making the storage **root** a regular file, so it no longer depends on the filename the fix sanitises, and the guard additionally asserts the result is empty rather than merely non-throwing. Mutation-verified: letting `IOException` escape `land()` fails it. |
| A-2 | `test(app): pin that an upload name cannot inject a Content-Disposition parameter` (red), `fix(app): build the Content-Disposition header instead of interpolating it` | Both handlers build the header with `ContentDisposition`. **The guard took three attempts**, and the middle one is the lesson: parsing the header back and comparing to the original name passed against the injection, because Spring's own `ContentDisposition.parse` is lenient and recovers `evil".zip"; filename="other.exe` from the escaped *and* the unescaped form alike. The assertion is now on the raw header — remove the escaped pairs and exactly two quotes must remain. |
| A-3 | `test(app): repair a guard that stopped reproducing its own condition` | One `MONGOD_VERSION`, referenced by both annotations through `VERSION_PROPERTY` and derived by the probe as `"V" + MONGOD_VERSION.replace('.', '_')`. `WebPersistenceIT` still runs 5→7 tests with 0 skipped, i.e. the probe still starts a real mongod rather than silently skipping. |
| A-4 | `test(app): cover the 415 refusal and both sweeps against a real database`, `test(frontend): ...`, `test(app): pin that an upload name ...` | All four gaps closed. **The cron guard failed on its first run and that is why it was worth writing:** deleting the only `modPack` row left the repository empty, which triggered the empty-repository *refusal* added earlier in the pass, so the GridFS twin survived and the assertion read as a leak. Two behaviours from this pass interact, and the mocked schedule tests could not show it — they stub `findAll()` per case and never see one case's premise invalidate another's. |
| A-5 | `test(app): repair a guard that stopped reproducing its own condition` | The comment cited `include-stacktrace=ALWAYS`, which a later commit in the same pass set to `NEVER`. Rewritten with the guard. |
| A-6 | `fix(app): reclaim an orphaned pack once, not once per directory entry` | A set of reclaimed ids. Mutation-verified: removing it fails `anOrphanWithBothAnArchiveAndAnExtractedDirectoryIsReclaimedOnce`. |
| A-7 | Not fixed — accepted | `reportFailure` persists whatever state the failed task reached. The row is being marked `ERROR`, which is the point; a half-applied `projectID`/`versionID` on a row that is about to be reaped by `DatabaseCleanupSchedule` is not worth a second save path. |
| A-8 | Not fixed — accepted | The concurrent-upload race is tolerated by design: `findFirstBySha256`'s `First` exists for it and its KDoc says so. Pinning the tolerated outcome would need two real uploads racing inside one test, which buys less than it costs. |

**Three guards in this pass passed for the wrong reason, and mutation found all three** — the C1 fixture
that stopped reproducing its own condition, the parse-based header assertion, and the frontend error
path whose `TypeError` became an unhandled rejection. Reading found none of them. That is now four
instances in this repository's log of the same failure mode, and the only technique that has ever caught
it is breaking the production code and watching.

---

# 2026-09-25 — test-depth & defect analysis of the `servertest` branch

Scope: `serverpackcreator-plugin-servertest` in full, plus the `-api` and `-clientside` changes on the
branch. Companion to the same day's section in `REFACTOR-AUDIT.md`, which covers commit hygiene; this
one covers depth, edge cases, defects and security. Findings confirmed by measurement say so; the rest
are marked PLAUSIBLE.

## HIGH

### A1 — the whole launch orchestration is untested, and it is logic, not rendering

`gui/ServerTestTab.launch` is ~60 lines of decisions: refuse an already-running pack, allocate a port,
allocate a *second* port only when RCON is on, borrow `server.properties`, build the session, register
it before starting it, and release the port and give the file back exactly once however the run ends.
None of it is covered. The module's testing stance — "pane rendering is untested by design" — is being
applied to something that is not rendering.

The `givenBack` `AtomicBoolean` is the sharpest example: it exists precisely because the give-back path
is reachable twice, and nothing asserts that it is not run twice, nor that it runs at all when `borrow`
throws.

**This is an MVC finding as much as a coverage one.** A view is holding the controller. Extracting the
orchestration into a `ServerLauncher` in `core` makes every branch above testable headless and leaves
the tab doing what a tab should.

**Suggested cases:** starting a pack already running refuses and allocates nothing; a `borrow` that
throws releases both ports and leaves no backup; RCON off allocates one port, RCON on allocates two;
the close callback releases exactly once when it is invoked twice; a pack with no free port refuses
without touching `server.properties`.

### A2 — three HTML-injection surfaces, two confirmed live

Carried over from `REFACTOR-AUDIT.md` H1–H3 because they are defects, not hygiene:

- **Pack table (CONFIRMED).** `getColumnClass` answers `Object`, so the HTML-disabled renderer is never
  consulted: `rendered view is HTML = true`.
- **`warn()` dialog (CONFIRMED).** A String message to `JOptionPane` installs an HTML view
  (`htmlViews=1`); the same text as a `PlainTextRendering.label` does not (`htmlViews=0`).
- **Console sub-tab titles (PLAUSIBLE).** `addTab(pack.name, …)` puts user-controlled text through
  `BasicTabbedPaneUI`'s `htmlViews`; not measured.

The input in all three is a *server-pack directory name*, which the user creates and which an
imported/downloaded modpack can influence.

### A3 — `ServerTestTab.warn`'s doc comment is false

`gui/ServerTestTab.kt:196` — *"Say something the user needs to act on, rendered as literal text."* It is
rendered as HTML, as A2 measured. The root `CLAUDE.md` is explicit that a stale comment is worse than
none; this one actively asserts the safety property that is missing, which is how the gap survived
review.

## MEDIUM

### A4 — `consoles` is an unsynchronised `HashMap` crossing threads

Written on the EDT, read on each session's reader thread (`onLine`, `onState`). See `REFACTOR-AUDIT.md`
M1. No test could have caught it; it is a reading finding.

### A5 — console panes are never released

No close control on a sub-tab, and `consoles`/`lastStates` only grow. One `JTextArea` of up to
`consoleScrollback` lines is retained per pack ever launched. Also a shortfall against the approved
plan, which specified closable sub-tabs.

### A6 — error paths are implemented and unexercised

Each of these is handled in code and asserted nowhere:

- `ServerSession.send` before `start()` — `standardInput` is null, returns `false`.
- `ServerSession.stop()`/`kill()` on a never-started session.
- A throwing `onLine` listener — `emit` wraps it in `runCatching` specifically so a bad listener cannot
  kill the reader thread; nothing proves it.
- `ServerPropertiesPatch.borrow` against a read-only `server.properties` (a generated pack ships it
  `rw-r--r--`, but a user may chmod it, and the pack may sit on a read-only mount).
- `ServerPropertiesPatch.rconEnabled()` with no properties file at all.
- `ServerPackCatalog` against a pack directory it cannot read.

### A7 — `@Synchronized` is claimed and never exercised concurrently

`PortAllocator.allocate/release` and every `SessionRegistry` method are `@Synchronized` because they are
reached from the EDT and from reader threads. Every guard is single-threaded. A guard that hammered
`allocate()` from N threads and asserted N distinct ports would make the annotation load-bearing.

### A8 — `ServerPropertiesPatch` misses two realistic file shapes

- A file carrying `server-port` **twice** (legal in `.properties`; Minecraft takes the last). The
  implementation replaces every occurrence, which is right, and nothing pins it.
- `borrow` called twice on the **same instance** without an intervening `restore`. The crash-recovery
  guard uses two instances, which is the cross-process case; the same-instance case is the one a
  double-click on Start would produce.

## LOW

### A9 — a late `Stopping` can overwrite a terminal `Exited`

`ServerSession.stop()`/`kill()` check liveness and then transition. If the process exits in between,
`Exited` is overwritten and the row reads "Stopping…" for good. Cosmetic — `startable` keys on the
registry, not the state.

### A10 — `ConsolePane` aliases a constructor parameter for no reason

`private val packVariables = variables` where the parameter could simply be `private val variables`.
Introduced when `ConsoleHints` was wired in.

### A11 — console wrapping changed behaviour with no guard

`lineWrap` and `wrapStyleWord` are two trivially assertable properties and the defect they fixed was
real (notes cut off mid-sentence).

## Verified clean — do not re-litigate

- **No unused imports** anywhere in the module — checked mechanically across every `.kt` file in
  `src/main` and `src/test`, not by eye.
- **No secrets, no credentials, no network calls.** The plugin talks to the local filesystem and spawns
  one child process.
- **Arbitrary-script execution is properly gated.** The plugin will only run `start.sh` from a directory
  containing ServerPackCreator's own `manifest.json`, which is what stops it offering to execute any
  `start.sh` a user happens to keep in the server-packs directory. Pinned by
  `ServerPackCatalogTest.skipsADirectoryWithoutAManifest`.
- **Process teardown kills descendants**, in both `-clientside` and the plugin, each pinned against a
  real spawned grandchild rather than a mock.
- **`core` is tested against real processes, real sockets and real files**, not mocks — the one place a
  mock would have agreed with the implementation and taught nothing.
- **The settings guard's vacuity was already found and fixed** by mutation during development; do not
  re-report it. The lesson (a shipped default equal to the code's fallback makes a value assertion
  blind) is recorded in the module's `CLAUDE.md`.

## 2026-09-25 (later the same day) — disposition, and two guards that proved weaker than they read

Every finding fixed. `ServerLauncher`/`LaunchOutcome` now own the launch sequence (A1), with seven guards
covering refusal, port counts, the borrow-failure path and the double-release; `RealPackBootTest` was
rewired to go through it, so the end-to-end exercises the production path rather than a copy that could
drift — re-run against the NeoForge fixture and green (`Done (3.995s)! For help, type "help"`, fixture
byte-identical afterwards). A2/A3 fixed as recorded in `REFACTOR-AUDIT.md`. A4–A8, A10 and A11 fixed with
guards. Module suite 69 → 90.

**A9 could not be pinned, and the attempt is the finding.** The guard written for it —
`stopAndKillAfterExitLeaveTheStatusIntact` — stays **green with the production fix removed**, because
once the process is genuinely dead `stop()` and `kill()` return before reaching `transitionTo` at all.
The race it defends against (liveness checked, process exits, late `Stopping` overwrites `Exited`) is
real and no deterministic test in this suite reaches it. The test was renamed to what it actually pins
and its KDoc says so; the production check stands on reasoning, which is worth having stated rather than
implied by a green tick.

**A7 produced one guard with teeth and one without, which is why it was worth mutating both.**
`concurrentAllocationsNeverCollide` catches `PortAllocator.allocate` losing its `@Synchronized`.
`onlyOneConcurrentRegistrationWinsAPack` does **not** catch `SessionRegistry.register` losing its —
ten threads rarely land in a window a few instructions wide. Both are kept, both documented; they are
not equal evidence and the difference is now written down rather than assumed away.

**The generalising lesson, third instance on this branch.** A guard that reads like a pin may assert
nothing: the settings guard (shipped default equal to the code's fallback), the `PackVariables` anchor
(`lastOrNull` masking a missing `^`), and now these two. In every case the only thing that separated a
real pin from a decorative one was **running it against a deliberately broken implementation**. Mutation
is not a finishing flourish here; it is the only way this project's guards have been shown to have teeth.

---

# 2026-09-25 (third pass) — test-depth analysis of the branch delta

Scope: the 19 commits after `0e46afa61`.

## HIGH

### B1 — the dropdown's wiring is unpinned: it can stop refreshing entirely and every guard stays green

`PackListPane` registers `scriptChoice.addActionListener { onScriptChanged() }`. **Deleting that line
leaves the whole suite green** — measured. The dropdown would then be inert: choosing a different start
script would change no row's Status and no Start button, until something else triggered a refresh.

The existing selection guards call `pane.show(...)` *directly*, which is what the callback eventually
causes but not the callback itself. This is the same shape as the audit's earlier finding that a
registration is not a mitigation until something reaches it: here, a callback is not wired until
something fires the control.

**Suggested case, and the one applied:** set the combo's selection through the real component and assert
the callback ran — the only assertion that traverses the listener.

### B4 — an orphaned KDoc block shipped for four commits, hidden by up-to-date checks

`StartScriptSelector.kt:83`: inserting `NO_SCRIPTS_CONFIGURED` put it *between* `known`'s doc comment
and `known` itself, so that block attached to nothing and dokka would have dropped it. Caught by this
repository's own `KDocAttachmentTest`, which exists for exactly this.

**The part worth keeping is why it survived.** Two "full build green" claims were made in between, and
both were true and both were weaker than they read: `./gradlew build` left `:serverpackcreator-api:test`
**UP-TO-DATE**, because nothing under `-api` had changed, and `KDocAttachmentTest` lives there while the
file it was failing on lives in the plugin module. The guard only ran once an unrelated
`build.gradle.kts` edit invalidated the task.

Generalises past this branch: **a cross-module guard is only as current as the module it lives in.** When
work happens in module B and the guard that covers it lives in module A, a green `build` says nothing
until A's inputs change. `--rerun-tasks`, or running A's suite explicitly, is what actually asks.

## LOW

### B2 — two shapes of template key are unexercised

`StartScripts.forTemplateKeys` handles them correctly, and nothing says so:

- **Mixed case.** `forKey("SH")` looks the interpreter up lowercased but keeps the key's own case for the
  file name, so it yields `bash start.SH` — right, because ServerPackCreator generates `start.<key>`
  verbatim from the configured key.
- **A duplicate key** is collapsed by `distinct()`, so an operator who lists one twice gets one entry.

### B3 — an empty template key is accepted and yields `start.`

`forKey("")` produces a `StartScript` whose file is `start.`, offered as *"start. — run directly"*.
Pathological rather than reachable — an empty key in `startScriptTemplates` would already make
ServerPackCreator generate a file called `start.` — and `scriptKeysIn` already refuses to report one, so
the two halves disagree only in a configuration nobody has. Recorded rather than guarded.

## Verified clean — do not re-litigate

- **`GenerationNotifier` publishes on a `CopyOnWriteArrayList`**, so a subscriber cancelling during
  delivery cannot disturb the iteration — the case a plain list would fail on.
- **The sorting comparator was read for the shadowed-`it` trap** (`preferred.indexOf(it.lowercase())`
  inside `takeIf { i -> ... }`) and is correct: the inner lambda names its parameter, so the outer `it`
  is not shadowed.
- **`ServerTestTab.availableScripts` is read once at construction**, so changing the templates setting
  takes effect at the next tab build rather than live. Deliberate and documented in the module's
  `CLAUDE.md`; it is how every other setting a plugin reads behaves.

---

# 2026-10-12 — analysis of `9.1.0..develop`, iteration 1 of 3

Subject: the **72** ServerPackCreator commits of that range (39 of the 111 are imported website history;
see `claude-docs/REFACTOR-AUDIT.md` for the ancestry split that separates them). Baseline: `./gradlew build`
green on `05a58f57f`, **2,206 tests**, 0 failures, 0 compiler warnings.

Every finding below is in code this range *introduced* — the homepage merge and the workflow that drives
it. All four were measured, not inferred.

## HIGH

### H1 — the documentation assembler fetches from a mirror that does not have the file

`serverpackcreator-homepage/docs/assemble-docs.sh:34` defaults the release base to **github.com**, and
`fetch` is deliberately fail-hard. But GitHub is the *outward mirror*, and this repository's own
`.claude/rules/ci-workflows.md` records that the `mirror` job stalled on **5 of 12 releases — 9.2.1 among
them**. Measured today:

```
github.com  …/download/9.2.1/serverpackcreator-api-9.2.1-dokka-html.zip -> 404 (9 bytes)
git.griefed.de …/download/9.2.1/serverpackcreator-api-9.2.1-dokka-html.zip -> 200 (4,480,323 bytes)
git.griefed.de …/download/9.1.0/serverpackcreator-api-9.1.0-dokka-html.zip -> 200 (4,480,480 bytes)
```

`9.2.1` is the **first** entry in `versions.json`, so `homepage-image` dies on its first loop iteration.
The job that `fix(ci): publish the homepage image on every release` exists to make work cannot currently
build at all.

Forgejo is the canonical origin of every release — the root `CLAUDE.md` says so — so the mirror was the
wrong source from the first commit. It is also the host the job runs on, which makes it the near side of
the network rather than the far one.

### H2 — the manifest advertises a version that has no release anywhere

`versions.json` lists `9.2.0`. The **tag** exists on Forgejo (`0f5ef5a8c`), but no release *object* does:

```
Forgejo API /releases/tags/9.2.0 -> "The target couldn't be found."
GitHub  API /releases/tags/9.2.0 -> "Not Found"
Forgejo …/download/9.2.0/serverpackcreator-api-9.2.0-dokka-html.zip -> 404
```

So its API reference is unfetchable from either forge and the hard-fail fires a second time. A tag and a
release are different objects, and `versions.json` was written against the tag list.

(The help half is fine — `griefed/serverpackcreator-help` publishes `9.1.0`, `9.2.0` and `9.2.1`, all
HTTP 200 on Docker Hub. Only the API half is missing, which is why this is a manifest fault rather than a
release one.)

### H3 — the image published for a release never contains that release's documentation

`versions.json` is a **static, hand-curated file**, and `homepage-image` runs `docs/assemble-docs.sh
docs-build` with no version argument. So releasing `9.2.2` publishes a site whose newest documentation is
`9.2.1`, and whose `/help/latest` and `/api/latest` symlinks point at `9.2.1` — until somebody remembers to
hand-edit the manifest and cut *another* release.

The job already holds the answer: `needs.version.outputs.version`. Nothing reads it.

This is the one that matters most against the stated goal, because it fails **silently and successfully** —
a green pipeline, a published image, and documentation one release stale. H1 and H2 at least fail loudly.

## MEDIUM

### M1 — the assembler hard-requires `unzip`, which this runner is documented as not having

`assemble-docs.sh:37` exits 1 without `unzip`. Twelve lines of `docs.yml` above the job that calls it, the
*Check the API entry survived the build* step says in as many words:

> `unzip` is NOT a given on this runner image — that is why the dokka job ships a tar.gz and why
> Dockerfile.help has to `apk add unzip`. python3 is the fallback […]

The established pattern in the same file is unzip → python3 → loud skip. The new script adopts none of it
and converts a known-absent tool into a build failure. (It has not fired yet because H1 fails first.)

### M2 — `serverpackcreator-homepage/CI-NOTES.md` documents a CI that no longer exists

Imported with the subtree and never reconciled. It describes three workflows — `workflows/ci.yml`,
`workflows/build.yml`, `workflows/packages.yml` — and `serverpackcreator-homepage/.forgejo` and
`.github` **do not exist**: `ci(homepage): lift the website's CI to the root, drop what cannot work`
removed them, and `fix(ci): publish the homepage image on every release` folded the remainder into
`.forgejo/workflows/docs.yml`.

It also lists nine credentials — `DOCKERHUB_REPO`, `GHCR_USER`, `GHCR_TOKEN`, `PACKAGES_TOKEN`,
`GIT_USER`, `GIT_MAIL`, `RELEASE_USER`, `RELEASE_TOKEN` and `DOCKERHUB_TOKEN`. The jobs that exist use
**three and one variable**: `secrets.DOCKERHUB_USER`, `secrets.DOCKERHUB_TOKEN`, `secrets.GH_TOKEN`,
`vars.DOCKERHUB_HOMEPAGE_REPO`. Six of the nine are named nowhere in the repository.

A secrets table is read by an operator provisioning a runner, which makes a wrong one worse than an absent
one — it sends somebody to mint four tokens nothing will ever read. Against this repo's own rule that
documentation describes the present tense, the whole file is past tense about a different repository.

## LOW

### L1 — the homepage carries an eslint 8 island nothing runs

`serverpackcreator-homepage/` ships `.eslintrc.js`, `.eslintignore` and `eslint 8.37.0` with
`@babel/eslint-parser`, and `package.json` declares `"lint": "eslint --ext .js,.vue ./"`. **No workflow
invokes it** (`grep -rn homepage .forgejo/workflows/*.yml | grep -i lint` → nothing), and `"test"` is
`echo "No test specified" && exit 0`.

Meanwhile `build(deps-dev): bump eslint 9.39.5 -> 10.12.0` has just moved the *other* frontend to eslint 10
flat config, and `serverpackcreator-web-frontend/CLAUDE.md` records that its legacy
`.eslintrc.cjs`/`.eslintignore` "are gone". The repository now holds both generations, one of them unreachable.

Recorded, not fixed: migrating it is a separate piece of work with its own verification, and an unrun
linter is inert rather than wrong. It belongs in `BACKLOG.md`.

## Verified clean — do not re-litigate

- **A regex scan for dangling `@param`/`@return` tags was run and its output discarded as unsound.** It
  reported 29 `@param`s naming non-existent parameters and 86 `@return`s on Unit functions; both lists are
  false. `params_of` truncated the declaration at 40 lines, so `BootVerifier`'s long constructor looked
  short, and the Unit test keyed on an absent `: Type`, which every expression-bodied function lacks.
  **Do not re-run this scan; ask dokka.** Same failure mode as the name-based call graph that produced
  depth-54 chains before `misc/call-depth.py` moved to bytecode.
- **`KDocAttachmentTest` covers the whole repository**, not just `-api` — it walks from the directory
  holding `settings.gradle.kts` over every `.kt`/`.kts`, skipping `build`, `.git`, `.gradle`,
  `node_modules`, `dist`, `.idea`, and asserts it found >100 files so a mis-rooted scan fails loudly rather
  than passing vacuously. It is green across the doc sweeps, which is the guard those 74- and 79-file
  commits needed.
- **`docs-build/` is correctly excluded from git and correctly included in the Docker context.**
  `serverpackcreator-homepage/.gitignore` has `/docs-build/*` with `!/docs-build/.gitkeep`;
  `.dockerignore` does **not** list it. So the assembled 25 MB/version tree is never committed and always
  copied — the two files have to disagree for this to work, and they do.
- **`ln -sfn "$LATEST"` writes a *relative* symlink**, which survives `COPY docs-build/ /config/www/`
  intact. An absolute one would have pointed outside the image.
- **The `:latest` tag guard in `Compute tags` is correct.** `*[!0-9.]*` matches any version containing a
  character that is neither digit nor dot, so `9.2.2-alpha.1` takes the no-latest arm before `*.*.*` can
  see it. Prereleases cannot become what a bare `docker pull` returns.
- **`node -e` + `process.argv[1]` is right**, and is not the script-mode off-by-one it resembles: with
  `-e` there is no script filename, so the first user argument *is* `argv[1]`.

---

# 2026-10-12 — analysis of `9.1.0..develop`, iteration 2 of 3

Iteration 1 looked at the newest code (the homepage merge). This pass looked at the rest: the Kotlin
and shell changes in the range, test depth, and security. Two findings, both fixed, plus a third found
while confirming the second.

## HIGH

### H4 — a fix landed on one of the two call sites it was written for

`fix(ci): make the continuous release say why it failed instead of printing a traceback` is **in this
range**. Its message names two stacked blindfolds:

> `curl -f` prints NOTHING on an HTTP error […] and the step ran under `set -eu` WITHOUT `pipefail`,
> so a pipeline's status is python's: curl's non-zero exit was masked

It fixed the **Forgejo** step. The **GitHub mirror** step, 35 lines below in the same file, still had
both:

```
set -eu                                    # no pipefail
ID=$(curl -sf -X POST .../releases ... | python3 -c '...json.load(sys.stdin)["id"]')
```

with the comment explaining why that is wrong sitting 42 lines above it. Two more `curl -sf` calls
(the Forgejo asset upload, the GitHub release delete) and a fourth (the GitHub asset upload) could
also fail without printing a reason.

Fixed: all four capture status and body, both GitHub steps gained `-o pipefail`, and the create payload
moved to a heredoc. Verified by extracting **every** `run:` block in `.forgejo/` through the YAML parser
and running `bash -n` — 65 parse, 0 fail — which is what proves the heredoc terminator survives
block-scalar dedenting instead of landing indented and never matching.

This is the fourth recorded instance of *a fix written against one artifact is a fix for one artifact*.

### H5 — a doc sweep left three exported properties undocumented in the published module

`docs: strip @param/@return that only repeat the prose above them` touched 74 files. Dokka reports
exactly three members as `Undocumented`, and all three trace to that commit:

```
- * @param minecraftVersion Minecraft version.          ForgeInstance
- * @param forgeVersion     Forge version.              ForgeInstance
- * @param minecraftVersion The Minecraft version of this server.   MinecraftServer
```

Each was the sole documentation of a **public property of a Maven-published class**. The tell is in the
source without a tool: both blocks still carry `@param` for their *private* parameters while the public
`val`s above them have none — the sweep kept the tags documenting what no reader of the reference can
see and dropped the ones documenting what they can.

The individual removals were defensible: `@param minecraftVersion Minecraft version.` really does only
restate the signature. The conventions anticipate precisely this — *"A doc that only restates the
signature is barely better than none — but silence is worse."* So they are **written, not restored**.

Fixed; dokka goes 3 `w:` lines → 0 for `-api`.

## Out of range, fixed anyway

### 17 dokka warnings predating `9.1.0`

Running dokka across all **eight** modules that apply the convention rather than only the published one
turned 3 findings into 17: two unresolved links (`[selection]` in `LaunchablePack`, which meant
`[StartScriptSelection]`) and fifteen undocumented members. `git log 9.1.0..develop -- <file>` returns
nothing for every file involved, so none are this range's doing — fixed under *"a defect noticed in
passing gets surfaced and fixed in its own commit, never deferred as out of scope."*

Dokka now emits zero `w:` lines project-wide.

## Verified clean — do not re-litigate

- **`fix(api): forward a stop signal to the server in the bash start script` is exemplary**, and is the
  model for this repository's riskiest change class. `test(api): pin that a stop signal reaches the
  server, red` lands *before* the fix; both halves are mutation-verified (a handler forwarding nothing →
  FAILED; `"$JAVA" ${1} &` without the fd-0 redirection → FAILED); the guard itself was repaired before
  being trusted, because its first form read the console from a pipe the JDK closes at exit; and the
  fish and PowerShell exclusions are **measured**, not assumed.
- **The `spch.tree` href templating is guarded on both sides.** `grep -qF` before the `sed` (because
  `sed` matching nothing exits 0 and would ship the Pages link silently), `grep -qF` after, and a
  separate downstream step reads `HelpTOC.json` back out of the *built* zip. A `&` or `|` smuggled in
  through a branch name would corrupt the replacement and then fail the post-check rather than publish
  a wrong link.
- **No secrets and no script-injection surface.** No hard-coded credential pattern in the added
  homepage code or the workflows, and no interpolation of `github.event.*` title/body/message/name/ref
  or `github.head_ref` into any shell — the fields an outside contributor can set.
- **The `curl -sf` calls left in `release-build.yml` are the two documented carve-outs** (VirusTotal's
  submissions, whose failure must not fail a published release), matching
  `.claude/rules/ci-workflows.md` exactly. `qodana.yml`'s two are a notification webhook, same class.
- **The app-vite 3 alias rename is complete.** No `from 'src/…'`, `'boot/…'`, `'stores/…'`,
  `'components/…'`, `'layouts/…'`, `'pages/…'` or `'assets/…'` import survives anywhere in `src/` or
  `test/`; the `~@/assets/…` template form and the `url("@/assets/…")` SCSS form are both present and
  consistent; and **all nine referenced assets exist on disk**. Six assets are referenced by nothing
  (`app.png`, `banner.png`, `fileID.webp`, `projectID.webp`, `prosper.webp`, `sad.svg`) — not from this
  range, so not this audit's business.
- **`9964e485a`'s `timedOut` contract change is recorded** in `claude-docs/API-BEHAVIOUR-CHANGES.md` by
  `8cce5007e`, which is the process working.

---

# 2026-10-12 — analysis of `9.1.0..develop`, iteration 3 of 3

Iteration 1 took the newest code, iteration 2 the rest of the range plus a project-wide dokka pass.
This one took the two rules this repository has written down *because it keeps breaking them*, and
aimed each at the range: **"a benchmark is not a test"**, **"a guard gated on something nobody supplies
is absent, not gated"** — and then followed the thread the second one opened.

## HIGH

### H6 — `docs.yml` never called the action that exists to be the only caller of this decision

`.forgejo/actions/resolve-version` opens with *"The one place that decides what a build calls itself"*,
and its header records the exact failure it was created to end: the prerelease classification and the
tag-shape rejection *"existed in only one of the two, so a malformed tag was caught and a malformed
anything else was not."*

`release-build.yml` and `devbuild.yml` use it. `docs.yml` did not — it hand-rolled

```
if [ "${{ github.ref_type }}" = "tag" ]; then V="${{ github.ref_name }}"; else V=<short sha>; fi
```

so the refusal never ran. Its trigger is `tags: ['*.*.*']`, which Forgejo compiles to `^.*\..*\..*$`,
and that matches far more than a release:

| tag | matches the trigger | `resolve-version` says |
|---|---|---|
| `9.2.2`, `9.2.2-alpha.1`, `9.2.2-beta.3` | yes | accept |
| `v9.2.2`, `test.tag.x`, `release.candidate.1`, `9.2.2.1` | **yes** | **refuse** |

Each of those four would have been taken verbatim as the version — publishing
`serverpackcreator-help:<junk>` (that job pushes) and a help site whose API entry points at
`/api/<junk>/index.html`. Measured by extracting the action's own script and running it against all
seven.

**And the question it answers was being re-answered twice more.** `Compute tags` decided "plain
release?" with `case "$V" in *[!0-9.]*) ;; *.*.*)`, and `assemble-docs.sh` decided it again with
`/^[0-9]+\.[0-9]+\.[0-9]+$/`. Three copies, two of them downstream of the one that is authoritative —
the **fifth** recorded instance of duplicated knowledge drifting toward whichever copy is easier to
reach.

They could disagree, and `9.2.2.1` is exactly where: the `case` matches `*.*.*` and would have pushed
`:latest`, while the regex requires three components and would have left `/help/latest` alone. That it
is unreachable today is only because `resolve-version` refuses the tag — which is the argument for
calling it, not for keeping the copies.

Fixed: a tag goes through the action; the `version` job outputs `prerelease`; both former derivations
are now reads. The script's regex is **deleted** rather than corrected, defaulting to `false` so a
standalone run adds a version without promoting it.

A branch build is still named by its short commit and deliberately does **not** use the action's
`branch` mode, which needs `fetch-depth: 0` for `git describe`.

## Verified clean — do not re-litigate

- **`build(grinder): measure what a restart pays to load the verdict store` obeys the benchmark rule.**
  The file is `StoreWriteBenchmark.kt`, the method is `measureLoadCost`, and the commit *also* adds a
  real asserting `@Test` to `BootWorkspaceReaperTest` whose precondition is asserted after the sweep so
  it **skips rather than passes** when it does not hold. This is the rule working, one iteration after
  `StoreWriteBenchTest` was renamed for breaking it.
- **Every skipped test has a named owner for its gate, or a stated reason for having none.** 28 skips:
  `DockerJavaContainerEngineIT` (9) and `ContainerOwnershipIT` (3) are gated on `GRINDER_DOCKER_IT`,
  which `grinder-container-it.yml` **sets** — and deliberately *not* `test.yml`, whose comment explains
  that the repository-wide concurrency lock is the point. `CurseForgeCrawlLiveIT` (10),
  `CatalogCrawlLiveIT` (3), `GrinderAuditIT` (1) and `ScriptTemplateMatrixIT` (1) are gated on
  `GRINDER_CF_IT` / `GRINDER_LIVE_IT` / `GRINDER_AUDIT_IT` / `GRINDER_TEMPLATE_IT`, which nothing sets —
  and `test.yml` says why in as many words: *"they need a live Modrinth/CurseForge API, a built image,
  or a deployed grinder."* `RealPackBootTest` (1) is a real boot, run on purpose. **Nobody-sets-it with
  a written reason is the answer the rule asks for**, not a finding.
  *Method note:* a `grep -l` for the gate names reports `test.yml` as setting `GRINDER_DOCKER_IT`. It
  does not — that is the comment saying it deliberately doesn't. Parse the YAML and read `env:`.
- **The SPA really is built twice on a release**, once by `homepage-build` and once inside the
  Dockerfile, and `homepage-image` consumes only the `dokka-html` artifact — never `homepage-spa`.
  Deliberate: the Dockerfile builds from its own context so `docker build` works standalone, and
  `homepage-build` exists to be the per-push check that gates the publish through `needs`. The uploaded
  `homepage-spa` is a 7-day convenience copy that nothing consumes, like `test.yml`'s `build-artifacts`.
- **No dangling references after the rewire.** No `steps.resolve.*` survives, and every one of the ten
  `needs.version.outputs.*` reads belongs to a job that declares `needs: version`.
- **Unused imports: none.** `./gradlew build` emits zero `w:` lines, and an unused Kotlin import is a
  compiler warning — so the whole class is empty by construction rather than by scan.

## Closing state

`./gradlew build` **BUILD SUCCESSFUL**, 0 compiler warnings, **2,168 JVM tests + 38 frontend = 2,206**,
0 failures, 28 skipped. `dokkaGenerateHtml` across all eight modules: **0 warnings**. 65 bash run-blocks
parse. `misc/ci-duplication-check.py`: 3 duplicated-and-unexplained, 4 allowlisted — unchanged through
all three iterations.

---

# 2026-10-21 — analysis of `9.2.1..develop`, iteration 1 of 3

Subject: the ~57 commits authored since the 2026-10-12 audit (17 of the range's 74 non-merge commits
were cleared there and are not re-examined), plus test depth across the codebase at `77fdd27d6`.

## MEDIUM

### M1 — `writeAtomically`'s staging cleanup is unpinned, and the comment above it makes a claim no test checks

`serverpackcreator-api/src/main/kotlin/.../utilities/common/FileUtilities.kt:564-568`

```kotlin
} catch (ex: Exception) {
    // Never leave the staging behind: it is invisible to every reader and would otherwise accumulate one
    // per failed attempt, beside the file it failed to become.
    temporaryFile.delete()
    throw ex
}
```

`AtomicTextWriteTest` has four guards and they are good ones — the destination holds the whole text, no
staging survives a *successful* write, a failed write leaves the previous contents intact, and each write
stages into a file of its own. None of them reaches this `delete()`.

The reason is the shape of the failure the third guard provokes: it points the staging file at
`absent-directory/manifest.json.tmp`, so `write(temporaryFile)` throws while *creating* the file. The
staging file never exists, and `temporaryFile.delete()` is a no-op on a path that was never written.

**Mutation-verified.** Replacing `temporaryFile.delete()` with a comment and running
`:serverpackcreator-api:test --tests '*AtomicTextWriteTest*'` → **BUILD SUCCESSFUL**. The branch can be
deleted and nothing goes red.

What it costs if it rots: exactly what the comment says — one invisible `.tmp` beside every manifest and
every cached installer, per failed attempt, forever. The grinder is the reachable case, because it runs N
workers against one shared installer cache.

**Proposed test**, which fails against the mutation and passes against the code as written: let the
staging file be created successfully and make the *move* the thing that fails, by handing
`writeAtomically` a staging file that the lambda writes and then making the destination path
un-moveable-onto — e.g. a destination that is an existing **directory**. Then assert the staging file no
longer exists:

```kotlin
@Test
fun aStagingFileIsRemovedWhenTheMoveFails(@TempDir tempDir: File) {
    val destination = File(tempDir, "manifest.json").apply { mkdirs() } // a directory: the move cannot land
    val staging = File(tempDir, "staged.tmp")

    Assertions.assertThrows(Exception::class.java) {
        destination.writeAtomically(staging) { it.writeText("""{"versions":["one"]}""") }
    }

    Assertions.assertFalse(
        staging.exists(),
        "a staging file that was written and could not be moved must not be left beside its destination"
    )
}
```

### M2 — the recorded "zero compiler warnings" baseline is a measurement artifact; the real figure is 40

`claude-docs/REFACTOR-AUDIT.md`, 2026-10-12 iteration 1, *Verified clean*:

> **`./gradlew build` is green on `05a58f57f` with zero `w:` lines.** Not "no new warnings" — none at all.

That reading cannot fail. Measured on `77fdd27d6`:

| command | `w:` lines |
|---|---|
| `./gradlew :…api:compileKotlin :…app:compileKotlin` (tasks UP-TO-DATE) | **0** |
| the same four modules with `--rerun-tasks` | **40** |

A Kotlin compile task that is UP-TO-DATE does not run the compiler, so it emits no warnings and a grep
for `w:` returns zero whatever the source contains. The number is a statement about Gradle's task graph,
not about the code — the same shape as the entries this repository already records under *a check that
cannot run must skip, never pass*.

The 40 are **not** a regression from this range's work and are not individually alarming: 31 are
`Deprecated in Java` (the `Locale(String)` constructors, Jackson's `fields()` and `readTree(URL)`,
night-config's `valueMap()`), 3 are `Unchecked cast of 'Any!' to 'List<File>'` in drag-and-drop handlers,
4 are `@OptIn` delicate-API notices for `CoroutineStart.ATOMIC`, 1 is a redundant conversion call. Several
are library-driven and would appear without any source change, which is precisely why attribution needs a
baseline that was taken honestly.

**Fix: record the real number and the command that produces it**, so *"no new compiler warnings"* in the
Definition of done has something to compare against. `--rerun-tasks` is the measurement; anything else is
reporting the cache.

## LOW

### L1 — `withLineEndings` has no empty-string case

`LineEndingTest` covers CRLF→LF, lone CR, mixed conventions, LF→CRLF, no-doubling, idempotence and
text without line breaks — genuinely good depth for a function this repository has shipped two silent
template bugs through. `""` is covered only incidentally by the no-line-breaks case. Worth one line,
because the function is reached for every generated run-file and an empty template is a reachable state
(an operator may empty one).

## Verified clean — do not re-litigate

- **`writeAtomically` is safe against the cross-filesystem `ATOMIC_MOVE` failure by construction.** The
  default staging path is `File(absoluteFile.parentFile, "$name.<uuid>.tmp")` — same directory, therefore
  same filesystem. All five production call sites (`ServerPackProvisioner` ×4, `ManifestUpdater` ×1) use
  that default; none passes a staging path of its own. `AtomicMoveNotSupportedException` is an
  `IOException`, so even a future caller that got this wrong would land on the documented contract
  (this file untouched) rather than a surprise.
- **`withLineEndings` is correct for every ordering.** `replace("\r\n","\n").replace('\r','\n')
  .replace("\n", sequence)` normalises before expanding, which is what stops a CRLF source doubling into
  `\r\r\n` and what stops a lone-CR source collapsing into one line — the two failures this repository has
  receipts for.
- **The dead-code deletion left no unused imports or variables.** A forced recompile of all four Kotlin
  modules reports zero `never used`/`unused` warnings, and none of the 40 warnings is in
  `UpdateDialogs.kt`, `GuiSettings.kt` or `GlobalSettings.kt`.
- **Zero unresolved KDoc references** across the published module after the 267-file documentation
  rewrite — the check that matters for a change converting `@param x` into `[x]` prose links, because a
  dangling link is silent.
- **No `@Test` function in the repository lacks a reachable assertion**, 2,181 scanned; the three the
  scan flagged are all its own false positives, verified by reading. Detail in
  `claude-docs/REFACTOR-AUDIT.md` for the same date.
- **Suite green at the tip**: 2,205 tests, 0 failures, 28 skipped, across all eight modules.

---

# 2026-10-21 — analysis of `9.2.1..develop`, iteration 2 of 3

Same subject. Iteration 1 covered edge cases and test depth in the range's own code; this pass went at
the **guards themselves** — the tests that assert against *source text* — and at iteration 1's fixes.

## HIGH

### H1 — six source-text guards do not strip comments, and one of them passes against the defect it exists to catch

`HtmlProofingTest` reads source and strips comments before matching (`withoutComments`, a local helper).
It is the only one of the seven that does:

| guard | strips comments |
|---|---|
| `HtmlProofingTest` | yes |
| `FabricQuiltStepDownTest` | **no** |
| `BootVerifierArtifactSinkTest` | **no** |
| `VerifyClientsideArtifactSinkTest` | **no** |
| `GrinderConfigurationTest` | **no** |
| `ClientsideReadmeFlagsTest` | **no** |
| `GrindTestFixtures` (`grinderMainBody`) | **no** |

`FabricQuiltStepDownTest.everyLoaderLineIsWiredIntoTheStepDown` is the dangerous one, because it asserts
with `text.contains(wiring)` — so a comment **satisfies** it:

```
serverpackcreator-grinder/src/test/.../loader/FabricQuiltStepDownTest.kt:168
    Assertions.assertTrue(text.contains(wiring), "a loader line lost its step-down; …")
```

**Mutation-verified.** `ContainerCandidateVerifier.kt:330` was changed from

```kotlin
        "Fabric" -> LoaderStepDown.newestFirst(apiWrapper.versionMeta.fabric.loaderVersions())
```

to the identical text **in a comment**, with the live branch replaced by `"Fabric" -> emptyList()`.
`:serverpackcreator-grinder:test --tests '*FabricQuiltStepDownTest*'` → **BUILD SUCCESSFUL**.

That is precisely the defect the guard was written for. `serverpackcreator-grinder/CLAUDE.md` describes
the step-down's absence as *"that loader has no fallback when the newest build refuses to install, which
is the defect this fixed"*, and records that Quilt `0.31.0-beta.3` / Minecraft 1.20.6 failed to install
with nothing to fall back to, taking every candidate wanting that tuple with it. The guard cannot see it
come back.

`BootVerifierArtifactSinkTest.onlyOneCallSiteInvokesRunPrepared` fails in the other direction — it
*counts* `runPrepared(` occurrences, so a doc comment mentioning the call inflates the count and reports
"found 2 call sites" about a structure that did not change. It is currently at 1 and correct, but
`BootVerifier.kt`'s doc comments were rewritten wholesale in `25dbe12df`, which is exactly the kind of
change that trips it.

**Fix: strip comments before matching, following `HtmlProofingTest`'s own precedent.** The repository
already has the pattern and the reasoning written down; six guards simply do not use it.

## Verified clean — do not re-litigate

- **Every function the range added to a main source is reachable from a test.** 15 added (9 private);
  the one name no test mentions is `BundledJars.nonBlankStringOrNull`, a private extension covered
  behaviourally by `aBlankRangeYieldsNoOpinion` and `aRangeThatIsNotAPlainStringYieldsNoOpinion`.
- **`ManifestIntegrity` has real depth**, and it is the piece most likely to be shallow because its
  failure is a startup crash: truncated JSON restored, truncated XML restored, an intact manifest left
  byte-identical, a broken manifest with no shipped copy removed, the ETag sidecar discarded with it, and
  the restored file asking for the full upstream copy next time. Six guards, one per side effect.
- **No hardcoded credential** in any tracked Kotlin, Gradle, YAML or properties file under the module
  sources, `.forgejo`, `.github` or `buildSrc`. The CurseForge key is read from the environment only, and
  travels as an `x-api-key` header that `JdkHttpFetcher` never logs.
- **`writeAtomically`'s staging cleanup is now pinned** — iteration 1's `aStagingFileIsRemovedWhenTheMoveFails`,
  observed red against the mutation that deletes the branch and green against the code as written.

---

# 2026-10-21 — analysis of `9.2.1..develop`, iteration 3 of 3

Same subject. Aimed at the two guards iteration 2 left, at ground neither earlier pass covered, and at
both iterations' own fixes.

## MEDIUM

### M1 — `grinderMainBody` brace-matches over raw source, so a comment can move the end of the window

`serverpackcreator-grinder/src/test/.../GrindTestFixtures.kt:113`

The helper cuts `main`'s body by counting braces from its opening one, over text that still contains
comments. A `{` or `}` inside a comment in `GrinderApplication.main` therefore shifts where the window
closes. Its own KDoc guards the *other* direction — it asserts the window stops before the declarations
that follow `main`, "exactly how a guard built on it would keep passing while asserting nothing" — but a
comment can end the window early just as silently, and five guards are built on it.

The same read also carries iteration 2's H1: every consumer asserts with `contains()` over the result, so
a comment naming a wiring satisfies a guard whose code was deleted.

**Both fixed by stripping comments before the brace walk.** Mutation-verified through a consumer rather
than on the helper: `GrinderApplication.kt:283`'s `host = bindHost` was replaced by
`host = "127.0.0.1"` with the original line left beside it as a comment, and
`ReportBindWiringTest` → **BUILD FAILED**, where the raw-text version passed.

### M2 — a commented-out `@Option` counts as a declared flag

`serverpackcreator-app/src/test/.../ClientsideReadmeFlagsTest.kt:56`

`declaredFlags()` scrapes `names = [...]` out of each command's source to decide which long-form options
the CLI really accepts, and cross-checks the README against them. A commented-out `@Option` is counted,
so the guard would call a README mention of a removed flag legitimate — the exact phantom it exists to
find. Fixed the same way.

## Verified clean — do not re-litigate

- **The frontend suite passes**: 38 tests, 15 files, vitest 5.0.3. Unrun by anything else this session,
  and not covered by the Gradle suites.
- **Six of six source-text guards strip comments now**, up from one of seven. Three proven by mutation to
  catch what they previously missed.
- **Iterations 1 and 2 fixes still green**: `aStagingFileIsRemovedWhenTheMoveFails`,
  `emptyTextIsUnchanged`, and the four guards repaired in iteration 2.
- **`(?<!:)//.*` rather than `//.*` where the source can hold URLs.** A bare line-comment strip mangles
  `https://…` inside a string literal into `https:`; it is harmless for the tokens the iteration-2 guards
  match and is not harmless for `main` or the CLI commands.

---

# 2026-10-21 — analysis of `9.2.1..develop`, round 4

New ground: resource lifetimes, discarded failure returns, and the skip gates. Rounds 1-3 covered
conventions, test depth, source-text guards, secrets, boundaries and the frontend.

## MEDIUM

### M1 — `WebUtilities.getResponseAsString` leaks its stream on the path it documents as expected

`serverpackcreator-api/src/main/kotlin/.../utilities/common/WebUtilities.kt:365`

```kotlin
@Throws(IOException::class)
fun getResponseAsString(url: URL): String {
    val `in` = BufferedReader(InputStreamReader(openTimedStream(url)))
    ...
    while (`in`.readLine().also { currentLine = it } != null) { response.append(currentLine) }
    `in`.close()          // skipped whenever readLine() throws
    return response.toString()
}
```

`close()` is the last statement of the happy path, not a `finally`. The method is annotated
`@Throws(IOException::class)` and `readLine()` over a network stream is exactly where one comes from,
so the leak is on the path the signature advertises. What leaks is a socket and its connection, not a
file handle.

**It has zero callers inside the repository** — and that is what makes it worth fixing rather than
deleting. `WebUtilities` is exported from `-api`, which publishes to Maven Central, so the only code
that can reach this is an embedder's, and the compatibility policy keeps the signature for at least a
major. Fix the body; leave the surface alone.

### M2 — `JavaConfig.testJava` drains its process wrongly, prints to stdout, and leaks both on failure

`serverpackcreator-api/src/main/kotlin/.../settings/JavaConfig.kt:254`

```kotlin
val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
while (bufferedReader.readLine() != null && bufferedReader.readLine() != "null") {
    println(bufferedReader.readLine())
}
bufferedReader.close()
process.destroyForcibly()
```

Three defects in six lines:

- **`readLine()` is called three times per iteration.** One line is tested for `null`, a second is
  compared against the literal string `"null"`, and a *third* is printed — so two lines in three are
  discarded and the printed one can itself be `null`. `java -version` emits three lines, so in practice
  this prints the third or `null`.
- **`println` writes to stdout from library code**, in a class that holds a logger. The four `println`s
  in `ApiProperties` are a deliberate exception — that class *is* log4j's `ConfigurationFactory`, so
  logging does not exist yet when they run — and this one is not in that category. It reaches the
  console of every GUI and CLI user whose Java path is being validated.
- **`close()` and `destroyForcibly()` are both skipped if `readLine()` throws.** The `catch (e:
  IOException)` below logs and returns `false` without closing either, so a failing probe leaks a reader
  *and* leaves the `java -version` process behind.

The return value is unaffected by all three, which is why `JavaConfigTest.javaPathsAcceptOnlyValidInstallations`
passes and will keep passing — it pins accept/reject, which is the contract. That is the finding: the
contract is pinned and the body is not.

## Verified clean — do not re-litigate

- **`ThemeManager:222` is not a leak, checked in bytecode rather than assumed.**
  `FlatLaf.setup(IntelliJTheme.createLaf(FileInputStream(themeFile)))` looks like a classic inline-stream
  leak. `javap -c` on `flatlaf-3.7.2.jar` shows `IntelliJTheme(InputStream)` wrapping it in an
  `InputStreamReader` inside try-with-resources — two `Reader.close()` call sites plus
  `Throwable.addSuppressed` — so the `FileInputStream` is closed by the library.
- **The other resource sites are correct**: `ServerPackProvisioner:441` uses `zip.use { }`, and
  `ApiProperties.getXmlConfig` closes its stream explicitly.
- **`ServerPropertiesPatch.restore` throwing cannot strand a port.** `ServerLauncher:132` calls it as
  `runCatching { patch.restore() }` with `allocator.release(serverPort)` on the next line, so the
  give-back completes either way. Leaving the backup behind on a failed restore is correct, not a leak —
  its presence is the crash marker the next `borrow()` restores from.
- **All 28 skipped tests map to documented gated integration classes**, and no new silent skip has
  appeared: `CurseForgeCrawlLiveIT` 10, `DockerJavaContainerEngineIT` 9, `CatalogCrawlLiveIT` 3,
  `ContainerOwnershipIT` 3, `GrinderAuditIT` 1, `ScriptTemplateMatrixIT` 1, `RealPackBootTest` 1. Each
  appears in `serverpackcreator-grinder/CLAUDE.md`'s gate table or the servertest module's, with the
  reason its gate is unset.
- **The 44 discarded filesystem booleans were reviewed and are best-effort cleanup**, where a failure
  changes nothing a caller could act on. The one with a real consequence —
  `ManifestIntegrity.setLastModified(0)`, whose failure would strand a host on the shipped snapshot — is
  immediately followed by the next refresh asking unconditionally anyway, and the file is already
  restored by that point.

---

# 2026-10-21 — analysis of `9.2.1..develop`, round 5

New ground: swallowed exceptions and unbounded I/O in **build logic**, which every earlier audit of this
rule explicitly scoped itself out of.

## MEDIUM

### M1 — the build makes an unbounded network call, and the landmine that forbids it audited only `-api` and `-app`

`buildSrc/src/main/kotlin/de/griefed/common/gradle/LicenseAgreementRenderer.kt:144`

```kotlin
if (!license.url.isNullOrBlank() && license.url.endsWith(".txt", true) && Files.maybeLicenseUrl(license.url)) {
    try {
        text = append(text, URI(license.url).toURL().readText() + "\n")
    } catch (_: Exception) {}
}
```

`URL.readText()` opens a stream with the JDK's defaults, and `serverpackcreator-api/CLAUDE.md` states
what those are: *"The JDK's default connect- and read-timeout is infinite, so `url.openConnection()` or
`url.openStream()` written anywhere else is a hang waiting to happen."* That landmine closed twelve call
sites and ends by telling the next reader to *"grep for `openStream()`/`openConnection()` across `-api`
and `-app`"* — which is exactly the blind spot: **`buildSrc` is a separate build and was never in that
grep.**

It is the only network call in `buildSrc`'s main source, and `generateLicenseReport` is on the build
path (`build.gradle.kts:197`). A POM licence URL ending in `.txt` whose host accepts the connection and
then never answers stalls the build with no timeout and no output — on a developer machine and on CI
alike. The `catch` cannot help: a hang never throws.

**Fix:** a bounded read local to `buildSrc`. It cannot use `-api`'s `URL.timedConnection`, because
buildSrc is a separate build that does not depend on the modules — which is the same reason the landmine
gives for that function existing at all.

### M2 — the one silent catch in the repository with no stated reason

Same line. The convention is explicit: *"**Errors:** always handle, never ignore with `_` unless
intentional (comment why)."* Measured across every main source: **38 of 39 silent catches carry their
reason**; this is the one that does not, and it is swallowing `Exception` — which includes the
`IOException` the unbounded read above would produce if it ever did fail rather than hang.

## Verified clean — do not re-litigate

- **The convention on silent catches is being kept**, which is worth recording because the raw count
  looks alarming. 39 `catch` blocks in main sources discard their exception; **38 state why**, including
  the one that looks worst out of context — `ConfigCheckTimer:76` swallowing an `OutOfMemoryError`,
  which carries three lines explaining that a pathologically large server-icon must not take the GUI
  down with it and that the icon simply is not validated on that tick.
- **No new silent skip, no new `!!`, no `TODO`/`FIXME`** anywhere in main sources.

---

# 2026-10-21 — analysis of `9.2.1..develop`, round 6

New ground: shared mutable state and concurrency on the published surface — the defect class this
module already carries two receipts for.

## MEDIUM

### M1 — `updateFallback()` empties the live clientside-mod list and refills it, on a list read from another thread

`serverpackcreator-api/src/main/kotlin/.../settings/UpdateConfig.kt:146`

```kotlin
generationConfig.clientsideMods.clear()
generationConfig.clientsideMods.addAll(newBlacklist.split(","))
```

This is the **same two-step** `GenerationConfigTest.regexVariantListsAreNotSharedBetweenReads` already
pins against, in the one path that was not fixed when its siblings were. That guard's own doc states the
reason: *"a caller holding an earlier result silently saw it emptied and rewritten under them, and
because the mutation is not atomic a concurrent reader could observe the set mid-clear."*

**It is reachable at runtime, not only at startup.** `UpdateDefaultModslistItem` wires
`apiProperties.updateFallback()` to an Edit-menu `ActionListener`, so it runs on the event dispatch
thread on a click — while `ConfigCheckTimer` validates from a coroutine every 500 ms after a keystroke
and `ControlPanel.generate` builds a `PackConfig` on `guiProps.generationDispatcher`.

`TreeSet` is not thread-safe, so the exposure is **not merely a stale read**: a structural modification
during another thread's traversal is undefined, and the window spans a whole `split(",")` of the
fallback list, not an instant.

**Fix:** `replaceClientsideMods` / `replaceModsWhitelist` build the new set and publish it in one
assignment to a now-`@Volatile` field, so a reader sees the old complete list or the new complete one.
`loadFallbackModsList` / `loadFallbackWhitelist` were moved onto the same path. Pinned by
`replacingAFallbackListPublishesANewSetInsteadOfEmptyingTheLiveOne`, which asserts **identity** —
the property that makes the window impossible — and was verified red against restoring the two-step.

Recorded in `claude-docs/API-BEHAVIOUR-CHANGES.md`: contents and declared type are unchanged, but an
embedder holding a reference across an update now keeps the complete old set instead of watching it be
rewritten.

## Verified clean — do not re-litigate

- **Returning copies from the fallback getters would have been wrong, and was checked before being
  ruled out.** `ApiProperties.clientsideMods` / `modsWhitelist` hand out the live set by design —
  `UpdateConfig` and `GenerationConfig.loadFallbackModsList` mutate through exactly that reference, so a
  defensive copy would have made the fallback update a silent no-op. The fix had to be atomic
  *publication*, not defensive copying.
- **No caller mutates any `fallback*` set.** `fallbackDirectoriesInclusion`, `fallbackDirectoriesExclusion`,
  `fallbackZipExclusions`, `fallbackPostInstallCleanupFiles`, `fallbackPreInstallCleanupFiles` and
  `fallbackUpdateProtectedPaths` are handed out live but are never written through, in any module. Latent
  exposure on published API, no active defect, and narrowing their declared types would be a
  source-breaking change for no measured gain.
- **`versionmeta`'s immutable-snapshot fix is intact** — ten classes publish fresh collections into
  `@Volatile` fields, and the `@Volatile` count per file is unchanged from what the earlier work recorded.
