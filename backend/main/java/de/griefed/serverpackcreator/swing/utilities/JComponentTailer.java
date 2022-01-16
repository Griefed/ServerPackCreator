package de.griefed.serverpackcreator.swing.utilities;

import javax.swing.*;
import java.awt.*;

public abstract class JComponentTailer extends JComponent {

    protected JTextArea textArea;

    public JComponent create() {
        JComponent jComponent = new JPanel(false);
        jComponent.setLayout(new GridBagLayout());
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

        jComponent.add(scrollPane, constraints);

        return jComponent;
    }

    protected abstract void createTailer();

}
