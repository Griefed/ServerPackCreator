package de.griefed.serverpackcreator.gui.splash

import java.awt.Color
import java.awt.Font
import javax.swing.JLabel

class Version(width: Int, height: Int,version: String, color: Color): JLabel(version) {
    init {
        font = Font("arial", Font.BOLD, 15)
        setBounds(15,height - 40,width,40)
        foreground = color
    }
}