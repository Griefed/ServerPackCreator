package de.griefed.serverpackcreator.swing.utilities;

import de.griefed.serverpackcreator.utilities.misc.Generated;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@Generated
public abstract class JComponentTailer extends JPanel {

  protected JTextArea textArea;

  public JComponentTailer(String tooltip) {
    setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();

    constraints.anchor = GridBagConstraints.CENTER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weighty = 1;
    constraints.weightx = 1;

    // Log Panel
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setFont(new Font("Noto Sans Display Regular", Font.PLAIN, 15));

    if (tooltip != null) {
      textArea.setToolTipText(tooltip);
    }

    JScrollPane scrollPane =
        new JScrollPane(
            textArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    new SmartScroller(scrollPane, SmartScroller.VERTICAL, SmartScroller.END);

    add(scrollPane, constraints);
  }

  protected abstract void createTailer();
}
