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
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.gui.window.settings.SettingsEditorsTab
import dyorgio.runtime.run.`as`.root.RootExecutor
import net.miginfocom.swing.MigLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel

class SettingsHandling(
    guiProps: GuiProps,
    private val settingsEditorsTab: SettingsEditorsTab,
    private val apiProperties: ApiProperties,
    private val mainFrame: MainFrame
) {
    val panel = JPanel()
    private val load =
        BalloonTipButton("Load Configuration", guiProps.loadIcon, "Load settings from disk", guiProps) { load() }
    private val save =
        BalloonTipButton("Save Configuration", guiProps.saveIcon, "Save your settings", guiProps) { save() }
    private val lastActionLabel = JLabel("Not saved or loaded yet...")
    private val rootExecutor = RootExecutor()

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
        panel.add(lastActionLabel, "cell 3 0, grow, height 30!")
    }

    private fun currentTime(): String {
        val format = SimpleDateFormat("HH:mm")
        return format.format(Date())
    }

    private fun showHomeDirDialog() {
        JOptionPane.showMessageDialog(
            mainFrame.frame,
            "Home directory changed. Restart ServerPackCreator for this setting to take effect.",
            "Home directory changed!",
            JOptionPane.WARNING_MESSAGE
        )
    }

    private fun showCancelDialog() {
        JOptionPane.showMessageDialog(
            mainFrame.frame,
            "Home directory setting not saved.",
            "Canceled",
            JOptionPane.WARNING_MESSAGE
        )
    }

    private fun rootWarning(): Int {
        return JOptionPane.showConfirmDialog(
            mainFrame.frame,
            "Storing of the new home-directory setting requires root/admin-privileges. Continue?",
            "Root/Admin privileges required",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
    }

    fun save() {
        val previousHome = apiProperties.homeDirectory.absolutePath
        var saved = true
        settingsEditorsTab.global.saveSettings()
        settingsEditorsTab.gui.saveSettings()
        settingsEditorsTab.webservice.saveSettings()
        apiProperties.saveProperties(apiProperties.serverPackCreatorPropertiesFile)
        if (!apiProperties.overrideProperties.canWrite()) {
            if (rootWarning() == JOptionPane.OK_OPTION) {
                val overridePath = apiProperties.overrideProperties.absolutePath
                val overrides = apiProperties.overridesAsString()
                rootExecutor.run {
                    File(overridePath).writeText(overrides)
                }
            } else {
                showCancelDialog()
                saved = false
            }
        } else {
            apiProperties.saveOverrides()
        }
        if (previousHome != settingsEditorsTab.global.homeSetting.file.absolutePath && saved) {
            showHomeDirDialog()
        }
        lastAction = "Settings last saved ${currentTime()} ..."
    }

    fun load() {
        val propertiesChooser = PropertiesChooser(apiProperties, "Properties Chooser")
        if (propertiesChooser.showOpenDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            apiProperties.loadProperties(propertiesChooser.selectedFile)
            settingsEditorsTab.global.loadSettings()
            settingsEditorsTab.gui.loadSettings()
            settingsEditorsTab.webservice.loadSettings()
            lastAction = "Settings last loaded ${currentTime()} ..."
        }
    }
}