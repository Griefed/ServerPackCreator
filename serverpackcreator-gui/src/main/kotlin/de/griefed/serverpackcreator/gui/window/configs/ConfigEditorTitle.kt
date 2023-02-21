package de.griefed.serverpackcreator.gui.window.configs

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * TODO docs
 */
class ConfigEditorTitle(
    guiProps: GuiProps,
    configsTab: ConfigsTab,
    configEditorPanel: ConfigEditorPanel
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val errorIconLabel = JLabel(guiProps.mediumErrorIcon)
    private val warningIconLabel = JLabel(guiProps.mediumWarningIcon)
    val titleLabel = JLabel(Gui.createserverpack_gui_title_new.toString())
    val closeButton = JButton(guiProps.closeIcon)

    init {
        isOpaque = false
        titleLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
        warningIconLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
        errorIconLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
        warningIconLabel.toolTipText = Gui.configuration_title_warning.toString()
        errorIconLabel.isVisible = false
        warningIconLabel.isVisible = false
        add(errorIconLabel)
        add(warningIconLabel)
        add(titleLabel)
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val currentTab = configsTab.tabs.selectedIndex
                configsTab.tabs.remove(configEditorPanel)

                if (currentTab - 1 > 0) {
                    configsTab.tabs.selectedIndex = currentTab - 1
                }
            }
        })
        closeButton.isVisible = false
        add(closeButton)
    }

    /**
     * TODO docs
     */
    fun setErrorIcon(tooltip: String = Gui.configuration_title_error.toString()) {
        errorIconLabel.isVisible = true
        errorIconLabel.toolTipText = tooltip
    }

    /**
     * TODO docs
     */
    fun setWarningIcon() {
        warningIconLabel.isVisible = true
    }

    /**
     * TODO docs
     */
    fun clearErrorIcon() {
        errorIconLabel.isVisible = false
    }

    /**
     * TODO docs
     */
    fun clearWarningIcon() {
        warningIconLabel.isVisible = false
    }
}