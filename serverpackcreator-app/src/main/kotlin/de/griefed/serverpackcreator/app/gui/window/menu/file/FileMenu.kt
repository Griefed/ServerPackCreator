/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.window.menu.file

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * File menu to present the user all file-related operations, such as starting a new server pack config, loading a config,
 * saving the currently selected config, saving all currently opened configs etc.
 *
 * @author Griefed
 */
class FileMenu(
    tabbedConfigsTab: TabbedConfigsTab,
    apiProperties: ApiProperties,
    mainFrame: MainFrame,
    utilities: Utilities,
    guiProps: GuiProps
) : JMenu(Translations.menubar_gui_menu_file.toString()) {

    private val newConfig = NewConfigItem(tabbedConfigsTab)
    private val loadConfig = LoadConfigItem(tabbedConfigsTab)
    private val saveConfig = SaveConfigItem(tabbedConfigsTab)
    private val saveConfigAs = SaveConfigAsItem(tabbedConfigsTab)
    private val saveAll = SaveAllConfigsItem(tabbedConfigsTab)
    private val mainLogUpload = MainLogToHasteBinItem(utilities.webUtilities, apiProperties, guiProps, mainFrame.frame)
    private val configUpload = ConfigToHasteBinItem(tabbedConfigsTab, utilities.webUtilities, guiProps, mainFrame.frame)
    private val exit = ExitItem(mainFrame)

    init {
        add(newConfig)
        add(loadConfig)
        add(JSeparator())
        add(saveConfig)
        add(saveConfigAs)
        add(saveAll)
        add(JSeparator())
        add(mainLogUpload)
        add(configUpload)
        add(JSeparator())
        add(exit)
    }
}