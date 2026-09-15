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
 * Pins **every** path on which no container ever runs, because the four-verdict redesign fixed one of them
 * and left two behind (audit iteration 34, HIGH-1 and HIGH-2).
 *
 * `Verdict.ERROR` exists to separate *"the grind could not be performed"* from *"the grind ran and taught us
 * nothing"*. Staging refusals were given `stagingPrevented` when the verdict was introduced; these two were
 * not, so they still publish as INCONCLUSIVE — the exact conflation the redesign was built to remove:
 *
 *  - **a thrown pack post-processor.** That hook is the grinder's `overlayLoaderInstall`, a *loader-cache*
 *    operation, so it fails precisely when the host is broken. This is the worst possible site for the bug,
 *    because it is the missing-runtime-image shape: a host defect published as a verdict about a mod.
 *  - **`RunResult.NotStarted`**, which is the runner reporting it never started the server at all.
 *
 * **`aStagedGrindWithNoObservationIsAnError` looks like it already covers the second and does not.** It
 * asserts on `boot == null`, while `NotStarted` produces a *non-null* outcome carrying INCONCLUSIVE, so
 * `verdictOf` never reaches that branch. A guard that appears to cover a case it cannot reach is worse than
 * an absent one, because it stops anyone looking — which is why these are pinned on the outcome itself
 * rather than only through the policy.
 */
internal class PreventedGrindTest {

    private fun preparedPack(dir: File) =
        BootVerifier.Prepared.Ready(File(dir, "pack"), File(dir, "boot.log"), "1.20.1", "Forge", "47.2.0")

    /** The grinder's overlay failing means no container ran, so this is the operator's problem. */
    @Test
    fun aThrownPostProcessorMarksTheGrindPrevented(@TempDir dir: File) {
        val runner = ServerRunner { _, _, _ -> RunResult.Completed(emptyList(), 0, false) }

        val outcome = BootVerifier.runPrepared(
            preparedPack(dir), runner, { throw IllegalStateException("overlay failed") }, Duration.ofMinutes(1)
        )

        Assertions.assertTrue(
            outcome.stagingPrevented,
            "the hook is the loader-cache overlay; when it throws nothing booted, so this is not evidence"
        )
    }

    /** The runner never started the server — definitionally a grind that could not be performed. */
    @Test
    fun aRunThatNeverStartedMarksTheGrindPrevented(@TempDir dir: File) {
        val outcome = BootVerifier.outcomeFor(
            RunResult.NotStarted("No start.sh in the generated server pack."),
            logFile = File(dir, "boot.log"),
            label = "Forge 47.2.0 / Minecraft 1.20.1"
        )

        Assertions.assertTrue(outcome.stagingPrevented, "nothing ran, so nothing about the mod was learned")
    }

    /** A boot that really ran and really failed must stay evidence, or this fix would erase true positives. */
    @Test
    fun aRealBootThatFailedIsNotMarkedPrevented(@TempDir dir: File) {
        val outcome = BootVerifier.outcomeFor(
            RunResult.Completed(listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"), 1, false),
            logFile = File(dir, "boot.log"),
            label = "Forge 47.2.0 / Minecraft 1.20.1"
        )

        Assertions.assertFalse(
            outcome.stagingPrevented,
            "the container ran and the mod crashed — marking this prevented would discard the finding"
        )
        Assertions.assertEquals(BootResult.CRASHED, outcome.result)
    }

    /** End to end: both prevented paths reach [Verdict.ERROR], not [Verdict.INCONCLUSIVE]. */
    @Test
    fun bothPreventedPathsPublishAsError(@TempDir dir: File) {
        val thrownHook = BootVerifier.runPrepared(
            preparedPack(dir),
            ServerRunner { _, _, _ -> RunResult.Completed(emptyList(), 0, false) },
            { throw IllegalStateException("overlay failed") },
            Duration.ofMinutes(1)
        )
        val neverStarted = BootVerifier.outcomeFor(
            RunResult.NotStarted("No start.sh in the generated server pack."),
            File(dir, "never-started.log"), "Forge / 1.20.1"
        )

        listOf(thrownHook, neverStarted).forEach { outcome ->
            Assertions.assertEquals(
                Verdict.ERROR,
                ClientsideVerifier.verdictOf(
                    DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH,
                    bootOutcome = outcome, bootAttempted = true
                ).verdict,
                "a grind that never ran must be an operator's problem: ${outcome.detail}"
            )
        }
    }
}
