package de.griefed.serverpackcreator.swing.themes;

import mdlaf.themes.JMarsDarkTheme;
import mdlaf.utils.MaterialColors;
import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;

/**
 * This is the dark-theme which ServerPackCreator uses. It is based on {@link JMarsDarkTheme} via <code>extends</code>
 * which allows us to use the base dark-theme as a starting point but changing every aspect of it in whatever way we like.
 * @author Griefed
 */
public class DarkTheme extends JMarsDarkTheme {

    private ColorUIResource selectedBackground = new ColorUIResource(50, 66, 74);

    @Override
    protected void installBorders() {
        super.installBorders();
        this.borderPanel = new BorderUIResource(BorderFactory.createEmptyBorder());
        this.tabInsetsTabbedPane = new InsetsUIResource(6, 10, 10, 10);
        this.selectedTabInsetsTabbedPane = new InsetsUIResource(6, 10, 10, 10);
        this.borderFrameRootPane = new BorderUIResource(BorderFactory.createEmptyBorder());
    }

    @Override
    protected void installDefaultColor() {
        super.installDefaultColor();
        this.buttonDefaultTextColor = this.textColor;

        this.foregroundTableHeader = this.textColor;
        this.selectionForegroundTable = this.highlightBackgroundPrimary;
    }

    @Override
    protected void installColor() {
        this.backgroundPrimary = new ColorUIResource(45, 48, 56);
        this.highlightBackgroundPrimary = new ColorUIResource(66, 179, 176);

        this.textColor = new ColorUIResource(255, 255, 255);
        this.disableTextColor = new ColorUIResource(170, 170, 170);

        this.buttonBackgroundColor = new ColorUIResource(45, 48, 56);
        this.buttonBackgroundColorMouseHover = new ColorUIResource(81, 86, 101);
        this.buttonDefaultBackgroundColorMouseHover = new ColorUIResource(23, 137, 134);
        this.buttonDefaultBackgroundColor = new ColorUIResource(66, 179, 176);
        this.buttonDisabledBackground = new ColorUIResource(66, 69, 76);

        this.buttonFocusColor = buttonDefaultBackgroundColor;
        this.buttonDefaultFocusColor = MaterialColors.WHITE;
        this.buttonBorderColor = MaterialColors.WHITE;
        this.buttonColorHighlight = buttonBackgroundColorMouseHover;

        this.selectedInDropDownBackgroundComboBox = new ColorUIResource(249, 192, 98);
        this.selectedForegroundComboBox = MaterialColors.BLACK;

        this.menuBackground = new ColorUIResource(59, 62, 69);
        this.menuBackgroundMouseHover = new ColorUIResource(249, 192, 98);

        this.trackColorScrollBar = new ColorUIResource(81, 86, 101);
        this.thumbColorScrollBar = new ColorUIResource(155, 155, 155);

        this.trackColorSlider = new ColorUIResource(119, 119, 119);
        this.haloColorSlider = MaterialColors.bleach(new Color(249, 192, 98), 0.2f);

        this.mouseHoverButtonColorSpinner = backgroundPrimary;

        this.highlightColorTabbedPane = new ColorUIResource(45, 48, 56);
        this.borderHighlightColorTabbedPane = new ColorUIResource(45, 48, 56);
        this.focusColorLineTabbedPane = new ColorUIResource(249, 192, 98);
        this.disableColorTabTabbedPane = new ColorUIResource(170, 170, 170);

        this.backgroundTable = new ColorUIResource(45, 48, 56);
        this.backgroundTableHeader = new ColorUIResource(66, 179, 176);
        this.selectionBackgroundTable = new ColorUIResource(126, 132, 153);
        this.gridColorTable = new ColorUIResource(151, 151, 151);
        this.alternateRowBackgroundTable = new ColorUIResource(59, 62, 69);

        this.backgroundTextField = new ColorUIResource(81, 86, 101);
        this.inactiveForegroundTextField = MaterialColors.WHITE;
        this.inactiveBackgroundTextField = new ColorUIResource(81, 86, 101);
        this.selectionBackgroundTextField = new ColorUIResource(249, 192, 98);
        super.disabledBackgroudnTextField = new ColorUIResource(94, 94, 94);
        super.disabledForegroundTextField = new ColorUIResource(170, 170, 170);
        this.selectionForegroundTextField = MaterialColors.BLACK;
        this.inactiveColorLineTextField = MaterialColors.WHITE;
        this.activeColorLineTextField = new ColorUIResource(249, 192, 98);

        this.titleBackgroundGradientStartTaskPane = MaterialColors.GRAY_300;
        this.titleBackgroundGradientEndTaskPane = MaterialColors.GRAY_500;
        this.titleOverTaskPane = new ColorUIResource(249, 192, 98);
        this.specialTitleOverTaskPane = MaterialColors.WHITE;

        this.selectionBackgroundList = new ColorUIResource(249, 192, 98);
        this.selectionForegroundList = MaterialColors.BLACK;

        this.backgroundProgressBar = new ColorUIResource(81, 86, 101);
        this.foregroundProgressBar = MaterialColors.WHITE;

        this.withoutIconSelectedForegoundToggleButton = MaterialColors.BLACK;
        this.withoutIconForegroundToggleButton = MaterialColors.WHITE;

        this.colorDividierSplitPane = MaterialColors.COSMO_DARK_GRAY;
        this.colorDividierFocusSplitPane = new ColorUIResource(249, 192, 98);

        super.backgroundSeparator = MaterialColors.GRAY_300;
        super.foregroundSeparator = MaterialColors.GRAY_300;

        super.backgroundToolTip = backgroundPrimary;
    }
}
