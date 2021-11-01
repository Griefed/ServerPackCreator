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
package de.griefed.serverpackcreator.swing;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import de.griefed.serverpackcreator.ApplicationProperties;
import de.griefed.serverpackcreator.swing.utilities.SmartScroller;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Properties;

/**
 * This class creates the tab which display the latest serverpackcreator.log tailer.
 * @author Griefed
 */
public class TabServerPackCreatorLog extends JComponent {

    private final LocalizationManager LOCALIZATIONMANAGER;

    private ApplicationProperties serverPackCreatorProperties;

    private JTextArea textArea;

    /**
     * <strong>Constructor</strong><p>
     * Used for Dependency Injection.<p>
     * Receives an instance of {@link LocalizationManager} or creates one if the received
     * one is null. Required for use of localization.
     * @author Griefed
     * @param injectedLocalizationManager Instance of {@link LocalizationManager} required for localized log messages.
     * @param injectedServerPackCreatorProperties Instance of {@link Properties} required for various different things.
     */
    public TabServerPackCreatorLog(LocalizationManager injectedLocalizationManager, ApplicationProperties injectedServerPackCreatorProperties) {
        if (injectedServerPackCreatorProperties == null) {
            this.serverPackCreatorProperties = new ApplicationProperties();
        } else {
            this.serverPackCreatorProperties = injectedServerPackCreatorProperties;
        }

        if (injectedLocalizationManager == null) {
            this.LOCALIZATIONMANAGER = new LocalizationManager(serverPackCreatorProperties);
        } else {
            this.LOCALIZATIONMANAGER = injectedLocalizationManager;
        }
    }

    /**
     * Create the tab for the serverpackcreator.log tailer in a JScrollPane with an always available vertical scrollbar
     * and a horizontal scrollbar available as needed. Uses Apache commons-io's {@link Tailer} to keep the JTextArea up
     * to date with the latest log entries. Should any line contain "Checking entered configuration." the textarea is
     * cleared.
     * @author Griefed
     * @return JComponent. Returns a JPanel containing a JScrollPane containing the JTextArea with the latest
     * serverpackcreator.log entries.
     */
    JComponent serverPackCreatorLogTab() {
        JComponent serverPackCreatorLogPanel = new JPanel(false);
        serverPackCreatorLogPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.weightx = 1;

        //Log Panel
        textArea = new JTextArea();
        textArea.setEditable(false);

        createTailer();

        JScrollPane scrollPane = new JScrollPane(
                textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        new SmartScroller(scrollPane);

        serverPackCreatorLogPanel.add(scrollPane, constraints);

        return serverPackCreatorLogPanel;
    }

    /**
     *
     * @author Griefed
     */
    private void createTailer() {
        class MyTailerListener extends TailerListenerAdapter {
            public void handle(String line) {
                if (line.contains(LOCALIZATIONMANAGER.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"))) {
                    textArea.setText("");
                }
                if (!line.contains("DEBUG")) {
                    textArea.append(line + "\n");
                }
            }
        }
        TailerListener tailerListener = new MyTailerListener();
        Tailer tailer = new Tailer(new File("./logs/serverpackcreator.log"), tailerListener, 2000);
        Thread thread = new Thread(tailer);
        thread.setDaemon(true);
        thread.start();
    }
}