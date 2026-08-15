# serverpackcreator-api — module context

> Core library, published to Maven Central. **Public surface is a plugin-compatibility
> constraint** — see the API compatibility policy in the root `CLAUDE.md`. Domain core: must not
> depend on Swing, Spring web, or the frontend, and must be unit-testable without a Spring context.

## Layout & composition

- Packages: `config` (validation, `PackConfig`), `serverpack` (generation, `ServerPackHandler`),
  `modscanning` (clientside-mod detection per loader), `versionmeta` (Minecraft/loader manifests),
  `plugins` (pf4j plugin API), `settings` (extracted config groups), `utilities`.
- `ApiWrapper` is the composition root — a thin, lazy, constructor-injected collaborator graph.
  Leave it thin.
- Tests: JUnit 5; real fixture modpacks under `tests/` and `src/test/resources/testresources/`.
  "Tested" = unit tests per class **plus** generation end-to-end. **Offline for versions in the shipped manifest
  snapshot** (`src/main/resources/de/griefed/resources/manifests`, seeded into the home by `ApiWrapper.setup()`); a
  newer version costs one `mcserver/<version>.json` fetch, and the snapshot lags its own parent manifest (B25).
  `cleanup()` in the java-conventions plugin wipes the test home before every run but **spares `manifests/`** —
  before 2026-07-31 it did not, taking that cache from 643 files to 0 on every single run.

## Established patterns

- **Settings-group extraction** (used to break up `ApiProperties`): (1) write group tests first
  against `PropertyStore`; (2) move get/set logic verbatim into the group class, keys as companion
  constants; (3) `ApiProperties` keeps thin facade properties delegating to the group; (4) run API
  + app suites. Large data blocks (e.g. fallback mod-lists) are moved by script, not retyped.
- `ApiProperties` now delegates to `PropertyStore` + 8 `settings.*Config` groups and retains only
  orchestration, jar/OS info, version/firstRun, preferences, hasteBin and the log4j factory.

## Landmines & verified quirks (durable — do not relearn)

- **Property-group declaration order:** group declarations in `ApiProperties` must come **after**
  the groups they depend on — Kotlin initializes properties in declaration order. Reordering can
  NPE at construction.
- **`ApiProperties` IS log4j's `ConfigurationFactory`** (via `@Plugin`). The log4j-XML machinery
  stays there; moving it risks breaking log4j plugin-discovery.
- **Loader regexes have a single source of truth:** `config.SupportedModloaders` (5 exact-match
  regexes + canonical `names`). Do **not** reintroduce `"^forge$"`-style literals anywhere else.
- **So does loader→scanner selection: `ModScanner.scannerFor(modloader, minecraftVersion)`.** Every
  consumer dispatches through it — `ModListCompiler` for a real generation, `-clientside`'s
  `MetadataScanner` for the metadata signal — so the two cannot disagree about what a jar declared. Do
  **not** re-add a `when (modloader)` over the concrete scanners; that duplication is what hid the Forge
  era bug in two places at once (versioning-scheme landmine below). A `null` return means "no scanner
  knows this loader" and each caller turns it into keep-every-mod. The Quilt arm returns
  `QuiltPackScanner`, which owns the quilt+fabric merge — CLIENT wins, and the *Quilt* `ScannedMod` is
  kept when both agree, because its id and dependency list feed the downstream dependency-rescue.
- **A jar carrying no descriptor is NOT a scan failure — do not log it as one.** Every scanner is handed
  the whole mods-directory, and a Quilt pack is scanned by **both** the Quilt and Fabric scanner by
  design, so one of the two finds nothing in every single-format jar. `MissingDescriptorException`
  (raised by `getJarJson` / `ForgeTomlScanner.getConfig` when the entry is absent) exists purely so
  `DescriptorScanner` can log that at DEBUG while everything else keeps an ERROR **with** its stack
  trace. Measured over one api suite run, scan-failure ERROR lines went **159 → 51**; the survivors are
  37 `ZipException` (corrupt archive) and 14 `ParsingException` (malformed TOML), both real defects in
  a jar. Do not "simplify" the two catches back into one.
- **`ForgeTomlScanner` treats an absent `[[dependencies]]` block as *no dependencies*, not an error.**
  It used to raise `ScanningException`, which aborted `read()` mid-way and replaced the already-parsed
  modId with the **filename**. The verdict was unaffected (no dependencies ⇒ no clientside signal ⇒
  SERVER either way), which is why it never broke a pack — but it discarded good data and shouted about
  an ordinary descriptor. `ScanningException` is gone; nothing threw it afterwards.
- **Scanner hierarchy:** `ModJarScanner` (public contract) → `DescriptorScanner` (owns the walk-the-jars
  loop and the **one-`ScannedMod`-per-input-jar** guarantee; `scan` is `final`, subclasses implement
  `read(File)` — public, because *which* exception it throws is the meaningful part and `scan` flattens
  both outcomes to a default entry — and may throw) → `JsonDescriptorScanner` → `FabricFamilyScanner` (Fabric + Quilt share id
  and environment reading, differing only in field *paths*; dependency blocks differ in *shape*, so they
  stay abstract). `JsonBasedScanner`, the previous JSON helper, was **removed** rather than kept as a
  deprecated facade — Griefed's call on 2026-08-15, overriding the adopted compatibility policy: scanners
  are not a pf4j extension point, so a plugin could subclass it but never register the result, making the
  facade cost with no reachable benefit. A subclass compiled against it will no longer compile; use
  `JsonDescriptorScanner`.
- **A constant kept on an extraction facade must *read* its owner, never re-declare the literal.**
  `ServerPackHandler.modFileEndings` and `ConfigurationHandler.zipCheck` are getters delegating to
  `ModListCompiler.modFileEndings` / `ModpackZipInspector.zipCheck`, pinned by
  `FacadeConstantDelegationTest` — which asserts **identity**, because a value comparison passes
  against a re-introduced equal-valued copy, i.e. exactly the state being guarded. Until 2026-08-02
  both existed twice: Phase 1c/1d moved each constant's sole call site into the new class along with a
  *private copy*, leaving the public declaration behind. Nothing regressed — the copies agreed — but
  **the explanation lived on the dead copy while the consulted one had none**, so an edit aimed at the
  documented constant would have changed nothing at all. Same rule as `SupportedModloaders` above.
  **Use a getter, not `val x = collaborator.y`:** both facades are declared *before* their
  collaborator (`ServerPackHandler:92` vs `:98`, `ConfigurationHandler:88` vs `:109`), so an
  initialiser would read it before it exists — the declaration-order landmine directly above.
- **LANDMINE — Minecraft has two versioning schemes; never read a component in isolation.** Releases are
  either `1.x[.y]` or the newer `YY.x[.y]` (`26.1.2`, `26.2`). Any test on the *minor* component alone is
  therefore wrong: `26.2`'s minor is `2`, which reads as the 1.2 era. Two live instances were found and fixed
  on 2026-07-31, both in the start-script templates and both silent — a wrong branch produces a pack that dies
  before loading a mod, not an error:
  - the Forge launcher era (`SEMANTICS[1] -le 16`) sent every Forge boot on Minecraft 26.x down the legacy
    `forge.jar` path → `Error: Unable to access jarfile forge.jar`. **24 grinder boot logs, all Forge, never
    started the server.**
  - the NeoForge 1.20/1.20.1 installer coordinate (`SEMANTICS[1] -eq 20`) would send a future `26.20` at a
    1.20-era URL. Latent, fixed anyway.

  Both now require major `1` as well, pinned by `ScriptTemplateContentTest`, which **executes** the extracted
  shell functions across both schemes.

  **The Kotlin side was NOT clean — this file claimed it was until 2026-08-15, and a third instance was
  sitting in the generation path the whole time.** `ModListCompiler` chose Forge's scanner with
  `mcVersions[1].toInt() > 12`, and `MetadataScanner` (in `-clientside`) with the same test, so Minecraft
  `26.2` read as the 1.2 era and every modern Forge pack was scanned with `ForgeAnnotationScanner` — the
  1.12-and-older one. No modern jar carries `fml_cache_annotation.json`, so every jar threw, every jar fell
  back to the never-drop-a-jar `SERVER` default, and **auto-exclusion silently did nothing on Forge 26.x**
  while logging one ERROR per mod. It fails safe (everything is included), which is why nobody noticed, and
  the earlier survey looked only at the boot/selection code the grinder work had just touched. Both now
  compare every component via `SemanticVersionComparator` against the version Forge actually switched at
  (1.13), the choice lives once in `ModScanner.scannerFor`, and `ModScannerDispatchTest` pins both era
  boundaries across both schemes.

  Derive from metadata or compare all components; never hand-roll an era heuristic. What *is* clean, and was
  re-checked: `BootCandidateSelector.minecraftComparator` compares component-wise, `ImageJavaRuntimes` takes
  required-Java from `MinecraftMeta.requiredJavaVersion` (Mojang's own declaration), and
  `LoaderVersionResolver` delegates to the manifests.
- **LANDMINE — `-Djava.security.manager=allow` is fatal from Java 24 on.** JEP 486 removed Security Manager
  support, so the VM *refuses to start* rather than ignoring the flag. `PackConfig.spcSSJArgsKeyDefaultValue`
  still defaults `SSJ_FORGE_ARGS` to it, because Forge's ServerStarterJar needs it on older Java — the
  templates therefore pass it **only below Java 24**. Minecraft 26.x requires Java 25, so before that guard
  every modern Forge pack died before Forge loaded; NeoForge/Fabric/Quilt never pass the flag, which is why
  only Forge was affected. Pinned by `ScriptTemplateContentTest`. Do not "simplify" by dropping the default —
  old packs still need it — and do not pass it unconditionally.

  **The flag is load-bearing, not cosmetic — dropping it exposed a second failure.** ServerStarterJar runs the
  Forge installer **inside its own JVM** and installs a `SecurityManager` (`SecurityAccess.wrapNoForceExit`)
  purely to swallow the `System.exit(0)` that installer calls on success. On Java 24+ that manager cannot be
  installed, SSJ catches the `UnsupportedOperationException` **silently**, and the installer's exit terminates
  the whole process: a fresh Forge pack installs, prints *"The server installed successfully"*, exits **0**, and
  never launches. Booting the same pack again works, because the install is then present and SSJ only launches.
  Verified on Minecraft 26.2 / Java 25, in bash *and* fish, while Forge 1.20.1 (Java 17) and NeoForge 26.2
  (Java 25) both install-and-launch in one go — so it is Forge-on-modern-Java specifically, and **exit code 0
  means `BootLogClassifier` cannot distinguish it from a clean shutdown.** From Java 24 on the templates
  therefore never hand SSJ the install: they run the Forge installer themselves and launch from the argfile it
  produces (`unix_args.txt`, `win_args.txt` on Windows). Below Java 24 the SSJ path is untouched. Pinned by
  `ScriptTemplateContentTest.theBashTemplateInstallsForgeItselfWhenSSJCannotTrapTheInstallersExit` and by
  `ScriptTemplateMatrixIT` (Forge 26.2, bash + fish, fresh pack, first invocation).
  **The grinder cannot catch this class of bug** — it pre-bakes the install and boots offline from cache, so it
  only ever exercises the launch of an already-installed tuple. Cached tuples stay valid across this change:
  their `unix_args.txt` is what the new path launches, and `downloadIfNotExist` short-circuits on it offline.
- **LANDMINE — a path derived from the home directory must be computed on access, never captured.**
  `PathsConfig.homeDirectory` re-reads on every access (and now honours `-Dde.griefed.serverpackcreator.home`
  first), so `serverFilesDirectory` and friends move when the home moves — `--home`, the `-D` override, or the GUI
  settings panel. A plain `val x = File(serverFilesDirectory, …)` freezes the *old* home at construction. The eight
  shipped script-template properties did exactly that until 2026-07-31: they feed `defaultStartScriptTemplates()` /
  `defaultJavaScriptTemplates()`, so generation read templates out of a directory the user had left behind — their
  edits silently did nothing, with no error anywhere. All eight are now `val … get() = …`, pinned by
  `PathsConfigTest.defaultTemplatePathsFollowAHomeDirectoryChangedAfterConstruction`, which changes the home
  underneath a *live* instance (the older test built the config afterwards, so a captured value still looked right).
  The file's other 31 path properties use an equivalent field-assigning getter; either shape is fine, a bare
  initialiser is not.
- **`PackConfig.modloader` setter silently ignores unrecognized values** — it does *not* fall back to
  Forge, as this file claimed until 2026-08-14. The setter assigns only on a match (`PackConfig.kt:328-341`),
  so an unrecognised value leaves the field at whatever it already held, which starts as `""`. A config whose
  loader never matched therefore reaches generation with an **empty** modloader. That empty string used to
  reach `ModListCompiler`'s scanner-selection `when`, which had no `else`, and produced a silently empty
  server pack; the `else` now warns and includes every mod, pinned by
  `ModListCompilerTest.unrecognisedModloaderStillYieldsEveryMod`. Most-specific loader names must still be
  matched first (LegacyFabric before Fabric, etc.).
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
- **Plugin test jar:** `serverpackcreator-plugin-example-dev.jar` under the API test-resources is a
  build artifact regenerated by `copyPluginsApiUnitTests` — **don't commit rebuilds.** `ApiPluginsTest`
  loads it via pf4j and asserts all six extension points are discovered.

## When extracting/refactoring here

Follow root **Refactor discipline**: characterization tests first, verbatim move, thin deprecated
facade, suites green. The Phase 1a characterization tests (ConfigurationHandler manifest parsing;
ServerPackHandler file-gathering/cleanup/icon/properties/placeholders) pin behavior through the
facades — keep them green.
