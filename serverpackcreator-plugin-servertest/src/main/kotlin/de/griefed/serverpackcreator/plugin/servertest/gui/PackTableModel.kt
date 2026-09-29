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
import de.griefed.serverpackcreator.plugin.servertest.core.SessionState
import de.griefed.serverpackcreator.plugin.servertest.core.StartScriptSelection
import javax.swing.table.AbstractTableModel

/**
 * One row of the pack list: a pack and what it is doing right now.
 *
 * The status is computed here rather than stored on [LaunchablePack], because launchability is a fact about
 * the pack on disk while running-ness is a fact about this session.
 */
data class PackRow(
    /** The pack this row shows. */
    val pack: LaunchablePack,
    /** The last state reported for this pack, including after it stopped; `null` if it never ran. */
    val state: SessionState?,
    /** Whether a server is running out of it *right now*, which [state] alone cannot say. */
    val running: Boolean
) {

    /**
     * Whether Start should be offered: the pack has a script and nothing is running out of it.
     *
     * Keyed on [running] rather than on [state] being null, so a pack that has already been tested once can
     * be tested again — its last state stays on show as `Stopped (exit 0)` while the button comes back.
     */
    val startable: Boolean
        get() = !running && pack.selection is StartScriptSelection.Available

    /** What the Status column reads, in one short phrase. */
    val status: String
        get() = when (state) {
            SessionState.Starting -> "Starting…"
            SessionState.Ready -> "Running"
            SessionState.Stopping -> "Stopping…"
            is SessionState.Exited -> "Stopped (exit ${state.exitCode ?: "unknown"})"
            // A blocked pack's reason belongs here rather than in a tooltip: it is the answer to the only
            // question the row raises, which is why its Start button is grey.
            null -> when (val selection = pack.selection) {
                is StartScriptSelection.Available -> "Ready to launch"
                is StartScriptSelection.Missing -> selection.reason
            }
        }
}

/**
 * The pack list's table model.
 *
 * Holds rows and nothing else — no selection state, no launching. Separated from the pane because this is the
 * part worth testing: what each column says for a pack that is idle, blocked or running is exactly the logic
 * a user reads, while the pane's rendering is not worth the brittleness of asserting on it.
 *
 * @author Griefed
 */
class PackTableModel : AbstractTableModel() {

    /** The rows on show. Assigning fires a table-wide refresh. */
    var rows: List<PackRow> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    /** The pack in [row], for the pane to act on when Start is pressed. */
    fun packAt(row: Int): LaunchablePack = rows[row].pack

    /** Whether [row] can be launched right now. */
    fun startableAt(row: Int): Boolean = rows[row].startable

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = COLUMNS.size

    override fun getColumnName(column: Int): String = COLUMNS[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): String {
        val row = rows[rowIndex]
        return when (columnIndex) {
            NAME_COLUMN -> row.pack.name
            MINECRAFT_COLUMN -> row.pack.minecraftVersion
            MODLOADER_COLUMN -> row.pack.modloader
            MODLOADER_VERSION_COLUMN -> row.pack.modloaderVersion
            else -> row.status
        }
    }

    companion object {
        /** Column order, and the only place it is spelled. */
        val COLUMNS = listOf("Server Pack", "Minecraft", "Modloader", "Loader Version", "Status")

        /** Index of the pack-name column. */
        const val NAME_COLUMN = 0

        /** Index of the Minecraft-version column. */
        const val MINECRAFT_COLUMN = 1

        /** Index of the modloader column. */
        const val MODLOADER_COLUMN = 2

        /** Index of the modloader-version column. */
        const val MODLOADER_VERSION_COLUMN = 3

        /** Index of the status column — the widest, and the one carrying a blocked pack's reason. */
        const val STATUS_COLUMN = 4
    }
}
