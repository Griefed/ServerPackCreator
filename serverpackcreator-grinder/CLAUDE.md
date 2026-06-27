# serverpackcreator-grinder — module context

> Standalone fire-and-forget service that boot-verifies mods **at scale**, in **isolated, network-less
> Docker containers**, to accumulate a catalog-wide list of suspected-clientside mods. Package
> `de.griefed.serverpackcreator.grinder`. Depends on `serverpackcreator-clientside` (+ `-api`
> transitively) and **docker-java** (`docker-java-core` + `docker-java-transport-zerodep`, 3.7.1). No
> Spring, no Swing. Not published to Maven.

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
- **Candidate source**: `ModrinthCandidateSource` enumerates Modrinth mod projects **most-downloaded
  first** (keyless search API; popularity = downloads, so the mods most likely to be in a modpack get
  ground first), paginating behind the clientside `HttpFetcher` seam (unit-tested with canned JSON).
  Feeds `GrindPool` its popularity-ranked queue. A CurseForge sibling (needs the API key, no declared
  sideness) is the natural follow-up.

**Landmine — network vs. install:** the hardening default is `--network none`, but the *first* boot of
a given loader/MC needs network for the ServerStarterJar to download the loader + libraries. The plan
is to **pre-bake that once per `(loader, loaderVersion, minecraftVersion)`** into a cached, read-only
base tree (network only on the cache-miss), then every actual mod-boot mounts it and runs offline.
Don't wire the candidate-mod boot to run with network — that defeats the isolation.

**Loader / Java facts (durable — drive the runtime image):** SPC's generated `start.sh` is
*self-contained* — it installs the modloader + Minecraft server itself at first boot, so the image is
**loader-agnostic** (no per-loader logic). The **`ServerStarterJar` (neoforged) is Forge/NeoForge
only**; Fabric uses `fabric-installer`/`fabric-server-launch(er).jar`, Quilt the `quilt-installer`,
LegacyFabric its own installer. A single JDK can't boot every Minecraft version (≤1.16→8,
1.17–1.20.4→17, 1.20.5+→21), so the image bundles Temurin 8/17/21 and the grinder sets `$JAVA` per MC
version (via the pack's `variables.txt`) — **no Java download**, which is what keeps mod-boots runnable
under `--network none`. The template needs `bash`, `curl`/`wget`, `gawk`, `tar`/`gzip`. See
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
  bundled per-MC JDK (`/opt/java-{8,17,21}`).
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
- `ModrinthCandidateSourceTest` (canned search JSON via a fake `HttpFetcher`): download-order
  preserved, pagination + catalog-exhaustion + over-limit trim, failed-page returns partial, limit-0
  fetches nothing.
- docker-java and the real installer have no offline doubles; `DockerJavaContainerEngine` and the
  production `LoaderInstaller` are integration-only.
- **`DockerJavaContainerEngineIT`** is the live-daemon integration test for the docker glue, **gated
  behind `GRINDER_DOCKER_IT=1`** (`@EnabledIfEnvironmentVariable`) so it is skipped on a normal /
  daemon-less CI run. Run it with a daemon + the `busybox:latest` image present:
  `docker pull busybox && GRINDER_DOCKER_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*DockerJavaContainerEngineIT"`.
  It verifies the full path (create → start → stream → ready-detect/stop → exit code → **force-remove**,
  no leaked containers) under the production hardening defaults. **Verified passing** against Docker
  29.5 on 2026-06-26.

## Still to build (the fire-and-forget service)

Done so far: container `ServerRunner` (+ hardening), `LoaderCache` pre-bake, grind **orchestration**
(`Grinder`/`GrindPool`/`VerdictStore`/`VerdictCsvExporter`), restart-safe `JsonVerdictStore`, and the
self-contained web report (`VerdictReportRenderer` + `ReportServer`). The **visible half** (sortable
table + CSV) is shipped; what's left is the integration that makes a real boot happen.

1. **Runtime image** — **drafted** at `docker/Dockerfile` (loader-agnostic: bash + curl/wget + gawk +
   tar + Temurin JDK 8/17/21; SPC's `start.sh` installs the loader itself). Still needs a real build +
   boot to validate. Then the real `LoaderInstaller` — a setup container run *with* network that
   snapshots the install into the `LoaderCache`.
2. **Real `CandidateVerifier`** — the integration adapter: a `ClientsideVerifier` whose
   `bootVerifierFactory` builds a `BootVerifier` over a `ContainerServerRunner`, with the `LoaderCache`
   base overlaid onto the generated pack before the offline boot. **The cache-overlay seam** (between
   `prepareBootPack` and the container run) is the one design decision left — resolve it with real
   ServerStarterJar behaviour in hand (it may need a hook in `BootVerifier`, since `ServerRunner.run`
   does not carry the loader/MC tuple). Needs the host prerequisites above (CF key + Playwright).
3. **CurseForge candidate source** — `ModrinthCandidateSource` is done (keyless, popularity-ranked);
   the CF sibling needs the API key and leans entirely on the jar scan (CF declares no sideness).
4. **Main entrypoint** wiring candidate-source → `GrindPool(Grinder(realVerifier, JsonVerdictStore))`
   + `ReportServer` for the actual fire-and-forget run (can already use `ModrinthCandidateSource`).
