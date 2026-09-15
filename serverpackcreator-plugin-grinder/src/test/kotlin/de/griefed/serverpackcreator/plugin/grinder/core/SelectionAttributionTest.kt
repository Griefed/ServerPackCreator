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
package de.griefed.serverpackcreator.plugin.grinder.core

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins which of the two configuration keys each ticked entry is written under.
 *
 * This was logic buried in `GrinderTab`, untested because the class around it is Swing — and it was
 * wrong. It decides nothing about *whether* a mod is excluded (generation reads the union of both keys)
 * and everything about whether the user's record of what they accepted **at their own risk** survives,
 * which is the entire reason the interface has two panes.
 *
 * The case that broke it is the module's designed steady state, not an edge: `SelectionStore` and
 * `VerdictTableModel` both deliberately never prune an entry the grinder has stopped reporting, so
 * entries shown in *neither* pane accumulate — and the old `partition { it in shownInOther }` filed
 * every one of them as CONFIRMED on the next tick.
 */
internal class SelectionAttributionTest {

    /** An entry the Confirmed pane is showing is filed as confirmed. */
    @Test
    fun filesAnEntryUnderThePaneShowingIt() {
        val split = SelectionAttribution.split(
            selected = setOf("creativecore-", "iceberg-"),
            shownInConfirmed = setOf("creativecore-"),
            shownInOther = setOf("iceberg-"),
            storedConfirmed = emptySet(),
            storedOther = emptySet()
        )

        Assertions.assertEquals(setOf("creativecore-"), split.confirmed)
        Assertions.assertEquals(setOf("iceberg-"), split.other)
    }

    /**
     * The regression. An entry neither pane is showing — the grinder stopped reporting it, or has not
     * been reached this session — keeps the pane it was saved under. Filing it as confirmed would tell
     * the user a mod was *proven* clientside-only when what they actually did was accept a risk.
     */
    @Test
    fun keepsAStaleEntryUnderThePaneItWasSavedIn() {
        val split = SelectionAttribution.split(
            selected = setOf("vanished-risky-", "vanished-proven-"),
            shownInConfirmed = emptySet(),
            shownInOther = emptySet(),
            storedConfirmed = setOf("vanished-proven-"),
            storedOther = setOf("vanished-risky-")
        )

        Assertions.assertEquals(setOf("vanished-proven-"), split.confirmed)
        Assertions.assertEquals(
            setOf("vanished-risky-"), split.other,
            "a risk the user accepted must not be silently reclassified as a proven finding"
        )
    }

    /**
     * *Select all* on a pane that has not loaded republishes the unchanged selection, so the split runs
     * with nothing shown anywhere. It must be a no-op rather than a migration.
     */
    @Test
    fun isANoOpWhenNeitherPaneHasLoaded() {
        val storedConfirmed = setOf("creativecore-", "jei-")
        val storedOther = setOf("iceberg-")

        val split = SelectionAttribution.split(
            selected = storedConfirmed + storedOther,
            shownInConfirmed = emptySet(),
            shownInOther = emptySet(),
            storedConfirmed = storedConfirmed,
            storedOther = storedOther
        )

        Assertions.assertEquals(storedConfirmed, split.confirmed)
        Assertions.assertEquals(storedOther, split.other)
    }

    /**
     * What is *shown* wins over what was stored: this is how a verdict that has been re-classified by
     * the grinder — `INCONCLUSIVE` re-ground into `CONFIRMED` — moves to the pane it now belongs in.
     */
    @Test
    fun letsTheDisplayedPaneOverrideAStaleStoredPane() {
        val split = SelectionAttribution.split(
            selected = setOf("promoted-"),
            shownInConfirmed = setOf("promoted-"),
            shownInOther = emptySet(),
            storedConfirmed = emptySet(),
            storedOther = setOf("promoted-")
        )

        Assertions.assertEquals(setOf("promoted-"), split.confirmed)
        Assertions.assertTrue(split.other.isEmpty())
    }

    /** An entry that is neither shown nor stored is new and unattributable; the risk pane is the safe home. */
    @Test
    fun filesAnEntryItHasNeverSeenUnderTheRiskPane() {
        val split = SelectionAttribution.split(
            selected = setOf("from-nowhere-"),
            shownInConfirmed = emptySet(),
            shownInOther = emptySet(),
            storedConfirmed = emptySet(),
            storedOther = emptySet()
        )

        Assertions.assertTrue(
            split.confirmed.isEmpty(),
            "an entry with no evidence behind it must not be recorded as a proven finding"
        )
        Assertions.assertEquals(setOf("from-nowhere-"), split.other)
    }

    /** Unticking removes an entry from both keys rather than leaving it behind in the one it was not filed under. */
    @Test
    fun dropsWhatIsNoLongerSelected() {
        val split = SelectionAttribution.split(
            selected = emptySet(),
            shownInConfirmed = setOf("creativecore-"),
            shownInOther = setOf("iceberg-"),
            storedConfirmed = setOf("creativecore-"),
            storedOther = setOf("iceberg-")
        )

        Assertions.assertTrue(split.confirmed.isEmpty())
        Assertions.assertTrue(split.other.isEmpty())
    }

    /**
     * A pane cannot claim an entry the other is also showing — the two hold disjoint verdict classes, so
     * this should never arise, but the split must be total rather than depend on that.
     */
    @Test
    fun prefersConfirmedIfBothPanesSomehowShowTheSameEntry() {
        val split = SelectionAttribution.split(
            selected = setOf("both-"),
            shownInConfirmed = setOf("both-"),
            shownInOther = setOf("both-"),
            storedConfirmed = emptySet(),
            storedOther = emptySet()
        )

        Assertions.assertEquals(setOf("both-"), split.confirmed)
        Assertions.assertTrue(split.other.isEmpty())
    }
}
