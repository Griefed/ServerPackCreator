package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import Gui
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

/**
 * TODO docs
 */
class ServerPropertiesAction(private val configsTab: ConfigsTab) : AbstractAction() {
    private var properties: File? = null

    init {
        putValue(NAME, Gui.filebrowser_action_properties.toString())
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    override fun actionPerformed(e: ActionEvent) {
        configsTab.selectedEditor?.setServerPropertiesPath(properties!!.absolutePath)
    }

    /**
     * TODO docs
     */
    fun setProperties(file: File?) {
        properties = file
    }
}