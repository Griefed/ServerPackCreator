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
package de.griefed.serverpackcreator.gui.window.settings.components

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.settings.SettingsEditorsTab
import net.miginfocom.swing.MigLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class SettingsHandling(
    private val guiProps: GuiProps,
    private val settingsEditorsTab: SettingsEditorsTab,
    private val apiProperties: ApiProperties
) {
    val panel = JPanel()
    private val load = JButton("Load Configuration")
    private val save = JButton("Save Configuration")
    private val lastActionLabel = JLabel("Not saved or loaded yet...")
    var lastAction: String
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
        panel.add(lastActionLabel,"cell 3 0, grow, height 30!")
    }

    fun save() {
        TODO("Save all tabs configs")
        settingsEditorsTab.global.saveSettings()
        settingsEditorsTab.gui.saveSettings()
        settingsEditorsTab.webservice.saveSettings()
        apiProperties.saveToDisk(apiProperties.serverPackCreatorPropertiesFile)
    }

    fun load() {
        TODO("load from serverpackcreator.properties")
        apiProperties.loadProperties()
        settingsEditorsTab.global.loadSettings()
        settingsEditorsTab.gui.loadSettings()
        settingsEditorsTab.webservice.loadSettings()
    }
}