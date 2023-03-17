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
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane

/**
 * Frame housing the filebrowser allowing users to update their modpack-directory, server-icon and server-properties
 * in a very convenient way.
 *
 * @author Andrew Thompson
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class FileBrowserFrame(
    private val browserModel: FileBrowserModel,
    private val tabbedConfigsTab: TabbedConfigsTab,
    private val guiProps: GuiProps,
    private val utilities: Utilities
) {
    private lateinit var frame: JFrame

    init {
        GlobalScope.launch(Dispatchers.Swing) {
            frame = async {
                val frame = JFrame()
                val filePreviewPanel = FilePreviewPanel(guiProps)
                val fileDetailPanel = FileDetailPanel()
                val tableScrollPane =
                    TableScrollPane(browserModel, tabbedConfigsTab, utilities, fileDetailPanel, filePreviewPanel)
                val northPanel = JPanel()
                val southPanel = JPanel()
                northPanel.layout = BorderLayout()

                frame.title = Gui.filebrowser.toString()
                frame.iconImage = guiProps.appIcon
                frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
                frame.addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(event: WindowEvent) {
                        frame.isVisible = false
                    }
                })

                northPanel.add(tableScrollPane.panel, BorderLayout.CENTER)
                southPanel.layout = BorderLayout()
                southPanel.add(fileDetailPanel, BorderLayout.PAGE_START)
                southPanel.add(filePreviewPanel, BorderLayout.CENTER)

                val tablePreviewSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, northPanel, southPanel)
                tablePreviewSplit.isOneTouchExpandable = true
                tablePreviewSplit.dividerLocation = 200
                tablePreviewSplit.dividerSize = 20

                val treeScrollPane = TreeScrollPane(
                    browserModel,
                    tabbedConfigsTab,
                    utilities,
                    fileDetailPanel,
                    filePreviewPanel,
                    tableScrollPane
                )
                val splitTreeTable = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, tablePreviewSplit)
                splitTreeTable.isOneTouchExpandable = true
                splitTreeTable.dividerLocation = 300
                splitTreeTable.dividerSize = 20
                frame.add(splitTreeTable)
                frame.pack()
                frame.isLocationByPlatform = true
                frame.isAutoRequestFocus = true
                return@async frame
            }.await()
        }

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