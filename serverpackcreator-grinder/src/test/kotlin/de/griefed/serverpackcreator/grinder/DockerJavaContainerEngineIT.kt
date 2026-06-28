/* Copyright (C) 2025 Griefed
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
}
