# serverpackcreator-api

The heart and soul of ServerPackCreator. This is what's responsible for turning your modpacks into server
packs — and it is published to Maven Central so you can do the same from your own code.

This document is the **developer guide**. If you are a user or an admin looking for command-line tools, see
[`serverpackcreator-clientside`](../serverpackcreator-clientside/README.md) (verify clientside-only mods) or
[`serverpackcreator-grinder`](../serverpackcreator-grinder/README.md) (do it at catalogue scale).

**Contents**

1. [Add the dependency](#1-add-the-dependency)
2. [Quickstart: modpack → server pack](#2-quickstart-modpack--server-pack)
3. [The composition root: `ApiWrapper`](#3-the-composition-root-apiwrapper)
4. [Configuring a pack: `PackConfig`](#4-configuring-a-pack-packconfig)
5. [Validating before you generate](#5-validating-before-you-generate)
6. [Generating, and reading the result](#6-generating-and-reading-the-result)
7. [Version metadata](#7-version-metadata)
8. [Scanning mods for sideness](#8-scanning-mods-for-sideness)
9. [Settings, home directory, properties](#9-settings-home-directory-properties)
10. [Writing a plugin](#10-writing-a-plugin)
11. [Pitfalls worth knowing](#11-pitfalls-worth-knowing)
12. [Building & testing this module](#12-building--testing-this-module)

---

## 1. Add the dependency

Group `de.griefed.serverpackcreator`, artifact `serverpackcreator-api`. Use the version of the
[latest release](https://github.com/Griefed/ServerPackCreator/releases).

```kotlin
// build.gradle.kts
repositories { mavenCentral() }

dependencies {
    implementation("de.griefed.serverpackcreator:serverpackcreator-api:<version>")
}
```

```xml
<dependency>
    <groupId>de.griefed.serverpackcreator</groupId>
    <artifactId>serverpackcreator-api</artifactId>
    <version><!-- version --></version>
</dependency>
```

Requires **Java 21+**. The API is Kotlin, but every example below works from Java with the usual
`getFoo()`/`setFoo()` accessors.

---

## 2. Quickstart: modpack → server pack

Four steps: get the API, describe the pack, validate, generate.

```kotlin
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import java.io.File

fun main() {
    val api = ApiWrapper.api()                       // 1. initialise (creates the home directory)

    val config = PackConfig().apply {                // 2. describe what to build
        modpackDir = "/path/to/modpack"
        minecraftVersion = "1.20.1"
        modloader = "Forge"
        modloaderVersion = "47.2.0"
        inclusions.addAll(api.configurationHandler.suggestInclusions(modpackDir))
    }

    val check = api.configurationHandler.checkConfiguration(config)   // 3. validate
    if (!check.allChecksPassed) {
        check.encounteredErrors.forEach { println("error: $it") }
        return
    }

    val generation = api.serverPackHandler.run(config)                 // 4. generate
    if (generation.success) {
        println("Server pack: ${generation.serverPack}")
        generation.serverPackZip.ifPresent { println("ZIP: $it") }
    } else {
        generation.errors.forEach { println("failed: $it") }
    }
}
```

That's the whole happy path. Everything below is detail.

---

## 3. The composition root: `ApiWrapper`

`ApiWrapper` builds and holds the collaborator graph. Every member is lazy, so you only pay for what you
touch.

```kotlin
val api = ApiWrapper.api()                                    // default properties file
val api = ApiWrapper.api(File("/etc/spc/serverpackcreator.properties"))
val api = ApiWrapper.api(File("…"), runSetup = false)         // skip filesystem setup
```

**`api()` is a synchronized singleton.** The first call wins: later calls return that same instance and
*ignore* the arguments you pass. Decide your properties file once, at start-up. If you need setup to run
again, call `api.setup(force = true)`.

| Member                                                       | Use it for                                |
|--------------------------------------------------------------|-------------------------------------------|
| `apiProperties`                                              | Settings, directories, the home directory |
| `configurationHandler`                                       | Validation, `suggestInclusions`           |
| `serverPackHandler`                                          | Generation                                |
| `versionMeta`                                                | Minecraft/loader versions, required Java  |
| `modScanner`                                                 | Declared sideness of local jars           |
| `apiPlugins`                                                 | Loaded pf4j plugins                       |
| `utilities`, `webUtilities`, `jsonUtilities`, `xmlUtilities` | File/HTTP/JSON/XML helpers                |

`runSetup = true` (the default) creates the home directory and extracts the files SPC needs — start
templates, `server.properties`, `server-icon.png`, cached version manifests. Pass `false` only if you have
already done that yourself.

---

## 4. Configuring a pack: `PackConfig`

```kotlin
val config = PackConfig()                                  // empty, fill it in
val config = PackConfig(File("serverpackcreator.conf"))    // load a saved config
config.save(File("serverpackcreator.conf"), api.apiProperties)
```

### Required

| Property           | Example                         | Notes                                                   |
|--------------------|---------------------------------|---------------------------------------------------------|
| `modpackDir`       | `/home/me/modpacks/AwesomePack` | The modpack to convert                                  |
| `minecraftVersion` | `1.20.1`                        |                                                         |
| `modloader`        | `Forge`                         | `Forge`, `NeoForge`, `Fabric`, `Quilt`, `LegacyFabric`  |
| `modloaderVersion` | `47.2.0`                        | Must exist for that Minecraft version                   |
| `inclusions`       | see below                       | Without any, the config check rejects the pack as empty |

### `inclusions` — what actually gets copied

An `InclusionSpecification` is a `source` plus three optional parts:

```kotlin
InclusionSpecification(
    source = "mods",              // relative to modpackDir (or an absolute path)
    destination = null,           // rename inside the server pack
    inclusionFilter = null,       // regex: only matching files
    exclusionFilter = null        // regex: skip matching files
)
```

Let SPC work them out for you — this is what the GUI and CLI do:

```kotlin
config.inclusions.addAll(api.configurationHandler.suggestInclusions(config.modpackDir))
```

### Optional

| Property                                                              | Default                     | Notes                                                                |
|-----------------------------------------------------------------------|-----------------------------|----------------------------------------------------------------------|
| `clientMods`                                                          | empty → SPC's fallback list | Name patterns to exclude as client-only                              |
| `modsWhitelist`                                                       | empty → fallback whitelist  | Never exclude these, even if they match                              |
| `javaArgs`                                                            | empty                       | JVM args baked into the start scripts, e.g. `"-Xmx4G -Xms4G"`        |
| `serverPackSuffix`                                                    | empty                       | Appended to the output directory name                                |
| `serverIconPath` / `serverPropertiesPath`                             | empty → defaults            | Custom icon / properties                                             |
| `isServerIconInclusionDesired` / `isServerPropertiesInclusionDesired` | `true`                      |                                                                      |
| `isZipCreationDesired`                                                | `true`                      | Also produce a ZIP archive                                           |
| `customDestination`                                                   | empty (`Optional`)          | Write the pack here instead of the configured server-packs directory |
| `scriptSettings`                                                      | empty                       | Extra `KEY=VALUE` pairs placed in the generated `variables.txt`      |
| `name`                                                                | empty                       | Display name only; not used for generation                           |

---

## 5. Validating before you generate

```kotlin
val check = api.configurationHandler.checkConfiguration(config)
if (!check.allChecksPassed) {
    check.encounteredErrors.forEach(::println)
}
```

Or straight from a config file, which fills the `PackConfig` as it goes:

```kotlin
val config = PackConfig()
val check = api.configurationHandler.checkConfiguration(File("serverpackcreator.conf"), config)
```

Pass `quietCheck = true` to suppress logging. `ConfigCheck` instances combine with `and(other)`, which is
how plugin-supplied checks are merged in.

Always validate. `checkConfiguration` also *normalises* the config (resolving versions, inspecting the
modpack), so generating without it can behave differently than you expect.

---

## 6. Generating, and reading the result

```kotlin
val generation = api.serverPackHandler.run(config)
```

`ServerPackGeneration` gives you:

| Member          | Meaning                                                |
|-----------------|--------------------------------------------------------|
| `success`       | Whether the pack was produced                          |
| `serverPack`    | The output directory                                   |
| `serverPackZip` | `Optional<File>` — present when `isZipCreationDesired` |
| `errors`        | Why it failed                                          |

Generation is synchronous and can take a while on a large modpack. Run it off your UI thread.

---

## 7. Version metadata

`versionMeta` wraps Mojang's and the loaders' manifests. They are cached in the home directory, so most
lookups work offline.

```kotlin
val meta = api.versionMeta

meta.minecraft.serverReleases()                     // stable releases that have a server
meta.minecraft.requiredJavaVersion("1.20.1")        // Optional<String> — Mojang's declared Java major

meta.forge.newestForgeVersion("1.20.1")             // Optional<String>
meta.neoForge.newestNeoForgeVersion("1.21.1")
meta.fabric.latestLoader()                          // loader versions are Minecraft-independent…
meta.fabric.isMinecraftSupported("1.20.1")          // …so check support separately
meta.quilt.isMinecraftSupported("1.20.1")
meta.legacyFabric.isMinecraftSupported("1.12.2")
```

**Read that Fabric/Quilt/LegacyFabric pair carefully.** Their loader versions are not tied to a Minecraft
version, so `latestLoader()` always returns something. Whether that loader actually *works* for your
Minecraft version is `isMinecraftSupported(...)` — a brand-new Minecraft has no intermediary yet, and
LegacyFabric only covers the pre-1.14 era. Forge and NeoForge are Minecraft-specific and return an empty
`Optional` when unsupported.

Use `requiredJavaVersion` rather than a hand-rolled "era → JDK" mapping; it survives Minecraft's versioning
scheme changes.

---

## 8. Scanning mods for sideness

`modScanner` reads what a jar *declares* about client/server support, per loader. A scan takes a
`Collection<File>` of jars and returns a `ScanResult` — **not** a list of files:

```kotlin
val result = api.modScanner.fabricScanner.scan(jarFiles)   // ScanResult

// mods the scan says can be excluded, with the mod-id that declared it
result.exclusions.forEach { println("${it.modId} -> ${it.excludedMod.name}") }

// mods other mods depend on, so you don't strip something still required
result.dependencies.forEach { println("needed: ${it.identifier}") }
```

Scanners available: `fabricScanner`, `quiltScanner`, `forgeTomlScanner`, `neoForgeTomlScanner`,
`forgeAnnotationScanner` (older Forge). Pick by the pack's loader and Minecraft version.

Declared sideness is a **self-report and often wrong** — a mod can claim `both` and still crash a server. If
you need a trustworthy answer, boot the mod: that is exactly what
[`serverpackcreator-clientside`](../serverpackcreator-clientside/README.md) does.

---

## 9. Settings, home directory, properties

`apiProperties` is the settings hub, backed by a `serverpackcreator.properties` and grouped config objects.

```kotlin
val props = api.apiProperties

props.homeDirectory
props.serverPacksDirectory
props.serverFilesDirectory        // start templates, server.properties, server-icon.png

props.isAutoExcludingModsEnabled = false      // keep every mod, skip scanner-driven exclusion
props.startScriptTemplates                    // extension -> template path (sh / fish / ps1 / bat)
props.javaScriptTemplates
props.defaultStartScriptTemplates()           // the shipped defaults
```

Point the API at a specific configuration by handing that file to `ApiWrapper.api(...)` — best for
reproducible or server-side runs where you don't want the ambient home directory.

Full setting reference: <https://help.serverpackcreator.de/settings-and-configs.html>

---

## 10. Writing a plugin

Plugins are [pf4j](https://github.com/pf4j/pf4j) plugins that hook into generation. There are **six**
extension points:

| Extension point        | Fires                                                 |
|------------------------|-------------------------------------------------------|
| `ConfigCheckExtension` | during configuration validation — add your own checks |
| `PreGenExtension`      | before generation starts                              |
| `PreZipExtension`      | after files are gathered, before the ZIP is written   |
| `PostGenExtension`     | after generation finishes                             |
| `TabExtension`         | adds a tab to the Swing GUI                           |
| `ConfigPanelExtension` | adds a panel to a config editor in the GUI            |

Each extension carries identifying metadata (`name`, `description`, `author`, `version`) via
`ExtensionInformation`, and your plugin class extends `ServerPackCreatorPlugin`.

The **canonical, always-current reference is
[`serverpackcreator-plugin-example`](../serverpackcreator-plugin-example)** — it implements every one of the
six extension points and is deliberately kept in step with the current API idiom. Copy from it rather than
from prose: it is documentation-by-example, and it compiles against the API in CI.

Build your plugin as a jar and drop it into the `plugins` directory of SPC's home directory; `apiPlugins`
discovers it at start-up.

**Compatibility:** the plugin-facing surface stays source-compatible within a major version. Refactors keep
old entry points as `@Deprecated` facades carrying `ReplaceWith` for at least one major release before
removal.

---

## 11. Pitfalls worth knowing

- **`ApiWrapper.api()` is a singleton.** Arguments after the first call are ignored.
- **`PackConfig.modloader` silently ignores unrecognised values**, and unknown loaders fall back to Forge.
  Validate the string yourself if it comes from user input.
- **No `inclusions` means the config check rejects the pack as empty.** Use `suggestInclusions`.
- **Prefer `save(destination, apiProperties)`.** The single-argument `save(destination)` is `@Deprecated` and
  resolves `ApiProperties` through the singleton.
- **`InclusionSpecification` has hand-written `equals`/`hashCode`** over its four fields and is deliberately
  *not* a data class, to keep the public API stable for plugins.
- **Domain logic here is unit-testable without a Spring context.** If you find yourself needing a container
  to test something in this module, that's a design smell worth reporting.
- **`versionMeta` reads cached manifests**, so it works offline; a version whose per-version server JSON was
  never cached may need network on first lookup.

---

## 12. Building & testing this module

```bash
./gradlew serverpackcreator-api:build --full-stacktrace --info
```

```bash
./gradlew :serverpackcreator-api:clean :serverpackcreator-app:test --stacktrace --info
```

Coverage report (Kover), written to `build/reports/kover/`:

```bash
./gradlew :serverpackcreator-api:koverHtmlReport
```

Tests run against real fixture modpacks under `tests/` and `src/test/resources/testresources/` with cached
version manifests — no live network needed.

Deeper architecture notes, established patterns and the durable landmine list live in
[`CLAUDE.md`](CLAUDE.md); the per-package narrative is in [`module.md`](module.md), rendered into the API
docs at <https://help.serverpackcreator.de>.
