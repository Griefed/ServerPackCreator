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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller

import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.SortedTreeNode
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.runnable.AddNodes
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.ExpandVetoException

/**
 * Expansion-listener to update available nodes upon expansion, or set nodes invisible upon collapse.
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class TreeExpandListener(
    private val browserModel: FileBrowserModel
) : TreeWillExpandListener {

    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(event: TreeExpansionEvent) {
        val path = event.path
        val node = path.lastPathComponent as SortedTreeNode
        for (child in node.children()) {
            child as SortedTreeNode
            for (grandChild in child.children()) {
                grandChild as SortedTreeNode
                grandChild.removeAllChildren()
            }
            child.removeAllChildren()
        }
        node.removeAllChildren()
        browserModel.addChildNodes(node)
        browserModel.treeModel.nodeStructureChanged(node)
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillExpand(event: TreeExpansionEvent) {
        val path = event.path
        val node = path.lastPathComponent as SortedTreeNode
        AddNodes(browserModel, node)
    }
}