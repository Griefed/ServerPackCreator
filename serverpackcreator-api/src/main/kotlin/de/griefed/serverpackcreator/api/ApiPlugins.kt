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

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.file.FileNotFoundAction
import com.electronwill.nightconfig.toml.TomlParser
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.CustomPluginFactory
import de.griefed.serverpackcreator.api.plugins.ExtensionException
import de.griefed.serverpackcreator.api.plugins.ExtensionInformation
import de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreZipExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.ConfigPanelExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.apache.logging.log4j.kotlin.logger
import org.pf4j.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import javax.swing.JTabbedPane

/**
 * Manager for ServerPackCreator plugins. In itself it doesn't do much. It gathers lists of all
 * available extensions for [TabExtension],[PreGenExtension],[PreZipExtension] and
 * [PostGenExtension] so they can then be run during server pack generation and during
 * initialization of the GUI.
 *
 * @param tomlParser    To read plugin-configurations, so they can be provided to extensions.
 * @param apiProperties ServerPackCreator settings to be provided to extensions.
 * @param versionMeta   Version meta to be provided to extensions.
 * @param utilities     Utilities to be provided to extensions.
 *
 * @author Griefed
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ApiPlugins(
    tomlParser: TomlParser,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities
) : JarPluginManager(apiProperties.pluginsDirectory.toPath()) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val pluginsLog = logger("PluginsLogger")
    private val pluginConfigs: HashMap<String, CommentedConfig> = HashMap<String, CommentedConfig>(100)
    private val pluginConfigFiles = HashMap<String, File>(100)
    private val toml = ".toml"
    private val configToml = "config$toml"

    init {
        loadPlugins()
        startPlugins()
        extractPluginConfigs(tomlParser)
        availableExtensions()
    }

    /**
     * Retrieve the config.toml of a plugin and store it in the `plugins/config`-directory,
     * using the ID of the plugin as the name for the extracted file. `plugin.toml`-files must be
     * stored in the root of a plugin JAR-file in order for ServerPackCreator to be reliably be able to
     * retrieve it.
     *
     * A given plugin does not have to provide a config.toml, as not every plugin
     * requires a global config. When no file is provided by the plugin, no file is extracted. This
     * also means that subsequent runs of any extension provided by the plugin do not receive a global,
     * plugin-specific, configuration.
     *
     * When a config-file has been successfully extracted, it is
     * added to a map with the plugins ID, which in turn will be accessed when extensions are run, to
     * retrieve said configuration and pass it to any extensions.
     *
     * @param tomlParser Toml parser to read the config into a [CommentedConfig], mapped to the
     * plugins ID.
     * @author Griefed
     */
    private fun extractPluginConfigs(tomlParser: TomlParser) {
        for (plugin in getPlugins()) {
            val pluginConfig: String = plugin.pluginId + toml
            val pluginConfigFile = File(
                apiProperties.pluginsConfigsDirectory,
                pluginConfig
            )
            if (!pluginConfigFile.exists()) {
                try {
                    ZipFile(plugin.pluginPath.toFile()).use {
                        it.extractFile(
                            configToml,
                            apiProperties.pluginsConfigsDirectory.toString(),
                            pluginConfig
                        )
                    }
                } catch (ex: Exception) {
                    log.error("Could not extract config.toml from ${plugin.pluginPath.toFile().name}. Does it contain a valid config.toml?")
                    log.debug("", ex)
                }
            }
            if (pluginConfigFile.isFile) {
                registerPluginConfig(tomlParser, plugin.pluginId, pluginConfigFile)
            }
        }
    }

    /**
     * List available extensions.
     *
     * @author Griefed
     */
    private fun availableExtensions() {
        for (plugin in getPlugins()) {
            val preGen = getAllExtensionsOfPlugin(plugin, PreGenExtension::class.java)
            val preZip = getAllExtensionsOfPlugin(plugin, PreZipExtension::class.java)
            val postGen = getAllExtensionsOfPlugin(plugin, PostGenExtension::class.java)
            val tab = getAllExtensionsOfPlugin(plugin, TabExtension::class.java)
            val panel = getAllExtensionsOfPlugin(plugin, ConfigPanelExtension::class.java)
            val check = getAllExtensionsOfPlugin(plugin, ConfigCheckExtension::class.java)
            if (preGen.isEmpty()
                && preZip.isEmpty()
                && postGen.isEmpty()
                && tab.isEmpty()
                && panel.isEmpty()
                && check.isEmpty()
            ) {
                log.info("No extensions installed.")
                continue
            }
            log.info("Extension(s) for plugin ${plugin.pluginId}:")
            extensionsInfo(preGen)
            extensionsInfo(preZip)
            extensionsInfo(postGen)
            extensionsInfo(tab)
            extensionsInfo(panel)
            extensionsInfo(check)
        }
    }

    /**
     * List the extension name, id, description, version and author of a given extension.
     * @author Griefed
     */
    private fun extensionsInfo(extensions: List<ExtensionInformation>) {
        if (extensions.isNotEmpty()) {
            for (extension in extensions) {
                log.info("  Name:       ${extension.name}(${extension.extensionId})")
                log.info("    Description: ${extension.description}")
                log.info("    Version:     ${extension.version}")
                log.info("    Author:      ${extension.author}")
            }
        }
    }

    /**
     * Parse and register a config.toml of a plugin mapped to the plugins ID.
     *
     * @param tomlParser  Toml parser to parse the config into a [CommentedConfig].
     * @param pluginId    The plugins ID to map the config to.
     * @param pluginConfig The global configuration file corresponding to the plugins ID.
     * @author Griefed
     */
    private fun registerPluginConfig(tomlParser: TomlParser, pluginId: String, pluginConfig: File) {
        try {
            pluginConfigs[pluginId] = tomlParser.parse(
                pluginConfig, FileNotFoundAction.THROW_ERROR,
                StandardCharsets.UTF_8
            )
            pluginConfigFiles[pluginId] = pluginConfig
        } catch (ex: Exception) {
            log.error(
                "Could not parse plugin config for $pluginId, file ${pluginConfig.name}",
                ex
            )
        }
    }

    /**
     * Get all extension of the specified [type] for the specified [plugin].
     * @author Griefed
     */
    fun <T> getAllExtensionsOfPlugin(plugin: PluginWrapper, type: Class<T>): List<T> =
        plugin.pluginManager.getExtensions(type)

    override fun createExtensionFactory(): ExtensionFactory =
        SingletonExtensionFactory(
            this,
            ConfigCheckExtension::class.java.name,
            PostGenExtension::class.java.name,
            PreGenExtension::class.java.name,
            PreZipExtension::class.java.name,
            ConfigPanelExtension::class.java.name,
            TabExtension::class.java.name
        )

    override fun createPluginFactory(): PluginFactory {
        return CustomPluginFactory()
    }

    /**
     * Get the global plugin configuration for an plugin of the passed ID. The configuration is wrapped
     * in an [Optional], because an plugin may not provide a global configuration. If you intend
     * on using a global configuration for your plugin, make sure to check whether it is present before
     * trying to use it!
     *
     * @param pluginId The plugin ID of the...well...plugin.
     * @return The global plugin configuration, wrapped in an Optional.
     * @author Griefed
     */
    fun getPluginConfig(pluginId: String) = Optional.ofNullable<CommentedConfig>(pluginConfigs[pluginId])

    /**
     * Get and return any configuration for the extension about to be run. If none is available, the
     * returned list is empty. In order for a given extension to provide a configuration, the list of
     * available configurations for the encompassing plugin is scanned for
     * `extension=extensionID` pairs. If any `extension` matches the ID of the extension
     * being run, the configuration is added to the list and provided to the extension by
     * ServerPackCreator.
     *
     * @param plugin             The plugin which contains the extension.
     * @param packConfig The configuration model with which the server pack is, or will be,
     * generated.
     * @param extensionId        The ID of the extension about to be run.
     * @return A list of configurations for the specified extension of the specified plugin. May be
     * empty, if no configuration is available.
     * @author Griefed
     */
    private fun getExtensionSpecificConfigs(
        plugin: PluginWrapper,
        packConfig: PackConfig,
        extensionId: String
    ): ArrayList<CommentedConfig> {
        val extConf: ArrayList<CommentedConfig> = ArrayList<CommentedConfig>(10)
        val pluginConfigs = packConfig.getPluginConfigs(plugin.pluginId)
        if (pluginConfigs.isNotEmpty()) {
            val extensionConfigs = getExtensionConfigs(plugin, packConfig)
            for (config in extensionConfigs) {
                val id = config.get<Any>("extension")
                if (id == extensionId) {
                    extConf.add(config)
                }
            }
        }
        return extConf
    }

    /**
     * Get all available extension configurations from the passed ConfigurationModel for the specified
     * plugin.
     *
     * @param plugin             The plugin for which to acquire the list of extension-configurations.
     * @param packConfig The configuration model which holds the extension configurations.
     * @return A list of available extension-configurations, if any.
     * @author Griefed
     */
    private fun getExtensionConfigs(
        plugin: PluginWrapper,
        packConfig: PackConfig
    ): ArrayList<CommentedConfig> {
        val configs: ArrayList<CommentedConfig> = ArrayList<CommentedConfig>(10)
        val pluginConfigs = packConfig.getPluginConfigs(plugin.pluginId)
        if (pluginConfigs.isNotEmpty()) {
            configs.addAll(pluginConfigs)
        }
        return configs
    }

    /**
     * Run any and all Pre-Server Pack-Generation extensions, using the passed configuration model and
     * the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPreGenExtensions(packConfig: PackConfig, destination: String) {
        for (plugin in getPlugins()) {
            log.info("Executing PreGenExtension extensions.")
            pluginsLog.info("Executing PreGenExtension extensions.")
            for (extension in getAllExtensionsOfPlugin(plugin, PreGenExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing PreGenExtension ${extension.name}")
                try {
                    val pluginConfig = getPluginConfig(plugin.pluginId)
                    val extensionConfigs = getExtensionSpecificConfigs(plugin, packConfig, extension.extensionId)
                    extension.run(
                        versionMeta,
                        utilities,
                        apiProperties,
                        packConfig,
                        destination,
                        pluginConfig,
                        extensionConfigs
                    )
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                }
            }
        }
    }

    /**
     * Run any and all Pre-ZIP-archive creation extensions, using the passed configuration model and
     * the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPreZipExtensions(packConfig: PackConfig, destination: String) {
        for (plugin in getPlugins()) {
            log.info("Executing PreZipExtension extensions.")
            pluginsLog.info("Executing PreZipExtension extensions.")
            for (extension in getAllExtensionsOfPlugin(plugin, PreZipExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing PreZipExtension ${extension.name}")
                val pluginConfig = getPluginConfig(plugin.pluginId)
                val extensionConfigs = getExtensionSpecificConfigs(plugin, packConfig, extension.extensionId)
                try {
                    extension.run(
                        versionMeta,
                        utilities,
                        apiProperties,
                        packConfig,
                        destination,
                        pluginConfig,
                        extensionConfigs
                    )
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                }
            }
        }
    }

    /**
     * Run any and all Post-server pack-generation extensions, using the passed configuration model
     * and the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPostGenExtensions(packConfig: PackConfig, destination: String) {
        for (plugin in getPlugins()) {
            log.info("Executing PostGenExtension extensions.")
            pluginsLog.info("Executing PostGenExtension extensions.")
            for (extension in getAllExtensionsOfPlugin(plugin, PostGenExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing PostGenExtension ${extension.name}")
                val pluginConfig = getPluginConfig(plugin.pluginId)
                val extensionConfigs = getExtensionSpecificConfigs(plugin, packConfig, extension.extensionId)
                try {
                    extension.run(
                        versionMeta,
                        utilities,
                        apiProperties,
                        packConfig,
                        destination,
                        pluginConfig,
                        extensionConfigs
                    )
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                }
            }
        }
    }

    /**
     * Add any and all additional tabs to the ServerPackCreator tabbed pane (main GUI). You may use
     * this to add tabs to your own [JTabbedPane], if you so desire. Could be pretty awesome to
     * have your plugins extra tabs in a separate window!
     *
     * @param tabbedPane The tabbed pane to which the additional panels should be added to as tabs.
     * @author Griefed
     */
    fun addTabExtensionTabs(tabbedPane: JTabbedPane) {
        for (plugin in getPlugins()) {
            log.info("Executing TabExtensions extensions.")
            pluginsLog.info("Executing TabExtensions extensions.")
            for (extension in getAllExtensionsOfPlugin(plugin, TabExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing TabExtension ${extension.name}")
                val pluginConfig = getPluginConfig(plugin.pluginId)
                val pluginConfigFile = getPluginConfigFile(plugin.pluginId)
                val tab = extension.getTab(versionMeta, apiProperties, utilities, pluginConfig, pluginConfigFile)
                try {
                    tabbedPane.addTab(extension.title, extension.icon, tab, extension.tooltip)
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                }
            }
        }
    }

    /**
     * Create config panels for the passed server pack configuration tab. Note that this method does
     * **NOT** add the panels to the tab, it only creates them and passes the server
     * pack config tab object-reference to each config panel, so they, in turn, may use any available
     * fields and methods for their own operations. A given server pack config tab needs to add the
     * panels which are returned by this method, so a user may make their configurations accordingly.
     *
     * @param serverPackConfigTab The server pack configuration tab to which the config panels are to
     * be added.
     * @return A list of config panels specifically created for the passed server pack
     * configuration-tab.
     * @author Griefed
     */
    fun getConfigPanels(serverPackConfigTab: ServerPackConfigTab): List<ExtensionConfigPanel> {
        val panels: MutableList<ExtensionConfigPanel> = ArrayList<ExtensionConfigPanel>(10)
        for (plugin in getPlugins()) {
            log.info("Getting ConfigPanelExtension from ${plugin.pluginId}.")
            pluginsLog.info("Getting ConfigPanelExtension from ${plugin.pluginId}.")
            for (extension in getAllExtensionsOfPlugin(plugin, ConfigPanelExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing ConfigPanelExtension ${extension.name}")
                try {
                    val pluginConfig = getPluginConfig(plugin.pluginId)
                    val panel = extension.getPanel(
                        versionMeta,
                        apiProperties,
                        utilities,
                        serverPackConfigTab,
                        pluginConfig,
                        extension.name,
                        plugin.pluginId
                    )
                    panels.add(panel)
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                }
            }
        }
        return panels
    }

    /**
     * Run any and all configuration-check extensions, using the passed configuration model and the
     * destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model containing the server pack and plugin
     * configurations to check.
     * @param configCheck Collection of encountered errors, if any, for convenient result-checks.
     * @return `true` if any custom check detected an error with the configuration.
     * **Only** return `false` when not a **single** check
     * errored.
     * @author Griefed
     */
    fun runConfigCheckExtensions(
        packConfig: PackConfig,
        configCheck: ConfigCheck = ConfigCheck()
    ): ConfigCheck {
        for (plugin in getPlugins()) {
            log.info("Executing ConfigCheckExtensions extensions.")
            pluginsLog.info("Executing ConfigCheckExtensions extensions.")
            for (extension in getAllExtensionsOfPlugin(plugin, ConfigCheckExtension::class.java)) {
                val extensionError = "Extension ${extension.name} in plugin ${plugin.pluginId} encountered an error."
                pluginsLog.info("Executing ConfigCheckExtension ${extension.name}")
                try {
                    val pluginConfig = getPluginConfig(plugin.pluginId)
                    val extensionConfigs = getExtensionConfigs(plugin, packConfig)
                    extension.runCheck(
                        versionMeta,
                        apiProperties,
                        utilities,
                        packConfig,
                        configCheck,
                        pluginConfig,
                        extensionConfigs
                    )
                } catch (ex: ExtensionException) {
                    pluginsLog.error(extensionError, ex)
                    configCheck.pluginsErrors.add(extensionError)
                } catch (ex: Error) {
                    pluginsLog.error(extensionError, ex)
                    configCheck.pluginsErrors.add(extensionError)
                } catch (ex: Exception) {
                    pluginsLog.error(extensionError, ex)
                    configCheck.pluginsErrors.add(extensionError)
                }
            }
        }
        return configCheck
    }

    /**
     * Get the configuration-file for a plugin, if it exists. This is wrapped in an [Optional],
     * because not every plugin may provide a configuration-file to use globally for the relevant
     * plugins settings. If you intend on using a global configuration, make sure to check whether the
     * file is present, before moving on!
     *
     * @param pluginId The plugin ID with which to identify the correct config-file to return.
     * @return The config-file corresponding to the ID of the plugin, wrapped in an Optional.
     * @author Griefed
     */
    fun getPluginConfigFile(pluginId: String) = Optional.ofNullable(pluginConfigFiles[pluginId])
}