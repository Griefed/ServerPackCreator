# serverpackcreator-app — module context

> Four applications in one module under `de.griefed.serverpackcreator.app`: `cli`, `gui` (Swing),
> `web` (Spring Boot 4 / Spring 7 backend serving the frontend), `updater`. Entry points
> `ServerPackCreator.kt` + `Mode.kt` decide which runs; args parsed in `CommandlineParser.kt`.

## Clientside-mod verification (CLI tooling — `clientside/` package)

Automates the `[Clientside-mod Addition Request]` issues: derive the clientside-list file-name
stem(s) for a CurseForge/Modrinth project, assess whether it is server-unsafe, and — once accepted —
open the PR. **All three phases done:** metadata signal + server-boot signal + issue-comments +
`accepted`-label → auto-PR.

- **Four CLI verbs**, wired the standard way (a picocli `Command` **and** a `Mode` +
  `CommandlineParser` parse + `ServerPackCreator.kt` dispatch — non-interactive runs go through
  `Mode`, *not* the picocli shell): `-scan <dir> --loader <L> --minecraft <V>` (declared-sideness of
  local jars → JSON), `-clientsidereport <url> [--output <f>]` (metadata-only report),
  `-verifyclientside <url> [--output <f>]` (metadata **+** server-boot), and
  `-clientsideapply --report <json>` (insert accepted entries into the list files — pure editing, no
  staging). The first three reuse `apiWrapper.modScanner`.
  - **Landmine — arg parsing order:** `-clientsideapply` must be checked **before** `CONFIG`/`CGEN` in
    `CommandlineParser`, because those use `.contains()` and `--generation-config` contains `-config`
    (would otherwise be misread as `CONFIG`). The new modes are matched with exact `==`.
- **`MetadataScanner`** mirrors `ModListCompiler`'s loader→scanner dispatch (kept in sync deliberately;
  it is *not* shared code — if you change one, check the other).
- **Platform layer**: `ModPlatform` (Modrinth/CurseForge) over an injectable `HttpFetcher` so tests use
  canned JSON (no live network). **CurseForge has no sideness field** → `Sideness.UNKNOWN`; only
  Modrinth declares `client_side`/`server_side`. Use `JsonNode.textOrNull` for nullable URL fields —
  `asText(null)` returns the literal `"null"` for a JSON-null and would defeat `ModFile.locked`.
- **Boot signal** (`BootVerifier`): force-includes the mod (auto-exclude off, empty clientside-list) +
  its recursively-resolved required deps, generates a server pack and boots it via the ServerStarterJar.
  `BootLogClassifier` reads the `Done (…)! For help` ready-line vs a non-zero exit (pure, unit-tested).
  **Asymmetry baked into the confidence model:** only a CRASH is decisive (→ HIGH, incl. the
  "declares server/both yet crashes" lie); a clean boot does not prove server-safe.
- **`allowModDistribution=false`** CurseForge files arrive with `downloadUrl=null` (`ModFile.locked`);
  routed (`selectDownloader`) to the **Playwright** headless-browser `BrowserDownloader` (lazy; only
  launched for locked files), everything else to `HttpJarDownloader`. Playwright dep is in the app
  build (`com.microsoft.playwright:playwright`).
- **`ClientsideListEditor`** (pure, unit-tested) inserts accepted entries into both files that ship the
  fallback-list: the `fallbackMods` `listOf(...)` block in `GenerationConfig.kt` (sorted, aligned
  `//link` comment, Kotlin trailing-comma is fine) and the backslash-continued `fallbackmodslist` in
  `serverpackcreator.properties`. **Landmine — properties continuation:** the *last* entry must NOT end
  in `,\`, or the continuation bleeds into the next property and corrupts it; the editor strips the
  delimiter off the final line and adds one to the previous-last when appending.
- **Workflows** build the jar **from source** (the verbs aren't in any release yet) and post/update a
  sticky comment (marker `<!-- serverpackcreator-clientside-report -->`); the report embeds its data as
  a hidden `<!-- clientside-report-data … -->` JSON block the accept-workflow reads back.
  `clientside-verify.yml` (issues opened/edited) runs the cheap metadata pass; `clientside-boot.yml`
  (the `verify-boot` label or manual dispatch) runs the expensive per-loader boot + uploads boot logs;
  `clientside-accept.yml` (the `accepted` label) runs `-clientsideapply` and opens a PR against
  `develop` via `peter-evans/create-pull-request`. All three need the `CURSEFORGE_API_KEY` repo secret
  for CurseForge links.

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
