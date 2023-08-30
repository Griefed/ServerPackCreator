package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import java.awt.Dimension
import javax.swing.JFileChooser

/**
 * Customized filechooser for picking ServerPackCreator script templates.
 *
 * @author Griefed
 */
class ScriptTemplatesChooser(apiProperties: ApiProperties, title: String) : JFileChooser() {
    init {
        currentDirectory = apiProperties.serverFilesDirectory
        dialogTitle = title
        fileSelectionMode = FILES_ONLY
        isAcceptAllFileFilterUsed = true
        isMultiSelectionEnabled = true
        dialogType = SAVE_DIALOG
        preferredSize = Dimension(750, 450)
    }
}