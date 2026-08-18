# Refactor audit — `claude-perf-network-startup`

**Range:** `7abd7c85c..62c51e2d7` (6 commits) · **Branch:** `claude-perf-network-startup`
**Date:** 2026-08-17 · **Mode:** READ-ONLY. No source modified. Verification used a throwaway git
worktree under the scratchpad, plus hybrid checkouts (fix-commit production + pin-commit tests) to test
the red→green chain independently rather than trusting commit messages.

| Commit | Type | Verdict |
|---|---|---|
| `9fce12419` | test(api) | red claim **verified**; chain to its fix **broken** (F1) |
| `c124331b0` | fix(api) | central claim **overstated** (F2, F3); everything else verified |
| `04c0e571a` | refactor(api) | verbatim **verified**; visibility widened undisclosed (F4, F5) |
| `6b191480a` | test(api) | red claims **verified exactly** |
| `76dea6527` | fix(api) | red→green chain **verified clean**; figures internally consistent |
| `62c51e2d7` | docs | one inaccurate completeness claim (F3) |

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

This is precisely the defect `9fce12419` was written to pin and `c124331b0` claims to have eliminated. A
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

`c124331b0` states: *"Every outbound call now goes through one opener."*
`serverpackcreator-api/CLAUDE.md:208-211` states the rule and adds: *"until 2026-08-17 that was every
single call site."*

Measured at `c124331b0` and still true at branch tip — sites **not** routed:

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

- At `9fce12419`: `WebUtilitiesTimeoutTest` — 2 tests, **both FAILED** ("did not return within 15s").
  The red claim is genuine, and caused by the real defect (production consulted no timeout at all).
- Hybrid (production from `c124331b0`, test file exactly as committed at `9fce12419`): **both still
  FAILED**, same message.

Cause: the pin used `mockk<ApiProperties>(relaxed = true)`, which answers `0` for an `Int`, and `0` *is*
the JDK's "wait forever". Once the fix made production read those properties, the fixture supplied the
defect itself. `c124331b0` therefore had to edit the already-committed test (+`timedProperties()`, two
call-site swaps) to turn it green.

So `git checkout 9fce12419 && <apply fix> ` shows **red → red**, not red → green. The assertions and the
15s bound are unchanged — only the fixture — and `c124331b0`'s message discloses this in full ("both
stall-guards failed against the *fixed* code until these stubs were added"), which is why this is MEDIUM
and not HIGH. But the conventions' evidence chain is the point of committing the pin separately, and here
it does not hold. The same trap is now documented as a landmine, which is the right outcome.

For contrast, the second pair is clean and was verified the same way: `6b191480a` red on exactly the three
stated tests (2 requests vs 1, twice; `If-Modified-Since` absent), and the **unedited** pin from
`6b191480a` passes all six against `76dea6527`'s production code.

### F4 — The "verbatim move" widened published API surface, and its own commit message does not say so

`04c0e571a` moved `checkManifest` from `private fun` in `VersionMeta` to **`fun`** (public) in a **public**
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

`76dea6527` adds `anUnreachableHostLeavesThePresentManifestIntact` (+24 lines in
`ManifestUpdaterTest.kt`) alongside the behaviour change.

The convention keeps "add tests" and "change behaviour" in separate commits. The guard covers a behaviour
the same commit deliberately *preserved* (offline stays a WARN, not twelve ERRORs), so bundling is
defensible and the message explains it — but it is still a mix, and it means that guard has no red
ancestor. Nothing verifies it would have failed had the WARN path been written differently.

---

## LOW

### F6 — `ModpackZipInspector`-style testability seam absent here, so one claim rests on the message alone

`c124331b0` asserts warning counts ("21 before, 21 after") and suite counts. Counts were re-verified at
branch tip (21 for `-api`), but per-commit warning counts are not reproducible from the repository alone.
No action needed — noted only so a future reader knows which figures in these messages are re-checkable
and which are testimony.

### F7 — Verified-correct claims, recorded so they are not re-litigated

Checked and **holding**, each independently:

- No `setConnectTimeout`/`setReadTimeout` anywhere before the branch (`git grep` at `9fce12419^`: 0 hits).
- `URLConnection` (not `HttpURLConnection`) as the opener's return type is load-bearing: the 4-arg
  `JarUtilities.copyFileFromJar` chain resolves `getResourceAsStream("/$fileToCopy")` — an **absolute**
  path — so the `VersionMeta::class.java` → `ManifestUpdater::class.java` swap in `04c0e571a` is genuinely
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

**Range:** `claude-perf-network-startup..8117ec0fa` (7 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Verified the same way: throwaway worktree, plus hybrid checkouts (fix-commit production + pin-commit
tests, unedited) to test each red→green chain independently.

| Commit | Type | Verdict |
|---|---|---|
| `1fad7db42` | refactor(app) | clean; behaviour-preserving move, dead param removed |
| `a368be12d` | refactor(api) | adds **published API** with no compatibility-table row (G3) |
| `71dac2b2f` | test(app) | red **verified** (3 of 13) |
| `e6c529754` | fix(app) | chain **verified clean** red→green, pin unedited; but ships NUL bytes (G1) |
| `7fa1bb39a` | test(app) | red, but the guard is **wrong** — it can never pass (G2) |
| `5a1cbc315` | fix(app) | rewrote that guard's assertion; disclosed (G2) |
| `8117ec0fa` | docs | accurate; figures are testimony, noted |

## MEDIUM

### G1 — A Kotlin source file contains NUL bytes, so git records it as binary

**`serverpackcreator-app/.../configs/ConfigEditorViewModel.kt:107`**

```
val triple = "$minecraftVersion\x00$modloader\x00$modloaderVersion"
```

Two `0x00` bytes at offsets 5786 and 5797, where spaces were intended. `git show --stat` for
`e6c529754` reports `Bin 5266 -> 8399 bytes` — the diff of that commit is **unreviewable**, and every
future diff of this file will be too. The file is still valid UTF-8 and compiles.

Not a runtime defect: the separator is used consistently when writing and reading the key, and NUL cannot
appear in a version string, so lookups are correct and collision-proof. It is a hygiene and reviewability
defect — an invisible control character in source that no one wrote deliberately, which will confuse the
IDE, Qodana and any human reviewer.

**Fix:** drop the string key entirely and use Kotlin's `Triple` as the map key — collision-free by
construction, no separator to choose, and the intent is visible.

### G2 — The autocomplete pin's *assertion* was rewritten by its own fix, so the committed guard can never pass

`7fa1bb39a` commits `anUnchangedSuggestionListIsParsedOnce` asserting
`verify(exactly = 1) { guiProps.getGuiProperty("autocomplete.clientmods") }`.

`5a1cbc315` changes that same guard to `verify(exactly = 20)` and swaps `assertEquals(size)` for
`assertSame(identity)`.

This is the conventions' explicit stop-and-flag signal — a changed *expectation*, not a fixture tweak —
and it is worse than the equivalent finding on the previous branch (F1). There, the pin was correct and
merely defeated by its fixture. Here the pin was **incorrect**: it demanded that the property be read once
per query, which the design deliberately does not do (the memo is *keyed* on the property value, so the
cheap read must happen every time). Anyone checking out `7fa1bb39a` sees a guard that no correct
implementation can satisfy.

Mitigating, and the reason this is MEDIUM: `5a1cbc315`'s message states all of this plainly, and the
replacement assertion was verified to have teeth by deliberately defeating the cache and observing it fail
on identity while contents matched. The lesson is already recorded as a landmine. No code fix is needed —
the guard is correct now — but the branch's red commit is a misleading artifact.

### G3 — New published API added in a `refactor:` commit, with no compatibility-table row

`a368be12d` adds two public methods to a module published to Maven Central:

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
- `1fad7db42` removes `ConfigCheckTimer`'s now-unused `apiWrapper` parameter and `java.io.File` import, and
  the throwaway `PackConfig` per tick. Boy-Scout, within scope, no sprawl.
- Chain `71dac2b2f` → `e6c529754` verified by hybrid checkout: 3 of 13 red before, all 13 green after with
  the pin **unedited**. This is the pattern the other chains should follow.
- The performance figures in `e6c529754` and `8117ec0fa` (4.70 ms vs 0.021 ms parse/fingerprint on the
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

**Range:** `claude-perf-gui..200588627` (8 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Both red→green chains verified by hybrid checkout (fix production + pin tests, unedited).

| Commit | Type | Verdict |
|---|---|---|
| `004e345c6` | test(api) | red **verified** (3/3) |
| `1bf414e41` | fix(api) | chain **verified clean** red→green, pin unedited |
| `78eb879a2` | test(api) | contains **production code** (H1) |
| `e7da71ade` | fix(api) | **three concerns in one commit** (H2); one undisclosed error-path change (H4) |
| `c9d1b8b4b` | test(api) | red **verified** (1/12) |
| `8426f8f98` | fix(api) | chain **verified clean** red→green, pin unedited |
| `16f1a148c` | refactor(api) | published property changes shape, no compat row (H3) |
| `200588627` | docs | accurate, including the corrected estimates |

This is the strongest branch of the four on evidence: both pins were genuinely red on exactly the stated
tests, and both go green with the pin **untouched**. It is the weakest on commit hygiene.

## MEDIUM

### H1 — A `test(api):` commit ships production code

`78eb879a2` adds `ModpackZipInspector`'s defaulted `openZip: (File) -> ZipFile` constructor parameter
(+18 lines of production) alongside its guard.

The convention keeps "add tests" and "change production" apart. The message discloses it and the reasoning
is real — open-counts are invisible from outside, so the guard cannot exist without the seam, and the
parameter is inert until the next commit — but the label says `test` and the diff says otherwise. Either
the seam belonged in its own `refactor(api):` commit first, or the commit should have been labelled for
what it contained.

### H2 — A `fix(api):` commit bundles three unrelated changes

`e7da71ade` contains:

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

`16f1a148c` turns `ForgeAnnotationScanner.dependencyCheck` / `dependencyReplace` from
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

`e7da71ade` reduces `getAllFilesAndDirectoriesInModpackZip` from **two** `catch` blocks to **one**
(verified: 2 occurrences of "Could not acquire file or directory" before, 1 after).

Before, directories and files were fetched by separate calls, each with its own `try`/`catch`, so a failure
in one still returned the other's results plus one logged error. Now a single failure loses both and logs
once. The commit message describes the single-pass optimisation but not this consequence.

Barely reachable in practice — both old calls opened the *same* archive, so a failure in one would almost
certainly fail the other — which is why it is LOW rather than MEDIUM. Worth recording because "we now lose
partial results on failure" is the kind of change that surprises someone reading the error log later.

### H5 — Verified-correct, recorded so it is not re-litigated

- Both chains verified by hybrid checkout: `004e345c6` red on exactly its three stated tests → all green
  under `1bf414e41` with the pin unedited; `c9d1b8b4b` red on its one test → green under `8426f8f98`,
  likewise unedited. This is the discipline the earlier branches' first pins lacked.
- `e7da71ade` uses `putIfAbsent` rather than `associateBy` in the Quilt merge, preserving `find`'s
  first-match-wins. Correct, and the message explains why — a real distinction, not pedantry.
- `checkZipArchive`'s `val foldersInModpackZip` is assigned inside the `use` block and read after it;
  definite-assignment holds because the early-return path precedes the assignment. Compiler-enforced,
  checked rather than assumed.
- The docs commit records that the branch's headline estimate was **wrong** (the `exclusionFilter` read is
  worth ~3 ms, not a substantial win) rather than quietly dropping it. That is the behaviour the
  conventions ask for.

---

# Refactor audit — `claude-perf-web`

**Range:** `claude-perf-generation..bf408b13a` (8 commits) · **Date:** 2026-08-17 · **Mode:** READ-ONLY
Both red→green chains verified by hybrid checkout (fix production + pin tests, unedited).

| Commit | Type | Verdict |
|---|---|---|
| `c51e582c2` | test(app) | red **verified** (1/2) |
| `4c710c676` | fix(app) | chain **verified clean**, pin unedited |
| `bff391a7e` | refactor(app) | clean, genuinely behaviour-preserving |
| `99d9d01ec` | test(app) | ships **production code** (W2) |
| `cf6fc1d35` | fix(app) | chain **verified clean** (3/3 red → 3/3 green), pin unedited |
| `cb5d264d5` | fix(app) | 18 files, big-bang; **not deployable on its own** (W3, W4) |
| `4ff94614a` | feat(app) | migration **runner is untested** (W1) |
| `bf408b13a` | docs | accurate |

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

`99d9d01ec` adds `@Indexed` to `ModPack.sha256` (+3) and `findBySha256` to `ModPackRepository` (+9).
Identical in kind to H1 on the previous branch, disclosed the same way, and wrong the same way: the label
says `test`, the diff includes production. Twice on one stack means the pattern, not the slip, is the
finding — when a guard needs new surface to exist, that surface belongs in a preceding
`refactor:`/`feat:` commit.

### W3 — `cb5d264d5` is a big-bang change across 18 files

One commit deletes three `@Document` classes and four repositories, retypes three entity fields, rewrites
a service, changes a derived query, rewrites a test (deleting four cases), and changes five frontend
files including two Vitest fixtures.

The convention asks for incremental change behind stable interfaces. The counter-argument is real and
stated in the message: the field type *is* the change, so nothing compiles between the halves, and the
frontend consumes the same JSON contract. But "it cannot be split" is not quite true — the frontend could
have moved in its own commit after the backend, since the SPA is built and deployed from the same tree but
is not compiled against Kotlin.

### W4 — An intermediate commit leaves the application unable to read its own data

`cb5d264d5` changes the persisted shape; the migration arrives only in `4ff94614a`. Deploying or bisecting
to `cb5d264d5` gives an application whose mapped type cannot read existing `runConfiguration` documents.

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

- Both chains verified by hybrid checkout: `c51e582c2` red on its one stated test → green under
  `4c710c676`; `99d9d01ec` red on all three → green under `cf6fc1d35`. Pins unedited in both cases.
- **Writing inside a `find()` cursor is safe here**, though only because the rewrite is idempotent: a
  document returned twice by a moving cursor fails `needsRewrite` on the second visit and is skipped.
  Worth recording, since the same loop would be unsafe if the transformation were not idempotent.
- `replaceOne` is handed a `migrated` document that still carries its original `_id`, so the replace is a
  true in-place update rather than an insert.
- The `In`-means-contains-any bug fixed in `cb5d264d5` is real and was found while reading, not by a test —
  surfaced explicitly in the message rather than silently corrected, which is what the conventions ask.
- Four tests were **deleted** in `cb5d264d5` because the behaviour they pinned ceased to exist, and the
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

Rebasing the stack, `git rebase --onto claude-perf-generation 72ad406a2^ claude-perf-web` used the wrong
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
| G2 — autocomplete pin asserted the wrong thing | **Fixed.** `parsedSuggestions()` extracted first, pin holds the identity assertion, red on `@186d20a3` vs `@74ab779f` with equal contents. |
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

Examples: `REFACTOR-LOG.md` cites `41607582`, `5d30890b`, `350d7cb9`, `f7fdcff3` for the generation work
and `3d25dd22`, `84aa970a`, `c67e021a`, `549d7e30`, `7acc5fdc`, `16a3f399` for the web work — the entire
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
> `checkout 9fce12419 && apply c124331b0` shows **red → red**, not red → green. The later timeout pins
> […] were written against the *existing* signatures precisely so their fixes turn them green untouched —
> **copy those, not the first one.**

Two problems. The hashes are dead (N1), and the claim is **no longer true**: the F1 remediation reordered
that pair so the pin *does* go red → green untouched. A future reader is told to avoid copying the
`WebUtilitiesTimeoutTest` pattern, which is now the correct one and the most thorough example on the
branch.

The underlying lesson — a relaxed mockk answers `0`, which is the JDK's "wait forever" — is still valuable
and must survive; only the "and therefore this pair is a bad example" conclusion is stale.

### N3 — The remediation created a third instance of the violation it was fixing

`c90b5ec5d test(app): cover the migration runner's safety decisions, behind a MigrationStore seam` ships
**two main-source files** (`MigrationStore.kt`, and the runner rewired onto it).

This is precisely H1/W2 — a `test:`-labelled commit containing production code — committed *while* those
two were being split apart for the same reason. Applying a standard to inherited work and not to one's own
is the worse failure of the two.

Fix: split into `refactor(app): put the migration's database access behind MigrationStore`
(production, behaviour-preserving) followed by the guards.

## LOW

### N4 — Two new guards ride along in a `fix:` commit

`5625223a2 fix(api): route every outbound call through one timed opener` adds
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
