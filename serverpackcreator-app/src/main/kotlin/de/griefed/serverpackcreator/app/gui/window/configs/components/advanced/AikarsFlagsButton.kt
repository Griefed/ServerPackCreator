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
package de.griefed.serverpackcreator.app.gui.window.configs.components.advanced

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.CompoundIcon
import de.griefed.serverpackcreator.app.gui.components.TextIcon
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import net.java.balloontip.BalloonTip
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JLabel

/**
 * Button to set a server pack configs JVM flags to Aikars commonly used flags.
 *
 * @author Griefed
 */
class AikarsFlagsButton(configEditor: ConfigEditor, guiProps: GuiProps) : JButton() {
    init {
        this.addActionListener { configEditor.setAikarsFlagsAsJavaArguments() }
        toolTipText = null
        val balloonTip = BalloonTip(
            this,
            JLabel(Translations.createserverpack_gui_createserverpack_javaargs_aikar_tooltip.toString()),
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
        val parts = Translations.createserverpack_gui_createserverpack_javaargs_aikar.toString().split(" ")
        val flags = mutableListOf<TextIcon>()
        for (part in parts) {
            flags.add(TextIcon(this, part))
        }
        icon = CompoundIcon(
            flags.toTypedArray(),
            5,
            CompoundIcon.Axis.Y_AXIS
        )
    }
}