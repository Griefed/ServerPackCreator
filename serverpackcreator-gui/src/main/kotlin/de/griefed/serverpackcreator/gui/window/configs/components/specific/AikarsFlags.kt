package de.griefed.serverpackcreator.gui.window.configs.components.specific

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditorPanel
import de.griefed.serverpackcreator.gui.window.configs.components.general.CompoundIcon
import de.griefed.serverpackcreator.gui.window.configs.components.general.TextIcon
import net.java.balloontip.BalloonTip
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JLabel

class AikarsFlags(configEditorPanel: ConfigEditorPanel, guiProps: GuiProps) : JButton() {
    init {
        addActionListener { configEditorPanel.setAikarsFlagsAsJavaArguments() }
        toolTipText = null
        val balloonTip = BalloonTip(
            this,
            JLabel(Gui.createserverpack_gui_createserverpack_javaargs_aikar_tooltip.toString()),
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
        val parts = Gui.createserverpack_gui_createserverpack_javaargs_aikar.toString().split(" ")
        val flags = mutableListOf<TextIcon>()
        for (part in parts) {
            flags.add(TextIcon(this, part))
        }
        icon = CompoundIcon(
            flags.toTypedArray(),
            5,
            CompoundIcon.Axis.Y_AXIS
        )
    }
}