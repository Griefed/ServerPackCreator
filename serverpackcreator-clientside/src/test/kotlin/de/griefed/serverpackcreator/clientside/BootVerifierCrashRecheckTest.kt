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
 * Pins the crash re-check — the safeguard that makes it safe for a caller to boot an *older* loader build
 * than the newest one (which the grinder does, to reuse its ~150 MB install cache instead of re-installing
 * every time a loader ships a build).
 *
 * The hazard is specific: a mod needing a newer loader than the cached build fails to load, the server exits
 * non-zero, `BootLogClassifier` reads that as CRASHED, and a perfectly server-safe mod is published as a
 * **HIGH-confidence clientside mod**. This whole module is built around never producing that false HIGH, so a
 * crash on a non-newest build is re-checked against the newest before it is allowed to stand.
 *
 * The two decisions live as pure functions because `BootVerifier.verify` itself needs an `ApiWrapper`, real
 * generation and a running server to exercise — the same split that keeps `outcomeFor` unit-tested.
 */
internal class BootVerifierCrashRecheckTest {

    private fun outcome(result: BootResult, detail: String = "detail", excerpt: String? = null) =
        BootVerifier.BootOutcome(result, null, detail, excerpt)

    // --- when to re-check ---------------------------------------------------------------------------

    @Test
    fun aCrashOnAnOlderBuildIsWorthReChecking() {
        Assertions.assertTrue(
            BootVerifier.shouldRecheckCrash(outcome(BootResult.CRASHED), bootedVersion = "52.1.16", latestVersion = "52.1.20")
        )
    }

    /** Already newest ⇒ nothing better to try, so the crash is the real answer. */
    @Test
    fun aCrashOnTheNewestBuildIsNotReChecked() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckCrash(outcome(BootResult.CRASHED), bootedVersion = "52.1.20", latestVersion = "52.1.20")
        )
    }

    /** Only a crash is decisive, so only a crash is worth a second boot — the others cost time for nothing. */
    @Test
    fun nonCrashOutcomesAreNeverReChecked() {
        listOf(BootResult.SURVIVED, BootResult.INCONCLUSIVE).forEach { result ->
            Assertions.assertFalse(
                BootVerifier.shouldRecheckCrash(outcome(result), bootedVersion = "52.1.16", latestVersion = "52.1.20"),
                "$result must not trigger a re-boot"
            )
        }
    }

    @Test
    fun anUnknownNewestVersionCannotBeReCheckedAgainst() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckCrash(outcome(BootResult.CRASHED), bootedVersion = "52.1.16", latestVersion = null)
        )
    }

    // --- how the two attempts reconcile ------------------------------------------------------------

    /**
     * The case the safeguard exists for: the older build crashed, the newest one boots. The crash was an
     * artifact of the build, not the mod, so the newest attempt's verdict wins — and the note says why, so a
     * reader of the report is not left wondering.
     */
    @Test
    fun aCrashThatDisappearsOnTheNewestBuildYieldsTheNewestVerdict() {
        val reconciled = BootVerifier.reconcileRecheck(
            first = outcome(BootResult.CRASHED, "Forge 52.1.16 / Minecraft 1.21.1 → CRASHED", excerpt = "old crash"),
            second = outcome(BootResult.SURVIVED, "Forge 52.1.20 / Minecraft 1.21.1 → SURVIVED"),
            bootedVersion = "52.1.16",
            latestVersion = "52.1.20"
        )

        Assertions.assertEquals(BootResult.SURVIVED, reconciled.result, "the newest build's result is the honest one")
        Assertions.assertTrue(reconciled.detail.contains("52.1.16"), "the note must name the build that crashed: ${reconciled.detail}")
        Assertions.assertTrue(reconciled.detail.contains("52.1.20"), "and the build that did not: ${reconciled.detail}")
        Assertions.assertNull(reconciled.crashExcerpt, "no crash survived, so no excerpt should be reported")
    }

    /** Crashing on both builds means the mod crashes servers — keep the crash, with the newest evidence. */
    @Test
    fun aCrashConfirmedOnTheNewestBuildStandsWithTheNewestEvidence() {
        val reconciled = BootVerifier.reconcileRecheck(
            first = outcome(BootResult.CRASHED, "Forge 52.1.16 → CRASHED", excerpt = "old crash"),
            second = outcome(BootResult.CRASHED, "Forge 52.1.20 → CRASHED", excerpt = "newest crash"),
            bootedVersion = "52.1.16",
            latestVersion = "52.1.20"
        )

        Assertions.assertEquals(BootResult.CRASHED, reconciled.result)
        Assertions.assertEquals("newest crash", reconciled.crashExcerpt, "report the excerpt from the authoritative build")
        Assertions.assertTrue(reconciled.detail.contains("confirmed"), "the note must say it was confirmed: ${reconciled.detail}")
    }

    /**
     * An inconclusive re-check proves nothing either way, so the original crash must **not** be softened —
     * the conservative direction is to keep the decisive signal rather than lose it to a flaky second boot.
     */
    @Test
    fun anInconclusiveReCheckKeepsTheOriginalCrash() {
        val first = outcome(BootResult.CRASHED, "Forge 52.1.16 → CRASHED", excerpt = "old crash")
        val reconciled = BootVerifier.reconcileRecheck(
            first = first,
            second = outcome(BootResult.INCONCLUSIVE, "timed out"),
            bootedVersion = "52.1.16",
            latestVersion = "52.1.20"
        )

        Assertions.assertEquals(BootResult.CRASHED, reconciled.result, "an unusable re-check cannot clear a crash")
        Assertions.assertEquals("old crash", reconciled.crashExcerpt)
        Assertions.assertTrue(
            reconciled.detail.contains("could not be re-checked") || reconciled.detail.contains("inconclusive"),
            "the note must admit the re-check was unusable: ${reconciled.detail}"
        )
    }
}
