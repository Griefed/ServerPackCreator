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
package de.griefed.serverpackcreator.gui.window.settings

import Gui
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.settings.components.Editor
import java.awt.GraphicsEnvironment
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.event.ChangeListener
import javax.swing.plaf.FontUIResource


/**
 * @author Griefed
 */
class GuiSettings(
    private val guiProps: GuiProps,
    actionListener: ActionListener,
    changeListener: ChangeListener
) : Editor(Gui.settings_gui.toString(), guiProps) {

    val fontSizeIcon = StatusIcon(guiProps,Gui.settings_gui_font_tooltip.toString())
    val fontSizeLabel = ElementLabel(Gui.settings_gui_font_label.toString())
    val fontSizeSetting = ActionSlider(8,76,guiProps.fontSize,changeListener)
    val fontSizeRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(), guiProps) {
        fontSizeSetting.value = guiProps.fontSize
    }
    val fontSizeReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) {
        fontSizeSetting.value = 12
    }

    val startFocusIcon = StatusIcon(guiProps,Gui.settings_gui_focus_start_tooltip.toString())
    val startFocusLabel = ElementLabel(Gui.settings_gui_focus_start_label.toString())
    val startFocusSetting = ActionCheckBox(actionListener)
    val startFocusRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) {
        startFocusSetting.isSelected = guiProps.startFocusEnabled
    }
    val startFocusReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) {
        startFocusSetting.isSelected = false
    }

    val generationFocusIcon = StatusIcon(guiProps,Gui.settings_gui_focus_generation_tooltip.toString())
    val generationFocusLabel = ElementLabel(Gui.settings_gui_focus_generation_label.toString())
    val generationFocusSetting = ActionCheckBox(actionListener)
    val generationFocusRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) {
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
    }
    val generationFocusReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) {
        generationFocusSetting.isSelected = false
    }

    val themeIcon = StatusIcon(guiProps,Gui.settings_gui_theme_tooltip.toString())
    val themeLabel = ElementLabel(Gui.settings_gui_theme_label.toString())
    val themeSetting = ActionComboBox(DefaultComboBoxModel(FlatAllIJThemes.INFOS.map { it.name }.toTypedArray()),actionListener)
    val themeRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) {
        loadThemeFromProperties()
    }
    val themeReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) {
        for (theme in FlatAllIJThemes.INFOS) {
            if (theme.className == FlatDarkPurpleIJTheme().javaClass.name) {
                themeSetting.selectedItem = theme.name
            }
        }
    }

    val fontIcon = StatusIcon(guiProps,Gui.settings_gui_font_family_tooltip.toString())
    val fontLabel = ElementLabel(Gui.settings_gui_font_family_label.toString())
    val fontSetting = ActionComboBox(DefaultComboBoxModel(GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames),actionListener)
    val fontRevert = BalloonTipButton(null,guiProps.revertIcon,Gui.settings_revert.toString(),guiProps) {
        loadFontProperties()
    }
    val fontReset = BalloonTipButton(null,guiProps.resetIcon,Gui.settings_reset.toString(),guiProps) {
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == "JetBrains Mono") {
                fontSetting.selectedItem = font
            }
        }
    }

    init {
        loadSettings()
        fontSizeSetting.paintTicks = true
        fontSizeSetting.paintLabels = true
        fontSizeSetting.majorTickSpacing = 4
        fontSizeSetting.minorTickSpacing = 2

        val themeConfigClassName = guiProps.getGuiProperty("theme", FlatDarkPurpleIJTheme().javaClass.name)
        for (theme in FlatAllIJThemes.INFOS) {
            if (theme.className == themeConfigClassName) {
                themeSetting.selectedItem = theme.name
            }
        }
        val currentFont = guiProps.font
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == currentFont.family) {
                fontSetting.selectedItem = font
            }
        }

        var y = 0
        panel.add(fontSizeIcon, "cell 0 $y")
        panel.add(fontSizeLabel, "cell 1 $y")
        panel.add(fontSizeSetting, "cell 2 $y, grow")
        panel.add(fontSizeRevert, "cell 3 $y")
        panel.add(fontSizeReset, "cell 4 $y")

        y++
        panel.add(startFocusIcon, "cell 0 $y")
        panel.add(startFocusLabel, "cell 1 $y")
        panel.add(startFocusSetting, "cell 2 $y, grow")
        panel.add(startFocusRevert, "cell 3 $y")
        panel.add(startFocusReset, "cell 4 $y")

        y++
        panel.add(generationFocusIcon, "cell 0 $y")
        panel.add(generationFocusLabel, "cell 1 $y")
        panel.add(generationFocusSetting, "cell 2 $y, grow")
        panel.add(generationFocusRevert, "cell 3 $y")
        panel.add(generationFocusReset, "cell 4 $y")

        y++
        panel.add(themeIcon, "cell 0 $y")
        panel.add(themeLabel, "cell 1 $y")
        panel.add(themeSetting, "cell 2 $y, grow")
        panel.add(themeRevert, "cell 3 $y")
        panel.add(themeReset, "cell 4 $y")

        y++
        panel.add(fontIcon, "cell 0 $y")
        panel.add(fontLabel, "cell 1 $y")
        panel.add(fontSetting, "cell 2 $y, grow")
        panel.add(fontRevert, "cell 3 $y")
        panel.add(fontReset, "cell 4 $y")
    }

    /**
     * @author Griefed
     */
    override fun loadSettings() {
        fontSizeSetting.value = guiProps.fontSize
        startFocusSetting.isSelected = guiProps.startFocusEnabled
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
        loadThemeFromProperties()
        loadFontProperties()
    }

    /**
     * @author Griefed
     */
    override fun saveSettings() {
        guiProps.fontSize = fontSizeSetting.value
        guiProps.startFocusEnabled = startFocusSetting.isSelected
        guiProps.generationFocusEnabled = generationFocusSetting.isSelected
        for (theme in FlatAllIJThemes.INFOS) {
            if (theme.name == themeSetting.selectedItem.toString()) {
                val instance = Class.forName(theme.className).getDeclaredConstructor().newInstance() as FlatLaf
                guiProps.currentTheme = instance
            }
        }
        guiProps.font = FontUIResource(fontSetting.selectedItem.toString(),font.size,font.style,)
    }

    /**
     * @author Griefed
     */
    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()
        if (fontSizeSetting.value < 8 || fontSizeSetting.value > 76) {
            fontSizeIcon.error(Gui.settings_gui_font_error.toString())
            errors.add(Gui.settings_gui_font_error.toString())
        } else {
            fontSizeIcon.info()
        }
        if (errors.isNotEmpty()) {
            title.setAndShowErrorIcon(Gui.settings_gui_errors.toString())
        } else {
            title.hideErrorIcon()
        }
        return errors.toList()
    }

    /**
     * @author Griefed
     */
    override fun hasUnsavedChanges(): Boolean {
        val changes = fontSizeSetting.value != guiProps.fontSize ||
                startFocusSetting.isSelected != guiProps.startFocusEnabled ||
                generationFocusSetting.isSelected != guiProps.generationFocusEnabled ||
                fontSizeSetting.value != guiProps.fontSize ||
                fontSetting.selectedItem.toString() != guiProps.font.family
        if (changes) {
            title.showWarningIcon()
        } else {
            title.hideWarningIcon()
        }
        return changes
    }

    /**
     * @author Griefed
     */
    private fun loadThemeFromProperties() {
        val current = guiProps.getGuiProperty("theme", FlatDarkPurpleIJTheme().javaClass.name)
        for (theme in FlatAllIJThemes.INFOS) {
            if (theme.className == current) {
                themeSetting.selectedItem = theme.name
            }
        }
    }

    /**
     * @author Griefed
     */
    private fun loadFontProperties() {
        val current = guiProps.font
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == current.family) {
                fontSetting.selectedItem = font
            }
        }
    }
}