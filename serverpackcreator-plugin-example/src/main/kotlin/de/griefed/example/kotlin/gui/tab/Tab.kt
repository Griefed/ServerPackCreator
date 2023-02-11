/*
 * MIT License
 *
 * Copyright (c) 2022 Griefed
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
package de.griefed.example.kotlin.gui.tab

import Example
import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import org.pf4j.Extension
import java.io.File
import java.util.*
import javax.swing.Icon

/**
 * Extension which returns the Tab from {@link TetrisTab}.
 * @author Griefed
 */
@Suppress("unused")
@Extension
class Tab : TabExtension {
    /**
     * @param versionMeta   Instance of [VersionMeta] so you can work with available Minecraft,
     * Forge, Fabric, LegacyFabric and Quilt versions.
     * @param apiProperties Instance of [ApiProperties] The current configuration of
     * ServerPackCreator, like the default list of clientside-only mods, the
     * server pack directory etc.
     * @param utilities     Instance of [Utilities] commonly used across ServerPackCreator.
     * @param pluginConfig   Addon specific configuration conveniently provided by ServerPackCreator.
     * This is the global configuration of the addon which provides the
     * ConfigPanelExtension to ServerPackCreator.
     * @param configFile    The config-file corresponding to the ID of the addon, wrapped in an
     * Optional.
     * @return Component to add to the ServerPackCreator GUI as a tab.
     * @author Griefed
     */
    override fun getTab(
        versionMeta: VersionMeta, apiProperties: ApiProperties,
        utilities: Utilities, pluginConfig: Optional<CommentedConfig>, configFile: Optional<File>
    ): ExtensionTab {
        return TetrisTab(versionMeta, apiProperties, utilities, pluginConfig, configFile)
    }

    /**
     * Get the [Icon] for this tab to display to the ServerPackCreator GUI.
     *
     * @return Icon to be used by the added tab.
     * @author Griefed
     */
    override val icon: Icon?
        get() = null

    /**
     * Get the title of this tab to display in the ServerPackCreator GUI.
     *
     * @return The title of this addons tabbed pane.
     * @author Griefed
     */
    override val title: String
        get() = Example.example_tab_title.toString()

    /**
     * Get the tooltip for this tab to display in the ServerPackCreator GUI.
     *
     * @return The tooltip of this addons tabbed pane.
     * @author Griefed
     */
    override val tooltip: String
        get() = Example.example_tab_tooltip.toString()

    /**
     * Get the name of this addon.
     *
     * @return The name of this addon.
     * @author Griefed
     */
    override val name: String
        get() = Example.example_tab_name.toString()

    /**
     * Get the description of this addon.
     *
     * @return The description of this addon.
     * @author Griefed
     */
    override val description: String
        get() = Example.example_tab_description.toString()

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
     * Get the if of this extension. Used by ServerPackCreator to determine which configuration, if
     * any, to provide to any given extension being run.
     *
     * @return The ID of this extension.
     * @author Griefed
     */
    override val extensionId: String
        get() = "tabextensionexample"
}