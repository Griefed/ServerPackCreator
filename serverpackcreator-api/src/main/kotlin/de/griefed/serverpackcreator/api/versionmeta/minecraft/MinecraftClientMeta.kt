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

import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.Type
import de.griefed.serverpackcreator.api.versionmeta.VersionMetaConfig
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.util.Collections
import java.net.URI

/**
 * Minecraft client meta containing information about available Minecraft releases.
 *
 * @param forgeMeta         To acquire Forge instances for this [MinecraftClient] version.
 * @param minecraftManifest Minecraft manifest file.
 * @param utilities         Commonly used utilities across ServerPackCreator.
 * @param apiProperties     ServerPackCreator settings.
 *
 * @author Griefed
 */
internal class MinecraftClientMeta(
    private val minecraftManifest: File,
    private val forgeMeta: ForgeMeta,
    private val utilities: Utilities,
    private val apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    /**
     * Published as **immutable snapshots behind `@Volatile`**, not as collections [update] mutates in place.
     *
     * The refresh runs on a background coroutine while callers read; clearing and refilling a shared list
     * let a reader throw `ConcurrentModificationException` or silently observe the empty window between the
     * two. Publishing a finished list in one assignment means a reader sees the whole previous state or the
     * whole next one.
     */
    @Volatile
    var releases: List<MinecraftClient> = emptyList()
        private set

    @Volatile
    var snapshots: List<MinecraftClient> = emptyList()
        private set

    /**
     * Every version, release and snapshot alike.
     *
     * **This one also used to grow without bound**: [update] cleared `releases`, `snapshots` and `meta` but
     * never `allVersions`, so each refresh appended the whole manifest again. Building a fresh list per
     * update fixes that as a side effect of fixing the race.
     */
    @Volatile
    var allVersions: List<MinecraftClient> = emptyList()
        private set

    @Volatile
    var meta: Map<String, MinecraftClient> = emptyMap()
        private set
    var latestRelease: MinecraftClient? = null
        private set
    var latestSnapshot: MinecraftClient? = null
        private set
    private val versions = VersionMetaConfig.TAG_VERSIONS
    private val latestType = VersionMetaConfig.TAG_LATEST
    private val releaseType = VersionMetaConfig.TAG_RELEASE
    private val snapshotType = VersionMetaConfig.TAG_SNAPSHOT
    private val type = VersionMetaConfig.TAG_TYPE
    private val id = VersionMetaConfig.TAG_ID
    private val url = VersionMetaConfig.TAG_URL

    /**
     * Update the meta information.
     *
     * @throws IOException if the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        val nextReleases = ArrayList<MinecraftClient>(100)
        val nextSnapshots = ArrayList<MinecraftClient>(200)
        val nextAllVersions = ArrayList<MinecraftClient>(300)
        val nextMeta = HashMap<String, MinecraftClient>(300)
        val minecraftManifest: JsonNode = utilities.jsonUtilities.getJson(minecraftManifest)
        val versions = minecraftManifest.get(versions)
        for (version in versions) {
            var client: MinecraftClient? = null
            val type = version.get(type).asText().lowercase()
            val id = version.get(id).asText()
            val url = version.get(url).asText()
            if (type == releaseType) {
                try {
                    client = MinecraftClient(id, Type.RELEASE, URI(url).toURL(), forgeMeta, utilities, apiProperties)
                    nextReleases.add(client)
                } catch (ex: IOException) {
                    log.debug("No server available for MinecraftClient version $id", ex)
                }
            } else if (type == snapshotType) {
                try {
                    client = MinecraftClient(id, Type.SNAPSHOT, URI(url).toURL(), forgeMeta, utilities, apiProperties)
                    nextSnapshots.add(client)
                } catch (ex: IOException) {
                    log.debug("No server available for MinecraftClient version $id", ex)
                }
            }
            if (client != null) {
                nextMeta[client.version] = client
                nextAllVersions.add(client)
            }
        }
        val releaseVersion = minecraftManifest.get(latestType).get(releaseType).asText()
        val releaseUrl = nextMeta[minecraftManifest.get(latestType).get(releaseType).asText()]!!.url
        val releaseServer = nextMeta[minecraftManifest.get(latestType).get(releaseType).asText()]!!.minecraftServer
        latestRelease = MinecraftClient(
            releaseVersion,
            Type.RELEASE,
            releaseUrl,
            releaseServer,
            forgeMeta,
            utilities,
            apiProperties
        )
        val snapshotVersion = minecraftManifest.get(latestType).get(snapshotType).asText()
        val snapshotUrl = nextMeta[minecraftManifest.get(latestType).get(snapshotType).asText()]!!.url
        val snapshotServer = nextMeta[minecraftManifest.get(latestType).get(snapshotType).asText()]!!.minecraftServer
        latestSnapshot = MinecraftClient(
            snapshotVersion,
            Type.SNAPSHOT,
            snapshotUrl,
            snapshotServer,
            forgeMeta,
            utilities,
            apiProperties
        )
        // Published last, and each in one assignment: a reader sees the whole previous state or the whole
        // next one, never a list being refilled.
        releases = Collections.unmodifiableList(nextReleases)
        snapshots = Collections.unmodifiableList(nextSnapshots)
        allVersions = Collections.unmodifiableList(nextAllVersions)
        meta = Collections.unmodifiableMap(nextMeta)
    }
}
