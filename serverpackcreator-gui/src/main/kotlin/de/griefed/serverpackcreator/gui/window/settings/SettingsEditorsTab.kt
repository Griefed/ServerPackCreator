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
package de.griefed.serverpackcreator.gui.window.settings

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.DocumentChangeListener
import de.griefed.serverpackcreator.gui.components.TabPanel
import de.griefed.serverpackcreator.gui.themes.ThemeManager
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.gui.window.settings.components.SettingsCheckTimer
import de.griefed.serverpackcreator.gui.window.settings.components.SettingsTitle
import java.awt.BorderLayout
import java.awt.event.ActionListener
import javax.swing.event.ChangeListener
import javax.swing.event.DocumentEvent
import javax.swing.event.TableModelListener

/**
 * @author Griefed
 */
class SettingsEditorsTab(
    guiProps: GuiProps,
    apiProperties: ApiProperties,
    mainFrame: MainFrame,
    themeManager: ThemeManager,
    controlPanel: ControlPanel
) :
    TabPanel() {

    val settingsHandling = SettingsHandling(guiProps, this, apiProperties, mainFrame, controlPanel)
    private val componentResizer = ComponentResizer()
    private val checkTimer = SettingsCheckTimer(250, this, guiProps)
    private val documentChangeListener = object : DocumentChangeListener {
        override fun update(e: DocumentEvent) {
            checkTimer.restart()
        }
    }
    private val actionListener = ActionListener { checkTimer.restart() }
    private val changeListener = ChangeListener { checkTimer.restart() }
    private val tableModelListener = TableModelListener { checkTimer.restart() }

    val global = GlobalSettings(guiProps, apiProperties, componentResizer, mainFrame, documentChangeListener, actionListener, tableModelListener, controlPanel)
    val webservice = WebserviceSettings(guiProps, apiProperties, mainFrame, documentChangeListener, changeListener, controlPanel)
    val gui = GuiSettings(guiProps, actionListener, changeListener, themeManager, controlPanel)
    val title = SettingsTitle(guiProps)

    init {
        tabs.add(global)
        tabs.setTabComponentAt(tabs.tabCount - 1, global.title)
        tabs.add(gui)
        tabs.setTabComponentAt(tabs.tabCount - 1, gui.title)
        tabs.add(webservice)
        tabs.setTabComponentAt(tabs.tabCount - 1, webservice.title)
        tabs.selectedIndex = 0
        panel.add(settingsHandling.panel, BorderLayout.SOUTH)
    }
}
