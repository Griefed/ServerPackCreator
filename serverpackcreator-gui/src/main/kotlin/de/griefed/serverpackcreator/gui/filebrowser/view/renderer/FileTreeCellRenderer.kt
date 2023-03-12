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
package de.griefed.serverpackcreator.gui.filebrowser.view.renderer

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import java.awt.Component
import java.io.File
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

/**
 * Renderer for a node in the tree.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
class FileTreeCellRenderer(private val browserModel: FileBrowserModel) : TreeCellRenderer {
    private val label: JLabel = JLabel(" ")

    init {
        label.isOpaque = false
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val node: DefaultMutableTreeNode = value as DefaultMutableTreeNode
        val fileNode: FileNode = node.userObject as FileNode
        val file: File = fileNode.file
        label.icon = browserModel.getFileIcon(file)
        label.text = browserModel.getFileText(file)
        return label
    }
}