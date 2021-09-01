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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.AddonsHandler;
import de.griefed.serverpackcreator.ConfigurationHandler;
import de.griefed.serverpackcreator.ConfigurationModel;
import de.griefed.serverpackcreator.ServerPackHandler;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.VersionLister;
import mdlaf.components.textpane.MaterialTextPaneUI;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <strong>Table of methods</strong><p>
 * 1. {@link #TabCreateServerPack(LocalizationManager, ConfigurationHandler, CurseCreateModpack, ServerPackHandler, AddonsHandler, VersionLister, Properties, JFrame)}<br>
 * 2. {@link #actionEventCheckBoxServer(ActionEvent)}<br>
 * 3. {@link #actionEventComboBoxFabricVersions(ActionEvent)}<br>
 * 4. {@link #actionEventComboBoxForgeVersions(ActionEvent)}<br>
 * 5. {@link #actionEventComboBoxMinecraftVersion(ActionEvent)}<br>
 * 6. {@link #addMouseListenerContentAreaFilledToButton(JButton)}<br>
 * 7. {@link #changeForgeVersionListDependingOnMinecraftVersion(String)}<br>
 * 8. {@link #chooseJava()}<br>
 * 9. {@link #createServerPackTab()}<br>
 * 10.{@link #generateServerpack(ActionEvent)}<br>
 * 11.{@link #getChosenModloader()}<br>
 * 12.{@link #getFILE_SERVERPACKCREATOR_PROPERTIES()}<br>
 * 13.{@link #getJavaArgs()}<br>
 * 14.{@link #getSelectedModloaderVersion()}<br>
 * 15.{@link #getSERVER_PACKS_DIR()}<br>
 * 16.{@link #itemEventRadioButtonModloaderFabric(ItemEvent)}<br>
 * 17.{@link #itemEventRadioButtonModloaderForge(ItemEvent)}<br>
 * 18.{@link #loadConfig(File)}<br>
 * 19.{@link #saveConfig(File, boolean)}<br>
 * 20.{@link #selectClientMods(ActionEvent)}<br>
 * 21.{@link #selectCopyDirs(ActionEvent)}<br>
 * 22.{@link #selectJavaInstallation(ActionEvent)}<br>
 * 23.{@link #selectModpackDirectory(ActionEvent)}<br>
 * 24.{@link #setChosenModloader(String)}<br>
 * 25.{@link #setJavaArgs(String)}<br>
 * 26.{@link #updateModloaderGuiComponents(boolean, boolean, String)}<p>
 * This class creates the tab which displays the labels, textfields, buttons and functions in order to create a new
 * server pack. Available are:<br>
 * JLabels and JTextFields for modpackDir, clientMods, copyDirs, javaPath, minecraftVersion, modLoader, modLoaderVersion<br>
 * Checkboxes for Include- serverInstallation, serverIcon, serverProperties, startScripts, ZIP-archive<br>
 * Buttons opening the file explorer and choosing: modpackDir, clientMods, copyDirs, javaPath, loadConfigFromFile<br>
 * A button for displaying an information windows which provides detailed explanation of the important parts of the GUI.<br>
 * The button start the generation of a new server pack.<br>
 * The label under the button to start the generation of a new server pack, which displays the latest log entry of the
 * serverpackcreator.log <em>during</em> the creation of a new server pack.<br>
 * If a configuration file is found during startup of ServerPackCreator, it is automatically loaded into the GUI.
 * @author Griefed
 */
public class TabCreateServerPack extends JComponent {

    private static final Logger LOG = LogManager.getLogger(TabCreateServerPack.class);

    private final JFrame FRAME_SERVERPACKCREATOR;

    private final ConfigurationHandler CONFIGURATIONHANDLER;
    private final LocalizationManager LOCALIZATIONMANAGER;
    private final ServerPackHandler CREATESERVERPACK;
    private final CurseCreateModpack CURSECREATEMODPACK;
    private final AddonsHandler ADDONSHANDLER;
    private final VersionLister VERSIONLISTER;

    private final StyledDocument serverPackGeneratedDocument = new DefaultStyledDocument();

    private final SimpleAttributeSet serverPackGeneratedAttributeSet = new SimpleAttributeSet();

    private final JTextPane serverPackGeneratedTextPane = new JTextPane(serverPackGeneratedDocument);

    private final ImageIcon FOLDERICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/folder.png")));
    private final ImageIcon STARTGENERATIONICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/start_generation.png")));
    private final Dimension FOLDERBUTTONDIMENTION = new Dimension(24,24);
    private final Dimension STARTDIMENSION = new Dimension(64,64);
    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);

    private final JButton BUTTON_MODPACKDIRECTORY = new JButton();
    private final JButton BUTTON_CLIENTSIDEMODS = new JButton();
    private final JButton BUTTON_COPYDIRECTORIES = new JButton();
    private final JButton BUTTON_JAVAPATH = new JButton();
    private final JButton BUTTON_GENERATESERVERPACK = new JButton();

    private final ButtonGroup BUTTONGROUP_MODLOADERRADIOBUTTONS = new ButtonGroup();

    private final Insets TWENTY_TEN_ZERO_ZERO = new Insets(20,10,0,0);
    private final Insets ZERO_TEN_ZERO_ZERO = new Insets(0,10,0,0);
    private final Insets ZERO_ONEHUNDRET_ZERO_ZERO = new Insets(0,100,0,0);
    private final Insets TEN_TEN_ZERO_ZERO = new Insets(10,10,0,0);
    private final Insets ZERO_TEN_ZERO_TEN = new Insets(0,10,0,10);
    private final Insets FIVE_ZERO_FIVE_ZERO = new Insets(5,0,5,0);

    private final GridBagConstraints GRIDBAGCONSTRAINTS = new GridBagConstraints();

    private final JComponent CREATESERVERPACKPANEL = new JPanel(false);

    private final MaterialTextPaneUI MATERIALTEXTPANEUI = new MaterialTextPaneUI();

    private final JComboBox<String> COMBOBOX_MINECRAFTVERSIONS = new JComboBox<>();
    private final JComboBox<String> COMBOBOX_FORGEVERSIONS = new JComboBox<>();
    private final JComboBox<String> COMBOBOX_FABRICVERSIONS = new JComboBox<>();

    private final JTextField TEXTFIELD_MODPACKDIRECTORY = new JTextField("");
    private final JTextField TEXTFIELD_CLIENTSIDEMODS = new JTextField("");
    private final JTextField TEXTFIELD_COPYDIRECTORIES = new JTextField("");
    private final JTextField TEXTFIELD_JAVAPATH = new JTextField("");

    private final File FILE_SERVERPACKCREATOR_PROPERTIES = new File("serverpackcreator.properties");

    private final String SERVER_PACKS_DIR;

    private Properties serverPackCreatorProperties;

    private JLabel labelGenerateServerPack;
    private JLabel labelModpackDir;
    private JLabel labelClientMods;
    private JLabel labelCopyDirs;
    private JLabel labelJavaPath;
    private JLabel labelMinecraftVersion;
    private JLabel labelModloader;
    private JLabel labelModloaderVersion;

    private DefaultComboBoxModel<String> forgeComboBoxModel;

    private JFileChooser modpackDirChooser;
    private JFileChooser clientModsChooser;
    private JFileChooser copyDirsChooser;
    private JFileChooser javaChooser;

    private JCheckBox checkBoxServer;
    private JCheckBox checkBoxIcon;
    private JCheckBox checkBoxProperties;
    private JCheckBox checkBoxScripts;
    private JCheckBox checkBoxZIP;

    private String chosenModloader;
    private String chosenMinecraftVersion;
    private String chosenFabricVersion;
    private String chosenForgeVersion;
    private String javaArgs = "empty";

    private JRadioButton forgeRadioButton;
    private JRadioButton fabricRadioButton;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link ConfigurationHandler} required to successfully and correctly create the server pack.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * Receives an instance of {@link ServerPackHandler} which is required to generate a server pack.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedConfigurationHandler Instance of {@link ConfigurationHandler} required to successfully and correctly create the server pack.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedServerPackHandler Instance of {@link ServerPackHandler} required for the generation of server packs.
     * @param injectedAddonsHandler Instance of {@link AddonsHandler} required for accessing installed addons, if any exist.
     * @param injectedVersionLister Instance of {@link VersionLister} required for setting/changing comboboxes.
     * @param injectedServerPackCreatorProperties Instance of {@link Properties} required for various different things.
     * @param injectedServerPackCreatorFrame Our parent frame which contains all of ServerPackCreator.
     */
    public TabCreateServerPack(LocalizationManager injectedLocalizationManager, ConfigurationHandler injectedConfigurationHandler,
                               CurseCreateModpack injectedCurseCreateModpack, ServerPackHandler injectedServerPackHandler,
                               AddonsHandler injectedAddonsHandler, VersionLister injectedVersionLister, Properties injectedServerPackCreatorProperties,
                               JFrame injectedServerPackCreatorFrame) {

        if (injectedServerPackCreatorProperties == null) {
            try (InputStream inputStream = new FileInputStream("serverpackcreator.properties")) {
                this.serverPackCreatorProperties = new Properties();
                this.serverPackCreatorProperties.load(inputStream);
            } catch (IOException ex) {
                LOG.error("Couldn't read properties file.", ex);
            }
        } else {
            this.serverPackCreatorProperties = injectedServerPackCreatorProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }

        if (injectedAddonsHandler == null) {
            this.ADDONSHANDLER = new AddonsHandler(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        } else {
            this.ADDONSHANDLER = injectedAddonsHandler;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, serverPackCreatorProperties);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(serverPackCreatorProperties);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }

        if (injectedConfigurationHandler == null) {
            this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, serverPackCreatorProperties);
        } else {
            this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        }

        if (injectedServerPackHandler == null) {
            this.CREATESERVERPACK = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, ADDONSHANDLER, CONFIGURATIONHANDLER, serverPackCreatorProperties, VERSIONLISTER);
        } else {
            this.CREATESERVERPACK = injectedServerPackHandler;
        }

        this.FRAME_SERVERPACKCREATOR = injectedServerPackCreatorFrame;

        String tempDir = null;
        try {
            tempDir = serverPackCreatorProperties.getProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
        } catch (NullPointerException npe) {
            serverPackCreatorProperties.setProperty("de.griefed.serverpackcreator.dir.serverpacks","server-packs");
            tempDir = "server-packs";
        } finally {
            if (tempDir != null && !tempDir.equals("") && new File(tempDir).isDirectory()) {
                serverPackCreatorProperties.setProperty("de.griefed.serverpackcreator.dir.serverpacks",tempDir);
                SERVER_PACKS_DIR = tempDir;

                try (OutputStream outputStream = new FileOutputStream(getFILE_SERVERPACKCREATOR_PROPERTIES())) {
                    serverPackCreatorProperties.store(outputStream, null);
                } catch (IOException ex) {
                    LOG.error("Couldn't write properties-file.", ex);
                }

            } else {
                SERVER_PACKS_DIR = "server-packs";
            }
        }
    }

    /**
     * Getter for the serverpackcreator.properties-file.
     * @author Griefed
     * @return File. Returns the serverpackcreator.properties-file.
     */
    public File getFILE_SERVERPACKCREATOR_PROPERTIES() {
        return FILE_SERVERPACKCREATOR_PROPERTIES;
    }

    /**
     * Getter for the directory in which server-packs will be generated and stored in.
     * @author Griefed
     * @return String. Returns the path to the server-packs directory as a string.
     */
    public String getSERVER_PACKS_DIR() {
        return SERVER_PACKS_DIR;
    }

    /**
     * Getter for the chosen modloader from the JRadioButtons.
     * @author Griefed
     * @return String. Returns either Fabric or Forge.
     */
    public String getChosenModloader() {
        return chosenModloader;
    }

    /**
     * Setter for the chosen modloader from the JRadioButtons.
     * @author Griefed
     * @param chosenModloader String. Sets the chosen modloader for later use in server pack generation and config creation.
     */
    public void setChosenModloader(String chosenModloader) {
        this.chosenModloader = chosenModloader;
    }

    /**
     * Getter for the selected modloader version depending on which modloader is currently selected.
     * @author Griefed
     * @return String. Returns the modloader version depending on which modloader is currently selected.
     */
    private String getSelectedModloaderVersion() {
        if (chosenModloader.equalsIgnoreCase("Fabric")) {
            return chosenFabricVersion;
        } else {
            return chosenForgeVersion;
        }
    }

    public String getJavaArgs() {
        return javaArgs;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    /**
     * Create the tab which displays every component related to configuring ServerPackCreator and creating a server pack.
     * @author Griefed
     * @return JComponent. Returns a JPanel everything needed for displaying the TabCreateServerPack.
     * */
    public JComponent createServerPackTab() {

        CREATESERVERPACKPANEL.setLayout(new GridBagLayout());

// ----------------------------------------------------------------------------------------LABELS AND TEXTFIELDS--------
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
        GRIDBAGCONSTRAINTS.gridwidth = 4;
        GRIDBAGCONSTRAINTS.weightx = 1;

        //Label and textfield modpackDir
        labelModpackDir = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir"));
        labelModpackDir.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 0;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelModpackDir, GRIDBAGCONSTRAINTS);

        TEXTFIELD_MODPACKDIRECTORY.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 1;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_MODPACKDIRECTORY, GRIDBAGCONSTRAINTS);

        //Label and textfield clientMods
        labelClientMods = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods"));
        labelClientMods.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 2;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelClientMods, GRIDBAGCONSTRAINTS);

        TEXTFIELD_CLIENTSIDEMODS.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 3;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_CLIENTSIDEMODS, GRIDBAGCONSTRAINTS);

        //Label and textfield copyDirs
        labelCopyDirs = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs"));
        labelCopyDirs.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 4;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelCopyDirs, GRIDBAGCONSTRAINTS);

        TEXTFIELD_COPYDIRECTORIES.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 5;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_COPYDIRECTORIES, GRIDBAGCONSTRAINTS);

        //Label and textfield javaPath
        labelJavaPath = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath"));
        labelJavaPath.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 6;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelJavaPath, GRIDBAGCONSTRAINTS);

        TEXTFIELD_JAVAPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 7;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_JAVAPATH, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.gridwidth = 1;
        //Label and combobox minecraftVersion
        labelMinecraftVersion = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft"));
        labelMinecraftVersion.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 8;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelMinecraftVersion, GRIDBAGCONSTRAINTS);

        COMBOBOX_MINECRAFTVERSIONS.setModel(new DefaultComboBoxModel<>(VERSIONLISTER.getMinecraftReleaseVersionsAsArray()));
        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(0);
        COMBOBOX_MINECRAFTVERSIONS.addActionListener(this::actionEventComboBoxMinecraftVersion);

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 9;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(COMBOBOX_MINECRAFTVERSIONS, GRIDBAGCONSTRAINTS);

        //Label and radio buttons Modloader
        labelModloader = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader"));
        labelModloader.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 8;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelModloader, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;

        forgeRadioButton = new JRadioButton(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.slider.forge"),true);

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 9;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        BUTTONGROUP_MODLOADERRADIOBUTTONS.add(forgeRadioButton);

        forgeRadioButton.addItemListener(this::itemEventRadioButtonModloaderForge);

        CREATESERVERPACKPANEL.add(forgeRadioButton, GRIDBAGCONSTRAINTS);

        fabricRadioButton = new JRadioButton(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.slider.fabric"),false);

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 9;
        GRIDBAGCONSTRAINTS.insets = ZERO_ONEHUNDRET_ZERO_ZERO;

        BUTTONGROUP_MODLOADERRADIOBUTTONS.add(fabricRadioButton);

        fabricRadioButton.addItemListener(this::itemEventRadioButtonModloaderFabric);

        CREATESERVERPACKPANEL.add(fabricRadioButton, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
        GRIDBAGCONSTRAINTS.gridwidth = 2;
        //Label and textfield modloaderVersion
        labelModloaderVersion = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion"));
        labelModloaderVersion.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));

        GRIDBAGCONSTRAINTS.gridx = 2;
        GRIDBAGCONSTRAINTS.gridy = 8;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelModloaderVersion, GRIDBAGCONSTRAINTS);

        COMBOBOX_FABRICVERSIONS.setModel(new DefaultComboBoxModel<>(VERSIONLISTER.reverseOrderArray(VERSIONLISTER.getFabricVersionsAsArray())));
        COMBOBOX_FABRICVERSIONS.setSelectedIndex(0);
        COMBOBOX_FABRICVERSIONS.addActionListener(this::actionEventComboBoxFabricVersions);
        COMBOBOX_FABRICVERSIONS.setVisible(false);

        forgeComboBoxModel = new DefaultComboBoxModel<>(VERSIONLISTER.getForgeMeta().get(Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString()));

        COMBOBOX_FORGEVERSIONS.setModel(forgeComboBoxModel);
        COMBOBOX_FORGEVERSIONS.setSelectedIndex(0);
        COMBOBOX_FORGEVERSIONS.addActionListener(this::actionEventComboBoxForgeVersions);

        GRIDBAGCONSTRAINTS.gridx = 2;
        GRIDBAGCONSTRAINTS.gridy = 9;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(COMBOBOX_FABRICVERSIONS, GRIDBAGCONSTRAINTS);
        CREATESERVERPACKPANEL.add(COMBOBOX_FORGEVERSIONS, GRIDBAGCONSTRAINTS);

// ----------------------------------------------------------------------------------------LABELS AND CHECKBOXES--------

        GRIDBAGCONSTRAINTS.insets = TEN_TEN_ZERO_ZERO;
        GRIDBAGCONSTRAINTS.gridwidth = 1;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.SOUTHWEST;
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;

        //Checkbox installServer
        checkBoxServer = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver"),true);
        checkBoxServer.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.tip"));
        checkBoxServer.addActionListener(this::actionEventCheckBoxServer);

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 14;

        CREATESERVERPACKPANEL.add(checkBoxServer, GRIDBAGCONSTRAINTS);

        //Checkbox copyIcon
        checkBoxIcon = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon"),true);
        checkBoxIcon.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon.tip"));

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 14;

        CREATESERVERPACKPANEL.add(checkBoxIcon, GRIDBAGCONSTRAINTS);

        //Checkbox copyProperties
        checkBoxProperties = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties"),true);
        checkBoxProperties.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 15;

        CREATESERVERPACKPANEL.add(checkBoxProperties, GRIDBAGCONSTRAINTS);

        //Checkbox copyScripts
        checkBoxScripts = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts"),true);
        checkBoxScripts.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts.tip"));

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 15;

        CREATESERVERPACKPANEL.add(checkBoxScripts, GRIDBAGCONSTRAINTS);

        //Checkbox createZIP
        checkBoxZIP = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip"),true);
        checkBoxZIP.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip.tip"));

        GRIDBAGCONSTRAINTS.gridx = 2;
        GRIDBAGCONSTRAINTS.gridy = 14;

        CREATESERVERPACKPANEL.add(checkBoxZIP, GRIDBAGCONSTRAINTS);

// ------------------------------------------------------------------------------------------------------BUTTONS--------

        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_TEN;
        GRIDBAGCONSTRAINTS.weightx = 0;
        GRIDBAGCONSTRAINTS.weighty = 0;

        BUTTON_MODPACKDIRECTORY.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonmodpackdir"));
        BUTTON_MODPACKDIRECTORY.setContentAreaFilled(false);
        BUTTON_MODPACKDIRECTORY.setIcon(FOLDERICON);
        BUTTON_MODPACKDIRECTORY.setMinimumSize(FOLDERBUTTONDIMENTION);
        BUTTON_MODPACKDIRECTORY.setPreferredSize(FOLDERBUTTONDIMENTION);
        BUTTON_MODPACKDIRECTORY.setMaximumSize(FOLDERBUTTONDIMENTION);
        BUTTON_MODPACKDIRECTORY.addActionListener(this::selectModpackDirectory);
        addMouseListenerContentAreaFilledToButton(BUTTON_MODPACKDIRECTORY);

        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 1;

        CREATESERVERPACKPANEL.add(BUTTON_MODPACKDIRECTORY, GRIDBAGCONSTRAINTS);

        BUTTON_CLIENTSIDEMODS.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods"));
        BUTTON_CLIENTSIDEMODS.setContentAreaFilled(false);
        BUTTON_CLIENTSIDEMODS.setIcon(FOLDERICON);
        BUTTON_CLIENTSIDEMODS.setMinimumSize(FOLDERBUTTONDIMENTION);
        BUTTON_CLIENTSIDEMODS.setPreferredSize(FOLDERBUTTONDIMENTION);
        BUTTON_CLIENTSIDEMODS.setMaximumSize(FOLDERBUTTONDIMENTION);
        BUTTON_CLIENTSIDEMODS.addActionListener(this::selectClientMods);
        addMouseListenerContentAreaFilledToButton(BUTTON_CLIENTSIDEMODS);

        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 3;

        CREATESERVERPACKPANEL.add(BUTTON_CLIENTSIDEMODS, GRIDBAGCONSTRAINTS);

        BUTTON_COPYDIRECTORIES.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttoncopydirs"));
        BUTTON_COPYDIRECTORIES.setContentAreaFilled(false);
        BUTTON_COPYDIRECTORIES.setIcon(FOLDERICON);
        BUTTON_COPYDIRECTORIES.setMinimumSize(FOLDERBUTTONDIMENTION);
        BUTTON_COPYDIRECTORIES.setPreferredSize(FOLDERBUTTONDIMENTION);
        BUTTON_COPYDIRECTORIES.setMaximumSize(FOLDERBUTTONDIMENTION);
        BUTTON_COPYDIRECTORIES.addActionListener(this::selectCopyDirs);
        addMouseListenerContentAreaFilledToButton(BUTTON_COPYDIRECTORIES);

        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 5;

        CREATESERVERPACKPANEL.add(BUTTON_COPYDIRECTORIES, GRIDBAGCONSTRAINTS);

        BUTTON_JAVAPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonjavapath"));
        BUTTON_JAVAPATH.setContentAreaFilled(false);
        BUTTON_JAVAPATH.setIcon(FOLDERICON);
        BUTTON_JAVAPATH.setMinimumSize(FOLDERBUTTONDIMENTION);
        BUTTON_JAVAPATH.setPreferredSize(FOLDERBUTTONDIMENTION);
        BUTTON_JAVAPATH.setMaximumSize(FOLDERBUTTONDIMENTION);
        BUTTON_JAVAPATH.addActionListener(this::selectJavaInstallation);
        addMouseListenerContentAreaFilledToButton(BUTTON_JAVAPATH);

        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 7;

        CREATESERVERPACKPANEL.add(BUTTON_JAVAPATH, GRIDBAGCONSTRAINTS);

// ---------------------------------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------

        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
        GRIDBAGCONSTRAINTS.insets = FIVE_ZERO_FIVE_ZERO;

        labelGenerateServerPack = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttongenerateserverpack.ready"));
        labelGenerateServerPack.setFont(new Font(labelGenerateServerPack.getFont().getName(), Font.BOLD, labelGenerateServerPack.getFont().getSize()));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 18;
        GRIDBAGCONSTRAINTS.gridwidth = 4;
        GRIDBAGCONSTRAINTS.weightx = 0;
        GRIDBAGCONSTRAINTS.weighty = 0;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.PAGE_END;

        CREATESERVERPACKPANEL.add(labelGenerateServerPack, GRIDBAGCONSTRAINTS);

        BUTTON_GENERATESERVERPACK.setContentAreaFilled(false);
        BUTTON_GENERATESERVERPACK.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttongenerateserverpack.tip"));
        BUTTON_GENERATESERVERPACK.setIcon(STARTGENERATIONICON);
        BUTTON_GENERATESERVERPACK.setMinimumSize(STARTDIMENSION);
        BUTTON_GENERATESERVERPACK.setPreferredSize(STARTDIMENSION);
        BUTTON_GENERATESERVERPACK.setMaximumSize(STARTDIMENSION);
        BUTTON_GENERATESERVERPACK.addActionListener(this::generateServerpack);
        addMouseListenerContentAreaFilledToButton(BUTTON_GENERATESERVERPACK);

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 17;
        GRIDBAGCONSTRAINTS.gridwidth = 4;
        GRIDBAGCONSTRAINTS.weightx = 1;
        GRIDBAGCONSTRAINTS.weighty = 1;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.CENTER;

        CREATESERVERPACKPANEL.add(BUTTON_GENERATESERVERPACK, GRIDBAGCONSTRAINTS);

// --------------------------------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
        serverPackGeneratedTextPane.setOpaque(false);
        serverPackGeneratedTextPane.setEditable(false);
        StyleConstants.setBold(serverPackGeneratedAttributeSet, true);
        StyleConstants.setFontSize(serverPackGeneratedAttributeSet, 14);
        serverPackGeneratedTextPane.setCharacterAttributes(serverPackGeneratedAttributeSet, true);
        StyleConstants.setAlignment(serverPackGeneratedAttributeSet, StyleConstants.ALIGN_LEFT);
        try {
            serverPackGeneratedDocument.insertString(0,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.openfolder.browse"),
                    serverPackGeneratedAttributeSet);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
        loadConfig(new File("serverpackcreator.conf"));

        return CREATESERVERPACKPANEL;
    }

    /**
     * Checks whether the checkbox for the modloader-server installation is selected and whether the path to the Java-installation
     * is defined. If true and empty, a message is displayed, warning the user that Javapath needs to be defined for the
     * modloader-server installation to work. If "Yes" is clicked, a filechooser will open where the user can select their
     * Java-executable/binary. If "No" is selected, the user is warned about the consequences of not setting the Javapath.
     * @author Griefed
     * @param actionEvent The event which triggers this method.
     */
    private void actionEventCheckBoxServer(ActionEvent actionEvent) {
        // TODO: Replace with lang keys
        if (checkBoxServer.isSelected() && TEXTFIELD_JAVAPATH.getText().equals("")) {
            switch (JOptionPane.showConfirmDialog(
                    CREATESERVERPACKPANEL,
                    "Install modlaoder-server selected, but no path to Java defined. Choose now?",
                    "Javapath not sespecified!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            )) {
                case 0:

                    chooseJava();
                    break;

                case 1:

                    JOptionPane.showMessageDialog(
                            CREATESERVERPACKPANEL,
                            "Caution: Javapath needs to be defined in order to install the modloader-server!",
                            "WARNING!",
                            JOptionPane.ERROR_MESSAGE,
                            null
                    );
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Adds a mouse listener to the passed button which sets the content area of said button filled|not-filled when the
     * mouse enters|leaves the button.
     * @author Griefed
     * @param buttonToAddMouseListenerTo JButton. The button to add the mouse listener to.
     */
    private void addMouseListenerContentAreaFilledToButton(JButton buttonToAddMouseListenerTo) {

        buttonToAddMouseListenerTo.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                buttonToAddMouseListenerTo.setContentAreaFilled(true);
            }

            public void mouseExited(MouseEvent event) {
                buttonToAddMouseListenerTo.setContentAreaFilled(false);
            }
        });
    }

    /**
     * Setter for the list of available Forge versions depending on the passed Minecraft version.
     * @author Griefed
     * @param chosenMinecraftVersion String. The selected Minecraft version which determines the list of available Forge versions.
     */
    void changeForgeVersionListDependingOnMinecraftVersion(String chosenMinecraftVersion) {

        forgeComboBoxModel = new DefaultComboBoxModel<>(VERSIONLISTER.getForgeMeta().get(chosenMinecraftVersion));

        COMBOBOX_FORGEVERSIONS.setModel(forgeComboBoxModel);
        COMBOBOX_FORGEVERSIONS.setSelectedIndex(0);
    }

    /**
     * Setter for the Minecraft version depending on which one is selected in the GUI.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void actionEventComboBoxMinecraftVersion(ActionEvent event) {

        chosenMinecraftVersion = Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString();

        changeForgeVersionListDependingOnMinecraftVersion(Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString());

        LOG.debug("Selected Minecraft version: " + COMBOBOX_MINECRAFTVERSIONS.getSelectedItem());
    }

    /**
     * Setter for the Fabric version depending on which one is selected in the GUI.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void actionEventComboBoxFabricVersions(ActionEvent event) {

        chosenFabricVersion = Objects.requireNonNull(COMBOBOX_FABRICVERSIONS.getSelectedItem()).toString();

        LOG.debug("Selected Fabric version: " + COMBOBOX_FABRICVERSIONS.getSelectedItem());
    }

    /**
     * Setter for the Forge version depending on which one is selected in the GUI.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void actionEventComboBoxForgeVersions(ActionEvent event) {

        chosenForgeVersion = Objects.requireNonNull(COMBOBOX_FORGEVERSIONS.getSelectedItem()).toString();

        LOG.debug("Selected Forge version: " + COMBOBOX_FORGEVERSIONS.getSelectedItem());
    }

    /**
     * On selection, set the modloader to Forge.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void itemEventRadioButtonModloaderForge(ItemEvent event) {

        if (event.getStateChange() == ItemEvent.SELECTED) {

            COMBOBOX_FABRICVERSIONS.setVisible(false);

            setChosenModloader("Forge");

            chosenForgeVersion = Objects.requireNonNull(COMBOBOX_FORGEVERSIONS.getSelectedItem()).toString();

            LOG.debug("Forge selected. Version: " + chosenForgeVersion);

        } else if (event.getStateChange() == ItemEvent.DESELECTED) {

            COMBOBOX_FABRICVERSIONS.setVisible(true);

            LOG.debug("Forge deselected.");

        }
    }

    /**
     * On selection, set the modloader to Fabric.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void itemEventRadioButtonModloaderFabric(ItemEvent event) {

        if (event.getStateChange() == ItemEvent.SELECTED) {

            COMBOBOX_FORGEVERSIONS.setVisible(false);

            setChosenModloader("Fabric");

            chosenFabricVersion = Objects.requireNonNull(COMBOBOX_FABRICVERSIONS.getSelectedItem()).toString();

            LOG.debug("Fabric selected. Version: " + chosenFabricVersion);

        } else if (event.getStateChange() == ItemEvent.DESELECTED) {

            COMBOBOX_FORGEVERSIONS.setVisible(true);

            LOG.debug("Fabric deselected.");
        }
    }

    /**
     * Upon button-press, open a file-selector for the modpack-directory.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectModpackDirectory(ActionEvent event) {

        modpackDirChooser = new JFileChooser();

        modpackDirChooser.setCurrentDirectory(new File("."));
        modpackDirChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonmodpackdir.title"));
        modpackDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        modpackDirChooser.setAcceptAllFileFilterUsed(false);
        modpackDirChooser.setMultiSelectionEnabled(false);
        modpackDirChooser.setPreferredSize(CHOOSERDIMENSION);

        if (modpackDirChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            try {
                TEXTFIELD_MODPACKDIRECTORY.setText(modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

                /* This log is meant to be read by the user, therefore we allow translation. */
                LOG.debug("Selected modpack directory: " + modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

            } catch (IOException ex) {
                LOG.error("Error getting directory from modpack directory chooser.", ex);
            }
        }
    }

    /**
     * Upon button-press, open a file-selector for clientside-only mods. If the modpack-directory is specified, the file-selector
     * will open in the mods-directory in the modpack-directory.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectClientMods(ActionEvent event) {

        clientModsChooser = new JFileChooser();

        if (TEXTFIELD_MODPACKDIRECTORY.getText().length() > 0 &&
                new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory() &&
                new File(String.format("%s/mods", TEXTFIELD_MODPACKDIRECTORY.getText())).isDirectory()) {

            clientModsChooser.setCurrentDirectory(new File(String.format("%s/mods", TEXTFIELD_MODPACKDIRECTORY.getText())));
        } else {
            clientModsChooser.setCurrentDirectory(new File("."));
        }

        clientModsChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods.title"));
        clientModsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        clientModsChooser.setFileFilter(new FileNameExtensionFilter(
                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods.filter"),
                "jar"
        ));

        clientModsChooser.setAcceptAllFileFilterUsed(false);
        clientModsChooser.setMultiSelectionEnabled(true);
        clientModsChooser.setPreferredSize(CHOOSERDIMENSION);

        if (clientModsChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            File[] clientMods = clientModsChooser.getSelectedFiles();
            ArrayList<String> clientModsFilenames = new ArrayList<>();

            for (File mod : clientMods) {
                clientModsFilenames.add(mod.getName());
            }

            TEXTFIELD_CLIENTSIDEMODS.setText(
                    CONFIGURATIONHANDLER.buildString(
                            Arrays.toString(
                                    clientModsFilenames.toArray(new String[0])))
            );

            LOG.debug("Selected mods: " + clientModsFilenames);
        }
    }

    /**
     * Upon button-press, open a file-selector for directories which are to be copied to the server pack. If the modpack-directory
     * is specified, the file-selector will be opened in the modpack-directory.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectCopyDirs(ActionEvent event) {

        copyDirsChooser = new JFileChooser();

        if (TEXTFIELD_MODPACKDIRECTORY.getText().length() > 0 &&
                new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory()) {

            copyDirsChooser.setCurrentDirectory(new File(TEXTFIELD_MODPACKDIRECTORY.getText()));
        } else {
            copyDirsChooser.setCurrentDirectory(new File("."));
        }

        copyDirsChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttoncopydirs.title"));
        copyDirsChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        copyDirsChooser.setAcceptAllFileFilterUsed(false);
        copyDirsChooser.setMultiSelectionEnabled(true);
        copyDirsChooser.setPreferredSize(CHOOSERDIMENSION);

        if (copyDirsChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            File[] directoriesToCopy = copyDirsChooser.getSelectedFiles();
            ArrayList<String> copyDirsNames = new ArrayList<>();

            for (File directory : directoriesToCopy) {
                copyDirsNames.add(directory.getName());
            }

            TEXTFIELD_COPYDIRECTORIES.setText(CONFIGURATIONHANDLER.buildString(Arrays.toString(copyDirsNames.toArray(new String[0]))));

            LOG.debug("Selected directories: " + copyDirsNames);
        }
    }

    /**
     * Upon button-press, open a file-selector to select the Java-executable.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectJavaInstallation(ActionEvent event) {
        chooseJava();
    }

    /**
     * Opens a filechooser to select the Java-executable/binary.
     * @author Griefed
     */
    private void chooseJava() {
        javaChooser = new JFileChooser();

        if (new File(String.format("%s/bin/", System.getProperty("java.home").replace("\\", "/"))).isDirectory()) {

            javaChooser.setCurrentDirectory(new File(
                    String.format("%s/bin/",
                            System.getProperty("java.home").replace("\\", "/"))
            ));

        } else {
            javaChooser.setCurrentDirectory(new File("."));
        }

        javaChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonjavapath.tile"));
        javaChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        javaChooser.setAcceptAllFileFilterUsed(true);
        javaChooser.setMultiSelectionEnabled(false);
        javaChooser.setPreferredSize(CHOOSERDIMENSION);

        if (javaChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            try {
                TEXTFIELD_JAVAPATH.setText(javaChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

                LOG.debug("Set path to Java executable to: " + javaChooser.getSelectedFile().getCanonicalPath().replace("\\", "/"));

            } catch (IOException ex) {
                LOG.error("LOCALIZATIONMANAGER.getLocalizedString(\"createserverpack.log.error.buttonjavapath\")", ex);
            }
        }
    }

    /**
     * Upon button-press, check the entered configuration and if successfull, generate a server pack.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void generateServerpack(ActionEvent event) {

        FRAME_SERVERPACKCREATOR.setResizable(false);

        BUTTON_GENERATESERVERPACK.setEnabled(false);

        Tailer tailer = Tailer.create(new File("./logs/serverpackcreator.log"), new TailerListenerAdapter() {
            public void handle(String line) {
                synchronized (this) {
                    labelGenerateServerPack.setText(line.substring(line.indexOf(") - ") + 4));
                }
            }
        }, 100, false);

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));
        labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));

        saveConfig(new File("serverpackcreator.tmp"),true);

        ConfigurationModel configurationModel = new ConfigurationModel();

        if (!CONFIGURATIONHANDLER.checkConfiguration(new File("serverpackcreator.tmp"), false, configurationModel)) {
            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));

            if (new File("serverpackcreator.tmp").exists()) {
                boolean delTmp = new File("serverpackcreator.tmp").delete();
                if (delTmp) {
                    labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                } else {
                    labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                    /* This log is meant to be read by the user, therefore we allow translation. */
                    LOG.error(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                }
            }

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));

            saveConfig(CONFIGURATIONHANDLER.getConfigFile(), false);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));

            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {

                if (CREATESERVERPACK.run(CONFIGURATIONHANDLER.getConfigFile(), configurationModel)) {
                    tailer.stop();

                    System.gc();
                    System.runFinalization();

                    BUTTON_GENERATESERVERPACK.setEnabled(true);
                    labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));

                    TEXTFIELD_MODPACKDIRECTORY.setText(configurationModel.getModpackDir());
                    TEXTFIELD_COPYDIRECTORIES.setText(CONFIGURATIONHANDLER.buildString(configurationModel.getCopyDirs().toString()));

                    serverPackGeneratedDocument.setParagraphAttributes(0, serverPackGeneratedDocument.getLength(), serverPackGeneratedAttributeSet, false);
                    MATERIALTEXTPANEUI.installUI(serverPackGeneratedTextPane);

                    if (JOptionPane.showConfirmDialog(
                            CREATESERVERPACKPANEL,
                            serverPackGeneratedTextPane,
                            LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.openfolder.title"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE) == 0) {

                        try {
                            Desktop.getDesktop().open(new File(String.format("%s/%s", getSERVER_PACKS_DIR(), configurationModel.getModpackDir().substring(configurationModel.getModpackDir().lastIndexOf("/") + 1))));
                        } catch (IOException ex) {
                            LOG.error("Error opening file explorer for server pack.", ex);
                        }

                    }

                    BUTTON_GENERATESERVERPACK.setEnabled(true);

                    System.gc();
                    System.runFinalization();

                    tailer.stop();
                    executorService.shutdown();

                    FRAME_SERVERPACKCREATOR.setResizable(true);

                } else {

                    tailer.stop();

                    System.gc();
                    System.runFinalization();

                    BUTTON_GENERATESERVERPACK.setEnabled(true);
                    labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));

                    executorService.shutdown();

                    FRAME_SERVERPACKCREATOR.setResizable(true);

                }
            });

        } else {
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttongenerateserverpack.fail"));
            BUTTON_GENERATESERVERPACK.setEnabled(true);
            FRAME_SERVERPACKCREATOR.setResizable(true);
        }
    }

    /**
     * Save the current configuration to a specified file.
     * @author Griefed
     * @param configFile File. The file to store the configuration under.
     * @param temporary Whether the file is temporary. Determines whether the config in the basedir is deleted first.
     */
    void saveConfig(File configFile, boolean temporary) {
        if (javaArgs.equals("")) {
            javaArgs = "empty";
        }

        List<String> tempClientMods = new ArrayList<>(Arrays.asList(TEXTFIELD_CLIENTSIDEMODS.getText().replace(", ", ",").split(",")));
        List<String> tempCopyDirs = new ArrayList<>(Arrays.asList(TEXTFIELD_COPYDIRECTORIES.getText().replace(", ", ",").split(",")));

        CONFIGURATIONHANDLER.writeConfigToFile(
                TEXTFIELD_MODPACKDIRECTORY.getText(),
                tempClientMods,
                tempCopyDirs,
                checkBoxServer.isSelected(),
                TEXTFIELD_JAVAPATH.getText(),
                chosenMinecraftVersion,
                getChosenModloader(),
                getSelectedModloaderVersion(),
                checkBoxIcon.isSelected(),
                checkBoxProperties.isSelected(),
                checkBoxScripts.isSelected(),
                checkBoxZIP.isSelected(),
                getJavaArgs(),
                configFile,
                temporary
        );
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into ServerPackCreator.
     * @author Griefed
     * @param configFile File. The configuration file to parse and load into the GUI.
     */
    protected void loadConfig(File configFile) {

        try {

            Config config = ConfigFactory.parseFile(configFile);

            try {
                TEXTFIELD_MODPACKDIRECTORY.setText(config.getString("modpackDir").replace("\\", "/"));
            } catch (NullPointerException ex) {
                LOG.error("Error parsing modpackdir from configfile: " + configFile, ex);
            }

            if (config.getStringList("clientMods").isEmpty()) {

                TEXTFIELD_CLIENTSIDEMODS.setText(CONFIGURATIONHANDLER.buildString(CONFIGURATIONHANDLER.getFallbackModsList().toString()));
                LOG.debug("Set clientMods with fallback list.");

            } else {
                TEXTFIELD_CLIENTSIDEMODS.setText(CONFIGURATIONHANDLER.buildString(config.getStringList("clientMods").toString()));
            }

            TEXTFIELD_COPYDIRECTORIES.setText(CONFIGURATIONHANDLER.buildString(config.getStringList("copyDirs").toString().replace("\\","/")));

            TEXTFIELD_JAVAPATH.setText(CONFIGURATIONHANDLER.checkJavaPath(config.getString("javaPath").replace("\\", "/")));

            try {
                String minecraftVersion;
                try {
                    minecraftVersion = config.getString("minecraftVersion");
                } catch (NullPointerException npe) {
                    minecraftVersion = "1.17.1";
                }

                if (minecraftVersion.equals("")) {
                    minecraftVersion = "1.17.1";
                }

                String[] mcver = VERSIONLISTER.getMinecraftReleaseVersionsAsArray();
                for (int i = 0; i < mcver.length; i++) {
                    if (mcver[i].equals(minecraftVersion)) {
                        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
                        chosenMinecraftVersion = minecraftVersion;
                    }
                }

            } catch (NullPointerException ex) {
                LOG.error("Error parsing minecraft-version from configfile: " + configFile, ex);
            }

            try {

                String modloader;
                try {
                    if (CONFIGURATIONHANDLER.setModLoaderCase(config.getString("modLoader")).equals("Fabric")) {
                        modloader = "Fabric";
                    } else if (CONFIGURATIONHANDLER.setModLoaderCase(config.getString("modLoader")).equals("Forge")) {
                        modloader = "Forge";
                    } else {
                        modloader = "Forge";
                    }
                } catch (NullPointerException | ConfigException ex) {
                    modloader = "Forge";
                }

                String modloaderver = config.getString("modLoaderVersion");
                
                if (modloader.equals("Fabric")) {

                    String[] fabricver = VERSIONLISTER.getFabricVersionsAsArray();

                    updateModloaderGuiComponents(true, false, "Fabric");

                    if (!config.getString("modLoaderVersion").equals("")) {
                        for (int i = 0; i < fabricver.length; i++) {
                            if (fabricver[i].equals(config.getString("modLoaderVersion"))) {
                                COMBOBOX_FABRICVERSIONS.setSelectedIndex(i);
                                chosenFabricVersion = config.getString("modLoaderVersion");
                            }
                        }
                    }

                } else {
                    String[] forgever = VERSIONLISTER.getForgeMeta().get(chosenMinecraftVersion);
                    changeForgeVersionListDependingOnMinecraftVersion(chosenMinecraftVersion);

                    updateModloaderGuiComponents(false, true, "Forge");

                    if (!config.getString("modLoaderVersion").equals("")) {
                        for (int i = 0; i < forgever.length; i++) {
                            if (forgever[i].equals(modloaderver)) {
                                COMBOBOX_FORGEVERSIONS.setSelectedIndex(i);
                                chosenForgeVersion = config.getString("modLoaderVersion");
                            }
                        }
                    }
                }
            } catch (NullPointerException ex) {

                LOG.error("Error parsing modloader-version from configfile: " + configFile, ex);
                updateModloaderGuiComponents(false, true, "Forge");

                changeForgeVersionListDependingOnMinecraftVersion(Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString());
            }

            checkBoxServer.setSelected(CONFIGURATIONHANDLER.convertToBoolean(config.getString("includeServerInstallation")));

            checkBoxIcon.setSelected(CONFIGURATIONHANDLER.convertToBoolean(config.getString("includeServerIcon")));

            checkBoxProperties.setSelected(CONFIGURATIONHANDLER.convertToBoolean(config.getString("includeServerProperties")));

            checkBoxScripts.setSelected(CONFIGURATIONHANDLER.convertToBoolean(config.getString("includeStartScripts")));

            checkBoxZIP.setSelected(CONFIGURATIONHANDLER.convertToBoolean(config.getString("includeZipCreation")));

            try {
                setJavaArgs(config.getString("javaArgs"));
            } catch (ConfigException | NullPointerException ex) {
                LOG.warn(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonloadconfig.javaargs"));
                setJavaArgs("empty");
            }

        } catch (NullPointerException ex) {

            LOG.error("Error parsing configfile.", ex);
        }
    }

    /**
     * Helper method which changes various states of GUI components.
     * @author Griefed
     * @param fabric Boolean. Whether Fabric is active.
     * @param forge Boolean. Whether Forge is active.
     * @param chosenModloader String. The modloader to set.
     */
    private void updateModloaderGuiComponents(boolean fabric, boolean forge, String chosenModloader) {

        fabricRadioButton.setSelected(fabric);
        COMBOBOX_FABRICVERSIONS.setVisible(fabric);

        forgeRadioButton.setSelected(forge);
        COMBOBOX_FORGEVERSIONS.setVisible(forge);

        setChosenModloader(chosenModloader);
    }
}