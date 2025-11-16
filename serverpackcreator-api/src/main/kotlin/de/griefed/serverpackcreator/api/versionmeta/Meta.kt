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
package de.griefed.serverpackcreator.api.versionmeta

import java.io.File
import java.net.URL
import java.util.*

/**
 * Provide default methods to access a given Metas versions.
 * @author Griefed
 */
interface Meta {
    /**
     * Update the meta-information for this modloader-meta, updating the available loader and
     * installer versions, thus giving you access to version-checks, URLs etc.
     *
     * @author Griefed
     */
    fun update()

    /**
     * Get the latest loader version.
     *
     * @return The latest version.
     * @author Griefed
     */
    fun latestLoader(): String

    /**
     * Get the release loader version.
     *
     * @return The release loader version.
     * @author Griefed
     */
    fun releaseLoader(): String

    /**
     * Get the latest installer version.
     *
     * @return The latest installer version.
     * @author Griefed
     */
    fun latestInstaller(): String

    /**
     * Get the release installer version.
     *
     * @return The release installer version.
     * @author Griefed
     */
    fun releaseInstaller(): String

    /**
     * List of available loader versions in ascending order.
     *
     * @return Available loader versions in ascending order.
     * @author Griefed
     */
    fun loaderVersions(): List<String>

    /**
     * List of available installer version in ascending order.
     *
     * @return Available installer version in ascending order.
     * @author Griefed
     */
    fun installerVersions(): List<String>

    /**
     * Get the URL to the latest installer.
     *
     * @return URL to the latest installer.
     * @author Griefed
     */
    fun latestInstallerUrl(): URL

    /**
     * Get the URL to the release installer.
     *
     * @return URL to the release installer.
     * @author Griefed
     */
    fun releaseInstallerUrl(): URL

    /**
     * Installer file for the specified modloader version, wrapped in an [Optional], so you can check whether
     * it is available first.
     *
     * * **Fabric:** Pass the installer version you require.
     * * **LegacyFabric:** Pass the installer version you require.
     * * **Quilt:** Pass the installer version you require.
     *
     * @author Griefed
     */
    fun installerFor(version: String): Optional<File>

    /**
     * Check whether a URL to an installer is available for the specified version.
     *
     * @param version The modloader version for which to check for installer availability.
     * @return `true` if available.
     * @author Griefed
     */
    fun isInstallerUrlAvailable(version: String): Boolean

    /**
     * Get the URL to the installer for the specified version, wrapped in an Optional.
     *
     * @param version The modloader version for which to get the installer.
     * @return The URL to the installer, wrapped in an Optional.
     * @author Griefed
     */
    fun getInstallerUrl(version: String): Optional<URL>

    /**
     * Check whether the specified version is available/correct/valid.
     *
     * @param version The version to check.
     * @return `true` if the specified version is available/correct/valid.
     * @author Griefed
     */
    fun isVersionValid(version: String): Boolean

    /**
     * Check whether the given Minecraft version is supported by this modloader.
     *
     * @param minecraftVersion The Minecraft version for which to check for support.
     * @return `true` if the specified Minecraft version is supported.
     * @author Griefed
     */
    fun isMinecraftSupported(minecraftVersion: String): Boolean
}