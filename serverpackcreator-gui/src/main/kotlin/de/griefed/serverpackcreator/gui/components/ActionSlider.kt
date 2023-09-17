package de.griefed.serverpackcreator.gui.components

import javax.swing.JSlider
import javax.swing.event.ChangeListener

class ActionSlider(min: Int, max: Int, value: Int, listener: ChangeListener) : JSlider(min, max, value) {
    init {
        addChangeListener(listener)
    }
}