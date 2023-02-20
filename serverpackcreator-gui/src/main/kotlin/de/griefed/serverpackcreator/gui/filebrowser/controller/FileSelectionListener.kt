package de.griefed.serverpackcreator.gui.filebrowser.controller

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
    private val browserModel: FileBrowserModel
) : TreeSelectionListener {

    /**
     * TODO docs
     */
    override fun valueChanged(event: TreeSelectionEvent) {
        val node = event.path.lastPathComponent as DefaultMutableTreeNode
        val fileNode = node.userObject as FileNode
        AddNodes(browserModel, node)
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