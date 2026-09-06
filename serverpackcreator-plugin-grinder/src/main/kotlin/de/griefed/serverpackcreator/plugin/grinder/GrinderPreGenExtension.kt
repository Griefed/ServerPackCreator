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
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.serverpackhandler.PreGenExtension
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.plugin.grinder.core.ClientsideEntryInjector
import de.griefed.serverpackcreator.plugin.grinder.core.SelectionStore
import org.apache.logging.log4j.LogManager
import org.pf4j.Extension
import java.util.Optional

/**
 * Folds the entries ticked in the Grinder tab into the server pack's clientside-mod exclusion list.
 *
 * This is where the plugin actually changes a server pack. `ServerPackHandler.run` calls
 * `ApiPlugins.runPreGenExtensions` and *then* reads `packConfig.clientMods` to compile the mod list, so
 * adding to that list here is what excludes a mod — and since the hook sits inside `run`, it applies to
 * the GUI, the CLI and the web backend alike. The selection is read from the plugin configuration, which
 * is what lets a headless run honour ticks made in the GUI: `ApiPlugins` hands the same `CommentedConfig`
 * instance to the tab and to this extension.
 *
 * Nothing here is destructive. The user's own list is left in place and in order, the entries are only
 * appended, and `serverpackcreator.conf` is never written — untick an entry and the next generation is
 * back to what it was.
 *
 * @author Griefed
 */
@Suppress("unused")
@Extension
class GrinderPreGenExtension : PreGenExtension {

    private val pluginsLog = LogManager.getLogger("AddonsLogger")

    override val name = "Grinder clientside-mod exclusions"
    override val description =
        "Adds the verdicts you ticked in the Grinder tab to this server pack's clientside-mod exclusions."
    override val author = "Griefed"
    override val version = "1.0.0"
    override val extensionId = "grinder-clientside-exclusions"

    /**
     * Append the ticked entries to [packConfig]'s exclusion list, then log what was added.
     *
     * An absent [pluginConfig] — what ServerPackCreator provides when the configuration file could not
     * be parsed — is a no-op rather than a failure: an exclusion the user cannot even see is not worth
     * aborting somebody's server pack over. The merge is idempotent, which matters because one session
     * generates repeatedly against this same live configuration.
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
        val selected = pluginConfig.map { SelectionStore(it).allSelected() }.orElse(emptySet())
        if (selected.isEmpty()) {
            return
        }
        val before = packConfig.clientMods.toList()
        val merged = ClientsideEntryInjector.inject(before, selected)
        val added = merged.size - before.size
        if (added == 0) {
            // Everything ticked was already excluded. Say so rather than staying silent: an operator
            // reading the log wants to know the plugin ran and decided, not wonder whether it did.
            pluginsLog.info("Grinder: all ${selected.size} ticked entries were already excluded.")
            return
        }
        packConfig.setClientMods(merged.toMutableList())
        pluginsLog.info("Grinder: added $added ticked entries to the clientside-mod exclusions.")
    }
}
