/* Copyright (C) 2023  Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.gui.window.configs.components.general

import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.DocumentListener

/**
 * Scrollable textarea.
 *
 * @author Griefed
 */
class ScrollTextArea(
    text: String,
    private val textArea: JTextArea = JTextArea(text),
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_AS_NEEDED,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : JScrollPane(verticalScrollbarVisibility, horizontalScrollbarVisibility) {

    constructor(text: String, documentChangeListener: DocumentChangeListener) : this(text) {
        addDocumentListener(documentChangeListener)
    }

    constructor(text: List<String>, documentChangeListener: DocumentChangeListener) : this(text.joinToString(",")) {
        addDocumentListener(documentChangeListener)
    }

    init {
        textArea.lineWrap = true
        viewport.view = textArea
    }

    var text: String
        get() {
            return textArea.text
        }
        set(value) {
            textArea.text = value
        }

    fun addDocumentListener(listener: DocumentListener) {
        textArea.document.addDocumentListener(listener)
    }
}