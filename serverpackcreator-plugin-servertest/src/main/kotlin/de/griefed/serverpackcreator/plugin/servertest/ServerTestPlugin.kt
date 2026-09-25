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
package de.griefed.serverpackcreator.plugin.servertest

import de.griefed.serverpackcreator.api.plugins.PluginContext
import de.griefed.serverpackcreator.api.plugins.ServerPackCreatorPlugin

/**
 * Lets a generated server pack be launched and driven from inside ServerPackCreator.
 *
 * It exists because testing a pack meant leaving the application: find the directory, open a terminal,
 * `bash start.sh`, answer the EULA, wait, then start a client. The tab this plugin contributes lists every
 * pack in the server-packs directory, runs the selected one **through its own start script** — never a
 * hand-built java command, so the scripts are under test too — and hands the user that server's console
 * together with an input line into its stdin.
 *
 * The class itself carries no state. Its identity, name and version come from the `plugin.toml` the
 * superclass reads out of this jar, and the running servers are owned by the tab's session registry.
 *
 * @param context Plugin context provided by ServerPackCreator.
 * @author Griefed
 */
@Suppress("unused")
class ServerTestPlugin(context: PluginContext) : ServerPackCreatorPlugin(context)
