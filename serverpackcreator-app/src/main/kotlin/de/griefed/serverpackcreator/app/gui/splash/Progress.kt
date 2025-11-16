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

import java.awt.Color
import javax.swing.JProgressBar
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.roundToInt

/**
 * Progressbar to display in the splashscreen to give a vague sense of how far SPC is loaded.
 *
 * @author Griefed
 */
class Progress(width: Int, height: Int, props: SplashProps) : JProgressBar() {
    init {
        val offset = 20f
        val x = (width / 100f * offset).roundToInt()
        val y = Math.floorDiv(height, 2)
        val barWidth = (width / 100f * (100f - offset * 2)).roundToInt()
        setBounds(x, y, barWidth, 20)
        alignmentY = 0.0f
        isBorderPainted = true
        isStringPainted = true
        background = Color.WHITE
        foreground = props.primaryColour
        value = 0
        setUI(
            object : BasicProgressBarUI() {
                override fun getSelectionForeground(): Color {
                    return props.secondaryColour
                }

                // Text-colour when bar is NOT covering the loading-text
                override fun getSelectionBackground(): Color {
                    return props.primaryColour
                }
            }
        )
    }
}