/*
 * #####################################################################################################################
 *
 * Addendum from the Java Tips Weblog's About page:
 *
 * It should be noted that none of the code presented here is used in any real application,  (I only do this for a hobby)
 * and therefore you use it at your own risk. Although I must admit I took about 5-10 minutes to do extensive testing, so
 * I doubt you will find many bugs :-).
 *
 * We assume no responsibility for the code. You are free to use and/or modify and/or distribute any or all code posted
 * on the Java Tips Weblog without restriction. A credit in the code comments would be nice, but not in any way mandatory.
 *
 * "Give somebody a fish, and they eat for a day.
 * Teach somebody to fish they eat for life!"
 *
 * In following the philosophy of the above quote, whenever possible, I will also attempt to provide links for related
 * reading to help you better understand the suggested solution. These readings may include tips to help solve your next
 * problem. I may include links to:
 *
 * technical articles
 * tutorials
 * the Java API
 * The Java API is huge, and we do not have time to read it from start to finish. If the examples do not solve your problem,
 * hopefully they can at least give you some ideas and introduce you to new APIs and concepts.
 *
 * Rob
 *
 * #####################################################################################################################
 *
 */
package de.griefed.serverpackcreator.gui.utilities

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import kotlin.math.roundToInt

/**
 * See [Compound Icon](https://tips4java.wordpress.com/2009/03/29/compound-icon/)
 *
 * The CompoundIcon will paint two, or more, Icons as a single Icon. The Icons are painted in
 * the order in which they are added.
 *
 * The Icons are laid out on the specified axis:
 *
 *  * X-Axis (horizontally)
 *  * Y-Axis (vertically)
 *  * Z-Axis (stacked)
 *
 * Create a CompoundIcon specifying all the properties.
 *
 * @param axis       the axis used to lay out the icons for painting Must be one of the Axis
 * enums: X_AXIS, Y_AXIS, Z_Axis.
 * @param gap        the gap between the icons
 * @param alignmentX the X alignment of the icons. Common values are LEFT, CENTER, RIGHT. Can be
 * any value between 0.0 and 1.0
 * @param alignmentY the Y alignment of the icons. Common values are TOP, CENTER, BOTTOM. Can be
 * any value between 0.0 and 1.0
 * @param icons      the Icons to be painted as part of the CompoundIcon
 * @author Rob Camick
 */
@Suppress("unused")
class CompoundIcon(
    private val icons: Array<Icon>,
    private val gap: Int = 12,
    private val axis: Axis = Axis.X_AXIS,
    private var alignmentX: Float = 0.5f,
    private var alignmentY: Float = 0.5f
) : Icon {

    init {
        alignmentX = if (alignmentX > 1.0f) 1.0f else alignmentX.coerceAtLeast(0.0f)
        alignmentY = if (alignmentY > 1.0f) 1.0f else alignmentY.coerceAtLeast(0.0f)
    }

    /**
     * Paint the icons of this compound icon at the specified location
     *
     * @param c The component on which the icon is painted
     * @param g The graphics context
     * @param xCoord The X coordinate of the icon's top-left corner
     * @param yCoord The Y coordinate of the icon's top-left corner
     * @author Rob Camick
     */
    override fun paintIcon(
        c: Component,
        g: Graphics,
        xCoord: Int,
        yCoord: Int
    ) {
        var x = xCoord
        var y = yCoord
        when (axis) {
            Axis.X_AXIS -> {
                val height = iconHeight
                for (icon in icons) {
                    val iconY = getOffset(height, icon.iconHeight, alignmentY)
                    icon.paintIcon(c, g, x, y + iconY)
                    x += icon.iconWidth + gap
                }
            }

            Axis.Y_AXIS -> {
                val width = iconWidth
                for (icon in icons) {
                    val iconX = getOffset(width, icon.iconWidth, alignmentX)
                    icon.paintIcon(c, g, x + iconX, y)
                    y += icon.iconHeight + gap
                }
            }

            else -> {
                /* must be Z_AXIS */
                val width = iconWidth
                val height = iconHeight
                for (icon in icons) {
                    val iconX = getOffset(width, icon.iconWidth, alignmentX)
                    val iconY = getOffset(height, icon.iconHeight, alignmentY)
                    icon.paintIcon(c, g, x + iconX, y + iconY)
                }
            }
        }
    }

    /**
     * Gets the width of this icon.
     *
     * @return The width of the icon in pixels.
     * @author Rob Camick
     */
    override fun getIconWidth(): Int {
        var width = 0

        //  Add the width of all Icons while also including the gap
        if (axis == Axis.X_AXIS) {
            width += (icons.size - 1) * gap
            for (icon in icons) {
                width += icon.iconWidth
            }
        } else {

            //  Just find the maximum width
            for (icon in icons) {
                width = width.coerceAtLeast(icon.iconWidth)
            }
        }
        return width
    }

    /**
     * Gets the height of this icon.
     *
     * @return The height of the icon in pixels.
     * @author Rob Camick
     */
    override fun getIconHeight(): Int {
        var height = 0

        //  Add the height of all Icons while also including the gap
        if (axis == Axis.Y_AXIS) {
            height += (icons.size - 1) * gap
            for (icon in icons) {
                height += icon.iconHeight
            }
        } else {
            //  Just find the maximum height
            for (icon in icons) {
                height = height.coerceAtLeast(icon.iconHeight)
            }
        }
        return height
    }

    /**
     *  When the icon value is smaller than the maximum value of all icons the
     *  icon needs to be aligned appropriately. Calculate the offset to be used
     *  when painting the icon to achieve the proper alignment.
     *  @author Rob Camick
     */
    private fun getOffset(
        maxValue: Int,
        iconValue: Int,
        alignment: Float
    ): Int {
        val offset = (maxValue - iconValue) * alignment
        return offset.roundToInt()
    }

    enum class Axis {
        X_AXIS, Y_AXIS, Z_AXIS
    }
}