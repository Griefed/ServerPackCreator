package de.griefed.serverpackcreator.app.gui.window.settings.components

import Translations
import com.formdev.flatlaf.util.SystemFileChooser
import java.io.File

class WritableDirectoryFilter  : SystemFileChooser.FileFilter() {
    fun accept(file: File): Boolean {
        val files = file.walk().filter { it.isDirectory }
        return file.isDirectory && (file.canWrite() || files.any { it.canWrite() })
    }

    override fun getDescription(): String {
        return Translations.settings_directory_filter.toString()
    }
}