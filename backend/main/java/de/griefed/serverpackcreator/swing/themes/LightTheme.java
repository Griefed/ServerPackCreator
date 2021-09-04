package de.griefed.serverpackcreator.swing.themes;

import mdlaf.shadows.DropShadowBorder;
import mdlaf.themes.MaterialLiteTheme;
import mdlaf.utils.MaterialBorders;
import mdlaf.utils.MaterialColors;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;

/**
 * This is the light-theme which ServerPackCreator uses. It is based on {@link MaterialLiteTheme} via <code>extends</code>
 * which allows us to use the base light-theme as a starting point but changing every aspect of it in whatever way we like.
 * @author Griefed
 */
public class LightTheme extends MaterialLiteTheme {

    @Override
    protected void installBorders() {
        super.installBorders();
        borderMenuBar =
                new BorderUIResource(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 156, 43)));
        borderPopupMenu = new BorderUIResource(BorderFactory.createLineBorder(backgroundPrimary));
        borderSpinner = new BorderUIResource(BorderFactory.createLineBorder(backgroundTextField));
        borderSlider =
                new BorderUIResource(
                        BorderFactory.createCompoundBorder(
                                borderSpinner, BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        cellBorderTableHeader =
                new BorderUIResource(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(backgroundTableHeader),
                                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        borderToolBar = borderSpinner;

        borderDialogRootPane = MaterialBorders.LIGHT_SHADOW_BORDER;

        borderProgressBar = borderSpinner;

        // this.borderComboBox = MaterialBorders.roundedLineColorBorder(MaterialColors.WHITE,
        // this.getArchBorderComboBox());
        this.borderTable = borderSpinner;
        this.borderTableHeader =
                new BorderUIResource(
                        new DropShadowBorder(this.backgroundPrimary, 5, 3, 0.4f, 12, true, true, true, true));

        super.borderTitledBorder =
                new BorderUIResource(BorderFactory.createLineBorder(MaterialColors.WHITE));

        super.titleColorTaskPane = MaterialColors.BLACK;
    }

}
