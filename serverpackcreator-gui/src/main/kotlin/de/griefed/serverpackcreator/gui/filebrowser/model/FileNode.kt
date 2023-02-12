package de.griefed.serverpackcreator.gui.filebrowser.model

import java.io.File

class FileNode(val file: File) {
    var isGenerateGrandchildren = true

    override fun toString(): String {
        val name = file.name
        return if (name == "") {
            file.absolutePath
        } else {
            name
        }
    }
}