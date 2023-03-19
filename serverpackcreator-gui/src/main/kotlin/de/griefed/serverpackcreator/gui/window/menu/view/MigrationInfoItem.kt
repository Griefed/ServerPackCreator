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
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.updater.MigrationManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JScrollPane

/**
 * Menu item to display any available migration messages to the user.
 *
 * @author Griefed
 */
class MigrationInfoItem(
    private val apiWrapper: ApiWrapper,
    private val migrationManager: MigrationManager,
    private val guiProps: GuiProps
) : JMenuItem(Gui.menubar_gui_migration.toString()) {

    init {
        addActionListener { displayMigrationMessages() }
        displayMigrationMessages()
    }

    /**
     * Display the available migration messages.
     *
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun displayMigrationMessages() {
        val label = JLabel("<html>")
        val lineBreak = "<br>"
        if (!apiWrapper.apiProperties.devBuild && !apiWrapper.apiProperties.preRelease) {
            for (message in migrationManager.migrationMessages) {
                label.text += message.get()
                label.text += lineBreak
            }
        } else {
            label.text += "<b>Migrations will not occur in a dev, alpha or beta version of ServerPackCreator.</b>"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "<b>This message serves as a test message for development.</b>"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut"
            label.text += lineBreak
            label.text += "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris"
            label.text += lineBreak
            label.text += "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit"
            label.text += lineBreak
            label.text += "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt"
            label.text += lineBreak
            label.text += "in culpa qui officia deserunt mollit anim id est laborum."
            label.text += lineBreak
            label.text += "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."
            label.text += lineBreak
            label.text += lineBreak
            label.text += "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem."
            label.text += lineBreak
            label.text += lineBreak
            label.text += "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?"
            label.text += lineBreak
            label.text += "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut"
            label.text += lineBreak
            label.text += "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris"
            label.text += lineBreak
            label.text += "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit"
            label.text += lineBreak
            label.text += "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt"
            label.text += lineBreak
            label.text += "in culpa qui officia deserunt mollit anim id est laborum."
            label.text += lineBreak
            label.text += "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."
            label.text += lineBreak
            label.text += lineBreak
            label.text += "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem."
            label.text += lineBreak
            label.text += lineBreak
            label.text += "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit"
            label.text += lineBreak
            label.text += lineBreak
            label.text += "esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?"
        }
        label.text += "</html>"
        val scroll =
            JScrollPane(label, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

        if (migrationManager.migrationMessages.isNotEmpty() || (apiWrapper.apiProperties.devBuild || apiWrapper.apiProperties.preRelease)) {
            GlobalScope.launch(Dispatchers.Swing) {
                DialogUtilities.createDialog(
                    scroll,
                    Gui.migration_message_title.toString(),
                    this@MigrationInfoItem,
                    JOptionPane.INFORMATION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    guiProps.infoIcon,
                    true, true,
                    null, null,
                    800, 600
                )
            }
        }
    }
}