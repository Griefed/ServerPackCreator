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
package de.griefed.serverpackcreator.gui.old.utilities

import Gui
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Color
import java.awt.Component
import java.awt.event.*
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.LineBorder
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
class ScriptSettings(
    tableModel: TableModel,
    tableColumnModel: TableColumnModel?,
    listSelectionModel: ListSelectionModel?
) : JTable(tableModel, tableColumnModel, listSelectionModel) {
    private val log = cachedLoggerOf(this.javaClass)
    private var isSelectAllForMouseEvent = false
    private var isSelectAllForActionEvent = false
    private var isSelectAllForKeyEvent = false

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
                Gui.createserverpack_gui_createserverpack_scriptsettings_table_column_clear.toString()
            ),
            100
        ),
        null,
        null
    )

    init {
        try {
            val delete: Action = object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val table: JTable = e.source as JTable
                    val modelRow: Int = e.actionCommand.toInt()
                    (table.model as DefaultTableModel).removeRow(modelRow)
                }
            }
            ButtonColumn(this, delete, 2)
        } catch (ex: IOException) {
            log.error("Couldn't create button column.", ex)
        }
        putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)
        setSelectAllForEdit(true)
        getColumnModel().getColumn(2).minWidth = 60
        getColumnModel().getColumn(2).maxWidth = 60
        getColumnModel().getColumn(2).width = 60
        getColumnModel().getColumn(2).resizable = false
        tableModel.setValueAt("", 0, 0)
        tableModel.setValueAt("", 0, 1)
    }

    /**
     * Sets the Select All property for all event types
     *
     * @param isSelectAllForEdit Whether to select all for editing
     */
    private fun setSelectAllForEdit(isSelectAllForEdit: Boolean) {
        isSelectAllForMouseEvent = isSelectAllForEdit
        isSelectAllForActionEvent = isSelectAllForEdit
        isSelectAllForKeyEvent = isSelectAllForEdit
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
        if (isSelectAllForMouseEvent
            || isSelectAllForActionEvent
            || isSelectAllForKeyEvent
        ) {
            selectAll(e)
        }
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

        //  Typing in the cell was used to activate the editor
        if (e is KeyEvent && isSelectAllForKeyEvent) {
            (editor as JTextComponent).selectAll()
            return
        }

        //  F2 was used to activate the editor
        if (e is ActionEvent && isSelectAllForActionEvent) {
            (editor as JTextComponent).selectAll()
            return
        }

        //  A mouse click was used to activate the editor.
        //  Generally this is a double click and the second mouse click is
        //  passed to the editor which would remove the text selection unless
        //  we use the invokeLater()
        if (e is MouseEvent && isSelectAllForMouseEvent) {
            SwingUtilities.invokeLater { (editor as JTextComponent).selectAll() }
        }
    }

    /**
     * Set the Select All property when editing is invoked by the mouse
     *
     * @param isSelectAllForMouseEvent Whether to select all for editing
     */
    fun setSelectAllForMouseEvent(isSelectAllForMouseEvent: Boolean) {
        this.isSelectAllForMouseEvent = isSelectAllForMouseEvent
    }

    /**
     * Set the Select All property when editing is invoked by the "F2" key
     *
     * @param isSelectAllForActionEvent Whether to select all for editing
     */
    fun setSelectAllForActionEvent(isSelectAllForActionEvent: Boolean) {
        this.isSelectAllForActionEvent = isSelectAllForActionEvent
    }

    /**
     * Set the Select All property when editing is invoked by typing directly into the cell
     *
     * @param isSelectAllForKeyEvent Whether to select all for editing
     */
    fun setSelectAllForKeyEvent(isSelectAllForKeyEvent: Boolean) {
        this.isSelectAllForKeyEvent = isSelectAllForKeyEvent
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

    inner class ButtonColumn(
        private val table: JTable,
        private val action: Action,
        column: Int
    ) : AbstractCellEditor(), TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
        private val originalBorder: Border
        private val renderButton: JButton
        private val editButton: JButton
        private var focusBorder: Border? = null
        private var editorValue: Any? = null
        private var isButtonColumnEditor = false
        private val resourcePrefix = "/de/griefed/resources/gui"

        init {
            val delete = ImageUtilities.imageIconFromResourceStream(this.javaClass, "$resourcePrefix/delete.png")
                .getScaledInstance(32, 32)
            renderButton = JButton(delete)
            editButton = JButton(delete)
            editButton.isFocusPainted = false
            editButton.addActionListener(this)
            originalBorder = editButton.border
            setFocusBorder(LineBorder(Color.BLUE))
            val columnModel: TableColumnModel = table.columnModel
            columnModel.getColumn(column).cellRenderer = this
            columnModel.getColumn(column).cellEditor = this
            table.addMouseListener(this)
        }

        /**
         * The foreground color of the button when the cell has focus
         *
         * @param focusBorder the foreground color
         */
        private fun setFocusBorder(focusBorder: Border?) {
            this.focusBorder = focusBorder
            editButton.border = focusBorder
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            editorValue = value
            return editButton
        }

        override fun getCellEditorValue(): Any? {
            return editorValue
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            if (isSelected) {
                renderButton.foreground = table.selectionForeground
                renderButton.background = table.selectionBackground
            } else {
                renderButton.foreground = table.foreground
                renderButton.background = UIManager.getColor("Button.background")
            }
            if (hasFocus) {
                renderButton.border = focusBorder
            } else {
                renderButton.border = originalBorder
            }
            return renderButton
        }

        /**
         * The button has been pressed. Stop editing and invoke the custom Action
         */
        override fun actionPerformed(e: ActionEvent) {
            val row: Int = table.convertRowIndexToModel(table.editingRow)
            fireEditingStopped()
            val event = ActionEvent(
                table,
                ActionEvent.ACTION_PERFORMED, row.toString()
            )
            action.actionPerformed(event)
        }

        override fun mouseClicked(e: MouseEvent) {}

        /**
         * When the mouse is pressed the editor is invoked. If you then drag the mouse to another cell
         * before releasing it, the editor is still active. Make sure editing is stopped when the mouse
         * is released.
         */
        override fun mousePressed(e: MouseEvent) {
            if (table.isEditing && table.cellEditor == this) {
                isButtonColumnEditor = true
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            if (isButtonColumnEditor && table.isEditing) {
                table.cellEditor.stopCellEditing()
            }
            isButtonColumnEditor = false
        }

        override fun mouseEntered(e: MouseEvent) {}
        override fun mouseExited(e: MouseEvent) {}
    }
}