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

import de.griefed.serverpackcreator.grinder.container.MAX_PARALLEL_STOPS
import de.griefed.serverpackcreator.grinder.container.SHUTDOWN_GRACE
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.ceil

/**
 * Keeps the shutdown contract joined up across the three places it lives: `main`'s hook, the shipped unit, and
 * the grace window both quote.
 *
 * None of it can be executed — the hook builds an `ApiWrapper` and a Docker client — so it is asserted against
 * the sources, the same technique `ReportBindWiringTest` and `FallbackListWiringTest` use. Audit iteration 20,
 * M2/M3/M4: every piece worked, and nothing would have noticed if one of them stopped.
 */
internal class ShutdownWiringTest {

    private val unit = File("deploy/spc-grinder.service")

    /** `TimeoutStopSec=` from the shipped unit, in seconds. */
    private fun unitStopTimeoutSeconds(): Long {
        val value = Regex("""(?m)^TimeoutStopSec=(\d+)s?$""").find(unit.readText())
            ?: Assertions.fail("the unit no longer sets TimeoutStopSec, so systemd's 90s default silently applies")
        return value.groupValues[1].toLong()
    }

    @Test
    fun theShutdownHookStopsContainersBeforeItWaitsOnWorkers() {
        val body = grinderMainBody()
        val closesEngine = body.indexOf("engine.close()")
        val awaitsWorkers = body.indexOf("awaitStop(")

        Assertions.assertTrue(closesEngine >= 0, "main() no longer closes the container engine on shutdown")
        Assertions.assertTrue(awaitsWorkers >= 0, "main() no longer waits for the workers to stop")
        Assertions.assertTrue(
            closesEngine < awaitsWorkers,
            "the engine must be closed BEFORE waiting on workers: close() is what sets the closed flag, so " +
                "reversing these re-opens the window in which a worker starts a container behind the sweep"
        )
    }

    @Test
    fun startupReapsContainersAKilledRunLeftBehind() {
        Assertions.assertTrue(
            grinderMainBody().contains("reapOrphans()"),
            "main() no longer reaps orphaned containers at startup — after a SIGKILL they are unreachable " +
                "forever, since they belong to the docker daemon's control group rather than the unit's"
        )
    }

    /**
     * Fifteen seconds is not an implementation detail; it is the number the unit's comments and README §5 both
     * quote to an operator, and the window a mod's server gets to save its world.
     */
    @Test
    fun theGraceWindowIsTheFifteenSecondsTheDocumentationPromises() {
        Assertions.assertEquals(15L, SHUTDOWN_GRACE.seconds)
    }

    /**
     * The unit's stop timeout must outlast the cleanup, or systemd's SIGKILL lands *during* the very work that
     * prevents orphaned containers. The arithmetic is the unit's own: stops run concurrently up to
     * [MAX_PARALLEL_STOPS], so the container phase costs `ceil(workers / cap) * grace` — one window for any
     * worker count at or below the cap, which is every realistic deployment. Checked against a generous worker
     * count rather than the default, because raising SPC_GRINDER_WORKERS is the normal thing to do and nothing
     * else would catch it.
     */
    @Test
    fun theUnitAllowsEnoughTimeForTheCleanupItDependsOn() {
        val workers = 16L
        val batches = ceil(workers.toDouble() / MAX_PARALLEL_STOPS).toLong()
        val cleanupSeconds = batches * SHUTDOWN_GRACE.seconds

        Assertions.assertTrue(
            unitStopTimeoutSeconds() > cleanupSeconds,
            "TimeoutStopSec=${unitStopTimeoutSeconds()}s does not outlast a ${cleanupSeconds}s cleanup at " +
                "$workers workers — systemd would SIGKILL mid-sweep and leave containers running"
        )
    }

    /**
     * Audit iteration 21, P21-H1. The one-shot run built its pool inline and never stored it in `activePool`,
     * which is the only handle the shutdown hook has — so Ctrl-C during the end-to-end verification path
     * signalled and awaited nothing, both calls no-opping through a null receiver. It looked fine because the
     * engine still closed and the boots collapsed with their containers.
     */
    @Test
    fun theOneShotRunRegistersItsPoolSoCtrlCCanStopIt() {
        val body = grinderMainBody()
        // Counting `activePool.set(` occurrences would be the obvious check and is wrong: the pass loop also
        // clears the reference with `activePool.set(null)`, so a reset reads as a registration and the
        // unregistered one-shot pool passed. Each construction is therefore checked against what follows it.
        val constructions = Regex("""GrindPool\([^)]*\)""").findAll(body).toList()
        Assertions.assertTrue(constructions.isNotEmpty(), "main() no longer builds a GrindPool — did it change shape?")

        val unregistered = constructions.filterNot { construction ->
            body.substring(construction.range.last, minOf(body.length, construction.range.last + 60))
                .contains("activePool.set(")
        }
        Assertions.assertTrue(
            unregistered.isEmpty(),
            "${unregistered.size} GrindPool(s) are built without reaching activePool, which is the only handle " +
                "the shutdown hook has — an unregistered pool's workers are never signalled or awaited"
        )
    }

    /**
     * Audit iteration 21, P21-M1. The workers must get what remains of the *shared* window, not a second full
     * one after the containers have spent it — the hook's own log line, its comment and README §5 all promise
     * a single 15-second budget, and the unit's TimeoutStopSec arithmetic assumes the two overlap.
     */
    @Test
    fun theWorkersGetTheRemainderOfTheWindowRatherThanASecondOne() {
        val body = grinderMainBody()

        Assertions.assertTrue(body.contains("awaitStop("), "main() no longer waits for the workers at all")
        // Positive, not "does not contain awaitStop(SHUTDOWN_GRACE)": an absence passes for any spelling that
        // is not that exact string, so a rewrite could reintroduce the second window under another name.
        Assertions.assertTrue(
            body.contains("val deadline = System.currentTimeMillis() + SHUTDOWN_GRACE"),
            "the hook must take a deadline at entry, or the two halves cannot share one window"
        )
        Assertions.assertTrue(
            Regex("""awaitStop\(\s*remaining\s*\)""").containsMatchIn(body),
            "awaitStop must be handed what is left of the shared window, not a window of its own"
        )
    }

}
