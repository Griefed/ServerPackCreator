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

import com.fasterxml.jackson.databind.JsonNode
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.Type
import de.griefed.serverpackcreator.api.versionmeta.forge.ForgeMeta
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.net.URL

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
    private val log = cachedLoggerOf(this.javaClass)
    val releases: MutableList<MinecraftClient> = ArrayList(100)
    val snapshots: MutableList<MinecraftClient> = ArrayList(200)
    val allVersions: MutableList<MinecraftClient> = ArrayList(300)
    val meta = HashMap<String, MinecraftClient>(300)
    var latestRelease: MinecraftClient? = null
        private set
    var latestSnapshot: MinecraftClient? = null
        private set

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
        for (version in minecraftManifest.get("versions")) {
            var client: MinecraftClient? = null
            if (version.get("type").asText().lowercase() == "release") {
                try {
                    client = MinecraftClient(
                        version.get("id").asText(),
                        Type.RELEASE,
                        URL(version.get("url").asText()),
                        forgeMeta,
                        utilities,
                        apiProperties
                    )
                    releases.add(client)
                } catch (ex: IOException) {
                    log.debug(
                        "No server available for MinecraftClient version "
                                + version.get("id").asText(),
                        ex
                    )
                }
            } else if (version.get("type").asText().lowercase() == "snapshot") {
                try {
                    client = MinecraftClient(
                        version.get("id").asText(),
                        Type.SNAPSHOT,
                        URL(version.get("url").asText()),
                        forgeMeta,
                        utilities,
                        apiProperties
                    )
                    snapshots.add(client)
                } catch (ex: IOException) {
                    log.debug(
                        "No server available for MinecraftClient version "
                                + version.get("id").asText(),
                        ex
                    )
                }
            }
            if (client != null) {
                meta[client.version] = client
                allVersions.add(client)
            }
        }
        latestRelease = MinecraftClient(
            minecraftManifest.get("latest").get("release").asText(),
            Type.RELEASE,
            meta[minecraftManifest.get("latest").get("release").asText()]!!.url,
            meta[minecraftManifest.get("latest").get("release").asText()]!!.minecraftServer,
            forgeMeta,
            utilities,
            apiProperties
        )
        latestSnapshot = MinecraftClient(
            minecraftManifest.get("latest").get("snapshot").asText(),
            Type.SNAPSHOT,
            meta[minecraftManifest.get("latest").get("snapshot").asText()]!!.url,
            meta[minecraftManifest.get("latest").get("snapshot").asText()]!!.minecraftServer,
            forgeMeta,
            utilities,
            apiProperties
        )
    }
}