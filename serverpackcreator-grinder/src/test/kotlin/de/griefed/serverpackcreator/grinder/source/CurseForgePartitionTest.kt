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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the CurseForge partition plan — the traversal that gets past the platform's 10 000-result paging cap.
 * It is deliberately a pure function of (current partition, that partition's `totalCount`, version list) so
 * the *only* code that decides what gets crawled is testable without an API key, which this module does not
 * have. Every rule here is a coverage decision: a wrong "next" silently skips part of the catalog.
 */
internal class CurseForgePartitionTest {

    private val versions = listOf("1.21.1", "1.20.1", "1.12.2")
    private val underCap = CurseForgeCandidateSource.MAX_INDEX - 1
    private val overCap = CurseForgeCandidateSource.MAX_INDEX + 1

    /** Every sweep opens with the unfiltered, most-downloaded-first slice, preserving the popularity premise. */
    @Test
    fun aSweepStartsWithTheWholeCatalogMostDownloadedFirst() {
        val first = CurseForgePartitions.FIRST

        Assertions.assertNull(first.gameVersion, "the opening partition filters by nothing")
        Assertions.assertNull(first.modLoaderType)
        Assertions.assertFalse(first.ascending, "most-downloaded first")
    }

    /**
     * The unfiltered slice is *always* bigger than the cap, so splitting it would be pointless — the
     * per-version partitions are what reach past it. It therefore hands straight over to the newest version.
     */
    @Test
    fun theWholeCatalogPartitionHandsOverToTheNewestVersion() {
        val next = CurseForgePartitions.next(CurseForgePartitions.FIRST, totalCount = 250_000, versions = versions)

        Assertions.assertEquals(CurseForgePartition("1.21.1", null, ascending = false), next)
    }

    @Test
    fun aVersionThatFitsUnderTheCapMovesStraightToTheNextVersion() {
        val next = CurseForgePartitions.next(
            CurseForgePartition("1.21.1", null, ascending = false), totalCount = underCap, versions = versions
        )

        Assertions.assertEquals(
            CurseForgePartition("1.20.1", null, ascending = false), next,
            "a version whose mods are all reachable needs no loader split"
        )
    }

    /** Over the cap, the version is re-crawled per modloader — each slice small enough to page through. */
    @Test
    fun aVersionOverTheCapSplitsByModLoader() {
        val next = CurseForgePartitions.next(
            CurseForgePartition("1.20.1", null, ascending = false), totalCount = overCap, versions = versions
        )

        Assertions.assertEquals(
            CurseForgePartition("1.20.1", CurseForgePartitions.FORGE, ascending = false), next,
            "the split starts with the first modloader of the same version"
        )
    }

    @Test
    fun loaderSlicesUnderTheCapWalkThroughEveryLoaderThenTheNextVersion() {
        var current = CurseForgePartition("1.20.1", CurseForgePartitions.FORGE, ascending = false)
        val visited = mutableListOf<Int?>()

        // Walk the whole loader split, each slice comfortably under the cap.
        while (current.gameVersion == "1.20.1" && current.modLoaderType != null) {
            visited.add(current.modLoaderType)
            current = CurseForgePartitions.next(current, underCap, versions)!!
        }

        Assertions.assertEquals(
            listOf(
                CurseForgePartitions.FORGE, CurseForgePartitions.CAULDRON, CurseForgePartitions.LITELOADER,
                CurseForgePartitions.FABRIC, CurseForgePartitions.QUILT, CurseForgePartitions.NEOFORGE
            ),
            visited,
            "every documented modloader must be crawled, or its mods are unreachable"
        )
        Assertions.assertEquals(CurseForgePartition("1.12.2", null, ascending = false), current)
    }

    /**
     * A loader slice that *itself* exceeds the cap is crawled from both ends: descending reaches the top
     * 10 000 by downloads, ascending the bottom 10 000, so a slice of up to 20 000 is covered completely.
     */
    @Test
    fun aLoaderSliceOverTheCapIsAlsoCrawledFromTheBottom() {
        val forgeDescending = CurseForgePartition("1.20.1", CurseForgePartitions.FORGE, ascending = false)

        val next = CurseForgePartitions.next(forgeDescending, totalCount = overCap, versions = versions)

        Assertions.assertEquals(forgeDescending.copy(ascending = true), next, "same slice, least-downloaded first")
    }

    /** After the bottom-up pass the plan moves on regardless of size — the middle of a >20 000 slice is lost. */
    @Test
    fun anAscendingSliceAlwaysMovesOnToTheNextLoader() {
        val next = CurseForgePartitions.next(
            CurseForgePartition("1.20.1", CurseForgePartitions.FORGE, ascending = true),
            totalCount = 100_000,
            versions = versions
        )

        Assertions.assertEquals(CurseForgePartition("1.20.1", CurseForgePartitions.CAULDRON, ascending = false), next)
    }

    @Test
    fun theLastLoaderOfTheLastVersionEndsTheCatalog() {
        val last = CurseForgePartition("1.12.2", CurseForgePartitions.NEOFORGE, ascending = false)

        Assertions.assertNull(
            CurseForgePartitions.next(last, underCap, versions),
            "nothing left to crawl ⇒ end of catalog, which makes the crawler wrap and start a new sweep"
        )
    }

    @Test
    fun theLastVersionUnderTheCapEndsTheCatalog() {
        val last = CurseForgePartition("1.12.2", null, ascending = false)

        Assertions.assertNull(CurseForgePartitions.next(last, underCap, versions))
    }

    /** Without a version list there is nothing to partition, so the sweep is just the unfiltered slice. */
    @Test
    fun anEmptyVersionListEndsAfterTheWholeCatalogPartition() {
        Assertions.assertNull(
            CurseForgePartitions.next(CurseForgePartitions.FIRST, totalCount = 250_000, versions = emptyList())
        )
    }

    /** A version that vanished from the platform's list must not dead-end the crawl. */
    @Test
    fun aVersionNoLongerInTheListFallsForwardToTheNewestOne() {
        val next = CurseForgePartitions.next(
            CurseForgePartition("1.19.9-removed", null, ascending = false), underCap, versions
        )

        Assertions.assertEquals(CurseForgePartition("1.21.1", null, ascending = false), next)
    }

    @Test
    fun partitionKeysRoundTrip() {
        val partitions = listOf(
            CurseForgePartitions.FIRST,
            CurseForgePartition("1.20.1", null, ascending = false),
            CurseForgePartition("1.20.1", CurseForgePartitions.FABRIC, ascending = true),
            CurseForgePartition("1.20.1-Snapshot", CurseForgePartitions.NEOFORGE, ascending = false)
        )

        partitions.forEach { partition ->
            Assertions.assertEquals(partition, CurseForgePartition.parse(partition.key), "round trip of ${partition.key}")
        }
    }

    /** An unreadable token restarts the sweep rather than crashing the daemon or silently crawling nothing. */
    @Test
    fun anUnreadableKeyParsesBackToTheStartOfTheSweep() {
        listOf("", "garbage", "1.20.1", "1.20.1|notanumber|desc", "1.20.1|1|sideways").forEach {
            Assertions.assertEquals(CurseForgePartitions.FIRST, CurseForgePartition.parse(it), "token '$it'")
        }
    }

    /**
     * Version order decides what gets crawled first, and — because the cursor stores a partition *key* — a
     * stable order keeps a restart in roughly the same place. Newest first, unparsable strings last.
     */
    @Test
    fun versionsAreOrderedNewestFirstWithUnparsableOnesLast() {
        val ordered = CurseForgePartitions.orderVersions(
            listOf("1.9.4", "1.21.11", "Forge", "1.20.1", "1.21.2", "1.12.2", "26.2", "1.21")
        )

        Assertions.assertEquals(
            listOf("26.2", "1.21.11", "1.21.2", "1.21", "1.20.1", "1.12.2", "1.9.4", "Forge"),
            ordered,
            "numeric components compare as numbers (1.21.11 above 1.21.2), not as text"
        )
    }

    @Test
    fun orderingIsStableForDuplicateAndBlankEntries() {
        val ordered = CurseForgePartitions.orderVersions(listOf("1.20.1", "", "1.20.1", " ", "1.21"))

        Assertions.assertEquals(listOf("1.21", "1.20.1", "1.20.1"), ordered, "blank version strings are dropped")
    }
}
