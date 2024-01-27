package de.griefed.serverpackcreator.gui.window.settings.components

import Translations
import java.io.File

class WritableDirectoryFilter  : javax.swing.filechooser.FileFilter() {
    override fun accept(file: File): Boolean {
        val files = file.walk().filter { it.isDirectory }
        return file.isDirectory && (file.canWrite() || files.any { it.canWrite() })
    }

    override fun getDescription(): String {
        return Translations.settings_directory_filter.toString()
    }
}