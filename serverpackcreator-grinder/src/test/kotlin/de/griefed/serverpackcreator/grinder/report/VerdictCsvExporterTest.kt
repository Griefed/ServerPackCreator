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
import java.time.Instant

/**
 * Pins the CSV export: a header, highest-confidence-first ordering, the name-pattern column, and
 * RFC-4180 escaping for fields carrying commas / quotes / newlines (a mod detail can contain any).
 */
internal class VerdictCsvExporterTest {

    @Test
    fun emitsHeaderAndOrdersHighestConfidenceFirst() {
        val csv = VerdictCsvExporter.toCsv(
            listOf(
                grindVerdict("low-mod", "Forge", confidence = Confidence.LOW),
                grindVerdict("high-mod", "Forge", confidence = Confidence.HIGH),
                grindVerdict("medium-mod", "Forge", confidence = Confidence.MEDIUM)
            )
        )
        val lines = csv.lines()
        Assertions.assertEquals("Name,Project,NamePattern,Confidence,Loader,Platform,ProjectSideness,JarSideness,Detail,Rule,Dependencies,Scanned", lines[0])
        Assertions.assertTrue(lines[1].startsWith("high-mod,"), "HIGH must come first: ${lines[1]}")
        Assertions.assertTrue(lines[2].startsWith("medium-mod,"))
        Assertions.assertTrue(lines[3].startsWith("low-mod,"))
    }

    @Test
    fun carriesTheNamePatternColumn() {
        val csv = VerdictCsvExporter.toCsv(listOf(grindVerdict("jei", "Forge", suggestedEntry = "jei-")))
        // Name, Project, NamePattern=jei-, Confidence, Loader, Detail
        Assertions.assertTrue(csv.lines()[1].contains(",jei-,"), "name-pattern column missing: ${csv.lines()[1]}")
    }

    @Test
    fun escapesFieldsWithCommasQuotesAndNewlines() {
        val csv = VerdictCsvExporter.toCsv(
            listOf(grindVerdict("mod", "Forge", detail = "crashed: a, b and \"quoted\"\nsecond line"))
        )
        // The detail field must be wrapped and its quotes doubled; embedded newline stays inside the quotes.
        Assertions.assertTrue(csv.contains("\"crashed: a, b and \"\"quoted\"\"\nsecond line\""), "bad escaping:\n$csv")
    }

    /**
     * The scan date rides along in the export too, so a downloaded CSV can be sorted or filtered by it — the
     * store has always carried `verifiedAt`, and until now nothing showed it.
     */
    @Test
    fun carriesTheScanDate() {
        val csv = VerdictCsvExporter.toCsv(
            listOf(grindVerdict("jei", "Forge", verifiedAt = Instant.parse("2026-08-23T19:41:13Z")))
        )
        Assertions.assertTrue(csv.lines()[1].endsWith(",2026/08/23"), "scan date must close the row: ${csv.lines()[1]}")
    }

    @Test
    fun emptyVerdictsStillEmitTheHeader() {
        Assertions.assertEquals("Name,Project,NamePattern,Confidence,Loader,Platform,ProjectSideness,JarSideness,Detail,Rule,Dependencies,Scanned", VerdictCsvExporter.toCsv(emptyList()))
    }
}
