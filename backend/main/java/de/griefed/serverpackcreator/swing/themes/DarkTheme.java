package de.griefed.serverpackcreator.swing.themes;

import mdlaf.themes.JMarsDarkTheme;

import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.InsetsUIResource;

public class DarkTheme extends JMarsDarkTheme {

    @Override
    protected void installBorders() {
        super.installBorders();
        this.borderPanel = new BorderUIResource(BorderFactory.createEmptyBorder());
        this.tabInsetsTabbedPane = new InsetsUIResource(6, 10, 10, 10);
        this.selectedTabInsetsTabbedPane = new InsetsUIResource(6, 10, 10, 10);
        this.borderFrameRootPane = new BorderUIResource(BorderFactory.createEmptyBorder());
    }

}
