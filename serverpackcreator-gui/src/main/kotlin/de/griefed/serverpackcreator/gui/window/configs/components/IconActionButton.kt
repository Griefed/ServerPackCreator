package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.event.ActionListener
import javax.swing.ImageIcon
import javax.swing.JButton

/**
 * TODO docs
 */
class IconActionButton(icon: ImageIcon, tooltip: String, action: ActionListener) : JButton(icon) {
    init {
        toolTipText = tooltip
        addActionListener(action)
    }
}