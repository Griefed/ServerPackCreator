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
package de.griefed.serverpackcreator.gui.window.configs.filebrowser

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.window.configs.filebrowser.view.FileBrowserFrame

/**
 * @author Andrew Thompson
 * @see <a href="https://codereview.stackexchange.com/questions/4446/file-browser-gui">File Browser GUI</a>
 * @license LGPL
 */
class FileBrowser(tabbedConfigsTab: TabbedConfigsTab, guiProps: GuiProps, utilities: Utilities) {
    private val browserModel: FileBrowserModel = FileBrowserModel(guiProps,utilities.fileUtilities)
    private val frame: FileBrowserFrame = FileBrowserFrame(browserModel, tabbedConfigsTab, guiProps, utilities)

    /**
     * TODO docs
     */
    fun reload() {
        browserModel.reload()
    }

    /**
     * TODO docs
     */
    fun show() {
        frame.show()
    }

    /**
     * TODO docs
     */
    fun hide() {
        frame.hide()
    }
}