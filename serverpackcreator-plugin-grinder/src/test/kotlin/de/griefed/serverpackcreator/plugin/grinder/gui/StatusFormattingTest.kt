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
package de.griefed.serverpackcreator.plugin.grinder.gui

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins how a `/status` document is turned into the strings the Dashboard shows.
 *
 * These were private functions inside a Swing panel, which is why they had no guards — and they are the
 * only part of that panel that is not layout. They read a document produced by a daemon the user upgrades
 * independently, so "the field is missing" and "the field changed type" are the ordinary cases, not the
 * exotic ones.
 */
internal class StatusFormattingTest {

    private val mapper = ObjectMapper()

    private fun node(json: String) = mapper.readTree(json)

    /** Durations read as a human would say them, which is the whole reason not to print raw seconds. */
    @Test
    fun rendersDurationsTheWayTheDaemonsOwnDashboardDoes() {
        Assertions.assertEquals("45s", StatusFormatting.duration(node("45")))
        Assertions.assertEquals("2m 5s", StatusFormatting.duration(node("125")))
        Assertions.assertEquals("4h 12m", StatusFormatting.duration(node("15134")))
        Assertions.assertEquals("2d 3h 25m", StatusFormatting.duration(node("185100")))
    }

    /** The boundaries between the four shapes, where an off-by-one would show "0h 5m" or "60m 0s". */
    @Test
    fun switchesShapeExactlyOnTheBoundaries() {
        Assertions.assertEquals("59s", StatusFormatting.duration(node("59")))
        Assertions.assertEquals("1m 0s", StatusFormatting.duration(node("60")))
        Assertions.assertEquals("59m 59s", StatusFormatting.duration(node("3599")))
        Assertions.assertEquals("1h 0m", StatusFormatting.duration(node("3600")))
        Assertions.assertEquals("23h 59m", StatusFormatting.duration(node("86399")))
        Assertions.assertEquals("1d 0h 0m", StatusFormatting.duration(node("86400")))
    }

    /** Zero is a real answer — a pass that has just begun — and must not read as "unknown". */
    @Test
    fun rendersZeroAsADurationRatherThanAsAbsent() {
        Assertions.assertEquals("0s", StatusFormatting.duration(node("0")))
    }

    /**
     * Anything that is not a number is a gap, including a *string* holding digits: a daemon that starts
     * sending `"15134"` has changed its contract, and quietly reading it would hide that.
     */
    @Test
    fun reportsANonNumericDurationAsAGap() {
        Assertions.assertEquals("—", StatusFormatting.duration(node("null")))
        Assertions.assertEquals("—", StatusFormatting.duration(node("\"soon\"")))
        Assertions.assertEquals("—", StatusFormatting.duration(node("\"15134\"")))
        Assertions.assertEquals("—", StatusFormatting.duration(mapper.createObjectNode().path("absent")))
    }

    /** A negative duration cannot happen, but must not render as garbage if it does. */
    @Test
    fun refusesToRenderANegativeDuration() {
        Assertions.assertEquals("—", StatusFormatting.duration(node("-1")))
    }

    /** A plain value renders as itself; an absent, null or blank one renders as a visible gap. */
    @Test
    fun rendersAValueOrAVisibleGap() {
        Assertions.assertEquals("1483", StatusFormatting.text(node("1483")))
        Assertions.assertEquals("Modrinth", StatusFormatting.text(node("\"Modrinth\"")))
        Assertions.assertEquals("—", StatusFormatting.text(node("null")))
        Assertions.assertEquals("—", StatusFormatting.text(node("\"\"")))
        Assertions.assertEquals("—", StatusFormatting.text(node("\"   \"")))
        Assertions.assertEquals("—", StatusFormatting.text(mapper.createObjectNode().path("absent")))
    }

    /** `false` is a value, not a gap — an em dash there would misreport a real answer. */
    @Test
    fun rendersFalseRatherThanTreatingItAsAbsent() {
        Assertions.assertEquals("false", StatusFormatting.text(node("false")))
    }
}
