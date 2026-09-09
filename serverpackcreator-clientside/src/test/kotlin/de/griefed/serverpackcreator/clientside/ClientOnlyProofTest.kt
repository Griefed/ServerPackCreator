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
 * Pins that **proof a mod reaches client-only code is about the mod, not about the build that proved it** —
 * so no other version and no other loader may clear it, and every loader inherits it.
 *
 * `sodium` is the reported case, and it is as clear-cut as this engine gets: Modrinth declares
 * `client_side: required, server_side: unsupported`, and the mod is a client renderer. Its NeoForge
 * 26.2.0.76 boot crashed reaching LWJGL — decisive evidence no harness can fabricate. It was then thrown
 * away, because the other-version re-check sampled `sodium-fabric-0.9.2-beta.1+mc26.1.2.jar`, that booted
 * cleanly, and `reconcileOtherVersionRecheck` replaces the crash outright with any survivor. Final verdict:
 * INCONCLUSIVE, for a mod nobody disputes is client-only.
 *
 * **Why a clean boot elsewhere is not a counter-argument here.** The re-checks exist to tell *"this build
 * crashed"* apart from *"this mod cannot run on a server"* — a real distinction, and the reason `iron-chests`
 * stopped publishing off one bad build. But client-only evidence already answers that question: the server
 * loaded the mod and the mod reached for the client. Another build starting without crashing does not
 * disprove it; a client mod can start a server without being any use on one, which is the asymmetry this
 * module has documented since the boot-test existed.
 *
 * **And it crosses loaders** (Griefed's call): a mod's *features* are the same on Fabric and NeoForge, only
 * the implementation differs. If one loader's build reaches client-only code, the mod is client-only, and
 * every loader's entry is exclusion-worthy.
 *
 * Deliberately **not** propagated from [BootDecision.OPERATOR_RULE], though it is `decisive`: an operator's
 * rule reaching CRASHED is a statement that *this console* means a crash, which is not necessarily a
 * statement about sideness. Only the three rungs that are client-only evidence by construction propagate.
 */
internal class ClientOnlyProofTest {

    private fun crash(decision: BootDecision) =
        BootVerifier.BootOutcome(BootResult.CRASHED, null, "NeoForge 26.2.0.76 / Minecraft 26.2 → CRASHED", decidedBy = decision)

    private fun verdict(loader: String, entry: String, result: BootResult?, decision: BootDecision? = null) =
        LoaderVerdict(
            loader = loader, suggestedEntry = entry,
            declaredClientSide = DeclaredSupport.REQUIRED, declaredServerSide = DeclaredSupport.UNSUPPORTED,
            jarScan = JarScan.SERVER_OR_BOTH, bootResult = result, bootedLoader = loader,
            bootCrashExcerpt = null, sampleFile = null, note = null, decidedBy = decision,
            verdict = if (decision?.provesClientOnly == true) Verdict.CONFIRMED else Verdict.INCONCLUSIVE
        )

    /** Exactly the three rungs that are client-only evidence by construction. */
    @Test
    fun onlyClientOnlyEvidenceProvesSideness() {
        Assertions.assertEquals(
            setOf(
                BootDecision.CLIENT_ONLY_CLASS,
                BootDecision.LWJGL_ON_A_DEDICATED_SERVER,
                BootDecision.FML_INVALID_DIST
            ),
            BootDecision.entries.filter { it.provesClientOnly }.toSet()
        )
        Assertions.assertFalse(
            BootDecision.OPERATOR_RULE.provesClientOnly,
            "an operator rule says this console is a crash, not necessarily that the mod is client-only"
        )
        Assertions.assertFalse(BootDecision.EXIT_CODE.provesClientOnly)
    }

    /** Spending boots to re-check a crash that already proved sideness buys nothing. */
    @Test
    fun aClientOnlyProvenCrashIsNotReChecked() {
        Assertions.assertFalse(
            BootVerifier.shouldRecheckAgainstOtherVersions(
                crash(BootDecision.LWJGL_ON_A_DEDICATED_SERVER), metadataDeclaresServerSupport = true, limit = 2
            ),
            "the question the re-check answers is already answered"
        )
        Assertions.assertTrue(
            BootVerifier.shouldRecheckAgainstOtherVersions(
                crash(BootDecision.EXIT_CODE), metadataDeclaresServerSupport = true, limit = 2
            ),
            "a crash nobody explained is still worth re-checking"
        )
    }

    /** The sodium case: a survivor in the sample must not replace client-only proof. */
    @Test
    fun aSurvivingOtherVersionDoesNotClearClientOnlyProof() {
        val proven = crash(BootDecision.LWJGL_ON_A_DEDICATED_SERVER)
        val survivor = BootVerifier.OtherVersionAttempt(
            "sodium-fabric-0.9.2-beta.1+mc26.1.2.jar (Fabric, Minecraft 26.1.2)",
            BootVerifier.BootOutcome(BootResult.SURVIVED, null, "SURVIVED")
        )

        val reconciled = BootVerifier.reconcileOtherVersionRecheck(proven, listOf(survivor))

        Assertions.assertEquals(
            BootResult.CRASHED, reconciled.result,
            "another build starting is not evidence the mod is server-capable"
        )
    }

    /** Nor may another loader's clean boot supersede it. */
    @Test
    fun anotherLoadersCleanBootDoesNotSupersedeClientOnlyProof() {
        val proven = verdict("NeoForge", "sodium-", BootResult.CRASHED, BootDecision.LWJGL_ON_A_DEDICATED_SERVER)
        val survived = verdict("Fabric", "sodium-", BootResult.SURVIVED)

        Assertions.assertNull(
            ClientsideVerifier.loaderDisprovingTheCrash(proven, listOf(proven, survived)),
            "a clean boot cannot disprove the mod having reached the client"
        )
    }

    /** A crash *without* client-only proof is still disprovable — the `iron-chests` guard must survive. */
    @Test
    fun anUnexplainedCrashIsStillDisprovedByAnotherLoader() {
        val unexplained = verdict("Forge", "ironchest-", BootResult.CRASHED, BootDecision.EXIT_CODE)
        val survived = verdict("NeoForge", "ironchest-", BootResult.SURVIVED)

        Assertions.assertEquals(
            "NeoForge",
            ClientsideVerifier.loaderDisprovingTheCrash(unexplained, listOf(unexplained, survived))?.loader,
            "one build's crash must still not condemn a mod another loader boots cleanly"
        )
    }

    /**
     * **The propagation.** Features are the same across loaders, so one loader's proof excludes them all —
     * including a loader that booted cleanly, whose own entry would otherwise stay publishable.
     */
    @Test
    fun oneLoadersProofConfirmsEveryLoader() {
        val neoforge = verdict("NeoForge", "sodium-neoforge-", BootResult.CRASHED, BootDecision.LWJGL_ON_A_DEDICATED_SERVER)
        val fabric = verdict("Fabric", "sodium-fabric-", BootResult.SURVIVED)

        val propagated = ClientsideVerifier.propagateClientOnlyProof(listOf(neoforge, fabric))

        Assertions.assertEquals(
            listOf(Verdict.CONFIRMED, Verdict.CONFIRMED), propagated.map { it.verdict },
            "a mod's features do not change with the loader, so both entries are exclusion-worthy"
        )
        Assertions.assertTrue(
            propagated.single { it.loader == "Fabric" }.note.orEmpty().contains("NeoForge"),
            "the inheriting loader must say which one proved it, or the verdict cannot be audited"
        )
    }

    /** Its own boot result is left untouched — the report must not claim Fabric crashed when it did not. */
    @Test
    fun propagationDoesNotRewriteWhatEachLoaderActuallyDid() {
        val neoforge = verdict("NeoForge", "sodium-neoforge-", BootResult.CRASHED, BootDecision.LWJGL_ON_A_DEDICATED_SERVER)
        val fabric = verdict("Fabric", "sodium-fabric-", BootResult.SURVIVED)

        val propagated = ClientsideVerifier.propagateClientOnlyProof(listOf(neoforge, fabric))

        Assertions.assertEquals(
            BootResult.SURVIVED, propagated.single { it.loader == "Fabric" }.bootResult,
            "the boot really did survive; only its standing changes"
        )
    }

    /** With no proof anywhere, nothing is propagated and the existing reconciliation is untouched. */
    @Test
    fun withoutProofNothingIsPropagated() {
        val a = verdict("Fabric", "mod-", BootResult.SURVIVED)
        val b = verdict("NeoForge", "mod-", BootResult.CRASHED, BootDecision.EXIT_CODE)

        Assertions.assertEquals(
            listOf(Verdict.INCONCLUSIVE, Verdict.INCONCLUSIVE),
            ClientsideVerifier.propagateClientOnlyProof(listOf(a, b)).map { it.verdict }
        )
    }
}
