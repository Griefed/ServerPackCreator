package de.griefed.serverpackcreator.swing.utilities;

import de.griefed.serverpackcreator.utilities.misc.Generated;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@Generated
public abstract class JComponentTailer extends JComponent {

  protected JTextArea textArea;

  public JComponent create(String tooltip) {
    JComponent jComponent = new JPanel(false);
    jComponent.setLayout(new GridBagLayout());
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

    if (tooltip != null) {
      textArea.setToolTipText(tooltip);
    }

    JScrollPane scrollPane =
        new JScrollPane(
            textArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    new SmartScroller(scrollPane, SmartScroller.VERTICAL, SmartScroller.END);

    jComponent.add(scrollPane, constraints);

    createTailer();

    return jComponent;
  }

  protected abstract void createTailer();
}
