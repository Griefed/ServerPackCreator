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

import Gui
import de.griefed.serverpackcreator.gui.window.control.components.LarsonScanner
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.TabPanel
import de.griefed.serverpackcreator.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.gui.window.logs.TabbedLogsTab
import de.griefed.serverpackcreator.gui.window.settings.SettingsEditorTab
import net.miginfocom.swing.MigLayout
import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * TODO docs
 */
class MainPanel(
    private val guiProps: GuiProps,
    private val apiWrapper: ApiWrapper,
    larsonScanner: LarsonScanner
) : TabPanel(
    MigLayout(
        "",
        "0[grow]0",
        "0[top]0[bottom]0[bottom]0"
    )
) {
    val tabbedConfigsTab = TabbedConfigsTab(guiProps, apiWrapper)
    private val tabbedLogsTab = TabbedLogsTab(apiWrapper.apiProperties)
    private val settingsEditorTab = SettingsEditorTab(guiProps, apiWrapper.apiProperties)
    private val controlPanel = ControlPanel(
        guiProps,
        tabbedConfigsTab,
        larsonScanner,
        apiWrapper
    )

    init {
        tabs.addTab("Configs", tabbedConfigsTab.panel)
        tabs.addTab("Logs", tabbedLogsTab.panel)
        tabs.addTab("Settings", settingsEditorTab.panel)
        panel.add(larsonScanner, "height 40!,growx, south")
        panel.add(controlPanel.panel, "height 160!,growx, south")
        larsonScanner.loadConfig(guiProps.idleConfig)
        larsonScanner.play()
    }

    fun closeAndExit() {
        if (tabbedConfigsTab.tabs.tabCount == 0) {
            exitProcess(0)
        }
        val configs = mutableListOf<String>()
        for (tab in tabbedConfigsTab.allTabs) {
            val config = tab as ConfigEditor
            val modpackName = File(config.getModpackDirectory()).name
            if (config.editorTitle.title != Gui.createserverpack_gui_title_new.toString() && config.hasUnsavedChanges()) {
                tabbedConfigsTab.tabs.selectedComponent = tab
                if (DialogUtilities.createShowGet(
                        Gui.createserverpack_gui_close_unsaved_message(modpackName),
                        Gui.createserverpack_gui_close_unsaved_title(modpackName),
                        panel,
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION,
                        guiProps.warningIcon
                    ) == 0
                ) {
                    config.saveCurrentConfiguration()
                }
            }
            if (config.configFile != null && config.editorTitle.title != Gui.createserverpack_gui_title_new.toString()) {
                configs.add(config.configFile!!.absolutePath)
            }
        }
        apiWrapper.apiProperties.storeCustomProperty("lastloaded", configs.joinToString(","))
        apiWrapper.apiProperties.saveToDisk(apiWrapper.apiProperties.serverPackCreatorPropertiesFile)
        exitProcess(0)
    }
}