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
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant
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
    apiProperties: ApiProperties,
    private val downloadCooldown: Duration = Duration.ofHours(1),
    private val clock: () -> Instant = Instant::now
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val manifestFile: File = File(apiProperties.minecraftServerManifestsDirectory, "$minecraftVersion.json")
    private var serverJson: JsonNode? = null

    /**
     * When this version's manifest download was last attempted, or `null` if never. A failed
     * [Utilities.webUtilities] download deletes the partial file, so without this every lookup re-attempts -- and
     * each attempt logs an ERROR with a stack trace. Gates the *download* only; see [readServerJson].
     */
    private var lastDownloadAttempt: Instant? = null
    private val downloads = VersionMetaConfig.TAG_DOWNLOADS
    private val server = VersionMetaConfig.TAG_SERVER
    private val url = VersionMetaConfig.TAG_URL
    private val javaVersion = VersionMetaConfig.TAG_JAVA_VERSION
    private val majorVersion = VersionMetaConfig.TAG_MAJOR_VERSION

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
            // An unreadable or undownloadable manifest is indistinguishable from "no server URL declared" to a
            // caller, so leave a trace of which one happened. Callers treat the empty Optional as "no server
            // available". DEBUG, message-only: see javaVersion() for why this must not be warn-with-stacktrace.
            log.debug("No server download URL for Minecraft $minecraftVersion in $manifestFile: ${e.javaClass.simpleName}")
            Optional.empty()
        }


    /**
     * Read and store the server manifest.
     *
     * @author Griefed
     */
    private fun setServerJson() {
        readServerJson()
    }

    /**
     * Resolve this version's manifest, caching it once read. A manifest already on disk is always read -- that costs
     * nothing and cannot fail for the reason a download does. A **download** is attempted only when the file is
     * absent *and* the last attempt is older than [downloadCooldown], because a failed download deletes the file and
     * would otherwise be retried by every single lookup: `getServer` consults both [url] and [javaVersion], and that
     * pair runs per candidate in the grinder, per cell in the template matrix and per GUI version selection.
     *
     * A cooldown rather than a permanent memory on purpose -- the grinder runs for days, and a transient network
     * failure must not write a version off for the life of the process. Same shape as `LoaderCache.failureCooldown`.
     */
    private fun readServerJson() {
        if (!manifestFile.exists()) {
            val lastAttempt = lastDownloadAttempt
            if (lastAttempt != null && Duration.between(lastAttempt, clock()) < downloadCooldown) {
                log.debug(
                    "Not re-attempting the manifest download for Minecraft $minecraftVersion: the last attempt " +
                        "failed less than $downloadCooldown ago."
                )
                return
            }
            lastDownloadAttempt = clock()
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
            // The empty Optional here is what every consumer reads as "this version declares no required Java",
            // which is also what a *failed manifest download* produces -- and downstream that became a
            // benign-looking "not applicable" that silently dropped the newest Minecraft versions from the
            // template matrix. The Optional contract is exported, so it stays; the cause leaves a trace instead
            // of vanishing. See ImageSupport.REQUIREMENT_UNKNOWN for the consumer-side half.
            //
            // DEBUG and message-only, deliberately. This is a hot path on a *repeating* failure: setServerJson()
            // does not remember a failed fetch, and getServer() calls both url() and javaVersion(), so one
            // requiredJavaVersion lookup on a broken version costs two attempts -- and that lookup runs per
            // candidate in the grinder, per cell in the template matrix, and on every GUI version selection. At
            // warn-with-stacktrace that is a log flood; the exception type carries the diagnosis without it.
            log.debug("No required Java version for Minecraft $minecraftVersion in $manifestFile: ${e.javaClass.simpleName}")
            Optional.empty()
        }

}
