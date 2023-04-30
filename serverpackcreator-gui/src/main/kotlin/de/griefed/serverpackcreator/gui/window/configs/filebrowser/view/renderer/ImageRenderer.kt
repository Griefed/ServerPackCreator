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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer

import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Canvas in which an image preview is displayed in.
 *
 * @author Griefed
 */
class ImageRenderer(private val isServerIconPreview: Boolean = false, title: String = "Image Preview") : JPanel() {
    var image: Image? = null
        set(value) {
            field = if (value == null || !isServerIconPreview) {
                value
            } else {
                value.getScaledInstance(64, 64, Image.SCALE_SMOOTH)
            }
        }

    init {
        border = BorderFactory.createTitledBorder(title)
    }

    override fun paint(g: Graphics?) {
        super.paint(g)
        if (image == null || !isVisible) {
            g?.dispose()
            return
        }

        val borderInsets = border.getBorderInsets(this)
        val widthInset = borderInsets.left + borderInsets.right + (insets.left + insets.right)
        val heightInset = borderInsets.top + borderInsets.bottom + (insets.top + insets.bottom) / 2
        val imgSize = Dimension(image!!.getWidth(null), image!!.getHeight(null))
        val widthRatio: Double = (width.toDouble() - widthInset) / imgSize.width
        val heightRatio: Double = (height.toDouble() - heightInset) / imgSize.height
        val ratio = widthRatio.coerceAtMost(heightRatio)

        val newWidth = imgSize.width * ratio
        val newHeight = imgSize.height * ratio
        val startX = (width - newWidth) / 2
        val startY = ((height - newHeight) / 2) + borderInsets.bottom

        try {
            g?.drawImage(
                image!!.getScaledInstance(newWidth.toInt(), newHeight.toInt(), Image.SCALE_SMOOTH),
                startX.toInt(),
                startY.toInt(),
                newWidth.toInt(),
                newHeight.toInt(),
                this
            )
        } catch (_: IllegalArgumentException) {}

        g?.dispose()
    }
}
