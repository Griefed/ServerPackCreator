/* Copyright (C) 2022  Griefed
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
package de.griefed.serverpackcreator.swing.utilities;

import de.griefed.serverpackcreator.i18n.I18n;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hey, Griefed here. This class is based on another masterpiece by the wonderful Rob Camick. It
 * provides a couple of convenience methods which make the script settings experience a little nice,
 * so I thought I'd make use of it, and expand on it whilst I am already at it.<br>Source at <a
 * href="https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/">Table Select All
 * Editor</a>.
 * <br><br><br>
 * The ScriptSettings provides some extensions to the default JTable
 * <p>
 * 1) Select All editing - when a text related cell is placed in editing mode the text is selected.
 * Controlled by invoking a "setSelectAll..." method.
 * <p>
 * 2) reorderColumns - static convenience method for reodering table columns
 *
 * @author Rob Camick
 * @author Griefed
 */
public class ScriptSettings extends JTable {

  private static final Logger LOG = LogManager.getLogger(ScriptSettings.class);
  private boolean isSelectAllForMouseEvent = false;
  private boolean isSelectAllForActionEvent = false;
  private boolean isSelectAllForKeyEvent = false;

  /**
   * Create a new Script Settings table, using ServerPackCreators I18n in order to set the column
   * names according to the currently used language.
   *
   * @param i18n ServerPackCreators internationalization for acquiring the column names.
   */
  public ScriptSettings(I18n i18n) {
    this(new DefaultTableModel(
            new Object[]{
                i18n.getMessage(
                    "createserverpack.gui.createserverpack.scriptsettings.table.column.variable"),
                i18n.getMessage(
                    "createserverpack.gui.createserverpack.scriptsettings.table.column.value"),
                i18n.getMessage(
                    "createserverpack.gui.createserverpack.scriptsettings.table.column.clear")},
            100),
        null,
        null);
  }

  /**
   * Constructs a <code>ScriptSettings</code> that is initialized with
   * <code>dm</code> as the data model, <code>cm</code> as the
   * column model, and <code>sm</code> as the selection model. If any of the parameters are
   * <code>null</code> this method will initialize the table with the corresponding default model.
   * The <code>autoCreateColumnsFromModel</code> flag is set to false if <code>cm</code> is
   * non-null, otherwise it is set to true and the column model is populated with suitable
   * <code>TableColumns</code> for the columns in <code>dm</code>.
   *
   * @param dm the data model for the table
   * @param cm the column model for the table
   * @param sm the row selection model for the table
   */
  public ScriptSettings(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
    super(dm, cm, sm);
    try {
      Action delete = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          JTable table = (JTable) e.getSource();
          int modelRow = Integer.parseInt(e.getActionCommand());
          ((DefaultTableModel) table.getModel()).removeRow(modelRow);
        }
      };
      new ButtonColumn(this, delete, 2);
    } catch (IOException ex) {
      LOG.error("Couldn't create button column.", ex);
    }
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    setSelectAllForEdit(true);
    getColumnModel().getColumn(2).setMinWidth(60);
    getColumnModel().getColumn(2).setMaxWidth(60);
    getColumnModel().getColumn(2).setWidth(60);
    getColumnModel().getColumn(2).setResizable(false);
  }

  /**
   * Override to provide Select All editing functionality
   */
  public boolean editCellAt(int row, int column, EventObject e) {
    boolean result = super.editCellAt(row, column, e);

    if (isSelectAllForMouseEvent
        || isSelectAllForActionEvent
        || isSelectAllForKeyEvent) {
      selectAll(e);
    }

    return result;
  }

  /**
   * Select the text when editing on a text related cell is started
   */
  private void selectAll(EventObject e) {
    final Component editor = getEditorComponent();

    if (!(editor instanceof JTextComponent)) {
      return;
    }

    if (e == null) {
      ((JTextComponent) editor).selectAll();
      return;
    }

    //  Typing in the cell was used to activate the editor

    if (e instanceof KeyEvent && isSelectAllForKeyEvent) {
      ((JTextComponent) editor).selectAll();
      return;
    }

    //  F2 was used to activate the editor

    if (e instanceof ActionEvent && isSelectAllForActionEvent) {
      ((JTextComponent) editor).selectAll();
      return;
    }

    //  A mouse click was used to activate the editor.
    //  Generally this is a double click and the second mouse click is
    //  passed to the editor which would remove the text selection unless
    //  we use the invokeLater()

    if (e instanceof MouseEvent && isSelectAllForMouseEvent) {
      SwingUtilities.invokeLater(() -> ((JTextComponent) editor).selectAll());
    }
  }

  /**
   * Sets the Select All property for all event types
   *
   * @param isSelectAllForEdit Whether to select all for editing
   */
  public void setSelectAllForEdit(boolean isSelectAllForEdit) {
    setSelectAllForMouseEvent(isSelectAllForEdit);
    setSelectAllForActionEvent(isSelectAllForEdit);
    setSelectAllForKeyEvent(isSelectAllForEdit);
  }

  /**
   * Set the Select All property when editing is invoked by the mouse
   *
   * @param isSelectAllForMouseEvent Whether to select all for editing
   */
  public void setSelectAllForMouseEvent(boolean isSelectAllForMouseEvent) {
    this.isSelectAllForMouseEvent = isSelectAllForMouseEvent;
  }

  /**
   * Set the Select All property when editing is invoked by the "F2" key
   *
   * @param isSelectAllForActionEvent Whether to select all for editing
   */
  public void setSelectAllForActionEvent(boolean isSelectAllForActionEvent) {
    this.isSelectAllForActionEvent = isSelectAllForActionEvent;
  }

  /**
   * Set the Select All property when editing is invoked by typing directly into the cell
   *
   * @param isSelectAllForKeyEvent Whether to select all for editing
   */
  public void setSelectAllForKeyEvent(boolean isSelectAllForKeyEvent) {
    this.isSelectAllForKeyEvent = isSelectAllForKeyEvent;
  }

  /**
   * Clear and load the provided hashmap into the table. They <code>KEY</code> is placed into column
   * 1 (Placeholder) , the <code>VALUE</code> is placed into column 2 (Value).
   *
   * @param data The map containing the data to load into the table.
   * @author Griefed
   */
  public void loadData(HashMap<String, String> data) {
    for (int row = 0; row < getModel().getRowCount(); row++) {
      getModel().setValueAt("", row, 0);
      getModel().setValueAt("", row, 1);
    }
    int row = 0;
    for (Map.Entry<String, String> entry : data.entrySet()) {
      getModel().setValueAt(entry.getKey(), row, 0);
      getModel().setValueAt(entry.getValue(), row, 1);
      row += 1;
    }
  }

  /**
   * Get the data from the table as a map. Column 1 (Placeholder) will be mapped to the maps
   * <code>KEY</code>, column 2 (Value) will be mapped to the maps <code>VALUE</code>. Rows are
   * ignored of they do not contain values for both columns.
   *
   * @return A map containing the data of the table.
   * @author Griefed
   */
  public HashMap<String, String> getData() {
    HashMap<String, String> data = new HashMap<>();
    for (int row = 0; row < getModel().getRowCount(); row++) {
      if (!getModel().getValueAt(row, 0).toString().isEmpty()
          && !getModel().getValueAt(row, 1).toString().isEmpty()) {

        data.put(
            getModel().getValueAt(row, 0).toString(),
            getModel().getValueAt(row, 1).toString()
        );
      }
    }
    return data;
  }

  @SuppressWarnings("InnerClassMayBeStatic")
  public class ButtonColumn extends AbstractCellEditor
      implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {

    private final JTable table;
    private final Action action;
    private final Border originalBorder;
    private final JButton renderButton;
    private final JButton editButton;
    private Border focusBorder;
    private Object editorValue;
    private boolean isButtonColumnEditor;

    /**
     * Create the ButtonColumn to be used as a renderer and editor. The renderer and editor will
     * automatically be installed on the TableColumn of the specified column.
     *
     * @param table  the table containing the button renderer/editor
     * @param action the Action to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     * @throws IOException if the icon for the delete button can not be initialized.
     */
    public ButtonColumn(JTable table, Action action, int column) throws IOException {
      this.table = table;
      this.action = action;

      Icon delete = new ImageIcon(
          ImageIO.read(
                  Objects.requireNonNull(
                      ScriptSettings.class.getResource("/de/griefed/resources/gui/delete.png")))
              .getScaledInstance(32, 32, Image.SCALE_SMOOTH));
      renderButton = new JButton(delete);
      editButton = new JButton(delete);
      editButton.setFocusPainted(false);
      editButton.addActionListener(this);
      originalBorder = editButton.getBorder();
      setFocusBorder(new LineBorder(Color.BLUE));

      TableColumnModel columnModel = table.getColumnModel();
      columnModel.getColumn(column).setCellRenderer(this);
      columnModel.getColumn(column).setCellEditor(this);
      table.addMouseListener(this);
    }

    /**
     * The foreground color of the button when the cell has focus
     *
     * @param focusBorder the foreground color
     */
    public void setFocusBorder(Border focusBorder) {
      this.focusBorder = focusBorder;
      editButton.setBorder(focusBorder);
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
      this.editorValue = value;
      return editButton;
    }

    @Override
    public Object getCellEditorValue() {
      return editorValue;
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (isSelected) {
        renderButton.setForeground(table.getSelectionForeground());
        renderButton.setBackground(table.getSelectionBackground());
      } else {
        renderButton.setForeground(table.getForeground());
        renderButton.setBackground(UIManager.getColor("Button.background"));
      }

      if (hasFocus) {
        renderButton.setBorder(focusBorder);
      } else {
        renderButton.setBorder(originalBorder);
      }

      return renderButton;
    }

    /**
     * The button has been pressed. Stop editing and invoke the custom Action
     */
    public void actionPerformed(ActionEvent e) {
      int row = table.convertRowIndexToModel(table.getEditingRow());
      fireEditingStopped();

      ActionEvent event = new ActionEvent(
          table,
          ActionEvent.ACTION_PERFORMED,
          "" + row);
      action.actionPerformed(event);
    }

    /**
     * When the mouse is pressed the editor is invoked. If you then drag the mouse to another cell
     * before releasing it, the editor is still active. Make sure editing is stopped when the mouse
     * is released.
     */
    public void mousePressed(MouseEvent e) {
      if (table.isEditing()
          && table.getCellEditor() == this) {
        isButtonColumnEditor = true;
      }
    }

    public void mouseReleased(MouseEvent e) {
      if (isButtonColumnEditor
          && table.isEditing()) {
        table.getCellEditor().stopCellEditing();
      }

      isButtonColumnEditor = false;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
  }
}