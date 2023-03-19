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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser.view

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.controller.action.*
import java.awt.Component
import java.awt.event.MouseAdapter
import java.io.File
import javax.swing.JPopupMenu
import javax.swing.JSeparator

/**
 * TODO docs
 *
 * @author Griefed (Kotlin Conversion and minor changes)
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
open class SelectionPopMenu(tabbedConfigsTab: TabbedConfigsTab, utilities: Utilities) : MouseAdapter() {
    private val menu: JPopupMenu = JPopupMenu()
    private val directory = ModpackDirectoryAction(tabbedConfigsTab)
    private val icon = ServerIconAction(tabbedConfigsTab)
    private val properties = ServerPropertiesAction(tabbedConfigsTab)
    private val open = OpenAction(utilities)
    private val openContainingFolder = OpenContainingFolder(utilities)
    private val imageRegex = ".*\\.([Pp][Nn][Gg]|[Jj][Pp][Gg]|[Jj][Pp][Ee][Gg]|[Bb][Mm][Pp])".toRegex()
    private val props: String = "properties"

    init {
        menu.add(open)
        menu.add(openContainingFolder)
        menu.add(JSeparator())
        menu.add(directory)
        menu.add(icon)
        menu.add(properties)
    }

    /**
     * Show the context menu with visibilities of contained elements based on the file-type.
     *
     * @author Griefed
     */
    fun show(invoker: Component?, x: Int, y: Int, file: File) {
        setVisibilities(file)
        menu.show(invoker, x, y)
    }

    /**
     * @author Griefed
     */
    private fun setVisibilities(file: File) {
        open.setFile(file)
        openContainingFolder.setDirectory(file)
        if (file.isDirectory) {
            directory.setDirectory(file)
            directory.isEnabled = true
            icon.isEnabled = false
            properties.isEnabled = false
            return
        }
        if (file.isFile && file.name.lowercase().matches(imageRegex)) {
            icon.setIcon(file)
            directory.isEnabled = false
            icon.isEnabled = true
            properties.isEnabled = false
            return
        }
        if (file.isFile && file.name.lowercase().endsWith(props)) {
            properties.setProperties(file)
            directory.isEnabled = false
            icon.isEnabled = false
            properties.isEnabled = true
        }
    }
}