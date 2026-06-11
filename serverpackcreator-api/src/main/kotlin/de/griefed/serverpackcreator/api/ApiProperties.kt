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

import de.comahe.i18n4k.Locale
import de.comahe.i18n4k.config.I18n4kConfigDefault
import de.comahe.i18n4k.i18n4k
import de.griefed.serverpackcreator.api.settings.GenerationConfig
import de.griefed.serverpackcreator.api.settings.PathsConfig
import de.griefed.serverpackcreator.api.settings.WebserviceConfig
import de.comahe.i18n4k.toTag
import de.griefed.serverpackcreator.api.config.ExclusionFilter
import de.griefed.serverpackcreator.api.utilities.common.*
import org.apache.logging.log4j.core.Core
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.Order
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.xml.XmlConfiguration
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.*
import java.net.URI
import java.net.URL
import java.util.*
import java.util.prefs.Preferences

/**
 * Base settings of ServerPackCreator, such as working directories, default list of clientside-only
 * mods, default list of directories to include in a server pack, script templates, java paths and
 * much more.
 *
 * @param propertiesFile  serverpackcreator.properties-file containing settings and configurations to load the API with.
 * @author Griefed
 */
@Suppress("unused")
@Plugin(name = "ServerPackCreatorConfigFactory", category = Core.CATEGORY_NAME)
@Order(50)
class ApiProperties(propertiesFile: File = File("serverpackcreator.properties")) : ConfigurationFactory() {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    /**
     * Property-storage core handling file-loading, typed accessors and saving. ApiProperties
     * orchestrates load-ordering and domain-semantics on top of it (refactor Phase 1b).
     */
    private val store = PropertyStore()
    private val internalProps = store.properties
    private val spcPreferences = Preferences.userRoot().node("ServerPackCreator")
    private val serverPackCreatorProperties = "serverpackcreator.properties"
    private val jarInformation: JarInformation = JarInformation(this.javaClass)
    private val jarFolderProperties: File = File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties)

    private val pVersionCheckPreRelease =
        "de.griefed.serverpackcreator.versioncheck.prerelease"
    private val pLanguage =
        "de.griefed.serverpackcreator.language"
    private val pConfigurationFallbackUpdateURL =
        "de.griefed.serverpackcreator.configuration.fallback.updateurl"
    private val pConfigurationFallbackModsList =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist"
    private val pConfigurationFallbackModsListRegex =
        "de.griefed.serverpackcreator.configuration.fallbackmodslist.regex"
    private val pConfigurationFallbackModsWhiteList =
        "de.griefed.serverpackcreator.configuration.modswhitelist"
    private val pConfigurationHasteBinServerUrl =
        "de.griefed.serverpackcreator.configuration.hastebinserver"
    private val pConfigurationDirectoriesServerPacks =
        "de.griefed.serverpackcreator.configuration.directories.serverpacks"
    private val pServerPackStartScriptTemplatesPrefix =
        "de.griefed.serverpackcreator.serverpack.script.template."
    private val pServerPackJavaScriptTemplatesPrefix =
        "de.griefed.serverpackcreator.serverpack.java.template."
    private val pJavaForServerInstall =
        "de.griefed.serverpackcreator.java"
    private val pScriptVariablesJavaPaths =
        "de.griefed.serverpackcreator.script.java"
    private val pScriptVariablesAutoUpdateJavaPathsEnabled =
        "de.griefed.serverpackcreator.script.java.autoupdate"
    private val pOldVersion =
        "de.griefed.serverpackcreator.version.old"
    private val pLogLevel = "de.griefed.serverpackcreator.loglevel"

    @Deprecated("Deprecated as of 6.0.0")
    private val pServerPackScriptTemplates =
        "de.griefed.serverpackcreator.serverpack.script.template"

    private val suffixes = arrayOf(".xml")

    /**
     * Default home-directory for ServerPackCreator. The directory containing the
     * ServerPackCreator JAR.
     *
     * @author Griefed
     */
    val home: File = jarInformation.jarFolder.absoluteFile

    /**
     * Settings-group for server pack generation: mod-lists, directory in-/exclusions,
     * cleanup-files, ZIP-exclusions, exclusion-filter, generation-flags and Aikar's flags.
     * Prefer accessing these values through this group; the individual properties on
     * ApiProperties remain as facade.
     */
    val generationConfig = GenerationConfig(store)

    /**
     * Fallback-list of directories to include in a server pack.
     */
    val fallbackDirectoriesInclusion: TreeSet<String> get() = generationConfig.fallbackDirectoriesInclusion

    /**
     * Fallback-list of directories to exclude from a server pack.
     */
    val fallbackDirectoriesExclusion: TreeSet<String> get() = generationConfig.fallbackDirectoriesExclusion

    /**
     * Fallback-list of files to exclude from the server pack ZIP-archive.
     */
    val fallbackZipExclusions: TreeSet<String> get() = generationConfig.fallbackZipExclusions

    /**
     * Fallback-list of files to delete after a modloader-server installation.
     */
    val fallbackPostInstallCleanupFiles: TreeSet<String> get() = generationConfig.fallbackPostInstallCleanupFiles

    /**
     * Fallback-list of files to delete before a modloader-server installation.
     */
    val fallbackPreInstallCleanupFiles: TreeSet<String> get() = generationConfig.fallbackPreInstallCleanupFiles

    /**
     * Fallback Aikar's flags for the generated start-scripts.
     */
    val fallbackAikarsFlags: String get() = generationConfig.fallbackAikarsFlags

    val fallbackUpdateURL =
        "https://raw.githubusercontent.com/Griefed/ServerPackCreator/main/serverpackcreator-api/src/main/resources/serverpackcreator.properties"
    val fallbackExclusionFilter: ExclusionFilter get() = generationConfig.fallbackExclusionFilter
    val fallbackOverwriteEnabled: Boolean get() = generationConfig.fallbackOverwriteEnabled
    val fallbackJavaScriptAutoupdateEnabled = true
    val fallbackCheckingForPreReleasesEnabled = false
    val fallbackZipFileExclusionEnabled: Boolean get() = generationConfig.fallbackZipFileExclusionEnabled
    val fallbackServerPackCleanupEnabled: Boolean get() = generationConfig.fallbackServerPackCleanupEnabled
    val fallbackMinecraftPreReleasesAvailabilityEnabled: Boolean get() = generationConfig.fallbackMinecraftPreReleasesAvailabilityEnabled
    val fallbackAutoExcludingModsEnabled: Boolean get() = generationConfig.fallbackAutoExcludingModsEnabled
    val fallbackArtemisQueueMaxDiskUsage = 90
    val fallbackCleanupSchedule = "0 0 0 * * *"
    val fallbackVersionSchedule = "0 0 0 * * *"
    val fallbackDatabaseCleanupSchedule = "0 0 0 * * *"
    val fallbackUpdateServerPack: Boolean get() = generationConfig.fallbackUpdateServerPack
    private val checkedJavas = hashMapOf<String, Boolean>()
    private val trueFalseRegex = "^(true|false)$".toRegex()
    private val alphaBetaRegex = "^(.*alpha.*|.*beta.*)$".toRegex()
    val i18n4kConfig = I18n4kConfigDefault()

    /**
     * String-list of clientside-only mods to exclude from server packs.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val clientsideMods: TreeSet<String> get() = generationConfig.clientsideMods

    /**
     * String-list of mods to include if present, regardless whether a match was found through [clientsideMods].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val modsWhitelist: TreeSet<String> get() = generationConfig.modsWhitelist

    /**
     * Regex-list of clientside-only mods to exclude from server packs.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val clientsideModsRegex: TreeSet<String> get() = generationConfig.clientsideModsRegex

    /**
     * Regex-list of mods to include if present, regardless whether a match was found throug [clientsideModsRegex].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val modsWhitelistRegex: TreeSet<String> get() = generationConfig.modsWhitelistRegex

    /**
     * Modloaders supported by ServerPackCreator.
     */
    val supportedModloaders = arrayOf("Fabric", "Forge", "Quilt", "LegacyFabric", "NeoForge")

    /**
     * The folder containing the ServerPackCreator.exe or JAR-file.
     *
     * @return Folder containing the ServerPackCreator.exe or JAR-file.
     * @author Griefed
     */
    fun getJarFolder() = jarInformation.jarFolder

    /**
     * Whether a .exe or JAR-file was used for running ServerPackCreator.
     *
     * @return `true` if a .exe was/is used.
     * @author Griefed
     */
    fun isExe() = jarInformation.isExe

    /**
     * The .exe or JAR-file of ServerPackCreator.
     *
     * @return The .exe or JAR-file of ServerPackCreator.
     * @author Griefed
     */
    fun getJarFile() = jarInformation.jarFile

    /**
     * The name of the .exe or JAR-file.
     *
     * @return The name of the .exe or JAR-file.
     * @author Griefed
     */
    fun getJarName(): String = jarInformation.jarFile.name

    /**
     * The Java version used to run ServerPackCreator.
     *
     * @return Java version.
     * @author Griefed
     */
    fun getJavaVersion() = jarInformation.javaVersion

    /**
     * Architecture of the operating system on which ServerPackCreator is running on.
     *
     * @return Arch.
     * @author Griefed
     */
    fun getOSArch() = jarInformation.osArch

    /**
     * The name of the operating system on which ServerPackCreator is running on.
     *
     * @return OS name.
     * @author Griefed
     */
    fun getOSName() = jarInformation.osName

    /**
     * The version of the OS on which ServerPackCreator is running on.
     *
     * @return Version of the OS.
     * @author Griefed
     */
    fun getOSVersion() = jarInformation.osVersion

    /**
     * The version of the ServerPackCreator API.
     */
    val apiVersion: String = javaClass.getPackage().implementationVersion ?: "dev"

    val devBuild: Boolean
        get() {
            return apiVersion == "dev"
        }

    val preRelease: Boolean
        get() {
            return apiVersion.matches(alphaBetaRegex)
        }

    val configVersion: String = if (preRelease || devBuild) {
        "TEST"
    } else {
        "4"
    }

    /**
     * Only the first call to this property will return true if this is the first time ServerPackCreator is being run
     * on a given host. Any subsequent call will return false. Handle with care!
     *
     * @author Griefed
     */
    val firstRun: Boolean

    var logLevel = "INFO"
        get() {
            field = acquireProperty(pLogLevel, "INFO").uppercase()
            return field
        }
        set(value) {
            field = value.uppercase()
            defineProperty(pLogLevel, field)
            setLoggingLevel(field)
        }

    /**
     * Directories to include in a server pack.
     */
    var directoriesToInclude: TreeSet<String>
        get() = generationConfig.directoriesToInclude
        set(value) {
            generationConfig.directoriesToInclude = value
        }

    /**
     * Directories to exclude from a server pack.
     */
    var directoriesToExclude: TreeSet<String>
        get() = generationConfig.directoriesToExclude
        set(value) {
            generationConfig.directoriesToExclude = value
        }

    /**
     * List of files to delete after a server pack server installation.
     */
    var postInstallCleanupFiles: TreeSet<String>
        get() = generationConfig.postInstallCleanupFiles
        set(value) {
            generationConfig.postInstallCleanupFiles = value
        }

    /**
     * List of files to delete before a server pack server installation.
     */
    var preInstallCleanupFiles: TreeSet<String>
        get() = generationConfig.preInstallCleanupFiles
        set(value) {
            generationConfig.preInstallCleanupFiles = value
        }

    /**
     * List of files to be excluded from ZIP-archives. Current filters are:
     *
     *  * `MINECRAFT_VERSION` - Will be replaced with the Minecraft version of the server pack
     *  * `MODLOADER` - Will be replaced with the modloader of the server pack
     *  * `MODLOADER_VERSION` - Will be replaced with the modloader version of the server pack
     *
     * Should you want these filters to be expanded, open an issue on [GitHub](https://github.com/Griefed/ServerPackCreator/issues)
     */
    var zipArchiveExclusions: TreeSet<String>
        get() = generationConfig.zipArchiveExclusions
        set(value) {
            generationConfig.zipArchiveExclusions = value
        }

    /**
     * Paths to Java installations available to SPC for automatically updating the script variables of a given server pack
     * configuration.
     * * key: Java version
     * * value: Path to the Java .exe or binary
     *
     * If you plan on overwriting this property, make sure to format they key-value-pairs as follows:
     * * key: `de.griefed.serverpackcreator.script.java` followed by the number representing the Java version
     * * value: Valid path to a Java installation corresponding to the number used in the key
     */
    var javaPaths = HashMap<String, String>(256)
        get() {
            val paths = HashMap<String, String>(256)
            var path: String
            var position: String
            for (i in 8..255) {
                position = pScriptVariablesJavaPaths + i
                path = internalProps.getProperty(position, "")
                if (checkJavaPath(path)) {
                    paths[i.toString()] = path
                    internalProps.setProperty(position, path)
                }
            }
            field = paths
            return paths
        }
        set(values) {
            var position: Int?
            var newKey: String
            val paths = HashMap<String, String>(256)
            for (i in 8..255) {
                internalProps.remove(pScriptVariablesJavaPaths + i)
            }
            for ((key, value) in values) {
                if (!checkJavaPath(value)) {
                    continue
                }
                position = key.replace(pScriptVariablesJavaPaths, "").toIntOrNull()
                newKey = pScriptVariablesJavaPaths + position
                if (position != null && 8 <= position!! && position!! < 256) {
                    internalProps.setProperty(newKey, value)
                    paths[newKey] = value
                }
            }
            field = paths
            log.info("Available Java paths for scripts:")
            for ((key, value) in field) {
                log.info("Java $key path: $value")
            }
        }

    fun getPreference(pref: String, def: String? = null) : Optional<String> {
        return Optional.ofNullable(spcPreferences.get(pref, def))
    }

    fun storePreference(pref: String, value: String) {
        spcPreferences.put(pref, value)
        spcPreferences.sync()
    }

    /**
     * Default list of script templates used by ServerPackCreator.
     *
     * @author Griefed
     */
    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("defaultScriptTemplateMap"))
    fun defaultScriptTemplates(): List<File> {
        // See whether we have custom files.
        val currentFiles = serverFilesDirectory.walk().maxDepth(1).filter {
            it.name.endsWith("sh", ignoreCase = true) ||
                    it.name.endsWith("ps1", ignoreCase = true) ||
                    it.name.endsWith("bat", ignoreCase = true)
        }.toList()
        val customTemplates = currentFiles.filter {
            !it.name.contains("default_template", ignoreCase = true)
        }

        val newTemplates = mutableListOf<File>()
        var shellPresent = false
        var powershellPresent = false
        var batchPresent = false
        for (customTemplate in customTemplates) {
            when {
                customTemplate.name.endsWith("sh", ignoreCase = true) && !shellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    shellPresent = true
                }

                customTemplate.name.endsWith("ps1", ignoreCase = true) && !powershellPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    powershellPresent = true
                }

                customTemplate.name.endsWith("bat", ignoreCase = true) && !batchPresent -> {
                    newTemplates.add(customTemplate.absoluteFile)
                    batchPresent = true
                }

                else -> {
                    newTemplates.add(customTemplate.absoluteFile)
                }
            }
        }

        if (!shellPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultShellScriptTemplate.name).absoluteFile)
        }
        if (!powershellPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultPowerShellScriptTemplate.name).absoluteFile)
        }
        if (!batchPresent) {
            newTemplates.add(File(serverFilesDirectory.absolutePath, defaultBatchScriptTemplate.name).absoluteFile)
        }

        return newTemplates.toList()
    }

    @Deprecated("Deprecated as of 6.0.0", ReplaceWith("startScriptTemplates"))
    var scriptTemplates: TreeSet<File> = TreeSet()
        get() {
            val scriptSetting = internalProps.getProperty(pServerPackScriptTemplates)
            val entries =
                if (scriptSetting != null && scriptSetting == "default_template.ps1,default_template.sh,default_template.bat") {
                    defaultScriptTemplates()
                } else {
                    getListProperty(
                        pServerPackScriptTemplates,
                        defaultScriptTemplates().joinToString(",") { it.absolutePath }
                    ).map { File(it).absoluteFile }
                }
            field.clear()
            field.addAll(entries)
            return field
        }
        set(value) {
            val entries = value.map { it.absolutePath }
            setListProperty(pServerPackScriptTemplates, entries, ",")
            field.clear()
            field.addAll(value.map { it.absoluteFile })
            log.info("Using script templates:")
            for (template in field) {
                log.info("    " + template.path)
            }
        }

    /**
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultStartScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(serverFilesDirectory.absolutePath, defaultShellScriptTemplate.name).absolutePath),
            Pair("ps1", File(serverFilesDirectory.absolutePath, defaultPowerShellScriptTemplate.name).absolutePath),
            Pair("bat", File(serverFilesDirectory.absolutePath, defaultBatchScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var startScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = internalProps.keys
                .filter { entry -> (entry as String).startsWith(pServerPackStartScriptTemplatesPrefix) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(pServerPackStartScriptTemplatesPrefix, "").isBlank()
                }) {
                log.warn("Found empty definitions for start script templates. Using defaults.")
                field = defaultStartScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(pServerPackStartScriptTemplatesPrefix, "")
                    field[type] = File(internalProps[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No start script templates defined. Using defaults.")
                field = defaultStartScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                defineProperty("$pServerPackStartScriptTemplatesPrefix$key", value)
                log.info("Set $pServerPackStartScriptTemplatesPrefix$key to $value")
            }
            field = map
        }

    /**
     * Default map of start-script templates: sh, ps1, bat.
     */
    fun defaultJavaScriptTemplates(): HashMap<String, String> {
        return hashMapOf(
            Pair("sh", File(serverFilesDirectory.absolutePath, defaultJavaShellScriptTemplate.name).absolutePath),
            Pair("ps1", File(serverFilesDirectory.absolutePath, defaultJavaPowerShellScriptTemplate.name).absolutePath)
        )
    }

    /**
     * Start-script templates to use during server pack generation.
     * Each key represents a different template and script-type.
     */
    var javaScriptTemplates: HashMap<String, String> = hashMapOf()
        get() {
            val templateProps = internalProps.keys
                .filter { entry -> (entry as String).startsWith(pServerPackJavaScriptTemplatesPrefix) }
                .map { entry -> entry as String }
            var type: String
            if (templateProps.isEmpty() || templateProps.any { entry ->
                    entry.replace(
                        pServerPackJavaScriptTemplatesPrefix,
                        ""
                    ).isBlank()
                }) {
                log.warn("Found empty definitions for java script templates. Using defaults.")
                field = defaultJavaScriptTemplates()
            } else {
                for (templateProp in templateProps) {
                    type = templateProp.replace(pServerPackJavaScriptTemplatesPrefix, "")
                    field[type] = File(internalProps[templateProp] as String).absolutePath
                }
            }
            if (field.isEmpty()) {
                log.error("No java script templates defined. Using defaults.")
                field = defaultJavaScriptTemplates()
            }
            return field
        }
        set(map) {
            for ((key, value) in map) {
                defineProperty("$pServerPackJavaScriptTemplatesPrefix$key", value)
                log.info("Set $pServerPackJavaScriptTemplatesPrefix$key to $value")
            }
            field = map
        }

    /**
     * The URL from which a .properties-file is read during updating of the fallback clientside-mods list.
     * The default can be found in [fallbackUpdateURL].
     */
    var updateUrl: URL = URI(fallbackUpdateURL).toURL()
        get() {
            field = URI(acquireProperty(pConfigurationFallbackUpdateURL, fallbackUpdateURL)).toURL()
            return field
        }
        set(value) {
            defineProperty(pConfigurationFallbackUpdateURL, value.toString())
            field = value
        }

    /**
     * The filter method with which to determine whether a user-specified clientside-only mod should
     * be excluded from the server pack. Available settings are:
     *
     *  * [ExclusionFilter.START]
     *  * [ExclusionFilter.END]
     *  * [ExclusionFilter.CONTAIN]
     *  * [ExclusionFilter.REGEX]
     *  * [ExclusionFilter.EITHER]
     */
    var exclusionFilter: ExclusionFilter
        get() = generationConfig.exclusionFilter
        set(value) {
            generationConfig.exclusionFilter = value
        }

    /**
     * Whether the search for available PreReleases is enabled or disabled. Depending on
     * `de.griefed.serverpackcreator.versioncheck.prerelease`, returns `true` if checks for available PreReleases are
     * enabled, `false` if no checks for available PreReleases should be made.
     */
    var isCheckingForPreReleasesEnabled = fallbackCheckingForPreReleasesEnabled
        get() {
            field = getBoolProperty(pVersionCheckPreRelease, fallbackCheckingForPreReleasesEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pVersionCheckPreRelease, value)
            field = value
            log.info("Checking for pre-releases set to $field.")
        }

    /**
     * Whether the exclusion of files from the ZIP-archive of the server pack is enabled.
     */
    var isZipFileExclusionEnabled: Boolean
        get() = generationConfig.isZipFileExclusionEnabled
        set(value) {
            generationConfig.isZipFileExclusionEnabled = value
        }

    /**
     * Is auto excluding of clientside-only mods enabled.
     */
    var isAutoExcludingModsEnabled: Boolean
        get() = generationConfig.isAutoExcludingModsEnabled
        set(value) {
            generationConfig.isAutoExcludingModsEnabled = value
        }

    /**
     * Whether overwriting of already existing server packs is enabled.
     */
    var isServerPacksOverwriteEnabled: Boolean
        get() = generationConfig.isServerPacksOverwriteEnabled
        set(value) {
            generationConfig.isServerPacksOverwriteEnabled = value
        }

    /**
     * Whether cleanup procedures after server pack generation are enabled.
     */
    var isServerPackCleanupEnabled: Boolean
        get() = generationConfig.isServerPackCleanupEnabled
        set(value) {
            generationConfig.isServerPackCleanupEnabled = value
        }

    /**
     * Whether Minecraft pre-releases and snapshots are available to the user in, for example, the GUI.
     */
    var isMinecraftPreReleasesAvailabilityEnabled: Boolean
        get() = generationConfig.isMinecraftPreReleasesAvailabilityEnabled
        set(value) {
            generationConfig.isMinecraftPreReleasesAvailabilityEnabled = value
        }

    /**
     * Whether a server pack should be updated instead of cleanly generated.
     */
    var isUpdatingServerPacksEnabled: Boolean
        get() = generationConfig.isUpdatingServerPacksEnabled
        set(value) {
            generationConfig.isUpdatingServerPacksEnabled = value
        }

    /**
     * Whether to automatically update the `SPC_JAVA_SPC`-placeholder in the script variables
     * table with a Java path matching the required Java version for the Minecraft server.
     */
    var isJavaScriptAutoupdateEnabled = fallbackJavaScriptAutoupdateEnabled
        get() {
            field = getBoolProperty(pScriptVariablesAutoUpdateJavaPathsEnabled, fallbackJavaScriptAutoupdateEnabled)
            return field
        }
        set(value) {
            setBoolProperty(pScriptVariablesAutoUpdateJavaPathsEnabled, value)
            field = value
            log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $field")
        }

    /**
     * Aikars Flags commonly used for Minecraft servers to improve performance in various places.
     */
    var aikarsFlags: String
        get() = generationConfig.aikarsFlags
        set(value) {
            generationConfig.aikarsFlags = value
        }

    /**
     * Web-service settings-group: database-URI and webservice-schedules. Prefer accessing these
     * through this group; the individual properties on ApiProperties remain as facade.
     */
    val webserviceConfig = WebserviceConfig(store)

    /**
     * Path to the database used by the webservice-side of ServerPackCreator.
     */
    var databaseUri: String
        get() = webserviceConfig.databaseUri
        set(value) {
            webserviceConfig.databaseUri = value
        }

    /**
     * Language used by ServerPackCreator.
     */
    var language = Locale("en", "GB")
        get() {
            val prop = internalProps.getProperty(pLanguage)
            val lang = if (prop.contains("_")) {
                val split = prop.split("_")
                if (split.size == 3) {
                    Locale(split[0], split[1], split[2])
                } else {
                    Locale(split[0], split[1])
                }
            } else {
                Locale(prop)
            }
            field = lang
            i18n4kConfig.locale = field
            return field
        }
        set(value) {
            internalProps.setProperty(pLanguage, value.toTag())
            i18n4kConfig.locale = value
            field = value
            log.info("Language set to: ${field.displayLanguage} (${field.toTag()}).")
        }

    /**
     * URL to the HasteBin server where logs and configs are uploaded to.
     */
    var hasteBinServerUrl = "https://haste.zneix.eu/documents"
        get() {
            field = acquireProperty(pConfigurationHasteBinServerUrl, "https://haste.zneix.eu/documents")
            return field
        }
        set(value) {
            defineProperty(pConfigurationHasteBinServerUrl, value)
            field = value
            log.info("HasteBin documents endpoint set to: $field")
        }

    /**
     * Java installation used for installing the modloader server during server pack creation.
     */
    var javaPath = "java"
        get() {
            val prop = internalProps.getProperty(pJavaForServerInstall, null)
            field = if (checkJavaPath(prop)) {
                prop
            } else {
                val acquired = acquireJavaPath()
                internalProps.setProperty(pJavaForServerInstall, acquired)
                acquired
            }
            return field
        }
        set(value) {
            if (checkJavaPath(value)) {
                internalProps.setProperty(pJavaForServerInstall, value)
                field = value
                log.info("Java path set to: $field")
            } else {
                log.error("Invalid Java path specified: $value")
            }
        }

    /**
     * Cron-schedule of the webservice's cleanup-job. Facade for [WebserviceConfig.cleanupSchedule].
     */
    var webserviceCleanupSchedule: String
        get() = webserviceConfig.cleanupSchedule
        set(value) {
            webserviceConfig.cleanupSchedule = value
        }

    /**
     * Cron-schedule of the webservice's version-refresh-job. Facade for
     * [WebserviceConfig.versionSchedule].
     */
    var webserviceVersionSchedule: String
        get() = webserviceConfig.versionSchedule
        set(value) {
            webserviceConfig.versionSchedule = value
        }

    /**
     * Cron-schedule of the webservice's file-cleanup-job. Facade for
     * [WebserviceConfig.databaseCleanupSchedule].
     */
    var webserviceDatabaseCleanupSchedule: String
        get() = webserviceConfig.databaseCleanupSchedule
        set(value) {
            webserviceConfig.databaseCleanupSchedule = value
        }

    /**
     * The default webservice database-URI, for resetting the configuration to factory-state.
     */
    fun defaultWebserviceDatabase(): String = webserviceConfig.defaultDatabase()

    /**
     * Settings-group for ServerPackCreators home-directory and every directory and file derived
     * from it. Prefer accessing these values through this group; the individual properties on
     * ApiProperties remain as facade.
     */
    val pathsConfig = PathsConfig(store, spcPreferences, jarInformation, devBuild)

    /**
     * ServerPackCreators home-directory, in which all important files and folders are stored in.
     */
    var homeDirectory: File
        get() = pathsConfig.homeDirectory
        set(value) {
            pathsConfig.homeDirectory = value
        }

    /**
     * The serverpackcreator.properties-file of the currently running instance.
     */
    val serverPackCreatorPropertiesFile: File get() = pathsConfig.serverPackCreatorPropertiesFile

    /**
     * Overrides-file which overrides any property set in the regular properties-file.
     */
    val overridesPropertiesFile: File get() = pathsConfig.overridesPropertiesFile

    /**
     * Default configuration-file for a server pack generation in the home-directory.
     */
    val defaultConfig: File get() = pathsConfig.defaultConfig

    /**
     * Directory in which GUI-created configurations are saved by default.
     */
    val configsDirectory: File get() = pathsConfig.configsDirectory

    /**
     * Base-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    var tomcatBaseDirectory: File
        get() = pathsConfig.tomcatBaseDirectory
        set(value) {
            pathsConfig.tomcatBaseDirectory = value
        }

    /**
     * The default Tomcat base-directory: the home-directory.
     */
    fun defaultTomcatBaseDirectory(): File = pathsConfig.defaultTomcatBaseDirectory()

    /**
     * The default server-packs directory: server-packs inside the home-directory.
     */
    fun defaultServerPacksDirectory(): File = pathsConfig.defaultServerPacksDirectory()

    /**
     * Directory in which generated server packs and their ZIP-archives are stored.
     */
    var serverPacksDirectory: File
        get() = pathsConfig.serverPacksDirectory
        set(value) {
            pathsConfig.serverPacksDirectory = value
        }

    /**
     * Storage-location for logs created by ServerPackCreator.
     */
    val logsDirectory: File get() = pathsConfig.logsDirectory

    /**
     * Logs-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    var tomcatLogsDirectory: File
        get() = pathsConfig.tomcatLogsDirectory
        set(value) {
            pathsConfig.tomcatLogsDirectory = value
        }

    /**
     * The default Tomcat logs-directory: logs inside the home-directory.
     */
    fun defaultTomcatLogsDirectory(): File = pathsConfig.defaultTomcatLogsDirectory()

    /**
     * Directory to which default/fallback version-manifests are copied during startup.
     */
    val manifestsDirectory: File get() = pathsConfig.manifestsDirectory

    /**
     * The Fabric intermediaries-manifest, used by Quilt, Fabric and LegacyFabric.
     */
    val fabricIntermediariesManifest: File get() = pathsConfig.fabricIntermediariesManifest

    /**
     * The LegacyFabric game-version manifest.
     */
    val legacyFabricGameManifest: File get() = pathsConfig.legacyFabricGameManifest

    /**
     * The LegacyFabric loader-manifest.
     */
    val legacyFabricLoaderManifest: File get() = pathsConfig.legacyFabricLoaderManifest

    /**
     * The LegacyFabric installer-manifest.
     */
    val legacyFabricInstallerManifest: File get() = pathsConfig.legacyFabricInstallerManifest

    /**
     * The Fabric installer-manifest.
     */
    val fabricInstallerManifest: File get() = pathsConfig.fabricInstallerManifest

    /**
     * The Quilt version-manifest.
     */
    val quiltVersionManifest: File get() = pathsConfig.quiltVersionManifest

    /**
     * The Quilt installer-manifest.
     */
    val quiltInstallerManifest: File get() = pathsConfig.quiltInstallerManifest

    /**
     * The Forge version-manifest.
     */
    val forgeVersionManifest: File get() = pathsConfig.forgeVersionManifest

    /**
     * The old NeoForge version-manifest, covering Minecraft 1.20.1 only.
     */
    val oldNeoForgeVersionManifest: File get() = pathsConfig.oldNeoForgeVersionManifest

    /**
     * The new NeoForge version-manifest.
     */
    val newNeoForgeVersionManifest: File get() = pathsConfig.newNeoForgeVersionManifest

    /**
     * The Fabric version-manifest.
     */
    val fabricVersionManifest: File get() = pathsConfig.fabricVersionManifest

    /**
     * Directory in which Minecraft-server manifests are stored.
     */
    val minecraftServerManifestsDirectory: File get() = pathsConfig.minecraftServerManifestsDirectory

    /**
     * The Minecraft version-manifest.
     */
    val minecraftVersionManifest: File get() = pathsConfig.minecraftVersionManifest

    /**
     * Work-directory for storage of files and folders required temporarily during any operation.
     */
    val workDirectory: File get() = pathsConfig.workDirectory

    /**
     * Caching-directory for various types of installers.
     */
    val installerCacheDirectory: File get() = pathsConfig.installerCacheDirectory

    /**
     * Temp-directory storing files and folders required temporarily during a run.
     */
    val tempDirectory: File get() = pathsConfig.tempDirectory

    /**
     * Modpacks-directory in which uploaded modpack-archives and extracted modpacks are stored.
     */
    val modpacksDirectory: File get() = pathsConfig.modpacksDirectory

    /**
     * Directory in which default server-files are stored in.
     */
    val serverFilesDirectory: File get() = pathsConfig.serverFilesDirectory

    /**
     * The default shell-template for the modded server start-scripts.
     */
    val defaultShellScriptTemplate: File get() = pathsConfig.defaultShellScriptTemplate

    /**
     * The default PowerShell-template for the modded server start-scripts.
     */
    val defaultPowerShellScriptTemplate: File get() = pathsConfig.defaultPowerShellScriptTemplate

    /**
     * The default Batch-template for the modded server start-scripts.
     */
    val defaultBatchScriptTemplate: File get() = pathsConfig.defaultBatchScriptTemplate

    /**
     * The default shell-template for the java-install scripts.
     */
    val defaultJavaShellScriptTemplate: File get() = pathsConfig.defaultJavaShellScriptTemplate

    /**
     * The default PowerShell-template for the java-install scripts.
     */
    val defaultJavaPowerShellScriptTemplate: File get() = pathsConfig.defaultJavaPowerShellScriptTemplate

    /**
     * The default Batch-template for the java-install scripts.
     */
    val defaultJavaBatchScriptTemplate: File get() = pathsConfig.defaultJavaBatchScriptTemplate

    /**
     * Directory in which properties for quick selection are stored and retrieved.
     */
    val propertiesDirectory: File get() = pathsConfig.propertiesDirectory

    /**
     * Directory in which icons for quick selection are stored and retrieved.
     */
    val iconsDirectory: File get() = pathsConfig.iconsDirectory

    /**
     * Default server.properties-file used by Minecraft servers.
     */
    val defaultServerProperties: File get() = pathsConfig.defaultServerProperties

    /**
     * Default server-icon.png-file used by Minecraft servers.
     */
    val defaultServerIcon: File get() = pathsConfig.defaultServerIcon

    /**
     * Directory in which plugins for ServerPackCreator are to be placed in.
     */
    val pluginsDirectory: File get() = pathsConfig.pluginsDirectory

    /**
     * Directory in which plugin-specific configurations are stored in.
     */
    val pluginsConfigsDirectory: File get() = pathsConfig.pluginsConfigsDirectory

    /**
     * Loads the given properties-file into [props] via the [store], dropping blank values and
     * tracking the file for later saving.
     */
    private fun loadFile(propertiesFile: File, props: Properties) {
        store.loadInto(propertiesFile, props)
    }

    /**
     * Loads the overrides-properties directly into the store, replacing already-loaded values.
     */
    fun loadOverrides(properties: File = overridesPropertiesFile) {
        store.loadOverrides(properties)
    }

    /**
     * Load properties using the default file path.
     * Only call this method on an already initialized ApiProperties-object.
     *
     * @author Griefed
     */
    fun loadProperties(saveProps: Boolean = true) {
        loadProperties(serverPackCreatorPropertiesFile, saveProps)
    }

    /**
     * Reload from a specific properties-file.
     *
     * @param propertiesFile The properties-file with which to loadProperties the settings and
     * configuration.
     * @author Griefed
     */
    fun loadProperties(
        propertiesFile: File = File(serverPackCreatorProperties).absoluteFile,
        saveProps: Boolean = true
    ) {
        val props = Properties()
        val jarFolderFile = File(jarInformation.jarFolder.absoluteFile, serverPackCreatorProperties).absoluteFile
        val serverPackCreatorHomeDir = File(home, "ServerPackCreator").absoluteFile
        val homeDirFile = File(serverPackCreatorHomeDir, serverPackCreatorProperties).absoluteFile
        val relativeDirFile = File(serverPackCreatorProperties).absoluteFile

        // Load the properties file from the classpath, providing default values.
        try {
            javaClass.getResourceAsStream("/$serverPackCreatorProperties").use {
                props.load(it)
            }
            log.info("Loaded properties from classpath.")
        } catch (ex: Exception) {
            log.error("Couldn't read properties from classpath.", ex)
        }

        // If our properties-file exists in SPCs home directory, load it.
        loadFile(jarFolderFile, props)
        // If our properties-file exists in the users home dir ServerPackCreator-dir, load it.
        loadFile(homeDirFile, props)
        // If our properties-file in the directory from which the user is executing SPC exists, load it.
        loadFile(relativeDirFile, props)
        // Load the specified properties-file.
        loadFile(propertiesFile, props)

        internalProps.putAll(props)

        //Acquisition done, now, load from the home-directory
        loadFile(serverPackCreatorPropertiesFile, props)
        internalProps.putAll(props)

        internalProps.setProperty(PathsConfig.TOMCAT_BASE_DIRECTORY_KEY, homeDirectory.absolutePath)
        if (internalProps.getProperty(pLanguage) != "en_GB") {
            changeLocale(Locale(internalProps.getProperty(pLanguage)))
        }

        // Load all values from the overrides-properties
        loadOverrides(overridesPropertiesFile)

        if (updateFallback()) {
            log.info("Fallback lists updated.")
        } else {
            setFallbackModsList()
            setFallbackWhitelist()
        }
        if (saveProps) {
            //Store properties in the configured SPC home-directory
            saveProperties(serverPackCreatorPropertiesFile)
        }
    }

    /**
     * Set up our fallback list of clientside-only mods.
     *
     * @author Griefed
     */
    private fun setFallbackModsList() {
        generationConfig.loadFallbackModsList()
    }

    /**
     * Set up our fallback list of clientside-only mods.
     *
     * @author Griefed
     */
    private fun setFallbackWhitelist() {
        generationConfig.loadFallbackWhitelist()
    }

    /**
     * Get a property from our ApplicationProperties. If the property is not available, it is created
     * with the specified value, thus allowing subsequent calls.
     *
     * @param key          The key of the property to acquire.
     * @param defaultValue The default value for the specified key in case the key is not present or
     * empty.
     * @return The value stored in the specified key.
     * @author Griefed
     */
    private fun acquireProperty(key: String, defaultValue: String) =
        store.acquire(key, defaultValue)

    /**
     * Set a property in our ApplicationProperties.
     *
     * @param key   The key in which to store the property.
     * @param value The value to store in the specified key.
     * @return The [value] to which the [key] was set to.
     * @author Griefed
     */
    private fun defineProperty(key: String, value: String): String =
        store.define(key, value)

    /**
     * Get a list from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested list.
     * @author Griefed
     */
    private fun getListProperty(
        key: String,
        defaultValue: String
    ) = store.getList(key, defaultValue)

    /**
     * Join the [value] via usage of the [separator] and overwrite the [key].
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun setListProperty(key: String, value: List<String>, separator: String) {
        store.setList(key, value, separator)
    }

    /**
     * Get an integer from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested integer.
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun getIntProperty(key: String, defaultValue: Int) =
        store.getInt(key, defaultValue)

    /**
     * Set the integer property with the given [key] to the given [value].
     *
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun setIntProperty(key: String, value: Int) = store.setInt(key, value)

    /**
     * Get a list of files from our properties, with each file having a specific prefix.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @param filePrefix   The prefix every file should receive.
     * @return The requested list of files.
     * @author Griefed
     */
    @Suppress("SameParameterValue")
    private fun getFileListProperty(key: String, defaultValue: String, filePrefix: String): List<File> =
        store.getFileList(key, defaultValue, filePrefix)

    /**
     * Get a boolean from our properties.
     *
     * @param key          The key of the property which holds the comma-separated list.
     * @param defaultValue The default value to set the property to in case it is undefined.
     * @return The requested integer.
     * @author Griefed
     */
    private fun getBoolProperty(key: String, defaultValue: Boolean) =
        store.getBool(key, defaultValue)

    /**
     * Set the integer property with the given [key] to the given [value].
     *
     * @author Griefed
     */
    private fun setBoolProperty(key: String, value: Boolean) = store.setBool(key, value)


    /**
     * Check the given path to a Java installation for validity and return it, if it is valid. If the
     * passed path is a UNIX symlink or Windows lnk, it is resolved, then returned. If the passed path
     * is considered invalid, the system default is acquired and returned.
     *
     * @param pathToJava The path to check for whether it is a valid Java installation.
     * @return Returns the path to the Java installation. If user input was incorrect, SPC will try to
     * acquire the path automatically.
     * @author Griefed
     */
    fun acquireJavaPath(pathToJava: String? = null): String {
        var checkedJavaPath: String
        try {
            if (!pathToJava.isNullOrBlank()) {
                if (checkJavaPath(pathToJava)) {
                    return pathToJava
                }
                if (checkJavaPath("$pathToJava.exe")) {
                    return "$pathToJava.exe"
                }
                if (checkJavaPath("$pathToJava.lnk")) {
                    return FileUtilities.resolveLink(File("$pathToJava.lnk"))
                }
            }
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Acquired path to Java installation: $checkedJavaPath")
        } catch (ex: NullPointerException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: InvalidFileTypeException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        } catch (ex: IOException) {
            log.info("Java setting invalid or otherwise not usable. Using system default.")
            checkedJavaPath = SystemUtilities.acquireJavaPathFromSystem()
            log.debug("Automatically acquired path to Java installation: $checkedJavaPath", ex)
        }
        return checkedJavaPath
    }

    /**
     * Store the ApplicationProperties to disk, overwriting the existing one.
     *
     * @param propertiesFile The file to store the properties to.
     * @author Griefed
     */
    fun saveProperties(propertiesFile: File) {
        store.save(
            propertiesFile,
            alwaysWrite = serverPackCreatorPropertiesFile,
            removeKeys = listOf(pConfigurationFallbackModsListRegex)
        )
    }

    /**
     * Check whether the given path is a valid Java specification.
     *
     * @param pathToJava Path to the Java executable
     * @return `true` if the path is valid.
     * @author Griefed
     */
    private fun checkJavaPath(pathToJava: String?): Boolean {
        if (pathToJava.isNullOrBlank()) {
            return false
        }
        if (checkedJavas.containsKey(pathToJava)) {
            return checkedJavas[pathToJava]!!
        }
        val result: Boolean
        when (FileUtilities.checkFileType(pathToJava)) {
            FileType.FILE -> {
                result = testJava(pathToJava)
            }

            FileType.LINK, FileType.SYMLINK -> {
                result = try {
                    testJava(FileUtilities.resolveLink(File(pathToJava)))
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                } catch (ex: IOException) {
                    log.error("Could not read Java link/symlink.", ex)
                    false
                }
            }

            FileType.DIRECTORY -> {
                log.error("Directory specified. Path to Java must lead to a lnk, symlink or file.")
                result = false
            }

            FileType.INVALID -> result = false
        }
        checkedJavas[pathToJava] = result
        return result
    }

    /**
     * Test for a valid Java specification by trying to run `java -version`. If the command goes
     * through without errors, it is considered a correct specification.
     *
     * @param pathToJava Path to the java executable/binary.
     * @return `true` if the specified file is a valid Java executable/binary.
     * @author Griefed
     */
    private fun testJava(pathToJava: String): Boolean {
        val testSuccessful: Boolean = try {
            val processBuilder = ProcessBuilder(listOf(pathToJava, "-version"))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            while (bufferedReader.readLine() != null && bufferedReader.readLine() != "null") {
                println(bufferedReader.readLine())
            }
            bufferedReader.close()
            process.destroyForcibly()
            true
        } catch (e: IOException) {
            log.error("Invalid Java specified.")
            false
        }
        return testSuccessful
    }

    /**
     * Whether a viable path to a Java executable or binary has been configured for
     * ServerPackCreator.
     *
     * @return `true` if a viable path has been set.
     * @author Griefed
     */
    fun javaAvailable() = checkJavaPath(javaPath)

    /**
     * Writes the specified locale from -lang your_locale to a lang.properties file to ensure every
     * subsequent start of serverpackcreator is executed using said locale.
     *
     * @param locale The locale the user specified when they ran serverpackcreator with -lang
     * -your_locale.
     * @author Griefed
     */
    fun changeLocale(locale: Locale) {
        language = locale
        saveProperties(serverPackCreatorPropertiesFile)
        log.info("Changed locale to $language")
    }

    /**
     * Acquire the default fallback list of clientside-only mods. If
     * `de.griefed.serverpackcreator.serverpack.autodiscovery.filter` is set to
     * [ExclusionFilter.REGEX], a regex fallback list is returned.
     *
     * @return The fallback list of clientside-only mods.
     * @author Griefed
     */
    fun clientSideMods() = generationConfig.clientSideMods()

    /**
     * Acquire the default fallback list of whitelisted mods. If
     * `de.griefed.serverpackcreator.serverpack.autodiscovery.filter` is set to
     * [ExclusionFilter.REGEX], a regex fallback list is returned.
     *
     * @return The fallback list of whitelisted mods.
     * @author Griefed
     */
    fun whitelistedMods() = generationConfig.whitelistedMods()

    /**
     * Whether the fallback lists for clientside-mods and whitelisted mods have been updated.
     *
     * `true` if either was updated.
     */
    var fallbackUpdated: Boolean = false
        private set

    /**
     * Update the fallback clientside-only mod-list of our `serverpackcreator.properties` from
     * the main-repository or one of its mirrors.
     *
     * @return `true` if the fallback-property was updated.
     * @author Griefed
     */
    fun updateFallback(): Boolean {
        var properties: Properties? = null
        try {
            URI(
                acquireProperty(pConfigurationFallbackUpdateURL, fallbackUpdateURL)
            ).toURL().openStream().use {
                properties = Properties()
                properties!!.load(it)
            }
        } catch (e: IOException) {
            log.debug("GitHub could not be reached.", e)
        }
        fallbackUpdated = false
        if (properties != null) {
            val newBlacklist = properties!!.getProperty(pConfigurationFallbackModsList)
            val currentBlacklist = internalProps.getProperty(pConfigurationFallbackModsList)
            if (newBlacklist != null && currentBlacklist != newBlacklist) {
                internalProps.setProperty(pConfigurationFallbackModsList, newBlacklist)
                clientsideMods.clear()
                clientsideMods.addAll(internalProps.getProperty(pConfigurationFallbackModsList).split(","))
                log.info("The fallback-list for clientside only mods has been updated to: $clientsideMods")
                fallbackUpdated = true
            }

            val newWhitelist = properties!!.getProperty(pConfigurationFallbackModsWhiteList)
            val currentWhitelist = internalProps.getProperty(pConfigurationFallbackModsWhiteList)
            if (newWhitelist != null && currentWhitelist != newWhitelist) {
                internalProps.setProperty(pConfigurationFallbackModsWhiteList, newWhitelist)
                modsWhitelist.clear()
                modsWhitelist.addAll(internalProps.getProperty(pConfigurationFallbackModsWhiteList).split(","))
                log.info("The fallback-list for whitelisted mods has been updated to: $modsWhitelist")
                fallbackUpdated = true
            }
        }
        if (fallbackUpdated) {
            saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
        }
        return fallbackUpdated
    }

    /**
     * Store a custom property in the serverpackcreator.properties-file. Beware that every property you add
     * receives a prefix, to prevent clashes with any other properties.
     *
     * Said prefix consists of `custom.property.` followed by the property you specified coming in last.
     *
     * Say you have a value in the property `saved`, then the resulting property in the serverpackcreator.properties
     * would be:
     * * `custom.property.saved`
     *
     * @author Griefed
     */
    fun storeCustomProperty(property: String, value: String): String =
        store.storeCustomProperty(property, value)

    /**
     * Retrieve a custom property in the serverpackcreator.properties-file. Beware that every property you retrieve this
     * way contains a prefix, to prevent clashes with any other properties.
     *
     * Said prefix consists of `custom.property.` followed by the property you specified coming in last.
     *
     * Say you have a property `saved`, then the resulting property in the serverpackcreator.properties would be:
     * `custom.property.saved`
     *
     * @author Griefed
     */
    fun retrieveCustomProperty(property: String): String? =
        store.retrieveCustomProperty(property)

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: Int) =
        if (javaPaths.containsKey(javaVersion.toString())
            && javaPaths[javaVersion.toString()]?.let { File(it).isFile } == true
        ) {
            Optional.ofNullable(javaPaths[javaVersion.toString()])
        } else {
            Optional.empty()
        }

    /**
     * Get the path to the specified Java executable/binary, wrapped in an [Optional] for your
     * convenience.
     *
     * @param javaVersion The Java version to acquire the path for.
     * @return The path to the Java executable/binary, if available.
     * @author Griefed
     */
    fun javaPath(javaVersion: String) = javaPath(javaVersion.toInt())

    /**
     * Set the old version of ServerPackCreator used to perform necessary migrations between the old
     * and the current version.
     *
     * @param version Old version used before upgrading to the current version.
     * @author Griefed
     */
    fun setOldVersion(version: String) {
        internalProps.setProperty(pOldVersion, version)
        saveProperties(serverPackCreatorPropertiesFile)
    }

    /**
     * Get the old version of ServerPackCreator used to perform necessary migrations between the old
     * and the current version.
     *
     * @return Old version used before updating. Empty if this is the first run of ServerPackCreator.
     */
    fun oldVersion(): String = internalProps.getProperty(pOldVersion, "")

    fun clearPropertyFileList() {
        store.clearTrackedFiles()
    }

    private fun printSettings() {
        log.info("============================== PROPERTIES ==============================")
        log.info("Set Aikars flags to:   $aikarsFlags")
        log.info("Set database path to:  $databaseUri")
        log.info("Home directory set to: $homeDirectory")
        log.info("Language set to:       ${language.displayLanguage} (${language.toTag()})")
        log.info("Java path set to:      $javaPath")
        log.info("Set Tomcat base-directory to:       $tomcatBaseDirectory")
        log.info("Server packs directory set to:      $serverPacksDirectory")
        log.info("Set Tomcat logs-directory to:       $tomcatLogsDirectory")
        log.info("Checking for pre-releases set to:   $isCheckingForPreReleasesEnabled")
        log.info("Zip-file exclusion enabled set to:  $isZipFileExclusionEnabled")
        log.info("HasteBin documents endpoint set to: $hasteBinServerUrl")
        log.info("Directories which must always be included set to: $directoriesToInclude")
        log.info("Directories which must always be excluded set to: $directoriesToExclude")
        log.info("Cleanup of already existing server packs set to:  $isServerPackCleanupEnabled")
        log.info("Auto-discovery of clientside-only mods set to:    $isAutoExcludingModsEnabled")
        log.info("Overwriting of already existing server packs set to:        $isServerPacksOverwriteEnabled")
        log.info("Minecraft pre-releases and snapshots available set to:      $isMinecraftPreReleasesAvailabilityEnabled")
        log.info("Files which must be excluded from ZIP-archives set to:      $zipArchiveExclusions")
        log.info("User specified clientside-only mod exclusion filter set to: $exclusionFilter")
        log.info("Automatically update SPC_JAVA_SPC-placeholder in script variables table set to: $isJavaScriptAutoupdateEnabled")
        log.info("Clientside-mods set to:")
        ListUtilities.printListToLogChunked(clientsideMods.toList(), 5, "    ", true)
        log.info("Regex clientside-mods-list set to:")
        ListUtilities.printListToLogChunked(clientsideModsRegex.toList(), 5, "    ", true)
        log.info("Available Java paths for scripts:")
        for ((key, value) in javaPaths) {
            log.info("    Java $key path: $value")
        }
        log.info("Using script templates:")
        for ((key, value) in startScriptTemplates) {
            log.info("    $key: $value")
        }
        log.info("============================== PROPERTIES ==============================")
    }

    val installLocationXml: File = File(home, "log4j2.xml")
    val log4jXml: File

    init {
        i18n4k = i18n4kConfig
        i18n4kConfig.defaultLocale = Locale("en_GB")
        loadProperties(propertiesFile, false)
        log4jXml = File(homeDirectory, "log4j2.xml")
        try {
            var log4j: String
            val oldLogs = "<Property name=\"log-path\">logs</Property>"
            val newLogs = "<Property name=\"log-path\">${logsDirectory.absolutePath}</Property>"
            this.javaClass.getResourceAsStream("/log4j2.xml").use {
                log4j = it?.readText().toString()
                log4j = log4j.replace(oldLogs, newLogs)
                if (!log4jXml.isFile || devBuild || preRelease) {
                    log4jXml.writeText(log4j)
                }
            }
        } catch (ex: IOException) {
            println("Error reading/writing log4j2.xml.")
            ex.printStackTrace()
        }

        if (devBuild || preRelease) {
            logLevel = "DEBUG"
        } else {
            setLoggingLevel(logLevel)
        }

        firstRun = getBoolProperty("de.griefed.serverpackcreator.firstrun", true)
        setBoolProperty("de.griefed.serverpackcreator.firstrun", false)
        logsDirectory.create(createFileOrDir = true, asDirectory = true)
        serverFilesDirectory.create(createFileOrDir = true, asDirectory = true)
        propertiesDirectory.create(createFileOrDir = true, asDirectory = true)
        iconsDirectory.create(createFileOrDir = true, asDirectory = true)
        configsDirectory.create(createFileOrDir = true, asDirectory = true)
        workDirectory.create(createFileOrDir = true, asDirectory = true)
        tempDirectory.create(createFileOrDir = true, asDirectory = true)
        modpacksDirectory.create(createFileOrDir = true, asDirectory = true)
        serverPacksDirectory.create(createFileOrDir = true, asDirectory = true)
        pluginsDirectory.create(createFileOrDir = true, asDirectory = true)
        pluginsConfigsDirectory.create(createFileOrDir = true, asDirectory = true)
        manifestsDirectory.create(createFileOrDir = true, asDirectory = true)
        minecraftServerManifestsDirectory.create(createFileOrDir = true, asDirectory = true)
        installerCacheDirectory.create(createFileOrDir = true, asDirectory = true)
        printSettings()
        saveProperties(File(homeDirectory, serverPackCreatorProperties).absoluteFile)
    }

    private fun setLoggingLevel(level: String) {
        var loggingConfig = log4jXml.readText()
        loggingConfig = loggingConfig.replace(
            "<Property name=\"log-level-spc\">.*</Property>".toRegex(),
            "<Property name=\"log-level-spc\">${level.uppercase()}</Property>"
        )
        log4jXml.writeText(loggingConfig)
    }

    companion object {
        /**
         * @author Griefed
         */
        @JvmStatic
        fun getSeparator(): String {
            return File.separator
        }
    }

    override fun getSupportedTypes(): Array<String> = suffixes

    /**
     * Depending on whether this is the first run of ServerPackCreator on a users machine, the default
     * log4j2 configuration may be present at different locations. The default one is the config
     * inside the home-directory of SPC, of which we will try to set up our logging with. If said file
     * fails for whatever reason, we will try to use a config inside the directory from which SPC was
     * executed. Should that fail, too, the config from the classpath is used, to ensure we always
     * have default configs available. Should that fail, too, though, log4j is set up with its own
     * default settings.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param source        configuration source passed from log4j itself. Attempts to overwrite it
     * are made, but if all else fails it is used to set up logging with log4j's
     * default config.
     * @return Custom configuration with proper logs-directory set.
     * @author Griefed
     */
    override fun getConfiguration(loggerContext: LoggerContext, source: ConfigurationSource): Configuration {
        val configSource: ConfigurationSource
        if (log4jXml.isFile) {
            try {
                return getXmlConfig(log4jXml, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $log4jXml.")
                ex.printStackTrace()
            }
        } else if (installLocationXml.isFile) {
            try {
                return getXmlConfig(installLocationXml, loggerContext)
            } catch (ex: IOException) {
                println("Couldn't parse $installLocationXml.")
                ex.printStackTrace()
            }
        }
        try {
            configSource = ConfigurationSource(this.javaClass.getResourceAsStream("/log4j2.xml")!!)
            return CustomXMLConfiguration(loggerContext, configSource)
        } catch (ex: IOException) {
            println("Couldn't parse resource log4j2.xml.")
            ex.printStackTrace()
        }
        return CustomXMLConfiguration(loggerContext, source)
    }

    private fun getXmlConfig(sourceFile: File, loggerContext: LoggerContext): CustomXMLConfiguration {
        val configSource: ConfigurationSource
        val stream = sourceFile.inputStream()
        configSource = ConfigurationSource(stream, sourceFile)
        val custom = CustomXMLConfiguration(loggerContext, configSource)
        stream.close()
        return custom
    }

    /**
     * Custom XmlConfiguration to pass our custom log4j2.xml config to log4j.
     *
     * Set up the XML configuration with the passed context and config source. For the config source
     * being used, [ApiProperties.getConfiguration] where
     * multiple attempts at creating a new private val log by lazy { cachedLoggerOf(this.javaClass) } using our own log4j2.xml are made
     * before the default log4j setup is used.
     *
     * @param loggerContext logger context passed from log4j itself
     * @param configSource  configuration source passed from
     * [ApiProperties.getConfiguration].
     * @author Griefed
     */
    inner class CustomXMLConfiguration(loggerContext: LoggerContext?, configSource: ConfigurationSource?) :
        XmlConfiguration(loggerContext, configSource)
}