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
import java.time.Instant

/**
 * Pins the live activity record behind `/status`. The verdict store says what the grinder has *found*; this says
 * what it is *doing* — the question an operator actually has when a boot has been running for eight minutes and
 * the log has gone quiet.
 */
internal class GrinderStatusTest {

    private var now = Instant.parse("2026-07-30T12:00:00Z")
    private val status = GrinderStatus { now }

    private fun candidate(platform: String, slug: String) =
        GrindCandidate("https://example.invalid/$slug", slug, 1, platform)

    private fun tick(seconds: Long) {
        now = now.plusSeconds(seconds)
    }

    @Test
    fun reportsUptimeAndTheCurrentPass() {
        tick(90)
        status.beginPass(number = 4, candidates = 100)
        tick(30)

        val snapshot = status.snapshot()

        Assertions.assertEquals(120, snapshot.uptimeSeconds)
        Assertions.assertEquals(4, snapshot.pass)
        Assertions.assertEquals(100, snapshot.passCandidates)
        Assertions.assertEquals(30, snapshot.passRunningSeconds, "the pass timer restarts with the pass")
    }

    /** The central question: which candidate is a worker holding, and for how long. */
    @Test
    fun reportsWhatTheWorkerIsHoldingAndForHowLong() {
        status.beginCandidate(candidate("CurseForge", "jei"))
        tick(480)

        val worker = status.snapshot().workers.single()

        Assertions.assertEquals(Thread.currentThread().name, worker.worker)
        Assertions.assertEquals("CurseForge", worker.platform)
        Assertions.assertEquals("jei", worker.slug)
        Assertions.assertEquals(480, worker.busySeconds, "a boot stuck for 8 minutes must be visible as such")
    }

    @Test
    fun aFinishedWorkerIsNoLongerBusy() {
        status.beginCandidate(candidate("Modrinth", "sodium"))
        status.endCandidate(GrindOutcome.VERIFIED)

        Assertions.assertTrue(status.snapshot().workers.isEmpty(), "an idle worker is simply absent")
    }

    @Test
    fun countsOutcomesSeparately() {
        listOf(GrindOutcome.VERIFIED, GrindOutcome.VERIFIED, GrindOutcome.FAILED, GrindOutcome.SKIPPED_FRESH)
            .forEach { outcome ->
                status.beginCandidate(candidate("Modrinth", "mod"))
                status.endCandidate(outcome)
            }

        val snapshot = status.snapshot()

        Assertions.assertEquals(2, snapshot.verified)
        Assertions.assertEquals(1, snapshot.failed)
        Assertions.assertEquals(1, snapshot.skippedFresh)
    }

    /** Several workers hold different candidates at once; each is listed, ordered so the output is stable. */
    @Test
    fun reportsEveryBusyWorker() {
        val threads = (1..3).map { index ->
            Thread({
                status.beginCandidate(candidate("CurseForge", "mod$index"))
            }, "grind-worker-$index").apply { start() }
        }
        threads.forEach { it.join(5_000) }

        val workers = status.snapshot().workers

        Assertions.assertEquals(listOf("grind-worker-1", "grind-worker-2", "grind-worker-3"), workers.map { it.worker })
        Assertions.assertEquals(setOf("mod1", "mod2", "mod3"), workers.map { it.slug }.toSet())
    }

    /** A snapshot is a copy: serializing it while the workers carry on must not observe them mutating. */
    @Test
    fun aSnapshotDoesNotChangeUnderTheReader() {
        status.beginCandidate(candidate("Modrinth", "iris"))
        val snapshot = status.snapshot()

        status.endCandidate(GrindOutcome.VERIFIED)
        status.beginPass(number = 9, candidates = 5)

        Assertions.assertEquals(1, snapshot.workers.size, "the taken snapshot is unaffected by later activity")
        Assertions.assertEquals(0, snapshot.verified)
    }

    @Test
    fun uptimeUsesTheInjectedClockNotWallTime() {
        tick(Duration.ofHours(3).seconds)

        Assertions.assertEquals(Duration.ofHours(3).seconds, status.snapshot().uptimeSeconds)
    }
}
