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
package de.griefed.serverpackcreator.app.gui.window.settings.components

import de.griefed.serverpackcreator.app.gui.GuiProps
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel
import javax.swing.JScrollPane

/**
 * @author Griefed
 */
abstract class Editor(name: String, guiProps : GuiProps) : JScrollPane(), SettingsEditor {
    val title = SettingsTitle(guiProps)
    val panel = JPanel(
        MigLayout(
            "left,wrap",
            "[left,::64]5[left]5[left,grow,100:200:]5[left,::64]5[left,::64]5[left,::64]",
            "30"
        )
    )

    init {
        title.title = name
        viewport.view = panel
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBar.unitIncrement = 10
    }
}