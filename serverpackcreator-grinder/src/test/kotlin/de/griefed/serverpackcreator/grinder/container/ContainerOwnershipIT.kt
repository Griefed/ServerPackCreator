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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Duration

/**
 * Pins that a container says *which* engine made it, not merely that a grinder did.
 *
 * `OWNER_LABEL` answers "a grinder made this" and nothing more, so every question asked of the daemon —
 * what is still running, what may be reaped — is a question about the whole machine. That is fine for the
 * shipped singleton service and false everywhere else, and CI is everywhere else: two `test.yml` jobs share
 * one runner and one daemon, and on 2026-09-27 they ran this module's container suite seven seconds apart
 * (runs 954 and 956) and each failed a *different* test of it. A defect fails the same test in both; that
 * pattern is interference.
 *
 * The identifying label is what makes a scoped question possible at all, which is why it is pinned here and
 * used by `DockerJavaContainerEngineIT` rather than the other way round.
 *
 * **What it does not fix, deliberately:** `reapOrphans` still removes another *live* engine's containers,
 * because no label can tell "left by a process that died" from "in use by a process that is still running"
 * — see the landmine on that method. Two engines sharing a daemon still need to be kept apart by whoever
 * starts them.
 *
 * @author Griefed
 */
@EnabledIfEnvironmentVariable(named = "GRINDER_DOCKER_IT", matches = "1")
internal class ContainerOwnershipIT {

    /**
     * A container that waits to be stopped, and actually stops when asked.
     *
     * The trap and the one-second loop are not decoration. `sh -c "…; sleep 120"` leaves `sh` as PID 1,
     * which ignores SIGTERM and does not forward it, so `docker stop` waits out its whole grace window and
     * `close` gives up and interrupts its own stopper mid-call — a fixture that fails the engine for doing
     * exactly what it promises. The same idiom is in `DockerJavaContainerEngineIT` for the same reason.
     */
    private fun sleeperSpec() = ContainerSpec(
        image = "busybox:latest",
        command = listOf("sh", "-c", "trap 'exit 0' TERM; echo up-and-waiting; while true; do sleep 1; done"),
        workingDir = "/",
        mounts = emptyList()
    )

    /** An engine whose shutdown grace is short, so a test that closes one does not wait out production's. */
    private fun engine() = DockerJavaContainerEngine(shutdownGrace = Duration.ofSeconds(5))

    /** Every container this engine has on the daemon, asked for by its own instance label. */
    private fun containersOf(engine: DockerJavaContainerEngine): List<String> =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(true)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.INSTANCE_LABEL to engine.instanceId))
            .exec().map { it.id }

    /** The containers [engine] has *running*, which is what `close` promises to end. */
    private fun runningContainersOf(engine: DockerJavaContainerEngine): List<String> =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(false)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.INSTANCE_LABEL to engine.instanceId))
            .exec().map { it.id }

    /**
     * Wait for [engine] to have nothing running, rather than asserting the instant `close` returns.
     *
     * `close` budgets the whole sweep: it asks the daemon to stop each container and gives the set one
     * grace window, then interrupts its own stoppers so shutdown cannot hang. A busy daemon can miss that
     * window — measured on the runner while `docker-test.yml` was pulling an image beside it — and the
     * container is then stopped by the daemon a moment later, with its *removal* left to the next
     * startup's reap. That is the documented design, so the test waits for the outcome `close` promises
     * (nothing running) instead of the one it does not (nothing left at all).
     */
    private fun awaitNothingRunning(engine: DockerJavaContainerEngine): List<String> {
        val until = System.currentTimeMillis() + 60_000
        var running = runningContainersOf(engine)
        while (running.isNotEmpty() && System.currentTimeMillis() < until) {
            Thread.sleep(250)
            running = runningContainersOf(engine)
        }
        return running
    }

    /**
     * Start a container in the background and block until the daemon reports it **running**, so the test
     * never races.
     *
     * Not `containersOf`: that asks with `withShowAll(true)`, so it answers the moment `run` has *created*
     * the container — which is several statements before `startContainerCmd`. Every assertion in this class
     * is about what is *running*, and on a contended daemon the gap between the two states is seconds.
     * Measured on the release run for PR #679 (run index 716, 2026-09-27): this test's two sleepers took
     * ~41s just to reach `created`, `closingOneEngineLeavesAnotherEnginesContainerRunning` then read **0**
     * running for an engine nothing had touched, and the release failed on it. The fixture was wrong, not
     * the engine — `close()` had correctly logged "Stopping 1 container(s)", its own and no other.
     *
     * 90s, not 60: the budget now covers starting as well as creating, sized against that same run. Expiry
     * **fails here**, naming what it waited for. A fixture that gives up quietly is the "timing out
     * disguised as a verdict" this file already warned about in prose while doing it anyway.
     */
    private fun startSleeper(engine: DockerJavaContainerEngine): Thread {
        val booting = Thread {
            runCatching {
                engine.run(sleeperSpec(), Regex("this-never-appears"), Duration.ofMinutes(2)) { }
            }
        }.apply { isDaemon = true; start() }
        val until = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < until && runningContainersOf(engine).isEmpty()) {
            Thread.sleep(200)
        }
        Assertions.assertFalse(
            runningContainersOf(engine).isEmpty(),
            "the fixture never got a container of this engine running within 90s; the daemon was too slow " +
                "or the boot failed, and neither is the thing this test is about"
        )
        return booting
    }

    /** Two engines on one daemon must be distinguishable, and each must see only its own container. */
    @Test
    fun eachEngineStampsItsOwnContainersAndCanFindThemAlone() {
        val first = engine()
        val second = engine()
        try {
            Assertions.assertNotEquals(first.instanceId, second.instanceId, "two engines must not share an identity")
            startSleeper(first)
            startSleeper(second)

            val theirs = containersOf(second)
            Assertions.assertEquals(1, containersOf(first).size, "the first engine must see exactly its own container")
            Assertions.assertEquals(1, theirs.size, "the second engine must see exactly its own container")
            Assertions.assertNotEquals(containersOf(first), theirs, "neither may see the other's")
        } finally {
            first.close()
            second.close()
        }
    }

    /**
     * Closing one engine must leave the other's container alone.
     *
     * `close` sweeps an in-process set, so this already held — but nothing said so, and the sweep is one
     * edit away from being written as "every container with the owner label", which is exactly the shape
     * `reapOrphans` has and the reason this suite started failing.
     */
    @Test
    fun closingOneEngineLeavesAnotherEnginesContainerRunning() {
        val closing = engine()
        val surviving = engine()
        try {
            startSleeper(closing)
            startSleeper(surviving)

            closing.close()

            Assertions.assertTrue(
                awaitNothingRunning(closing).isEmpty(),
                "the closed engine's own container must stop"
            )
            Assertions.assertEquals(
                1,
                runningContainersOf(surviving).size,
                "and the other engine's must still be running"
            )
        } finally {
            surviving.close()
        }
    }

    /**
     * Reaping takes another engine's container and spares its own — both halves, in one run.
     *
     * At startup an engine owns nothing, which is why the sparing half never bit; but `reapOrphans` is
     * public, nothing stops a running engine calling it, and its whole definition of an orphan is "not
     * mine". Asserting only the sparing half would pass against a `reapOrphans` that removed *nothing*,
     * so the foreign container is here to make the other half fail if the filter is inverted or absent.
     */
    @Test
    fun reapingTakesAnotherEnginesContainerAndSparesItsOwn() {
        val reaper = engine()
        val foreign = engine()
        try {
            startSleeper(reaper)
            startSleeper(foreign)
            Assertions.assertEquals(1, containersOf(reaper).size, "test setup: the reaper must own a container")
            Assertions.assertEquals(1, containersOf(foreign).size, "test setup: there must be one to reap")

            val reaped = reaper.reapOrphans()

            Assertions.assertTrue(reaped >= 1, "the other engine's container is an orphan to this one, got $reaped")
            Assertions.assertTrue(containersOf(foreign).isEmpty(), "the other engine's container must be reaped")
            Assertions.assertEquals(1, containersOf(reaper).size, "a reap must not take the reaper's own container")
        } finally {
            foreign.close()
            reaper.close()
        }
    }
}
