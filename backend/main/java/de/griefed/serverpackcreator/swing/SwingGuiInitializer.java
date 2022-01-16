/* Copyright (C) 2022  Griefed
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

import de.griefed.serverpackcreator.AddonsHandler;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.utilities.BackgroundPanel;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.VersionLister;
import de.griefed.serverpackcreator.utilities.*;
import mdlaf.MaterialLookAndFeel;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

/**
 * This class creates and shows the GUI needed for running ServerPackCreator in....well...GUI mode. Calls {@link #mainGUI()}
 * which then calls {@link #createAndShowGUI()} in order to create and show the GUI of ServerPackCreator. Instances of
 * the {@link TabCreateServerPack}, {@link TabServerPackCreatorLog}, {@link TabModloaderInstallerLog}
 * are created in the constructor of this class to make sure they are ready when the GUI is created and shown to the user.
 * @author Griefed
 */
public class SwingGuiInitializer extends JPanel {

    private static final Logger LOG = LogManager.getLogger(SwingGuiInitializer.class);

    private final ImageIcon ICON_SERVERPACKCREATOR_BANNER = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/banner.png")));
    private final Image ICON_SERVERPACKCREATOR = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/app.png")));
    private final Dimension DIMENSION_WINDOW = new Dimension(1050,800);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final ServerPackHandler CREATESERVERPACK;
    private final AddonsHandler ADDONSHANDLER;
    private final VersionLister VERSIONLISTER;
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;
    private final SystemUtilities SYSTEMUTILITIES;
    private final ApplicationProperties APPLICATIONPROPERTIES;

    private final LightTheme LIGHTTHEME = new LightTheme();
    private final DarkTheme DARKTHEME = new DarkTheme();

    private final MaterialLookAndFeel LAF_LIGHT = new MaterialLookAndFeel(LIGHTTHEME);
    private final MaterialLookAndFeel LAF_DARK = new MaterialLookAndFeel(DARKTHEME);

    private final BackgroundPanel BACKGROUNDPANEL;

    private final JFrame FRAME_SERVERPACKCREATOR;

    private final TabCreateServerPack TAB_CREATESERVERPACK;
    private final TabServerPackCreatorLog TAB_LOG_SERVERPACKCREATOR;
    private final TabModloaderInstallerLog TAB_LOG_MODLOADERINSTALLER;
    private final TabAddonsHandlerLog TAB_LOG_ADDONSHANDLER;

    private final JTabbedPane TABBEDPANE;

    private final MenuBar MENUBAR;

    private BufferedImage bufferedImage;

    private String update;

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
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     * @param injectedVersionLister Instance of {@link VersionLister} required for everything version related in the GUI.
     * @param injectedBooleanUtilities Instance of {@link BooleanUtilities}.
     * @param injectedListUtilities Instance of {@link ListUtilities}.
     * @param injectedStringUtilities Instance of {@link StringUtilities}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
     * @param injectedSystemUtilities Instance of {@link SystemUtilities}.
     * @param updater String. Update message fetcher from Main.
     */
    public SwingGuiInitializer(LocalizationManager injectedLocalizationManager, ConfigurationHandler injectedConfigurationHandler,
                               CurseCreateModpack injectedCurseCreateModpack, ServerPackHandler injectedServerPackHandler,
                               AddonsHandler injectedAddonsHandler, ApplicationProperties injectedApplicationProperties,
                               VersionLister injectedVersionLister, BooleanUtilities injectedBooleanUtilities, ListUtilities injectedListUtilities,
                               StringUtilities injectedStringUtilities, ConfigUtilities injectedConfigUtilities, SystemUtilities injectedSystemUtilities,
                               String updater) {

        super(new GridLayout(1, 1));

        this.update = updater;

        if (injectedApplicationProperties == null) {
            this.APPLICATIONPROPERTIES = new ApplicationProperties();
        } else {
            this.APPLICATIONPROPERTIES = injectedApplicationProperties;
        }
        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(APPLICATIONPROPERTIES);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }

        if (injectedBooleanUtilities == null) {
            this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.BOOLEANUTILITIES = injectedBooleanUtilities;
        }

        if (injectedListUtilities == null) {
            this.LISTUTILITIES = new ListUtilities();
        } else {
            this.LISTUTILITIES = injectedListUtilities;
        }

        if (injectedStringUtilities == null) {
            this.STRINGUTILITIES = new StringUtilities();
        } else {
            this.STRINGUTILITIES = injectedStringUtilities;
        }

        if (injectedSystemUtilities == null) {
            this.SYSTEMUTILITIES = new SystemUtilities();
        } else {
            this.SYSTEMUTILITIES = injectedSystemUtilities;
        }

        if (injectedConfigUtilities == null) {
            this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES);
        } else {
            this.CONFIGUTILITIES = injectedConfigUtilities;
        }

        if (injectedConfigurationHandler == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedAddonsHandler == null) {
            this.ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES);
        } else {
            this.ADDONSHANDLER = injectedAddonsHandler;
        }

        if (injectedConfigurationHandler == null) {
            this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, APPLICATIONPROPERTIES, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES);
        } else {
            this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        }

        if (injectedServerPackHandler == null) {
            this.CREATESERVERPACK = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES);
        } else {
            this.CREATESERVERPACK = injectedServerPackHandler;
        }

        try {
            bufferedImage = ImageIO.read(Objects.requireNonNull(getClass().getResource("/de/griefed/resources/gui/tile.png")));
        } catch (IOException ex) {
            LOG.error("Could not read image for tiling.", ex);
        }

        this.FRAME_SERVERPACKCREATOR = new JFrame(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createandshowgui") + " - " + APPLICATIONPROPERTIES.getServerPackCreatorVersion());

        this.TAB_CREATESERVERPACK = new TabCreateServerPack(
                LOCALIZATIONMANAGER, CONFIGURATIONHANDLER, CURSECREATEMODPACK, CREATESERVERPACK, ADDONSHANDLER, VERSIONLISTER, APPLICATIONPROPERTIES, FRAME_SERVERPACKCREATOR, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES, SYSTEMUTILITIES
        );

        this.TAB_LOG_SERVERPACKCREATOR = new TabServerPackCreatorLog(
                LOCALIZATIONMANAGER, APPLICATIONPROPERTIES
        );

        this.TAB_LOG_MODLOADERINSTALLER = new TabModloaderInstallerLog(
                LOCALIZATIONMANAGER, APPLICATIONPROPERTIES
        );

        this.TAB_LOG_ADDONSHANDLER = new TabAddonsHandlerLog(
                LOCALIZATIONMANAGER, APPLICATIONPROPERTIES
        );

        this.BACKGROUNDPANEL = new BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f);

        this.TABBEDPANE = new JTabbedPane(JTabbedPane.TOP);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.title"),
                null,
                TAB_CREATESERVERPACK.createServerPackTab(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.tip"));

        TABBEDPANE.setMnemonicAt(0, KeyEvent.VK_1);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"),
                null,
                TAB_LOG_SERVERPACKCREATOR.create(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));

        TABBEDPANE.setMnemonicAt(1, KeyEvent.VK_2);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.title"),
                null,
                TAB_LOG_MODLOADERINSTALLER.create(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.tip"));

        TABBEDPANE.setMnemonicAt(2, KeyEvent.VK_3);

        TABBEDPANE.addTab(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.addonshandlerlog.title"),
                null,
                TAB_LOG_ADDONSHANDLER.create(),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.tabbedpane.addonshandlerlog.tip"));

        TABBEDPANE.setMnemonicAt(3, KeyEvent.VK_4);

        add(TABBEDPANE);

        TABBEDPANE.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        MENUBAR = new MenuBar(
                LOCALIZATIONMANAGER,
                LIGHTTHEME,
                DARKTHEME,
                FRAME_SERVERPACKCREATOR,
                LAF_LIGHT,
                LAF_DARK,
                TAB_CREATESERVERPACK,
                TABBEDPANE,
                APPLICATIONPROPERTIES
        );

        FRAME_SERVERPACKCREATOR.setJMenuBar(MENUBAR.createMenuBar());
    }

    /**
     * Shows the GUI from the EDT by using SwingUtilities, and it's invokeLater method by calling {@link #createAndShowGUI()}.
     * Sets the font to bold, which may be overridden by the LookAndFeel which gets automatically determined and depends
     * on the OS ServerPackCreator is run on.
     * @author Griefed
     */
    public void mainGUI() {
        SwingUtilities.invokeLater(() -> {

            try {

                if (APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.gui.darkmode").equals("true")) {

                    UIManager.setLookAndFeel(LAF_DARK);
                    MaterialLookAndFeel.changeTheme(DARKTHEME);

                } else {

                    UIManager.setLookAndFeel(LAF_LIGHT);
                    MaterialLookAndFeel.changeTheme(LIGHTTHEME);

                }

            } catch (UnsupportedLookAndFeelException ex) {
                LOG.error("Error: There was an error setting the look and feel.", ex);
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

        FRAME_SERVERPACKCREATOR.add(TABBEDPANE, BorderLayout.CENTER);

        FRAME_SERVERPACKCREATOR.setSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setPreferredSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setMinimumSize(DIMENSION_WINDOW);
        FRAME_SERVERPACKCREATOR.setResizable(true);

        FRAME_SERVERPACKCREATOR.pack();

        /*
         * I know this looks stupid. Why update the tree if it isn't even visible yet?
         * Because otherwise, when switching from light to dark-theme, the inset for tabs of the tabbed pane suddenly
         * changes, which looks ugly. Calling this does the same, but before the GUI is visible. Dirty hack? Maybe.
         * Does it work? Yeah.
         */
        SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);

        TABBEDPANE.setOpaque(true);

        FRAME_SERVERPACKCREATOR.setVisible(true);

        displayUpdateDialog();
    }

    /**
     * If an update is available for ServerPackCreator, display a dialog asking the user whether
     * @author Griefed
     */
    private void displayUpdateDialog() {

        if (!update.equals(LOCALIZATIONMANAGER.getLocalizedString("updates.log.info.none"))) {
            String version = update.split(";")[0];
            String url = update.split(";")[1];
            String textContent = String.format(LOCALIZATIONMANAGER.getLocalizedString("update.dialog.new"), url);

            StyledDocument styledDocument = new DefaultStyledDocument();
            SimpleAttributeSet simpleAttributeSet = new SimpleAttributeSet();
            MaterialTextPaneUI materialTextPaneUI = new MaterialTextPaneUI();
            JTextPane jTextPane = new JTextPane(styledDocument);
            StyleConstants.setBold(simpleAttributeSet, true);
            StyleConstants.setFontSize(simpleAttributeSet, 14);
            jTextPane.setCharacterAttributes(simpleAttributeSet, true);
            StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT);
            styledDocument.setParagraphAttributes(0, styledDocument.getLength(), simpleAttributeSet, false);
            jTextPane.addHierarchyListener(e1 -> {
                Window window = SwingUtilities.getWindowAncestor(jTextPane);
                if (window instanceof Dialog) {
                    Dialog dialog = (Dialog) window;
                    if (!dialog.isResizable()) {
                        dialog.setResizable(true);
                    }
                }
            });
            jTextPane.setOpaque(false);
            jTextPane.setEditable(false);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String[] options = new String[3];

            options[0] = LOCALIZATIONMANAGER.getLocalizedString("update.dialog.yes");
            options[1] = LOCALIZATIONMANAGER.getLocalizedString("update.dialog.no");
            options[2] = LOCALIZATIONMANAGER.getLocalizedString("update.dialog.clipboard");


            try {
                styledDocument.insertString(0, textContent, simpleAttributeSet);
            } catch (BadLocationException ex) {
                LOG.error("Error inserting text into aboutDocument.", ex);
            }

            materialTextPaneUI.installUI(jTextPane);

            switch (JOptionPane.showOptionDialog(
                    FRAME_SERVERPACKCREATOR,
                    jTextPane,
                    LOCALIZATIONMANAGER.getLocalizedString("update.dialog.available"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    UIManager.getIcon("OptionPane.informationIcon"),
                    options,
                    options[0])) {

                case 0:

                    try {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI.create(url));
                        }
                    } catch (IOException ex) {
                        LOG.error("Error opening browser.", ex);
                    }
                    break;

                case 1:

                    clipboard.setContents(new StringSelection(url), null);
                    break;

                default:
                    break;
            }
        }
    }
}