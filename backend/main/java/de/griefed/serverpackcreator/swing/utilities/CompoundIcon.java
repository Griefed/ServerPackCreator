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

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Heya, Griefed here. This is yet another miracle by Rob Camick. This man is a godsend.<br> See <a
 * href="https://tips4java.wordpress.com/2009/03/29/compound-icon/">Compound Icon</a><br>
 *
 * <p>The CompoundIcon will paint two, or more, Icons as a single Icon. The Icons are painted in
 * the order in which they are added.
 *
 * <p>The Icons are layed out on the specified axis:
 *
 * <ul>
 *   <li>X-Axis (horizontally)
 *   <li>Y-Axis (vertically)
 *   <li>Z-Axis (stacked)
 * </ul>
 *
 * @author Rob Camick
 */
public class CompoundIcon implements Icon {

  private final Icon[] icons;
  private final Axis axis;
  private final int gap;
  private final float alignmentX;
  private final float alignmentY;

  /**
   * Convenience contructor for creating a CompoundIcon where the icons are layed out on the X-AXIS,
   * the gap is 0 and the X/Y alignments will default to CENTER.
   *
   * @param icons the Icons to be painted as part of the CompoundIcon
   */
  public CompoundIcon(Icon... icons) {
    this(Axis.X_AXIS, icons);
  }

  /**
   * Convenience constructor for creating a CompoundIcon where the gap is 0 and the X/Y alignments
   * will default to CENTER.
   *
   * @param axis  the axis used to lay out the icons for painting. Must be one of the Axis enums:
   *              X_AXIS, Y_AXIS, Z_Axis.
   * @param icons the Icons to be painted as part of the CompoundIcon
   */
  public CompoundIcon(Axis axis, Icon... icons) {
    this(axis, 0, icons);
  }

  /**
   * Convenience constructor for creating a CompoundIcon where the X/Y alignments will default to
   * CENTER.
   *
   * @param axis  the axis used to lay out the icons for painting Must be one of the Axis enums:
   *              X_AXIS, Y_AXIS, Z_Axis.
   * @param gap   the gap between the icons
   * @param icons the Icons to be painted as part of the CompoundIcon
   */
  public CompoundIcon(Axis axis, int gap, Icon... icons) {
    this(axis, gap, 0.5f, 0.5f, icons);
  }

  /**
   * Create a CompoundIcon specifying all the properties.
   *
   * @param axis       the axis used to lay out the icons for painting Must be one of the Axis
   *                   enums: X_AXIS, Y_AXIS, Z_Axis.
   * @param gap        the gap between the icons
   * @param alignmentX the X alignment of the icons. Common values are LEFT, CENTER, RIGHT. Can be
   *                   any value between 0.0 and 1.0
   * @param alignmentY the Y alignment of the icons. Common values are TOP, CENTER, BOTTOM. Can be
   *                   any value between 0.0 and 1.0
   * @param icons      the Icons to be painted as part of the CompoundIcon
   */
  public CompoundIcon(Axis axis, int gap, float alignmentX, float alignmentY, Icon... icons) {
    /*
     * TOP = 0.0f
     * LEFT = 0.0f
     * CENTER = 0.5f
     * BOTTOM = 1.0f
     * RIGHT = 1.0f
     */
    this.axis = axis;
    this.gap = gap;
    this.alignmentX = alignmentX > 1.0f ? 1.0f : Math.max(alignmentX, 0.0f);
    this.alignmentY = alignmentY > 1.0f ? 1.0f : Math.max(alignmentY, 0.0f);

    for (int i = 0; i < icons.length; i++) {
      if (icons[i] == null) {
        String message = "Icon (" + i + ") cannot be null";
        throw new IllegalArgumentException(message);
      }
    }

    this.icons = icons;
  }

  /**
   * Get the Axis along which each icon is painted.
   *
   * @return the Axis
   */
  public Axis getAxis() {
    return axis;
  }

  /**
   * Get the gap between each icon
   *
   * @return the gap in pixels
   */
  public int getGap() {
    return gap;
  }

  /**
   * Get the alignment of the icon on the x-axis
   *
   * @return the alignment
   */
  public float getAlignmentX() {
    return alignmentX;
  }

  /**
   * Get the alignment of the icon on the y-axis
   *
   * @return the alignment
   */
  public float getAlignmentY() {
    return alignmentY;
  }

  /**
   * Get the number of Icons contained in this CompoundIcon.
   *
   * @return the total number of Icons
   */
  public int getIconCount() {
    return icons.length;
  }

  /**
   * Get the Icon at the specified index.
   *
   * @param index the index of the Icon to be returned
   * @return the Icon at the specifed index
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public Icon getIcon(int index) {
    return icons[index];
  }

  /**
   * Gets the width of this icon.
   *
   * @return the width of the icon in pixels.
   */
  @Override
  public int getIconWidth() {
    int width = 0;

    //  Add the width of all Icons while also including the gap

    if (axis == Axis.X_AXIS) {

      width += (icons.length - 1) * gap;

      for (Icon icon : icons) {
        width += icon.getIconWidth();
      }

    } else {

      //  Just find the maximum width
      for (Icon icon : icons) {
        width = Math.max(width, icon.getIconWidth());
      }
    }

    return width;
  }

  /**
   * Gets the height of this icon.
   *
   * @return the height of the icon in pixels.
   */
  @Override
  public int getIconHeight() {
    int height = 0;

    //  Add the height of all Icons while also including the gap

    if (axis == Axis.Y_AXIS) {
      height += (icons.length - 1) * gap;

      for (Icon icon : icons) {
        height += icon.getIconHeight();
      }

    } else {
      //  Just find the maximum height
      for (Icon icon : icons) {
        height = Math.max(height, icon.getIconHeight());
      }
    }

    return height;
  }

  /**
   * Paint the icons of this compound icon at the specified location
   *
   * @param c The component on which the icon is painted
   * @param g the graphics context
   * @param x the X coordinate of the icon's top-left corner
   * @param y the Y coordinate of the icon's top-left corner
   */
  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (axis == Axis.X_AXIS) {

      int height = getIconHeight();

      for (Icon icon : icons) {

        int iconY = getOffset(height, icon.getIconHeight(), alignmentY);
        icon.paintIcon(c, g, x, y + iconY);
        x += icon.getIconWidth() + gap;
      }

    } else if (axis == Axis.Y_AXIS) {

      int width = getIconWidth();

      for (Icon icon : icons) {

        int iconX = getOffset(width, icon.getIconWidth(), alignmentX);
        icon.paintIcon(c, g, x + iconX, y);
        y += icon.getIconHeight() + gap;
      }

    } else /* must be Z_AXIS */ {

      int width = getIconWidth();
      int height = getIconHeight();

      for (Icon icon : icons) {

        int iconX = getOffset(width, icon.getIconWidth(), alignmentX);
        int iconY = getOffset(height, icon.getIconHeight(), alignmentY);
        icon.paintIcon(c, g, x + iconX, y + iconY);
      }
    }
  }

  /*
   *  When the icon value is smaller than the maximum value of all icons the
   *  icon needs to be aligned appropriately. Calculate the offset to be used
   *  when painting the icon to achieve the proper alignment.
   */
  private int getOffset(int maxValue, int iconValue, float alignment) {
    float offset = (maxValue - iconValue) * alignment;
    return Math.round(offset);
  }

  public enum Axis {
    X_AXIS,
    Y_AXIS,
    Z_AXIS
  }
}
