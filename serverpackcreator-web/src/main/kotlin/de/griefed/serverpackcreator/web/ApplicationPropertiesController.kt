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
package de.griefed.serverpackcreator.web

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RestController for acquiring the configuration of this ServerPackCreator instance.
 *
 * @author Griefed
 */
@Suppress("unused")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/settings")
class ApplicationPropertiesController @Autowired constructor(
    private val apiProperties: ApiProperties,
    private val utilities: Utilities
) {

    @get:GetMapping(produces = ["application/json"])
    val configuration: String
        get() = (((("{"
                + "\"listFallbackMods\":"
                + utilities.listUtilities.encapsulateListElements(apiProperties.clientSideMods())
                ) + ","
                + "\"listDirectoriesExclude\":"
                + utilities.listUtilities
            .encapsulateListElements(apiProperties.directoriesToExclude.toList())
                ) + ","
                + "\"serverPackCreatorVersion\":\""
                + apiProperties.apiVersion
                ) + "\","
                + "\"supportedModloaders\":"
                + utilities.listUtilities
            .encapsulateListElements(apiProperties.supportedModloaders.toList())
                ) + "}"
}