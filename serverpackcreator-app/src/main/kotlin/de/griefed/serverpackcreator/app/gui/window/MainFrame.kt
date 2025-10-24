/* Copyright (C) 2024  Griefed
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
package de.griefed.serverpackcreator.app.gui.window

import Translations
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.themes.ThemeManager
import de.griefed.serverpackcreator.app.gui.window.menu.MainMenuBar
import de.griefed.serverpackcreator.app.updater.MigrationManager
import de.griefed.serverpackcreator.app.updater.UpdateChecker
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.WindowConstants

/**
 * Main Frame of ServerPackCreator, housing [MainPanel], [MainMenuBar].
 *
 * @author Griefed
 */
class MainFrame(
    guiProps: GuiProps,
    apiWrapper: ApiWrapper,
    updateChecker: UpdateChecker,
    migrationManager: MigrationManager,
    themeManager: ThemeManager
) {
    val frame: JFrame = JFrame("${Translations.createserverpack_gui_createandshowgui} ${apiWrapper.apiProperties.apiVersion}")
    val mainPanel = MainPanel(guiProps, apiWrapper, guiProps.larsonScanner, this, themeManager)
    private val updateDialogs: UpdateDialogs = UpdateDialogs(
        guiProps, apiWrapper.utilities.webUtilities,
        apiWrapper.apiProperties, updateChecker, frame
    )
    private val menuBar: MainMenuBar = MainMenuBar(
        guiProps, apiWrapper, updateDialogs,
        this, migrationManager, themeManager
    )

    init {
        frame.jMenuBar = menuBar.menuBar
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                mainPanel.closeAndExit()
            }
        })
        frame.iconImage = guiProps.appIcon
        frame.contentPane = mainPanel.panel
        frame.isLocationByPlatform = true
        frame.preferredSize = Dimension(1100, 860)
        frame.pack()
        guiProps.initFont()
        guiProps.larsonScanner.loadConfig(guiProps.idleConfig)
        guiProps.larsonScanner.play()
        KeyComboManager(frame, mainPanel)
    }

    /**
     * @author Griefed
     */
    fun show() {
        frame.isVisible = true
    }

    /**
     * @author Griefed
     */
    fun displayMigrationMessages() {
        menuBar.displayMigrationMessages()
    }

    /**
     * @author Griefed
     */
    fun showFallbacksUpdatedMessage() {
        menuBar.showFallbacksUpdatedMessage()
    }

    /**
     * @author Griefed
     */
    fun showTip() {
        menuBar.showTip()
    }

    fun showRestartNotice() {
        JOptionPane.showMessageDialog(
            frame,
            Translations.settings_notice_restart.toString()
        )
    }

    /**
     * @author Griefed
     */
    fun toFront() {
        frame.extendedState = JFrame.ICONIFIED
        frame.isAutoRequestFocus = true
        show()
        frame.toFront()
        frame.requestFocus()
        frame.requestFocusInWindow()
        frame.extendedState = JFrame.NORMAL
        frame.repaint()
    }

    /**
     * @author Griefed
     */
    fun stepByStepGuide() {
        mainPanel.stepByStepGuide()
    }
}