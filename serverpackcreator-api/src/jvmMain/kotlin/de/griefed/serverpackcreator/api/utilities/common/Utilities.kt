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
package de.griefed.serverpackcreator.api.utilities.common

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Access to any and all utility-classes we may have.
 *
 * @author Griefed
 */
@Suppress("unused")
actual class Utilities {
    actual val booleanUtilities: BooleanUtilities
    actual val fileUtilities: FileUtilities
    actual val jarUtilities: JarUtilities
    actual val listUtilities: ListUtilities
    actual val stringUtilities: StringUtilities
    actual val systemUtilities: SystemUtilities
    actual val webUtilities: WebUtilities
    actual val jsonUtilities: JsonUtilities
    actual val xmlUtilities: XmlUtilities

    /**
     * @param apiProperties   API configuration of this instance.
     * @param objectMapper    Used for JSON-parsing.
     * @param documentBuilderFactory Used for XML-parsing.
     */
    constructor(
        apiProperties: ApiProperties,
        objectMapper: ObjectMapper,
        documentBuilderFactory: DocumentBuilderFactory
    ) {
        booleanUtilities = BooleanUtilities()
        fileUtilities = FileUtilities()
        jarUtilities = JarUtilities()
        listUtilities = ListUtilities()
        stringUtilities = StringUtilities()
        systemUtilities = SystemUtilities()
        webUtilities = WebUtilities(apiProperties)
        jsonUtilities = JsonUtilities(objectMapper)
        xmlUtilities = XmlUtilities(documentBuilderFactory)
    }

    /**
     * @param booleanUtilities Common [Boolean]-related utilities.
     * @param fileUtilities    Common [java.io.File]-related utilities.
     * @param jarUtilities     Common JAR-file-related utilities.
     * @param listUtilities    Common list-related utilities.
     * @param stringUtilities  Common [String]-related utilities.
     * @param systemUtilities  Common System- and OS-related utilities.
     * @param webUtilities     Common Web-based-related utilities.
     * @param jsonUtilities    Common JSON-related utilities.
     * @param xmlUtilities     Common XML-related utilities.
     */
    constructor(
        booleanUtilities: BooleanUtilities,
        fileUtilities: FileUtilities,
        jarUtilities: JarUtilities,
        listUtilities: ListUtilities,
        stringUtilities: StringUtilities,
        systemUtilities: SystemUtilities,
        webUtilities: WebUtilities,
        jsonUtilities: JsonUtilities,
        xmlUtilities: XmlUtilities
    ) {
        this.booleanUtilities = booleanUtilities
        this.fileUtilities = fileUtilities
        this.jarUtilities = jarUtilities
        this.listUtilities = listUtilities
        this.stringUtilities = stringUtilities
        this.systemUtilities = systemUtilities
        this.webUtilities = webUtilities
        this.jsonUtilities = jsonUtilities
        this.xmlUtilities = xmlUtilities
    }
}