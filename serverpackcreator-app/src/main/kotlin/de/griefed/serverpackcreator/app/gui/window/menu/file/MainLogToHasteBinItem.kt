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
package de.griefed.serverpackcreator.app.gui.window.menu.file

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import javax.swing.JFrame
import javax.swing.JTextPane
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyledDocument

/**
 * Menu item to upload the current serverpackcreator.log to the in the [ApiProperties.hasteBinServerUrl] configured
 * HasteBin server.
 *
 * @author Griefed
 */
class MainLogToHasteBinItem(
    private val webUtilities: WebUtilities,
    private val apiProperties: ApiProperties,
    guiProps: GuiProps,
    mainFrame: JFrame
) : HasteBinMenuItem(Translations.menubar_gui_menuitem_uploadlog.toString(), mainFrame, guiProps, webUtilities) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val spcLogDocument: StyledDocument = DefaultStyledDocument()
    private val spcLogAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val spcLogWindowTextPane: JTextPane = JTextPane(spcLogDocument)

    init {
        this.addActionListener { uploadLog() }
    }

    /**
     * @author Griefed
     */
    private fun uploadLog() {
        if (webUtilities.hasteBinPreChecks(
                File(apiProperties.logsDirectory, "serverpackcreator.log")
            )
        ) {
            val urlToHasteBin: String = webUtilities.createHasteBinFromFile(
                File(apiProperties.logsDirectory, "serverpackcreator.log")
            )
            val textContent = "URL: %s".format(urlToHasteBin)
            try {
                spcLogDocument.insertString(0, textContent, spcLogAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            displayUploadUrl(urlToHasteBin, spcLogWindowTextPane)
        } else {
            errorDialog(Translations.menubar_gui_filetoolarge.toString(),Translations.menubar_gui_filetoolargetitle.toString())
        }
    }
}