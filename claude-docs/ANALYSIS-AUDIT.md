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
