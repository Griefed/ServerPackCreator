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

/**
 * Pins the two-phase crawl: [CatalogCrawler.nextBatch] hands candidates out, [CatalogCrawler.commit] advances
 * the position — and only past candidates something actually ground.
 *
 * **The bug this prevents, observed in a live sweep:** the cursor used to advance the moment candidates were
 * handed out. Restarting the daemon mid-pass (twice, that day) abandoned the rest of the batch while the cursor
 * had already moved past it, so those projects were not revisited until the *next full sweep* — weeks or months
 * later at real throughput. Frequent restarts would have left coverage systematically patchy at the tail, and
 * nothing in the logs would have said so.
 *
 * Granularity is per **page**, not per candidate: a partitioned source can cross partitions inside one page, so
 * a candidate's exact catalog position is not recoverable from outside. Re-handing a whole page is nearly free
 * because everything already ground in it now has a fresh verdict and is skipped in microseconds.
 */
internal class CatalogCrawlCommitTest {

    /** A catalog of [catalogSize] synthetic projects, served from whatever offset is asked for. */
    private class FakeSource(override val platform: String, private val catalogSize: Int) : CandidateSource {
        val requestedOffsets = mutableListOf<Int>()
        override fun page(offset: Int, limit: Int, partition: String?): CandidatePage {
            requestedOffsets.add(offset)
            val candidates = (offset until minOf(offset + limit, catalogSize)).map {
                GrindCandidate("https://example.invalid/$platform/mod$it", "mod$it", (catalogSize - it).toLong(), platform)
            }
            return CandidatePage(candidates, offset + candidates.size, endOfCatalog = offset + limit >= catalogSize)
        }
    }

    /** The core property: handing out is not progress. */
    @Test
    fun handingOutCandidatesDoesNotMoveTheCursor() {
        val source = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 3)

        val first = crawler.nextBatch()

        Assertions.assertEquals(CatalogCursor.START, cursors.cursor("Modrinth"), "nothing is committed yet")
        Assertions.assertEquals(
            first.candidates, crawler.nextBatch().candidates,
            "without a commit the same slice is handed out again — nothing may be skipped on the strength of a hand-out"
        )
    }

    @Test
    fun committingAFullyGroundBatchAdvancesPastIt() {
        val source = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 3)
        val batch = crawler.nextBatch()

        crawler.commit(batch, batch.candidates.toSet())

        Assertions.assertEquals(CatalogCursor(offset = 3, sweeps = 0), cursors.cursor("Modrinth"))
        Assertions.assertEquals(listOf("mod3", "mod4", "mod5"), crawler.nextBatch().candidates.map { it.slug })
    }

    /**
     * The abandoned-pass case. The cursor must stay at the page's start, so **every** candidate in it — including
     * the ones already ground — is handed out again. Re-grinding the done ones costs a fresh-verdict skip; losing
     * the un-ground ones would cost a whole sweep.
     */
    @Test
    fun aPartiallyGroundBatchRewindsToThePageStart() {
        val source = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 4)
        val batch = crawler.nextBatch()
        val groundTwoOfFour = batch.candidates.take(2).toSet()

        crawler.commit(batch, groundTwoOfFour)

        Assertions.assertEquals(CatalogCursor.START, cursors.cursor("Modrinth"), "held at the page start")
        Assertions.assertEquals(
            listOf("mod0", "mod1", "mod2", "mod3"), crawler.nextBatch().candidates.map { it.slug },
            "the two that were never ground come back — with the two that were, harmlessly"
        )
    }

    @Test
    fun anEntirelyAbandonedPassLeavesTheCursorUntouched() {
        val source = FakeSource("Modrinth", catalogSize = 100)
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 40, sweeps = 2)) }
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 5)
        val batch = crawler.nextBatch()

        crawler.commit(batch, emptySet())

        Assertions.assertEquals(CatalogCursor(offset = 40, sweeps = 2), cursors.cursor("Modrinth"))
    }

    /** Each source is committed on its own evidence — one stalling must not hold the other back. */
    @Test
    fun sourcesAreCommittedIndependently() {
        val modrinth = FakeSource("Modrinth", catalogSize = 100)
        val curseForge = FakeSource("CurseForge", catalogSize = 100)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(modrinth, curseForge), cursors, batchSize = 2)
        val batch = crawler.nextBatch()
        // Everything from Modrinth got ground; CurseForge's slice was abandoned.
        val ground = batch.candidates.filter { it.platform == "Modrinth" }.toSet()

        crawler.commit(batch, ground)

        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"), "advanced")
        Assertions.assertEquals(CatalogCursor.START, cursors.cursor("CurseForge"), "held")
    }

    /** A sweep may only be counted when the page that ended the catalog was itself fully ground. */
    @Test
    fun aPartiallyGroundEndOfCatalogPageDoesNotCountASweep() {
        val source = FakeSource("Modrinth", catalogSize = 4)
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 2, sweeps = 0)) }
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 4)
        val batch = crawler.nextBatch()

        Assertions.assertTrue(batch.sweepCompleted, "the source did report the end of its catalog")
        crawler.commit(batch, batch.candidates.take(1).toSet())

        Assertions.assertEquals(
            CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"),
            "no wrap and no sweep increment while part of the final page is still un-ground"
        )
    }

    @Test
    fun aFullyGroundEndOfCatalogPageWrapsAndCountsTheSweep() {
        val source = FakeSource("Modrinth", catalogSize = 4)
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 2, sweeps = 1)) }
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 4)
        val batch = crawler.nextBatch()

        crawler.commit(batch, batch.candidates.toSet())

        Assertions.assertEquals(CatalogCursor(offset = 0, sweeps = 2, partition = null), cursors.cursor("Modrinth"))
    }

    /**
     * A wrap that also fetched the new sweep's head produces two pages for one source. Grinding the tail but not
     * the head must land the cursor at the *new sweep's start* — the sweep genuinely finished, and the head page
     * has to come back intact.
     */
    @Test
    fun aWrapWhoseHeadPageWasNotGroundLandsAtTheNewSweepStart() {
        val source = FakeSource("Modrinth", catalogSize = 6)
        // Position past the end, so the tail page comes back empty and the crawler wraps + fetches the head.
        val cursors = InMemoryCursorStore().apply { store("Modrinth", CatalogCursor(offset = 9, sweeps = 0)) }
        val crawler = CatalogCrawler(listOf(source), cursors, batchSize = 2)
        val batch = crawler.nextBatch()

        Assertions.assertEquals(listOf(9, 0), source.requestedOffsets, "precondition: tail then head")
        crawler.commit(batch, emptySet()) // the head page's candidates were never ground

        Assertions.assertEquals(
            CatalogCursor(offset = 0, sweeps = 1, partition = null), cursors.cursor("Modrinth"),
            "the empty tail page committed the wrap; the un-ground head page did not advance past itself"
        )
        Assertions.assertEquals(listOf("mod0", "mod1"), crawler.nextBatch().candidates.map { it.slug })
    }

    /** A source whose request failed contributes no page, so it neither advances nor blocks. */
    @Test
    fun aFailedSourceContributesNoPageToCommit() {
        val throwing = object : CandidateSource {
            override val platform = "CurseForge"
            override fun page(offset: Int, limit: Int, partition: String?): CandidatePage =
                throw IllegalStateException("down")
        }
        val healthy = FakeSource("Modrinth", catalogSize = 10)
        val cursors = InMemoryCursorStore()
        val crawler = CatalogCrawler(listOf(throwing, healthy), cursors, batchSize = 2)
        val batch = crawler.nextBatch()

        crawler.commit(batch, batch.candidates.toSet())

        Assertions.assertEquals(CatalogCursor.START, cursors.cursor("CurseForge"))
        Assertions.assertEquals(CatalogCursor(offset = 2, sweeps = 0), cursors.cursor("Modrinth"))
    }
}
