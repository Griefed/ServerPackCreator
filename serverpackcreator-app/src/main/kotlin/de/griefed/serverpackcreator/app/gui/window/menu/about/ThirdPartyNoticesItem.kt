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
package de.griefed.serverpackcreator.app.gui.window.menu.about

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.DialogUtilities
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextPane

/**
 * Menu item to display the license report of used dependencies in ServerPackCreator.
 *
 * @author Griefed
 */
class ThirdPartyNoticesItem(private val mainFrame: MainFrame, private val guiProps: GuiProps) :
    JMenuItem(Translations.menubar_gui_menuitem_licensereport.toString()) {
    private val thirdPartyNoticesWindowTextPane: JTextPane = JTextPane()
    private val thirdPartyNoticesScrollPane = JScrollPane(
        thirdPartyNoticesWindowTextPane,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )

    init {
        thirdPartyNoticesWindowTextPane.isEditable = false
        thirdPartyNoticesWindowTextPane.text =
            this.javaClass.classLoader.getResource("de/griefed/resources/gui/LICENSE-AGREEMENT")?.readText()
                ?: "Could not read resource. Please report this at https://github.com/Griefed/ServerPackCreator/issues/new?assignees=Griefed&labels=bug&projects=&template=bug-report.yml&title=%5BBug%5D%3A+"
        this.addActionListener { displayThirdPartyNotices() }

    }

    /**
     * @author Griefed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun displayThirdPartyNotices() {
        GlobalScope.launch(Dispatchers.Swing) {
            DialogUtilities.createDialog(
                thirdPartyNoticesScrollPane,
                Translations.menubar_gui_menuitem_licensereport.toString(),
                mainFrame.frame,
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                guiProps.infoIcon,
                resizable = false, display = true,
                options = null, initialValue = null,
                width = 800, height = 600
            )
        }
    }
}