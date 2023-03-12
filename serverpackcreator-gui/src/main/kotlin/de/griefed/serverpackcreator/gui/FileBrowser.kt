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
package de.griefed.serverpackcreator.gui

import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.gui.filebrowser.model.FileBrowserModel
import de.griefed.serverpackcreator.gui.filebrowser.view.FileBrowserFrame
import de.griefed.serverpackcreator.gui.window.configs.ConfigsTab
import kotlinx.coroutines.*

/**
 * TODO docs
 */
@OptIn(DelicateCoroutinesApi::class)
class FileBrowser(private val configsTab: ConfigsTab, guiProps: GuiProps, utilities: Utilities) {
    private val browserModel: FileBrowserModel = FileBrowserModel(guiProps)
    private lateinit var frame: FileBrowserFrame

    init {
        GlobalScope.launch(guiProps.fileBrowserDispatcher) {
            frame = FileBrowserFrame(browserModel, configsTab, guiProps, utilities)
        }
    }

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