package de.griefed.serverpackcreator.gui;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.File;


public class ModloaderInstallerLog extends Component {

    private volatile StringBuffer stringBuffer = new StringBuffer(10000);

    JComponent modloaderInstallerLog() {
        JComponent modloaderInstallerLog = new JPanel(false);
        modloaderInstallerLog.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.weighty = 0.9;

        //Log Panel
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        Tailer.create(new File("./logs/modloader_installer.log"), new TailerListenerAdapter() {
            public void handle(String line) {
                synchronized (this) {
                    if (stringBuffer.length() + line.length() > 5000) {
                        stringBuffer = new StringBuffer();
                    }
                    textArea.append(line + "\n");
                }
            }
        }, 2000, false);


        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setMinimumSize(new Dimension(775,getMaximumSize().height));
        scrollPane.setPreferredSize(new Dimension(775,getMaximumSize().height));
        scrollPane.setMaximumSize(new Dimension(775,getMaximumSize().height));

        modloaderInstallerLog.add(scrollPane, constraints);

        return modloaderInstallerLog;
    }
}