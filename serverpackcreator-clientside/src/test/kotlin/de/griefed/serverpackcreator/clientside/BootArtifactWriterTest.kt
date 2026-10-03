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
 * Pins the writer that puts a boot's evidence somewhere staging will not wipe it.
 *
 * The case that matters is the second guard: one candidate's first boot and its re-checks all stage into
 * the *same* attempt directory by design, so a writer keyed on the attempt name alone keeps exactly one of
 * them — and the re-checks are the attempts a contested verdict is argued with.
 *
 * @author Griefed
 */
internal class BootArtifactWriterTest {

    /** A staged pack with a crash report and a log, i.e. the shape a failed boot leaves behind. */
    private fun stagedPack(dir: File, attempt: String = "Modrinth-jei-Forge-1.20"): BootVerifier.Prepared.Ready {
        val serverPack = File(dir, "$attempt/serverpack")
        File(serverPack, "logs").mkdirs()
        File(serverPack, "crash-reports").mkdirs()
        File(serverPack, "logs/latest.log").writeText("[main] mod loading\n")
        File(serverPack, "crash-reports/crash-2026-10-03.txt").writeText("java.lang.NoClassDefFoundError\n")
        return BootVerifier.Prepared.Ready(serverPack, File(dir, "$attempt/boot.log"), "1.20.1", "Forge", "47.2.0")
    }

    /** An outcome with [result] and a console, which is the half of the evidence the pack itself never holds. */
    private fun outcome(result: BootResult, console: String = "Exception in server tick loop\n") =
        BootVerifier.BootOutcome(result, null, "detail", console = console, bootedLoader = "Forge")

    /**
     * A crash's console, the server's own logs and the crash report all land, and the index naming what was
     * found lands with them — a capped set of logs that does not say so reads as a complete one.
     */
    @Test
    fun aCrashedBootsEvidenceIsWrittenUnderItsOwnDirectory(@TempDir dir: File) {
        val written = BootArtifactWriter(File(dir, "evidence")).keep(stagedPack(dir), outcome(BootResult.CRASHED))

        val names = written.map { it.name }.toSet()
        Assertions.assertTrue(
            names.containsAll(setOf(BootArtifacts.CONSOLE_NAME, BootArtifacts.INDEX_NAME)),
            "the console and the index are the two a reader cannot reconstruct; got $names"
        )
        Assertions.assertTrue(
            names.any { it.startsWith("crash-reports-") }, "the crash report is the evidence itself; got $names"
        )
        Assertions.assertTrue(
            names.any { it.startsWith("logs-") },
            "logs/latest.log holds what stdout never sees; got $names"
        )
        Assertions.assertTrue(written.all { it.isFile && it.length() > 0 }, "every named file must exist and be non-empty")
    }

    /** A boot that reached its ready line explains nothing, so there is nothing worth the disk. */
    @Test
    fun aSurvivingBootKeepsNothing(@TempDir dir: File) {
        val written = BootArtifactWriter(File(dir, "evidence")).keep(stagedPack(dir), outcome(BootResult.SURVIVED))

        Assertions.assertTrue(written.isEmpty(), "BootArtifacts.worthKeeping is the one retention rule; got $written")
    }

    /**
     * **The whole point of the class.** Three boots of one candidate share one attempt directory, so two
     * keeps under the same attempt name must not land in the same place. Written against the same pack
     * twice, which is exactly what the newest-build re-check does.
     */
    @Test
    fun eachAttemptGetsItsOwnDirectory(@TempDir dir: File) {
        val writer = BootArtifactWriter(File(dir, "evidence"))
        val pack = stagedPack(dir)

        val first = writer.keep(pack, outcome(BootResult.CRASHED, console = "first boot\n"))
        val second = writer.keep(pack, outcome(BootResult.INCONCLUSIVE, console = "the re-check\n"))

        val firstConsole = first.single { it.name == BootArtifacts.CONSOLE_NAME }
        val secondConsole = second.single { it.name == BootArtifacts.CONSOLE_NAME }
        Assertions.assertNotEquals(
            firstConsole.parentFile, secondConsole.parentFile,
            "a re-check staging into the crashing attempt's directory must not overwrite its evidence"
        )
        Assertions.assertEquals("first boot\n", firstConsole.readText(), "the first boot's console survives the second")
        Assertions.assertEquals("the re-check\n", secondConsole.readText())
    }

    /**
     * The attempt directory is filed under the `(platform, slug, loader, Minecraft line)` tuple everything
     * else in the staging layer is keyed by, so a downloaded artifact can be read without a verdict beside
     * it, and it names the loader build that actually ran — which differs between a boot and its
     * newest-build re-check while every other part of the name stays identical.
     */
    @Test
    fun theDirectoryNamesTheAttemptAndTheLoaderBuildThatRan(@TempDir dir: File) {
        val pack = stagedPack(dir)

        val written = BootArtifactWriter(File(dir, "evidence")).keep(pack, outcome(BootResult.CRASHED))

        val attemptDirectory = written.first().parentFile
        Assertions.assertEquals(
            pack.attemptName, attemptDirectory.parentFile.name,
            "evidence is filed under the same tuple the staging directory is named for"
        )
        Assertions.assertTrue(
            attemptDirectory.name.contains("Forge") && attemptDirectory.name.contains("47.2.0"),
            "the loader build is what tells two attempts of one tuple apart; got ${attemptDirectory.name}"
        )
    }
}
