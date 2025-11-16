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
package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.api.versionmeta.Meta
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
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
class LegacyFabricMeta(
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
        installerDirectory.create(createFileOrDir = true, asDirectory = true)
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun update() {
        gameVersions.update()
        loaderVersions.update()
        installerVersions.update()
    }

    override fun latestLoader() = loaderVersions.allVersions[0]
    override fun releaseLoader() = loaderVersions.releases[0]
    override fun latestInstaller() = installerVersions.latest!!
    override fun releaseInstaller() = installerVersions.release!!
    override fun loaderVersions() = loaderVersions.allVersions
    override fun installerVersions() = installerVersions.allVersions

    @Throws(MalformedURLException::class)
    override fun latestInstallerUrl() = installerVersions.latestURL()

    @Throws(MalformedURLException::class)
    override fun releaseInstallerUrl() = installerVersions.releaseURL()

    override fun installerFor(version: String) =
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
    override fun isInstallerUrlAvailable(version: String): Boolean =
        try {
            installerVersions.specificURL(version).isPresent
        } catch (e: MalformedURLException) {
            false
        }

    /**
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    override fun getInstallerUrl(version: String): Optional<URL> = installerVersions.specificURL(version)

    /**
     * @author Griefed
     */
    override fun isVersionValid(version: String): Boolean = loaderVersions.allVersions.contains(version)

    /**
     * @author Griefed
     */
    override fun isMinecraftSupported(minecraftVersion: String): Boolean =
        gameVersions.allVersions.contains(minecraftVersion)

    /**
     * All Legacy Fabric supported Minecraft versions.
     *
     * @return All Legacy Fabric supported Minecraft versions.
     * @author Griefed
     */
    fun supportedMinecraftVersions(): MutableList<String> = gameVersions.allVersions
}