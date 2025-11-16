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
package de.griefed.serverpackcreator.app.gui.window.configs

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.api.config.PackConfig
import de.griefed.serverpackcreator.api.utilities.common.FileUtilities
import de.griefed.serverpackcreator.api.utilities.common.InvalidFileTypeException
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.TabPanel
import de.griefed.serverpackcreator.app.gui.components.TabTitle
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.gui.window.configs.components.ComponentResizer
import de.griefed.serverpackcreator.app.gui.window.configs.components.ConfigCheckTimer
import de.griefed.serverpackcreator.app.gui.window.menu.file.ConfigChooser
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.apache.commons.io.monitor.FileAlterationListener
import org.apache.commons.io.monitor.FileAlterationMonitor
import org.apache.commons.io.monitor.FileAlterationObserver
import org.apache.commons.io.monitor.FileEntry
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.concurrent.Executors
import javax.swing.*

/**
 * Tabbed pane housing every server pack config tab.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class TabbedConfigsTab(
    private val guiProps: GuiProps,
    private val apiWrapper: ApiWrapper,
    private val mainFrame: MainFrame
) : TabPanel() {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val choose = arrayOf(Translations.createserverpack_gui_quickselect_choose.toString())
    private val noVersions = DefaultComboBoxModel(arrayOf(Translations.createserverpack_gui_createserverpack_forge_none.toString()))
    private val componentResizer = ComponentResizer()
    private val timer = ConfigCheckTimer(500, guiProps, apiWrapper,this)
    val selectedEditor: ConfigEditor?
        get() {
            return if (activeTab != null) {
                activeTab as ConfigEditor
            } else {
                null
            }
        }

    val title = TabTitle(guiProps,"Configs")

    init {
        iconsDirectoryWatcher()
        propertiesDirectoryWatcher()
        tabs.addChangeListener {
            GlobalScope.launch(guiProps.configDispatcher, CoroutineStart.UNDISPATCHED) {
                if (tabs.tabCount != 0) {
                    for (tab in 0 until tabs.tabCount) {
                        (tabs.getComponentAt(tab) as ConfigEditor).title.closeButton.isVisible = false
                    }
                    if (activeTab != null) {
                        (activeTab as ConfigEditor).title.closeButton.isVisible = true
                    }
                }
            }

        }

        tabs.dropTarget = object : DropTarget() {
            override fun drop(event: DropTargetDropEvent) {
                val transferable = event?.transferable ?: return
                if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return
                }
                val files : List<File>

                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
                    files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                } catch (e: Exception) {
                    return
                }

                for (file in files) {
                    if (file.name.endsWith(".conf", ignoreCase = true)) {
                        loadConfig(file)
                    }
                }
            }
        }

        val lastLoadedConfigs = guiProps.getGuiProperty("lastloaded")
        if (!lastLoadedConfigs.isNullOrBlank()) {
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
        (activeTab!! as ConfigEditor).title.closeButton.isVisible = true

        val newAndLoadMenu = JPopupMenu()
        val newTabItem = JMenuItem(Translations.createserverpack_gui_title_new.toString())
        val loadConfigItem = JMenuItem(Translations.menubar_gui_menuitem_loadconfig.toString())
        newTabItem.addActionListener { addTab() }
        loadConfigItem.addActionListener { loadConfigFile() }
        newAndLoadMenu.add(newTabItem)
        newAndLoadMenu.add(loadConfigItem)

        val mouseAdapter = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                openPopup(e)
            }

            override fun mousePressed(e: MouseEvent) {
                super.mousePressed(e)
                openPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                super.mouseReleased(e)
                openPopup(e)
            }
            fun openPopup(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON3) {
                    if (tabs.ui.tabForCoordinate(tabs,e.x,e.y) == -1) {
                        newAndLoadMenu.show(tabs,e.x,e.y)
                    }
                }
            }
        }
        tabs.addMouseListener(mouseAdapter)
    }

    fun addTab(): ConfigEditor {
        val editor = ConfigEditor(
            guiProps,
            this,
            apiWrapper,
            noVersions,
            componentResizer
        )
        tabs.add(editor)
        tabs.setTabComponentAt(tabs.tabCount - 1, editor.title)
        tabs.selectedIndex = tabs.tabCount - 1
        return editor
    }

    fun saveAll() {
        for (tab in allTabs) {
            (tab as ConfigEditor).saveCurrentConfiguration()
        }
        checkAll()
    }

    fun saveAs(editor: ConfigEditor? = selectedEditor) {
        if (editor == null) {
            return
        }
        val configChooser = ConfigChooser(apiWrapper.apiProperties, Translations.menubar_gui_menuitem_saveas_title.toString())
        configChooser.dialogType = JFileChooser.SAVE_DIALOG
        if (configChooser.showSaveDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            if (configChooser.selectedFile.path.endsWith(".conf")) {
                editor.getCurrentConfiguration().save(configChooser.selectedFile.absoluteFile)
                log.debug("Saved configuration to: ${configChooser.selectedFile.absoluteFile}")
            } else {
                editor.getCurrentConfiguration().save(File("${configChooser.selectedFile.absoluteFile}.conf"))
                log.debug("Saved configuration to: ${configChooser.selectedFile.absoluteFile}.conf")
            }
        }
        checkAll()
    }

    fun checkAll() {
        timer.restart()
    }

    /**
     * When the GUI has finished loading, try and load the existing serverpackcreator.conf-file into
     * ServerPackCreator.
     *
     * @param configFile The configuration file to parse and load into the GUI.
     *
     * @author Griefed
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun loadConfig(configFile: File, tab: ConfigEditor = addTab()) {
        if (configFile.isFile) {
            tab.loadConfiguration(PackConfig(configFile), configFile)
        } else {
            GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
                JOptionPane.showMessageDialog(
                    panel,
                    Translations.createserverpack_gui_tabs_notfound_message(configFile.absoluteFile),
                    Translations.createserverpack_gui_tabs_notfound_title.toString(),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    fun loadConfigFile() {
        val configChooser = ConfigChooser(apiWrapper.apiProperties, Translations.createserverpack_gui_buttonloadconfig_title.toString())
        configChooser.isMultiSelectionEnabled = true
        if (configChooser.showOpenDialog(mainFrame.frame) == JFileChooser.APPROVE_OPTION) {
            val files = configChooser.selectedFiles.map { file ->
                try {
                    File(FileUtilities.resolveLink(file)).absoluteFile
                } catch (ex: InvalidFileTypeException) {
                    log.error("Could not resolve link/symlink. Using entry from user input for checks.", ex)
                    file.absoluteFile
                }
            }
            GlobalScope.launch(Dispatchers.Swing) {
                for (file in files) {
                    if (tabs.tabCount > 0 &&
                        DialogUtilities.createShowGet(
                            Translations.menubar_gui_config_load_message(file.absolutePath),
                            Translations.menubar_gui_config_load_title.toString(),
                            mainFrame.frame,
                            JOptionPane.QUESTION_MESSAGE,
                            JOptionPane.YES_NO_OPTION,
                            guiProps.warningIcon,
                            false,
                            arrayOf(Translations.menubar_gui_config_load_current, Translations.menubar_gui_config_load_new)
                        ) == 0
                    ) {
                        run {
                            loadConfig(file, selectedEditor!!)
                        }
                    } else {
                        run {
                            loadConfig(file)
                        }
                    }
                    Thread.sleep(2000)
                }
            }

        }
    }

    @Suppress("DuplicatedCode")
    private fun iconsDirectoryWatcher() {
        Executors.newSingleThreadExecutor().execute {
            val observer = FileAlterationObserver.builder()
                .setRootEntry(FileEntry(apiWrapper.apiProperties.iconsDirectory))
                .get()
            val alterations = object : FileAlterationListener {
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
                        val configTab = tab as ConfigEditor
                        val model = DefaultComboBoxModel(choose)
                        model.addAll(iconQuickSelections())
                        configTab.iconQuickSelectModel = model
                    }
                }
            }

            observer.addListener(alterations)
            val monitor = FileAlterationMonitor(2000)
            monitor.addObserver(observer)
            try {
                monitor.start()
            } catch (ex: Exception) {
                log.error("Error starting the FileWatcher Monitor.", ex)
            }
        }
    }

    @Suppress("DuplicatedCode")
    private fun propertiesDirectoryWatcher() {
        Executors.newSingleThreadExecutor().execute {
            val observer = FileAlterationObserver.builder()
                .setRootEntry(FileEntry(apiWrapper.apiProperties.propertiesDirectory))
                .get()
            val alterations = object : FileAlterationListener {
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
                        val configTab = tab as ConfigEditor
                        val model = DefaultComboBoxModel(choose)
                        model.addAll(propertiesQuickSelections())
                        configTab.propertiesQuickSelectModel = model
                    }
                }
            }
            observer.addListener(alterations)
            val monitor = FileAlterationMonitor(2000)
            monitor.addObserver(observer)
            try {
                monitor.start()
            } catch (ex: Exception) {
                log.error("Error starting the FileWatcher Monitor.", ex)
            }
        }
    }

    /**
     * List of server icons for quick selection in a given config tab.
     *
     * @author Griefed
     */
    fun iconQuickSelections(): List<String> {
        return getNames(apiWrapper.apiProperties.iconsDirectory, guiProps.imageRegex)
    }

    /**
     * Acquire all files-names for the passed [directory], filtered using the passed [matcher].
     *
     * @author Griefed
     */
    private fun getNames(directory: File, matcher: Regex): List<String> {
        val files = directory.listFiles()!!
        val filtered = files.filter { file -> file.name.matches(matcher) }
        return filtered.map { file -> file.name }
    }

    /**
     * List of server properties for quick selection in a given config tab.
     *
     * @author Griefed
     */
    fun propertiesQuickSelections(): List<String> {
        return getNames(apiWrapper.apiProperties.propertiesDirectory, guiProps.propertiesRegex)
    }

    fun stepByStepGuide() {
        selectedEditor?.stepByStepGuide() ?: addTab().stepByStepGuide()
    }
}