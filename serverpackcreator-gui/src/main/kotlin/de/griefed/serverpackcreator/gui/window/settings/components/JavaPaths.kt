package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.ConvenientJTable
import java.util.*
import javax.swing.event.TableModelListener
import kotlin.collections.HashMap

class JavaPaths(guiProps: GuiProps, private val apiProperties: ApiProperties, tableModelListener: TableModelListener): ConvenientJTable(guiProps) {

    init {
        addTableModelListener(tableModelListener)
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