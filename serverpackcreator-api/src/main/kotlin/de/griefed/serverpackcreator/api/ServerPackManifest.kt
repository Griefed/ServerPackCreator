/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.api

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class ServerPackManifest {
    var files: List<String> = ArrayList(10000)
    var minecraftVersion: String = ""
    var modloader: String = ""
    var modloaderVersion: String = ""
    var serverPackCreatorVersion: String = ""

    constructor(
        files: List<String>,
        minecraftVersion: String,
        modloader: String,
        modloaderVersion: String,
        serverPackCreatorVersion: String
    ) {
        this.files = files
        this.minecraftVersion = minecraftVersion
        this.modloader = modloader
        this.modloaderVersion = modloaderVersion
        this.serverPackCreatorVersion = serverPackCreatorVersion
    }

    constructor()

    fun writeToFile(destination: File, objectMapper: ObjectMapper) {
        val content = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(this)
        File(destination, "manifest.json").writeText(content)
    }

}