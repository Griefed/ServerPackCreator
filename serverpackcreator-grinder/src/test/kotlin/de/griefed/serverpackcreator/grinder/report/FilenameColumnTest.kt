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
 * Pins the `Filename` column: **the file that was actually sampled, named as the platform names it**, beside
 * the broad historical `NamePattern`.
 *
 * `iris` published `iris-` for Fabric, `iris-neoforge-` for NeoForge and `iris-` for Quilt — three rows
 * where two say nothing about which file was looked at. The name pattern is the common prefix over a
 * project's whole history and must stay broad, because the fallback list matches it with `startsWith`. This
 * column is the other half a maintainer needs: the artifact to go and look at on the project page.
 *
 * **It used to carry a derived *stem* rather than a filename**, and that was the defect Griefed reported on
 * 2026-09-10: `FilenameStemDeriver.deriveStem` was run over the sampled file, so the column titled
 * "Filename" held `iris-fabric-` instead of `iris-fabric-1.7.5+mc1.21.1.jar`. Measured over 400 live rows:
 * **not one** value ended in `.jar`, and **270 (67%)** were byte-identical to `NamePattern`, so the column
 * was redundant two thirds of the time and never once answered the question it is named for.
 *
 * The real name serves the stem's documented purpose strictly better — it keeps the loader token history
 * erases *and* the version, which is what identifies the artifact on the platform.
 *
 * `GrindTargetVerdict.sampleFile` had the right value all along; `Grinder.grind`'s hand-written 18-field copy
 * simply never carried it. That is the same mapping `claude-docs/ANALYSIS-AUDIT.md` flagged on 2026-09-05 as
 * asserted only five fields deep.
 */
internal class FilenameColumnTest {

    private fun row(slug: String, entry: String?, fileName: String?) =
        grindVerdict(slug, "Quilt", verdict = Verdict.CONFIRMED, suggestedEntry = entry)
            .copy(fileName = fileName)

    /** Both columns exist, and the filename sits beside the pattern it identifies an artifact for. */
    @Test
    fun theCsvCarriesBothPatterns() {
        val header = VerdictCsvExporter.toCsv(emptyList()).lineSequence().first()

        Assertions.assertTrue(header.contains("NamePattern"), header)
        Assertions.assertTrue(header.contains("Filename"), header)
    }

    /**
     * **The reported defect.** The Quilt row publishes the broad `iris-` and shows the Fabric artifact it
     * actually booted — by its full published name, not a stem of it.
     */
    @Test
    fun aQuiltRowNamesTheFabricFileItActuallyBooted() {
        val csv = VerdictCsvExporter.toCsv(listOf(row("iris", "iris-", "iris-fabric-1.7.5+mc1.21.1.jar")))

        Assertions.assertTrue(
            csv.contains("iris-fabric-1.7.5+mc1.21.1.jar"),
            "the column has to name the artifact a maintainer would download: $csv"
        )
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
     * **The published entry is unchanged, and this guard matters more now than it did.**
     * `/as-properties` still serves `suggestedEntry`. Publishing a *stem* of one file would already have
     * stopped excluding the builds it misses; publishing a full **filename** would narrow the fallback list
     * to a single build of a single loader, which is the worst version of that mistake.
     */
    @Test
    fun theSampledFilenameIsNotWhatGetsPublished() {
        val rendered = FallbackPropertiesRenderer.render(
            emptyList(), emptyList(), listOf(row("iris", "iris-", "iris-fabric-1.7.5+mc1.21.1.jar"))
        )

        Assertions.assertTrue(rendered.contains("iris-"), rendered)
        Assertions.assertFalse(
            rendered.contains("iris-fabric-1.7.5+mc1.21.1.jar"),
            "the broad historical entry is what must be published, never one artifact's name: $rendered"
        )
    }
}
