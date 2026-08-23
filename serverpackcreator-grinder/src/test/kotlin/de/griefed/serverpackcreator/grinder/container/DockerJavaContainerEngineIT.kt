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

import com.github.dockerjava.api.DockerClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Duration

/**
 * Integration test for the one piece no unit test can cover: [DockerJavaContainerEngine] against a
 * **live Docker daemon**. Gated behind `GRINDER_DOCKER_IT=1` so it never runs in a daemon-less CI; it
 * needs the `busybox:latest` image present (`docker pull busybox`). Exercises the full path —
 * create → start → stream logs → ready-detect/stop (or natural exit) → inspect exit code → remove —
 * under the production hardening defaults (`--network none`, read-only rootfs, dropped caps, non-root).
 */
@EnabledIfEnvironmentVariable(named = "GRINDER_DOCKER_IT", matches = "1")
internal class DockerJavaContainerEngineIT {

    private val engine = DockerJavaContainerEngine()

    private fun busyboxSpec(script: String) = ContainerSpec(
        image = "busybox:latest",
        command = listOf("sh", "-c", script),
        workingDir = "/",
        mounts = emptyList()
    )

    @Test
    fun capturesConsoleAndNonZeroExitFromARealContainer() {
        val output = engine.run(
            busyboxSpec("echo hello-from-container; echo crashing-now; exit 3"),
            readyPattern = Regex("this-never-appears"),
            timeout = Duration.ofSeconds(30)
        )

        Assertions.assertTrue(output.lines.any { it.contains("hello-from-container") }, "stdout must be captured: ${output.lines}")
        Assertions.assertEquals(3, output.exitCode, "the container's non-zero exit must be read back")
        Assertions.assertFalse(output.timedOut)
    }

    @Test
    fun detectsReadyLineAndStopsALongRunningContainerPromptly() {
        val startedAt = System.currentTimeMillis()
        val output = engine.run(
            busyboxSpec("echo 'Done (2.5s)! For help, type help'; sleep 120"),
            readyPattern = Regex("""Done \([^)]*\)! For help"""),
            timeout = Duration.ofSeconds(60)
        )
        val elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000

        Assertions.assertTrue(output.lines.any { it.contains("For help") }, "ready line must be captured: ${output.lines}")
        Assertions.assertFalse(output.timedOut, "ready was seen, so this is not a timeout")
        Assertions.assertTrue(elapsedSeconds < 30, "must stop on ready, not wait out the 120s sleep (took ${elapsedSeconds}s)")
    }

    /**
     * The shutdown drain: a boot abandoned by JVM teardown never reaches [DockerJavaContainerEngine.run]'s
     * `finally`, so [DockerJavaContainerEngine.close] must remove whatever is still in flight. Simulated by
     * starting a long-running container on another thread and closing the engine while it runs — observed
     * once for real, when a `SIGTERM` mid-boot left a Minecraft server container behind.
     */
    @Test
    fun closeRemovesAContainerLeftRunningByAnAbandonedRun() {
        val drainEngine = DockerJavaContainerEngine()
        val booting = Thread {
            runCatching {
                drainEngine.run(busyboxSpec("echo booting; sleep 300"), Regex("this-never-appears"), Duration.ofMinutes(5))
            }
        }.apply { isDaemon = true; start() }

        // Wait for the container to actually exist before pulling the rug out.
        val client = DockerJavaContainerEngine.defaultClient()
        val deadline = System.currentTimeMillis() + 60_000
        var running = countBusyboxSleepers(client)
        while (running == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(500)
            running = countBusyboxSleepers(client)
        }
        Assertions.assertTrue(running > 0, "the probe container should be running before close()")

        drainEngine.close()
        booting.interrupt()

        // close() force-removes, so the sleeper must be gone almost immediately.
        val goneBy = System.currentTimeMillis() + 30_000
        while (countBusyboxSleepers(client) > 0 && System.currentTimeMillis() < goneBy) {
            Thread.sleep(500)
        }
        Assertions.assertEquals(0, countBusyboxSleepers(client), "close() must force-remove abandoned containers")
    }

    /** Count running containers that look like this test's probe, so the assertion can't match anything else. */
    private fun countBusyboxSleepers(client: DockerClient): Int =
        client.listContainersCmd().withShowAll(false).exec()
            .count { container ->
                container.image == "busybox:latest" && (container.command?.contains("sleep 300") == true)
            }

    /**
     * `systemctl stop` must *signal* a container, not shoot it. `close` therefore issues a `docker stop` with a
     * bounded grace window before force-removing, so a Minecraft server gets its chance to save and exit — the
     * previous behaviour went straight to `remove --force`, which is a SIGKILL to PID 1 and loses the world save
     * of an in-flight boot.
     *
     * Asserted on the container's own exit: a shell trapping TERM writes its marker and exits 0 only if the
     * signal actually arrived.
     */
    @Test
    fun closeSignalsAContainerBeforeKillingIt() {
        val signalEngine = DockerJavaContainerEngine()
        val sawSignal = java.util.concurrent.atomic.AtomicBoolean(false)
        val booting = Thread {
            runCatching {
                signalEngine.run(
                    busyboxSpec("trap 'echo GRACEFUL-TERM; exit 0' TERM; echo ready-to-be-stopped; while true; do sleep 1; done"),
                    Regex("this-never-appears"),
                    Duration.ofMinutes(5)
                ) { line -> if (line.contains("GRACEFUL-TERM")) sawSignal.set(true) }
            }
        }.apply { isDaemon = true; start() }

        waitForContainer(signalEngine)
        signalEngine.close()
        booting.join(30_000)

        Assertions.assertTrue(sawSignal.get(), "the container must receive SIGTERM and get to run its handler before removal")
        Assertions.assertTrue(runningGrinderContainers().isEmpty(), "nothing may be left running after close")
    }

    /**
     * The teardown race: `requestStop` only stops a worker taking a *new candidate*, and once shutdown hooks are
     * running the JVM no longer waits for worker threads — so a worker between its loader install and its mod
     * boot could create a container *after* `close` had already swept, and that one was never removed.
     */
    @Test
    fun refusesToCreateAContainerOnceClosed() {
        val closedEngine = DockerJavaContainerEngine()
        closedEngine.close()

        Assertions.assertThrows(IllegalStateException::class.java) {
            closedEngine.run(busyboxSpec("echo should-never-start"), Regex("x"), Duration.ofSeconds(30))
        }
        Assertions.assertTrue(runningGrinderContainers().isEmpty(), "a refused run must leave nothing behind")
    }

    /**
     * The SIGKILL case, which no in-process hook can cover: systemd kills the JVM before `close` finishes and the
     * containers keep running, parented by the docker daemon rather than the unit's cgroup. They carry a label so
     * the next start can find and remove them — without one, an orphan survives every restart forever.
     */
    @Test
    fun reapsALabelledOrphanLeftByAPreviousProcess() {
        val orphanEngine = DockerJavaContainerEngine()
        Thread {
            runCatching {
                orphanEngine.run(busyboxSpec("echo orphan-alive; sleep 300"), Regex("this-never-appears"), Duration.ofMinutes(5))
            }
        }.apply { isDaemon = true; start() }
        waitForContainer(orphanEngine)
        // Forget the container the way a killed JVM does: the tracking set dies with the process, the container
        // does not. A fresh engine is exactly what the next `systemctl start` brings up.
        Assertions.assertEquals(1, runningGrinderContainers().size, "test setup: the orphan must be running")

        val reaped = DockerJavaContainerEngine().reapOrphans()

        Assertions.assertEquals(1, reaped, "the labelled orphan must be found and removed")
        Assertions.assertTrue(runningGrinderContainers().isEmpty(), "no grinder container may survive the reap")
    }

    /** Every container this engine owns, by the label it stamps on them. */
    private fun runningGrinderContainers(): List<String> =
        DockerJavaContainerEngine.defaultClient().listContainersCmd().withShowAll(true)
            .withLabelFilter(mapOf(DockerJavaContainerEngine.OWNER_LABEL to "1"))
            .exec().map { it.id }

    /** Block until [engine] actually has a container up, so a test never pulls the rug before there is one. */
    private fun waitForContainer(engine: DockerJavaContainerEngine) {
        val until = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < until && runningGrinderContainers().isEmpty()) {
            Thread.sleep(200)
        }
    }

}
