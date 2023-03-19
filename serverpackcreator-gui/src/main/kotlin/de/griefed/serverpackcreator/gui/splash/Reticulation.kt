package de.griefed.serverpackcreator.gui.splash

import de.griefed.serverpackcreator.api.utilities.ReticulatingSplines
import java.awt.Font
import javax.swing.JLabel

class Reticulation(
    width: Int,
    height: Int,
    props: SplashProps,
    private val reticulatingSplines: ReticulatingSplines
) : JLabel() {
    init {
        font = Font("arial", Font.BOLD, 20)
        horizontalAlignment = CENTER
        val y = Math.floorDiv(height, 2) + 20
        setBounds(0, y, width, 40)
        foreground = props.secondaryColour
        reticulate()
    }

    fun reticulate() {
        text = reticulatingSplines.reticulate()
    }
}