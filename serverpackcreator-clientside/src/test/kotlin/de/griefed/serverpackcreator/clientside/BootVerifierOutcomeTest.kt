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
package de.griefed.serverpackcreator.clientside

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Pins the shared verdict seam [BootVerifier.outcomeFor] — the classification both the host and the
 * future container runner funnel through, decoupled from any real boot. Covers each [RunResult] branch:
 * a non-launch is INCONCLUSIVE with no log, a crash is CRASHED with an excerpt and a written log, and a
 * ready server SURVIVED with no excerpt.
 */
internal class BootVerifierOutcomeTest {

    @Test
    fun notStartedIsInconclusiveWithNoLog(@TempDir dir: File) {
        val logFile = File(dir, "boot.log")
        val outcome = BootVerifier.outcomeFor(RunResult.NotStarted("No start.sh in the generated server pack."), logFile, "Forge 1.0 / Minecraft 1.20.1")

        Assertions.assertEquals(BootResult.INCONCLUSIVE, outcome.result)
        Assertions.assertNull(outcome.logFile)
        Assertions.assertNull(outcome.crashExcerpt)
        Assertions.assertEquals("No start.sh in the generated server pack.", outcome.detail)
        Assertions.assertFalse(logFile.exists(), "a non-launch must not write a log file")
    }

    @Test
    fun nonZeroExitCrashesWithExcerptAndWrittenLog(@TempDir dir: File) {
        val logFile = File(dir, "boot.log")
        val lines = listOf(
            "[Server thread/INFO]: Starting minecraft server",
            "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft",
            "    at com.example.ClientOnlyMod.init(ClientOnlyMod.java:12)"
        )
        val outcome = BootVerifier.outcomeFor(RunResult.Completed(lines, exitCode = 1, timedOut = false), logFile, "Forge 1.0 / Minecraft 1.20.1")

        Assertions.assertEquals(BootResult.CRASHED, outcome.result)
        Assertions.assertEquals(logFile, outcome.logFile)
        Assertions.assertEquals("Forge 1.0 / Minecraft 1.20.1 → CRASHED", outcome.detail)
        Assertions.assertNotNull(outcome.crashExcerpt)
        Assertions.assertTrue(outcome.crashExcerpt!!.contains("NoClassDefFoundError"))
        Assertions.assertEquals(lines.joinToString("\n"), logFile.readText())
    }

    @Test
    fun readyLineSurvivesWithoutExcerpt(@TempDir dir: File) {
        val logFile = File(dir, "boot.log")
        val lines = listOf("[Server thread/INFO]: Done (21.5s)! For help, type \"help\"")
        val outcome = BootVerifier.outcomeFor(RunResult.Completed(lines, exitCode = 0, timedOut = false), logFile, "Fabric 2.0 / Minecraft 1.21")

        Assertions.assertEquals(BootResult.SURVIVED, outcome.result)
        Assertions.assertEquals(logFile, outcome.logFile)
        Assertions.assertNull(outcome.crashExcerpt, "a clean boot is no crash, so no excerpt")
        Assertions.assertTrue(logFile.exists())
    }
}
