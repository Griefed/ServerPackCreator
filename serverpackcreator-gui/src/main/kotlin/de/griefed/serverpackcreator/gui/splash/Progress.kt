package de.griefed.serverpackcreator.gui.splash

import java.awt.Color
import javax.swing.JProgressBar
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.roundToInt

class Progress(width: Int, height: Int, props: SplashProps) : JProgressBar() {
    init {
        val offset = 20f
        val x = (width / 100f * offset).roundToInt()
        val y = Math.floorDiv(height, 2)
        val barWidth = (width / 100f * (100f - offset * 2)).roundToInt()
        setBounds(x, y, barWidth, 20)
        alignmentY = 0.0f
        isBorderPainted = true
        isStringPainted = true
        background = Color.WHITE
        foreground = props.primaryColour
        value = 0
        setUI(
            object : BasicProgressBarUI() {
                override fun getSelectionForeground(): Color {
                    return props.secondaryColour
                }

                // Text-colour when bar is NOT covering the loading-text
                override fun getSelectionBackground(): Color {
                    return props.primaryColour
                }
            }
        )
    }
}