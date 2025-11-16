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
package de.griefed.serverpackcreator.api.versionmeta.quilt

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.utilities.common.create
import de.griefed.serverpackcreator.api.versionmeta.Meta
import de.griefed.serverpackcreator.api.versionmeta.fabric.FabricIntermediaries
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.util.*
import javax.xml.parsers.ParserConfigurationException

/**
 * Quilt meta containing information about available Quilt versions and installers.
 *
 * @param quiltManifest          Quilt manifest file.
 * @param quiltInstallerManifest Quilt-installer manifest file.
 * @param fabricIntermediaries   Fabric-Intermediaries for further compatibility tests.
 * @param utilities              Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
class QuiltMeta(
    quiltManifest: File,
    quiltInstallerManifest: File,
    private val fabricIntermediaries: FabricIntermediaries,
    private val utilities: Utilities,
    installerCacheDirectory: File
) : Meta {
    private val quiltLoader: QuiltLoader = QuiltLoader(quiltManifest, utilities)
    private val quiltInstaller: QuiltInstaller = QuiltInstaller(quiltInstallerManifest, utilities)
    private val installerDirectory: File = File(installerCacheDirectory, "quilt")

    init {
        installerDirectory.create(createFileOrDir = true, asDirectory = true)
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    override fun update() {
        quiltLoader.update()
        quiltInstaller.update()
    }

    override fun latestLoader() = quiltLoader.latest!!
    override fun releaseLoader() = quiltLoader.release!!
    override fun latestInstaller() = quiltInstaller.latestInstaller!!
    override fun releaseInstaller() = quiltInstaller.releaseInstaller!!
    override fun loaderVersions() = quiltLoader.loaders
    override fun installerVersions() = quiltInstaller.installers
    override fun latestInstallerUrl() = quiltInstaller.latestInstallerUrl!!
    override fun releaseInstallerUrl() = quiltInstaller.releaseInstallerUrl!!

    override fun installerFor(version: String) =
        if (isInstallerUrlAvailable(version)) {
            val destination = File(installerDirectory, "$version.jar")
            if (!destination.isFile) {
                val url = quiltInstaller.installerUrlMeta[version]!!
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

    override fun isInstallerUrlAvailable(version: String) = Optional.ofNullable(quiltInstaller.installerUrlMeta[version]).isPresent
    override fun getInstallerUrl(version: String) = Optional.ofNullable(quiltInstaller.installerUrlMeta[version])
    override fun isVersionValid(version: String) = quiltLoader.loaders.contains(version)
    override fun isMinecraftSupported(minecraftVersion: String) = fabricIntermediaries.isIntermediariesPresent(minecraftVersion)
}