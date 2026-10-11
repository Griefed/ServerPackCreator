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
package de.griefed.serverpackcreator.app

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.settings.PathsConfig
import java.util.prefs.Preferences

/**
 * The one place in the app that reads or writes ServerPackCreator's stored home directory.
 *
 * The home directory lives in a `Preferences` node, and **which** node is not fixed: `-api` resolves it through
 * [ApiProperties.resolvePreferencesNode], so a host that must not share state with other ServerPackCreator processes
 * (the grinder daemon, each module's test suite) can claim its own. **Never hard-code the node name or the key**:
 * the app would then write its home somewhere `-api` never reads it back from, which is the class of mismatch that
 * lets one process relocate another's home.
 *
 * **`GuiProps` deliberately stays on the default node** and does not go through here. Window geometry and layout belong
 * to the *installation* a user sees, not to whichever process resolved a home directory, and moving it would reset
 * every existing user's saved layout for no benefit.
 *
 * @author Griefed
 */
internal object HomeDirectoryPreference {

    /** The `Preferences` node `-api` will read the home directory from for this process. */
    private fun node(): Preferences = Preferences.userRoot().node(ApiProperties.resolvePreferencesNode())

    /** The stored home-directory path, or `null` when this installation has never had one set. */
    fun stored(): String? = node().get(PathsConfig.HOME_DIRECTORY_KEY, null)

    /** Store [path] as the home directory, where `-api` will find it on the next start. */
    fun store(path: String) {
        node().put(PathsConfig.HOME_DIRECTORY_KEY, path)
    }
}
