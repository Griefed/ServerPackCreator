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
package de.griefed.serverpackcreator.plugin.grinder

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.grinder.gui.GrinderTab
import org.pf4j.Extension
import java.io.File
import java.util.Optional
import javax.swing.Icon

/**
 * Registers the plugin's tab with ServerPackCreator's window.
 *
 * A `TabExtension` contributes exactly one tab, so the four panes the feature is made of live inside
 * [GrinderTab] as a nested tabbed pane rather than as four extensions.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class GrinderTabExtension : TabExtension {

    override fun getTab(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        pluginConfig: Optional<CommentedConfig>,
        configFile: Optional<File>
    ): ExtensionTab = GrinderTab(versionMeta, apiProperties, utilities, pluginConfig, configFile)

    /** No icon: ServerPackCreator renders the title alone, which is what the tab is identified by anyway. */
    override val icon: Icon? = null

    override val title = "Grinder"
    override val tooltip = "Pick clientside-only mods from a grinder's verdicts."
    override val name = "Grinder verdict browser"
    override val description =
        "Browse a grinder's verdicts, tick the mods to exclude, and watch what the daemon is doing."
    override val author = "Griefed"
    override val version = "1.0.0"
    override val extensionId = "grinder-verdict-tab"
}
