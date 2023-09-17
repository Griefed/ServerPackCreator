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

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.*
import de.griefed.serverpackcreator.gui.window.settings.components.Editor
import java.awt.event.ActionListener
import javax.swing.event.ChangeListener

class GuiSettings(
    private val guiProps: GuiProps,
    actionListener: ActionListener,
    changeListener: ChangeListener
) : Editor("Gui", guiProps) {

    val fontSizeIcon = StatusIcon(guiProps,"Size of fonts in ServerPackCreators GUI.")
    val fontSizeLabel = ElementLabel("Font Size")
    val fontSizeSetting = ActionSlider(8,76,guiProps.fontSize,changeListener)
    val fontSizeRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes", guiProps) {
        fontSizeSetting.value = guiProps.fontSize
    }
    val fontSizeReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) {
        fontSizeSetting.value = 12
    }

    val startFocusIcon = StatusIcon(guiProps,"Whether ServerPackCreator should be focused after starting.")
    val startFocusLabel = ElementLabel("Focus On Start")
    val startFocusSetting = ActionCheckBox(actionListener)
    val startFocusRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes",guiProps) {
        startFocusSetting.isSelected = guiProps.startFocusEnabled
    }
    val startFocusReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) {
        startFocusSetting.isSelected = false
    }

    val generationFocusIcon = StatusIcon(guiProps,"Whether ServerPackCreator should be focused after a server pack has been generated.")
    val generationFocusLabel = ElementLabel("Focus After Generation")
    val generationFocusSetting = ActionCheckBox(actionListener)
    val generationFocusRevert = BalloonTipButton(null,guiProps.revertIcon,"Revert changes",guiProps) {
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
    }
    val generationFocusReset = BalloonTipButton(null,guiProps.resetIcon,"Reset to default value",guiProps) {
        generationFocusSetting.isSelected = false
    }

    init {
        loadSettings()
        fontSizeSetting.paintTicks = true
        fontSizeSetting.paintLabels = true
        fontSizeSetting.majorTickSpacing = 4
        fontSizeSetting.minorTickSpacing = 2

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
    }

    override fun loadSettings() {
        fontSizeSetting.value = guiProps.fontSize
        startFocusSetting.isSelected = guiProps.startFocusEnabled
        generationFocusSetting.isSelected = guiProps.generationFocusEnabled
    }

    override fun saveSettings() {
        guiProps.fontSize = fontSizeSetting.value
        guiProps.startFocusEnabled = startFocusSetting.isSelected
        guiProps.generationFocusEnabled = generationFocusSetting.isSelected
    }

    override fun validateSettings(): List<String> {
        return listOf("")
    }

    override fun hasUnsavedChanges(): Boolean {
        return fontSizeSetting.value != guiProps.fontSize ||
            startFocusSetting.isSelected != guiProps.startFocusEnabled ||
            generationFocusSetting.isSelected != guiProps.generationFocusEnabled
    }
}