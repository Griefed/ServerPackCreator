package de.griefed.serverpackcreator.gui.window.menu

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.intellijthemes.*
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiPlugins
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.ConfigurationHandler
import de.griefed.serverpackcreator.api.ServerPackHandler
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import javax.swing.*

class MainMenu(
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
) {
    val menuBar: JMenuBar = JMenuBar()
    private val menuOne = JMenu("One")
    private val menuTwo = JMenu("Two")
    private val menuThree = JMenu("Three")
    private val menuFour = JMenu("Four")

    private val m1i1 = JMenuItem("Item 1")
    private val m1i2 = JMenuItem("Item 2")
    private val m1i3 = JMenuItem("Item 3")

    private val m2i1 = JMenuItem("Item 1")
    private val m2i2 = JMenuItem("Item 2")
    private val m2i3 = JMenuItem("Item 3")

    private val m3i1 = JMenuItem("Item 1")
    private val m3i2 = JMenuItem("Item 2")
    private val m3i3 = JMenuItem("Item 3")

    private val m4i1 = JMenuItem("arc")
    private val m4i2 = JMenuItem("arcOrange")
    private val m4i3 = JMenuItem("arcDark")
    private val m4i4 = JMenuItem("arcDarkOrange")
    private val m4i5 = JMenuItem("carbon")
    private val m4i6 = JMenuItem("cobalt2")
    private val m4i7 = JMenuItem("cyanLight")
    private val m4i8 = JMenuItem("darkFlat")
    private val m4i9 = JMenuItem("darkPurple")
    private val m4i10 = JMenuItem("dracula")
    private val m4i11 = JMenuItem("gradiantoDarkFuchsia")
    private val m4i12 = JMenuItem("gradiantoDeepOcean")
    private val m4i13 = JMenuItem("gradiantoMignightBlue")
    private val m4i14 = JMenuItem("gradiantoNatureGreen")
    private val m4i15 = JMenuItem("gray")
    private val m4i16 = JMenuItem("gruvboxDarkHard")
    private val m4i17 = JMenuItem("gruvboxDarkMedium")
    private val m4i18 = JMenuItem("gruvboxDarkSoft")
    private val m4i19 = JMenuItem("hiberbeeDark")
    private val m4i20 = JMenuItem("highContrast")
    private val m4i21 = JMenuItem("lightFlat")
    private val m4i22 = JMenuItem("materialDesignDark")
    private val m4i23 = JMenuItem("monocai")
    private val m4i24 = JMenuItem("monokaiPro")
    private val m4i25 = JMenuItem("nord")
    private val m4i26 = JMenuItem("oneDark")
    private val m4i27 = JMenuItem("solarizedDark")
    private val m4i28 = JMenuItem("solarizedLight")
    private val m4i29 = JMenuItem("spacegray")
    private val m4i30 = JMenuItem("vuesion")
    private val m4i31 = JMenuItem("xcodeDark")
    private val m4i32 = JMenuItem("light")
    private val m4i33 = JMenuItem("dark")

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

        menuFour.add(m4i1)
        menuFour.add(m4i2)
        menuFour.add(m4i3)
        menuFour.add(m4i4)
        menuFour.add(m4i5)
        menuFour.add(m4i6)
        menuFour.add(m4i7)
        menuFour.add(m4i8)
        menuFour.add(m4i9)
        menuFour.add(m4i10)
        menuFour.add(m4i11)
        menuFour.add(m4i12)
        menuFour.add(m4i13)
        menuFour.add(m4i14)
        menuFour.add(m4i15)
        menuFour.add(m4i16)
        menuFour.add(m4i17)
        menuFour.add(m4i18)
        menuFour.add(m4i19)
        menuFour.add(m4i20)
        menuFour.add(m4i21)
        menuFour.add(m4i22)
        menuFour.add(m4i23)
        menuFour.add(m4i24)
        menuFour.add(m4i25)
        menuFour.add(m4i26)
        menuFour.add(m4i27)
        menuFour.add(m4i28)
        menuFour.add(m4i29)
        menuFour.add(m4i30)
        menuFour.add(m4i31)
        menuFour.add(m4i32)
        menuFour.add(m4i33)

        m4i1.addActionListener { arc() }
        m4i2.addActionListener { arcOrange() }
        m4i3.addActionListener { arcDark() }
        m4i4.addActionListener { arcDarkOrange() }
        m4i5.addActionListener { carbon() }
        m4i6.addActionListener { cobalt2() }
        m4i7.addActionListener { cyanLight() }
        m4i8.addActionListener { darkFlat() }
        m4i9.addActionListener { darkPurple() }
        m4i10.addActionListener { dracula() }
        m4i11.addActionListener { gradiantoDarkFuchsia() }
        m4i12.addActionListener { gradiantoDeepOcean() }
        m4i13.addActionListener { gradiantoMignightBlue() }
        m4i14.addActionListener { gradiantoNatureGreen() }
        m4i15.addActionListener { gray() }
        m4i16.addActionListener { gruvboxDarkHard() }
        m4i17.addActionListener { gruvboxDarkMedium() }
        m4i18.addActionListener { gruvboxDarkSoft() }
        m4i19.addActionListener { hiberbeeDark() }
        m4i20.addActionListener { highContrast() }
        m4i21.addActionListener { lightFlat() }
        m4i22.addActionListener { materialDesignDark() }
        m4i23.addActionListener { monocai() }
        m4i24.addActionListener { monokaiPro() }
        m4i25.addActionListener { nord() }
        m4i26.addActionListener { oneDark() }
        m4i27.addActionListener { solarizedDark() }
        m4i28.addActionListener { solarizedLight() }
        m4i29.addActionListener { spacegray() }
        m4i30.addActionListener { vuesion() }
        m4i31.addActionListener { xcodeDark() }
        m4i32.addActionListener { light() }
        m4i33.addActionListener { dark() }

        menuBar.add(menuOne)
        menuBar.add(menuTwo)
        menuBar.add(menuThree)
        menuBar.add(menuFour)
    }

    private fun changeTheme(theme: FlatLaf) {
        try {
            FlatAnimatedLafChange.showSnapshot()
            UIManager.setLookAndFeel(theme)
            updateLarsonConfig()
            FlatDarculaLaf.updateUI()
            FlatAnimatedLafChange.hideSnapshotWithAnimation()
        } catch (ex: UnsupportedLookAndFeelException) {
            throw RuntimeException(ex)
        }
    }

    private fun updateLarsonConfig() {
        val backgroundColour = UIManager.getColor("Panel.background")
        guiProps.busyConfig.eyeBackgroundColour = backgroundColour
        guiProps.busyConfig.scannerBackgroundColour = backgroundColour
        guiProps.idleConfig.eyeBackgroundColour = backgroundColour
        guiProps.idleConfig.scannerBackgroundColour = backgroundColour
        val config = larsonScanner.currentConfig
        config.eyeBackgroundColour = backgroundColour
        config.scannerBackgroundColour = backgroundColour
        config.eyeColours = arrayOf(
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor"),
            UIManager.getColor("TabbedPane.focusColor")
        )
        larsonScanner.loadConfig(config)
    }

    private fun arc() {
        changeTheme(FlatArcIJTheme())
    }

    private fun arcOrange() {
        changeTheme(FlatArcOrangeIJTheme())
    }

    private fun arcDark() {
        changeTheme(FlatArcDarkIJTheme())
    }

    private fun arcDarkOrange() {
        changeTheme(FlatArcDarkOrangeIJTheme())
    }

    private fun carbon() {
        changeTheme(FlatCarbonIJTheme())
    }

    private fun cobalt2() {
        changeTheme(FlatCobalt2IJTheme())
    }

    private fun cyanLight() {
        changeTheme(FlatCyanLightIJTheme())
    }

    private fun darkFlat() {
        changeTheme(FlatDarkFlatIJTheme())
    }

    private fun darkPurple() {
        changeTheme(FlatDarkPurpleIJTheme())
    }

    private fun dracula() {
        changeTheme(FlatDraculaIJTheme())
    }

    private fun gradiantoDarkFuchsia() {
        changeTheme(FlatGradiantoDarkFuchsiaIJTheme())
    }

    private fun gradiantoDeepOcean() {
        changeTheme(FlatGradiantoDeepOceanIJTheme())
    }

    private fun gradiantoMignightBlue() {
        changeTheme(FlatGradiantoMidnightBlueIJTheme())
    }

    private fun gradiantoNatureGreen() {
        changeTheme(FlatGradiantoNatureGreenIJTheme())
    }

    private fun gray() {
        changeTheme(FlatGrayIJTheme())
    }

    private fun gruvboxDarkHard() {
        changeTheme(FlatGruvboxDarkHardIJTheme())
    }

    private fun gruvboxDarkMedium() {
        changeTheme(FlatGruvboxDarkMediumIJTheme())
    }

    private fun gruvboxDarkSoft() {
        changeTheme(FlatGruvboxDarkSoftIJTheme())
    }

    private fun hiberbeeDark() {
        changeTheme(FlatHiberbeeDarkIJTheme())
    }

    private fun highContrast() {
        changeTheme(FlatHighContrastIJTheme())
    }

    private fun lightFlat() {
        changeTheme(FlatLightFlatIJTheme())
    }

    private fun materialDesignDark() {
        changeTheme(FlatMaterialDesignDarkIJTheme())
    }

    private fun monocai() {
        changeTheme(FlatMonocaiIJTheme())
    }

    private fun monokaiPro() {
        changeTheme(FlatMonokaiProIJTheme())
    }

    private fun nord() {
        changeTheme(FlatNordIJTheme())
    }

    private fun oneDark() {
        changeTheme(FlatOneDarkIJTheme())
    }

    private fun solarizedDark() {
        changeTheme(FlatSolarizedDarkIJTheme())
    }

    private fun solarizedLight() {
        changeTheme(FlatSolarizedLightIJTheme())
    }

    private fun spacegray() {
        changeTheme(FlatSpacegrayIJTheme())
    }

    private fun vuesion() {
        changeTheme(FlatVuesionIJTheme())
    }

    private fun xcodeDark() {
        changeTheme(FlatXcodeDarkIJTheme())
    }

    private fun light() {
        changeTheme(FlatLightLaf())
    }

    private fun dark() {
        changeTheme(FlatDarkLaf())
    }
}