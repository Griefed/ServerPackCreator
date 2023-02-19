package de.griefed.serverpackcreator.gui.filebrowser.view.renderer

import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

/**
 * TODO docs
 */
class FileNameComparator :Comparator<TreeNode> {
    override fun compare(o1: TreeNode, o2: TreeNode): Int {
        val t1 = (o1 as DefaultMutableTreeNode).userObject
        val t2 = (o2 as DefaultMutableTreeNode).userObject
        val f1 = (t1 as FileNode).file
        val f2 = (t2 as FileNode).file
        return f1.name.compareTo(f2.name)
    }
}