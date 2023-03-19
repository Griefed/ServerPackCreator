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
package de.griefed.serverpackcreator.gui.window.configs.components

import de.griefed.serverpackcreator.gui.GuiProps
import net.java.balloontip.BalloonTip
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JLabel

/**
 * TODO docs
 */
abstract class StatusIcon(private val guiProps: GuiProps, private val infoToolTip: String) : JLabel() {
    private val tooltTipLabel = JLabel(infoToolTip)

    init {
        icon = guiProps.infoIcon
        toolTipText = null
        val balloonTip = BalloonTip(
            this,
            tooltTipLabel,
            guiProps.balloonStyle,
            false
        )
        balloonTip.isVisible = false
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mousePressed(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseReleased(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseEntered(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseExited(e: MouseEvent?) {
                balloonTip.isVisible = false
            }
        })
    }

    /**
     * TODO docs
     */
    fun error(tooltip: String) {
        icon = guiProps.errorIcon
        tooltTipLabel.text = tooltip
    }

    /**
     * TODO docs
     */
    fun info() {
        icon = guiProps.infoIcon
        tooltTipLabel.text = infoToolTip
    }

    /**
     * TODO docs
     */
    fun warning(tooltip: String) {
        icon = guiProps.warningIcon
        tooltTipLabel.text = tooltip
    }
}