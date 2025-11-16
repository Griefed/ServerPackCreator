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
package de.griefed.serverpackcreator.api.plugins

/**
 * Base information to be provided by every extension.
 *
 * @author Griefed
 */
interface BaseInformation {
    /**
     * Get the name of this plugin.
     *
     * @return The name of this plugin.
     * @author Griefed
     */
    val name: String

    /**
     * Get the description of this plugin.
     *
     * @return The description of this plugin.
     * @author Griefed
     */
    val description: String

    /**
     * Get the author of this plugin.
     *
     * @return The author of this plugin.
     * @author Griefed
     */
    val author: String

    /**
     * Get the version of this plugin.
     *
     * @return The version of this plugin.
     * @author Griefed
     */
    val version: String
}