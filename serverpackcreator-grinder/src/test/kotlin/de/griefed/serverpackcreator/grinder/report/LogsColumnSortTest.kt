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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.grinder.GrindVerdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Sorting by the **Logs** column.
 *
 * Logs was originally exempt from both filtering and sorting, on the reasoning that it is not derivable
 * from a [GrindVerdict]. Filtering it still is exempt, but sorting is not meaningless at all: **not every
 * entry gets logs** — artifacts are kept only for boots that did not survive, and the reaper drops the
 * oldest attempts once the budget is passed — so "show me the rows that actually have something to read"
 * is exactly the question a maintainer opens this table to ask.
 *
 * The count comes from a lookup rather than the verdict, so the sort takes one too. It is the same
 * per-request directory snapshot the renderer already uses; nothing lists the store per row.
 */
internal class LogsColumnSortTest {

    private val store = listOf(
        grindVerdict("jei", "Forge"),
        grindVerdict("sodium", "Fabric"),
        grindVerdict("iron-chests", "NeoForge"),
        grindVerdict("create", "Quilt")
    )

    /** Three logs for `sodium`, one for `create`, none for the rest — the uneven shape the sort exists for. */
    private val logCounts: (GrindVerdict) -> Int = { verdict ->
        when (verdict.slug) {
            "sodium" -> 3
            "create" -> 1
            else -> 0
        }
    }

    private fun order(raw: String) = VerdictSelection
        .select(store, VerdictQuery.parse(QueryParams.parse(raw), 250), logCounts)
        .rows.map { it.slug }

    @Test
    fun sortingByLogsOrdersByHowManyEachRowHas() {
        Assertions.assertEquals(
            listOf("iron-chests", "jei", "create", "sodium"), order("sort=logs"),
            "ascending must run fewest-first, with the two log-less rows tie-broken by slug"
        )
    }

    /** Descending is the direction a maintainer actually wants: the rows with the most to read on top. */
    @Test
    fun sortingByLogsDescendingPutsTheRichestRowsFirst() {
        Assertions.assertEquals(
            listOf("sodium", "create", "iron-chests", "jei"), order("sort=logs&dir=desc"),
            "descending must lead with the most logs, and still tie-break log-less rows by slug"
        )
    }

    /**
     * The tie-break is by slug in **both** directions, deliberately unlike the field sorts, which reverse
     * their whole comparator. Reversing the tie-break too would shuffle every log-less row when a reader
     * merely flipped the arrow, and those rows are the majority.
     */
    @Test
    fun theTieBreakIsStableWhicheverWayTheSortRuns() {
        val ascendingTail = order("sort=logs").take(2)
        val descendingTail = order("sort=logs&dir=desc").takeLast(2)

        Assertions.assertEquals(
            ascendingTail, descendingTail,
            "the log-less rows must keep one order however the sort is flipped"
        )
    }

    /** A bookmarked or shared `sort=logs` has to survive the round trip like any other column. */
    @Test
    fun theLogsSortRoundTripsThroughTheUrl() {
        val parsed = VerdictQuery.parse(QueryParams.parse("sort=logs&dir=desc"), 250)

        Assertions.assertEquals("?sort=logs&dir=desc", parsed.toQueryString())
        Assertions.assertEquals(listOf("sodium", "create", "iron-chests", "jei"), order(parsed.toQueryString().removePrefix("?")))
    }

    /** Without a lookup the sort must not throw or reorder — every row simply has nothing. */
    @Test
    fun theSortIsHarmlessWhenNothingKnowsAboutLogs() {
        val rows = VerdictSelection.select(store, VerdictQuery.parse(QueryParams.parse("sort=logs"), 250)).rows

        Assertions.assertEquals(store.size, rows.size)
    }

    /** The header must be a sort link like every other column, or the feature is unreachable by clicking. */
    @Test
    fun theLogsHeaderIsASortLink() {
        val page = VerdictSelection.select(store, VerdictQuery.parse(QueryParams.parse(null), 250), logCounts)
        val html = VerdictReportRenderer.toHtml(page)

        Assertions.assertTrue(
            html.contains("sort=logs"),
            "the Logs header renders no sort link, so a reader cannot reach the sort at all"
        )
    }
}
