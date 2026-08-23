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

import de.griefed.serverpackcreator.clientside.ClientsideReport
import de.griefed.serverpackcreator.grinder.report.InMemoryVerdictStore
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pins what `systemctl stop` must do to the workers: signal them to quit, and stop waiting after a bounded
 * grace period rather than blocking on a boot that has a fifteen-minute budget.
 *
 * `requestStop` alone was never enough for that. It sets a flag the worker loop reads *between* candidates, so
 * a worker parked inside a boot keeps going until that boot finishes — while systemd counts down to a SIGKILL
 * that orphans the container. The grace window has to be enforced against a worker that does not cooperate,
 * which is what the timeout case here holds.
 */
internal class GrindPoolShutdownTest {

    /** A verifier that parks until released, so a "boot in progress" can be held across a shutdown. */
    private class ParkedVerifier : CandidateVerifier {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val interrupted = AtomicBoolean(false)

        override fun verify(candidate: GrindCandidate): ClientsideReport {
            entered.countDown()
            try {
                release.await()
            } catch (_: InterruptedException) {
                interrupted.set(true)
                Thread.currentThread().interrupt()
            }
            return clientsideReport(candidate.slug, emptyList())
        }
    }

    private fun candidates(count: Int) = (1..count).map {
        GrindCandidate("https://modrinth.com/mod/mod$it", "mod$it", it.toLong(), "Modrinth")
    }

    @Test
    @Timeout(30)
    fun interruptsAWorkerParkedInABootRatherThanWaitingOutItsBudget() {
        val verifier = ParkedVerifier()
        val pool = GrindPool(Grinder(verifier, InMemoryVerdictStore()), workerCount = 1)
        val pass = Thread { pool.grindAll(candidates(1)) }.apply { isDaemon = true; start() }

        Assertions.assertTrue(verifier.entered.await(10, TimeUnit.SECONDS), "the worker never started its candidate")
        val stoppedCleanly = pool.awaitStop(Duration.ofSeconds(10))

        Assertions.assertTrue(stoppedCleanly, "the worker must be interrupted out of its boot, not waited out")
        Assertions.assertTrue(verifier.interrupted.get(), "the in-flight verification must actually see the interrupt")
        pass.join(5_000)
    }

    @Test
    @Timeout(30)
    fun givesUpAfterTheGraceWindowWhenAWorkerWillNotQuit() {
        // Swallows the interrupt entirely — the case the grace window exists for. Nothing can force a thread
        // to die in the JVM, so "force kill" is the process exiting; awaitStop's job is to stop waiting.
        val stubborn = object : CandidateVerifier {
            val entered = CountDownLatch(1)
            override fun verify(candidate: GrindCandidate): ClientsideReport {
                entered.countDown()
                val until = System.currentTimeMillis() + 20_000
                while (System.currentTimeMillis() < until) {
                    runCatching { Thread.sleep(50) } // deliberately ignores the interrupt
                }
                return clientsideReport(candidate.slug, emptyList())
            }
        }
        val pool = GrindPool(Grinder(stubborn, InMemoryVerdictStore()), workerCount = 1)
        Thread { pool.grindAll(candidates(1)) }.apply { isDaemon = true; start() }
        Assertions.assertTrue(stubborn.entered.await(10, TimeUnit.SECONDS))

        val startedAt = System.currentTimeMillis()
        val stoppedCleanly = pool.awaitStop(Duration.ofSeconds(2))
        val waited = System.currentTimeMillis() - startedAt

        Assertions.assertFalse(stoppedCleanly, "a worker that ignores the signal must be reported as not stopped")
        Assertions.assertTrue(waited < 10_000, "must return at the grace window, not when the worker finishes (waited ${waited}ms)")
    }

    @Test
    @Timeout(30)
    fun stopsWorkersTakingFurtherCandidates() {
        val ground = AtomicInteger(0)
        val counting = CandidateVerifier { candidate ->
            ground.incrementAndGet()
            Thread.sleep(200)
            clientsideReport(candidate.slug, emptyList())
        }
        val pool = GrindPool(Grinder(counting, InMemoryVerdictStore()), workerCount = 1)
        Thread { pool.grindAll(candidates(50)) }.apply { isDaemon = true; start() }
        while (ground.get() == 0) {
            Thread.sleep(20)
        }

        pool.awaitStop(Duration.ofSeconds(5))
        val atStop = ground.get()
        Thread.sleep(500)

        Assertions.assertEquals(atStop, ground.get(), "no candidate may be picked up after the stop")
    }
}
