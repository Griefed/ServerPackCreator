package de.griefed.serverpackcreator.app.gui.window.settings.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.ConvenientJTable
import java.util.*
import javax.swing.event.TableModelListener

/** The table mapping a Java major version to the JDK to use for it, so a pack can be generated for a Minecraft its default Java cannot run. */
class JavaPaths(guiProps: GuiProps, tableModelListener: TableModelListener) : ConvenientJTable(
    guiProps,
    Translations.settings_global_javapaths_key.toString(),
    Translations.settings_global_javapaths_value.toString()
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

    /** The configured JDK for one Java major version, empty when the table names none. */
    @Suppress("unused")
    fun getJavaPath(javaVersion: Int): Optional<String> {
        return Optional.ofNullable(getData()[javaVersion.toString()])
    }
}