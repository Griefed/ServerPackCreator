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
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.updater.MigrationManager
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * Menu revolving around viewing directories and logs, mainly.
 *
 * @author Griefed
 */
class ViewMenu(apiWrapper: ApiWrapper, migrationManager: MigrationManager, guiProps: GuiProps, mainFrame: MainFrame) :
    JMenu(Gui.menubar_gui_menu_view.toString()) {
    init {
        add(HomeDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(ServerPacksDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(ServerFilesDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(ConfigsDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(JSeparator())
        add(PluginsDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(PluginsConfigDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(JSeparator())
        add(MigrationInfoItem(apiWrapper, migrationManager, guiProps, mainFrame))
        add(JSeparator())
        add(ServerPackCreatorLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(PluginsLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
        add(ModloaderInstallerLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties))
    }
}