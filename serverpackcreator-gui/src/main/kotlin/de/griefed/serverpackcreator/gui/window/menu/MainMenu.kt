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
package de.griefed.serverpackcreator.gui.window.menu

import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.UpdateDialogs
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import de.griefed.serverpackcreator.gui.window.menu.about.AboutMenu
import de.griefed.serverpackcreator.gui.window.menu.edit.EditMenu
import de.griefed.serverpackcreator.gui.window.menu.file.FileMenu
import de.griefed.serverpackcreator.gui.window.menu.view.ViewMenu
import javax.swing.JMenuBar

/**
 * TODO docs
 */
class MainMenu(
    guiProps: GuiProps,
    apiProperties: ApiProperties,
    utilities: Utilities,
    updateDialogs: UpdateDialogs,
    larsonScanner: LarsonScanner,
    configsTab: ConfigsTab,
    mainFrame: MainFrame
) {
    val menuBar: JMenuBar = JMenuBar()

    init {
        menuBar.add(FileMenu(configsTab, apiProperties, mainFrame.frame, utilities, guiProps))
        menuBar.add(EditMenu(utilities.fileUtilities, apiProperties, guiProps, larsonScanner))
        menuBar.add(ViewMenu(utilities, apiProperties, mainFrame))
        menuBar.add(AboutMenu(utilities.webUtilities, updateDialogs))
    }
}