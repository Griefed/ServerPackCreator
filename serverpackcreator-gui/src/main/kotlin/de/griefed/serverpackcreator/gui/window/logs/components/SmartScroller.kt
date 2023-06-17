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
package de.griefed.serverpackcreator.gui.window.logs.components

import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.text.DefaultCaret
import javax.swing.text.JTextComponent

/**
 * Hey, Griefed here. I was playing around with scrolling in Swing and was getting fed up with the ways I came up with
 * which would allow me to set the scrollbar to the bottom at all times....I mean, it worked, sorta? The problem was
 * that you couldn't scroll up...making any previous log messages unreadable...I couldn't figure out how to make it,
 * so it would stop scrolling to the end if the user scrolled up, and when I was about to give up and just not deal
 * with any of that scrolling business, I came across this beauty here. Add your scrollpane to this SmartScroller and
 * BAM! You've got yourself a smartscrolling scroll pane which stops going to the end of the pane after you've scrolled
 * up, and resumes autoscrolling when you've scrolled to the bottom again. It's beautiful!
 *
 * Rob, you absolute madlad. You did it again.
 *
 * Links:
 *  * See [Smart Scrolling](https://tips4java.wordpress.com/2013/03/03/smart-scrolling/)
 *  * And [SmartScroller.java](https://www.camick.com/java/source/SmartScroller.java)
 *
 *  Seriously, give this man an award, because this class is a **BEAST**.
 *
 *
 * Rob, if you somehow ever get wind of your class being used here: Thank you, thank you, thank you, thank you,
 * thank you so very much! You seriously made my day here.
 *
 * Rob, you rule.
 *
 * The SmartScroller will attempt to keep the viewport positioned based on the users interaction with the scrollbar.
 * The normal behaviour is to keep the viewport positioned to see new data as it is dynamically added.
 *
 * Assuming vertical scrolling and data is added to the bottom:
 *
 * - when the viewport is at the bottom and new data is added, then automatically scroll the viewport to the bottom
 * - when the viewport is not at the bottom and new data is added, then do nothing with the viewport
 *
 * Assuming vertical scrolling and data is added to the top:
 *
 * - when the viewport is at the top and new data is added, then do nothing with the viewport
 * - when the viewport is not at the top and new data is added, then adjust the viewport to the relative position it
 * was at before the data was added
 *
 * Similar logic would apply for horizontal scrolling.
 *
 * @author Rob Camick
 */
class SmartScroller @JvmOverloads constructor(
    scrollPane: JScrollPane,
    scrollDirection: ScrollDirection = ScrollDirection.VERTICAL,
    private val viewPortPosition: ViewPortPosition = ViewPortPosition.END
) : AdjustmentListener {
    private var adjustScrollBar = true
    private var previousValue = -1
    private var previousMaximum = -1

    init {
        val scrollBar: JScrollBar = if (scrollDirection == ScrollDirection.HORIZONTAL) {
            scrollPane.horizontalScrollBar
        } else {
            scrollPane.verticalScrollBar
        }
        scrollBar.addAdjustmentListener(this)

        //  Turn off automatic scrolling for text components
        val view = scrollPane.viewport.view
        if (view is JTextComponent) {
            val caret = view.caret as DefaultCaret
            caret.updatePolicy = DefaultCaret.NEVER_UPDATE
        }
    }

    override fun adjustmentValueChanged(e: AdjustmentEvent) {
        SwingUtilities.invokeLater { checkScrollBar(e) }
    }

    /**
     * Analyze every adjustment event to determine when the viewport needs to be repositioned.
     *
     * @param e Adjustment event to analyse
     *
     * @author Rob Camick
     */
    private fun checkScrollBar(e: AdjustmentEvent) {
        /*
         * The scroll bar listModel contains information needed to determine
         * whether the viewport should be repositioned or not.
         */
        val scrollBar = e.source as JScrollBar
        val listModel = scrollBar.model
        var value = listModel.value
        val extent = listModel.extent
        val maximum = listModel.maximum
        val valueChanged = previousValue != value
        val maximumChanged = previousMaximum != maximum

        //  Check if the user has manually repositioned the scrollbar
        if (valueChanged && !maximumChanged) {
            adjustScrollBar =
                if (viewPortPosition == ViewPortPosition.START) {
                    value != 0
                } else {
                    value + extent >= maximum
                }
        }

        /*
        * Reset the "value" so we can reposition the viewport and
        * distinguish between a user scroll and a program scroll.
        * (i.e. valueChanged will be false on a program scroll)
        */
        if (adjustScrollBar && viewPortPosition == ViewPortPosition.END) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(this)
            value = maximum - extent
            scrollBar.value = value
            scrollBar.addAdjustmentListener(this)
        }
        if (adjustScrollBar && viewPortPosition == ViewPortPosition.START) {
            //  Keep the viewport at the same relative viewportPosition
            scrollBar.removeAdjustmentListener(this)
            value = value + maximum - previousMaximum
            scrollBar.value = value
            scrollBar.addAdjustmentListener(this)
        }
        previousValue = value
        previousMaximum = maximum
    }
}

/**
 * @author Rob Camick
 */
enum class ScrollDirection {
    HORIZONTAL,
    VERTICAL
}

/**
 * @author Rob Camick
 */
enum class ViewPortPosition {
    START,
    END,
}