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

/**
 * Pins that a verdict a rule had a hand in **says so**, in a field and not only in prose.
 *
 * "How many verdicts did rule X decide?" is the only way to find a rule that is firing too broadly, and a
 * sentence in a human-readable detail string cannot answer it. An unauditable heuristic deciding what gets
 * published to everyone polling the fallback list is the recurring defect class in this engine.
 */
internal class BootVerifierRuleProvenanceTest {

    private fun preparedPack(dir: File) =
        BootVerifier.Prepared.Ready(File(dir, "pack"), File(dir, "boot.log"), "1.20.1", "NeoForge", "21.1.77")

    private fun ruleSet(vararg rules: ConsoleRule) = ConsoleRuleSet(rules.toList(), emptyList(), "test")

    @Test
    fun aRuleThatDecidedABootIsNamedOnTheOutcome(@TempDir dir: File) {
        val console = listOf("java.lang.RuntimeException: Attempted to load class x for invalid dist DEDICATED_SERVER")

        val outcome = BootVerifier.outcomeFor(
            RunResult.Completed(console, exitCode = 0, timedOut = false),
            File(dir, "boot.log"),
            "NeoForge 21.1.77 / Minecraft 1.20.1",
            ruleSet(ConsoleRule("fml-invalid-dist", "for invalid dist DEDICATED_SERVER", BootResult.CRASHED, "FML refused a client class"))
        )

        Assertions.assertEquals(BootResult.CRASHED, outcome.result)
        Assertions.assertEquals("fml-invalid-dist", outcome.firedRule, "the rule must be a field, not only prose")
        Assertions.assertTrue(outcome.detail.contains("fml-invalid-dist"), "and readable in the detail: ${outcome.detail}")
        Assertions.assertTrue(
            outcome.detail.contains("FML refused a client class"),
            "the rule's own note is why it was written; carry it: ${outcome.detail}"
        )
    }

    /** A boot no rule touched must read exactly as it did before rules existed. */
    @Test
    fun anOutcomeNoRuleTouchedIsUnchanged(@TempDir dir: File) {
        val console = listOf("something unrecognised")

        val withoutRules = BootVerifier.outcomeFor(
            RunResult.Completed(console, exitCode = 1, timedOut = false), File(dir, "a.log"), "Forge 47.2.0 / Minecraft 1.20.1"
        )
        val withIrrelevantRules = BootVerifier.outcomeFor(
            RunResult.Completed(console, exitCode = 1, timedOut = false), File(dir, "b.log"), "Forge 47.2.0 / Minecraft 1.20.1",
            ruleSet(ConsoleRule("never-matches", "zzzzzzzz", BootResult.CRASHED))
        )

        Assertions.assertEquals(withoutRules.detail, withIrrelevantRules.detail)
        Assertions.assertNull(withoutRules.firedRule)
        Assertions.assertNull(withIrrelevantRules.firedRule)
    }

    /** A verdict-less rule fails safe to INCONCLUSIVE and still names itself on the outcome. */
    @Test
    fun aVerdictLessRuleIsNamedAndFailsSafe(@TempDir dir: File) {
        val console = listOf("java.lang.NoClassDefFoundError: com/benbenlaw/core/screen/util/slot/FilterSlot")
        val run = RunResult.Completed(console, exitCode = 1, timedOut = false)

        val plain = BootVerifier.outcomeFor(run, File(dir, "a.log"), "Forge 47.2.0 / Minecraft 1.20.1")
        val annotated = BootVerifier.outcomeFor(
            run, File(dir, "b.log"), "Forge 47.2.0 / Minecraft 1.20.1",
            ruleSet(ConsoleRule("third-party-screen", "NoClassDefFoundError: .*/screen/", note = "another mod's screen class"))
        )

        Assertions.assertEquals(BootResult.CRASHED, plain.result, "the ladder alone crashes this")
        Assertions.assertEquals(BootResult.INCONCLUSIVE, annotated.result, "a verdict-less rule fails safe")
        Assertions.assertEquals("third-party-screen", annotated.firedRule)
    }
}
