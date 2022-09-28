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

import de.griefed.serverpackcreator.ApplicationAddons;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.MigrationManager.MigrationMessage;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.swing.utilities.BackgroundPanel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import mdlaf.MaterialLookAndFeel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class creates and shows the GUI needed for running ServerPackCreator in....well...GUI mode.
 *
 * @author Griefed
 */
public final class ServerPackCreatorWindow extends JFrame {

  private static final Logger LOG = LogManager.getLogger(ServerPackCreatorWindow.class);
  private final ImageIcon ICON_SERVERPACKCREATOR_BANNER =
      new ImageIcon(
          Objects.requireNonNull(
              ServerPackCreatorWindow.class.getResource("/de/griefed/resources/gui/banner.png")));
  private final Image ICON_SERVERPACKCREATOR =
      Toolkit.getDefaultToolkit()
          .getImage(
              Objects.requireNonNull(
                  ServerPackCreatorWindow.class.getResource("/de/griefed/resources/gui/app.png")));
  private final ImageIcon ISSUE_ICON = new ImageIcon(ImageIO.read(Objects.requireNonNull(
          TabCreateServerPack.class.getResource("/de/griefed/resources/gui/issue.png")))
      .getScaledInstance(48, 48, Image.SCALE_SMOOTH));
  private final ImageIcon INFO_ICON = new ImageIcon(ImageIO.read(Objects.requireNonNull(
          TabCreateServerPack.class.getResource("/de/griefed/resources/gui/info.png")))
      .getScaledInstance(48, 48, Image.SCALE_SMOOTH));
  private final Dimension DIMENSION_WINDOW = new Dimension(1200, 800);
  private final I18n I18N;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final ServerPackCreatorSplash SERVERPACKCREATORSPLASH;
  private final LightTheme LIGHTTHEME = new LightTheme();
  private final DarkTheme DARKTHEME = new DarkTheme();
  private final MaterialLookAndFeel LAF_LIGHT = new MaterialLookAndFeel(LIGHTTHEME);
  private final MaterialLookAndFeel LAF_DARK = new MaterialLookAndFeel(DARKTHEME);
  private final BackgroundPanel BACKGROUNDPANEL;
  private final TabCreateServerPack TAB_CREATESERVERPACK;
  private final JTabbedPane TABBEDPANE;
  private final MainMenuBar MENUBAR;
  private final JScrollPane SCROLL_MIGRATION;
  private final boolean MIGRATIONS_MADE;

  /**
   * <strong>Constructor</strong>
   *
   * <p>Used for Dependency Injection.
   *
   * <p>Receives an instance of {@link I18n} or creates one if the received one is null. Required
   * for use of localization.
   *
   * <p>Receives an instance of {@link ConfigurationHandler} required to successfully and correctly
   * create the server pack.
   *
   * <p>Receives an instance of {@link ServerPackHandler} which is required to generate a server
   * pack.
   *
   * @param injectedI18n                    Instance of {@link I18n} required for localized log
   *                                        messages.
   * @param injectedConfigurationHandler    Instance of {@link ConfigurationHandler} required to
   *                                        successfully and correctly create the server pack.
   * @param injectedServerPackHandler       Instance of {@link ServerPackHandler} required for the
   *                                        generation of server packs.
   * @param injectedApplicationProperties   Instance of {@link Properties} required for various
   *                                        different things.
   * @param injectedVersionMeta             Instance of {@link VersionMeta} required for everything
   *                                        version related in the GUI.
   * @param injectedUtilities               Instance of {@link Utilities}.
   * @param injectedUpdateChecker           Instance of {@link UpdateChecker}.
   * @param injectedServerPackCreatorSplash Instance of {@link ServerPackCreatorSplash}.
   * @param injectedApplicationAddons       Instance of {@link ApplicationAddons}.
   * @param injectedConfigUtilities         Instance of {@link ConfigUtilities}.
   * @param migrationMessages               List of migration messages to display to the user.
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  public ServerPackCreatorWindow(
      I18n injectedI18n,
      ConfigurationHandler injectedConfigurationHandler,
      ServerPackHandler injectedServerPackHandler,
      ApplicationProperties injectedApplicationProperties,
      VersionMeta injectedVersionMeta,
      Utilities injectedUtilities,
      UpdateChecker injectedUpdateChecker,
      ServerPackCreatorSplash injectedServerPackCreatorSplash,
      ApplicationAddons injectedApplicationAddons,
      ConfigUtilities injectedConfigUtilities,
      List<MigrationMessage> migrationMessages)
      throws IOException {

    SERVERPACKCREATORSPLASH = injectedServerPackCreatorSplash;
    SERVERPACKCREATORSPLASH.update(90);
    APPLICATIONPROPERTIES = injectedApplicationProperties;
    I18N = injectedI18n;

    BufferedImage bufferedImage = ImageIO.read(
        Objects.requireNonNull(
            getClass()
                .getResource(
                    "/de/griefed/resources/gui/tile" + new Random().nextInt(4) + ".jpg")));

    setTitle(injectedI18n.getMessage("createserverpack.gui.createandshowgui")
        + " - "
        + APPLICATIONPROPERTIES.serverPackCreatorVersion());

    TAB_CREATESERVERPACK =
        new TabCreateServerPack(
            injectedI18n,
            injectedConfigurationHandler,
            injectedServerPackHandler,
            injectedVersionMeta,
            APPLICATIONPROPERTIES,
            this,
            injectedUtilities,
            DARKTHEME,
            LIGHTTHEME,
            injectedApplicationAddons,
            injectedConfigUtilities);

    TabServerPackCreatorLog TAB_LOG_SERVERPACKCREATOR =
        new TabServerPackCreatorLog(
            injectedI18n.getMessage(
                "createserverpack.gui.tabbedpane.serverpackcreatorlog.tooltip"),
            APPLICATIONPROPERTIES.logsDirectory());

    TabAddonsHandlerLog TAB_LOG_ADDONSHANDLER =
        new TabAddonsHandlerLog(
            injectedI18n.getMessage("createserverpack.gui.tabbedpane.addonshandlerlog.tip"),
            APPLICATIONPROPERTIES.logsDirectory());

    BACKGROUNDPANEL = new BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f);

    TABBEDPANE = new JTabbedPane(JTabbedPane.TOP);

    TABBEDPANE.addTab(
        injectedI18n.getMessage("createserverpack.gui.tabbedpane.createserverpack.title"),
        null,
        TAB_CREATESERVERPACK,
        injectedI18n.getMessage("createserverpack.gui.tabbedpane.createserverpack.tip"));

    TABBEDPANE.addTab(
        injectedI18n.getMessage("createserverpack.gui.tabbedpane.serverpackcreatorlog.title"),
        null,
        TAB_LOG_SERVERPACKCREATOR,
        injectedI18n.getMessage("createserverpack.gui.tabbedpane.serverpackcreatorlog.tip"));

    TABBEDPANE.addTab(
        injectedI18n.getMessage("createserverpack.gui.tabbedpane.addonshandlerlog.title"),
        null,
        TAB_LOG_ADDONSHANDLER);

    TABBEDPANE.setMnemonicAt(0, KeyEvent.VK_1);
    TABBEDPANE.setMnemonicAt(1, KeyEvent.VK_2);
    TABBEDPANE.setMnemonicAt(2, KeyEvent.VK_3);

    injectedApplicationAddons.addTabExtensionTabs(TABBEDPANE);

    TABBEDPANE.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    MIGRATIONS_MADE = migrationMessages.size() > 0;

    MENUBAR =
        new MainMenuBar(
            injectedI18n,
            LIGHTTHEME,
            DARKTHEME,
            this,
            LAF_LIGHT,
            LAF_DARK,
            TAB_CREATESERVERPACK,
            TABBEDPANE,
            APPLICATIONPROPERTIES,
            injectedUpdateChecker,
            injectedUtilities);

    setJMenuBar(MENUBAR.createMenuBar());

    StringBuilder messages = new StringBuilder();
    messages.append("<html>");
    for (MigrationMessage message : migrationMessages) {
      messages.append(message.get()).append("<br>");
    }
    messages.append("</html>");

    JLabel LABEL_MIGRATION = new JLabel(messages.toString().replace("\n","<br>"));
    SCROLL_MIGRATION = new JScrollPane(
        LABEL_MIGRATION,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    );
    SCROLL_MIGRATION.setMaximumSize(DIMENSION_WINDOW);
  }

  /**
   * Shows the GUI from the EDT by using SwingUtilities, and it's invokeLater method by calling
   * {@link #createAndShowGUI()}. Sets the font to bold, which may be overridden by the LookAndFeel
   * which gets automatically determined and depends on the OS ServerPackCreator is run on.
   *
   * @author Griefed
   */
  public void mainGUI() {
    SwingUtilities.invokeLater(
        () -> {
          try {

            if (APPLICATIONPROPERTIES.isDarkTheme()) {

              UIManager.setLookAndFeel(LAF_DARK);
              MaterialLookAndFeel.changeTheme(DARKTHEME);

            } else {

              UIManager.setLookAndFeel(LAF_LIGHT);
              MaterialLookAndFeel.changeTheme(LIGHTTHEME);

            }

            UIManager.put("Table.showVerticalLines", true);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.intercellSpacing", new Dimension(1, 1));

          } catch (UnsupportedLookAndFeelException ex) {
            LOG.error("Error: There was an error setting the look and feel.", ex);
          }

          SERVERPACKCREATORSPLASH.update(95);
          createAndShowGUI();
        });
  }

  /**
   * Creates the frame in which the banner, tabbed pane with all the tabs, icon and title are
   * displayed and shows it.
   *
   * @author Griefed
   */
  private void createAndShowGUI() {

    SERVERPACKCREATORSPLASH.close();

    JLabel serverPackCreatorBanner = new JLabel(ICON_SERVERPACKCREATOR_BANNER);
    serverPackCreatorBanner.setOpaque(false);

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setContentPane(BACKGROUNDPANEL);
    setIconImage(ICON_SERVERPACKCREATOR);

    add(serverPackCreatorBanner, BorderLayout.PAGE_START);
    add(TABBEDPANE, BorderLayout.CENTER);

    setSize(DIMENSION_WINDOW);
    setPreferredSize(DIMENSION_WINDOW);
    setLocationRelativeTo(null);
    setResizable(true);

    pack();
    /*
     * I know this looks stupid. Why initialize the tree if it isn't even visible yet?
     * Because otherwise, when switching from light to dark-theme, the inset for tabs of the tabbed pane suddenly
     * changes, which looks ugly. Calling this does the same, but before the GUI is visible. Dirty hack? Maybe.
     * Does it work? Yeah.
     */
    SwingUtilities.updateComponentTreeUI(this);

    /*
     * This call needs to stay here, otherwise we have a transparent background in the tab-bar of
     * our tabbed pane, which looks kinda cool, but also stupid and hard to read.
     */
    TABBEDPANE.setOpaque(true);

    setVisible(true);

    TAB_CREATESERVERPACK.validateInputFields();
    TAB_CREATESERVERPACK.updatePanelTheme();
    MENUBAR.displayUpdateDialog();
    displayMigrationMessages();
  }

  /**
   * If no Java is available, a message is displayed, warning the user that Javapath needs to be
   * defined for the modloader-server installation to work. If "Yes" is clicked, a filechooser will
   * open where the user can select their Java-executable/binary. If "No" is selected, the user is
   * warned about the consequences of not setting the Javapath.
   *
   * @return {@code true} if Java is available or was configured by the user.
   * @author Griefed
   */
  boolean checkJava() {
    if (!APPLICATIONPROPERTIES.javaAvailable()) {
      switch (JOptionPane.showConfirmDialog(
          this,
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.message"),
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.title"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          ISSUE_ICON)) {

        case 0:
          chooseJava();
          return true;

        case 1:
          JOptionPane.showMessageDialog(
              this,
              I18N.getMessage(
                  "createserverpack.gui.createserverpack.checkboxserver.message.message"),
              I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.message.title"),
              JOptionPane.ERROR_MESSAGE,
              ISSUE_ICON);

        default:
          return false;
      }
    } else {
      return true;
    }
  }

  /**
   * Opens a filechooser to select the Java-executable/binary.
   *
   * @author Griefed
   */
  void chooseJava() {
    JFileChooser javaChooser = new JFileChooser();

    if (new File(String.format("%s/bin/", System.getProperty("java.home")))
        .isDirectory()) {

      javaChooser.setCurrentDirectory(
          new File(String.format("%s/bin/", System.getProperty("java.home"))));

    } else {
      javaChooser.setCurrentDirectory(APPLICATIONPROPERTIES.homeDirectory());
    }

    javaChooser.setDialogTitle(I18N.getMessage("createserverpack.gui.buttonjavapath.tile"));
    javaChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    javaChooser.setAcceptAllFileFilterUsed(true);
    javaChooser.setMultiSelectionEnabled(false);
    javaChooser.setPreferredSize(new Dimension(750, 450));

    if (javaChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

      APPLICATIONPROPERTIES.setJavaPath(
          javaChooser.getSelectedFile().getPath());

      LOG.debug(
          "Set path to Java executable to: "
              + javaChooser.getSelectedFile().getPath());

    }
  }

  /**
   * Whether any migration were made.
   * @return {@code true} if migrations were made and therefor migration messages are available.
   * @author Griefed
   */
  boolean migrationMessagesAvailable() {
    return MIGRATIONS_MADE;
  }

  /**
   * Display the available migration messages.
   * @author Griefed
   */
  void displayMigrationMessages() {
    if (MIGRATIONS_MADE) {
      JOptionPane.showMessageDialog(
          null,
          SCROLL_MIGRATION,
          I18N.getMessage("migration.message.title"),
          JOptionPane.INFORMATION_MESSAGE,
          INFO_ICON
      );
    }
  }
}
