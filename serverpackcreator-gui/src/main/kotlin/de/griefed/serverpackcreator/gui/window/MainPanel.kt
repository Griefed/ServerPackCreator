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
package de.griefed.serverpackcreator.gui.window

import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.TabPanel
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditorPanel
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import de.griefed.serverpackcreator.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.gui.window.logs.Logs
import de.griefed.serverpackcreator.gui.window.settings.SettingsEditor
import net.miginfocom.swing.MigLayout
import java.io.File
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JSeparator
import kotlin.system.exitProcess

/**
 * TODO docs
 */
class MainPanel(
    private val guiProps: GuiProps,
    private val apiWrapper: ApiWrapper,
    larsonScanner: LarsonScanner
) : TabPanel() {
    val configsTab = ConfigsTab(guiProps, apiWrapper)
    private val logs = Logs(apiWrapper.apiProperties)
    private val settingsEditor = SettingsEditor(guiProps, apiWrapper.apiProperties)
    private val controlPanel = ControlPanel(
        guiProps,
        configsTab,
        larsonScanner,
        apiWrapper
    )
    val scroll = JScrollPane(
        panel,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )

    init {
        tabs.addTab("Configs", configsTab.panel)
        tabs.addTab("Logs", logs.panel)
        tabs.addTab("Settings", settingsEditor.panel)
        panel.layout = MigLayout(
            "",
            "0[grow]0",
            "0[top]0[bottom]0[bottom]0"
        )
        panel.add(tabs, "grow,push,w 50:50:, h 50:50:")
        panel.add(larsonScanner, "height 40!,growx, south")
        panel.add(controlPanel.panel, "height 160!,growx, south")
        panel.add(JSeparator(JSeparator.HORIZONTAL), "south")
        scroll.verticalScrollBar.unitIncrement = 5
        larsonScanner.loadConfig(guiProps.idleConfig)
        larsonScanner.play()
    }

    fun closeAndExit() {
        if (configsTab.tabs.tabCount == 0) {
            exitProcess(0)
        }
        val configs = mutableListOf<String>()
        for (tab in configsTab.allTabs) {
            val config = tab as ConfigEditorPanel
            val modpackName = File(config.getModpackDirectory()).name
            if (config.title.title != Gui.createserverpack_gui_title_new.toString() && config.hasUnsavedChanges()) {
                configsTab.tabs.selectedComponent = tab
                if (DialogUtilities.createShowGet(
                        Gui.createserverpack_gui_close_unsaved_message(modpackName),
                        Gui.createserverpack_gui_close_unsaved_title(modpackName),
                        panel.parent,
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION,
                        guiProps.warningIcon
                    ) == 0
                ) {
                    configs.add(config.saveCurrentConfiguration().absolutePath)
                }
            }
        }
        apiWrapper.apiProperties.storeCustomProperty("lastloaded", configs.joinToString(","))
        apiWrapper.apiProperties.saveToDisk(apiWrapper.apiProperties.serverPackCreatorPropertiesFile)
        exitProcess(0)
    }
}