package de.griefed.serverpackcreator.gui.window.main

import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.components.TabPanel
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import de.griefed.serverpackcreator.gui.window.logs.Logs
import de.griefed.serverpackcreator.gui.window.control.ControlPanel
import de.griefed.serverpackcreator.gui.window.settings.SettingsEditor
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import net.miginfocom.swing.MigLayout
import javax.swing.JScrollPane
import javax.swing.JSeparator

/**
 * TODO docs
 */
class MainPanel(
    private val guiProps: GuiProps,
    configurationHandler: ConfigurationHandler,
    serverPackHandler: ServerPackHandler,
    apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    updateChecker: UpdateChecker,
    apiPlugins: ApiPlugins,
    migrationManager: MigrationManager,
    private val larsonScanner: LarsonScanner
) : TabPanel() {
    val configsTab = ConfigsTab(
        guiProps,
        configurationHandler,
        apiProperties,
        versionMeta,
        utilities,
        serverPackHandler,
        apiPlugins,
    )
    private val logs = Logs(guiProps)
    private val settingsEditor = SettingsEditor(guiProps, apiProperties)
    private val controlPanel = ControlPanel(
        guiProps,
        configsTab,
        larsonScanner,
        configurationHandler,
        apiProperties,
        serverPackHandler,
        utilities
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
        panel.add(tabs, "grow,push")
        panel.add(larsonScanner, "height 40!,growx, south")
        panel.add(controlPanel.panel, "height 160!,growx, south")
        panel.add(JSeparator(JSeparator.HORIZONTAL), "south")
        scroll.verticalScrollBar.unitIncrement = 5
        larsonScanner.loadConfig(guiProps.idleConfig)
        larsonScanner.play()
    }

    /**
     * TODO docs
     */
    fun larsonIdle() {
        larsonScanner.loadConfig(guiProps.idleConfig)
    }

    /**
     * TODO docs
     */
    fun larsonBusy() {
        larsonScanner.loadConfig(guiProps.busyConfig)
    }
}