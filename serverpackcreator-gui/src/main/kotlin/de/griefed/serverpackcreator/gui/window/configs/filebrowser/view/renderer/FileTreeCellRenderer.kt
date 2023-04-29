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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer

import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.SortedTreeNode
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

/**
 * Renderer for a node in the tree.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPLFile
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
        val node = value as SortedTreeNode
        if (node.userObject != null) {
            val fileNode = node.userObject as FileNode
            val file = fileNode.file
            label.icon = browserModel.getFileIcon(file)
            label.text = browserModel.getFileText(file)
        } else {
            label.icon = null
            label.text = ""
        }
        return label
    }
}