/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.utilities.Optional
import de.griefed.serverpackcreator.api.utilities.CommentedConfig
import de.griefed.serverpackcreator.api.utilities.File

expect class ApiPlugins {

    /**
     * Get the global plugin configuration for an plugin of the passed ID. The configuration is wrapped
     * in an [Optional], because an plugin may not provide a global configuration. If you intend
     * on using a global configuration for your plugin, make sure to check whether it is present before
     * trying to use it!
     *
     * @param pluginId The plugin ID of the...well...plugin.
     * @return The global plugin configuration, wrapped in an Optional.
     * @author Griefed
     */
    fun getPluginConfig(pluginId: String): Optional<CommentedConfig>

    /**
     * Run any and all Pre-Server Pack-Generation extensions, using the passed configuration model and
     * the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPreGenExtensions(packConfig: PackConfig, destination: String)

    /**
     * Run any and all Pre-ZIP-archive creation extensions, using the passed configuration model and
     * the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPreZipExtensions(packConfig: PackConfig, destination: String)

    /**
     * Run any and all Post-server pack-generation extensions, using the passed configuration model
     * and the destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model from which to create the server pack.
     * @param destination        The destination at which the server pack will be generated and stored
     * at.
     * @author Griefed
     */
    fun runPostGenExtensions(packConfig: PackConfig, destination: String)

    /**
     * Run any and all configuration-check extensions, using the passed configuration model and the
     * destination at which the server pack is to be generated and stored at.
     *
     * @param packConfig The configuration model containing the server pack and plugin
     * configurations to check.
     * @param encounteredErrors  A list of encountered errors to add to in case anything goes wrong.
     * This list is displayed to the user after am unsuccessful server pack
     * generation to help them figure out what went wrong.
     * @return `true` if any custom check detected an error with the configuration.
     * **Only** return `false` when not a **single** check
     * errored.
     * @author Griefed
     */
    fun runConfigCheckExtensions(
        packConfig: PackConfig,
        encounteredErrors: MutableList<String>
    ): Boolean

    /**
     * Get the configuration-file for a plugin, if it exists. This is wrapped in an [Optional],
     * because not every plugin may provide a configuration-file to use globally for the relevant
     * plugins settings. If you intend on using a global configuration, make sure to check whether the
     * file is present, before moving on!
     *
     * @param pluginId The plugin ID with which to identify the correct config-file to return.
     * @return The config-file corresponding to the ID of the plugin, wrapped in an Optional.
     * @author Griefed
     */
    fun getPluginConfigFile(pluginId: String): Optional<File>
}
