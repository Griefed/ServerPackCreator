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
 * Enumerates the CurseForge mod catalog, **most-downloaded first**. Unlike Modrinth, CurseForge's API
 * requires an `x-api-key`, so this source is only wired when the key is present. One [page] call fills a
 * whole slice by walking `index`/`pageSize`; behind an [HttpFetcher] so it is unit-tested against canned
 * JSON. Each result's `links.websiteUrl` becomes the project link (a
 * `curseforge.com/minecraft/mc-mods/<slug>` URL the clientside `CurseForgePlatform` resolves), falling
 * back to one built from the slug.
 *
 * **The catalog is crawled in partitions, because one query cannot reach it all.** `/mods/search` refuses
 * an `index` beyond [MAX_INDEX], so any single query exposes at most 10 000 mods — far less than CurseForge
 * hosts. The crawl therefore walks a *sequence* of bounded queries ([CurseForgePartitions]): the unfiltered
 * catalog first (its top 10 000 by downloads, the mods that matter most), then every game version newest
 * first, splitting a version by modloader **and** by category when its `pagination.totalCount` says it holds
 * more than can be paged, and crawling any such slice from both ends when even that overflows. Which
 * partition the crawl is in travels in the crawl cursor ([CandidatePage.nextPartition]), so it survives
 * restarts.
 *
 * The axis lists come from `/games/{gameId}/versions` and `/categories?classId=…`, refreshed at the start of
 * each sweep; without versions the crawl degrades to the unfiltered top 10 000, and without categories an
 * over-cap version falls back to its modloader slices — reduced coverage, still working.
 * **Residual gap, logged rather than hidden:** a single (version, category, modloader) slice holding more than
 * `2 × MAX_INDEX` mods loses its middle. That is the narrowest slice this API can express, so a mod there —
 * or one carrying neither a loader nor a category — is beyond reach of any partitioning.
 *
 * **The request contract is verified, not assumed** (see [SORT_FIELD_TOTAL_DOWNLOADS]): `pageSize`
 * defaults to and maxes at 50, the API rejects `index + pageSize > 10 000`, `sortOrder` takes
 * `asc`/`desc`, results carry `downloadCount` and `links.websiteUrl`, and auth is the `x-api-key`
 * header, `pagination.totalCount` reports a query's true size, and `/games/{gameId}/versions` answers with
 * `data: [{type, versions: [...]}]` — all per CurseForge's REST docs. Ordering is additionally *not* relied
 * upon for correctness: `GrindPool` re-orders each batch itself (round-robin across platforms, each platform by
 * `popularity`), so a mis-sorted page would only change *which* projects get fetched, never the grind order.
 * [warnIfMisordered] surfaces that case instead of letting it pass silently.
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

    /**
     * The primary partition axis: platform game versions, newest first. Refreshed at the start of every sweep;
     * empty until the first successful fetch, which leaves the crawl unpartitioned (top [MAX_INDEX] only).
     */
    @Volatile
    private var gameVersions: List<String> = emptyList()

    /**
     * The second axis, for versions too big to page through: the mod class's category ids. Refreshed alongside
     * [gameVersions]; empty means an over-cap version is covered by its modloader slices alone.
     */
    @Volatile
    private var categoryIds: List<Int> = emptyList()

    override val platform = ModPlatforms.CURSEFORGE

    init {
        require(pageSize in 1..MAX_PAGE_SIZE) { "CurseForge caps pageSize at $MAX_PAGE_SIZE, was $pageSize" }
    }

    /**
     * Up to [limit] mod projects starting at [offset] **within [partition]**, in that partition's sort order,
     * continuing into the following partitions when one runs out. Ends the catalog only when the *last*
     * partition of the plan runs out; a failed request or a failed size probe ends the slice while keeping the
     * position, so the crawler retries that region instead of wrapping to the top.
     */
    override fun page(offset: Int, limit: Int, partition: String?): CandidatePage {
        require(offset >= 0) { "offset must be >= 0, was $offset" }
        require(limit >= 0) { "limit must be >= 0, was $limit" }
        // At the start of a sweep, re-read both axis lists so versions and categories added since the last one
        // get crawled. Also read them whenever they are *missing*, which is the case that matters after a
        // restart: the cursor resumes mid-sweep with a partition token, and without its axis lists the plan
        // would find no next partition, report the catalog finished and throw the resumed position away.
        if (partition == null || gameVersions.isEmpty()) {
            refreshGameVersions()
        }
        if (partition == null || categoryIds.isEmpty()) {
            refreshCategories()
        }
        var current = partition?.let { CurseForgePartition.parse(it) } ?: CurseForgePartitions.FIRST
        var index = offset
        var knownTotal: Int? = null
        val gathered = ArrayList<GrindCandidate>(minOf(limit, 1024))
        var endOfCatalog = false

        while (gathered.size < limit) {
            if (index >= MAX_INDEX) {
                // This query is paged out. Its true size decides whether the rest is reachable by splitting.
                val total = knownTotal ?: totalCountOf(current) ?: break // probe failed: keep the position
                warnIfSliceIsUnreachable(current, total)
                val following = CurseForgePartitions.next(current, total, gameVersions, categoryIds)
                if (following == null) {
                    endOfCatalog = true
                    break
                }
                current = following
                index = 0
                knownTotal = null
                continue
            }
            val remaining = limit - gathered.size
            // Clamp so `index + pageSize` never exceeds the cap — the API rejects such a request outright.
            val count = pageSize.coerceAtMost(remaining).coerceAtMost(MAX_INDEX - index)
            val response = searchPage(current, index, count) ?: break // request failed: partial slice
            knownTotal = response.totalCount ?: knownTotal
            val fitting = response.candidates.take(remaining)
            gathered.addAll(fitting)
            index += fitting.size
            if (response.candidates.size < count) {
                // Fewer than asked for ⇒ this partition is exhausted; carry on in the next one.
                val following = CurseForgePartitions.next(current, knownTotal ?: 0, gameVersions, categoryIds)
                if (following == null) {
                    endOfCatalog = true
                    break
                }
                current = following
                index = 0
                knownTotal = null
            }
        }
        return CandidatePage(gathered, index, endOfCatalog, current.key)
    }

    /**
     * Re-read the platform's game-version list — the partition axis — ordered newest first, **restricted to
     * Minecraft version types**. Called once per sweep (and whenever the list is missing) so versions released
     * while the daemon runs get crawled. A failed fetch keeps the previous list (or none at all, which degrades
     * the crawl to the unfiltered top [MAX_INDEX]) rather than aborting.
     *
     * The restriction is not cosmetic. `/games/{gameId}/versions` groups versions by *version type*, and for
     * Minecraft those types include far more than Minecraft releases: measured against the live API it returns
     * **7 335 version strings across 36 types**, among them modloader version families (Forge `47.0.42`) and
     * types named `Server Side`, `Shader Loader`, `Addons` and even `DO NOT USE - Grouped MC Versions`. Keeping
     * only the types whose name begins with `Minecraft ` (as reported by `/games/{gameId}/version-types`) leaves
     * **135** real versions — a 54× smaller axis, and a sweep that spends its requests on partitions that can
     * actually hold mods.
     */
    private fun refreshGameVersions() {
        val minecraftTypes = minecraftVersionTypeIds()
        val body = fetchOrNull("$apiBase/games/$minecraftGameId/versions") {
            "CurseForge game-version list unavailable ($it) — crawling only the unfiltered top $MAX_INDEX mods " +
                "this sweep, since partitioning the catalog needs that list."
        } ?: return
        val versions = runCatching {
            objectMapper.readTree(body).path("data")
                .filter { group -> minecraftTypes.isEmpty() || group.path("type").asInt(-1) in minecraftTypes }
                .flatMap { group -> group.path("versions").map { it.asText() } }
        }.getOrElse {
            log.warn("CurseForge game-version list could not be read (${it.message}); keeping the previous one.")
            return
        }
        val ordered = CurseForgePartitions.orderVersions(versions)
        if (ordered.isEmpty()) {
            log.warn("CurseForge reported no game versions; keeping the previous list of ${gameVersions.size}.")
            return
        }
        gameVersions = ordered
        log.info("CurseForge crawl covers ${ordered.size} game version(s), newest first (${ordered.first()}).")
    }

    /**
     * The `/games/{gameId}/version-types` ids whose name marks them a Minecraft release family. An **empty**
     * result means "could not tell", and [refreshGameVersions] then keeps every type — a slower sweep beats a
     * sweep that silently crawls nothing.
     */
    private fun minecraftVersionTypeIds(): Set<Int> {
        val body = fetchOrNull("$apiBase/games/$minecraftGameId/version-types") {
            "CurseForge version-type list unavailable ($it) — crawling every version type this sweep, including " +
                "the modloader and non-Minecraft ones, which wastes requests but loses no coverage."
        } ?: return emptySet()
        return runCatching {
            objectMapper.readTree(body).path("data")
                .filter { it.path("name").asText("").startsWith(MINECRAFT_VERSION_TYPE_PREFIX, ignoreCase = true) }
                .mapNotNull { node -> node.path("id").takeIf { it.isInt }?.asInt() }
                .toSet()
        }.getOrElse {
            log.warn("CurseForge version-type list could not be read (${it.message}); crawling every type.")
            emptySet()
        }
    }

    /**
     * Re-read the mod class's categories — the second partition axis, used for versions whose mods cannot all
     * be paged through. **Every** category is taken, parents and children alike, because whether a search on a
     * parent category also returns its children is not documented; crawling both costs a few requests and
     * removes the doubt. Class entries (`isClass`) are dropped: searching the class is what `classId` already
     * does. A failed fetch leaves the previous list (or none, which reduces an over-cap version to its
     * modloader slices) rather than aborting.
     */
    private fun refreshCategories() {
        val body = fetchOrNull("$apiBase/categories?gameId=$minecraftGameId&classId=$modsClassId") {
            "CurseForge category list unavailable ($it) — an over-cap game version will be covered by its " +
                "modloader slices only this sweep."
        } ?: return
        val ids = runCatching {
            objectMapper.readTree(body).path("data")
                .filterNot { it.path("isClass").asBoolean(false) }
                .mapNotNull { node -> node.path("id").takeIf { it.isInt }?.asInt() }
                .distinct()
                .sorted()
        }.getOrElse {
            log.warn("CurseForge category list could not be read (${it.message}); keeping the previous one.")
            return
        }
        if (ids.isEmpty()) {
            log.warn("CurseForge reported no mod categories; keeping the previous list of ${categoryIds.size}.")
            return
        }
        categoryIds = ids
        log.info("CurseForge crawl can narrow an over-cap game version by ${ids.size} categor(y/ies).")
    }

    /** GET [url], or `null` with the warning [onFailure] builds from the error — used for the axis lists. */
    private fun fetchOrNull(url: String, onFailure: (String?) -> String): String? =
        runCatching { httpFetcher.get(url, headers) }
            .getOrElse {
                log.warn(onFailure(it.message))
                return null
            }

    /**
     * The `pagination.totalCount` of [partition], read with a single-item query, or `null` when that request
     * failed. Only needed when a slice resumes exactly at the paging cap without having seen a count yet —
     * every ordinary response carries one for free.
     */
    private fun totalCountOf(partition: CurseForgePartition): Int? = searchPage(partition, index = 0, count = 1)?.totalCount

    /**
     * Warn when leaving [partition] at the paging cap loses mods for good: the plan crawls a loader slice from
     * both ends, so anything beyond `2 × MAX_INDEX` in one slice is unreachable. Reported as a number rather
     * than left silent, because a sweep that skips 30 000 mods must not look complete.
     */
    private fun warnIfSliceIsUnreachable(partition: CurseForgePartition, totalCount: Int) {
        val isDeepestSlice = partition.gameVersion != null && partition.categoryId != null && partition.modLoaderType != null
        // Saturation is the signal, not a size comparison: `totalCount` never reports more than the cap, so
        // "holds more than the two sort directions reach" is unobservable — asking for `> 2 × MAX_INDEX` here
        // meant this warning could never fire, and an unbounded coverage hole would have stayed silent.
        if (isDeepestSlice && partition.ascending && totalCount >= MAX_INDEX) {
            log.warn(
                "CurseForge partition ${partition.key} is still saturated ($totalCount = the API's reporting cap) " +
                    "after being crawled from both ends, so it holds an unknown number of mods beyond the " +
                    "${2 * MAX_INDEX} reachable this way, and they are being skipped. This is already the narrowest " +
                    "slice the search API allows (version × category × modloader), so covering them needs another " +
                    "filter entirely."
            )
        }
    }

    /** One page of mods for [partition], or `null` when the request failed. */
    private fun searchPage(partition: CurseForgePartition, index: Int, count: Int): SearchPage? {
        val url = searchUrl(partition, index, count)
        val body = runCatching { httpFetcher.get(url, headers) }
            .getOrElse {
                log.warn("CurseForge search failed for ${partition.key} at index $index: ${it.message}")
                return null
            }
        val tree = runCatching { objectMapper.readTree(body) }
            .getOrElse {
                log.warn("CurseForge search for ${partition.key} at index $index returned unreadable JSON: ${it.message}")
                return null
            }
        val candidates = tree.path("data").map { toCandidate(it) }
        val totalCount = tree.path("pagination").path("totalCount").let { if (it.isInt) it.asInt() else null }
        warnIfMisordered(candidates, partition, index)
        return SearchPage(candidates, totalCount)
    }

    /** The `/mods/search` URL for [partition] — the class docs list every parameter and its source. */
    private fun searchUrl(partition: CurseForgePartition, index: Int, count: Int): String = buildString {
        append("$apiBase/mods/search?gameId=$minecraftGameId&classId=$modsClassId")
        append("&sortField=$SORT_FIELD_TOTAL_DOWNLOADS")
        append("&sortOrder=${if (partition.ascending) "asc" else "desc"}")
        partition.gameVersion?.let { append("&gameVersion=${URLEncoder.encode(it, StandardCharsets.UTF_8)}") }
        partition.categoryId?.let { append("&categoryId=$it") }
        partition.modLoaderType?.let { append("&modLoaderType=$it") }
        append("&index=$index&pageSize=$count")
    }

    /**
     * Log a warning when [candidates] are not in the download order the request asked for — i.e. the API did
     * not honour the sort. Advisory only: the page is still used as-is (`GrindPool` sorts by popularity), but a
     * silently mis-sorted catalog would quietly change which projects get picked, so it must be visible. The
     * expected direction follows the partition, since the bottom of an oversized slice is fetched ascending.
     */
    private fun warnIfMisordered(candidates: List<GrindCandidate>, partition: CurseForgePartition, index: Int) {
        // Only the descending direction is checked, and only for its *trend*. Measured against the live API:
        // `sortOrder=asc` returns the catalog's tail but in no particular order, and even `desc` is not strictly
        // monotonic (a 10-mod page had 385 316 073 before 386 940 279 — the sort key is evidently not exactly
        // the `downloadCount` the response reports). Flagging adjacent inversions would therefore warn on
        // perfectly normal pages; what this is meant to catch is `sortField=6` ceasing to mean downloads at all,
        // which shows up as a page whose last mod outranks its first.
        if (partition.ascending || candidates.size < 2) {
            return
        }
        val trendsUpwards = candidates.first().popularity < candidates.last().popularity
        if (trendsUpwards) {
            log.warn(
                "CurseForge page for ${partition.key} at index $index trends *upwards* in downloads " +
                    "(${candidates.first().popularity} … ${candidates.last().popularity}) — " +
                    "sortField=$SORT_FIELD_TOTAL_DOWNLOADS may no longer mean TotalDownloads. " +
                    "Candidates are still usable (the pool re-orders each batch itself), but the fetched " +
                    "subset is no longer the intended one."
            )
        }
    }

    /** One parsed search response: the mods it listed, plus the query's true size when the API reported it. */
    private data class SearchPage(val candidates: List<GrindCandidate>, val totalCount: Int?)

    /** Map one CurseForge mod-node onto a [GrindCandidate] (websiteUrl → project link, downloads → rank). */
    private fun toCandidate(node: JsonNode): GrindCandidate {
        val slug = node.path("slug").asText()
        val website = node.path("links").path("websiteUrl")
            .takeIf { it.isTextual && it.asText().isNotBlank() }?.asText()
        return GrindCandidate(
            projectUrl = website ?: "https://www.curseforge.com/minecraft/mc-mods/$slug",
            slug = slug,
            popularity = node.path("downloadCount").asLong(0),
            platform = platform,
            // CurseForge's numeric project id, stringified. The slug is a display name and does change.
            projectId = node.path("id").takeIf { it.isNumber }?.asLong()?.toString()
        )
    }

    companion object {
        /**
         * CurseForge's `ModsSearchSortField` value for **TotalDownloads**. The REST docs render the enum
         * numerically without names, so the mapping is corroborated against PrismLauncher's
         * `FlameAPI::getSortingMethods()` (a long-standing consumer of this same endpoint), which lists:
         * `1 Featured, 2 Popularity, 3 LastUpdated, 4 Name, 5 Author, 6 TotalDownloads, 7 Category,
         * 8 GameVersion`. Note **2 is Popularity, not downloads** — the grinder deliberately ranks by
         * lifetime downloads. [warnIfMisordered] catches it should this ever change upstream.
         */
        const val SORT_FIELD_TOTAL_DOWNLOADS = 6

        /**
         * CurseForge rejects a search whose `index + pageSize` exceeds this (per its REST docs) — the reason
         * the catalog has to be crawled as partitions at all. See [CurseForgePartitions].
         */
        const val MAX_INDEX = 10_000

        /** CurseForge's documented default and maximum `pageSize`. */
        const val MAX_PAGE_SIZE = 50

        /**
         * Version-type names beginning with this are Minecraft release families; every other type
         * (`Server Side`, `Shader Loader`, modloader version families, …) is not a Minecraft version and must
         * not become a crawl partition. See [minecraftVersionTypeIds].
         */
        const val MINECRAFT_VERSION_TYPE_PREFIX = "Minecraft "
    }
}
