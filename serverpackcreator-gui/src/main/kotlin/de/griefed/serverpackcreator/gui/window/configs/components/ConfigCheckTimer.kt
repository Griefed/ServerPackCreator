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
package de.griefed.serverpackcreator.gui.window.configs.components

import Gui
import de.griefed.serverpackcreator.gui.GuiProps
import de.griefed.serverpackcreator.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.gui.window.configs.TabbedConfigsTab
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.event.ActionListener
import java.io.File
import javax.swing.Timer

/**
 * Timer responsible for starting configuration checks and comparisons.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class ConfigCheckTimer(delay: Int, configEditor: ConfigEditor, guiProps: GuiProps, tabbedConfigsTab: TabbedConfigsTab) : Timer(delay,
    ActionListener {
        GlobalScope.launch(guiProps.configDispatcher) {
            val errors = mutableListOf<String>()
            runBlocking {
                launch {
                    errors.addAll(configEditor.validateModpackDir())
                    configEditor.editorTitle.title = File(configEditor.getModpackDirectory()).name
                }
                launch {
                    errors.addAll(configEditor.validateSuffix())
                }
                launch {
                    errors.addAll(configEditor.validateExclusions())
                }
                launch {
                    errors.addAll(configEditor.validateInclusions())
                }
                launch {
                    try {
                        errors.addAll(configEditor.validateServerIcon())
                    } catch (_: OutOfMemoryError) {}
                }
                launch {
                    errors.addAll(configEditor.validateServerProperties())
                }
                launch {
                    if (!configEditor.checkServer()) {
                        errors.add(
                            Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                                configEditor.getMinecraftVersion(),
                                configEditor.getModloader(),
                                configEditor.getModloaderVersion()
                            )
                        )
                    }
                }
                launch {
                    if (configEditor.getModloaderVersion() == Gui.createserverpack_gui_createserverpack_forge_none.toString()) {
                        errors.add(
                            Gui.configuration_log_error_minecraft_modloader(
                                configEditor.getMinecraftVersion(),
                                configEditor.getModloader()
                            )
                        )
                    }
                }
                launch {
                    configEditor.compareSettings()
                }
            }
            if (errors.isEmpty()) {
                configEditor.editorTitle.hideErrorIcon()
            } else {
                configEditor.editorTitle.setAndShowErrorIcon("<html>${errors.joinToString("<br>")}</html>")
            }
            tabbedConfigsTab.checkAll()
        }
    }) {
    init {
        stop()
        isRepeats = false
    }
}