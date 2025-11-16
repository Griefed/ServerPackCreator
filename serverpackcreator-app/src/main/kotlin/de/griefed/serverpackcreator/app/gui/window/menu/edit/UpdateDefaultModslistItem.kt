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
package de.griefed.serverpackcreator.app.gui.window.menu.edit

import Translations
import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.gui.GuiProps
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JOptionPane

/**
 * Update the fallback modslist used to populate fresh configurations or when the user resets the configuration.
 *
 * @author Griefed
 */
class UpdateDefaultModslistItem(
    private val apiProperties: ApiProperties,
    private val mainFrame: JFrame,
    private val guiProps: GuiProps
) : JMenuItem(Translations.menubar_gui_menuitem_updatefallback.toString()) {
    init {
        this.addActionListener { updateFallbacks() }
    }

    /**
     * @author Griefed
     */
    private fun updateFallbacks() {
        if (apiProperties.updateFallback()) {
            showFallbacksUpdatedMessage()
        } else {
            JOptionPane.showMessageDialog(
                mainFrame,
                Translations.menubar_gui_menuitem_updatefallback_nochange.toString(),
                Translations.menubar_gui_menuitem_updatefallback_title.toString(),
                JOptionPane.INFORMATION_MESSAGE,
                guiProps.infoIcon
            )
        }
    }

    /**
     * @author Griefed
     */
    fun showFallbacksUpdatedMessage() {
        JOptionPane.showMessageDialog(
            mainFrame,
            Translations.menubar_gui_menuitem_updatefallback_updated.toString(),
            Translations.menubar_gui_menuitem_updatefallback_title.toString(),
            JOptionPane.INFORMATION_MESSAGE,
            guiProps.infoIcon
        )
    }
}