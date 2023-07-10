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

import de.griefed.serverpackcreator.api.ApiProperties
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import java.util.stream.Collectors
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import javax.swing.text.Utilities

class SuggestionProvider(
    private val sourceComponent: JTextComponent,
    private val apiProperties: ApiProperties,
    private val identifier: String
) {
    private val log = cachedLoggerOf(this.javaClass)
    private val suggestionMenu = JPopupMenu()
    private var suggestionListModel = DefaultListModel<String>()
    private var suggestionList = JList(suggestionListModel)
    private var disableTextEvent = false
    private val keyAdapter = object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_ENTER -> {
                    if (suggestionMenu.isVisible) {
                        val selectedIndex = suggestionList.selectedIndex
                        if (selectedIndex != -1) {
                            suggestionMenu.isVisible = false
                            val selectedValue = suggestionList.selectedValue
                            disableTextEvent = true
                            setSelectedText(sourceComponent, selectedValue)
                            disableTextEvent = false
                            e.consume()
                        }
                    }
                }

                KeyEvent.VK_UP -> {
                    if (suggestionMenu.isVisible && suggestionListModel.size > 0) {
                        val selectedIndex = suggestionList.selectedIndex
                        if (selectedIndex > 0) {
                            suggestionList.selectedIndex = selectedIndex - 1
                            e.consume()
                        }
                    }
                }

                KeyEvent.VK_DOWN -> {
                    if (suggestionMenu.isVisible && suggestionListModel.size > 0) {
                        val selectedIndex = suggestionList.selectedIndex
                        if (selectedIndex < suggestionListModel.size) {
                            suggestionList.selectedIndex = selectedIndex + 1
                            e.consume()
                        }
                    }
                }

                KeyEvent.VK_ESCAPE -> {
                    suggestionMenu.isVisible = false
                }
            }
        }
    }
    private val documentListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            if (disableTextEvent) {
                return
            }
            SwingUtilities.invokeLater {
                val suggestions = getSuggestions(sourceComponent)
                if (suggestions.isNotEmpty()) {
                    showPopup(suggestions)
                } else {
                    suggestionMenu.isVisible = false
                }
            }
        }
    }

    init {
        suggestionList.isFocusable = false
        suggestionMenu.isFocusable = false
        suggestionMenu.add(suggestionList)
        sourceComponent.document.addDocumentListener(documentListener)
        sourceComponent.addKeyListener(keyAdapter)
    }

    private fun showPopup(suggestions: List<String>) {
        suggestionListModel.clear()
        suggestionListModel.addAll(suggestions)
        val location = getPopupLocation(sourceComponent) ?: return
        suggestionMenu.pack()
        suggestionList.updateUI()
        suggestionMenu.updateUI()
        suggestionList.selectedIndex = 0
        suggestionMenu.show(sourceComponent, location.getX().toInt(), location.getY().toInt())
    }

    private fun getPopupLocation(invoker: JTextComponent): Point? {
        val caretPosition = invoker.caretPosition
        try {
            val rectangle2D = invoker.modelToView2D(caretPosition)
            val yPos = (rectangle2D.y + rectangle2D.height).toInt()
            return Point(rectangle2D.x.toInt(), yPos)
        } catch (ex: BadLocationException) {
            log.error("Bad location", ex)
        }
        return null
    }

    private fun setSelectedText(invoker: JTextComponent, selectedValue: String) {
        val textAt = invoker.caretPosition
        try {
            val trimmed = invoker.getText(textAt - 1, 1).trim { it <= ' ' }
            if (textAt == 0 || trimmed.isEmpty()) {
                invoker.document.insertString(textAt, selectedValue, null)
            } else {
                val previousWordIndex = Utilities.getPreviousWord(invoker, textAt)
                val text = invoker.getText(previousWordIndex, textAt - previousWordIndex)
                if (selectedValue.startsWith(text)) {
                    invoker.document.insertString(textAt, selectedValue.substring(text.length), null)
                } else {
                    invoker.document.insertString(textAt, selectedValue, null)
                }
            }
        } catch (ex: BadLocationException) {
            log.error("Bad location", ex)
        }
    }

    private fun getSuggestions(invoker: JTextComponent): List<String> {
        try {
            val cp = invoker.caretPosition
            if (cp != 0) {
                val text = invoker.getText(cp - 1, 1)
                if (text.trim { it <= ' ' }.isEmpty()) {
                    return listOf()
                }
            }
            val previousWordIndex = Utilities.getPreviousWord(invoker, cp)
            val text = try {
                if (invoker.getText(previousWordIndex - 1, 1).matches("\\W".toRegex())) {
                    invoker.getText(previousWordIndex - 1, cp - previousWordIndex + 1)
                } else {
                    invoker.getText(previousWordIndex, cp - previousWordIndex)
                }
            } catch (other: BadLocationException) {
                invoker.getText(previousWordIndex, cp - previousWordIndex)
            }
            return truncatedSuggestions(text.trim { it <= ' ' })
        } catch (_: BadLocationException) {
        }
        return listOf()
    }

    private fun truncatedSuggestions(input: String): List<String> {
        val entries = allSuggestions()
        return entries.stream().filter { s -> s.startsWith(input, ignoreCase = true) }.limit(20)
            .collect(Collectors.toList())
    }

    fun allSuggestions(): TreeSet<String> {
        val property = apiProperties.retrieveCustomProperty("autocomplete.$identifier").toString().trim { it <= ' ' }
        val entries = TreeSet<String>()
        if (property == "null") {
            return entries
        }
        if (property.contains(",")) {
            entries.addAll(property.split(","))
        } else {
            entries.add(property)
        }
        return entries
    }
}