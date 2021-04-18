package de.griefed.serverpackcreator.gui;

import de.griefed.serverpackcreator.i18n.LocalizationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerPackCreatorLog extends Component {
    private static final Logger appLogger = LogManager.getLogger(ServerPackCreatorLog.class);


    JComponent serverPackCreatorLog() {
        JComponent serverPackCreatorLog = new JPanel(false);
        serverPackCreatorLog.setLayout(new GridBagLayout());
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
        /* TODO: Find more efficient way of displaying logs in UI. This way bloats memory usage by ungodly amounts.
        final ScheduledExecutorService readLogExecutor = Executors.newSingleThreadScheduledExecutor();
        readLogExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try { textArea.read(new FileReader("./logs/serverpackcreator.log"),null); }
                catch (IOException ex) { appLogger.error(LocalizationManager.getLocalizedString("serverpackcreatorlog.log.error"), ex); }
            }
        }, 2, 1, TimeUnit.SECONDS);

         */



        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setMinimumSize(new Dimension(775,getMaximumSize().height));
        scrollPane.setPreferredSize(new Dimension(775,getMaximumSize().height));
        scrollPane.setMaximumSize(new Dimension(775,getMaximumSize().height));
        serverPackCreatorLog.add(scrollPane, constraints);

        return serverPackCreatorLog;
    }
}
