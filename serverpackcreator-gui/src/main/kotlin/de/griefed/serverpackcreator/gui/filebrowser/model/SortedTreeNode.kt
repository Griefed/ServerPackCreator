package de.griefed.serverpackcreator.gui.filebrowser.model

import de.griefed.serverpackcreator.gui.GuiProps
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode


class SortedTreeNode : DefaultMutableTreeNode {
    private val guiProps: GuiProps

    constructor(guiProps: GuiProps) : super() {
        this.guiProps = guiProps
    }

    constructor(guiProps: GuiProps, child: FileNode) : super(child) {
        this.guiProps = guiProps
    }

    override fun add(newChild: MutableTreeNode?) {
        super.add(newChild)
        children = sort()
    }

    private fun sort(): Vector<TreeNode> {
        Collections.sort(children, guiProps.typeComparator)
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

        return merge(directories, files)
    }

    private fun sortAlphabetically(files: MutableList<TreeNode>) {
        Collections.sort(files, guiProps.nameComparator)
    }

    private fun merge(directories: MutableList<TreeNode>, files: MutableList<TreeNode>): Vector<TreeNode> {
        val merged = mutableListOf<TreeNode>()
        merged.addAll(directories)
        merged.addAll(files)
        return Vector(merged)
    }
}