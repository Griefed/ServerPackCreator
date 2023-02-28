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
package de.griefed.serverpackcreator.api

import de.griefed.serverpackcreator.api.modscanning.ModScanner
import de.griefed.serverpackcreator.api.utilities.common.Utilities
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta

/**
 * Everything revolving around creating a server pack. The intended workflow is to create a [PackConfig] and run
 * it through any of the available [ConfigurationHandler.checkConfiguration]-variants, and then call [run] with the
 * previously checked configuration model. You may run with an unchecked configuration model, but no guarantees or
 * promises, yes not even support, is given for running a model without checking it first.
 *
 * This class also gives you access to the methods which are responsible for creating the server pack, in case you want
 * to do things manually.
 *
 * If you want to execute extensions, see
 * * [ApiPlugins.runPreGenExtensions]},
 * * [ApiPlugins.runPreZipExtensions]} and
 * * [ApiPlugins.runPostGenExtensions].
 *
 * @param apiProperties Base settings of ServerPackCreator needed for server pack generation, such as access to the
 * directories, script templates and so on.
 * @param versionMeta   Meta for modloader and version specific checks and information gathering, such as modloader
 * installer downloads.
 * @param utilities     Common utilities used across ServerPackCreator.
 * @param apiPlugins    Any addons which a user may want to execute during the generation of a server pack.
 * @param modScanner    In case a user enabled automatic sideness detection, this will exclude clientside-only mods
 * from a server pack.
 *
 * @author Griefed
 */
expect class ServerPackHandler(
    apiProperties: ApiProperties,
    versionMeta: VersionMeta,
    utilities: Utilities,
    apiPlugins: ApiPlugins,
    modScanner: ModScanner
)