package de.griefed.serverpackcreator.gui.filebrowser.controller.action

import Gui
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Desktop
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import javax.swing.AbstractAction

class OpenAction : AbstractAction() {
    private val log = cachedLoggerOf(this.javaClass)
    private var file: File? = null

    init {
        putValue(NAME, Gui.filebrowser_action_open.toString())
    }

    override fun actionPerformed(e: ActionEvent) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(file)
                } catch (ex: IOException) {
                    log.error("Couldn't open $file.")
                }
            }
        }
    }

    fun setFile(file: File?) {
        this.file = file
    }
}