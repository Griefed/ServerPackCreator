package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import Gui
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

/**
 * TODO docs
 */
class ServerIconAction(private val configsTab: ConfigsTab) : AbstractAction() {
    private var icon: File? = null

    init {
        putValue(NAME, Gui.filebrowser_action_icon.toString())
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Suppress("unused")
    override fun actionPerformed(e: ActionEvent) {
        configsTab.selectedEditor?.setServerIconPath(icon!!.absolutePath)
    }

    /**
     * TODO docs
     */
    fun setIcon(file: File?) {
        icon = file
    }
}