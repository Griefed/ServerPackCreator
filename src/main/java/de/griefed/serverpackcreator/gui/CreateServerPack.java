package de.griefed.serverpackcreator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.griefed.serverpackcreator.Reference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CreateServerPack extends Component  {
    private static final Logger appLogger = LogManager.getLogger(CreateServerPack.class);

    JComponent createServerPack() {
        JComponent createServerPackPanel = new JPanel(false);
        createServerPackPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

// ----------------------------------------------------------------------------------------LABELS AND TEXTFIELDS--------
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.7;

        //Label and textfield modpackDir
        JLabel labelModpackDir = new JLabel("Enter the path to your modpack directory:");
        labelModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModpackDir, constraints);
        JTextField textModpackDir = new JTextField("");
        textModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModpackDir, constraints);

        //Label and textfield clientMods
        JLabel labelClientMods = new JLabel("Enter the list of clientside-only mods in your modpack:");
        labelClientMods.setToolTipText("Comma separated. Example: AmbientSounds,BackTools,BetterAdvancement,BetterPing");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelClientMods, constraints);
        JTextField textClientMods = new JTextField("");
        textClientMods.setToolTipText("Comma separated. Example: AmbientSounds,BackTools,BetterAdvancement,BetterPing");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textClientMods, constraints);

        //Label and textfield copyDirs
        JLabel labelCopyDirs = new JLabel("Enter the list of directories in your modpack to include in the server pack:");
        labelCopyDirs.setToolTipText("Comma separated. Example: mods,config,defaultconfigs,scripts");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelCopyDirs, constraints);
        JTextField textCopyDirs = new JTextField("");
        textCopyDirs.setToolTipText("Comma separated. Example: mods,config,defaultconfigs,scripts");
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textCopyDirs, constraints);

        //Label and textfield javaPath
        JLabel labelJavaPath = new JLabel("Enter the path to your Java executable/binary:");
        labelJavaPath.setToolTipText("Must end with /java.exe or /java. Example: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelJavaPath, constraints);
        JTextField textJavaPath = new JTextField("");
        textJavaPath.setToolTipText("Must end with /java.exe or /java. Example: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textJavaPath, constraints);

        //Label and textfield minecraftVersion
        JLabel labelMinecraftVersion = new JLabel("Enter the Minecraft version your modpack uses:");
        labelMinecraftVersion.setToolTipText("Example: 1.16.5 or 1.12.2");
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelMinecraftVersion, constraints);
        JTextField textMinecraftVersion = new JTextField("");
        textMinecraftVersion.setToolTipText("Example: 1.16.5 or 1.12.2");
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textMinecraftVersion, constraints);

        //Label and textfield Modloader
        JLabel labelModloader = new JLabel("Enter the modloader your modpack uses:");
        labelModloader.setToolTipText("Must be either Forge or Fabric.");
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloader, constraints);
        JTextField textModloader = new JTextField("");
        textModloader.setToolTipText("Must be either Forge or Fabric.");
        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloader, constraints);

        //Label and textfield modloaderVersion
        JLabel labelModloaderVersion = new JLabel("Enter the version of the modloader your modpack uses:");
        labelModloaderVersion.setToolTipText("Example Forge: 36.1.4 - Example Fabric: 0.7.3");
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.insets = new Insets(20,10,0,0);
        createServerPackPanel.add(labelModloaderVersion, constraints);
        JTextField textModloaderVersion = new JTextField("");
        textModloaderVersion.setToolTipText("Example Forge: 36.1.4 - Example Fabric: 0.7.3");
        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloaderVersion, constraints);

// ----------------------------------------------------------------------------------------LABELS AND CHECKBOXES--------
        constraints.insets = new Insets(10,10,0,0);

        //Checkboxes
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.fill = GridBagConstraints.NONE;

        //Checkbox installServer
        JCheckBox checkBoxServer = new JCheckBox("Install modloader server in server pack?",true);
        checkBoxServer.setToolTipText("Whether to install the server-software for the chosen modloader.");
        constraints.gridx = 0;
        constraints.gridy = 14;
        createServerPackPanel.add(checkBoxServer, constraints);

        //Checkbox copyIcon
        JCheckBox checkBoxIcon = new JCheckBox("Include server-icon.png in server pack?",true);
        checkBoxIcon.setToolTipText("Whether to copy the server-icon.png from server_files to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 15;
        createServerPackPanel.add(checkBoxIcon, constraints);

        //Checkbox copyProperties
        JCheckBox checkBoxProperties = new JCheckBox("Include server.properties in server pack?",true);
        checkBoxProperties.setToolTipText("Whether to copy the server.properties to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 16;
        createServerPackPanel.add(checkBoxProperties, constraints);

        //Checkbox copyScripts
        JCheckBox checkBoxScripts = new JCheckBox("Include start scripts in server pack?",true);
        checkBoxScripts.setToolTipText("Whether to copy the start scripts for the chosen modloader to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 17;
        createServerPackPanel.add(checkBoxScripts, constraints);

        //Checkbox createZIP
        JCheckBox checkBoxZIP = new JCheckBox("Create ZIP-archive of server pack?",true);
        checkBoxZIP.setToolTipText("Whether to create a ZIP-archive of the server pack for immediate upload.");
        constraints.gridx = 0;
        constraints.gridy = 18;
        createServerPackPanel.add(checkBoxZIP, constraints);

// ------------------------------------------------------------------------------------------------------BUTTONS--------
        constraints.insets = new Insets(0,10,0,10);

        constraints.weightx = 0;
        constraints.weighty = 0;

        //Select modpackDir button
        JButton buttonModpackDir = new JButton();
        buttonModpackDir.setToolTipText("Open the file explorer to select the modpack directory.");
        buttonModpackDir.setIcon(ReferenceGUI.folderIcon);
        buttonModpackDir.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonModpackDir.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();

            folderChooser.setCurrentDirectory(new File("."));
            folderChooser.setDialogTitle("Select modpack directory.");
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            folderChooser.setAcceptAllFileFilterUsed(false);
            folderChooser.setMultiSelectionEnabled(false);
            folderChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (folderChooser.showOpenDialog(folderChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textModpackDir.setText(folderChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));
                    appLogger.info(String.format("Selected modpack directory: %s", folderChooser.getSelectedFile().getCanonicalPath().replace("\\","/")));
                } catch (IOException ex) {
                    appLogger.error("Error getting directory from modpack directory chooser.",ex);
                }
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 1;
        createServerPackPanel.add(buttonModpackDir, constraints);

        //Select clientside-mods button
        JButton buttonClientMods = new JButton();
        buttonClientMods.setToolTipText("Open the file explorer to select the clientside-only mods in your modpack.");
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
            fileChooser.setDialogTitle("Select Clientside-Only mods in modpack.");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Minecraft Mod-Jar","jar"));
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
                appLogger.info(String.format("Selected mods: %s", clientModsFilenames));
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 3;
        createServerPackPanel.add(buttonClientMods, constraints);

        //Select directories to copy to server pack button
        JButton buttonCopyDirs = new JButton();
        buttonCopyDirs.setToolTipText("Open the file explorer to select the directories to include in the server pack.");
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
            fileChooser.setDialogTitle("Select directories to include in server pack.");
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
                appLogger.info(String.format("Selected directories: %s", copyDirsNames));
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 5;
        createServerPackPanel.add(buttonCopyDirs, constraints);

        //Select javaPath button
        JButton buttonJavaPath = new JButton();
        buttonJavaPath.setToolTipText("Open the file explorer to select the Java binary/executable.");
        buttonJavaPath.setIcon(ReferenceGUI.folderIcon);
        buttonJavaPath.setMinimumSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.setPreferredSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.setMaximumSize(ReferenceGUI.folderButtonDimension);
        buttonJavaPath.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle("Select Java executable/binary.");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Java Binary/Executable", "java", "exe"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {
                try {
                    textJavaPath.setText(fileChooser.getSelectedFile().getCanonicalPath().replace("\\","/"));
                    appLogger.info(String.format("Set path to Java executable to: %s", fileChooser.getSelectedFile().getCanonicalPath().replace("\\","/")));
                } catch (IOException ex) {
                    appLogger.error("Error getting directory from modpack directory chooser.",ex);
                }
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 7;
        createServerPackPanel.add(buttonJavaPath, constraints);

        //Load config from file
        JButton buttonLoadConfigFromFile = new JButton();
        buttonLoadConfigFromFile.setToolTipText("Load configuration from a file.");
        buttonLoadConfigFromFile.setIcon(ReferenceGUI.loadIcon);
        buttonLoadConfigFromFile.setMinimumSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.setPreferredSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.setMaximumSize(ReferenceGUI.miscButtonDimension);
        buttonLoadConfigFromFile.addActionListener(e -> {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle("Select serverpackcreator.conf to load.");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("ServerPackCreator Configuration-File","conf"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setPreferredSize(ReferenceGUI.chooserDimension);

            if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {

                Config newConfigFile = null;

                try {
                    newConfigFile = ConfigFactory.parseFile(new File(fileChooser.getSelectedFile().getCanonicalPath()));
                    appLogger.info(String.format("Loading from configuration file: %s", fileChooser.getSelectedFile().getCanonicalPath()));
                } catch (IOException ex) {
                    appLogger.error("Error loading configuration from selected file.",ex);
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

                    appLogger.info("Configuration successfully loaded.");
                }
            }
        });
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        createServerPackPanel.add(buttonLoadConfigFromFile, constraints);

// ---------------------------------------------------------------------------------MAIN ACTION BUTTON AND LABEL--------
        constraints.weighty = 1.0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5,0,5,0);

        JLabel labelGenerateServerPack = new JLabel("ServerPackCreator ready.");
        constraints.gridx = 0;
        constraints.gridy = 19;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.PAGE_START;
        createServerPackPanel.add(labelGenerateServerPack, constraints);
        JButton buttonGenerateServerPack = new JButton();
        buttonGenerateServerPack.setToolTipText("Check the configuration for errors and start the generation of the server pack.");
        buttonGenerateServerPack.setIcon(ReferenceGUI.startGeneration);
        buttonGenerateServerPack.setPreferredSize(ReferenceGUI.startGenerationButton);
        buttonGenerateServerPack.addActionListener(e -> {
            buttonGenerateServerPack.setEnabled(false);
            appLogger.info("Checking entered configuration.");
            labelGenerateServerPack.setText("Checking entered configuration.");
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
                appLogger.info("Configuration checked successfully.");
                labelGenerateServerPack.setText("Configuration checked successfully.");

                if (new File("serverpackcreator.tmp").exists()) {
                    boolean delTmp = new File("serverpackcreator.tmp").delete();
                    if (delTmp) {
                        labelGenerateServerPack.setText("Deleted temporary config file.");
                        appLogger.info("Deleted temporary config file.");
                    } else {
                        labelGenerateServerPack.setText("Could not delete temporary config file.");
                        appLogger.error("Could not delete temporary config file.");
                    }
                }

                appLogger.info("Writing configuration to file.");
                labelGenerateServerPack.setText("Writing configuration to file.");
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

                appLogger.info("Generating server pack.");
                labelGenerateServerPack.setText("Generating server pack. Check the ServerPackCreator Log and Modloader-Installer Log tabs.");
                final ScheduledExecutorService readLogExecutor = Executors.newSingleThreadScheduledExecutor();
                readLogExecutor.scheduleWithFixedDelay(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader("./logs/serverpackcreator.log"));
                        while (true) {
                            try {
                                String text = reader.readLine();
                                if (text == null) { break; }
                                labelGenerateServerPack.setText(text.substring(23));
                            } catch (IOException ex) {
                                appLogger.error("Error reading log.", ex);
                            }
                        }}
                    catch (IOException ex) { appLogger.error("Log file not found.", ex); }
                }, 2, 1, TimeUnit.SECONDS);
                final ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    if (ReferenceGUI.handler.run()) {
                        labelGenerateServerPack.setText("ServerPackCreator ready.");
                        buttonGenerateServerPack.setEnabled(true);
                        readLogExecutor.shutdown();
                        executorService.shutdown();
                    } else {
                        labelGenerateServerPack.setText("ServerPackCreator ready.");
                        buttonGenerateServerPack.setEnabled(true);
                        readLogExecutor.shutdown();
                        executorService.shutdown();
                    }
                });
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 20;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.PAGE_END;
        createServerPackPanel.add(buttonGenerateServerPack, constraints);

// --------------------------------------------------------------------------------LEFTOVERS AND EVERYTHING ELSE--------
        constraints.fill = GridBagConstraints.NONE;

        try {
            if (new File("serverpackcreator.conf").exists()) {
                File configFile = new File("serverpackcreator.conf");
                Config config = ConfigFactory.parseFile(configFile);

                appLogger.info("Loading configuration from file.");

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

                appLogger.info("Configuration successfully loaded.");
            }
        } catch (NullPointerException ex) {
            appLogger.error("File or config string not found.");
        }

        return createServerPackPanel;
    }
}