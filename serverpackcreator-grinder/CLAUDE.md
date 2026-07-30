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
  availability + Java support, and the classifier's pre-launch setup-abort mapping), plus clientside's crash
  re-check when an older cached loader build was booted. See `serverpackcreator-clientside/CLAUDE.md`.
- **`installDist` is not rebuilt by `test`** — always rebuild before a live run, or you will draw conclusions
  from a stale jar (this has happened: a run reported the unfiltered 7 339-version axis because of it).

- **NEVER run any module's test suite while a live grinder run is going.** SPC resolves its home directory through
  `Preferences.userRoot().node("ServerPackCreator")` (`PathsConfig.homeDirectory`) — one **machine-wide, per-user**
  node shared by the GUI, the web backend, the test suites *and* the grinder — and the getter **re-reads it on every
  access**, storing whatever it resolved. So a test suite starting up relocates the *running* daemon's home to its
  own scratch dir (`serverpackcreator-<module>/tests`), and then deletes it. Measured 2026-07-30: at 14:29 a
  `:serverpackcreator-api:test` run silently moved the live daemon's home into the repo, and every subsequent boot
  failed with `.../serverpackcreator-api/tests/server_files/server-icon.png: The source file doesn't exist` — which
  surfaces as **`boot:none` metadata-only verdicts**, i.e. it looks exactly like "these mods were never bootable"
  rather than like a broken host. 30+ candidates were recorded that way before it was caught.
  **Consequences worth knowing:**
  - The daemon's home is decided by whichever SPC process last touched the preference — *not* by cwd or by
    `serverpackcreator.properties` (the preference is consulted **first** and wins over both, so
    `SPC_GRINDER_SPC_PROPERTIES` cannot protect against this either).
  - Editing a template under the grinder home is pointless while the preference points elsewhere — the generation
    reads `server_files` from the *then-current* home.
  - Repair: set the preference back explicitly (`Preferences.userRoot().node("ServerPackCreator")
    .put("de.griefed.serverpackcreator.home", …)` + `sync()`), then relaunch. Reading the value on macOS:
    `defaults read com.apple.java.util.prefs | grep -A2 ServerPackCreator`.
  - The collision runs **both ways**: a developer running the suites also moves their own GUI installation's home.
    Two test classes already isolate themselves onto their own nodes (`ServerPackCreatorPathsConfigTest`,
    `ServerPackCreatorScriptTemplatesConfigTest`), so the pattern exists — it is just not applied suite-wide.
    Making the node name injectable (tests and the grinder each on their own node) is the real fix; **not yet
    decided/implemented** — see the open question in `claude-docs/REFACTOR-LOG.md`.

- **Staging is reclaimed, not accumulated** (`BootWorkspaceReaper`). Each attempt stages a full server pack with the
  overlaid loader libraries under `<work>/verify/boot/<slug>-<loader>` plus downloaded jars under
  `<work>/verify/verify/<slug>-<loader>`, and staging only ever deleted a directory when that *same* `(slug, loader)`
  was retried — which during a catalog sweep is never. Measured 2026-07-30: **98 GB across 1750 attempt directories,
  ~23 GB/h**, enough to fill the host inside a day. The reaper strips each finished candidate's staging down to its
  `boot.log` (the verdict detail is read from it; the packs are reproducible), runs in a `finally` so a *thrown*
  verification is reclaimed too, and sweeps orphans at startup — first live startup reclaimed 8 897 MiB, taking the
  work tree from 8.7 GB to 155 MB. **Landmine:** it is scoped to one slug on purpose, matching `<slug>-<loader>` by
  cutting the loader suffix rather than prefix-matching the slug — workers run in parallel, and a prefix match
  (`jei` vs `jei-extras`) would delete the pack out from under a container that is still booting it.

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
  | `ScriptTemplateMatrixIT` | `GRINDER_TEMPLATE_IT=1` | the `spc-grinder-templates` image | 2026-07-29 |
  | `CatalogCrawlLiveIT` | `GRINDER_LIVE_IT=1` | network (Modrinth) | 2026-07-29 |
  | `CurseForgeCrawlLiveIT` | `GRINDER_CF_IT=1` | **plus** `CURSEFORGE_API_KEY` | 2026-07-30 |

  e.g. `docker pull busybox && GRINDER_DOCKER_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*DockerJavaContainerEngineIT"`
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
