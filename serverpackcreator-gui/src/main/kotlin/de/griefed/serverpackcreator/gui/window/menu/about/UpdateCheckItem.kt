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
package de.griefed.serverpackcreator.gui.window.menu.about

import Translations
import de.griefed.serverpackcreator.gui.components.BalloonTipButton
import de.griefed.serverpackcreator.gui.window.UpdateDialogs
import java.awt.event.ActionListener
import javax.swing.JMenuItem

/**
 * Menu item for checking for any available updates and displaying an informational dialog to the user about whether an
 * update is available.
 *
 * @author Griefed
 */
class UpdateCheckItem(private val updateDialogs: UpdateDialogs, private val updateButton: BalloonTipButton) :
    JMenuItem(Translations.menubar_gui_menuitem_updates.toString()) {
    init {
        val listener = ActionListener { updateButton.isVisible = updateDialogs.checkForUpdate() }
        updateButton.addActionListener(listener)
        this.addActionListener(listener)
    }
}