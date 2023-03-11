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
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.util.concurrent.Executors
import javax.swing.DefaultComboBoxModel

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
    private val log = cachedLoggerOf(this.javaClass)
    private val fileBrowser = FileBrowser(this, guiProps, utilities)
    private val choose = arrayOf(Gui.createserverpack_gui_quickselect_choose.toString())
    val selectedEditor: ConfigEditorPanel?
        get() {
            return if (activeTab != null) {
                activeTab as ConfigEditorPanel
            } else {
                null
            }
        }

    init {
        iconsDirectoryWatcher()
        propertiesDirectoryWatcher()
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

        val lastLoadedConfigs = apiProperties.retrieveCustomProperty(this.javaClass,"lastloaded")
        if (lastLoadedConfigs != null) {
            val configs = if (lastLoadedConfigs.contains(",")) {
                lastLoadedConfigs.split(",").map { File(it) }
            } else {
                listOf(File(lastLoadedConfigs))
            }
            for (configFile in configs) {
                loadConfig(configFile)
            }
        } else {
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
        tab.loadConfiguration(PackConfig(utilities, configFile),configFile)
    }

    private fun iconsDirectoryWatcher() {
        Executors.newSingleThreadExecutor().execute {
            val observer = FileAlterationObserver(apiProperties.iconsDirectory)
            val alterations = object: FileAlterationListener {
                override fun onStart(observer: FileAlterationObserver?) {}
                override fun onDirectoryCreate(directory: File?) {}
                override fun onDirectoryChange(directory: File?) {}
                override fun onDirectoryDelete(directory: File?) {}
                override fun onStop(observer: FileAlterationObserver?) {}

                override fun onFileCreate(file: File?) {
                    update()
                }

                override fun onFileChange(file: File?) {
                    update()
                }

                override fun onFileDelete(file: File?) {
                    update()
                }

                private fun update() {
                    if (tabs.tabCount == 0) {
                        return
                    }
                    for (tab in allTabs) {
                        val configTab = tab as ConfigEditorPanel
                        val model = DefaultComboBoxModel(choose)
                        model.addAll(apiProperties.iconQuickSelections)
                        configTab.iconQuickSelect.model = model
                    }
                }
            }
            observer.addListener(alterations)
            val monitor = FileAlterationMonitor(2000)
            monitor.addObserver(observer)
            try {
                monitor.start()
            } catch(ex: Exception) {
                log.error("Error starting the FileWatcher Monitor.", ex)
            }
        }
    }

    private fun propertiesDirectoryWatcher() {
        Executors.newSingleThreadExecutor().execute {
            val observer = FileAlterationObserver(apiProperties.propertiesDirectory)
            val alterations = object: FileAlterationListener {
                override fun onStart(observer: FileAlterationObserver?) {}
                override fun onDirectoryCreate(directory: File?) {}
                override fun onDirectoryChange(directory: File?) {}
                override fun onDirectoryDelete(directory: File?) {}
                override fun onStop(observer: FileAlterationObserver?) {}

                override fun onFileCreate(file: File?) {
                    update()
                }

                override fun onFileChange(file: File?) {
                    update()
                }

                override fun onFileDelete(file: File?) {
                    update()
                }

                private fun update() {
                    if (tabs.tabCount == 0) {
                        return
                    }
                    for (tab in allTabs) {
                        val configTab = tab as ConfigEditorPanel
                        val model = DefaultComboBoxModel(choose)
                        model.addAll(apiProperties.propertiesQuickSelections)
                        configTab.propertiesQuickSelect.model = model
                    }
                }
            }
            observer.addListener(alterations)
            val monitor = FileAlterationMonitor(2000)
            monitor.addObserver(observer)
            try {
                monitor.start()
            } catch(ex: Exception) {
                log.error("Error starting the FileWatcher Monitor.", ex)
            }
        }
    }
}