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

## Subsystem detail lives next to the code

Each subpackage has its own `CLAUDE.md`, loaded only when you work in that directory — that is what keeps this
file small enough to stay useful. Read the one for the subsystem you are touching:

| Subsystem | Detail file |
|---|---|
| container runtime, docker glue, hardening | `src/main/kotlin/de/griefed/serverpackcreator/grinder/container/CLAUDE.md` |
| loader install, cache, eviction, Java/image bound | `.../grinder/loader/CLAUDE.md` |
| candidate sources, CurseForge partitions, crawl cursor | `.../grinder/source/CLAUDE.md` |
| verdict store, report server, CSV | `.../grinder/report/CLAUDE.md` |

**The base package's own orchestration** (`Grinder`, `GrindPool`, `GrindPacing`, `ContainerCandidateVerifier`,
`GrinderApplication`) is documented here, since it is what wires the four together.

- **Orchestration** (`Grinder`, `GrindPool`, `VerdictStore`, `VerdictCsvExporter`): `Grinder.grind`
  verifies one candidate (skipping already-ground projects, *swallowing* a thrown boot so a bad mod
  can't sink a worker) via the `CandidateVerifier` seam and records one `GrindVerdict` per loader.
  `GrindPool.grindAll` drains a **popularity-ranked** batch across N worker threads (N ≈ host-RAM /
  per-boot-memory — each in-flight grind holds a booting container). `VerdictStore` (in-memory default)
  accumulates, keyed by `slug+loader` (re-verify replaces, not duplicates); `VerdictCsvExporter`
  renders RFC-4180 CSV (`Name, Project, NamePattern, Confidence, Loader, Detail`, highest-confidence
  first). **`CandidateVerifier` is the seam that collapses the integration-bound boot pipeline**, so
  the whole orchestration is unit-tested with fakes.

- **Pacing** (`GrindPacing.pauseAfterPass`, pure + unit-tested): work found → no pause; nothing due but
  catalog remains → short `SPC_GRINDER_SCAN_DELAY`; sweep completed with nothing due → `SPC_GRINDER_INTERVAL`.
  **A fixed per-pass sleep is what made coverage impossible** (25 projects/6 h vs. ~71 000 Modrinth mods).
  **Failed verifications deliberately don't count as work** — with a broken host every candidate fails, and
  counting that as progress would race the cursor through the catalog leaving thousands unverified.

## Operator-facing logging (three surfaces, all live)

*Operator-facing documentation for these lives in `README.md` §7 — keep the two in step.*

- **`/status` on the report server** (`GrinderStatus` → `StatusSnapshot`, Jackson-serialized): uptime, current
  pass, per-worker candidate + `busySeconds`, crawl cursor per platform, installed-tuple count. Written from the
  worker threads, read from HTTP threads — `snapshot()` is a point-in-time **copy**, not a live view, so a
  serializing reader never observes mutation. Optional collaborators (`status`/`cursors`/`cacheRoot`): absent ones
  render `null` instead of failing, because a monitoring endpoint that 500s is worse than a thin one. Slugs are
  internet-supplied, so the document is *serialized*, never string-built.
- **One INFO line per candidate** in `Grinder.grind` (`Grinding <platform>/<slug>` … `Done … → Forge=LOW`), with
  the fresh-skip deliberately at DEBUG — a pass can skip dozens in microseconds and would bury the real line.
- **Live per-boot console.** `ServerRunner.run` takes an `onLine` sink (defaulted, so callers that don't care are
  unaffected); `BootVerifier.runPrepared` appends+flushes each line into the attempt's `boot.log` *during* the
  boot, and `DockerLoaderInstaller` does the same into `<tuple>/.spc-install.log`. **Why it matters:** output used
  to be buffered in memory and written only on completion, so a hung boot was undiagnosable until its 12-minute
  timeout fired and a killed boot left nothing at all. **Landmine:** every write is wrapped — a failing sink or an
  unwritable log must never fail a boot (a test pins that; `outcomeFor`'s final write was unguarded and *did*
  propagate before this).

## Cross-cutting landmines (do not let these load lazily)

These bite regardless of which subsystem you are in, so they stay in this always-loaded-for-the-module file even
though their detail lives deeper:

- **Never mount the Docker socket into a boot container.** A candidate mod is untrusted code; the socket is
  root-equivalent host access. (Also why there is no containerised grinder *daemon* image — see the README.)
- **Never wire the candidate-mod boot with network.** `--network none` is the whole isolation guarantee; only
  the one-off loader install per tuple gets network. Detail: `grinder/loader/CLAUDE.md`.
- **A mod must never be mis-scored as a clientside crash.** Two independent guards exist (selection-time loader
  availability + Java support, and the classifier's pre-launch setup-abort mapping), plus *three* crash checks
  in clientside: one when an older cached loader build was booted, one — since 2026-08-23 — when the crash
  contradicts a declared server support, which boots up to two **other versions of the mod** to find out whether
  the crash was that build's, and a free cross-loader pass that refuses to let a crash stand when another loader
  of the same project booted a server with the same list-entry (the entry is what gets published, and it is
  loader-agnostic). See `serverpackcreator-clientside/CLAUDE.md`. **Consequence for pacing:** one
  contradicting crash can now hold a worker for up to three boot budgets (~45 min at the default 15), which is
  the price of not publishing a wrong `HIGH` to `/as-properties`. Only a crash the metadata contradicts pays it;
  a genuine clientside mod still costs one boot, because its metadata and its crash agree.
- **`installDist` is not rebuilt by `test`** — always rebuild before a live run, or you will draw conclusions
  from a stale jar (this has happened: a run reported the unfiltered 7 339-version axis because of it).

- **Configuration is `GrinderConfiguration`, and the sweep is `GrindLoop`; `main` composes and hands off.**
  Both were lifted out of a 294-line `main` on 2026-08-29 (now 259), and the point was never the line count:
  - **`GrinderConfiguration` reads every knob once and is *executable*.** `KNOBS` is a real list, so
    `ReadmeConfigurationTest` and `SystemdUnitConfigurationTest` iterate it instead of regexing
    `GrinderApplication`'s source — which only worked while every `env(...)` stayed inside one function and
    would have silently stopped covering anything that moved out. `from(lookup)` takes its environment as a
    parameter, so a test asserts what the daemon *would do* rather than that a string is present.
    **Landmine: a knob not in `KNOBS` is invisible to both documentation guards.**
  - **`GrindLoop` had no test at all before the extraction.** Requeue-before-catalog, committing only what
    was reached, and polling `running` between steps are the daemon's central behaviours, and all of them
    lived where a Docker daemon was needed to run them. It takes `evictUnusedInstalls` and `verdictCount` as
    functions rather than `LoaderCache`/`VerdictStore`, which is what keeps its tests free of a Docker-bound
    installer. **Landmine for writing its tests:** `running` is polled *between steps*, so a counter-based
    fake stops the loop mid-pass and proves nothing — flip the flag from the injected `sleeper`, which is
    where a real stop lands.
  - **18 source-text greps remain**, in `ReportBindWiringTest`, `ContainerLimitsWiringTest`,
    `FallbackListWiringTest`, `ShutdownWiringTest` and `GrinderSpcEnvironmentTest`. Those assert *joins*
    inside `main` — that a configured value reaches the collaborator it configures, that the shutdown hook
    is ordered correctly — and `main` still cannot be executed, so they stay. What changed is that they now
    grep for `config.<property>` rather than for `env("NAME", "default")`, and the *values* they used to
    imply are asserted for real elsewhere.

- **The daemon owns its own `Preferences` node — do not "simplify" that away.** SPC resolves its home directory
  through a `Preferences` node (`PathsConfig.homeDirectory`), historically the hard-coded, **machine-wide per-user**
  `ServerPackCreator` shared by the GUI, the web backend, every test suite *and* the grinder — and the getter
  **re-reads it on every access**, storing whatever it resolved. A test suite booting an `ApiWrapper` therefore
  relocated the *running* daemon's home to its own scratch dir and then deleted it. Measured 2026-07-30: a
  `:serverpackcreator-api:test` run mid-session moved the live daemon's home into the repo, and every subsequent boot
  failed with `.../serverpackcreator-api/tests/server_files/server-icon.png: The source file doesn't exist` —
  surfacing as **`boot:none` metadata-only verdicts**, i.e. looking exactly like "these mods were never bootable"
  rather than like a broken host. 30+ candidates were recorded that way before it was caught, and the store had to be
  archived. `GrinderApplication` now claims **`ServerPackCreator-grinder`** (via
  `ApiProperties.PREFERENCES_NODE_PROPERTY`, set before any `ApiProperties` exists, and only when the operator has
  not chosen a node themselves) and logs which node it used. Verified: a full api suite run *concurrently* with a
  live daemon left it untouched.
  - The node override is `-Dde.griefed.serverpackcreator.preferences.node` / `SPC_PREFERENCES_NODE`; the home
    override is `-Dde.griefed.serverpackcreator.home`, which now beats the dev-build working-directory fallback.
  - **The daemon also pins the home itself** (`pinSpcHomeDirectory`, 2026-08-22): claiming the node stopped another
    process from *moving* the home, but left SPC to *choose* one — and for a source build (which every locally
    built artifact is) that choice is the process working directory. Under systemd that is `/`. It sets
    `-Dde.griefed.serverpackcreator.home` to `SPC_GRINDER_HOME` unless the operator set it, which also repairs a
    host whose node already remembers a bad value, since a `-D` outranks the stored preference without replacing it.
  - The preference is consulted **before** cwd and `serverpackcreator.properties`, so `SPC_GRINDER_SPC_PROPERTIES`
    alone never protected against this — the node claim is what does.
  - Editing a template under the grinder home is pointless while the home resolves elsewhere; generation reads
    `server_files` from the *then-current* home. Check the daemon's own startup lines (`Using Preferences node …`,
    `Home directory set to: …`) rather than guessing.
  - **Measuring this from outside is unreliable:** Java's macOS `Preferences` backing store
    (`~/Library/Preferences/com.apple.java.util.prefs.plist`) is cached per process and flushed on a ~30 s timer, so
    concurrent JVMs clobber each other's view and an external `defaults read` can show a value that a still-running
    JVM is about to overwrite. Trust in-process logs and same-JVM tests, not cross-process snapshots. (A per-module
    loop appearing to show "every suite writes the shared node" was exactly this artifact.)
  - **ANSWERED 2026-07-31 — it was the build itself, and it is fixed.** Three build-script writers touched the
    shared node: `java-conventions`' `cleanup()` did `removeNode()` and then wrote the module's `tests` directory in
    as the home, and the `-api` and `-app` build files each `clear()`ed it at *configuration* time. `cleanup()` runs
    in `doFirst` of **both `test` and `clean`, for every module**, so any build relocated the home of the
    developer's own GUI — and of a running daemon — into the repository. All three are gone; the isolated per-module
    node plus the injected `-Dde.griefed.serverpackcreator.home` replace them entirely. The `-app` call sites were
    already routed through `HomeDirectoryPreference` (B1), with `GuiProps` left on the shared node deliberately.
    **A stale value may still be stored** from before the fix — check the shared node once if a GUI instance
    resolves a surprising home.

- **LANDMINE — the first `log.` call in `main` builds an `ApiProperties`, so every SPC decision must precede it.**
  `ApiProperties` is annotated `@Plugin` and *is* log4j's `ConfigurationFactory`, so log4j instantiates one while
  initialising — with whatever node and home are resolvable at that moment, and it *persists* what it resolved. The
  node claim originally sat **after** the `Grinder starting …` line, which is why the reported systemd crash's
  earliest stack frame is `GrinderApplication.getLog`, before `main` had wired anything. Both claims now run as the
  first statements of `main`; keep them there, and keep new startup logging below them. Pinned by
  `GrinderSpcEnvironmentTest.theSpcEnvironmentIsClaimedBeforeTheFirstLogStatement`, which asserts the ordering
  against the source, since a JVM whose logging is already initialised cannot observe it.
- **LANDMINE — the report binds loopback by default, and it is unauthenticated. Both halves matter.**
  `ReportServer`'s `host` defaults to `127.0.0.1`, and until `SPC_GRINDER_HOST` existed `main` never passed one,
  so the daemon was unreachable through any reverse proxy: a proxy in a container dials the host over the Docker
  bridge gateway, never `127.0.0.1`, and a loopback socket refuses that at the TCP layer — the operator sees a 502
  while the report answers fine over an SSH tunnel. Raise the bind to the *gateway address*, not `0.0.0.0`: `/`,
  `/status` and `/export.csv` all answer unconditionally, with no auth anywhere in `start()`. Pinned two ways,
  because neither alone reaches: `ReportServerBindAddressTest` executes the mechanism over a real non-loopback
  IPv4 (skips where the host has none), and `ReportBindWiringTest` asserts against `main`'s source that the
  variable actually reaches `ReportServer`'s `host` — the join no test can execute, because `main` boots Docker.
  README §5 *Exposing the report* is the operator-facing half.
- **LANDMINE — a container must run as the *owner of the directory it mounts*, not as the image's `USER`.**
  `docker/Dockerfile` bakes in `USER 1000:1000`, and `ContainerSpec.user` defaulted to the same literal — correct
  only while the daemon itself is uid 1000, which stopped being true the moment it became a systemd service under
  its own account. Every boot and install bind-mounts a directory the *host* process created, so a mismatch means
  the container reads the pack and writes nothing. **The failure names the wrong subsystem:** the start script
  carries on past its refused writes and dies ~20 lines later on the JVM's `Error: could not open
  'user_jvm_args.txt'`, which reads as a broken start-script template. Measured 2026-08-23: every install failed
  across Fabric, Forge *and* NeoForge at once — loader-indifference is the tell for a permission wall.
  `ContainerUser.forDirectory` resolves it (override `SPC_GRINDER_CONTAINER_USER`), `GrinderApplication` logs it
  as `containerUser=` on the startup line, and `InstallFailureDiagnosis` names it in the failure warning.
  **Corollary:** `DockerLoaderInstaller` quoted `output.lines.takeLast(25)`, and this cause sits at the *top* of
  the console — a tail is the wrong slice whenever the first failure is survivable, so the diagnosis scans all of it.
- **`/as-properties` publishes the fallback clientside list, and only `HIGH` may ever reach it.**
  `FallbackPropertiesRenderer` merges the list SPC currently holds with every crash-proven verdict and serves it
  where an instance's `de.griefed.serverpackcreator.configuration.fallback.updateurl` can poll it. Two things are
  load-bearing. It is written for `Properties.load(InputStream)`, which decodes **ISO-8859-1** — hence `\uXXXX`
  escaping and an ISO-8859-1 response, the one endpoint that is not UTF-8. And the confidence floor is not a
  tunable: a clean boot proves nothing, while a wrong entry silently strips a mod from every server pack built
  against the list. **Never point the grinder's own SPC instance at this endpoint** — its findings would fold back
  into what it publishes as "the shipped list", and an entry could then never leave it. **Second-order:** the
  base list it publishes is whatever *this* daemon's SPC holds, and `UpdateConfig` replaces a client's lists
  wholesale — so a grinder on an old build, or one that could not reach the repository at startup, hands every
  client a *staler* list than they had. Pinned end-to-end by `FallbackPropertiesConsumerTest`, which drives the
  real `UpdateConfig` against a running `ReportServer` over loopback — the model-vs-consumer distinction matters
  here, since everything else asserts against `java.util.Properties` rather than SPC itself.
- **A unit-level `CPUQuota=` bounds the JVM and nothing else** — same cause as the shutdown landmine below:
  containers belong to the docker daemon's control group, not the service's. `SPC_GRINDER_CPUS` (cores per
  container, default `2`, `0` = uncapped) is the only lever on the boots; the daemon's own host-side share —
  mod resolution/downloads, pack generation, the headless Chromium for a locked CurseForge file — is the half
  systemd *can* cap. Floor the knob at ~1 core: a Minecraft startup is largely single-thread-bound, and a boot
  throttled past its 15-minute budget is scored INCONCLUSIVE, which reads as a hanging mod rather than a
  starved host. Detail (and why the quota must be sent with its period) in `grinder/container/CLAUDE.md`.
- **LANDMINE — a container is not in the unit's control group, so only the application can stop it.**
  Containers are children of the docker daemon; `systemctl stop` kills the JVM's cgroup and never touches them.
  The shutdown hook is the *only* thing that does: it marks the engine closed (so a worker cannot create one
  behind the sweep), `docker stop`s each in flight with `SHUTDOWN_GRACE` (15s SIGTERM-then-kill, 8 at a time
  because the window is per container), then gives the workers what is left of the same window via
  `GrindPool.awaitStop`. Three things follow. `requestStop` alone can never end a shutdown — its flag is read
  only *between* candidates, so a worker inside a boot runs for up to that boot's 15-minute budget; the
  interrupt is what wakes it. `TimeoutStopSec` in the unit must stay above the window, or systemd's SIGKILL
  lands during the cleanup that prevents the leak (the arithmetic is in the unit's own comment). And workers
  are **threads**, so "force kill a worker" does not exist — the JVM exiting is the force, and `awaitStop`
  only decides when to stop waiting. Verified against a live daemon, not reasoned about: a container trapping
  SIGTERM proves the signal arrives before removal (`DockerJavaContainerEngineIT`, gated on
  `GRINDER_DOCKER_IT=1`).
- **Every container carries `OWNER_LABEL`, and that label is the only way to find an orphan.**
  A SIGKILLed JVM leaves containers running with nothing tracking them — the in-memory set died with the
  process, and they have no name and no autoremove. `reapOrphans()` at startup is the sole recovery, and it
  assumes **one grinder per Docker daemon**: the label says "a grinder made this", not "*this* grinder", so a
  second instance sharing a daemon would have its live boots reaped by the first one's startup. The shipped
  unit is a singleton, which is what makes the simple label safe.
- **Never hand SPC a *relative* properties file — a loaded one becomes a permanent write target.**
  `PropertyStore.loadProperties` adds every file it reads to `trackedPropertyFiles`, and `save()` writes to **all**
  of them on every save (skipping any that no longer exist, except `alwaysWrite`). `ApiProperties`' default is the
  relative `File("serverpackcreator.properties")`, so `ApiWrapper.api()` with no argument made the daemon create and
  rewrite a settings file in whatever directory it was started from — a checkout got one in its repository root on
  every start. `GrinderApplication.resolveSpcPropertiesFile` now always returns an absolute path (the operator's
  `SPC_GRINDER_SPC_PROPERTIES`, else one inside the daemon's own home), pinned by
  `GrinderPropertiesResolutionTest`; verified by starting the daemon *from* the repo root and watching the tree stay
  clean. **Expected and harmless:** the startup log also shows a save into
  `build/install/serverpackcreator-grinder/lib/serverpackcreator.properties` — the dist's own copy, which SPC loads
  and therefore tracks. It lives under `build/`, so it is regenerated and gitignored; don't chase it.
  **Also expected since the home is pinned to the base:** `<base>/serverpackcreator.properties` is now both the
  file the daemon passes in *and* the home candidate SPC looks for, so `Loaded properties from …` appears twice per
  start. Harmless — `PropertyStore.save` collects into a `TreeSet<File>`, so the duplicate collapses and the file is
  written once.
- **A cached install is a product of the templates that built it** (`TemplateProvenance` + the marker's
  `templates=` key). The install boot runs the pack's own `start.sh`, so a template change that alters what an
  install *produces* leaves cached layers stale — and the marker used to record only loader/version/Minecraft, so
  `ensureInstalled` served them regardless (the launcher-era fix needed two Forge tuples invalidated by hand). A
  **differing** digest is now a miss; an **absent** one is tolerated with one warning per run, because treating
  unknown as different would reinstall all ~74 tuples at ~150 MB and a networked boot each. That default is
  evidence-based: when the Forge install-ownership fix landed, the cached tuples were checked and every one was
  still bootable, since the argfile the new launch path uses is what the installer had already produced. **Check
  before invalidating.**
- **"Not applicable" must never mean "we could not find out."** `ImageJavaRuntimes.supportFor` distinguishes
  `JDK_NOT_BUNDLED` (a real, permanent exclusion) from `REQUIREMENT_UNKNOWN` (SPC could not determine the version's
  required Java — which `MinecraftServer.javaVersion()` also returns for a *failed* manifest download). Collapsing
  the two is how **all four Minecraft 26.2 cells**, Fabric included, disappeared from a green `ScriptTemplateMatrixIT`
  run — the newest Minecraft, and the exact branch the Forge template fixes were written for. The IT now fails the
  unknown case instead of skipping it, and `26.2` is in the default Minecraft axis so the `YY.x` scheme is exercised
  without anyone remembering to pass `SPC_GRINDER_TEMPLATE_MC`.
- **Staging is reclaimed, not accumulated** (`BootWorkspaceReaper`). Each attempt stages a full server pack with the
  overlaid loader libraries under `<work>/verify/boot/<platform>-<slug>-<loader>` plus downloaded jars under
  `<work>/verify/verify/<platform>-<slug>-<loader>`, and staging only ever deleted a directory when that *same*
  `(platform, slug, loader)`
  was retried — which during a catalog sweep is never. Measured 2026-07-30: **98 GB across 1750 attempt directories,
  ~23 GB/h**, enough to fill the host inside a day. The reaper strips each finished candidate's staging down to its
  `boot.log` (the verdict detail is read from it; the packs are reproducible), runs in a `finally` so a *thrown*
  verification is reclaimed too, and sweeps orphans at startup — first live startup reclaimed 8 897 MiB, taking the
  work tree from 8.7 GB to 155 MB. **Landmine:** it is scoped to one **`(platform, slug)`** on purpose, and both halves matter.
  The names are built and parsed by `AttemptDirectory` in `-clientside` — one place, because the two verifiers
  that *write* the name and this reaper, which decides what to *delete* from it, used to agree only by separate
  string literals happening to match. The loader suffix is cut rather than the slug prefix-matched (`jei` vs
  `jei-extras`), and the platform is part of the scope because **the same slug on Modrinth and CurseForge is two
  candidates this pool grinds in parallel** — freshness is keyed `(platform, slug)` for the same reason. Reaping
  on the bare slug deleted the other platform's pack mid-boot: measured on `creativecore`, 2026-08-23, two
  platform runs 71s apart produced NeoForge 26.2.0.66 / MC 26.2 reading **SURVIVED on one and CRASHED on the
  other** for the identical build, a Fabric boot exiting **127** (the shell could not find the command — the pack
  had gone), and re-checks reading INCONCLUSIVE on a file the other run had booted to a ready-line. A crash is
  the one outcome that reaches HIGH, so this manufactured false positives rather than merely losing runs.
  Directories staged before the rename match no owner and are cleared by the startup `reapAll()`.
  **Landmine — reap the identity the staging was *named* from, not the candidate's.** Directories carry
  `ProjectFiles.platform`/`slug` (the resolved report's); `ContainerCandidateVerifier.reapTarget` therefore
  prefers the report and falls back to the candidate only when the verification threw and there is no report
  to ask. `Grinder` logs `"Platform mismatch for …: candidate says 'X', resolved report says 'Y'"`, so the two
  are known to be able to disagree, and a slug is a mutable name a rename can move out from under a queued
  candidate. Asking with the candidate's copy of either matches nothing and leaks a whole pack per attempt.
- **Every non-survived attempt's evidence outlives its staging** (`BootLogStore`, `ContainerCandidateVerifier.keepAttemptArtifacts`, fired per attempt by `BootVerifier`'s `bootArtifactSink`). Was crash-console-only until 2026-08-28; now the container console *plus* the server's own `logs/` and `crash-reports/`, per attempt, for every boot that did not SURVIVE — because a mod wrongly **cleared** left no evidence at all, and neither did an error in the checking itself. Bounded by `pruneExcept` per tuple and `SPC_GRINDER_BOOT_LOG_BUDGET_MIB` (default 2048) overall; details and the concurrency landmine in `grinder/report/CLAUDE.md`. Superseded text follows for the reasoning that still holds:
  The reaper keeps one `boot.log` per attempt directory, but staging *wipes and re-creates* that directory, so
  the next re-grind of the same tuple destroyed the console for a verdict that is still published. Since a crash
  is the only outcome that reaches HIGH — and its usual cause, a server loading a mod that reaches for a
  client-only class (`NoClassDefFoundError: net/minecraft/client/…`), is legible from the console and nothing
  else — crashing consoles are copied into `<home>/crash-logs` as each candidate's verdicts land. **Only
  CRASHED is kept**: a clean boot proves nothing about sideness and explains nothing either.
  - **Under the *home*, not under `work/`** — everything below `work/` is scratch the reaper may reclaim.
  - **Growth is bounded by the catalog, not by uptime**: a log is named `<platform>-<slug>-<loader>.log` via
    the same `AttemptDirectory` helper, so a re-grind *replaces* it. That is the deliberate opposite of the
    naming that once grew the work tree to 98 GB. Oversized consoles keep their **tail** (the stack trace is
    at the end) with the truncation written into the file.
  - **LANDMINE — the name is untrusted input.** `/crash-log?name=` addresses the store by name, and this
    report has no authentication and is documented as reverse-proxyable. `read` requires a plain file name
    resolving directly inside the store — checked on the string before the filesystem is touched, then
    confirmed canonically so a symlink cannot lead out — and a refusal is deliberately indistinguishable from
    an absent log, so probing tells a caller nothing. Two tests pin it; do not "simplify" it to `File(dir, name)`.
- **The immediate re-grind queue is how a *defect in the engine* gets un-published** (`RequeueStore`,
  `RequeueSelection`, `Grinder.grind(force)`). The crawl and the re-verify TTL answer "when does this come
  round again?" with *eventually, at TTL* — correct when a mod changes, wrong when the bug is ours, and then
  the bad verdicts are already being served. Drained at the **start of every pass**, before the catalog slice.
  **LANDMINE — the drain must stay `force = true`.** A project is queued precisely because its verdict is
  wrong, and a wrong verdict is usually a *recent* one (engine defects are found by reading verdicts that were
  just produced), so an unforced drain turns straight into `SKIPPED_FRESH` and looks like it worked.
  `aForcedGrindReVerifiesEvenAFreshVerdict` pins it.
  - Selectors: `--requeue <url>…` for a named handful, `--requeue-before <ISO instant>` for the recurring
    shape — a defect invalidates a *population*, not a hand-assembled list. One candidate per project
    (platform + the platform's own id where known), so a rename is still one re-grind and the same slug on
    two platforms is still two.
  - **LANDMINE — the CLI path runs *before* `claimSpcPreferencesNode()`/`pinSpcHomeDirectory()` and must never
    use `log`.** It is run by an operator against a service that is already up: claiming or re-pinning would
    move the home out from under the running daemon, and those claims are remembered for every later run. The
    first `log` statement in a process constructs the very `ApiProperties` they exist to control, so this path
    prints to stdout. `theRequeuePathRunsBeforeTheClaimsAndNeverLogs` guards **both** halves — `main`'s own
    body cannot see a log call made from inside the helper, which is why the guard reads the helper's source.
  - **Not an HTTP endpoint, on purpose.** The report server has no authentication; a write endpoint there
    would let anyone who can reach the page schedule unbounded container work.
  - `/status` reports `requeued`; a queued grind logs `(re-grind requested)`, which is how the log tells
    "the crawl reached this" from "somebody decided the old verdict was wrong".
- **The report links every endpoint.** `/export.csv`, `/status`, `/as-properties` and `/crash-logs` are buttons
  beside "Download CSV", and each crashing row links its own console. They were previously reachable only from
  a line printed at startup, which an operator sees once. `VerdictReportRenderer.toHtml` takes a per-row
  *lookup* for the crash-log name rather than reading a field off `GrindVerdict`: the log lives on disk, so
  asking at render time means the link appears exactly when the file does, and a hand-deleted log cannot
  strand the table pointing at a 404.

and the grinder sets `$JAVA` per MC version (via the pack's `variables.txt`) from SPC's declared
required-Java — **no Java download**, which is what keeps mod-boots runnable under `--network none`.
The template needs `bash`, `curl`/`wget`, `gawk`, `tar`/`gzip`. See
`docker/README.md`.

**Host prerequisites (apply once `ContainerServerRunner` is wired into a `BootVerifier`):** the
download/resolve phase runs on the **host** (in `BootVerifier.prepareBootPack`), *not* in the boot
container, so the box running the grinder needs:
- **`CURSEFORGE_API_KEY`** env var — `clientside.supportedPlatforms()` only registers CurseForge when
  the key is present; without it CurseForge links cannot be resolved at all (Modrinth needs no key).
- **Playwright + Chromium** — distribution-locked CurseForge files (`allowModDistribution=false`,
  `downloadUrl=null`) are routed by `clientside.selectDownloader` to the headless-browser
  `BrowserDownloader`, which runs on the host during staging. The key and the browser are
  **complementary**: the key resolves the project and reveals the file is locked; the browser fetches
  the withheld jar. A locked CurseForge mod needs **both**. Wire the `BootVerifier` with a
  `BrowserDownloader()` (disposed via `use {}`) exactly as `VerifyClientsideCommand` does — locked-file
  support is then inherited, not reimplemented.
  - **LANDMINE — the *browser* is not the half that goes missing; the OS libraries are.** Playwright's
    Java binding downloads browsers itself on the first `Playwright.create()`
    (`DriverJar.installBrowsers()`, which is why `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD` exists), so a host
    that never ran an install still gets one. It does **not** install the libraries Chromium links
    against, and on a headless server those are absent by default — Chromium then launches and every
    navigation times out, which reads as CurseForge being slow. `clientside-boot.yml`'s reusable job
    installs exactly those and nothing else, which is the shape of the gap. `install-grinder.sh` now
    does both (`--skip-browser` opts out).
  - **Install with Playwright's own CLI, never `npx playwright install`.** Playwright pins one Chromium
    build per release and looks for that exact directory — 1.62.0 wants `chromium-1234` (Chrome for
    Testing 151.0.7922.34, in `driver-1.62.0.jar`'s `browsers.json`). `npx` fetches whatever revision
    the *npm* package pins, landing beside it as `chromium-<other>`, so a check for `chromium-*` reports
    success while the binding still downloads its own. Driving `com.microsoft.playwright.CLI` from the
    installed `lib/*` makes the version match by construction: the same driver jar that runs the
    download decides what to fetch, and a version bump therefore cannot leave it stale.

## Testing

The per-test-class inventory that used to live here is derivable — `ls serverpackcreator-grinder/src/test` and
read the files; it also drifted (it listed 8 of the 30 test files). What is *not* derivable is kept here:

- **Everything offline by default.** Orchestration, sources, crawl/cursor, partition plan, pacing, report and
  the pure loader pieces are unit-tested with fakes (`GrindTestFixtures.kt` holds the shared builders).
- **Integration-only, no offline double exists:** `DockerJavaContainerEngine`, the production
  `LoaderInstaller` (`DockerLoaderInstaller`) and `ApiVanillaPackGenerator` — they need a live daemon, the
  runtime image, and a real `ApiWrapper`. If you change them, verify against Docker; the unit-testable
  orchestration deliberately sits *behind* their seams for exactly this reason.
- **Gated integration tests** (skipped on a normal run — each needs something CI has not got):

  | Test | Gate | Also needs | Last verified |
  |---|---|---|---|
  | `DockerJavaContainerEngineIT` | `GRINDER_DOCKER_IT=1` | a daemon + `docker pull busybox` | Docker 29.5, 2026-06-26 |
  | *(boot-log capture, verified by a live one-shot rather than an IT)* | — | a daemon + `spc-grinder-runtime` | Docker 29.7.2, 2026-08-29 |
  | `ScriptTemplateMatrixIT` | `GRINDER_TEMPLATE_IT=1` | the `spc-grinder-templates` image | 2026-07-29 |
  | `CatalogCrawlLiveIT` | `GRINDER_LIVE_IT=1` | network (Modrinth) | 2026-07-29 |
  | `GrinderAuditIT` | `GRINDER_AUDIT_IT=1` | network + a **live grinder** (`SPC_GRINDER_AUDIT_URL`) | 2026-08-31 |
  | `CurseForgeCrawlLiveIT` | `GRINDER_CF_IT=1` | **plus** `CURSEFORGE_API_KEY` | 2026-07-30 |

  e.g. `docker pull busybox && GRINDER_DOCKER_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*DockerJavaContainerEngineIT"`
- **`GrinderAuditIT` grades a live daemon's published verdicts against their own evidence, and it exists
  because sample-and-fix failed twice.** A 200-log census (2026-08-29) and a merge gate reporting `HIGH 8 → 4`
  both preceded the 2026-08-31 finding that **four of five** sampled boot logs were scored `CRASHED` by the
  bare exit-code rung — one of the mods already in the served fallback list. Neither census was committed, so
  nothing re-checked the published list against the consoles behind it and the loop never closed.
  - It asks one question of the whole store: **is every published `HIGH` decided by a rung
    `BootDecision.decisive` marks?** Failure prints the distribution by decision, so a new defect shows up as
    a bucket rather than as a surprise.
  - **`/boot-logs` is HTML-only** — no machine-readable listing exists — so names are scraped from its
    `?name=` hrefs. The tuple is everything before the first `~`, which holds because slugs and loaders
    contain `-` but never `~`.
  - It assumes a **non-zero exit** when re-classifying, deliberately: the exit status is not published, and
    every rung a HIGH can legitimately come from is decided on the console alone. Assuming non-zero is what
    keeps the exit-code rung *reachable*, and therefore counted.
  - Politeness is part of the contract: gated, capped by `SPC_GRINDER_AUDIT_SAMPLE` (200), sequential, and it
    never fails the audit over one missing artifact.
- **The live ITs are the source of the platform facts quoted in this file.** Each prints `[live]` lines with
  its measured numbers (catalog sizes, slice sizes, saturation, ordering) — read those rather than trusting a
  number written down here, and re-run them after touching paging, the cursor, or the partition plan.

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
2. **Store dedup is project-identity — DONE 2026-07-31.** `GrindCandidate`/`GrindVerdict` carry the platform's
   immutable `projectId` (Modrinth `project_id`, CurseForge numeric `id`), and `verdictKey` uses it in place of the
   slug when present, so a renamed project replaces its own verdict and still counts as ground. The id is
   **nullable** and dedup falls back to the slug, because ~870 verdicts predate it — and recording an identified
   verdict *supersedes* the id-less row for that slug, so a live store converges as projects are re-ground with no
   schema step. Pinned by `ProjectIdentityDedupTest`, including the legacy fallback. Both stores derive the key from
   the shared `identityKey()` so they cannot drift.
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
