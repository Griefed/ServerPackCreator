/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import net.java.balloontip.BalloonTip
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JLabel

/**
 * Status icon to display either an info-, warning-, or error-icon. A status icon also implements [BalloonTip] to display
 * info, warning or error messages.
 *
 * @author Griefed
 */
open class StatusIcon(private val guiProps: GuiProps, private val infoToolTip: String) : JLabel() {
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
        this.addMouseListener(object : MouseListener {
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
     * Update the error tooltip with the given [tooltip] and set the icon to the error-icon, indicating that there was...
     * well...an error.
     *
     * @author Griefed
     */
    fun error(tooltip: String) {
        icon = guiProps.errorIcon
        tooltTipLabel.text = tooltip
    }

    /**
     * Set the icon to the info-icon and the tooltip with which this status icon was initialized with.
     *
     * @author Griefed
     */
    fun info() {
        icon = guiProps.infoIcon
        tooltTipLabel.text = infoToolTip
    }

    /**
     * Update the warning tooltip with the given [tooltip] and set the icon to the warning-icon, indicating that there was...
     * well...a warning.
     *
     * @author Griefed
     */
    @Suppress("unused")
    fun warning(tooltip: String) {
        icon = guiProps.warningIcon
        tooltTipLabel.text = tooltip
    }
}