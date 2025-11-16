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
package de.griefed.serverpackcreator.api.serverpack

import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.api.ApiWrapper
import java.io.File

/**
 * The server pack manifest shipped with every modpack. It includes information about the
 * * Minecraft version
 * * Modloader
 * * Modloader version
 * * ServerPackCreator version used in the generation of this server pack
 * * A list of relative files included in a server pack
 *
 * @author Griefed
 */
@Suppress("unused")
class ServerPackManifest {
    var files: List<String> = ArrayList(10000)
    var minecraftVersion: String = ""
    var modloader: String = ""
    var modloaderVersion: String = ""
    val serverPackCreatorVersion: String = ApiWrapper.api().apiProperties.apiVersion

    constructor(
        files: List<String>,
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
    ) {
        this.files = files
        this.minecraftVersion = minecraftVersion
        this.modloader = modloader
        this.modloaderVersion = modloaderVersion
    }

    constructor()

    fun writeToFile(destination: File, objectMapper: ObjectMapper) {
        val content = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(this)
        File(destination, "manifest.json").writeText(content)
    }

}