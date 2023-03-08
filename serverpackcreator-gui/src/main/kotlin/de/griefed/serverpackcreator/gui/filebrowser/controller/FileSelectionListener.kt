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

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.runnable.AddNodes
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode

/**
 * TODO docs
 */
class FileSelectionListener(
    private val frame: FileBrowserFrame,
    private val browserModel: FileBrowserModel,
    private val guiProps: GuiProps
) : TreeSelectionListener {

    /**
     * TODO docs
     */
    override fun valueChanged(event: TreeSelectionEvent) {
        val node = event.path.lastPathComponent as DefaultMutableTreeNode
        val fileNode = node.userObject as FileNode
        AddNodes(browserModel, node,guiProps)
        val file = fileNode.file
        frame.updateFileDetail(fileNode)
        frame.setFilePreviewNode(fileNode)
        if (file.isDirectory) {
            frame.setDefaultTableModel(node)
        } else {
            frame.clearDefaultTableModel()
        }
    }
}