package de.griefed.serverpackcreator.gui.window

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.window.main.MainFrame
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker

class MainWindow(
    private val configurationHandler: ConfigurationHandler,
    private val serverPackHandler: ServerPackHandler,
    private val apiProperties: ApiProperties,
    private val versionMeta: VersionMeta,
    private val utilities: Utilities,
    private val updateChecker: UpdateChecker,
    private val splashScreen: SplashScreen,
    private val apiPlugins: ApiPlugins,
    private val migrationManager: MigrationManager
) : Runnable {
    private val alphaBetaRegex = "^(.*alpha.*|.*beta.*|.*dev.*)$".toRegex()
    private val guiProps: GuiProps

    init {
        FlatJetBrainsMonoFont.install()
        FlatLaf.setPreferredFontFamily(FlatJetBrainsMonoFont.FAMILY)
        if (apiProperties.apiVersion.matches(alphaBetaRegex)) {
            FlatInspector.install( "ctrl shift alt X" )
            FlatUIDefaultsInspector.install( "ctrl shift alt Y" )
        }
        try {
            FlatDarkPurpleIJTheme.setup()
        } catch (weTried: Exception) {
            weTried.printStackTrace()
        }
        guiProps = GuiProps()
    }

    override fun run() {
        MainFrame(
            guiProps,
            configurationHandler,
            serverPackHandler,
            apiProperties,
            versionMeta,
            utilities,
            updateChecker,
            splashScreen,
            apiPlugins,
            migrationManager
        )
    }
}