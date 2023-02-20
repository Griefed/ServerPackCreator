package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.event.ActionListener
import javax.swing.JCheckBox

/**
 * TODO docs
 */
class ActionCheckBox(title: String, actionListener: ActionListener): JCheckBox(title) {
    init {
        addActionListener(actionListener)
    }
}