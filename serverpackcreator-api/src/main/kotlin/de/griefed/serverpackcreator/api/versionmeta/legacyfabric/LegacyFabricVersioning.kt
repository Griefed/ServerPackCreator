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
package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import java.io.File
import java.io.IOException

/**
 * LegacyFabric version parent-class, implemented by [LegacyFabricGame] and
 * [LegacyFabricLoader] to store and provide version information for game and loader
 * versions.
 *
 * @param manifest  The manifest holding the version information for this LegacyFabric type.
 * @param utilities Commonly used utilities across ServerPackCreator.
 * @author Griefed
 */
internal abstract class LegacyFabricVersioning(
    private val manifest: File,
    private val utilities: Utilities
) {
    val releases: MutableList<String> = ArrayList(100)
    val snapshots: MutableList<String> = ArrayList(100)
    val allVersions: MutableList<String> = ArrayList(200)

    /**
     * Update all lists of available versions with new information gathered from the manifest.
     *
     * @throws IOException When the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        releases.clear()
        snapshots.clear()
        allVersions.clear()
        for (node in utilities.jsonUtilities.getJson(manifest)) {
            val version: String = node.get("version").asText() // TODO Move tagName to property
            val stable = node.get("stable").asBoolean() // TODO Move tagName to property
            allVersions.add(version)
            if (stable) {
                releases.add(version)
            } else {
                snapshots.add(version)
            }
        }
    }
}