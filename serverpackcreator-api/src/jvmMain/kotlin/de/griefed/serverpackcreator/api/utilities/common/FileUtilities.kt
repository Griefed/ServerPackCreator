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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package de.griefed.serverpackcreator.api.utilities.common

import mslinks.ShellLink
import mslinks.ShellLinkException
import net.lingala.zip4j.ZipFile
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.moveTo
import kotlin.streams.asStream

/**
 * Utility-class revolving around various file-interactions.
 *
 * @author Griefed
 */
actual class FileUtilities {
    private val log = cachedLoggerOf(this.javaClass)
    private val windowsDrivers = "^[A-Za-z]:.*".toRegex()

    /**
     * Move a file from source to destination, and replace the destination file if it exists.
     *
     * @param sourceFile      The source file.
     * @param destinationFile The destination file to be replaced by the source file.
     * @return  Returns true if the file was successfully replaced.
     * @throws IOException Thrown if an error occurs when the file is moved.
     * @author Griefed
     */
    @Throws(IOException::class)
    actual fun replaceFile(sourceFile: File, destinationFile: File): Boolean {
        if (sourceFile.exists()) {
            sourceFile.toPath().moveTo(destinationFile.toPath(), true)
            return true
        }
        log.error("Source file not found.")
        return false
    }

    /**
     * Unzips the downloaded modpack ZIP-archive to the specified directory.
     *
     * @param zipFile              The path to the ZIP-archive which we want to unzip.
     * @param destinationDirectory The directory into which the ZIP-archive will be unzipped into.
     * @author Griefed
     */
    actual fun unzipArchive(zipFile: String, destinationDirectory: String) {
        log.info("Extracting ZIP-file: $zipFile")
        try {
            ZipFile(zipFile).use { zip -> zip.extractAll(destinationDirectory) }
        } catch (ex: IOException) {
            log.error("Error: There was an error extracting the archive $zipFile", ex)
        }
    }

    /**
     * Check the given file for its type, whether it is a regular file, a Windows link or a UNIX
     * symlink.
     *
     * @param file The file to check
     * @return The type of the given file. Either [FileType.FILE], [FileType.LINK] or
     * [FileType.SYMLINK]
     * @author Griefed
     */
    actual fun checkFileType(file: String): FileType {
        return if (file.isEmpty()) {
            FileType.INVALID
        } else checkFileType(File(file))
    }

    /**
     * Check the given file for its type, whether it is a regular file, a Windows link or a UNIX
     * symlink.
     *
     * @param file The file to check
     * @return The type of the given file. Either [FileType.FILE], [FileType.LINK] or
     * [FileType.SYMLINK]
     * @author Griefed
     */
    actual fun checkFileType(file: File): FileType {
        if (file.name.endsWith("lnk")) {
            return FileType.LINK
        }
        if (file.isDirectory) {
            return FileType.DIRECTORY
        }
        if (file.toPath().isSymbolicLink()) {
            return FileType.SYMLINK
        }
        return if (file.isFile) {
            FileType.FILE
        } else FileType.INVALID
    }

    /**
     * Check if the given file is a UNIX symlink or Windows lnk.
     *
     * @param file The file to check.
     * @return `true` if the given file is a UNIX symlink or Windows lnk.
     * @author Griefed
     */
    fun isLink(file: String) = isLink(File(file))

    /**
     * Check if the given file is a UNIX symlink or Windows lnk.
     *
     * @param file The file to check.
     * @return `true` if the given file is a UNIX symlink or Windows lnk.
     * @author Griefed
     */
    actual fun isLink(file: File) =
        if (file.name.endsWith("lnk")) {
            true
        } else !file.toString().matches(windowsDrivers) && file.toPath().isSymbolicLink()

    /**
     * Resolve a given link/symlink to its source.
     *
     * @param link The link you want to resolve.
     * @return Path to the source of the link. If the specified file is not a link, the path to the
     * passed file is returned.
     * @throws IOException              if the link could not be parsed.
     * @throws InvalidFileTypeException if the specified file is neither a file, lnk nor symlink.
     * @author Griefed
     */
    @Throws(InvalidFileTypeException::class, IOException::class)
    fun resolveLink(link: String) = resolveLink(File(link))

    /**
     * Resolve a given link/symlink to its source.
     *
     * @param link The link you want to resolve.
     * @return Path to the source of the link. If the specified file is not a link, the path to the
     * passed file is returned.
     * @throws IOException              if the link could not be parsed.
     * @throws InvalidFileTypeException if the specified file is neither a file, lnk nor symlink.
     * @author Griefed
     */
    @Throws(IOException::class, InvalidFileTypeException::class)
    actual fun resolveLink(link: File) =
        when (val type = checkFileType(link)) {
            FileType.LINK, FileType.SYMLINK -> {
                try {
                    resolveLink(link, type)
                } catch (ex: InvalidFileTypeException) {
                    log.error("Somehow an invalid FileType was specified. Please report this on GitHub!", ex)
                } catch (ex: InvalidLinkException) {
                    log.error("Somehow an invalid FileType was specified. Please report this on GitHub!", ex)
                } catch (ex: ShellLinkException) {
                    log.error("Somehow an invalid FileType was specified. Please report this on GitHub!", ex)
                }
                link.toString()
            }

            FileType.FILE, FileType.DIRECTORY -> link.toString()
            FileType.INVALID -> throw InvalidFileTypeException("FileType must be either LINK or SYMLINK")
        }

    /**
     * Resolve a given link/symlink to its source.
     *
     * This would not exist without the great answers from this StackOverflow question:
     * * [Windows Shortcut lnk parser in Java](https://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java)
     *
     * Huge shoutout to [Codebling](https://stackoverflow.com/users/675721/codebling)
     *
     * @param file     The file of which to acquire the source.
     * @param fileType The link-type. Either [FileType.LINK] for Windows, or
     * [FileType.SYMLINK] for UNIX systems.
     * @return The path to the source of the given link.
     * @throws InvalidFileTypeException if the specified [FileType] is invalid.
     * @throws InvalidLinkException     if the specified file is not a valid Windows link.
     * @throws ShellLinkException       if the specified file could not be parsed as a Windows link.
     * @throws IOException              if the link could not be parsed.
     * @author Griefed
     */
    @Throws(InvalidFileTypeException::class, IOException::class, InvalidLinkException::class, ShellLinkException::class)
    private fun resolveLink(
        file: File,
        fileType: FileType
    ) = when (fileType) {
        FileType.SYMLINK -> file.path
        FileType.LINK -> ShellLink(file).resolveTarget()
        else -> throw InvalidFileTypeException("FileType must be either LINK or SYMLINK")
    }

    /**
     * Check the given file or directory for read- and write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if both read- and write-permissions are set.
     * @author Griefed
     */
    fun isReadWritePermissionSet(fileOrDirectory: Path) =
        isReadPermissionSet(fileOrDirectory) && isWritePermissionSet(fileOrDirectory)

    /**
     * Check the given file or directory for read-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if read-permissions are set.
     * @author Griefed
     */
    fun isReadPermissionSet(fileOrDirectory: Path): Boolean {
        try {
            if (!Files.isReadable(fileOrDirectory)) {
                log.error("No read-permission for %s".format(fileOrDirectory))
                return false
            }
        } catch (ex: SecurityException) {
            log.error("No read-permission for %s".format(fileOrDirectory), ex)
            return false
        }
        return true
    }

    /**
     * Check the given file or directory for write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if write-permissions are set.
     * @author Griefed
     */
    fun isWritePermissionSet(fileOrDirectory: Path): Boolean {
        try {
            if (!Files.isWritable(fileOrDirectory)) {
                log.error("No write-permission for %s".format(fileOrDirectory))
                return false
            }
        } catch (ex: SecurityException) {
            log.error("No write-permission for %s".format(fileOrDirectory), ex)
            return false
        }
        return true
    }

    /**
     * Check the given file or directory for read-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if read-permissions are set.
     * @author Griefed
     */
    actual fun isReadPermissionSet(fileOrDirectory: String) = isReadPermissionSet(Paths.get(fileOrDirectory))

    /**
     * Check the given file or directory for read-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if read-permissions are set.
     * @author Griefed
     */
    actual fun isReadPermissionSet(fileOrDirectory: File) = isReadPermissionSet(fileOrDirectory.toPath())

    /**
     * Check the given file or directory for write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if write-permissions are set.
     * @author Griefed
     */
    actual fun isWritePermissionSet(fileOrDirectory: String) = isReadPermissionSet(Paths.get(fileOrDirectory))

    /**
     * Check the given file or directory for write-permission.
     *
     * @param fileOrDirectory File or directory.
     * @return `true` if write-permissions are set.
     * @author Griefed
     */
    actual fun isWritePermissionSet(fileOrDirectory: File) = isReadPermissionSet(fileOrDirectory.toPath())

    /**
     * Open the specified folder in the file explorer.
     *
     * @param folder The folder to open.
     * @author Griefed
     */
    actual fun openFolder(folder: String) = openFolder(File(folder))

    /**
     * Open the specified folder in the file explorer.
     *
     * @param folder The folder to open.
     * @author Griefed
     */
    actual fun openFolder(folder: File) {
        if (GraphicsEnvironment.isHeadless()) {
            log.error("Graphics environment not supported.")
        } else {
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(folder)
                } catch (ex: IOException) {
                    log.error("Error opening file explorer for $folder.", ex)
                }
            }
        }
    }

    /**
     * Open the specified file in an editor.
     *
     * @param fileToOpen The file to open.
     * @author Griefed
     */
    actual fun openFile(fileToOpen: String) = openFile(File(fileToOpen))

    /**
     * Open the specified file in an editor.
     *
     * @param fileToOpen The file to open.
     * @author Griefed
     */
    actual fun openFile(fileToOpen: File) {
        if (GraphicsEnvironment.isHeadless()) {
            log.error("Graphics environment not supported.")
        } else {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                    Desktop.getDesktop().open(fileToOpen)
                }
            } catch (ex: IOException) {
                log.error("Error opening file.", ex)
            }
        }
    }
}

/**
 * Try and delete this file or directory quietly. Deleting something quietly means that
 * * No guarantee is made whether the file or directory is successfully deleted
 * * No exceptions are thrown if an error occurs
 * * No information is carried outside should an exception occur, meaning you have no information about why the deletion, if it failed
 *
 * @author Griefed
 */
actual fun File.deleteQuietly() =
    if (this.isFile) {
        try {
            this.delete()
        } catch (ignored: Exception) {
        }
    } else {
        try {
            this.deleteRecursively()
        } catch (ignored: Exception) {
        }
    }

/**
 * Acquire the size of this file or directory in bytes. If this file-object denotes a directory, then the size of all
 * files in the directory will be checked and the sum of them returned.
 *
 * @return The filesize of this file, or sum of sizes of all files in this directory.
 * @author Griefed
 */
actual fun File.size(): Double {
    if (this.isDirectory) {
        val size = 0.0
        val entries = this.listFiles() ?: return size
        for (entry in entries) {
            size.plus(entry.size())
        }
        return size
    } else {
        return this.length().toDouble()
    }
}

/**
 * Walk this directory and return all file-objects which match any of the regular expressions in the passed list.
 *
 * @param filters List of regular expressions to use for filtering.
 * @param direction The direction in which to walk the directory. Default is [FileWalkDirection.TOP_DOWN].
 * @return All files inside this directory which matched the given filters.
 * @author Griefed
 */
actual fun File.regexWalk(filters: List<Regex>, direction: FileWalkDirection): MutableList<File> =
    this.walk(direction).asStream().filter { filters.matchAll(it.name) }.toList()

/**
 * Walk this directory and return all file-objects which match the specified filter-type using the passed list of filters.
 *
 * @param filters List of Strings to use for filtering
 * @param filterType Whether to filter by [FilterType.CONTAINS] (default), [FilterType.ENDS_WITH] or [FilterType.STARTS_WITH].
 * @param direction The direction in which to walk the directory. Default is [FileWalkDirection.TOP_DOWN].
 * @return All files inside this directory which matched the given filters.
 * @author Griefed
 */
actual fun File.filteredWalk(
    filters: List<String>,
    filterType: FilterType,
    direction: FileWalkDirection,
    recursive: Boolean
): MutableList<File> =
    when (filterType) {
        FilterType.CONTAINS -> {
            if (recursive) {
                this.walk(direction).asStream().filter { filters.contains(it.name) }.toList()
            } else {
                this.walk(direction).maxDepth(1).asStream().filter { filters.contains(it.name) }.toList()
            }
        }

        FilterType.ENDS_WITH -> {
            if (recursive) {
                this.walk(direction).asStream().filter { filters.endsWith(it.name) }.toList()
            } else {
                this.walk(direction).maxDepth(1).asStream().filter { filters.endsWith(it.name) }.toList()
            }
        }

        FilterType.STARTS_WITH -> {
            if (recursive) {
                this.walk(direction).asStream().filter { filters.startsWith(it.name) }.toList()
            } else {
                this.walk(direction).maxDepth(1).asStream().filter { filters.endsWith(it.name) }.toList()
            }
        }
    }


/**
 * All parent directories are created, but not the file itself.
 *
 * @param create Whether the file or directory should be created.
 * @param directory Whether a directory or file should be created
 * @author Griefed
 */
actual fun File.createDirectories(create: Boolean, directory: Boolean) {
    Files.createDirectories(this.absoluteFile.parentFile.toPath())
    if (create) {
        if (directory) {
            this.mkdir()
        } else {
            this.createNewFile()
        }
    }
}