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
 */
package de.griefed.serverpackcreator.swing.themes;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import mdlaf.shadows.DropShadowBorder;
import mdlaf.themes.MaterialLiteTheme;
import mdlaf.utils.MaterialBorders;
import mdlaf.utils.MaterialColors;

/**
 * This is the light-theme which ServerPackCreator uses. It is based on {@link MaterialLiteTheme}
 * via <code>extends</code> which allows us to use the base light-theme as a starting point but
 * changing every aspect of it in whatever way we like.
 *
 * @author Griefed
 */
public class LightTheme extends MaterialLiteTheme {

  private Color textErrorColour;

  public Color getTextErrorColour() {
    return textErrorColour;
  }

  @Override
  protected void installBorders() {
    super.installBorders();
    this.borderMenuBar =
        new BorderUIResource(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 156, 43)));
    this.borderPopupMenu = new BorderUIResource(BorderFactory.createLineBorder(backgroundPrimary));
    this.borderSpinner = new BorderUIResource(BorderFactory.createLineBorder(backgroundTextField));
    this.borderSlider =
        new BorderUIResource(
            BorderFactory.createCompoundBorder(
                borderSpinner, BorderFactory.createEmptyBorder(15, 15, 15, 15)));
    this.cellBorderTableHeader =
        new BorderUIResource(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundTableHeader),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    this.borderToolBar = borderSpinner;
    this.borderDialogRootPane = MaterialBorders.LIGHT_SHADOW_BORDER;
    this.borderProgressBar = borderSpinner;
    this.borderTable = borderSpinner;
    this.borderTableHeader =
        new BorderUIResource(
            new DropShadowBorder(this.backgroundPrimary, 5, 3, 0.4f, 12, true, true, true, true));
    super.borderTitledBorder =
        new BorderUIResource(BorderFactory.createLineBorder(MaterialColors.WHITE));
    super.titleColorTaskPane = MaterialColors.BLACK;
  }

  @Override
  protected void installColor() {
    super.installColor();
    ColorUIResource secondBackground = new ColorUIResource(238, 238, 238);
    ColorUIResource disableBackground = new ColorUIResource(210, 212, 213);
    ColorUIResource accentColor = new ColorUIResource(231, 231, 232);
    ColorUIResource selectedForeground = new ColorUIResource(84, 110, 122);
    ColorUIResource selectedBackground = new ColorUIResource(220, 239, 237);
    this.backgroundPrimary = new ColorUIResource(240, 240, 240);
    this.highlightBackgroundPrimary = new ColorUIResource(0, 188, 212);

    this.textColor = new ColorUIResource(0, 0, 0);
    this.textErrorColour = Color.magenta;
    this.disableTextColor = new ColorUIResource(148, 167, 176);

    this.buttonBackgroundColor = new ColorUIResource(243, 244, 245);
    this.buttonBackgroundColorMouseHover = new ColorUIResource(231, 231, 232);
    this.buttonDefaultBackgroundColorMouseHover = this.buttonBackgroundColorMouseHover;
    this.buttonDefaultBackgroundColor = secondBackground;
    this.buttonDisabledBackground = disableBackground;
    this.buttonDefaultFocusColor = this.buttonFocusColor;
    this.buttonBorderColor = new ColorUIResource(211, 225, 232);
    this.buttonColorHighlight = selectedBackground;
    this.buttonFocusColor = new ColorUIResource(new Color(0, 0, 0, 0));
    this.selectedInDropDownBackgroundComboBox = this.buttonBackgroundColorMouseHover;
    this.selectedForegroundComboBox = this.textColor;

    this.menuBackground = this.backgroundPrimary;
    this.menuBackgroundMouseHover = this.buttonBackgroundColorMouseHover;

    this.arrowButtonColorScrollBar = this.buttonBackgroundColor;
    this.trackColorScrollBar = accentColor;
    this.thumbColorScrollBar = disableBackground;

    this.trackColorSlider = this.textColor;
    this.haloColorSlider = MaterialColors.bleach(this.highlightBackgroundPrimary, 0.5f);

    this.highlightColorTabbedPane = this.buttonColorHighlight;
    this.borderHighlightColorTabbedPane = this.buttonColorHighlight;
    this.focusColorLineTabbedPane = this.highlightBackgroundPrimary;
    this.disableColorTabTabbedPane = disableBackground;

    this.backgroundTable = this.backgroundPrimary;
    this.backgroundTableHeader = this.backgroundPrimary;
    this.selectionBackgroundTable = this.buttonBackgroundColorMouseHover;
    this.gridColorTable = this.backgroundPrimary;
    this.alternateRowBackgroundTable = this.backgroundPrimary;

    this.backgroundTextField = accentColor;
    this.inactiveForegroundTextField = this.textColor;
    this.inactiveBackgroundTextField = accentColor;
    this.selectionBackgroundTextField = selectedBackground;
    this.selectionForegroundTextField = selectedForeground;
    super.disabledBackgroudnTextField = disableBackground;
    super.disabledForegroundTextField = this.disableTextColor;
    this.inactiveColorLineTextField = this.textColor;
    this.activeColorLineTextField = this.highlightBackgroundPrimary;

    this.mouseHoverButtonColorSpinner = this.buttonBackgroundColorMouseHover;
    this.titleBackgroundGradientStartTaskPane = secondBackground;
    this.titleBackgroundGradientEndTaskPane = secondBackground;
    this.titleOverTaskPane = selectedForeground;
    this.specialTitleOverTaskPane = selectedForeground;
    this.backgroundTaskPane = this.backgroundPrimary;
    this.borderColorTaskPane = new ColorUIResource(211, 225, 232);
    this.contentBackgroundTaskPane = secondBackground;

    this.selectionBackgroundList = selectedBackground;
    this.selectionForegroundList = selectedForeground;

    this.backgroundProgressBar = disableBackground;
    this.foregroundProgressBar = this.highlightBackgroundPrimary;

    this.withoutIconSelectedBackgroundToggleButton = MaterialColors.COSMO_DARK_GRAY;
    this.withoutIconSelectedForegoundToggleButton = MaterialColors.BLACK;
    this.withoutIconBackgroundToggleButton = MaterialColors.GRAY_300;
    this.withoutIconForegroundToggleButton = MaterialColors.BLACK;

    this.colorDividierSplitPane = MaterialColors.COSMO_DARK_GRAY;
    this.colorDividierFocusSplitPane = selectedBackground;

    super.backgroundSeparator = MaterialColors.GRAY_300;
    super.foregroundSeparator = MaterialColors.GRAY_300;
  }

  @Override
  public boolean getButtonBorderEnableToAll() {
    return true;
  }

  @Override
  public boolean getButtonBorderEnable() {
    return true;
  }

  @Override
  public ColorUIResource getGridColorTable() {
    return this.gridColorTable;
  }
}
