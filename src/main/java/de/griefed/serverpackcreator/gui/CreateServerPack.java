package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Handler;
import de.griefed.serverpackcreator.Reference;
import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateServerPack extends Component  {
    private static final Logger appLogger = LogManager.getLogger(CreateServerPack.class);

    JComponent createServerPack() {
        JComponent createServerPackPanel = new JPanel(false);
        createServerPackPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

// ----------------------------------------------------------------------------------------LABELS AND TEXTFIELDS--------
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.weightx = 0.7;

        //Label and textfield modpackDir
        JLabel labelModpackDir = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir"));
        labelModpackDir.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModpackDir, constraints);
        JTextField textModpackDir = new JTextField("");
        textModpackDir.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodpackdir.tip"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModpackDir, constraints);

        //Label and textfield clientMods
        JLabel labelClientMods = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods"));
        labelClientMods.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelClientMods, constraints);
        JTextField textClientMods = new JTextField("");
        textClientMods.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelclientmods.tip"));
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textClientMods, constraints);

        //Label and textfield copyDirs
        JLabel labelCopyDirs = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs"));
        labelCopyDirs.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelCopyDirs, constraints);
        JTextField textCopyDirs = new JTextField("");
        textCopyDirs.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelcopydirs.tip"));
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textCopyDirs, constraints);

        //Label and textfield javaPath
        JLabel labelJavaPath = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath"));
        labelJavaPath.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelJavaPath, constraints);
        JTextField textJavaPath = new JTextField("");
        textJavaPath.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labeljavapath.tip"));
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textJavaPath, constraints);

        //Label and textfield minecraftVersion
        JLabel labelMinecraftVersion = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft"));
        labelMinecraftVersion.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelMinecraftVersion, constraints);
        JTextField textMinecraftVersion = new JTextField("");
        textMinecraftVersion.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelminecraft.tip"));
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textMinecraftVersion, constraints);

        //Label and textfield Modloader
        JLabel labelModloader = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader"));
        labelModloader.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloader, constraints);
        JTextField textModloader = new JTextField("");
        textModloader.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloader.tip"));
        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloader, constraints);

        //Label and textfield modloaderVersion
        JLabel labelModloaderVersion = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion"));
        labelModloaderVersion.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloaderVersion, constraints);
        JTextField textModloaderVersion = new JTextField("");
        textModloaderVersion.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.labelmodloaderversion.tip"));
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
        JCheckBox checkBoxServer = new JCheckBox(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver"),true);
        checkBoxServer.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxserver.tip"));
        constraints.gridx = 0;
        constraints.gridy = 14;
        createServerPackPanel.add(checkBoxServer, constraints);

        //Checkbox copyIcon
        JCheckBox checkBoxIcon = new JCheckBox(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon"),true);
        checkBoxIcon.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxicon.tip"));
        constraints.gridx = 1;
        constraints.gridy = 14;
        createServerPackPanel.add(checkBoxIcon, constraints);

        //Checkbox copyProperties
        JCheckBox checkBoxProperties = new JCheckBox(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties"),true);
        checkBoxProperties.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxproperties.tip"));
        constraints.gridx = 0;
        constraints.gridy = 15;
        createServerPackPanel.add(checkBoxProperties, constraints);

        //Checkbox copyScripts
        JCheckBox checkBoxScripts = new JCheckBox(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts"),true);
        checkBoxScripts.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxscripts.tip"));
        constraints.gridx = 1;
        constraints.gridy = 15;
        createServerPackPanel.add(checkBoxScripts, constraints);

        //Checkbox createZIP
        JCheckBox checkBoxZIP = new JCheckBox(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip"),true);
        checkBoxZIP.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.createserverpack.checkboxzip.tip"));
        constraints.gridx = 0;
        constraints.gridy = 16;
        createServerPackPanel.add(checkBoxZIP, constraints);

// ------------------------------------------------------------------------------------------------------BUTTONS--------
        constraints.insets = new Insets(0,10,0,10);

        constraints.weightx = 0;
        constraints.weighty = 0;

        //Select modpackDir button
        JButton buttonModpackDir = new JButton();
        buttonModpackDir.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttonmodpackdir"));
        buttonModpackDir.setIcon(ReferenceGUI.folderIcon);
        buttonModpackDir.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();

            folderChooser.setCurrentDirectory(new File("."));
            folderChooser.setDialogTitle(LocalizationManager.getLocalizedString("createserverpack.gui.buttonmodpackdir.title"));
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setAcceptAllFileFilterUsed(false);
            folderChooser.setMultiSelectionEnabled(false);
            folderChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (folderChooser.showOpenDialog(folderChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textModpackDir.setText(folderChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));
                    appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonmodpack"), folderChooser.getSelectedFile().getCanonicalPath().replace("\\","/")));
                } catch (IOException ex) {
                    appLogger.error(LocalizationManager.getLocalizedString("createserverpack.log.error.buttonmodpack"),ex);
                }
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 1;
        createServerPackPanel.add(buttonModpackDir, constraints);

        //Select clientside-mods button
        JButton buttonClientMods = new JButton();
        buttonClientMods.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttonclientmods"));
        buttonClientMods.setIcon(ReferenceGUI.folderIcon);
        buttonClientMods.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonClientMods.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonClientMods.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonClientMods.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            if (textModpackDir.getText().length() > 0 &&
                new File(textModpackDir.getText()).isDirectory() &&
                new File(String.format("%s/mods",textModpackDir.getText())).isDirectory()) {

                fileChooser.setCurrentDirectory(new File(String.format("%s/mods",textModpackDir.getText())));
            } else {
                fileChooser.setCurrentDirectory(new File("."));
            }
            fileChooser.setDialogTitle(LocalizationManager.getLocalizedString("createserverpack.gui.buttonclientmods.title"));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter(LocalizationManager.getLocalizedString("createserverpack.gui.buttonclientmods.filter"),"jar"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {
                File[] clientMods = fileChooser.getSelectedFiles();
                ArrayList<String> clientModsFilenames = new ArrayList<>();

                for (int i = 0; i < clientMods.length; i++) {
                    clientModsFilenames.add(clientMods[i].getName());
                }

                textClientMods.setText(ReferenceGUI.cliSetup.buildString(Arrays.toString(clientModsFilenames.toArray(new String[0]))));
                appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonclientmods"), clientModsFilenames));
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 3;
        createServerPackPanel.add(buttonClientMods, constraints);

        //Select directories to copy to server pack button
        JButton buttonCopyDirs = new JButton();
        buttonCopyDirs.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttoncopydirs"));
        buttonCopyDirs.setIcon(ReferenceGUI.folderIcon);
        buttonCopyDirs.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonCopyDirs.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonCopyDirs.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonCopyDirs.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            if (textModpackDir.getText().length() > 0 &&
                    new File(textModpackDir.getText()).isDirectory()) {

                fileChooser.setCurrentDirectory(new File(textModpackDir.getText()));
            } else {
                fileChooser.setCurrentDirectory(new File("."));
            }
            fileChooser.setDialogTitle(LocalizationManager.getLocalizedString("createserverpack.gui.buttoncopydirs.title"));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {
                File[] copyDirs = fileChooser.getSelectedFiles();
                ArrayList<String> copyDirsNames = new ArrayList<>();

                for (int i = 0; i < copyDirs.length; i++) {
                    copyDirsNames.add(copyDirs[i].getName());
                }

                textCopyDirs.setText(ReferenceGUI.cliSetup.buildString(Arrays.toString(copyDirsNames.toArray(new String[0]))));
                appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncopydirs"), copyDirsNames));
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 5;
        createServerPackPanel.add(buttonCopyDirs, constraints);

        //Select javaPath button
        JButton buttonJavaPath = new JButton();
        buttonJavaPath.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttonjavapath"));
        buttonJavaPath.setIcon(ReferenceGUI.folderIcon);
        buttonJavaPath.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle(LocalizationManager.getLocalizedString("createserverpack.gui.buttonjavapath.tile"));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter(LocalizationManager.getLocalizedString("createserverpack.gui.buttonjavapath.filter"), "java", "exe"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textJavaPath.setText(fileChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));
                    appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonjavapath"), fileChooser.getSelectedFile().getCanonicalPath().replace("\\","/")));
                } catch (IOException ex) {
                    appLogger.error(LocalizationManager.getLocalizedString("createserverpack.log.error.buttonjavapath"),ex);
                }
            }
        });
        constraints.gridx = 2;
        constraints.gridy = 7;
        createServerPackPanel.add(buttonJavaPath, constraints);

        //Load config from file
        JButton buttonLoadConfigFromFile = new JButton();
        buttonLoadConfigFromFile.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig"));
        buttonLoadConfigFromFile.setIcon(ReferenceGUI.loadIcon);
        buttonLoadConfigFromFile.setMinimumSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.setPreferredSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.setMaximumSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.addActionListener(e -> {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle(LocalizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig.title"));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter(LocalizationManager.getLocalizedString("createserverpack.gui.buttonloadconfig.filter"),"conf"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {

                Config newConfigFile = null;

                try {
                    newConfigFile = ConfigFactory.parseFile(new File(fileChooser.getSelectedFile().getCanonicalPath()));
                    appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"), fileChooser.getSelectedFile().getCanonicalPath()));
                } catch (IOException ex) {
                    appLogger.error(LocalizationManager.getLocalizedString("createserverpack.log.error.buttonloadconfigfromfile"),ex);
                }

                if (newConfigFile != null) {

                    textModpackDir.setText(newConfigFile.getString("modpackDir"));

                    textClientMods.setText(ReferenceGUI.cliSetup.buildString(newConfigFile.getStringList("clientMods").toString()));

                    textCopyDirs.setText(ReferenceGUI.cliSetup.buildString(newConfigFile.getStringList("copyDirs").toString()));

                    textJavaPath.setText(newConfigFile.getString("javaPath"));

                    textMinecraftVersion.setText(newConfigFile.getString("minecraftVersion"));

                    textModloader.setText(newConfigFile.getString("modLoader"));

                    textModloaderVersion.setText(newConfigFile.getString("modLoaderVersion"));

                    checkBoxServer.setSelected(ReferenceGUI.configCheck.convertToBoolean(newConfigFile.getString("includeServerInstallation")));

                    checkBoxIcon.setSelected(ReferenceGUI.configCheck.convertToBoolean(newConfigFile.getString("includeServerIcon")));

                    checkBoxProperties.setSelected(ReferenceGUI.configCheck.convertToBoolean(newConfigFile.getString("includeServerProperties")));

                    checkBoxScripts.setSelected(ReferenceGUI.configCheck.convertToBoolean(newConfigFile.getString("includeStartScripts")));

                    checkBoxZIP.setSelected(ReferenceGUI.configCheck.convertToBoolean(newConfigFile.getString("includeZipCreation")));

                    appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile.finish"));
                }
            }
        });
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        createServerPackPanel.add(buttonLoadConfigFromFile, constraints);

// ---------------------------------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5,0,5,0);

        JLabel labelGenerateServerPack = new JLabel(LocalizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.ready"));
        constraints.gridx = 0;
        constraints.gridy = 18;
        constraints.gridwidth = 4;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.PAGE_END;
        createServerPackPanel.add(labelGenerateServerPack, constraints);

        JButton buttonGenerateServerPack = new JButton();
        buttonGenerateServerPack.setToolTipText(LocalizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.tip"));
        buttonGenerateServerPack.setIcon(ReferenceGUI.startGeneration);
        //buttonGenerateServerPack.setPreferredSize(ReferenceGUI.startGenerationButton);
        buttonGenerateServerPack.addActionListener(e -> {

            buttonGenerateServerPack.setEnabled(false);

            appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));
            labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"));

            Reference.filesSetup.writeConfigToFile(
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
            if (!ReferenceGUI.configCheck.checkConfigFile(new File("serverpackcreator.tmp"))) {
                appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));
                labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.checked"));

                if (new File("serverpackcreator.tmp").exists()) {
                    boolean delTmp = new File("serverpackcreator.tmp").delete();
                    if (delTmp) {
                        labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                        appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.tempfile"));
                    } else {
                        labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                        appLogger.error(LocalizationManager.getLocalizedString("createserverpack.log.error.buttoncreateserverpack.tempfile"));
                    }
                }

                appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));
                labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.writing"));
                Reference.filesSetup.writeConfigToFile(
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
                        ReferenceGUI.configFile,
                        false
                );

                appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));
                labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.generating"));


                Tailer tailer = Tailer.create(new File("./logs/serverpackcreator.log"), new TailerListenerAdapter() {
                    public void handle(String line) {
                        synchronized (this) {
                            labelGenerateServerPack.setText(line.substring(line.indexOf(") - ") + 4));
                        }
                    }
                }, 100, false);

                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    if (Handler.run()) {
                        labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));
                        buttonGenerateServerPack.setEnabled(true);
                        System.gc();
                        System.runFinalization();
                        tailer.stop();
                        executorService.shutdown();
                    } else {
                        labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.ready"));
                        buttonGenerateServerPack.setEnabled(true);
                        System.gc();
                        System.runFinalization();
                        tailer.stop();
                        executorService.shutdown();
                    }
                });
            } else {
                labelGenerateServerPack.setText(LocalizationManager.getLocalizedString("createserverpack.gui.buttongenerateserverpack.fail"));
                buttonGenerateServerPack.setEnabled(true);
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 17;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.ipadx = 80;
        constraints.anchor = GridBagConstraints.CENTER;
        createServerPackPanel.add(buttonGenerateServerPack, constraints);

// --------------------------------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        constraints.fill = GridBagConstraints.NONE;

        try {
            if (new File("serverpackcreator.conf").exists()) {
                File configFile = new File("serverpackcreator.conf");
                Config config = ConfigFactory.parseFile(configFile);

                appLogger.info(String.format(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile"), configFile));

                textModpackDir.setText(config.getString("modpackDir"));

                textClientMods.setText(ReferenceGUI.cliSetup.buildString(config.getStringList("clientMods").toString()));

                textCopyDirs.setText(ReferenceGUI.cliSetup.buildString(config.getStringList("copyDirs").toString()));

                textJavaPath.setText(config.getString("javaPath"));

                textMinecraftVersion.setText(config.getString("minecraftVersion"));

                textModloader.setText(config.getString("modLoader"));

                textModloaderVersion.setText(config.getString("modLoaderVersion"));

                checkBoxServer.setSelected(ReferenceGUI.configCheck.convertToBoolean(config.getString("includeServerInstallation")));

                checkBoxIcon.setSelected(ReferenceGUI.configCheck.convertToBoolean(config.getString("includeServerIcon")));

                checkBoxProperties.setSelected(ReferenceGUI.configCheck.convertToBoolean(config.getString("includeServerProperties")));

                checkBoxScripts.setSelected(ReferenceGUI.configCheck.convertToBoolean(config.getString("includeStartScripts")));

                checkBoxZIP.setSelected(ReferenceGUI.configCheck.convertToBoolean(config.getString("includeZipCreation")));

                appLogger.info(LocalizationManager.getLocalizedString("createserverpack.log.info.buttonloadconfigfromfile.finish"));
            }
        } catch (NullPointerException ignored) {

        }

        return createServerPackPanel;
    }
}