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
package de.griefed.serverpackcreator.gui.window.menu.file

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Menu item to load a configuration from a file into a new or the currently selected tab in the GUI.
 *
 * @author Griefed
 */
class LoadConfigItem(
    private val apiProperties: ApiProperties,
    private val mainFrame: JFrame,
    private val fileUtilities: FileUtilities,
    private val guiProps: GuiProps,
    private val tabbedConfigsTab: TabbedConfigsTab
) : JMenuItem(Gui.menubar_gui_menuitem_loadconfig.toString()) {
    private val log = cachedLoggerOf(this.javaClass)

    init {
        this.addActionListener { loadConfigFile() }
    }

    /**
     * @author Griefed
     */
    private fun loadConfigFile() {
        val configChooser = ConfigChooser(apiProperties, Gui.createserverpack_gui_buttonloadconfig_title.toString())
        configChooser.isMultiSelectionEnabled = true
        if (configChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            val files = configChooser.selectedFiles.map { file ->
                try {
                    File(fileUtilities.resolveLink(file)).absoluteFile
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not resolve link/symlink. Using entry from user input for checks.", ex)
                    file.absoluteFile
                }
            }
            for (file in files) {
                if (tabbedConfigsTab.tabs.tabCount > 0 &&
                    DialogUtilities.createShowGet(
                        Gui.menubar_gui_config_load_message(file.absolutePath),
                        Gui.menubar_gui_config_load_title.toString(),
                        mainFrame,
                        JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.YES_NO_OPTION,
                        guiProps.warningIcon,
                        false,
                        arrayOf(Gui.menubar_gui_config_load_current, Gui.menubar_gui_config_load_new)
                    ) == 0
                ) {
                    tabbedConfigsTab.loadConfig(file, tabbedConfigsTab.selectedEditor!!)
                } else {
                    tabbedConfigsTab.loadConfig(file)
                }
            }
        }
    }
}