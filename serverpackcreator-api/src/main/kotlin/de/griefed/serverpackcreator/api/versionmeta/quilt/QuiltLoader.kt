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
import javax.xml.parsers.ParserConfigurationException

/**
 * Information about releases of the Quilt loader.
 *
 * @param manifest  The manifest used when updating available versions.
 * @param utilities Commonly used utilities across ServerPackCreator.
 *
 * @author Griefed
 */
internal class QuiltLoader(
    private val manifest: File,
    private val utilities: Utilities
) {

    val loaders: MutableList<String> = ArrayList(100)
    var latest: String? = null
        private set
    var release: String? = null
        private set
    private val latestElement = "latest" // TODO Move tagName to property
    private val releaseElement = "release" // TODO Move tagName to property
    private val version = "version" // TODO Move tagName to property

    /**
     * Update the Quilt loader versions by parsing the Fabric loader manifest.
     *
     * @author Griefed
     */
    @Suppress("DuplicatedCode")
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun update() {
        val document: Document = utilities.xmlUtilities.getXml(manifest)
        val latestElements = document.getElementsByTagName(latestElement)
        val latestNode = latestElements.item(0)
        val latestChildren = latestNode.childNodes
        val latestItem = latestChildren.item(0)
        latest = latestItem.nodeValue
        val releaseElements = document.getElementsByTagName(releaseElement)
        val releaseNode = releaseElements.item(0)
        val releaseChildren = releaseNode.childNodes
        val releaseItem = releaseChildren.item(0)
        release = releaseItem.nodeValue
        loaders.clear()
        val elements = document.getElementsByTagName(version)
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            val children = node.childNodes
            val item = children.item(0)
            loaders.add(item.nodeValue)
        }
    }
}