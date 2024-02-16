/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.gui.window.configs.components.advanced

import Translations
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.ConvenientJTable
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor

/**
 * Custom key-value-pairs for the server pack start scripts. Best used along with custom script templates, as otherwise
 * only the SPC_JAVA_SPC will be used in the variables.txt-file.
 *
 * @author Griefed
 */
class ScriptKVPairs(guiProps: GuiProps, configEditor: ConfigEditor) : ConvenientJTable(
    guiProps,
    Translations.createserverpack_gui_createserverpack_scriptsettings_table_column_variable.toString(),
    Translations.createserverpack_gui_createserverpack_scriptsettings_table_column_value.toString()
) {

    init {
        addTableModelListener { configEditor.validateInputFields() }
        loadData(HashMap())
    }

    /**
     * Clear and load the provided hashmap into the table. They `KEY` is placed into column 1
     * (Placeholder) , the `VALUE` is placed into column 2 (Value).
     *
     * @param data The map containing the data to load into the table.
     * @author Griefed
     */
    override fun loadData(data: HashMap<String, String>, clearDataBeforeLoad: Boolean) {
        if (!data.containsKey("SPC_JAVA_SPC")) {
            data["SPC_JAVA_SPC"] = "java"
        }
        super.loadData(data, clearDataBeforeLoad)
    }
}