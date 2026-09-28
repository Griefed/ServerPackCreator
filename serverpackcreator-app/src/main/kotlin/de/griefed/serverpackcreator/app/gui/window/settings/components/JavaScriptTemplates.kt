package de.griefed.serverpackcreator.app.gui.window.settings.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.ConvenientJTable
import java.util.*
import javax.swing.event.TableModelListener

/** The table mapping a Java-script type to the template it is generated from — the Java-args counterpart of `ScriptTemplates`. */
class JavaScriptTemplates(guiProps: GuiProps, tableModelListener: TableModelListener) : ConvenientJTable(
    guiProps,
    Translations.settings_global_javascripts_key.toString(),
    Translations.settings_global_javascripts_value.toString()
) {

    init {
        addTableModelListener(tableModelListener)
        columnModel.getColumn(0).minWidth = 50
        columnModel.getColumn(0).width = 150
        columnModel.getColumn(0).maxWidth = 200
    }

    override fun loadData(data: HashMap<String, String>, clearDataBeforeLoad: Boolean) {
        if (data.isEmpty()) {
            data["placeholder"] = "/path/to/script/java/template.file"
        }
        super.loadData(data, clearDataBeforeLoad)
    }

    /** The configured template for one type, empty when the table names none. */
    @Suppress("unused")
    fun getTemplatePath(type: String): Optional<String> {
        return Optional.ofNullable(getData()[type])
    }
}