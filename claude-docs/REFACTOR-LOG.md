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
