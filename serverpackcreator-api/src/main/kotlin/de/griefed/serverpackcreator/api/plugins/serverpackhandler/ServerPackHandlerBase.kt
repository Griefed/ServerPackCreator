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
package de.griefed.serverpackcreator.api.plugins.serverpackhandler


import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.ExtensionException
import de.griefed.serverpackcreator.api.plugins.ExtensionInformation
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.util.*

/**
 * The shape every generation hook shares. Sealed, so the hook a plugin implements is one of
 * [PreGenExtension], [PreZipExtension] or [PostGenExtension] — which of the three decides *when*
 * [run] fires, not what it is handed.
 *
 * @author Griefed
 */
sealed interface ServerPackHandlerBase : ExtensionInformation {
    /**
     * Does this hook's work on the server pack at [destination], at the moment its own interface names.
     *
     * Called once per generation by `ApiPlugins.runPreGenExtensions`, `runPreZipExtensions` or
     * `runPostGenExtensions`, all three of which `ServerPackHandler.run` drives in that order: before
     * any file is gathered, after they are gathered but before the ZIP is written, and after the pack is
     * finished. [destination] is a real directory on disk at every one of those points, so a hook may
     * add to it or read it — a PreZip hook writing there still lands inside the archive, a PostGen hook
     * does not. Returning normally is the only success signal — the host catches `ExtensionException`,
     * `Exception` and `Error` alike and logs them against this plugin in `plugins.log`, so a hook cannot
     * fail a generation and a silent one is indistinguishable from one that did nothing.
     *
     * @param versionMeta           Instance of [VersionMeta] so you can work with available Minecraft, Forge, Fabric,
     * LegacyFabric and Quilt versions.
     * @param utilities             Instance of [Utilities] commonly used across ServerPackCreator.
     * @param apiProperties Instance of [ApiProperties] as ServerPackCreator itself uses it.
     * @param packConfig    Instance of [PackConfig] for a given server pack.
     * @param destination           String. The destination of the server pack.
     * @param pluginConfig          Configuration for this plugin, conveniently provided by ServerPackCreator.
     * @param packSpecificConfigs   Modpack and server pack specific configurations for this plugin, conveniently
     * provided by ServerPackCreator.
     * @throws ExtensionException when an uncaught error occurs in the plugin.
     * @author Griefed
     */
    @Throws(ExtensionException::class)
    fun run(
        versionMeta: VersionMeta,
        utilities: Utilities,
        apiProperties: ApiProperties,
        packConfig: PackConfig,
        destination: String,
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    )
}