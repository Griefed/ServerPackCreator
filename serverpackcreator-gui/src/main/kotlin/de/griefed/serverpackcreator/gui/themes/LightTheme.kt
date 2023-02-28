/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.gui.themes

import mdlaf.shadows.DropShadowBorder
import mdlaf.themes.MaterialLiteTheme
import mdlaf.utils.MaterialBorders
import mdlaf.utils.MaterialColors
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.plaf.BorderUIResource
import javax.swing.plaf.ColorUIResource

/**
 * This is the light-theme which ServerPackCreator uses. It is based on [MaterialLiteTheme]
 * via `extends` which allows us to use the base light-theme as a starting point but changing
 * every aspect of it in whatever way we like.
 *
 * @author Griefed
 */
class LightTheme : MaterialLiteTheme() {
    lateinit var textErrorColour: Color
        private set

    override fun installBorders() {
        super.installBorders()
        this.borderMenuBar = BorderUIResource(BorderFactory.createMatteBorder(0, 0, 1, 0, Color(225, 156, 43)))
        this.borderPopupMenu = BorderUIResource(BorderFactory.createLineBorder(backgroundPrimary))
        this.borderSpinner = BorderUIResource(BorderFactory.createLineBorder(backgroundTextField))
        this.borderSlider = BorderUIResource(
            BorderFactory.createCompoundBorder(
                borderSpinner, BorderFactory.createEmptyBorder(15, 15, 15, 15)
            )
        )
        this.cellBorderTableHeader = BorderUIResource(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundTableHeader),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            )
        )
        this.borderToolBar = borderSpinner
        this.borderDialogRootPane = MaterialBorders.LIGHT_SHADOW_BORDER
        this.borderProgressBar = borderSpinner
        this.borderTable = borderSpinner
        this.borderTableHeader = BorderUIResource(
            DropShadowBorder(this.backgroundPrimary, 5, 3, 0.4f, 12, true, true, true, true)
        )
        super.borderTitledBorder = BorderUIResource(BorderFactory.createLineBorder(MaterialColors.WHITE))
        super.titleColorTaskPane = MaterialColors.BLACK
    }

    override fun installColor() {
        super.installColor()
        val secondBackground = ColorUIResource(238, 238, 238)
        val disableBackground = ColorUIResource(210, 212, 213)
        val accentColor = ColorUIResource(231, 231, 232)
        val selectedForeground = ColorUIResource(84, 110, 122)
        val selectedBackground = ColorUIResource(220, 239, 237)
        this.backgroundPrimary = ColorUIResource(240, 240, 240)
        this.highlightBackgroundPrimary = ColorUIResource(0, 188, 212)
        this.textColor = ColorUIResource(0, 0, 0)
        this.textErrorColour = Color.magenta
        this.disableTextColor = ColorUIResource(148, 167, 176)
        this.buttonBackgroundColor = ColorUIResource(243, 244, 245)
        this.buttonBackgroundColorMouseHover = ColorUIResource(231, 231, 232)
        this.buttonDefaultBackgroundColorMouseHover = this.buttonBackgroundColorMouseHover
        this.buttonDefaultBackgroundColor = secondBackground
        this.buttonDisabledBackground = disableBackground
        this.buttonDefaultFocusColor = this.buttonFocusColor
        this.buttonBorderColor = ColorUIResource(211, 225, 232)
        this.buttonColorHighlight = selectedBackground
        this.buttonFocusColor = ColorUIResource(Color(0, 0, 0, 0))
        this.selectedInDropDownBackgroundComboBox = this.buttonBackgroundColorMouseHover
        this.selectedForegroundComboBox = this.textColor
        this.menuBackground = this.backgroundPrimary
        this.menuBackgroundMouseHover = this.buttonBackgroundColorMouseHover
        this.arrowButtonColorScrollBar = this.buttonBackgroundColor
        this.trackColorScrollBar = accentColor
        this.thumbColorScrollBar = disableBackground
        this.trackColorSlider = this.textColor
        this.haloColorSlider = MaterialColors.bleach(this.highlightBackgroundPrimary, 0.5f)
        this.highlightColorTabbedPane = this.buttonColorHighlight
        this.borderHighlightColorTabbedPane = this.buttonColorHighlight
        this.focusColorLineTabbedPane = this.highlightBackgroundPrimary
        this.disableColorTabTabbedPane = disableBackground
        this.backgroundTable = this.backgroundPrimary
        this.backgroundTableHeader = this.backgroundPrimary
        this.selectionBackgroundTable = this.buttonBackgroundColorMouseHover
        this.gridColorTable = this.backgroundPrimary
        this.alternateRowBackgroundTable = this.backgroundPrimary
        this.backgroundTextField = accentColor
        this.inactiveForegroundTextField = this.textColor
        this.inactiveBackgroundTextField = accentColor
        this.selectionBackgroundTextField = selectedBackground
        this.selectionForegroundTextField = selectedForeground
        super.disabledBackgroudnTextField = disableBackground
        super.disabledForegroundTextField = this.disableTextColor
        this.inactiveColorLineTextField = this.textColor
        this.activeColorLineTextField = this.highlightBackgroundPrimary
        this.mouseHoverButtonColorSpinner = this.buttonBackgroundColorMouseHover
        this.titleBackgroundGradientStartTaskPane = secondBackground
        this.titleBackgroundGradientEndTaskPane = secondBackground
        this.titleOverTaskPane = selectedForeground
        this.specialTitleOverTaskPane = selectedForeground
        this.backgroundTaskPane = this.backgroundPrimary
        this.borderColorTaskPane = ColorUIResource(211, 225, 232)
        this.contentBackgroundTaskPane = secondBackground
        this.selectionBackgroundList = selectedBackground
        this.selectionForegroundList = selectedForeground
        this.backgroundProgressBar = disableBackground
        this.foregroundProgressBar = this.highlightBackgroundPrimary
        this.withoutIconSelectedBackgroundToggleButton = MaterialColors.COSMO_DARK_GRAY
        this.withoutIconSelectedForegoundToggleButton = MaterialColors.BLACK
        this.withoutIconBackgroundToggleButton = MaterialColors.GRAY_300
        this.withoutIconForegroundToggleButton = MaterialColors.BLACK
        this.colorDividierSplitPane = MaterialColors.COSMO_DARK_GRAY
        this.colorDividierFocusSplitPane = selectedBackground
        super.backgroundSeparator = MaterialColors.GRAY_300
        super.foregroundSeparator = MaterialColors.GRAY_300
    }

    override fun installDefaultColor() {
        super.installDefaultColor()
        this.buttonDefaultTextColor = this.textColor
        this.foregroundTableHeader = this.textColor
        this.selectionForegroundTable = this.highlightBackgroundPrimary
    }

    override fun getButtonBorderEnableToAll(): Boolean {
        return true
    }

    override fun getButtonBorderEnable(): Boolean {
        return true
    }

    override fun getGridColorTable(): ColorUIResource {
        return this.gridColorTable
    }
}