package de.griefed.serverpackcreator.gui.filebrowser.runnable

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import kotlinx.coroutines.*
import javax.swing.tree.DefaultMutableTreeNode

@OptIn(DelicateCoroutinesApi::class)
class AddNodes(private val browserModel: FileBrowserModel, private val node: DefaultMutableTreeNode) {
    init {
        GlobalScope.launch(Dispatchers.IO) {
            val fileNode: FileNode = node.userObject as FileNode
            if (fileNode.isGenerateGrandchildren) {
                browserModel.addGrandchildNodes(node)
                fileNode.isGenerateGrandchildren = false
            }
        }
    }
}