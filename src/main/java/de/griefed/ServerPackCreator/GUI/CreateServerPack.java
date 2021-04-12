package de.griefed.ServerPackCreator.GUI;

import javax.swing.*;
import java.awt.*;

public class CreateServerPack extends Component  {

    JComponent createServerPack() {
        JComponent createServerPackPanel = new JPanel(false);
        createServerPackPanel.setPreferredSize(ReferenceGUI.panelDimension);
        //createServerPackPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        createServerPackPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.5;

        //Label and textfield modpackDir
        JLabel labelModpackDir = new JLabel("Enter the path to your modpack directory:");
        labelModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelModpackDir, constraints);

        JTextField textModpackDir = new JTextField("");
        textModpackDir.setToolTipText("Example: \"./Survive Create Prosper 4\" or CurseForge projectID,fileID combination 390331,3215793");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textModpackDir, constraints);

        //Label and textfield clientMods
        JLabel labelClientMods = new JLabel("Enter the list of clientside-only mods in your modpack:");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelClientMods, constraints);
        JTextField textClientMods = new JTextField("Comma separated with no spaces.");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textClientMods, constraints);

        //Label and textfield javaPath
        JLabel labelJavaPath = new JLabel("Enter the path to your Java executable/binary:");
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelJavaPath, constraints);
        JTextField textJavaPath = new JTextField("Must end with /java.exe or /java.");
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textJavaPath, constraints);

        //Label and textfield minecraftVersion
        JLabel labelMinecraftVersion = new JLabel("Enter the Minecraft version your modpack uses:");
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelMinecraftVersion, constraints);
        JTextField textMinecraftVersion = new JTextField("Example: 1.16.5");
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textMinecraftVersion, constraints);

        //Label and textfield Modloader
        JLabel labelModloader = new JLabel("Enter the modloader your modpack uses:");
        constraints.gridx = 0;
        constraints.gridy = 12;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelModloader, constraints);
        JTextField textModloader = new JTextField("Either Forge or Fabric.");
        constraints.gridx = 0;
        constraints.gridy = 13;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textModloader, constraints);

        //Label and textfield modloaderVersion
        JLabel labelModloaderVersion = new JLabel("Path to Java executable/binary:");
        constraints.gridx = 0;
        constraints.gridy = 15;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelModloaderVersion, constraints);
        JTextField textModloaderVersion = new JTextField("Must end with /java.exe or /java.");
        constraints.gridx = 0;
        constraints.gridy = 16;
        constraints.insets = new Insets(0,0,0,0);
        createServerPackPanel.add(textModloaderVersion, constraints);

        //Label and checkBox installServer
        JLabel labelServer = new JLabel("Install modloader server in server pack?");
        constraints.gridx = 0;
        constraints.gridy = 18;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelServer, constraints);
        JCheckBox checkBoxServer = new JCheckBox();
        constraints.gridx = 1;
        constraints.gridy = 18;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(checkBoxServer, constraints);

        //Label and checkBox installServer
        JLabel labelIcon = new JLabel("Include server-icon.png in server pack?");
        constraints.gridx = 0;
        constraints.gridy = 20;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelIcon, constraints);
        JCheckBox checkBoxIcon = new JCheckBox();
        constraints.gridx = 1;
        constraints.gridy = 20;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(checkBoxIcon, constraints);

        //Label and checkBox installServer
        JLabel labelProperties = new JLabel("Include server.properties in server pack?");
        constraints.gridx = 0;
        constraints.gridy = 22;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelProperties, constraints);
        JCheckBox checkBoxProperties = new JCheckBox();
        constraints.gridx = 1;
        constraints.gridy = 22;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(checkBoxProperties, constraints);

        //Label and checkBox installServer
        JLabel labelScripts = new JLabel("Include start scripts in server pack?");
        constraints.gridx = 0;
        constraints.gridy = 24;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelScripts, constraints);
        JCheckBox checkBoxScripts = new JCheckBox();
        constraints.gridx = 1;
        constraints.gridy = 24;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(checkBoxScripts, constraints);

        //Label and checkBox installServer
        JLabel labelZIP = new JLabel("Create ZIP-archive of server pack?");
        constraints.gridx = 0;
        constraints.gridy = 26;
        constraints.insets = new Insets(10,0,0,0);
        createServerPackPanel.add(labelZIP, constraints);
        JCheckBox checkBoxZIP = new JCheckBox();
        constraints.gridx = 1;
        constraints.gridy = 26;
        constraints.insets = new Insets(10,10,0,0);
        createServerPackPanel.add(checkBoxZIP, constraints);

        //Select modpackDir button
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        JButton buttonModpackDir = new JButton();
        buttonModpackDir.setIcon(ReferenceGUI.folderIcon);
        buttonModpackDir.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(buttonModpackDir, constraints);

        //Select javaPath button
        JButton buttonJavaPath = new JButton();
        buttonJavaPath.setIcon(ReferenceGUI.folderIcon);
        buttonJavaPath.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.insets = new Insets(0,10,0,0);
        createServerPackPanel.add(buttonJavaPath, constraints);

        //Start server pack generation button
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JButton buttonGenerateServerPack = new JButton();
        buttonGenerateServerPack.setIcon(ReferenceGUI.startGeneration);
        buttonGenerateServerPack.setPreferredSize(ReferenceGUI.folderButtonDimension);
        constraints.anchor = GridBagConstraints.PAGE_END;
        constraints.gridwidth = 3;
        //constraints.gridheight = 3;
        constraints.ipady = 50;
        constraints.ipadx = 50;
        constraints.weighty = 1.0;
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.gridy = 28;
        constraints.insets = new Insets(20,50,20,50);
        createServerPackPanel.add(buttonGenerateServerPack, constraints);

        constraints.fill = GridBagConstraints.VERTICAL;

        return createServerPackPanel;
    }
}