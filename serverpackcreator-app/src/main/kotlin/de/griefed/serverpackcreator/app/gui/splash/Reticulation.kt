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
 * full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.gui.splash

import de.griefed.serverpackcreator.api.utilities.ReticulatingSplines
import java.awt.Font
import javax.swing.JLabel

/**
 * Reticulation to generate nonsense messages to display in the splashscreen.
 *
 * @author Griefed
 */
class Reticulation(
    width: Int,
    height: Int,
    props: SplashProps,
) : JLabel() {
    init {
        font = Font("arial", Font.BOLD, 20)
        horizontalAlignment = CENTER
        val y = Math.floorDiv(height, 2) + 20
        setBounds(0, y, width, 40)
        foreground = props.secondaryColour
        reticulate()
    }

    /**
     * Generate a new reticulation.
     *
     * @author Griefed
     */
    fun reticulate() {
        text = ReticulatingSplines.reticulate()
    }
}