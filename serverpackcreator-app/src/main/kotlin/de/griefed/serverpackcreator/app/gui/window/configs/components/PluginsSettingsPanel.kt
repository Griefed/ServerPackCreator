/* Copyright (C) 2025 Griefed
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
package de.griefed.serverpackcreator.app.gui.window.configs.components

import de.griefed.serverpackcreator.api.plugins.swinggui.ExtensionConfigPanel
import de.griefed.serverpackcreator.app.gui.components.CollapsiblePanel
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel

/**
 * Collapsible panel for plugins which allow a user to customize server pack specific aspects. Each plugin which registers
 * a panel receives a separate panel from this class via use of [createCollapsiblePluginPanel].
 *
 * @author Griefed
 */
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

    /**
     * Create a collapsible panel for the provided [pluginPanel].
     *
     * @author Griefed
     */
    private fun createCollapsiblePluginPanel(pluginPanel: ExtensionConfigPanel): JPanel {
        return CollapsiblePanel(pluginPanel.extensionName, pluginPanel)
    }
}