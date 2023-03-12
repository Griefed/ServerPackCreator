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
package de.griefed.serverpackcreator.gui.window.menu.file

import de.griefed.serverpackcreator.gui.window.MainFrame
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JMenuItem

/**
 * Menu item to trigger a window closing event of ServerPackCreators main window, resulting in ServerPackCreator exiting.
 *
 * @author Griefed
 */
class ExitItem(private val mainFrame: MainFrame): JMenuItem(Gui.menubar_gui_menuitem_exit.toString()) {
    init {
        addActionListener { mainFrame.closeAndExit() }
    }
}