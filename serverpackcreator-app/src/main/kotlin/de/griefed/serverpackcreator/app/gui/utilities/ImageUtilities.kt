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
@file:Suppress("unused")

package de.griefed.serverpackcreator.app.gui.utilities

import java.awt.Dimension
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
            return ImageIcon(clazz.getResourceAsStream(name)!!.readAllBytes())
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

/**
 * Scale the image, keeping the aspect ratio, to the given [size]. By default, the width of the image will be changed to
 * the desired [size]. If you want to change the height of the image to the desired [size], then set [scaleBy] to [ScaleBy.HEIGHT].
 *
 * @author Griefed
 */
fun ImageIcon.getAspectRatioScaledInstance(size: Int, scaleBy: ScaleBy = ScaleBy.WIDTH, scaling: Int = Image.SCALE_SMOOTH): ImageIcon {
    val imgSize = Dimension(iconWidth, iconHeight)
    val widthRatio: Double
    val heightRatio: Double
    var newHeight: Int
    var newWidth: Int
    when (scaleBy) {
        ScaleBy.WIDTH -> {
            newWidth = size
            widthRatio = size.toDouble() / imgSize.width
            newHeight = (iconHeight * widthRatio).toInt()
        }
        ScaleBy.HEIGHT -> {
            newHeight = size
            heightRatio = size.toDouble() / imgSize.height
            newWidth = (iconHeight * heightRatio).toInt()
        }
    }
    return getScaledInstance(newWidth,newHeight,scaling)
}

/**
 * @author Griefed
 */
enum class ScaleBy {
    WIDTH,
    HEIGHT
}