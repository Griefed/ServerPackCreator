<!-- Loads when Claude works with files in this package. Module-wide context (package map, the
cross-cutting landmines, remaining work) lives in serverpackcreator-grinder/CLAUDE.md. -->

# grinder.loader — per-tuple loader install, cache and offline pre-bake

Everything that makes a mod-boot runnable under `--network none`: install once per tuple with network,
then mount the cached tree offline.

- **`LoaderCache`** — the pre-bake cache (the `--network none` enabler). `ensureInstalled(loader,
  loaderVersion, minecraftVersion)` returns a cached installed-server base, running a one-off
  `LoaderInstaller` (with network) only on a **miss**; **marker-gated** (`.spc-installed` written only
  after success, so a crash mid-install is redone, never served half-baked) and **serialized per
  tuple** so parallel workers share a single install. `LoaderInstaller` is the seam — its real impl (a
  setup container *with* network that snapshots the ServerStarterJar's self-install) is
  integration-only; everything else here is pure and unit-tested.

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
