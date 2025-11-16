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
package de.griefed.serverpackcreator.app.gui.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.Timer

/**
 * Regular JButton but with a [ThemedBalloonTip] instead of the regular old Java-Style tooltip.
 *
 * @author Griefed
 */
open class BalloonTipButton(text: String?, icon: Icon, toolTip: String, guiProps: GuiProps) : JButton(text, icon) {
    private val toolTipLabel = JLabel(toolTip)

    constructor(text: String?, icon: Icon, toolTip: String, guiProps: GuiProps, actionListener: ActionListener) : this(
        text, icon, toolTip, guiProps
    ) {
        this.addActionListener(actionListener)
    }

    init {
        multiClickThreshhold = 1000
        val balloonTip = ThemedBalloonTip(this,toolTipLabel,false, guiProps)
        val tooltipTimer = Timer(1000) {
            balloonTip.isVisible = true
        }
        tooltipTimer.stop()
        tooltipTimer.isRepeats = false
        this.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {}

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {
                tooltipTimer.restart()
            }

            override fun mouseExited(e: MouseEvent?) {
                tooltipTimer.stop()
                balloonTip.isVisible = false
            }
        })
    }

    override fun setToolTipText(text: String) {
        toolTipLabel.text = text
    }
}