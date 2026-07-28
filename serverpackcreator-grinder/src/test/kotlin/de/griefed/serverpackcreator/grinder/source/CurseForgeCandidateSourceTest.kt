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
import java.io.IOException

/**
 * Pins the CurseForge candidate source against canned search JSON (no network): mods map to candidates
 * preserving the API's download order, the project link is the `links.websiteUrl` (falling back to one
 * built from the slug), pagination spans `index` pages and stops when the catalog is exhausted, a failed
 * page returns what was already gathered, and `limit = 0` fetches nothing.
 */
internal class CurseForgeCandidateSourceTest {

    /** Canned [HttpFetcher] serving a page per `index=`, optionally throwing at one index. */
    private class CannedSearch(
        private val pagesByIndex: Map<Int, String>,
        private val throwAtIndex: Int? = null
    ) : HttpFetcher {
        val requestedIndexes = mutableListOf<Int>()
        override fun get(url: String, headers: Map<String, String>): String {
            val index = Regex("index=(\\d+)").find(url)!!.groupValues[1].toInt()
            requestedIndexes.add(index)
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
        val candidates = CurseForgeCandidateSource("key", fetcher).candidates(limit = 2)

        Assertions.assertEquals(listOf("jei", "jade"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(900_000_000L, 500_000_000L), candidates.map { it.popularity })
        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/jei", candidates.first().projectUrl)
    }

    @Test
    fun fallsBackToASlugBuiltUrlWhenWebsiteUrlIsMissing() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("appleskin", 100L, null))))
        val candidate = CurseForgeCandidateSource("key", fetcher).candidates(limit = 1).single()

        Assertions.assertEquals("https://www.curseforge.com/minecraft/mc-mods/appleskin", candidate.projectUrl)
    }

    @Test
    fun paginatesAcrossPagesUpToTheLimit() {
        val fetcher = CannedSearch(
            mapOf(
                0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b")),
                2 to searchJson(Triple("c", 7L, "u/c"), Triple("d", 6L, "u/d"))
            )
        )
        val candidates = CurseForgeCandidateSource("key", fetcher, pageSize = 2).candidates(limit = 3)

        Assertions.assertEquals(listOf("a", "b", "c"), candidates.map { it.slug })
        Assertions.assertEquals(listOf(0, 2), fetcher.requestedIndexes)
    }

    @Test
    fun stopsWhenTheCatalogIsExhausted() {
        val fetcher = CannedSearch(mapOf(0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b"))))
        val candidates = CurseForgeCandidateSource("key", fetcher, pageSize = 50).candidates(limit = 100)

        Assertions.assertEquals(2, candidates.size)
        Assertions.assertEquals(listOf(0), fetcher.requestedIndexes)
    }

    @Test
    fun aFailedPageReturnsWhatWasAlreadyGathered() {
        val fetcher = CannedSearch(
            mapOf(0 to searchJson(Triple("a", 9L, "u/a"), Triple("b", 8L, "u/b"))),
            throwAtIndex = 2
        )
        val candidates = CurseForgeCandidateSource("key", fetcher, pageSize = 2).candidates(limit = 10)

        Assertions.assertEquals(listOf("a", "b"), candidates.map { it.slug })
    }

    @Test
    fun limitZeroFetchesNothing() {
        val fetcher = CannedSearch(emptyMap())
        val candidates = CurseForgeCandidateSource("key", fetcher).candidates(limit = 0)

        Assertions.assertTrue(candidates.isEmpty())
        Assertions.assertTrue(fetcher.requestedIndexes.isEmpty(), "limit 0 must not hit the API")
    }
}
