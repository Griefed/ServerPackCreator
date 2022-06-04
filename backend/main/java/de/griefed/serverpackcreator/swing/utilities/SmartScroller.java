/* Copyright (C) 2022  Griefed
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
 *
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
 * The Java API is huge, and we don’t have time to read it from start to finish. If the examples don’t solve your problem,
 * hopefully they can at least give you some ideas and introduce you to new API’s and concepts.
 *
 * Rob
 *
 * #####################################################################################################################
 *
 */
package de.griefed.serverpackcreator.swing.utilities;

import de.griefed.serverpackcreator.utilities.misc.Generated;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * Hey, Griefed here. I was playing around with scrolling in Swing and was getting fed up with the ways I came up with
 * which would allow me to set the scrollbar to the bottom at all times....I mean, it worked, sorta? The problem was
 * that you couldn't scroll up...making any previous log messages unreadable...I couldn't figure out how to make it, so
 * it would stop scrolling to the end if the user scrolled up, and when I was about to give up and just not deal with any
 * of that scrolling business, I came across this beauty here. Add your scrollpane to this SmartScroller and BAM!
 * You've got yourself a smartscrolling scroll pane which stops going to the end of the pane after you've scrolled up,
 * and resumes autoscrolling when you've scrolled to the bottom again. It's beautiful!<br>
 * Rob, you absolute madlad. You did it again.<br>
 * Links:<br>
 * See <a href="https://tips4java.wordpress.com/2013/03/03/smart-scrolling/">Smart Scrolling</a><br>
 * And <a href="https://www.camick.com/java/source/SmartScroller.java">SmartScroller.java</a>
 * Seriously, give this man an award, because this class is a <strong>BEAST</strong>.<p>
 * Rob, if you somehow ever get wind of your class being used here: Thank you, thank you, thank you, thank you, thank you
 * so very much! You seriously made my day here.<br>
 * Rob, you rule.<br>
 * <br>
 * The SmartScroller will attempt to keep the viewport positioned based on<br>
 * the users interaction with the scrollbar. The normal behaviour is to keep<br>
 * the viewport positioned to see new data as it is dynamically added.<br>
 * <br>
 * Assuming vertical scrolling and data is added to the bottom:<br>
 * <br>
 * - when the viewport is at the bottom and new data is added,<br>
 * then automatically scroll the viewport to the bottom<br>
 * - when the viewport is not at the bottom and new data is added,<br>
 * then do nothing with the viewport<br>
 * <br>
 * Assuming vertical scrolling and data is added to the top:<br>
 * <br>
 * - when the viewport is at the top and new data is added,<br>
 * then do nothing with the viewport<br>
 * - when the viewport is not at the top and new data is added, then adjust<br>
 * the viewport to the relative position it was at before the data was added<br>
 * <br>
 * Similar logic would apply for horizontal scrolling.
 *
 * @author Rob Camick
 */
@Generated
public class SmartScroller implements AdjustmentListener {
    public final static int HORIZONTAL = 0;
    public final static int VERTICAL = 1;

    public final static int START = 0;
    public final static int END = 1;

    private final int viewportPosition;

    private boolean adjustScrollBar = true;

    private int previousValue = -1;
    private int previousMaximum = -1;

    /**
     * Convenience constructor.<br>
     * Scroll direction is VERTICAL and viewport position is at the END.
     *
     * @param scrollPane the scroll pane to monitor
     * @author Rob Camick
     */
    public SmartScroller(JScrollPane scrollPane) {
        this(scrollPane, VERTICAL, END);
    }

    /**
     * Convenience constructor.<br>
     * Scroll direction is VERTICAL.
     *
     * @param scrollPane       the scroll pane to monitor
     * @param viewportPosition valid values are START and END
     * @author Rob Camick
     */
    public SmartScroller(JScrollPane scrollPane, int viewportPosition) {
        this(scrollPane, VERTICAL, viewportPosition);
    }

    /**
     * Specify how the SmartScroller will function.
     *
     * @param scrollPane       the scroll pane to monitor
     * @param scrollDirection  indicates which JScrollBar to monitor.
     *                         Valid values are HORIZONTAL and VERTICAL.
     * @param viewportPosition indicates where the viewport will normally be
     *                         positioned as data is added.
     *                         Valid values are START and END
     * @author Rob Camick
     */
    public SmartScroller(JScrollPane scrollPane, int scrollDirection, int viewportPosition) {
        if (scrollDirection != HORIZONTAL
                && scrollDirection != VERTICAL) {
            throw new IllegalArgumentException("invalid scroll direction specified");
        }
        if (viewportPosition != START
                && viewportPosition != END) {
            throw new IllegalArgumentException("invalid viewport position specified");
        }

        this.viewportPosition = viewportPosition;

        JScrollBar scrollBar;
        if (scrollDirection == HORIZONTAL) {
            scrollBar = scrollPane.getHorizontalScrollBar();
        } else {
            scrollBar = scrollPane.getVerticalScrollBar();
        }

        scrollBar.addAdjustmentListener(this);

        //  Turn off automatic scrolling for text components

        Component view = scrollPane.getViewport().getView();

        if (view instanceof JTextComponent) {

            JTextComponent textComponent = (JTextComponent) view;
            DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        }
    }

    @Override
    public void adjustmentValueChanged(final AdjustmentEvent e) {
        SwingUtilities.invokeLater(() -> checkScrollBar(e));
    }

    /**
     * Analyze every adjustment event to determine when the viewport needs to be repositioned.
     *
     * @param e Adjustment event to analyse
     * @author Rob Camick
     */
    private void checkScrollBar(AdjustmentEvent e) {
        /*
         * The scroll bar listModel contains information needed to determine
         * whether the viewport should be repositioned or not.
         */

        JScrollBar scrollBar = (JScrollBar) e.getSource();
        BoundedRangeModel listModel = scrollBar.getModel();
        int value = listModel.getValue();
        int extent = listModel.getExtent();
        int maximum = listModel.getMaximum();

        boolean valueChanged = previousValue != value;
        boolean maximumChanged = previousMaximum != maximum;

        //  Check if the user has manually repositioned the scrollbar

        if (valueChanged && !maximumChanged) {
            if (viewportPosition == START)
                adjustScrollBar = value != 0;
            else
                adjustScrollBar = value + extent >= maximum;
        }

        /*
         * Reset the "value" so we can reposition the viewport and
         * distinguish between a user scroll and a program scroll.
         * (i.e. valueChanged will be false on a program scroll)
         */

        if (adjustScrollBar && viewportPosition == END) {
            //  Scroll the viewport to the end.
            scrollBar.removeAdjustmentListener(this);
            value = maximum - extent;
            scrollBar.setValue(value);
            scrollBar.addAdjustmentListener(this);
        }

        if (adjustScrollBar && viewportPosition == START) {
            //  Keep the viewport at the same relative viewportPosition
            scrollBar.removeAdjustmentListener(this);
            value = value + maximum - previousMaximum;
            scrollBar.setValue(value);
            scrollBar.addAdjustmentListener(this);
        }

        previousValue = value;
        previousMaximum = maximum;
    }
}