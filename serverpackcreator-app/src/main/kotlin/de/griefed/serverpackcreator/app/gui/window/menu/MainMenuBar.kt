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
package de.griefed.serverpackcreator.app.gui.window.menu

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.UpdateDialogs
import de.griefed.serverpackcreator.app.gui.window.menu.about.AboutMenu
import de.griefed.serverpackcreator.app.gui.window.menu.edit.EditMenu
import de.griefed.serverpackcreator.app.gui.window.menu.file.FileMenu
import de.griefed.serverpackcreator.app.gui.window.menu.view.ViewMenu
import de.griefed.serverpackcreator.app.updater.MigrationManager
import javax.swing.JMenuBar

/**
 * Main menubar displaying and allowing for various actions, such as config loading and saving, theme changing, opening
 * various directories, etc. etc...
 *
 * @author Griefed
 */
class MainMenuBar(
    guiProps: GuiProps,
    apiWrapper: ApiWrapper,
    updateDialogs: UpdateDialogs,
    mainFrame: MainFrame,
    migrationManager: MigrationManager,
    themeManager: ThemeManager
) {
    val menuBar: JMenuBar = JMenuBar()
    private val file = FileMenu(mainFrame.mainPanel.tabbedConfigsTab,apiWrapper.apiProperties,mainFrame,apiWrapper.utilities, guiProps)
    private val edit = EditMenu(apiWrapper.apiProperties, guiProps,mainFrame,mainFrame.mainPanel.tabbedConfigsTab)
    private val view = ViewMenu(apiWrapper, themeManager)
    private val about = AboutMenu(
        apiWrapper.utilities.webUtilities,
        updateDialogs,
        apiWrapper,
        migrationManager,
        guiProps,
        mainFrame
    )

    init {
        menuBar.add(file)
        menuBar.add(edit)
        menuBar.add(view)
        menuBar.add(about)
        menuBar.add(updateDialogs.updateButton)
    }

    /**
     * @author Griefed
     */
    fun displayMigrationMessages() {
        about.displayMigrationMessages()
    }

    /**
     * @author Griefed
     */
    fun showFallbacksUpdatedMessage() {
        edit.showFallbacksUpdatedMessage()
    }

    /**
     * @author Griefed
     */
    fun showTip() {
        about.showTip()
    }
}