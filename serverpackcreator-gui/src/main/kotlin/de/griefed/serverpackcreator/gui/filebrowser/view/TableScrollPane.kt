package de.griefed.serverpackcreator.gui.filebrowser.view

import Gui
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.filebrowser.controller.TableMouseListener
import de.griefed.serverpackcreator.gui.filebrowser.controller.TableSelectionListener
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.model.FileTableModel
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.text.NumberFormat
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

class TableScrollPane(
    frame: FileBrowserFrame,
    private val model: FileBrowserModel,
    configsTab: ConfigsTab,
    utilities: Utilities
) {
    private var ftModel: FileTableModel
    private var countLabel: JLabel
    private var tsListener: TableSelectionListener
    private var scrollPane: JScrollPane
    var panel: JPanel = JPanel()

    init {
        panel.layout = BorderLayout()

        val countPanel = JPanel()
        countLabel = JLabel(" ")
        countPanel.add(countLabel)
        panel.add(countPanel, BorderLayout.NORTH)

        ftModel = FileTableModel()
        val table = JTable(ftModel)
        table.autoCreateRowSorter = true
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.columnSelectionAllowed = false
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.addMouseListener(TableMouseListener(table, configsTab,utilities))

        tsListener = TableSelectionListener(frame, table)
        tsListener.setRowCount(ftModel.rowCount)

        val lsm: ListSelectionModel = table.selectionModel
        lsm.addListSelectionListener(tsListener)
        val width: Int = ftModel.setColumnWidths(table)
        table.preferredScrollableViewportSize = Dimension(width, table.rowHeight * 12)
        scrollPane = JScrollPane(table)
        panel.add(scrollPane, BorderLayout.CENTER)
    }

    private fun buildLabelString(count: Int): String {
        val nf: NumberFormat = NumberFormat.getInstance()
        return (nf.format(count.toLong()) + " " + Gui.filebrowser_table_header)
    }

    fun clearDefaultTableModel() {
        ftModel.removeRows()
        countLabel.text = " "
        ftModel.fireTableDataChanged()
    }

    fun setDefaultTableModel(node: DefaultMutableTreeNode) {
        ftModel.removeRows()
        val fileNode: FileNode = node.userObject as FileNode
        val file: File = fileNode.file
        if (file.isDirectory) {
            val enumeration: Enumeration<*> = node.children()
            while (enumeration.hasMoreElements()) {
                val childNode: DefaultMutableTreeNode = enumeration.nextElement() as DefaultMutableTreeNode
                val childFileNode: FileNode = childNode.userObject as FileNode
                ftModel.addRow(model, childFileNode)
            }
        }
        tsListener.setRowCount(ftModel.rowCount)
        countLabel.text = buildLabelString(ftModel.rowCount)
        ftModel.fireTableDataChanged()
        scrollPane.verticalScrollBar.value = 0
    }
}