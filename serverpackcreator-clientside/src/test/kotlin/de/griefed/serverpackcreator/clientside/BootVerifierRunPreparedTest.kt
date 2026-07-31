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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

/**
 * Pins [BootVerifier.runPrepared] — the post-process → boot → classify path the grinder hooks into,
 * tested directly (no `ApiWrapper`, unlike staging). Covers ordering (the post-processor runs before
 * the boot), the swallow-and-skip on a thrown hook, and the no-hook pass-through.
 */
internal class BootVerifierRunPreparedTest {

    private fun preparedPack(dir: File) =
        BootVerifier.Prepared.Ready(File(dir, "pack"), File(dir, "boot.log"), "1.20.1", "Forge", "47.2.0")

    @Test
    fun invokesPostProcessorBeforeBootingThenClassifies(@TempDir dir: File) {
        val pack = preparedPack(dir)
        val order = mutableListOf<String>()
        val handed = mutableListOf<BootVerifier.Prepared.Ready>()
        val runner = ServerRunner { _, _, _ ->
            order.add("run")
            RunResult.Completed(listOf("[Server thread/INFO]: Done (1.0s)! For help"), 0, false)
        }

        val outcome = BootVerifier.runPrepared(pack, runner, { order.add("process"); handed.add(it) }, Duration.ofMinutes(1))

        Assertions.assertEquals(listOf("process", "run"), order, "post-processor must run before the boot")
        Assertions.assertEquals(pack, handed.single(), "the hook receives the staged pack")
        Assertions.assertEquals(BootResult.SURVIVED, outcome.result)
    }

    @Test
    fun aThrownPostProcessorIsInconclusiveAndSkipsTheBoot(@TempDir dir: File) {
        var booted = false
        val runner = ServerRunner { _, _, _ -> booted = true; RunResult.Completed(emptyList(), 0, false) }

        val outcome = BootVerifier.runPrepared(preparedPack(dir), runner, { throw IllegalStateException("overlay failed") }, Duration.ofMinutes(1))

        Assertions.assertEquals(BootResult.INCONCLUSIVE, outcome.result)
        Assertions.assertFalse(booted, "a failed post-processor must skip the boot")
        Assertions.assertTrue(outcome.detail.contains("overlay failed"), "detail surfaces the cause: ${outcome.detail}")
    }

    @Test
    fun nullPostProcessorBootsDirectly(@TempDir dir: File) {
        val runner = ServerRunner { _, _, _ ->
            RunResult.Completed(listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Foo"), 1, false)
        }

        val outcome = BootVerifier.runPrepared(preparedPack(dir), runner, null, Duration.ofMinutes(1))

        Assertions.assertEquals(BootResult.CRASHED, outcome.result)
        Assertions.assertNotNull(outcome.crashExcerpt)
    }
}
