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
package de.griefed.serverpackcreator.gui.window.menu.edit

import Gui
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes
import de.griefed.larsonscanner.LarsonScanner
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.MainFrame
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

/**
 * Menu to give the user the choice to switch between any and all available themes provided in [FlatAllIJThemes].
 *
 * @author Griefed
 */
class SwitchThemeMenu(
    private val guiProps: GuiProps,
    private val larsonScanner: LarsonScanner,
    private val apiProperties: ApiProperties,
    private val mainFrame: MainFrame
) : JMenu(Gui.menubar_gui_menu_theme.toString()) {
    init {
        for (theme in FlatAllIJThemes.INFOS) {
            val item = JMenuItem(theme.name)
            item.addActionListener { changeTheme(theme) }
            add(item)
        }
    }

    /**
     * Change the theme to the provided [theme], updating the whole GUI and the current [larsonScanner] configuration
     * to match.
     *
     * @author Griefed
     */
    private fun changeTheme(theme: FlatAllIJThemes.FlatIJLookAndFeelInfo) {
        try {
            val instance = Class.forName(theme.className).getDeclaredConstructor().newInstance() as FlatLaf
            FlatAnimatedLafChange.showSnapshot()
            UIManager.setLookAndFeel(instance)
            updateThemeRelatedComponents()
            FlatLaf.updateUI()
            FlatAnimatedLafChange.hideSnapshotWithAnimation()
            apiProperties.storeCustomProperty("theme", theme.className)
        } catch (ex: UnsupportedLookAndFeelException) {
            throw RuntimeException(ex)
        }
    }

    /**
     * Update the [larsonScanner] and [guiProps] configurations to match the current theme.
     *
     * @author Griefed
     */
    private fun updateThemeRelatedComponents() {
        val panelBackgroundColour = UIManager.getColor("Panel.background")
        val tabbedPaneFocusColor = UIManager.getColor("TabbedPane.focusColor")
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