package de.griefed.serverpackcreator.gui.filebrowser.model

import de.griefed.serverpackcreator.gui.GuiProps
import java.io.File
import java.util.*
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * TODO docs
 */
class FileBrowserModel(private val guiProps: GuiProps) {
    private val rootManager: RootManager = RootManager(guiProps)
    val treeModel: DefaultTreeModel = createTreeModel()

    /**
     * TODO docs
     */
    fun reload() {
        treeModel.setRoot(updateRoot())
    }

    /**
     * TODO docs
     */
    private fun createTreeModel(): DefaultTreeModel {
        return DefaultTreeModel(updateRoot())
    }

    /**
     * TODO docs
     */
    private fun updateRoot(): DefaultMutableTreeNode {
        val root = rootManager.root
        addChildNodes(root)
        addGrandchildNodes(root)
        return root
    }

    /**
     * TODO docs
     */
    fun addGrandchildNodes(root: DefaultMutableTreeNode) {
        val enumeration: Enumeration<*> = root.children()
        while (enumeration.hasMoreElements()) {
            val node = enumeration.nextElement() as DefaultMutableTreeNode
            addChildNodes(node)
        }
    }

    /**
     * TODO docs
     */
    private fun addChildNodes(root: DefaultMutableTreeNode) {
        if (rootManager.isWindows) {
            val fileNode = root.userObject as FileNode
            val file = fileNode.file
            if (file.isDirectory) {
                val files = file.listFiles()
                files?.forEach { child ->
                    root.add(
                        SortedTreeNode(guiProps,
                            FileNode(child)
                        )
                    )
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
                        file.listFiles()?.forEach { child ->
                            node.add(
                                SortedTreeNode(guiProps,
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

    /**
     * TODO docs
     */
    fun getFileIcon(file: File?): Icon {
        return rootManager.fileSystemView.getSystemIcon(file)
    }

    /**
     * TODO docs
     */
    fun getFileText(file: File?): String {
        return rootManager.fileSystemView.getSystemDisplayName(file)
    }
}