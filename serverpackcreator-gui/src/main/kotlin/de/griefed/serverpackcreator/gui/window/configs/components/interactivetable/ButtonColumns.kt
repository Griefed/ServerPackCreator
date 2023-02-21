package de.griefed.serverpackcreator.gui.window.configs.components.interactivetable

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.ImageUtilities
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.border.Border
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumnModel

/**
 * TODO docs
 */
class ButtonColumns(
    private val table: JTable,
    column: Int,
    private val guiProps: GuiProps,
    private val type: ColumnType = ColumnType.CLEAR
) : AbstractCellEditor(), TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
    private val originalBorder: Border
    private val renderButton: JButton
    private val editButton: JButton
    private var focusBorder: Border? = null
    private var editorValue: Any? = null
    private var isButtonColumnEditor = false
    private val size = 18

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
     * TODO docs
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
        val columnModel: TableColumnModel = table.columnModel
        columnModel.getColumn(column).cellRenderer = this
        columnModel.getColumn(column).cellEditor = this
        editButton.addActionListener(this)
        table.addMouseListener(this)
    }

    /**
     * The foreground color of the button when the cell has focus
     *
     * @param focusBorder the foreground color
     */
    @Suppress("unused")
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

    /**
     * TODO docs
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