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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.SelectionPopMenu
import java.awt.event.MouseEvent
import javax.swing.JTable

/**
 * Mouse-listener to present the context menu upon pressing the right mouse-button on a file in the file table.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class TableMouseListener(
    private val jTable: JTable, tabbedConfigsTab: TabbedConfigsTab, utilities: Utilities
) : SelectionPopMenu(tabbedConfigsTab, utilities) {

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