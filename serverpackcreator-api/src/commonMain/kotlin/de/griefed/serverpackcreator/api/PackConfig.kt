/*
 * Copyright (C) 2023 Griefed.
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
import de.griefed.serverpackcreator.api.utilities.File

/**
 * A PackConfig contains the settings required to create a server pack.
 * A configuration model usually consists of:
 *
 *  * Modpack directory
 *  * Minecraft version
 *  * Modloader
 *  * Modloader version
 *  * Java args for the start scripts
 *  * Files and directories to copy to the server pack
 *  * Whether to pre-install the modloader server
 *  * Whether to include a server-icon
 *  * Whether to include a server.properties
 *  * Whether to create a ZIP-archive
 *
 * @author Griefed
 */
expect open class PackConfig {

    val clientMods: ArrayList<String>
    val copyDirs: ArrayList<String>
    val scriptSettings: HashMap<String, String>
    val pluginsConfigs: HashMap<String, ArrayList<CommentedConfig>>
    var modpackDir: String
    var minecraftVersion: String
    var modloaderVersion: String
    var javaArgs: String
    var serverPackSuffix: String
    var serverIconPath: String
    var serverPropertiesPath: String
    var modloader: String
    var isServerInstallationDesired: Boolean
    var isServerIconInclusionDesired: Boolean
    var isServerPropertiesInclusionDesired: Boolean

    fun getPluginConfigs(pluginId: String): ArrayList<CommentedConfig>
    fun save(destination: File): PackConfig

    /**
     * Construct a new configuration model with custom values.
     *
     * @param clientMods                List of clientside mods to exclude from the server pack.
     * @param copyDirs                  List of directories and/or files to include in the server pack.
     * @param modpackDir                The path to the modpack.
     * @param minecraftVersion          The Minecraft version the modpack uses.
     * @param modLoader                 The modloader the modpack uses. Either `Forge`, `Fabric` or `Quilt`.
     * @param modLoaderVersion          The modloader version the modpack uses.
     * @param javaArgs                  JVM flags to create the start scripts with.
     * @param serverPackSuffix          Suffix to create the server pack with.
     * @param serverIconPath            Path to the icon to use in the server pack.
     * @param serverPropertiesPath      Path to the server.properties to create the server pack with.
     * @param includeServerInstallation Whether to install the modloader server in the server pack.
     * @param includeServerIcon         Whether to include the server-icon.png in the server pack.
     * @param includeServerProperties   Whether to include the server.properties in the server pack.
     * @param includeZipCreation        Whether to create a ZIP-archive of the server pack.
     * @param scriptSettings            Map containing key-value pairs to be used in start script creation.
     * @param pluginsConfigs             Configuration for any and all plugins used by this configuration.
     * @author Griefed
     */
    constructor(
        clientMods: List<String>,
        copyDirs: List<String>,
        modpackDir: String,
        minecraftVersion: String,
        modLoader: String,
        modLoaderVersion: String,
        javaArgs: String,
        serverPackSuffix: String,
        serverIconPath: String,
        serverPropertiesPath: String,
        includeServerInstallation: Boolean,
        includeServerIcon: Boolean,
        includeServerProperties: Boolean,
        includeZipCreation: Boolean,
        scriptSettings: HashMap<String, String>,
        pluginsConfigs: HashMap<String, ArrayList<CommentedConfig>>
    )

    constructor()
}