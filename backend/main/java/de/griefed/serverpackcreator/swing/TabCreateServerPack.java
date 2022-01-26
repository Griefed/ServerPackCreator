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

import com.electronwill.nightconfig.core.file.FileConfig;
import de.griefed.serverpackcreator.*;
import de.griefed.serverpackcreator.curseforge.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.utilities.*;
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
    private final VersionLister VERSIONLISTER;
    private final BooleanUtilities BOOLEANUTILITIES;
    private final ListUtilities LISTUTILITIES;
    private final StringUtilities STRINGUTILITIES;
    private final ConfigUtilities CONFIGUTILITIES;
    private final SystemUtilities SYSTEMUTILITIES;
    private final ApplicationProperties APPLICATIONPROPERTIES;
    private final ApplicationPlugins APPLICATIONPLUGINS;

    private final StyledDocument SERVERPACKGENERATEDDOCUMENT = new DefaultStyledDocument();
    private final SimpleAttributeSet SERVERPACKGENERATEDATTRIBUTESET = new SimpleAttributeSet();
    private final JTextPane SERVERPACKGENERATEDTEXTPANE = new JTextPane(SERVERPACKGENERATEDDOCUMENT);

    private final StyledDocument LAZYMODEDOCUMENT = new DefaultStyledDocument();
    private final SimpleAttributeSet LAZYMODEATTRIBUTESET = new SimpleAttributeSet();
    private final JTextPane LAZYMODETEXTPANE = new JTextPane(LAZYMODEDOCUMENT);

    private final ImageIcon FOLDERICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/folder.png")));
    private final ImageIcon STARTGENERATIONICON = new ImageIcon(Objects.requireNonNull(SwingGuiInitializer.class.getResource("/de/griefed/resources/gui/start_generation.png")));
    private final Dimension FOLDERBUTTONDIMENSION = new Dimension(24,24);
    private final Dimension STARTDIMENSION = new Dimension(64,64);
    private final Dimension CHOOSERDIMENSION = new Dimension(750,450);

    private final JButton BUTTON_MODPACKDIRECTORY = new JButton();
    private final JButton BUTTON_CLIENTSIDEMODS = new JButton();
    private final JButton BUTTON_COPYDIRECTORIES = new JButton();
    private final JButton BUTTON_JAVAPATH = new JButton();
    private final JButton BUTTON_GENERATESERVERPACK = new JButton();
    private final JButton BUTTON_SERVERICON = new JButton();
    private final JButton BUTTON_SERVERPROPERTIES = new JButton();

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
    private final JTextField TEXTFIELD_SERVERPACKSUFFIX = new JTextField("");
    private final JTextField TEXTFIELD_SERVERICONPATH = new JTextField("");
    private final JTextField TEXTFIELD_SERVERPROPERTIESPATH = new JTextField("");

    private final File DIRECTORY_CHOOSER = new File(".");

    private JLabel labelGenerateServerPack;
    private JLabel labelModpackDir;
    private JLabel labelClientMods;
    private JLabel labelCopyDirs;
    private JLabel labelJavaPath;
    private JLabel labelMinecraftVersion;
    private JLabel labelModloader;
    private JLabel labelModloaderVersion;
    private JLabel labelServerPackSuffix;
    private JLabel labelServerIconPath;
    private JLabel labelServerPropertiesPath;

    private DefaultComboBoxModel<String> forgeComboBoxModel;

    private JFileChooser modpackDirChooser;
    private JFileChooser clientModsChooser;
    private JFileChooser copyDirsChooser;
    private JFileChooser javaChooser;
    private JFileChooser serverIconChooser;
    private JFileChooser serverPropertiesChooser;

    private JCheckBox checkBoxServer;
    private JCheckBox checkBoxIcon;
    private JCheckBox checkBoxProperties;
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
     * @param injectedVersionLister Instance of {@link VersionLister} required for setting/changing comboboxes.
     * @param injectedApplicationProperties Instance of {@link Properties} required for various different things.
     * @param injectedServerPackCreatorFrame Our parent frame which contains all of ServerPackCreator.
     * @param injectedBooleanUtilities Instance of {@link BooleanUtilities}.
     * @param injectedListUtilities Instance of {@link ListUtilities}.
     * @param injectedStringUtilities Instance of {@link StringUtilities}.
     * @param injectedConfigUtilities Instance of {@link ConfigUtilities}.
     * @param injectedSystemUtilities Instance of {@link SystemUtilities}.
     * @param injectedPluginManager Instance of {@link ApplicationPlugins}.
     */
    public TabCreateServerPack(LocalizationManager injectedLocalizationManager, ConfigurationHandler injectedConfigurationHandler,
                               CurseCreateModpack injectedCurseCreateModpack, ServerPackHandler injectedServerPackHandler,
                               VersionLister injectedVersionLister, ApplicationProperties injectedApplicationProperties,
                               JFrame injectedServerPackCreatorFrame, BooleanUtilities injectedBooleanUtilities, ListUtilities injectedListUtilities,
                               StringUtilities injectedStringUtilities, ConfigUtilities injectedConfigUtilities, SystemUtilities injectedSystemUtilities,
                               ApplicationPlugins injectedPluginManager) {

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

        if (injectedBooleanUtilities == null) {
            this.BOOLEANUTILITIES = new BooleanUtilities(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES);
        } else {
            this.BOOLEANUTILITIES = injectedBooleanUtilities;
        }

        if (injectedListUtilities == null) {
            this.LISTUTILITIES = new ListUtilities();
        } else {
            this.LISTUTILITIES = injectedListUtilities;
        }

        if (injectedStringUtilities == null) {
            this.STRINGUTILITIES = new StringUtilities();
        } else {
            this.STRINGUTILITIES = injectedStringUtilities;
        }

        if (injectedConfigUtilities == null) {
            this.CONFIGUTILITIES = new ConfigUtilities(LOCALIZATIONMANAGER, BOOLEANUTILITIES, LISTUTILITIES, APPLICATIONPROPERTIES, STRINGUTILITIES);
        } else {
            this.CONFIGUTILITIES = injectedConfigUtilities;
        }

        if (injectedVersionLister == null) {
            this.VERSIONLISTER = new VersionLister(APPLICATIONPROPERTIES);
        } else {
            this.VERSIONLISTER = injectedVersionLister;
        }

        if (injectedCurseCreateModpack == null) {
            this.CURSECREATEMODPACK = new CurseCreateModpack(LOCALIZATIONMANAGER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, CONFIGUTILITIES);
        } else {
            this.CURSECREATEMODPACK = injectedCurseCreateModpack;
        }

        if (injectedSystemUtilities == null) {
            this.SYSTEMUTILITIES = new SystemUtilities();
        } else {
            this.SYSTEMUTILITIES = injectedSystemUtilities;
        }

        if (injectedConfigurationHandler == null) {
            this.CONFIGURATIONHANDLER = new ConfigurationHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, VERSIONLISTER, APPLICATIONPROPERTIES, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES);
        } else {
            this.CONFIGURATIONHANDLER = injectedConfigurationHandler;
        }

        if (injectedPluginManager == null) {
            this.APPLICATIONPLUGINS = new ApplicationPlugins();
        } else {
            this.APPLICATIONPLUGINS = injectedPluginManager;
        }

        if (injectedServerPackHandler == null) {
            this.CREATESERVERPACK = new ServerPackHandler(LOCALIZATIONMANAGER, CURSECREATEMODPACK, CONFIGURATIONHANDLER, APPLICATIONPROPERTIES, VERSIONLISTER, BOOLEANUTILITIES, LISTUTILITIES, STRINGUTILITIES, SYSTEMUTILITIES, CONFIGUTILITIES, APPLICATIONPLUGINS);
        } else {
            this.CREATESERVERPACK = injectedServerPackHandler;
        }

        this.FRAME_SERVERPACKCREATOR = injectedServerPackCreatorFrame;

        SERVERPACKGENERATEDTEXTPANE.setOpaque(false);
        SERVERPACKGENERATEDTEXTPANE.setEditable(false);
        StyleConstants.setBold(SERVERPACKGENERATEDATTRIBUTESET, true);
        StyleConstants.setFontSize(SERVERPACKGENERATEDATTRIBUTESET, 14);
        SERVERPACKGENERATEDTEXTPANE.setCharacterAttributes(SERVERPACKGENERATEDATTRIBUTESET, true);
        StyleConstants.setAlignment(SERVERPACKGENERATEDATTRIBUTESET, StyleConstants.ALIGN_LEFT);
        try {
            SERVERPACKGENERATEDDOCUMENT.insertString(0,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.openfolder.browse"),
                    SERVERPACKGENERATEDATTRIBUTESET);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }

        LAZYMODETEXTPANE.setOpaque(false);
        LAZYMODETEXTPANE.setEditable(false);
        StyleConstants.setBold(LAZYMODEATTRIBUTESET, true);
        StyleConstants.setFontSize(LAZYMODEATTRIBUTESET, 14);
        LAZYMODETEXTPANE.setCharacterAttributes(LAZYMODEATTRIBUTESET, true);
        StyleConstants.setAlignment(LAZYMODEATTRIBUTESET, StyleConstants.ALIGN_LEFT);
        try {
            LAZYMODEDOCUMENT.insertString(0,
                    LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0") + "\n\n" +
                        LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode1") + "\n" +
                        LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode2") + "\n" +
                        LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode3") + "\n\n" +
                        LOCALIZATIONMANAGER.getLocalizedString("configuration.log.warn.checkconfig.copydirs.lazymode0"),
                    LAZYMODEATTRIBUTESET);
        } catch (BadLocationException ex) {
            LOG.error("Error inserting text into aboutDocument.", ex);
        }
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

    /**
     * Getter for the currently set JVM flags / Java args.
     * @author Griefed
     * @return String. Returns the currently set JVM flags / Java args.
     */
    public String getJavaArgs() {
        return javaArgs;
    }

    /**
     * Getter for the current text from the currently set Javapath in the Javapath textfield.
     * @author Griefed
     * @return String. Returns the current text from the currently set Javapath in the Javapath textfield.
     */
    public String getJavaPath() {
        return TEXTFIELD_JAVAPATH.getText();
    }

    /**
     * Setter for the JVM flags / Java args.
     * @author Griefed
     * @param javaArgs String. The javaargs to set.
     */
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    /**
     * Getter for the text in the custom server-icon textfield.
     * @author Griefed
     * @return String. Returns the text in the server-icon textfield.
     */
    public String getServerIconPath() {
        return TEXTFIELD_SERVERICONPATH.getText();
    }

    /**
     * Getter for the text in the custom server.properties textfield
     * @author Griefed
     * @return String. Returns the text in the server.properties textfield.
     */
    public String getServerPropertiesPath() {
        return TEXTFIELD_SERVERPROPERTIESPATH.getText();
    }

    /**
     * Create the tab which displays every component related to configuring ServerPackCreator and creating a server pack.
     * @author Griefed
     * @return JComponent. Returns a JPanel everything needed for displaying the TabCreateServerPack.
     * */
    public JComponent createServerPackTab() {

        CREATESERVERPACKPANEL.setLayout(new GridBagLayout());
        // TODO: Move components to separate classes. Only pass localizationManager, return full component. End result CREATESERVERPACKPANEL.add(new ModpackDirLabel(LOCALIZATIONMANAGER), GRIDBAGCONSTRAINTS) and so on
// ----------------------------------------------------------------------------------------LABELS AND TEXTFIELDS--------
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
        GRIDBAGCONSTRAINTS.gridwidth = 3;
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

        //Label and textfield server pack suffix
        labelServerPackSuffix = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelsuffix"));
        labelServerPackSuffix.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelsuffix.tip"));

        GRIDBAGCONSTRAINTS.gridwidth = 2;
        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 0;
        GRIDBAGCONSTRAINTS.insets =  new Insets(20,-140,0,0);

        CREATESERVERPACKPANEL.add(labelServerPackSuffix, GRIDBAGCONSTRAINTS);

        TEXTFIELD_SERVERPACKSUFFIX.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelsuffix.tip"));

        GRIDBAGCONSTRAINTS.gridwidth = 1;
        GRIDBAGCONSTRAINTS.gridx = 4;
        GRIDBAGCONSTRAINTS.gridy = 1;
        GRIDBAGCONSTRAINTS.insets = new Insets(0,-140,0,0);

        CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPACKSUFFIX, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.gridwidth = 5;

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

        //Labels and textfields server-icon.png and server.properties paths
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
        GRIDBAGCONSTRAINTS.gridwidth = 2;

        labelServerIconPath = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeliconpath"));
        labelServerIconPath.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeliconpath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 6;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelServerIconPath, GRIDBAGCONSTRAINTS);

        TEXTFIELD_SERVERICONPATH.setText("");
        TEXTFIELD_SERVERICONPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.textfield.iconpath"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 7;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERICONPATH, GRIDBAGCONSTRAINTS);

        labelServerPropertiesPath = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelpropertiespath"));
        labelServerPropertiesPath.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelpropertiespath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 3;
        GRIDBAGCONSTRAINTS.gridy = 6;
        GRIDBAGCONSTRAINTS.insets = new Insets(20,-190,0,0);

        CREATESERVERPACKPANEL.add(labelServerPropertiesPath, GRIDBAGCONSTRAINTS);

        TEXTFIELD_SERVERPROPERTIESPATH.setText("");
        TEXTFIELD_SERVERPROPERTIESPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.textfield.propertiespath"));

        GRIDBAGCONSTRAINTS.gridx = 3;
        GRIDBAGCONSTRAINTS.gridy = 7;
        GRIDBAGCONSTRAINTS.insets = new Insets(0,-190,0,0);

        CREATESERVERPACKPANEL.add(TEXTFIELD_SERVERPROPERTIESPATH, GRIDBAGCONSTRAINTS);

        //Label and textfield javaPath
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.HORIZONTAL;
        GRIDBAGCONSTRAINTS.gridwidth = 5;

        labelJavaPath = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath"));
        labelJavaPath.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 8;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelJavaPath, GRIDBAGCONSTRAINTS);

        TEXTFIELD_JAVAPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 9;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(TEXTFIELD_JAVAPATH, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.gridwidth = 1;

        //Label and combobox minecraftVersion
        labelMinecraftVersion = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft"));
        labelMinecraftVersion.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 10;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelMinecraftVersion, GRIDBAGCONSTRAINTS);

        COMBOBOX_MINECRAFTVERSIONS.setModel(new DefaultComboBoxModel<>(VERSIONLISTER.getMinecraftReleaseVersionsAsArray()));
        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(0);
        COMBOBOX_MINECRAFTVERSIONS.addActionListener(this::actionEventComboBoxMinecraftVersion);

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 11;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(COMBOBOX_MINECRAFTVERSIONS, GRIDBAGCONSTRAINTS);

        //Label and radio buttons Modloader
        labelModloader = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader"));
        labelModloader.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 10;
        GRIDBAGCONSTRAINTS.insets = TWENTY_TEN_ZERO_ZERO;

        CREATESERVERPACKPANEL.add(labelModloader, GRIDBAGCONSTRAINTS);

        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.WEST;

        forgeRadioButton = new JRadioButton(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.slider.forge"),true);

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 11;
        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_ZERO;

        BUTTONGROUP_MODLOADERRADIOBUTTONS.add(forgeRadioButton);

        forgeRadioButton.addItemListener(this::itemEventRadioButtonModloaderForge);

        CREATESERVERPACKPANEL.add(forgeRadioButton, GRIDBAGCONSTRAINTS);

        fabricRadioButton = new JRadioButton(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.slider.fabric"),false);

        GRIDBAGCONSTRAINTS.gridx = 1;
        GRIDBAGCONSTRAINTS.gridy = 11;
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
        GRIDBAGCONSTRAINTS.gridy = 10;
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
        GRIDBAGCONSTRAINTS.gridy = 11;
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

        GRIDBAGCONSTRAINTS.gridx = 2;
        GRIDBAGCONSTRAINTS.gridy = 14;

        CREATESERVERPACKPANEL.add(checkBoxProperties, GRIDBAGCONSTRAINTS);

        //Checkbox createZIP
        checkBoxZIP = new JCheckBox(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip"),true);
        checkBoxZIP.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip.tip"));

        GRIDBAGCONSTRAINTS.gridx = 3;
        GRIDBAGCONSTRAINTS.gridy = 14;

        CREATESERVERPACKPANEL.add(checkBoxZIP, GRIDBAGCONSTRAINTS);

// ------------------------------------------------------------------------------------------------------BUTTONS--------

        GRIDBAGCONSTRAINTS.insets = ZERO_TEN_ZERO_TEN;
        GRIDBAGCONSTRAINTS.weightx = 0;
        GRIDBAGCONSTRAINTS.weighty = 0;

        BUTTON_MODPACKDIRECTORY.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonmodpackdir"));
        BUTTON_MODPACKDIRECTORY.setContentAreaFilled(false);
        BUTTON_MODPACKDIRECTORY.setIcon(FOLDERICON);
        BUTTON_MODPACKDIRECTORY.setMinimumSize(FOLDERBUTTONDIMENSION);
        BUTTON_MODPACKDIRECTORY.setPreferredSize(FOLDERBUTTONDIMENSION);
        BUTTON_MODPACKDIRECTORY.setMaximumSize(FOLDERBUTTONDIMENSION);
        BUTTON_MODPACKDIRECTORY.addActionListener(this::selectModpackDirectory);
        addMouseListenerContentAreaFilledToButton(BUTTON_MODPACKDIRECTORY);

        GRIDBAGCONSTRAINTS.gridx = 3;
        GRIDBAGCONSTRAINTS.gridy = 1;

        CREATESERVERPACKPANEL.add(BUTTON_MODPACKDIRECTORY, GRIDBAGCONSTRAINTS);

        BUTTON_CLIENTSIDEMODS.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonclientmods"));
        BUTTON_CLIENTSIDEMODS.setContentAreaFilled(false);
        BUTTON_CLIENTSIDEMODS.setIcon(FOLDERICON);
        BUTTON_CLIENTSIDEMODS.setMinimumSize(FOLDERBUTTONDIMENSION);
        BUTTON_CLIENTSIDEMODS.setPreferredSize(FOLDERBUTTONDIMENSION);
        BUTTON_CLIENTSIDEMODS.setMaximumSize(FOLDERBUTTONDIMENSION);
        BUTTON_CLIENTSIDEMODS.addActionListener(this::selectClientMods);
        addMouseListenerContentAreaFilledToButton(BUTTON_CLIENTSIDEMODS);

        GRIDBAGCONSTRAINTS.gridx = 5;
        GRIDBAGCONSTRAINTS.gridy = 3;

        CREATESERVERPACKPANEL.add(BUTTON_CLIENTSIDEMODS, GRIDBAGCONSTRAINTS);

        BUTTON_COPYDIRECTORIES.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttoncopydirs"));
        BUTTON_COPYDIRECTORIES.setContentAreaFilled(false);
        BUTTON_COPYDIRECTORIES.setIcon(FOLDERICON);
        BUTTON_COPYDIRECTORIES.setMinimumSize(FOLDERBUTTONDIMENSION);
        BUTTON_COPYDIRECTORIES.setPreferredSize(FOLDERBUTTONDIMENSION);
        BUTTON_COPYDIRECTORIES.setMaximumSize(FOLDERBUTTONDIMENSION);
        BUTTON_COPYDIRECTORIES.addActionListener(this::selectCopyDirs);
        addMouseListenerContentAreaFilledToButton(BUTTON_COPYDIRECTORIES);

        GRIDBAGCONSTRAINTS.gridx = 5;
        GRIDBAGCONSTRAINTS.gridy = 5;

        CREATESERVERPACKPANEL.add(BUTTON_COPYDIRECTORIES, GRIDBAGCONSTRAINTS);

        BUTTON_JAVAPATH.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttonjavapath"));
        BUTTON_JAVAPATH.setContentAreaFilled(false);
        BUTTON_JAVAPATH.setIcon(FOLDERICON);
        BUTTON_JAVAPATH.setMinimumSize(FOLDERBUTTONDIMENSION);
        BUTTON_JAVAPATH.setPreferredSize(FOLDERBUTTONDIMENSION);
        BUTTON_JAVAPATH.setMaximumSize(FOLDERBUTTONDIMENSION);
        BUTTON_JAVAPATH.addActionListener(this::selectJavaInstallation);
        addMouseListenerContentAreaFilledToButton(BUTTON_JAVAPATH);

        GRIDBAGCONSTRAINTS.gridx = 5;
        GRIDBAGCONSTRAINTS.gridy = 9;

        CREATESERVERPACKPANEL.add(BUTTON_JAVAPATH, GRIDBAGCONSTRAINTS);

        BUTTON_SERVERICON.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.button.icon"));
        BUTTON_SERVERICON.setContentAreaFilled(false);
        BUTTON_SERVERICON.setIcon(FOLDERICON);
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

        BUTTON_SERVERPROPERTIES.setToolTipText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.button.properties"));
        BUTTON_SERVERPROPERTIES.setContentAreaFilled(false);
        BUTTON_SERVERPROPERTIES.setIcon(FOLDERICON);
        BUTTON_SERVERPROPERTIES.setMinimumSize(FOLDERBUTTONDIMENSION);
        BUTTON_SERVERPROPERTIES.setPreferredSize(FOLDERBUTTONDIMENSION);
        BUTTON_SERVERPROPERTIES.setMaximumSize(FOLDERBUTTONDIMENSION);
        BUTTON_SERVERPROPERTIES.addActionListener(this::selectServerProperties);
        addMouseListenerContentAreaFilledToButton(BUTTON_SERVERPROPERTIES);

        GRIDBAGCONSTRAINTS.gridx = 5;
        GRIDBAGCONSTRAINTS.gridy = 7;

        CREATESERVERPACKPANEL.add(BUTTON_SERVERPROPERTIES, GRIDBAGCONSTRAINTS);

// ---------------------------------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------

        GRIDBAGCONSTRAINTS.weightx = 0;
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;
        GRIDBAGCONSTRAINTS.insets = FIVE_ZERO_FIVE_ZERO;

        labelGenerateServerPack = new JLabel(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttongenerateserverpack.ready"));
        labelGenerateServerPack.setFont(new Font(labelGenerateServerPack.getFont().getName(), Font.BOLD, labelGenerateServerPack.getFont().getSize()));

        GRIDBAGCONSTRAINTS.gridx = 0;
        GRIDBAGCONSTRAINTS.gridy = 18;
        GRIDBAGCONSTRAINTS.gridwidth = 5;
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
        GRIDBAGCONSTRAINTS.gridwidth = 5;
        GRIDBAGCONSTRAINTS.weightx = 1;
        GRIDBAGCONSTRAINTS.weighty = 1;
        GRIDBAGCONSTRAINTS.anchor = GridBagConstraints.CENTER;

        CREATESERVERPACKPANEL.add(BUTTON_GENERATESERVERPACK, GRIDBAGCONSTRAINTS);

// --------------------------------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        GRIDBAGCONSTRAINTS.fill = GridBagConstraints.NONE;

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
        if (checkBoxServer.isSelected() && TEXTFIELD_JAVAPATH.getText().equals("")) {
            switch (JOptionPane.showConfirmDialog(
                    CREATESERVERPACKPANEL,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.confirm.message"),
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.confirm.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            )) {
                case 0:

                    chooseJava();
                    break;

                case 1:

                    JOptionPane.showMessageDialog(
                            CREATESERVERPACKPANEL,
                            LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.message.message"),
                            LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.message.title"),
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

        if (new File(TEXTFIELD_MODPACKDIRECTORY.getText()).isDirectory()) {
            modpackDirChooser.setCurrentDirectory(new File(TEXTFIELD_MODPACKDIRECTORY.getText()));
        } else {
            modpackDirChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
        }
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
     * Upon button-press, open a file-selector for the server-icon.png.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectServerIcon(ActionEvent event) {

        serverIconChooser = new JFileChooser();

        serverIconChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
        serverIconChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.chooser.icon.title"));
        serverIconChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        serverIconChooser.setFileFilter(new FileNameExtensionFilter(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.chooser.icon.filter"), "png","jpg","jpeg","bmp"));
        serverIconChooser.setAcceptAllFileFilterUsed(false);
        serverIconChooser.setMultiSelectionEnabled(false);
        serverIconChooser.setPreferredSize(CHOOSERDIMENSION);

        if (serverIconChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            try {

                TEXTFIELD_SERVERICONPATH.setText(serverIconChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));

            } catch (IOException ex) {
                LOG.error("Error getting the icon-file for the server pack.", ex);
            }
        }
    }

    /**
     * Upon button-press, open a file-selector for the server.properties.
     * @author Griefed
     * @param event The event which triggers this method.
     */
    private void selectServerProperties(ActionEvent event) {

        serverPropertiesChooser = new JFileChooser();

        serverPropertiesChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
        serverPropertiesChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.chooser.properties.title"));
        serverPropertiesChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        serverPropertiesChooser.setFileFilter(new FileNameExtensionFilter(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.chooser.properties.filter"), "properties"));
        serverPropertiesChooser.setAcceptAllFileFilterUsed(false);
        serverPropertiesChooser.setMultiSelectionEnabled(false);
        serverPropertiesChooser.setPreferredSize(CHOOSERDIMENSION);

        if (serverPropertiesChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            try {

                TEXTFIELD_SERVERPROPERTIESPATH.setText(serverPropertiesChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));

            } catch (IOException ex) {
                LOG.error("Error getting the properties-file for the server pack.", ex);
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
            clientModsChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
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
            ArrayList<String> clientModsFilenames = new ArrayList<>(100);

            for (File mod : clientMods) {
                clientModsFilenames.add(mod.getName());
            }

            TEXTFIELD_CLIENTSIDEMODS.setText(
                    STRINGUTILITIES.buildString(
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
            copyDirsChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
        }

        copyDirsChooser.setDialogTitle(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttoncopydirs.title"));
        copyDirsChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        copyDirsChooser.setAcceptAllFileFilterUsed(false);
        copyDirsChooser.setMultiSelectionEnabled(true);
        copyDirsChooser.setPreferredSize(CHOOSERDIMENSION);

        if (copyDirsChooser.showOpenDialog(CREATESERVERPACKPANEL) == JFileChooser.APPROVE_OPTION) {
            File[] directoriesToCopy = copyDirsChooser.getSelectedFiles();
            ArrayList<String> copyDirsNames = new ArrayList<>(100);

            for (File directory : directoriesToCopy) {
                copyDirsNames.add(directory.getName());
            }

            TEXTFIELD_COPYDIRECTORIES.setText(STRINGUTILITIES.buildString(Arrays.toString(copyDirsNames.toArray(new String[0]))));

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
    void chooseJava() {
        javaChooser = new JFileChooser();

        if (new File(String.format("%s/bin/", System.getProperty("java.home").replace("\\", "/"))).isDirectory()) {

            javaChooser.setCurrentDirectory(new File(
                    String.format("%s/bin/",
                            System.getProperty("java.home").replace("\\", "/"))
            ));

        } else {
            javaChooser.setCurrentDirectory(DIRECTORY_CHOOSER);
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

        int decision = 0;

        MATERIALTEXTPANEUI.installUI(LAZYMODETEXTPANE);

        if (TEXTFIELD_COPYDIRECTORIES.getText().equals("lazy_mode")) {
            decision = JOptionPane.showConfirmDialog(
                    FRAME_SERVERPACKCREATOR,
                    LAZYMODETEXTPANE,
                    LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.lazymode"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
        }

        LOG.debug("Case " + decision);

        //No inspection in case we ever want to expand on this switch statement.
        //noinspection SwitchStatementWithTooFewBranches
        switch (decision) {
            case 0:

                generate();
                break;

            default:

                FRAME_SERVERPACKCREATOR.setResizable(true);
                BUTTON_GENERATESERVERPACK.setEnabled(true);
                break;
        }
    }

    private void generate() {

        Tailer tailer = Tailer.create(new File("./logs/serverpackcreator.log"), new TailerListenerAdapter() {
            public void handle(String line) {
                synchronized (this) {
                    if (!line.contains("DEBUG")) {
                        labelGenerateServerPack.setText(line.substring(line.indexOf(") - ") + 4));
                    }
                }
            }
        }, 100, false);

        /* This log is meant to be read by the user, therefore we allow translation. */
        LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));
        labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));

        saveConfig(new File("./work/temporaryConfig.conf"));

        List<String> encounteredErrors = new ArrayList<>(100);

        if (!CONFIGURATIONHANDLER.checkConfiguration(new File("./work/temporaryConfig.conf"), encounteredErrors, true)) {

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));

            if (new File("./work/temporaryConfig.conf").exists()) {

                boolean delTmp = new File("./work/temporaryConfig.conf").delete();

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

            saveConfig(APPLICATIONPROPERTIES.FILE_CONFIG);

            /* This log is meant to be read by the user, therefore we allow translation. */
            LOG.info(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));
            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));

            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {

                try {

                    ConfigurationModel configurationModel = new ConfigurationModel();

                    if (!CONFIGURATIONHANDLER.checkConfiguration(APPLICATIONPROPERTIES.FILE_CONFIG, configurationModel, false)) {

                        CREATESERVERPACK.run(configurationModel);

                        loadConfig(new File("serverpackcreator.conf"));

                        SERVERPACKGENERATEDDOCUMENT.setParagraphAttributes(0, SERVERPACKGENERATEDDOCUMENT.getLength(), SERVERPACKGENERATEDATTRIBUTESET, false);
                        MATERIALTEXTPANEUI.installUI(SERVERPACKGENERATEDTEXTPANE);

                        if (JOptionPane.showConfirmDialog(
                                FRAME_SERVERPACKCREATOR,
                                SERVERPACKGENERATEDTEXTPANE,
                                LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.openfolder.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE) == 0) {

                            try {
                                Desktop.getDesktop().open(new File(String.format("%s/%s", APPLICATIONPROPERTIES.getDirectoryServerPacks(), configurationModel.getModpackDir().substring(configurationModel.getModpackDir().lastIndexOf("/") + 1) + TEXTFIELD_SERVERPACKSUFFIX.getText())));
                            } catch (IOException ex) {
                                LOG.error("Error opening file explorer for server pack.", ex);
                            }

                        }
                    }

                } catch (Exception ex) {

                    LOG.error("An error occurred when generating the server pack.",ex);

                } finally {

                    labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));

                    tailer.stop();

                    BUTTON_GENERATESERVERPACK.setEnabled(true);
                    FRAME_SERVERPACKCREATOR.setResizable(true);

                    System.gc();
                    System.runFinalization();

                    executorService.shutdown();
                }
            });

        } else {

            labelGenerateServerPack.setText(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.buttongenerateserverpack.fail"));

            tailer.stop();

            if (encounteredErrors.size() > 0) {

                StringBuilder errors = new StringBuilder();

                for (int i = 0; i < encounteredErrors.size(); i++) {

                    errors.append(i + 1).append(": ").append(encounteredErrors.get(i)).append("\n");

                }

                JOptionPane.showMessageDialog(
                        FRAME_SERVERPACKCREATOR,
                        errors,
                        String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.errors.encountered"),encounteredErrors.size()),
                        JOptionPane.ERROR_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon")
                );
            }

            BUTTON_GENERATESERVERPACK.setEnabled(true);
            FRAME_SERVERPACKCREATOR.setResizable(true);

            System.gc();
            System.runFinalization();
        }
    }

    /**
     * Save the current configuration to a specified file.
     * @author Griefed
     * @param configFile File. The file to store the configuration under.
     */
    void saveConfig(File configFile) {
        if (javaArgs.equals("")) {
            javaArgs = "empty";
        }

        List<String> tempClientMods = LISTUTILITIES.cleanList(new ArrayList<>(Arrays.asList(TEXTFIELD_CLIENTSIDEMODS.getText().replace(", ", ",").split(","))));
        List<String> tempCopyDirs = LISTUTILITIES.cleanList(new ArrayList<>(Arrays.asList(TEXTFIELD_COPYDIRECTORIES.getText().replace(", ", ",").split(","))));

        CONFIGUTILITIES.writeConfigToFile(
                TEXTFIELD_MODPACKDIRECTORY.getText(),
                tempClientMods,
                tempCopyDirs,
                TEXTFIELD_SERVERICONPATH.getText(),
                TEXTFIELD_SERVERPROPERTIESPATH.getText(),
                checkBoxServer.isSelected(),
                TEXTFIELD_JAVAPATH.getText(),
                chosenMinecraftVersion,
                getChosenModloader(),
                getSelectedModloaderVersion(),
                checkBoxIcon.isSelected(),
                checkBoxProperties.isSelected(),
                checkBoxZIP.isSelected(),
                getJavaArgs(),
                TEXTFIELD_SERVERPACKSUFFIX.getText(),
                configFile
        );
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into ServerPackCreator.
     * @author Griefed
     * @param configFile File. The configuration file to parse and load into the GUI.
     */
    protected void loadConfig(File configFile) {

        try {

            FileConfig config = FileConfig.of(configFile);
            config.load();

            try {

                TEXTFIELD_MODPACKDIRECTORY.setText(config.getOrElse("modpackDir","").replace("\\", "/"));

            } catch (NullPointerException ex) {

                LOG.error("Error parsing modpackdir from configfile: " + configFile, ex);

            }

            if (config.getOrElse("clientMods", APPLICATIONPROPERTIES.getListFallbackMods()).isEmpty()) {

                TEXTFIELD_CLIENTSIDEMODS.setText(STRINGUTILITIES.buildString(APPLICATIONPROPERTIES.getListFallbackMods().toString()));
                LOG.debug("Set clientMods with fallback list.");

            } else {

                try {
                    TEXTFIELD_CLIENTSIDEMODS.setText(STRINGUTILITIES.buildString(config.get("clientMods").toString()));
                } catch (Exception ex) {
                    LOG.error("Couldn't parse clientMods. Using fallback.",ex);
                    TEXTFIELD_CLIENTSIDEMODS.setText(STRINGUTILITIES.buildString(APPLICATIONPROPERTIES.getListFallbackMods().toString()));
                }

            }

            if (config.getOrElse("copyDirs", Arrays.asList("config","mods")).isEmpty()) {

                TEXTFIELD_COPYDIRECTORIES.setText("config, mods");

            } else {

                try {
                    TEXTFIELD_COPYDIRECTORIES.setText(STRINGUTILITIES.buildString(config.get("copyDirs").toString().replace("\\", "/")));
                } catch (Exception ex) {
                    LOG.error("Couldn't parse copyDirs. Using fallback.",ex);
                    TEXTFIELD_COPYDIRECTORIES.setText("config, mods");
                }

            }

            TEXTFIELD_SERVERICONPATH.setText(config.getOrElse("serverIconPath","").replace("\\","/"));

            TEXTFIELD_SERVERPROPERTIESPATH.setText(config.getOrElse("serverPropertiesPath","").replace("\\","/"));

            TEXTFIELD_JAVAPATH.setText(SYSTEMUTILITIES.acquireJavaPathFromSystem());

            try {

                try {
                    if (!config.getOrElse("minecraftVersion","").equals("")) {
                        chosenMinecraftVersion = config.get("minecraftVersion");
                    } else {
                        chosenMinecraftVersion = VERSIONLISTER.getMinecraftReleaseVersion();
                    }

                } catch (NullPointerException ignored) {
                    chosenMinecraftVersion = VERSIONLISTER.getMinecraftReleaseVersion();
                }

                String[] mcver = VERSIONLISTER.getMinecraftReleaseVersionsAsArray();

                for (int i = 0; i < mcver.length; i++) {

                    if (mcver[i].equals(chosenMinecraftVersion)) {

                        COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
                        break;
                    }
                }

            } catch (NullPointerException ex) {
                LOG.error("Error parsing minecraft-version from configfile: " + configFile, ex);
                chosenMinecraftVersion = VERSIONLISTER.getMinecraftReleaseVersion();
            }

            // Set modloader and modloader version
            try {

                // Check for Fabric
                if (config.getOrElse("modLoader","Forge").equalsIgnoreCase("Fabric")) {

                    String[] fabricver = VERSIONLISTER.reverseOrderArray(VERSIONLISTER.getFabricVersionsAsArray());

                    updateModloaderGuiComponents(true, false, "Fabric");

                    if (!config.getOrElse("modLoaderVersion","").equals("")) {

                        // Go through all Fabric versions and check if specified version matches official version list
                        for (int i = 0; i < fabricver.length; i++) {

                            // If match is found, set selected version
                            if (fabricver[i].equals(config.get("modLoaderVersion").toString())) {

                                COMBOBOX_FABRICVERSIONS.setSelectedIndex(i);
                                chosenFabricVersion = config.get("modLoaderVersion").toString();

                            }

                        }

                    }

                // If not Fabric, then assume Forge
                } else {

                    String[] forgever = VERSIONLISTER.getForgeMeta().get(chosenMinecraftVersion);

                    changeForgeVersionListDependingOnMinecraftVersion(chosenMinecraftVersion);

                    updateModloaderGuiComponents(false, true, "Forge");

                    if (!config.getOrElse("modLoaderVersion","").equals("")) {

                        for (int i = 0; i < forgever.length; i++) {

                            if (forgever[i].equals(config.get("modLoaderVersion").toString())) {

                                COMBOBOX_FORGEVERSIONS.setSelectedIndex(i);
                                chosenForgeVersion = config.get("modLoaderVersion").toString();
                            }

                        }

                    }

                }

            } catch (NullPointerException ex) {

                LOG.error(String.format(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.gui.createserverpack.errors.modloader.version"), configFile));
                updateModloaderGuiComponents(false, true, "Forge");

                changeForgeVersionListDependingOnMinecraftVersion(Objects.requireNonNull(COMBOBOX_MINECRAFTVERSIONS.getSelectedItem()).toString());
            }

            checkBoxServer.setSelected(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerInstallation","False"))));

            checkBoxIcon.setSelected(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerIcon", "False"))));

            checkBoxProperties.setSelected(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeServerProperties", "False"))));

            checkBoxZIP.setSelected(BOOLEANUTILITIES.convertToBoolean(String.valueOf(config.getOrElse("includeZipCreation","False"))));

            setJavaArgs(config.getOrElse("javaArgs","empty"));

            TEXTFIELD_SERVERPACKSUFFIX.setText(config.getOrElse("serverPackSuffix",""));

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

    /**
     * Load default values for textfields so the user can start with a new configuration. Just as if ServerPackCreator
     * was started without a serverpackcreator.conf being present.
     * @author Griefed
     */
    protected void clearInterface() {
        TEXTFIELD_MODPACKDIRECTORY.setText("");
        TEXTFIELD_SERVERPACKSUFFIX.setText("");
        TEXTFIELD_CLIENTSIDEMODS.setText(STRINGUTILITIES.buildString(APPLICATIONPROPERTIES.getListFallbackMods().toString()));
        TEXTFIELD_COPYDIRECTORIES.setText("");
        TEXTFIELD_SERVERICONPATH.setText("");
        TEXTFIELD_SERVERPROPERTIESPATH.setText("");
        TEXTFIELD_JAVAPATH.setText(SYSTEMUTILITIES.acquireJavaPathFromSystem());

        String minecraftVersion = VERSIONLISTER.getMinecraftReleaseVersion();
        String[] mcver = VERSIONLISTER.getMinecraftReleaseVersionsAsArray();
        for (int i = 0; i < mcver.length; i++) {
            if (mcver[i].equals(minecraftVersion)) {
                COMBOBOX_MINECRAFTVERSIONS.setSelectedIndex(i);
                chosenMinecraftVersion = minecraftVersion;
            }
        }

        changeForgeVersionListDependingOnMinecraftVersion(chosenMinecraftVersion);
        updateModloaderGuiComponents(false, true, "Forge");

        checkBoxServer.setSelected(false);
        checkBoxIcon.setSelected(false);
        checkBoxProperties.setSelected(false);
        checkBoxZIP.setSelected(false);
        setJavaArgs("empty");
    }
}