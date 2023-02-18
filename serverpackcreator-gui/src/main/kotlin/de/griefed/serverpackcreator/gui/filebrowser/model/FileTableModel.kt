package de.griefed.serverpackcreator.gui.filebrowser.model

import Gui
import de.griefed.serverpackcreator.gui.filebrowser.view.renderer.DateRenderer
import java.awt.Dimension
import java.text.DateFormat
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn

class FileTableModel : AbstractTableModel() {
    private val rows: MutableList<List<Any>> = ArrayList(10)
    private val columns = arrayOf(
        Gui.filebrowser_table_icon.toString(),
        Gui.filebrowser_table_file.toString(),
        Gui.filebrowser_table_size.toString(),
        Gui.filebrowser_table_last.toString(),
        Gui.filebrowser_table_read.toString()
    )

    init {
        DateFormat.getDateInstance(DateFormat.DEFAULT)
    }

    override fun getColumnCount(): Int {
        return columns.size
    }

    override fun getRowCount(): Int {
        return rows.size
    }

    override fun getValueAt(row: Int, column: Int): Any {
        return rows[row][column]
    }

    override fun getColumnClass(column: Int): Class<*> {
        return when (column) {
            0 -> ImageIcon::class.java
            2 -> Long::class.java
            3 -> Date::class.java
            4, 5, 6, 7, 8, 9 -> Boolean::class.java
            else -> String::class.java
        }
    }

    override fun getColumnName(column: Int): String {
        return columns[column]
    }

    fun addRow(browserModel: FileBrowserModel, fileNode: FileNode) {
        val file = fileNode.file
        val list = mutableListOf<Any>()
        list.add(browserModel.getFileIcon(file))
        list.add(browserModel.getFileText(file))
        list.add(file.length())
        list.add(Date(file.lastModified()))
        list.add(file.canRead())
        list.add(fileNode)
        rows.add(list)
    }

    fun removeRows() {
        rows.clear()
    }

    fun setColumnWidths(table: JTable): Int {
        val centerRenderer = DateRenderer()
        centerRenderer.horizontalAlignment = JLabel.CENTER
        table.columnModel.getColumn(3).cellRenderer = centerRenderer
        var width = setColumnWidth(table, 0, 35)
        width += setColumnWidth(table, 1, 450)
        width += setColumnWidth(table, 2, 120)
        width += setColumnWidth(table, 3, 100)
        width += setColumnWidth(table, 4, 0)
        return width + 30
    }

    private fun setColumnWidth(table: JTable, column: Int, width: Int): Int {
        var columnWidth = width
        val tableColumn = table.columnModel.getColumn(column)
        val label = JLabel(tableColumn.headerValue as String)
        val preferred = label.preferredSize
        columnWidth = columnWidth.coerceAtLeast(preferred.getWidth().toInt() + 14)
        tableColumn.preferredWidth = columnWidth
        tableColumn.minWidth = columnWidth
        return columnWidth
    }
}