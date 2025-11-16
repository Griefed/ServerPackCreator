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
package de.griefed.serverpackcreator.api.utilities.common

enum class Comparison {
    /**
     * Used to determine whether two given versions are the same.
     */
    EQUAL,

    /**
     * Used to determine whether a given version is newer.
     */
    NEW,

    /**
     * Used to determine whether a given version is the same or newer.
     */
    EQUAL_OR_NEW
}

/**
 * File-type to use, identify and report configured Java versions with.
 *
 * @author Griefed
 */
enum class FileType {
    /**
     * A regular file.
     */
    FILE,

    /**
     * A regular directory.
     */
    DIRECTORY,

    /**
     * A Windows link.
     */
    LINK,

    /**
     * A UNIX symlink.
     */
    SYMLINK,

    /**
     * Not a valid file.
     */
    INVALID
}

/**
 * Filter-types by which to filter entries when walking through the files in a directory.
 * @author Griefed
 */
enum class FilterType {
    /**
     * Whether the string to check contains the given filter.
     */
    CONTAINS,

    /**
     * Whether the string to check ends with the given filter.
     */
    ENDS_WITH,

    /**
     * Whether the string to check starts with the given filter.
     */
    STARTS_WITH
}