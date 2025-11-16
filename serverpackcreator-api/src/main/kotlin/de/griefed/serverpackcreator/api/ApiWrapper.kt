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
package de.griefed.serverpackcreator.api

import com.electronwill.nightconfig.toml.TomlParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.api.modscanning.*
import de.griefed.serverpackcreator.api.serverpack.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * API wrapper, allowing you to conveniently initialize, setup and use the different aspects of ServerPackCreator.
 *
 * @param properties serverpackcreator.properties-file containing settings and configurations to load the API with.
 * @param runSetup   Whether to run the file-setup during API inizialization.
 * @author Griefed
 */
class ApiWrapper private constructor(
    val properties: File = File("serverpackcreator.properties"),
    runSetup: Boolean = true
) {
    val xmlJsonRegex = ".*\\.(xml|json)".toRegex()
    var setupWasRun: Boolean = false

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * This instances settings used across ServerPackCreator, such as the working-directories, files
     * and other settings.
     *
     * @return applicationProperties used across this ServerPackCreator-instance.
     * @author Griefed
     */
    @get:Synchronized
    val apiProperties: ApiProperties by lazy {
        ApiProperties(properties)
    }

    /**
     * Will return true if this is the first time ServerPackCreator is being run
     * on a given host.
     */
    @Volatile
    var firstRun: Boolean = apiProperties.getPreference("de.griefed.serverpackcreator.firstrun","true").get().toBoolean()
        get() {
            apiProperties.storePreference("de.griefed.serverpackcreator.firstrun", "false")
            return field
        }
        private set

    /**
     * This instances JSON-ObjectMapper used across ServerPackCreator with which this instance was
     * initialized. By default, the ObjectMapper used across ServerPackCreator has the following
     * features set:
     *
     *  * disabled: [DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES]
     *  * enabled: [DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY]
     *  * enabled: [JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS]
     *
     *
     * @return Json-ObjectMapper to parse and read JSON.
     * @author Griefed
     */
    @get:Synchronized
    val objectMapper: ObjectMapper = ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())

    /**
     * This instances common JSON utilities used across ServerPackCreator.
     *
     * @return Common JSON utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    val jsonUtilities: JsonUtilities = JsonUtilities(objectMapper)

    /**
     * This instances common web utilities used across ServerPackCreator.
     *
     * @return Common web utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    val webUtilities: WebUtilities by lazy {
        WebUtilities(apiProperties)
    }

    /**
     * This instances DocumentBuilder for working with XML-data.
     *
     * @return DocumentBuilder for working with XML.
     * @author Griefed
     */
    @get:Synchronized
    val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()

    /**
     * This instances common XML utilities used across ServerPackCreator.
     *
     * @return Common XML utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    val xmlUtilities: XmlUtilities = XmlUtilities(documentBuilderFactory)

    /**
     * This instances collection of common utilities used across ServerPackCreator.
     *
     * @return Collection of common utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    val utilities: Utilities by lazy {
        Utilities(
            webUtilities,
            jsonUtilities,
            xmlUtilities
        )
    }

    /**
     * This instances version meta used for checking version-correctness of Minecraft and supported
     * modloaders, as well as gathering information about Minecraft servers and modloader installers.
     *
     * @return Meta used for checking version-correctness of Minecraft and supported modloaders.
     * @throws IOException                  When manifests couldn't be parsed.
     * @throws ParserConfigurationException When xml-manifests couldn't be read.
     * @throws SAXException                 When xml-manifests couldn't be read.
     * @author Griefed
     */
    @get:Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    @get:Synchronized
    val versionMeta: VersionMeta by lazy {
        VersionMeta(
            apiProperties.minecraftVersionManifest,
            apiProperties.forgeVersionManifest,
            apiProperties.oldNeoForgeVersionManifest,
            apiProperties.newNeoForgeVersionManifest,
            apiProperties.fabricVersionManifest,
            apiProperties.fabricInstallerManifest,
            apiProperties.fabricIntermediariesManifest,
            apiProperties.quiltVersionManifest,
            apiProperties.quiltInstallerManifest,
            apiProperties.legacyFabricGameManifest,
            apiProperties.legacyFabricLoaderManifest,
            apiProperties.legacyFabricInstallerManifest,
            objectMapper,
            utilities,
            apiProperties
        )
    }

    /**
     * This instances ConfigurationHandler for checking a given [de.griefed.serverpackcreator.api.config.PackConfig] for
     * validity, so a server pack can safely be created from it.
     *
     * @return Handler for config checking.
     * @throws IOException                  When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @author Griefed
     */
    @get:Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    @get:Synchronized
    val configurationHandler: ConfigurationHandler by lazy {
        ConfigurationHandler(versionMeta, apiProperties, utilities, apiPlugins)
    }

    /**
     * This instances plugin manager for ServerPackCreator-plugins, if any are installed. This gives you
     * access to the available extensions, should any be available in your instance of
     * ServerPackCreator.
     *
     * @return Plugin manager for ServerPackCreator-plugins, if any are installed.
     * @throws IOException                  When the [VersionMeta] had to be instantiated, but an error occurred during
     * the parsing of a manifest.
     * @throws ParserConfigurationException When the [VersionMeta] had to be instantiated, but an error occurred during
     * the parsing of a manifest.
     * @throws SAXException                 When the [VersionMeta] had to be instantiated, but an error occurred during
     * the parsing of a manifest.
     * @author Griefed
     */
    @get:Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    @get:Synchronized
    val apiPlugins: ApiPlugins by lazy {
        ApiPlugins(tomlParser, apiProperties, versionMeta, utilities)
    }

    /**
     * This instances ServerPackHandler used to turn a [de.griefed.serverpackcreator.api.config.PackConfig] into a server pack.
     *
     * @return The ServerPackHandler with which config models can be used to create server packs.
     * @throws IOException                  When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws ParserConfigurationException When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     * @throws SAXException                 When the [VersionMeta] had to be instantiated, but
     * an error occurred during the parsing of a manifest.
     */
    @get:Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    @get:Synchronized
    val serverPackHandler: ServerPackHandler by lazy {
        ServerPackHandler(apiProperties, versionMeta, utilities, apiPlugins, modScanner)
    }

    /**
     * This instances modscanner to determine the sideness of a given Forge, Fabric, LegacyFabric or
     * Quilt mod.
     *
     * @return Modscanner to determine the sideness of a given Forge, Fabric, LegacyFabric or Quilt
     * mod.
     * @author Griefed
     */
    @get:Synchronized
    val modScanner: ModScanner by lazy {
        ModScanner(forgeAnnotationScanner, fabricScanner, quiltScanner, forgeTomlScanner, neoForgeTomlScanner)
    }

    /**
     * This instances annotation scanner used to determine the sideness of Forge mods for Minecraft
     * 1.12.2 and older.
     *
     * @return Annotation scanner used to determine the sideness of Forge mods for Minecraft 1.12.2
     * and older.
     * @author Griefed
     */
    @get:Synchronized
    val forgeAnnotationScanner: ForgeAnnotationScanner by lazy {
        ForgeAnnotationScanner(objectMapper, utilities)
    }

    /**
     * This instances scanner to determine the sideness of Fabric mods.
     *
     * @return Scanner to determine the sideness of Fabric mods.
     * @author Griefed
     */
    @get:Synchronized
    val fabricScanner: FabricScanner by lazy {
        FabricScanner(objectMapper, utilities)
    }

    /**
     * This instances scanner to determine the sideness of Quilt mods.
     *
     * @return Scanner to determine the sideness of Quilt mods.
     * @author Griefed
     */
    @get:Synchronized
    val quiltScanner: QuiltScanner by lazy {
        QuiltScanner(objectMapper, utilities)
    }

    /**
     * This instances toml parser to read and parse various `.toml`-files during mod-scanning,
     * plugin- and extension config loading and provisioning, serverpackcreator.conf reading and more.
     *
     * @return Toml parser to read and parse `.toml`-files.
     * @author Griefed
     */
    @get:Synchronized
    val tomlParser: TomlParser = TomlParser()

    /**
     * This instances toml scanner to determine the sideness of Forge mods for Minecraft 1.13.x and
     * newer.
     *
     * @return Scanner to determine the sideness of Forge mods for Minecraft 1.13.x and newer.
     * @author Griefed
     */
    val forgeTomlScanner: ForgeTomlScanner = ForgeTomlScanner(tomlParser)

    /**
     * This instances toml scanner to determine the sideness of NeoForge mods for Minecraft 1.20.5 and
     * newer.
     *
     * @return Scanner to determine the sideness of Forge mods for Minecraft 1.20.5 and newer.
     * @author Griefed
     */
    @get:Synchronized
    val neoForgeTomlScanner: NeoForgeTomlScanner = NeoForgeTomlScanner(tomlParser)

    init {
        if (runSetup) {
            setup()
        }
    }

    companion object {
        @Volatile
        private var api: ApiWrapper? = null

        /**
         * Acquire a new instance of the ServerPackCreator API.
         *
         * @param properties A ServerPackCreator properties-file to use for API initialization. This files may contain
         * configurations for the home-directory and many other settings. For details, see the [Wiki](https://help.serverpackcreator.de/settings-and-configs.html).
         * @param runSetup Whether to run the initial setup, creating and copying all required files to the filesystem and
         * ensuring the home-directory is properly set up and available to the API.
         * @author Griefed
         */
        @Synchronized
        fun api(
            properties: File = File("serverpackcreator.properties"),
            runSetup: Boolean = true
        ): ApiWrapper {
            if (api == null) {
                synchronized(this) {
                    if (api == null) {
                        api = ApiWrapper(properties, runSetup)
                    }
                }
            }
            return api!!
        }
    }

    /**
     * Convenience method to set up ServerPackCreator.
     *
     * @author Griefed
     */
    @Synchronized
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun setup(force: Boolean = false): ApiWrapper {
        if (force) {
            setupWasRun = false
        }
        if (!setupWasRun) {
            stageOne()
            stageTwo()
            stageThree()
            setupWasRun = true
        }
        return this
    }

    /**
     * Stage one of starting ServerPackCreator.
     *
     * Creates and prepares the environment for ServerPackCreator to run by creating required
     * directories and copying required files from the JAR-file to the filesystem. Some of these files
     * can and should be edited by a given user, others however, not.
     *
     *  * Checks the read- and write-permissions of ServerPackCreators base-directory.
     *  * Copies the `README.md` from the JAR to the home-directory.
     *  * Copies the `HELP.md` from the JAR to the home-directory.
     *  * Copies the `CHANGELOG.md` from the JAR to the home-directory.
     *  * Copies the `LICENSE` from the JAR to the home-directory.
     *  * Copies the fallback version-manifests to the manifests.
     *  * Creates default directories:
     *
     *  * server_files
     *  * work
     *  * temp
     *  * work/modpacks
     *  * server-packs (depending on the users settings, this may be anywhere on the users system)
     *  * plugins
     *  * plugins/config
     *
     *  * Example `disabled.txt`-file in plugins/disabled.txt.
     *  * Creates the default `server.properties` if it doesn't exist.
     *  * Creates the default `server-icon.png` if it doesn't exist.
     *  * Creates the default PowerShell and Shell script templates or overwrites them if they already exist.
     *  * Determines whether this instance of ServerPackCreator was updated from a previous version.
     *
     * If an update was detected, and migrations are available for any of the steps of the update, they are executed,
     * thus ensuring users are safe to update their instances. Writes ServerPackCreator and system information to the
     * console and logs, important for error reporting and debugging.
     *
     * @author Griefed
     */
    fun stageOne() {
        JarUtilities.copyFileFromJar(
            "README.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        JarUtilities.copyFileFromJar(
            "HELP.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        JarUtilities.copyFileFromJar(
            "CHANGELOG.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        JarUtilities.copyFileFromJar(
            "LICENSE", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )

        System.setProperty("file.encoding", StandardCharsets.UTF_8.name())
        if (!FileUtilities.isReadWritePermissionSet(apiProperties.getJarFolder())) {
            log.error("One or more file or directory has no read- or write-permission. " +
                    "This may lead to corrupted server packs! " +
                    "Check the permissions of the ServerPackCreator base directory!")
        }

        try {
            JarUtilities.copyFolderFromJar(
                this.javaClass,
                "de/griefed/resources/manifests",
                apiProperties.manifestsDirectory.absolutePath,
                "",
                xmlJsonRegex,
                apiProperties.tempDirectory
            )
        } catch (ex: IOException) {
            log.error("Error copying \"/de/griefed/resources/manifests\" from the JAR-file.", ex)
        }

        if (!File(apiProperties.pluginsDirectory, "disabled.txt").absoluteFile.isFile) {
            try {
                File(
                    apiProperties.pluginsDirectory, "disabled.txt"
                ).absoluteFile.writeText(
                    "########################################\n" + "#...Load all plugins except these......#\n" + "#...Add one plugin-id per line.........#\n" + "########################################\n" + "#example-plugin\n"
                )
            } catch (ex: IOException) {
                log.error("Error generating disable.txt in the plugins directory.", ex)
            }
        }
        val serverProperties = checkServerFilesFile(
            apiProperties.defaultServerProperties
        )
        val serverIcon = checkServerFilesFile(
            apiProperties.defaultServerIcon
        )
        overwriteServerFilesFile(apiProperties.defaultShellScriptTemplate)
        overwriteServerFilesFile(apiProperties.defaultPowerShellScriptTemplate)
        overwriteServerFilesFile(apiProperties.defaultBatchScriptTemplate)
        overwriteServerFilesFile(apiProperties.defaultJavaShellScriptTemplate)
        overwriteServerFilesFile(apiProperties.defaultJavaPowerShellScriptTemplate)
        overwriteServerFilesFile(apiProperties.defaultJavaBatchScriptTemplate)
        if (serverProperties || serverIcon) {
            log.warn("#################################################################")
            log.warn("#.............ONE OR MORE DEFAULT FILE(S) GENERATED.............#")
            log.warn("#..CHECK THE LOGS TO FIND OUT WHICH FILE(S) WAS/WERE GENERATED..#")
            log.warn("#...............CUSTOMIZE THEM BEFORE CONTINUING!...............#")
            log.warn("#################################################################")
        } else {
            log.info("Setup completed.")
        }

        // Print system information to console and logs.
        log.debug("Gathering system information to include in log to make debugging easier.")
        if (apiProperties.devBuild || apiProperties.preRelease) {
            log.debug("Warning user about possible data loss.")
            log.warn("################################################################")
            log.warn("#.............ALPHA | BETA | DEV VERSION DETECTED..............#")
            log.warn("#.............THESE VERSIONS ARE WORK IN PROGRESS!.............#")
            log.warn("#..USE AT YOUR OWN RISK! BE AWARE THAT DATA LOSS IS POSSIBLE!..#")
            log.warn("#........I WILL NOT BE HELD RESPONSIBLE FOR DATA LOSS!.........#")
            log.warn("#....................YOU HAVE BEEN WARNED!.....................#")
            log.warn("################################################################")
        }
        log.info("API INFORMATION:")
        log.info("API version:     ${apiProperties.apiVersion}")
        log.info("API home:        ${apiProperties.homeDirectory}")
        log.info("API Folder:      ${apiProperties.getJarFolder()}")
        log.info("API Path:        ${apiProperties.getJarFile()}")
        log.info("API Name:        ${apiProperties.getJarName()}")
        log.info("Include this information when reporting an issue on GitHub.")
    }

    /**
     * Initialize [de.griefed.serverpackcreator.api.versionmeta.VersionMeta], [ConfigurationHandler].
     *
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun stageTwo() {
        versionMeta
        configurationHandler
    }

    /**
     * Initialize [ApiPlugins], [de.griefed.serverpackcreator.api.modscanning.ModScanner] (consisting of [TomlParser],
     * [de.griefed.serverpackcreator.api.modscanning.ForgeAnnotationScanner],
     * [de.griefed.serverpackcreator.api.modscanning.FabricScanner],
     * [de.griefed.serverpackcreator.api.modscanning.ForgeTomlScanner],
     * [de.griefed.serverpackcreator.api.modscanning.QuiltScanner]),
     * [ServerPackHandler].
     *
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun stageThree() {
        apiPlugins
        forgeAnnotationScanner
        forgeTomlScanner
        neoForgeTomlScanner
        fabricScanner
        quiltScanner
        modScanner
        serverPackHandler
    }

    /**
     * Check whether the specified server-files file exists and create it if it doesn't.
     *
     * @param fileToCheckFor The file which is to be checked for whether it exists and if it doesn't,
     * should be created.
     * @return `true` if the file was generated.
     * @author Griefed
     */
    fun checkServerFilesFile(fileToCheckFor: File) = JarUtilities.copyFileFromJar(
        "de/griefed/resources/server_files/${fileToCheckFor.name}",
        File(apiProperties.serverFilesDirectory, fileToCheckFor.name),
        this.javaClass
    )

    /**
     * Overwrite the specified server-files file, even when it exists. Used to ensure files like the
     * default script templates are always up-to-date.
     *
     * @param fileToOverwrite The file which is to be overwritten. If it exists. it is first deleted,
     * then extracted from our JAR-file.
     * @author Griefed
     */
    fun overwriteServerFilesFile(fileToOverwrite: File) {
        File(apiProperties.serverFilesDirectory, fileToOverwrite.name).deleteQuietly()
        checkServerFilesFile(fileToOverwrite)
    }
}