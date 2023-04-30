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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer.ImagePreviewRenderer
import net.miginfocom.swing.MigLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextPane

/**
 * Preview panel displaying either an image or the contents of a .properties or .conf file, depending on what was
 * selected.
 *
 * @author Griefed
 */
class FilePreviewPanel(private val guiProps: GuiProps) : JPanel(
    MigLayout(
        "",
        "0[grow]0",
        "0[grow]0"
    )
) {
    private var fileNode: FileNode? = null
    private val iconPreview = ImagePreviewRenderer()
    private val textPreview = JTextPane()
    private val textScroll = JScrollPane(
        textPreview,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
    private val props = "properties"
    private val conf = "conf"

    init {
        iconPreview.isVisible = false
        textPreview.isVisible = false
        textPreview.isEditable = false
        add(iconPreview, "cell 0 0, grow, aligny center, alignx center, hidemode 3,w 10:10:,h 10:10:")
        add(textScroll, "cell 0 0, grow, aligny top, alignx left, hidemode 3,w 10:10:,h 10:10:")
    }

    /**
     * Update the current node. Depending on whether an image, properties or conf was selected, a different preview
     * will be visible.
     *
     * @author Griefed
     */
    fun setFileNode(fileNode: FileNode?) {
        this.fileNode = fileNode!!
        val name = fileNode.file.name
        if (name.matches(guiProps.imageRegex)) {
            iconPreview.updateImage(ImageIO.read(fileNode.file.absoluteFile))
            iconPreview.repaint()
            textPreview.text = ""
            textPreview.isVisible = false
            textScroll.isVisible = false
            iconPreview.isVisible = true
        } else if (name.endsWith(props)) {
            textPreview.text = fileNode.file.readText()
            iconPreview.updateImage(null)
            iconPreview.isVisible = false
            textPreview.isVisible = true
            textScroll.isVisible = true
        } else if (name.endsWith(conf)) {
            textPreview.text = fileNode.file.readText()
            iconPreview.updateImage(null)
            iconPreview.isVisible = false
            textPreview.isVisible = true
            textScroll.isVisible = true
        } else {
            iconPreview.updateImage(null)
            iconPreview.isVisible = false
            textPreview.text = ""
            textPreview.isVisible = false
            textScroll.isVisible = false
        }
    }
}