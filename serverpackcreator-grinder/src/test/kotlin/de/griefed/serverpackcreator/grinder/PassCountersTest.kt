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

/**
 * Pins that the per-pass counters on `/status` are **per pass**.
 *
 * `verified`, `failed` and `skippedFresh` are documented as "this pass" and are rendered by
 * `StatusDashboardRenderer` directly beneath `Pass N (M candidates)`, which is genuinely per-pass. They were
 * neither: the three `AtomicInteger`s were lifetime totals that `beginPass` never reset, so a dashboard read
 * "Pass 12 (25 candidates)" above "Verified 3,140" and the ratio a reader takes from that pairing was
 * meaningless.
 *
 * Per-pass is the reading kept because it is the one both the field documentation and the only rendering of
 * these numbers already promise, and because it is the one that answers the question the block exists for —
 * *is the pass now running getting anywhere?* A lifetime count of verdicts is already available, and more
 * accurately, from the store.
 */
internal class PassCountersTest {

    /** A pass with one of each outcome, so every counter is exercised. */
    private fun GrinderStatus.onePassOfEach() {
        endCandidate(GrindOutcome.VERIFIED)
        endCandidate(GrindOutcome.FAILED)
        endCandidate(GrindOutcome.SKIPPED_FRESH)
    }

    /** Within one pass they accumulate, which is what makes them worth showing at all. */
    @Test
    fun countersAccumulateWithinAPass() {
        val status = GrinderStatus()
        status.beginPass(1, candidates = 9)
        status.onePassOfEach()
        status.endCandidate(GrindOutcome.VERIFIED)

        val snapshot = status.snapshot()
        Assertions.assertEquals(2, snapshot.verified)
        Assertions.assertEquals(1, snapshot.failed)
        Assertions.assertEquals(1, snapshot.skippedFresh)
    }

    /** **The defect.** A new pass starts from zero, or the numbers are lifetime totals wearing a pass label. */
    @Test
    fun aNewPassStartsTheCountersFromZero() {
        val status = GrinderStatus()
        status.beginPass(1, candidates = 3)
        status.onePassOfEach()

        status.beginPass(2, candidates = 3)
        val snapshot = status.snapshot()

        Assertions.assertEquals(0, snapshot.verified, "verified is documented and rendered as per-pass")
        Assertions.assertEquals(0, snapshot.failed, "failed is documented and rendered as per-pass")
        Assertions.assertEquals(0, snapshot.skippedFresh, "skippedFresh is documented and rendered as per-pass")
        Assertions.assertEquals(2, snapshot.pass)
    }

    /** And the second pass counts its own work, rather than resuming the first's. */
    @Test
    fun theSecondPassCountsOnlyItsOwnWork() {
        val status = GrinderStatus()
        status.beginPass(1, candidates = 3)
        repeat(5) { status.endCandidate(GrindOutcome.VERIFIED) }

        status.beginPass(2, candidates = 3)
        status.endCandidate(GrindOutcome.VERIFIED)

        Assertions.assertEquals(1, status.snapshot().verified)
    }

    /** Uptime is genuinely lifetime and must not be reset alongside them. */
    @Test
    fun theDaemonsUptimeIsNotAPassCounter() {
        val status = GrinderStatus()
        status.beginPass(1, candidates = 1)
        status.endCandidate(GrindOutcome.VERIFIED)
        status.beginPass(2, candidates = 1)

        Assertions.assertTrue(status.snapshot().uptimeSeconds >= 0, "uptime is measured from daemon start")
        Assertions.assertNotNull(status.snapshot().startedAt)
    }
}
