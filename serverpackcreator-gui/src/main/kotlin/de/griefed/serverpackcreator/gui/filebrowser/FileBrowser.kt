package de.griefed.serverpackcreator.gui.filebrowser

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab

class FileBrowser(private val configsTab: ConfigsTab) : Runnable {
    private val browserModel: FileBrowserModel = FileBrowserModel()
    private lateinit var frame: FileBrowserFrame

    fun reload() {
        browserModel.reload()
    }

    fun show() {
        frame.show()
    }

    fun hide() {
        frame.hide()
    }

    override fun run() {
        frame = FileBrowserFrame(browserModel, configsTab)
    }
}