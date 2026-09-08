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
import javax.swing.table.AbstractTableModel

/**
 * The rows of one verdict list, and which of them are ticked.
 *
 * The selection is held as a set of *entries* rather than of row indices, which is what lets it survive
 * a refresh that reorders or drops rows, and what lets two tables share one saved selection without
 * either owning it.
 *
 * @author Griefed
 */
class VerdictTableModel : AbstractTableModel() {

    private var rows: List<GrinderVerdict> = emptyList()

    /**
     * The entries currently ticked, across both panes.
     *
     * Assigning it is how a saved configuration is restored, so it deliberately does **not** fire
     * [onSelectionChanged] — treating a load as a user edit would have the tab writing its config on
     * every refresh. User edits go through [setValueAt], [selectAll] and [deselectAll], which do.
     */
    var selection: Set<String> = emptySet()
        set(value) {
            field = value.toSet()
            fireTableDataChanged()
        }

    /** Called when the *user* changes the selection, with the new full set. */
    var onSelectionChanged: ((Set<String>) -> Unit)? = null

    /** Replace the displayed rows, leaving the selection untouched. */
    fun setRows(newRows: List<GrinderVerdict>) {
        rows = newRows
        fireTableDataChanged()
    }

    /** The verdict displayed in [row], for a detail view or a link. */
    fun rowAt(row: Int): GrinderVerdict = rows[row]

    /** Tick every row that offers an entry; rows without one are passed over rather than silently missed. */
    fun selectAll() = publish(selection + rows.mapNotNull { it.exclusionEntry })

    /**
     * Untick every row **this** table shows, leaving the rest of the selection alone — the other pane's
     * ticks live in the same set and are none of this table's business.
     */
    fun deselectAll() = publish(selection - rows.mapNotNull { it.exclusionEntry }.toSet())

    override fun getRowCount() = rows.size

    override fun getColumnCount() = COLUMNS.size

    override fun getColumnName(column: Int): String = COLUMNS[column]

    /**
     * `Boolean` for the tick column is what makes `JTable` render a checkbox instead of the word "true".
     *
     * `javaObjectType`, not `Boolean::class.java`: the latter is the **primitive** `boolean.class`, which
     * `JTable`'s renderer table has no entry for, so the column would fall back to the string renderer and
     * show "true"/"false". Spelling it `java.lang.Boolean::class.java` picks the same class but warns
     * ("not recommended for use in Kotlin"), and `javaObjectType` is the idiom that says boxed without it.
     */
    override fun getColumnClass(column: Int): Class<*> =
        if (column == TICK_COLUMN) Boolean::class.javaObjectType else String::class.java

    /** Only the tick is the user's to change; the verdict itself is the grinder's statement. */
    override fun isCellEditable(row: Int, column: Int) =
        column == TICK_COLUMN && rows[row].exclusionEntry != null

    override fun getValueAt(row: Int, column: Int): Any {
        val verdict = rows[row]
        return when (column) {
            // Read once: `exclusionEntry` trims and re-checks on every access, and getValueAt runs per
            // visible cell per repaint.
            TICK_COLUMN -> verdict.exclusionEntry?.let { it in selection } == true
            1 -> verdict.slug
            2 -> verdict.exclusionEntry.orEmpty()
            3 -> verdict.verdict
            // The two readings the verdict was drawn from. Empty rather than "null" when the grinder
            // recorded none: a table cell reading "null" looks like a value.
            4 -> verdict.declared.orEmpty()
            5 -> verdict.jarScan.orEmpty()
            6 -> verdict.loader
            7 -> verdict.platform
            8 -> verdict.scannedAt
            else -> verdict.detail
        }
    }

    /**
     * Apply a tick. A row offering no entry is ignored even when driven directly — a sorted view can
     * deliver a stale index, and selecting "nothing" would put an empty entry in the exclusion list.
     */
    override fun setValueAt(value: Any?, row: Int, column: Int) {
        if (column != TICK_COLUMN) {
            return
        }
        val entry = rows[row].exclusionEntry ?: return
        publish(if (value == true) selection + entry else selection - entry)
    }

    /** Adopt [updated] as the selection, repaint, and tell the listener this was a user edit. */
    private fun publish(updated: Set<String>) {
        selection = updated
        onSelectionChanged?.invoke(updated)
    }

    companion object {
        /** The checkbox column, addressed by name so the panes and the guards cannot drift from it. */
        const val TICK_COLUMN = 0

        /**
         * `Declared` and `JAR sideness` sit immediately after `Verdict` on purpose: the conclusion, then
         * the two readings it was drawn from. They disagree often enough to be worth reading together —
         * 161 of 2057 rows on the live feed are `CONTRADICTORY`.
         */
        private val COLUMNS = listOf(
            "", "Name", "Entry", "Verdict", "Declared", "JAR sideness", "Loader", "Platform",
            "Scanned", "Detail"
        )
    }
}
