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
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import javax.swing.JMenuItem

/**
 * Menu item to store the currently selected configuration-tabs config under a custom file and path.
 *
 * @author Griefed
 */
class SaveConfigAsItem(
    private val tabbedConfigsTab: TabbedConfigsTab
) : JMenuItem(Translations.menubar_gui_menuitem_saveas.toString()) {

    init {
        this.addActionListener { saveAs() }
    }

    /**
     * @author Griefed
     */
    private fun saveAs() {
        tabbedConfigsTab.saveAs()
    }
}