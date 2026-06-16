# ServerPackCreator — Claude Code context

> **Purpose of this file:** the durable context for Claude Code sessions on ServerPackCreator.
> Read it before touching code. (Per-sprint history lives in `git log`)
>
> General approach: I’m a coder, IT professional. I have broad resources but limited time. Help me leverage my skill set efficiently. Don’t reinvent the wheel; always evaluate existing resources and think outside the box. Do diligent research FIRST before advising and establish the true objectives first.
>
> Organization: I’m not naturally organized. Help me stay structured. I don’t always know best practices for a particular tool or service; proactively share efficient approaches.
>
> No Shortcuts, No Compromises:
> - Fix bugs when you find them. Don’t defer or call them “out of scope.”
> - Take the correct approach, not the easy one. Technical debt compounds.
> - Never assume, always verify. Read the code, check the docs, cite references.
> - “Good enough” is not good enough. If there’s a known issue, raise it.
> - Present tradeoffs with evidence and let me decide. Don’t silently pick the easy path.
> - Document everything you verify so context isn’t lost between sessions.
> - Don't push code yourself. I will take care of that.
>
> Communication style: Challenge my reasoning instead of excessive validation. Avoid unnecessary flattery. Always web-search before giving product-specific technical advice. Never give confident guidance on hardware, apps, or setup procedures without verifying current information first.
>
> Engineering principles (binding): KISS, MVC, TDD, SOLID. The naming and documentation
> rules are spelled out in **## Conventions** below — follow them for all code.

---

## What is ServerPackCreator?

ServerPackCreator creates a server pack from any given Forge, Fabric, Quilt, LegacyFabric and NeoForge Minecraft-modpack.

It is a Kotlin-application and API, where the API is located in serverpackcreator-api, the application in serverpackcreator-app
and serverpackcreator-web-frontend. 

---

## Module map

Gradle multi-project build (`settings.gradle.kts`), Kotlin 2.3.x, JVM 21, version catalog in
`libs.versions.toml`, convention plugins in `buildSrc/src/main/kotlin/serverpackcreator.*-conventions.gradle.kts`.

- **serverpackcreator-api** — the core library (published to Maven Central via nexusPublishing).
  Packages: `config` (validation, `PackConfig`), `serverpack` (generation, `ServerPackHandler`),
  `modscanning` (clientside-mod detection per loader), `versionmeta` (Minecraft/loader version
  manifests), `plugins` (pf4j-based plugin API), `utilities`, plus `ApiWrapper` (composition
  root), `ApiProperties` (global config), `ApiPlugins`. **Plugins compile against this module —
  its public surface is a compatibility constraint.**
- **serverpackcreator-app** — four applications in one module, under
  `de.griefed.serverpackcreator.app`: `cli` (interactive CLI), `gui` (Swing),
  `web` (Spring Boot 4 / Spring 7 backend serving the frontend), `updater` (self-update +
  migrations). Entry point `ServerPackCreator.kt` + `Mode.kt` decide which runs.
- **serverpackcreator-plugin-example** — pf4j example plugin showcasing every extension point
  (config check, pre/post generation, GUI tab/panel). Documentation-by-example: must always
  reflect current API idiom.
- **serverpackcreator-web-frontend** — Quasar 2 / Vue 3 SPA, JavaScript (TS migration planned),
  Pinia stores, built into the app's web backend via the org.siouan frontend Gradle plugin.
- Not in the Gradle build: `serverpackcreator-help` (docs), `buildSrc`, `docker`, `misc`.

## Build & test commands

- `./gradlew build` — full build. The app build depends on the frontend build and license report.
- `./gradlew :serverpackcreator-api:test` — API suite (~35 s, runs against fixture modpacks in
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`; no live network needed).
- `./gradlew :serverpackcreator-app:test` — app suite.
- `./gradlew :<module>:koverHtmlReport` / `koverXmlReport` — coverage (Kover), report under
  `<module>/build/reports/kover/`.
- Frontend: `npm install && npx quasar dev` in `serverpackcreator-web-frontend/` (dev server),
  `npx quasar build` for production build.
- Run the app locally: `./gradlew :serverpackcreator-app:run` (GUI by default; CLI/web via args,
  see `Mode.kt` / `CommandlineParser.kt`).
- `media` task needs install4j installed locally — not part of regular dev loop.

## Branching & git workflow

- PRs target **`develop`**; `main` is the release branch (verified from merge history).
- One branch per feature/fix, prefixed `claude-` when created by Claude.
- **Never push. Keep all changes local — the user pushes.** (Currently: everything stays local.)

## API compatibility policy (adopted default — Griefed may override)

- The plugin-facing API (everything `serverpackcreator-api` exports, esp. `plugins`,
  `ApiWrapper`, `PackConfig`) stays source-compatible within a major version.
- Refactors keep old entry points as thin deprecated facades (`@Deprecated` with
  `ReplaceWith`) for at least one major release before removal.
- Internal-only types may move/change freely once they are no longer exported.

## Testing conventions

- JUnit 5 (Jupiter) everywhere; API tests use real fixture modpacks under
  `serverpackcreator-api/tests/` and `src/test/resources/testresources/`.
- Definition of "tested" per module: API = unit tests per class plus generation end-to-end;
  app-web = `@SpringBootTest` + MockMvc per controller; app-gui = view-model unit tests (Swing
  views stay dumb); frontend = Vitest + Vue Test Utils.
- TDD on legacy code means: pin current behavior with characterization tests **before**
  restructuring; refactor in small steps; keep the suite green at every commit.

## Definition of done (per change)

1. Tests written first and green (`./gradlew :<module>:test`).
2. Doc comments per **## Conventions** on every new/changed unit.
3. No new compiler warnings; stale comments updated.
4. CLAUDE.md "Refactor state" updated when an architectural step lands.

## Refactor state (living section — update as steps land)

Goal: KISS/MVC/TDD/SOLID across api → app → plugin-example → web-frontend. Phases:
0 baseline, 1 API (pin behavior, split ApiProperties/ConfigurationHandler/ServerPackHandler,
constructor injection), 2 app (web tests + MVC layering, GUI view-models), 3 plugin example,
4 frontend (Vitest, TypeScript, logic into stores).

- **Baseline (2026-06-11):** API 75 tests green, **75.4 % line coverage**; app 5 tests green
  (1 Spring context-load, 4 CLI), **1.1 % line coverage**; plugin-example has **zero** tests
  (`AddonTests.kt` is an empty class); frontend has no test infrastructure. Kover added via
  `serverpackcreator.kotlin-conventions`.
- Known hotspots: `ApiProperties.kt` 3,007 lines; `ConfigurationHandler.kt` 1,562;
  `ServerPackHandler.kt` 1,466; app `ConfigEditor.kt` 1,369; `LarsonScanner.kt` 2,217
  (self-contained widget, low priority). App web backend is effectively untested.
- **Phase 1a, API side (2026-06-11):** characterization tests added for ConfigurationHandler
  (manifest parsing for CurseForge/GDLauncher/ATLauncher/MultiMC, zip checks, inclusion
  suggestions) and ServerPackHandler (file gathering, cleanup, icon/properties, placeholder
  replacement). API now 105 tests. Fixed bug: `getModLoaderCase` detected "legacyfabric" as
  Fabric (branch order) and had a dead `contains("NeoForge")`-on-lowercase check — most
  specific loader names are now checked first. Documented quirks: `PackConfig.modloader`
  setter silently ignores unrecognized values; unknown loaders default to Forge.
  (`ReticulatingSplines`: GUI-only splash-texts but an intentional just-for-fun API endpoint —
  stays in the API, see Phase 1e.)
- **Phase 1a, app side (2026-06-11, started):** the web backend uses **MongoDB**
  (spring-boot-starter-data-mongodb), not JPA — full-context tests would need a Mongo
  instance. Established pattern instead: **standalone MockMvc per controller** with real API
  beans (cached version manifests make VersionMeta work offline); springmockk is available
  for mocking Mongo repositories in the remaining controllers. `VersionsControllerTest`
  (6 tests) is the template. The pre-existing `WebServiceTest` boots an empty context and
  asserts nothing — replace it during Phase 2.
- **Phase 1a, app side complete (2026-06-11):** standalone-MockMvc tests for all seven web
  controllers (versions, settings, modpack, serverpack, runconfiguration, events, stats) —
  app suite 5 → 39 tests. Web-entity IDs are `private set` (Spring Data PersistenceCreator);
  tests assign them via the `assignEntityId` reflection-helper. Two bugs found and fixed:
  (1) StatsController mapped server pack download-history to `/downloads/modpacks/{id}`,
  colliding with the modpack-history route — now `/downloads/serverpacks/{id}`;
  (2) SettingsController's Boolean settings-fields lost their "is"-prefix through Jackson,
  so the frontend (setting-store.js) read `undefined` — fixed with `@get:JsonProperty`.
- **Phase 1b in progress (2026-06-11):** `PropertyStore` extracted as the property-storage
  core (loading with blank-filtering and file-tracking, typed accessors with
  define-if-absent, custom-property prefix, override-loading, saving to tracked files; 10
  unit tests). ApiProperties delegates — `internalProps` is a reference to
  `store.properties`, so internal call sites stayed unchanged. First settings-group
  extracted: `api.settings.WebserviceConfig` (database-URI migration/normalization + three
  webservice-schedules; 4 tests), with ApiProperties keeping facade-properties.
  **Established extraction pattern:** (1) write group-tests first against PropertyStore,
  (2) move get/set-logic verbatim into the group-class with keys as companion-constants,
  (3) ApiProperties keeps thin facade-properties delegating to the group, (4) run API+app
  suites. Large data-blocks (e.g. the fallback mod-lists) are moved by python-script, not
  retyped. Second group extracted: `api.settings.GenerationConfig` (mod-lists + whitelist
  + regex-variants, directory in-/exclusions with include-wins-rule, pre/post-install
  cleanup-files, ZIP-exclusions, exclusion-filter, six generation-flags incl. legacy
  auto-discovery migration, Aikar's flags; 11 tests). ApiProperties: 3,007 → 2,126 lines.
  Dead code removed: `addDirectoryToExclude` had zero callers. `updateFallback()` stayed in
  ApiProperties (network + save orchestration). Third group extracted:
  `api.settings.PathsConfig` (homeDirectory with Preferences-resolution — the Preferences-node
  is constructor-injected so tests use a scratch-node — all derived directories/files, 12
  version-manifests, default script-templates, server-packs override, Tomcat-directories;
  8 tests). Pinned quirk: a deviating Tomcat base-directory is reset to the home-directory on
  read. ApiProperties: 2,126 → 1,754 lines; `getPreference`/`storePreference` stay on
  ApiProperties (GUI uses them for general preferences).
- **Phase 1b, groups 4+5 (2026-06-12):** `api.settings.ScriptTemplatesConfig` (start-/java-
  template maps under prefixed keys, defaults via PathsConfig, deprecated list-handling;
  4 tests — note: group-declarations in ApiProperties must come AFTER the groups they depend
  on, Kotlin initializes properties in declaration order) and `api.settings.JavaConfig`
  (javaPath with validation + system-fallback, per-version javaPaths map, java-version
  Optionals, autoupdate-flag; 5 tests using the running JVM's binary as known-valid Java).
  ApiProperties: 1,624 → 1,440 lines.
- **Phase 1b COMPLETE (2026-06-12):** final groups extracted — `api.settings.UpdateConfig`
  (update-URL, pre-release-check flag, old-version tracking, updateFallback with injected
  save-callback and GenerationConfig; 4 tests, updateFallback tested via file://-URL),
  `api.settings.I18nConfig` (language-parsing, i18n4k-propagation, changeLocale with
  save-callback; 3 tests) and `api.settings.LoggingConfig` (uppercased log-level with
  injected apply-callback; 1 test — the log4j-XML machinery stays in ApiProperties, which IS
  log4j's ConfigurationFactory via @Plugin; moving that would risk plugin-discovery).
  Webservice fallback-schedules moved into WebserviceConfig;
  `fallbackArtemisQueueMaxDiskUsage` deprecated (dead — no consumer since the MongoDB-move).
  **ApiProperties final: 1,372 lines (from 3,007), now: orchestration (loadProperties
  ordering, init), jar/OS-info, version/firstRun, preferences, hasteBin, log4j-factory, and
  facades over 8 settings-groups + PropertyStore.**
- **Phase 1c (2026-06-12):** ConfigurationHandler decomposed, 1,564 → 897 lines. Extracted
  into `api.config`: `ModpackZipInspector` (ZIP-listing + validity-checks; 2 tests),
  `ModpackManifestParser` (manifest-dispatch + 7 launcher-parsers + modloader-normalization),
  `ModloaderValidator` (name + version-checks vs VersionMeta), `InclusionsValidator`,
  `ModpackDirectoryValidator` (4 direct tests; deeper behavior pinned by the Phase 1a
  characterization tests through the facades). ConfigurationHandler is now orchestrator
  (checkConfiguration, isDir/isZip, checkForProjectInformation), pre-processing
  (sanitizeLinks, ensureScriptSettingsDefaults), reporting (printConfigurationModel — stays
  deliberately, it reports the orchestration-result), and facades. Bugs fixed: duplicate
  unreachable mmcPrismPack-branch in checkManifests; isZip assigned a found
  server.properties to serverIconPath instead of serverPropertiesPath (copy-paste).
  Note: the loader-regexes now exist in PackConfig, ConfigurationHandler (public vals) and
  the new classes — consolidate during Phase 1e.
- **Phase 1d (2026-06-12):** ServerPackHandler split, 1,466 → 490 lines. Extracted into
  `api.serverpack`: `ModListCompiler` (mods-walk, clientside-exclusion via scanner +
  user-lists, whitelist), `ServerPackFileGatherer(modListCompiler)` (inclusion-resolution,
  filters, explicit/save/directory/regex-gathering, the copy itself),
  `ServerPackProvisioner` (icon, properties, start-scripts + variables.txt + HOW-TO-RUN.md,
  ZIP-archive, improved Fabric-launcher, installer-availability, pre-/post-install cleanup).
  ServerPackHandler is now the generation-orchestrator: run() composes
  gather → icon/properties → manifest → scripts → zip → security-scan, plus plugin-hooks,
  event-listeners, destination-handling and facades. Verified by the five end-to-end
  generation tests plus the Phase 1a characterization tests — all running through the new
  pipeline-classes via the facades.
- **Phase 1e (2026-06-12):** loader-regexes consolidated into one source of truth —
  `api.config.SupportedModloaders` (the 5 exact-match regexes + canonical `names` array).
  PackConfig, ConfigurationHandler, ModloaderValidator, ModpackManifestParser and
  ApiProperties.supportedModloaders now reference it; zero `"^forge$"`-style literals remain
  outside it. Service-locator reach-backs removed: `ServerPackManifest` derived its
  SPC-version via `ApiWrapper.api()` — now reads `javaClass.getPackage().implementationVersion`
  directly; `PackConfig.save(destination, apiProperties)` is now the primary (injection-
  required) overload, with the old `save(destination)` kept as a `@Deprecated` facade that
  resolves ApiProperties via the singleton. App call-sites (CLI, ConfigGenCommand,
  TabbedConfigsTab, ConfigEditor) updated to inject explicitly. `ApiWrapper` was already a
  thin composition-root (lazy, constructor-injected collaborators) — left as-is.
  `ReticulatingSplines` (SimCity splash-texts) is an intentional just-for-fun API endpoint per
  Griefed and **stays in the API** — not moved, not deprecated. **Phase 1 (API) COMPLETE:**
  ApiProperties 3,007→1,372, ConfigurationHandler 1,562→897, ServerPackHandler 1,466→490; all
  behind source-compatible facades, six bugs fixed, API tests 75→161.
- **Phase 2a, app safety-net (2026-06-12):** characterization tests for the two app entry-point
  classes before restructuring. `CommandlineParserTest` (10 tests) pins the argument→mode
  mapping, priority-ordering and file/locale parsing — only the deterministic branches that
  `return` before the `GraphicsEnvironment.isHeadless()` GUI/failsafe checks, so headless-
  independent. The `--home` Preferences side-effect is pinned with save/restore of the real
  node. `MigrationManagerTest` (6 tests) pins `migrate()`'s version-decision logic via a
  mockk-mocked ApiProperties (controls previous/current version, verifies `setOldVersion`);
  version-ranges chosen to never match a real migration-method (highest is 6.0.0), so no
  filesystem side-effects. App suite 39→55 tests.
- **Phase 2b, web-backend assessment (2026-06-12):** the Spring backend is **already
  MVC-layered** — controllers delegate to services (ModPackService, ServerPackService,
  RunConfigurationService, EventService, the stats-services), no file over 254 lines,
  scheduling isolated in `web/scheduling`. No restructuring warranted; the controller-tests
  from Phase 1a already pin the layering. The substantive Phase 2 target is the GUI.
- **Phase 2b, GUI view-models started (2026-06-12):** first view-model extracted from the
  1,369-line ConfigEditor — `ConfigEditorViewModel.hasUnsavedChanges(current, lastSaved)` holds
  the editor's dirty-check (15-field PackConfig comparison) display-independently; the Swing
  `compareSettings()` is now a 5-line view that just shows/hides the warning-icon. 5 unit tests.
  App suite 55→60 tests.
- **Phase 2b, InclusionSpecification value-equality (2026-06-12):** fixed the dirty-check
  over-report at its root — `InclusionSpecification` gained `equals`/`hashCode` over its four
  fields (source, destination, inclusion/exclusion-filter). Manual override, NOT a `data class`
  conversion, to keep the public API surface stable for plugins. Verified safe: no
  hash-based collections (`HashSet`/`TreeSet`/`toSet`/`distinct`) of inclusions exist anywhere,
  so adding `hashCode` has no keying side-effects. Two sites changed, both toward correctness:
  the editor dirty-check (now accurate), and `ConfigurationHandler.isZip`'s
  `newCopyDirs.contains(entry)` dedup — which previously NEVER matched (reference equality), so
  ZIP-extraction could append duplicate inclusions; it now dedupes by value. All other
  inclusion call-sites use `.source` directly and are unaffected. 4 new InclusionSpecification
  tests; the editor quirk-test flipped to pin the corrected behavior.
- **Next:** continue extracting ConfigEditor logic into the view-model (validation orchestration,
  field↔PackConfig mapping, required-Java-version derivation), leaving Swing as dumb views.

---

## Conventions

- **KISS + MVC + TDD + SOLID** — always
- **No shortcuts:** fix bugs when found, don't defer
- **No assumptions:** read the code, check the docs before advising
- **git** - Never push yourself, let the user handle pushing of commits. One branch per feature/fix. When features or bugs are related, group them. Always prefix a branch you create with "claude-".
- **Naming — speaking names:** variables, parameters, constants, types, and methods get
  names a reader can derive meaning and context from (`configRepo`, `attemptCount`,
  `tokenOverridesJSON`) — not single letters. The one carve-out you named is the throwaway
  loop counter. That carve-out generalises to a *small, closed* set of
  established Kotlin idioms whose meaning is universal and whose scope is only a few lines — keep
  these, but do not let the habit spread beyond them. The line is
  "would a newcomer have to scroll up to learn what this is?" — if yes, name it.)
- **Documentation — comment everything:** every function, method, and exported
  constant carries a doc comment — unexported ones too, not just the public API. It states,
  briefly, WHAT the thing is for and HOW it achieves it, not a restatement of the signature.
  One or two sentences is the target; needing more is a sign the
  unit is doing too much (KISS). Keep comments truthful as the code changes — a stale comment
  is worse than none.
- **Errors:** always handle, never ignore with `_` unless intentional (comment why). 