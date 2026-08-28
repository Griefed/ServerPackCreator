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

/**
 * Pins what a finished boot leaves behind. Only the container console was ever kept, and only for a
 * crash — so a mod wrongly *cleared* left no evidence at all, and neither did an error in the checking
 * itself. The server's own `logs/` and `crash-reports/` were never read anywhere in the codebase.
 */
internal class BootArtifactsTest {

    /** A staged pack with the files a real boot leaves in it, plus decoys that must not be collected. */
    private fun serverPack(dir: File): File = File(dir, "serverpack").apply {
        File(this, "logs").mkdirs()
        File(this, "crash-reports").mkdirs()
        File(this, "mods").mkdirs()
        File(this, "logs/latest.log").writeText("latest log body")
        File(this, "logs/debug.log").writeText("debug log body")
        File(this, "crash-reports/crash-2026-08-28.txt").writeText("crash report body")
        File(this, "mods/some-mod.jar").writeText("not a log")
        File(this, "server.properties").writeText("not a log either")
    }

    @Test
    fun collectsTheConsoleAndEveryServerLogAndCrashReport(@TempDir dir: File) {
        val artifacts = BootArtifacts.collect(serverPack(dir), console = "console body")

        val byName = artifacts.associateBy { it.name }
        Assertions.assertEquals("console body", byName[BootArtifacts.CONSOLE_NAME]?.content)
        Assertions.assertEquals("latest log body", byName["logs-latest.log"]?.content)
        Assertions.assertEquals("debug log body", byName["logs-debug.log"]?.content)
        Assertions.assertEquals("crash report body", byName["crash-reports-crash-2026-08-28.txt"]?.content)
        Assertions.assertTrue(
            byName.keys.none { it.contains("some-mod") || it.contains("server.properties") },
            "only logs and crash reports are evidence; the pack itself is reproducible — got ${byName.keys}"
        )
    }

    /**
     * The tail, not the head: a stack trace is at the end of a log, and the truncation has to be stated
     * in the file or a reader silently draws conclusions from a partial log.
     */
    @Test
    fun anOversizedArtifactKeepsItsTailWithTheTruncationStated(@TempDir dir: File) {
        val pack = File(dir, "serverpack").apply { File(this, "logs").mkdirs() }
        val log = File(pack, "logs/latest.log")
        log.bufferedWriter().use { writer ->
            repeat(3) { writer.write("y".repeat(1024 * 1024)) }
            writer.write("THE-VERY-END")
        }

        val collected = BootArtifacts.collect(pack, console = null).single { it.name == "logs-latest.log" }

        Assertions.assertTrue(collected.truncated, "an oversized artifact must say it was truncated")
        Assertions.assertTrue(collected.content.endsWith("THE-VERY-END"), "the tail is the part worth keeping")
        Assertions.assertTrue(
            collected.content.length <= BootArtifacts.MAX_BYTES_PER_ARTIFACT + 512,
            "kept ${collected.content.length} bytes, cap is ${BootArtifacts.MAX_BYTES_PER_ARTIFACT}"
        )
    }

    /** Reading a huge log whole to then throw most of it away is how a daemon dies of an OOM it swallowed. */
    @Test
    fun anOversizedArtifactIsNeverReadWholeIntoMemory(@TempDir dir: File) {
        val pack = File(dir, "serverpack").apply { File(this, "logs").mkdirs() }
        val log = File(pack, "logs/latest.log")
        val chunk = "z".repeat(1024 * 1024)
        log.bufferedWriter().use { writer -> repeat(64) { writer.write(chunk) } }
        Assertions.assertTrue(log.length() > BootArtifacts.MAX_BYTES_PER_ARTIFACT * 8L)

        val runtime = Runtime.getRuntime()
        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val collected = BootArtifacts.collect(pack, console = null)
        val peak = runtime.totalMemory() - runtime.freeMemory()

        Assertions.assertTrue(collected.any { it.name == "logs-latest.log" && it.truncated })
        Assertions.assertTrue(
            peak - before < log.length(),
            "collecting a ${log.length() / 1024 / 1024} MiB log allocated ${(peak - before) / 1024 / 1024} MiB — " +
                "it is being read whole before the cap is applied"
        )
    }

    /**
     * A cap that silently drops files reads as "this is everything". The index is what makes the cap
     * observable, so a reader can tell "there was nothing else" from "we chose not to keep it".
     */
    @Test
    fun theArtifactCountIsCappedAndEveryOmissionIsNamedInTheIndex(@TempDir dir: File) {
        val pack = File(dir, "serverpack").apply { File(this, "crash-reports").mkdirs() }
        repeat(20) { index ->
            File(pack, "crash-reports/crash-$index.txt").writeText("body $index")
        }

        val artifacts = BootArtifacts.collect(pack, console = null)
        val index = artifacts.single { it.name == BootArtifacts.INDEX_NAME }

        Assertions.assertEquals(
            BootArtifacts.MAX_ARTIFACTS, artifacts.count { it.name != BootArtifacts.INDEX_NAME },
            "the artifact count must be capped"
        )
        repeat(20) { number ->
            Assertions.assertTrue(
                index.content.contains("crash-$number.txt"),
                "the index must name crash-$number.txt whether it was kept or not"
            )
        }
        Assertions.assertTrue(index.content.contains("not kept"), "the index must mark what was dropped")
    }

    /** A boot of minutes does not rotate its logs; decompressing one to find that out is cost for nothing. */
    @Test
    fun aRotatedArchiveIsSkippedAndSaidSoInTheIndex(@TempDir dir: File) {
        val pack = File(dir, "serverpack").apply { File(this, "logs").mkdirs() }
        File(pack, "logs/latest.log").writeText("kept")
        File(pack, "logs/2026-08-28-1.log.gz").writeText("compressed noise")

        val artifacts = BootArtifacts.collect(pack, console = null)

        Assertions.assertTrue(artifacts.none { it.name.endsWith(".gz") }, "a rotated archive is not read")
        Assertions.assertTrue(
            artifacts.single { it.name == BootArtifacts.INDEX_NAME }.content.contains("2026-08-28-1.log.gz"),
            "skipping it still has to be said out loud"
        )
    }

    /**
     * Retention lives here rather than in the grinder so the CLI verb and the daemon cannot disagree about
     * which boots are worth keeping. A clean boot explains nothing and proves nothing about sideness.
     */
    @Test
    fun onlyANonSurvivedBootIsWorthKeeping() {
        Assertions.assertFalse(BootArtifacts.worthKeeping(BootResult.SURVIVED))
        Assertions.assertTrue(BootArtifacts.worthKeeping(BootResult.CRASHED))
        Assertions.assertTrue(BootArtifacts.worthKeeping(BootResult.INCONCLUSIVE))
    }

    /** A pack with nothing to say yields nothing — not an index announcing its own emptiness. */
    @Test
    fun aPackWithNoLogsAndNoConsoleYieldsNothing(@TempDir dir: File) {
        val pack = File(dir, "serverpack").apply { mkdirs() }

        Assertions.assertTrue(BootArtifacts.collect(pack, console = null).isEmpty())
    }
}
