package de.griefed.serverpackcreator.gui.filebrowser.view

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
class FilePreviewPanel {
    private var fileNode: FileNode? = null
    private val iconPreview = ImageCanvas()
    private val textPreview = JTextPane()
    private val textScroll =
        JScrollPane(textPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

    // TODO move regexes to guiProps
    private val imageRegex = ".*\\.([Pp][Nn][Gg]|[Jj][Pp][Gg]|[Jj][Pp][Ee][Gg]|[Bb][Mm][Pp])".toRegex()
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
        if (name.matches(imageRegex)) {
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
            if (fileNode?.file?.absoluteFile!!.name.matches(imageRegex)) {
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