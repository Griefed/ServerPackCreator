package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.runnable.AddNodes
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath

class TreeExpandListener(private val browserModel: FileBrowserModel) : TreeWillExpandListener {
    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(event: TreeExpansionEvent) {
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillExpand(event: TreeExpansionEvent) {
        val path: TreePath = event.path
        val node: DefaultMutableTreeNode = path.lastPathComponent as DefaultMutableTreeNode
        AddNodes(browserModel, node)
    }
}