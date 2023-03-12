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

import Gui
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Frame housing the filebrowser allowing users to update their modpack-directory, server-icon and server-properties
 * in a very convenient way.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
class FileBrowserFrame(
    private val browserModel: FileBrowserModel,
    configsTab: ConfigsTab,
    guiProps: GuiProps,
    utilities: Utilities
) {
    private val filePreviewPanel: FilePreviewPanel
    private val fileDetailPanel: FileDetailPanel
    private val frame: JFrame = JFrame()
    private val splitTreeTable: JSplitPane
    private val tableScrollPane: TableScrollPane
    private val treeScrollPane: TreeScrollPane
    private val tablePreviewSplit: JSplitPane
    private val northPanel = JPanel()
    private val southPanel = JPanel()

    init {
        frame.title = Gui.filebrowser.toString()
        frame.iconImage = guiProps.appIcon
        frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                frame.isVisible = false
            }
        })
        northPanel.layout = BorderLayout()
        tableScrollPane = TableScrollPane(this, browserModel, configsTab, utilities)
        tableScrollPane.panel.let {
            northPanel.add(
                it,
                BorderLayout.CENTER
            )
        }
        southPanel.layout = BorderLayout()
        fileDetailPanel = FileDetailPanel()
        fileDetailPanel.panel.let { southPanel.add(it, BorderLayout.PAGE_START) }
        filePreviewPanel = FilePreviewPanel(guiProps)
        southPanel.add(
            filePreviewPanel.panel,
            BorderLayout.CENTER
        )

        tablePreviewSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, northPanel, southPanel)
        tablePreviewSplit.isOneTouchExpandable = true
        tablePreviewSplit.dividerLocation = 200
        tablePreviewSplit.dividerSize = 20

        treeScrollPane = TreeScrollPane(this, browserModel, configsTab, utilities, guiProps)
        splitTreeTable = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            treeScrollPane.scrollPane,
            tablePreviewSplit
        )
        splitTreeTable.isOneTouchExpandable = true
        splitTreeTable.dividerLocation = 300
        splitTreeTable.dividerSize = 20
        frame.add(splitTreeTable)
        frame.pack()
        frame.isLocationByPlatform = true
        frame.isAutoRequestFocus = true
    }

    /**
     * Update the node of the detail-panel.
     *
     * @author Griefed
     */
    fun updateFileDetail(fileNode: FileNode?) {
        fileDetailPanel.setFileNode(fileNode, browserModel)
    }

    /**
     * Set the filemodel of the table-of-contents table.
     *
     * @author Griefed
     */
    fun setDefaultTableModel(node: DefaultMutableTreeNode) {
        tableScrollPane.setDefaultTableModel(node)
    }

    /**
     * Clear the table-of-contents for a directory.
     *
     * @author Griefed
     */
    fun clearDefaultTableModel() {
        tableScrollPane.clearDefaultTableModel()
    }

    /**
     * Update the filenode of the filepreview-panel.
     *
     * @author Griefed
     */
    fun setFilePreviewNode(fileNode: FileNode?) {
        fileNode?.let { filePreviewPanel.setFileNode(it) }
    }

    /**
     * Make the filebrowser visible.
     *
     * @author Griefed
     */
    fun show() {
        if (frame.isVisible) {
            frame.toFront()
        } else {
            frame.isVisible = true
            frame.toFront()
        }
        treeScrollPane.expandPathsToModpackDir()
    }

    /**
     * Hide the filebrowser.
     *
     * @author Griefed
     */
    fun hide() {
        frame.isVisible = false
    }
}