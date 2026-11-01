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
     * The component ServerPackCreator adds to its GUI as a tab.
     *
     * Every parameter is handed over by ServerPackCreator: [versionMeta] for the available Minecraft,
     * Forge, Fabric, LegacyFabric and Quilt versions; [apiProperties] for the current configuration,
     * such as the default clientside-only mods and the server pack directory; [utilities] for the
     * helpers used across ServerPackCreator; [pluginConfig] for your plugin's own global
     * configuration; and [configFile] for the config-file corresponding to your plugin's id.
     *
     * @author Griefed
     */
    override fun getTab(
        versionMeta: VersionMeta, apiProperties: ApiProperties,
        utilities: Utilities, pluginConfig: Optional<CommentedConfig>, configFile: Optional<File>
    ): ExtensionTab {
        return TetrisTab(versionMeta, apiProperties, utilities, pluginConfig, configFile)
    }

    /**
     * The [Icon] this tab displays in the ServerPackCreator GUI.
     *
     * @author Griefed
     */
    override val icon: Icon?
        get() = null

    /**
     * The title this tab displays in the ServerPackCreator GUI.
     *
     * @author Griefed
     */
    override val title: String
        get() = Example.example_tab_title.toString()

    /**
     * The tooltip this tab displays in the ServerPackCreator GUI.
     *
     * @author Griefed
     */
    override val tooltip: String
        get() = Example.example_tab_tooltip.toString()

    /** Shown in the GUI's plugin list. @author Griefed */
    override val name: String
        get() = Example.example_tab_name.toString()

    /** Shown beside the name. @author Griefed */
    override val description: String
        get() = Example.example_tab_description.toString()

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
        get() = "tabextensionexample"
}