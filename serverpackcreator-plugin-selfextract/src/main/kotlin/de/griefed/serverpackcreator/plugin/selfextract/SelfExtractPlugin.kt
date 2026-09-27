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
package de.griefed.serverpackcreator.plugin.selfextract

import de.griefed.serverpackcreator.api.plugins.PluginContext
import de.griefed.serverpackcreator.api.plugins.ServerPackCreatorPlugin

/**
 * Wraps every generated server pack in a script that unpacks itself and starts the server.
 *
 * Distributing a server pack means sending a ZIP and then explaining what to do with it. This plugin
 * turns each generated pack into two artifacts that need no explaining: a `.bsx` a Linux or macOS user
 * runs, and a `.cmd` a Windows user double-clicks. Either one extracts the pack into
 * `mc-servers/<pack>` inside the user's home directory and starts the server.
 *
 * **It contributes no tab and reads no configuration, deliberately.** There is nothing to choose:
 * every generated pack gets both artifacts, built from the pack that was just written. Installing the
 * plugin *is* the setting, and removing it is how you turn it off — which is one fewer place for the
 * answer to live than a checkbox that can disagree with what the plugin actually does.
 *
 * The recipe is the one in `HELP.md` under *Fun Stuff → Self-extracting, self-contained script*, so a
 * user who wants to know what they were handed can read the chapter rather than this source.
 *
 * @param context Plugin context provided by ServerPackCreator.
 * @author Griefed
 */
@Suppress("unused")
class SelfExtractPlugin(context: PluginContext) : ServerPackCreatorPlugin(context)
