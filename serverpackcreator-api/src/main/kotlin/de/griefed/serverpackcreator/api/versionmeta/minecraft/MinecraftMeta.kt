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
@file:Suppress("unused")

package de.griefed.serverpackcreator.api.versionmeta.minecraft

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Minecraft meta containing information about available client- and server releases.
 *
 * @param minecraftManifest Minecraft manifest file.
 * @param injectedForgeMeta To acquire Forge instances for this [MinecraftClient] version.
 * @param utilities         Commonly used utilities across ServerPackCreator.
 * @param apiProperties     ServerPackCreator settings.
 *
 * @author Griefed
 */
class MinecraftMeta(
    minecraftManifest: File,
    injectedForgeMeta: ForgeMeta,
    utilities: Utilities,
    private val apiProperties: ApiProperties
) {
    private val minecraftClientMeta: MinecraftClientMeta = MinecraftClientMeta(
        minecraftManifest,
        injectedForgeMeta,
        utilities,
        apiProperties
    )
    private val minecraftServerMeta: MinecraftServerMeta = MinecraftServerMeta(minecraftClientMeta)

    /**
     * Update the [MinecraftClientMeta] and [MinecraftServerMeta]. Usually called after
     * the manifest-files have been refreshed.
     *
     * @throws IOException if the [MinecraftClientMeta] could not be initialized.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        minecraftClientMeta.update()
        minecraftServerMeta.update()
    }
    /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................STRINGS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */
    /**
     * Check whether a [MinecraftClient] is available for the specified Minecraft-version.
     *
     * @param minecraftVersion The Minecraft-version.
     * @return `true` if a [MinecraftClient] is available.
     * @author Griefed
     */
    fun isMinecraftVersionAvailable(minecraftVersion: String): Boolean {
        return getClient(minecraftVersion).isPresent
    }

    /**
     * Get a specific Minecraft client as a [MinecraftClient] wrapped in an [Optional].
     *
     * @param minecraftVersion Minecraft version.
     * @return Client wrapped in an [Optional]
     * @author Griefed
     */
    fun getClient(minecraftVersion: String): Optional<MinecraftClient> {
        return Optional.ofNullable(minecraftClientMeta.meta[minecraftVersion])
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in ascending
     * order.
     *
     * @return Release client-list
     * @author Griefed
     */
    fun clientReleases(): List<MinecraftClient> {
        return minecraftClientMeta.releases
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in ascending
     * order.
     *
     * @return Snapshot client-list
     * @author Griefed
     */
    fun clientSnapshots(): List<MinecraftClient> {
        return minecraftClientMeta.snapshots
    }

    /**
     * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
     * descending order.
     *
     * @return All available Minecraft releases in descending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun allVersions(): List<MinecraftClient> {
        return minecraftClientMeta.allVersions
    }

    /**
     * Get the latest Minecraft release as a [MinecraftClient].
     *
     * @return Latest Minecraft release.
     * @author Griefed
     */
    fun latestRelease(): MinecraftClient {
        return minecraftClientMeta.latestRelease!!
    }

    /**
     * Get the latest Minecraft snapshot as a [MinecraftClient].
     *
     * @return Latest Minecraft snapshot.
     * @author Griefed
     */
    fun latestSnapshot(): MinecraftClient {
        return minecraftClientMeta.latestSnapshot!!
    }

    /**
     * Check whether a [MinecraftServer] is available for the specified Minecraft-version.
     *
     * @param minecraftVersion The Minecraft-version.
     * @return `true` if a [MinecraftServer] is available.
     * @author Griefed
     */
    fun isServerAvailable(minecraftVersion: String): Boolean {
        return getServer(minecraftVersion).isPresent
    }

    /**
     * Get a specific [MinecraftServer] for the specified Minecraft-version, wrapped in an
     * [Optional].
     *
     * @param minecraftVersion The Minecraft-version.
     * @return Server wrapped in an [Optional]
     * @author Griefed
     */
    fun getServer(minecraftVersion: String): Optional<MinecraftServer> {
        try {
            val server = minecraftServerMeta.meta[minecraftVersion]!!
            if (server.url().isPresent && server.javaVersion().isPresent) {
                return Optional.ofNullable(minecraftServerMeta.meta[minecraftVersion])
            }
        } catch (ignored: Exception) {
        }
        return Optional.empty()
    }

    /**
     * Get the latest [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], wrapped in an
     * [Optional].
     *
     * @return Server wrapped in an [Optional]
     * @author Griefed
     */
    fun latestReleaseServer(): MinecraftServer {
        return minecraftClientMeta.latestRelease!!.minecraftServer
    }

    /**
     * Get the latest [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], wrapped in an
     * [Optional].
     *
     * @return Server wrapped in an [Optional]
     * @author Griefed
     */
    fun latestSnapshotServer(): MinecraftServer {
        return minecraftClientMeta.latestSnapshot!!.minecraftServer
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * descending order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun serverReleases(): List<MinecraftServer> {
        return minecraftServerMeta.releases
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * descending order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun serverSnapshots(): List<MinecraftServer> {
        return minecraftServerMeta.snapshots
    }

    /**
     * Depending on whether *de.griefed.serverpackcreator.minecraft.snapshots*-property is set to *true|false* this will return
     * either [allVersions] or [clientReleases].
     */
    fun settingsDependantVersions(): Array<String> {
        return if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            allVersions().map { it.version }.toTypedArray()
        } else {
            clientReleases().map { it.version }.toTypedArray()
        }
    }
}