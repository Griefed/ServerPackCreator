package de.griefed.serverpackcreator.gui;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class About extends Component {
    private static final Logger appLogger = LogManager.getLogger(About.class);

    JComponent about() {
        JComponent about = new JPanel(false);
        about.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;


        //Button to upload log file to pastebin
        JButton buttonCheckForUpdate = new JButton();
        buttonCheckForUpdate.setToolTipText("Check for updates!");
        buttonCheckForUpdate.setIcon(ReferenceGUI.loadIcon);
        buttonCheckForUpdate.setPreferredSize(ReferenceGUI.miscButtonDimension);
        constraints.gridx = 0;
        constraints.gridy = 1;
        about.add(buttonCheckForUpdate, constraints);

        //Button to open a new issue on GitHub
        JButton buttonGitHub = new JButton();
        buttonGitHub.setToolTipText("Visit the GitHub repository!");
        buttonGitHub.setIcon(ReferenceGUI.folderIcon);
        buttonGitHub.setPreferredSize(ReferenceGUI.miscButtonDimension);
        constraints.gridx = 1;
        constraints.gridy = 1;
        about.add(buttonGitHub, constraints);

        //Button to open the invite link to the discord server
        JButton buttonDiscord = new JButton();
        buttonDiscord.setToolTipText("Join the community on Discord!");
        buttonDiscord.setIcon(ReferenceGUI.folderIcon);
        buttonDiscord.setPreferredSize(ReferenceGUI.miscButtonDimension);
        constraints.gridx = 2;
        constraints.gridy = 1;
        about.add(buttonDiscord, constraints);

        return about;
    }

}
