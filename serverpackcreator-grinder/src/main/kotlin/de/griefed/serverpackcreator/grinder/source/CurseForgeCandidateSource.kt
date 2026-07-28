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
 * Seeds the grind queue from CurseForge, **most-downloaded first** (`sortField=6` = TotalDownloads).
 * Unlike Modrinth, CurseForge's API requires an `x-api-key`, so this source is only wired when the key
 * is present. Paginates by `index`/`pageSize` (CF caps a page at 50 and the `index` at 10 000) until
 * [candidates]' `limit` is met or the catalog is exhausted; behind an [HttpFetcher] so it is unit-tested
 * against canned JSON. Each result's `links.websiteUrl` becomes the project link (a
 * `curseforge.com/minecraft/mc-mods/<slug>` URL the clientside `CurseForgePlatform` resolves), falling
 * back to one built from the slug.
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
    private val sortFieldTotalDownloads = 6

    /** CurseForge rejects a search whose `index` reaches this cap, so pagination stops here. */
    private val maxIndex = 10_000
    private val headers = mapOf("x-api-key" to apiKey, "Accept" to "application/json")

    init {
        require(pageSize in 1..50) { "CurseForge caps pageSize at 50, was $pageSize" }
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
        while (gathered.size < limit && index < maxIndex) {
            val count = pageSize.coerceAtMost(limit - gathered.size).coerceAtMost(maxIndex - index)
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
            "&sortField=$sortFieldTotalDownloads&sortOrder=desc&index=$index&pageSize=$count"
        val body = runCatching { httpFetcher.get(url, headers) }
            .getOrElse {
                log.warn("CurseForge search failed at index $index: ${it.message}")
                return null
            }
        return objectMapper.readTree(body).path("data").map { toCandidate(it) }
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
}
