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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

/**
 * Pins that tearing a boot down kills the **server**, not only the shell that launched it.
 *
 * `start.sh` is a launcher: the Minecraft server is a `java` child of the `bash` the runner spawned.
 * `destroyForcibly()` sends SIGKILL to that one process, and SIGKILL cannot be trapped or forwarded, so the
 * child was reparented to init and kept running — holding the world directory, the port, and several GB of
 * heap. Nothing in this repository called [ProcessHandle.descendants] before this guard.
 *
 * It is invisible to every other test because [RunResult] reports the *shell's* exit, which is exactly what a
 * force-kill produces whether or not the server died with it. The only way to see it is to ask the operating
 * system afterwards, which is what this does: a stand-in start script records its child's PID, and the
 * assertion is made against that PID once the run has returned.
 */
internal class HostProcessDescendantTeardownTest {

    /** The PID the stand-in script reported, kept so a failing run cannot leak the process it spawned. */
    private var spawnedChildPid: Long? = null

    /**
     * Kill the stand-in's child if the assertion failed, so a red run does not leave a process behind for
     * every re-run. A passing run finds nothing to do, which is the point of the test.
     */
    @AfterEach
    fun killLeakedChild() {
        spawnedChildPid?.let { pid -> ProcessHandle.of(pid).ifPresent { it.destroyForcibly() } }
    }

    /**
     * A boot whose script spawns a long-lived child must leave no trace of that child once the runner returns.
     *
     * The script never prints the ready-line, so the run ends by timing out and force-killing — the same path
     * a hung or crashed server takes, and the one where a survivor is least likely to be noticed.
     */
    @Test
    fun tearingDownABootKillsTheProcessesTheScriptSpawned(@TempDir packDir: File) {
        Assumptions.assumeTrue(File("/bin/bash").canExecute(), "Needs bash; the runner spawns `bash start.sh`.")

        val pidFile = File(packDir, "child.pid")
        File(packDir, "start.sh").writeText(
            """
            #!/usr/bin/env bash
            # Stands in for a real start script: the server is a CHILD of this shell, never this shell itself.
            sleep 300 &
            echo ${'$'}! > "${pidFile.absolutePath}"
            wait
            """.trimIndent() + "\n"
        )

        HostProcessServerRunner().run(packDir, Duration.ofSeconds(2)) { }

        Assertions.assertTrue(pidFile.isFile, "The stand-in script never reported a child PID.")
        val childPid = pidFile.readText().trim().toLong().also { spawnedChildPid = it }
        Assertions.assertFalse(
            ProcessHandle.of(childPid).map { it.isAlive }.orElse(false),
            "Process $childPid outlived the boot that spawned it. Killing the launcher shell does not kill " +
                    "the server it started; the descendants have to be destroyed too."
        )
    }
}
