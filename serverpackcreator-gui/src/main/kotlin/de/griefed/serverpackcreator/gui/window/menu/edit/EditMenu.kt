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
package de.griefed.serverpackcreator.gui.window.menu.edit

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.control.components.LarsonScanner
import javax.swing.JMenu

/**
 * Menu related to editing files or switching the current theme.
 *
 * @author Griefed
 */
class EditMenu(
    fileUtilities: FileUtilities,
    apiProperties: ApiProperties,
    guiProps: GuiProps,
    larsonScanner: LarsonScanner,
    mainFrame: MainFrame
) : JMenu(Gui.menubar_gui_menu_edit.toString()) {
    init {
        add(DefaultServerPropertiesItem(fileUtilities, apiProperties))
        add(DefaultServerIconItem(fileUtilities, apiProperties))
        add(SwitchThemeMenu(guiProps, larsonScanner, apiProperties, mainFrame))
    }
}