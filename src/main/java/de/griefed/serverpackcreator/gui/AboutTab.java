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

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * This class creates the tab which displays the About-tab, with the about text, the list of contributors, buttons for
 * opening PasteBin in your browser, opening ServerPackCreator's issue page on GitHub and for opening the invite link
 * to Griefed#s discord server in your browser.
 */
public class AboutTab extends Component {
    private static final Logger appLogger = LogManager.getLogger(AboutTab.class);

    private LocalizationManager localizationManager;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     */
    public AboutTab(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }
    }

    private final Dimension miscButtonDimension = new Dimension(50,50);
    private final ImageIcon issueIcon           = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/issue.png")));
    private final ImageIcon pastebinIcon        = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/pastebin.png")));
    private final ImageIcon prosperIcon         = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/prosper.png")));

    private JComponent aboutPanel;
    private GridBagConstraints constraints;
    private JTextPane textPane;
    private SimpleAttributeSet attributeSet;
    private StyledDocument document;
    private JButton buttonCreatePasteBin;
    private JButton buttonOpenIssue;
    private JButton buttonDiscord;

    /**
     * Create the tab for the About-page of ServerpackCreator. This tab displays the about text, the list of contributors
     * to ServerPackCreator and offers three buttons to the user. Left to right: Open PasteBin in the user's browser to
     * create pastes of the the log files or configuration files or whatever they wish. Open ServerPackCreator's issues
     * page on GitHub in case the user wants to report an issue. Open the invite link to Griefed's discord server in the
     * users browser.
     * @return JComponent. Returns a JPanel containing a JTextPane with the about text in a styled document, as well as
     * the three aforementioned buttons.
     */
    JComponent aboutTab() {
        aboutPanel = new JPanel(false);
        aboutPanel.setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;

        //About Panel
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setMinimumSize(new Dimension(getMaximumSize().width,520));
        textPane.setPreferredSize(new Dimension(getMaximumSize().width,520));
        textPane.setMaximumSize(new Dimension(getMaximumSize().width,520));

        attributeSet = new SimpleAttributeSet();
        StyleConstants.setBold(attributeSet, true);
        StyleConstants.setFontSize(attributeSet, 14);
        textPane.setCharacterAttributes(attributeSet, true);

        document = textPane.getStyledDocument();
        StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
        document.setParagraphAttributes(0, document.getLength(), attributeSet, false);

        try {
            document.insertString(
                    document.getLength(),
                    localizationManager.getLocalizedString("createserverpack.gui.about.text"),
                    attributeSet
            ); } catch (BadLocationException ex) {
            appLogger.error(localizationManager.getLocalizedString("about.log.error.document"), ex);
        }
        aboutPanel.add(textPane, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        aboutPanel.add(new JSeparator(JSeparator.HORIZONTAL), constraints);

        //Buttons
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(15,10,10,10);
        constraints.weightx = 0.1;
        constraints.weighty = 0;
        constraints.ipady = 40;
        constraints.gridwidth = 1;

        //Button to upload log file to pastebin
        buttonCreatePasteBin = new JButton();
        buttonCreatePasteBin.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.about.pastebin"));
        buttonCreatePasteBin.setIcon(pastebinIcon);
        buttonCreatePasteBin.setPreferredSize(miscButtonDimension);
        buttonCreatePasteBin.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://pastebin.com"));
                }
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 0;
        constraints.gridy = 2;
        aboutPanel.add(buttonCreatePasteBin, constraints);

        //Button to open a new issue on GitHub
        buttonOpenIssue = new JButton();
        buttonOpenIssue.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.about.issue"));
        buttonOpenIssue.setIcon(issueIcon);
        buttonOpenIssue.setPreferredSize(miscButtonDimension);
        buttonOpenIssue.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://github.com/Griefed/ServerPackCreator/issues"));
                }
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 1;
        constraints.gridy = 2;
        aboutPanel.add(buttonOpenIssue, constraints);

        //Button to open the invite link to the discord server
        buttonDiscord = new JButton();
        buttonDiscord.setToolTipText(localizationManager.getLocalizedString("createserverpack.gui.about.discord"));
        buttonDiscord.setIcon(prosperIcon);
        buttonDiscord.setPreferredSize(miscButtonDimension);
        buttonDiscord.addActionListener(e -> {

            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("https://discord.griefed.de"));
                }
            } catch (IOException ex) {
                appLogger.error(localizationManager.getLocalizedString("about.log.error.browser"), ex);
            }

        });
        constraints.gridx = 2;
        constraints.gridy = 2;
        aboutPanel.add(buttonDiscord, constraints);

        return aboutPanel;
    }

}