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
 * Pins **which rung decided** a boot, and which rungs count as decisive evidence of client-only-ness.
 *
 * Why this has to exist at all: `CRASHED` is reachable two ways — the client-only-class marker, which is
 * decisive on the console alone, and the bare exit-code fallback, which means only "the process exited
 * non-zero and nothing recognised why". Both produced an identical `HIGH`, so the published fallback list
 * could not tell a mod reaching for `net/minecraft/client` from one whose mixins failed to apply. Measured
 * against five real boot logs on 2026-08-31, four were the latter and one of them —
 * `created_ltab` — was already published.
 */
internal class BootDecisionTest {

    private fun rules(vararg rules: ConsoleRule) = ConsoleRuleSet(rules.toList(), emptyList(), "test")

    @Test
    fun everyRungNamesItselfAsTheDecision() {
        val cases = listOf(
            Triple(listOf("[Server thread/INFO]: Done (4.2s)! For help"), 0, BootDecision.READY_LINE),
            Triple(listOf("Fabric is not available for Minecraft 26.2"), 1, BootDecision.SETUP_ABORT),
            Triple(listOf("Error: Unable to access jarfile forge.jar"), 1, BootDecision.LAUNCH_FAILURE),
            Triple(listOf("Could not find parent layer for module java.base"), 1, BootDecision.LOADER_BOOTSTRAP_FAILURE),
            Triple(listOf("java.lang.OutOfMemoryError: Java heap space"), 1, BootDecision.KILLED_OR_OOM),
            Triple(listOf("java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft"), 1, BootDecision.CLIENT_ONLY_CLASS),
            Triple(listOf("Missing or unsupported mandatory dependencies"), 1, BootDecision.DEPENDENCY_FAILURE),
            Triple(listOf("java.net.UnknownHostException: api.polyfrost.org"), 1, BootDecision.SANDBOX_NETWORK),
            Triple(listOf("something nobody recognises"), 1, BootDecision.EXIT_CODE)
        )

        for ((console, exit, expected) in cases) {
            Assertions.assertEquals(
                expected, BootLogClassifier.classify(console, exit, timedOut = false, ConsoleRuleSet.EMPTY).decidedBy,
                "wrong decision reported for $console"
            )
        }
        Assertions.assertEquals(
            BootDecision.TIMED_OUT,
            BootLogClassifier.classify(listOf("nothing"), null, timedOut = true, ConsoleRuleSet.EMPTY).decidedBy
        )
    }

    @Test
    fun anOperatorRuleNamesItselfAsTheDecision() {
        val decided = BootLogClassifier.classify(
            listOf("for invalid dist DEDICATED_SERVER"), exitCode = 0, timedOut = false,
            rules = rules(ConsoleRule("fml-invalid-dist", "for invalid dist DEDICATED_SERVER", BootResult.CRASHED))
        )

        Assertions.assertEquals(BootDecision.OPERATOR_RULE, decided.decidedBy)
        Assertions.assertEquals(BootResult.CRASHED, decided.result)
    }

    /**
     * An *undecided* rule that lets the ladder settle the verdict must not claim the decision — the ladder
     * made it. Otherwise a note-only rule would launder a bare exit-code crash into decisive evidence, which
     * is exactly the laundering this whole type exists to prevent.
     */
    @Test
    fun anUndecidedRuleRidingAlongDoesNotClaimTheDecision() {
        val decided = BootLogClassifier.classify(
            listOf("something nobody recognises"), exitCode = 1, timedOut = false,
            rules = rules(ConsoleRule("just-a-note", "nobody recognises"))
        )

        Assertions.assertEquals(BootResult.CRASHED, decided.result)
        Assertions.assertEquals(BootDecision.EXIT_CODE, decided.decidedBy, "the ladder decided, not the rule")
        Assertions.assertEquals("just-a-note", decided.firedRule?.rule?.id, "but the rule still rides along")
        Assertions.assertFalse(decided.decidedBy.decisive)
    }

    /**
     * **The set is deliberately tiny, and this test is the whole point of the type.** Only two rungs may
     * publish a clientside entry: the client-only-class marker, which no environment failure can fabricate,
     * and an operator rule — because a rule reaching CRASHED stated CRASHED deliberately (an undecided rule
     * resolves to the ladder or to INCONCLUSIVE, never to CRASHED).
     */
    @Test
    fun theDecisiveSetIsSmallAndExplicit() {
        Assertions.assertEquals(
            setOf(
                BootDecision.CLIENT_ONLY_CLASS,
                // Both added 2026-09-04, when `iris` scored INCONCLUSIVE on
                // `NoClassDefFoundError: org/lwjgl/Version`. A dedicated server ships no LWJGL, and FML
                // saying "invalid dist DEDICATED_SERVER" is the loader itself refusing a client-only
                // class: neither can be fabricated by a broken harness, which is the bar for this set.
                BootDecision.LWJGL_ON_A_DEDICATED_SERVER,
                BootDecision.FML_INVALID_DIST,
                BootDecision.OPERATOR_RULE
            ),
            BootDecision.entries.filter { it.decisive }.toSet(),
            "this set is what may publish a mod; every addition needs a reason a harness cannot fake"
        )
        Assertions.assertFalse(BootDecision.EXIT_CODE.decisive, "a non-zero exit nobody recognised is not evidence")
    }
}
