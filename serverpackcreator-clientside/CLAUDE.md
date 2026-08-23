# serverpackcreator-clientside — module context

> The **clientside-mod verification engine**, extracted out of `serverpackcreator-app` so it can be
> reused by both the app's CLI verbs and the planned standalone Docker "grinder" service. Package
> `de.griefed.serverpackcreator.clientside`. **Depends only on `serverpackcreator-api`** (plus
> Playwright + jackson-module-kotlin) — it must never gain a dependency on `-app`, Spring, or Swing.
> Not published to Maven Central (unlike `-api`), so it can churn freely without compatibility lock-in.

## What it does

Given a Modrinth/CurseForge project-link: pick the platform, resolve the project's files, derive the
clientside-list file-name stem(s), and combine signals into a per-loader `Confidence`. Driven by the
app's four CLI verbs (`-scan`, `-clientsidereport`, `-verifyclientside`, `-clientsideapply`) and the
`clientside-*.yml` workflows — see `serverpackcreator-app/CLAUDE.md` for the verb wiring and CI.

## Engine details & landmines (durable)

- **`MetadataScanner` no longer mirrors `ModListCompiler` — since 2026-08-15 both dispatch through
  `ModScanner.scannerFor(modloader, minecraftVersion)` in `-api`.** It *is* shared code now; do not
  re-add a local `when (loader)` over the concrete scanners. The old "kept in sync deliberately" note
  is why this entry exists: the two copies had already drifted, and both carried the same Forge
  era bug (Minecraft's `YY.x.y` scheme read as `1.x` — see the versioning-scheme landmine in
  `serverpackcreator-api/CLAUDE.md`), so the metadata signal was degraded on every modern Forge mod.
  A loader `scannerFor` does not know yields `null`, which this class reads as `SERVER_OR_BOTH`:
  nothing was read, so nothing declared the mod client-only.
- **Platform layer**: `ModPlatform` (Modrinth/CurseForge) over an injectable `HttpFetcher` so tests use
  canned JSON (no live network). **CurseForge has no sideness field** → `DeclaredSupport.UNKNOWN`; only
  Modrinth declares `client_side`/`server_side`. Use `JsonNode.textOrNull` for nullable URL fields —
  `asText(null)` returns the literal `"null"` for a JSON-null and would defeat `ModFile.locked`.
- **LANDMINE — a Modrinth version's `files[]` is not a list of mods.** Authors attach source jars, and
  Modrinth flags the real one `"primary": true`; `ModrinthPlatform.modFilesOf` keeps only those, falling
  back to *every* file of a version that flags none (3 of `creativecore`'s 300 versions genuinely do, and
  dropping them would lose real builds). Without it a source jar is a candidate for everything a `ModFile`
  feeds: the published list-entry, the jar-scan sample, and — order permitting — the boot itself.
  **What it cost, measured against the live API on 2026-08-23:** `creativecore`'s Fabric group holds 143
  files, one being the stray `CreativeCore-sources.jar`. That name shares no delimited prefix with the
  `CreativeCore_FABRIC_v*.jar` builds, so `FilenameStemDeriver` fell back to stripping the version off the
  *shortest* name and published `CreativeCore-sources` — an entry matching nothing the project ships. It
  also cost the mod its cross-loader disproof: `loaderDisprovingTheCrash` compares entries, and that stem
  matched neither other loader's `CreativeCore_`, so a Fabric crash stood as HIGH while NeoForge had booted
  a server in the same run. **CurseForge has no equivalent flag** — its file list is plain uploads, so an
  author who publishes a source jar as a normal file there is still unfiltered; nothing has been seen doing
  it, and there is no signal to act on if one does.
- **CurseForge file resolution pages; a dependency's deliberately does not.** `resolve` walks
  `/mods/{id}/files` with `index` until `totalCount` is reached, capped at `MAX_FILE_PAGES` (10 × 50) with a
  warning when it truncates. **Why:** the newest 50 files are 50 files *across all loaders*, so a project that
  migrated Forge → NeoForge keeps publishing NeoForge builds until its older Forge builds fall out of the
  window — leaving the crash re-check nothing of that loader to try, precisely for the projects that produce a
  false clientside verdict. Most projects still cost one call. `resolveDependency` stays single-page on
  purpose: it needs *a* usable file for one loader/Minecraft pair, not a history, and paging every dependency
  of every candidate would multiply what a catalog sweep spends of the API key's quota.
- **LANDMINE — `DeclaredSupport` and `api.modscanning.Sideness` are different concepts; do not merge them.**
  This module's enum was itself called `Sideness` until 2026-08-14, which made them look like duplicates
  of one idea. They are not, and the confidence model depends on the difference:

  | | `api.modscanning.Sideness` | `clientside.DeclaredSupport` |
  |---|---|---|
  | Values | `SERVER`, `CLIENT` | `REQUIRED`, `OPTIONAL`, `UNSUPPORTED`, `UNKNOWN` |
  | Is | SPC's own **verdict** — which side a mod belongs on | a platform's **self-report** about *one* side |
  | Shape | one value = the whole answer | read as a **pair** (`clientSide` + `serverSide`) |
  | Default | `SERVER` when undetermined, so nothing is dropped | `UNKNOWN` when absent (all of CurseForge) |

  `ClientsideVerifier.aggregate` folds this, `JarScan` (where the API's verdict arrives) and `BootResult`
  into a `Confidence` **precisely because the platform's claim is unreliable** — which is the whole reason
  the expensive boot-test exists. The domains are bridged deliberately at that one call-site, and it takes
  *two* `DeclaredSupport` values to derive one client/server leaning. Merging the enums would collapse the
  distinction the model is built on, and would push a third party's field vocabulary into `-api`, which is
  published to Maven and is a plugin-compatibility constraint. The rename removed a real hazard, not just
  an aesthetic one: `MetadataScanner` sits in *this* package and imports the API's `Sideness`, which
  silently shadowed the package-local type — so one file's `Sideness` meant the opposite of its neighbours'.
- **Boot signal** (`BootVerifier`): force-includes the mod (auto-exclude off, empty clientside-list) +
  its recursively-resolved required deps, generates a server pack and boots it via the ServerStarterJar.
  `BootLogClassifier` reads the `Done (…)! For help` ready-line vs a non-zero exit (pure, unit-tested).
  **Asymmetry baked into the confidence model:** only a CRASH is decisive (→ HIGH, incl. the
  "declares server/both yet crashes" lie); a clean boot does not prove server-safe. **The declared
  sideness is a self-report and is unreliable** — that asymmetry is *why* the expensive boot exists.
  **Landmine — loader-availability & pre-launch aborts (two-layer defense against a false HIGH):**
  a loader lacking a build for a brand-new Minecraft (e.g. Fabric on 26.2) must never be scored a
  clientside crash. (1) *Selection gate* — `LoaderVersionResolver.latest` returns `null` for a
  Minecraft the loader doesn't support: Forge/NeoForge are already MC-specific, and Fabric/Quilt/
  LegacyFabric are gated on `Meta.isMinecraftSupported(mc)` (the intermediary check). A `null` drops
  the combo from `BootVerifier.prepareBootPack`'s candidate selection, so no container is even spun up.
  (2) *Boot backstop* — if a boot still aborts pre-launch, `start.sh`'s `crashServer` messages (loader
  not available, launcher-jar/install download failure, Java/EULA/`variables.txt` setup failure,
  unknown modloader) exit non-zero *before the mod is loaded*; `BootLogClassifier` maps those
  (`setupAbortMarkers`) to **INCONCLUSIVE**, not CRASHED. Both found via a grinder e2e on MC 26.2.
  **(3) Killed-from-outside backstop** — `killedExitCodes` (137 `SIGKILL`, 143 `SIGTERM`) and `outOfMemoryMarkers`
  (`java.lang.OutOfMemoryError`, `insufficient memory for the Java Runtime Environment`, `Cannot allocate memory`,
  the shell's `Killed "$JAVA"`) also map to **INCONCLUSIVE**. Found 2026-07-30 while sizing a catalog sweep: the
  grinder caps a boot at 3 GiB but the host's Docker VM held **1.93 GiB**, so the cap cannot be honoured and a fat
  mod is OOM-killed by the VM — exit 137, no ready-line, and `Killed "$JAVA"` deliberately doesn't match the
  setup-abort markers, leaving `CRASHED` as the only outcome, i.e. **a HIGH-confidence "clientside" produced purely
  by host memory pressure**, systematically, for the biggest mods. `SIGABRT` (134) is deliberately *not* excused: a
  fatal JVM abort is a real failure of the running server. Keep the guard narrow — a genuine mod-load crash
  (`NoClassDefFoundError: net/minecraft/client/…`) must still read CRASHED, and a test pins that.
- **A crash that contradicts the metadata is re-checked on the mod's other versions.**
  `recheckCrashOnOtherModVersions`, over the pure `shouldRecheckAgainstOtherVersions`,
  `reconcileOtherVersionRecheck` and `BootCandidateSelector.pickRecheckCandidates`.
  **Why:** only one build of a project is ever booted, so
  "this build crashes" and "this mod cannot run on a server" produced identical evidence — reported
  2026-08-23, `iron-chests` published `HIGH` off a single crashing `Forge 48.1.0 / Minecraft 1.20.2`. A mod
  that cannot run server-side cannot run server-side in *any* build, so one clean boot on another version
  clears the crash. Stops at the first clean boot.
  **The sample is deliberately diverse, not simply the next-newest builds** (2026-08-23). Each pick
  introduces a Minecraft **version-line** (`minecraftLine` = the first two components, so `26.1.2` and `26.1`
  are one line and `26.2` another — the granularity at which mod source actually differs) *and* a loader that
  no earlier pick used, newest Minecraft first, with the crashing combination's own line marked used from the
  start. **Why:** `creativecore` — a mod whose own description advertises server features — crashed on
  Fabric / MC 26.2 and spent both re-checks on Fabric 26.1.2 and Fabric 26.1, same loader, same loader
  version `0.19.3`. Both INCONCLUSIVE, so the crash published HIGH, while NeoForge 26.1.2.97 booted a server
  for the same project *in the same run* and the CurseForge run minutes earlier booted the very file the 26.1
  re-check gave up on. Two boots that close to the crashing combination re-test its environment, not the mod.
  The same budget now buys Fabric 26.1.2 + NeoForge 1.21.11 — verified by running the real
  `ModrinthPlatform` + `pickRecheckCandidates` over the project's live 300-version response, which is also
  where the `CreativeCore_FABRIC_` stem above was confirmed. **Diversity is a preference, not a filter** — it
  relaxes to a new line, then a new loader, then whatever is left, so a project publishing one loader and one
  Minecraft line samples exactly as deeply as before; `aSingleMinecraftLineStillSpendsTheWholeBudget` pins that
  direction. **Landmine — crossing the loader here is a wider claim than `loaderDisprovingTheCrash` permits**,
  and the difference is the gate: that pass applies to *any* crash, so it insists on a matching entry, while
  this sample is spent only where the crash already contradicts a declared server support, i.e. where one of
  the two signals is known to be wrong. Do not loosen one by pointing at the other. Two consequences worth
  knowing: every attempt's label names its loader (`file.jar (NeoForge, Minecraft 1.21.1)`) because the
  returned outcome may be a boot run under a *different* loader than the verdict is about, and every attempt
  stages into the **crashing** loader's directory (`stageBootPack`'s `attemptDirName`) — staging under the
  candidate's own loader would wipe the pack and console that loader's own verdict is about to be built from.
  **Landmine — the gate is the contradiction, not the crash.** It arms only when
  `ClientsideVerifier.declaresServerSupport` holds, which is the same predicate that prints the "Declared
  server/both but the server crashed" note; keep them sharing it, or the report states a contradiction the
  re-check silently decided did not exist. Where the metadata already leans clientside the crash *confirms*
  it, and in a catalog sweep that agreement is the common case — arming there costs two boots per true
  positive and buys nothing. **CurseForge has no sideness field**, so its claim can only come from the jar
  scan: a gate reading the platform alone never arms for a CurseForge mod, i.e. never for the report that
  prompted this. Conservative in every other direction, like the re-check below: crashes elsewhere
  corroborate, and an attempt that learned nothing leaves the crash standing.
- **A crash cannot outrank another loader's clean boot** (`ClientsideVerifier.reconcileAcrossLoaders`, over the
  pure `loaderDisprovingTheCrash` / `supersededByLoader`). `report()` runs a second pass once every loader is
  in: where one loader CRASHED and another SURVIVED deriving the **same** list-entry, the crash stops counting
  as sideness evidence and the confidence drops to what `aggregate` yields with no boot. **Why the entry and
  not the loader:** the published artefact is a loader-agnostic file-name stem matched with `startsWith`, so
  the crash's entry would strip the surviving build. Where the stems differ nothing is stripped and there is
  no contradiction — a mod really can be client-only on one loader, and this must not become "any survival
  clears any crash". The live case (2026-08-23) had both rows in **one run**, and the disproof was discarded:

  | Loader | Was | Entry | Boot |
  |---|---|---|---|
  | Forge | `HIGH` | `ironchest-` | Forge 48.1.0 / MC 1.20.2 → CRASHED (exit 1) |
  | NeoForge | `LOW` | `ironchest-` | NeoForge 21.11.45 / MC 1.21.11 → SURVIVED (exit 137) |

  **Exit 137 on a SURVIVED row is normal, not a kill to investigate** — `ContainerServerRunner` watches for the
  ready-line and stops the container the moment it appears, so every clean container boot exits 137.
  **The crash is not erased:** `bootResult` and the excerpt stay, because the server did crash and that is
  worth diagnosing; only its *standing* changes. The note is **rebuilt**, not appended to — its old text ended
  in "a strong clientside signal", and bolting a correction onto a false sentence is how prose goes stale.
- **The two crash guards layer, and both cost.** The within-loader other-version re-check runs *during* the
  crashing loader's own boot; cross-loader reconciliation runs after every loader is in. So a crash on a
  project whose other loader survives still pays the two extra boots first — loaders are assessed in sorted
  order (`Fabric, Forge, NeoForge, Quilt`), and nothing looks ahead. Deliberate: the extra boots also produce
  the *within*-loader answer, which is the more specific one.
- **Landmine — every attempt for one candidate writes the *same* `boot.log`.** Staging wipes
  `<work>/boot/<slug>-<loader>` and re-creates it, so the loader-build re-check and each other-version boot
  overwrite the previous console, while the *reported* verdict is usually the first crash. The grinder's
  reaper keeps exactly that one file, so the log a `HIGH` is diagnosed from would be a different boot's.
  `BootOutcome.console` + `restoreDecisiveConsole` (called at the end of `verify`) put the decided outcome's
  own console back. Any new re-check path must leave that call last.
- **`LoaderVersionPolicy` (seam) + crash re-check.** `BootVerifier` takes a *policy*, not the concrete
  `LoaderVersionResolver`: `preferredVersion` is what gets booted, `latestVersion` is the authoritative newest.
  The default resolver answers both identically. A caller may prefer an **older** build it already has installed
  (the grinder's `CachedLoaderVersions` does, to avoid a ~150 MB install per loader release) — which is only safe
  because of the guard: **`latestVersion` alone drives the support gate**, and a CRASHED outcome on a
  non-newest build is re-booted on the newest one before it may stand (`recheckCrashOnNewestVersion`).
  **Why it must exist:** a mod needing a newer loader than the cached build fails to load, exits non-zero, and
  the classifier reads CRASHED → a server-safe mod published as a **HIGH-confidence clientside mod**. The two
  decisions are pure and unit-tested (`shouldRecheckCrash`, `reconcileRecheck`) because `verify` itself needs an
  `ApiWrapper` + real generation + a running server — same split as `outcomeFor`. **Landmine:** an INCONCLUSIVE
  re-check must never clear the crash (a flaky second boot is not evidence); only a clean boot on the newest may.
- **LANDMINE — `"<Loader> is not available for Minecraft X"` in a boot log does NOT mean the loader lacks a
  build.** `default_template.sh:316` raises it when `FABRIC_AVAILABLE != 200`, and that variable is an HTTP status
  from a `curl`/`wget` probe — which cannot succeed in the grinder's `--network none` boot container. The message
  therefore means *"I could not check"*, not *"unsupported"*. Measured 2026-07-30: a `LoaderSupportMemory` that
  treated it as unsupported marked Fabric unusable for **22 Minecraft versions** (1.19.2 through 26.2 — i.e. all
  of them) within minutes, and was reverted. `0.19.3` is a genuine current Fabric *loader* version, so SPC's
  version plumbing is fine; Fabric installer versions (`1.1.2`…) are a separate series and are not what is passed.
  **FIXED 2026-07-30 in the templates (all three shells):** `setupFabric` now settles the launcher from disk
  (`fabric-server-launcher.jar` / `fabric-server-launch.jar`) *before* probing, so an offline pack that already has
  its launcher never asks the network. **Landmine — the fix has two halves, and the first attempt only had one:**
  the disk branch must **fall through** to the `SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION}
  nogui"` assignment at the end of the function. The first cut `return 0`-ed as soon as it found the jar, jumping
  over that assignment, so the pack launched `java -Dlog4j2... do_not_manually_edit` (the untouched placeholder)
  and died with `Could not find or load main class do_not_manually_edit` — *past* every ordering assertion the
  guard test made. `-api`'s `ScriptTemplateContentTest` now **executes** the extracted bash `setupFabric` against a
  staged launcher jar with network calls stubbed to fail, which is the only assertion that catches this; verified
  to fail when the `return 0` is reinstated.
- **LANDMINE — the exit code is NOT a reliable crash signal; the console decides.** Measured 2026-07-30: NeoForge's
  **ServerStarterJar prints a mod-loading crash in full and then exits `0`**. Because `classify` keyed CRASHED on a
  non-zero exit, `modelfix` — whose console holds a textbook `NoClassDefFoundError: net/minecraft/client/Minecraft` —
  came out INCONCLUSIVE, and **no verdict in a 517-verdict store ever reached HIGH**: the expensive boot was running,
  crashing correctly, and being discarded. `clientOnlyClassMarker` now returns **CRASHED from the console alone**,
  ahead of the exit-code logic, and that is what finally produced the engine's first `HIGH(boot:CRASHED)`. It is safe
  to trust over the exit code precisely because no environment failure can fabricate it — but it stays **subordinate
  to the timeout and killed/OOM guards**, so host trouble can never manufacture a HIGH (tests pin both directions).
  A separate fix propagates the server's real status through the start scripts (`SERVER_EXIT_CODE`, all three
  templates), which is correct and useful for users' service wrappers — it just cannot rescue a loader that reports
  success for a crash, so **never make CRASHED depend on the exit code alone again**.
- **Missing dependencies must not reach a boot at all.** `downloadWithDependencies` collects every required dependency
  it could not stage (unresolvable ref, no usable file, failed download — the first of which used to be a *silent*
  `continue`), and `refuseForMissingDependencies` then aborts staging with a named reason instead of booting. A loader
  that rejects a mod for missing dependencies never runs the mod's code, so the run cannot speak to sideness; it just
  produces a failure that looks like a crash. Measured across 112 kept boot logs: **36** failed exactly that way, the
  largest single failure class, each burning ~70 s to learn nothing. `BootLogClassifier` keeps a matching backstop
  (`dependencyFailureMarkers` → INCONCLUSIVE) for deps that go missing despite staging.
- **Quilt dependencies fall back to the Fabric build** (`BootCandidateSelector.fallbackLoaders`). Quilt deliberately
  runs Fabric mods, which is why the canonical dependency of a Quilt mod is **Fabric API — a project publishing only
  Fabric-tagged files**. Strict loader matching dropped it silently: measured 2026-07-30, **210** dropped
  dependencies, all but 44 on Quilt, `P7dR8mSH`/`306612` (Fabric API) the most-dropped ref. The map is deliberately
  one-way and minimal — Fabric cannot load Quilt mods, and NeoForge/Forge cross-loading is version-dependent, so
  guessing there would stage a jar the loader cannot use.
- **`allowModDistribution=false`** CurseForge files arrive with `downloadUrl=null` (`ModFile.locked`);
  routed (`selectDownloader`) to the **Playwright** headless-browser `BrowserDownloader` (lazy; only
  launched for locked files), everything else to `HttpJarDownloader`. Playwright is declared in **this**
  module's build (`com.microsoft.playwright:playwright`), exported `api` so `-app` gets it transitively.
  **LANDMINE — the `/download` navigation is *supposed* to fail.** CurseForge answers it with a file transfer,
  and Chromium aborts a navigation that becomes a download, so Playwright throws `net::ERR_ABORTED`. That throw
  used to escape the `waitForDownload` callback and tear the wait down, discarding a download that had already
  started (observed 2026-08-23 on bwncr-neoforge, tombstone-neoforge, Structory). `isDownloadAbort` swallows
  exactly that and nothing else — a timeout or a DNS failure must still fail, or the downloader returns `null`
  forever in silence. Both navigations also wait for `DOMCONTENTLOADED`, never the default `load`: an ad-laden
  project page keeps fetching long after it is usable, and the whole 30s default budget was being spent on it.
- **`ClientsideListEditor`** (pure, unit-tested) inserts accepted entries into both files that ship the
  fallback-list: the `fallbackMods` `listOf(...)` block in `GenerationConfig.kt` (sorted, aligned
  `//link` comment, Kotlin trailing-comma is fine) and the backslash-continued `fallbackmodslist` in
  `serverpackcreator.properties`. **Landmine — properties continuation:** the *last* entry must NOT end
  in `,\`, or the continuation bleeds into the next property and corrupts it; the editor strips the
  delimiter off the final line and adds one to the previous-last when appending.

## Boot seam (for the grinder — DONE)

`BootVerifier` is split so a container-backed runner can reuse it: **`prepareBootPack()`** does
host-side staging (pick combo → download mod + deps → generate the self-installing pack) and returns a
`Prepared.Ready`; a **`ServerRunner`** runs the pack and returns *raw* `RunResult` lines + exit status
(classification deliberately left to the caller); **`BootVerifier.outcomeFor()`** is the shared verdict
seam (writes the log, then `BootLogClassifier` + `BootLogExcerpt`). The default
`HostProcessServerRunner` spawns `start.sh`; the grinder will supply a `ContainerServerRunner`
(`--network none`, resource-capped) and `BootLogClassifier` is reused verbatim on its streamed logs.
`verify()`'s signature is unchanged (the `serverRunner` ctor param defaults to the host runner).

## Testing patterns

- 130 tests, all offline. Most build jars in-memory (`java.util.jar`) or feed canned
  JSON to a fake `HttpFetcher`; **`MetadataScannerTest` is the only one needing a resource** — it boots
  an offline `ApiWrapper` from `src/test/resources/serverpackcreator.properties` (whose `ModScanner`
  relies on the API's cached version-manifests, hence `test` `dependsOn :serverpackcreator-api:processTestResources`).
- `BootCandidateSelector`, `BootLogClassifier`, `FilenameStemDeriver`, `ClientsideListEditor`, plus the
  extracted `BootVerifier.outcomeFor` (`BootVerifierOutcomeTest`) and `HostProcessServerRunner`'s
  no-start-script contract are pure/offline-testable without a running server — keep new logic that way.
  Both crash re-checks follow the same split: `verify` needs an `ApiWrapper` and a running server, so what is
  pinned is *when* a re-check happens and *how the attempts reconcile*, in `BootVerifierCrashRecheckTest`.

## Roadmap — the grinder (`serverpackcreator-grinder`, planned)

A standalone fire-and-forget Docker service depending on this module + `docker-java` boots candidate
mods **in parallel, isolated containers** to build a catalog-wide list of suspected-clientside mods.
The boot seam above is in place; remaining to build: the `ContainerServerRunner`, the worker
pool/queue, and a per-`(loader, loaderVer, mcVer)` pre-bake cache of the installed loader+libraries so
each actual mod-boot runs offline.

## Boot deadlines are suspend-aware (shared with the grinder)

`SuspendAwareDeadline` (this module) is what both boot paths poll: `HostProcessServerRunner` here and the grinder's
`DockerJavaContainerEngine`. A plain wall-clock deadline expires on a boot the host froze — measured, a laptop
idle-sleeping in ~16-minute cycles produced 19 of 153 verdicts reading `timed out`, several `SURVIVED (timed out)`
whose console showed the server reaching ready seconds after launch. Any gap between polls too large to be mere
slowness is added back to the budget, so a timeout means *"the boot had this long and did not make it"*.

It lives **here, not in the grinder**, because grinder depends on clientside and not the reverse — the detection was
originally written in the container engine's companion, where this module could not reach it. The clock is injected
so the threshold is testable at all: both real callers are integration-shaped and cannot be made to sleep.
**Landmine:** any new poll/park loop that bounds a boot must use it rather than `System.currentTimeMillis()`, or that
path silently reacquires the bug.
