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
package de.griefed.serverpackcreator.gui.filebrowser.view

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import net.miginfocom.swing.MigLayout
import java.awt.Canvas
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextPane

/**
 * TODO docs
 */
class FilePreviewPanel(private val guiProps: GuiProps) {
    private var fileNode: FileNode? = null
    private val iconPreview = ImageCanvas()
    private val textPreview = JTextPane()
    private val textScroll =
        JScrollPane(textPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
    private val props = "properties"
    private val conf = "conf"
    val panel = JPanel(
        MigLayout(
            "",
            "0[grow]0",
            "0[grow]0"
        )
    )

    init {
        iconPreview.isVisible = false
        textPreview.isVisible = false
        textPreview.isEditable = false
        panel.add(iconPreview, "cell 0 0, grow, aligny center, alignx center, hidemode 3,w 10:10:,h 10:10:")
        panel.add(textScroll, "cell 0 0, grow, aligny top, alignx left, hidemode 3,w 10:10:,h 10:10:")
    }

    /**
     * TODO docs
     */
    fun setFileNode(fileNode: FileNode) {
        this.fileNode = fileNode
        val name = fileNode.file.name
        if (name.matches(guiProps.imageRegex)) {
            iconPreview.repaint()
            textPreview.isVisible = false
            textScroll.isVisible = false
            iconPreview.isVisible = true
        } else if (name.endsWith(props)) {
            textPreview.text = fileNode.file.readText()
            iconPreview.isVisible = false
            textPreview.isVisible = true
            textScroll.isVisible = true
        } else if (name.endsWith(conf)) {
            textPreview.text = fileNode.file.readText()
            iconPreview.isVisible = false
            textPreview.isVisible = true
            textScroll.isVisible = true
        } else {
            iconPreview.isVisible = false
            textPreview.isVisible = false
            textScroll.isVisible = false
        }
    }

    /**
     * TODO docs
     */
    inner class ImageCanvas : Canvas() {
        override fun paint(g: Graphics?) {
            if (fileNode?.file?.absoluteFile!!.name.matches(guiProps.imageRegex)) {
                val image = ImageIO.read(fileNode?.file?.absoluteFile)
                val imgSize = Dimension(image.getWidth(null), image.getHeight(null))
                val boundary = this.size

                val widthRatio: Double = boundary.width.toDouble() / imgSize.width
                val heightRatio: Double = boundary.height.toDouble() / imgSize.height
                val ratio = widthRatio.coerceAtMost(heightRatio)
                val newWidth = imgSize.width * ratio
                val newHeight = imgSize.height * ratio

                val startX = (boundary.width - newWidth) / 2
                val startY = (boundary.height - newHeight) / 2

                g?.drawImage(
                    image.getScaledInstance(newWidth.toInt(), newHeight.toInt(), Image.SCALE_SMOOTH),
                    startX.toInt(),
                    startY.toInt(),
                    newWidth.toInt(),
                    newHeight.toInt(),
                    this@ImageCanvas
                )
            }
        }
    }
}