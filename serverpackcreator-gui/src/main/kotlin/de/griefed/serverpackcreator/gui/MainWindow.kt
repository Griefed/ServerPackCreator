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
package de.griefed.serverpackcreator.gui

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme
import de.griefed.serverpackcreator.api.*
import de.griefed.serverpackcreator.gui.splash.SplashScreen
import de.griefed.serverpackcreator.gui.window.MainFrame
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import javax.swing.UIManager

/**
 * Main window of ServerPackCreator housing everything needed to configure a server pack, generate it, view logs, manage
 * files etc.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class MainWindow(
    private val apiWrapper: ApiWrapper,
    private val updateChecker: UpdateChecker,
    private val splashScreen: SplashScreen,
    private val migrationManager: MigrationManager
) {

    init {
        GlobalScope.launch(Dispatchers.Swing) {
            if (apiWrapper.apiProperties.apiVersion.matches("^(.*alpha.*|.*beta.*|.*dev.*)$".toRegex())) {
                FlatInspector.install("ctrl shift alt X")
                FlatUIDefaultsInspector.install("ctrl shift alt Y")
            }

            FlatJetBrainsMonoFont.install()
            FlatLaf.setPreferredFontFamily(FlatJetBrainsMonoFont.FAMILY)

            try {
                val themeClassName = apiWrapper.apiProperties.retrieveCustomProperty("theme")
                val themeClass = Class.forName(themeClassName)
                val instance = themeClass.getDeclaredConstructor().newInstance() as FlatLaf
                UIManager.setLookAndFeel(instance)
                FlatLaf.updateUI()
            } catch (ignored: Exception) {
                FlatDarkPurpleIJTheme.setup()
            }
            MainFrame(
                apiWrapper,
                updateChecker,
                splashScreen,
                migrationManager
            )
        }
    }
}