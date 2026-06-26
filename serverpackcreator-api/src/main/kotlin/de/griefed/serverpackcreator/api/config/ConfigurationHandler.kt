/* Copyright (C) 2025 Griefed
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

import Translations
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.SPCConfigCheckListener
import de.griefed.serverpackcreator.api.utilities.SPCGenericListener
import de.griefed.serverpackcreator.api.utilities.SecurityScans
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * Check any given [PackConfig] for errors and, if so desired, add them to a passed
 * list of errors, so you may display them in a GUI, CLI or website. The most important method is
 * [checkConfiguration] and all of its variants which will check
 * your passed configuration model for errors, indicating whether it is safe to use for further
 * operations. Running your model through the checks also ensures that the default script settings
 * are present and set according to your pack's environment.
 *
 * @param apiProperties Base settings of SPC used just about everywhere.
 * @param versionMeta   Meta used for Minecraft and modloader version checks and verification.
 * @param utilities     Common utilities used all across SPC.
 * @param apiPlugins    Addons and extensions added by external addons which can add additional checks to a given
 * configuration check.
 *
 * @author Griefed
 */
class ConfigurationHandler(
    private val versionMeta: VersionMeta,
    private val apiProperties: ApiProperties,
    private val utilities: Utilities,
    private val apiPlugins: ApiPlugins
) {
    private val zipRegex = "\\.[Zz][Ii][Pp]".toRegex()
    val log by lazy { cachedLoggerOf(this.javaClass) }
    val forge = SupportedModloaders.forge
    val neoForge = SupportedModloaders.neoForge
    val fabric = SupportedModloaders.fabric
    val quilt = SupportedModloaders.quilt
    val legacyFabric = SupportedModloaders.legacyFabric
    val whitespace = "^\\s+$".toRegex()
    val previous = ".*_\\d".toRegex()
    val zipCheck = "^\\w+[/\\\\]$".toRegex()

    /**
     * Validator for modloader-names and modloader-versions.
     */
    val modloaderValidator = ModloaderValidator(versionMeta)

    /**
     * Validator for inclusion-specifications.
     */
    val inclusionsValidator = InclusionsValidator()

    /**
     * Validator for the modpack-directory.
     */
    val modpackDirectoryValidator = ModpackDirectoryValidator()

    /**
     * Inspector for modpack ZIP-archives: content-listing and validity-checks.
     */
    val zipInspector = ModpackZipInspector()

    /**
     * Parser deriving PackConfig-values from the manifests of various launchers.
     */
    val manifestParser = ModpackManifestParser(apiProperties, utilities)

    private val spcGenericEventListeners: ArrayList<SPCGenericListener> = ArrayList(0)
    private val spcConfigEventListeners: ArrayList<SPCConfigCheckListener> = ArrayList(0)

    fun addEventListener(genericEventListener: SPCGenericListener) {
        spcGenericEventListeners.add(genericEventListener)
    }

    fun addEventListener(configEventListener: SPCConfigCheckListener) {
        spcConfigEventListeners.add(configEventListener)
    }

    private fun runEventListeners(packConfig: PackConfig,configCheck: ConfigCheck = ConfigCheck()) {
        for (listener in spcGenericEventListeners) {
            listener.run()
        }
        for (listener in spcConfigEventListeners) {
            listener.run(packConfig, configCheck)
        }
    }

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
    fun checkConfiguration(configFile: File, packConfig: PackConfig = PackConfig(), configCheck: ConfigCheck = ConfigCheck(), quietCheck: Boolean = false): ConfigCheck {
        try {
            val fileConf = PackConfig(configFile)
            packConfig.setClientMods(fileConf.clientMods)
            packConfig.setInclusions(fileConf.inclusions)
            packConfig.setModsWhitelist(fileConf.modsWhitelist)
            packConfig.modpackDir = fileConf.modpackDir
            packConfig.minecraftVersion = fileConf.minecraftVersion
            packConfig.modloader = fileConf.modloader
            packConfig.modloaderVersion = fileConf.modloaderVersion
            packConfig.javaArgs = fileConf.javaArgs
            packConfig.serverPackSuffix = fileConf.serverPackSuffix
            packConfig.serverIconPath = fileConf.serverIconPath
            packConfig.serverPropertiesPath = fileConf.serverPropertiesPath
            packConfig.isServerIconInclusionDesired = fileConf.isServerIconInclusionDesired
            packConfig.isServerPropertiesInclusionDesired = fileConf.isServerPropertiesInclusionDesired
            packConfig.isZipCreationDesired = fileConf.isZipCreationDesired
            packConfig.setScriptSettings(fileConf.scriptSettings)
            packConfig.setPluginsConfigs(fileConf.pluginsConfigs)
            return checkConfiguration(packConfig, configCheck, quietCheck)
        } catch (ex: Exception) {
            log.error(
                "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\"."
            )

            
            configCheck.configErrors.add(Translations.configuration_log_error_checkconfig_start.toString())
            return configCheck
        }
    }

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
    fun checkConfiguration(packConfig: PackConfig, configCheck: ConfigCheck = ConfigCheck(), quietCheck: Boolean = false): ConfigCheck {
        sanitizeLinks(packConfig)
        log.info("Checking configuration...")
        if (packConfig.clientMods.isEmpty()) {
            log.warn("No clientside-only mods specified. Using fallback list.")
            packConfig.setClientMods(apiProperties.clientSideMods().toMutableList())
        }
        if (packConfig.modsWhitelist.isEmpty()) {
            log.warn("No whitelist mods specified. Using fallback list.")
            packConfig.setModsWhitelist(apiProperties.whitelistedMods().toMutableList())
        }

        val modpack = File(packConfig.modpackDir)
        log.info("Performing security scans")
        log.info("Performing Nekodetector scan")
        if (modpack.isDirectory) {
            configCheck.otherErrors.addAll(SecurityScans.scanUsingNekodetector(modpack.toPath()))
        }

        if (!checkIconAndProperties(packConfig.serverIconPath)) {
            configCheck.serverIconErrors.add(Translations.configuration_log_error_servericon(packConfig.serverIconPath))
            log.error("The specified server-icon does not exist: ${packConfig.serverIconPath}")
            
        } else if (packConfig.serverIconPath.isNotEmpty()
            && File(packConfig.serverIconPath).exists()
            && !FileUtilities.isReadPermissionSet(packConfig.serverIconPath)
        ) {
            configCheck.serverIconErrors.add(Translations.configuration_log_error_checkcopydirs_read(packConfig.serverIconPath))
            @Suppress("LoggingSimilarMessage")
            log.error("No read-permission for ${packConfig.serverIconPath}")
        }
        if (!checkIconAndProperties(packConfig.serverPropertiesPath)) {
            configCheck.serverPropertiesErrors.add(Translations.configuration_log_error_serverproperties(packConfig.serverPropertiesPath))
            log.error("The specified server.properties does not exist: ${packConfig.serverPropertiesPath}")
        } else if (packConfig.serverPropertiesPath.isNotEmpty()
            && File(packConfig.serverPropertiesPath).exists()
            && !FileUtilities.isReadPermissionSet(packConfig.serverPropertiesPath)
        ) {
            configCheck.serverPropertiesErrors.add(Translations.configuration_log_error_checkcopydirs_read(packConfig.serverPropertiesPath))
            @Suppress("LoggingSimilarMessage")
            log.error("No read-permission for ${packConfig.serverPropertiesPath}")
        }

        if (modpack.isDirectory) {
            isDir(packConfig, configCheck)
        } else if (modpack.isFile && modpack.name.endsWith("zip")) {
            packConfig.source = ModpackSource.ZIP
            try {
                isZip(packConfig, configCheck)
            } catch (ex: IOException) {
                configCheck.modpackErrors.add("An error occurred whilst working with the ZIP-archive.")
                log.error("An error occurred whilst working with the ZIP-archive.", ex)
            }
        } else {
            configCheck.modpackErrors.add(Translations.configuration_log_error_checkmodpackdir.toString())
            log.error("Modpack directory not specified. Please specify an existing directory. Specified: ${packConfig.modpackDir}")
        }

        if (checkModloader(packConfig.modloader, configCheck).modloaderChecksPassed) {
            log.debug("modLoader settings check passed.")
        } else {
            log.error("There's something wrong with your modloader or modloader version setting.")
        }
        if (checkModloaderVersion(packConfig.modloader,packConfig.modloaderVersion,packConfig.minecraftVersion,configCheck).modloaderVersionChecksPassed) {
            log.debug("modLoaderVersion setting check passed.")
        } else {
            log.error("There's something wrong with your modloader version setting.")
        }

        if (versionMeta.minecraft.isMinecraftVersionAvailable(packConfig.minecraftVersion)) {
            log.debug("minecraftversion settings check passed.")
        } else {
            configCheck.minecraftVersionErrors.add(Translations.configuration_log_error_minecraft.toString())
            log.error("There's something wrong with your Minecraft version setting.")
        }

        checkForProjectInformation(packConfig)

        apiPlugins.runConfigCheckExtensions(packConfig, configCheck)
        runEventListeners(packConfig, configCheck)

        if (quietCheck) {
            printConfigurationModel(packConfig)
        }
        if (configCheck.allChecksPassed) {
            log.info("Config check successful. No errors encountered.")
        } else {
            log.error("Config check not successful. Check your config for errors.")
            printEncounteredErrors(configCheck.encounteredErrors)
        }
        ensureScriptSettingsDefaults(packConfig)
        return configCheck
    }

    /**
     * Check for minecraftinstance.json and profile.json and if either is present, try to obtain the project- and
     * fileIDs as well as the modpack distribution-platform (Modrinth or CurseForge).
     *
     * @author Griefed
     */
    fun checkForProjectInformation(packConfig: PackConfig) {
        val modpackDirectory = File(packConfig.modpackDir)
        if (!modpackDirectory.isDirectory) {
            log.info("Modpack is not a directory. Skipping project information gathering.")
            return
        }

        val subConfig = PackConfig()
        var name: String? = null

        try {
            name = checkManifests(packConfig.modpackDir, subConfig)
        } catch (ex: NullPointerException) {
            log.error("Could not retrieve project and/or file IDs.", ex)
        }

        packConfig.projectID = subConfig.projectID
        packConfig.versionID = subConfig.versionID
        packConfig.source = subConfig.source
        @Suppress("IfThenToElvis")
        packConfig.name = if (subConfig.name != null) {
            subConfig.name
        } else if (name != null) {
            name
        } else {
            modpackDirectory.name
        }
    }

    /**
     * Checks whether a supported modloader was specified.
     */
    fun checkModloader(modloader: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck =
        modloaderValidator.checkModloader(modloader, configCheck)

    /**
     * Sanitize any and all links in a given instance of [PackConfig] modpack-directory,
     * server-icon path, server-properties path, Java path and copy-directories entries.
     *
     * @param packConfig Instance of [PackConfig] in which to sanitize links to
     * their respective destinations.
     * @author Griefed
     */
    fun sanitizeLinks(packConfig: PackConfig) {
        log.info("Checking configuration for links...")
        if (packConfig.modpackDir.isNotEmpty() && FileUtilities.isLink(packConfig.modpackDir)) {
            try {
                packConfig.modpackDir = FileUtilities.resolveLink(packConfig.modpackDir)
                log.info("Resolved modpack directory link to: ${packConfig.modpackDir}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for modpack directory.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for modpack directory.", ex)
            }
        }
        if (packConfig.serverIconPath.isNotEmpty() && FileUtilities.isLink(packConfig.serverIconPath)) {
            try {
                packConfig.serverIconPath = FileUtilities.resolveLink(packConfig.serverIconPath)
                log.info("Resolved server-icon link to: ${packConfig.serverIconPath}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for server-icon.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for server-icon.", ex)
            }
        }
        if (packConfig.serverPropertiesPath.isNotEmpty() && FileUtilities.isLink(packConfig.serverPropertiesPath)) {
            try {
                packConfig.serverPropertiesPath = FileUtilities.resolveLink(packConfig.serverPropertiesPath)
                log.info("Resolved server-properties link to: ${packConfig.serverPropertiesPath}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for server-properties.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for server-properties.", ex)
            }
        }
        if (packConfig.inclusions.isNotEmpty()) {
            val copyDirs: ArrayList<InclusionSpecification> = packConfig.inclusions
            var inclusionChanges = false
            var entry: InclusionSpecification
            var link: String
            for (inclusion in packConfig.inclusions.indices) {
                entry = packConfig.inclusions[inclusion]

                if (!entry.source.startsWith(packConfig.modpackDir) && FileUtilities.isLink(entry.source)) {
                    link = FileUtilities.resolveLink(entry.source)
                    entry.source = link
                    inclusionChanges = true
                    log.info("Resolved source to $link.")
                } else if (FileUtilities.isLink(packConfig.modpackDir + File.separator + entry.source)) {
                    link = FileUtilities.resolveLink("${packConfig.modpackDir}${File.separator}${entry.source}")
                    entry.source = link
                    inclusionChanges = true
                    log.info("Resolved copy-directories link to: $link")
                }
            }
            if (inclusionChanges) {
                packConfig.setInclusions(copyDirs)
            }
        }
    }

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
    fun checkIconAndProperties(iconOrPropertiesPath: String) = if (iconOrPropertiesPath.isEmpty()) {
        true
    } else {
        File(iconOrPropertiesPath).isFile
    }

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
    fun isDir(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck {
        if (checkInclusions(packConfig.inclusions, packConfig.modpackDir, configCheck).inclusionsChecksPassed) {
            log.debug("copyDirs setting check passed.")
        } else {
            log.error("There's something wrong with your setting of directories to include in your server pack.")
            configCheck.inclusionErrors.add(Translations.configuration_log_error_isdir_copydir.toString())
        }
        return configCheck
    }

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
    @Throws(IOException::class)
    fun isZip(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck {
        // modpackDir points at a ZIP-file. Get the path to the would be modpack directory.
        val name = File(packConfig.modpackDir).name
        val cleaned = name.replace(zipRegex, "")
        val unzippedModpack = "${apiProperties.modpacksDirectory}${File.separator}$cleaned"
        if (!checkZipArchive(Paths.get(packConfig.modpackDir).toAbsolutePath().toString(), configCheck).modpackChecksPassed) {
            return configCheck
        }

        // Does the modpack extracted from the ZIP-archive already exist?
        //unzippedModpack = unzipDestination(unzippedModpack)

        // Extract the archive to the modpack directory.
        FileUtilities.unzipArchive(packConfig.modpackDir, unzippedModpack)
        packConfig.modpackDir = unzippedModpack

        // Expand the already set copyDirs with suggestions from extracted ZIP-archive.
        val newCopyDirs = suggestInclusions(unzippedModpack)
        for (entry in packConfig.inclusions) {
            if (!newCopyDirs.contains(entry)) {
                newCopyDirs.add(entry)
            }
        }
        packConfig.setInclusions(newCopyDirs)

        // If various manifests exist, gather as much information as possible.
        // Check CurseForge manifest available if a modpack was exported through a client like
        // Overwolf's CurseForge or through GDLauncher.
        val amountOfErrors = configCheck.modpackErrors.size

        var packName = checkManifests(unzippedModpack, packConfig, configCheck)
        if (configCheck.modpackErrors.size > amountOfErrors) {
            configCheck.modpackErrors.add(Translations.configuration_log_error_zip_manifests.toString())
        }

        // If no json was read from the modpack, we must sadly use the ZIP-files name as the new
        // destination. Sad-face.
        if (packName == null) {
            packName = unzippedModpack
        }
        packName = File(StringUtilities.pathSecureTextAlternative(packName)).path

        // Does the modpack contain a server-icon or server.properties? If so, include
        // them in the server pack.
        var file = File(packName, "server-icon.png")
        if (file.exists()) {
            packConfig.serverIconPath = file.absolutePath
        }
        file = File(packName, "server.properties")
        if (file.exists()) {
            packConfig.serverPropertiesPath = file.absolutePath
        }
        return configCheck
    }

    /**
     * Check the given Minecraft- and modloader-versions for the specified modloader.
     */
    fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, configCheck: ConfigCheck = ConfigCheck()
    ): ConfigCheck = modloaderValidator.checkModloaderVersion(modloader, modloaderVersion, minecraftVersion, configCheck)

    /**
     * Convenience method which passes the important fields from an instance of
     * [PackConfig] to
     * [.printConfigurationModel]
     *
     * @param packConfig Instance of [PackConfig] to print to console and logs.
     * @author Griefed
     */
    fun printConfigurationModel(packConfig: PackConfig) = printConfigurationModel(
        packConfig.modpackDir,
        packConfig.clientMods,
        packConfig.modsWhitelist,
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
    fun ensureScriptSettingsDefaults(packConfig: PackConfig) {
        //From modpack -> server pack specific values
        packConfig.scriptSettings["SPC_SERVERPACKCREATOR_VERSION_SPC"] = apiProperties.apiVersion
        packConfig.scriptSettings["SPC_MINECRAFT_VERSION_SPC"] = packConfig.minecraftVersion
        packConfig.scriptSettings["SPC_MODLOADER_SPC"] = packConfig.modloader
        packConfig.scriptSettings["SPC_MODLOADER_VERSION_SPC"] = packConfig.modloaderVersion
        packConfig.scriptSettings["SPC_JAVA_ARGS_SPC"] = packConfig.javaArgs
        packConfig.scriptSettings["SPC_FABRIC_INSTALLER_VERSION_SPC"] = versionMeta.fabric.releaseInstaller()
        packConfig.scriptSettings["SPC_QUILT_INSTALLER_VERSION_SPC"] = versionMeta.quilt.releaseInstaller()
        packConfig.scriptSettings["SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"] = versionMeta.legacyFabric.releaseInstaller()

        if (!packConfig.scriptSettings.containsKey("SPC_RECOMMENDED_JAVA_VERSION_SPC")) {
            val server = versionMeta.minecraft.getServer(packConfig.minecraftVersion)
            if (server.isPresent && server.get().javaVersion().isPresent) {
                packConfig.scriptSettings["SPC_RECOMMENDED_JAVA_VERSION_SPC"] = server.get().javaVersion().get().toString()
            } else {
                packConfig.scriptSettings["SPC_RECOMMENDED_JAVA_VERSION_SPC"] = "?"
            }
        }

        // Make sure default values are present
        for ((key,value) in PackConfig.defaultScriptValues) {
            if (!packConfig.scriptSettings.containsKey(key)) {
                packConfig.scriptSettings[key] = value
            }
        }
    }

    /**
     * Check the inclusion-specifications for existing sources, valid destinations and valid
     * in-/exclusion-filter regexes.
     */
    fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck = inclusionsValidator.checkInclusions(inclusions, modpackDir, configCheck, printLog)

    /**
     * Check a given ZIP-archives contents for validity as a modpack.
     */
    fun checkZipArchive(pathToZip: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck =
        zipInspector.checkZipArchive(pathToZip, configCheck)

    /**
     * Update the destination to which the ZIP-archive will be extracted, based on whether a
     * directory of the same name already exists.
     */
    @Suppress("unused")
    fun unzipDestination(destination: String): String = zipInspector.unzipDestination(destination)

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
    fun suggestInclusions(modpackDir: String): ArrayList<InclusionSpecification> {
        
        log.info("Preparing a list of directories to include in server pack...")
        var doNotInclude: String
        val listDirectoriesInModpack = File(modpackDir).listFiles()
        val dirsInModpack: ArrayList<InclusionSpecification> = ArrayList(100)
        try {
            assert(listDirectoriesInModpack != null)
            for (dir in listDirectoriesInModpack!!) {
                if (dir.isDirectory) {
                    dirsInModpack.add(InclusionSpecification(dir.name))
                }
            }
        } catch (np: NullPointerException) {
            log.error(
                "Error: Something went wrong during the setup of the modpack. Copy dirs should never be empty. Please check the logs for errors and open an issue on https://github.com/Griefed/ServerPackCreator/issues.",
                np
            )
        }
        for (i in apiProperties.directoriesToExclude.indices) {
            doNotInclude = apiProperties.directoriesToExclude.toList()[i]
            dirsInModpack.removeIf { it.source == doNotInclude }
        }
        log.info("Modpack directory checked. Suggested directories for copyDirs-setting are:")
        for (inclusion in dirsInModpack) {
            log.info("    ${inclusion.source}")
        }
        return dirsInModpack
    }

    /**
     * Check whether various manifests from various launchers exist and use them to update the
     * PackConfig and pack-name.
     */
    fun checkManifests(destination: String, packConfig: PackConfig, configCheck: ConfigCheck = ConfigCheck()): String? =
        manifestParser.checkManifests(destination, packConfig, configCheck)

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
    fun printConfigurationModel(
        modpackDirectory: String,
        clientsideMods: List<String>,
        modsWhitelist: List<String>,
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
    ) {
        log.info("Your configuration is:")
        log.info("Modpack directory: $modpackDirectory")
        
        if (clientsideMods.isEmpty()) {
            log.warn("No client mods specified.")
        } else {
            log.info("Client mods specified. Client mods are:")
            ListUtilities.printListToLogChunked(clientsideMods, 5, "    ", true)
        }

        if (modsWhitelist.isEmpty()) {
            log.info("No whitelisted mods specified.")
        } else {
            log.info("Whitelisted mods specified. Whitelisted mods are:")
            ListUtilities.printListToLogChunked(modsWhitelist, 5, "    ", true)
        }

        log.info("Inclusions:")
        for (i in inclusions.indices) {
            log.info("Inclusion $i:")
            log.info("    %s".format(inclusions[i].source))
            log.info("    %s".format(inclusions[i].destination))
            log.info("    %s".format(inclusions[i].inclusionFilter))
            log.info("    %s".format(inclusions[i].exclusionFilter))
        }

        log.info("Minecraft version:                 $minecraftVer")
        log.info("Modloader:                         $modloader")
        log.info("Modloader Version:                 $modloaderVersion")
        log.info("Include server icon:               $includeIcon")
        log.info("Include server properties:         $includeProperties")
        log.info("Create zip-archive of server pack: $includeZip")
        log.info("Java arguments for start-scripts:  $javaArgs")
        log.info("Server pack suffix:                $serverPackSuffix")
        log.info("Path to custom server-icon:        $serverIconPath")
        log.info("Path to custom server.properties:  $serverPropertiesPath")
        log.info("Script settings:")
        for ((key, value) in scriptSettings) {
            log.info("  Placeholder: $key")
            log.info("        Value: $value")
        }
    }

    /**
     * Acquire a list of all directories in the base-directory of a ZIP-file.
     */
    fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String> =
        zipInspector.getDirectoriesInModpackZipBaseDirectory(zipFile)

    /**
     * Update the given PackConfig with values from a CurseForge manifest.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: File) =
        manifestParser.updateConfigModelFromCurseManifest(packConfig, manifest)

    /**
     * Acquire the modpacks name from the modpack-JSON stored in the PackConfig.
     */
    fun updatePackName(packConfig: PackConfig, vararg childNodes: String): String? =
        manifestParser.updatePackName(packConfig, *childNodes)

    /**
     * Update the given PackConfig with values from a CurseForge minecraftinstance.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: File) =
        manifestParser.updateConfigModelFromMinecraftInstance(packConfig, minecraftInstance)

    /**
     * Update the given PackConfig with values from a Modrinth modrinth.index.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: File) =
        manifestParser.updateConfigModelFromModrinthManifest(packConfig, manifest)

    /**
     * Update the given PackConfig with values from an ATLauncher instance.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: File) =
        manifestParser.updateConfigModelFromATLauncherInstance(packConfig, manifest)

    /**
     * Update the given PackConfig with values from a GDLauncher config.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File) =
        manifestParser.updateConfigModelFromConfigJson(packConfig, config)

    /**
     * Update the given PackConfig with values from a GDLauncher instance.json in the modpacks
     * parent-directory.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromGDInstanceJson(packConfig: PackConfig, instance: File) =
        manifestParser.updateConfigModelFromGDInstanceJson(packConfig, instance)

    /**
     * Update the given PackConfig with values from a MultiMC/Prism mmc-pack.json.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: File) =
        manifestParser.updateConfigModelFromMMCPack(packConfig, mmcPack)

    /**
     * Acquire the instance-name from a MultiMC/Prism instance.cfg.
     */
    @Throws(IOException::class)
    fun updateDestinationFromInstanceCfg(instanceCfg: File): String =
        manifestParser.updateDestinationFromInstanceCfg(instanceCfg)

    /**
     * Normalize the modloader-name to first-letter-uppercase, defaulting to Forge for unknown
     * loaders.
     */
    fun getModLoaderCase(modloader: String): String = manifestParser.getModLoaderCase(modloader)

    /**
     * Check the passed directory for existence, type and the absence of the overrides-directory.
     */
    fun checkModpackDir(
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck = modpackDirectoryValidator.checkModpackDir(modpackDir, configCheck, printLog)

    /**
     * Acquire a list of all files and directories in a ZIP-file.
     */
    fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String> =
        zipInspector.getAllFilesAndDirectoriesInModpackZip(zipFile)

    /**
     * Acquire a list of all directories in a ZIP-file, excluding files.
     */
    fun getDirectoriesInModpackZip(zipFile: File): List<String> =
        zipInspector.getDirectoriesInModpackZip(zipFile)

    /**
     * Acquire a list of all files in a ZIP-file, excluding directories.
     */
    fun getFilesInModpackZip(zipFile: File): List<String> =
        zipInspector.getFilesInModpackZip(zipFile)

    /**
     * Generate a [PackConfig] from a modpack-directory, resulting in a basic server pack configuration with default
     * values, for an easy-to-use starting point of a server pack config.
     *
     * @param modpackDirectory The directory which contains the modpack for which a server pack config should be generated.
     * @return A [PackConfig] for the specified modpack. If no manifests were available, then this PackConfig will only
     * contain basic values, like a list of clientside-only mods, but no detected Minecraft version, modloader, or modloader
     * version etc.
     *
     * @author Griefed
     */
    fun generateConfigFromModpack(modpackDirectory: File): PackConfig {
        val packConfig = PackConfig()
        if (modpackDirectory.isDirectory) {
            packConfig.modpackDir = modpackDirectory.absolutePath
            try {
                val inclusions = emptyList<InclusionSpecification>().toMutableList()
                val files = modpackDirectory.listFiles()

                packConfig.name = checkManifests(modpackDirectory.absolutePath, packConfig)

                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        if (apiProperties.directoriesToInclude.contains(file.name) &&
                            !inclusions.any { inclusion -> inclusion.source == file.name }
                        ) {
                            inclusions.add(InclusionSpecification(file.name))
                        }
                    }
                }
                inclusions.removeIf { !File(modpackDirectory,it.source).exists() && !File(it.source).exists() }

                packConfig.setInclusions(ArrayList(inclusions))

            } catch (ex: IOException) {
                log.error("Couldn't create server pack config from modpack manifests.", ex)
            }
        }
        return packConfig
    }
}