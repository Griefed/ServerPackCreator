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
import java.io.File

/**
 * Pins that a confirmation names the rule that **decided** it, not one that merely matched alongside.
 *
 * `Classification.firedRule` is documented as "the rule that decided **or annotated**" the result — an
 * operator rule stating no verdict still rides along on whatever the ladder settles, deliberately, so the
 * author can see their pattern matched without it changing anything. `verdictOf` then read
 * `firedRule ?: decidedBy?.ruleId` as the confirming rule, which attributes the confirmation to a rule that
 * explicitly declined to state one.
 *
 * What that costs is auditability, which is this engine's stated standard: *a verdict that cannot name its
 * own evidence cannot be audited*. The report's Rule column, and an operator asking "which rule excluded
 * this mod?", are sent to a rule that did not. The verdict itself was never wrong — CONFIRMED is gated on
 * `BootDecision.decisive`, and an annotating rule cannot change the rung — so this is a diagnosis defect,
 * the same family as an install "not retried" that had just been attempted.
 */
internal class ConfirmationAttributionTest {

    private fun outcome(console: List<String>, rules: ConsoleRuleSet) =
        BootVerifier.outcomeFor(
            RunResult.Completed(console, 1, false),
            logFile = File.createTempFile("attribution", ".log").apply { deleteOnExit() },
            label = "Forge 47.2.0 / Minecraft 1.20.1",
            rules = rules
        )

    private fun assessmentFor(outcome: BootVerifier.BootOutcome) =
        ClientsideVerifier.verdictOf(
            DeclaredSupport.UNKNOWN, DeclaredSupport.UNKNOWN, JarScan.SERVER_OR_BOTH,
            bootOutcome = outcome, bootAttempted = true
        )

    /** A rule that states no verdict annotates the result; it must not be named as the confirming rule. */
    @Test
    fun anAnnotatingRuleIsNotCreditedWithTheConfirmation() {
        val annotating = ConsoleRuleSet(
            listOf(ConsoleRule("operator-note", "Preparing spawn area", null)), emptyList(), "test"
        )
        val assessed = assessmentFor(
            outcome(
                listOf(
                    "[Server thread/INFO]: Preparing spawn area",
                    "java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"
                ),
                annotating
            )
        )

        Assertions.assertEquals(Verdict.CONFIRMED, assessed.verdict, "the decisive rung still confirms")
        Assertions.assertEquals(
            BootDecision.CLIENT_ONLY_CLASS.ruleId, assessed.confirmedByRule,
            "the client-only marker decided this, not the operator's non-deciding rule"
        )
    }

    /** A rule that *does* decide is credited, because then it really is the evidence. */
    @Test
    fun aDecidingRuleIsCreditedWithTheConfirmation() {
        val deciding = ConsoleRuleSet(
            listOf(ConsoleRule("operator-crash", "custom clientside signature", BootResult.CRASHED)),
            emptyList(), "test"
        )
        val assessed = assessmentFor(outcome(listOf("custom clientside signature"), deciding))

        Assertions.assertEquals(Verdict.CONFIRMED, assessed.verdict)
        Assertions.assertEquals("operator-crash", assessed.confirmedByRule)
    }

    /** With no rule at all, the rung names itself, exactly as before. */
    @Test
    fun withoutARuleTheRungNamesItself() {
        val assessed = assessmentFor(
            outcome(listOf("java.lang.NoClassDefFoundError: org/lwjgl/Version"), ConsoleRuleSet.EMPTY)
        )

        Assertions.assertEquals(
            BootDecision.LWJGL_ON_A_DEDICATED_SERVER.ruleId, assessed.confirmedByRule
        )
    }
}
