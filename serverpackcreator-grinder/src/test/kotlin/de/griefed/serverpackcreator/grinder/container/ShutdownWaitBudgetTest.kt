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
package de.griefed.serverpackcreator.grinder.container

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Pins that the container-stop drain **gives up**, because every other wait in the shutdown path does.
 *
 * `GrinderApplication`'s hook budgets one `SHUTDOWN_GRACE` window across the whole stop: containers first,
 * then whatever is left to the workers, floored so an interrupt can still be observed. `close()` was the one
 * step in that budget that could not expire — it blocked on `Future.get()` with no timeout. Its per-container
 * `stopContainerCmd.withTimeout(...)` bounds Docker's *internal* SIGTERM-to-SIGKILL window, not the HTTP call
 * that asks for it, so a wedged daemon socket parks the shutdown hook indefinitely and systemd's
 * `TimeoutStopSec` arrives instead — the SIGKILL that orphans containers, which is the very outcome `close()`
 * exists to prevent.
 *
 * Extracted as a pure helper rather than pinned through the engine: [DockerJavaContainerEngine] needs a live
 * daemon and this module carries no mocking library, so the alternative was a hand-written stub of an
 * 80-method interface. The decision — *wait for these, but not past here* — is the part that was wrong, and
 * it needs no Docker at all.
 *
 * Timing assertions are deliberately loose. What is asserted is the **outcome** (did it give up?) and that it
 * returned nowhere near the blocked task's own duration; a tight margin here would only buy flakiness on a
 * loaded CI box.
 */
internal class ShutdownWaitBudgetTest {

    /** Tasks that finish immediately: the drain reports success and does not sit out the budget. */
    @Test
    fun tasksThatFinishInTimeReportSuccess() {
        val pool = Executors.newFixedThreadPool(2)
        try {
            val done = (1..2).map { pool.submit { } }
            val startedAt = System.currentTimeMillis()

            Assertions.assertTrue(DockerJavaContainerEngine.awaitWithin(done, Duration.ofSeconds(10)))
            Assertions.assertTrue(
                System.currentTimeMillis() - startedAt < 5_000,
                "a completed drain must return at once, not wait out its budget"
            )
        } finally {
            pool.shutdownNow()
        }
    }

    /** **The defect.** One wedged task must not hold the shutdown open for its own duration. */
    @Test
    fun aWedgedTaskDoesNotHoldTheShutdownOpen() {
        val pool = Executors.newFixedThreadPool(2)
        val release = CountDownLatch(1)
        try {
            val blocked = pool.submit { release.await(5, TimeUnit.MINUTES) }
            val startedAt = System.currentTimeMillis()

            val completed = DockerJavaContainerEngine.awaitWithin(listOf(blocked), Duration.ofMillis(200))
            val elapsed = System.currentTimeMillis() - startedAt

            Assertions.assertFalse(completed, "a task that outlasts the budget must be reported as unfinished")
            Assertions.assertTrue(elapsed < 30_000, "must give up on the budget, not on the task (took ${elapsed}ms)")
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    /** A budget already spent stops immediately rather than waiting once per remaining task. */
    @Test
    fun anExhaustedBudgetDoesNotWaitPerTask() {
        val pool = Executors.newFixedThreadPool(2)
        val release = CountDownLatch(1)
        try {
            val blocked = (1..4).map { pool.submit { release.await(5, TimeUnit.MINUTES) } }
            val startedAt = System.currentTimeMillis()

            Assertions.assertFalse(DockerJavaContainerEngine.awaitWithin(blocked, Duration.ZERO))
            Assertions.assertTrue(
                System.currentTimeMillis() - startedAt < 30_000,
                "four blocked tasks must cost one budget between them, never one each"
            )
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    /** Nothing to wait for is a completed drain, not an expired one. */
    @Test
    fun anEmptyDrainCompletes() {
        Assertions.assertTrue(DockerJavaContainerEngine.awaitWithin(emptyList<Future<*>>(), Duration.ZERO))
    }
}
