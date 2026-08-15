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
package de.griefed.serverpackcreator.api.modscanning

import java.io.File
import java.io.IOException

/**
 * A jar does not contain the descriptor a scanner reads.
 *
 * **This is a normal outcome, not a failure.** Every scanner is handed the whole mods-directory, so a
 * Fabric jar reaching the Forge scanner — or any jar reaching the Quilt scan of a Quilt pack, which
 * deliberately runs both scanners over everything — simply has nothing for that scanner to read. It
 * is a distinct type precisely so [DescriptorScanner] can tell it apart from a genuine failure and
 * log it at DEBUG instead of shouting about a mod nobody needs to fix.
 *
 * Extends [IOException] so the `@Throws` contract of the descriptor readers is unchanged and an
 * existing `catch (IOException)` keeps working.
 *
 * @param descriptor Path of the descriptor that was looked for, e.g. `META-INF/mods.toml`.
 * @param jar        The jar it was looked for in.
 */
class MissingDescriptorException(val descriptor: String, val jar: File) :
    IOException("${jar.name} contains no $descriptor.")
