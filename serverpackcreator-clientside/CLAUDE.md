# serverpackcreator-clientside — module context

> The **clientside-mod verification engine**, extracted out of `serverpackcreator-app` so it can be
> reused by both the app's CLI verbs and the planned standalone Docker "grinder" service. Package
> `de.griefed.serverpackcreator.clientside`. **Depends only on `serverpackcreator-api`** (plus
> Playwright + jackson-module-kotlin) — it must never gain a dependency on `-app`, Spring, or Swing.
> Not published to Maven Central (unlike `-api`), so it can churn freely without compatibility lock-in.

## What it does

Given a Modrinth/CurseForge project-link: pick the platform, resolve the project's files, derive the
clientside-list file-name stem(s), and combine signals into a per-loader `Confidence`. Driven by the
app's four CLI verbs (`-scan`, `-clientsidereport`, `-verifyclientside`, `-clientsideapply`) and the
`clientside-*.yml` workflows — see `serverpackcreator-app/CLAUDE.md` for the verb wiring and CI.

## Engine details & landmines (durable)

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
  "declares server/both yet crashes" lie); a clean boot does not prove server-safe. **The declared
  sideness is a self-report and is unreliable** — that asymmetry is *why* the expensive boot exists.
- **`allowModDistribution=false`** CurseForge files arrive with `downloadUrl=null` (`ModFile.locked`);
  routed (`selectDownloader`) to the **Playwright** headless-browser `BrowserDownloader` (lazy; only
  launched for locked files), everything else to `HttpJarDownloader`. Playwright is declared in **this**
  module's build (`com.microsoft.playwright:playwright`), exported `api` so `-app` gets it transitively.
- **`ClientsideListEditor`** (pure, unit-tested) inserts accepted entries into both files that ship the
  fallback-list: the `fallbackMods` `listOf(...)` block in `GenerationConfig.kt` (sorted, aligned
  `//link` comment, Kotlin trailing-comma is fine) and the backslash-continued `fallbackmodslist` in
  `serverpackcreator.properties`. **Landmine — properties continuation:** the *last* entry must NOT end
  in `,\`, or the continuation bleeds into the next property and corrupts it; the editor strips the
  delimiter off the final line and adds one to the previous-last when appending.

## Testing patterns

- 37 tests across 10 files, all offline. Most build jars in-memory (`java.util.jar`) or feed canned
  JSON to a fake `HttpFetcher`; **`MetadataScannerTest` is the only one needing a resource** — it boots
  an offline `ApiWrapper` from `src/test/resources/serverpackcreator.properties` (whose `ModScanner`
  relies on the API's cached version-manifests, hence `test` `dependsOn :serverpackcreator-api:processTestResources`).
- `BootCandidateSelector`, `BootLogClassifier`, `FilenameStemDeriver`, `ClientsideListEditor` are pure
  and unit-tested without a network or a running server — keep new logic that way where you can.

## Roadmap — the grinder (`serverpackcreator-grinder`, planned)

A standalone fire-and-forget Docker service depending on this module + `docker-java` will boot
candidate mods **in parallel, isolated containers** (`--network none`, resource-capped) to build a
catalog-wide list of suspected-clientside mods. The seam it needs: **split `BootVerifier` into
`prepareBootPack()` (host-side staging, reused) and a `runServer()` interface** with a host-process
impl (current behavior, keeps tests green) and a container impl (new). `BootLogClassifier` is reused
verbatim on the container's streamed logs. Pre-bake/cache the installed loader+libraries per
`(loader, loaderVer, mcVer)` so each actual mod-boot runs offline.
