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
package de.griefed.serverpackcreator.grinder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * Pins the daemon's pacing policy — the reason a fixed sleep between passes was replaced. A fixed interval
 * caps how fast the catalog can be swept (25 projects per 6 h would need years for Modrinth), so the loop
 * only waits when it has nothing to do: it grinds straight on while work keeps turning up, pages ahead
 * gently while merely scanning, and idles properly once a whole sweep found nothing left to verify.
 */
internal class GrindPacingTest {

    private val betweenSweeps = Duration.ofHours(6)
    private val whileCrawling = Duration.ofSeconds(15)

    @Test
    fun aPassThatDidWorkContinuesImmediately() {
        val pause = GrindPacing.pauseAfterPass(
            verified = 3,
            sweepCompleted = false,
            betweenSweeps = betweenSweeps,
            whileCrawling = whileCrawling
        )

        Assertions.assertEquals(Duration.ZERO, pause, "while there is work, the next slice starts at once")
    }

    @Test
    fun aPassWithNothingDueMidCatalogPagesAheadGently() {
        val pause = GrindPacing.pauseAfterPass(
            verified = 0,
            sweepCompleted = false,
            betweenSweeps = betweenSweeps,
            whileCrawling = whileCrawling
        )

        Assertions.assertEquals(whileCrawling, pause, "nothing due here, but the catalog goes on — keep scanning")
    }

    @Test
    fun aFullSweepWithNothingDueIdlesForTheLongInterval() {
        val pause = GrindPacing.pauseAfterPass(
            verified = 0,
            sweepCompleted = true,
            betweenSweeps = betweenSweeps,
            whileCrawling = whileCrawling
        )

        Assertions.assertEquals(betweenSweeps, pause, "the reachable catalog is covered and current — idle")
    }

    /** A completed sweep is no reason to idle while there is still stale work turning up. */
    @Test
    fun workOutranksACompletedSweep() {
        val pause = GrindPacing.pauseAfterPass(
            verified = 1,
            sweepCompleted = true,
            betweenSweeps = betweenSweeps,
            whileCrawling = whileCrawling
        )

        Assertions.assertEquals(Duration.ZERO, pause)
    }
}
