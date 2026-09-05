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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

/**
 * Meta containing information about Minecraft servers.
 *
 * @param minecraftClientMeta Instance of [MinecraftClientMeta].
 *
 * @author Griefed
 */
import java.util.Collections

internal class MinecraftServerMeta(private val minecraftClientMeta: MinecraftClientMeta) {

    /**
     * Published as **immutable snapshots behind `@Volatile`**, not as collections [update] mutates in place.
     *
     * `VersionMeta` refreshes on a background coroutine while callers read, and the previous shape —
     * `clear()` then re-`add()` on a shared `ArrayList` handed straight to callers — let a reader either
     * throw `ConcurrentModificationException` or, worse, silently observe the empty window between the two.
     * An empty release list makes `BootVerifier.bootableCombination()` refuse every candidate. Swapping a
     * finished list into a volatile field means a reader sees the whole previous state or the whole next
     * one, with no lock on the hot read path.
     */
    @Volatile
    var releases: List<MinecraftServer> = emptyList()
        private set

    @Volatile
    var snapshots: List<MinecraftServer> = emptyList()
        private set

    @Volatile
    var meta: Map<String, MinecraftServer> = emptyMap()
        private set

    /**
     * Update this instance with new information.
     *
     * Builds fresh collections and publishes them in one assignment each; nothing a caller already holds is
     * touched.
     *
     * @author Griefed
     */
    fun update() {
        val nextReleases = ArrayList<MinecraftServer>(100)
        val nextSnapshots = ArrayList<MinecraftServer>(200)
        val nextMeta = HashMap<String, MinecraftServer>(300)
        for (release in minecraftClientMeta.releases) {
            nextReleases.add(release.minecraftServer)
            nextMeta[release.version] = release.minecraftServer
        }
        for (snapshot in minecraftClientMeta.snapshots) {
            nextSnapshots.add(snapshot.minecraftServer)
            nextMeta[snapshot.version] = snapshot.minecraftServer
        }
        // Unmodifiable views, not the builders themselves: a `List`-typed field still holds an ArrayList at
        // runtime, so a caller could cast and mutate the metadata's own state. The wrapper makes the
        // snapshot a snapshot in fact and not merely in the type.
        releases = Collections.unmodifiableList(nextReleases)
        snapshots = Collections.unmodifiableList(nextSnapshots)
        meta = Collections.unmodifiableMap(nextMeta)
    }
}