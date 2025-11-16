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
 * Base-interface from which every [de.griefed.serverpackcreator.api.serverpack.ServerPackHandler]-extension interface starts from.
 *
 * @author Griefed
 */
sealed interface ServerPackHandlerBase : ExtensionInformation {
    /**
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