package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.FilesSetup;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class TabbedPane extends JPanel {
    private static final Logger appLogger = LogManager.getLogger(TabbedPane.class);

    public void mainGUI() {
        SwingUtilities.invokeLater(() -> {
            //Bold fonts = true, else false
            UIManager.put("swing.boldMetal", true);

            try {
                if (new File("serverpackcreator.conf").exists()) {

                    File configFile = new File("serverpackcreator.conf");
                    Config secret = ConfigFactory.parseFile(configFile);

                    if (secret.getString("topsicrets") != null && !secret.getString("topsicrets").equals("") && secret.getString("topsicrets").length() > 0) {

                        appLogger.info(LocalizationManager.getLocalizedString("topsicrets"));
                        appLogger.info(LocalizationManager.getLocalizedString("topsicrets.moar"));
                        for (UIManager.LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
                            appLogger.info(look.getClassName());
                        }

                        UIManager.setLookAndFeel(secret.getString("topsicrets"));

                    } else {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }

                } else {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }

            } catch (ConfigException | NullPointerException | ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    appLogger.error(LocalizationManager.getLocalizedString("tabbedpane.log.error"), ex);
                }
            }
            createAndShowGUI();
        });
    }

    private void createAndShowGUI() {

        JFrame frame = new JFrame(LocalizationManager.getLocalizedString("createserverpack.gui.createandshowgui"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setIconImage(ReferenceGUI.icon);
        JLabel banner = new JLabel(ReferenceGUI.bannerIcon);
        banner.setOpaque(false);

        frame.add(banner, BorderLayout.PAGE_START);
        frame.add(new TabbedPane(), BorderLayout.CENTER);

        frame.setMinimumSize(ReferenceGUI.windowDimension);
        frame.setPreferredSize(ReferenceGUI.windowDimension);
        frame.setMaximumSize(ReferenceGUI.windowDimension);
        frame.setResizable(false);

        frame.pack();

        frame.setVisible(true);
    }

    public TabbedPane() {
        super(new GridLayout(1, 1));

        if (FilesSetup.checkLocaleFile()) {

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setBackground(ReferenceGUI.backgroundColour);

            tabbedPane.addTab(LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.title"), null, new CreateServerPack().createServerPack(), LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.tip"));
            tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

            tabbedPane.addTab(LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"), null, new ServerPackCreatorLog().serverPackCreatorLog(), LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));
            tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

            tabbedPane.addTab(LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.title"), null, new ModloaderInstallerLog().modloaderInstallerLog(), "createserverpack.gui.tabbedpane.modloaderinstallerlog.tip");
            tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

            tabbedPane.addTab(LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.title"), null, new About().about(), LocalizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.tip"));
            tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

            //Add the tabbed pane to this panel.
            add(tabbedPane);

            //The following line enables to use scrolling tabs.
            tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }
    }
}
