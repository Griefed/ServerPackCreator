package de.griefed.serverpackcreator.gui.filebrowser.view

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.filebrowser.controller.FileSelectionListener
import de.griefed.serverpackcreator.gui.filebrowser.controller.TreeExpandListener
import de.griefed.serverpackcreator.gui.filebrowser.controller.TreeMouseListener
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.view.renderer.FileTreeCellRenderer
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.Dimension
import java.io.File
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.text.Position
import javax.swing.tree.TreePath

class TreeScrollPane(
    frame: FileBrowserFrame,
    private val browserModel: FileBrowserModel,
    private val configsTab: ConfigsTab,
    utilities: Utilities
) {
    private val slashRegex: Regex = "/".toRegex()
    var tree: JTree = JTree(this.browserModel.treeModel)
    var scrollPane: JScrollPane

    init {
        tree.addTreeSelectionListener(
            FileSelectionListener(frame, this.browserModel)
        )
        tree.addTreeWillExpandListener(
            TreeExpandListener(this.browserModel)
        )
        tree.isRootVisible = true
        tree.cellRenderer = FileTreeCellRenderer(this.browserModel)
        tree.showsRootHandles = true
        tree.addMouseListener(TreeMouseListener(tree, configsTab,utilities))
        scrollPane = JScrollPane(tree)
        val preferredSize: Dimension = scrollPane.preferredSize
        val widePreferred = Dimension(
            300, preferredSize.getHeight().toInt()
        )
        scrollPane.preferredSize = widePreferred
    }

    fun expandPathsToModpackDir() {
        val activeTab = configsTab.selectedEditor
        if (activeTab != null) {
            val prefixes = File(activeTab.getModpackDirectory()).absolutePath
                .replace("\\", "/").split(slashRegex)
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            var path: TreePath? = null
            for (prefix in prefixes) {
                path = tree.getNextMatch(prefix, 0, Position.Bias.Forward)
                tree.expandPath(path)
            }
            if (path != null) {
                tree.selectionPath = path
            }
        }
    }
}