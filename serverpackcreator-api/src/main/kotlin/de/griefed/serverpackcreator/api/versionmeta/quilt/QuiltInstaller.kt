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
package de.griefed.serverpackcreator.api.versionmeta.quilt

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMetaConfig
import org.w3c.dom.Document
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import javax.xml.parsers.ParserConfigurationException
import java.util.Collections

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
    val installerUrlTemplate = VersionMetaConfig.QUILT_INSTALLER_TEMPLATE
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place. The refresh runs on a background coroutine while callers read; clearing and refilling a
     * shared list let a reader throw `ConcurrentModificationException` or silently observe the empty
     * window between the two.
     */
    @Volatile
    var installers: List<String> = emptyList()
        private set
    val installerUrlMeta = HashMap<String, URL>(100)
    var latestInstaller: String? = null
        private set
    var releaseInstaller: String? = null
        private set
    var latestInstallerUrl: URL? = null
        private set
    var releaseInstallerUrl: URL? = null
        private set
    private val latest = VersionMetaConfig.TAG_LATEST
    private val release = VersionMetaConfig.TAG_RELEASE
    private val version = VersionMetaConfig.TAG_VERSION

    /**
     * Update the Quilt installer versions by parsing the Fabric loader manifest.
     *
     * @author Griefed
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val next_installers = ArrayList<String>(100)
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
            // The latest-installer URL couldn't be parsed -> latestInstallerUrl stays unset.
        }
        val releaseUrl = installerUrlTemplate.format(releaseInstaller, releaseInstaller)
        try {
            releaseInstallerUrl = URI(releaseUrl).toURL()
        } catch (ignored: MalformedURLException) {
            // The release-installer URL couldn't be parsed -> releaseInstallerUrl stays unset.
        }
        val elements = document.getElementsByTagName(version)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            next_installers.add(item.nodeValue)
        }
        installerUrlMeta.clear()
        for (version in installers) {
            try {
                installerUrlMeta[version] = installerUrl(version)
            } catch (ignored: MalformedURLException) {
                // A version whose installer URL can't be parsed is omitted from the URL map.
            }
        }
            // Published in one assignment each, as unmodifiable views: a `List`-typed field still
        // holds an ArrayList at runtime, so a caller could otherwise cast and mutate our state.
        installers = Collections.unmodifiableList(next_installers)
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
