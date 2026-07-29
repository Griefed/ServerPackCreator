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
import org.apache.logging.log4j.kotlin.cachedLoggerOf

/**
 * Seeds the grind queue from CurseForge, **most-downloaded first**. Unlike Modrinth, CurseForge's API
 * requires an `x-api-key`, so this source is only wired when the key is present. Paginates by
 * `index`/`pageSize` until [candidates]' `limit` is met or the catalog is exhausted; behind an
 * [HttpFetcher] so it is unit-tested against canned JSON. Each result's `links.websiteUrl` becomes the
 * project link (a `curseforge.com/minecraft/mc-mods/<slug>` URL the clientside `CurseForgePlatform`
 * resolves), falling back to one built from the slug.
 *
 * **The request contract is verified, not assumed** (see [SORT_FIELD_TOTAL_DOWNLOADS]): `pageSize`
 * defaults to and maxes at 50, the API rejects `index + pageSize > 10 000`, `sortOrder` takes
 * `asc`/`desc`, results carry `downloadCount` and `links.websiteUrl`, and auth is the `x-api-key`
 * header — all per CurseForge's REST docs. Ordering is additionally *not* relied upon for correctness:
 * `GrindPool` re-sorts the union of all sources by `popularity` anyway, so a mis-sorted page would only
 * change *which* projects get fetched, never the grind order. [warnIfNotDescending] surfaces that case
 * instead of letting it pass silently.
 *
 * @param apiKey       The CurseForge API-key (from `CURSEFORGE_API_KEY`).
 * @param httpFetcher  HTTP boundary, swapped for canned JSON in tests.
 * @param objectMapper Jackson mapper for the JSON responses.
 * @param pageSize     Hits per call (CurseForge caps a page at 50).
 * @author Griefed
 */
class CurseForgeCandidateSource(
    private val apiKey: String,
    private val httpFetcher: HttpFetcher = JdkHttpFetcher(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val pageSize: Int = 50
) : CandidateSource {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val apiBase = "https://api.curseforge.com/v1"
    private val minecraftGameId = 432
    private val modsClassId = 6
    private val headers = mapOf("x-api-key" to apiKey, "Accept" to "application/json")

    init {
        require(pageSize in 1..MAX_PAGE_SIZE) { "CurseForge caps pageSize at $MAX_PAGE_SIZE, was $pageSize" }
    }

    /**
     * Up to [limit] mod projects, most-downloaded first. A failed page stops pagination and returns what
     * was gathered so far (a partial catalog beats aborting); pagination also stops at CurseForge's
     * `index` cap.
     */
    override fun candidates(limit: Int): List<GrindCandidate> {
        require(limit >= 0) { "limit must be >= 0, was $limit" }
        val gathered = ArrayList<GrindCandidate>(minOf(limit, 1024))
        var index = 0
        while (gathered.size < limit && index < MAX_INDEX) {
            val count = pageSize.coerceAtMost(limit - gathered.size).coerceAtMost(MAX_INDEX - index)
            if (count <= 0) {
                break
            }
            val hits = searchPage(index, count) ?: break
            if (hits.isEmpty()) {
                break
            }
            hits.forEach { gathered.add(it) }
            if (hits.size < count) {
                break // fewer than asked for ⇒ catalog exhausted
            }
            index += hits.size
        }
        return gathered.take(limit)
    }

    /** One page of mods ordered by downloads, or `null` when the request failed. */
    private fun searchPage(index: Int, count: Int): List<GrindCandidate>? {
        val url = "$apiBase/mods/search?gameId=$minecraftGameId&classId=$modsClassId" +
            "&sortField=$SORT_FIELD_TOTAL_DOWNLOADS&sortOrder=desc&index=$index&pageSize=$count"
        val body = runCatching { httpFetcher.get(url, headers) }
            .getOrElse {
                log.warn("CurseForge search failed at index $index: ${it.message}")
                return null
            }
        val page = objectMapper.readTree(body).path("data").map { toCandidate(it) }
        warnIfNotDescending(page, index)
        return page
    }

    /**
     * Log a warning when [page] is not in descending download order — i.e. the API did not honour the
     * requested sort. Advisory only: the page is still used as-is (`GrindPool` sorts by popularity), but
     * a silently mis-sorted catalog would quietly change which projects get picked, so it must be visible.
     */
    private fun warnIfNotDescending(page: List<GrindCandidate>, index: Int) {
        val outOfOrder = page.zipWithNext().any { (left, right) -> right.popularity > left.popularity }
        if (outOfOrder) {
            log.warn(
                "CurseForge page at index $index is not sorted by descending downloads — " +
                    "sortField=$SORT_FIELD_TOTAL_DOWNLOADS may no longer mean TotalDownloads. " +
                    "Candidates are still usable (the pool re-sorts by popularity), but the fetched " +
                    "subset is no longer the most-downloaded projects."
            )
        }
    }

    /** Map one CurseForge mod-node onto a [GrindCandidate] (websiteUrl → project link, downloads → rank). */
    private fun toCandidate(node: JsonNode): GrindCandidate {
        val slug = node.path("slug").asText()
        val website = node.path("links").path("websiteUrl")
            .takeIf { it.isTextual && it.asText().isNotBlank() }?.asText()
        return GrindCandidate(
            projectUrl = website ?: "https://www.curseforge.com/minecraft/mc-mods/$slug",
            slug = slug,
            popularity = node.path("downloadCount").asLong(0)
        )
    }

    companion object {
        /**
         * CurseForge's `ModsSearchSortField` value for **TotalDownloads**. The REST docs render the enum
         * numerically without names, so the mapping is corroborated against PrismLauncher's
         * `FlameAPI::getSortingMethods()` (a long-standing consumer of this same endpoint), which lists:
         * `1 Featured, 2 Popularity, 3 LastUpdated, 4 Name, 5 Author, 6 TotalDownloads, 7 Category,
         * 8 GameVersion`. Note **2 is Popularity, not downloads** — the grinder deliberately ranks by
         * lifetime downloads. [warnIfNotDescending] catches it should this ever change upstream.
         */
        const val SORT_FIELD_TOTAL_DOWNLOADS = 6

        /** CurseForge rejects a search whose `index + pageSize` exceeds this (per its REST docs). */
        const val MAX_INDEX = 10_000

        /** CurseForge's documented default and maximum `pageSize`. */
        const val MAX_PAGE_SIZE = 50
    }
}
