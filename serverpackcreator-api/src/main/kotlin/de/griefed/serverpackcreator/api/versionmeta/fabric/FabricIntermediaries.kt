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
package de.griefed.serverpackcreator.api.versionmeta.fabric

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Intermediaries for Fabric.
 *
 * @param intermediaryManifest Fabric Intermediary manifest-file.
 * @param objectMapper         Object mapper for JSON parsing.
 *
 * @author Griefed
 */
@Suppress("unused")
class FabricIntermediaries(
    private val intermediaryManifest: File,
    private val objectMapper: ObjectMapper
) {
    @Suppress("MemberVisibilityCanBePrivate")
    val intermediaries = HashMap<String?, FabricIntermediary>(100)

    /**
     * Update the intermediaries for Fabric.
     *
     * @throws IOException when the manifest could not be read.
     */
    @Throws(IOException::class)
    fun update() {
        for (intermediary in listIntermediariesFromManifest()) {
            intermediaries[intermediary.version] = intermediary
        }
    }

    /**
     * Get a list of intermediaries from the manifest.
     *
     * @return List of intermediaries.
     * @throws IOException when the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    private fun listIntermediariesFromManifest() =
        objectMapper.readValue(intermediaryManifest, object : TypeReference<List<FabricIntermediary>>() {})

    /**
     * Check whether Fabric Intermediaries for the given Minecraft version are present, indicating
     * that the given Minecraft version is supported.
     *
     * @param minecraftVersion The Minecraft version to check for.
     * @return `true` if intermediaries are present.
     */
    fun isIntermediariesPresent(minecraftVersion: String) = getIntermediary(minecraftVersion).isPresent

    /**
     * Get a specific intermediary, wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return A specific intermediary, wrapped in an [Optional].
     * @author Griefed
     */
    fun getIntermediary(minecraftVersion: String) = Optional.ofNullable(intermediaries[minecraftVersion])
}