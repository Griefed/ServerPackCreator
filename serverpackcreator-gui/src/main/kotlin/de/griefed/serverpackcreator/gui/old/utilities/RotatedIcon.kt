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
package de.griefed.serverpackcreator.gui.old.utilities

import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Heya, Griefed here. This is yet another miracle by Rob Camick. This man is a godsend.<br></br> See [Rotated Icon](https://tips4java.wordpress.com/2009/04/06/rotated-icon/)<br></br>
 *
 *
 * The RotatedIcon allows you to change the orientation of an Icon by rotating the Icon before
 * it is painted. This class supports the following orientations:
 *
 *
 *  * DOWN - rotated 90 degrees
 *  * UP (default) - rotated -90 degrees
 *  * UPSIDE_DOWN - rotated 180 degrees
 *  * ABOUT_CENTER - the icon is rotated by the specified degrees about its center.
 *
 *
 * @author Rob Camick
 */
class RotatedIcon constructor(
    private val icon: Icon,
    private var degrees: Double = 0.0,
    private val rotate: Rotate = Rotate.UP,
    private var isCircularIcon: Boolean = false
) : Icon {

    /**
     * Paint the icons of this compound icon at the specified location
     *
     * @param c The component on which the icon is painted
     * @param g The graphics context
     * @param x The X coordinate of the icon's top-left corner
     * @param y The Y coordinate of the icon's top-left corner
     */
    override fun paintIcon(
        c: Component,
        g: Graphics,
        x: Int,
        y: Int
    ) {
        val g2 = g.create() as Graphics2D
        val cWidth = icon.iconWidth / 2
        val cHeight = icon.iconHeight / 2
        val xAdjustment = if (icon.iconWidth % 2 == 0) 0 else -1
        val yAdjustment = if (icon.iconHeight % 2 == 0) 0 else -1
        when (rotate) {
            Rotate.DOWN -> {
                g2.translate(x + cHeight, y + cWidth)
                g2.rotate(Math.toRadians(90.0))
                icon.paintIcon(c, g2, -cWidth, yAdjustment - cHeight)
            }

            Rotate.UP -> {
                g2.translate(x + cHeight, y + cWidth)
                g2.rotate(Math.toRadians(-90.0))
                icon.paintIcon(c, g2, xAdjustment - cWidth, -cHeight)
            }

            Rotate.UPSIDE_DOWN -> {
                g2.translate(x + cWidth, y + cHeight)
                g2.rotate(Math.toRadians(180.0))
                icon.paintIcon(c, g2, xAdjustment - cWidth, yAdjustment - cHeight)
            }

            Rotate.ABOUT_CENTER -> {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setClip(x, y, iconWidth, iconHeight)
                g2.translate(
                    (iconWidth - icon.iconWidth) / 2, (iconHeight - icon.iconHeight) / 2
                )
                g2.rotate(Math.toRadians(degrees), (x + cWidth).toDouble(), (y + cHeight).toDouble())
                icon.paintIcon(c, g2, x, y)
            }
        }
        g2.dispose()
    }

    /**
     * Gets the width of this icon.
     *
     * @return The width of the icon in pixels.
     */
    override fun getIconWidth(): Int {
        return if (rotate == Rotate.ABOUT_CENTER) {
            if (isCircularIcon) {
                icon.iconWidth
            } else {
                val radians = Math.toRadians(degrees)
                val sin = abs(sin(radians))
                val cos = abs(cos(radians))
                floor(icon.iconWidth * cos + icon.iconHeight * sin).toInt()
            }
        } else if (rotate == Rotate.UPSIDE_DOWN) {
            icon.iconWidth
        } else {
            icon.iconHeight
        }
    }

    /**
     * Gets the height of this icon.
     *
     * @return The height of the icon in pixels.
     */
    override fun getIconHeight(): Int {
        return if (rotate == Rotate.ABOUT_CENTER) {
            if (isCircularIcon) {
                icon.iconHeight
            } else {
                val radians = Math.toRadians(degrees)
                val sin = abs(sin(radians))
                val cos = abs(cos(radians))
                floor(icon.iconHeight * cos + icon.iconWidth * sin).toInt()
            }
        } else if (rotate == Rotate.UPSIDE_DOWN) {
            icon.iconHeight
        } else {
            icon.iconWidth
        }
    }

    enum class Rotate {
        DOWN, UP, UPSIDE_DOWN, ABOUT_CENTER
    }
}