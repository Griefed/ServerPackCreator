package de.griefed.serverpackcreator.swing;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import mdlaf.MaterialLookAndFeel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class MenuBar {

    private static final Logger LOG = LogManager.getLogger(MenuBar.class);

    private final LocalizationManager LOCALIZATIONMANAGER;
    private final LightTheme LIGHTTHEME;
    private final DarkTheme DARKTHEME;
    private final JFrame FRAME_SERVERPACKCREATOR;
    private final TabCreateServerPack TAB_CREATESERVERPACK;

    private final MaterialLookAndFeel LAF_DARK;
    private final MaterialLookAndFeel LAF_LIGHT;

    private final File PROPERTIESFILE = new File("serverpackcreator.properties");

    private JTextArea helpTextArea;

    private JScrollPane helpScrollPane;

    private Properties serverpackcreatorproperties;

    private boolean isDarkTheme;

    public MenuBar(LocalizationManager injectedLocalizationManager, LightTheme injectedLightTheme, DarkTheme injectedDarkTheme,
                   JFrame injectedJFrame, MaterialLookAndFeel injectedLAF_Light, MaterialLookAndFeel injectedLAF_Dark,
                   TabCreateServerPack injectedTabCreateServerPack) {
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
    }

    private final ImageIcon HELPICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/help.png")));

    private final JMenuBar MENUBAR = new JMenuBar();

    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);

    private JFileChooser configChooser;

    private JMenu dataMenu;
    private JMenu addonsMenu;
    private JMenu filesMenu;
    private JMenu aboutMenu;

    private JMenuItem switchTheme;
    private JMenuItem loadConfigurationFileMenuItem;
    private JMenuItem refreshManifestsMenuItem;
    private JMenuItem refreshInstalledAddonsMenuItem;
    private JMenuItem openAddonsDirectoryMenuItem;
    private JMenuItem viewExampleAddonMenuItem;
    private JMenuItem openSPCDirectoryMenuItem;
    private JMenuItem openServerPacksDirectoryMenuItem;
    private JMenuItem openServerFilesDirectoryMenuItem;
    private JMenuItem openAboutSPCMenuItem;
    private JMenuItem openHelpMenuItem;
    private JMenuItem openGitHubMenuItem;
    private JMenuItem openDonateMenuItem;
    private JMenuItem openReleasesMenuItem;

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
        dataMenu = new JMenu("Data");
        filesMenu = new JMenu("Files");
        addonsMenu = new JMenu("Addons");
        aboutMenu = new JMenu("About");

        // create menu items
        switchTheme = new JMenuItem("Switch theme between light/dark-mode");
        loadConfigurationFileMenuItem = new JMenuItem("Load configuration from file");
        refreshManifestsMenuItem = new JMenuItem("Refresh Manifests");
        refreshInstalledAddonsMenuItem = new JMenuItem("Refresh Addons");
        openAddonsDirectoryMenuItem = new JMenuItem("Open addons-directory");
        viewExampleAddonMenuItem = new JMenuItem("Visit example-addon repository");
        openSPCDirectoryMenuItem = new JMenuItem("Open installation-directory");
        openServerPacksDirectoryMenuItem = new JMenuItem("Open server packs directory");
        openServerFilesDirectoryMenuItem = new JMenuItem("Open server-files directory");
        openAboutSPCMenuItem = new JMenuItem("About ServerPackCreator");
        openHelpMenuItem = new JMenuItem("View Help");
        openGitHubMenuItem = new JMenuItem("Visit ServerPackCreator GitHub");
        openDonateMenuItem = new JMenuItem("Support me!");
        openReleasesMenuItem = new JMenuItem("Visit release-page");

        // create action listeners
        switchTheme.addActionListener(this::actionEventSwitchThemeMenuItem);
        loadConfigurationFileMenuItem.addActionListener(this::actionEventLoadConfigurationFromFileMenuItem);
        refreshManifestsMenuItem.addActionListener(this::actionEventRefreshManifestsMenuItem);
        refreshInstalledAddonsMenuItem.addActionListener(this::actionEventRefreshInstalledAddonsMenuItem);
        openAddonsDirectoryMenuItem.addActionListener(this::actionEventOpenAddonsDirectoryMenuItem);
        viewExampleAddonMenuItem.addActionListener(this::actionEventViewExampleAddonMenuItem);
        openSPCDirectoryMenuItem.addActionListener(this::actionEventOpenSPCDirectoryMenuItem);
        openServerPacksDirectoryMenuItem.addActionListener(this::actionEventOpenServerPacksDirectoryMenuItem);
        openServerFilesDirectoryMenuItem.addActionListener(this::actionEventOpenServerFilesDirectoryMenuItem);
        openAboutSPCMenuItem.addActionListener(this::actionEventOpenAboutSPCMenuItem);
        openHelpMenuItem.addActionListener(this::actionEventOpenHelpMenuItem);
        openGitHubMenuItem.addActionListener(this::actionEventOpenGitHubMenuItem);
        openDonateMenuItem.addActionListener(this::actionEventOpenDonateMenuItem);
        openReleasesMenuItem.addActionListener(this::actionEventOpenReleaseMenuItem);

        // add items to menus
        dataMenu.add(loadConfigurationFileMenuItem);
        dataMenu.add(refreshManifestsMenuItem);
        dataMenu.add(switchTheme);

        filesMenu.add(openSPCDirectoryMenuItem);
        filesMenu.add(openServerFilesDirectoryMenuItem);
        filesMenu.add(openServerFilesDirectoryMenuItem);

        addonsMenu.add(refreshInstalledAddonsMenuItem);
        addonsMenu.add(openAddonsDirectoryMenuItem);
        addonsMenu.add(viewExampleAddonMenuItem);

        aboutMenu.add(openAboutSPCMenuItem);
        aboutMenu.add(openHelpMenuItem);
        aboutMenu.add(openGitHubMenuItem);
        aboutMenu.add(openDonateMenuItem);
        aboutMenu.add(openReleasesMenuItem);

        // add menus
        MENUBAR.add(dataMenu);
        MENUBAR.add(filesMenu);
        MENUBAR.add(addonsMenu);
        MENUBAR.add(aboutMenu);

        return MENUBAR;
    }

    private void actionEventSwitchThemeMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked switch theme.");

        if (!isDarkTheme) {
            try {
                UIManager.setLookAndFeel(LAF_DARK);
                MaterialLookAndFeel.changeTheme(DARKTHEME);

                SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);

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

        configChooser.setFileFilter(new FileNameExtensionFilter(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonloadconfig.filter"),
                "conf"));

        configChooser.setAcceptAllFileFilterUsed(false);
        configChooser.setMultiSelectionEnabled(false);
        configChooser.setPreferredSize(CHOOSERDIMENSION);

        if (configChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

            try {
                try {
                    LOG.debug("Is EventDispatchedThread " + SwingUtilities.isEventDispatchThread());
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
    }

    /**
     * Upon button-press, open an info-window containing information/help about how to configure ServerPackCreator.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventOpenHelpMenuItem(ActionEvent actionEvent) {
        LOG.debug("Clicked open help window.");

        helpTextArea = new JTextArea(String.format(
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
        ));

        helpTextArea.setEditable(false);
        helpTextArea.setOpaque(false);

        helpScrollPane = new JScrollPane(
                helpTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        helpScrollPane.setBorder(null);

        helpTextArea.addHierarchyListener(e1 -> {
            Window window = SwingUtilities.getWindowAncestor(helpTextArea);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
            }
        });

        JOptionPane.showMessageDialog(
                FRAME_SERVERPACKCREATOR,
                helpScrollPane,
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
