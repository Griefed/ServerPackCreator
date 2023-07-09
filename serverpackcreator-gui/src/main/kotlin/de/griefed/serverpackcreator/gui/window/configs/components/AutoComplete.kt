package de.griefed.serverpackcreator.gui.window.configs.components

import de.comahe.i18n4k.Locale
import de.griefed.serverpackcreator.api.ApiProperties
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException


class AutoComplete(private val textField: JTextField, apiProperties: ApiProperties, identifier: String, var mode: Mode = Mode.COMPLETION) :
    DocumentListener {

    private val keywords = TreeSet<String>()

    init {
        val property = apiProperties.retrieveCustomProperty("autocomplete.$identifier").toString()
        val entries = mutableListOf<String>()
        if (property.contains(",")) {
            entries.addAll(property.split(","))
        } else {
            entries.add(property)
        }
        keywords.addAll(entries)
    }

    override fun changedUpdate(ev: DocumentEvent?) {}
    override fun removeUpdate(ev: DocumentEvent?) {}
    override fun insertUpdate(ev: DocumentEvent) {
        if (ev.length != 1) return
        val pos: Int = ev.offset
        var content: String? = null
        try {
            content = textField.getText(0, pos + 1)
        } catch (e: BadLocationException) {
            e.printStackTrace()
        }

        // Find where the word starts
        var w: Int = pos
        while (w >= 0) {
            if (!Character.isLetter(content!![w])) {
                break
            }
            w--
        }

        // Too few chars
        if (pos - w < 2) return
        val prefix = content!!.substring(w + 1).lowercase(Locale.getDefault())
        val words = keywords.toList()
        val toSearch = keywords.toList().map { entry -> entry.lowercase(Locale.getDefault()) }
        val n = Collections.binarySearch(toSearch, prefix)
        if (n < 0 && -n <= words.size) {
            val match = toSearch[-n - 1]
            val forCompletion = words[-n - 1]
            if (match.startsWith(prefix)) {
                // A completion is found
                val completion = forCompletion.substring(pos - w)
                // We cannot modify Document from within notification,
                // so we submit a task that does the change later
                SwingUtilities.invokeLater(CompletionTask(completion, pos + 1))
            }
        } else {
            // Nothing found
            mode = Mode.INSERT
        }
    }

    inner class CommitAction : AbstractAction() {
        override fun actionPerformed(ev: ActionEvent?) {
            if (mode == Mode.COMPLETION) {
                val pos = textField.selectionEnd
                val sb = StringBuffer(textField.text)
                sb.insert(pos, " ")
                textField.text = sb.toString()
                textField.caretPosition = pos + 1
                mode = Mode.INSERT
            } else {
                textField.replaceSelection("\t")
            }
        }

        private val serialVersionUID = 5794543109646743416L
    }

    private inner class CompletionTask(private val completion: String, private val position: Int) : Runnable {
        override fun run() {
            val sb = StringBuffer(textField.text)
            sb.insert(position, completion)
            textField.text = sb.toString()
            textField.caretPosition = position + completion.length
            textField.moveCaretPosition(position)
            mode = Mode.COMPLETION
        }
    }

    enum class Mode {
        INSERT, COMPLETION
    }
}