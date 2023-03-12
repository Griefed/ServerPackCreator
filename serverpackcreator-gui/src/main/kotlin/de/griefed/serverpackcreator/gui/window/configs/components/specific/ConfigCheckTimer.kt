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
package de.griefed.serverpackcreator.gui.window.configs.components.specific

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditorPanel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.event.ActionListener
import javax.swing.Timer

/**
 * Timer responsible for starting configuration checks and comparisons.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class ConfigCheckTimer(delay: Int, configEditorPanel: ConfigEditorPanel, guiProps: GuiProps) : Timer(delay,
    ActionListener {
        GlobalScope.launch(guiProps.configDispatcher) {
            val errors = mutableListOf<String>()
            runBlocking {
                launch {
                    errors.addAll(configEditorPanel.validateModpackDir())
                }
                launch {
                    errors.addAll(configEditorPanel.validateSuffix())
                }
                launch {
                    errors.addAll(configEditorPanel.validateExclusions())
                }
                launch {
                    errors.addAll(configEditorPanel.validateServerPackFiles())
                }
                launch {
                    errors.addAll(configEditorPanel.validateServerIcon())
                }
                launch {
                    errors.addAll(configEditorPanel.validateServerProperties())
                }
                launch {
                    if (!configEditorPanel.checkServer()) {
                        errors.add(
                            Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                                configEditorPanel.getMinecraftVersion(),
                                configEditorPanel.getModloader(),
                                configEditorPanel.getModloaderVersion()
                            )
                        )
                    }
                }
                launch {
                    if (configEditorPanel.getModloaderVersion() == Gui.createserverpack_gui_createserverpack_forge_none.toString()) {
                        errors.add(
                            Gui.configuration_log_error_minecraft_modloader(
                                configEditorPanel.getMinecraftVersion(),
                                configEditorPanel.getModloader()
                            )
                        )
                    }
                }
                launch {
                    configEditorPanel.compareSettings()
                }
            }
            if (errors.isEmpty()) {
                configEditorPanel.title.hideErrorIcon()
            } else {
                configEditorPanel.title.setAndShowErrorIcon("<html>${errors.joinToString("<br>")}</html>")
            }
        }
    }) {
    init {
        stop()
        isRepeats = false
    }
}