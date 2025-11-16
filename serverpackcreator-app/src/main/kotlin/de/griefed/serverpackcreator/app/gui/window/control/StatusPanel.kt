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
package de.griefed.serverpackcreator.app.gui.window.control

import de.griefed.serverpackcreator.api.utilities.ReticulatingSplines
import de.griefed.serverpackcreator.app.gui.window.control.components.StatusLabel
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Status panel housing the [StatusLabel], to display them in the [ControlPanel].
 *
 * @author Griefed
 */
class StatusPanel {
    val panel = JPanel()
    private val statusLine0 = StatusLabel("...${ReticulatingSplines.reticulate()}", 20)
    private val statusLine1 = StatusLabel("...${ReticulatingSplines.reticulate()}", 50)
    private val statusLine2 = StatusLabel("...${ReticulatingSplines.reticulate()}", 100)
    private val statusLine3 = StatusLabel("...${ReticulatingSplines.reticulate()}", 150)
    private val statusLine4 = StatusLabel("...${ReticulatingSplines.reticulate()}", 200)
    private val statusLine5 = StatusLabel("...${ReticulatingSplines.reticulate()}")

    init {
        panel.layout = MigLayout("fillx", "0[grow]0")
        statusLine0.horizontalAlignment = JLabel.LEFT
        statusLine1.horizontalAlignment = JLabel.LEFT
        statusLine2.horizontalAlignment = JLabel.LEFT
        statusLine3.horizontalAlignment = JLabel.LEFT
        statusLine4.horizontalAlignment = JLabel.LEFT
        statusLine5.horizontalAlignment = JLabel.LEFT
        panel.add(statusLine0, "cell 0 0,grow")
        panel.add(statusLine1, "cell 0 1,grow")
        panel.add(statusLine2, "cell 0 2,grow")
        panel.add(statusLine3, "cell 0 3,grow")
        panel.add(statusLine4, "cell 0 4,grow")
        panel.add(statusLine5, "cell 0 5,grow")
    }

    /**
     * Update the labels in the status panel.
     *
     * @param text The text to update the status with.
     * @author Griefed
     */
    fun updateStatus(text: String) {
        statusLine0.text = statusLine1.text
        statusLine1.text = statusLine2.text
        statusLine2.text = statusLine3.text
        statusLine3.text = statusLine4.text
        statusLine4.text = statusLine5.text
        statusLine5.text = text
    }
}