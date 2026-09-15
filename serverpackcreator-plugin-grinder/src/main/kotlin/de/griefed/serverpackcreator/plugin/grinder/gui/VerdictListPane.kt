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
package de.griefed.serverpackcreator.plugin.grinder.gui

import de.griefed.serverpackcreator.plugin.grinder.core.GrinderVerdict
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.RowFilter
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableRowSorter

/**
 * One tickable list of verdicts: an optional warning banner, a search field and select-all buttons, and
 * the table itself.
 *
 * Both verdict tabs are this class with different rows and a different banner. They deliberately share
 * one [VerdictTableModel.selection] — the split into "proven" and "everything else" is a statement about
 * risk that this interface makes to the user, not a distinction a server pack generation observes.
 *
 * A `JTable` rather than a column of checkboxes because a mature grinder holds thousands of verdicts,
 * which is also why filtering goes through a [TableRowSorter] instead of rebuilding the model.
 *
 * @param warning A banner to show above the list, or `null` for none.
 * @param onSelectionChanged Called when the user ticks or unticks anything, with the new full selection.
 *
 * @author Griefed
 */
class VerdictListPane(
    warning: String? = null,
    onSelectionChanged: (Set<String>) -> Unit
) : JPanel(BorderLayout(0, 6)) {

    private val model = VerdictTableModel().apply { this.onSelectionChanged = onSelectionChanged }
    private val table = JTable(model)
    private val sorter = TableRowSorter(model)
    private val searchField = JTextField(24)
    private val summary = JLabel()

    init {
        table.rowSorter = sorter
        // Every text cell carries grinder-supplied text, and the default renderer would parse a value
        // beginning with <html> as markup. See PlainTextRendering.
        table.setDefaultRenderer(String::class.java, PlainTextRendering.tableCellRenderer())
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.fillsViewportHeight = true
        // The tick column holds a checkbox and nothing else, so it is sized to it rather than sharing
        // the table's width with columns that carry text.
        table.columnModel.getColumn(VerdictTableModel.TICK_COLUMN).apply {
            maxWidth = 34
            minWidth = 34
        }
        // Both evidence columns hold long, fixed vocabularies, and at an equal share of the table their
        // widest values clip: `SERVER_OR_BOTH` (1718 of 2057 rows on the live feed) and `CONTRADICTORY`
        // (161) -- the second being the exact value these two columns were added to surface, so rendering
        // it as `CONTRADICTO...` defeats the point of having them. Seen in the running GUI, not deduced.
        // Preferred widths only: both still shrink with the window, they just do not start clipped.
        table.columnModel.getColumn(VerdictTableModel.DECLARED_COLUMN).preferredWidth = 130
        table.columnModel.getColumn(VerdictTableModel.JAR_SIDENESS_COLUMN).preferredWidth = 140

        warning?.let { add(banner(it), BorderLayout.NORTH) }
        add(JScrollPane(table), BorderLayout.CENTER)
        add(controls(), BorderLayout.SOUTH)

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = applyFilter()
            override fun removeUpdate(event: DocumentEvent) = applyFilter()
            override fun changedUpdate(event: DocumentEvent) = applyFilter()
        })
    }

    /** Replace the displayed verdicts. The selection survives, so a refresh never unticks anything. */
    fun setVerdicts(verdicts: List<GrinderVerdict>) {
        model.setRows(verdicts)
        updateSummary()
    }

    /**
     * The entries this pane currently displays, whether ticked or not.
     *
     * The two panes share one selection set, so this is how a tick is attributed back to the list it was
     * made in when the selection is written to the configuration.
     */
    fun shownEntries(): Set<String> =
        (0 until model.rowCount).mapNotNullTo(HashSet()) { model.rowAt(it).exclusionEntry }

    /** Show [selected] as ticked, without treating it as a user edit. Used when a saved config loads. */
    fun showSelection(selected: Set<String>) {
        model.selection = selected
        updateSummary()
    }

    /** The warning shown above the at-your-own-risk list, styled to be read rather than skimmed past. */
    private fun banner(text: String) = JLabel("<html><b>$text</b></html>").apply {
        isOpaque = true
        // A literal colour rather than a UIManager key: this must stay a warning under every look and
        // feel SPC ships, including the dark themes, where a themed "info" background reads as ordinary.
        background = Color(0xFF, 0xE0, 0x8A)
        foreground = Color(0x40, 0x2A, 0x00)
        border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
    }

    /**
     * The search field, the two bulk buttons and the count, in one row under the table.
     *
     * The listeners call [bulk] rather than the model directly: inside a `JButton.apply { }` the name
     * `model` resolves to the *button's* own `ButtonModel`, not this pane's table model.
     */
    private fun controls() = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
        add(JLabel("Filter:"))
        add(searchField)
        add(JButton("Select all").apply { addActionListener { bulk(select = true) } })
        add(JButton("Deselect all").apply { addActionListener { bulk(select = false) } })
        add(summary)
    }

    /** Tick or untick every row this pane shows, then refresh the count. */
    private fun bulk(select: Boolean) {
        if (select) model.selectAll() else model.deselectAll()
        updateSummary()
    }

    /**
     * Filter the visible rows on the typed text, case-insensitively across every column. `(?i)` plus
     * [java.util.regex.Pattern.quote] rather than a hand-built regex: the field is free text, and an
     * operator typing `c++` must get a search, not a `PatternSyntaxException`.
     */
    private fun applyFilter() {
        val term = searchField.text.orEmpty()
        sorter.rowFilter =
            if (term.isBlank()) null else RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(term))
        updateSummary()
    }

    /**
     * "12 of 340 shown · 5 ticked" — the two numbers a user needs while filtering a long list.
     *
     * Counted as a set intersection rather than a nested scan. This runs on every keystroke in the filter
     * field, and the pane exists in its current form because the store runs to thousands of rows, so
     * `selection × rows` was a contradiction of the file's own reasoning.
     */
    private fun updateSummary() {
        val shown = shownEntries()
        val ticked = model.selection.count { it in shown }
        summary.text = "${table.rowCount} of ${model.rowCount} shown · $ticked ticked here"
    }
}
