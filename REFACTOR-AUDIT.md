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
