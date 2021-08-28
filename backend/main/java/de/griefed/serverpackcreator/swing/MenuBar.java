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

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import mdlaf.MaterialLookAndFeel;
import mdlaf.components.textfield.MaterialTextFieldUI;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.system.ApplicationHome;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class MenuBar extends Component {

    private static final Logger LOG = LogManager.getLogger(MenuBar.class);

    private final ApplicationHome applicationHome = new ApplicationHome(de.griefed.serverpackcreator.Main.class);

    private final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    private final LocalizationManager LOCALIZATIONMANAGER;

    private final LightTheme LIGHTTHEME;
    private final DarkTheme DARKTHEME;

    private final JFrame FRAME_SERVERPACKCREATOR;

    private final TabCreateServerPack TAB_CREATESERVERPACK;

    private final JTabbedPane TABBEDPANE;

    private final WindowEvent CLOSEEVENT;

    private final MaterialLookAndFeel LAF_DARK;
    private final MaterialLookAndFeel LAF_LIGHT;

    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);
    private final Dimension JAVAARGSDIMENSION = new Dimension(750,25);
    private final Dimension ABOUTDIMENSION = new Dimension(925,520);
    private final Dimension FILETOOLARGEDIMENSION = new Dimension(200,10);

    private final ImageIcon HELPICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/help.png")));
    private final ImageIcon ICON_HASTEBIN = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/hastebin.png")));

    private final JMenuBar MENUBAR = new JMenuBar();

    private final File PROPERTIESFILE = new File("serverpackcreator.properties");

    private final String HELPWINDOWTEXT;
    private final String ABOUTWINDOWTEXT;
    private final String FILETOOLARGETEXT;
    private final String FILETOOLARGETITLE;

    private final String[] JAVAARGSOPTIONS = new String[4];
    private final String[] JAVAARGSSELECTIONS = new String[2];
    private final String[] HASTEOPTIONS = new String[3];

    private final JTextField JAVAARGS = new JTextField();

    private Properties serverpackcreatorproperties;

    private boolean isDarkTheme;

    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenu aboutMenu;

    private JMenuItem file_LoadConfigMenuItem;
    private JMenuItem file_SaveConfigMenuItem;
    private JMenuItem file_SaveAsConfigMenuItem;
    private JMenuItem file_UploadConfigurationToHasteBin;
    private JMenuItem file_UploadServerPackCreatorLogToHasteBin;
    private JMenuItem file_ExitConfigMenuItem;

    private JMenuItem edit_SwitchTheme;
    private JMenuItem edit_ChangeJavaArgs;
    private JMenuItem edit_OpenInEditorServerProperties;
    private JMenuItem edit_OpenInEditorServerIcon;

    private JMenuItem view_OpenServerPackCreatorDirectoryMenuItem;
    private JMenuItem view_OpenServerPacksDirectoryMenuItem;
    private JMenuItem view_OpenServerFilesDirectoryMenuItem;
    private JMenuItem view_OpenAddonsDirectoryMenuItem;
    private JMenuItem view_ExampleAddonRepositoryMenuItem;

    private JMenuItem about_OpenAboutWindowMenuItem;
    private JMenuItem about_OpenGitHubPageMenuItem;
    private JMenuItem about_OpenGitHubIssuesPageMenuItem;
    private JMenuItem about_OpenDonationsPageMenuItem;
    private JMenuItem about_OpenReleasesPageMenuItem;
    private JMenuItem about_OpenDiscordLinkMenuItem;

    private JMenuItem help_OpenHelpWindowMenuItem;

    private JFileChooser configChooser;

    private StyledDocument helpWindowDocument = new DefaultStyledDocument();
    private StyledDocument aboutWindowDocument = new DefaultStyledDocument();
    private StyledDocument configWindowDocument = new DefaultStyledDocument();
    private StyledDocument spcLogWindowDocument = new DefaultStyledDocument();
    private StyledDocument fileTooLargeWindowDocument = new DefaultStyledDocument();

    private SimpleAttributeSet aboutAttributeSet = new SimpleAttributeSet();
    private SimpleAttributeSet helpAttributeSet = new SimpleAttributeSet();
    private SimpleAttributeSet configAttributeSet = new SimpleAttributeSet();
    private SimpleAttributeSet spcLogAttributeSet = new SimpleAttributeSet();
    private SimpleAttributeSet fileTooLargeAttributeSet = new SimpleAttributeSet();

    private JTextPane helpWindowTextPane = new JTextPane(helpWindowDocument);
    private JTextPane aboutWindowTextPane = new JTextPane(aboutWindowDocument);
    private JTextPane configWindowTextPane = new JTextPane(configWindowDocument);
    private JTextPane spcLogWindowTextPane = new JTextPane(spcLogWindowDocument);
    private JTextPane fileTooLargeWindowTextPane = new JTextPane();

    private MaterialTextPaneUI materialTextPaneUI = new MaterialTextPaneUI();

    public MenuBar(LocalizationManager injectedLocalizationManager, LightTheme injectedLightTheme, DarkTheme injectedDarkTheme,
                   JFrame injectedJFrame, MaterialLookAndFeel injectedLAF_Light, MaterialLookAndFeel injectedLAF_Dark,
                   TabCreateServerPack injectedTabCreateServerPack, JTabbedPane injectedTabbedPane, Properties injectedServerPackCreatorProperties) {

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager();
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        this.LIGHTTHEME = injectedLightTheme;
        this.DARKTHEME = injectedDarkTheme;
        this.FRAME_SERVERPACKCREATOR = injectedJFrame;
        this.LAF_LIGHT = injectedLAF_Light;
        this.LAF_DARK = injectedLAF_Dark;
        this.TAB_CREATESERVERPACK = injectedTabCreateServerPack;
        this.TABBEDPANE = injectedTabbedPane;

        this.serverpackcreatorproperties = injectedServerPackCreatorProperties;

        try {
            isDarkTheme = Boolean.parseBoolean(serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.gui.darkmode"));
        } catch (NullPointerException ex) {
            LOG.error("No setting for darkmode found in properties-file. Using true.");
            isDarkTheme = true;
            serverpackcreatorproperties.put("de.griefed.serverpackcreator.gui.darkmode", "true");
        }

        CLOSEEVENT = new WindowEvent(FRAME_SERVERPACKCREATOR, WindowEvent.WINDOW_CLOSING);

        ABOUTWINDOWTEXT = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.text");
        aboutWindowTextPane.setEditable(false);
        aboutWindowTextPane.setOpaque(false);
        aboutWindowTextPane.setMinimumSize(ABOUTDIMENSION);
        aboutWindowTextPane.setPreferredSize(ABOUTDIMENSION);
        aboutWindowTextPane.setMaximumSize(ABOUTDIMENSION);
        StyleConstants.setBold(aboutAttributeSet, true);
        StyleConstants.setFontSize(aboutAttributeSet, 14);
        aboutWindowTextPane.setCharacterAttributes(aboutAttributeSet, true);
        StyleConstants.setAlignment(aboutAttributeSet, StyleConstants.ALIGN_CENTER);
        aboutWindowDocument.setParagraphAttributes(0, aboutWindowDocument.getLength(), aboutAttributeSet, false);
        try {
            aboutWindowDocument.insertString(0, ABOUTWINDOWTEXT, aboutAttributeSet);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        aboutWindowTextPane.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(aboutWindowTextPane);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        HELPWINDOWTEXT = String.format(
                "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.modpackdir"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.clientsidemods"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.directories"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.pathtojava"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.minecraftversion"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.modloader"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.modloaderversion"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.installserver"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.copypropertires"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.copyscripts"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.copyicon"),
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.createzip")
        );
        helpWindowTextPane.setEditable(false);
        helpWindowTextPane.setOpaque(false);
        StyleConstants.setBold(helpAttributeSet, true);
        StyleConstants.setFontSize(helpAttributeSet, 14);
        helpWindowTextPane.setCharacterAttributes(helpAttributeSet, true);
        StyleConstants.setAlignment(helpAttributeSet, StyleConstants.ALIGN_LEFT);
        helpWindowDocument.setParagraphAttributes(0, helpWindowDocument.getLength(), helpAttributeSet, false);
        try {
            helpWindowDocument.insertString(0, HELPWINDOWTEXT, helpAttributeSet);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        helpWindowTextPane.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(helpWindowTextPane);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        HASTEOPTIONS[0] = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.yes");
        HASTEOPTIONS[1] = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.clipboard");
        HASTEOPTIONS[2] = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog.no");

        configWindowTextPane.setOpaque(false);
        configWindowTextPane.setEditable(false);
        StyleConstants.setBold(configAttributeSet, true);
        StyleConstants.setFontSize(configAttributeSet, 14);
        configWindowTextPane.setCharacterAttributes(configAttributeSet, true);
        StyleConstants.setAlignment(configAttributeSet, StyleConstants.ALIGN_LEFT);
        configWindowDocument.setParagraphAttributes(0, configWindowDocument.getLength(), configAttributeSet, false);
        configWindowTextPane.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(configWindowTextPane);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        spcLogWindowTextPane.setOpaque(false);
        spcLogWindowTextPane.setEditable(false);
        StyleConstants.setBold(spcLogAttributeSet, true);
        StyleConstants.setFontSize(spcLogAttributeSet, 14);
        spcLogWindowTextPane.setCharacterAttributes(spcLogAttributeSet, true);
        StyleConstants.setAlignment(spcLogAttributeSet, StyleConstants.ALIGN_LEFT);
        spcLogWindowDocument.setParagraphAttributes(0, spcLogWindowDocument.getLength(), spcLogAttributeSet, false);
        spcLogWindowTextPane.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(spcLogWindowTextPane);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        // TODO: Replace with lang key.
        FILETOOLARGETEXT = "Size exceeds 10MB. Must be smaller\nthan 10MB for HasteBin upload.  ";
        // TODO: Replace with lang key.
        FILETOOLARGETITLE = "File too large!";
        fileTooLargeWindowTextPane.setOpaque(false);
        fileTooLargeWindowTextPane.setEditable(false);
        fileTooLargeWindowTextPane.setMinimumSize(FILETOOLARGEDIMENSION);
        fileTooLargeWindowTextPane.setPreferredSize(FILETOOLARGEDIMENSION);
        fileTooLargeWindowTextPane.setMaximumSize(FILETOOLARGEDIMENSION);
        StyleConstants.setBold(fileTooLargeAttributeSet, true);
        StyleConstants.setFontSize(fileTooLargeAttributeSet, 14);
        fileTooLargeWindowTextPane.setCharacterAttributes(fileTooLargeAttributeSet, true);
        StyleConstants.setAlignment(fileTooLargeAttributeSet, StyleConstants.ALIGN_LEFT);
        fileTooLargeWindowDocument.setParagraphAttributes(0, fileTooLargeWindowDocument.getLength(), fileTooLargeAttributeSet, false);
        try {
            fileTooLargeWindowDocument.insertString(0, FILETOOLARGETEXT, fileTooLargeAttributeSet);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        fileTooLargeWindowTextPane.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(fileTooLargeWindowTextPane);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        // TODO: Replace with lang key
        JAVAARGSOPTIONS[0] = "OK";
        // TODO: Replace with lang key
        JAVAARGSOPTIONS[1] = "Use Aikars flags";
        // TODO: Replace with lang key
        JAVAARGSOPTIONS[2] = "Empty";
        // TODO: Replace with lang key
        JAVAARGSOPTIONS[3] = "Cancel";

        JAVAARGSSELECTIONS[0] = "empty";
        JAVAARGSSELECTIONS[1] = "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
                "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 " +
                "-XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 " +
                "-XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -XX:G1NewSizePercent=30 " +
                "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 " +
                "-XX:InitiatingHeapOccupancyPercent=15 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true";

        JAVAARGS.setMinimumSize(JAVAARGSDIMENSION);
        JAVAARGS.setMaximumSize(JAVAARGSDIMENSION);
        JAVAARGS.setPreferredSize(JAVAARGSDIMENSION);
    }

    /**
     * Getter for the serverpackcreator.properties file.
     * @author Griefed
     * @return File. Returns the serverpackcreator.properties-file.
     */
    File getPropertiesFile() {
        return PROPERTIESFILE;
    }

    /**
     * Create the menubar, add all menus, add all menuitems and add actionlisteners for our menuitems.
     * @author Griefed
     * @return JMenuBar. Returns the menubar containing all elements we need to control various aspects of our app.
     */
    public JMenuBar createMenuBar() {

        // create menus
        // TODO: Replace with lang key
        fileMenu = new JMenu("File");
        // TODO: Replace with lang key
        editMenu = new JMenu("Edit");
        // TODO: Replace with lang key
        viewMenu = new JMenu("View");
        // TODO: Replace with lang key
        aboutMenu = new JMenu("About");
        // TODO: Replace with lang key
        help_OpenHelpWindowMenuItem = new JMenuItem("Help");

        // create menu items
        // TODO: Replace with lang key
        file_LoadConfigMenuItem = new JMenuItem("Load Configuration");
        // TODO: Replace with lang key
        file_SaveConfigMenuItem = new JMenuItem("Save Configuration");
        // TODO: Replace with lang key
        file_SaveAsConfigMenuItem = new JMenuItem("Save Configuration As...");
        // TODO: Replace with lang key
        file_UploadConfigurationToHasteBin = new JMenuItem("Upload Configuration to HasteBin");
        // TODO: Replace with lang key
        file_UploadServerPackCreatorLogToHasteBin = new JMenuItem("Upload ServerPackCreator Log to HasteBin");
        // TODO: Replace with lang key
        file_ExitConfigMenuItem = new JMenuItem("Exit");

        // TODO: Replace with lang key
        edit_SwitchTheme = new JMenuItem("Toggle light/dark-mode");
        // TODO: Replace with lang key
        edit_ChangeJavaArgs = new JMenuItem("Edit Start-Scripts Java Args");
        // TODO: Replace with lang key
        edit_OpenInEditorServerProperties = new JMenuItem("Open server.properties in Editor");
        // TODO: Replace with lang key
        edit_OpenInEditorServerIcon = new JMenuItem("Open server-icon.png in Editor");

        // TODO: Replace with lang key
        view_OpenAddonsDirectoryMenuItem = new JMenuItem("Open addons-directory");
        // TODO: Replace with lang key
        view_ExampleAddonRepositoryMenuItem = new JMenuItem("Visit example-addon repository");
        // TODO: Replace with lang key
        view_OpenServerPackCreatorDirectoryMenuItem = new JMenuItem("Open installation-directory");
        // TODO: Replace with lang key
        view_OpenServerPacksDirectoryMenuItem = new JMenuItem("Open server packs directory");
        // TODO: Replace with lang key
        view_OpenServerFilesDirectoryMenuItem = new JMenuItem("Open server-files directory");

        // TODO: Replace with lang key
        about_OpenAboutWindowMenuItem = new JMenuItem("About");
        // TODO: Replace with lang key
        about_OpenGitHubPageMenuItem = new JMenuItem("View Repository on GitHub");
        // TODO: Replace with lang key
        about_OpenGitHubIssuesPageMenuItem = new JMenuItem("View Issues on GitHub");
        // TODO: Replace with lang key
        about_OpenReleasesPageMenuItem = new JMenuItem("View Releases on GitHub");
        // TODO: Replace with lang key
        about_OpenDiscordLinkMenuItem = new JMenuItem("Join my Discord server!");
        // TODO: Replace with lang key
        about_OpenDonationsPageMenuItem = new JMenuItem("Support me!");

        // create action listeners for items
        file_LoadConfigMenuItem.addActionListener(this::actionEventLoadConfigurationFromFileMenuItem);
        file_SaveConfigMenuItem.addActionListener(this::actionEventSaveConfigToFileMenuItem);
        file_SaveAsConfigMenuItem.addActionListener(this::actionEventSaveAsConfigToFileMenuItem);
        file_UploadConfigurationToHasteBin.addActionListener(this::actionEventUploadConfigurationToHasteBinMenuItem);
        file_UploadServerPackCreatorLogToHasteBin.addActionListener(this::actionEventUploadServerPackCreatorLogToHasteBinMenuItem);
        file_ExitConfigMenuItem.addActionListener(this::actionEventExitMenuItem);

        edit_SwitchTheme.addActionListener(this::actionEventSwitchThemeMenuItem);
        edit_ChangeJavaArgs.addActionListener(this::actionEventChangeJavaArgsMenuItem);
        edit_OpenInEditorServerProperties.addActionListener(this::actionEventOpenInEditorServerProperties);
        edit_OpenInEditorServerIcon.addActionListener(this::actionEventOpenServerIcon);

        view_OpenServerPackCreatorDirectoryMenuItem.addActionListener(this::actionEventOpenSPCDirectoryMenuItem);
        view_OpenServerPacksDirectoryMenuItem.addActionListener(this::actionEventOpenServerPacksDirectoryMenuItem);
        view_OpenServerFilesDirectoryMenuItem.addActionListener(this::actionEventOpenServerFilesDirectoryMenuItem);
        view_OpenAddonsDirectoryMenuItem.addActionListener(this::actionEventOpenAddonsDirectoryMenuItem);
        view_ExampleAddonRepositoryMenuItem.addActionListener(this::actionEventViewExampleAddonMenuItem);

        about_OpenAboutWindowMenuItem.addActionListener(this::actionEventOpenAboutSPCMenuItem);
        about_OpenGitHubPageMenuItem.addActionListener(this::actionEventOpenGitHubMenuItem);
        about_OpenGitHubIssuesPageMenuItem.addActionListener(this::actionEventOpenIssuesMenuItem);
        about_OpenReleasesPageMenuItem.addActionListener(this::actionEventOpenReleaseMenuItem);
        about_OpenDiscordLinkMenuItem.addActionListener(this::actionEventOpenDiscordLinkMenuItem);
        about_OpenDonationsPageMenuItem.addActionListener(this::actionEventOpenDonateMenuItem);

        help_OpenHelpWindowMenuItem.addActionListener(this::actionEventOpenHelpMenuItem);

        // add items to menus
        fileMenu.add(file_LoadConfigMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_SaveConfigMenuItem);
        fileMenu.add(file_SaveAsConfigMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_UploadConfigurationToHasteBin);
        fileMenu.add(file_UploadServerPackCreatorLogToHasteBin);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_ExitConfigMenuItem);

        editMenu.add(edit_ChangeJavaArgs);
        editMenu.add(new JSeparator());
        editMenu.add(edit_OpenInEditorServerProperties);
        editMenu.add(edit_OpenInEditorServerIcon);
        editMenu.add(new JSeparator());
        editMenu.add(edit_SwitchTheme);

        viewMenu.add(view_OpenServerPackCreatorDirectoryMenuItem);
        viewMenu.add(view_OpenServerPacksDirectoryMenuItem);
        viewMenu.add(view_OpenServerFilesDirectoryMenuItem);
        viewMenu.add(view_OpenAddonsDirectoryMenuItem);
        viewMenu.add(new JSeparator());
        viewMenu.add(view_ExampleAddonRepositoryMenuItem);

        aboutMenu.add(about_OpenAboutWindowMenuItem);
        aboutMenu.add(new JSeparator());
        aboutMenu.add(about_OpenGitHubPageMenuItem);
        aboutMenu.add(about_OpenGitHubIssuesPageMenuItem);
        aboutMenu.add(about_OpenReleasesPageMenuItem);
        aboutMenu.add(new JSeparator());
        aboutMenu.add(about_OpenDiscordLinkMenuItem);
        aboutMenu.add(new JSeparator());
        aboutMenu.add(about_OpenDonationsPageMenuItem);

        // add menus
        MENUBAR.add(fileMenu);
        MENUBAR.add(editMenu);
        MENUBAR.add(viewMenu);
        MENUBAR.add(aboutMenu);
        MENUBAR.add(help_OpenHelpWindowMenuItem);

        return MENUBAR;
    }

    /**
     * Upon button-press, open the Discord invite-link to Griefed's Discord server in the users default browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenDiscordLinkMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Join Discord.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://discord.griefed.de"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser.", ex);
        }
    }

    /**
     * Upon button-press, open ServerPackCreators issue-page on GitHub in the users default browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenIssuesMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Open Issues page on GitHub.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser.", ex);
        }
    }

    /**
     * Upon button-press, uploads the serverpackcreator.log-file to HasteBin and display a dialog asking the user whether
     * they want to open the URL in their default browser or copy the link to their clipboard. If the filesize exceeds 10MB,
     * a warning is displayed, telling the user about filesize limitations of HasteBin.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventUploadServerPackCreatorLogToHasteBinMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Upload ServerPackCreator Log to HasteBin.");

        if (checkFileSize(new File("logs/serverpackcreator.log"))) {
            String urltoHasteBin = createHasteBinFromFile(new File("logs/serverpackcreator.log"));
            String textContent = String.format("URL: %s", urltoHasteBin);

            try {
                spcLogWindowDocument.insertString(0, textContent, spcLogAttributeSet);
            } catch (BadLocationException ex) {
                LOG.error("Error inserting text into aboutDocument.", ex);
            }

            materialTextPaneUI.installUI(spcLogWindowTextPane);

            switch (JOptionPane.showOptionDialog(
                    FRAME_SERVERPACKCREATOR,
                    spcLogWindowTextPane,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    ICON_HASTEBIN,
                    HASTEOPTIONS,
                    HASTEOPTIONS[0])) {

                case 0:

                    try {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI.create(urltoHasteBin));
                        }
                    } catch (IOException ex) {
                        LOG.error("Error opening browser.", ex);
                    }
                    break;

                case 1:

                    CLIPBOARD.setContents(new StringSelection(urltoHasteBin), null);
                    break;

                default:
                    break;
            }
        } else {
            fileTooLargeDialog();
        }
    }

    /**
     * Upon button-press, uploads the serverpackcreator.conf-file to HasteBin and display a dialog asking the user whether
     * they want to open the URL in their default browser or copy the link to their clipboard.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventUploadConfigurationToHasteBinMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Upload Configuration to HasteBin.");

        if (checkFileSize(new File("serverpackcreator.conf"))) {

            String urltoHasteBin = createHasteBinFromFile(new File("serverpackcreator.conf"));
            String textContent = String.format("URL: %s", urltoHasteBin);

            try {
                configWindowDocument.insertString(0, textContent, configAttributeSet);
            } catch (BadLocationException ex) {
                LOG.error("Error inserting text into aboutDocument.", ex);
            }

            materialTextPaneUI.installUI(configWindowTextPane);

            switch (JOptionPane.showOptionDialog(
                    FRAME_SERVERPACKCREATOR,
                    configWindowTextPane,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    ICON_HASTEBIN,
                    HASTEOPTIONS,
                    HASTEOPTIONS[0])) {

                case 0:

                    try {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI.create(urltoHasteBin));
                        }
                    } catch (IOException ex) {
                        LOG.error("Error opening browser.", ex);
                    }
                    break;

                case 1:

                    CLIPBOARD.setContents(new StringSelection(urltoHasteBin), null);
                    break;

                default:
                    break;
            }
        } else {
            fileTooLargeDialog();
        }
    }

    /**
     * Opens a dialog informing the user that a file exceeds 10MB in size.
     * @author Griefed
     */
    private void fileTooLargeDialog() {
        materialTextPaneUI.installUI(fileTooLargeWindowTextPane);
        JOptionPane.showConfirmDialog(
                FRAME_SERVERPACKCREATOR,
                fileTooLargeWindowTextPane,
                FILETOOLARGETITLE,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                ICON_HASTEBIN
        );
    }

    /**
     * Upon button-press, open the server.properties-file, in the server-files directory, in the users default text-editor.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenInEditorServerProperties(ActionEvent actionEvent) {
        LOG.debug("Clicked Open server.properties in Editor.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                Desktop.getDesktop().open(
                        new File(
                                applicationHome.getSource().toString().replace("\\","/")
                                        .replace(applicationHome.getSource().toString()
                                                .substring(
                                                        applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                        .replace("\\","/")
                                        + "/server_files/server.properties")
                );
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for ServerPackCreator GitHub repository.", ex);
        }
    }

    /**
     * Upon button-press, open the server-icon.png-file, in the server-files directory, in the users default picture-viewer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenServerIcon(ActionEvent actionEvent) {
        LOG.debug("Clicked Open server-icon.png in Editor.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(
                        new File(
                                applicationHome.getSource().toString().replace("\\","/")
                                        .replace(applicationHome.getSource().toString()
                                                .substring(
                                                        applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                        .replace("\\","/")
                                        + "/server_files/server-icon.png")
                );
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for ServerPackCreator GitHub repository.", ex);
        }
    }

    /**
     * Upon button-press, open a dialog which allows the user to specify JVM flags/Java args for the start-scripts which
     * can be created by ServerPackCreator. Provides options to use Aikars flags, clear the args, confirm the current
     * configuration and save it as well as simply canceling the dialog.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventChangeJavaArgsMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Edit Start-Scripts Java Args.");

        if (TAB_CREATESERVERPACK.getJavaArgs().equalsIgnoreCase("empty")) {
            JAVAARGS.setText("");
        } else {
            JAVAARGS.setText(TAB_CREATESERVERPACK.getJavaArgs());
        }

        new MaterialTextFieldUI().installUI(JAVAARGS);

        switch (JOptionPane.showOptionDialog(
                FRAME_SERVERPACKCREATOR,
                JAVAARGS,
                "Java Arguments",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                JAVAARGSOPTIONS,
                JAVAARGSOPTIONS[3]
                )
        ) {
            case 0:

                if (JAVAARGS.getText().equals("")) {
                    TAB_CREATESERVERPACK.setJavaArgs("empty");
                } else {
                    TAB_CREATESERVERPACK.setJavaArgs(JAVAARGS.getText());
                }
                break;

            case 1:

                TAB_CREATESERVERPACK.setJavaArgs(JAVAARGSSELECTIONS[1]);
                break;

            case 2:

                TAB_CREATESERVERPACK.setJavaArgs("empty");
                break;

            default:

        }
        LOG.debug("Java args set to: " + TAB_CREATESERVERPACK.getJavaArgs());
    }

    /**
     * Upon button-press, close ServerPackCreator gracefully.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventExitMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Exit.");
        FRAME_SERVERPACKCREATOR.dispatchEvent(CLOSEEVENT);
    }

    /**
     * Upon button-press, open a Filechooser dialog which allows the user to specify a file in which the current configuration
     * in the GUI will be saved to.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventSaveAsConfigToFileMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Save As...");

        configChooser = new JFileChooser();
        configChooser.setCurrentDirectory(new File("."));
        configChooser.setDialogTitle("Store current configuration");
        configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        configChooser.setFileFilter(new FileNameExtensionFilter(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonloadconfig.filter"),"conf"));
        configChooser.setAcceptAllFileFilterUsed(false);
        configChooser.setMultiSelectionEnabled(false);
        configChooser.setPreferredSize(CHOOSERDIMENSION);

        if (configChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

            try {
                TAB_CREATESERVERPACK.saveConfig(new File(configChooser.getSelectedFile().getCanonicalPath()), true);

                LOG.debug("Saved configuration to: " + configChooser.getSelectedFile().getCanonicalPath());

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"), ex);
            }
        }
    }

    /**
     * Upon button-press, save the current configuration in the GUI to the serverpackcreator.conf-file in ServerPackCreators
     * base directory.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventSaveConfigToFileMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Save.");
        TAB_CREATESERVERPACK.saveConfig(new File("./serverpackcreator.conf"), false);
    }

    /**
     * Upon button-press, change the current theme to either light or dark-mode, depending on which theme is currently active.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventSwitchThemeMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Toggle light/dark-mode.");

        if (!isDarkTheme) {
            try {
                UIManager.setLookAndFeel(LAF_DARK);
                MaterialLookAndFeel.changeTheme(DARKTHEME);

                SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);

                TABBEDPANE.setOpaque(true);

                isDarkTheme = true;

                try (OutputStream outputStream = new FileOutputStream(getPropertiesFile())) {

                    serverpackcreatorproperties.setProperty("de.griefed.serverpackcreator.gui.darkmode", String.valueOf(true));
                    serverpackcreatorproperties.store(outputStream, null);

                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } catch (UnsupportedLookAndFeelException ex) {
                LOG.error("Couldn't change theme.", ex);
            }
        } else {
            try {
                UIManager.setLookAndFeel(LAF_LIGHT);
                MaterialLookAndFeel.changeTheme(LIGHTTHEME);

                SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);

                TABBEDPANE.setOpaque(true);

                isDarkTheme = false;

                try (OutputStream outputStream = new FileOutputStream(getPropertiesFile())) {

                    serverpackcreatorproperties.setProperty("de.griefed.serverpackcreator.gui.darkmode", String.valueOf(false));
                    serverpackcreatorproperties.store(outputStream, null);

                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } catch (UnsupportedLookAndFeelException ex) {
                LOG.error("Couldn't change theme.", ex);
            }
        }
    }

    /**
     * Upon button-press, open a file-selector to load a serverpackcreator.conf-file into ServerPackCreator.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventLoadConfigurationFromFileMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked load configuration from file.");

        configChooser = new JFileChooser();
        configChooser.setCurrentDirectory(new File("."));
        configChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonloadconfig.title"));
        configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        configChooser.setFileFilter(new FileNameExtensionFilter(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonloadconfig.filter"),"conf"));
        configChooser.setAcceptAllFileFilterUsed(false);
        configChooser.setMultiSelectionEnabled(false);
        configChooser.setPreferredSize(CHOOSERDIMENSION);

        if (configChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

            try {

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.info(String.format(
                        LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"),
                        configChooser.getSelectedFile().getCanonicalPath()
                ));

                TAB_CREATESERVERPACK.loadConfig(new File(configChooser.getSelectedFile().getCanonicalPath()));

            } catch (IOException ex) {
                LOG.error("Error loading configuration from selected file.", ex);
            }

            LOG.debug("Configuration successfully loaded.");

        }
    }

    /**
     * Upon button-press, open the folder containing installed addons for ServerPackCreator in the users file-explorer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenAddonsDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open addons directory.");

        try {
            Desktop.getDesktop().open(
                    new File(
                            applicationHome.getSource().toString().replace("\\","/")
                                    .replace(applicationHome.getSource().toString()
                                            .substring(
                                                    applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                    .replace("\\","/")
                                    + "/addons")
            );
        } catch (IOException ex) {
            LOG.error("Error opening file explorer for addons-directory.", ex);
        }
    }

    /**
     * Upon button-press, open the example addons repository-page on GitHub in the users default browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventViewExampleAddonMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked view example addon");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreatorExampleAddon"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for example-addon repository.", ex);
        }
    }

    /**
     * Upon button-press, open the base directory of ServerPackCreator in the users file-explorer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenSPCDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open installation directory.");

        try {
            Desktop.getDesktop().open(
                    new File(
                            applicationHome.getSource().toString().replace("\\","/")
                                    .replace(applicationHome.getSource().toString()
                                            .substring(
                                                    applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                    .replace("\\","/")
                                    + "/")
            );
        } catch (IOException ex) {
            LOG.error("Error opening file explorer for ServerPackCreator base-directory.", ex);
        }
    }

    /**
     * Upon button-press, open the folder containing generated server packs in the users file-explorer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenServerPacksDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open server packs directory.");

        try {
            Desktop.getDesktop().open(
                    new File(
                            applicationHome.getSource().toString().replace("\\","/")
                                    .replace(applicationHome.getSource().toString()
                                            .substring(
                                                    applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                    .replace("\\","/")
                                    + "/server-packs")
            );
        } catch (IOException ex) {
            LOG.error("Error opening file explorer for server-packs.", ex);
        }
    }

    /**
     * Upon button-press, open the folder containing the server-icon.png and server.properties files in the users file-explorer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenServerFilesDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open server files directory.");

        try {
            Desktop.getDesktop().open(
                    new File(
                            applicationHome.getSource().toString().replace("\\","/")
                                    .replace(applicationHome.getSource().toString()
                                            .substring(
                                                    applicationHome.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                    .replace("\\","/")
                                    + "/server_files")
            );
        } catch (IOException ex) {
            LOG.error("Error opening file explorer for server_files.", ex);
        }
    }

    /**
     * Upon button-press, open an About-window containing information about ServerPackCreator.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenAboutSPCMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open about window.");

        materialTextPaneUI.installUI(aboutWindowTextPane);

        JOptionPane.showMessageDialog(
                FRAME_SERVERPACKCREATOR,
                aboutWindowTextPane,
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.title"),
                JOptionPane.INFORMATION_MESSAGE,
                HELPICON
        );
    }

    /**
     * Upon button-press, open an info-window containing information/help about how to configure ServerPackCreator.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenHelpMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open help window.");

        materialTextPaneUI.installUI(helpWindowTextPane);

        JOptionPane.showMessageDialog(
                FRAME_SERVERPACKCREATOR,
                helpWindowTextPane,
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.help.title"),
                JOptionPane.INFORMATION_MESSAGE,
                HELPICON
        );
    }

    /**
     * Upon button-press, open the ServerPackCreator repository GitHub page in the users default-browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenGitHubMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open GitHub repository link.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for ServerPackCreator GitHub repository.", ex);
        }
    }

    /**
     * Upon button-press, open the GitHub Sponsors page in the users default-browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenDonateMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open donations link.");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/sponsors/Griefed"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for donations page.", ex);
        }
    }

    /**
     * Upon button-press, open the GitHub releases page in the users default-browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenReleaseMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open releases link");

        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator/releases"));
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser for releases page.", ex);
        }
    }

    /**
     * Checks the filesize of the given file whether it is smaller or bigger than 10MB.
     * @author Griefed
     * @param fileToCheck The file or directory to check.
     * @return Boolean. True if the file is smaller, false if the file is bigger than 10MB.
     */
    private boolean checkFileSize(File fileToCheck) {
        long fileSize = FileUtils.sizeOf(fileToCheck);

        if (fileSize < 10000000) {
            LOG.debug("Smaller. " + fileSize + " byte.");
            return true;
        } else {
            LOG.debug("Bigger. " + fileSize + " byte.");
            return false;
        }
    }

    /**
     * Create a HasteBin post from a given text file. The text file provided is read into a string and then passed onto
     * <a href="https://haste.zneix.eu">Haste zneix</a> which creates a HasteBin post out of the passed String and
     * returns the URL to the newly created post.<br>
     * Created with the help of <a href="https://github.com/kaimu-kun/hastebin.java">kaimu-kun's hastebin.java (MIT License)</a>
     * and edited to use HasteBin fork <a href="https://github.com/zneix/haste-server">zneix/haste-server</a>. My fork
     * of kaimu-kun's hastebin.java is available at <a href="https://github.com/Griefed/hastebin.java">Griefed/hastebin.java</a>.
     * @author <a href="https://github.com/kaimu-kun">kaimu-kun/hastebin.java</a>
     * @author Griefed
     * @param textFile The file which will be read into a String of which then to create a HasteBin post of.
     * @return String. Returns a String containing the URL to the newly created HasteBin post.
     */
    private String createHasteBinFromFile(File textFile) {
        String text = null;
        String requestURL = serverpackcreatorproperties.getProperty(
                "de.griefed.serverpackcreator.configuration.hastebinserver",
                "https://haste.zneix.eu/documents"
        );

        String response = null;

        int postDataLength;

        URL url = null;

        HttpsURLConnection conn = null;

        byte[] postData;

        DataOutputStream dataOutputStream;

        BufferedReader bufferedReader;

        try {
            url = new URL(requestURL);
        }
        catch (IOException ex) {
            LOG.error("Error during acquisition of request URL.", ex);
        }

        try {
            text = FileUtils.readFileToString(textFile, "UTF-8");
        } catch (IOException ex) {
            LOG.error("Error reading text from file.",ex);
        }

        postData = Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8);
        postDataLength = postData.length;

        try {
            conn = (HttpsURLConnection) Objects.requireNonNull(url).openConnection();
        } catch (IOException ex) {
            LOG.error("Error during opening of connection to URL.", ex);
        }

        Objects.requireNonNull(conn).setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException ex) {
            LOG.error("Error during request of POST method.", ex);
        }

        conn.setRequestProperty("User-Agent", "HasteBin-Creator for ServerPackCreator");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);

        try {
            dataOutputStream = new DataOutputStream(conn.getOutputStream());
            dataOutputStream.write(postData);
            bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response = bufferedReader.readLine();
        } catch (IOException ex) {
            LOG.error("Error encountered when acquiring response from URL.", ex);
        }

        if (Objects.requireNonNull(response).contains("\"key\"")) {
            response = "https://haste.zneix.eu/" + response.substring(response.indexOf(":") + 2, response.length() - 2);
        }

        if (response.contains("https://haste.zneix.eu")) {
            return response;
        } else {
            return LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.response");
        }

    }
}
