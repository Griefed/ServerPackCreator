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
