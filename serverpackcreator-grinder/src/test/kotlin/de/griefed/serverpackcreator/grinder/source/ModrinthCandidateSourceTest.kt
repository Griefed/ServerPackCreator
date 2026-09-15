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
 * Pins the Modrinth candidate source against canned search JSON (no network): hits map to candidates
 * preserving the API's download order, one slice may span several API pages, a slice starts at the
 * requested offset and reports where to continue, and — the crawl-critical part — a *short* page reports
 * `endOfCatalog` while a *failed* page does not.
 */
internal class ModrinthCandidateSourceTest {

    /** Canned [HttpFetcher] serving a page per `offset=`, optionally throwing at one offset. */
    private class CannedSearch(
        private val pagesByOffset: Map<Int, String>,
        private val throwAtOffset: Int? = null
    ) : HttpFetcher {
        val requestedOffsets = mutableListOf<Int>()
        override fun get(url: String, headers: Map<String, String>): String {
            val offset = Regex("offset=(\\d+)").find(url)!!.groupValues[1].toInt()
            requestedOffsets.add(offset)
            if (offset == throwAtOffset) throw IOException("modrinth 503")
            return pagesByOffset[offset] ?: """{"hits":[]}"""
        }
    }

    /** Build a Modrinth-search body from (slug, downloads) pairs in order. */
    private fun searchJson(vararg hits: Pair<String, Long>): String =
        """{"hits":[${hits.joinToString(",") { (slug, downloads) -> """{"slug":"$slug","downloads":$downloads,"project_type":"mod"}""" }}]}"""

    @Test
    fun mapsHitsToCandidatesPreservingDownloadOrder() {
        val fetcher = CannedSearch(mapOf(0 to searchJson("sodium" to 50_000_000, "jei" to 40_000_000, "create" to 30_000_000)))
        val page = ModrinthCandidateSource(fetcher).page(offset = 0, limit = 3)

        Assertions.assertEquals(listOf("sodium", "jei", "create"), page.candidates.map { it.slug })
        Assertions.assertEquals(listOf(50_000_000L, 40_000_000L, 30_000_000L), page.candidates.map { it.popularity })
        Assertions.assertEquals("https://modrinth.com/mod/sodium", page.candidates.first().projectUrl)
        Assertions.assertEquals(ModPlatforms.MODRINTH, page.candidates.first().platform)
    }

    @Test
    fun fillsOneSliceFromSeveralApiPages() {
        val fetcher = CannedSearch(
            mapOf(
                0 to searchJson("a" to 9, "b" to 8),
                2 to searchJson("c" to 7, "d" to 6)
            )
        )
        val page = ModrinthCandidateSource(fetcher, pageSize = 2).page(offset = 0, limit = 3)

        Assertions.assertEquals(listOf("a", "b", "c"), page.candidates.map { it.slug })
        Assertions.assertEquals(listOf(0, 2), fetcher.requestedOffsets)
        Assertions.assertEquals(3, page.nextOffset, "only what was handed out counts as consumed")
        Assertions.assertFalse(page.endOfCatalog)
    }

    /** The crawl case: a slice deep in the catalog starts where it was told to, not at the top. */
    @Test
    fun startsAtTheRequestedOffsetAndReportsWhereToContinue() {
        val fetcher = CannedSearch(mapOf(500 to searchJson("deep" to 3, "deeper" to 2)))
        val page = ModrinthCandidateSource(fetcher, pageSize = 2).page(offset = 500, limit = 2)

        Assertions.assertEquals(listOf(500), fetcher.requestedOffsets, "must not restart at offset 0")
        Assertions.assertEquals(listOf("deep", "deeper"), page.candidates.map { it.slug })
        Assertions.assertEquals(502, page.nextOffset)
    }

    @Test
    fun aShortPageMeansTheCatalogEnded() {
        val fetcher = CannedSearch(mapOf(0 to searchJson("a" to 9, "b" to 8)))
        // Asks for 100 but only two exist; the short page ends the slice without a wasted extra call.
        val page = ModrinthCandidateSource(fetcher, pageSize = 100).page(offset = 0, limit = 100)

        Assertions.assertEquals(2, page.candidates.size)
        Assertions.assertEquals(listOf(0), fetcher.requestedOffsets)
        Assertions.assertTrue(page.endOfCatalog, "fewer hits than asked for is the end of the catalog")
    }

    @Test
    fun anEmptyPagePastTheEndMeansTheCatalogEnded() {
        val fetcher = CannedSearch(emptyMap()) // every offset answers with zero hits
        val page = ModrinthCandidateSource(fetcher, pageSize = 100).page(offset = 90_000, limit = 100)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(page.endOfCatalog)
        Assertions.assertEquals(90_000, page.nextOffset, "nothing consumed, so the offset stands")
    }

    /**
     * A failed request must **not** look like the end of the catalog: the crawler wraps around to offset 0
     * on `endOfCatalog`, so a transient 503 deep in the catalog would otherwise throw away the whole crawl
     * position and restart at the most-downloaded mods.
     */
    @Test
    fun aFailedPageReturnsWhatWasGatheredWithoutClaimingTheCatalogEnded() {
        val fetcher = CannedSearch(mapOf(0 to searchJson("a" to 9, "b" to 8)), throwAtOffset = 2)
        val page = ModrinthCandidateSource(fetcher, pageSize = 2).page(offset = 0, limit = 10)

        Assertions.assertEquals(listOf("a", "b"), page.candidates.map { it.slug })
        Assertions.assertEquals(2, page.nextOffset, "resume after what did arrive")
        Assertions.assertFalse(page.endOfCatalog, "a failed request is not the end of the catalog")
    }

    @Test
    fun limitZeroFetchesNothing() {
        val fetcher = CannedSearch(emptyMap())
        val page = ModrinthCandidateSource(fetcher).page(offset = 40, limit = 0)

        Assertions.assertTrue(page.candidates.isEmpty())
        Assertions.assertTrue(fetcher.requestedOffsets.isEmpty(), "limit 0 must not hit the API")
        Assertions.assertEquals(40, page.nextOffset)
        Assertions.assertFalse(page.endOfCatalog, "asking for nothing proves nothing about the catalog")
    }

    @Test
    fun rejectsANegativeOffsetOrLimit() {
        val source = ModrinthCandidateSource(CannedSearch(emptyMap()))
        assertThrows<IllegalArgumentException> { source.page(offset = -1, limit = 10) }
        assertThrows<IllegalArgumentException> { source.page(offset = 0, limit = -1) }
    }

    /**
     * Pins the offset-ceiling boundary that decides whether a truncated crawl can be told apart from a finished one.
     *
     * Measured 2026-07-29: Modrinth serves deep offsets (40 000 returns real hits) but clamps at 99 999, answering
     * with **zero hits** past it rather than an error — which `page` cannot distinguish from an exhausted catalog. At
     * ~71 000 `project_type:mod` projects there is headroom today, so this is a guard against a future silent
     * truncation: if the catalog outgrows the ceiling, the sweep would wrap early and report itself complete.
     */
    @Test
    fun theOffsetCeilingBoundaryIsPinned() {
        // One page short of the ceiling: still safe, no warning warranted.
        Assertions.assertFalse(ModrinthCandidateSource.approachingOffsetCeiling(offset = 99_000, pageSize = 100))
        // The next page would reach it.
        Assertions.assertTrue(ModrinthCandidateSource.approachingOffsetCeiling(offset = 99_899, pageSize = 100))
        Assertions.assertTrue(ModrinthCandidateSource.approachingOffsetCeiling(offset = 120_000, pageSize = 100))

        // An empty page below the ceiling really is the end of the catalog.
        Assertions.assertFalse(ModrinthCandidateSource.ceilingMayMasqueradeAsEnd(offset = 71_000))
        // At or past it, "empty" is ambiguous and must be called out.
        Assertions.assertTrue(ModrinthCandidateSource.ceilingMayMasqueradeAsEnd(offset = ModrinthCandidateSource.OFFSET_CEILING))
        Assertions.assertTrue(ModrinthCandidateSource.ceilingMayMasqueradeAsEnd(offset = 150_000))
    }

    /** Today's catalog size must stay clear of the ceiling; if this ever inverts, the crawl needs facet-partitioning. */
    @Test
    fun todaysCatalogFitsBelowTheCeiling() {
        val measuredModProjects = 71_000
        Assertions.assertTrue(
            measuredModProjects < ModrinthCandidateSource.OFFSET_CEILING,
            "Modrinth's mod catalog has outgrown the offset ceiling — the tail is unreachable and the crawl wraps early"
        )
    }
}
