package de.griefed.serverpackcreator.gui.window.configs.components

import de.griefed.serverpackcreator.gui.GuiProps
import javax.swing.JLabel

/**
 * TODO docs
 */
class StatusIcon(private val guiProps: GuiProps, private val infoToolTip: String) : JLabel() {
    init {
        icon = guiProps.infoIcon
    }

    /**
     * TODO docs
     */
    fun error(tooltip: String) {
        icon = guiProps.errorIcon
        toolTipText = tooltip
    }

    /**
     * TODO docs
     */
    fun info() {
        icon = guiProps.infoIcon
        toolTipText = infoToolTip
    }

    /**
     * TODO docs
     */
    fun warning(tooltip: String) {
        icon = guiProps.warningIcon
        toolTipText = tooltip
    }
}