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
package de.griefed.serverpackcreator.api.utilities.common

import de.griefed.serverpackcreator.api.utilities.File

/**
 * Utility-class revolving around various file-interactions.
 *
 * @author Griefed
 */
expect class FileUtilities {
    /**
     * Move a file from source to destination, and replace the destination file if it exists.
     *
     * @param sourceFile      The source file.
     * @param destinationFile The destination file to be replaced by the source file.
     * @return  Returns true if the file was successfully replaced.
     * @author Griefed
     */
    fun replaceFile(sourceFile: File, destinationFile: File): Boolean

    /**
     * Unzips the downloaded modpack ZIP-archive to the specified directory.
     *
     * @param zipFile              The path to the ZIP-archive which we want to unzip.
     * @param destinationDirectory The directory into which the ZIP-archive will be unzipped into.
     * @author Griefed
     */
    fun unzipArchive(zipFile: String, destinationDirectory: String)

    /**
     * Check the given file for its type, whether it is a regular file, a Windows link or a UNIX
     * symlink.
     *
     * @param file The file to check
     * @return The type of the given file. Either [FileType.FILE], [FileType.LINK] or
     * [FileType.SYMLINK]
     * @author Griefed
     */
    fun checkFileType(file: String): FileType

    /**
     * Check the given file for its type, whether it is a regular file, a Windows link or a UNIX
     * symlink.
     *
     * @param file The file to check
     * @return The type of the given file. Either [FileType.FILE], [FileType.LINK] or
     * [FileType.SYMLINK]
     * @author Griefed
     */
    fun checkFileType(file: File): FileType

    /**
     * Check if the given file is a UNIX symlink or Windows lnk.
     *
     * @param file The file to check.
     * @return `true` if the given file is a UNIX symlink or Windows lnk.
     * @author Griefed
     */
    fun isLink(file: File): Boolean

    /**
     * Resolve a given link/symlink to its source.
     *
     * @param link The link you want to resolve.
     * @return Path to the source of the link. If the specified file is not a link, the path to the
     * passed file is returned.
     * @author Griefed
     */
    fun resolveLink(link: File): String

    /**
     * Check the given file or directory for read-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if read-permissions are set.
     * @author Griefed
     */
    fun isReadPermissionSet(fileOrDirectory: String): Boolean

    /**
     * Check the given file or directory for read-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if read-permissions are set.
     * @author Griefed
     */
    fun isReadPermissionSet(fileOrDirectory: File): Boolean

    /**
     * Check the given file or directory for write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if write-permissions are set.
     * @author Griefed
     */
    fun isWritePermissionSet(fileOrDirectory: String): Boolean

    /**
     * Check the given file or directory for write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if write-permissions are set.
     * @author Griefed
     */
    fun isWritePermissionSet(fileOrDirectory: File): Boolean

    /**
     * Open the specified folder in the file explorer.
     *
     * @param folder The folder to open.
     * @author Griefed
     */
    fun openFolder(folder: String)

    /**
     * Open the specified folder in the file explorer.
     *
     * @param folder The folder to open.
     * @author Griefed
     */
    fun openFolder(folder: File)

    /**
     * Open the specified file in an editor.
     *
     * @param fileToOpen The file to open.
     * @author Griefed
     */
    fun openFile(fileToOpen: String)

    /**
     * Open the specified file in an editor.
     *
     * @param fileToOpen The file to open.
     * @author Griefed
     */
    fun openFile(fileToOpen: File)
}

/**
 * Try and delete this file or directory quietly. Deleting something quietly means that
 * * No guarantee is made whether the file or directory is successfully deleted
 * * No exceptions are thrown if an error occurs
 * * No information is carried outside should an exception occur, meaning you have no information about why the deletion, if it failed
 *
 * @author Griefed
 */
expect fun File.deleteQuietly(): Any

/**
 * Acquire the size of this file or directory in bytes. If this file-object denotes a directory, then the size of all
 * files in the directory will be checked and the sum of them returned.
 *
 * @return The filesize of this file, or sum of sizes of all files in this directory.
 * @author Griefed
 */
expect fun File.size(): Double

/**
 * Walk this directory and return all file-objects which match any of the regular expressions in the passed list.
 *
 * @param filters List of regular expressions to use for filtering.
 * @param direction The direction in which to walk the directory. Default is [FileWalkDirection.TOP_DOWN].
 * @return All files inside this directory which matched the given filters.
 * @author Griefed
 */
expect fun File.regexWalk(
    filters: List<Regex>,
    direction: FileWalkDirection = FileWalkDirection.TOP_DOWN
): MutableList<File>

/**
 * Walk this directory and return all file-objects which match the specified filter-type using the passed list of filters.
 *
 * @param filters List of Strings to use for filtering
 * @param filterType Whether to filter by [FilterType.CONTAINS] (default), [FilterType.ENDS_WITH] or [FilterType.STARTS_WITH].
 * @param direction The direction in which to walk the directory. Default is [FileWalkDirection.TOP_DOWN].
 * @return All files inside this directory which matched the given filters.
 * @author Griefed
 */
expect fun File.filteredWalk(
    filters: List<String>,
    filterType: FilterType = FilterType.CONTAINS,
    direction: FileWalkDirection = FileWalkDirection.TOP_DOWN
): MutableList<File>

/**
 * All parent directories are created, but not the file itself.
 *
 * @param create Whether the file or directory should be created.
 * @param directory Whether a directory or file should be created
 * @author Griefed
 */
expect fun File.createDirectories(create: Boolean = false, directory: Boolean = false)