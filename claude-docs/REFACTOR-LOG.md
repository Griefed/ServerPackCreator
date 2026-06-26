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
