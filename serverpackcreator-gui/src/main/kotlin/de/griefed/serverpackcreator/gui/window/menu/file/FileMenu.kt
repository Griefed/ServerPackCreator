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
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import javax.swing.JMenu
import javax.swing.JSeparator

class FileMenu(
    tabbedConfigsTab: TabbedConfigsTab,
    apiProperties: ApiProperties,
    mainFrame: MainFrame,
    utilities: Utilities,
    guiProps: GuiProps
) : JMenu(Gui.menubar_gui_menu_file.toString()) {
    init {
        add(NewConfigItem(tabbedConfigsTab))
        add(LoadConfigItem(apiProperties, mainFrame.frame, utilities.fileUtilities, guiProps, tabbedConfigsTab))
        add(JSeparator())
        add(SaveConfigItem(tabbedConfigsTab))
        add(SaveConfigAsItem(apiProperties, mainFrame.frame, tabbedConfigsTab))
        add(SaveAllConfigsItem(tabbedConfigsTab))
        add(JSeparator())
        add(MainLogToHasteBinItem(utilities.webUtilities, apiProperties, guiProps, mainFrame.frame))
        add(ConfigToHasteBinItem(tabbedConfigsTab, utilities.webUtilities, apiProperties, guiProps, mainFrame.frame))
        add(JSeparator())
        add(ExitItem(mainFrame))
    }
}