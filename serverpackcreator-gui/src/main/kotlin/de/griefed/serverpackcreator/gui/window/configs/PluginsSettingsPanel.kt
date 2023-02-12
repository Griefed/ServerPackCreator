package de.griefed.serverpackcreator.gui.window.configs

import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.gui.window.components.CollapsiblePanel
import net.miginfocom.swing.MigLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class PluginsSettingsPanel(pluginPanels: List<ExtensionConfigPanel>) : JPanel(
    MigLayout(
        "left,wrap",
        "0[left,grow,push]0", "30"
    )
) {

    init {
        for (panel in pluginPanels.indices) {
            add(createCollapsiblePluginPanel(pluginPanels[panel]), "cell 0 $panel,grow")
        }
        isVisible = false
    }

    private fun createCollapsiblePluginPanel(pluginPanel: ExtensionConfigPanel): JPanel {
        return CollapsiblePanel(pluginPanel.extensionName,pluginPanel)
    }
}