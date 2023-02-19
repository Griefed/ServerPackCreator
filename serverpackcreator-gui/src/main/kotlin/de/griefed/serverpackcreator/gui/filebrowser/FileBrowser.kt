package de.griefed.serverpackcreator.gui.filebrowser

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing

/**
 * TODO docs
 */
@OptIn(DelicateCoroutinesApi::class)
class FileBrowser(private val configsTab: ConfigsTab, guiProps: GuiProps,utilities: Utilities) {
    private val browserModel: FileBrowserModel = FileBrowserModel(guiProps)
    private lateinit var frame: FileBrowserFrame

    init {
        GlobalScope.launch(Dispatchers.Default) {
            frame = FileBrowserFrame(browserModel, configsTab, guiProps,utilities)
        }
    }

    /**
     * TODO docs
     */
    fun reload() {
        browserModel.reload()
    }

    /**
     * TODO docs
     */
    fun show() {
        frame.show()
    }

    /**
     * TODO docs
     */
    fun hide() {
        frame.hide()
    }
}