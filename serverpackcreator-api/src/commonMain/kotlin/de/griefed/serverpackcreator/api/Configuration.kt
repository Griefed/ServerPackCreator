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
@file:Suppress("unused")

package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.File
import mu.KotlinLogging

/**
 * Base for the [de.griefed.serverpackcreator.api.ConfigurationHandler] to ensure we have the basics for config-checks
 * and config-handling.
 *
 * @author Griefed
 */
abstract class Configuration<F, P> {
    protected val log = KotlinLogging.logger {}
    protected val forge = "^forge$".toRegex()
    protected val neoForge = "^neoforge$".toRegex()
    protected val fabric = "^fabric$".toRegex()
    protected val quilt = "^quilt$".toRegex()
    protected val legacyFabric = "^legacyfabric$".toRegex()
    protected val whitespace = "^\\s+$".toRegex()
    protected val previous = ".*_\\d".toRegex()
    protected val zipCheck = "^\\w+[/\\\\]$".toRegex()

    /**
     * Check the passed configuration-file. If any check returns `true` then the server pack
     * will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     *
     * @param configFile         The configuration file to check. Must either be an existing file to
     * load a configuration from or null if you want to use the passed
     * configuration model.
     * @param packConfig Instance of a configuration of a modpack. Can be used to further
     * display or use any information within, as it may be changed or
     * otherwise altered by this method.
     * @param configCheck Contains all encountered errors during the check of the passed configuration.
     * @param quietCheck         Whether the configuration should be printed to the console and logs.
     * Pass false to quietly check the configuration.
     * @return `false` if the configuration has passed all tests.
     * @author Griefed
     */
    abstract fun checkConfiguration(
        configFile: F,
        packConfig: PackConfig,
        configCheck: ConfigCheck,
        quietCheck: Boolean
    ): ConfigCheck

    /**
     * Check the passed [packConfig]. If any check returns `true` then the server
     * pack will not be created. In order to find out which check failed, the user has to check their
     * serverpackcreator.log in the logs-directory.
     *
     * The passed [packConfig] can be used to further display or use any information within, as it may be changed or otherwise
     * altered by this method.
     *
     * @param packConfig Instance of a configuration of a modpack. Can be used to further
     * display or use any information within, as it may be changed or
     * otherwise altered by this method.
     * @param configCheck Contains all encountered errors during the check of the passed configuration.
     * @param quietCheck         Whether the configuration should be printed to the console and logs.
     * Pass false to quietly check the configuration.
     * @return `false` if all checks are passed.
     * @author Griefed
     */
    abstract fun checkConfiguration(
        packConfig: PackConfig,
        configCheck: ConfigCheck,
        quietCheck: Boolean
    ): ConfigCheck

    /**
     * Sanitize any and all links in a given instance of [PackConfig] modpack-directory,
     * server-icon path, server-properties path, Java path and copy-directories entries.
     *
     * @param packConfig Instance of [PackConfig] in which to sanitize links to
     * their respective destinations.
     * @author Griefed
     */
    abstract fun sanitizeLinks(packConfig: PackConfig)

    /**
     * Checks the passed String whether it is an existing file. If the passed String is empty, then
     * ServerPackCreator will treat it as the user being fine with the default files and return the
     * corresponding boolean.
     *
     * @param iconOrPropertiesPath The path to the custom server-icon.png or server.properties file to
     * check.
     * @return `true` if the file exists or an empty String was passed, false if a file was
     * specified, but the file was not found.
     * @author Griefed
     */
    abstract fun checkIconAndProperties(iconOrPropertiesPath: String): Boolean

    /**
     * If the in the configuration specified modpack dir is an existing directory, checks are made for
     * valid configuration of: directories to copy to server pack, if includeServerInstallation is
     * `true` path to Java executable/binary, Minecraft version, modloader and modloader
     * version.
     *
     * @param packConfig An instance of [PackConfig] which contains the
     * configuration of the modpack.
     * @param configCheck Contains all encountered errors during the check of the passed configuration.
     * @return `true` if an error is found during configuration check.
     * @author Griefed
     */
    abstract fun isDir(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck

    /**
     * Checks the specified ZIP-archive for validity. In order for a modpack ZIP-archive to be
     * considered valid, it needs to contain the `mods` and `config` folders at minimum.
     * If any of `manifest.json`, `minecraftinstance.json` or `config.json` are
     * available, gather as much information from them as possible.
     *
     * @param packConfig Instance of [PackConfig] with a server pack
     * configuration.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return `false` when no errors were encountered.
     * @author Griefed
     */
    abstract fun isZip(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck

    /**
     * Checks whether either Forge or Fabric were specified as the modloader.
     *
     * @param modloader Check as case-insensitive for Forge or Fabric.
     * @return `true` if the specified modloader is either Forge or Fabric. False if neither.
     * @author Griefed
     */
    abstract fun checkModloader(modloader: String, configCheck: ConfigCheck): ConfigCheck

    /**
     * Check the given Minecraft and modloader versions for the specified modloader.
     *
     * @param modloader        The passed modloader which determines whether the check for Forge or
     * Fabric is called.
     * @param modloaderVersion The version of the modloader which is checked against the corresponding
     * modloader's manifest.
     * @param minecraftVersion The version of Minecraft used for checking the Forge version.
     * @return `true` if the specified modloader version was found in the corresponding
     * manifest.
     * @author Griefed
     */
    abstract fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, configCheck: ConfigCheck
    ): ConfigCheck

    /**
     * Convenience method which passes the important fields from an instance of
     * [PackConfig] to
     * [.printConfigurationModel]
     *
     * @param packConfig Instance of [PackConfig] to print to console and logs.
     * @author Griefed
     */
    fun printConfigurationModel(packConfig: Pack<*, *, *>) = printConfigurationModel(
        packConfig.modpackDir,
        packConfig.clientMods,
        packConfig.inclusions,
        packConfig.minecraftVersion,
        packConfig.modloader,
        packConfig.modloaderVersion,
        packConfig.isServerIconInclusionDesired,
        packConfig.isServerPropertiesInclusionDesired,
        packConfig.isZipCreationDesired,
        packConfig.javaArgs,
        packConfig.serverPackSuffix,
        packConfig.serverIconPath,
        packConfig.serverPropertiesPath,
        packConfig.scriptSettings
    )

    /**
     * Print all encountered errors to logs.
     *
     * @param encounteredErrors A list of all errors which were encountered during a configuration
     * check.
     * @author Griefed
     */
    fun printEncounteredErrors(encounteredErrors: List<String>) {
        log.error("Encountered ${encounteredErrors.size} errors during the configuration check.")
        var encounteredErrorNumber: Int
        for (i in encounteredErrors.indices) {
            encounteredErrorNumber = i + 1
            log.error("Error $encounteredErrorNumber: ${encounteredErrors[i]}")
        }
    }

    /**
     * Update the script settings and ensure the default keys, with values gathered from the passed
     * [PackConfig], are present:
     *
     *
     *  1. `SPC_SERVERPACKCREATOR_VERSION_SPC` : `ServerPackCreator version with which the scripts were created`
     *  1. `SPC_MINECRAFT_VERSION_SPC` : `Minecraft version of the modpack`
     *  1. `SPC_MINECRAFT_SERVER_URL_SPC` : `Download-URL to the Minecraft server
    ` *
     *  1. `SPC_MODLOADER_SPC` : `The modloader of the modpack`
     *  1. `SPC_MODLOADER_VERSION_SPC` : `The modloader version of the modpack
    ` *
     *  1. `SPC_JAVA_ARGS_SPC` : `The JVM args to be used to run the server`
     *  1. `SPC_JAVA_SPC` : `Path to the java installation to be used to run the server`
     *  1. `SPC_FABRIC_INSTALLER_VERSION_SPC` : `Most recent version of the Fabric installer at the time of creating the scripts`
     *  1. `SPC_QUILT_INSTALLER_VERSION_SPC` : `Most recent version of the Quilt installer at the time of creating the scripts`
     *
     *
     * @param packConfig Model in which to ensure the default key-value pairs are present.
     * @author Griefed
     */
    abstract fun ensureScriptSettingsDefaults(packConfig: PackConfig)

    /**
     * Checks whether the passed list of directories which are supposed to be in the modpack directory
     * is empty, or whether all directories in the list exist in the modpack directory. If the user
     * specified a `source/file;destination/file`-combination, it is checked whether the
     * specified source-file exists on the host.
     *
     * @param inclusions Directories, or `source/file;destination/file`-combinations, to
     * check for existence.
     * `source/file;destination/file`-combinations must be absolute
     * paths to the source-file.
     * @param modpackDir        Path to the modpack directory in which to check for existence of the
     * passed list of directories.
     * @return `true` if every directory was found in the modpack directory. If any single one
     * was not found, false is returned.
     * @author Griefed
     */
    abstract fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        configCheck: ConfigCheck,
        printLog: Boolean
    ): ConfigCheck

    /**
     * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it
     * contains neither the mods nor the config directories, consider it invalid.
     *
     * @param pathToZip         Path to the ZIP-file to check.
     * @author Griefed
     */
    abstract fun checkZipArchive(pathToZip: String, configCheck: ConfigCheck): ConfigCheck

    /**
     * Update the destination to which the ZIP-archive will the extracted to, based on whether a
     * directory of the same name already exists.
     *
     * @param destination The destination to where the ZIP-archive was about to be extracted to.
     * @return The destination where the ZIP-archive will be extracted to.
     * @author Griefed
     */
    abstract fun unzipDestination(destination: String): String

    /**
     * Creates a list of suggested directories to include in server pack which is later on written to
     * a new configuration file. The list of directories to include in the server pack which is
     * generated by this method excludes well know directories which would not be needed by a server
     * pack. If you have suggestions to this list, open a feature request issue on [GitHub](https://github.com/Griefed/ServerPackCreator/issues/new/choose)
     *
     * @param modpackDir The directory for which to gather a list of directories to copy to the server
     * pack.
     * @return Directories inside the modpack, excluding well known client-side only directories.
     * @author Griefed
     */
    abstract fun suggestInclusions(modpackDir: String): ArrayList<InclusionSpecification>

    /**
     * Check whether various manifests from various launchers exist and use them to update our
     * ConfigurationModel and pack name.
     *
     * @param destination        The destination in which the manifests are.
     * @param packConfig The ConfigurationModel to update.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return The name of the modpack currently being checked. `null` if the name could not be
     * acquired.
     * @author Griefed
     */
    abstract fun checkManifests(
        destination: String,
        packConfig: PackConfig,
        configCheck: ConfigCheck
    ): String?

    /**
     * Check whether a server pack for the given destination already exists and get an incrementor
     * based on whether one exists, how many, or none exist. Think if this as the incrementation
     * Windows does when a file of the same name is copied. `foo.bar` becomes
     * `foo (1).bar` etc.
     *
     * @param source          The name of the modpack.
     * @param destination The name of the server pack about to be generated.
     * @return An incremented number, based on whether a server pack of the same name already exists.
     * @author Griefed
     */
    abstract fun checkServerPacksForIncrement(source: String, destination: String): String

    /**
     * Prints all passed fields to the console and serverpackcreator.log. Used to show the user the
     * configuration before ServerPackCreator starts the generation of the server pack or, if checks
     * failed, to show the user their last configuration, so they can more easily identify problems
     * with said configuration.
     *
     * Should a user report an issue on GitHub and include their logs (which I hope they do....), this would also help
     * me help them. Logging is good. People should use more logging.
     *
     * @param modpackDirectory     The used modpackDir field either from a configuration file or from
     * configuration setup.
     * @param clientsideMods       List of clientside-only mods to exclude from the server pack...
     * @param inclusions      List of directories in the modpack which are to be included in the
     * server pack.
     * @param minecraftVer         The Minecraft version the modpack uses.
     * @param modloader            The modloader the modpack uses.
     * @param modloaderVersion     The version of the modloader the modpack uses.
     * @param includeIcon          Whether to include the server-icon.png in the server pack.
     * @param includeProperties    Whether to include the server.properties in the server pack.
     * @param includeZip           Whether to create a zip-archive of the server pack, excluding the
     * Minecraft server JAR according to Mojang's TOS and EULA.
     * @param javaArgs             Java arguments to write the start-scripts with.
     * @param serverPackSuffix     Suffix to append to name of the server pack to be generated.
     * @param serverIconPath       The path to the custom server-icon.png to be used in the server
     * pack.
     * @param serverPropertiesPath The path to the custom server.properties to be used in the server
     * pack.
     * @param scriptSettings       Custom settings for start script creation. `KEY`s are the
     * placeholder, `VALUE`s are the values with which the
     * placeholders are to be replaced.
     * @author Griefed
     */
    abstract fun printConfigurationModel(
        modpackDirectory: String,
        clientsideMods: List<String>,
        inclusions: List<InclusionSpecification>,
        minecraftVer: String,
        modloader: String,
        modloaderVersion: String,
        includeIcon: Boolean,
        includeProperties: Boolean,
        includeZip: Boolean,
        javaArgs: String,
        serverPackSuffix: String,
        serverIconPath: String,
        serverPropertiesPath: String,
        scriptSettings: HashMap<String, String>
    )

    /**
     * Acquire a list of directories in the base-directory of a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the base-directory of the ZIP-file.
     * @author Griefed
     */
    abstract fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String>

    /**
     * **`manifest.json`**
     *
     * Update the given ConfigurationModel with values gathered from the downloaded CurseForge
     * modpack. A manifest.json-file is usually created when a modpack is exported through launchers
     * like Overwolf's CurseForge or GDLauncher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param manifest           The CurseForge manifest.json-file of the modpack to read.
     * @author Griefed
     */
    abstract fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: F)

    /**
     * Acquire the modpacks name from the JSON previously acquired and stored in the
     * ConfigurationModel.
     *
     * @param packConfig The ConfigurationModel containing the JsonNode from which to acquire
     * the modpacks name.
     * @param childNodes         The child nodes, in order, which contain the requested packname.
     * @return The new name of the modpack.
     * @author Griefed
     */
    abstract fun updatePackName(packConfig: PackConfig, vararg childNodes: String): String?

    /**
     * **`minecraftinstance.json`**
     *
     * Update the given ConfigurationModel with values gathered from the minecraftinstance.json of
     * the modpack. A minecraftinstance.json is usually created by Overwolf's CurseForge launcher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param minecraftInstance  The minecraftinstance.json-file of the modpack to read.
     * @author Griefed
     */
    abstract fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: F)

    /**
     * **`modrinth.index.json`**
     *
     * Update the given ConfigurationModel with values gathered from a Modrinth `modrinth.index.json`-manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    abstract fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: F)

    /**
     * **`instance.json`**
     *
     * Update the given ConfigurationModel with values gathered from a ATLauncher manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    abstract fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: F)

    /**
     * **`config.json`**
     *
     * Update the given ConfigurationModel with values gathered from the modpacks config.json. A
     * config.json is usually created by GDLauncher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param config             The config.json-file of the modpack to read.
     * @author Griefed
     */
    abstract fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: F)

    /**
     * **`mmc-pack.json`**
     *
     *
     * Update the given ConfigurationModel with values gathered from the modpacks mmc-pack.json. A
     * mmc-pack.json is usually created by the MultiMC launcher.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param mmcPack            The config.json-file of the modpack to read.
     * @author Griefed
     */
    abstract fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: F)

    /**
     * **`instance.cfg`**
     *
     * Acquire the name of the modpack/instance of a MultiMC modpack from the modpacks
     * instance.cfg, which is usually created by the MultiMC launcher.
     *
     * @param instanceCfg The config.json-file of the modpack to read.
     * @return The instance name.
     * @author Griefed
     */
    abstract fun updateDestinationFromInstanceCfg(instanceCfg: F): String

    /**
     * Ensures the modloader is normalized to first letter upper case and rest lower case. Basically
     * allows the user to input Forge or Fabric in any combination of upper- and lowercase and
     * ServerPackCreator will still be able to work with the users input.
     *
     * @param modloader Modloader String-representation to normalize.
     * @return A normalized String of the specified modloader.
     * @author Griefed
     */
    fun getModLoaderCase(modloader: String) = when {
        modloader.lowercase().matches(forge) || modloader.lowercase().contains("forge") &&
                !(modloader.lowercase().matches(neoForge) || modloader.lowercase().contains("NeoForge"))-> {
            "Forge"
        }
        modloader.lowercase().matches(fabric) || modloader.lowercase().contains("fabric") -> {
            "Fabric"
        }
        modloader.lowercase().matches(quilt) || modloader.lowercase().contains("quilt") -> {
            "Quilt"
        }
        modloader.lowercase().matches(legacyFabric) || modloader.lowercase().contains("legacyfabric") -> {
            "LegacyFabric"
        }
        modloader.lowercase().matches(neoForge) || modloader.lowercase().contains("NeoForge") -> {
            "NeoForge"
        }
        else -> {
            log.warn { "No suitable modloader found. Defaulting to Forge." }
            "Forge"
        }
    }

    /**
     * Check the passed directory for existence and whether it is a directory, rather than a file.
     *
     * @param modpackDir The modpack directory.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return `true` if the directory exists.
     * @author Griefed
     */
    abstract fun checkModpackDir(
        modpackDir: String,
        configCheck: ConfigCheck,
        printLog: Boolean
    ): ConfigCheck

    /**
     * Acquire a list of all files and directories in a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files and directories in the ZIP-file.
     * @author Griefed
     */
    abstract fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String>

    /**
     * Acquire a list of all directories in a ZIP-file. The resulting list excludes files.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the ZIP-file.
     * @author Griefed
     */
    abstract fun getDirectoriesInModpackZip(zipFile: File): List<String>

    /**
     * Acquire a list of all files in a ZIP-file. The resulting list excludes directories.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files in the ZIP-file.
     * @author Griefed
     */
    abstract fun getFilesInModpackZip(zipFile: File): List<String>
}