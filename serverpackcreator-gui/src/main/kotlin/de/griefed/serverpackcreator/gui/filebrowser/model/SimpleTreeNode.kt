package de.griefed.serverpackcreator.gui.filebrowser.model

import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode


class SimpleTreeNode : DefaultMutableTreeNode {

    constructor()
    constructor(child: FileNode) : super(child)

    override fun add(newChild: MutableTreeNode?) {
        super.add(newChild)
        children = sort()
    }

    private fun sort() : Vector<TreeNode> {
        Collections.sort(children, Comparator { o1: TreeNode, o2: TreeNode ->
            val t1 = (o1 as DefaultMutableTreeNode).userObject
            val t2 = (o2 as DefaultMutableTreeNode).userObject
            val f1 = (t1 as FileNode).file
            val f2 = (t2 as FileNode).file
            if (f1.isDirectory == f2.isDirectory) {
                return@Comparator f1.name.compareTo(f2.name)
            } else if (f1.isDirectory) {
                return@Comparator -1
            } else return@Comparator 1
        })

        val directories = mutableListOf<TreeNode>()
        val files = mutableListOf<TreeNode>()

        for (child in children) {
            val t = (child as DefaultMutableTreeNode).userObject
            val f = (t as FileNode).file
            if (f.isDirectory) {
                directories.add(child)
            } else {
                files.add(child)
            }
        }

        sortAlphabetically(directories)
        sortAlphabetically(files)

        return merge(directories,files)
    }

    private fun sortAlphabetically(files: MutableList<TreeNode>) {
        Collections.sort(files, Comparator { o1: TreeNode, o2: TreeNode ->
            val t1 = (o1 as DefaultMutableTreeNode).userObject
            val t2 = (o2 as DefaultMutableTreeNode).userObject
            val f1 = (t1 as FileNode).file
            val f2 = (t2 as FileNode).file
            return@Comparator f1.name.compareTo(f2.name)
        })
    }

    private fun merge(directories: MutableList<TreeNode>,files: MutableList<TreeNode>) : Vector<TreeNode> {
        val merged = mutableListOf<TreeNode>()
        merged.addAll(directories)
        merged.addAll(files)
        return Vector(merged)
    }
}