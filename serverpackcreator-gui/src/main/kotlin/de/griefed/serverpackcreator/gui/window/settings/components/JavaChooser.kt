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
package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.components.BaseFileChooser
import java.io.File

/**
 * Customized filechooser for picking the Java executable with which server installations will be performed.
 *
 * @author Griefed
 */
class JavaChooser(apiProperties: ApiProperties, title: String) : BaseFileChooser() {
    init {
        currentDirectory = File(apiProperties.javaPath).absoluteFile.parentFile
        dialogTitle = title
        fileSelectionMode = FILES_ONLY
        isAcceptAllFileFilterUsed = true
        isMultiSelectionEnabled = false
        dialogType = SAVE_DIALOG
    }
}