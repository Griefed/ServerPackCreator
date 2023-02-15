package de.griefed.serverpackcreator.gui.filebrowser

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing

@OptIn(DelicateCoroutinesApi::class)
class FileBrowser(private val configsTab: ConfigsTab, guiProps: GuiProps) {
    private val browserModel: FileBrowserModel = FileBrowserModel()
    private lateinit var frame: FileBrowserFrame

    init {
        GlobalScope.launch(Dispatchers.Default) {
            frame = FileBrowserFrame(browserModel, configsTab, guiProps)
        }
    }

    fun reload() {
        browserModel.reload()
    }

    fun show() {
        frame.show()
    }

    fun hide() {
        frame.hide()
    }
}