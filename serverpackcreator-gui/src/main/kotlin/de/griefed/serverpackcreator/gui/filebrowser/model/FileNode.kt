package de.griefed.serverpackcreator.gui.filebrowser.model

import java.io.File

/**
 * TODO docs
 */
//TODO extend file?
class FileNode(val file: File) {
    var isGenerateGrandchildren = true

    /**
     * TODO docs
     */
    override fun toString(): String {
        val name = file.name
        return if (name == "") {
            file.absolutePath
        } else {
            name
        }
    }
}