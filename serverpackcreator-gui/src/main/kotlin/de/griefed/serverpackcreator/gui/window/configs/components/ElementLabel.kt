package de.griefed.serverpackcreator.gui.window.configs.components

import java.awt.font.TextAttribute
import javax.swing.JLabel

/**
 * TODO docs
 */
class ElementLabel(text: String, private var size: Int = 0) : JLabel(text) {
    init {
        if (size == 0) {
            size = font.size
        }
        font = font.deriveFont(
            mutableMapOf(
                Pair(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD),
                Pair(TextAttribute.SIZE, size)
            )
        )
    }
}