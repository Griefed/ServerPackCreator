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
package de.griefed.serverpackcreator.api.versionmeta.legacyfabric

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMetaConfig
import java.io.File
import java.io.IOException
import java.util.Collections

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
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place. The refresh runs on a background coroutine while callers read; clearing and refilling a
     * shared list let a reader throw `ConcurrentModificationException` or silently observe the empty
     * window between the two.
     */
    @Volatile
    var releases: List<String> = emptyList()
        private set
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place. The refresh runs on a background coroutine while callers read; clearing and refilling a
     * shared list let a reader throw `ConcurrentModificationException` or silently observe the empty
     * window between the two.
     */
    @Volatile
    var snapshots: List<String> = emptyList()
        private set
    /**
     * Published as an **immutable snapshot behind `@Volatile`**, not as a collection [update] mutates in
     * place. The refresh runs on a background coroutine while callers read; clearing and refilling a
     * shared list let a reader throw `ConcurrentModificationException` or silently observe the empty
     * window between the two.
     */
    @Volatile
    var allVersions: List<String> = emptyList()
        private set

    /**
     * Update all lists of available versions with new information gathered from the manifest.
     *
     * @throws IOException When the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        val next_releases = ArrayList<String>(100)
        val next_snapshots = ArrayList<String>(100)
        val next_allVersions = ArrayList<String>(200)
        for (node in utilities.jsonUtilities.getJson(manifest)) {
            val version: String = node.get(VersionMetaConfig.TAG_VERSION).asText()
            val stable = node.get(VersionMetaConfig.TAG_STABLE).asBoolean()
            next_allVersions.add(version)
            if (stable) {
                next_releases.add(version)
            } else {
                next_snapshots.add(version)
            }
        }
            // Published in one assignment each, as unmodifiable views: a `List`-typed field still
        // holds an ArrayList at runtime, so a caller could otherwise cast and mutate our state.
        releases = Collections.unmodifiableList(next_releases)
        snapshots = Collections.unmodifiableList(next_snapshots)
        allVersions = Collections.unmodifiableList(next_allVersions)
}
}
