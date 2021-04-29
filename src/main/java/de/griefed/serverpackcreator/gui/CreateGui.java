package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Configuration;
import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Objects;

public class CreateGui extends JPanel {
    private static final Logger appLogger = LogManager.getLogger(CreateGui.class);

    private final ImageIcon bannerIcon      = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/banner.png")));
    private final Image icon                = Toolkit.getDefaultToolkit().getImage(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/app.png")));
    private final Dimension windowDimension = new Dimension(800,860);

    private LocalizationManager localizationManager;
    private Configuration configuration;
    private CurseCreateModpack curseCreateModpack;

    public CreateGui(LocalizationManager injectedLocalizationManager, Configuration injectedConfiguration, CurseCreateModpack injectectedCurseCreateModpack) {
        super(new GridLayout(1, 1));

        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }

        if (injectedConfiguration == null) {
            this.curseCreateModpack = new CurseCreateModpack(localizationManager);
        } else {
            this.curseCreateModpack = injectectedCurseCreateModpack;
        }

        if (injectedConfiguration == null) {
            this.configuration = new Configuration(localizationManager, curseCreateModpack);
        } else {
            this.configuration = injectedConfiguration;
        }

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

        tabbedPane.addTab(localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.title"), null, new CreateServerPackTab(localizationManager, configuration, curseCreateModpack).createServerPackTab(), localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.createserverpack.tip"));
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        tabbedPane.addTab(localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"), null, new ServerPackCreatorLogTab(localizationManager).serverPackCreatorLogTab(), localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        tabbedPane.addTab(localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.title"), null, new ModloaderInstallerLogTab(localizationManager).modloaderInstallerLogTab(), localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.modloaderinstallerlog.tip"));
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        tabbedPane.addTab(localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.title"), null, new AboutTab(localizationManager).aboutTab(), localizationManager.getLocalizedString("createserverpack.gui.tabbedpane.about.tip"));
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

        //Add the tabbed pane to this panel.
        add(tabbedPane);

        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public void mainGUI() {
        SwingUtilities.invokeLater(() -> {
            //Bold fonts = true, else false
            UIManager.put("swing.boldMetal", true);

            try {
                if (new File("serverpackcreator.conf").exists()) {

                    File configFile = new File("serverpackcreator.conf");
                    Config secret = ConfigFactory.parseFile(configFile);

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

    private void createAndShowGUI() {

        JFrame frame = new JFrame(localizationManager.getLocalizedString("createserverpack.gui.createandshowgui"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setIconImage(icon);
        JLabel banner = new JLabel(bannerIcon);
        banner.setOpaque(false);

        frame.add(banner, BorderLayout.PAGE_START);
        frame.add(new CreateGui(localizationManager, configuration, curseCreateModpack), BorderLayout.CENTER);

        frame.setPreferredSize(windowDimension);
        frame.setMaximumSize(windowDimension);
        frame.setResizable(true);

        frame.pack();

        frame.setVisible(true);
    }
}