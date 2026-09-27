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
import java.util.concurrent.atomic.AtomicBoolean

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

    /** Long enough to be caught running, short enough not to hold the suite up if something goes wrong. */
    private fun sleeperSpec() = ContainerSpec(
        image = "busybox:latest",
        command = listOf("sh", "-c", "echo up-and-waiting; sleep 120"),
        workingDir = "/",
        mounts = emptyList()
    )

    /** Every container this engine has on the daemon, asked for by its own instance label. */
    private fun containersOf(engine: DockerJavaContainerEngine): List<String> =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(true)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.INSTANCE_LABEL to engine.instanceId))
            .exec().map { it.id }

    /** Start a container in the background and block until the daemon reports it, so the test never races. */
    private fun startSleeper(engine: DockerJavaContainerEngine): Thread {
        val booting = Thread {
            runCatching {
                engine.run(sleeperSpec(), Regex("this-never-appears"), Duration.ofMinutes(2)) { }
            }
        }.apply { isDaemon = true; start() }
        val until = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < until && containersOf(engine).isEmpty()) {
            Thread.sleep(200)
        }
        return booting
    }

    /** Two engines on one daemon must be distinguishable, and each must see only its own container. */
    @Test
    fun eachEngineStampsItsOwnContainersAndCanFindThemAlone() {
        val first = DockerJavaContainerEngine()
        val second = DockerJavaContainerEngine()
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
        val closing = DockerJavaContainerEngine()
        val surviving = DockerJavaContainerEngine()
        try {
            startSleeper(closing)
            startSleeper(surviving)

            closing.close()

            Assertions.assertTrue(containersOf(closing).isEmpty(), "the closed engine must take its own container")
            Assertions.assertEquals(1, containersOf(surviving).size, "and must leave the other engine's alone")
        } finally {
            surviving.close()
        }
    }

    /**
     * Reaping must not remove the reaper's own live containers.
     *
     * At startup an engine has none, which is why this never bit — but `reapOrphans` is public and nothing
     * stops it being called from a running engine, and its whole definition of an orphan is "not mine".
     */
    @Test
    fun reapingSparesTheReapersOwnContainers() {
        val reaper = DockerJavaContainerEngine()
        try {
            startSleeper(reaper)
            Assertions.assertEquals(1, containersOf(reaper).size, "test setup: the reaper must own a container")

            val reaped = AtomicBoolean(false)
            reaper.reapOrphans().also { reaped.set(it >= 0) }

            Assertions.assertTrue(reaped.get(), "test setup: the reap must have run")
            Assertions.assertEquals(1, containersOf(reaper).size, "a reap must not take the reaper's own container")
        } finally {
            reaper.close()
        }
    }
}
