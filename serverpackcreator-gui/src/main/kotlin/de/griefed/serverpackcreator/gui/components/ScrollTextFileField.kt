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
package de.griefed.serverpackcreator.gui.components

import de.griefed.serverpackcreator.gui.GuiProps
import java.io.File
import javax.swing.JTextField

/**
 * Scrollable textfield with file-provisioning. The contained text is note editable directly. Data in this field is
 * supposed to display, retrieve and provide an absolute file path.
 *
 * @author Griefed
 */
class ScrollTextFileField(
    guiProps: GuiProps,
    text: String,
    textField: JTextField = JTextField(text),
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_AS_NEEDED
) : ScrollTextField(guiProps, text, null, textField, horizontalScrollbarVisibility) {
    constructor(guiProps: GuiProps,file: File, documentChangeListener: DocumentChangeListener) : this(guiProps,file.absolutePath) {
        this.addDocumentListener(documentChangeListener)
    }

    var file: File
        get() {
            return File(text).absoluteFile
        }
        set(value) {
            text = value.absolutePath
        }

    init {
        textField.isEditable = guiProps.allowManualEditing
        file = File(text).absoluteFile
    }
}