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

import de.griefed.serverpackcreator.api.ApiWrapper
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.menu.MainMenuBar
import de.griefed.serverpackcreator.updater.MigrationManager
import de.griefed.serverpackcreator.updater.UpdateChecker
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.JFrame

/**
 * Common key-combinations usable across ServerPackCreator, like loading and saving configs, generating
 * a server pack, opening a new tab, closing a tab, etc. etc.
 *
 * * CTRL + W closes the current tab
 * * CTRL + T opens a new tab
 * * CTRL + S saves the current tab
 * * CTRL + LSHIFT + S saves all tabs
 * * CTRL + L opens the file selection for loading
 * * CTRL + G generates the current tab
 *
 * @author Griefed
 */
class KeyComboManager(
    private val guiProps: GuiProps,
    private val apiWrapper: ApiWrapper,
    private val updateChecker: UpdateChecker,
    private val updateDialogs: UpdateDialogs,
    private val migrationManager: MigrationManager,
    private val frame: JFrame,
    private val mainPanel: MainPanel,
    private val menu: MainMenuBar
) {

    private val configs = mainPanel.tabbedConfigsTab
    private val control = mainPanel.controlPanel

    init {
        addCloseTabCombo()
        addNewTabCombo()
        addSaveCurrentTabCombo()
        addSaveAllTabsCombo()
        addLoadConfigsCombo()
        addStartGenerationCombo()
    }

    /**
     * @author Griefed
     */
    private fun addCloseTabCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_W && event.isControlDown -> {
                        configs.selectedEditor!!.title.close()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addNewTabCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_T && event.isControlDown -> {
                        configs.addTab()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addSaveCurrentTabCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_S && event.isControlDown && !event.isShiftDown -> {
                        configs.selectedEditor!!.saveCurrentConfiguration()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addSaveAllTabsCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_S && event.isControlDown && event.isShiftDown -> {
                        configs.saveAll()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addLoadConfigsCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_L && event.isControlDown -> {
                        configs.loadConfigFile()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addStartGenerationCombo() {
        Toolkit.getDefaultToolkit().addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_G && event.isControlDown -> {
                        control.generate()
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }
}