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
    val ending = "^\\.[0-9a-zA-Z]+$".toRegex()

    val variables = """
        ###
        # Remember:
        #   Escape \ and : in your Java path on Windows with another \
        #   Example:
        #     From: C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\bin\java.exe
        #     To:   C\:\\Program Files\\Eclipse Adoptium\\jdk-17.0.9.9-hotspot\\bin\\java.exe
        #   More on escape characters at https://en.wikipedia.org/wiki/Escape_character
        #
        # WAIT_FOR_USER_INPUT true/false allows you to enable/disable user confirmation upon
        #   graceful script ending.
        # RESTART true/false allows you to enable/disable automatically restarting the server
        #   should it crash.
        # SKIP_JAVA_CHECK true/false allows you to disable/enable the compatibility check
        #   of your Minecraft version and the provided Java version, as well as the automatic
        #   installation of a compatible Java version, should JAVA be set to 'java'.
        # JDK_VENDOR is for the automatic installation of a JDK compatible with the Minecraft
        #   version of your server pack. For an extensive list of available vendors, check out
        #   https://github.com/Jabba-Team/index/blob/main/index.json
        #   Note - For the installation to take place:
        #   - SKIP_JAVA_CHECK must be set to 'false'
        #   - JAVA be set to 'java'
        #   - No 'java' command be available OR
        #   - The available Java version behind 'java' be incompatible with your Minecraft version.
        # JABBA_INSTALL_VERSION has no effect on the installation of Jabba when using PowerShell.
        # MINECRAFT_VERSION is tightly coupled with the modloader version. Be careful when changing this, as the new
        #   new version you set may not be compatible with the modloader and modloader version combination.
        # MODLOADER and MODLOADER_VERSION same thing as with MINECRAFT_VERSION. Changing any of these three values may
        #   have unforseen consequences. Well, I say unforseen, it mostly causes the server to straight up not start,
        #   because of incompatibilities. Be very careful when changing these!
        # SERVERSTARTERJAR_FORCE_FETCH true/false allows you to enable/disable the force-refreshing of the server.jar
        #   when using Forge or NeoForge as your modloader. Force-refreshing means the file is replaced with a freshly
        #   downloaded one every time you run the start scripts.
        # SERVERSTARTERJAR_VERSION allows you to manually set the version of the server.jar downloaded by the scripts.
        #   If you want to always use the latest version, set this to exactly "latest". For a specific version, see
        #   https://github.com/neoforged/ServerStarterJar/releases and use the tags on the left as the version,
        #   e.g. 0.1.24 or 0.1.25. When setting a specific version, make sure the release you pick actually has a server.jar
        #   available for download. When the download fails with the "latest"-setting, then pick a specific one and/or
        #   contact the devs of the ServerStarterJar about the latest release not having a server.jar to download.
        # USE_SSJ true/false allows you to enable/disable the usage of the ServerStarterJar by the NeoForge project when you are
        #   using Forge. Some Forge versions may be incompatible with said ServerStarterJar. As of right now, people
        #   ran into trouble when using Forge and Minecraft 1.20.2 and 1.20.3.
        #
        # DO NOT EDIT THE FOLLOWING VARIABLES MANUALLY
        #   - FABRIC_INSTALLER_VERSION
        #   - QUILT_INSTALLER_VERSION
        #   - LEGACYFABRIC_INSTALLER_VERSION
        #
        # Variables are not reloaded between automatic restarts. If you've made changes to your
        #   variables and you want them to take effect, stop the server and script, then
        #   re-run it.
        ###
        MINECRAFT_VERSION=SPC_MINECRAFT_VERSION_SPC
        MODLOADER=SPC_MODLOADER_SPC
        MODLOADER_VERSION=SPC_MODLOADER_VERSION_SPC
        LEGACYFABRIC_INSTALLER_VERSION=SPC_LEGACYFABRIC_INSTALLER_VERSION_SPC
        FABRIC_INSTALLER_VERSION=SPC_FABRIC_INSTALLER_VERSION_SPC
        QUILT_INSTALLER_VERSION=SPC_QUILT_INSTALLER_VERSION_SPC
        RECOMMENDED_JAVA_VERSION=SPC_RECOMMENDED_JAVA_VERSION_SPC
        JAVA_ARGS="SPC_JAVA_ARGS_SPC"
        JAVA="SPC_JAVA_SPC"
        WAIT_FOR_USER_INPUT=SPC_WAIT_FOR_USER_INPUT_SPC
        ADDITIONAL_ARGS=SPC_ADDITIONAL_ARGS_SPC
        RESTART=SPC_RESTART_SPC
        SKIP_JAVA_CHECK=SPC_SKIP_JAVA_CHECK_SPC
        JDK_VENDOR=SPC_JDK_VENDOR_SPC
        JABBA_INSTALL_URL_SH=SPC_JABBA_INSTALL_URL_SH_SPC
        JABBA_INSTALL_URL_PS=SPC_JABBA_INSTALL_URL_PS_SPC
        JABBA_INSTALL_VERSION=SPC_JABBA_INSTALL_VERSION_SPC
        SERVERSTARTERJAR_FORCE_FETCH=SPC_SERVERSTARTERJAR_FORCE_FETCH_SPC
        SERVERSTARTERJAR_VERSION=SPC_SERVERSTARTERJAR_VERSION_SPC
        USE_SSJ=SPC_USE_SSJ_SPC
    """.trimIndent()
    private val howToStartTheServer = """
        # How To Start / Run The Server
        
        If your `variables.txt` has `JAVA=java` set, then a suitable Java version for your Minecraft server will
        be installed automatically.
        
        Forge and NeoForge 1.17 and up will create run.xx-scripts due to the ServerStarterJar being used to install
        and run the server. It is safe to ignore these and continue using the start.xx-scripts.
        Deleting the run.xx-scripts will result in the server being installed again by the ServerStarterJar. More about
        the ServerStarterJar at https://github.com/neoforged/ServerStarterJar
        
        ## Linux
        
        Run `.\start.sh` or `bash start.sh` to start the server.
        
        ## Windows
        
        Run `start.bat`.
        Do **not** delete the PowerShell (ps1) files!
        
        ### Convenience
        
        You may run `start.ps1` from a console-window manually, but using the Batch-script is recommended.
        Running PowerShell-scripts requires changing the ExecutionPolicy of your Windows-system. The Batch-script
        can bypass this for the start-script.
        
        TL;DR: start.bat better than start.ps1
        
        ## MacOS
        
        Run `.\start.sh` or `bash start.sh` to start the server.
        
        # Issues with this server pack
        
        If you downloaded this server pack from the internet and you run into issues with this server pack, then please
        contact the creators of the server pack about your issue(s).
        
        If you've created this server pack yourself and you run into issues, feel free to contact the developers of
        ServerPackCreator for support.
    """.trimIndent()

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
        val serverPackManifest: ServerPackManifest
        var serverPackZip: Optional<File> = Optional.empty()
        val serverPack = if (packConfig.customDestination.isPresent) {
            packConfig.customDestination.get()
        } else {
            File(getServerPackDestination(packConfig))
        }
        val existingManifest: File = File(serverPack.absolutePath, "manifest.json")
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
        } catch (ignored: IOException) {
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
        /*log.info("Performing jNeedle scan")
        findings.addAll(SecurityScans.scanUsingJNeedle(serverPack.toPath()))*/

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
     * Copies all specified directories and mods, excluding clientside-only mods, from the modpack
     * directory into the server pack directory. If a `source/file;destination/file`
     * -combination is provided, the specified source-file is copied to the specified
     * destination-file. One of the reasons as to why it is recommended to run a given
     * ConfigurationModel through the ConfigurationHandler first, is because the ConfigurationHandler
     * will resolve links to their files first before then correcting the given
     * ConfigurationModel.
     *
     * @param modpackDir        Files and directories are copied into the server_pack directory inside
     * the modpack directory.
     * @param inclusions All directories and files therein to copy to the server pack.
     * @param clientMods        List of clientside-only mods to exclude from the server pack.
     * @param minecraftVersion  The Minecraft version the modpack uses.
     * @param destination       The destination where the files should be copied to.
     * @param modloader         The modloader used for mod sideness detection.
     * @author Griefed
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
    ) : List<File> {
        val exclusions = mutableListOf<Regex>()
        var acquired: List<ServerPackFile>
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100000)
        val copiedFiles: MutableList<File> = ArrayList(10000)
        try {
            File(destination).create()
        } catch (ex: IOException) {
            log.error("Failed to create directory $destination")
        }

        if (inclusions.size == 1 && inclusions[0].source == "lazy_mode") {
            log.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!")
            log.warn("Lazy mode specified. This will copy the WHOLE modpack to the server pack. No exceptions.")
            log.warn("You will not receive any support for a server pack generated this way.")
            log.warn("Do not open an issue on GitHub if this configuration errors or results in a broken server pack.")
            log.warn("!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!")
            try {
                File(modpackDir).copyRecursively(File(destination), true)
            } catch (ex: IOException) {
                log.error("An error occurred copying the modpack to the server pack in lazy mode.", ex)
            }
            return copiedFiles
        }

        for (inclusion in inclusions) {
            acquired = getServerFiles(
                inclusion,
                modpackDir,
                destination,
                exclusions,
                clientMods,
                whitelist,
                minecraftVersion,
                modloader
            )
            serverPackFiles.addAll(acquired)
        }

        log.info("Ensuring files and/or directories are properly excluded.")
        serverPackFiles.removeIf { it: ServerPackFile ->
            excludeFileOrDirectory(modpackDir, it.sourceFile, exclusions)
        }
        log.info("Copying files to the server pack. This may take a while...")
        for (file in serverPackFiles) {
            try {
                copiedFiles.add(file.copy(overwrite))
            } catch (ex: IOException) {
                log.error(
                    "An error occurred trying to copy " + file.sourceFile + " to " + file.destinationFile + ".",
                    ex
                )
            }
        }
        return copiedFiles
    }

    fun getServerFiles(
        inclusion: InclusionSpecification,
        modpackDir: String,
        destination: String,
        exclusions: MutableList<Regex>,
        clientMods: List<String>,
        whitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): List<ServerPackFile> {
        val serverPackFiles = mutableListOf<ServerPackFile>()
        val clientDir = File(modpackDir, inclusion.source)
        val serverDir = File(destination, inclusion.source)
        val acquired: List<ServerPackFile>
        val processed: List<ServerPackFile>
        val serverPackFile: ServerPackFile
        val inclusionSourceFile = File(inclusion.source).absoluteFile
        val inclusionDestinationFile = File(destination, inclusionSourceFile.name).absoluteFile
        when {
            inclusion.isGlobalFilter() -> {
                if (inclusion.hasExclusionFilter()) {
                    try {
                        exclusions.add(inclusion.exclusionFilter!!.toRegex())
                    } catch (ex: PatternSyntaxException) {
                        log.error("Invalid exclusion-regex specified: ${inclusion.exclusionFilter}.",ex)
                    }
                }
            }

            inclusion.hasDestination() -> {
                val destinationFile = File(destination,inclusion.destination ?: inclusionSourceFile.name)
                when {
                    clientDir.isDirectory -> {
                        acquired = getExplicitFiles(clientDir.absolutePath, inclusion.destination!!, modpackDir, destination)
                        processed = runFilters(acquired, inclusion, modpackDir)
                        serverPackFiles.addAll(processed)
                    }
                    clientDir.absoluteFile.isFile -> {
                        serverPackFile = ServerPackFile(clientDir, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                    inclusionSourceFile.isDirectory -> {
                        acquired = getExplicitFiles(inclusion.source, inclusion.destination!!, modpackDir, destination)
                        processed = runFilters(acquired, inclusion, modpackDir)
                        serverPackFiles.addAll(processed)
                    }
                    inclusionSourceFile.isFile -> {
                        serverPackFile = ServerPackFile(inclusionSourceFile, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                    else -> {
                        serverPackFile = ServerPackFile(inclusionSourceFile, destinationFile)
                        serverPackFiles.add(serverPackFile)
                    }
                }
            }

            inclusion.source == "mods" -> {
                try {
                    serverDir.create()
                } catch (ignored: IOException) {
                }
                acquired = mutableListOf()
                for (mod in getModsToInclude(clientDir.absolutePath, clientMods, whitelist, minecraftVersion, modloader)) {
                    acquired.add(ServerPackFile(mod, File(serverDir, mod.name)))
                }
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            clientDir.absoluteFile.isDirectory -> {
                acquired = getDirectoryFiles(clientDir.absolutePath, serverDir.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            clientDir.absoluteFile.isFile -> {
                serverPackFile = ServerPackFile(clientDir, serverDir)
                serverPackFiles.add(serverPackFile)
            }

            inclusionSourceFile.isFile -> {
                serverPackFile = ServerPackFile(inclusionSourceFile, inclusionDestinationFile)
                serverPackFiles.add(serverPackFile)
            }

            inclusionSourceFile.isDirectory -> {
                acquired = getDirectoryFiles(inclusionSourceFile.absolutePath, inclusionDestinationFile.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }

            else -> {
                acquired = getDirectoryFiles(clientDir.absolutePath, serverDir.absolutePath)
                processed = runFilters(acquired, inclusion, modpackDir)
                serverPackFiles.addAll(processed)
            }
        }
        return serverPackFiles
    }

    private fun runFilters(
        acquired: List<ServerPackFile>,
        inclusionSpec: InclusionSpecification,
        modpackDir: String
    ): List<ServerPackFile> {
        val processed = mutableListOf<ServerPackFile>()
        val inclusionFilter = if (inclusionSpec.inclusionFilter.isNullOrBlank()) {
            null
        } else {
            try {
                inclusionSpec.inclusionFilter!!.toRegex()
            } catch (ex: PatternSyntaxException) {
                log.error("Invalid inclusion-regex specified: ${inclusionSpec.inclusionFilter}.",ex)
                null
            }
        }
        val exclusionFilter = if (inclusionSpec.exclusionFilter.isNullOrBlank()) {
            null
        } else {
            try {
                inclusionSpec.exclusionFilter!!.toRegex()
            } catch (ex: PatternSyntaxException) {
                log.error("Invalid exclusion-regex specified: ${inclusionSpec.exclusionFilter}.",ex)
                null
            }
        }
        if (inclusionFilter != null) {
            for (file in acquired) {
                if (file.sourceFile.absolutePath.replace(modpackDir + File.separator, "").matches(inclusionFilter)) {
                    processed.add(file)
                    log.debug("$file matched Inclusion-Filter $inclusionFilter.")
                }
            }
        } else {
            processed.addAll(acquired)
        }
        if (exclusionFilter != null) {
            processed.removeIf { file ->
                val source = file.sourceFile.absolutePath.replace(modpackDir + File.separator, "")
                return@removeIf if (source.matches(exclusionFilter)) {
                    log.debug("${file.sourceFile} matched Inclusion-Filter $exclusionFilter.")
                    true
                } else {
                    false
                }
            }
        }
        return processed
    }

    /**
     * Download and provide the improved Fabric Server Launcher, if it is available for the given
     * Minecraft and Fabric version.
     *
     * @param minecraftVersion The Minecraft version the modpack uses and the Fabric Server Launcher
     * should be downloaded for.
     * @param fabricVersion    The modloader version the modpack uses and the Fabric Server Launcher
     * should be downloaded for.
     * @param destination      The destination of the server pack.
     * @author Griefed
     */
    fun getImprovedFabricLauncher(minecraftVersion: String, fabricVersion: String, destination: String) {
        val fileDestination = File(destination, "fabric-server-launcher.jar")
        if (versionMeta.fabric.launcherFor(minecraftVersion, fabricVersion).isPresent) {
            versionMeta.fabric.launcherFor(minecraftVersion, fabricVersion).get().copyTo(fileDestination)
            log.info("Successfully provided improved Fabric Server Launcher.")
            val text = """
                |If you are using this server pack on a managed server, meaning you can not execute scripts, please use the fabric-server-launcher.jar instead of the fabric-server-launch.jar. Note the extra "er" at the end of "launcher".
                |This is the improved Fabric Server Launcher, which will take care of downloading and installing the Minecraft server and any and all libraries needed for running the Fabric server.
                |
                |The downside of this method is the occasional incompatibility of mods with the Fabric version, as the new Fabric Server Launcher always uses the latest available Fabric version.
                |If a mod is incompatible with said latest Fabric version, contact the mod-author and ask them to remedy the situation.
                |The official Fabric Discord had the following to add to this:
                |    Fabric loader however is cross version, so unless there is a mod incompatibility (which usually involves the mod being broken / using non-api internals)
                |    there is no good reason to use anything but the latest. I.e. the latest loader on any Minecraft version works with the new server launcher.
            """.trimMargin()
            File(destination, "SERVER_PACK_INFO.txt").writeText(text)
        }
    }

    /**
     * Copies the server-icon.png into server pack. The sever-icon is automatically scaled to a
     * resolution of 64x64 pixels.
     *
     * @param destination      The destination where the icon should be copied to.
     * @param pathToServerIcon The path to the custom server-icon.
     * @author Griefed
     */
    fun copyIcon(destination: String, pathToServerIcon: String) {
        log.info("Copying server-icon.png...")
        val customIcon = File(destination, apiProperties.defaultServerIcon.name)
        if (File(pathToServerIcon).exists()) {
            try {
                val originalImage: BufferedImage = ImageIO.read(File(pathToServerIcon))
                if (originalImage.height == 64 && originalImage.width == 64) {
                    try {
                        File(pathToServerIcon).copyTo(customIcon, true)
                    } catch (e: IOException) {
                        log.error("An error occurred trying to copy the server-icon.", e)
                    }
                } else {
                    val scaledImage: Image = originalImage.getScaledInstance(64, 64, Image.SCALE_SMOOTH)
                    val outputImage = BufferedImage(
                        scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB
                    )
                    outputImage.graphics.drawImage(scaledImage, 0, 0, null)
                    try {
                        ImageIO.write(outputImage, "png", customIcon)
                    } catch (ex: IOException) {
                        log.error("Error scaling image.", ex)
                    }
                }
            } catch (ex: Exception) {
                log.error("Error reading server-icon image.", ex)
            }
        } else if (pathToServerIcon.isEmpty()) {
            log.info("No custom icon specified or the file doesn't exist.")
            apiProperties.defaultServerIcon.copyTo(customIcon, true)
        } else {
            log.error("The specified server-icon does not exist: $pathToServerIcon")
        }
    }

    /**
     * Copies the server.properties into server pack.
     *
     * @param destination            The destination where the properties should be copied to.
     * @param pathToServerProperties The path to the custom server.properties.
     * @author Griefed
     */
    fun copyProperties(destination: String, pathToServerProperties: String) {
        log.info("Copying server.properties...")
        val customProperties = File(destination, apiProperties.defaultServerProperties.name)
        if (File(pathToServerProperties).exists()) {
            File(pathToServerProperties).copyTo(customProperties, true)
        } else if (pathToServerProperties.isEmpty()) {
            log.info("No custom properties specified or the file doesn't exist.")
            apiProperties.defaultServerProperties.copyTo(customProperties, true)
        } else {
            log.error("The specified server.properties does not exist: $pathToServerProperties")
        }
    }

    /**
     * Create start-scripts for the generated server pack using the templates the user has defined for
     * their instance of ServerPackCreator.
     *
     * @param scriptSettings Key-value pairs to replace in the script. A given key in the script is
     * replaced with its value.
     * @param destination    The destination where the scripts should be created in.
     * @param isLocal        Whether the start scripts should be created for a locally usable server
     * pack. Use `false` if the start scripts should be created for a
     * server pack about to be zipped.
     * @author Griefed
     */
    fun createServerRunFiles(scriptSettings: HashMap<String, String>, destination: String, isLocal: Boolean) {
        var script: File
        var content: String
        val scripts = mutableListOf<File>()
        for ((key, value) in apiProperties.startScriptTemplates) {
            try {
                script = File(destination, "start.$key")
                content = replacePlaceholders(isLocal, File(value).readText(), scriptSettings).replace("\r", "")
                if (script.exists()) {
                    script.setWritable(true)
                }
                script.writeText(content)
                scripts.add(script)
            } catch (ex: Exception) {
                log.error("$key-File not accessible: $value.", ex)
            }
        }

        for ((key, value) in apiProperties.javaScriptTemplates) {
            try {
                script = File(destination, "install_java.$key")
                content = replacePlaceholders(isLocal, File(value).readText(), scriptSettings).replace("\r", "")
                if (script.exists()) {
                    script.setWritable(true)
                }
                script.writeText(content)
                scripts.add(script)
            } catch (ex: Exception) {
                log.error("$key-File not accessible: $value.", ex)
            }
        }
        for (scriptFile in scripts) {
            scriptFile.setExecutable(true)
            scriptFile.setReadable(true)
            scriptFile.setWritable(false)
        }

        try {
            val destinationVariables = File(destination, "variables.txt")
            var variablesContent = variables
            variablesContent = replacePlaceholders(isLocal, variablesContent, scriptSettings)
            for ((key, value) in scriptSettings) {
                if (key.startsWith("CUSTOM_") && key.endsWith("_CUSTOM")) {
                    val varKey = key.replace("CUSTOM_","").replace("_CUSTOM","")
                    variablesContent += "\n$varKey=$value"
                }
            }
            destinationVariables.writeText(variablesContent.replace("\r", ""))
            destinationVariables.setReadable(true)
            destinationVariables.setWritable(true)
            destinationVariables.setExecutable(false)
        } catch (ex: Exception) {
            log.error("File not accessible: ${File(destination, "variables.txt")}.", ex)
        }

        try {
            val howToStartTheScriptReadme = File(destination, "HOW-TO-RUN.md")
            if (howToStartTheScriptReadme.exists()) {
                howToStartTheScriptReadme.setWritable(true)
            }
            howToStartTheScriptReadme.writeText(howToStartTheServer.replace("\r", ""))
            howToStartTheScriptReadme.setExecutable(false)
            howToStartTheScriptReadme.setReadable(true)
            howToStartTheScriptReadme.setWritable(false)
        } catch (ex: Exception) {
            log.error("File not accessible: ${File(destination, "HOW-TO-RUN.md")}.", ex)
        }
    }

    /**
     * Creates a ZIP-archive of specified directory. Depending on the property `de.griefed.serverpackcreator.serverpack.zip.exclude.enabled`,
     * files will be excluded. To customize the files which will be excluded, the property `de.griefed.serverpackcreator.serverpack.zip.exclude`
     * must be configured accordingly. The created ZIP-archive will be stored alongside the specified
     * destination, with `_server_pack.zip` appended to its name.
     *
     * @param minecraftVersion          Determines the name of the Minecraft server JAR to exclude
     * from the ZIP-archive if the modloader is Forge.
     * @param destination               The destination where the ZIP-archive should be created in.
     * @param modloader                 The modloader the modpack and server pack use.
     * @param modloaderVersion          The modloader version the modpack and server pack use.
     * @author Griefed
     */
    fun zipBuilder(
        minecraftVersion: String,
        destination: String,
        modloader: String,
        modloaderVersion: String
    ) : Optional<File> {
        log.info("Creating zip archive of serverpack...")
        val zipParameters = ZipParameters()
        var zip: ZipFile? = null
        val filesToExclude: MutableList<File> = ArrayList(100)
        if (apiProperties.isZipFileExclusionEnabled) {
            for (entry in apiProperties.zipArchiveExclusions) {
                filesToExclude.add(
                    File(
                        destination,
                        entry.replace("MINECRAFT_VERSION", minecraftVersion).replace("MODLOADER", modloader)
                            .replace("MODLOADER_VERSION", modloaderVersion)
                    )
                )
            }
            val excludeFileFilter = ExcludeFileFilter { o: File -> filesToExclude.contains(o) }
            zipParameters.excludeFileFilter = excludeFileFilter
        } else {
            log.info("File exclusion from ZIP-archives deactivated.")
        }
        val comment = ("Server pack made with ServerPackCreator ${apiProperties.apiVersion} by Griefed.")
        zipParameters.isIncludeRootFolder = false
        zipParameters.fileComment = comment
        try {
            zip = ZipFile("${destination}_server_pack.zip")
            zip.use {
                it.addFolder(File(destination), zipParameters)
                it.comment = comment
            }
        } catch (ex: IOException) {
            log.error("There was an error during zip creation.", ex)
        }
        log.info("Finished creation of zip archive.")
        return Optional.ofNullable(zip?.file)
    }

    /**
     * Delete files and folders from previous installations to prevent errors during server installation due to already
     * existing files.
     * @param destination The folder in which to perform the cleanup operations.
     *
     * @author Griefed
     */
    fun preInstallationCleanup(destination: String) {
        log.info("Pre server installation cleanup.")
        var fileToDelete: File
        for (file in apiProperties.preInstallCleanupFiles) {
            fileToDelete = File(destination,file)
            if (fileToDelete.deleteQuietly()) {
                log.info("Deleted $fileToDelete")
            }
        }
    }

    /**
     * Gather a list of all files from an explicit source;destination-combination. If the source is a
     * file, a singular [ServerPackFile] is returned. If the source is a directory, then all
     * files in said directory are returned.
     *
     * @param source source-file/directory
     * @param destination destination-file/directory
     * @param modpackDir  The modpack-directory.
     * @param serverPackDestination The destination, normally the server pack-directory.
     * @return List of [ServerPackFile].
     * @author Griefed
     */
    fun getExplicitFiles(
        source: String,
        destination: String,
        modpackDir: String,
        serverPackDestination: String
    ): MutableList<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100)
        if (File(modpackDir, source).isFile) {
            serverPackFiles.add(
                ServerPackFile(
                    File(modpackDir, source), File(serverPackDestination, destination)
                )
            )
        } else if (File(modpackDir, source).isDirectory) {
            serverPackFiles.addAll(
                getDirectoryFiles(
                    modpackDir + File.separator + source, serverPackDestination + File.separator + destination
                )
            )
        } else if (File(source).isFile) {
            serverPackFiles.add(
                ServerPackFile(
                    File(source), File(serverPackDestination, destination)
                )
            )
        } else if (File(source).isDirectory) {
            serverPackFiles.addAll(
                getDirectoryFiles(
                    source, serverPackDestination + File.separator + destination
                )
            )
        }
        return serverPackFiles
    }

    /**
     * Recursively acquire all files and directories inside the given save-directory as a list of
     * [ServerPackFile].
     *
     * @param clientDir   Target directory in the server pack. Usually the name of the world.
     * @param directory   The save-directory.
     * @param destination The destination of the server pack.
     * @return List of [ServerPackFile] which will be included in the server pack.
     * @author Griefed
     */
    fun getSaveFiles(clientDir: String, directory: String, destination: String): List<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(2000)
        try {
            Files.walk(Paths.get(clientDir)).use {
                for (path in it) {
                    try {
                        serverPackFiles.add(
                            ServerPackFile(
                                path,
                                Paths.get(destination + File.separator + directory.substring(6))
                                    .resolve(Paths.get(clientDir).relativize(path))
                            )
                        )
                    } catch (ex: UnsupportedOperationException) {
                        log.error("Couldn't gather file $path from directory $clientDir.", ex)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("An error occurred during the copy-procedure to the server pack.", ex)
        }
        return serverPackFiles
    }

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param packConfig The configurationModel containing the modpack directory, list of
     * clientside-only mods to exclude, Minecraft version used by the
     * modpack and server pack and the modloader used by the modpack and
     * server pack.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    fun getModsToInclude(packConfig: PackConfig) = getModsToInclude(
        "${packConfig.modpackDir}${File.separator}mods",
        packConfig.clientMods,
        packConfig.modsWhitelist,
        packConfig.minecraftVersion,
        packConfig.modloader
    )

    /**
     * Generates a list of all mods to include in the server pack. If the user specified
     * clientside-mods to exclude, and/or if the automatic exclusion of clientside-only mods is
     * active, they will be excluded, too.
     *
     * @param modsDir                 The mods-directory of the modpack of which to generate a list of
     * all its contents.
     * @param userSpecifiedClientMods A list of all clientside-only mods.
     * @param userSpecifiedModsWhitelist  A list of mods to include regardless if a match was found in [userSpecifiedClientMods].
     * @param minecraftVersion        The Minecraft version the modpack uses. When the modloader is
     * Forge, this determines whether Annotations or Tomls are
     * scanned.
     * @param modloader               The modloader the modpack uses.
     * @return A list of all mods to include in the server pack.
     * @author Griefed
     */
    fun getModsToInclude(
        modsDir: String,
        userSpecifiedClientMods: List<String>,
        userSpecifiedModsWhitelist: List<String>,
        minecraftVersion: String,
        modloader: String
    ): List<File> {
        log.info("Preparing a list of mods to include in server pack...")
        val filesInModsDir: Collection<File> = File(modsDir).filteredWalk(modFileEndings, FilterType.ENDS_WITH, FileWalkDirection.TOP_DOWN, recursive = false)
        val modsInModpack = TreeSet(filesInModsDir)
        val autodiscoveredClientMods: MutableList<File> = ArrayList(100)

        // Check whether scanning mods for sideness is activated.
        if (apiProperties.isAutoExcludingModsEnabled) {
            val scanningStopWatch = SimpleStopWatch().start()
            when (modloader) {
                "LegacyFabric", "Fabric" -> autodiscoveredClientMods.addAll(modScanner.fabricScanner.scan(filesInModsDir))

                "Forge" -> if (minecraftVersion.split(".").dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].toInt() > 12) {
                    autodiscoveredClientMods.addAll(modScanner.forgeTomlScanner.scan(filesInModsDir))
                } else {
                    autodiscoveredClientMods.addAll(modScanner.forgeAnnotationScanner.scan(filesInModsDir))
                }

                "NeoForge" -> {
                    if (SemanticVersionComparator.compareSemantics("1.20.5", minecraftVersion, Comparison.EQUAL_OR_NEW)) {
                        log.debug("Scanning using NeoForge scanner.")
                        autodiscoveredClientMods.addAll(modScanner.neoForgeTomlScanner.scan(filesInModsDir))
                    } else {
                        log.debug("Scanning using Forge scanner.")
                        autodiscoveredClientMods.addAll(modScanner.forgeTomlScanner.scan(filesInModsDir))
                    }
                }

                "Quilt" -> {
                    val discoMods = TreeSet<File>()
                    discoMods.addAll(modScanner.fabricScanner.scan(filesInModsDir))
                    discoMods.addAll(modScanner.quiltScanner.scan(filesInModsDir))
                    autodiscoveredClientMods.addAll(discoMods)
                    discoMods.clear()
                }
            }

            // Exclude scanned mods from copying if said functionality is enabled.
            excludeMods(autodiscoveredClientMods, modsInModpack)
            log.debug(
                "Scanning and excluding of ${filesInModsDir.size} mods took ${scanningStopWatch.stop().getTime()}"
            )
        } else {
            log.info("Automatic clientside-only mod detection disabled.")
        }

        // Exclude user-specified mods from copying.
        excludeUserSpecifiedMod(userSpecifiedClientMods, userSpecifiedModsWhitelist, modsInModpack)
        return ArrayList(modsInModpack)
    }

    /**
     * Recursively acquire all files and directories inside the given directory as a list of
     * [ServerPackFile].
     *
     * @param source      The source-directory.
     * @param destination The server pack-directory.
     * @return List of files and folders of the server pack.
     * @author Griefed
     */
    fun getDirectoryFiles(source: String, destination: String): List<ServerPackFile> {
        val serverPackFiles: MutableList<ServerPackFile> = ArrayList(100)
        try {
            Files.walk(Paths.get(source).absolute()).use {
                for (path in it) {
                    try {
                        val pathFile = path.toFile().absolutePath
                        val sourceFile = File(source).absolutePath
                        val destFile = File(destination, pathFile.replace(sourceFile, ""))
                        serverPackFiles.add(
                            ServerPackFile(
                                path.toFile(),
                                destFile
                            )
                        )
                    } catch (ex: UnsupportedOperationException) {
                        log.error("Couldn't gather file $path from directory $source.", ex)
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("An error occurred gathering files to copy to the server pack for directory $source.", ex)
        }

        return serverPackFiles
    }

    /**
     * Check whether the given file or directory should be excluded from the server pack.
     *
     * @param modpackDir     The directory where the modpack resides in. Used to filter out any
     * unwanted directories using the property `de.griefed.serverpackcreator.configuration.directories.shouldexclude`.
     * @param fileToCheckFor The file or directory to check whether it should be excluded from the
     * server pack.
     * @param exclusions     Files or directories determined by ServerPackCreator to be excluded from
     * the server pack
     * @return `true` if the file or directory was determined to be excluded from the server
     * pack.
     * @author Griefed
     */
    fun excludeFileOrDirectory(modpackDir: String, fileToCheckFor: File, exclusions: List<Regex>): Boolean {
        val cleaned = fileToCheckFor.absolutePath.replace(File(modpackDir).absolutePath + File.separator, "")
        return exclusions.any { regex ->
            if (cleaned.matches(regex)) {
                log.info("Excluding '$cleaned' as per global exclusion filter '$regex'.")
                return@any true
            } else {
                return@any false
            }
        }
    }

    /**
     * Check whether the installer for the given combination of Minecraft version, modloader and
     * modloader version is available/reachable.
     *
     * @param mcVersion        The Minecraft version.
     * @param modloader        The modloader.
     * @param modloaderVersion The modloader version.
     * @return `true` if the installer can be downloaded.
     * @author Griefed
     */
    fun serverDownloadable(mcVersion: String, modloader: String, modloaderVersion: String) = when (modloader) {
        "Fabric" -> utilities.webUtilities.isReachable(versionMeta.fabric.releaseInstallerUrl())

        "Forge" -> {
            val instance = versionMeta.forge.getForgeInstance(mcVersion, modloaderVersion)
            instance.isPresent && utilities.webUtilities.isReachable(instance.get().installerUrl)
        }

        "Quilt" -> utilities.webUtilities.isReachable(versionMeta.quilt.releaseInstallerUrl())

        "LegacyFabric" -> {
            try {
                utilities.webUtilities.isReachable(versionMeta.legacyFabric.releaseInstallerUrl())
            } catch (e: MalformedURLException) {
                false
            }
        }

        "NeoForge" -> {
            val instance = versionMeta.neoForge.getNeoForgeInstance(mcVersion,modloaderVersion)
            instance.isPresent && utilities.webUtilities.isReachable(instance.get().installerUrl)
        }

        else -> false
    }

    /**
     * Cleans up the server_pack directory by deleting left-over files from modloader installations
     * and version checking.
     *
     * @param destination      The destination where we should clean up in.
     * @author Griefed
     */
    fun postInstallCleanup(destination: String) {
        log.info("Cleanup after modloader server installation.")
        var fileToDelete: File
        for (file in apiProperties.postInstallCleanupFiles) {
            fileToDelete = File(destination, file)
            if (fileToDelete.deleteQuietly()) {
                log.info("  Deleted $fileToDelete")
            }
        }
    }

    /**
     * Exclude every automatically discovered clientside-only mod from the list of mods in the
     * modpack.
     *
     * @param autodiscoveredClientMods Automatically discovered clientside-only mods in the modpack.
     * @param modsInModpack            All mods in the modpack.
     * @author Griefed
     */
    fun excludeMods(autodiscoveredClientMods: List<File>, modsInModpack: TreeSet<File>) {
        if (autodiscoveredClientMods.isNotEmpty()) {
            log.info("Automatically detected mods: ${autodiscoveredClientMods.size}")
            for (discoveredMod in autodiscoveredClientMods) {
                modsInModpack.removeIf {
                    if (it.name.contains(discoveredMod.name)) {
                        log.warn("Automatically excluding mod: ${discoveredMod.name}")
                        return@removeIf true
                    } else {
                        return@removeIf false
                    }
                }
            }
        } else {
            log.info("No clientside-only mods detected.")
        }
    }

    /**
     * Exclude user-specified mods from the server pack.
     *
     * @param userSpecifiedExclusions User-specified clientside-only mods to exclude from the server
     * pack.
     * @param modsInModpack           Every mod ending with `jar` or `disabled` in the
     * modpack.
     * @author Griefed
     */
    fun excludeUserSpecifiedMod(userSpecifiedExclusions: List<String>, userSpecifiedModsWhitelist: List<String>, modsInModpack: TreeSet<File>) {
        if (userSpecifiedExclusions.isNotEmpty()) {
            log.info("Performing ${apiProperties.exclusionFilter}-type checks for user-specified clientside-only mod exclusion.")
            for (userSpecifiedExclusion in userSpecifiedExclusions) {
                exclude(userSpecifiedExclusion, userSpecifiedModsWhitelist, modsInModpack)
            }
        } else {
            log.warn("User specified no clientside-only mods.")
        }
    }

    /**
     * Walk through the specified directory and add a [ServerPackFile] for every file/folder
     * which matches the given regex.
     *
     * @param source          The source-directory to walk through and perform regex-matches in.
     * @param destination     The destination-directory where a matched file should be copied to,
     * usually the server pack directory.
     * @param regex           Regex with which to perform matches against files in the
     * source-directory.
     * @param serverPackFiles List of files to copy to the server pack to which any matched file will
     * be added to.
     * @author Griefed
     */
    fun regexWalk(
        source: File, destination: String, regex: Regex, serverPackFiles: MutableList<ServerPackFile>
    ) {
        var toMatch: String
        try {
            Files.walk(source.toPath()).use {
                for (path in it) {
                    toMatch = path.toFile().absolutePath.replace(source.absolutePath, "")
                    if (toMatch.startsWith(File.separator)) {
                        toMatch = toMatch.substring(1)
                    }
                    if (toMatch.matches(regex)) {
                        val add = Paths.get(destination + File.separator + source.name)
                            .resolve(source.toPath().relativize(path))
                        serverPackFiles.add(
                            ServerPackFile(
                                path, add
                            )
                        )
                        log.debug("Including through regex-match:")
                        log.debug("    SOURCE: $path")
                        log.debug("    DESTINATION: $add")
                    }
                }
            }
        } catch (ex: IOException) {
            log.error("Couldn't gather all files from ${source.absolutePath} for filter \"$regex\".", ex)
        }
    }

    /**
     * Go through the mods in the modpack and exclude any of the user-specified clientside-only mods
     * according to the filter method set in the serverpackcreator.properties. For available filters,
     * see [ExclusionFilter].
     *
     * @param userSpecifiedExclusion The client mod to check whether it needs to be excluded.
     * @param modsInModpack          All mods in the modpack.
     *
     * @author Griefed
     */
    fun exclude(userSpecifiedExclusion: String, userSpecifiedModsWhitelist: List<String>, modsInModpack: TreeSet<File>) {
        modsInModpack.removeIf { modToCheck ->
            val excluded: Boolean
            val modName = modToCheck.name
            excluded = when (apiProperties.exclusionFilter) {
                ExclusionFilter.START -> modName.startsWith(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.startsWith(whitelistedMod) }
                ExclusionFilter.END -> modName.endsWith(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.endsWith(whitelistedMod) }
                ExclusionFilter.CONTAIN -> modName.contains(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.contains(whitelistedMod) }
                ExclusionFilter.REGEX -> modName.matches(userSpecifiedExclusion.toRegex()) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.matches(whitelistedMod.toRegex()) }
                ExclusionFilter.EITHER -> (
                            (modName.startsWith(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.startsWith(whitelistedMod) }) ||
                            (modName.endsWith(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.endsWith(whitelistedMod) }) ||
                            (modName.contains(userSpecifiedExclusion) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.contains(whitelistedMod) }) ||
                            (modName.matches(userSpecifiedExclusion.toRegex()) && !userSpecifiedModsWhitelist.any { whitelistedMod -> modName.matches(whitelistedMod.toRegex()) }))
            }
            if (excluded) {
                log.debug("Removed ${modToCheck.name} as per user-specified check: $userSpecifiedExclusion")
            }
            excluded
        }
    }

    /**
     * Replace placeholders for script settings in the given [content] with their respective values, both provided via the
     * HashMap [scriptSettings].
     *
     * @param isLocal Whether the start scripts should be created for a locally usable server pack. Use false if the
     * start scripts should be created for a server pack about to be zipped
     *
     * @author Griefed
     */
    fun replacePlaceholders(isLocal: Boolean, content: String, scriptSettings: HashMap<String, String>): String {
        var result = content
        for ((key, value) in scriptSettings) {
            result = if (isLocal && key == "SPC_JAVA_SPC") {
                result.replace(key, value.escapePath())
            } else if (!isLocal && key == "SPC_JAVA_SPC") {
                result.replace(key, "java")
            } else if (!isLocal && key == "SPC_RESTART_SPC") {
                result.replace(key, "true")
            } else {
                result.replace(key, value)
            }
        }
        return result
    }
}