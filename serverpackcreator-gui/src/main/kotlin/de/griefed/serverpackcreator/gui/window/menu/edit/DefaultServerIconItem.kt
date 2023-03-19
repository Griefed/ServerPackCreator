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
package de.griefed.serverpackcreator.gui.window.menu.edit

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import java.io.File
import javax.swing.JMenuItem

/**
 * Open the default server-icon.png in the users default app.
 *
 * @author Griefed
 */
class DefaultServerIconItem(private val fileUtilities: FileUtilities, apiProperties: ApiProperties) :
    JMenuItem(Gui.menubar_gui_menuitem_servericon.toString()) {
    private val icon = File(apiProperties.serverFilesDirectory, "server-icon.png")

    init {
        addActionListener { openIcon() }
    }

    private fun openIcon() {
        fileUtilities.openFile(icon)
    }
}