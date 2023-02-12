package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

class ServerPropertiesAction(private val configsTab: ConfigsTab) : AbstractAction() {
    private var properties: File? = null
    init {
        putValue(NAME, "Set as server.properties")
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    override fun actionPerformed(e: ActionEvent) {
        configsTab.selectedEditor?.setServerPropertiesPath(properties!!.absolutePath)
    }

    fun setProperties(file: File?) {
        properties = file
    }
}