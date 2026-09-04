/* Copyright (C) 2026 Griefed
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
import de.griefed.serverpackcreator.api.versionmeta.VersionMetaConfig
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.util.*
import javax.xml.parsers.ParserConfigurationException
import java.util.Collections

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
    /** Every LegacyFabric *installer* version, newest first. A separate series from the loader versions. */
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place. The refresh runs on a background coroutine while callers read; clearing and refilling a
     * shared list let a reader throw `ConcurrentModificationException` or silently observe the empty
     * window between the two.
     */
    @Volatile
    var allVersions: List<String> = emptyList()
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    /** URL template the installer download is built from, with the version substituted in. */
    val installerUrlTemplate = VersionMetaConfig.LEGACYFABRIC_INSTALLER_TEMPLATE
    /** Newest installer version the manifest advertises, or `null` before the manifest has been read. */
    var latest: String? = null
        private set
    /** Newest *stable* installer version, or `null` before the manifest has been read. */
    var release: String? = null
        private set
    private val latestElement = VersionMetaConfig.TAG_LATEST
    private val releaseElement = VersionMetaConfig.TAG_RELEASE
    private val version = VersionMetaConfig.TAG_VERSION

    /**
     * Update all lists of available versions with new information gathered from the manifest.
     *
     * @throws IOException when the manifest could not be parsed.
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val next_allVersions = ArrayList<String>(100)
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
        val elements = installerManifest.getElementsByTagName(version)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            next_allVersions.add(item.nodeValue)
        }
            // Published in one assignment each, as unmodifiable views: a `List`-typed field still
        // holds an ArrayList at runtime, so a caller could otherwise cast and mutate our state.
        allVersions = Collections.unmodifiableList(next_allVersions)
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
