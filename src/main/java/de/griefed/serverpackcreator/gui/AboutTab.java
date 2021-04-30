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

class AboutTab extends Component {
    private static final Logger appLogger = LogManager.getLogger(AboutTab.class);

    private final Dimension miscButtonDimension = new Dimension(50,50);
    private final ImageIcon issueIcon           = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/issue.png")));
    private final ImageIcon pastebinIcon        = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/pastebin.png")));
    private final ImageIcon prosperIcon         = new ImageIcon(Objects.requireNonNull(CreateGui.class.getResource("/de/griefed/resources/gui/prosper.png")));

    private LocalizationManager localizationManager;

    public AboutTab(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }
    }

    JComponent aboutTab() {
        JComponent aboutPanel = new JPanel(false);
        aboutPanel.setLayout(new GridBagLayout());
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
        JButton buttonCreatePasteBin = new JButton();
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
        JButton buttonOpenIssue = new JButton();
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
        JButton buttonDiscord = new JButton();
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