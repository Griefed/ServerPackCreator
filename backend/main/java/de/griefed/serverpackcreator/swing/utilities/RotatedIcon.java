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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Heya, Griefed here. This is yet another miracle by Rob Camick. This man is a godsend.<br> See <a
 * href="https://tips4java.wordpress.com/2009/04/06/rotated-icon/">Rotated Icon</a><br>
 *
 * <p>The RotatedIcon allows you to change the orientation of an Icon by rotating the Icon before
 * it
 * is painted. This class supports the following orientations:
 *
 * <ul>
 *   <li>DOWN - rotated 90 degrees
 *   <li>UP (default) - rotated -90 degrees
 *   <li>UPSIDE_DOWN - rotated 180 degrees
 *   <li>ABOUT_CENTER - the icon is rotated by the specified degrees about its center.
 * </ul>
 *
 * @author Rob Camick
 */
public class RotatedIcon implements Icon {

  private final Icon icon;
  private final Rotate rotate;
  private double degrees;
  private boolean circularIcon;

  /**
   * Convenience constructor to create a RotatedIcon that is rotated DOWN.
   *
   * @param icon the Icon to rotate
   */
  public RotatedIcon(Icon icon) {
    this(icon, Rotate.UP);
  }

  /**
   * Create a RotatedIcon
   *
   * @param icon   the Icon to rotate
   * @param rotate the direction of rotation
   */
  public RotatedIcon(Icon icon, Rotate rotate) {
    this.icon = icon;
    this.rotate = rotate;
  }

  /**
   * Create a RotatedIcon. The icon will rotate about its center. This constructor will
   * automatically set the Rotate enum to ABOUT_CENTER.
   *
   * @param icon    the Icon to rotate
   * @param degrees the degrees of rotation
   */
  public RotatedIcon(Icon icon, double degrees) {
    this(icon, degrees, false);
  }

  /**
   * Create a RotatedIcon. The icon will rotate about its center. This constructor will
   * automatically set the Rotate enum to ABOUT_CENTER.
   *
   * @param icon         the Icon to rotate
   * @param degrees      the degrees of rotation
   * @param circularIcon treat the icon as circular so its size doesn't change
   */
  public RotatedIcon(Icon icon, double degrees, boolean circularIcon) {
    this(icon, Rotate.ABOUT_CENTER);
    setDegrees(degrees);
    setCircularIcon(circularIcon);
  }

  /**
   * Gets the Icon to be rotated
   *
   * @return the Icon to be rotated
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Gets the Rotate enum which indicates the direction of rotation
   *
   * @return the Rotate enum
   */
  public Rotate getRotate() {
    return rotate;
  }

  /**
   * Gets the degrees of rotation. Only used for Rotate.ABOUT_CENTER.
   *
   * @return the degrees of rotation
   */
  public double getDegrees() {
    return degrees;
  }

  /**
   * Set the degrees of rotation. Only used for Rotate.ABOUT_CENTER. This method only sets the
   * degress of rotation, it will not cause the Icon to be repainted. You must invoke repaint() on
   * any component using this icon for it to be repainted.
   *
   * @param degrees the degrees of rotation
   */
  public void setDegrees(double degrees) {
    this.degrees = degrees;
  }

  /**
   * Is the image circular or rectangular? Only used for Rotate.ABOUT_CENTER. When true, the icon
   * width/height will not change as the Icon is rotated.
   *
   * @return true for a circular Icon, false otherwise
   */
  public boolean isCircularIcon() {
    return circularIcon;
  }

  /**
   * Set the Icon as circular or rectangular. Only used for Rotate.ABOUT_CENTER. When true, the icon
   * width/height will not change as the Icon is rotated.
   *
   * @param circularIcon true for a circular Icon, false otherwise
   */
  public void setCircularIcon(boolean circularIcon) {
    this.circularIcon = circularIcon;
  }

  /**
   * Gets the width of this icon.
   *
   * @return the width of the icon in pixels.
   */
  @Override
  public int getIconWidth() {
    if (rotate == Rotate.ABOUT_CENTER) {
      if (circularIcon) {
        return icon.getIconWidth();
      } else {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        return (int) Math.floor(icon.getIconWidth() * cos + icon.getIconHeight() * sin);
      }
    } else if (rotate == Rotate.UPSIDE_DOWN) {
      return icon.getIconWidth();
    } else {
      return icon.getIconHeight();
    }
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
    if (rotate == Rotate.ABOUT_CENTER) {
      if (circularIcon) {
        return icon.getIconHeight();
      } else {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        return (int) Math.floor(icon.getIconHeight() * cos + icon.getIconWidth() * sin);
      }
    } else if (rotate == Rotate.UPSIDE_DOWN) {
      return icon.getIconHeight();
    } else {
      return icon.getIconWidth();
    }
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
    Graphics2D g2 = (Graphics2D) g.create();

    int cWidth = icon.getIconWidth() / 2;
    int cHeight = icon.getIconHeight() / 2;
    int xAdjustment = (icon.getIconWidth() % 2) == 0 ? 0 : -1;
    int yAdjustment = (icon.getIconHeight() % 2) == 0 ? 0 : -1;

    if (rotate == Rotate.DOWN) {

      g2.translate(x + cHeight, y + cWidth);
      g2.rotate(Math.toRadians(90));
      icon.paintIcon(c, g2, -cWidth, yAdjustment - cHeight);

    } else if (rotate == Rotate.UP) {

      g2.translate(x + cHeight, y + cWidth);
      g2.rotate(Math.toRadians(-90));
      icon.paintIcon(c, g2, xAdjustment - cWidth, -cHeight);

    } else if (rotate == Rotate.UPSIDE_DOWN) {

      g2.translate(x + cWidth, y + cHeight);
      g2.rotate(Math.toRadians(180));
      icon.paintIcon(c, g2, xAdjustment - cWidth, yAdjustment - cHeight);

    } else if (rotate == Rotate.ABOUT_CENTER) {

      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setClip(x, y, getIconWidth(), getIconHeight());
      g2.translate(
          (getIconWidth() - icon.getIconWidth()) / 2, (getIconHeight() - icon.getIconHeight()) / 2);
      g2.rotate(Math.toRadians(degrees), x + cWidth, y + cHeight);
      icon.paintIcon(c, g2, x, y);
    }

    g2.dispose();
  }

  public enum Rotate {
    DOWN,
    UP,
    UPSIDE_DOWN,
    ABOUT_CENTER
  }
}
