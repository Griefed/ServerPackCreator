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
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import javax.swing.JFrame
import javax.swing.JTextPane

/**
 * Menu item to upload the currently selected configuration to the in the [ApiProperties.hasteBinServerUrl] configured
 * HasteBin server.
 *
 * @author Griefed
 */
class ConfigToHasteBinItem(
    private val tabbedConfigsTabs: TabbedConfigsTab,
    private val webUtilities: WebUtilities,
    guiProps: GuiProps,
    mainFrame: JFrame
) : HasteBinMenuItem(Translations.menubar_gui_menuitem_uploadconfig.toString(), mainFrame, guiProps, webUtilities) {
    private val configWindowTextPane: JTextPane = JTextPane()

    init {
        configWindowTextPane.isEditable = false
        addActionListener { uploadConfig() }
    }

    /**
     * @author Griefed
     */
    private fun uploadConfig() {
        if (tabbedConfigsTabs.selectedEditor == null || tabbedConfigsTabs.selectedEditor!!.configFile == null) {
            errorDialog(Translations.menubar_gui_noconfig_message.toString(), Translations.menubar_gui_noconfig_title.toString())
            return
        }
        if (webUtilities.hasteBinPreChecks(tabbedConfigsTabs.selectedEditor!!.configFile!!)) {
            val urlToHasteBin: String =
                webUtilities.createHasteBinFromFile(tabbedConfigsTabs.selectedEditor!!.configFile!!)
            val textContent = "URL: %s".format(urlToHasteBin)
            configWindowTextPane.text = textContent
            displayUploadUrl(urlToHasteBin, configWindowTextPane)
        } else {
            errorDialog(Translations.menubar_gui_filetoolarge.toString(), Translations.menubar_gui_filetoolargetitle.toString())
        }
    }
}