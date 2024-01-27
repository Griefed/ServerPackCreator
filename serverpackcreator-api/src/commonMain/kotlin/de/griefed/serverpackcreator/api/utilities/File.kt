/*
 * Copyright (C) 2024 Griefed.
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

package de.griefed.serverpackcreator.api.utilities

/**
 * Class giving access to several things like:
 * - path
 * - absolute path
 * - parent-directory
 * - whether an instance is a file or directory
 * - separator / path separator depending on the filesystem
 * - constructor accepting a parent and the actual file
 * - constructor accepting the full path of the file
 * - constructor accepting the relative path to the file in relation to the current working directory
 * - name of the file
 * - check read and write access
 * - whether the file exists
 * - deletion
 * - recursive deletion if the file is a directory
 * - list contents of the file if it is a directory
 * - create a directory
 * - rename
 * - move
 * - copy
 * - copy recursively
 * - filesize
 * - directory size
 *
 * See the File-class from Java for reference.
 *
 * @author Griefed
 */
expect class File