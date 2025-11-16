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
package de.griefed.serverpackcreator.app.gui.window.menu.about

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.UpdateDialogs
import de.griefed.serverpackcreator.app.updater.MigrationManager
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
    mainFrame: MainFrame
) : JMenu(Translations.menubar_gui_menu_about.toString()) {

    private val update = UpdateCheckItem(updateDialogs)
    private val migration = MigrationInfoItem(apiWrapper, migrationManager, guiProps, mainFrame)
    private val help = WikiHelpItem(webUtilities)
    private val page = GitHubPageItem(webUtilities)
    private val issues = GitHubIssuesItem(webUtilities)
    private val releases = GitHubReleasesItem(webUtilities)
    private val discord = DiscordItem(webUtilities)
    private val donate = DonationsItem(webUtilities)
    private val thirdParty = ThirdPartyNoticesItem(mainFrame, guiProps)
    private val tipOfTheDayItem = TipOfTheDayItem(guiProps, mainFrame)
    private val stepByStepItem = StepByStepItem(mainFrame)

    init {
        add(update)
        add(migration)
        add(JSeparator())
        add(stepByStepItem)
        add(help)
        add(tipOfTheDayItem)
        add(JSeparator())
        add(page)
        add(issues)
        add(releases)
        add(JSeparator())
        add(discord)
        add(JSeparator())
        add(donate)
        add(thirdParty)
    }

    /**
     * @author Griefed
     */
    fun displayMigrationMessages() {
        migration.displayMigrationMessages()
    }

    /**
     * @author Griefed
     */
    fun showTip() {
        tipOfTheDayItem.showTip()
    }
}