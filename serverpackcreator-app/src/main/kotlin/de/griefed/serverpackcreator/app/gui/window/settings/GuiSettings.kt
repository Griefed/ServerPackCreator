/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.window.settings

import Translations
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.*
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import de.griefed.serverpackcreator.app.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.app.gui.window.settings.components.Editor
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
    changeListener: ChangeListener,
    private val themeManager: ThemeManager,
    private val controlPanel: ControlPanel
) : Editor(Translations.settings_gui.toString(), guiProps) {

    private val fontSizeIcon = StatusIcon(guiProps,Translations.settings_gui_font_tooltip.toString())
    private val fontSizeLabel = ElementLabel(Translations.settings_gui_font_label.toString())
    private val fontSizeSetting = ActionSlider(8,76, guiProps.fontSize,changeListener)
    private val fontSizeRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        fontSizeSetting.value = guiProps.fontSize
    }
    private val fontSizeReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        fontSizeSetting.value = 12
    }

    private val startFocusIcon = StatusIcon(guiProps,Translations.settings_gui_focus_start_tooltip.toString())
    private val startFocusLabel = ElementLabel(Translations.settings_gui_focus_start_label.toString())
    private val startFocusSetting = ActionCheckBox(actionListener)
    private val startFocusRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        startFocusSetting.isSelected = guiProps.startFocusEnabled
    }
    private val startFocusReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        startFocusSetting.isSelected = false
    }

    private val generationFocusIcon = StatusIcon(guiProps,Translations.settings_gui_focus_generation_tooltip.toString())
    private val generationFocusLabel = ElementLabel(Translations.settings_gui_focus_generation_label.toString())
    private val generationFocusSetting = ActionCheckBox(actionListener)
    private val generationFocusRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
    }
    private val generationFocusReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        generationFocusSetting.isSelected = false
    }

    private val themeIcon = StatusIcon(guiProps,Translations.settings_gui_theme_tooltip.toString())
    private val themeLabel = ElementLabel(Translations.settings_gui_theme_label.toString())
    private val themeSetting = ActionComboBox(DefaultComboBoxModel(themeManager.themes.map { it.name }.toTypedArray()),actionListener)
    private val themeRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        loadThemeFromProperties()
    }
    private val themeReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        for (theme in FlatAllIJThemes.INFOS) {
            if (theme.className == FlatDarkPurpleIJTheme().javaClass.name) {
                themeSetting.selectedItem = theme.name
            }
        }
    }

/*    private val fontIcon = StatusIcon(guiProps,Translations.settings_gui_font_family_tooltip.toString())
    private val fontLabel = ElementLabel(Translations.settings_gui_font_family_label.toString())
    private val fontSetting = ActionComboBox(DefaultComboBoxModel(GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames),actionListener)
    private val fontRevert = BalloonTipButton(null, guiProps.revertIcon,Translations.settings_revert.toString(), guiProps) {
        loadFontProperties()
    }
    private val fontReset = BalloonTipButton(null, guiProps.resetIcon,Translations.settings_reset.toString(), guiProps) {
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == "JetBrains Mono") {
                fontSetting.selectedItem = font
            }
        }
    }*/

    private val manualEditIcon = StatusIcon(guiProps, Translations.settings_gui_manualedit_tooltip.toString())
    private val manualEditLabel = ElementLabel(Translations.settings_gui_manualedit_label.toString())
    private val manualEditSetting = ActionCheckBox(actionListener)
    private val manualEditRevert = BalloonTipButton(null, guiProps.revertIcon, Translations.settings_revert.toString(), guiProps) {
        manualEditSetting.isSelected = guiProps.allowManualEditing
    }
    private val manualEditReset = BalloonTipButton(null, guiProps.resetIcon, Translations.settings_reset.toString(), guiProps) {
        manualEditSetting.isSelected = false
    }

    init {
        loadSettings()
        fontSizeSetting.paintTicks = true
        fontSizeSetting.paintLabels = true
        fontSizeSetting.majorTickSpacing = 4
        fontSizeSetting.minorTickSpacing = 2

        for (theme in themeManager.themes) {
            if (theme.name == guiProps.theme) {
                themeSetting.selectedItem = theme.name
            }
        }
/*        val currentFont = guiProps.font
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == currentFont.family) {
                fontSetting.selectedItem = font
            }
        }*/

        var y = 0
        panel.add(fontSizeIcon, "cell 0 0")
        panel.add(fontSizeLabel, "cell 1 0")
        panel.add(fontSizeSetting, "cell 2 0, grow")
        panel.add(fontSizeRevert, "cell 3 0")
        panel.add(fontSizeReset, "cell 4 0")

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

/*        y++
        panel.add(fontIcon, "cell 0 $y")
        panel.add(fontLabel, "cell 1 $y")
        panel.add(fontSetting, "cell 2 $y, grow")
        panel.add(fontRevert, "cell 3 $y")
        panel.add(fontReset, "cell 4 $y")*/

        y++
        panel.add(manualEditIcon, "cell 0 $y")
        panel.add(manualEditLabel, "cell 1 $y")
        panel.add(manualEditSetting, "cell 2 $y, grow")
        panel.add(manualEditRevert, "cell 3 $y")
        panel.add(manualEditReset, "cell 4 $y")
    }

    override fun loadSettings() {
        fontSizeSetting.value = guiProps.fontSize
        startFocusSetting.isSelected = guiProps.startFocusEnabled
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
        loadThemeFromProperties()
        /*loadFontProperties()*/
        manualEditSetting.isSelected = guiProps.allowManualEditing
    }

    override fun saveSettings() {
        guiProps.fontSize = fontSizeSetting.value
        guiProps.startFocusEnabled = startFocusSetting.isSelected
        guiProps.generationFocusEnabled = generationFocusSetting.isSelected
        themeManager.setTheme(themeManager.getThemeInfo(themeSetting.selectedItem.toString())!!)
        guiProps.theme = themeSetting.selectedItem.toString()
        /*guiProps.font = FontUIResource(fontSetting.selectedItem.toString(),font.size,font.style)
        if (guiProps.allowManualEditing != manualEditSetting.isSelected) {
            controlPanel.updateStatus(Translations.settings_gui_restart_manualedit(manualEditSetting.isSelected.toString()))
        }*/
        guiProps.allowManualEditing = manualEditSetting.isSelected
    }

    override fun validateSettings(): List<String> {
        val errors = mutableListOf<String>()
        if (fontSizeSetting.value < 8 || fontSizeSetting.value > 76) {
            fontSizeIcon.error(Translations.settings_gui_font_error.toString())
            errors.add(Translations.settings_gui_font_error.toString())
        } else {
            fontSizeIcon.info()
        }
        if (errors.isNotEmpty()) {
            title.setAndShowErrorIcon(Translations.settings_gui_errors.toString())
        } else {
            title.hideErrorIcon()
        }
        return errors.toList()
    }

    override fun hasUnsavedChanges(): Boolean {
        val changes = fontSizeSetting.value != guiProps.fontSize ||
                startFocusSetting.isSelected != guiProps.startFocusEnabled ||
                generationFocusSetting.isSelected != guiProps.generationFocusEnabled ||
                fontSizeSetting.value != guiProps.fontSize ||
                /*fontSetting.selectedItem.toString() != guiProps.font.family ||*/
                themeSetting.selectedItem != guiProps.theme ||
                manualEditSetting.isSelected != guiProps.allowManualEditing
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
    /*private fun loadFontProperties() {
        val current = guiProps.font
        for (font in GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames) {
            if (font == current.family) {
                fontSetting.selectedItem = font
            }
        }
    }*/
}