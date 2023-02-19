package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import Gui
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

/**
 * TODO docs
 */
class OpenContainingFolder(private val utilities: Utilities) : AbstractAction() {
    private lateinit var directory: File

    init {
        putValue(NAME, Gui.filebrowser_action_open_folder.toString())
    }

    /**
     * TODO docs
     */
    override fun actionPerformed(e: ActionEvent?) {
        utilities.fileUtilities.openFolder(directory)
    }

    /**
     * TODO docs
     */
    fun setDirectory(file: File) {
        directory = file.parentFile
    }
}