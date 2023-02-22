package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.runnable.AddNodes
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.ExpandVetoException

/**
 * TODO docs
 */
class TreeExpandListener(
    private val browserModel: FileBrowserModel,
    private val guiProps: GuiProps
) : TreeWillExpandListener {

    /**
     * TODO docs
     */
    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(event: TreeExpansionEvent) {
    }

    /**
     * TODO docs
     */
    @Throws(ExpandVetoException::class)
    override fun treeWillExpand(event: TreeExpansionEvent) {
        val path = event.path
        val node = path.lastPathComponent as DefaultMutableTreeNode
        AddNodes(browserModel, node, guiProps)
    }
}