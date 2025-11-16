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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.components.TabTitle
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu

/**
 * Title of a config editor tab in the main GUI, managing the close operation of a selected tab and displays icons
 * when a given config contains errors or unsaved changes.
 *
 * @author Griefed
 */
class ConfigEditorTitle(
    private val guiProps: GuiProps,
    private val tabbedConfigsTab: TabbedConfigsTab,
    private val configEditor: ConfigEditor
) : TabTitle(guiProps) {

    val closeButton = JButton(guiProps.closeIcon)
    private val editorTitleMouseAdapter = object : MouseAdapter() {
        val closeSaveAndSaveAsMenu = JPopupMenu()
        val closeTabItem = JMenuItem(Translations.createserverpack_gui_close_title.toString())
        val saveTabItem = JMenuItem(Translations.menubar_gui_menuitem_saveconfig.toString())
        val saveTabAsItem = JMenuItem(Translations.menubar_gui_menuitem_saveas.toString())

        init {
            closeTabItem.addActionListener { close() }
            saveTabItem.addActionListener { configEditor.saveCurrentConfiguration() }
            saveTabAsItem.addActionListener { tabbedConfigsTab.saveAs(configEditor) }
            closeSaveAndSaveAsMenu.add(closeTabItem)
            closeSaveAndSaveAsMenu.add(saveTabItem)
            closeSaveAndSaveAsMenu.add(saveTabAsItem)
        }

        override fun mouseClicked(e: MouseEvent) {
            super.mouseClicked(e)
            openPopup(e)
        }

        override fun mousePressed(e: MouseEvent) {
            super.mousePressed(e)
            openPopup(e)
        }

        override fun mouseReleased(e: MouseEvent) {
            super.mouseReleased(e)
            openPopup(e)
        }
        fun openPopup(e: MouseEvent) {
            if (e.button == MouseEvent.BUTTON3) {
                closeSaveAndSaveAsMenu.show(this@ConfigEditorTitle,e.x,e.y)
            } else {
                tabbedConfigsTab.tabs.selectedComponent = configEditor
            }
        }
    }

    init {
        addMouseListener(editorTitleMouseAdapter)
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                close()
            }
        })
        closeButton.isVisible = false
        add(closeButton)
    }

    /**
     * Close this tab. If this config has unsaved changes, ask whether this config should be saved first.
     *
     * @author Griefed
     */
    fun close() {
        if (hasUnsavedChanges) {
            tabbedConfigsTab.tabs.selectedComponent = configEditor
            if (DialogUtilities.createShowGet(
                    Translations.createserverpack_gui_close_unsaved_message(title),
                    Translations.createserverpack_gui_close_unsaved_title(title),
                    tabbedConfigsTab.panel.parent,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION,
                    guiProps.warningIcon
                ) == 0
            ) {
                configEditor.saveCurrentConfiguration()
            }
        }
        val currentTab = tabbedConfigsTab.tabs.selectedIndex
        tabbedConfigsTab.tabs.remove(configEditor)

        if (tabbedConfigsTab.tabs.tabCount != 0) {
            tabbedConfigsTab.tabs.selectedIndex = currentTab - 1
        } else {
            tabbedConfigsTab.addTab()
        }
        tabbedConfigsTab.checkAll()
    }
}