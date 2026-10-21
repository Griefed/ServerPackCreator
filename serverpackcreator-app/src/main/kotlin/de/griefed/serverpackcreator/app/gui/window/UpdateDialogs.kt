/* Copyright (C) 2026 Griefed
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
package de.griefed.serverpackcreator.app.gui.window

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import de.griefed.serverpackcreator.app.updater.versionchecker.Update
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionListener
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
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    /** The button that appears once an update is known, animated to draw the eye. */
    val updateButton = BalloonTipButton(null, guiProps.updateAnimation, Translations.update_dialog_available.toString(), guiProps)
    /** Re-runs the check on demand, for the menu entry that asks for one. */
    val updateCheckListener = ActionListener { checkForUpdate() }
    /** The update found, if any. Checked once at construction and then re-assigned by [updateCheckListener]. */
    var update: Optional<Update> = updateChecker.checkForUpdate(
        apiProperties.apiVersion,
        apiProperties.isCheckingForPreReleasesEnabled
    )
        private set

    init {
        updateButton.isBorderPainted = false
        updateButton.isContentAreaFilled = false
        updateButton.isVisible = update.isPresent
        updateButton.addActionListener(updateCheckListener)
    }

    /**
     * If an update for ServerPackCreator is available, display a dialog letting the user choose whether they want to
     * visit the releases page or do nothing.
     *
     * Returns `true` if an update was found and the dialog displayed.
     *
     * @author Griefed
     */
    private fun displayUpdateDialog(): Boolean {
        return if (update.isPresent) {
            val textContent: String = Translations.update_dialog_new(update.get().url())
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
            options[0] = Translations.update_dialog_yes.toString()
            options[1] = Translations.update_dialog_no.toString()
            options[2] = Translations.update_dialog_clipboard.toString()
            try {
                styledDocument.insertString(0, textContent, simpleAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            when (DialogUtilities.createShowGet(
                jTextPane,
                Translations.update_dialog_available.toString(),
                mainFrame,
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon,
                resizable = true,
                options,
                options[0]
            )) {
                0 -> try {
                    webUtilities.openLinkInBrowser(update.get().url().toURI())
                } catch (ex: Exception) {
                    log.error("Error performing action.", ex)
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
     * Checks for a newer release and either offers it or reports that none is available.
     *
     * @author Griefed
     */
    fun checkForUpdate() {
        update = updateChecker.checkForUpdate(apiProperties.apiVersion, apiProperties.isCheckingForPreReleasesEnabled)
        updateButton.isVisible = update.isPresent
        if (!displayUpdateDialog()) {
            DialogUtilities.createDialog(
                Translations.menubar_gui_menuitem_updates_none.toString() + "   ",
                Translations.menubar_gui_menuitem_updates_none_title.toString() + "   ",
                mainFrame,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                guiProps.infoIcon,
                resizable = true
            )
        }
    }

}