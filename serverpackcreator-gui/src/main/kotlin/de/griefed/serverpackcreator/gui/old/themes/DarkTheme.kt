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
package de.griefed.serverpackcreator.gui.old.themes

import mdlaf.themes.JMarsDarkTheme
import mdlaf.utils.MaterialColors
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.plaf.BorderUIResource
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.InsetsUIResource

/**
 * This is the dark-theme which ServerPackCreator uses. It is based on [JMarsDarkTheme] via
 * `extends` which allows us to use the base dark-theme as a starting point but changing every
 * aspect of it in whatever way we like.
 *
 * @author Griefed
 */
@Suppress("unused")
class DarkTheme : JMarsDarkTheme() {
    private val selectedBackground: ColorUIResource = ColorUIResource(50, 66, 74)
    lateinit var textErrorColour: Color
        private set

    override fun installBorders() {
        super.installBorders()
        this.buttonBorder = BorderUIResource(BorderFactory.createEmptyBorder(8, 12, 8, 12))
        this.borderPanel = BorderUIResource(BorderFactory.createEmptyBorder())
        this.tabInsetsTabbedPane = InsetsUIResource(6, 10, 10, 10)
        this.selectedTabInsetsTabbedPane = InsetsUIResource(6, 10, 10, 10)
        this.borderFrameRootPane = BorderUIResource(BorderFactory.createEmptyBorder())
        this.cellBorderTableHeader = BorderUIResource(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorUIResource(192, 255, 238)),
                BorderFactory.createLineBorder(ColorUIResource(192, 255, 238))
            )
        )
    }

    override fun installColor() {
        super.installColor()
        this.backgroundPrimary = ColorUIResource(45, 48, 56)
        this.highlightBackgroundPrimary = ColorUIResource(66, 179, 176)
        this.textColor = ColorUIResource(255, 255, 255)
        this.textErrorColour = Color.cyan
        this.disableTextColor = ColorUIResource(170, 170, 170)
        this.buttonBackgroundColor = ColorUIResource(45, 48, 56)
        this.buttonBackgroundColorMouseHover = ColorUIResource(81, 86, 101)
        this.buttonDefaultBackgroundColorMouseHover = ColorUIResource(23, 137, 134)
        this.buttonDefaultBackgroundColor = ColorUIResource(66, 179, 176)
        this.buttonDisabledBackground = ColorUIResource(66, 69, 76)
        this.buttonFocusColor = ColorUIResource(Color(0, 0, 0, 0))
        this.buttonDefaultFocusColor = MaterialColors.WHITE
        this.buttonBorderColor = ColorUIResource(50, 83, 88)
        this.buttonColorHighlight = buttonBackgroundColorMouseHover
        this.selectedInDropDownBackgroundComboBox = ColorUIResource(249, 192, 98)
        this.selectedForegroundComboBox = MaterialColors.BLACK
        this.menuBackground = ColorUIResource(59, 62, 69)
        this.menuBackgroundMouseHover = ColorUIResource(249, 192, 98)
        this.trackColorScrollBar = ColorUIResource(81, 86, 101)
        this.thumbColorScrollBar = ColorUIResource(155, 155, 155)
        this.trackColorSlider = ColorUIResource(119, 119, 119)
        this.haloColorSlider = MaterialColors.bleach(Color(249, 192, 98), 0.2f)
        this.mouseHoverButtonColorSpinner = backgroundPrimary
        this.highlightColorTabbedPane = ColorUIResource(45, 48, 56)
        this.borderHighlightColorTabbedPane = ColorUIResource(45, 48, 56)
        this.focusColorLineTabbedPane = ColorUIResource(249, 192, 98)
        this.disableColorTabTabbedPane = ColorUIResource(170, 170, 170)
        this.backgroundTable = ColorUIResource(45, 48, 56)
        this.backgroundTableHeader = ColorUIResource(66, 179, 176)
        this.selectionBackgroundTable = ColorUIResource(126, 132, 153)
        this.gridColorTable = ColorUIResource(192, 255, 238)
        this.alternateRowBackgroundTable = ColorUIResource(59, 62, 69)
        this.backgroundTextField = ColorUIResource(81, 86, 101)
        this.inactiveForegroundTextField = MaterialColors.WHITE
        this.inactiveBackgroundTextField = ColorUIResource(81, 86, 101)
        this.selectionBackgroundTextField = ColorUIResource(249, 192, 98)
        super.disabledBackgroudnTextField = ColorUIResource(94, 94, 94)
        super.disabledForegroundTextField = ColorUIResource(170, 170, 170)
        this.selectionForegroundTextField = MaterialColors.BLACK
        this.inactiveColorLineTextField = MaterialColors.WHITE
        this.activeColorLineTextField = ColorUIResource(249, 192, 98)
        this.titleBackgroundGradientStartTaskPane = MaterialColors.GRAY_300
        this.titleBackgroundGradientEndTaskPane = MaterialColors.GRAY_500
        this.titleOverTaskPane = ColorUIResource(249, 192, 98)
        this.specialTitleOverTaskPane = MaterialColors.WHITE
        this.selectionBackgroundList = ColorUIResource(249, 192, 98)
        this.selectionForegroundList = MaterialColors.BLACK
        this.backgroundProgressBar = ColorUIResource(81, 86, 101)
        this.foregroundProgressBar = MaterialColors.WHITE
        this.withoutIconSelectedForegoundToggleButton = MaterialColors.BLACK
        this.withoutIconForegroundToggleButton = MaterialColors.WHITE
        this.colorDividierSplitPane = MaterialColors.COSMO_DARK_GRAY
        this.colorDividierFocusSplitPane = ColorUIResource(249, 192, 98)
        super.backgroundSeparator = MaterialColors.GRAY_300
        super.foregroundSeparator = MaterialColors.GRAY_300
        super.backgroundToolTip = backgroundPrimary
    }

    override fun installDefaultColor() {
        super.installDefaultColor()
        this.buttonDefaultTextColor = this.textColor
        this.foregroundTableHeader = this.textColor
        this.selectionForegroundTable = this.highlightBackgroundPrimary
    }

    override fun getButtonBorderEnable(): Boolean {
        return true
    }

    override fun getButtonBorderEnableToAll(): Boolean {
        return true
    }

    override fun getGridColorTable(): ColorUIResource {
        return this.gridColorTable
    }
}