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
@file:Suppress("unused")

package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.utilities.File

/**
 * Check the given file or directory for read- and write-permission.
 *
 * @param fileOrDirectory File or directory.
 * @return `true` if both read- and write-permissions are set.
 * @author Griefed
 */
fun FileUtilities.isReadWritePermissionSet(fileOrDirectory: String) =
    isReadPermissionSet(fileOrDirectory) && isWritePermissionSet(fileOrDirectory)

/**
 * Check the given file or directory for read- and write-permission.
 *
 * @param fileOrDirectory File or directory.
 * @return `true` if both read- and write-permissions are set.
 * @author Griefed
 */
fun FileUtilities.isReadWritePermissionSet(fileOrDirectory: File) =
    isReadPermissionSet(fileOrDirectory) && isWritePermissionSet(fileOrDirectory)