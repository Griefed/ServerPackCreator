package de.griefed.serverpackcreator.gui.components

import de.griefed.serverpackcreator.gui.GuiProps
import net.java.balloontip.BalloonTip
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.Timer

/**
 * Regular JButton but with a [BalloonTip] instead of the regular old Java-Style tooltip.
 */
open class BalloonTipButton(text: String?, icon: Icon, toolTip: String, guiProps: GuiProps) : JButton(text, icon) {
    private val toolTipLabel = JLabel(toolTip)

    constructor(
        text: String?,
        icon: Icon,
        toolTip: String,
        guiProps: GuiProps,
        actionListener: ActionListener) : this(
        text,
        icon,
        toolTip,
        guiProps
    ) {
        addActionListener(actionListener)
    }

    init {
        multiClickThreshhold = 1000
        val balloonTip = BalloonTip(
            this,
            toolTipLabel,
            guiProps.balloonStyle,
            false
        )
        balloonTip.isVisible = false
        val timer = Timer(1000) {
            balloonTip.style = guiProps.balloonStyle
            balloonTip.isVisible = true
        }
        timer.stop()
        timer.isRepeats = false
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {}

            override fun mousePressed(e: MouseEvent?) {}

            override fun mouseReleased(e: MouseEvent?) {}

            override fun mouseEntered(e: MouseEvent?) {
                timer.restart()
            }

            override fun mouseExited(e: MouseEvent?) {
                timer.stop()
                balloonTip.isVisible = false
            }
        })
    }
}