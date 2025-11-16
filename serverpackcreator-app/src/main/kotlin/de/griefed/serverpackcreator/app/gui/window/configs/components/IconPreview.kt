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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.getScaledInstance
import kotlinx.coroutines.*
import net.java.balloontip.BalloonTip
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel

/**
 * Smallish image preview for server icons.
 *
 * @author Griefed
 */
class IconPreview(private val guiProps: GuiProps) : JLabel(guiProps.serverIcon) {
    private val bigPreview = JLabel(scaled(guiProps.serverIcon))
    private val balloonTip: BalloonTip = BalloonTip(
        this,
        bigPreview,
        guiProps.balloonStyle,
        false
    )
    private var lastLoadedIcon: File? = null

    init {
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
     * @author Griefed
     */
    private fun scaled(icon: ImageIcon, width: Int = 128, height: Int = 128): ImageIcon {
        return icon.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun updateIcon(newIcon: File) {
        if (lastLoadedIcon != null && lastLoadedIcon!!.absolutePath == newIcon.absolutePath) {
            return
        }
        icon = guiProps.loadingAnimation32
        bigPreview.icon = guiProps.loadingAnimation128
        lastLoadedIcon = newIcon.absoluteFile
        GlobalScope.launch(guiProps.miscDispatcher, CoroutineStart.ATOMIC) {
            updateIcon(ImageIcon(ImageIO.read(newIcon)))
        }
    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun updateIcon(newIcon: ImageIcon, reset: Boolean = false) {
        GlobalScope.launch(guiProps.miscDispatcher, CoroutineStart.ATOMIC) {
            run {
                icon = scaled(newIcon, 32, 32)
            }
            run {
                bigPreview.icon = scaled(newIcon)
            }
        }
        if (reset) {
            lastLoadedIcon = null
        }
    }
}