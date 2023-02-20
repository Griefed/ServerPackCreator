package de.griefed.serverpackcreator.gui.window.menu

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes.FlatIJLookAndFeelInfo
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import net.java.balloontip.styles.EdgedBalloonStyle
import javax.swing.*

/**
 * TODO docs
 */
class MainMenu(
    private val guiProps: GuiProps,
    configurationHandler: ConfigurationHandler,
    serverPackHandler: ServerPackHandler,
    private val apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    updateChecker: UpdateChecker,
    apiPlugins: ApiPlugins,
    migrationManager: MigrationManager,
    private val larsonScanner: LarsonScanner,
    private val configsTab: ConfigsTab
) {
    val menuBar: JMenuBar = JMenuBar()
    private val menuOne = JMenu("One")
    private val menuTwo = JMenu("Two")
    private val menuThree = JMenu("Three")
    private val menuFour = JMenu("Themes")

    private val m1i1 = JMenuItem("Item 1 Add Tab")
    private val m1i2 = JMenuItem("Item 2 Load New")
    private val m1i3 = JMenuItem("Item 3 Load")

    private val m2i1 = JMenuItem("Item 1")
    private val m2i2 = JMenuItem("Item 2")
    private val m2i3 = JMenuItem("Item 3")

    private val m3i1 = JMenuItem("Item 1")
    private val m3i2 = JMenuItem("Item 2")
    private val m3i3 = JMenuItem("Item 3")

    // TODO move menus from old menu
    // TODO move each menu to separate class
    init {
        menuOne.add(m1i1)
        menuOne.add(m1i2)
        menuOne.add(m1i3)

        menuTwo.add(m2i1)
        menuTwo.add(m2i2)
        menuTwo.add(m2i3)

        menuThree.add(m3i1)
        menuThree.add(m3i2)
        menuThree.add(m3i3)

        //Themes
        for (theme in FlatAllIJThemes.INFOS) {
            val item = JMenuItem(theme.name)
            item.addActionListener { changeTheme(theme) }
            menuFour.add(item)
        }

        m1i1.addActionListener { addTab() }
        m1i2.addActionListener { loadNew() }
        m1i3.addActionListener { load() }


        menuBar.add(menuOne)
        menuBar.add(menuTwo)
        menuBar.add(menuThree)
        menuBar.add(menuFour)
    }

    /**
     * TODO docs
     */
    private fun addTab() {
        configsTab.addTab()
    }

    /**
     * TODO docs
     */
    private fun loadNew() {
        configsTab.loadConfig(apiProperties.defaultConfig)
    }

    /**
     * TODO docs
     */
    private fun load() {
        configsTab.loadConfig(apiProperties.defaultConfig, configsTab.selectedEditor!!)
    }

    /**
     * TODO docs
     */
    private fun changeTheme(theme: FlatIJLookAndFeelInfo) {
        try {
            val instance = Class.forName(theme.className).getDeclaredConstructor().newInstance()  as FlatLaf
            FlatAnimatedLafChange.showSnapshot()
            UIManager.setLookAndFeel(instance)
            updateLarsonConfig()
            FlatLaf.updateUI()
            FlatAnimatedLafChange.hideSnapshotWithAnimation()
        } catch (ex: UnsupportedLookAndFeelException) {
            throw RuntimeException(ex)
        }
    }

    /**
     * TODO docs
     */
    private fun updateLarsonConfig() {
        val panelBackgroundColour = UIManager.getColor("Panel.background")
        val tabbedPaneFocusColor = UIManager.getColor("TabbedPane.focusColor")
        guiProps.balloonStyle = EdgedBalloonStyle(panelBackgroundColour,tabbedPaneFocusColor)
        guiProps.busyConfig.eyeBackgroundColour = panelBackgroundColour
        guiProps.busyConfig.scannerBackgroundColour = panelBackgroundColour
        guiProps.idleConfig.eyeBackgroundColour = panelBackgroundColour
        guiProps.idleConfig.scannerBackgroundColour = panelBackgroundColour
        val config = larsonScanner.currentConfig
        config.eyeBackgroundColour = panelBackgroundColour
        config.scannerBackgroundColour = panelBackgroundColour
        config.eyeColours = arrayOf(
            tabbedPaneFocusColor,
            tabbedPaneFocusColor,
            tabbedPaneFocusColor,
            tabbedPaneFocusColor,
            tabbedPaneFocusColor,
            tabbedPaneFocusColor,
            tabbedPaneFocusColor
        )
        larsonScanner.loadConfig(config)
    }
}