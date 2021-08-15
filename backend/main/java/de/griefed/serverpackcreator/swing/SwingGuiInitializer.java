/* Copyright (C) 2021  Griefed
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
package de.griefed.serverpackcreator.swing;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.AddonsHandler;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.utilities.BackgroundPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * <strong>Table of methods</strong><br>
 * {@link #SwingGuiInitializer(LocalizationManager, ConfigurationHandler, CurseCreateModpack, ServerPackHandler, AddonsHandler)}<br>
 * {@link #mainGUI()}<br>
 * {@link #createAndShowGUI()}<p>
 * This class creates and shows the GUI needed for running ServerPackCreator in....well...GUI mode. Calls {@link #mainGUI()}
 * which then calls {@link #createAndShowGUI()} in order to create and show the GUI of ServerPackCreator. Instances of
 * the {@link TabCreateServerPack}, {@link TabServerPackCreatorLog}, {@link TabModloaderInstallerLog}, {@link TabAbout}
 * are created in the constructor of this class to make sure they are ready when the GUI is created and shown to the user.
 * @author Griefed
 */
public class SwingGuiInitializer extends JPanel {
    private static final Logger LOG = LogManager.getLogger(SwingGuiInitializer.class);

    private final ImageIcon ICON_SERVERPACKCREATOR_BANNER = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/banner.png")));
    private final Image ICON_SERVERPACKCREATOR = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/app.png")));
    private final Dimension DIMENSION_WINDOW = new Dimension(800,860);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final ServerPackHandler CREATESERVERPACK;
    private final AddonsHandler ADDONSHANDLER;

    private final BackgroundPanel BACKGROUNDPANEL;
    private final JFrame FRAME_SERVERPACKCREATOR;

    private File secretFile;
    private Config secret;
    private BufferedImage bufferedImage;
    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link ConfigurationHandler} required to successfully and correctly create the server pack.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * Receives an instance of {@link ServerPackHandler} which is required to generate a server pack.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler} required to successfully and correctly create the server pack.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedServerPackHandler Instance of {@link ServerPackHandler} required for the generation of server packs.
     * @param injectedAddonsHandler Instance of {@link AddonsHandler} required for accessing installed addons, if any exist.
     */
    public SwingGuiInitializer(LocalizationManager injectedLocalizationManager, ConfigurationHandler injectedConfigurationHandler, CurseCreateModpack injectedCurseCreateModpack, ServerPackHandler injectedServerPackHandler, AddonsHandler injectedAddonsHandler) {
        super(new GridLayout(1, 1));

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedConfigurationHandler == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedAddonsHandler == null) {
            this.ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER);
        } else {
            this.ADDONSHANDLER = injectedAddonsHandler;
        }

        if (injectedConfigurationHandler == null) {
            this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK);
        } else {
            this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        }

        if (injectedServerPackHandler == null) {
            this.CREATESERVERPACK = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER);
        } else {
            this.CREATESERVERPACK = injectedServerPackHandler;
        }



        try { bufferedImage = ImageIO.read(Objects.requireNonNull(getClass().getResource("/de/griefed/resources/gui/tile.png")));}
        catch (IOException ex) { LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createandshowgui.image"), ex); }

        TabCreateServerPack TAB_CREATESERVERPACK = new TabCreateServerPack(LOCALIZATIONMANAGER, CONFIGURATIONHANDLER, CURSECREATEMODPACK, CREATESERVERPACK, ADDONSHANDLER);
        TabServerPackCreatorLog TAB_LOG_SERVERPACKCREATOR = new TabServerPackCreatorLog(LOCALIZATIONMANAGER);
        TabModloaderInstallerLog TAB_LOG_MODLOADERINSTALLER = new TabModloaderInstallerLog(LOCALIZATIONMANAGER);
        TabAddonsHandlerLog TAB_LOG_ADDONSHANDLER = new TabAddonsHandlerLog(LOCALIZATIONMANAGER);
        TabAbout TAB_ABOUT = new TabAbout(LOCALIZATIONMANAGER);

        FRAME_SERVERPACKCREATOR = new JFrame(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createandshowgui"));

        BACKGROUNDPANEL = new BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f);

        JTabbedPane TABBEDPANE = new JTabbedPane(JTabbedPane.TOP);

        /*
         * Remove the border insets so the panes fully fill out the area available to them. Prevents the image
         * painted by BackgroundPanel from being displayed along the border of the pane.
         */
        TABBEDPANE.setUI(new BasicTabbedPaneUI() {
            private final Insets borderInsets = new Insets(0, 0, 0, 0);

            @Override
            protected Insets getContentBorderInsets(int tabPlacement) {
                return borderInsets;
            }
        });

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.title"),
                null,
                TAB_CREATESERVERPACK.createServerPackTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.tip"));

        TABBEDPANE.setMnemonicAt(0, KeyEvent.VK_1);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"),
                null,
                TAB_LOG_SERVERPACKCREATOR.serverPackCreatorLogTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));

        TABBEDPANE.setMnemonicAt(1, KeyEvent.VK_2);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.title"),
                null,
                TAB_LOG_MODLOADERINSTALLER.modloaderInstallerLogTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.tip"));

        TABBEDPANE.setMnemonicAt(2, KeyEvent.VK_3);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.addonshandlerlog.title"),
                null,
                TAB_LOG_ADDONSHANDLER.addonsHandlerLogTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.addonshandlerlog.tip"));

        TABBEDPANE.setMnemonicAt(3, KeyEvent.VK_4);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.about.title"),
                null,
                TAB_ABOUT.aboutTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.about.tip"));

        TABBEDPANE.setMnemonicAt(4, KeyEvent.VK_5);

        TABBEDPANE.setOpaque(true);

        add(TABBEDPANE);

        /*
         * We need both in order to have a transparent TabbedPane
         * behind which we can see the image painted by BackgroundPanel
         */
        setOpaque(false);
        TABBEDPANE.setOpaque(false);

        TABBEDPANE.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * Shows the GUI from the EDT by using SwingUtilities, and it's invokeLater method by calling {@link #createAndShowGUI()}.
     * Sets the font to bold, which may be overridden by the LookAndFeel which gets automatically determined and depends
     * on the OS ServerPackCreator is run on.
     * @author Griefed
     */
    public void mainGUI() {
        SwingUtilities.invokeLater(() -> {
            // Bold fonts = true
            UIManager.put("swing.boldMetal", true);
            UIManager.put("Slider.focus", UIManager.get("Slider.background"));
            /*
             * A little secret setting which allows the user to temporarily overwrite the Look and Feel of ServerPackCreator.
             * Note: This setting is not carried over to new configuration files written by ServerPackCreator when,
             * for example, the configuration file for a modpack acquired from CurseForge is written.
             * It was just something I wanted to play around with. So I did.
             */
            try {
                if (new File("serverpackcreator.conf").exists()) {

                    secretFile = new File("serverpackcreator.conf");
                    secret = ConfigFactory.parseFile(secretFile);

                    if (secret.getString("topsicrets") != null && !secret.getString("topsicrets").equals("") && secret.getString("topsicrets").length() > 0) {

                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("topsicrets"));
                        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("topsicrets.moar"));
                        for (UIManager.LookAndFeelInfo look : UIManager.getInstalledLookAndFeels()) {
                            LOG.info(look.getClassName());
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
                    //UIManager.setLookAndFeel(new MaterialLookAndFeel(new JMarsDarkTheme()));
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("tabbedpane.log.error"), ex);
                }
            }

            createAndShowGUI();
        });
    }

    /**
     * Creates the frame in which the banner, tabbed pane with all the tabs, icon and title are displayed and shows it.
     * @author Griefed
     */
    private void createAndShowGUI() {

        FRAME_SERVERPACKCREATOR.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        FRAME_SERVERPACKCREATOR.setContentPane(BACKGROUNDPANEL);

        FRAME_SERVERPACKCREATOR.setIconImage(ICON_SERVERPACKCREATOR);

        JLabel serverPackCreatorBanner = new JLabel(ICON_SERVERPACKCREATOR_BANNER);
        serverPackCreatorBanner.setOpaque(false);

        FRAME_SERVERPACKCREATOR.add(serverPackCreatorBanner, BorderLayout.PAGE_START);

        FRAME_SERVERPACKCREATOR.add(new SwingGuiInitializer(LOCALIZATIONMANAGER, CONFIGURATIONHANDLER, CURSECREATEMODPACK, CREATESERVERPACK, ADDONSHANDLER), BorderLayout.CENTER);

        FRAME_SERVERPACKCREATOR.setSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setPreferredSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setMaximumSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setResizable(true);

        FRAME_SERVERPACKCREATOR.pack();

        FRAME_SERVERPACKCREATOR.setVisible(true);
    }
}