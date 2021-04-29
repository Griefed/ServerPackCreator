package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.File;

class ServerPackCreatorLogTab extends Component {

    private LocalizationManager localizationManager;

    public ServerPackCreatorLogTab(LocalizationManager injectedLocalizationManager) {
        if (injectedLocalizationManager == null) {
            this.localizationManager = new LocalizationManager();
        } else {
            this.localizationManager = injectedLocalizationManager;
        }
    }

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
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        Tailer.create(new File("./logs/serverpackcreator.log"), new TailerListenerAdapter() {
            public void handle(String line) {
                synchronized (this) {
                    if (line.contains(localizationManager.getLocalizedString("createserverpack.log.info.buttoncreateserverpack.start"))) {
                        textArea.setText("");
                    }
                    textArea.append(line + "\n");
                }
            }
        }, 2000, false);

        JScrollPane scrollPane = new JScrollPane(
                textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        serverPackCreatorLogPanel.add(scrollPane, constraints);

        return serverPackCreatorLogPanel;
    }
}