package de.griefed.serverpackcreator.gui.window.icons

import com.formdev.flatlaf.icons.FlatOptionPaneInformationIcon
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import kotlin.math.roundToInt

/**
 * TODO docs
 */
class InfoIcon(private val zoom: Float = 0.7f) : FlatOptionPaneInformationIcon() {
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(x, y)
            g2.scale(zoom.toDouble(), zoom.toDouble())
            super.paintIcon(c, g2, 0, 0)
        } finally {
            g2.dispose()
        }
    }

    override fun getIconWidth(): Int {
        return (super.getIconWidth() * zoom).roundToInt()
    }

    override fun getIconHeight(): Int {
        return (super.getIconHeight() * zoom).roundToInt()
    }
}