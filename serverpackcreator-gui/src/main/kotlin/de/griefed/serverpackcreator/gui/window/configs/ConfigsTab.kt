package de.griefed.serverpackcreator.gui.window.configs

import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.filebrowser.FileBrowser
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.window.components.TabPanel
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker

class ConfigsTab(
    private val guiProps: GuiProps,
    private val configurationHandler: ConfigurationHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val serverPackHandler: ServerPackHandler,
    private val apiPlugins: ApiPlugins,
) : TabPanel() {
    private val fileBrowser = FileBrowser(this)
    val selectedEditor: ConfigEditorPanel?
        get() {
            return if (activeTab != null) {
                activeTab as ConfigEditorPanel
            } else {
                null
            }
        }

    init {
        Thread(fileBrowser).start()
        tabs.addChangeListener {
            if (tabs.tabCount != 0) {
                for (tab in 0 until tabs.tabCount) {
                    (tabs.getComponentAt(tab) as ConfigEditorPanel).title.closeButton.isVisible = false
                }
                (activeTab!! as ConfigEditorPanel).title.closeButton.isVisible = true
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
            apiPlugins,
        ) { fileBrowser.show() }
        tabs.add(editor)
        tabs.setTabComponentAt(tabs.tabCount - 1, editor.title)
        return editor
    }
}