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
 * File-chooser to allow a user to select the modpack directory from which the server pack should be generated.
 *
 * @author Griefed
 */
class ModpackChooser(current: File?, dimension: Dimension) : BaseFileChooser() {
    constructor(dimension: Dimension) : this(null, dimension)

    init {
        currentDirectory = current
        isFileHidingEnabled = false
        dialogTitle = Translations.createserverpack_gui_buttonmodpackdir_title.toString()
        fileSelectionMode = FILES_AND_DIRECTORIES
        fileFilter = FileNameExtensionFilter(
            Translations.createserverpack_gui_createserverpack_chooser_modpack_filter.toString(), "zip"
        )
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
        preferredSize = dimension
    }
}