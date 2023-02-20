package de.griefed.serverpackcreator.gui.window.configs.components

import java.io.File
import javax.swing.JTextField

/**
 * TODO docs
 */
class FileTextField(text: String, editable: Boolean = false) : ListeningTextField(text) {
    constructor(text: File, documentChangeListener: DocumentChangeListener) : this(text.absolutePath) {
        document.addDocumentListener(documentChangeListener)
    }

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