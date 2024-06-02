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
package de.griefed.serverpackcreator.app.gui.components

import java.awt.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.Icon
import javax.swing.JComponent
import kotlin.math.roundToInt

/**
 * Heya, Griefed here. This is yet another miracle by Rob Camick. This man is a godsend.
 *
 * See [Text Icon](https://tips4java.wordpress.com/2009/04/02/text-icon/)
 *
 * The TextIcon will paint a String of text as an Icon. The Icon can be used by any Swing
 * component that supports icons.
 *
 * TextIcon supports two different layout styles:
 *
 *  * Horizontally - does normal rendering of the text by using the Graphics.drawString(...) method
 *  * Vertically - Each character is displayed on a separate line
 *
 * TextIcon was designed to be rendered on a specific JComponent as it requires FontMetrics
 * information in order to calculate its size and to do the rendering. Therefore, it should only be
 * added to component it was created for.
 *
 * By default, the text will be rendered using the Font and foreground color of its associated
 * component. However, this class does allow you to override these properties. Also starting in JDK6
 * the desktop rendering hints will be used to renderer the text. For versions not supporting the
 * rendering hints antialiasing will be turned on.
 *
 * @author Rob Camick
 */
class TextIcon(
    private val component: JComponent,
    private var text: String,
    private val layout: Layout = Layout.HORIZONTAL
) : Icon, PropertyChangeListener {
    private var font: Font? = null
        set(value) {
            field = value
            calculateIconDimensions()
        }
        get() {
            return if (field == null) {
                component.font
            } else {
                field
            }
        }
    private var foreground: Color? = null
        get() = if (field == null) {
            component.foreground
        } else {
            field
        }
        set(foreground) {
            field = foreground
            component.repaint()
        }
    private var padding = 0
        set(value) {
            field = value
            calculateIconDimensions()
        }

    private var iconWidth = 0
    private var iconHeight = 0
    private lateinit var strings: Array<String?>
    private lateinit var stringWidths: IntArray

    init {
        setText(text)
        component.addPropertyChangeListener("font", this)
    }

    /**
     * Calculate the size of the Icon using the FontMetrics of the Font.
     *
     * @author Rob Camick
     */
    private fun calculateIconDimensions() {
        val fm = component.getFontMetrics(font)
        if (layout == Layout.HORIZONTAL) {
            iconWidth = text.let { fm.stringWidth(it) } + padding * 2
            iconHeight = fm.height
        } else if (layout == Layout.VERTICAL) {
            var maxWidth = 0
            strings = arrayOfNulls(text.length)
            stringWidths = IntArray(text.length)
            //  Find the widest character in the text string
            for (i in text.indices) {
                strings[i] = text.substring(i, i + 1)
                stringWidths[i] = strings[i]?.let { fm.stringWidth(it) }!!
                maxWidth = maxWidth.coerceAtLeast(stringWidths[i])
            }
            //  Add a minimum of 2 extra pixels, plus the leading value,
            //  on each side of the character.
            iconWidth = maxWidth + (fm.leading + 2) * 2
            //  Decrease then normal gap between lines of text by taking into
            //  account the descent.
            iconHeight = (fm.height - fm.descent) * text.length
            iconHeight += padding * 2
        }
        component.revalidate()
    }

    /**
     * Set the text to be rendered on the Icon
     *
     * @param text The text to be rendered on the Icon
     *
     * @author Rob Camick
     */
    private fun setText(text: String) {
        this.text = text
        calculateIconDimensions()
    }

    /**
     * Paint the icons of this compound icon at the specified location
     *
     * @param c The component to which the icon is added
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     *
     * @author Rob Camick
     */
    override fun paintIcon(
        c: Component,
        g: Graphics,
        x: Int,
        y: Int
    ) {
        val g2 = g.create() as Graphics2D
        val toolkit = Toolkit.getDefaultToolkit()
        if (toolkit.getDesktopProperty("awt.font.desktophints") != null) {
            val map = toolkit.getDesktopProperty("awt.font.desktophints") as Map<*, *>
            g2.addRenderingHints(map)
        }
        g2.font = font!!
        g2.color = foreground
        val fm = g2.fontMetrics
        if (layout == Layout.HORIZONTAL) {
            g2.translate(x, y + fm.ascent)
            text.let { g2.drawString(it, padding, 0) }
        } else if (layout == Layout.VERTICAL) {
            var offsetY = fm.ascent - fm.descent + padding
            val incrementY = fm.height - fm.descent
            for (i in text.indices) {
                val offsetX = ((iconWidth - stringWidths[i]) / 2.0f).roundToInt()
                strings[i]?.let { g2.drawString(it, x + offsetX, y + offsetY) }
                offsetY += incrementY
            }
        }
        g2.dispose()
    }

    /**
     * Gets the width of this icon.
     *
     * @return the width of the icon in pixels.
     *
     * @author Rob Camick
     */
    override fun getIconWidth(): Int {
        return iconWidth
    }

    /**
     * Gets the height of this icon.
     *
     * @return the height of the icon in pixels.
     *
     * @author Rob Camick
     */
    override fun getIconHeight(): Int {
        return iconHeight
    }

    /**
     * @author Rob Camick
     */
    override fun propertyChange(e: PropertyChangeEvent) {
        //  Handle font change when using the default font
        if (font == null) {
            calculateIconDimensions()
        }
    }

    /**
     * @author Rob Camick
     */
    enum class Layout {
        HORIZONTAL, VERTICAL
    }
}