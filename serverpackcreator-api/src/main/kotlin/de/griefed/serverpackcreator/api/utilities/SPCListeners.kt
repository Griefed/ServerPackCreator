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
package de.griefed.serverpackcreator.api.utilities

import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import java.nio.file.Path

/**
 * Listener for an event that carries no payload: a plugin is told *that* something happened, not what. Registered
 * through `ConfigurationHandler.addEventListener` / `ServerPackHandler.addEventListener`; reach for one of the
 * typed listeners below when the details are what you need.
 *
 * @author Griefed
 */
interface SPCGenericListener {
    /** Called when the event fires. Carries no payload — use a typed listener when the details matter. */
    fun run()
}

/**
 * Config check-listener executed during the checking of a given server pack configuration.
 *
 * @author Griefed
 */
interface SPCConfigCheckListener {

    /**
     * Called once a configuration has been checked, with [configCheck] holding whatever the validators
     * found. The outcome is already decided by the time this fires — to add a check of your own, use
     * [de.griefed.serverpackcreator.api.plugins.configurationhandler.ConfigCheckExtension].
     */
    fun run(packConfig: PackConfig, configCheck: ConfigCheck = ConfigCheck())
}

/**
 * Pre-Server Pack listener executed before a server pack is generated.
 *
 * @author Griefed
 */
interface SPCPreServerPackGenerationListener {

    /**
     * Called before any file is gathered. [serverPackPath] is the directory the pack will be built in;
     * it exists, but it is only empty on a first run — regenerating over a previous pack leaves that
     * pack's files in place until the copy overwrites them.
     */
    fun run(packConfig: PackConfig, serverPackPath: Path)
}

/**
 * Pre ZIP-archive listener executed before a server pack ZIP-archive is created.
 * Whether a ZIP-archive for a server pack is actually generated has no effect on whether this listener gets fired.
 *
 * @author Griefed
 */
interface SPCPreServerPackZipListener {

    /**
     * Called once every file is in [serverPackPath] and before the archive is written, so anything added
     * here still lands inside the ZIP. Fires whether or not an archive is actually configured.
     */
    fun run(packConfig: PackConfig, serverPackPath: Path)
}

/**
 * Post Generation listener executed after the server pack was generated.
 *
 * @author Griefed
 */
interface SPCPostGenListener {

    /**
     * Called once the pack in [serverPackPath] is finished. The ZIP, if one was configured, is already
     * written — anything added here is in the directory but not in the archive.
     */
    fun run(packConfig: PackConfig, serverPackPath: Path)
}