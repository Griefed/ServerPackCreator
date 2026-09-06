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

