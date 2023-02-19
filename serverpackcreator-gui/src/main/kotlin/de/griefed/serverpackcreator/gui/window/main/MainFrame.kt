package de.griefed.serverpackcreator.gui.window.main

import Gui
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.window.menu.MainMenu
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import javax.swing.JFrame
import javax.swing.WindowConstants

/**
 * TODO docs
 */
class MainFrame(
    guiProps: GuiProps,
    configurationHandler: ConfigurationHandler,
    serverPackHandler: ServerPackHandler,
    apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    updateChecker: UpdateChecker,
    splashScreen: SplashScreen,
    apiPlugins: ApiPlugins,
    migrationManager: MigrationManager
) {
    private val mainFrame: JFrame = JFrame(Gui.createserverpack_gui_createandshowgui.toString())
    private val larsonScanner = LarsonScanner()
    private val mainPanel = MainPanel(
        guiProps,
        configurationHandler,
        serverPackHandler,
        apiProperties,
        versionMeta,
        utilities,
        updateChecker,
        apiPlugins,
        migrationManager,
        larsonScanner
    )
    private val mainMenu = MainMenu(
        guiProps,
        configurationHandler,
        serverPackHandler,
        apiProperties,
        versionMeta,
        utilities,
        updateChecker,
        apiPlugins,
        migrationManager,
        larsonScanner,
        mainPanel.configsTab
    )

    init {
        mainFrame.iconImage = guiProps.appIcon
        mainFrame.jMenuBar = mainMenu.menuBar
        mainFrame.contentPane.add(mainPanel.scroll)
        mainFrame.pack()
        mainFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        mainFrame.isLocationByPlatform = true
        mainFrame.setSize(1200, 800)
        mainFrame.isVisible = true
        mainFrame.isAutoRequestFocus = true
        splashScreen.close()
    }
}