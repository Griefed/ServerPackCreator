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
 * Pins the one predicate that decides whether a mod *claims* to support servers, from the platform's
 * self-report and the jar's own sideness spec together.
 *
 * It is shared on purpose. The same answer both drives the "Declared server/both but the server crashed"
 * note and arms the other-version crash re-check, and the two must never disagree about what "declared
 * server" means — a report that prints the contradiction while the re-check silently decided there was none
 * is worse than either behaviour on its own.
 */
internal class ClientsideVerifierServerSupportTest {

    /** Modrinth is the only platform with the field, and `required` is the unambiguous claim. */
    @Test
    fun aPlatformRequiringTheServerSideDeclaresServerSupport() {
        Assertions.assertTrue(ClientsideVerifier.declaresServerSupport(DeclaredSupport.REQUIRED, JarScan.ERROR))
    }

    /**
     * The `iron-chests` shape, and the only signal CurseForge can ever produce: the platform has no sideness
     * field at all, so [DeclaredSupport.UNKNOWN] is all it says, and the claim comes from SPC's own scan of
     * the jar. Miss this and the re-check never arms for a CurseForge mod — which is every mod that produced
     * the false positive this guard exists for.
     */
    @Test
    fun aJarDeclaringServerOrBothDeclaresServerSupportEvenWhenThePlatformSaysNothing() {
        Assertions.assertTrue(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH))
    }

    /** Nothing contradicts a crash here: both signals lean clientside, or say nothing at all. */
    @Test
    fun clientLeaningAndSilentSignalsDeclareNoServerSupport() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNSUPPORTED, JarScan.CLIENT))
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.CLIENT))
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.ERROR))
    }

    /**
     * A distribution-locked file was never scanned, so it declared nothing — "we could not look" must not be
     * read as "it said server", the same distinction the rest of the engine is built on.
     */
    @Test
    fun aDeferredJarScanDeclaresNothing() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.UNKNOWN, JarScan.DEFERRED))
    }

    /**
     * `optional` deliberately does not count, mirroring the confidence aggregation exactly: it means the mod
     * runs with or without the side, which is not a claim that the server works.
     */
    @Test
    fun anOptionalServerSideIsNotAClaimOfServerSupport() {
        Assertions.assertFalse(ClientsideVerifier.declaresServerSupport(DeclaredSupport.OPTIONAL, JarScan.ERROR))
    }
}

/**
 * Pins the two INCONCLUSIVE populations the live store surfaced on 2026-09-01, both of which threw away
 * something the engine had actually learned.
 */
internal class SurvivedBootConfidenceTest {

    /**
     * A boot that reached its ready-line proves the server started, and that must not read as "we learned
     * nothing". Under the old scale this was the difference between `LOW` and `INCONCLUSIVE`; it is now the
     * difference between [Verdict.CLEAR] and [Verdict.INCONCLUSIVE], and it matters for the same reason.
     *
     * Live examples, all `JarSideness = ERROR`, all `SURVIVED (exit 137)`: `better-stats`, `tcdcommons` and
     * `yacl`, each once recorded INCONCLUSIVE while the boot had in fact succeeded.
     */
    @Test
    fun aSurvivedBootIsClearRatherThanInconclusiveWhenTheMetadataSaysNothing() {
        val assessed = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.ERROR,
            bootOutcome = BootVerifier.BootOutcome(BootResult.SURVIVED, null, "", decidedBy = BootDecision.READY_LINE),
            bootAttempted = true
        )

        Assertions.assertEquals(
            Verdict.CLEAR, assessed.verdict,
            "a server that started is what CLEAR means; INCONCLUSIVE claims we learned nothing"
        )
    }

    /** With no boot and no metadata there genuinely is nothing, and that must stay INCONCLUSIVE. */
    @Test
    fun noBootAndNoMetadataIsStillInconclusive() {
        Assertions.assertEquals(
            Verdict.INCONCLUSIVE,
            ClientsideVerifier.verdictOf(
                DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, JarScan.ERROR,
                bootOutcome = null, bootAttempted = false
            ).verdict
        )
    }

    /**
     * **The premise this test used to hold has been deliberately reversed, and that is worth stating rather
     * than deleting quietly.**
     *
     * It asserted that a clean boot must not overturn a client-only declaration — under the old model,
     * `metadataClient` outranked a survived boot and the row stayed MEDIUM. The redesign inverts the
     * precedence: *the console decides and the metadata only declares*, because a self-report is the
     * unreliable half and is the entire reason a container is paid for. So a mod claiming client-only that
     * boots a server cleanly is now CLEAR, and its claim is recorded beside the verdict rather than
     * overriding it.
     *
     * The claim is not discarded — `Declaration.CLIENT` is still carried — and the case is pinned from the
     * precedence side in `ConsoleOutranksMetadataTest`.
     */
    @Test
    fun aClientOnlyDeclarationNoLongerOutranksACleanBoot() {
        val assessed = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNSUPPORTED,
            clientSide = DeclaredSupport.REQUIRED,
            jarScan = JarScan.CLIENT,
            bootOutcome = BootVerifier.BootOutcome(BootResult.SURVIVED, null, "", decidedBy = BootDecision.READY_LINE),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.CLEAR, assessed.verdict, "the boot is the evidence, the claim is not")
        Assertions.assertEquals(Declaration.CLIENT, assessed.declared, "and the claim is still recorded")
    }

    /**
     * A crash only outranks things when a *decisive* rung explained it. A bare non-zero exit is the rung
     * that filled the store with unevidenced findings, so it stays INCONCLUSIVE however the mod is declared.
     */
    @Test
    fun aCrashOutranksTheMetadataOnlyWhenARuleExplainedIt() {
        fun crashDecidedBy(decision: BootDecision) = ClientsideVerifier.verdictOf(
            DeclaredSupport.REQUIRED, DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH,
            BootVerifier.BootOutcome(BootResult.CRASHED, null, "", decidedBy = decision), bootAttempted = true
        ).verdict

        Assertions.assertEquals(Verdict.CONFIRMED, crashDecidedBy(BootDecision.CLIENT_ONLY_CLASS))
        Assertions.assertEquals(Verdict.INCONCLUSIVE, crashDecidedBy(BootDecision.EXIT_CODE))
    }

    /** An inconclusive boot taught us nothing, so it must not be promoted the way a survived one is. */
    @Test
    fun anInconclusiveBootIsNotEvidence() {
        Assertions.assertEquals(
            Verdict.INCONCLUSIVE,
            ClientsideVerifier.verdictOf(
                DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, JarScan.ERROR,
                BootVerifier.BootOutcome(BootResult.INCONCLUSIVE, null, ""), bootAttempted = true
            ).verdict
        )
    }
}
