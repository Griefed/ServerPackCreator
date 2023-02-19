package de.griefed.serverpackcreator.gui.window.configs.components

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * TODO docs
 */
interface DocumentChangeListener : DocumentListener {

    /**
     * TODO docs
     */
    fun update(e: DocumentEvent)

    override fun insertUpdate(e: DocumentEvent) {
        update(e)
    }

    override fun removeUpdate(e: DocumentEvent) {
        update(e)
    }

    override fun changedUpdate(e: DocumentEvent) {
        update(e)
    }
}