package de.griefed.serverpackcreator.gui.window.settings

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.settings.components.Editor

class WebserviceSettings(guiProps: GuiProps) : Editor("Webservice", guiProps) {
    override fun getSettings(): HashMap<String, Any> {
        TODO("Not yet implemented")
    }

    override fun loadSettings(settings: HashMap<String, Any>) {
        TODO("Not yet implemented")
    }

    override fun restoreSettings() {
        TODO("Not yet implemented")
    }

    override fun loadDefaults() {
        TODO("Not yet implemented")
    }

    override fun importSettings() {
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