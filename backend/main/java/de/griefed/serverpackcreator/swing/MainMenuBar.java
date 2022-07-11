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
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.InvalidFileTypeException;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.versionchecker.Update;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;
import mdlaf.MaterialLookAndFeel;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class creates our menubar which will be displayed at the top of the ServerPackCreator frame.
 * It contains various menus and menuitems to execute, change, open and edit various different
 * aspects of ServerPackCreator.
 *
 * @author Griefed
 */
@Generated
public class MainMenuBar extends Component {

  private static final Logger LOG = LogManager.getLogger(MainMenuBar.class);

  private final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

  private final I18n I18N;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final UpdateChecker UPDATECHECKER;
  private final de.griefed.serverpackcreator.utilities.common.Utilities UTILITIES;

  private final LightTheme LIGHTTHEME;
  private final DarkTheme DARKTHEME;

  private final JFrame FRAME_SERVERPACKCREATOR;

  private final TabCreateServerPack TAB_CREATESERVERPACK;

  private final JTabbedPane TABBEDPANE;

  private final WindowEvent CLOSEEVENT;

  private final MaterialLookAndFeel LAF_DARK;
  private final MaterialLookAndFeel LAF_LIGHT;

  private final Dimension CHOOSERDIMENSION = new Dimension(750, 450);
  private final Dimension ABOUTDIMENSION = new Dimension(925, 520);

  private final ImageIcon HELPICON =
      new ImageIcon(
          Objects.requireNonNull(
              ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/help.png")));
  private final ImageIcon ICON_HASTEBIN =
      new ImageIcon(
          Objects.requireNonNull(
              ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/hastebin.png")));

  private final JMenuBar MENUBAR = new JMenuBar();

  private final String[] HASTEOPTIONS = new String[3];

  private final StyledDocument ABOUT_DOCUMENT = new DefaultStyledDocument();
  private final StyledDocument CONFIG_DOCUMENT = new DefaultStyledDocument();
  private final StyledDocument SPCLOG_DOCUMENT = new DefaultStyledDocument();

  private final SimpleAttributeSet CONFIG_ATTRIBUTESET = new SimpleAttributeSet();
  private final SimpleAttributeSet SPCLOG_ATTRIBUTESET = new SimpleAttributeSet();

  private final JTextPane ABOUT_WINDOW_TEXTPANE = new JTextPane(ABOUT_DOCUMENT);
  private final JTextPane CONFIG_WINDOW_TEXTPANE = new JTextPane(CONFIG_DOCUMENT);
  private final JTextPane SPCLOG_WINDOW_TEXTPANE = new JTextPane(SPCLOG_DOCUMENT);
  private final JTextPane FILETOOLARGE_WINDOW_TEXTPANE = new JTextPane();

  private final MaterialTextPaneUI MATERIALTEXTPANEUI = new MaterialTextPaneUI();

  private boolean isDarkTheme;

  private JFileChooser configChooser;

  private File lastLoadedConfigurationFile = null;

  /**
   * Constructor for our MainMenuBar. Prepares various Strings, Arrays, Panels and windows.
   *
   * @param injectedI18n Instance of {@link I18n} required for localized log messages.
   * @param injectedLightTheme Instance of {@link LightTheme} required for theme switching.
   * @param injectedDarkTheme Instance of {@link DarkTheme} required for theme switching.
   * @param injectedJFrame The parent from in which everything ServerPackCreator is displayed in.
   * @param injectedLAF_Light Instance of {@link MaterialLookAndFeel} with our {@link LightTheme}.
   * @param injectedLAF_Dark Instance of {@link MaterialLookAndFeel} with our {@link DarkTheme}.
   * @param injectedTabCreateServerPack Our tab for configuring ServerPackCreator.
   * @param injectedTabbedPane The tabbed pane which holds all our tabs.
   * @param injectedApplicationProperties Instance of {@link Properties} required for various
   *     different things.
   * @param injectedUpdateChecker Instance of {@link UpdateChecker} to check for
   *     update-availability.
   * @param injectedUtilities Instance of {@link Utilities} for various things.
   * @author Griefed
   */
  public MainMenuBar(
      I18n injectedI18n,
      LightTheme injectedLightTheme,
      DarkTheme injectedDarkTheme,
      JFrame injectedJFrame,
      MaterialLookAndFeel injectedLAF_Light,
      MaterialLookAndFeel injectedLAF_Dark,
      TabCreateServerPack injectedTabCreateServerPack,
      JTabbedPane injectedTabbedPane,
      ApplicationProperties injectedApplicationProperties,
      UpdateChecker injectedUpdateChecker,
      de.griefed.serverpackcreator.utilities.common.Utilities injectedUtilities) {

    if (injectedApplicationProperties == null) {
      this.APPLICATIONPROPERTIES = new ApplicationProperties();
    } else {
      this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    }

    if (injectedI18n == null) {
      this.I18N = new I18n(APPLICATIONPROPERTIES);
    } else {
      this.I18N = injectedI18n;
    }

    if (injectedUpdateChecker == null) {
      this.UPDATECHECKER = new UpdateChecker();
    } else {
      this.UPDATECHECKER = injectedUpdateChecker;
    }

    if (injectedUtilities == null) {
      this.UTILITIES =
          new de.griefed.serverpackcreator.utilities.common.Utilities(I18N, APPLICATIONPROPERTIES);
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
      isDarkTheme =
          Boolean.parseBoolean(
              APPLICATIONPROPERTIES.getProperty("de.griefed.serverpackcreator.gui.darkmode"));
    } catch (NullPointerException ex) {
      LOG.error("No setting for darkmode found in properties-file. Using true.");
      isDarkTheme = true;
      APPLICATIONPROPERTIES.put("de.griefed.serverpackcreator.gui.darkmode", "true");
    }

    CLOSEEVENT = new WindowEvent(FRAME_SERVERPACKCREATOR, WindowEvent.WINDOW_CLOSING);

    String ABOUTWINDOWTEXT = I18N.getMessage("createserverpack.gui.about.text");
    ABOUT_WINDOW_TEXTPANE.setEditable(false);
    ABOUT_WINDOW_TEXTPANE.setOpaque(false);
    ABOUT_WINDOW_TEXTPANE.setMinimumSize(ABOUTDIMENSION);
    ABOUT_WINDOW_TEXTPANE.setPreferredSize(ABOUTDIMENSION);
    ABOUT_WINDOW_TEXTPANE.setMaximumSize(ABOUTDIMENSION);
    SimpleAttributeSet ABOUTATTRIBUTESET = new SimpleAttributeSet();
    StyleConstants.setBold(ABOUTATTRIBUTESET, true);
    StyleConstants.setFontSize(ABOUTATTRIBUTESET, 14);
    ABOUT_WINDOW_TEXTPANE.setCharacterAttributes(ABOUTATTRIBUTESET, true);
    StyleConstants.setAlignment(ABOUTATTRIBUTESET, StyleConstants.ALIGN_CENTER);
    ABOUT_DOCUMENT.setParagraphAttributes(0, ABOUT_DOCUMENT.getLength(), ABOUTATTRIBUTESET, false);
    try {
      ABOUT_DOCUMENT.insertString(0, ABOUTWINDOWTEXT, ABOUTATTRIBUTESET);
    } catch (BadLocationException ex) {
      LOG.error("Error inserting text into aboutDocument.", ex);
    }
    ABOUT_WINDOW_TEXTPANE.addHierarchyListener(
        e1 -> {
          Window window = SwingUtilities.getWindowAncestor(ABOUT_WINDOW_TEXTPANE);
          if (window instanceof Dialog) {
            Dialog dialog = (Dialog) window;
            if (!dialog.isResizable()) {
              dialog.setResizable(true);
            }
          }
        });

    HASTEOPTIONS[0] = I18N.getMessage("createserverpack.gui.about.hastebin.dialog.yes");
    HASTEOPTIONS[1] = I18N.getMessage("createserverpack.gui.about.hastebin.dialog.clipboard");
    HASTEOPTIONS[2] = I18N.getMessage("createserverpack.gui.about.hastebin.dialog.no");

    CONFIG_WINDOW_TEXTPANE.setOpaque(false);
    CONFIG_WINDOW_TEXTPANE.setEditable(false);
    StyleConstants.setBold(CONFIG_ATTRIBUTESET, true);
    StyleConstants.setFontSize(CONFIG_ATTRIBUTESET, 14);
    CONFIG_WINDOW_TEXTPANE.setCharacterAttributes(CONFIG_ATTRIBUTESET, true);
    StyleConstants.setAlignment(CONFIG_ATTRIBUTESET, StyleConstants.ALIGN_LEFT);
    CONFIG_DOCUMENT.setParagraphAttributes(
        0, CONFIG_DOCUMENT.getLength(), CONFIG_ATTRIBUTESET, false);
    CONFIG_WINDOW_TEXTPANE.addHierarchyListener(
        e1 -> {
          Window window = SwingUtilities.getWindowAncestor(CONFIG_WINDOW_TEXTPANE);
          if (window instanceof Dialog) {
            Dialog dialog = (Dialog) window;
            if (!dialog.isResizable()) {
              dialog.setResizable(true);
            }
          }
        });

    SPCLOG_WINDOW_TEXTPANE.setOpaque(false);
    SPCLOG_WINDOW_TEXTPANE.setEditable(false);
    StyleConstants.setBold(SPCLOG_ATTRIBUTESET, true);
    StyleConstants.setFontSize(SPCLOG_ATTRIBUTESET, 14);
    SPCLOG_WINDOW_TEXTPANE.setCharacterAttributes(SPCLOG_ATTRIBUTESET, true);
    StyleConstants.setAlignment(SPCLOG_ATTRIBUTESET, StyleConstants.ALIGN_LEFT);
    SPCLOG_DOCUMENT.setParagraphAttributes(
        0, SPCLOG_DOCUMENT.getLength(), SPCLOG_ATTRIBUTESET, false);
    SPCLOG_WINDOW_TEXTPANE.addHierarchyListener(
        e1 -> {
          Window window = SwingUtilities.getWindowAncestor(SPCLOG_WINDOW_TEXTPANE);
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
   *
   * @return JMenuBar. Returns the menubar containing all elements we need to control various
   *     aspects of our app.
   * @author Griefed
   */
  public JMenuBar createMenuBar() {

    // create menus
    JMenu fileMenu = new JMenu(I18N.getMessage("menubar.gui.menu.file"));
    JMenu editMenu = new JMenu(I18N.getMessage("menubar.gui.menu.edit"));
    JMenu viewMenu = new JMenu(I18N.getMessage("menubar.gui.menu.view"));
    JMenu aboutMenu = new JMenu(I18N.getMessage("menubar.gui.menu.about"));

    // create menu items
    JMenuItem file_NewConfigurationMenuItem = new JMenuItem("New configuration");
    JMenuItem file_LoadConfigMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.loadconfig"));
    JMenuItem file_SaveConfigMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.saveconfig"));
    JMenuItem file_SaveAsConfigMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.saveas"));
    JMenuItem file_UploadConfigurationToHasteBin =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.uploadconfig"));
    JMenuItem file_UploadServerPackCreatorLogToHasteBin =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.uploadlog"));
    JMenuItem file_UpdateFallbackModslist =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.updatefallback"));
    JMenuItem file_ExitConfigMenuItem = new JMenuItem(I18N.getMessage("menubar.gui.menuitem.exit"));

    JMenuItem edit_SwitchTheme = new JMenuItem(I18N.getMessage("menubar.gui.menuitem.theme"));
    JMenuItem edit_OpenInEditorServerProperties =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.serverproperties"));
    JMenuItem edit_OpenInEditorServerIcon =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.servericon"));

    JMenuItem view_OpenAddonsDirectoryMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.addonsdir"));
    JMenuItem view_ExampleAddonRepositoryMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.exampleaddonrepo"));
    JMenuItem view_OpenServerPackCreatorDirectoryMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.spcdir"));
    JMenuItem view_OpenServerPacksDirectoryMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.serverpacksdir"));
    JMenuItem view_OpenServerFilesDirectoryMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.serverfilesdir"));
    JMenuItem view_OpenSPCLog = new JMenuItem(I18N.getMessage("menubar.gui.menuitem.spclog"));
    JMenuItem view_OpenModloaderInstallerLog =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.modloaderlog"));
    JMenuItem view_OpenAddonLog = new JMenuItem(I18N.getMessage("menubar.gui.menuitem.addonlog"));

    JMenuItem about_OpenAboutWindowMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.about"));
    JMenuItem about_OpenGitHubPageMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.repository"));
    JMenuItem about_OpenGitHubIssuesPageMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.issues"));
    JMenuItem about_OpenReleasesPageMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.releases"));
    JMenuItem about_OpenDiscordLinkMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.discord"));
    JMenuItem about_OpenDonationsPageMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.donate"));
    JMenuItem about_OpenWikiHelpMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.wiki.help"));
    JMenuItem about_OpenWikiHowToMenuItem =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.wiki.howto"));
    JMenuItem about_CheckForUpdates =
        new JMenuItem(I18N.getMessage("menubar.gui.menuitem.updates"));

    // create action listeners for items
    file_NewConfigurationMenuItem.addActionListener(this::newConfiguration);
    file_LoadConfigMenuItem.addActionListener(this::loadConfigurationFromFileMenuItem);
    file_SaveConfigMenuItem.addActionListener(this::saveConfigToFileMenuItem);
    file_SaveAsConfigMenuItem.addActionListener(this::saveAsConfigToFileMenuItem);
    file_UploadConfigurationToHasteBin.addActionListener(
        this::uploadConfigurationToHasteBinMenuItem);
    file_UploadServerPackCreatorLogToHasteBin.addActionListener(
        this::uploadServerPackCreatorLogToHasteBinMenuItem);
    file_UpdateFallbackModslist.addActionListener(this::updateFallbackModslist);
    file_ExitConfigMenuItem.addActionListener(this::exitMenuItem);

    edit_SwitchTheme.addActionListener(this::switchThemeMenuItem);
    edit_OpenInEditorServerProperties.addActionListener(this::openInEditorServerProperties);
    edit_OpenInEditorServerIcon.addActionListener(this::openServerIcon);

    view_OpenServerPackCreatorDirectoryMenuItem.addActionListener(this::openSPCDirectoryMenuItem);
    view_OpenServerPacksDirectoryMenuItem.addActionListener(this::openServerPacksDirectoryMenuItem);
    view_OpenServerFilesDirectoryMenuItem.addActionListener(this::openServerFilesDirectoryMenuItem);
    view_OpenAddonsDirectoryMenuItem.addActionListener(this::openPluginsDirectoryMenuItem);
    view_ExampleAddonRepositoryMenuItem.addActionListener(this::viewExampleAddonMenuItem);
    view_OpenSPCLog.addActionListener(this::openSPClog);
    view_OpenModloaderInstallerLog.addActionListener(this::openModloaderInstallerLog);
    view_OpenAddonLog.addActionListener(this::openAddonsLog);

    about_OpenAboutWindowMenuItem.addActionListener(this::openAboutSPCMenuItem);
    about_OpenGitHubPageMenuItem.addActionListener(this::openGitHubMenuItem);
    about_OpenGitHubIssuesPageMenuItem.addActionListener(this::openIssuesMenuItem);
    about_OpenReleasesPageMenuItem.addActionListener(this::openReleaseMenuItem);
    about_OpenDiscordLinkMenuItem.addActionListener(this::openDiscordLinkMenuItem);
    about_OpenDonationsPageMenuItem.addActionListener(this::openDonateMenuItem);
    about_OpenWikiHelpMenuItem.addActionListener(this::openWikiHelpMenuItem);
    about_OpenWikiHowToMenuItem.addActionListener(this::openWikiHowToMenuItem);
    about_CheckForUpdates.addActionListener(this::checkForUpdates);

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
    viewMenu.add(new JSeparator());
    viewMenu.add(view_OpenSPCLog);
    viewMenu.add(view_OpenModloaderInstallerLog);
    viewMenu.add(view_OpenAddonLog);

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

  private void openAddonsLog(ActionEvent actionEvent) {
    LOG.debug("Clicked open Addons-log.");

    UTILITIES.FileUtils().openFile("logs/addons.log");
  }

  private void openModloaderInstallerLog(ActionEvent actionEvent) {
    LOG.debug("Clicked open Modloader-Installer-log.");

    UTILITIES.FileUtils().openFile("logs/modloader_installer.log");
  }

  private void openSPClog(ActionEvent actionEvent) {
    LOG.debug("Clicked open ServerPackCreator-log.");

    UTILITIES.FileUtils().openFile("logs/serverpackcreator.log");
  }

  private void checkForUpdates(ActionEvent actionEvent) {
    LOG.debug("Clicked Check for Updates");

    if (!displayUpdateDialog()) {
      JOptionPane.showMessageDialog(
          FRAME_SERVERPACKCREATOR,
          I18N.getMessage("menubar.gui.menuitem.updates.none"),
          I18N.getMessage("menubar.gui.menuitem.updates.none.title"),
          JOptionPane.INFORMATION_MESSAGE,
          UIManager.getIcon("OptionPane.informationIcon"));
    }
  }

  /**
   * If an initialize is available for ServerPackCreator, display a dialog asking the user whether
   *
   * @return {@link Boolean} <code>true</code> if an update was found and the dialog displayed.
   * @author Griefed
   */
  protected boolean displayUpdateDialog() {

    Optional<Update> update =
        UPDATECHECKER.checkForUpdate(
            APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION(),
            APPLICATIONPROPERTIES.checkForAvailablePreReleases());

    if (update.isPresent()) {
      String textContent = String.format(I18N.getMessage("update.dialog.new"), update.get().url());

      StyledDocument styledDocument = new DefaultStyledDocument();
      SimpleAttributeSet simpleAttributeSet = new SimpleAttributeSet();
      MaterialTextPaneUI materialTextPaneUI = new MaterialTextPaneUI();
      JTextPane jTextPane = new JTextPane(styledDocument);
      StyleConstants.setBold(simpleAttributeSet, true);
      StyleConstants.setFontSize(simpleAttributeSet, 14);
      jTextPane.setCharacterAttributes(simpleAttributeSet, true);
      StyleConstants.setAlignment(simpleAttributeSet, StyleConstants.ALIGN_LEFT);
      styledDocument.setParagraphAttributes(
          0, styledDocument.getLength(), simpleAttributeSet, false);
      jTextPane.addHierarchyListener(
          e1 -> {
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

      options[0] = I18N.getMessage("update.dialog.yes");
      options[1] = I18N.getMessage("update.dialog.no");
      options[2] = I18N.getMessage("update.dialog.clipboard");

      try {
        styledDocument.insertString(0, textContent, simpleAttributeSet);
      } catch (BadLocationException ex) {
        LOG.error("Error inserting text into aboutDocument.", ex);
      }

      materialTextPaneUI.installUI(jTextPane);

      switch (JOptionPane.showOptionDialog(
          FRAME_SERVERPACKCREATOR,
          jTextPane,
          I18N.getMessage("update.dialog.available"),
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.INFORMATION_MESSAGE,
          UIManager.getIcon("OptionPane.informationIcon"),
          options,
          options[0])) {
        case 0:
          try {
            UTILITIES.WebUtils().openLinkInBrowser(update.get().url().toURI());
          } catch (RuntimeException | URISyntaxException ex) {
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
   *
   * @param actionEvent The event which triggers this method.
   * @author Grefed
   */
  private void updateFallbackModslist(ActionEvent actionEvent) {
    LOG.debug("Running update check for fallback modslist...");
    if (APPLICATIONPROPERTIES.updateFallback()) {
      JOptionPane.showMessageDialog(
          FRAME_SERVERPACKCREATOR,
          I18N.getMessage("menubar.gui.menuitem.updatefallback.updated"),
          I18N.getMessage("menubar.gui.menuitem.updatefallback.title"),
          JOptionPane.INFORMATION_MESSAGE,
          UIManager.getIcon("OptionPane.informationIcon"));
    } else {
      JOptionPane.showMessageDialog(
          FRAME_SERVERPACKCREATOR,
          I18N.getMessage("menubar.gui.menuitem.updatefallback.nochange"),
          I18N.getMessage("menubar.gui.menuitem.updatefallback.title"),
          JOptionPane.INFORMATION_MESSAGE,
          UIManager.getIcon("OptionPane.informationIcon"));
    }
  }

  /**
   * Open the Help-section of the wiki in a browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openWikiHelpMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Help.");

    UTILITIES.WebUtils()
        .openLinkInBrowser(
            URI.create(
                "https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-Help"));
  }

  /**
   * Open the HowTo-section of the wiki in a browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openWikiHowToMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Getting started.");

    UTILITIES.WebUtils()
        .openLinkInBrowser(
            URI.create(
                "https://wiki.griefed.de/en/Documentation/ServerPackCreator/ServerPackCreator-HowTo"));
  }

  /**
   * Upon button-press, load default values for textfields so the user can start with a new
   * configuration. Just as if ServerPackCreator was started without a serverpackcreator.conf being
   * present.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void newConfiguration(ActionEvent actionEvent) {
    LOG.debug("Clearing GUI...");
    TAB_CREATESERVERPACK.clearInterface();
    lastLoadedConfigurationFile = null;
  }

  /**
   * Upon button-press, open the Discord invite-link to Griefed's Discord server in the users
   * default browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openDiscordLinkMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Join Discord.");

    UTILITIES.WebUtils().openLinkInBrowser(URI.create("https://discord.griefed.de"));
  }

  /**
   * Upon button-press, open ServerPackCreators issue-page on GitHub in the users default browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openIssuesMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Open Issues page on GitHub.");

    UTILITIES.WebUtils()
        .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
  }

  /**
   * Upon button-press, uploads the serverpackcreator.log-file to HasteBin and display a dialog
   * asking the user whether they want to open the URL in their default browser or copy the link to
   * their clipboard. If the filesize exceeds 10 MB, a warning is displayed, telling the user about
   * filesize limitations of HasteBin.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void uploadServerPackCreatorLogToHasteBinMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Upload ServerPackCreator Log to HasteBin.");

    if (UTILITIES.WebUtils().hasteBinPreChecks(new File("logs/serverpackcreator.log"))) {
      String urltoHasteBin =
          UTILITIES.WebUtils().createHasteBinFromFile(new File("logs/serverpackcreator.log"));
      String textContent = String.format("URL: %s", urltoHasteBin);

      try {
        SPCLOG_DOCUMENT.insertString(0, textContent, SPCLOG_ATTRIBUTESET);
      } catch (BadLocationException ex) {
        LOG.error("Error inserting text into aboutDocument.", ex);
      }

      displayUploadUrl(urltoHasteBin, SPCLOG_WINDOW_TEXTPANE);
    } else {
      fileTooLargeDialog();
    }
  }

  /**
   * Upon button-press, uploads the serverpackcreator.conf-file to HasteBin and display a dialog
   * asking the user whether they want to open the URL in their default browser or copy the link to
   * their clipboard.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void uploadConfigurationToHasteBinMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Upload Configuration to HasteBin.");

    if (UTILITIES.WebUtils().hasteBinPreChecks(new File("serverpackcreator.conf"))) {

      String urltoHasteBin =
          UTILITIES.WebUtils().createHasteBinFromFile(new File("serverpackcreator.conf"));
      String textContent = String.format("URL: %s", urltoHasteBin);

      try {
        CONFIG_DOCUMENT.insertString(0, textContent, CONFIG_ATTRIBUTESET);
      } catch (BadLocationException ex) {
        LOG.error("Error inserting text into aboutDocument.", ex);
      }

      displayUploadUrl(urltoHasteBin, CONFIG_WINDOW_TEXTPANE);
    } else {
      fileTooLargeDialog();
    }
  }

  /**
   * Display the given URL in a text pane.
   *
   * @param urltoHasteBin {@link String} The URL, as a String, to display.
   * @param displayTextPane {@link JTextPane} The text pane to display the URL in.
   * @author Griefed
   */
  private void displayUploadUrl(String urltoHasteBin, JTextPane displayTextPane) {
    MATERIALTEXTPANEUI.installUI(displayTextPane);

    switch (JOptionPane.showOptionDialog(
        FRAME_SERVERPACKCREATOR,
        displayTextPane,
        I18N.getMessage("createserverpack.gui.about.hastebin.dialog"),
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.INFORMATION_MESSAGE,
        ICON_HASTEBIN,
        HASTEOPTIONS,
        HASTEOPTIONS[0])) {
      case 0:
        UTILITIES.WebUtils().openLinkInBrowser(URI.create(urltoHasteBin));

        break;

      case 1:
        CLIPBOARD.setContents(new StringSelection(urltoHasteBin), null);
        break;

      default:
        break;
    }
  }

  /**
   * Opens a dialog informing the user that a file exceeds 10 MB in size.
   *
   * @author Griefed
   */
  private void fileTooLargeDialog() {
    MATERIALTEXTPANEUI.installUI(FILETOOLARGE_WINDOW_TEXTPANE);
    JOptionPane.showConfirmDialog(
        FRAME_SERVERPACKCREATOR,
        I18N.getMessage("menubar.gui.filetoolarge"),
        I18N.getMessage("menubar.gui.filetoolargetitle"),
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE,
        ICON_HASTEBIN);
  }

  /**
   * Upon button-press, open the server.properties-file, in the server-files directory, in the users
   * default text-editor.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openInEditorServerProperties(ActionEvent actionEvent) {
    LOG.debug("Clicked Open server.properties in Editor.");

    if (new File(TAB_CREATESERVERPACK.getServerPropertiesPath()).isFile()) {
      UTILITIES.FileUtils().openFile(TAB_CREATESERVERPACK.getServerPropertiesPath());
    } else {
      UTILITIES.FileUtils().openFile("./server_files/server.properties");
    }
  }

  /**
   * Upon button-press, open the server-icon.png-file, in the server-files directory, in the users
   * default picture-viewer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openServerIcon(ActionEvent actionEvent) {
    LOG.debug("Clicked Open server-icon.png in Editor.");

    if (new File(TAB_CREATESERVERPACK.getServerIconPath()).isFile()) {
      UTILITIES.FileUtils().openFile(TAB_CREATESERVERPACK.getServerIconPath());
    } else {
      UTILITIES.FileUtils().openFile("./server_files/server-icon.png");
    }
  }

  /**
   * Upon button-press, close ServerPackCreator gracefully.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void exitMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Exit.");
    FRAME_SERVERPACKCREATOR.dispatchEvent(CLOSEEVENT);
  }

  /**
   * Upon button-press, open a Filechooser dialog which allows the user to specify a file in which
   * the current configuration in the GUI will be saved to.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void saveAsConfigToFileMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Save As...");

    configChooser = new JFileChooser();
    configChooser.setCurrentDirectory(new File("."));
    configChooser.setDialogTitle("Store current configuration");
    configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    configChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.buttonloadconfig.filter"), "conf"));
    configChooser.setAcceptAllFileFilterUsed(false);
    configChooser.setMultiSelectionEnabled(false);
    configChooser.setPreferredSize(CHOOSERDIMENSION);

    if (configChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

      try {

        if (configChooser.getSelectedFile().getCanonicalPath().endsWith(".conf")) {

          TAB_CREATESERVERPACK.saveConfig(
              new File(configChooser.getSelectedFile().getCanonicalPath()));
          LOG.debug(
              "Saved configuration to: " + configChooser.getSelectedFile().getCanonicalPath());

        } else {

          TAB_CREATESERVERPACK.saveConfig(
              new File(configChooser.getSelectedFile().getCanonicalPath() + ".conf"));
          LOG.debug(
              "Saved configuration to: "
                  + configChooser.getSelectedFile().getCanonicalPath()
                  + ".conf");
        }

      } catch (IOException ex) {
        LOG.error("Error loading configuration from selected file.", ex);
      }
    }
  }

  /**
   * Upon button-press, save the current configuration in the GUI to the serverpackcreator.conf-file
   * in ServerPackCreators base directory. if <code>
   * de.griefed.serverpackcreator.configuration.saveloadedconfig</code> is set to <code>true</code>
   * and the field <code>lastLoadedConfigurationFile</code> is not null, the last loaded
   * configuration-file is also saved to.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void saveConfigToFileMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Save.");
    LOG.debug("Saving serverpackcreator.conf");
    TAB_CREATESERVERPACK.saveConfig(new File("./serverpackcreator.conf"));

    if (lastLoadedConfigurationFile != null && APPLICATIONPROPERTIES.getSaveLoadedConfiguration()) {
      LOG.debug("Saving " + lastLoadedConfigurationFile.getName());
      TAB_CREATESERVERPACK.saveConfig(lastLoadedConfigurationFile);
    }
  }

  /**
   * Upon button-press, change the current theme to either light or dark-mode, depending on which
   * theme is currently active.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void switchThemeMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked Toggle light/dark-mode.");

    if (!isDarkTheme) {
      try {
        UIManager.setLookAndFeel(LAF_DARK);
        MaterialLookAndFeel.changeTheme(DARKTHEME);

        isDarkTheme = true;

        try (OutputStream outputStream =
            Files.newOutputStream(APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES().toPath())) {

          APPLICATIONPROPERTIES.setProperty(
              "de.griefed.serverpackcreator.gui.darkmode", String.valueOf(true));
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

        isDarkTheme = false;

        try (OutputStream outputStream =
            Files.newOutputStream(APPLICATIONPROPERTIES.SERVERPACKCREATOR_PROPERTIES().toPath())) {

          APPLICATIONPROPERTIES.setProperty(
              "de.griefed.serverpackcreator.gui.darkmode", String.valueOf(false));
          APPLICATIONPROPERTIES.store(outputStream, null);

        } catch (IOException ex) {
          LOG.error("Couldn't write properties-file.", ex);
        }

      } catch (UnsupportedLookAndFeelException ex) {
        LOG.error("Couldn't change theme.", ex);
      }
    }

    SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);
    TABBEDPANE.setOpaque(true);
    TAB_CREATESERVERPACK.validateInputFields();
    TAB_CREATESERVERPACK.updatePanelTheme();
  }

  /**
   * Upon button-press, open a file-selector to load a serverpackcreator.conf-file into
   * ServerPackCreator.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void loadConfigurationFromFileMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked load configuration from file.");

    configChooser = new JFileChooser();
    configChooser.setCurrentDirectory(new File("."));
    configChooser.setDialogTitle(I18N.getMessage("createserverpack.gui.buttonloadconfig.title"));
    configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    configChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.buttonloadconfig.filter"), "conf"));
    configChooser.setAcceptAllFileFilterUsed(false);
    configChooser.setMultiSelectionEnabled(false);
    configChooser.setPreferredSize(CHOOSERDIMENSION);

    if (configChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

      try {

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info("Loading from configuration file: " + configChooser.getSelectedFile().getCanonicalPath());

        File specifiedConfigFile;
        try {
          specifiedConfigFile =
              new File(UTILITIES.FileUtils().resolveLink(configChooser.getSelectedFile()));
        } catch (InvalidFileTypeException ex) {
          LOG.error("Could not resolve link/symlink. Using entry from user input for checks.", ex);
          specifiedConfigFile =
              new File(configChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));
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
   * Upon button-press, open the folder containing installed plugins for ServerPackCreator in the
   * users file-explorer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openPluginsDirectoryMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open plugins directory.");
    UTILITIES.FileUtils().openFolder(APPLICATIONPROPERTIES.DIRECTORY_PLUGINS());
  }

  /**
   * Upon button-press, open the example plugins repository-page on GitHub in the users default
   * browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void viewExampleAddonMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked view example addon");

    UTILITIES.WebUtils()
        .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreatorExampleAddon"));
  }

  /**
   * Upon button-press, open the base directory of ServerPackCreator in the users file-explorer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openSPCDirectoryMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open installation directory.");
    UTILITIES.FileUtils().openFolder(".");
  }

  /**
   * Upon button-press, open the folder containing generated server packs in the users
   * file-explorer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openServerPacksDirectoryMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open server packs directory.");
    UTILITIES.FileUtils().openFolder(APPLICATIONPROPERTIES.getDirectoryServerPacks());
  }

  /**
   * Upon button-press, open the folder containing the server-icon.png and server.properties files
   * in the users file-explorer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openServerFilesDirectoryMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open server files directory.");
    UTILITIES.FileUtils().openFolder(APPLICATIONPROPERTIES.DIRECTORY_SERVER_FILES());
  }

  /**
   * Upon button-press, open an About-window containing information about ServerPackCreator.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openAboutSPCMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open about window.");

    JScrollPane ABOUTWINDOWSCROLLPANE =
        new JScrollPane(
            ABOUT_WINDOW_TEXTPANE,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    ABOUTWINDOWSCROLLPANE.setMinimumSize(ABOUTDIMENSION);
    ABOUTWINDOWSCROLLPANE.setPreferredSize(ABOUTDIMENSION);
    ABOUTWINDOWSCROLLPANE.setMaximumSize(ABOUTDIMENSION);

    JOptionPane.showMessageDialog(
        FRAME_SERVERPACKCREATOR,
        ABOUTWINDOWSCROLLPANE,
        I18N.getMessage("createserverpack.gui.createserverpack.about.title"),
        JOptionPane.INFORMATION_MESSAGE,
        HELPICON);
  }

  /**
   * Upon button-press, open the ServerPackCreator repository GitHub page in the users
   * default-browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openGitHubMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open GitHub repository link.");

    UTILITIES.WebUtils()
        .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator"));
  }

  /**
   * Upon button-press, open the GitHub Sponsors page in the users default-browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openDonateMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open donations link.");

    UTILITIES.WebUtils().openLinkInBrowser(URI.create("https://github.com/sponsors/Griefed"));
  }

  /**
   * Upon button-press, open the GitHub releases page in the users default-browser.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void openReleaseMenuItem(ActionEvent actionEvent) {
    LOG.debug("Clicked open releases link");

    UTILITIES.WebUtils()
        .openLinkInBrowser(URI.create("https://github.com/Griefed/ServerPackCreator/releases"));
  }
}
