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
package de.griefed.serverpackcreator.app.gui.window

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.TabPanel
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.app.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.app.gui.window.control.components.LarsonScanner
import de.griefed.serverpackcreator.app.gui.window.logs.TabbedLogsTab
import de.griefed.serverpackcreator.app.gui.window.settings.SettingsEditorsTab
import de.griefed.serverpackcreator.app.gui.window.settings.components.Editor
import net.miginfocom.swing.MigLayout
import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * Main Panel, displayed in the [MainFrame], housing the config tabs, log tabs and settings.
 *
 * @author Griefed
 */
class MainPanel(
    private val guiProps: GuiProps,
    private val apiWrapper: ApiWrapper,
    larsonScanner: LarsonScanner,
    mainFrame: MainFrame,
    themeManager: ThemeManager
) : TabPanel(
    MigLayout(
        "",
        "0[grow]0",
        "0[top]0[bottom]0[bottom]0"
    ),
    "growx,growy,north"
) {
    val tabbedConfigsTab = TabbedConfigsTab(guiProps, apiWrapper, mainFrame)

    @Suppress("MemberVisibilityCanBePrivate")
    val tabbedLogsTab = TabbedLogsTab(apiWrapper.apiProperties)

    val controlPanel = ControlPanel(guiProps, tabbedConfigsTab, larsonScanner, apiWrapper, mainFrame)
    @Suppress("MemberVisibilityCanBePrivate")
    val settingsEditorsTab = SettingsEditorsTab(guiProps, apiWrapper.apiProperties, mainFrame, themeManager, controlPanel)

    init {
        tabs.addTab(Translations.main_tabs_config.toString(), tabbedConfigsTab.panel)
        tabs.setTabComponentAt(tabs.tabCount - 1, tabbedConfigsTab.title)
        tabs.addTab(Translations.main_tabs_logs.toString(), tabbedLogsTab.panel)
        tabs.addTab(Translations.main_tabs_settings.toString(), settingsEditorsTab.panel)
        tabs.setTabComponentAt(tabs.tabCount - 1, settingsEditorsTab.title)
        apiWrapper.apiPlugins.addTabExtensionTabs(tabs)
        panel.add(larsonScanner, "height 40!,growx, south")
        panel.add(controlPanel.panel, "height 160!,growx, south")
    }

    /**
     * @author Griefed
     */
    fun closeAndExit() {
        val configs = mutableListOf<String>()
        if (tabbedConfigsTab.tabs.tabCount != 0) {
            for (tab in tabbedConfigsTab.allTabs) {
                val config = tab as ConfigEditor
                val modpackName = File(config.getModpackDirectory()).name
                if (!config.isNewTab() && config.hasUnsavedChanges()) {
                    tabbedConfigsTab.tabs.selectedComponent = tab
                    if (DialogUtilities.createShowGet(
                            Translations.createserverpack_gui_close_unsaved_message(modpackName),
                            Translations.createserverpack_gui_close_unsaved_title(modpackName),
                            panel,
                            JOptionPane.WARNING_MESSAGE,
                            JOptionPane.YES_NO_OPTION,
                            guiProps.warningIcon
                        ) == 0
                    ) {
                        config.saveCurrentConfiguration()
                    }
                }
                if (config.configFile != null && config.title.title != Translations.createserverpack_gui_title_new.toString()) {
                    configs.add(config.configFile!!.absolutePath)
                }
            }
        }
        guiProps.storeGuiProperty("lastloaded", configs.joinToString(","))
        apiWrapper.apiProperties.saveProperties(apiWrapper.apiProperties.serverPackCreatorPropertiesFile)
        if (settingsEditorsTab.allTabs.any { (it as Editor).hasUnsavedChanges() }) {
            if (DialogUtilities.createShowGet(
                    Translations.main_unsaved_message.toString(),
                    Translations.main_unsaved_title.toString(),
                    panel,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION,
                    guiProps.warningIcon
                ) == 0
            ) {
                settingsEditorsTab.settingsHandling.save()
            }
        }
        exitProcess(0)
    }

    /**
     * @author Griefed
     */
    fun stepByStepGuide() {
        tabs.selectedIndex = 0
        tabbedConfigsTab.stepByStepGuide()
    }
}