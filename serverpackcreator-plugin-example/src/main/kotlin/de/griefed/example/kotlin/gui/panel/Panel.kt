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
     * serverpackcreator.conf, and could be used in any of the [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler]
     * extension-points, which would then download the version you specified via this here panel.
     *
     * Every parameter is handed over by ServerPackCreator: [versionMeta] for the available Minecraft,
     * Forge, Fabric, LegacyFabric and Quilt versions; [apiProperties] for the current configuration,
     * such as the default clientside-only mods and the server pack directory; [utilities] for the
     * helpers used across ServerPackCreator; [serverPackConfigTab] for the fields of the configuration
     * tab, like the modpack directory and the selected Minecraft and modloader versions;
     * [pluginConfig] for your plugin's own global configuration; [extensionName] for this panel's
     * titled border; and [pluginID], which is your plugin's id.
     *
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

    /** Shown in the GUI's plugin list. @author Griefed */
    override val name: String
        get() = Example.example_panel_name.toString()

    /** Shown beside the name. @author Griefed */
    override val description: String
        get() = Example.example_panel_description.toString()

    /** Credited in the GUI. @author Griefed */
    override val author: String
        get() = "Griefed"

    /** Displayed as-is; SPC compares nothing against it. @author Griefed */
    override val version: String
        get() = "0.0.1-SNAPSHOT"

    /**
     * Get the ID of this extension. Used by ServerPackCreator to determine which configuration, if
     * any, to provide to any given extension being run.
     *
     * @author Griefed
     */
    override val extensionId: String
        get() = "configpanelexample"
}