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
package de.griefed.serverpackcreator.plugin.grinder

import de.griefed.serverpackcreator.api.plugins.PluginContext
import de.griefed.serverpackcreator.api.plugins.ServerPackCreatorPlugin

/**
 * Bridges a ServerPackCreator instance to a grinder daemon: the GUI tab lets you browse the grinder's
 * verdicts and tick the mods you want excluded, and the pre-generation extension folds those ticks into
 * the clientside-mod exclusion list of every server pack — GUI, CLI and web alike.
 *
 * It exists because the grinder's `/as-properties` endpoint is all-or-nothing: it publishes *every*
 * `CONFIRMED` finding into `serverpackcreator.properties` with no way to disagree with one, and says
 * nothing at all about the other three verdict classes. This plugin is the pick-and-choose alternative.
 *
 * The class itself carries no state. Its identity, name and version come from the `plugin.toml` the
 * superclass reads out of this jar, and everything that does hold state — the configured URL and the
 * ticked entries — lives in the `CommentedConfig` ServerPackCreator hands to each extension.
 *
 * @param context Plugin context provided by ServerPackCreator.
 * @author Griefed
 */
@Suppress("unused")
class GrinderPlugin(context: PluginContext) : ServerPackCreatorPlugin(context)
