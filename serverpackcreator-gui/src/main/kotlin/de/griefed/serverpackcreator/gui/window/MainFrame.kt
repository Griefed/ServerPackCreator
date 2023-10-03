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
package de.griefed.serverpackcreator.gui.window

import Gui
import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.menu.MainMenuBar
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
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
    migrationManager: MigrationManager
) {
    val frame: JFrame = JFrame(Gui.createserverpack_gui_createandshowgui.toString())
    val mainPanel = MainPanel(guiProps, apiWrapper, guiProps.larsonScanner,this)
    private val updateDialogs : UpdateDialogs
    private val menuBar: MainMenuBar

    init {
        updateDialogs = UpdateDialogs(
            guiProps, apiWrapper.utilities!!.webUtilities,
            apiWrapper.apiProperties, updateChecker, frame
        )
        menuBar = MainMenuBar(
            guiProps, apiWrapper, updateDialogs,
            this, migrationManager
        )
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
        KeyComboManager(guiProps,apiWrapper,updateChecker,updateDialogs,migrationManager,frame,mainPanel,menuBar)
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
    fun showTip() {
        menuBar.showTip()
    }

    /**
     * @author Griefed
     */
    fun toFront() {
        frame.isAutoRequestFocus = true
        show()
        frame.toFront()
        frame.requestFocus()
    }

    /**
     * @author Griefed
     */
    fun stepByStepGuide() {
        mainPanel.stepByStepGuide()
    }
}