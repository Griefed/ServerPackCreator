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
- **`grinder.source`** — candidate discovery: the `CandidateSource` interface + `ModrinthCandidateSource`
  and `CurseForgeCandidateSource`.

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
- **Candidate sources** (`CandidateSource` interface — `candidates(limit): List<GrindCandidate>`,
  most-downloaded first): `ModrinthCandidateSource` (keyless Modrinth search) and
  `CurseForgeCandidateSource` (CF `/mods/search` sorted by `sortField=6` TotalDownloads, `x-api-key`,
  `index`/`pageSize≤50` pagination capped at `index<10000`; project link = `links.websiteUrl`). Both
  paginate behind the clientside `HttpFetcher` seam (unit-tested with canned JSON). `GrinderApplication`
  wires Modrinth always and CurseForge **only when `CURSEFORGE_API_KEY` is set**; `GrindPool` re-sorts
  the union by popularity so the platforms interleave. Store dedup is by `slug`, so a mod on both
  platforms is treated as one project (accepted for now).
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
- `ModrinthCandidateSourceTest` (canned search JSON via a fake `HttpFetcher`): download-order
  preserved, pagination + catalog-exhaustion + over-limit trim, failed-page returns partial, limit-0
  fetches nothing.
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
each pass re-pulls the popularity-ranked candidates and grinds them; `Grinder` skips a project whose
verdict is still *fresh* (younger than `reverifyTtl`, via `VerdictStore.newestVerification`) and
re-verifies stale ones, so evolving mods, new loader versions and newly-supported Minecraft releases get
picked up over successive passes. Verdicts persist after every record, so a restart resumes. A JVM
shutdown hook stops the loop. Passing explicit project URLs keeps the **one-shot** path (verification).
Config (env): `SPC_GRINDER_INTERVAL` (seconds between passes, default 21600 = 6h),
`SPC_GRINDER_REVERIFY_TTL_DAYS` (verdict staleness, default 30). There is still **no queue cursor** —
each pass re-fetches the source fresh (cheap; the store's freshness check does the skipping).

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

Remaining:

1. **Store dedup across platforms** (minor) — verdicts are keyed by `slug`, so the same mod on Modrinth
   and CurseForge collapses to one project. Fine for now; a platform-qualified key would separate them.
