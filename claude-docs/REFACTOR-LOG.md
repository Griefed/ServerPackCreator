# ServerPackCreator — Refactor log (narrative history)

> Blow-by-blow history of the KISS/MVC/TDD/SOLID refactoring. Moved out of `CLAUDE.md` so it
> doesn't load into every Claude Code session — read on demand when you need the "why" of a past
> decision. The current snapshot and standing rules live in the root `CLAUDE.md`; durable
> module-specific facts live in each module's `CLAUDE.md`.

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
- **Phase 2b, view-model rounded out (2026-06-17):** `requiredJavaVersion(minecraftVersion)`
  (Minecraft→required-Java derivation with the "?"-fallback) moved into ConfigEditorViewModel,
  which now takes `VersionMeta`; `ConfigEditor.acquireRequiredJavaVersion()` is a one-line
  facade. 2 more tests (mockk-mocked VersionMeta→minecraft→getServer→javaVersion chain). App
  suite 60→62. **Assessment: ConfigEditor extraction is essentially done for now** — the two
  genuinely-pure pieces (dirty-check, Java-version) are out and tested; the remaining ~1,330
  lines are legitimately view code (widget wiring, MigLayout, status-icon updates, combo-box
  models, event handlers) whose domain logic already lives in the API (ConfigurationHandler,
  fully tested in Phase 1c). Not worth mechanically extracting thin Swing getters.
  **Flagged, NOT changed (needs runtime verification):** ConfigEditor uses `GlobalScope.launch`
  in 4 places (lines ~702, 1043, 1211, 1333) — a structured-concurrency anti-pattern
  (`@OptIn(DelicateCoroutinesApi)`). Proper fix is a component-lifecycle-scoped CoroutineScope;
  deferred because it changes async execution and can't be verified without running the GUI.
- **Phase 3, plugin-example (2026-06-18):** the example plugin already uses current API idiom —
  no deprecated calls (`ApiWrapper.api()` to register listeners is the intended plugin idiom,
  not the internal constructor-injection). Fixed the test layout: the empty `AddonTests.kt`
  lived in `src/test/java`; replaced with a real `ConfigurationCheckTest` (3 tests) in the
  correct `src/test/kotlin`, doubling as documentation-by-example of unit-testing a
  `ConfigCheckExtension` (relaxed-mockk the unused versionMeta/apiProperties/utilities). Added
  `io.mockk:mockk:1.14.6` to plugin-example test deps. Plugin-example suite 0→3 tests.
  **Integration coverage already exists and was verified:** `ApiPluginsTest` (API module, where
  the fixtures live) loads the freshly-built plugin jar via pf4j and asserts all six extension
  points are discovered (PostGen/Tab/PreGen/PreZip/ConfigCheck/ConfigPanel) — rebuilt the jar
  against the refactored API and confirmed it still loads, proving the compatibility policy held
  and the Phase 1 plugin-hook refactoring preserved extension wiring. A hook-firing-during-
  generation test was deliberately NOT added: the example hooks only println (verifying them
  means brittle stdout-capture), and the discovery test + the Phase 1d generation tests already
  cover the meaningful integration. Note: `serverpackcreator-plugin-example-dev.jar` under the
  API test-resources is a build artifact regenerated by `copyPluginsApiUnitTests` — don't commit
  rebuilds.
- **Phase 4a, frontend test infra (2026-06-18):** stood up Vitest (was zero tests). Added
  dev-deps `vitest@3 @vue/test-utils@2 @pinia/testing@1 happy-dom`; `vitest.config.js` maps the
  Quasar path-aliases (src/boot/stores/components/...) and uses happy-dom. `npm test` now runs
  `vitest run`. First tests: `test/stores/setting-store.test.js` (2 tests) pins the settings
  store's `refresh()` data-fetching against a mocked axios boot-module (`vi.mock('boot/axios.js')`
  avoids the Quasar-only `#q-app/wrappers` import chain and the network). Store cleanup: removed
  the dead `doubleCount` getter (Pinia-template leftover referencing a non-existent `counter`),
  and `refresh()` now `return`s its promise so callers (and tests) can await it — the sole caller
  (SubmitModPackForm) doesn't await, so behavior is unchanged.
  **Findings (not yet addressed):** (1) `stores/index.js` registers NO Pinia plugins, so the
  `this.$q.notify(...)` in `refresh()`'s error path hits an undefined `$q` — the error handling
  is latently broken; fix is to decouple the store from `$q` (return/throw and let the component
  notify). (2) `jsconfig.json` extends a non-existent `./tsconfig.json` — to be resolved by the
  TS migration. (3) Build/lint tooling stays on JS+ESLint9; TS migration is the next step.
- **Phase 4b, settings-store `$q` decoupling (2026-06-22):** the store's `refresh()` error path
  called `this.$q.notify(...)`, but `stores/index.js` registers no Pinia plugins, so `$q` was
  undefined — error handling was latently broken. Made `refresh()` pure data-fetching: it now
  returns/rejects its promise and the store stays UI-agnostic (MVC). The sole caller,
  `SubmitModPackForm.setup()`, gets `$q` via `useQuasar()` and notifies in a `.catch()`. Added a
  store test pinning that `refresh()` rejects on a failed request (suite 2→3). Also committed a
  docs restructure first: split the monolithic root `CLAUDE.md` into a current-state root + one
  lazy-loaded `CLAUDE.md` per module + this `REFACTOR-LOG.md`; fixed the log's path references
  (file lives at repo root, not `docs/`).
- **Phase 4c, TypeScript migration (2026-06-22):** converted the whole SPA to TS in six green
  steps, each verified by vue-tsc + eslint + vitest (and a full `quasar build` at the end).
  - 4c-1 infra: root `tsconfig.json` extends-only (`./.quasar/tsconfig.json`, regenerated by
    `quasar prepare`); strictness via `quasar.config.js` → `build.typescript { strict, vueShim }`
    (not by editing the generated tsconfig); `vite-plugin-checker` gains `vueTsc: true`; ESLint
    flat config gains `typescript-eslint` + the TS parser for `<script lang="ts">`; dropped the
    dead legacy `.eslintrc.cjs`/`.eslintignore` and the dangling `jsconfig.json`; dev-deps
    `typescript` (explicit), `vue-tsc`, `typescript-eslint`; added a `type-check` script. Matches
    Quasar's official "Supporting TypeScript" guidance.
  - 4c-2..6: boot/router/store/i18n scaffolding → settings store (explicit `SettingsState`) →
    display components (cards/tables) → layout + pages → `SubmitModPackForm`. Recurring fixes:
    typed method params and axios `.map`/`.forEach` callbacks; `QTableColumn[]` so `align`
    literals don't widen (inert `field` on slot-rendered action columns); QScrollArea style
    `opacity` as strings; dropped the invalid `this.` prefix from in-template state/dictionary
    access (idiomatic + no longer possibly-undefined `this`); coerced `$route.params.id`
    (`string|string[]|undefined`) at axios call sites.
  - Kept `noUncheckedIndexedAccess` ON (Quasar's strict preset adds it) — it caught a real bug:
    QFile `@rejected` emits `QRejectedEntry[]`, so the old `rejectedEntry.name` was always
    `undefined`; fixed to read `rejectedEntries[0].file.name`. Cost is optional-chaining on
    dictionary/array index access (mainly `SubmitModPackForm`'s picker templates).
  - Behavior-preserving cleanups surfaced by typing: `SubmitModPackForm`'s four `ref(new Map)`
    values were only ever used as string-keyed dictionaries (bracket access; the version maps get
    overwritten by JSON objects) → retyped as `Record<…>`; removed a stray debug `console.log` in
    `ModPackDownload.current()`. Backend payload shapes the templates read live in
    `src/types/api.ts` (only the fields used). All `src/` is now TS (9 `.ts` + 21 `.vue`, 0
    `.js`); config files and the one test stay JS (Quasar convention).
- **Phase 4d, component test harness (2026-06-22):** stood up Vue Test Utils component testing.
  `vitest.config.js` adds `@quasar/vite-plugin` (already a transitive dep) so SFCs get Quasar's
  on-demand component auto-import — without it `<q-*>` render as unresolved custom elements; the
  plugin also resolves Quasar to its client build (Node resolution otherwise picks the `node`/
  server build, whose `install()` throws in happy-dom — found the hard way). `test/install-quasar.ts`
  (a `setupFiles` entry) installs Quasar globally on the VTU `config.global.plugins` so mounted
  components get `$q` + directives. Added `test/components/AboutItem.test.ts` (smoke: renders a
  target=_blank anchor to its link prop) and `test/components/ErrorsCard.test.ts` (one `.q-item`
  per error with id+message; empty list for none). Suite 3→6. vue-tsc type-checks the `.ts` tests
  (tsconfig include covers `test/`); lint scope stays `src*` as before.
- **Phase 4 complete.** All five modules refactored (api/app/plugin-example/web-frontend) per
  KISS/MVC/TDD/SOLID. **Next (optional):** broaden component-test coverage; resolve the flagged
  `ConfigEditor` `GlobalScope.launch` anti-pattern (app) once GUI runtime verification is possible.
- **Phase 4e, broadened component coverage (2026-06-23):** characterized the rest of the cards and
  nav SFCs the earlier audit flagged as untested (M1). Three presentational tests — `DrawerLink`,
  `IndexItem` (prop→title/caption/icon, routing left to integration), `NotAvailableCard` (static
  N/A). Three data-card tests mocking `boot/axios` + `flushPromises`: `ModPackCard` pins the
  `projectID/versionID.length === 1 ? value : 'N/A'` Modrinth-id template quirk (plus size-MB and
  server-pack count, asserted via ordered `.q-item__label--caption` nodes); `RunConfigurationCard`
  pins the nested-array flattening (`{argument}`/`{mod}` → flat strings) and the space-joined
  start-args; `ServerPackCard` pins fetched-field wiring + size-MB. `RunConfigurationCard` imports
  `runConfigs` via a relative path that resolves to the same module the `boot/axios` alias points
  at, so the single `vi.mock('boot/axios')` intercepts it. Suite 6→23 (12→23 vs. the 4d audit
  baseline). **Bug found + fixed in its own commit** (audit-discipline): `DrawerLink` used
  `colour="accent"` — Quasar's QIcon prop is `color`, so the misspelled attribute was silently
  ignored and the drawer icons rendered in the default color rather than accent. Sole occurrence
  (100 correct `color=` usages elsewhere); the characterization test pins rendering not color, so
  it stayed green across the fix. **Tables deliberately left untested**
  (`ModpacksTable`/`ServerPacksTable`/`HistoryTable`): the only non-presentational logic is trivial
  column `format` lambdas; pinning them requires mounting the full QTable against mocked rows and
  asserting slot-rendered cells — high brittleness for near-zero logic density (same judgment call
  as `LarsonScanner`). Frontend Phase 4 (4a–4e) now closed; the one remaining cross-module flag is
  `ConfigEditor`'s `GlobalScope.launch` (app, needs GUI runtime).
- **GUI structured-concurrency (2026-06-23, branch `claude-gui-coroutinescope`):** eliminated the
  `GlobalScope.launch` anti-pattern across the whole Swing GUI — 26 launch sites in 14 files now run
  on `gui.utilities.ComponentCoroutineScope` (a `SupervisorJob` scope that lazily re-creates after a
  cancel; `@Synchronized` because launches may start off the EDT). Cancellation is anchored to the
  owner's lifecycle: JComponents (`ConfigEditor`, `ScrollTextArea`, `IconPreview`,
  `SelectedInclusionDetails`, `InclusionsEditor`, the two about-menu `JMenuItem`s) cancel from
  `removeNotify()`; non-components anchor an `AncestorListener` (`ancestorRemoved`) on their backing
  component (`SuggestionProvider`→sourceComponent, `TabbedConfigsTab`/`ControlPanel`→panel, the two
  check-timers→owning tab panel) or a `WindowListener` (`windowClosed`) on the frame (`MainWindow`,
  `TipOfTheDayManager`). Done in two phases: **real-leak components first** (ConfigEditor,
  ScrollTextArea, IconPreview, SuggestionProvider, inclusions, TabbedConfigsTab — 19 sites,
  **GUI-verified** by Griefed: load/scan/close-mid-scan, tab close-button, not-found dialog,
  text-area search/replace, autocomplete, icon preview, inclusion tips, step-by-step guide), then
  the **app-lifetime singletons + ControlPanel** (MainWindow, the two menu items, TipOfTheDayManager,
  the two check timers, ControlPanel generation). Notes: (1) `CoroutineStart.ATOMIC` is itself
  `@DelicateCoroutinesApi` independent of GlobalScope, so the three ATOMIC sites
  (`ConfigEditor.loadConfiguration`, `IconPreview`, `ControlPanel.generate`) keep a *narrow*
  `@OptIn(DelicateCoroutinesApi)` with the start preserved; all other opt-ins removed. (2)
  `ControlPanel.generate` is anchored to the always-visible bottom bar so a running generation is
  cancelled only on window close, never by a tab-switch. (3) The check timers (`SettingsCheckTimer`,
  `ConfigCheckTimer`) moved their `ActionListener` out of the `Timer` super-constructor (where `this`
  is unavailable) into `init`, to launch on an instance scope — `ConfigCheckTimer` keeps its large
  listener body verbatim as a property to avoid re-indentation churn. No automated tests (Swing view
  code, verified via the running GUI); app suite stays green throughout, no new compiler warnings.
  **`checkServer()` confirmed live** (called by `ConfigCheckTimer`), despite the "install server"
  checkbox being gone from the GUI.
- **Clientside engine extracted to its own module (2026-06-26, branch `claude-grinder`):**
  behavior-preserving move of the `clientside/` package out of `serverpackcreator-app` into a new
  `serverpackcreator-clientside` Gradle module, ahead of building a standalone Docker "grinder"
  service that boots candidate mods in parallel isolated containers. **Why a new module, not `-api`
  and not exclusions:** the engine pulls Playwright + boot machinery, which must not pollute the
  published, compatibility-frozen `-api` core (module-boundary rule); and excluding `-app`'s
  transitive deps from a grinder dependency is fragile whack-a-mole (runtime `NoClassDefFoundError`,
  rotting exclude-list) versus a lean module that never carries Spring/Swing in the first place. The
  engine was already clean — its only imports are `-api` (`ApiWrapper`, `ModScanner`, `PackConfig`,
  `versionmeta`, `utilities`), Playwright, Jackson and log4j-kotlin; no `-app` types. Moves: 16 main
  + 10 test files via `git mv`, package renamed `de.griefed.serverpackcreator.app.clientside` →
  `de.griefed.serverpackcreator.clientside` (dropping the wrong `.app.` segment). New module build
  reuses `kotlin`/`dokka` conventions, declares Playwright + `jackson-module-kotlin` (the API ships
  `jackson-databind` and `log4j-api-kotlin` transitively but not the Kotlin module), and mirrors the
  app's `dependsOn(:serverpackcreator-api:processTestResources)` for `MetadataScannerTest`'s offline
  `ApiWrapper`. `-app` now depends on the module via `api(project(...))` (Playwright dropped from its
  build — transitive from here); the four CLI command files repointed their imports. Docs split
  accordingly: `module.md` and module `CLAUDE.md` for the engine created in the new module, trimmed
  out of `-app`'s (CLI verbs + arg-order landmine stay). **All 37 clientside tests green; full app
  suite (71) green; no new warnings.** Next: split `BootVerifier` into a host-side `prepareBootPack()`
  and a `runServer()` interface so a container impl can slot in for the grinder.
- **BootVerifier split into staging + a ServerRunner seam (2026-06-26, branch `claude-grinder`):**
  behavior-preserving extraction so the grinder can boot prepared packs in isolated containers while
  reusing the verdict logic. `BootVerifier.verify()` now delegates to (1) `prepareBootPack()` — public
  host-side staging (candidate pick → download mod + deps → generate self-installing pack), returning a
  `Prepared.Ready`/`Failed`; (2) a `ServerRunner` interface returning *raw* `RunResult.Completed(lines,
  exitCode, timedOut)` / `NotStarted(detail)` (classification deliberately left to the caller so every
  runner is judged by the same `BootLogClassifier`), with `HostProcessServerRunner` = the old `start.sh`
  -spawning `boot()` body verbatim; and (3) a companion `outcomeFor()` — the shared verdict seam (write
  log → classify → crash-excerpt). `verify()`'s signature is unchanged; the new `serverRunner` ctor
  param defaults to the host runner, so the `VerifyClientsideCommand` caller (named args) is untouched.
  The split makes two previously boot-only paths offline-testable: `BootVerifierOutcomeTest` (3 cases:
  NotStarted→INCONCLUSIVE/no-log, non-zero→CRASHED+excerpt+written-log, ready-line→SURVIVED/no-excerpt)
  and `HostProcessServerRunnerTest` (no-`start.sh`→`NotStarted`). Done as two commits (pure refactor,
  then add tests). **clientside 41/41 green; app compiles; no new warnings.** Next for the grinder: a
  `ContainerServerRunner` (`docker-java`, `--network none`, resource-capped), the worker pool/queue, and
  a per-`(loader, loaderVer, mcVer)` pre-bake cache so each mod-boot runs offline.
- **Grinder module scaffolded — container-backed ServerRunner (2026-06-26, branch `claude-grinder`):**
  new standalone `serverpackcreator-grinder` (package `de.griefed.serverpackcreator.grinder`),
  depending only on `-clientside` + `docker-java` (`docker-java-core` + `-transport-zerodep`, 3.7.1; no
  Spring/Swing, unpublished). Reuses the clientside `ServerRunner` seam: `ContainerServerRunner`
  implements it by booting a prepared pack in a hardened container instead of a host process, so it
  drops into `BootVerifier` unchanged and feeds the same `BootLogClassifier` (via `outcomeFor`). The
  docker interaction sits behind a `ContainerEngine` seam (same injectable-boundary pattern as
  clientside's `HttpFetcher`), so the runner's host-side staging (start-script check, eula,
  `ContainerSpec` assembly) + result mapping are unit-tested with a fake; `ContainerSpec` carries the
  untrusted-mod hardening as *defaults* (`--network none`, read-only rootfs, drop ALL caps,
  no-new-privileges, non-root, tmpfs `/tmp`, memory/cpu/pids caps; socket never mounted). The real
  `DockerJavaContainerEngine` (create→start→follow→stop→inspect→remove) is integration-only — it
  compiles against docker-java 3.7.1 (validating the API surface) but needs a live daemon to run.
  Also a Boy-Scout cleanup of the just-landed seam: writing the second runner revealed `ServerRunner.run`
  carried a dead `logFile` param (the caller persists the log), now dropped. `ContainerServerRunnerTest`
  (3, offline) pins no-start.sh→NotStarted-without-launching, raw-output→Completed, and the hardened
  spec/mount/eula. **grinder 3/3 green; clientside 41/41 green.** Remaining for fire-and-forget: pre-bake
  cache, popularity-ranked queue + worker pool, verdict store → sortable/CSV table via the existing
  frontend.
- **Grinder pre-bake cache + host-prereqs doc (2026-06-26, branch `claude-grinder`):** added
  `LoaderCache` — the `--network none` enabler. `ensureInstalled(loader, loaderVersion,
  minecraftVersion)` returns a cached installed-server base, running a one-off `LoaderInstaller` (with
  network) only on a miss; **marker-gated** (`.spc-installed` written only after success, so a partial
  install is redone not served) and **serialized per tuple** (parallel workers share one install).
  `LoaderInstaller` is the seam (real impl = a setup container with network snapshotting the
  ServerStarterJar self-install — integration-only); the cache logic is pure. `LoaderCacheTest` (5,
  offline, fake installer) pins miss-installs-once-then-hits, failed/throwing→null+clean, install-once
  across 6 concurrent threads, and independent tuples. Also documented the host prerequisites that land
  when `BootVerifier` is wired in: `CURSEFORGE_API_KEY` (CF resolution) + Playwright/Chromium on the
  host (locked-file `BrowserDownloader`, which runs host-side during staging, not in the boot
  container) — key + browser are complementary, a locked CF mod needs both. **grinder 8/8 green.**
- **Grinder docker glue live-verified + grind orchestration (2026-06-26, branch `claude-grinder`):**
  two steps. (1) `DockerJavaContainerEngineIT` — gated behind `GRINDER_DOCKER_IT=1`
  (`@EnabledIfEnvironmentVariable`, skipped on daemon-less CI) — ran against **Docker Desktop 29.5.3**
  and passed: create→start→stream→ready-detect/stop→exit-code→force-remove, under the production
  hardening (`--network none`, read-only rootfs, dropped caps, non-root), no leaked containers. This is
  the one layer no unit test can reach, now validated. (2) The grind **orchestration**, built at a
  testable altitude by collapsing the integration-bound boot pipeline behind a `CandidateVerifier`
  seam: `Grinder.grind` verifies one candidate (skip already-ground, swallow a thrown boot) and records
  one `GrindVerdict` per loader; `GrindPool.grindAll` drains a popularity-ranked batch across N worker
  threads; `VerdictStore` (in-memory, `slug+loader`-keyed, replace-not-duplicate) accumulates;
  `VerdictCsvExporter` renders RFC-4180 CSV (`Name, Project, NamePattern, Confidence, Loader, Detail`,
  highest-confidence-first) — the export from the original feature ask. 13 new unit tests
  (`VerdictStoreTest`, `VerdictCsvExporterTest`, `GrinderTest` + shared `GrindTestFixtures`) cover
  replace/skip/swallow/per-loader-recording/CSV-escaping+ordering/pool-drain+popularity. **grinder
  21/21 unit green, +2 IT (gated).** Remaining: runtime image + real `LoaderInstaller`, the real
  `CandidateVerifier` integration adapter (incl. the cache-overlay seam in `BootVerifier`), a
  persistent `VerdictStore`, and the web table over the existing Quasar frontend.
- **Grinder persistence + self-contained web report (2026-06-26, branch `claude-grinder`):** the
  *visible half* of the original feature ask. `JsonVerdictStore` — file-backed `VerdictStore` (loads on
  start, whole-file temp-then-atomic-move write so a crash can't truncate it, corrupt-file → empty +
  log) so a multi-day fire-and-forget run resumes after a restart; Instant via jackson-datatype-jsr310.
  `VerdictReportRenderer` — a self-contained HTML page with click-to-sort columns and an embedded-CSV
  download button, HTML-escaped cells **and** `\uXXXX`-escaped CSV inside the `<script>` block so a
  mod-supplied `</script>` can't break out. `ReportServer` — serves the table (`/`) + CSV
  (`/export.csv`) live off the store via the JDK's built-in `com.sun.net.httpserver.HttpServer`, **no
  Spring / no new web dependency**. **Decision:** kept the report standalone rather than rendering
  through the app's Quasar frontend (as first mooted), because the grinder must not depend on `-app`
  (which would drag in Spring/Mongo/Swing and break its standalone nature). 9 new tests incl.
  `ReportServerTest` exercising a real loopback HTTP server on an ephemeral port. **grinder 30/30 unit
  green (+2 gated IT).** Remaining: runtime image + real `LoaderInstaller`, the real `CandidateVerifier`
  adapter (cache-overlay seam), a candidate source, and the main fire-and-forget entrypoint.
- **Grinder Modrinth candidate source (2026-06-26, branch `claude-grinder`):** `ModrinthCandidateSource`
  seeds the queue from Modrinth **most-downloaded-first** — the keyless search API returns the download
  count, so the popularity ranking (which decides what to grind first) is free. Paginates until the
  requested limit or catalog exhaustion, behind the clientside `HttpFetcher` seam (reused from
  `-clientside`), so it's unit-tested against canned JSON with no network. 5 tests
  (`ModrinthCandidateSourceTest`) — order preserved, pagination + exhaustion + a defensive over-limit
  `take` (a test caught a final page overshooting the limit), failed-page-returns-partial, limit-0
  no-fetch. **grinder 35/35 unit green (+2 gated IT).** A CurseForge sibling (needs the API key, no
  declared sideness) is the natural follow-up; the visible half (table/CSV) plus the queue source are
  now in place, leaving the integration adapter (real `CandidateVerifier` + runtime image) and the
  main entrypoint.
- **Grinder runtime image drafted + build-verified (2026-06-26, branch `claude-grinder`):**
  `docker/Dockerfile` (+ `docker/README.md`). Key realisation from reading `ServerPackProvisioner` +
  the `default_template.sh`: SPC's generated `start.sh` installs the loader itself, so the image is
  **loader-agnostic** — the **neoforged `ServerStarterJar` is Forge/NeoForge only** (Fabric uses
  `fabric-installer`/`fabric-server-launch(er).jar`, Quilt the `quilt-installer`, LegacyFabric its
  own). The image therefore ships only the shell tooling the template needs (`bash`, `curl`/`wget`,
  `gawk`, `tar`/`gzip`, `ca-certificates`) + Temurin JDK **8/17/21** (a single JDK can't boot every MC:
  ≤1.16→8, 1.17–1.20.4→17, 1.20.5+→21), so the grinder sets `$JAVA` per MC version with no Java
  download (keeps mod-boots offline under `--network none`). Built on Docker Desktop 29.5.3 and
  smoke-tested: all three `java -version` work, `$JAVA` defaults to 21, tools present, runs non-root
  uid 1000; ~1.6 GB. Next: the real `LoaderInstaller` (setup boot *with* network, snapshot into
  `LoaderCache`) + the `CandidateVerifier` cache-overlay seam.
- **Grinder cache-overlay hook + real LoaderInstaller (2026-06-28, branch `claude-grinder`):** built on
  the loader-install spike. (1) **`BootVerifier.packPostProcessor` hook** (in `-clientside`): an
  optional `((Prepared.Ready) -> Unit)?` run after `prepareBootPack`, before `serverRunner.run` — the
  cache-overlay seam, default `null` = unchanged host behavior, a thrown hook → INCONCLUSIVE. Extracted
  the post-process→boot→classify path into a companion `runPrepared` so it's unit-tested without
  `ApiWrapper` (`BootVerifierRunPreparedTest`, 3); `ServerRunner` is now a `fun interface`. (2) The real
  **`LoaderInstaller`** (`DockerLoaderInstaller`): generates a mod-less pack (`VanillaPackGenerator`/
  `ApiVanillaPackGenerator`), boots once **with network** (`bridge`), snapshots the install layer into
  `LoaderCache`. Its error-prone cores are pure + tested: `InstallLayerSnapshot` (denylist diff/copy —
  snapshot added non-runtime files; SPC's `CLEANUP` var rejected as incomplete), `PackVariables` (eula
  + `WAIT_FOR_USER_INPUT=false` + `JAVA` per MC + offline `SERVERSTARTERJAR_FORCE_FETCH=false`),
  `JavaForMinecraft` (MC→bundled-JDK, the 1.20.4/1.20.5 boundary). 11 new tests. **clientside 44/44,
  grinder 43/43 unit green (+2 gated IT).** Remaining: the real `CandidateVerifier` wiring (post-processor
  → ensureInstalled → overlay) + the main fire-and-forget entrypoint.

- **Grinder MC selection bounded to image-supported Java (2026-06-28, branch `claude-grinder`):**
  resolves the Java/image limitation the e2e verification surfaced (the grinder picked the *newest*
  Minecraft release — 26.x in this environment — whose required JDK the image's 8/17/21 set lacks, so
  `start.sh` aborted at a Jabba Java-install prompt). Replaced the hand-rolled `JavaForMinecraft`
  heuristic (wrong for the `26.x` scheme) with **`ImageJavaRuntimes`**, which sources the required Java
  major **authoritatively** from `MinecraftMeta.requiredJavaVersion(mc)` (Mojang's declared
  `javaVersion.majorVersion`) and exposes `supports(mc)` (required-Java known *and* in `bundledMajors`,
  default 8/17/21 — mirrors the Dockerfile) + `javaPath(mc)`. `BootVerifier` gained an injected
  `minecraftAcceptable: (String)->Boolean = { true }` AND-ed into candidate selection (host CLI keeps
  accept-all; `ContainerCandidateVerifier` passes `imageJava::supports`), so a version whose JDK the
  image lacks is **never selected** — never booted on the wrong JDK and never mis-scored as a clientside
  crash (false HIGH). Deliberately *not* `SKIP_JAVA_CHECK`. `PackVariables.prepareUnattended` now takes a
  resolved `javaPath` (no version heuristic); `DockerLoaderInstaller`/`ContainerCandidateVerifier` resolve
  it via `ImageJavaRuntimes`. Trade-off: until a newer JDK is bundled, mods targeting *only* 26.x are
  skipped (extend coverage by adding the JDK to the Dockerfile **and** `ImageJavaRuntimes.bundledMajors`).
  Swapped `JavaForMinecraftTest`→`ImageJavaRuntimesTest` (gate + resolution). **clientside 44/44, grinder
  44/44 unit green (+2 gated IT).**

- **Grinder runtime image: bundle Temurin 25 for current Minecraft (2026-06-28, branch `claude-grinder`):**
  the verified follow-up to the Java/image bound. Checked: latest MC release is **26.2**, declaring
  **Java 25** (`java-runtime-epsilon`); Adoptium ships Temurin 25 GA (now most-recent LTS) and
  `temurin-25-jdk` is in the `bookworm` apt pool (amd64 + arm64). Added `temurin-25-jdk` + the
  `/opt/java-25` symlink to the Dockerfile and `25` to `ImageJavaRuntimes.bundledMajors`
  (`setOf(8,17,21,25)`). Java 26 is *not* bundled — it appears only on snapshots, which the release-gate
  skips. Rebuilt + smoke-tested: all four JDKs resolve (`25.0.3` LTS), `$JAVA` defaults to 21, non-root
  uid 1000, tooling intact; image ~2.08 GB (was ~1.6 GB). New test `bundlingTheRequiredJavaMakesTheVersionSupported`
  pins 26.2→Java 25 now booting. grinder 45/45 unit green (+2 gated IT).

- **Audit remediation on `claude-grinder-followups` (2026-07-29):** `/audit` over the 7 follow-up commits
  reported 0 HIGH / 4 MEDIUM / 5 LOW; all fixed or closed with evidence.
  **M1** — `VerdictStore.kt` carried a literal **NUL byte** (`"${slug}\x00${loader}"` instead of a space),
  which made git store a Kotlin source as **binary**: no textual diffs, useless blame, textually
  unresolvable merges — and it left `InMemoryVerdictStore` keying by NUL while `JsonVerdictStore.keyOf`
  used a space. A repo-wide byte scan found it was the *only* affected tracked source. Replaced with a
  space; the committed blob is now text and forward diffs render normally (verified).
  **M2** — the CurseForge search contract was unverified magic numbers. Verified against CF's REST docs
  (`pageSize` max 50, `index + pageSize <= 10000`, `sortOrder`, `downloadCount`, `links.websiteUrl`,
  `x-api-key`); the docs render `ModsSearchSortField` *without names*, so `sortField=6` = TotalDownloads
  was corroborated against PrismLauncher's `FlameAPI` sort table (the original assumption was right).
  Named the constants with their sources, added `warnIfNotDescending` so an unsorted page is logged rather
  than silently changing which projects get fetched, and documented that ordering never depended on the
  API (`GrindPool` re-sorts by popularity). New tests pin the request contract + the pageSize guard.
  **M3** — the fish `--no-empty` fix was only covered by the daemon-gated matrix IT, so it could regress
  silently in a *published* resource. Added `-api`'s `ScriptTemplateContentTest`: pins the construct at
  source level (**verified to fail when the bug is reintroduced**) and runs `fish -n` over both fish
  templates when a fish binary exists, skipping otherwise.
  **M4** — suspected sibling bug in `cleanServerFiles`' comma split **investigated and closed as a
  non-issue**: bash's `IFS="," read -ra` keeps empty fields too, and both shells hand the empty token to
  `find -name ""`, which matches nothing. Confirmed empirically in the container (both shells: 3 fields,
  deleted exactly the target, keeper untouched). Left unchanged deliberately and documented.
  **LOW** — one `PACK_MOUNT` const instead of three; all `!!` removed from touched files (the IT now
  resolves each cell's loader version once into a map that doubles as the validity filter);
  the IT's process-wide `ApiProperties` mutation is scoped and restored in a `finally`.
  Suites after remediation: **api 228 (1 skip), clientside 53, grinder 60 (3 daemon-gated)**, all green.
  Not fixed (history-only): the audit's L1/L2 — a refactor mixed into `913e6462f` and a fix landing with
  its first test in `14c319e4b` — remediable only by rewriting history; left for Griefed to decide.

- **Full script-template matrix + per-platform verdict dedup (2026-07-29, branch `claude-grinder-matrix-dedup`):**
  **(c)** Ran the whole grid `{1.12.2, 1.16.1, 1.20.1} × {Forge, NeoForge, Fabric, Quilt} × {bash, fish}`
  plus the `.ps1` parse check. **bash ≡ fish in every cell** — the parity this harness exists to prove.
  N/A cells (NeoForge <1.20, Fabric/Quilt on 1.12.2) were filtered correctly by the step-3
  `LoaderVersionResolver` gate. Two real outcomes: **(i)** Quilt on **1.16.1** fails in *both* shells —
  `Quilt Installer requires Java 17 or greater` while 1.16.1 pins `$JAVA` to Java 8 (Mojang's declared
  requirement), i.e. installer and server need different JDKs; a genuine template/toolchain constraint,
  left for Griefed. **(ii)** A first pass with 3 workers produced **8 spurious failures**
  (`start.sh: line 144: Killed "$JAVA"` — container OOM mid "Preparing level"); every one passed on a
  serial re-run, so `SPC_GRINDER_TEMPLATE_WORKERS` now defaults to **1** with the reason documented — a
  false FAIL in a correctness harness is worse than a slow pass. Also found: pwsh cannot *boot* `.ps1` on
  Linux at all (the template calls Windows `CMD /C`), so PowerShell is covered by a parser check instead.
  **(d)** Verdict dedup moved from `slug + loader` to **`platform + slug + loader`** via a shared
  `verdictKey()` (both stores, so the schemes can't drift as they once did). Slugs aren't globally unique —
  `jei` is on Modrinth *and* CurseForge — and the old key meant one platform's verdict overwrote the
  other's *and* made it look already-ground, so it was never verified. `GrindCandidate` now carries its
  platform (`ModPlatforms`), `hasVerdictFor`/`newestVerification` are platform-scoped, and `Grinder` warns
  if a candidate's platform disagrees with the resolved report's (that pair would re-grind forever). No
  store migration needed. grinder 64/64 green.

- **Shutdown drain, Quilt installer JDK, 1.21 line + LegacyFabric (2026-07-29, branch
  `claude-grinder-drain-quilt-versions`):** three follow-ups, each verified rather than assumed.
  **Shutdown drain:** `DockerJavaContainerEngine` now tracks its containers and is `AutoCloseable`;
  `close()` force-removes in-flight ones, since `run`'s `finally` is skipped when the JVM dies mid-boot
  (a `SIGTERM` had left a Minecraft server running). The hook is registered right after the engine is
  built so it covers one-shot runs too, and `GrindPool.requestStop()` abandons the queue after the current
  candidate. Proven on a live daemon ("Removing 1 container(s) abandoned by an interrupted run").
  **Quilt installer JDK:** Quilt could not install on old Minecraft at all — its installer needs Java 17+
  while 1.16.1 must run on Java 8. All three templates now run modloader installers via
  `runInstallerJavaCommand`, honouring an **optional** `JAVA_INSTALLER` from `variables.txt` and falling
  back to `JAVA`, so existing packs are untouched and no new placeholder plumbing was required; the
  misleading "check your internet connection" message now names the real cause. The grinder supplies it
  from `ImageJavaRuntimes.installerJavaPath()` (newest bundled ≥17). Quilt 1.16.1 went from failing in both
  shells to reaching the ready-line in both. Gotcha found while fixing: each boot path writes its own
  variables, and the matrix IT had its own `prepareUnattended` call that also needed the parameter.
  **Coverage:** matrix defaults grew to `{1.12.2, 1.16.1, 1.20.1, 1.21.1, 1.21.11}` ×
  `{Forge, NeoForge, Fabric, Quilt, LegacyFabric}` — LegacyFabric's first tests anywhere. Ran the new
  cells: **all green**, incl. LegacyFabric 1.12.2 and the whole 1.21.1/1.21.11 rows for the four modern
  loaders, with N/A correctly filtered (LegacyFabric ≥1.14, Fabric/Quilt pre-intermediary). Unit coverage
  added for the 1.21 line (two-digit patch deliberately) and LegacyFabric's era in
  `LoaderVersionResolverTest`/`ImageJavaRuntimesTest`. Suites: api 229, clientside 56, grinder 68 — green.

- **Audit remediation on `claude-grinder-drain-quilt-versions` (2026-07-29):** `/audit` over the branch
  reported 0 HIGH / 4 MEDIUM / 5 LOW; all closed.
  **M1** — a scripted docs edit had rewritten *all* of `CLAUDE.md` (CRLF→LF, 179 lines changed where only
  **3** were content, per `git diff --ignore-all-space`). Original CRLF restored, so the docs diff is the
  three table rows it claims to be; the history was rebuilt (below) so the churn never lands.
  **M2** — the `JAVA_INSTALLER`-*unset* fallback, which is the branch every real pack takes, was executed by
  nothing: all three boot paths passed the override unconditionally. Now
  `ImageJavaRuntimes.installerJavaPathFor(mc)` supplies it **only** when the server's Java is older than the
  installer minimum, so modern-Minecraft cells run the fallback for real (verified: Quilt 1.20.1 boots with
  no `JAVA_INSTALLER`; Quilt 1.16.1 still boots with it).
  **M3** — the `.ps1` change shipped parse-verified only. Added a test that extracts
  `RunInstallerJavaCommand` from the shipped template via the PowerShell **AST**, stubs `CMD`, and executes
  both branches on Linux pwsh (`unset → /server/java8`, `set → /installer/java21`) — real execution of
  template code whose production path needs Windows.
  **M4** — the Quilt commit had bundled api templates + a new grinder API + a signature change + call sites
  + test infra. The branch's last three commits were rebuilt into concern-separated commits (api template
  fix · grinder installer-JDK · seam cleanup · coverage · docs), each compiled in turn.
  **LOWs** — container cleanup moved onto the `ContainerEngine` seam (`AutoCloseable` with a no-op default)
  so the seam's own "always removes it" contract is enforceable and any engine can be drained (L1); two
  shutdown hooks collapsed into one with defined ordering (L4); a stray cross-subject assertion dropped
  (L2); the drain IT tidied — `DockerClient` imported, no shadowed `engine` (L3). Idioms were already clean.

- **Catalog crawl cursor on `claude-grinder-catalog-cursor` (2026-07-29):** answered "left alone long enough,
  will the grinder check *every* mod on CurseForge and Modrinth?" — it would not. Both sources restarted at
  offset 0 on every call (`candidates(limit)`), so the continuous loop re-fetched the *same* top-25 per
  platform forever: ~50 projects ground for the lifetime of the service, rank 26 unreachable, and after the
  first pass every pass was a no-op until the 30-day TTL. Fixed in four concern-separated commits:
  **(1) offset paging** — `CandidateSource` becomes `platform` + `page(offset, limit): CandidatePage`
  (candidates + `nextOffset` + `endOfCatalog`). `endOfCatalog` is the load-bearing bit: true only when the
  platform genuinely ran out (or CF hit its index cap), **never** on a failed request, because the crawler
  wraps to 0 on it and a transient 503 deep in the catalog would otherwise reset the whole crawl to the
  popular head. **(2) the cursor** — `CatalogCursor` (offset + completed sweeps), `CursorStore` with an
  in-memory double and a `JsonCursorStore` (temp-then-atomic-move like the verdict store, corrupt → start),
  and `CatalogCrawler` handing out the next slice per source per pass: advance by what was handed out, wrap +
  count a sweep at the end, keep the position on a failed page, skip a *throwing* source, and on a
  past-the-end position wrap **and** take the head slice in the same pass (guarded by `offset > 0` so an
  empty catalog can't spin). **(3) work-driven pacing** — `Grinder.grind` returns a `GrindOutcome`
  (VERIFIED/FAILED/SKIPPED_FRESH) and `GrindPool.grindAll` returns the verified count, feeding the pure
  `GrindPacing.pauseAfterPass`: no pause while work keeps turning up, a short `SPC_GRINDER_SCAN_DELAY` while
  only scanning past fresh verdicts, the long `SPC_GRINDER_INTERVAL` once a full sweep found nothing due. A
  fixed per-pass sleep was the second ceiling — 25 projects per 6 h cannot cover 71 000. Failures deliberately
  don't count as work, so a broken host throttles instead of racing the cursor past thousands of unverified
  projects. **(4) wiring + docs** — `SPC_GRINDER_BATCH` (replacing the two per-platform `*_LIMIT` knobs, whose
  "top N" meaning no longer existed), `SPC_GRINDER_CURSORS`, `SPC_GRINDER_SCAN_DELAY`; README gained the
  sweep-time arithmetic and the coverage ceilings.
  **Measured, not assumed:** probed the live Modrinth API — `total_hits` 71 267 for `project_type:mod`,
  offset 40 000 serves real projects, offset clamps at 99 999 — and kept it as `CatalogCrawlLiveIT` (gated
  `GRINDER_LIVE_IT=1`): consecutive live batches return different projects, a fresh crawler over the same
  cursor file resumes, deep offset works. **Honest limit:** CurseForge's `/mods/search` refuses
  `index >= 10 000`, so CF coverage is capped at its 10 000 most-downloaded mods however long the service
  runs; the cursor cannot fix that (it needs a partitioned search) and the cap is now logged, tested and
  documented rather than silent. Suite: grinder 93 run + 8 gated, green.
  **Bug found by that verification, fixed here:** `SIGTERM` mid-pass killed the JVM with a bare
  `Exception in thread "main"` — the shutdown hook interrupts the main thread, which is parked in
  `GrindPool.grindAll`'s `Thread.join()`, and the `InterruptedException` escaped `main`. Pre-existing (the
  hook and the join were both already there), reproduced as a failing test first, then fixed: `grindAll`
  catches it, requests stop, restores the interrupt flag and returns the count so far. Re-verified live —
  clean shutdown, no leaked containers.

- **CurseForge partitioned crawl on `claude-grinder-catalog-cursor` (2026-07-29):** closed the coverage hole the
  cursor work had left explicit — CF's `/mods/search` refuses `index + pageSize > 10 000`, so *one* query can
  never expose more than the 10 000 most-downloaded mods no matter how long the service runs. **Researched
  before designing** (no API key here, so the contract had to come from documentation): `docs.curseforge.com`
  confirmed the `index + pageSize ≤ 10 000` cap, `pageSize ≤ 50`, the `gameVersion` / `modLoaderType` /
  `categoryId` filters, `sortOrder` asc|desc, `pagination.totalCount`, and the
  `/games/{gameId}/versions` → `data: [{type, versions[]}]` shape; PrismLauncher's Flame integration corroborated
  the namelessly-documented `ModLoaderType` enum (Forge 1, Cauldron 2, LiteLoader 3, Fabric 4, Quilt 5,
  NeoForge 6) — the same source already cited for `sortField=6`.
  **Design:** the catalog is walked as a *sequence* of bounded queries — unfiltered catalog first (its top
  10 000, i.e. exactly what the crawl did before), then every game version newest-first, a version whose
  `totalCount` exceeds the cap re-crawled per modloader (the part that actually reaches past 10 000), and a
  loader slice still over the cap crawled ascending too (bottom 10 000 → ≤20 000 per slice fully covered).
  Splitting only where a *reported* count demands it keeps a sweep at ~1 request per version instead of per
  version×loader, and `totalCount` rides along on every response so sizing costs nothing (the one probe case is a
  slice resuming exactly at the cap). All six loaders are crawled, legacy ones included — one request each versus
  making their mods unreachable.
  **Plumbing:** traversal state travels as an *opaque, source-defined* token (`CatalogCursor.partition` /
  `CandidatePage.nextPartition`) that the crawler persists and replays verbatim, so the crawler never learns what
  a game version is and a partitioned crawl is restart-safe. `CurseForgePartition.parse` falls back to the start
  of the sweep for any unreadable token, and a `cursors.json` from before this change (no `partition` key) still
  loads — both pinned by tests.
  **Kept honest rather than optimistic:** the version list refresh happens at sweep start and *degrades* to the
  unfiltered top 10 000 when unavailable; a failed request or probe keeps its position; and the two residual gaps
  (a >20 000 (version, loader) slice losing its middle, a loader-less mod beyond its version's cap) are logged
  with counts. Caught while implementing: the existing `warnIfNotDescending` would have cried wolf on every
  ascending slice — it now follows the partition's direction (`warnIfMisordered`).
  **Verification status — explicitly incomplete:** the traversal is a pure function with 14 unit tests and the
  source has 18 canned-JSON tests, but **nothing here has ever touched the real CurseForge API** (no key, the
  module's oldest open item). The README and module CLAUDE.md say so, and name the log lines to watch on a first
  keyed run. Suite: grinder 120 run + 8 gated, green; the Modrinth live IT still passes.
  **Third axis (`categoryId`) added the same day.** The loader split left two holes: a mod with no modloader tag
  was unreachable past its version's top 10 000 (it appears in no loader slice), and a (version, loader) slice
  above 20 000 lost its middle. Researched first: `/categories?gameId=&classId=` is documented with `isClass`
  separating the class from its categories — but CF's own support docs **disagree** on whether a category is
  mandatory (the submission guide calls the main category required; the project-creation page lists only the
  class as required). So the axis is added *alongside* the loader stage rather than replacing it: an over-cap
  version is crawled per loader **and** per category, which makes a mod reachable if it carries *either* tag —
  ~6 extra requests per over-cap version to remove a silent hole. A category slice past both sort directions is
  narrowed by loader (version × category × loader, the deepest the API expresses). Every category is crawled,
  children included, because "does a parent category include its children" is undocumented. Residual gap is now
  a single deepest slice above 20 000 (logged with a count) plus mods with neither tag (undetectable).
  **Bug found by the new tests, and it was a real one:** the axis lists were read only when `partition == null`,
  i.e. at sweep start. A daemon restarting *mid-sweep* resumes with a partition token and an empty in-memory
  list, so the plan found no next partition, reported the catalog finished and **wrapped — discarding exactly
  the position the cursor exists to preserve**. Introduced by the partitioning commit earlier the same day;
  fixed by also re-reading a list whenever it is missing, and pinned by
  `resumingMidSweepFetchesTheAxisListsItHasNotGotYet`. Kept in the same commit as the category axis because the
  category-stage test that exposed it cannot pass without the fix. Suite: grinder 133 run + 8 gated, green;
  Modrinth live IT still passes.
  **Live-verified with a real API key (2026-07-30) — and the docs turned out to be badly insufficient.** Griefed
  supplied a `CURSEFORGE_API_KEY` via the macOS Keychain (no file, no transcript). Probing the real API before
  touching code found **two silent design-killers** in the partitioning that had been derived from
  documentation:
  1. **`pagination.totalCount` saturates at the paging cap.** The whole catalog, `gameVersion=1.12.2` and any
     slice above 10 000 all report exactly `10 000`; only genuinely smaller slices report a true size. Every
     split condition had been written as `> CAP` (and `> 2 × CAP` for the category→loader narrowing), so **not
     one of them could ever fire** — the "partitioned" crawl would have covered the top 10 000 of each version
     and nothing more, silently, and `warnIfSliceIsUnreachable` was dead for the same reason. All rules now key
     off `>= CAP` ("saturated ⇒ at least this many, possibly far more"), which is the only signal the API gives.
  2. **The version axis was 98 % junk.** `/games/432/versions` returns **7 339** strings over 36 version types,
     including Forge version families (`47.0.42`) and types named `Server Side`, `Shader Loader`, `Addons`,
     `DO NOT USE - Grouped MC Versions`. Filtering to types whose name starts with `Minecraft ` (via
     `/games/432/version-types`) leaves **135** real versions — a 54× smaller axis; unfiltered, a sweep would
     have burned 7 200 requests on partitions that can hold no mods.
  A third fix came from the live data too: `sortOrder=desc` only *trends* by downloads (one adjacent inversion in
  a 10-mod page) and `asc` is not ordered at all, so `warnIfMisordered` would have cried wolf on ordinary pages;
  it now checks the descending **trend** (first vs last) and skips ascending, which does reach the tail and is
  what makes the both-ends crawl worth an extra ~10 000 mods per slice.
  **Assumptions that held:** the cap applies to `index + pageSize` (9 950+50 served, 9 951+50 refused); the
  modloader filter is honoured and maps as PrismLauncher documents (1.16.5 → Forge 10 000 / Fabric 3 344 /
  Quilt 377 / NeoForge 238, and `sodium` appears under Fabric but not Forge); 0 of 100 sampled mods lack a
  category. One assumption was *disproved in the safe direction*: a parent category does **not** reliably include
  its children (3 of 6 sampled child mods invisible under the parent), which is exactly why the crawl already
  visited all 52 categories rather than the 23 parents.
  All of it is now pinned by **`CurseForgeCrawlLiveIT`** (gated `GRINDER_CF_IT=1` + a present key, ~40 small
  calls), including the saturation fix end-to-end: `1.12.2` paged out at 10 000 continues into `1.12.2|*|1|desc`
  with real candidates instead of declaring the catalog finished, and consecutive batches advance and survive a
  restart (jei/mouse-tweaks → geckolib/cloth-config → placebo/waystones). Suite: grinder 134 offline + 18 gated
  (12 of them live-API), all green. Still unproven: a full sweep, which is weeks of wall-clock and a large slice
  of the key's quota.
  **Supervised live run (2026-07-30) — the CurseForge *grind* path proven, plus one more interrupt bug.** Two
  runs. (a) Continuous mode with both sources wired for the first time: `Modrinth + CurseForge`, 135-version and
  52-category axes reported correctly, 12 candidates/pass popularity-interleaved across platforms, `cursors.json`
  carrying both platforms (CF partition `*|*|*|desc`), report server 200 on `/` and `/export.csv`, clean
  `SIGTERM`, no leaked containers. **Caught a self-inflicted trap first:** the initial attempt logged
  `covers 7339 game version(s), newest first (65.1.0)` — the *unfiltered* axis with a Forge version at its head —
  because the run used a `installDist` jar one commit older than the version-type filter. `test` does not rebuild
  the dist; recorded as a landmine. (b) One-shot grind of `curseforge.com/minecraft/mc-mods/curios`: resolve →
  download → mod scan → containerised loader install (network, 174 MB for 1.20.6/Forge + 161 MB for
  26.2/NeoForge, both `.spc-installed`-marked) → **offline** mod boot (7 × `UnknownHostException`, `/opt/java-25`,
  `Compatibility level set to JAVA_25`) → ready-line `Done (4.684s)! For help` on NeoForge/26.2 and
  `Done (7.161s)!` on Forge/1.20.6 → two `LOW` verdicts (correct: `curios` declares server/both and did not
  crash, so `metadataServer -> Confidence.LOW`) → store → CSV. Ran inside Docker's 1.93 GiB VM despite the 3 GiB
  cgroup cap, peaking ~770 MiB.
  **Bug found and fixed:** `SIGTERM` on the one-shot path printed `Exception in thread "main"
  java.lang.InterruptedException` — the `CountDownLatch.await()` holding the report server open let the shutdown
  hook's interrupt escape `main`, the same defect fixed earlier inside `GrindPool.grindAll` for the continuous
  path. Now caught and logged as "Report server stopped"; re-verified by SIGTERM against a rebuilt dist.

- **Loader-install reuse + crash re-check (2026-07-30):** Griefed spotted that the install cache, while keyed
  uniquely on `(Minecraft, loader, loaderVersion)`, churns — `BootVerifier` always booted
  `LoaderVersionResolver.latest`, so every loader release minted another ~150 MB install for a server that boots
  mods identically. Two changes, deliberately paired. **(1)** Extracted `LoaderVersionPolicy` in `-clientside`
  (`preferredVersion` = what to boot, `latestVersion` = authoritative newest; `LoaderVersionResolver` answers both
  the same, so the default path is unchanged) and gave the grinder `CachedLoaderVersions`, which prefers the
  most-recently-used installed build for the pair — most-recently-used so the sweep stays on one build and keeps it
  warm against the new eviction instead of rotating. Raw versions are recovered from the completion marker, not the
  sanitized directory name. **(2)** The safeguard that makes (1) admissible: `BootVerifier` now re-boots a CRASHED
  outcome on the **newest** build whenever the crash happened on an older one. Without it, a mod merely needing a
  newer loader fails to load, exits non-zero, classifies as CRASHED, and is published as a HIGH-confidence
  clientside mod — precisely the false HIGH this module is built to avoid. `latestVersion` also still drives the
  support gate, so a cached build can never revive an unsupported loader/Minecraft combination.
  Decisions kept pure and unit-tested (`shouldRecheckCrash`, `reconcileRecheck`) since `verify` needs an
  `ApiWrapper` + generation + a live server: crash-then-survive takes the newest verdict and says why,
  crash-then-crash keeps CRASHED with the newest evidence, and an **INCONCLUSIVE re-check leaves the crash
  standing** (a flaky second boot is not evidence). Suites: api / clientside (63) / grinder (147+18 gated) / app
  all green, no new warnings.

- **Crawl cursor advances on work done, not hand-out (2026-07-30):** the live sweep exposed that
  `CatalogCrawler.nextBatch()` committed each source's position the moment candidates were handed out. Restarting
  the daemon mid-pass — which happened twice that day, to pick up new builds — abandoned the remainder of the
  in-flight batch while both cursors had already moved past it, so those projects were silently deferred to the
  *next full sweep* (~7 weeks at the measured 60 projects/hour). Griefed asked for the correct fix rather than the
  cheap round-robin one. Split into two phases: `nextBatch()` moves nothing and returns the batch plus a
  `CrawledPage` per source (its cursor-at-start, candidates, end-of-catalog flag, continuation), and
  `commit(batch, reached)` advances each source only past pages whose candidates were **all** reached, stopping at
  the first that was not. `GrindPool.grindAll` now returns `GrindPass(reached, verified)`; *reached* deliberately
  includes fresh-skips and failures (a poison candidate must not stall the sweep) but only after `grind` returns,
  so a candidate still being ground during teardown comes back next pass. Commit granularity is per **page**, not
  per candidate, because a partitioned source can cross partitions inside one page — re-handing a page costs a
  fresh-verdict skip, while per-candidate positions aren't recoverable from outside the source. A sweep counts
  only when the page that ended the catalog was itself fully ground. Nine new tests pin it, incl. the
  wrap-with-un-ground-head case. Suites: clientside + grinder (156 run, 18 gated) green, no warnings.
  **Process note:** a scripted edit computed its slice boundaries backwards (`grindAll` lives *after* the enum),
  so `str.replace("", …)` inflated `Grinder.kt` to 18 MB; restored from HEAD and redone with anchored edits.
  **Round-robin ordering across platforms (2026-07-30, follow-up):** the hour-later check on the live sweep showed
  the fairness half of the same problem — `GrindPool` sorted each batch by `popularity`, and CurseForge's counts
  run several times Modrinth's for equivalent mods (`jei` 602 M vs `fabric-api` 218 M), so *every* CF candidate
  outranked *every* Modrinth one. Measured: 65 min of grinding produced 108 CurseForge projects and **zero**
  Modrinth ones, and with a ~2-hour pass any shorter interruption meant Modrinth never progressed at all. The
  two-phase commit prevents *loss* but not starvation. `GrindPool.interleaveByPlatform` now rotates one candidate
  per platform per turn, keeping each platform's own most-downloaded-first order and dropping a platform out of the
  rotation when it runs out. The deeper justification: the two counts are not comparable in the first place (CF
  counts file downloads across every version, Modrinth counts differently), so ranking them against each other was
  a category error that silently promoted one platform for the whole run. Four tests, incl. the one that states the
  goal — an interrupted pass must have reached both platforms. Every doc claiming a global popularity sort was
  corrected in the same commit. Grinder suite 160 run + 18 gated, green.

- **Operator-facing logging (2026-07-30):** Griefed asked whether an admin can see what the grinder and its
  workers are doing. Audit: the daemon *did* have a live rolling log (`~/.spc-grinder/logs/serverpackcreator.log`,
  log4j `ApplicationLogger`, worker thread in every line), but the boot containers had **nothing** — the engine
  streamed with `withFollowStream(true)` yet only did `lines.add(line)`, and the per-attempt `boot.log` was written
  by `outcomeFor` *after* the run, so a hung boot was undiagnosable until its 12-minute timeout and a killed boot
  left no output at all. The report server exposed only `/` and `/export.csv` — results, never activity. Three
  additions:
  **(1) Live per-boot console.** `ServerRunner.run` gained an `onLine` sink (defaulted, so indifferent callers are
  untouched; `ServerRunner` stopped being a `fun interface` briefly for that and was reverted — a functional
  interface may not default its abstract method's parameters, so the sink is explicit and the three SAM fakes took
  a third `_`). `BootVerifier.runPrepared` appends+flushes each line into the attempt's `boot.log` while the boot
  runs; `DockerLoaderInstaller` does the same into `<tuple>/.spc-install.log` (the slow cold-cache phase).
  **(2) `/status`** — `GrinderStatus`/`StatusSnapshot` served as Jackson JSON: uptime, current pass, each busy
  worker with its candidate and `busySeconds`, crawl cursor per platform, installed-tuple count. Snapshot is a
  copy, absent collaborators render `null` rather than 500, and slugs are serialized rather than string-built.
  **(3) One INFO line per candidate** (`Grinding <platform>/<slug>` … `Done … → Forge=LOW`), fresh-skips at DEBUG
  so they cannot bury it.
  **Bug found by the new tests:** `outcomeFor`'s final `logFile.writeText` was unguarded, so an unwritable boot log
  propagated out and failed a verification that had already run — pre-existing, now wrapped, pinned by
  `anUnwritableLogFileDoesNotFailTheBoot`. Suites: clientside 67, grinder 170 run + 18 gated, app — all green,
  no warnings.

- **Diagnosing the "465 projects/hour" sweep, and four fixes (2026-07-30):** the hourly check showed throughput
  jumping 10× while the loader cache stayed frozen at 44 tuples — the tell that verdicts were being produced
  *without booting*. In the log window: 10 candidates ground, 18 boots attempted, **every one INCONCLUSIVE**. The
  new logging paid for itself: `DockerLoaderInstaller`'s failure dump showed NeoForge `21.1.247`'s
  `-installer.jar` returning 404 (the version *is* in maven metadata — verified by hand), and the new per-boot
  consoles showed `Fabric is not available for Minecraft 26.1.2 / 26.2` across **103** boot directories.
  **Griefed's question — did the grinder boot a version the mod never listed? — answered: no.** CurseForge's own
  `latestFilesIndexes` matrix lists `26.1.2 modLoader=4` (Fabric) for Croptopia, so the mod does claim it;
  `BootCandidateSelector` pairs each file with *its own* declared versions and was correct. The wrong party was the
  loader-support gate: Fabric's meta lists 26.1.2 and returns a **placeholder `0.0.0` intermediary**, so
  `Meta.isMinecraftSupported` says yes and `start.sh` then aborts. Layer 2 (setup-abort → INCONCLUSIVE) caught it
  every time, so no false HIGH — but each occurrence wasted a boot.
  Fixes: **(1)** `LoaderCache.failureCooldown` (1h, in memory) so a broken tuple is not re-installed per candidate;
  **(2)** the live install console moved out of the cache dir, which `LoaderCache` wipes on failure — deleting the
  evidence exactly when needed (a defect in the logging shipped an hour earlier); **(3)** `BootVerifier` logs *why*
  a boot was inconclusive, at the source, instead of leaving only `boot:INCONCLUSIVE`; **(4)** `LoaderSupportMemory`
  — learn from the abort: a `(loader, Minecraft)` combination whose console says the loader has no build is
  recorded and dropped from candidate selection (24h expiry so upstream can catch up), keyed off a deliberately
  narrow `BootLogClassifier.loaderUnavailable` so the Java/EULA/variables aborts cannot poison good combinations.
  Suites: clientside 75, grinder 175 run + 18 gated, app — green, no warnings.
  **Fix (4) reverted the same hour, on evidence.** Within minutes of deploying, `LoaderSupportMemory` had marked
  Fabric unusable for **22 Minecraft versions** — 1.19.2, 1.20.x, 1.21.x through 26.2, i.e. every version Fabric
  actually supports. The marker was wrong, not the data: `default_template.sh:316` raises
  `"Fabric is not available for Minecraft X"` when `FABRIC_AVAILABLE != 200`, and that variable holds an HTTP
  status from a `curl`/`wget` probe that **cannot succeed under `--network none`**. The message means "I could not
  check", not "unsupported". Left running, the fix would have deleted Fabric from the sweep — trading wasted boots
  for a silent coverage hole, a strictly worse outcome. Reverted: the gate, the recording, the class, its tests and
  the misleading `BootLogClassifier.loaderUnavailable` predicate are all gone; the *finding* is kept as a landmine
  in `serverpackcreator-clientside/CLAUDE.md`. Fixes (1)-(3) stand and were verified firing in the live daemon
  (cooldown on NeoForge 21.1.247, install logs beside the pack, 38 inconclusive-reason lines).
  **The real open issue this exposed:** Fabric's offline path in the pre-baked install layer is incomplete —
  Forge/NeoForge/Quilt reach the ready line under `--network none`, Fabric aborts on an online availability probe.
  That is what needs fixing; suppressing the symptom was the wrong instinct. Also note the store now holds
  metadata-only verdicts for Fabric candidates that are "fresh" for a year (`SPC_GRINDER_REVERIFY_TTL_DAYS=365`),
  so they must be invalidated once the offline path works, or they will never be re-ground with a real boot.
  **Root cause fixed in the templates (2026-07-30).** `setupFabric` settled the launcher from the *network* before
  looking at disk: `default_template.sh:311-317` took the improved-launcher branch only on an HTTP `200`, and
  otherwise crashed on `FABRIC_AVAILABLE != "200"` — the **negative** form, which an unreachable network satisfies
  trivially. Quilt and LegacyFabric crash on the *positive* form (`== "[]"`), which is exactly why they booted
  offline and Fabric never did. All three templates now check for an existing `fabric-server-launcher.jar` /
  `fabric-server-launch.jar` **first** and return immediately when one is there — which is also correct for any
  user with a complete pack and no internet, not just the grinder. The pre-baked cache already contained
  `fabric-server-launcher.jar` (verified in `cache/1.14/Fabric/0.19.3/`), so no change to the install layer was
  needed. Pinned at source level by `ScriptTemplateContentTest.allTemplatesUseAnAlreadyInstalledFabricLauncher-
  BeforeCheckingTheNetwork`, which asserts the disk check *precedes* the probe in each template — a check that
  works without fish or pwsh installed.

### 2026-07-30 — the Fabric offline fix needed a second half, plus two bugs it exposed

**The offline short-circuit was only half a fix.** The disk-first check landed correctly, but it `return 0`-ed as
soon as it found the launcher — jumping over `setupFabric`'s closing
`SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"`. Every Fabric boot then ran
`java -Dlog4j2.formatMsgNoLookups=true do_not_manually_edit` and died with
`Could not find or load main class do_not_manually_edit`, i.e. still INCONCLUSIVE — and *every* assertion in
`allTemplatesUseAnAlreadyInstalledFabricLauncherBeforeCheckingTheNetwork` stayed green, because ordering was all it
checked. All three templates now fall through into the assignment (the network path moved into an `else`).
**New test with actual teeth:** `theBashTemplateStillBuildsARunCommandWhenTheFabricLauncherIsAlreadyInstalled`
extracts the bash `setupFabric`, sources it with `commandAvailable` denying curl/wget and every download/install
stub exiting non-zero, stages a launcher jar, runs it, and asserts the assembled command — verified to fail when
the `return 0` is reinstated. **Live confirmation:** `Modrinth/simple-voice-chat → Fabric=LOW(boot:SURVIVED)`,
whose `boot.log` shows `fabric-server-launcher.jar present. Moving on...`,
`-jar fabric-server-launcher.jar nogui`, and `Done (7.277s)! For help` under `--network none`.

**Bug found while verifying: the work tree grew without bound.** Staging keeps a full server pack (with the
overlaid loader libraries) per `(slug, loader)` and only deleted it when that same pair was retried — never, during
a catalog sweep. Measured: **98 GB across 1750 attempt directories, ~23 GB/h**. New `BootWorkspaceReaper` strips a
finished candidate's staging to its `boot.log`, in a `finally` (a thrown verification is exactly when garbage is
left), scoped to one slug by cutting the `-<loader>` suffix rather than prefix-matching (workers run in parallel;
`jei` must not reap `jei-extras`), plus a startup sweep for what a killed run left behind. First live startup
reclaimed **8 897 MiB, 8.7 GB → 155 MB**; per-candidate reclamation is ~700 MiB. 10 unit tests.

**Bug found while diagnosing: the test suites hijack a live daemon's home directory.** SPC resolves
`PathsConfig.homeDirectory` through `Preferences.userRoot().node("ServerPackCreator")` — one machine-wide per-user
node shared by GUI, web backend, test suites and grinder — and re-reads it on **every access**. A
`:serverpackcreator-api:test` run mid-session moved the *running* daemon's home to `serverpackcreator-api/tests`
and then deleted it, after which every boot failed on `server_files/server-icon.png: The source file doesn't exist`
and was recorded as a **metadata-only `boot:none` verdict** — indistinguishable, in the report, from "this mod was
never bootable". 30+ candidates were polluted before it was caught; the store was archived and the run restarted.
The preference wins over both cwd and `serverpackcreator.properties`, so `SPC_GRINDER_SPC_PROPERTIES` is no
defence. It cuts both ways: running the suites also relocates a developer's own GUI installation.

**OPEN QUESTION (needs Griefed's call):** the real fix is to make the preferences node name injectable so tests and
the grinder each get their own node — `ServerPackCreatorPathsConfigTest` and `ServerPackCreatorScriptTemplatesConfigTest`
already do exactly that, so the pattern exists and is simply not applied suite-wide. It is an *additive* API change
(optional ctor param / env var), but it touches published `-api` surface and changes where a test-suite run stores
state, so it was **not** implemented unilaterally. Until then: never run a test suite while a grinder run is live.

**Resolved (same day, Griefed's call: make the node injectable).** `ApiProperties.resolvePreferencesNode()` now picks
the `Preferences` node from `-Dde.griefed.serverpackcreator.preferences.node`, else `SPC_PREFERENCES_NODE`, else the
unchanged default `ServerPackCreator` (blank overrides fall back, since `userRoot().node("")` is the *root* node).
`GrinderApplication` claims `ServerPackCreator-grinder` before any `ApiProperties` exists and logs it; the build gives
every test JVM `ServerPackCreator-test-<module>`.

**The isolated node immediately exposed a second, worse fault — one this change introduced.** With no *stored* home in
a fresh node, `homeDirectory` fell through to the dev-build branch `File("").absolutePath`, i.e. the test JVM's working
directory = **the module's own source directory** — and `ApiWrapper.setup()` *writes* into the home (README.md,
CHANGELOG.md, `server_files`, `log4j2.xml`, `manifests/`). The clientside module's checked-in 186-line CLI guide was
overwritten by the bundled root README, which is what `ClientsideReadmeFlagsTest` then failed on: `--setup` documented
but unaccepted, nine real flags undocumented. `PathsConfig` therefore also honours
`-Dde.griefed.serverpackcreator.home` ahead of that fallback, and the build points every test JVM at
`<module>/build/spc-test-home`. `PathsConfigTest` and `ScriptTemplatesConfigTest` clear the property per test (they
exist to exercise the preference/properties/fallback layers, which an explicit override outranks).

**Verified:** all four suites green; a full api suite run *concurrently with a live daemon* left it untouched
(`Using Preferences node 'ServerPackCreator-grinder'`, `Home directory set to: ~/.spc-grinder`, zero references to the
repo test home); no README/LICENSE/manifests churn in `git status` after a full run.

**Correction to an earlier claim in this log's session:** the first "all four suites pass" reading counted the app
module as passing when Gradle had reported it up-to-date without executing anything. The two
`ClientsideReadmeFlagsTest` failures were real and pre-existing at that moment.

**Method note:** Java's macOS `Preferences` store is per-process cached and flushed on a ~30 s timer, so concurrent
JVMs clobber each other's view. Cross-process `defaults read` snapshots taken while Gradle JVMs are alive are not
evidence — an apparent "every suite writes the shared node" result was this artifact. Use in-process logs and
same-JVM tests.

**Still open:** whether anything writes the *shared* `ServerPackCreator` node during a build (it holds a repo test
path on this machine, affecting only a GUI/dev instance). The five remaining hard-coded call sites are all in `-app`.

### 2026-07-30 — sizing the real sweep surfaced a systematic false-HIGH source

Before starting the catalog sweep, the host was measured: 48 GiB RAM, 16 CPUs — but **Docker Desktop's VM held
1.93 GiB**, while `ContainerResources` caps each boot at **3 GiB**. The cap therefore cannot be honoured, and a fat
modpack mod is OOM-killed by the VM. Docker reports that as exit **137**, there is no ready-line, and the template's
`Killed "$JAVA"` line deliberately does not match `setupAbortMarkers` — which left `BootLogClassifier.classify` exactly
one outcome: `CRASHED`, promoted by `ClientsideVerifier.aggregate` to **HIGH confidence "this mod is clientside"**.
Purely from host memory pressure, and biased towards the *largest* mods. Over a months-long sweep whose entire
deliverable is the suspected-clientside list, that is a systematic poison, so it was fixed before the sweep ran:
`killedExitCodes` (137/143) and `outOfMemoryMarkers` now map to INCONCLUSIVE. `SIGABRT` (134) is deliberately still
CRASHED (a fatal JVM abort is a real failure), and a test pins that a genuine
`NoClassDefFoundError: net/minecraft/client/…` still reads CRASHED — verified to fail with the guard removed.

**Sweep sizing at one worker** (`WORKERS=1` is forced by the 1.93 GiB VM): ~60–90 s per candidate including boots, so
Modrinth's ~71 000 mod projects alone are ~2 months of wall-clock, both platforms interleaved considerably more.
`REVERIFY_TTL_DAYS=365` comfortably exceeds that (the sizing rule: TTL must be longer than a sweep takes).
**Raising Docker Desktop's memory is by far the biggest throughput lever available** — at 16 GiB the host could run
4 concurrent 3 GiB boots, roughly quartering the sweep. That is a Docker Desktop UI change (Settings → Resources)
which restarts the daemon, so it wants doing between sweeps, with `SPC_GRINDER_WORKERS` raised to match.

### 2026-07-30 — the grinder had never produced a single HIGH verdict, and why

Classifying the 26 % INCONCLUSIVE rate turned up something much worse than an efficiency problem: the store held
**131 MEDIUM, 387 LOW, 0 HIGH** after 3.5 h and 517 verdicts, while a kept boot log sat there containing
`java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft` — a textbook clientside crash. The one decisive
signal in the whole confidence model was being produced and then thrown away. Three causes, in the order found:

1. **The start scripts swallowed the server's exit status.** `default_template.sh`'s run loop ended in an
   unconditional `exit 0`, so `BootLogClassifier` saw `0` for every boot and took its `null, 0 -> INCONCLUSIVE`
   branch. Fixed in all three templates (`SERVER_EXIT_CODE`, captured immediately because the following checks
   overwrite `$?`). Correct for users too — systemd, Docker restart policies and CI all read that code — and pinned by
   a test that **executes** the extracted run loop for statuses 0/1/137, verified to fail with `exit 0` reinstated.
2. **The exit code is not trustworthy anyway.** With the template fixed, the reproducer *still* read INCONCLUSIVE. The
   new exit status in the boot detail gave the answer: **`exit 0`** — NeoForge's ServerStarterJar reports the crash in
   full and exits successfully. So `clientOnlyClassMarker` now decides **CRASHED from the console alone**, ahead of
   the exit-code logic, while staying subordinate to the timeout and killed/OOM guards so host trouble can never
   manufacture a HIGH. That produced the engine's first ever `NeoForge=HIGH(boot:CRASHED)` on `modelfix`.
3. **Missing dependencies were wasting boots** (Griefed: "the required dependencies should be downloaded as well in
   order to prevent exactly that"). Two causes: `pickDependencyFile` matched loaders strictly, so a Quilt boot dropped
   **Fabric API** — the canonical Quilt dependency, published only as Fabric files — 210 dropped deps overall, all but
   44 on Quilt; and an unresolvable ref was a **silent** `continue`. Now Quilt falls back to the Fabric build
   (one-way, deliberately not extended to Fabric→Quilt or NeoForge→Forge), every unstageable required dependency is
   collected and logged, and `refuseForMissingDependencies` **aborts staging instead of booting** — a mod the loader
   rejects for missing deps never runs its own code, so the boot cannot speak to sideness. 36 of 112 kept boot logs
   had failed exactly that way, ~70 s each.

**Corrections recorded:** the "missing dependencies become false HIGHs" hypothesis was **wrong** — they were already
INCONCLUSIVE, because cause 1 made *everything* INCONCLUSIVE; and the NeoForge `21.1.247` 404 was **not** the dominant
inconclusive cause (that build still produced 17 SURVIVED verdicts). Both were checked against the store before being
acted on, which is what redirected the work to the real fault.

**Consequence for the store:** all 517 verdicts predate the crash signal working, so none of them can contain a HIGH
and every one is fresh for 365 days — they would never be re-ground. Archived rather than kept.

### 2026-07-31 — Minecraft's two versioning schemes, and the Forge coverage they cost (B8 + sibling)

`REFACTOR-AUDIT.md`'s programme started with the backlog's biggest functional gap, and exploration turned it from
one bug into a class. All three start-script templates chose Forge's launcher era with `SEMANTICS[1] -le 16` — the
Minecraft **minor** component — which only carries that meaning under the `1.x` scheme. Minecraft `26.2` has minor
`2`, so every Forge boot on current Minecraft took the legacy `forge.jar` path and died with `Error: Unable to
access jarfile forge.jar` *before loading any mod*: **24 grinder boot logs, every one of them Forge, never started
the server.** Forge coverage on current Minecraft was effectively zero, and only harmless because
`launchFailureMarkers` scores a never-launched JVM INCONCLUSIVE rather than as a false clientside HIGH.

Sweeping for siblings rather than stopping at the observed symptom found a second instance: NeoForge's
1.20/1.20.1-only legacy installer coordinate (`SEMANTICS[1] -eq 20`) would send a future Minecraft `26.20` at a URL
that does not exist for it. Latent, and fixed anyway — an unreachable bug is cheaper to close than to rediscover.

Both tests **execute** the extracted shell function across both schemes and were confirmed failing against the
unmodified templates first (`"Minecraft 26.1.2 picked the legacy forge.jar launcher"`, `"Minecraft 26.20 was sent at
the legacy 1.20-era URL"`) — the pin-first order that audit finding M-A says this class of change keeps slipping on.

**The Kotlin side was surveyed and is clean**, which bounds the class: `BootCandidateSelector.minecraftComparator`
compares component-wise (and correctly ranks `26.2` above `1.21.1`, which is precisely what walked Forge into the
broken branch), `ImageJavaRuntimes` sources required-Java from `MinecraftMeta.requiredJavaVersion`, and
`LoaderVersionResolver` delegates to the manifests. The rule is recorded as a landmine in
`serverpackcreator-api/CLAUDE.md` so the next scheme change has one place to check.

**The era fix uncovered the actual blocker.** With Forge finally reaching its ServerStarterJar path, the boot
failed differently: `-Djava.security.manager=allow` — SPC's default `SSJ_FORGE_ARGS` — makes a Java 24+ VM refuse
to start outright (JEP 486 removed Security Manager support). Minecraft 26.x requires Java 25, so **every modern
Forge pack SPC generates died before Forge loaded**, and this is user-facing rather than grinder-specific. NeoForge,
Fabric and Quilt never pass the flag, which is exactly why only Forge was ever affected. All three templates now
pass it only below Java 24, guarded against the non-numeric `JAVA_VERSION` that `SKIP_JAVA_CHECK` leaves behind; the
default is unchanged because older Java still needs it.

**A second consequence worth knowing: the pre-bake cache had to be invalidated by hand.** The install boot runs the
same templates, so the cached Forge layers for 26.x had been produced by the legacy branch — no `server.jar`, and a
success marker that made `ensureInstalled` serve them anyway. Deleting the two affected tuples let them reinstall
correctly. **A template change that alters what an install produces requires invalidating the affected cache
tuples**, because the marker records success without recording which template produced it.

Verified end-to-end: `balm` on Minecraft 26.1.2 now reports `Fabric=LOW(boot:SURVIVED), Forge=LOW(boot:SURVIVED),
NeoForge=LOW(boot:SURVIVED)` — the first successful Forge boot on current Minecraft — with `server.jar` and
`libraries` present in the reinstalled cache entry.

### 2026-07-31 — Phase 2: grinder correctness and coverage (B12, B6, B9, B2)

Four backlog items, one commit each, all with the pin-first order the audit's M-A finding asked for.

- **B12** `ForgeLoader.forgeVersionFrom` sliced blindly, so an entry with nothing after its Minecraft key threw
  `StringIndexOutOfBoundsException` — uncaught by `update()`, which would have abandoned the Forge parse for every
  remaining Minecraft version. It now returns `null` and the caller logs and skips. The guard is a **length check**,
  never `startsWith`, because entries carry the raw manifest key while the Minecraft version may be the reconciled
  `1.7.10-pre4` form. Verified against the real manifest: of 5025 entries across 77 keys it rejects **0**.
- **B6** Modrinth clamps `offset` at 99 999 and answers with zero hits past it, which `page` cannot distinguish from an
  exhausted catalog — so a catalog that outgrows the ceiling would wrap early and report itself complete. Two warnings
  now mark the region; behaviour is deliberately unchanged, because the source genuinely cannot tell the cases apart
  and guessing either way is worse than saying so. The boundary is a pure decision, tested without an HTTP fetcher.
- **B9** The boot deadline was wall-clock, so a suspended host spent the budget on a frozen container. The wait loop
  now adds a detected suspend back to the deadline, so a timeout means "the boot had this long and did not make it".
  Detection is conservative on purpose (a gap must exceed a minute *and* 30× the poll interval): under-detecting only
  preserves the old behaviour, while over-detecting would hand a genuinely slow boot budget it should not get.
- **B2** A loader build can be listed by maven metadata while its installer artifact is absent (NeoForge `21.1.247`
  404s). With nothing cached the policy now prefers the newest build the cache is not already refusing, stepping down
  when it is on install cooldown; `LoaderCache.isInstallOnCooldown` lets the policy ask before choosing instead of each
  candidate discovering it the expensive way. **`latestVersion` still delegates**, so the support gate and the crash
  re-check keep measuring against the real newest — a crash on a stepped-down build is re-checked exactly as a cached
  build's is. Scoped to Forge/NeoForge, the only loaders with sibling per-Minecraft builds.

**Correction — a bookkeeping mistake, owned here.** The commit that closed B8 (`64d2d70e5`) truncated `BACKLOG.md` from
B8's heading to end-of-file, which silently removed **B9, B10, B11 and B12** as well. B10 and B11 were still open and
have been restored from history; B9 and B12 are closed by this phase, so they stay gone deliberately rather than by
accident. Removing a queue entry must cut only that entry's own section.

**Deferred, not forgotten: B5** (verdict dedup by project identity). It needs a stable project id threaded through
`GrindCandidate`, `GrindVerdict` and the store key, plus a migration for existing `verdicts.json` files that carry no
id — a schema change that deserves its own focused pass rather than the tail of a long one. Its cost today is only that
a renamed project is re-ground as new, which is wasted work rather than a wrong verdict.

### 2026-07-31 — Phase 3: API and app hygiene (B1, B10)

- **B1** Four call-sites in `-app` hard-coded both the `Preferences` node name and the home key while `-api` resolves
  the node through `ApiProperties.resolvePreferencesNode`, so a host claiming its own node — the grinder, every test
  JVM — had the app writing a home `-api` would never read back. `HomeDirectoryPreference` now owns both, resolving the
  node **per call** rather than capturing it. `CommandlineParserTest` reads through the same resolver, because
  asserting against the literal node only passes while the default happens to be in play.
  **`GuiProps` deliberately stays on the default node**, with the reasoning recorded at the call-site: window geometry
  belongs to the installation a user sees, not to whichever process resolved a home, and routing it through the
  resolver would reset every existing user's saved layout. It needs a migration, not a rename.
- **A bug found by B1's test, fixed first.** `PathsConfig.homeDirectory` persists whatever it resolves, so the `-D`
  override added in `dd4fcc935` was writing *itself* into the preference — a temporary override, which the build sets
  for every test JVM, quietly replacing the user's durable home, with every later read inheriting it. It is now honoured
  for the process and never persisted. Surfaced as the `--home` test failing: `CommandlineParser` stored a home and the
  next `ApiProperties` read overwrote it. Verified by reinstating the persistence and watching the new test fail.
- **B10** The 91-line `variables.txt` body moved out of a Kotlin string literal into `server_files`, beside the
  start-script templates. Verified as a faithful move — the resource is **byte-identical** to what the literal produced
  through `trimIndent` (91 lines, 5912 chars). Existing homes keep their copy (`checkServerFilesFile`, not the
  templates' overwrite), so edited wording survives an upgrade, and the delete-watcher restores it when removed so the
  file the operator edits is the file generation reads. Reading from disk introduces a failure the literal could not
  have, so it is guarded: a missing or unreadable template falls back to the jar's copy rather than shipping a pack with
  no `variables.txt`.

### 2026-07-31 — Phase 4: operator documentation (B7)

`SPC_GRINDER_WORKERS` is the biggest lever on sweep duration and the README documented only "budget ~3 GB RAM each".
§5 now carries the rule — `workers ≈ (memory available to Docker − overhead) / 3 GiB` — with figures for a dedicated
box (~20), a workstation (4) and a laptop on Docker Desktop's default (**1**), plus the two facts an operator actually
trips over: the constraint is the memory assigned to *Docker*, not the host's (measured: a 48 GB laptop whose VM held
1.93 GiB, less than one boot's cap), and over-subscribing wastes boots rather than corrupting results, because an
OOM-killed boot is scored INCONCLUSIVE. Keeping the host awake is noted for the same reason B9 exists.

`ReadmeConfigurationTest` now compares the quoted per-boot figure against `ContainerResources.memoryBytes`, since the
formula divides by it — change the cap and the advice would silently start over-subscribing. Verified by doubling the
cap and watching the test fail.

### 2026-07-31 — Phase 5: frontend component coverage (B3)

Suite 23 → 31, across two files. The judgement about *what not to test* is the substance here: of the untested
surface, only `MainLayout` held real logic.

- **`MainLayout`** — its drawer `linksList` is hand-maintained and must track the router, so the test compares it
  against `src/router/routes` **in both directions**: a routed page with no drawer link is unreachable from the UI, and
  a link pointing at no route goes nowhere. Comparing the array against a literal copy of itself would have passed
  forever regardless of what the app actually routes. `drawerClick` is pinned too, including the `stopPropagation`
  call — without it the click bubbles to the drawer and toggles the mini-state straight back, a break that leaves every
  other test green.
- **`AboutPage`** — no logic, so the test covers only what fails invisibly: an empty, relative or non-`https` link
  renders as a perfectly normal row and the only symptom is a user going nowhere.
- **Verified by breaking all three**: removing the History nav entry, removing `stopPropagation`, and dropping
  `https://` from the Discord link each fail with the intended message.
- **Deliberately still untested:** `SubmissionPage` (its only script content is two scrollbar style objects),
  `DownloadsPage`, `HistoryPage`, `ErrorPage` — pure composition — and the three tables, for 4e's reason. Covering them
  would raise the count without raising confidence.

Two harness facts cost time and are now recorded in the module's `CLAUDE.md`: `src/router/routes` *statically* imports
the two download pages, so importing it drags in `boot/axios` → `#q-app/wrappers` and needs `vi.mock('boot/axios')`;
and **QPage refuses to render outside a QLayout**, so a page test must stub it as a passthrough or the page's children
never mount — which is why the existing download-page tests assert through `vm` rather than the DOM.

### 2026-07-31 — Phase 6: the convention the session kept paying for (audit M-A)

The audit's M-A finding was that template and parsing fixes get verified by hand and pinned afterwards — a *habit*, not
an incident, found across two audits in three instances (`28a786b58` NeoForge mapping with no test; `2e16bf0c8` fish
`--no-empty` pinned five commits later; `1a55797df` Fabric fall-through, whose guard asserted ordering and stayed green
while the behaviour was broken). Phase 1 then supplied two more data points in the other direction: writing the test
first and *watching it fail* is what proved both the Forge launcher-era bug and its latent NeoForge sibling.

Two lines are now in the root `CLAUDE.md`'s **Refactor discipline**:

1. Shell templates, manifests and version parsing get their test written and observed failing first — with the reason
   (these fail silently, producing a plausible value rather than an error) and the measured cost (24 wasted boots; 820
   mis-attributed NeoForge versions).
2. A test that only asserts *shape* is not a pin — prefer executing the unit, and confirm the test fails before the fix.
   That second half is not theoretical: a teeth-check silently passed **twice in this session** because a mis-indented
   edit meant the supposedly-broken run was unmodified code. A guard whose teeth were never checked has repeatedly
   turned out to assert nothing.

**Also recorded (B13–B18):** six items observed while executing the plan but outside its phases — the unverified
fish/`.ps1` template changes (the gated matrix IT has not run since), `HostProcessServerRunner` sharing the wall-clock
deadline B9 fixed only in the container engine, the loader-cache marker not recording which template produced an
install, the checked-in test properties still carrying machine-specific absolute paths (M1's other half), `.gitignore`
hiding new `server_files` resources, and an install failure's console being wiped by the next attempt on that tuple.

## 2026-08-21 — CI/CD moved to Forgejo (`claude-forgejo-ci`)

`git.griefed.de` is **Forgejo 16.0.3**, not GitLab — verified against `/api/v1/version`. `.gitlab-ci.yml`
and its 22 jobs are deleted; CI now lives in `.forgejo/workflows`, and Forgejo is the origin of every
release.

**The constraint that shaped the whole thing:** `.forgejo/workflows` is *all-or-nothing*. Per a Forgejo
maintainer on [forgejo#9203](https://codeberg.org/forgejo/forgejo/issues/9203), *"If a project contains a
`.forgejo` and a `.github` folder, then the `.github` folder is ignored."* Forgejo had been running the
`.github` workflows as a fallback — that is where releases `9.0.0-alpha.2` … `.6` came from — so the
commit adding `.forgejo/workflows` is a **cutover**: everything Forgejo must do had to land in the same
change, or releases would simply stop. That is why this is one branch and not five.

**Nine workflows.** `test.yml` and `docker-test.yml` (the GitLab `Build Test` / `Docker Test` role),
`qodana.yml`, `release-generate.yml` (semantic-release), `release-build.yml` (assets, the Forgejo release,
Maven, Docker, the outward mirror and the VirusTotal scan), `devbuild.yml`, `docs.yml`, `update-readme.yml`.

**Decisions worth keeping:**
- **`uses:` references are the same `actions/...@<github-sha>` lines the `.github` workflows used.** The
  Forgejo docs recommend `https://data.forgejo.org/...` instead, but these exact references are *proven*
  to resolve on this instance — they built the existing alpha releases. Swapping them for a mirror whose
  commit SHAs may differ would trade something known to work for something merely recommended. install4j
  is the one deliberate exception and is fetched from GitHub by full URL, which needs the instance to
  permit that.
- **semantic-release keeps doing version + changelog + tag, and nothing else.** `@semantic-release/gitlab`
  and `gitlabUrl` are gone and `publish` is `false`; the release is created by the tag-triggered workflow,
  which is where the assets are. Same two-phase shape GitLab had, so the `releaseRules` that produce your
  version numbers are untouched. **The tag must be pushed with a real user token** — Forgejo, like GitHub,
  does not trigger workflows from pushes made with the automatic per-run token, so an automatic-token push
  would tag a release that never gets built.
- **`GitGriefed` Maven repository retargeted, name kept.** It pointed at
  `https://git.griefed.de/api/v4/projects/63/packages/maven` with a `Private-Token` header — a GitLab path
  and a GitLab auth scheme, neither of which exists on Forgejo. Now `/api/packages/Griefed/maven` with
  HTTP Basic. The repository *name* is unchanged so the generated task name CI calls,
  `publishMavenJavaPublicationToGitGriefedRepository`, still exists. GitHub Packages, gitlab.com and OSSRH
  are untouched: the move is off the self-hosted GitLab, not off gitlab.com.
- **Releases are mirrored by explicit API calls, not by a setting.** Forgejo push-mirrors replicate refs
  but not releases. The mirror job runs last and only on success, so no downstream forge advertises a
  release Forgejo does not have.
- **VirusTotal became a job in `release-build.yml`** rather than a release-triggered workflow: the assets
  are already there as an artifact, and `crazy-max/ghaction-virustotal` updates a *GitHub* release body,
  which is the wrong forge now. Submitted through VirusTotal's API, permalinks appended to the Forgejo
  release notes.
- **`docker-test.yml` does not push, and that is a deliberate change.** The GitLab job built with `--push`
  and tagged every commit's image on ghcr.io *and* Docker Hub, so every branch push published a public
  image nobody consumed. Restoring it is a two-line change, noted in the file.
- **Two regressions avoided by reading the code being replaced rather than skimming it:** `update-readme`
  sent its token in an `Authorization` header specifically so it could not leak into logs — the port keeps
  that instead of putting the token in the push URL; and Qodana's JBR `chmod` dance existed because
  GitLab's cache drops the executable bit, which `actions/cache` does not, so it survives as documented
  insurance rather than being deleted or blindly copied.

GitHub keeps a **smoke test** and the four `clientside-*` workflows (three `issues:`-triggered, one
`workflow_call:` helper they invoke). `github_release.yml`,
`github-prerelease.yml`, `devbuild.yml`, `update_readme.yml` and `virustotal.yml` are deleted from there.
`devbuild` additionally clears the stale GitHub `continuous` *release* while leaving its *tag*, so the
mirror recreates it from Forgejo.

**B26–B29 are dropped** rather than answered: they described GitLab dind and a GitLab-Pages-hosted Qodana
report. The infrastructure they were about no longer exists. The backlog is now empty.


## 2026-08-21 — the deferred performance items, B30/B31/B32 (`claude-perf-deferred`)

The last three backlog items from the startup/network work, cleared.

**B31 was the real win, and its entry was incomplete.** `VersionMeta` blocked on checking twelve manifests
before its constructor returned, although `ApiWrapper.setup()` had already seeded every one from the jar.
Same probe, same machine, constructing over the same home: **~399 ms median before (399/390/835), ~47 ms
after (56/36/47)** — and far more offline, where the old path waited out twelve connect timeouts before a
window appeared.

What the entry missed is why this could not be a straight move: the GUI's version dropdowns are
`DefaultComboBoxModel`s built once in `ConfigEditor`, and **nothing anywhere repopulates them**. A naive
background refresh would have hidden a freshly released Minecraft version until the next launch — the exact
workflow the application exists for. Two awaits close it: an `init` block in `ConfigEditor` placed *above*
the version-list properties (Kotlin runs initialisers in declaration order, so that is the only point that
works), and `ConfigurationHandler.checkConfiguration`, the single choke point every CLI, interactive, web
and embedder path passes through — one site instead of four, so a short-lived `--headless` run cannot reject
a version published minutes ago.

**B30** sends `If-None-Match` beside `If-Modified-Since`. `files.minecraftforge.net` ignores the timestamp
but honours the ETag, and it was the largest manifest still transferred in full every startup (121,492 B of
213,885 B). The ETag lives in a `<manifest>.etag` sidecar **with the manifest's byte length**, and is
offered only while that still matches: an ETag describes one exact body, so a manifest replaced by other
means — a re-seed from the jar is the real case — would otherwise earn a `304` for content we do not hold
and suppress a genuine update permanently. It is also recorded only when the manifest is actually adopted,
since the updater declines an upstream copy with fewer versions. Both guards were mutation-verified;
before the implementation existed three of the four passed vacuously, which is the only reason to trust
them now. A build-side hazard came with it: `updateManifests` copied `tests/manifests` unfiltered into the
shipped resources, so sidecars would have been packaged and seeded into every user's home. Excluded.
Scoped honestly, B30 buys ~0 ms — the twelve checks run concurrently and Forge is not the slowest — only
bandwidth on a metered link.

**B32** stopped `hasteBinPreChecks` materialising up to 20 MB of `char` to count characters; it now streams
through an 8 KB buffer and stops at the limit. Not replaced by a byte check, which is the obvious shortcut
and wrong: the method applies two independent limits and UTF-8 spends up to four bytes per character, so
200,000 `€` is 600,000 bytes but only 200,000 characters and must still be accepted. The characterization
tests caught a second mistake too — `File.length()` on a directory returns a small number, so the first
version's early return made the check *accept* a directory where it had always rejected one.


## 2026-08-21 — root `CLAUDE.md` back under the large-memory floor (`claude-context-trim`)

Closes B35. Claude Code warns when one loaded memory file exceeds ~5 % of the context window, floor
~40,000 characters; after four branches merged, root `CLAUDE.md` was **56,078**. Now **33,208** — a 41 %
reduction, with ~17 % headroom under the floor.

**The API behaviour-change table moved to `claude-docs/API-BEHAVIOUR-CHANGES.md`** (20,218 chars, the
single largest block at ~5,200 tokens per session). The *policy* stayed in root; only its evidence moved.
Verified as a move rather than a rewrite: all **24** rows are byte-identical and in the original order.
It is consulted when writing release notes or answering "will this break an embedder?" — not context a
session needs up front.

**The refactor-state table's per-test enumerations are gone** (api 1,392 → 307 chars, app 1,456 → 386,
grinder 1,168 → 484). They listed test class names, which `ls src/test` answers, and they contradicted
this file's own rule to cite what a guard asserts rather than how many tests exist. Each row now carries
the durable fact instead — the *guard style* to follow when adding one, which is the part a newcomer
cannot derive.

Nothing was dropped without checking where it already lived. Every backticked symbol in those rows was
tested against the owning module's `CLAUDE.md`: the survivors were test-class names (derivable) or facts
documented better elsewhere — `IncorrectResultSizeDataAccessException` in `ModPackRepository`'s KDoc, and
the CurseForge crawl's two design-killers as LANDMINE #1 and #2 in the grinder's `source/CLAUDE.md`, in
more detail than root's summary. Root now points at those instead of paraphrasing them.

**The build-layout section deliberately stayed**, against the original plan. Moving it to a paths-scoped
`.claude/rules/build-layout.md` would save ~2,900 tokens, but scoped loading was never verified end to
end here, and the landmines it holds are the expensive kind — the Boot BOM `platform()` trap cost 16 app
tests once, and the Kotlin/coroutines skew failed silently. The target was met without that gamble, so it
was not taken. If someone confirms `paths` scoping works, that block is the next ~2,900 tokens.

Also refreshed while in there: the status date, and the frontend suite count (31 → 32) the merge had left
behind.


## 2026-08-21 — the database URI never reached MongoDB (`claude-mongo-boot4-property`)

**Spring Boot 4.0.0 retired `spring.data.mongodb.uri`, the key ServerPackCreator writes.** Metadata
`deprecation.level = "error"`, replacement `spring.mongodb.uri`; the connection properties moved from
`DataMongoProperties` (`spring.data.mongodb`) to `MongoProperties` (`spring.mongodb`). A retired key is not
bound and does not warn, so Boot used its own default `mongodb://localhost/test` — ignoring every
configured host, credential and database, and every `SPC_DATABASE_*` container variable, on **shipped**
versions (`main` on Boot 4.0.3, develop/alpha on 4.1.0).

Found while building a real-MongoDB harness to verify the performance branch's DBRef→embedded migration —
the migration reported `0 inspected` against a correctly-seeded database, which made no sense until the
client's own log line showed `hosts=[localhost:27017]`, `credential=null`.

This is the answer to the question `DOCKER-MONGO-INVESTIGATION.md` left open, and it retires that
document's middle triage row: a `localhost:27017` connection error did not mean the reporter's
`overrides.properties` was missing. Their file was fine; the key name was wrong for everyone.

Two guards, then the fix. The behavioural one registers the URI under `WebserviceConfig.DATABASE_URI_KEY`
via `@DynamicPropertySource` — keyed by the production constant, never a repeated literal — and asserts
host, credentials and database on Boot's resolved `MongoConnectionDetails`, no database required. It uses
`127.0.0.1` deliberately, because `localhost` is Boot's fallback and only a different host proves binding.
The second reads Boot's own `spring-configuration-metadata.json` off the classpath and fails on any key we
write that Boot has retired; that one generalises past this rename.

Reading stays backward-compatible: `databaseUri` falls back to `LEGACY_DATABASE_URI_KEY` and re-writes the
value under the live key, so no existing properties file needs editing. **No `MigrationManager` step** —
migrations run release→release only, so they would miss every dev/alpha/beta user and would need a release
number that does not exist yet; the getter covers every build type on first read. The legacy line is left
in place so a downgrade still finds its URI.

Grouped in, because the fallback requires it: the scheme check was `!startsWith("mongodb")`, which accepts
the degenerate `mongodb:` a partly-configured container produced. It now enumerates `mongodb://` and
`mongodb+srv://`, with the SRV form pinned since a prefix check had been accepting it by luck.

Verified end-to-end, not only by unit test: a document seeded into a non-default database `spc_e2e`, with
SPC configured through the **legacy** key alone, came back from `GET /api/v2/runconfigs/all`. Before the
fix that query hit `localhost/test` and returned `[]`. `docker/tests/init-spc-config-test.sh` re-run in the
production base image, 10/10. `./gradlew build` green — api 312 (1 skip), app 110, clientside 88, grinder
233 (19 skip), plugin-example 3.

**Note for whoever merges `claude-performance-improvements`:** its `BACKLOG.md` carries B33 and B34, which
described this defect's symptoms from the outside. Both are resolved here and should be dropped in the
merge rather than carried forward as open items.


## 2026-07-31 — audit/backlog cleanup, Phases 1–2 (`claude-audit-backlog-cleanup`)

**Phase 1 — audit findings.** H-B closed by tabling the `variables.txt` contract change (generation reads an
operator-editable file; two new exported members) in the root `CLAUDE.md` compatibility table — the policy exists
for exactly that case and had not been applied to it. M-B and M-C closed as binding rules: pin-first now names the
*commit* boundary (red test commit, then the fix), and `refactor:` is reserved for behaviour-preserving change, a
changed *existing* test being the stop-and-flag signal. Both cite the commits that got it wrong.

**L-C withdrawn.** The finding claimed gratuitous exported mutability at `PathsConfig.kt:694`. Reading the whole
declaration before changing it showed otherwise: the setter is `private`, and the `var` is load-bearing because the
getter assigns the backing field so the path re-derives per access and follows a changed home — the pattern 31
properties in that file use. The two plain `val`s the audit measured against are the exception *and* carry the real
defect (captured once at construction, they do not follow a home change), recorded as **B21**.

**Phase 2 — B19, a shipped defect.** A fresh Forge pack on Minecraft 26.x installed and exited **0** without ever
launching. The suspected cause (Forge passes an installer URL where NeoForge passes a bare version) was wrong.
ServerStarterJar runs the Forge installer in its own JVM and depends on a `SecurityManager` to swallow the
installer's `System.exit(0)`; JEP 486 removed that from Java 24 and SSJ catches the failure silently, so the exit
takes the process with it. This was the second half of `c571e2d7f`: dropping the fatal flag stopped the VM refusing
to start and revealed the flag was load-bearing for SSJ's *install* step.

Landed as the two commits the new rule requires — `203a32534` adds the guard **red** (executing `setupForge` across
Java 17/21/24/25; observed failing with *"on Java 24 … expected: <false> but was: <true>"*), `f6c23e972` turns it
green across sh/fish/ps1. From Java 24 on the templates install Forge themselves and launch from the installer's
argfile (`unix_args.txt`; `win_args.txt` for PowerShell); below 24 nothing changed.

Verified end-to-end by `ScriptTemplateMatrixIT`: **8/8 cells green**, Forge 26.2 passing in bash *and* fish on a
fresh pack at first invocation (world directory created), with Forge 1.20.1 and NeoForge 26.2 as regression
controls, plus both PowerShell tests. Cached loader tuples were checked rather than blanket-invalidated: every 26.x
Forge tuple already carries the `unix_args.txt` the new path launches and `downloadIfNotExist` short-circuits on it,
so offline boots keep working and hours of re-installs were avoided. `verdicts.json` archived (Forge on 26.x now
reaches a real signal); sweep rebuilt and restarted under `caffeinate`.

**Environment, not code.** Two of the four matrix runs failed wholesale on container DNS: the host resolves through
`nameserver 127.0.0.1`, a loopback resolver that Docker's VM forwarder cannot reach — a standing incompatibility on
this machine, not a transient wedge (a hard Docker restart cleared it once and then stopped working). The fix is to
pin explicit resolvers for the daemon in `~/.docker/daemon.json` (a timestamped backup sits beside it).

**Which resolvers is a trust decision, not a technical one — do not re-suggest the obvious public ones.** Griefed
rejected Cloudflare and Google outright (a resolver sees every hostname you look up, so its operator's business model
is the whole question) and set OpenDNS + Quad9 instead. Any future advice here should name the *requirement* — a
reachable, non-loopback resolver the operator trusts — and let them pick.

Worth knowing regardless: broken container DNS silently blocks the sweep's **pre-bake**, which is the one networked
boot, so new loader tuples stop installing while cached ones keep booting fine. It looks like a quiet sweep, not an
error.

## 2026-07-31 — audit/backlog cleanup, Phases 3–6 (`claude-audit-backlog-cleanup`)

**Phase 3 — B20, the silent skip that hid B19.** `MinecraftServer.javaVersion()` turned every exception, a failed
manifest download included, into the same `Optional.empty()` that means "declares no required Java", and
`ImageJavaRuntimes.supports()` collapsed that into the same `false` as "the image lacks this JDK". The template
matrix rendered the result as `[N/A] SKIPPED` — so all four Minecraft 26.2 cells, Fabric among them, vanished from a
green run. `supportFor()` now returns `SUPPORTED` / `JDK_NOT_BUNDLED` / `REQUIREMENT_UNKNOWN` with `supports()` as a
facade keeping its contract; the IT skips the first two with accurate reasons and **fails** the third, saying what is
missing and why. The api-side swallow keeps its exported `Optional` but logs the cause. `26.2` also joins the IT's
default Minecraft axis, which had contained only `1.x` versions while three separate bugs lived in `YY.x` handling.

**Phase 4 — B14, B15, B18.**
- **B14** — `HostProcessServerRunner` had the wall-clock deadline B9 fixed only in the container engine, so a host
  suspend still wrote off a boot on the `-verifyclientside` path. Rather than copy it, `SuspendAwareDeadline` was
  extracted into **`-clientside`**: grinder depends on clientside, so that is the direction that keeps dependencies
  pointing inward. It takes an injected clock, which is the only way the threshold is testable — both real callers
  are integration-shaped and cannot be made to sleep. Three commits: red pin, behaviour-preserving extraction (the
  grinder's existing `SuspendGapTest` repointed, assertions untouched), then the host-runner adoption as its own
  behaviour change.
- **B15** — the loader-cache marker now records a digest of the start-script templates that produced an install, and
  a *differing* provenance is a miss. An **absent** one is tolerated with a single warning per run: treating unknown
  as different would reinstall all 74 cached tuples at ~150 MB and a networked boot each, and Phase 2 is the proof
  that would have been waste — its cached Forge tuples were checked and every one was still bootable.
- **B18** — one generation of install console now survives the wipe that starts a retry.

**Phase 5 — build hygiene.**
- **B16** — the committed test properties no longer carry one machine's absolute paths; `processTestResources` fills
  the JDK path from the configured Java 21 toolchain and the tomcat basedir from `<module>/tests`, both declared as
  task inputs and escaped for properties syntax.
- **B17** — `.gitignore`'s bare `server_files` rule hid the shipped resources (it swallowed a `git add` twice this
  session). Re-included with the idiom the file already uses for `configs`, deliberately *not* by anchoring to the
  repository root, since each module's test home is `<module>/tests` and `<module>/tests/server_files` must stay
  ignored. **Measurement note:** `git check-ignore` suppresses any path containing tracked files and reported the
  still-ignored directory as clean — `--no-index` is what tells the truth.

**Suites at the end of the branch:** api 272 (1 skipped) · clientside 87 · grinder 224 (19 skipped) · app 76 — all
green. Every guard in these phases was verified by breaking it and watching it fail.

**Audit outcome.** H-B, M-B and M-C closed; **L-C withdrawn** as wrong on inspection, with the inverse defect it
pointed at recorded as B21. Backlog now holds only B4, B5, B11 (deliberate deferrals) plus B21 and B22.

## 2026-08-02 — Qodana report audit and its fallout (`claude-qodana-audit-fixes`)

Audit of the Qodana report for job 37392 (revision `89305a8e8`, i.e. `develop`'s head at the time): **54 problems,
46 High / 8 Moderate**. Verified one by one against the code rather than taken at face value. Outcome: **15 real,
22 false positives, 14 by-design, 3 cosmetic** — and the false-positive rate turned out to be the most important
finding in the report.

**The 18-finding phantom cluster, and the version skew behind it.** Every `KotlinUnreachableCode` hit — all 18, in
`CurseForgeCandidateSource.kt`, i.e. **39 % of the report's High severity** — is not real. Established three
independent ways before touching anything: (1) `kotlinc` 2.3.20 compiles the module with zero warnings and never
emits `UNREACHABLE_CODE`; (2) `CurseForgeCandidateSourceTest` passes 15 tests that *require* the supposedly dead
lines to execute — line 199 sets the version axis, 251 the categories, 307 returns every mapped page; (3) the root
cause, measured straight out of the images:

    docker run --rm --entrypoint sh jetbrains/qodana-jvm-community:<tag> \
      -c 'cat /opt/idea/plugins/Kotlin/kotlinc/build.txt'
      2025.1 -> 2.1.10-release-473
      2026.2 -> 2.3.20-release-208

The project builds with Kotlin 2.3.20, so the pinned linter analysed 2.3 source with a **2.1 frontend**, two minor
versions behind. Every phantom hit sits after a `runCatching {}.getOrElse { …; return }`, whose type parameter the
older frontend infers as `Nothing`, making everything below look dead. Bumped to 2026.2 (current stable, `latest`,
published 2026-07-27) in `qodana.yaml` and `.gitlab-ci.yml`, cache keys moved with it. **Not verified by a local
full run:** Qodana OOM-killed at exit 137 during Gradle import, because the dev machine's Docker VM is capped at
**1.93 GiB** against 48 GiB of host RAM — the same cap already recorded as a grinder landmine. The image and its
compiler version are measured; the resulting problem count is not, and wants confirming against the next CI report.

**`WritableDirectoryFilter` was inert — and the report undersold it as an unused function.** FlatLaf's
`SystemFileChooser.FileFilter` declares **only** `getDescription()` (checked with `javap` against flatlaf 3.7.1), so
`accept(File)` overrode nothing — which is exactly why it compiled without an `override` modifier. FlatLaf drives a
*native* OS dialog that cannot call back into Java per file; its only real filters (`FileNameExtensionFilter`,
`PatternFilter`) are `final` and declarative. **No behaviour was lost**, and the first reading of this finding —
"the writability restriction silently vanished" — was wrong: all four call sites already validate after the dialog
returns, via `File.testFileWrite()` plus a `settings_directory_error` dialog (`GlobalSettings.kt:63,96`,
`WebserviceSettings.kt:70,89`). So the fix is deletion, not repair. Dropping the assignment is safe because
`getFiltersForDialog` null-checks the field on every read (bytecode offsets 41/56/77) and the choosers are
`DIRECTORIES_ONLY` anyway, leaving a *file* filter nothing to act on. The class's sole translation key went with it.

**Three functions had silently lost their documentation.** A doc comment immediately followed by another attaches to
nothing: the first documents no declaration and the function it was written for ends up bare. Seven such blocks
existed. `BootVerifier` had the docs for `outcomeFor` *and* `shouldRecheckCrash` both drifted above
`refuseForMissingDependencies`, stacked three deep — the verdict seam every runner shares, and the guard that stops
a stale loader build being published as a HIGH-confidence clientside mod, each undocumented at its definition.
`GrinderApplication`'s `env` doc sat above `resolveSpcPropertiesFile`. Four more in `Tetris.kt`, stranded by the
Java→Kotlin conversion where getter pairs became properties and two constructor overloads became one primary
constructor with defaults. **Dokka cannot catch this class of defect** — an unattached block yields no declaration
to warn about — which is why it took Qodana's `KDocUnresolvedReference` on the dangling `[logFile]`, `[label]`,
`[key]` and `[default]` references to surface them at all.

**Five KDoc links that resolved to nothing**, each for a different reason: `[renderMarkdown]` lives on
`ClientsideReportRenderer`, not on the report; `ContainerEngine`'s interface doc linked `[readyPattern]`/`[timeout]`,
which are `run`'s parameters; `LoaderVersionPolicy` carried an `@param versionMeta` although an interface has no
parameters (it documents `LoaderVersionResolver`'s constructor); and `[GlobalScope]` ×2 survived the GlobalScope
removal that took the import with it. Measured by Dokka: `Couldn't resolve link` across api/clientside/grinder/app
goes **12 → 0**. Dokka only inspects Public/Protected/Package, so the `internal`/`private` sites were confirmed with
`documentedVisibilities` temporarily widened, then reverted.

**Dead code**, all four inert: an unused `ScanResult()` local in `FabricScanner.scan`; `fileNamesForLoader` with no
caller anywhere (and `-clientside` is unpublished, so no compatibility claim protects it); an unused logger in
`ContainerCandidateVerifier`; and `InclusionsEditor.removeSelectedEntry`'s `selected++` … `--selected`, which cancel
exactly — the post-increment compares the old value and the pre-decrement restores it before use.

**`modFileEndings` and `zipCheck` — rewired, not deprecated.** Both were reported unused, and the first instinct was
to deprecate them. Tracing them first showed that would have been wrong. Each had exactly one call site before its
extraction, and each extraction (`a35f3cda6` Phase 1c, `b0dc98131` Phase 1d) moved that call site into the new class
along with a *private copy* of the constant; the moved code is byte-identical and both public facades still
delegate. **So it was never a bug that they fell out of use, and no third site should be reusing them** — checked:
`"disabled"` appears in only one other production place (`ServerPackFileGatherer:199`, renaming a disabled mod's
destination, a different concern), and the only other `filteredWalk` call site filters the *plugins* directory.

What was wrong is subtler and worth stating: two unlinked copies of each literal, with **the reasoning stranded on
the copy that does nothing**. `ModListCompiler:57` and `ModpackZipInspector:48` — the values generation actually
consults — carried no documentation, while the dead `ServerPackHandler:90` and `ConfigurationHandler:83` (pre-change line numbers) explained
why `disabled` counts and what the pattern detects. An edit aimed at the documented copy would have changed nothing
at all. The project's own `SupportedModloaders` rule already names this failure mode, so the fix is a single source
of truth, not a deprecation: the facades became getters reading their owner, and the docs moved to the live copies.

- **Getters, not initialisers, on purpose.** `modFileEndings` is declared at `ServerPackHandler:92` but
  `modListCompiler` only at `:98`; `zipCheck` at `ConfigurationHandler:88`, `zipInspector` at `:109`. Kotlin
  initialises properties in declaration order, so `val x = collaborator.y` would read the collaborator before it
  exists — the ordering landmine already recorded for `ApiProperties`' setting groups.
- **The pin asserts identity, not value**, because a value comparison passes against a re-introduced duplicate that
  happens to agree — precisely the state being guarded. Committed red (`c59b11318`), failing on assertion rather
  than compilation, with the failure output stating the hazard exactly: `expected: …ArrayList@61f97194<[jar,
  disabled]> but was: …ArrayList@6afc2700<[jar, disabled]>`. Two objects, identical contents, nothing linking them.
  Promoting the two owned constants from `private` to public is folded into that commit as the enabling change —
  without it the guard cannot be *expressed*, and a non-compiling commit would be a broken build, not a red test.

**Suites at the end of the branch:** api 280 (1 skipped) · clientside 87 · app 76 · grinder 233 (19 skipped) ·
plugin-example 3 — all green, no existing assertion changed anywhere.

**Left deliberately unfixed.** The five api `UnusedSymbol` hits on published facades stay: they are compatibility
surface, and Qodana's scope excludes test sources — `compileModList(packConfig)` is called by
`ServerPackHandlerCharacterizationTest:177` and was flagged regardless, so every `UnusedSymbol` hit reads as "unused
in main", not "unused". Also rejected: `CanBeParameter` on `Dependency.modID` (dropping `val` removes a published
property), the five `CanUnescapeDollarLiteral` and three `RemoveRedundantQualifierName` (explicit is more readable;
the latter is only "redundant" under Kotlin 2.2+ context-sensitive resolution).

---

## 2026-08-04 — CI: the Qodana 2026.2 bump could not run, and the dind service never could

Two branches: `claude-ci-qodana-jbr-cache` (merged into `develop` as `f16dff7ff`, and present in `alpha`) and
`claude-ci-audit-fixes` (the audit remediation). Only `.gitlab-ci.yml` and docs; no Kotlin, so no suite moved.

**The failure.** The `2026.2` bump (`b14d30d45`) left the job failing one second into `step_script` on
`fork/exec …/qodana/cache/qodana-jbr/qodana-jbrsdk-25.0.2-linux-x64-b329.72/…/bin/java: permission denied`.

**Root cause, measured against the pinned image rather than guessed.** 2026.2 builds its "effective configuration"
by running `libs/config-loader-cli-0.0.38.jar` in a **separate JVM**, and downloads its own runtime for it into
`<cache-dir>/qodana-jbr`. That path is derived from `--cache-dir`, and there is no way to redirect it or to reuse
the JBR the image already ships at `/opt/idea/jbr`: `docker image inspect` gives `JAVA_HOME=/opt/idea/jbr` and
`USER=0`, `qodana scan --help` offers only `--cache-dir` / `--clear-cache`, and `grep -a -oE "QODANA_[A-Z0-9_]+"`
over the Go binary (the image has no `strings`) shows no JBR or JAVA override. So an executable necessarily lives
inside the directory the job caches, and **GitLab's cache round-trip does not preserve the executable bit**
(gitlab-runner#27496/#1782, both open). The container runs as root, which bypasses ownership but still needs one
`x` bit to `execve`. 2025.1 never hit it: no `config-loader-cli`, nothing executable in the cache.

`noexec` on `/builds` was ruled out without access to the runner: `cache:when` defaults to `on_success`, so the
restored key can only have been written by an earlier **successful** 2026.2 run that exec'd that same path.

**Reproduced locally before fixing**, against a minimal fixture rather than this repo — the failure happens before
any analysis, so the 1.93 GiB Docker VM never approached the OOM that killed the earlier local attempt. Empty cache
→ `bin/java` is `-rwxr-xr-x`, config loads; x bits stripped from files only → the byte-identical CI error, exit 1;
`chmod -R +x` → `openjdk version "25.0.2"` and `Loaded Qodana Configuration`.

- **6 of 133** files in the JBR carry an x bit — `bin/{java,keytool,jrunscript,rmiregistry}` and
  `lib/{jexec,jspawnhelper}`. Two are outside `bin/`, which is why the repair is `-R` and not a `bin/` predicate.
- **No other file anywhere else in the cache** is executable — `config-loader-cli-0.0.38.jar` is cached too but is
  run *by* java. Hence the chmod is scoped to `qodana-jbr/` instead of the whole cache, so the next instance
  surfaces rather than being masked.

**Exec is the only reliable test of that bit.** Measured while fixing audit finding L-1: on a Docker Desktop bind
mount a host-side `0644` file is **reported** as `-rwxr-xr-x` inside the container and `[ -x ]` answers
"executable", while `execve` still fails with EACCES. An intermediate version of the guard asserted `[ -x ]` and was
therefore unsound; the probe now runs the binary and **classifies the failure** — "permission denied" fails the job,
anything else warns, because a stale or truncated JBR tree that Qodana may not even use must not take the pipeline
down (measured: a tree holding only `bin/java` exits 127 on `libjli.so: cannot open shared object file`). The same
attribute-caching quirk explains a discrepancy in the first session's evidence: previously mounted paths report a
stale 0755, freshly created ones report the truth. The exec result was faithful throughout.

**Second finding, from the same log: the dind service has never worked.** `dockerd` dies at startup on
`can't create unix socket /var/run/docker.sock: device or resource busy` — something is already mounted at that path
in the service container, which is runner config, outside this repo. It was declared top-level, so **20 of 20 jobs**
started it and each paid the health-check wait (34 s, measured 20:19:41 → 20:20:15) for nothing. Narrowed to the
**7** jobs that genuinely need a daemon, verified by parsing every job body against the `extends:` list:
`Update README:on-schedule` needs one (it runs `act` against `catthehacker/ubuntu:act-*`) despite containing no
literal `docker` token, `Generate Release` does not (every `@semantic-release/exec` block is commented out), and no
Gradle job does (the Docker-dependent grinder tests are gated behind `GRINDER_DOCKER_IT` / `GRINDER_TEMPLATE_IT`).
Deliberately conservative rather than deleting it outright — B26/B27/B28 carry the rest.

**Griefed confirmed a full green pipeline on 2026-08-04**, which validates both changes in CI.

**The audit's own finding worth keeping.** `358675fbf` was labelled `refactor(ci)` while stopping 13 jobs from
starting a container — a behaviour change, and the third commit in this project to get that label wrong. It was
audited **after** being merged into `develop` and `alpha`, so the honest remedy was a record in `CLAUDE.md` rather
than force-pushing two shared branches. That asymmetry is now written into the convention itself: cheap before the
merge, unfixable after it.

## 2026-08-14 — modscanning hardening (`claude-modscan-test-hardening`, `claude-modscan-tidyup`), api 280 → 295

The modscan rewrite (`0a12d41d0`) and its six follow-ups changed behaviour four times without adding a single test case; two
regressions shipped and **neither turned the suite red**. Auto-exclusion was dead for a day — the user-exclusion
pass re-walked every scanned mod and re-enabled anything it had not itself matched — and its only coverage
guarded on `Assumptions.assumeTrue`, so an empty result **skipped** instead of failing (the B13–B20 lesson,
regressed). The Quilt merge keyed on `modID`, which is mismatched by construction in exactly the entries it
merges: `quilt_tests/aaaaa.jar` declares `ok_zoomer` in one descriptor and `ok_zoomer-pmw` in the other, and
`bbbbb.jar` has no `fabric.mod.json` at all, so the failed scan falls back to the filename — 5 jars in, 7 entries
out, each duplication announced in an INFO log as though it were a repair. Both are fixed and now pinned, each
**observed red against the commit before its fix** (`7004f3c88^`, `bf226c2ac^`, `2ec5ff202^`), with the
observed failure text in the commit body.

Three things worth keeping. **The fixtures cannot express an absent field** — every committed `fabric.mod.json`
and `quilt.mod.json` declares an `environment`, so the SERVER-when-undeclared default (a decision: an
undeclared mod must never be dropped from a pack) had *no* coverage, which is how `bf226c2ac`'s defect got in.
New cases build a real jar in a `@TempDir` from JSON written inline, so the descriptor under test is visible in
the diff and no binary enters the repo. **The committed jars stay as they are** — they are real-world captures
and that messiness is the point (`fabric_tests/fffff.jar` carries a literal newline inside a JSON string, invalid
strict JSON that the scanner handles anyway), they are consumed by eight test classes across `-api` *and* `-app`,
and `forge_old/aaaaa.jar` holds a genuine 1 MB `fml_cache_annotation.json`; 776 KB total is not worth
regenerating. **A green characterization test proves nothing until you try to break it** — the new
exact-match-not-prefix assertion on `dependencyExclusions` was confirmed by mutating the regex to `fabric.*` and
watching it fail, because `fabric-api-base` must survive a filter that drops `fabric`.

Also fixed here (audit M-4): the scanner-selection `when` had **no `else`**, and since the rewrite the
include-list is built solely from what a scanner returned — so an unrecognised loader produced a **silently empty
server pack** where the pre-rewrite code returned every jar. Reachable from an ordinary `PackConfig`, because
`PackConfig.modloader`'s setter silently ignores a value it does not recognise and leaves the field at `""`.
The `else` now warns and includes everything.

**Tidy-up (`claude-modscan-tidyup`).** `ScannedMod`/`ModDependency` are immutable and built through their
constructors, which removed **all 8** `!!` assertions and **all 4** mutable `currentModID` fields — those were
shared state on scanners `ApiWrapper` holds as **singletons**, so two concurrent `scan()` calls interleaved
through them. `ForgeAnnotationScanner` needed the id returned rather than passed in, because there it is a
*result*: whichever annotation first carries one, with every later annotation read relative to it. The
four-branch exclusion-filter `when` — written out **three** times, already drifted in formatting — is read once;
the whitelist `while(any{}) { removeIf{} }` collapses to one `removeIf` (the predicate reads only the mod and
the whitelist, so nothing a removal does can make a remaining entry start matching), while the dependency
rescue keeps its loop because each rescue adds to `serverMods` and puts further dependencies in play. The Quilt
copy-loop was **unreachable** and is gone: both scanners return one entry per input file, so its lookup never
missed — it fired only under the old `modID` key, where it was the duplicate-producing mechanism rather than a
fallback. **Do not "fix" `ScannedMod` with a `data class`:** two entries for one jar can disagree on
`sideness`, and value-equality would let a `Set`/`distinct()` keep whichever landed first and drop the other
verdict, which is the merge the Quilt arm makes deliberately. Compare on `file`.

**A second fabricated-reference problem, worth the same suspicion as the `PackConfig` one below.**
`ReadmeExamplesTest` cited **seven** README sections that do not exist (`§2 Quickstart`, `§3 Composition root`,
`§4 PackConfig`, `§5 Validating`, `§7 Version metadata`, `§8 Scanning mods`, `§9 Settings`) and claimed the
guide "teaches roughly a dozen snippets". `README.md` carries exactly **two** Kotlin API snippets, both under
*6. API → Example*. That file's entire justification is being the compiler-gate for the README, so a reader
trusting its citations would hunt for prose that was never written. Cases that genuinely mirror a snippet now
cite it; the rest say plainly that they guard adjacent surface.

And (audit I-6) the dependency rescue additionally required the **disabled** mod to be `Sideness.SERVER` — but a
mod auto-disabled by a scanner is `CLIENT` by construction, so *"don't exclude something's dependency"* only ever
reached mods the scanner had called server-side and the user had excluded by name, never the auto-detected ones it
exists for. Clause dropped; pinned in both the direct and the **transitive** case (`servermod → midlib → deeplib`,
middle and leaf both clientside), because a single-pass rescue keeps the leaf excluded and still looks like it
worked — which is what the surrounding `while` is for.

---

## Modscanning generification (`claude-modscanning-generification`, 2026-08-15)

Triggered by a Qodana report review (job 37558, rev "RELEASE: 9.0.0-alpha.5": 58 problems, 13 High, no security or
correctness inspections — 7 self-inflicted `KotlinDeprecation` on the 6.0.0 `scriptTemplates` facades, 4
`KDocUnresolvedReference` in `ClientsideModels.kt`, one `RedundantInnerClassModifier`, and one
`UnusedSymbol` that turned out to be in `modscanning`) plus the question of what in the scanners could be
generified.

**The bug the reading found, fixed before any restructuring.** `ModListCompiler` picked Forge's scanner with
`mcVersions[1].toInt() > 12`, and `-clientside`'s `MetadataScanner` with the same test. Minecraft has two
versioning schemes, so `26.2`'s minor of `2` read as the 1.2 era and sent every modern Forge pack to
`ForgeAnnotationScanner` — the 1.12-and-older one. No modern jar carries `fml_cache_annotation.json`, so every
jar threw, every jar fell back to the never-drop-a-jar `SERVER` default, and **auto-exclusion silently did
nothing on Forge 26.x** while logging one ERROR per mod. It fails safe (everything included), which is why it
had gone unnoticed; `autoDiscoveryReachesScannerBranchPerLoader` only ever covered 1.12.2 and 1.16.5.

This is the versioning-scheme landmine's **third** instance and the first outside the shell templates — and
`serverpackcreator-api/CLAUDE.md` had asserted *"the Kotlin side was surveyed and is clean by construction"*.
It was not: the survey covered the boot/selection code the grinder work had just touched, not the generation
path. That claim is now corrected in place rather than deleted, because the wrong-but-confident version is the
part worth remembering.

Pinned red first in its own commit (`f8cb89bff`), both tests looping 1.20.1 and 26.2 against a real jar with a
modern `META-INF/mods.toml` and asserting the *outcome* (excluded / kept, CLIENT / SERVER_OR_BOTH) rather than
which scanner was chosen. Observed failing for the right reason — the 1.20.1 iteration passed in both, so the
fixtures were valid and only the era selection was wrong. Fixed in `4dbf653cc` by comparing every component
through `SemanticVersionComparator` against 1.13, the version Forge actually switched at, which is the call the
NeoForge branch three lines below had been making correctly all along.

**Then the generification.** Four extractions, behaviour-preserving, no existing assertion touched:

1. **`ModJarScanner`** (public) replaces the `internal Scanner<T, U>`, whose two type parameters had exactly
   one instantiation across all five implementations. The real problem was `internal`: `-clientside` and
   `-grinder` could not see it, which is *why* `MetadataScanner` hand-wrote dispatch over concrete types. A
   plugin can now implement a scanner for the first time.
2. **`DescriptorScanner`** owns the walk-the-jars loop and the one-`ScannedMod`-per-input-jar guarantee all
   five repeated. `scan` is `final`; subclasses implement `read(File)` and may throw. That contract is the one
   thing no scanner may get wrong — a dropped entry is a mod missing from the finished pack.
3. **`FabricFamilyScanner`** absorbs what Fabric and Quilt genuinely share (id + environment reading, differing
   only in field *paths*, including the subtle "no environment entry means SERVER" default). Dependencies stay
   abstract: Fabric declares an object keyed by mod id, Quilt an array of either objects or bare strings, so
   the block's *shape* differs, not its path.
4. **`ModScanner.scannerFor(modloader, minecraftVersion)`** is now the single dispatch for both callers, and
   **`QuiltPackScanner`** holds the two-descriptor merge that only `ModListCompiler` implemented. The
   clientside `CLAUDE.md`'s *"kept in sync deliberately; it is not shared code"* is retired — that instruction
   is exactly what let one bug live in two files.

**Quilt merge equivalence**, since the two callers differed: `ModListCompiler` kept the Quilt entry unless
Quilt said SERVER and Fabric said CLIENT; `MetadataScanner` unioned the two clientside sets. Both yield CLIENT
iff either scanner did, so the composite reproduces `ModListCompiler`'s rule exactly and `MetadataScanner`'s
answer is unchanged. What the union had lost — *which* `ScannedMod`, and so which id and dependency list,
survives — is preserved, and it matters for the downstream dependency-rescue.

**`JsonBasedScanner` was removed, not deprecated — Griefed's call, overriding the adopted policy.** The first
cut kept it as a standalone `@Deprecated(ReplaceWith("JsonDescriptorScanner"))` facade, because it is published
and gaining the abstract `read` would break any plugin subclass. Griefed overrode that the same day: scanners
are **not** a pf4j extension point, so a plugin could subclass the helper but never register the result — the
facade was compatibility cost with no reachable benefit. It is deleted, `getJarJson` lives on
`JsonDescriptorScanner`, and the break is recorded in the root `CLAUDE.md` compatibility table rather than
papered over. Worth remembering as the shape of a legitimate override: the policy protects *reachable* plugin
surface, and this was not.

Swept up along the way: the Qodana `UnusedSymbol` (`JsonBasedScanner`'s never-read `log`), the same in
`ForgeTomlScanner` once its catch moved to the base, and a dead `NullPointerException` catch in `FabricScanner`
around a `ModDependency` construction that cannot throw. Two logging changes are stated rather than hidden —
`ForgeAnnotationScanner`'s per-jar failure loses its stack trace in favour of the message form the other four
used, and `ModListCompiler`'s two NeoForge "Scanning using X scanner." debug lines go with the branch that
emitted them.

Code lines with comments and blanks stripped: `ModListCompiler` 169 → 131, `MetadataScanner` 43 → 21, the
`modscanning` package 523 → 527 — i.e. the duplication became a shared, documented abstraction at roughly zero
net cost, and the dispatch now exists once. `ModScannerDispatchTest` (6 tests, asserted on **identity**) pins
the selection itself, including that an unparseable version falls back to the modern scanner instead of
throwing — `"26"` used to raise `IndexOutOfBoundsException` out of the bare-component parsing.

Suites: api 302 (1 skip), clientside 88, app 80, grinder 233 — all green.

### Qodana moderates (same branch, 2026-08-15)

45 Moderate findings from the same report, cleared in three commits. Of them **41 applied, 4
deliberately not** — an inspection is a suggestion, not a verdict:

- **UsePropertyAccessSyntax** (LarsonScanner) does not compile. `Graphics2D.getRenderingHints()`
  returns `RenderingHints` while `setRenderingHints` takes a `Map`, so Kotlin exposes the property
  read-only; `g2d.renderingHints = …` fails with *"'val' cannot be reassigned"*. Tried, reverted,
  and the call now carries a comment so nobody repeats it.
- **DestructuringDeclaration** ×3 (`ClientsideReportRenderer` ×2, `Grinder`) are all
  `for (verdict in report.perLoader)` over `LoaderVerdict`, a data class with eight-plus fields.
  Positional destructuring costs every speaking name the loop bodies use, and `componentN` is
  positional — a reordered property would silently *rebind* every variable rather than fail to
  compile. Exactly the silent-failure class this codebase guards against.

Two **RedundantIf** findings were applied as `when`, not as the `||` Qodana implies:
`BooleanUtilities.convert`'s recognised-false branch and its fallback both return false, but only
the fallback warns — a plain `||` would have fired *"couldn't parse boolean"* on every valid
`"false"`/`"0"`/`"no"`. And `JsonUtilities.getNestedBoolean` keeps its three-way shape with the
throw; `toBooleanStrictOrNull()` is **not** a drop-in there, it is case-sensitive and that method
accepts `"True"`/`"FALSE"`. Both now say so in a comment.

**Two untested things had to be pinned before they could be touched**, both in the version-parsing
silent-failure category:

1. `VersionChecker` had **zero** tests and Qodana wanted three of its boolean chains collapsed. A
   canned subclass over `allVersions()` pins the whole alpha/beta path offline. Writing it surfaced
   a genuine defect — channel-blind pre-release comparison — pinned as-is at first, then **fixed on
   request later the same day** (see below).
2. `MigrationManager`'s lambda-suffix regex was written out **twice** in two escaping-heavy copies
   with no coverage. Hoisted to one documented `LAMBDA_SUFFIX` constant, converted, and pinned — and
   the pin's **teeth were checked** (broken to `"[0-9]*lambda[0-9]*"` it fails with
   `expected: <SixDotZeroDotZero> but was: <SixDotZeroDotZero$$1>`, green again on restore).

The three Spring `@Scheduled(cron = …)` placeholders have no test that loads the scheduling context,
so they were verified by **measurement** instead: `javap` on the compiled classes shows the
constant-pool entry unchanged — `#106 = Utf8 ${de.griefed.serverpackcreator.spring.schedules.database.cleanup}`.

Swept up in the four loops the `indices` fix already touched: each called its repository's finder
**twice** per element (once for `isPresent`, once for `get()`). Now one lookup reused through
`orElseGet`, halving the queries on every run-configuration save and every event carrying errors.

Suites after: api 302 (1 skip), clientside 88, app 88, grinder 233 (19 skip), plugin-example 3 —
**714 total**, full `./gradlew build` green.

### VersionChecker pre-release ordering (same branch, 2026-08-15)

The quirk the characterization tests had recorded, fixed on request — plus a second defect the first
one was hiding. Both pinned **red in their own commit** before the fix.

**1. Channel-blind comparison.** `isPreReleaseNewer` compared only the number after the dot, so a
beta did not supersede an alpha of the same version: `alpha.5` vs `beta.3` reduced to `3 > 5`. What
an alpha user was offered therefore depended on a numeric accident — `alpha.2` got `beta.3`,
`alpha.5` got nothing at all with both published. Now channel first (`alpha < beta < release`) with
the number as the tie-break.

**2. Latest-of-channel ignored the version.** `latestBeta`/`latestAlpha` kept a candidate only if it
was *both* semantically newer-or-equal **and** higher-numbered, so a version restarting its count —
`3.2.0-beta.1` after `3.1.0-beta.3` — lost to the older one. Both scans now use `isVersionNewer`:
semantic version first, pre-release ordering only within one version.

**The second pin took three attempts to make honest, and that is the lesson worth keeping.** The
first fixture passed against the broken code because it was newest-first, so `latestBeta`'s wrong
answer never mattered. The second passed too: `isUpdateAvailable` **falls through to
`latestVersion()`**, which masks a wrong `latestBeta` whenever the newest release is a newer *base*
version. It only reaches a user when the beta branch itself fires and returns `latestBeta()`
directly — needing an oldest-first list *and* a current version old enough to trigger that branch
(`3.1.0-beta.1`). A pin that had been committed at either earlier stage would have looked like a
guard while asserting nothing about the defect.

Two consequences worth remembering:

- The test fake's `latestVersion()` now **computes** the newest instead of taking the list head. The
  real `allVersions()` comes from a repository API whose ordering nothing guarantees, and a fixture
  that is silently newest-first cannot catch code that depends on that ordering.
- `isNewAlphaAvailable`'s explicit *"a beta is never offered an alpha of the same version"* guard is
  **gone**, subsumed by the channel ordering — verified by removing it and watching
  `aBetaIsNotOfferedAnAlphaOfTheSameVersion` stay green, which makes the removal provable rather than
  argued. Weaken the ordering and that rule vanishes with it; that one test is what will say so.

`preReleaseNumber` also stopped throwing on a version with no pre-release suffix. The old
split-and-index raised `IndexOutOfBoundsException`, which `checkForUpdate` does **not** catch — it
catches `NumberFormatException` only.

app 89, full build green.

### Scan-failure log levels (`claude-scan-log-levels`, 2026-08-15)

Follow-up to `6026f3640`, which restored the exception + stack trace on the shared `DescriptorScanner`
catch. That was right for real failures and wrong for the majority of what reached it: **159** ERROR
lines with stack traces in a single api suite run, most describing nothing an operator or mod-author
could act on. Two causes, both fixed at the source rather than muted.

**1. "No dependencies specified." was never an error.** `ForgeTomlScanner.getMapOfDependencyLists`
raised `ScanningException` when a `mods.toml` carried no `[[dependencies]]` block — an ordinary
descriptor. The raise aborted `read()` mid-way, so the mod fell back to the unreadable-jar defaults and
its **already-parsed modId was replaced by the filename** (`expected: <lonelymod> but was:
<lonelymod-1.0.0>`, pinned red first). Traced before claiming a defect: the *verdict* is unaffected, since
a mod declaring no dependencies has no clientside signal and comes out SERVER down either path, and the
lost id is joined on only by the dependency rescue — which looks up *disabled* mods, and a mod on this
path is never disabled. So: real data loss, no reachable consequence. Now returns an empty map;
`ScanningException` had no other thrower and is deleted.

**2. A jar with no descriptor is not this scanner's business.** Every scanner is handed the whole
mods-directory, and a Quilt pack is deliberately scanned by *both* the Quilt and Fabric scanner, so one
of the two finds nothing in every single-format jar. The absence used to surface as an NPE from
`JarFile.getInputStream(null)` — indistinguishable from a genuine failure. It is now explicit
(`MissingDescriptorException`, extending `IOException` so the readers' `@Throws` contract is unchanged)
and logged at DEBUG.

Measured over a full `:serverpackcreator-api:test` run:

| | lines | breakdown |
|---|---|---|
| before | 159 | 80 `NullPointerException`, 37 `ZipException`, 28 `ScanningException` |
| after | **51** | 37 `ZipException` (corrupt archive), 14 `ParsingException` (malformed TOML) |

Both survivors are real defects in a jar and stay loud, with the stack trace. `DescriptorScanner.read`
was promoted protected → public in the process: *which* exception it throws is the meaningful part and
is only observable there, since `scan` flattens both outcomes to a default entry by design.

## 2026-08-17 — network + startup performance (`claude-perf-network-startup`), api 309 → 326

Opened by a read-only performance investigation of `-api` and `-app`. That survey produced fifteen
findings; this branch takes the first two phases of the resulting plan (timeouts, then startup), and
four of the original claims were **corrected on re-verification** before any code was written —
recorded here because the wrong versions were stated out loud first:

- `serverDownloadable` does **not** gate generation. Its only production caller is
  `ConfigEditor.kt:1198`, the GUI check timer — so the risk of touching it is far lower than claimed,
  but it also means an HTTP request per keystroke-pause exists purely to drive a warning label.
- `exclusionFilter` defaults to `START` (`GenerationConfig.kt:815`), so the per-comparison
  `entry.toRegex()` hits only users who chose `REGEX`/`EITHER`. The real hot-loop cost on the default
  path is the *filter read itself* — `matchesFilter` reads `apiProperties.exclusionFilter` once per
  (mod × list-entry) pair, and that getter does two `Properties.getProperty` calls, i.e. two
  **synchronized** `Hashtable` lookups, ~330 k of them for a 300-mod pack.
- Zip inspection is **not** on the GUI timer path — `checkModpackDir` only does a `listFiles`. It is
  once per generation, not once per keystroke.
- The `ModListCompiler` dependency-rescue loop, called a hotspot, is ~10–20 ms at realistic pack
  sizes. A real smell, a negligible user win; demoted to opportunistic.

**The finding that reframed the work:** nothing in the codebase set an HTTP connect- or read-timeout,
so twelve calls on the *blocking* startup path could wait forever. Not a slow start — a hang at
splash-screen 20 % with no recovery but killing the process.

Phase 0, timeouts ("pin that HTTP calls give up instead of hanging forever" red, "bound every HTTP call with configurable timeouts" fix). Guards written against a loopback `ServerSocket`
that accepts and never answers; both failed past 15 s, blocking in
`sun.net.www.http.HttpClient.parseHTTPHeader`. Fixed with a `NetworkConfig` settings group (5 s
connect / 15 s read / 60 s download-read, all tunable, `0` kept as the documented escape hatch to the
old behaviour) and one sanctioned opener, `WebUtilities.openTimedConnection` / `openTimedStream`, with
every call site routed through it. Two lessons, both now landmines in the module `CLAUDE.md`:
the opener must return `URLConnection` — narrowing it to `HttpURLConnection` turned every `file:`
download into a `ClassCastException`, which is not an `IOException` and so escaped `downloadFile`'s
error handling entirely; and `mockk(relaxed = true)` answers `0` for an `Int`, which *is* the JDK's
"wait forever", so the fixture silently reproduced the defect and both guards still failed against the
fixed code until the values were explicitly stubbed.

Phase 1, startup ("extract manifest refreshing into ManifestUpdater" extract, "pin what a manifest check costs" red, "halve startup requests and skip unchanged manifests" fix). `ManifestUpdater` extracted from
`VersionMeta` as a strict verbatim move — including the ugly `var countOldFile/countNewFile`
accumulators and the LegacyFabric equal-count nudge — purely to create a seam, since `VersionMeta`
resolves its twelve URLs from `VersionMetaConfig` constants inside its constructor and nothing about
request counts was reachable from a test. Then both `isReachable` pre-checks dropped and
`If-Modified-Since` added.

Measured, and the measurement is the point — pinned by *request count* against a loopback
`com.sun.net.httpserver.HttpServer`, never by wall-clock:

| | Requests | Bytes | Batch wall-clock (median of 3) |
|---|---|---|---|
| before | 24 | 489,038 | ~601 ms |
| after | 12 | 213,885 | ~392 ms |

Only 4 of 12 hosts honour `If-Modified-Since` (Mojang 206,986 B, fabric-intermediaries 56,270 B,
fabric-loader, fabric-installer). Deliberately no per-host special-casing: a host that ignores it
answers `200` and the version-count comparison gates the replacement exactly as before.

One behaviour preserved on purpose and pinned: an unreachable host still logs one **WARN** per
manifest. Dropping the pre-check moved that case onto the `IOException` path, which would have printed
twelve ERRORs with stack traces on every networkless launch — which is exactly how a genuine manifest
failure gets buried.

Two follow-ups deferred with numbers rather than opinions (**B30**, **B31**). B30, `If-None-Match` for
the Forge manifest, was measured and **rejected for now**: it is another 121,492 B, 57 % of what still
transfers, but ~0 ms of startup, because the twelve checks run concurrently and the critical path is
LegacyFabric at ~330 ms for **498 bytes** — pure latency, while Forge finishes in ~234 ms, below the
gate. It only matters below roughly 4 Mbit/s. B31 is the larger prize the same measurement exposed:
`ApiWrapper.setup()` already seeds every manifest from the jar, so the refresh need not block startup
at all (~392 ms → ~0), but that weakens `VersionMeta`'s construction contract and needs the grinder and
the web version-schedule checked against it first.

Doc note: the `app` row in the root `CLAUDE.md` refactor-state table said 102 tests; the suite actually
runs **108**. Pre-existing drift, not caused by this branch — corrected to the measured number without
attempting to reconstruct which six were added when.

## 2026-08-17 — GUI typing-path performance (`claude-perf-gui`), app 108 → 118

Phase 2 of the performance plan. The config-check timer is a 500 ms debounce restarted by a document
change in *any* field, running its whole validation pass for *every* open tab — so everything it does
is paid each time a user pauses while typing, multiplied by their open configs. It was doing two
things per tick from inputs that had not changed.

Sequenced as refactor → red → fix, three times over:

1. "move the check-timer's server probe and pack-name read behind the view model" **refactor(app)** — `ConfigEditorViewModel` gains `isServerDownloadable` and `packName`
   as plain delegation, and the timer/editor call those instead of reaching into `ApiWrapper`. The
   view-model now takes `ConfigurationHandler` and `ServerPackHandler` beside `VersionMeta`; all three
   are `-api` types, so it stays unit-testable without a display. Fell out of the move: the timer no
   longer builds a `PackConfig` per tick (it only read `.name` off a throwaway one), and
   `ConfigCheckTimer`'s now-unused `apiWrapper` parameter went away.
2. "make the launcher-manifest candidates askable" **refactor(api)** — `ModpackManifestParser.manifestCandidates` exposes the six launcher
   manifests `checkManifests` consults, with a `ConfigurationHandler` facade, so the app can ask
   whether they changed instead of hardcoding the paths. The alternative was a second source of truth
   that drifts — the failure this repo already documents for `SupportedModloaders`. Pinned by
   `ManifestCandidatesTest`, including that absent files are still reported (a memo must notice a
   manifest about to be created).
3. "pin how often the check-timer consults the network and the disk" **test(app)** red / "stop the check-timer re-probing the network and re-parsing the manifest" **fix(app)** — memoize both. The installer probe is cached
   per version-triple, successes only: a published installer does not vanish, but a cached `false`
   would leave the editor stuck on "server unavailable" until restart. The manifest read is keyed on a
   six-`stat` fingerprint of the candidates.
4. "pin that the autocomplete list is parsed once, not per keystroke" **test(app)** red / "parse the autocomplete list once, and stop reinstalling the LAF per keystroke" **fix(app)** — `SuggestionProvider` parses its ~550-entry
   autocomplete list once instead of per keystroke, drops the per-keystroke `updateUI()`, and hoists
   its `\W` regex.

Measured:

| | before | after |
|---|---|---|
| installer probe per debounce tick, per tab | 1 HTTP request (~234 ms) | 1 per distinct version-triple |
| manifest read per tick, per tab | 4.70 ms parse of 2,715,835 bytes | 0.021 ms, six `stat`s (221x) |
| autocomplete parse per keystroke | split + 550 sorted inserts | once per property change |

Three things worth keeping:

- **`allSuggestions()` must keep returning a fresh set.** Every caller mutates it and persists the
  result, so the *parse* is cached and copied on the way out. Caching the instance would have
  corrupted the source and accumulated across calls — caught by reading the callers before writing the
  cache, not after.
- **A `tailSet(prefix)` prefix-walk is a trap, not an optimisation** — case-insensitive matching over a
  case-sensitive ordering means matches are not contiguous (`tailSet("op")` skips `OptiFine`).
  Recorded in a comment at the site.
- **One guard was wrong on the first attempt and is worth remembering as a pattern.**
  `anUnchangedSuggestionListIsParsedOnce` first verified the *property-read* count — a claim the design
  deliberately does not make, so it stayed red against correct code. Rewritten to assert reuse by
  identity. Because the corrected form had then only ever run green, the cache was temporarily defeated
  to check it: it fails with two distinct `TreeSet` instances whose **contents are identical**, which is
  exactly why identity is the right assertion and a value comparison would have guarded nothing. Same
  lesson as the `parallelMap` guards in `-api`: being red once is necessary, not sufficient — and a
  guard corrected after the fix must be re-broken deliberately.

GUI-verified, and the method is reusable: `osascript` has no Accessibility permission on this machine,
so no synthetic clicks or keystrokes are possible. A throwaway JUnit harness drove Swing from inside
the test JVM instead (`-app` tests are not headless), showing a real provider on a real `JFrame`,
inserting characters on the EDT, and logging every visible `JList`'s row count and `preferredSize`
while `screencapture` took stills. Result: the popup **resizes** with its content —
`56x85 px at 5 matches → 54x34 px at 2` — correctly filtered, first row preselected, positioned at the
caret. A first attempt looked like a failure until focus was re-asserted before each burst; the popup
only shows while the component `isFocusOwner`. Harness deleted after use.

## 2026-08-17 — generation throughput (`claude-perf-generation`), api 329 → 337

Phase 3 of the performance plan, and the phase where **measuring first repeatedly contradicted the
plan's own estimates**. Each candidate was benchmarked at realistic pack scale before being
implemented, and the numbers reordered the work:

| Candidate | Measured cost | Verdict |
|---|---|---|
| Archive central-directory re-parses | **79.9 ms** per read, 10,000-entry archive, at two sites | the only real win |
| `Pattern.compile` per comparison (REGEX/EITHER) | 20 ms at 300 mods x 550 entries | small |
| Quilt merge nested `find` | 4.71 ms at 500 mods | trivial |
| `exclusionFilter` read per comparison | **3 ms** at 300 mods x 550 entries | negligible |
| `File(source).absolutePath` per walked file | 3.0 ms at 50,000 files | negligible |

**The `exclusionFilter` read was the plan's headline item for this phase and it is worth ~3 ms.** The
reasoning behind the estimate was sound — 330,000 synchronized `Hashtable` lookups on the default path —
but `Hashtable.get` is fast and its monitor uncontended, so the arithmetic simply does not translate into
time. That correction is recorded in the commit message rather than quietly dropped, because the wrong
version had already been stated twice (in the investigation and in the plan).

What the phase actually delivered:

- "stop a bad regex aborting the mod-list, and hoist the loop invariants" **fix(api)** — one `FilterMatcher` per generation. Its value is a **bug fix**, not speed: a
  single malformed clientside-list entry used to throw `PatternSyntaxException` out of `compileModList`
  and abort generation, because `entry.toRegex()` ran per comparison. Now compiled up front, logged once,
  skipped, and the rest of the list still applies.
- "read a modpack archive once per inspection, and index the Quilt merge" **fix(api)** — the archive is read once per inspection at both sites (~80 ms each, scaling
  with the archive), plus the two negligible hoists, each labelled as such. `putIfAbsent` rather than
  `associateBy` in the Quilt merge, because `find` returned the *first* match and `associateBy` keeps the
  last — indistinguishable under the one-entry-per-jar contract, but first-wins is what was being replaced.
- "hand out a fresh regex mod-list per read" **fix(api)** — `clientsideModsRegex`/`modsWhitelistRegex` return a fresh set per read. Not a
  performance change at all: the shared field was cleared and refilled per read, so a held result was
  emptied underneath its caller and a concurrent reader could observe it part-way through. Published, and
  the GUI reads settings from a `parallelStream`.
- "compile the Forge annotation-scanner's regexes once" **refactor(api)** — the Forge annotation-scanner's two `get() = "…".toRegex()` properties
  become `val`s, and a private `additionalDependencyRegex` holding the *identical* literal is gone. That
  duplicate was the real find: it was what the two `additionalDependency*` checks actually used, so an
  edit to the documented copy would have changed nothing there. 1.12-and-older path only, so no
  performance claim.

Testing notes worth keeping: archive open-counts are unobservable from the outside — every method returns
the same answer regardless — so `ModpackZipInspector` gained a defaulted `openZip` parameter purely to
count them. And zip4j's `addFile` with a path-in-zip writes no directory entries, which made the first
version of the open-count test fail for entirely the wrong reason.

Deliberately **not** done: the `ModListCompiler` dependency-rescue loop (`:202-222`), whose O(n²·d) shape
with two list allocations per pair is a genuine smell but ~10–20 ms at realistic sizes. Left alone rather
than churn the most delicate logic in the file for a rounding error; it is described in the root
`CLAUDE.md` hotspot notes if it ever matters.

## 2026-08-17 — web query shapes and the DBRef flattening (`claude-perf-web`), app 118 → 127

Phase 4, the only phase touching persisted data. Split deliberately: risk-free query fixes first, the
schema change and its migration second.

**4a, no schema change.** `AmountStatsService` did four full-collection loads to answer one
`/api/v2/stats` request — one for the tally and three purely for `.size`; the three become `count()`
("pin that the stats endpoint counts instead of scanning" red, "count the stats totals instead of scanning three collections" fix). `ModPackService`'s upload duplicate-check loaded every modpack to
compare one hash; extracted as `existingUploadOf` ("extract the upload duplicate-check from saveUploadedFile"), then pinned and moved onto an indexed
`findBySha256` ("pin that the stats endpoint counts instead of scanning"… see "look an upload's hash up by index instead of scanning every modpack"). Each avoided load mattered more than its row count because
of the eager `@DBRef` fan-out that 4b then removed at the root.

That fix also surfaced a latent semantic bug: with the in-memory comparison, `available.sha256 == sha256`
is true when **both** are null, so a hash-less upload would be called a duplicate of any stored modpack
that also lacked one. Unreachable from the upload path (`SavedFile.sha256` is non-null), and guarded
anyway because the parameter is nullable, stored documents genuinely carry null, and Mongo's own
`{sha256: null}` query would match them too — so the fix has to say no explicitly.

**4b, the flattening** ("embed the run-configuration mod lists instead of joining three collections"). `startArgs`/`clientMods`/`whitelistedMods` become embedded
`List<String>`; `ClientMod`, `WhitelistedMod`, `StartArgument`, their three repositories and
`ModRepository` are deleted. Each was a `@Document` whose only field was its `@MongoId` — a `ClientMod`
document is literally `{_id: "OptiFine"}` — so three collections and four repositories existed to store
nothing, and the eager join resolved to the string it was already keyed by.

Measured effect: creating a run-configuration went from ~550 sequential round-trips (one `findBy` per
entry plus a `save` per miss, on the default clientside list) to **two** calls, pinned by
`buildingAConfigurationCostsTwoRepositoryCalls`.

Two things fell out of it:

- **A real bug.** The duplicate lookup was `…AndStartArgsInAndClientModsInAndWhitelistedModsIn`, and
  Spring Data's `In` means "contains any of", not "equals" — so a configuration could be matched and
  reused because it shared a *single* mod with the one being created. Now an exact array match.
- **Four tests were deleted rather than adapted**, which is normally the stop-and-flag signal and here
  is the honest consequence: they described resolution against collections that no longer exist. The two
  that *also* covered comma-splitting were replaced by tests keeping exactly that assertion, so no
  coverage was lost. Everything else changed only by dropping `.map { it.mod }`.

The frontend moved in the same commit, because it is one contract: `types/api.ts` → `string[]`, and the
unwrapping in `RunConfigurationCard.vue` and `SubmitModPackForm.vue` (two sites) deleted. The Vitest
fixtures moved to the new shape with **expectations untouched** — they failed first with
`"[object Object], [object Object]"`, which is exactly the coupling being fixed.

**The migration** ("migrate stored run-configurations to embedded mod-lists") is what makes the flattening deployable. It is join-free: a DBRef's `$id`
*is* the value, so `{$ref:"clientMod",$id:"OptiFine"}` → `"OptiFine"` reads nothing, and still works
after the referenced collections are dropped. Element-wise so an interrupted run is completed rather
than corrupting a half-rewritten document; idempotent so a restart costs one read and no writes; on
`ApplicationReadyEvent` so an unreachable database delays it instead of blocking the boot; failures
logged and swallowed; orphaned collections dropped only after a fully successful pass.

Verified it costs the suite nothing: `WebServiceContextTest` fires the listener against an unreachable
Mongo and still runs in 0.438 s, because localhost *refuses* rather than black-holes and server
selection fails fast instead of waiting out the 30 s default. That would not hold for a remote host.

Deliberately dropped from the plan: projections for `FileCleanupSchedule` / `DatabaseCleanupSchedule`.
Their cost was the eager `@DBRef` fan-out on `findAll()`, which the flattening removed at the source, so
the remaining work would have been machinery for a midnight cron with nothing left to win.

## 2026-08-22 — the grinder as a systemd service: home resolution (`claude-fix-service-home-resolution`), api 352 → 356, grinder 233 → 237

Backfilled 2026-08-23, flagged as M1 by audit iteration 13 — this branch and the one below it both landed
without a log entry, leaving the file two branches stale at 2026-08-17.

Reported failure: the grinder, newly run as a systemd unit, died on `java.io.FileNotFoundException: /log4j2.xml`
and `Could not create directory /logs`. Reproduced by launching the installed distribution from `/`, which is
where systemd starts a unit with no `WorkingDirectory=`.

Two independent causes, which is why the fix spans two modules:

- **`-api` resolved its home to the working directory without ever asking whether it could write there.** Fixed
  by `fix(api): never resolve the home directory to a place SPC cannot write`, after
  `refactor(api): inject PathsConfig's working directory` made the decision reachable from a test at all. The
  injected value is behaviour-identical — the JVM resolves `File("")` against the directory it was *launched*
  with and ignores a later `user.dir`, so per-access and once-at-construction cannot differ.
- **The grinder never told SPC where its home was**, and could not simply log the fact first: `ApiProperties` is
  annotated `@Plugin` and *is* log4j's `ConfigurationFactory`, so the first `log.` call in the process builds one
  and it permanently keeps whatever it resolved. `fix(grinder): pin SPC's home to the daemon's base before
  anything logs` moves both claims to the top of `main`, pinned by a source-level ordering guard because a JVM
  whose logging is already initialised cannot observe the ordering from inside.

The audit of that branch (iteration 12, L1) then found the new writability probe racing on a fixed `poke`
filename: two SPC processes probing one home interleave and one wrongly concludes the home is unwritable.
Measured at 34 of 64 concurrent probes false. `fix(api): probe writability with a name nothing else can hold`
switched to `Files.createTempFile`, with a behaviour row in `claude-docs/API-BEHAVIOUR-CHANGES.md`.

## 2026-08-23 — the report's bind address (`claude-grinder-report-bind-host`), grinder 237 → 245

Reported failure: an nginx reverse proxy 502ing against the grinder's report while the report answered fine on
the box itself.

`ReportServer` had accepted a `host` since it was written, defaulting to `127.0.0.1`, and `main` had never passed
one — only the port was ever wired to the environment. A proxy in a container reaches the host over the Docker
bridge gateway, never over `127.0.0.1`, and a loopback socket refuses that at the TCP layer, so no proxy
configuration could have worked. `SPC_GRINDER_HOST` now carries it, still defaulting to loopback because the
report is unauthenticated end to end.

Pinned twice on purpose, because neither guard reaches alone: `ReportServerBindAddressTest` executes the
mechanism over a real non-loopback IPv4 (and aborts to a skip where the host has none), while
`ReportBindWiringTest` states against `main`'s own source that the variable actually reaches `ReportServer`'s
`host` — the join no test can execute, since `main` boots Docker. Reading `main`'s text is the same technique the
ordering guard above uses, and shares its brace-matched window via `grinderMainBody()`.

Audit iteration 13 then found the logged URL, which had been changed to follow the bind address, broken for two
of the shapes a bind can take: `0.0.0.0` printed an unopenable `http://0.0.0.0:8757`, and an IPv6 literal printed
`http://::1:8757`, which `URI` does not reject — it silently parses the port as `-1`. Fixed after extracting
`reportUrl` so it was testable at all; the concrete-IPv4 case was pinned green *before* the fix, so the change is
provably confined to the two broken shapes.

## 2026-08-23 — the grinder was producing nothing, in three unrelated ways

Reported as three symptoms in one message: CurseForge jars would not download, Modrinth ones downloaded but the
check never ran, and separately the report was still unreachable through the reverse proxy. They turned out to
share nothing but the day.

**The proxy was never the grinder's problem.** The bind had already been widened — `ReportServer` logged
`0:0:0:0:0:0:0:0`, which is a dual-stack wildcard socket, while `main` logged `127.0.0.1` because `reportUrl`
rewrites `0.0.0.0` into something clickable. The two lines disagreeing is what identified the bind as `0.0.0.0`.
ufw was dropping the proxy's packets: its allow rules for the port named `172.17.0.0/24` and `172.18.0.0/24`,
both with **zero packet counts**, and the proxy container sat on a third bridge. Timeout rather than
connection-refused is the discriminator, and it is now a troubleshooting row — the existing row attributes that
symptom to a loopback bind, which was true until the bind moved.

**Every container write was being refused.** The runtime image bakes in `USER 1000:1000` and `ContainerSpec.user`
defaulted to the same literal, which was correct only while the daemon ran as uid 1000 — it stopped being so on
2026-08-22, when the grinder became a systemd service under its own account. The install console shows the shape
exactly: three `Permission denied` lines near the top, the start script carrying on regardless, and twenty lines
later the JVM's `Error: could not open 'user_jvm_args.txt'`. That last line is what the failure warning quoted,
because it printed `output.lines.takeLast(25)` — so the visible evidence pointed at the start-script template
while the cause had scrolled away. Three rounds of diagnosis went to networks, templates and loader versions
before the full console was read.

Two fixes, because the reporting failure is as real as the bug: `ContainerUser.forDirectory` resolves the owner
of the mounted directory (override `SPC_GRINDER_CONTAINER_USER`, image default as fallback), and
`InstallFailureDiagnosis` scans the *whole* console for a nameable cause, since a tail is the wrong slice
whenever the first failure is survivable. The give-away worth remembering: every loader failed at once, and a
permission wall is the only thing indifferent to which loader is being installed.

**Locked CurseForge files were being downloaded and then thrown away.** `BrowserDownloader` navigates to
`/download` from inside `waitForDownload`; CurseForge answers with a file transfer, Chromium aborts a navigation
that becomes a download, and Playwright's `net::ERR_ABORTED` escaped the callback and tore down the wait that
would have caught the file. Both wordings Playwright uses are now recognised, and nothing else is — a timeout or
a DNS failure must still fail, or the downloader returns `null` in silence forever. Both navigations also stop
waiting for `load`: a CurseForge page keeps fetching ads long after it is usable, and every timeout in the run
was the untouched 30s default rather than a page-specific budget. The remaining half is on the host —
`Playwright Host validation warning` was in the log all along, listing OS libraries nobody had installed, which
is now checked by `install-grinder.sh` against the *service account's* cache rather than the caller's.

**`/as-properties`, so the fallback list stops needing a maintainer.** The grinder already boots mods
continuously and a crash is exactly the evidence the clientside list encodes, so the report server now serves a
`serverpackcreator.properties` fragment an instance can poll through
`de.griefed.serverpackcreator.configuration.fallback.updateurl`: the shipped list plus every `HIGH`-confidence
finding, whitelist passed through so it replaces the GitHub URL wholesale rather than freezing half of it.

The confidence floor is deliberately not a tunable. A clean boot proves nothing, while a false entry silently
strips a mod out of every server pack built against the list, so only a crash-proven mod is published. Two
encoding details are load-bearing and pinned by *parsing* the output with `java.util.Properties` rather than
asserting on its shape: the consumer decodes ISO-8859-1, so entries are `\uXXXX`-escaped and this one endpoint
does not answer UTF-8; and rendering is order-stable, so a poll that sees a difference has seen a real change.

Three audit passes followed (iterations 17–19). The first found a code change riding inside a `docs:` commit
and two joins with no guard at all — the browser's navigation options, and the wiring that feeds
`/as-properties` SPC's real lists — plus two silent corruptions: an entry containing a comma, which the
consumer's `split(",")` turns into two bogus prefix-matchers, and a malformed `SPC_GRINDER_CONTAINER_USER`
being discarded without a word on the one knob whose purpose is overriding a resolution that already went
wrong once.

The second pass replaced the endpoint's *model* of its consumer with the consumer: `FallbackPropertiesConsumerTest`
points a real `UpdateConfig.updateFallback` at a running `ReportServer` over loopback and checks the entries
land in `GenerationConfig.clientsideMods`. Teeth verified by dropping the continuation backslash, which
collapses the whole list to `[, entityculling-]`. It also recorded what this workstation *cannot* answer: with
a named volume chowned to `1001:1001`, a root container reads it back as `1001:1001` while a `--user 1001:1001`
container reads the same inode as `0:0` — Docker Desktop's id remapping, not kernel DAC, so neither the bug nor
the fix reproduces here. The two-command check for the Linux host is in the audit rather than a claim of
verification.

Equivalence against the base was checked the usual way — `develop`'s unmodified test tree run against this
branch's production code: **339 pre-existing guards, zero failures, zero compile errors**, so every signature
gained a default and nothing existing changed shape.

## 2026-08-23 — stopping the service actually stops the work

Asked directly whether `systemctl stop` kills the workers and the containers. The honest answer was "yes, by
two different mechanisms, and there are two holes" — which turned into this branch.

The workers were never the problem in principle: they are threads in the one JVM, so there is nothing for
systemd to kill separately. But `requestStop` sets a flag the worker loop reads *between* candidates, so a
worker parked in a boot kept going for up to that boot's fifteen-minute budget while systemd counted down.
`GrindPool.awaitStop(grace)` now signals, interrupts and joins with a deadline; a worker that ignores its
interrupt is abandoned and logged, because nothing can force a thread to die in the JVM and the actual force
is the process exiting.

The containers were the real hazard, for a reason that is not visible in the unit file: **they are children of
the docker daemon, not members of the unit's control group**, so `KillMode=control-group` never touches them.
The shutdown hook was the only thing stopping them, and it went straight to `remove --force` — a SIGKILL to
PID 1, costing an in-flight Minecraft server its world save. It now `docker stop`s each with a 15-second
window, 8 at a time, because the window is per container and ten workers stopped serially would be ten windows
and would overrun `TimeoutStopSec` into the SIGKILL the whole path exists to avoid.

Two holes the question exposed:

- Nothing stopped a worker creating a container *after* the sweep. Once shutdown hooks run, the JVM no longer
  waits for worker threads, so a worker between its loader install and its mod boot could start one that
  outlived the process. A `closed` flag now refuses creation, re-checked after the tracking-set add so a
  container created in the gap removes itself.
- A SIGKILLed JVM left containers running that **nothing could ever find again** — no label, no name, no
  autoremove, and the tracking set died with the process. They now carry
  `de.griefed.serverpackcreator.grinder` and startup reaps whatever wears it, which is the same shape as the
  staging sweep that already ran beside it.

Verified against a live daemon rather than argued: 6/6 gated cases on docker 29.7.2, including a container
trapping SIGTERM to prove the signal arrives and is honoured before removal, and a labelled orphan reaped by
the fresh engine a restart brings up. The pre-existing drain case went from instant to 15.6s, which is the
change working — busybox's shell does not forward SIGTERM to `sleep`, so it uses the whole window and is then
killed.

Three audit passes followed (iterations 20–22), and each found something the previous had not. Iteration 20
found the branch's own guarantee resting on a race: `grindAll` started its worker threads inside the `map` and
published the list `awaitStop` reads only afterwards, so a stop landing in that window would have interrupted
nobody and returned `true` — a clean stop that had not happened. It also found the hook's wiring, the 15-second
window and the unit's stop timeout all unpinned, which is the same gap `FallbackListWiringTest` was written to
close two audits earlier, simply not applied here.

Iteration 21 found the one-shot run building its `GrindPool` inline and never registering it in `activePool`,
the only handle the hook has — so Ctrl-C on the end-to-end verification path signalled and awaited nothing,
both calls no-opping through a null receiver. It looked like it worked, because the engine still closed and the
boots collapsed with their containers. The same pass caught the workers being handed a *second* full window
after the containers had spent the first, against a log line, a comment and a README section that all promised
one shared budget.

Iteration 22 found the promised single window was still not real above eight in-flight containers, because the
stop concurrency was capped there — at the deployed ten workers the container phase alone was thirty seconds
and the workers got none of the budget. The cap was raised to sixty-four, which is above anything a host has
the memory to run, and the arithmetic that had been transcribed into a test, a unit comment and the README
collapsed to one window.

Two process notes worth more than the individual bugs. **An expected red that does not arrive is the finding.**
The one-shot guard passed when it should have failed, because counting `activePool.set(` occurrences also
counts the pass loop's `activePool.set(null)` — a reset reading as a registration. And **H1's guards were never
observed red at all**: the interleaving could not be provoked at 8 workers or at 64, because the first
`Grinder.grind` initialises log4j and reliably delays worker 1 past the construction loop. That is stated in
the test's own doc rather than glossed, because a guard whose teeth were never checked has repeatedly turned
out to assert nothing.

## 2026-08-23 — the container CPU cap becomes an operator knob

`SPC_GRINDER_CPUS` caps every container the grinder starts — each mod boot and each loader install — in
cores, the way docker's own `--cpus` does. The cap was not new: `ContainerResources` has carried a
200,000µs quota since the container runtime existed, and `ContainerCandidateVerifier`,
`ContainerServerRunner` and `DockerLoaderInstaller` have all accepted one. `main` never passed one, so the
value was unreachable from outside the source — the same shape of gap `SPC_GRINDER_HOST` closed for the
report's bind address, and `CpuLimitWiringTest` is the same kind of guard, asserting the join against
`main`'s own text because `main` boots Docker.

The default stays 2 cores, so upgrading re-tunes nothing, and `ContainerResourcesTest` pins that
equivalence (`ContainerResources() == forCpus(2.0)`) instead of asserting it in prose. `forCpus` also
absorbs the two ends that otherwise surface far from their cause: `0` means an unset quota — docker's own
"no limit", matching how `0` reads for `SPC_GRINDER_CACHE_TTL_DAYS` — and anything positive below the
daemon's 1ms floor is raised, because a quota docker refuses fails every container at create time rather
than throttling it.

**The finding was in the half nobody would have looked at.** `hostConfigFor` sent `withCpuQuota` and no
period. A quota is a fraction of a period, so the real cap was whatever the daemon's default period made
it — correct today by coincidence, since the kernel's `cpu.cfs_period_us` is the 100ms the arithmetic
assumed. Measured against a live daemon (Docker 29.7.2) with the period dropped and a 50ms period
requested, 1.5 cores arrived in the container's cgroup as `75000 100000`: **0.75 cores, silently halved,
with nothing reporting a problem.** `theCpuCapReachesTheKernelWithItsPeriod` reads the numbers back from
*inside* the container for that reason — docker echoing a `HostConfig` only proves the field was
transmitted — and uses the non-default period on purpose, since at 100ms the assertion passes with the
period never sent. That is the guard whose teeth were checked by removing the fix and watching it go red.

The operator-facing half states what the knob does *not* cover, which is the more useful sentence: a
unit-level `CPUQuota=` bounds the JVM's host-side work (mod resolution and downloads, pack generation, the
headless Chromium a distribution-locked CurseForge file needs) and can never reach a boot, because
containers are children of the docker daemon rather than of the service's control group — the same fact
that makes the shutdown hook the only thing able to stop them. Both halves are now in README §5 (*Capping
CPU*, with `workers × cpus` as what the grinder can occupy) and in the unit as a commented `CPUQuota=`
beside the new `Environment=` line. The floor is stated too: below ~1 core a boot that cannot reach its
ready-line inside 15 minutes is scored INCONCLUSIVE, which reads as a mod that hangs rather than as a
starved host — so fewer workers beats starving each of them.

Suite 290 → 298, 0 failures, 22 skipped; the gated `DockerJavaContainerEngineIT` green at 7/7.

**Audit iteration 23 then found the knob's own inversion.** `forCpus` used its *computed* quota as the
"uncapped" sentinel, so any count below 5e-6 cores rounded to 0µs and returned quota `0` — which is docker's
no-limit, verified in the container's cgroup as `max 100000`. A request for the smallest possible cap
produced no cap at all, against a KDoc that promised the floor, in the one direction a hardening knob must
not fail. The decision now reads the input (`cpus == 0.0`), the sentinel is named, and non-finite input is
rejected up front: both `Infinity` and `NaN` survive `String.toDouble()`, and both round into a lie —
`Long.MAX_VALUE` (so large it means uncapped) and `0` (uncapped outright). The same pass established, by
measurement, that an over-large value needs **no** clamp: on a 16-core host a 1000-core quota is accepted and
reported verbatim, because the raw cfs path carries none of `--cpus`'s host-bound validation. Three
operator-facing findings went with it — the startup line now states the cap as `cpus=2.0 cores
(200000/100000µs)` or `cpus=uncapped` instead of a raw quota that read as "zero CPU" at the escape hatch,
`### Capping CPU` stopped splitting the worker-sizing section in half, and the installer's "worth a decision"
list names the knob. Final: **303 tests, 0 failures**, 16 skipped with the gated Docker IT enabled and 23
without.

## 2026-08-23 — the memory cap joins it, with a warning instead of a formula

`SPC_GRINDER_MEMORY_GIB` completes the per-container budget: both caps now come from the environment through
one `ContainerResources.forLimits` call, on the rules the CPU knob established — exact `0` uncapped, a smaller
positive value raised to the daemon's floor rather than refused by it ("Minimum memory limit allowed is 6MB",
its own words), negative and non-finite rejected. The default stays 3 GiB, so nothing an install already runs
changes, and a new guard pins the thing neither literal default could: that `main`'s fallbacks resolve to
*exactly* the `ContainerResources` property defaults, since every other construction site falls back to those
independently. Its teeth were checked by flipping the class default to 4 GiB and watching it fail.

**Why it shipped with a warning rather than as a lever.** The knob was asked for with "only change this when
you know what you are doing", and the measurement behind that turned out to be sharper than expected: the
packs the grinder builds leave `javaArgs` empty, so nothing passes `-Xmx` and the JVM derives the server's
heap from the container's cgroup limit — Temurin 21, `--memory=3g` → `MaxHeapSize 805306368` (768 MiB, 25%);
`--memory=1g` → `268435456`. The cap is therefore not a ceiling the boot happens to sit under, it is *what
the heap is*. And it is simultaneously the divisor in §5's worker-sizing formula. So lowering it starves boots
of heap, raising it without lowering `SPC_GRINDER_WORKERS` over-subscribes the host by exactly that factor,
and both failures are OOM kills scored `INCONCLUSIVE` — indistinguishable, from the report, from mods that
hang. That is the whole content of the README warning, the unit's comment block and the entry point's own:
if the intent is "grind faster", the levers are `WORKERS` and `CPUS`.

`CpuLimitWiringTest` became `ContainerLimitsWiringTest` in the process (it guards two knobs now, with the
existing assertions intact and the memory equivalents added). Suite 303 → **310, 0 failures**, 16 skipped with
the gated Docker IT enabled and 23 without.

## 2026-08-23 — one build is not a mod: the other-version crash re-check

`iron-chests` was reported `HIGH` off `Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)`, with the note
"Declared server/both but the server crashed — a strong clientside signal". It is not a clientside mod, and
the engine had no way to know: exactly **one** build of a project was ever booted, so "this build crashes"
and "this mod cannot run on a server" produced identical evidence, and the tie was broken toward the answer
that reaches `/as-properties` — where a wrong entry silently strips the mod from every server pack built
against the fallback list.

The third guard against a false `HIGH` (after the selection-time loader/Java gate and the classifier's
setup-abort/killed mapping, and alongside the loader-build re-check) is therefore: **a crash that contradicts
the metadata is re-checked on other versions of the mod**, and one clean boot there clears it. A mod that
cannot run server-side cannot run server-side in *any* build, so a version that boots proves the crash
belonged to that build. The sample is the newest file of each of the next two most-recent Minecraft versions
— one per version, because two rebuilds for one Minecraft are near-identical code while a different version
line is an independent sample — and it stops at the first clean boot.

**The gate is the contradiction, not the crash.** It arms only when the platform's `server_side: required` or
SPC's own jar scan claims server support, which is the same predicate that prints that note
(`ClientsideVerifier.declaresServerSupport`, now shared so the two can never drift about what "declared
server" means). Where the metadata already leans clientside, the crash *confirms* it and a re-check would
spend boots to learn nothing while the crawl falls behind — and in a catalog sweep that agreement is the
common case. Worth knowing for CurseForge, which is where the report came from: it has no sideness field at
all, so the claim can only ever come from the jar scan, and a gate reading the platform alone would never arm
for a CurseForge mod.

Every other direction stays conservative, matching the loader-build re-check: crashes elsewhere corroborate
and are named in the detail, and an attempt that learned nothing — staging failed, timed out — leaves the
crash exactly as it was. Budget is a constructor knob (`otherVersionRecheckLimit`, default 2, `0` off) rather
than an env var: no new deployment surface for a number nobody has evidence to tune yet. Cost is two extra
boots per contradicting crash and nowhere else.

**A defect the change forced out of hiding.** Every attempt for one candidate stages into
`<work>/boot/<slug>-<loader>`, which staging wipes, so all of them write the same `boot.log` — while the
*reported* verdict is frequently not the last boot, since both re-checks keep the original crash. The
grinder's reaper then keeps that single file and deletes the staging around it, so the console a `HIGH` was
diagnosed from was a different boot's. Pre-existing since the loader-build re-check landed and occasional;
with up to three boots now sharing the file it would have been near-certain. `BootOutcome` carries its own
console and `verify` writes the decided one back, best-effort like the write it repairs.

Both fixes' guards had their teeth checked rather than assumed: stubbing the survivor lookup to `null` fails
the two clearing guards, removing `distinctBy` fails the one-per-version guard, and removing the restore's
`writeText` fails the console guard. Suite 93 → **113, 0 failures**.

## 2026-08-23 — the disproof was already in hand: cross-loader reconciliation

The live verdict pair for `iron-chests` turned the previous entry's guess into evidence, and added a finding
it had missed. Both rows come from **one run**:

| Loader | Confidence | Entry | Boot |
|---|---|---|---|
| Forge | `HIGH` | `ironchest-` | Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1) |
| NeoForge | `LOW` | `ironchest-` | NeoForge 21.11.45 / Minecraft 1.21.11 → SURVIVED (exit 137) |

The engine booted a real Minecraft server with this mod, watched it reach its ready-line, and then published
the mod as clientside off the *other* loader's crash. (Exit 137 on the surviving row is not a kill worth
investigating: `ContainerServerRunner` watches for the ready-line and stops the container the moment it
appears, so every clean container boot exits 137.) The shape also confirms the abandoned-port hypothesis —
Forge stops at 1.20.2 while NeoForge is at 1.21.11, i.e. the project migrated and left one final Forge build
behind.

**Why one loader's crash is not the other loader's business — except that it is.** The per-loader model is
deliberate, and a mod genuinely can be client-only on one loader. But the *published* artefact is a
loader-agnostic file-name stem matched with `startsWith`, and both rows derive `ironchest-`, so publishing
the Forge crash strips the NeoForge build that had just proven itself. `reconcileAcrossLoaders` therefore
keys on **the entry colliding**, not on any survival anywhere: where the stems differ nothing is stripped and
there is no contradiction to resolve. The confidence drops to whatever `aggregate` yields for the same
signals with no boot — re-derived, so there is one ladder rather than a second one — while `bootResult` and
the crash excerpt stay, because the server did crash and that is worth diagnosing. The note is *rebuilt*
rather than appended to: it used to end in "a strong clientside signal", and bolting a correction onto a
false sentence is the stale-prose failure this project keeps paying for.

**And the fix from earlier the same day would probably not have saved this mod.** `CurseForgePlatform.resolve`
took `?pageSize=50` — the newest 50 files *across all loaders*. A project that migrated Forge → NeoForge keeps
publishing NeoForge builds, so its last Forge build sinks toward the far end of that window and the builds
before it drop out of it entirely, leaving the other-version re-check nothing of that loader to boot. Exactly
the projects that produce the false positive are the ones the window hides the evidence from. Resolution now
walks `index` until `totalCount`, capped at `MAX_FILE_PAGES` (10 × 50) with a warning when it truncates; a
project inside one page still costs one call. Dependency resolution stays single-page on purpose — it needs
*a* usable file for one loader/Minecraft pair, not a history.

The two crash guards layer rather than compete: the within-loader re-check runs during the crashing loader's
own boot, cross-loader reconciliation after every loader is in, so a crash must survive both. Loaders are
assessed in sorted order and nothing looks ahead, so a project like this one still pays the two extra Forge
boots before NeoForge supersedes them — deliberate, since those boots also produce the more specific
within-loader answer.

Teeth checked: relaxing the entry-collision condition fails `aLoaderBootingUnderADifferentEntryDisprovesNothing`;
capping the file walk at one page fails `resolvePagesThroughEveryPublishedFile` and
`aTotalCountThatIsNeverReachedStopsAtTheCap`. Suite 113 → **126, 0 failures**.

---

## 2026-08-23 — `creativecore`: a source jar as a list-entry, and a re-check that never left the neighbourhood

Reported the same day as `iron-chests`, and it survived every guard that case installed. `creativecore` — a
library mod whose own project description advertises server-side features — was published `HIGH` clientside
for Modrinth/Fabric under the suggested entry **`CreativeCore-sources`**. Two independent defects had to line
up for that, and each is worth its own note.

**A Modrinth version's `files[]` is not a list of mods.** `filesOf` mapped every entry onto a `ModFile`, and a
Modrinth version routinely carries more than one: authors attach source jars, flagged `"primary": false`.
Measured against the live API: the project publishes 300 versions, its Fabric group holding 143 files, of
which exactly one is the stray `CreativeCore-sources.jar` (fabric, 1.21.1, non-primary, uploaded 2024-09-04).
That single name shares no delimited prefix with the `CreativeCore_FABRIC_v*.jar` builds, so
`FilenameStemDeriver` fell through to its last resort — strip the version off the **shortest** name — and
derived an entry matching nothing the project has ever shipped. The deriver behaved exactly as documented;
it was fed something that is not a mod.

The knock-on is the interesting part. `loaderDisprovingTheCrash`, installed hours earlier, compares *entries*,
and `CreativeCore-sources` matches neither of the other loaders' `CreativeCore_`. So the guard that exists
precisely to stop one loader's crash outranking another loader's clean boot looked at a run where NeoForge had
booted a server, found no colliding entry, and let the Fabric crash stand. A garbage stem does not merely
publish a useless entry — it disables the disproof.

`modFilesOf` now keeps only the primaries, falling back to every file of a version that flags none. That
fallback is load-bearing rather than defensive: 3 of the 300 versions genuinely carry no primary flag, and
dropping them would lose real builds. CurseForge has no equivalent field, and nothing has been seen publishing
a source jar as a plain CF upload — stated so the asymmetry is a known gap, not an oversight.

**The other-version re-check spent both boots in the crashing combination's own neighbourhood.**
`pickRecheckCandidates` took the newest file of each *other Minecraft version* of the crashing loader, which
with a budget of two means the two versions either side of it. Here: Fabric / MC 26.2 crashed, and the
re-checks went to Fabric 26.1.2 and Fabric 26.1 — same loader, same loader version `0.19.3`, adjacent
Minecraft versions, i.e. near-identical code re-tested in a near-identical environment. Both came back
INCONCLUSIVE (exit 1 and exit 0), so the crash stood. Meanwhile, in the *same* run, NeoForge 26.1.2.97 booted
a server for this project, and the CurseForge sweep two minutes earlier had booted
`CreativeCore_FABRIC_v2.14.13_mc26.1.jar` — the exact file the Modrinth 26.1 re-check gave up on — to a clean
ready-line. The evidence existed; the sample was aimed away from it.

Each pick now has to introduce a Minecraft **version-line** and a loader that no earlier pick used, considered
newest-Minecraft-first, with the crashing combination's own line marked used from the start. A line is the
first two components (`26.1.2` and `26.1` are one, `26.2` another) because that is the granularity at which
mod source actually differs — builds within a line are ports of the same source across a patch release. On
this shape the same two boots become Fabric 26.1.2 and NeoForge 1.21.11 — not asserted from the
miniature in the unit test but from running the real `ModrinthPlatform` and `pickRecheckCandidates` over the
project's live 300-version response, which is also where the recovered `CreativeCore_FABRIC_` stem was
confirmed.

**Diversity is a preference, not a filter**, and that distinction is pinned: selection relaxes to a new line,
then a new loader, then whatever is left, so a project publishing one loader and one Minecraft line samples
exactly as deeply as it did before. The budget is unchanged — this buys better boots, not more of them.

**Crossing the loader is a wider claim than `loaderDisprovingTheCrash` permits, and the difference is the
gate.** That pass runs on *any* crash, so it insists on a colliding entry; this sample is spent only where the
crash already contradicts a declared server support, i.e. where one of the two signals is already known to be
wrong. A project whose author declares it server-capable, and which boots a server under another loader, is
far better explained by a broken build than by sideness. Two consequences fall out: every attempt's label now
names its loader, because the returned outcome may be a boot run under a different loader than the verdict is
about; and every attempt still stages into the **crashing** loader's directory, since staging under the
candidate's own would wipe the pack and console that loader's own verdict is about to be built from.

Teeth checked: both Modrinth pins were committed red and fail on the unfiltered `files[]`
(`nonPrimaryFilesAreNotModFiles`, `aSourceJarDoesNotPoisonTheDerivedListEntry`); the selector pins were
committed red as a compile failure, the honest shape of a signature change, and
`aCrashIsReCheckedOnAnotherLoaderRatherThanTwiceOnItsOwn` is the miniature of the live report.
Suite 126 → **130, 0 failures**.

---

## 2026-08-23 — the same slug on two platforms was one directory

Follow-up to the `creativecore` report above, from the loose end it left: the Minecraft 26.2 boots in that
report did not merely disagree with each other, they disagreed *about the same build*. CurseForge had
NeoForge 26.2.0.66 / MC 26.2 → **SURVIVED** (exit 137) while Modrinth had NeoForge 26.2.0.66 / MC 26.2 →
**CRASHED** (exit 1) — identical loader build, identical Minecraft, identical mod, verdicts 71 seconds apart.
A CurseForge Fabric boot exited **127**, which is a shell reporting that the command it was told to run does
not exist. And the Modrinth Fabric re-check on `CreativeCore_FABRIC_v2.14.13_mc26.1.jar` came back
INCONCLUSIVE (exit 0, no ready-line) on the very file the CurseForge run had booted to a ready-line two
minutes earlier. Those are not four flaky boots; they are one cause.

**Per-attempt scratch space was keyed on `(slug, loader)`.** Staging *wipes* that directory before using it
(`stageBootPack` opens with `deleteRecursively()`), and `BootWorkspaceReaper.reap(slug)` deletes it again once
a candidate's verdicts are in. The grinder, meanwhile, is explicit that the same slug on Modrinth and on
CurseForge is two candidates — `Grinder` keys verdict freshness on `(platform, slug)` and says so in a comment
— and `GrindPool` runs them on parallel workers. So both runs of `creativecore` shared
`<work>/boot/creativecore-NeoForge`, and either was free to delete the server pack out from under a container
the other was still booting. Exit 127 is the signature of exactly that: `start.sh` went missing mid-run.

The reaper had a landmine for the neighbouring hazard already — *"scoped to one slug on purpose … workers run
in parallel, and a prefix match would delete the pack out from under a container that is still booting it"* —
and its test carried `leavesOtherCandidatesAlone`. Both reasoned about *different* slugs. The case where two
candidates **share** a slug was the hole, and it is the case the platform column exists to name.

**`AttemptDirectory`** now builds `<platform>-<slug>-<loader>` and reads it back to its owner. Both halves
live in one object in `-clientside` because three callers depend on them agreeing: `ClientsideVerifier` for
the jar-scan download, `BootVerifier` for the staged pack, and the grinder's reaper, which decides what to
delete from the name alone. Until now they agreed only by two separate string literals happening to match —
the kind of coupling that survives until someone changes one of them. Parsing still cuts only the loader
suffix rather than prefix-matching the slug, so `creativecore` does not claim `creativecore-extras`.
Directories staged under the old name match no owner and are cleared by the startup `reapAll()`.

**Why this mattered more than a lost run.** Every affected boot was scored as evidence about a mod when it was
evidence about a deleted directory — and the confidence model is deliberately asymmetric: a crash is the one
outcome that reaches HIGH. A boot the environment destroyed therefore does not degrade to "we learned
nothing", it manufactures a false positive, and a false positive is what writes a wrong entry into the
fallback list. Two of the guards this project already built exist to catch environment failures masquerading
as crashes (`killedExitCodes`/`outOfMemoryMarkers`, `launchFailureMarkers`); this one produced consoles those
guards had no reason to distrust.

Teeth checked: reaping on the bare slug fails `reapingOnePlatformLeavesTheSameSlugOnAnotherPlatformAlone`;
restoring either producer's `"${project.slug}-$loader"` fails
`theJarScanOfTwoPlatformsSharingASlugDownloadsIntoSeparateDirectories` and
`theSameSlugOnTwoPlatformsStagesIntoSeparateDirectories`. Clientside 130 → **134**, grinder 310 → **311**,
0 failures in either.

---

## 2026-08-23 — the immediate re-grind queue: how a defect in the *engine* gets un-published

Three engine defects landed in one day — a source jar becoming a list-entry, a crash re-check that never left
the crashing combination's neighbourhood, and two platform runs of one slug sharing a staging directory. Each
one invalidated verdicts that were **already being published** through `/as-properties`, and none of them had
a remedy: the catalog crawl plus the 30-day re-verify TTL answer *when does this project come round again?*
with **eventually**. Correct when a mod changes. Wrong when the bug is ours, because then the answer is
"serve the wrong clientside entry for a month".

`RequeueStore` is the missing lane. Persisted (`SPC_GRINDER_REQUEUE`), drained at the **start of every pass**
ahead of the catalog slice, and ground with `force = true`.

**The force is the whole feature, and it is the part that would have been easy to leave out.** A project is
queued precisely because its stored verdict is wrong — and a wrong verdict is almost always a *recent* one,
since engine defects get found by reading verdicts that were just produced. Without the force a drained queue
turns straight into `SKIPPED_FRESH`: the log says the queue drained, the queue is empty afterwards, and
nothing was re-verified. That is a failure mode that looks exactly like success, which is why
`aForcedGrindReVerifiesEvenAFreshVerdict` pins both directions in one test.

**Two selectors, because two things actually happen.** `--requeue <url>…` is a named handful — a report a
user disputed. `--requeue-before <instant>` is the recurring one, and the reason the feature generalises: a
defect invalidates a *population*, not a list somebody assembles by hand. Naming the moment is also
auditable — a reader of the log can tell exactly which population was re-verified and why. One candidate per
*project* rather than per verdict row, identified by platform plus the platform's own id where known, so a
renamed project is one re-grind and the same slug on two platforms is still two.

**Three placement decisions, each with a reason that is not obvious from the code.**

*Not an HTTP endpoint.* The report server has no authentication — that is deliberate and landmined — so a
write endpoint on it would let anyone who can reach the page schedule unbounded container work. The queue is
authored through the CLI, i.e. through the machine's own access control.

*Before `claimSpcPreferencesNode()` and `pinSpcHomeDirectory()`.* The command is run **against a daemon that
is already up**. Claiming the preferences node or re-pinning SPC's home from a one-shot would move the home
out from under the running service, and those claims are remembered for every later run.

*Stdout, never `log`.* This is the same landmine one level removed: `ApiProperties` is registered as log4j's
`ConfigurationFactory`, so the first log statement in a process constructs one — the very thing the claims
exist to control. A `log.info` on this path would re-introduce the hazard from inside a helper, where the
existing guard (which scans `main`'s body) could not see it.
`theRequeuePathRunsBeforeTheClaimsAndNeverLogs` therefore asserts the ordering *and* reads the helper's own
source for `log.`.

**Verified against the real entry point**, not only through the suite, because the operator-facing half is
exactly what a mocked test cannot answer. A store sliced from the live 875-verdict file, run through
`:serverpackcreator-grinder:run`:

| Command | Result |
|---|---|
| `--requeue-before 2030-01-01T00:00:00Z` | 14 rows → **7 distinct projects**, both platforms of `chipped` and `ambientsounds` kept apart |
| the same command again | `Queued 0 of 7 … (7 already waiting)` — additive and idempotent |
| `--requeue https://modrinth.com/mod/creativecore` | `Queued 1 of 1 … 8 now pending` |
| `--requeue-before yesterday` | the ISO-8601 hint, not a stack trace |

The run left **only `requeue.json`** in the home — no `logs/` directory — which is the observable proof that
no `ApiProperties` was constructed and the landmine above holds in the built artefact rather than only in the
source guard.

Suite: grinder 325 → **336, 0 failures**.

---

## 2026-08-23 — three grinder reports: a favicon, container name resolution, and two Forge failures

Five items, all from the live daemon at `grinder.serverpackcreator.de`. Two of them were false clientside
evidence; one was noise that turned out to be a real environment defect; two were interface work.

### 1. The report serves its own tab icon

`img/config.png`, copied byte-identical into the grinder's resources and served off the classpath, so the
page still fetches nothing external. Registered under **both** `/favicon.ico` and `/favicon.png` — the red
test is what showed why: without its own context, a browser's unprompted `/favicon.ico` request falls through
to the catch-all `/` and is answered `text/html` with the whole verdict table. Asserted on the PNG signature,
because a 404 page and an HTML fall-through are also non-empty 200 bodies.

### 2. A boot container could not resolve its own hostname

`Modrinth-chloride-NeoForge.log` opened with three `UnknownHostException: 928f022c75b5: Temporary failure in
name resolution` stacktraces before a single mod was loaded. Cause: the daemon writes an `<ip> <hostname>`
line into `/etc/hosts` only for a container that *has* an address, and a grinder boot is `--network none`.
log4j calls `InetAddress.getLocalHost()` while configuring itself, so every boot paid for it.

Fixed by adopting exactly what the daemon does for a networked container, with loopback standing in for the
address it cannot have: a fixed hostname (`spc-grinder` — the mapping is part of the create call, and the
container id does not exist until after it) plus `--add-host spc-grinder:127.0.0.1`.

Measured against docker 29.7.2 under `--network none`, through `wget` because it calls the same `getaddrinfo`
the JVM does:

| | console |
|---|---|
| before | `wget: bad address '11419499a196:1'` |
| after | `wget: can't connect to remote host (127.0.0.1): Connection refused` |

i.e. resolution now reaches the connect. `--add-host` is honoured with no network at all, which is what makes
this possible without granting the boot one. The guard (`theContainersOwnHostnameResolvesWithoutANetwork`)
was committed red against a live daemon and is green after.

### 3. A Forge server that never bootstrapped was scored as a mod crash

`CurseForge-ars-nouveau-Forge.log` died in `BootstrapLauncher.main` with `IllegalStateException: Could not
find parent layer for module \`java.base\` read by \`net.minecraftforge.eventbus\``. Non-zero exit, no
ready-line, nothing else recognised — so it reached the classifier's floor as **CRASHED**, i.e. a clientside
HIGH for a mod whose code never ran.

The cause is upstream and deterministic, established by reading both sources and then reproducing it:

* `ServerStarterJar`'s `installModulePath` defines a layer for the module path in `unix_args.txt` with
  `List.of(ModuleLayer.boot())` as parent, then makes `ModuleLayer.boot()` return it.
* Forge's `SecureModuleClassLoader` resolves a read module's configuration by scanning its **direct** parents
  (`parents.stream().filter(p -> p.configuration() == other.configuration())`) and throws when none matches.
  `java.base` lives one level further up, in the real boot configuration.
* cpw's original `ModuleClassLoader` — what NeoForge runs — ends the same lookup with
  `.orElse(ClassLoader.getPlatformClassLoader())`. That asymmetry is the whole reason the same starter jar
  launches NeoForge and not Forge, and it is why `HELP.md` already records "people ran into trouble when using
  Forge and Minecraft 1.20.2 and 1.20.3" with `USE_SSJ` as the escape hatch.

Two fixes, because the defect has two halves:

**The classifier can no longer read it as a crash.** New `loaderBootstrapFailureMarkers` rung between
launch-failure and killed/OOM, matching the *message* and not the module — reproduced locally, the identical
run named `java.management.rmi` read by `JarJarMetadata` instead, so the iteration order varies. The starter
jar's own give-ups (`Failed to find run file at`, `Failed to find startup arguments using run script path`)
joined it.

**The grinder's Forge boots take the hatch.** `PackVariables` now writes `USE_SSJ=false` on every pack, on the
install boot as well as the mod boot (installing one way and launching the other would cache a layer the
offline boot cannot use). Measured on Forge 1.20.2-48.1.0, installed by its own `--installServer` and booted
under `--network none` with a 3 GiB cap on Temurin 17:

| launch | result |
|---|---|
| `-jar server.jar --installer-force --installer …` | `IllegalStateException: Could not find parent layer for module` at `SecureModuleClassLoader.java:137` |
| `@user_jvm_args.txt @libraries/…/unix_args.txt nogui` | `[Server thread/INFO]: Done (5.183s)! For help, type "help"` |

Same install, same JVM, same flags otherwise. That run also confirmed fix #2 end to end: the only
`UnknownHostException` left in it is `api.minecraftservices.com`, which is the no-network design working.

**And the gap the fix opened, closed in the same branch.** Forge now boots from an `@argfile`, so an install
layer cached without `unix_args.txt` fails with the launcher's `Error: could not open \`…'` — Temurin 17,
verbatim — which `launchFailureMarkers` did not know and which therefore scored CRASHED. It is the same
incomplete-cached-install case that guard already existed for; only the file the boot depends on changed.
Matched with the launcher's own `Error: ` prefix so a mod logging "could not open" about one of its own files
is still judged on its merits.

### 4. `Modrinth-polytone-NeoForge.log` — a correct verdict, and a real finding underneath it

The verdict is right: `NoClassDefFoundError: net/minecraft/client/multiplayer/ClientLevel` from
`mods/polytone-26.2-6.4.1-neoforge.jar` is the decisive `clientOnlyClassMarker`, and the mod earns its HIGH.

What the log also shows is `NoClassDefFoundError: Could not initialize class com.sun.jna.Native` and
`Failed retrieving info for group processor/memory/software`. Docker mounts a `--tmpfs` as
`rw,nosuid,nodev,noexec` (verified on 29.7.2) and the rootfs is read-only, so JNA cannot extract and map its
native library — which is what Minecraft's own `oshi` system-report probes need. Harmless *here*: it degraded
only the crash report's diagnostics. **Not necessarily harmless in general** — a mod needing JNA at load time
would fail for the environment and arrive at the classifier as a crash. Landmined in
`grinder/container/CLAUDE.md` rather than fixed, because adding `exec` to `/tmp` is a deliberate weakening of
the untrusted-mod posture and that is a decision, not a cleanup.

### 5. The overview says when each mod was scanned

`GrindVerdict.verifiedAt` was recorded from the start — it is what the re-verify TTL compares against — and
shown nowhere, so a reader could not tell a fresh verdict from one reached weeks ago on a loader build long
since superseded. Now a `Scanned (UTC)` column on the table and a `Scanned` column closing each CSV row,
through one shared `ScanDate`, because the download button hands out the exporter's own output and a
divergence would show as the page disagreeing with its own file. `YEAR/MM/DD`, UTC (a stored `Instant` reads
the same on any host) and zero-padded (the table sorts as text, so `2026/1/5` would sort after `2026/11/…`).

### Suites

clientside 136 → **138**, grinder 344 → **351**, zero failures. Counts read back from
`<module>/build/test-results/test/*.xml`. Every code commit is preceded by its own red `test(...)` commit.

---

## 2026-08-23 (later) — the template fix: which Forge versions the ServerStarterJar cannot launch

Follow-up to item 3 above, after Griefed asked for the shipped-template change and — crucially — for it to
be **tested rather than assumed**. That instruction is what saved it: the suggestion in the earlier report
was *wrong*.

### The wrong hypothesis, and what disproved it

Reading the sources said: Forge switched from cpw's `securejarhandler` to its own `securemodules` fork at
Minecraft 1.20.2, cpw's parent-layer lookup ends in `.orElse(getPlatformClassLoader())` while Forge's
*throws*, and the throw is still present in `securemodules` **2.2.21** (verified in the jar's own class
bytes). Conclusion: every Forge from 1.20.2 onwards is unlaunchable by the ServerStarterJar.

Then `1.21.1-52.1.0` booted **through** the starter jar — `Done (6.593s)! For help` — logging the line that
explains everything:

```
Launching in jar mode, using jar: /w/forge-1.21.1-52.1.0-shim.jar
```

The deciding artefact is not the module loader, it is **which argfile the installer writes**:

| Minecraft | argfile | shim jar | ServerStarterJar |
|---|---|---|---|
| 1.17 – 1.20.1 | `-p <module path>`, cpw securejarhandler | no | works — cpw's loader falls back |
| **1.20.2** | `-p <module path> --add-modules ALL-MODULE-PATH`, Forge securemodules | **no** | **dies** |
| 1.20.3 onwards | `-jar forge-<version>-shim.jar` | yes | works — jar mode, nothing synthesised |

Boots, all on Temurin under `--network none` with a 3 GiB cap:

| Forge | through SSJ | from its own argfile |
|---|---|---|
| `1.20.1-47.4.0` | ready-line reached | — |
| `1.20.2-48.1.0` | `IllegalStateException` at `SecureModuleClassLoader.<init>` | `Done (5.183s)! For help` |
| `1.21.1-52.1.0` | `Done (6.593s)! For help` | — |

Had the wrong rule shipped, every modern Forge pack would have lost the hosting-company compatibility the
starter jar exists to provide. `securemodules` 2.2.21 still containing the throw is exactly the kind of
evidence that reads as conclusive and is not: the code is there, the path to it is gone.

### What shipped

`forgeNeedsItsOwnArgfile` in all three templates, as a *second* independent reason to bypass the starter
jar beside the existing Java-24 one; the two now share one argfile block instead of two copies. 1.20.3 is
bypassed on HELP.md's word rather than a boot — it ships the shim, so it probably works, but it has two
Forge builds in total, so over-including costs nothing and under-including costs a dead server.

**Verified by execution in all three shells, not by reading two of them.** bash through the new
`ScriptTemplateContentTest` case; fish and PowerShell by extracting the function and driving it in
containers, since neither is installable on every dev machine:

| Minecraft | 1.17.1 | 1.19.2 | 1.20 | 1.20.1 | 1.20.2 | 1.20.3 | 1.20.4 | 1.21.1 | 26.2 | 26.20.2 |
|---|---|---|---|---|---|---|---|---|---|---|
| bash / fish / pwsh | SSJ | SSJ | SSJ | SSJ | **bypass** | **bypass** | SSJ | SSJ | SSJ | SSJ |

All ten agree in all three. `26.20.2` is why the major is part of the test: it matches 1.20.2 component for
component below the major. Whole templates also pass `fish -n` and PowerShell's own
`Parser::ParseFile` — the PowerShell behaviour run needed `-Command` rather than `-File`, because the
amd64 image aborts under QEMU on this host with `-File`.

### And the grinder's workaround came back out

The earlier `USE_SSJ=false` in `PackVariables` was right while the templates could not tell the affected
versions apart, and wrong afterwards: it is blanket, so it also disabled the starter jar for 1.17–1.20.1
and 1.20.4+, where it works. The grinder would then boot every Forge pack by a route almost no user's pack
takes — and would never again notice that route breaking. It noticed once, which is why the templates now
decide, so the knob is reverted and `leavesTheStarterJarChoiceToTheTemplates` fails if it returns.

`variables.txt` and `HELP.md` now tell operators they should not need the knob at all, instead of naming
two Minecraft versions and leaving them to act.

Suites: api 356 → **361**, clientside **139**, grinder **351**, zero failures — read back from
`<module>/build/test-results/test/*.xml` after the run, not carried forward from the earlier section.

---

## 2026-08-24 — the boot tmpfs is executable now, and `noexec` was costing more than it looked

Griefed's call, after the earlier report left it as a decision: grant `exec` even though it weakens the
sandbox. Measuring it first changed what the change is *for*.

The finding that raised it was cosmetic — `NoClassDefFoundError: Could not initialize class
com.sun.jna.Native` in `Modrinth-polytone-NeoForge.log`, degrading only the crash report's own system
information. Booting a real Forge server under the grinder's actual posture (read-only rootfs, `--tmpfs
/tmp:rw`, no network, all caps dropped, no-new-privileges) showed the real cost:

| `/tmp` | boot console |
|---|---|
| `rw` | `NativeLibraryLoader: /tmp/libnetty_transport_native_epoll_aarch_64….so exists but cannot be executed even when execute permissions set; check volume for "noexec" flag` → `Using default channel type` |
| `rw,exec` | `Using epoll channel type` |

So **every boot the grinder has ever run** fell back from Netty's native epoll transport to NIO, and said so
in a line nobody was reading. JNA on its own, same posture otherwise:

| `/tmp` | `com.sun.jna.Native` |
|---|---|
| `rw` | `UnsatisfiedLinkError: /tmp/jna….tmp: failed to map segment from shared object` |
| `rw,exec` | `JNA-OK pointerSize=8` |

**What was given away.** A mod can now run a native binary it wrote into `/tmp`. Set against a workload that
is already an untrusted JVM — an arbitrary-code execution engine — in a container with no network, no
capabilities, no privilege escalation, a read-only rootfs and a non-root user, none of which changed. And
`nosuid`/`nodev` stay: docker applies both even when only `exec` is asked for, verified rather than assumed
(`rw,exec` and `rw,nosuid,nodev,exec` both yield `rw,nosuid,nodev,relatime`).

The guard **executes** a binary out of `/tmp` rather than reading the mount flag — the flag is the mechanism,
running the file is the promise — and then asserts the two options that must *not* have gone with it. It was
red first (`sh: line 0: /tmp/echo: Permission denied`).

Suite: grinder 351 → **352**, zero failures.

## 2026-08-28 — verdict-store integrity, verdict provenance, and per-attempt boot logs

Three things, in the order they had to happen.

**A data-loss path found while planning, fixed before anything could reach it.** `JsonVerdictStore` built a
bare `jacksonObjectMapper()`, so `FAIL_ON_UNKNOWN_PROPERTIES` was on and `readValue<List<GrindVerdict>>` was
all-or-nothing. Adding *any* field to `GrindVerdict` therefore armed this: a newer build writes the field, the
operator rolls back, `load()` throws, `runCatching` logs "starting empty", and the very next `record()`
serialises the whole (empty) map over the file. A 100 000-verdict store for one unknown field name.
`aCorruptFileDegradesToEmpty` stayed green throughout, because it pins *"start empty rather than crash"* and
not *"and then don't destroy it"* — a good illustration of a guard whose teeth point somewhere else. Now:
unknown properties tolerated, rows read individually so one bad verdict costs one verdict, and anything unread
copied to `<name>.unreadable-<epoch>` **before** returning. The forward direction never needed a change —
absent properties take the Kotlin constructor defaults, which is why the live store's 875 rows carry no
`projectId` key and load fine.

**Verdict provenance.** `LoaderVerdict` already carried `declaredClientSide`, `declaredServerSide`, `jarScan`
and `bootedLoader`, and the CLI's `ClientsideReportRenderer` already printed three of them — they simply never
reached `GrindVerdict`, so the grinder's report could show what a verdict *was* but not what it was based on.
Threaded through the single mapping site in `Grinder`. Sideness and jar scan are **nullable, not defaulted to
`UNKNOWN`**: `UNKNOWN` is a real answer a platform gives — CurseForge gives it for every project, because
`CurseForgePlatform.resolve` hardcodes it and never queries CurseForge for a sideness field — while `null`
means the question was never recorded. Rendering both the same way would tell a reader that ~870 legacy rows
had been checked and found not-client-side.

**Per-attempt boot logs.** Only the console of a **CRASHED** boot was kept, so a mod wrongly *cleared* left no
evidence at all, and neither did an error in the checking itself. The server's own `logs/` and
`crash-reports/` were read nowhere in the codebase — the only mention of those names was
`InstallLayerSnapshot` *excluding* them from the install cache.

- The seam is a `bootArtifactSink` invoked **inside `runPrepared`**, per attempt. It has to be: `stageBootPack`
  does `deleteRecursively()` on the attempt directory, so the newest-build re-check and each other-version
  boot destroy the previous attempt's pack and console, and anything read after `verify` returns can only ever
  see the last one. A `refactor:` commit collapsed the three `runPrepared` call sites into one private
  `BootVerifier.boot` first — 139 tests green with no test edited, which is the proof it was
  behaviour-preserving — and `onlyOneCallSiteInvokesRunPrepared` now pins the structure, because a hook added
  at two of three sites would silently lose exactly the re-check evidence a contested crash is argued with.
- `BootArtifacts` (in `-clientside`, so the CLI verb and the daemon cannot disagree about retention) collects
  console + `logs/` + `crash-reports/` as separate entries. Separate because they disagree usefully:
  `logs/latest.log` is log4j's file appender, holding entries stdout never sees and missing the launcher
  output stdout has. Capped by a **seeking** tail read rather than `readText`-then-trim, and everything found
  is named in an `index.txt` whether kept or not — a silently capped set of logs reads as a complete one.
- `CrashLogStore` became `BootLogStore` (pure rename first, machinery carried verbatim: the traversal guard is
  scarred code, and retyping it is how that landmine comes back). Growth is bounded by `pruneExcept` per tuple
  — deterministic naming only replaces the attempts a re-grind writes *again*, so a re-check sampling a
  different loader or Minecraft line would otherwise strand the previous grind's files forever — with
  `SPC_GRINDER_BOOT_LOG_BUDGET_MIB` (default 2048) as the backstop.

**Two defects found in my own work, on review rather than by a test.** First, "what this grind wrote" was an
instance field on `ContainerCandidateVerifier` — but **one** instance serves every `GrindPool` worker, so one
candidate's prune would have deleted logs another had just written. Same cross-candidate class as the
unqualified attempt directory that once wiped a pack mid-boot. Now per-invocation, pinned by
`candidatesPrunedInParallelDoNotDeleteEachOthersLogs`. Second, `namesFor` lists the store on every call, and
the table asks per row — 875 rows meant 875 directory listings per page load. `ReportServer` now snapshots one
listing per request and groups it on the owner prefix.

Also worth recording: exit codes from `./gradlew … | grep …` are *grep's*, not Gradle's. Two build results
were misread that way before the pipeline was changed to `tee` a full log.

### 2026-08-29 — feature A verified against a real runtime, and what it measured

A one-shot grind of `modelfix` (Modrinth, all four loaders, 456 s) on Docker 29.7.2 with a freshly built
`spc-grinder-runtime:latest`. What only a real runtime could answer, answered:

- **The NeoForge attempt crashed and left a genuine `crash-reports/crash-…-fml.txt`** — a NeoForge
  `ModLoadingCrashException` — which the engine had never captured before. Kept alongside the console,
  `logs/latest.log` and `logs/debug.log`, five artifacts under
  `Modrinth-modelfix-NeoForge~NeoForge_21.8.54_mc1.21.8~*`, with `index.txt` correctly reporting
  "3 kept, 0 not kept".
- **Retention behaved exactly as designed.** Fabric, Forge and Quilt all SURVIVED and kept *nothing*; so
  did the Fabric 1.20.4 other-version re-check. Only the crashed attempt has artifacts.
- **The run exercised the case the per-attempt sink exists for.** The NeoForge crash was superseded by that
  re-check — which staged into the *crashing loader's* directory, as designed — so the published verdict is
  `NeoForge=MEDIUM(boot:SURVIVED)` while the evidence behind the crash survives on disk. A post-hoc copy
  after `verify()` would have found that directory already overwritten.
- **The console and the server's own log genuinely differ, measured rather than asserted.** The console
  (31,780 bytes) carries launcher output `latest.log` lacks (`Start script generated by ServerPackCreator`,
  `Detected 1.21.8 - Java 21`), and `latest.log` (19,027 bytes) carries **100 lines the console does not**.
  That is the justification in `BootArtifacts`' doc, now with numbers behind it.
- **The report serves it end to end:** `/` renders the row with `<details><summary>5 log(s)</summary>` and
  five `/boot-log?name=` links, all nine columns including the new `Rule` and `Logs`; `/boot-logs` indexes
  them; `/boot-log?name=` returns one by name; `/export.csv` carries the `Rule` column; and `/status`
  reports `bootRules {source: none, ruleCount: 0, errors: []}`, i.e. feature C's observability with no rule
  file present — the default install behaving as it did before rules existed.

### 2026-08-29 — feature B: dependency resolution across both sources

**The `-api` bug first, because it was the same one twice.** `FabricScanner` and `QuiltScanner` both carry
the doc comment *"ids that are the platform rather than a mod"* and both broke it: `fabricloader`,
`quilt_loader` and `quilt_base` are the platform, but `fabric` is **Fabric API** and `quilted_fabric_api` is
**QFAPI** — mods, and the ones a server most often genuinely needs. Reported by Griefed, who was right on
both halves. `ModDependency` gained `versionConstraint` (verbatim and unparsed — the grammars differ per
loader) behind `@JvmOverloads`, because pf4j loads *compiled* plugin jars and binary compatibility, not
merely source, is the contract that binds. Blast radius checked rather than feared: `ModListCompiler`'s
rescue loop now pulls a disabled Fabric API back into a pack, which is correct, and neither id is in the
shipped fallback list, so a stock install generates identically.

**Two safety properties carry the rest of the feature, and both are inversions of the obvious.**

`VersionConstraint` **fails towards accept**. A constraint it cannot parse must never *refuse*, because a
refusal is indistinguishable from the dependency being genuinely unsatisfiable — a grammar gap would
present as a catalog-wide mass-INCONCLUSIVE event rather than as a parser bug. That direction shipped
broken **twice** during development: `numbersOf` maps a digit-less component to `0`, so a bare clause like
`whatever` compared equal to `0.0.0`, and separately a bare `.x` took an empty prefix. Both were invisible
to inspection and both were caught by tests, the second only after a deliberate adversarial sweep —
`VersionConstraintFuzzTest` now runs 30 malformed shapes against 6 real versions and asserts not one
refuses, paired with a guard that readable constraints still bite so the rule cannot decay into "accept
everything".

The **refusal split** is structural, not a flag. `unmapped` never reaches `refuseForMissingDependencies` at
all. A manifest id is a weaker signal than a platform ref — it may name something bundled inside another
jar (`fabric-api-base` ships *inside* Fabric API), provided by the loader, or optional in practice — and
since a refusal is scored INCONCLUSIVE, treating every unresolvable one as fatal would convert a large
share of *working* boots into INCONCLUSIVE. `refuseForMissingDependencies` kept its exact signature and its
three existing pins stayed green untouched, which is the evidence the semantics were reused and not
rewritten.

**Attribution annotates and requeues; it never downgrades** (Griefed's call, reversing an earlier choice).
The candidate did crash a server in the configuration a real pack produces, so downgrading on a string
match trades a false positive for a lost true positive — the expensive direction for a list that decides
what gets stripped from every pack built against it. `attributionNeverChangesTheBootResult` makes that safe
by construction, and the blamed dependency is queued so the question is answered by *grinding it*. The
stand-down guard also had to widen: an exception line and the `at` frames beneath it are one crash, and
judging line-by-line blamed the dependency on the strength of the first line alone.

### 2026-08-29 — the report becomes navigable at catalog scale

`VerdictField` collapses what were four hand-synced lists (HTML headers, HTML cells, CSV header, CSV rows)
into one enum. They had already drifted — the CSV carried seven fields against the table's eight — and
`everyHeaderHasACellBeneathIt` existed because adding a header without its cell still rendered, shifting
every column past the gap onto its neighbour's data. Two characterization guards landed **green** first to
protect the restructure: `everyColumnRendersTheValueItsHeaderNames` (a distinct sentinel per field, since
counting cells cannot catch an off-by-one) and `theCsvAndTheTableAgreeOnTheirDataColumns`.

Selection is a pure unit — `QueryParams` → `VerdictQuery` → `VerdictSelection` → `VerdictPage` — rather
than handler code, which is what makes `/` and `/export.csv` *provably* agree: they share the function
instead of being kept in step by hand.

**Measured against the real 875-row store**, not asserted: 4 pages at size 250, sizes offered
`[100, 250, 500, all]`, filters returning HIGH=39 / Fabric=229 / `q=create`→8 (matching the store's own
profile), **20 filtered+sorted selections in 13 ms**, and page bytes **110,703** at `size=250` against
**369,873** at `size=all`. The cost was never the filtering; it is the HTML, which is what paging fixes.

**Two bugs the tests caught that reading would not have.** `toQueryString` built its parts inside
`buildList`, whose `MutableList` receiver's own `size` **shadows** the property — so it compared the list's
length to the default and emitted that as the value: `size=250` rendered `size=0`, `size=2` rendered
`size=4`, and every shared link would have carried a wrong page size. And my own expectation for the size
ladder was wrong rather than the code: for 1,842 rows a size of 2,000 shows everything on one page, which
is what `all` already is, so sizes at or above the count are omitted as *duplicates* rather than kept as
"the next one up".

Filtering ended up needing **no JavaScript at all** — `<select>`s for the low-cardinality columns
(measured: 4 confidences, 5 loaders, 2 platforms), `<input>`s for the rest, one GET form, submitting *is*
the URL update. The DOM sort is gone: it was lost on every reload and could not be shared, which is the
whole point of putting state in the URL.

### 2026-08-29 — item 9: the grinder's entry point, and what "simplify" actually meant here

`GrinderApplication.main` was 294 lines. The line count was never the interesting part: what mattered was
that **~20 assertions across 8 test files grepped that function's source text**, because `main` boots Docker
and cannot be executed, so a string being present in a file was the only guard available. That shape also
degrades silently — a source scan stops covering anything that moves out of the file it scans, without
failing.

**`GrinderConfiguration`** now reads every knob once, and is *executable*. `KNOBS` is a real list the README
and systemd-unit guards iterate instead of regexing Kotlin, and `from(lookup)` takes its environment as a
parameter, so a test asserts what the daemon *would do* with a value rather than that a literal appears
somewhere. The two documentation guards went from regex-over-source to **zero** source greps, and gained
things they could not previously state at all: that a malformed number falls back to its documented default,
that a blank value reads as unset, that every path defaults beneath the home while staying individually
overridable.

**`GrindLoop`** is the sweep — drain the re-grind lane, take the catalog slice, commit what was reached,
evict, pace — and it **had no test whatsoever** while it lived inside `main`. It takes
`evictUnusedInstalls` and `verdictCount` as functions rather than `LoaderCache` and `VerdictStore`, which is
what keeps its tests free of a Docker-bound installer. Four guards now cover behaviour that was previously
only greppable, including the one that matters most operationally: a stop arriving *during* the drain must
not start the catalog pass, or the daemon spends another boot budget per candidate after being asked to
stop and systemd's `TimeoutStopSec` lands mid-boot.

**Writing those tests taught something worth keeping:** `running` is polled *between steps*, deliberately,
so a counter-based fake flag stops the loop mid-pass and proves nothing. The flag has to be flipped from the
injected `sleeper`, which is where a real stop lands. Two of my first three assertions were wrong for
exactly that reason, and the loop was right.

**Result: 294 → 259 lines, and 20 → 18 source greps** — but the 18 that remain are the *joins* (a configured
value reaching the collaborator it configures; the shutdown hook's ordering), which genuinely cannot be
executed, and they now grep `config.<property>` rather than `env("NAME", "default")`. The values those greps
used to stand in for are asserted for real. Grinder suite 382 → 386.

**Not done, and deliberately:** the composition itself — the ~60 lines wiring `ApiWrapper`, the loader cache,
the verifier and the report server — stays in `main`. Extracting it would move the remaining wiring guards
without making any of them executable, since what they assert is precisely that this composition happens.
That is churn with a migration cost and no gain in coverage.

### 2026-08-29 — the requeue lane and the report, verified against a live daemon

A running grinder on a copy of the real store (876 verdicts at the time of the checks), Docker 29.7.2.

**The report answers real queries.** `875 of 875` unfiltered; `f.confidence=HIGH` → **39 of 875**, matching
the store's own distribution; `f.loader=Fabric&q=create` → 1; `size=100&page=2&sort=name` → **"Page 2 of 9"**
with exactly 100 rows. `/export.csv?f.confidence=HIGH` returned the same 39, a bare `/export.csv` all 876,
and both parse as well-formed CSV (12 fields, no ragged rows). `Content-Disposition` is set, so the browser
downloads rather than renders. Sort links carry the active filter (`/?f.confidence=HIGH&sort=name`) and the
CSV button carries the query plus `size=all`.

**Two counting traps worth naming**, because both looked like defects and neither was: `wc -l` under-counts
a CSV whose last line has no trailing newline, and the row total *moved during the run* — the daemon was
grinding, so 875 became 876. `/status` agreed with the CSV at every point; the discrepancy was the
measurement, not the export.

**The requeue lane works end to end.** `--requeue https://modrinth.com/mod/jei` from a second process
reported `1 now pending`, `/status` showed `requeued: 1`, and the daemon drained it **forced and ahead of
the crawl** on its next pass — `Pass #3: re-grinding 1 requested candidate(s) ahead of the crawl`, then
`Grinding Modrinth/jei (re-grind requested)`, three loaders in 330 s, after which the catalog slice ran as
usual. That exercises `GrindLoop`'s requeue-before-catalog ordering in production, which until this week was
only a source-text grep.

`/status` also confirmed the new rule default is live: `bootRules { undecidedVerdict: "grinder decides" }`.

**What this run did *not* prove:** that a crash naming an injected dependency reaches that lane. Producing
one on demand means finding a mod whose console blames a dependency by name, which no candidate here did.
The path is covered by unit tests either way (`aDependencyBlamedForACrashIsQueuedForItsOwnVerification`
pins `Grinder`'s end, `DependencyAttributionTest` the blame itself), and the lane it feeds is now proven.

---

## 2026-08-29 — the crash-log census, and B35 closed

Griefed sent 21 crash logs from the live grinder with one observation: `autogg-reimagined` failed on
`java.net.UnknownHostException: api.polyfrost.org`, and "appears to require a connection to the internet."
That turned out to be a whole class of false positive, and pulling on it found a second, larger one of our
own making.

**The census.** 609 crash logs are published; 200 were sampled and classified with the real
`BootLogClassifier`. **130 of 200 (65%) carried no client-side evidence at all**, yet every one scored
CRASHED — crash logs are only kept for non-SURVIVED boots, so all 200 had. Of the 21 Griefed sent, exactly
**two** had genuine sideness evidence: `arcane-vortex` (FML `for invalid dist DEDICATED_SERVER`) and
`avm-mod` (`net/minecraft/class_746`, the intermediary name for `LocalPlayer`).

**The network class is the harness, not the mod.** Boots run `--network none` — that isolation is the entire
guarantee — so a mod whose loader phones home at startup is certain to die here and nowhere else. OneConfig
fetches its own stage1 from `api.polyfrost.org`, falls back to a Swing error dialog when it cannot (which is
why the tail of every one of those logs is `Fontconfig error: No writable cache directories`, in a headless
container) and calls `System.exit`. 15 of 200.

**The largest cause was ours.** `Fabric API requires version ...` was the single biggest failure class — 63
of 200, with Fabric API the *requirer* in 55 — and it was caused by `BootCandidateSelector`, not by any mod.
`pickForLoader` treated the Minecraft version as a preference *inside* each loader attempt and fell back to
the newest file for that loader whatever version it targeted. CurseForge tags only recent Fabric API files
as Quilt-compatible, so a Quilt boot matched a `+26.3` file on the loader, took it despite the mismatch, and
never reached the Fabric build carrying the right Minecraft version. Measured: **20 of the 35** boots that
staged a Fabric API staged one for the wrong version — all Quilt, all `+26.3`, into packs as old as 1.19.2.
Quilt Loader refused each pack outright and the *candidate* wore the verdict.

Two fixes, each pinned red in its own commit first:

- The Minecraft version is now fixed **across** both loader attempts, which is what makes the
  Quilt-to-Fabric fallback reachable at all. A dependency matching no file for the pack's version is not
  staged, and the boot is refused as INCONCLUSIVE. The *candidate* keeps its loose fallback: a near-miss
  candidate still tests the candidate, whereas a near-miss dependency only manufactures a conflict to blame
  on it. One existing assertion moved with this (`dependencyFilePrefersExactMinecraftMatchThenFallsBack`
  expected a 1.19.2 dependency in a 1.21 pack) — flagged rather than relabelled, since a changed expected
  value means `fix:`, not `refactor:`.
- `BootLogClassifier` gained `sandboxNetworkMarkers` and widened `dependencyFailureMarkers` (Quilt's
  `requires version [x, y) of z`, mixin `ClassMetadataNotFoundException`, the legacy `MixinTweaker` CNFE).
  Both sit **below** `clientOnlyClassMarker`, which is the whole design — an excuse may never outrank
  decisive client-only evidence. `aClientClassCrashOutranksTheNetworkExcuse` was green before the fix and
  had to stay green through it; that is what pins the ordering.

Re-classifying the real logs: the 21 went **21 CRASHED → 11 CRASHED / 10 INCONCLUSIVE**, the 200-log sample
**200 → 113 / 87**. Both true positives retained; nothing carrying client-side evidence moved.

**Known gap, deliberately unfixed.** `clientOnlyClassMarker` misses Fabric intermediary names. `class_746`
is `LocalPlayer`, but `class_NNNN` is intermediary for *every* class, not only client ones, so a pattern
would trade these false negatives for false positives. It needs a version-specific ID list or nothing.

**B35 closed — and the append-log was not needed.** The backlog asked whether dropping the pretty-printer
would be enough. Measured at 100 k rows: sort 40.6 ms, pretty write 707.5 ms (45.6 MiB), compact write
360.7 ms (38.2 MiB) — a 2× win that still left ~360 ms *per verdict*. The O(n) whole-file rewrite was the
cost. So `record()` now buffers and a daemon flusher persists every `SPC_GRINDER_STORE_FLUSH_SECONDS`
(default 30), with the shutdown hook flushing last, after the workers stop:

| rows | write-through | coalesced |
|------:|--------------:|----------:|
| 1 000 | 20.8 ms | 2.5 µs |
| 10 000 | 94.7 ms | 2.1 µs |
| 100 000 | 1242.5 ms | 2.6 µs |

Coalesced `record()` is **flat** — it no longer scales with the store, which was the defect; the deployed
store held 38,258 verdicts and only grows. Format, pretty-printing and atomic move are untouched; only the
frequency changed. The default stays write-through (`Duration.ZERO`) so coalescing is opted into at the
composition root and no existing caller silently loses durability. A hard kill can lose at most one
interval, re-derived by the re-verify TTL. The append-log alternative — a store-format change with recovery,
compaction and `supersededLegacyKey` dedup semantics, on a file holding 38 k live verdicts — was therefore
never built, to improve on a path that is now 2.6 µs.

Also here: `StoreWriteBenchTest` had been swept into `47ccb99d4` as a scratch file and was seeding a
100 k-row store on every build. It is now gated behind `SPC_GRINDER_BENCH=1`.

### B36 filed and closed the same day — the store was reset instead

Filed after measuring that 174 of the 455 published HIGH verdicts holding a crash log (**38%**) were false
positives under the fixes merged that day, and that HIGH is the only confidence reaching `/as-properties` —
so each one was a working mod being stripped from users' server packs. By loader: Quilt 118, Forge 52,
NeoForge 4, the Quilt concentration being the `pickForLoader` bug. The plan was a targeted
`--requeue` of the 428 published projects once the fixes were deployed.

It never needed doing. Griefed deployed the merge and **reset the store**, so the daemon is re-grinding the
catalog from scratch on the fixed build. Verified against the live service: `/status` now reports the
`bootRules` block (so it is running the merged code), the store went 38 258 → 186 verdicts, and the
confidence spread is 130 LOW / 54 MEDIUM / **2 HIGH** — the 466 HIGH and their 174 false positives are
simply gone, and `/as-properties` is back to essentially the shipped list.

Recorded because the *measurement* keeps its value even though the remedy changed: it is the only
end-to-end evidence of what the pre-fix engine was publishing, and it is the number to compare against once
the fresh sweep has covered comparable ground. `requeue-high-verdicts.txt` was deleted with the entry; it is
regenerable from `/export.csv` at any time, and would now list the wrong set anyway.


### The equivalence proof the plan required, run 2026-08-29

The convention's own recipe — the base branch's *unmodified* test tree against the branch's production code
— because green tests are HEAD's tests and pass by construction. Base is `57d22b57c`, the develop head
before any of this work.

**Four files could not compile.** Enumerated rather than worked around, per the rule; all three signature
changes are deliberate and each is a data-shape change, not a rename, so none is mechanically adaptable
without rewriting the fixture (which would be working around it):

| Signature change | Files affected | Why |
|---|---|---|
| `CrashLogStore` → `BootLogStore`, and `keep(platform, slug, loader, File)` → `keep(owner, attemptKey, List<Artifact>)` | `CrashLogStoreTest`, `ContainerCandidateVerifierReapTest`, `ReportServerTest` | Feature A keeps *every* artifact of an attempt, not one console file. The `/crash-log(s)` HTTP routes were deliberately kept as aliases. |
| `keepCrashConsoles` → `bootArtifactSink: ((Prepared.Ready, BootOutcome) -> Unit)?` | `ContainerCandidateVerifierReapTest` | Same. The sink is handed the staged pack and its outcome, not a console string. |
| `VerdictReportRenderer.toHtml(List<GrindVerdict>)` → `toHtml(VerdictPage, logLinks)` | `VerdictReportRendererTest` | The renderer takes a page, since the report now filters, sorts and pages. |

**The other 48 base test files compiled and ran: 323 tests, 25 skipped, 16 failed, and zero of the 16 is a
regression.** They are two deliberate changes:

- **14 — `main()` no longer calls `env(...)` itself.** `ContainerLimitsWiringTest` (4),
  `GrindPoolShutdownTest` (3), `SystemdUnitConfigurationTest` (3), `ReadmeConfigurationTest` (2),
  `ReportBindWiringTest` (2). Every message states it: *"main() does not read SPC_GRINDER_CPUS"*, *"no
  env(...) calls found — did the entry point change shape?"*, *"main() no longer counts passes"*. These are
  source-text guards over `main`'s body, and item 9 deliberately moved that body into `GrinderConfiguration`
  and `GrindLoop`. Their HEAD replacements are strictly stronger: they iterate `GrinderConfiguration.KNOBS`
  and drive `GrindLoop` with fakes, i.e. they *execute* what these could only grep.
- **2 — the CSV header grew** from `Name,Project,NamePattern,Confidence,Loader,Detail,Scanned` to the
  twelve-column set (`VerdictCsvExporterTest`). That is the sideness columns plus Rule, Dependencies and
  Platform. Griefed confirmed on 2026-08-28 that nothing consumes `/export.csv` positionally.

**Not run, and stated rather than quietly skipped:** B6's merge gate — the same ~100 candidates ground twice,
`develop` against the branch, with the verdict delta in the commit body. It needs Docker and hours of real
boots, and it has been overtaken: the deployed daemon was reset and is re-grinding the whole catalog on the
fixed build, which is a far larger comparison but has no controlled baseline to diff against. The engine
changes it was meant to catch were instead measured directly against 200 real published crash logs
(21 → 11/10 and 200 → 113/87), which is evidence of the same kind.

### The plan's remaining verification steps, run 2026-08-29

Three of the four had been specified and never executed. Running them found one real defect.

**Docker integration (`GRINDER_DOCKER_IT=1`): 9 tests, 9 passed, 0 skipped.** Real containers against a
real daemon — the CPU cap reaching the kernel with its period, a boot executing from its tmpfs while the
rest of the hardening holds, the container's own hostname resolving with `--network none`, a labelled
orphan reaped, and a long-running container stopped promptly. Host was constrained (Docker VM: 2 CPUs,
1.93 GiB), which is worth knowing when reading the timings below but did not affect the outcomes.

**B0's generation regression check — found a real defect, now fixed.** Three cases: a stock pack (both jars
kept), a clientside list naming Fabric API with a mod depending on `fabric-api` (rescued), and the same with
a mod depending on the historical `fabric` (**failed**). The failure was correct.

Verified against the artefact rather than reasoned about: Fabric API **0.92.11+1.20.1**, fetched from
Modrinth's CDN, declares `"id": "fabric-api"` with `"provides": ["fabric"]` and 53 nested jars; the newest
build, **0.158.3+26.3**, has dropped `provides` entirely. So a mod writing `depends: {"fabric": "*"}` names
an id no jar in the pack calls itself, the rescue compared `"fabric"` to `"fabric-api"`, and a custom
clientside list stripped Fabric API out from under it — the pack that installs and dies on load which the
rescue exists to prevent. B0 had fixed the exclusion sets but the rescue could not use what they now
recorded. Fixed by carrying `ScannedMod.provides` (behind `@JvmOverloads`, preserving the old
`(File, String, Sideness, List)` JVM constructor descriptor) and matching a dependency against a mod's id
**and** its aliases. Quilt's two entry shapes are pinned separately, since reading one silently yields a
plausible empty list rather than an error.

**The Fabric API acceptance check — passed, on a real boot.** The plan asked for a mod declaring Fabric API
*only* in the jar manifest and not in platform metadata. Found by scanning Modrinth: **`moonlight`**, whose
Fabric jar declares `depends: {"fabric": ">=0.116.6+1.21.1"}` while its Modrinth version metadata lists no
required Fabric API. The "previously" half is confirmed from the pre-branch source rather than asserted —
`57d22b57c`'s `FabricScanner` excludes `(fabric|fabricloader|java|minecraft)`, so the dependency was dropped
and nothing was staged. One-shot grind on the current build:

```
FabricScanner: Added dependency fabric for moonlight.
FabricScanner: fabric-api also provides [fabric].
moonlight [Fabric] -> LOW   Fabric 0.19.3 / Minecraft 1.21.1 → SURVIVED (exit 137)
   staged: ['fabric-api-0.116.15+1.21.1.jar']
```

`0.116.15` satisfies the declared `>=0.116.6+1.21.1`, and its Minecraft version matches the pack's 1.21.1 —
the dependency-selection fix. The verdict is a real one rather than an INCONCLUSIVE on
`dependencyFailureMarkers`. Forge and NeoForge staged nothing, correctly: those jars declare no Fabric API.

**Incidentally, a live confirmation of B35's coalesced writes.** The run held three verdicts in memory,
`/status` and `/export.csv` served them, and `verdicts.json` appeared on the flusher's interval rather than
per verdict; a `SIGTERM` then ran the shutdown flush and all three survived the stop.

### B6's merge gate, run 2026-08-30

The last unrun item of the plan. Its rationale was that B changes *what gets booted*, so unit tests cannot
say whether booting got better — only grinding the same candidates on both codebases can.

**Scale, stated rather than implied: 6 candidates, not the plan's ~100.** The Docker VM available was 2 CPUs
/ 1.93 GiB, where one multi-loader candidate takes ~20 minutes; 100 twice was not runnable. The six were
**pre-registered before either run** — four from the crash logs Griefed sent where a fix was predicted to
change the verdict, and two *controls* that had to stay CRASHED or the gate proves nothing. Baseline is
`57d22b57c`; both runs used a fresh home and a shared loader cache.

| candidate / loader | pre-branch | current | change |
|---|---|---|---|
| amblekit / Fabric | HIGH | HIGH | unchanged |
| amblekit / Forge | HIGH | LOW | false positive removed |
| animatica / Fabric | MEDIUM | MEDIUM | unchanged |
| animatica / Quilt | HIGH | MEDIUM | false positive removed |
| arcane-vortex / Forge | HIGH | HIGH | **control held** |
| arcane-vortex / NeoForge | HIGH | HIGH | **control held** |
| astronomical / Quilt | HIGH | LOW | false positive removed |
| autogg-reimagined / Forge | HIGH | MEDIUM | false positive removed |
| avm-mod / Fabric | HIGH | HIGH | **control held** |

**HIGH verdicts 8 → 4. Four false positives removed, three controls held, zero regressions.**

Each change is its intended mechanism, not a coincidence:

- `animatica/Quilt` — *"Required dependency unavailable for Quilt / Minecraft 1.21.6: 306612. Not booting"*.
  That is the strict Minecraft-version rule refusing CurseForge 306612 (Fabric API) instead of staging the
  `+26.3` build into a 1.21.6 pack. It now refuses in **3 s** where it used to spend a whole boot earning a
  verdict the harness had caused.
- `astronomical/Quilt` — **SURVIVED**, having staged
  `qfapi-4.0.0-beta.30_qsl-3.0.0-beta.29_fapi-0.77.0_mc-1.19.2.jar` and `cardinal-components-api-5.0.2.jar`.
  The strongest single result in the gate: not excused, *proven server-safe*, because the pack was finally
  assembled correctly.
- `amblekit/Forge` — INCONCLUSIVE via the widened `dependencyFailureMarkers`.
- `autogg-reimagined/Forge` — INCONCLUSIVE via `sandboxNetworkMarkers`; this is the OneConfig mod whose
  loader reaches `api.polyfrost.org` under `--network none`.
- `amblekit/Fabric` stays HIGH and CRASHED **with Fabric API correctly staged**, and the detail now records
  that the crash names an injected dependency. A retained positive, which is the point of the controls.

The controls holding is what makes the four removals meaningful: the changes are not a blanket softening of
the classifier — decisive client-only evidence (`arcane-vortex`'s FML invalid-dist, `avm-mod`'s
`class_746`) still reaches HIGH untouched.

**Noticed while reading the results, not fixed here:** `amblekit/Fabric`'s `stagedDependencies` lists
`fabric-api-0.100.8+1.20.6.jar` **twice** — the same file resolved through both the platform declaration and
the jar manifest. Cosmetic (the file is written once; the verdict is unaffected) but it reaches the report's
Dependencies column and the CSV, so it is worth a `distinct()`.

## 2026-08-31 — the decisive-evidence gate, and the census that justified it

Griefed sent five boot-log URLs from the deployed grinder and said more grinds were "invalid or otherwise
broken. Again". The "again" was the operative word: a 200-log census (2026-08-29) and B's merge gate
(`HIGH 8 → 4`) had both already happened, and neither was committed, so nothing re-checked the *published
list* against the consoles behind it.

**What the five logs actually were.** Checked regex-by-regex rather than by eye, **four of five reached the
bare exit-code rung** and were scored `CRASHED` — eligible for a clientside `HIGH` — on no sideness evidence:
a mixin `@Inject` that found no target (`create_ltab` on Minecraft 1.20.6), a `@Shadow` field missing from its
target (`debugify` on 1.19.1), Quilt's `Unhandled solver error` (a phrasing sharing *nothing* with Fabric's,
so `dependencyFailureMarkers` never reached it), and `Missing language javafml version [46,)` from a **Forge**
jar staged for a **NeoForge** boot. The fifth, Fabric's `requires any version of …`, was already correct and
is kept as the control.

**Two root causes, both in selection, both systematic.** `pickBootableCandidate` boots the *newest* Minecraft
in a file's declared set and never asks what the jar was built for, while `ModrinthPlatform.filesOf` applies a
version node's `game_versions` to *every* file of it. And one `ModFile` can carry two loaders — a Modrinth
version tagged `[forge, neoforge]` with two primary jars gives both jars both loaders — so a stable sort hands
the NeoForge attempt whichever the platform listed first. **The suite could not see either shape**: every
`pickBootableCandidate` test used a single-element `loaders` set *and* a single-element `minecraftVersions`
set. Both are now characterized, and they pass as written, which is the point — they prove the defect.

**Three things landed.**

1. **`BootDecision`** names which rung decided a boot and marks exactly two as decisive evidence:
   `CLIENT_ONLY_CLASS` (no broken harness can fabricate it) and `OPERATOR_RULE` (a rule reaching `CRASHED`
   said so deliberately; an undecided rule resolves to the ladder or to `INCONCLUSIVE`, never to `CRASHED`).
   `/as-properties` publishes nothing else. A legacy verdict has no recorded decision and so does not
   publish — deliberately emptying the grinder's contribution until a sweep re-grinds, because an empty
   contribution beats a wrong one.
2. **Three marker sets**, all *below* `clientOnlyClassMarker` so a mod reaching a client class *through* a
   mixin still reads `CRASHED`: `mixinApplyFailureMarkers`, `loaderSolverFailureMarkers`,
   `runtimeMismatchMarkers`.
3. **`JarSelfDeclaration`**, consulted in `stageBootPack` before generation, over the new additive
   `ScannedMod.minecraftConstraint` — a value every scanner already parsed and threw away. **It fails toward
   accept**: unreadable jar, absent descriptor, unparseable range, unrecognised loader, a scan that throws —
   all boot. Only a positive, readable contradiction refuses, because a gate refusing on doubt turns a
   descriptor gap into a catalog-wide mass-INCONCLUSIVE event.

**`GrinderAuditIT` is the standing instrument**, gated `GRINDER_AUDIT_IT=1`. First live run, and it earned
its keep twice — once on the store and once on itself.

| | undefensible / total |
|---|---|
| per kept console (my first, wrong version) | 67 / 91 |
| **per verdict (correct)** | **27 / 43** |

A candidate is booted several times and every non-survived attempt keeps its own console, so grading
artifacts counted one verdict repeatedly *and* counted a re-check attempt against a verdict another attempt
decided. Grouped per tuple, a verdict is defensible if any of its consoles carries decisive evidence.

Distribution, one entry per published HIGH, 43 verdicts over 91 consoles, no rule file:
`CLIENT_ONLY_CLASS` 16 · `EXIT_CODE` 12 · `DEPENDENCY_FAILURE` 8 · `MIXIN_APPLY_FAILURE` 4 ·
`RUNTIME_MISMATCH` 1 · `LAUNCH_FAILURE` 1 · `LOADER_BOOTSTRAP_FAILURE` 1.

**The honest cost, stated because the numbers make it concrete.** Among the 27 are `sodium-extra`,
`reeses-sodium-options`, `better-ping-display` and `immersive-ui` — mods that genuinely *are* client-only but
whose crash was a dependency failure or a bare non-zero exit, so the engine holds no *proof*. The gate is
conservative, not precise: it drops real findings alongside the false ones (`debugify`, `charm` and
`tectonic` are in the same list and are **not** client-only). The recovery path is the rule engine — an
operator who has verified a signature writes a rule, and `OPERATOR_RULE` counts as decisive. C and this gate
compose for exactly that reason.

**Three mistakes of mine, all caught by guards that already existed** — recorded because each is easy to
repeat. A Forge fixture asserted `CLIENT` where `sidenessOf` correctly yields `SERVER`, because it declared
two platform entries and `sidenessOf` is SERVER unless *every* signal says CLIENT. A new `-api` test pointed
at `src/test/resources/serverpackcreator.properties`, and SPC wrote this machine's absolute paths into the
committed file — every other test uses the processed `build/resources/test` copy for exactly that reason, and
`TestPropertiesTest.theCommittedTestPropertiesNameNoHost` caught it. And
`everyColumnRendersTheValueItsHeaderNames` caught the new `Decision` column the moment it was added to one
of the two places that must agree.

Also corrected: the module doc claimed the classifier ladder was "eight rungs" (it was eleven, is now
fourteen), and `theGuardOrderIsPinnedAsAWhole`'s own KDoc omitted the rule and sandbox rungs while asserting
both. The count is replaced with an instruction to re-derive it from `classify`, having been wrong twice.

### The same day — rules for the unproven, and a poisoned cache entry

Asked to write rules recovering the client-only mods that lacked proof. Reading the consoles produced one
rule, one deliberate refusal, and a harness defect worth more than either.

**One rule, verified.** `sodium-extra`, `reeses-sodium-options` and `better-block-entities` all die on
`NoClassDefFoundError: org/lwjgl/Version`. LWJGL is the client's windowing and OpenGL binding, which a
dedicated server never ships, so reaching it *is* proof — decisive in a way `clientOnlyClassMarker` misses,
since that matches only `net/minecraft/client`. `better-block-entities` did not reach it itself; Sodium, its
required dependency, did — and the entry still holds, because a mod whose required dependency cannot run on
a server cannot run on one either. `ShippedBootRulesTest` holds the shipped example to those real excerpts
and also pins that no shipped rule can overturn a ready-line or host trouble.

**One rule deliberately not written, with the reason in the file.** `better-ping-display`, `immersive-ui` and
`certain-questing-additions` are published HIGH and all die on a missing **log4j-core**. A rule would have
recovered three entries and been exactly backwards: log4j-core is a logging library the server is supposed
to *have*.

**Which found the real defect.** All three share one attempt tuple, so three *unrelated* mods on it were
checked — `bbrb`, `chisels-bits`, `corgilib` — and every one fails identically. `corgilib` is a library and
`chisels-bits` runs on servers. **The cached loader install for `NeoForge 21.11.45 / Minecraft 1.21.11` is
broken, and all 90 boots against it are worthless**; the ones reaching a non-zero exit were published as
clientside. One poisoned cache entry manufacturing false positives across an entire tuple is precisely what
a bare exit-code verdict cannot distinguish from a mod crashing on its own merits. log4j-core therefore
joined `runtimeMismatchMarkers` — a marker, not a rule, because it is the *absence* of evidence.
**Operator action no code change covers: invalidate that tuple and re-grind it.**

**Measured impact on the live store, 43 HIGH verdicts:**

| | defensible | undefensible |
|---|---|---|
| deployed classifier, no rules | 16 | **27** |
| + the three new markers | 16 | 27 — redistributed (`EXIT_CODE` 12→6, `RUNTIME_MISMATCH` 1→7) |
| + the LWJGL rule | **19** | **24** |

The markers recover nothing by design; they move verdicts to INCONCLUSIVE, which is the correct answer. Only
a verified rule recovers, and it recovered exactly the three that were verified.

## 2026-09-03 — the runtime image was gone, and a thousand mods wore the verdict

Griefed reported thousands of live verdicts reading `Pack post-processing failed: Loader install for
<LOADER> <VERSION> / Minecraft <MC_VERSION> failed recently and is on cooldown, so it was not retried.` The
sentence is an echo, not a cause: `ContainerCandidateVerifier.overlayLoaderInstall` throws it when
`LoaderCache.ensureInstalled` returns `null`, and `BootVerifier.runPrepared` reports a thrown
`packPostProcessor` as INCONCLUSIVE. One broken tuple therefore speaks once per candidate that wants it.

The journal named the cause on the first line the operator pasted:

```
WARN (LoaderCache.kt:166) - Loader install threw for NeoForge 26.2.0.26-beta / Minecraft 26.2:
  Status 404: {"message":"No such image: spc-grinder-runtime:latest"}
```

`spc-grinder-runtime:latest` had been removed from the Docker daemon. Nothing in `install-grinder.sh`
removes it; `docker system prune -a` does, because the image is only in use *during* a boot. Corroborating
state from the host: `find <cache> -name .spc-installed | wc -l` = **0** (nothing servable from cache
either), every `install.log` **zero bytes** (no container ever started), disk 63% used, container DNS fine,
`work/` owned by `1003:1004` = `grinder` — so neither of the two landmines that usually explain a total
install failure applied.

The damage is not lost time. `JsonVerdictStore.record` replaces by identity, `FallbackPropertiesRenderer`
publishes only `HIGH`, and the re-verify TTL is 30 days — so every project ground during the outage lost
whatever it held, including decisive HIGH entries already being served from `/as-properties`.

**Four fixes, each pinned first and committed red.**

1. **A host defect must not be published as verdicts about mods.** `ContainerEngine.hasImage` (default
   `true`, so no fake is affected) + `RuntimeImagePreflight`; `main` refuses and exits 1 immediately after
   building the engine, so `Restart=on-failure` retries every 30s and the unit sits visibly in `failed`.
   This is the loader-cache-poisoning lesson one level up — *an environment defect looks exactly like a
   subject defect unless something distinguishes them* — and the per-tuple cooldown had been disguising it,
   bookkeeping one host-wide failure as one independent failure per tuple.
2. **An exception is named by its type.** The second tuple in the same journal read `Loader install threw
   for NeoForge 21.1.23 / Minecraft 1.21.1: null`, because `${'$'}{it.message}` on a throwable carrying none
   prints exactly that. `LoaderCache.installThrewMessage` names the type and the message where there is one,
   and the throwable now reaches the logger, so the stack trace is in the journal.
3. **"Not retried" is only said about an install that was not retried.** `isInstallOnCooldown` was read
   *after* `ensureInstalled`, which records the cooldown on its way out of a failure, so the candidate that
   paid for the attempt got the skip message too and the failure branch was unreachable in production.
   `ContainerCandidateVerifier.installedBase` asks before, which is the only moment the two differ — and
   during an outage that difference is the diagnosis.
4. **`--requeue-since <instant>`.** `--requeue-before` answers "a defect was found, the past is suspect"; it
   selects the exact *complement* of an outage window, so recovering this incident with it would have queued
   the entire 38k-row store. `RequeueSelection.verifiedSince` is inclusive of the instant, so the two
   selectors partition the store.

Suite: 434 → **446** (29 skipped, unchanged). Not fixed, and deliberate: `LoaderCache` still logs its
"not retrying for 60m" notice only on a tuple's first failure per process (`recentFailures.put(...) == null`),
because the *cause* — the installer's own warning, or the throw above — is logged on every attempt.

---

## 2026-09-03 — a jar's own version range should narrow the pick, not cancel it

Reported from the live grinder: `jei-1.21.1-forge-19.52.0.422.jar` refused with *"declares Minecraft
'[1.21, 1.21.1)', but the pack is 1.21.1"*, while CurseForge lists 1.21.1 among its game versions.

**The reading was right and the mod is wrong — verified before touching anything.** The jar's
`META-INF/mods.toml` really does carry `versionRange="[1.21, 1.21.1)"`, space and all, and JEI's
`gradle.properties` on its 1.21.1 branch pairs `minecraftVersion=1.21.1` with
`minecraftVersionRange=[1.21, 1.21.1)` — the range is built as `[start, thisVersion)` where it should be
`[start, nextVersion)`, so the descriptor genuinely excludes the version the file is named after. Both
halves of our path are correct: `ForgeTomlScanner.getVersionRange` returns the TOML value verbatim, and
`VersionConstraint.mavenRangeHolds` trims its bounds exactly like Maven's own `parseRestriction`. **The
parser is not the defect and must not be "fixed".**

**The defect was ours, one level up: the descriptor check was a post-selection veto rather than a
selection filter.** `pickBootableCandidate` can only see platform metadata, because the jar is not
downloaded until after selection — so it took 1.21.1, `refuseForSelfDeclaration` contradicted it, and
staging gave up while **1.21, tagged by the platform and accepted by the jar, sat untried in the same
list**. That refusal publishes `BootResult.INCONCLUSIVE`, which overwrites a decisive verdict: the same
harm shape as the missing-runtime-image outage, except permanent rather than windowed, and it fires on
every project whose newest tagged version its own descriptor excludes — a common shape, since authors
routinely tick `X` and `X.1` while the toml covers only `X`.

Three commits, and the pin boundary was *checked out and run*, not asserted:

1. `test(clientside): reproduce JEI's refusal …` — fails **behaviourally** at its own commit, reproducing
   the live message against versions derived from the cached manifest (`'[26.1.2, 26.2)' … pack is 26.2`).
   It writes a real jar with a real `META-INF/mods.toml` read by the actual `ForgeTomlScanner`; nothing is
   faked past the network boundary.
2. `test(clientside): pin the version a jar's own range would have us pick` — three pure pins, red with
   `Unresolved reference 'newestVersionSatisfying'`.
3. `fix(clientside): re-select a Minecraft version the jar accepts, don't refuse` — green.

Split into two test commits deliberately: with both pins in one commit the compile error masked the
behavioural red, and that behavioural red is the most valuable artifact in the branch. Re-cut before
anything was pushed. Verified by checking out all three in a scratch worktree: **behavioural red →
compile red → green.**

**Landmine carried into the module file:** `Prepared.Failed.declaredMinecraftConstraint` is set *only* for
the Minecraft disagreement, and the predicate is re-asked rather than inferred from
`JarSelfDeclaration.contradiction` being non-null — that same string also reports a jar carrying the wrong
loader's descriptor, which no other version can fix. Widened, a NeoForge-tagged Forge jar would re-stage
down its entire version list learning nothing each time. The retry calls `stageBootPack`, never
`prepareBootPack`, so a second contradiction surfaces instead of looping.

**Open, and deliberately not assumed:** whether Forge *fatally* enforces that range at runtime. JEI
19.52.0.422 is the canonical 1.21.1 Forge build and is universally used, which is strong circumstantial
evidence it does not — but it was not demonstrated, and the fix is correct either way because 1.21 is a
real boot yielding real evidence. If a boot ever shows Forge is lenient here, the gate is additionally too
strict on the Minecraft axis and should warn rather than refuse. Per the repo's own rule: what only a real
runtime can answer, ask a real runtime.

Suite: clientside 262 → **266**; grinder 446 and app green, both read from `build/test-results` rather
than inferred from `BUILD SUCCESSFUL`.

---

## 2026-09-04 — an optional dependency was a hard requirement, because nothing ever read the word

Reported from the live grinder: `advancement-plaques` refused with *"Required dependency unavailable for
Forge / Minecraft 26.2: prism. Not booting — a mod refused for missing dependencies says nothing about
sideness."* — while Modrinth lists prism as **optional**, with a specific version linked.

**Verified against the artifacts before writing any code.** `AdvancementPlaques-26.2-forge-1.7.2.jar`'s own
`META-INF/mods.toml` declares `iceberg` `mandatory=true`, and both `prism` and `toastcontrol`
`mandatory=false`; Modrinth's API agrees, giving prism (`1OE8wbN0`) `dependency_type: optional` against
iceberg (`5faXoLqX`) `required`.

**The platform half was already right; the manifest half never existed.** `ModrinthPlatform` keeps only
`dependency_type == "required"` and `CurseForgePlatform` only `relationType == 3`. But *neither* `mandatory`
nor `type` appeared anywhere in `-api`'s main source, so `ModDependency` had no field to carry optionality
and `stageableRequirements` had nothing to filter on. Every declared entry was a hard requirement whatever
the author wrote, and an unmet one refuses the boot as `INCONCLUSIVE`.

**Two spellings, one reader.** Forge's `mods.toml` uses `mandatory = true|false`. NeoForge's
`neoforge.mods.toml` dropped that field for `type`, a string defaulting to `"required"` and also taking
`"optional"`, `"incompatible"` and `"discouraged"` — verified against NeoForged's own mod-files
documentation rather than assumed from Forge's shape. `NeoForgeTomlScanner` overrides only the descriptor's
file name, and NeoForge on Minecraft 1.20.2-1.20.4 still ships `mods.toml`, so `ForgeTomlScanner.isOptional`
has to read both. `"incompatible"` counts as not-required deliberately: it means the mod must *not* be
present.

**Absent means required**, which is NeoForge's documented default and the safe direction — a required
dependency read as optional boots a mod without what it needs, fails as a crash and can publish a *wrong*
verdict, whereas the reverse only refuses a boot and learns nothing.

**Optional dependencies are still recorded, only flagged — and that is a decision, not an oversight.** The
instruction was "do not include optional dependencies", and the literal reading (drop them at scan time)
would also drop them from `ModListCompiler`'s dependency rescue, which keeps a mod on the server because
something declares it. That could *remove* mods from users' server packs, against this module's own stated
rule that dropping a mod which does belong on the server breaks the pack while keeping a superfluous one
costs a few megabytes. So the filter lives at the boot-staging consumer, `stageableRequirements`, which
fixes the grinder and leaves generation untouched. Flagged rather than dropped is also what lets a future
consumer choose differently.

Two commits, pin then fix; both pins run before committing and red only for the missing field
(`Unresolved reference 'optional'`, `No parameter with name 'optional' found`).

Suite: api 383 → **387**, clientside 266 → **267**, both read from `build/test-results` after
`--rerun-tasks` with the previous results wiped.

---

## 2026-09-04 — the result-system redesign: four verdicts, and every clientside rule in a file

Griefed: *"the verdict system is unreliable. We should redesign the result-system. Extract all rules which
determine a mod to be clientside to the rules-file so users can always edit them, no hardcoded rules."*

Five stages, Strangler-Fig throughout, suite green at every commit.

**The old model conflated two questions.** `BootResult` (what happened) × `Confidence` (how sure) could not
express the one thing an operator most needed: **whether the grind ran at all**. That is the whole shape of
the missing-runtime-image outage, where a host-wide defect published as one INCONCLUSIVE per candidate and
overwrote decisive verdicts the 30-day TTL would have left alone — and of the JEI and `advancement-plaques`
refusals, one candidate at a time. `Verdict.ERROR` is the verdict whose absence caused all three.

| Stage | What landed |
|---|---|
| 1 | `Verdict { CONFIRMED, CLEAR, ERROR, INCONCLUSIVE }` + pure `VerdictPolicy` |
| 2 | the eleven hardcoded marker groups became `boot-rules.default.json`; the classifier reads its patterns back out |
| 3 | `RuleSource.METADATA` + `MetadataFacts`, so declared sideness is rule-driven too |
| 3b | **correction:** the console decides, the metadata only declares |
| 4 | `verdictOf` replaces `aggregateFor`; publish gate, store, report and CSV move onto the verdict |
| 5 | `Confidence`, `aggregateFor` and the duplicate `BootObservation` deleted |

**Decisions worth keeping.**

- **Only a rule reaches CONFIRMED, and only from a decisive rung.** A flat reading of "matches a rule means
  exclusion-worthy" would have inverted the existing ladder and turned missing dependencies into clientside
  verdicts. The bare exit-code rung — 27 of 43 published HIGHs — can no longer publish anything.
- **File order is the ladder**, and the two inversions are pinned rather than described: an excuse above the
  evidence silently discards true positives; the evidence above the fair-run guards publishes host trouble as
  a mod's fault.
- **The ladder's order stayed in code; only its content moved.** Re-ordering rungs changes judgment, and the
  killed-exit-code check sits *between* rungs, so file order alone cannot express it. Stated rather than
  faked.
- **Metadata renders as one canonical fact line**, because the platform-vs-jar contradiction is a
  *conjunction* and a regex matches one line at a time. Losing it would have made the rules *more* confident
  than the code they replaced — the wrong direction for a redesign premised on the old verdicts being
  unreliable.
- **Stage 3 shipped a short-circuit and Griefed caught it.** Metadata rules could reach CONFIRMED on their
  own, which would have published mods on their own say-so with no boot. The correction is now the design:
  a `RuleSource.METADATA` rule sets `declares` and **may not** set `verdict`, and a guard fails the build if
  one does — because that regression is silent. The target case is a mod claiming **server** whose console
  reaches a client-only class; an honestly-declared client mod is already excludable from its metadata and
  costs nothing to find.
- **CONFIRMED keeps its logs, which was not asked for.** A confirmation publishes a mod to the fallback list;
  the rule id says *which* rule fired, only the console says what it fired on, and a verdict that cannot name
  its own evidence cannot be audited.
- **Old stored rows load as INCONCLUSIVE rather than being deleted or translated.** "Start clean" without
  data loss: the `Confidence` scale has no honest mapping onto four verdicts, so nothing is treated as
  evidence and each row is re-earned by a real boot.

**Self-inflicted, recorded because the class of mistake matters more than the instances.** A regex that
double-applied and passed `verdict` twice; a migration pass that crashed part-way leaving a file half-edited;
new fields inserted mid-constructor, breaking positional call sites; a deletion slice wide enough to take two
neighbouring helpers with it; `ruleId` appended to a file's last brace instead of its enum's. Every one was
caught by the compiler or the suite within a minute, and every one was reverted with `git checkout --` and
redone in a single pass rather than patched on top. A bulk rename across nineteen files is precisely where a
silent half-edit hides, which is why each step ran the suite instead of trusting the substitution.

**The subtlest trap was in a test, not the code.** `VerdictSortRankTest`'s CSV cross-check matched enum names
*anywhere in the line*, and `INCONCLUSIVE` belongs to both the old and the new vocabulary — so it would have
found a stale value and quietly agreed with itself. It now matches the Verdict column's own cell. Where
fixtures used two confidences to prove a store *replaced* rather than duplicated, the distinguishing values
were recovered from `git diff` rather than guessed; a sweep that dropped them would have left those tests
green while proving nothing.

**Left open, deliberately:** `ConsoleRule` (operator file, `BootResult`) and `BootRule` (bundled ladder,
`Verdict`) are two implementations of one idea. Collapsing them means migrating the operator file's
documented `CRASHED|SURVIVED|INCONCLUSIVE` vocabulary, which is a breaking change to an operator-facing
format — deferred rather than done quietly, and recorded in `serverpackcreator-clientside/CLAUDE.md`.

Suites: api 383 (1 skip), clientside 266 → **309**, grinder 446 → **455** (29 skip), app 149,
plugin-example 3 — **1299 total, zero failures**, re-run with `--rerun-tasks` after wiping
`build/test-results`.

## 2026-09-04 — `Filename`: the artifact a verdict sampled, beside the pattern it publishes

**Branch:** `claude-filename-column`

Reported from the live grinder: `iris` returns three rows whose patterns are `iris-` (Fabric),
`iris-neoforge-` (NeoForge) and `iris-` (Quilt). Two name no loader, and the third names one whose
relationship to the row is not stated.

**Not a bug in the deriver.** `suggestedEntry` is the longest common prefix over a project's *whole*
published history, which is exactly right for its purpose: `/as-properties` serves it and the fallback
list matches it with `startsWith`, so it has to cover every build the project ever shipped. Measured
against the live Modrinth API:

| loader   | files | stem             | why |
|----------|-------|------------------|-----|
| Fabric   | 191   | `iris-`          | oldest files are `iris-mc1.16.5-1.0.0.jar`, pre-dating the loader token |
| NeoForge | 42    | `iris-neoforge-` | no such history — every file carries it |
| Quilt    | 143   | `iris-`          | Quilt boots Fabric builds; same eroded prefix |

So the loader token is not missing, it is *correctly* absent: no single prefix covers both naming
conventions, and the broad one is the one that must be published.

**The fix is a second column.** `FilenameStemDeriver.deriveStem` over the sampled file alone keeps
whatever that file is called, because there is no older convention to erode it against. The deriver
needed **no change** — a characterization test proved that before any code moved, and corrected one of
my assumptions in passing (`iris-mc1.16.5-1.0.0.jar` yields `iris-`, not `iris-mc`; `mc` is stripped as
the Minecraft marker it is). Everything after that is plumbing: `ClientsideVerifier`'s existing `sample`
→ `LoaderVerdict.filenamePattern` → `GrindVerdict` → one `VerdictField` entry, which the HTML table and
the CSV both derive their columns from.

**The property worth guarding is that the two never swap.** Publishing the narrow pattern would stop
excluding every build it misses — for `iris`, its entire pre-2022 history — so
`theFilenamePatternIsNotWhatGetsPublished` asserts `/as-properties` still serves the broad stem. Pinned
alongside it: a row with no sampled file renders **blank**, never the historical stem repeated, so the
column cannot imply an artifact was examined when none was.

**Four existing assertions changed, and that is the label working.** Two CSV header literals, the
`ReportServer` header prefix, and the renderer's per-column sentinel list all name the column set, so the
commit is `feat:` rather than `refactor:`. The renderer guard was given its own `SENTINELFILENAME` rather
than a bumped count — counting cells is precisely the check it exists to be stronger than.

**Deliberately not done:** `ClientsideReportRenderer`, the CLI's Markdown report, still shows only
`Suggested entry`. Same information gap, but the ask was the grinder's catalog table, where a reader has
no other context for the row.

Suites from clean (`--rerun-tasks`): clientside **354**, grinder **465** (29 skipped), both green;
every module compiles.

## 2026-09-04 — one refusal, two defects: the dependency's name and the window it was sought in

**Branch:** `claude-dependency-resolution`

Reported: `architectury-api` publishing `ERROR` with *"Required dependency unavailable for Quilt /
Minecraft 1.20.4: **306612**"*. Fetched the live store to check rather than trusting the paste — 68
verdicts, two dependency refusals, both CurseForge, both bare ids:

| mod | loader / MC | ref | actually |
|---|---|---|---|
| `architectury-api` | Quilt / 1.20.4 | `306612` | Fabric API |
| `waystones` | Forge / 1.21.11 | `531761` | Balm |

**Defect 1 — the name, and why the previous fix missed it.** `unsatisfiedLabel` names a resolved project
by `ProjectFiles.slug`, and `DependencyLabelTest` proves it does. But **both** platforms'
`resolveDependency` passed `nativeRef` into the `slug` parameter *positionally*, so the label resolved the
project and read back the ref it started from. The earlier fix therefore only ever helped the branches
that append something — `(unresolved X project)`, `(distribution-locked on X)` — while the plain resolved
case, the common one, still printed the id.

The reusable lesson is the test boundary, not the bug: **a unit test that constructs the value under test
cannot see a producer that constructs it wrongly.** Same shape as the loader step-down, whose pin injected
the very versions it was meant to prove were fetched. `DependencySlugTest` drives the real
`resolveDependency` with canned JSON and asserts the composition — the only arrangement in which a
positional slip in either platform fails a test.

**Defect 2 — the window, which is what actually cost verdicts.** `resolveDependency` reads one page of 50
files. That is still right (a dependency needs *a* usable file, not a history), but it asked
**unfiltered**, and CurseForge answers newest-first across every loader and Minecraft version. Fabric API
has 1000+ files there, so its newest 50 are all current Minecraft and a 1.20.4 boot finds nothing — a
refusal for a file that has existed since December 2023, published as ERROR over whatever the store held.

`/v1/mods/{modId}/files` takes `gameVersion` (parameters verified against
https://docs.curseforge.com/rest-api/: `gameVersion`, `modLoaderType`, `gameVersionTypeId`, `index`,
`pageSize`). **`modLoaderType` is deliberately not sent** — asking for Quilt returns nothing for Fabric
API and re-creates the same refusal one layer down, because `BootCandidateSelector.fallbackLoaders` has to
*see* the Fabric builds to fall back to them. Version narrows the set; loader choice stays in the
selector.

Modrinth accepts the parameter and ignores it: its version endpoint returns a whole version list in one
response, so it has no newest-N window. Its slug, though, costs one extra GET on the dependency path, and
falls back to the ref rather than losing the project.

**Process note, recorded because it went wrong:** the four commits were made directly on `develop`, against
this project's one-branch-per-fix rule. Nothing had been pushed, so they were moved onto
`claude-dependency-resolution` and `develop` was reset to the previous merge — the same shape the rule
would have produced. Cheap here only because it was caught before a push.

Suites from clean (`--rerun-tasks`): clientside **362**, grinder **465** (29 skipped), app green.

## 2026-09-05 — grinder audit: eight defects, and one mechanism that was never wired in

**Branch:** `claude-grinder-audit-fixes`

A read-only pass over all 7,915 lines of `serverpackcreator-grinder/src/main`, then every finding fixed.

**The one that mattered: template provenance was write-only.** `LoaderCache.isInstalled` compares a cached
tuple's recorded start-script digest against the current one, and `TemplateProvenanceTest` proves it does.
`grep -rn "isInstalled" src/main` returns **nothing** — the production path is `ensureInstalled`, which
decides a hit with `markUsed`, which only asks whether the marker file exists. So `TemplateProvenance.digestOf`
ran, the supplier was wired from `GrinderApplication`, the digest was written into every marker, and it was
never read. A template change was served from the layer the old templates produced, indefinitely — the exact
failure the mechanism was built to prevent, and one both this log's module file and that test's own class
comment described as fixed.

The pin had to be an **installer call count**, because a marker assertion passes against the broken code:
only "did it install again?" separates served-from-cache from rebuilt. One of six cases went red, which is
what proved the fixture rather than the guard.

**This is the third correct-unit-no-caller-reaches-it defect in two days** — the dependency slug, the loader
step-down, and now this. The pattern is specific enough to name: *when a mechanism exists to change a
decision, pin the decision, through the call the daemon actually makes.* A unit test that constructs the
value under test cannot see a producer that constructs it wrongly, and a unit test of a predicate cannot see
a caller that never consults it.

**The rest, by what they cost:**

| Finding | Consequence |
|---|---|
| `/status` counters lifetime, documented and rendered per-pass | dashboard shows "Pass 12 (25 candidates)" above "Verified 3,140" |
| `SPC_GRINDER_WORKERS=0` unvalidated | daemon starts healthy, dies on first pass naming an internal parameter; restart loop |
| `SPC_GRINDER_INTERVAL=-1` unvalidated | no error at all — the loop simply stops pausing |
| `close()`'s untimed `Future.get()` | a wedged Docker socket holds the shutdown hook to `TimeoutStopSec`, whose SIGKILL orphans containers |
| 6 dangling KDoc blocks | six declarations undocumented, their prose discarded by the compiler |
| requeue temp file on failed write | one file leaked per failure, in a directory nothing sweeps |
| `store` shadowed in `queueBlamedDependencies` | a `RequeueStore` hiding a `VerdictStore` in the class holding both |
| `JsonVerdictStore.close()` never called | `AutoCloseable` declared and unhonoured; flusher never stopped |

The config fix follows `from`'s documented contract — *never throw, a typo must not stop a service that has
verdicts to serve* — so it **coerces to the default** rather than rejecting, and leaves boundaries that mean
something (port 0, 0 cores, 0 budget) inside the allowed range.

**Process note, recorded because it went wrong twice.** A `git add -u` swept the requeue fix into the `docs:`
commit, putting a behaviour change under a label that denies one. Caught by reading `git log` before merging;
the three affected commits were rebuilt from deterministic transforms and the resulting tree verified
byte-identical to the contaminated one (`git rev-parse HEAD^{tree}`). Cheap only because nothing was pushed —
the same lesson as yesterday's commits-on-develop slip, and the same remedy.

Suites from clean (`--rerun-tasks`): grinder **490** (29 skipped, up from 465), clientside **362**, app **149**.

## 2026-09-05 — clientside audit: a rung that could switch itself off, and docs that had drifted past the code

**Branch:** `claude-clientside-audit-fixes`

A read-only pass over all 5,552 lines of `serverpackcreator-clientside/src/main`, then every finding fixed.
Same method as the grinder audit earlier the same day, and it found the same *class* of defect twice more.

**The one that could have silenced the engine.** `BootLogClassifier` keeps the ladder's *order* in code and
each rung's *pattern* in `boot-rules.default.json`, looked up by id. `bundledPattern` resolved a missing id
to `Regex("(?!)")` — matches nothing — with no log and no guard. `BootRule.regex` is
`runCatching { Regex(pattern) }.getOrNull()`, so an id that *is* present but carries an uncompilable pattern
does the same thing; the compiler found that second path when the fix was written.

Neither direction announces itself:

| what goes | what an operator sees |
|---|---|
| `client-only-class`, `lwjgl-…` or `fml-invalid-dist` | every true positive falls to the exit-code rung, which cannot publish — the engine looks like it found nothing |
| `out-of-memory`, `launch-failure`, … | host trouble stops being excused; a starved box publishes its biggest mods as clientside |

The file ships inside our own jar, so an unresolved id is a packaging fault: it is now recorded, logged at
ERROR, and `BundledRuleIdsResolveTest` fails the build. A bundled file that cannot be read *at all* keeps its
deliberate degradation to "no console rules".

**Six of sixteen rungs had no position in the guard that exists to pin position.** Rungs 9, 10 and 12–15 —
the decisive pair below `client-only-class` and the four excuses below them — were asserted nowhere, so
reordering any of them passed every test. Extended green, because the code was right and only the guard was
missing, and therefore **mutation-verified**: hoisting `mixin-apply-failure` above `client-only-class` now
fails with *"the client-class marker must outrank a mixin that could not apply"*, and did not before.

**A confirmation credited a rule that had declined to decide.** `Classification.firedRule` deliberately
carries both the deciding rule and one that merely annotated, and `verdictOf` read `firedRule ?:
decidedBy?.ruleId`. The verdict was never wrong — CONFIRMED is gated on `BootDecision.decisive` — but the
Rule column pointed an operator at a rule that had stated no verdict, against this module's own standard
that *a verdict which cannot name its own evidence cannot be audited*.

**Documentation that had drifted past the code**, all of it in the safety-critical file:

- `BootDecision.decisive` said "**exactly two** qualify" and there are **four** — it never followed when
  `lwjgl-on-a-dedicated-server` and `fml-invalid-dist` were promoted from examples to shipped defaults, so
  it understated what may publish a clientside entry by half. The module `CLAUDE.md` repeated it.
- `theGuardOrderIsPinnedAsAWhole` said "eight ordered guards" while listing fourteen, omitted two rungs, and
  kept a stray fragment of an older ladder after a closing parenthesis. The real count is **sixteen**; the
  module doc said fourteen. Its own note already recorded having been wrong twice.
- **Seven dangling KDoc blocks**, including three stacked at one point so that `BootResult` and
  `Classification` were both undocumented while their prose sat sixty lines away on `BootDecision`. One of
  them was `loaderDisprovingTheCrash`'s, which carries the landmine about checking *whose* boot a SURVIVED
  belongs to — dokka was dropping it entirely.

**And one piece of dead code in the grinder**, found by following this module's `propagateClientOnlyProof`
outward: `FallbackPropertiesRenderer.decisive()` was the old second publication gate, uncalled since
CONFIRMED became structural. Left in place it would have been restored eventually and would now be *wrong* —
propagation mints CONFIRMED for loaders inheriting another loader's proof, and those rows carry their own
non-decisive `decidedBy`, so re-deriving decisiveness there drops exactly the sodium case.

Suites from clean (`--rerun-tasks`): clientside **368** (up from 362), grinder **490**.

## 2026-09-05 — closing the analysis findings, and sweeping the audit log for anything still open

**Branch:** `claude-audit-followups`

Three findings from `claude-docs/ANALYSIS-AUDIT.md` fixed, and the four candidate open items in
`claude-docs/REFACTOR-AUDIT.md` checked against the code and found already closed.

**All three fixes were green when written**, because none was a broken behaviour — each was a *missing
guard* over behaviour that happened to be right. That makes mutation verification the whole point rather
than a flourish: a guard added green and never mutated is indistinguishable from one that asserts nothing.

| Fix | Mutation applied | Result |
|---|---|---|
| M-1 sentinel mapping | `filenamePattern = verdict.suggestedEntry` | `'SENTINEL_FILENAME' was dropped by the mapping` |
| M-1 sentinel mapping | `declaredClientSide = verdict.declaredServerSide` | `expected: <REQUIRED> but was: <UNSUPPORTED>` |
| M-2 arm precedence | swap `pickDependencyFile` arms 2 and 3 | `expected: <lib-0.9.0.jar> but was: <lib-1.5.0.jar>` |

**M-1 is the one worth remembering.** `Grinder.grind` assigns eighteen fields by hand from `LoaderVerdict`
to `GrindVerdict`, and five were asserted end to end. Every report, CSV, query and filter test builds its
`GrindVerdict` through a fixture, so the producer was untested by construction — the same boundary as the
dependency-label bug, where both platforms fed a correct labeller the wrong `slug`. The unasserted fields
were the load-bearing ones: `verdict` gates publication, `declared`/`firedRule`/`decidedBy` make an
exclusion auditable. Distinct sentinels are the mechanism, since equal values cannot detect a swap.

**L-1 needed care rather than a delete.** `FilenameStemDeriver.deriveStems` had no caller, but its KDoc
carried the `sodium-fabric-` versus `embeddium-` example that two other files cite as authoritative — the
explanation lived on the one function nothing ran. It moved onto `deriveStem`, with the consequence now
stated: that divergence is *why* `loaderDisprovingTheCrash` compares entries rather than loaders. Second
instance today of dead surface reading as load-bearing because a comment vouches for it.

**The audit sweep found nothing open.** OBS-1's QSL rule, iteration 38's `!!`, iteration 39's two snapshot
accessors and iteration 40's wiring guard are all in the code; the table in `ANALYSIS-AUDIT.md` records
where each was verified so they are not re-litigated. Iteration 34's MED-1/MED-2 stay as recorded history —
that commit was re-cut later, and the audit entry is the remedy the conventions prescribe for a shape found
after the fact.

Status-table counts refreshed from `build/test-results`: api **387 → 405**, clientside **368 → 369**,
grinder **490 → 495**. The api number had been stale for some time; it is re-derived, not incremented.


## 2026-09-06 — three field reports from the live grinder

Reported by Griefed from the deployed daemon, each pinned red before its fix and each recorded in
`serverpackcreator-clientside/CLAUDE.md` in full; the short version, so this log is not silent about a day's
work:

- **NeoForge runs Forge builds on Minecraft 1.20.1, and nowhere else** (`LoaderCompatibility`). NeoForge
  20.1.x is a fork of Forge 47 that kept the `net.minecraftforge` packages and `META-INF/mods.toml`, so on
  that one version a Forge jar and a NeoForge jar are the same file. `CurseForge/mantle` had published an
  ERROR row refusing a file CurseForge ticks for both loaders, minutes after that same file reached a
  ready-line under Forge. The fact now has one home instead of two divergent `Quilt to Fabric` maps.
- **A Sinytra Connector placeholder is scanned as the Fabric mod it wraps.** `continuity`'s Forge row read
  `jarScan=SERVER_OR_BOTH` against a platform declaring `client_side=REQUIRED` — a contradiction manufactured
  entirely by scanning a stub `mods.toml` whose only job is to get the file past Forge's discovery. A false
  contradiction is expensive, not merely wrong: it is what arms the other-version crash re-check, up to three
  boot budgets per candidate.
- **A pack whose own jars contradict each other backtracks instead of booting** (`DependencyBacktrack`), from
  `Modrinth/zoomify` on Quilt / 1.20.5.

## 2026-09-07/08 — the false-conflict storm the backtrack caused, and the three defects behind it

**The backtrack shipped on the 6th and the daemon spent the 7th demoting almost everything.** Griefed
reported three mods with "unresolved dependencies"; the store held **47** `ERROR` rows saying
*"Required dependency unavailable"*, and the CurseForge API returns every one of those files on request,
correctly loader-tagged (`misc/cf-dependency-probe.sh`, whose header carries the measurements).

Three defects, each pinned red in its own commit first:

1. **A CurseForge `ModFile.version` is the author-typed `displayName`**, and `numbersOf` maps a digit-less
   component to `0` — `Balm 26.2.0.7` reads as `[0, 2, 0, 7]`, `balm-fabric-26.2-26.2.0.7.jar` as `[0]`. Nearly
   every CurseForge dependency therefore looked older than its declared range, so the backtrack demoted it,
   re-staged, saw the same thing and walked the project's file list to the end. Measured on the daemon: **1014**
   `re-staging … without it` lines and **146** `publishes no … file for Minecraft` lines in one day against **4**
   genuine staging failures. `readableVersion` now gates the version side of `satisfies`, which the class doc had
   promised since it was written and only ever applied to the constraint side.
2. **A staging refusal named no evidence.** Five ways a dependency goes unmet, three of them printing the bare
   slug: diagnosing the 47 rows needed a CurseForge API probe *and* a log grep on the daemon host purely to learn
   which of them it was. `UnmetReason` now travels beside the name — beside, not inside, so the dedupe that keeps
   one mod one entry when both the platform and the manifest route miss it survives the two routes failing
   differently.
3. **A jar-in-jar library was invisible to the coherence check**, so `createaddition` booted a pack in which
   `create` demanded `ponder [1.0.82,)` against the `1.0.64` nested in another jar, and the *candidate* wore the
   INCONCLUSIVE. `BundledJars.versionsIn` + `BootVerifier.nestedVersions` close it, with bundled copies ranked
   below top-level jars and ambiguity contributing nothing.

**Audited and analysed the same day** (`claude-docs/REFACTOR-AUDIT.md`, `claude-docs/ANALYSIS-AUDIT.md`), which
found one more instance of defect 1 one door along — a component above `Int.MAX_VALUE` parses to `null` and was
read as `0`, so `readableVersion` now asks `toIntOrNull() != null` rather than "all digits" — plus the coverage
gaps around the new nested-version rules, all since closed. The audit's own methodology produced the other
lesson worth keeping: verifying per-commit red/green in a *reused* worktree build directory reports
`No tests found` for a class that is present, and would have manufactured two false findings.

Suites: clientside **410 → 438** across 2026-09-07/08, grinder **503** (29 skipped), app **149**, all green,
every figure re-derived from `build/test-results` rather than incremented. The 410 is what the tree carried at
`300a4aae6`; the 2026-09-06 batch reported 395 at `2329996a5` and grew from there, so the two spans are stated
separately rather than chained into one number nobody measured.

---

## 2026-09-09 — the grinder's `ERROR` bucket, read

Griefed asked for two things: a `LOCKED` verdict for distribution-locked files, "technically not an error on
our side, but a limitation by CurseForge", and a look at the public grinder's remaining dependency-related
`ERROR` rows. Both turned out to be the same finding from two directions — a bucket named after a
*consequence* accumulates everything with that consequence, whatever caused it — so the pass ended with two
new verdicts and three dependency-resolution fixes.

### What the 53 `ERROR` rows on `grinder.serverpackcreator.de` actually were

Read straight off `/verdicts.json?f.verdict=ERROR`, and every diagnosis below re-checked against the live
Modrinth API the same day:

| Cause | Rows |
|---|---|
| The mod's own file distribution-locked (`corail-tombstone`, `entityculling`, `not-enough-animations`, `skin-layers-3d`, `structory`) | 15 |
| A required dependency distribution-locked (`better-combat-by-daedelus`) | 2 |
| A dependency already staged in the pack, refused anyway | 10 |
| A dependency one *patch release* away | 6 |
| One mod id served by two projects, only the first remembered | 1 |
| A jar carrying only another loader's descriptor | 4 |
| Genuinely upstream-absent after all three fixes | ~14 |
| The platform and the jar disagreeing about server support (a note, not a cause) | 5 |

### The three dependency defects

1. **A dependency already in the pack could refuse its own boot.** `stageableRequirements` dropped a
   requirement that was optional, bundled, environment-provided or already resolved *by ref* — and the ref
   dedupe is the wrong question, because one project is reachable under the ref its platform page links and
   under whatever `LearnedModIds`/`KnownModIds` maps the manifest id to. Where those differ the id was
   resolved a second time against a **different project**, and that project's empty file list refused the
   boot. `createaddition` requires Modrinth project `LNytGWDc`, which publishes **17 Forge 1.20.1 and 11
   NeoForge 1.21.1 files**; it was staged, and the verdict still read *"Required dependency unavailable …
   create (nothing published for this loader and Minecraft version)"*. A `provided` set of every staged jar's
   own identity closes it — which is also why the descriptor is now read **once** per staged jar for all
   three of its readers instead of twice.
2. **One mod id is served by several projects and the map remembered one.** Forks and unofficial ports keep
   the original's mod id (Create ↔ Create Fabric, Farmer's Delight ↔ its Fabric port, Sophisticated Core ↔
   its Fabric port), so whichever was ground first owned the id for every loader afterwards — with an
   `Alias`'s right to refuse a boot. `LearnedModIds` now keeps every prover in order and
   `planManifestDependency` tries each; a refusal needs all of them to fail. Keeping every prover is what
   makes the lookup loader-aware **without** a loader dimension, since `pickDependencyFile` already filters
   by loader and Minecraft version.
3. **A dependency is now staged from a neighbouring patch release** (Griefed's call, and a deliberate
   relaxation of a rule this log previously recorded as correct). Refusing every version but the exact one is
   right across a version-*line* and too strict inside one. Six rows had their dependency one patch away:
   `playeranimator` for Forge 1.20.2 against published 1.20/1.20.1, `yacl` and `forgified-fabric-api` for
   Forge 1.20.6, `cobblemon` for Fabric 1.21.11 against published 1.21.1, and QSL for Quilt 1.21.1 and
   1.21.11 against published 1.21. Nearest patch first, ties to the newer build, only versions the project
   really publishes, never across a line.

### The verdicts

`Verdict.ERROR` documented itself as *"an operator's problem, never evidence about the mod"* while holding 17
opt-outs and ~18 upstream gaps. `LOCKED` and `UNVERIFIABLE` now carry those, `StagingOutcome.Prevented`
carries a typed `PreventionCause` instead of only a sentence, and `UnmetReason` owns its own cause so a
reason added later cannot reach a refusal without somebody deciding whose problem it is. Ranked
`CONFIRMED, INCONCLUSIVE, ERROR, LOCKED, UNVERIFIABLE, CLEAR`, with `everyVerdictHasARank` failing the build
if a verdict is added without one. `/as-properties` still gates on `CONFIRMED` alone.

### Three things worth carrying forward

- **An added enum constant cannot be pinned red**, only fail to compile, and the same is true of a new
  parameter. Three of this batch's four red commits therefore pin the *behaviour* through a path that
  compiles against the old code — `assertNotEquals(Verdict.ERROR, …)` for the verdict split, and real
  staging for the two dependency fixes — with the unit-level guards landing beside the signatures they
  exercise. Stated in each commit message rather than left for an auditor to notice.
- **A guard must not construct the thing it asserts on.** `PreventedGrindBlameTest` drives the real
  `prepareBootPack` and the real `refuseForMissingDependencies`, because a fixture handed the cause would
  only prove that a `when` branches on its argument. Mutation-verified: forcing either cause site to `HOST`
  fails exactly the three "not our failure" guards and leaves the counterweight green.
- **The published report was enough to find the bug.** `chefs-delight`'s refusal printed the bare id
  `farmersdelight`; the platform route labels with the resolved project's slug (`farmers-delight`) and the
  manifest route refuses only on a confident mapping, which the id table does not give that id — so the
  alias could only have come from the learned map. No daemon access, no logs. The corollary is that the
  precision of a refusal's wording is load-bearing, which is the argument `unsatisfiedLabel` and
  `UnmetReason` were built on.

Suites re-derived from `build/test-results`: clientside **475 → 515**, grinder **509 → 512** (29 skipped),
plugin-grinder **73**, api **412** (1 skipped), app **149** — the last needing a local MongoDB on
`localhost:27017`, without which its Spring context tests time out and take the Gradle worker with them
(confirmed by running it against `mongo:8.0.5` in Docker, where it is green).

### Analysed and audited the same day, and the analysis found the hole the relaxation left

`claude-docs/ANALYSIS-AUDIT.md` and `claude-docs/REFACTOR-AUDIT.md` carry the two reports. The audit's
per-commit verification is worth quoting because it is the property the convention is actually after: each
commit checked out into its **own fresh worktree** — never a reused build directory, which is what reported
`No tests found` for a present class the day before — with the *whole* clientside suite run so a filter
cannot silently match nothing. **10 red, 33 green across nine commits, and not one collateral failure**, so
`git checkout <fix>^` really does show the missing implementation at all four test/fix pairs.

The analysis's headline finding is the one that matters most, and it is a consequence of this batch rather
than a pre-existing defect: **relaxing the exact-Minecraft rule removed the only gate in that dimension.**
`refuseForSelfDeclaration` reads the candidate's descriptor; nothing read a *dependency*'s, because until
now a dependency was never staged for another version and so could not disagree about one.
`outsideThePacksMinecraft` closes it inside `dependencyToDemote` — which already held a `ScannedMod` for
every staged jar with the field on it — and closes the same exposure in the cross-loader and untagged
fallbacks, which predate the patch fallback. Without it a `cobblemon` Fabric 1.21.1 build stages into a
1.21.11 pack, the loader refuses the pack, and the *candidate* wears an INCONCLUSIVE that overwrites a
decisive verdict.

Ten more findings were closed the same day — a `first {}` that threw on an empty fold, three guards that
listed the verdicts they knew about instead of asking `Verdict.entries`, an unpinned thread-safety claim on
a map that had just gained a mutable value type, three new `!!` in a test, and a KDoc link to a type that
does not exist. Both resolution tables name every one.

**Two of them are worth carrying beyond this module.** A guard can assert a rule that was invented for it:
the retention drift-guard's first implementation partitioned on "did a container run?" and went red against
*correct* code, because `ERROR` keeps its logs despite nothing having run — an admin has to diagnose the
host. Reading *why* a red happened is what separates that from a real defect, and it costs one run. And a
guard that enumerates the values it knows about stops covering the vocabulary the moment the vocabulary
grows: `everyVerdictButClearKeepsItsLogs` stayed green while its own *name* became false.

**Equivalence checked against `origin/develop`'s unmodified test tree**, by the recipe in the root
`CLAUDE.md`, and re-run after the audit fixes: **475 pre-existing guards, zero failures** against the
production code both times. **Four** files needed adapting, each an enumerated deliberate change —
`LearnedModIdsTest` and `ManifestDependencyTest` on `mappingFor` → `mappingsFor` (thirteen call sites) plus
`restore`'s value type; `VerdictAggregationTest` on `BootOutcome`'s `stagingPrevented` constructor argument
becoming `prevention`; and, after the audit fix, `UnmetDependencyReasonTest` on
`DROPPED_BY_BACKTRACK`'s sentence, which is the one *expectation* change in the batch and is why that commit
is `fix:` rather than `refactor:`. The first three are argument-only, with every assertion byte-identical.
Nothing else in the base tree noticed, which is the claim worth having: the behaviour that moved is the
behaviour that was meant to.


## 2026-09-10/11 — the `UNVERIFIABLE` bucket, read the same way the `ERROR` one was

`LOCKED` and `UNVERIFIABLE` had been live for a day, and the new bucket immediately held **42 rows** (out of
3931 verdicts: `CONFIRMED` 104, `CLEAR` 3597, `INCONCLUSIVE` 157, `ERROR` 8, `LOCKED` 32). Griefed's
directive was the right one: *these mods do run on a client or a server, other launchers install them fine,
and the grinder must too.* Every row was attributed to a measured cause, against the live APIs and by opening
the actual jars — **27 of 42 were ours**, and the remaining 15 are genuine upstream facts (two of which now
report better).

| Cause | Rows | Fixed |
|---|---|---|
| NeoForge on Minecraft ≤1.20.4 ships `META-INF/mods.toml`; the loader gate was version-blind | **13** | yes |
| Quilt's `unless` clause ignored | **5** (4 fixable) | yes |
| Platform metadata mis-ticks a loader the jar was never built for | **10** | yes |
| Mod id ≠ project slug, or the fork is a separate project | **3** | yes |
| CurseForge patch-neighbour fallback **provably inert** | **2** (+2) | yes |
| Pre-1.13 Forge descriptor (`mcmod.info`) recognised nowhere | **1** | yes |
| A **beta** picked over 16 stable releases | **1** | yes |
| `quilt_base` hard-required with no `unless`; `yacl` has no Forge builds; a dead project | 5 | upstream |

### The eight changes, and what each one is really about

- **One home for "which descriptor evidences loader L on Minecraft V"** (`LoaderDescriptors`, in `-api`).
  `-api` already had `NEOFORGE_TOML_MINIMUM_MINECRAFT = "1.20.5"` and its own `CLAUDE.md` said outright that
  NeoForge on 1.20.2–1.20.4 still ships `mods.toml`; `-clientside`'s `JarSelfDeclaration` held a **second,
  flat, version-blind copy** and refused 13 genuine NeoForge jars on exactly those versions. This is the
  `MetadataScanner`/`ModListCompiler` landmine again, so the fix **removed** the duplicate rather than adding
  a third. `ModScanner.scannerFor` now dispatches through the same object, which as a side effect fixed
  `scannerFor("NeoForge", "26")` throwing — the NeoForge arm compared unguarded where the Forge arm already
  fell back, and `ModListCompiler` does not wrap the call, so it aborted **generation** in the published
  module.
- **Declaring a descriptor is not reading one.** The first cut of the era gate put only `neoforge.mods.toml`
  in NeoForge's declaring set above 1.20.5 and accepted a `neoforge.mods.toml`-only jar for a *Forge* boot at
  1.20.1. NeoForge declares both files at every version for that reason.
- **An existing expectation encoded the bug.** `aForgeJarIsRefusedForANeoForgeBoot` asked at Minecraft
  1.20.4, where a `mods.toml` genuinely is a NeoForge descriptor. It moved to 1.21.1 with a doc stating what
  the gate gives up in 1.20.2–1.20.4 and that `runtimeMismatchMarkers` already scores that INCONCLUSIVE —
  the stop-and-flag signal working as intended, which is why the commit is `fix:`.
- **Quilt's `unless`.** `{"id": "quilt_resource_loader", "unless": "fabric-resource-loader-v0"}` is what
  Quilt Loader honours and what `QuiltScanner` discarded. QSL publishes nothing for 1.21.1+ while
  `fabric-api` 1.21.1 has 36 versions, so four mods that run everywhere refused everywhere.
- **The release channel was read nowhere.** `grep releaseType\|version_type` hit only `-api`'s Mojang
  metadata. `hybrid-aquatic` has **16 stable Forge releases** and 10 `[Sinytra]` betas, and
  newest-Minecraft-first *prefers* a beta systematically: authors publish experimental newer-Minecraft ports
  as betas while the stable line sits on an older version. A preference, never a filter — `faster-random`
  publishes an alpha and zero releases for its loader.
- **The patch-version fallback was inert on CurseForge from the day it shipped.** `resolveDependency` narrows
  its page with `gameVersion=<exact>`, so every file in hand carries the exact version and the neighbour rung
  could never match anything the exact rung did not. All six rows it closed were Modrinth. The widened ask is
  a **second overload** defaulting to the narrow one, so Modrinth — which returns a whole history in one
  response — needs no change and every implementation stays valid.
- **The jar wins over the web form** (Griefed's call). A `neoforge.mods.toml`-only jar ticked Forge is now
  verified under NeoForge. `Prepared.Failed.declaredLoaders` is a **sibling** channel to
  `declaredMinecraftConstraint`, not a widening of it: the landmine says re-selecting a version cannot answer
  a loader mismatch, and a refusal offered the wrong retry would re-stage down its whole version list
  learning nothing. Exactly one retry fires, the loader one first, and it stages into the **requested**
  loader's directory so the borrowed loader's own verdict keeps its pack and console.
- **One mod id, two projects, and neither the fork table nor the platform boundary could say so.** `create`
  publishes `[forge, neoforge]`; the Fabric port is the separate project `create-fabric`, and both declare
  the id `create`. `KnownModIds.alternatives` is tried **after** the primary, because mapping `create` onto
  the fork outright would send every Forge boot to a project with no Forge build. And a dependency the
  candidate's own platform cannot supply is now fetched from the other one — only the *manifest* route can
  cross, because only it knows the mod id.

### Three things worth carrying beyond this pass

- **A test can pass against unfixed code because one fixture value is a prefix of another.**
  `CurseForgeDependencyLineTest` matched `contains("gameVersion=$older")`, and `1.20` is a prefix of
  `1.20.6`: the request for the *newer* version matched the *older* fixture arm and the whole test went green
  before the fix existed. Parsing the value and comparing it exactly is what gave it teeth. The repo's
  existing rule — *run the pin and read why it failed* — catches this only if you also ask why it **passed**.
- **A guard that cannot compile is not a red pin.** Two fixes here needed a new parameter before their guard
  could even be expressed. Where the seam was separable it landed as its own behaviour-preserving
  `refactor:` commit first (`KnownModIds.mappingsFor`), and the pin went red against it for the right reason.
  Where it was not (`alternatePlatforms` on the constructor), the commit says so and quotes the mutation that
  reproduces the red — an honest boundary beats a fake one.
- **A refusal state's *name* is about our confidence, not about the world.** The cross-platform fallback's
  first cut fired only on `Unsatisfied`, and the commonest real case is `Unmapped` — the difference between
  them is how much *this* platform trusts its own mapping, which says nothing about whether the other site
  has the mod. Its guard caught that, which is the argument for driving these through real staging rather
  than asserting on a plan.

**Equivalence checked against `86d3d441b`'s unmodified test tree**, by the recipe in the root `CLAUDE.md`:
**api 412 pre-existing guards, zero failures; clientside 523, two failures** — both
`JarSelfDeclarationTest`'s `mods.toml`-only jar against a NeoForge boot at Minecraft 1.20.2/1.20.4, i.e.
exactly the one deliberate change, and both already restated at 1.21.1 in the HEAD tree. **Three** files
needed adapting, all reference-only: `JarSelfDeclarationTest` on `declaredLoaders` taking the Minecraft
version (passed `1.21.1`, where every one of its five base expectations is unchanged), and
`LearnedIdCollisionTest`/`LearnedModIdsTest` on `mappingsFor`'s `orElse` returning a list. Every assertion
byte-identical.

**The two facts only a real jar could settle were settled by real jars**, per the root `CLAUDE.md`'s
"ask a real runtime" rule, and both are one `unzip -l` rather than a test:

```
architectury-11.1.17-neoforge.jar  (NeoForge, MC 1.20.4)  ->  META-INF/mods.toml
architectury-13.0.11-neoforge.jar  (NeoForge, MC 1.21.1)  ->  META-INF/neoforge.mods.toml
Geophilic v3.6.mod.jar        quilt_loader.depends: quilt_resource_loader unless fabric-resource-loader-v0
Terralith_1.21.x_v2.5.14.jar  quilt_loader.depends: quilt_resource_loader unless fabric-resource-loader-v0
```

The first pair *is* the era boundary the gate now encodes — a NeoForge build below 1.20.5 carries the file
the gate used to read as Forge's, and nothing in either archive distinguishes the two loaders there. The
second pair is the clause `QuiltScanner` discarded, in the shape the scanner reads (an object under
`quilt_loader.depends`, not a bare string).

**Each new gate was mutation-verified, and each failed exactly its own guards:** dropping the bootability
check on the re-selected loader; never setting `declaredLoaders`; forcing `alsoVersions` back to an empty
list; dropping the fork alternatives; never consulting the other platform.

**Two items were deferred with reasons, as B36 and B37** — Sinytra Connector as a boot strategy (the row that
prompted it was a beta whose project ships 16 stable Forge builds, and the repo has already measured the
Connector attempt failing with every dependency staged correctly), and search-then-confirm for a mod id no
registry resolves (three cheaper routes landed first and this pass produced no instance the search would be
needed for).

## Moved out of the root `CLAUDE.md` on 2026-09-11 — the three "Since then" narratives

> These sat in the always-loaded root file, which the file's own header designates *this* file for
> ("Per-sprint **narrative** history → `git log` and `claude-docs/REFACTOR-LOG.md`"). Verbatim, so nothing
> was lost; the lessons that generalise beyond their incident stayed behind as one-liners in the root.

**Since then (2026-09-06): the grinder plugin**, a second pf4j plugin module (`-plugin-grinder`) plus the
`/verdicts.json` feed it reads. Two things worth carrying forward beyond that module's own docs:

- **`ApiPlugins.getAllExtensionsOfPlugin` ignored its `plugin` argument**, so every tab was added once per
  *installed plugin* and every generation extension ran that many times. Invisible for as long as this
  repository shipped exactly one plugin — one times one is one — and visible the first time two were
  installed together, as a tab strip reading `Grinder | Tetris | Grinder | Tetris`. Fixed, pinned by
  `ExtensionScopingTest` (which builds its second plugin by cloning the example jar), recorded in
  `claude-docs/API-BEHAVIOUR-CHANGES.md`. **The general lesson: a defect whose multiplier is the count of
  something the repo only ever has one of cannot be found by testing what the repo ships.**
- **CLOSED 2026-09-08 — the plugin-loading recursion** (was: "the example plugin dies with a
  `StackOverflowError` in `CustomPluginFactory`, pre-existing, GUI unaffected"). Both halves of that
  description turned out to be understated: it is not confined to CLI, and it is unbounded recursion rather
  than one failed instantiation. **Plugin loading now happens after the API it reaches into exists** —
  `ApiPlugins.loadAndStart()` instead of the constructor's `init`, called **last** by `stageThree`, and
  `ApiWrapper.api()` publishes its singleton **before** running setup. Measured on one startup with the
  example plugin installed: **53 ApiWrapper constructions, 268 `example-kotlin` log lines, an
  OutOfMemoryError** — reported by Griefed as the example plugin's output appearing "a gazillion times" —
  against **0 / 7 / 0** after. Detail and the two rows it owes an embedder:
  `serverpackcreator-api/CLAUDE.md` and `claude-docs/API-BEHAVIOUR-CHANGES.md`.
  **The lesson worth carrying:** the api suite had the example plugin installed and stayed green for
  months, because whichever test called `ApiWrapper.api()` first did so before anything copied a jar into
  `tests/plugins`. The defect needs a populated plugins directory at *first* startup — every real run, and
  no test. A fixture that is installed *after* the thing it is meant to exercise has already run is not a
  fixture.

**Since then (2026-09-09): the grinder's `ERROR` bucket, read.** Two things generalise beyond the clientside
module, whose own `CLAUDE.md` carries the detail and the landmines:

- **A bucket that mixes "somebody must fix this" with "nobody can" is not readable, and stops being read.**
  `Verdict.ERROR` documented itself as *"an operator's problem, never evidence about the mod"* and held, out
  of 53 published rows, **17 CurseForge distribution opt-outs and ~18 combinations nothing upstream ever
  published for**. Neither is anybody's problem, and both were sitting in the one column an operator scans
  to find work. `LOCKED` and `UNVERIFIABLE` now carry them, and what remains in `ERROR` is actionable by
  construction. The general form: *a category defined by its consequence ("the grind did not happen") will
  accumulate everything with that consequence, whatever its cause* — so define it by the cause, and make the
  type carry it (`PreventionCause`) rather than a sentence a reader has to parse.
- **A defect whose evidence is a published report can be diagnosed without touching the host.**
  `chefs-delight`'s refusal printed the bare mod id `farmersdelight`; the platform route labels with the
  resolved project's *slug* (`farmers-delight`), and the manifest route refuses only on a confident mapping,
  which the id table does not give that id. Two facts in the code plus one string in the feed located the
  bug in `LearnedModIds` — no shell on the daemon, no log. Worth doing before asking for access: the report
  is evidence, and its wording is part of it. That is also the argument for the wording being precise, which
  is why `unsatisfiedLabel` and `UnmetReason` exist at all.

**Since then (2026-09-10/11): the `UNVERIFIABLE` bucket, read the same way.** 42 rows, 27 of them ours, all
attributed to a measured cause. The module's own `CLAUDE.md` carries the six fixes; three lessons generalise:

- **A test can pass against unfixed code because one fixture value is a prefix of another.** A guard for the
  CurseForge version-line matched `contains("gameVersion=$older")`, and `1.20` is a prefix of `1.20.6` — so
  the request for the *newer* version matched the *older* fixture arm and the whole test went green before
  the fix existed. This file already says *run the pin and read why it failed*; the missing half is **ask why
  it passed**, whenever a fixture's values could contain one another. Parsing the value and comparing it
  exactly is what gave that guard teeth.
- **A guard that cannot compile is not a red pin, and the honest options are both better than a fake
  boundary.** Two fixes here needed a new parameter before their guard could be expressed at all. Where the
  seam was separable it landed first as its own behaviour-preserving `refactor:` commit and the pin went red
  against it; where it was not, the commit says so and quotes the mutation that reproduces the red. Committing
  a test that does not compile proves nothing, and neither does quietly merging pin and fix.
- **Duplicated knowledge drifts in the direction of whichever copy is easier to reach.** `-api` knew that
  NeoForge below Minecraft 1.20.5 ships `META-INF/mods.toml` — it is in the dispatch constant *and* in that
  module's `CLAUDE.md` — while `-clientside` held a flat, version-blind second copy and refused 13 genuine
  NeoForge jars. Third instance of this exact shape after `MetadataScanner`/`ModListCompiler` and the two
  `Quilt to Fabric` maps. **The fix is always to delete the duplicate, never to correct it**, and the
  question to ask of any new lookup table is which existing one already answers it.
