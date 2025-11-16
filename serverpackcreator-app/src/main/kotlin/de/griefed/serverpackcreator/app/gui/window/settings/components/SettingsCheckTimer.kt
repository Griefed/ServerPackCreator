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
package de.griefed.serverpackcreator.app.gui.window.settings.components

import Translations
import de.griefed.serverpackcreator.app.gui.GuiProps
import de.griefed.serverpackcreator.app.gui.window.settings.SettingsEditorsTab
import kotlinx.coroutines.*
import java.awt.event.ActionListener
import javax.swing.Timer

/**
 * Timer responsible for starting configuration checks and comparisons.
 *
 * @author Griefed
 */
@OptIn(DelicateCoroutinesApi::class)
class SettingsCheckTimer(
    delay: Int,
    settingsEditor: SettingsEditorsTab,
    guiProps: GuiProps
) : Timer(delay,
    ActionListener {
        GlobalScope.launch(guiProps.configDispatcher, CoroutineStart.UNDISPATCHED) {
            val errors = mutableListOf<String>()
            runBlocking {
                launch {
                    for (editor in settingsEditor.allTabs) {
                        errors.addAll((editor as Editor).validateSettings())
                    }
                }
            }
            if (errors.isEmpty()) {
                settingsEditor.title.hideErrorIcon()
            } else {
                settingsEditor.title.setAndShowErrorIcon(Translations.settings_check_errors.toString())
            }
            settingsEditor.settingsHandling.checkAll()
        }
    }) {
    init {
        stop()
        isRepeats = false
    }
}