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
import de.griefed.serverpackcreator.app.gui.tips.TipOfTheDayManager
import de.griefed.serverpackcreator.app.gui.window.MainFrame
import javax.swing.JMenuItem

/**
 * Menuitem to display the tip of the day.
 *
 * @author Griefed
 */
class TipOfTheDayItem(guiProps: GuiProps, mainFrame: MainFrame): JMenuItem(Translations.menubar_gui_menuitem_tipoftheday.toString()) {

    private val tipManager = TipOfTheDayManager(mainFrame.frame, guiProps)

    init {
        this.addActionListener { showTip() }
    }

    /**
     * @author Griefed
     */
    fun showTip() {
        tipManager.showTipOfTheDay()
    }
}