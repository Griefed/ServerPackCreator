package de.griefed.serverpackcreator.gui.filebrowser.runnable

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import javax.swing.tree.DefaultMutableTreeNode

class AddNodes(private val browserModel: FileBrowserModel, private val node: DefaultMutableTreeNode) : Runnable {
    override fun run() {
        val fileNode: FileNode = node.userObject as FileNode
        if (fileNode.isGenerateGrandchildren) {
            browserModel.addGrandchildNodes(node)
            fileNode.isGenerateGrandchildren = false
        }
    }
}