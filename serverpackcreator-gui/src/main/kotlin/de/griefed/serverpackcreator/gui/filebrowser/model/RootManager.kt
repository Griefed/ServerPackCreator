package de.griefed.serverpackcreator.gui.filebrowser.model

import de.griefed.serverpackcreator.gui.GuiProps
import java.io.File
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode

class RootManager(private val guiProps: GuiProps) {
    private val osName = System.getProperty("os.name")
    val fileSystemView: FileSystemView = FileSystemView.getFileSystemView()

    @get:Throws(IllegalStateException::class)
    val root: DefaultMutableTreeNode
        get() {
            if (isWindows) {
                return myComputer
            }
            val roots = File.listRoots()
            val rootNode = SortedTreeNode(guiProps)
            if (roots.isNotEmpty()) {
                for (root in roots) {
                    rootNode.add(SortedTreeNode(guiProps,FileNode(root)))
                }
            } else {
                throw IllegalStateException("No roots available")
            }
            return rootNode
        }

    @get:Throws(IllegalStateException::class)
    private val myComputer: DefaultMutableTreeNode
        get() {
            for (file in fileSystemView.roots) {
                if (file.name.equals("::{20D04FE0-3AEA-1069-A2D8-08002B30309D}", ignoreCase = true)) {
                    return SortedTreeNode(guiProps,FileNode(file))
                }
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        if (child.name.equals("::{20D04FE0-3AEA-1069-A2D8-08002B30309D}", ignoreCase = true)) {
                            return SortedTreeNode(guiProps,FileNode(child))
                        }
                    }
                }
            }
            throw IllegalStateException("My Computer not available!")
        }
    private val isOSX: Boolean
        get() = osName.equals("Max OS X", ignoreCase = true)
    private val isLinux: Boolean
        get() = osName.equals("Linux", ignoreCase = true)
    private val isSolaris: Boolean
        get() = osName.equals("SunOS", ignoreCase = true)
    val isWindows: Boolean
        get() = !(isOSX || isLinux || isSolaris) && osName.lowercase().contains("windows")
}