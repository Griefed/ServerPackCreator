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
 * Pins the operator-editable rule file: how it loads, when it reloads, and — the half that matters most —
 * what it does with input it cannot make sense of.
 *
 * The whole point is that a newly-observed clientside signature becomes a file edit rather than a release,
 * so the failure modes have to be *safe* (never fail a boot, never invent a verdict) **and** *observable*
 * (a typo that silently disabled an operator's rules would defeat the feature).
 */
internal class ConsoleRuleFileTest {

    private fun rulesFile(dir: File, body: String) = File(dir, "boot-rules.json").apply { writeText(body) }

    @Test
    fun readsTheRulesItIsGiven(@TempDir dir: File) {
        val file = rulesFile(
            dir,
            """
            [
              { "id": "fml-invalid-dist", "pattern": "for invalid dist DEDICATED_SERVER",
                "verdict": "CRASHED", "note": "FML refused a client-only class on a dedicated server." }
            ]
            """.trimIndent()
        )

        val loaded = ConsoleRuleFile(file).current()

        Assertions.assertEquals(1, loaded.rules.size)
        Assertions.assertEquals("fml-invalid-dist", loaded.rules.single().id)
        Assertions.assertEquals(BootResult.CRASHED, loaded.rules.single().verdict)
        Assertions.assertTrue(loaded.errors.isEmpty())
    }

    /** The feature is "edit it while the daemon runs", so a changed file has to be picked up on the next read. */
    @Test
    fun anEditedRuleFileIsReloadedOnTheNextRead(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "first", "pattern": "aaa", "verdict": "CRASHED" }]""")
        val rules = ConsoleRuleFile(file)
        Assertions.assertEquals("first", rules.current().rules.single().id)

        file.writeText("""[{ "id": "second", "pattern": "bbbbbbbbbb", "verdict": "INCONCLUSIVE" }]""")

        Assertions.assertEquals("second", rules.current().rules.single().id, "an edit must be picked up without a restart")
    }

    /** `classify` runs once per boot, but re-parsing on every read would still be waste with no upside. */
    @Test
    fun anUnchangedFileIsNotReParsed(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "first", "pattern": "aaa" }]""")
        val rules = ConsoleRuleFile(file)

        val first = rules.current()
        val second = rules.current()

        Assertions.assertSame(first, second, "an unchanged file must hand back the parsed set it already has")
    }

    /**
     * "Keep the last good rules" is what stops a mid-edit save from silently disabling every rule an
     * operator wrote — but it hides the breakage unless the error is surfaced, which is what `errors` is for.
     */
    @Test
    fun aMalformedFileKeepsTheLastGoodRulesAndReportsTheError(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "good", "pattern": "aaa", "verdict": "CRASHED" }]""")
        val rules = ConsoleRuleFile(file)
        Assertions.assertEquals("good", rules.current().rules.single().id)

        file.writeText("{ this is not json at all")
        val afterBreakage = rules.current()

        Assertions.assertEquals("good", afterBreakage.rules.single().id, "the last good rules must survive a bad save")
        Assertions.assertTrue(afterBreakage.errors.isNotEmpty(), "and the breakage must be visible, not silent")
    }

    /** One bad regex costs its own rule. Failing the file would let a typo disable every other rule. */
    @Test
    fun anUncompilableRegexDropsOnlyItsOwnRule(@TempDir dir: File) {
        val file = rulesFile(
            dir,
            """
            [
              { "id": "fine-before", "pattern": "aaa", "verdict": "CRASHED" },
              { "id": "broken", "pattern": "[unclosed", "verdict": "CRASHED" },
              { "id": "fine-after", "pattern": "bbb", "verdict": "CRASHED" }
            ]
            """.trimIndent()
        )

        val loaded = ConsoleRuleFile(file).current()

        Assertions.assertEquals(listOf("fine-before", "fine-after"), loaded.rules.map { it.id })
        Assertions.assertTrue(loaded.errors.single().contains("broken"), "the error must name the rule: ${loaded.errors}")
    }

    /**
     * **The one that matters most.** Defaulting an unrecognised verdict to CRASHED would let a typo publish
     * a HIGH-confidence clientside entry, which is the single most expensive mistake this engine can make.
     */
    @Test
    fun anUnknownVerdictDropsTheRuleRatherThanDefaultingToCrashed(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "typo", "pattern": "aaa", "verdict": "CRASHDE" }]""")

        val loaded = ConsoleRuleFile(file).current()

        Assertions.assertTrue(loaded.rules.isEmpty(), "a rule whose verdict cannot be read must not fire at all")
        Assertions.assertTrue(loaded.errors.single().contains("typo"))
    }

    /** No file is the normal case, and it must mean "behave exactly as before rules existed". */
    @Test
    fun anAbsentFileIsAnEmptyRuleSetAndNotAnError(@TempDir dir: File) {
        val loaded = ConsoleRuleFile(File(dir, "never-written.json")).current()

        Assertions.assertTrue(loaded.rules.isEmpty())
        Assertions.assertTrue(loaded.errors.isEmpty(), "an absent file is not a failure")
    }

    /** A rule kept in the file but switched off must not fire — the alternative is deleting and retyping it. */
    @Test
    fun aDisabledRuleIsNotLoaded(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "off", "pattern": "aaa", "verdict": "CRASHED", "enabled": false }]""")

        Assertions.assertTrue(ConsoleRuleFile(file).current().rules.isEmpty())
    }

    /** A rule without a verdict is legal: it identifies without deciding, leaving the ladder to rule. */
    @Test
    fun aRuleMayCarryNoVerdictAtAll(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "id": "just-a-note", "pattern": "aaa", "note": "seen this before" }]""")

        val rule = ConsoleRuleFile(file).current().rules.single()

        Assertions.assertNull(rule.verdict)
        Assertions.assertEquals("seen this before", rule.note)
    }

    /** A rule with no id cannot be reported on, and an unreportable rule is worse than an absent one. */
    @Test
    fun aRuleWithoutAnIdOrAPatternIsDropped(@TempDir dir: File) {
        val file = rulesFile(dir, """[{ "pattern": "aaa" }, { "id": "no-pattern" }]""")

        val loaded = ConsoleRuleFile(file).current()

        Assertions.assertTrue(loaded.rules.isEmpty())
        Assertions.assertEquals(2, loaded.errors.size, "both omissions must be reported: ${loaded.errors}")
    }
}
