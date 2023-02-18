package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class TableSelectionListener(private val frame: FileBrowserFrame, private val jTable: JTable) : ListSelectionListener {
    private var rowCount = 0

    fun setRowCount(rowCount: Int) {
        this.rowCount = rowCount
    }

    override fun valueChanged(event: ListSelectionEvent) {
        if (!event.valueIsAdjusting) {
            val lsm = event.source as ListSelectionModel
            var row = lsm.minSelectionIndex
            if (row in 0 until rowCount) {
                row = jTable.convertRowIndexToModel(row)
                val fileNode: FileNode = jTable.model
                    .getValueAt(row, 5) as FileNode
                frame.updateFileDetail(fileNode)
                frame.setFilePreviewNode(fileNode)
            }
        }
    }
}