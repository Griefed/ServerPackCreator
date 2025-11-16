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
package de.griefed.serverpackcreator.app.gui.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.swing.DropMode
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
    dropType: FileFieldDropType,
    textField: JTextField = JTextField(text),
    horizontalScrollbarVisibility: Int = HORIZONTAL_SCROLLBAR_AS_NEEDED
) : ScrollTextField(guiProps, text, null, textField, horizontalScrollbarVisibility) {
    constructor(guiProps: GuiProps,file: File, dropType: FileFieldDropType, documentChangeListener: DocumentChangeListener) : this(guiProps,file.absolutePath, dropType) {
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

        textField.dropMode = DropMode.INSERT
        textField.dropTarget = object : DropTarget() {
            override fun drop(event: DropTargetDropEvent?) {
                val transferable = event?.transferable ?: return
                if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return
                }
                val files : List<File>

                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
                    files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                } catch (e: Exception) {
                    return
                }

                if (files.size != 1) {
                    return
                }

                val file = files[0]

                when (dropType) {
                    FileFieldDropType.FILE -> {
                        if (!file.isFile || file.isDirectory) {
                            return
                        }
                    }

                    FileFieldDropType.FOLDER -> {
                        if (!file.isDirectory) {
                            return
                        }
                    }

                    FileFieldDropType.FOLDER_OR_ZIP -> {
                        if (!file.isDirectory && !file.name.endsWith(".zip", ignoreCase = true)) {
                            return
                        }
                    }

                    FileFieldDropType.IMAGE -> {
                        if (!file.isFile || file.isDirectory ||
                            (!file.name.endsWith(".png", ignoreCase = true) &&
                                    !file.name.endsWith(".jpg", ignoreCase = true) &&
                                    !file.name.endsWith(".jpeg", ignoreCase = true) &&
                                    !file.name.endsWith(".bmp", ignoreCase = true))) {
                            return
                        }
                    }

                    FileFieldDropType.PROPERTIES -> {
                        if (!file.isFile || file.isDirectory ||
                            !file.name.endsWith(".properties", ignoreCase = true)) {
                            return
                        }
                    }
                }

                this@ScrollTextFileField.file = file
            }
        }
    }
}