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

import de.griefed.serverpackcreator.clientside.HttpFetcher
import de.griefed.serverpackcreator.grinder.ModPlatforms
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

/**
 * Pins the CurseForge candidate source against canned JSON (no network, and no API key — which this module
 * has never had, so these tests *are* the contract). Two things are checked: the mapping (download order,
 * `links.websiteUrl` with a slug fallback, the documented request parameters) and the **partitioned crawl**
 * that gets past the platform's 10 000-result paging cap — which partition each slice queries, how a slice
 * crosses into the next partition, and that a failure anywhere keeps the crawl position instead of losing it.
 */
internal class CurseForgeCandidateSourceTest {

    private val searchUrl = "https://api.curseforge.com/v1/mods/search"
    private val versionsUrl = "https://api.curseforge.com/v1/games/432/versions"
    private val categoriesUrl = "https://api.curseforge.com/v1/categories"

    /**
     * Canned [HttpFetcher] answering by URL. `searchAnswers` maps a *substring* of the search URL (the
     * distinguishing filters, e.g. `gameVersion=1.20.1&`) to a body; the first match wins, so a test only
     * spells out the parts it cares about. Anything unmatched answers with an empty result set.
     */
    private class CannedApi(
        private val searchAnswers: List<Pair<String, String>> = emptyList(),
        private val versionsBody: String? = null,
        private val categoriesBody: String? = null,
        private val failUrlsContaining: String? = null
    ) : HttpFetcher {
        val requestedUrls = mutableListOf<String>()
        val seenHeaders = mutableListOf<Map<String, String>>()

        override fun get(url: String, headers: Map<String, String>): String {
            requestedUrls.add(url)
            seenHeaders.add(headers)
            failUrlsContaining?.let { if (url.contains(it)) throw IOException("curseforge 503 for $url") }
            if (url.contains("/games/432/versions")) {
                return versionsBody ?: """{"data":[]}"""
            }
            if (url.contains("/categories")) {
                return categoriesBody ?: """{"data":[]}"""
            }
            return searchAnswers.firstOrNull { (fragment, _) -> url.contains(fragment) }?.second
                ?: """{"data":[],"pagination":{"index":0,"pageSize":50,"resultCount":0,"totalCount":0}}"""
        }

        /** Search URLs only, in request order — the crawl path. */
        fun searchRequests(): List<String> = requestedUrls.filter { it.contains("/mods/search") }
    }

    /** A CurseForge search body: mods plus the `pagination.totalCount` that drives partition splitting. */
    private fun searchJson(vararg mods: Triple<String, Long, String?>, totalCount: Int = mods.size): String {
        val data = mods.joinToString(",") { (slug, downloads, website) ->
            val links = if (website == null) """{}""" else """{"websiteUrl":"$website"}"""
            """{"slug":"$slug","downloadCount":$downloads,"links":$links}"""
        }
        return """{"data":[$data],"pagination":{"index":0,"pageSize":50,"resultCount":${mods.size},"totalCount":$totalCount}}"""
    }

    /** A `/games/{id}/versions` body: version-type groups, each with its version strings. */
    private fun versionsJson(vararg groups: List<String>): String {
        val data = groups.mapIndexed { groupIndex, versions ->
            """{"type":${7000 + groupIndex},"versions":[${versions.joinToString(",") { "\"$it\"" }}]}"""
        }
        return """{"data":[${data.joinToString(",")}]}"""
    }

    /** A `/categories` body: the mods class itself plus its categories, as the API returns them together. */
    private fun categoriesJson(vararg categoryIds: Int): String {
        val theClass = """{"id":6,"name":"Mods","isClass":true,"classId":null}"""
        val categories = categoryIds.map { """{"id":$it,"name":"cat$it","isClass":false,"classId":6}""" }
        return """{"data":[${(listOf(theClass) + categories).joinToString(",")}]}"""
    }

    @Test
    fun mapsResultsToCandidatesPreservingDownloadOrder() {
        val fetcher = CannedApi(
            listOf(
                "index=0" to searchJson(
                    Triple("jei", 900_000_000L, "https://www.curseforge.com/minecraft/mc-mods/jei"),
                    Triple("jade", 500_000_000L, "https://www.curseforge.com/minecraft/mc-mods/jade")
                )
            )
        )
        val page = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 2)

        Assertions.assertEquals(listOf("jei", "jade"), page.candidates.map { it.slug })
        Assertions.assertEquals(listOf(900_000_000L, 500_000_000L), page.candidates.map { it.popularity })
        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/jei", page.candidates.first().projectUrl)
        Assertions.assertEquals(ModPlatforms.CURSEFORGE, page.candidates.first().platform)
    }

    @Test
    fun fallsBackToASlugBuiltUrlWhenWebsiteUrlIsMissing() {
        val fetcher = CannedApi(listOf("index=0" to searchJson(Triple("appleskin", 100L, null))))
        val candidate = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 1).candidates.single()

        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/appleskin", candidate.projectUrl)
    }

    /**
     * Pins the request contract verified against CurseForge's REST docs (and, for the sort value, against
     * PrismLauncher's published enum): Minecraft `gameId=432`, mods `classId=6`, `sortField=6`
     * (TotalDownloads) with `sortOrder=desc`, `pageSize` within the documented max, and the `x-api-key`
     * header. A silent change to any of these would fetch the wrong catalog slice.
     */
    @Test
    fun requestsTheDocumentedSearchContract() {
        val fetcher = CannedApi(listOf("index=0" to searchJson(Triple("jei", 9L, "u/jei"))))
        CurseForgeCandidateSource("secret-key", fetcher, pageSize = 50).page(offset = 0, limit = 1)

        val url = fetcher.searchRequests().single()
        Assertions.assertTrue(url.startsWith("$searchUrl?"), url)
        listOf("gameId=432", "classId=6", "sortField=${CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS}", "sortOrder=desc", "index=0")
            .forEach { Assertions.assertTrue(url.contains(it), "missing '$it' in $url") }
        Assertions.assertEquals(6, CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS, "CF TotalDownloads sort value")
        Assertions.assertEquals("secret-key", fetcher.seenHeaders.first()["x-api-key"])
    }

    @Test
    fun rejectsAPageSizeAboveTheDocumentedMaximum() {
        assertThrows<IllegalArgumentException> {
            CurseForgeCandidateSource("key", CannedApi(), pageSize = CurseForgeCandidateSource.MAX_PAGE_SIZE + 1)
        }
    }

    @Test
    fun rejectsANegativeOffsetOrLimit() {
        val source = CurseForgeCandidateSource("key", CannedApi())
        assertThrows<IllegalArgumentException> { source.page(offset = -1, limit = 10) }
        assertThrows<IllegalArgumentException> { source.page(offset = 0, limit = -1) }
    }

    @Test
    fun limitZeroFetchesNothing() {
        val fetcher = CannedApi()
        val page = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 0)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(fetcher.searchRequests().isEmpty(), "limit 0 must not search")
    }

    /** A slice deep inside a partition starts where it was told to, not at the top. */
    @Test
    fun startsAtTheRequestedOffsetAndReportsWhereToContinue() {
        val fetcher = CannedApi(listOf("index=400" to searchJson(Triple("deep", 3L, "u/deep"), Triple("deeper", 2L, "u/deeper"))))
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 400, limit = 2)

        Assertions.assertEquals(listOf("deep", "deeper"), page.candidates.map { it.slug })
        Assertions.assertEquals(402, page.nextOffset)
    }

    @Test
    fun aFailedPageReturnsWhatWasGatheredWithoutClaimingTheCatalogEnded() {
        val fetcher = CannedApi(
            searchAnswers = listOf("index=0" to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b"), totalCount = 500)),
            failUrlsContaining = "index=2"
        )
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 0, limit = 10)

        Assertions.assertEquals(listOf("a", "b"), page.candidates.map { it.slug })
        Assertions.assertEquals(2, page.nextOffset, "resume after what did arrive")
        Assertions.assertFalse(page.endOfCatalog, "a failed request is not the end of the catalog")
    }

    // ---------------------------------------------------------------------------------------------------
    // The partitioned crawl — how coverage gets past the 10 000-result cap.
    // ---------------------------------------------------------------------------------------------------

    /** A sweep opens on the unfiltered catalog: no version or loader filter, most-downloaded first. */
    @Test
    fun aSweepStartsOnTheUnfilteredCatalog() {
        val fetcher = CannedApi(listOf("index=0" to searchJson(Triple("jei", 9L, "u/jei"), totalCount = 250_000)))
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = null)

        val url = fetcher.searchRequests().single()
        Assertions.assertFalse(url.contains("gameVersion="), url)
        Assertions.assertFalse(url.contains("modLoaderType="), url)
        Assertions.assertEquals(CurseForgePartitions.FIRST.key, page.nextPartition)
    }

    /** Both axis lists are refreshed at the start of a sweep only — not on every slice of it. */
    @Test
    fun theAxisListsAreRefreshedAtTheStartOfASweepOnly() {
        val fetcher = CannedApi(
            searchAnswers = listOf("index=0" to searchJson(Triple("jei", 9L, "u/jei"), totalCount = 250_000)),
            versionsBody = versionsJson(listOf("1.21.1", "1.20.1")),
            categoriesBody = categoriesJson(406, 426)
        )
        val source = CurseForgeCandidateSource("key", fetcher, pageSize = 50)

        source.page(offset = 0, limit = 1, partition = null)
        Assertions.assertEquals(1, fetcher.requestedUrls.count { it == versionsUrl }, "sweep start refreshes the versions")
        Assertions.assertEquals(1, fetcher.requestedUrls.count { it.startsWith(categoriesUrl) }, "and the categories")

        source.page(offset = 50, limit = 1, partition = CurseForgePartitions.FIRST.key)
        Assertions.assertEquals(1, fetcher.requestedUrls.count { it == versionsUrl }, "mid-sweep slices must not re-fetch")
        Assertions.assertEquals(1, fetcher.requestedUrls.count { it.startsWith(categoriesUrl) }, "either list")
    }

    /**
     * The restart case, and the reason the axis lists are not only read at the start of a sweep: a resumed
     * cursor arrives with a partition token and an in-memory source that knows no versions or categories. It
     * has to fetch them, or the plan finds no next partition, declares the catalog finished, and wraps — which
     * discards exactly the position the cursor exists to preserve.
     */
    @Test
    fun resumingMidSweepFetchesTheAxisListsItHasNotGotYet() {
        val fetcher = CannedApi(
            searchAnswers = listOf("gameVersion=1.21.1" to searchJson(Triple("resumed", 5L, "u/resumed"), totalCount = 4_000)),
            versionsBody = versionsJson(listOf("1.21.1", "1.20.1")),
            categoriesBody = categoriesJson(406)
        )
        val resumedPartition = CurseForgePartition("1.21.1", categoryId = null, modLoaderType = null, ascending = false)

        // A fresh source, as after a restart — never asked for partition == null.
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50)
            .page(offset = 200, limit = 1, partition = resumedPartition.key)

        Assertions.assertEquals(1, fetcher.requestedUrls.count { it == versionsUrl }, "the resumed crawl needs the versions")
        Assertions.assertEquals(1, fetcher.requestedUrls.count { it.startsWith(categoriesUrl) }, "and the categories")
        Assertions.assertTrue(fetcher.searchRequests().single().contains("index=200"), "it resumes at the stored offset")
        Assertions.assertEquals(listOf("resumed"), page.candidates.map { it.slug })
        Assertions.assertFalse(page.endOfCatalog, "resuming mid-sweep must not look like a finished catalog")
        Assertions.assertEquals(resumedPartition.key, page.nextPartition, "still in the partition it resumed into")
        Assertions.assertEquals(201, page.nextOffset)
    }

    /** The categories request has to ask for the *mods* class, or it returns another class's categories. */
    @Test
    fun theCategoryListIsRequestedForTheModsClass() {
        val fetcher = CannedApi(categoriesBody = categoriesJson(406))
        CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 1, partition = null)

        val url = fetcher.requestedUrls.single { it.startsWith(categoriesUrl) }
        Assertions.assertTrue(url.contains("gameId=432"), url)
        Assertions.assertTrue(url.contains("classId=6"), url)
    }

    /** A category slice queries that category — the axis that reaches mods carrying no modloader tag. */
    @Test
    fun aCategoryPartitionFiltersByThatCategory() {
        val partition = CurseForgePartition("1.20.1", categoryId = 426, modLoaderType = null, ascending = false)
        val fetcher = CannedApi(
            searchAnswers = listOf("categoryId=426" to searchJson(Triple("mapmod", 3L, "u/mapmod"), totalCount = 40)),
            versionsBody = versionsJson(listOf("1.20.1")),
            categoriesBody = categoriesJson(406, 426)
        )

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = partition.key)

        val url = fetcher.searchRequests().single()
        Assertions.assertTrue(url.contains("gameVersion=1.20.1"), url)
        Assertions.assertTrue(url.contains("categoryId=426"), url)
        Assertions.assertFalse(url.contains("modLoaderType="), "a category slice is not narrowed by loader yet: $url")
        Assertions.assertEquals(listOf("mapmod"), page.candidates.map { it.slug })
    }

    /** The deepest slice carries all three filters at once. */
    @Test
    fun theDeepestPartitionFiltersByVersionCategoryAndLoader() {
        val partition = CurseForgePartition("1.20.1", categoryId = 426, modLoaderType = CurseForgePartitions.QUILT, ascending = false)
        val fetcher = CannedApi(
            searchAnswers = listOf("categoryId=426" to searchJson(Triple("deep", 2L, "u/deep"), totalCount = 5)),
            versionsBody = versionsJson(listOf("1.20.1")),
            categoriesBody = categoriesJson(426)
        )

        CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = partition.key)

        val url = fetcher.searchRequests().single()
        listOf("gameVersion=1.20.1", "categoryId=426", "modLoaderType=${CurseForgePartitions.QUILT}")
            .forEach { Assertions.assertTrue(url.contains(it), "missing '$it' in $url") }
    }

    /**
     * After the last modloader of an over-cap version, the crawl continues into that version's **category**
     * slices — the transition that closes the loader-less-mod hole, end to end through the source.
     */
    @Test
    fun theLoaderStageHandsOverToTheCategoryStageWithinOneSlice() {
        val fetcher = CannedApi(
            searchAnswers = listOf("categoryId=406" to searchJson(Triple("categorised", 3L, "u/cat"), totalCount = 70)),
            versionsBody = versionsJson(listOf("1.20.1")),
            categoriesBody = categoriesJson(406, 426)
        )
        // The last loader slice answers empty (exhausted), so the slice must move on to the first category.
        val lastLoader = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = CurseForgePartitions.NEOFORGE, ascending = false)

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = lastLoader.key)

        Assertions.assertEquals(listOf("categorised"), page.candidates.map { it.slug })
        Assertions.assertEquals(
            CurseForgePartition("1.20.1", categoryId = 406, modLoaderType = null, ascending = false).key,
            page.nextPartition,
            "the crawl is now in the version's first category slice"
        )
        Assertions.assertFalse(page.endOfCatalog)
    }

    /** No category list must not stop the crawl — the version is simply done after its loader slices. */
    @Test
    fun anUnavailableCategoryListLeavesTheLoaderStageWorking() {
        val fetcher = CannedApi(
            versionsBody = versionsJson(listOf("1.20.1")),
            failUrlsContaining = "/categories"
        )
        val lastLoader = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = CurseForgePartitions.NEOFORGE, ascending = false)

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = lastLoader.key)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(page.endOfCatalog, "one version, its loaders done, no categories to fall back on")
    }

    @Test
    fun aVersionPartitionFiltersByThatGameVersion() {
        val fetcher = CannedApi(
            searchAnswers = listOf("gameVersion=1.20.1" to searchJson(Triple("create", 5L, "u/create"), totalCount = 300)),
            versionsBody = versionsJson(listOf("1.21.1", "1.20.1"))
        )
        val partition = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = null, ascending = false)

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50)
            .page(offset = 0, limit = 1, partition = partition.key)

        Assertions.assertEquals(listOf("create"), page.candidates.map { it.slug })
        Assertions.assertTrue(fetcher.searchRequests().single().contains("gameVersion=1.20.1"))
        Assertions.assertEquals(partition.key, page.nextPartition)
    }

    /** A loader slice carries both the modloader filter and, when crawling the bottom, the ascending sort. */
    @Test
    fun aLoaderPartitionFiltersByModLoaderAndSortDirection() {
        val partition = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = CurseForgePartitions.FABRIC, ascending = true)
        val fetcher = CannedApi(
            searchAnswers = listOf("modLoaderType=${CurseForgePartitions.FABRIC}" to searchJson(Triple("tiny", 1L, "u/tiny"), totalCount = 20)),
            versionsBody = versionsJson(listOf("1.20.1"))
        )

        CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 1, partition = partition.key)

        val url = fetcher.searchRequests().single()
        Assertions.assertTrue(url.contains("gameVersion=1.20.1"), url)
        Assertions.assertTrue(url.contains("modLoaderType=${CurseForgePartitions.FABRIC}"), url)
        Assertions.assertTrue(url.contains("sortOrder=asc"), "the bottom of a slice is reached by sorting ascending: $url")
    }

    /**
     * The heart of it: when a partition runs out mid-slice the crawl continues **in the next partition**
     * within the same call, and reports an offset relative to that new partition — otherwise the cursor would
     * point into the wrong query and skip a chunk of the catalog.
     */
    @Test
    fun crossingAPartitionBoundaryContinuesInTheNextPartitionAndResetsTheOffset() {
        val fetcher = CannedApi(
            searchAnswers = listOf(
                // The unfiltered slice is exhausted after one mod (a short page ⇒ partition over)...
                "gameVersion" to searchJson(Triple("versioned", 4L, "u/versioned"), totalCount = 300),
                "index=0" to searchJson(Triple("global", 9L, "u/global"), totalCount = 1)
            ),
            versionsBody = versionsJson(listOf("1.21.1", "1.20.1"))
        )

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 0, limit = 2, partition = null)

        Assertions.assertEquals(listOf("global", "versioned"), page.candidates.map { it.slug }, "both partitions contributed")
        Assertions.assertEquals(
            CurseForgePartition("1.21.1", categoryId = null, modLoaderType = null, ascending = false).key, page.nextPartition,
            "the crawl is now in the newest version's partition"
        )
        Assertions.assertEquals(1, page.nextOffset, "offset is relative to the new partition, not the old one")
        Assertions.assertFalse(page.endOfCatalog, "there are more versions to crawl")
    }

    /**
     * Reaching the paging cap is not the end of the catalog any more: the partition's `totalCount` says it
     * holds more than can be paged, so the crawl splits that version by modloader and carries on. The size is
     * probed when the slice resumes exactly at the cap and no count has been seen yet this call.
     */
    @Test
    fun reachingThePagingCapSplitsTheVersionByModLoader() {
        val overCap = CurseForgeCandidateSource.MAX_INDEX + 5_000
        val fetcher = CannedApi(
            searchAnswers = listOf(
                "modLoaderType=${CurseForgePartitions.FORGE}" to searchJson(Triple("forge-mod", 7L, "u/forge"), totalCount = 900),
                "gameVersion=1.20.1" to searchJson(Triple("probe-only", 1L, "u/probe"), totalCount = overCap)
            ),
            versionsBody = versionsJson(listOf("1.20.1"))
        )
        val cappedPartition = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = null, ascending = false)

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50)
            .page(offset = CurseForgeCandidateSource.MAX_INDEX, limit = 1, partition = cappedPartition.key)

        Assertions.assertEquals(listOf("forge-mod"), page.candidates.map { it.slug })
        Assertions.assertEquals(
            CurseForgePartition("1.20.1", categoryId = null, modLoaderType = CurseForgePartitions.FORGE, ascending = false).key, page.nextPartition,
            "past the cap, the version is re-crawled per modloader"
        )
        Assertions.assertFalse(page.endOfCatalog, "the catalog is not over — it is only this query that is capped")
        Assertions.assertTrue(
            fetcher.searchRequests().any { it.contains("pageSize=1") },
            "the partition size is probed with a single-item query: ${fetcher.searchRequests()}"
        )
    }

    /** If the size probe fails, the crawl must stay put rather than skip the rest of that partition. */
    @Test
    fun aFailedSizeProbeKeepsTheCrawlPosition() {
        val partition = CurseForgePartition("1.20.1", categoryId = null, modLoaderType = null, ascending = false)
        val fetcher = CannedApi(failUrlsContaining = "pageSize=1")

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50)
            .page(offset = CurseForgeCandidateSource.MAX_INDEX, limit = 5, partition = partition.key)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertFalse(page.endOfCatalog)
        Assertions.assertEquals(CurseForgeCandidateSource.MAX_INDEX, page.nextOffset)
        Assertions.assertEquals(partition.key, page.nextPartition, "same partition, to be retried next pass")
    }

    /**
     * Without a version list there is nothing to partition, so the crawl degrades to what it did before
     * partitioning existed: the top 10 000 by downloads, then wrap. Coverage is reduced, not broken.
     */
    @Test
    fun anUnavailableVersionListDegradesToTheUnpartitionedTopOfTheCatalog() {
        val fetcher = CannedApi(failUrlsContaining = "/games/432/versions")
        val source = CurseForgeCandidateSource("key", fetcher, pageSize = 50)

        val page = source.page(offset = CurseForgeCandidateSource.MAX_INDEX, limit = 5, partition = CurseForgePartitions.FIRST.key)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(page.endOfCatalog, "with no partitions, the paging cap really is the end of the crawl")
    }

    @Test
    fun theLastPartitionRunningOutEndsTheCatalog() {
        // One version and one category, and every query answers empty ⇒ the plan runs out.
        val fetcher = CannedApi(versionsBody = versionsJson(listOf("1.20.1")), categoriesBody = categoriesJson(406))
        val lastPartition = CurseForgePartition("1.20.1", categoryId = 406, modLoaderType = null, ascending = false)

        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50)
            .page(offset = 0, limit = 5, partition = lastPartition.key)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(page.endOfCatalog, "the end of the last partition is the end of the catalog")
    }

    /**
     * A page the API returned out of the requested order is still passed through unchanged (the warning is
     * advisory — `GrindPool` re-orders each batch itself, so nothing is dropped or reordered here). This one trends
     * *upwards*, which is the only shape that warns: live CurseForge pages are not strictly monotonic, so
     * adjacent jitter must never be treated as a mis-sorted catalog.
     */
    @Test
    fun anOutOfOrderPageIsStillReturnedIntact() {
        val fetcher = CannedApi(listOf("index=0" to searchJson(Triple("low", 1L, "u/low"), Triple("high", 999L, "u/high"))))
        val page = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 2)

        Assertions.assertEquals(listOf("low", "high"), page.candidates.map { it.slug })
        Assertions.assertEquals(listOf(1L, 999L), page.candidates.map { it.popularity })
    }
}
