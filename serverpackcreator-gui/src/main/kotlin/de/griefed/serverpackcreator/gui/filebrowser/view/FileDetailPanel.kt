package de.griefed.serverpackcreator.gui.filebrowser.view

import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.model.FileNode
import java.awt.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class FileDetailPanel {
    private val insets: Insets = Insets(6, 6, 6, 6)
    private var fileNameLabel: JLabel = JLabel(" ")
    private var fileNameTextLabel: JLabel = JLabel(" ")
    private var lastModifiedLabel: JLabel = JLabel(" ")
    private var fileSizeLabel: JLabel = JLabel(" ")
    private var filePathField: JTextField = JTextField(70)
    var fileNode: FileNode? = null
    val panel: JPanel = JPanel()

    init {
        panel.layout = GridBagLayout()
        var gridy = 0
        addComponent(
            panel, fileNameTextLabel, 0, gridy,
            insets
        )
        addComponent(
            panel, fileNameLabel, 1, gridy++,
            insets
        )
        val filePathTextLabel = JLabel("Path:")
        addComponent(
            panel, filePathTextLabel, 0, gridy,
            insets
        )
        filePathField.isEditable = false
        addComponent(
            panel, filePathField, 1, gridy++,
            insets
        )
        val lastModifiedTextLabel = JLabel("Last Modified:")
        addComponent(
            panel, lastModifiedTextLabel, 0, gridy,
            insets
        )
        addComponent(
            panel, lastModifiedLabel, 1, gridy++,
            insets
        )
        val fileSizeTextLabel = JLabel("File Size:")
        addComponent(
            panel, fileSizeTextLabel, 0, gridy,
            insets
        )
        addComponent(
            panel, fileSizeLabel, 1, gridy++,
            insets
        )
    }

    private fun addComponent(
        container: Container,
        component: Component,
        gridx: Int,
        gridy: Int,
        insets: Insets
    ) {
        val gbc = GridBagConstraints(
            gridx, gridy,
            1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
            insets, 0, 0
        )
        container.add(component, gbc)
    }

    private fun updatePartControl(browserModel: FileBrowserModel) {
        if (fileNode != null) {
            val file: File = fileNode!!.file
            if (file.isDirectory) {
                fileNameTextLabel.text = "Directory:"
            } else {
                fileNameTextLabel.text = "File:"
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