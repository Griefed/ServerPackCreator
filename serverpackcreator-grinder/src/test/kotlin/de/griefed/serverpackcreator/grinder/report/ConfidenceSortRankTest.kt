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

import de.griefed.serverpackcreator.clientside.Confidence
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Confidence sorts by **severity**, not by the alphabet.
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
internal class ConfidenceSortRankTest {

    private val store = listOf(
        grindVerdict("a-low", "Forge", confidence = Confidence.LOW),
        grindVerdict("b-inconclusive", "Forge", confidence = Confidence.INCONCLUSIVE),
        grindVerdict("c-high", "Forge", confidence = Confidence.HIGH),
        grindVerdict("d-medium", "Forge", confidence = Confidence.MEDIUM)
    )

    private fun confidencesFor(raw: String) = VerdictSelection
        .select(store, VerdictQuery.parse(QueryParams.parse(raw), 250))
        .rows.map { it.confidence }

    @Test
    fun sortingByConfidenceRunsStrongestSignalFirst() {
        Assertions.assertEquals(
            listOf(Confidence.HIGH, Confidence.MEDIUM, Confidence.LOW, Confidence.INCONCLUSIVE),
            confidencesFor("sort=confidence"),
            "a named confidence sort must run by severity; alphabetically INCONCLUSIVE outranks MEDIUM"
        )
    }

    @Test
    fun sortingByConfidenceDescendingRunsWeakestFirst() {
        Assertions.assertEquals(
            listOf(Confidence.INCONCLUSIVE, Confidence.LOW, Confidence.MEDIUM, Confidence.HIGH),
            confidencesFor("sort=confidence&dir=desc")
        )
    }

    /** The named sort and the default must agree — they are the same ordering, asked for two ways. */
    @Test
    fun theNamedSortAgreesWithTheDefaultOrder() {
        Assertions.assertEquals(
            confidencesFor(""), confidencesFor("sort=confidence"),
            "the default order is highest-confidence-first, so an explicit ascending sort must match it"
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
            .map { line -> Confidence.entries.first { line.contains(it.name) } }
            .toList()

        Assertions.assertEquals(
            confidencesFor(""), csvOrder,
            "an unordered /export.csv must use the same confidence ranking the table does"
        )
    }
}
