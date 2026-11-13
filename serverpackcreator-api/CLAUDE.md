# serverpackcreator-api — module context

> Core library, published to Maven Central. **Public surface is a plugin-compatibility
> constraint** — see the API compatibility policy in the root `CLAUDE.md`. Domain core: must not
> depend on Swing, Spring web, or the frontend, and must be unit-testable without a Spring context.

> **History:** incident narratives, measurements and the full original text of every landmine below are in
> `claude-docs/API-HISTORY.md` — read it when a rule's reason is unclear, or before re-opening a recorded decision.

## Layout & composition

- `ApiWrapper` is the composition root — a thin, lazy, constructor-injected collaborator graph.
  Leave it thin.
- Tests: JUnit 5; real fixture modpacks under `tests/` and `src/test/resources/testresources/`.
  "Tested" = unit tests per class **plus** generation end-to-end. **Offline for versions in the shipped manifest
  snapshot** (`src/main/resources/de/griefed/resources/manifests`, seeded into the home by `ApiWrapper.setup()`); a
  newer version costs one `mcserver/<version>.json` fetch. Keep the snapshot in step with its parent: the release in
  `minecraft-manifest.json`'s `latest.release` must have a matching `mcserver/` file.
  `TestHome.prepare` (a compiled buildSrc class in the java-conventions plugin) wipes the test home before every run
  but **spares `manifests/`**, so that cache persists.

- **LANDMINE — dependency optionality has two spellings; read both or every dependency becomes required.**
  Forge's `mods.toml` uses `mandatory = true|false`; NeoForge's `neoforge.mods.toml` uses `type`
  (`"required"` default, `"optional"`, `"incompatible"`, `"discouraged"`). `ForgeTomlScanner.isOptional` reads
  **both** and must keep doing so — `NeoForgeTomlScanner` overrides only the file name, and NeoForge on MC
  1.20.2–1.20.4 still ships `mods.toml` with `mandatory`, so a per-scanner split misses that overlap.
  - `"incompatible"` is **not a dependency at all** (the mod must *not* be present): `getSidenessAndDependencies`
    drops it before `ModDependency`. It also stays in `notRequiredTypes` so the Forge path never calls it required.
    `"discouraged"` still names something a pack may carry, so it survives as optional.
  - `type` has exactly ONE reader, `getType`, which trims, lowercases and reads via `toString()` (never a cast).
    Hand-written descriptors contain ` Incompatible `; a numeric `type` cast throws `ClassCastException`, which the
    scan loop does not catch, flattening the jar to the never-read-anything fallback silently.
  - **Absent or unreadable means required**, deliberately: required-read-as-optional boots a mod without what it
    needs (a crash, possibly a wrong verdict); optional-read-as-required only refuses a boot. It is also NeoForge's
    documented default. Pinned by `OptionalDependencyTest`.

- **LANDMINE — version metadata is published as immutable snapshots; never hand out a live collection.**
  `VersionMeta` refreshes manifests on a **background coroutine** (`refreshScope.launch`) while callers read. A
  clear-then-refill collection returned directly yields `ConcurrentModificationException` or, silently, an empty
  window — and an empty `serverReleases()` makes `BootVerifier.bootableCombination()` refuse every candidate with
  "No bootable file/Minecraft/loader combination", i.e. engine timing published as a statement about a mod.
  Every `versionmeta` class builds a fresh collection and publishes it in one assignment to a `@Volatile` field.
  - **Unmodifiable views, not merely `List`-typed fields** — a `List` field holding an `ArrayList` can be cast and
    mutated.
  - **`VersionMeta.update()` and `refreshManifests()` share one monitor.** The coroutine calls each meta's
    `update()` directly, so locking `VersionMeta.update()` alone guards nothing.
  - The narrowed published signatures (`MutableList`→`List`, `HashMap`→`Map`) are recorded in
    `claude-docs/API-BEHAVIOUR-CHANGES.md`.

## Established patterns

- **Settings-group extraction** (used to break up `ApiProperties`): (1) write group tests first
  against `PropertyStore`; (2) move get/set logic verbatim into the group class, keys as companion
  constants; (3) `ApiProperties` keeps thin facade properties delegating to the group; (4) run API
  + app suites. Large data blocks (e.g. fallback mod-lists) are moved by script, not retyped.
- `ApiProperties` delegates to `PropertyStore` + the `settings.*Config` groups and keeps only orchestration.
- **`NetworkConfig` is the group to reach for when adding an HTTP call.** It owns the connect/read/download-read
  timeouts; `WebUtilities.openTimedConnection` / `openTimedStream` are the *only* sanctioned way to open a
  connection (see the timeout landmine). It depends only on `PropertyStore` and nothing inside `ApiProperties`
  reads it, so the declaration-order landmine does not apply to it.

## Landmines & verified quirks (durable — do not relearn)

- **Property-group declaration order:** group declarations in `ApiProperties` must come **after**
  the groups they depend on — Kotlin initializes properties in declaration order. Reordering can
  NPE at construction.
- **`ApiProperties` IS log4j's `ConfigurationFactory`** (via `@Plugin`). The log4j-XML machinery
  stays there; moving it risks breaking log4j plugin-discovery.
- **Loader regexes have a single source of truth:** `config.SupportedModloaders` (5 exact-match
  regexes + canonical `names`). Do **not** reintroduce `"^forge$"`-style literals anywhere else.
- **So does loader→scanner selection: `ModScanner.scannerFor(modloader, minecraftVersion)`.** Every consumer
  (`ModListCompiler`, `-clientside`'s `MetadataScanner`) dispatches through it so they cannot disagree. Do **not**
  re-add a `when (modloader)` over concrete scanners — duplication is what hid the Forge-era bug in two places. A
  `null` return means "no scanner knows this loader"; each caller turns it into keep-every-mod. The Quilt arm returns
  `QuiltPackScanner`, which merges quilt+fabric: CLIENT wins a sideness disagreement, otherwise whichever scan
  **actually read a descriptor** wins.
- **LANDMINE — the "nothing could be read" fallback is indistinguishable from a real scan by value.**
  `DescriptorScanner` flattens a missing descriptor to `ScannedMod(modJar)` (file name as `modID`, `SERVER`, empty
  dependencies/provides, `null` constraint) — all values a real descriptor could produce. Anything **merging two
  scans of one jar** must read `ScannedMod.descriptorRead`, never infer from values; otherwise a Fabric-only jar on
  Quilt loses its whole declaration (its dependencies go unreported, so e.g. Fabric API is not kept).
- **A jar carrying no descriptor is NOT a scan failure — do not log it as one.** Every scanner sees the whole
  mods directory and Quilt packs are scanned by both Quilt and Fabric scanners, so one always finds nothing.
  `MissingDescriptorException` (from `getJarJson` / `ForgeTomlScanner.getConfig`) lets `DescriptorScanner` log that
  at DEBUG while real failures (`ZipException`, `ParsingException`) keep ERROR **with** stack trace. Do not merge
  the two catches.
- **`ForgeTomlScanner` treats an absent `[[dependencies]]` block as *no dependencies*, not an error** — raising
  aborts `read()` and replaces the parsed modId with the file name.
- **Scanner hierarchy:** `ModJarScanner` (public contract) → `DescriptorScanner` (owns the walk-the-jars loop and
  the **one-`ScannedMod`-per-input-jar** guarantee; `scan` is `final`; subclasses implement public `read(File)`,
  which may throw — *which* exception is the meaningful part) → `JsonDescriptorScanner` → `FabricFamilyScanner`
  (Fabric + Quilt share id/environment reading, differing in field *paths*; dependency blocks differ in *shape* and
  stay abstract). `JsonBasedScanner` is **removed**, not deprecated — Griefed's call on 2026-08-15, overriding the
  compatibility policy: scanners are not a pf4j extension point, so a facade had no reachable benefit. Use
  `JsonDescriptorScanner`.
- **A constant kept on an extraction facade must *read* its owner, never re-declare the literal.**
  `ServerPackHandler.modFileEndings` and `ConfigurationHandler.zipCheck` are getters delegating to
  `ModListCompiler.modFileEndings` / `ModpackZipInspector.zipCheck`, pinned by `FacadeConstantDelegationTest`,
  which asserts **identity** (a value comparison passes against an equal-valued copy). A duplicated constant lets
  docs and edits land on the dead copy. **Use a getter, not `val x = collaborator.y`:** both facades are declared
  before their collaborator, so an initialiser reads it before it exists (declaration-order landmine).
- **LANDMINE — Minecraft has two versioning schemes; never read a component in isolation.** Releases are
  `1.x[.y]` or `YY.x[.y]` (`26.1.2`, `26.2`); `26.2`'s minor `2` reads as the 1.2 era. Wrong branches fail
  silently (a pack that dies before loading a mod, or a scan that falls back to keep-everything).
  - Shell templates: era tests (Forge launcher era, NeoForge 1.20/1.20.1 installer coordinate) must also require
    major `1`. Pinned by `ScriptTemplateContentTest`, which **executes** the extracted functions across both schemes.
  - Kotlin: scanner era selection compares every component via `SemanticVersionComparator` against 1.13 (where
    Forge switched), lives only in `ModScanner.scannerFor`, pinned by `ModScannerDispatchTest`.
  - Derive from metadata or compare all components; never hand-roll an era heuristic. Known clean:
    `BootCandidateSelector.minecraftComparator`, `ImageJavaRuntimes` (uses `MinecraftMeta.requiredJavaVersion`),
    `LoaderVersionResolver`.
- **LANDMINE — `-Djava.security.manager=allow` is fatal from Java 24 on** (JEP 486: the VM refuses to start).
  `PackConfig.spcSSJArgsKeyDefaultValue` still defaults `SSJ_FORGE_ARGS` to it because ServerStarterJar needs it on
  older Java, so the templates pass it **only below Java 24**. Do not drop the default (old packs need it) and do
  not pass it unconditionally. Only Forge passes it.
  - **`SKIP_JAVA_CHECK=true` still reads the Java version, deliberately** — the resolve call sits outside that
    conditional; skipping means skip comparing/installing, not looking. A user with a custom `JAVA` must still stay
    on the SSJ path for Java 17/21. Pinned by
    `ScriptTemplateContentTest.theBashTemplateResolvesTheJavaVersionEvenWhenChecksAreSkipped`. Don't move it back
    inside the conditional.
  - **`getJavaVersion` runs after the whole Java-check block, and the guard fails safe.** `JAVA_VERSION` starts as
    the literal `do_not_manually_edit` and `installJava` never sets it, so a pack installing its own Java would
    otherwise reach `setupForge` with the placeholder. The guard is `not numeric OR >= 24` → self-install path, so an
    unresolvable version never passes the fatal flag. Pinned by
    `ScriptTemplateContentTest.theBashTemplateDropsTheSecurityManagerFlagWhenTheJavaVersionIsUnknown`. **Neither the
    grinder nor `ScriptTemplateMatrixIT` can catch this class** — both pre-bake Java and never take the install path.
  - **From Java 24 on the templates run the Forge installer themselves** and launch from its argfile
    (`unix_args.txt` / `win_args.txt`). SSJ runs the installer in its own JVM and needs a `SecurityManager` to trap
    the installer's `System.exit(0)`; on 24+ it cannot install one, swallows the error, and the process exits **0**
    after "The server installed successfully" without launching — indistinguishable from a clean shutdown to
    `BootLogClassifier`. Below Java 24 the SSJ path is untouched. Pinned by
    `ScriptTemplateContentTest.theBashTemplateInstallsForgeItselfWhenSSJCannotTrapTheInstallersExit` and
    `ScriptTemplateMatrixIT`. **The grinder cannot catch this class** — it only launches already-installed tuples.
  - **Second reason to bypass SSJ: Forge on MC 1.20.2/1.20.3** (`forgeNeedsItsOwnArgfile` in all three templates).
    Forge's installer writes one of two argfiles and SSJ can only start one:

  | Minecraft | argfile | ServerStarterJar |
  |---|---|---|
  | 1.17 – 1.20.1 | `-p <module path>`, cpw `securejarhandler` | works — cpw's `ModuleClassLoader` ends its parent-layer lookup with `.orElse(getPlatformClassLoader())` |
  | **1.20.2** | `-p <module path> --add-modules ALL-MODULE-PATH`, Forge `securemodules` | **dies** — Forge's fork *throws* `Could not find parent layer for module` instead, because SSJ's synthesised boot layer is one level below the real boot configuration |
  | 1.20.3 onwards | `-jar forge-<version>-shim.jar` | works — SSJ takes its own "jar mode" and synthesises nothing |

  **LANDMINE — do not widen this to "every Forge from 1.20.2 on" from reading the source.** The throw is still in
  `securemodules`, but from 1.20.3 the argfile no longer takes the module-path route, and boots confirm SSJ works
  there. Over-widening costs every modern pack the hosting compatibility SSJ exists for. 1.20.3 is included because
  over-including it is free. **The module named in the exception varies per run**, so match on the message.
  Pinned by `ScriptTemplateContentTest.theBashTemplateBypassesTheStarterJarOnlyWhereForgeCannotBeLaunchedWithIt`
  (both schemes, `26.20.2` included) and `allTemplatesBypassTheStarterJarForTheAffectedForgeVersionsAndTestTheMajor`.
  Keep bash, fish and PowerShell in agreement — verify the latter two by executing the extracted function.
- **LANDMINE — a syntax guard that skips when its interpreter is missing is a guard that never runs.**
  `ShellTemplateSyntaxTest` asks the real interpreter — local if installed, a container otherwise — and **skips,
  never passes**, when neither is reachable. Keep that asymmetry.
  - **Name the PowerShell image's platform** (`--platform linux/amd64`): `mcr.microsoft.com/powershell` has no
    linux/arm64, so Apple Silicon picks arm/v7, qemu segfaults and the container hangs.
  - **Never read pwsh's exit code on an aarch64 host** — under Rosetta it crashes at teardown or flaps. Checks prove
    success **positively**: `SETUP_MARKER`, `FAIL <name>` per rejected template, `DONE_MARKER` last. For the same
    reason `PowerShellInstallerJavaTest` walks the AST with `Where-Object`, not `Ast.FindAll`.
  - `PowerShellInstallerJavaTest` owns the `JAVA_INSTALLER` override check (the fallback to `JAVA` every hand-made
    pack takes). `TemplateInterpreterRunner` is the one definition of "reach an interpreter, locally or in a
    container" — don't grow a second.
- **LANDMINE — a manifest is seeded from the jar only when ABSENT, so an unreadable one must be repaired elsewhere.**
  `JarUtilities.copyFolderFromJar` skips existing files, and `ManifestUpdater.checkManifest` parses the local copy
  too, so a half-written manifest would otherwise be unreplaceable (every start dies in `VersionMeta.<init>`).
  Three pieces, all needed:
  - **`File.writeAtomically`** (`FileUtilities.kt`) stages into a sibling and `ATOMIC_MOVE`s it; `writeTextAtomically`
    is the string overload; `WebUtilities.downloadFile` uses it too. Its `temporaryFile` parameter exists **only so
    the failure can be executed in a test** (same as `ModpackZipInspector`'s `openZip`). **The default staging name
    must stay unique per call** — concurrent writers (grinder workers on a shared installer cache) would interleave
    into a shared one.
  - **`ManifestIntegrity.restoreIfUnreadable`** replaces an unparseable file with the jar's copy, or deletes it when
    the jar has none (`mcserver/`), and deletes the `.etag` sidecar.
  - **`VersionMeta.updateOrRepair`** wraps every meta's `update()`: on failure it repairs and retries **once**. A
    failure that repaired nothing is **rethrown unchanged, deliberately** — the alternative is an empty meta.
  **Do not add a proactive manifest scan to `setup()`** — costly every start; the repair belongs on the failure path.
  `ManifestUpdater`'s SAX catch no longer restores from the jar: it describes only the *upstream* answer.
  **`WebUtilities.downloadFile` returns *did this call write the file*,** never truncates the destination before
  the stream opens, and keeps the existing copy on failure; callers treat `true` as "I have this artifact" and
  `installerFor` never re-downloads an existing file. `downloadAndReplaceFile` deletes first — that is its contract.
  `MinecraftServer.readServerJson` discards an unparseable `mcserver/<version>.json` rather than letting it read as
  "declares no required Java" forever.
- **LANDMINE — deleting `\r` is not a line-ending conversion, and generated files do not all want the same
  ending.** Every run-file from `ServerPackProvisioner.createServerRunFiles` goes through `String.withLineEndings`
  (`FileUtilities.kt`); `lineEndingFor` picks **LF for everything except `bat`/`cmd`, which get CRLF**. Never
  reintroduce `.replace("\r", "")`.
  - Deleting CR collapses a lone-CR file into one line (no shebang, no error). `withLineEndings` normalises to LF
    first, then expands, which also avoids `\r\r\n`.
  - `.bat` must be CRLF: `cmd.exe` can miss a `goto`/`call` label crossing a 512-byte boundary in an LF-only file
    ([DosTips](https://www.dostips.com/forum/viewtopic.php?t=8988)). Templates are operator-editable, so this
    matters even without labels today. `serverpackcreator-plugin-selfextract`'s `Stubs.batch` encodes the same
    fact — change both or neither.
  - Generation decides the ending, never the template. `.gitattributes` keeps templates LF in the working tree only;
    its comment goes stale if `lineEndingFor` moves.
  - Pinned by `RunFileLineEndingsTest` (mocks `ApiProperties` rather than repointing the real template maps, which
    are backed by the shared test home) and `LineEndingTest`.
- **LANDMINE — `variables.txt` is read by FOUR shipped templates, each with its own loader, and must be repaired
  on read.** FTP ASCII mode, hosting-panel editors and Windows editors rewrite it after SPC writes it, so every
  loader strips **both** carriage returns and a leading byte-order mark — `loadVariables` in `default_template.sh`,
  `default_template.fish`, `default_java_template.sh`, `default_java_template.fish`. The installer templates read it
  themselves (they also run standalone), so fixing the start scripts does not cover them. Pinned by
  `VariablesFileLoadingTest` (each template × LF/CRLF/lone-CR × mark/no-mark).
  - **A BOM damages only the FIRST setting** (it sticks to the name, e.g. `JAVA` is silently ignored), so the
    fixture puts `JAVA` first deliberately; a comment-first fixture cannot catch it.
  - **`${settings#$'\xEF\xBB\xBF'}` in bash, `string replace -r '^\x{feff}'` in fish** — anchored, never a bare
    delete (those bytes are legal inside UTF-8). fish's `string trim` does not remove a BOM.
  - bash breaks on CRLF and lone CR; fish only on lone CR. Fix both shells.
  - **`eval "$(tr ...)"`, never `source <(tr ...)`** — bash 3.2 (macOS) silently sets nothing from a process
    substitution handed to `source`.
  - **`tr '\r' '\n'`, never `tr -d '\r'`** — same reason as `withLineEndings`; the extra blank lines are harmless.
  - **The script comments are written for the operator, not for us — Griefed's call, 2026-10-16.**
    These files ship inside every server pack and are write-protected but editable, so a user or a
    contributor reading one has to be able to follow it. "Readability trumps professionalism": say what
    goes wrong in plain words and what the line does, rather than naming the mechanism and moving on.
  - **A fish probe must `functions --erase` between templates** — fish keeps a function defined when a later
    `source` fails, so one template's copy silently grades the next.
- **LANDMINE — a path derived from the home directory must be computed on access, never captured.**
  `PathsConfig.homeDirectory` re-reads on every access (honouring `-Dde.griefed.serverpackcreator.home` first), so
  derived paths must move when the home moves (`--home`, `-D`, GUI settings). A bare `val x = File(serverFilesDirectory, …)`
  freezes the old home — e.g. the script-template properties feeding `defaultStartScriptTemplates()` /
  `defaultJavaScriptTemplates()` would read templates from an abandoned directory. Use `val … get() = …` or a
  field-assigning getter, never a bare initialiser. Pinned by
  `PathsConfigTest.defaultTemplatePathsFollowAHomeDirectoryChangedAfterConstruction` (changes the home under a
  *live* instance).
- **LANDMINE — the first log statement in the process constructs an `ApiProperties`, and a source build's home is
  the working directory.** `ApiProperties` *is* a log4j `ConfigurationFactory`, so log4j builds one during its own
  init. A host must set SPC's environment (`Preferences` node, home) **before its own first log call**, or that
  instance has already resolved and persisted something else. Locally built artifacts have `version=dev`, so
  `devBuild` is true and the home falls back to the working directory (`/` under systemd without
  `WorkingDirectory=`). The fallback requires a writable working directory, `ApiProperties.init` refuses an unusable
  home with a message naming it, and `setLoggingLevel` does not throw. Pinned by
  `PathsConfigTest.theDevEnvironmentFallbackSkipsAnUnwritableWorkingDirectory`, `ApiPropertiesHomeDirectoryTest`,
  `GrinderSpcEnvironmentTest`.
- **LANDMINE — a server pack directory is a *running server's* directory, and `ServerPackUpdater` keeps it that
  way.** It holds a world, `ops.json` and hand-tuned `server.properties` SPC never wrote.
  - **Updating beats overwriting.** `isServerPacksOverwriteEnabled` defaults to **true**; its cleanup must not run
    before `manifest.json` is read, or an update deletes the world. Enforce it where the behaviour lives, not in one
    adapter (the GUI greys the checkbox; other entry points do not).
  - **Prune after the copy, never before.** A copy producing *zero* files is a broken run and prunes nothing; since
    run-files are provisioned either way, only `files` distinguishes it — the guard lives at the call site *and*
    inside `prune`.
  - **Protection guards what exists; it does not forbid creating it.** `preserves()` is
    `isUpdateRun && protects && exists` — without `exists` a first generation cannot ship `server.properties`,
    `variables.txt` or a world.
  - **The ZIP exclusion is keyed on the manifest, not the protected list.** Protected ≠ absent:
    `server.properties` and `variables.txt` are protected *and* part of every pack. Exclude what the manifest does
    not list.
  - **A guard asserting something is *excluded* must also assert what is still *included*** — the archive guard
    names the three files that must survive.
  - The manifest must describe the **whole** pack — icon, `server.properties`, scripts, `variables.txt`,
    `HOW-TO-RUN.md` — with names read from `ServerPackProvisioner.serverRunFileNames`, not re-spelled. Entries are
    `/`-separated via `Path.relativize`.

- **`PackConfig.modloader` setter silently ignores unrecognized values** — it does *not* fall back to Forge; it
  assigns only on a match, so an unmatched config reaches generation with an **empty** modloader. `ModListCompiler`
  warns and includes every mod in that case, pinned by `ModListCompilerTest.unrecognisedModloaderStillYieldsEveryMod`.
  Most-specific loader names must be matched first (LegacyFabric before Fabric, etc.).
- **LANDMINE — never default a parameter to a dispatcher that owns a thread.** `newSingleThreadContext` /
  `newFixedThreadPoolContext` must be `close()`d by their creator; a defaulted parameter has none, so each call
  strands a thread for the JVM's life. `ListUtilities.parallelMap` defaults to `Dispatchers.Default`. **Opting in to
  `DelicateCoroutinesApi` for a default value is the bug, not a formality.** Published API matters even with zero
  call sites in the repo.
- **A thread-identity assertion must use `Thread.threadId()`, never the thread name.** Gradle enables assertions,
  which turns on kotlinx.coroutines debug mode and appends ` @coroutine#N` to thread names, so a name set counts
  coroutines. Conversely, **count** a population of same-named threads — `List - Set` drops all duplicates, so a
  set-difference leak check goes blind once the baseline is non-empty.
- **A guard written after its code is unproven until you break the code.** Change one production line, run the
  class, confirm red, revert — for every guard that never had a red state. Prefer asserting **that a collaborator
  was or was not called** (`verify(exactly = n)`) over a return value a wrong implementation could also produce;
  don't `.sorted()` away an order the code promises; don't assert a value you stubbed the mock to return.
- **LANDMINE — every network call must carry a timeout, through one of two sanctioned routes.** The JDK default is
  *infinite*, and manifest checks sit on the blocking GUI startup path, so a DROPping host hangs the splash screen.
  - `WebUtilities.openTimedConnection` / `openTimedStream` — for anything holding an `ApiProperties`.
  - `URL.timedConnection(connectTimeout, readTimeout)` — what both routes end in, for callers that cannot reach
    `WebUtilities`: settings groups inside `ApiProperties` (a cycle — `UpdateConfig` reads its own `NetworkConfig`),
    and `-app`'s `VersionChecker` (abstract, no-arg constructor; `UpdateChecker` sets its timeout `var`s).
  - **Known benign exceptions:** `ServerPackCreatorPlugin.kt` and `ClassUtilities.kt` open **`jar:` URLs** (no
    socket). Before adding a call, grep `openStream()`/`openConnection()` across `-api` and `-app` rather than
    trusting this list.
  - **The opener returns `URLConnection` — do not narrow it.** `downloadFile` is published and takes any `URL`; a
    `file:` URL yields `FileURLConnection`, and the resulting `ClassCastException` escapes every `IOException`
    catch. Pinned by `WebUtilitiesTimeoutTest.aNonHttpUrlCanStillBeDownloaded`.
  - **A `mockk(relaxed = true)` fixture defeats a timeout guard** — relaxed `Int` is `0`, the JDK's "wait forever".
    Stub timeouts explicitly. Land the settings group first so a new-API pin references real properties and the fix
    turns it green untouched.
  - **A hang guard's bound must not equal the timeout it measures** (`VersionCheckerTimeoutTest`: 45 s vs a 15 s
    default) — equal bounds race. A guard may not depend on the setting it guards, which is why that test is slow.
- **`ManifestUpdater` owns the manifest refresh, and a check must cost exactly one request.** It exists as a
  testable seam (`VersionMeta` resolves its URLs internally). It sends `If-Modified-Since` and returns on `304`
  without parsing. **Do not re-add a reachability pre-check** (a second full request, plus a fresh handshake because
  it disconnects undrained). An unreachable host logs one **WARN** per manifest, not an ERROR with stack trace —
  connection failure and unparseable manifest are caught separately. Pinned by `ManifestUpdaterTest`
  (`aManifestCheckCostsOneRequest`, `anAbsentManifestIsDownloadedInOneRequest`,
  `anUnreachableHostLeavesThePresentManifestIntact`). Further byte savings via ETags are not worth chasing; see the
  archive.
- **The clientside-exclusion loop runs mods × list-entries times — keep invariants out of it.** `ModListCompiler`
  builds one `FilterMatcher` per `compileModList`: the filter setting is read once and each `REGEX`/`EITHER` entry
  compiled once. The point is robustness, not speed: a malformed entry is logged once and skipped instead of
  aborting generation with `PatternSyntaxException`. Pinned by `ModListCompilerHotLoopTest`.
- **Open a modpack archive's central directory once per inspection** — it scales with entry count.
  `checkZipArchive` shares one open between validity check and base-directory scan (`baseDirectoriesOf`), and
  `getAllFilesAndDirectoriesInModpackZip` partitions one header pass. `ModpackZipInspector`'s **defaulted**
  `openZip: (File) -> ZipFile` exists so `ModpackZipInspectorOpenCountTest` can count opens.
  **Fixture landmine:** zip4j's `addFile` with a path-in-zip writes no directory entries; use `addFolder` from a real
  tree. (`checkZipArchive` derives `mods/` from a *file* entry's name, so it works without them.)
- **A getter must not hand out the config's own mutable state.** `GenerationConfig.clientsideModsRegex` /
  `modsWhitelistRegex` build a fresh set via `regexVariantOf` per read (published via `ApiProperties`; the GUI reads
  settings from a `parallelStream`). Pinned by `GenerationConfigTest.regexVariantListsAreNotSharedBetweenReads`.
- **`PackConfig.save(destination, apiProperties)`** is the primary (injection-required) overload;
  `save(destination)` is a `@Deprecated` facade resolving `ApiProperties` via the singleton — don't
  build new call-sites on the deprecated one.
- **`InclusionSpecification`** has a **manual** `equals`/`hashCode` over its four fields (source,
  destination, inclusion/exclusion filter) — intentionally *not* a `data class`, to keep the public
  API stable for plugins. Verified safe: no hash-based collections of inclusions exist.
- **`ReticulatingSplines`** (SimCity-style splash texts) is an intentional just-for-fun API
  endpoint per Griefed — it **stays** in the API; do not move or deprecate it.
- **`java.awt.Desktop` convenience methods stay in `-api`** (`WebUtilities.openLinkInBrowser`,
  `FileUtilities.openFolder`/`openFile`). They are **deliberately** here, not a boundary violation:
  only `-api` is published to Maven, and a plugin may run under the GUI **or** the web backend, so
  keeping these in the API lets plugin authors open browsers/files/folders regardless of host. Do
  **not** invert them behind an app-side adapter — that would remove the capability from plugins.
  (`java.awt` is core JDK; the inward-dep rule only forbids Swing / Spring-web / frontend.)
- **LANDMINE — plugin code runs during loading and reaches back into this API, so loading must happen LAST.**
  `ApiPlugins.loadAndStart()` exists to make that orderable — never load plugins from a constructor or a lazy
  initialiser `stageThree` touches first. Two recursions; closing one without the other swaps them:
  - `ApiWrapper.api()` publishes the singleton **before** running `setup()` (and un-publishes if setup throws).
    `@Synchronized` / `synchronized(this)` are re-entrant on one thread, so a plugin calling `ApiWrapper.api()` from
    its `init` would otherwise build a second wrapper.
  - Kotlin's `SynchronizedLazyImpl` **re-runs the initialiser** on a re-entrant same-thread read rather than
    blocking — relevant anywhere two lazies reach each other (e.g. `serverPackHandler` ↔ `apiPlugins`).
  **Do not "fix" this class by editing the example plugin** — calling `ApiWrapper.api()` from a plugin's `init` is
  documented idiom third parties copy; the API must survive it. `PluginLoadingOrderTest` reproduces the real startup
  order (installs the jar in `@BeforeAll`, then triggers `api()`); a test that installs its fixture after the
  singleton is published cannot catch this.

- **Plugin test jar:** `serverpackcreator-plugin-example-dev.jar` under the API test-resources is a
  build artifact regenerated by `copyPluginsApiUnitTests` — **don't commit rebuilds.** `ApiPluginsTest`
  loads it via pf4j and asserts all six extension points are discovered.

## When extracting/refactoring here

Follow root **Refactor discipline**: characterization tests first, verbatim move, thin deprecated
facade, suites green. The Phase 1a characterization tests (ConfigurationHandler manifest parsing;
ServerPackHandler file-gathering/cleanup/icon/properties/placeholders) pin behavior through the
facades — keep them green.

Guard style: manifest and generation work is pinned by *request*, *read* and *open counts* against loopback servers
and injected openers, never by wall-clock; shipped shell templates are pinned by **executing** them, in a container
for shells not installed locally.
