package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

/**
 * TODO docs
 */
class ActionComboBox(actionListener: ActionListener) : JComboBox<String>() {
    constructor(
        defaultComboBoxModel: DefaultComboBoxModel<String>,
        actionListener: ActionListener
    ) : this(
        actionListener
    ) {
        model = defaultComboBoxModel
    }

    init {
        addActionListener(actionListener)
    }
}