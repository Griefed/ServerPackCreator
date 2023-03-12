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
package de.griefed.serverpackcreator.gui.window.menu.file

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import javax.swing.JFrame
import javax.swing.JTextPane
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyledDocument

/**
 * Menu item to upload the currently selected configuration to the in the [ApiProperties.hasteBinServerUrl] configured
 * HasteBin server.
 *
 * @author Griefed
 */
class ConfigToHasteBinItem(
    private val configsTabs: ConfigsTab,
    private val webUtilities: WebUtilities,
    private val apiProperties: ApiProperties,
    guiProps: GuiProps,
    mainFrame: JFrame
) : HasteBinMenuItem(Gui.menubar_gui_menuitem_uploadconfig.toString(), mainFrame, guiProps, webUtilities) {
    private val log = cachedLoggerOf(this.javaClass)
    private val configDocument: StyledDocument = DefaultStyledDocument()
    private val configAttributeSet: SimpleAttributeSet = SimpleAttributeSet()
    private val configWindowTextPane: JTextPane = JTextPane(configDocument)
    private val tempFile = File(apiProperties.workDirectory, "temp.conf")

    init {
        addActionListener { uploadConfig() }
    }

    private fun uploadConfig() {
        if (webUtilities.hasteBinPreChecks(
                File(apiProperties.homeDirectory, "serverpackcreator.conf")
            )
        ) {
            configsTabs.selectedEditor!!.getCurrentConfiguration().save(tempFile)
            val urlToHasteBin: String = webUtilities.createHasteBinFromFile(tempFile)
            val textContent = "URL: %s".format(urlToHasteBin)
            try {
                configDocument.insertString(0, textContent, configAttributeSet)
            } catch (ex: BadLocationException) {
                log.error("Error inserting text into aboutDocument.", ex)
            }
            displayUploadUrl(urlToHasteBin, configWindowTextPane)
        } else {
            fileTooLargeDialog()
        }
    }
}