/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.configs.components.ResizeIndicatorScrollPane
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Component
import java.awt.event.*
import java.io.IOException
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.TableModelListener
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
abstract class ConvenientJTable(
    guiProps: GuiProps,
    keyLabel: String = "Key",
    valueLabel: String = "Value",
    tableModel: DefaultTableModel = DefaultTableModel(
        arrayOf<Any>(
            keyLabel,
            valueLabel,
            "", "", "", "", "", ""
        ),
        1
    ),
    tableColumnModel: DefaultTableColumnModel? = null,
    listSelectionModel: DefaultListSelectionModel? = null
) : JTable(tableModel, tableColumnModel, listSelectionModel) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    val scrollPanel: ResizeIndicatorScrollPane

    init {
        rowHeight = 25
        tableHeader.reorderingAllowed = false
        try {
            ButtonColumns(2, guiProps, ColumnType.CLEAR)
            ButtonColumns(3, guiProps, ColumnType.DELETE)
            ButtonColumns(4, guiProps, ColumnType.ADD_BEFORE)
            ButtonColumns(5, guiProps, ColumnType.ADD_AFTER)
            ButtonColumns(6, guiProps, ColumnType.MOVE_UP)
            ButtonColumns(7, guiProps, ColumnType.MOVE_DOWN)
        } catch (ex: IOException) {
            log.error("Couldn't create button column.", ex)
        }
        putClientProperty("terminateEditOnFocusLost", true)
        val sorter = TableRowSorter(this.model)
        sorter.sortsOnUpdates = false
        for (column in 2..7) {
            columnModel.getColumn(column).minWidth = 30
            columnModel.getColumn(column).maxWidth = 30
            columnModel.getColumn(column).width = 30
            columnModel.getColumn(column).resizable = false
            sorter.setSortable(column, false)
        }
        this.rowSorter = sorter
        scrollPanel = ResizeIndicatorScrollPane(guiProps, this)
    }

    fun addTableModelListener(tableModelListener: TableModelListener) {
        model.addTableModelListener(tableModelListener)
    }

    open fun loadData(data: HashMap<String, String>, clearDataBeforeLoad: Boolean = true) {
        if (clearDataBeforeLoad) {
            clearData()
        }
        var row = 0
        val model = model as DefaultTableModel
        for ((key, value) in data) {
            if (key.isBlank() && value.isBlank()) {
                continue
            }
            model.insertRow(row, arrayOf(key,value))
            row += 1
        }
    }

    /**
     * Override to provide Select All editing functionality
     *
     * @author Rob Camick
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
     *
     * @author Rob Camick
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
     * Clear the table of all data.
     *
     * @author Griefed
     */
    fun clearData() {
        val model = model as DefaultTableModel
        for (row in 0 until model.rowCount) {
            model.setValueAt("", row, 0)
        }
        for (row in model.rowCount - 1 downTo 0) {
            if (model.getValueAt(row, 0).toString().isBlank()) {
                model.removeRow(row)
            }
        }
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

    /**
     * Button colums to add to the table
     *
     * @author Griefed
     */
    inner class ButtonColumns(
        column: Int,
        guiProps: GuiProps,
        private val type: ColumnType = ColumnType.CLEAR
    ) : AbstractCellEditor(), TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
        private val originalBorder: Border
        private val renderButton: JButton
        private val editButton: JButton
        private var focusBorder: Border? = null
        private var editorValue: Any? = null
        private var isButtonColumnEditor = false

        private val action: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val modelRow: Int = e.actionCommand.toInt()
                val tableModel = ((e.source as JTable).model as DefaultTableModel)
                when (type) {
                    ColumnType.CLEAR -> {
                        clearRow(tableModel, modelRow)
                    }

                    ColumnType.DELETE -> {
                        if (tableModel.rowCount > 1) {
                            tableModel.removeRow(modelRow)
                        } else {
                            clearRow(tableModel, modelRow)
                        }
                    }

                    ColumnType.ADD_BEFORE -> {
                        tableModel.insertRow(modelRow, arrayOf("", ""))
                    }

                    ColumnType.ADD_AFTER -> {
                        tableModel.insertRow(modelRow + 1, arrayOf("", ""))
                    }

                    ColumnType.MOVE_UP -> {
                        if (modelRow != 0) {
                            tableModel.moveRow(modelRow, modelRow, modelRow - 1)
                        }
                    }

                    ColumnType.MOVE_DOWN -> {
                        if (tableModel.rowCount - 1 != modelRow) {
                            tableModel.moveRow(modelRow, modelRow, modelRow + 1)
                        }
                    }
                }
            }
        }

        /**
         * @author Griefed
         */
        fun clearRow(tableModel: DefaultTableModel, modelRow: Int) {
            tableModel.setValueAt("", modelRow, 0)
            tableModel.setValueAt("", modelRow, 1)
        }

        init {
            when (type) {
                ColumnType.CLEAR -> {
                    renderButton = JButton(guiProps.clearIcon)
                    editButton = JButton(guiProps.clearIcon)
                }

                ColumnType.DELETE -> {
                    renderButton = JButton(guiProps.deleteIcon)
                    editButton = JButton(guiProps.deleteIcon)
                }

                ColumnType.ADD_BEFORE -> {
                    renderButton = JButton(guiProps.addBeforeIcon)
                    editButton = JButton(guiProps.addBeforeIcon)
                }

                ColumnType.ADD_AFTER -> {
                    renderButton = JButton(guiProps.addAfterIcon)
                    editButton = JButton(guiProps.addAfterIcon)
                }

                ColumnType.MOVE_UP -> {
                    renderButton = JButton(guiProps.moveUpIcon)
                    editButton = JButton(guiProps.moveUpIcon)
                }

                else -> {
                    renderButton = JButton(guiProps.moveDownIcon)
                    editButton = JButton(guiProps.moveDownIcon)
                }
            }
            originalBorder = editButton.border
            val columnModel: TableColumnModel = this@ConvenientJTable.columnModel
            columnModel.getColumn(column).cellRenderer = this
            columnModel.getColumn(column).cellEditor = this
            editButton.addActionListener(this)
            this@ConvenientJTable.addMouseListener(this)
        }

        /**
         * The foreground color of the button when the cell has focus
         *
         * @param focusBorder the foreground color
         * @author Griefed
         */
        @Suppress("unused")
        private fun setFocusBorder(focusBorder: Border?) {
            this.focusBorder = focusBorder
            editButton.border = focusBorder
        }

        /**
         * @author Griefed
         */
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

        /**
         * @author Griefed
         */
        override fun getCellEditorValue(): Any? {
            return editorValue
        }

        /**
         * @author Griefed
         */
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
         *
         * @author Griefed
         */
        override fun actionPerformed(e: ActionEvent) {
            val row: Int = this@ConvenientJTable.convertRowIndexToModel(this@ConvenientJTable.editingRow)
            fireEditingStopped()
            val event = ActionEvent(
                this@ConvenientJTable,
                ActionEvent.ACTION_PERFORMED, row.toString()
            )
            action.actionPerformed(event)
        }

        /**
         * @author Griefed
         */
        override fun mouseClicked(e: MouseEvent) {}

        /**
         * When the mouse is pressed the editor is invoked. If you then drag the mouse to another cell
         * before releasing it, the editor is still active. Make sure editing is stopped when the mouse
         * is released.
         *
         * @author Griefed
         */
        override fun mousePressed(e: MouseEvent) {
            if (this@ConvenientJTable.isEditing && this@ConvenientJTable.cellEditor == this) {
                isButtonColumnEditor = true
            }
        }

        /**
         * @author Griefed
         */
        override fun mouseReleased(e: MouseEvent) {
            if (isButtonColumnEditor && this@ConvenientJTable.isEditing) {
                this@ConvenientJTable.cellEditor.stopCellEditing()
            }
            isButtonColumnEditor = false
        }

        /**
         * @author Griefed
         */
        override fun mouseEntered(e: MouseEvent) {}

        /**
         * @author Griefed
         */
        override fun mouseExited(e: MouseEvent) {}


    }

    /**
     * Column types to determine which button to display in a given button column.
     *
     * @author Griefed
     */
    enum class ColumnType {
        /**
         * Clear the row
         */
        CLEAR,

        /**
         * Delete the row
         */
        DELETE,

        /**
         * Add a new row before
         */
        ADD_BEFORE,

        /**
         * Add a new row after
         */
        ADD_AFTER,

        /**
         * Move row up
         */
        MOVE_UP,

        /**
         * Move row down
         */
        MOVE_DOWN
    }
}