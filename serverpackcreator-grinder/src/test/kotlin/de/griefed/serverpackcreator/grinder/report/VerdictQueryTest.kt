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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.ModPlatforms
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the report's filter/sort/page selection as a **pure unit**, deliberately not through the HTTP
 * handler.
 *
 * Every edge case below — a page past the end, a junk size, a sort naming a column that no longer exists,
 * a filter value carrying `&` — costs three lines here and microseconds. Through the server each would be
 * a socket round trip asserting against a 60 KB blob. It is also what makes the table and the CSV
 * *provably* agree: they agree because they run this same function over the same query, not because two
 * renderers were kept in step by hand.
 */
internal class VerdictQueryTest {

    private val store = listOf(
        grindVerdict("jei", "Forge", confidence = Confidence.HIGH, detail = "crashed hard"),
        grindVerdict("sodium", "Fabric", confidence = Confidence.LOW, detail = "clean boot"),
        grindVerdict("iron-chests", "NeoForge", confidence = Confidence.MEDIUM, detail = "declared server"),
        grindVerdict("jei", "Fabric", confidence = Confidence.LOW, platform = ModPlatforms.CURSEFORGE)
    )

    private fun select(raw: String?) = VerdictSelection.select(store, VerdictQuery.parse(QueryParams.parse(raw), 250))

    @Test
    fun noQuerySelectsEverythingInTheDefaultOrder() {
        val page = select(null)

        Assertions.assertEquals(4, page.matched)
        Assertions.assertEquals(4, page.total)
        Assertions.assertEquals(4, page.rows.size)
        Assertions.assertEquals(Confidence.HIGH, page.rows.first().confidence, "highest confidence still leads")
    }

    @Test
    fun aChoiceFilterMatchesExactlyAndCaseInsensitively() {
        Assertions.assertEquals(2, select("f.loader=Fabric").matched)
        Assertions.assertEquals(2, select("f.loader=fabric").matched)
        Assertions.assertEquals(1, select("f.confidence=HIGH").matched)
        Assertions.assertEquals(1, select("f.platform=CurseForge").matched)
    }

    /** Filters combine with AND across columns, so each one narrows what the previous left. */
    @Test
    fun filtersOnDifferentColumnsNarrowTogether() {
        Assertions.assertEquals(1, select("f.loader=Fabric&f.platform=CurseForge").matched)
        Assertions.assertEquals(0, select("f.loader=Forge&f.platform=CurseForge").matched)
    }

    /** A repeated filter is an OR within its own column — "show me Forge or Fabric" has to be expressible. */
    @Test
    fun aRepeatedFilterIsAnOrWithinItsColumn() {
        Assertions.assertEquals(3, select("f.loader=Fabric&f.loader=Forge").matched)
    }

    /** Text columns are substring matches, which is what makes a search box useful at all. */
    @Test
    fun aTextFilterMatchesASubstring() {
        Assertions.assertEquals(2, select("f.name=jei").matched)
        Assertions.assertEquals(1, select("f.detail=clean").matched)
    }

    /** The free-text search spans every column, so a reader need not know which one holds their term. */
    @Test
    fun theSearchSpansEveryColumn() {
        Assertions.assertEquals(1, select("q=declared").matched, "matches a detail")
        Assertions.assertEquals(1, select("q=NeoForge").matched, "and a loader")
        Assertions.assertEquals(2, select("q=jei").matched, "and a name")
    }

    @Test
    fun sortingIsByColumnAndDirection() {
        Assertions.assertEquals(
            listOf("iron-chests", "jei", "jei", "sodium"),
            select("sort=name").rows.map { it.slug }
        )
        Assertions.assertEquals(
            listOf("sodium", "jei", "jei", "iron-chests"),
            select("sort=name&dir=desc").rows.map { it.slug }
        )
    }

    @Test
    fun pagingSlicesAndReportsThePageCount() {
        val page = select("size=2&page=2&sort=name")

        Assertions.assertEquals(2, page.rows.size)
        Assertions.assertEquals(2, page.pages)
        Assertions.assertEquals(2, page.page)
        Assertions.assertEquals(4, page.matched)
        Assertions.assertEquals(listOf("jei", "sodium"), page.rows.map { it.slug })
    }

    /** `all` is a real choice, not a very large number, and has to survive a round trip through the URL. */
    @Test
    fun sizeAllReturnsEverythingOnOnePage() {
        val page = select("size=all")

        Assertions.assertEquals(4, page.rows.size)
        Assertions.assertEquals(1, page.pages)
        Assertions.assertNull(page.query.size)
        Assertions.assertTrue(page.query.toQueryString().contains("size=all"))
    }

    /**
     * **Nothing a URL can carry may throw.** These arrive from a bookmark, a shared link, or somebody
     * editing the address bar, and a report that 500s is worse than one that quietly falls back.
     */
    @Test
    fun junkInTheQueryFallsBackInsteadOfThrowing() {
        Assertions.assertEquals(4, select("page=0&size=banana&sort=nonexistent&dir=sideways").matched)
        Assertions.assertEquals(1, select("page=0").page, "a page below the first is clamped to it")
        Assertions.assertEquals(2, select("page=9999&size=2").page, "a page past the end clamps to the last")
        Assertions.assertEquals(4, select("&&&=&f.=x&").matched)
        Assertions.assertEquals(4, select("f.nosuchcolumn=whatever").matched, "an unknown column is ignored, not fatal")
    }

    /** A page past the end clamps to the last one, so a stale bookmark shows rows rather than emptiness. */
    @Test
    fun aPagePastTheEndClampsToTheLast() {
        val page = select("size=2&page=99&sort=name")

        Assertions.assertEquals(2, page.page)
        Assertions.assertEquals(2, page.rows.size)
    }

    /**
     * A GET form submits its empty controls, so `?f.name=&q=` arrives constantly. Empty must read as
     * absent, or every shared link carries a tail of meaningless parameters.
     */
    @Test
    fun emptyParametersReadAsAbsentAndAreNotEmitted() {
        val page = select("f.name=&q=&sort=&dir=")

        Assertions.assertEquals(4, page.matched)
        Assertions.assertEquals("", page.query.toQueryString(), "a query of nothing serialises to nothing")
    }

    /** The applied query round-trips, which is what makes every link on the page state-preserving. */
    @Test
    fun theAppliedQueryRoundTrips() {
        val original = VerdictQuery.parse(QueryParams.parse("q=jei&f.loader=Fabric&sort=name&dir=desc&size=2&page=1"), 250)
        val reparsed = VerdictQuery.parse(QueryParams.parse(original.toQueryString().removePrefix("?")), 250)

        Assertions.assertEquals(original, reparsed)
    }

    /** A filter value carrying reserved characters must survive the round trip intact. */
    @Test
    fun aFilterValueWithReservedCharactersSurvivesARoundTrip() {
        val original = VerdictQuery.parse(QueryParams.parse("f.detail=" + "a%26b%20c"), 250)

        Assertions.assertEquals("a&b c", original.filters[VerdictField.DETAIL]?.single())
        Assertions.assertEquals(
            original,
            VerdictQuery.parse(QueryParams.parse(original.toQueryString().removePrefix("?")), 250)
        )
    }

    /**
     * The size selector must not offer sizes the data cannot fill — "no need to show 100.000 when we only
     * have 2.000" — while always keeping `all` and the size actually in force.
     */
    @Test
    fun onlyThePageSizesAResultCountJustifiesAreOffered() {
        // 1,842 rows: 100/250/500/1000 genuinely paginate. 2,000 and up would each show everything on one
        // page, which is what `all` already is — so they are omitted as duplicates rather than as "too big".
        Assertions.assertEquals(listOf(100, 250, 500, 1_000, null), VerdictQuery.offeredSizes(1_842, 250))
        Assertions.assertFalse(
            VerdictQuery.offeredSizes(1_842, 250).contains(100_000),
            "no need to offer 100,000 when there are 1,842 rows"
        )
        // 12 rows: nothing paginates, so only the size in force and `all` remain.
        Assertions.assertEquals(listOf(250, null), VerdictQuery.offeredSizes(12, 250))
    }

    /**
     * **The load-bearing one for the selector.** Filter 104,000 rows down to 12 while `size=1000` and the
     * offered list would hold only `all` — the browser then shows the first option while the URL says
     * something else, so the page silently disagrees with itself. The size in force is always offered.
     */
    @Test
    fun theSizeInForceIsAlwaysOfferedEvenWhenTheResultCountWouldNot() {
        Assertions.assertTrue(
            VerdictQuery.offeredSizes(12, 1_000).contains(1_000),
            "the current size must be selectable or the control disagrees with the URL"
        )
    }
}
