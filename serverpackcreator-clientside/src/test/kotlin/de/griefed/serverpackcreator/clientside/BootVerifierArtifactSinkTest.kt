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
 * Pins the hook that lets a caller keep a boot's evidence. It fires *inside* `runPrepared`, per attempt,
 * because staging wipes and re-creates the attempt directory on every stage — so anything read after
 * `verify` returns can only ever see the last attempt, and the re-check attempts are precisely the ones a
 * disputed verdict turns on.
 */
internal class BootVerifierArtifactSinkTest {

    private fun preparedPack(dir: File, loaderVersion: String = "47.2.0") = BootVerifier.Prepared.Ready(
        File(dir, "Modrinth-jei-Forge/serverpack"),
        File(dir, "Modrinth-jei-Forge/boot.log"),
        "1.20.1",
        "Forge",
        loaderVersion
    )

    private fun survivingRunner() = ServerRunner { _, _, _ ->
        RunResult.Completed(listOf("[Server thread/INFO]: Done (1.0s)! For help"), 0, false)
    }

    @Test
    fun theSinkReceivesTheAttemptsOwnPackAndItsOutcome(@TempDir dir: File) {
        val seen = mutableListOf<Pair<BootVerifier.Prepared.Ready, BootVerifier.BootOutcome>>()
        val pack = preparedPack(dir)

        val outcome = BootVerifier.runPrepared(pack, survivingRunner(), null, Duration.ofMinutes(1)) { staged, result ->
            seen.add(staged to result)
        }

        Assertions.assertEquals(1, seen.size, "one attempt, one call")
        Assertions.assertEquals(pack, seen.single().first, "the sink gets the pack that was actually booted")
        Assertions.assertEquals(outcome.result, seen.single().second.result)
        Assertions.assertEquals(
            BootResult.SURVIVED, seen.single().second.result,
            "the sink sees the classified outcome, not a half-built one"
        )
    }

    /**
     * Same rule as the live-log sink beside it: persisting evidence must never fail a boot. A verdict that
     * already ran is not worth losing to a full disk.
     */
    @Test
    fun aThrowingSinkDoesNotFailTheBoot(@TempDir dir: File) {
        val outcome = BootVerifier.runPrepared(preparedPack(dir), survivingRunner(), null, Duration.ofMinutes(1)) { _, _ ->
            throw IllegalStateException("the log store is unwritable")
        }

        Assertions.assertEquals(BootResult.SURVIVED, outcome.result)
    }

    /** A boot that never started still has a pack worth looking at — a partial install is evidence too. */
    @Test
    fun theSinkAlsoSeesARunThatNeverStarted(@TempDir dir: File) {
        val runner = ServerRunner { _, _, _ -> RunResult.NotStarted("no start.sh in the pack") }
        val seen = mutableListOf<BootVerifier.BootOutcome>()

        BootVerifier.runPrepared(preparedPack(dir), runner, null, Duration.ofMinutes(1)) { _, result -> seen.add(result) }

        Assertions.assertEquals(BootResult.INCONCLUSIVE, seen.single().result)
    }

    /** A failed post-processor skips the boot, so there is no attempt to keep evidence for. */
    @Test
    fun aSkippedBootNeverReachesTheSink(@TempDir dir: File) {
        var called = false

        BootVerifier.runPrepared(
            preparedPack(dir), survivingRunner(), { throw IllegalStateException("overlay failed") }, Duration.ofMinutes(1)
        ) { _, _ -> called = true }

        Assertions.assertFalse(called, "nothing was booted, so there is nothing to capture")
    }

    /**
     * The attempt directory's name is the `(platform, slug, loader)` tuple the whole staging layer is keyed
     * by, and a sink needs it to file what it keeps. Deriving it from the log file's parent is what avoids
     * threading three more parameters through every attempt — and it stays correct for the other-version
     * re-check, which deliberately stages into the *crashing* loader's directory.
     */
    @Test
    fun theAttemptNameIsTheStagingDirectoryItBootedFrom(@TempDir dir: File) {
        Assertions.assertEquals(
            AttemptDirectory.nameFor("Modrinth", "jei", "Forge"),
            preparedPack(dir).attemptName
        )
    }

    /**
     * `runPrepared` is called once per attempt from exactly one place. Added at two of the three former
     * call sites, a per-attempt hook would silently cover the first boot and miss both re-checks — and a
     * boot that gets re-checked is by definition a contested one. The collapse can be undone by a later
     * edit without any behavioural test noticing, so the structure itself is what gets pinned.
     */
    @Test
    fun onlyOneCallSiteInvokesRunPrepared() {
        val source = File("src/main/kotlin/de/griefed/serverpackcreator/clientside/BootVerifier.kt")
        Assertions.assertTrue(source.isFile, "BootVerifier.kt not found at ${source.absolutePath}")

        val invocations = Regex("""(?<!fun )runPrepared\(""").findAll(source.readText()).count()

        Assertions.assertEquals(
            1, invocations,
            "every boot attempt must go through BootVerifier.boot(...); found $invocations call sites"
        )
    }
}
