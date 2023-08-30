package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import java.awt.Dimension
import javax.swing.JFileChooser

/**
 * Customized filechooser for picking ServerPackCreator server pack directory.
 *
 * @author Griefed
 */
class ServerPackDirChooser(apiProperties: ApiProperties, title: String) : JFileChooser() {
    init {
        currentDirectory = apiProperties.serverPacksDirectory
        dialogTitle = title
        fileSelectionMode = DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
        dialogType = SAVE_DIALOG
        preferredSize = Dimension(750, 450)
    }
}