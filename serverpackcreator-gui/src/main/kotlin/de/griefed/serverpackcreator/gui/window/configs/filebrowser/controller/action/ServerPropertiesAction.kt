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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller.action

import Gui
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction

/**
 * Action to set the server.properties in the currently active config-tab to the in the filebrowser selected properties.
 *
 * @author Griefed
 */
class ServerPropertiesAction(private val tabbedConfigsTab: TabbedConfigsTab) : AbstractAction() {
    private var properties: File? = null

    init {
        putValue(NAME, Gui.filebrowser_action_properties.toString())
    }

    override fun actionPerformed(e: ActionEvent) {
        tabbedConfigsTab.selectedEditor?.setServerPropertiesPath(properties!!.absolutePath)
    }

    /**
     * @author Griefed
     */
    fun setProperties(file: File?) {
        properties = file
    }
}