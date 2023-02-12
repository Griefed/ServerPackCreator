package de.griefed.serverpackcreator.gui.filebrowser.view.renderer

import java.text.SimpleDateFormat
import javax.swing.table.DefaultTableCellRenderer

class DateRenderer : DefaultTableCellRenderer() {
    private val formatter: SimpleDateFormat = SimpleDateFormat("dd MMM yyyy")
    override fun setValue(value: Any) {
        text = formatter.format(value)
    }
}