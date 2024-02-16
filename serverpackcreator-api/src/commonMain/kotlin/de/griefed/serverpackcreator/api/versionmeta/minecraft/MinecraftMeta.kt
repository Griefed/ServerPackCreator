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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

/**
 * Minecraft meta containing information about available client- and server releases.
 *
 * @author Griefed
 */
expect class MinecraftMeta {
    /**
     * Check whether a client is available for the specified Minecraft-version.
     *
     * @param minecraftVersion The Minecraft-version.
     * @return `true` if a client is available.
     * @author Griefed
     */
    fun isMinecraftVersionAvailable(minecraftVersion: String): Boolean

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * descending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * descending order.
     * @author Griefed
     */
    fun releaseVersionsArrayDescending(): Array<String>

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * descending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * descending order.
     * @author Griefed
     */
    fun releaseVersionsDescending(): List<String>

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * ascending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * ascending order.
     * @author Griefed
     */
    fun releaseVersionsArrayAscending(): Array<String>

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * ascending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in ascending
     * order.
     * @author Griefed
     */
    fun releaseVersionsAscending(): List<String>

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * descending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * descending order.
     * @author Griefed
     */
    fun snapshotVersionsArrayDescending(): Array<String>

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * descending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * descending order.
     * @author Griefed
     */
    fun snapshotVersionsDescending(): List<String>

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * ascending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * ascending order.
     * @author Griefed
     */
    fun snapshotVersionsArrayAscending(): Array<String>

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * ascending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * ascending order.
     * @author Griefed
     */
    fun snapshotVersionsAscending(): List<String>

    /**
     * Get an array of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in descending order.
     *
     * @return All available Minecraft versions in descending order.
     * @author Griefed
     */
    fun allVersionsArrayDescending(): Array<String>

    /**
     * Get a list of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in descending order.
     *
     * @return All available Minecraft versions in descending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun allVersionsDescending(): List<String>

    /**
     * Get an array of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in ascending order.
     *
     * @return All available Minecraft versions in ascending order.
     * @author Griefed
     */
    @Suppress("unused")
    fun allVersionsArrayAscending(): Array<String>

    /**
     * Get a list of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in ascending order.
     *
     * @return All available Minecraft versions in ascending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun allVersionsAscending(): List<String>

    /**
     * Check whether a server is available for the specified Minecraft-version.
     *
     * @param minecraftVersion The Minecraft-version.
     * @return `true` if a server is available.
     * @author Griefed
     */
    fun isServerAvailable(minecraftVersion: String): Boolean

}