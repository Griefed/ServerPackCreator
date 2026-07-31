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
}
