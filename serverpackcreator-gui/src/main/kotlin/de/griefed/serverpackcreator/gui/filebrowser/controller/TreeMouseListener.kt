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
package de.griefed.serverpackcreator.gui.filebrowser.controller

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.filebrowser.view.SelectionPopMenu
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Mouse-listener to display the context-menu when a user presses the right mouse-button on a node.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
class TreeMouseListener(
    private val jTree: JTree, configsTab: ConfigsTab, utilities: Utilities
) : SelectionPopMenu(configsTab, utilities) {
  override fun mousePressed(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseEvent.BUTTON3) {
            if (jTree.getPathForLocation(mouseEvent.x, mouseEvent.y) != null) {
                val treePath = jTree.getPathForLocation(mouseEvent.x, mouseEvent.y)!!
                val treeNode = treePath.lastPathComponent as DefaultMutableTreeNode
                val fileNode = treeNode.userObject as FileNode
                val file = fileNode.file
                show(jTree, mouseEvent.x, mouseEvent.y, file)
            }
        }
    }
}