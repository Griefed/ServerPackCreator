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
package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Configuration;
import de.griefed.serverpackcreator.CreateServerPack;
import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * <strong>Table of methods</strong><br>
 * {@link #CreateGui(LocalizationManager, Configuration, CurseCreateModpack, CreateServerPack)}<br>
 * {@link #mainGUI()}<br>
 * {@link #createAndShowGUI()}<p>
 * This class creates and shows the GUI needed for running ServerPackCreator in....well...GUI mode. Calls {@link #mainGUI()}
 * which then calls {@link #createAndShowGUI()} in order to create and show the GUI of ServerPackCreator. Instances of
 * the {@link CreateServerPackTab}, {@link ServerPackCreatorLogTab}, {@link ModloaderInstallerLogTab}, {@link AboutTab}
 * are created in the constructor of this class to make sure they are ready when the GUI is created and shown to the user.
 */
public class CreateGui extends JPanel {
    private static final Logger appLogger = LogManager.getLogger(CreateGui.class);

    private final ImageIcon bannerIcon      = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/banner.png")));
    private final Image icon                = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/app.png")));
    private final Dimension windowDimension = new Dimension(800,860);
    private final Image tile                = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/tile.png")));
    private BufferedImage bufferedImage;
    {
        try {
            bufferedImage = ImageIO.read(getClass().getResource("/de/griefed/resources/gui/tile.png"));
        } catch (IOException ex) {
            //Can't use localization here.
            appLogger.error("Could not read image for tiling.", ex);
        }
    }

    private LocalizationManager localizationManager;
    private Configuration configuration;
    private CurseCreateModpack curseCreateModpack;
    private CreateServerPack createServerPack;

    private CreateServerPackTab createServerPackTab;
    private ServerPackCreatorLogTab serverPackCreatorLogTab;
    private ModloaderInstallerLogTab modloaderInstallerLogTab;
    private AboutTab aboutTab;
    private BackgroundPanel backgroundPanel;
    private JTabbedPane tabbedPane;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link Configuration} required to successfully and correctly create the server pack.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * Receives an instance of {@link CreateServerPack} which is required to generate a server pack.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedConfiguration Instance of {@link Configuration} required to successfully and correctly create the server pack.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedCreateServerPack Instance of {@link CreateServerPack} required for the generation of server packs.
     */
    public CreateGui(LocalizationManager injectedLocalizationManager, Configuration injectedConfiguration, CurseCreateModpack injectedCurseCreateModpack, CreateServerPack injectedCreateServerPack) {
        super(new GridLayout(1, 1));

        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }

        if (injectedConfiguration == null) {
            this.curseCreateModpack = new CurseCreateModpack(localizationManager);
        } else {
            this.curseCreateModpack = injectedCurseCreateModpack;
        }

        if (injectedConfiguration == null) {
            this.configuration = new Configuration(localizationManager, curseCreateModpack);
        } else {
            this.configuration = injectedConfiguration;
        }

        if (injectedCreateServerPack == null) {
            this.createServerPack = new CreateServerPack(localizationManager, configuration, curseCreateModpack);
        } else {
            this.createServerPack = injectedCreateServerPack;
        }

        createServerPackTab = new CreateServerPackTab(localizationManager, configuration, curseCreateModpack, createServerPack);
        serverPackCreatorLogTab = new ServerPackCreatorLogTab(localizationManager);
        modloaderInstallerLogTab = new ModloaderInstallerLogTab(localizationManager);
        aboutTab = new AboutTab(localizationManager);
        serverPackCreatorFrame = new JFrame(localizationManager.getLocalizedString("createserverpack.gui.createandshowgui"));
        backgroundPanel = new BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f);
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        tabbedPane.setUI(new BasicTabbedPaneUI() {
            private final Insets borderInsets = new Insets(0, 0, 0, 0);

            /*
             * Remove the border insets so the panes fully fill out the area available to them. Prevents the image painted
             * by BackgroundPanel from being displayed along the border of the pane.
             */
            @Override
            protected Insets getContentBorderInsets(int tabPlacement) {
                return borderInsets;
            }
        });

        tabbedPane.addTab(
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.title"),
                null,
                createServerPackTab.createServerPackTab(),
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.tip"));

        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        tabbedPane.addTab(
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"),
                null,
                serverPackCreatorLogTab.serverPackCreatorLogTab(),
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));

        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        tabbedPane.addTab(
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.title"),
                null,
                modloaderInstallerLogTab.modloaderInstallerLogTab(),
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.tip"));

        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        tabbedPane.addTab(
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.title"),
                null,
                aboutTab.aboutTab(),
                localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.tip"));

        tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

        tabbedPane.setOpaque(true);

        add(tabbedPane);

        // We need both in order to have a transparent TabbedPane
        // behind which we can see the image painted by BackgroundPanel
        setOpaque(false);
        tabbedPane.setOpaque(false);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    private JFrame serverPackCreatorFrame;
    private JLabel serverPackCreatorBanner;
    private File secretFile;
    private Config secret;

    /**
     * Shows the GUI from the EDT by using SwingUtilities and it's invokeLater method by calling {@link #createAndShowGUI()}.
     * Sets the font to bold, which may be overridden by the LookAndFeel which gets automatically determined and depends
     * on the OS ServerPackCreator is run on.
     */
    public void mainGUI() {
        SwingUtilities.invokeLater(() -> {
            //Bold fonts = true
            UIManager.put("swing.boldMetal", true);

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

                        appLogger.info(localizationManager.getLocalizedString("topsicrets"));
                        appLogger.info(localizationManager.getLocalizedString("topsicrets.moar"));
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
                    appLogger.error(localizationManager.getLocalizedString("tabbedpane.log.error"), ex);
                }
            }

            createAndShowGUI();
        });
    }

    /**
     * Creates the frame in which the banner, tabbed pane with all the tabs, icon and title are displayed and shows it.
     */
    private void createAndShowGUI() {

        serverPackCreatorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        serverPackCreatorFrame.setContentPane(backgroundPanel);

        serverPackCreatorFrame.setIconImage(icon);

        serverPackCreatorBanner = new JLabel(bannerIcon);
        serverPackCreatorBanner.setOpaque(false);

        serverPackCreatorFrame.add(serverPackCreatorBanner, BorderLayout.PAGE_START);

        serverPackCreatorFrame.add(new CreateGui(localizationManager, configuration, curseCreateModpack, createServerPack), BorderLayout.CENTER);

        serverPackCreatorFrame.setSize(windowDimension);
        serverPackCreatorFrame.setPreferredSize(windowDimension);
        serverPackCreatorFrame.setMaximumSize(windowDimension);
        serverPackCreatorFrame.setResizable(true);

        serverPackCreatorFrame.pack();

        serverPackCreatorFrame.setVisible(true);
    }
}