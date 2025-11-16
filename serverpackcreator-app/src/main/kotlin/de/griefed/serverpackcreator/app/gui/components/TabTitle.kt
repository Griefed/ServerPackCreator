/* Copyright (C) 2025 Griefed
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * The full license can be found at https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE
 */
package de.griefed.serverpackcreator.app.gui.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Griefed
 */
open class TabTitle(guiProps: GuiProps) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val errorIconLabel = JLabel(guiProps.smallErrorIcon)
    private val warningIconLabel = JLabel(guiProps.smallWarningIcon)
    private val titleLabel = JLabel(Translations.createserverpack_gui_title_new.toString())

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
        warningIconLabel.toolTipText = Translations.configuration_title_warning.toString()
        errorIconLabel.isVisible = false
        warningIconLabel.isVisible = false
        this.add(errorIconLabel)
        this.add(warningIconLabel)
        this.add(titleLabel)
    }


    /**
     * Show the error icon, indicating the configuration has errors.
     *
     * @author Griefed
     */
    fun setAndShowErrorIcon(tooltip: String = Translations.configuration_title_error.toString()) {
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