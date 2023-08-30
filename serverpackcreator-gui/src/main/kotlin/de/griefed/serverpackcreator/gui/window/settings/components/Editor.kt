package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.gui.GuiProps
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel
import javax.swing.JScrollPane

abstract class Editor(title: String, guiProps : GuiProps) : JScrollPane(), SettingsEditor {
    val title = EditorTitle(title,guiProps)
    val panel = JPanel(
        MigLayout(
            "left,wrap",
            "[left,::64]5[left]5[left,grow,100:200:]5[left,::64]5[left,::64]5[left,::64]",
            "30"
        )
    )

    init {
        viewport.view = panel
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBar.unitIncrement = 10
    }
}