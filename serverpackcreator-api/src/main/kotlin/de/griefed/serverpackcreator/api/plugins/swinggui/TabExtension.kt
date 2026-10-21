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
package de.griefed.serverpackcreator.api.plugins.swinggui

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.ExtensionInformation
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.io.File
import java.util.*
import javax.swing.Icon

/**
 * Extension point for plugins which add additional [javax.swing.JPanel]s as additional tabs to the
 * ServerPackCreator GUI.
 *
 * @author Griefed
 */
@Suppress("unused")
interface TabExtension : ExtensionInformation {
    /**
     * Builds the tab this plugin contributes to the GUI, once, when the main window is assembled.
     *
     * Called by `ApiPlugins.addTabExtensionTabs` at GUI startup — not per generation — so the returned
     * [ExtensionTab] is long-lived and owns its own state. A throw here is caught and logged against the
     * plugin rather than propagated, so a failing tab costs its own tab and not the window. [icon] and
     * [title] name it in the tab bar.
     *
     * Returns the component to add to the ServerPackCreator GUI as a tab.
     *
     * @author Griefed
     */
    fun getTab(
        /**
         * Instance of [VersionMeta] so you can work with available Minecraft, Forge, Fabric, LegacyFabric and
         * Quilt versions.
         */
        versionMeta: VersionMeta,
        /**
         * Instance of [ApiProperties] The current configuration of ServerPackCreator, like the default list of
         * clientside-only mods, the server pack directory etc.
         */
        apiProperties: ApiProperties,
        /** Instance of [Utilities] commonly used across ServerPackCreator. */
        utilities: Utilities,
        /**
         * Plugin specific configuration conveniently provided by ServerPackCreator. This is the global
         * configuration of the plugin which provides the ConfigPanelExtension to ServerPackCreator.
         */
        pluginConfig: Optional<CommentedConfig>,
        /** The config-file corresponding to the ID of the plugin, wrapped in an Optional. */
        configFile: Optional<File>
    ): ExtensionTab

    /**
     * Get the [Icon] for this tab to display to the ServerPackCreator GUI.
     *
     * Returns the icon to be used by the added tab.
     *
     * @author Griefed
     */
    val icon: Icon?

    /**
     * Get the title of this tab to display in the ServerPackCreator GUI.
     *
     * Returns the title of this plugin's tabbed pane.
     *
     * @author Griefed
     */
    val title: String

    /**
     * Get the tooltip for this tab to display in the ServerPackCreator GUI.
     *
     * Returns the tooltip of this plugin's tabbed pane.
     *
     * @author Griefed
     */
    val tooltip: String
}