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

import Gui
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.GuiProps
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Update the fallback modslist used to populate fresh configurations or when the user resets the configuration.
 *
 * @author Griefed
 */
class UpdateFallbackModslistItem(
    private val apiProperties: ApiProperties,
    private val mainFrame: JFrame,
    private val guiProps: GuiProps
) : JMenuItem(Gui.menubar_gui_menuitem_updatefallback.toString()) {
    init {
        addActionListener { updateFallbacks() }
    }
    private fun updateFallbacks() {
        if (apiProperties.updateFallback()) {
            JOptionPane.showMessageDialog(
                mainFrame,
                Gui.menubar_gui_menuitem_updatefallback_updated.toString(),
                Gui.menubar_gui_menuitem_updatefallback_title.toString(),
                JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon
            )
        } else {
            JOptionPane.showMessageDialog(
                mainFrame,
                Gui.menubar_gui_menuitem_updatefallback_nochange.toString(),
                Gui.menubar_gui_menuitem_updatefallback_title.toString(),
                JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon
            )
        }
    }
}