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
package de.griefed.serverpackcreator.app.gui.window.menu.view

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * Menu revolving around viewing directories and logs, mainly.
 *
 * @author Griefed
 */
class ViewMenu(apiWrapper: ApiWrapper, themeManager: ThemeManager) : JMenu(Translations.menubar_gui_menu_view.toString()) {

    private val homeDir = HomeDirItem(apiWrapper.apiProperties)
    private val serverPacksDir = ServerPacksDirItem(apiWrapper.apiProperties)
    private val serverFilesDir = ServerFilesDirItem(apiWrapper.apiProperties)
    private val propsDir = OpenPropertiesDirectory(apiWrapper.apiProperties)
    private val iconsDir = OpenIconsDirectory(apiWrapper.apiProperties)
    private val configsDir = ConfigsDirItem(apiWrapper.apiProperties)
    private val pluginsDir = PluginsDirItem(apiWrapper.apiProperties)
    private val pluginConfigsDir = PluginsConfigDirItem(apiWrapper.apiProperties)
    private val spcLog = ServerPackCreatorLogItem(apiWrapper.apiProperties)
    private val pluginsLog = PluginsLogItem(apiWrapper.apiProperties)
    private val modloaderInstLog = ModloaderInstallerLogItem(apiWrapper.apiProperties)
    private val themesDir = ThemesDirItem(themeManager)

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