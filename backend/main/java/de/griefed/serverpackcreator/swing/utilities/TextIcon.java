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
 * The Java API is huge, and we do not have time to read it from start to finish. If the examples do not solve your problem,
 * hopefully they can at least give you some ideas and introduce you to new APIs and concepts.
 *
 * Rob
 *
 * #####################################################################################################################
 *
 */
package de.griefed.serverpackcreator.swing.utilities;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * Heya, Griefed here. This is yet another miracle by Rob Camick. This man is a godsend.<br>
 * See <a href="https://tips4java.wordpress.com/2009/04/02/text-icon/">Text Icon</a><br>
 * <p>
 * The TextIcon will paint a String of text as an Icon. The Icon
 * can be used by any Swing component that supports icons.
 * <p>
 * TextIcon supports two different layout styles:
 * <ul>
 * <li>Horizontally - does normal rendering of the text by using the
 *   Graphics.drawString(...) method
 * <li>Vertically - Each character is displayed on a separate line
 * </ul>
 * <p>
 * TextIcon was designed to be rendered on a specific JComponent as it
 * requires FontMetrics information in order to calculate its size and to do
 * the rendering. Therefore, it should only be added to component it was
 * created for.
 * <p>
 * By default the text will be rendered using the Font and foreground color
 * of its associated component. However, this class does allow you to override
 * these properties. Also starting in JDK6 the desktop renderering hints will
 * be used to renderer the text. For versions not supporting the rendering
 * hints antialiasing will be turned on.
 * <p>
 * @author Rob Camick
 */
public class TextIcon implements Icon, PropertyChangeListener {
    private final JComponent component;
    private final Layout layout;
    private String text;
    private Font font;
    private Color foreground;
    private int padding;
    private int iconWidth;

    //  Used for the implementation of Icon interface
    private int iconHeight;
    private String[] strings;

    //  Used for Layout.VERTICAL to save reparsing the text every time the
    //  icon is repainted
    private int[] stringWidths;
    /**
     * Convenience constructor to create a TextIcon with a HORIZONTAL layout.
     *
     * @param component the component to which the icon will be added
     * @param text      the text to be rendered on the Icon
     */
    public TextIcon(JComponent component, String text) {
        this(component, text, Layout.HORIZONTAL);
    }

    /**
     * Create a TextIcon specifying all the properties.
     *
     * @param component the component to which the icon will be added
     * @param text      the text to be rendered on the Icon
     * @param layout    specify the layout of the text. Must be one of
     *                  the Layout enums: HORIZONTAL or VERTICAL
     */
    public TextIcon(JComponent component, String text, Layout layout) {
        this.component = component;
        this.layout = layout;
        setText(text);

        component.addPropertyChangeListener("font", this);
    }

    /**
     * Get the Layout enum
     *
     * @return the Layout enum
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Get the text String that will be rendered on the Icon
     *
     * @return the text of the Icon
     */
    public String getText() {
        return text;
    }

    /**
     * Set the text to be rendered on the Icon
     *
     * @param text the text to be rendered on the Icon
     */
    public void setText(String text) {
        this.text = text;

        calculateIconDimensions();
    }

    /**
     * Get the Font used to render the text. This will default to the Font
     * of the component unless the Font has been overridden by using the
     * setFont() method.
     *
     * @return the Font used to render the text
     */
    public Font getFont() {
        if (font == null)
            return component.getFont();
        else
            return font;
    }

    /**
     * Set the Font to be used for rendering the text
     *
     * @param font the Font to be used for rendering the text
     */
    public void setFont(Font font) {
        this.font = font;

        calculateIconDimensions();
    }

    /**
     * Get the foreground Color used to render the text. This will default to
     * the foreground Color of the component unless the foreground Color has
     * been overridden by using the setForeground() method.
     *
     * @return the Color used to render the text
     */
    public Color getForeground() {
        if (foreground == null)
            return component.getForeground();
        else
            return foreground;
    }

    /**
     * Set the foreground Color to be used for rendering the text
     *
     * @param foreground the foreground Color to be used for rendering the text
     */
    public void setForeground(Color foreground) {
        this.foreground = foreground;
        component.repaint();
    }

    /**
     * Get the padding used when rendering the text
     *
     * @return the padding specified in pixels
     */
    public int getPadding() {
        return padding;
    }

    /**
     * By default, the size of the Icon is based on the size of the rendered
     * text. You can specify some padding to be added to the start and end
     * of the text when it is rendered.
     *
     * @param padding the padding amount in pixels
     */
    public void setPadding(int padding) {
        this.padding = padding;

        calculateIconDimensions();
    }

    /**
     * Calculate the size of the Icon using the FontMetrics of the Font.
     */
    private void calculateIconDimensions() {
        Font font = getFont();
        FontMetrics fm = component.getFontMetrics(font);

        if (layout == Layout.HORIZONTAL) {
            iconWidth = fm.stringWidth(text) + (padding * 2);
            iconHeight = fm.getHeight();
        } else if (layout == Layout.VERTICAL) {

            int maxWidth = 0;
            strings = new String[text.length()];
            stringWidths = new int[text.length()];

            //  Find the widest character in the text string

            for (int i = 0; i < text.length(); i++) {

                strings[i] = text.substring(i, i + 1);
                stringWidths[i] = fm.stringWidth(strings[i]);
                maxWidth = Math.max(maxWidth, stringWidths[i]);

            }

            //  Add a minimum of 2 extra pixels, plus the leading value,
            //  on each side of the character.

            iconWidth = maxWidth + ((fm.getLeading() + 2) * 2);

            //  Decrease then normal gap betweens lines of text by taking into
            //  account the descent.

            iconHeight = (fm.getHeight() - fm.getDescent()) * text.length();
            iconHeight += padding * 2;
        }

        component.revalidate();
    }

    /**
     * Gets the width of this icon.
     *
     * @return the width of the icon in pixels.
     */
    @Override
    public int getIconWidth() {
        return iconWidth;
    }
//
//  Implement the Icon Interface
//

    /**
     * Gets the height of this icon.
     *
     * @return the height of the icon in pixels.
     */
    @Override
    public int getIconHeight() {
        return iconHeight;
    }

    /**
     * Paint the icons of this compound icon at the specified location
     *
     * @param c The component to which the icon is added
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        //  The "desktophints" is supported in JDK6

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Map map = (Map) (toolkit.getDesktopProperty("awt.font.desktophints"));

        if (map != null) {

            g2.addRenderingHints(map);

        } else {

            g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
        }

        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();

        if (layout == Layout.HORIZONTAL) {

            g2.translate(x, y + fm.getAscent());
            g2.drawString(text, padding, 0);

        } else if (layout == Layout.VERTICAL) {

            int offsetY = fm.getAscent() - fm.getDescent() + padding;
            int incrementY = fm.getHeight() - fm.getDescent();

            for (int i = 0; i < text.length(); i++) {

                int offsetX = Math.round((getIconWidth() - stringWidths[i]) / 2.0f);
                g2.drawString(strings[i], x + offsetX, y + offsetY);
                offsetY += incrementY;

            }
        }

        g2.dispose();
    }

    //
//  Implement the PropertyChangeListener interface
//
    public void propertyChange(PropertyChangeEvent e) {
        //  Handle font change when using the default font

        if (font == null) {
            calculateIconDimensions();
        }
    }

    public enum Layout {
        HORIZONTAL,
        VERTICAL
    }
}