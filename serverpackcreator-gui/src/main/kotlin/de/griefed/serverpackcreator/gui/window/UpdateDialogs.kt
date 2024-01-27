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
package de.griefed.serverpackcreator.gui.window

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.updater.UpdateChecker
import de.griefed.versionchecker.Update
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URISyntaxException
import java.util.*
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JTextPane
import javax.swing.text.*

/**
 * Utility-class for checking and displaying dialogs when an update is available or not.
 *
 * @author Griefed
 */
@Suppress("MemberVisibilityCanBePrivate")
class UpdateDialogs(
    private val guiProps: GuiProps,
    private val webUtilities: WebUtilities,
    private val apiProperties: ApiProperties,
    private val updateChecker: UpdateChecker,
    private val mainFrame: JFrame
) {
    private val log = cachedLoggerOf(this.javaClass)
    var update: Optional<Update> = updateChecker.checkForUpdate(
        apiProperties.apiVersion,
        apiProperties.isCheckingForPreReleasesEnabled
    )
        private set

    /**
     * If an update for ServerPackCreator is available, display a dialog letting the user choose whether they want to
     * visit the releases page or do nothing.
     *
     * @return `true` if an update was found and the dialog displayed.
     * @author Griefed
     */
    private fun displayUpdateDialog(): Boolean {
        return if (update.isPresent) {
            val textContent: String = Gui.update_dialog_new(update.get().url())
            val styledDocument: StyledDocument = DefaultStyledDocument()
            val simpleAttributeSet = SimpleAttributeSet()
            val jTextPane = JTextPane(styledDocument)
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val options = arrayOfNulls<String>(3)
            StyleConstants.setBold(simpleAttributeSet, true)
            StyleConstants.setFontSize(simpleAttributeSet, 14)
            jTextPane.setCharacterAttributes(simpleAttributeSet, true)
            StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT)
            styledDocument.setParagraphAttributes(
                0, styledDocument.length, simpleAttributeSet, false
            )
            jTextPane.isOpaque = false
            jTextPane.isEditable = false
            options[0] = Gui.update_dialog_yes.toString()
            options[1] = Gui.update_dialog_no.toString()
            options[2] = Gui.update_dialog_clipboard.toString()
            try {
                styledDocument.insertString(0, textContent, simpleAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            when (DialogUtilities.createShowGet(
                jTextPane,
                Gui.update_dialog_available.toString(),
                mainFrame,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon,
                resizable = true,
                options,
                options[0]
            )) {
                0 -> try {
                    webUtilities.openLinkInBrowser(update.get().url().toURI())
                } catch (ex: RuntimeException) {
                    log.error("Error opening browser.", ex)
                } catch (ex: URISyntaxException) {
                    log.error("Error opening browser.", ex)
                }

                1 -> clipboard.setContents(StringSelection(update.get().url().toString()), null)
                else -> {}
            }
            true
        } else {
            false
        }
    }

    /**
     * @author Griefed
     */
    fun checkForUpdate(): Boolean {
        update = updateChecker.checkForUpdate(apiProperties.apiVersion, apiProperties.isCheckingForPreReleasesEnabled)
        if (!displayUpdateDialog()) {
            DialogUtilities.createDialog(
                Gui.menubar_gui_menuitem_updates_none.toString() + "   ",
                Gui.menubar_gui_menuitem_updates_none_title.toString() + "   ",
                mainFrame,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                guiProps.infoIcon,
                resizable = true
            )
        }
        return update.isPresent
    }
}