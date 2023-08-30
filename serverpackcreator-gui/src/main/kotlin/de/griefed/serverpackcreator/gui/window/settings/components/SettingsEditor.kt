package de.griefed.serverpackcreator.gui.window.settings.components

interface SettingsEditor {
    fun getSettings(): HashMap<String, Any>
    fun loadSettings(settings: HashMap<String, Any>)
    fun restoreSettings()
    fun loadDefaults()
    fun importSettings()
}