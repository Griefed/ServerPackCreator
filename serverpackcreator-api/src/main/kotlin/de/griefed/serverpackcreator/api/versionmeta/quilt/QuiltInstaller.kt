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
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
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
        "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar" // TODO Move URL to property
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
    private val latest = "latest" // TODO Move tagName to property
    private val release = "release" // TODO Move tagName to property
    private val version = "version" // TODO Move tagName to property

    /**
     * Update the Quilt installer versions by parsing the Fabric loader manifest.
     *
     * @author Griefed
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val document: Document = utilities.xmlUtilities.getXml(manifest)
        val latestElements = document.getElementsByTagName(latest)
        val latestNode = latestElements.item(0)
        val latestChildren = latestNode.childNodes
        val latestItem = latestChildren.item(0)
        latestInstaller = latestItem.nodeValue
        val releaseElements = document.getElementsByTagName(release)
        val releaseNode = releaseElements.item(0)
        val releaseChildren = releaseNode.childNodes
        val releaseItem = releaseChildren.item(0)
        releaseInstaller = releaseItem.nodeValue
        val latestUrl = installerUrlTemplate.format(latestInstaller, latestInstaller)
        try {
            latestInstallerUrl = URI(latestUrl).toURL()
        } catch (ignored: MalformedURLException) {
        }
        val releaseUrl = installerUrlTemplate.format(releaseInstaller, releaseInstaller)
        try {
            releaseInstallerUrl = URI(releaseUrl).toURL()
        } catch (ignored: MalformedURLException) {
        }
        installers.clear()
        val elements = document.getElementsByTagName(version)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            installers.add(item.nodeValue)
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
        URI(installerUrlTemplate.format(quiltInstallerVersion, quiltInstallerVersion)).toURL()
}