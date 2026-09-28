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
 * Pins the fold that replaces `aggregateFor`: what one loader's evidence adds up to under the four-state
 * verdict, with the console deciding and the metadata only declaring.
 *
 * This is where the redesign becomes visible in the report. The old fold produced a `Confidence` from a
 * `BootResult` and two self-reports, and could not distinguish *"the grind was prevented"* from *"the grind
 * learned nothing"* — the conflation that published a host-wide outage as thousands of per-candidate
 * verdicts, overwriting decisive ones.
 */
internal class VerdictAggregationTest {

    private fun outcome(
        result: BootResult?,
        decidedBy: BootDecision? = null,
        firedRule: String? = null,
        stagingPrevented: Boolean = false
    ) = result?.let {
        BootVerifier.BootOutcome(
            result = it,
            logFile = null,
            detail = "",
            firedRule = firedRule,
            decidedBy = decidedBy,
            // `BootOutcome` carries the prevention *cause* since 2026-09-09; HOST is the reading every
            // prevented grind had before the causes were told apart, which is what these guards mean.
            prevention = if (stagingPrevented) PreventionCause.HOST else null
        )
    }

    /**
     * **The target case.** Both sources declare the server supported, and the boot then dies on a
     * client-only class. CONFIRMED, with the declaration recorded as the thing it contradicts.
     */
    @Test
    fun aServerDeclaringModThatCrashesOnAClientClassIsConfirmed() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.REQUIRED,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.CRASHED, BootDecision.CLIENT_ONLY_CLASS),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.CONFIRMED, assessment.verdict)
        Assertions.assertEquals(
            Declaration.SERVER, assessment.declared,
            "the claim has to be recorded — a contradicted server declaration IS the finding"
        )
    }

    /** A staging refusal is an operator problem, and must not read as a boot that learned nothing. */
    @Test
    fun aPreventedGrindIsAnError() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.INCONCLUSIVE, stagingPrevented = true),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.ERROR, assessment.verdict)
    }

    /** A clean boot is proof of server-safety and keeps its own verdict rather than collapsing into doubt. */
    @Test
    fun aCleanBootIsClear() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.SURVIVED, BootDecision.READY_LINE),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.CLEAR, assessment.verdict)
    }

    /**
     * A crash decided by the bare exit-code rung means only *"exited non-zero, nothing recognised why"*. It
     * is not evidence of sideness and must not confirm — the rung that produced 27 of 43 published HIGH
     * verdicts resting on no decisive evidence.
     */
    @Test
    fun aCrashNoRuleExplainedIsInconclusive() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.REQUIRED,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.CRASHED, BootDecision.EXIT_CODE),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.INCONCLUSIVE, assessment.verdict)
    }

    /**
     * **A client declaration is not a finding.** The mod says client-only, and nothing booted to check. It
     * stays INCONCLUSIVE: the declaration is recorded, but a self-report may not publish a mod.
     */
    @Test
    fun aClientDeclarationAloneIsInconclusive() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNSUPPORTED,
            clientSide = DeclaredSupport.REQUIRED,
            jarScan = JarScan.CLIENT,
            bootOutcome = null,
            bootAttempted = false
        )

        Assertions.assertEquals(Verdict.INCONCLUSIVE, assessment.verdict)
        Assertions.assertEquals(Declaration.CLIENT, assessment.declared, "the claim is still worth recording")
    }

    /**
     * A run that deliberately did not boot is not an ERROR — nothing was prevented, the caller simply asked
     * for metadata only (the `-clientsidereport` verb). ERROR is reserved for a grind that could not be
     * performed, or it stops meaning anything an operator can act on.
     */
    @Test
    fun notBootingOnPurposeIsNotAnError() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.REQUIRED,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = null,
            bootAttempted = false
        )

        Assertions.assertEquals(Verdict.INCONCLUSIVE, assessment.verdict)
    }

    /**
     * An operator rule that stated CRASHED is decisive by the author's own intent, so it confirms — the same
     * standing the built-in client-class marker has, which is why `BootDecision.decisive` names both.
     */
    @Test
    fun anOperatorRuleThatCrashedConfirmsAndNamesItself() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.CRASHED, BootDecision.OPERATOR_RULE, firedRule = "fml-invalid-dist"),
            bootAttempted = true
        )

        Assertions.assertEquals(Verdict.CONFIRMED, assessment.verdict)
        Assertions.assertEquals(
            "fml-invalid-dist", assessment.confirmedByRule,
            "every confirmation names the rule that produced it, or it cannot be audited or revoked"
        )
    }

    /** The built-in decisive marker names itself by its rule id, so the report reads the same either way. */
    @Test
    fun theBuiltInMarkerNamesItselfByItsRuleId() {
        val assessment = ClientsideVerifier.verdictOf(
            serverSide = DeclaredSupport.UNKNOWN,
            clientSide = DeclaredSupport.UNKNOWN,
            jarScan = JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome(BootResult.CRASHED, BootDecision.CLIENT_ONLY_CLASS),
            bootAttempted = true
        )

        Assertions.assertEquals("client-only-class", assessment.confirmedByRule)
    }
}
