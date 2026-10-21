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
    /** Download and link-opening helpers. Deliberately in `-api` so plugins get them too. */
    val webUtilities: WebUtilities
    /** JSON reading/parsing helpers, injected wherever a manifest has to be understood. */
    val jsonUtilities: JsonUtilities
    /** XML reading helpers, used for the maven-metadata manifests Forge and Fabric publish. */
    val xmlUtilities: XmlUtilities

    /**
     * Builds the bundle from the pieces each helper needs, constructing the helpers itself.
     */
    constructor(
        /** API configuration of this instance. */
        apiProperties: ApiProperties,
        /** Used for JSON-parsing. */
        objectMapper: ObjectMapper,
        /** Used for XML-parsing. */
        documentBuilderFactory: DocumentBuilderFactory
    ) {
        webUtilities = WebUtilities(apiProperties)
        jsonUtilities = JsonUtilities(objectMapper)
        xmlUtilities = XmlUtilities(documentBuilderFactory)
    }

    /**
     * Builds the bundle from helpers that have already been constructed.
     */
    constructor(
        /** Common Web-based-related utilities. */
        webUtilities: WebUtilities,
        /** Common JSON-related utilities. */
        jsonUtilities: JsonUtilities,
        /** Common XML-related utilities. */
        xmlUtilities: XmlUtilities
    ) {
        this.webUtilities = webUtilities
        this.jsonUtilities = jsonUtilities
        this.xmlUtilities = xmlUtilities
    }
}