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
package de.griefed.serverpackcreator.gui.window.menu.about

import Gui
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.UpdateDialogs
import de.griefed.serverpackcreator.updater.MigrationManager
import javax.swing.JMenu
import javax.swing.JSeparator

/**
 * About menu for opening various webpages in the users browser, such as a how-to guide, GitHub releases / issues and
 * for performing on-demand update checks.
 *
 * @author Griefed
 */
class AboutMenu(
    webUtilities: WebUtilities,
    updateDialogs: UpdateDialogs,
    apiWrapper: ApiWrapper,
    migrationManager: MigrationManager,
    guiProps: GuiProps,
    mainFrame: MainFrame,
    updateButton: BalloonTipButton
) : JMenu(Gui.menubar_gui_menu_about.toString()) {

    private val update = UpdateCheckItem(updateDialogs,updateButton)
    private val migration = MigrationInfoItem(apiWrapper, migrationManager, guiProps, mainFrame)
    private val help = WikiHelpItem(webUtilities)
    private val how = WikiHowToItem(webUtilities)
    private val page = GitHubPageItem(webUtilities)
    private val issues = GitHubIssuesItem(webUtilities)
    private val releases = GitHubReleasesItem(webUtilities)
    private val discord = DiscordItem(webUtilities)
    private val donate = DonationsItem(webUtilities)
    private val thirdparty = ThirdPartyNoticesItem(mainFrame,guiProps)

    init {
        add(update)
        add(migration)
        add(JSeparator())
        add(help)
        add(how)
        add(JSeparator())
        add(page)
        add(issues)
        add(releases)
        add(JSeparator())
        add(discord)
        add(JSeparator())
        add(donate)
        add(JSeparator())
        add(thirdparty)
    }

    /**
     * @author Griefed
     */
    fun displayMigrationMessages() {
        migration.displayMigrationMessages()
    }
}