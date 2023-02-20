package de.griefed.serverpackcreator.gui.window.configs.components

import javax.swing.JTextField

open class ListeningTextField(text: String) : JTextField(text) {
    constructor(text: String, documentChangeListener: DocumentChangeListener) : this(text) {
        document.addDocumentListener(documentChangeListener)
    }
}