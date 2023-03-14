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

import de.griefed.serverpackcreator.api.utilities.common.parallelMap
import de.griefed.serverpackcreator.gui.GuiProps
import java.io.File
import java.util.*
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

/**
 * Base model from which access to the root-manager, tree-model and update routines is granted.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
class FileBrowserModel(private val guiProps: GuiProps) {
    private val rootManager: RootManager = RootManager(guiProps)
    val treeModel: DefaultTreeModel = createTreeModel()

    /**
     * Update the root of out root-manager to take filesyste-changes into account.
     */
    fun reload() {
        treeModel.setRoot(updateRoot())
    }

    private fun createTreeModel(): DefaultTreeModel {
        return DefaultTreeModel(updateRoot())
    }

    private fun updateRoot(): DefaultMutableTreeNode {
        val root = rootManager.root
        addChildNodes(root)
        addGrandchildNodes(root)
        return root
    }

    /**
     * Add grandchild-nodes to the parent for every file inside it.
     */
    fun addGrandchildNodes(root: DefaultMutableTreeNode) {
        root.children().toList().parallelMap {node ->
            addChildNodes(node as DefaultMutableTreeNode)
        }
        /*val enumeration: Enumeration<TreeNode> = root.children()
        while (enumeration.hasMoreElements()) {
            val node = enumeration.nextElement() as DefaultMutableTreeNode
            addChildNodes(node)
        }*/
    }

    /**
     * Add child-nodes to the parent for every file inside it.
     */
    private fun addChildNodes(root: DefaultMutableTreeNode) {
        if (rootManager.isWindows) {
            val fileNode = root.userObject as FileNode
            val file = fileNode.file
            if (file.isDirectory) {
                try {
                    file.listFiles()!!.forEach { child ->
                        root.add(
                            SortedTreeNode(
                                guiProps,
                                FileNode(child)
                            )
                        )
                    }

                } catch (npe: NullPointerException) {
                    npe.printStackTrace()
                }
            }
        } else {
            val enumeration: Enumeration<*> = root.children()
            while (enumeration.hasMoreElements()) {
                val node = enumeration.nextElement() as DefaultMutableTreeNode
                val fileNode = node.userObject as FileNode
                val file = fileNode.file
                if (file.isDirectory) {
                    try {
                        file.listFiles()!!.forEach { child ->
                            node.add(
                                SortedTreeNode(
                                    guiProps,
                                    FileNode(child)
                                )
                            )
                        }
                    } catch (npe: NullPointerException) {
                        npe.printStackTrace()
                    }
                }
            }
        }

    }

    fun getFileIcon(file: File?): Icon {
        return rootManager.fileSystemView.getSystemIcon(file)
    }

    fun getFileText(file: File?): String {
        return rootManager.fileSystemView.getSystemDisplayName(file)
    }
}