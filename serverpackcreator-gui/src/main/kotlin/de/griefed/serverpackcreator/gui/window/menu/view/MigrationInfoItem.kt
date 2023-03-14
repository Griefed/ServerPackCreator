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
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Dimension
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.text.*

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
    private val log = cachedLoggerOf(this.javaClass)

    init {
        addActionListener { displayMigrationMessages() }
        displayMigrationMessages()
    }

    /**
     * Display the available migration messages.
     *
     * @author Griefed
     */
    fun displayMigrationMessages() {
        val styledDocument: StyledDocument = DefaultStyledDocument()
        val simpleAttributeSet = SimpleAttributeSet()
        StyleConstants.setBold(simpleAttributeSet, true)
        StyleConstants.setFontSize(simpleAttributeSet, 14)
        StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT)
        styledDocument.setParagraphAttributes(
            0, styledDocument.length, simpleAttributeSet, false
        )
        val jTextPane = JTextPane(styledDocument)
        jTextPane.setCharacterAttributes(simpleAttributeSet, true)

        val messages = StringBuilder()
        if (apiWrapper.apiProperties.apiVersion != "dev") {
            if (migrationManager.migrationMessages.isEmpty()) {
                messages.append("No migrations occurred. :)")
            } else {
                for (message in migrationManager.migrationMessages) {
                    messages.append(message.get()).append("\n")
                }
            }
        } else {
            messages.append("No migrations will occur in a dev-version of SPC.")
        }

        try {
            styledDocument.insertString(0, messages.toString(), simpleAttributeSet)
        } catch (ex: BadLocationException) {
            log.error("Error inserting text into aboutDocument.", ex)
        }

        val scrollPane = JScrollPane(
            jTextPane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        scrollPane.preferredSize = Dimension(800, 400)
        jTextPane.isEditable = false

        DialogUtilities.createDialog(
            scrollPane,
            Gui.migration_message_title.toString(),
            this,
            JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION,
            guiProps.infoIcon,
            true
        )
    }
}