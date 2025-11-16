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
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.*
import javax.xml.parsers.ParserConfigurationException

/**
 * Information about the LegacyFabric installer and versions.
 *
 * @param installerManifest Manifest containing information about LegacyFabric installer versions.
 * @param utilities         Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
class LegacyFabricInstaller(
    private val installerManifest: File,
    private val utilities: Utilities
) {
    val allVersions: MutableList<String> = ArrayList(100)

    @Suppress("MemberVisibilityCanBePrivate")
    val installerUrlTemplate = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/%s/fabric-installer-%s.jar" // TODO Move URL to property
    var latest: String? = null
        private set
    var release: String? = null
        private set
    private val latestElement = "latest" // TODO Move tagName to property
    private val releaseElement = "release" // TODO Move tagName to property
    private val version = "version" // TODO Move tagName to property

    /**
     * Update all lists of available versions with new information gathered from the manifest.
     *
     * @throws IOException when the manifest could not be parsed.
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val installerManifest: Document = utilities.xmlUtilities.getXml(installerManifest)
        val latestElements = installerManifest.getElementsByTagName(latestElement)
        val latestNode = latestElements.item(0)
        val latestChildren = latestNode.childNodes
        val latestItem = latestChildren.item(0)
        latest = latestItem.nodeValue

        val releaseElements = installerManifest.getElementsByTagName(releaseElement)
        val releaseNode = releaseElements.item(0)
        val releaseChildren = releaseNode.childNodes
        val releaseItem = releaseChildren.item(0)
        release = releaseItem.nodeValue
        allVersions.clear()
        val elements = installerManifest.getElementsByTagName(version)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            allVersions.add(item.nodeValue)
        }
    }

    /**
     * The URL to the latest installer for Legacy Fabric.
     *
     * @return URL to the latest installer for Legacy Fabric.
     * @throws MalformedURLException when the URL could not be created.
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    fun latestURL(): URL = URI(installerUrlTemplate.format(latest, latest)).toURL()

    /**
     * The URL to the release installer for Legacy Fabric.
     *
     * @return URL to the release installer for Legacy Fabric.
     * @throws MalformedURLException when the URL could not be created.
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    fun releaseURL(): URL = URI(installerUrlTemplate.format(release, latest)).toURL()

    /**
     * Get the URL for a specific installer version, wrapped in an Optional.
     *
     * @param version The version of the installer for which to get the URL.
     * @return URL to the installer, for the specified version, wrapped in an Optional.
     * @throws MalformedURLException when the URL could not be created.
     * @author Griefed
     */
    @Throws(MalformedURLException::class)
    fun specificURL(version: String): Optional<URL> =
        if (allVersions.contains(version)) {
            val url = installerUrlTemplate.format(version, version)
            Optional.of(URI(url).toURL())
        } else {
            Optional.empty()
        }

}