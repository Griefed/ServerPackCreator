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
package de.griefed.serverpackcreator.api.utilities

import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.PackConfig
import java.nio.file.Path

interface SPCGenericListener {
    fun run()
}

/**
 * Config check-listener executed during the checking of a given server pack configuration.
 *
 * @author Griefed
 */
interface SPCConfigCheckListener {

    /**
     * Run the given listener with a config and a config check object.
     *
     * @param packConfig A pack config from which a server pack can be generated.
     * @param configCheck Config check object holding check results.
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
     * Run the given listener with a config and path to the server pack.
     *
     * @param packConfig A pack config from which a server pack can be generated.
     * @param serverPackPath The path to the server pack in question.
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
     * Run the given listener with a config and path to the server pack.
     *
     * @param packConfig A pack config from which a server pack can be generated.
     * @param serverPackPath The path to the server pack in question.
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
     * Run the given listener with a config and path to the server pack.
     *
     * @param packConfig A pack config from which a server pack can be generated.
     * @param serverPackPath The path to the server pack in question.
     */
    fun run(packConfig: PackConfig, serverPackPath: Path)
}