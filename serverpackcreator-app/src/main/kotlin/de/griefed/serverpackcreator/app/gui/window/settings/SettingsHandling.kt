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
package de.griefed.serverpackcreator.app.gui.window.settings

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.app.gui.window.settings.components.Editor
import de.griefed.serverpackcreator.app.gui.window.settings.components.PropertiesChooser
import net.miginfocom.swing.MigLayout
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Griefed
 */
class SettingsHandling(
    guiProps: GuiProps,
    private val settingsEditorsTab: SettingsEditorsTab,
    private val apiProperties: ApiProperties,
    private val mainFrame: MainFrame,
    private val controlPanel: ControlPanel
) {
    val panel = JPanel()
    private val load =
        BalloonTipButton(Translations.settings_handle_load_label.toString(), guiProps.loadIcon, Translations.settings_handle_load_tooltip.toString(), guiProps) { load() }
    private val save =
        BalloonTipButton(Translations.settings_handle_save_label.toString(), guiProps.saveIcon, Translations.settings_handle_save_tooltip.toString(), guiProps) { save() }
    private val lastActionLabel = JLabel(Translations.settings_handle_idle.toString())

    private var lastAction: String
        set(value) {
            lastActionLabel.text = value
        }
        get() {
            return lastActionLabel.text
        }

    init {
        panel.layout = MigLayout(
            "",
            "5[]10[]10[]5",
            "0[30!]0"
        )
        panel.add(load, "cell 0 0, grow, height 30!")
        panel.add(save, "cell 1 0, grow, height 30!")
        panel.add(lastActionLabel, "cell 3 0, grow, height 30!")
    }

    private fun currentTime(): String {
        val format = SimpleDateFormat("HH:mm")
        return format.format(Date())
    }

    fun save() {
        for (tab in settingsEditorsTab.allTabs) {
            (tab as Editor).saveSettings()
        }
        apiProperties.saveProperties(apiProperties.serverPackCreatorPropertiesFile)
        lastAction = Translations.settings_handle_saved(currentTime())
        checkAll()
        controlPanel.updateStatus(Translations.settings_info_saved(apiProperties.serverPackCreatorPropertiesFile.absolutePath))
    }

    fun load() {
        val propertiesChooser = PropertiesChooser(apiProperties, Translations.settings_handle_chooser.toString())
        if (propertiesChooser.showOpenDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            apiProperties.loadProperties(propertiesChooser.selectedFile, false)
            for (tab in settingsEditorsTab.allTabs) {
                (tab as Editor).loadSettings()
            }
            lastAction = Translations.settings_handle_loaded(currentTime())
            controlPanel.updateStatus(Translations.settings_info_loaded(propertiesChooser.selectedFile.absolutePath))
        }
        checkAll()
    }

    fun checkAll() {
        val changes = settingsEditorsTab.allTabs.any {
            (it as Editor).hasUnsavedChanges()
        }
        if (changes) {
            settingsEditorsTab.title.showWarningIcon()
        } else {
            settingsEditorsTab.title.hideWarningIcon()
        }
    }
}