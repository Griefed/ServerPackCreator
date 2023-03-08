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
package de.griefed.serverpackcreator.gui.filebrowser.view

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
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

/**
 * TODO docs
 */
class TreeScrollPane(
    frame: FileBrowserFrame,
    private val browserModel: FileBrowserModel,
    private val configsTab: ConfigsTab,
    utilities: Utilities,
    guiProps: GuiProps
) {
    private val slashRegex: Regex = "/".toRegex()
    var tree: JTree = JTree(this.browserModel.treeModel)
    var scrollPane: JScrollPane

    init {
        tree.addTreeSelectionListener(
            FileSelectionListener(frame, this.browserModel, guiProps)
        )
        tree.addTreeWillExpandListener(
            TreeExpandListener(this.browserModel, guiProps)
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

    /**
     * TODO docs
     */
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