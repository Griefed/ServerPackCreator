/* Copyright (C) 2023  Griefed
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
actual class MinecraftMeta(
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
    actual fun isMinecraftVersionAvailable(minecraftVersion: String): Boolean {
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
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * descending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * descending order.
     * @author Griefed
     */
    actual fun releaseVersionsArrayDescending(): Array<String> {
        return releaseVersionsDescending().toTypedArray()
    }

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * descending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * descending order.
     * @author Griefed
     */
    actual fun releaseVersionsDescending(): List<String> {
        val list: MutableList<String> = ArrayList(100)
        for (client in releasesDescending()) {
            list.add(client.version)
        }
        return list
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in ascending
     * order.
     *
     * @return Release client-list
     * @author Griefed
     */
    fun releasesDescending(): List<MinecraftClient> {
        return minecraftClientMeta.releases
    }

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * ascending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in
     * ascending order.
     * @author Griefed
     */
    actual fun releaseVersionsArrayAscending(): Array<String> {
        return releaseVersionsAscending().toTypedArray()
    }

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] in
     * ascending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] versions in ascending
     * order.
     * @author Griefed
     */
    actual fun releaseVersionsAscending(): List<String> {
        val list: MutableList<String> = ArrayList(100)
        for (client in releasesDescending()) {
            list.add(client.version)
        }
        return list.reversed()
    }

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * descending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * descending order.
     * @author Griefed
     */
    actual fun snapshotVersionsArrayDescending(): Array<String> {
        return snapshotVersionsDescending().toTypedArray()
    }

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * descending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * descending order.
     * @author Griefed
     */
    actual fun snapshotVersionsDescending(): List<String> {
        val list: MutableList<String> = ArrayList(100)
        for (client in snapshotsDescending()) {
            list.add(client.version)
        }
        return list
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in ascending
     * order.
     *
     * @return Snapshot client-list
     * @author Griefed
     */
    fun snapshotsDescending(): List<MinecraftClient> {
        return minecraftClientMeta.snapshots
    }

    /**
     * Get the array of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * ascending order.
     *
     * @return Array of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * ascending order.
     * @author Griefed
     */
    actual fun snapshotVersionsArrayAscending(): Array<String> {
        return snapshotVersionsAscending().toTypedArray()
    }

    /**
     * Get the list of available Minecraft version of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in
     * ascending order.
     *
     * @return List of all available Minecraft [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] versions in
     * ascending order.
     * @author Griefed
     */
    actual fun snapshotVersionsAscending(): List<String> {
        val list: MutableList<String> = ArrayList(100)
        for (client in snapshotsDescending()) {
            list.add(client.version)
        }
        return list.reversed()
    }

    /**
     * Get an array of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in descending order.
     *
     * @return All available Minecraft versions in descending order.
     * @author Griefed
     */
    actual fun allVersionsArrayDescending(): Array<String> {
        return allVersionsDescending().toTypedArray()
    }
    /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................CLIENTS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */
    /**
     * Get a list of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in descending order.
     *
     * @return All available Minecraft versions in descending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    actual fun allVersionsDescending(): List<String> {
        val versions: MutableList<String> = ArrayList(100)
        for (client in allDescending()) {
            versions.add(client.version)
        }
        return versions
    }

    /**
     * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
     * descending order.
     *
     * @return All available Minecraft releases in descending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun allDescending(): List<MinecraftClient> {
        return minecraftClientMeta.allVersions
    }

    /**
     * Get an array of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in ascending order.
     *
     * @return All available Minecraft versions in ascending order.
     * @author Griefed
     */
    @Suppress("unused")
    actual fun allVersionsArrayAscending(): Array<String> {
        return allVersionsAscending().toTypedArray()
    }

    /**
     * Get a list of all available Minecraft versions of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE] and
     * [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT] in ascending order.
     *
     * @return All available Minecraft versions in ascending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    actual fun allVersionsAscending(): List<String> {
        val versions: MutableList<String> = ArrayList(releaseVersionsAscending())
        for (client in allAscending()) {
            versions.add(client.version)
        }
        return versions
    }

    /**
     * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
     * ascending order.
     *
     * @return All available Minecraft releases in ascending order.
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun allAscending(): List<MinecraftClient> {
        return minecraftClientMeta.allVersions.reversed()
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
     * Get an array of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in ascending
     * order.
     *
     * @return Release client-array
     * @author Griefed
     */
    fun releasesArrayDescending(): Array<MinecraftClient> {
        return releasesDescending().toTypedArray()
    }

    /**
     * Get an array of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * descending order.
     *
     * @return Release client-array
     * @author Griefed
     */
    fun releasesArrayAscending(): Array<MinecraftClient> {
        return releasesAscending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in descending
     * order.
     *
     * @return Release client-list
     * @author Griefed
     */
    fun releasesAscending(): List<MinecraftClient> {
        return minecraftClientMeta.releases.reversed()
    }

    /**
     * Get an array of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * ascending order.
     *
     * @return Snapshot client-array
     * @author Griefed
     */
    fun snapshotsArrayDescending(): Array<MinecraftClient> {
        return snapshotsDescending().toTypedArray()
    }

    /**
     * Get an array of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * descending order.
     *
     * @return Snapshot client-array
     * @author Griefed
     */
    fun snapshotsArrayAscending(): Array<MinecraftClient> {
        return snapshotsAscending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftClient] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in descending
     * order.
     *
     * @return Snapshot client-list
     * @author Griefed
     */
    fun snapshotsAscending(): List<MinecraftClient> {
        return minecraftClientMeta.snapshots.reversed()
    }

    /**
     * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
     * descending order.
     *
     * @return All available Minecraft releases in descending order.
     * @author Griefed
     */
    @Suppress("unused")
    fun allDescendingArray(): Array<MinecraftClient> {
        return allDescending().toTypedArray()
    }

    /**
     * Get all available Minecraft releases, both releases and pre-releases or snapshots, in
     * ascending order.
     *
     * @return All available Minecraft releases in ascending order.
     * @author Griefed
     */
    @Suppress("unused")
    fun allAscendingArray(): Array<MinecraftClient> {
        return allAscending().toTypedArray()
    }
    /*
   * #==============================================================================================================#
   * #..............................................................................................................#
   * #...................................................SERVERS....................................................#
   * #..............................................................................................................#
   * #==============================================================================================================#
   */
    /**
     * Check whether a [MinecraftServer] is available for the specified Minecraft-version.
     *
     * @param minecraftVersion The Minecraft-version.
     * @return `true` if a [MinecraftServer] is available.
     * @author Griefed
     */
    actual fun isServerAvailable(minecraftVersion: String): Boolean {
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
     * Get an array of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * descending order.
     *
     * @return Server-array
     * @author Griefed
     */
    fun releasesServersArrayDescending(): Array<MinecraftServer> {
        return releasesServersDescending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * descending order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun releasesServersDescending(): List<MinecraftServer> {
        return minecraftServerMeta.releases
    }

    /**
     * Get an array of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * ascending order.
     *
     * @return Server-array
     * @author Griefed
     */
    fun releasesServersArrayAscending(): Array<MinecraftServer> {
        return releasesServersAscending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.RELEASE], in
     * ascending order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun releasesServersAscending(): List<MinecraftServer> {
        return minecraftServerMeta.releases.reversed()
    }

    /**
     * Get an array of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * ascending order.
     *
     * @return Server-array
     * @author Griefed
     */
    fun snapshotsServersArrayDescending(): Array<MinecraftServer> {
        return snapshotsServersDescending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * descending order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun snapshotsServersDescending(): List<MinecraftServer> {
        return minecraftServerMeta.snapshots
    }

    /**
     * Get an array of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in
     * descending order.
     *
     * @return Server-array
     * @author Griefed
     */
    fun snapshotsServersArrayAscending(): Array<MinecraftServer> {
        return snapshotsServersAscending().toTypedArray()
    }

    /**
     * Get a list of all available [MinecraftServer] of the [de.griefed.serverpackcreator.api.versionmeta.Type.SNAPSHOT], in descending
     * order.
     *
     * @return Server-list
     * @author Griefed
     */
    fun snapshotsServersAscending(): List<MinecraftServer> {
        return minecraftServerMeta.snapshots.reversed()
    }

    /**
     * Depending on whether *de.griefed.serverpackcreator.minecraft.snapshots*-property is set to *true|false* this will return
     * either [allVersionsArrayDescending] or [releaseVersionsArrayDescending].
     */
    fun settingsDependantVersionsArrayDescending(): Array<String> {
        return if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            allVersionsArrayDescending()
        } else {
            releaseVersionsArrayDescending()
        }
    }

    /**
     * Depending on whether *de.griefed.serverpackcreator.minecraft.snapshots*-property is set to *true|false* this will return
     * either [allVersionsArrayAscending] or [releaseVersionsArrayAscending].
     */
    fun settingsDependantVersionsArrayAscending(): Array<String> {
        return if (apiProperties.isMinecraftPreReleasesAvailabilityEnabled) {
            allVersionsArrayAscending()
        } else {
            releaseVersionsArrayAscending()
        }
    }
}