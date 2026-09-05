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
 * Pins the `Filename` column: the pattern of the artifact actually sampled, beside the broad historical
 * `NamePattern`.
 *
 * `iris` published `iris-` for Fabric, `iris-neoforge-` for NeoForge and `iris-` for Quilt — three rows
 * where two say nothing about which file was looked at. The name pattern is the common prefix over a
 * project's whole history and must stay broad, because the fallback list matches it with `startsWith`. The
 * filename pattern is derived from the sampled file alone and keeps the loader token that history erases.
 *
 * The two are shown side by side rather than one replacing the other: the first is what gets *published*,
 * the second is what a maintainer needs to check the finding against the platform page.
 */
internal class FilenameColumnTest {

    private fun row(slug: String, entry: String?, filename: String?) =
        grindVerdict(slug, "Quilt", verdict = Verdict.CONFIRMED, suggestedEntry = entry)
            .copy(filenamePattern = filename)

    /** Both columns exist, and the filename sits beside the pattern it narrows. */
    @Test
    fun theCsvCarriesBothPatterns() {
        val header = VerdictCsvExporter.toCsv(emptyList()).lineSequence().first()

        Assertions.assertTrue(header.contains("NamePattern"), header)
        Assertions.assertTrue(header.contains("Filename"), header)
    }

    /** The reported Quilt row: broad entry published, Fabric filename shown beside it. */
    @Test
    fun aQuiltRowShowsTheFabricFileItActuallyBooted() {
        val csv = VerdictCsvExporter.toCsv(listOf(row("iris", "iris-", "iris-fabric-")))

        Assertions.assertTrue(csv.contains("iris-fabric-"), "the sampled artifact must be visible: $csv")
        Assertions.assertTrue(csv.contains("iris-"), csv)
    }

    /** A row with no sampled file renders blank rather than repeating the broad entry. */
    @Test
    fun aRowWithoutASampleRendersBlank() {
        val csv = VerdictCsvExporter.toCsv(listOf(row("quiet", "quiet-", null)))
        val cells = csv.lineSequence().drop(1).first().split(",")

        Assertions.assertTrue(
            cells.any { it.isBlank() },
            "an absent sample must not be filled in with the historical stem: $csv"
        )
    }

    /**
     * **The published entry is unchanged.** `/as-properties` still serves `suggestedEntry`; adding a
     * narrower column must not narrow what reaches users' fallback lists, or a mod would stop being
     * excluded for the builds the narrow pattern misses.
     */
    @Test
    fun theFilenamePatternIsNotWhatGetsPublished() {
        val rendered = FallbackPropertiesRenderer.render(
            emptyList(), emptyList(), listOf(row("iris", "iris-", "iris-fabric-"))
        )

        Assertions.assertTrue(rendered.contains("iris-"), rendered)
        Assertions.assertFalse(
            rendered.contains("iris-fabric-"),
            "the broad historical entry is what must be published, not the narrow one: $rendered"
        )
    }
}
