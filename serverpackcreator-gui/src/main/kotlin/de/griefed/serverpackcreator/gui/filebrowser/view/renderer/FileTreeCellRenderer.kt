package de.griefed.serverpackcreator.gui.filebrowser.view.renderer

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import java.awt.Component
import java.io.File
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

class FileTreeCellRenderer(private val browserModel: FileBrowserModel) : TreeCellRenderer {
    private val label: JLabel = JLabel(" ")

    init {
        label.isOpaque = true
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