/* Copyright (C) 2024  Griefed
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
import kotlinx.coroutines.*
import java.awt.event.ActionListener
import java.io.File
import javax.swing.Timer

/**
 * Timer responsible for starting configuration checks and comparisons.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class ConfigCheckTimer(delay: Int, guiProps: GuiProps, tabbedConfigsTab: TabbedConfigsTab) : Timer(delay,
    ActionListener {
        GlobalScope.launch(guiProps.configDispatcher, CoroutineStart.UNDISPATCHED) {
            var errorsEncountered = false
            tabbedConfigsTab.allTabs.parallelStream().forEach { component ->
                val errors = mutableListOf<String>()
                val editor = component as ConfigEditor
                runBlocking {
                    launch {
                        errors.addAll(editor.validateModpackDir())
                        editor.title.title = File(editor.getModpackDirectory()).name
                    }
                    launch {
                        errors.addAll(editor.validateSuffix())
                    }
                    launch {
                        errors.addAll(editor.validateExclusions())
                    }
                    launch {
                        errors.addAll(editor.validateWhitelist())
                    }
                    launch {
                        errors.addAll(editor.validateInclusions())
                    }
                    launch {
                        try {
                            errors.addAll(editor.validateServerIcon())
                        } catch (_: OutOfMemoryError) {}
                    }
                    launch {
                        errors.addAll(editor.validateServerProperties())
                    }
                    launch {
                        if (!editor.checkServer()) {
                            errors.add(
                                Gui.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                                    editor.getMinecraftVersion(),
                                    editor.getModloader(),
                                    editor.getModloaderVersion()
                                )
                            )
                        }
                    }
                    launch {
                        if (editor.getModloaderVersion() == Gui.createserverpack_gui_createserverpack_forge_none.toString()) {
                            errors.add(
                                Gui.configuration_log_error_minecraft_modloader(
                                    editor.getMinecraftVersion(),
                                    editor.getModloader()
                                )
                            )
                        }
                    }
                    launch {
                        editor.compareSettings()
                    }
                }
                if (errors.isEmpty()) {
                    editor.title.hideErrorIcon()
                } else {
                    editor.title.setAndShowErrorIcon("<html>${errors.joinToString("<br>")}</html>")
                    errorsEncountered = true
                }
            }
            if (tabbedConfigsTab.allTabs.any { component -> (component as ConfigEditor).hasUnsavedChanges() }) {
                tabbedConfigsTab.title.showWarningIcon()
            } else {
                tabbedConfigsTab.title.hideWarningIcon()
            }
            if (errorsEncountered) {
                tabbedConfigsTab.title.setAndShowErrorIcon(Gui.createserverpack_gui_tabs_errors.toString())
            } else {
                tabbedConfigsTab.title.hideErrorIcon()
            }
            errorsEncountered = false
        }
    }) {
    init {
        stop()
        isRepeats = false
    }
}