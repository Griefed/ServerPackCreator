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

import de.griefed.serverpackcreator.api.utilities.CommentedConfig

/**
 * A server pack configuration from which to generate a server pack.
 *
 * @author Griefed
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class Pack<F, J, out P> {
    protected val forge = "^forge$".toRegex()
    protected val fabric = "^fabric$".toRegex()
    protected val quilt = "^quilt$".toRegex()
    protected val legacyFabric = "^legacyfabric$".toRegex()
    protected val whitespace = "\\s+".toRegex()

    val clientMods: ArrayList<String> = ArrayList(1000)
    val inclusions: ArrayList<InclusionSpecification> = ArrayList(100)
    val scriptSettings = HashMap<String, String>(100)
    val pluginsConfigs = HashMap<String, ArrayList<CommentedConfig>>(20)
    var modpackDir = ""
    var minecraftVersion = ""
    var modloaderVersion = ""
    var javaArgs = ""
    var serverPackSuffix = ""
    var serverIconPath = ""
    var serverPropertiesPath = ""
    var modloader = ""
        set(newModLoader) {
            if (newModLoader.lowercase().matches(forge)) {
                field = "Forge"
            } else if (newModLoader.lowercase().matches(fabric)) {
                field = "Fabric"
            } else if (newModLoader.lowercase().matches(quilt)) {
                field = "Quilt"
            } else if (newModLoader.lowercase().matches(legacyFabric)) {
                field = "LegacyFabric"
            }
        }
    var isServerInstallationDesired = true
    var isServerIconInclusionDesired = true
    var isServerPropertiesInclusionDesired = true
    var isZipCreationDesired = true
    var modpackJson: J? = null

    open var projectName: String? = null
    open var fileName: String? = null
    open var fileDiskName: String? = null

    /**
     * Save this configuration.
     *
     * @param destination The file to store the configuration in.
     * @return The configuration for further operations.
     * @author Griefed
     */
    abstract fun save(destination: F): P

    fun setPluginsConfigs(pluginConfigs: HashMap<String, ArrayList<CommentedConfig>>) {
        this.pluginsConfigs.clear()
        this.pluginsConfigs.putAll(pluginConfigs)
    }

    fun getPluginConfigs(pluginId: String): ArrayList<CommentedConfig> {
        if (!pluginsConfigs.containsKey(pluginId)) {
            pluginsConfigs[pluginId] = java.util.ArrayList(100)
        }
        return pluginsConfigs[pluginId]!!
    }

    fun setClientMods(newClientMods: ArrayList<String>) {
        clientMods.clear()
        newClientMods.removeIf { entry: String -> entry.matches(whitespace) || entry.isEmpty() }
        clientMods.addAll(newClientMods)
    }

    fun setInclusions(newCopyDirs: ArrayList<InclusionSpecification>) {
        inclusions.clear()
        inclusions.addAll(newCopyDirs)
    }

    fun setScriptSettings(settings: HashMap<String, String>) {
        scriptSettings.clear()
        scriptSettings.putAll(settings)
    }

    override fun toString(): String {
        return "Pack(" +
                " clientMods=$clientMods," +
                " copyDirs=$inclusions," +
                " scriptSettings=$scriptSettings," +
                " pluginsConfigs=$pluginsConfigs," +
                " modpackDir='$modpackDir'," +
                " minecraftVersion='$minecraftVersion'," +
                " modloaderVersion='$modloaderVersion'," +
                " javaArgs='$javaArgs'," +
                " serverPackSuffix='$serverPackSuffix'," +
                " serverIconPath='$serverIconPath'," +
                " serverPropertiesPath='$serverPropertiesPath'," +
                " modloader='$modloader'," +
                " isServerInstallationDesired=$isServerInstallationDesired," +
                " isServerIconInclusionDesired=$isServerIconInclusionDesired," +
                " isServerPropertiesInclusionDesired=$isServerPropertiesInclusionDesired," +
                " isZipCreationDesired=$isZipCreationDesired)"
    }
}