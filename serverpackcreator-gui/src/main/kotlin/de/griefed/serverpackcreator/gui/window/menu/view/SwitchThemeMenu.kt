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
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import de.griefed.serverpackcreator.gui.GuiProps
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * Menu to give the user the choice to switch between any and all available themes provided in [FlatAllIJThemes].
 *
 * @author Griefed
 */
class SwitchThemeMenu(
    private val guiProps: GuiProps,
) : JMenu(Gui.menubar_gui_menu_theme.toString()) {
    private val themeItems = HashMap<String,JMenuItem>(FlatAllIJThemes.INFOS.size)
    init {
        for (theme in FlatAllIJThemes.INFOS) {
            val item = JMenuItem(theme.name)
            item.addActionListener { changeTheme(theme) }
            if (guiProps.currentTheme.name == theme.name) {
                item.icon = guiProps.smallCheckmarkIcon
            }
            themeItems[theme.name] = item
            add(item)
        }
    }

    /**
     * Change the theme to the provided [theme], updating the whole GUI.
     *
     * @author Griefed
     */
    private fun changeTheme(theme: FlatAllIJThemes.FlatIJLookAndFeelInfo) {
        val instance = Class.forName(theme.className).getDeclaredConstructor().newInstance() as FlatLaf
        themeItems[guiProps.currentTheme.name]!!.icon = null
        guiProps.currentTheme = instance
        themeItems[guiProps.currentTheme.name]!!.icon = guiProps.smallCheckmarkIcon
    }
}