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

import de.griefed.serverpackcreator.grinder.GrindCandidate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Pins the catalog crawler — the piece that turns a top-N re-verifier into something that eventually
 * covers a whole catalog. What matters here is the *position*: consecutive batches must walk forward, the
 * position must survive a restart, a wrap-around must start a new sweep, and a failed request must **not**
 * be mistaken for the end of the catalog (which would silently restart a deep crawl at the top).
 */
internal class CatalogCrawlerTest {

    /**
     * A catalog of [catalogSize] synthetic projects, served from whatever offset is asked for. Records the
     * offsets it was asked for so the crawl path itself is observable. `failAtOffset` returns the
     * failed-request shape (nothing gathered, catalog *not* ended); `throwAtOffset` blows up entirely.
     */
    private class FakeSource(
        override val platform: String,
        private val catalogSize: Int,
        private val failAtOffset: Int? = null,
        private val throwAtOffset: Int? = null
    ) : CandidateSource {
        val requestedOffsets = mutableListOf<Int>()

        override fun page(offset: Int, limit: Int): CandidatePage {
            requestedOffsets.add(offset)
            if (offset == throwAtOffset) throw IllegalStateException("$platform exploded at $offset")
            if (offset == failAtOffset) return CandidatePage(emptyList(), offset, endOfCatalog = false)
            val candidates = (offset until minOf(offset + limit, catalogSize)).map {
                GrindCandidate("https://example.invalid/$platform/mod$it", "mod$it", (catalogSize - it).toLong(), platform)
            }
            return CandidatePage(candidates, offset + candidates.size, endOfCatalog = offset + limit >= catalogSize)
        }
    }

    @Test
    fun advancesThroughTheCatalogAcrossBatches() {
        val source = FakeSource("Modrinth", catalogSize = 5)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 2)

        val first = crawler.nextBatch()
        val second = crawler.nextBatch()

        Assertions.assertEquals(listOf("mod0", "mod1"), first.candidates.map { it.slug })
        Assertions.assertEquals(listOf("mod2", "mod3"), second.candidates.map { it.slug }, "the second batch must move on")
        Assertions.assertEquals(listOf(0, 2), source.requestedOffsets)
        Assertions.assertEquals(CatalogCursor(offset = 4, sweeps = 0), cursors.cursor("Modrinth"))
        Assertions.assertFalse(second.sweepCompleted, "the catalog still has projects ahead")
    }

    @Test
    fun wrapsAroundAndCountsASweepWhenTheCatalogEnds() {
        val source = FakeSource("Modrinth", catalogSize = 5)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 2)

        crawler.nextBatch() // mod0, mod1
        crawler.nextBatch() // mod2, mod3
        val last = crawler.nextBatch() // mod4, and the catalog ends

        Assertions.assertEquals(listOf("mod4"), last.candidates.map { it.slug })
        Assertions.assertTrue(last.sweepCompleted, "reaching the end of the catalog completes a sweep")
        Assertions.assertEquals(CatalogCursor(offset = 0, sweeps = 1), cursors.cursor("Modrinth"))
        Assertions.assertEquals(listOf("mod0", "mod1"), crawler.nextBatch().candidates.map { it.slug }, "next sweep restarts at the top")
    }

    /** The restart property: a fresh crawler over the same store continues, it does not start over. */
    @Test
    fun resumesFromThePersistedPosition() {
        val source = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 40, sweeps = 3)) }

        val batch = CatalogCrawler(listOf(source), cursors, batchSize = 2).nextBatch()

        Assertions.assertEquals(listOf(40), source.requestedOffsets)
        Assertions.assertEquals(listOf("mod40", "mod41"), batch.candidates.map { it.slug })
        Assertions.assertEquals(CatalogCursor(offset = 42, sweeps = 3), cursors.cursor("Modrinth"), "the sweep count carries over")
    }

    /**
     * The failure mode this design exists to avoid: a failed request must leave the position alone, so the
     * crawler retries the same region on the next pass instead of throwing away a deep crawl and restarting
     * at the most-downloaded mods.
     */
    @Test
    fun aFailedRequestKeepsThePositionAndDoesNotCountASweep() {
        val source = FakeSource("Modrinth", catalogSize = 100, failAtOffset = 2)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 2)

        crawler.nextBatch() // mod0, mod1 -> offset 2
        val failed = crawler.nextBatch() // fails at offset 2

        Assertions.assertTrue(failed.candidates.isEmpty())
        Assertions.assertFalse(failed.sweepCompleted, "a failed request is not the end of the catalog")
        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"))

        crawler.nextBatch()
        Assertions.assertEquals(listOf(0, 2, 2), source.requestedOffsets, "the failed region is retried, not skipped")
    }

    @Test
    fun crawlsEverySourceIndependentlyAndUnionsTheirCandidates() {
        val modrinth = FakeSource("Modrinth", catalogSize = 100)
        val curseForge = FakeSource("CurseForge", catalogSize = 100)
        val cursors = InMemoryCursorStore().apply { store("CurseForge", CatalogCursor(offset = 50, sweeps = 0)) }
        val crawler = CatalogCrawler(listOf(modrinth, curseForge), cursors, batchSize = 2)

        val batch = crawler.nextBatch()

        Assertions.assertEquals(
            setOf("Modrinth" to "mod0", "Modrinth" to "mod1", "CurseForge" to "mod50", "CurseForge" to "mod51"),
            batch.candidates.map { it.platform to it.slug }.toSet()
        )
        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"))
        Assertions.assertEquals(CatalogCursor(offset = 52, sweeps = 0), cursors.cursor("CurseForge"))
    }

    /**
     * A position that already sits past the end (the catalog shrank, or the platform's reachable range did)
     * wraps *and* fetches the new sweep's first slice in the same batch, so the pass is not wasted idling.
     */
    @Test
    fun aPositionPastTheEndWrapsAndStillReturnsTheHeadInTheSameBatch() {
        val source = FakeSource("Modrinth", catalogSize = 5)
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 10, sweeps = 0)) }

        val batch = CatalogCrawler(listOf(source), cursors, batchSize = 2).nextBatch()

        Assertions.assertEquals(listOf(10, 0), source.requestedOffsets, "wrap, then take the head straight away")
        Assertions.assertEquals(listOf("mod0", "mod1"), batch.candidates.map { it.slug })
        Assertions.assertTrue(batch.sweepCompleted)
        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 1), cursors.cursor("Modrinth"))
    }

    /** The wrap-and-retry must not turn an empty catalog into an endless request loop. */
    @Test
    fun anEmptyCatalogIsRequestedOnlyOnce() {
        val source = FakeSource("Modrinth", catalogSize = 0)
        val cursors = InMemoryCursorStore()

        val batch = CatalogCrawler(listOf(source), cursors, batchSize = 2).nextBatch()

        Assertions.assertTrue(batch.candidates.isEmpty())
        Assertions.assertEquals(listOf(0), source.requestedOffsets, "already at the head — nothing to wrap to")
        Assertions.assertTrue(batch.sweepCompleted)
        Assertions.assertEquals(CatalogCursor(offset = 0, sweeps = 1), cursors.cursor("Modrinth"))
    }

    /** One broken source cannot end the daemon's crawl — the others still contribute. */
    @Test
    fun aThrowingSourceIsSkippedWithoutSinkingTheBatch() {
        val broken = FakeSource("CurseForge", catalogSize = 100, throwAtOffset = 0)
        val healthy = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore()

        val batch = CatalogCrawler(listOf(broken, healthy), cursors, batchSize = 2).nextBatch()

        Assertions.assertEquals(listOf("mod0", "mod1"), batch.candidates.map { it.slug })
        Assertions.assertEquals(CatalogCursor(offset = 0, sweeps = 0), cursors.cursor("CurseForge"), "a thrown page leaves the position alone")
        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"))
    }

    @Test
    fun rejectsANonPositiveBatchSize() {
        assertThrows<IllegalArgumentException> {
            CatalogCrawler(listOf(FakeSource("Modrinth", 10)), InMemoryCursorStore(), batchSize = 0)
        }
    }
}
