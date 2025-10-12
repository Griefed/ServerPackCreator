/*
 * MIT License
 *
 * Copyright (C) 2024 Griefed
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package de.griefed.example.kotlin.gui.panel

import Example
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.toml.TomlFormat
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.*
import java.util.function.Consumer
import javax.swing.JLabel
import javax.swing.JSeparator
import javax.swing.JTextField

/**
 * Custom configuration panel for configuration of custom values. Such custom values could be used during configuration
 * checks or server pack creations.
 * @param versionMeta         Instance of {@link VersionMeta} so you can work with available
 *                            Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
 * @param apiProperties       Instance of {@link ApiProperties} The current configuration of
 *                            ServerPackCreator, like the default list of clientside-only mods,
 *                            the server pack directory etc.
 * @param utilities           Instance of {@link Utilities} commonly used across
 *                            ServerPackCreator.
 * @param serverPackConfigTab Instance of {@link ServerPackConfigTab} to give you access to the
 *                            various fields inside it, like the modpack directory, selected
 *                            Minecraft, modloader and modloader versions, etc.
 * @param addonConfig         Addon specific configuration conveniently provided by
 *                            ServerPackCreator. This is the global configuration of the addon
 *                            which provides the ConfigPanelExtension to ServerPackCreator.
 * @param extensionName       The name the titled border of this ConfigPanel will get.
 * @param pluginID            The ID of the addon providing this extension implementation. The
 *                            pluginID determines which extension specific configurations are
 *                            provided to this panel, and how they are stored in a given
 *                            serverpackcreator.conf.
 * @author Griefed
 */
class ConfigurationPanel(
    versionMeta: VersionMeta,
    apiProperties: ApiProperties,
    utilities: Utilities,
    serverPackConfigTab: ServerPackConfigTab,
    addonConfig: Optional<CommentedConfig>?,
    extensionName: String, pluginID: String
) : ExtensionConfigPanel(
    versionMeta, apiProperties, utilities, serverPackConfigTab, addonConfig!!,
    extensionName, pluginID
) {
    private val panelName: String = "$extensionName ($pluginID)"
    private val pregen = JTextField()
    private val prezip = JTextField()
    private val postgen = JTextField()
    private val confcheck = JTextField()

    init {
        val extensionHeader = Font("Noto Sans Display Regular", Font.BOLD, 18)
        layout = GridBagLayout()
        val gridBagConstraints = GridBagConstraints()
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL
        gridBagConstraints.anchor = GridBagConstraints.WEST
        gridBagConstraints.insets = Insets(5, 5, 5, 5)
        gridBagConstraints.gridwidth = 1
        gridBagConstraints.gridheight = 1
        gridBagConstraints.weightx = 1.0
        gridBagConstraints.weighty = 1.0
        val pregenexample = JLabel(Example.example_panel_pregen.toString())
        pregenexample.font = extensionHeader
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy = 0
        add(pregenexample, gridBagConstraints)
        gridBagConstraints.gridy += 1
        val pregenexampletext = JLabel(Example.example_panel_pregen_text.toString())
        add(pregenexampletext, gridBagConstraints)
        gridBagConstraints.gridy += 1
        add(pregen, gridBagConstraints)
        add(JSeparator())
        val prezipexample = JLabel(Example.example_panel_prezip.toString())
        prezipexample.font = extensionHeader
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy += 1
        add(prezipexample, gridBagConstraints)
        gridBagConstraints.gridy += 1
        val prezipexampletext = JLabel(Example.example_panel_prezip_text.toString())
        add(prezipexampletext, gridBagConstraints)
        gridBagConstraints.gridy += 1
        add(prezip, gridBagConstraints)
        add(JSeparator())
        val postgenexample = JLabel(Example.example_panel_postgen.toString())
        postgenexample.font = extensionHeader
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy += 1
        add(postgenexample, gridBagConstraints)
        gridBagConstraints.gridy += 1
        val postgenexampletext = JLabel(Example.example_panel_postgen_text.toString())
        add(postgenexampletext, gridBagConstraints)
        gridBagConstraints.gridy += 1
        add(postgen, gridBagConstraints)
        add(JSeparator())
        val configcheckexample = JLabel(Example.example_panel_configcheck.toString())
        configcheckexample.font = extensionHeader
        gridBagConstraints.gridx = 0
        gridBagConstraints.gridy += 1
        add(configcheckexample, gridBagConstraints)
        gridBagConstraints.gridy += 1
        val configcheckexampletext = JLabel(Example.example_panel_configcheck_text.toString())
        add(configcheckexampletext, gridBagConstraints)
        gridBagConstraints.gridy += 1
        add(confcheck, gridBagConstraints)
    }

    /**
     * Retrieve this extensions server pack specific configuration. When no configuration with configs
     * for this extension has been loaded yet, the returned list is empty. Fill it with life!
     *
     * @return Config list to be used in subsequent server pack generation runs, by various other
     * extensions.
     * @author Griefed
     */
    override fun serverPackExtensionConfig(): ArrayList<CommentedConfig> {
        pluginsLog.info("Retrieving $panelName configuration.")
        val comment =
            " The ID of the extension.\n This field is used to identify the configuration to use when\n running the extension during config checks or server pack generation."
        val pregenexample = TomlFormat.newConfig()
        pregenexample.setComment("extension", comment)
        pregenexample.set<Any>("extension", "pregenexample")
        pregenexample.set<Any>("text", pregen.text)
        val prezipexample = TomlFormat.newConfig()
        prezipexample.setComment("extension", comment)
        prezipexample.set<Any>("extension", "prezipexample")
        prezipexample.set<Any>("text", prezip.text)
        val postgenexample = TomlFormat.newConfig()
        postgenexample.setComment("extension", comment)
        postgenexample.set<Any>("extension", "postgenexample")
        postgenexample.set<Any>("text", postgen.text)
        val configcheckexample = TomlFormat.newConfig()
        configcheckexample.setComment("extension", comment)
        configcheckexample.set<Any>("extension", "configcheckexample")
        configcheckexample.set<Any>("text", confcheck.text)
        val extensionConfig = serverPackExtensionConfig
        extensionConfig.clear()
        extensionConfig.add(pregenexample)
        extensionConfig.add(prezipexample)
        extensionConfig.add(postgenexample)
        extensionConfig.add(configcheckexample)
        return extensionConfig
    }

    /**
     * Pass the extension configuration to the configuration panel, so it can then, in turn, load the
     * available configurations and make them editable, if so desired.
     *
     * @param serverPackExtensionConfig The list of extension configurations to pass to the
     * configuration panel.
     * @author Griefed
     */
    override fun setServerPackExtensionConfig(
        serverPackExtensionConfig: ArrayList<CommentedConfig>
    ) {
        pluginsLog.info("Setting $panelName configuration.")
        serverPackExtensionConfig.forEach(
            Consumer { config: CommentedConfig ->
                if (config.getOptional<Any>("extension").isPresent) {
                    when (config.getOptional<Any>("extension").get().toString()) {
                        "pregenexample" -> pregen.text = config.get<Any>("text").toString()
                        "prezipexample" -> prezip.text = config.get<Any>("text").toString()
                        "postgenexample" -> postgen.text = config.get<Any>("text").toString()
                        "configcheckexample" -> confcheck.text = config.get<Any>("text").toString()
                    }
                }
            }
        )
    }

    /**
     * Clear the interface, or in other words, reset this extensions config panel UI. If your Config
     * Panel Extensions has no elements you wish to reset, then simply overwrite this method with an
     * empty method body.<br></br><br></br> The `clear()`-method is called when the owning
     * `TabCreateServerPack.clearInterface()`-method is called.
     */
    override fun clear() {
        pluginsLog.info("Clearing $panelName interface.")
        pregen.text = ""
        prezip.text = ""
        postgen.text = ""
        confcheck.text = ""
    }
}