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
package de.griefed.serverpackcreator.api.serverpack

import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.config.InclusionSpecification
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.utilities.*
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.PatternSyntaxException
import javax.imageio.ImageIO
import kotlin.io.path.absolute

/**
 * Everything revolving around creating a server pack. The intended workflow is to create a [PackConfig] and run
 * it through any of the available [de.griefed.serverpackcreator.api.config.ConfigurationHandler.checkConfiguration]-variants, and then call [run] with the
 * previously checked configuration model. You may run with an unchecked configuration model, but no guarantees or
 * promises, yes not even support, is given for running a model without checking it first.
 *
 * This class also gives you access to the methods which are responsible for creating the server pack, in case you want
 * to do things manually.
 *
 * The methods in question are:
 *  * [cleanupEnvironment]
 *  * [ApiPlugins.runPreZipExtensions]
 *  * [copyFiles]
 *  * [getImprovedFabricLauncher] if Fabric is the chosen Modloader
 *  * [copyIcon]
 *  * [copyProperties]
 *  * [ApiPlugins.runPreZipExtensions]
 *  * [zipBuilder]
 *  * [createServerRunFiles]
 *  * [ApiPlugins.runPostGenExtensions]
 *
 * If you want to execute extensions, see
 * * [ApiPlugins.runPreGenExtensions]},
 * * [ApiPlugins.runPreZipExtensions]} and
 * * [ApiPlugins.runPostGenExtensions].
 *
 * @param apiProperties Base settings of ServerPackCreator needed for server pack generation, such as access to the
 * directories, script templates and so on.
 * @param versionMeta   Meta for modloader and version specific checks and information gathering, such as modloader
 * installer downloads.
 * @param utilities     Common utilities used across ServerPackCreator.
 * @param apiPlugins    Any addons which a user may want to execute during the generation of a server pack.
 * @param modScanner    In case a user enabled automatic sideness detection, this will exclude clientside-only mods
 * from a server pack.
 *
 * @author Griefed
 */
class ServerPackHandler(
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val apiPlugins: ApiPlugins,
    private val modScanner: ModScanner
) {

    val log by lazy { cachedLoggerOf(this.javaClass) }
    val modFileEndings = listOf("jar", "disabled")

    /**
     * Compiler of the mod-list, excluding clientside-only mods and honoring the whitelist.
     */
    val modListCompiler = ModListCompiler(apiProperties, modScanner)

    /**
     * Gatherer of all files which make up the server pack.
     */
    val fileGatherer = ServerPackFileGatherer(modListCompiler)

    /**
     * Provisioner for icon, properties, start-scripts, ZIP-archive and installer-extras.
     */
    val provisioner = ServerPackProvisioner(apiProperties, versionMeta, utilities)

    /**
     * Content of the variables.txt-file written to every server pack.
     */
    val variables: String get() = provisioner.variables

    private val spcGenericEventListeners: ArrayList<SPCGenericListener> = ArrayList(0)
    private val spcPreServerPackGenerationListener: ArrayList<SPCPreServerPackGenerationListener> = ArrayList(0)
    private val spcPreServerPackZipListener: ArrayList<SPCPreServerPackZipListener> = ArrayList(0)
    private val spcPostGenListener: ArrayList<SPCPostGenListener> = ArrayList(0)

    fun addEventListener(genericEventListener: SPCGenericListener) {
        spcGenericEventListeners.add(genericEventListener)
    }

    fun addEventListener(preServerPackGenerationListener: SPCPreServerPackGenerationListener) {
        spcPreServerPackGenerationListener.add(preServerPackGenerationListener)
    }

    fun addEventListener(preServerPackZipListener: SPCPreServerPackZipListener) {
        spcPreServerPackZipListener.add(preServerPackZipListener)
    }

    fun addEventListener(postGenListener: SPCPostGenListener) {
        spcPostGenListener.add(postGenListener)
    }

    private fun runGenericEventListeners() {
        for (listener in spcGenericEventListeners) {
            listener.run()
        }
    }

    private fun runPreServerPackGenerationListeners(packConfig: PackConfig, serverPackPath: Path) {
        for (listener in spcPreServerPackGenerationListener) {
            listener.run(packConfig, serverPackPath)
        }
    }

    private fun runPreServerPackZipListeners(packConfig: PackConfig, serverPackPath: Path) {
        for (listener in spcPreServerPackZipListener) {
            listener.run(packConfig, serverPackPath)
        }
    }

    private fun runPostGenListeners(packConfig: PackConfig, serverPackPath: Path) {
        for (listener in spcPostGenListener) {
            listener.run(packConfig, serverPackPath)
        }
    }

    /**
     * Acquire the destination directory in which the server pack will be generated. The directory in
     * which the server pack will be created has all its spaces replaces with underscores, so
     * `Survive Create Prosper 4 - 5.0.1` would become `Survive_Create_Prosper_4_-_5.0.1 `
     * Even though it is the year 2022, spaces in paths can and do still cause trouble. Such as for
     * Powershell scripts. Powershell throws a complete fit if the path contains spaces....so, we
     * remove them. Better safe than sorry.
     *
     * @param packConfig Model containing the modpack directory of the modpack from which the
     * server pack will be generated.
     * @return The complete path to the directory in which the server pack will be generated.
     * @author Griefed
     */
    fun getServerPackDestination(packConfig: PackConfig): String {
        var serverPackToBe = if (packConfig.name != null) {
            packConfig.name!!
        } else {
            File(packConfig.modpackDir).name
        }
        serverPackToBe += packConfig.serverPackSuffix
        serverPackToBe = StringUtilities.pathSecureText(serverPackToBe.replace(" ", "_"))
        return File(apiProperties.serverPacksDirectory, serverPackToBe).absolutePath
    }

    /**
     * Create a server pack from a given instance of [PackConfig].
     *
     * @param packConfig An instance of [PackConfig] which contains the
     * configuration of the modpack from which the server pack is to be
     * created.
     * @return `true` if the server pack was successfully generated.
     * @author Griefed
     */
    fun run(packConfig: PackConfig): ServerPackGeneration {
        val files : ArrayList<File> = ArrayList(10000)
        val relativeFiles : ArrayList<String> = ArrayList(10000)
        @Suppress("JoinDeclarationAndAssignment") val serverPackManifest: ServerPackManifest
        var serverPackZip: Optional<File> = Optional.empty()
        val serverPack = if (packConfig.customDestination.isPresent) {
            packConfig.customDestination.get()
        } else {
            File(getServerPackDestination(packConfig))
        }
        val existingManifest = File(serverPack.absolutePath, "manifest.json")
        val oldManifest: ServerPackManifest
        var oldFile: File
        val generationStopWatch = SimpleStopWatch().start()

        /*
        * Check whether the server pack for the specified modpack already exists and whether overwrite is disabled.
        * If the server pack exists and overwrite is disabled, no new server pack will be generated.
        */
        if (apiProperties.isServerPacksOverwriteEnabled) {
            // Make sure no files from previously generated server packs interrupt us.
            cleanupEnvironment(true, serverPack.absolutePath)
        } else {
            log.info("Overwrite disabled, not performing cleanup before server pack generation.")
            deleteExistingServerPackZip(serverPack.absolutePath)
        }

        try {
            serverPack.create(createFileOrDir = true, asDirectory = true)
        } catch (_: IOException) {
            // The server-pack directory may already exist; a genuine inability to create it would
            // surface later when files are written into it during generation.
        }

        if (apiProperties.isUpdatingServerPacksEnabled && existingManifest.isFile) {
            oldManifest = utilities.jsonUtilities.objectMapper.readValue(existingManifest, ServerPackManifest::class.java)
            for (entry in oldManifest.files) {
                oldFile = File(serverPack.absolutePath, entry)
                //I know, .isFile is only true if it's really just a file...checking for !dir is still safer.
                if (oldFile.isFile && !oldFile.isDirectory) {
                    log.debug("Deleting old file: ${oldFile.absolutePath}")
                    oldFile.deleteQuietly()
                }
            }
        }

        apiPlugins.runPreGenExtensions(packConfig, serverPack.absolutePath)
        runPreServerPackGenerationListeners(packConfig, serverPack.absoluteFile.toPath())
        runGenericEventListeners()

        // Recursively copy all specified directories and files, excluding clientside-only mods, to server pack.
        files.addAll(
            copyFiles(
                packConfig.modpackDir,
                packConfig.inclusions,
                packConfig.clientMods,
                packConfig.modsWhitelist,
                packConfig.minecraftVersion,
                serverPack.absolutePath,
                packConfig.modloader,
                !(!apiProperties.isServerPacksOverwriteEnabled && !apiProperties.isUpdatingServerPacksEnabled)
            )
        )

        // If true, copy the server-icon.png from server_files to the server pack.
        if (packConfig.isServerIconInclusionDesired) {
            copyIcon(serverPack.absolutePath, packConfig.serverIconPath)
        } else {
            log.info("Not including servericon.")
        }

        // If true, copy the server.properties from server_files to the server pack.
        if (packConfig.isServerPropertiesInclusionDesired) {
            copyProperties(serverPack.absolutePath, packConfig.serverPropertiesPath)
        } else {
            log.info("Not including server.properties.")
        }
        relativeFiles.addAll(files
            .map { file -> file.absolutePath }
            .map { entry -> entry.replace(serverPack.absolutePath,"")}
            .map { entry -> entry.substring(1) })
        serverPackManifest = ServerPackManifest(
            relativeFiles,
            packConfig.minecraftVersion,
            packConfig.modloader,
            packConfig.modloaderVersion
        )
        serverPackManifest.writeToFile(serverPack, utilities.jsonUtilities.objectMapper)

        apiPlugins.runPreZipExtensions(packConfig, serverPack.absolutePath)
        runPreServerPackZipListeners(packConfig, serverPack.absoluteFile.toPath())
        runGenericEventListeners()

        // If true, create a ZIP-archive excluding the Minecraft server JAR of the server pack.
        if (packConfig.isZipCreationDesired) {

            /*
            * Create the start scripts for this server pack. Ignores custom SPC_JAVA_SPC setting if one
            * is present. This is because a ZIP-archive, if one is created, is supposed to be uploaded
            * to platforms like CurseForge. We must not have scripts with custom Java paths there.
            */
            createServerRunFiles(packConfig.scriptSettings, serverPack.absolutePath, false)
            serverPackZip = zipBuilder(
                packConfig.minecraftVersion,
                serverPack.absolutePath,
                packConfig.modloader,
                packConfig.modloaderVersion
            )
        } else {
            log.info("Not creating zip archive of serverpack.")
        }

        /*
        * Create the start scripts for this server pack to be used for local testing.
        * The difference to the previous call is that these scripts respect the SPC_JAVA_SPC
        * placeholder setting, if the user has set one
        */
        createServerRunFiles(packConfig.scriptSettings, serverPack.absolutePath, true)

        // Inform user about location of newly generated server pack.
        log.info("Server pack available at: ${serverPack.absolutePath}")
        log.info("Server pack archive available at: ${serverPack.absolutePath}_server_pack.zip")
        log.info("Done!")
        apiPlugins.runPostGenExtensions(packConfig, serverPack.absolutePath)
        runPostGenListeners(packConfig, serverPack.absoluteFile.toPath())
        runGenericEventListeners()
        log.debug("Generation took ${generationStopWatch.stop().getTime()}")

        log.info("Performing security scans")
        val findings = mutableListOf<String>()
        log.info("Performing Nekodetector scan")
        findings.addAll(SecurityScans.scanUsingNekodetector(serverPack.toPath()))

        return ServerPackGeneration(
            serverPack,
            findings,
            serverPackZip,
            packConfig,
            files
        )
    }

    /**
     * Deletes all files, directories and ZIP-archives of previously generated server packs to ensure
     * newly generated server pack is as clean as possible. This will completely empty the server pack
     * directory, so use with caution!
     *
     * @param deleteZip   Whether to delete the server pack ZIP-archive.
     * @param destination The destination at which to clean up in.
     * @author Griefed
     */
    fun cleanupEnvironment(deleteZip: Boolean, destination: String) {
        log.info("Found old server pack at $destination. Cleaning up...")
        deleteExistingServerPack(destination)
        File(destination).deleteQuietly()
        if (deleteZip) {
            deleteExistingServerPackZip(destination)
        }
    }

    private fun deleteExistingServerPack(destination: String) {
        File(destination).deleteQuietly()
    }

    private fun deleteExistingServerPackZip(destination: String) {
        File(destination + "_server_pack.zip").deleteQuietly()
    }

    /**
     * Recursively copy all specified directories and files, excluding clientside-only mods, to
     * the server pack.
     */
    fun copyFiles(
        modpackDir: String,
        inclusions: ArrayList<InclusionSpecification>,
        clientMods: List<String>,
        whitelist: List<String>,
        minecraftVersion: String,
        destination: String,
        modloader: String,
        overwrite: Boolean
    ): List<File> = fileGatherer.copyFiles(
        modpackDir, inclusions, clientMods, whitelist, minecraftVersion, destination, modloader, overwrite
    )
    /**
     * Gather the server pack-files for a single inclusion-specification.
     */
    fun getServerFiles(
        inclusion: InclusionSpecification,
        modpackDir: String,
        destination: String,
        exclusions: MutableList<Regex>,
        clientMods: List<String>,
        modWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): List<ServerPackFile> = fileGatherer.getServerFiles(
        inclusion, modpackDir, destination, exclusions, clientMods, modWhitelist, minecraftVersion, modloader
    )
    /**
     * Download and provide the improved Fabric Server Launcher, if available for the given
     * versions.
     */
    fun getImprovedFabricLauncher(minecraftVersion: String, fabricVersion: String, destination: String) =
        provisioner.getImprovedFabricLauncher(minecraftVersion, fabricVersion, destination)
    /**
     * Copy the server-icon.png into the server pack, scaled to 64x64.
     */
    fun copyIcon(destination: String, pathToServerIcon: String) =
        provisioner.copyIcon(destination, pathToServerIcon)
    /**
     * Copy the server.properties into the server pack.
     */
    fun copyProperties(destination: String, pathToServerProperties: String) =
        provisioner.copyProperties(destination, pathToServerProperties)
    /**
     * Create start-scripts, variables.txt and HOW-TO-RUN.md for the generated server pack.
     */
    fun createServerRunFiles(scriptSettings: HashMap<String, String>, destination: String, isLocal: Boolean) =
        provisioner.createServerRunFiles(scriptSettings, destination, isLocal)
    /**
     * Create the ZIP-archive of the server pack, honoring the configured ZIP-exclusions.
     */
    fun zipBuilder(
        minecraftVersion: String,
        destination: String,
        modloader: String,
        modloaderVersion: String
    ): Optional<File> = provisioner.zipBuilder(minecraftVersion, destination, modloader, modloaderVersion)
    /**
     * Delete configured leftover-files before a modloader-server installation.
     */
    @Suppress("unused")
    fun preInstallationCleanup(destination: String) = provisioner.preInstallationCleanup(destination)
    /**
     * Gather all files for an explicit source-destination-combination.
     */
    fun getExplicitFiles(
        source: String,
        destination: String,
        modpackDir: String,
        serverPackDestination: String
    ): MutableList<ServerPackFile> = fileGatherer.getExplicitFiles(source, destination, modpackDir, serverPackDestination)
    /**
     * Recursively gather all files of the given save-directory.
     */
    @Suppress("unused")
    fun getSaveFiles(clientDir: String, directory: String, destination: String): List<ServerPackFile> =
        fileGatherer.getSaveFiles(clientDir, directory, destination)
    /**
     * Generate the list of mods to include in the server pack from the given configuration.
     */
    @Suppress("unused")
    fun compileModList(packConfig: PackConfig) = modListCompiler.compileModList(packConfig)

    /**
     * Generate the list of mods to include in the server pack, excluding clientside-only mods.
     */
    fun compileModList(
        modsDir: String,
        clientsideModsList: List<String>,
        modWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): Pair<List<File>, List<File>> =
        modListCompiler.compileModList(modsDir, clientsideModsList, modWhitelist, minecraftVersion, modloader)
    /**
     * Recursively gather all files of the given directory as source-destination-pairs.
     */
    fun getDirectoryFiles(source: String, destination: String): List<ServerPackFile> =
        fileGatherer.getDirectoryFiles(source, destination)
    /**
     * Whether the given file or directory matches any of the given exclusion-regexes.
     */
    fun excludeFileOrDirectory(modpackDir: String, fileToCheckFor: File, exclusions: List<Regex>): Boolean =
        fileGatherer.excludeFileOrDirectory(modpackDir, fileToCheckFor, exclusions)
    /**
     * Whether the installer for the given modloader-combination is available/reachable.
     */
    fun serverDownloadable(mcVersion: String, modloader: String, modloaderVersion: String): Boolean =
        provisioner.serverDownloadable(mcVersion, modloader, modloaderVersion)
    /**
     * Delete configured installer-leftovers after a modloader-server installation.
     */
    @Suppress("unused")
    fun postInstallCleanup(destination: String) = provisioner.postInstallCleanup(destination)
    /**
     * Gather every file matching the given regex from the source-directory.
     */
    @Suppress("unused")
    fun regexWalk(source: File, destination: String, regex: Regex, serverPackFiles: MutableList<ServerPackFile>) =
        fileGatherer.regexWalk(source, destination, regex, serverPackFiles)
    /**
     * Replace script-placeholders in the given content with their configured values.
     */
    fun replacePlaceholders(isLocal: Boolean, content: String, scriptSettings: HashMap<String, String>): String =
        provisioner.replacePlaceholders(isLocal, content, scriptSettings)
}