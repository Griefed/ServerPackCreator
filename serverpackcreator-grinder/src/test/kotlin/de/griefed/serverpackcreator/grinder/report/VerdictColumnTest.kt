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
package de.griefed.serverpackcreator.grinder.report

import de.griefed.serverpackcreator.clientside.BootArtifacts
import de.griefed.serverpackcreator.clientside.BootResult
import de.griefed.serverpackcreator.clientside.Declaration
import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins what an operator actually reads: the report table and the CSV speak the four verdicts, and they show
 * the claim each verdict confirms or contradicts.
 *
 * The two columns are useless apart. A `CONFIRMED` row says a console proved the mod reaches client-only
 * code; the `Declared` column beside it says whether the mod had *claimed the server*, which is the
 * difference between "an honestly-labelled client mod" and "a mod coded unclean" — the second being the only
 * one worth anyone's attention, and the reason the boot is paid for at all.
 */
internal class VerdictColumnTest {

    private fun row(slug: String, verdict: Verdict, declared: Declaration? = null) =
        grindVerdict(slug, "Forge", verdict = verdict).copy(declared = declared)

    /** The CSV header names the verdict and the declaration, so a spreadsheet reader sees both. */
    @Test
    fun theCsvNamesTheVerdictAndTheDeclaration() {
        val header = VerdictCsvExporter.toCsv(emptyList()).lineSequence().first()

        Assertions.assertTrue(header.contains("Verdict"), header)
        Assertions.assertTrue(header.contains("Declared"), header)
        Assertions.assertFalse(header.contains("Confidence"), "the old scale is gone: $header")
    }

    /** A row renders the verdict it holds, not a translation of some other scale. */
    @Test
    fun aRowRendersItsOwnVerdict() {
        val csv = VerdictCsvExporter.toCsv(listOf(row("modelfix", Verdict.CONFIRMED, Declaration.SERVER)))

        Assertions.assertTrue(csv.contains("CONFIRMED"), csv)
        Assertions.assertTrue(csv.contains("SERVER"), "the contradicted claim is the finding: $csv")
    }

    /**
     * **The default order leads with what a maintainer came for.** CONFIRMED first — those are the findings.
     * INCONCLUSIVE next, because those consoles are the raw material the next rule is written from. ERROR
     * after, an operator problem rather than a mod one. CLEAR last: nothing to do.
     */
    @Test
    fun theDefaultOrderLeadsWithConfirmations() {
        val csv = VerdictCsvExporter.toCsv(
            listOf(
                row("d-clear", Verdict.CLEAR),
                row("c-error", Verdict.ERROR),
                row("b-unclear", Verdict.INCONCLUSIVE),
                row("a-confirmed", Verdict.CONFIRMED)
            )
        )
        val order = csv.lineSequence().drop(1).map { it.substringBefore(",") }.toList()

        Assertions.assertEquals(
            listOf("a-confirmed", "b-unclear", "c-error", "d-clear"), order,
            "slug order would have produced the same list, so this is ranked by verdict:\n$csv"
        )
    }

    /**
     * A mod that declared nothing recognisable renders as blank rather than as a word implying we asked and
     * were told. Every CurseForge project is in this state, since the platform publishes no sideness at all.
     */
    @Test
    fun anAbsentDeclarationIsNotRenderedAsAnAnswer() {
        val csv = VerdictCsvExporter.toCsv(listOf(row("quiet", Verdict.INCONCLUSIVE, declared = null)))

        Assertions.assertFalse(csv.contains("UNKNOWN"), "an absent claim must not read as a checked one: $csv")
        Assertions.assertFalse(csv.contains("null"), csv)
    }

    /**
     * **Drift guard.** Artifact retention is decided per *attempt* by `BootArtifacts.worthKeeping`, long
     * before a verdict exists, while [Verdict.keepsLogs] states the same policy for the published row. The
     * two are independent expressions of one rule and nothing makes them agree, so this asserts they do:
     * a boot that survived is the only one worth discarding, and CLEAR is the only verdict that discards.
     */
    @Test
    fun attemptRetentionAgreesWithVerdictRetention() {
        Assertions.assertFalse(
            BootArtifacts.worthKeeping(BootResult.SURVIVED),
            "a clean boot keeps nothing, matching Verdict.CLEAR"
        )
        Assertions.assertFalse(Verdict.CLEAR.keepsLogs, "and CLEAR is the verdict a clean boot produces")

        listOf(BootResult.CRASHED, BootResult.INCONCLUSIVE).forEach {
            Assertions.assertTrue(BootArtifacts.worthKeeping(it), "$it must leave evidence behind")
        }
        // Asked over `Verdict.entries` rather than as a list, so a verdict added to the vocabulary fails
        // this guard until somebody classifies it -- the list form covered four of six after LOCKED and
        // UNVERIFIABLE arrived, silently. `VerdictPublicationTest.everyVerdictIsClassifiedForRetention`
        // carries the per-verdict reasoning; this half is only about agreeing with `worthKeeping`.
        Verdict.entries.filter { it.keepsLogs }.forEach {
            Assertions.assertTrue(it.grindRan || it == Verdict.ERROR, "$it keeps logs, so a boot ran or it is ours")
        }
        Assertions.assertEquals(
            setOf(Verdict.CLEAR, Verdict.LOCKED, Verdict.UNVERIFIABLE),
            Verdict.entries.filterNot { it.keepsLogs }.toSet(),
            "the discarding set has to stay in step with BootArtifacts.worthKeeping's only false case"
        )
    }
}
