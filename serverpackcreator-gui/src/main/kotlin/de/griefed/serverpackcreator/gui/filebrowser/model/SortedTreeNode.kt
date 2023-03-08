/* Copyright (C) 2023  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.gui.filebrowser.model

import de.griefed.serverpackcreator.gui.GuiProps
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

/**
 * TODO docs
 */
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

    /**
     * TODO docs
     */
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

    /**
     * TODO docs
     */
    private fun sortAlphabetically(files: MutableList<TreeNode>) {
        Collections.sort(files, guiProps.nameComparator)
    }

    /**
     * TODO docs
     */
    private fun merge(directories: MutableList<TreeNode>, files: MutableList<TreeNode>): Vector<TreeNode> {
        val merged = mutableListOf<TreeNode>()
        merged.addAll(directories)
        merged.addAll(files)
        return Vector(merged)
    }
}