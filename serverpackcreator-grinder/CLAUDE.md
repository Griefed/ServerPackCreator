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
  `/games/{gameId}/versions`, (3) a version whose `pagination.totalCount` exceeds the cap re-crawled **per
  modloader and then per category** (`/categories?classId=6`) — *this* is what reaches past 10 000 — (4) any
  slice still over the cap also crawled `sortOrder=asc` (bottom 10 000 ⇒ ≤20 000 covered), and a *category*
  slice past even that narrowed by modloader (version × category × loader, the deepest the API allows).
  **Both axes on purpose, not redundancy:** a mod carries a loader tag only if it has one, and CF's own docs
  disagree on whether a category is mandatory (submission guide says the main category is required; the
  project-creation page lists only the class) — so neither axis is provably total, and running both means a mod
  is reachable if it has *either*. ~6 extra requests per over-cap version buys removal of a silent hole.
  Splitting only where a count demands it keeps a sweep at ~1 request per version; `totalCount` rides along on
  every response, so sizing is free (the lone probe case is a slice resuming exactly at the cap). All six
  documented loaders are crawled incl. legacy Cauldron/LiteLoader, and **every** category incl. children —
  measured live: a parent category does **not** reliably include its children (3 of 6 sampled child-category
  mods were invisible under their parent), so crawling parents only would lose them.
  **LANDMINE #1 — `pagination.totalCount` SATURATES at `MAX_INDEX`** (measured 2026-07-30: the whole catalog,
  `gameVersion=1.12.2` and a 200 000-mod slice all report exactly `10 000`; only a slice genuinely below the cap
  reports its true size). Therefore **every split condition is `>= CAP`, never `> CAP`** — the `> CAP` /
  `> 2 × CAP` rules written from the docs could never fire, which made the entire partition plan inert (it would
  have crawled the top 10 000 of each version and nothing else) and left `warnIfSliceIsUnreachable` dead too.
  Do not "tidy" these back into size comparisons.
  **LANDMINE #2 — the version axis must be filtered by version *type*.** `/games/432/versions` returns **7 339**
  strings across 36 types, including Forge version families (`47.0.42`) and types named `Server Side`,
  `Shader Loader`, `Addons`, `DO NOT USE - Grouped MC Versions`. Keeping only types whose name starts with
  `Minecraft ` (via `/games/432/version-types`) leaves **135** real versions — a 54× smaller axis. Unfiltered,
  a sweep would burn 7 200 requests on partitions that can hold no mods.
  **LANDMINE #3 — sort order is approximate.** `desc` only *trends* by downloads (a live 10-mod page had one
  adjacent inversion: 385 316 073 before 386 940 279), and `asc` is not ordered at all though it does reach the
  tail (counts in the hundreds). `warnIfMisordered` therefore checks the descending **trend** (first vs last)
  and skips ascending entirely; checking adjacent pairs would warn on ordinary pages.
  **Axis lists refresh at sweep start *and whenever missing*** — the second condition is not an optimisation
  but a **restart-correctness fix**: a resumed cursor arrives with a partition token and an empty in-memory
  list, and without re-reading it the plan finds no next partition, reports the catalog finished and **throws
  the resumed position away** (found by `theLoaderStageHandsOverToTheCategoryStageWithinOneSlice`, pinned by
  `resumingMidSweepFetchesTheAxisListsItHasNotGotYet`). A failed version fetch degrades to the unfiltered top
  10 000; a failed category fetch leaves the loader stage working.
  **Residual gap (logged with a count, not hidden):** a (version, category, loader) slice >20 000 loses its
  middle — no narrower filter exists. A mod with *neither* a loader nor a category is unreachable beyond its
  version's cap and cannot be detected from outside. **Landmine:** `warnIfMisordered` must follow the
  partition's direction — an ascending slice is *supposed* to come back least-downloaded first, so the old
  descending-only check would have cried wolf on every bottom-up slice.
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
- **Loader-install reuse** (`CachedLoaderVersions`, `grinder.loader`): boots a loader build the cache already
  holds instead of the newest, falling back to newest when the `(loader, Minecraft)` pair is uncached. Loaders
  ship builds constantly and each new one is a fresh ~150 MB networked install for a server that boots mods
  identically, so on a catalog sweep this is the difference between a cache that grows with every loader release
  and one bounded by the pairs actually crawled. Picks the **most recently used** cached build, which also keeps
  it warm against eviction rather than rotating builds. Raw versions come from the marker, not the sanitized
  directory name (`LoaderCache.installedVersions`). **Safe only because of clientside's crash re-check** — see
  `serverpackcreator-clientside/CLAUDE.md`; `latestVersion` always delegates, so the cache can never revive an
  unsupported combo nor turn "needs a newer loader" into a false HIGH.
- **Cache eviction** (`LoaderCache.evictUnusedSince`, run after every pass, `SPC_GRINDER_CACHE_TTL_DAYS`,
  default 7, `0` = off): drops tuples nothing has booted in the window. Keyed on **last use** — `ensureInstalled`
  stamps the marker on every hit — so an in-service tuple is never pulled out from under a boot; unmarked
  partials go at any age. Takes the same per-tuple lock as installing, which is why that lock is keyed on the
  sanitized **path** rather than the raw tuple (eviction only ever learns the on-disk name).
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
  the newest version, under-cap version skips every split, over-cap version splits by loader **then by
  category**, all six loaders + every category walked in order for one version, each stage's ascending twin,
  category >2×cap narrowed by loader, deepest slices walk loaders then the next category, last category/version
  ends the catalog, empty version list ends after the unfiltered slice, **no categories still leaves the loader
  stage**, a vanished version/category falls forward, key round-trip + unreadable key (incl. the **3-field
  pre-category token**) ⇒ start of sweep, version ordering numeric-descending with unparsable last.
  Partitioned-source behaviour in `CurseForgeCandidateSourceTest`: which filters each slice queries (version,
  category, and all three at once), `/categories` requested for the mods class, crossing a partition boundary
  mid-slice (offset resets relative to the new partition), loader stage → category stage inside one slice, axis
  lists refreshed at sweep start only *but re-read when resuming mid-sweep*, cap → loader split via `totalCount`
  (incl. the single-item probe), failed probe keeps the position, and unavailable version/category lists
  degrading rather than breaking.
  `GrindPacingTest`: the three pause cases + work outranking a completed sweep.
- **`CatalogCrawlLiveIT`** (gated `GRINDER_LIVE_IT=1`, no containers, a few search calls) pins the *platform*
  assumptions no fake can: consecutive live batches return **different** projects, a fresh crawler over the
  same cursor file resumes rather than re-serving the head, and offset 40 000 still serves real projects.
  **Verified passing 2026-07-29.** Run it after touching paging or the cursor.
- **`CurseForgeCrawlLiveIT`** (gated `GRINDER_CF_IT=1` **and** a present `CURSEFORGE_API_KEY`; ~40 small calls,
  no downloads) pins every CurseForge behaviour the design rests on, each of which was *wrong or unknown* when
  taken from the docs alone: `totalCount` saturation, the cap applying to `index + pageSize`, the descending
  trend + unordered-but-tail-reaching ascending, the Minecraft-only version axis, the loader filter being
  honoured (1.16.5 slices: Forge 10 000 / Fabric 3 344 / Quilt 377 / NeoForge 238) and `sodium` appearing under
  Fabric but not Forge, a parent category not reliably including children, ~0 % of sampled mods lacking a
  category, the saturation split working end-to-end (`1.12.2` paged out at 10 000 → `1.12.2|*|1|desc` with real
  candidates), and the crawl advancing + resuming. **Verified passing 2026-07-30.** Each test prints a `[live]`
  line with the measured numbers — read them, they are the source for the facts quoted here.
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

## Java / image bound (landmine)

The grinder picks the *newest* Minecraft release, and one JDK cannot boot every version.
**`ImageJavaRuntimes`** sources the required Java major **authoritatively** from
`MinecraftMeta.requiredJavaVersion(mc)` (Mojang's declared `javaVersion.majorVersion` — scheme-proof, no
hand-rolled heuristic) and exposes `supports(mc)` (required Java known *and* in `bundledMajors`, default
**8/17/21/25**, which **must mirror the Dockerfile**) plus `javaPath(mc)`. `BootVerifier` takes an injected
`minecraftAcceptable` predicate (accept-all for the host CLI; `imageJava::supports` from
`ContainerCandidateVerifier`), AND-ed into candidate selection — so a version whose JDK the image lacks is
**never selected**, never booted on the wrong JDK, and **never mis-scored as a clientside crash (false
HIGH)**. Deliberately *not* `SKIP_JAVA_CHECK`. Java **26** is intentionally not bundled: it appears only on
snapshots, which the release gate already skips.
**To extend coverage** to a future release, add its JDK to the Dockerfile *and* to
`ImageJavaRuntimes.bundledMajors` — the two are the single coupled source of truth.
## Status & what remains

**Continuous mode + crawl cursor.** With no project-URL args `GrinderApplication` loops: each pass takes
the next catalog slice from `CatalogCrawler`, grinds what is stale, and persists verdicts *and* the crawl
position after every step, so a restart resumes mid-catalog. Env vars and their defaults are documented in
`README.md` §5 (pinned by `ReadmeConfigurationTest`); the implementation history is in `REFACTOR-LOG.md`.
**Sizing gotcha:** a sweep is `catalog ÷ batch × pass-duration`, so `SPC_GRINDER_REVERIFY_TTL_DAYS` must be
**longer than a sweep takes** — otherwise verdicts go stale faster than the crawl advances and the tail is
never reached.
**Loader-availability at selection — DONE.** `LoaderVersionResolver.latest` now returns `null` for a
Minecraft a loader doesn't support (Fabric/Quilt/LegacyFabric gated on `Meta.isMinecraftSupported`;
Forge/NeoForge already MC-specific), so an unsupported combo is dropped from selection instead of spun
up and aborted. The classifier's setup-abort INCONCLUSIVE mapping remains the backstop.

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

**Matrix results are point-in-time** — the last full run (5 Minecraft × 5 loaders × {bash, fish}, bash ≡
fish everywhere, `.ps1` parse ✅) is recorded in `claude-docs/REFACTOR-LOG.md`. Re-run it, don't trust a
table here. `N/A` cells are `LoaderVersionResolver`'s support gate filtering correctly, not failures.
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

**Landmine — `installDist` is not rebuilt by `test`.** A live run launched from
`build/install/serverpackcreator-grinder/bin/…` uses whatever jar was last built, and a stale one lies
convincingly: on 2026-07-30 a supervised run reported `crawl covers 7339 game version(s), newest first (65.1.0)`
— the *unfiltered* axis with a Forge version at its head — purely because the dist predated the version-type
filter by one commit. **Always `./gradlew :serverpackcreator-grinder:installDist` immediately before a live
run**, and sanity-check the axis log line (135 versions, newest a real Minecraft version) before trusting
anything the run says.

**Shutdown — `SIGTERM` mid-pass used to kill the JVM with a bare `Exception in thread "main"`** (found
2026-07-29 while verifying the crawl loop): the hook interrupts the main thread, which is normally parked in
`GrindPool.grindAll`'s `Thread.join()`, and the `InterruptedException` escaped `main`. `grindAll` now catches
it, `requestStop()`s and restores the interrupt flag, returning the count so far — pinned by
`anInterruptedPassStopsInsteadOfThrowing`. (The JVM often halts before `main` can log "Grinder stopped": once
the hooks finish it exits, so a missing final line on `SIGTERM` is normal, not a hang.) **The one-shot path had
the same hole** — its `CountDownLatch.await()` that holds the report server open threw the interrupt straight
out of `main`; both paths now swallow it. Any new park/join in `main` must do likewise.

**Shutdown drain.** `DockerJavaContainerEngine` is `AutoCloseable` and force-removes the containers it
still owns, because `run`'s per-run `finally` never executes when the JVM is torn down mid-boot;
`GrinderApplication` registers that as a shutdown hook (covers the one-shot path too) and
`GrindPool.requestStop()` makes workers abandon the queue after their current candidate.
Remaining:

1. **CurseForge is now live-verified end to end** (2026-07-30): discovery by `CurseForgeCrawlLiveIT`, and the
   *grind* path by a supervised one-shot that produced real verdicts for a CurseForge project on two loaders
   (Forge/1.20.6 and NeoForge/26.2, both booting offline). The module's oldest open item is closed. What is
   *still* unproven is a **full sweep**: weeks of wall-clock and a large slice of an API key's quota, so nobody
   has watched the crawl walk all 135 versions to the end.
2. **Store dedup is slug+platform, not project-identity** — good enough today; a mod that changes slug on a
   platform would be re-ground as a new project.
3. **The API key lives in the macOS Keychain on Griefed's machine** (`security find-generic-password -w -s
   spc-curseforge-key`), deliberately not in a file or in any transcript. The grinder itself only reads
   `CURSEFORGE_API_KEY` from the environment — there is no dotenv support anywhere in the build — so pass it in
   per command. The key rides in the `x-api-key` **header** and `JdkHttpFetcher` logs nothing, so it cannot leak
   into grinder logs or a failing test's output.
4. **Residual CF gap by design** — a (version, category, loader) slice >20 000 mods loses its middle (logged
   with a count; no narrower filter exists), and a mod with *neither* a loader tag nor a category is
   unreachable beyond its version's cap (undetectable from outside).
5. **Modrinth's offset ceiling is 99 999** (measured) vs. ~71 000 mod projects today, so the whole catalog is
   reachable — but **if it ever exceeds 100 000 the tail silently looks like the end of the catalog** and the
   crawl would wrap early.
