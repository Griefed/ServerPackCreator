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
package de.griefed.serverpackcreator.gui.window.logs

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.gui.components.TabPanel

/**
 * Tab for viewing the three logs ServerPackCreator creates, all in their own tab.
 *
 * @author Griefed
 */
class TabbedLogsTab(apiProperties: ApiProperties) : TabPanel() {
    init {
        tabs.addTab("ServerPackCreatorLog", ServerPackCreatorLog(apiProperties))
        tabs.addTab("ModloaderInstallerLog", ModloaderInstallerLog(apiProperties))
        tabs.addTab("PluginsLog", PluginsLog(apiProperties))
    }
}