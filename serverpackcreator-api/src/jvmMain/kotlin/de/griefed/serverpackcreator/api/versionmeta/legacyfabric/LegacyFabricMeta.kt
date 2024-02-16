/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.createDirectories
import de.griefed.serverpackcreator.api.versionmeta.Meta
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.util.*
import javax.xml.parsers.ParserConfigurationException

/**
 * LegacyFabric meta providing game, loader and installer version information.
 *
 * @param gameVersionsManifest      Game version manifest.
 * @param loaderVersionsManifest    Loader version manifest.
 * @param installerVersionsManifest Installer version manifest.
 * @param utilities                 Commonly used utilities across ServerPackCreator.
 * @param installerCacheDirectory   The cache-directory for all installers.
 *
 * @author Griefed
 */
@Suppress("unused")
actual class LegacyFabricMeta actual constructor(
    gameVersionsManifest: File,
    loaderVersionsManifest: File,
    installerVersionsManifest: File,
    private val utilities: Utilities,
    installerCacheDirectory: File
) : Meta {
    private val gameVersions: LegacyFabricGame = LegacyFabricGame(gameVersionsManifest, utilities)
    private val loaderVersions: LegacyFabricLoader = LegacyFabricLoader(loaderVersionsManifest, utilities)
    private val installerVersions: LegacyFabricInstaller = LegacyFabricInstaller(installerVersionsManifest, utilities)
    private val installerDirectory: File = File(installerCacheDirectory, "legacyfabric")

    init {
        installerDirectory.createDirectories(create = true, directory = true)
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun update() {
        gameVersions.update()
        loaderVersions.update()
        installerVersions.update()
    }

    actual override fun latestLoader() = loaderVersions.allVersions[0]
    actual override fun releaseLoader() = loaderVersions.releases[0]
    actual override fun latestInstaller() = installerVersions.latest!!
    actual override fun releaseInstaller() = installerVersions.release!!
    actual override fun loaderVersionsListAscending() = loaderVersionsListDescending().reversed().toMutableList()
    actual override fun loaderVersionsListDescending() = loaderVersions.allVersions
    actual override fun loaderVersionsArrayAscending() = loaderVersionsListAscending().toTypedArray()
    actual override fun loaderVersionsArrayDescending() = loaderVersionsListDescending().toTypedArray()
    actual override fun installerVersionsListAscending() = installerVersions.allVersions
    actual override fun installerVersionsListDescending() = installerVersionsListAscending().reversed().toMutableList()
    actual override fun installerVersionsArrayAscending() = installerVersionsListAscending().toTypedArray()
    actual override fun installerVersionsArrayDescending() = installerVersionsListDescending().toTypedArray()

    @Throws(MalformedURLException::class)
    override fun latestInstallerUrl() = installerVersions.latestURL()

    @Throws(MalformedURLException::class)
    override fun releaseInstallerUrl() = installerVersions.releaseURL()

    actual override fun installerFor(version: String) =
        if (isInstallerUrlAvailable(version)) {
            val destination = File(installerDirectory, "$version.jar")
            if (!destination.isFile) {
                val url = installerVersions.specificURL(version).get()
                val downloaded = utilities.webUtilities.downloadFile(destination, url)
                if (downloaded) {
                    Optional.of(destination)
                } else {
                    Optional.empty()
                }
            } else {
                Optional.of(destination)
            }
        } else {
            Optional.empty()
        }

    /**
     * @author Griefed
     */
    actual override fun isInstallerUrlAvailable(version: String) =
        try {
            installerVersions.specificURL(version).isPresent
        } catch (e: MalformedURLException) {
            false
        }

    /**
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    override fun getInstallerUrl(version: String) = installerVersions.specificURL(version)

    /**
     * @author Griefed
     */
    actual override fun isVersionValid(version: String) = loaderVersions.allVersions.contains(version)

    /**
     * @author Griefed
     */
    actual override fun isMinecraftSupported(minecraftVersion: String) =
        gameVersions.allVersions.contains(minecraftVersion)

    /**
     * All Legacy Fabric supported Minecraft versions.
     *
     * @return All Legacy Fabric supported Minecraft versions.
     * @author Griefed
     */
    actual fun supportedMinecraftVersions() = gameVersions.allVersions
}