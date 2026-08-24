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
package de.griefed.serverpackcreator.app.gui.window.settings.components

/**
 * @author Griefed
 */
interface SettingsEditor {
    /** Fill the widgets from the stored settings, discarding whatever the user had typed. */
    fun loadSettings()
    /** Write the widgets into the stored settings. The caller re-loads afterwards; see the interface doc for why. */
    fun saveSettings()
    /** Problems with what the widgets currently hold, as messages to show. Empty means the panel is valid. */
    fun validateSettings(): List<String>
    /** Whether the widgets differ from what is stored — compared against the *normalised* getters, hence the reload rule. */
    fun hasUnsavedChanges(): Boolean
}