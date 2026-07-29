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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException

/**
 * Pins the CurseForge candidate source against canned search JSON (no network): mods map to candidates
 * preserving the API's download order, the project link is the `links.websiteUrl` (falling back to one
 * built from the slug), one slice spans several `index` pages, a slice starts at the requested offset, and
 * — the crawl-critical part — the catalog counts as ended on a short page *or* at CurseForge's hard
 * search-index cap, but never on a failed request.
 */
internal class CurseForgeCandidateSourceTest {

    /** Canned [HttpFetcher] serving a page per `index=`, optionally throwing at one index. */
    private class CannedSearch(
        private val pagesByIndex: Map<Int, String>,
        private val throwAtIndex: Int? = null
    ) : HttpFetcher {
        val requestedIndexes = mutableListOf<Int>()
        val requestedUrls = mutableListOf<String>()
        val seenHeaders = mutableListOf<Map<String, String>>()
        override fun get(url: String, headers: Map<String, String>): String {
            val index = requireNotNull(Regex("index=(\\d+)").find(url)) { "no index= in $url" }
                .groupValues[1].toInt()
            requestedIndexes.add(index)
            requestedUrls.add(url)
            seenHeaders.add(headers)
            if (index == throwAtIndex) throw IOException("curseforge 503")
            return pagesByIndex[index] ?: """{"data":[]}"""
        }
    }

    /** Build a CurseForge-search body; each mod carries slug, downloadCount and a websiteUrl link. */
    private fun searchJson(vararg mods: Triple<String, Long, String?>): String {
        val data = mods.joinToString(",") { (slug, downloads, website) ->
            val links = if (website == null) """{}""" else """{"websiteUrl":"$website"}"""
            """{"slug":"$slug","downloadCount":$downloads,"links":$links}"""
        }
        return """{"data":[$data]}"""
    }

    @Test
    fun mapsResultsToCandidatesPreservingDownloadOrder() {
        val fetcher = CannedSearch(
            mapOf(
                0 to searchJson(
                    Triple("jei", 900_000_000L, "https://www.curseforge.com/minecraft/mc-mods/jei"),
                    Triple("jade", 500_000_000L, "https://www.curseforge.com/minecraft/mc-mods/jade")
                )
            )
        )
        val candidates = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 2).candidates

        Assertions.assertEquals(listOf("jei", "jade"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(900_000_000L, 500_000_000L), candidates.map { it.popularity })
        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/jei", candidates.first().projectUrl)
    }

    @Test
    fun fallsBackToASlugBuiltUrlWhenWebsiteUrlIsMissing() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("appleskin", 100L, null))))
        val candidate = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 1).candidates.single()

        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/appleskin", candidate.projectUrl)
    }

    @Test
    fun fillsOneSliceFromSeveralApiPages() {
        val fetcher = CannedSearch(
            mapOf(
                0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b")),
                2 to searchJson(Triple("c", 7L, "u/c"), Triple("d", 6L, "u/d"))
            )
        )
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 0, limit = 3)

        Assertions.assertEquals(listOf("a", "b", "c"), page.candidates.map { it.slug })
        Assertions.assertEquals(listOf(0, 2), fetcher.requestedIndexes)
        Assertions.assertEquals(3, page.nextOffset, "only what was handed out counts as consumed")
        Assertions.assertFalse(page.endOfCatalog)
    }

    /** The crawl case: a slice deep in the catalog starts where it was told to, not at the top. */
    @Test
    fun startsAtTheRequestedOffsetAndReportsWhereToContinue() {
        val fetcher = CannedSearch(mapOf(400 to searchJson(Triple("deep", 3L, "u/deep"), Triple("deeper", 2L, "u/deeper"))))
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 400, limit = 2)

        Assertions.assertEquals(listOf(400), fetcher.requestedIndexes, "must not restart at index 0")
        Assertions.assertEquals(listOf("deep", "deeper"), page.candidates.map { it.slug })
        Assertions.assertEquals(402, page.nextOffset)
    }

    @Test
    fun aShortPageMeansTheCatalogEnded() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b"))))
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 50).page(offset = 0, limit = 100)

        Assertions.assertEquals(2, page.candidates.size)
        Assertions.assertEquals(listOf(0), fetcher.requestedIndexes)
        Assertions.assertTrue(page.endOfCatalog)
    }

    /**
     * CurseForge's `/mods/search` rejects an `index` at or beyond [CurseForgeCandidateSource.MAX_INDEX], so
     * the *reachable* catalog ends there even though the platform hosts far more mods. The source must
     * report that as the end of the catalog — the crawler then wraps around and starts a new sweep instead
     * of hammering a request the API will refuse. **This is the documented CurseForge coverage ceiling.**
     */
    @Test
    fun theSearchIndexCapCountsAsTheEndOfTheCatalog() {
        val fetcher = CannedSearch(emptyMap())
        val source = CurseForgeCandidateSource("key", fetcher, pageSize = 50)

        val atCap = source.page(offset = CurseForgeCandidateSource.MAX_INDEX, limit = 50)
        Assertions.assertTrue(atCap.candidates.isEmpty())
        Assertions.assertTrue(atCap.endOfCatalog, "the index cap is the end of the reachable catalog")
        Assertions.assertTrue(fetcher.requestedIndexes.isEmpty(), "a request the API would reject is not sent")

        // A slice that *straddles* the cap is clamped to it rather than asking for a rejected range.
        val straddling = source.page(offset = CurseForgeCandidateSource.MAX_INDEX - 10, limit = 50)
        Assertions.assertEquals(listOf(CurseForgeCandidateSource.MAX_INDEX - 10), fetcher.requestedIndexes)
        Assertions.assertTrue(
            fetcher.requestedUrls.single().contains("pageSize=10"),
            "index + pageSize must stay within the cap, was ${fetcher.requestedUrls.single()}"
        )
        Assertions.assertTrue(straddling.endOfCatalog)
    }

    /**
     * A failed request must **not** look like the end of the catalog: the crawler wraps around to offset 0
     * on `endOfCatalog`, so a transient 503 deep in the catalog would otherwise throw away the whole crawl
     * position and restart at the most-downloaded mods.
     */
    @Test
    fun aFailedPageReturnsWhatWasGatheredWithoutClaimingTheCatalogEnded() {
        val fetcher = CannedSearch(
            mapOf(0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b"))),
            throwAtIndex = 2
        )
        val page = CurseForgeCandidateSource("key", fetcher, pageSize = 2).page(offset = 0, limit = 10)

        Assertions.assertEquals(listOf("a", "b"), page.candidates.map { it.slug })
        Assertions.assertEquals(2, page.nextOffset, "resume after what did arrive")
        Assertions.assertFalse(page.endOfCatalog, "a failed request is not the end of the catalog")
    }

    /**
     * Pins the request contract verified against CurseForge's REST docs (and, for the sort value, against
     * PrismLauncher's published enum): Minecraft `gameId=432`, mods `classId=6`, `sortField=6`
     * (TotalDownloads) with `sortOrder=desc`, `pageSize` within the documented max, and the `x-api-key`
     * header. A silent change to any of these would fetch the wrong catalog slice.
     */
    @Test
    fun requestsTheDocumentedSearchContract() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("jei", 9L, "u/jei"))))
        CurseForgeCandidateSource("secret-key", fetcher, pageSize = 50).page(offset = 0, limit = 1)

        val url = fetcher.requestedUrls.single()
        Assertions.assertTrue(url.startsWith("https://api.curseforge.com/v1/mods/search?"), url)
        listOf("gameId=432", "classId=6", "sortField=${CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS}", "sortOrder=desc", "index=0")
            .forEach { Assertions.assertTrue(url.contains(it), "missing '$it' in $url") }
        Assertions.assertEquals(6, CurseForgeCandidateSource.SORT_FIELD_TOTAL_DOWNLOADS, "CF TotalDownloads sort value")
        Assertions.assertEquals("secret-key", fetcher.seenHeaders.single()["x-api-key"])
    }

    @Test
    fun rejectsAPageSizeAboveTheDocumentedMaximum() {
        assertThrows<IllegalArgumentException> {
            CurseForgeCandidateSource("key", CannedSearch(emptyMap()), pageSize = CurseForgeCandidateSource.MAX_PAGE_SIZE + 1)
        }
    }

    /**
     * A page the API returned out of download-order is still passed through unchanged (the warning is
     * advisory — `GrindPool` re-sorts by popularity, so nothing is dropped or reordered here).
     */
    @Test
    fun anOutOfOrderPageIsStillReturnedIntact() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("low", 1L, "u/low"), Triple("high", 999L, "u/high"))))
        val candidates = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 2).candidates

        Assertions.assertEquals(listOf("low", "high"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(1L, 999L), candidates.map { it.popularity })
    }

    @Test
    fun limitZeroFetchesNothing() {
        val fetcher = CannedSearch(emptyMap())
        val candidates = CurseForgeCandidateSource("key", fetcher).page(offset = 0, limit = 0).candidates

        Assertions.assertTrue(candidates.isEmpty())
        Assertions.assertTrue(fetcher.requestedIndexes.isEmpty(), "limit 0 must not hit the API")
    }
}
