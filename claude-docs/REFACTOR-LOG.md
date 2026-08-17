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

Triggered by a Qodana report review (job 37558, rev `dc5aed6`: 58 problems, 13 High, no security or
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

Phase 0, timeouts (`9fce124` red, `c124331` fix). Guards written against a loopback `ServerSocket`
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

Phase 1, startup (`04c0e57` extract, `6b19148` red, `76dea65` fix). `ManifestUpdater` extracted from
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
