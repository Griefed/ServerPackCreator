package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;

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
            appLogger.error(LocalizationManager.getLocalizedString("about.log.error.document"), ex);
        }
        about.add(textPane, constraints);


        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        about.add(new JSeparator(JSeparator.HORIZONTAL), constraints);

        //Buttons
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(15,10,10,10);
        constraints.weightx = 0.1;
        constraints.weighty = 0;
        constraints.ipady = 40;
        constraints.gridwidth = 1;

        //Button to upload log file to pastebin
        JButton buttonCreatePasteBin = new JButton();
        buttonCreatePasteBin.setToolTipText("Open pastebin in your browser to create pastes of logs files or config files.");
        buttonCreatePasteBin.setIcon(ReferenceGUI.pastebinIcon);
        buttonCreatePasteBin.setPreferredSize(ReferenceGUI.miscButtonDimension);
        buttonCreatePasteBin.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://pastebin.com"));
                }
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 0;
        constraints.gridy = 2;
        about.add(buttonCreatePasteBin, constraints);

        //Button to open a new issue on GitHub
        JButton buttonOpenIssue = new JButton();
        buttonOpenIssue.setToolTipText("Create an issue on GitHub.");
        buttonOpenIssue.setIcon(ReferenceGUI.issueIcon);
        buttonOpenIssue.setPreferredSize(ReferenceGUI.miscButtonDimension);
        buttonOpenIssue.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
                }
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 1;
        constraints.gridy = 2;
        about.add(buttonOpenIssue, constraints);

        //Button to open the invite link to the discord server
        JButton buttonDiscord = new JButton();
        buttonDiscord.setToolTipText("Get support and chat on Griefed's Discord server.");
        buttonDiscord.setIcon(ReferenceGUI.prosperIcon);
        buttonDiscord.setPreferredSize(ReferenceGUI.miscButtonDimension);
        buttonDiscord.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://discord.griefed.de"));
                }
            } catch (IOException ex) {
                appLogger.error(LocalizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 2;
        constraints.gridy = 2;
        about.add(buttonDiscord, constraints);

        return about;
    }

}