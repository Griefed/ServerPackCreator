# serverpackcreator-app — module context

> Four applications in one module under `de.griefed.serverpackcreator.app`: `cli`, `gui` (Swing),
> `web` (Spring Boot 4 / Spring 7 backend serving the frontend), `updater`. Entry points
> `ServerPackCreator.kt` + `Mode.kt` decide which runs; args parsed in `CommandlineParser.kt`.

> **History:** `claude-docs/APP-HISTORY.md` holds the verbatim incident narratives, measurements and audit
> write-ups behind the rules below. Read it when a rule's reason is unclear, or before re-opening a recorded decision.

## Clientside-mod verification verbs (CLI wiring; engine in `serverpackcreator-clientside`)

The verification **engine** moved to its own standalone module —
`serverpackcreator-clientside`, package `de.griefed.serverpackcreator.clientside` (depends only on
`-api`). See **`serverpackcreator-clientside/CLAUDE.md`** for the metadata/boot signals, platform
layer, downloaders and list-editor. This module keeps only the **CLI verbs** that drive it and their
dispatch; it consumes the engine via `api(project(":serverpackcreator-clientside"))`.

Automates the `[Clientside-mod Addition Request]` issues: derive the clientside-list file-name
stem(s), assess server-safety, and — once accepted — open the PR. **All three phases done.**

- **Four CLI verbs**, wired the standard way (a picocli `Command` **and** a `Mode` +
  `CommandlineParser` parse + `ServerPackCreator.kt` dispatch — non-interactive runs go through
  `Mode`, *not* the picocli shell): `-scan <dir> --loader <L> --minecraft <V>` (declared-sideness of
  local jars → JSON), `-clientsidereport <url> [--output <f>]` (metadata-only report),
  `-verifyclientside <url> [--output <f>]` (metadata **+** server-boot, each pack on the JDK its Minecraft
  version requires — taken from `JAVA_HOME_<major>_<arch>` plus the running JVM, a missing one an `ERROR` row
  naming it; see `HostJavaSelection` in `serverpackcreator-clientside/CLAUDE.md`), and
  `-clientsideapply --report <json>` (insert accepted entries into the list files — pure editing, no
  staging). The commands in `cli/commands/` are thin wrappers; the first three build the engine with
  `apiWrapper.modScanner`.
  - **LANDMINE — a flag is a whole argument, never a substring.** `CommandlineParser.isFlag` matches a mode's
    flag as an entire argument (one or two leading dashes). Arguments beside a flag are links and paths, so a
    `contains` match selects the wrong mode (`…/inventory-helper` reads as `-help`, `…/pack-cgen/…` as `-cgen`).
    Never use `contains` for a new mode; `anArgumentThatOnlyContainsAFlagSelectsNothing` pins it.
- **Workflows** build the jar **from source** (the verbs aren't in any release yet) and post/update a
  sticky comment (marker `<!-- serverpackcreator-clientside-report -->`, bot-authored comments only); the
  report embeds its data as a hidden `<!-- clientside-report-data … -->` JSON block the accept-workflow
  reads back. **Every addition request is booted**: `clientside-verify.yml` (issue opened; edited when the
  edit changes the project link; the `verify-boot` label, which `resolve` creates when the repository lacks
  it; manual dispatch) is a thin caller of the reusable
  `clientside-report-reusable.yml`, whose `resolve` job decides whether the event earns a boot and whose
  `report` job runs `-verifyclientside` and posts the report — each boot's console included — plus a link
  to every log. The GitHub-side logic (that decision, the lookup, the evidence block) lives in
  `.github/scripts/clientside-verification.js`, tested by `node --test` in Forgejo's `test.yml`.
  - **LANDMINE — no workflow-level `concurrency` on an `issues:` workflow whose jobs filter by `if:`.** The
    issue form applies a label, so opening an issue fires `opened` *and* `labeled`; a workflow-level group is
    entered before any job's `if:` runs, so the `labeled` run cancels the `opened` boot and then skips itself —
    nothing is ever booted. The group belongs on the `report` job, behind the `resolve` gate.
  - `clientside-accept.yml` (the `accepted` label) runs `-clientsideapply` and opens a PR against `develop`
    via `peter-evans/create-pull-request` — it stays **standalone** (a reusable workflow is a separate job,
    so it cannot share the build job's working-tree edits with the PR step), with its own least-privilege
    `permissions` (verify needs only `issues:write`; accept needs `contents`/`pull-requests:write`).
  - The `report` job installs Temurin 8, 16, 17 and 25 beside the build's JDK 21, because a boot runs on
    the exact major its Minecraft version requires (`HostJavaSelection`). CurseForge links need the
    `CURSEFORGE_API_KEY` repo secret; without it CurseForge is not a registered platform at all.

## Web backend

- **Persistence is MongoDB** (`spring-boot-starter-data-mongodb`), **not JPA.** Full-context tests
  would need a live Mongo instance — don't assume JPA anywhere.
- **Do not reintroduce JPA/H2/JDBC settings** (`spring.jpa.*`, `spring.datasource.*`, `spring.jdbc.*`,
  `spring.transaction.default-timeout`, an H2 dependency) in either `application.properties`. Nothing uses them,
  and a non-`mongodb://` value in the Mongo URI is a hard startup failure that surfaces only in the first real
  `@SpringBootTest`, with a message pointing nowhere near the cause.
- **LANDMINE — the database-URI property is `spring.mongodb.uri`; `spring.data.mongodb.uri` silently does
  nothing.** Spring Boot 4 retired the old key (metadata `deprecation.level = "error"`; the property moved from
  `DataMongoProperties` to `MongoProperties`). A retired key does not warn — it is not bound, and Boot connects
  to its default `mongodb://localhost/test`, ignoring every configured host, credential and database. A log
  naming `localhost:27017` when something else is configured is the tell.
  - `WebserviceConfig` **reads** `LEGACY_DATABASE_URI_KEY` when the live key is absent and re-writes it under the
    live one; nothing writes the old key. The carry-over must stay in the getter, because migrations run
    release→release only and would miss dev, alpha and beta users.
  - `MigrationMethods.NinePointZeroPointZero` exists only to **tell the operator** (their own
    `overrides.properties`, container env or deploy scripts may still write the old key, which no code of ours
    can fix). It fires only when `hasLegacyDatabaseUri` is true; the negative case is pinned too.
  - The stale legacy line is **left in the file on purpose** — deleting it strands anyone downgrading to a
    pre-Boot-4 ServerPackCreator, and Spring ignores it.
  - URI query parameters reach the client (e.g. `?serverSelectionTimeoutMS=250` → `serverSelectionTimeout='250 ms'`),
    so a `@SpringBootTest` booting without a database can bound the ~30 s selection wait that way.
  - Pinned by `DatabaseUriPropertyTest`; its second guard reads Boot's own `spring-configuration-metadata.json`
    and fails on any key we write that Boot has retired, so the next rename is a build failure.
- **LANDMINE — the URI is used verbatim, with no validation and no fallback inside Spring.**
  `PropertiesMongoConnectionDetails.getConnectionString()` is
  `if (properties.getUri() != null) return new ConnectionString(properties.getUri())`, and `ConnectionString`
  accepts **only** `mongodb://` or `mongodb+srv://` (read with `javap` — the autoconfigure module publishes no
  sources jar; re-check after a Boot/driver bump). Consequences: a malformed URI is a hard startup failure, never a
  degraded connection; `host`/`port`/`username`/`password` are **silently ignored** whenever a `uri` is set (the
  `"localhost"` default lives only in the unreachable else-branch, as does `MongoProperties.DEFAULT_URI`); and a
  log naming `localhost:27017` means the property was **absent everywhere** (or written under the retired key),
  not wrong. `determineUri()` exists but the connection path never calls it — don't reason from it.
- **The docker override chain feeds that property, and it is long.** `WebService.springArguments`
  appends `--spring.config.location=` with eight locations, `overrides.properties` **last** (later
  locations win). The s6 script `init-spc-config/run` composes `SPC_DATABASE_*` into that file, under `spring.mongodb.uri`; it is
  pinned by `docker/tests/init-spc-config-test.sh`, which runs the real script in the production base
  image. See `claude-docs/DOCKER-MONGO-INVESTIGATION.md`.
- **Already MVC-layered:** controllers delegate to services (`ModPackService`, `ServerPackService`,
  `RunConfigurationService`, `EventService`, the stats services); no controller is bloated;
  scheduling isolated in `web/scheduling`. No restructuring warranted here.

## Indexes are created after startup, never during it

- **LANDMINE — do not set `spring.data.mongodb.auto-index-creation=true`.** It makes `MongoTemplate`'s bean
  creation run `createIndexes` during context refresh, so an unreachable MongoDB kills startup
  (`MongoTimeoutException`, refresh cancelled). `docker/docker-compose.yml` starts the app alongside its `db`
  service, so losing that race is the normal first boot. The line sits commented out in `application.properties`
  with that reason; leave it so.
- `DeclaredIndexCreator` creates them on `ApplicationReadyEvent` instead — the same trade the migration
  runner makes, and for the same reason. Failures are logged and swallowed: a missing index makes a query
  slower, an exception there would take down an application already serving, and the next start retries
  because creating an existing identical index is a server-side no-op.
- **`@Indexed` stays the single declaration.** Definitions come from the mapping context via Spring Data's
  `MongoPersistentEntityIndexResolver`, so adding an index to an entity needs no change to the creator, and
  nothing is restated in two places. `IndexStore` is the seam, for the same reason `MigrationStore` is.
- **LANDMINE — `src/test/resources/application.properties` shadows the shipped one on the test classpath.**
  So *no* test runs with the shipped web configuration — `WebServiceContextTest` boots over the test copy, and a
  bad shipped setting stays green. When a shipped setting matters, read that file explicitly (enumerate
  `getResources("application.properties")` and discard the test copy — `DeclaredIndexStartupTest` does) *and*
  assert the behaviour in a booted context. A guard that reads a shipped file is not a test that runs with it.
- With no database reachable, each context boot pays a driver server-selection timeout per operation (the
  migration runner's read, the index creation). Keep new `@SpringBootTest` properties **identical** to
  `WebServiceContextTest`'s so Spring's context cache reuses one boot; a divergent set costs a whole extra boot.
- **The test URI carries `?serverSelectionTimeoutMS=250`; do not remove it** — it is what keeps the module suite
  fast (each unbounded boot waits out the 30 s default). The URI these tests bind is
  `src/test/resources/serverpackcreator.properties` via `application.properties`' own `spring.config.import` (and,
  for `DatabaseUriPropertyTest`, its `@DynamicPropertySource`); `processTestResources` does not touch it. Pinned by
  `TestDatabaseTimeoutTest`, which reads the *processed* file under `build/resources/test`.
- **H2 is not an option.** The driver speaks the MongoDB wire protocol and `ConnectionString` accepts only
  `mongodb://`/`mongodb+srv://`; there is no adapter and Spring Data MongoDB has no relational backend.

## A real MongoDB runs in the test JVM

- **`de.flapdoodle.embed.mongo.spring4x` starts a real `mongod` in-process — no Docker.** Docker's own
  MongoDB images refuse to start on Linux kernels 6.19+
  ([SERVER-121912](https://jira.mongodb.org/browse/SERVER-121912)). Use the `spring4x` artifact (the Boot 4 line);
  `spring3x` against Boot 4 is flapdoodle issue #77 and fails on the changed Mongo autoconfiguration.
- **LANDMINE — the dependency is not inert. `EmbeddedMongoAutoConfiguration` activates for EVERY Spring
  context on the test classpath** and throws *"Set the de.flapdoodle.mongodb.embedded.version property"*
  when it is absent. Every `@SpringBootTest` therefore opts **in** (`de.flapdoodle.mongodb.embedded.version=8.0.5`)
  or **out** (`spring.autoconfigure.exclude=de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration`).
- **Two classes must stay opted out.** `DatabaseUriPropertyTest` asserts that the *configured* URI reaches the
  driver, and an embedded server overrides it (mongod binds its own port, the write goes there), which would
  quietly defeat the guard. `DeclaredIndexStartupTest` asserts the context starts without a database.
- **`WebPersistenceIT` is where questions about the database itself belong** — an index that exists, a
  migration that rewrites a document, an upload round-tripping GridFS *and* the filesystem, a row written
  before a schema change that still reads back. Everything else in this module pins persistence against
  Spring Data's own machinery (`MongoMappingContext`, `QueryMapper`, `PartTree`), which is the right
  substitute for "does this declaration mean what I think" and no substitute for "does the database do it".
- **`EmbeddedMongoAvailable` skips rather than fails where `mongod` cannot start**, because CI runs on
  `ubuntu-latest` and the kernel is not ours to pin. **A skipped guard proves nothing** — a CI run that skips
  these has no database coverage at all and still goes green; the skip message is the only thing that says so.
  After a red CI run, check whether the green one actually *ran* these.
- **LANDMINE — the start budget is 180 s, set by `EmbeddedMongoAvailable.START_TIMEOUT_PROPERTY`; flapdoodle's
  30 s default is not survivable on a loaded CI runner.** The probe and the contexts must carry the **same**
  budget: a probe more patient than what it vouches for approves a start the context then fails on, and a less
  patient one turns a database failure into a green skip. The property is verifiably consumed (set it to `1` and
  every test in both classes fails with `no failure or success message after 1ms`) — re-check that if you touch
  it, because a silently ignored key is this module's recurring failure mode.

## The download route streams, and four things keep it that way

`ModPackController.downloadModpack` / `ServerPackController.downloadServerPack` return
`ResponseEntity<Resource>` over a `FileSystemResource`. Spring writes that with `StreamUtils.copy`, so
nothing buffers — and `ArchiveStreamingTest` pins it by asserting the body is **not** a `ByteArrayResource`.
Keep the return type.

- **Spring handles `Range` itself for a `Resource` return value — do not add `ResourceRegion` code.**
  `AbstractMessageConverterMethodProcessor` sets `Accept-Ranges: bytes`, converts to regions on a `Range`
  header, answers 206 with `Content-Range` and 416 when unsatisfiable (read out of `spring-webmvc` with `javap`).
  Pinned by `ServerPackControllerTest.aRangeRequestIsAnsweredWithThatRangeSoADownloadCanResume`. **A grep of this
  repo finds none of it**, because it is the framework's — don't conclude Range support is absent. It is what
  lets a multi-gigabyte download resume.
- **Nothing may wrap the response stream.** There is no `ShallowEtagHeaderFilter` (it buffers the whole
  body in memory to hash it), no `server.compression.*`, and no `WebMvcConfigurer` anywhere in the module.
  Adding any of the three silently undoes the streaming the test above thinks it is guarding — the body
  would still not be a `ByteArrayResource`, so the guard would stay green.
- **`FileSystemStorageService.load` resolves `$id.zip`; never match by substring.** `ConfigurationHandler.isZip`
  extracts an archive into a sibling directory named after it, so the root holds `anId.zip` **and** `anId/`, and a
  path-contains-id scan serves whichever the listing yields first. The remaining legacy fallback is restricted to
  regular files and matches the entry's own name, not its path.
- **`cache(GridFSFile, GridFsResource)` deliberately does not hash.** It is the GridFS fallback on the download
  route, inside the request, and `StorageSystem.cacheFile` discards the hash anyway. Pinned by
  `recoveringAnArchiveFromGridFsDoesNotHashItOnTheWayOut`, which also asserts the bytes still arrive, so "never
  hashes" cannot be met by never working.

**LANDMINE — `ServerPack.fileName` is a name, never a path, and its getter enforces that.** The write site stores
`.name`, and the getter reports `File(it).name` so legacy rows holding an absolute path stop exposing the server's
filesystem layout without rewriting stored documents. The field is serialised under `/api/v2/serverpacks/{id}`
and used for the `Content-Disposition` header. `zipBuilder` already suffixes `_server_pack.zip`, so the controller
must **not** re-append a suffix. The SPA overrides the name client-side with its own `download` attribute, so a
regression here goes unreported — **a guard rail in one adapter is a reason a bug stays unreported.** Keep the
test fixture production-shaped (a real path with suffix): a fixture that cannot occur in production is not a pin.

## The web module's mod-lists are embedded, not referenced

- `RunConfiguration.startArgs` / `clientMods` / `whitelistedMods` are `MutableList<String>` **embedded in the
  document**. **Do not reintroduce** `@DBRef` collections (`StartArgument` / `ClientMod` / `WhitelistedMod`) for
  them: a document whose only field is its id stores nothing, and the eager join costs a round-trip per entry and
  fans every read out across collections. Building a configuration costs two repository calls, pinned by
  `RunConfigurationServiceTest.buildingAConfigurationCostsTwoRepositoryCalls`.
- **LANDMINE — `In` in a derived query name means "contains any of", not "equals".** The duplicate lookup is
  `…AndStartArgsAndClientModsAndWhitelistedMods`, an exact array match; an `…In` variant reuses a configuration
  that shares a *single* mod with the one being created.
- **The JSON shape is part of this contract, and the SPA is not its only consumer.**
  `RunConfigurationController` returns the entity itself under `/api/v2/runconfigs` — a *versioned* path — so the
  entity's fields are the response body. Changing it means changing all of these in the same commit:
  `serverpackcreator-web-frontend/src/types/api.ts` (declares `string[]`), `RunConfigurationCard.vue` and
  `SubmitModPackForm.vue` (two sites), **and the published description in `serverpackcreator-help`** —
  `Writerside/api-docs.yaml` plus the response samples in `Run-Configs.md`, `Server-Packs.md` and `Modpacks.md`,
  which embed a run-configuration too. **Nothing in the build catches the last group** — `serverpackcreator-help`
  is not a Gradle module. `springdoc` is `developmentOnly`, with the `bootRun` + `curl` command beside it in
  `serverpackcreator-app/build.gradle.kts`, so **regenerate rather than hand-edit**. Only the `info:` block is
  applied by hand afterwards, and says so: springdoc cannot be configured from an annotation on a production
  class, and its defaults are "OpenAPI definition", version `v0` and a `servers` entry for `http://localhost:8080`.
  (Known older drift: `id` typed `integer` where the entities use `@MongoId(FieldType.STRING)`.)
- **`web/migration/` is the pattern to copy if another stored shape changes.**
  - `RunConfigurationListMigration` is the per-document rewrite — join-free (a DBRef's `$id` is the value, so it
    works even after the referenced collections are dropped), tested without a database. It uses `MongoTemplate`
    rather than the repository, necessarily: the mapped type cannot read the old shape.
  - `RunConfigurationListMigrationRunner` applies it on `ApplicationReadyEvent`, not during context startup, so an
    unreachable database delays the migration instead of blocking the boot. Idempotent and element-wise, so an
    interrupted run is *completed* on the next start; orphaned collections are dropped only after a fully
    successful pass. Failures are logged and swallowed on purpose.
  - **Its safety decisions are behind `MigrationStore`, and that is the point.** The ordering (every rewrite
    before any drop), the skip (never drop when nothing was rewritten) and the swallow (an unreachable database
    must not fail the boot) are what can lose data if wrong. `RunConfigurationListMigrationRunnerTest` asserts
    them against a recording double. Copy this seam for the next migration.
  - `MongoMigrationStore.findAll` materialises the collection rather than streaming the cursor, deliberately:
    writing while iterating a live cursor can return a moved document twice, harmless only while every rewrite
    is idempotent.
  - **The retry promise is half true:** the rewrite retries on the next start, the *drop* does not — a pass that
    rewrote everything then failed to drop leaves those collections for good. Harmless, not self-healing.
  - Against an unreachable *localhost* Mongo the listener fails fast (connection refused); a black-holing remote
    host would wait out the selection timeout instead.

## Testing patterns

- **Standalone MockMvc per controller** (not `@SpringBootTest` full-context) is the established
  pattern: real API beans (cached version manifests make `VersionMeta` work offline), `springmockk`
  available for mocking Mongo repositories. **`VersionsControllerTest` is the template.** All seven
  web controllers are covered (versions, settings, modpack, serverpack, runconfiguration, events,
  stats).
- **Web-entity IDs are `private set`** (Spring Data `PersistenceCreator`); tests assign them via the
  `assignEntityId` reflection helper.
- **The controller tests `mockk()` their service, so a green controller test says nothing about the
  service.** `EventServiceTest` and `RunConfigurationServiceTest` test the services directly with mocked
  repositories. They pin the *outcome* — which entries the built object holds and which reach `save` —
  deliberately **not** the number of repository lookups, which is an implementation detail.
- `WebServiceArgumentsTest` covers `WebService.springArguments` **and** `configLocationArgument` — pure
  composition, no context, extracted from `start()` because it hands them straight to Spring Boot. The
  config-location tests pin the **order** of the eight property-file locations, because later locations win and
  the two `overrides.properties` entries must stay last — that is where a container's `spring.mongodb.uri`
  arrives from (see the Mongo landmines above).
- **`WebServiceContextTest` boots the real application context, and needs no database.** The MongoDB driver
  connects lazily, so every bean is constructed and every injection point resolved without a server; it catches
  wiring breaks (a new constructor parameter, a misplaced annotation — removing `@Service` from `EventService`
  fails it). **Landmine:** the three schedules are disabled in it via Spring's `CRON_DISABLED` (`-`) —
  `FileCleanupSchedule` deletes modpack files whose IDs are absent from the database, and a suite running at
  midnight against an unreachable database must not find out what that does. Keep them disabled if you add cases.
- **`web/storage` and `web/scheduling` have their own tests; copy their patterns.** `StorageSystemTest` drives the
  real `FileSystemStorageService` against a `@TempDir` with only the GridFS collaborators mocked;
  `DatabaseStorageServiceTest` asks Spring Data's own `QueryMapper` whether an id `String` maps to an `ObjectId`,
  pinning the GridFS lookup **without a database**; `DownloadRowMappingTest` asks the `MongoMappingContext` which
  property is the id and whether a `Sort` names a field the document actually has — a sort on an absent field is
  not an error in MongoDB, it simply does not order. Remember `ModPackControllerTest` mocks the service, so
  nothing below `ModPackService` is covered by controller tests.
- **`cleanFiles` and `cleanDatabase` are `private`.** Spring invokes them reflectively through
  `@Scheduled`, and so do their tests. Do not widen them to make testing easier.
- **LANDMINE — do not edit `-api` or `-app` sources while a test task is running.** The test JVM loads classes
  lazily, so a recompile underneath it surfaces as `NoClassDefFoundError` in unrelated tests — indistinguishable
  from a real regression. Wait for the run.
- GUI: view-model unit tests; Swing views stay dumb. CLI/entry-point logic pinned by
  `CommandlineParserTest` (headless-independent branches only) and `MigrationManagerTest`
  (mockk-mocked `ApiProperties`, version ranges chosen to never hit a real migration method, plus a
  pin on `LAMBDA_SUFFIX` — the regex stripping the compiler's `$0lambda$1` off a migration method's
  name, which both discovery and version parsing run every declared name through).
- **`VersionCheckerTest`** covers the update-check comparison offline: the class is abstract and `allVersions()`
  is its only data source, so a canned subclass exercises the whole alpha/beta path. **The fake's
  `latestVersion()` computes the newest rather than taking the list head, deliberately:** the real
  `allVersions()` comes from a repository API whose ordering nothing guarantees, and a newest-first fixture
  cannot catch code that depends on ordering.
- **Pre-release ordering is channel-first.** `preReleaseChannel` (alpha < beta < release) with the number as
  tie-break, and `isVersionNewer` (semantic version first) for latest-of-channel — never compare only the number
  after the dot, and never require a candidate to be both newer *and* higher-numbered. **Landmine:**
  `isNewAlphaAvailable` has no explicit "a beta is never offered an alpha" guard because the channel ordering
  subsumes it — weaken that ordering and the rule disappears, and only
  `VersionCheckerTest.aBetaIsNotOfferedAnAlphaOfTheSameVersion` will say so.

## Landmines & verified quirks (durable)

- **LANDMINE — never construct a `Scanner` over `System.in`; prompt through `cli/ConsolePrompt`.**
  `Scanner.close()` closes its source, `System.in` cannot be reopened, and JLine's POSIX terminal holds that
  *same* descriptor as its pty slave (`OsXNativePty`/`LinuxNativePty.current()` pass fd 0; `getSize()` calls
  `ioctl(slave, TIOCGWINSZ)`). Closing it kills the calling shell: the next `readLine` raises
  `Error calling ioctl(TIOCGWINSZ)` inside a `java.io.IOError` — an **Error**, which escapes the shell's per-line
  `catch (e: Exception)` and ends the session. `ConsolePromptTest.noCommandReachesSystemInOnItsOwn` greps the main
  source for `Scanner(System.` and expects nothing.
- **All prompting goes through one `ConsolePrompt`; do not copy prompt loops.** Copied loops drift (wrong
  validation for the thing asked, offered values the matcher rejects). **`readChoice` takes one map**: its keys are
  both what is listed and what is matched, so "offers a choice it will not accept" is unrepresentable.
- **A path argument names itself when it is wrong.** `CommandlineParser.pathAfter` records the path as given and
  returns `null` only when the flag is absent or is the last word; it does **not** decide whether the path is
  usable. That belongs to whoever runs the thing, which is the only place that can name it. Never `get()` an
  Optional path in `ServerPackCreator.run`; when fixing one verb's argument handling, fix every verb's (SCAN,
  CLIENTSIDE_REPORT, VERIFY_CLIENTSIDE, CLIENTSIDE_APPLY, `-config`) — fix the class, not the reported instance.
- **A one-shot run's exit code means something, and `main` must not exit on success.** `run()` returns
  `EXIT_SUCCESS`/`EXIT_FAILURE`, and `main` calls `exitProcess` **only** for a failure. **LANDMINE —
  never `exitProcess(0)` there:** GUI and WEB return from `run()` the moment they hand off to the Swing
  EDT and the embedded server respectively, and both keep the JVM alive on non-daemon threads, so
  exiting on success kills the window or the server as it finishes starting. Bad/missing `-config`, bad `-cgen`,
  bad `-scan`/`-clientsidereport` exit `1`; good `-cgen` and `-help` exit `0`.
  `-withallinconfigdir` fails if **any** config failed, but an empty configs-directory is success —
  nothing to do is not a failure, an unreadable directory is.

- **LANDMINE — FlatLaf's `SystemFileChooser` has no per-file `accept` callback; do not write a `FileFilter`
  subclass expecting one.** `SystemFileChooser.FileFilter` declares **only** `getDescription()` (verified with
  `javap`), because the chooser drives a *native* OS dialog that cannot call back into Java per file. Its only real
  filters, `FileNameExtensionFilter` and `PatternFilter`, are `final` and purely declarative. A subclass adding
  `accept(File)` overrides nothing, compiles **without** `override` — the tell — and is never invoked. Directory
  validation lives *after* the dialog returns, via `File.testFileWrite()` plus a `settings_directory_error` dialog
  (`GlobalSettings`, `WebserviceSettings`) — keep it there. If a restriction is ever needed *inside* the dialog,
  the mechanism is `setApproveCallback`, not a filter.
- **`@get:JsonProperty` on Boolean settings fields:** Jackson drops the `is` prefix otherwise, so
  the frontend (`setting-store.js`) reads `undefined`. Keep the annotation on Boolean web-entity
  getters.
- Stats download-history routes: server packs are `/downloads/serverpacks/{id}` (must not collide
  with the modpack-history route `/downloads/modpacks/{id}`).
- **Settings dirty-check normalizes on store/read, so the GUI must reload after save.** Several
  `ApiProperties` settings are normalized when written or read — `WebserviceConfig` migrates the
  database URI, the locale is parsed, and `PathsConfig.tomcatBaseDirectory`'s **getter has a
  side-effect** (a value deviating from the home-directory is reset to it on read). The settings
  panels' `hasUnsavedChanges()` compares the raw widget value against the (normalized) getter, so
  saving without reloading leaves the unsaved-changes icon stuck on. `SettingsHandling.save()` must
  call each editor's `loadSettings()` after persisting and before `checkAll()` (as `load()` does) so
  the widgets hold exactly what the dirty-check reads back. Don't remove that reload.

## Coroutine ownership (no `GlobalScope`)

- Every GUI coroutine launches on a `gui.utilities.ComponentCoroutineScope` — never `GlobalScope.launch`. It is
  a `SupervisorJob` scope, lazily re-created after a cancel, with synchronized access (launches may start off the
  EDT). Components cancel it from `removeNotify()`; non-components anchor it to their backing component via an
  `AncestorListener` (`ancestorRemoved`) or to the frame via a `WindowListener` (`windowClosed`).
- **`CoroutineStart.ATOMIC` is independently `@DelicateCoroutinesApi`** (unrelated to GlobalScope) —
  the three ATOMIC sites (`ConfigEditor.loadConfiguration`, `IconPreview`, `ControlPanel.generate`)
  keep a narrow `@OptIn(DelicateCoroutinesApi)` for ATOMIC alone, with the start preserved (a
  started generation/load must not be cancellable before its first suspension).
- **`ControlPanel.generate` is anchored to the always-visible bottom-bar panel**, so a running
  generation is cancelled only on window close, never by a tab-switch. Don't re-anchor it to a
  switchable container.
- The check timers register their `ActionListener` in `init` (not the `Timer` super-constructor,
  where `this` is unavailable) so they can launch on an instance scope.

## The config-check timer is on the typing path — keep it cheap

`ConfigCheckTimer` is a **500 ms debounce restarted by a document change in *any* field**
(`ConfigEditor.validationChangeListener` → `checkAll()` → `TabbedConfigsTab.timer.restart()`), and it runs its
whole validation pass for **every open tab** — a user pays for anything it does each time they pause while typing,
multiplied by open configs. Two expensive inputs are memoized in `ConfigEditorViewModel`:

- **`isServerDownloadable`** — an HTTP request to the modloader's maven, existing only to drive a warning label.
  Cached per `(minecraftVersion, modloader, modloaderVersion)`. **Successes are cached, failures are not**,
  deliberately: a published installer does not vanish, but a `false` may only mean the network blinked, and
  remembering it would leave the editor insisting "server unavailable" until restart. Pinned both ways in
  `ConfigEditorViewModelTest`.
- **`packName`** — `checkManifests` parses the launcher manifest (multi-MB) into a Jackson tree; it is re-read
  only when the fingerprint (existence/size/mtime) of the candidates changes. **The fingerprint must read
  `ConfigurationHandler.manifestCandidates`**, never its own copy of the paths — a drifted list makes the memo miss
  real edits. `ManifestCandidatesTest` in `-api` guards it.

Do not build a `PackConfig` per tick. **The concurrency inside is illusory:** the `launch { }` blocks sit inside
`runBlocking { }`, whose dispatcher is the single blocked thread's event loop, so they run sequentially — the only
reason the shared `errors` `ArrayList` is safe. Making them genuinely concurrent introduces a data race.

## SuggestionProvider runs on every keystroke, on the EDT

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
  look-and-feel delegate and is for a LAF *change*, never per keystroke.

**GUI-verifying a Swing popup without Accessibility permission (`osascript` cannot send synthetic input on this
machine):** write a throwaway JUnit test that drives Swing from *inside* the test JVM (`-app` tests are not
headless) — a real component on a real `JFrame`, characters inserted into the document on the EDT, logging each
visible `JList`'s row count and `preferredSize` while `screencapture` takes stills. Re-assert focus before each
burst (the popup shows only while the component `isFocusOwner`). Delete the harness afterwards.

## ConfigEditor status

Extraction is essentially done: the two genuinely-pure pieces — `hasUnsavedChanges` (15-field
PackConfig dirty-check) and `requiredJavaVersion(minecraftVersion)` — live in
`ConfigEditorViewModel` (which takes `VersionMeta`); the Swing methods are one-line facades. The
remaining ~1,330 lines are legitimate view code (MigLayout wiring, combo-box models, status-icon
updates, event handlers) whose domain logic already lives in the tested API. Don't mechanically
extract thin Swing getters.

GUI hot paths are pinned by *call counts* and set identity, never wall-clock; the web module's persistence
declarations are pinned against Spring Data's own machinery (`PartTree`, `MongoMappingContext`,
`MongoPersistentEntityIndexResolver`) so none of them needs a database.
