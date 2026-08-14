# Refactor Audit — modscanning follow-ups (`0a12d41d0..HEAD`)

**Range audited:** `0a12d41d0..HEAD` — **6 commits**, `bf226c2ac` … `7004f3c88` (2026-08-14)
**Base:** `0a12d41d0` *("fix: Vastly improve automated modscanning…")* — the rewrite these six commits
follow up on. Not itself in range; its surviving conditions are reported separately below.
**Mode:** READ-ONLY at audit time. No source was modified while auditing.

**Remediation:** branch `claude-modscan-test-hardening`, **11 commits**, `7279962bd` … `35c5787ad`.
API suite **280 → 294 tests**, 1 skip (the `fish`-absent skip in `ScriptTemplateContentTest`,
unchanged), 0 failures. `:serverpackcreator-app:test` 80 tests, green.

### Status

| # | Finding | Severity | Status |
|---|---|---|---|
| H-1 | Exported types deleted under a `refactor:` label | HIGH | **Withdrawn** — see below |
| M-1 | No characterization test in any of the six commits | MEDIUM | **Fixed** — 5 test commits, +12 tests |
| M-2 | Exported `modID` default changed silently, unpinned | MEDIUM | **Pinned**; still undocumented (→ I-3) |
| M-3 | Fixture cannot reach the Quilt fabric-fallback branch | MEDIUM | **Moot** — superseded by N-1 |
| M-4 | Unrecognised modloader yields an empty server pack | MEDIUM | **Fixed** + pinned |
| N-1 | The Quilt copy-loop is now unreachable dead code | LOW | **New**, open |
| L-1 | Unused `SupportedModloaders.quilt` import | LOW | Open |
| L-2 | Value identity hand-rolled at six call sites | LOW | Open |
| L-3 | Log statement reads the value it just overwrote | LOW | Open |
| I-6 | Dependency rescue could not fire for clientside mods | MEDIUM | **Fixed** + pinned |
| I-1…I-5, I-7, I-8 | Inherited from `0a12d41d0` | mixed | Open |

---

## Withdrawn

### H-1 — Exported types deleted under a `refactor:` label

`0df7b2835` deleted `ScanResult`, `Exclusion` and `Dependency` from a module published to Maven
Central, labelled `refactor:`.

**Withdrawn on Griefed's call:** the removed types were already unused and break in 9.x regardless.
The API compatibility policy governs source-compatibility *within* a major version, so removal at a
major boundary is exactly what it permits — the finding mistook a deliberate major-version break for
an accidental one. The `refactor:` label remains imprecise for a change that is not
behaviour-preserving for an embedder, but that is not worth rewriting shared history for.

Worth one line in the 9.x release notes so plugin authors meet it in the changelog rather than in a
compiler error. That is the only outstanding action.

---

## Fixed

### M-1 — No characterization test in any of the six commits → fixed

Four of the six commits changed behaviour and none shipped a pin. Two regressions reached `develop`
and **neither turned the suite red**; both were found by inspection.

Five test commits now pin every behaviour the modscan work changed. **Each was observed red against
the commit before its fix** — not written-then-asserted — with the observed failure text recorded in
the commit body:

| Pin | Reverted to | Observed failure |
|---|---|---|
| `autoDetectedClientsideModsStayDisabledWithoutUserExclusions` | `7004f3c88^` | `…must remain disabled with no user exclusions; disabled=[]` |
| `whitelistRescuesAutoDiscoveredClientsideMod` (was `assumeTrue`) | `7004f3c88^` | now FAILS where it previously **skipped** |
| `quiltArmReturnsEachJarExactlyOnce` | aggregation re-keyed to `modID` | `A jar must not be both included and disabled; both=[bbbbb.jar]` |
| `quiltModWithoutAnEnvironmentIsServerSide` | `bf226c2ac^` | `expected: <SERVER> but was: <CLIENT>` |
| `anUnreadableJarIsServerSideAndCarriesItsFilenameAsId` | `2ec5ff202^` | `expected: <brokenmod> but was: <N/A>` |

Three things the remediation established that are worth keeping:

**The two defects mask each other.** `quiltArmReturnsEachJarExactlyOnce` does *not* go red against
`7004f3c88^`; it had to be isolated by re-keying the aggregation on `modID` while leaving the
auto-exclusion fix in place. With auto-exclusion broken, everything lands in `serverMods`, the
disabled list is empty, and disjointness holds trivially. The second defect only becomes observable
once the first is fixed — which is why one commit fixing both left no test able to see either.

**The fixtures cannot express an absent field.** Every committed `fabric.mod.json` and
`quilt.mod.json` declares an `environment`, so the SERVER-when-undeclared default had *no* coverage —
which is how `bf226c2ac`'s defect got in. New cases build a real jar in a `@TempDir` from JSON written
inline, so the descriptor under test is visible in the diff and no binary enters the repository.

**A green characterization test proves nothing until you try to break it.** The new
exact-match-not-prefix assertion on `dependencyExclusions` was confirmed by mutating the regex to
`fabric.*` and watching `fabricDependenciesAreRecordedWithoutThePlatform` fail — `fabric-api-base`
must survive a filter that drops `fabric`.

**The commit boundary was verified, not asserted.** `b8f809ff8` was committed **red** and then checked
out and re-run to confirm it fails there. `CLAUDE.md` records eight commits where that boundary
collapsed; this one holds.

### M-4 — Unrecognised modloader yields an empty server pack → fixed

The scanner-selection `when` had no `else`, and since the rewrite the include-list is built solely
from what a scanner returned — so a loader string matching no arm returned two empty lists: a pack
with no mods and no warning, where the pre-rewrite code returned every jar.

Reachable from an ordinary `PackConfig`, not just an embedder passing something odd:
`PackConfig.modloader`'s setter assigns only on a match (`PackConfig.kt:328-341`), so an unrecognised
value leaves the field at its initial `""`, and that empty string reached the `when`.

Fixed in `2eafe1b34` — the `else` warns and enters every file as an unscanned SERVER mod, restoring
the prior contract while making the situation visible. Pinned by
`ModListCompilerTest.unrecognisedModloaderStillYieldsEveryMod`, committed red in `b8f809ff8`.

**A documentation error fell out of this.** `serverpackcreator-api/CLAUDE.md` claimed unknown
modloaders "default to **Forge**". They do not — the field keeps whatever it held, starting at `""`.
Corrected in `1f93755d2`. The wrong claim is what made the missing `else` look unreachable.

### I-6 — Dependency rescue could not fire for clientside mods → fixed

Excluding a mod something else depends on produces a pack that installs and then dies on load, which
is worse than shipping one mod too many — so a dependency has to win over a clientside verdict. The
rescue could never do that: it additionally required the *disabled* mod to be `Sideness.SERVER`, but a
mod auto-disabled by a scanner is `CLIENT` by construction, so the protection only ever reached mods
the scanner had judged server-side and the user had excluded by name.

Griefed dropped the clause from both the `while` guard and the `removeIf` predicate (`35c5787ad`).
Pinned by two cases in `ModListCompilerTest`, committed red in `d8dfeb4ac`:

- `aClientsideModDependedOnByAServerModIsRescued` — `servermod` (`environment: "*"`) depends on
  `clientlib` (`environment: "client"`); `clientlib` must be kept.
- `theDependencyRescueFollowsAChain` — `servermod → midlib → deeplib`, middle and leaf both
  clientside. This is what the surrounding `while` exists for: rescuing one mod puts *its*
  dependencies in play. Observed red as `expected: <[servermod.jar, midlib.jar, deeplib.jar]> but
  was: <[servermod.jar]>`, i.e. a single-pass rescue would have kept `deeplib` excluded and still
  looked like it worked.

Termination is unchanged: each iteration whose guard holds removes at least one entry from
`disabledMods`, and nothing is ever added back to it.

### M-2 — Exported `modID` default → pinned, still undocumented

`ScannedMod.modID` defaults to `file.nameWithoutExtension` for any jar the scanner cannot read. That
value is not a mod id, and nothing in the type says so. Now pinned by
`anUnreadableJarIsServerSideAndCarriesItsFilenameAsId`, which also pins that an unreadable jar stays
SERVER rather than being dropped.

The test now serves as the documentation. The KDoc gap itself is I-3 and remains open.

---

## Moot

### M-3 — Fixture cannot reach the Quilt fabric-fallback branch

Superseded by **N-1**: that branch cannot fire at all any more, so the missing fixture is no longer
the reason it is untested.

---

## New — found during remediation

### N-1 — The Quilt copy-loop is unreachable dead code

- **Commit:** `7004f3c88`
- **File:** `ModListCompiler.kt:149-154`

```kotlin
for (fabric in fabricScan) {
    if (quiltScan.find { quilt -> quilt.file.name == fabric.file.name } == null) {
        log.info("Quilt-scan did not have a scan for ${fabric.file.name}, Fabric-scan did, though. Copying entry. ")
        quiltScan.add(fabric)
    }
}
```

Both scanners return **exactly one entry per input file** whatever the outcome — each `scan` loop's
`catch` adds a bare `ScannedMod(modJar)` (`FabricScanner.kt:83-86`, `QuiltScanner.kt:88-91`) — and both
are called with the same `filesInModsDir`. So every `fabric.file.name` is always present in
`quiltScan`, the `find` never returns null, and the body never executes. Confirmed empirically: the
log line occurs **0 times** across the fixture.

This became dead when `7004f3c88` re-keyed the join from `modID` to `file.name`. Under the old key it
*did* fire — that firing is precisely what produced the duplicate entries the commit set out to stop.

Not a defect: the invariant it defends against is now guaranteed upstream, and that invariant is
itself pinned by `everyJarYieldsExactlyOneEntryWhateverTheOutcome`. But it is unreachable code that
reads as a live fallback, and the first loop above it already handles the only case that can occur.
Removing it (or reducing the arm to a single merge keyed on the file) would make the Quilt arm say
what it actually does.

---

## Still open

### L-1 — Unused import

`ModListCompiler.kt:25` — `import de.griefed.serverpackcreator.api.config.SupportedModloaders.quilt`.
The `when` arm still matches the string literal `"Quilt"`; `quilt` is referenced nowhere. A
build-level unused-import gate would close the category rather than the instance — this range also
contained a stray `import sun.util.calendar.CalendarUtils.mod`.

### L-2 — Value identity hand-rolled at six call sites

`ScannedMod` declares no `equals`/`hashCode`, so `remove`/`find` match by identity and cannot see a
sibling instance describing the same jar. Six `it.file.name == mod.file.name` comparisons work around
this. Correct today only because `filteredWalk` is called with `recursive = false` 40 lines away.

**Not to be fixed with a `data class`.** Equality by file would make two entries with conflicting
`sideness` silently interchangeable — exactly the merge the Quilt arm performs deliberately, where
which entry survives would then depend on insertion order. (A `data class` over `file` alone would
*not* produce a false negative for a jar declaring two different ids, since body properties are
excluded from the generated members — but the conflicting-verdict problem is the real one.) Extract a
single named helper comparing `file` instead, and identity stays explicit.

### L-3 — Log statement reads the value it just overwrote

`ModListCompiler.kt:145-146` logs `quiltScan[i].file.name` *after* `quiltScan[i] = match`. Correct only
by accident of the predicate directly above it guaranteeing the two names are equal. Read
`match.file.name`, or log before assigning.

### I-1…I-5, I-7, I-8 — Inherited from `0a12d41d0`

Out of the audited range; untouched by the follow-ups and by the remediation branch. (I-6 is fixed —
see above.)

| # | Condition | Location |
|---|---|---|
| I-1 | 8 `!!` non-null assertions introduced by the rewrite | `ForgeAnnotationScanner.kt` (4), `ForgeTomlScanner.kt` (2), `FabricScanner.kt` (1), `QuiltScanner.kt` (1) |
| I-2 | Per-scan state as mutable instance fields (`private var currentModID`) on scanners `ApiWrapper` holds as singletons, so concurrent `scan()` calls interleave | all four scanners |
| I-3 | 7 exported declarations carry zero KDoc, including the two load-bearing defaults now pinned by tests | `modscanning/ScanResult.kt:5-23` |
| I-4 | `var` in value types, populated by assignment after construction | `ScanResult.kt:7-8,17` |
| I-5 | File still named `ScanResult.kt` after `ScanResult` was deleted | `modscanning/ScanResult.kt` |
| I-7 | The four-branch `when (exclusionFilter)` is written 3 times in one function; the whitelist `while` re-evaluates a predicate `removeIf` has already exhausted | `ModListCompiler.kt:214-249` |
| I-8 | `ReadmeExamplesTest` KDoc and test name still describe the deleted `ScanResult` contract; `README.md` untouched | `ReadmeExamplesTest.kt:36,43,118,123` |

---

## Rules checked and found clean

- **Module boundaries.** No commit in range or in the remediation adds a Swing, Spring-web or frontend
  dependency to `-api`. All touch `-api` only.
- **Scope sprawl / Boy Scout.** Each follow-up touches 1–3 files, all reachable from its stated
  concern. The remediation touches two test files, one production file (M-4) and two `CLAUDE.md`s.
- **Commit labelling.** The four behaviour commits in range are correctly `fix:`; `d185fd74c` is
  correctly `test:`. `0df7b2835`'s `refactor:` is imprecise but withdrawn with H-1. The remediation
  keeps `test:`, `fix:` and `docs:` in separate commits, with the pin landing red before its fix.
- **Scanner correctness.** All four scanners pass `ModScannerTest` and the nine new
  `ModScannerSidenessTest` cases.

---

## Summary

| Severity | Total | Fixed | Withdrawn | Moot | Open |
|---|---|---|---|---|---|
| HIGH | 1 | — | 1 | — | 0 |
| MEDIUM | 4 | 3 | — | 1 | 0 |
| LOW | 4 | — | — | — | 4 |
| Inherited | 8 | 1 | — | — | 7 |

Every finding in the audited range is closed. The behaviour the modscan work changed is now pinned by
tests observed red against the commit before each fix, so the next regression fails the build instead
of skipping a test and writing an INFO log.

I-6 — the one inherited condition with a behavioural consequence — is fixed and pinned too, so
"taking care of dependencies" now reaches the population it was written for.

What remains is cosmetic: one unused import, one unreachable loop, one log statement reading a stale
local, and seven inherited conditions (the `!!`s, mutable scanner state, missing KDoc, the
`ScanResult.kt` filename, and the duplicated `when` blocks). None changes behaviour; they are a
tidy-up branch whenever it suits.
