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
package de.griefed.serverpackcreator.app.gui.window.menu.view

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import java.io.File
import javax.swing.JMenuItem

/**
 * Menu item to open the ServerPackCreator log in the users default app.
 *
 * @author Griefed
 */
class ServerPackCreatorLogItem(
    private val apiProperties: ApiProperties
) : JMenuItem(Translations.menubar_gui_menuitem_spclog.toString()) {
    init {
        this.addActionListener { openServerPackCreatorLog() }
    }

    /**
     * @author Griefed
     */
    private fun openServerPackCreatorLog() {
        FileUtilities.openFile(File(apiProperties.logsDirectory, "serverpackcreator.log"))
    }
}