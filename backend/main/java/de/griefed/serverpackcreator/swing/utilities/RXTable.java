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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

/**
 * Hey, Griefed here. This class is based on another masterpiece by the wonderful Rob Camick. It
 * provides a couple of convenience methods which make the script settings experience a little nice,
 * so I thought I'd make use of it, and expand on it whilst I am already at it.<br>Source at <a
 * href="https://tips4java.wordpress.com/2008/10/20/table-select-all-editor/">Table Select All
 * Editor</a>.
 * <br><br><br>
 * The RXTable provides some extensions to the default JTable
 * <p>
 * 1) Select All editing - when a text related cell is placed in editing mode the text is selected.
 * Controlled by invoking a "setSelectAll..." method.
 * <p>
 * 2) reorderColumns - static convenience method for reodering table columns
 *
 * @author Rob Camick
 * @author Griefed
 */
public class RXTable extends JTable {

  private boolean isSelectAllForMouseEvent = false;
  private boolean isSelectAllForActionEvent = false;
  private boolean isSelectAllForKeyEvent = false;

  /**
   * Create a new Script Settings table, using ServerPackCreators I18n in order to set the column
   * names according to the currently used language.
   *
   * @param i18n ServerPackCreators internationalization for acquiring the column names.
   */
  public RXTable(I18n i18n) {
    this(new DefaultTableModel(new Object[]{i18n.getMessage(
        "createserverpack.gui.createserverpack.scriptsettings.table.column.variable"),
        i18n.getMessage("createserverpack.gui.createserverpack.scriptsettings.table.column.value")},
        100), null, null);
  }

  /**
   * Constructs a <code>RXTable</code> that is initialized with
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
  public RXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
    super(dm, cm, sm);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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

}