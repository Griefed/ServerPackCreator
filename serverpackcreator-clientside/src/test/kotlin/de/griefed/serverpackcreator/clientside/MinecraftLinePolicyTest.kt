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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins [MinecraftLinePolicy] — which of a project's Minecraft version-lines get ground.
 *
 * The behaviours worth guarding are the ones that cost boots or lose coverage: the newest-N half must not
 * be a *string* comparison, the anchors must be an intersection rather than a wish-list, and no
 * configuration may select nothing.
 *
 * @author Griefed
 */
internal class MinecraftLinePolicyTest {

    /** JEI's shape: sixteen lines, of which only a handful are worth a boot. */
    private val manyLines = listOf("1.7", "1.8", "1.10", "1.12", "1.16", "1.18", "1.19", "1.20", "1.21", "26.1", "26.2")

    /**
     * The head of the catalogue needs no configuration: whatever the newest lines turn out to be, they are
     * ground. That is the half a bare anchor list cannot do — a new Minecraft release would simply never be
     * ground, and nothing would report it.
     */
    @Test
    fun theNewestLinesAreAlwaysKept() {
        Assertions.assertEquals(
            listOf("26.2", "26.1"),
            MinecraftLinePolicy(newestCount = 2, anchors = emptySet()).select(manyLines)
        )
    }

    /**
     * **Newest is numeric, not lexical.** `1.9` sorts above `1.20` as a string, so a string compare would
     * pick a decade-old line as "newest" for any project still publishing for it — and silently, because
     * the answer is a plausible version rather than an error.
     */
    @Test
    fun newestIsOrderedNumerically() {
        Assertions.assertEquals(
            listOf("1.20"),
            MinecraftLinePolicy(newestCount = 1, anchors = emptySet()).select(listOf("1.9", "1.20", "1.8"))
        )
    }

    /** The anchors add older lines the newest-N would never reach, and the result stays newest-first. */
    @Test
    fun anchorsAddOlderLinesAndTheOrderStaysNewestFirst() {
        Assertions.assertEquals(
            listOf("26.2", "26.1", "1.20", "1.12"),
            MinecraftLinePolicy(newestCount = 2, anchors = setOf("1.20", "1.12")).select(manyLines)
        )
    }

    /**
     * An anchor is an **intersection**, never a wish: a line the project never published for cannot be
     * ground, and returning it would hand the caller a line with no files behind it.
     */
    @Test
    fun anAnchorTheProjectNeverPublishedForIsNotSelected() {
        Assertions.assertEquals(
            listOf("1.21", "1.20"),
            MinecraftLinePolicy(newestCount = 1, anchors = setOf("1.20", "1.12")).select(listOf("1.20", "1.21"))
        )
    }

    /**
     * An operator reads `1.12.2` off a platform page, not `1.12`. Refusing the full version would fail by
     * matching nothing — silently, and in the direction that grinds less than was asked for.
     */
    @Test
    fun anAnchorMayBeSpelledAsAFullVersion() {
        Assertions.assertEquals(
            listOf("1.21", "1.12"),
            MinecraftLinePolicy(newestCount = 1, anchors = setOf("1.12.2")).select(listOf("1.12", "1.21"))
        )
    }

    /**
     * **No configuration may select nothing.** A candidate that records no verdict is indistinguishable
     * from one the engine failed on — the freshness check keeps answering "never seen", so the project is
     * re-selected every sweep forever. `newestCount = 0` with anchors nothing matches is exactly that
     * configuration, and it is floored rather than honoured.
     */
    @Test
    fun aPolicyThatWouldSelectNothingStillKeepsTheNewestLine() {
        Assertions.assertEquals(
            listOf("1.21"),
            MinecraftLinePolicy(newestCount = 0, anchors = setOf("1.7")).select(listOf("1.20", "1.21"))
        )
        Assertions.assertEquals(
            listOf("1.21"),
            MinecraftLinePolicy(newestCount = -3, anchors = emptySet()).select(listOf("1.20", "1.21"))
        )
    }

    /** A project with no published line yields none; nothing is invented. */
    @Test
    fun noLinesYieldsNoSelection() {
        Assertions.assertEquals(emptyList<String>(), MinecraftLinePolicy().select(emptyList()))
    }

    /** A line offered twice is ground once. */
    @Test
    fun duplicateLinesCollapse() {
        Assertions.assertEquals(
            listOf("1.21", "1.20"),
            MinecraftLinePolicy(newestCount = 2, anchors = emptySet()).select(listOf("1.20", "1.21", "1.20", "1.21"))
        )
    }

    /**
     * The shipped defaults, asserted as a whole rather than field by field: they are what every daemon runs
     * with unless an operator says otherwise, and the 1.25x measurement behind them is about this exact set.
     */
    @Test
    fun theShippedDefaultsGrindTheNewestTwoPlusThreeAnchors() {
        Assertions.assertEquals(
            listOf("26.2", "26.1", "1.21", "1.20", "1.12"),
            MinecraftLinePolicy().select(manyLines)
        )
    }
}
