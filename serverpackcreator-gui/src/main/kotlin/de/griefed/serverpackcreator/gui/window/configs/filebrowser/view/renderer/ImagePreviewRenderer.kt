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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.renderer

import java.awt.image.BufferedImage
import javax.swing.JSplitPane

/**
 * Image previewer, showing the selected image in its original aspect-ratio, and a 1:1 aspect-ratio as it would be used
 * as the server icon.
 *
 * @author Griefed
 */
class ImagePreviewRenderer(
    split: Int = HORIZONTAL_SPLIT,
    private val regularPreview: ImageRenderer = ImageRenderer(),
    private val serverIconPreview: ImageRenderer = ImageRenderer(isServerIconPreview = true, "Server Icon Preview")
) : JSplitPane(split, regularPreview, serverIconPreview) {

    init {
        isOneTouchExpandable = true
        dividerLocation = 640
        dividerSize = 20
    }

    /**
     * Update the image previews wit the passed [image].
     *
     * @author Griefed
     */
    fun updateImage(image: BufferedImage?) {
        regularPreview.image = image
        serverIconPreview.image = image
    }
}