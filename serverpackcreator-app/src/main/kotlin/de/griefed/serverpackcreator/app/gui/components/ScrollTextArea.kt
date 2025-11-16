/* Copyright (C) 2025 Griefed
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

import Translations
import de.griefed.serverpackcreator.api.utilities.common.regexReplace
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.configs.components.ResizeIndicatorScrollPane
import de.griefed.serverpackcreator.app.gui.window.configs.components.SuggestionProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*
import javax.swing.event.DocumentListener
import javax.swing.event.UndoableEditEvent
import javax.swing.event.UndoableEditListener
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
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
    areaName: String,
    private val guiProps: GuiProps,
    private val textArea: JTextArea = JTextArea(text),
    verticalScrollbarVisibility: Int = VERTICAL_SCROLLBAR_ALWAYS,
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_NEVER
) : ResizeIndicatorScrollPane(guiProps, textArea, verticalScrollbarVisibility, horizontalScrollbarVisibility),
    UndoableEditListener,
    KeyListener {

    constructor(
        text: String,
        areaName: String,
        documentChangeListener: DocumentChangeListener,
        guiProps: GuiProps
    ) : this(text, areaName, guiProps) {
        this.addDocumentListener(documentChangeListener)
    }

    private val undoManager = UndoManager()
    private val searchFor = JTextField(100)
    private val replaceWith = JTextField(100)
    private val search = arrayOf<Any>(
        Translations.createserverpack_gui_textarea_search_message.toString(),
        searchFor
    )
    private val searchRegex = arrayOf<Any>(
        Translations.createserverpack_gui_textarea_search_regex_message.toString(),
        searchFor
    )
    private val searchAndReplace = arrayOf<Any>(
        Translations.createserverpack_gui_textarea_replace_query.toString(),
        searchFor,
        Translations.createserverpack_gui_textarea_replace_replace.toString(),
        replaceWith
    )
    private val searchAndReplaceRegex = arrayOf<Any>(
        Translations.createserverpack_gui_textarea_replace_regex_query.toString(),
        searchFor,
        Translations.createserverpack_gui_textarea_replace_regex_replace.toString(),
        replaceWith
    )
    val suggestionProvider: SuggestionProvider?
    val identifier: String

    init {
        undoManager.limit = 10
        textArea.lineWrap = true
        textArea.document.addUndoableEditListener(this)
        textArea.addKeyListener(this)

        viewport.view = textArea
        name = areaName
        identifier = name.lowercase().replace(" ", "")
        suggestionProvider = if (identifier.isNotBlank()) {
            SuggestionProvider(guiProps, textArea, identifier)
        } else {
            null
        }
    }

    var text: String
        get() {
            return textArea.text
        }
        set(value) {
            textArea.text = value
        }

    fun append(text: String) {
        textArea.append(text)
    }

    /**
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun addDocumentListener(listener: DocumentListener) {
        textArea.document.addDocumentListener(listener)
    }

    override fun undoableEditHappened(e: UndoableEditEvent) {
        undoManager.addEdit(e.edit)
    }

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        textArea.highlighter.removeAllHighlights()
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

            e.keyCode == KeyEvent.VK_F && e.isControlDown && !e.isShiftDown -> searchDialog()

            e.keyCode == KeyEvent.VK_F && e.isControlDown && e.isShiftDown -> searchRegexDialog()

            e.keyCode == KeyEvent.VK_R && e.isControlDown && !e.isShiftDown -> searchAndReplace()

            e.keyCode == KeyEvent.VK_R && e.isControlDown && e.isShiftDown -> searchRegexAndReplace()
        }
    }

    override fun keyReleased(e: KeyEvent) {}

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun requestFocus(component: JComponent) {
        GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
            delay(250)
            component.requestFocus()
            component.grabFocus()
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun searchDialog() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                search,
                Translations.createserverpack_gui_textarea_search_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            textArea.isEnabled = false
            GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
                var i = 0
                while (i < text.length) {
                    val end = i + searchFor.text.length
                    if (end <= text.length && text.substring(i, end).equals(searchFor.text, true)) {
                        textArea.highlighter.addHighlight(
                            i,
                            end,
                            DefaultHighlightPainter(UIManager.getColor("textHighlight"))
                        )
                        i = end
                    } else {
                        i++
                    }
                }
                textArea.isEnabled = true
            }
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun searchRegexDialog() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                searchRegex,
                Translations.createserverpack_gui_textarea_search_regex_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            textArea.isEnabled = false
            GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
                val regex = searchFor.text.toRegex()
                var i = 0
                while (i < text.length) {
                    for (n in text.length downTo i) {
                        if (text.substring(i, n).matches(regex)) {
                            textArea.highlighter.addHighlight(
                                i,
                                n,
                                DefaultHighlightPainter(UIManager.getColor("textHighlight"))
                            )
                            i = n
                            break
                        }
                    }
                    i++
                }
                textArea.isEnabled = true
            }
        }

    }

    /**
     * @author Griefed
     */
    private fun searchAndReplace() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                searchAndReplace,
                Translations.createserverpack_gui_textarea_replace_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            text = text.replace(searchFor.text, replaceWith.text, false)
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun searchRegexAndReplace() {
        requestFocus(searchFor)
        if (JOptionPane.showConfirmDialog(
                parent,
                searchAndReplaceRegex,
                Translations.createserverpack_gui_textarea_replace_regex_title(name),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                guiProps.inspectMediumIcon
            ) == JOptionPane.OK_OPTION
        ) {
            textArea.isEnabled = false
            GlobalScope.launch(Dispatchers.Swing) {
                text = text.regexReplace(searchFor.text.toRegex(), replaceWith.text)
                textArea.isEnabled = true
            }
        }
    }
}