package de.griefed.serverpackcreator.gui.components

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

open class TabTitle(guiProps: GuiProps) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val errorIconLabel = JLabel(guiProps.smallErrorIcon)
    private val warningIconLabel = JLabel(guiProps.smallWarningIcon)
    private val titleLabel = JLabel(Gui.createserverpack_gui_title_new.toString())

    var hasUnsavedChanges: Boolean = false
        get() {
            return warningIconLabel.isVisible
        }
        private set

    var title: String
        get() {
            return titleLabel.text
        }
        set(value) {
            titleLabel.text = value
        }

    constructor(guiProps: GuiProps, name: String) : this(guiProps) {
        title = name
    }

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
    }


    /**
     * Show the error icon, indicating the configuration has errors.
     *
     * @author Griefed
     */
    fun setAndShowErrorIcon(tooltip: String = Gui.configuration_title_error.toString()) {
        errorIconLabel.isVisible = true
        errorIconLabel.toolTipText = tooltip
    }

    /**
     * Show the warning icon, indicating the configuration has unsaved changes.
     *
     * @author Griefed
     */
    fun showWarningIcon() {
        warningIconLabel.isVisible = true
    }

    /**
     * Hide the error icon, indicating the configuration is free from errors.
     *
     * @author Griefed
     */
    fun hideErrorIcon() {
        errorIconLabel.isVisible = false
    }

    /**
     * Hide the warning icon, indicating the configuration has no unsaved changes.
     *
     * @author Griefed
     */
    fun hideWarningIcon() {
        warningIconLabel.isVisible = false
    }
}