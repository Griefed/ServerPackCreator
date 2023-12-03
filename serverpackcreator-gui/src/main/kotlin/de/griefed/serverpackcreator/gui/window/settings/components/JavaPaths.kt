package de.griefed.serverpackcreator.gui.window.settings.components

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.ConvenientJTable
import java.util.*
import javax.swing.event.TableModelListener

class JavaPaths(guiProps: GuiProps, tableModelListener: TableModelListener) : ConvenientJTable(
    guiProps,
    Gui.settings_global_javapaths_key.toString(),
    Gui.settings_global_javapaths_value.toString()
) {

    init {
        addTableModelListener(tableModelListener)
        columnModel.getColumn(0).minWidth = 50
        columnModel.getColumn(0).width = 150
        columnModel.getColumn(0).maxWidth = 200
    }

    override fun loadData(data: HashMap<String, String>, clearDataBeforeLoad: Boolean) {
        if (data.isEmpty()) {
            data["placeholder"] = "/path/to/java/binary/exe"
        }
        super.loadData(data, clearDataBeforeLoad)
    }

    fun getJavaPath(javaVersion: Int): Optional<String> {
        return Optional.ofNullable(getData()[javaVersion.toString()])
    }
}