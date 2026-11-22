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

    /** The grinder answered and the answer parsed. */
    data class Ok<T>(
        /** What was read. */
        val value: T
    ) : FetchResult<T>

    /** Nothing usable came back. */
    data class Failed(
        /** A sentence fit to show a user, naming what went wrong rather than where. */
        val reason: String
    ) : FetchResult<Nothing>
}

/**
 * Reads a grinder's report endpoints over HTTP.
 *
 * Responses are walked as a JSON **tree** rather than bound to a class, which is what lets a plugin keep
 * working against a daemon newer than itself: a field nobody here has heard of is simply not read. The
 * two endpoints it knows are `/verdicts.json` and `/status`, both derived through [GrinderUrl].
 *
 * @author Griefed
 */
class GrinderClient(
    /**
     * The mapper to parse with. ServerPackCreator hands its own to every extension, so the plugin adds no
     * JSON dependency of its own.
     */
    private val objectMapper: ObjectMapper,
    /** How long a single request may take, applied to both connect and response. */
    private val timeout: Duration = Duration.ofSeconds(15),
    /** The client to send with; injectable so a test can shorten its connect timeout. */
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(timeout).build(),
    /** Rows per request when paging the feed. */
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    /** The most pages one [fetchVerdicts] will request; injected so a test needs no thousand round trips. */
    private val maxPages: Int = MAX_PAGES
) {

    /**
     * Every verdict the grinder at [baseUrl] holds, each in the newest state served, read a page at a time.
     *
     * The request is unfiltered on purpose: both list tabs are populated from one response, so filtering
     * server-side would cost a second round-trip to learn the same rows. It is **paged**, though, because
     * a bare `/verdicts.json` returns the whole store by documented design, which can be far too large to
     * receive inside one [timeout].
     *
     * Paging is by offset over a selection the daemon re-derives per request while grinds record into it, so a
     * verdict that moves between two requests can land on two pages or on none. The first is absorbed by
     * keying rows on their identity, the later copy winning. The second shows as fewer rows than the daemon's
     * `matched`: the feed is then read once more, and a second shortfall fails the call rather than return a
     * short list a maintainer would tick exclusions from. What this cannot detect is a row deleted ahead of the
     * cursor, which hides one row behind it while the count still agrees; deletions happen only when a project
     * is re-ground under a new key, and the next refresh shows the row again.
     *
     * A page that fails, or a feed past [maxPages], fails the whole call.
     */
    fun fetchVerdicts(baseUrl: String): FetchResult<List<GrinderVerdict>> {
        val first = readFeed(baseUrl)
        if (first !is FetchResult.Ok || !first.value.isShort) {
            return first.map { it.rows }
        }
        return when (val second = readFeed(baseUrl)) {
            is FetchResult.Failed -> second
            is FetchResult.Ok -> if (second.value.isShort) {
                FetchResult.Failed(
                    "The grinder's verdicts changed while they were being read (${second.value.rows.size} of " +
                        "${second.value.matched} arrived), twice. Refresh again once the grinder is less busy."
                )
            } else {
                FetchResult.Ok(second.value.rows)
            }
        }
    }

    /** One full read of the feed: its rows, and the `matched` the last page reported, when it reported one. */
    private data class Feed(
        val rows: List<GrinderVerdict>,
        val matched: Int?
    ) {
        /** Whether fewer rows arrived than the daemon said it matched, the sign of a row skipped by a shift. */
        val isShort: Boolean get() = matched != null && rows.size < matched
    }

    private fun <T, R> FetchResult<T>.map(transform: (T) -> R): FetchResult<R> = when (this) {
        is FetchResult.Ok -> FetchResult.Ok(transform(value))
        is FetchResult.Failed -> this
    }

    /** Page through the feed once; see [fetchVerdicts]. */
    private fun readFeed(baseUrl: String): FetchResult<Feed> {
        val collected = LinkedHashMap<RowIdentity, GrinderVerdict>()
        var page = 1
        while (true) {
            val result = fetchTree(baseUrl) { base -> GrinderUrl.verdicts(base, page, pageSize) }
            val document = when (result) {
                is FetchResult.Failed -> return result
                is FetchResult.Ok -> result.value
            }
            // A wrong-shaped document is a failure, not an empty result: reported as "no verdicts found"
            // it is indistinguishable from a grinder that has genuinely ground nothing, and that is
            // exactly the pair an operator debugging an empty tab needs told apart.
            val rows = readVerdicts(document)
                ?: return FetchResult.Failed("The grinder answered with something that is not a verdict document.")
            rows.forEach { collected[RowIdentity.of(it)] = it }
            val matched = document["matched"]?.takeIf { it.isInt }?.asInt()

            // No envelope means no paging to do: a daemon publishing the bare array has served everything
            // it is going to, and asking for a second page would re-request the same rows for ever.
            val pages = document["pages"]?.takeIf { it.isInt }?.asInt()
                ?: return FetchResult.Ok(Feed(collected.values.toList(), matched))
            if (page >= pages || rows.isEmpty()) {
                return FetchResult.Ok(Feed(collected.values.toList(), matched))
            }
            if (page >= maxPages) {
                // A failure rather than what has been collected so far: stopping at the ceiling and calling
                // it the feed is the silent truncation this whole method exists to avoid.
                return FetchResult.Failed(
                    "The grinder reported $pages pages of verdicts, more than this plugin will request " +
                        "($maxPages x $pageSize rows). Narrow the grinder's store or raise the page size."
                )
            }
            page++
        }
    }

    /**
     * What makes two rows the same verdict, as [GrinderVerdict] itself defines it: a project is ground once
     * per Minecraft line per loader, and the same slug exists on both platforms as two candidates.
     */
    private data class RowIdentity(
        val platform: String,
        val slug: String,
        val loader: String,
        val minecraftLine: String?
    ) {
        companion object {
            fun of(verdict: GrinderVerdict) =
                RowIdentity(verdict.platform, verdict.slug, verdict.loader, verdict.minecraftLine)
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
     * `null` means the document is not a verdict feed at all, which the caller turns into a failure.
     */
    private fun readVerdicts(document: JsonNode): List<GrinderVerdict>? {
        val rows = when {
            document.isArray -> document
            document["verdicts"]?.isArray == true -> document["verdicts"]
            // `null`, not an empty list — see fetchVerdicts. An empty `verdicts` array still reaches the
            // branch above, so a genuinely empty grinder is still a success.
            else -> return null
        }
        return rows.mapNotNull { row ->
            val slug = row.textOrNull("slug") ?: return@mapNotNull null
            GrinderVerdict(
                slug = slug,
                projectUrl = row.textOrNull("projectUrl").orEmpty(),
                platform = row.textOrNull("platform").orEmpty(),
                loader = row.textOrNull("loader").orEmpty(),
            // Absent from a daemon older than the line axis, and left absent here rather than defaulted:
            // an era nobody recorded must not read as an era that was.
            minecraftLine = row.textOrNull("minecraftLine"),
                verdict = row.textOrNull("verdict").orEmpty(),
                suggestedEntry = row.textOrNull("suggestedEntry"),
                fileName = row.textOrNull("fileName"),
                detail = row.textOrNull("detail").orEmpty(),
                scannedAt = row.textOrNull("verifiedAt").orEmpty(),
                // Both stay nullable: a row the grinder recorded no reading for must render as an empty
                // cell, not as the word "null" -- 18 of 2057 rows on the live feed have no `declared`.
                declared = row.textOrNull("declared"),
                jarScan = row.textOrNull("jarScan")
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

    companion object {
        /**
         * Rows per request when paging the feed.
         *
         * Two thousand because the cost is split between round trips and the size of each answer: at roughly
         * 800 bytes of JSON per row this is about 1.6 MB per request, comfortably inside the 15-second timeout,
         * while a store of a few hundred thousand rows still costs a few hundred requests rather than thousands.
         */
        const val DEFAULT_PAGE_SIZE = 2_000

        /**
         * The most pages one [fetchVerdicts] will request before giving up.
         *
         * A ceiling is needed because `pages` is the daemon's own claim and this loop advances on it, so a
         * wrong or runaway value would page for ever behind a Swing worker. Deliberately generous — at
         * [DEFAULT_PAGE_SIZE] it is four million rows, an order of magnitude past anything observed — so
         * that reaching it means something is wrong rather than merely large.
         */
        const val MAX_PAGES = 2_000
    }
}
