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

import Api
import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.utilities.File
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.isNotValidZipFile
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import java.io.IOException
import java.nio.file.*
import java.util.*
import java.util.regex.PatternSyntaxException
import kotlin.io.path.moveTo

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
actual class ConfigurationHandler constructor(
    private val versionMeta: VersionMeta,
    private val apiProperties: ApiProperties,
    private val utilities: Utilities,
    private val apiPlugins: ApiPlugins
) : Configuration<File, Path>() {
    private val modpackRegex = "\\.[Zz][Ii][Pp]".toRegex()

    actual override fun checkConfiguration(
        configFile: File, packConfig: PackConfig, encounteredErrors: MutableList<String>, quietCheck: Boolean
    ): Boolean {
        try {
            val fileConf = PackConfig(utilities, configFile)
            packConfig.setClientMods(fileConf.clientMods)
            packConfig.setCopyDirs(fileConf.copyDirs)
            packConfig.modpackDir = fileConf.modpackDir
            packConfig.minecraftVersion = fileConf.minecraftVersion
            packConfig.modloader = fileConf.modloader
            packConfig.modloaderVersion = fileConf.modloaderVersion
            packConfig.javaArgs = fileConf.javaArgs
            packConfig.serverPackSuffix = fileConf.serverPackSuffix
            packConfig.serverIconPath = fileConf.serverIconPath
            packConfig.serverPropertiesPath = fileConf.serverPropertiesPath
            packConfig.isServerInstallationDesired = fileConf.isServerInstallationDesired
            packConfig.isServerIconInclusionDesired = fileConf.isServerIconInclusionDesired
            packConfig.isServerPropertiesInclusionDesired = fileConf.isServerPropertiesInclusionDesired
            packConfig.isZipCreationDesired = fileConf.isZipCreationDesired
            packConfig.setScriptSettings(fileConf.scriptSettings)
            packConfig.setPluginsConfigs(fileConf.pluginsConfigs)
        } catch (ex: Exception) {
            log.error(
                "Couldn't parse config file. Consider checking your config file and fixing empty values. If the value needs to be an empty string, leave its value to \"\"."
            )

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(
                Api.configuration_log_error_checkconfig_start.toString()
            )
        }
        return checkConfiguration(packConfig, encounteredErrors, quietCheck)
    }

    actual override fun checkConfiguration(
        packConfig: PackConfig, encounteredErrors: MutableList<String>, quietCheck: Boolean
    ): Boolean {
        var configHasError = false
        sanitizeLinks(packConfig)
        log.info("Checking configuration...")
        if (packConfig.clientMods.isEmpty()) {
            log.warn("No clientside-only mods specified. Using fallback list.")
            packConfig.setClientMods(apiProperties.clientSideMods())
        }
        if (!checkIconAndProperties(packConfig.serverIconPath)) {
            configHasError = true
            log.error("The specified server-icon does not exist: ${packConfig.serverIconPath}")
            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(
                Api.configuration_log_error_servericon(packConfig.serverIconPath)
            )
        } else if (packConfig.serverIconPath.isNotEmpty() && File(packConfig.serverIconPath).exists() && !utilities.fileUtilities.isReadPermissionSet(
                packConfig.serverIconPath
            )
        ) {
            configHasError = true
            log.error("No read-permission for ${packConfig.serverIconPath}")

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(
                Api.configuration_log_error_checkcopydirs_read(packConfig.serverIconPath)
            )
        }
        if (!checkIconAndProperties(packConfig.serverPropertiesPath)) {
            configHasError = true
            log.error("The specified server.properties does not exist: ${packConfig.serverPropertiesPath}")

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(
                Api.configuration_log_error_serverproperties(packConfig.serverPropertiesPath)
            )
        } else if (packConfig.serverPropertiesPath.isNotEmpty() && File(packConfig.serverPropertiesPath).exists() && !utilities.fileUtilities.isReadPermissionSet(
                packConfig.serverPropertiesPath
            )
        ) {
            configHasError = true
            log.error("No read-permission for ${packConfig.serverPropertiesPath}")

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(
                Api.configuration_log_error_checkcopydirs_read(packConfig.serverPropertiesPath)
            )
        }

        /*
         * Run checks on the specified modpack directory.
         * Depending on the setting, various configurations can be acquired automatically.
         *
         * 1. If the modpackDir is an actual directory, every check needs to run. Nothing can be acquired automatically, or rather, we want the user to set everything accordingly.
         * 2. If CurseForge is activated and the specified modpackDir is a CurseForge projectID and fileID combination, create the modpack and gather information from said CurseForge modpack.
         * 3. If the modpackDir is a ZIP-archive, check it and if it is found to be valid, extract it, gather as much information as possible.
         *
         * Last but by no means least: Run final checks.
         */
        val modpack = File(packConfig.modpackDir)
        if (modpack.isDirectory) {
            if (isDir(packConfig, encounteredErrors)) {
                configHasError = true
            }
        } else if (modpack.isFile && modpack.name.endsWith("zip")) {
            try {
                if (isZip(packConfig, encounteredErrors)) {
                    configHasError = true
                }
            } catch (ex: IOException) {
                configHasError = true
                log.error("An error occurred whilst working with the ZIP-archive.", ex)
            }
        } else {
            configHasError = true
            log.error("Modpack directory not specified. Please specify an existing directory. Specified: ${packConfig.modpackDir}")

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_checkmodpackdir.toString())
        }
        if (checkModloader(packConfig.modloader)) {
            if (versionMeta.minecraft.isMinecraftVersionAvailable(packConfig.minecraftVersion)) {
                log.debug("minecraftVersion setting check passed.")
                log.debug("modLoader setting check passed.")
                if (checkModloaderVersion(
                        packConfig.modloader,
                        packConfig.modloaderVersion,
                        packConfig.minecraftVersion,
                        encounteredErrors
                    )
                ) {
                    log.debug("modLoaderVersion setting check passed.")
                } else {
                    configHasError = true
                    log.error("There's something wrong with your modloader version setting.")

                    // This log is meant to be read by the user, therefore we allow translation.
                    encounteredErrors.add(Api.configuration_log_error_checkmodloaderversion.toString())
                }
            } else {
                configHasError = true
                log.error("There's something wrong with your Minecraft version setting.")

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_minecraft.toString())
            }
        } else {
            configHasError = true
            log.error("There's something wrong with your modloader or modloader version setting.")

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_checkmodloader.toString())
        }
        if (apiPlugins.runConfigCheckExtensions(packConfig, encounteredErrors)) {
            configHasError = true
        }
        if (quietCheck) {
            printConfigurationModel(packConfig)
        }
        if (!configHasError) {
            log.info("Config check successful. No errors encountered.")
        } else {
            log.error("Config check not successful. Check your config for errors.")
            printEncounteredErrors(encounteredErrors)
        }
        ensureScriptSettingsDefaults(packConfig)
        return configHasError
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
        if (packConfig.copyDirs.isNotEmpty()) {
            val copyDirs: ArrayList<String> = packConfig.copyDirs
            var copyDirChanges = false
            var link: String
            for (i in packConfig.copyDirs.indices) {
                val entry = copyDirs[i]
                val equalsArray = entry.split("==").dropLastWhile { it.isEmpty() }.toTypedArray()
                val semicolonArray = entry.split(";").dropLastWhile { it.isEmpty() }.toTypedArray()
                val fileUtils = utilities.fileUtilities
                try {
                    if (!entry.startsWith("==") && entry.contains("==") && equalsArray.size == 2) {

                        if (fileUtils.isLink(equalsArray[0])) {
                            link = fileUtils.resolveLink(equalsArray[0])
                            copyDirs[i] = "$link==${equalsArray[1]}"
                            log.info("Resolved regex-directory link to: ${copyDirs[i]}")
                            copyDirChanges = true
                        }

                    } else if (entry.contains(";") && semicolonArray.size == 2) {

                        // Source;Destination-combination
                        if (fileUtils.isLink(semicolonArray[0])) {
                            link = fileUtils.resolveLink(semicolonArray[0])
                            copyDirs[i] = "$link;${semicolonArray[1]}"
                            log.info("Resolved copy-directories link to: ${copyDirs[i]}")
                            copyDirChanges = true

                        } else if (fileUtils.isLink("${packConfig.modpackDir}${File.separator}${semicolonArray[0]}")) {
                            val file = "${packConfig.modpackDir}${File.separator}${semicolonArray[0]}"
                            link = fileUtils.resolveLink(file)
                            copyDirs[i] = "$link;${semicolonArray[1]}"
                            log.info("Resolved copy-directories link to: ${copyDirs[i]}")
                            copyDirChanges = true
                        }

                    } else if (entry.startsWith("!")) {

                        if (entry.contains("==") && equalsArray.size == 2) {
                            val file = equalsArray[0].substring(1)
                            if (fileUtils.isLink(file)) {
                                link = fileUtils.resolveLink(file)
                                val target = equalsArray[1]
                                copyDirs[i] = "!$link==$target"
                                log.info("Resolved regex-directory link to: ${copyDirs[i]}")
                                copyDirChanges = true
                            }

                        } else if (fileUtils.isLink(entry.substring(1))) {
                            val file = entry.substring(1)
                            val resolved = fileUtils.resolveLink(file)
                            copyDirs[i] = "!$resolved"
                            log.info("Resolved copy-directories link to: ${copyDirs[i]}")
                            copyDirChanges = true

                        } else if (fileUtils.isLink("${packConfig.modpackDir}${File.separator}${entry.substring(1)}")) {
                            val file = "!${packConfig.modpackDir}${File.separator}${entry.substring(1)}"
                            copyDirs[i] = fileUtils.resolveLink(file)
                            log.info("Resolved copy-directories link to: ${copyDirs[i]}")
                            copyDirChanges = true

                        }
                    } else if (fileUtils.isLink(entry)) {
                        // Regular entry, may be absolute path or relative one
                        copyDirs[i] = fileUtils.resolveLink(entry)
                        log.info("Resolved to: ${packConfig.modpackDir}")
                        copyDirChanges = true

                    } else if (fileUtils.isLink(packConfig.modpackDir + File.separator + entry)) {
                        copyDirs[i] = fileUtils.resolveLink("${packConfig.modpackDir}${File.separator}$entry")
                        log.info("Resolved copy-directories link to: ${copyDirs[i]}")
                        copyDirChanges = true

                    }

                } catch (ex: InvalidFileTypeException) {
                    log.error("Couldn't resolve link for copy-directories entry: ${copyDirs[i]}", ex)
                } catch (ex: IOException) {
                    log.error("Couldn't resolve link for copy-directories entry: ${copyDirs[i]}", ex)
                }
            }
            if (copyDirChanges) {
                packConfig.setCopyDirs(copyDirs)
            }
        }
    }

    actual override fun checkIconAndProperties(iconOrPropertiesPath: String) = if (iconOrPropertiesPath.isEmpty()) {
        true
    } else {
        File(iconOrPropertiesPath).isFile
    }

    actual override fun isDir(packConfig: PackConfig, encounteredErrors: MutableList<String>): Boolean {
        var configHasError = false
        if (checkCopyDirs(packConfig.copyDirs, packConfig.modpackDir, encounteredErrors)) {
            log.debug("copyDirs setting check passed.")
        } else {
            configHasError = true
            log.error("There's something wrong with your setting of directories to include in your server pack.")
            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_isdir_copydir.toString())
        }
        return configHasError
    }

    @Throws(IOException::class)
    actual override fun isZip(packConfig: PackConfig, encounteredErrors: MutableList<String>): Boolean {
        var configHasError = false

        // modpackDir points at a ZIP-file. Get the path to the would be modpack directory.
        val name = File(packConfig.modpackDir).name
        val cleaned = name.replace(modpackRegex, "")
        var unzippedModpack = "${apiProperties.modpacksDirectory}${File.separator}$cleaned"
        if (checkZipArchive(Paths.get(packConfig.modpackDir).toAbsolutePath().toString(), encounteredErrors)) {
            return true
        }

        // Does the modpack extracted from the ZIP-archive already exist?
        unzippedModpack = unzipDestination(unzippedModpack)

        // Extract the archive to the modpack directory.
        utilities.fileUtilities.unzipArchive(packConfig.modpackDir, unzippedModpack)

        // Expand the already set copyDirs with suggestions from extracted ZIP-archive.
        val newCopyDirs = suggestCopyDirs(unzippedModpack)
        for (entry in packConfig.copyDirs) {
            if (!newCopyDirs.contains(entry)) {
                newCopyDirs.add(entry)
            }
        }
        packConfig.setCopyDirs(newCopyDirs)

        // If various manifests exist, gather as much information as possible.
        // Check CurseForge manifest available if a modpack was exported through a client like
        // Overwolf's CurseForge or through GDLauncher.
        val amountOfErrors = encounteredErrors.size

        var packName = checkManifests(unzippedModpack, packConfig, encounteredErrors)
        if (encounteredErrors.size > amountOfErrors) {
            configHasError = true
        }

        // If no json was read from the modpack, we must sadly use the ZIP-files name as the new
        // destination. Sad-face.
        if (packName == null) {
            packName = unzippedModpack
        }
        packName = File(utilities.stringUtilities.pathSecureTextAlternative(packName)).path

        // Get the path to the would-be-server-pack with the new destination.
        var createServerPackFrom = File(
            apiProperties.serverPacksDirectory, File(packName!!).name + packConfig.serverPackSuffix
        ).absolutePath

        // Check whether a server pack for the new destination already exists.
        // If it does, we need to change it to avoid overwriting any existing files.
        createServerPackFrom = checkServerPacksForIncrement(unzippedModpack, createServerPackFrom)

        // Finally, move to new destination to avoid overwriting of server pack
        if (!File(unzippedModpack).name.equals(File(createServerPackFrom).name)) {
            File(unzippedModpack).toPath().moveTo(File(createServerPackFrom).toPath(), overwrite = true)
        }

        // Last but not least, use the newly acquired packname as the modpack directory.
        packConfig.modpackDir = createServerPackFrom

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
        return configHasError
    }

    actual override fun checkModloaderVersion(
        modloader: String, modloaderVersion: String, minecraftVersion: String, encounteredErrors: MutableList<String>
    ) = when (modloader) {
        "Forge" -> if (versionMeta.forge.isForgeAndMinecraftCombinationValid(minecraftVersion, modloaderVersion)) {
            true
        } else {
            encounteredErrors.add(
                Api.configuration_log_error_checkmodloaderandversion(
                    minecraftVersion, modloader, modloaderVersion
                )
            )
            false
        }

        "Fabric" -> if (versionMeta.fabric.isVersionValid(modloaderVersion) && versionMeta.fabric.getLoaderDetails(
                minecraftVersion,
                modloaderVersion
            ).isPresent
        ) {
            true
        } else {
            encounteredErrors.add(
                Api.configuration_log_error_checkmodloaderandversion(
                    minecraftVersion, modloader, modloaderVersion
                )
            )
            false
        }

        "Quilt" -> if (versionMeta.quilt.isVersionValid(modloaderVersion) && versionMeta.fabric.isMinecraftSupported(
                minecraftVersion
            )
        ) {
            true
        } else {
            encounteredErrors.add(
                Api.configuration_log_error_checkmodloaderandversion(
                    minecraftVersion, modloader, modloaderVersion
                )
            )
            false
        }

        "LegacyFabric" -> if (versionMeta.legacyFabric.isVersionValid(modloaderVersion) && versionMeta.legacyFabric.isMinecraftSupported(
                minecraftVersion
            )
        ) {
            true
        } else {
            encounteredErrors.add(
                Api.configuration_log_error_checkmodloaderandversion(
                    minecraftVersion, modloader, modloaderVersion
                )
            )
            false
        }

        else -> {
            log.error(
                "Specified incorrect modloader version. Please check your modpack for the correct version and enter again."
            )
            false
        }
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

    actual override fun checkCopyDirs(
        directoriesToCopy: MutableList<String>,
        modpackDir: String,
        encounteredErrors: MutableList<String>,
        printLog: Boolean
    ): Boolean {
        var configCorrect = true
        directoriesToCopy.removeIf { entry: String? -> entry!!.matches(whitespace) || entry.isEmpty() }
        if (directoriesToCopy.isEmpty()) {
            configCorrect = false
            if (printLog) {
                log.error("No directories or files specified for copying. This would result in an empty server pack.")
            }
            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_empty.toString())
        } else if (directoriesToCopy.size == 1 && directoriesToCopy[0] == "lazy_mode") {
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
            if (directoriesToCopy.size > 1 && directoriesToCopy.contains("lazy_mode") && printLog) {
                log.warn(
                    "You specified lazy mode in your configuration, but your copyDirs configuration contains other"
                            + " entries. To use the lazy mode, only specify \"lazy_mode\" and nothing else. Ignoring lazy mode."
                )
            }
            directoriesToCopy.removeIf { entry: String? -> entry == "lazy_mode" }
            for (directory in directoriesToCopy) {

                // Check whether the user specified a source;destination-combination
                if (directory.contains(";")) {
                    val sourceDestinationCombo = directory.split(";").dropLastWhile { it.isEmpty() }.toTypedArray()
                    val modpackSource = File(modpackDir, sourceDestinationCombo[0])
                    val source = File(sourceDestinationCombo[0])
                    if (!modpackSource.isFile && !modpackSource.isDirectory && !source.isFile && !source.isDirectory) {
                        configCorrect = false
                        if (printLog) {
                            log.error("Copy-file $modpackSource does not exist. Please specify existing files.")
                        }
                        // This log is meant to be read by the user, therefore we allow translation.
                        encounteredErrors.add(
                            Api.configuration_log_error_checkcopydirs_filenotfound(
                                modpackSource
                            )
                        )
                    } else {
                        val fileThree = "$modpackDir${File.separator}${sourceDestinationCombo[0]}"
                        if (modpackSource.exists() && !utilities.fileUtilities.isReadPermissionSet(fileThree)) {
                            configCorrect = false
                            if (printLog) {
                                log.error("No read-permission for $fileThree")
                            }
                            // This log is meant to be read by the user, therefore we allow translation.
                            encounteredErrors.add(
                                Api.configuration_log_error_checkcopydirs_read(fileThree)
                            )
                        } else if (modpackSource.exists() && !utilities.fileUtilities.isReadPermissionSet(fileThree)) {
                            configCorrect = false
                            if (printLog) {
                                log.error("No read-permission for $fileThree")
                            }
                            // This log is meant to be read by the user, therefore we allow translation.
                            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(fileThree))
                        } else if (source.exists()
                            && !utilities.fileUtilities.isReadPermissionSet(sourceDestinationCombo[0])
                        ) {
                            configCorrect = false
                            if (printLog) {
                                log.error("No read-permission for ${sourceDestinationCombo[0]}")
                            }
                            // This log is meant to be read by the user, therefore we allow translation.
                            encounteredErrors.add(
                                Api.configuration_log_error_checkcopydirs_read(
                                    sourceDestinationCombo[0]
                                )
                            )
                        }
                        if (modpackSource.isDirectory) {
                            for (file in modpackSource.listFiles()!!) {
                                if (!utilities.fileUtilities.isReadPermissionSet(file)) {
                                    configCorrect = false
                                    if (printLog) {
                                        log.error("No read-permission for $file")
                                    }
                                    // This log is meant to be read by the user, therefore we allow translation.
                                    encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(file))
                                }
                            }
                        } else if (source.isDirectory) {
                            for (file in source.listFiles()!!) {
                                if (!utilities.fileUtilities.isReadPermissionSet(file)) {
                                    configCorrect = false
                                    if (printLog) {
                                        log.error("No read-permission for $file")
                                    }
                                    // This log is meant to be read by the user, therefore we allow translation.
                                    encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(file))
                                }
                            }
                        }
                    }

                    // Add an entry to the list of directories/files to exclude if it starts with !
                } else if (directory.startsWith("!")) {
                    val toExclude = directory.substring(1)
                    if (toExclude.contains("==")) {
                        if (!checkRegex(modpackDir, directory, true, encounteredErrors)) {
                            configCorrect = false
                        }
                    } else {
                        val fileOrDirectory = File(modpackDir, toExclude)
                        if (fileOrDirectory.isFile && printLog) {
                            log.warn("File $toExclude will be ignored.")
                        } else if (fileOrDirectory.isDirectory && printLog) {
                            log.warn("Directory $toExclude will be ignored.")
                        } else if (printLog) {
                            log.warn("Could not determine whether $fileOrDirectory is a file or directory.")
                        }
                    }
                } else if (directory.contains("==")) {
                    if (!checkRegex(modpackDir, directory, false, encounteredErrors)) {
                        configCorrect = false
                    }

                    // Check if the entry exists
                } else {
                    val dirToCheck = File(modpackDir, directory)
                    if (!dirToCheck.exists() && !File(directory).exists() && !File(directory).isFile && !File(directory).isDirectory) {
                        configCorrect = false
                        if (printLog) {
                            log.error(
                                "Copy-file or copy-directory $directory does not exist. Please specify existing directories or files."
                            )
                        }
                        // This log is meant to be read by the user, therefore we allow translation.
                        encounteredErrors.add(Api.configuration_log_error_checkcopydirs_notfound(directory))
                    } else {
                        if (dirToCheck.exists() && !utilities.fileUtilities.isReadPermissionSet(dirToCheck)) {
                            configCorrect = false
                            if (printLog) {
                                log.error("No read-permission for $dirToCheck")
                            }
                            // This log is meant to be read by the user, therefore we allow translation.
                            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(dirToCheck))
                        } else if (File(directory).exists() && !utilities.fileUtilities.isReadPermissionSet(directory)) {
                            configCorrect = false
                            if (printLog) {
                                log.error("No read-permission for $directory")
                            }
                            // This log is meant to be read by the user, therefore we allow translation.
                            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(directory))
                        }
                        if (dirToCheck.isDirectory) {
                            for (file in dirToCheck.listFiles()!!) {
                                if (!utilities.fileUtilities.isReadPermissionSet(file)) {
                                    configCorrect = false
                                    if (printLog) {
                                        log.error("No read-permission for $file")
                                    }
                                    // This log is meant to be read by the user, therefore we allow translation.
                                    encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(file))
                                }
                            }
                        } else if (File(directory).isDirectory) {
                            for (file in File(directory).listFiles()!!) {
                                if (!utilities.fileUtilities.isReadPermissionSet(file)) {
                                    configCorrect = false
                                    if (printLog) {
                                        log.error("No read-permission for $file")
                                    }
                                    // This log is meant to be read by the user, therefore we allow translation.
                                    encounteredErrors.add(Api.configuration_log_error_checkcopydirs_read(file))
                                }
                            }
                        }
                    }
                }
            }
        }
        return configCorrect
    }

    actual override fun checkZipArchive(pathToZip: String, encounteredErrors: MutableList<String>): Boolean {
        try {
            ZipFile(Paths.get(pathToZip).toFile()).use {
                if (it.isNotValidZipFile()) {
                    return true
                }
            }
        } catch (ex: IOException) {
            log.error("Could not validate ZIP-file $pathToZip.", ex)
            return true
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
                encounteredErrors.add(Api.configuration_log_error_zip_overrides(foldersInModpackZip[0]))
                return true

                // If the ZIP-file does not contain the mods or config directories, consider it invalid.
            } else if (!foldersInModpackZip.contains("mods/") || !foldersInModpackZip.contains("config/")) {
                log.error(
                    "The ZIP-file you specified does not contain the mods or config directories. What use is a modded server without mods and their configurations?"
                )

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_modsorconfig.toString())
                return true
            }
        } catch (ex: IOException) {
            log.error("Couldn't acquire directories in ZIP-file.", ex)

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_zip_directories.toString())
            return true
        }
        return false
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

    actual override fun suggestCopyDirs(modpackDir: String): ArrayList<String> {
        // This log is meant to be read by the user, therefore we allow translation.
        log.info("Preparing a list of directories to include in server pack...")
        val listDirectoriesInModpack = File(modpackDir).listFiles()
        val dirsInModpack: ArrayList<String> = ArrayList(100)
        try {
            assert(listDirectoriesInModpack != null)
            for (dir in listDirectoriesInModpack!!) {
                if (dir.isDirectory) {
                    dirsInModpack.add(dir.name)
                }
            }
        } catch (np: NullPointerException) {
            log.error(
                "Error: Something went wrong during the setup of the modpack. Copy dirs should never be empty. Please check the logs for errors and open an issue on https://github.com/Griefed/ServerPackCreator/issues.",
                np
            )
        }
        for (i in apiProperties.directoriesToExclude.indices) {
            dirsInModpack.removeIf { it.contains(apiProperties.directoriesToExclude.toList()[i]) }
        }
        log.info("Modpack directory checked. Suggested directories for copyDirs-setting are: $dirsInModpack")
        return dirsInModpack
    }

    actual override fun checkManifests(
        destination: String, packConfig: PackConfig, encounteredErrors: MutableList<String>
    ): String? {
        var packName: String? = null
        val manifest = File(destination, "manifest.json")
        val minecraftInstance = File(destination, "minecraftinstance.json")
        val modrinth = File(destination, "modrinth.index.json")
        val instance = File(destination, "instance.json")
        val config = File(destination, "config.json")
        val mmc = File(destination, "mmc-pack.json")
        val instanceConfig = File(destination, "instance.cfg")
        if (manifest.exists()) {
            try {
                updateConfigModelFromCurseManifest(packConfig, manifest)
                packName = updatePackName(packConfig, "name")
            } catch (ex: IOException) {
                log.error("Error parsing CurseForge manifest.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_manifest.toString())
            }

            // Check minecraftinstance.json usually created by Overwolf's CurseForge launcher.
        } else if (minecraftInstance.exists()) {
            try {
                updateConfigModelFromMinecraftInstance(packConfig, minecraftInstance)
                packName = updatePackName(packConfig, "name")
            } catch (ex: IOException) {
                log.error("Error parsing minecraftinstance.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_instance.toString())
            }

            // Check modrinth.index.json usually available if the modpack is from Modrinth
        } else if (modrinth.exists()) {
            try {
                updateConfigModelFromModrinthManifest(packConfig, modrinth)
                packName = updatePackName(packConfig, "name")
            } catch (ex: IOException) {
                log.error("Error parsing modrinth.index.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_config.toString())
            }

            // Check instance.json usually created by ATLauncher
        } else if (instance.exists()) {
            try {
                updateConfigModelFromATLauncherInstance(packConfig, File(destination, "instance.json"))

                // If JSON was acquired, get the name of the modpack and overwrite newDestination using
                // modpack name.
                packConfig.modpackJson?.let {
                    utilities.jsonUtilities.getNestedText(
                        it, "launcher", "name"
                    )
                }
                packName = packConfig.modpackJson?.let {
                    utilities.jsonUtilities.getNestedText(
                        it, "launcher", "name"
                    )
                }
            } catch (ex: IOException) {
                log.error("Error parsing config.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_config.toString())
            }

            // Check the config.json usually created by GDLauncher.
        } else if (config.exists()) {
            try {
                updateConfigModelFromConfigJson(packConfig, config)

                // If JSON was acquired, get the name of the modpack and overwrite newDestination using
                // modpack name.
                packName = packConfig.modpackJson?.let {
                    utilities.jsonUtilities.getNestedText(
                        it, "loader", "sourceName"
                    )
                }
            } catch (ex: IOException) {
                log.error("Error parsing config.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_config.toString())
            }

            // Check mmc-pack.json usually created by MultiMC.
        } else if (mmc.exists()) {
            try {
                updateConfigModelFromMMCPack(packConfig, mmc)
            } catch (ex: IOException) {
                log.error("Error parsing mmc-pack.json from ZIP-file.", ex)

                // This log is meant to be read by the user, therefore we allow translation.
                encounteredErrors.add(Api.configuration_log_error_zip_mmcpack.toString())
            }
            try {
                if (instanceConfig.exists()) {
                    packName = updateDestinationFromInstanceCfg(instanceConfig)
                }
            } catch (ex: IOException) {
                log.error("Couldn't read instance.cfg.", ex)
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
        copyDirectories: List<String>,
        installServer: Boolean,
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
        log.info("Directories to copy:")
        for (directory in copyDirectories) {
            log.info("    %s".format(directory))
        }
        // This log is meant to be read by the user, therefore we allow translation.
        log.info("Include server installation:       $installServer")
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

    actual override fun checkRegex(
        modpackDir: String, entry: String, exclusion: Boolean, encounteredErrors: MutableList<String>
    ) = try {
        if (exclusion) {
            exclusionRegexCheck(modpackDir, entry, encounteredErrors)
        } else {
            inclusionRegexCheck(modpackDir, entry, encounteredErrors)
        }
    } catch (ex: PatternSyntaxException) {
        log.error("Invalid regex specified: $entry. Error near regex-index ${ex.index}.")
        encounteredErrors.add(
            Api.configuration_log_error_checkcopydirs_checkforregex_invalid_regex(
                entry, ex.index
            )
        )
        false
    }

    @Throws(
        IllegalArgumentException::class,
        FileSystemAlreadyExistsException::class,
        ProviderNotFoundException::class,
        IOException::class,
        SecurityException::class
    )
    actual override fun getDirectoriesInModpackZipBaseDirectory(zipFile: File): List<String> {
        val baseDirectories: MutableList<String> = ArrayList(100)
        ZipFile(zipFile).use {
            val headers = it.fileHeaders
            for (header in headers) {
                if (header.fileName.matches(zipCheck)) {
                    baseDirectories.add(header.fileName)
                }
            }
        }
        return baseDirectories
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
        val base = packConfig.modpackJson!!.get("baseModLoader")
        val modloaderInfo = base.get("name").asText()
        val modloader = modloaderInfo.split("-")[0]
        packConfig.modloader = getModLoaderCase(modloader)
        packConfig.modloaderVersion = base.get("forgeVersion").asText()
        packConfig.minecraftVersion = base.get("minecraftVersion").asText()
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
        packConfig.minecraftVersion = packConfig.modpackJson!!.get("id").asText()
        val launcher = packConfig.modpackJson!!.get("launcher")
        val loaderVersion = launcher.get("loaderVersion")
        packConfig.modloader = loaderVersion.get("type").asText()
        packConfig.modloaderVersion = loaderVersion.get("version").asText()
    }

    @Throws(IOException::class)
    actual override fun updateConfigModelFromConfigJson(packConfig: PackConfig, config: File) {
        packConfig.modpackJson = utilities.jsonUtilities.getJson(config)
        val loader = packConfig.modpackJson!!.get("loader")
        packConfig.modloader = getModLoaderCase(loader.get("loaderType").asText())
        packConfig.minecraftVersion = loader.get("mcVersion").asText()
        packConfig.modloaderVersion = loader.get("loaderVersion").asText().replace(packConfig.minecraftVersion + "-", "")
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

    actual override fun exclusionRegexCheck(modpackDir: String, entry: String, encounteredErrors: MutableList<String>) =
        if (entry.startsWith("!==") && entry.length > 3) {
            val source = File(modpackDir)
            countRegexMatches(source, entry.toRegex())
            true
        } else if (entry.contains("==") && entry.split("==").dropLastWhile { it.isEmpty() }.toTypedArray().size == 2) {
            val sourceRegex = entry.split("==").dropLastWhile { it.isEmpty() }.toTypedArray()

            /*
            * Matches inside modpack-directory
            */
            val file = File(modpackDir, sourceRegex[0].substring(1))
            val sourceFile = File(sourceRegex[0].substring(1))
            if (file.isDirectory) {
                countRegexMatches(file, sourceRegex[1].toRegex())
                true

                /*
                * Matches inside directory outside modpack-directory
                */
            } else if (sourceFile.isDirectory) {
                countRegexMatches(sourceFile, sourceRegex[1].toRegex())
                true
            } else {
                encounteredErrors.add(Api.configuration_log_error_checkcopydirs_checkforregex(sourceRegex[0]))
                false
            }
        } else {
            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_checkforregex_invalid.toString())
            false
        }

    actual override fun inclusionRegexCheck(modpackDir: String, entry: String, encounteredErrors: MutableList<String>) =
        if (entry.startsWith("==") && entry.length > 2) {
            val source = File(modpackDir)
            countRegexMatches(source, entry.toRegex())
            true

            /*
            * Check for matches in the specified directory
            */
        } else if (entry.contains("==") && entry.split("==").dropLastWhile { it.isEmpty() }.toTypedArray().size == 2) {
            val sourceRegex = entry.split("==").dropLastWhile { it.isEmpty() }.toTypedArray()

            /*
            * Matches inside modpack-directory
            */
            val file = File(modpackDir, sourceRegex[0])
            val sourceFile = File(sourceRegex[0])
            if (file.isDirectory) {
                countRegexMatches(file, sourceRegex[1].toRegex())
                true

                /*
                 * Matches inside directory outside modpack-directory
                 */
            } else if (sourceFile.isDirectory) {
                countRegexMatches(sourceFile, sourceRegex[1].toRegex())
                true
            } else {
                encounteredErrors.add(Api.configuration_log_error_checkcopydirs_checkforregex(sourceRegex[0]))
                false
            }
        } else {
            encounteredErrors.add(Api.configuration_log_error_checkcopydirs_checkforregex_invalid.toString())
            false
        }

    actual override fun countRegexMatches(source: File, regex: Regex) {
        var counter = 0
        var toMatch: String
        try {
            Files.walk(source.toPath()).use {
                for (path in it) {
                    toMatch = path.toFile().absolutePath.replace(
                        source.absolutePath, ""
                    )
                    if (toMatch.startsWith(File.separator)) {
                        toMatch = toMatch.substring(1)
                    }
                    if (toMatch.matches(regex)) {
                        counter += 1
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("Could not check your regex entry \"$regex\" in directory $source", ex)
        }
        log.info(
            "Regex \"$regex\" matched $counter files/folders."
        )
    }

    actual override fun checkModpackDir(
        modpackDir: String,
        encounteredErrors: MutableList<String>,
        printLog: Boolean
    ): Boolean {
        var configCorrect = false
        if (modpackDir.isEmpty()) {
            if (printLog) {
                log.error("Modpack directory not specified. Please specify an existing directory.")
            }

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_checkmodpackdir.toString())
        } else if (!File(modpackDir).isDirectory) {
            if (printLog) {
                log.warn("Couldn't find directory $modpackDir.")
            }

            // This log is meant to be read by the user, therefore we allow translation.
            encounteredErrors.add(Api.configuration_log_error_modpackdirectory(modpackDir))
        } else {
            configCorrect = true
        }
        return configCorrect
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