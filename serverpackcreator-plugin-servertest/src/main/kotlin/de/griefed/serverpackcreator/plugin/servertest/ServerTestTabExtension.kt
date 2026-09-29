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
package de.griefed.serverpackcreator.plugin.servertest

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionTab
import de.griefed.serverpackcreator.api.plugins.swinggui.TabExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.servertest.gui.ServerTestTab
import org.pf4j.Extension
import java.io.File
import java.util.Optional
import javax.swing.Icon

/**
 * Registers the plugin's tab with ServerPackCreator's window.
 *
 * A `TabExtension` contributes exactly one tab, so the pack list and every running server's console live
 * inside [ServerTestTab] as a nested tabbed pane rather than as separate extensions.
 *
 * The constants below are what ServerPackCreator shows the user and writes to `plugins.log`; none of them
 * is read by the feature itself. [extensionId] is the exception — SPC keys per-extension configuration on
 * it, so it is an identity rather than a label.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class ServerTestTabExtension : TabExtension {

    /**
     * Build the one tab this extension contributes, handing [ServerTestTab] the collaborators SPC supplies.
     *
     * Called once per installed plugin when the GUI is assembled. **This call is outside the try-block in
     * `ApiPlugins.addTabExtensionTabs`**, so anything thrown here escapes into GUI assembly rather than being
     * logged like the rest — which is why [ServerTestTab]'s constructor reports its problems in the pane
     * instead of throwing them.
     */
    override fun getTab(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        pluginConfig: Optional<CommentedConfig>,
        configFile: Optional<File>
    ): ExtensionTab = ServerTestTab(versionMeta, apiProperties, utilities, pluginConfig, configFile)

    /** No icon: ServerPackCreator renders the title alone, which is what the tab is identified by anyway. */
    override val icon: Icon? = null

    /** The tab's label in SPC's window, and — with no [icon] — the only thing it is identified by. */
    override val title = "Server Test"
    /** Hover text on that label, saying what the tab is for before the user opens it. */
    override val tooltip = "Launch a generated server pack and drive its console."
    /** This extension's name as SPC lists it — distinct from the *plugin* name, since one plugin may
     *  provide several extensions. */
    override val name = "Server pack test launcher"
    /** One line explaining the extension wherever SPC lists it. */
    override val description =
        "Start any generated server pack through its own start scripts and interact with the running server."
    /** Who to blame in `plugins.log` when this extension misbehaves. */
    override val author = "Griefed"
    /** This extension's own version, deliberately independent of the jar's — the jar carries the project
     *  version, this one changes when the tab's contract does. */
    override val version = "1.0.0"
    /** The key SPC stores this extension's configuration under. **Stable across releases** — changing it
     *  makes SPC look for a configuration that no longer exists. */
    override val extensionId = "servertest-launcher-tab"
}
