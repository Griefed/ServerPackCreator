# serverpackcreator-app — module context

> Four applications in one module under `de.griefed.serverpackcreator.app`: `cli`, `gui` (Swing),
> `web` (Spring Boot 4 / Spring 7 backend serving the frontend), `updater`. Entry points
> `ServerPackCreator.kt` + `Mode.kt` decide which runs; args parsed in `CommandlineParser.kt`.

## Clientside-mod verification verbs (CLI wiring; engine in `serverpackcreator-clientside`)

The verification **engine** moved to its own standalone module —
`serverpackcreator-clientside`, package `de.griefed.serverpackcreator.clientside` (depends only on
`-api`). See **`serverpackcreator-clientside/CLAUDE.md`** for the metadata/boot signals, platform
layer, downloaders and list-editor. This module keeps only the **CLI verbs** that drive it and their
dispatch; it consumes the engine via `api(project(":serverpackcreator-clientside"))` (Playwright now
arrives transitively from there — it is no longer declared in the app build).

Automates the `[Clientside-mod Addition Request]` issues: derive the clientside-list file-name
stem(s), assess server-safety, and — once accepted — open the PR. **All three phases done.**

- **Four CLI verbs**, wired the standard way (a picocli `Command` **and** a `Mode` +
  `CommandlineParser` parse + `ServerPackCreator.kt` dispatch — non-interactive runs go through
  `Mode`, *not* the picocli shell): `-scan <dir> --loader <L> --minecraft <V>` (declared-sideness of
  local jars → JSON), `-clientsidereport <url> [--output <f>]` (metadata-only report),
  `-verifyclientside <url> [--output <f>]` (metadata **+** server-boot), and
  `-clientsideapply --report <json>` (insert accepted entries into the list files — pure editing, no
  staging). The commands in `cli/commands/` are thin wrappers; the first three build the engine with
  `apiWrapper.modScanner`.
  - **Landmine — arg parsing order:** `-clientsideapply` must be checked **before** `CONFIG`/`CGEN` in
    `CommandlineParser`, because those use `.contains()` and `--generation-config` contains `-config`
    (would otherwise be misread as `CONFIG`). The new modes are matched with exact `==`.
- **Workflows** build the jar **from source** (the verbs aren't in any release yet) and post/update a
  sticky comment (marker `<!-- serverpackcreator-clientside-report -->`); the report embeds its data as
  a hidden `<!-- clientside-report-data … -->` JSON block the accept-workflow reads back.
  `clientside-verify.yml` (issues opened/edited) runs the cheap metadata pass; `clientside-boot.yml`
  (the `verify-boot` label or manual dispatch) runs the expensive per-loader boot + uploads boot logs.
  Both are **thin callers** of the reusable `clientside-report-reusable.yml` (`workflow_call`, the only
  difference is `boot: true/false`); the report pipeline (build jar → run verb → post sticky comment)
  lives there once. `clientside-accept.yml` (the `accepted` label) runs `-clientsideapply` and opens a
  PR against `develop` via `peter-evans/create-pull-request` — it stays **standalone** (a reusable
  workflow is a separate job, so it cannot share the build job's working-tree edits with the PR step).
  Each per-goal workflow keeps its own least-privilege `permissions` (verify/boot need only
  `issues:write`; accept needs `contents`/`pull-requests:write`), which is why they are not collapsed
  into one file. All need the `CURSEFORGE_API_KEY` repo secret for CurseForge links.

## Web backend

- **Persistence is MongoDB** (`spring-boot-starter-data-mongodb`), **not JPA.** Full-context tests
  would need a live Mongo instance — don't assume JPA anywhere.
- **LANDMINE — `spring.data.mongodb.uri` is used verbatim, with no validation and no fallback.**
  Measured with `javap` against the pinned `spring-boot-mongodb-4.0.2` and `mongodb-driver-core-5.6.2`
  (no sources jar is published for the autoconfigure module):
  `PropertiesMongoConnectionDetails.getConnectionString()` is
  `if (properties.getUri() != null) return new ConnectionString(properties.getUri())`, and
  `ConnectionString` accepts **only** `mongodb://` or `mongodb+srv://`. Consequences: a malformed URI
  is a hard startup failure, never a degraded connection; the `host`/`port`/`username`/`password`
  properties are **silently ignored** whenever a `uri` is set (the `"localhost"` default lives only in
  that unreachable else-branch, as does `MongoProperties.DEFAULT_URI = "mongodb://localhost/test"`); and
  a log line naming `localhost:27017` therefore means the property was **absent everywhere**, not
  wrong. `determineUri()` exists and returns `uri ?: DEFAULT_URI`, but the connection path never calls
  it — don't reason from it.
- **The docker override chain feeds that property, and it is long.** `WebService.springArguments`
  appends `--spring.config.location=` with eight locations, `overrides.properties` **last** (later
  locations win). The s6 script `init-spc-config/run` composes `SPC_DATABASE_*` into that file; it is
  pinned by `docker/tests/init-spc-config-test.sh`, which runs the real script in the production base
  image. See `claude-docs/DOCKER-MONGO-INVESTIGATION.md`.
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
- `WebServiceArgumentsTest` covers `WebService.springArguments` — pure argument composition, no context.
  It exists because `start()` boots Spring, so the composition had to be extracted to be assertable;
  the old context-only `WebServiceTest` is still the one CLAUDE.md says to replace rather than extend.
- GUI: view-model unit tests; Swing views stay dumb. CLI/entry-point logic pinned by
  `CommandlineParserTest` (headless-independent branches only) and `MigrationManagerTest`
  (mockk-mocked `ApiProperties`, version ranges chosen to never hit a real migration method, plus a
  pin on `LAMBDA_SUFFIX` — the regex stripping the compiler's `$0lambda$1` off a migration method's
  name, which both discovery and version parsing run every declared name through).
- **`VersionCheckerTest`** covers the update-check comparison, which had **zero** tests until
  2026-08-15. The class is abstract and `allVersions()` is its only data source, so a canned subclass
  exercises the whole alpha/beta path offline — no repository, no network. **Quirk pinned there, not
  fixed:** `isPreReleaseNewer` compares only the number after the dot and is blind to the channel,
  while `isUpdateAvailable` consults beta before alpha — so `alpha.2` is offered `beta.3` (3 > 2) and
  `alpha.5` is offered *nothing* (3 > 5 and 5 > 5 both fail) even with a newer alpha published.
  Changing that is a product decision nobody has made; the pin exists so it cannot change by accident.

## Landmines & verified quirks (durable)

- **LANDMINE — FlatLaf's `SystemFileChooser` has no per-file `accept` callback; do not write a
  `FileFilter` subclass expecting one.** `SystemFileChooser.FileFilter` declares **only**
  `getDescription()` (verified with `javap` against flatlaf 3.7.1), because the chooser drives a
  *native* OS dialog that cannot call back into Java per file. Its only real filters,
  `FileNameExtensionFilter` and `PatternFilter`, are `final` and purely declarative. A subclass that
  adds `accept(File)` therefore overrides nothing, compiles **without** an `override` modifier — the
  tell — and is never invoked. `WritableDirectoryFilter` was exactly that on four directory choosers
  (home, server packs, tomcat base, tomcat logs) until 2026-08-02, apparently left over from a
  `javax.swing.JFileChooser` migration. **Nothing was actually broken**, because every call site
  validates *after* the dialog returns via `File.testFileWrite()` plus a `settings_directory_error`
  dialog (`GlobalSettings.kt:63,96`, `WebserviceSettings.kt:70,89`) — that is where the rule lives, so
  keep it there. The filter was deleted rather than repaired; a `DIRECTORIES_ONLY` chooser has no
  files to filter anyway, and `getFiltersForDialog` null-checks the field on every read. If a real
  restriction is ever needed *inside* the dialog, the mechanism is `setApproveCallback`, not a filter.
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

## Coroutine ownership (GlobalScope anti-pattern — RESOLVED)

- Every GUI `GlobalScope.launch` (26 sites across 14 files) now launches on a
  `gui.utilities.ComponentCoroutineScope` — a `SupervisorJob` scope, lazily re-created after a
  cancel, with synchronized access (launches may start off the EDT). Components cancel it from
  `removeNotify()`; non-components anchor it to their backing component via an `AncestorListener`
  (`ancestorRemoved`) or to the frame via a `WindowListener` (`windowClosed`).
- **`CoroutineStart.ATOMIC` is independently `@DelicateCoroutinesApi`** (unrelated to GlobalScope) —
  the three ATOMIC sites (`ConfigEditor.loadConfiguration`, `IconPreview`, `ControlPanel.generate`)
  keep a narrow `@OptIn(DelicateCoroutinesApi)` for ATOMIC alone, with the start preserved (a
  started generation/load must not be cancellable before its first suspension).
- **`ControlPanel.generate` is anchored to the always-visible bottom-bar panel**, so a running
  generation is cancelled only on window close, never by a tab-switch. Don't re-anchor it to a
  switchable container.
- The check timers register their `ActionListener` in `init` (not the `Timer` super-constructor,
  where `this` is unavailable) so they can launch on an instance scope.

## ConfigEditor status

Extraction is essentially done: the two genuinely-pure pieces — `hasUnsavedChanges` (15-field
PackConfig dirty-check) and `requiredJavaVersion(minecraftVersion)` — live in
`ConfigEditorViewModel` (which takes `VersionMeta`); the Swing methods are one-line facades. The
remaining ~1,330 lines are legitimate view code (MigLayout wiring, combo-box models, status-icon
updates, event handlers) whose domain logic already lives in the tested API. Don't mechanically
extract thin Swing getters.
