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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.DocumentChangeListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import java.util.stream.Collectors
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPopupMenu
import javax.swing.event.DocumentEvent
import javax.swing.text.BadLocationException
import javax.swing.text.JTextComponent
import javax.swing.text.Utilities

/**
 * Suggestion provider which will open a dropdown menu in textfield and textareas, from which the user may select any entry
 * and apply it via pressing ENTER.
 *
 * Suggestions are acquired from ServerPackCreators properties, using the [identifier] with which this provider was instantiated
 * with.
 *
 * This feature is inspired by [Java Swing - Dropdown Text Suggestion Component Example](https://www.logicbig.com/tutorials/java-swing/text-suggestion-component.html)
 *
 * @author Griefed
 */
class SuggestionProvider(
    private val guiProps: GuiProps,
    private val sourceComponent: JTextComponent,
    private val identifier: String
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val suggestionMenu = JPopupMenu()
    private var suggestionListModel = DefaultListModel<String>()
    private var suggestionList = JList(suggestionListModel)
    private var disableTextEvent = false
    private val keyAdapter = object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_ENTER -> {
                    if (suggestionMenu.isVisible) {
                        if (suggestionList.selectedIndex != -1) {
                            suggestionMenu.isVisible = false
                            disableTextEvent = true
                            setSelectedText(sourceComponent, suggestionList.selectedValue)
                            disableTextEvent = false
                            e.consume()
                        }
                    }
                }

                KeyEvent.VK_UP -> {
                    if (suggestionMenu.isVisible && suggestionListModel.size > 0) {
                        if (suggestionList.selectedIndex > 0) {
                            suggestionList.selectedIndex--
                            e.consume()
                        }
                    }
                }

                KeyEvent.VK_DOWN -> {
                    if (suggestionMenu.isVisible && suggestionListModel.size > 0) {
                        if (suggestionList.selectedIndex < suggestionListModel.size) {
                            suggestionList.selectedIndex++
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

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private val documentListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            if (disableTextEvent) {
                return
            }
            if (!sourceComponent.isFocusOwner) {
                return
            }
            GlobalScope.launch(Dispatchers.Swing) {
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

    /**
     * @author Griefed
     * @author LogicBig (https://www.logicbig.com/)
     */
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

    /**
     * @author Griefed
     * @author LogicBig (https://www.logicbig.com/)
     */
    private fun getPopupLocation(component: JTextComponent): Point? {
        val caretPosition = component.caretPosition
        try {
            val rectangle2D = component.modelToView2D(caretPosition)
            val yPos = (rectangle2D.y + rectangle2D.height).toInt()
            return Point(rectangle2D.x.toInt(), yPos)
        } catch (ex: BadLocationException) {
            log.error("Bad location", ex)
        }
        return null
    }

    /**
     * @author Griefed
     * @author LogicBig (https://www.logicbig.com/)
     */
    private fun setSelectedText(component: JTextComponent, selectedValue: String) {
        val textAt = component.caretPosition
        try {
            val trimmed = component.getText(textAt - 1, 1).trim { it <= ' ' }
            if (textAt == 0 || trimmed.isEmpty()) {
                component.document.insertString(textAt, selectedValue, null)
            } else {
                val previousWordIndex = Utilities.getPreviousWord(component, textAt)
                val text = component.getText(previousWordIndex, textAt - previousWordIndex)
                if (selectedValue.startsWith(text)) {
                    component.document.insertString(textAt, selectedValue.substring(text.length), null)
                } else {
                    component.document.insertString(textAt, selectedValue, null)
                }
            }
        } catch (ex: BadLocationException) {
            log.error("Bad location", ex)
        }
    }

    /**
     * @author Griefed
     * @author LogicBig (https://www.logicbig.com/)
     */
    private fun getSuggestions(component: JTextComponent): List<String> {
        try {
            val cp = component.caretPosition
            if (cp != 0) {
                val text = component.getText(cp - 1, 1)
                if (text.trim { it <= ' ' }.isEmpty()) {
                    return listOf()
                }
            }
            val previousWordIndex = Utilities.getPreviousWord(component, cp)
            val text = try {
                if (component.getText(previousWordIndex - 1, 1).matches("\\W".toRegex())) {
                    component.getText(previousWordIndex - 1, cp - previousWordIndex + 1)
                } else {
                    component.getText(previousWordIndex, cp - previousWordIndex)
                }
            } catch (other: BadLocationException) {
                component.getText(previousWordIndex, cp - previousWordIndex)
            }
            return truncatedSuggestions(text.trim { it <= ' ' })
        } catch (_: BadLocationException) {
        }
        return listOf()
    }

    /**
     * @author Griefed
     */
    private fun truncatedSuggestions(text: String): List<String> {
        val entries = allSuggestions()
        val truncated = entries.filter { entry -> entry.startsWith(text, ignoreCase = true) }
        return if (truncated.size == 1 && truncated[0] == text) {
            listOf()
        } else {
            truncated.stream().limit(guiProps.getGuiProperty("autocomplete.limit")?.toLong() ?: 10)
                .collect(Collectors.toList())
        }
    }

    /**
     * @author Griefed
     */
    fun allSuggestions(): TreeSet<String> {
        val property = guiProps.getGuiProperty("autocomplete.$identifier").toString().trim { it <= ' ' }
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