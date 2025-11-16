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
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.moveTo
import kotlin.streams.asStream

/**
 * Utility-class revolving around various file-interactions.
 *
 * @author Griefed
 */
class FileUtilities {
    companion object {
        private val log by lazy { cachedLoggerOf(FileUtilities::class.java) }
        private val windowsDrivers = "^[A-Za-z]:.*".toRegex()
        private const val LNK = "lnk"

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
        fun replaceFile(sourceFile: File, destinationFile: File): Boolean {
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
        fun unzipArchive(zipFile: String, destinationDirectory: String) {
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
        fun checkFileType(file: String): FileType {
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
        fun checkFileType(file: File): FileType {
            @Suppress("RegExpRedundantEscape")
            return when {
                file.name.endsWith(LNK) -> {
                    FileType.LINK
                }

                file.name.matches("::\\{[0-9a-zA-Z-]+\\}".toRegex()) -> {
                    FileType.FILE
                }

                file.isDirectory -> {
                    FileType.DIRECTORY
                }

                file.toPath().isSymbolicLink() -> {
                    FileType.SYMLINK
                }

                file.isFile -> {
                    FileType.FILE
                }

                else -> FileType.INVALID
            }
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
        fun isLink(file: File) =
            if (file.name.endsWith(LNK)) {
                true
            } else {
                try {
                    !file.toString().matches(windowsDrivers)
                            && file.toPath().isSymbolicLink()
                } catch (ex: InvalidPathException) {
                    false
                }
            }

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
        fun resolveLink(link: File): String =
            when (val type = checkFileType(link)) {
                FileType.LINK, FileType.SYMLINK -> {
                    try {
                        resolveLink(link, type).toString()
                    } catch (ex: InvalidFileTypeException) {
                        log.error(
                            "Somehow an invalid FileType was specified: $link, type $type. Please report this on GitHub!",
                            ex
                        )
                        link.absolutePath
                    } catch (ex: InvalidLinkException) {
                        log.error(
                            "Somehow an invalid FileType was specified: $link, type $type. Please report this on GitHub!",
                            ex
                        )
                        link.absolutePath
                    } catch (ex: ShellLinkException) {
                        log.error(
                            "Somehow an invalid FileType was specified: $link, type $type. Please report this on GitHub!",
                            ex
                        )
                        link.absolutePath
                    }
                }

                FileType.FILE, FileType.DIRECTORY -> link.toString()
                FileType.INVALID -> throw InvalidFileTypeException("FileType must be either LINK or SYMLINK, type: $type")
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
            FileType.SYMLINK -> file.toPath().toRealPath().toString()
            FileType.LINK -> ShellLink(file.absoluteFile).resolveTarget()
            else -> throw InvalidFileTypeException("FileType must be either LINK or SYMLINK. Specified $fileType.")
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
        fun isReadPermissionSet(fileOrDirectory: String) = isReadPermissionSet(Paths.get(fileOrDirectory))

        /**
         * Check the given file or directory for read-permission.
         *
         * @param fileOrDirectory File or directory.
         * @return `true` if read-permissions are set.
         * @author Griefed
         */
        fun isReadPermissionSet(fileOrDirectory: File) = isReadPermissionSet(fileOrDirectory.toPath())

        /**
         * Check the given file or directory for write-permission.
         *
         * @param fileOrDirectory File or directory.
         * @return `true` if write-permissions are set.
         * @author Griefed
         */
        fun isWritePermissionSet(fileOrDirectory: String) = isReadPermissionSet(Paths.get(fileOrDirectory))

        /**
         * Check the given file or directory for write-permission.
         *
         * @param fileOrDirectory File or directory.
         * @return `true` if write-permissions are set.
         * @author Griefed
         */
        fun isWritePermissionSet(fileOrDirectory: File) = isReadPermissionSet(fileOrDirectory.toPath())

        /**
         * Check the given file or directory for read- and write-permission.
         *
         * @param fileOrDirectory File or directory.
         * @return `true` if both read- and write-permissions are set.
         * @author Griefed
         */
        fun isReadWritePermissionSet(fileOrDirectory: String) =
            isReadPermissionSet(fileOrDirectory) && isWritePermissionSet(fileOrDirectory)

        /**
         * Check the given file or directory for read- and write-permission.
         *
         * @param fileOrDirectory File or directory.
         * @return `true` if both read- and write-permissions are set.
         * @author Griefed
         */
        fun isReadWritePermissionSet(fileOrDirectory: File) =
            isReadPermissionSet(fileOrDirectory) && isWritePermissionSet(fileOrDirectory)

        /**
         * Open the specified folder in the file explorer.
         *
         * @param folder The folder to open.
         * @author Griefed
         */
        fun openFolder(folder: String) = openFolder(File(folder))

        /**
         * Open the specified folder in the file explorer.
         *
         * @param folder The folder to open.
         * @author Griefed
         */
        fun openFolder(folder: File) {
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
        fun openFile(fileToOpen: String) = openFile(File(fileToOpen))

        /**
         * Open the specified file in an editor.
         *
         * @param fileToOpen The file to open.
         * @author Griefed
         */
        fun openFile(fileToOpen: File) {
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

        /**
         * Delete multiple files quietly.
         *
         * @author Griefed
         */
        fun deleteMultiple(vararg files: File) {
            for (file in files) {
                file.deleteQuietly()
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
 * @return `true` if, and only if, the file or directory was deleted.
 * @author Griefed
 */
fun File.deleteQuietly(): Boolean =
    if (this.isFile) {
        try {
            this.delete()
        } catch (ignored: Exception) {
            false
        }
    } else {
        try {
            this.deleteRecursively()
        } catch (ignored: Exception) {
            false
        }
    }

/**
 * Acquire the size of this file or directory in bytes. If this file-object denotes a directory, then the size of all
 * files in the directory will be checked and the sum of them returned.
 *
 * @return The filesize of this file, or sum of sizes of all files in this directory.
 * @author Griefed
 */
fun File.size(): Double {
    if (this.isDirectory) {
        var size = 0.0
        val entries = this.listFiles() ?: return size
        for (entry in entries) {
            size = size.plus(entry.size())
        }
        return size
    } else {
        return this.length().toDouble()
    }
}

/**
 * Walk this directory and return all file-objects which match any of the regular expressions in the provided list.
 *
 * @param filters List of regular expressions to use for filtering.
 * @param direction The direction in which to walk the directory. Default is [FileWalkDirection.TOP_DOWN].
 * @return All files inside this directory which matched the given filters.
 * @author Griefed
 */
fun File.regexWalk(filters: List<Regex>, direction: FileWalkDirection): MutableList<File> =
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
fun File.filteredWalk(
    filters: List<String>,
    filterType: FilterType = FilterType.CONTAINS,
    direction: FileWalkDirection = FileWalkDirection.TOP_DOWN,
    recursive: Boolean = true
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
 * [createFileOrDir] in combination with [asDirectory] will result in this file being treated as a directory. As such, all
 * parents will be created, along with this file itself being created as a directory.
 *
 * [createFileOrDir] without [asDirectory] will result in this file being created as a file.
 *
 * @param createFileOrDir Whether the file or directory should be created. If left to `false`, then [asDirectory] won't have any effect.
 * @param asDirectory true to create a directory, false to create a file. Requires [createFileOrDir] to be true
 * @author Griefed
 */
fun File.create(createFileOrDir: Boolean = false, asDirectory: Boolean = false) {
    absoluteFile.toPath().createParentDirectories()
    if (createFileOrDir) {
        if (asDirectory) {
            this.mkdirs()
        } else {
            this.createNewFile()
        }
    }
}

/**
 * Test whether files can be written to this file denoting a directory.
 * If this file is not a directory, an [IllegalArgumentException] will be thrown.
 *
 * @author Griefed
 */
@Throws(IllegalArgumentException::class)
fun File.testFileWrite() : Boolean {
    if (!this.isDirectory) {
        throw(IllegalArgumentException("Destination must be a directory."))
    }
    return try {
        val file = File(this,"poke")
        file.writeText("writable")
        if (file.exists()) {
            file.deleteQuietly()
            true
        } else {
            false
        }
    } catch (ex: Exception) {
        false
    }
}