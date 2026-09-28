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

import java.time.Duration

/**
 * When the continuous grind should wait between passes. Kept apart from the daemon loop so the policy — the
 * thing that decides whether a whole catalog is ever covered — is a pure function with tests, rather than a
 * `Thread.sleep` buried in `main`.
 *
 * @author Griefed
 */
object GrindPacing {
    /**
     * How long to wait before the next pass, given how many candidates that pass actually [verified] and
     * whether a source finished a sweep of its catalog while doing so.
     *
     * Three cases: work was found, so carry straight on to the next slice; nothing was due but the catalog
     * still has ground to cover, so page ahead after a short [whileCrawling] courtesy pause; or a sweep
     * completed with nothing due, meaning the reachable catalog is covered and current — idle for
     * [betweenSweeps] and let the verdict TTL bring the next work along.
     *
     * Failed verifications deliberately do not count as work: when the host is broken (no Docker daemon, say)
     * every candidate fails, and treating that as progress would race the crawl position through the catalog
     * leaving thousands of projects unverified. Failing passes therefore throttle instead.
     */
    fun pauseAfterPass(
        verified: Int,
        sweepCompleted: Boolean,
        betweenSweeps: Duration,
        whileCrawling: Duration
    ): Duration = when {
        verified > 0 -> Duration.ZERO
        sweepCompleted -> betweenSweeps
        else -> whileCrawling
    }

    /**
     * How long to wait *before looking again* while serving out a [pause] from [pauseAfterPass] — the whole
     * pause when it is shorter than [POLL_SLICE], otherwise one slice.
     *
     * **Why the wait is sliced at all.** [pauseAfterPass] returns `betweenSweeps` after a completed sweep that
     * verified nothing, and that default is six hours. Sleeping it in one call means an operator who queues a
     * re-grind into a just-dozed daemon waits up to six hours for it — and the immediate re-grind queue exists
     * precisely so that a verdict known to be wrong is not served while a timer runs down. Trading a 30-day
     * TTL for a 6-hour one is better and still not what was built.
     *
     * **LANDMINE — a remainder that has already elapsed slices to zero, never to a negative.** The caller
     * computes `wakeAt - now` *after* testing `now < wakeAt`, with a read of the queue file in between, so on
     * the final slice — bounded by construction to `(0, POLL_SLICE]` — an I/O stall longer than the remainder
     * makes it negative. `Thread.sleep` throws `IllegalArgumentException` on a negative timeout, and that is
     * not an `InterruptedException`: it would escape the wait's catch, escape `while (running.get())` and end
     * `main`, leaving a fire-and-forget daemon quietly not grinding.
     */
    fun pollInterval(pause: Duration): Duration = when {
        pause.isNegative -> Duration.ZERO
        pause < POLL_SLICE -> pause
        else -> POLL_SLICE
    }

    /**
     * The longest the daemon sleeps without checking whether work has been queued.
     *
     * Small enough that a re-grind feels immediate, large enough that a dozing daemon is not spinning: across
     * a six-hour pause this is 1 440 wake-ups that each read one small JSON file, which is nothing beside the
     * container boots the same daemon does when it is awake.
     */
    private val POLL_SLICE: Duration = Duration.ofSeconds(15)
}
