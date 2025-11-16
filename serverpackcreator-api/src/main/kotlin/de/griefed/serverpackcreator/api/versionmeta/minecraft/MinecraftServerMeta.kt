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
package de.griefed.serverpackcreator.api.versionmeta.minecraft

/**
 * Meta containing information about Minecraft servers.
 *
 * @param minecraftClientMeta Instance of [MinecraftClientMeta].
 *
 * @author Griefed
 */
internal class MinecraftServerMeta(private val minecraftClientMeta: MinecraftClientMeta) {

    val releases: MutableList<MinecraftServer> = ArrayList(100)
    val snapshots: MutableList<MinecraftServer> = ArrayList(200)
    val meta = HashMap<String, MinecraftServer>(300)

    /**
     * Update this instance of with new information.
     *
     * @author Griefed
     */
    fun update() {
        releases.clear()
        snapshots.clear()
        meta.clear()
        for (release in minecraftClientMeta.releases) {
            releases.add(release.minecraftServer)
            meta[release.version] = release.minecraftServer
        }
        for (snapshot in minecraftClientMeta.snapshots) {
            snapshots.add(snapshot.minecraftServer)
            meta[snapshot.version] = snapshot.minecraftServer
        }
    }
}