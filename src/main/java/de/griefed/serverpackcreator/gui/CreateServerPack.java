package de.griefed.serverpackcreator.gui;

import javax.swing.*;
import java.awt.*;

public class CreateServerPack extends Component  {

    JComponent createServerPack() {
        JComponent createServerPackPanel = new JPanel(false);
        createServerPackPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        //Labels and textinputs
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;

        //Label and textfield modpackDir
        JLabel labelModpackDir = new JLabel("Enter the path to your modpack directory:");
        labelModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(labelModpackDir, constraints);
        JTextField textModpackDir = new JTextField("");
        textModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModpackDir, constraints);

        //Label and textfield clientMods
        JLabel labelClientMods = new JLabel("Enter the list of clientside-only mods in your modpack:");
        labelClientMods.setToolTipText("Comma separated with no spaces. Example: AmbientSounds,BackTools,BetterAdvancement,BetterPing");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(labelClientMods, constraints);
        JTextField textClientMods = new JTextField("");
        textClientMods.setToolTipText("Comma separated with no spaces. Example: AmbientSounds,BackTools,BetterAdvancement,BetterPing");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textClientMods, constraints);

        //Label and textfield javaPath
        JLabel labelJavaPath = new JLabel("Enter the path to your Java executable/binary:");
        labelJavaPath.setToolTipText("Must end with /java.exe or /java. Example: C:/Program Files/AdoptOpenJDK/jdk-8.0.275.1-hotspot/jre/bin/java.exe");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.insets = new Insets(10,10,0,0);
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
        constraints.gridy = 9;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(labelMinecraftVersion, constraints);
        JTextField textMinecraftVersion = new JTextField("");
        textMinecraftVersion.setToolTipText("Example: 1.16.5 or 1.12.2");
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textMinecraftVersion, constraints);

        //Label and textfield Modloader
        JLabel labelModloader = new JLabel("Enter the modloader your modpack uses:");
        labelModloader.setToolTipText("Must be either Forge or Fabric.");
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(labelModloader, constraints);
        JTextField textModloader = new JTextField("");
        textModloader.setToolTipText("Must be either Forge or Fabric.");
        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloader, constraints);

        //Label and textfield modloaderVersion
        JLabel labelModloaderVersion = new JLabel("Path to Java executable/binary:");
        labelModloaderVersion.setToolTipText("Example Forge: 36.1.4 - Example Fabric: 0.7.3");
        constraints.gridx = 0;
        constraints.gridy = 15;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(labelModloaderVersion, constraints);
        JTextField textModloaderVersion = new JTextField("");
        textModloaderVersion.setToolTipText("Example Forge: 36.1.4 - Example Fabric: 0.7.3");
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(textModloaderVersion, constraints);

        //Labels and checkboxes
        constraints.insets = new Insets(10,10,0,0);

        //Label installServer
        JLabel labelServer = new JLabel("Install modloader server in server pack?");
        labelServer.setToolTipText("Whether to install the server-software for the chosen modloader.");
        constraints.gridx = 0;
        constraints.gridy = 18;
        createServerPackPanel.add(labelServer, constraints);

        //Label copyIcon
        JLabel labelIcon = new JLabel("Include server-icon.png in server pack?");
        labelIcon.setToolTipText("Whether to copy the server-icon.png from server_files to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 20;
        createServerPackPanel.add(labelIcon, constraints);

        //Label copyProperties
        JLabel labelProperties = new JLabel("Include server.properties in server pack?");
        labelProperties.setToolTipText("Whether to copy the server.properties to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 22;
        createServerPackPanel.add(labelProperties, constraints);

        //Label copyStartScripts
        JLabel labelScripts = new JLabel("Include start scripts in server pack?");
        labelScripts.setToolTipText("Whether to copy the start scripts for the chosen modloader to the server pack.");
        constraints.gridx = 0;
        constraints.gridy = 24;
        createServerPackPanel.add(labelScripts, constraints);

        //Label createZIParchive
        JLabel labelZIP = new JLabel("Create ZIP-archive of server pack?");
        labelZIP.setToolTipText("Whether to create a ZIP-archive of the server pack for immediate upload.");
        constraints.gridx = 0;
        constraints.gridy = 26;
        createServerPackPanel.add(labelZIP, constraints);

        //Checkboxes and buttons
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;

        //Checkbox installServer
        JCheckBox checkBoxServer = new JCheckBox("",true);
        constraints.gridx = 1;
        constraints.gridy = 18;
        createServerPackPanel.add(checkBoxServer, constraints);

        //Checkbox copyIcon
        JCheckBox checkBoxIcon = new JCheckBox("",true);
        constraints.gridx = 1;
        constraints.gridy = 20;
        createServerPackPanel.add(checkBoxIcon, constraints);

        //Checkbox copyProperties
        JCheckBox checkBoxProperties = new JCheckBox("",true);
        constraints.gridx = 1;
        constraints.gridy = 22;
        createServerPackPanel.add(checkBoxProperties, constraints);

        //Checkbox copyScripts
        JCheckBox checkBoxScripts = new JCheckBox("",true);
        constraints.gridx = 1;
        constraints.gridy = 24;
        createServerPackPanel.add(checkBoxScripts, constraints);

        //Checkbox createZIP
        JCheckBox checkBoxZIP = new JCheckBox("",true);
        constraints.gridx = 1;
        constraints.gridy = 26;
        createServerPackPanel.add(checkBoxZIP, constraints);

        //Buttons
        constraints.insets = new Insets(0,10,0,0);

        //Select modpackDir button
        JButton buttonModpackDir = new JButton();
        buttonModpackDir.setToolTipText("Open the file explorer to select the modpack directory.");
        buttonModpackDir.setIcon(ReferenceGUI.folderIcon);
        buttonModpackDir.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.gridx = 1;
        constraints.gridy = 1;
        createServerPackPanel.add(buttonModpackDir, constraints);

        //Select javaPath button
        JButton buttonJavaPath = new JButton();
        buttonJavaPath.setToolTipText("Open the file explorer to select the Java binary/executable.");
        buttonJavaPath.setIcon(ReferenceGUI.folderIcon);
        buttonJavaPath.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.gridx = 1;
        constraints.gridy = 7;
        createServerPackPanel.add(buttonJavaPath, constraints);

        //Load config from URL
        JButton buttonLoadConfigURL = new JButton();
        buttonLoadConfigURL.setToolTipText("Load a config from a URL. URL must be a direct link to a serverpackcreator.conf file.");
        buttonLoadConfigURL.setIcon(ReferenceGUI.loadIcon);
        buttonLoadConfigURL.setPreferredSize(ReferenceGUI.miscButtonDimension);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(buttonLoadConfigURL, constraints);


        //Main action button
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.CENTER;

        //Start server pack generation button
        JButton buttonGenerateServerPack = new JButton();
        buttonGenerateServerPack.setToolTipText("Check the configuration for errors and start the generation of the server pack.");
        buttonGenerateServerPack.setIcon(ReferenceGUI.startGeneration);
        buttonGenerateServerPack.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.gridwidth = 3;
        constraints.ipady = 70;
        //constraints.weighty = 1.0;
        //constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.gridy = 27;
        constraints.insets = new Insets(30,180,20,180);
        createServerPackPanel.add(buttonGenerateServerPack, constraints);

        //Leftovers
        constraints.fill = GridBagConstraints.VERTICAL;

        return createServerPackPanel;
    }


}