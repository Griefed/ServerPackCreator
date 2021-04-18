package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.Reference;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ReferenceGUI extends Reference {
    static final ImageIcon folderIcon               = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/folder.png")));
    static final ImageIcon startGeneration          = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/start_generation.png")));
    static final ImageIcon bannerIcon               = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/banner.png")));
    static final ImageIcon loadIcon                 = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/load.png")));
    static final ImageIcon issueIcon                = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/issue.png")));
    static final ImageIcon pastebinIcon             = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/pastebin.png")));
    static final ImageIcon prosperIcon              = new ImageIcon(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/prosper.png")));

    static final Image icon = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(TabbedPane.class.getResource("/de/griefed/resources/gui/app.png")));

    static final Color backgroundColour = new Color(255,248,235);

    static final Dimension windowDimension          = new Dimension(800,860);
    static final Dimension chooserDimension         = new Dimension(750,450);

    static final Dimension folderButtonDimension    = new Dimension(24,24);
    static final Dimension miscButtonDimension      = new Dimension(50,50);
    static final Dimension startGenerationButton    = new Dimension(120,70);
}