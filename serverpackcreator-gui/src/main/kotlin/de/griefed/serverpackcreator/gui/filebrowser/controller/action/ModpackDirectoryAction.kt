package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import Gui
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

class ModpackDirectoryAction(private val configsTab: ConfigsTab) : AbstractAction() {
    private var directory: File? = null

    init {
        putValue(NAME, Gui.filebrowser_action_modpack.toString())
    }

    override fun actionPerformed(e: ActionEvent) {
        configsTab.selectedEditor?.setModpackDirectory(directory!!.absolutePath)
        configsTab.selectedEditor?.updateGuiFromSelectedModpack()
    }

    fun setDirectory(file: File?) {
        directory = file
    }
}