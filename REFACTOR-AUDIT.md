# Refactor audit — `claude-modscanning-generification`

**Scope:** `git log develop..HEAD` — 16 commits (`f8cb89bff` … `94b6a8c1b`), audited against the
Refactoring Conventions. **Read-only: no source was modified.**

**Verdict:** one HIGH, six MEDIUM, three LOW. The HIGH is a staging error, not a logic error — the
code at HEAD is correct and the full build is green, but one commit's contents do not match its
message, which makes part of this branch's history untrustworthy for `git blame` and for anyone
bisecting.

**Remediation (2026-08-15, on Griefed's go-ahead): H-1 and M-3 are FIXED.** The branch was rebuilt
from `d13252234`; the two rebuilt commits and the eight replayed ones are unchanged in content except
as described below, and the final tree differs from the pre-remediation tip by exactly the two new
test files. `./gradlew build` green; suites api 302 (1 skip), clientside 88, app **102** (was 88),
grinder 233 (19 skip), plugin-example 3 — **728 total**. Superseded findings are marked inline; M-1,
M-2, M-4 – M-6 and the LOWs are **open and accepted**.

---

## HIGH

### H-1 · ~~FIXED~~ · Four `-api` production refactors are committed inside a `test(app)` commit — and the `refactor:` commit that claims them contains none of them

**Commits:** `73e16ba9c` (holds the code), `25b83e8e9` (claims it)
**Rule broken:** *One concern per commit. Keep "add tests" and "refactor (no behavior change)" in
separate commits.*

`73e16ba9c` is labelled `test(app): characterize VersionChecker's pre-release comparison`. Its
diffstat:

```
serverpackcreator-api/.../config/ConfigurationHandler.kt          |   7 +-
serverpackcreator-api/.../utilities/common/BooleanUtilities.kt    |  29 ++---
serverpackcreator-api/.../utilities/common/FileUtilities.kt       |  18 ++-
serverpackcreator-api/.../utilities/common/JsonUtilities.kt       |  12 +-
serverpackcreator-app/.../versionchecker/VersionCheckerTest.kt    | 137 +++++
```

Four `-api` production files — a different module from the commit's `(app)` scope, and a different
concern from "add tests". They are the `RedundantIf` collapses:

- `BooleanUtilities.convert` — `if/else if/else` → `when` (`BooleanUtilities.kt:91-107`)
- `JsonUtilities.getNestedBoolean` — `if/else if/else` → `when` (`JsonUtilities.kt:136-142`)
- `ConfigurationHandler.checkIconAndProperties` — collapsed to `||` (`ConfigurationHandler.kt:415`)
- `FileUtilities.isLink` — collapsed to `||`, `catch (ex)` → `catch (_)` (`FileUtilities.kt:153-159`)

The next commit, `25b83e8e9 refactor: collapse the redundant conditionals Qodana flagged`, names all
four in its message and explains at length *why* `BooleanUtilities` and `JsonUtilities` became `when`
rather than `||` (the `log.warn` side-effect; `toBooleanStrictOrNull` being case-sensitive). Verified
against the actual trees:

| file | in `25b83e8e9` | in `73e16ba9c` |
|---|---|---|
| `BooleanUtilities` | 0 | 1 |
| `JsonUtilities` | 0 | 1 |
| `FileUtilities` | 0 | 1 |
| `ConfigurationHandler` | 0 | 1 |

**Why this matters beyond tidiness.** This is the failure mode `serverpackcreator-api/CLAUDE.md`
already records for the `modFileEndings`/`zipCheck` copies: *the explanation lives on one artifact
while the code lives on another.* Someone running `git log -- BooleanUtilities.kt` to learn why that
`when` has a comment about warning side-effects lands on a commit about `VersionChecker`, whose
message says nothing about it. Additionally, `73e16ba9c` was committed after running only
`:serverpackcreator-app:test --tests '*VersionCheckerTest*'` — the `-api` suite was **not** run
against those four files until `25b83e8e9`, two commits later. It did pass there, so no defect
shipped, but that commit was never verified green when it was made.

**Cause:** `git add -A` staging without checking `git status` first.

**FIXED.** The branch was rebuilt from `d13252234`: `ec79084bd` is the test commit carrying **only**
`VersionCheckerTest.kt`, and `4e1669456` is the refactor commit carrying all ten files its message
describes, the four `-api` ones included. Verified afterwards — each of the four now reports `1` in the
refactor commit and `0` in the test commit, and the rebuilt tree is byte-identical to the original
`25b83e8e9` tree at that point. Done before pushing, which is the window `CLAUDE.md` describes for
`358675fbf`, where the same class of problem became unfixable after merge.

---

## MEDIUM

### M-1 · `d04a62a79` is a big-bang rewrite of `modscanning` in one commit

**Rule broken:** *Refactor incrementally behind stable interfaces (Strangler Fig). No big-bang
rewrite of a module in a single commit.*

13 files, +506/−397, carrying **four** independent extractions that were each viable alone:

1. replace `internal Scanner<T,U>` with public `ModJarScanner`
2. add `DescriptorScanner` (the walk-the-jars template method) and move 5 scanners onto it
3. add `FabricFamilyScanner` (Fabric/Quilt sharing)
4. move loader→scanner dispatch onto `ModScanner.scannerFor` + add `QuiltPackScanner`, rewiring
   `ModListCompiler` *and* `-clientside`'s `MetadataScanner`

Each step keeps the suite green independently and (1)–(3) are invisible to callers, so there was no
technical reason to land them together. A reviewer now has to hold all four in their head at once,
and a bisect landing here cannot tell which extraction caused a regression.

*Mitigating:* no existing test was touched (0 test files in the commit), and the commit message does
enumerate the four steps.

### M-2 · `d04a62a79` changes logging behaviour inside a `refactor:` commit

**File:** `ForgeAnnotationScanner.kt`, `ModListCompiler.kt`
**Rule broken:** *Never mix a refactor with a behavior change.*

Four log statements were removed or altered:

```
- log.error("Could not scan ${modJar.name}. Consider reporting this:", e)   // lost its stack trace
- log.error("Could not scan ... no modId in the annotation cache.")          // moved, not lost
- log.debug("Scanning using NeoForge scanner.")
- log.debug("Scanning using Forge scanner.")
```

Log output is observable behaviour, and the stack trace is the one that matters: an unparseable
1.12-era jar now reports `${e.cause}: ${e.message}` where it used to give a full trace. A strict
reading of the rubric makes this HIGH ("behaviour change mixed into a refactor"); it is filed MEDIUM
because no functional contract changed and the commit message discloses both changes explicitly.

### M-3 · ~~FIXED~~ · `4a30aa4d9` changes two units that have **zero** real test coverage

**Files:** `EventService.kt:51-56`, `RunConfigurationService.kt:61-64, 76-79, 91-94`
**Rule broken:** *Never refactor untested code blind. Write characterization tests first and commit
them on their own.*

Both loops went from two repository lookups per element to one:

```kotlin
- if (repo.findByX(item.x).isPresent) { list[i] = repo.findByX(item.x).get() } else { … }
+ val stored = repo.findByX(item.x); list[i] = stored.orElseGet { repo.save(list[i]) }
```

Neither service has a test. `RunConfigurationControllerTest` and `EventControllerTest` exist but
**mock the services** (`private val runConfigurationService: RunConfigurationService = mockk()`), so
they exercise none of this. The change is almost certainly equivalent — but "almost certainly" is
what characterization tests exist to replace, and the persistence layer is MongoDB, where a
save-on-absent path is not trivially reasoned about from the source.

**FIXED, and in the right place.** `d603378d9` adds `EventServiceTest` (5) and
`RunConfigurationServiceTest` (8) and is inserted **before** `d37fc61cc` (the former `4a30aa4d9`), so
the tests were written against, and verified green on, the two-lookup code they characterize — then
stayed green when the change was replayed on top. That is what turns "almost certainly equivalent"
into evidence. They pin the outcome (which entries the built object holds, which reach `save`) and
deliberately **not** the lookup count, since pinning an implementation detail would have made them red
for the very next commit.

*Also:* halving the query count is a performance change riding in a `refactor:` commit. Disclosed in
the message, but it is a second concern.

### M-4 · `9a3736853` pins the dispatch **after** the refactor it guards

**Rule broken:** *Before refactoring any unit, ensure characterization tests exist that pin its
current behavior.*

`ModScannerDispatchTest` (6 tests, identity-asserted) is exactly the right guard for
`ModScanner.scannerFor` — but it lands one commit *after* `d04a62a79` created it. During the
refactor itself, the dispatch was covered only by outcome (`autoDiscoveryReachesScannerBranchPerLoader`
asserts jar partitioning, not which scanner ran), which is the weaker "asserts shape, not behaviour"
form `CLAUDE.md` warns about.

*Mitigating:* the units being restructured (the five scanners) *were* well covered beforehand by
`ModScannerTest` / `ModScannerSidenessTest` / `ModListCompilerTest`, so this was not a blind refactor
— only the new seam was unguarded, and briefly.

### M-5 · `a5736776e` mixes three concerns

**Rule broken:** *One concern per commit.*

The commit contains (a) a refactor — hoisting the duplicated lambda-suffix regex into
`MigrationManager.LAMBDA_SUFFIX`, (b) a **new test** for it (`MigrationManagerTest.kt`, +26), and
(c) the behaviour-neutral multi-dollar literal conversions across five files in two modules.

The message argues (a)+(b) belong together under the enabling-change carve-out `CLAUDE.md` records
for per-parameter KDoc, and that is defensible — you cannot test a private literal. It does not cover
(c), which is unrelated to the extraction and could have been its own commit.

### M-6 · `694cb2120` mixes two unrelated concerns across two modules

**Rule broken:** *One concern per commit; don't let cleanup sprawl across unrelated files.*

- `-clientside`: `ClientsideModels.kt` — repair 4 dangling KDoc links (`Project` → `ProjectFiles`)
- `-app`: `MigrationManager.kt` — drop a redundant `inner` modifier, plus the consequent
  `MigrationManagerTest.kt` receiver change

A documentation fix and a class-modifier change, in different modules, sharing only the fact that
Qodana reported both. The `fix(docs)` type also understates it: the `inner` removal is a code change,
not a docs change.

*Note:* the test edit itself is fine — receiver-only, every assertion byte-identical, which is the
carve-out `CLAUDE.md` grants for symbol moves.

---

## LOW

### L-1 · `0bbc0234f` is typed `docs(...)` but modifies Kotlin source

Adds `@Suppress("DEPRECATION")` annotations to `ScriptTemplatesConfig.kt` and `MigrationManager.kt`.
Annotations are code, not documentation. `chore(...)` or `refactor(...)` would be honest. Behaviour
is unaffected, and the suppressions were verified effective (7 → 0 warnings).

### L-2 · `4dbf653cc` bundles two behaviour changes

The Forge era fix, plus a change of error posture — an unparseable Minecraft version now falls back
to the modern scanner in `ModListCompiler` where it previously threw `IndexOutOfBoundsException` out
of `compileModList`. The conventions permit grouping *related* behaviour changes, and unifying the
two call-sites' posture genuinely was a precondition for the later shared dispatch. Disclosed in the
message. Recorded only for completeness.

### L-3 · Untested view/demo units refactored without characterization tests

`ConfigEditor.checkJava` (guard-clause inversion), `InclusionsEditor.canImport` (collapsed to `&&`),
`Tetris` (6 range-check conversions). None has a test. All are Swing view code or example-plugin
demo code where the project has explicitly decided not to invest in tests, and all six Tetris changes
are mechanical (`x < 0 || x >= n` → `x !in 0 until n`). Flagged for the record, not for action.

---

## Conventions upheld (verified, not assumed)

- **Pin-first with observed red, in its own commit** — done correctly twice, and the failing output is
  quoted in each commit body: `f8cb89bff` → `4dbf653cc` (Forge era) and `1c7804977` → `96eccff59`
  (pre-release ordering). `1c7804977`'s message also records that the second pin took three attempts
  before it failed for the right reason.
- **No existing test assertion was changed by a `refactor:` commit** — verified: `d04a62a79`,
  `25b83e8e9`, `4a30aa4d9`, `d13252234` touch **zero** test files; `a5736776e` only *adds* one.
- **Bug found during refactor was surfaced and fixed in its own commit, not worked around** — the
  Forge era defect was fixed before any restructuring began.
- **No Kotlin idiom regressions** — `git diff develop..HEAD -- '*/src/main/*.kt'` introduces no new
  `!!` and no new `var`.
- **Documentation kept in separate commits** — `d76e2edad`, `a1d81990f`, `94b6a8c1b`.
- **A stale claim in the module docs was corrected rather than deleted** — the `-api` versioning-scheme
  landmine had asserted the Kotlin side was clean; `d76e2edad` records that it was not, and why the
  earlier survey missed it.

## Not a violation, but your call to confirm

`d13252234` removes the published `JsonBasedScanner`, which the rubric would classify HIGH ("changed
plugin-API contract"). It is excluded from the findings because it was **explicitly requested** after
the alternative (a deprecated facade) was implemented and presented, and the break is recorded in the
root `CLAUDE.md` compatibility table. Listed here so the decision stays visible rather than buried.

---

## Suggested order of remediation

1. **H-1** — rebase the four `-api` files from `73e16ba9c` into `25b83e8e9`. Do it before pushing;
   after that the honest remedy becomes a follow-up note instead of a fix.
2. **M-3** — write characterization tests for `EventService` / `RunConfigurationService`, or revert
   the double-lookup change until they exist.
3. **M-1 / M-2** — history-only; fixable in the same rebase as H-1 by splitting `d04a62a79`, or
   accept as-is with the rationale already in the message.
4. **M-4 / M-5 / M-6 / L-1** — cosmetic history issues; no action needed unless you want the log clean.
