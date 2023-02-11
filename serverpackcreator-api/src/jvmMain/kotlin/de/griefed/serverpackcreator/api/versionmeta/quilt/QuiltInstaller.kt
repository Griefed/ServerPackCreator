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
package de.griefed.serverpackcreator.api.versionmeta.quilt

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import javax.xml.parsers.ParserConfigurationException

/**
 * Information about the Quilt installer.
 *
 * @param manifest  Quilt installer information.
 * @param utilities Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
internal class QuiltInstaller(
    private val manifest: File,
    private val utilities: Utilities
) {
    @Suppress("MemberVisibilityCanBePrivate")
    val installerUrlTemplate =
        "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar"
    val installers: MutableList<String> = ArrayList(100)
    val installerUrlMeta = HashMap<String, URL>(100)
    var latestInstaller: String? = null
        private set
    var releaseInstaller: String? = null
        private set
    var latestInstallerUrl: URL? = null
        private set
    var releaseInstallerUrl: URL? = null
        private set

    /**
     * Update the Quilt installer versions by parsing the Fabric loader manifest.
     *
     * @author Griefed
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val document: Document = utilities.xmlUtilities.getXml(manifest)
        latestInstaller = document
            .getElementsByTagName("latest")
            .item(0)
            .childNodes
            .item(0)
            .nodeValue
        releaseInstaller = document
            .getElementsByTagName("release")
            .item(0)
            .childNodes
            .item(0)
            .nodeValue
        try {
            latestInstallerUrl = URL(installerUrlTemplate.format(latestInstaller, latestInstaller))
        } catch (ignored: MalformedURLException) {
        }
        try {
            releaseInstallerUrl = URL(installerUrlTemplate.format(releaseInstaller, releaseInstaller))
        } catch (ignored: MalformedURLException) {
        }
        installers.clear()
        for (i in 0 until document.getElementsByTagName("version").length) {
            installers.add(
                document
                    .getElementsByTagName("version")
                    .item(i)
                    .childNodes
                    .item(0)
                    .nodeValue
            )
        }
        installerUrlMeta.clear()
        for (version in installers) {
            try {
                installerUrlMeta[version] = installerUrl(version)
            } catch (ignored: MalformedURLException) {
            }
        }
    }

    /**
     * Acquire the URL for the given Quilt version.
     *
     * @param quiltInstallerVersion Quilt version.
     * @return URL to the installer for the given Quilt version.
     * @throws MalformedURLException if the URL could not be formed.
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    private fun installerUrl(quiltInstallerVersion: String) =
        URL(installerUrlTemplate.format(quiltInstallerVersion, quiltInstallerVersion))
}