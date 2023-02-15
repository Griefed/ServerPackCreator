/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.gui.window.components.interactivetable

import Gui
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Component
import java.awt.event.*
import java.io.IOException
import java.lang.Boolean.TRUE
import java.util.*
import javax.swing.*
import javax.swing.table.*
import javax.swing.text.JTextComponent

/**
 * Hey, Griefed here. This class is based on another masterpiece by the wonderful Rob Camick. It
 * provides a couple of convenience methods which make the script settings experience a little nice,
 * so I thought I'd make use of it, and expand on it whilst I am already at it.
 *
 * Source at [Table Select All
 * Editor](https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/).
 *
 * The ScriptSettings provides some extensions to the default JTable
 *
 *
 * 1) Select All editing - when a text related cell is placed in editing mode the text is selected.
 * Controlled by invoking a "setSelectAll..." method.
 *
 *
 * 2) reorderColumns - static convenience method for reodering table columns
 *
 * @author Rob Camick
 * @author Griefed
 */
@Suppress("unused")
class InteractiveTable(
    tableModel: TableModel,
    tableColumnModel: TableColumnModel?,
    listSelectionModel: ListSelectionModel?
) : JTable(tableModel, tableColumnModel, listSelectionModel) {
    private val log = cachedLoggerOf(this.javaClass)
    val scrollPanel: JScrollPane

    /**
     * Create a new Script Settings table, using ServerPackCreators I18n in order to set the column
     * names according to the currently used language.
     *
     */
    constructor() : this(
        DefaultTableModel(
            arrayOf<Any>(
                Gui.createserverpack_gui_createserverpack_scriptsettings_table_column_variable.toString(),
                Gui.createserverpack_gui_createserverpack_scriptsettings_table_column_value.toString(),
                "", "", "", "", "", ""
            ),
            1
        ),
        null,
        null
    )

    init {
        setRowHeight(25)
        tableHeader.reorderingAllowed = false
        try {
            ButtonColumns(this, 2, ButtonColumns.ColumnType.CLEAR)
            ButtonColumns(this, 3, ButtonColumns.ColumnType.DELETE)
            ButtonColumns(this, 4, ButtonColumns.ColumnType.ADD_BEFORE)
            ButtonColumns(this, 5, ButtonColumns.ColumnType.ADD_AFTER)
            ButtonColumns(this, 6, ButtonColumns.ColumnType.MOVE_UP)
            ButtonColumns(this, 7, ButtonColumns.ColumnType.MOVE_DOWN)
        } catch (ex: IOException) {
            log.error("Couldn't create button column.", ex)
        }
        putClientProperty("terminateEditOnFocusLost", TRUE)
        val sorter = TableRowSorter(this.model)
        sorter.sortsOnUpdates = false
        for (column in 2..7) {
            getColumnModel().getColumn(column).minWidth = 30
            getColumnModel().getColumn(column).maxWidth = 30
            getColumnModel().getColumn(column).width = 30
            getColumnModel().getColumn(column).resizable = false
            sorter.setSortable(column, false)
        }
        this.rowSorter = sorter
        scrollPanel = JScrollPane(this)
        clearData()
    }

    /**
     * Override to provide Select All editing functionality
     */
    override fun editCellAt(
        row: Int,
        column: Int,
        e: EventObject?
    ): Boolean {
        val result = super.editCellAt(row, column, e)
        selectAll(e)
        return result
    }

    /**
     * Select the text when editing on a text related cell is started
     */
    private fun selectAll(e: EventObject?) {
        val editor: Component = editorComponent as? JTextComponent ?: return
        if (e == null) {
            (editor as JTextComponent).selectAll()
            return
        }

        if (e is KeyEvent) {
            (editor as JTextComponent).selectAll()
            return
        }

        if (e is ActionEvent) {
            (editor as JTextComponent).selectAll()
            return
        }

        if (e is MouseEvent) {
            SwingUtilities.invokeLater { (editor as JTextComponent).selectAll() }
        }
    }

    /**
     * Clear and load the provided hashmap into the table. They `KEY` is placed into column 1
     * (Placeholder) , the `VALUE` is placed into column 2 (Value).
     *
     * @param data The map containing the data to load into the table.
     * @author Griefed
     */
    fun loadData(data: HashMap<String, String>) {
        clearData()
        var row = 0
        for ((key, value) in data) {
            model.setValueAt(key, row, 0)
            model.setValueAt(value, row, 1)
            row += 1
        }
    }

    /**
     * Clear the table of all data. Only leave SPC_JAVA_SPC behind.
     *
     * @author Griefed
     */
    fun clearData() {
        for (row in 0 until model.rowCount) {
            model.setValueAt("", row, 0)
            model.setValueAt("", row, 1)
        }
        model.setValueAt("SPC_JAVA_SPC", 0, 0)
        model.setValueAt("java", 0, 1)
    }

    /**
     * Get the data from the table as a map. Column 1 (Placeholder) will be mapped to the maps
     * `KEY`, column 2 (Value) will be mapped to the maps `VALUE`. Rows are ignored of
     * they do not contain values for both columns.
     *
     * @return A map containing the data of the table.
     * @author Griefed
     */
    fun getData(): HashMap<String, String> {
        val data = HashMap<String, String>(100)
        for (row in 0 until model.rowCount) {
            if (model.getValueAt(row, 0).toString().isNotEmpty()
                && model.getValueAt(row, 1).toString().isNotEmpty()
            ) {
                data[model.getValueAt(row, 0).toString()] = model.getValueAt(row, 1).toString()
            }
        }
        return data
    }
}