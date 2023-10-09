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
 */
package de.griefed.serverpackcreator.gui.splash

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * Hey, Griefed here. I tried to add a tiled background image to the frame which holds the
 * JTabbedPane, but after several failed attempts, I gave up and almost threw the idea out of the
 * window. I wanted to set the background to a tiled image, because simply setting a colour seemed
 * too boring, and I needed *something* in the background so the banner icon would be clearer to the eye.
 * So, I activated my Google-Fu and encountered this holy grail of tiling images for Swing.
 *
 * Links:
 * * [Background Panel by Rob Camick from October 12, 2008](https://tips4java.wordpress.com/2008/10/12/background-panel/)
 * * [BackgroundPanel.java](https://www.camick.com/java/source/BackgroundPanel.java)
 *
 * Seriously, give this man an award, because this class is a **BEAST**.
 *
 * Rob, if you somehow ever get wind of your class being used here: Thank you, thank you, thank
 * you, thank you, thank you so very much! You seriously made my day here.
 *
 * Rob, you rule.
 *
 */
@Suppress("unused")
class BackgroundPanel(
    private var image: Image,
    private var style: Int = SCALED,
    private var alignX: Float = 0.5f,
    private var alignY: Float = 0.5f,
    private var isTransparentAdd: Boolean = true
) : JPanel() {
    init {
        setImage(image)
        setStyle(style)
        setImageAlignmentX(alignmentX)
        setImageAlignmentY(alignmentY)
        layout = BorderLayout()
    }

    /**
     * Setter for the image used as the background.
     *
     * @param image Image to be set as the background.
     * @author Rob Camick
     */
    private fun setImage(image: Image) {
        this.image = image
        repaint()
    }

    /**
     * Setter the style used to paint the background image.
     *
     * @param style Sets the style with which the image should be painted.
     * @author Rob Camick
     */
    private fun setStyle(style: Int) {
        this.style = style
        repaint()
    }

    /**
     * Setter for the horizontal alignment of the image when using ACTUAL style.
     *
     * @param alignmentX Sets the alignment along the x-axis.
     * @author Rob Camick
     */
    private fun setImageAlignmentX(alignmentX: Float) {
        this.alignX = if (alignmentX > 1.0f) 1.0f else alignmentX.coerceAtLeast(0.0f)
        repaint()
    }

    /**
     * Setter for the horizontal alignment of the image when using ACTUAL style.
     *
     * @param alignmentY Sets the alignment along the y-axis.
     * @author Rob Camick
     */
    private fun setImageAlignmentY(alignmentY: Float) {
        this.alignY = if (alignmentY > 1.0f) 1.0f else alignmentY.coerceAtLeast(0.0f)
        repaint()
    }

    /**
     * Override method so we can make the component transparent.
     *
     * @param component   JComponent to add to the panel.
     * @param constraints Constraints with which the panel should be added.
     * @author Rob Camick
     */
    fun addComponent(
        component: JComponent,
        constraints: Any? = null
    ) {
        if (isTransparentAdd) {
            makeComponentTransparent(component)
        }
        super.add(component, constraints)
    }

    /**
     * Try to make the component transparent. For components that use renderers, like JTable, you will
     * also need to change the renderer to be transparent. An easy way to do this it to set the
     * background of the table to a Color using an alpha value of 0.
     *
     * @param component The component to make transparent.
     * @author Rob Camick
     */
    private fun makeComponentTransparent(component: JComponent) {
        component.isOpaque = false
        if (component is JScrollPane) {
            val viewport = component.viewport
            viewport.isOpaque = false
            val c = viewport.view
            if (c is JComponent) {
                c.isOpaque = false
            }
        }
    }

    /**
     * Controls whether components added to this panel should automatically be made transparent. That
     * is, setOpaque(false) will be invoked. The default is set to true.
     *
     * @param isTransparentAdd Whether to automatically make components transparent.
     * @author Rob Camick
     */
    fun setTransparentAdd(isTransparentAdd: Boolean) {
        this.isTransparentAdd = isTransparentAdd
    }

    /**
     * Add custom painting.
     *
     * @param g Received from parent.
     * @author Rob Camick
     */
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        when (style) {
            TILED -> drawTiled(g)
            ACTUAL -> drawActual(g)
            else -> drawScaled(g)
        }
    }

    /**
     * Override to provide a preferred size equal to the image size.
     *
     * @return Returns the dimension of the passed image.
     * @author Rob Camick
     */
    override fun getPreferredSize(): Dimension {
        return Dimension(image.getWidth(null), image.getHeight(null))
    }

    /**
     * Custom painting code for drawing TILED images as the background.
     *
     * @param g Received from parent.
     * @author Rob Camick
     */
    private fun drawTiled(g: Graphics) {
        val d = size
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        run {
            var x = 0
            while (x < d.width) {
                run {
                    var y = 0
                    while (y < d.height) {
                        g.drawImage(image, x, y, null, null)
                        y += height
                    }
                }
                x += width
            }
        }
    }

    /**
     * Custom painting code for drawing the ACTUAL image as the background. The image is positioned in
     * the panel based on the horizontal and vertical alignments specified.
     *
     * @param g Received from parent.
     * @author Rob Camick
     */
    private fun drawActual(g: Graphics) {
        val d = size
        val insets = insets
        val width = d.width - insets.left - insets.right
        val height = d.height - insets.top - insets.left
        val x = (width - image.getWidth(null)) * alignX
        val y = (height - image.getHeight(null)) * alignY
        g.drawImage(image, x.toInt() + insets.left, y.toInt() + insets.top, this)
    }

    /**
     * Custom painting code for drawing a SCALED image as the background.
     *
     * @param g Received from parent.
     * @author Rob Camick
     */
    private fun drawScaled(g: Graphics) {
        val d = size
        g.drawImage(image, 0, 0, d.width, d.height, null)
    }

    companion object {
        const val SCALED = 0
        const val TILED = 1
        const val ACTUAL = 2
    }
}