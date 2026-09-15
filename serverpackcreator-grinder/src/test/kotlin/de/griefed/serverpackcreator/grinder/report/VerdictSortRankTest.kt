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

import de.griefed.serverpackcreator.clientside.Verdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * The verdict sorts by **rank**, not by the alphabet.
 *
 * Observed on the live report 2026-08-29: `?sort=confidence` returned HIGH, HIGH, INCONCLUSIVE,
 * INCONCLUSIVE, INCONCLUSIVE, LOW — alphabetical order, in which INCONCLUSIVE ("nothing was learned")
 * outranks both MEDIUM and LOW. Only the *default* order carried a rank; a named sort fell through to
 * `VerdictField.text`, which for this column is the enum name.
 *
 * The fix is the one the plan specified: a `sortKey` on [VerdictField], defaulting to `text` and overridden
 * only here — which also collapses the two hand-maintained rank tables (this layer's and the CSV
 * exporter's) onto one declaration.
 */
internal class VerdictSortRankTest {

    private val store = listOf(
        grindVerdict("a-low", "Forge", verdict = Verdict.ERROR),
        grindVerdict("b-inconclusive", "Forge", verdict = Verdict.CLEAR),
        grindVerdict("c-high", "Forge", verdict = Verdict.CONFIRMED),
        grindVerdict("d-medium", "Forge", verdict = Verdict.INCONCLUSIVE)
    )

    private fun verdictsFor(raw: String) = VerdictSelection
        .select(store, VerdictQuery.parse(QueryParams.parse(raw), 250))
        .rows.map { it.verdict }

    /**
     * **Drift guard.** An unranked verdict sorts to `99` — behind everything, silently — so adding one to
     * the vocabulary and forgetting the rank table produces a column that looks sorted and is not. Asked of
     * `Verdict.entries` rather than of a list written here, or this guard would need the same edit it exists
     * to demand.
     */
    @Test
    fun everyVerdictHasARank() {
        Assertions.assertEquals(
            emptyList<Verdict>(),
            Verdict.entries.filterNot { it in VerdictField.VERDICT_RANK },
            "an unranked verdict sorts behind everything without saying so"
        )
        Assertions.assertEquals(
            Verdict.entries.size,
            VerdictField.VERDICT_RANK.values.distinct().size,
            "two verdicts sharing a rank makes their relative order the sort's own accident"
        )
    }

    @Test
    fun sortingByVerdictRunsTheFindingsFirst() {
        Assertions.assertEquals(
            listOf(Verdict.CONFIRMED, Verdict.INCONCLUSIVE, Verdict.ERROR, Verdict.CLEAR),
            verdictsFor("sort=verdict"),
            "a named verdict sort must run by rank; alphabetically CLEAR would outrank CONFIRMED"
        )
    }

    @Test
    fun sortingByVerdictDescendingRunsTheQuietRowsFirst() {
        Assertions.assertEquals(
            listOf(Verdict.CLEAR, Verdict.ERROR, Verdict.INCONCLUSIVE, Verdict.CONFIRMED),
            verdictsFor("sort=verdict&dir=desc")
        )
    }

    /** The named sort and the default must agree — they are the same ordering, asked for two ways. */
    @Test
    fun theNamedSortAgreesWithTheDefaultOrder() {
        Assertions.assertEquals(
            verdictsFor(""), verdictsFor("sort=verdict"),
            "the default order leads with confirmations, so an explicit ascending sort must match it"
        )
    }

    /**
     * One rank table, not two. The CSV exporter kept its own copy, so the table and the export could drift
     * into disagreeing about what "highest confidence first" means — the exact hazard the single
     * [VerdictField] declaration exists to remove.
     */
    @Test
    fun theCsvDefaultOrderIsTheSameOrdering() {
        val csvOrder = VerdictCsvExporter.toCsv(store).lineSequence()
            .drop(1).filter { it.isNotBlank() }
            // Matched on the Verdict column's own cell rather than anywhere in the line: INCONCLUSIVE is
            // a member of both the old and the new vocabulary, so a loose `contains` would still find a
            // stale one and quietly agree with itself.
            .map { line -> Verdict.entries.first { line.split(",").any { cell -> cell == it.name } } }
            .toList()

        Assertions.assertEquals(
            verdictsFor(""), csvOrder,
            "an unordered /export.csv must use the same verdict ranking the table does"
        )
    }
}
