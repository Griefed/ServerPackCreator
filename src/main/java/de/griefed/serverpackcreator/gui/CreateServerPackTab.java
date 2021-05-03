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
package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Configuration;
import de.griefed.serverpackcreator.CreateServerPack;
import de.griefed.serverpackcreator.curseforgemodpack.CurseCreateModpack;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class creates the tab which displays the labels, textfields, buttons and functions in order to create a new
 * server pack. Available are:<br>
 * JLabels and JTextFields for modpackDir, clientMods, copyDirs, javaPath, minecraftVersion, modLoader, modLoaderVersion<br>
 * Checkboxes for Include- serverInstallation, serverIcon, serverProperties, startScripts, ZIParchive<br>
 * Buttons opening the file explorer and choosing: modpackDir, clientMods, copyDirs, javaPath, loadConfigFromFile<br>
 * A button for displaying an information windows which provides detailed explanation of the important parts of the GUI.<br>
 * The button start the generation of a new server pack.<br>
 * The label under the button to start the generation of a new server pack, which displays the latest log entry of the
 * serverpackcreator.log <em>during</em> the creation of a new server pack.<p>
 *     If a configuration file is found during startup of ServerPackCreator, it is automatically loaded into the GUI.
 * </p>
 */
public class CreateServerPackTab extends JComponent {
    private static final Logger appLogger = LogManager.getLogger(CreateServerPackTab.class);

    private Configuration configuration;
    private LocalizationManager localizationManager;
    private CurseCreateModpack curseCreateModpack;
    private CreateServerPack createServerPack;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.<p>
     * Receives an instance of {@link Configuration} required to successfully and correctly create the server pack.<p>
     * Receives an instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * Receives an instance of {@link CreateServerPack} which is required to generate a server pack.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedConfiguration Instance of {@link Configuration} required to successfully and correctly create the server pack.
     * @param injectedCurseCreateModpack Instance of {@link CurseCreateModpack} in case the modpack has to be created from a combination of
     * CurseForge projectID and fileID, from which to <em>then</em> create the server pack.
     * @param injectedCreateServerPack Instance of {@link CreateServerPack} required for the generation of server packs.
     */
    public CreateServerPackTab(LocalizationManager injectedLocalizationManager, Configuration injectedConfiguration, CurseCreateModpack injectedCurseCreateModpack, CreateServerPack injectedCreateServerPack) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }

        if (injectedConfiguration == null) {
            this.curseCreateModpack = new CurseCreateModpack(localizationManager);
        } else {
            this.curseCreateModpack = injectedCurseCreateModpack;
        }

        if (injectedConfiguration == null) {
            this.configuration = new Configuration(localizationManager, curseCreateModpack);
        } else {
            this.configuration = injectedConfiguration;
        }

        if (injectedCreateServerPack == null) {
            this.createServerPack = new CreateServerPack(localizationManager, configuration, curseCreateModpack);
        } else {
            this.createServerPack = injectedCreateServerPack;
        }
    }

    private final ImageIcon loadIcon              = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/load.png")));
    private final ImageIcon folderIcon            = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/folder.png")));
    private final ImageIcon startGeneration       = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/start_generation.png")));
    private final ImageIcon helpIcon              = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/help.png")));
    private final Dimension folderButtonDimension = new Dimension(24,24);
    private final Dimension miscButtonDimension   = new Dimension(50,50);
    private final Dimension chooserDimension      = new Dimension(750,450);

    private JComponent createServerPackPanel;

    private JScrollPane helpScrollPane;

    private GridBagConstraints constraints;

    private JLabel labelModpackDir;
    private JLabel labelClientMods;
    private JLabel labelCopyDirs;
    private JLabel labelJavaPath;
    private JLabel labelMinecraftVersion;
    private JLabel labelModloader;
    private JLabel labelModloaderVersion;
    private JLabel labelGenerateServerPack;

    private JTextField textModpackDir;
    private JTextField textClientMods;
    private JTextField textCopyDirs;
    private JTextField textJavaPath;
    private JTextField textMinecraftVersion;
    private JTextField textModloader;
    private JTextField textModloaderVersion;

    private JTextArea helpTextArea;

    private JCheckBox checkBoxServer;
    private JCheckBox checkBoxIcon;
    private JCheckBox checkBoxProperties;
    private JCheckBox checkBoxScripts;
    private JCheckBox checkBoxZIP;

    private JButton buttonModpackDir;
    private JButton buttonClientMods;
    private JButton buttonCopyDirs;
    private JButton buttonJavaPath;
    private JButton buttonLoadConfigFromFile;
    private JButton buttonInfoWindow;
    private JButton buttonGenerateServerPack;

    private JFileChooser modpackDirChooser;
    private JFileChooser clientModsChooser;
    private JFileChooser copyDirsChooser;
    private JFileChooser javaChooser;
    private JFileChooser configChooser;

    /**
     * Create the tab which displays every component related to configuring ServerPackCreator and creating a server pack.
     * @return JComponent. Returns a JPanel everything needed for displaying the CreateServerPackTab.
     * */
    JComponent createServerPackTab() {
        createServerPackPanel = new JPanel(false);
        createServerPackPanel.setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();

// ----------------------------------------------------------------------------------------LABELS AND TEXTFIELDS--------
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.weightx = 0.7;

        //Label and textfield modpackDir
        labelModpackDir = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir"));
        labelModpackDir.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModpackDir, constraints);
        textModpackDir = new JTextField("");
        textModpackDir.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModpackDir, constraints);

        //Label and textfield clientMods
        labelClientMods = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods"));
        labelClientMods.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelClientMods, constraints);
        textClientMods = new JTextField("");
        textClientMods.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textClientMods, constraints);

        //Label and textfield copyDirs
        labelCopyDirs = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs"));
        labelCopyDirs.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelCopyDirs, constraints);
        textCopyDirs = new JTextField("");
        textCopyDirs.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textCopyDirs, constraints);

        //Label and textfield javaPath
        labelJavaPath = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath"));
        labelJavaPath.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelJavaPath, constraints);
        textJavaPath = new JTextField("");
        textJavaPath.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textJavaPath, constraints);

        //Label and textfield minecraftVersion
        labelMinecraftVersion = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft"));
        labelMinecraftVersion.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelMinecraftVersion, constraints);
        textMinecraftVersion = new JTextField("");
        textMinecraftVersion.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textMinecraftVersion, constraints);

        //Label and textfield Modloader
        labelModloader = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader"));
        labelModloader.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloader, constraints);
        textModloader = new JTextField("");
        textModloader.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));
        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloader, constraints);

        //Label and textfield modloaderVersion
        labelModloaderVersion = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion"));
        labelModloaderVersion.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloaderVersion, constraints);
        textModloaderVersion = new JTextField("");
        textModloaderVersion.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));
        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloaderVersion, constraints);

// ----------------------------------------------------------------------------------------LABELS AND CHECKBOXES--------
        constraints.insets = new Insets(10,10,0,0);
        constraints.gridwidth = 1;

        //Checkboxes
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.fill = GridBagConstraints.NONE;

        //Checkbox installServer
        checkBoxServer = new JCheckBox(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver"),true);
        checkBoxServer.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.tip"));
        constraints.gridx = 0;
        constraints.gridy = 14;
        createServerPackPanel.add(checkBoxServer, constraints);

        //Checkbox copyIcon
        checkBoxIcon = new JCheckBox(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon"),true);
        checkBoxIcon.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon.tip"));
        constraints.gridx = 1;
        constraints.gridy = 14;
        createServerPackPanel.add(checkBoxIcon, constraints);

        //Checkbox copyProperties
        checkBoxProperties = new JCheckBox(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties"),true);
        checkBoxProperties.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties.tip"));
        constraints.gridx = 0;
        constraints.gridy = 15;
        createServerPackPanel.add(checkBoxProperties, constraints);

        //Checkbox copyScripts
        checkBoxScripts = new JCheckBox(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts"),true);
        checkBoxScripts.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts.tip"));
        constraints.gridx = 1;
        constraints.gridy = 15;
        createServerPackPanel.add(checkBoxScripts, constraints);

        //Checkbox createZIP
        checkBoxZIP = new JCheckBox(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip"),true);
        checkBoxZIP.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip.tip"));
        constraints.gridx = 0;
        constraints.gridy = 16;
        createServerPackPanel.add(checkBoxZIP, constraints);

// ------------------------------------------------------------------------------------------------------BUTTONS--------
        constraints.insets = new Insets(0,10,0,10);

        constraints.weightx = 0;
        constraints.weighty = 0;

        //Select modpackDir button
        buttonModpackDir = new JButton();
        buttonModpackDir.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttonmodpackdir"));
        buttonModpackDir.setContentAreaFilled(false);
        buttonModpackDir.setIcon(folderIcon);
        buttonModpackDir.setMinimumSize(folderButtonDimension);
        buttonModpackDir.setPreferredSize(folderButtonDimension);
        buttonModpackDir.setMaximumSize(folderButtonDimension);
        buttonModpackDir.addActionListener(e -> {
            modpackDirChooser = new JFileChooser();

            modpackDirChooser.setCurrentDirectory(new File("."));
            modpackDirChooser.setDialogTitle(localizationManager.getLocalizedString("createserverpack.gui.buttonmodpackdir.title"));
            modpackDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            modpackDirChooser.setAcceptAllFileFilterUsed(false);
            modpackDirChooser.setMultiSelectionEnabled(false);
            modpackDirChooser.setPreferredSize(chooserDimension);

            if (modpackDirChooser.showOpenDialog(modpackDirChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textModpackDir.setText(modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));

                    appLogger.info(String.format(
                            localizationManager.getLocalizedString("createserverpack.log.info.buttonmodpack"),
                            modpackDirChooser.getSelectedFile().getCanonicalPath().replace("\\","/")));

                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("createserverpack.log.error.buttonmodpack"),ex);
                }
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 1;
        createServerPackPanel.add(buttonModpackDir, constraints);

        //Select clientside-mods button
        buttonClientMods = new JButton();
        buttonClientMods.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttonclientmods"));
        buttonClientMods.setContentAreaFilled(false);
        buttonClientMods.setIcon(folderIcon);
        buttonClientMods.setMinimumSize(folderButtonDimension);
        buttonClientMods.setPreferredSize(folderButtonDimension);
        buttonClientMods.setMaximumSize(folderButtonDimension);
        buttonClientMods.addActionListener(e -> {
            clientModsChooser = new JFileChooser();

            if (textModpackDir.getText().length() > 0 &&
                    new File(textModpackDir.getText()).isDirectory() &&
                    new File(String.format("%s/mods",textModpackDir.getText())).isDirectory()) {

                clientModsChooser.setCurrentDirectory(new File(String.format("%s/mods",textModpackDir.getText())));
            } else {
                clientModsChooser.setCurrentDirectory(new File("."));
            }

            clientModsChooser.setDialogTitle(localizationManager.getLocalizedString("createserverpack.gui.buttonclientmods.title"));
            clientModsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            clientModsChooser.setFileFilter(new FileNameExtensionFilter(
                    localizationManager.getLocalizedString("createserverpack.gui.buttonclientmods.filter"),
                    "jar"
            ));

            clientModsChooser.setAcceptAllFileFilterUsed(false);
            clientModsChooser.setMultiSelectionEnabled(true);
            clientModsChooser.setPreferredSize(chooserDimension);

            if (clientModsChooser.showOpenDialog(clientModsChooser) == JFileChooser.APPROVE_OPTION) {
                File[] clientMods = clientModsChooser.getSelectedFiles();
                ArrayList<String> clientModsFilenames = new ArrayList<>();

                for (int i = 0; i < clientMods.length; i++) {
                    clientModsFilenames.add(clientMods[i].getName());
                }

                textClientMods.setText(
                        configuration.buildString(
                                Arrays.toString(
                                        clientModsFilenames.toArray(new String[0])))
                );
                appLogger.info(String.format(localizationManager.getLocalizedString("createserverpack.log.info.buttonclientmods"), clientModsFilenames));
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 3;
        createServerPackPanel.add(buttonClientMods, constraints);

        //Select directories to copy to server pack button
        buttonCopyDirs = new JButton();
        buttonCopyDirs.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttoncopydirs"));
        buttonCopyDirs.setContentAreaFilled(false);
        buttonCopyDirs.setIcon(folderIcon);
        buttonCopyDirs.setMinimumSize(folderButtonDimension);
        buttonCopyDirs.setPreferredSize(folderButtonDimension);
        buttonCopyDirs.setMaximumSize(folderButtonDimension);
        buttonCopyDirs.addActionListener(e -> {
            copyDirsChooser = new JFileChooser();

            if (textModpackDir.getText().length() > 0 &&
                    new File(textModpackDir.getText()).isDirectory()) {

                copyDirsChooser.setCurrentDirectory(new File(textModpackDir.getText()));
            } else {
                copyDirsChooser.setCurrentDirectory(new File("."));
            }

            copyDirsChooser.setDialogTitle(localizationManager.getLocalizedString("createserverpack.gui.buttoncopydirs.title"));
            copyDirsChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            copyDirsChooser.setAcceptAllFileFilterUsed(false);
            copyDirsChooser.setMultiSelectionEnabled(true);
            copyDirsChooser.setPreferredSize(chooserDimension);

            if (copyDirsChooser.showOpenDialog(copyDirsChooser) == JFileChooser.APPROVE_OPTION) {
                File[] copyDirs = copyDirsChooser.getSelectedFiles();
                ArrayList<String> copyDirsNames = new ArrayList<>();

                for (int i = 0; i < copyDirs.length; i++) {
                    copyDirsNames.add(copyDirs[i].getName());
                }

                textCopyDirs.setText(configuration.buildString(Arrays.toString(copyDirsNames.toArray(new String[0]))));
                appLogger.info(String.format(localizationManager.getLocalizedString("createserverpack.log.info.buttoncopydirs"), copyDirsNames));
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 5;
        createServerPackPanel.add(buttonCopyDirs, constraints);

        //Select javaPath button
        buttonJavaPath = new JButton();
        buttonJavaPath.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttonjavapath"));
        buttonJavaPath.setContentAreaFilled(false);
        buttonJavaPath.setIcon(folderIcon);
        buttonJavaPath.setMinimumSize(folderButtonDimension);
        buttonJavaPath.setPreferredSize(folderButtonDimension);
        buttonJavaPath.setMaximumSize(folderButtonDimension);
        buttonJavaPath.addActionListener(e -> {
            javaChooser = new JFileChooser();

            if (new File(String.format("%s/bin/",System.getProperty("java.home").replace("\\", "/"))).isDirectory()) {

                javaChooser.setCurrentDirectory(new File(
                        String.format("%s/bin/",
                                System.getProperty("java.home").replace("\\", "/"))
                ));

            } else {
                javaChooser.setCurrentDirectory(new File("."));
            }

            javaChooser.setDialogTitle(localizationManager.getLocalizedString("createserverpack.gui.buttonjavapath.tile"));
            javaChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            javaChooser.setAcceptAllFileFilterUsed(true);
            javaChooser.setMultiSelectionEnabled(false);
            javaChooser.setPreferredSize(chooserDimension);

            if (javaChooser.showOpenDialog(javaChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textJavaPath.setText(javaChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));

                    appLogger.info(String.format(
                            localizationManager.getLocalizedString("createserverpack.log.info.buttonjavapath"),
                            javaChooser.getSelectedFile().getCanonicalPath().replace("\\","/")
                    ));

                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("createserverpack.log.error.buttonjavapath"),ex);
                }
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 7;
        createServerPackPanel.add(buttonJavaPath, constraints);

        //Load config from file
        buttonLoadConfigFromFile = new JButton();
        buttonLoadConfigFromFile.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig"));
        buttonLoadConfigFromFile.setIcon(loadIcon);
        buttonLoadConfigFromFile.setMinimumSize(miscButtonDimension);
        buttonLoadConfigFromFile.setPreferredSize(miscButtonDimension);
        buttonLoadConfigFromFile.setMaximumSize(miscButtonDimension);
        buttonLoadConfigFromFile.addActionListener(e -> {
            configChooser = new JFileChooser();

            configChooser.setCurrentDirectory(new File("."));
            configChooser.setDialogTitle(localizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig.title"));
            configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            configChooser.setFileFilter(new FileNameExtensionFilter(
                    localizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig.filter"),
                    "conf"));

            configChooser.setAcceptAllFileFilterUsed(false);
            configChooser.setMultiSelectionEnabled(false);
            configChooser.setPreferredSize(chooserDimension);

            if (configChooser.showOpenDialog(configChooser) == JFileChooser.APPROVE_OPTION) {

                Config newConfigFile = null;

                try {
                    newConfigFile = ConfigFactory.parseFile(new File(configChooser.getSelectedFile().getCanonicalPath()));

                    appLogger.info(String.format(
                            localizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"),
                            configChooser.getSelectedFile().getCanonicalPath()
                    ));

                } catch (IOException ex) {
                    appLogger.error(localizationManager.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"),ex);
                }

                if (newConfigFile != null) {

                    textModpackDir.setText(newConfigFile.getString("modpackDir"));

                    textClientMods.setText(configuration.buildString(newConfigFile.getStringList("clientMods").toString()));

                    textCopyDirs.setText(configuration.buildString(newConfigFile.getStringList("copyDirs").toString()));

                    textJavaPath.setText(newConfigFile.getString("javaPath"));

                    textMinecraftVersion.setText(newConfigFile.getString("minecraftVersion"));

                    textModloader.setText(newConfigFile.getString("modLoader"));

                    textModloaderVersion.setText(newConfigFile.getString("modLoaderVersion"));

                    checkBoxServer.setSelected(configuration.convertToBoolean(newConfigFile.getString("includeServerInstallation")));

                    checkBoxIcon.setSelected(configuration.convertToBoolean(newConfigFile.getString("includeServerIcon")));

                    checkBoxProperties.setSelected(configuration.convertToBoolean(newConfigFile.getString("includeServerProperties")));

                    checkBoxScripts.setSelected(configuration.convertToBoolean(newConfigFile.getString("includeStartScripts")));

                    checkBoxZIP.setSelected(configuration.convertToBoolean(newConfigFile.getString("includeZipCreation")));

                    appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile.finish"));
                }
            }
        });
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        createServerPackPanel.add(buttonLoadConfigFromFile, constraints);

        //Load config from file
        buttonInfoWindow = new JButton();
        buttonInfoWindow.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.button"));
        buttonInfoWindow.setIcon(helpIcon);
        buttonInfoWindow.setMinimumSize(miscButtonDimension);
        buttonInfoWindow.setPreferredSize(miscButtonDimension);
        buttonInfoWindow.setMaximumSize(miscButtonDimension);
        buttonInfoWindow.addActionListener(e -> {

            helpTextArea = new JTextArea(String.format(
                    "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.modpackdir"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.clientsidemods"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.directories"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.pathtojava"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.minecraftversion"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.modloader"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.modloaderversion"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.installserver"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.copypropertires"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.copyscripts"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.copyicon"),
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.createzip")
            ));

            helpTextArea.setEditable(false);
            helpTextArea.setOpaque(false);

            helpScrollPane = new JScrollPane(
                    helpTextArea,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            );
            helpScrollPane.setBorder(null);

            helpTextArea.addHierarchyListener(
                    new HierarchyListener() {
                        @Override
                        public void hierarchyChanged(HierarchyEvent e) {
                            Window window = SwingUtilities.getWindowAncestor(helpTextArea);
                            if (window instanceof Dialog) {
                                Dialog dialog = (Dialog) window;
                                if (!dialog.isResizable()) {
                                    dialog.setResizable(true);
                                }
                            }
                        }
                    });

            JOptionPane.showMessageDialog(
                    createServerPackPanel,
                    helpScrollPane,
                    localizationManager.getLocalizedString("createserverpack.gui.createserverpack.help.title"),
                    JOptionPane.INFORMATION_MESSAGE,
                    helpIcon
            );

        });
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 3;
        constraints.gridy = 5;
        constraints.gridheight = 3;
        createServerPackPanel.add(buttonInfoWindow, constraints);

// ---------------------------------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5,0,5,0);

        labelGenerateServerPack = new JLabel(localizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.ready"));
        labelGenerateServerPack.setFont(new Font(labelGenerateServerPack.getFont().getName(), Font.BOLD, labelGenerateServerPack.getFont().getSize()));
        constraints.gridx = 0;
        constraints.gridy = 18;
        constraints.gridwidth = 4;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.PAGE_END;
        createServerPackPanel.add(labelGenerateServerPack, constraints);

        buttonGenerateServerPack = new JButton();
        buttonGenerateServerPack.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.tip"));
        buttonGenerateServerPack.setIcon(startGeneration);
        buttonGenerateServerPack.addActionListener(e -> {

            buttonGenerateServerPack.setEnabled(false);

            appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));
            labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));

            configuration.writeConfigToFile(
                    textModpackDir.getText(),
                    textClientMods.getText(),
                    textCopyDirs.getText(),
                    checkBoxServer.isSelected(),
                    textJavaPath.getText(),
                    textMinecraftVersion.getText(),
                    textModloader.getText(),
                    textModloaderVersion.getText(),
                    checkBoxIcon.isSelected(),
                    checkBoxProperties.isSelected(),
                    checkBoxScripts.isSelected(),
                    checkBoxZIP.isSelected(),
                    new File("serverpackcreator.tmp"),
                    true
            );
            if (!configuration.checkConfigFile(new File("serverpackcreator.tmp"), false)) {
                appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));
                labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));

                if (new File("serverpackcreator.tmp").exists()) {
                    boolean delTmp = new File("serverpackcreator.tmp").delete();
                    if (delTmp) {
                        labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                        appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                    } else {
                        labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                        appLogger.error(localizationManager.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                    }
                }

                appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));
                labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));
                configuration.writeConfigToFile(
                        textModpackDir.getText(),
                        textClientMods.getText(),
                        textCopyDirs.getText(),
                        checkBoxServer.isSelected(),
                        textJavaPath.getText(),
                        textMinecraftVersion.getText(),
                        textModloader.getText(),
                        textModloaderVersion.getText(),
                        checkBoxIcon.isSelected(),
                        checkBoxProperties.isSelected(),
                        checkBoxScripts.isSelected(),
                        checkBoxZIP.isSelected(),
                        configuration.getConfigFile(),
                        false
                );

                appLogger.info(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));
                labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));


                Tailer tailer = Tailer.create(new File("./logs/serverpackcreator.log"), new TailerListenerAdapter() {
                    public void handle(String line) {
                        synchronized (this) {
                            labelGenerateServerPack.setText(line.substring(line.indexOf(") - ") + 4));
                        }
                    }
                }, 100, false);

                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    if (createServerPack.run()) {
                        tailer.stop();

                        System.gc();
                        System.runFinalization();

                        buttonGenerateServerPack.setEnabled(true);
                        labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));
                        textModpackDir.setText(configuration.getModpackDir());
                        textCopyDirs.setText(configuration.getCopyDirs().toString().replace("[","").replace("]",""));

                        JTextArea textArea = new JTextArea();
                        textArea.setOpaque(false);
                        textArea.setText(String.format(
                                "%s\n%s",
                                localizationManager.getLocalizedString("createserverpack.gui.createserverpack.openfolder.browse"),
                                String.format("%s/server_pack",configuration.getModpackDir())
                                )
                        );

                        if (JOptionPane.showConfirmDialog(
                                createServerPackPanel,
                                textArea,
                                localizationManager.getLocalizedString("createserverpack.gui.createserverpack.openfolder.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE) == 0) {
                            try {
                                Desktop.getDesktop().open(new File(String.format("%s/server_pack",configuration.getModpackDir())));
                            } catch (IOException ex) {
                                appLogger.error(localizationManager.getLocalizedString("createserverpack.log.error.browserserverpack"));
                            }
                        }
                        buttonGenerateServerPack.setEnabled(true);
                        System.gc();
                        System.runFinalization();
                        tailer.stop();
                        executorService.shutdown();

                    } else {
                        tailer.stop();

                        System.gc();
                        System.runFinalization();

                        buttonGenerateServerPack.setEnabled(true);
                        labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));

                        executorService.shutdown();
                    }
                });
            } else {
                labelGenerateServerPack.setText(localizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.fail"));
                buttonGenerateServerPack.setEnabled(true);
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 17;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.ipadx = 80;
        constraints.anchor = GridBagConstraints.CENTER;
        createServerPackPanel.add(buttonGenerateServerPack, constraints);

// --------------------------------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        constraints.fill = GridBagConstraints.NONE;

        try {
            if (new File("serverpackcreator.conf").exists()) {
                File configFile = new File("serverpackcreator.conf");
                Config config = ConfigFactory.parseFile(configFile);

                textModpackDir.setText(config.getString("modpackDir"));

                textClientMods.setText(configuration.buildString(config.getStringList("clientMods").toString()));

                textCopyDirs.setText(configuration.buildString(config.getStringList("copyDirs").toString()));

                textJavaPath.setText(config.getString("javaPath"));

                textMinecraftVersion.setText(config.getString("minecraftVersion"));

                textModloader.setText(config.getString("modLoader"));

                textModloaderVersion.setText(config.getString("modLoaderVersion"));

                checkBoxServer.setSelected(configuration.convertToBoolean(config.getString("includeServerInstallation")));

                checkBoxIcon.setSelected(configuration.convertToBoolean(config.getString("includeServerIcon")));

                checkBoxProperties.setSelected(configuration.convertToBoolean(config.getString("includeServerProperties")));

                checkBoxScripts.setSelected(configuration.convertToBoolean(config.getString("includeStartScripts")));

                checkBoxZIP.setSelected(configuration.convertToBoolean(config.getString("includeZipCreation")));
            }
        } catch (NullPointerException ignored) {}

        return createServerPackPanel;
    }
}