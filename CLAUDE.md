# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable, *current-state* context for Claude Code sessions on
> ServerPackCreator. Read it before touching code.
>
> - Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`.
> - **Deferred-but-agreed work** → `claude-docs/BACKLOG.md` (why it waited + context to pick it up cold).
> - Module-specific facts, patterns and landmines → each module's own `CLAUDE.md`
>   (lazy-loaded by Claude Code when you work in that module).
> - Personal working preferences (general approach, organization, no-shortcuts ethos,
>   communication style) → `~/.claude/CLAUDE.md` (user level, applies to all projects), so they
>   don't ship in this public repo's shared file. **See that file before advising.**
>
> **Engineering principles (binding):** KISS, MVC, TDD, SOLID. Operational naming, documentation,
> module-boundary and Kotlin rules are in **## Conventions** below — follow them for all code.

---

## What is ServerPackCreator?

ServerPackCreator creates a server pack from any given Forge, Fabric, Quilt, LegacyFabric and
NeoForge Minecraft-modpack.

It is a Kotlin application and API: the API lives in `serverpackcreator-api`, the application in
`serverpackcreator-app`, the SPA in `serverpackcreator-web-frontend`.

---

## Module map

Each in-build module has its own `CLAUDE.md` with the details — the entries below are the map only.

- **serverpackcreator-api** — core library, published to Maven Central. Packages: `config`,
  `serverpack`, `modscanning`, `versionmeta`, `plugins` (pf4j API), `utilities`, plus `ApiWrapper`
  (composition root), `ApiProperties`, `ApiPlugins`. **Plugins compile against this module — its
  public surface is a compatibility constraint.** See `serverpackcreator-api/CLAUDE.md`.
- **serverpackcreator-clientside** — the clientside-mod verification engine (platforms, metadata +
  server-boot signals, downloaders, fallback-list editor). Depends only on `-api`; **not** published
  to Maven, so it churns freely. Reused by the app's CLI verbs and the planned grinder service. See
  `serverpackcreator-clientside/CLAUDE.md`.
- **serverpackcreator-app** — four apps in one module under `de.griefed.serverpackcreator.app`:
  `cli`, `gui` (Swing), `web` (Spring Boot 4 backend), `updater`. Entry point `ServerPackCreator.kt`
    + `Mode.kt`. See `serverpackcreator-app/CLAUDE.md`.
- **serverpackcreator-plugin-example** — pf4j example plugin exercising every extension point.
  Documentation-by-example: must always reflect current API idiom. See
  `serverpackcreator-plugin-example/CLAUDE.md`.
- **serverpackcreator-web-frontend** — Quasar 2 / Vue 3 SPA, JavaScript (TS migration planned),
  Pinia stores, built into the app's web backend via the org.siouan frontend Gradle plugin. See
  `serverpackcreator-web-frontend/CLAUDE.md`.
- **serverpackcreator-grinder** — standalone fire-and-forget service that boot-verifies mods at scale
  in isolated, network-less **Docker containers** (docker-java). Depends on `-clientside` (+ `-api`);
  no Spring/Swing; not published. Foundation stage: the container-backed `ServerRunner`. See
  `serverpackcreator-grinder/CLAUDE.md`.
- Not in the Gradle build: `serverpackcreator-help` (docs), `buildSrc`, `docker`, `misc`.

## Build & test commands

- `./gradlew build` — full build. The app build depends on the frontend build and license report, and
  since 2026-08-14 it also **runs the frontend's Vitest suite** (`checkFrontend` → `npm run test`). Before
  that, `checkScript` was unset, so the plugin SKIPped `checkFrontend` and a green `build` had executed
  zero frontend tests while still compiling and bundling the SPA.
- `./gradlew :serverpackcreator-api:test` — API suite (runs against fixture modpacks in
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`). **Offline for every Minecraft version in
  the shipped manifest snapshot**, which `ApiWrapper.setup()` seeds from the jar; a version newer than that snapshot
  costs one fetch of its `mcserver/<version>.json`. The snapshot currently lags its own parent manifest (backlog
  B25). The test home is wiped before each run **except** `manifests/`, so that cache persists and accumulates.
- `./gradlew :serverpackcreator-app:test` — app suite.
- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
  `<module>/build/reports/kover/`.
- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
  `npx quasar build` for production build, `npm test` (Vitest).
- Run the app locally: `./gradlew :serverpackcreator-app:bootRun` (GUI by default; CLI/web via args,
  **not `:run`** — `-app` applies the Spring Boot plugin, not `application`, so `run` does not exist there;
  `:serverpackcreator-grinder:run` does, because the grinder applies `application`; see `Mode.kt` /
  `CommandlineParser.kt` for the arguments).
- `media` task needs install4j installed locally — not part of regular dev loop.

### Build layout (durable — where things are declared)

- **Repositories are declared once**, in `settings.gradle.kts` under `dependencyResolutionManagement`,
  with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` — a project-level `repositories { }` is a build
  failure, not a silent override. They were previously in 13 places. `buildSrc/build.gradle.kts` keeps
  its own because it is a **separate build** and cannot read the root settings; it deliberately does
  **not** list `mavenLocal()`, which used to be first there and let a stale `~/.m2` artifact shadow the
  real one.
- **The foojay toolchain resolver is declared TWICE, differently, and both are required.**
  `settings.gradle.kts` has it `version "0.8.0"`; `buildSrc/settings.gradle.kts` has it **without** a
  version. buildSrc is a separate build and does **not** inherit the root's toolchain repositories
  (verified — it fails with *"Toolchain download repositories have not been configured"*), yet by the
  time its settings evaluate the plugin is already on the classpath, so requesting a version there
  fails with *"already on the classpath with an unknown version"*. Don't "tidy" either one away.
- **Versions live in `gradle/libs.versions.toml`** — `[versions]`, `[libraries]` (49) and `[plugins]`
  (12). Do not re-add a hardcoded coordinate to a module build file.
  **Plugins are consumed by two different routes, and only one of them works everywhere:**
  - a *real* build script (`build.gradle.kts`, a module's own) uses `plugins { alias(libs.plugins.x) }`;
  - a **precompiled script plugin** (`buildSrc/src/main/kotlin/*.gradle.kts`) **cannot** — `alias(...)`
    there fails at `:buildSrc:compilePluginsBlocks` with `Unresolved reference: libs`. Verified by
    trying it, not assumed. Those apply a versionless `id("...")`, and the version arrives from the
    plugin **marker** (`<id>:<id>.gradle.plugin:<version>`) that `buildSrc/build.gradle.kts` puts on
    its own compile classpath via `libs.plugins.x.marker()`.

    Either route reads this one file, so a plugin's id and version are declared exactly once. Before
    2026-08-16 buildSrc depended on plugin *implementation* artifacts under `[libraries]`
    (`kotlinGradlePlugin`, `dokka`, …) while the convention plugins named the plugin *id* — two
    unlinked strings per plugin. Converting to markers is behaviour-preserving; measured, the
    flattened buildSrc compile classpath gained only the marker POMs and **lost
    `org.jetbrains.dokka:javadoc-plugin`**, which the `org.jetbrains.dokka-javadoc` marker does not
    depend on. That artifact turned out to be unnecessary: a from-scratch `dokkaJavadocJar` still
    produces 467 files / 356 HTML pages. Check that jar if you touch dokka wiring — `-api`'s javadoc
    is **published to Maven Central**, and the task reports success either way.
  - `settings.gradle.kts` cannot use the catalog in its own `plugins { }` block (it is evaluated
    before the catalog exists), which is why the foojay resolver keeps a literal version there. `buildSrc/settings.gradle.kts` points at the same file
  explicitly: buildSrc does **not** inherit the root catalog (verified on Gradle 8.14.4 — removing the
  block fails with `Unresolved reference: libs`).
  **Everything Kotlin is ONE `kotlin` entry (2.4.10) — keep it that way.** The compiler plugin, the
  allopen/jpa/spring compiler plugins and the stdlib/reflect/test libraries all read `version.ref =
  "kotlin"`. JetBrains versions these together, so a split only ever produces skew: until 2026-08-16
  this was four entries (`kotlin`, `kotlinAllOpen`, `kotlinJpa` on 2.3.20; `kotlinLibs` on 2.4.10),
  which meant `-api` compiled with a 2.3.20 compiler against a 2.4.10 stdlib. That combination did
  work — but it is the same *shape* as the coroutines failure below: a compiler reading metadata from
  a newer library fails hard with *"binary version of its metadata is X, expected Y"*, and nothing
  warns you as the gap widens. Do not re-split it to bump libraries without the compiler.
  Unifying was measured, not assumed: compiler warnings **243 before, 243 after**, the only delta
  being one warning the newer compiler rewords in place (`ServerPackCreator.kt:164:95`, elvis
  operator); 741 tests green; `bootJar`, `dokkaJavadocJar` (356 HTML pages), `sourcesJar` and
  `generateLicenseReport` all still succeed. Note the compiler version binds **Gradle** only —
  IntelliJ analyses with its own bundled Kotlin plugin, so an IDE older than the catalog can report
  metadata errors the command line does not.
- **Only `-api` publishes.** `serverpackcreator.publishing-conventions` is applied by that module
  alone, matching CI (`.gitlab-ci.yml` runs four `:serverpackcreator-api:publish...` invocations and
  nothing else). Non-api modules produce no sources/javadoc jar and run no `signing`. Do not move this
  back into `java-conventions`.
- **Convention plugin graph:** `java-conventions` (toolchain, test isolation, jar manifest) ←
  `kotlin-conventions` (Kotlin + Kover) ← `application-conventions` (= kotlin + spring);
  `spring-conventions`, `dokka-conventions`, `quasar-conventions` and `publishing-conventions` are
  applied on top as needed.
- **No cross-project configuration in the root build.** `allprojects { }`,
  `evaluationDependsOnChildren()` and `project("x").tasks.y.get()` are gone. A module that needs to run
  after another declares it itself, by task **path** (`-app`'s
  `mustRunAfter(":generateLicenseReport", ":serverpackcreator-web-frontend:build")`) — a string path
  resolves lazily, reaching into another project's task container forces it to be evaluated. The
  example-plugin jar is consumed as an artifact (`pluginArtifact`, a consumable configuration on
  `-plugin-example`) rather than dug out of `childProjects[...]`, which is what removed the build's last
  `!!`. Do not re-introduce any of the four.
- **Configuration cache is NOT enabled, and step 5 above is not what is blocking it** — measured, because
  this was claimed and was wrong: `build --dry-run --configuration-cache` reported the *same* 20 problems
  (13 unique) before and after the cross-project work, and configuration time was ~4.95 s either way.
  Those constructs block project **isolation**, a different feature. The 20 problems are:
  - `:generateLicenseReport` holds a `Project` reference — **third-party** (jk1 gradle-license-report),
    not fixable here.
  - every module's `test` and `processTestResources` "cannot serialize Gradle script object references" —
    **ours**: the `filter { }` in `processTestResources` and the `doFirst { cleanup() }` in `test`, both in
    `java-conventions`, capture the enclosing script; `-app`'s `test.doFirst` additionally captures
    `projectDir`.
  So the ceiling without excluding `generateLicenseReport` is "fewer problems", not zero. Fixing our own is
  a real, separate piece of work; do not start it expecting the cache to switch on at the end of it.
- **LANDMINE — Boot's BOM is a `platform()`, never `io.spring.dependency-management`. Do not "restore"
  that plugin.** Boot's BOM manages far more than Spring — verified in 4.0.2's BOM: `kotlin.version`
  2.2.21, `kotlin-coroutines.version` 1.10.2, `log4j2.version` 2.25.3, `jackson-2-bom.version` 2.20.2,
  `jackson-bom.version` 3.0.4, `junit-jupiter.version` 6.0.2, `mongodb.version` 5.6.2, i.e. most of what
  this project pins for itself. `io.spring.dependency-management` applies those as **forced** versions
  that beat every transitive request, so each catalog bump upgraded the other modules and was silently
  reverted in `-app`. That is not a warning and not a build failure — it surfaces as a
  `NoSuchMethodError` the first time the newer API is *called*. It cost 16 app tests on the coroutines
  1.11.0 bump (`BuildersKt.runBlockingK`, renamed in 1.11.0, absent from the 1.10.2 the BOM forced),
  while `./gradlew compileKotlin` was green in every module.
  Since 2026-08-16 `serverpackcreator.spring-conventions` imports the BOM as a Gradle `platform()`,
  whose versions are ordinary constraints that lose to a higher request — the catalog wins, Boot still
  versions everything we do not pin. Measured `-api` vs `-app` on shared coordinates:

  | Configuration | Differing before | Differing after |
  |---|---|---|
  | `runtimeClasspath` | 13 of 79 | **0 of 79** |
  | `testRuntimeClasspath` | 31 of 101 | **3 of 102** |

  The three survivors are `-app` resolving *higher* (byte-buddy 1.18.10, asm 9.7.1) from test
  dependencies `-api` lacks — correct conflict resolution, not drift. **Two related traps:**
  - The BOM coordinate comes from the catalog's `springBoot`, **not** `SpringBootPlugin.BOM_COORDINATES`,
    which is the *Gradle plugin's* version (`springGradle`). Those had drifted to 4.0.2 vs 4.1.0, leaving
    Boot internally inconsistent — `spring-boot` at 4.0.2 while `spring-boot-starter-web` was 4.1.0.
  - A platform only out-ranks what the module actually *requests*. `-app` got mockk only transitively
    from springmockk (1.14.6), so the catalog's 1.14.11 never applied and `-api`'s comment claiming the
    build is mockk-single-versioned was false. `-app` now declares `libs.mockk` explicitly. Bumping a
    library that reaches a module **only transitively** still needs an explicit declaration there.
- **LANDMINE — never do filesystem work in a task's configuration block.** `-api` shipped its
  root-level documents with fifteen bare `copy { }` calls inside `tasks.processResources { }`, so they
  ran when the task was *configured* — including on runs where `processResources` was UP-TO-DATE and did
  nothing — with no inputs, no outputs and no caching, writing into two source trees. They are now the
  `shipRootDocuments` / `shipWritersideDocuments` / `shipWritersideImages` Copy tasks. Making them
  visible immediately surfaced a real undeclared dependency (`sourcesJar` packages what
  `shipRootDocuments` writes), which had been ordering by luck.

## Branching & git workflow

- PRs target **`develop`**; `main` is the release branch.
- One branch per feature/fix, prefixed `claude-` when created by Claude. Group related fixes.
- **Never push. Keep all changes local — the user pushes.**

## API compatibility policy (adopted default — Griefed may override)

- The plugin-facing API (everything `serverpackcreator-api` exports, esp. `plugins`,
  `ApiWrapper`, `PackConfig`) stays source-compatible within a major version.
- Refactors keep old entry points as thin deprecated facades (`@Deprecated` with `ReplaceWith`)
  for at least one major release before removal.
- Internal-only types may move/change freely once they are no longer exported.
- **Source-compatible is not the same as behaviour-compatible.** A change that keeps every signature but
  alters what an exported call *returns* is still a contract change for embedders, and belongs in the
  release notes even though nothing fails to compile. Recorded because this branch made one:

| Change | Effect on an embedder |
|---|---|
| `PathsConfig.homeDirectory` consults `-Dde.griefed.serverpackcreator.home` **before** the stored preference and the properties file (`PathsConfig.kt:106`) | A host that sets that property now resolves a different home than the same code did before. Additive and opt-in — nothing changes unless the property is set — but every plugin reading `apiProperties.homeDirectory` follows it. |
| `ApiProperties.resolvePreferencesNode` + `PREFERENCES_NODE_PROPERTY` / `PREFERENCES_NODE_ENV` / `DEFAULT_PREFERENCES_NODE` | New exported surface; the default node name is unchanged, so existing installations keep reading their own settings. |
| The eight `default*ScriptTemplate` properties are computed per access (`PathsConfig.kt:586`–`:643`) instead of captured at construction, and the `ApiProperties` facades (`:801`–`:836`) pass that through | For a stable home the value is identical, so nothing changes for a normal embedder. What changes is that the value is no longer a *constant*: a host that moves the home at runtime (`--home`, the `-D` override, the GUI settings panel) now sees the template paths follow it, where before they kept pointing into the old home. Anything caching one of these paths across a home change was reading a stale path and should re-read instead. |
| `MinecraftServer` gains defaulted `downloadCooldown` / `clock` parameters, and a failed manifest **download** is not re-attempted for an hour (`MinecraftServer.kt`, `readServerJson`) | A manifest already on disk is still always read, so a working installation is unchanged. What changes is a *failing* one: an embedder that previously saw a download attempt — and `WebUtilities`' ERROR-with-stack-trace — on every `getServer`/`requiredJavaVersion` call now sees at most one per hour per version. The exported `Optional` shape is unchanged; a caller reading empty as "no server available" still gets that. |
| `ServerPackProvisioner.variables` reads the shipped `server_files/variables.txt` instead of a compiled-in string literal, falling back to the bundled copy (`ServerPackProvisioner.kt:56-63`). New exported members: `PathsConfig.defaultVariablesTemplate`, `ApiProperties.defaultVariablesTemplate` | A default installation gets byte-identical output — but the value is no longer a constant. An operator who edits that file changes what **every** embedder's generation emits, and one who deletes it gets the bundled fallback (the app's delete-watcher restores it). Anything asserting on a fixed `variables` string should read the template instead. |
| `ServerPackHandler.modFileEndings` and `ConfigurationHandler.zipCheck` become getters reading `ModListCompiler.modFileEndings` / `ModpackZipInspector.zipCheck`, which are promoted from `private` to public (new exported surface) | Both facades return the identical value they always did, so nothing observable changes today — this is listed because they are no longer *constants*: each is now one object shared with its owner, where before the facade held a separate equal-valued copy. An embedder comparing either by identity (`===`) against the owner's now succeeds where it previously failed; one mutating a captured reference would affect both, though both values are immutable. |
| `modscanning` gains `MissingDescriptorException`, and `DescriptorScanner.read` becomes public | Additive. The exception extends `IOException`, which the descriptor readers already declared, so an existing `catch (IOException)` is unaffected — what changes is that an absent descriptor is now *distinguishable* from a failed read, which is what lets the scanners log it at DEBUG instead of ERROR. An embedder calling `read` directly can act on that distinction; `scan` still flattens both to a default entry. **`ScanningException` is removed** — it was `internal`, so nothing outside `-api` could reference it. |
| `modscanning` gains `ModJarScanner`, `DescriptorScanner`, `JsonDescriptorScanner`, `FabricFamilyScanner`, `QuiltPackScanner` and `ModScanner.scannerFor` / `ModScanner.quiltPackScanner`; the `internal` `Scanner<T, U>` and the published `JsonBasedScanner` are **removed** | Mostly additive: a plugin can implement a scanner for the first time, and `Scanner<T, U>` was `internal` so nothing outside `-api` could ever reference it. Every concrete scanner keeps its class name, its public members and its `scan(Collection<File>): List<ScannedMod>` signature. **The one break:** `JsonBasedScanner` is gone rather than deprecated — a subclass compiled against it will not compile, and must extend `JsonDescriptorScanner` instead (same `getJarJson`, plus the scanning contract). Griefed's explicit call on 2026-08-15 overriding the policy below, on the grounds that scanners are not a pf4j extension point: a plugin could subclass the helper but never register the result, so the facade was cost without reachable benefit. |
| `ListUtilities.parallelMap` defaults its `context` to `Dispatchers.Default` instead of `newSingleThreadContext("parallelMap")` (`ListUtilities.kt:213`) | **Behaviour change on published API, and the second half of it is not just a repair.** The leak half is unambiguous: the old default handed out a dispatcher owning a dedicated thread that its creator must `close()`, which a defaulted parameter can never do, so every call stranded one thread for the life of the JVM (measured: 4 calls → 4 surviving threads named `parallelMap`). The half to actually read before upgrading: elements now run on the shared processor-sized pool rather than being confined to one thread, so an embedder whose lambda mutated shared state **without synchronisation was previously serialised by accident and can now race**. Signature unchanged, so a caller passing its own context sees nothing. Zero call sites inside this repo — the exposure is entirely embedders and plugins. Pinned by `ListUtilitiesTest.parallelMapDoesNotLeakAThreadPerInvocation` / `…RunsElementsOnMoreThanOneThread`. |
| Building `-api` against kotlinx-coroutines **1.11.0** raises the *runtime* floor to coroutines ≥ 1.11.0 | **Not a source change at all — nothing fails to compile, and that is exactly why it belongs here.** 1.11.0 renames the Kotlin-facing `runBlocking` to JVM name `runBlockingK` (verified with `javap`: `BuildersKt.runBlockingK` exists in 1.11.0, is **absent** in 1.10.2; our compiled `VersionMeta.class` emits `invokestatic BuildersKt.runBlockingK`). An embedder whose resolution **pins** coroutines to 1.10.x — a strict constraint or a BOM — gets `NoSuchMethodError` at runtime, not a build failure. Normal resolution upgrades and hides this. The break is one-directional: old bytecode calling the old name still links against 1.11.0. Widened by `parallelMap` being `inline`, which bakes the call into every downstream caller's own bytecode. **This is not hypothetical — it hit our own `-app` first** (see the build-layout landmine below), so assume it will hit any embedder on a Spring Boot BOM. |
| `ModListCompiler` / `MetadataScanner` pick Forge's scanner by comparing the whole Minecraft version instead of its minor component | **Behaviour change, and the point of the fix.** An embedder generating a pack for Forge on a `YY.x.y` Minecraft (26.x) previously got no clientside detection at all — every jar failed the annotation scan and was kept — and now gets the `mods.toml` scan that actually works. A pack that relied on "nothing is ever auto-excluded" will start excluding mods; that is the bug being fixed, not a regression. A version that cannot be parsed at all no longer throws out of `compileModList`, it falls back to the modern scanner. |
| Every outbound HTTP call now carries a connect- and read-timeout, configurable via the new `NetworkConfig` group (`WebUtilities.openTimedConnection` / `openTimedStream`; new exported `ApiProperties.networkConnectTimeout` / `networkReadTimeout` / `networkDownloadReadTimeout`) | **Behaviour change on published API, and the one most likely to be *wanted*.** Nothing set a timeout before, so `isReachable`, `downloadFile`, `getResponseAsString`, `getResponseCode`, `createHasteBinFromString` and the manifest refresh all inherited the JDK's "wait forever". An embedder on a very slow link, or one whose host black-holes packets, now sees a `SocketTimeoutException`-driven failure where it previously blocked indefinitely — which is why the values are properties, not constants: raising them restores patience, and **`0` is honoured as the JDK's no-timeout**, i.e. the exact old behaviour. Defaults 5 s connect / 15 s read / 60 s download-read. Note read-timeout bounds a *single* read, not the whole transfer, so the download value is not a transfer budget. Pinned by `WebUtilitiesTimeoutTest` against a loopback server that accepts and never answers. |
| `UpdateConfig.updateFallback` and (in `-app`) `VersionChecker.getResponse` are bounded by the same `NetworkConfig` timeouts, via the new exported `URL.timedConnection(connectTimeout, readTimeout)`; `NetworkConfig` gains `DEFAULT_CONNECT_TIMEOUT` / `DEFAULT_READ_TIMEOUT` / `DEFAULT_DOWNLOAD_READ_TIMEOUT` | **Behaviour change on published API, and the completion of the timeout work rather than an extension of it.** An audit found the "every outbound call is bounded" claim false: these two were still unbounded, and `updateFallback` is the worst-placed of all — `ApiProperties`' own `init` calls it, so an embedder constructing `ApiProperties` against a silent host blocked *there*, before `stageOne` returned and before the manifest checks that were bounded first. Both now fail with a `SocketTimeoutException`-driven error where they previously waited forever; both are configurable through the same properties, and `0` still restores the old unbounded behaviour. `URL.timedConnection` is exported because a settings group cannot use `WebUtilities` (it is constructed *from* `ApiProperties`, so the dependency would close a cycle) and needed the mechanism without a second copy of it. Pinned by `UpdateConfigTimeoutTest` and `VersionCheckerTimeoutTest`. |
| `ModpackManifestParser.manifestCandidates(destination)` and the `ConfigurationHandler.manifestCandidates` facade | Additive: the six launcher-manifest paths `checkManifests` consults, existing or not, most-specific first. Exported so a caller can ask whether a modpack's manifests *changed* without duplicating the paths — the GUI's config-editor keys a memo on their size and mtime to avoid re-parsing a multi-megabyte `minecraftinstance.json` on every keystroke-pause. Nothing existing changes: `checkManifests` consumes the same list it now returns, and the order follows the branches it always took. The list deliberately includes files that do **not** exist, because a memo has to notice a manifest about to be created. Pinned by `ManifestCandidatesTest`, including that the facade reads the parser rather than re-declaring the paths. |
| `ModListCompiler` builds one `FilterMatcher` per generation: the exclusion-filter setting is read once and REGEX/EITHER patterns are compiled once per entry, and a malformed entry is logged-and-skipped instead of thrown | **Behaviour change, and the point of it.** Under `REGEX`/`EITHER` a single malformed clientside-list entry used to throw `PatternSyntaxException` straight out of `compileModList`, aborting the whole generation; now that entry never matches, its error is logged once per generation rather than once per mod, and every other entry still applies. An embedder relying on the throw to detect a bad list must read the log instead. The performance half is real but small — measured at 300 mods x 550 entries, the per-comparison property read was ~3 ms and the per-comparison `Pattern.compile` ~20 ms, so do not expect a visible speed-up. Pinned by `ModListCompilerHotLoopTest`. |
| `ModpackZipInspector` reads a modpack archive's central directory **once** per inspection, and gains a defaulted `openZip` constructor parameter | Additive and behaviour-preserving in what it returns; the change is in how much I/O it costs. `checkZipArchive` shared one open between its validity check and its base-directory scan (was two), and `getAllFilesAndDirectoriesInModpackZip` partitions one header pass (was two opens via the per-kind methods). Measured 79.9 ms per central-directory read on a 10,000-entry archive, so ~80 ms saved at each site, scaling with the archive. `getDirectoriesInModpackZipBaseDirectory` keeps its signature and delegates to the new private `baseDirectoriesOf`. The constructor parameter is defaulted, so Kotlin still emits a no-arg constructor and Java callers are unaffected; it exists because open-counts are otherwise unobservable (`ModpackZipInspectorOpenCountTest`). |
| `GenerationConfig.clientsideModsRegex` / `modsWhitelistRegex` (and their `ApiProperties` facades) return a fresh `TreeSet` per read instead of one shared, cleared-and-refilled field | **Behaviour change on published API.** Same values, same signatures — but the caller no longer receives an alias of the config's own state. Before, a held result was emptied and rewritten on the next read, and because clear-then-refill is not atomic a concurrent reader could observe the set *part-way through* (briefly wrong, not merely stale) — reachable, since the GUI reads settings from a `parallelStream` walk over its open tabs. Both properties change from `var … private set` to a computed `val`; verified dead first, the private setter was never assigned in `-api` or `-app`. Pinned by `GenerationConfigTest.regexVariantListsAreNotSharedBetweenReads`. |
| `ForgeAnnotationScanner.dependencyCheck` / `dependencyReplace` become `val`s instead of `get() = "…".toRegex()` | Source-compatible, and the same *shape* of change this table already records for `modFileEndings` and `zipCheck`: the value is no longer created fresh on every read, so all callers now share one `Regex`. Nothing observable changes for a caller that matches with it — `Regex` is immutable and matching allocates its own matcher — but one comparing by identity (`===`) across two reads now succeeds where it used to fail. The point of the change is that the getter recompiled the pattern on every read, inside per-dependency loops. A private `additionalDependencyRegex` holding the *identical* literal is gone with it, so the pattern now exists once; it was what the two `additionalDependency*` checks actually used, meaning an edit to the documented copy would have changed nothing there. Minecraft 1.12-and-older path only. |
| `ConfigurationHandler.getAllFilesAndDirectoriesInModpackZip` loses partial results when the archive cannot be read | **Behaviour change on an error path, and the one thing about the single-pass rewrite that is not a pure win.** It used to fetch directories and files in two separate calls, each with its own `try`/`catch`, so a failure in one still returned the other's results and logged two errors. It is now one pass over the headers, so a failure returns an empty list and logs once. Barely reachable — both calls opened the *same* archive, so a failure in one would almost certainly have failed the other — but an embedder that treated a partial list as usable now gets nothing instead. |
| `WebUtilities.openTimedConnection` returns `URLConnection`, not `HttpURLConnection` | Additive, but the choice is load-bearing and must not be "tightened". The timeout setters live on `URLConnection`, so narrowing gains nothing — and `downloadFile` is published API accepting any `URL`: a `file:` URL yields a `FileURLConnection`, and casting that throws `ClassCastException`, which is **not** an `IOException` and so escapes every caller's error handling. Callers needing `responseCode` cast for themselves, as they always did. Pinned by `WebUtilitiesTimeoutTest.aNonHttpUrlCanStillBeDownloaded`. |
| `VersionMeta`'s manifest refresh drops its `isReachable` pre-check and sends `If-Modified-Since`; the logic now lives in the new **internal** `versionmeta.ManifestUpdater` | **Fewer requests and less data, with one visible log change.** A check cost two full requests (a reachability GET whose body was discarded, then the real fetch — and since the probe `disconnect()`ed without draining, the real request paid a fresh TCP+TLS handshake); it now costs one, so startup went 24 → 12 requests. An unchanged manifest that answers `304` is no longer downloaded or parsed at all: measured, 275,153 of 489,038 bytes per startup, and ~601 ms → ~392 ms median batch wall-clock. A host ignoring the header answers `200` and the version-count comparison gates the replacement exactly as before, so no per-host special-casing exists. **What an embedder watching logs will notice:** an unreachable host still logs one WARN per manifest (deliberately preserved — the failure moved onto the `IOException` path, which would otherwise have printed twelve ERRORs with stack traces on every networkless launch), with the exception itself demoted to DEBUG; the absent-manifest CRITICAL message now names the download failure instead of "unreachable". |

---

## Conventions

- **Cite names, not snapshots.** Three consecutive audits of the performance branches found the same
  defect class and nothing else: a fact quoted in prose going stale the moment the code moved — 54 commit
  hashes killed by a rebase, a landmine still describing a flaw that had been fixed, a line number shifted
  by the very commit that cited it, and suite counts left behind by the tests that were just added. Prefer
  the **commit subject** over its hash (subjects survive rebase, cherry-pick and squash), the **symbol name**
  over `File.kt:123`, and "what the guard asserts" over "how many tests exist". Where a number genuinely
  earns its place — a measurement, a byte count — say what produced it, so a reader can re-run it instead of
  trusting it.
- **KISS + MVC + TDD + SOLID** — always.
- **No shortcuts:** fix bugs when found, don't defer.
- **No assumptions:** read the code, check the docs before advising.
- **git:** never push yourself; let the user push. One branch per feature/fix; group related work;
  prefix Claude-created branches with `claude-`.

### Module boundaries (architecture — SOLID / MVC)

- `serverpackcreator-api` is the domain core. It must **not** gain compile dependencies on Swing,
  Spring web, or the web frontend. Dependencies point inward toward `-api`, never outward; the app
  and frontend are adapters around it. Flag any inward-pointing violation.
- Domain logic in `-api` must be unit-testable **without** booting a Spring context. A class that
  needs the Spring container to be tested is a design smell — flag it.
- (Plugin-API stability is governed by the **API compatibility policy** above — don't duplicate it.)

### Refactor discipline (behavior-preserving by default)

- One logical concern per commit. Keep "add tests", "pure refactor (no behavior change)", and
  "change behavior" in **separate** commits. Related behavior changes may be grouped.
- Pin current behavior with characterization tests **before** restructuring legacy code; refactor
  in small steps behind source-compatible facades (Strangler-Fig), keep the suite green at every
  commit.
- A pure-refactor commit must keep the **existing** assertions green. If a test must change for a
  "refactor", that's a signal the change is **not** behavior-preserving — stop and flag it.
- Boy-Scout rule: leave touched files cleaner than you found them, but stay within the commit's
  stated scope; don't let cleanup sprawl into unrelated files.
- **Shell templates, manifests and version parsing get their test written and observed failing FIRST.**
  Not because the rule is different there, but because these fail *silently* — a wrong branch or a
  mis-sliced version produces a plausible value, not an error — so they get verified by hand and pinned
  afterwards, if at all. Two audits found three instances (`28a786b58` NeoForge mapping, no test;
  `2e16bf0c8` fish `--no-empty`, test five commits later; `1a55797df` Fabric fall-through, whose guard
  asserted *ordering* and stayed green while the behaviour was still broken). The cost is measurable:
  the Forge launcher-era bug wasted **24 boots** before anything noticed, and 820 NeoForge versions were
  mis-attributed to one Minecraft release.
- **A test that only asserts shape is not a pin.** Prefer *executing* the unit — `ScriptTemplateContentTest`
  extracts a shell function and runs it — over asserting substring positions or that a symbol exists. And
  **confirm the test fails before the fix**: a guard whose teeth were never checked has repeatedly turned
  out to assert nothing (twice in one session, when a mis-indented edit meant the "broken" run was
  actually unmodified code).
- **Build logic is verified by measurement, not by tests — and the measurement goes in the commit message.**
  `buildSrc` has no test source set and no Gradle TestKit harness, and we have decided not to add one to pin single
  predicates (a task-wiring change or a one-line filter is not worth a second test framework in the build). So for a
  change to `buildSrc`, a `build.gradle.kts` or task wiring, the standard is: **measure the behaviour before and
  after, and record both numbers in the commit message.** `8b87057cf` did this (a planted marker plus a cached
  manifest; the cache went 643 → 0 before the change and survived after), as did the `updateManifests` retarget (app
  home 643 files vs api home 659, the difference being exactly the 16 releases that could never have been copied).
  Two audits flagged these as missing pins; this is the deliberate ceiling, so state it rather than re-flag it. Where
  a *consequence* is reachable from a normal suite, pin that instead — `ShippedManifestSnapshotTest` guards the
  outcome of the manifest work even though nothing can guard `cleanup()` itself, because `ApiWrapper.setup()`
  re-seeds from the jar and makes a wiped cache indistinguishable from a preserved one at test time.
- **Pin first means *commit* first, not just write first.** The failing test lands in its own `test(...)`
  commit, **red**, and the fix follows in the next one. In-session verification is not a substitute: it leaves
  no evidence, and it is exactly what silently passed twice above. Audited 2026-07-31 — all **eight** code
  commits of that day's plan (`2a9a03473`, `30f6cbded`, `c571e2d7f`, `07a647f01`, `aa2d27f7f`, `91ac0e1a9`,
  `1f92f585c`, `5caa6833f`) bundled guard and change, so nobody can check out `2a9a03473^` and watch the pin
  go red. The tests were written first; only the boundary collapsed, which is the part that costs nothing to
  keep and everything to reconstruct later.
- **`refactor:` is a claim about behaviour, not about intent.** Use it only when behaviour is preserved; label
  a behaviour change `fix:` or `feat:` however tidy it looks. If an **existing** test's *assertion, argument or
  expected value* has to change, the label is already wrong — that is the stop-and-flag signal, not a formality.
  **Carve-out: a reference-only update is not the signal.** Moving a symbol between modules necessarily updates
  imports and receivers in its tests, and Strangler-Fig moves are exactly what the conventions ask for — so
  `- Old.isSuspendGap(gap, poll)` / `+ New.isSuspendGap(gap, poll)`, with every assertion byte-identical, stays a
  `refactor:`. Judge the diff, not the file list: if no expectation changed, the test did not change in the sense
  this rule means. (Written after the rule's first draft flagged `b6b778b82`, a clean cross-module move, as
  mislabelled — a convention that cries wolf on legitimate refactors gets ignored wholesale.) Three commits got
  this wrong: `5f138ef8a` (`refactor(app)`) moved four call-sites onto a *resolved* Preferences node, changing where
  any host with its own node reads and writes, and had to edit `CommandlineParserTest`; `7815d5960`
  (`refactor(api)`) added an operator-editable template path plus a delete-watcher branch in `-app`; `358675fbf`
  (`refactor(ci)`) stopped 13 of the pipeline's 20 jobs from starting a dind service, cutting ~34 s off each. All
  three described the change honestly in the body — only the type lied. **`358675fbf` also shows why the label is
  worth getting right the first time:** it was audited only after being merged into `develop` and `alpha`, at which
  point the honest remedy is this entry, because the alternative is force-pushing two shared branches. The rule is
  cheap before the merge and unfixable after it.

### Kotlin idioms

- Prefer `val` over `var`; immutable data by default.
- `data class` for value types, `sealed class` + exhaustive `when` for state, Kotlin null-safety
  instead of defensive null checks. Don't port Java patterns 1:1.
- No **new** `!!` non-null assertions in refactored code — handle nullability explicitly.
- **Naming — speaking names:** variables, parameters, constants, types and methods get names a
  reader can derive meaning and context from (`configRepo`, `attemptCount`, `tokenOverridesJSON`),
  not single letters. The one carve-out is the throwaway loop counter; it generalises only to a
  small, closed set of established Kotlin idioms whose meaning is universal and whose scope is a few
  lines. The line is "would a newcomer have to scroll up to learn what this is?" — if yes, name it.
- **Documentation — comment everything:** every function, method and exported constant carries a
  doc comment — unexported ones too. State briefly WHAT it's for and HOW it achieves it, not a
  restatement of the signature. One or two sentences is the target; needing more is a sign the unit
  is doing too much (KISS). Keep comments truthful as code changes — a stale comment is worse than
  none.
- **Documenting a single-line constructor means reshaping it, and that reshape is in scope.** Per-parameter KDoc
  cannot attach to a parameter sharing a line with others, so `data class X(val a: A, val b: B)` has to become one
  parameter per line before each can be documented. Four declarations were reshaped that way on 2026-07-31
  (`ContainerRunOutput`, `CatalogCursor`, `Exclusion`, `Dependency`); an audit flagged it as scope creep, which it is
  not — it is the enabling change. Keep it to the declaration being documented, and verify the obvious: parameter
  names, types, order and defaults must survive untouched. The alternative, a class-level `@param` block, leaves the
  properties themselves undocumented as far as dokka is concerned.
- **A doc that only restates the signature is barely better than none — but silence is worse.** For a pure accessor
  over a well-named field there is often nothing to add, and dokka flags the omission either way. Put the shared
  meaning where it belongs: `ServerPackConfigTab`'s accessors were labels until the *interface* doc explained that
  they read live tab state rather than the saved configuration, which is the fact every one of them depends on.
  Prefer one honest paragraph on the type over forty restatements on its members.
- **Errors:** always handle, never ignore with `_` unless intentional (comment why).

## Definition of done (per change)

1. Tests written first and green (`./gradlew :<module>:test`).
2. Doc comments per **## Conventions** on every new/changed unit.
3. No new compiler warnings; stale comments updated.
4. CLAUDE.md "Refactor state" (and the relevant module `CLAUDE.md`) updated when an architectural
   step lands. Append the blow-by-blow to `claude-docs/REFACTOR-LOG.md`, not here.

---

## Refactor state (living — current snapshot only; full history in `claude-docs/REFACTOR-LOG.md`)

**Goal:** KISS/MVC/TDD/SOLID across api → app → plugin-example → web-frontend.
**Phases:** 0 baseline · 1 API · 2 app · 3 plugin-example · 4 frontend.

**Current status (2026-08-17):**

| Module         | Tests         | Notes                                                                                |
|----------------|---------------|--------------------------------------------------------------------------------------|
| api            | 339 (1 skip)  | Phase 1 **complete**; + `FacadeConstantDelegationTest` (the published `modFileEndings`/`zipCheck` facades must *read* their owner, asserted on identity so a re-introduced equal-valued copy still fails); + `MinecraftMetaTest` (`requiredJavaVersion`) and `ScriptTemplateContentTest` (non-gated guard for the shipped templates; skips its `fish -n` case where fish is absent, and **executes** the bash `setupFabric` to pin the offline launcher path); + `ModScannerSidenessTest` and the `ModListCompilerTest` additions (modscanning hardening, 2026-08-14 — see `claude-docs/REFACTOR-LOG.md`); + `ModScannerDispatchTest` and the Forge-era pins (modscanning generification, 2026-08-15); + the two `ListUtilitiesTest` `parallelMap` guards (thread-leak + real parallelism, 2026-08-16); + `WebUtilitiesTimeoutTest`, `NetworkConfigTest` and `ManifestUpdaterTest` (network/startup performance, 2026-08-17 — HTTP timeouts, and manifest checks pinned by *request count* against a loopback `com.sun.net.httpserver.HttpServer` rather than by wall-clock); + `ManifestCandidatesTest` (the launcher-manifest candidate list, which the GUI's memo keys on); + `ModListCompilerHotLoopTest` and `ModpackZipInspectorOpenCountTest` (generation throughput, 2026-08-17 — pinned by *read* and *open* counts, with an injected `openZip` because archive-opens are otherwise unobservable). |
| clientside     | 88            | Extracted from `-app`; `BootVerifier` split + `packPostProcessor` hook; selection (MC-support gate) + setup-abort classification pinned; `MetadataScanner` now dispatches through `ModScanner.scannerFor` instead of its own copy |
| app            | 135           | Phase 2 largely complete; clientside engine extracted out, CLI verbs stay; + `VersionCheckerTest`, `EventServiceTest` and `RunConfigurationServiceTest` (all three previously untested) and a pin on `MigrationManager.LAMBDA_SUFFIX`; + the `ConfigEditorViewModel` memo guards and `SuggestionProviderTest` (GUI typing-path performance, 2026-08-17 — pinned by *call counts* and by set identity, never wall-clock); + `AmountStatsServiceTest`, `ModPackDuplicateCheckTest` and `RunConfigurationListMigrationTest` (web query-shape and the DBRef-to-embedded migration, 2026-08-17) |
| plugin-example | 3 (from 0)    | Phase 3 **complete**                                                                  |
| web-frontend   | 31 (from 0)   | Phase 4a–4e done: Vitest, `$q` decoupling, **full TS migration**, component coverage; `types/api.ts` mod-lists are `string[]` since the web module embedded them (2026-08-17) |
| grinder        | 233 (19 skip) | + `BootWorkspaceReaper` — staging reclamation; the work tree grew unbounded at ~23 GB/h (98 GB measured) before it. Core loop **e2e-verified on current MC** (26.2/Quilt boots offline on JDK 25); **continuous fire-and-forget** with a **persisted catalog crawl cursor** (each pass takes the next slice, so coverage accumulates instead of re-checking the top N) + work-driven pacing; Modrinth + CurseForge sources; **script-template matrix IT** (bash/fish/pwsh — caught + fixed a real `.fish` bug); container/loader/report/source subpackages; MC selection bounded to image-supported Java. 94 run + 8 gated (3 engine IT, 3 live-crawl IT, 2 template-matrix). Template matrix fully green: 5 MC x 5 loaders x bash/fish, bash == fish everywhere. CurseForge is crawled **in partitions** (135 Minecraft versions × modloader × category, both sort directions) to get past its 10 000-result API cap — **now live-verified with a real API key** (`CurseForgeCrawlLiveIT`), which caught two silent design-killers the docs had hidden: `totalCount` saturates at the cap (so no split could ever fire) and the version list is 98 % non-Minecraft strings |

Key size reductions (all behind source-compatible facades): `ApiProperties.kt` 3,007 → 1,372;
`ConfigurationHandler.kt` 1,562 → 897; `ServerPackHandler.kt` 1,466 → 490.

**Remaining hotspots:** `ConfigEditor.kt` 1,369 — assessed: the two pure pieces (dirty-check,
Java-version) are extracted; the rest is legitimate Swing view code, not worth mechanically
splitting. `LarsonScanner.kt` 2,217 — self-contained widget, low priority.

**Open issues (details + locations in the relevant module `CLAUDE.md`):**
The GUI `GlobalScope.launch` anti-pattern (app) is **resolved** — all 26 sites now use
`gui.utilities.ComponentCoroutineScope` (lifecycle-cancelled), GUI-verified. The frontend's
settings-store `$q` coupling (4b) and `jsconfig.json`/TS gap (4c) are **resolved**.

**Current phase — 4 (frontend) complete; GUI structured-concurrency done.** Frontend 4a–4e: Vitest,
settings-store `$q` decoupling, full TypeScript migration (all `src/` is TS, verified by
`quasar build`), a Quasar component test harness (Vue Test Utils), and broadened component coverage
(all cards + nav SFCs; suite at 31 across 14 files; tables left untested by design — trivial format-lambda logic vs.
brittle QTable rendering). The GUI `GlobalScope.launch` anti-pattern is resolved (see Open issues),
GUI-verified. **Next (optional):** broaden component-test coverage further.
