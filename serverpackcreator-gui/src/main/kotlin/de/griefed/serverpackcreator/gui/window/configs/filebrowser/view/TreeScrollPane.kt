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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller.FileSelectionListener
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller.TreeExpandListener
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller.TreeMouseListener
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer.FileTreeCellRenderer
import java.awt.Dimension
import javax.swing.JScrollPane
import javax.swing.JTree

/**
 * Scroll-pane housing our tree with all our nodes.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class TreeScrollPane(
    browserModel: FileBrowserModel,
    tabbedConfigsTab: TabbedConfigsTab,
    utilities: Utilities,
    fileDetailPanel: FileDetailPanel,
    filePreviewPanel: FilePreviewPanel,
    tableScrollPane: TableScrollPane,
    guiProps: GuiProps
) : JScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    var tree: JTree = JTree(browserModel.treeModel)

    init {
        viewport.view = tree
        tree.addTreeSelectionListener(
            FileSelectionListener(
                browserModel, fileDetailPanel, filePreviewPanel,
                tableScrollPane, utilities.fileUtilities, guiProps
            )
        )
        tree.addTreeWillExpandListener(TreeExpandListener(browserModel,guiProps,utilities.fileUtilities))
        tree.isRootVisible = true
        tree.cellRenderer = FileTreeCellRenderer(browserModel)
        tree.showsRootHandles = true
        tree.addMouseListener(TreeMouseListener(tree, tabbedConfigsTab, utilities))
        val widePreferred = Dimension(
            300, preferredSize.getHeight().toInt()
        )
        preferredSize = widePreferred
    }
}