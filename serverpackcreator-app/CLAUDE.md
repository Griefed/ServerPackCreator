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
  **`spring.data.mongodb.uri=jdbc:h2:mem:testdb`**, a Mongo URI holding a JDBC URL. Per the landmine
  below, `ConnectionString` accepts only `mongodb://`/`mongodb+srv://`, so that value is a hard startup
  failure the moment Mongo autoconfiguration runs. It never did, purely because `WebServiceTest` is
  `@SpringBootTest(classes = [WebServiceTest::class])` and so boots a context of exactly one class — the
  same test this file already says to replace rather than extend. **The trap:** the first real
  `@SpringBootTest` anyone writes inherits that URI and fails with a message pointing nowhere near the
  cause. Verified dead before removal: no `@Transactional`, no JPA/JDBC types in main source, and
  neither hibernate, tomcat-jdbc nor h2 on the runtime classpath. The unused `testRuntimeOnly` H2
  dependency went with them.
- **LANDMINE — `spring.data.mongodb.uri` is used verbatim, with no validation and no fallback.**
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
  it — don't reason from it.
- **The docker override chain feeds that property, and it is long.** `WebService.springArguments`
  appends `--spring.config.location=` with eight locations, `overrides.properties` **last** (later
  locations win). The s6 script `init-spc-config/run` composes `SPC_DATABASE_*` into that file; it is
  pinned by `docker/tests/init-spc-config-test.sh`, which runs the real script in the production base
  image. See `claude-docs/DOCKER-MONGO-INVESTIGATION.md`.
- **Already MVC-layered:** controllers delegate to services (`ModPackService`, `ServerPackService`,
  `RunConfigurationService`, `EventService`, the stats services); no controller is bloated;
  scheduling isolated in `web/scheduling`. No restructuring warranted here.

## The web module's mod-lists are embedded, not referenced (2026-08-17)

- `RunConfiguration.startArgs` / `clientMods` / `whitelistedMods` are `MutableList<String>` **embedded
  in the document**. They were `@DBRef` arrays pointing at `StartArgument` / `ClientMod` /
  `WhitelistedMod` — each a `@Document` whose *only* field was its `@MongoId`, so a `ClientMod`
  document was literally `{_id: "OptiFine"}` and the eager join resolved to the string it was already
  keyed by. Three collections and four repositories stored nothing. **Do not reintroduce them.**
- That removed ~550 sequential round-trips per created run-configuration (one `findBy` per entry plus a
  `save` per miss, on the default clientside list) — now two calls total, pinned by
  `RunConfigurationServiceTest.buildingAConfigurationCostsTwoRepositoryCalls`. It also removed an eager
  join from every read reaching a RunConfiguration, which is what made `findAll()` on server packs or
  modpacks fan out across four collections.
- **LANDMINE — `In` in a derived query name means "contains any of", not "equals".** The duplicate
  lookup was `…AndStartArgsInAndClientModsInAndWhitelistedModsIn`, so a configuration could be matched
  and reused because it shared a *single* mod with the one being created. It is now
  `…AndStartArgsAndClientModsAndWhitelistedMods`, an exact array match.
- **The JSON shape is part of this contract, and the SPA is not its only consumer.**
  `RunConfigurationController` returns the entity itself under `/api/v2/runconfigs` — a *versioned* path —
  so the entity's fields are the response body. Changing it means changing all of these in the same commit:
  `serverpackcreator-web-frontend/src/types/api.ts` (declares `string[]`), `RunConfigurationCard.vue` and
  `SubmitModPackForm.vue` (two sites), **and the published description in `serverpackcreator-help`** —
  `Writerside/api-docs.yaml` plus the response samples in `Run-Configs.md`, `Server-Packs.md` and
  `Modpacks.md`, which embed a run-configuration too. An audit caught the last group missed: the spec was
  still `$ref`-ing `StartArgument` / `ClientMod` / `WhitelistedMod` schemas whose classes this very change
  deleted. **Nothing in the build can catch that** — `serverpackcreator-help` is not a Gradle module and
  `springdoc` is commented out in `serverpackcreator-app/build.gradle.kts`, so the spec is a hand-maintained
  snapshot. Treat it as source. (It carries older drift of its own, e.g. `id` typed `integer` where the
  entities use `@MongoId(FieldType.STRING)`; that predates this work.)
- **`web/migration/` exists for the upgrade**, and is the pattern to copy if another shape ever changes:
  `RunConfigurationListMigration` is the per-document rewrite (join-free — a DBRef's `$id` is the value,
  so nothing needs reading, and it works even after the referenced collections are dropped), tested
  without a database; `RunConfigurationListMigrationRunner` applies it on `ApplicationReadyEvent`, not
  during context startup, so an unreachable database delays the migration instead of blocking the boot.
  It is idempotent and element-wise, so an interrupted run is *completed* on the next start rather than
  corrupting a half-rewritten document, and the orphaned collections are dropped only after a fully
  successful pass. Failures are logged and swallowed on purpose.
  Note it uses `MongoTemplate` rather than the repository, necessarily: the mapped type can no longer
  read the old shape, which is the very problem being fixed.
  **Its safety decisions are behind `MigrationStore`, and that is the point.** The ordering (every rewrite
  before any drop), the skip (never drop when nothing was rewritten), and the swallow (an unreachable
  database must not fail the boot) are what can lose data if wrong, and they were untestable while the
  runner talked to `MongoTemplate` directly — an audit caught the runner with **no** test at all while its
  pure transformation was well covered. `RunConfigurationListMigrationRunnerTest` asserts them against a
  recording double, and each was verified to have teeth by deliberately breaking it (dropping first fails
  4 guards; dropping when nothing was rewritten fails 2). Copy this seam for the next migration.
  Note `MongoMigrationStore.findAll` materialises the collection rather than streaming the cursor,
  deliberately: writing while iterating a live cursor can return a moved document twice, which is only
  harmless while every rewrite is idempotent.
  **The retry promise is half true:** the rewrite retries on the next start, the *drop* does not — it runs
  only when something was rewritten, so a pass that rewrote everything then failed to drop leaves those
  collections for good. Harmless, but not self-healing.
  It costs the suite nothing — `WebServiceContextTest` fires the listener against an unreachable Mongo
  and still runs in 0.438 s, because localhost *refuses* rather than black-holes, so server selection
  fails fast instead of waiting out the 30 s default. Do not assume that holds for a remote host.

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

## The config-check timer is on the typing path — keep it cheap (2026-08-17)

`ConfigCheckTimer` is a **500 ms debounce restarted by a document change in *any* field**
(`ConfigEditor.validationChangeListener` → `checkAll()` → `TabbedConfigsTab.timer.restart()`), and it runs its whole validation
pass for **every open tab**. So anything it does, a user pays for each time they pause while typing,
multiplied by their open configs. Two things it used to do per tick, now memoized in
`ConfigEditorViewModel`:

- **`isServerDownloadable`** — an HTTP request to the modloader's maven (~234 ms measured against
  `files.minecraftforge.net`), existing only to drive a warning label. Cached per
  `(minecraftVersion, modloader, modloaderVersion)`. **Successes are cached, failures are not**, and
  the asymmetry is deliberate: a published installer does not vanish, but a `false` may only mean the
  network blinked, and remembering it would leave the editor insisting "server unavailable" until
  restart. Pinned both ways in `ConfigEditorViewModelTest`.
- **`packName`** — `checkManifests` parses the launcher manifest into a Jackson tree. Re-read only
  when the fingerprint (existence/size/mtime) of the six candidates changes. Measured against this
  repo's own CurseForge fixture: **4.70 ms parse vs 0.021 ms fingerprint** on 2,715,835 bytes, 221x.
  The latency saved is modest; the garbage avoided (a tree of a 2.7 MB document per keystroke-pause,
  per tab) is the real gain.
  **The fingerprint must read `ConfigurationHandler.manifestCandidates`**, never its own copy of the
  paths — a drifted list makes the memo miss real edits. `ManifestCandidatesTest` in `-api` guards it.

The timer no longer builds a `PackConfig` per tick either; it only ever read `.name` off a throwaway
one. **Note the concurrency inside is illusory** and always was: the ten `launch { }` blocks sit inside
`runBlocking { }`, whose dispatcher is the single blocked thread's event loop, so they run
sequentially — which is also the only reason the shared `errors` `ArrayList` is safe. Making them
genuinely concurrent would introduce a data race.

## SuggestionProvider runs on every keystroke, on the EDT (2026-08-17)

- The autocomplete set is parsed once and reused until the property changes, keyed on the **raw
  property value** rather than a change-listener — saving suggestions writes it back via
  `storeGuiProperty`, so comparing the string cannot miss an update. The property *read* stays
  per-call on purpose (a map lookup); rebuilding a ~550-entry sorted set was the cost.
- **`allSuggestions()` must keep returning a fresh `TreeSet`.** Every caller mutates it and persists
  the result (`ConfigEditor.saveSuggestions` adds the field value, `InclusionsEditor.saveSuggestions`
  adds and `removeIf`s), so caching the *instance* would corrupt the source and accumulate across
  calls. Cache the parse, copy on the way out. Pinned by `eachCallerGetsItsOwnMutableSet`.
- **LANDMINE — do not "optimise" the prefix filter into `TreeSet.tailSet(prefix)`.** The match is
  case-**in**sensitive while the set's ordering is case-sensitive, so matches are not contiguous:
  `tailSet("op")` skips `OptiFine`. Making the set case-insensitive instead silently deduplicates
  entries differing only in case. A linear `startsWith` over a few hundred parsed strings is
  microseconds.
- `showPopup` uses `revalidate`/`pack`/`repaint`, **not `updateUI()`** — that re-installs the
  look-and-feel delegate and is for a LAF *change*, and it ran per keystroke.

**How the popup was GUI-verified, since `osascript` has no Accessibility permission on this machine
(no synthetic clicks or keystrokes):** a throwaway JUnit "harness" test drove Swing from *inside* the
test JVM — `-app` tests are not headless — showing a real `SuggestionProvider` on a real `JFrame`,
inserting characters into the document on the EDT, and logging each visible `JList`'s row count and
`preferredSize` while `screencapture` took stills. That produced the actual evidence the `updateUI()`
removal needed: the popup **resizes** with its content, `56x85 px at 5 matches → 54x34 px at 2`,
correctly filtered and positioned at the caret. Re-assert focus before each burst — the popup only
shows while the component `isFocusOwner`, and anything stealing focus closes it, which made a first
attempt look like a failure when it was only unfocused. The harness was deleted afterwards; it is a
technique to repeat, not a test to keep.

## ConfigEditor status

Extraction is essentially done: the two genuinely-pure pieces — `hasUnsavedChanges` (15-field
PackConfig dirty-check) and `requiredJavaVersion(minecraftVersion)` — live in
`ConfigEditorViewModel` (which takes `VersionMeta`); the Swing methods are one-line facades. The
remaining ~1,330 lines are legitimate view code (MigLayout wiring, combo-box models, status-icon
updates, event handlers) whose domain logic already lives in the tested API. Don't mechanically
extract thin Swing getters.
