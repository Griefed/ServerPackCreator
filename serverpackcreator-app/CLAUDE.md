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
- **The JPA/H2 relics are gone (2026-08-15) — do not let them back in.** Both `application.properties`
  carried settings for a stack this app has not used since the move to MongoDB: `spring.jpa.*`,
  `spring.datasource.*`, `spring.jdbc.*`, `spring.transaction.default-timeout`, and — in the test one —
  **`spring.data.mongodb.uri=jdbc:h2:mem:testdb`** (the key's pre-Boot-4 name), a Mongo URI holding a
  JDBC URL. Per the landmine
  below, `ConnectionString` accepts only `mongodb://`/`mongodb+srv://`, so that value is a hard startup
  failure the moment Mongo autoconfiguration runs. It never did, purely because `WebServiceTest` is
  `@SpringBootTest(classes = [WebServiceTest::class])` and so boots a context of exactly one class — the
  same test this file already says to replace rather than extend. **The trap:** the first real
  `@SpringBootTest` anyone writes inherits that URI and fails with a message pointing nowhere near the
  cause. Verified dead before removal: no `@Transactional`, no JPA/JDBC types in main source, and
  neither hibernate, tomcat-jdbc nor h2 on the runtime classpath. The unused `testRuntimeOnly` H2
  dependency went with them.
- **LANDMINE — the database-URI property is `spring.mongodb.uri`, and the old name silently does nothing.**
  Spring Boot **4.0.0 retired `spring.data.mongodb.uri`**: its metadata carries
  `deprecation.level = "error"`, `replacement = "spring.mongodb.uri"`, and the connection properties moved
  from `DataMongoProperties` (`@ConfigurationProperties("spring.data.mongodb")`, which no longer declares a
  `uri` at all) to `MongoProperties` (`@ConfigurationProperties("spring.mongodb")`). A retired key does not
  warn — it is simply not bound, so Boot uses `spring.mongodb.uri`'s own default `mongodb://localhost/test`.
  SPC wrote the old key until 2026-08-18 and therefore ignored every configured host, credential and
  database. Measured with the real bootJar, same URI, only the key differing:

  | key written | resolved hosts | credential |
  |---|---|---|
  | `spring.data.mongodb.uri` | `[localhost:27017]` | `null` |
  | `spring.mongodb.uri` | `[127.0.0.1:27017]` | `MongoCredential{userName='spcuser'…}` |

  The host substitution is the tell: `127.0.0.1` was configured, `localhost` is Boot's literal default.
  `WebserviceConfig` still **reads** `LEGACY_DATABASE_URI_KEY` when the live key is absent and re-writes it
  under the live one, so an installation predating this keeps working; nothing writes the old key any more.
  **The `MigrationManager` step is for the message, not the mechanism.**
  `MigrationMethods.NinePointZeroPointZero` reports the rename; the carry-over itself is the getter's, and
  has to be, because migrations run release→release only and would miss every dev, alpha and beta user.
  What a migration adds is telling the operator — anything *they* own that still writes the old key (their
  own `overrides.properties`, a container environment, a deployment script) is silently ignored by Spring,
  and no code of ours can fix those. It fires only when `hasLegacyDatabaseUri` is true, so an installation
  that never used the old key hears nothing; that negative case is pinned too. The stale legacy line is
  **left in the file on purpose** — deleting it would strand anyone downgrading to a pre-Boot-4
  ServerPackCreator, and Spring ignores it, so the cost is one dead line.
  **Corollary worth knowing: URI query parameters work again.** They never did while the key was dead, which
  is why two attempts to shorten the driver's server-selection timeout in tests looked like they "did not
  work" — the URI was not reaching the client at all. Measured after the fix:
  `…/spc_t?serverSelectionTimeoutMS=250` yields `serverSelectionTimeout='250 ms'` in the client's own
  settings line, against `'30000 ms'` by default. So a `@SpringBootTest` that boots the web context without
  a database can cut ~30 s per boot by putting that parameter in its URI — relevant to any test that
  triggers Mongo access at `ApplicationReadyEvent`.
  Pinned by `DatabaseUriPropertyTest`, whose second guard reads Boot's own
  `spring-configuration-metadata.json` and fails on any key we write that Boot has retired — so the next
  such rename is a build failure, not a silently redirected production database.
- **LANDMINE — the URI is used verbatim, with no validation and no fallback inside Spring.**
  Measured with `javap` against the pinned `spring-boot-mongodb-4.1.0` and `mongodb-driver-core-5.8.0`
  (no sources jar is published for the autoconfigure module). **Re-verified at those versions on
  2026-08-16** — the Boot 4.0.6 -> 4.1.0 bump moved the driver 5.6.2 -> 5.8.0 and the behaviour below
  is byte-for-byte the same shape, so the landmine stands; only the version numbers had gone stale:
  `PropertiesMongoConnectionDetails.getConnectionString()` is
  `if (properties.getUri() != null) return new ConnectionString(properties.getUri())`, and
  `ConnectionString` accepts **only** `mongodb://` or `mongodb+srv://`. Consequences: a malformed URI
  is a hard startup failure, never a degraded connection; the `host`/`port`/`username`/`password`
  properties are **silently ignored** whenever a `uri` is set (the `"localhost"` default lives only in
  that unreachable else-branch, as does `MongoProperties.DEFAULT_URI = "mongodb://localhost/test"`); and
  a log line naming `localhost:27017` therefore means the property was **absent everywhere**, not
  wrong. `determineUri()` exists and returns `uri ?: DEFAULT_URI`, but the connection path never calls
  it — don't reason from it. **That "absent everywhere" case had a cause, and it was the retired key
  above** — which is what `claude-docs/DOCKER-MONGO-INVESTIGATION.md` was unable to explain at the time.
- **The docker override chain feeds that property, and it is long.** `WebService.springArguments`
  appends `--spring.config.location=` with eight locations, `overrides.properties` **last** (later
  locations win). The s6 script `init-spc-config/run` composes `SPC_DATABASE_*` into that file, under `spring.mongodb.uri`; it is
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
- **The controller tests `mockk()` their service, so a green controller test says nothing about the
  service.** `EventServiceTest` and `RunConfigurationServiceTest` (added 2026-08-15) test the services
  directly with mocked repositories, because the look-up-or-store loops in both had been executed by
  **no** test at all. They pin the *outcome* — which entries the built object holds and which reach
  `save` — deliberately **not** the number of repository lookups, which is an implementation detail.
- `WebServiceArgumentsTest` covers `WebService.springArguments` **and** `configLocationArgument` — pure
  composition, no context. Both were extracted from `start()` for the same reason: it hands them
  straight to Spring Boot, so nothing welded to it can be asserted. The config-location tests pin the
  **order** of the eight property-file locations, because later locations win and the two
  `overrides.properties` entries must stay last — that is where a container's `spring.data.mongodb.uri`
  arrives from (see the Mongo landmine below).
- **`WebServiceContextTest` boots the real application context, and needs no database.** The MongoDB
  driver connects lazily, so every bean is constructed and every injection point resolved without a
  server being reachable — the driver logs a connection error in the background and startup continues.
  That covers bean wiring across all controllers, services, repositories and scheduling, which is what
  breaks when someone adds a constructor parameter or misplaces an annotation. Verified it can fail:
  removing `@Service` from `EventService` fails it with `NoSuchBeanDefinitionException`. It replaces
  the old `WebServiceTest`, which was `@SpringBootTest(classes = [WebServiceTest::class])` — a context
  of one class, itself — with an empty test body, and so could not fail for any ServerPackCreator
  reason. **Landmine:** the three schedules are disabled in it via Spring's `CRON_DISABLED` (`-`), not
  left on their midnight crons — `FileCleanupSchedule` deletes modpack files whose IDs are absent from
  the database, and a suite running at 00:30 against an unreachable database should not find out what
  that does. Keep them disabled if you add cases.
- GUI: view-model unit tests; Swing views stay dumb. CLI/entry-point logic pinned by
  `CommandlineParserTest` (headless-independent branches only) and `MigrationManagerTest`
  (mockk-mocked `ApiProperties`, version ranges chosen to never hit a real migration method, plus a
  pin on `LAMBDA_SUFFIX` — the regex stripping the compiler's `$0lambda$1` off a migration method's
  name, which both discovery and version parsing run every declared name through).
- **`VersionCheckerTest`** covers the update-check comparison, which had **zero** tests until
  2026-08-15. The class is abstract and `allVersions()` is its only data source, so a canned subclass
  exercises the whole alpha/beta path offline — no repository, no network. **The fake's
  `latestVersion()` computes the newest rather than taking the list head, deliberately:** the real
  `allVersions()` comes from a repository API whose ordering nothing guarantees, and a fixture that
  is silently newest-first cannot catch code depending on that ordering — which is exactly the bug it
  did hide on the first attempt.
- **Pre-release ordering is channel-first, and both halves were broken until 2026-08-15.**
  `isPreReleaseNewer` compared only the number after the dot, so a beta did not supersede an alpha of
  the same version: `alpha.2` was offered `beta.3` (3 > 2) while `alpha.5` was offered **nothing**
  (3 > 5) with the same two releases published. And `latestBeta`/`latestAlpha` required a candidate to
  be *both* newer-or-equal **and** higher-numbered, so `3.2.0-beta.1` lost to `3.1.0-beta.3` — whether
  that reached a user depended on repository ordering. Now `preReleaseChannel` (alpha < beta <
  release) with the number as tie-break, and `isVersionNewer` (semantic version first) for
  latest-of-channel. **Landmine:** `isNewAlphaAvailable`'s explicit "a beta is never offered an alpha"
  guard was removed as dead once the channel ordering subsumed it — if you ever weaken that ordering,
  that rule disappears with it, and only
  `VersionCheckerTest.aBetaIsNotOfferedAnAlphaOfTheSameVersion` will say so.

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
