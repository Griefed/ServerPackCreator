/* Copyright (C) 2026 Griefed
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

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.utilities.ComponentCoroutineScope
import de.griefed.serverpackcreator.app.gui.window.configs.ConfigEditor
import de.griefed.serverpackcreator.app.gui.window.configs.TabbedConfigsTab
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.event.ActionListener
import javax.swing.Timer
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

/**
 * Timer responsible for starting configuration checks and comparisons.
 *
 * @author Griefed
 */
class ConfigCheckTimer(delay: Int, guiProps: GuiProps, tabbedConfigsTab: TabbedConfigsTab) : Timer(delay, null) {

    /** Owns the periodic config-check coroutine; cancelled when the configs tab leaves the screen
     * (ancestor-listener in `init`). The check is idempotent, so cancel-on-tab-switch + the
     * helper's lazy re-create is harmless. */
    private val componentScope = ComponentCoroutineScope()

    /** The check, kept as a property (not passed to the Timer super-constructor) so it can launch
     * on [componentScope] — `this` is unavailable in a super-constructor argument. */
    private val checkListener = ActionListener {
        componentScope.scope().launch(guiProps.configDispatcher, CoroutineStart.UNDISPATCHED) {
            var errorsEncountered = false
            tabbedConfigsTab.allTabs.parallelStream().forEach { component ->
                val errors = mutableListOf<String>()
                val editor = component as ConfigEditor

                runBlocking {
                    launch {
                        errors.addAll(editor.validateModpackDir())
                        editor.title.title = editor.resolvePackName()
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
                        } catch (_: OutOfMemoryError) {
                            // Reading a pathologically large server-icon can exhaust the heap;
                            // swallow it so the periodic validation coroutine doesn't take down the
                            // GUI — the icon simply isn't validated on this tick.
                        }
                    }
                    launch {
                        errors.addAll(editor.validateServerProperties())
                    }
                    launch {
                        if (!editor.checkServer()) {
                            errors.add(
                                Translations.createserverpack_gui_createserverpack_checkboxserver_unavailable_title(
                                    editor.getMinecraftVersion(),
                                    editor.getModloader(),
                                    editor.getModloaderVersion()
                                )
                            )
                        }
                    }
                    launch {
                        if (editor.getModloaderVersion() == Translations.createserverpack_gui_createserverpack_forge_none.toString()) {
                            errors.add(
                                Translations.configuration_log_error_minecraft_modloader(
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
                tabbedConfigsTab.title.setAndShowErrorIcon(Translations.createserverpack_gui_tabs_errors.toString())
            } else {
                tabbedConfigsTab.title.hideErrorIcon()
            }
            errorsEncountered = false
        }
    }

    init {
        stop()
        isRepeats = false
        addActionListener(checkListener)
        tabbedConfigsTab.panel.addAncestorListener(object : AncestorListener {
            override fun ancestorRemoved(event: AncestorEvent?) {
                componentScope.cancel()
            }

            override fun ancestorAdded(event: AncestorEvent?) {}

            override fun ancestorMoved(event: AncestorEvent?) {}
        })
    }
}