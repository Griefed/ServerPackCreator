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
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.updater.MigrationManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextPane

/**
 * Menu item to display any available migration messages to the user.
 *
 * @author Griefed
 */
class MigrationInfoItem(
    apiWrapper: ApiWrapper,
    migrationManager: MigrationManager,
    private val guiProps: GuiProps,
    private val mainFrame: MainFrame
) : JMenuItem(Translations.menubar_gui_migration.toString()) {
    private val migrationWindowTextPane: JTextPane = JTextPane()
    private val migrationScrollPane = JScrollPane(
        migrationWindowTextPane,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )
    private val lineBreak = "\n"

    init {
        migrationWindowTextPane.isEditable = false
        if (!apiWrapper.apiProperties.devBuild && !apiWrapper.apiProperties.preRelease) {
            for (message in migrationManager.migrationMessages) {
                addText(message.get())
                addLineBreak()
            }
        } else if (apiWrapper.apiProperties.devBuild || apiWrapper.apiProperties.preRelease) {
            addText("Migrations will not occur in a dev, alpha or beta version of ServerPackCreator.")
            addLineBreak(2)
            addText("This message serves as a test message for development.")
            addLineBreak(2)
            addText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut")
            addLineBreak()
            addText("labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris")
            addLineBreak()
            addText("nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit")
            addLineBreak()
            addText("esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt")
            addLineBreak()
            addText("in culpa qui officia deserunt mollit anim id est laborum.")
            addLineBreak()
            addText("Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto")
            addLineBreak(2)
            addText("beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.")
            addLineBreak(2)
            addText("Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.")
            addLineBreak(2)
            addText("Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit")
            addLineBreak(2)
            addText("esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?")
            addLineBreak()
            addText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut")
            addLineBreak()
            addText("labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris")
            addLineBreak()
            addText("nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit")
            addLineBreak()
            addText("esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt")
            addLineBreak()
            addText("in culpa qui officia deserunt mollit anim id est laborum.")
            addLineBreak()
            addText("Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto")
            addLineBreak(2)
            addText("beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.")
            addLineBreak(2)
            addText("Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.")
            addLineBreak(2)
            addText("Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit")
            addLineBreak(2)
            addText("esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?")
        }
        this.addActionListener { displayMigrationMessages() }
    }

    /**
     * Display the available migration messages.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun displayMigrationMessages() {
        migrationWindowTextPane.font = guiProps.font
        if (migrationWindowTextPane.text.isNotBlank()) {
            GlobalScope.launch(Dispatchers.Swing) {
                DialogUtilities.createDialog(
                    migrationScrollPane,
                    Translations.migration_message_title.toString(),
                    mainFrame.frame,
                    JOptionPane.INFORMATION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    guiProps.infoIcon,
                    resizable = true, display = true,
                    options = null, initialValue = null,
                    width = 800, height = 600
                )
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun addText(text: String) {
        migrationWindowTextPane.text += text
    }

    /**
     * @author Griefed
     */
    private fun addLineBreak(amount: Int = 1) {
        for (i in 0 until amount) {
            addText(lineBreak)
        }
    }
}