package de.griefed.serverpackcreator.gui;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
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

        //About Panel
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setMinimumSize(new Dimension(getMaximumSize().width,520));
        textPane.setPreferredSize(new Dimension(getMaximumSize().width,520));
        textPane.setMaximumSize(new Dimension(getMaximumSize().width,520));

        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setBold(attributeSet, true);
        StyleConstants.setFontSize(attributeSet, 14);
        textPane.setCharacterAttributes(attributeSet, true);

        StyledDocument document = textPane.getStyledDocument();
        StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
        document.setParagraphAttributes(0, document.getLength(), attributeSet, false);

        try {
        document.insertString(
                document.getLength(),
                "ServerPackCreator was made as a training ground for Java \n" +
                "and because I wanted an easier way to update/make server packs for updates to my modpack.\n" +
                "It has grown substantially since the first release, which was when it was CLI only.\n" +
                "Now it provides a GUI as well!\n" +
                "The fact that this is, and always will be, a playground for me, in Java, still remains though. As with every application, bugs are bound to appear, and I will try to fix them whenever I can.\n" +
                "I am working on this program in my spare time, please keep that in mind when reporting issues or requesting new features to be added.\n" +
                "\n" +
                "This project would not be where it is today without the help from my contributors:\n" +
                "\n" +
                "Whitebear60\n",
                attributeSet
        ); } catch (BadLocationException ex) {
            appLogger.error("Error inserting string into document.", ex);
        }

        about.add(textPane, constraints);

        //Buttons
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(15,10,10,10);
        constraints.weightx = 0.1;
        constraints.weighty = 0;
        constraints.ipady = 40;
        constraints.gridwidth = 1;

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
