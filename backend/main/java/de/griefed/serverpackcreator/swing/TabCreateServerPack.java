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

import com.electronwill.nightconfig.core.CommentedConfig;
import de.griefed.larsonscanner.LarsonScanner;
import de.griefed.larsonscanner.LarsonScanner.ScannerConfig;
import de.griefed.serverpackcreator.ApplicationAddons;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.addons.swinggui.ExtensionConfigPanel;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.swing.utilities.CompoundIcon;
import de.griefed.serverpackcreator.swing.utilities.IconTextArea;
import de.griefed.serverpackcreator.swing.utilities.IconTextField;
import de.griefed.serverpackcreator.swing.utilities.RotatedIcon;
import de.griefed.serverpackcreator.swing.utilities.ScriptSettings;
import de.griefed.serverpackcreator.swing.utilities.SimpleDocumentListener;
import de.griefed.serverpackcreator.swing.utilities.TextIcon;
import de.griefed.serverpackcreator.utilities.ConfigUtilities;
import de.griefed.serverpackcreator.utilities.ReticulatingSplines;
import de.griefed.serverpackcreator.utilities.common.Utilities;
import de.griefed.serverpackcreator.versionmeta.VersionMeta;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class creates the tab which displays the labels, textfields, buttons and functions in order
 * to create a new server pack. Available are:<br> JLabels and JTextFields for modpackDir,
 * clientMods, copyDirs, javaPath, minecraftVersion, modLoader, modLoaderVersion<br> Checkboxes for
 * Include- serverInstallation, serverIcon, serverProperties, startScripts, ZIP-archive<br> Buttons
 * opening the file explorer and choosing: modpackDir, clientMods, copyDirs, javaPath,
 * loadConfigFromFile<br> A button for displaying an information windows which provides detailed
 * explanation of the important parts of the GUI.<br> The button start the generation of a new
 * server pack.<br> The label under the button to start the generation of a new server pack, which
 * displays the latest log entry of the serverpackcreator.log <em>during</em> the creation of a new
 * server pack.
 * <br>
 * If a configuration file is found during startup of ServerPackCreator, it is automatically loaded
 * into the GUI.
 *
 * @author Griefed
 */
public class TabCreateServerPack extends JPanel {

  private static final Logger LOG = LogManager.getLogger(TabCreateServerPack.class);
  private final JFrame FRAME_SERVERPACKCREATOR;
  private final ConfigurationHandler CONFIGURATIONHANDLER;
  private final I18n I18N;
  private final ServerPackHandler SERVERPACKHANDLER;
  private final VersionMeta VERSIONMETA;
  private final Utilities UTILITIES;
  private final ConfigUtilities CONFIG_UTILITIES;
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final DarkTheme DARKTHEME;
  private final LightTheme LIGHTTHEME;
  private final ReticulatingSplines RETICULATING_SPLINES = new ReticulatingSplines();
  private final StyledDocument SERVERPACKGENERATEDDOCUMENT = new DefaultStyledDocument();
  private final SimpleAttributeSet SERVERPACKGENERATEDATTRIBUTESET = new SimpleAttributeSet();
  private final JTextPane SERVERPACKGENERATEDTEXTPANE = new JTextPane(SERVERPACKGENERATEDDOCUMENT);
  private final StyledDocument LAZYMODEDOCUMENT = new DefaultStyledDocument();
  private final JTextPane LAZYMODETEXTPANE = new JTextPane(LAZYMODEDOCUMENT);
  private final int ERROR_ICON_SIZE = 18;
  private final BufferedImage ERROR_ICON_BASE =
      ImageIO.read(
          Objects.requireNonNull(
              TabCreateServerPack.class.getResource("/de/griefed/resources/gui/error.png")));
  private final ImageIcon ERROR_ICON_MODPACKDIRECTORY =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_SERVERPACK_SUFFIX =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_CLIENTSIDE_MODS =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_COPYDIRECTORIES =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_SERVERICON =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_SERVERPROPERTIES =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ISSUE_ICON = new ImageIcon(ImageIO.read(Objects.requireNonNull(
          TabCreateServerPack.class.getResource("/de/griefed/resources/gui/issue.png")))
      .getScaledInstance(48, 48, Image.SCALE_SMOOTH));
  private final ImageIcon INFO_ICON = new ImageIcon(ImageIO.read(Objects.requireNonNull(
          TabCreateServerPack.class.getResource("/de/griefed/resources/gui/info.png")))
      .getScaledInstance(48, 48, Image.SCALE_SMOOTH));
  private final Dimension CHOOSERDIMENSION = new Dimension(750, 450);
  private final JButton BUTTON_GENERATESERVERPACK = new JButton();
  private final JPanel CREATESERVERPACKPANEL = new JPanel();
  private final LarsonScanner STATUS_BAR = new LarsonScanner();
  private final Color C0FFEE = new Color(192, 255, 238);
  private final Color SWAMP_GREEN = new Color(50, 83, 88);
  private final Color DARK_GREY = new Color(49, 47, 47);
  private final ScannerConfig IDLE_CONFIG =
      new ScannerConfig(
          2,
          new short[]{25, 50, 75, 100, 150, 200, 255},
          (short) 50,
          (short) 50,
          (byte) 7,
          new float[]{0.4f, 1.0f},
          50.0f,
          5.0D,
          false,
          false,
          true,
          false,
          false,
          new Color[]{
              SWAMP_GREEN,
              SWAMP_GREEN,
              SWAMP_GREEN,
              SWAMP_GREEN,
              SWAMP_GREEN,
              SWAMP_GREEN,
              SWAMP_GREEN
          },
          DARK_GREY,
          DARK_GREY);
  private final ScannerConfig BUSY_CONFIG =
      new ScannerConfig(
          2,
          new short[]{25, 50, 75, 100, 150, 200, 255},
          (short) 100,
          (short) 25,
          (byte) 7,
          new float[]{0.4f, 1.0f},
          25.0f,
          5.0D,
          false,
          false,
          true,
          true,
          false,
          new Color[]{
              C0FFEE,
              C0FFEE,
              C0FFEE,
              C0FFEE,
              C0FFEE,
              C0FFEE,
              C0FFEE
          },
          DARK_GREY,
          DARK_GREY);
  private final MaterialTextPaneUI MATERIALTEXTPANEUI = new MaterialTextPaneUI();
  private final JComboBox<String> COMBOBOX_MINECRAFTVERSIONS = new JComboBox<>();
  private final JComboBox<String> COMBOBOX_MODLOADERS = new JComboBox<>();
  private final JComboBox<String> COMBOBOX_MODLOADER_VERSIONS = new JComboBox<>();
  private final DefaultComboBoxModel<String> NO_VERSIONS;
  private final DefaultComboBoxModel<String> LEGACY_FABRIC_VERSIONS;
  private final DefaultComboBoxModel<String> FABRIC_VERSIONS;
  private final DefaultComboBoxModel<String> QUILT_VERSIONS;
  private final IconTextArea TEXTAREA_CLIENTSIDEMODS = new IconTextArea("");
  private final IconTextArea TEXTAREA_JAVAARGS = new IconTextArea("");
  private final IconTextArea TEXTAREA_COPYDIRECTORIES = new IconTextArea("");
  private final IconTextField TEXTFIELD_MODPACKDIRECTORY = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERPACKSUFFIX = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERICONPATH = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERPROPERTIESPATH = new IconTextField("");
  private final File DIRECTORY_CHOOSER = new File(".");
  private final List<ExtensionConfigPanel> CONFIG_PANELS = new ArrayList<>();
  private final ScriptSettings SCRIPT_VARIABLES;
  private final JLabel REQUIRED_JAVA_VERSION = new JLabel("");
  private final JLabel STATUS_LABEL_LINE_0;
  private final JLabel STATUS_LABEL_LINE_1;
  private final JLabel STATUS_LABEL_LINE_2;
  private final JLabel STATUS_LABEL_LINE_3;
  private final JLabel STATUS_LABEL_LINE_4;
  private final JLabel STATUS_LABEL_LINE_5;
  private final JCheckBox CHECKBOX_SERVER;
  private final JCheckBox CHECKBOX_ICON;
  private final JCheckBox CHECKBOX_PROPERTIES;
  private final JCheckBox CHECKBOX_ZIP;

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
   * @param injectedI18n                   Instance of {@link I18n}.
   * @param injectedConfigurationHandler   Instance of {@link ConfigurationHandler}.
   * @param injectedServerPackHandler      Instance of {@link ServerPackHandler}.
   * @param injectedVersionMeta            Instance of {@link VersionMeta}.
   * @param injectedApplicationProperties  Instance of {@link Properties}.
   * @param injectedServerPackCreatorFrame Our parent frame which contains all of
   *                                       ServerPackCreator.
   * @param injectedUtilities              Instance of {@link Utilities}.
   * @param injectedDarkTheme              Instance of {@link DarkTheme}.
   * @param injectedLightTheme             Instance of {@link LightTheme}.
   * @param injectedApplicationAddons      Instance of {@link ApplicationAddons}.
   * @param injectedConfigUtilities        Instance of {@link ConfigUtilities}.
   * @throws IOException if the {@link VersionMeta} could not be instantiated.
   * @author Griefed
   */
  public TabCreateServerPack(
      I18n injectedI18n,
      ConfigurationHandler injectedConfigurationHandler,
      ServerPackHandler injectedServerPackHandler,
      VersionMeta injectedVersionMeta,
      ApplicationProperties injectedApplicationProperties,
      JFrame injectedServerPackCreatorFrame,
      Utilities injectedUtilities,
      DarkTheme injectedDarkTheme,
      LightTheme injectedLightTheme,
      ApplicationAddons injectedApplicationAddons,
      ConfigUtilities injectedConfigUtilities)
      throws IOException {

    DARKTHEME = injectedDarkTheme;
    LIGHTTHEME = injectedLightTheme;
    APPLICATIONPROPERTIES = injectedApplicationProperties;
    I18N = injectedI18n;
    VERSIONMETA = injectedVersionMeta;
    UTILITIES = injectedUtilities;
    CONFIGURATIONHANDLER = injectedConfigurationHandler;
    SERVERPACKHANDLER = injectedServerPackHandler;
    FRAME_SERVERPACKCREATOR = injectedServerPackCreatorFrame;
    CONFIG_UTILITIES = injectedConfigUtilities;

    SERVERPACKGENERATEDTEXTPANE.setOpaque(false);
    SERVERPACKGENERATEDTEXTPANE.setEditable(false);
    StyleConstants.setBold(SERVERPACKGENERATEDATTRIBUTESET, true);
    StyleConstants.setFontSize(SERVERPACKGENERATEDATTRIBUTESET, 14);
    SERVERPACKGENERATEDTEXTPANE.setCharacterAttributes(SERVERPACKGENERATEDATTRIBUTESET, true);
    StyleConstants.setAlignment(SERVERPACKGENERATEDATTRIBUTESET, StyleConstants.ALIGN_LEFT);
    try {
      SERVERPACKGENERATEDDOCUMENT.insertString(
          0,
          I18N.getMessage("createserverpack.gui.createserverpack.openfolder.browse") + "    ",
          SERVERPACKGENERATEDATTRIBUTESET);
    } catch (BadLocationException ex) {
      LOG.error("Error inserting text into aboutDocument.", ex);
    }

    LAZYMODETEXTPANE.setOpaque(false);
    LAZYMODETEXTPANE.setEditable(false);
    SimpleAttributeSet LAZYMODEATTRIBUTESET = new SimpleAttributeSet();
    StyleConstants.setBold(LAZYMODEATTRIBUTESET, true);
    StyleConstants.setFontSize(LAZYMODEATTRIBUTESET, 14);
    LAZYMODETEXTPANE.setCharacterAttributes(LAZYMODEATTRIBUTESET, true);
    StyleConstants.setAlignment(LAZYMODEATTRIBUTESET, StyleConstants.ALIGN_LEFT);
    try {
      LAZYMODEDOCUMENT.insertString(
          0,
          I18N.getMessage("configuration.log.warn.checkconfig.copydirs.lazymode0")
              + "\n\n"
              + I18N.getMessage("configuration.log.warn.checkconfig.copydirs.lazymode1")
              + "\n"
              + I18N.getMessage("configuration.log.warn.checkconfig.copydirs.lazymode2")
              + "\n"
              + I18N.getMessage("configuration.log.warn.checkconfig.copydirs.lazymode3")
              + "\n\n"
              + I18N.getMessage("configuration.log.warn.checkconfig.copydirs.lazymode0"),
          LAZYMODEATTRIBUTESET);
    } catch (BadLocationException ex) {
      LOG.error("Error inserting text into aboutDocument.", ex);
    }

    String[] NONE = new String[]{
        I18N.getMessage("createserverpack.gui.createserverpack.forge.none")};
    NO_VERSIONS = new DefaultComboBoxModel<>(NONE);

    LEGACY_FABRIC_VERSIONS = new DefaultComboBoxModel<>(
        VERSIONMETA.legacyFabric().loaderVersionsArrayDescending());
    FABRIC_VERSIONS =
        new DefaultComboBoxModel<>(VERSIONMETA.fabric().loaderVersionsArrayDescending());
    QUILT_VERSIONS =
        new DefaultComboBoxModel<>(VERSIONMETA.quilt().loaderVersionsArrayDescending());

    CREATESERVERPACKPANEL.setLayout(new GridBagLayout());

    Dimension folderButtonDimension = new Dimension(24, 24);
    ImageIcon folderIcon =
        new ImageIcon(
            Objects.requireNonNull(
                ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/folder.png")));

    Font notoSansDisplayRegularBold15 = new Font("Noto Sans Display Regular", Font.BOLD, 15);

    // ----------------------------------------------------------------LABELS AND TEXTFIELDS--------
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;

    // Label and textfield modpackDir
    JLabel labelModpackDir =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir"));
    labelModpackDir.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir.tip"));

    Insets TWENTY_TEN_ZERO_ZERO = new Insets(20, 10, 0, 0);
    gridBagConstraints.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelModpackDir, gridBagConstraints);

    TEXTFIELD_MODPACKDIRECTORY.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
    TEXTFIELD_MODPACKDIRECTORY.addDocumentListener(
        (SimpleDocumentListener) e -> validateInputFields());

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    Insets ZERO_TEN_ZERO_ZERO = new Insets(0, 10, 0, 0);
    gridBagConstraints.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(TEXTFIELD_MODPACKDIRECTORY, gridBagConstraints);

    JButton buttonModpackDirectory = new JButton();
    buttonModpackDirectory.setToolTipText(
        I18N.getMessage("createserverpack.gui.buttonmodpackdir"));
    buttonModpackDirectory.setContentAreaFilled(false);
    buttonModpackDirectory.setMultiClickThreshhold(1000);
    buttonModpackDirectory.setIcon(folderIcon);
    buttonModpackDirectory.setMinimumSize(folderButtonDimension);
    buttonModpackDirectory.setPreferredSize(folderButtonDimension);
    buttonModpackDirectory.setMaximumSize(folderButtonDimension);
    buttonModpackDirectory.addActionListener(this::selectModpackDirectory);
    addMouseListenerContentAreaFilledToButton(buttonModpackDirectory);

    gridBagConstraints.fill = GridBagConstraints.NONE;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;

    CREATESERVERPACKPANEL.add(buttonModpackDirectory, gridBagConstraints);

    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

    // Label and textfield server pack suffix
    JLabel labelServerPackSuffix =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix"));
    labelServerPackSuffix.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix.tip"));

    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new Insets(20, 45, 0, 0);

    CREATESERVERPACKPANEL.add(labelServerPackSuffix, gridBagConstraints);

    TEXTFIELD_SERVERPACKSUFFIX.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix.tip"));
    ERROR_ICON_SERVERPACK_SUFFIX.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textsuffix.error"));
    TEXTFIELD_SERVERPACKSUFFIX.addDocumentListener((SimpleDocumentListener) e -> validateInputFields());

    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.insets = new Insets(0, 45, 0, 0);

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPACKSUFFIX, gridBagConstraints);

    gridBagConstraints.gridwidth = 5;

    // Label and textfield clientMods
    JLabel labelClientMods =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods"));
    labelClientMods.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelClientMods, gridBagConstraints);

    TEXTAREA_CLIENTSIDEMODS.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));
    Font notoSansDisplayRegularPlain15 = new Font("Noto Sans Display Regular", Font.PLAIN, 15);
    TEXTAREA_CLIENTSIDEMODS.setFont(notoSansDisplayRegularPlain15);
    ERROR_ICON_CLIENTSIDE_MODS.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textclientmods.error"));
    TEXTAREA_CLIENTSIDEMODS.addDocumentListener((SimpleDocumentListener) e -> validateInputFields());
    JPanel clientsidemodsJpanel = new JPanel();
    clientsidemodsJpanel.setLayout(new GridBagLayout());
    GridBagConstraints textareaClientsidemodsJpanelConstraints = new GridBagConstraints();
    textareaClientsidemodsJpanelConstraints.anchor = GridBagConstraints.CENTER;
    textareaClientsidemodsJpanelConstraints.fill = GridBagConstraints.BOTH;
    textareaClientsidemodsJpanelConstraints.gridx = 0;
    textareaClientsidemodsJpanelConstraints.gridy = 0;
    textareaClientsidemodsJpanelConstraints.weighty = 1;
    textareaClientsidemodsJpanelConstraints.weightx = 1;

    JScrollPane scrollPanelClientsideMods =
        new JScrollPane(
            TEXTAREA_CLIENTSIDEMODS,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    clientsidemodsJpanel.add(
        scrollPanelClientsideMods, textareaClientsidemodsJpanelConstraints);
    Dimension client = new Dimension(100, 150);
    clientsidemodsJpanel.setSize(client);
    clientsidemodsJpanel.setPreferredSize(client);
    clientsidemodsJpanel.setMaximumSize(client);
    clientsidemodsJpanel.setMinimumSize(client);

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.insets = ZERO_TEN_ZERO_ZERO;
    gridBagConstraints.fill = GridBagConstraints.BOTH;

    CREATESERVERPACKPANEL.add(clientsidemodsJpanel, gridBagConstraints);

    // Label and textfield copyDirs
    JLabel labelCopyDirs =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs"));
    labelCopyDirs.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelCopyDirs, gridBagConstraints);

    TEXTAREA_COPYDIRECTORIES.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));
    TEXTAREA_COPYDIRECTORIES.setFont(notoSansDisplayRegularPlain15);
    ERROR_ICON_COPYDIRECTORIES.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textclientmods.error"));
    TEXTAREA_COPYDIRECTORIES.addDocumentListener((SimpleDocumentListener) e -> validateInputFields());
    JPanel copydirectoriesJpanel = new JPanel();
    copydirectoriesJpanel.setLayout(new GridBagLayout());
    GridBagConstraints textareaCopydirectoriesJpanelConstraints = new GridBagConstraints();
    textareaCopydirectoriesJpanelConstraints.anchor = GridBagConstraints.CENTER;
    textareaCopydirectoriesJpanelConstraints.fill = GridBagConstraints.BOTH;
    textareaCopydirectoriesJpanelConstraints.gridx = 0;
    textareaCopydirectoriesJpanelConstraints.gridy = 0;
    textareaCopydirectoriesJpanelConstraints.weighty = 1;
    textareaCopydirectoriesJpanelConstraints.weightx = 1;

    JScrollPane SCROLL_PANEL_COPYDIRECTORIES =
        new JScrollPane(
            TEXTAREA_COPYDIRECTORIES,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    copydirectoriesJpanel.add(
        SCROLL_PANEL_COPYDIRECTORIES, textareaCopydirectoriesJpanelConstraints);
    Dimension copy = new Dimension(100, 100);
    copydirectoriesJpanel.setSize(copy);
    copydirectoriesJpanel.setPreferredSize(copy);
    copydirectoriesJpanel.setMaximumSize(copy);
    copydirectoriesJpanel.setMinimumSize(copy);

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(copydirectoriesJpanel, gridBagConstraints);

    // Labels and textfields server-icon.png and server.properties paths
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridwidth = 5;

    JLabel labelServerIconPath =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labeliconpath"));
    labelServerIconPath.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labeliconpath.tip"));

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelServerIconPath, gridBagConstraints);

    TEXTFIELD_SERVERICONPATH.setText("");
    TEXTFIELD_SERVERICONPATH.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath"));
    ERROR_ICON_SERVERICON.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath.error"));
    TEXTFIELD_SERVERICONPATH.addDocumentListener(
        (SimpleDocumentListener) e -> validateInputFields());

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERICONPATH, gridBagConstraints);

    JLabel labelServerPropertiesPath =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelpropertiespath"));
    labelServerPropertiesPath.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelpropertiespath.tip"));

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelServerPropertiesPath, gridBagConstraints);

    TEXTFIELD_SERVERPROPERTIESPATH.setText("");
    TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath"));
    ERROR_ICON_SERVERPROPERTIES.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath.error"));
    TEXTFIELD_SERVERPROPERTIESPATH.addDocumentListener(
        (SimpleDocumentListener) e -> validateInputFields());

    gridBagConstraints.gridy = 9;
    gridBagConstraints.insets = new Insets(0, 10, 10, 0);

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPROPERTIESPATH, gridBagConstraints);

    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.fill = GridBagConstraints.NONE;
    gridBagConstraints.gridx = 0;
    Dimension combo = new Dimension(270, 30);

    // Label and combobox minecraftVersion
    gridBagConstraints.insets = new Insets(0, 10, 0, 5);
    JLabel labelMinecraftVersion =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelminecraft"));
    labelMinecraftVersion.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelminecraft.tip"));
    gridBagConstraints.gridy = 10;
    labelMinecraftVersion.setPreferredSize(combo);
    labelMinecraftVersion.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(labelMinecraftVersion, gridBagConstraints);

    if (APPLICATIONPROPERTIES.enableMinecraftPreReleases()) {
      COMBOBOX_MINECRAFTVERSIONS.setModel(
          new DefaultComboBoxModel<>(VERSIONMETA.minecraft().allVersionsArrayDescending()));
    } else {
      COMBOBOX_MINECRAFTVERSIONS.setModel(
          new DefaultComboBoxModel<>(VERSIONMETA.minecraft().releaseVersionsArrayDescending()));
    }
    if (COMBOBOX_MINECRAFTVERSIONS.getSelectedItem() == null) {
      COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(0);
    }
    COMBOBOX_MINECRAFTVERSIONS.addActionListener(this::actionEventComboBoxMinecraftVersion);
    gridBagConstraints.gridy = 11;
    COMBOBOX_MINECRAFTVERSIONS.setPreferredSize(combo);
    COMBOBOX_MINECRAFTVERSIONS.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(COMBOBOX_MINECRAFTVERSIONS, gridBagConstraints);

    // Label and combobox buttons Modloader
    gridBagConstraints.insets = new Insets(0, 300, 0, 5);
    JLabel labelModloader =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodloader"));
    labelModloader.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodloader.tip"));
    gridBagConstraints.gridy = 10;
    labelModloader.setPreferredSize(combo);
    labelModloader.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(labelModloader, gridBagConstraints);

    COMBOBOX_MODLOADERS.setModel(
        new DefaultComboBoxModel<>(APPLICATIONPROPERTIES.SUPPORTED_MODLOADERS()));
    if (COMBOBOX_MODLOADERS.getSelectedItem() == null) {
      COMBOBOX_MODLOADERS.setSelectedIndex(0);
    }
    COMBOBOX_MODLOADERS.addActionListener(this::actionEventComboBoxModloaders);
    gridBagConstraints.gridy = 11;
    COMBOBOX_MODLOADERS.setPreferredSize(combo);
    COMBOBOX_MODLOADERS.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(COMBOBOX_MODLOADERS, gridBagConstraints);

    // Label and combobox modloaderVersion
    gridBagConstraints.insets = new Insets(0, 590, 0, 5);
    JLabel labelModloaderVersion =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodloaderversion"));
    labelModloaderVersion.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));
    gridBagConstraints.gridy = 10;
    labelModloaderVersion.setPreferredSize(combo);
    labelModloaderVersion.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(labelModloaderVersion, gridBagConstraints);

    COMBOBOX_MODLOADER_VERSIONS.setModel(FABRIC_VERSIONS);
    COMBOBOX_MODLOADER_VERSIONS.setSelectedIndex(0);
    COMBOBOX_MODLOADER_VERSIONS.setVisible(true);
    gridBagConstraints.gridy = 11;
    COMBOBOX_MODLOADER_VERSIONS.setPreferredSize(combo);
    COMBOBOX_MODLOADER_VERSIONS.setMaximumSize(combo);
    CREATESERVERPACKPANEL.add(COMBOBOX_MODLOADER_VERSIONS, gridBagConstraints);

    gridBagConstraints.insets = new Insets(0, 880, 0, 5);
    Dimension java = new Dimension(160, 30);
    JLabel labelRequiredJavaVersion =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.minecraft.java"));
    labelRequiredJavaVersion.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.minecraft.java.tooltip"));
    gridBagConstraints.gridy = 10;
    labelRequiredJavaVersion.setPreferredSize(java);
    labelRequiredJavaVersion.setMaximumSize(java);
    CREATESERVERPACKPANEL.add(labelRequiredJavaVersion, gridBagConstraints);

    REQUIRED_JAVA_VERSION.setPreferredSize(java);
    REQUIRED_JAVA_VERSION.setMaximumSize(java);
    Font requiredJavaFont = new Font("Noto Sans Display Regular", Font.BOLD, 20);
    REQUIRED_JAVA_VERSION.setFont(requiredJavaFont);
    gridBagConstraints.gridy = 11;
    CREATESERVERPACKPANEL.add(REQUIRED_JAVA_VERSION, gridBagConstraints);

    // ----------------------------------------------------------------CHECKBOXES-------------------

    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.fill = GridBagConstraints.NONE;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.insets = new Insets(10, 5, 5, 5);
    Dimension check = new Dimension(270, 40);

    // Checkbox installServer
    CHECKBOX_SERVER =
        new JCheckBox(
            I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver"), true);
    CHECKBOX_SERVER.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.tip"));
    CHECKBOX_SERVER.addActionListener(this::actionEventCheckBoxServer);
    CHECKBOX_SERVER.setSize(check);
    CHECKBOX_SERVER.setMinimumSize(check);
    CHECKBOX_SERVER.setPreferredSize(check);
    CHECKBOX_SERVER.setMaximumSize(check);

    CREATESERVERPACKPANEL.add(CHECKBOX_SERVER, gridBagConstraints);

    // Checkbox copyIcon
    CHECKBOX_ICON =
        new JCheckBox(I18N.getMessage("createserverpack.gui.createserverpack.checkboxicon"), true);
    CHECKBOX_ICON.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxicon.tip"));
    CHECKBOX_ICON.setSize(check);
    CHECKBOX_ICON.setMinimumSize(check);
    CHECKBOX_ICON.setPreferredSize(check);
    CHECKBOX_ICON.setMaximumSize(check);

    gridBagConstraints.insets = new Insets(5, 275, 5, 5);
    CREATESERVERPACKPANEL.add(CHECKBOX_ICON, gridBagConstraints);

    // Checkbox copyProperties
    CHECKBOX_PROPERTIES =
        new JCheckBox(
            I18N.getMessage("createserverpack.gui.createserverpack.checkboxproperties"), true);
    CHECKBOX_PROPERTIES.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxproperties.tip"));
    CHECKBOX_PROPERTIES.setSize(check);
    CHECKBOX_PROPERTIES.setMinimumSize(check);
    CHECKBOX_PROPERTIES.setPreferredSize(check);
    CHECKBOX_PROPERTIES.setMaximumSize(check);

    gridBagConstraints.insets = new Insets(5, 545, 5, 5);
    CREATESERVERPACKPANEL.add(CHECKBOX_PROPERTIES, gridBagConstraints);

    // Checkbox createZIP
    CHECKBOX_ZIP =
        new JCheckBox(I18N.getMessage("createserverpack.gui.createserverpack.checkboxzip"), true);
    CHECKBOX_ZIP.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxzip.tip"));
    CHECKBOX_ZIP.setSize(check);
    CHECKBOX_ZIP.setMinimumSize(check);
    CHECKBOX_ZIP.setPreferredSize(check);
    CHECKBOX_ZIP.setMaximumSize(check);

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.insets = new Insets(5, 815, 5, 5);
    CREATESERVERPACKPANEL.add(CHECKBOX_ZIP, gridBagConstraints);

    JLabel labelJavaArgs =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.javaargs"));
    labelJavaArgs.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.javaargs.tip"));

    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.insets = new Insets(0, 10, 0, 0);

    CREATESERVERPACKPANEL.add(labelJavaArgs, gridBagConstraints);

    TEXTAREA_JAVAARGS.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.javaargs.tip"));
    TEXTAREA_JAVAARGS.setFont(notoSansDisplayRegularPlain15);
    JPanel javaargsJpanel = new JPanel();
    javaargsJpanel.setLayout(new GridBagLayout());
    GridBagConstraints textareaJavaargsJpanelConstraints = new GridBagConstraints();
    textareaJavaargsJpanelConstraints.anchor = GridBagConstraints.CENTER;
    textareaJavaargsJpanelConstraints.fill = GridBagConstraints.BOTH;
    textareaJavaargsJpanelConstraints.gridx = 0;
    textareaJavaargsJpanelConstraints.gridy = 0;
    textareaJavaargsJpanelConstraints.weighty = 1;
    textareaJavaargsJpanelConstraints.weightx = 1;

    JScrollPane SCROLL_PANEL_JAVAARGS =
        new JScrollPane(
            TEXTAREA_JAVAARGS,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    javaargsJpanel.add(SCROLL_PANEL_JAVAARGS, textareaJavaargsJpanelConstraints);
    javaargsJpanel.setSize(100, 100);
    javaargsJpanel.setPreferredSize(new Dimension(100, 100));
    javaargsJpanel.setMaximumSize(new Dimension(100, 100));
    javaargsJpanel.setMinimumSize(new Dimension(100, 100));

    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(javaargsJpanel, gridBagConstraints);

    // ------------------------------------------------------------------------------BUTTONS--------

    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.fill = GridBagConstraints.NONE;
    gridBagConstraints.insets = new Insets(0, 10, 0, 10);
    gridBagConstraints.weightx = 0;
    gridBagConstraints.weighty = 0;

    JButton buttonClientsidemods = new JButton();
    buttonClientsidemods.setToolTipText(I18N.getMessage("createserverpack.gui.buttonclientmods"));
    buttonClientsidemods.setContentAreaFilled(false);
    buttonClientsidemods.setMultiClickThreshhold(1000);
    buttonClientsidemods.setIcon(folderIcon);
    buttonClientsidemods.setMinimumSize(folderButtonDimension);
    buttonClientsidemods.setPreferredSize(folderButtonDimension);
    buttonClientsidemods.setMaximumSize(folderButtonDimension);
    buttonClientsidemods.addActionListener(this::selectClientMods);
    addMouseListenerContentAreaFilledToButton(buttonClientsidemods);

    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;

    CREATESERVERPACKPANEL.add(buttonClientsidemods, gridBagConstraints);

    JButton buttonCopydirectories = new JButton();
    buttonCopydirectories.setToolTipText(I18N.getMessage("createserverpack.gui.buttoncopydirs"));
    buttonCopydirectories.setContentAreaFilled(false);
    buttonCopydirectories.setIcon(folderIcon);
    buttonCopydirectories.setMultiClickThreshhold(1000);
    buttonCopydirectories.setMinimumSize(folderButtonDimension);
    buttonCopydirectories.setPreferredSize(folderButtonDimension);
    buttonCopydirectories.setMaximumSize(folderButtonDimension);
    buttonCopydirectories.addActionListener(this::selectCopyDirs);
    addMouseListenerContentAreaFilledToButton(buttonCopydirectories);

    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 5;

    CREATESERVERPACKPANEL.add(buttonCopydirectories, gridBagConstraints);

    JButton buttonServericon = new JButton();
    buttonServericon.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.icon"));
    buttonServericon.setContentAreaFilled(false);
    buttonServericon.setIcon(folderIcon);
    buttonServericon.setMultiClickThreshhold(1000);

    buttonServericon.setMinimumSize(folderButtonDimension);
    buttonServericon.setPreferredSize(folderButtonDimension);
    buttonServericon.setMaximumSize(folderButtonDimension);
    buttonServericon.addActionListener(this::selectServerIcon);
    addMouseListenerContentAreaFilledToButton(buttonServericon);
    gridBagConstraints.fill = GridBagConstraints.NONE;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 0;
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 7;

    CREATESERVERPACKPANEL.add(buttonServericon, gridBagConstraints);

    JButton buttonServerproperties = new JButton();
    buttonServerproperties.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.properties"));
    buttonServerproperties.setContentAreaFilled(false);
    buttonServerproperties.setIcon(folderIcon);
    buttonServerproperties.setMultiClickThreshhold(1000);
    buttonServerproperties.setMinimumSize(folderButtonDimension);
    buttonServerproperties.setPreferredSize(folderButtonDimension);
    buttonServerproperties.setMaximumSize(folderButtonDimension);
    buttonServerproperties.addActionListener(this::selectServerProperties);
    addMouseListenerContentAreaFilledToButton(buttonServerproperties);

    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 9;

    CREATESERVERPACKPANEL.add(buttonServerproperties, gridBagConstraints);

    JButton buttonAikarsFlags = new JButton();
    buttonAikarsFlags.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.properties"));
    buttonAikarsFlags.setContentAreaFilled(false);
    buttonAikarsFlags.setIcon(
        new RotatedIcon(
            new TextIcon(
                buttonAikarsFlags,
                I18N.getMessage("createserverpack.gui.createserverpack.javaargs.aikar"),
                TextIcon.Layout.HORIZONTAL),
            RotatedIcon.Rotate.UP));
    buttonAikarsFlags.setMultiClickThreshhold(1000);
    buttonAikarsFlags.setMargin(new Insets(20, 20, 20, 20));
    buttonAikarsFlags.addActionListener(this::setAikarsFlags);
    addMouseListenerContentAreaFilledToButton(buttonAikarsFlags);

    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 16;

    CREATESERVERPACKPANEL.add(buttonAikarsFlags, gridBagConstraints);

    // --------------------------------------------------------SCRIPT VARIABLES---------------------

    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.gridheight = 1;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.insets = new Insets(10, 10, 10, 0);

    JLabel scriptSettingsLabel =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.scriptsettings.label"));
    scriptSettingsLabel.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.scriptsettings.label.tooltip"));
    CREATESERVERPACKPANEL.add(scriptSettingsLabel, gridBagConstraints);

    SCRIPT_VARIABLES = new ScriptSettings(I18N);
    JScrollPane tableScrollPane = new JScrollPane(SCRIPT_VARIABLES);
    tableScrollPane.setPreferredSize(new Dimension(700, 300));
    tableScrollPane.setMaximumSize(new Dimension(700, 300));
    gridBagConstraints.gridy = 18;
    gridBagConstraints.insets = new Insets(0, 10, 20, 0);
    CREATESERVERPACKPANEL.add(tableScrollPane, gridBagConstraints);

    // --------------------------------------------------------CONFIGPANE EXTENSIONS----------------

    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.insets = new Insets(10, 10, 10, 10);
    gridBagConstraints.gridwidth = 6;
    AtomicInteger yPos = new AtomicInteger(gridBagConstraints.gridy);
    if (!injectedApplicationAddons.configPanelExtensions().isEmpty()) {
      CONFIG_PANELS.clear();
      CONFIG_PANELS.addAll(injectedApplicationAddons.getConfigPanels(this));
      CONFIG_PANELS
          .forEach(
              extensionConfigPanel -> {
                gridBagConstraints.gridy = yPos.addAndGet(1);
                CREATESERVERPACKPANEL.add(extensionConfigPanel, gridBagConstraints);
              });
    }

    // ---------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------

    gridBagConstraints.insets = new Insets(5, 10, 5, 10);

    BufferedImage GENERATE =
        ImageIO.read(
            Objects.requireNonNull(
                ServerPackCreatorGui.class.getResource(
                    "/de/griefed/resources/gui/start_generation.png")));
    Dimension dimension = new Dimension(200, 70);
    BUTTON_GENERATESERVERPACK.setIcon(
        new CompoundIcon(
            CompoundIcon.Axis.X_AXIS,
            12,
            new ImageIcon(GENERATE.getScaledInstance(32, 32, Image.SCALE_SMOOTH)),
            new TextIcon(
                BUTTON_GENERATESERVERPACK,
                I18N.getMessage("createserverpack.gui.buttongenerateserverpack"))));
    BUTTON_GENERATESERVERPACK.addActionListener(this::generateServerpack);
    BUTTON_GENERATESERVERPACK.setMultiClickThreshhold(1000);
    BUTTON_GENERATESERVERPACK.setToolTipText(
        I18N.getMessage("createserverpack.gui.buttongenerateserverpack.tip"));
    BUTTON_GENERATESERVERPACK.setSize(dimension);
    BUTTON_GENERATESERVERPACK.setMinimumSize(dimension);
    BUTTON_GENERATESERVERPACK.setPreferredSize(dimension);
    BUTTON_GENERATESERVERPACK.setMaximumSize(dimension);

    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy += 1;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.gridheight = 1;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.weighty = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.fill = GridBagConstraints.NONE;

    CREATESERVERPACKPANEL.add(BUTTON_GENERATESERVERPACK, gridBagConstraints);

    JButton buttonServerPacks = new JButton();
    buttonServerPacks.setIcon(
        new CompoundIcon(
            CompoundIcon.Axis.X_AXIS,
            8,
            folderIcon,
            new TextIcon(
                buttonServerPacks, I18N.getMessage("createserverpack.gui.buttonserverpacks"))));
    buttonServerPacks.addActionListener(this::openServerPacksFolder);
    buttonServerPacks.setMultiClickThreshhold(1000);
    buttonServerPacks.setToolTipText(
        I18N.getMessage("createserverpack.gui.buttonserverpacks.tip"));
    buttonServerPacks.setSize(dimension);
    buttonServerPacks.setMinimumSize(dimension);
    buttonServerPacks.setPreferredSize(dimension);
    buttonServerPacks.setMaximumSize(dimension);

    gridBagConstraints.insets = new Insets(85, 10, 5, 10);
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

    CREATESERVERPACKPANEL.add(buttonServerPacks, gridBagConstraints);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
    statusPanel.setAlignmentX(0.0F);

    STATUS_LABEL_LINE_0 = new JLabel("..." + RETICULATING_SPLINES.reticulate() + "   ");
    STATUS_LABEL_LINE_1 = new JLabel("..." + RETICULATING_SPLINES.reticulate() + "   ");
    STATUS_LABEL_LINE_2 = new JLabel("..." + RETICULATING_SPLINES.reticulate() + "   ");
    STATUS_LABEL_LINE_3 = new JLabel("..." + RETICULATING_SPLINES.reticulate() + "   ");
    STATUS_LABEL_LINE_4 = new JLabel("..." + RETICULATING_SPLINES.reticulate() + "   ");
    STATUS_LABEL_LINE_5 =
        new JLabel(I18N.getMessage("createserverpack.gui.buttongenerateserverpack.ready") + "   ");

    // Make sure all labels start on the left and extend to the right
    STATUS_LABEL_LINE_0.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_1.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_2.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_3.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_4.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_5.setHorizontalAlignment(JLabel.LEFT);

    // Set the preferred size of the labels, so they do not resize when long texts are set later on
    Dimension labelDimension = new Dimension(700, 30);
    STATUS_LABEL_LINE_0.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_1.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_2.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_3.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_4.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_5.setPreferredSize(labelDimension);

    STATUS_LABEL_LINE_0.setFont(notoSansDisplayRegularBold15);
    STATUS_LABEL_LINE_1.setFont(notoSansDisplayRegularBold15);
    STATUS_LABEL_LINE_2.setFont(notoSansDisplayRegularBold15);
    STATUS_LABEL_LINE_3.setFont(notoSansDisplayRegularBold15);
    STATUS_LABEL_LINE_4.setFont(notoSansDisplayRegularBold15);
    STATUS_LABEL_LINE_5.setFont(notoSansDisplayRegularBold15);

    updatePanelTheme();

    statusPanel.add(STATUS_LABEL_LINE_0);
    statusPanel.add(STATUS_LABEL_LINE_1);
    statusPanel.add(STATUS_LABEL_LINE_2);
    statusPanel.add(STATUS_LABEL_LINE_3);
    statusPanel.add(STATUS_LABEL_LINE_4);
    statusPanel.add(STATUS_LABEL_LINE_5);
    statusPanel.setPreferredSize(new Dimension(700, 140));

    gridBagConstraints.insets = new Insets(15, 220, 5, 10);
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 6;
    gridBagConstraints.gridheight = 1;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.weighty = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    CREATESERVERPACKPANEL.add(statusPanel, gridBagConstraints);

    gridBagConstraints.insets = new Insets(0, 0, 0, 0);
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy += 1;
    gridBagConstraints.gridwidth = 6;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.weighty = 1;
    gridBagConstraints.anchor = GridBagConstraints.SOUTH;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

    STATUS_BAR.setPreferredSize(new Dimension(700, 40));
    CREATESERVERPACKPANEL.add(STATUS_BAR, gridBagConstraints);

    STATUS_BAR.loadConfig(IDLE_CONFIG);
    STATUS_BAR.play();

    // --------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
    gridBagConstraints.fill = GridBagConstraints.NONE;

    JScrollPane tabCreateserverpacktabScrollPanel = new JScrollPane(CREATESERVERPACKPANEL);
    tabCreateserverpacktabScrollPanel.getVerticalScrollBar().setUnitIncrement(16);

    setLayout(new BorderLayout());
    add(tabCreateserverpacktabScrollPanel, BorderLayout.CENTER);

    loadConfig(new File("serverpackcreator.conf"));
  }

  /**
   * Update the status labels with the current themes font-color and alpha.
   *
   * @author Griefed
   */
  protected void updatePanelTheme() {
    STATUS_LABEL_LINE_0.setForeground(
        new Color(
            getThemeTextColor().getRed(),
            getThemeTextColor().getGreen(),
            getThemeTextColor().getBlue(),
            20));
    STATUS_LABEL_LINE_1.setForeground(
        new Color(
            getThemeTextColor().getRed(),
            getThemeTextColor().getGreen(),
            getThemeTextColor().getBlue(),
            50));
    STATUS_LABEL_LINE_2.setForeground(
        new Color(
            getThemeTextColor().getRed(),
            getThemeTextColor().getGreen(),
            getThemeTextColor().getBlue(),
            100));
    STATUS_LABEL_LINE_3.setForeground(
        new Color(
            getThemeTextColor().getRed(),
            getThemeTextColor().getGreen(),
            getThemeTextColor().getBlue(),
            150));
    STATUS_LABEL_LINE_4.setForeground(
        new Color(
            getThemeTextColor().getRed(),
            getThemeTextColor().getGreen(),
            getThemeTextColor().getBlue(),
            200));

    IDLE_CONFIG.setScannerBackgroundColour(getBackground());
    IDLE_CONFIG.setEyeBackgroundColour(getBackground());
    BUSY_CONFIG.setScannerBackgroundColour(getBackground());
    BUSY_CONFIG.setEyeBackgroundColour(getBackground());

    STATUS_BAR.setBackground(getBackground());
    STATUS_BAR.setEyeBackground(getBackground());

    SCRIPT_VARIABLES.setShowGrid(true);
    SCRIPT_VARIABLES.setShowHorizontalLines(true);
    SCRIPT_VARIABLES.setShowVerticalLines(true);

    if (APPLICATIONPROPERTIES.isDarkTheme()) {
      SCRIPT_VARIABLES.setGridColor(C0FFEE);
    } else {
      SCRIPT_VARIABLES.setGridColor(SWAMP_GREEN);
    }
  }

  /**
   * Update the labels in the status panel.
   *
   * @param text The text to update the status with.
   * @author Griefed
   */
  private void updateStatus(String text) {
    this.STATUS_LABEL_LINE_0.setText(STATUS_LABEL_LINE_1.getText() + "   ");
    this.STATUS_LABEL_LINE_1.setText(STATUS_LABEL_LINE_2.getText() + "   ");
    this.STATUS_LABEL_LINE_2.setText(STATUS_LABEL_LINE_3.getText() + "   ");
    this.STATUS_LABEL_LINE_3.setText(STATUS_LABEL_LINE_4.getText() + "   ");
    this.STATUS_LABEL_LINE_4.setText(STATUS_LABEL_LINE_5.getText() + "   ");
    this.STATUS_LABEL_LINE_5.setText(text + "       ");
  }

  /**
   * Upon button-press, open the folder containing generated server packs in the users
   * file-explorer.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  protected void openServerPacksFolder(ActionEvent actionEvent) {
    UTILITIES.FileUtils().openFolder(APPLICATIONPROPERTIES.getDirectoryServerPacks());
  }

  /**
   * Validate all text-based input fields.
   *
   * @author Griefed
   */
  public void validateInputFields() {
    validateModpackDir();

    validateSuffix();

    validateClientMods();

    validateCopyDirs();

    validateServerIcon();

    validateServerProperties();
  }

  /**
   * Validate the input field for modpack directory.
   *
   * @author Griefed
   */
  private void validateModpackDir() {
    SwingUtilities.invokeLater(() -> {

      List<String> errorsTEXTFIELD_MODPACKDIRECTORY = new ArrayList<>();
      if (CONFIGURATIONHANDLER.checkModpackDir(
          getModpackDirectory(), errorsTEXTFIELD_MODPACKDIRECTORY)) {

        TEXTFIELD_MODPACKDIRECTORY.setIcon(null);
        TEXTFIELD_MODPACKDIRECTORY.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir.tip"));

        TEXTFIELD_MODPACKDIRECTORY.setForeground(getThemeTextColor());

      } else {

        TEXTFIELD_MODPACKDIRECTORY.setForeground(getThemeErrorColor());
        TEXTFIELD_MODPACKDIRECTORY.setIcon(ERROR_ICON_MODPACKDIRECTORY);
        ERROR_ICON_MODPACKDIRECTORY.setDescription(
            String.join(",", errorsTEXTFIELD_MODPACKDIRECTORY));
        TEXTFIELD_MODPACKDIRECTORY.setToolTipText(String.join(",", errorsTEXTFIELD_MODPACKDIRECTORY));
      }
    });
  }

  /**
   * Validate the input field for server pack suffix
   *
   * @author Griefed
   */
  private void validateSuffix() {
    SwingUtilities.invokeLater(() -> {
      if (UTILITIES.StringUtils().checkForIllegalCharacters(TEXTFIELD_SERVERPACKSUFFIX.getText())) {

        TEXTFIELD_SERVERPACKSUFFIX.setIcon(null);
        TEXTFIELD_SERVERPACKSUFFIX.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix.tip"));

        TEXTFIELD_SERVERPACKSUFFIX.setForeground(getThemeTextColor());

      } else {

        TEXTFIELD_SERVERPACKSUFFIX.setForeground(getThemeErrorColor());
        TEXTFIELD_SERVERPACKSUFFIX.setIcon(ERROR_ICON_SERVERPACK_SUFFIX);
        TEXTFIELD_SERVERPACKSUFFIX.setToolTipText(ERROR_ICON_SERVERPACK_SUFFIX.getDescription());
      }
    });
  }

  /**
   * Validate the input field for client mods.
   *
   * @author Griefed
   */
  private void validateClientMods() {
    SwingUtilities.invokeLater(() -> {
      if (!getClientsideMods().matches("^.*,\\s*\\\\*$")) {

        TEXTAREA_CLIENTSIDEMODS.setIcon(null);
        TEXTAREA_CLIENTSIDEMODS.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));

        TEXTAREA_CLIENTSIDEMODS.setForeground(getThemeTextColor());

      } else {

        TEXTAREA_CLIENTSIDEMODS.setForeground(getThemeErrorColor());
        TEXTAREA_CLIENTSIDEMODS.setIcon(ERROR_ICON_CLIENTSIDE_MODS);
        TEXTAREA_CLIENTSIDEMODS.setToolTipText(ERROR_ICON_CLIENTSIDE_MODS.getDescription());
      }
    });
  }

  /**
   * Validate the input field for copy directories.
   *
   * @author Griefed
   */
  private void validateCopyDirs() {
    SwingUtilities.invokeLater(() -> {
      List<String> errors = new ArrayList<>();

      if (!getCopyDirectories().matches("^.*,\\s*\\\\*$")
          && CONFIGURATIONHANDLER.checkCopyDirs(
          getCopyDirectoriesList(),
          getModpackDirectory(),
          errors)) {

        TEXTAREA_COPYDIRECTORIES.setIcon(null);
        TEXTAREA_COPYDIRECTORIES.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));

        TEXTAREA_COPYDIRECTORIES.setForeground(getThemeTextColor());

      } else {

        TEXTAREA_COPYDIRECTORIES.setForeground(getThemeErrorColor());
        TEXTAREA_COPYDIRECTORIES.setIcon(ERROR_ICON_COPYDIRECTORIES);
        ERROR_ICON_COPYDIRECTORIES.setDescription(
            String.join(",", errors));
        TEXTAREA_COPYDIRECTORIES.setToolTipText(String.join(",", errors));
      }
    });
  }

  /**
   * Validate the input field for server icon.
   *
   * @author Griefed
   */
  private void validateServerIcon() {
    SwingUtilities.invokeLater(() -> {
      if (CONFIGURATIONHANDLER.checkIconAndProperties(getServerIconPath())) {

        TEXTFIELD_SERVERICONPATH.setIcon(null);
        TEXTFIELD_SERVERICONPATH.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath"));

        TEXTFIELD_SERVERICONPATH.setForeground(getThemeTextColor());

      } else {

        TEXTFIELD_SERVERICONPATH.setForeground(getThemeErrorColor());
        TEXTFIELD_SERVERICONPATH.setIcon(ERROR_ICON_SERVERICON);
        TEXTFIELD_SERVERICONPATH.setToolTipText(ERROR_ICON_SERVERICON.getDescription());
      }
    });
  }

  /**
   * Validate the inputfield for server properties.
   *
   * @author Griefed
   */
  private void validateServerProperties() {
    SwingUtilities.invokeLater(() -> {
      if (CONFIGURATIONHANDLER.checkIconAndProperties(getServerPropertiesPath())) {
        TEXTFIELD_SERVERPROPERTIESPATH.setIcon(null);
        TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath"));

        TEXTFIELD_SERVERPROPERTIESPATH.setForeground(getThemeTextColor());

      } else {

        TEXTFIELD_SERVERPROPERTIESPATH.setForeground(getThemeErrorColor());
        TEXTFIELD_SERVERPROPERTIESPATH.setIcon(ERROR_ICON_SERVERPROPERTIES);
        TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(ERROR_ICON_SERVERPROPERTIES.getDescription());
      }
    });
  }

  /**
   * Check the current content of the Java args textarea. If it is empty, set it to Aikars flags,
   * else ask the user whether they want to overwrite the current setting with Aikars flags.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void setAikarsFlags(ActionEvent actionEvent) {
    setAikarsFlags();
  }

  /**
   * Sets the text of the Java args textarea to the popular Aikar flags.
   *
   * @author Griefed
   */
  public void setAikarsFlags() {
    if (!getJavaArgs().isEmpty()) {
      switch (JOptionPane.showConfirmDialog(
          CREATESERVERPACKPANEL,
          I18N.getMessage("createserverpack.gui.createserverpack.javaargs.confirm.message"),
          I18N.getMessage("createserverpack.gui.createserverpack.javaargs.confirm.title"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          ISSUE_ICON)) {
        case 0:
          setJavaArgs(APPLICATIONPROPERTIES.getAikarsFlags());

        case 1:
        default:
          break;
      }

    } else {

      setJavaArgs(APPLICATIONPROPERTIES.getAikarsFlags());
    }
  }

  /**
   * Checks whether the checkbox for the modloader-server installation is selected and whether the
   * path to the Java-installation is defined. If true and empty, a message is displayed, warning
   * the user that Javapath needs to be defined for the modloader-server installation to work. If
   * "Yes" is clicked, a filechooser will open where the user can select their
   * Java-executable/binary. If "No" is selected, the user is warned about the consequences of not
   * setting the Javapath.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void actionEventCheckBoxServer(ActionEvent actionEvent) {
    if (isServerInstallationTicked() && !APPLICATIONPROPERTIES.javaAvailable()) {
      switch (JOptionPane.showConfirmDialog(
          CREATESERVERPACKPANEL,
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.message"),
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.title"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          ISSUE_ICON)) {
        case 0:
          chooseJava();
          break;

        case 1:
          JOptionPane.showMessageDialog(
              CREATESERVERPACKPANEL,
              I18N.getMessage(
                  "createserverpack.gui.createserverpack.checkboxserver.message.message"),
              I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.message.title"),
              JOptionPane.ERROR_MESSAGE,
              ISSUE_ICON);
          setServerInstallationSelection(false);
      }
    }
  }

  /**
   * Adds a mouse listener to the passed button which sets the content area of said button
   * filled|not-filled when the mouse enters|leaves the button.
   *
   * @param buttonToAddMouseListenerTo JButton. The button to add the mouse listener to.
   * @author Griefed
   */
  private void addMouseListenerContentAreaFilledToButton(JButton buttonToAddMouseListenerTo) {

    buttonToAddMouseListenerTo.addMouseListener(
        new MouseAdapter() {
          public void mouseEntered(MouseEvent event) {
            buttonToAddMouseListenerTo.setContentAreaFilled(true);
          }

          public void mouseExited(MouseEvent event) {
            buttonToAddMouseListenerTo.setContentAreaFilled(false);
          }
        });
  }

  /**
   * Set the modloader version combobox model depending on the currently selected modloader and
   * Minecraft version.
   *
   * @author Griefed
   */
  private void setModloaderVersionsModel() {
    setModloaderVersionsModel(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString());
  }

  /**
   * Set the modloader version combobox model depending on the currently selected modloader, with
   * the specified Minecraft version.
   *
   * @param minecraftVersion The Minecraft version to work with in the GUI update.
   * @author Griefed
   */
  private void setModloaderVersionsModel(String minecraftVersion) {
    switch (COMBOBOX_MODLOADERS.getSelectedIndex()) {
      case 0:
        if (VERSIONMETA.fabric().isMinecraftSupported(minecraftVersion)) {

          COMBOBOX_MODLOADER_VERSIONS.setModel(FABRIC_VERSIONS);

        } else {
          COMBOBOX_MODLOADER_VERSIONS.setModel(NO_VERSIONS);
        }
        return;

      case 1:
        if (VERSIONMETA
            .forge()
            .availableForgeVersionsArrayDescending(minecraftVersion)
            .isPresent()) {

          COMBOBOX_MODLOADER_VERSIONS.setModel(new DefaultComboBoxModel<>(
              VERSIONMETA.forge().availableForgeVersionsArrayDescending(minecraftVersion).get()));

        } else {
          COMBOBOX_MODLOADER_VERSIONS.setModel(NO_VERSIONS);
        }
        return;

      case 2:
        if (VERSIONMETA.fabric().isMinecraftSupported(minecraftVersion)) {

          COMBOBOX_MODLOADER_VERSIONS.setModel(QUILT_VERSIONS);

        } else {
          COMBOBOX_MODLOADER_VERSIONS.setModel(NO_VERSIONS);
        }
        return;

      case 3:
        if (VERSIONMETA.legacyFabric().isMinecraftSupported(minecraftVersion)) {

          COMBOBOX_MODLOADER_VERSIONS.setModel(LEGACY_FABRIC_VERSIONS);

        } else {
          COMBOBOX_MODLOADER_VERSIONS.setModel(NO_VERSIONS);
        }
    }
  }

  /**
   * Setter for the Minecraft version depending on which one is selected in the GUI.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void actionEventComboBoxMinecraftVersion(ActionEvent event) {
    setModloaderVersionsModel(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString());
    updateRequiredJavaVersion();
    checkMinecraftServer();
  }

  private void updateRequiredJavaVersion() {
    if (VERSIONMETA.minecraft().getServer(getMinecraftVersion()).isPresent()
        && VERSIONMETA.minecraft().getServer(getMinecraftVersion()).get().javaVersion()
        .isPresent()) {

      REQUIRED_JAVA_VERSION.setText(
          VERSIONMETA.minecraft().getServer(getMinecraftVersion()).get().javaVersion().get()
              .toString());

      if (REQUIRED_JAVA_VERSION.getText().equals("8") && APPLICATIONPROPERTIES.java8Path().isPresent()) {

        HashMap<String,String> data = SCRIPT_VARIABLES.getData();
        data.replace("SPC_JAVA_SPC",APPLICATIONPROPERTIES.java8Path().get());
        SCRIPT_VARIABLES.loadData(data);
        LOG.info("Automatically adjusted script variable SPC_JAVA_SPC to " + APPLICATIONPROPERTIES.java8Path().get());

      } else if (REQUIRED_JAVA_VERSION.getText().equals("17") && APPLICATIONPROPERTIES.java17Path().isPresent()) {

        HashMap<String,String> data = SCRIPT_VARIABLES.getData();
        data.replace("SPC_JAVA_SPC",APPLICATIONPROPERTIES.java17Path().get());
        SCRIPT_VARIABLES.loadData(data);
        LOG.info("Automatically adjusted script variable SPC_JAVA_SPC to " + APPLICATIONPROPERTIES.java17Path().get());

      }
    } else {
      REQUIRED_JAVA_VERSION.setText("?");
    }
  }

  /**
   * Check whether the selected Minecraft version has a server available. If no server is available,
   * or no URL to download the server for the selected version is available, a warning is
   * displayed.
   *
   * @author Griefed
   */
  public void checkMinecraftServer() {
    if (!VERSIONMETA.minecraft().getServer(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString())
        .isPresent()) {

      JOptionPane.showMessageDialog(
          this,
          String.format(
              I18N.getMessage("createserverpack.gui.createserverpack.minecraft.server.unavailable"),
              COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()) + "   ",
          I18N.getMessage("createserverpack.gui.createserverpack.minecraft.server"),
          JOptionPane.WARNING_MESSAGE,
          ISSUE_ICON
      );
    } else if (
        VERSIONMETA.minecraft().getServer(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString())
            .isPresent()
            && !VERSIONMETA.minecraft()
            .getServer(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()).get().url()
            .isPresent()) {

      JOptionPane.showMessageDialog(
          this,
          String.format(I18N.getMessage(
                  "createserverpack.gui.createserverpack.minecraft.server.url.unavailable"),
              COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()) + "   ",
          I18N.getMessage("createserverpack.gui.createserverpack.minecraft.server"),
          JOptionPane.WARNING_MESSAGE,
          ISSUE_ICON
      );
    }
  }

  /**
   * Setter for the modloader depending on which one is selected in the GUI.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void actionEventComboBoxModloaders(ActionEvent event) {
    setModloader(COMBOBOX_MODLOADERS.getSelectedItem().toString());
  }

  /**
   * Upon button-press, open a file-selector for the modpack-directory.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectModpackDirectory(ActionEvent event) {

    JFileChooser modpackDirChooser = new JFileChooser();

    if (new File(getModpackDirectory()).isDirectory()) {

      modpackDirChooser.setCurrentDirectory(new File(getModpackDirectory()));

    } else if (new File(getModpackDirectory()).isFile()) {

      modpackDirChooser.setCurrentDirectory(
          new File(new File(getModpackDirectory()).getParent()));

    } else {
      modpackDirChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    }
    modpackDirChooser.setDialogTitle(
        I18N.getMessage("createserverpack.gui.buttonmodpackdir.title"));
    modpackDirChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    modpackDirChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.createserverpack.chooser.modpack.filter"),
            "zip"));
    modpackDirChooser.setAcceptAllFileFilterUsed(false);
    modpackDirChooser.setMultiSelectionEnabled(false);
    modpackDirChooser.setPreferredSize(CHOOSERDIMENSION);

    if (modpackDirChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {
      try {

        setModpackDirectory(modpackDirChooser.getSelectedFile().getCanonicalPath());
        updateGuiFromSelectedModpack();

        LOG.debug(
            "Selected modpack directory: "
                + modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

      } catch (IOException ex) {
        LOG.error("Error getting directory from modpack directory chooser.", ex);
      }
    }
  }

  private void updateGuiFromSelectedModpack() {
    SwingUtilities.invokeLater(() -> {
      try {
        ConfigurationModel configurationModel = new ConfigurationModel();
        boolean updated = false;

        if (new File(getModpackDirectory() + "/manifest.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromCurseManifest(configurationModel,new File(getModpackDirectory() + "/manifest.json"));
          updated = true;

        } else if (new File(getModpackDirectory() + "/minecraftinstance.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromMinecraftInstance(configurationModel,new File(getModpackDirectory() + "/minecraftinstance.json"));
          updated = true;

        } else if (new File(getModpackDirectory() + "/modrinth.index.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromModrinthManifest(configurationModel,new File(getModpackDirectory() + "/modrinth.index.json"));
          updated = true;

        } else if (new File(getModpackDirectory() + "/instance.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromATLauncherInstance(configurationModel,new File(getModpackDirectory() + "/instance.json"));
          updated = true;

        } else if (new File(getModpackDirectory() + "/config.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromConfigJson(configurationModel,new File(getModpackDirectory() + "/config.json"));
          updated = true;

        } else if (new File(getModpackDirectory() + "/mmc-pack.json").isFile()) {
          CONFIG_UTILITIES.updateConfigModelFromMMCPack(configurationModel,new File(getModpackDirectory() + "/mmc-pack.json"));
          updated = true;
        }

        if (updated) {

          setMinecraftVersion(configurationModel.getMinecraftVersion());
          setModloader(configurationModel.getModLoader());
          setModloaderVersion(configurationModel.getModLoaderVersion());

          JOptionPane.showMessageDialog(
              this,
              String.format(
                  I18N.getMessage("createserverpack.gui.modpack.scan.message"),
                  getMinecraftVersion(),
                  getModloader(),
                  getModloaderVersion()) + "   ",
              I18N.getMessage("createserverpack.gui.modpack.scan"),
              JOptionPane.INFORMATION_MESSAGE,
              INFO_ICON
          );

        }
      } catch (IOException ex) {
        LOG.error("Couldn't update GUI from modpack manifests.",ex);
      }
    });
  }

  /**
   * Upon button-press, open a file-selector for the server-icon.png.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectServerIcon(ActionEvent event) {

    JFileChooser serverIconChooser = new JFileChooser();

    serverIconChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    serverIconChooser.setDialogTitle(
        I18N.getMessage("createserverpack.gui.createserverpack.chooser.icon.title"));
    serverIconChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    serverIconChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.createserverpack.chooser.icon.filter"),
            "png",
            "jpg",
            "jpeg",
            "bmp"));
    serverIconChooser.setAcceptAllFileFilterUsed(false);
    serverIconChooser.setMultiSelectionEnabled(false);
    serverIconChooser.setPreferredSize(CHOOSERDIMENSION);

    if (serverIconChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

      try {
        setServerIconPath(serverIconChooser.getSelectedFile().getCanonicalPath());

      } catch (IOException ex) {
        LOG.error("Error getting the icon-file for the server pack.", ex);
      }
    }
  }

  /**
   * Upon button-press, open a file-selector for the server.properties.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectServerProperties(ActionEvent event) {

    JFileChooser serverPropertiesChooser = new JFileChooser();

    serverPropertiesChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    serverPropertiesChooser.setDialogTitle(
        I18N.getMessage("createserverpack.gui.createserverpack.chooser.properties.title"));
    serverPropertiesChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    serverPropertiesChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.createserverpack.chooser.properties.filter"),
            "properties"));
    serverPropertiesChooser.setAcceptAllFileFilterUsed(false);
    serverPropertiesChooser.setMultiSelectionEnabled(false);
    serverPropertiesChooser.setPreferredSize(CHOOSERDIMENSION);

    if (serverPropertiesChooser.showOpenDialog(FRAME_SERVERPACKCREATOR)
        == JFileChooser.APPROVE_OPTION) {

      try {
        setServerPropertiesPath(serverPropertiesChooser.getSelectedFile().getCanonicalPath());

      } catch (IOException ex) {
        LOG.error("Error getting the properties-file for the server pack.", ex);
      }
    }
  }

  /**
   * Upon button-press, open a file-selector for clientside-only mods. If the modpack-directory is
   * specified, the file-selector will open in the mods-directory in the modpack-directory.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectClientMods(ActionEvent event) {

    JFileChooser clientModsChooser = new JFileChooser();

    if (getModpackDirectory().length() > 0
        && new File(getModpackDirectory()).isDirectory()
        && new File(getModpackDirectory() + "/mods").isDirectory()) {

      clientModsChooser.setCurrentDirectory(
          new File(getModpackDirectory() + "/mods"));
    } else {
      clientModsChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    }

    clientModsChooser.setDialogTitle(
        I18N.getMessage("createserverpack.gui.buttonclientmods.title"));
    clientModsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    clientModsChooser.setFileFilter(
        new FileNameExtensionFilter(
            I18N.getMessage("createserverpack.gui.buttonclientmods.filter"), "jar"));

    clientModsChooser.setAcceptAllFileFilterUsed(false);
    clientModsChooser.setMultiSelectionEnabled(true);
    clientModsChooser.setPreferredSize(CHOOSERDIMENSION);

    if (clientModsChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {

      File[] clientMods = clientModsChooser.getSelectedFiles();
      List<String> clientModsFilenames = new ArrayList<>(100);

      for (File mod : clientMods) {
        clientModsFilenames.add(mod.getName());
      }

      setClientsideMods(clientModsFilenames);

      LOG.debug("Selected mods: " + clientModsFilenames);
    }
  }

  /**
   * Upon button-press, open a file-selector for directories which are to be copied to the server
   * pack. If the modpack-directory is specified, the file-selector will be opened in the
   * modpack-directory.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectCopyDirs(ActionEvent event) {

    JFileChooser copyDirsChooser = new JFileChooser();

    if (getModpackDirectory().length() > 0
        && new File(getModpackDirectory()).isDirectory()) {

      copyDirsChooser.setCurrentDirectory(new File(getModpackDirectory()));
    } else {
      copyDirsChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    }

    copyDirsChooser.setDialogTitle(I18N.getMessage("createserverpack.gui.buttoncopydirs.title"));
    copyDirsChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    copyDirsChooser.setAcceptAllFileFilterUsed(false);
    copyDirsChooser.setMultiSelectionEnabled(true);
    copyDirsChooser.setPreferredSize(CHOOSERDIMENSION);

    if (copyDirsChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {
      File[] directoriesToCopy = copyDirsChooser.getSelectedFiles();
      List<String> copyDirsNames = new ArrayList<>(100);

      for (File directory : directoriesToCopy) {
        copyDirsNames.add(directory.getName());
      }

      setCopyDirectories(copyDirsNames);

      LOG.debug("Selected directories: " + copyDirsNames);
    }
  }

  /**
   * Opens a filechooser to select the Java-executable/binary.
   *
   * @author Griefed
   */
  void chooseJava() {
    JFileChooser javaChooser = new JFileChooser();

    if (new File(String.format("%s/bin/", System.getProperty("java.home").replace("\\", "/")))
        .isDirectory()) {

      javaChooser.setCurrentDirectory(
          new File(String.format("%s/bin/", System.getProperty("java.home").replace("\\", "/"))));

    } else {
      javaChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
    }

    javaChooser.setDialogTitle(I18N.getMessage("createserverpack.gui.buttonjavapath.tile"));
    javaChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    javaChooser.setAcceptAllFileFilterUsed(true);
    javaChooser.setMultiSelectionEnabled(false);
    javaChooser.setPreferredSize(CHOOSERDIMENSION);

    if (javaChooser.showOpenDialog(FRAME_SERVERPACKCREATOR) == JFileChooser.APPROVE_OPTION) {
      try {
        APPLICATIONPROPERTIES.setJavaPath(
            javaChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

        LOG.debug(
            "Set path to Java executable to: "
                + javaChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

      } catch (IOException ex) {
        LOG.error("Couldn't set java executable path.", ex);
      }
    }
  }

  /**
   * Upon button-press, check the entered configuration and if successfull, generate a server pack.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void generateServerpack(ActionEvent event) {

    BUTTON_GENERATESERVERPACK.setEnabled(false);
    STATUS_BAR.loadConfig(BUSY_CONFIG);

    int decision = 0;

    MATERIALTEXTPANEUI.installUI(LAZYMODETEXTPANE);

    if (getCopyDirectories().equals("lazy_mode")) {
      decision =
          JOptionPane.showConfirmDialog(
              FRAME_SERVERPACKCREATOR,
              LAZYMODETEXTPANE,
              I18N.getMessage("createserverpack.gui.createserverpack.lazymode"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.INFORMATION_MESSAGE);
    }

    LOG.debug("Case " + decision);

    // No inspection in case we ever want to expand on this switch statement.
    //noinspection SwitchStatementWithTooFewBranches
    switch (decision) {
      case 0:
        generate();
        break;

      default:
        ready();
        break;
    }
  }

  /**
   * Acquire the current settings in the GUI as a {@link ConfigurationModel}.
   *
   * @return The current settings in the GUI.
   * @author Griefed
   */
  public ConfigurationModel currentConfigAsModel() {
    return new ConfigurationModel(
        getClientsideModsList(),
        getCopyDirectoriesList(),
        getModpackDirectory(),
        getMinecraftVersion(),
        getModloader(),
        getModloaderVersion(),
        getJavaArgs(),
        getServerpackSuffix(),
        getServerIconPath(),
        getServerPropertiesPath(),
        isServerInstallationTicked(),
        isServerIconInclusionTicked(),
        isServerPropertiesInclusionTicked(),
        isZipCreationTicked(),
        getScriptSettings(),
        getConfigPanelConfigs());
  }

  /**
   * Get the configurations of the available ExtensionConfigPanel as a hashmap, so we can store them
   * in our serverpackcreator.conf.
   *
   * @return Map containing lists of CommentedConfigs mapped to the corresponding pluginID.
   */
  private HashMap<String, ArrayList<CommentedConfig>> getConfigPanelConfigs() {
    HashMap<String, ArrayList<CommentedConfig>> configs = new HashMap<>();
    if (!CONFIG_PANELS.isEmpty()) {
      CONFIG_PANELS.forEach(
          panel -> configs.put(panel.pluginID(), panel.serverPackExtensionConfig())
      );
    }
    return configs;
  }

  /**
   * Generate a server pack from the current configuration in the GUI.
   *
   * @author Griefed
   */
  private void generate() {

    Tailer tailer =
        Tailer.create(
            new File("./logs/serverpackcreator.log"),
            new TailerListenerAdapter() {
              public void handle(String line) {
                if (!line.contains("DEBUG")) {
                  // labelGenerateServerPack.setText(line.substring(line.indexOf(") - ") + 4));
                  updateStatus(line.substring(line.indexOf(") - ") + 4));
                }
              }
            },
            100,
            false);

    LOG.info("Checking entered configuration.");

    updateStatus(I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.start"));

    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.execute(
        () -> {

          ConfigurationModel configurationModel = currentConfigAsModel();
          List<String> encounteredErrors = new ArrayList<>(100);

          if (!CONFIGURATIONHANDLER.checkConfiguration(
              configurationModel, encounteredErrors, true)) {

            LOG.info("Configuration checked successfully.");

            updateStatus(
                I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.checked"));

            configurationModel.save(APPLICATIONPROPERTIES.DEFAULT_CONFIG());

            LOG.info("Starting ServerPackCreator run.");

            updateStatus(
                I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.generating"));

            try {

              SERVERPACKHANDLER.run(configurationModel);

              loadConfig(APPLICATIONPROPERTIES.DEFAULT_CONFIG());

              updateStatus(
                  I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.ready"));

              SERVERPACKGENERATEDDOCUMENT.setParagraphAttributes(
                  0,
                  SERVERPACKGENERATEDDOCUMENT.getLength(),
                  SERVERPACKGENERATEDATTRIBUTESET,
                  false);
              MATERIALTEXTPANEUI.installUI(SERVERPACKGENERATEDTEXTPANE);

              ready();

              if (JOptionPane.showConfirmDialog(
                  FRAME_SERVERPACKCREATOR,
                  SERVERPACKGENERATEDTEXTPANE,
                  I18N.getMessage("createserverpack.gui.createserverpack.openfolder.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.INFORMATION_MESSAGE,
                  INFO_ICON)
                  == 0) {

                try {
                  Desktop.getDesktop()
                      .open(
                          new File(SERVERPACKHANDLER.getServerPackDestination(configurationModel)));
                } catch (IOException ex) {
                  LOG.error("Error opening file explorer for server pack.", ex);
                }
              }

            } catch (Exception ex) {

              LOG.error("An error occurred when generating the server pack.", ex);
            }

          } else {

            updateStatus(I18N.getMessage("createserverpack.gui.buttongenerateserverpack.fail"));

            if (encounteredErrors.size() > 0) {

              StringBuilder errors = new StringBuilder();

              for (int i = 0; i < encounteredErrors.size(); i++) {

                errors.append(i + 1).append(": ").append(encounteredErrors.get(i)).append("    ")
                    .append("\n");
              }

              ready();

              JOptionPane.showMessageDialog(
                  FRAME_SERVERPACKCREATOR,
                  errors,
                  String.format(
                      I18N.getMessage("createserverpack.gui.createserverpack.errors.encountered"),
                      encounteredErrors.size()),
                  JOptionPane.ERROR_MESSAGE,
                  ISSUE_ICON);
            }
          }

          encounteredErrors.clear();
          tailer.stop();

          ready();

          System.gc();
          System.runFinalization();

          executorService.shutdownNow();
        });
  }

  /**
   * Set the GUI ready for the next generation.
   *
   * @author Griefed
   */
  private void ready() {
    BUTTON_GENERATESERVERPACK.setEnabled(true);
    STATUS_BAR.loadConfig(IDLE_CONFIG);
  }

  /**
   * Save the current configuration to a specified file.
   *
   * @param configFile The file to store the configuration under.
   * @author Griefed
   */
  void saveConfig(File configFile) {
    currentConfigAsModel().save(configFile);
  }

  /**
   * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
   * ServerPackCreator.
   *
   * @param configFile The configuration file to parse and load into the GUI.
   * @author Griefed
   */
  protected void loadConfig(File configFile) {
    try {
      ConfigurationModel configurationModel = new ConfigurationModel(UTILITIES, configFile);

      setModpackDirectory(configurationModel.getModpackDir());

      if (configurationModel.getClientMods().isEmpty()) {

        setClientsideMods(APPLICATIONPROPERTIES.getListFallbackMods());
        LOG.debug("Set clientMods with fallback list.");

      } else {
        setClientsideMods(configurationModel.getClientMods());
      }

      if (configurationModel.getCopyDirs().isEmpty()) {

        setCopyDirectories(APPLICATIONPROPERTIES.getDirectoriesToInclude());

      } else {

        setCopyDirectories(configurationModel.getCopyDirs());
      }

      setScriptVariables(configurationModel.getScriptSettings());

      setServerIconPath(configurationModel.getServerIconPath());
      setServerPropertiesPath(configurationModel.getServerPropertiesPath());

      if (configurationModel.getMinecraftVersion().isEmpty()) {
        configurationModel.setMinecraftVersion(VERSIONMETA.minecraft().latestRelease().version());
      }

      setMinecraftVersion(configurationModel.getMinecraftVersion());
      setModloader(configurationModel.getModLoader());
      setModloaderVersion(configurationModel.getModLoaderVersion());

      setServerInstallationSelection(configurationModel.getIncludeServerInstallation());
      setServerIconSelection(configurationModel.getIncludeServerIcon());
      setServerPropertiesSelection(configurationModel.getIncludeServerProperties());
      setServerZipArchiveSelection(configurationModel.getIncludeZipCreation());

      setJavaArgs(configurationModel.getJavaArgs());

      setServerpackSuffix(configurationModel.getServerPackSuffix());

      CONFIG_PANELS.forEach(
          panel ->
              panel.setServerPackExtensionConfig(
                  configurationModel.getOrCreateAddonConfigList(panel.pluginID())
              )
      );

    } catch (Exception ex) {

      LOG.error("Couldn't load configuration file.", ex);
      JOptionPane.showMessageDialog(
          this,
          I18N.getMessage("createserverpack.gui.config.load.error.message") + " " + ex.getCause() + "   ",
          I18N.getMessage("createserverpack.gui.config.load.error"),
          JOptionPane.ERROR_MESSAGE,
          ISSUE_ICON
      );
    }
  }

  /**
   * Load default values for textfields so the user can start with a new configuration. Just as if
   * ServerPackCreator was started without a serverpackcreator.conf being present.
   *
   * @author Griefed
   */
  protected void clearInterface() {

    setModpackDirectory("");
    setServerpackSuffix("");

    setClientsideMods(APPLICATIONPROPERTIES.getListFallbackMods());
    setCopyDirectories(APPLICATIONPROPERTIES.getDirectoriesToInclude());

    setServerIconPath("");
    setServerPropertiesPath("");

    COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(0);
    setModloader("Forge");

    setServerInstallationSelection(true);
    setServerIconSelection(true);
    setServerPropertiesSelection(true);
    setServerZipArchiveSelection(true);

    setJavaArgs("");

    clearScriptVariables();

    CONFIG_PANELS.forEach(ExtensionConfigPanel::clear);
  }

  /**
   * Get the current themes error-text colour.
   *
   * @return The current themes error-text colour.
   * @author Griefed
   */
  public Color getThemeErrorColor() {
    if (APPLICATIONPROPERTIES.isDarkTheme()) {

      return DARKTHEME.getTextErrorColour();

    } else {

      return LIGHTTHEME.getTextErrorColour();
    }
  }

  /**
   * The current themes default text colour.
   *
   * @return The current themes default text colour.
   * @author Griefed
   */
  public Color getThemeTextColor() {
    if (APPLICATIONPROPERTIES.isDarkTheme()) {

      return DARKTHEME.getTextColor();

    } else {

      return LIGHTTHEME.getTextColor();
    }
  }

  /**
   * Getter for the currently set JVM flags / Java args.
   *
   * @return String. Returns the currently set JVM flags / Java args.
   * @author Griefed
   */
  public String getJavaArgs() {
    return TEXTAREA_JAVAARGS.getText();
  }

  /**
   * Setter for the JVM flags / Java args.
   *
   * @param javaArgs The javaargs to set.
   * @author Griefed
   */
  public void setJavaArgs(String javaArgs) {
    TEXTAREA_JAVAARGS.setText(javaArgs);
  }

  /**
   * Getter for the text in the custom server-icon textfield.
   *
   * @return String. Returns the text in the server-icon textfield.
   * @author Griefed
   */
  public String getServerIconPath() {
    return TEXTFIELD_SERVERICONPATH.getText();
  }

  /**
   * Getter for the text in the custom server-icon textfield.
   *
   * @param path The path to the server icon file.
   * @author Griefed
   */
  public void setServerIconPath(String path) {
    TEXTFIELD_SERVERICONPATH.setText(path.replace("\\", "/"));
  }

  /**
   * Getter for the text in the custom server.properties textfield
   *
   * @return String. Returns the text in the server.properties textfield.
   * @author Griefed
   */
  public String getServerPropertiesPath() {
    return TEXTFIELD_SERVERPROPERTIESPATH.getText();
  }

  /**
   * Getter for the text in the custom server.properties textfield
   *
   * @param path The path to the server properties file.
   * @author Griefed
   */
  public void setServerPropertiesPath(String path) {
    TEXTFIELD_SERVERPROPERTIESPATH.setText(path.replace("\\", "/"));
  }

  /**
   * Get the currently loaded configuration of the status bar.
   *
   * @return The configuration of the status bar.
   * @author Griefed
   */
  public ScannerConfig getStatusBarSettings() {
    return STATUS_BAR.getCurrentConfig();
  }

  /**
   * set the configuration for the status bar in the GUI. Bear in mind that after the generation of
   * a server pack has finished, the idle-config is loaded into the status bar.
   *
   * @param config The configuration to load into the status bar.
   * @author Griefed
   */
  public void setStatusBarSettings(ScannerConfig config) {
    STATUS_BAR.loadConfig(config);
  }

  /**
   * Get the Minecraft version currently selected in the GUI.
   *
   * @return The Minecraft version currently selected in the GUI.
   * @author Griefed
   */
  public String getMinecraftVersion() {
    return COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString();
  }

  /**
   * Set the Minecraft version selected in the GUI. If the specified version is not available in the
   * current combobox model, nothing happens.
   *
   * @param minecraftVersion The Minecraft version to select.
   * @author Griefed
   */
  public void setMinecraftVersion(String minecraftVersion) {
    for (int i = 0; i < COMBOBOX_MINECRAFTVERSIONS.getModel().getSize(); i++) {
      if (COMBOBOX_MINECRAFTVERSIONS.getModel().getElementAt(i).equals(minecraftVersion)) {
        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
        break;
      }
    }
  }

  /**
   * Get the modloader selected in the GUI.
   *
   * @return The modloader selected in the GUI.
   * @author Griefed
   */
  public String getModloader() {
    return COMBOBOX_MODLOADERS.getSelectedItem().toString();
  }

  /**
   * Set the modloader selected in the GUI. If the specified modloader is not viable, nothing
   * happens.
   *
   * @param modloader The modloader to select.
   * @author Griefed
   */
  public void setModloader(String modloader) {
    switch (modloader) {
      // Fabric
      case "Fabric":
        COMBOBOX_MODLOADERS.setSelectedIndex(0);

        break;

      // Forge
      case "Forge":
        COMBOBOX_MODLOADERS.setSelectedIndex(1);

        break;

      // Quilt
      case "Quilt":
        COMBOBOX_MODLOADERS.setSelectedIndex(2);

        break;

      case "LegacyFabric":
        COMBOBOX_MODLOADERS.setSelectedIndex(3);
    }
    setModloaderVersionsModel();
  }

  /**
   * Get the modloader version selected in the GUI.
   *
   * @return The modloader version selected in the GUI.
   * @author Griefed
   */
  public String getModloaderVersion() {
    return COMBOBOX_MODLOADER_VERSIONS.getSelectedItem().toString();
  }

  /**
   * Set the modloader version selected in the GUI. If the specified version is not available in the
   * currently set modloader version combobox model, nothing happens.
   *
   * @param version The modloader version to select.
   * @author Griefed
   */
  public void setModloaderVersion(String version) {
    for (int i = 0; i < COMBOBOX_MODLOADER_VERSIONS.getModel().getSize(); i++) {
      if (COMBOBOX_MODLOADER_VERSIONS.getModel().getElementAt(i).equals(version)) {
        COMBOBOX_MODLOADER_VERSIONS.setSelectedIndex(i);
        break;
      }
    }
  }

  /**
   * Get the list of clientside-only mods to exclude from the server pack.
   *
   * @return The list of clientside-only mods to exclude from the server pack.
   * @author Griefed
   */
  public String getClientsideMods() {
    return TEXTAREA_CLIENTSIDEMODS.getText().replace(", ", ",");
  }

  /**
   * Set the list of clientside-only mods to exclude from the server pack.
   *
   * @param mods the list of clientside-only mods.
   * @author Griefed
   */
  public void setClientsideMods(List<String> mods) {
    TEXTAREA_CLIENTSIDEMODS.setText(UTILITIES.StringUtils().buildString(mods));
  }

  /**
   * Get the list of clientside-only mods to exclude from the server pack.
   *
   * @return The list of clientside-only mods to exclude from the server pack.
   * @author Griefed
   */
  public List<String> getClientsideModsList() {
    return UTILITIES.ListUtils()
        .cleanList(new ArrayList<>(Arrays.asList(getClientsideMods().split(","))));
  }

  /**
   * Get the list of files and directories to include in the server pack.
   *
   * @return The list of files and directories to include in the server pack.
   * @author Griefed
   */
  public String getCopyDirectories() {
    return TEXTAREA_COPYDIRECTORIES.getText().replace(", ", ",");
  }

  /**
   * Set the list of files and directories to include in the server pack.
   *
   * @param directoriesAndFiles The list of files and directories to include in the server pack.
   * @author Griefed
   */
  public void setCopyDirectories(List<String> directoriesAndFiles) {
    TEXTAREA_COPYDIRECTORIES.setText(
        UTILITIES.StringUtils()
            .buildString(directoriesAndFiles.toString().replace("\\", "/")));
  }

  /**
   * Get the list of files and directories to include in the server pack.
   *
   * @return The list of files and directories to include in the server pack.
   * @author Griefed
   */
  public List<String> getCopyDirectoriesList() {
    return UTILITIES.ListUtils()
        .cleanList(new ArrayList<>(Arrays.asList(getCopyDirectories().split(","))));
  }

  /**
   * Get the modpack directory.
   *
   * @return The modpack directory.
   * @author Griefed
   */
  public String getModpackDirectory() {
    return TEXTFIELD_MODPACKDIRECTORY.getText().replace("\\", "/");
  }

  /**
   * Set the modpack directory.
   *
   * @param directory The directory which holds the modpack.
   * @author Griefed
   */
  public void setModpackDirectory(String directory) {
    TEXTFIELD_MODPACKDIRECTORY.setText(directory.replace("\\", "/"));
  }

  /**
   * Get the server pack suffix text.
   *
   * @return The server pack suffix text.
   * @author Griefed
   */
  public String getServerpackSuffix() {
    return UTILITIES.StringUtils().pathSecureText(TEXTFIELD_SERVERPACKSUFFIX.getText());
  }

  /**
   * Set the server pack suffix text.
   *
   * @param suffix The suffix to append to the server pack folder and ZIP-archive.
   * @author Griefed
   */
  public void setServerpackSuffix(String suffix) {
    TEXTFIELD_SERVERPACKSUFFIX.setText(UTILITIES.StringUtils().pathSecureText(suffix));
  }

  /**
   * Get a hashmap of the data available in the script variables table.
   *
   * @return A map containig all placeholders with their respective values.
   * @author Griefed
   */
  public HashMap<String, String> getScriptSettings() {
    return SCRIPT_VARIABLES.getData();
  }

  /**
   * Load the hashmap into the script variables table. Before the data is loaded, all currently
   * available data in the table is cleared. Use with caution.
   *
   * @param data The new map of placeholder-value pairs to set the table to.
   * @author Griefed
   */
  public void setScriptVariables(HashMap<String, String> data) {
    SCRIPT_VARIABLES.loadData(data);
  }

  /**
   * Clear any available data in the script variables table.
   */
  public void clearScriptVariables() {
    SCRIPT_VARIABLES.clearData();
  }

  /**
   * Is the modloader server installation desired?
   *
   * @return <code>true</code> if it is.
   * @author Griefed
   */
  public boolean isServerInstallationTicked() {
    return CHECKBOX_SERVER.isSelected();
  }

  /**
   * Is the inclusion of a server-icon.png desired?
   *
   * @return <code>true</code> if it is.
   * @author Griefed
   */
  public boolean isServerIconInclusionTicked() {
    return CHECKBOX_ICON.isSelected();
  }

  /**
   * Is the inclusion of a server.properties-file desired?
   *
   * @return <code>true</code> if it is.
   * @author Griefed
   */
  public boolean isServerPropertiesInclusionTicked() {
    return CHECKBOX_PROPERTIES.isSelected();
  }

  /**
   * Is the creation of a server pack ZIP-archive desired?
   *
   * @return <code>true</code> if it is.
   * @author Griefed
   */
  public boolean isZipCreationTicked() {
    return CHECKBOX_ZIP.isSelected();
  }

  /**
   * Change the selection of the server installation checkbox.
   *
   * @param selected Whether the checkbox should be ticked.
   * @author Griefed
   */
  public void setServerInstallationSelection(boolean selected) {
    CHECKBOX_SERVER.setSelected(selected);
  }

  /**
   * Change the selection of the server icon checkbox.
   *
   * @param selected Whether the checkbox should be ticked.
   * @author Griefed
   */
  public void setServerIconSelection(boolean selected) {
    CHECKBOX_ICON.setSelected(selected);
  }

  /**
   * Change the selection of the server properties checkbox.
   *
   * @param selected Whether the checkbox should be ticked.
   * @author Griefed
   */
  public void setServerPropertiesSelection(boolean selected) {
    CHECKBOX_PROPERTIES.setSelected(selected);
  }

  /**
   * Change the selection of the server ZIP-archive checkbox.
   *
   * @param selected Whether the checkbox should be ticked.
   * @author Griefed
   */
  public void setServerZipArchiveSelection(boolean selected) {
    CHECKBOX_ZIP.setSelected(selected);
  }

  /**
   * Get a random reticulation from {@link ReticulatingSplines}.
   *
   * @return Calculating Llama Expectoration Trajectory
   * @author Griefed
   */
  public String reticulate() {
    return RETICULATING_SPLINES.reticulate();
  }
}
