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
package de.griefed.serverpackcreator.clientside

/**
 * The mod's own version, read out of the string a platform published the file under.
 *
 * Seam only for now — it returns [ModFile.version] unchanged, so this commit changes no behaviour. What it
 * exists for is the next one: a published version routinely leads with the *Minecraft* version, and every
 * comparison against a dependant's declared range then compares the wrong number.
 *
 * @author Griefed
 */
object VersionOfFile {

    /** The version [file] should be compared by. Currently its published version, verbatim. */
    fun of(file: ModFile): String? = file.version
}
