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
package de.griefed.serverpackcreator.gui.window.logs

import de.griefed.serverpackcreator.gui.components.ScrollDirection
import de.griefed.serverpackcreator.gui.components.SmartScroller
import de.griefed.serverpackcreator.gui.components.ViewPortPosition
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * TODO docs
 */
abstract class LogTailer(tooltip: String) : JPanel() {
    protected var textArea: JTextArea

    init {
        layout = GridBagLayout()
        val constraints = GridBagConstraints()
        constraints.anchor = GridBagConstraints.CENTER
        constraints.fill = GridBagConstraints.BOTH
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weighty = 1.0
        constraints.weightx = 1.0

        // Log Panel
        textArea = JTextArea()
        textArea.isEditable = false
        textArea.font = Font("Noto Sans Display Regular", Font.PLAIN, 15)
        textArea.toolTipText = tooltip
        val scrollPane = JScrollPane(
            textArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        SmartScroller(scrollPane, ScrollDirection.VERTICAL, ViewPortPosition.END)
        this.add(scrollPane, constraints)
    }

    /**
     * TODO docs
     */
    protected abstract fun createTailer(logsDirectory: File)
}