package de.griefed.serverpackcreator.gui.filebrowser.view

import Gui
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import de.griefed.serverpackcreator.gui.window.components.ElementLabel
import net.miginfocom.swing.MigLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class FileDetailPanel {
    private var fileNameLabel = JLabel(" ")
    private var lastModifiedLabel = JLabel(" ")
    private var fileSizeLabel = JLabel(" ")
    private var filePathField = JTextField(70)
    private var fileNameTextLabel = ElementLabel(" ")
    private val filePathTextLabel = ElementLabel(Gui.filebrowser_detail_path.toString())
    private val lastModifiedTextLabel = ElementLabel(Gui.filebrowser_detail_last.toString())
    private val fileSizeTextLabel = ElementLabel(Gui.filebrowser_detail_size.toString())
    var fileNode: FileNode? = null
    val panel: JPanel = JPanel(
        MigLayout(
            "fillx",
            "10[150!]0[grow]20"
        )
    )

    init {
        filePathField.isEditable = false
        panel.add(fileNameTextLabel, "cell 0 0")
        panel.add(fileNameLabel, "cell 1 0,grow")

        panel.add(filePathTextLabel, "cell 0 1")
        panel.add(filePathField, "cell 1 1,grow")

        panel.add(lastModifiedTextLabel, "cell 0 2")
        panel.add(lastModifiedLabel, "cell 1 2,grow")

        panel.add(fileSizeTextLabel, "cell 0 3")
        panel.add(fileSizeLabel, "cell 1 3,grow")
    }

    private fun updatePartControl(browserModel: FileBrowserModel) {
        if (fileNode != null) {
            val file: File = fileNode!!.file
            if (file.isDirectory) {
                fileNameTextLabel.text = Gui.filebrowser_detail_directory.toString()
            } else {
                fileNameTextLabel.text = Gui.filebrowser_detail_file.toString()
            }
            fileNameLabel.text = file.name
            fileNameLabel.icon = browserModel.getFileIcon(file)
            filePathField.text = file.absolutePath
            filePathField.caretPosition = 0
            lastModifiedLabel.text = generateLastModified(file)
            fileSizeLabel.text = generateFileSize(file)
        }
    }

    private fun generateLastModified(file: File): String {
        val timestamp = file.lastModified()
        val date = Date(timestamp)
        val pattern = "EEE, d MMM yyyy zzzz  h:mm:ss aa"
        val sdf = SimpleDateFormat(pattern)
        return sdf.format(date)
    }

    private fun generateFileSize(file: File): String {
        val label = arrayOf("bytes", "KB", "GB", "TB")
        var dbytes = file.length().toDouble()
        var count = 0
        while (dbytes > 1000.0) {
            dbytes /= 1024.0
            count++
        }
        return String.format("%.3f ", dbytes) + label[count]
    }

    fun setFileNode(fileNode: FileNode?, browserModel: FileBrowserModel) {
        this.fileNode = fileNode
        updatePartControl(browserModel)
    }
}