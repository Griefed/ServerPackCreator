package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.Reference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class TabbedPane extends JPanel {
    private static final Logger appLogger = LogManager.getLogger(TabbedPane.class);

    public void main() {
        SwingUtilities.invokeLater(() -> {
            //Bold fonts = true, else false
            UIManager.put("swing.boldMetal", true);
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                appLogger.error("Error: There was an error setting the look and feel.", ex);
            }
            createAndShowGUI();
        });
        /*
        LOOK AND FEEL:
        Possibly restricted to platforms:
        com.sun.java.swing.plaf.motif.MotifLookAndFeel
        com.sun.java.swing.plaf.windows.WindowsLookAndFeel
        com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel

        Possibly cross-platform:
        javax.swing.plaf.nimbus.NimbusLookAndFeel
        javax.swing.plaf.metal.MetalLookAndFeel
         */
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("serverpackcreator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setIconImage(ReferenceGUI.icon);
        JLabel banner = new JLabel(ReferenceGUI.bannerIcon);
        banner.setOpaque(true);

        frame.add(banner, BorderLayout.PAGE_START);
        frame.add(new TabbedPane(), BorderLayout.CENTER);

        frame.setMinimumSize(ReferenceGUI.panelDimension);
        frame.setPreferredSize(ReferenceGUI.panelDimension);
        frame.setMaximumSize(ReferenceGUI.panelDimension);
        frame.setResizable(false);

        frame.pack();
        frame.setVisible(true);
    }


    public TabbedPane() {
        super(new GridLayout(1, 1));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ReferenceGUI.backgroundColour);

        tabbedPane.addTab("Create Server Pack", null, new CreateServerPack().createServerPack() , "Configure and start generation of server pack.");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        tabbedPane.addTab("serverpackcreator Log",null, new ServerPackCreatorLog().serverPackCreatorLog(), "Latest serverpackcreator log tail.");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        tabbedPane.addTab("Modloader-Installer Log",null, new ModloaderInstallerLog().modloaderInstallerLog(), "Latest modloader-installer log tail.");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        tabbedPane.addTab("About", null, new About().about(), "Info, Updates, Support.");
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

        //Add the tabbed pane to this panel.
        add(tabbedPane);

        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }
}
