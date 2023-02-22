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
 * TODO docs
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
        tableScrollPane = TableScrollPane(this, browserModel, configsTab,utilities)
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

        treeScrollPane = TreeScrollPane(this, browserModel, configsTab,utilities,guiProps)
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
     * TODO docs
     */
    fun updateFileDetail(fileNode: FileNode?) {
        fileDetailPanel.setFileNode(fileNode, browserModel)
    }

    /**
     * TODO docs
     */
    fun setDefaultTableModel(node: DefaultMutableTreeNode) {
        tableScrollPane.setDefaultTableModel(node)
    }

    /**
     * TODO docs
     */
    fun clearDefaultTableModel() {
        tableScrollPane.clearDefaultTableModel()
    }

    /**
     * TODO docs
     */
    fun setFilePreviewNode(fileNode: FileNode?) {
        fileNode?.let { filePreviewPanel.setFileNode(it) }
    }

    /**
     * TODO docs
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
     * TODO docs
     */
    fun hide() {
        frame.isVisible = false
    }
}