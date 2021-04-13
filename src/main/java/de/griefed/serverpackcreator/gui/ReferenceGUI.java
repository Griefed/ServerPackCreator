package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.Reference;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ReferenceGUI extends Reference {
    static final ImageIcon folderIcon               = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/folder.png")));
    static final ImageIcon serverPackCreatorIcon    = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/icon.png")));
    static final ImageIcon startGeneration          = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/start_generation.png")));
    static final ImageIcon bannerIcon               = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/banner.png")));
    static final ImageIcon loadIcon                 = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/load.png")));
    static final ImageIcon configIcon               = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/config.png")));
    static final ImageIcon settingsIcon             = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/settings.png")));

    static final Image icon = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/app.png")));

    static final Color backgroundColour = new Color(255,248,235);

    static final Dimension panelDimension           = new Dimension(600,800);
    static final Dimension folderButtonDimension    = new Dimension(24,24);
    static final Dimension miscButtonDimension      = new Dimension(50,50);
}
