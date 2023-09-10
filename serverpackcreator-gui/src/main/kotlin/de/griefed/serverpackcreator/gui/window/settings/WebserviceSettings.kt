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
package de.griefed.serverpackcreator.gui.window.settings

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.settings.components.Editor

class WebserviceSettings(guiProps: GuiProps) : Editor("Webservice", guiProps) {


    override fun loadSettings() {
        TODO("Not yet implemented")
    }

    override fun saveSettings() {
        TODO("Not yet implemented")
    }

    /*TODO
        Artemis-Data
        spring.artemis.embedded.data-directory
     */

    /*TODO
        Artemis Max Disk Usage
        de.griefed.serverpackcreator.spring.artemis.queue.max_disk_usage
     */

    /*TODO
        Webservice Database
        spring.datasource.url
     */

    /*TODO
        Webservice Cleanup Schedule
        de.griefed.serverpackcreator.spring.schedules.database.cleanup
     */

    /*TODO
        Webservice Accesslog Directory
        server.tomcat.accesslog.directory
     */

    /*TODO
        Webservice-Server Base-Directory
        server.tomcat.basedir
     */

    /*TODO
        Webservice Version Refreshing
        de.griefed.serverpackcreator.spring.schedules.versions.refresh
     */

    /*TODO
        Webservice database cleanups
        de.griefed.serverpackcreator.spring.schedules.files.cleanup
     */
}