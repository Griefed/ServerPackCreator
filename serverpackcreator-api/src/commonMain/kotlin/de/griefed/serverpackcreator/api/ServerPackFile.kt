/* Copyright (C) 2024  Griefed
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

/**
 * A ServerPackFile represents a source-destination-combination of two files/directories. The
 * source is the file/directory, usually in the modpack, whilst the destination is the file to
 * which the source is supposed to be copied to in the server pack.
 *
 * @author Griefed
 */
expect class ServerPackFile {

    /**
     * Copy the file to the server pack.
     *
     * @author Griefed
     */
    fun copy(overwrite: Boolean = true)
}