/* Copyright (C) 2024  Griefed
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

import Translations
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import javax.swing.JMenuItem

/**
 * Menu item which adds a new configuration-tab with default values.
 *
 * @author Griefed
 */
class NewConfigItem(private val tabbedConfigsTab: TabbedConfigsTab) :
    JMenuItem(Translations.menubar_gui_menuitem_newconfig.toString()) {
    init {
        this.addActionListener { tabbedConfigsTab.addTab() }
    }
}