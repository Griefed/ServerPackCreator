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
package de.griefed.serverpackcreator.plugin.grinder.gui

import de.griefed.serverpackcreator.plugin.grinder.core.GrinderVerdict
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Pins the table **model**, which is where the checkbox-to-selection logic lives. The rendering is left
 * untested by design, matching this project's stance on tables elsewhere: trivial format lambdas against
 * brittle component assertions. Everything a user can get wrong by clicking, though, is decided here.
 *
 * No Swing component is instantiated, so this runs headless — an `AbstractTableModel` needs no display.
 */
internal class VerdictTableModelTest {

    private fun verdict(slug: String, entry: String? = "$slug-", verdict: String = "CONFIRMED") = GrinderVerdict(
        slug = slug,
        projectUrl = "https://modrinth.com/mod/$slug",
        platform = "Modrinth",
        loader = "Fabric",
        minecraftLine = "1.21",
        verdict = verdict,
        suggestedEntry = entry,
        fileName = null,
        detail = "because",
        scannedAt = "2026-09-04T12:30:00Z"
    )

    /** Ticking a box adds that row's entry to the selection and tells whoever is listening. */
    @Test
    fun tickingARowSelectsItsEntry() {
        var published: Set<String>? = null
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore")))
            onSelectionChanged = { published = it }
        }

        model.setValueAt(true, 0, VerdictTableModel.TICK_COLUMN)

        Assertions.assertEquals(setOf("creativecore-"), model.selection)
        Assertions.assertEquals(setOf("creativecore-"), published)
        Assertions.assertEquals(true, model.getValueAt(0, VerdictTableModel.TICK_COLUMN))
    }

    /** And unticking removes exactly that one. */
    @Test
    fun untickingARowDeselectsItsEntry() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore"), verdict("jei")))
            selection = setOf("creativecore-", "jei-")
        }

        model.setValueAt(false, 0, VerdictTableModel.TICK_COLUMN)

        Assertions.assertEquals(setOf("jei-"), model.selection)
    }

    /**
     * A row the grinder gave no name-pattern for has nothing that could be excluded, so its box is not
     * editable. Rendering it as an ordinary unticked checkbox would invite a click that silently does
     * nothing — the worst of the three options.
     */
    @Test
    fun refusesToTickARowThatOffersNoEntry() {
        val model = VerdictTableModel().apply { setRows(listOf(verdict("mystery", entry = null))) }

        Assertions.assertFalse(model.isCellEditable(0, VerdictTableModel.TICK_COLUMN))

        // Even driven directly, as a stale edit from a sorted view could, it must not select nothing.
        model.setValueAt(true, 0, VerdictTableModel.TICK_COLUMN)
        Assertions.assertTrue(model.selection.isEmpty())
    }

    /** Only the tick column is editable; the verdict itself is the grinder's to state, not the user's. */
    @Test
    fun leavesEveryOtherColumnReadOnly() {
        val model = VerdictTableModel().apply { setRows(listOf(verdict("creativecore"))) }
        for (column in 0 until model.columnCount) {
            Assertions.assertEquals(
                column == VerdictTableModel.TICK_COLUMN, model.isCellEditable(0, column),
                "column ${model.getColumnName(column)} had the wrong editability"
            )
        }
    }

    /**
     * The tick column reports Boolean, which is what makes JTable render a checkbox rather than "true" —
     * and every other column reports String, so a change making them all Boolean cannot pass by
     * satisfying only the first half.
     */
    @Test
    fun declaresTheTickColumnAsBooleanAndTheRestAsText() {
        val model = VerdictTableModel()
        // javaObjectType is the BOXED java.lang.Boolean; Boolean::class.java would be primitive
        // boolean.class, which JTable has no renderer for.
        Assertions.assertEquals(Boolean::class.javaObjectType, model.getColumnClass(VerdictTableModel.TICK_COLUMN))
        for (column in 0 until model.columnCount) {
            if (column != VerdictTableModel.TICK_COLUMN) {
                Assertions.assertEquals(
                    String::class.java, model.getColumnClass(column),
                    "column ${model.getColumnName(column)} would render with the wrong editor"
                )
            }
        }
    }

    /** Select-all ticks what can be ticked and passes over what cannot. */
    @Test
    fun selectAllSkipsRowsWithNoEntry() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore"), verdict("mystery", entry = null), verdict("jei")))
        }

        model.selectAll()

        Assertions.assertEquals(setOf("creativecore-", "jei-"), model.selection)
    }

    /**
     * Deselect-all clears only what this table shows. The two panes hold separate lists and share one
     * saved selection, so clearing the Confirmed table must not untick anything in Other Verdicts.
     */
    @Test
    fun deselectAllClearsOnlyTheRowsThisTableShows() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore")))
            selection = setOf("creativecore-", "from-the-other-tab-")
        }

        model.deselectAll()

        Assertions.assertEquals(setOf("from-the-other-tab-"), model.selection)
    }

    /**
     * A refresh replaces the rows, never the selection. Entries the grinder has stopped reporting stay
     * ticked — the same rule SelectionStore keeps, for the same reason: silently un-excluding a mod the
     * user chose to exclude is the one outcome nobody would notice until their server pack was wrong.
     */
    @Test
    fun refreshingRowsKeepsTheSelectionIntact() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore"), verdict("jei")))
            selection = setOf("creativecore-", "jei-")
        }

        model.setRows(listOf(verdict("creativecore")))

        Assertions.assertEquals(setOf("creativecore-", "jei-"), model.selection)
        Assertions.assertEquals(true, model.getValueAt(0, VerdictTableModel.TICK_COLUMN))
    }

    /** A row ticked from the saved configuration shows as ticked without anyone clicking it. */
    @Test
    fun showsARestoredSelectionAsTicked() {
        val model = VerdictTableModel().apply {
            selection = setOf("creativecore-")
            setRows(listOf(verdict("creativecore"), verdict("jei")))
        }

        Assertions.assertEquals(true, model.getValueAt(0, VerdictTableModel.TICK_COLUMN))
        Assertions.assertEquals(false, model.getValueAt(1, VerdictTableModel.TICK_COLUMN))
    }

    /** The columns carry what the row actually says, so a user can audit a verdict before ticking it. */
    @Test
    fun rendersTheVerdictsOwnFields() {
        val model = VerdictTableModel().apply { setRows(listOf(verdict("creativecore", verdict = "INCONCLUSIVE"))) }

        val cells = (0 until model.columnCount).associate { model.getColumnName(it) to model.getValueAt(0, it) }
        Assertions.assertEquals("creativecore", cells["Name"])
        Assertions.assertEquals("creativecore-", cells["Entry"])
        Assertions.assertEquals("INCONCLUSIVE", cells["Verdict"])
        Assertions.assertEquals("1.21", cells["Minecraft"])
        Assertions.assertEquals("Fabric", cells["Loader"])
        Assertions.assertEquals("Modrinth", cells["Platform"])
        Assertions.assertEquals("because", cells["Detail"])
        Assertions.assertEquals("2026-09-04T12:30:00Z", cells["Scanned"])
    }

    /** Setting the selection wholesale, as loading a saved configuration does, does not re-notify. */
    @Test
    fun doesNotPublishASelectionItWasHandedRatherThanTold() {
        var published = 0
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore")))
            onSelectionChanged = { published++ }
        }

        model.selection = setOf("creativecore-")

        Assertions.assertEquals(
            0, published,
            "loading a saved selection must not look like a user edit, or the tab saves on every refresh"
        )
    }


    // --- the evidence columns -------------------------------------------------------------------------

    /**
     * **Why these two columns exist.** `Verdict` is the conclusion; `Declared` and `JAR sideness` are the
     * two things it was concluded from, and they disagree often enough to be worth reading side by side —
     * on the live feed, 161 of 2057 rows are `CONTRADICTORY`, which means the platform and the jar say
     * different things about the same mod.
     *
     * They sit immediately after `Verdict` for that reason: conclusion first, then what it rests on.
     */
    @Test
    fun theEvidenceColumnsSitBesideTheVerdict() {
        val model = VerdictTableModel()
        val names = (0 until model.columnCount).map { model.getColumnName(it) }

        Assertions.assertEquals(
            listOf("Verdict", "Declared", "JAR sideness"),
            names.subList(names.indexOf("Verdict"), names.indexOf("Verdict") + 3),
            "the conclusion, then the two readings it came from: $names"
        )
    }

    /** Each shows what the grinder recorded, verbatim — this table reports, it does not re-interpret. */
    @Test
    fun theEvidenceColumnsShowWhatTheGrinderRecorded() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("creativecore").copy(declared = "CONTRADICTORY", jarScan = "SERVER_OR_BOTH")))
        }
        val names = (0 until model.columnCount).map { model.getColumnName(it) }

        Assertions.assertEquals("CONTRADICTORY", model.getValueAt(0, names.indexOf("Declared")))
        Assertions.assertEquals("SERVER_OR_BOTH", model.getValueAt(0, names.indexOf("JAR sideness")))
    }

    /**
     * **The Minecraft line is the row's identity**, and without it a project's several rows are
     * indistinguishable: one project is ground once per era, and the same loader routinely holds more than
     * one of them, so `Loader` alone no longer tells two rows apart.
     */
    @Test
    fun oneProjectsRowsAreToldApartByTheirMinecraftLine() {
        val model = VerdictTableModel().apply {
            setRows(
                listOf(
                    verdict("aether").copy(minecraftLine = "1.21", loader = "NeoForge"),
                    verdict("aether").copy(minecraftLine = "1.20", loader = "NeoForge")
                )
            )
        }
        val era = (0 until model.columnCount).map { model.getColumnName(it) }.indexOf("Minecraft")

        Assertions.assertEquals(
            listOf("1.21", "1.20"), listOf(model.getValueAt(0, era), model.getValueAt(1, era)),
            "two rows of one project under one loader, and only this column separates them"
        )
    }

    /** A daemon older than the line axis sends no era, and an absent one must not render as a real one. */
    @Test
    fun aRowFromADaemonWithoutTheLineAxisShowsNoEra() {
        val model = VerdictTableModel().apply { setRows(listOf(verdict("jei").copy(minecraftLine = null))) }
        val names = (0 until model.columnCount).map { model.getColumnName(it) }

        Assertions.assertEquals("", model.getValueAt(0, names.indexOf("Minecraft")))
    }

    /**
     * A row the grinder recorded nothing for shows an empty cell. 18 of 2057 rows on the live feed have
     * `declared: null`, and "null" in a table cell reads as a value rather than as its absence.
     */
    @Test
    fun anUnrecordedReadingShowsNothingRatherThanTheWordNull() {
        val model = VerdictTableModel().apply {
            setRows(listOf(verdict("mystery").copy(declared = null, jarScan = null)))
        }
        val names = (0 until model.columnCount).map { model.getColumnName(it) }

        Assertions.assertEquals("", model.getValueAt(0, names.indexOf("Declared")))
        Assertions.assertEquals("", model.getValueAt(0, names.indexOf("JAR sideness")))
    }


    /**
     * `VerdictListPane` sizes that column by index, so the constant and the column list must not drift —
     * the failure mode is a *different* column being widened, which nothing else would notice.
     */
    @Test
    fun theSizedColumnConstantsPointAtTheColumnsTheyName() {
        val model = VerdictTableModel()

        Assertions.assertEquals("Declared", model.getColumnName(VerdictTableModel.DECLARED_COLUMN))
        Assertions.assertEquals("JAR sideness", model.getColumnName(VerdictTableModel.JAR_SIDENESS_COLUMN))
    }
}
