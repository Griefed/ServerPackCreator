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
package de.griefed.serverpackcreator.api.utilities.common

/**
 * match the given string against all regular expressions in this list.
 * @return true if any match was found, false otherwise.
 * @author Griefed
 */
fun List<Regex>.matchAll(string: String): Boolean {
    var found = false
    for (entry in this) {
        if (string.matches(entry)) {
            found = true
        }
    }
    return found
}

/**
 * Turn this string into a regex in which every `.` matches a literal dot instead of any character.
 *
 * Intended for building patterns out of dotted version numbers, where an unescaped `.` silently makes a pattern too
 * permissive — `1.21` would otherwise also match `1x21`.
 *
 * @return This string as a [Regex], with every dot escaped.
 * @author Griefed
 */
fun String.toDotEscapedRegex(): Regex {
    val escaped = this.replace(".", "\\.")
    return escaped.toRegex()
}
