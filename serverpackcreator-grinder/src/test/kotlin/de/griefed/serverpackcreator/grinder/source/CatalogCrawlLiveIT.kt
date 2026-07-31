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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Live-API check for the crawl, **gated behind `GRINDER_LIVE_IT=1`** so a normal (offline) run skips it. The
 * unit tests pin the crawler against a fake catalog; this pins the two *platform* assumptions the whole
 * "eventually covers everything" claim rests on and which no fake can prove:
 *
 * 1. Modrinth actually serves the offsets the crawler walks (deep offsets return real, *different* projects
 *    rather than repeating the head or erroring), and
 * 2. the position survives a restart, so consecutive runs keep moving forward.
 *
 * It is deliberately tiny (a handful of search calls, no downloads, no containers): run it after touching
 * paging or the cursor with
 * `GRINDER_LIVE_IT=1 ./gradlew :serverpackcreator-grinder:test --tests "*CatalogCrawlLiveIT"`.
 */
@EnabledIfEnvironmentVariable(named = "GRINDER_LIVE_IT", matches = "1")
internal class CatalogCrawlLiveIT {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun consecutiveBatchesWalkForwardThroughTheLiveModrinthCatalog() {
        val cursorFile = File(tempDir.toFile(), "cursors.json")
        val source = ModrinthCandidateSource()
        val crawler = CatalogCrawler(listOf(source), JsonCursorStore(cursorFile), batchSize = 5)

        val first = crawler.nextBatch()
        val second = crawler.nextBatch()

        Assertions.assertEquals(5, first.candidates.size, "the live catalog must fill a 5-project slice")
        Assertions.assertEquals(5, second.candidates.size)
        Assertions.assertTrue(
            (first.candidates.map { it.slug }.toSet() intersect second.candidates.map { it.slug }.toSet()).isEmpty(),
            "the second batch must be different projects, not the same head again"
        )
        Assertions.assertFalse(first.sweepCompleted, "Modrinth's catalog is far larger than 10 projects")
        Assertions.assertEquals(CatalogCursor(offset = 10, sweeps = 0), JsonCursorStore(cursorFile).cursor("Modrinth"))
    }

    /** A restart must continue the crawl, which is what makes coverage accumulate over weeks. */
    @Test
    fun aRestartResumesWhereTheCrawlLeftOff() {
        val cursorFile = File(tempDir.toFile(), "cursors.json")
        val firstRun = CatalogCrawler(listOf(ModrinthCandidateSource()), JsonCursorStore(cursorFile), batchSize = 5)
        val before = firstRun.nextBatch().candidates.map { it.slug }

        // A brand-new crawler over a brand-new store instance — as after a service restart.
        val afterRestart = CatalogCrawler(listOf(ModrinthCandidateSource()), JsonCursorStore(cursorFile), batchSize = 5)
        val after = afterRestart.nextBatch().candidates.map { it.slug }

        Assertions.assertTrue(
            (before.toSet() intersect after.toSet()).isEmpty(),
            "a restart re-served the same projects: $before then $after"
        )
        Assertions.assertEquals(CatalogCursor(offset = 10, sweeps = 0), JsonCursorStore(cursorFile).cursor("Modrinth"))
    }

    /**
     * The deep tail is reachable — the property that separates "crawls the catalog" from "re-checks the
     * popular head". Modrinth clamps `offset` at 99 999; well before that it must still serve real projects.
     */
    @Test
    fun aDeepOffsetServesRealProjects() {
        val deep = ModrinthCandidateSource().page(offset = 40_000, limit = 5)

        Assertions.assertEquals(5, deep.candidates.size, "offset 40 000 must still return mod projects")
        Assertions.assertFalse(deep.endOfCatalog)
        Assertions.assertEquals(40_005, deep.nextOffset)
        Assertions.assertTrue(deep.candidates.all { it.slug.isNotBlank() })
    }
}
