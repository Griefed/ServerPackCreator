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
