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

import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.commonutilities.InvalidFileTypeException;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.versionchecker.Update;
import mdlaf.MaterialLookAndFeel;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * This class creates our menubar which will be displayed at the top of the ServerPackCreator frame. It contains various
 * menus and menuitems to execute, change, open and edit various different aspects of ServerPackCreator.
 * @author Griefed
 */
@Generated
public class MainMenuBar extends Component {

    private static final Logger LOG = LogManager.getLogger(MainMenuBar.class);

    private final ApplicationHome APPLICATIONHOME = new ApplicationHome(de.griefed.serverpackcreator.Main.class);

    private final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final UpdateChecker UPDATECHECKER;
    private final de.griefed.serverpackcreator.utilities.commonutilities.Utilities UTILITIES;

    private final LightTheme LIGHTTHEME;
    private final DarkTheme DARKTHEME;

    private final JFrame FRAME_SERVERPACKCREATOR;

    private final TabCreateServerPack TAB_CREATESERVERPACK;

    private final JTabbedPane TABBEDPANE;

    private final WindowEvent CLOSEEVENT;

    private final MaterialLookAndFeel LAF_DARK;
    private final MaterialLookAndFeel LAF_LIGHT;

    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);
    private final Dimension ABOUTDIMENSION = new Dimension(925,520);
    private final Dimension FILETOOLARGEDIMENSION = new Dimension(200,10);

    private final ImageIcon HELPICON = new ImageIcon(Objects.requireNonNull(ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/help.png")));
    private final ImageIcon ICON_HASTEBIN = new ImageIcon(Objects.requireNonNull(ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/hastebin.png")));

    private final JMenuBar MENUBAR = new JMenuBar();

    private final String ABOUTWINDOWTEXT;
    private final String FILETOOLARGETEXT;
    private final String FILETOOLARGETITLE;

    private final String[] HASTEOPTIONS = new String[3];

    private final StyledDocument ABOUTWINDOWDOCUMENT = new DefaultStyledDocument();
    private final StyledDocument CONFIGWINDOWDOCUMENT = new DefaultStyledDocument();
    private final StyledDocument SPCLOGWINDOWDOCUMENT = new DefaultStyledDocument();
    private final StyledDocument FILETOOLARGEWINDOWDOCUMENT = new DefaultStyledDocument();

    private final SimpleAttributeSet ABOUTATTRIBUTESET = new SimpleAttributeSet();
    private final SimpleAttributeSet CONFIGATTRIBUTESET = new SimpleAttributeSet();
    private final SimpleAttributeSet SPCLOGATTRIBUTESET = new SimpleAttributeSet();
    private final SimpleAttributeSet FILETOOLARGEATTRIBUTESET = new SimpleAttributeSet();

    private final JTextPane ABOUTWINDOWTEXTPANE = new JTextPane(ABOUTWINDOWDOCUMENT);
    private final JTextPane CONFIGWINDOWTEXTPANE = new JTextPane(CONFIGWINDOWDOCUMENT);
    private final JTextPane SPCLOGWINDOWTEXTPANE = new JTextPane(SPCLOGWINDOWDOCUMENT);
    private final JTextPane FILETOOLARGEWINDOWTEXTPANE = new JTextPane();

    private final MaterialTextPaneUI MATERIALTEXTPANEUI = new MaterialTextPaneUI();

    private boolean isDarkTheme;

    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenu aboutMenu;

    private JMenuItem file_NewConfigurationMenuItem;
    private JMenuItem file_LoadConfigMenuItem;
    private JMenuItem file_SaveConfigMenuItem;
    private JMenuItem file_SaveAsConfigMenuItem;
    private JMenuItem file_UploadConfigurationToHasteBin;
    private JMenuItem file_UploadServerPackCreatorLogToHasteBin;
    private JMenuItem file_ExitConfigMenuItem;
    private JMenuItem file_UpdateFallbackModslist;

    private JMenuItem edit_SwitchTheme;
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
    private JMenuItem about_OpenWikiHelpMenuItem;
    private JMenuItem about_OpenWikiHowToMenuItem;
    private JMenuItem about_CheckForUpdates;

    private JFileChooser configChooser;

    private File lastLoadedConfigurationFile = null;

    /**
     * Constructor for our MainMenuBar. Prepares various Strings, Arrays, Panels and windows.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedLightTheme Instance of {@link LightTheme} required for theme switching.
     * @param injectedDarkTheme Instance of {@link DarkTheme} required for theme switching.
     * @param injectedJFrame The parent from in which everything ServerPackCreator is displayed in.
     * @param injectedLAF_Light Instance of {@link MaterialLookAndFeel} with our {@link LightTheme}.
     * @param injectedLAF_Dark Instance of {@link MaterialLookAndFeel} with our {@link DarkTheme}.
     * @param injectedTabCreateServerPack Our tab for configuring ServerPackCreator.
     * @param injectedTabbedPane The tabbed pane which holds all our tabs.
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     * @param injectedUpdateChecker Instance of {@link UpdateChecker} to check for update-availability.
     * @param injectedUtilities Instance of {@link Utilities} for various things.
     */
    public MainMenuBar(LocalizationManager injectedLocalizationManager,
                       LightTheme injectedLightTheme,
                       DarkTheme injectedDarkTheme,
                       JFrame injectedJFrame,
                       MaterialLookAndFeel injectedLAF_Light,
                       MaterialLookAndFeel injectedLAF_Dark,
                       TabCreateServerPack injectedTabCreateServerPack,
                       JTabbedPane injectedTabbedPane,
                       ApplicationProperties injectedApplicationProperties,
                       UpdateChecker injectedUpdateChecker,
                       de.griefed.serverpackcreator.utilities.commonutilities.Utilities injectedUtilities
    ) {

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

        if (injectedUpdateChecker == null) {
            this.UPDATECHECKER = new UpdateChecker(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.UPDATECHECKER = injectedUpdateChecker;
        }

        if (injectedUtilities == null) {
            this.UTILITIES = new de.griefed.serverpackcreator.utilities.commonutilities.Utilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.UTILITIES = injectedUtilities;
        }

        this.LIGHTTHEME = injectedLightTheme;
        this.DARKTHEME = injectedDarkTheme;
        this.FRAME_SERVERPACKCREATOR = injectedJFrame;
        this.LAF_LIGHT = injectedLAF_Light;
        this.LAF_DARK = injectedLAF_Dark;
        this.TAB_CREATESERVERPACK = injectedTabCreateServerPack;
        this.TABBEDPANE = injectedTabbedPane;

        try {
            isDarkTheme = Boolean.parseBoolean(APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.gui.darkmode"));
        } catch (NullPointerException ex) {
            LOG.error("No setting for darkmode found in properties-file. Using true.");
            isDarkTheme = true;
            APPLICATIONPROPERTIES.put("de.griefed.serverpackcreator.gui.darkmode", "true");
        }

        CLOSEEVENT = new WindowEvent(FRAME_SERVERPACKCREATOR, WindowEvent.WINDOW_CLOSING);

        ABOUTWINDOWTEXT = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.text");
        ABOUTWINDOWTEXTPANE.setEditable(false);
        ABOUTWINDOWTEXTPANE.setOpaque(false);
        ABOUTWINDOWTEXTPANE.setMinimumSize(ABOUTDIMENSION);
        ABOUTWINDOWTEXTPANE.setPreferredSize(ABOUTDIMENSION);
        ABOUTWINDOWTEXTPANE.setMaximumSize(ABOUTDIMENSION);
        StyleConstants.setBold(ABOUTATTRIBUTESET, true);
        StyleConstants.setFontSize(ABOUTATTRIBUTESET, 14);
        ABOUTWINDOWTEXTPANE.setCharacterAttributes(ABOUTATTRIBUTESET, true);
        StyleConstants.setAlignment(ABOUTATTRIBUTESET, StyleConstants.ALIGN_CENTER);
        ABOUTWINDOWDOCUMENT.setParagraphAttributes(0, ABOUTWINDOWDOCUMENT.getLength(), ABOUTATTRIBUTESET, false);
        try {
            ABOUTWINDOWDOCUMENT.insertString(0, ABOUTWINDOWTEXT, ABOUTATTRIBUTESET);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        ABOUTWINDOWTEXTPANE.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(ABOUTWINDOWTEXTPANE);
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

        CONFIGWINDOWTEXTPANE.setOpaque(false);
        CONFIGWINDOWTEXTPANE.setEditable(false);
        StyleConstants.setBold(CONFIGATTRIBUTESET, true);
        StyleConstants.setFontSize(CONFIGATTRIBUTESET, 14);
        CONFIGWINDOWTEXTPANE.setCharacterAttributes(CONFIGATTRIBUTESET, true);
        StyleConstants.setAlignment(CONFIGATTRIBUTESET, StyleConstants.ALIGN_LEFT);
        CONFIGWINDOWDOCUMENT.setParagraphAttributes(0, CONFIGWINDOWDOCUMENT.getLength(), CONFIGATTRIBUTESET, false);
        CONFIGWINDOWTEXTPANE.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(CONFIGWINDOWTEXTPANE);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        SPCLOGWINDOWTEXTPANE.setOpaque(false);
        SPCLOGWINDOWTEXTPANE.setEditable(false);
        StyleConstants.setBold(SPCLOGATTRIBUTESET, true);
        StyleConstants.setFontSize(SPCLOGATTRIBUTESET, 14);
        SPCLOGWINDOWTEXTPANE.setCharacterAttributes(SPCLOGATTRIBUTESET, true);
        StyleConstants.setAlignment(SPCLOGATTRIBUTESET, StyleConstants.ALIGN_LEFT);
        SPCLOGWINDOWDOCUMENT.setParagraphAttributes(0, SPCLOGWINDOWDOCUMENT.getLength(), SPCLOGATTRIBUTESET, false);
        SPCLOGWINDOWTEXTPANE.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(SPCLOGWINDOWTEXTPANE);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        FILETOOLARGETEXT = LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.filetoolarge");
        FILETOOLARGETITLE = LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.filetoolargetitle");
        FILETOOLARGEWINDOWTEXTPANE.setOpaque(false);
        FILETOOLARGEWINDOWTEXTPANE.setEditable(false);
        FILETOOLARGEWINDOWTEXTPANE.setMinimumSize(FILETOOLARGEDIMENSION);
        FILETOOLARGEWINDOWTEXTPANE.setPreferredSize(FILETOOLARGEDIMENSION);
        FILETOOLARGEWINDOWTEXTPANE.setMaximumSize(FILETOOLARGEDIMENSION);
        StyleConstants.setBold(FILETOOLARGEATTRIBUTESET, true);
        StyleConstants.setFontSize(FILETOOLARGEATTRIBUTESET, 14);
        FILETOOLARGEWINDOWTEXTPANE.setCharacterAttributes(FILETOOLARGEATTRIBUTESET, true);
        StyleConstants.setAlignment(FILETOOLARGEATTRIBUTESET, StyleConstants.ALIGN_LEFT);
        FILETOOLARGEWINDOWDOCUMENT.setParagraphAttributes(0, FILETOOLARGEWINDOWDOCUMENT.getLength(), FILETOOLARGEATTRIBUTESET, false);
        try {
            FILETOOLARGEWINDOWDOCUMENT.insertString(0, FILETOOLARGETEXT, FILETOOLARGEATTRIBUTESET);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        FILETOOLARGEWINDOWTEXTPANE.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(FILETOOLARGEWINDOWTEXTPANE);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });
    }

    /**
     * Create the menubar, add all menus, add all menuitems and add actionlisteners for our menuitems.
     * @author Griefed
     * @return JMenuBar. Returns the menubar containing all elements we need to control various aspects of our app.
     */
    public JMenuBar createMenuBar() {

        // create menus
        fileMenu = new JMenu(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menu.file"));
        editMenu = new JMenu(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menu.edit"));
        viewMenu = new JMenu(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menu.view"));
        aboutMenu = new JMenu(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menu.about"));

        // create menu items
        file_NewConfigurationMenuItem = new JMenuItem("New configuration");
        file_LoadConfigMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.loadconfig"));
        file_SaveConfigMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.saveconfig"));
        file_SaveAsConfigMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.saveas"));
        file_UploadConfigurationToHasteBin = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.uploadconfig"));
        file_UploadServerPackCreatorLogToHasteBin = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.uploadlog"));
        file_UpdateFallbackModslist = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updatefallback"));
        file_ExitConfigMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.exit"));

        edit_SwitchTheme = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.theme"));
        edit_OpenInEditorServerProperties = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.serverproperties"));
        edit_OpenInEditorServerIcon = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.servericon"));

        view_OpenAddonsDirectoryMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.addonsdir"));
        view_ExampleAddonRepositoryMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.exampleaddonrepo"));
        view_OpenServerPackCreatorDirectoryMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.spcdir"));
        view_OpenServerPacksDirectoryMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.serverpacksdir"));
        view_OpenServerFilesDirectoryMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.serverfilesdir"));

        about_OpenAboutWindowMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.about"));
        about_OpenGitHubPageMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.repository"));
        about_OpenGitHubIssuesPageMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.issues"));
        about_OpenReleasesPageMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.releases"));
        about_OpenDiscordLinkMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.discord"));
        about_OpenDonationsPageMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.donate"));
        about_OpenWikiHelpMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.wiki.help"));
        about_OpenWikiHowToMenuItem = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.wiki.howto"));
        about_CheckForUpdates = new JMenuItem(LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updates"));

        // create action listeners for items
        file_NewConfigurationMenuItem.addActionListener(this::actionEventNewConfiguration);
        file_LoadConfigMenuItem.addActionListener(this::actionEventLoadConfigurationFromFileMenuItem);
        file_SaveConfigMenuItem.addActionListener(this::actionEventSaveConfigToFileMenuItem);
        file_SaveAsConfigMenuItem.addActionListener(this::actionEventSaveAsConfigToFileMenuItem);
        file_UploadConfigurationToHasteBin.addActionListener(this::actionEventUploadConfigurationToHasteBinMenuItem);
        file_UploadServerPackCreatorLogToHasteBin.addActionListener(this::actionEventUploadServerPackCreatorLogToHasteBinMenuItem);
        file_UpdateFallbackModslist.addActionListener(this::actionEventUpdateFallbackModslist);
        file_ExitConfigMenuItem.addActionListener(this::actionEventExitMenuItem);

        edit_SwitchTheme.addActionListener(this::actionEventSwitchThemeMenuItem);
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
        about_OpenWikiHelpMenuItem.addActionListener(this::actionEventOpenWikiHelpMenuItem);
        about_OpenWikiHowToMenuItem.addActionListener(this::actionEventOpenWikiHowToMenuItem);
        about_CheckForUpdates.addActionListener(this::actionEventCheckForUpdates);

        // add items to menus
        fileMenu.add(file_NewConfigurationMenuItem);
        fileMenu.add(file_LoadConfigMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_SaveConfigMenuItem);
        fileMenu.add(file_SaveAsConfigMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_UploadConfigurationToHasteBin);
        fileMenu.add(file_UploadServerPackCreatorLogToHasteBin);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_UpdateFallbackModslist);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_ExitConfigMenuItem);

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
        aboutMenu.add(about_CheckForUpdates);
        aboutMenu.add(new JSeparator());
        aboutMenu.add(about_OpenWikiHelpMenuItem);
        aboutMenu.add(about_OpenWikiHowToMenuItem);
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

        return MENUBAR;
    }

    private void actionEventCheckForUpdates(ActionEvent actionEvent) {
        LOG.debug("Clicked Check for Updates");

        if (!displayUpdateDialog()) {
            JOptionPane.showMessageDialog(
                    FRAME_SERVERPACKCREATOR,
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updates.none"),
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updates.none.title"),
                    JOptionPane.INFORMATION_MESSAGE,
                    UIManager.getIcon("OptionPane.informationIcon")
            );
        }
    }

    /**
     * If an initialize is available for ServerPackCreator, display a dialog asking the user whether
     * @author Griefed
     * @return {@link Boolean} <code>true</code> if an update was found and the dialog displayed.
     */
    protected boolean displayUpdateDialog() {

        Optional<Update> update = UPDATECHECKER.checkForUpdate(APPLICATIONPROPERTIES.getServerPackCreatorVersion(), APPLICATIONPROPERTIES.checkForAvailablePreReleases());

        if (update.isPresent()) {
            String textContent = String.format(LOCALIZATIONMANAGER.getLocalizedString("update.dialog.new"), update.get().url());

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
                            Desktop.getDesktop().browse(update.get().url().toURI());
                        }
                    } catch (IOException | RuntimeException | URISyntaxException ex) {
                        LOG.error("Error opening browser.", ex);
                    }
                    break;

                case 1:

                    clipboard.setContents(new StringSelection(update.get().url().toString()), null);
                    break;

                default:
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Update the fallback clientside-only mods-list from the repositories.
     * @author Grefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventUpdateFallbackModslist(ActionEvent actionEvent) {
        LOG.debug("Running update check for fallback modslist...");
        if (APPLICATIONPROPERTIES.updateFallback()) {
            JOptionPane.showMessageDialog(
                    FRAME_SERVERPACKCREATOR,
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updatefallback.updated"),
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updatefallback.title"),
                    JOptionPane.INFORMATION_MESSAGE,
                    UIManager.getIcon("OptionPane.informationIcon")
            );
        } else {
            JOptionPane.showMessageDialog(
                    FRAME_SERVERPACKCREATOR,
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updatefallback.nochange"),
                    LOCALIZATIONMANAGER.getLocalizedString("menubar.gui.menuitem.updatefallback.title"),
                    JOptionPane.INFORMATION_MESSAGE,
                    UIManager.getIcon("OptionPane.informationIcon")
            );
        }
    }

    /**
     * Open the given url in a browser.
     * @author Griefed
     * @param uri {@link URI} the URI to the website you want to open.
     */
    private void openLinkInBrowser(URI uri) {
        try {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (IOException ex) {
            LOG.error("Error opening browser with link " + uri + ".", ex);
        }
    }

    /**
     * Open the Help-section of the wiki in a browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenWikiHelpMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Help.");

        openLinkInBrowser(URI.create("https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help"));
    }

    /**
     * Open the HowTo-section of the wiki in a browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenWikiHowToMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Getting started.");

        openLinkInBrowser(URI.create("https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-HowTo"));
    }

    /**
     * Upon button-press, load default values for textfields so the user can start with a new configuration. Just as if ServerPackCreator
     * was started without a serverpackcreator.conf being present.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventNewConfiguration(ActionEvent actionEvent) {
        LOG.debug("Clearing GUI...");
        TAB_CREATESERVERPACK.clearInterface();
        lastLoadedConfigurationFile = null;
    }

    /**
     * Upon button-press, open the Discord invite-link to Griefed's Discord server in the users default browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenDiscordLinkMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Join Discord.");

        openLinkInBrowser(URI.create("https://discord.griefed.de"));
    }

    /**
     * Upon button-press, open ServerPackCreators issue-page on GitHub in the users default browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenIssuesMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Open Issues page on GitHub.");

        openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
    }

    /**
     * Upon button-press, uploads the serverpackcreator.log-file to HasteBin and display a dialog asking the user whether
     * they want to open the URL in their default browser or copy the link to their clipboard. If the filesize exceeds 10 MB,
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
                SPCLOGWINDOWDOCUMENT.insertString(0, textContent, SPCLOGATTRIBUTESET);
            } catch (BadLocationException ex) {
                LOG.error("Error inserting text into aboutDocument.", ex);
            }

            MATERIALTEXTPANEUI.installUI(SPCLOGWINDOWTEXTPANE);

            switch (JOptionPane.showOptionDialog(
                    FRAME_SERVERPACKCREATOR,
                    SPCLOGWINDOWTEXTPANE,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    ICON_HASTEBIN,
                    HASTEOPTIONS,
                    HASTEOPTIONS[0])) {

                case 0:

                    openLinkInBrowser(URI.create(urltoHasteBin));

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
                CONFIGWINDOWDOCUMENT.insertString(0, textContent, CONFIGATTRIBUTESET);
            } catch (BadLocationException ex) {
                LOG.error("Error inserting text into aboutDocument.", ex);
            }

            MATERIALTEXTPANEUI.installUI(CONFIGWINDOWTEXTPANE);

            switch (JOptionPane.showOptionDialog(
                    FRAME_SERVERPACKCREATOR,
                    CONFIGWINDOWTEXTPANE,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.hastebin.dialog"),
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    ICON_HASTEBIN,
                    HASTEOPTIONS,
                    HASTEOPTIONS[0])) {

                case 0:

                    openLinkInBrowser(URI.create(urltoHasteBin));

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
     * Opens a dialog informing the user that a file exceeds 10 MB in size.
     * @author Griefed
     */
    private void fileTooLargeDialog() {
        MATERIALTEXTPANEUI.installUI(FILETOOLARGEWINDOWTEXTPANE);
        JOptionPane.showConfirmDialog(
                FRAME_SERVERPACKCREATOR,
                FILETOOLARGEWINDOWTEXTPANE,
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

        if (new File(TAB_CREATESERVERPACK.getServerPropertiesPath()).isFile()) {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                    Desktop.getDesktop().open(
                            new File(TAB_CREATESERVERPACK.getServerPropertiesPath())
                    );
                }
            } catch (IOException ex) {
                LOG.error("Error opening custom server.properties.", ex);
            }
        } else {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                    Desktop.getDesktop().open(
                            new File(
                                    APPLICATIONHOME.getSource().toString().replace("\\","/")
                                            .replace(APPLICATIONHOME.getSource().toString()
                                                    .substring(
                                                            APPLICATIONHOME.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                            .replace("\\","/")
                                            + "/server_files/server.properties")
                    );
                }
            } catch (IOException ex) {
                LOG.error("Error opening default server.properties.", ex);
            }
        }
    }

    /**
     * Upon button-press, open the server-icon.png-file, in the server-files directory, in the users default picture-viewer.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenServerIcon(ActionEvent actionEvent) {
        LOG.debug("Clicked Open server-icon.png in Editor.");

        if (new File(TAB_CREATESERVERPACK.getServerIconPath()).isFile()) {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                    Desktop.getDesktop().open(
                            new File(TAB_CREATESERVERPACK.getServerIconPath())
                    );
                }
            } catch (IOException ex) {
                LOG.error("Error opening custom server-icon.png.", ex);
            }
        } else {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(
                            new File(
                                    APPLICATIONHOME.getSource().toString().replace("\\","/")
                                            .replace(APPLICATIONHOME.getSource().toString()
                                                    .substring(
                                                            APPLICATIONHOME.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
                                            .replace("\\","/")
                                            + "/server_files/server-icon.png")
                    );
                }
            } catch (IOException ex) {
                LOG.error("Error opening default server-icon.png.", ex);
            }
        }

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
                TAB_CREATESERVERPACK.saveConfig(new File(configChooser.getSelectedFile().getCanonicalPath()));

                LOG.debug("Saved configuration to: " + configChooser.getSelectedFile().getCanonicalPath());

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"), ex);
            }
        }
    }

    /**
     * Upon button-press, save the current configuration in the GUI to the serverpackcreator.conf-file in ServerPackCreators
     * base directory. if <code>de.griefed.serverpackcreator.configuration.saveloadedconfig</code> is set to <code>true</code> and
     * the field <code>lastLoadedConfigurationFile</code> is not null, the last loaded configuration-file is also saved to.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventSaveConfigToFileMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Save.");
        LOG.debug("Saving serverpackcreator.conf");
        TAB_CREATESERVERPACK.saveConfig(new File("./serverpackcreator.conf"));

        if (lastLoadedConfigurationFile != null && APPLICATIONPROPERTIES.getSaveLoadedConfiguration()) {
            LOG.debug("Saving " + lastLoadedConfigurationFile.getName());
            TAB_CREATESERVERPACK.saveConfig(lastLoadedConfigurationFile);
        }

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

                try (OutputStream outputStream = Files.newOutputStream(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES.toPath())) {

                    APPLICATIONPROPERTIES.setProperty("de.griefed.serverpackcreator.gui.darkmode", String.valueOf(true));
                    APPLICATIONPROPERTIES.store(outputStream, null);

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

                try (OutputStream outputStream = Files.newOutputStream(APPLICATIONPROPERTIES.FILE_SERVERPACKCREATOR_PROPERTIES.toPath())) {

                    APPLICATIONPROPERTIES.setProperty("de.griefed.serverpackcreator.gui.darkmode", String.valueOf(false));
                    APPLICATIONPROPERTIES.store(outputStream, null);

                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } catch (UnsupportedLookAndFeelException ex) {
                LOG.error("Couldn't change theme.", ex);
            }
        }

        TAB_CREATESERVERPACK.validateInputFields();
        TAB_CREATESERVERPACK.updateStatusLabelForeground();
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

                File specifiedConfigFile;
                try {
                    specifiedConfigFile = new File(UTILITIES.FileUtils().resolveLink(configChooser.getSelectedFile()));
                } catch (InvalidFileTypeException ex) {
                    LOG.error("Could not resolve link/symlink. Using entry from user input for checks.",ex);
                    specifiedConfigFile = new File(configChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));
                }

                TAB_CREATESERVERPACK.loadConfig(specifiedConfigFile);
                lastLoadedConfigurationFile = specifiedConfigFile;

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
                            APPLICATIONHOME.getSource().toString().replace("\\","/")
                                    .replace(APPLICATIONHOME.getSource().toString()
                                            .substring(
                                                    APPLICATIONHOME.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
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

        openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreatorExampleAddon"));
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
                            APPLICATIONHOME.getSource().toString().replace("\\","/")
                                    .replace(APPLICATIONHOME.getSource().toString()
                                            .substring(
                                                    APPLICATIONHOME.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
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

        TAB_CREATESERVERPACK.openServerPacksFolder();
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
                            APPLICATIONHOME.getSource().toString().replace("\\","/")
                                    .replace(APPLICATIONHOME.getSource().toString()
                                            .substring(
                                                    APPLICATIONHOME.getSource().toString().replace("\\","/").lastIndexOf("/") + 1),"")
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

        MATERIALTEXTPANEUI.installUI(ABOUTWINDOWTEXTPANE);

        JOptionPane.showMessageDialog(
                FRAME_SERVERPACKCREATOR,
                ABOUTWINDOWTEXTPANE,
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

        openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator"));
    }

    /**
     * Upon button-press, open the GitHub Sponsors page in the users default-browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenDonateMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open donations link.");

        openLinkInBrowser(URI.create("https://github.com/sponsors/Griefed"));
    }

    /**
     * Upon button-press, open the GitHub releases page in the users default-browser.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenReleaseMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open releases link");

        openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator/releases"));
    }

    /**
     * Checks the filesize of the given file whether it is smaller or bigger than 10 MB.
     * @author Griefed
     * @param fileToCheck The file or directory to check.
     * @return Boolean. True if the file is smaller, false if the file is bigger than 10 MB.
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
        String requestURL = APPLICATIONPROPERTIES.getProperty(
                "de.griefed.serverpackcreator.configuration.hastebinserver",
                "https://haste.zneix.eu/documents"
        );

        String response = null;

        int postDataLength;

        URL url = null;

        HttpsURLConnection conn = null;

        byte[] postData;

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

        try (DataOutputStream dataOutputStream = new DataOutputStream(conn.getOutputStream())) {
            //dataOutputStream = new DataOutputStream(conn.getOutputStream());
            dataOutputStream.write(postData);

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

                response = bufferedReader.readLine();

            } catch (IOException ex) {
                LOG.error("Error encountered when acquiring HasteBin.", ex);
            }

        } catch (IOException ex) {
            LOG.error("Error encountered when acquiring HasteBin.", ex);
        }

        if (Objects.requireNonNull(response).contains("\"key\"")) {
            response = requestURL.replace("/documents", "/") + response.substring(response.indexOf(":") + 2, response.length() - 2);
        }

        if (response.contains(requestURL.replace("/documents", ""))) {
            return response;
        } else {
            return LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.abouttab.hastebin.response");
        }

    }
}
