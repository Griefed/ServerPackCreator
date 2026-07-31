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

- **`MetadataScanner`** mirrors `ModListCompiler`'s loader→scanner dispatch (kept in sync deliberately;
  it is *not* shared code — if you change one, check the other).
- **Platform layer**: `ModPlatform` (Modrinth/CurseForge) over an injectable `HttpFetcher` so tests use
  canned JSON (no live network). **CurseForge has no sideness field** → `Sideness.UNKNOWN`; only
  Modrinth declares `client_side`/`server_side`. Use `JsonNode.textOrNull` for nullable URL fields —
  `asText(null)` returns the literal `"null"` for a JSON-null and would defeat `ModFile.locked`.
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

- 41 tests across 12 files, all offline. Most build jars in-memory (`java.util.jar`) or feed canned
  JSON to a fake `HttpFetcher`; **`MetadataScannerTest` is the only one needing a resource** — it boots
  an offline `ApiWrapper` from `src/test/resources/serverpackcreator.properties` (whose `ModScanner`
  relies on the API's cached version-manifests, hence `test` `dependsOn :serverpackcreator-api:processTestResources`).
- `BootCandidateSelector`, `BootLogClassifier`, `FilenameStemDeriver`, `ClientsideListEditor`, plus the
  extracted `BootVerifier.outcomeFor` (`BootVerifierOutcomeTest`) and `HostProcessServerRunner`'s
  no-start-script contract are pure/offline-testable without a running server — keep new logic that way.

## Roadmap — the grinder (`serverpackcreator-grinder`, planned)

A standalone fire-and-forget Docker service depending on this module + `docker-java` boots candidate
mods **in parallel, isolated containers** to build a catalog-wide list of suspected-clientside mods.
The boot seam above is in place; remaining to build: the `ContainerServerRunner`, the worker
pool/queue, and a per-`(loader, loaderVer, mcVer)` pre-bake cache of the installed loader+libraries so
each actual mod-boot runs offline.
