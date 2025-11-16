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
package de.griefed.serverpackcreator.app.gui

import Translations
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatInspector
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector
import com.formdev.flatlaf.fonts.inter.FlatInterFont
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont
import com.formdev.flatlaf.fonts.roboto_mono.FlatRobotoMonoFont
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.splash.SplashScreen
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import de.griefed.serverpackcreator.app.updater.MigrationManager
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.EventQueue
import java.awt.Frame
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.Timer

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
    private val guiProps = GuiProps(apiWrapper.apiProperties)
    private val themeManager = ThemeManager(apiWrapper, guiProps)

    init {
        EventQueue.invokeLater {

            if (apiWrapper.apiProperties.preRelease || apiWrapper.apiProperties.devBuild) {
                FlatInspector.install("ctrl shift alt X")
                FlatUIDefaultsInspector.install("ctrl shift alt Y")
            }

            FlatInterFont.install()
            FlatJetBrainsMonoFont.install()
            FlatRobotoFont.install()
            FlatRobotoMonoFont.install()
            FlatLaf.setPreferredFontFamily("Arial Unicode MS")

            themeManager.updateLookAndFeel(guiProps.theme)

            val main = MainFrame(
                guiProps,
                apiWrapper,
                updateChecker,
                migrationManager,
                themeManager
            )
            splashScreen.close()
            guiProps.font = guiProps.font
            if (guiProps.startFocusEnabled) {
                main.toFront()
            } else {
                main.show()
            }
            if (apiWrapper.firstRun) {
                GlobalScope.launch(Dispatchers.Swing, CoroutineStart.UNDISPATCHED) {
                    if (JOptionPane.showConfirmDialog(
                            main.frame,
                            Translations.firstrun_message.toString(),
                            Translations.firstrun_title.toString(),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        ) == 0
                    ) {
                        main.stepByStepGuide()
                    }
                }
            } else {
                if (migrationManager.migrationMessages.isNotEmpty()) {
                    main.displayMigrationMessages()
                }
                if (apiWrapper.apiProperties.fallbackUpdated) {
                    main.showFallbacksUpdatedMessage()
                } else if (guiProps.showTipOnStartup) {
                    main.showTip()
                }
            }

            val resizeTimer = Timer(250) {
                for (frame in Frame.getFrames()) {
                    SwingUtilities.updateComponentTreeUI(frame)
                }
                main.frame.revalidate()
                main.frame.repaint()
            }
            resizeTimer.stop()
            resizeTimer.isRepeats = false
            resizeTimer.initialDelay = 250
            resizeTimer.delay = 250
            resizeTimer.isCoalesce = true
            main.frame.addComponentListener(object : ComponentListener {
                override fun componentResized(e: ComponentEvent?) {
                    resizeTimer.restart()
                }

                override fun componentMoved(e: ComponentEvent?) {}

                override fun componentShown(e: ComponentEvent?) {}

                override fun componentHidden(e: ComponentEvent?) {}

            })
        }
    }
}