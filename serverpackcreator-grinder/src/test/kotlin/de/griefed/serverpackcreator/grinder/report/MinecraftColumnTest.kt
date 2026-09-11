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

import de.griefed.serverpackcreator.grinder.GrindVerdict
import de.griefed.serverpackcreator.grinder.grindVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the `Minecraft` column — the one that says which era a verdict is about, and therefore the one a
 * reader needs before any other cell means anything.
 *
 * `VerdictField` is the single declaration behind the header, the CSV cell, the query key, the filter and
 * the sort, so one entry has to carry all five. What it cannot carry for free is the **ordering**: a
 * version-line sorted as text puts `1.9` above `1.20`, which is the mistake `minecraftComparator` exists to
 * prevent one layer down and would be just as silent here — a plausible order rather than an error.
 *
 * @author Griefed
 */
internal class MinecraftColumnTest {

    private fun row(slug: String, line: String, loader: String = "Forge") =
        grindVerdict(slug, loader, minecraftLine = line, minecraftVersion = "$line.1")

    private fun select(rows: List<GrindVerdict>, rawQuery: String?) =
        VerdictSelection.select(rows, VerdictQuery.parse(QueryParams.parse(rawQuery), VerdictQuery.DEFAULT_PAGE_SIZE))

    /** The line is a column of the table and of the CSV, from the one declaration both render from. */
    @Test
    fun theLineIsAColumnOfTheTableAndTheCsv() {
        val header = VerdictCsvExporter.toCsv(emptyList()).lineSequence().first()
        Assertions.assertTrue(header.contains("Minecraft"), header)

        val html = VerdictReportRenderer.toHtml(select(listOf(row("jei", "1.12")), null))
        Assertions.assertTrue(html.contains(">1.12<"), "the row must show the era it is about")
    }

    /** Filtering by era is the question an operator actually asks of this table. */
    @Test
    fun rowsFilterByTheirMinecraftLine() {
        val rows = listOf(row("jei", "1.21"), row("jei", "1.12"), row("sodium", "1.21"))

        Assertions.assertEquals(
            listOf("jei" to "1.12"),
            select(rows, "f.minecraft=1.12").rows.map { it.slug to it.minecraftLine }
        )
    }

    /**
     * **Sorted numerically, not as text.** `1.9` above `1.20` is exactly the failure the selector's own
     * comparator exists for, and a table cannot report it — it just looks like an odd order.
     */
    @Test
    fun theLineSortsNumericallyRatherThanAlphabetically() {
        val rows = listOf(row("a", "1.9"), row("b", "1.20"), row("c", "1.12"), row("d", "26.2"))

        Assertions.assertEquals(
            listOf("1.9", "1.12", "1.20", "26.2"),
            select(rows, "sort=minecraft").rows.map { it.minecraftLine }
        )
    }

    /**
     * Inside one project the newest era leads, which is both the order the grind produces and the one a
     * reader wants — the line a pack is most likely being built on first.
     */
    @Test
    fun aProjectsRowsLeadWithItsNewestEra() {
        val rows = listOf(row("jei", "1.12"), row("jei", "26.2"), row("jei", "1.20"))

        Assertions.assertEquals(
            listOf("26.2", "1.20", "1.12"),
            select(rows, null).rows.map { it.minecraftLine }
        )
    }

    /**
     * A row written before the axis moved has no line, and says so with a blank rather than a word: "1.20"
     * would claim an era nobody recorded, and "UNKNOWN" reads as though we looked.
     */
    @Test
    fun aLegacyRowShowsNoEraRatherThanAGuessedOne() {
        val legacy = grindVerdict("jei", "Forge", minecraftLine = null, minecraftVersion = null)

        Assertions.assertEquals("", VerdictField.MINECRAFT.text(legacy))
        Assertions.assertEquals("", VerdictField.MINECRAFT_VERSION.text(legacy))
    }

    /** The exact version rides beside the line, so a finding can be reproduced rather than only located. */
    @Test
    fun theExactVersionIsCarriedBesideTheLine() {
        Assertions.assertEquals("1.12.1", VerdictField.MINECRAFT_VERSION.text(row("jei", "1.12")))
    }
}
