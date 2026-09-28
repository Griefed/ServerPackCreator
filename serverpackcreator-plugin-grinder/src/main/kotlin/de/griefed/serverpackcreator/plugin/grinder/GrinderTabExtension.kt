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
 * The constants below are what ServerPackCreator shows the user and writes to `plugins.log`; none of
 * them is read by the feature itself. [extensionId] is the exception — SPC keys per-extension
 * configuration on it, so it is an identity rather than a label.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class GrinderTabExtension : TabExtension {

    /**
     * Build the one tab this extension contributes, handing [GrinderTab] the collaborators SPC supplies.
     *
     * Called once per installed plugin when the GUI is assembled. The `pluginConfig` instance matters:
     * it is the *same* object [GrinderPreGenExtension] receives, which is how a tick made here reaches a
     * generation without a save or a restart.
     */
    override fun getTab(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        pluginConfig: Optional<CommentedConfig>,
        configFile: Optional<File>
    ): ExtensionTab = GrinderTab(versionMeta, apiProperties, utilities, pluginConfig, configFile)

    /** No icon: ServerPackCreator renders the title alone, which is what the tab is identified by anyway. */
    override val icon: Icon? = null

    /** The tab's label in SPC's window, and — with no [icon] — the only thing it is identified by. */
    override val title = "Grinder"
    /** Hover text on that label, saying what the tab is for before the user opens it. */
    override val tooltip = "Pick clientside-only mods from a grinder's verdicts."
    /** This extension's name as SPC lists it — distinct from the *plugin* name (`Grinder`), since one
     *  plugin may provide several extensions. */
    override val name = "Grinder verdict browser"
    /** One line explaining the extension wherever SPC lists it. */
    override val description =
        "Browse a grinder's verdicts, tick the mods to exclude, and watch what the daemon is doing."
    /** Who to blame in `plugins.log` when this extension misbehaves. */
    override val author = "Griefed"
    /** This extension's own version, deliberately independent of the jar's — the jar carries the
     *  project version, this one changes when the tab's contract does. */
    override val version = "1.0.0"
    /** The key SPC stores this extension's configuration under. **Stable across releases** — changing
     *  it makes SPC look for a configuration that no longer exists. */
    override val extensionId = "grinder-verdict-tab"
}
