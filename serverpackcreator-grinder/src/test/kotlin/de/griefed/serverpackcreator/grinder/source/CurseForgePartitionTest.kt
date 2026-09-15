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
 * It is deliberately a pure function of (current partition, that partition's `totalCount`, version list,
 * category list) so the *only* code that decides what gets crawled is testable without an API key, which this
 * module does not have. Every rule here is a coverage decision: a wrong "next" silently skips part of the
 * catalog.
 *
 * The plan crawls an over-cap version along **two independent axes** — modloader and category — because
 * neither is provably total: CurseForge tags a mod with a loader only when it has one, and its own submission
 * docs disagree on whether a category is mandatory. Running both means a mod is reachable if it has *either*,
 * and the tests below assert that both stages really do run.
 */
internal class CurseForgePartitionTest {

    private val versions = listOf("1.21.1", "1.20.1", "1.12.2")
    private val categories = listOf(406, 426, 4485) // as returned by /categories?classId=6, ascending by id
    /**
     * `totalCount` as the live API actually reports it: its **true size** while a slice fits under the cap, and
     * **clamped to exactly the cap** for anything at or above it (measured 2026-07-29 — a 200 000-mod slice and
     * a 10 000-mod slice both report 10 000). A count above the cap is therefore not a value any test may use:
     * it cannot occur, and pinning the plan against it is what made the splits unreachable in the first place.
     */
    private val underCap = CurseForgeCandidateSource.MAX_INDEX - 1
    private val saturated = CurseForgeCandidateSource.MAX_INDEX

    private fun versionSlice(version: String, ascending: Boolean = false) =
        CurseForgePartition(gameVersion = version, categoryId = null, modLoaderType = null, ascending = ascending)

    private fun loaderSlice(version: String, loader: Int, ascending: Boolean = false) =
        CurseForgePartition(gameVersion = version, categoryId = null, modLoaderType = loader, ascending = ascending)

    private fun categorySlice(version: String, category: Int, ascending: Boolean = false) =
        CurseForgePartition(gameVersion = version, categoryId = category, modLoaderType = null, ascending = ascending)

    private fun categoryLoaderSlice(version: String, category: Int, loader: Int, ascending: Boolean = false) =
        CurseForgePartition(gameVersion = version, categoryId = category, modLoaderType = loader, ascending = ascending)

    private fun next(current: CurseForgePartition, totalCount: Int) =
        CurseForgePartitions.next(current, totalCount, versions, categories)

    /** Every sweep opens with the unfiltered, most-downloaded-first slice, preserving the popularity premise. */
    @Test
    fun aSweepStartsWithTheWholeCatalogMostDownloadedFirst() {
        val first = CurseForgePartitions.FIRST

        Assertions.assertNull(first.gameVersion, "the opening partition filters by nothing")
        Assertions.assertNull(first.categoryId)
        Assertions.assertNull(first.modLoaderType)
        Assertions.assertFalse(first.ascending, "most-downloaded first")
    }

    /**
     * The unfiltered slice is *always* bigger than the cap, so splitting it would be pointless — the
     * per-version partitions are what reach past it. It therefore hands straight over to the newest version.
     */
    @Test
    fun theWholeCatalogPartitionHandsOverToTheNewestVersion() {
        Assertions.assertEquals(versionSlice("1.21.1"), next(CurseForgePartitions.FIRST, totalCount = 250_000))
    }

    @Test
    fun aVersionThatFitsUnderTheCapMovesStraightToTheNextVersion() {
        Assertions.assertEquals(
            versionSlice("1.20.1"), next(versionSlice("1.21.1"), underCap),
            "a version whose mods are all reachable needs neither loader nor category split"
        )
    }

    /** Saturated, the version is re-crawled per modloader — each slice small enough to page through. */
    @Test
    fun aSaturatedVersionSplitsByModLoader() {
        Assertions.assertEquals(
            loaderSlice("1.20.1", CurseForgePartitions.FORGE), next(versionSlice("1.20.1"), saturated),
            "the split starts with the first modloader of the same version"
        )
    }

    /**
     * The whole point of the third axis: after the loader stage, an over-cap version is crawled *again* by
     * category. A mod that carries no modloader tag appears in no loader slice, so without this stage it was
     * unreachable beyond the version's top 10 000.
     */
    @Test
    fun theLoaderStageIsFollowedByTheCategoryStage() {
        val lastLoader = loaderSlice("1.20.1", CurseForgePartitions.NEOFORGE)

        Assertions.assertEquals(
            categorySlice("1.20.1", 406), next(lastLoader, underCap),
            "after the last loader the same version is crawled by category"
        )
    }

    @Test
    fun everyLoaderThenEveryCategoryIsVisitedForAnOverCapVersion() {
        val loadersVisited = mutableListOf<Int>()
        val categoriesVisited = mutableListOf<Int>()
        var current: CurseForgePartition? = next(versionSlice("1.20.1"), saturated)

        // Walk the whole of 1.20.1's split, every sub-slice comfortably under the cap.
        while (current != null && current.gameVersion == "1.20.1") {
            val slice = current
            if (slice.categoryId == null) {
                slice.modLoaderType?.let { loadersVisited.add(it) }
            } else if (slice.modLoaderType == null) {
                categoriesVisited.add(slice.categoryId)
            }
            current = next(slice, underCap)
        }

        Assertions.assertEquals(CurseForgePartitions.LOADERS, loadersVisited, "every documented modloader")
        Assertions.assertEquals(categories, categoriesVisited, "every category of the mods class")
        Assertions.assertEquals(versionSlice("1.12.2"), current, "then on to the next version")
    }

    /**
     * A loader slice that *itself* exceeds the cap is crawled from both ends: descending reaches the top
     * 10 000 by downloads, ascending the bottom 10 000, so a slice of up to 20 000 is covered completely.
     */
    @Test
    fun aLoaderSliceOverTheCapIsAlsoCrawledFromTheBottom() {
        val forgeDescending = loaderSlice("1.20.1", CurseForgePartitions.FORGE)

        Assertions.assertEquals(
            forgeDescending.copy(ascending = true), next(forgeDescending, saturated),
            "same slice, least-downloaded first"
        )
    }

    /**
     * A loader slice that is *still* saturated after both directions moves on regardless: the category stage is
     * the second path to those mods, and narrowing a loader slice further is not something the API allows.
     */
    @Test
    fun anAscendingLoaderSliceMovesOnToTheNextLoader() {
        Assertions.assertEquals(
            loaderSlice("1.20.1", CurseForgePartitions.CAULDRON),
            next(loaderSlice("1.20.1", CurseForgePartitions.FORGE, ascending = true), saturated),
            "the loader stage moves on; the category stage is the second path to those mods"
        )
    }

    @Test
    fun aCategorySliceOverTheCapIsAlsoCrawledFromTheBottom() {
        val category = categorySlice("1.20.1", 426)

        Assertions.assertEquals(category.copy(ascending = true), next(category, saturated))
    }

    /**
     * The deepest split: a category slice still reporting a saturated count after **both** sort directions have
     * been crawled may hold more than the 20 000 they reach, so it is narrowed by modloader. Saturation is the
     * only evidence available — the API will not report a size above the cap — so this rule cannot be written
     * as "more than twice the cap", which is exactly the mistake the live run exposed.
     */
    @Test
    fun aStillSaturatedCategorySliceIsNarrowedByModLoaderAfterBothDirections() {
        val exhaustedCategory = categorySlice("1.20.1", 426, ascending = true)

        Assertions.assertEquals(
            categoryLoaderSlice("1.20.1", 426, CurseForgePartitions.FORGE),
            next(exhaustedCategory, saturated),
            "category × loader is the last available narrowing"
        )
    }

    /** A category slice whose count fits under the cap was covered in one direction — no narrowing needed. */
    @Test
    fun aCategorySliceUnderTheCapMovesStraightToTheNextCategory() {
        Assertions.assertEquals(
            categorySlice("1.20.1", 4485),
            next(categorySlice("1.20.1", 426, ascending = true), underCap)
        )
    }

    @Test
    fun categoryLoaderSlicesWalkEveryLoaderThenTheNextCategory() {
        var current = categoryLoaderSlice("1.20.1", 426, CurseForgePartitions.FORGE)
        val visited = mutableListOf<Int>()

        while (current.categoryId == 426) {
            val loader = current.modLoaderType ?: break
            visited.add(loader)
            current = next(current, underCap)!!
        }

        Assertions.assertEquals(CurseForgePartitions.LOADERS, visited)
        Assertions.assertEquals(categorySlice("1.20.1", 4485), current, "then the next category of that version")
    }

    @Test
    fun aCategoryLoaderSliceOverTheCapIsAlsoCrawledFromTheBottom() {
        val deepest = categoryLoaderSlice("1.20.1", 426, CurseForgePartitions.FABRIC)

        Assertions.assertEquals(deepest.copy(ascending = true), next(deepest, saturated))
    }

    @Test
    fun theLastCategoryOfTheLastVersionEndsTheCatalog() {
        Assertions.assertNull(
            next(categorySlice("1.12.2", categories.last()), underCap),
            "nothing left to crawl ⇒ end of catalog, which makes the crawler wrap and start a new sweep"
        )
    }

    @Test
    fun theLastVersionUnderTheCapEndsTheCatalog() {
        Assertions.assertNull(next(versionSlice("1.12.2"), underCap))
    }

    /** Without a version list there is nothing to partition, so the sweep is just the unfiltered slice. */
    @Test
    fun anEmptyVersionListEndsAfterTheWholeCatalogPartition() {
        Assertions.assertNull(
            CurseForgePartitions.next(CurseForgePartitions.FIRST, 250_000, versions = emptyList(), categories = categories)
        )
    }

    /** No category list (the platform call failed) must still leave the loader stage working. */
    @Test
    fun withoutCategoriesAnOverCapVersionStillGetsItsLoaderStage() {
        val lastLoader = loaderSlice("1.20.1", CurseForgePartitions.NEOFORGE)

        Assertions.assertEquals(
            versionSlice("1.12.2"),
            CurseForgePartitions.next(lastLoader, underCap, versions, categories = emptyList()),
            "with no categories to crawl, the version is done after its loaders"
        )
    }

    /** A version that vanished from the platform's list must not dead-end the crawl. */
    @Test
    fun aVersionNoLongerInTheListFallsForwardToTheNewestOne() {
        Assertions.assertEquals(versionSlice("1.21.1"), next(versionSlice("1.19.9-removed"), underCap))
    }

    /** A category that vanished likewise falls forward instead of stalling that version. */
    @Test
    fun aCategoryNoLongerInTheListFallsForwardToTheFirstOne() {
        Assertions.assertEquals(categorySlice("1.20.1", categories.first()), next(categorySlice("1.20.1", 999_999), underCap))
    }

    @Test
    fun partitionKeysRoundTrip() {
        listOf(
            CurseForgePartitions.FIRST,
            versionSlice("1.20.1"),
            loaderSlice("1.20.1", CurseForgePartitions.FABRIC, ascending = true),
            categorySlice("1.20.1", 426),
            categoryLoaderSlice("1.20.1-Snapshot", 4485, CurseForgePartitions.NEOFORGE, ascending = true)
        ).forEach { partition ->
            Assertions.assertEquals(partition, CurseForgePartition.parse(partition.key), "round trip of ${partition.key}")
        }
    }

    /**
     * An unreadable token restarts the sweep rather than crashing the daemon or silently crawling nothing.
     * That deliberately includes the **three-field token** written before the category axis existed: one
     * re-sweep costs nothing (fresh verdicts are skipped), whereas mis-reading it would crawl the wrong slice.
     */
    @Test
    fun anUnreadableKeyParsesBackToTheStartOfTheSweep() {
        listOf("", "garbage", "1.20.1", "1.20.1|1|desc", "1.20.1|*|notanumber|desc", "1.20.1|*|1|sideways")
            .forEach { Assertions.assertEquals(CurseForgePartitions.FIRST, CurseForgePartition.parse(it), "token '$it'") }
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
