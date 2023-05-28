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
 * full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.gui.components

import de.griefed.serverpackcreator.gui.GuiProps
import net.java.balloontip.BalloonTip
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.Timer

/**
 * Regular JButton but with a [BalloonTip] instead of the regular old Java-Style tooltip.
 */
open class BalloonTipButton(text: String?, icon: Icon, toolTip: String, guiProps: GuiProps) : JButton(text, icon) {
    private val toolTipLabel = JLabel(toolTip)

    constructor(text: String?, icon: Icon, toolTip: String, guiProps: GuiProps, actionListener: ActionListener) : this(
        text, icon, toolTip, guiProps
    ) {
        addActionListener(actionListener)
    }

    init {
        multiClickThreshhold = 1000
        val balloonTip = BalloonTip(
            this,
            toolTipLabel,
            guiProps.balloonStyle,
            false
        )
        balloonTip.isVisible = false
        val timer = Timer(1000) {
            balloonTip.style = guiProps.balloonStyle
            balloonTip.isVisible = true
        }
        timer.stop()
        timer.isRepeats = false
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {}

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {
                timer.restart()
            }

            override fun mouseExited(e: MouseEvent?) {
                timer.stop()
                balloonTip.isVisible = false
            }
        })
    }
}