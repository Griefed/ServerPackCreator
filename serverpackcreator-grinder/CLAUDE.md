# serverpackcreator-grinder — module context

> Standalone fire-and-forget service that boot-verifies mods **at scale**, in **isolated, network-less
> Docker containers**, to accumulate a catalog-wide list of suspected-clientside mods. Package
> `de.griefed.serverpackcreator.grinder`. Depends on `serverpackcreator-clientside` (+ `-api`
> transitively) and **docker-java** (`docker-java-core` + `docker-java-transport-zerodep`, 3.7.1). No
> Spring, no Swing. Not published to Maven.

## Package layout

Organized by subsystem; the base package is the composition/orchestration core and the four
subpackages are leaves it wires together (dependencies point **inward**: subpackages never import the
base package's orchestration, only its domain models).

- **`grinder`** (base) — the core: `GrindModels` (`GrindCandidate`, `GrindVerdict`, the
  `CandidateVerifier` seam — the shared vocabulary every subpackage speaks), `Grinder`/`GrindPool`
  (orchestration), `ContainerCandidateVerifier` (the production `CandidateVerifier` that wires the
  subsystems together), and `GrinderApplication` (the `main` entry point / composition root — its
  fully-qualified name is the build's `mainClass`, so it stays here).
- **`grinder.container`** — the isolated-container runtime: `ContainerEngine` (+ `ContainerSpec`,
  `ContainerResources`, `BindMount`, `ContainerRunOutput`), `DockerJavaContainerEngine`,
  `ContainerServerRunner`.
- **`grinder.loader`** — per-tuple loader install + offline pre-bake: `LoaderCache` (+ `LoaderInstaller`
  seam), `DockerLoaderInstaller`, `VanillaPackGenerator`, `InstallLayerSnapshot`, `PackVariables`,
  `ImageJavaRuntimes`. Depends on `grinder.container`.
- **`grinder.report`** — verdict persistence + web/CSV output: `VerdictStore` (+ `InMemoryVerdictStore`),
  `JsonVerdictStore`, `VerdictCsvExporter`, `VerdictReportRenderer`, `ReportServer`.
- **`grinder.source`** — candidate discovery **and the crawl position**: the `CandidateSource` interface
  (`page(offset, limit, partition)` → `CandidatePage`) + `ModrinthCandidateSource` /
  `CurseForgeCandidateSource` (+ `CurseForgePartition` / `CurseForgePartitions`, its partitioned-crawl plan),
  plus `CatalogCrawler` (hands out the next slice per pass) and `CursorStore` / `InMemoryCursorStore` /
  `JsonCursorStore` (`CatalogCursor` = offset + completed sweeps + the source's opaque partition token).

## Current state — the boot seam (container ServerRunner)

The reuse hinge is the clientside module's `ServerRunner` interface. The grinder implements it for
containers:

- **`ContainerServerRunner`** implements `clientside.ServerRunner` — boots a prepared pack in a
  container instead of a host process, so it slots into `BootVerifier` unchanged and feeds the same
  `BootLogClassifier` via `BootVerifier.outcomeFor`. Host-side staging (start-script check, write
  `eula.txt`, assemble the `ContainerSpec`) + mapping the engine's raw output onto `RunResult` live
  here, so it is unit-tested with a fake engine.
- **`ContainerEngine`** is the thin docker boundary (`ContainerSpec` → `ContainerRunOutput`), the same
  injectable-seam pattern as clientside's `HttpFetcher`. **`ContainerSpec` carries the untrusted-mod
  hardening as defaults**: `networkMode=none`, `readonlyRootfs`, `dropAllCapabilities`,
  `noNewPrivileges`, non-root `user`, tmpfs for `/tmp`, plus memory/cpu/pids caps. **Never mount the
  Docker socket into a boot container.**
- **`DockerJavaContainerEngine`** is the real docker-java impl (create → start → follow logs → stop →
  inspect exit → force-remove). **Not unit-tested** (needs a live daemon) — that is the whole reason
  the testable orchestration sits in `ContainerServerRunner` behind the seam. If you change it, verify
  against a real Docker daemon.
- **`LoaderCache`** — the pre-bake cache (the `--network none` enabler). `ensureInstalled(loader,
  loaderVersion, minecraftVersion)` returns a cached installed-server base, running a one-off
  `LoaderInstaller` (with network) only on a **miss**; **marker-gated** (`.spc-installed` written only
  after success, so a crash mid-install is redone, never served half-baked) and **serialized per
  tuple** so parallel workers share a single install. `LoaderInstaller` is the seam — its real impl (a
  setup container *with* network that snapshots the ServerStarterJar's self-install) is
  integration-only; everything else here is pure and unit-tested.
- **Orchestration** (`Grinder`, `GrindPool`, `VerdictStore`, `VerdictCsvExporter`): `Grinder.grind`
  verifies one candidate (skipping already-ground projects, *swallowing* a thrown boot so a bad mod
  can't sink a worker) via the `CandidateVerifier` seam and records one `GrindVerdict` per loader.
  `GrindPool.grindAll` drains a **popularity-ranked** batch across N worker threads (N ≈ host-RAM /
  per-boot-memory — each in-flight grind holds a booting container). `VerdictStore` (in-memory default)
  accumulates, keyed by `slug+loader` (re-verify replaces, not duplicates); `VerdictCsvExporter`
  renders RFC-4180 CSV (`Name, Project, NamePattern, Confidence, Loader, Detail`, highest-confidence
  first). **`CandidateVerifier` is the seam that collapses the integration-bound boot pipeline**, so
  the whole orchestration is unit-tested with fakes.
- **Persistence + web interface**: `JsonVerdictStore` (file-backed, loads on start, whole-file
  temp-then-atomic-move write, corrupt-file → empty) makes a multi-day run restart-safe.
  `VerdictReportRenderer` renders a **self-contained** HTML page — click-to-sort columns, an
  embedded-CSV download button, HTML-escaped cells **and** `\uXXXX`-escaped CSV-in-`<script>` so a
  mod-supplied `</script>` can't break out. `ReportServer` serves the table (`/`) and CSV
  (`/export.csv`) live off the store via the **JDK's built-in `com.sun.net.httpserver.HttpServer`** —
  **no Spring, no new dependency**. *Deliberately standalone:* the report is self-contained rather than
  rendered through the app's Quasar frontend, because the grinder must not depend on `-app` (that would
  drag in Spring/Mongo/Swing and break its standalone nature).
- **Candidate sources** (`CandidateSource` — `platform` + `page(offset, limit, partition): CandidatePage`,
  most-downloaded first): `ModrinthCandidateSource` (keyless Modrinth search, one offset sequence, ignores
  `partition`) and `CurseForgeCandidateSource` (CF `/mods/search` sorted by `sortField=6` TotalDownloads,
  `x-api-key`, `index`/`pageSize≤50`, `index+pageSize≤10000`; project link = `links.websiteUrl`; **crawled in
  partitions**, see below). Both paginate behind the clientside `HttpFetcher` seam (unit-tested with canned
  JSON). `GrinderApplication` wires Modrinth always and CurseForge **only when `CURSEFORGE_API_KEY` is set**;
  `GrindPool` re-sorts the union by popularity so the platforms interleave. Store dedup is by `slug`, so a mod
  on both platforms is treated as one project (accepted for now).
- **CurseForge partitioned crawl** (`CurseForgePartitions`, pure + unit-tested — the *only* place that decides
  what CF gets crawled): one query can never expose more than 10 000 mods (`index+pageSize` cap), so a sweep
  walks a **sequence** of bounded queries: (1) the unfiltered catalog, (2) each game version newest-first from
  `/games/{gameId}/versions`, (3) a version whose `pagination.totalCount` exceeds the cap re-crawled per
  modloader — *this* is what reaches past 10 000 — (4) a loader slice still over the cap also crawled
  `sortOrder=asc` (bottom 10 000), so ≤20 000 per slice is fully covered. Splitting only where a count demands
  it keeps a sweep at ~1 request per version, not per version×loader; `totalCount` rides along on every
  response, so sizing is free (the lone probe case is a slice resuming exactly at the cap). All six documented
  loaders are crawled incl. legacy Cauldron/LiteLoader — one request each beats making their mods unreachable.
  **Version list refreshes at sweep start** (`partition == null`), so versions released mid-run get picked up;
  a failed fetch degrades to the unfiltered top 10 000 rather than crawling nothing.
  **Residual gaps (logged with counts, not hidden):** a single (version, loader) slice >20 000 loses its middle,
  and a mod with no loader tag is only reachable while its version fits under the cap → the remedy is a third
  axis (`categoryId`). **Landmine:** `warnIfMisordered` must follow the partition's direction — an ascending
  slice is *supposed* to come back least-downloaded first, so the old descending-only check would have cried
  wolf on every bottom-up slice.
  **`CandidatePage.endOfCatalog` is load-bearing, don't collapse it:** it is `true` only when the platform
  genuinely ran out — for a partitioned source, when the *last* partition ran out — never on a failed request
  or a failed size probe. The crawler wraps to the start of the plan on it, so treating a transient 503 as
  "the end" would silently reset a deep crawl to the popular head.
  **The partition token is opaque outside its source.** `CatalogCursor.partition` / `CandidatePage.nextPartition`
  are carried and persisted verbatim by the crawler; only `CurseForgeCandidateSource` parses them
  (`CurseForgePartition.parse`, which falls back to the start of the sweep for anything unreadable, so an old
  or hand-edited `cursors.json` can't crash the daemon). Don't teach the crawler what a partition means.
- **Catalog crawl (the coverage mechanism)**: `CatalogCrawler.nextBatch()` resumes each source at its
  persisted `CatalogCursor`, advances it by what was actually handed out, and on `endOfCatalog` wraps to the
  top and counts a sweep (the verdict TTL then decides what the new sweep re-grinds). A failed page keeps its
  position (retried next pass); a *throwing* source is skipped, not fatal. A position already past the end
  wraps **and** takes the head slice in the same pass — guarded by `cursor.offset > 0 || partition != null` so
  an empty catalog can't spin. `JsonCursorStore` persists offset+sweeps+partition per platform
  (temp-then-atomic-move, corrupt → start of catalog) — **this file is the difference between eventual full coverage and re-checking the top N
  forever**; deleting it costs one re-sweep (fresh verdicts are skipped), not correctness.
- **Pacing** (`GrindPacing.pauseAfterPass`, pure + unit-tested): work found → no pause; nothing due but
  catalog remains → short `SPC_GRINDER_SCAN_DELAY`; sweep completed with nothing due → `SPC_GRINDER_INTERVAL`.
  **A fixed per-pass sleep is what made coverage impossible** (25 projects/6 h vs. ~71 000 Modrinth mods).
  **Failed verifications deliberately don't count as work** — with a broken host every candidate fails, and
  counting that as progress would race the cursor through the catalog leaving thousands unverified.
- **Loader install (the `LoaderCache` `LoaderInstaller`)**: `DockerLoaderInstaller` generates a
  **mod-less** pack (`VanillaPackGenerator` → `ApiVanillaPackGenerator` over `ApiWrapper`), boots it
  **once with network** (`networkMode="bridge"` — the *only* networked boot) so `start.sh` installs the
  loader + MC server + libraries, then snapshots the install layer into the cache. The error-prone
  pieces are pure + unit-tested: **`InstallLayerSnapshot`** (denylist diff/copy — snapshots added
  non-runtime files, the spike-derived design), **`PackVariables`** (the unattended-boot levers:
  eula + `WAIT_FOR_USER_INPUT=false` + a resolved `JAVA` path + `SERVERSTARTERJAR_FORCE_FETCH=false`
  *only* for offline boots), and **`ImageJavaRuntimes`** (MC→bundled-JDK + the supported-Java gate,
  required-Java sourced authoritatively from `MinecraftMeta.requiredJavaVersion`). `DockerLoaderInstaller` /
  `ApiVanillaPackGenerator` themselves are integration-only (daemon + image + real `ApiWrapper`).
  **Operational note:** the bind-mounted pack must be writable by the container's uid 1000 (the install
  writes `libraries/` etc. into it) — align uids or `--user root` (Docker Desktop maps automatically).

**Landmine — network vs. install:** the hardening default is `--network none`, but the *first* boot of
a given loader/MC needs network for the ServerStarterJar to download the loader + libraries. The plan
is to **pre-bake that once per `(loader, loaderVersion, minecraftVersion)`** into a cached, read-only
base tree (network only on the cache-miss), then every actual mod-boot mounts it and runs offline.
Don't wire the candidate-mod boot to run with network — that defeats the isolation.

**Loader / Java facts (durable — drive the runtime image):** SPC's generated `start.sh` is
*self-contained* — it installs the modloader + Minecraft server itself at first boot, so the image is
**loader-agnostic** (no per-loader logic). The **`ServerStarterJar` (neoforged) is Forge/NeoForge
only**; Fabric uses `fabric-installer`/`fabric-server-launch(er).jar`, Quilt the `quilt-installer`,
LegacyFabric its own installer. A single JDK can't boot every Minecraft version (8 for the oldest,
through 21 for 1.20.5+ and **25** for the current release 26.2), so the image bundles Temurin 8/17/21/25
and the grinder sets `$JAVA` per MC version (via the pack's `variables.txt`) from SPC's declared
required-Java — **no Java download**, which is what keeps mod-boots runnable under `--network none`.
The template needs `bash`, `curl`/`wget`, `gawk`, `tar`/`gzip`. See
`docker/README.md`.

**Host prerequisites (apply once `ContainerServerRunner` is wired into a `BootVerifier`):** the
download/resolve phase runs on the **host** (in `BootVerifier.prepareBootPack`), *not* in the boot
container, so the box running the grinder needs:
- **`CURSEFORGE_API_KEY`** env var — `clientside.supportedPlatforms()` only registers CurseForge when
  the key is present; without it CurseForge links cannot be resolved at all (Modrinth needs no key).
- **Playwright + Chromium installed** (`playwright install chromium` + OS deps, as `clientside-boot.yml`
  does) — distribution-locked CurseForge files (`allowModDistribution=false`, `downloadUrl=null`) are
  routed by `clientside.selectDownloader` to the headless-browser `BrowserDownloader`, which runs on
  the host during staging. The key and the browser are **complementary**: the key resolves the project
  and reveals the file is locked; the browser fetches the withheld jar. A locked CurseForge mod needs
  **both**. Wire the `BootVerifier` with a `BrowserDownloader()` (disposed via `use {}`) exactly as
  `VerifyClientsideCommand` does — locked-file support is then inherited, not reimplemented.

## Loader-install spike findings (2026-06-27 — drive the `LoaderCache` + cache-overlay)

Generated + booted real packs (Forge 1.20.6 & 1.12.2, NeoForge 1.21, Fabric 1.20.6, Quilt 1.20.6) and
diffed each booted dir against its pre-boot baseline. Conclusions:

- **The install layer is mod-independent but loader-specific.** Per loader, the boot adds (besides the
  always-present **`libraries/`**, the dominant cost — ~40 MB Fabric/Quilt/old-Forge to ~180 MB
  NeoForge/Forge-1.20):
  - **SSJ (Forge ≥1.17, NeoForge):** `server.jar` + `<loader>-<ver>-installer.jar`(+`.log`) +
    `*-shim.jar` (Forge) + `run.sh` + `run.bat` + `user_jvm_args.txt`.
  - **Old Forge (<1.17, e.g. 1.12.2):** `forge.jar` + `minecraft_server.<mc>.jar` +
    `forge-installer.jar.log` — **no** `server.jar`/run-scripts (the SSJ path is not taken).
  - **Fabric:** `fabric-server-launcher.jar` + `.fabric/` + `versions/`.
  - **Quilt:** `quilt-server-launch.jar` + `quilt-server-launcher.properties` + `server.jar` (vanilla MC)
    + `.cache/` + `versions/`.
- **The `CLEANUP` variable is an *incomplete* snapshot manifest — do not use it as the include-list.**
  It lists `libraries, run.sh, run.bat, *installer.jar(.log), server.jar, fabric-server-launch(er).jar,
  …` but misses `forge.jar`, `minecraft_server.*.jar`, `*-shim.jar`, `quilt-server-launch.jar`,
  `versions/`, `.fabric/`, `.cache/`, `user_jvm_args.txt`.
- **Snapshot strategy = DENYLIST, not includelist.** `LoaderInstaller` boots a *vanilla* (empty-mods)
  pack once per tuple **with** network, then snapshots `(post-boot files) − (pre-boot pack files) −
  runtime-state`. The **runtime-state denylist** (created at boot, never cached): `world*/`, `logs/`,
  `crash-reports/`, `ops.json`, `whitelist.json`, `banned-ips.json`, `banned-players.json`,
  `usercache.json`, `eula.txt`, `.previousrun`, `hs_err_pid*.log`, `README.txt`, `.DS_Store`.
- **Offline-boot levers (set in `variables.txt` before every cached `--network none` boot; confirmed in
  `default_template.sh`):** `WAIT_FOR_USER_INPUT=false` (else it blocks on a `read`),
  `SERVERSTARTERJAR_FORCE_FETCH=false` (else Forge/NeoForge *re-download* `server.jar` → needs network),
  and pre-write `eula.txt` = `eula=true` (else an interactive EULA prompt). Also set `JAVA` to the
  bundled per-MC JDK (`/opt/java-{8,17,21,25}`).
- **Cache-overlay seam — RESOLVED (no deep `BootVerifier` change needed).** The install layer never
  name-collides with pack files (`libraries/`, `server.jar`, run-scripts vs. `start.sh`/`mods/`/`config/`),
  so the overlay is a plain recursive copy. Plan: add an optional `packPostProcessor:
  ((Prepared.Ready) -> Unit)? = null` hook to `BootVerifier.verify`, invoked **after** `prepareBootPack`
  and **before** `serverRunner.run` (it receives `loader`/`loaderVersion`/`minecraftVersion`). The
  grinder's post-processor does `loaderCache.ensureInstalled(tuple)` → copy the install layer in → set
  the offline levers + write `eula.txt`. Default `null` keeps the host runner's behavior unchanged.

Spike workspace (not committed): `~/spc-grinder-spike/{configs,packs,baselines}` + the
`serverpackcreator-app-dev.jar` generation command. Reusable by the `LoaderInstaller` work.

## Testing

- `ContainerServerRunnerTest` uses a fake `ContainerEngine`: no-start.sh → `NotStarted` (engine never
  called), raw output → `RunResult.Completed`, and the assembled spec carries the hardening + pack
  mount + written eula. All offline.
- `LoaderCacheTest` uses a fake `LoaderInstaller`: miss-installs-once-then-hits, failed/throwing
  install → `null` + nothing left installed, concurrent requests for one tuple install once, distinct
  tuples cached independently. All offline.
- `VerdictStoreTest`, `VerdictCsvExporterTest`, `GrinderTest` cover the orchestration with a fake
  `CandidateVerifier` (shared builders in `GrindTestFixtures.kt`): replace-not-duplicate, CSV
  escaping + confidence ordering + name-pattern column, per-loader recording, skip-already-done,
  swallow-throw, pool drains every candidate + most-popular-first. All offline.
- `JsonVerdictStoreTest` (survive-reopen, replace-across-reopen, corrupt→empty, creates-file+parents),
  `VerdictReportRendererTest` (sortable headers, embedded CSV, HTML/script escaping), `ReportServerTest`
  (real **loopback** HTTP on an ephemeral port: `/` HTML + `/export.csv`, live store, content-types).
- `ModrinthCandidateSourceTest` / `CurseForgeCandidateSourceTest` (canned search JSON via a fake
  `HttpFetcher`): download-order preserved, one slice spanning several API pages, the slice starting at the
  requested offset, short/empty page ⇒ `endOfCatalog`, **failed page ⇒ NOT `endOfCatalog`**, limit-0 fetches
  nothing; CF additionally: the documented request contract and the `websiteUrl` fallback (its partition
  behaviour is listed below).
- `CatalogCrawlerTest` (fake catalog of N synthetic projects): consecutive batches walk forward, wrap +
  sweep-count at the end, resume from a persisted position, failed request keeps its position and is retried,
  sources crawled independently and unioned, past-the-end wrap fetches the head in the same pass, empty
  catalog requested only once, throwing source skipped, **partition token replayed verbatim** and cleared on a
  completed sweep. `CursorStoreTest`: unknown source ⇒ start, per-source positions, JSON survives reopen,
  replaces rather than appends, creates file+parents, corrupt ⇒ start, partition token round-trips, and a
  pre-partition `cursors.json` (no `partition` key) still loads.
- `CurseForgePartitionTest` (pure, no HTTP — **the CF coverage spec**): sweep opens unfiltered, hands over to
  the newest version, under-cap version skips the loader split, over-cap version splits by loader, all six
  loaders walked in order, over-cap loader slice gets its ascending twin, ascending always moves on, last
  loader/version ends the catalog, empty version list ends after the unfiltered slice, a version that vanished
  falls forward, key round-trip + unreadable key ⇒ start of sweep, version ordering numeric-descending with
  unparsable last. Partitioned-source behaviour in `CurseForgeCandidateSourceTest`: which filters each slice
  queries, crossing a partition boundary mid-slice (offset resets relative to the new partition), version list
  refreshed at sweep start only, cap → loader split via `totalCount` (incl. the single-item probe), failed
  probe keeps the position, and an unavailable version list degrading to the unfiltered top 10 000.
  `GrindPacingTest`: the three pause cases + work outranking a completed sweep.
- **`CatalogCrawlLiveIT`** (gated `GRINDER_LIVE_IT=1`, no containers, a few search calls) pins the *platform*
  assumptions no fake can: consecutive live batches return **different** projects, a fresh crawler over the
  same cursor file resumes rather than re-serving the head, and offset 40 000 still serves real projects.
  **Verified passing 2026-07-29.** Run it after touching paging or the cursor.
- `InstallLayerSnapshotTest` (added-non-runtime files copied, pre-boot + runtime excluded),
  `PackVariablesTest` (in-place key replace not touching `JAVA_ARGS`, append-if-absent, offline
  force-fetch toggle, eula), `ImageJavaRuntimesTest` (the bundled-JDK resolution + supported-Java gate:
  a version whose required Java isn't bundled is unsupported, not booted on the wrong JDK). The
  `LoaderInstaller`/`VanillaPackGenerator` impls are integration-only.
- docker-java and the real installer have no offline doubles; `DockerJavaContainerEngine` and the
  production `LoaderInstaller` are integration-only.
- **`DockerJavaContainerEngineIT`** is the live-daemon integration test for the docker glue, **gated
  behind `GRINDER_DOCKER_IT=1`** (`@EnabledIfEnvironmentVariable`) so it is skipped on a normal /
  daemon-less CI run. Run it with a daemon + the `busybox:latest` image present:
  `docker pull busybox && GRINDER_DOCKER_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*DockerJavaContainerEngineIT"`.
  It verifies the full path (create → start → stream → ready-detect/stop → exit code → **force-remove**,
  no leaked containers) under the production hardening defaults. **Verified passing** against Docker
  29.5 on 2026-06-26.

## End-to-end verification & the Java limitation

**Full-loop verification on current Minecraft (2026-07-28).** `GrinderApplication` grinding
`modrinth.com/mod/modmenu` against the **JDK-25 image** produced two verdicts on **MC 26.2** and proved
the complete chain on current Minecraft:
- **Quilt / 26.2 → SURVIVED (MEDIUM): a genuine full success.** The cached loader install ran (network,
  102 jars snapshotted), then the mod-boot ran **offline** (`--network none`, confirmed by
  `UnknownHostException` for Mojang hosts) on `/opt/java-25` (`Compatibility level set to JAVA_25`) and
  reached **`Done (5.744s)! For help`** — MC 26.2 server fully started → correctly SURVIVED (a clean
  boot proves nothing for a clientside mod, hence MEDIUM). This validates container install → offline
  boot → classify → verdict → store on current MC with the new image.
- **Fabric / 26.2 → was a false HIGH, now fixed.** Fabric has no build for 26.2 yet, so start.sh
  aborted "Fabric is not available for Minecraft 26.2" *before loading the mod*; the classifier scored
  the exit-1 as CRASHED → HIGH. Fixed in `-clientside`: `BootLogClassifier.setupAbortMarkers` maps all
  pre-launch `crashServer` failures (loader-unavailable, install/download failure, Java/EULA/variables
  setup) to **INCONCLUSIVE** — the mod was never tested. See `serverpackcreator-clientside/CLAUDE.md`.

**Earlier run (2026-06-28)** verified the visible half (resolve → download → generate → verdict →
`JsonVerdictStore` → CSV → `ReportServer`) on live data and the hardened install on a Java-21 Minecraft
(1.20.6 → 38 library files), and **found + fixed** the boot-pack `inclusions` bug (boot had never
actually worked) plus the release-only MC gate and install diagnostics.

**Java/image bound (RESOLVED 2026-06-28).** The grinder picks the *newest* Minecraft release; in this
environment that is **26.2**, which requires **Java 25** (`java-runtime-epsilon`). Originally the image
bundled only 8/17/21, so `start.sh` aborted at a Jabba Java-install prompt. The fix is
**`ImageJavaRuntimes`**: it sources the required Java major **authoritatively** from
`MinecraftMeta.requiredJavaVersion(mc)` (Mojang's declared `javaVersion.majorVersion`, scheme-proof — no
hand-rolled heuristic) and exposes (a) `supports(mc)` — required-Java known *and* in `bundledMajors`
(default **8/17/21/25**, **must mirror the Dockerfile**), and (b) `javaPath(mc)` → the bundled JDK path
or null. `BootVerifier` now takes an injected `minecraftAcceptable` predicate (default accept-all for the
host CLI; `ContainerCandidateVerifier` passes `imageJava::supports`), AND-ed into candidate selection, so
a version whose JDK the image lacks is **never selected** — never booted on the wrong JDK and **never
mis-scored as a clientside crash (false HIGH)**. This is deliberately *not* `SKIP_JAVA_CHECK`.
**Image now ships Temurin 25** (verified: `25.0.3` LTS, image ~2.08 GB), so the current release 26.2 boots.
Java-**26** is intentionally *not* bundled: it only appears on snapshots (e.g. 26.3-snapshot), which the
release-gate already skips. **To extend coverage** to a future release: add its JDK to the Dockerfile
*and* to `ImageJavaRuntimes.bundledMajors` — the two are the single coupled source of truth.

## Status & what remains

**The core loop is built and e2e-verified** (see the verification section above). The full chain —
candidate source → `Grinder`/`GrindPool` → `ContainerCandidateVerifier` (`ClientsideVerifier` +
container `BootVerifier` + `packPostProcessor` doing `loaderCache.ensureInstalled` → install-layer
overlay → offline boot) → `JsonVerdictStore` → `ReportServer`/CSV — runs end-to-end via
`GrinderApplication`, seeded by `ModrinthCandidateSource`.

**Continuous operation — DONE.** With no project-URL args, `GrinderApplication` loops fire-and-forget:
each pass takes the **next slice** of every platform's catalog from the `CatalogCrawler` and grinds it;
`Grinder` skips a project whose verdict is still *fresh* (younger than `reverifyTtl`, via
`VerdictStore.newestVerification`) and re-verifies stale ones. Verdicts *and* the crawl position persist
after every step, so a restart resumes mid-catalog. A JVM shutdown hook stops the loop. Passing explicit
project URLs keeps the **one-shot** path (verification).
Config (env): `SPC_GRINDER_BATCH` (projects per platform per pass, default 25 — **the sweep-speed lever**),
`SPC_GRINDER_CURSORS` (crawl-position file), `SPC_GRINDER_INTERVAL` (idle after a completed sweep found
nothing due, default 21600 = 6h), `SPC_GRINDER_SCAN_DELAY` (pause while only scanning past fresh verdicts,
default 15s), `SPC_GRINDER_REVERIFY_TTL_DAYS` (verdict staleness, default 30).

**Queue cursor — DONE (2026-07-29).** Previously every pass re-fetched *the same* top-N (both sources
restarted at offset 0), so the grinder verified ~50 projects forever and rank N+1 was unreachable. Now the
crawl position is persisted per platform and advances each pass, so an unattended grinder works through a
catalog and then keeps it current. **Sizing matters more than it looks:** a sweep is
`catalog ÷ batch × pass-duration`, so the default 25/pass over ~71 000 Modrinth mods is ~2 850 passes —
raise `SPC_GRINDER_BATCH`/`SPC_GRINDER_WORKERS` and keep `SPC_GRINDER_REVERIFY_TTL_DAYS` **longer than a
sweep takes**, or verdicts go stale faster than the crawl advances and the tail is never reached.

**Loader-availability at selection — DONE.** `LoaderVersionResolver.latest` now returns `null` for a
Minecraft a loader doesn't support (Fabric/Quilt/LegacyFabric gated on `Meta.isMinecraftSupported`;
Forge/NeoForge already MC-specific), so an unsupported combo is dropped from selection instead of spun
up and aborted. The classifier's setup-abort INCONCLUSIVE mapping remains the backstop.

**CurseForge candidate source — DONE.** `CurseForgeCandidateSource` enumerates CF most-downloaded-first
behind the `CandidateSource` interface; wired when `CURSEFORGE_API_KEY` is set (see the candidate-sources
bullet above).

**Script-template matrix — DONE.** `ScriptTemplateMatrixIT` (gated `GRINDER_TEMPLATE_IT=1`) boots the
generated `start.{sh,fish,ps1}` across `{MC} × {loader} × {bash,fish,pwsh}` cells in the
`spc-grinder-templates` image (base + fish + pwsh, `docker/Dockerfile.templates`), asserting each valid
cell reaches the ready-line; invalid loader/MC combos are skipped via the step-3 `LoaderVersionResolver`
gate. Cells generate a pack (default sh/fish/ps1 templates forced on), point `$JAVA` at the bundled JDK,
and boot with `networkMode=bridge` (the template does its own install), through a bounded executor with
one `@TestFactory` `DynamicTest` per cell. Matrix dims are env-overridable for a focused subset. **On its
first real run it earned its keep:** Fabric/1.20.1 bash passed but fish failed with "Could not find or
load main class" — `default_template.fish`'s `runJavaCommand` split the command on spaces *keeping* empty
tokens (fish, unlike bash, doesn't drop them), so an empty `$JAVA_ARGS` injected a stray `""` arg. Fixed
in the api template with `string split --no-empty`; re-run → bash and fish both reach the ready-line.
**Non-gated guard:** because this IT never runs in CI, `-api`'s `ScriptTemplateContentTest` pins the
`--no-empty` construct at source level (and runs `fish -n` on both fish templates when a `fish` binary is
present, skipping otherwise) — verified to fail if the bug is reintroduced.
**Verified NOT a bug — `cleanServerFiles`' comma split** (`default_template.fish:204`, no `--no-empty`):
bash's `IFS="," read -ra` keeps empty fields too, and both shells hand the empty token to
`find -maxdepth 1 -name ""`, which matches nothing and exits 0. Checked empirically in the container
(`CLEANUP="deleteme.jar,,*.absent"` → both shells produced 3 fields, deleted exactly `deleteme.jar`, left
the keeper). Left as-is deliberately: adding `--no-empty` there would be churn and a needless divergence
from the bash reference. Don't "fix" it again.

**Landmine — `.ps1` CANNOT be boot-tested on Linux.** The PowerShell template shells out to Windows
**`CMD /C`** in three places (`default_template.ps1`: Java-version detection ~line 142, the server launch
~228, the bit check ~696). In a Linux container that fails with `The term 'CMD' is not recognized`, the
Java version then reads as `do_not_manually_edit`, and the run aborts at the Jabba prompt — a platform
mismatch, **not** a template defect. (A `pwsh` boot also needs `HOME` on a writable mount, since it
creates `$HOME/.cache` and the rootfs is read-only — exit 133 before it even parses the script.) So
PowerShell is covered by **`powerShellTemplatesParse`**, which runs PowerShell's *own* parser
(`Parser::ParseFile`) over both shipped `.ps1` files inside the image — catching the syntax-class
regressions these tests exist for. Don't "fix" the matrix by adding a `pwsh` boot cell; `scriptFor`
rejects it with the reason.

**Full matrix (2026-07-29) — 5 Minecraft versions × 5 loaders × {bash, fish}, plus the `.ps1` parse check.
bash ≡ fish in every single cell, and every runnable cell is green:**

| Loader | 1.12.2 | 1.16.1 | 1.20.1 | 1.21.1 | 1.21.11 |
|---|---|---|---|---|---|
| Forge | ✅ ✅ | ✅ ✅ | ✅ ✅ | ✅ ✅ | ✅ ✅ |
| NeoForge | N/A | N/A | ✅ ✅ | ✅ ✅ | ✅ ✅ |
| Fabric | N/A | ✅ ✅ | ✅ ✅ | ✅ ✅ | ✅ ✅ |
| Quilt | N/A | ✅ ✅ *(after the JAVA_INSTALLER fix)* | ✅ ✅ | ✅ ✅ | ✅ ✅ |
| LegacyFabric | ✅ ✅ | N/A | N/A | N/A | N/A |

(`✅ ✅` = bash, fish. N/A = the loader genuinely has no build for that Minecraft — `LoaderVersionResolver`'s
support gate filtering correctly, incl. LegacyFabric's pre-1.14 era and Fabric/Quilt's need for an
intermediary. `.ps1` parse ✅.)

**Fixed — Quilt could not install on old Minecraft (found here, in all shells).** `Quilt Installer requires
Java 17 or greater to run.` → `quilt-server-launch.jar not found`, because the templates ran *every*
installer with `$JAVA`, which for 1.16.1 is Java 8 (Mojang's declared requirement) — one JDK cannot satisfy
both installer and server. All three templates now run modloader installers through
`runInstallerJavaCommand` / `RunInstallerJavaCommand`, which uses the **optional** `JAVA_INSTALLER` from
`variables.txt` and falls back to `JAVA` when unset (so existing packs are unaffected, and no new
placeholder plumbing was needed — the templates already parse `variables.txt`).

The grinder supplies it through **`ImageJavaRuntimes.installerJavaPathFor(minecraftVersion)`**, which
deliberately returns the override *only when the server's own Java is older than* `MINIMUM_INSTALLER_JAVA`
(17). A modern Minecraft therefore gets **no** `JAVA_INSTALLER` — identical to a hand-made pack — which is
the point: the plain-`JAVA` fallback is the branch every real pack takes, so it must stay the *exercised*
one. Consequence in the matrix: Quilt on 1.16.1 covers the override, Quilt on 1.20.1+ covers the fallback.
Do **not** "simplify" this back to an unconditional `installerJavaPath()` — that leaves the fallback
untested while every user runs it. **Landmine:** each boot path writes its own variables (installer,
verifier overlay *and* the matrix IT), so a new boot path must pass `installerJavaPath` too, or
Quilt-on-old-MC silently breaks again.
**`.ps1` selection is executed, not just parsed:** `powerShellInstallerJavaSelectionHonoursTheOverrideAndIts
Fallback` pulls `RunInstallerJavaCommand` out of the shipped template via the PowerShell AST, stubs `CMD`
and runs both branches on Linux pwsh — the only way to execute template code whose real path needs Windows.

**Landmine — do not raise `SPC_GRINDER_TEMPLATE_WORKERS`.** It defaults to **1**. Each cell boots a real
Minecraft server capped at 3 GB, so parallel cells starve the host: at 3 workers this run produced **8
spurious failures** (`start.sh: line 144: Killed "$JAVA"` — SIGKILL mid "Preparing level"), *all* of which
passed when re-run serially. Judge no cell from a parallel run.

**Continuous mode — verified live (2026-07-29).** With a pre-seeded *fresh* verdict and `interval=40s`:
two passes ran 40s apart, both skipping the fresh project (no boots), the report server answered
`HTTP 200` + CSV throughout, and `SIGTERM` fired the shutdown hook mid-sleep ("Grinder stopped after 2
pass(es)"). With `SPC_GRINDER_REVERIFY_TTL_DAYS=0` the same verdict became stale and re-verification
**actually ran** (resolve → mod scan → boot-pack *and* install-pack generation for 26.2/Fabric), proving
both sides of the TTL boundary outside the unit tests.

**Catalog crawl — verified live (2026-07-29).** Ran the installed daemon against live Modrinth with
`SPC_GRINDER_BATCH=5`, `SPC_GRINDER_SCAN_DELAY=3` and a verdict store pre-seeded *fresh* for the top 10
projects: passes #1 and #2 each took 5 candidates, verified **0** (all fresh) and paused exactly 3s — the
scan-ahead pacing — while `cursors.json` advanced 5 → 10 → 15 and pass #3 reached the first *unseeded*
projects and began real verification. That is the whole claim in one run: the position moves forward, is
persisted, and unverified projects deeper in the catalog do get reached.

**Shutdown — `SIGTERM` mid-pass used to kill the JVM with a bare `Exception in thread "main"`** (found
2026-07-29 while verifying the crawl loop): the hook interrupts the main thread, which is normally parked in
`GrindPool.grindAll`'s `Thread.join()`, and the `InterruptedException` escaped `main`. `grindAll` now catches
it, `requestStop()`s and restores the interrupt flag, returning the count so far — pinned by
`anInterruptedPassStopsInsteadOfThrowing`. (The JVM often halts before `main` can log "Grinder stopped": once
the hooks finish it exits, so a missing final line on `SIGTERM` is normal, not a hang.)

**Shutdown drain — DONE.** `DockerJavaContainerEngine` tracks the containers it owns and is `AutoCloseable`;
`close()` force-removes whatever is still in flight, because `run`'s per-run `finally` never executes when
the JVM is torn down mid-boot (a `SIGTERM` used to leave a Minecraft server running — observed once, removed
by hand). `GrinderApplication` registers that cleanup as a shutdown hook immediately after building the
engine, so it covers the one-shot path too, and `GrindPool.requestStop()` makes workers abandon the queue
after their current candidate instead of draining a whole batch. Verified against a live daemon by
`closeRemovesAContainerLeftRunningByAnAbandonedRun`.

Remaining:

1. **`CurseForgeCandidateSource` has never made a real API call** (no `CURSEFORGE_API_KEY` available). Its
   contract is docs-verified and defended by `warnIfNotDescending`, which is not the same as observed.
2. **Store dedup is slug+platform, not project-identity** — good enough today; a mod that changes slug on a
   platform would be re-ground as a new project.
3. **CurseForge partitioning is implemented but never observed live** (same root cause as 1: no key). The
   traversal is pinned by pure unit tests and canned JSON against the published contract; what cannot be
   checked offline is whether CF's `gameVersion` vocabulary, `totalCount` and loader filters behave as
   documented. First run with a key: confirm the "crawl covers N game version(s)" log line, and watch for
   `not sorted by` / `holds N mods but only` warnings.
4. **Residual CF gaps by design** — a (version, loader) slice >20 000 mods loses its middle; a mod with no
   loader tag is unreachable beyond its version's cap. Both are logged with counts. Fix is a third axis
   (`categoryId`), deferred until a real run shows it matters.
5. **Modrinth's offset ceiling is 99 999** (measured) vs. ~71 000 mod projects today, so the whole catalog is
   reachable — but **if it ever exceeds 100 000 the tail silently looks like the end of the catalog** and the
   crawl would wrap early.
