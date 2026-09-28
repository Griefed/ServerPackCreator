/* Copyright (C) 2026 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.api.config

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.core.file.NoFormatFoundException
import com.electronwill.nightconfig.core.io.WritingMode
import com.electronwill.nightconfig.toml.TomlFormat
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.StringUtilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.*

private const val modpackComment =
    "\n Path to your modpack. Can be either relative or absolute." +
    "\n Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\""

private const val serverPackSuffixComment =
    "\n Suffix to append to the server pack to be generated. Can be left blank/empty."

private const val minecraftVersionComment =
    "\n Which Minecraft version to use. Example: \"1.16.5\"." +
    "\n Automatically set when projectID,fileID for modpackDir has been specified."

private const val modloaderComment =
    "\n Which modloader the server pack uses. Must be either \"Forge\", \"NeoForge\", \"Fabric\", \"Quilt\" or \"LegacyFabric\"."

private const val modloaderVersionComment =
    "\n The version of the modloader you want to install. Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\"."

private const val serverPropertiesPathComment =
    "\n Path to a custom server.properties-file to include in the server pack. Can be left blank/empty."

private const val serverIconPathComment =
    "\n Path to a custom server-icon.png-file to include in the server pack. Can be left blank/empty."

private const val javaArgsComment =
    "\n Java arguments to set in the start-scripts for the generated server pack. Default value is \"empty\"." +
    "\n Leave as \"empty\" to not have Java arguments in your start-scripts."

private const val inclusionsComment =
    "\n Server pack inclusion specifications." +
    "\n Requires at minimum a source declaration, with destination and filters being optional." +
    "\n An inclusion filter determines which files get included from the source, whilst the exclusion filter excludes."

private const val clientModsComment =
    "\n List of client-only mods to delete from server pack." +
    "\n No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge/Modrinth!" +
    "\n Example: [AmbientSounds-,ClientTweaks-,PackMenu-,BetterAdvancement-,jeiintegration-]"

private const val whitelistComment =
    "\n List of mods to include if present, regardless whether a match was found through the list of clientside-only mods." +
    "\n No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge/Modrinth!" +
    "\n Example: [Ping-Wheel-]"

private const val includeServerPropertiesComment =
    "\n Include a server.properties in your server pack. Must be true or false." +
    "\n If no server.properties is provided but setting set to true, a default one will be provided. Default value is true."

private const val includeServerIconComment =
    "\n Include a server-icon.png in your server pack. Must be true or false. Default value is true." +
    "\n If no icon is provided but this setting set to true, a default one will be provided. Default value is true."

private const val includeZipCreationComment =
    "\n Create a CurseForge compatible ZIP-archive of the server pack. Must be true or false." +
    "\n Default value is true."

private const val pluginsComment =
    "\n Configurations for any and all plugins installed and used by this configuration. A plugin is identified by its ID."

private const val scriptsComment =
    "\n Key-value pairs for start scripts. A given key in a start script is replaced with the value." +
    "\n Custom key-value pairs are best used in combination with custom script templates, otherwise these settings will" +
    "\n most probably not make it to the scripts when using the default templates."

private const val configVersionComment =
    "\n DO NOT EDIT! ServerPackCreator internal value used to determine potential migration steps necessary between version changes."

private const val modpackDirKey = "modpackDir"

private const val serverPackSuffixKey = "serverPackSuffix"

private const val minecraftVersionKey = "minecraftVersion"

private const val modLoaderKey = "modLoader"

private const val modLoaderVersionKey = "modLoaderVersion"

private const val serverPropertiesPathKey = "serverPropertiesPath"

private const val serverIconPathKey = "serverIconPath"

private const val javaArgsKey = "javaArgs"

private const val inclusionsKey = "inclusions"

private const val clientModsKey = "clientMods"

private const val whitelistKey = "whitelist"

private const val includeServerPropertiesKey = "includeServerProperties"

private const val includeServerIconKey = "includeServerIcon"

private const val includeZipCreationKey = "includeZipCreation"

private const val pluginsKey = "plugins"

private const val scriptsKey = "scripts"

private const val javaKey = "SPC_JAVA_SPC"

private const val configVersionKey = "configVersion"

private const val spcWaitForUserInputKey = "SPC_WAIT_FOR_USER_INPUT_SPC"

private const val spcRestartServerKey = "SPC_RESTART_SPC"

private const val spcSkipJavaCheckKey = "SPC_SKIP_JAVA_CHECK_SPC"

private const val spcJDKVendorKey = "SPC_JDK_VENDOR_SPC"

private const val spcJabbaInstallURLShKey = "SPC_JABBA_INSTALL_URL_SH_SPC"

private const val spcJabbaInstallURLPSKey = "SPC_JABBA_INSTALL_URL_PS_SPC"

private const val spcJabbaInstallVersionKey = "SPC_JABBA_INSTALL_VERSION_SPC"

private const val spcAdditionalArgsKey = "SPC_ADDITIONAL_ARGS_SPC"

private const val spcSSJArgsKey = "SPC_SSJ_FORGE_ARGS_SPC"

private const val spcServerStarterJarForceFetchKey = "SPC_SERVERSTARTERJAR_FORCE_FETCH_SPC"

private const val spcServerStarterJarVersionKey = "SPC_SERVERSTARTERJAR_VERSION_SPC"

private const val spcUseServerStarterJarKey = "SPC_USE_SSJ_SPC"

private const val spcCleanupKey = "SPC_CLEANUP_SPC"

/**
 * Default `JAVA`: the `java` on `PATH`. The grinder overrides it with a bundled JDK per Minecraft version, which
 * is what keeps its boots runnable with no network.
 */
const val javaKeyDefaultValue = "java"
/**
 * Default `WAIT_FOR_USER_INPUT`: a human-run server pauses before closing so the console can be read. **Any
 * unattended boot must set this to `false`, or the script blocks on a `read` forever.**
 */
const val spcWaitForUserInputKeyDefaultValue = "true"
/**
 * Default `RESTART`: do not relaunch the server after it stops.
 */
const val spcRestartServerKeyDefaultValue = "false"
/**
 * Default `SKIP_JAVA_CHECK`: verify the Java version before launching. Deliberately not used to paper over an
 * unsupported JDK — the grinder gates on the image's bundled Javas instead.
 */
const val spcSkipJavaCheckKeyDefaultValue = "false"
/**
 * Default JDK vendor for the Jabba-based Java install helper.
 */
const val spcJDKVendorKeyDefaultValue = "temurin"
/**
 * Where the shell Java-install helper fetches Jabba from.
 */
const val spcJabbaInstallURLShKeyDefaultValue = "https://github.com/Jabba-Team/jabba/raw/main/install.sh"
/**
 * Where the PowerShell Java-install helper fetches Jabba from.
 */
const val spcJabbaInstallURLPSKeyDefaultValue = "https://github.com/Jabba-Team/jabba/raw/main/install.ps1"
/**
 * Pinned Jabba version, so the helper cannot change under a pack that already worked.
 */
const val spcJabbaInstallVersionKeyDefaultValue = "0.14.0"
/**
 * Default `ADDITIONAL_ARGS`: the Log4Shell mitigation, passed to every server SPC generates.
 */
const val spcAdditionalArgsKeyDefaultValue = "-Dlog4j2.formatMsgNoLookups=true"
/**
 * Default `SSJ_FORGE_ARGS`.
 *
 * **Landmine — do not remove this default, and do not pass it unconditionally.** Forge's ServerStarterJar needs it
 * below Java 24 to trap the installer's `System.exit`; from Java 24 JEP 486 makes the VM *refuse to start* with it.
 * The templates therefore pass it only below 24 and install Forge themselves above, and both halves are pinned by
 * `ScriptTemplateContentTest`.
 */
const val spcSSJArgsKeyDefaultValue = "-Djava.security.manager=allow"
/**
 * Default `SERVERSTARTERJAR_FORCE_FETCH`: re-download the starter jar on each boot. **An offline boot must set
 * this to `false`**, or Forge/NeoForge try to fetch it with no network.
 */
const val spcServerStarterJarForceFetchKeyDefaultValue = "true"
/**
 * Default ServerStarterJar version to fetch.
 */
const val spcServerStarterJarVersionKeyDefaultValue = "latest"
/**
 * Default `USE_SSJ`: launch Forge/NeoForge through the ServerStarterJar rather than the installer's argfile.
 */
const val spcUseServerStarterJarKeyDefaultValue = "true"
/** Default `CLEANUP`: which installer leftovers a finished pack deletes on first boot. */
const val spcCleanupKeyDefaultValue =
    "libraries," +
    "run.sh," +
    "run.bat," +
    "*installer.jar," +
    "*installer.jar.log,server.jar," +
    ".mixin.out," +
    "ldlib," +
    "local," +
    "fabric-server-launcher.jar," +
    "fabric-server-launch.jar," +
    ".fabric-installer," +
    "fabric-installer.jar," +
    "legacyfabric-installer.jar," +
    ".fabric versions"

/**
 * A PackConfig contains the settings required to create a server pack.
 * A configuration model usually consists of:
 *
 *  * Modpack directory
 *  * Minecraft version
 *  * Modloader
 *  * Modloader version
 *  * Java args for the start scripts
 *  * Files and directories to copy to the server pack
 *  * Whether to pre-install the modloader server
 *  * Whether to include a server-icon
 *  * Whether to include a server.properties
 *  * Whether to create a ZIP-archive
 *
 * @author Griefed
 */
open class PackConfig() {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /** Loader-name matcher for Forge, from the single source of truth in `SupportedModloaders`. */
    val forge = SupportedModloaders.forge

    /** Loader-name matcher for NeoForge. Tested before [forge], whose pattern also matches `neoforge`. */
    val neoForge = SupportedModloaders.neoForge

    /** Loader-name matcher for Fabric. */
    val fabric = SupportedModloaders.fabric

    /** Loader-name matcher for Quilt. */
    val quilt = SupportedModloaders.quilt

    /** Loader-name matcher for LegacyFabric. Tested before [fabric], whose pattern would otherwise swallow it. */
    val legacyFabric = SupportedModloaders.legacyFabric

    /** Matches a string that is only whitespace, so a blank-but-not-empty config value can be rejected. */
    val whitespace = "^\\s+$".toRegex()

    /**
     * Mods to exclude from the server pack because they are clientside — filenames or name-fragments matched
     * against the modpack's `mods` directory. [modsWhitelist] wins over anything matched here.
     */
    val clientMods: ArrayList<String> = ArrayList(1000)

    /** Mods to keep even when [clientMods] matches them — the escape hatch for an over-broad exclusion. */
    val modsWhitelist: ArrayList<String> = ArrayList(1000)

    /** What to copy into the server pack: per-entry source, destination and optional filters. */
    val inclusions: ArrayList<InclusionSpecification> = ArrayList(100)

    /** Values substituted into the generated start scripts, keyed by their `SPC_..._SPC` placeholder. */
    val scriptSettings = HashMap<String, String>(100)

    /** Per-plugin configuration, keyed by plugin id and handed to whichever extension declared it. */
    val pluginsConfigs = HashMap<String, ArrayList<CommentedConfig>>(20)

    /** Path to the modpack this server pack is generated from. */
    var modpackDir = ""

    /** The Minecraft version the modpack targets, which drives loader-version validity and the required Java. */
    var minecraftVersion = ""

    /** The modloader version — meaningful only together with [minecraftVersion] and [modloader]. */
    var modloaderVersion = ""

    /** JVM arguments written into the generated start scripts. */
    var javaArgs = ""

    /** Appended to the server pack's directory name, so variants of one modpack can coexist. */
    var serverPackSuffix = ""

    /** Path to a server icon to include, or empty to use the shipped default. */
    var serverIconPath = ""

    /** Path to a `server.properties` to include, or empty to use the shipped default. */
    var serverPropertiesPath = ""

    /**
     * The modloader, always stored in SPC's canonical spelling (`Forge`, `NeoForge`, `Fabric`, `Quilt`,
     * `LegacyFabric`).
     *
     * **Landmine:** the setter *silently ignores* an unrecognised value, leaving the previous one in place — a typo
     * does not fail, it does nothing. Check order matters for the same reason: `neoforge` also matches Forge's
     * pattern and `legacyfabric` matches Fabric's, so the specific names are tested first.
     */
    var modloader = ""
        set(newModLoader) {
            if (newModLoader.lowercase().matches(forge)) {
                field = "Forge"
            } else if (newModLoader.lowercase().matches(neoForge)) {
                field = "NeoForge"
            } else if (newModLoader.lowercase().matches(fabric)) {
                field = "Fabric"
            } else if (newModLoader.lowercase().matches(quilt)) {
                field = "Quilt"
            } else if (newModLoader.lowercase().matches(legacyFabric)) {
                field = "LegacyFabric"
            }
        }
    /** Whether the server icon is copied into the pack. Keep the `@get:JsonProperty` rule in mind for web entities. */
    var isServerIconInclusionDesired = true

    /** Whether `server.properties` is copied into the pack. */
    var isServerPropertiesInclusionDesired = true

    /** Whether the finished pack is also zipped, which is what a user downloads from the web frontend. */
    var isZipCreationDesired = true

    /** The modpack's own manifest, when generation was driven from one (CurseForge/Modrinth export). */
    var modpackJson: JsonNode? = null

    /** Version of the config format this instance was read from, used to migrate older `.conf` files. */
    var configVersion: String? = null

    /** Where to write the server pack instead of SPC's server-packs directory, when a caller overrides it. */
    var customDestination: Optional<File> = Optional.empty()

    /** Platform project id when the modpack came from Modrinth or CurseForge, else `null`. */
    open var projectID: String? = null

    /** Platform version/file id the modpack was resolved from, else `null`. */
    open var versionID: String? = null

    /** Where the modpack came from — a directory, a zip, or a platform link — which decides how it is unpacked. */
    open var source: ModpackSource = ModpackSource.DIRECTORY

    /** Display name for the pack, used for the generated directory and in the web frontend. */
    open var name: String? = null

    /**
     * Construct a new configuration model with custom values.
     *
     * @param clientMods                List of clientside mods to exclude from the server pack.
     * @param whitelist                 List of mods to include if present, regardless whether a match was found through [clientMods]
     * @param copyDirs                  List of directories and/or files to include in the server pack.
     * @param modpackDir                The path to the modpack.
     * @param minecraftVersion          The Minecraft version the modpack uses.
     * @param modLoader                 The modloader the modpack uses. Either `Forge`, `Fabric` or `Quilt`.
     * @param modLoaderVersion          The modloader version the modpack uses.
     * @param javaArgs                  JVM flags to create the start scripts with.
     * @param serverPackSuffix          Suffix to create the server pack with.
     * @param serverIconPath            Path to the icon to use in the server pack.
     * @param serverPropertiesPath      Path to the server.properties to create the server pack with.
     * @param includeServerIcon         Whether to include the server-icon.png in the server pack.
     * @param includeServerProperties   Whether to include the server.properties in the server pack.
     * @param includeZipCreation        Whether to create a ZIP-archive of the server pack.
     * @param scriptSettings            Map containing key-value pairs to be used in start script creation.
     * @param pluginsConfigs             Configuration for any and all plugins used by this configuration.
     * @author Griefed
     */
    constructor(
        clientMods: List<String>,
        whitelist: List<String>,
        copyDirs: List<InclusionSpecification>,
        modpackDir: String,
        minecraftVersion: String,
        modLoader: String,
        modLoaderVersion: String,
        javaArgs: String,
        serverPackSuffix: String,
        serverIconPath: String,
        serverPropertiesPath: String,
        includeServerIcon: Boolean,
        includeServerProperties: Boolean,
        includeZipCreation: Boolean,
        scriptSettings: HashMap<String, String>,
        pluginsConfigs: HashMap<String, ArrayList<CommentedConfig>>
    ) : this() {
        this.clientMods.addAll(clientMods)
        this.modsWhitelist.addAll(whitelist)
        this.inclusions.addAll(copyDirs)
        this.modpackDir = modpackDir
        this.minecraftVersion = minecraftVersion
        this.modloader = modLoader
        this.modloaderVersion = modLoaderVersion
        this.javaArgs = javaArgs
        this.serverPackSuffix = serverPackSuffix
        this.serverIconPath = serverIconPath
        this.serverPropertiesPath = serverPropertiesPath
        this.isServerIconInclusionDesired = includeServerIcon
        this.isServerPropertiesInclusionDesired = includeServerProperties
        this.isZipCreationDesired = includeZipCreation
        this.scriptSettings.putAll(scriptSettings)
        this.pluginsConfigs.putAll(pluginsConfigs)
    }

    /**
     * Create a new configuration model from a config file.
     *
     * @param configFile Configuration file to load.
     * @throws FileNotFoundException  if the specified file can not be found.
     * @throws NoFormatFoundException if the configuration format could not be determined by
     * Night-Config.
     * @author Griefed
     */
    @Throws(NoFormatFoundException::class, FileNotFoundException::class)
    @Suppress("UNCHECKED_CAST")
    constructor(configFile: File) : this() {
        if (!configFile.exists()) {
            throw FileNotFoundException("Couldn't find file: $configFile")
        }
        val config = FileConfig.of(configFile, TomlFormat.instance())
        config.load()

        configVersion = config.get(configVersionKey)
        if (configVersion == null) {
            if ((config.getOrElse("copyDirs", mutableListOf<String>()) as ArrayList<String>).isNotEmpty()) {
                log.info("Migrating old copyDirs to new inclusions")
                migrateCopyDirsToInclusions(config)
            }
        } else if (configVersion == "TEST") {
            log.warn("You are using a server pack configuration created through a dev|alpha|beta-version.")
            log.warn("Things may break, the config may be incompatible, errors may occur.")
        }

        val inclusions = config.get<ArrayList<CommentedConfig>>(inclusionsKey)
        val inclusionSpecs = ArrayList<InclusionSpecification>()
        for (inclusion in inclusions) {
            inclusionSpecs.add(
                InclusionSpecification(
                    inclusion.get("source"),
                    inclusion.get("destination"),
                    inclusion.get("inclusionFilter"),
                    inclusion.get("exclusionFilter")
                )
            )
        }
        setInclusions(inclusionSpecs)

        setClientMods(config.getOrElse(clientModsKey, listOf("")).toMutableList())
        setModsWhitelist(config.getOrElse(whitelistKey, listOf("")).toMutableList())
        modpackDir = config.getOrElse(modpackDirKey, "")
        minecraftVersion = config.getOrElse(minecraftVersionKey, "")
        modloader = config.getOrElse(modLoaderKey, "")
        modloaderVersion = config.getOrElse(modLoaderVersionKey, "")
        javaArgs = config.getOrElse(javaArgsKey, "")
        serverPackSuffix = StringUtilities.pathSecureText(config.getOrElse(serverPackSuffixKey, ""))
        serverIconPath = config.getOrElse(serverIconPathKey, "")
        serverPropertiesPath = config.getOrElse(serverPropertiesPathKey, "")
        isServerIconInclusionDesired = config.getOrElse(includeServerIconKey, false)
        isServerPropertiesInclusionDesired = config.getOrElse(includeServerPropertiesKey, false)
        isZipCreationDesired = config.getOrElse(includeZipCreationKey, false)
        try {
            for ((key, value) in (config.get<Any>(pluginsKey) as CommentedConfig).valueMap()) {
                pluginsConfigs[key] = value as ArrayList<CommentedConfig>
            }
        } catch (ignored: Exception) {
            // Plugin-configs are optional; a missing or old-format section is intentionally skipped.
        }
        try {
            for ((key, value) in (config.get<Any>(scriptsKey) as CommentedConfig).valueMap()) {
                scriptSettings[key] = value.toString()
            }
        } catch (ignored: Exception) {
            // Script-settings are optional; a missing or old-format section is intentionally skipped.
        }

        for ((key,value) in defaultScriptValues) {
            if (!scriptSettings.containsKey(key)) {
                scriptSettings[key] = value
            }
        }

        config.close()
    }

    private fun migrateCopyDirsToInclusions(config: FileConfig) {
        val copyDirs = config.get("copyDirs") as ArrayList<Any>
        val inclusions = ArrayList<InclusionSpecification>()
        var entries: List<String>
        var inclusion: InclusionSpecification
        for (dir in copyDirs) {
            if (dir is InclusionSpecification) {
                inclusions.add(dir)
            } else if (dir is String) {
                if (dir.contains(";")) {
                    entries = dir.split(";")
                    inclusion = InclusionSpecification(entries[0], entries[1])
                    inclusions.add(inclusion)
                } else if (dir.contains("==")) {
                    entries = dir.split("==")
                    inclusion = InclusionSpecification(entries[0], null, entries[1])
                    inclusions.add(inclusion)
                } else if (dir.startsWith("!")) {
                    val cleaned = dir.substring(1)
                    if (cleaned.contains("==")) {
                        entries = dir.split("==")
                        inclusion = InclusionSpecification(entries[0], null, null, entries[1])
                        inclusions.add(inclusion)
                    } else {
                        inclusion = InclusionSpecification("", null, null, cleaned)
                        inclusions.add(inclusion)
                    }
                } else {
                    inclusion = InclusionSpecification(dir, null, null, null)
                    inclusions.add(inclusion)
                }
            }
        }

        val newInclusionsList = mutableListOf<CommentedConfig>()
        var newInclusionConfig: Config
        var newInclusionMap: HashMap<String, String>
        for (newInclusion in inclusions) {
            newInclusionConfig = TomlFormat.newConfig()
            newInclusionMap = newInclusion.asHashMap()
            newInclusionConfig.valueMap().putAll(newInclusionMap)
            newInclusionsList.add(newInclusionConfig)
        }
        config.set<Any>(inclusionsKey, newInclusionsList)
    }

    /**
     * Save this configuration to disk, resolving the [ApiProperties] through the global
     * [ApiWrapper]-singleton. Prefer the explicit [save] overload which receives the
     * [ApiProperties] directly, avoiding the hidden service-locator dependency.
     */
    @Deprecated(
        "Resolves ApiProperties through the ApiWrapper-singleton.",
        ReplaceWith("save(destination, apiProperties)")
    )
    fun save(destination: File): PackConfig = save(destination, ApiWrapper.api().apiProperties)

    /**
     * Save this configuration to disk, using the given [apiProperties] for the configuration
     * version-stamp.
     */
    @Suppress("DuplicatedCode")
    fun save(destination: File, apiProperties: ApiProperties): PackConfig {
        val conf = TomlFormat.instance().createConfig()

        conf.setComment(configVersionKey, configVersionComment)
        conf.set<Any>(configVersionKey, apiProperties.configVersion)

        conf.setComment(modpackDirKey, modpackComment)
        conf.set<Any>(modpackDirKey, modpackDir)

        conf.setComment(serverPackSuffixKey, serverPackSuffixComment)
        conf.set<Any>(serverPackSuffixKey, serverPackSuffix)

        conf.setComment(minecraftVersionKey, minecraftVersionComment)
        conf.set<Any>(minecraftVersionKey, minecraftVersion)

        conf.setComment(modLoaderKey, modloaderComment)
        conf.set<Any>(modLoaderKey, modloader)

        conf.setComment(modLoaderVersionKey, modloaderVersionComment)
        conf.set<Any>(modLoaderVersionKey, modloaderVersion)

        conf.setComment(serverPropertiesPathKey, serverPropertiesPathComment)
        conf.set<Any>(serverPropertiesPathKey, serverPropertiesPath)

        conf.setComment(serverIconPathKey, serverIconPathComment)
        conf.set<Any>(serverIconPathKey, serverIconPath)

        conf.setComment(javaArgsKey, javaArgsComment)
        conf.set<Any>(javaArgsKey, javaArgs)

        conf.setComment(clientModsKey, clientModsComment)
        conf.set<Any>(clientModsKey, clientMods)

        conf.setComment(whitelistKey, whitelistComment)
        conf.set<Any>(whitelistKey, modsWhitelist)

        conf.setComment(includeServerPropertiesKey, includeServerPropertiesComment)
        conf.set<Any>(includeServerPropertiesKey, isServerPropertiesInclusionDesired)

        conf.setComment(includeServerIconKey, includeServerIconComment)
        conf.set<Any>(includeServerIconKey, isServerIconInclusionDesired)

        conf.setComment(includeZipCreationKey, includeZipCreationComment)
        conf.set<Any>(includeZipCreationKey, isZipCreationDesired)

        val inclusionsList = mutableListOf<CommentedConfig>()
        var inclusionConfig: Config
        var inclusionMap: HashMap<String, String>
        for (inclusion in inclusions) {
            inclusionConfig = TomlFormat.newConfig()
            inclusionMap = inclusion.asHashMap()
            inclusionConfig.valueMap().putAll(inclusionMap)
            inclusionsList.add(inclusionConfig)
        }
        conf.setComment(inclusionsKey, inclusionsComment)
        conf.set<Any>(inclusionsKey, inclusionsList)

        val scripts: Config = TomlFormat.newConfig()
        for ((key, value) in scriptSettings) {
            scripts.set<Any>(key, value)
        }
        conf.setComment(scriptsKey, scriptsComment)
        conf.add(scriptsKey, scripts)

        val plugins: Config = TomlFormat.newConfig()
        plugins.valueMap().putAll(pluginsConfigs)
        conf.setComment(pluginsKey, pluginsComment)
        conf.set<Any>(pluginsKey, plugins)

        val confFile = if (!destination.name.endsWith(".conf")) {
            File( "${destination.absolutePath}.conf")
        } else {
            destination
        }
        TomlFormat.instance().createWriter().write(conf, confFile, WritingMode.REPLACE, StandardCharsets.UTF_8)
        log.debug("Saved config to ${destination.absolutePath}")
        return this
    }

    /** Replace all per-plugin configuration wholesale, discarding whatever was set before. */
    fun setPluginsConfigs(pluginConfigs: HashMap<String, ArrayList<CommentedConfig>>) {
        this.pluginsConfigs.clear()
        this.pluginsConfigs.putAll(pluginConfigs)
    }

    /**
     * Configuration for one plugin, **creating an empty list on first ask** so an extension can always read and
     * append without null-checking. That side effect is why this is a function rather than a map access.
     */
    fun getPluginConfigs(pluginId: String): ArrayList<CommentedConfig> {
        if (!pluginsConfigs.containsKey(pluginId)) {
            pluginsConfigs[pluginId] = ArrayList(100)
        }
        return pluginsConfigs[pluginId]!!
    }

    /** Replace the clientside-exclusion list wholesale. */
    fun setClientMods(newClientMods: MutableList<String>) {
        clientMods.clear()
        newClientMods.removeIf { entry: String -> entry.isBlank() || entry.matches(whitespace) }
        clientMods.addAll(newClientMods)
    }

    /** Replace the whitelist wholesale — entries here survive a [clientMods] match. */
    fun setModsWhitelist(newModsWhitelist: MutableList<String>) {
        modsWhitelist.clear()
        newModsWhitelist.removeIf { entry: String -> entry.isBlank() || entry.matches(whitespace) }
        modsWhitelist.addAll(newModsWhitelist)
    }

    /** Replace the inclusion specifications wholesale. */
    fun setInclusions(newCopyDirs: ArrayList<InclusionSpecification>) {
        inclusions.clear()
        inclusions.addAll(newCopyDirs)
    }

    /** Replace the start-script placeholder values wholesale. */
    fun setScriptSettings(settings: HashMap<String, String>) {
        scriptSettings.clear()
        scriptSettings.putAll(settings)
    }

    /** Every field of this configuration, for logging a generation run and for debugging a rejected config. */
    override fun toString(): String {
        return "Pack(" +
                " clientMods=$clientMods," +
                " whiteList=$modsWhitelist," +
                " copyDirs=$inclusions," +
                " scriptSettings=$scriptSettings," +
                " pluginsConfigs=$pluginsConfigs," +
                " modpackDir='$modpackDir'," +
                " minecraftVersion='$minecraftVersion'," +
                " modloaderVersion='$modloaderVersion'," +
                " javaArgs='$javaArgs'," +
                " serverPackSuffix='$serverPackSuffix'," +
                " serverIconPath='$serverIconPath'," +
                " serverPropertiesPath='$serverPropertiesPath'," +
                " modloader='$modloader'," +
                " isServerIconInclusionDesired=$isServerIconInclusionDesired," +
                " isServerPropertiesInclusionDesired=$isServerPropertiesInclusionDesired," +
                " isZipCreationDesired=$isZipCreationDesired)"
    }

    /** The start-script placeholder defaults every new configuration begins with. */
    companion object {
        /**
         * Default value per `SPC_..._SPC` placeholder, applied unless a config overrides it. **Do not drop entries
         * here to "simplify":** `SPC_SSJ_FORGE_ARGS_SPC` still defaults to `-Djava.security.manager=allow` because
         * Forge's ServerStarterJar needs it below Java 24, and the templates decide when to pass it.
         */
        val defaultScriptValues = hashMapOf(
            Pair(javaKey,javaKeyDefaultValue),
            Pair(spcWaitForUserInputKey,spcWaitForUserInputKeyDefaultValue),
            Pair(spcRestartServerKey,spcRestartServerKeyDefaultValue),
            Pair(spcSkipJavaCheckKey,spcSkipJavaCheckKeyDefaultValue),
            Pair(spcJDKVendorKey,spcJDKVendorKeyDefaultValue),
            Pair(spcJabbaInstallURLShKey,spcJabbaInstallURLShKeyDefaultValue),
            Pair(spcJabbaInstallURLPSKey,spcJabbaInstallURLPSKeyDefaultValue),
            Pair(spcJabbaInstallVersionKey,spcJabbaInstallVersionKeyDefaultValue),
            Pair(spcAdditionalArgsKey,spcAdditionalArgsKeyDefaultValue),
            Pair(spcSSJArgsKey,spcSSJArgsKeyDefaultValue),
            Pair(spcServerStarterJarForceFetchKey, spcServerStarterJarForceFetchKeyDefaultValue),
            Pair(spcServerStarterJarVersionKey, spcServerStarterJarVersionKeyDefaultValue),
            Pair(spcUseServerStarterJarKey,spcUseServerStarterJarKeyDefaultValue),
            Pair(spcCleanupKey, spcCleanupKeyDefaultValue),
        )
    }
}