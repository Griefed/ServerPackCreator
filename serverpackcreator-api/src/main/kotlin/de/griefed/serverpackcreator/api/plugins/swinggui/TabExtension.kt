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
     * @param versionMeta           Instance of [VersionMeta] so you can work with available Minecraft, Forge, Fabric,
     * LegacyFabric and Quilt versions.
     * @param apiProperties Instance of [ApiProperties] The current configuration of ServerPackCreator,
     * like the default list of clientside-only mods, the server pack directory etc.
     * @param utilities             Instance of [Utilities] commonly used across ServerPackCreator.
     * @param pluginConfig          Plugin specific configuration conveniently provided by ServerPackCreator. This is
     * the global configuration of the plugin which provides the ConfigPanelExtension to ServerPackCreator.
     * @param configFile            The config-file corresponding to the ID of the plugin, wrapped in an Optional.
     * @return Component to add to the ServerPackCreator GUI as a tab.
     * @author Griefed
     */
    fun getTab(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        pluginConfig: Optional<CommentedConfig>,
        configFile: Optional<File>
    ): ExtensionTab

    /**
     * Get the [Icon] for this tab to display to the ServerPackCreator GUI.
     *
     * @return Icon to be used by the added tab.
     * @author Griefed
     */
    val icon: Icon?

    /**
     * Get the title of this tab to display in the ServerPackCreator GUI.
     *
     * @return The title of this plugin's tabbed pane.
     * @author Griefed
     */
    val title: String

    /**
     * Get the tooltip for this tab to display in the ServerPackCreator GUI.
     *
     * @return The tooltip of this plugin's tabbed pane.
     * @author Griefed
     */
    val tooltip: String
}