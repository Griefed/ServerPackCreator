/* Copyright (C) 2026 Griefed
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
    /**
     * Names the autocomplete bucket this field's suggestions are stored under. `null` or blank means no
     * [suggestionProvider] is attached at all, which is how a field opts out of autocomplete.
     */
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
    /** The autocomplete popup, attached to the *wrapped* field so it can position itself at the caret. `null` when [identifier] named no bucket. */
    val suggestionProvider: SuggestionProvider?

    /** Forwards to the wrapped text field — a `JScrollPane` has no editability of its own. */
    var isEditable: Boolean
        get() {
            return textField.isEditable
        }
        set(value) {
            textField.isEditable = value
        }
    /** Forwards to the wrapped text field. Reading the scroll pane instead would yield nothing. */
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

    /**
     * The wrapped field's drop target, not the scroll pane's.
     * 
     * Overridden because Swing hands a drop to whichever component the mouse is over, and that is the scroll pane —
     * so without this, dropping a file on a path field does nothing.
     */
    override fun getDropTarget(): DropTarget {
        return textField.dropTarget
    }

    /** Sets the drop target on the wrapped field, for the reason [getDropTarget] describes. */
    override fun setDropTarget(dropTarget: DropTarget) {
        textField.dropTarget = dropTarget
    }

    /** Forwards the drop mode to the wrapped field. */
    @Suppress("unused")
    fun setDropMode(mode: DropMode) {
        textField.dropMode = mode
    }

    /** The wrapped field's drop mode. */
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

    /** Records the edit with this field's own undo manager, which is capped at ten steps. */
    override fun undoableEditHappened(e: UndoableEditEvent) {
        undoManager.addEdit(e.edit)
    }

    /** Unused; the shortcuts are handled on key-press. */
    override fun keyTyped(e: KeyEvent) {}

    /** Handles undo and redo, and lets everything else through to the field. */
    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
            e.keyCode if e.isControlDown -> {
                try {
                    undoManager.undo()
                } catch (cue: CannotUndoException) {
                    Toolkit.getDefaultToolkit().beep()
                }
            }
            e.keyCode if e.isControlDown -> {
                try {
                    undoManager.redo()
                } catch (cue: CannotRedoException) {
                    Toolkit.getDefaultToolkit().beep()
                }
            }
        }
    }

    /** Unused; see [keyPressed]. */
    override fun keyReleased(e: KeyEvent) {}
}