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
package de.griefed.serverpackcreator.plugin.servertest.gui

import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import de.griefed.serverpackcreator.plugin.servertest.core.StartScript
import de.griefed.serverpackcreator.plugin.servertest.core.StartScripts
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.Container
import java.io.File
import javax.swing.JTable

/**
 * Pins that redrawing the list does not take the user's selection away.
 *
 * Reported from use: picking a different start script cleared the selected pack, so Start greyed out and
 * the pack had to be picked again before it could be launched — the dropdown made the list *harder* to
 * use than before it existed. The cause is that replacing the rows fires a table-wide change, and a table
 * told everything changed drops its selection.
 *
 * The same redraw runs on Refresh and whenever a generation finishes, so this is not only about the
 * dropdown: an auto-refresh landing while somebody was about to press Start would have done the same
 * thing.
 *
 * Driven through a real [PackListPane] and its real [JTable], because what is under test is Swing's
 * reaction to a model event — the one thing a hand-rolled assertion about which event was fired would get
 * to decide for itself.
 */
internal class PackListPaneSelectionTest {

    private val sh = StartScripts.forKey("sh")
    private val bat = StartScripts.forKey("bat")

    private fun pack(name: String, scripts: Set<String> = setOf("sh", "bat")) =
        LaunchablePack(File("/packs/$name"), name, "1.21", "NeoForge", "21.0.18", scripts)

    private fun rows(script: StartScript, vararg packs: LaunchablePack) =
        packs.map { PackRow(it, null, running = false, script) }

    private fun paneWith(vararg packs: LaunchablePack): Pair<PackListPane, JTable> {
        val pane = PackListPane(listOf(sh, bat), {}, {})
        pane.show(rows(sh, *packs))
        return pane to requireNotNull(findTable(pane)) { "PackListPane must contain a JTable." }
    }

    private fun findTable(container: Container): JTable? {
        for (child in container.components) {
            if (child is JTable) return child
            if (child is Container) findTable(child)?.let { return it }
        }
        return null
    }

    /** The pack selected in the table right now, by name, or `null` when nothing is selected. */
    private fun selectedName(pane: PackListPane, table: JTable): String? {
        val view = table.selectedRow
        if (view < 0) {
            return null
        }
        return table.getValueAt(view, PackTableModel.NAME_COLUMN) as String
    }

    /** The reported bug: choosing another script must leave the chosen pack chosen. */
    @Test
    fun changingTheStartScriptKeepsThePackSelected() {
        val alpha = pack("Alpha")
        val (pane, table) = paneWith(alpha, pack("Beta"))
        table.setRowSelectionInterval(0, 0)
        Assertions.assertEquals("Alpha", selectedName(pane, table))

        pane.show(rows(bat, alpha, pack("Beta")))

        Assertions.assertEquals(
            "Alpha",
            selectedName(pane, table),
            "Changing the start script must not cost the user their selected pack."
        )
    }

    /**
     * Restored by *pack*, not by row index.
     *
     * A refresh can reorder the list — a generation adds a pack that sorts before the selected one — and
     * restoring an index would quietly move the selection onto a different pack. That is worse than losing
     * it, because Start would then launch something the user did not pick.
     */
    @Test
    fun theSamePackStaysSelectedEvenWhenTheListReorders() {
        val zebra = pack("Zebra")
        val (pane, table) = paneWith(zebra)
        table.setRowSelectionInterval(0, 0)

        pane.show(rows(sh, pack("Alpha"), pack("Middle"), zebra))

        Assertions.assertEquals(
            "Zebra",
            selectedName(pane, table),
            "The selection must follow the pack, not the row it happened to be in."
        )
    }

    /** A pack that has gone leaves nothing selected, rather than selecting whatever took its place. */
    @Test
    fun aSelectedPackThatDisappearsLeavesNothingSelected() {
        val gone = pack("Gone")
        val (pane, table) = paneWith(gone, pack("Stays"))
        table.setRowSelectionInterval(0, 0)

        pane.show(rows(sh, pack("Stays")))

        Assertions.assertNull(
            selectedName(pane, table),
            "A pack that is no longer there must not hand its selection to another."
        )
    }

    /** Redrawing with nothing selected selects nothing — the restore must not invent a selection. */
    @Test
    fun redrawingWithNothingSelectedSelectsNothing() {
        val (pane, table) = paneWith(pack("Alpha"), pack("Beta"))

        pane.show(rows(bat, pack("Alpha"), pack("Beta")))

        Assertions.assertNull(selectedName(pane, table))
    }
}
