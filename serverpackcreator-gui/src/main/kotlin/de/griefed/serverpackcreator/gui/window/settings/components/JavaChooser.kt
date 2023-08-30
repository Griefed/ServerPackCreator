package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import java.awt.Dimension
import java.io.File
import javax.swing.JFileChooser

/**
 * Customized filechooser for picking the Java executable with which server installations will be performed.
 *
 * @author Griefed
 */
class JavaChooser(apiProperties: ApiProperties, title: String) : JFileChooser() {
    init {
        currentDirectory = File(apiProperties.javaPath).absoluteFile.parentFile
        dialogTitle = title
        fileSelectionMode = FILES_ONLY
        isAcceptAllFileFilterUsed = true
        isMultiSelectionEnabled = false
        dialogType = SAVE_DIALOG
        preferredSize = Dimension(750, 450)
    }
}