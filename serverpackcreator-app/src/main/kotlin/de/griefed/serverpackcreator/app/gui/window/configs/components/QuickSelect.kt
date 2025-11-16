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
import java.awt.event.ActionListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

/**
 * Quick selection combobox allowing a user to set either the properties or icon from a choice of acquired files in the
 * respective directories.
 *
 * @author Griefed
 */
class QuickSelect(content: List<String>, actionListener: ActionListener) : JComboBox<String>() {
    init {
        val choose = DefaultComboBoxModel(arrayOf(Translations.createserverpack_gui_quickselect_choose.toString()))
        choose.addAll(content)
        model = choose
        this.addActionListener(actionListener)
    }
}