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
package de.griefed.serverpackcreator.gui.window.menu

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.UpdateDialogs
import de.griefed.serverpackcreator.gui.window.menu.about.AboutMenu
import de.griefed.serverpackcreator.gui.window.menu.edit.EditMenu
import de.griefed.serverpackcreator.gui.window.menu.file.FileMenu
import de.griefed.serverpackcreator.gui.window.menu.view.ViewMenu
import de.griefed.serverpackcreator.updater.MigrationManager
import javax.swing.JMenuBar

/**
 * Main menubar displaying and allowing for various actions, such as config loading and saving, theme changing, opening
 * various directories, etc. etc..
 *
 * @author Griefed
 */
class MainMenuBar(
    guiProps: GuiProps,
    apiWrapper: ApiWrapper,
    updateDialogs: UpdateDialogs,
    mainFrame: MainFrame,
    migrationManager: MigrationManager
) {
    val menuBar: JMenuBar = JMenuBar()
    private val updateButton = BalloonTipButton(null, guiProps.updateAnimation, "Update available!", guiProps)

    init {
        updateButton.isBorderPainted = false
        updateButton.isContentAreaFilled = false
        updateButton.isVisible = updateDialogs.update.isPresent

        menuBar.add(
            FileMenu(
                mainFrame.mainPanel.tabbedConfigsTab,
                apiWrapper.apiProperties,
                mainFrame,
                apiWrapper.utilities!!,
                guiProps
            )
        )
        menuBar.add(
            EditMenu(
                apiWrapper.apiProperties,
                guiProps,
                mainFrame,
                apiWrapper.fileUtilities,
                mainFrame.mainPanel.tabbedConfigsTab
            )
        )
        menuBar.add(
            ViewMenu(
                apiWrapper
            )
        )
        menuBar.add(
            AboutMenu(
                apiWrapper.utilities!!.webUtilities,
                updateDialogs,
                apiWrapper,
                migrationManager,
                guiProps,
                mainFrame,
                updateButton
            )
        )
        menuBar.add(updateButton)
    }
}