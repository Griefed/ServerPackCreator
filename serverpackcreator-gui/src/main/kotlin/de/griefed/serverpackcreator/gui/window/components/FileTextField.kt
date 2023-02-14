package de.griefed.serverpackcreator.gui.window.components

import java.io.File
import javax.swing.JTextField

class FileTextField(text: String, editable: Boolean = false) : JTextField(text) {
    constructor(text: File) : this(text.absolutePath)
    var file: File
        get() {
            return File(text).absoluteFile
        }
        set(value) {
            text = value.absolutePath
        }

    init {
        isEditable = editable
        file = File(text).absoluteFile
    }
}