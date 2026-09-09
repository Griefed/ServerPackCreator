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

/**
 * Pins that a boot the **loader build itself** was too old for is re-checked on the newest build, whatever
 * verdict the classifier reached.
 *
 * **The guard existed and stopped covering the case it was written for.** `BootVerifierCrashRecheckTest`'s
 * own doc states the hazard: *"a mod needing a newer loader than the cached build fails to load, the server
 * exits non-zero, `BootLogClassifier` reads that as CRASHED"* — and `shouldRecheckCrash` re-boots a CRASHED
 * outcome on the newest build before letting it stand. Then `dependencyFailureMarkers` was widened
 * (2026-08-29) and that console became **INCONCLUSIVE** instead, which the re-check does not look at. The
 * premise moved out from under the guard.
 *
 * **Measured on the live daemon 2026-09-08**, ten hours after a full clear: **all 511** Fabric boots ran
 * loader `0.19.3` while Fabric's current stable is `0.19.5`, and **17 of 42** `DEPENDENCY_FAILURE` rows are
 * this and nothing else — `fabric-language-kotlin`, a dependency of a great many mods, demands
 * `fabricloader >=0.19.5`. Each one is an INCONCLUSIVE charged to a candidate over the harness's choice of
 * loader build, which is the "never got a fair run" shape this module exists to prevent.
 *
 * `CachedLoaderVersions` even documents the property that does not hold, in the line it logs when it reuses
 * an older cached build: *"(a crash on it is re-checked against <newest> before it counts)"*. True for a
 * crash; false for the dependency failure that same choice actually produces.
 *
 * Every console below is **verbatim from a live boot log**, except the Forge/NeoForge one — that message
 * shape is verbatim (`createaddition`, `Mod ID: 'ponder', … Expected range: …`) with the id being the loader
 * rather than a mod, which is how FML words the same complaint about itself.
 *
 * @author Griefed
 */
internal class LoaderTooOldRecheckTest {

    /** Fabric Loader's own wording, from `Modrinth/libipn` on Fabric 0.19.3 / Minecraft 26.2. */
    private val fabricConsole = listOf(
        "[08:44:09] [main/WARN]: Mod resolution failed",
        "[08:44:09] [main/INFO]: Immediate reason: [HARD_DEP_NO_CANDIDATE fabric-language-kotlin " +
            "1.14.1+kotlin.2.4.20 {depends fabricloader @ [>=0.19.5]}]",
        "[08:44:09] [main/ERROR]: Incompatible mods found!",
        " - Mod 'Fabric Language Kotlin' (fabric-language-kotlin) 1.14.1+kotlin.2.4.20 requires version " +
            "0.19.5 or later of mod 'Fabric Loader' (fabricloader), but only the wrong version is present: 0.19.3!"
    )

    /** Quilt's wording, from `CurseForge/fzzy-config` on Quilt 0.30.1 / Minecraft 26.2. */
    private val quiltConsole = listOf(
        "---- Quilt Loader: Failed to load ----",
        "Quilt Loader Version: 0.30.1",
        "Fabric Language Kotlin requires version [0.19.5, ∞) of fabricloader, but only wrong versions are present:",
        "- version 0.19.3 provided by Quilt Loader from /srv/pack/libraries/org/quiltmc/quilt-loader/0.30.1/quilt-loader-0.30.1.jar"
    )

    /** FML's wording, with the loader as the unmet id rather than a mod. */
    private val neoForgeConsole = listOf(
        "Missing or unsupported mandatory dependencies:",
        "Mod ID: 'neoforge', Requested by: 'somemod', Expected range: '[21.1.100,)', Actual version: '21.1.80'"
    )

    /**
     * **The negative that matters**, verbatim from `CurseForge/createaddition` on NeoForge 21.1.250: a
     * version demand against another *mod*. Re-booting on a newer loader cannot fix that, and treating it as
     * a loader complaint would spend a second container per pack-incoherence — which `DependencyBacktrack`
     * already owns.
     */
    private val modVersionConsole = listOf(
        "Missing or unsupported mandatory dependencies:",
        "Mod ID: 'ponder', Requested by: 'create', Expected range: '[1.0.82,)', Actual version: '1.0.64'"
    )

    private fun outcome(result: BootResult, console: List<String>) =
        BootVerifier.BootOutcome(result, null, "detail", null, console = console.joinToString("\n"))

    // --- reading the console ------------------------------------------------------------------------

    /** All three loaders word it differently; all three are the same fact. */
    @Test
    fun everyLoaderSayingItIsTooOldIsRecognised() {
        Assertions.assertTrue(LoaderVersionDemand.unmetIn(fabricConsole), "Fabric")
        Assertions.assertTrue(LoaderVersionDemand.unmetIn(quiltConsole), "Quilt")
        Assertions.assertTrue(LoaderVersionDemand.unmetIn(neoForgeConsole), "NeoForge")
    }

    /** A demand against a mod is not a demand against the loader, however similar the sentence. */
    @Test
    fun aVersionDemandAgainstAnotherModIsNotALoaderComplaint() {
        Assertions.assertFalse(LoaderVersionDemand.unmetIn(modVersionConsole))
    }

    /** And an ordinary console says nothing of the kind. */
    @Test
    fun anOrdinaryConsoleIsNotALoaderComplaint() {
        Assertions.assertFalse(
            LoaderVersionDemand.unmetIn(listOf("[12:00:00] [main/INFO]: Done (21.533s)! For help, type \"help\""))
        )
        Assertions.assertFalse(LoaderVersionDemand.unmetIn(emptyList()))
    }

    // --- deciding to re-boot ------------------------------------------------------------------------

    /** The live case: INCONCLUSIVE, and the console says the loader build was the problem. */
    @Test
    fun aLoaderTooOldForTheModIsReCheckedEvenWhenItIsNotACrash() {
        Assertions.assertTrue(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.INCONCLUSIVE, fabricConsole), bootedVersion = "0.19.3", latestVersion = "0.19.5"
            )
        )
    }

    /** An INCONCLUSIVE that says nothing about the loader is not worth a second container. */
    @Test
    fun anInconclusiveWithNoLoaderComplaintIsNotReChecked() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.INCONCLUSIVE, modVersionConsole), bootedVersion = "0.19.3", latestVersion = "0.19.5"
            )
        )
    }

    /** The behaviour that already existed is untouched: a crash on a non-newest build is still re-checked. */
    @Test
    fun aCrashOnAnOlderBuildIsStillReChecked() {
        Assertions.assertTrue(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.CRASHED, emptyList()), bootedVersion = "52.1.16", latestVersion = "52.1.20"
            )
        )
    }

    /** Nothing newer to try means nothing to learn, whichever way the boot went. */
    @Test
    fun thereIsNothingToReCheckOnTheNewestBuildItself() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.INCONCLUSIVE, fabricConsole), bootedVersion = "0.19.5", latestVersion = "0.19.5"
            )
        )
        Assertions.assertFalse(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.INCONCLUSIVE, fabricConsole), bootedVersion = "0.19.3", latestVersion = null
            )
        )
    }

    /** A clean boot is never re-run: it already answered the question. */
    @Test
    fun aCleanBootIsNeverReChecked() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckOnNewestBuild(
                outcome(BootResult.SURVIVED, fabricConsole), bootedVersion = "0.19.3", latestVersion = "0.19.5"
            )
        )
    }
}
