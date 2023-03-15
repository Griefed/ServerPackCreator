package de.griefed.serverpackcreator.gui.splash

import java.awt.Color
import java.awt.Font
import javax.swing.JLabel

class By(width: Int, height: Int, colour: Color): JLabel("By Griefed") {
    init {
        font = Font("arial", Font.BOLD, 15)
        setBounds(width - 100,height - 40,width,40)
        foreground = colour
    }
}