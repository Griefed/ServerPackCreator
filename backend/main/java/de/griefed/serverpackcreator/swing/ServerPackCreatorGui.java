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

import de.griefed.serverpackcreator.ApplicationPlugins;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.swing.utilities.BackgroundPanel;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.UpdateChecker;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.utilities.misc.Generated;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
@Generated
public class ServerPackCreatorGui {

  private static final Logger LOG = LogManager.getLogger(ServerPackCreatorGui.class);

  private final ImageIcon ICON_SERVERPACKCREATOR_BANNER =
      new ImageIcon(
          Objects.requireNonNull(
              ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/banner.png")));
  private final Image ICON_SERVERPACKCREATOR =
      Toolkit.getDefaultToolkit()
          .getImage(
              Objects.requireNonNull(
                  ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/app.png")));
  private final Dimension DIMENSION_WINDOW = new Dimension(1050, 800);

  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final ServerPackCreatorSplash SERVERPACKCREATORSPLASH;

  private final LightTheme LIGHTTHEME = new LightTheme();
  private final DarkTheme DARKTHEME = new DarkTheme();

  private final MaterialLookAndFeel LAF_LIGHT = new MaterialLookAndFeel(LIGHTTHEME);
  private final MaterialLookAndFeel LAF_DARK = new MaterialLookAndFeel(DARKTHEME);

  private final BackgroundPanel BACKGROUNDPANEL;

  private final JFrame FRAME_SERVERPACKCREATOR;

  private final TabCreateServerPack TAB_CREATESERVERPACK;

  private final JTabbedPane TABBEDPANE;

  private final MainMenuBar MENUBAR;

  private BufferedImage bufferedImage;

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
   * @param injectedI18n Instance of {@link I18n} required for localized log messages.
   * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler} required to
   *     successfully and correctly create the server pack.
   * @param injectedServerPackHandler Instance of {@link ServerPackHandler} required for the
   *     generation of server packs.
   * @param injectedApplicationProperties Instance of {@link Properties} required for various
   *     different things.
   * @param injectedVersionMeta Instance of {@link VersionMeta} required for everything version
   *     related in the GUI.
   * @param injectedUtilities Instance of {@link Utilities}.
   * @param injectedUpdateChecker Instance of {@link UpdateChecker}.
   * @param injectedPluginManager Instance of {@link ApplicationPlugins}.
   * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
   * @param injectedServerPackCreatorSplash Instance of {@link ServerPackCreatorSplash}
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  public ServerPackCreatorGui(
      I18n injectedI18n,
      ConfigurationHandler injectedConfigurationHandler,
      ServerPackHandler injectedServerPackHandler,
      ApplicationProperties injectedApplicationProperties,
      VersionMeta injectedVersionMeta,
      Utilities injectedUtilities,
      UpdateChecker injectedUpdateChecker,
      ApplicationPlugins injectedPluginManager,
      ConfigUtilities injectedConfigUtilities,
      ServerPackCreatorSplash injectedServerPackCreatorSplash)
      throws IOException {

    this.SERVERPACKCREATORSPLASH = injectedServerPackCreatorSplash;
    this.SERVERPACKCREATORSPLASH.update(90);
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;

    try {
      bufferedImage =
          ImageIO.read(
              Objects.requireNonNull(
                  getClass()
                      .getResource(
                          "/de/griefed/resources/gui/tile" + new Random().nextInt(4) + ".jpg")));
    } catch (IOException ex) {
      LOG.error("Could not read image for tiling.", ex);
    }

    this.FRAME_SERVERPACKCREATOR =
        new JFrame(
            injectedI18n.getMessage("createserverpack.gui.createandshowgui")
                + " - "
                + APPLICATIONPROPERTIES.SERVERPACKCREATOR_VERSION());

    this.TAB_CREATESERVERPACK =
        new TabCreateServerPack(
            injectedI18n,
            injectedConfigurationHandler,
            injectedServerPackHandler,
            injectedVersionMeta,
            APPLICATIONPROPERTIES,
            FRAME_SERVERPACKCREATOR,
            injectedUtilities,
            injectedConfigUtilities,
            DARKTHEME,
            LIGHTTHEME);

    TabServerPackCreatorLog TAB_LOG_SERVERPACKCREATOR =
        new TabServerPackCreatorLog(
            injectedI18n.getMessage(
                "createserverpack.gui.tabbedpane.serverpackcreatorlog.tooltip"));

    TabAddonsHandlerLog TAB_LOG_ADDONSHANDLER =
        new TabAddonsHandlerLog(
            injectedI18n.getMessage("createserverpack.gui.tabbedpane.addonshandlerlog.tip"));

    this.BACKGROUNDPANEL = new BackgroundPanel(bufferedImage, BackgroundPanel.TILED, 0.0f, 0.0f);

    this.TABBEDPANE = new JTabbedPane(JTabbedPane.TOP);

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

    if (!injectedPluginManager.pluginsTabExtension().isEmpty()) {
      injectedPluginManager
          .pluginsTabExtension()
          .forEach(
              plugin ->
                  TABBEDPANE.addTab(
                      plugin.getTabTitle(),
                      plugin.getTabIcon(),
                      plugin.getTab(),
                      plugin.getTabTooltip()));
    } else {
      LOG.info("No TabbedPane addons to add.");
    }

    TABBEDPANE.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    MENUBAR =
        new MainMenuBar(
            injectedI18n,
            LIGHTTHEME,
            DARKTHEME,
            FRAME_SERVERPACKCREATOR,
            LAF_LIGHT,
            LAF_DARK,
            TAB_CREATESERVERPACK,
            TABBEDPANE,
            APPLICATIONPROPERTIES,
            injectedUpdateChecker,
            injectedUtilities);

    FRAME_SERVERPACKCREATOR.setJMenuBar(MENUBAR.createMenuBar());
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

            if (APPLICATIONPROPERTIES
                .getProperty("de.griefed.serverpackcreator.gui.darkmode")
                .equals("true")) {

              UIManager.setLookAndFeel(LAF_DARK);
              MaterialLookAndFeel.changeTheme(DARKTHEME);

            } else {

              UIManager.setLookAndFeel(LAF_LIGHT);
              MaterialLookAndFeel.changeTheme(LIGHTTHEME);
            }

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

    FRAME_SERVERPACKCREATOR.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    FRAME_SERVERPACKCREATOR.setContentPane(BACKGROUNDPANEL);
    FRAME_SERVERPACKCREATOR.setIconImage(ICON_SERVERPACKCREATOR);

    FRAME_SERVERPACKCREATOR.add(serverPackCreatorBanner, BorderLayout.PAGE_START);
    FRAME_SERVERPACKCREATOR.add(TABBEDPANE, BorderLayout.CENTER);

    FRAME_SERVERPACKCREATOR.setSize(DIMENSION_WINDOW);
    FRAME_SERVERPACKCREATOR.setPreferredSize(DIMENSION_WINDOW);
    FRAME_SERVERPACKCREATOR.setLocationRelativeTo(null);
    FRAME_SERVERPACKCREATOR.setResizable(true);

    FRAME_SERVERPACKCREATOR.pack();
    /*
     * I know this looks stupid. Why initialize the tree if it isn't even visible yet?
     * Because otherwise, when switching from light to dark-theme, the inset for tabs of the tabbed pane suddenly
     * changes, which looks ugly. Calling this does the same, but before the GUI is visible. Dirty hack? Maybe.
     * Does it work? Yeah.
     */
    SwingUtilities.updateComponentTreeUI(FRAME_SERVERPACKCREATOR);

    /*
     * This call needs to stay here, otherwise we have a transparent background in the tab-bar of
     * our tabbed pane, which looks kinda cool, but also stupid and hard to read.
     */
    TABBEDPANE.setOpaque(true);

    FRAME_SERVERPACKCREATOR.setVisible(true);

    TAB_CREATESERVERPACK.validateInputFields();
    TAB_CREATESERVERPACK.updatePanelTheme();
    MENUBAR.displayUpdateDialog();
  }
}
