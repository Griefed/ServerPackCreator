package de.griefed.serverpackcreator.gui.window.configs.components

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.utilities.getScaledInstance
import net.java.balloontip.BalloonTip
import java.awt.Image
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.ImageIcon
import javax.swing.JLabel

/**
 * TODO docs
 */
class IconPreview(guiProps: GuiProps): JLabel(guiProps.serverIcon) {
    private val bigPreview = JLabel(scaled(guiProps.serverIcon))
    private val balloonTip: BalloonTip
    init {
        balloonTip = BalloonTip(
            this,
            bigPreview,
            guiProps.balloonStyle,
            false
        )
        balloonTip.isVisible = false
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mousePressed(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseReleased(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseEntered(e: MouseEvent?) {
                balloonTip.style = guiProps.balloonStyle
                balloonTip.isVisible = true
            }

            override fun mouseExited(e: MouseEvent?) {
                balloonTip.isVisible = false
            }

        })
    }

    /**
     * TODO docs
     */
    private fun scaled(icon: ImageIcon, width: Int = 128, height: Int = 128): ImageIcon {
        return icon.getScaledInstance(width,height,Image.SCALE_SMOOTH)
    }

    /**
     * TODO docs
     */
    fun updateIcon(newIcon: ImageIcon) {
        icon = scaled(newIcon,32,32)
        bigPreview.icon = scaled(newIcon)
    }
}