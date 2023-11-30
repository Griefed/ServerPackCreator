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

import com.electronwill.nightconfig.toml.TomlParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.modscanning.*
import de.griefed.serverpackcreator.api.utilities.common.*
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
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
 * @param language   Language to use with ServerPackCreator. Ensure to use a language for which you have the localization available.
 * @param runSetup   Whether to run the file-setup during API inizialization.
 * @author Griefed
 */
actual class ApiWrapper private constructor(
    val properties: File = File("serverpackcreator.properties"),
    val language: Locale = Locale("en_GB"),
    runSetup: Boolean = true
) : Api<File>() {

    /**
     * This instances settings used across ServerPackCreator, such as the working-directories, files
     * and other settings.
     *
     * @return applicationProperties used across this ServerPackCreator-instance.
     * @author Griefed
     */
    @get:Synchronized
    actual val apiProperties: ApiProperties by lazy {
        ApiProperties(fileUtilities, systemUtilities, listUtilities, jarUtilities, properties)
    }

    /**
     * This instances common file utilities used across ServerPackCreator.
     *
     * @return Common file utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    actual val fileUtilities: FileUtilities = FileUtilities()

    /**
     * This instances common JAR-utilities used across ServerPackCreator.
     *
     * @return Common JAR-utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    actual val jarUtilities: JarUtilities = JarUtilities()

    /**
     * This instances common system utilities used across ServerPackCreator.
     *
     * @return Common system utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    actual val systemUtilities: SystemUtilities = SystemUtilities()

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
    actual val jsonUtilities: JsonUtilities = JsonUtilities(objectMapper)

    /**
     * This instances common web utilities used across ServerPackCreator.
     *
     * @return Common web utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    actual val webUtilities: WebUtilities by lazy {
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
    actual val xmlUtilities: XmlUtilities = XmlUtilities(documentBuilderFactory)

    /**
     * This instances collection of common utilities used across ServerPackCreator.
     *
     * @return Collection of common utilities used across ServerPackCreator.
     * @author Griefed
     */
    @get:Synchronized
    actual val utilities: Utilities by lazy {
        Utilities(
            booleanUtilities,
            fileUtilities,
            jarUtilities,
            listUtilities,
            stringUtilities,
            systemUtilities,
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
    actual val versionMeta: VersionMeta by lazy {
        VersionMeta(
            apiProperties.minecraftVersionManifest,
            apiProperties.forgeVersionManifest,
            apiProperties.neoForgeVersionManifest,
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
     * This instances ConfigurationHandler for checking a given [PackConfig] for
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
    actual val configurationHandler: ConfigurationHandler by lazy {
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
    actual val apiPlugins: ApiPlugins by lazy {
        ApiPlugins(tomlParser, apiProperties, versionMeta, utilities)
    }

    /**
     * This instances ServerPackHandler used to turn a [PackConfig] into a server pack.
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
    actual val serverPackHandler: ServerPackHandler by lazy {
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
    actual val modScanner: ModScanner by lazy {
        ModScanner(annotationScanner, fabricScanner, quiltScanner, tomlScanner)
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
    actual val annotationScanner: AnnotationScanner by lazy {
        AnnotationScanner(objectMapper, utilities)
    }

    /**
     * This instances scanner to determine the sideness of Fabric mods.
     *
     * @return Scanner to determine the sideness of Fabric mods.
     * @author Griefed
     */
    @get:Synchronized
    actual val fabricScanner: FabricScanner by lazy {
        FabricScanner(objectMapper, utilities)
    }

    /**
     * This instances scanner to determine the sideness of Quilt mods.
     *
     * @return Scanner to determine the sideness of Quilt mods.
     * @author Griefed
     */
    @get:Synchronized
    actual val quiltScanner: QuiltScanner by lazy {
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
    actual val tomlParser: TomlParser = TomlParser()

    /**
     * This instances toml scanner to determine the sideness of Forge mods for Minecraft 1.13.x and
     * newer.
     *
     * @return Scanner to determine the sideness of Forge mods for Minecraft 1.13.x and newer.
     * @author Griefed
     */
    @get:Synchronized
    actual val tomlScanner: TomlScanner = TomlScanner(tomlParser)

    init {
        apiProperties.changeLocale(language)
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
         * configurations for the home-directory and many other settings. For details, see the [Wiki](https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help#serverpackcreatorproperties).
         * @param language The language with which to initialize the localization. Format: `en_gb` or `de_de`
         * @param runSetup Whether to run the initial setup, creating and copying all required files to the filesystem and
         * ensuring the home-directory is properly set up and available to the API.
         * @author Griefed
         */
        @Synchronized
        fun api(
            properties: File = File("serverpackcreator.properties"),
            language: Locale = Locale("en_GB"),
            runSetup: Boolean = true
        ): ApiWrapper {
            if (api == null) {
                synchronized(this) {
                    if (api == null) {
                        api = ApiWrapper(properties, language, runSetup)
                    }
                }
            }
            return api!!
        }
    }

    /**
     * @author Griefed
     */
    @Synchronized
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun setup(force: Boolean): ApiWrapper {
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
     * @author Griefed
     */
    override fun stageOne() {
        utilities.jarUtilities.copyFileFromJar(
            "README.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        utilities.jarUtilities.copyFileFromJar(
            "HELP.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        utilities.jarUtilities.copyFileFromJar(
            "CHANGELOG.md", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )
        utilities.jarUtilities.copyFileFromJar(
            "LICENSE", true, this.javaClass, apiProperties.homeDirectory.absoluteFile.toString()
        )

        System.setProperty("file.encoding", StandardCharsets.UTF_8.name())
        if (!utilities.fileUtilities.isReadWritePermissionSet(apiProperties.getJarFolder())) {
            log.error(
                "One or more file or directory has no read- or write-permission." + " This may lead to corrupted server packs!" + " Check the permissions of the ServerPackCreator base directory!"
            )
        }

        try {
            var manifestPrefix = "BOOT-INF/classes"
            if (apiProperties.isExe()) {
                manifestPrefix = ""
                //source = "de/griefed/resources/manifests"
            }
            utilities.jarUtilities.copyFolderFromJar(
                this.javaClass,
                "de/griefed/resources/manifests",
                apiProperties.manifestsDirectory.absolutePath,
                manifestPrefix,
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
        if (apiProperties.apiVersion.matches(versionsRegex)) {
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
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun stageTwo() {
        versionMeta
        configurationHandler
    }

    /**
     * @author Griefed
     */
    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun stageThree() {
        apiPlugins
        annotationScanner
        fabricScanner
        quiltScanner
        modScanner
        serverPackHandler
    }

    /**
     * @author Griefed
     */
    override fun checkServerFilesFile(fileToCheckFor: File) = utilities.jarUtilities.copyFileFromJar(
        "de/griefed/resources/server_files/${fileToCheckFor.name}",
        File(apiProperties.serverFilesDirectory, fileToCheckFor.name),
        this.javaClass
    )

    /**
     * @author Griefed
     */
    override fun overwriteServerFilesFile(fileToOverwrite: File) {
        File(apiProperties.serverFilesDirectory, fileToOverwrite.name).deleteQuietly()
        checkServerFilesFile(fileToOverwrite)
    }
}