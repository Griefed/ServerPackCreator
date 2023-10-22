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
package de.griefed.serverpackcreator.gui.window.configs.components

import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * The ComponentResizer allows you to resize a component by dragging a border of the component.
 *
 * @param dragInsets The insets specify an area where mouseDragged events are recognized from the edge of the border
 * inwards. A value of 0 for any size will imply that the border is not resizable. Otherwise, the appropriate drag cursor
 * will appear when the mouse is inside the resizable border area.
 * @param snapSize Control how many pixels a border must be dragged before the size of the component is changed. The
 * border will snap to the size once dragging has passed the halfway mark. Dimension object allows you to separately
 * specify a horizontal and vertical snap size.
 *
 * @author Griefed
 * @author Rob Camick
 */
class ComponentResizer(
    dragInsets: Insets = Insets(5, 5, 5, 5),
    private var snapSize: Dimension = Dimension(1, 1),
) : MouseAdapter() {

    private val south = 4
    private val cursors: MutableMap<Int, Int> = HashMap()
    private var dragInsets: Insets = Insets(5, 5, 5, 5)
        set(value) {
            validateMinimumAndInsets(minimumSize, value)
            field = value
        }
    private var direction = 0
    private var sourceCursor: Cursor? = null
    private var resizing = false
    private var bounds: Rectangle? = null
    private var pressed: Point? = null
    private var autoscroll = false
    private var minimumSize = Dimension(100, 100)
        set(value) {
            validateMinimumAndInsets(value, dragInsets)
            field = value
        }
    private val components = hashMapOf<Component, String>()
    private var maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

    init {
        cursors[4] = Cursor.S_RESIZE_CURSOR
        cursors[6] = Cursor.SW_RESIZE_CURSOR
        cursors[12] = Cursor.SE_RESIZE_CURSOR
        this.dragInsets = dragInsets
    }

    /**
     * Add the required listeners to the specified component
     *
     * @param component The component the listeners are added to.
     * @param constraint The miglayout constraints to update. Use {0} and {1} for width and height respectively, i.e.
     * "cell 2 3 1 3,grow,w 10:{0}:,h {1}!"
     *
     * @author Rob Camick
     * @author Griefed
     */
    fun registerComponent(component: Component, constraint: String) {
        component.addMouseListener(this)
        component.addMouseMotionListener(this)
        if (component is ResizeIndicatorScrollPane) {
            component.viewport.view.addMouseListener(this)
            component.viewport.view.addMouseMotionListener(this)
        }
        components[component] = constraint
    }

    /**
     * When the components minimum size is less than the drag insets then
     * we can't determine which border should be resized, so we need to
     * prevent this from happening.
     *
     * @author Rob Camick
     * @author Griefed
     */
    private fun validateMinimumAndInsets(minimum: Dimension, drag: Insets?) {
        val minimumWidth = drag!!.left + drag.right
        val minimumHeight = drag.top + drag.bottom
        if (minimum.width < minimumWidth
            || minimum.height < minimumHeight
        ) {
            val message = "Minimum size cannot be less than drag insets"
            throw IllegalArgumentException(message)
        }
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    override fun mouseMoved(e: MouseEvent) {
        val source = getSource(e)
        val location = e.point
        direction = 0

        val handleBarPosition = source.handleBarPosition!!
        val x = handleBarPosition.x - source.insets.left
        val maxX = handleBarPosition.width + handleBarPosition.x + source.insets.right
        val y = handleBarPosition.y - source.insets.top
        val maxY = handleBarPosition.height + handleBarPosition.y + source.insets.bottom
        if (location.x in x..maxX && location.y in y..maxY) {
            direction += south
        }

        //  Mouse is no longer over a resizable border
        when (direction) {
            0 -> {
                updateCursor(e, sourceCursor!!)
            }

            4 -> {
                // use the appropriate resizable cursor
                val cursorType = cursors[direction]!!
                val cursor = Cursor.getPredefinedCursor(cursorType)
                updateCursor(e, cursor)
            }
        }
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    private fun changeBounds(
        e: MouseEvent,
        source: Component,
        direction: Int,
        bounds: Rectangle?,
        pressed: Point?,
        current: Point
    ) {
        if (direction != 4 && direction != 6 && direction != 12) {
            return
        }
        //  Start with original location and size
        val x = bounds!!.x
        val y = bounds.y
        var height = bounds.height

        //  Resizing the West or North border affects the size and location
        var drag = getDragDistance(current.y, pressed!!.y, snapSize.height)
        val maximum = (maximumSize.height - y).coerceAtMost(maximumSize.height)
        drag = getDragBounded(drag, snapSize.height, height, minimumSize.height, maximum)
        height += drag

        val layout: MigLayout
        if (source is ResizeIndicatorScrollPane) {
            layout = source.parent.layout as MigLayout
            layout.setComponentConstraints(source, components[source]?.format(height))
        }

        source.setBounds(x, y, source.width, height)
        if (source is JComponent) {
            updateAutoscrolls(e, autoscroll)
            source.rootPane.grabFocus()
            source.grabFocus()
        }
        source.revalidate()
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    private fun getSource(e: MouseEvent): ResizeIndicatorScrollPane {
        return if (e.component is ResizeIndicatorScrollPane) {
            e.component as ResizeIndicatorScrollPane
        } else {
            e.component.parent.parent as ResizeIndicatorScrollPane
        }
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    override fun mouseEntered(e: MouseEvent) {
        if (!resizing) {
            val source = getSource(e)
            sourceCursor = source.cursor
        }
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    override fun mouseExited(e: MouseEvent) {
        if (!resizing) {
            updateCursor(e, sourceCursor!!)
        }
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    private fun updateCursor(e: MouseEvent, cursor: Cursor) {
        val source = getSource(e)
        source.cursor = cursor
        e.component.cursor = cursor
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    override fun mousePressed(e: MouseEvent) {
        //	The mouseMoved event continually updates this variable
        if (direction == 0) {
            return
        }

        //  Setup for resizing. All future dragging calculations are done based
        //  on the original bounds of the component and mouse pressed location.
        resizing = true
        val source = getSource(e)
        pressed = e.point
        SwingUtilities.convertPointToScreen(pressed, source)
        bounds = source.bounds

        //  Making sure autoscrolls is false will allow for smoother resizing
        //  of components
        autoscroll = source.autoscrolls
        updateAutoscrolls(e, false)
    }

    /**
     * @author Rob Camick
     * @author Griefed
     */
    private fun updateAutoscrolls(e: MouseEvent, setting: Boolean) {
        val source = getSource(e)
        source.autoscrolls = setting
        (e.component as JComponent).autoscrolls = setting
    }

    /**
     * Restore the original state of the Component
     *
     * @author Rob Camick
     * @author Griefed
     */
    override fun mouseReleased(e: MouseEvent) {
        resizing = false
        updateCursor(e, sourceCursor!!)
        updateAutoscrolls(e, autoscroll)
    }

    /**
     * Resize the component ensuring location and size is within the bounds
     * of the parent container and that the size is within the minimum and
     * maximum constraints.
     *
     * All calculations are done using the bounds of the component when the
     * resizing started.
     *
     * @author Rob Camick
     * @author Griefed
     */
    override fun mouseDragged(e: MouseEvent) {
        if (!resizing) {
            return
        }
        val source = getSource(e)
        val dragged = e.point
        SwingUtilities.convertPointToScreen(dragged, source)
        changeBounds(e, source, direction, bounds, pressed, dragged)
    }

    /**
     *  Determine how far the mouse has moved from where dragging started
     *
     * @author Rob Camick
     * @author Griefed
     */
    private fun getDragDistance(larger: Int, smaller: Int, snapSize: Int): Int {
        val halfway = snapSize / 2
        var drag = larger - smaller
        drag += if (drag < 0) {
            -halfway
        } else {
            halfway
        }
        drag = drag / snapSize * snapSize
        return drag
    }

    /**
     *  Adjust the drag value to be within the minimum and maximum range.
     *
     * @author Rob Camick
     * @author Griefed
     */
    private fun getDragBounded(drag: Int, snapSize: Int, dimension: Int, minimum: Int, maximum: Int): Int {
        var newDrag = drag
        while (dimension + newDrag < minimum) {
            newDrag += snapSize
        }
        while (dimension + newDrag > maximum) {
            newDrag -= snapSize
        }
        return newDrag
    }
}