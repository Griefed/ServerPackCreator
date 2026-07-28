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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Seeds the grind queue from Modrinth, **most-downloaded first**. Modrinth's search API needs no key
 * and returns the download count, so the popularity ranking that decides what to grind first is free
 * (and the mods most likely to land in a modpack get verified first). Paginates until [candidates]'
 * `limit` is met or the catalog is exhausted; behind an [HttpFetcher] so it is unit-tested against
 * canned JSON without touching the network.
 *
 * CurseForge is the natural sibling (a `CurseForgeCandidateSource`) but needs the API key and has no
 * declared sideness — added later; Modrinth is the cheap, keyless start.
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
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val apiBase = "https://api.modrinth.com/v2"
    private val headers = mapOf(
        "User-Agent" to "Griefed/ServerPackCreator (grinder; griefed@griefed.de)",
        "Accept" to "application/json"
    )

    /**
     * Up to [limit] mod projects, most-downloaded first, as queue candidates. A failed page stops
     * pagination and returns what was gathered so far (the grind proceeds with a partial catalog rather
     * than aborting).
     */
    fun candidates(limit: Int): List<GrindCandidate> {
        require(limit >= 0) { "limit must be >= 0, was $limit" }
        val gathered = ArrayList<GrindCandidate>(minOf(limit, 1024))
        var offset = 0
        while (gathered.size < limit) {
            val batch = pageSize.coerceAtMost(limit - gathered.size)
            val hits = searchPage(offset, batch) ?: break
            if (hits.isEmpty()) {
                break
            }
            hits.forEach { gathered.add(it) }
            if (hits.size < batch) {
                break // fewer than asked for ⇒ catalog exhausted
            }
            offset += hits.size
        }
        // Trim defensively: a page may hand back more than the slots left in this final batch.
        return gathered.take(limit)
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
            popularity = hit.path("downloads").asLong(0)
        )
    }
}
