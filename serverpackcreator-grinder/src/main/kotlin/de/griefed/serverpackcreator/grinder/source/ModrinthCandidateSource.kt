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
package de.griefed.serverpackcreator.grinder.source

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.griefed.serverpackcreator.clientside.HttpFetcher
import de.griefed.serverpackcreator.clientside.JdkHttpFetcher
import de.griefed.serverpackcreator.grinder.GrindCandidate
import de.griefed.serverpackcreator.grinder.ModPlatforms
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Enumerates the Modrinth mod catalog, **most-downloaded first**. Modrinth's search API needs no key
 * and returns the download count, so the popularity ranking that decides what to grind first is free
 * (and the mods most likely to land in a modpack get verified first). One [page] call fills a whole
 * slice, requesting as many API pages as it takes; behind an [HttpFetcher] so it is unit-tested against
 * canned JSON without touching the network.
 *
 * **Offset ceiling (measured, 2026-07-29):** the API serves deep offsets happily (40 000 returns real
 * hits) but clamps `offset` at 99 999, answering with zero hits beyond it. `project_type:mod` currently
 * counts ~71 000 projects, so the whole catalog is reachable today; should it ever pass 100 000, the tail
 * becomes unreachable and looks exactly like the end of the catalog — the crawl would silently wrap early.
 *
 * @param httpFetcher  HTTP boundary, swapped for canned JSON in tests.
 * @param objectMapper Jackson mapper for the JSON responses.
 * @param pageSize     How many hits to request per call (Modrinth caps a page at 100).
 * @author Griefed
 */
class ModrinthCandidateSource(
    private val httpFetcher: HttpFetcher = JdkHttpFetcher(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val pageSize: Int = 100
) : CandidateSource {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val apiBase = "https://api.modrinth.com/v2"
    private val headers = mapOf(
        "User-Agent" to "Griefed/ServerPackCreator (grinder; griefed@griefed.de)",
        "Accept" to "application/json"
    )

    override val platform = ModPlatforms.MODRINTH

    /**
     * Up to [limit] mod projects starting at [offset], most-downloaded first. A short or empty page ends
     * the catalog (`endOfCatalog`); a *failed* request only ends this slice, returning what was gathered
     * with `endOfCatalog = false` so the crawler retries the same region instead of wrapping to the top.
     *
     * `partition` is ignored and never handed back: Modrinth's whole mod catalog is one offset sequence (its
     * offset ceiling of 99 999 sits comfortably above today's ~71 000 projects), so there is nothing to
     * partition — unlike CurseForge, whose paging cap forces a partitioned crawl.
     */
    override fun page(offset: Int, limit: Int, partition: String?): CandidatePage {
        require(offset >= 0) { "offset must be >= 0, was $offset" }
        require(limit >= 0) { "limit must be >= 0, was $limit" }
        val gathered = ArrayList<GrindCandidate>(minOf(limit, 1024))
        var cursor = offset
        var endOfCatalog = false
        while (gathered.size < limit) {
            val batch = pageSize.coerceAtMost(limit - gathered.size)
            val hits = searchPage(cursor, batch) ?: break // request failed: partial slice, catalog unknown
            if (hits.isEmpty()) {
                // An empty page normally means the catalog ran out — but at the offset ceiling the API also answers
                // with zero hits, and the two are indistinguishable from here. Say so rather than let a truncated
                // crawl look like a completed one.
                if (ceilingMayMasqueradeAsEnd(cursor)) {
                    log.warn(
                        "Modrinth returned no hits at offset $cursor, which is at or past the measured offset ceiling " +
                            "of $OFFSET_CEILING. This is reported as the end of the catalog, but it may instead be the " +
                            "ceiling clamping the response — in which case every project beyond it is unreachable and " +
                            "the sweep is silently incomplete. Narrow the query (facets) if the catalog has outgrown it."
                    )
                }
                endOfCatalog = true
                break
            }
            hits.forEach { gathered.add(it) }
            cursor += hits.size
            if (approachingOffsetCeiling(cursor, pageSize)) {
                log.warn(
                    "Modrinth crawl is at offset $cursor, within one page of the measured offset ceiling of " +
                        "$OFFSET_CEILING. Beyond it the API answers with zero hits, which this source cannot tell " +
                        "apart from the end of the catalog — the tail would be dropped without an error."
                )
            }
            if (hits.size < batch) {
                endOfCatalog = true // fewer than asked for ⇒ catalog exhausted
                break
            }
        }
        // Trim defensively (a page may hand back more than the slots left) and treat only what is handed
        // out as consumed, so nothing is skipped when that happens.
        val slice = gathered.take(limit)
        return CandidatePage(slice, offset + slice.size, endOfCatalog)
    }

    /** One page of mod hits ordered by downloads, or `null` when the request failed. */
    private fun searchPage(offset: Int, limit: Int): List<GrindCandidate>? {
        val facets = URLEncoder.encode("""[["project_type:mod"]]""", StandardCharsets.UTF_8)
        val url = "$apiBase/search?index=downloads&offset=$offset&limit=$limit&facets=$facets"
        val body = runCatching { httpFetcher.get(url, headers) }
            .getOrElse {
                log.warn("Modrinth search failed at offset $offset: ${it.message}")
                return null
            }
        return objectMapper.readTree(body).path("hits").map { toCandidate(it) }
    }

    /** Map one Modrinth search-hit onto a [GrindCandidate] (slug → project link, downloads → rank). */
    private fun toCandidate(hit: JsonNode): GrindCandidate {
        val slug = hit.path("slug").asText()
        return GrindCandidate(
            projectUrl = "https://modrinth.com/mod/$slug",
            slug = slug,
            popularity = hit.path("downloads").asLong(0),
            platform = platform
        )
    }

    internal companion object {
        /**
         * The deepest `offset` Modrinth's search will serve, measured 2026-07-29: offset 40 000 returns real hits,
         * and beyond this the API answers with **zero hits** rather than an error. `project_type:mod` sits around
         * 71 000 projects today, comfortably below it.
         */
        const val OFFSET_CEILING = 99_999

        /**
         * True when the next page would reach the ceiling, so the crawl is about to enter the region where a
         * truncated answer is indistinguishable from an exhausted catalog. Pure so the boundary is testable without
         * an HTTP fetcher.
         */
        internal fun approachingOffsetCeiling(offset: Int, pageSize: Int): Boolean = offset + pageSize >= OFFSET_CEILING

        /**
         * True when an empty page at this [offset] could be the ceiling clamping the response rather than the real
         * end of the catalog — the case that would otherwise let an incomplete sweep report itself as complete.
         */
        internal fun ceilingMayMasqueradeAsEnd(offset: Int): Boolean = offset >= OFFSET_CEILING
    }
}
