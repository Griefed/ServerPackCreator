/* Copyright (C) 2023  Griefed
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
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

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

    /**
     * Construct a new configuration model with custom values.
     *
     * @param clientMods                List of clientside mods to exclude from the server pack.
     * @param copyDirs                  List of directories and/or files to include in the server pack.
     * @param modpackDir                The path to the modpack.
     * @param minecraftVersion          The Minecraft version the modpack uses.
     * @param modLoader                 The modloader the modpack uses. Either `Forge`, `Fabric` or `Quilt`.
     * @param modLoaderVersion          The modloader version the modpack uses.
     * @param javaArgs                  JVM flags to create the start scripts with.
     * @param serverPackSuffix          Suffix to create the server pack with.
     * @param serverIconPath            Path to the icon to use in the server pack.
     * @param serverPropertiesPath      Path to the server.properties to create the server pack with.
     * @param includeServerInstallation Whether to install the modloader server in the server pack.
     * @param includeServerIcon         Whether to include the server-icon.png in the server pack.
     * @param includeServerProperties   Whether to include the server.properties in the server pack.
     * @param includeZipCreation        Whether to create a ZIP-archive of the server pack.
     * @param scriptSettings            Map containing key-value pairs to be used in start script creation.
     * @param pluginsConfigs             Configuration for any and all plugins used by this configuration.
     * @author Griefed
     */
    actual constructor(
        clientMods: List<String>,
        copyDirs: List<String>,
        modpackDir: String,
        minecraftVersion: String,
        modLoader: String,
        modLoaderVersion: String,
        javaArgs: String,
        serverPackSuffix: String,
        serverIconPath: String,
        serverPropertiesPath: String,
        includeServerInstallation: Boolean,
        includeServerIcon: Boolean,
        includeServerProperties: Boolean,
        includeZipCreation: Boolean,
        scriptSettings: HashMap<String, String>,
        pluginsConfigs: HashMap<String, ArrayList<CommentedConfig>>
    ) : this() {
        this.clientMods.addAll(clientMods)
        this.copyDirs.addAll(copyDirs)
        this.modpackDir = modpackDir
        this.minecraftVersion = minecraftVersion
        this.modloader = modLoader
        this.modloaderVersion = modLoaderVersion
        this.javaArgs = javaArgs
        this.serverPackSuffix = serverPackSuffix
        this.serverIconPath = serverIconPath
        this.serverPropertiesPath = serverPropertiesPath
        isServerInstallationDesired = includeServerInstallation
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
        setClientMods(config.getOrElse("clientMods", listOf("")) as ArrayList<String>)
        setCopyDirs(config.getOrElse("copyDirs", listOf("")) as ArrayList<String>)
        modpackDir = config.getOrElse("modpackDir", "")
        minecraftVersion = config.getOrElse("minecraftVersion", "")
        modloader = config.getOrElse("modLoader", "")
        modloaderVersion = config.getOrElse("modLoaderVersion", "")
        javaArgs = config.getOrElse("javaArgs", "")
        serverPackSuffix = utilities.stringUtilities
            .pathSecureText(config.getOrElse("serverPackSuffix", ""))
        serverIconPath = config.getOrElse("serverIconPath", "")
        serverPropertiesPath = config.getOrElse("serverPropertiesPath", "")
        isServerInstallationDesired = config.getOrElse("includeServerInstallation", false)
        isServerIconInclusionDesired = config.getOrElse("includeServerIcon", false)
        isServerPropertiesInclusionDesired = config.getOrElse("includeServerProperties", false)
        isZipCreationDesired = config.getOrElse("includeZipCreation", false)
        try {
            for ((key, value) in (config.get<Any>("plugins") as CommentedConfig).valueMap()) {
                pluginsConfigs[key] = value as ArrayList<CommentedConfig>
            }
        } catch (ignored: Exception) {
        }
        try {
            for ((key, value) in (config.get<Any>("scripts") as CommentedConfig).valueMap()) {
                scriptSettings[key] = value.toString()
            }
        } catch (ignored: Exception) {
        }
        if (!scriptSettings.containsKey("SPC_JAVA_SPC")) {
            scriptSettings["SPC_JAVA_SPC"] = "java"
        }
        config.close()
    }

    actual override fun save(destination: File): PackConfig {
        val conf = TomlFormat.instance().createConfig()
        conf.set<Any>(
            "includeServerInstallation",
            isServerInstallationDesired
        )
        conf.setComment(
            "includeServerInstallation",
            " Whether to install a Forge/Fabric/Quilt server for the serverpack. Must be true or false.\n Default value is true."
        )
        conf.setComment(
            "serverIconPath",
            "\n Path to a custom server-icon.png-file to include in the server pack."
        )
        conf.set<Any>("serverIconPath", serverIconPath)
        conf.setComment(
            "copyDirs",
            "\n Name of directories or files to include in serverpack.\n When specifying \"saves/world_name\", \"world_name\" will be copied to the base directory of the serverpack\n for immediate use with the server. Automatically set when projectID,fileID for modpackDir has been specified.\n Example: [config,mods,scripts]"
        )
        conf.set<Any>("copyDirs", copyDirs)
        conf.setComment(
            "serverPackSuffix",
            "\n Suffix to append to the server pack to be generated. Can be left blank/empty."
        )
        conf.set<Any>("serverPackSuffix", serverPackSuffix)
        @Suppress("SpellCheckingInspection")
        conf.setComment(
            "clientMods",
            "\n List of client-only mods to delete from serverpack.\n No need to include version specifics. Must be the filenames of the mods, not their project names on CurseForge!\n Example: [AmbientSounds-,ClientTweaks-,PackMenu-,BetterAdvancement-,jeiintegration-]"
        )
        conf.set<Any>("clientMods", clientMods)
        conf.setComment(
            "serverPropertiesPath",
            "\n Path to a custom server.properties-file to include in the server pack."
        )
        conf.set<Any>("serverPropertiesPath", serverPropertiesPath)
        conf.setComment(
            "includeServerProperties",
            "\n Include a server.properties in your serverpack. Must be true or false.\n If no server.properties is provided but is set to true, a default one will be provided.\n Default value is true."
        )
        conf.set<Any>("includeServerProperties", isServerPropertiesInclusionDesired)
        conf.setComment(
            "javaArgs",
            "\n Java arguments to set in the start-scripts for the generated server pack. Default value is \"empty\".\n Leave as \"empty\" to not have Java arguments in your start-scripts."
        )
        conf.set<Any>("javaArgs", javaArgs)
        conf.setComment(
            "modpackDir",
            "\n Path to your modpack. Can be either relative or absolute.\n Example: \"./Some Modpack\" or \"C:/Minecraft/Some Modpack\""
        )
        conf.set<Any>("modpackDir", modpackDir)
        conf.setComment(
            "includeServerIcon",
            "\n Include a server-icon.png in your serverpack. Must be true or false\n Default value is true."
        )
        conf.set<Any>("includeServerIcon", isServerIconInclusionDesired)
        conf.setComment(
            "includeZipCreation",
            "\n Create zip-archive of serverpack. Must be true or false.\n Default value is true."
        )
        conf.set<Any>("includeZipCreation", isZipCreationDesired)
        conf.setComment(
            "modLoaderVersion",
            "\n The version of the modloader you want to install. Example for Fabric=\"0.7.3\", example for Forge=\"36.0.15\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true."
        )
        conf.set<Any>("modLoaderVersion", modloaderVersion)
        conf.setComment(
            "minecraftVersion",
            "\n Which Minecraft version to use. Example: \"1.16.5\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true."
        )
        conf.set<Any>("minecraftVersion", minecraftVersion)
        conf.setComment(
            "modLoader",
            "\n Which modloader to install. Must be either \"Forge\", \"Fabric\", \"Quilt\" or \"LegacyFabric\".\n Automatically set when projectID,fileID for modpackDir has been specified.\n Only needed if includeServerInstallation is true."
        )
        conf.set<Any>("modLoader", modloader)
        val plugins: Config = TomlFormat.newConfig()
        plugins.valueMap().putAll(pluginsConfigs)
        conf.setComment(
            "plugins",
            "\n Configurations for any and all plugins installed and used by this configuration."
        )
        conf.setComment("plugins", " Settings related to plugins. A plugin is identified by its ID.")
        conf.set<Any>("plugins", plugins)
        val scripts: Config = TomlFormat.newConfig()
        for ((key, value) in scriptSettings) {
            if (key != "SPC_SERVERPACKCREATOR_VERSION_SPC" && key != "SPC_MINECRAFT_VERSION_SPC"
                && key != "SPC_MODLOADER_SPC" && key != "SPC_MODLOADER_VERSION_SPC"
                && key != "SPC_JAVA_ARGS_SPC" && key != "SPC_FABRIC_INSTALLER_VERSION_SPC"
                && key != "SPC_QUILT_INSTALLER_VERSION_SPC" && key != "SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"
                && key != "SPC_MINECRAFT_SERVER_URL_SPC"
            ) {
                scripts.set<Any>(key, value)
            }
        }
        conf.setComment(
            "scripts",
            "\n Key-value pairs for start scripts. A given key in a start script is replaced with the value."
        )
        conf.add("scripts", scripts)
        TomlFormat.instance().createWriter()
            .write(conf, destination, WritingMode.REPLACE, StandardCharsets.UTF_8)
        return this
    }
}