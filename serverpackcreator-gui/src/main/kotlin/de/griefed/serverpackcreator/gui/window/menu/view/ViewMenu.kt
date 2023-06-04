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
package de.griefed.serverpackcreator.gui.window.menu.view

import Gui
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * Menu revolving around viewing directories and logs, mainly.
 *
 * @author Griefed
 */
class ViewMenu(
    apiWrapper: ApiWrapper,
    guiProps: GuiProps
) :
    JMenu(Gui.menubar_gui_menu_view.toString()) {
    init {
        val fileUtilities = apiWrapper.utilities!!.fileUtilities
        val apiProperties = apiWrapper.apiProperties
        add(HomeDirItem(fileUtilities, apiProperties))
        add(ServerPacksDirItem(fileUtilities, apiProperties))
        add(ServerFilesDirItem(fileUtilities, apiProperties))
        add(OpenPropertiesDirectory(fileUtilities, apiProperties))
        add(OpenIconsDirectory(fileUtilities, apiProperties))
        add(ConfigsDirItem(fileUtilities, apiProperties))
        add(JSeparator())
        add(PluginsDirItem(fileUtilities, apiProperties))
        add(PluginsConfigDirItem(fileUtilities, apiProperties))
        add(JSeparator())
        add(ServerPackCreatorLogItem(fileUtilities, apiProperties))
        add(PluginsLogItem(fileUtilities, apiProperties))
        add(ModloaderInstallerLogItem(fileUtilities, apiProperties))
        add(JSeparator())
        add(SwitchThemeMenu(guiProps))
    }
}