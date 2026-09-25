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
package de.griefed.serverpackcreator.plugin.servertest.gui

import de.griefed.serverpackcreator.plugin.servertest.core.LaunchablePack
import java.io.File
import de.griefed.serverpackcreator.plugin.servertest.core.StartScript
import de.griefed.serverpackcreator.plugin.servertest.core.StartScripts
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel

/**
 * The list of server packs, and the Start button.
 *
 * Shows every pack in ServerPackCreator's server-packs directory, including the ones that cannot be launched
 * here — those carry their reason in the Status column rather than being dropped, because a pack the user can
 * see in their file manager and not in this list reads as the plugin having lost it.
 *
 * @param onStart   Called with the pack to launch when Start is pressed.
 * @param onRefresh Called when the user asks for the directory to be read again.
 * @author Griefed
 */
class PackListPane(
    availableScripts: List<StartScript>,
    private val onStart: (LaunchablePack) -> Unit,
    private val onRefresh: () -> Unit,
    private val onScriptChanged: () -> Unit = {}
) : JPanel(BorderLayout()) {

    /**
     * Which of the four start scripts a launch uses.
     *
     * The entries are **ServerPackCreator's own configured start-script templates**, so the list can neither
     * offer a script no generation produces nor omit one an operator added. Pre-selected for the host and
     * then left to the user: the plugin choosing on their behalf is the one failure with no workaround, as
     * a guess landing on a script their machine cannot run would leave them unable to start anything. Every
     * entry stays selectable for that reason, including ones this host has no interpreter for — such a
     * launch fails on the console with the interpreter's own error, which beats a disabled control.
     */
    private val scriptChoice = JComboBox(availableScripts.toTypedArray()).apply {
        selectedItem = StartScripts.defaultFor(availableScripts)
        isEnabled = availableScripts.isNotEmpty()
        toolTipText = "Which start script to run. Defaults to the one for this operating system."
        // The labels are this plugin's own literals, so markup is not a concern here as it is for pack
        // names — but the renderer is shared with nothing, and showing `label` rather than the enum's
        // name is why it exists.
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component = super.getListCellRendererComponent(
                list, (value as? StartScript)?.label ?: value, index, isSelected, cellHasFocus
            )
        }
    }

    /** The script the user has chosen, or `null` when ServerPackCreator has no templates configured. */
    val selectedScript: StartScript?
        get() = scriptChoice.selectedItem as? StartScript

    /** The rows. Owned here, replaced wholesale on every refresh. */
    val model = PackTableModel()

    private val table = JTable(model).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        autoCreateRowSorter = true
        // Pack names and manifest version strings are user-controlled, and Swing's default renderer parses
        // any value starting with <html> as markup whose image tags it will fetch.
        setDefaultRenderer(String::class.java, PlainTextRendering.tableCellRenderer())
    }

    private val startButton = JButton("Start selected server pack").apply { isEnabled = false }

    init {
        add(notice(), BorderLayout.NORTH)
        add(JScrollPane(table), BorderLayout.CENTER)
        add(controls(), BorderLayout.SOUTH)

        table.selectionModel.addListSelectionListener { startButton.isEnabled = selectedStartable() }
        startButton.addActionListener { selectedPack()?.let(onStart) }
        // Changing the script re-decides every row: a pack missing the chosen one becomes unlaunchable and
        // says so, which is the whole reason the Status column carries the reason rather than a tooltip.
        scriptChoice.addActionListener { onScriptChanged() }
    }

    /** The two things a user needs to know before pressing Start, stated once rather than per launch. */
    private fun notice(): JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
        add(
            PlainTextRendering.label(
                "Packs are launched with their own start scripts, so the scripts are tested too."
            )
        )
        add(
            PlainTextRendering.label(
                "Mojang's EULA is never accepted for you: the script asks on the console, and you answer it " +
                        "there. Every pack that has not been started before will ask once."
            )
        )
    }

    /** Refresh, Start, and the script the launch uses — in the order a user reads them. */
    private fun controls(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(JButton("Refresh").apply { addActionListener { onRefresh() } })
        add(startButton)
        add(PlainTextRendering.label("  using  "))
        add(scriptChoice)
    }

    /** The model row behind the current selection, accounting for the table's own sorting. */
    private fun selectedModelRow(): Int? {
        val viewRow = table.selectedRow
        return if (viewRow < 0) null else table.convertRowIndexToModel(viewRow)
    }

    /** The selected pack, or `null` when nothing is selected. */
    private fun selectedPack(): LaunchablePack? = selectedModelRow()?.let { model.packAt(it) }

    /** Whether the selected row can be launched right now. */
    private fun selectedStartable(): Boolean = selectedModelRow()?.let { model.startableAt(it) } ?: false

    /**
     * Replace the rows, keeping whatever the user had selected selected.
     *
     * Replacing the rows fires a table-wide change and Swing answers by dropping the selection, which is
     * why this is restored by hand. It is not only about the dropdown: the same redraw runs on Refresh and
     * on every finished generation, so without this an auto-refresh could clear the selection out from
     * under somebody reaching for Start.
     *
     * Restored by **pack**, never by row index. A refresh can reorder the list — a generation adds a pack
     * that sorts before the selected one — and restoring an index would quietly move the selection onto a
     * different pack, which is worse than losing it: Start would then launch something the user did not
     * choose.
     */
    fun show(rows: List<PackRow>) {
        val previouslySelected = selectedPack()?.directory

        model.rows = rows
        reselect(previouslySelected)

        startButton.isEnabled = selectedStartable()
    }

    /**
     * Select the row for [directory] again, if it is still on show.
     *
     * A pack that has gone leaves nothing selected rather than handing its selection to whatever took its
     * place. The view index is asked for separately because the table sorts for itself, so the model's row
     * order is not what the user is looking at.
     */
    private fun reselect(directory: File?) {
        if (directory == null) {
            return
        }
        val modelRow = model.rows.indexOfFirst { it.pack.directory == directory }
        if (modelRow < 0) {
            return
        }
        val viewRow = table.convertRowIndexToView(modelRow)
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow)
        }
    }
}
