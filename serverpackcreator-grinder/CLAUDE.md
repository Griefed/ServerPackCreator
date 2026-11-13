# serverpackcreator-grinder — module context

> Standalone fire-and-forget service that boot-verifies mods **at scale**, in **isolated, network-less
> Docker containers**, to accumulate a catalog-wide list of suspected-clientside mods. Package
> `de.griefed.serverpackcreator.grinder`. Depends on `serverpackcreator-clientside` (+ `-api`
> transitively) and **docker-java** (`docker-java-core` + `docker-java-transport-zerodep`, 3.7.1). No
> Spring, no Swing. Not published to Maven.

> **History, incidents and measurements behind the rules below are in `claude-docs/GRINDER-HISTORY.md`.**
> Read it when a rule's reason is unclear, or before re-opening a recorded decision or re-running a measurement.

## Package layout

The base package is the composition/orchestration core; `container`, `loader`, `report` and `source` are leaves
it wires together. **Dependencies point inward: subpackages never import the base package's orchestration, only
its domain models** (`GrindModels`). `GrinderApplication`'s fully-qualified name is the build's `mainClass`, so it
stays in the base package. The boot levers `PackVariables` and `ImageJavaRuntimes` live in `-clientside`'s `boot`
package so the CLI's `-verifyclientside` boots packs the same way.

**Audited against the root *Package structure* rule and left unchanged** — the three base-package residents that
look like strays are not:

- **`ContainerCandidateVerifier` cannot move into `container`**: it imports `container`, `loader` and `report`,
  and `loader` imports `container`, so it would create `container -> loader -> container`. It is the composition point.
- **`JsonLearnedModIds`** is composition-level daemon state (neither verdict nor crawl position); its KDoc argues this.
- **`BootWorkspaceReaper`** imports only `clientside.boot.AttemptDirectory`; a `workspace` package would hold one file.

Re-derive the edges with one grep over `^import de.griefed.serverpackcreator.grinder` per subpackage before
re-opening this.

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

- **Orchestration.** `Grinder.grind` verifies one candidate via the `CandidateVerifier` seam, skipping
  already-ground projects and *swallowing* a thrown boot so a bad mod cannot sink a worker, and records one
  `GrindVerdict` per **Minecraft version-line**. `GrindPool.grindAll` drains a popularity-ranked batch across N
  workers (N ≈ host-RAM / per-boot-memory). `VerdictStore` is keyed `platform + project + version-line`
  (re-verify replaces). **`CandidateVerifier` collapses the integration-bound boot pipeline**, so orchestration
  is unit-tested with fakes.
- **Pacing** (`GrindPacing.pauseAfterPass`, pure): work found → no pause; nothing due but catalog remains →
  `SPC_GRINDER_SCAN_DELAY`; sweep completed with nothing due → `SPC_GRINDER_INTERVAL`. A fixed per-pass sleep
  makes catalog coverage impossible. **Failed verifications deliberately don't count as work** — on a broken
  host every candidate fails, and counting that would race the cursor through the catalog unverified.

## Operator-facing logging (three surfaces, all live)

*Operator-facing documentation for these lives in `README.md` §7 — keep the two in step.*

- **`/status`** (`GrinderStatus` → `StatusSnapshot`, Jackson-serialized). Written by workers, read by HTTP
  threads, so `snapshot()` is a point-in-time **copy**. Absent optional collaborators render `null` rather than
  failing — a monitoring endpoint that 500s is worse than a thin one. Slugs are internet-supplied, so the
  document is *serialized*, never string-built.
- **One INFO line per candidate** in `Grinder.grind`; the fresh-skip is deliberately DEBUG so it cannot bury it.
- **Live per-boot console.** `ServerRunner.run`'s `onLine` sink: `BootVerifier.runPrepared` appends+flushes into
  the attempt's `boot.log` *during* the boot, `DockerLoaderInstaller` into `<tuple>/.spc-install.log`, so a hung
  boot is diagnosable before its timeout. **Landmine:** every write is wrapped — a failing sink or unwritable log
  must never fail a boot (pinned).
- **The learned mod-id map is persisted daemon state** (`JsonLearnedModIds`, `SPC_GRINDER_LEARNED_IDS`): mod
  **ids** → platform **refs**, same load/atomic-write contract as `JsonCursorStore`. **Pure cache — deleting it
  costs downloads, never correctness.** Written only when a pair is genuinely new.

## The grind axis is the Minecraft version-line

**A project is ground once per Minecraft version-line, under exactly one modloader** — the first of
`BootCandidateSelector.LOADER_PRIORITY` (`NeoForge, Forge, Fabric, Quilt, LegacyFabric`) that line publishes a
build for. **Why:** sideness is a property of a build, and builds differ far more across Minecraft eras than
across loaders of one era.

- **Which lines** is `MinecraftLinePolicy` (`-clientside`): newest N ∪ an anchor list
  (`SPC_GRINDER_MINECRAFT_LINES_NEWEST` / `SPC_GRINDER_MINECRAFT_LINE_ANCHORS`). "Every line" would break the rule
  that `SPC_GRINDER_REVERIFY_TTL_DAYS` must outlast a sweep. **A project always gets at least its newest line** —
  a candidate recording no verdict looks like an engine failure and would be re-selected every sweep for ever.
- **LANDMINE — the loader is not a row's identity, and the scratch directory knows it.**
  `AttemptDirectory.nameFor` is `<platform>-<slug>-<loader>-<line>` and `ownerOf` cuts **two** trailing parts.
  One loader routinely wins two lines, so a three-part name lets the second target wipe the first's pack and
  console mid-run. Keep `nameFor`'s part count and `ownerOf`'s `SUFFIX_PARTS` in lockstep or the reaper
  silently re-scopes.
- **LANDMINE — boot logs carrying the old three-part owner are unreachable from any row**;
  `SPC_GRINDER_BOOT_LOG_BUDGET_MIB` reclaims them. A console adopted by `adoptLegacy` records no Minecraft version,
  so it is attributable to no line — pinned deliberately; never invent one.
- **The store migrates per project as it is re-ground** (`supersededLoaderKeys`), by key **prefix** — hence the
  `mc:` marker in the key. `supersededLegacyKey` cannot be extended: several loader rows collapse into one line
  row. Never sweep on any other trigger — loader-keyed rows must not go before a replacement exists.
- **`suggestedEntry` stays the loader's whole history**, not the line's: `/as-properties` matches with
  `startsWith`, and `ClientsideVerifier.targetDisprovingTheCrash` compares entries across lines.
- **The crash guard the axis owes** lives in `-clientside`: a decisive crash re-checks even when metadata agrees,
  and `pickRecheckCandidates` spends its first attempt on the crashing era's **other loader**. Detail in
  `serverpackcreator-clientside/CLAUDE.md`.

## The audit grades evidence, so it has to know where the evidence lives

`GrinderAuditIT` asks a live daemon: *is every published `CONFIRMED` backed by a decision `BootDecision.decisive`
marks?*

- **It must build the owner tuple the current naming scheme uses** (including the line). A tuple matching nothing
  makes the audit assume-skip — **a green run that graded nothing is the worst outcome an audit has**; check for
  it first whenever a naming scheme moves.
- **It must grade inherited proofs at the sibling that owns the evidence.** `propagateClientOnlyProof` confers
  CONFIRMED from a sibling's console; the row's own console is usually a clean boot. `GrindVerdict.inheritedProofFrom`
  / `inheritedProofRule` (the `Inherited proof` column) carry it.

## Cross-cutting landmines (do not let these load lazily)

- **LANDMINE — a slow restart is neither the verdict-store load nor the reaper's traversal; both measured
  negative** (re-run `StoreWriteBenchmark.measureLoadCost` before re-arguing). `ReportServer.start()` comes last,
  so everything before it is dead time. `GrinderApplication.phase` logs each startup step's time
  **unconditionally** at INFO — read those lines; without them a slow start is **unattributed**. Remaining
  suspects: `ImageJavaRuntimes.from`, `BootLogStore` migrations, `engine.reapOrphans()`, a JVM stall (no `-Xmx`).

- **LANDMINE — "every endpoint 502s" does NOT mean the report is wedged.** A cheap endpoint failing does not
  distinguish "no `SPC_GRINDER_HTTP_THREADS` thread free" from "the request never arrived". Prove the request
  reaches the daemon before blaming it. Full triage: grinder README, *Report responsiveness* → *When the proxy
  cannot reach a healthy report*.
  - **On the host:** `curl -m 5 -o /dev/null -w '%{http_code} %{time_total}\n'
    http://127.0.0.1:$SPC_GRINDER_PORT/dashboard` — `/dashboard` is a constant, so a 200 exonerates the daemon.
    **No `pool-*` thread in `jcmd <pid> Thread.print` means no request ever reached a handler** (the pool creates
    workers lazily and never retires them).
  - **Proxy errno: 111/refused is a wrong bind, 110/timed out (~131 s) is a filter** — the known cause is the
    host's `Chain INPUT (policy DROP …)`. "No rule for this" ≠ "not filtering this" ≠ "`ufw status` allows this"
    ≠ "the kernel allows this": read `iptables -L INPUT -n -v` and `iptables -S ufw-user-input | grep <port>`
    (`ufw status` reads `/etc/ufw/user.rules`; `ufw reload` repairs a stale running chain).
  - **Scope the fix by subnet, never by port alone** (`ufw allow from 172.19.0.0/16 to 172.19.0.1 port 9090
    proto tcp`): with `SPC_GRINDER_HOST=0.0.0.0` the DROP policy is the only thing keeping the unauthenticated
    report off the internet (the `ufw deny in on eth0` rule matches nothing — the NIC is `eno1`).
- **Selection is cached per store version, because pagination does not bound it.** Selecting sorts all verdicts
  and gathers every filter column; page size bounds only rendering. `VerdictSnapshot` holds both derivations
  lazily and `VerdictSnapshotCache` rebuilds only when `VerdictStore.version` moves. **`version` is a counter,
  not a row count** — `record()` replaces by identity, so a re-ground project changes the report at equal size.
  - `select(list, …)` delegates to `select(snapshot, …)`, so `/`, `/export.csv` and `/verdicts.json` share one
    implementation — **a guard comparing the two overloads asserts nothing**. Assert outcomes.

These bite regardless of which subsystem you are in, so they stay in this always-loaded-for-the-module file even
though their detail lives deeper:

- **Never mount the Docker socket into a boot container.** A candidate mod is untrusted code; the socket is
  root-equivalent host access. (Also why there is no containerised grinder *daemon* image — see the README.)
- **Never wire the candidate-mod boot with network.** `--network none` is the whole isolation guarantee; only
  the one-off loader install per tuple gets network. Detail: `grinder/loader/CLAUDE.md`.
- **A mod must never be mis-scored as a clientside crash.** Guards: selection-time loader availability + Java
  support, the classifier's pre-launch setup-abort mapping, and three crash checks in `-clientside` (older cached
  loader build booted; crash contradicting declared server support → boot up to two other mod versions; a
  cross-loader pass refusing a crash when another loader with the same list-entry booted). See
  `serverpackcreator-clientside/CLAUDE.md`. **Pacing consequence:** one contradicting crash can hold a worker for
  up to three boot budgets — the price of not publishing a wrong exclusion; a genuine clientside mod costs one boot.
- **`installDist` is not rebuilt by `test`** — always `./gradlew :serverpackcreator-grinder:installDist`
  immediately before a live run, or you draw conclusions from a stale jar. Sanity-check the crawl-axis log line
  (newest entry a real Minecraft version) before trusting the run.

- **Configuration is `GrinderConfiguration`, the sweep is `GrindLoop`; `main` composes and hands off.**
  - **Landmine: a knob not in `GrinderConfiguration.KNOBS` is invisible to both documentation guards**
    (`ReadmeConfigurationTest`, `SystemdUnitConfigurationTest` iterate it).
  - **Landmine for `GrindLoop` tests:** `running` is polled *between steps*, so a counter-based fake stops
    mid-pass and proves nothing — flip the flag from the injected `sleeper`.
  - The source-text greps in the `*WiringTest`s and `GrinderSpcEnvironmentTest` stay: they assert joins inside
    `main`, which cannot be executed. They grep for `config.<property>`.

- **The daemon owns its own `Preferences` node — do not "simplify" that away.** SPC resolves its home through a
  `Preferences` node (`PathsConfig.homeDirectory`) whose getter **re-reads on every access**; on the shared
  machine-wide `ServerPackCreator` node any process booting an `ApiWrapper` (a test suite, the GUI) relocates a
  running daemon's home, and every boot then fails in a way that reads as `boot:none` "never bootable" verdicts,
  not as a broken host. `GrinderApplication` claims **`ServerPackCreator-grinder`** via
  `ApiProperties.PREFERENCES_NODE_PROPERTY` (before any `ApiProperties` exists, unless the operator chose a node)
  and logs which node it used.
  - Overrides: node `-Dde.griefed.serverpackcreator.preferences.node` / `SPC_PREFERENCES_NODE`; home
    `-Dde.griefed.serverpackcreator.home` (beats the dev-build working-directory fallback).
  - **The daemon also pins the home** (`pinSpcHomeDirectory`) to `SPC_GRINDER_HOME` unless the operator set the
    `-D` — otherwise a source build picks the working directory (`/` under systemd). The preference is consulted
    **before** cwd and `serverpackcreator.properties`, so `SPC_GRINDER_SPC_PROPERTIES` alone never protects.
  - Check the startup lines (`Using Preferences node …`, `Home directory set to: …`) rather than guessing. **Don't
    measure from outside:** macOS's Java `Preferences` plist is cached per process (~30 s flush), so cross-process
    snapshots lie. A stale shared-node value may still be stored if a GUI resolves a surprising home.

- **LANDMINE — the first `log.` call in `main` builds an `ApiProperties`, so every SPC decision must precede it.**
  `ApiProperties` is `@Plugin` and *is* log4j's `ConfigurationFactory`, so log4j instantiates one while
  initialising — with whatever node and home resolve then — and *persists* what it resolved. Both claims run as
  the first statements of `main`; keep new startup logging below them. Pinned by
  `GrinderSpcEnvironmentTest.theSpcEnvironmentIsClaimedBeforeTheFirstLogStatement` (source-ordering, since an
  initialised JVM cannot observe it).
- **LANDMINE — the report binds loopback by default, and it is unauthenticated. Both halves matter.**
  `ReportServer`'s `host` defaults to `127.0.0.1`; a containerised proxy dials over the Docker bridge gateway and
  gets refused at TCP (a 502 while an SSH tunnel works). Raise the bind via `SPC_GRINDER_HOST` to the *gateway
  address*, not `0.0.0.0`: `/`, `/status`, `/export.csv` and `/verdicts.json` answer unconditionally. Pinned by
  `ReportServerBindAddressTest` (real non-loopback IPv4; skips without one) and `ReportBindWiringTest` (the join
  in `main`). README §5 *Exposing the report* is the operator half.
- **LANDMINE — a container must run as the *owner of the directory it mounts*, not as the image's `USER`.**
  `docker/Dockerfile` bakes in `USER 1000:1000`; every boot and install bind-mounts a host-created directory, so
  a mismatch means the container reads the pack and writes nothing. **The failure names the wrong subsystem:**
  the start script dies later on `Error: could not open 'user_jvm_args.txt'`, which reads as a broken template;
  failure across every loader at once is the tell for a permission wall. `ContainerUser.forDirectory` resolves it
  (override `SPC_GRINDER_CONTAINER_USER`), logged as `containerUser=`; `InstallFailureDiagnosis` names it.
  **Corollary:** scan the whole console — a tail is the wrong slice whenever the first failure is survivable.
- **`/verdicts.json` is the machine-readable feed for the SPC grinder plugin.** `VerdictField`, `VerdictQuery`
  and `VerdictSelection` are `internal`, so the selection travels over the wire. It serialises `GrindVerdict`
  itself (so `stagedDependencies` stays an array), shares `VerdictSelection.select` with `/` and `/export.csv`,
  and is unpaged by default like the CSV. Detail and the shared-mapper landmine: `grinder/report/CLAUDE.md`.
- **`/as-properties` publishes the fallback clientside list, and only `Verdict.CONFIRMED` may ever reach it.**
  `ClientsideVerifier.verdictOf` reaches CONFIRMED only from a rung `BootDecision.decisive` marks, so the gate
  asks once — asking twice would let the two drift.
  - **A metadata declaration publishes nothing**, and an old-schema (`Confidence`) row loads as `INCONCLUSIVE` —
    every exclusion is earned by a boot's console.
  - Verdicts rank CONFIRMED, INCONCLUSIVE, ERROR, LOCKED, UNVERIFIABLE, CLEAR. `VerdictField` is the single
    declaration behind header, CSV, query, filter and sort; `everyVerdictHasARank` fails the build on an unranked
    verdict. **`ERROR` must never publish** (`VerdictPublicationTest`, twenty rows — the failure mode is a flood).
  - `Verdict.keepsLogs`: **CONFIRMED keeps its console** — a published exclusion must stay auditable.
    `GrinderAuditIT` parses the live CSV's `Verdict` column; update it with any schema change.

  `FallbackPropertiesRenderer` merges SPC's current list with every CONFIRMED verdict for an instance's
  `…configuration.fallback.updateurl`. `Properties.load(InputStream)` decodes **ISO-8859-1**, hence `\uXXXX`
  escaping and the one non-UTF-8 response. The floor is not a tunable. **Never point the grinder's own SPC
  instance at this endpoint** — findings would fold into its "shipped list" and never leave. `UpdateConfig`
  replaces client lists wholesale, so a grinder on an old build hands clients a *staler* list. Pinned end-to-end
  by `FallbackPropertiesConsumerTest`.
- **A unit-level `CPUQuota=` bounds the JVM and nothing else** — containers belong to the docker daemon's cgroup.
  `SPC_GRINDER_CPUS` (cores per container, default `2`, `0` = uncapped) is the only lever on boots. Floor it at
  ~1 core: startup is largely single-threaded, and a boot throttled past its budget scores INCONCLUSIVE, reading
  as a hanging mod. `SPC_GRINDER_MEMORY_GIB` is equally load-bearing (heap derivation and worker sizing). Detail
  in `grinder/container/CLAUDE.md`.
- **LANDMINE — a container is not in the unit's control group, so only the application can stop it.**
  `systemctl stop` never touches containers. The shutdown hook marks the engine closed, `docker stop`s each in
  flight with `SHUTDOWN_GRACE` (15 s SIGTERM-then-kill, 8 at a time), then gives workers the rest of the window via
  `GrindPool.awaitStop`. `requestStop` alone cannot end a shutdown (its flag is read only between candidates; the
  interrupt wakes a worker mid-boot). `TimeoutStopSec` must stay above the window (arithmetic in the unit's
  comment). Workers are **threads** — the JVM exiting is the force. Signal-before-removal is proven by
  `DockerJavaContainerEngineIT` (gated `GRINDER_DOCKER_IT=1`).
- **LANDMINE — a host defect is published as thousands of verdicts about mods unless something stops it.** An
  **environment defect looks exactly like a subject defect unless something distinguishes them**, and the
  per-tuple install cooldown disguises one host-wide failure as many per-tuple ones. `docker system prune -a`
  removes `spc-grinder-runtime:latest` (it is in use only during a boot), so `RuntimeImagePreflight` refuses to
  start (`main`, right after the engine is built, exit 1 so `Restart=on-failure` retries and `systemctl status`
  shows `failed`); `ContainerEngine.hasImage` defaults to `true` so fakes are unaffected. **Recovery after an
  outage is `--requeue-since <the moment it broke>`** — `--requeue-before` selects its exact complement.
- **Every container carries two labels, and the difference is the whole point.** `OWNER_LABEL` = "a grinder made
  this"; `INSTANCE_LABEL` = "*this* engine made this" (`DockerJavaContainerEngine.instanceId`). Any ownership
  question to the daemon must use the second. `reapOrphans()` at startup is the sole recovery from a SIGKILLed JVM
  and skips its own instance.
  - **Not fixable with a label:** a live second engine's container is indistinguishable from a dead process's,
    and a pid does not settle it across pid namespaces. **Two grinders sharing a daemon must be kept apart by
    whoever starts them** — the unit is a singleton, and `.forgejo/workflows/grinder-container-it.yml` holds a
    repository-wide concurrency lock.
  - **A defect fails the same test in both of two concurrent runs; different tests failing is interference.**
- **Never hand SPC a *relative* properties file — a loaded one becomes a permanent write target.**
  `PropertyStore.loadProperties` tracks every file it reads and `save()` writes to all of them; `ApiProperties`'
  default is the relative `serverpackcreator.properties`. `GrinderApplication.resolveSpcPropertiesFile` always
  returns an absolute path (`SPC_GRINDER_SPC_PROPERTIES`, else inside the daemon's home), pinned by
  `GrinderPropertiesResolutionTest`. **Expected and harmless:** a save into the dist's
  `build/install/.../lib/serverpackcreator.properties`, and `Loaded properties from …` appearing twice
  (`PropertyStore.save` collects into a `TreeSet<File>`, so it is written once).
- **The loader step-down covers Fabric and Quilt too** (`CachedLoaderVersions`, `knownLoaderVersionsNewestFirst`
  returns the whole loader line). Prevention is ruled out — every published source calls a failing pairing valid,
  so **no pre-check predicts an installer that fails to run**. `LoaderStepDown.newestFirst` filters nothing:
  Quilt's metadata marks betas as release, and a pre-release filter could empty the line. `latestVersion` stays
  truthful so the support gate and crash re-check measure against the real newest.

- **A cached install is a product of the templates that built it** (`TemplateProvenance` + the marker's
  `templates=` key). A **differing** digest is a miss; an **absent** one is tolerated with one warning per run,
  because treating unknown as different would reinstall every tuple (~150 MB and a networked boot each).
  **Check before invalidating** — a template change does not necessarily make cached layers unbootable.
- **"Not applicable" must never mean "we could not find out."** `ImageJavaRuntimes.supportFor` distinguishes
  `JDK_NOT_BUNDLED` (permanent exclusion) from `REQUIREMENT_UNKNOWN` (also what a *failed* manifest download
  yields via `MinecraftServer.javaVersion()`). Collapsing them silently drops the newest Minecraft from
  `ScriptTemplateMatrixIT`, so the IT fails the unknown case, and `26.2` stays in the default axis to exercise
  the `YY.x` scheme.
- **Staging is reclaimed, not accumulated** (`BootWorkspaceReaper`): a finished candidate's staging under
  `<work>/verify/` goes **entirely**, in a `finally`, and orphans are swept at startup. No `boot.log` stays in
  staging — `bootArtifactSink` already copied it to `BootLogStore` (`leavesNoAttemptDirectoryBehind` asserts on
  the staging **root**).
  **Landmine — scope is one `(platform, slug)`.** Names are built and parsed only by `AttemptDirectory`; cut the
  suffix rather than prefix-match (`jei` vs `jei-extras`), and keep the platform, because **the same slug on
  Modrinth and CurseForge is two candidates ground in parallel** — a bare-slug reap deletes the other's pack
  mid-boot and manufactures false crashes.
  **Landmine — reap the identity the staging was *named* from:** `ContainerCandidateVerifier.reapTarget` prefers
  the resolved report's `ProjectFiles.platform`/`slug` (candidate only when verification threw); the two can
  disagree, and the wrong one leaks a pack per attempt.
- **Every non-survived attempt's evidence outlives its staging** (`BootLogStore`,
  `ContainerCandidateVerifier.keepAttemptArtifacts`, fired per attempt by `BootVerifier`'s `bootArtifactSink`):
  the container console plus the server's `logs/` and `crash-reports/`, for every boot that did not SURVIVE —
  a wrongly **cleared** mod must leave evidence too. Bounded by `pruneExcept` per tuple and
  `SPC_GRINDER_BOOT_LOG_BUDGET_MIB` (default 2048) overall; concurrency landmine in `grinder/report/CLAUDE.md`.
  - **Under the *home*, not under `work/`** — everything below `work/` is scratch the reaper may reclaim.
  - **LANDMINE — the log name is untrusted input.** The report has no authentication and is reverse-proxyable.
    `read` requires a plain file name resolving directly inside the store — checked on the string before the
    filesystem is touched, then confirmed canonically against symlinks — and a refusal is indistinguishable from
    an absent log. Two tests pin it; do not "simplify" it to `File(dir, name)`.
- **The immediate re-grind queue is how a *defect in the engine* gets un-published** (`RequeueStore`,
  `RequeueSelection`, `Grinder.grind(force)`), drained at the **start of every pass**, before the catalog slice.
  **LANDMINE — the drain must stay `force = true`**: a queued verdict is usually a recent one, so an unforced
  drain becomes `SKIPPED_FRESH` and looks like it worked. Pinned by `aForcedGrindReVerifiesEvenAFreshVerdict`.
  - Selectors: `--requeue <url>…` (a handful), `--requeue-before <ISO instant>` (engine defect, the past is
    suspect), `--requeue-since <ISO instant>` (an outage window). The instant selectors are mirrors that partition
    the store (`-since` is inclusive) — **picking the wrong one queues exactly what you did not mean**. One
    candidate per project (platform + platform id where known).
  - **LANDMINE — the CLI path runs *before* `claimSpcPreferencesNode()`/`pinSpcHomeDirectory()` and must never use
    `log`.** It runs against an already-up service: claiming would move the home under the running daemon, and
    the first `log` constructs `ApiProperties`. It prints to stdout. `theRequeuePathRunsBeforeTheClaimsAndNeverLogs`
    reads the helper's source too, because `main`'s body cannot see a log call inside the helper.
  - **Not an HTTP endpoint, on purpose** — an unauthenticated write endpoint would let anyone schedule unbounded
    container work.
  - `/status` reports `requeued`; a queued grind logs `(re-grind requested)`.
- **The report links every endpoint and each row's console.** `VerdictReportRenderer.toHtml` takes a per-row
  *lookup* for the log name, not a `GrindVerdict` field, so a link appears exactly when the file exists.
- **The Project cell links only `http`/`https`** and renders any other scheme as escaped text: HTML-escaping
  stops attribute breakout but leaves `javascript:` a working href, and `projectUrl` is operator- or
  platform-supplied on an unauthenticated server.
- **`deploy/install-grinder.sh`'s mode is its uid, not a flag**: as a user it builds and installs the checkout it
  lives in; as root it clones a branch and re-runs *the clone's copy* as an unprivileged build account (a Gradle
  build as root leaves root-owned files in `build/`). Keep the name — a deployed copy invokes it **by path**. It
  has no harness: verify by running it (shellcheck `-S style`).

The boot path sets `$JAVA` per Minecraft version (via the pack's `variables.txt`) from SPC's declared required
Java — **no Java download**, which keeps mod boots runnable under `--network none`. The template needs `bash`,
`curl`/`wget`, `gawk`, `tar`/`gzip`. See `docker/README.md`.

**Host prerequisites:** the download/resolve phase runs on the **host** (in `BootVerifier.prepareBootPack`), not
in the boot container, so the box running the grinder needs:
- **`CURSEFORGE_API_KEY`** env var — `clientside.supportedPlatforms()` only registers CurseForge when the key is
  present (Modrinth needs no key).
- **No browser, and no Playwright.** Distribution-locked CurseForge files (`allowModDistribution=false`,
  `downloadUrl=null`) are **not obtainable** — the author opted out. Such a candidate is reported unverifiable
  with a refusal naming the lock. Do not reintroduce a browser route; detail in
  `serverpackcreator-clientside/CLAUDE.md`.

- **The report carries two patterns per row, `Name-pattern` and `Filename`, and neither replaces the other.**
  `Name-pattern` is `suggestedEntry`, the common prefix over the project's whole history, and is what
  `/as-properties` publishes — it must stay broad enough for `startsWith` against every build. `Filename` is the
  one artifact the verdict sampled (a Quilt row showing `iris-fabric-` is correct: Quilt boots Fabric builds).
  A column is one `VerdictField` entry, never five edits. **Do not swap the published one for the narrow one** —
  landmine in `serverpackcreator-clientside/CLAUDE.md`.
- **LANDMINE — a cache-hit check must ask [LoaderCache.isInstalled], never `markUsed` alone.** `isInstalled`
  compares the template digest in a tuple's marker against the current one; `markUsed` only checks the marker
  exists and stamps its mtime. A hit is `isInstalled(...) && markUsed(...)` — the first decides whether the layer
  may be served, the second keeps it alive against `evictUnusedSince`.
  - Template changes fail **silently** (a stale layer yields a plausible verdict), so this is pinned through
    `ensureInstalled` by **installer call count** (`ProvenanceReachesEnsureInstalledTest`); a marker-based
    assertion passes against broken code.
  - Accepted: `templateProvenance()` is evaluated per lookup, and a rebuilt tuple logs its mismatch twice (fast
    path and under the lock) — a second silent predicate is how the metadata scanners drifted.
  - **When a mechanism exists to change a decision, pin the *decision*, through the call the daemon actually
    makes** — a correct unit no caller reaches is a recurring defect class here.
- **`/status`'s `verified`/`failed`/`skippedFresh` are per-pass; `beginPass` resets them.** `uptimeSeconds` and
  `startedAt` stay lifetime and are pinned as such, so the reset cannot grow to cover them.
- **A knob the daemon cannot use falls back like one it cannot parse.** `GrinderConfiguration.from` never throws:
  `intIn`, `longAtLeast`, `capAtLeastZero` coerce unusable values (e.g. `SPC_GRINDER_WORKERS=0`,
  `SPC_GRINDER_INTERVAL=-1`, `NaN`/`Infinity`) to the documented default, rather than dying mid-run in a
  `require` that names an internal parameter.
  - **Boundaries that mean something are inside the allowed range:** port `0` (any free port), `0` cores or GiB
    (uncapped), a `0` log budget (keep nothing), zero/negative flush interval (write-through).
  - **`everyVariableReadIsDeclaredAsAKnob`'s regex alphabet is explicit and must stay so** — a regex matching any
    quoted-name call would match the `Knob("SPC_…")` declarations and assert nothing. Add a reader to the
    alphabet when you add one.
- **LANDMINE — every wait in the shutdown path is budgeted, including the container drain.**
  `stopContainerCmd.withTimeout(...)` bounds Docker's internal SIGTERM-to-SIGKILL window, not the HTTP call, so an
  untimed `Future.get()` on a wedged socket holds the hook until `TimeoutStopSec`'s SIGKILL orphans the
  containers. `DockerJavaContainerEngine.close()` waits through `awaitWithin(pending, shutdownGrace)`: **one budget
  across the whole set, never one per task**. A still-running task is left to `shutdownNow`; its container keeps
  the owner label for `reapOrphans`. Pinned as a pure helper.

## Testing

- **Everything offline by default.** Orchestration, sources, crawl/cursor, partition plan, pacing, report and the
  pure loader pieces are unit-tested with fakes (`GrindTestFixtures.kt` holds the shared builders).
- **Integration-only, no offline double exists:** `DockerJavaContainerEngine`, `DockerLoaderInstaller` and
  `ApiVanillaPackGenerator` need a live daemon, the runtime image and a real `ApiWrapper`. If you change them,
  verify against Docker; the unit-testable orchestration deliberately sits *behind* their seams.
- **Gated integration tests** (skipped on a normal run — each needs something CI has not got):

  | Test | Gate | Also needs |
  |---|---|---|
  | `DockerJavaContainerEngineIT` | `GRINDER_DOCKER_IT=1` — set by `grinder-container-it.yml`, **not** by `test.yml` | a daemon + `docker pull busybox` |
  | `ContainerOwnershipIT` | same gate, same workflow | the same |
  | `ScriptTemplateMatrixIT` | `GRINDER_TEMPLATE_IT=1` | the `spc-grinder-templates` image |
  | `CatalogCrawlLiveIT` | `GRINDER_LIVE_IT=1` | network (Modrinth) |
  | `GrinderAuditIT` | `GRINDER_AUDIT_IT=1` | network + a **live grinder** (`SPC_GRINDER_AUDIT_URL`) |
  | `CurseForgeCrawlLiveIT` | `GRINDER_CF_IT=1` | **plus** `CURSEFORGE_API_KEY` |

  e.g. `docker pull busybox && GRINDER_DOCKER_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*DockerJavaContainerEngineIT"`

  `grinder-container-it.yml` holds a concurrency group with **no ref in it**, so one run of the container ITs
  happens at a time repository-wide. `test.yml` must **not** set the gate: its group is per-ref, and one of these
  tests reaps every grinder container on the shared daemon. The other gates stay unset on purpose (live APIs, a
  built image plus Minecraft downloads, or a deployed grinder).

- **Measurements are a *program* in the `benchmark` source set**, not a test: `StoreWriteBenchmark`, run by a
  `JavaExec` task (`./gradlew :serverpackcreator-grinder:benchmark`). It asserts no timings but `check(...)`s its
  own fixture. **LANDMINE — every task of type `Test` is pulled into `check`** (by type, declared nowhere), so a
  second measurement must be a program too. The source set extends `implementation`/`runtimeOnly`, and Kover
  needs `excludedSourceSets` or it counts as production code.
- **`GrinderAuditIT` grades a live daemon's published verdicts against their own evidence**, because
  sample-and-fix censuses were never committed and so never re-checked.
  - Failure prints the distribution by decision, so a new defect shows up as a bucket.
  - **`/boot-logs` is HTML-only**, so names are scraped from its `?name=` hrefs; the tuple is everything before
    the first `~` (slugs and loaders contain `-` but never `~`).
  - It assumes a **non-zero exit** when re-classifying (the exit status is not published), which keeps the
    exit-code rung reachable and therefore counted.
  - Politeness is part of the contract: gated, capped by `SPC_GRINDER_AUDIT_SAMPLE` (200), sequential, and never
    fails over one missing artifact.
- **The live ITs are the source of platform facts.** Each prints `[live]` lines with measured numbers (catalog
  sizes, slice sizes, saturation, ordering) — read those rather than numbers written down anywhere, and re-run
  them after touching paging, the cursor, or the partition plan.

## Status & what remains

**Continuous mode + crawl cursor.** With no project-URL args `GrinderApplication` loops: each pass takes the next
catalog slice from `CatalogCrawler`, grinds what is stale, and persists verdicts *and* the crawl position after
every step, so a restart resumes mid-catalog. Env vars and defaults are in `README.md` §5 (pinned by
`ReadmeConfigurationTest`). **Sizing gotcha:** a sweep is `catalog ÷ batch × pass-duration`, so
`SPC_GRINDER_REVERIFY_TTL_DAYS` must be **longer than a sweep takes**, or the tail is never reached.
**Loader availability is gated at selection:** `LoaderVersionResolver.latest` returns `null` for a Minecraft a
loader does not support; the classifier's setup-abort INCONCLUSIVE mapping remains the backstop.

**Script-template matrix.** `ScriptTemplateMatrixIT` (gated `GRINDER_TEMPLATE_IT=1`) boots generated start
scripts across `{MC} × {loader} × {shell}` in the `spc-grinder-templates` image; `N/A` cells are the
`LoaderVersionResolver` gate filtering correctly. It never runs in CI, so `-api`'s `ScriptTemplateContentTest` pins
fish's `string split --no-empty` in `runJavaCommand`. Results are point-in-time — re-run rather than trust a table.
**Verified NOT a bug — `cleanServerFiles`' comma split** in `default_template.fish` has no `--no-empty`, and both
shells hand an empty token to `find -maxdepth 1 -name ""`, which matches nothing. Don't "fix" it.

**Landmine — `.ps1` CANNOT be boot-tested on Linux.** The PowerShell template shells out to Windows **`CMD /C`**
(Java-version detection, server launch, bit check), so a Linux run fails as a platform mismatch, not a template
defect (a `pwsh` boot also needs `HOME` on a writable mount). PowerShell coverage is in `-api`:
`ShellTemplateSyntaxTest` (PowerShell's own `Parser::ParseFile`) and `PowerShellInstallerJavaTest`, both running
on every push. **No PowerShell check lives here**; don't add a `pwsh` boot cell — `scriptFor` rejects it with the
reason.

**Modloader installers run on `JAVA_INSTALLER`.** All three templates run installers through
`runInstallerJavaCommand` / `RunInstallerJavaCommand`, using the **optional** `JAVA_INSTALLER` from
`variables.txt` and falling back to `JAVA` (one JDK cannot satisfy both a modern installer and an old server).
`ImageJavaRuntimes.installerJavaPathFor(minecraftVersion)` returns the override **only when the server's Java is
older than** `MINIMUM_INSTALLER_JAVA` (17), so modern Minecraft exercises the plain-`JAVA` fallback every real pack
takes. Do **not** "simplify" to an unconditional `installerJavaPath()`. **Landmine:** each boot path writes its
own variables (installer, verifier overlay, matrix IT) — a new boot path must pass `installerJavaPath` too, or
Quilt-on-old-MC silently breaks.

**Landmine — do not raise `SPC_GRINDER_TEMPLATE_WORKERS`** (default **1**). Each cell boots a real server capped
at 3 GB; parallel cells starve the host and produce spurious SIGKILLs (`Killed "$JAVA"`). Judge no cell from a
parallel run.

**Shutdown.** An interrupt while `main` is parked (`GrindPool.grindAll`'s `Thread.join()`, the one-shot
`CountDownLatch.await()`) is caught, not thrown (`anInterruptedPassStopsInsteadOfThrowing`). **Any new park/join in
`main` must do likewise.** A missing final "Grinder stopped" line on SIGTERM is normal.

Remaining:

1. **A full CurseForge sweep is unproven** — nobody has watched the crawl walk every version to the end.
2. **Store dedup is project-identity:** `verdictKey` (via the shared `identityKey()`) uses the immutable
   `projectId`, falling back to the slug; an identified verdict supersedes the id-less row.
3. **The CurseForge API key lives in the macOS Keychain on Griefed's machine** (`security find-generic-password -w
   -s spc-curseforge-key`), deliberately never in a file or transcript. The grinder reads only
   `CURSEFORGE_API_KEY` from the environment (no dotenv anywhere), so pass it per command. It rides in the
   `x-api-key` **header** and `JdkHttpFetcher` logs nothing.
4. **Residual CF gap by design** — a (version, category, loader) slice >20 000 mods loses its middle (logged with
   a count; no narrower filter exists), and a mod with *neither* a loader tag nor a category is unreachable beyond
   its version's cap.
5. **Modrinth's offset ceiling is 99 999** — if the catalog ever exceeds 100 000 mod projects **the tail silently
   looks like the end of the catalog** and the crawl wraps early.
