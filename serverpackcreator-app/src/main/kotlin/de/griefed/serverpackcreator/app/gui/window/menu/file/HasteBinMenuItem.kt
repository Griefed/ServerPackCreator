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
import de.griefed.serverpackcreator.api.utilities.common.WebUtilities
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JTextPane

/**
 * Base from which to create a menu item supposed to upload a given file to the HasteBin server configured in
 * [de.griefed.serverpackcreator.api.ApiProperties.hasteBinServerUrl].
 *
 * @author Griefed
 */
abstract class HasteBinMenuItem(
    title: String,
    private val mainFrame: JFrame,
    private val guiProps: GuiProps,
    private val webUtilities: WebUtilities,
) : JMenuItem(title) {
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    private val hasteBinOptions = arrayOf(
        Translations.createserverpack_gui_about_hastebin_dialog_yes.toString(),
        Translations.createserverpack_gui_about_hastebin_dialog_clipboard.toString(),
        Translations.createserverpack_gui_about_hastebin_dialog_no.toString()
    )

    /**
     * Display the given URL in a text pane.
     *
     * @param urlToHasteBin   The URL, as a String, to display.
     * @param displayTextPane The text pane to display the URL in.
     * @author Griefed
     */
    fun displayUploadUrl(
        urlToHasteBin: String,
        displayTextPane: JTextPane
    ) {
        when (DialogUtilities.createShowGet(
            displayTextPane,
            Translations.createserverpack_gui_about_hastebin_dialog.toString(),
            mainFrame,
            JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
            guiProps.hasteBinIcon,
            resizable = true,
            hasteBinOptions,
            hasteBinOptions[0]
        )) {
            0 -> webUtilities.openLinkInBrowser(URI.create(urlToHasteBin))
            1 -> clipboard.setContents(StringSelection(urlToHasteBin), null)
            else -> {}
        }
    }

    /**
     * Display an error dialog with the given [title] and [message].
     *
     * @author Griefed
     */
    fun errorDialog(message: String, title: String) {
        JOptionPane.showConfirmDialog(
            mainFrame,
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            guiProps.hasteBinIcon
        )
    }
}