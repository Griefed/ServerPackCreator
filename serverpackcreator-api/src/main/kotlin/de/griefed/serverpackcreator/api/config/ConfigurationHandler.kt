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
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.SPCConfigCheckListener
import de.griefed.serverpackcreator.api.utilities.SPCGenericListener
import de.griefed.serverpackcreator.api.utilities.SecurityScans
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.Paths
import java.nio.file.ProviderNotFoundException
import java.util.*
import java.util.regex.PatternSyntaxException

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
    val forge = "^forge$".toRegex()
    val neoForge = "^neoforge$".toRegex()
    val fabric = "^fabric$".toRegex()
    val quilt = "^quilt$".toRegex()
    val legacyFabric = "^legacyfabric$".toRegex()
    val whitespace = "^\\s+$".toRegex()
    val previous = ".*_\\d".toRegex()
    val zipCheck = "^\\w+[/\\\\]$".toRegex()

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

            // This log is meant to be read by the user, therefore we allow translation.
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
        /*log.info("Performing jNeedle scan")
        configCheck.otherErrors.addAll(SecurityScans.scanUsingJNeedle(modpack.toPath()))*/

        if (!checkIconAndProperties(packConfig.serverIconPath)) {
            configCheck.serverIconErrors.add(Translations.configuration_log_error_servericon(packConfig.serverIconPath))
            log.error("The specified server-icon does not exist: ${packConfig.serverIconPath}")
            // This log is meant to be read by the user, therefore we allow translation.
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
     * Checks whether either Forge or Fabric were specified as the modloader.
     *
     * @param modloader Check as case-insensitive for Forge or Fabric.
     * @return `true` if the specified modloader is either Forge or Fabric. False if neither.
     * @author Griefed
     */
    fun checkModloader(modloader: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck {
        if (!modloader.lowercase().matches(forge)
            && !modloader.lowercase().matches(neoForge)
            && !modloader.lowercase().matches(fabric)
            && !modloader.lowercase().matches(quilt)
            && !modloader.lowercase().matches(legacyFabric)
        ) {
            configCheck.modloaderErrors.add(Translations.configuration_log_error_checkmodloader.toString())
            log.error("Invalid modloader specified. Modloader must be either Forge, NeoForge, Fabric or Quilt.")
        }
        return configCheck
    }

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
            packConfig.serverIconPath = file.absolutePath
        }
        return configCheck
    }

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
    fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, configCheck: ConfigCheck = ConfigCheck()
    ): ConfigCheck {
        when (modloader) {
            "Forge" -> if (!versionMeta.forge.isForgeAndMinecraftCombinationValid(minecraftVersion, modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "NeoForge" -> if (!versionMeta.neoForge.isNeoForgeAndMinecraftCombinationValid(minecraftVersion,modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Fabric" -> if (!versionMeta.fabric.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.getLoaderDetails(minecraftVersion,modloaderVersion).isPresent) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Quilt" -> if (!versionMeta.quilt.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "LegacyFabric" -> if (!versionMeta.legacyFabric.isVersionValid(modloaderVersion)
                || !versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Translations.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            else -> {
                log.error("Specified incorrect modloader version. Please check your modpack for the correct version and enter again.")
                configCheck.modloaderVersionErrors.add("Specified incorrect modloader version. Please check your modpack for the correct version and enter again.")
            }
        }
        return configCheck
    }

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
    fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck {
        val hasLazy = inclusions.any { entry -> entry.source == "lazy_mode" }
        if (inclusions.isEmpty()) {
            if (printLog) {
                log.error("No directories or files specified for copying. This would result in an empty server pack.")
            }
            configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_empty.toString())
        } else if (inclusions.size == 1 && hasLazy) {
            if (printLog) {
                log.warn(
                    "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!"
                )
                log.warn(
                    "Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions."
                )
                log.warn(
                    "You will not receive support from me for a server pack generated this way."
                )
                log.warn(
                    "Do not open an issue on GitHub if this configuration errors or results in a broken server pack."
                )
                log.warn(
                    "!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!"
                )
            }
        } else {
            if (inclusions.size > 1 && hasLazy && printLog) {
                log.warn(
                    "You specified lazy mode in your configuration, but your copyDirs configuration contains other"
                            + " entries. To use the lazy mode, only specify \"lazy_mode\" and nothing else. Ignoring lazy mode."
                )
            }
            inclusions.removeIf { entry -> entry.source == "lazy_mode" }
            for (inclusion in inclusions) {
                if (inclusion.isGlobalFilter()) {
                    continue
                }
                val modpackSource = File(modpackDir, inclusion.source).absoluteFile
                if (!File(inclusion.source).absoluteFile.exists() && !modpackSource.exists()) {
                    if (printLog) {
                        log.error("Source ${inclusion.source} does not exist. Please specify existing files.")
                    }
                    configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_filenotfound(inclusion.source))
                }
                if (inclusion.hasDestination()
                    && !StringUtilities.checkForInvalidPathCharacters(inclusion.destination!!)) {
                    log.warn("Invalid destination specified: ${inclusion.destination}.")
                    inclusion.destination = null
                    configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_destination(inclusion.source))
                }
                if (inclusion.hasInclusionFilter()) {
                    try {
                        inclusion.inclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid inclusion-regex specified: ${inclusion.inclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_inclusion(inclusion.inclusionFilter ?: ""))
                    }
                }
                if (inclusion.hasExclusionFilter()) {
                    try {
                        inclusion.exclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid exclusion-regex specified: ${inclusion.exclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Translations.configuration_log_error_checkcopydirs_inclusion(inclusion.exclusionFilter ?: ""))
                    }
                }
            }
        }
        return configCheck
    }

    /**
     * Check a given ZIP-archives contents. If the ZIP-archive only contains one directory, or if it
     * contains neither the mods nor the config directories, consider it invalid.
     *
     * @param pathToZip         Path to the ZIP-file to check.
     * @author Griefed
     */
    fun checkZipArchive(pathToZip: String, configCheck: ConfigCheck = ConfigCheck()): ConfigCheck {
        try {
            ZipFile(Paths.get(pathToZip).toFile()).use {
                if (it.isNotValidZipFile()) {
                    configCheck.modpackErrors.add("$pathToZip is not a valid ZIP-file.")
                    return configCheck
                }
            }
        } catch (ex: IOException) {
            log.error("Could not validate ZIP-file $pathToZip.", ex)
            configCheck.modpackErrors.add("Could not validate ZIP-file $pathToZip.")
            return configCheck
        }
        try {
            val foldersInModpackZip = getDirectoriesInModpackZipBaseDirectory(Paths.get(pathToZip).toFile())

            // If the ZIP-file only contains one directory, assume it is overrides and return true to
            // indicate invalid configuration.
            if (foldersInModpackZip.size == 1) {
                log.error(
                    "The ZIP-file you specified only contains one directory: ${foldersInModpackZip[0]}. "
                            + "ZIP-files for ServerPackCreator must be full modpacks, with all their contents being in the root of the ZIP-file."
                )

                // This log is meant to be read by the user, therefore we allow translation.
                configCheck.modpackErrors.add(Translations.configuration_log_error_zip_overrides(foldersInModpackZip[0]))
                return configCheck

                // If the ZIP-file does not contain the mods or config directories, consider it invalid.
            } else if (!foldersInModpackZip.contains("mods/") || !foldersInModpackZip.contains("config/")) {
                log.error(
                    "The ZIP-file you specified does not contain the mods or config directories. What use is a modded server without mods and their configurations?"
                )

                // This log is meant to be read by the user, therefore we allow translation.
                configCheck.modpackErrors.add(Translations.configuration_log_error_zip_modsorconfig.toString())
                return configCheck
            }
        } catch (ex: IOException) {
            log.error("Couldn't acquire directories in ZIP-file.", ex)

            // This log is meant to be read by the user, therefore we allow translation.
            configCheck.modpackErrors.add(Translations.configuration_log_error_zip_directories.toString())
            return configCheck
        }
        return configCheck
    }

    /**
     * Update the destination to which the ZIP-archive will the extracted to, based on whether a
     * directory of the same name already exists.
     *
     * @param destination The destination to where the ZIP-archive was about to be extracted to.
     * @return The destination where the ZIP-archive will be extracted to.
     * @author Griefed
     */
    @Suppress("unused")
    fun unzipDestination(destination: String): String {
        var dest = destination
        if (File(dest).isDirectory || File("${dest}_0").isDirectory) {
            var incrementation = 0
            while (File("${dest}_$incrementation").isDirectory) {
                incrementation++
            }
            dest = "${dest}_$incrementation"
        } else {
            dest += "_0"
        }
        return File(dest).path
    }

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
        // This log is meant to be read by the user, therefore we allow translation.
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
     * Check whether various manifests from various launchers exist and use them to update our
     * ConfigurationModel and pack name.
     *
     * @param destination The destination in which the manifests are.
     * @param packConfig The ConfigurationModel to update.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return The name of the modpack currently being checked. `null` if the name could not be
     * acquired.
     * @author Griefed
     */
    fun checkManifests(destination: String, packConfig: PackConfig, configCheck: ConfigCheck = ConfigCheck()): String? {
        var packName: String? = null
        val curseManifest = File(destination, "manifest.json")
        val curseMinecraftInstance = File(destination, "minecraftinstance.json")
        val atLauncherInstance = File(destination, "instance.json")
        val gdLauncherInstance = File(File(destination).parentFile,"instance.json")
        val mmcPack = File(destination, "mmc-pack.json")
        val mmcInstance = File(destination, "instance.cfg")
        when {
            curseMinecraftInstance.exists() -> {
                // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
                // Check misc/curseforge/minecraftinstance.json in the repo
                try {
                    updateConfigModelFromMinecraftInstance(packConfig, curseMinecraftInstance)
                    packName = if (packConfig.name != null) {
                        packConfig.name!!
                    } else {
                        updatePackName(packConfig, "name")
                    }
                } catch (ex: IOException) {
                    log.error("Error parsing minecraftinstance.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_instance.toString())
                }
            }

            curseManifest.exists() -> {
                // Check manifest.json usually created by Overwolf's CurseForge launcher.
                // Check misc/curseforge/manifest.json in the repo
                try {
                    updateConfigModelFromCurseManifest(packConfig, curseManifest)
                    packName = updatePackName(packConfig, "name")
                } catch (ex: IOException) {
                    log.error("Error parsing CurseForge manifest.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_manifest.toString())
                }
            }

            atLauncherInstance.exists() -> {
                // Check instance.json usually created by ATLauncher
                // Check misc/atlauncher/instance.json in the repo
                try {
                    updateConfigModelFromATLauncherInstance(packConfig, File(destination, "instance.json"))
                    packName = updatePackName(packConfig, "launcher", "name")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_config.toString())
                }
            }


            gdLauncherInstance.exists() -> {
                // Check the instance.json usually created by new versions of GDLauncher, in the parent folder.
                try {
                    updateConfigModelFromGDInstanceJson(packConfig, gdLauncherInstance)
                    packName = updatePackName(packConfig, "loader", "sourceName")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_config.toString())
                }
            }

            mmcPack.exists() -> {
                // Check mmc-pack.json usually created by MultiMC.
                try {
                    updateConfigModelFromMMCPack(packConfig, mmcPack)
                } catch (ex: IOException) {
                    log.error("Error parsing mmc-pack.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Translations.configuration_log_error_zip_mmcpack.toString())
                }
                try {
                    if (mmcInstance.exists()) {
                        packName = updateDestinationFromInstanceCfg(mmcInstance)
                        packConfig.name = packName
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't read instance.cfg.", ex)
                }
            }
        }
        return packName
    }

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
            // This log is meant to be read by the user, therefore we allow translation.
            log.warn("No client mods specified.")
        } else {
            // This log is meant to be read by the user, therefore we allow translation.
            log.info("Client mods specified. Client mods are:")
            ListUtilities.printListToLogChunked(clientsideMods, 5, "    ", true)
        }
        // This log is meant to be read by the user, therefore we allow translation.
        log.info("Inclusions:")
        for (i in inclusions.indices) {
            log.info("Inclusion $i:")
            log.info("    %s".format(inclusions[i].source))
            log.info("    %s".format(inclusions[i].destination))
            log.info("    %s".format(inclusions[i].inclusionFilter))
            log.info("    %s".format(inclusions[i].exclusionFilter))
        }
        // This log is meant to be read by the user, therefore we allow translation.
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
     * Acquire a list of directories in the base-directory of a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the base-directory of the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String> {
        val baseDirectories: TreeSet<String> = TreeSet()
        var headerBeginning: String
        ZipFile(zipFile).use {
            val headers = it.fileHeaders
            for (header in headers) {
                try {
                    headerBeginning = header.fileName.substring(0,header.fileName.indexOfFirst { char -> char == '/' } + 1)
                    log.trace("Header beginning $headerBeginning")
                    if (headerBeginning.matches(zipCheck)) {
                        baseDirectories.add(headerBeginning)
                    }
                } catch (ex: StringIndexOutOfBoundsException) {
                    log.debug("Could not parse ${header.fileName}")
                }
            }
        }
        return baseDirectories.toList()
    }

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
    @Throws(IOException::class)
    fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val minecraft = packConfig.modpackJson!!.get("minecraft")
        val modloaders = minecraft.get("modLoaders").get(0)
        val id = modloaders.get("id").asText()
        val modloaderAndVersion: List<String> = id.split("-")
        packConfig.minecraftVersion = minecraft.get("version").asText()
        packConfig.modloader = modloaderAndVersion[0]
        packConfig.modloaderVersion = modloaderAndVersion[1]
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
    }

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
    fun updatePackName(packConfig: PackConfig, vararg childNodes: String) = try {
        val modpackDir = apiProperties.modpacksDirectory.toString()
        val packName = packConfig.modpackJson?.let {
            utilities.jsonUtilities.getNestedText(
                it, *childNodes
            )
        }
        @Suppress("IfThenToElvis")
        if (packName != null) {
            packName
        } else {
            File(modpackDir).name
        }
    } catch (npe: NullPointerException) {
        null
    }

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
    @Throws(IOException::class)
    fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(minecraftInstance)
        val json = packConfig.modpackJson!!
        val base = json.get("baseModLoader")
        val modloader = base.get("name").asText().split("-")[0]
        packConfig.modloader = getModLoaderCase(modloader)
        //even Fabric, Quilt, and NeoForge have the modloader version under this JSON tag
        packConfig.modloaderVersion = base.get("forgeVersion").asText()
        packConfig.minecraftVersion = base.get("minecraftVersion").asText()
        val urlPath = arrayOf("installedModpack", "thumbnailUrl")
        val namePath = arrayOf("name")
        try {
            getAndSetIcon(json, packConfig, urlPath, namePath)
        } catch (_: NullPointerException) {
        } catch (ex: Exception) {
            log.error("Error acquiring icon.", ex)
        }
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
        packConfig.projectID = packConfig.modpackJson!!.get("projectID").asText()
        packConfig.versionID = packConfig.modpackJson!!.get("fileID").asText()
        packConfig.source = ModpackSource.CURSEFORGE
    }

    /**
     * **`modrinth.index.json`**
     *
     * Update the given ConfigurationModel with values gathered from a Modrinth `modrinth.index.json`-manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val dependencies = packConfig.modpackJson!!.get("dependencies")
        packConfig.minecraftVersion = dependencies.get("minecraft").asText()
        val iterator: Iterator<Map.Entry<String, JsonNode>> = dependencies.fields()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            when (key) {
                "fabric-loader" -> {
                    packConfig.modloader = "Fabric"
                    packConfig.modloaderVersion = value.asText()
                }

                "quilt-loader" -> {
                    packConfig.modloader = "Quilt"
                    packConfig.modloaderVersion = value.asText()
                }

                "forge" -> {
                    packConfig.modloader = "Forge"
                    packConfig.modloaderVersion = value.asText()
                }

                "neoforge" -> {
                    packConfig.modloader = "NeoForge"
                    packConfig.modloaderVersion = value.asText()
                }
            }
        }
    }

    /**
     * **`instance.json`**
     *
     * Update the given ConfigurationModel with values gathered from a ATLauncher manifest.
     *
     * @param packConfig The model to update.
     * @param manifest           The manifest file.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val json = packConfig.modpackJson!!
        packConfig.minecraftVersion = json.get("id").asText()
        val launcher = json.get("launcher")
        val loaderVersion = launcher.get("loaderVersion")
        packConfig.modloader = loaderVersion.get("type").asText()
        packConfig.modloaderVersion = loaderVersion.get("version").asText()
        val urlPath = arrayOf("launcher", "curseForgeProject", "logo", "thumbnailUrl")
        val namePath = arrayOf("launcher", "name")
        try {
            getAndSetIcon(json, packConfig, urlPath, namePath)
        } catch (_: NullPointerException) {
        } catch (ex: Exception) {
            log.error("Error acquiring icon.", ex)
        }
        packConfig.name = packConfig.modpackJson!!.get("launcher").get("name").asText()
        try {
            packConfig.projectID = packConfig.modpackJson!!.get("launcher").get("curseForgeProject").get("id").asText()
            packConfig.versionID = packConfig.modpackJson!!.get("curseForgeFile").get("id").asText()
            packConfig.source = ModpackSource.CURSEFORGE
        } catch (ex: Exception) {
            log.error("Error acquiring modpack-source details. Please report this to ServerPackCreator in GitHub.", ex)
        }
    }

    @Throws(NullPointerException::class)
    private fun getAndSetIcon(json: JsonNode, packConfig: PackConfig, urlPath: Array<String>, namePath: Array<String>) {
        val iconUrl = URI(utilities.jsonUtilities.getNestedText(json, *urlPath)).toURL()
        val iconName = utilities.jsonUtilities.getNestedText(json, *namePath) + ".png"
        val iconFile = File(apiProperties.iconsDirectory.absolutePath, iconName)
        if (utilities.webUtilities.downloadFile(iconFile, iconUrl)) {
            packConfig.serverIconPath = iconFile.absolutePath
        }
    }

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
    @Throws(IOException::class)
    fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(config)
        val loader = packConfig.modpackJson!!.get("loader")
        packConfig.modloader = getModLoaderCase(loader.get("loaderType").asText())
        packConfig.minecraftVersion = loader.get("mcVersion").asText()
        packConfig.modloaderVersion =
            loader.get("loaderVersion").asText().replace("${packConfig.minecraftVersion}-", "")
    }

    /**
     * **`parentDirectory/instance.json`**
     *
     * Update the given PackConfig with values gathered from the modpacks instance.json. An
     * instance.json is usually created by GDLauncher and located in the modpacks parent directory of the data-directory.
     *
     * @param packConfig An instance containing a configuration for a modpack from which to
     * create a server pack.
     * @param instance             The instance.json-file of the modpack to read.
     */
    @Throws(IOException::class)
    fun updateConfigModelFromGDInstanceJson(packConfig: PackConfig, instance: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(instance)
        val version = packConfig.modpackJson!!.get("game_configuration").get("version")
        packConfig.modloader = version.get("modloaders")[0].get("type").asText()
        packConfig.minecraftVersion = version.get("release").asText()
        packConfig.modloaderVersion = version.get("modloaders")[0].get("version").asText().replace("${packConfig.minecraftVersion}-","")
        packConfig.name = packConfig.modpackJson!!.get("name").asText()
        packConfig.projectID = packConfig.modpackJson!!.get("modpack").get("project_id").asInt().toString()
        packConfig.versionID = packConfig.modpackJson!!.get("modpack").get("file_id").asInt().toString()
        val source = packConfig.modpackJson!!.get("modpack").get("platform").asText()
        packConfig.source = if (source == "Curseforge") {
            ModpackSource.CURSEFORGE
        } else {
            ModpackSource.MODRINTH
        }
    }

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
    @Throws(IOException::class)
    fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(mmcPack)
        val components = packConfig.modpackJson!!.get("components")
        for (jsonNode in components) {
            val version = jsonNode.get("version").asText()
            when (jsonNode.get("uid").asText()) {
                "net.minecraft" -> packConfig.minecraftVersion = version
                "net.minecraftforge" -> {
                    packConfig.modloader = "Forge"
                    packConfig.modloaderVersion = version
                }

                "net.fabricmc.fabric-loader" -> {
                    packConfig.modloader = "Fabric"
                    packConfig.modloaderVersion = version
                }

                "org.quiltmc.quilt-loader" -> {
                    packConfig.modloader = "Quilt"
                    packConfig.modloaderVersion = version
                }

                "net.neoforged" -> {
                    packConfig.modloader = "NeoForge"
                    packConfig.modloaderVersion = version
                }
            }
        }
    }

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
    @Throws(IOException::class)
    fun updateDestinationFromInstanceCfg(instanceCfg: File): String {
        var name: String
        instanceCfg.inputStream().use {
            val properties = Properties()
            properties.load(it)
            name = properties.getProperty("name", null)
        }
        return name
    }

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
    fun checkModpackDir(
        modpackDir: String,
        configCheck: ConfigCheck = ConfigCheck(),
        printLog: Boolean = true
    ): ConfigCheck {
        val modpack = File(modpackDir)
        if (modpackDir.isEmpty()) {
            if (printLog) {
                log.error("Modpack directory not specified. Please specify an existing directory.")
            }
            configCheck.modpackErrors.add(Translations.configuration_log_error_checkmodpackdir.toString())
        } else if (!modpack.exists()) {
            if (printLog) {
                log.warn("Couldn't find directory $modpackDir.")
            }
            configCheck.modpackErrors.add(Translations.configuration_log_error_modpackdirectory(modpackDir))
        } else if (modpack.isDirectory){
            val files = modpack.listFiles { entry -> entry.isDirectory }
            if (files.any { entry -> entry.name == "overrides" }) {
                log.error("Modpack contains directory \"overrides\". Modpacks must be installed through a client such as CurseForge, GDLauncher, MultiMC etc. Full modpacks shouldn't contain the overrides directory anymore.")
                configCheck.modpackErrors.add(Translations.configuration_log_error_modpack_overrides.toString())
            }
        }
        return configCheck
    }

    /**
     * Acquire a list of all files and directories in a ZIP-file.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files and directories in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String> {
        val filesAndDirectories: MutableList<String> = ArrayList(100)
        try {
            filesAndDirectories.addAll(getDirectoriesInModpackZip(zipFile))
        } catch (ex: IOException) {
            log.error("Could not acquire file or directory from ZIP-archive.", ex)
        }
        try {
            filesAndDirectories.addAll(getFilesInModpackZip(zipFile))
        } catch (ex: IOException) {
            log.error("Could not acquire file or directory from ZIP-archive.", ex)
        }
        return filesAndDirectories
    }

    /**
     * Acquire a list of all directories in a ZIP-file. The resulting list excludes files.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All directories in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getDirectoriesInModpackZip(zipFile: File): List<String> {
        val directories: MutableList<String> = ArrayList(100)
        ZipFile(zipFile).use {
            for (header in it.fileHeaders) {
                if (header.isDirectory) {
                    directories.add(header.fileName)
                }
            }
        }
        return directories
    }

    /**
     * Acquire a list of all files in a ZIP-file. The resulting list excludes directories.
     *
     * @param zipFile The ZIP-archive to get the list of files from.
     * @return All files in the ZIP-file.
     * @author Griefed
     */
    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    fun getFilesInModpackZip(zipFile: File): List<String> {
        val files: MutableList<String> = ArrayList(100)
        ZipFile(zipFile).use {
            for (header in it.fileHeaders) {
                if (!header.isDirectory) {
                    files.add(header.fileName)
                }
            }
        }
        return files
    }

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

                packConfig.setInclusions(ArrayList<InclusionSpecification>(inclusions))

            } catch (ex: IOException) {
                log.error("Couldn't create server pack config from modpack manifests.", ex)
            }
        }
        return packConfig
    }
}