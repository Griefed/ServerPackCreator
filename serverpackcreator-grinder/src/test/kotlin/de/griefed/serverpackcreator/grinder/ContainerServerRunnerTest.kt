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

import de.griefed.serverpackcreator.clientside.RunResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

/**
 * Pins [ContainerServerRunner]'s host-side behaviour with a fake [ContainerEngine] (no daemon): the
 * "cannot launch" contract, the raw-output mapping onto [RunResult.Completed], and — crucially — that
 * the assembled [ContainerSpec] carries the untrusted-mod hardening (no network, read-only rootfs,
 * dropped capabilities) and mounts the pack. The real docker-java translation is out of unit scope.
 */
internal class ContainerServerRunnerTest {

    /** Records the spec/ready/timeout it was handed and replays a canned output. */
    private class RecordingEngine(private val output: ContainerRunOutput) : ContainerEngine {
        var lastSpec: ContainerSpec? = null
        var called = false
        override fun run(spec: ContainerSpec, readyPattern: Regex, timeout: Duration): ContainerRunOutput {
            called = true
            lastSpec = spec
            return output
        }
    }

    private fun packWithStartScript(@TempDir dir: File): File {
        File(dir, "start.sh").writeText("#!/usr/bin/env bash\necho hi\n")
        return dir
    }

    @Test
    fun reportsNotStartedWithoutLaunchingWhenNoStartScript(@TempDir packDir: File) {
        val engine = RecordingEngine(ContainerRunOutput(emptyList(), 0, false))
        val outcome = ContainerServerRunner(engine, "spc-grind:latest").run(packDir, Duration.ofMinutes(1))

        Assertions.assertTrue(outcome is RunResult.NotStarted)
        Assertions.assertFalse(engine.called, "a pack with no start.sh must never reach the container engine")
    }

    @Test
    fun mapsContainerOutputOntoCompleted(@TempDir packDir: File) {
        val pack = packWithStartScript(packDir)
        val lines = listOf("[Server thread/INFO]: Starting", "java.lang.NoClassDefFoundError: net/minecraft/client/Foo")
        val engine = RecordingEngine(ContainerRunOutput(lines, exitCode = 1, timedOut = false))

        val outcome = ContainerServerRunner(engine, "spc-grind:latest").run(pack, Duration.ofMinutes(1))

        Assertions.assertTrue(outcome is RunResult.Completed)
        val completed = outcome as RunResult.Completed
        Assertions.assertEquals(lines, completed.lines)
        Assertions.assertEquals(1, completed.exitCode)
        Assertions.assertFalse(completed.timedOut)
    }

    @Test
    fun assemblesHardenedSpecMountingThePackAndWritesEula(@TempDir packDir: File) {
        val pack = packWithStartScript(packDir)
        val engine = RecordingEngine(ContainerRunOutput(emptyList(), 0, false))

        ContainerServerRunner(engine, "spc-grind:latest").run(pack, Duration.ofMinutes(1))

        val spec = engine.lastSpec!!
        Assertions.assertEquals("none", spec.networkMode, "an untrusted mod must boot without network")
        Assertions.assertTrue(spec.readonlyRootfs)
        Assertions.assertTrue(spec.dropAllCapabilities)
        Assertions.assertTrue(spec.noNewPrivileges)
        Assertions.assertEquals(listOf("bash", "start.sh"), spec.command)
        Assertions.assertEquals(ContainerServerRunner.PACK_MOUNT, spec.workingDir)
        val packMount = spec.mounts.single()
        Assertions.assertEquals(pack.absolutePath, packMount.hostPath)
        Assertions.assertEquals(ContainerServerRunner.PACK_MOUNT, packMount.containerPath)
        Assertions.assertFalse(packMount.readOnly, "the server writes its world/logs into the pack")
        Assertions.assertEquals("eula=true\n", File(pack, "eula.txt").readText())
    }
}
