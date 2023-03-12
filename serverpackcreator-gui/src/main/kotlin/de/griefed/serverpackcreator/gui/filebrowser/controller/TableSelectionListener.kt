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
package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

/**
 * Listener to update the detail and preview panels upon selection of a file in the file-table.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
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