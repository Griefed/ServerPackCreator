# Module serverpackcreator-api

The heart and soul of ServerPackCreator. This is what's responsible for turning your modpacks into
server packs. It is the published library (Maven Central) that the GUI, CLI, web-service and any
third-party plugin build on top of — everything else in the project is just a user-interface wrapped
around this core.

## ELI5: what does this library actually do?

A *modpack* is meant for players' game clients; a *server pack* is the slimmed-down, server-ready
version of it. ServerPackCreator automates that conversion. The whole library boils down to three
steps, and almost every package below serves one of them:

1. **Describe** what you want — fill in a [PackConfig][de.griefed.serverpackcreator.api.config.PackConfig]
   (which modpack, which modloader, which Minecraft version, what to include/exclude).
2. **Check** it for mistakes —
   [ConfigurationHandler.checkConfiguration][de.griefed.serverpackcreator.api.config.ConfigurationHandler.checkConfiguration]
   verifies the modpack exists, the loader/version are real, the filters are valid, etc.
3. **Build** the server pack —
   [ServerPackHandler.run][de.griefed.serverpackcreator.api.serverpack.ServerPackHandler.run] copies
   the right files, drops client-only mods, writes start-scripts and a server-icon, and zips it up.

[ApiWrapper][de.griefed.serverpackcreator.api.ApiWrapper] is the front-door that wires all the
collaborators together so you don't have to.

# Package de.griefed.serverpackcreator.api

**The core package — the front-door and the global state everyone shares.**

This is where a consumer of the library starts. It owns the object-graph and the settings; the actual
work (config-checking, generation, scanning) lives in the sub-packages.

- `ApiWrapper` — **start here.** The composition root: ask it for any part of ServerPackCreator
  (`configurationHandler`, `serverPackHandler`, `versionMeta`, `modScanner`, …) and it lazily builds
  and connects them for you. Think "the factory that hands you a fully assembled ServerPackCreator".
- `ApiProperties` — **all the settings** in one place: working directories, the default client-only
  mod list, default included directories, script templates, Java paths and much more. Loaded from
  `serverpackcreator.properties`. (It is also, by historical necessity, log4j's configuration
  factory.)
- `PropertyStore` — the storage engine `ApiProperties` sits on: loads `.properties` files, gives
  typed get/set accessors with sensible defaults, keeps user-custom properties under their own
  prefix, and writes everything back to disk.
- `ApiPlugins` — the plugin manager. It doesn't generate anything itself; it discovers all installed
  plugins and collects their extension-points so generation and the GUI can call into them.

# Package de.griefed.serverpackcreator.api.config

**Step 1 & 2: describing a server pack and proving the description is valid.**

`PackConfig` is the central data object; everything else here either helps build it (parsing a
modpack's manifest) or checks it before generation.

- `PackConfig` — the recipe for one server pack: modpack directory, modloader + version, Minecraft
  version, which directories/mods to include or exclude, and the generation flags. The single object
  you hand to the checker and then to the generator.
- `ConfigurationHandler` — the **validator-in-chief**. `checkConfiguration()` (and its variants) runs
  every check and reports the errors back so a GUI/CLI/website can show them. It delegates to the
  focused validators below.
- `InclusionsValidator` / `ModloaderValidator` / `ModpackDirectoryValidator` — one validator per
  concern (the include/exclude rules, the modloader+version, the modpack directory itself), split out
  of `ConfigurationHandler` so each is small and unit-testable.
- `InclusionSpecification` — one include-rule: a source, an optional destination, and optional
  inclusion/exclusion regex-filters. (Deliberately *not* a `data class`, to keep the plugin API
  stable.)
- `ExclusionFilter` — *how* client-only mods get excluded (the matching strategy).
- `ModpackManifestParser` — reads a modpack exported from a launcher (CurseForge, Modrinth,
  ATLauncher, GDLauncher, MultiMC/Prism) and pre-fills a `PackConfig` from its manifest, so users
  don't type everything by hand.
- `ModpackZipInspector` — peeks inside a modpack ZIP to list its contents and judge whether it's a
  valid full modpack (has `mods`/`config` at the root).
- `ModpackSource` — an enum marking where a modpack came from.
- `SupportedModloaders` — the **single source of truth** for which loaders exist (their detection
  regexes and canonical names). Do not re-hardcode loader names elsewhere.
- `ConfigCheck` — a small result-holder bundling each check-type's pass/fail and any errors.

# Package de.griefed.serverpackcreator.api.modscanning

**Works out which mods are client-only so they can be left out of the server pack.**

Each modloader stores its mod-metadata differently, so there's one scanner per loader, all reachable
through a single facade.

- `ModScanner` — the **one-stop entry point**: `scannerFor(modloader, minecraftVersion)` answers the
  scanner that reads what that loader read *at that point in its history*, so callers pick the right
  one without knowing the details.
- `ModJarScanner` — the common interface every scanner implements.
- `LoaderDescriptors` — which descriptor file evidences a loader on a given Minecraft version, and the
  era boundaries behind it. The single home for that knowledge: `scannerFor` dispatches through it, and
  it is what `-clientside`'s pre-boot loader gate asks instead of keeping its own copy.
- `FabricScanner` — reads `fabric.mod.json`. `QuiltScanner` — reads `quilt.mod.json`.
  `QuiltPackScanner` merges the Fabric descriptor most Quilt mods also ship; `FabricFamilyScanner` is
  the shared base of the two.
- `ForgeAnnotationScanner` — reads `META-INF/fml_cache_annotation.json` (Forge before Minecraft 1.13).
  `ForgeTomlScanner` — reads `META-INF/mods.toml` (Forge from 1.13, and NeoForge before 1.20.5).
  `NeoForgeTomlScanner` — reads `META-INF/neoforge.mods.toml` (NeoForge from **1.20.5**).
- `JsonDescriptorScanner` — shared helper code for the JSON-based scanners (Fabric/Quilt).
- `ScannedMod` — one scanned jar: its id, what it provides, its dependencies and its declared
  Minecraft range. `MissingDescriptorException` — thrown when a jar carries no descriptor to read.

# Package de.griefed.serverpackcreator.api.serverpack

**Step 3: actually building the server pack.**

`ServerPackHandler` orchestrates; the other classes are the focused workers it delegates to (each was
split out of the once-giant handler).

- `ServerPackHandler` — the **conductor of generation.** Give it a checked `PackConfig` and call
  `run()`; it produces the finished server pack.
- `ModListCompiler` — decides *which mods make the cut*: walks the mods folder, removes user-excluded
  and auto-detected client-only mods, and honours the whitelist.
- `ServerPackFileGatherer` — figures out *which files to copy where*: turns include-specs into
  source→destination pairs, applies the filters, and does the copying.
- `ServerPackFile` — one such source→destination pair (a file/dir in the modpack and where it lands
  in the server pack).
- `ServerPackProvisioner` — adds everything that *isn't* modpack content: the server-icon,
  `server.properties`, start-scripts, the ZIP-archive, installer checks, and the pre/post plugin
  hooks.
- `ServerPackManifest` — the manifest written into every server pack (Minecraft version, loader +
  version, SPC version, file-list).
- `ServerPackGeneration` — the result object `run()` returns, describing the pack that was (or
  failed to be) generated.

# Package de.griefed.serverpackcreator.api.settings

**The settings, broken into focused groups.**

`ApiProperties` used to be one enormous class; its settings now live in these cohesive groups, each
owning one concern and persisting itself through an injected save-callback.

- `PathsConfig` — the home-directory and every path derived from it (configs, logs, manifests, work,
  modpacks, server-files, plugins, server-packs, Tomcat).
- `GenerationConfig` — generation knobs: the client-only mod lists + whitelist, directory
  in-/exclusions, cleanup files, ZIP exclusions, the exclusion-filter and Aikar's flags. **This is
  the file the clientside-tooling edits when a mod is accepted.**
- `JavaConfig` — which Java to use for installs and per-version script-variables.
- `ScriptTemplatesConfig` — the start-/java-script templates used during generation.
- `I18nConfig` — language/locale handling (parses the stored locale, feeds the i18n4k runtime).
- `LoggingConfig` — the log-level (the log4j-XML machinery itself stays in `ApiProperties`).
- `UpdateConfig` — update/release tracking and the URL the fallback mod-lists refresh from.
- `WebserviceConfig` — web-service settings: the MongoDB URI (with legacy migration) and the cron
  schedules for its cleanup/refresh jobs.

# Package de.griefed.serverpackcreator.api.versionmeta

**The "what versions exist?" knowledge-base for Minecraft and every modloader.**

This package answers questions like "is Forge X valid for Minecraft Y?" and "where's the installer
for it?" by parsing cached manifests, so the rest of SPC never has to hit the network at runtime. The
shape repeats per loader: a `*Meta` aggregates/queries, a `*Loader` lists loader-versions, an
`*Installer` lists installer-versions, and `*Instance`/`*Details` model the manifest data.

- `VersionMeta` — the **umbrella**: hands you `minecraft`, `forge`, `fabric`, `quilt`, `legacyFabric`
  and `neoForge` metas.
- `Meta` — the common interface giving uniform access to a loader's versions.
- `Type` — enums for the various version/release aspects. `VersionMetaConfig` — manifest locations &
  config. `InvalidTypeException` — thrown for an invalid `Type`.

# Package de.griefed.serverpackcreator.api.versionmeta.minecraft

Everything about Minecraft itself. `MinecraftMeta` is the entry point; it separates the
`MinecraftClientMeta`/`MinecraftClient` (client versions) from the `MinecraftServerMeta`/
`MinecraftServer` (server versions and their server-jar download URLs).

# Package de.griefed.serverpackcreator.api.versionmeta.forge

Anything and everything related to the Forge-modloader. `ForgeMeta` answers Forge version-questions;
`ForgeLoader` lists the Forge versions per Minecraft version; `ForgeInstance` is one concrete
Minecraft+Forge combination plus its server-installer URL.

# Package de.griefed.serverpackcreator.api.versionmeta.neoforge

Anything and everything related to the NeoForge-modloader. `NeoForgeMeta` is the entry point;
`NeoForgeLoader` lists versions; `NeoForgeInstance` models one combination — with `OldNeoForgeInstance`
and `NewNeoForgeInstance` covering the two naming/coordinate schemes NeoForge has used over time.

# Package de.griefed.serverpackcreator.api.versionmeta.fabric

Anything and everything related to the Fabric-modloader. `FabricMeta` is the entry point.
Because Fabric's loader is Minecraft-independent, the model is richer: `FabricLoader`/
`FabricLoaderDetails` (loader versions), `FabricInstaller` (installer versions), `FabricIntermediary`/
`FabricIntermediaries` (the per-Minecraft "intermediary" mappings Fabric needs), plus
`FabricArguments`, `FabricLibrary` and `FabricDetails` modelling the launch metadata.

# Package de.griefed.serverpackcreator.api.versionmeta.quilt

Anything and everything related to the Quilt-modloader. `QuiltMeta` is the entry point; `QuiltLoader`
lists loader versions and `QuiltInstaller` the installers. Quilt reuses Fabric's intermediaries.

# Package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

Anything and everything related to the LegacyFabric-modloader (Fabric backported to old Minecraft).
`LegacyFabricMeta` is the entry point; `LegacyFabricGame` lists supported Minecraft versions,
`LegacyFabricLoader` the loaders, `LegacyFabricInstaller` the installers, and `LegacyFabricVersioning`
holds the shared version-handling base.

# Package de.griefed.serverpackcreator.api.utilities

A small set of cross-cutting helpers that don't belong to any one step.

- `SecurityScans` — security/integrity checks. `SimpleStopWatch` — lightweight timing for
  generation steps. `SPCListeners` — generic listener interfaces for progress/events.
- `ReticulatingSplines` — SimCity-style just-for-fun splash texts. Intentionally part of the public
  API per Griefed — it stays.

# Package de.griefed.serverpackcreator.api.utilities.common

**The toolbox.** Whenever a small operation turns out to be useful in more than one place, it lands
here — converting strings to booleans, quoting list-entries, downloading/copying files, reading from
JARs, comparing semantic versions, and so on. Each class groups one kind of utility:

- File/IO: `FileUtilities`, `InputStreamUtilities`, `ZipUtilities`, `JarUtilities`, `JarInformation`.
- Text/data: `StringUtilities`, `ListUtilities`, `RegexUtilities`, `BooleanUtilities`,
  `JsonUtilities`, `XmlUtilities`.
- System/web: `SystemUtilities`, `WebUtilities` (also opens links/files/folders — deliberately kept
  here so plugins can use it under either host), `ClassUtilities`.
- Versions: `SemanticVersionComparator`, `UtilityEnums` (e.g. the `Comparison` enum).
- `Utilities` — a convenience aggregate of the above.
- Exceptions: `InvalidFileTypeException`, `InvalidLinkException`, `JarAccessException`, `JsonException`.

# Package de.griefed.serverpackcreator.api.plugins

**The plugin system (pf4j).** This is the public surface third-party plugins compile against, so it
stays source-compatible within a major version. A plugin hooks into well-defined points of the
config-check and generation flow.

- `ServerPackCreatorPlugin` — the base class every plugin extends.
- `PluginContext` — what a plugin receives to interact with SPC. `CustomPluginFactory` — how pf4j
  instantiates plugins.
- `BaseInformation` / `ExtensionInformation` — interfaces each extension implements so SPC knows its
  id/name/description. `ExtensionException` — thrown by a misbehaving extension.

# Package de.griefed.serverpackcreator.api.plugins.configurationhandler

Plugin hook into `ConfigurationHandler`: `ConfigCheckExtension` lets a plugin add its own
configuration checks.

# Package de.griefed.serverpackcreator.api.plugins.serverpackhandler

Plugin hooks into `ServerPackHandler` generation. `PreGenExtension` (before generation),
`PreZipExtension` (after files gathered, before zipping) and `PostGenExtension` (after generation)
run at their named moments; `ServerPackHandlerBase` is the shared base they build on.

# Package de.griefed.serverpackcreator.api.plugins.swinggui

Plugin hooks into the Swing GUI. `TabExtension` adds a whole new tab (`ExtensionTab` is the base to
extend); `ConfigPanelExtension` adds a panel into the config editor (`ExtensionConfigPanel` is the
base); `ServerPackConfigTab` is the interface the host config-tab exposes to those panels.
