package de.griefed.serverpackcreator.gui.window.settings

import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.settings.components.Editor

class GuiSettings(guiProps: GuiProps) : Editor("Gui", guiProps) {
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
        HasteBin-server URL for pastes
        de.griefed.serverpackcreator.configuration.hastebinserver
     */

    /*TODO
        Font size

     */

    /*TODO
        Focus request on SPC start

     */

    /*TODO
        Focus request on server pack generation

     */
}