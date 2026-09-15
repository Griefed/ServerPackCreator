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
 * Pins the **live** boot log. A boot takes minutes; before this the console was buffered in memory and only
 * written once the run finished, so a hung boot could not be inspected until its timeout fired and a boot killed
 * mid-flight left nothing behind at all. The log file must therefore be readable *while* the server is still
 * booting — that is what makes `tail -f` on an in-flight boot possible.
 */
internal class BootVerifierLiveLogTest {

    @TempDir
    lateinit var dir: File

    private fun preparedPack(root: File): BootVerifier.Prepared.Ready {
        val serverPack = File(root, "serverpack").apply { mkdirs() }
        return BootVerifier.Prepared.Ready(serverPack, File(root, "boot.log"), "1.21.1", "Forge", "52.1.16")
    }

    /**
     * The runner reads the log file back *from inside its own run*, which is only possible if the caller is
     * appending as lines arrive rather than writing at the end.
     */
    @Test
    fun consoleLinesReachTheLogFileWhileTheBootIsStillRunning() {
        val pack = preparedPack(dir)
        var seenMidRun: String? = null
        val runner = ServerRunner { _, _, onLine ->
            onLine("[Server thread/INFO]: Starting minecraft server")
            onLine("[Server thread/INFO]: Preparing level \"world\"")
            // Still "booting" here — whatever is on disk now is what an operator tailing the file would see.
            seenMidRun = pack.logFile.takeIf { it.isFile }?.readText()
            onLine("[Server thread/INFO]: Done (4.2s)! For help, type \"help\"")
            RunResult.Completed(listOf("[Server thread/INFO]: Done (4.2s)! For help, type \"help\""), 0, false)
        }

        BootVerifier.runPrepared(pack, runner, null, Duration.ofMinutes(1))

        Assertions.assertNotNull(seenMidRun, "the log file must exist during the boot, not only after it")
        Assertions.assertTrue(
            seenMidRun!!.contains("Starting minecraft server") && seenMidRun!!.contains("Preparing level"),
            "lines already streamed must be on disk mid-run, was: $seenMidRun"
        )
        Assertions.assertFalse(
            seenMidRun!!.contains("Done (4.2s)"),
            "a line not yet emitted must not be there — otherwise the test proves nothing about streaming"
        )
    }

    /** After the run the file holds the authoritative console, whatever the streaming did. */
    @Test
    fun theFinishedLogHoldsTheCompleteConsole() {
        val pack = preparedPack(dir)
        val console = listOf("first", "second", "[Server thread/INFO]: Done (1.0s)! For help")
        val runner = ServerRunner { _, _, onLine ->
            console.forEach(onLine)
            RunResult.Completed(console, 0, false)
        }

        val outcome = BootVerifier.runPrepared(pack, runner, null, Duration.ofMinutes(1))

        Assertions.assertEquals(BootResult.SURVIVED, outcome.result)
        Assertions.assertEquals(console, pack.logFile.readLines().filter { it.isNotBlank() })
    }

    /**
     * A boot that never produces a terminal result — killed, or timing out — must still leave its console on
     * disk. This is the case the old write-at-the-end behaviour lost entirely.
     */
    @Test
    fun outputSurvivesARunThatEndsWithoutAResult() {
        val pack = preparedPack(dir)
        val runner = ServerRunner { _, _, onLine ->
            onLine("[Server thread/INFO]: Preparing spawn area: 12%")
            throw IllegalStateException("container vanished")
        }

        runCatching { BootVerifier.runPrepared(pack, runner, null, Duration.ofMinutes(1)) }

        Assertions.assertTrue(pack.logFile.isFile, "the log must exist even though the run threw")
        Assertions.assertTrue(
            pack.logFile.readText().contains("Preparing spawn area"),
            "whatever the server said before it died has to be recoverable"
        )
    }

    /** A broken sink must never take the boot down — logging is diagnostics, not a dependency. */
    @Test
    fun anUnwritableLogFileDoesNotFailTheBoot() {
        val serverPack = File(dir, "serverpack").apply { mkdirs() }
        // A *directory* where the log file should be: opening it as a writer fails.
        val pack = BootVerifier.Prepared.Ready(serverPack, File(dir, "boot.log").apply { mkdirs() }, "1.21.1", "Forge", "52.1.16")
        val runner = ServerRunner { _, _, onLine ->
            onLine("still booting")
            RunResult.Completed(listOf("[Server thread/INFO]: Done (1.0s)! For help"), 0, false)
        }

        val outcome = runCatching { BootVerifier.runPrepared(pack, runner, null, Duration.ofMinutes(1)) }

        Assertions.assertTrue(outcome.isSuccess, "an unwritable log must not propagate: ${outcome.exceptionOrNull()}")
    }
}
