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
package de.griefed.serverpackcreator.api.plugins

/**
 * Base information to be provided by every extension.
 *
 * @author Griefed
 */
interface BaseInformation {
    /** Shown in the GUI's plugin list and named in `plugins.log` whenever this extension misbehaves. */
    val name: String

    /** One line telling a user what this does, shown beside [name]. */
    val description: String

    /** Credited in the GUI; free-form, and never used to identify anything. */
    val author: String

    /** Displayed as-is. SPC compares nothing against it, so any scheme works. */
    val version: String
}