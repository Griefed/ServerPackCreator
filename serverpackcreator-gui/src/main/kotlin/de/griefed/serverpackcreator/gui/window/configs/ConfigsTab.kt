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
package de.griefed.serverpackcreator.gui.window.configs

import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.FileBrowser
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.TabPanel
import kotlinx.coroutines.*
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
class ConfigsTab(
    private val guiProps: GuiProps,
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val serverPackHandler: ServerPackHandler,
    private val apiPlugins: ApiPlugins,
) : TabPanel() {
    private val fileBrowser = FileBrowser(this, guiProps, utilities)
    val selectedEditor: ConfigEditorPanel?
        get() {
            return if (activeTab != null) {
                activeTab as ConfigEditorPanel
            } else {
                null
            }
        }

    init {
        tabs.addChangeListener {
            GlobalScope.launch(guiProps.configDispatcher) {
                if (tabs.tabCount != 0) {
                    for (tab in 0 until tabs.tabCount) {
                        (tabs.getComponentAt(tab) as ConfigEditorPanel).title.closeButton.isVisible = false
                    }
                    (activeTab!! as ConfigEditorPanel).title.closeButton.isVisible = true
                }
            }

        }
        for (i in 0..5) {
            addTab()
        }
        tabs.selectedIndex = 0
        (activeTab!! as ConfigEditorPanel).title.closeButton.isVisible = true
    }

    fun addTab(): ConfigEditorPanel {
        val editor = ConfigEditorPanel(
            guiProps, this,
            configurationHandler,
            apiProperties,
            versionMeta,
            utilities,
            serverPackHandler,
            apiPlugins
        ) { fileBrowser.show() }
        tabs.add(editor)
        tabs.setTabComponentAt(tabs.tabCount - 1, editor.title)
        tabs.selectedIndex = tabs.tabCount - 1
        return editor
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @param configFile The configuration file to parse and load into the GUI.
     * @author Griefed
     */
    fun loadConfig(configFile: File, tab: ConfigEditorPanel = addTab()) {
        tab.loadConfiguration(PackConfig(utilities, configFile))
    }
}