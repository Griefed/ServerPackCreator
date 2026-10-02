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
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PostGenExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.servertest.core.GenerationNotifier
import org.pf4j.Extension
import java.io.File
import java.util.Optional

/**
 * Tells the plugin's tab when ServerPackCreator has finished generating a server pack, so its list can
 * refresh itself instead of waiting to be asked.
 *
 * A `PostGenExtension` rather than an `SPCPostGenListener`, deliberately. Both fire from the same place —
 * adjacent lines at the end of `ServerPackHandler.run`, the choke point every GUI, CLI and web generation
 * passes through — but a listener has to be registered by reaching back through `ApiWrapper.api()` from
 * plugin `init`, which is the call the API's own notes record as having caused two unbounded recursions,
 * and `ServerPackHandler` offers no way to *remove* one afterwards. An extension is owned by pf4j, needs no
 * reach-back, and `ApiPlugins.runPostGenExtensions` already wraps every call so a throw here cannot abort
 * somebody's generation.
 *
 * Carries no state: the generated pack's path goes straight to [GenerationNotifier], which is the only
 * thing the tab and this class share.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class ServerTestPostGenExtension : PostGenExtension {

    /**
     * Publish the freshly generated pack at [destination].
     *
     * Every other parameter is part of the extension point's contract and unused here — what the tab needs
     * is the directory, which it re-reads for itself so the row it shows comes from the same
     * `manifest.json` every other row does.
     */
    override fun run(
        versionMeta: VersionMeta,
        utilities: Utilities,
        apiProperties: ApiProperties,
        packConfig: PackConfig,
        destination: String,
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    ) {
        GenerationNotifier.packGenerated(File(destination))
    }

    /** This extension's name as SPC lists it in `plugins.log`. */
    override val name = "Server pack test refresher"
    /** One line explaining the extension wherever SPC lists it. */
    override val description = "Refreshes the Server Test tab's pack list when a server pack is generated."
    /** Who to blame in `plugins.log` when this extension misbehaves. */
    override val author = "Griefed"
    /** This extension's own version, independent of the jar's. */
    override val version = "1.0.0"
    /** The key SPC stores this extension's configuration under. **Stable across releases.** */
    override val extensionId = "servertest-generation-refresher"
}
