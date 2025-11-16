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

import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.Type
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
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
    val releases: MutableList<MinecraftClient> = ArrayList(100)
    val snapshots: MutableList<MinecraftClient> = ArrayList(200)
    val allVersions: MutableList<MinecraftClient> = ArrayList(300)
    val meta = HashMap<String, MinecraftClient>(300)
    var latestRelease: MinecraftClient? = null
        private set
    var latestSnapshot: MinecraftClient? = null
        private set
    private val versions = "versions" // TODO Move tagName to property
    private val latestType = "latest" // TODO Move tagName to property
    private val releaseType = "release" // TODO Move tagName to property
    private val snapshotType = "snapshot" // TODO Move tagName to property
    private val type = "type" // TODO Move tagName to property
    private val id = "id" // TODO Move tagName to property
    private val url = "url" // TODO Move tagName to property

    /**
     * Update the meta information.
     *
     * @throws IOException if the manifest could not be read.
     * @author Griefed
     */
    @Throws(IOException::class)
    fun update() {
        releases.clear()
        snapshots.clear()
        meta.clear()
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
                    releases.add(client)
                } catch (ex: IOException) {
                    log.debug("No server available for MinecraftClient version $id", ex)
                }
            } else if (type == snapshotType) {
                try {
                    client = MinecraftClient(id, Type.SNAPSHOT, URI(url).toURL(), forgeMeta, utilities, apiProperties)
                    snapshots.add(client)
                } catch (ex: IOException) {
                    log.debug("No server available for MinecraftClient version $id", ex)
                }
            }
            if (client != null) {
                meta[client.version] = client
                allVersions.add(client)
            }
        }
        val releaseVersion = minecraftManifest.get(latestType).get(releaseType).asText()
        val releaseUrl = meta[minecraftManifest.get(latestType).get(releaseType).asText()]!!.url
        val releaseServer = meta[minecraftManifest.get(latestType).get(releaseType).asText()]!!.minecraftServer
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
        val snapshotUrl = meta[minecraftManifest.get(latestType).get(snapshotType).asText()]!!.url
        val snapshotServer = meta[minecraftManifest.get(latestType).get(snapshotType).asText()]!!.minecraftServer
        latestSnapshot = MinecraftClient(
            snapshotVersion,
            Type.SNAPSHOT,
            snapshotUrl,
            snapshotServer,
            forgeMeta,
            utilities,
            apiProperties
        )
    }
}