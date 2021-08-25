package de.griefed.serverpackcreator.swing.themes;

import mdlaf.themes.MaterialLiteTheme;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;

public class LightTheme extends MaterialLiteTheme {

    @Override
    protected void installBorders() {
        super.installBorders();
        borderMenuBar = new BorderUIResource(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 156, 43)));
        this.borderPanel = new BorderUIResource(BorderFactory.createEmptyBorder());
        this.borderFrameRootPane = new BorderUIResource(BorderFactory.createEmptyBorder());
    }

}
