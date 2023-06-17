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
package de.griefed.serverpackcreator.gui.utilities

import java.awt.Image
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon

/**
 * Various utilities revolving around working with images and icons.
 *
 * @author Griefed
 */
class ImageUtilities {
    companion object {
        /**
         * Create an [Image] from a stream acquired from a resource, where [name] is the path to the resource in you JAR
         * or classpath.
         *
         * @author Griefed
         */
        @Throws(NullPointerException::class)
        fun imageFromResourceStream(clazz: Class<*>, name: String): Image {
            return ImageIcon(ImageIO.read(clazz.getResourceAsStream(name))).image
        }

        /**
         * Create an [ImageIcon] from a stream acquired from a resource, where [name] is the path to the resource in
         * you JAR or classpath.
         *
         * @author Griefed
         */
        @Throws(NullPointerException::class)
        fun imageIconFromResourceStream(clazz: Class<*>, name: String): ImageIcon {
            return ImageIcon(clazz.getResourceAsStream(name).readAllBytes())
        }

        /**
         * Create an image icon from a base64 encoded image.
         *
         * @author Griefed
         */
        fun fromBase64(
            encoded: String,
            width: Int = 24,
            height: Int = 24,
            scaling: Int = Image.SCALE_SMOOTH
        ): ImageIcon {
            return ImageIcon(
                Base64.getDecoder().decode(encoded)
            ).getScaledInstance(width, height, scaling)
        }
    }
}

/**
 * Scale the image icon to the specified [width] and [height], using the specified scaling-method [scaling].
 *
 * @author Griefed
 */
fun ImageIcon.getScaledInstance(width: Int, height: Int, scaling: Int = Image.SCALE_SMOOTH): ImageIcon {
    return ImageIcon(this.image.getScaledInstance(width, height, scaling))
}