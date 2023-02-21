package de.griefed.serverpackcreator.gui.window.configs.components

import Gui
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

/**
 * TODO docs
 */
class QuickSelect(content: List<String>, actionListener: ActionListener) : JComboBox<String>() {
    init {
        val choose = DefaultComboBoxModel(arrayOf(Gui.createserverpack_gui_quickselect_choose.toString()))
        choose.addAll(content)
        model = choose
        addActionListener(actionListener)
    }
}