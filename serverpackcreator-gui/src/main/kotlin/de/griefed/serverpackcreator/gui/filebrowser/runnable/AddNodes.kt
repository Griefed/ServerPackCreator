package de.griefed.serverpackcreator.gui.filebrowser.runnable

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import kotlinx.coroutines.*
import javax.swing.tree.DefaultMutableTreeNode

/**
 * TODO docs
 */
@OptIn(DelicateCoroutinesApi::class)
class AddNodes(browserModel: FileBrowserModel,node: DefaultMutableTreeNode, guiProps: GuiProps) {
    init {
        GlobalScope.launch(guiProps.fileBrowserDispatcher) {
            val fileNode: FileNode = node.userObject as FileNode
            if (fileNode.isGenerateGrandchildren) {
                browserModel.addGrandchildNodes(node)
                fileNode.isGenerateGrandchildren = false
            }
        }
    }
}