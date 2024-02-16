/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import com.electronwill.nightconfig.core.file.NoFormatFoundException
import com.electronwill.nightconfig.core.io.WritingMode
import com.electronwill.nightconfig.toml.TomlFormat
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

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

private const val spcVersionKey = "SPC_SERVERPACKCREATOR_VERSION_SPC"

private const val spcMinecraftVersionKey = "SPC_MINECRAFT_VERSION_SPC"

private const val spcModloaderKey = "SPC_MODLOADER_SPC"

private const val spcModloaderVersionKey = "SPC_MODLOADER_VERSION_SPC"

private const val spcJavaArgsKey = "SPC_JAVA_ARGS_SPC"

private const val spcFabricInstallerVersionKey = "SPC_FABRIC_INSTALLER_VERSION_SPC"

private const val spcQuiltInstallerVersionKey = "SPC_QUILT_INSTALLER_VERSION_SPC"

private const val spcLegacyFabricInstallerVersionKey = "SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"

private const val spcMinecraftServerUrlKey = "SPC_MINECRAFT_SERVER_URL_SPC"

private val scriptSettingsDefaultKeys = arrayOf(
    spcVersionKey,
    spcMinecraftVersionKey,
    spcModloaderKey,
    spcModloaderVersionKey,
    spcJavaArgsKey,
    spcFabricInstallerVersionKey,
    spcQuiltInstallerVersionKey,
    spcLegacyFabricInstallerVersionKey,
    spcMinecraftServerUrlKey
)

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
actual open class PackConfig actual constructor() : Pack<File, JsonNode, PackConfig>() {
    private val log = cachedLoggerOf(this.javaClass)
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
    actual constructor(
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
        this.inclusions.addAll(copyDirs)
        this.modpackDir = modpackDir
        this.minecraftVersion = minecraftVersion
        this.modloader = modLoader
        this.modloaderVersion = modLoaderVersion
        this.javaArgs = javaArgs
        this.serverPackSuffix = serverPackSuffix
        this.serverIconPath = serverIconPath
        this.serverPropertiesPath = serverPropertiesPath
        isServerIconInclusionDesired = includeServerIcon
        isServerPropertiesInclusionDesired = includeServerProperties
        isZipCreationDesired = includeZipCreation
        this.scriptSettings.putAll(scriptSettings)
        this.pluginsConfigs.putAll(pluginsConfigs)
    }

    /**
     * Create a new configuration model from a config file.
     *
     * @param utilities  Instance of our SPC utilities.
     * @param configFile Configuration file to load.
     * @throws FileNotFoundException  if the specified file can not be found.
     * @throws NoFormatFoundException if the configuration format could not be determined by
     * Night-Config.
     * @author Griefed
     */
    @Throws(NoFormatFoundException::class, FileNotFoundException::class)
    @Suppress("UNCHECKED_CAST")
    constructor(utilities: Utilities, configFile: File) : this() {
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
        serverPackSuffix = utilities.stringUtilities
            .pathSecureText(config.getOrElse(serverPackSuffixKey, ""))
        serverIconPath = config.getOrElse(serverIconPathKey, "")
        serverPropertiesPath = config.getOrElse(serverPropertiesPathKey, "")
        isServerIconInclusionDesired = config.getOrElse(includeServerIconKey, false)
        isServerPropertiesInclusionDesired = config.getOrElse(includeServerPropertiesKey, false)
        isZipCreationDesired = config.getOrElse(includeZipCreationKey, false)
        try {
            for ((key, value) in (config.get<Any>(pluginsKey) as CommentedConfig).valueMap()) {
                pluginsConfigs[key] = value as ArrayList<CommentedConfig>
            }
        } catch (ignored: Exception) {}
        try {
            for ((key, value) in (config.get<Any>(scriptsKey) as CommentedConfig).valueMap()) {
                scriptSettings[key] = value.toString()
            }
        } catch (ignored: Exception) {
        }
        if (!scriptSettings.containsKey(javaKey)) {
            scriptSettings[javaKey] = "java"
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

    @Suppress("DuplicatedCode")
    actual override fun save(destination: File, apiProperties: ApiProperties): PackConfig {
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
            if (scriptSettingsDefaultKeys.all { key != it }) {
                scripts.set<Any>(key, value)
            }
        }
        conf.setComment(scriptsKey, scriptsComment)
        conf.add(scriptsKey, scripts)

        val plugins: Config = TomlFormat.newConfig()
        plugins.valueMap().putAll(pluginsConfigs)
        conf.setComment(pluginsKey, pluginsComment)
        conf.set<Any>(pluginsKey, plugins)

        TomlFormat.instance().createWriter()
            .write(conf, destination, WritingMode.REPLACE, StandardCharsets.UTF_8)
        return this
    }
}