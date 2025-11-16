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
package de.griefed.serverpackcreator.api.utilities.common

import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Commonly used XML utilities.
 *
 * @param documentBuilderFactory used for parsing XML-files.
 *
 * @author Griefed
 */
@Suppress("unused")
class XmlUtilities(private val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()) {

    /**
     * Reads the file into a [Document] and [Document.normalize] it.
     *
     * @param manifest The xml-file to parse into a Document.
     * @return The parsed and normalized document.
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws IOException                  if any IO errors occur.
     * @throws SAXException                 if any parse errors occur.
     * @author Griefed
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun getXml(manifest: File) = getXml(manifest.readText())

    /**
     * Reads the string into a [Document] and [Document.normalize] it.
     *
     * @param string The xml-string to parse into a Document.
     * @return The parsed and normalized document.
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws IOException                  if any IO errors occur.
     * @throws SAXException                 if any parse errors occur.
     * @author Griefed
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun getXml(string: String): Document {
        val reader = StringReader(string)
        val xml = documentBuilderFactory.newDocumentBuilder().parse(InputSource(reader))
        reader.close()
        xml.normalize()
        return xml
    }

    /**
     * Reads the input-stream into a [Document] and [Document.normalize] it.
     *
     * @param manifest The xml-input-stream to parse into a Document.
     * @return The parsed and normalized document.
     * @throws ParserConfigurationException indicates a serious configuration error.
     * @throws IOException                  if any IO errors occur.
     * @throws SAXException                 if any parse errors occur.
     * @author Griefed
     */
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun getXml(manifest: InputStream) = getXml(manifest.readText())
}