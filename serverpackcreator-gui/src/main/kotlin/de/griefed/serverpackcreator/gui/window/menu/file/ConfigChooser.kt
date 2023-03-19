package de.griefed.serverpackcreator.gui.window.menu.file

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import java.awt.Dimension
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Customized filechooser for picking ServerPackCreator config-files.
 *
 * @author Griefed
 */
class ConfigChooser(apiProperties: ApiProperties, title: String) : JFileChooser() {
    init {
        currentDirectory = apiProperties.configsDirectory
        dialogTitle = title
        fileSelectionMode = FILES_ONLY
        fileFilter = FileNameExtensionFilter(Gui.createserverpack_gui_buttonloadconfig_filter.toString(), "conf")
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
        preferredSize = Dimension(750, 450)
    }
}