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
package de.griefed.serverpackcreator.api.plugins.configurationhandler

import com.electronwill.nightconfig.core.CommentedConfig
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.plugins.ExtensionException
import de.griefed.serverpackcreator.api.plugins.ExtensionInformation
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import java.util.*

/**
 * Extension point for configuration checks, so you can run your own checks on a given
 * [PackConfig] should you so desire.
 *
 * @author Griefed
 */
@Suppress("unused")
interface ConfigCheckExtension : ExtensionInformation {

    /**
     * @param versionMeta           Instance of [VersionMeta] so you can work with available
     * Minecraft, Forge, Fabric, LegacyFabric and Quilt versions.
     * @param apiProperties Instance of [ApiProperties] The current
     * configuration of ServerPackCreator, like the default list of
     * clientside-only mods, the server pack directory etc.
     * @param utilities             Instance of [Utilities] commonly used across
     * ServerPackCreator.
     * @param packConfig    The configuration to check.
     * @param configCheck Contains all encountered errors during the check of the passed configuration.
     * @param pluginConfig           Configuration for this plugin, conveniently provided by
     * ServerPackCreator.
     * @param packSpecificConfigs   Modpack and server pack specific configurations for this plugin,
     * conveniently provided by ServerPackCreator.
     * @return `true` if an error was encountered. `false` if the checks were successful.
     * @throws ExtensionException if any unexpected error is encountered during the execution of this method.
     * @author Griefed
     */
    @Throws(ExtensionException::class)
    fun runCheck(
        versionMeta: VersionMeta,
        apiProperties: ApiProperties,
        utilities: Utilities,
        packConfig: PackConfig,
        configCheck: ConfigCheck = ConfigCheck(),
        pluginConfig: Optional<CommentedConfig>,
        packSpecificConfigs: ArrayList<CommentedConfig>
    ): Boolean
}