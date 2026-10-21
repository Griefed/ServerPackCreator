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
package de.griefed.serverpackcreator.api.versionmeta.fabric

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Intermediaries for Fabric.
 *
 * @author Griefed
 */
@Suppress("unused")
class FabricIntermediaries(
    /** Fabric Intermediary manifest-file. */
    private val intermediaryManifest: File,
    /** Object mapper for JSON parsing. */
    private val objectMapper: ObjectMapper
) {
    @Suppress("MemberVisibilityCanBePrivate")
    /** Intermediary mappings by Minecraft version — the authority on whether Fabric supports a version. */
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
     * @throws IOException when the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    private fun listIntermediariesFromManifest() =
        objectMapper.readValue(intermediaryManifest, object : TypeReference<List<FabricIntermediary>>() {})

    /**
     * Check whether Fabric Intermediaries for the given Minecraft version are present, indicating
     * that the given Minecraft version is supported.
     */
    fun isIntermediariesPresent(minecraftVersion: String) = getIntermediary(minecraftVersion).isPresent

    /**
     * Get a specific intermediary, wrapped in an [Optional].
     *
     * @author Griefed
     */
    fun getIntermediary(minecraftVersion: String) = Optional.ofNullable(intermediaries[minecraftVersion])
}