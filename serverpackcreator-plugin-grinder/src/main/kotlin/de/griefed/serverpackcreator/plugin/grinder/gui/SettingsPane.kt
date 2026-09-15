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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

/**
 * Where the grinder to read is configured: its address, how often the Dashboard polls it, and a button
 * to find out whether it answers before leaving the tab.
 *
 * @param initialUrl The stored address, shown exactly as it was typed.
 * @param initialInterval The stored poll interval in seconds.
 * @param onSave Called with the address and interval when the user saves; the caller persists and reloads.
 * @param onTest Called when the user asks to test the typed address, and answers with a line to display.
 *
 * @author Griefed
 */
class SettingsPane(
    initialUrl: String,
    initialInterval: Int,
    private val onSave: (url: String, intervalSeconds: Int) -> Unit,
    private val onTest: (url: String, report: (Boolean, String) -> Unit) -> Unit
) : JPanel(BorderLayout()) {

    private val urlField = JTextField(initialUrl, 40)
    private val intervalSpinner = JSpinner(SpinnerNumberModel(initialInterval, 1, 3_600, 1))
    // Carries GrinderClient's reason strings, which interpolate the typed URL and an exception's
    // message — so it goes through the plain-text label like everything else that shows outside text.
    private val feedback = PlainTextRendering.label(" ")

    init {
        border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        add(form(), BorderLayout.NORTH)
    }

    /** The address the user has typed, unnormalised — what gets stored, so the field reads back the same. */
    val typedUrl: String get() = urlField.text.orEmpty()

    /** The whole settings form, top-aligned so it does not stretch across an otherwise empty tab. */
    private fun form() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(row(JLabel("Grinder URL:"), urlField))
        add(
            hint(
                "The address of a running grinder, e.g. http://localhost:8757 — the same one its report " +
                        "server prints on startup. Leave empty to disable the plugin entirely."
            )
        )
        add(row(JLabel("Dashboard refresh (seconds):"), intervalSpinner))
        add(
            row(
                JButton("Test connection").apply {
                    addActionListener {
                        feedback.text = "Testing…"
                        feedback.foreground = null
                        onTest(typedUrl) { ok, message -> report(ok, message) }
                    }
                },
                JButton("Save").apply {
                    addActionListener {
                        onSave(typedUrl, intervalSpinner.value as Int)
                        report(true, "Saved.")
                    }
                }
            )
        )
        add(row(feedback))
        add(
            hint(
                "The grinder's report server has no authentication. Point this at a grinder you trust: " +
                        "its verdicts decide which mods get excluded from your server packs."
            )
        )
    }

    /** Show [message], coloured by whether it reports success. */
    private fun report(ok: Boolean, message: String) {
        feedback.text = message
        feedback.foreground = if (ok) null else ERROR_COLOUR
    }

    /** One left-aligned row of the form. */
    private fun row(vararg components: java.awt.Component) = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
        components.forEach { add(it) }
    }

    /** Explanatory text under a field, wrapped so it stays readable at any tab width. */
    private fun hint(text: String) = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        add(JLabel("<html><body style='width:520px'>$text</body></html>"))
    }

    private companion object {
        /** A red legible on both the light and dark look-and-feels ServerPackCreator ships. */
        val ERROR_COLOUR = Color(0xC0, 0x39, 0x2B)
    }
}
