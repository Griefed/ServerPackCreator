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

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiProperties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Access to any and all utility-classes we may have.
 *
 * @author Griefed
 */
@Suppress("unused")
class Utilities {
    val webUtilities: WebUtilities
    val jsonUtilities: JsonUtilities
    val xmlUtilities: XmlUtilities

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
        webUtilities = WebUtilities(apiProperties)
        jsonUtilities = JsonUtilities(objectMapper)
        xmlUtilities = XmlUtilities(documentBuilderFactory)
    }

    /**
     * @param webUtilities     Common Web-based-related utilities.
     * @param jsonUtilities    Common JSON-related utilities.
     * @param xmlUtilities     Common XML-related utilities.
     */
    constructor(
        webUtilities: WebUtilities,
        jsonUtilities: JsonUtilities,
        xmlUtilities: XmlUtilities
    ) {
        this.webUtilities = webUtilities
        this.jsonUtilities = jsonUtilities
        this.xmlUtilities = xmlUtilities
    }
}