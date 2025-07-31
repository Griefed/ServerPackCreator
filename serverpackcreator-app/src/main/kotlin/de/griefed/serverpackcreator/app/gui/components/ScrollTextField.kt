/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.configs.components.SuggestionProvider
import java.awt.Toolkit
import java.awt.dnd.DropTarget
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.BorderFactory
import javax.swing.DropMode
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.event.DocumentListener
import javax.swing.event.UndoableEditEvent
import javax.swing.event.UndoableEditListener
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException
import javax.swing.undo.UndoManager


/**
 * Scrollable textfield with an [UndoManager] providing up to ten undos. By default, the horizontal scrollbar is
 * displayed as needed.
 *
 * @author Griefed
 */
open class ScrollTextField(
    guiProps: GuiProps,
    text: String,
    val identifier: String? = null,
    private val textField: JTextField = JTextField(text),
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_AS_NEEDED
) : JScrollPane(VERTICAL_SCROLLBAR_NEVER, horizontalScrollbarVisibility),
    UndoableEditListener,
    KeyListener {

    constructor(
        guiProps: GuiProps,
        text: String,
        identifier: String?,
        documentChangeListener: DocumentChangeListener
    ) : this(guiProps, text, identifier) {
        this.addDocumentListener(documentChangeListener)
    }

    private val undoManager = UndoManager()
    val suggestionProvider: SuggestionProvider?

    var isEditable: Boolean
        get() {
            return textField.isEditable
        }
        set(value) {
            textField.isEditable = value
        }
    var text: String
        get() {
            return textField.text
        }
        set(value) {
            textField.text = value
        }

    init {
        undoManager.limit = 10
        textField.text = text
        textField.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        textField.document.addUndoableEditListener(this)
        textField.addKeyListener(this)
        viewport.view = textField
        suggestionProvider = if (!identifier.isNullOrBlank()) {
            SuggestionProvider(guiProps, textField, identifier)
        } else {
            null
        }
    }

    override fun getDropTarget(): DropTarget {
        return textField.dropTarget
    }

    override fun setDropTarget(dropTarget: DropTarget) {
        textField.dropTarget = dropTarget
    }

    @Suppress("unused")
    fun setDropMode(mode: DropMode) {
        textField.dropMode = mode
    }

    @Suppress("unused")
    fun getDropMode(): DropMode {
        return textField.dropMode
    }

    /**
     * @author Griefed
     */
    fun highlight() {
        textField.requestFocusInWindow()
    }

    /**
     * @author Griefed
     */
    fun addDocumentListener(listener: DocumentListener) {
        textField.document.addDocumentListener(listener)
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