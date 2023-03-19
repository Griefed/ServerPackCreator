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

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.runnable.AddNodes
import de.griefed.serverpackcreator.gui.filebrowser.view.FileDetailPanel
import de.griefed.serverpackcreator.gui.filebrowser.view.FilePreviewPanel
import de.griefed.serverpackcreator.gui.filebrowser.view.TableScrollPane
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Listen to node-selections in the file-tree of our filebrowser and add nodes upon opening of a directory.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class FileSelectionListener(
    private val browserModel: FileBrowserModel,
    private val fileDetailPanel: FileDetailPanel,
    private val filePreviewPanel: FilePreviewPanel,
    private val tableScrollPane: TableScrollPane
) : TreeSelectionListener {

    override fun valueChanged(event: TreeSelectionEvent) {
        val node = event.path.lastPathComponent as DefaultMutableTreeNode
        val fileNode = node.userObject as FileNode
        AddNodes(browserModel, node)
        val file = fileNode.file
        fileDetailPanel.setFileNode(fileNode, browserModel)
        filePreviewPanel.setFileNode(fileNode)
        if (file.isDirectory) {
            tableScrollPane.setDefaultTableModel(node)
        } else {
            tableScrollPane.clearDefaultTableModel()
        }
    }
}