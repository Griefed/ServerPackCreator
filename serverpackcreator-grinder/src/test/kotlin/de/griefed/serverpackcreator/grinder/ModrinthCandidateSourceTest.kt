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
package de.griefed.serverpackcreator.grinder

import de.griefed.serverpackcreator.clientside.HttpFetcher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Pins the Modrinth candidate source against canned search JSON (no network): hits map to candidates
 * preserving the API's download order, pagination spans pages and stops when the catalog is exhausted,
 * a failed page returns what was already gathered, and `limit = 0` fetches nothing.
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
        val candidates = ModrinthCandidateSource(fetcher).candidates(limit = 3)

        Assertions.assertEquals(listOf("sodium", "jei", "create"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(50_000_000L, 40_000_000L, 30_000_000L), candidates.map { it.popularity })
        Assertions.assertEquals("https://modrinth.com/mod/sodium", candidates.first().projectUrl)
    }

    @Test
    fun paginatesAcrossPagesUpToTheLimit() {
        val fetcher = CannedSearch(
            mapOf(
                0 to searchJson("a" to 9, "b" to 8),
                2 to searchJson("c" to 7, "d" to 6)
            )
        )
        val candidates = ModrinthCandidateSource(fetcher, pageSize = 2).candidates(limit = 3)

        Assertions.assertEquals(listOf("a", "b", "c"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(0, 2), fetcher.requestedOffsets)
    }

    @Test
    fun stopsWhenTheCatalogIsExhausted() {
        val fetcher = CannedSearch(mapOf(0 to searchJson("a" to 9, "b" to 8)))
        // Asks for 100 but only two exist; the short page ends pagination without a wasted extra call.
        val candidates = ModrinthCandidateSource(fetcher, pageSize = 100).candidates(limit = 100)

        Assertions.assertEquals(2, candidates.size)
        Assertions.assertEquals(listOf(0), fetcher.requestedOffsets)
    }

    @Test
    fun aFailedPageReturnsWhatWasAlreadyGathered() {
        val fetcher = CannedSearch(mapOf(0 to searchJson("a" to 9, "b" to 8)), throwAtOffset = 2)
        val candidates = ModrinthCandidateSource(fetcher, pageSize = 2).candidates(limit = 10)

        Assertions.assertEquals(listOf("a", "b"), candidates.map { it.slug })
    }

    @Test
    fun limitZeroFetchesNothing() {
        val fetcher = CannedSearch(emptyMap())
        val candidates = ModrinthCandidateSource(fetcher).candidates(limit = 0)

        Assertions.assertTrue(candidates.isEmpty())
        Assertions.assertTrue(fetcher.requestedOffsets.isEmpty(), "limit 0 must not hit the API")
    }
}
