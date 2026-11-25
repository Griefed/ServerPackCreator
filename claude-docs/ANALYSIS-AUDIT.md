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

---

# 2026-10-21 — analysis of `9.2.1..develop`, round 7

New ground: equality contracts, and cross-shell consistency of the shipped start-script templates.

## HIGH

### H1 — `start.fish` swallows every stop request, so a fish-started pack is SIGKILLed with its world unsaved

`serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.fish`

The bash template traps `TERM` and forwards it to the server, and says exactly why:

> *"Without this the signal dies here. A pack run under docker or systemd has this script as PID 1, and
> the kernel discards a signal aimed at PID 1 unless a handler is installed for it — so the request is
> lost, the stop window expires, and the server is SIGKILLed with its world unsaved."*

**The fish template has no handler at all** — no `trap`, no `--on-signal`, and it runs the server in the
*foreground*, so there is nothing to forward from. `stopServer` is the only function present in
`default_template.sh` and absent from `default_template.fish` (18 vs 17 functions).

**Reproduced, not reasoned about.** fish as PID 1 in `alpine:latest`, the template's own
`runJavaCommand` verbatim, a stand-in server that traps `TERM` and announces it:

```
before SIGTERM:  CHILD-UP
after  SIGTERM:  CHILD-UP          ← the child never heard it
container still running: yes
```

`docker stop` would wait out its whole window and then SIGKILL. The recorded bash measurement for the
same shape is *5096 ms, exit 137* against *72 ms, exit 0* once forwarding works.

`start.bat` has no handler either; Windows signal semantics differ enough that it is a separate
question. `start.ps1` likewise.

### H1a — the obvious fix is wrong, and that is the finding worth keeping

Four shapes were executed against a real fish in a container. **Each satisfies one half of the
contract and breaks the other:**

| shape | forwards SIGTERM | preserves the server's exit code |
|---|---|---|
| foreground (today's template) | **no** | yes (42) |
| background + `wait` | **no** — fish does not dispatch `--on-signal` while blocked in `wait` | untested |
| background + poll loop | **yes** (`CHILD-GOT-TERM`, ~1 s) | **no** — `$status` comes back `0`, not `42` |
| background + poll + status file | untested for signal (handler did not fire) | yes (42) |

The exit code is not optional: the fish template captures `$status` immediately after `runJavaCommand`
into `server_exit_code` and exits with it, and `serverpackcreator-api/CLAUDE.md` records why — *"a
crashed server is distinguishable from a clean shutdown by anything reading the exit code — systemd,
Docker restart policies, CI, and the grinder's boot classifier."* A fix that forwards the signal and
silently zeroes the exit code trades one silent defect for another.

Two facts established along the way that a future session should not have to rediscover:

- **fish does not run an `--on-signal` handler while blocked in `wait`.** A poll loop between commands
  does let it fire; `wait` does not. This is why bash's trap-plus-`wait` shape does not translate.
- **fish does not redirect a background job's stdin to `/dev/null`.** bash does, which is why the bash
  template needs `0<&0`; the fish equivalent does not, and the console survives backgrounding
  (`CHILD-STDIN:say hello` observed in every backgrounded shape).

**Not fixed in this round, deliberately.** This is a shipped artifact in the area this repository has
the most receipts for — *"these fail silently: a wrong branch or a mis-sliced version produces a
plausible value, not an error"* — and no shape has yet been executed that satisfies both halves.
Shipping one now would be the guess that convention exists to prevent. Filed as **B52** with every
experiment and its outcome.

## Verified clean — do not re-litigate

- **All eight manual `equals`/`hashCode` pairs agree on their field sets** — `InclusionSpecification`,
  `ModPack`, `ZipResponse`, `ServerPack`, `RunConfiguration`, `ErrorEntry`, `QueueEvent`, `TaskDetail`.
  No class overrides one without the other. A first scan reported five disagreements and was a false
  positive of its own regex, which matched `==` and not `!=`; the implementations use the
  `if (a != other.a) return false` form.
- **Exit-code propagation IS present in all three templates CLAUDE.md claims it for** — `.sh`, `.fish`
  and `.ps1`. A grep for the literal `SERVER_EXIT_CODE` finds it only in bash and reads as a gap; fish
  spells it `server_exit_code` (a lowercase local) and PowerShell `$ServerExitCode` from
  `$LASTEXITCODE`. Three naming conventions, one behaviour. `start.bat` does not propagate it and is
  not claimed to.

---

# 2026-10-21 — analysis of `9.2.1..develop`, round 8

New ground: `@Suppress` annotations and the acknowledged duplicate pairs behind them, plus the
published API spec.

## Findings

**None.** Every check in this round came back clean or inconclusive-by-tooling; nothing was changed.

## Verified clean — do not re-litigate

- **The ten `@Suppress("DuplicatedCode")` pairs have not drifted.** This is the shape with a receipt —
  four prompt loops were copies, two carried the annotation, and *"the annotation was sitting on the copy
  holding the worst bug."* Each acknowledged pair was extracted and diffed with the loader name
  normalised away:
  - `FabricLoader` vs `QuiltLoader`: differ only in a field name (`loaderManifest` / `manifest`) and two
    blank lines.
  - `FabricInstaller` vs `QuiltInstaller` vs `LegacyFabricInstaller`: differ only in local variable
    names (`latestNodeChildren` / `latestChildren`, `nextInstallers` / `nextAllVersions`).

    No behavioural difference in any pair.
- **No `@Suppress` is hiding a flagged problem.** The 243 suppressions in main sources are dominated by
  `unused` (106) and `MemberVisibilityCanBePrivate` (68); 52 of the `unused` are in `-api`, where an
  exported symbol having no internal caller is the normal state for a published module. The two
  self-flagging ones (`RedundantSuppression`, `RedundantNullableReturnType`) are deliberate and
  annotated as such.
- **The published API spec carries no stale schema references.** `Writerside/api-docs.yaml` mentions
  `StartArgument`, `ClientMod` and `WhitelistedMod` **zero** times, and none of those classes exists in
  the source any more — the drift an earlier audit caught has stayed fixed.

## Inconclusive, and why it is recorded rather than reported as a finding

A count of spec operations (44) against controller method-level mappings (46) looked like two
undocumented endpoints. It is **not** evidence: reconstructing a Spring path means composing a
class-level `@RequestMapping` with a method-level one and normalising the inline regex constraints
(`{id:[0-9a-zA-Z]+}` → `{id}`), and the parser written for this got that wrong — it reported
`/api/v2/modpacksbyserverpack/{id}` as missing from the spec when the controller's real path is
`/api/v2/modpacks/byserverpack/{id}`, which the spec documents correctly.

The repository already says what to do here instead, and it is not a hand-comparison: *"springdoc is
wired into `-app` as `developmentOnly`, and the regeneration command sits beside that dependency in
`serverpackcreator-app/build.gradle.kts` … Regenerate it rather than patching it."* Any real answer
about spec drift comes from a regeneration, not from parsing annotations.

# 2026-11-15 — analysis of `8239cb6e9..develop` (198 commits, 155 non-merge)

Range `8239cb6e9149ff0710a1243bdac1e3e8261f96d1..develop` at `069c13d69`: 198 commits, 155 non-merge,
2026-10-12 to 2026-11-15. Reviewed by five parallel read-only reviewers, one per date slice, static only (no
Gradle, npm or Docker runs); slices A and B fell inside the 2026-10-21 rounds' scope and were checked against
them rather than re-reviewed. Commit grouping is judged against the repo's current rule, "One commit per GOAL,
not per edit — Griefed's call, 2026-10-12", which supersedes the command's "one concern per commit" wording.
Slice E covers this session's own commits and every dependency bump.

Slices: **A** 2026-10-12..10-18, c18d38cb7..08b8549f9 (in the 2026-10-21 rounds' scope; re-checked against them); **B** 2026-10-19..10-21, f5c9e819d..97fca093a (in the 2026-10-21 rounds' scope; re-checked against them); **C** 2026-10-23..10-24, 8e224d62e..db025e531 (first review); **D** 2026-10-29..11-11, 4782e4012..f6085c414 (first review); **E** 2026-11-13..11-15 non-dependency commits, plus every dependency commit in the range (first review).

## Findings by severity (index; full text per slice below)

Two findings appear in two slices: **A-A2** and **E-N1** (MongoDB 9.0.8 deployed while docs and the embedded
test server stay on 8.0), and **C-AN-11** and slice E's stale `docs.yml` node comment. Severities for slice E,
and for the few slice-C entries without a label, were assigned by the coordinator from each reviewer's own
ranking.

### HIGH

- **C-AN-1** — the GitHub PAT is sent as a bearer token to gitea.com and git.griefed.de
- **D-N-1** — Any issue opener's chosen mod runs as arbitrary code on the GitHub runner, with the job's secret, a persisted token and a writable cache in reach.

### MEDIUM

- **B-N-2** — Bug (consistency): `updateFallback` parses the list with a raw `split(",")`, while `PropertyStore.getList` drops trailing empties
- **B-N-1** — Bug (concurrency, published API): the clear-then-refill shape d2108dedc removed from `clientsideMods`/`modsWhitelist` survives in five sibling properties, two of them on the READ path
- **A-A1** — the dev stack publishes MongoDB on every interface, with a root password committed in the file.
- **A-A2** — the deployment compose file says "stay on MongoDB 8.0" and runs 9.0; the README says 8.0.34; the suite tests 8.0.5.
- **A-A3** — the configuration-cache problem `4bdd0aa64` filed as "ours, origin unknown" is very probably `-app`'s `processResources.doFirst`.
- **A-A4** — `VersionMeta.updateOrRepair`'s two failure branches are unpinned.
- **A-A5** — `downloadFile` accepts a truncated HTTP body as a complete artifact.
- **B-N-3** — Bug (CI): the news job's asset check accepts a partially uploaded GitHub release
- **B-N-4** — Shallow pin: two of d5d979624's three fixes, and 9f9495975's call site, are unpinned
- **B-N-9** — Stale docs left by the docs-site rework
- **C-AN-2** — the browser refresh replaces the complete baked total with one that is missing Forgejo
- **C-AN-3** — Gitea paging hard-codes the instance cap, and pagination has no bound at all
- **C-AN-4** — the collector has no timeout
- **C-AN-5** — the GridFS fallback leaks its stream and can permanently serve a truncated archive
- **C-AN-6** — resumes and segmented downloads inflate the download counter
- **C-AN-7** — SHALLOW TESTS: Range edge cases are documented but not pinned
- **C-AN-9** — Mongoku's origin does not match the URL the docs give
- **D-N-2** — A streamed export that fails part-way still arrives as a complete 200, and the JSON one is even well-formed.
- **D-N-3** — Paging a store that changes during the fetch can silently skip rows, and the KDoc claims it cannot.
- **D-N-4** — The comment's head is not budgeted, so a many-crash report can still exceed GitHub's 65,536-character limit.
- **E-N1** — Consistency/coverage: production MongoDB 9.0.8 vs a test suite on 8.0.5.
- **E-N2** — Bug risk: the dev-stack fallback is now a downgrade across a major version on a shared volume.
- **E-N4** — Consistency regression: Renovate re-pins the user-facing compose file, undoing `7471aab2c`.
- **E-N5** — Supply chain / reproducibility: the Docker image build now downloads a JDK at build time.
- **E-N8** — Test depth: `48ce85cac` fixes the race only for this class, and the test still uses the real network.

### LOW–MEDIUM

- **C-AN-8** — a failed download is now silent in the SPA
- **C-AN-10** — `Dev stack (with app)` probably drops the developer's `docker/.env` overrides
- **D-N-5** — `withEvidence` and `dataBlockIn` disagree on what the data-block opener is.

### LOW

- **B-N-10** — Duplicated knowledge: da99bab5c fixed the comment-stripping class by copying the regex into each guard, in two dialects
- **B-N-7** — Error handling (CI): the two convention checks share one `set -eu` step, so a red env check hides the duplication check's verdict
- **A-A6** — the "four readers of `variables.txt`" sweep missed the two PowerShell readers.
- **A-A7** — `JarDownloaderRoutingTest` still hard-codes package names.
- **A-A8** — the clientside package graph's acyclicity is documented as "the property to preserve", and nothing checks it.
- **A-A9** — `"incompatible"` in `notRequiredTypes` is unreachable, and two docs claim a path that does not exist.
- **A-A10** — `media` depends on a root `build` that builds nothing.
- **B-N-5** — Missing timeout: `JavaConfig.testJava` blocks until the probed executable closes stdout
- **B-N-6** — Test coverage gap: the env-reference checker has no self-test for its four learned exclusions
- **B-N-8** — Security (LOW): unpinned PyPI install on every CI build
- **B-N-11** — `WebUtilities.getResponseAsString` silently drops line breaks and uses the platform charset
- **C-AN-11** — STALE DOC: the node-version comment no longer matches the value
- **C-AN-12** — a fresh checkout cannot `quasar dev` or type-check without running the collector first
- **C-AN-13** — the SPA still renames downloads client-side
- **C-AN-14** — MISSING TEST: the flashcard images (the B49 symptom)
- **C-AN-15** — MISSING TEST: the homepage route table
- **C-AN-16** — the sponsors probe
- **C-AN-17** — nothing checks that pinned digests still exist
- **C-AN-18** — download-counter details
- **D-N-6** — Crash excerpts skip the console sanitising, and any `-->` in a JSON value shows the data block.
- **D-N-7** — The table never selects "all"; its doc still says it is always offered.
- **D-N-8** — Remaining `runCatching` calls still swallow `Throwable`, the defect class `438581fa4` diagnosed.
- **D-N-9** — The report reads each whole console file to keep 200 lines.
- **D-N-10** — `-lang` as the last argument crashes the parser.
- **D-N-11** — Two flag-matching rules now coexist in one parser.
- **D-N-12** — A JDK advertised for two architectures resolves arbitrarily.
- **D-N-13** — An issue opener can tie up Actions concurrency with long boots.
- **D-N-14** — Build-cache hits across machines are still defeated by other per-machine manifest attributes.
- **D-N-15** — The streaming guard checks a header, not the body.
- **E-N3** — Missing edge case: an in-place upgrade of an existing 8.0 deployment was never exercised.
- **E-N6** — Unused dependency: `@pinia/testing`.
- **E-N7** — Consistency: the homepage stays on `quasar` 2.35.2 while web-frontend moved to 2.35.3.
- **E-N9** — Missing guard: `.dockerignore` versus declared Gradle inputs.
- **E-N10** — Release path not exercised: semantic-release on Node 26.
- **E-N11** — Gradle wrapper integrity (predates the slice, surfaced by `7913de669`).
- **E-N12** — Gate opportunity after `9fa73aee3`.
- **E-N13** — Minor: majors hidden behind ordinary-looking subjects.

## Coordinator verification of reviewer claims

- **C-AN-1 confirmed in code.** `serverpackcreator-homepage/scripts/collect-downloads.ts` passes `GITHUB_TOKEN`
  (`.forgejo/workflows/docs.yml` sets it from `secrets.GH_TOKEN`) into `countAllDownloads`, which hands the same
  options to every counter; `GiteaDownloadCounter` passes them to `fetchReleasePage`, and `releasePages.ts` adds
  `authorization: Bearer <token>` whenever a token is set. The PAT therefore reaches gitea.com and git.griefed.de
  on every homepage build.
- **D-N-1 confirmed in the workflow, cache step inferred.** `clientside-verify.yml` runs on
  `issues: [opened, edited, labeled]` and calls `clientside-report-reusable.yml`, whose report job boots the
  requested mod with `CURSEFORGE_API_KEY` in its environment after a default `actions/checkout@v7` (credentials
  persisted; job token `contents: read`, `issues: write`) and an unconfigured `gradle/actions/setup-gradle@v6`;
  `clientside-accept.yml` (`contents: write`, `pull-requests: write`) runs the same Gradle action. The
  cache-poisoning path rests on that action's documented default of writing the cache from the default branch.

## Slice A — 2026-10-12..10-18, c18d38cb7..08b8549f9 (in the 2026-10-21 rounds' scope; re-checked against them)

**A1 — Security — the dev stack publishes MongoDB on every interface, with a root password committed in the file.**
- Where: `docker/docker-compose-dev.yml:40-44` (`- "27017:27017"`, `MONGO_INITDB_ROOT_USERNAME: root`, `MONGO_INITDB_ROOT_PASSWORD: 1234567890`), the same password at `:81` and `:113`, and the same URI in `.run/WebService (dev stack).run.xml`.
- Introduced by `ff9b20fa2`. Before it, `git show ff9b20fa2^:docker/docker-compose-dev.yml` published no database port.
- Evidence: `92d0049c7 fix(docker): the dev stack's database browser was a deprecated image, two years unbuilt` (2026-10-23) bound Mongoku to `127.0.0.1:8081` because the only auth there was credentials committed in the file, "so it excluded nobody who could read the repository; refusing the LAN is the stronger guarantee". Exactly the same reasoning applies to `27017`, which was left on `0.0.0.0`. On Linux, Docker's published ports bypass ufw/firewalld INPUT rules. The fix went to the one instance in hand and not to the whole class — the shape this repo has already recorded four times.
- Remedy: `- "127.0.0.1:27017:27017"`. The IDE-run app and IDEA's Database window both connect to `localhost`, so nothing loses access. Also consider the dev app's `8080:8080` at `:128-129`.
- Test: a cheap parse-the-YAML check (Kotlin test or a step beside `ci-env-reference-check.py`) asserting every `ports:` entry in `docker/docker-compose-dev.yml` starts with `127.0.0.1:`. Observe it red against the current file.

**A2 — Stale doc / untested runtime — the deployment compose file says "stay on MongoDB 8.0" and runs 9.0; the README says 8.0.34; the suite tests 8.0.5.**
- Where: `docker/docker-compose.yml:41-43` (comment from `7471aab2c`: *"Stay on the 8.0 line … 8.3 and 9.0 images exist and are a deliberate, separate upgrade"*) vs `:52` `mongodb-community-server:9.0.8-ubuntu2204@sha256:…`; `README.md:661` `8.0.34-ubuntu2204`; `docker/docker-compose-dev.yml:37` default `9.0.8`, with `:36` suggesting an override to `8.0.5`; `serverpackcreator-app/src/test/kotlin/.../web/integration/EmbeddedMongoAvailable.kt:72` `MONGOD_VERSION = "8.0.5"`.
- Evidence: `e9f1f6396 deps(deps): update mongodb/mongodb-community-server docker tag to v9` (2026-11-13) crossed the major that the comment forbids. `renovate.json` has no `packageRules`, so the comment was the only "guard" and Renovate cannot read it. Memory notes "Mongo 9" as an accepted item of that batch, so the upgrade may be intended. If it is, the comment, the README sample and the tested version are all now wrong. If it is not, the compose file is. Either way the three disagree.
- Secondary: the dev override hint (`SPC_DEV_MONGO_IMAGE=…8.0.5`) points at a major *downgrade*. A data volume initialised by 9.0 will not start under an 8.0 binary (featureCompatibilityVersion) — unverified against these exact images, but this is MongoDB's documented upgrade/downgrade rule.
- Remedy (for Griefed to choose): either (a) accept 9.0: update the comment, `README.md:661`, `BUILD.md:240` and the dev hint, and decide whether `MONGOD_VERSION` follows (flapdoodle must support it); or (b) hold 8.0: add a Renovate `packageRules` entry with `matchPackageNames: ["mongodb/mongodb-community-server"]`, `allowedVersions: "<8.1"`, and revert the image.
- Test: a check that the major.minor in the compose files' Mongo tag equals `EmbeddedMongoAvailable.MONGOD_VERSION`'s major.minor. That makes "tested against what we ship" mechanical rather than a comment.

**A3 — Build / config cache — the configuration-cache problem `4bdd0aa64` filed as "ours, origin unknown" is very probably `-app`'s `processResources.doFirst`.**
- Where: `serverpackcreator-app/build.gradle.kts:108-118`: `doFirst { val notice = rootDir.resolve(…); check(…) { "… ${notice.relativeTo(rootDir)} …" } }`. Documented as unexplained in `.claude/rules/build-layout.md`'s configuration-cache bullet (*"`:serverpackcreator-app:processResources` holding one, which is ours and is new since 2026-09-26 … worth a `git log -S`"*).
- Evidence: `git log -S'last thing standing'` → `1bcc0340f fix(build): stop tracking the generated third-party licence notice` (2026-10-11). That date sits inside the 2026-09-26 .. 2026-10-16 window the doc names. `rootDir` inside a task action of a build script resolves through the script's `Project` receiver at execution time, which is the classic "task action references Project" configuration-cache problem. **Unverified**: confirming it needs `./gradlew build --configuration-cache --dry-run`, which this review was not allowed to run.
- Remedy: capture outside the action, e.g. `val notice = rootDir.resolve("…")` (and a precomputed relative string) at configuration time, then `doFirst { check(notice.isFile && notice.length() > 0) { … } }`. Re-measure, and update build-layout.md's 11/3 figure either way.
- Test: the measurement itself (problem count before/after) in the commit message, per the "build logic verified by measurement" rule.

**A4 — Test depth — `VersionMeta.updateOrRepair`'s two failure branches are unpinned.**
- Where: `serverpackcreator-api/src/main/kotlin/.../versionmeta/VersionMeta.kt:297-308`; `serverpackcreator-api/src/test/kotlin/.../versionmeta/VersionMetaManifestRepairTest.kt` has two tests (`aTruncatedForgeManifestIsRepairedInsteadOfKillingStartup`, `aTruncatedNeoForgeManifestIsRepairedInsteadOfKillingStartup`), both on the success path.
- Issue: the KDoc and `serverpackcreator-api/CLAUDE.md` both call the rethrow *"deliberately"* load-bearing (*"the alternative is an empty meta"*), yet no test reaches `if (repaired.isEmpty()) throw ex`. A mutation that swallows `ex` (or returns without retrying) would keep both tests green. The second branch — repair succeeded but the retried `update()` throws again — is also untested.
- Related edge: `ManifestIntegrity.restoreIfUnreadable` (`ManifestIntegrity.kt`) treats an **absent** file as unreadable (`!manifest.isFile → false`). For a name the jar does not ship, it then "deletes" a file that does not exist and returns `true`. `updateOrRepair` therefore logs `Repaired <name> …` and retries over a file that was never there. The behaviour is harmless and the log is misleading. `ManifestIntegrityTest` has no absent-file case.
- Tests to add:
  1. `aFailureWhoseManifestsAllParseIsRethrownUnchanged`: manifests intact, a meta whose `update()` throws `IllegalStateException("x")` → `assertThrows` the same instance (`assertSame`), and every manifest byte-identical afterwards.
  2. `aRepairThatDoesNotHelpFailsWithTheRetrysException`: truncate the manifest, make the retried `update()` throw too → the exception propagates and is not swallowed.
  3. `ManifestIntegrityTest.anAbsentManifestTheJarDoesNotShipIsNotReportedRepaired` → decide the contract (probably `false`), pin it, observe red against today's code.

**A5 — Potential bug (unverified) / test depth — `downloadFile` accepts a truncated HTTP body as a complete artifact.**
- Where: `serverpackcreator-api/src/main/kotlin/.../utilities/common/WebUtilities.kt:145-172` (`84ccf4d19`).
- Issue: `84ccf4d19` closed "a killed one cached as complete" for a killed *process*. A connection the server closes early is a separate route to the same outcome. `transferFrom(channel, 0, Long.MAX_VALUE)` stops at EOF and returns a count nobody checks, and the result is compared with neither `Content-Length` nor any checksum. If the JDK's stream reports a short fixed-length body as EOF instead of throwing, the partial file is moved into place and `true` is returned. Per `serverpackcreator-api/CLAUDE.md`, *"callers treat `true` as 'I have this artifact' and `installerFor` never re-downloads an existing file"*, so a truncated installer would be cached permanently (the grinder's shared installer cache is the reachable case). **Unverified** whether `HttpURLConnection` throws ("Premature EOF") or returns -1 for a short `Content-Length` body on JDK 21. `DownloadAtomicityTest`'s four tests use no HTTP server, so the question is open in the suite too.
- Test to add: `aBodyShorterThanItsContentLengthIsNotReportedAsDownloaded`. Use a `com.sun.net.httpserver.HttpServer` on loopback that sends `Content-Length: 1000`, writes 500 bytes and closes, with an existing destination holding known bytes. Assert `downloadFile` returns `false`, the destination is byte-identical, and no `*.tmp` sibling remains. If it goes green as written, the JDK throws and the finding closes. If it goes red, compare `transferFrom`'s count with `contentLengthLong` when that is ≥ 0 and throw `IOException` on mismatch inside the `writeAtomically` lambda.

**A6 — Consistency / coverage — the "four readers of `variables.txt`" sweep missed the two PowerShell readers.**
- Where: `serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.ps1:773` and `default_java_template.ps1:36` (`Get-Content -raw … | ConvertFrom-StringData`). `start.bat` delegates to `start.ps1` (`default_template.bat:33-38`), so PowerShell is the only Windows reader.
- Evidence: `7680f7e8c`'s subject is *"line endings and invisible characters can no longer break a server pack"*, and its message says the four sh/fish readers were *"Found by grepping for the other readers before calling it done"*. The `.ps1` readers are not mentioned anywhere in the message or in `serverpackcreator-api/CLAUDE.md`'s `variables.txt` landmine ("read by FOUR shipped templates"). `VariablesFileLoadingTest` covers sh and fish only.
- Likely outcome by case: BOM is handled (`Get-Content` detects it); CRLF very probably too (`ConvertFrom-StringData` trims each line); **lone CR is unverified**. If the cmdlet splits only on `\n`, a lone-CR file reads as one key holding the rest of the file, and every other setting silently disappears.
- Test to add: extend `VariablesFileLoadingTest` with the two `.ps1` loaders × {LF, CRLF, lone CR} × {BOM, none}, run through `TemplateInterpreterRunner` (pwsh locally or in a container, `--platform linux/amd64`). Check success positively with markers, per the pwsh-on-aarch64 landmine. At minimum, the landmine text should say the ps1 readers were considered and why they are exempt.

**A7 — Shallow guard that will be weakened again — `JarDownloaderRoutingTest` still hard-codes package names.**
- Where: `serverpackcreator-clientside/src/test/kotlin/.../platform/JarDownloaderRoutingTest.kt:61` and `:74` (`Class.forName("de.griefed.serverpackcreator.clientside.platform.BrowserDownloader")`, `…platform.JarDownloaderKt`).
- Evidence: `5658f621b`'s message records that the package move left both strings naming the old flat package, so each guard "went on asking about a package the class could never appear in — still green, and green for ever". The fix re-typed the new literal, so the next move recreates the defect.
- Remedy/test: derive the package from the subject, e.g. `JarDownloader::class.java.packageName + ".BrowserDownloader"`. To prove it: move `JarDownloader` in a scratch worktree and confirm the guard still targets its package.

**A8 — Missing guard — the clientside package graph's acyclicity is documented as "the property to preserve", and nothing checks it.**
- Where: `serverpackcreator-clientside/CLAUDE.md` § Package layout: *"The package graph is acyclic — that is the property to preserve, and the one a careless import silently destroys."*
- Verified today: the **code-level** graph is acyclic and matches the table. The **import** graph is not: `version` imports `boot.BootVerifier`, `selection.BootCandidateSelector`, `metadata.JarSelfDeclaration` and `platform.ModFile`; `platform` imports `verdict.Verdict`; `selection` imports `boot.BootVerifier`; `metadata` imports `selection.BootCandidateSelector`; `dependency` imports `verdict.BootResult`. All of these are KDoc-only (zero non-comment uses, checked file by file), as the CLAUDE.md caveat says. So a reviewer cannot use the import list, and the property is enforced by nobody.
- Test to add: `PackageGraphIsAcyclicTest`. For each main file, collect `clientside.<pkg>.<Type>` references outside `import` lines and comments (reuse `HtmlProofingTest.withoutComments`'s `(?<!:)//.*` stripping), build the package graph, and assert no cycle plus the "May depend on" table. Observe it red by adding a real `version → boot` reference in a scratch edit.

**A9 — Documentation accuracy — `"incompatible"` in `notRequiredTypes` is unreachable, and two docs claim a path that does not exist.**
- Where: `ForgeTomlScanner.kt:53-57` (KDoc: *"`incompatible` is here because it means the mod must not be present"*) and `serverpackcreator-api/CLAUDE.md` (*"It also stays in `notRequiredTypes` so the Forge path never calls it required"*).
- Evidence: `isOptional` is `private` and has exactly one call site, `ForgeTomlScanner.kt:142`, which is reached only after the `continue` at `:125-127` has dropped every `type == "incompatible"` entry. There is no separate "Forge path". The entry is defensive dead data, harmless, but the docs give a reason that cannot happen.
- Remedy: either drop `"incompatible"` from the set (it cannot be observed, so behaviour is unchanged), or keep it and reword both docs to "kept so `isOptional` stays correct on its own if the drop is ever removed". Prefer the first: one statement in one place.

**A10 — Build footgun — `media` depends on a root `build` that builds nothing.**
- Where: `build.gradle.kts:227-244` (`dependsOn(tasks.build, tasks.named("generateLicenseReport"))`); explained in `.forgejo/workflows/artifacts-reusable.yml:332-344` (comments from `b7ca73e58`/`57eadb9b7`).
- Issue: the workflow comment states it plainly — *"nothing here builds the application it installs"*. CI is safe because it restores `gradle-jars` first. `./gradlew media` on a clean local checkout still runs with no app jar in `serverpackcreator-app/build/libs`, and whatever install4j then does is not guarded. `dependsOn(tasks.build)` also reads as if it built the app, which is how the previous "two fat jars" defect in `b7ca73e58`'s message came about.
- Remedy: make `media` fail fast in `doFirst` when `serverpackcreator-app/build/libs` holds no `*.jar` other than `*-plain.jar` (and more than one, which is the scan-order hazard), and drop the inert `tasks.build` edge. Capture the directory outside the action (see A3).

---

## Slice B — 2026-10-19..10-21, f5c9e819d..97fca093a (in the 2026-10-21 rounds' scope; re-checked against them)

**N-1 — Bug (concurrency, published API): the clear-then-refill shape d2108dedc removed from
`clientsideMods`/`modsWhitelist` survives in five sibling properties, two of them on the READ path**
- `serverpackcreator-api/.../settings/GenerationConfig.kt:1029-1038`: the `directoriesToExclude`
  getter runs `field.clear(); field.addAll(use)` on every read. `:1011-1016`
  (`directoriesToInclude`), `:1053-1057` (`postInstallCleanupFiles`), `:1070-…`
  (`preInstallCleanupFiles`) and `:1088-…` (`zipArchiveExclusions`) do `field.addAll(...)` on read, and
  every one of their setters does `field.clear(); field.addAll(value)` on the live `TreeSet`.
- Evidence of concurrent readers: `ConfigurationHandler.kt:745` iterates
  `apiProperties.directoriesToExclude` (`ApiProperties.kt:371-374` delegates straight through) during
  config checks. Those checks run from `ConfigCheckTimer` coroutines and generation in the GUI, and
  concurrently per request in `-app`'s web mode (`SettingsController.kt:56` reads the same property).
  A reader iterating while another read clears the set gets a `ConcurrentModificationException`, or an
  empty window in which no directory is excluded.
- Why the earlier rounds missed it: round 6 "verified clean" covers only the `fallback*` sets (never
  written through) and the regex variants. These are different fields.
- Fix: same shape as d2108dedc. Build a fresh `TreeSet` and assign it to a `@Volatile` field, or return
  a fresh set from the getter (as `updateProtectedPaths` at `:1248-1251` already does). Record the
  change in `API-BEHAVIOUR-CHANGES.md`.
- Tests: (a) `aReadOfDirectoriesToExcludeDoesNotMutateASetAnotherReaderHolds`: hold the result of one
  read, read again, `assertSame`/`assertNotSame` per the chosen design, and assert the held set is
  unchanged. (b) A stress test with two threads, one iterating `directoriesToExclude` and one reading
  it, over N iterations with no CME. Weaker than (a), so add it only alongside (a).

**N-2 — Bug (consistency): `updateFallback` parses the list with a raw `split(",")`, while
`PropertyStore.getList` drops trailing empties**
- `UpdateConfig.kt:146` and `:155` (`newBlacklist.split(",")`, touched by d2108dedc) versus
  `PropertyStore.kt:122-129` (`.dropLastWhile { it.isEmpty() }`).
- Effect: a remote list ending in `,` puts `""` into the live `clientsideMods` until restart. Under
  `ExclusionFilter.START`/`END`/`CONTAIN`/`EITHER`, `ModListCompiler.FilterMatcher.matches`
  (`ModListCompiler.kt:116-121`) answers `true` for an empty entry against every mod name. Whether that
  reaches generation depends on whether the fallback list feeds the exclusion set (**unverified**:
  I did not trace `clientsideMods` into `compileModList`). After a restart the same stored value is read
  through `getList`, so the defect is visible only in the session that performed the update. That makes
  it hard to notice.
- Test: `updateFallback` against a loopback properties server answering `…clientside=a,b,` should give
  `clientsideMods == sortedSetOf("a","b")`. Also `a,,b` and an empty value. Better: route both parsers
  through one function.

**N-3 — Bug (CI): the news job's asset check accepts a partially uploaded GitHub release**
(5d1b96270)
- `.forgejo/workflows/release-build.yml:669-676`: fails only on `COUNT -eq 0`. The mirror upload loop
  (`:511-525`) exits on the first failed upload, so a release holding 1 to 11 of its 12 assets passes.
  The step's own comment calls a release "carrying none … a half-finished mirror", but a release
  carrying some is just as half-finished. With `mirror` in `needs:` for ordering only (`if:` ignores its
  result, by design), this check is the only gate. A mirror that died mid-loop therefore leads to an
  announcement that sends users to an incomplete GitHub release.
- Fix: compare against the Forgejo release's asset count. That is a public GET on
  `$SERVER_URL/api/v1/repos/$REPOSITORY/releases/tags/$VERSION` and needs no token. An alternative is an
  expected count passed as a `mirror` output.
- Test (CI-level, executable via a fixture JSON): run the step body with `gh-release.json` holding 5
  assets and an expected count of 12, and expect exit 1.

**N-4 — Shallow pin: two of d5d979624's three fixes, and 9f9495975's call site, are unpinned**
- d5d979624 pins only the `println` (`JavaConfigTest.validatingAJavaPathPrintsNothingToStandardOut`).
  The `use {}` close, the `finally { destroyForcibly() }` in `JavaConfig.testJava`, and the whole
  `WebUtilities.getResponseAsString` rewrite (zero callers, zero tests) can each be reverted with
  everything green. The message is honest about what it pins, so this is depth, not a false claim.
- 9f9495975: `LicenseAgreementRendererTest.theLicenceReadCarriesBothTimeouts` exercises
  `readBounded` directly. Reverting the call site at `LicenseAgreementRenderer.kt:145` to
  `URI(license.url).toURL().readText()` leaves the test green. This is the grinder CLAUDE.md's recorded
  defect class: "a correct unit no caller reaches … pin the decision through the call the daemon
  actually makes".
- Tests: (a) `getResponseAsString` against a loopback `HttpServer` that writes half a body and then
  resets. Assert `IOException` and that the server observes the connection closed. (b) `testJava` with a
  `pathToJava` script that prints a line and then sleeps: assert it returns and that the child is dead
  afterwards (`ProcessHandle.allProcesses()` filtered by command). (c) Drive the renderer's licence path
  with a `file:` URL through a seam, or assert via a fake `URLConnection` factory that `readBounded` is
  the route.

**N-5 — Missing timeout: `JavaConfig.testJava` blocks until the probed executable closes stdout**
- `JavaConfig.kt` `testJava`: reads to EOF with no bound. A configured "Java" path that never exits (a
  wrapper waiting on stdin, a hung binary) blocks the validating thread indefinitely. That thread is the
  GUI settings path. Pre-existing, and the d5d979624 rewrite kept it.
- Fix: `process.waitFor(timeout)` on a drained stream, or a drain thread plus `waitFor(5, SECONDS)`, then
  `destroyForcibly`.
- Test: a temp script `#!/bin/sh\nsleep 600` returns `false` (or `true`, per the chosen contract) within
  the bound.

**N-6 — Test coverage gap: the env-reference checker has no self-test for its four learned exclusions**
(f5c9e819d)
- `misc/ci-env-reference-check.py`: its message documents four false-positive classes it learned
  (quoted heredoc bodies, mid-line assignments, single-quoted strings and `${{ }}`, PowerShell
  `$env:`). Nothing pins them, and the gate runs only against the real workflows, so a regex regression
  shows up as a red build about the wrong thing, or as silence.
- Also unhandled (latent, no current instance): job- or workflow-level `env:` keys are never checked for
  UNUSED (only step `env`); `defaults.run.shell` is ignored, so a pwsh-default job would be scanned as
  bash and `$env:NAME` would read as a reference to `env`; `RUNTIME_PREFIXES` exempts `GITHUB_*`/
  `FORGEJO_*` only from UNDEFINED, which is right, but a misspelt `GITHUB_TOKNE` read is never reported.
- Tests: a `misc/ci-env-reference-check.test.sh` (the pattern `assemble-docs.test.sh` set) feeding six
  small workflow fixtures and expecting the six verdicts. One of them should reproduce the original
  `WEBHOOK_URL`/`NEWS_WEBHOOK_URL` defect, so the checker's own teeth are pinned outside git history.

**N-7 — Error handling (CI): the two convention checks share one `set -eu` step, so a red env check
hides the duplication check's verdict** (84422876d)
- `.forgejo/workflows/test.yml:130-136`: if `ci-env-reference-check.py` exits 1, `ci-duplication-check.py`
  never runs. Someone fixing the first finding learns about the second only on the next push.
- Fix: `rc=0; python3 a.py || rc=1; python3 b.py || rc=1; exit $rc`. That keeps one step and one pyyaml
  install.

**N-8 — Security (LOW): unpinned PyPI install on every CI build** (5860bcaec)
- `.forgejo/workflows/test.yml:133-134`: `pip3 install --break-system-packages pyyaml` with no version
  and no hash, in the `build` job, which is not a secret-bearing step (checked: no `env:` secrets on it).
  The supply-chain exposure is small but needless. Remedy: `pyyaml==6.0.3`, the version 5860bcaec
  measured.

**N-9 — Stale docs left by the docs-site rework** (3d6b8dc2c, 0fe3d5731)
- `serverpackcreator-help/Writerside/spch.tree:17-19`: says the Dokka merge happens "in two places that
  must stay in step: `docker/Dockerfile.help` … and `.github/workflows/pages.yml`". 3d6b8dc2c deleted
  `docker/Dockerfile.help`. The second half now lives in `serverpackcreator-homepage/docs/assemble-docs.sh`,
  and the comment does not say so.
- `.github/workflows/pages.yml:128-130`: "`spch.tree` links to `api/index.html` RELATIVE to the site
  root, which is what makes one link serve both this deployment … and the container's (`/api/`)". 3d6b8dc2c
  edited this very comment and left the claim. The href is absolute
  (`spch.tree:46`, `https://griefed.github.io/ServerPackCreator/api/index.html`, made absolute by
  `fix(docs): the help site's Dokka link must be absolute, or it never navigates`, 2026-10-03), and the
  "container" deployment no longer exists. `docs.yml:157-165` rewrites it to
  `https://serverpackcreator.de/api/latest/index.html`.
- Remedy: point both comments at `assemble-docs.sh` and at the absolute-href rewrite in `docs.yml`.

**N-10 — Duplicated knowledge: da99bab5c fixed the comment-stripping class by copying the regex into each
guard, in two dialects**
- `git grep '//\.\*' -- '*/src/test/*'` finds 8 copies at develop: `ClientsideReadmeFlagsTest.kt:60` and
  `GrindTestFixtures.kt:119` (`(?<!:)//.*`); `VerifyClientsideArtifactSinkTest.kt:105`,
  `VerifyClientsideJavaSelectionTest.kt:44` (added after the slice), `BootVerifierArtifactSinkTest.kt:144`,
  `GrinderConfigurationTest.kt:172`, `FabricQuiltStepDownTest.kt:191` and `HtmlProofingTest.kt:179`
  (`//.*`).
- The commit's own message calls this "the sixth recorded instance of duplicated knowledge drifting",
  and the fix produced six more copies with a dialect split already in place. A new guard copies
  whichever it finds first. The `/\*.*?\*/` half also eats code when a string literal contains `/*`
  (for example a glob `"mods/*.jar"`). No guarded source has one today (checked
  `GrinderApplication.kt` and `app/cli/commands/*.kt`), so this is latent.
- Remedy: one shared test-fixture function per module (or one in a shared test source) using the `(?<!:)`
  form. Test: feed it `val u = "https://x"; /* c */ val g = "a/*b" // t` and assert the output.

**N-11 — `WebUtilities.getResponseAsString` silently drops line breaks and uses the platform charset**
(touched by d5d979624, behaviour preserved)
- `WebUtilities.kt` `getResponseAsString`: `response.append(currentLine)` with no separator, and
  `InputStreamReader(stream)` with no charset. Preserving this was correct for a fix commit on published
  API, but no doc says so, and an embedder reading a multi-line body gets it concatenated.
- Remedy: document both on the KDoc. Do not change behaviour (published API; the
  `API-BEHAVIOUR-CHANGES.md` policy applies).
- Test: a characterization test pinning `"a\nb"` → `"ab"`, so a later "fix" is a visible behaviour
  change.

---

## Slice C — 2026-10-23..10-24, 8e224d62e..db025e531 (first review)

**AN-1 — SECURITY (HIGH): the GitHub PAT is sent as a bearer token to gitea.com and git.griefed.de**
- `serverpackcreator-homepage/scripts/collect-downloads.ts` passes `token: process.env.GITHUB_TOKEN` into `countAllDownloads`.
  `countDownloads.ts` (`countOnce`) hands the same `options` to every counter, and `src/services/downloads/releasePages.ts:30–31` adds
  `authorization: Bearer <token>` to **every** request, whatever the host. CI sets `GITHUB_TOKEN: ${{ secrets.GH_TOKEN }}` on the
  *Collect the download counts* step (`.forgejo/workflows/docs.yml`, homepage-build). The allowlist note (`misc/ci-env-reference-allowlist.yml:13`) shows
  the intent was GitHub only. So every CI homepage build discloses `GH_TOKEN` (a classic PAT with `read:user`, used for sponsors) to gitea.com, a third party.
- Probable second effect (unverified at runtime): Gitea and Forgejo typically answer an unrecognised bearer token with 401 even on public
  endpoints. If so, both forges fail in CI whenever the token is present, and the baked total silently drops ~14.9k. That is the exact
  "silently low total" the retry was added for, and the retry cannot fix it.
- Introduced by `109b06bdb`; open at develop. The only token test (`test/services/downloads/github-counter.test.js:63`) covers GitHub alone.
- Tests to add: (a) `countAllDownloads(..., { token, fetch })` with all three hosts. Assert that `init.headers.authorization` is present only on
  `https://api.github.com/...` URLs and absent on gitea/forgejo URLs. (b) A per-platform token map
  (`tokens: { github: '…' }`), if that is how it gets fixed.

**AN-2 — BUG (MEDIUM): the browser refresh replaces the complete baked total with one that is missing Forgejo**
- `src/services/downloads/downloadTotal.ts`, `refreshDownloadTotal`. The figure is stale 24 h after the build (`MAX_AGE_MS`). The browser cannot
  reach git.griefed.de (no CORS, as documented), so `report.platforms` holds github + gitea, `length > 0`, and
  `refreshed = { total: github+gitea }` is written to localStorage. Its `generatedAt` is newer than the baked one, so `readDownloadTotal` then
  prefers it. Every visitor 24 h after a build sees a total ~8.5k lower than the build counted, and it persists.
- The documentation says the opposite: `src/pages/Index.vue:779–780` ("A forge that is unreachable from a browser … changes nothing: the
  baked figure already has it") and `serverpackcreator-homepage/CLAUDE.md` ("Nothing breaks without it; the baked figure already includes
  Forgejo"). Both claims are false against the code. Each visit also spends two CORS-failing Forgejo requests (the retry).
- Tests to add: stale snapshot, fetch rejecting for `git.griefed.de` and answering for the others. Assert `snapshot.total >= BAKED.total`,
  or better, that a failed platform keeps its baked per-platform figure (`generated.platforms` is already in the JSON). Also: a refresh that
  yields fewer platforms than the baked report must not be cached as newer.

**AN-3 — BUG (MEDIUM): Gitea paging hard-codes the instance cap, and pagination has no bound at all**
- `src/services/downloads/GiteaDownloadCounter.ts`: `GITEA_PAGE_SIZE = 50`, and the loop stops on `releases.length < 50`. `MAX_RESPONSE_ITEMS`
  is per-instance configuration (50 is only the default). On an instance capped lower, page 1 comes back with e.g. 30 and the counter stops
  after one page, reproducing the silent undercount the module's own LANDMINE warns about for `per_page`. The library is advertised as usable
  against any instance (`countPlatformDownloads(url)`).
- Neither counter has a page ceiling. A server that ignores `page`, or a cyclic `Link` header (`GitHubDownloadCounter.ts`, `nextPage`), loops forever.
- Tests to add: (a) an instance answering 30 per page across 3 pages. Expect all pages read, using `X-Total-Count` (which Gitea sends on list
  endpoints; unverified for Forgejo's releases route) or "continue until an empty page". (b) A stub that always returns a full page must
  terminate with a failure after N pages. (c) A `Link` that points at itself must terminate.

**AN-4 — MISSING ERROR HANDLING (MEDIUM): the collector has no timeout**
- `scripts/collect-downloads.ts` passes no `signal`, and `fetchReleasePage` uses one only if supplied. The collector is "fail-soft" for errors,
  but a forge that accepts the connection and never answers stalls the CI step and the Docker build until the job timeout. Fail-soft does not cover hangs.
- Test to add: a fetch stub that never resolves plus an `AbortSignal.timeout`-style default. Expect the platform to land in `failures`
  within the budget, using fake timers.

**AN-5 — BUG (MEDIUM): the GridFS fallback leaks its stream and can permanently serve a truncated archive**
- `serverpackcreator-app/.../web/storage/FileSystemStorageService.kt`, `cache` (renamed from `store(GridFSFile,…)` in `8e224d62e`):
  `FileUtils.copyToFile(resource.inputStream, destination)`. commons-io's `copyToFile(InputStream, File)` leaves the source stream **open**
  (`copyInputStreamToFile` is the closing variant), so every GridFS recovery leaks a GridFS download stream. This bug predates the slice, but the
  commit rewrote this function, so "fix bugs when you find them" applies.
- It writes straight to the final `${id}.zip`. An `IOException` mid-copy leaves a partial file, and from then on `load` takes the
  `archive.isFile` fast path and serves the truncated file with a matching `Content-Length`, forever. Two concurrent first downloads of the
  same GridFS-only pack race: request B can see A's half-written file as `isFile` and stream it.
- Remedy: `resource.inputStream.use { }`, copy to a temp file in the same directory, then `Files.move(…, ATOMIC_MOVE)`.
- Tests to add: (a) a `resource.inputStream` that throws after N bytes. Assert that `load(id)` then returns empty or recovers again, never the
  partial file. (b) Verify the stream is closed (spy `close()`). (c) Two threads recovering the same id both receive the full bytes.

**AN-6 — BUG (MEDIUM): resumes and segmented downloads inflate the download counter**
- `ServerPackController.kt:65` (`updateDownloadStats(id)`) and `ModPackController.kt:69` run on every GET, including `Range` requests.
  `8e224d62e` deliberately made downloads resumable (an anchor plus pinned 206 support), so a download manager fetching 8 segments, or a
  resumed transfer, now counts 2–8 downloads.
- Tests to add: a `Range: bytes=10-19` request leaves the counter unchanged (`verify(exactly = 0) { serverPackService.updateDownloadStats(any()) }`),
  while `bytes=0-` or no Range increments it once. The same for the modpack route (`ArchiveStreamingTest`).

**AN-7 — SHALLOW TESTS: Range edge cases are documented but not pinned**
- `serverpackcreator-app/CLAUDE.md` (the "download route streams" section) claims 416 on an unsatisfiable range. Only the single-range happy
  path is pinned (`ServerPackControllerTest.aRangeRequestIsAnsweredWithThatRangeSoADownloadCanResume`).
- Tests to add: `bytes=100-` on a 64-byte archive → 416 with `Content-Range: bytes */64`; suffix `bytes=-10` → the last 10 bytes;
  multi-range `bytes=0-1,5-6` → `multipart/byteranges`; `Content-Length == 10` on the 206 (the controller sets the full length explicitly, so it is
  worth asserting the converter overrides it); `HEAD` → no body and no stats increment; and the same on the modpack route.

**AN-8 — MISSING ERROR HANDLING (LOW–MEDIUM): a failed download is now silent in the SPA**
- `serverpackcreator-web-frontend/src/pages/{ModPack,ServerPack}Download.vue`. The removed `downloadWithAxios` showed `$q.notify` on failure.
  The anchor hands a 404/500 to the browser, which shows only "Failed – file wasn't available", while the page says "Downloading — your
  browser is saving the file". The commit message does not mention this behaviour loss.
- `startDownload()` sets `started = true` even when `$refs.downloadButton?.$el` is missing and nothing was clicked.
- Tests to add: with fake timers, after 5 s the countdown clicks the anchor (spy `HTMLAnchorElement.prototype.click`) and shows the
  "Downloading" label; the cancel button stops the click; no "Downloading" claim when the ref is absent.

**AN-9 — BUG (MEDIUM, dev stack): Mongoku's origin does not match the URL the docs give**
- `docker/docker-compose-dev.yml:87` sets `MONGOKU_SERVER_ORIGIN=http://127.0.0.1:8081`, but `BUILD.md:200` and the compose comment at `:59`
  send developers to `http://localhost:8081`. Mongoku compares origins exactly: `src/lib/components/OriginWarning.svelte` on Mongoku
  `master` tests `serverOrigin !== page.url.origin`, and SvelteKit's adapter-node `ORIGIN` CSRF check is exact too. Following the documented
  link therefore gives the warning banner and refused writes, the very regression the comment says the variable prevents.
  (Verified against Mongoku master source; the pinned 2.11.5 is assumed to match.)
- The comment at `:69–72` ("Mongoku has no authentication of its own") is false on master: `MONGOKU_AUTH_BASIC` exists (`src/hooks.server.ts:13`,
  README). Whether 2.11.x has it is unverified. Loopback binding is still the right call; the stated reason is wrong.
- Remedy: point BUILD.md and the comment at `http://127.0.0.1:8081`, or set the origin to `http://localhost:8081`.
- Test to add: none is practical in-repo. A `docker compose config` + grep consistency check between `MONGOKU_SERVER_ORIGIN` and the
  documented URL would do.

**AN-10 — BUG (LOW–MEDIUM, dev tooling): `Dev stack (with app)` probably drops the developer's `docker/.env` overrides**
- `.run/Dev stack (with app).run.xml` sets `envFilePath=docker/app-profile.env`. With Compose, `--env-file` replaces the default `.env`
  lookup, so `SPC_DEV_MONGO_IMAGE`, the override `docker-compose-dev.yml:34–37` tells developers to set in `docker/.env`, is silently ignored
  under this configuration. Whether IntelliJ passes the field as `--env-file` or as process env is unverified, and the commit says so.
- The message's premise that `envFilePath` is "the one option key this repository already proves works, in the existing `Dev stack` config"
  is weak: that config has `envFilePath value=""` (`.run/Dev stack.run.xml:5`), which proves nothing about forwarding.
- Remedy: use the run configuration's Profiles option instead (no env file), or document the interaction.

**AN-11 — STALE DOC: the node-version comment no longer matches the value**
- `.forgejo/workflows/docs.yml:294–298`: the comment says "the image's builder stage is pinned to node:22-bookworm-slim, so 22 keeps the two
  verified against the same major", but the value is `'26'` (and the Dockerfile is `node:26-bookworm-slim`). The comment came from `8521241b1`;
  the value was moved by `5dfdc6d92 deps(deps): update dependency node to v26` (outside the slice) without touching the comment.

**AN-12 — DOC/UX (LOW, runtime unverified): a fresh checkout cannot `quasar dev` or type-check without running the collector first**
- `src/services/downloads/downloadTotal.ts:1` imports `@/generated/download-counts.json`, which is gitignored (`serverpackcreator-homepage/.gitignore`).
  `"dev": "quasar dev"` does not run `collect-downloads`, yet `serverpackcreator-homepage/CLAUDE.md` "Local development" leads with
  `npm ci && npx quasar dev`. CI orders the collector before the type-check for exactly this reason (docs.yml comment).
- Remedy: make `dev` (and `type-check`) depend on the collector, or ship a committed null-total fallback file under another name.

**AN-13 — CONSISTENCY (LOW): the SPA still renames downloads client-side**
- `serverpackcreator-web-frontend/CLAUDE.md` ("Downloads are the browser's") says "Do not reintroduce client-side renaming". Both pages still
  compute `name` with the old renaming logic (`My_Cool_Pack_server_pack.zip`) and bind it as `:download`, and the characterization tests pin
  it. It is only a fallback, because `Content-Disposition` wins, but the fallback name differs from the server's (`My Pack_server_pack.zip`).
  Either drop the attribute value (a bare `download`) or document the fallback as intentional.

**AN-14 — MISSING TEST: the flashcard images (the B49 symptom)**
- `src/pages/Index.vue:693` (`import.meta.glob`) and `:754` (`cardImage`): a wrong glob path or a renamed file yields `undefined` and an
  `<img>` with no `src`, and the build stays green.
- Tests to add: `for n in 1..9: expect(wrapper.vm.cardImage(n)).toMatch(/\.png/)`; mount Index and assert 9 card `<img>` elements carry a
  non-empty `src`.

**AN-15 — MISSING TEST: the homepage route table**
- `src/router/routes.ts`: assert `/` → Index under MainLayout, `/addons` → AddonIndex, `/nonsense` → Error404, by mounting the router with
  memory history.

**AN-16 — UNTESTED CI LOGIC (LOW): the sponsors probe**
- `.forgejo/workflows/update-readme.yml` (`ab95db72e`): ~45 lines of inline Python decide whether README sponsors get overwritten. The six
  canned-body checks in the message were one-off and are not committed. Extract the Python to `misc/` with a pytest over those six bodies,
  so the next bump is checked.
- The probe must stay "identical to the action's query", but nothing checks that drift. Pinning the action at `@v1.6.1` (`363b20354`) makes
  the drift controllable. Whether the probe query matches v1.6.1's `src/template.ts` is unverified.

**AN-17 — RECURRENCE (LOW): nothing checks that pinned digests still exist**
- `d65c3313e` fixed a pinned mongodb digest that had stopped resolving, but added nothing to catch the next one. Renovate has re-pinned the
  digest twice since (`29d26ca46`, `1a9be02c7`). A scheduled CI step running `docker manifest inspect` over every `@sha256:` in
  `docker/*.yml` and the Dockerfiles would catch the next one before users do.

**AN-18 — MINOR ROBUSTNESS (LOW): download-counter details**
- `countDownloads.ts`: the retry is immediate, with no backoff, and also retries a 403 rate-limit, which cannot succeed within the same minute.
- `apiEndpoints.ts`, `releasesEndpoint`: owner and repository are interpolated without `encodeURIComponent`.
- `parseRepositoryUrl` keeps only the origin, so a GitHub Enterprise or Gitea instance served under a path prefix (`https://host/git/owner/repo`)
  is mis-parsed. Worth a test that documents the limitation.

## Slice D — 2026-10-29..11-11, 4782e4012..f6085c414 (first review)

**N-1: HIGH (security). Any issue opener's chosen mod runs as arbitrary code on the GitHub runner, with the job's secret, a persisted token and a writable cache in reach.**
- Commit: `46928a8a6`. It made the previously-cancelled boot actually run on every `opened`.
- Where: `.github/workflows/clientside-report-reusable.yml:47-129` (`report` job) and `serverpackcreator-clientside/src/main/kotlin/de/griefed/serverpackcreator/clientside/boot/ServerRunner.kt:104-108`.
- Issue: `issues: opened` from any GitHub user, with no write access needed, boots a server containing a mod the reporter picked from CurseForge or Modrinth. Booting runs that mod's code on the bare runner. Three things are reachable from it:
  - **The secret.** `CURSEFORGE_API_KEY` sits in the step `env`, and `ProcessBuilder("bash", "start.sh")` inherits the full environment (no `environment().remove(...)`), so the Minecraft server JVM has the key.
  - **The token.** `actions/checkout@v7` runs with default `persist-credentials: true`, so the job's `GITHUB_TOKEN` (`issues: write`) is in `.git/config` in the workspace.
  - **The cache.** `setup-java … cache: 'gradle'` and `gradle/actions/setup-gradle` save `~/.gradle` from a default-branch run. Mod code that writes into `~/.gradle` (for example `init.d/`) can poison the cache restored by `clientside-accept.yml`, which holds `contents: write` and `pull-requests: write`, and runs `./gradlew` before `create-pull-request`. That is an escalation path to repository write. The cache-scope and save-on-success details are from the actions' documented behaviour, not re-checked here: treat that link as plausible, not demonstrated.
- The project already treats mods this way elsewhere: the grinder runs the same workload in a no-network, no-caps, read-only container (`claude-docs/REFACTOR-LOG.md:2773`, "an untrusted JVM — an arbitrary-code execution engine"). Nothing in `CI-SECRETS.md`, `ci-workflows.md` or the clientside/app `CLAUDE.md` acknowledges that `-verifyclientside` in CI runs untrusted code on the host.
- Remedy, in order of cost:
  1. Set `persist-credentials: false` on the report job's checkout.
  2. Clear the environment for the server process: `ProcessBuilder.environment()` keeps only `PATH`/`JAVA*`/`HOME`.
  3. Have the report job neither restore nor save the Gradle cache (`cache-read-only` / no `cache:`), or build the jar in one job and boot in a second job with no cache and no secret.
  4. Ideally boot inside the grinder's hardened container image.
- Tests to add:
  - A `ServerRunner` test that boots a fake `start.sh` printing `env` and asserts `CURSEFORGE_API_KEY` and `GITHUB_TOKEN` are absent.
  - A workflow lint (in the existing `ci-env-reference-check` style) asserting that the `report` job's checkout sets `persist-credentials: false` and that no step of that job declares `cache:`.

**N-2: MEDIUM (bug). A streamed export that fails part-way still arrives as a complete 200, and the JSON one is even well-formed.**
- Commit: `438581fa4`
- Where: `serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/report/ReportServer.kt:350-366` (`streamVerdicts`) and `:440-444` (`respondStreaming`).
- Issue, JSON: `mapper.createGenerator(body).use { … }` runs Jackson 2.22's `close()` on exception, and `JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT` is on by default. That writes the missing `]}`. `exchange.responseBody.use(writeBody)` then closes the chunked stream normally, which writes the terminating chunk. The client receives a complete, parseable 200 whose `verdicts` array is shorter than `matched`.
- Issue, CSV: the unflushed `OutputStreamWriter` drops its buffer and the stream is closed cleanly, which can end mid-row.
- Why it matters: the KDoc promises "truncates the document". It is truncated, but in a way no client can detect, which is the "defect wearing a success" the same commit condemns in `GrinderClient`. `GrinderClient.fetchVerdicts` never compares rows received with `matched`.
- Logging: the exception propagates to the JDK `HttpServer`, which (from memory of `sun.net.httpserver.ServerImpl.Exchange.run`; unverified against the exact JDK build) logs non-`Error` exceptions only at TRACE. So a mid-stream `IOException` or serializer failure still reaches no application log, the original complaint.
- Remedy: disable `AUTO_CLOSE_JSON_CONTENT` on that generator. In `respondStreaming`, catch, log at ERROR, and abort without closing the body cleanly so the client sees an incomplete chunked response: for example call `exchange.close()` only on success and rethrow, or close the connection.
- Tests to add:
  - Serve a selection whose 2nd row's serialisation throws (a `GrindVerdict` subclass or mapper seam). Assert that the `java.net.http` client gets an `IOException` (or a body that fails `readTree`), and that the log appender captured an ERROR.
  - The same for `/export.csv`.
  - In `GrinderClientTest`: a page whose `verdicts.size` is below what `matched`/`pages` imply should produce `Failed`.

**N-3: MEDIUM (bug). Paging a store that changes during the fetch can silently skip rows, and the KDoc claims it cannot.**
- Commit: `438581fa4`
- Where: `serverpackcreator-plugin-grinder/src/main/kotlin/de/griefed/serverpackcreator/plugin/grinder/core/GrinderClient.kt:89-127`
- Issue: offset paging (`?size=&page=`) over a selection the daemon re-derives per request while grinding. The code de-duplicates rows that shift *forward* onto two pages, but a row inserted *before* the cursor (default order is confidence-first, so inserts land anywhere) pushes a boundary row back onto an already-fetched page. That row is never seen.
  - The KDoc says "Partial results are never returned", which is false under concurrent writes, the exact condition the de-dup comment describes.
  - `putIfAbsent` also keeps the *older* copy of a re-recorded verdict, contradicting "newest state included".
- Remedy:
  - Compare `collected.size` with the last document's `matched`. On a shortfall, refetch once or return `Failed`.
  - Or add a snapshot or version token to `/verdicts.json` so pages come from one selection.
  - Use `put` (last wins) if newest state is the intent.
- Tests to add: a `servingByPage` fake whose page 2 is computed after inserting a row at index 0 (so `prism`, last on page 1, moves to page 2's head and the old page-2 head moves to page 1). Assert every slug is present, or `Failed`. A second test: a slug repeated with a newer `verifiedAt` on page 2 should yield the newer one.

**N-4: MEDIUM (edge case). The comment's head is not budgeted, so a many-crash report can still exceed GitHub's 65,536-character limit.**
- Commit: `082bc2faa`
- Where: `serverpackcreator-clientside/src/main/kotlin/de/griefed/serverpackcreator/clientside/ClientsideReportRenderer.kt:64-69` (`renderMarkdown`) and `:98-109` (crash excerpts in `head`).
- Issue: only the consoles and the tail are fitted to `MAX_COMMENT_CHARS`. `head` is unbounded:
  - Each CRASHED target adds a `bootCrashExcerpt` of up to 4,000 characters (`BootLogClassifier.crashExcerpt` `maxChars = 4_000`).
  - Notes, including the new exception-message notes from `97623aed9`, have no cap.
  - `compact` drops excerpts only from the *data block*, never from the head.
  - A client-only mod crashing on every target is the normal CONFIRMED case: 5 lines × 3-4 loaders × 4,000 is 60-80k from excerpts alone. When `budget` goes negative, `bootLogs` still appends its header and "left out" lines.
  - The commit message's "drops … the crash excerpts its data block repeats" is accurate only for the data block.
- Remedy: budget the head as well. When `head + compact tail > MAX`, drop or shorten the crash excerpts in the head (consoles already show the same text), then cap notes.
- Test to add: a report with 20 `CRASHED` targets, each with a 4,000-character excerpt and a 2,000-character note. Assert `rendered.length <= MAX_COMMENT_CHARS` and that `dataBlockOf(rendered)` still parses with `suggestedEntries`.

**N-5: LOW-MEDIUM (bug or robustness). `withEvidence` and `dataBlockIn` disagree on what the data-block opener is.**
- Commit: `46928a8a6`
- Where: `.github/scripts/clientside-verification.js:110-115` (`withEvidence`: `report.lastIndexOf(DATA_BLOCK)`, *without* the newline) and `:209-217` (`dataBlockIn`: `lastIndexOf(DATA_BLOCK + '\n')`).
- Issue: the non-compact data block serialises `bootCrashExcerpt`, which is raw server-console text. A console that printed `<!-- clientside-report-data` lands inside a JSON string, where the newline is escaped as `\n`. So:
  - `dataBlockIn` correctly skips it.
  - `withEvidence` finds it as the *last* occurrence and splices the evidence block (with raw newlines) into the middle of the JSON string.
  - `dataBlockIn` then returns invalid JSON, and `clientside-accept.yml` writes it, so `-clientsideapply` fails.
- Effect: denial of acceptance, not forgery. That is the same threat model the `dataBlockIn` KDoc describes, which is why the script reads the *last* block.
- Remedy: use one opener constant (`DATA_BLOCK + '\n'`) in both functions.
- Test to add (node:test): a report whose data-block JSON contains `"bootCrashExcerpt": "<!-- clientside-report-data\\n{}"`. Assert `JSON.parse(dataBlockIn(withEvidence(report, evidence)))` deep-equals the original object.

**N-6: LOW (consistency or bug). Crash excerpts skip the console sanitising, and any `-->` in a JSON value shows the data block.**
- Commit: `082bc2faa`. The excerpt rendering itself predates it but was touched by `97623aed9`.
- Where: `ClientsideReportRenderer.kt:101-108`
- Issue:
  - The crash excerpt uses a fixed three-backtick fence and is not passed through `printable` (no ANSI stripping, no 400-character cut). The new console blocks do both, and grow their fence past any backtick run. An excerpt containing three backticks breaks out of its fence and renders the rest of the comment as markdown.
  - Separately, any `-->` inside a JSON string in the data block (common in mod logs, for example `A --> B`) ends the HTML comment early on GitHub, so the remaining JSON renders visibly. Parsing is unaffected because `dataBlockIn` searches for `\n-->`.
- Remedy: route the excerpt through `printable` plus the adaptive fence. Serialise the data block with `JsonGenerator.Feature.ESCAPE_NON_ASCII`-style escaping of `>` (a custom `CharacterEscapes` for `>` as `>`).
- Tests to add: an excerpt containing three backticks and an ANSI colour, asserting the fence and the stripping. A note containing `-->`, asserting the rendered comment contains exactly one `\n-->` after the opener, and that no `-->` appears inside the block's JSON text.

**N-7: LOW (bug). The table never selects "all"; its doc still says it is always offered.**
- Commit: `438581fa4`
- Where:
  - `serverpackcreator-grinder/src/main/kotlin/de/griefed/serverpackcreator/grinder/report/VerdictQuery.kt:302` (`forTable`) and `:351-366` (`offeredSizes`).
  - `VerdictReportRenderer.kt:158-165` (`sizeSelect`).
- Issue:
  - `forTable()` maps `size = null` ("all") to 5,000 before rendering, so `page.query.size` is never `null` on the HTML route. `sizeSelect`'s `size == page.query.size` therefore never marks "all" as selected.
  - A reader who picks "all" with ≤ 5,000 matches gets "5000" selected and `size=5000` in every pager link: the page disagreeing with its URL, which the comment at `:363` says this prevents.
  - The KDoc at `:351` still says "always including `all` (as `null`)", which has been false since this commit.
- Remedy: keep `null` when `matched <= MAX_TABLE_PAGE_SIZE` (resolve after counting, in `VerdictSelection`), or drop "all" from the table entirely. Fix the KDoc either way.
- Test to add: render `/?size=all` against a 12-row store and assert the `<option value="all" selected>` is present and the pager links carry `size=all`.

**N-8: LOW (consistency). Remaining `runCatching` calls still swallow `Throwable`, the defect class `438581fa4` diagnosed.**
- Where: `ReportServer.kt:411` (`/status`: `runCatching { … writeValueAsString }.getOrElse { "{\"error\":\"status unavailable\"}" }`), `:261` (`fallbackLists`) and `:405`.
- Issue: `438581fa4` diagnosed exactly this ("`runCatching` … catches Throwable, so heap exhaustion was answered 200 … reached no log") and fixed only the two endpoints it rewrote.
- Remedy: catch `Exception` and log it at WARN with the exception.
- Test to add: `/status` with a `cursors` store whose `cursor()` throws, asserting a log line.

**N-9: LOW (performance or robustness). The report reads each whole console file to keep 200 lines.**
- Commit: `082bc2faa`
- Where: `serverpackcreator-app/src/main/kotlin/de/griefed/serverpackcreator/app/cli/commands/VerifyClientsideCommand.kt` (the `renderMarkdown(report) { … ?.readLines() }` lambda).
- Issue: the renderer keeps at most 200 lines per console, but `readLines()` loads the whole file. A mod that spams its log for the 12-minute boot budget can produce hundreds of MB per attempt, times the number of targets, on a 7 GB runner. An `IOException` here also escapes `renderMarkdown` and loses the whole report.
- Remedy: tail-read (for example `RandomAccessFile` backwards to 200 newlines, or a bounded `ArrayDeque` while streaming lines), wrapped in `runCatching` that degrades to "no console was recorded".
- Test to add: a 50 MB console fixture, asserting that the heap delta stays bounded (or that the reader is called with a line cap), plus an unreadable console that degrades to the "no server console" line.

**N-10: LOW (bug, pre-existing, in a touched file). `-lang` as the last argument crashes the parser.**
- Commit: `e8900d1f9` touched `CommandlineParser.kt` and left it unfixed.
- Where: `serverpackcreator-app/src/main/kotlin/de/griefed/serverpackcreator/app/CommandlineParser.kt:95-98`
- Issue: `argsList.size >= argsList.indexOf(Mode.LANG.argument()) + 1` is always true whenever `-lang` is present. So `-lang` as the last argument indexes past the end and throws `IndexOutOfBoundsException` at startup. It should be `>`. "Fix bugs when you find them" plus Boy-Scout within the same file and goal (argument parsing).
- Test to add: `parse("-lang")` neither throws nor sets a language.

**N-11: LOW (consistency). Two flag-matching rules now coexist in one parser.**
- Commit: `e8900d1f9`
- Where: `CommandlineParser.kt:288-289` (`isFlag`) against `:147`, `:163`, `:176`, `:189` (`entry == Mode.X.argument()` for SCAN, CLIENTSIDE_REPORT, VERIFY_CLIENTSIDE, CLIENTSIDE_APPLY) and `optionValue` at `:316`.
- Issue:
  - `--help` and `--cli` are accepted but `--verifyclientside` and `--scan` are not.
  - `isFlag` uses `trimStart('-')`, so `---help` is accepted too, although the KDoc says "one or two leading dashes".
  - `-home` (one dash) is now accepted for `Mode.HOME("--home")`, a widening the message does not mention.
- Remedy: route every mode through `isFlag` and bound it to one or two dashes, or document why the clientside verbs are exact-match only.
- Tests to add: `parse("--verifyclientside", link)` (decide and pin), `parse("---help")` (is not HELP), `parse("-home", dir)`.

**N-12: LOW (edge case). A JDK advertised for two architectures resolves arbitrarily.**
- Commit: `958f33f13`
- Where: `ImageJavaRuntimes.javaHomesIn` (`serverpackcreator-clientside/.../boot/ImageJavaRuntimes.kt`, companion)
- Issue: `JAVA_HOME_21_X64` and `JAVA_HOME_21_ARM64` both map to major 21, and `toMap()` keeps whichever `System.getenv()` iterates last. Iteration order is unspecified. On an ARM runner with an x64 toolcache entry, the wrong-architecture JDK can be chosen.
- Remedy: prefer the entry whose `<arch>` matches `os.arch`.
- Test to add: both variables present with `os.arch=aarch64` should yield the ARM64 home.

**N-13: LOW (abuse or DoS). An issue opener can tie up Actions concurrency with long boots.**
- Commit: `46928a8a6`
- Where: `.github/workflows/clientside-verify.yml`, `clientside-report-reusable.yml:52` (`timeout-minutes: 200`).
- Issue: every `opened` request (and every edit that changes the link) starts a boot of up to 200 minutes. Concurrency is per issue, so N issues mean N parallel jobs. Any account can exhaust the repository's Actions concurrency and block the GitHub smoke `test.yml` for hours.
- Remedy: a repository-wide concurrency cap on the boot job (for example `group: clientside-report` with `cancel-in-progress: false`, queueing), or gate first-time contributors behind `verify-boot`.
- Test: none practical. Document the trade-off in `ci-workflows.md`.

**N-14: LOW (build). Build-cache hits across machines are still defeated by other per-machine manifest attributes.**
- Commit: `1b58d9aa2`
- Where: `buildSrc/src/main/kotlin/serverpackcreator.java-conventions.gradle.kts:143-150` and `:156-167`.
- Issue: only `Build-Timestamp` is ignored. `Built-By` (`user.name`), `Build-Jdk` and `Build-OS` also differ between developer machines and CI, so the message's claim that tests can now be "served from the build cache" holds for the *same* machine only. The landmine in `.claude/rules/build-layout.md` says "a new attribute that changes per build needs the same ignoreAttribute". These change per *machine*, which the landmine does not cover.
- Remedy: ignore those three as well, or state the same-machine scope in the landmine.
- Verification: measure with two clones under different users.

**N-15: LOW (test depth). The streaming guard checks a header, not the body.**
- Commit: `438581fa4`
- Where: `serverpackcreator-grinder/src/test/kotlin/de/griefed/serverpackcreator/grinder/report/VerdictsJsonEndpointTest.kt` (`theUnpagedEndpointsAreStreamedRatherThanBuiltInHeap`).
- Issue: the guard asserts only the absence of `Content-Length`, which is a shape check of the transport. Nothing asserts that the streamed `/export.csv` body is byte-identical to `VerdictCsvExporter.toCsv(rows, preOrdered = true)`, or that streamed JSON round-trips to the same document the old `writeValueAsString` produced. Other tests in the file read the JSON, but no CSV-body equality test was found.
- Test to add: an equality test between the streamed CSV body and `toCsv` over a fixture containing a comma, a quote and a newline in a field.

---

## Slice E — 2026-11-13..11-15 non-dependency commits, plus every dependency commit in the range (first review)

**N1 — Consistency/coverage: production MongoDB 9.0.8 vs a test suite on 8.0.5.**
- Where: `docker/docker-compose.yml:52`, `docker/docker-compose-dev.yml:37`, `EmbeddedMongoAvailable.kt:72`.
- Issue: every `-app` persistence test runs against embedded 8.0.5. The 9.0 compatibility evidence is one manual boot recorded in `e9f1f6396`
  (fresh init, an empty `runconfigs/all`, index creation). Not covered: migration of legacy documents, the upload path, any query shape.
- Tests to add: (1) a guard that parses the `image:` tag in `docker/docker-compose.yml` and asserts its major.minor equals
  `EmbeddedMongoAvailable.MONGOD_VERSION`'s. That would have failed on `e9f1f6396`, and it is exactly the invariant the KDoc claims.
  (2) If flapdoodle 4.33.0 supports 9.0 (unverified), run `WebPersistenceIT` with `MONGOD_VERSION = "9.0.8"`.

**N2 — Bug risk: the dev-stack fallback is now a downgrade across a major version on a shared volume.**
- Where: `docker/docker-compose-dev.yml:34-36` and `BUILD.md:240` both recommend `SPC_DEV_MONGO_IMAGE=mongodb/mongodb-community-server:8.0.5-ubuntu2204`
  over the same `spcdb-data` volume.
- Issue: a volume first initialised by 9.0.8 starts at featureCompatibilityVersion 9.0. That is MongoDB's normal behaviour for a fresh deployment, unverified
  for 9.0 specifically. 8.0.5 cannot open it. The `e9f1f6396` note *"a rollback to 8.0 stays possible"* holds only for volumes upgraded from 8.0.
  The production compose comment also says not to downgrade to an unguarded image, and 8.0.5 is unguarded (`7471aab2c`).
- Check to add: change the advice to "use a fresh volume (`docker compose -p spc-dev down -v`) when overriding to 8.0". Or offer a guarded 8.0.34 instead of 8.0.5.

**N3 — Missing edge case: an in-place upgrade of an existing 8.0 deployment was never exercised.**
- Where: `docker/docker-compose.yml` user deployments.
- Issue: users who copied the earlier compose (8.0.5 / 8.0.34) and pull the new one jump 8.0 → 9.0 in place. The commit states the FCV precondition but did not
  test it, and an operator never sees a commit message. `MONGO_INITDB_*` deprecation is also noted only in the message.
- Check to add: boot 8.0.34 on a volume, write a run config, then switch the image to 9.0.8 and confirm startup and data. Put the FCV note in the README.

**N4 — Consistency regression: Renovate re-pins the user-facing compose file, undoing `7471aab2c`.**
- Where: `docker/docker-compose.yml:5`, `renovate.json`.
- Issue: `7471aab2c` removed the digest from the user-copied compose file on purpose (*"the one place a digest pin is wrong"*).
  `30f8657b9` (Renovate "pin dependencies") put digests back on both the app and mongo lines, and `3ad91b407` carried it to `9.4.1@sha256:0da94a2…`.
  `renovate.json` has no rule to stop it, so it will recur on every pin pass.
- Check to add: a `packageRules` entry, `{"matchFileNames": ["docker/docker-compose.yml"], "pinDigests": false}`. Then un-pin line 5, and line 52 if the
  same reasoning applies to it.

**N5 — Supply chain / reproducibility: the Docker image build now downloads a JDK at build time.**
- Where: `docker/Dockerfile:1` (`eclipse-temurin:25`), plus the foojay resolver in both settings files.
- Issue: Java 21 is provisioned from foojay inside the builder on every uncached build. That is a network dependency and an unpinned JDK binary for the image
  that ships, and it runs twice (buildSrc and the main build share Gradle's toolchain store, so probably once in practice; unverified).
  It started with `c81d17137` (Java 22, non-LTS) and continues with `bbf660ce8`.
- Check to add: build the image with `-Dorg.gradle.java.installations.auto-download=false` (via `GRADLE_OPTS` in the RUN). The build then fails loudly
  if the launcher JDK is not the toolchain. Alternatively, put the builder back on `21-jdk-jammy`.

**N6 — Unused dependency: `@pinia/testing`.**
- Where: `serverpackcreator-web-frontend/package.json:32`.
- Evidence: `git grep "@pinia/testing"` matches only `package.json` and `CLAUDE.md:38`. No test imports `createTestingPinia`, and the tests use
  `setActivePinia(createPinia())`. `c69b769eb` resolved an ERESOLVE major for a package nothing uses.
- Remedy: remove it from devDependencies and fix `CLAUDE.md:38`.

**N7 — Consistency: the homepage stays on `quasar` 2.35.2 while web-frontend moved to 2.35.3.**
- Where: `serverpackcreator-homepage/package.json:22` (exact pin) vs `bca2e10f9`.
- Issue: Renovate PR #758 would normally carry the exact homepage pin in the same PR. `bca2e10f9` mentions only addons-overview and web-frontend.
  Whether a homepage hunk existed and was dropped is unverified (the PR itself was not inspected).
- Check: open #758's diff. If it had a homepage hunk, apply it.

**N8 — Test depth: `48ce85cac` fixes the race only for this class, and the test still uses the real network.**
- Where: `serverpackcreator-api/src/test/kotlin/.../versionmeta/VersionMetaManifestRepairTest.kt:72-87,112`.
- Issue: the class builds a real `WebUtilities`, so the background refresh really downloads. The 60 s bound is a wall-clock wait on the shared CI host,
  which the CI rules say must be sized for the pathological host. Production `VersionMeta` exposes no cancel/close, so an embedder that discards a
  `VersionMeta` cannot stop its refresh writing into a directory it is tearing down.
- Tests to add: inject a fake fetcher that blocks on a latch. Assert that `awaitManifestRefresh(…)` returns `false` while it is held and `true` after release.
  The test is then deterministic and offline, as the CLAUDE.md "API suite is offline" statement expects. Consider a `VersionMeta.close()` that cancels the refresh, with a test that no file
  appears after `close()`.

**N9 — Missing guard: `.dockerignore` versus declared Gradle inputs.**
- Where: `.dockerignore:25-37`; inputs at `buildSrc/build.gradle.kts:49`, `serverpackcreator-api/build.gradle.kts:137`,
  `serverpackcreator-clientside/build.gradle.kts:29`.
- Issue: `79cdffe85` is correct; the context now keeps `.github/scripts/` and nothing else under `.github`. But this is the second time in a month that an
  ignore line broke the image (`7c256ae8d` for `.forgejo/`), and the only safeguard is a comment.
- Test to add: a `test.yml` step (or a `misc/` script beside the duplication check) that collects every `inputs.file(`/`file("../…")` path in `*.kts` and
  checks it against `.dockerignore` (Docker semantics, `!` negation included). It should fail on `.github/` without `!.github/scripts/`.

**N10 — Release path not exercised: semantic-release on Node 26.**
- Where: `.forgejo/workflows/release-generate.yml:45-48,77-82`.
- Issue: `5dfdc6d92` verified the homepage image and the two CI scripts, not semantic-release. The first run on Node 26 will be a release. `npx --yes -p
  semantic-release@24 …` also floats four majors in the release job; that predates this slice and is a supply-chain point.
- Check to add: a `--dry-run` invocation on Node 26 (workflow_dispatch input or a scoped `push:` probe, per ci-workflows.md).

**N11 — Gradle wrapper integrity (predates the slice, surfaced by `7913de669`).**
- Where: `gradle/wrapper/gradle-wrapper.properties`.
- Issue: no `distributionSha256Sum`, and the 9.9.1 bump changed only `distributionUrl`. `gradle-wrapper.jar`/`gradlew` were not regenerated with
  `./gradlew wrapper --gradle-version 9.9.1` (whether they differ from 9.8.1's is unverified).
- Check: regenerate with `--gradle-distribution-sha256-sum` so the checksum is pinned.

**N12 — Gate opportunity after `9fa73aee3`.**
- Where: `buildSrc/src/main/kotlin/serverpackcreator.dokka-conventions.gradle.kts:30` (`failOnWarning.set(false)`, HTML publication only).
- Issue: `-api`'s Javadoc (published to Maven Central) is now at 0 warnings, so a regression like the 2.3.0 one would show up only by reading logs.
- Check: `failOnWarning.set(true)` on the Javadoc publication for `-api`. Or a test that runs `dokkaGeneratePublicationJavadoc` and greps for `w:`.
  The fix itself (`classpath.from(configurations.named("runtimeClasspath"))`, line 67) is lazy and additive. I verified it applies to every source set
  of every module using the convention.

**N13 — Minor: majors hidden behind ordinary-looking subjects.**
- `ee93fcf45` moved web-frontend `globals` 16.5.0→17.14.0, a major, under "update dependency globals to v17.14.0".
- `be7f8f8c8` moved happy-dom 20→21, a major, with no verification.
- Later evidence: `704c801cb` reports `npm run lint` clean in both projects, and `c938edbac` reports vitest 44/44. So no failure is suspected. This is noted for completeness.

---

# 2026-11-22 — remediation of the 2026-11-15 `8239cb6e9..develop` findings

Fixed on five branches, one per area, each in its own worktree and merged into `develop` with `--no-ff`:
`claude-fix-api`, `claude-fix-homepage`, `claude-fix-app`, `claude-fix-infra`, `claude-fix-clientside`. The
branches are kept; their commits are the per-finding history. Every fix commit states that its pin was observed
red first and quotes the red, or the mutation that reproduces it. Owner decisions (2026-11-15): commits scoped to
the homepage, CI, tests, docs or dev tooling mint no release; issue-triggered clientside boots are hardened AND
gated on a maintainer. Outcomes: **FIXED**, **ACCEPTED** (concerns a pushed commit's grouping, type or message,
which only a rewrite of shared history could change; the lesson is recorded here or in CLAUDE.md), **OPEN**
(a stated residual, with the reason).

## Audit findings

| Finding | Outcome | Where |
|---|---|---|
| A HIGH-1 — `-api` behaviour change unrecorded | FIXED | `d29dfe168` (new row, corrected row) |
| A MEDIUM-1 — `e4844fa5e` untested, left develop red | ALREADY FIXED | `4de775286`, before this audit |
| A LOW-1 — `d5ca59d10` grab-bag | ACCEPTED | pushed |
| A LOW-2 — one goal in two commits, unverified claim | ACCEPTED | pushed |
| A LOW-3 — new `!!` in `BuildVersionTest` | FIXED | `834d6df8d` |
| B A-M1 — env-reference gate has no stale-allowlist direction | FIXED | `c11c380b1` |
| B A-M2 — undated history in workflow comments | FIXED | `c11c380b1` (`test.yml`), `b2c7f3189` (`release-build.yml`) |
| B A-M3 — CI/test/website commits minted releases | FIXED | `964a4bf8b` (`releaseRules`) |
| B A-L1 — `5d1b96270` groups two reasons | ACCEPTED | pushed |
| B A-L2 — allowlist cites line numbers | FIXED | `c11c380b1` |
| B A-L3 — `var` in `JavaConfig.testJava` | FIXED | `9c1fe376f` |
| B A-L4 — docs narrate the past | FIXED | `595d69772` |
| B A-L5 — a dead module left in the tree 25 days | ACCEPTED | process; module removed by `c9cb16311` |
| B A-L6 — squash-boundary rule never landed | FIXED | `7bb677b9f` (root CLAUDE.md) |
| C A-M1 — `8e224d62e` groups two goals | ACCEPTED | pushed |
| C A-M2 — website/dev commits minted 9.4.0; bogus changelog links | FIXED | `964a4bf8b` (rules, `CHANGELOG.md` cleanup, backtick convention in CLAUDE.md) |
| C A-M3 — homepage thinly pinned before its migration | FIXED (remedy) | `717b55871` |
| C A-L1 — history-narrating comments | FIXED | `225aae5ba`, `067db46dd`, `3a23d37b6`, `272ae96ee` |
| C A-L2 — grouped commits without the red recorded | ACCEPTED | pushed |
| C A-L3 — `db025e531`'s message cites a subject on no ref | ACCEPTED | the citation exists only in that pushed message; `Index.vue` holds none |
| C A-L4 — test idiom (`stubGlobal` cleanup) | FIXED | `5592a8699` |
| C A-L5 — two backlog items closed in one commit | ACCEPTED | pushed |
| D A-M1 — Docker copy of the fallback list deferred without a backlog entry | FIXED | `8191ef54d` (copies equal, `FallbackModsCopiesTest`) |
| D A-M2 — B57 deferred on an unverified trigger | FIXED | `1e65252dc` (B57 closed) |
| D A-M3 — verb wiring pinned by grepping source | FIXED | `5bf96c698` |
| D A-L1 — "unchanged" move that also extends the class | ACCEPTED | pushed; the clean move survives on `claude-clientside-always-boot` |
| D A-L2 — doc comments carry incident history | FIXED | `028130cff` |
| D A-L3 — incident narrative in the root CLAUDE.md | ALREADY FIXED | `85b9ba577` |
| D A-L4 — `438581fa4` groups startup timing | ACCEPTED | pushed |
| E A-M1 — `8f7bf93f2`'s message: "nothing enforces `packageManager`" is false | FIXED (fact) / ACCEPTED (message) | `7a033fa27` records that the frontend plugin installs it through Corepack |
| E A-M2 — MongoDB 9.0.8 contradicted by docs and comments | FIXED | `fb1275db2` |
| E A-M3 — `docs.yml` node comment contradicts its value | FIXED | `ac403993a` |
| E A-M4 — Temurin 25 builder verified where the change cannot be seen | FIXED | `d5ef3ef9f` (builder back on 21, download disabled) |
| E A-M5 — homepage lockfile not truly regenerated | FIXED | `de295e474` (also `rm -rf node_modules` is required; recorded) |
| E A-L1 — `b137607af` repairs `85b9ba577` separately | ACCEPTED | pushed |
| E A-L2 — Dokka 2.3.0 warnings fixed two days later | ACCEPTED | pushed; `9fa73aee3` fixed them, `a925a9481` now fails on any |
| E A-L3 — addons-overview bumped minutes before deletion | ACCEPTED | pushed |
| E A-L4 — `eeaed3672` understates its scope | ACCEPTED | pushed |
| E A-L5 — nonexistent `shouldRecheckCrash` | FIXED | `8d7031d52` |
| E A-L6 — stale `@pinia/testing@1` line | FIXED | `39ec2e30f` |

## Analysis findings

| Finding | Outcome | Where |
|---|---|---|
| A-A1 — dev MongoDB on every interface | FIXED | `3a23d37b6` (27017 and 8080 on 127.0.0.1, pinned) |
| A-A2 — compose 9.0 vs README/tests 8.0 | FIXED | `fb1275db2`; the embedded test server stays 8.0.5 because flapdoodle `spring4x` 4.33.0 cannot download any 9.x — documented and pinned by `MongoVersionAlignmentTest` |
| A-A3 — config-cache problem in `-app:processResources` | FIXED | `a3470de88` (11 problems / 3 unique → 10 / 2, both third-party) |
| A-A4 — `updateOrRepair` failure branches unpinned | FIXED | `dde7ff495` |
| A-A5 — truncated HTTP body accepted as complete | FIXED | `be058287a` (JDK 21 reports it as EOF; now checked against Content-Length) |
| A-A6 — PowerShell `variables.txt` readers missed | FIXED | `8eadcf9c5` (lone CR broke both; CRLF and BOM were fine) |
| A-A7 — routing test hard-codes package names | FIXED | `26d15a142` |
| A-A8 — package graph acyclicity unchecked | FIXED | `b74e93e1a` |
| A-A9 — unreachable `"incompatible"` branch, wrong docs | FIXED | `5fdb349d0`; also fixed a mod whose only platform entry was incompatible reading as CLIENT, and an excluded range returned as `minecraftConstraint` |
| A-A10 — `media` depends on a root build that builds nothing | FIXED | `4c0232c42` |
| B-N-1 — clear-then-refill in five `GenerationConfig` lists | FIXED | `15908d92b` |
| B-N-2 — trailing comma injects an empty mod-list entry | FIXED | `43cae4794` (the two mod lists; `PropertyStore.getList` unchanged — see OPEN) |
| B-N-3 — partial GitHub release announced | FIXED | `b2c7f3189` |
| B-N-4 — reverts of three fixes stay green | FIXED | `9c1fe376f`, `1380d5f19`, `d619d4660` |
| B-N-5 — Java probe has no timeout | FIXED | `9c1fe376f`, `da9dbadca` |
| B-N-6 — env checker has no self-test | FIXED | `c11c380b1` |
| B-N-7 — one `set -eu` step hides the second verdict | FIXED | `c11c380b1` |
| B-N-8 — unpinned PyPI install | FIXED | `c11c380b1` (pyyaml 6.0.3 with hashes) |
| B-N-9 — stale docs from the docs-site rework | FIXED | `ac403993a` |
| B-N-10 — comment-stripping regex in 8 copies | FIXED (partly) | `53e88e050` (one helper each in `-app` and `-grinder`; the single copies in `-clientside` and `-plugin-servertest` stay rather than add a cross-module test dependency) |
| B-N-11 — `getResponseAsString` drops line breaks | FIXED (documented, pinned; behaviour kept as recommended) | `1380d5f19` |
| C-AN-1 — GitHub PAT sent to gitea.com and git.griefed.de | FIXED | `8be3e52f7` (tokens per platform) |
| C-AN-2 — browser refresh drops Forgejo | FIXED | `aec8b4b11` |
| C-AN-3 — Gitea paging cap hard-coded, no bound | FIXED | `6b05a8fa2` |
| C-AN-4 — collector has no timeout | FIXED | `745450739` |
| C-AN-5 — GridFS fallback can serve a truncated archive | FIXED | `815dcd103` |
| C-AN-6 — Range requests inflate the counter | FIXED | `bc7a7d3c0` (no Range, or a first range from byte 0) |
| C-AN-7 — Range edge cases unpinned | FIXED | `455f25d65` |
| C-AN-8 — failed download silent in the SPA | FIXED | `0a694332e` |
| C-AN-9 — Mongoku origin vs documented URL | FIXED | `3a23d37b6` |
| C-AN-10 — dev-stack run configuration drops `docker/.env` | FIXED (unverified in IntelliJ) | `3a23d37b6` |
| C-AN-11 — node comment vs value | FIXED | `ac403993a` |
| C-AN-12 — fresh checkout cannot type-check | FIXED | `d5cd52bf0` |
| C-AN-13 — SPA renames downloads client-side | FIXED | `46939bd45` |
| C-AN-14, C-AN-15 — flashcard images, route table untested | FIXED | `717b55871` |
| C-AN-16 — sponsors probe untested | FIXED | `272ae96ee` |
| C-AN-17 — nothing checks pinned digests exist | FIXED | `4673446ef` (weekly) |
| C-AN-18 — download-counter robustness | FIXED | `259196b3d` |
| D-N-1 — issue opener's mod runs next to secrets | FIXED, residual OPEN | `376129b2a` (maintainer gate, key read once from a file and deleted, allowlisted boot environment, `persist-credentials: false`, read-only/disabled caches, posting in a separate job) |
| D-N-2 — failed streamed export arrives as 200 | FIXED | `69870d68b` |
| D-N-3 — offset paging skips rows | FIXED, gap OPEN | `d765be657` |
| D-N-4, D-N-5, D-N-6 — comment budget, data-block opener, sanitising | FIXED | `6b505ad9f` |
| D-N-7 — table never selects "all" | FIXED | `23089373d` (changes one existing assertion, flagged in the message) |
| D-N-8 — `runCatching` swallows `Throwable` | FIXED (slice code), rest OPEN | `1dbe3056f` |
| D-N-9 — whole console read to keep 200 lines | FIXED | `6997c0a18` |
| D-N-10 — trailing `-lang` crashes the parser | FIXED | `f50b07c56` |
| D-N-11 — two flag-matching rules | FIXED | `800a8690e` (one or two dashes, every mode) |
| D-N-12 — dual-architecture JDK resolves arbitrarily | FIXED | `35d148ea0` |
| D-N-13 — issue openers can tie up Actions | FIXED | `376129b2a` (the gate) |
| D-N-14 — per-machine manifest attributes defeat the cache | FIXED | `d8021a1fa` |
| D-N-15 — streaming guard checks a header only | FIXED | `4d5953030` |
| E-N1, E-N2, E-N3 — MongoDB 9 vs tests, dev fallback, upgrade precondition | FIXED | `fb1275db2` (precondition documented; a real 8.0 → 9.0 upgrade not exercised) |
| E-N4 — Renovate re-pins the user-facing compose file | FIXED | `5cc930ac6` |
| E-N5 — image build downloads a JDK | FIXED | `d5ef3ef9f` |
| E-N6 — unused `@pinia/testing` | FIXED | `39ec2e30f` |
| E-N7 — homepage quasar behind web-frontend | FIXED | `5f7a77994` |
| E-N8 — repair test races and uses the network | FIXED | `d1cd00d30` |
| E-N9 — `.dockerignore` vs declared inputs unguarded | FIXED | `80e373bf2` |
| E-N10 — semantic-release on Node 26 never run | FIXED | `d428d16ca` (dry run on Node 26) |
| E-N11 — wrapper integrity | FIXED | `6f61c04bf` (checksum pinned; the jar was 9.8.1's, now 9.9.1's) |
| E-N12 — no gate on Dokka warnings | FIXED | `a925a9481` |
| E-N13 — majors behind ordinary subjects | FIXED | `d1b1177ff` |

## Found while fixing

- `5fdb349d0` — the sideness and `minecraftConstraint` defects above (`-api`, recorded in `API-BEHAVIOUR-CHANGES.md`).
- `1dbe3056f` — narrowing `runCatching` exposed a hang: an `Error` in a report handler killed the JDK request
  thread and left the client waiting; every route now goes through one handler that logs and drops the connection.
- `4aace8d85` — the frontend plugin's `package-manager-specification.txt` was tracked despite the ignore rule, so
  every `packageManager` bump left the tree dirty after a build.

## Integration: what only the full build on develop showed

Each branch ran its own modules' tests; the first `./gradlew build` over all five merged went red three times, and
none was a merge conflict:
- `baf173c77` — two download-page tests failed only under load. Pages that earlier tests left mounted kept a
  five-second countdown running, which then clicked their link during a later test (reproduced: 2 stray clicks
  after 6.5 real seconds). The pages now stop counting when unmounted, and every test's page is unmounted.
- `5fb999b71` — `--docker-properties` was missing from the clientside README, and a moved test separated a KDoc
  block from its declaration; both from `claude-fix-clientside`, whose narrower runs did not reach those guards.
- `c81954f21` — `KDocAttachmentTest` scanned the agents' worktrees under `.claude/worktrees/` as if they were this
  checkout; it now skips any nested checkout.

After them: `./gradlew build` green, 2,377 JVM tests and 56 frontend tests, 0 failures, 0 Dokka warnings.

## Still OPEN, each needing a decision

- **D-N-1, residual.** On GitHub-hosted runners the runner user has passwordless sudo, so a determined mod can read
  the runner's memory; the maintainer gate is the control that matters. Full isolation needs the engine split into
  prepare and boot phases (boot in a job with no secret) or booting inside the grinder's hardened container.
- **D-N-3, gap.** A row deleted ahead of the paging cursor can hide one row undetected; closing it needs keyset
  paging in the grinder's `/verdicts.json` (a wire-format change).
- **D-N-8, remainder.** About 90 `runCatching` calls outside the audited code in `-clientside`, `-grinder` and
  `-plugin-grinder`.
- **MongoDB in tests.** The embedded server stays on 8.0.5 until flapdoodle `spring4x` can download 9.x; 8.3.2 is
  the newest it supports, or a custom download resolver.
- **Commit-body links.** Backticking `#<hex>` is a written convention; enforcing it needs a JS release config.
- **Published 9.4.0 notes.** The Forgejo and GitHub release bodies still carry the bogus links; only
  `CHANGELOG.md` was cleaned.
- **From the api stream:** a cancellable `VersionMeta` would be new published API; `PropertyStore.getList` still
  keeps a doubled comma's empty entry for every other list setting; `JavaConfig.testJava` accepts any exit code.
- **Unverified here:** every workflow change (no runner), the Docker image build (2 GB Docker VM), the IntelliJ
  `profiles` XML in the dev-stack run configuration, and a real MongoDB 8.0 → 9.0 in-place upgrade.

# 2026-11-22 (later) — follow-up on the remediation's OPEN list

Commit subjects are quoted rather than hashes; these commits were unpushed when written.

## Closed
- **Embedded MongoDB moved from 8.0.5 to 8.3.2:** `test(app): run the database tests on MongoDB 8.3.2, the newest line flapdoodle can download`.
  - 8.3.2 is the newest release flapdoodle 4.33.0 can download. Its `PlatformPackageResolver` resolves 8.3.2 for Ubuntu 22.04 and 24.04 x86_64 and for macOS arm64 and x86_64.
  - Compose still deploys 9.0, so the test/production gap stays, recorded by `MongoVersionAlignmentTest`.
  - If SERVER-121912 (MongoDB 8.x on Linux kernel 6.19+) hits the CI host, the database tests skip rather than fail.
- **`PropertyStore.getList` kept a doubled comma's empty entry:** closed by `fix(api): list settings never hold a blank entry`.
  - `getList` drops every blank entry, and the five directory/file list setters do the same.
  - It is a behaviour change on published API, recorded in `API-BEHAVIOUR-CHANGES.md`.
- **Found while researching that item, HIGH severity, in neither audit:** a blank entry in `preInstallCleanupFiles` or `postInstallCleanupFiles` deleted the whole server pack.
  - `File(destination, "")` is the destination, and `deleteQuietly` deletes directories recursively. `..` deleted the directory holding the pack.
  - Closed by `fix(api): a cleanup entry can never delete the server pack or anything outside it`, which skips any entry not strictly inside the pack, compared canonically.
  - A blank ZIP exclusion was checked and found harmless, because zip4j's filter only sees entries below the root.

## Assessed, decision still Griefed's
- **D-N-1 residual.** The gate stays the control. One new observation: with sudo, a mod can also lift the job's `ACTIONS_RUNTIME_TOKEN`. Issue events run on the default-branch ref, so it could write cache entries that later default-branch GitHub workflows restore. `cache-read-only` limits setup-gradle only.
  - Recommended next step: boot through the existing `ServerRunner` seam in the grinder's hardened container (`ContainerServerRunner`). That is preferred over a prepare/boot job split, which is an engine redesign because the re-checks interleave download and boot, and which leaves the boot job holding a runtime token.
- **D-N-3 gap.** Recommended: put `VerdictStore.version` in the `/verdicts.json` envelope, and have `GrinderClient` restart a read whose version changes, with bounded retries. Keyset paging is the fallback. Nothing here touches `-api`.
- **D-N-8 remainder.** The inventory counts 112 sites in main sources: 95 safe, 14 that should log, and 3 that change a result.
  - `DockerJavaContainerEngine.exitCodeOf`: an inspect failure turns CRASHED into INCONCLUSIVE.
  - `JsonRequeueStore.read`: an unreadable file is overwritten, losing queued re-grinds.
  - servertest `ServerLauncher.giveBack`: a failed restore leaves the user's `server.properties` holding the borrowed port.
  - None runs in coroutine code. Four sites swallow `InterruptedException` without restoring the interrupt flag.
- **Commit-body links.** Recommended: a CI lint over `origin/main..HEAD`, following the `misc/*-check.py` pattern. A `release.config.js` only with a committed replay test, so that porting `releaseRules` cannot silently change what releases.

# 2026-11-23 — the condensed history: where each cited commit now lives

`origin/main..develop` was condensed on 2026-11-23 from 136 commits to 69: one commit per Renovate PR, each
with the fixes to its own change folded in, and the remaining audit fixes grouped by goal. The hashes this file
cites still resolve: the per-finding commits on the five `claude-fix-*` branches, every other replaced commit on
`claude-develop-before-condense`. This table names the condensed commit on `develop` that now holds each one,
by subject, because subjects survive a rewrite and hashes do not.

| Cited | Its subject | Now part of |
|---|---|---|
| `010f29e61` | docs: keep the module CLAUDE.md files to their rules, and their history in claude-docs | docs: keep the always-loaded CLAUDE.md files to their rules, and their history in claude-docs |
| `0165bd10b` | docs(audit): record the follow-up on the 2026-11-22 remediation's OPEN items | docs(audit): audit of 8239cb6e9..develop, its remediation, and the follow-up on what stayed open |
| `028130cff` | docs(grinder): state the report and startup constraints in the present tense | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `067db46dd` | docs(homepage): describe the download-chip and retry guards in the present tense | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `082bc2faa` | feat(clientside): post each boot's console in the verification report, within one comment | feat(clientside): post each boot's console in the verification report, within one comment |
| `0a694332e` | fix(web-frontend): report a failed download on the page instead of claiming it started | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `1380d5f19` | test(api): pin getResponseAsString's connection release and its line-joining | fix(api): an absent manifest is not "repaired", and a download cut short is not complete |
| `15908d92b` | fix(api): the five directory and file lists no longer refill a shared set | fix(api): the five directory and file lists no longer refill a shared set |
| `17adcab88` | deps(deps): update ghcr.io/linuxserver/nginx docker tag to v1.30.6 | deps(deps): update ghcr.io/linuxserver/nginx docker tag to v1.30.6 |
| `1a9be02c7` | deps(deps): update mongodb/mongodb-community-server:8.0.34-ubuntu2204 docker digest to 241eb6e | deps(deps): update mongodb/mongodb-community-server:8.0.34-ubuntu2204 docker digest to 241eb6e |
| `1b58d9aa2` | fix(build): stop every jar's build timestamp from re-running every module's tests | fix(build): stop every jar's build timestamp from re-running every module's tests |
| `1dbe3056f` | fix(grinder): the report logs what it degrades over, and no longer swallows an Error | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `1e65252dc` | fix(clientside): a jar-scan that throws costs its row, not the whole report (B57) | fix(clientside): a jar-scan that throws costs its row, not the whole report (B57) |
| `2060823ef` | deps(deps): update dependency autoprefixer to v10.6.2 | deps(deps): update dependency autoprefixer to v10.6.2 |
| `2242bbf7a` | deps(deps): update dependency com.install4j:install4j-runtime to v13.1.3 | deps(deps): update dependency com.install4j:install4j-runtime to v13.1.3 |
| `225aae5ba` | docs(web): state the download-route constraints in the present tense | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `23089373d` | fix(grinder): "all" the table can render stays "all" on the page and in its links | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `259196b3d` | fix(homepage): retry only transient forge failures, after a delay, and encode API paths | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `26d15a142` | test(clientside): find a browser downloader wherever it lives, not in one named package | test: guards that read sources, build files and the package graph assert what they claim |
| `272ae96ee` | fix(ci): the sponsors probe's verdict lives in a tested script | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `3449dde45` | test(app): run the database tests on MongoDB 8.3.2, the newest line flapdoodle can download | deps(deps): update mongodb/mongodb-community-server docker tag to v9 |
| `35d148ea0` | fix(clientside): a JDK advertised for two architectures resolves to the host's | fix(clientside): boot each verification pack on the Java its Minecraft version requires |
| `376129b2a` | fix(ci): boot a clientside request only on a maintainer's say-so, and contain what the boot can reach | fix(ci): boot a server for each clientside-mod request a maintainer approves |
| `39ec2e30f` | chore(web-frontend): drop the unused @pinia/testing devDependency | deps(deps): lock file maintenance |
| `3a23d37b6` | fix(dev): the dev stack listens on loopback only, and its browser and run configuration work as documented | fix(dev): the dev stack listens on loopback only, and its browser and run configuration work as documented |
| `3a665e381` | deps(deps): update dependency @quasar/extras to v2 | deps(deps): update dependency @quasar/extras to v2 |
| `3ad91b407` | deps(deps): update griefed/serverpackcreator docker tag to v9.4.1 | deps(deps): update griefed/serverpackcreator docker tag to v9.4.1 |
| `41b605999` | fix(api): list settings never hold a blank entry | fix(api): a blank list entry can no longer delete the server pack |
| `43cae4794` | fix(api): a doubled or trailing comma no longer puts an empty entry into the mod lists | fix(api): a doubled or trailing comma no longer puts an empty entry into the mod lists |
| `455f25d65` | test(web): pin the Range edge cases both archive download routes inherit | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `4673446ef` | ci: a weekly check that every pinned image digest still exists | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `46928a8a6` | fix(ci): boot a server for every clientside-mod request | fix(ci): boot a server for each clientside-mod request a maintainer approves |
| `46939bd45` | fix(web-frontend): show and save under the server's archive name, not a client-side one | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `4776f0ff0` | deps(deps): update dependency pinia to v4 | deps(deps): update dependency pinia to v4 |
| `48ce85cac` | fix(api): await the background manifest refresh before the repair tests' temp dir is deleted | fix(api): await the background manifest refresh before the repair tests' temp dir is deleted |
| `4aace8d85` | chore(build): stop tracking the frontend plugin's resolved package-manager file | fix(build): build checks no longer capture the Project, media packages exactly one jar, and the tree stays clean |
| `4c0232c42` | fix(build): media refuses to package anything but exactly one application jar | fix(build): build checks no longer capture the Project, media packages exactly one jar, and the tree stays clean |
| `4c5b1d8b8` | deps(deps): update dependency @babel/eslint-parser to v7.29.13 | deps(deps): update dependency @babel/eslint-parser to v7.29.13 |
| `4d529811a` | deps(deps): update npm to v11.21.2 | deps(deps): update npm to v11.21.2 |
| `4d5953030` | test(grinder): pin what the streamed exports contain, not only that they stream | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `5294575a9` | deps(deps): update flatlaf to v3.8 | deps(deps): update flatlaf to v3.8 |
| `53e88e050` | test(app,grinder): one comment-stripping helper per module instead of five copies in two dialects | test: guards that read sources, build files and the package graph assert what they claim |
| `542200c18` | deps(deps): update node.js to e9f565d | deps(deps): update node.js to e9f565d |
| `5592a8699` | test: undo stubbed globals in afterEach and drop two test-side !! assertions | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `574965224` | deps(deps): update dependency eslint-plugin-vue to v9.33.0 | deps(deps): update dependency eslint-plugin-vue to v9.33.0 |
| `595d69772` | docs(api): three guard docs state their constraint instead of narrating the past | fix(api): validating a Java path can no longer hang on, or leak, the probed executable |
| `5a5af9193` | docs(grinder): a ufw rule can be present, correct and not loaded | docs(grinder): a ufw rule can be present, correct and not loaded |
| `5bf96c698` | test(app): pin -verifyclientside's Java selection by running it, not by grepping its source | test: guards that read sources, build files and the package graph assert what they claim |
| `5cc930ac6` | fix(docker): the user-facing compose file carries tags only, and Renovate stops re-pinning it | deps(deps): update mongodb/mongodb-community-server docker tag to v9 |
| `5dfdc6d92` | deps(deps): update dependency node to v26 | deps(deps): update dependency node to v26 |
| `5f7a77994` | deps(homepage): quasar 2.35.3, matching web-frontend | deps(deps): lock file maintenance |
| `5fb999b71` | docs(clientside): document -clientsideapply's --docker-properties, and reattach a test's KDoc | fix(docker): ship the same clientside fallback list in the image as everywhere else, and keep it so |
| `5fdb349d0` | fix(api): an incompatible dependency entry no longer decides a mod's side or Minecraft range | fix(api): an incompatible dependency entry no longer decides a mod's side or Minecraft range |
| `610e231d1` | fix(api): a cleanup entry can never delete the server pack or anything outside it | fix(api): a blank list entry can no longer delete the server pack |
| `66c164898` | deps(deps): update dependency @quasar/app-vite to v3.10.4 | deps(deps): update dependency @quasar/app-vite to v3.10.4 |
| `69870d68b` | fix(grinder): a streamed export that fails part-way no longer arrives as a complete 200 | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `6997c0a18` | fix(clientside): read only the end of each console the report shows | fix(ci): boot a server for each clientside-mod request a maintainer approves |
| `6b05a8fa2` | fix(homepage): page Gitea by X-Total-Count, and bound both counters' paging | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `6b505ad9f` | fix(clientside): keep the verification comment whole and under GitHub's limit whatever the boots print | fix(ci): boot a server for each clientside-mod request a maintainer approves |
| `6e7ac4681` | deps(deps): update huggingface/mongoku docker tag to v2.11.5 | deps(deps): update huggingface/mongoku docker tag to v2.11.5 |
| `6f61c04bf` | chore(build): pin the Gradle distribution's checksum and ship 9.9.1's own wrapper jar | deps(deps): update gradle to v9.9.1 |
| `704c801cb` | deps(deps): update dependency eslint-plugin-vue to v10.12.1 | deps(deps): update dependency eslint-plugin-vue to v10.12.1 |
| `717b55871` | test(homepage): pin the flashcard images and the route table | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `7241597aa` | deps(deps): update dependency eslint-config-prettier to v8.10.2 | deps(deps): update dependency eslint-config-prettier to v8.10.2 |
| `745450739` | fix(homepage): give every download-count request a timeout | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `7913de669` | deps(deps): update gradle to v9.9.1 | deps(deps): update gradle to v9.9.1 |
| `79cdffe85` | fix(docker): keep the clientside workflow script in the image's build context | fix(docker): keep the clientside workflow script in the image's build context |
| `7a033fa27` | docs(web-frontend): record that the build enforces packageManager through Corepack | deps(deps): update npm to v12 |
| `7bb677b9f` | docs: a squash must say where its detailed history survives | docs: keep the always-loaded CLAUDE.md files to their rules, and their history in claude-docs |
| `7ea41ccb6` | deps(deps): update dependency com.github.jk1.dependency-license-report to v3.1.5 | deps(deps): update dependency com.github.jk1.dependency-license-report to v3.1.5 |
| `800a8690e` | fix(app): match every commandline flag by one rule, with one or two dashes | fix(app): match commandline flags as whole arguments, not substrings |
| `80e373bf2` | fix(ci): CI fails when .dockerignore hides a declared Gradle input from the image build | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `815dcd103` | fix(web): never serve a partial archive recovered from GridFS | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `8191ef54d` | fix(docker): ship the same clientside fallback list in the image as everywhere else, and keep it so | fix(docker): ship the same clientside fallback list in the image as everywhere else, and keep it so |
| `8337054a9` | deps(deps): update dependency @intlify/unplugin-vue-i18n to v11.2.8 | deps(deps): update dependency @intlify/unplugin-vue-i18n to v11.2.8 |
| `834d6df8d` | test(build): drop the `!!` from BuildVersionTest's sed-program lookup | test: guards that read sources, build files and the package graph assert what they claim |
| `85b9ba577` | docs: take the refactor state and its lessons out of the always-loaded CLAUDE.md | docs: keep the always-loaded CLAUDE.md files to their rules, and their history in claude-docs |
| `86f2f693e` | deps(deps): update dependency vitest to v5.1.0 | deps(deps): update dependency vitest to v5.1.0 |
| `8be3e52f7` | fix(homepage): send the GitHub token to GitHub only | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `8d7031d52` | docs(clientside): name the crash re-check by the function that exists | test: guards that read sources, build files and the package graph assert what they claim |
| `8eadcf9c5` | fix(api): the PowerShell templates read a variables.txt saved with lone carriage returns | fix(api): the PowerShell templates read a variables.txt saved with lone carriage returns |
| `8f7bf93f2` | deps(deps): update npm to v12 | deps(deps): update npm to v12 |
| `921215767` | docs(audit): audit and analysis of 8239cb6e9..develop | docs(audit): audit of 8239cb6e9..develop, its remediation, and the follow-up on what stayed open |
| `94bd3cda4` | deps(deps): update dependency typescript-eslint to v8.72.2 | deps(deps): update dependency typescript-eslint to v8.72.2 |
| `958f33f13` | fix(clientside): boot each verification pack on the Java its Minecraft version requires | fix(clientside): boot each verification pack on the Java its Minecraft version requires |
| `964a4bf8b` | chore(release): homepage, CI, test, docs and dev-tooling scopes mint no release | chore(release): homepage, CI, test, docs and dev-tooling scopes mint no release |
| `97623aed9` | fix(clientside): make every verification report row say what it booted, and why not | fix(clientside): make every verification report row say what it booted, and why not |
| `9c1fe376f` | fix(api): validating a Java path can no longer hang on, or leak, the probed executable | fix(api): validating a Java path can no longer hang on, or leak, the probed executable |
| `9fa73aee3` | fix(build): give Dokka the runtime classpath so library superclasses resolve | deps(deps): update dokka to v2.3.0 |
| `a2dedf4ac` | deps(deps): update dependency com.install4j.gradle to v13.1.3 | deps(deps): update dependency com.install4j.gradle to v13.1.3 |
| `a3470de88` | fix(build): keep -app's processResources check from capturing the Project | fix(build): build checks no longer capture the Project, media packages exactly one jar, and the tree stays clean |
| `a5e2f0064` | deps(deps): update dependency typescript to v5.9.3 | deps(deps): update dependency typescript to v5.9.3 |
| `a925a9481` | chore(build): a Dokka warning fails the build, now that there are none | deps(deps): update dokka to v2.3.0 |
| `ac403993a` | docs(ci): the docs pipeline's comments describe the pipeline as it is | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `ad002d094` | deps(deps): update ghcr.io/linuxserver/baseimage-ubuntu:noble docker digest to f113b70 | deps(deps): update ghcr.io/linuxserver/baseimage-ubuntu:noble docker digest to f113b70 |
| `aec8b4b11` | fix(homepage): the browser refresh keeps the build's figure for a forge it cannot reach | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `b137607af` | docs: point build-layout.md's buildSrc cross-references at the section now in that file | docs: keep the always-loaded CLAUDE.md files to their rules, and their history in claude-docs |
| `b29978d4a` | deps(deps): update dokka to v2.3.0 | deps(deps): update dokka to v2.3.0 |
| `b2c7f3189` | fix(ci): the release announcement requires every asset on GitHub, not just one | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `b74a66dda` | deps(deps): update dependency org.pf4j:pf4j to v3.16.1 | deps(deps): update dependency org.pf4j:pf4j to v3.16.1 |
| `b74e93e1a` | test(clientside): hold the package graph to its documented table, and acyclic | test: guards that read sources, build files and the package graph assert what they claim |
| `b812c109e` | deps(deps): update dependency vue-tsc to v3.3.14 | deps(deps): update dependency vue-tsc to v3.3.14 |
| `b81bba738` | docs(audit): record the remediation of the 2026-11-15 findings | docs(audit): audit of 8239cb6e9..develop, its remediation, and the follow-up on what stayed open |
| `baf173c77` | fix(web-frontend): a download page that is gone stops its countdown, and no test leaves one running | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `bbf660ce8` | deps(deps): update eclipse-temurin docker tag to v25 | deps(deps): update eclipse-temurin docker tag to v25 |
| `bc7a7d3c0` | fix(web): count a resumed or segmented download once, not once per request | fix(web): an archive download is complete, counted once, named by the server, and its failure shown |
| `bca2e10f9` | deps(deps): update dependency quasar to v2.35.3 | deps(deps): update dependency quasar to v2.35.3 |
| `bde9a0a46` | deps(deps): update eslint monorepo | deps(deps): update eslint monorepo |
| `be058287a` | fix(api): a download cut short is no longer reported as complete | fix(api): an absent manifest is not "repaired", and a download cut short is not complete |
| `c11c380b1` | fix(ci): the env-reference check reports stale exemptions, and the conventions step reports every verdict | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `c69b769eb` | deps(deps): update dependency @pinia/testing to v2 | deps(deps): update dependency @pinia/testing to v2 |
| `c81954f21` | test(api): KDocAttachmentTest no longer grades another checkout nested under the repository | test: guards that read sources, build files and the package graph assert what they claim |
| `c938edbac` | deps(deps): lock file maintenance | deps(deps): lock file maintenance |
| `c9cb16311` | chore: remove serverpackcreator-addons-overview | chore: remove serverpackcreator-addons-overview |
| `d1b1177ff` | chore(renovate): a major update says so in its subject and labels | fix(ci): convention checks report every verdict, and release and probe steps verify what they claim |
| `d1cd00d30` | test(api): the manifest-repair guard runs offline, and the refresh await is pinned | fix(api): an absent manifest is not "repaired", and a download cut short is not complete |
| `d29dfe168` | docs: record that NeoForge incompatible dependencies left ScannedMod.dependencies | fix(api): an incompatible dependency entry no longer decides a mod's side or Minecraft range |
| `d428d16ca` | docs(ci): record semantic-release's Node floor, verified on Node 26 by a dry run | deps(deps): update dependency node to v26 |
| `d5cd52bf0` | fix(homepage): a checkout that never ran the collector still builds, type-checks and tests | fix(homepage): the download counters send each token only to its own forge, and survive a forge that fails |
| `d5ef3ef9f` | fix(docker): the image builder runs the toolchain's JDK, and a mismatch fails instead of downloading one | deps(deps): update eclipse-temurin docker tag to v25 |
| `d619d4660` | test(build): pin that the licence report's fetch goes through the bounded read | test: guards that read sources, build files and the package graph assert what they claim |
| `d765be657` | fix(plugin-grinder): re-read the feed when a shifting store skipped a row, and keep the newer copy | fix(grinder): the report never passes off a failed export as complete, and logs what it degrades over |
| `d8021a1fa` | fix(build): ignore the per-machine manifest attributes too, so a cache serves across machines | fix(build): stop every jar's build timestamp from re-running every module's tests |
| `d90fdb471` | docs: record the clientside, grinder and workflow fixes from the 2026-11-15 audit | docs(audit): audit of 8239cb6e9..develop, its remediation, and the follow-up on what stayed open |
| `da9dbadca` | test(api): the hanging-Java pin no longer depends on a script starting in time | fix(api): validating a Java path can no longer hang on, or leak, the probed executable |
| `dde7ff495` | fix(api): an absent manifest is no longer "repaired", and the repair's failure branches are pinned | fix(api): an absent manifest is not "repaired", and a download cut short is not complete |
| `de295e474` | deps(homepage): regenerate the lockfile from scratch, for real this time | deps(deps): lock file maintenance |
| `dee9167c8` | chore: Add continuity to list of clientside-mods. | chore: Add continuity to list of clientside-mods. |
| `e8900d1f9` | fix(app): match commandline flags as whole arguments, not substrings | fix(app): match commandline flags as whole arguments, not substrings |
| `e9f1f6396` | deps(deps): update mongodb/mongodb-community-server docker tag to v9 | deps(deps): update mongodb/mongodb-community-server docker tag to v9 |
| `ee93fcf45` | deps(deps): update dependency globals to v17.14.0 | deps(deps): update dependency globals to v17.14.0 |
| `f50b07c56` | fix(app): a trailing -lang no longer aborts startup | fix(app): match commandline flags as whole arguments, not substrings |
| `f6085c414` | docs: record why no clientside-mod request was ever booted, and what stood behind the first one | docs: record why no clientside-mod request was ever booted, and what stood behind the first one |
| `f7ea11a84` | deps(deps): update dependency prettier to v2.8.8 | deps(deps): update dependency prettier to v2.8.8 |
| `fa9bd0f14` | deps(deps): update dependency vue to v3.5.45 | deps(deps): update dependency vue to v3.5.45 |
| `fb1275db2` | fix(docker): every MongoDB reference agrees with the 9.0.8 deployment, and the tests say why they differ | deps(deps): update mongodb/mongodb-community-server docker tag to v9 |

# 2026-11-23 — the follow-up's decisions, implemented

Griefed chose to implement four of the follow-up's recommendations. Each is listed by commit subject; the
per-goal detail and red-first evidence are on the named kept branch.

- **D-N-1 residual, cache poisoning:** closed by `fix(ci): a booted mod's cache token cannot save, whatever started the boot` (`claude-b1-cache`).
  - Since 2026-06-26 GitHub already issues `issues` runs a cache token that cannot save to the default branch. The gap was a manually dispatched boot, which is a trusted trigger.
  - `cache-mode: read` on the calling job and on `report`, and `cache-mode: none` on `clientside-accept`, restrict the token itself, so a token read out of memory is restricted too.
  - Still open, for the container-boot decision: the CurseForge key and the `contents: read` token can be read from memory with sudo, and the runtime token can replace the run's own `clientside-report` artifact.
- **D-N-3 gap, paging:** closed by `fix(grinder,plugin-grinder): a paged verdict read finishes on the snapshot it started from` (`claude-b2-paging`).
  - Every page carries the snapshot's version, and the daemon keeps up to 8 snapshots for 60 s after their last use.
  - The client pins pages 2..n to page 1's version, and restarts on `410 Gone` or on a changed version, at most 3 times.
  - What remains: a pinned read evicted on all three attempts fails rather than returning a list.
- **D-N-8, the result-changing `runCatching` sites** (`claude-b3-runcatching`):
  - `feat(api): orNullLogged, a logged null that swallows only what it may` adds the shared helper. It catches `Exception` only, restores the interrupt flag and rethrows a cancellation.
  - `exitCodeOf`, checked against `VerdictPolicy`: the published verdict could not change, but the per-boot reading could, so the failure is now logged.
  - The re-grind queue: an unreadable file is set aside, and `add()` counts only what was saved.
  - servertest: a failed `server.properties` give-back tells the user where the original is.
  - `isRunning`: an unanswered liveness inspect no longer ends a boot. After 60 unanswered polls in a row the boot is abandoned as `ERROR`.
  - The sites that should only log are closed by `fix(grinder): a lost template, marker, install console or kept boot log is logged, not silent` and `fix(servertest): a broken console, close, shutdown kill or clipboard copy is logged, not silent` (`claude-b3-log-sites`).
    - Each now leaves a WARN naming the file or pack, and returns what it returned before. Grinder: 7 reads, writes and the live install console. Servertest: 4 sites.
    - Two of the inventory's 14 were re-judged safe and left alone: the `-api` minecraft-constraint reads. `ForgeTomlScanner.readMinecraftConstraint` re-reads what `read()` already parsed, so it cannot fail there. `QuiltScanner`'s throws only for a mod without a `depends` block, which is normal.
- **Commit-body links:** enforced by `ci: fail a push whose commit message would mislink an issue in the release notes` (`claude-b5-link-lint`).
  - The checker ports conventional-commits-parser 6.4.0's reference extraction, and agrees with the real parser on all 7,700 commit messages.
  - It runs over each push's own range, so already-pushed history never keeps CI red.
  - Its one finding on `origin/main..develop` was reworded before the next push.
- **Found while implementing:** `fix(ci): the .dockerignore input check does not read another checkout's build scripts`. It is the nested-worktree defect `KDocAttachmentTest` had.
