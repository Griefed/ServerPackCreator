package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.runnable.AddNodes
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import java.io.File
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode

class FileSelectionListener(
    private val frame: FileBrowserFrame,
    private val browserModel: FileBrowserModel
) : TreeSelectionListener {
    override fun valueChanged(event: TreeSelectionEvent) {
        val node: DefaultMutableTreeNode = event.path.lastPathComponent as DefaultMutableTreeNode
        val fileNode: FileNode = node.userObject as FileNode
        val addNodes = AddNodes(browserModel, node)
        Thread(addNodes).start()
        val file: File = fileNode.file
        frame.updateFileDetail(fileNode)
        frame.setDesktopButtonFileNode(fileNode)
        if (file.isDirectory) {
            frame.setDefaultTableModel(node)
        } else {
            frame.clearDefaultTableModel()
        }
    }
}