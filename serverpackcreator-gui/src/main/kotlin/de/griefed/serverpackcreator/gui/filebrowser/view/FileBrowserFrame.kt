package de.griefed.serverpackcreator.gui.filebrowser.view

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

class FileBrowserFrame(
    private val browserModel: FileBrowserModel,
    private val configsTab: ConfigsTab
) {
    private var filePreviewPanel: FilePreviewPanel
    private var fileDetailPanel: FileDetailPanel
    private var frame: JFrame = JFrame()
    private var splitTreeTable: JSplitPane
    private var tableScrollPane: TableScrollPane

    init {
        frame.title = "File Browser"
        frame.defaultCloseOperation = JFrame.HIDE_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                frame.isVisible = false
            }
        })
        val northPanel = JPanel()
        northPanel.layout = BorderLayout()
        tableScrollPane = TableScrollPane(this, browserModel, configsTab)
        tableScrollPane.panel.let {
            northPanel.add(
                it,
                BorderLayout.CENTER
            )
        }
        val southPanel = JPanel()
        southPanel.layout = BorderLayout()
        fileDetailPanel = FileDetailPanel()
        fileDetailPanel.panel.let { southPanel.add(it, BorderLayout.PAGE_START) }
        filePreviewPanel = FilePreviewPanel()
        southPanel.add(
            filePreviewPanel.panel,
            BorderLayout.CENTER
        )

        val tablePreviewSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT,northPanel,southPanel)
        tablePreviewSplit.isOneTouchExpandable = true
        tablePreviewSplit.dividerLocation = 200
        tablePreviewSplit.dividerSize = 20

        val treeScrollPane = TreeScrollPane(this, browserModel, configsTab)
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

    fun updateFileDetail(fileNode: FileNode?) {
        fileDetailPanel.setFileNode(fileNode, browserModel)
    }

    fun setDefaultTableModel(node: DefaultMutableTreeNode) {
        tableScrollPane.setDefaultTableModel(node)
    }

    fun clearDefaultTableModel() {
        tableScrollPane.clearDefaultTableModel()
    }

    fun setDesktopButtonFileNode(fileNode: FileNode?) {
        fileNode?.let { filePreviewPanel.setFileNode(it) }
    }

    fun show() {
        if (frame.isVisible) {
            frame.toFront()
        } else {
            frame.isVisible = true
            frame.toFront()
        }
    }

    fun hide() {
        frame.isVisible = false
    }
}