/*
 * Copyright (C) 2024 Griefed.
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
package de.griefed.serverpackcreator.api.versionmeta.forge

import de.griefed.serverpackcreator.api.utilities.File
import de.griefed.serverpackcreator.api.utilities.Optional
import de.griefed.serverpackcreator.api.utilities.URL
import de.griefed.serverpackcreator.api.utilities.common.Utilities

/**
 * Forge meta containing information about available Forge releases.
 *
 * @param forgeManifest The manifest from which to acquire version information.
 * @param utilities     Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
@Suppress("unused")
expect class ForgeMeta(
    forgeManifest: File,
    utilities: Utilities,
    installerCacheDirectory: File
) {
    /**
     * Check whether the given Minecraft and Forge versions are valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @param forgeVersion     Forge version.
     * @return `true` if the given Minecraft and Forge versions are valid/supported/available.
     * @author Griefed
     */
    fun isForgeAndMinecraftCombinationValid(
        minecraftVersion: String,
        forgeVersion: String
    ): Boolean

    /**
     * Check whether a given Minecraft version is valid/supported/available.
     *
     * @param minecraftVersion Minecraft version.
     * @return `true` if the given Minecraft version is valid/supported/available.
     * @author Griefed
     */
    fun isMinecraftVersionSupported(minecraftVersion: String): Boolean

    /**
     * Check whether a given Forge version is valid/supported/available.
     *
     * @param forgeVersion Forge version.
     * @return `true` if the given Forge version is valid/supported/available.
     * @author Griefed
     */
    fun isForgeVersionValid(forgeVersion: String): Boolean

    /**
     * Check whether Forge is available for a given Forge- and Minecraft version.
     *
     * @param minecraftVersion Minecraft version.
     * @param forgeVersion     Forge version.
     * @return `true` if Forge is available for the given Forge- and Minecraft version.
     * @author Griefed
     */
    fun isForgeInstanceAvailable(minecraftVersion: String, forgeVersion: String): Boolean

    /**
     * Check whether Forge is available for a given Forge version
     *
     * @param forgeVersion Forge version.
     * @return `true` if Forge is available for the given Forge version.
     * @author Griefed
     */
    fun isForgeInstanceAvailable(forgeVersion: String): Boolean

    /**
     * Latest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Latest Forge version for the given Minecraft version, wrapped in an [Optional]
     * @author Griefed
     */
    fun newestForgeVersion(minecraftVersion: String): Optional<String>

    /**
     * Oldest Forge version for a given Minecraft version, wrapped in [Optional]
     *
     * @param minecraftVersion Minecraft version.
     * @return Oldest Forge version for the given Minecraft version, wrapped in [Optional]
     * @author Griefed
     */
    fun oldestForgeVersion(minecraftVersion: String): Optional<String>

    /**
     * Get the list of available Forge versions, in ascending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    fun forgeVersionsAscending(): MutableList<String>

    /**
     * Get the list of available Forge versions, in descending order.
     *
     * @return List of available Forge versions.
     * @author Griefed
     */
    fun forgeVersionsDescending(): List<String>

    /**
     * Get the array of available Forge versions, in ascending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    fun forgeVersionsAscendingArray(): Array<String>

    /**
     * Get the array of available Forge versions, in descending order.
     *
     * @return Array of available Forge versions.
     * @author Griefed
     */
    fun forgeVersionsDescendingArray(): Array<String>

    /**
     * Get a list of available Forge version for a given Minecraft version in ascending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in ascending order.
     * @author Griefed
     */
    fun supportedForgeVersionsAscending(minecraftVersion: String): Optional<List<String>>

    /**
     * Get a list of available Forge version for a given Minecraft version in descending order.
     *
     * @param minecraftVersion Minecraft version.
     * @return List of available Forge versions for the given Minecraft version in descending order.
     * @author Griefed
     */
    fun supportedForgeVersionsDescending(minecraftVersion: String): Optional<List<String>>

    /**
     * Get an array of available Forge version for a given Minecraft version, in ascending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in ascending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    fun supportedForgeVersionsAscendingArray(minecraftVersion: String): Optional<Array<String>>

    /**
     * Get an array of available Forge version for a given Minecraft version, in descending order,
     * wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Array of available Forge versions for the given Minecraft version, in descending order,
     * wrapped in an [Optional]
     * @author Griefed
     */
    fun supportedForgeVersionsDescendingArray(minecraftVersion: String): Optional<Array<String>>

    /**
     * Get the Minecraft version for a given Forge version, wrapped in an [Optional].
     *
     * @param forgeVersion Forge version.
     * @return Minecraft version for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    fun minecraftVersion(forgeVersion: String): Optional<String>

    /**
     * Get the list of Forge supported Minecraft versions, in ascending order.
     *
     * @return List of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsAscending(): MutableList<String>

    /**
     * Get the list of Forge supported Minecraft versions, in descending order.
     *
     * @return List of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsDescending(): List<String>

    /**
     * Get the array of Forge supported Minecraft versions, in ascending order.
     *
     * @return Array of Forge supported Minecraft versions, in ascending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsAscendingArray(): Array<String>

    /**
     * Get the array of Forge supported Minecraft versions, in descending order.
     *
     * @return Array of Forge supported Minecraft versions, in descending order.
     * @author Griefed
     */
    fun supportedMinecraftVersionsDescendingArray(): Array<String>

    /**
     * Get the Forge server installer URL for a given Forge version, wrapped in an [Optional].
     *
     * @param forgeVersion Forge version.
     * @return Forge server installer URL for the given Forge version, wrapped in an [Optional].
     * @author Griefed
     */
    fun installerUrl(forgeVersion: String): Optional<URL>

    /**
     * Installer file for the specified [forgeVersion] and [minecraftVersion] version, wrapped in an [Optional], so you
     * can check whether it is available first.
     * @author Griefed
     */
    fun installerFor(forgeVersion: String, minecraftVersion: String): Optional<File>


}