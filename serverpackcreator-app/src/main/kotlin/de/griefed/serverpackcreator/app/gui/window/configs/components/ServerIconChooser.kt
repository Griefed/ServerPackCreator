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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import Translations
import de.griefed.serverpackcreator.app.gui.components.BaseFileChooser
import java.awt.Dimension
import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * File-chooser allowing a user to select the server-icon from their filesystem.
 *
 * @author Griefed
 */
class ServerIconChooser(current: File?, dimension: Dimension) : BaseFileChooser() {
    constructor(dimension: Dimension) : this(null, dimension)

    init {
        currentDirectory = current
        isFileHidingEnabled = false
        dialogTitle = Translations.createserverpack_gui_createserverpack_chooser_icon_title.toString()
        fileSelectionMode = FILES_ONLY
        fileFilter = FileNameExtensionFilter(
            Translations.createserverpack_gui_createserverpack_chooser_icon_filter.toString(), "png", "jpg", "jpeg", "bmp"
        )
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
        preferredSize = dimension
    }
}