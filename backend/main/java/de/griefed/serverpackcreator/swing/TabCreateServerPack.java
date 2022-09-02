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

import de.griefed.larsonscanner.LarsonScanner;
import de.griefed.larsonscanner.LarsonScanner.ScannerConfig;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.i18n.I18n;
import de.griefed.serverpackcreator.swing.themes.DarkTheme;
import de.griefed.serverpackcreator.swing.themes.LightTheme;
import de.griefed.serverpackcreator.swing.utilities.CompoundIcon;
import de.griefed.serverpackcreator.swing.utilities.IconTextArea;
import de.griefed.serverpackcreator.swing.utilities.IconTextField;
import de.griefed.serverpackcreator.swing.utilities.RotatedIcon;
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
import javax.swing.UIManager;
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
import org.jetbrains.annotations.Nullable;

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
  private final ApplicationProperties APPLICATIONPROPERTIES;
  private final ConfigUtilities CONFIGUTILITIES;
  private final DarkTheme DARKTHEME;
  private final LightTheme LIGHTTHEME;

  private final StyledDocument SERVERPACKGENERATEDDOCUMENT = new DefaultStyledDocument();
  private final SimpleAttributeSet SERVERPACKGENERATEDATTRIBUTESET = new SimpleAttributeSet();
  private final JTextPane SERVERPACKGENERATEDTEXTPANE = new JTextPane(SERVERPACKGENERATEDDOCUMENT);

  private final StyledDocument LAZYMODEDOCUMENT = new DefaultStyledDocument();
  private final JTextPane LAZYMODETEXTPANE = new JTextPane(LAZYMODEDOCUMENT);

  private final int ERROR_ICON_SIZE = 18;
  private final BufferedImage ERROR_ICON_BASE =
      ImageIO.read(
          Objects.requireNonNull(
              ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/error.png")));
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
  private final ImageIcon ERROR_ICON_JAVAPATH =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_SERVERICON =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));
  private final ImageIcon ERROR_ICON_SERVERPROPERTIES =
      new ImageIcon(
          ERROR_ICON_BASE.getScaledInstance(ERROR_ICON_SIZE, ERROR_ICON_SIZE, Image.SCALE_SMOOTH));

  private final Dimension CHOOSERDIMENSION = new Dimension(750, 450);

  private final JButton BUTTON_GENERATESERVERPACK = new JButton();

  private final JPanel CREATESERVERPACKPANEL = new JPanel();

  private final LarsonScanner STATUS_BAR = new LarsonScanner();
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
              new Color(50, 83, 88),
              new Color(50, 83, 88),
              new Color(50, 83, 88),
              new Color(50, 83, 88),
              new Color(50, 83, 88),
              new Color(50, 83, 88),
              new Color(50, 83, 88)
          },
          new Color(49, 47, 47),
          new Color(49, 47, 47));
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
              new Color(192, 255, 238),
              new Color(192, 255, 238),
              new Color(192, 255, 238),
              new Color(192, 255, 238),
              new Color(192, 255, 238),
              new Color(192, 255, 238),
              new Color(192, 255, 238)
          },
          new Color(49, 47, 47),
          new Color(49, 47, 47));
  private final MaterialTextPaneUI MATERIALTEXTPANEUI = new MaterialTextPaneUI();

  private final JComboBox<String> COMBOBOX_MINECRAFTVERSIONS = new JComboBox<>();
  private final JComboBox<String> COMBOBOX_MODLOADERS = new JComboBox<>();
  private final JComboBox<String> COMBOBOX_MODLOADER_VERSIONS = new JComboBox<>();

  private final DefaultComboBoxModel<String> LEGACY_FABRIC_VERSIONS;
  private final DefaultComboBoxModel<String> FABRIC_VERSIONS;
  private final DefaultComboBoxModel<String> QUILT_VERSIONS;

  private final IconTextArea TEXTAREA_CLIENTSIDEMODS = new IconTextArea("");
  private final IconTextArea TEXTAREA_JAVAARGS = new IconTextArea("");
  private final IconTextArea TEXTAREA_COPYDIRECTORIES = new IconTextArea("");

  private final IconTextField TEXTFIELD_MODPACKDIRECTORY = new IconTextField("");
  private final IconTextField TEXTFIELD_JAVAPATH = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERPACKSUFFIX = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERICONPATH = new IconTextField("");
  private final IconTextField TEXTFIELD_SERVERPROPERTIESPATH = new IconTextField("");

  private final File DIRECTORY_CHOOSER = new File(".");

  private final String[] NONE;

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
  private String chosenModloader = "Fabric";
  private String chosenModloaderVersion;
  private String chosenMinecraftVersion = "1.18.2";
  private String javaArgs = "";

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
   * @param injectedI18n                   Instance of {@link I18n} required for localized log
   *                                       messages.
   * @param injectedConfigurationHandler   Instance of {@link ConfigurationHandler} required to
   *                                       successfully and correctly create the server pack.
   * @param injectedServerPackHandler      Instance of {@link ServerPackHandler} required for the
   *                                       generation of server packs.
   * @param injectedVersionMeta            Instance of {@link VersionMeta} required for
   *                                       setting/changing comboboxes.
   * @param injectedApplicationProperties  Instance of {@link Properties} required for various
   *                                       different things.
   * @param injectedServerPackCreatorFrame Our parent frame which contains all of
   *                                       ServerPackCreator.
   * @param injectedUtilities              Instance of {@link Utilities}.
   * @param injectedConfigUtilities        Instance of {@link ConfigUtilities}.
   * @param injectedDarkTheme              Instance of {@link DarkTheme}.
   * @param injectedLightTheme             Instance of {@link LightTheme}.
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
      ConfigUtilities injectedConfigUtilities,
      DarkTheme injectedDarkTheme,
      LightTheme injectedLightTheme)
      throws IOException {

    this.DARKTHEME = injectedDarkTheme;
    this.LIGHTTHEME = injectedLightTheme;
    this.APPLICATIONPROPERTIES = injectedApplicationProperties;
    this.I18N = injectedI18n;
    this.VERSIONMETA = injectedVersionMeta;
    this.UTILITIES = injectedUtilities;
    this.CONFIGUTILITIES = injectedConfigUtilities;
    this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
    this.SERVERPACKHANDLER = injectedServerPackHandler;
    this.FRAME_SERVERPACKCREATOR = injectedServerPackCreatorFrame;

    SERVERPACKGENERATEDTEXTPANE.setOpaque(false);
    SERVERPACKGENERATEDTEXTPANE.setEditable(false);
    StyleConstants.setBold(SERVERPACKGENERATEDATTRIBUTESET, true);
    StyleConstants.setFontSize(SERVERPACKGENERATEDATTRIBUTESET, 14);
    SERVERPACKGENERATEDTEXTPANE.setCharacterAttributes(SERVERPACKGENERATEDATTRIBUTESET, true);
    StyleConstants.setAlignment(SERVERPACKGENERATEDATTRIBUTESET, StyleConstants.ALIGN_LEFT);
    try {
      SERVERPACKGENERATEDDOCUMENT.insertString(
          0,
          I18N.getMessage("createserverpack.gui.createserverpack.openfolder.browse"),
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

    NONE = new String[]{I18N.getMessage("createserverpack.gui.createserverpack.forge.none")};

    LEGACY_FABRIC_VERSIONS = new DefaultComboBoxModel<>(
        VERSIONMETA.legacyFabric().loaderVersionsArrayDescending());
    FABRIC_VERSIONS =
        new DefaultComboBoxModel<>(VERSIONMETA.fabric().loaderVersionsArrayDescending());
    QUILT_VERSIONS =
        new DefaultComboBoxModel<>(VERSIONMETA.quilt().loaderVersionsArrayDescending());

    CREATESERVERPACKPANEL.setLayout(new GridBagLayout());
    // ----------------------------------------------------------------LABELS AND TEXTFIELDS--------
    GridBagConstraints GRIDBAGCONSTRAINTS = new GridBagConstraints();
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridwidth = 3;
    GRIDBAGCONSTRAINTS.weightx = 1;

    // Label and textfield modpackDir
    JLabel labelModpackDir =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir"));
    labelModpackDir.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 0;
    Insets TWENTY_TEN_ZERO_ZERO = new Insets(20, 10, 0, 0);
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelModpackDir, GRIDBAGCONSTRAINTS);

    TEXTFIELD_MODPACKDIRECTORY.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
    TEXTFIELD_MODPACKDIRECTORY.addDocumentListener(
        (SimpleDocumentListener) e -> validateModpackDir());

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 1;
    Insets ZERO_TEN_ZERO_ZERO = new Insets(0, 10, 0, 0);
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(TEXTFIELD_MODPACKDIRECTORY, GRIDBAGCONSTRAINTS);

    // Label and textfield server pack suffix
    JLabel labelServerPackSuffix =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix"));
    labelServerPackSuffix.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix.tip"));

    GRIDBAGCONSTRAINTS.gridwidth = 2;
    GRIDBAGCONSTRAINTS.gridx = 4;
    GRIDBAGCONSTRAINTS.gridy = 0;
    GRIDBAGCONSTRAINTS.insets = new Insets(20, -140, 0, 0);

    CREATESERVERPACKPANEL.add(labelServerPackSuffix, GRIDBAGCONSTRAINTS);

    TEXTFIELD_SERVERPACKSUFFIX.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelsuffix.tip"));
    ERROR_ICON_SERVERPACK_SUFFIX.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textsuffix.error"));
    TEXTFIELD_SERVERPACKSUFFIX.addDocumentListener((SimpleDocumentListener) e -> validateSuffix());

    GRIDBAGCONSTRAINTS.gridwidth = 1;
    GRIDBAGCONSTRAINTS.gridx = 4;
    GRIDBAGCONSTRAINTS.gridy = 1;
    GRIDBAGCONSTRAINTS.insets = new Insets(0, -140, 0, 0);

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPACKSUFFIX, GRIDBAGCONSTRAINTS);

    GRIDBAGCONSTRAINTS.gridwidth = 5;

    // Label and textfield clientMods
    JLabel labelClientMods =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods"));
    labelClientMods.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 2;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelClientMods, GRIDBAGCONSTRAINTS);

    TEXTAREA_CLIENTSIDEMODS.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));
    TEXTAREA_CLIENTSIDEMODS.setFont(new Font("Noto Sans Display Regular", Font.PLAIN, 15));
    ERROR_ICON_CLIENTSIDE_MODS.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textclientmods.error"));
    TEXTAREA_CLIENTSIDEMODS.addDocumentListener((SimpleDocumentListener) e -> validateClientMods());
    JPanel CLIENTSIDEMODS_JPANEL = new JPanel();
    CLIENTSIDEMODS_JPANEL.setLayout(new GridBagLayout());
    GridBagConstraints TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS = new GridBagConstraints();
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.anchor = GridBagConstraints.CENTER;
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.fill = GridBagConstraints.BOTH;
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.gridx = 0;
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.gridy = 0;
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.weighty = 1;
    TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS.weightx = 1;

    JScrollPane SCROLL_PANEL_CLIENTSIDEMODS =
        new JScrollPane(
            TEXTAREA_CLIENTSIDEMODS,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    CLIENTSIDEMODS_JPANEL.add(
        SCROLL_PANEL_CLIENTSIDEMODS, TEXTAREA_CLIENTSIDEMODS_JPANEL_CONSTRAINTS);
    CLIENTSIDEMODS_JPANEL.setSize(100, 100);
    CLIENTSIDEMODS_JPANEL.setPreferredSize(new Dimension(100, 100));
    CLIENTSIDEMODS_JPANEL.setMaximumSize(new Dimension(100, 100));
    CLIENTSIDEMODS_JPANEL.setMinimumSize(new Dimension(100, 100));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 3;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(CLIENTSIDEMODS_JPANEL, GRIDBAGCONSTRAINTS);

    // Label and textfield copyDirs
    JLabel labelCopyDirs =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs"));
    labelCopyDirs.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 4;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelCopyDirs, GRIDBAGCONSTRAINTS);

    TEXTAREA_COPYDIRECTORIES.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));
    TEXTAREA_COPYDIRECTORIES.setFont(new Font("Noto Sans Display Regular", Font.PLAIN, 15));
    ERROR_ICON_COPYDIRECTORIES.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textclientmods.error"));
    TEXTAREA_COPYDIRECTORIES.addDocumentListener((SimpleDocumentListener) e -> validateCopyDirs());
    JPanel COPYDIRECTORIES_JPANEL = new JPanel();
    COPYDIRECTORIES_JPANEL.setLayout(new GridBagLayout());
    GridBagConstraints TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS = new GridBagConstraints();
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.anchor = GridBagConstraints.CENTER;
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.fill = GridBagConstraints.BOTH;
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.gridx = 0;
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.gridy = 0;
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.weighty = 1;
    TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS.weightx = 1;

    JScrollPane SCROLL_PANEL_COPYDIRECTORIES =
        new JScrollPane(
            TEXTAREA_COPYDIRECTORIES,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    COPYDIRECTORIES_JPANEL.add(
        SCROLL_PANEL_COPYDIRECTORIES, TEXTAREA_COPYDIRECTORIES_JPANEL_CONSTRAINTS);
    COPYDIRECTORIES_JPANEL.setSize(100, 100);
    COPYDIRECTORIES_JPANEL.setPreferredSize(new Dimension(100, 100));
    COPYDIRECTORIES_JPANEL.setMaximumSize(new Dimension(100, 100));
    COPYDIRECTORIES_JPANEL.setMinimumSize(new Dimension(100, 100));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 5;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(COPYDIRECTORIES_JPANEL, GRIDBAGCONSTRAINTS);

    // Labels and textfields server-icon.png and server.properties paths
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridwidth = 2;

    JLabel labelServerIconPath =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labeliconpath"));
    labelServerIconPath.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labeliconpath.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 6;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelServerIconPath, GRIDBAGCONSTRAINTS);

    TEXTFIELD_SERVERICONPATH.setText("");
    TEXTFIELD_SERVERICONPATH.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath"));
    ERROR_ICON_SERVERICON.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath.error"));
    TEXTFIELD_SERVERICONPATH.addDocumentListener(
        (SimpleDocumentListener) e -> validateServerIcon());

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 7;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERICONPATH, GRIDBAGCONSTRAINTS);

    JLabel labelServerPropertiesPath =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelpropertiespath"));
    labelServerPropertiesPath.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelpropertiespath.tip"));

    GRIDBAGCONSTRAINTS.gridx = 3;
    GRIDBAGCONSTRAINTS.gridy = 6;
    GRIDBAGCONSTRAINTS.insets = new Insets(20, -190, 0, 0);

    CREATESERVERPACKPANEL.add(labelServerPropertiesPath, GRIDBAGCONSTRAINTS);

    TEXTFIELD_SERVERPROPERTIESPATH.setText("");
    TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath"));
    ERROR_ICON_SERVERPROPERTIES.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath.error"));
    TEXTFIELD_SERVERPROPERTIESPATH.addDocumentListener(
        (SimpleDocumentListener) e -> validateServerProperties());

    GRIDBAGCONSTRAINTS.gridx = 3;
    GRIDBAGCONSTRAINTS.gridy = 7;
    GRIDBAGCONSTRAINTS.insets = new Insets(0, -190, 0, 0);

    CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPROPERTIESPATH, GRIDBAGCONSTRAINTS);

    // Label and textfield javaPath
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridwidth = 5;

    JLabel labelJavaPath =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labeljavapath"));
    labelJavaPath.setToolTipText(
        "<html>"
            + I18N.getMessage("createserverpack.gui.createserverpack.labeljavapath.tip")
            + "</html>");

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 8;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelJavaPath, GRIDBAGCONSTRAINTS);

    TEXTFIELD_JAVAPATH.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labeljavapath.tip"));
    ERROR_ICON_JAVAPATH.setDescription(
        I18N.getMessage("createserverpack.gui.createserverpack.textjavapath.error"));
    TEXTFIELD_JAVAPATH.addDocumentListener((SimpleDocumentListener) e -> validateJavaPath());

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 9;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(TEXTFIELD_JAVAPATH, GRIDBAGCONSTRAINTS);

    GRIDBAGCONSTRAINTS.gridwidth = 1;

    // Label and combobox minecraftVersion
    JLabel labelMinecraftVersion =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelminecraft"));
    labelMinecraftVersion.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelminecraft.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 10;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelMinecraftVersion, GRIDBAGCONSTRAINTS);

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

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 11;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(COMBOBOX_MINECRAFTVERSIONS, GRIDBAGCONSTRAINTS);

    // Label and radio buttons Modloader
    JLabel labelModloader =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodloader"));
    labelModloader.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodloader.tip"));

    GRIDBAGCONSTRAINTS.gridx = 1;
    GRIDBAGCONSTRAINTS.gridy = 10;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelModloader, GRIDBAGCONSTRAINTS);

    COMBOBOX_MODLOADERS.setModel(
        new DefaultComboBoxModel<>(APPLICATIONPROPERTIES.SUPPORTED_MODLOADERS()));
    if (COMBOBOX_MODLOADERS.getSelectedItem() == null) {
      COMBOBOX_MODLOADERS.setSelectedIndex(0);
    }
    COMBOBOX_MODLOADERS.addActionListener(this::actionEventComboBoxModloaders);

    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;
    GRIDBAGCONSTRAINTS.gridx = 1;
    GRIDBAGCONSTRAINTS.gridy = 11;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(COMBOBOX_MODLOADERS, GRIDBAGCONSTRAINTS);

    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridwidth = 2;

    // Label and textfield modloaderVersion
    JLabel labelModloaderVersion =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.labelmodloaderversion"));
    labelModloaderVersion.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));

    GRIDBAGCONSTRAINTS.gridx = 2;
    GRIDBAGCONSTRAINTS.gridy = 10;
    GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(labelModloaderVersion, GRIDBAGCONSTRAINTS);

    COMBOBOX_MODLOADER_VERSIONS.setModel(FABRIC_VERSIONS);
    COMBOBOX_MODLOADER_VERSIONS.setSelectedIndex(0);
    COMBOBOX_MODLOADER_VERSIONS.addActionListener(this::actionEventComboBoxModloaderVersions);
    COMBOBOX_MODLOADER_VERSIONS.setVisible(true);

    GRIDBAGCONSTRAINTS.gridx = 2;
    GRIDBAGCONSTRAINTS.gridy = 11;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(COMBOBOX_MODLOADER_VERSIONS, GRIDBAGCONSTRAINTS);

    // ----------------------------------------------------------------LABELS AND CHECKBOXES--------

    GRIDBAGCONSTRAINTS.insets = new Insets(10, 10, 0, 0);
    GRIDBAGCONSTRAINTS.gridwidth = 1;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.SOUTHWEST;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;

    // Checkbox installServer
    CHECKBOX_SERVER =
        new JCheckBox(
            I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver"), true);
    CHECKBOX_SERVER.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.tip"));
    CHECKBOX_SERVER.addActionListener(this::actionEventCheckBoxServer);

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 14;

    CREATESERVERPACKPANEL.add(CHECKBOX_SERVER, GRIDBAGCONSTRAINTS);

    // Checkbox copyIcon
    CHECKBOX_ICON =
        new JCheckBox(I18N.getMessage("createserverpack.gui.createserverpack.checkboxicon"), true);
    CHECKBOX_ICON.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxicon.tip"));

    GRIDBAGCONSTRAINTS.gridx = 1;
    GRIDBAGCONSTRAINTS.gridy = 14;

    CREATESERVERPACKPANEL.add(CHECKBOX_ICON, GRIDBAGCONSTRAINTS);

    // Checkbox copyProperties
    CHECKBOX_PROPERTIES =
        new JCheckBox(
            I18N.getMessage("createserverpack.gui.createserverpack.checkboxproperties"), true);
    CHECKBOX_PROPERTIES.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxproperties.tip"));

    GRIDBAGCONSTRAINTS.gridx = 2;
    GRIDBAGCONSTRAINTS.gridy = 14;

    CREATESERVERPACKPANEL.add(CHECKBOX_PROPERTIES, GRIDBAGCONSTRAINTS);

    // Checkbox createZIP
    CHECKBOX_ZIP =
        new JCheckBox(I18N.getMessage("createserverpack.gui.createserverpack.checkboxzip"), true);
    CHECKBOX_ZIP.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.checkboxzip.tip"));

    GRIDBAGCONSTRAINTS.gridx = 3;
    GRIDBAGCONSTRAINTS.gridy = 14;

    CREATESERVERPACKPANEL.add(CHECKBOX_ZIP, GRIDBAGCONSTRAINTS);

    JLabel labelJavaArgs =
        new JLabel(I18N.getMessage("createserverpack.gui.createserverpack.javaargs"));
    labelJavaArgs.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.javaargs.tip"));

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 15;
    GRIDBAGCONSTRAINTS.gridwidth = 5;
    GRIDBAGCONSTRAINTS.insets = new Insets(20, 10, -19, 0);

    CREATESERVERPACKPANEL.add(labelJavaArgs, GRIDBAGCONSTRAINTS);

    TEXTAREA_JAVAARGS.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.javaargs.tip"));
    TEXTAREA_JAVAARGS.setFont(new Font("Noto Sans Display Regular", Font.PLAIN, 15));
    JPanel JAVAARGS_JPANEL = new JPanel();
    JAVAARGS_JPANEL.setLayout(new GridBagLayout());
    GridBagConstraints TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS = new GridBagConstraints();
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.anchor = GridBagConstraints.CENTER;
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.fill = GridBagConstraints.BOTH;
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.gridx = 0;
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.gridy = 0;
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.weighty = 1;
    TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS.weightx = 1;

    JScrollPane SCROLL_PANEL_JAVAARGS =
        new JScrollPane(
            TEXTAREA_JAVAARGS,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JAVAARGS_JPANEL.add(SCROLL_PANEL_JAVAARGS, TEXTAREA_JAVAARGS_JPANEL_CONSTRAINTS);
    JAVAARGS_JPANEL.setSize(100, 100);
    JAVAARGS_JPANEL.setPreferredSize(new Dimension(100, 100));
    JAVAARGS_JPANEL.setMaximumSize(new Dimension(100, 100));
    JAVAARGS_JPANEL.setMinimumSize(new Dimension(100, 100));

    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridwidth = 4;
    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 16;
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

    CREATESERVERPACKPANEL.add(JAVAARGS_JPANEL, GRIDBAGCONSTRAINTS);

    // ------------------------------------------------------------------------------BUTTONS--------

    GRIDBAGCONSTRAINTS.gridwidth = 1;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
    Insets ZERO_TEN_ZERO_TEN = new Insets(0, 10, 0, 10);
    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_TEN;
    GRIDBAGCONSTRAINTS.weightx = 0;
    GRIDBAGCONSTRAINTS.weighty = 0;

    JButton BUTTON_MODPACKDIRECTORY = new JButton();
    BUTTON_MODPACKDIRECTORY.setToolTipText(
        I18N.getMessage("createserverpack.gui.buttonmodpackdir"));
    BUTTON_MODPACKDIRECTORY.setContentAreaFilled(false);
    BUTTON_MODPACKDIRECTORY.setMultiClickThreshhold(1000);
    ImageIcon FOLDERICON =
        new ImageIcon(
            Objects.requireNonNull(
                ServerPackCreatorGui.class.getResource("/de/griefed/resources/gui/folder.png")));
    BUTTON_MODPACKDIRECTORY.setIcon(FOLDERICON);
    Dimension FOLDERBUTTONDIMENSION = new Dimension(24, 24);
    BUTTON_MODPACKDIRECTORY.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_MODPACKDIRECTORY.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_MODPACKDIRECTORY.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_MODPACKDIRECTORY.addActionListener(this::selectModpackDirectory);
    addMouseListenerContentAreaFilledToButton(BUTTON_MODPACKDIRECTORY);

    GRIDBAGCONSTRAINTS.gridx = 3;
    GRIDBAGCONSTRAINTS.gridy = 1;

    CREATESERVERPACKPANEL.add(BUTTON_MODPACKDIRECTORY, GRIDBAGCONSTRAINTS);

    JButton BUTTON_CLIENTSIDEMODS = new JButton();
    BUTTON_CLIENTSIDEMODS.setToolTipText(I18N.getMessage("createserverpack.gui.buttonclientmods"));
    BUTTON_CLIENTSIDEMODS.setContentAreaFilled(false);
    BUTTON_CLIENTSIDEMODS.setMultiClickThreshhold(1000);
    BUTTON_CLIENTSIDEMODS.setIcon(FOLDERICON);
    BUTTON_CLIENTSIDEMODS.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_CLIENTSIDEMODS.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_CLIENTSIDEMODS.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_CLIENTSIDEMODS.addActionListener(this::selectClientMods);
    addMouseListenerContentAreaFilledToButton(BUTTON_CLIENTSIDEMODS);

    GRIDBAGCONSTRAINTS.gridx = 5;
    GRIDBAGCONSTRAINTS.gridy = 3;

    CREATESERVERPACKPANEL.add(BUTTON_CLIENTSIDEMODS, GRIDBAGCONSTRAINTS);

    JButton BUTTON_COPYDIRECTORIES = new JButton();
    BUTTON_COPYDIRECTORIES.setToolTipText(I18N.getMessage("createserverpack.gui.buttoncopydirs"));
    BUTTON_COPYDIRECTORIES.setContentAreaFilled(false);
    BUTTON_COPYDIRECTORIES.setIcon(FOLDERICON);
    BUTTON_COPYDIRECTORIES.setMultiClickThreshhold(1000);
    BUTTON_COPYDIRECTORIES.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_COPYDIRECTORIES.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_COPYDIRECTORIES.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_COPYDIRECTORIES.addActionListener(this::selectCopyDirs);
    addMouseListenerContentAreaFilledToButton(BUTTON_COPYDIRECTORIES);

    GRIDBAGCONSTRAINTS.gridx = 5;
    GRIDBAGCONSTRAINTS.gridy = 5;

    CREATESERVERPACKPANEL.add(BUTTON_COPYDIRECTORIES, GRIDBAGCONSTRAINTS);

    JButton BUTTON_JAVAPATH = new JButton();
    BUTTON_JAVAPATH.setToolTipText(I18N.getMessage("createserverpack.gui.buttonjavapath"));
    BUTTON_JAVAPATH.setContentAreaFilled(false);
    BUTTON_JAVAPATH.setIcon(FOLDERICON);
    BUTTON_JAVAPATH.setMultiClickThreshhold(1000);
    BUTTON_JAVAPATH.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_JAVAPATH.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_JAVAPATH.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_JAVAPATH.addActionListener(this::selectJavaInstallation);
    addMouseListenerContentAreaFilledToButton(BUTTON_JAVAPATH);

    GRIDBAGCONSTRAINTS.gridx = 5;
    GRIDBAGCONSTRAINTS.gridy = 9;

    CREATESERVERPACKPANEL.add(BUTTON_JAVAPATH, GRIDBAGCONSTRAINTS);

    JButton BUTTON_SERVERICON = new JButton();
    BUTTON_SERVERICON.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.icon"));
    BUTTON_SERVERICON.setContentAreaFilled(false);
    BUTTON_SERVERICON.setIcon(FOLDERICON);
    BUTTON_SERVERICON.setMultiClickThreshhold(1000);

    BUTTON_SERVERICON.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERICON.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERICON.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERICON.addActionListener(this::selectServerIcon);
    addMouseListenerContentAreaFilledToButton(BUTTON_SERVERICON);
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.LINE_START;
    GRIDBAGCONSTRAINTS.weightx = 0;
    GRIDBAGCONSTRAINTS.gridx = 2;
    GRIDBAGCONSTRAINTS.gridy = 7;

    CREATESERVERPACKPANEL.add(BUTTON_SERVERICON, GRIDBAGCONSTRAINTS);

    JButton BUTTON_SERVERPROPERTIES = new JButton();
    BUTTON_SERVERPROPERTIES.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.properties"));
    BUTTON_SERVERPROPERTIES.setContentAreaFilled(false);
    BUTTON_SERVERPROPERTIES.setIcon(FOLDERICON);
    BUTTON_SERVERPROPERTIES.setMultiClickThreshhold(1000);
    BUTTON_SERVERPROPERTIES.setMinimumSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERPROPERTIES.setPreferredSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERPROPERTIES.setMaximumSize(FOLDERBUTTONDIMENSION);
    BUTTON_SERVERPROPERTIES.addActionListener(this::selectServerProperties);
    addMouseListenerContentAreaFilledToButton(BUTTON_SERVERPROPERTIES);

    GRIDBAGCONSTRAINTS.gridx = 5;
    GRIDBAGCONSTRAINTS.gridy = 7;

    CREATESERVERPACKPANEL.add(BUTTON_SERVERPROPERTIES, GRIDBAGCONSTRAINTS);

    JButton BUTTON_AIKARS_FLAGS = new JButton();
    BUTTON_AIKARS_FLAGS.setToolTipText(
        I18N.getMessage("createserverpack.gui.createserverpack.button.properties"));
    BUTTON_AIKARS_FLAGS.setContentAreaFilled(false);
    BUTTON_AIKARS_FLAGS.setIcon(
        new RotatedIcon(
            new TextIcon(
                BUTTON_AIKARS_FLAGS,
                I18N.getMessage("createserverpack.gui.createserverpack.javaargs.aikar"),
                TextIcon.Layout.HORIZONTAL),
            RotatedIcon.Rotate.UP));
    BUTTON_AIKARS_FLAGS.setMultiClickThreshhold(1000);
    // BUTTON_AIKARS_FLAGS.setBorder(BorderFactory.createLineBorder(PRIMARY, 1));
    BUTTON_AIKARS_FLAGS.setMargin(new Insets(20, 20, 20, 20));
    BUTTON_AIKARS_FLAGS.addActionListener(this::setAikarsFlags);
    addMouseListenerContentAreaFilledToButton(BUTTON_AIKARS_FLAGS);

    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
    GRIDBAGCONSTRAINTS.gridx = 5;
    GRIDBAGCONSTRAINTS.gridy = 16;

    CREATESERVERPACKPANEL.add(BUTTON_AIKARS_FLAGS, GRIDBAGCONSTRAINTS);

    // ---------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------

    GRIDBAGCONSTRAINTS.weightx = 0;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
    GRIDBAGCONSTRAINTS.insets = new Insets(5, 0, 5, 0);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

    ReticulatingSplines RETICULATOR = new ReticulatingSplines();
    STATUS_LABEL_LINE_0 = new JLabel("..." + RETICULATOR.reticulate() + "   ");
    STATUS_LABEL_LINE_1 = new JLabel("..." + RETICULATOR.reticulate() + "   ");
    STATUS_LABEL_LINE_2 = new JLabel("..." + RETICULATOR.reticulate() + "   ");
    STATUS_LABEL_LINE_3 = new JLabel("..." + RETICULATOR.reticulate() + "   ");
    STATUS_LABEL_LINE_4 = new JLabel("..." + RETICULATOR.reticulate() + "   ");
    STATUS_LABEL_LINE_5 =
        new JLabel(I18N.getMessage("createserverpack.gui.buttongenerateserverpack.ready") + "   ");

    // Make sure all labels start on the left and extrend to the right
    STATUS_LABEL_LINE_0.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_1.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_2.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_3.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_4.setHorizontalAlignment(JLabel.LEFT);
    STATUS_LABEL_LINE_5.setHorizontalAlignment(JLabel.LEFT);

    // Set the preferred size of the labels so they do not resize when long texts are set later on
    Dimension labelDimension = new Dimension(700, 30);
    STATUS_LABEL_LINE_0.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_1.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_2.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_3.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_4.setPreferredSize(labelDimension);
    STATUS_LABEL_LINE_5.setPreferredSize(labelDimension);

    updatePanelTheme();

    statusPanel.add(STATUS_LABEL_LINE_0);
    statusPanel.add(STATUS_LABEL_LINE_1);
    statusPanel.add(STATUS_LABEL_LINE_2);
    statusPanel.add(STATUS_LABEL_LINE_3);
    statusPanel.add(STATUS_LABEL_LINE_4);
    statusPanel.add(STATUS_LABEL_LINE_5);
    statusPanel.setPreferredSize(new Dimension(700, 140));

    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
    GRIDBAGCONSTRAINTS.gridx = 1;
    GRIDBAGCONSTRAINTS.gridy = 19;
    GRIDBAGCONSTRAINTS.gridwidth = 5;
    GRIDBAGCONSTRAINTS.gridheight = 2;
    GRIDBAGCONSTRAINTS.weightx = 1;
    GRIDBAGCONSTRAINTS.weighty = 1;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;

    CREATESERVERPACKPANEL.add(statusPanel, GRIDBAGCONSTRAINTS);

    GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_TEN;

    BufferedImage GENERATE =
        ImageIO.read(
            Objects.requireNonNull(
                ServerPackCreatorGui.class.getResource(
                    "/de/griefed/resources/gui/start_generation.png")));
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

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 19;
    GRIDBAGCONSTRAINTS.gridwidth = 1;
    GRIDBAGCONSTRAINTS.gridheight = 1;
    GRIDBAGCONSTRAINTS.weightx = 1;
    GRIDBAGCONSTRAINTS.weighty = 1;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;

    CREATESERVERPACKPANEL.add(BUTTON_GENERATESERVERPACK, GRIDBAGCONSTRAINTS);

    JButton BUTTON_SERVER_PACKS = new JButton();
    BUTTON_SERVER_PACKS.setIcon(
        new CompoundIcon(
            CompoundIcon.Axis.X_AXIS,
            8,
            FOLDERICON,
            new TextIcon(
                BUTTON_SERVER_PACKS, I18N.getMessage("createserverpack.gui.buttonserverpacks"))));
    BUTTON_SERVER_PACKS.addActionListener(this::openServerPacksFolder);
    BUTTON_SERVER_PACKS.setMultiClickThreshhold(1000);
    BUTTON_SERVER_PACKS.setToolTipText(
        I18N.getMessage("createserverpack.gui.buttonserverpacks.tip"));

    GRIDBAGCONSTRAINTS.gridy = 20;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;

    CREATESERVERPACKPANEL.add(BUTTON_SERVER_PACKS, GRIDBAGCONSTRAINTS);

    GRIDBAGCONSTRAINTS.gridx = 0;
    GRIDBAGCONSTRAINTS.gridy = 21;
    GRIDBAGCONSTRAINTS.gridwidth = 6;
    GRIDBAGCONSTRAINTS.weightx = 1;
    GRIDBAGCONSTRAINTS.weighty = 1;
    GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.NORTH;
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;

    STATUS_BAR.setPreferredSize(new Dimension(700, 40));
    STATUS_BAR.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

    CREATESERVERPACKPANEL.add(STATUS_BAR, GRIDBAGCONSTRAINTS);

    STATUS_BAR.loadConfig(IDLE_CONFIG);
    STATUS_BAR.play();

    // --------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
    GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;

    JScrollPane TAB_CREATESERVERPACKTAB_SCROLL_PANEL = new JScrollPane(CREATESERVERPACKPANEL);
    TAB_CREATESERVERPACKTAB_SCROLL_PANEL.getVerticalScrollBar().setUnitIncrement(16);

    setLayout(new BorderLayout());
    add(TAB_CREATESERVERPACKTAB_SCROLL_PANEL, BorderLayout.CENTER);

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
    this.STATUS_LABEL_LINE_5.setText(text + "   ");
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

    validateJavaPath();
  }

  /**
   * Validate the input field for modpack directory.
   *
   * @author Griefed
   */
  private void validateModpackDir() {
    List<String> errorsTEXTFIELD_MODPACKDIRECTORY = new ArrayList<>();
    if (CONFIGURATIONHANDLER.checkModpackDir(
        TEXTFIELD_MODPACKDIRECTORY.getText(), errorsTEXTFIELD_MODPACKDIRECTORY)) {

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

    validateCopyDirs();
  }

  /**
   * Validate the input field for server pack suffix
   *
   * @author Griefed
   */
  private void validateSuffix() {
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
  }

  /**
   * Validate the input field for client mods.
   *
   * @author Griefed
   */
  private void validateClientMods() {
    if (!TEXTAREA_CLIENTSIDEMODS.getText().matches("^.*,\\s*\\\\*$")) {

      TEXTAREA_CLIENTSIDEMODS.setIcon(null);
      TEXTAREA_CLIENTSIDEMODS.setToolTipText(
          I18N.getMessage("createserverpack.gui.createserverpack.labelclientmods.tip"));

      TEXTAREA_CLIENTSIDEMODS.setForeground(getThemeTextColor());

    } else {

      TEXTAREA_CLIENTSIDEMODS.setForeground(getThemeErrorColor());
      TEXTAREA_CLIENTSIDEMODS.setIcon(ERROR_ICON_CLIENTSIDE_MODS);
      TEXTAREA_CLIENTSIDEMODS.setToolTipText(ERROR_ICON_CLIENTSIDE_MODS.getDescription());
    }
  }

  /**
   * Validate the input field for copy directories.
   *
   * @author Griefed
   */
  private void validateCopyDirs() {
    if (CONFIGURATIONHANDLER.checkModpackDir(TEXTFIELD_MODPACKDIRECTORY.getText())) {
      List<String> errorsTEXTFIELD_COPYDIRECTORIES = new ArrayList<>();
      if (!TEXTAREA_COPYDIRECTORIES.getText().matches("^.*,\\s*\\\\*$")
          && CONFIGURATIONHANDLER.checkCopyDirs(
          UTILITIES.ListUtils()
              .cleanList(
                  new ArrayList<>(
                      Arrays.asList(
                          TEXTAREA_COPYDIRECTORIES.getText().replace(", ", ",").split(",")))),
          TEXTFIELD_MODPACKDIRECTORY.getText(),
          errorsTEXTFIELD_COPYDIRECTORIES)) {

        TEXTAREA_COPYDIRECTORIES.setIcon(null);
        TEXTAREA_COPYDIRECTORIES.setToolTipText(
            I18N.getMessage("createserverpack.gui.createserverpack.labelcopydirs.tip"));

        TEXTAREA_COPYDIRECTORIES.setForeground(getThemeTextColor());

      } else {

        TEXTAREA_COPYDIRECTORIES.setForeground(getThemeErrorColor());
        TEXTAREA_COPYDIRECTORIES.setIcon(ERROR_ICON_COPYDIRECTORIES);
        ERROR_ICON_COPYDIRECTORIES.setDescription(
            String.join(",", errorsTEXTFIELD_COPYDIRECTORIES));
        TEXTAREA_COPYDIRECTORIES.setToolTipText(String.join(",", errorsTEXTFIELD_COPYDIRECTORIES));
      }
    }
  }

  /**
   * Validate the input field for server icon.
   *
   * @author Griefed
   */
  private void validateServerIcon() {
    if (CONFIGURATIONHANDLER.checkIconAndProperties(TEXTFIELD_SERVERICONPATH.getText())) {

      TEXTFIELD_SERVERICONPATH.setIcon(null);
      TEXTFIELD_SERVERICONPATH.setToolTipText(
          I18N.getMessage("createserverpack.gui.createserverpack.textfield.iconpath"));

      TEXTFIELD_SERVERICONPATH.setForeground(getThemeTextColor());

    } else {

      TEXTFIELD_SERVERICONPATH.setForeground(getThemeErrorColor());
      TEXTFIELD_SERVERICONPATH.setIcon(ERROR_ICON_SERVERICON);
      TEXTFIELD_SERVERICONPATH.setToolTipText(ERROR_ICON_SERVERICON.getDescription());
    }
  }

  /**
   * Validate the inputfield for server properties.
   *
   * @author Griefed
   */
  private void validateServerProperties() {
    if (CONFIGURATIONHANDLER.checkIconAndProperties(TEXTFIELD_SERVERPROPERTIESPATH.getText())) {
      TEXTFIELD_SERVERPROPERTIESPATH.setIcon(null);
      TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(
          I18N.getMessage("createserverpack.gui.createserverpack.textfield.propertiespath"));

      TEXTFIELD_SERVERPROPERTIESPATH.setForeground(getThemeTextColor());

    } else {

      TEXTFIELD_SERVERPROPERTIESPATH.setForeground(getThemeErrorColor());
      TEXTFIELD_SERVERPROPERTIESPATH.setIcon(ERROR_ICON_SERVERPROPERTIES);
      TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(ERROR_ICON_SERVERPROPERTIES.getDescription());
    }
  }

  /**
   * Validate the inputfield for Java path.
   *
   * @author Griefed
   */
  private void validateJavaPath() {
    if (CONFIGURATIONHANDLER.checkJavaPath(TEXTFIELD_JAVAPATH.getText())) {

      TEXTFIELD_JAVAPATH.setIcon(null);
      TEXTFIELD_JAVAPATH.setToolTipText(
          I18N.getMessage("createserverpack.gui.createserverpack.labeljavapath.tip"));
      TEXTFIELD_JAVAPATH.setForeground(getThemeTextColor());

    } else {

      TEXTFIELD_JAVAPATH.setForeground(getThemeErrorColor());
      TEXTFIELD_JAVAPATH.setIcon(ERROR_ICON_JAVAPATH);
      TEXTFIELD_JAVAPATH.setToolTipText(ERROR_ICON_JAVAPATH.getDescription());
    }
  }

  /**
   * Check the current content of the Java args textarea. If it is empty, set it to Aikars flags,
   * else ask the user whether they want to overwrite the current setting with Aikars flags.
   *
   * @param actionEvent The event which triggers this method.
   * @author Griefed
   */
  private void setAikarsFlags(ActionEvent actionEvent) {
    if (!TEXTAREA_JAVAARGS.getText().isEmpty()) {
      switch (JOptionPane.showConfirmDialog(
          CREATESERVERPACKPANEL,
          I18N.getMessage("createserverpack.gui.createserverpack.javaargs.confirm.message"),
          I18N.getMessage("createserverpack.gui.createserverpack.javaargs.confirm.title"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE)) {
        case 0:
          setAikarsFlags();

        case 1:
        default:
          break;
      }

    } else {

      setAikarsFlags();
    }
  }

  /**
   * Sets the text of the Java args textarea to the popular Aikar flags.
   *
   * @author Griefed
   */
  private void setAikarsFlags() {
    TEXTAREA_JAVAARGS.setText(APPLICATIONPROPERTIES.getAikarsFlags());
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
    if (CHECKBOX_SERVER.isSelected() && TEXTFIELD_JAVAPATH.getText().isEmpty()) {
      switch (JOptionPane.showConfirmDialog(
          CREATESERVERPACKPANEL,
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.message"),
          I18N.getMessage("createserverpack.gui.createserverpack.checkboxserver.confirm.title"),
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE)) {
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
              null);
          break;

        default:
          break;
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
   * Get the DefaultComboboxModel of available Forge versions depending on the passed Minecraft
   * version. If the specified Minecraft version is not supported, the DCBM will be set to NONE.
   *
   * @param chosenMinecraftVersion The selected Minecraft version which determines the list of
   *                               available Forge versions.
   * @author Griefed
   */
  private DefaultComboBoxModel<String> updateForgeComboBoxVersions(
      @Nullable String chosenMinecraftVersion) {

    if (chosenMinecraftVersion != null
        && VERSIONMETA
        .forge()
        .availableForgeVersionsArrayDescending(chosenMinecraftVersion)
        .isPresent()) {

      return new DefaultComboBoxModel<>(
          VERSIONMETA.forge().availableForgeVersionsArrayDescending(chosenMinecraftVersion).get());

    } else {

      return new DefaultComboBoxModel<>(this.NONE);
    }
  }

  /**
   * Get the DefaultComboboxModel of available Fabric versions depending on the passed Minecraft
   * version. If the specified Minecraft version is not supported, the DCBM will be set to NONE.
   *
   * @param chosenMinecraftVersion The selected Minecraft version which determines the list of
   *                               available Fabric versions.
   * @author Griefed
   */
  private DefaultComboBoxModel<String> updateFabricComboBoxVersions(
      @Nullable String chosenMinecraftVersion) {

    if (chosenMinecraftVersion != null
        && VERSIONMETA.fabric().isMinecraftSupported(chosenMinecraftVersion)) {

      return FABRIC_VERSIONS;

    } else {

      return new DefaultComboBoxModel<>(this.NONE);
    }
  }

  /**
   * Get the DefaultComboboxModel of available LegacyFabric versions depending on the passed
   * Minecraft version. If the specified Minecraft version is not supported, the DCBM will be set to
   * NONE.
   *
   * @param chosenMinecraftVersion The selected Minecraft version which determines the list of
   *                               available LegacyFabric versions.
   * @author Griefed
   */
  private DefaultComboBoxModel<String> updateLegacyFabricComboBoxVersions(
      @Nullable String chosenMinecraftVersion) {

    if (chosenMinecraftVersion != null
        && VERSIONMETA.legacyFabric().isMinecraftSupported(chosenMinecraftVersion)) {

      return LEGACY_FABRIC_VERSIONS;

    } else {

      return new DefaultComboBoxModel<>(this.NONE);
    }
  }

  /**
   * Get the DefaultComboboxModel of available Quilt versions depending on the passed Minecraft
   * version. If the specified Minecraft version is not supported, the DCBM will be set to NONE.
   *
   * @param chosenMinecraftVersion The selected Minecraft version which determines the list of
   *                               available Fabric versions.
   * @author Griefed
   */
  private DefaultComboBoxModel<String> updateQuiltComboBoxVersions(
      @Nullable String chosenMinecraftVersion) {

    if (chosenMinecraftVersion != null
        && VERSIONMETA.fabric().isMinecraftSupported(chosenMinecraftVersion)) {

      return QUILT_VERSIONS;

    } else {

      return new DefaultComboBoxModel<>(this.NONE);
    }
  }

  /**
   * Setter for the Minecraft version depending on which one is selected in the GUI.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void actionEventComboBoxMinecraftVersion(ActionEvent event) {

    chosenMinecraftVersion =
        Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString();

    updateModloaderGuiComponents(
        Objects.requireNonNull(COMBOBOX_MODLOADERS.getSelectedItem()).toString());
  }

  private void actionEventComboBoxModloaderVersions(ActionEvent event) {

    chosenModloaderVersion =
        Objects.requireNonNull(COMBOBOX_MODLOADER_VERSIONS.getSelectedItem()).toString();
  }

  /**
   * Setter for the modloader depending on which one is selected in the GUI.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void actionEventComboBoxModloaders(ActionEvent event) {

    String modloader;
    if (COMBOBOX_MODLOADERS.getSelectedItem() != null) {
      modloader = COMBOBOX_MODLOADERS.getSelectedItem().toString();
    } else {
      modloader = "Forge";
    }

    updateModloaderGuiComponents(modloader);
  }

  /**
   * Helper method which changes various states of GUI components.
   *
   * @param modloader The modloader to set.
   * @author Griefed
   */
  private void updateModloaderGuiComponents(String modloader) {

    switch (modloader) {
      // Fabric
      case "Fabric":
        COMBOBOX_MODLOADERS.setSelectedIndex(0);
        //noinspection ConstantConditions
        COMBOBOX_MODLOADER_VERSIONS.setModel(
            updateFabricComboBoxVersions(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));

        break;

      // Forge
      case "Forge":
        COMBOBOX_MODLOADERS.setSelectedIndex(1);
        //noinspection ConstantConditions
        COMBOBOX_MODLOADER_VERSIONS.setModel(
            updateForgeComboBoxVersions(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));

        break;

      // Quilt
      case "Quilt":
        COMBOBOX_MODLOADERS.setSelectedIndex(2);
        //noinspection ConstantConditions
        COMBOBOX_MODLOADER_VERSIONS.setModel(
            updateQuiltComboBoxVersions(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));

        break;

      case "LegacyFabric":
        COMBOBOX_MODLOADERS.setSelectedIndex(3);
        //noinspection ConstantConditions
        COMBOBOX_MODLOADER_VERSIONS.setModel(
            updateLegacyFabricComboBoxVersions(
                COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));

    }

    COMBOBOX_MODLOADER_VERSIONS.setSelectedIndex(0);
    //noinspection ConstantConditions
    chosenModloaderVersion = COMBOBOX_MODLOADER_VERSIONS.getSelectedItem().toString();
    chosenModloader = modloader;

    //noinspection ConstantConditions
    LOG.debug(
        "Minecraft version: "
            + COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()
            + "; Modloader: "
            + COMBOBOX_MODLOADERS.getSelectedItem().toString()
            + "; Modloader version "
            + COMBOBOX_MODLOADER_VERSIONS.getSelectedItem().toString());
  }

  /**
   * Upon button-press, open a file-selector for the modpack-directory.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectModpackDirectory(ActionEvent event) {

    JFileChooser modpackDirChooser = new JFileChooser();

    if (new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory()) {

      modpackDirChooser.setCurrentDirectory(new File(TEXTFIELD_MODPACKDIRECTORY.getText()));

    } else if (new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isFile()) {

      modpackDirChooser.setCurrentDirectory(
          new File(new File(TEXTFIELD_MODPACKDIRECTORY.getText()).getParent()));

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
        TEXTFIELD_MODPACKDIRECTORY.setText(
            modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.debug(
            "Selected modpack directory: "
                + modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

      } catch (IOException ex) {
        LOG.error("Error getting directory from modpack directory chooser.", ex);
      }
    }
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

        TEXTFIELD_SERVERICONPATH.setText(
            serverIconChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

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

        TEXTFIELD_SERVERPROPERTIESPATH.setText(
            serverPropertiesChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

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

    if (TEXTFIELD_MODPACKDIRECTORY.getText().length() > 0
        && new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory()
        && new File(String.format("%s/mods", TEXTFIELD_MODPACKDIRECTORY.getText())).isDirectory()) {

      clientModsChooser.setCurrentDirectory(
          new File(String.format("%s/mods", TEXTFIELD_MODPACKDIRECTORY.getText())));
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

      TEXTAREA_CLIENTSIDEMODS.setText(UTILITIES.StringUtils().buildString(clientModsFilenames));

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

    if (TEXTFIELD_MODPACKDIRECTORY.getText().length() > 0
        && new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory()) {

      copyDirsChooser.setCurrentDirectory(new File(TEXTFIELD_MODPACKDIRECTORY.getText()));
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

      TEXTAREA_COPYDIRECTORIES.setText(UTILITIES.StringUtils().buildString(copyDirsNames));

      LOG.debug("Selected directories: " + copyDirsNames);
    }
  }

  /**
   * Upon button-press, open a file-selector to select the Java-executable.
   *
   * @param event The event which triggers this method.
   * @author Griefed
   */
  private void selectJavaInstallation(ActionEvent event) {
    chooseJava();
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
        TEXTFIELD_JAVAPATH.setText(
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

    if (TEXTAREA_COPYDIRECTORIES.getText().equals("lazy_mode")) {
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
  private ConfigurationModel currentConfigAsModel() {
    return new ConfigurationModel(
        UTILITIES.ListUtils()
            .cleanList(
                new ArrayList<>(
                    Arrays.asList(
                        TEXTAREA_CLIENTSIDEMODS.getText().replace(", ", ",").split(",")))),
        UTILITIES.ListUtils()
            .cleanList(
                new ArrayList<>(
                    Arrays.asList(
                        TEXTAREA_COPYDIRECTORIES.getText().replace(", ", ",").split(",")))),
        TEXTFIELD_MODPACKDIRECTORY.getText().replace("\\", "/"),
        TEXTFIELD_JAVAPATH.getText().replace("\\", "/"),
        COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString(),
        COMBOBOX_MODLOADERS.getSelectedItem().toString(),
        COMBOBOX_MODLOADER_VERSIONS.getSelectedItem().toString(),
        TEXTAREA_JAVAARGS.getText(),
        UTILITIES.StringUtils().pathSecureText(TEXTFIELD_SERVERPACKSUFFIX.getText()),
        TEXTFIELD_SERVERICONPATH.getText().replace("\\", "/"),
        TEXTFIELD_SERVERPROPERTIESPATH.getText().replace("\\", "/"),
        CHECKBOX_SERVER.isSelected(),
        CHECKBOX_ICON.isSelected(),
        CHECKBOX_PROPERTIES.isSelected(),
        CHECKBOX_ZIP.isSelected(),
        new HashMap<>(),
        new HashMap<>());
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

    List<String> encounteredErrors = new ArrayList<>(100);

    ConfigurationModel configurationModel = currentConfigAsModel();

    final ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.execute(
        () -> {
          if (!CONFIGURATIONHANDLER.checkConfiguration(
              configurationModel, encounteredErrors, true)) {

            LOG.info("Configuration checked successfully.");

            updateStatus(
                I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.checked"));

            saveConfig(APPLICATIONPROPERTIES.DEFAULT_CONFIG());

            LOG.info("Starting ServerPackCreator run.");

            updateStatus(
                I18N.getMessage("createserverpack.log.info.buttoncreateserverpack.generating"));

            try {

              SERVERPACKHANDLER.run(configurationModel);

              loadConfig(new File("serverpackcreator.conf"));

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
                  JOptionPane.INFORMATION_MESSAGE)
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

                errors.append(i + 1).append(": ").append(encounteredErrors.get(i)).append("\n");
              }

              ready();

              JOptionPane.showMessageDialog(
                  FRAME_SERVERPACKCREATOR,
                  errors,
                  String.format(
                      I18N.getMessage("createserverpack.gui.createserverpack.errors.encountered"),
                      encounteredErrors.size()),
                  JOptionPane.ERROR_MESSAGE,
                  UIManager.getIcon("OptionPane.errorIcon"));
            }
          }

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

      TEXTFIELD_MODPACKDIRECTORY.setText(configurationModel.getModpackDir().replace("\\", "/"));

      if (configurationModel.getClientMods().isEmpty()) {

        TEXTAREA_CLIENTSIDEMODS.setText(
            UTILITIES.StringUtils().buildString(APPLICATIONPROPERTIES.getListFallbackMods()));
        LOG.debug("Set clientMods with fallback list.");

      } else {

        TEXTAREA_CLIENTSIDEMODS.setText(
            UTILITIES.StringUtils().buildString(configurationModel.getClientMods()));
      }

      if (configurationModel.getCopyDirs().isEmpty()) {

        TEXTAREA_COPYDIRECTORIES.setText(
            UTILITIES.StringUtils().buildString(APPLICATIONPROPERTIES.getDirectoriesToInclude()));

      } else {

        TEXTAREA_COPYDIRECTORIES.setText(
            UTILITIES.StringUtils()
                .buildString(configurationModel.getCopyDirs().toString().replace("\\", "/")));
      }

      TEXTFIELD_SERVERICONPATH.setText(configurationModel.getServerIconPath().replace("\\", "/"));

      TEXTFIELD_SERVERPROPERTIESPATH.setText(
          configurationModel.getServerPropertiesPath().replace("\\", "/"));

      TEXTFIELD_JAVAPATH.setText(UTILITIES.SystemUtils().acquireJavaPathFromSystem());

      if (configurationModel.getMinecraftVersion().isEmpty()) {
        chosenMinecraftVersion = VERSIONMETA.minecraft().latestRelease().version();
      } else {
        chosenMinecraftVersion = configurationModel.getMinecraftVersion();
      }

      for (int i = 0; i < COMBOBOX_MINECRAFTVERSIONS.getModel().getSize(); i++) {
        if (COMBOBOX_MINECRAFTVERSIONS.getModel().getElementAt(i).equals(chosenMinecraftVersion)) {
          COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
          break;
        }
      }

      // Set modloader and modloader version
      switch (configurationModel.getModLoader().toLowerCase()) {
        case "fabric":
          if (configurationModel.getModLoaderVersion().isEmpty()) {
            configurationModel.setModLoaderVersion(VERSIONMETA.fabric().releaseLoader());
          }

          COMBOBOX_MODLOADERS.setSelectedIndex(0);
          COMBOBOX_MODLOADER_VERSIONS.setModel(
              updateFabricComboBoxVersions(
                  COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));
          break;

        default:
        case "forge":

          if (configurationModel.getModLoaderVersion().isEmpty() && VERSIONMETA.forge()
              .latestForgeVersion(configurationModel.getMinecraftVersion()).isPresent()) {

            configurationModel.setModLoaderVersion(
                VERSIONMETA.forge().latestForgeVersion(configurationModel.getMinecraftVersion())
                    .get());
          }

          COMBOBOX_MODLOADERS.setSelectedIndex(1);
          COMBOBOX_MODLOADER_VERSIONS.setModel(
              updateForgeComboBoxVersions(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));
          break;

        case "quilt":
          if (!configurationModel.getModLoaderVersion().isEmpty()) {
            configurationModel.setModLoaderVersion(VERSIONMETA.quilt().releaseLoader());
          }

          COMBOBOX_MODLOADERS.setSelectedIndex(2);
          COMBOBOX_MODLOADER_VERSIONS.setModel(
              updateQuiltComboBoxVersions(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));
          break;

        case "legacyfabric":
          if (configurationModel.getModLoaderVersion().isEmpty()) {
            configurationModel.setModLoaderVersion(VERSIONMETA.legacyFabric().releaseLoader());
          }

          COMBOBOX_MODLOADERS.setSelectedIndex(3);
          COMBOBOX_MODLOADER_VERSIONS.setModel(
              updateLegacyFabricComboBoxVersions(
                  COMBOBOX_MINECRAFTVERSIONS.getSelectedItem().toString()));
      }

      // Go through all Fabric versions and check if specified version matches official
      // version list
      for (int i = 0; i < COMBOBOX_MODLOADER_VERSIONS.getModel().getSize(); i++) {

        // If match is found, set selected version
        if (COMBOBOX_MODLOADER_VERSIONS.getModel().getElementAt(i)
            .equals(configurationModel.getModLoaderVersion())) {

          COMBOBOX_MODLOADER_VERSIONS.setSelectedIndex(i);
          chosenModloaderVersion = configurationModel.getModLoaderVersion();
          break;
        }
      }

      CHECKBOX_SERVER.setSelected(configurationModel.getIncludeServerInstallation());
      CHECKBOX_ICON.setSelected(configurationModel.getIncludeServerIcon());
      CHECKBOX_PROPERTIES.setSelected(configurationModel.getIncludeServerProperties());
      CHECKBOX_ZIP.setSelected(configurationModel.getIncludeZipCreation());

      TEXTAREA_JAVAARGS.setText(configurationModel.getJavaArgs());

      TEXTFIELD_SERVERPACKSUFFIX.setText(
          UTILITIES.StringUtils().pathSecureText(configurationModel.getServerPackSuffix()));

    } catch (Exception ex) {

      LOG.error("Couldn't load configuration file.", ex);
      JOptionPane.showMessageDialog(this, "Couldn't load configuration file. " + ex.getCause());
    }
  }

  /**
   * Load default values for textfields so the user can start with a new configuration. Just as if
   * ServerPackCreator was started without a serverpackcreator.conf being present.
   *
   * @author Griefed
   */
  protected void clearInterface() {
    TEXTFIELD_MODPACKDIRECTORY.setText("");
    TEXTFIELD_SERVERPACKSUFFIX.setText("");
    TEXTAREA_CLIENTSIDEMODS.setText(
        UTILITIES.StringUtils().buildString(APPLICATIONPROPERTIES.getListFallbackMods()));
    TEXTAREA_COPYDIRECTORIES.setText(
        UTILITIES.StringUtils().buildString(APPLICATIONPROPERTIES.getDirectoriesToInclude()));
    TEXTFIELD_SERVERICONPATH.setText("");
    TEXTFIELD_SERVERPROPERTIESPATH.setText("");
    TEXTFIELD_JAVAPATH.setText(UTILITIES.SystemUtils().acquireJavaPathFromSystem());

    for (int i = 0; i < VERSIONMETA.minecraft().releaseVersionsArrayDescending().length; i++) {

      if (VERSIONMETA
          .minecraft()
          .releaseVersionsArrayDescending()[i]
          .equals(VERSIONMETA.minecraft().latestRelease().version())) {

        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
        chosenMinecraftVersion = VERSIONMETA.minecraft().latestRelease().version();
      }
    }

    updateModloaderGuiComponents("Forge");

    CHECKBOX_SERVER.setSelected(false);
    CHECKBOX_ICON.setSelected(false);
    CHECKBOX_PROPERTIES.setSelected(false);
    CHECKBOX_ZIP.setSelected(false);
    setJavaArgs("");

    validateInputFields();
  }

  /**
   * Get the current themes error-text colour.
   *
   * @return The current themes error-text colour.
   * @author Griefed
   */
  private Color getThemeErrorColor() {
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
  private Color getThemeTextColor() {
    if (APPLICATIONPROPERTIES.isDarkTheme()) {

      return DARKTHEME.getTextColor();

    } else {

      return LIGHTTHEME.getTextColor();
    }
  }

  /**
   * Getter for the chosen modloader from the JRadioButtons.
   *
   * @return String. Returns either Fabric or Forge.
   * @author Griefed
   */
  public String getChosenModloader() {
    return chosenModloader;
  }

  /**
   * Getter for the selected modloader version depending on which modloader is currently selected.
   *
   * @return String. Returns the modloader version depending on which modloader is currently
   * selected.
   * @author Griefed
   */
  private String getSelectedModloaderVersion() {
    return chosenModloaderVersion;
  }

  /**
   * Getter for the currently set JVM flags / Java args.
   *
   * @return String. Returns the currently set JVM flags / Java args.
   * @author Griefed
   */
  public String getJavaArgs() {
    return javaArgs;
  }

  /**
   * Setter for the JVM flags / Java args.
   *
   * @param javaArgs The javaargs to set.
   * @author Griefed
   */
  public void setJavaArgs(String javaArgs) {
    this.javaArgs = javaArgs;
  }

  /**
   * Getter for the current text from the currently set Javapath in the Javapath textfield.
   *
   * @return String. Returns the current text from the currently set Javapath in the Javapath
   * textfield.
   * @author Griefed
   */
  public String getJavaPath() {
    return TEXTFIELD_JAVAPATH.getText();
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
   * Getter for the text in the custom server.properties textfield
   *
   * @return String. Returns the text in the server.properties textfield.
   * @author Griefed
   */
  public String getServerPropertiesPath() {
    return TEXTFIELD_SERVERPROPERTIESPATH.getText();
  }
}
