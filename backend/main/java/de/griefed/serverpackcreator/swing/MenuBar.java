package de.griefed.serverpackcreator.swing;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import mdlaf.MaterialLookAndFeel;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class MenuBar extends Component {

    private static final Logger LOG = LogManager.getLogger(MenuBar.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final LightTheme LIGHTTHEME;
    private final DarkTheme DARKTHEME;
    private final JFrame FRAME_SERVERPACKCREATOR;
    private final TabCreateServerPack TAB_CREATESERVERPACK;
    private final JTabbedPane TABBEDPANE;

    private final MaterialLookAndFeel LAF_DARK;
    private final MaterialLookAndFeel LAF_LIGHT;

    private final File PROPERTIESFILE = new File("serverpackcreator.properties");

    private final WindowEvent CLOSEEVENT;

    private final String helpWindowText;
    private final String aboutWindowText;

    private final ImageIcon HELPICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/help.png")));

    private final JMenuBar MENUBAR = new JMenuBar();

    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);

    private Properties serverpackcreatorproperties;

    private boolean isDarkTheme;

    private final String[] javaArgsOptions = new String[4];

    private final String[] javaArgsSelections = new String[2];

    private final JTextField javaArgs = new JTextField();

    public MenuBar(LocalizationManager injectedLocalizationManager, LightTheme injectedLightTheme, DarkTheme injectedDarkTheme,
                   JFrame injectedJFrame, MaterialLookAndFeel injectedLAF_Light, MaterialLookAndFeel injectedLAF_Dark,
                   TabCreateServerPack injectedTabCreateServerPack, JTabbedPane injectedTabbedPane) {

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

        try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
            this.serverpackcreatorproperties = new Properties();
            this.serverpackcreatorproperties.load(inputStream);
        } catch (IOException ex) {
            LOG.error("Couldn't read properties file.", ex);
        }

        if (serverpackcreatorproperties.getProperty("de.griefed.serverpackcreator.gui.darkmode").equals("true")) {
            isDarkTheme = true;
        } else {
            isDarkTheme = false;
        }

        CLOSEEVENT = new WindowEvent(FRAME_SERVERPACKCREATOR, WindowEvent.WINDOW_CLOSING);

        aboutWindowText = LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.about.text");
        aboutWindowTextPane.setEditable(false);
        aboutWindowTextPane.setOpaque(false);
        aboutWindowTextPane.setMinimumSize(new Dimension(925,520));
        aboutWindowTextPane.setPreferredSize(new Dimension(925,520));
        aboutWindowTextPane.setMaximumSize(new Dimension(925,520));
        StyleConstants.setBold(aboutAttributeSet, true);
        StyleConstants.setFontSize(aboutAttributeSet, 14);
        aboutWindowTextPane.setCharacterAttributes(aboutAttributeSet, true);
        StyleConstants.setAlignment(aboutAttributeSet, StyleConstants.ALIGN_CENTER);
        aboutWindowDocument.setParagraphAttributes(0, aboutWindowDocument.getLength(), aboutAttributeSet, false);
        try {
            aboutWindowDocument.insertString(0, aboutWindowText, aboutAttributeSet);
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

        helpWindowText = String.format(
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
            helpWindowDocument.insertString(0, helpWindowText, helpAttributeSet);
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

        javaArgsOptions[0] = "OK";
        javaArgsOptions[1] = "Use Aikars flags";
        javaArgsOptions[2] = "Empty";
        javaArgsOptions[3] = "Cancel";

        javaArgsSelections[0] = "empty";
        javaArgsSelections[1] = "-Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
                "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 " +
                "-XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 " +
                "-XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -XX:G1NewSizePercent=30 " +
                "-XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 " +
                "-XX:InitiatingHeapOccupancyPercent=15 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true";

        Dimension dimension = new Dimension(750,20);
        javaArgs.setMinimumSize(dimension);
        javaArgs.setMaximumSize(dimension);
        javaArgs.setPreferredSize(dimension);
    }

    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenu aboutMenu;

    private JMenuItem file_LoadConfigMenuItem;
    private JMenuItem file_SaveConfigMenuItem;
    private JMenuItem file_SaveAsConfigMenuItem;
    private JMenuItem file_UploadConfigurationToHasteBin;
    private JMenuItem file_UploadServerPackCreatorLogToHasteBin;
    private JMenuItem file_RefreshManifestsMenuItem;
    private JMenuItem file_RefreshInstalledAddonsMenuItem;
    private JMenuItem file_ExitConfigMenuItem;

    private JMenuItem edit_SwitchTheme;
    private JMenuItem edit_ChangeJavaArgs;
    private JMenuItem edit_OpenInEditorServerProperties;
    private JMenuItem edit_OpenInEditorStartScriptWindowsFabric;
    private JMenuItem edit_OpenInEditorStartScriptLinuxFabric;
    private JMenuItem edit_OpenInEditorStartScriptWindowsForge;
    private JMenuItem edit_OpenInEditorStartScriptLinuxForge;

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

    private SimpleAttributeSet aboutAttributeSet = new SimpleAttributeSet();
    private SimpleAttributeSet helpAttributeSet = new SimpleAttributeSet();

    private JTextPane helpWindowTextPane = new JTextPane(helpWindowDocument);
    private JTextPane aboutWindowTextPane = new JTextPane(aboutWindowDocument);

    private MaterialTextPaneUI materialTextPaneUI = new MaterialTextPaneUI();

    /**
     * Getter for the serverpackcreator.properties file.
     * @author whitebear60
     * @return File. Returns the file which will set the locale for ServerPackCreator.
     */
    File getPropertiesFile() {
        return PROPERTIESFILE;
    }

    public JMenuBar createMenuBar() {

        // create menus
        fileMenu = new JMenu("File");
        editMenu = new JMenu("Edit");
        viewMenu = new JMenu("View");
        aboutMenu = new JMenu("About");
        help_OpenHelpWindowMenuItem = new JMenuItem("Help");

        // create menu items
        file_LoadConfigMenuItem = new JMenuItem("Load Configuration");
        file_SaveConfigMenuItem = new JMenuItem("Save Configuration");
        file_SaveAsConfigMenuItem = new JMenuItem("Save Configuration As...");
        file_UploadConfigurationToHasteBin = new JMenuItem("Upload Configuration to HasteBin");
        file_UploadServerPackCreatorLogToHasteBin = new JMenuItem("Upload ServerPackCreator Log to HasteBin");
        file_RefreshManifestsMenuItem = new JMenuItem("Reload Version-Manifests");
        file_RefreshInstalledAddonsMenuItem = new JMenuItem("Reload Installed Addons");
        file_ExitConfigMenuItem = new JMenuItem("Exit");

        edit_SwitchTheme = new JMenuItem("Toggle light/dark-mode");
        edit_ChangeJavaArgs = new JMenuItem("Edit Start-Scripts Java Args");
        edit_OpenInEditorServerProperties = new JMenuItem("Open server.properties in Editor");
        edit_OpenInEditorStartScriptWindowsFabric = new JMenuItem("Open start-fabric.bat in Editor");
        edit_OpenInEditorStartScriptLinuxFabric = new JMenuItem("Open start-fabric.sh in Editor");
        edit_OpenInEditorStartScriptWindowsForge = new JMenuItem("Open start-forge.bat in Editor");
        edit_OpenInEditorStartScriptLinuxForge = new JMenuItem("Open start-forge.sh in Editor");

        view_OpenAddonsDirectoryMenuItem = new JMenuItem("Open addons-directory");
        view_ExampleAddonRepositoryMenuItem = new JMenuItem("Visit example-addon repository");
        view_OpenServerPackCreatorDirectoryMenuItem = new JMenuItem("Open installation-directory");
        view_OpenServerPacksDirectoryMenuItem = new JMenuItem("Open server packs directory");
        view_OpenServerFilesDirectoryMenuItem = new JMenuItem("Open server-files directory");

        about_OpenAboutWindowMenuItem = new JMenuItem("About");
        about_OpenGitHubPageMenuItem = new JMenuItem("View Repository on GitHub");
        about_OpenGitHubIssuesPageMenuItem = new JMenuItem("View Issues on GitHub");
        about_OpenReleasesPageMenuItem = new JMenuItem("View Releases on GitHub");
        about_OpenDiscordLinkMenuItem = new JMenuItem("Join my Discord server!");
        about_OpenDonationsPageMenuItem = new JMenuItem("Support me!");

        // create action listeners for items
        file_LoadConfigMenuItem.addActionListener(this::actionEventLoadConfigurationFromFileMenuItem);
        file_SaveConfigMenuItem.addActionListener(this::actionEventSaveConfigToFileMenuItem);
        file_SaveAsConfigMenuItem.addActionListener(this::actionEventSaveAsConfigToFileMenuItem);
        file_UploadConfigurationToHasteBin.addActionListener(this::actionEventUploadConfigurationToHasteBinMenuItem);
        file_UploadServerPackCreatorLogToHasteBin.addActionListener(this::actionEventUploadServerPackCreatorLogToHasteBinMenuItem);
        file_RefreshManifestsMenuItem.addActionListener(this::actionEventRefreshManifestsMenuItem);
        file_RefreshInstalledAddonsMenuItem.addActionListener(this::actionEventRefreshInstalledAddonsMenuItem);
        file_ExitConfigMenuItem.addActionListener(this::actionEventExitMenuItem);

        edit_SwitchTheme.addActionListener(this::actionEventSwitchThemeMenuItem);
        edit_ChangeJavaArgs.addActionListener(this::actionEventChangeJavaArgsMenuItem);
        edit_OpenInEditorServerProperties.addActionListener(this::actionEventOpenInEditorServerProperties);
        edit_OpenInEditorStartScriptWindowsFabric.addActionListener(this::actionEventOpenInEditorStartScriptWindowsFabric);
        edit_OpenInEditorStartScriptLinuxFabric.addActionListener(this::actionEventOpenInEditorStartScriptLinuxFabric);
        edit_OpenInEditorStartScriptWindowsForge.addActionListener(this::actionEventOpenInEditorStartScriptWindowsForge);
        edit_OpenInEditorStartScriptLinuxForge.addActionListener(this::actionEventOpenInEditorStartScriptLinuxForge);

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
        fileMenu.add(file_RefreshManifestsMenuItem);
        fileMenu.add(file_RefreshInstalledAddonsMenuItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(file_ExitConfigMenuItem);

        editMenu.add(edit_ChangeJavaArgs);
        editMenu.add(new JSeparator());
        editMenu.add(edit_OpenInEditorServerProperties);
        editMenu.add(edit_OpenInEditorStartScriptWindowsFabric);
        editMenu.add(edit_OpenInEditorStartScriptLinuxFabric);
        editMenu.add(edit_OpenInEditorStartScriptWindowsForge);
        editMenu.add(edit_OpenInEditorStartScriptLinuxForge);
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

    private void actionEventOpenDiscordLinkMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Join Discord.");
    }

    private void actionEventOpenIssuesMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Open Issues page on GitHub.");
    }

    private void actionEventUploadServerPackCreatorLogToHasteBinMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Upload ServerPackCreator Log to HasteBin.");
    }

    private void actionEventUploadConfigurationToHasteBinMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Upload Configuration to HasteBin.");
    }

    private void actionEventOpenInEditorStartScriptLinuxForge(ActionEvent actionEvent) {
        LOG.debug("Clicked Open start-forge.sh in Editor.");
    }

    private void actionEventOpenInEditorStartScriptWindowsForge(ActionEvent actionEvent) {
        LOG.debug("Clicked Open start-forge.bat in Editor.");
    }

    private void actionEventOpenInEditorStartScriptLinuxFabric(ActionEvent actionEvent) {
        LOG.debug("Clicked Open start-fabric.sh in Editor.");
    }

    private void actionEventOpenInEditorStartScriptWindowsFabric(ActionEvent actionEvent) {
        LOG.debug("Clicked Open start-fabric.bat in Editor.");
    }

    private void actionEventOpenInEditorServerProperties(ActionEvent actionEvent) {
        LOG.debug("Clicked Open server.properties in Editor.");
    }

    private void actionEventChangeJavaArgsMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Edit Start-Scripts Java Args.");

        javaArgs.setText(TAB_CREATESERVERPACK.getJavaArgs());

        switch (JOptionPane.showOptionDialog(
                FRAME_SERVERPACKCREATOR,
                javaArgs,
                "Java Arguments",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                javaArgsOptions,
                javaArgsOptions[3]
                )
        ) {
            case 0:

                if (javaArgs.getText().equals("")) {
                    TAB_CREATESERVERPACK.setJavaArgs("empty");
                } else {
                    TAB_CREATESERVERPACK.setJavaArgs(javaArgs.getText());
                }

                break;

            case 1:

                TAB_CREATESERVERPACK.setJavaArgs(javaArgsSelections[1]);
                break;

            case 2:

                TAB_CREATESERVERPACK.setJavaArgs("empty");
                break;

            default:

        }

        LOG.debug("Java args set to: " + TAB_CREATESERVERPACK.getJavaArgs());
    }

    private void actionEventExitMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Exit.");
        FRAME_SERVERPACKCREATOR.dispatchEvent(CLOSEEVENT);
    }

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

                LOG.info("Saved configuration to: " + configChooser.getSelectedFile().getCanonicalPath());

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"), ex);
            }
        }
    }

    private void actionEventSaveConfigToFileMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked Save.");
        TAB_CREATESERVERPACK.saveConfig(new File("./serverpackcreator.conf"), false);
    }

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
                try {
                    TAB_CREATESERVERPACK.loadConfig(new File(configChooser.getSelectedFile().getCanonicalPath()));

                    LOG.info(String.format(
                            LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"),
                            configChooser.getSelectedFile().getCanonicalPath()
                    ));

                } catch (IOException ex) {
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"), ex);
                }

                LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile.finish"));


                LOG.info(String.format(
                        LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"),
                        configChooser.getSelectedFile().getCanonicalPath()
                ));

            } catch (IOException ex) {
                LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"), ex);
            }
        }
    }

    private void actionEventRefreshManifestsMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked refresh manifests.");
    }

    private void actionEventRefreshInstalledAddonsMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked refresh installed addons.");
    }

    private void actionEventOpenAddonsDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open addons directory.");
    }

    private void actionEventViewExampleAddonMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked view example addon");
    }

    private void actionEventOpenSPCDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open installation directory.");
    }

    private void actionEventOpenServerPacksDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open server packs directory.");
    }

    private void actionEventOpenServerFilesDirectoryMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open server files directory.");
    }

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

    private void actionEventOpenGitHubMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open GitHub repository link.");
    }

    private void actionEventOpenDonateMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open donations link.");
    }

    private void actionEventOpenReleaseMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open releases link");
    }
}
