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
import net.java.balloontip.BalloonTip
import java.awt.event.ActionListener
import javax.swing.JComponent

/**
 * Custom BalloonTip which ensures that the currently set theme is always used.
 *
 * @author Griefed
 */
class ThemedBalloonTip(
    attachedComponent: JComponent,
    contents: JComponent,
    useCloseButton: Boolean,
    private val guiProps: GuiProps,
    orientation: Orientation = Orientation.LEFT_BELOW,
    attachLocation: AttachLocation = AttachLocation.CENTER,
    horizontalOffset: Int = 15,
    verticalOffset: Int = 15
) : BalloonTip(attachedComponent, contents, guiProps.balloonStyle, orientation, attachLocation, horizontalOffset, verticalOffset, useCloseButton) {

    constructor(
        attachedComponent: JComponent,
        contents: JComponent,
        useCloseButton: Boolean,
        guiProps: GuiProps,
        actionListener: ActionListener
    ) : this(attachedComponent, contents, useCloseButton, guiProps) {
        val button = getDefaultCloseButton()
        button.addActionListener(actionListener)
        button.addActionListener { isVisible = false }
        setCloseButton(button)
    }

    init {
        isVisible = false
    }

    override fun setVisible(visible: Boolean) {
        style = guiProps.balloonStyle
        super.setVisible(visible)
    }
}