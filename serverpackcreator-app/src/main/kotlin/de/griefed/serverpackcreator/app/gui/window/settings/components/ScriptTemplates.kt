package de.griefed.serverpackcreator.app.gui.window.settings.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.ConvenientJTable
import java.util.*
import javax.swing.event.TableModelListener

class ScriptTemplates(guiProps: GuiProps, tableModelListener: TableModelListener) : ConvenientJTable(
    guiProps,
    Translations.settings_global_scripts_key.toString(),
    Translations.settings_global_scripts_value.toString()
) {

    init {
        addTableModelListener(tableModelListener)
        columnModel.getColumn(0).minWidth = 50
        columnModel.getColumn(0).width = 150
        columnModel.getColumn(0).maxWidth = 200
    }

    override fun loadData(data: HashMap<String, String>, clearDataBeforeLoad: Boolean) {
        if (data.isEmpty()) {
            data["placeholder"] = "/path/to/script/template.file"
        }
        super.loadData(data, clearDataBeforeLoad)
    }

    @Suppress("unused")
    fun getTemplatePath(type: String): Optional<String> {
        return Optional.ofNullable(getData()[type])
    }
}