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
        // The exit status is part of the detail on purpose: when no ready-line appeared it is the input that decides
        // CRASHED vs INCONCLUSIVE, and without it an inconclusive verdict cannot be diagnosed from the report alone.
        Assertions.assertEquals("Forge 1.0 / Minecraft 1.20.1 → CRASHED (exit 1)", outcome.detail)
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

    /**
     * Staging must refuse to boot when a required dependency could not be supplied. The loader would reject the mod
     * before running any of its code, so the run cannot tell client-only from server-safe — it only yields a non-zero
     * exit that *looks* like a crash. Measured 2026-07-30: 36 of 112 kept boot logs failed exactly this way, the
     * largest failure class, each burning a full boot to learn nothing.
     */
    @Test
    fun stagingRefusesToBootWithoutARequiredDependency() {
        val refusal = BootVerifier.refuseForMissingDependencies(setOf("P7dR8mSH"), "Quilt", "1.20.1")

        Assertions.assertNotNull(refusal, "a missing required dependency must stop the boot")
        Assertions.assertTrue(
            refusal!!.detail.contains("P7dR8mSH") && refusal.detail.contains("Quilt") && refusal.detail.contains("1.20.1"),
            "the reason must name the dependency and the combination, was: ${refusal.detail}"
        )
        Assertions.assertTrue(refusal.detail.contains("dependency"), "singular for one missing dependency")
    }

    /** Several missing dependencies are listed in a stable order, so the same failure reads the same way twice. */
    @Test
    fun everyMissingDependencyIsNamedInAStableOrder() {
        val refusal = BootVerifier.refuseForMissingDependencies(setOf("zeta", "alpha"), "Forge", "1.21.1")

        Assertions.assertNotNull(refusal)
        Assertions.assertTrue(refusal!!.detail.contains("alpha, zeta"), "sorted, was: ${refusal.detail}")
        Assertions.assertTrue(refusal.detail.contains("dependencies"), "plural for more than one")
    }

    /** Nothing missing, nothing to report — staging proceeds to the boot. */
    @Test
    fun stagingProceedsWhenEveryDependencyWasStaged() {
        Assertions.assertNull(BootVerifier.refuseForMissingDependencies(emptySet(), "Fabric", "1.20.1"))
    }

    // --- the console that survives a re-check -------------------------------------------------------

    /**
     * Every attempt for one candidate stages into the same directory and therefore writes the same
     * `boot.log`, so a re-check leaves the file holding the *last* boot's console while the reported verdict
     * may come from an earlier one. The grinder's reaper keeps exactly that one file and deletes the rest of
     * the staging, so without this the crash a HIGH was published on is diagnosed from a different boot.
     */
    @Test
    fun theReportedOutcomesConsoleIsWhatEndsUpInItsLogFile(@TempDir dir: File) {
        val logFile = File(dir, "boot.log")
        val crash = BootVerifier.outcomeFor(
            RunResult.Completed(listOf("crashing boot"), exitCode = 1, timedOut = false), logFile, "Forge 1.0 / Minecraft 1.20.2"
        )
        // A later re-check boots another version through the same path and overwrites the file.
        BootVerifier.outcomeFor(RunResult.Completed(listOf("re-check boot"), exitCode = 1, timedOut = false), logFile, "Forge 1.0 / Minecraft 1.20.1")
        Assertions.assertEquals("re-check boot", logFile.readText(), "precondition: the re-check clobbered the crash log")

        BootVerifier.restoreDecisiveConsole(crash)

        Assertions.assertEquals("crashing boot", logFile.readText(), "the verdict's own console must be the one kept")
    }

    /** Nothing to restore, nothing touched — a verdict that never produced a console must not empty a log. */
    @Test
    fun anOutcomeWithoutAConsoleLeavesTheLogAlone(@TempDir dir: File) {
        val logFile = File(dir, "boot.log").apply { writeText("someone else's console") }

        BootVerifier.restoreDecisiveConsole(BootVerifier.BootOutcome(BootResult.INCONCLUSIVE, logFile, "never booted"))

        Assertions.assertEquals("someone else's console", logFile.readText())
    }

    /** Diagnostics may never cost a verdict: an unwritable log is logged and swallowed, exactly as the write is. */
    @Test
    fun anUnwritableLogDoesNotFailTheVerdict(@TempDir dir: File) {
        val directoryInTheWay = File(dir, "boot.log").apply { mkdirs() }
        val outcome = BootVerifier.BootOutcome(BootResult.CRASHED, directoryInTheWay, "detail", "excerpt", "console")

        Assertions.assertDoesNotThrow { BootVerifier.restoreDecisiveConsole(outcome) }
    }

    /**
     * **A refusal has to name its cause, and "could not download" names nothing.**
     *
     * Measured against the live store on 2026-09-01: **21 verdicts** said only `Could not download <file>`,
     * every one of them CurseForge, and the file names — `bwncr`, `tombstone`, `entityculling`,
     * `moreoverlays` — are the population this module already documents as **distribution-locked**
     * (`allowModDistribution=false`, so `downloadUrl` is null and the fetch has to go through the headless
     * browser). Read as written, those 21 are indistinguishable from a 404 or a flaky link, so nobody can
     * tell a broken host from a broken mod. A locked file says so, and names the host prerequisite it needs.
     */
    @Test
    fun aLockedFileSaysWhyItCouldNotBeDownloaded() {
        val locked = ModFile("tombstone-neoforge-26.2-9.9.3.jar", setOf("NeoForge"), setOf("26.2"), null, "https://cf/p", emptyList())

        val reason = BootVerifier.downloadFailureDetail(locked)

        Assertions.assertTrue(reason.contains(locked.fileName), reason)
        Assertions.assertTrue(reason.contains("distribution-locked"), "the cause has to be named: $reason")
        Assertions.assertTrue(
            reason.contains("Modrinth"), "and where the project can be verified instead: $reason"
        )
        // The browser workaround was removed on 2026-09-02; a refusal must not send anyone looking for it.
        Assertions.assertFalse(reason.contains("browser", ignoreCase = true), reason)
        Assertions.assertFalse(reason.contains("Playwright", ignoreCase = true), reason)
    }

    /** An ordinary file's failure must not blame the browser — that would send the operator the wrong way. */
    @Test
    fun anOrdinaryFileFailureDoesNotBlameTheBrowser() {
        val ordinary = ModFile("jei-1.20.1.jar", setOf("Forge"), setOf("1.20.1"), "https://cdn/jei.jar", null, emptyList())

        val reason = BootVerifier.downloadFailureDetail(ordinary)

        Assertions.assertTrue(reason.contains(ordinary.fileName), reason)
        Assertions.assertFalse(reason.contains("distribution-locked"), reason)
        Assertions.assertFalse(reason.contains("browser"), reason)
    }
}
