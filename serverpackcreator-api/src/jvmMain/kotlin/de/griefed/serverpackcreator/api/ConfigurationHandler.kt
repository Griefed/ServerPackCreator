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

import Api
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.utilities.File
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.isNotValidZipFile
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import java.io.IOException
import java.net.URI
import java.nio.file.*
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
actual class ConfigurationHandler(
    private val versionMeta: VersionMeta,
    private val apiProperties: ApiProperties,
    private val utilities: Utilities,
    private val apiPlugins: ApiPlugins
) : Configuration<File, Path>() {
    private val zipRegex = "\\.[Zz][Ii][Pp]".toRegex()

    actual override fun checkConfiguration(configFile: File, packConfig: PackConfig, configCheck: ConfigCheck, quietCheck: Boolean): ConfigCheck {
        try {
            val fileConf = PackConfig(utilities, configFile)
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
            configCheck.configErrors.add(Api.configuration_log_error_checkconfig_start.toString())
            return configCheck
        }
    }

    actual override fun checkConfiguration(packConfig: PackConfig, configCheck: ConfigCheck, quietCheck: Boolean): ConfigCheck {
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

        if (!checkIconAndProperties(packConfig.serverIconPath)) {
            configCheck.serverIconErrors.add(Api.configuration_log_error_servericon(packConfig.serverIconPath))
            log.error("The specified server-icon does not exist: ${packConfig.serverIconPath}")
            // This log is meant to be read by the user, therefore we allow translation.
        } else if (packConfig.serverIconPath.isNotEmpty()
            && File(packConfig.serverIconPath).exists()
            && !utilities.fileUtilities.isReadPermissionSet(packConfig.serverIconPath)
        ) {
            configCheck.serverIconErrors.add(Api.configuration_log_error_checkcopydirs_read(packConfig.serverIconPath))
            log.error("No read-permission for ${packConfig.serverIconPath}")
        }
        if (!checkIconAndProperties(packConfig.serverPropertiesPath)) {
            configCheck.serverPropertiesErrors.add(Api.configuration_log_error_serverproperties(packConfig.serverPropertiesPath))
            log.error("The specified server.properties does not exist: ${packConfig.serverPropertiesPath}")
        } else if (packConfig.serverPropertiesPath.isNotEmpty()
            && File(packConfig.serverPropertiesPath).exists()
            && !utilities.fileUtilities.isReadPermissionSet(packConfig.serverPropertiesPath)
        ) {
            configCheck.serverPropertiesErrors.add(Api.configuration_log_error_checkcopydirs_read(packConfig.serverPropertiesPath))
            log.error("No read-permission for ${packConfig.serverPropertiesPath}")
        }

        val modpack = File(packConfig.modpackDir)
        if (modpack.isDirectory) {
            isDir(packConfig, configCheck)
        } else if (modpack.isFile && modpack.name.endsWith("zip")) {
            try {
                isZip(packConfig, configCheck)
            } catch (ex: IOException) {
                configCheck.modpackErrors.add("An error occurred whilst working with the ZIP-archive.")
                log.error("An error occurred whilst working with the ZIP-archive.", ex)
            }
        } else {
            configCheck.modpackErrors.add(Api.configuration_log_error_checkmodpackdir.toString())
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
            configCheck.minecraftVersionErrors.add(Api.configuration_log_error_minecraft.toString())
            log.error("There's something wrong with your Minecraft version setting.")
        }

        apiPlugins.runConfigCheckExtensions(packConfig, configCheck)

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

    actual override fun checkModloader(modloader: String, configCheck: ConfigCheck): ConfigCheck {
        if (!modloader.lowercase().matches(forge)
            && !modloader.lowercase().matches(neoForge)
            && !modloader.lowercase().matches(fabric)
            && !modloader.lowercase().matches(quilt)
            && !modloader.lowercase().matches(legacyFabric)
        ) {
            configCheck.modloaderErrors.add(Api.configuration_log_error_checkmodloader.toString())
            log.error("Invalid modloader specified. Modloader must be either Forge, Fabric or Quilt.")
        }
        return configCheck
    }

    actual override fun sanitizeLinks(packConfig: PackConfig) {
        log.info("Checking configuration for links...")
        if (packConfig.modpackDir.isNotEmpty() && utilities.fileUtilities.isLink(packConfig.modpackDir)) {
            try {
                packConfig.modpackDir = utilities.fileUtilities.resolveLink(packConfig.modpackDir)
                log.info("Resolved modpack directory link to: ${packConfig.modpackDir}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for modpack directory.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for modpack directory.", ex)
            }
        }
        if (packConfig.serverIconPath.isNotEmpty() && utilities.fileUtilities.isLink(packConfig.serverIconPath)) {
            try {
                packConfig.serverIconPath = utilities.fileUtilities.resolveLink(packConfig.serverIconPath)
                log.info("Resolved server-icon link to: ${packConfig.serverIconPath}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for server-icon.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for server-icon.", ex)
            }
        }
        if (packConfig.serverPropertiesPath.isNotEmpty() && utilities.fileUtilities.isLink(packConfig.serverPropertiesPath)) {
            try {
                packConfig.serverPropertiesPath = utilities.fileUtilities.resolveLink(packConfig.serverPropertiesPath)
                log.info("Resolved server-properties link to: ${packConfig.serverPropertiesPath}")
            } catch (ex: InvalidFileTypeException) {
                log.error("Couldn't resolve link for server-properties.", ex)
            } catch (ex: IOException) {
                log.error("Couldn't resolve link for server-properties.", ex)
            }
        }
        if (packConfig.inclusions.isNotEmpty()) {
            val copyDirs: ArrayList<InclusionSpecification> = packConfig.inclusions
            val fileUtils = utilities.fileUtilities
            var inclusionChanges = false
            var entry: InclusionSpecification
            var link: String
            for (inclusion in packConfig.inclusions.indices) {
                entry = packConfig.inclusions[inclusion]

                if (!entry.source.startsWith(packConfig.modpackDir) && fileUtils.isLink(entry.source)) {
                    link = fileUtils.resolveLink(entry.source)
                    entry.source = link
                    inclusionChanges = true
                    log.info("Resolved source to $link.")
                } else if (fileUtils.isLink(packConfig.modpackDir + File.separator + entry.source)) {
                    link = fileUtils.resolveLink("${packConfig.modpackDir}${File.separator}${entry.source}")
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

    actual override fun checkIconAndProperties(iconOrPropertiesPath: String) = if (iconOrPropertiesPath.isEmpty()) {
        true
    } else {
        File(iconOrPropertiesPath).isFile
    }

    actual override fun isDir(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck {
        if (checkInclusions(packConfig.inclusions, packConfig.modpackDir, configCheck).inclusionsChecksPassed) {
            log.debug("copyDirs setting check passed.")
        } else {
            log.error("There's something wrong with your setting of directories to include in your server pack.")
            configCheck.inclusionErrors.add(Api.configuration_log_error_isdir_copydir.toString())
        }
        return configCheck
    }

    @Throws(IOException::class)
    actual override fun isZip(packConfig: PackConfig, configCheck: ConfigCheck): ConfigCheck {
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
        utilities.fileUtilities.unzipArchive(packConfig.modpackDir, unzippedModpack)
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
            configCheck.modpackErrors.add(Api.configuration_log_error_zip_manifests.toString())
        }

        // If no json was read from the modpack, we must sadly use the ZIP-files name as the new
        // destination. Sad-face.
        if (packName == null) {
            packName = unzippedModpack
        }
        packName = File(utilities.stringUtilities.pathSecureTextAlternative(packName)).path

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

    actual override fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, configCheck: ConfigCheck
    ): ConfigCheck {
        when (modloader) {
            "Forge" -> if (!versionMeta.forge.isForgeAndMinecraftCombinationValid(minecraftVersion, modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Api.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "NeoForge" -> if (!versionMeta.neoForge.isNeoForgeAndMinecraftCombinationValid(minecraftVersion,modloaderVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Api.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Fabric" -> if (!versionMeta.fabric.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.getLoaderDetails(minecraftVersion,modloaderVersion).isPresent) {
                configCheck.modloaderVersionErrors.add(
                    Api.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "Quilt" -> if (!versionMeta.quilt.isVersionValid(modloaderVersion)
                || !versionMeta.fabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Api.configuration_log_error_checkmodloaderandversion(
                        minecraftVersion, modloader, modloaderVersion
                    )
                )
            }

            "LegacyFabric" -> if (!versionMeta.legacyFabric.isVersionValid(modloaderVersion)
                || !versionMeta.legacyFabric.isMinecraftSupported(minecraftVersion)) {
                configCheck.modloaderVersionErrors.add(
                    Api.configuration_log_error_checkmodloaderandversion(
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

    actual override fun ensureScriptSettingsDefaults(packConfig: PackConfig) {
        val server = versionMeta.minecraft.getServer(packConfig.minecraftVersion)
        if (!server.isPresent || !server.get().url().isPresent) {
            packConfig.scriptSettings["SPC_MINECRAFT_SERVER_URL_SPC"] = ""
        } else {
            packConfig.scriptSettings["SPC_MINECRAFT_SERVER_URL_SPC"] = server.get().url().get().toString()
        }
        packConfig.scriptSettings["SPC_SERVERPACKCREATOR_VERSION_SPC"] = apiProperties.apiVersion
        packConfig.scriptSettings["SPC_MINECRAFT_VERSION_SPC"] = packConfig.minecraftVersion
        packConfig.scriptSettings["SPC_MODLOADER_SPC"] = packConfig.modloader
        packConfig.scriptSettings["SPC_MODLOADER_VERSION_SPC"] = packConfig.modloaderVersion
        packConfig.scriptSettings["SPC_JAVA_ARGS_SPC"] = packConfig.javaArgs
        if (!packConfig.scriptSettings.containsKey("SPC_JAVA_SPC")) {
            packConfig.scriptSettings["SPC_JAVA_SPC"] = "java"
        }
        packConfig.scriptSettings["SPC_FABRIC_INSTALLER_VERSION_SPC"] = versionMeta.fabric.releaseInstaller()
        packConfig.scriptSettings["SPC_QUILT_INSTALLER_VERSION_SPC"] = versionMeta.quilt.releaseInstaller()
        packConfig.scriptSettings["SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC"] =
            versionMeta.legacyFabric.releaseInstaller()
    }

    actual override fun checkInclusions(
        inclusions: MutableList<InclusionSpecification>,
        modpackDir: String,
        configCheck: ConfigCheck,
        printLog: Boolean
    ): ConfigCheck {
        val hasLazy = inclusions.any { entry -> entry.source == "lazy_mode" }
        if (inclusions.isEmpty()) {
            if (printLog) {
                log.error("No directories or files specified for copying. This would result in an empty server pack.")
            }
            configCheck.inclusionErrors.add(Api.configuration_log_error_checkcopydirs_empty.toString())
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
                    configCheck.inclusionErrors.add(Api.configuration_log_error_checkcopydirs_filenotfound(inclusion.source))
                }
                if (inclusion.hasDestination()
                    && !utilities.stringUtilities.checkForInvalidPathCharacters(inclusion.destination!!)) {
                    log.warn("Invalid destination specified: ${inclusion.destination}.")
                    inclusion.destination = null
                    configCheck.inclusionErrors.add(Api.configuration_log_error_checkcopydirs_destination(inclusion.source))
                }
                if (inclusion.hasInclusionFilter()) {
                    try {
                        inclusion.inclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid inclusion-regex specified: ${inclusion.inclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Api.configuration_log_error_checkcopydirs_inclusion(inclusion.inclusionFilter ?: ""))
                    }
                }
                if (inclusion.hasExclusionFilter()) {
                    try {
                        inclusion.exclusionFilter!!.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid exclusion-regex specified: ${inclusion.exclusionFilter}.", ex)
                        configCheck.inclusionErrors.add(Api.configuration_log_error_checkcopydirs_inclusion(inclusion.exclusionFilter ?: ""))
                    }
                }
            }
        }
        return configCheck
    }

    actual override fun checkZipArchive(pathToZip: String, configCheck: ConfigCheck): ConfigCheck {
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
                configCheck.modpackErrors.add(Api.configuration_log_error_zip_overrides(foldersInModpackZip[0]))
                return configCheck

                // If the ZIP-file does not contain the mods or config directories, consider it invalid.
            } else if (!foldersInModpackZip.contains("mods/") || !foldersInModpackZip.contains("config/")) {
                log.error(
                    "The ZIP-file you specified does not contain the mods or config directories. What use is a modded server without mods and their configurations?"
                )

                // This log is meant to be read by the user, therefore we allow translation.
                configCheck.modpackErrors.add(Api.configuration_log_error_zip_modsorconfig.toString())
                return configCheck
            }
        } catch (ex: IOException) {
            log.error("Couldn't acquire directories in ZIP-file.", ex)

            // This log is meant to be read by the user, therefore we allow translation.
            configCheck.modpackErrors.add(Api.configuration_log_error_zip_directories.toString())
            return configCheck
        }
        return configCheck
    }

    actual override fun unzipDestination(destination: String): String {
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

    actual override fun suggestInclusions(modpackDir: String): ArrayList<InclusionSpecification> {
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

    actual override fun checkManifests(destination: String, packConfig: PackConfig, configCheck: ConfigCheck): String? {
        var packName: String? = null
        val manifestJson = File(destination, "manifest.json")
        val minecraftInstanceJson = File(destination, "minecraftinstance.json")
        val modrinthIndexJson = File(destination, "modrinth.index.json")
        val instanceJson = File(destination, "instance.json")
        val configJson = File(destination, "config.json")
        val mmcPackJson = File(destination, "mmc-pack.json")
        val instanceCfg = File(destination, "instance.cfg")
        when {
            minecraftInstanceJson.exists() -> {
                // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
                try {
                    updateConfigModelFromMinecraftInstance(packConfig, minecraftInstanceJson)
                    packName = updatePackName(packConfig, "name")
                } catch (ex: IOException) {
                    log.error("Error parsing minecraftinstance.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_instance.toString())
                }
            }

            instanceJson.exists() -> {
                // Check instance.json usually created by ATLauncher
                try {
                    updateConfigModelFromATLauncherInstance(packConfig, File(destination, "instance.json"))
                    packName = updatePackName(packConfig, "launcher", "name")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_config.toString())
                }
            }

            manifestJson.exists() -> {
                try {
                    updateConfigModelFromCurseManifest(packConfig, manifestJson)
                    packName = updatePackName(packConfig, "name")
                } catch (ex: IOException) {
                    log.error("Error parsing CurseForge manifest.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_manifest.toString())
                }
            }

            modrinthIndexJson.exists() -> {
                // Check modrinth.index.json usually available if the modpack is from Modrinth
                try {
                    updateConfigModelFromModrinthManifest(packConfig, modrinthIndexJson)
                    packName = updatePackName(packConfig, "name")
                } catch (ex: IOException) {
                    log.error("Error parsing modrinth.index.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_config.toString())
                }
            }

            configJson.exists() -> {
                // Check the config.json usually created by GDLauncher.
                try {
                    updateConfigModelFromConfigJson(packConfig, configJson)
                    packName = updatePackName(packConfig, "loader", "sourceName")
                } catch (ex: IOException) {
                    log.error("Error parsing config.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_config.toString())
                }
            }

            mmcPackJson.exists() -> {
                // Check mmc-pack.json usually created by MultiMC.
                try {
                    updateConfigModelFromMMCPack(packConfig, mmcPackJson)
                } catch (ex: IOException) {
                    log.error("Error parsing mmc-pack.json from ZIP-file.", ex)
                    configCheck.modpackErrors.add(Api.configuration_log_error_zip_mmcpack.toString())
                }
                try {
                    if (instanceCfg.exists()) {
                        packName = updateDestinationFromInstanceCfg(instanceCfg)
                    }
                } catch (ex: IOException) {
                    log.error("Couldn't read instance.cfg.", ex)
                }
            }
        }
        return packName
    }

    actual override fun checkServerPacksForIncrement(source: String, destination: String): String {
        // Check whether a server pack for the new destination already exists.
        // If it does, we need to change it to avoid overwriting any existing files.
        val modPack = if (File(source).path.matches(previous)) {
            val path = File(source).absolutePath
            path.substring(0, path.length - 1)
        } else {
            "${source}_"
        }
        val serverPack = if (File(destination).path.matches(previous)) {
            val path = File(destination).absolutePath
            path.substring(0, path.length - 1)
        } else {
            "${destination}_"
        }
        var incrementation = 0
        while (File(modPack + incrementation).isDirectory || File(serverPack + incrementation).isDirectory) {
            incrementation++
        }
        return serverPack + incrementation
    }

    actual override fun printConfigurationModel(
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
            utilities.listUtilities.printListToLogChunked(clientsideMods, 5, "    ", true)
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

    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    actual override fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String> {
        val baseDirectories: TreeSet<String> = TreeSet()
        var headerBeginning: String
        ZipFile(zipFile).use {
            val headers = it.fileHeaders
            for (header in headers) {
                try {
                    headerBeginning = header.fileName.substring(0,header.fileName.indexOfFirst { char -> char == '/' } + 1)
                    log.debug("Header beginning $headerBeginning")
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

    @Throws(IOException::class)
    actual override fun updateConfigModelFromCurseManifest(packConfig: PackConfig, manifest: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(manifest)
        val minecraft = packConfig.modpackJson!!.get("minecraft")
        val modloaders = minecraft.get("modLoaders").get(0)
        val id = modloaders.get("id").asText()
        val modloaderAndVersion: List<String> = id.split("-")
        packConfig.minecraftVersion = minecraft.get("version").asText()
        packConfig.modloader = modloaderAndVersion[0]
        packConfig.modloaderVersion = modloaderAndVersion[1]
    }

    actual override fun updatePackName(packConfig: PackConfig, vararg childNodes: String) = try {
        val modpackDir = apiProperties.modpacksDirectory.toString()
        val packName = packConfig.modpackJson?.let {
            utilities.jsonUtilities.getNestedText(
                it, *childNodes
            )
        }
        modpackDir + File.separator + packName
    } catch (npe: NullPointerException) {
        null
    }

    @Throws(IOException::class)
    actual override fun updateConfigModelFromMinecraftInstance(packConfig: PackConfig, minecraftInstance: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(minecraftInstance)
        val json = packConfig.modpackJson!!
        val base = json.get("baseModLoader")
        val modloaderInfo = base.get("name").asText()
        val modloader = modloaderInfo.split("-")[0]
        packConfig.modloader = getModLoaderCase(modloader)
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
    }

    @Throws(IOException::class)
    actual override fun updateConfigModelFromModrinthManifest(packConfig: PackConfig, manifest: File) {
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
            }
        }
    }

    @Throws(IOException::class)
    actual override fun updateConfigModelFromATLauncherInstance(packConfig: PackConfig, manifest: File) {
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

    @Throws(IOException::class)
    actual override fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(config)
        val loader = packConfig.modpackJson!!.get("loader")
        packConfig.modloader = getModLoaderCase(loader.get("loaderType").asText())
        packConfig.minecraftVersion = loader.get("mcVersion").asText()
        packConfig.modloaderVersion =
            loader.get("loaderVersion").asText().replace(packConfig.minecraftVersion + "-", "")
    }

    @Throws(IOException::class)
    actual override fun updateConfigModelFromMMCPack(packConfig: PackConfig, mmcPack: File) {
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
            }
        }
    }

    @Throws(IOException::class)
    actual override fun updateDestinationFromInstanceCfg(instanceCfg: File): String {
        var name: String
        instanceCfg.inputStream().use {
            val properties = Properties()
            properties.load(it)
            name = properties.getProperty("name", null)
        }
        return name
    }

    actual override fun checkModpackDir(
        modpackDir: String,
        configCheck: ConfigCheck,
        printLog: Boolean
    ): ConfigCheck {
        if (modpackDir.isEmpty()) {
            if (printLog) {
                log.error("Modpack directory not specified. Please specify an existing directory.")
            }
            configCheck.modpackErrors.add(Api.configuration_log_error_checkmodpackdir.toString())
        } else if (!File(modpackDir).exists()) {
            if (printLog) {
                log.warn("Couldn't find directory $modpackDir.")
            }
            configCheck.modpackErrors.add(Api.configuration_log_error_modpackdirectory(modpackDir))
        }
        return configCheck
    }

    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    actual override fun getAllFilesAndDirectoriesInModpackZip(zipFile: File): List<String> {
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

    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    actual override fun getDirectoriesInModpackZip(zipFile: java.io.File): List<String> {
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

    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    actual override fun getFilesInModpackZip(zipFile: java.io.File): List<String> {
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
}