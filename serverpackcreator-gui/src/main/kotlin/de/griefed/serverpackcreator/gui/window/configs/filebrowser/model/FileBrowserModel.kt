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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.model

import de.griefed.serverpackcreator.api.utilities.common.FileType
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.parallelMap
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer.DirectoryLinkIcon
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer.FileLinkIcon
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

/**
 * Base model from which access to the root-manager, tree-model and update routines is granted.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class FileBrowserModel(private val guiProps: GuiProps, private val fileUtilities: FileUtilities) {
    private val log = cachedLoggerOf(this.javaClass)
    private val rootManager: RootManager = RootManager(guiProps)
    val treeModel: DefaultTreeModel = createTreeModel()

    /**
     * Update the root of out root-manager to take filesyste-changes into account.
     */
    fun reload() {
        treeModel.setRoot(updateRoot())
    }

    /**
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    private fun createTreeModel(): DefaultTreeModel {
        return DefaultTreeModel(updateRoot())
    }

    /**
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    private fun updateRoot(): DefaultMutableTreeNode {
        val root = rootManager.root
        addChildNodes(root)
        addGrandchildNodes(root)
        return root
    }

    /**
     * Add grandchild-nodes to the parent for every file inside it.
     *
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    fun addGrandchildNodes(root: DefaultMutableTreeNode) {
        root.children().toList().parallelMap { node ->
            addChildNodes(node as SortedTreeNode)
        }
    }

    /**
     * Add child-nodes to the parent for every file inside it.
     *
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    private fun addChildNodes(root: DefaultMutableTreeNode) {
        var node: DefaultMutableTreeNode
        var sortedNode: SortedTreeNode
        var fileNode: FileNode
        var file: File
        if (rootManager.isWindows) {
            fileNode = root.userObject as FileNode
            file = getFile(fileNode.file)
            if (file.isDirectory) {
                try {
                    file.listFiles()!!.forEach { child ->
                        try {
                            sortedNode = SortedTreeNode(guiProps, FileNode(child))
                            root.add(sortedNode)
                        } catch (npe: Exception) {
                            log.warn("Couldn't access $child.")
                        }
                    }

                } catch (npe: NullPointerException) {
                    log.warn("Couldn't access $file.")
                    root.remove(treeNode)
                }
            }
        } else {
            root.children().toList().forEach { treeNode ->
                node = treeNode as DefaultMutableTreeNode
                fileNode = node.userObject as FileNode
                file = getFile(fileNode.file)
                if (file.isDirectory) {
                    try {
                        file.listFiles()!!.forEach { child ->
                            try {
                                sortedNode = SortedTreeNode(guiProps, FileNode(child))
                                node.add(sortedNode)
                            } catch (npe: Exception) {
                                log.warn("Couldn't access $child.")
                            }
                        }
                    } catch (npe: NullPointerException) {
                        log.warn("Couldn't access $file.")
                        root.remove(treeNode)
                    }
                }
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun getFile(file: File): File {
        val resolved: String
        val type = fileUtilities.checkFileType(file)
        return if (type == FileType.FILE || type == FileType.DIRECTORY) {
            file
        } else {
            resolved = fileUtilities.resolveLink(file)
            File(resolved)
        }
    }

    /**
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    fun getFileIcon(file: File?): Icon {
        return try {
            if (file != null && fileUtilities.isLink(file)) {
                val resolved = fileUtilities.resolveLink(file)
                val resolvedFile = File(resolved)
                if (resolvedFile.isDirectory) {
                    DirectoryLinkIcon()
                } else {
                    FileLinkIcon()
                }
            } else {
                rootManager.fileSystemView.getSystemIcon(file)
            }
        } catch (ex: NullPointerException) {
            FileLinkIcon()
        }
    }

    /**
     * @author Griefed (Kotlin Conversion and minor changes)
     * @author Andrew Thompson
     */
    fun getFileText(file: File?): String {
        return rootManager.fileSystemView.getSystemDisplayName(file)
    }
}