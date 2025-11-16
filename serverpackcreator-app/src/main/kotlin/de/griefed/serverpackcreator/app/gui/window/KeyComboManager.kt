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
package de.griefed.serverpackcreator.app.gui.window

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.AWTEvent
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
    frame: JFrame,
    mainPanel: MainPanel
) {

    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val configs = mainPanel.tabbedConfigsTab
    private val control = mainPanel.controlPanel
    private val toolkit = frame.toolkit

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
        toolkit.addAWTEventListener({ e: AWTEvent ->
            val event = e as KeyEvent
            if (event.id == KeyEvent.KEY_RELEASED) {
                when {
                    event.keyCode == KeyEvent.VK_W && event.isControlDown -> {
                        try {
                            configs.selectedEditor!!.title.close()
                        } catch (_: NullPointerException) {
                            log.debug("No tab to close selected. Select tab first.")
                        }
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK)
    }

    /**
     * @author Griefed
     */
    private fun addNewTabCombo() {
        toolkit.addAWTEventListener({ e: AWTEvent ->
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
        toolkit.addAWTEventListener({ e: AWTEvent ->
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
        toolkit.addAWTEventListener({ e: AWTEvent ->
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
        toolkit.addAWTEventListener({ e: AWTEvent ->
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
        toolkit.addAWTEventListener({ e: AWTEvent ->
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