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
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*

/**
 * Representation of a Minecraft server, containing information about its Minecraft-version,
 * release-type, download-url and the java-version.
 *
 * @param minecraftVersion The Minecraft version of this server.
 * @param releaseType      The release-type of this server. Either [Type.RELEASE] or [Type.SNAPSHOT].
 * @param serverUrl        The URL to the download of these servers JAR-file.
 * @param utilities        Commonly used utilities across ServerPackCreator.
 * @param apiProperties    ServerPackCreator settings.
 *
 * @author Griefed
 */
class MinecraftServer internal constructor(
    val minecraftVersion: String,
    val releaseType: Type,
    @Suppress("MemberVisibilityCanBePrivate") val serverUrl: URL,
    private val utilities: Utilities,
    apiProperties: ApiProperties
) {
    private val manifestFile: File = File(apiProperties.minecraftServerManifestsDirectory, "$minecraftVersion.json")
    private var serverJson: JsonNode? = null
    private val downloads = "downloads" // TODO Move tagName to property
    private val server = "server" // TODO Move tagName to property
    private val url = "url" // TODO Move tagName to property
    private val javaVersion = "javaVersion" // TODO Move tagName to property
    private val majorVersion = "majorVersion" // TODO Move tagName to property

    /**
     * Get the [URL] to the download of this Minecraft-servers JAR-file.
     *
     * @return URL.
     * @author Griefed
     */
    fun url() =
        try {
            if (serverJson == null) {
                setServerJson()
            }
            val dwn = serverJson?.get(downloads)
            val srv = dwn?.get(server)
            val url = srv?.get(url)?.asText()
            Optional.ofNullable(URI(url).toURL())
        } catch (e: Exception) {
            Optional.empty()
        }


    /**
     * Read and store the server manifest.
     *
     * @author Griefed
     */
    private fun setServerJson() {
        if (!manifestFile.exists()) {
            utilities.webUtilities.downloadFile(manifestFile, serverUrl)
        }
        serverJson = utilities.jsonUtilities.getJson(manifestFile)
    }

    /**
     * Get the Java-version of this Minecraft-server.
     *
     * @return Java version.
     * @author Griefed
     */
    fun javaVersion() =
        try {
            if (serverJson == null) {
                setServerJson()
            }
            val jv = serverJson?.get(javaVersion)
            val major = jv?.get(majorVersion)?.asInt()?.toByte()
            Optional.ofNullable(major)
        } catch (e: Exception) {
            Optional.empty()
        }

}