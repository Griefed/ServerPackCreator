/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.api.settings

import de.griefed.serverpackcreator.api.PropertyStore
import de.griefed.serverpackcreator.api.utilities.common.JarInformation
import de.griefed.serverpackcreator.api.utilities.common.create
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.*
import java.util.prefs.Preferences

/**
 * Settings-group for ServerPackCreators home-directory and every directory and file derived from
 * it: configs, logs, manifests, work/temp, modpacks, server-files with the default templates,
 * plugins, the server-packs directory with its override, and the Tomcat-directories of the
 * webservice. The home-directory itself resolves through the given [preferences]. Extracted from
 * ApiProperties (refactor Phase 1b); ApiProperties remains the facade through which consumers
 * access these values.
 */
class PathsConfig(
    private val store: PropertyStore,
    private val preferences: Preferences,
    private val jarInformation: JarInformation,
    private val devBuild: Boolean
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val serverPacksRegex = "^(?:\\./)?server-packs$".toRegex()

    companion object {
        /**
         * Property- and preference-key holding ServerPackCreators home-directory.
         */
        const val HOME_DIRECTORY_KEY = "de.griefed.serverpackcreator.home"

        /**
         * Property-key holding the Tomcat base-directory of the webservice.
         */
        const val TOMCAT_BASE_DIRECTORY_KEY = "server.tomcat.basedir"

        /**
         * Property-key holding the Tomcat logs-directory of the webservice.
         */
        const val TOMCAT_LOGS_DIRECTORY_KEY = "server.tomcat.accesslog.directory"

        /**
         * Property-key holding the server-packs directory override.
         */
        const val SERVER_PACKS_DIRECTORY_KEY = "de.griefed.serverpackcreator.configuration.directories.serverpacks"

        /**
         * Filename of ServerPackCreators properties-file.
         */
        const val SERVERPACKCREATOR_PROPERTIES = "serverpackcreator.properties"
    }

    /**
     * Default home-directory for ServerPackCreator: the directory containing the
     * ServerPackCreator JAR.
     */
    val home: File = jarInformation.jarFolder.absoluteFile

    /**
     * Reads the given preference from the [preferences]-node, wrapped in an Optional.
     */
    private fun getPreference(pref: String, def: String? = null): Optional<String> =
        Optional.ofNullable(preferences.get(pref, def))

    /**
     * Stores the given preference in the [preferences]-node and syncs it to the backing-store.
     */
    private fun storePreference(pref: String, value: String) {
        preferences.put(pref, value)
        preferences.sync()
    }

    /**
     * ServerPackCreators home directory, in which all important files and folders are stored in.
     *
     * Changes made to this variable are stored in an overrides.properties inside the installation directory of the
     * ServerPackCreator application.
     *
     * Every operation is based on this home-directory, with the exception being the
     * [serverPacksDirectory], which can be configured independently of ServerPackCreators
     * home-directory.
     */
    var homeDirectory: File = home.absoluteFile
        get() {
            // An explicit `-D` wins outright, and is deliberately **not persisted**. It is what lets a host that must
            // not touch shared state pin its home -- above all a test JVM, whose working directory is the module's own
            // source tree, because `ApiWrapper.setup()` *writes* into the home directory (README, CHANGELOG,
            // server_files). Writing it into the preference as well would let a one-off override quietly replace the
            // user's durable setting, and every later read (including from another process) would inherit it.
            System.getProperty(HOME_DIRECTORY_KEY)?.takeIf { it.isNotBlank() }?.let { overridden ->
                field = File(overridden).absoluteFile
                if (!field.isDirectory) {
                    field.create(createFileOrDir = true, asDirectory = true)
                }
                return field
            }

            val setting = if (getPreference(HOME_DIRECTORY_KEY).isPresent) {
                getPreference(HOME_DIRECTORY_KEY).get()
            } else if (store.properties.containsKey(HOME_DIRECTORY_KEY) && store.properties.getProperty(HOME_DIRECTORY_KEY).isNotBlank()) {
                store.properties.getProperty(HOME_DIRECTORY_KEY)
            } else if (jarInformation.jarPath.toFile().isDirectory || devBuild) {
                // Dev environment
                File("").absolutePath
            } else if (File(System.getProperty("user.home")).isDirectory) {
                File(System.getProperty("user.home"),"ServerPackCreator").absolutePath
            } else {
                home.absolutePath
            }

            store.properties.remove(HOME_DIRECTORY_KEY)
            storePreference(HOME_DIRECTORY_KEY, setting)
            field = File(getPreference(HOME_DIRECTORY_KEY).get()).absoluteFile

            if (!field.isDirectory) {
                field.create(createFileOrDir = true, asDirectory = true)
            }

            return field
        }
        set(value) {
            storePreference(HOME_DIRECTORY_KEY, value.absolutePath)
            field = value.absoluteFile
            log.info("Home-directory set to: $field")
            log.warn("Restart ServerPackCreator for this change to take full effect.")
        }

    /**
     * The `serverpackcreator.properties`-file which both resulted from starting
     * ServerPackCreator and provided the settings, properties and configurations for the currently
     * running instance.
     */
    var serverPackCreatorPropertiesFile: File = File(homeDirectory, SERVERPACKCREATOR_PROPERTIES).absoluteFile
        get() {
            field = File(homeDirectory, SERVERPACKCREATOR_PROPERTIES).absoluteFile
            return field
        }
        private set

    /**
     * Overrides which, well, override, any property which may be set in the regular [serverPackCreatorPropertiesFile].
     */
    var overridesPropertiesFile: File = File(homeDirectory, "overrides.properties")
        get() {
            field = File(homeDirectory, "overrides.properties")
            return field
        }
        private set

    /**
     * Default configuration-file for a server pack generation inside ServerPackCreators
     * home-directory.
     */
    var defaultConfig: File = File(homeDirectory, "serverpackcreator.conf").absoluteFile
        get() {
            field = File(homeDirectory, "serverpackcreator.conf").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which ServerPackCreator configurations from the GUI get saved in by default.
     */
    var configsDirectory: File = File(homeDirectory, "configs").absoluteFile
        get() {
            field = File(homeDirectory, "configs").absoluteFile
            return field
        }
        private set

    /**
     * Base-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    var tomcatBaseDirectory: File = homeDirectory
        get() {
            val prop = store.properties.getProperty(TOMCAT_BASE_DIRECTORY_KEY, homeDirectory.absolutePath)
            val dir = if (prop != homeDirectory.absolutePath) {
                store.properties.setProperty(TOMCAT_BASE_DIRECTORY_KEY, homeDirectory.absolutePath)
                homeDirectory.absolutePath
            } else {
                store.properties.getProperty(TOMCAT_BASE_DIRECTORY_KEY, homeDirectory.absolutePath)
            }
            field = File(dir).absoluteFile
            return field
        }
        set(value) {
            store.properties.setProperty(TOMCAT_BASE_DIRECTORY_KEY, value.absolutePath)
            field = value.absoluteFile
            log.info("Set Tomcat base-directory to: $field")
        }

    fun defaultTomcatBaseDirectory(): File {
        return homeDirectory.absoluteFile
    }

    fun defaultServerPacksDirectory(): File {
        return File(homeDirectory, "server-packs").absoluteFile
    }

    /**
     * Directory in which generated server packs, or server packs being generated, are stored in, as
     * well as their ZIP-archives, if created.
     *
     * By default, this directory will be the `server-packs`-directory in the home-directory of
     * ServerPackCreator, but it can be configured using the property
     * `de.griefed.serverpackcreator.configuration.directories.serverpacks` and can even be
     * configured to be completely independent of ServerPackCreators home-directory.
     */
    var serverPacksDirectory: File = File(homeDirectory, "server-packs")
        get() {
            val prop = store.properties.getProperty(SERVER_PACKS_DIRECTORY_KEY)
            val directory: File = if (prop.isNullOrBlank() || prop.matches(serverPacksRegex)) {
                defaultServerPacksDirectory()
            } else {
                File(store.properties.getProperty(SERVER_PACKS_DIRECTORY_KEY))
            }
            if (field.absolutePath != directory.absolutePath) {
                field = directory
            }
            return field
        }
        set(value) {
            store.properties.setProperty(SERVER_PACKS_DIRECTORY_KEY, value.absolutePath)
            field = value.absoluteFile
            log.info("Server packs directory set to: $field")
        }

    /**
     * Storage location for logs created by ServerPackCreator. This is the `logs`-directory
     * inside ServerPackCreators home-directory.
     */
    var logsDirectory: File = File(homeDirectory, "logs").absoluteFile
        get() {
            field = File(homeDirectory, "logs").absoluteFile
            return field
        }
        private set

    /**
     * Logs-directory for Tomcat, used by the webservice-side of ServerPackCreator.
     */
    var tomcatLogsDirectory: File = logsDirectory
        get() {
            val default = logsDirectory.absolutePath
            val prop = store.properties.getProperty(TOMCAT_LOGS_DIRECTORY_KEY, default)
            val dir = if (File(prop).canWrite()) {
                store.properties.getProperty(TOMCAT_LOGS_DIRECTORY_KEY, default)
            } else {
                default
            }
            field = File(dir).absoluteFile
            return field
        }
        set(value) {
            store.properties.setProperty(TOMCAT_LOGS_DIRECTORY_KEY, value.absolutePath)
            field = value.absoluteFile
            log.info("Set Tomcat logs-directory to: $field")
        }

    fun defaultTomcatLogsDirectory(): File {
        return File(homeDirectory, "logs").absoluteFile
    }

    /**
     * Directory to which default/fallback manifests are copied to during the startup of
     * ServerPackCreator.
     *
     * When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] is initialized, the
     * manifests copied to this directory will provide ServerPackCreator with the information required
     * to check and create your server packs.
     *
     * By default, this is the `manifests`-directory inside ServerPackCreators home-directory.
     */
    var manifestsDirectory: File = File(homeDirectory, "manifests").absoluteFile
        get() {
            field = File(homeDirectory, "manifests").absoluteFile
            return field
        }
        private set

    /**
     * The Fabric intermediaries manifest containing all required information about Fabrics
     * intermediaries. These intermediaries are used by Quilt, Fabric and LegacyFabric.
     *
     *
     * By default, the `fabric-intermediaries-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var fabricIntermediariesManifest: File =
        File(manifestsDirectory, "fabric-intermediaries-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-intermediaries-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * The LegacyFabric game version manifest containing information about which Minecraft version
     * LegacyFabric is available for.
     *
     *
     * By default, the `legacy-fabric-game-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricGameManifest: File = File(manifestsDirectory, "legacy-fabric-game-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-game-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * LegacyFabric loader manifest containing information about Fabric loader maven versions.
     *
     * By default, the `legacy-fabric-loader-manifest.json`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricLoaderManifest: File = File(manifestsDirectory, "legacy-fabric-loader-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-loader-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * LegacyFabric installer manifest containing information about available LegacyFabric installers
     * with which to install a server.
     *
     * By default, the `legacy-fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var legacyFabricInstallerManifest: File =
        File(manifestsDirectory, "legacy-fabric-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "legacy-fabric-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Fabric installer manifest containing information about available Fabric installers with which
     * to install a server.
     *
     * By default, the `fabric-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var fabricInstallerManifest: File = File(manifestsDirectory, "fabric-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Quilt version manifest containing information about available Quilt loader versions.
     *
     * By default, the `quilt-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var quiltVersionManifest: File = File(manifestsDirectory, "quilt-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "quilt-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Quilt installer manifest containing information about available Quilt installers with which to
     * install a server.
     *
     * By default, the `quilt-installer-manifest.xml`-file resides in the
     * `manifests`-directory inside ServerPackCreators home-directory.
     */
    var quiltInstallerManifest: File = File(manifestsDirectory, "quilt-installer-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "quilt-installer-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Forge version manifest containing information about available Forge loader versions.
     *
     *
     * By default, the `forge-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var forgeVersionManifest: File = File(manifestsDirectory, "forge-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "forge-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * Old NeoForge version manifest containing information about available NeoForge loader versions.
     * This manifest only contains versions for Minecraft 1.20.1.
     *
     *
     * By default, the `neoforge-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var oldNeoForgeVersionManifest: File = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "neoforge-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * New NeoForge version manifest containing information about available NeoForge loader versions.
     * This manifest contains versions for Minecraft 1.20.2 and up.
     *
     *
     * By default, the `neoforge-manifest-new.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var newNeoForgeVersionManifest: File = File(manifestsDirectory, "neoforge-manifest-new.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "neoforge-manifest-new.xml").absoluteFile
            return field
        }
        private set

    /**
     * Fabric version manifest containing information about available Fabric loader versions.
     *
     *
     * By default, the `fabric-manifest.xml`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var fabricVersionManifest: File = File(manifestsDirectory, "fabric-manifest.xml").absoluteFile
        get() {
            field = File(manifestsDirectory, "fabric-manifest.xml").absoluteFile
            return field
        }
        private set

    /**
     * Directory to which Minecraft server manifests are copied during the startup of
     * ServerPackCreator.
     *
     * When the [de.griefed.serverpackcreator.api.versionmeta.VersionMeta] is initialized, the
     * manifests copied to this directory will provide ServerPackCreator with the information required
     * to check and create your server packs.
     *
     * The Minecraft server manifests contain information about the Java version required, the
     * download-URL of the server-JAR and much more.
     *
     * By default, this is the `mcserver`-directory inside the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var minecraftServerManifestsDirectory: File = File(manifestsDirectory, "mcserver").absoluteFile
        get() {
            field = File(manifestsDirectory, "mcserver").absoluteFile
            return field
        }
        private set

    /**
     * Minecraft version manifest containing information about available Minecraft versions.
     *
     * By default, the `minecraft-manifest.json`-file resides in the `manifests`-directory
     * inside ServerPackCreators home-directory.
     */
    var minecraftVersionManifest: File = File(manifestsDirectory, "minecraft-manifest.json").absoluteFile
        get() {
            field = File(manifestsDirectory, "minecraft-manifest.json").absoluteFile
            return field
        }
        private set

    /**
     * Work-directory for storing temporary, non-critical, files and directories.
     *
     * Any file and/or directory inside the work-directory is considered `safe-to-delete`,
     * meaning that it can safely be emptied when ServerPackCreator is not running, without running
     * the risk of corrupting anything. It is not recommended to empty this directory whilst
     * ServerPackCreator is running, as in that case, it may interfere with any currently running
     * operation.
     *
     * By default, this is the `work`-directory inside ServerPackCreators home-directory.
     */
    var workDirectory: File = File(homeDirectory, "work").absoluteFile
        get() {
            field = File(homeDirectory, "work").absoluteFile
            return field
        }
        private set

    /**
     * Caching directory for various types of installers. Mainly used by the version-meta for caching modloaders
     * server installers, but also used as the ServerPackCreator installer cache-directory in certain scenarios.
     *
     * @author Griefed
     */
    var installerCacheDirectory: File = File(workDirectory, "installers").absoluteFile
        get() {
            field = File(workDirectory, "installers").absoluteFile
            return field
        }
        private set

    /**
     * Temp-directory storing files and folders required temporarily during the run of a server pack
     * generation or other operations.
     *
     * One example would be when running ServerPackCreator as a webservice and uploading a zipped
     * modpack for the automatic creation of a server pack from said modpack.
     *
     * Any file and/or directory inside the work-directory is considered `safe-to-delete`,
     * meaning that it can safely be emptied when ServerPackCreator is not running, without running
     * the risk of corrupting anything. It is not recommended to empty this directory whilst
     * ServerPackCreator is running, as in that case, it may interfere with any currently running
     * operation.
     *
     *
     * By default, this directory is `work/temp` inside ServerPackCreators home-directory.
     */
    var tempDirectory: File = File(workDirectory, "temp").absoluteFile
        get() {
            field = File(workDirectory, "temp").absoluteFile
            return field
        }
        private set

    /**
     * Modpacks directory in which uploaded modpack ZIP-archives and extracted modpacks are stored.
     *
     * By default, this is the `modpacks`-directory inside the `temp`-directory inside
     * ServerPackCreators home-directory.
     */
    var modpacksDirectory: File = File(homeDirectory, "modpacks").absoluteFile
        get() {
            field = File(homeDirectory, "modpacks").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which default server-files are stored in.
     *
     * Default server-files are, for example, the `server.properties`, `server-icon.png`,
     * `default_template.sh` and `default_template.ps1`.
     *
     * The properties and icon are placeholders and/or templates for the user to change to their
     * liking, should they so desire. The script-templates serve as a one-size-fits-all template for
     * supporting `Forge`, `Fabric`, `LegacyFabric` and `Quilt`.
     *
     * By default, this directory is `server_files` inside ServerPackCreators home-directory.
     */
    var serverFilesDirectory: File = File(homeDirectory, "server_files").absoluteFile
        get() {
            field = File(homeDirectory, "server_files").absoluteFile
            return field
        }
        private set

    /**
     * The default shell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `startScriptTemplates`.
     */
    val defaultShellScriptTemplate: File get() = File(serverFilesDirectory, "default_template.sh")

    /**
     * The default fish-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `startScriptTemplates`.
     */
    val defaultFishScriptTemplate: File get() = File(serverFilesDirectory, "default_template.fish")


    /**
     * The default PowerShell-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `startScriptTemplates`.
     */
    val defaultPowerShellScriptTemplate: File get() = File(serverFilesDirectory, "default_template.ps1")

    /**
     * The default Batch-template for the modded server start scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `startScriptTemplates`.
     */
    val defaultBatchScriptTemplate: File get() = File(serverFilesDirectory, "default_template.bat")

    /**
     * The default shell-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `javaScriptTemplates`.
     */
    val defaultJavaShellScriptTemplate: File get() = File(serverFilesDirectory, "default_java_template.sh")

    /**
     * The default fish-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `javaScriptTemplates`.
     */
    val defaultJavaFishScriptTemplate: File get() = File(serverFilesDirectory, "default_java_template.fish")

    /**
     * The default PowerShell-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `javaScriptTemplates`.
     */
    val defaultJavaPowerShellScriptTemplate: File get() = File(serverFilesDirectory, "default_java_template.ps1")

    /**
     * The default Batch-template for the java-install scripts. The file returned by this
     * method does not represent the script-template in the `server_files`-directory. If you
     * wish access the configured script templates inside the `server_files`-directory, use
     * `javaScriptTemplates`.
     */
    val defaultJavaBatchScriptTemplate: File get() = File(serverFilesDirectory, "default_java_template.bat")

    /**
     * Directory in which the properties for quick selection are to be stored in and retrieved from.
     */
    var propertiesDirectory: File = File(serverFilesDirectory, "properties").absoluteFile
        get() {
            field = File(serverFilesDirectory, "properties").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which the icons for quick selection are to be stored in and retrieved from.
     */
    var iconsDirectory: File = File(serverFilesDirectory, "icons").absoluteFile
        get() {
            field = File(serverFilesDirectory, "icons").absoluteFile
            return field
        }
        private set

    /**
     * Default server.properties-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    var defaultServerProperties: File = File(serverFilesDirectory, "server.properties").absoluteFile
        get() {
            field = File(serverFilesDirectory, "server.properties").absoluteFile
            return field
        }
        private set

    /**
     * Default server-icon.png-file used by Minecraft servers. This file resides in the
     * `server_files`-directory inside ServerPackCreators home-directory.
     */
    var defaultServerIcon: File = File(serverFilesDirectory, "server-icon.png").absoluteFile
        get() {
            field = File(serverFilesDirectory, "server-icon.png").absoluteFile
            return field
        }
        private set

    /**
     * The `variables.txt` template shipped with SPC, in the `server_files`-directory inside its home-directory.
     *
     * Generation reads this file and substitutes its `SPC_..._SPC` placeholders per server pack, so the text an
     * operator sees — the comments explaining every setting — is editable without rebuilding the API. It sits
     * alongside the start-script templates because it is the same kind of thing: shipped content the user may adjust.
     */
    var defaultVariablesTemplate: File = File(serverFilesDirectory, "variables.txt").absoluteFile
        get() {
            field = File(serverFilesDirectory, "variables.txt").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which plugins for ServerPackCreator are to be placed in.
     *
     * This directory not only holds any potential plugins for ServerPackCreator, but also contains the
     * directory in which plugin-specific config-files are stored in, as well as the
     * `disabled.txt`-file, which allows a user to disable any installed plugin.
     *
     *
     * By default, this is the `plugins`-directory inside the ServerPackCreator home-directory.
     */
    var pluginsDirectory: File = File(homeDirectory, "plugins").absoluteFile
        get() {
            field = File(homeDirectory, "plugins").absoluteFile
            return field
        }
        private set

    /**
     * Directory in which plugin-specific configurations are stored in.
     *
     * When ServerPackCreator starts and loads all available plugins, it will also extract a plugins
     * config-file, if available. This file will be stored inside the config-directory using the ID of
     * the plugin as its name, with `.toml` appended to it. Think of this like the
     * config-directory in a modded Minecraft server. Do the names of the config-files there look
     * familiar to the mods they belong to? Well, they should!
     *
     * By default, this is the `config`-directory inside the `plugins`-directory inside
     * ServerPackCreators home-directory.
     */
    var pluginsConfigsDirectory: File = File(pluginsDirectory, "config").absoluteFile
        get() {
            field = File(pluginsDirectory, "config").absoluteFile
            return field
        }
        private set
}
