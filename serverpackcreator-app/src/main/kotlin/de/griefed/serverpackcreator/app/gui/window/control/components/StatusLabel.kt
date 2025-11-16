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
package de.griefed.serverpackcreator.app.gui.window.control.components

import java.awt.Color
import java.awt.Graphics
import javax.swing.JLabel
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

/**
 * Status label used in [de.griefed.serverpackcreator.app.gui.window.control.ControlPanel] to display latest INFO-type messages.
 *
 * @author Griefed
 */
class StatusLabel(text: String, private val transparency: Int = 255) : JLabel(text) {
    init {
        updateColour()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        updateColour()
    }

    /**
     * @author Griefed
     */
    private fun updateColour() {
        val color = UIManager.get("Label.foreground") as ColorUIResource
        foreground = Color(color.red, color.green, color.blue, transparency)
    }
}