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
 * Pins both crash re-checks: the **loader-build** one, which makes it safe for a caller to boot an *older*
 * loader build than the newest one (which the grinder does, to reuse its ~150 MB install cache instead of
 * re-installing every time a loader ships a build), and the **other-version** one, which asks whether a
 * crash belongs to one build of the mod rather than to the mod.
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

    // --- the other-version re-check: when it is worth spending boots --------------------------------

    private fun attempt(label: String, result: BootResult, detail: String = result.name, excerpt: String? = null) =
        BootVerifier.OtherVersionAttempt(label, outcome(result, detail, excerpt))

    /**
     * The false positive this exists for, reported live on 2026-08-23: `iron-chests` — a mod that is
     * unarguably server-safe — came out `HIGH` on `Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)`, with
     * the note "Declared server/both but the server crashed". Exactly one build of the mod was ever booted,
     * so "this build crashes" and "this mod is clientside" were indistinguishable. When the metadata says
     * server and the boot says crash, one of the two is wrong — and another version of the mod is the cheapest
     * evidence available for deciding which.
     */
    @Test
    fun aCrashContradictingDeclaredServerSupportIsWorthCheckingOtherVersions() {
        Assertions.assertTrue(
            BootVerifier.shouldRecheckAgainstOtherVersions(
                outcome(BootResult.CRASHED), metadataDeclaresServerSupport = true, limit = 2
            )
        )
    }

    /**
     * Nothing contradicts anything when the metadata already leans clientside: the crash *confirms* what the
     * jar and the platform say, and every extra boot spent there is a boot not spent on the catalog. Left
     * deliberately narrow for that reason — the true positives are the common case in a sweep.
     */
    @Test
    fun aCrashThatTheMetadataAgreesWithIsNotReCheckedElsewhere() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckAgainstOtherVersions(
                outcome(BootResult.CRASHED), metadataDeclaresServerSupport = false, limit = 2
            )
        )
    }

    /** Only a crash is decisive, so only a crash can be worth disproving; and a zero budget buys no boots. */
    @Test
    fun neitherANonCrashNorAZeroBudgetTriggersOtherVersionBoots() {
        listOf(BootResult.SURVIVED, BootResult.INCONCLUSIVE).forEach { result ->
            Assertions.assertFalse(
                BootVerifier.shouldRecheckAgainstOtherVersions(outcome(result), metadataDeclaresServerSupport = true, limit = 2),
                "$result must not trigger a re-boot"
            )
        }
        Assertions.assertFalse(
            BootVerifier.shouldRecheckAgainstOtherVersions(
                outcome(BootResult.CRASHED), metadataDeclaresServerSupport = true, limit = 0
            )
        )
    }

    // --- the other-version re-check: how the attempts reconcile -------------------------------------

    /**
     * One other version booting a server clean is proof the mod is not clientside — a mod that cannot run
     * server-side cannot run server-side in *any* build. So the crash was that build's, and the surviving
     * attempt becomes the verdict, carrying both halves of the story in its detail.
     */
    @Test
    fun aVersionThatBootsCleanClearsTheCrash() {
        val reconciled = BootVerifier.reconcileOtherVersionRecheck(
            first = outcome(BootResult.CRASHED, "Forge 48.1.0 / Minecraft 1.20.2 → CRASHED (exit 1)", excerpt = "crash"),
            attempts = listOf(
                attempt("ironchest-1.20.1.jar (Minecraft 1.20.1)", BootResult.SURVIVED, "Forge 47.3.0 / Minecraft 1.20.1 → SURVIVED")
            )
        )

        Assertions.assertEquals(BootResult.SURVIVED, reconciled.result, "another version booting proves the mod runs on a server")
        Assertions.assertNull(reconciled.crashExcerpt, "no crash stands, so no excerpt may be reported")
        Assertions.assertTrue(reconciled.detail.contains("1.20.2"), "the note must name the version that crashed: ${reconciled.detail}")
        Assertions.assertTrue(reconciled.detail.contains("1.20.1"), "and the version that did not: ${reconciled.detail}")
    }

    /** A survivor decides even when a crashing version was tried first — one clean boot is enough. */
    @Test
    fun aSurvivorOutweighsAnotherVersionThatAlsoCrashed() {
        val reconciled = BootVerifier.reconcileOtherVersionRecheck(
            first = outcome(BootResult.CRASHED, "1.20.2 → CRASHED", excerpt = "crash"),
            attempts = listOf(
                attempt("mod-1.20.1.jar (Minecraft 1.20.1)", BootResult.CRASHED),
                attempt("mod-1.19.2.jar (Minecraft 1.19.2)", BootResult.SURVIVED)
            )
        )

        Assertions.assertEquals(BootResult.SURVIVED, reconciled.result)
    }

    /** Crashing everywhere is the mod, not a build — keep the crash, and record that it was corroborated. */
    @Test
    fun aCrashOnEveryVersionTriedStandsAndNamesThem() {
        val first = outcome(BootResult.CRASHED, "1.20.2 → CRASHED", excerpt = "crash")
        val reconciled = BootVerifier.reconcileOtherVersionRecheck(
            first = first,
            attempts = listOf(
                attempt("mod-1.20.1.jar (Minecraft 1.20.1)", BootResult.CRASHED),
                attempt("mod-1.19.2.jar (Minecraft 1.19.2)", BootResult.CRASHED)
            )
        )

        Assertions.assertEquals(BootResult.CRASHED, reconciled.result)
        Assertions.assertEquals("crash", reconciled.crashExcerpt, "the original crash is still the reported evidence")
        Assertions.assertTrue(reconciled.detail.contains("1.20.1"), "the note must name the corroborating versions: ${reconciled.detail}")
        Assertions.assertTrue(reconciled.detail.contains("1.19.2"), "both of them: ${reconciled.detail}")
    }

    /**
     * Same conservative direction as the loader-build re-check: an attempt that learned nothing — it could not
     * be staged, it timed out, the host was busy — must never soften a crash. A crash we cannot disprove stays
     * a crash, and the note has to admit that is what happened.
     */
    @Test
    fun attemptsThatLearnedNothingLeaveTheCrashStanding() {
        val first = outcome(BootResult.CRASHED, "1.20.2 → CRASHED", excerpt = "crash")
        val reconciled = BootVerifier.reconcileOtherVersionRecheck(
            first = first,
            attempts = listOf(
                attempt("mod-1.20.1.jar (Minecraft 1.20.1)", BootResult.INCONCLUSIVE, "timed out"),
                attempt("mod-1.19.2.jar (Minecraft 1.19.2)", BootResult.INCONCLUSIVE, "Could not download mod-1.19.2.jar.")
            )
        )

        Assertions.assertEquals(BootResult.CRASHED, reconciled.result, "an unusable re-check cannot clear a crash")
        Assertions.assertEquals("crash", reconciled.crashExcerpt)
        Assertions.assertTrue(
            reconciled.detail.contains("could not be re-checked"),
            "the note must admit the re-checks were unusable: ${reconciled.detail}"
        )
        Assertions.assertTrue(reconciled.detail.contains("timed out"), "and say why: ${reconciled.detail}")
    }

    /**
     * A mod published for a single Minecraft version has nothing to be re-checked against. The crash stands —
     * but the report must say the sample was one build, not imply a second opinion was taken.
     */
    @Test
    fun havingNoOtherVersionToTryIsSaidOutLoud() {
        val first = outcome(BootResult.CRASHED, "1.20.2 → CRASHED", excerpt = "crash")
        val reconciled = BootVerifier.reconcileOtherVersionRecheck(first = first, attempts = emptyList())

        Assertions.assertEquals(BootResult.CRASHED, reconciled.result)
        Assertions.assertTrue(
            reconciled.detail.contains("no other version"),
            "the note must say the mod had no other version to try: ${reconciled.detail}"
        )
    }
}
