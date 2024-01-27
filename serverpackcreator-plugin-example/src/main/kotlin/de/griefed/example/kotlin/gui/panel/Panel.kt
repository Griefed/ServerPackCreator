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
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ConfigPanelExtension
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.api.plugins.swinggui.ServerPackConfigTab
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.pf4j.Extension
import java.util.*

/**
 * Extension which returns the ConfigPanel from {@link ConfigurationPanel}.
 * @author Griefed
 */
@Suppress("unused")
@Extension
class Panel : ConfigPanelExtension {
    /**
     * This method gets called when an extension of this type is run. All parameters are provided by
     * ServerPackCreator, so you do not have to take care of them. A ConfigPanel is intended to be
     * used to change server pack-specific configurations which can then be used by other extensions
     * in your plugin. A simple example would be downloading a specific version of some mod. You could
     * add a panel which lets the user configure the version of the mod to use. When the user then
     * runs the server pack generation, your setting will be stored in the subsequently generated
     * serverpackcreator.conf, and could be used in any of the [de.griefed.serverpackcreator.api.ServerPackHandler]
     * extension-points, which would then download the version you specified via this here panel.
     *
     * @param versionMeta         Instance of [VersionMeta] so you can work with available
     * Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
     * @param apiProperties       Instance of [ApiProperties] The current configuration of
     * ServerPackCreator, like the default list of clientside-only mods,
     * the server pack directory etc.
     * @param utilities           Instance of [Utilities] commonly used across
     * ServerPackCreator.
     * @param serverPackConfigTab Instance of [ServerPackConfigTab] to give you access to the
     * various fields inside it, like the modpack directory, selected
     * Minecraft, modloader and modloader versions, etc.
     * @param pluginConfig         Addon specific configuration conveniently provided by
     * ServerPackCreator. This is the global configuration of the addon
     * which provides the ConfigPanelExtension to ServerPackCreator.
     * @param extensionName       The name the titled border of this ConfigPanel will get.
     * @param pluginID            The same as the PluginId.
     * @return A ConfigPanel allowing users to further customize their ServerPackCreator experience
     * when using an addon.
     * @author Griefed
     */
    override fun getPanel(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties, utilities: Utilities,
        serverPackConfigTab: ServerPackConfigTab, pluginConfig: Optional<CommentedConfig>,
        extensionName: String, pluginID: String
    ): ExtensionConfigPanel {
        return ConfigurationPanel(
            versionMeta, apiProperties, utilities,
            serverPackConfigTab, pluginConfig, extensionName, pluginID
        )
    }

    /**
     * Get the name of this addon.
     *
     * @return The name of this addon.
     * @author Griefed
     */
    override val name: String
        get() = Example.example_panel_name.toString()

    /**
     * Get the description of this addon.
     *
     * @return The description of this addon.
     * @author Griefed
     */
    override val description: String
        get() = Example.example_panel_description.toString()

    /**
     * Get the author of this addon.
     *
     * @return The author of this addon.
     * @author Griefed
     */
    override val author: String
        get() = "Griefed"

    /**
     * Get the version of this addon.
     *
     * @return The version of this addon.
     * @author Griefed
     */
    override val version: String
        get() = "0.0.1-SNAPSHOT"

    /**
     * Get the ID of this extension. Used by ServerPackCreator to determine which configuration, if
     * any, to provide to any given extension being run.
     *
     * @return The ID of this extension.
     * @author Griefed
     */
    override val extensionId: String
        get() = "configpanelexample"
}