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
package de.griefed.serverpackcreator.gui.window.settings

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.TabPanel
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.gui.window.settings.components.SettingsHandling
import java.awt.BorderLayout

/**
 * TODO docs
 */
class SettingsEditorsTab(guiProps: GuiProps, apiProperties: ApiProperties, mainFrame: MainFrame) :
    TabPanel() {

    private val componentResizer = ComponentResizer()
    val global = GlobalSettings(guiProps, apiProperties, componentResizer, mainFrame)
    val webservice = WebserviceSettings(guiProps, apiProperties,mainFrame)
    val gui = GuiSettings(guiProps)

    init {
        tabs.add(global)
        tabs.setTabComponentAt(tabs.tabCount - 1, global.title)
        tabs.add(gui)
        tabs.setTabComponentAt(tabs.tabCount - 1, gui.title)
        tabs.add(webservice)
        tabs.setTabComponentAt(tabs.tabCount - 1, webservice.title)
        tabs.selectedIndex = 0
        panel.add(SettingsHandling(guiProps, this, apiProperties, mainFrame).panel, BorderLayout.SOUTH)
    }
}
