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
