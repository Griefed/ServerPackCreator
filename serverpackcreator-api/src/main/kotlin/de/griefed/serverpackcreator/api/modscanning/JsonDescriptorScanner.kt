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
package de.griefed.serverpackcreator.api.modscanning

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

/**
 * Base for scanners whose descriptor is JSON: adds Jackson reading to the per-jar failure handling
 * of [DescriptorScanner], so subclasses implement [read] for a single jar and may throw freely.
 *
 * @author Griefed
 */
abstract class JsonDescriptorScanner : DescriptorScanner() {

    /**
     * Acquire a JsonNode from the specified entry in the specified jar.
     *
     * @param file         The jar from which to get the JsonNode.
     * @param entryInJar   The entry in the jar from which to get the JsonNode.
     * @param objectMapper The ObjectMapper with which to parse the JSON to a JsonNode.
     * @return A JsonNode containing all information from the requested entry in the specified jar.
     * @throws IOException           if the file could not be opened or read from, or if the JSON
     * could not be parsed into a JsonNode.
     * @throws SecurityException     if an error occurs reading the entry in the jar.
     * @throws IllegalStateException if an error occurs reading the entry in the jar.
     * @throws MissingDescriptorException if the jar does not contain the specified entry — a normal
     * outcome for a jar belonging to another loader, not a failure.
     * @author Griefed
     */
    @Throws(MissingDescriptorException::class, IOException::class, SecurityException::class, IllegalStateException::class)
    fun getJarJson(file: File, entryInJar: String, objectMapper: ObjectMapper): JsonNode {
        val jsonNode: JsonNode
        JarFile(file).use { jar ->
            val entry = jar.getJarEntry(entryInJar) ?: throw MissingDescriptorException(entryInJar, file)
            jar.getInputStream(entry).use {
                jsonNode = objectMapper.readTree(it)
            }
        }
        return jsonNode
    }
}
