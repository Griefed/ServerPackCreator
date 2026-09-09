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

    /**
     * **The inter-pass wait has to end early when work is queued.**
     *
     * `pauseAfterPass` returns `betweenSweeps` — default `SPC_GRINDER_INTERVAL`, 21 600 s — after a completed
     * sweep that verified nothing. Sleeping that in one call means an operator who queues a re-grind into a
     * just-dozed daemon waits up to six hours, and the queue exists precisely so a known-wrong verdict is not
     * served while a timer runs down. Trading a 30-day TTL for a 6-hour one is better and still not what was
     * built.
     *
     * Pinned as the pure decision — how long to wait *before looking again* — so no test has to sleep.
     */
    @Test
    fun theWaitIsSlicedSoQueuedWorkIsNoticedLongBeforeItEnds() {
        val sixHours = Duration.ofHours(6)

        Assertions.assertTrue(
            GrindPacing.pollInterval(sixHours) <= Duration.ofSeconds(30),
            "a six-hour pause must be looked at far more often than once"
        )
        Assertions.assertEquals(
            Duration.ofSeconds(5),
            GrindPacing.pollInterval(Duration.ofSeconds(5)),
            "a pause shorter than the slice is simply the pause — never round it up"
        )
        Assertions.assertTrue(
            GrindPacing.pollInterval(Duration.ZERO).isZero,
            "no pause, nothing to slice"
        )
    }

    /**
     * **A remainder that has already elapsed must slice to zero, never to a negative.**
     *
     * The caller computes `wakeAt - now` *after* checking `now < wakeAt`, with a synchronized read of the
     * queue file in between — so on the final slice, where the remainder is by construction somewhere in
     * `(0, 15s]`, an I/O stall longer than the remainder makes it negative. A negative `Duration` compares
     * below the slice and would be passed straight through, and `Thread.sleep(-5)` throws
     * `IllegalArgumentException` — not an `InterruptedException`, so it escapes the wait's catch, escapes
     * `while (running.get())`, and ends `main`. A fire-and-forget daemon then quietly stops grinding with no
     * crash anybody is watching for.
     */
    @Test
    fun aRemainderThatHasAlreadyElapsedSlicesToZero() {
        Assertions.assertTrue(GrindPacing.pollInterval(Duration.ofMillis(-5)).isZero)
        Assertions.assertTrue(GrindPacing.pollInterval(Duration.ofHours(-1)).isZero)
        Assertions.assertFalse(
            GrindPacing.pollInterval(Duration.ofMillis(-5)).isNegative,
            "Thread.sleep throws on a negative timeout, and nothing on that path catches it"
        )
    }
}
