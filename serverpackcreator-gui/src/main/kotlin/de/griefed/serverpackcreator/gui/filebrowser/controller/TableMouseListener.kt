package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.view.SelectionPopMenu
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JTable

class TableMouseListener(
    private val jTable: JTable, configsTab: ConfigsTab, utilities: Utilities
) : SelectionPopMenu(configsTab,utilities) {

    override fun mousePressed(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseEvent.BUTTON3) {
            val r = jTable.rowAtPoint(mouseEvent.point)
            if (r >= 0 && r < jTable.rowCount) {
                jTable.setRowSelectionInterval(r, r)
            } else {
                jTable.clearSelection()
            }
            val rowindex = jTable.selectedRow
            if (rowindex >= 0) {
                val fileNode = jTable.model.getValueAt(rowindex, 5) as FileNode
                val file = fileNode.file
                show(jTable, mouseEvent.x, mouseEvent.y, file)
            }
        }
    }
}