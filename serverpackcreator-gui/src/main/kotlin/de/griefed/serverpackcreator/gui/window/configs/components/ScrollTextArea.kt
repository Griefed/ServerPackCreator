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

package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.DocumentListener
import javax.swing.event.UndoableEditEvent
import javax.swing.event.UndoableEditListener
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException
import javax.swing.undo.UndoManager

/**
 * Scrollable textarea with an [UndoManager] providing up to ten undos. By default, the vertical scrollbar is
 * displayed as needed.
 *
 * @author Griefed
 */
class ScrollTextArea(
    text: String,
    private val textArea: JTextArea = JTextArea(text),
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_AS_NEEDED,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : JScrollPane(verticalScrollbarVisibility, horizontalScrollbarVisibility),
    UndoableEditListener,
    KeyListener {

    constructor(text: String, documentChangeListener: DocumentChangeListener) : this(text) {
        addDocumentListener(documentChangeListener)
    }

    private val undoManager = UndoManager()

    init {
        undoManager.limit = 10
        textArea.lineWrap = true
        textArea.document.addUndoableEditListener(this)
        textArea.addKeyListener(this)
        viewport.view = textArea
    }

    var text: String
        get() {
            return textArea.text
        }
        set(value) {
            textArea.text = value
        }

    @Suppress("MemberVisibilityCanBePrivate")
    fun addDocumentListener(listener: DocumentListener) {
        textArea.document.addDocumentListener(listener)
    }

    override fun undoableEditHappened(e: UndoableEditEvent) {
        undoManager.addEdit(e.edit)
    }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        when {
            e.keyCode == KeyEvent.VK_Z && e.isControlDown -> {
                try {
                    undoManager.undo()
                } catch (cue: CannotUndoException) {
                    Toolkit.getDefaultToolkit().beep()
                }
            }

            e.keyCode == KeyEvent.VK_Y && e.isControlDown -> {
                try {
                    undoManager.redo()
                } catch (cue: CannotRedoException) {
                    Toolkit.getDefaultToolkit().beep()
                }
            }
        }
    }

    override fun keyReleased(e: KeyEvent) {}
}