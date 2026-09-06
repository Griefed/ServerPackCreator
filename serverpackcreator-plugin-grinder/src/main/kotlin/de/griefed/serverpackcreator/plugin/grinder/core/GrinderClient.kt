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
package de.griefed.serverpackcreator.plugin.grinder.core

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The outcome of one request to a grinder. A sealed result rather than an exception because every caller
 * — a Swing worker painting a tab, a pre-generation extension in the middle of building a server pack —
 * has something better to do with a failure than propagate it.
 *
 * @author Griefed
 */
sealed interface FetchResult<out T> {

    /**
     * The grinder answered and the answer parsed.
     *
     * @param value What was read.
     */
    data class Ok<T>(val value: T) : FetchResult<T>

    /**
     * Nothing usable came back.
     *
     * @param reason A sentence fit to show a user, naming what went wrong rather than where.
     */
    data class Failed(val reason: String) : FetchResult<Nothing>
}

/**
 * Reads a grinder's report endpoints over HTTP.
 *
 * Responses are walked as a JSON **tree** rather than bound to a class, which is what lets a plugin keep
 * working against a daemon newer than itself: a field nobody here has heard of is simply not read. The
 * two endpoints it knows are `/verdicts.json` and `/status`, both derived through [GrinderUrl].
 *
 * @param objectMapper The mapper to parse with. ServerPackCreator hands its own to every extension, so
 *                     the plugin adds no JSON dependency of its own.
 * @param timeout How long a single request may take, applied to both connect and response.
 * @param httpClient The client to send with; injectable so a test can shorten its connect timeout.
 *
 * @author Griefed
 */
class GrinderClient(
    private val objectMapper: ObjectMapper,
    private val timeout: Duration = Duration.ofSeconds(15),
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(timeout).build()
) {

    /**
     * Every verdict the grinder at [baseUrl] holds, newest state included.
     *
     * The request is unfiltered on purpose: both list tabs are populated from one response, so filtering
     * server-side would cost a second round-trip to learn the same rows.
     */
    fun fetchVerdicts(baseUrl: String): FetchResult<List<GrinderVerdict>> =
        fetchTree(baseUrl, GrinderUrl::verdicts).let { result ->
            when (result) {
                is FetchResult.Failed -> result
                is FetchResult.Ok -> FetchResult.Ok(readVerdicts(result.value))
            }
        }

    /** The status document of the grinder at [baseUrl], as a tree — the Dashboard reads it field by field. */
    fun fetchStatus(baseUrl: String): FetchResult<JsonNode> = fetchTree(baseUrl, GrinderUrl::status)

    /**
     * One GET, resolved through [GrinderUrl] and parsed as a tree. Everything that can go wrong between
     * an unusable address and an unparseable body ends up as a [FetchResult.Failed] carrying a reason;
     * nothing escapes as an exception.
     */
    private fun fetchTree(baseUrl: String, endpoint: (String) -> String): FetchResult<JsonNode> {
        val base = GrinderUrl.normalise(baseUrl)
            ?: return FetchResult.Failed("No usable grinder URL configured. Set one in the Settings tab.")
        val target = endpoint(base)
        return try {
            val request = HttpRequest.newBuilder(URI.create(target))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                // The status is the one fact that tells an operator whether the daemon is down, the path
                // is wrong, or something in front of it answered instead.
                FetchResult.Failed("$target answered HTTP ${response.statusCode()}.")
            } else {
                FetchResult.Ok(objectMapper.readTree(response.body()))
            }
        } catch (ex: InterruptedException) {
            // Restoring the flag matters: this runs on a SwingWorker, whose cancellation is an interrupt.
            Thread.currentThread().interrupt()
            FetchResult.Failed("Request to $target was interrupted.")
        } catch (ex: Exception) {
            FetchResult.Failed("Could not read $target: ${ex.message ?: ex.javaClass.simpleName}")
        }
    }

    /**
     * The rows out of a feed document. Both the wrapped shape (`{"verdicts":[…]}`) and a bare array are
     * accepted, so the plugin survives a daemon that publishes the list without its paging envelope.
     * A row missing its slug is dropped rather than rendered nameless — it could not be acted on anyway.
     */
    private fun readVerdicts(document: JsonNode): List<GrinderVerdict> {
        val rows = when {
            document.isArray -> document
            document["verdicts"]?.isArray == true -> document["verdicts"]
            else -> return emptyList()
        }
        return rows.mapNotNull { row ->
            val slug = row.textOrNull("slug") ?: return@mapNotNull null
            GrinderVerdict(
                slug = slug,
                projectUrl = row.textOrNull("projectUrl").orEmpty(),
                platform = row.textOrNull("platform").orEmpty(),
                loader = row.textOrNull("loader").orEmpty(),
                verdict = row.textOrNull("verdict").orEmpty(),
                suggestedEntry = row.textOrNull("suggestedEntry"),
                filenamePattern = row.textOrNull("filenamePattern"),
                detail = row.textOrNull("detail").orEmpty(),
                scannedAt = row.textOrNull("verifiedAt").orEmpty()
            )
        }
    }

    /**
     * The textual value of [field], or `null` when it is absent or JSON `null`. Jackson's `asText()`
     * renders a null node as the string `"null"`, which would put the word "null" in a table cell and,
     * worse, make an absent entry look tickable.
     */
    private fun JsonNode.textOrNull(field: String): String? =
        this[field]?.takeIf { it.isTextual }?.asText()
}
