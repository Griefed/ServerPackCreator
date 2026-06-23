# serverpackcreator-app — module context

> Four applications in one module under `de.griefed.serverpackcreator.app`: `cli`, `gui` (Swing),
> `web` (Spring Boot 4 / Spring 7 backend serving the frontend), `updater`. Entry points
> `ServerPackCreator.kt` + `Mode.kt` decide which runs; args parsed in `CommandlineParser.kt`.

## Web backend

- **Persistence is MongoDB** (`spring-boot-starter-data-mongodb`), **not JPA.** Full-context tests
  would need a live Mongo instance — don't assume JPA anywhere.
- **Already MVC-layered:** controllers delegate to services (`ModPackService`, `ServerPackService`,
  `RunConfigurationService`, `EventService`, the stats services); no controller is bloated;
  scheduling isolated in `web/scheduling`. No restructuring warranted here.

## Testing patterns

- **Standalone MockMvc per controller** (not `@SpringBootTest` full-context) is the established
  pattern: real API beans (cached version manifests make `VersionMeta` work offline), `springmockk`
  available for mocking Mongo repositories. **`VersionsControllerTest` is the template.** All seven
  web controllers are covered (versions, settings, modpack, serverpack, runconfiguration, events,
  stats).
- **Web-entity IDs are `private set`** (Spring Data `PersistenceCreator`); tests assign them via the
  `assignEntityId` reflection helper.
- GUI: view-model unit tests; Swing views stay dumb. CLI/entry-point logic pinned by
  `CommandlineParserTest` (headless-independent branches only) and `MigrationManagerTest`
  (mockk-mocked `ApiProperties`, version ranges chosen to never hit a real migration method).

## Landmines & verified quirks (durable)

- **`@get:JsonProperty` on Boolean settings fields:** Jackson drops the `is` prefix otherwise, so
  the frontend (`setting-store.js`) reads `undefined`. Keep the annotation on Boolean web-entity
  getters.
- Stats download-history routes: server packs are `/downloads/serverpacks/{id}` (must not collide
  with the modpack-history route `/downloads/modpacks/{id}`).
- The old `WebServiceTest` boots an empty context and asserts nothing — replace, don't extend it.
- **Settings dirty-check normalizes on store/read, so the GUI must reload after save.** Several
  `ApiProperties` settings are normalized when written or read — `WebserviceConfig` migrates the
  database URI, the locale is parsed, and `PathsConfig.tomcatBaseDirectory`'s **getter has a
  side-effect** (a value deviating from the home-directory is reset to it on read). The settings
  panels' `hasUnsavedChanges()` compares the raw widget value against the (normalized) getter, so
  saving without reloading leaves the unsaved-changes icon stuck on. `SettingsHandling.save()` must
  call each editor's `loadSettings()` after persisting and before `checkAll()` (as `load()` does) so
  the widgets hold exactly what the dirty-check reads back. Don't remove that reload.

## OPEN ISSUE — flagged, NOT yet fixed (needs runtime verification)

- **`ConfigEditor` uses `GlobalScope.launch` in 4 places** (~lines 702, 1043, 1211, 1333) — a
  structured-concurrency anti-pattern (`@OptIn(DelicateCoroutinesApi)`). Proper fix: a
  component-lifecycle-scoped `CoroutineScope`. Deferred because it changes async execution and can't
  be verified without running the GUI. Raise before doing further `ConfigEditor` work.

## ConfigEditor status

Extraction is essentially done: the two genuinely-pure pieces — `hasUnsavedChanges` (15-field
PackConfig dirty-check) and `requiredJavaVersion(minecraftVersion)` — live in
`ConfigEditorViewModel` (which takes `VersionMeta`); the Swing methods are one-line facades. The
remaining ~1,330 lines are legitimate view code (MigLayout wiring, combo-box models, status-icon
updates, event handlers) whose domain logic already lives in the tested API. Don't mechanically
extract thin Swing getters.
