package de.griefed.serverpackcreator.gui.window.main.control

import java.awt.Color
import java.awt.Graphics
import javax.swing.JLabel
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

/**
 * TODO docs
 */
class StatusLabel(text: String, private val transparency: Int = 255) : JLabel(text) {
    init {
        updateColour()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        updateColour()
    }

    /**
     * TODO docs
     */
    private fun updateColour() {
        val color = UIManager.get("Label.foreground") as ColorUIResource
        foreground = Color(color.red, color.green, color.blue, transparency)
    }
}