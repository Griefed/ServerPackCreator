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
import de.griefed.serverpackcreator.gui.themes.ThemeManager
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * Menu revolving around viewing directories and logs, mainly.
 *
 * @author Griefed
 */
class ViewMenu(apiWrapper: ApiWrapper, themeManager: ThemeManager) : JMenu(Gui.menubar_gui_menu_view.toString()) {

    private val homeDir = HomeDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val serverPacksDir = ServerPacksDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val serverFilesDir = ServerFilesDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val propsDir = OpenPropertiesDirectory(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val iconsDir = OpenIconsDirectory(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val configsDir = ConfigsDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val pluginsDir = PluginsDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val pluginConfigsDir = PluginsConfigDirItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val spcLog = ServerPackCreatorLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val pluginsLog = PluginsLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val modloaderInstLog = ModloaderInstallerLogItem(apiWrapper.utilities!!.fileUtilities, apiWrapper.apiProperties)
    private val themesDir = ThemesDirItem(apiWrapper.utilities!!.fileUtilities, themeManager)

    init {
        add(homeDir)
        add(serverPacksDir)
        add(serverFilesDir)
        add(propsDir)
        add(iconsDir)
        add(configsDir)
        add(pluginsDir)
        add(pluginConfigsDir)
        add(themesDir)
        add(JSeparator())
        add(spcLog)
        add(pluginsLog)
        add(modloaderInstLog)
    }
}