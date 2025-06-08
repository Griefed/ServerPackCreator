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
package de.griefed.serverpackcreator.api.serverpack

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import java.io.File
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.LinkPermission
import java.nio.file.Path

/**
 * A ServerPackFile represents a source-destination-combination of two files/directories. The
 * source is the file/directory, usually in the modpack, whilst the destination is the file to
 * which the source is supposed to be copied to in the server pack.
 *
 * @author Griefed
 */
@Suppress("MemberVisibilityCanBePrivate")
class ServerPackFile {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    val sourceFile: File
    val sourcePath: Path
    val destinationFile: File
    val destinationPath: Path

    /**
     * Construct a new ServerPackFile from two [File]-objects, a source and a destination.
     *
     * @param sourceFile      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationFile The destination file/directory in the server pack.
     * @author Griefed
     */
    constructor(
        sourceFile: File, destinationFile: File
    ) {
        this.sourceFile = sourceFile
        this.sourcePath = sourceFile.toPath()
        this.destinationFile = destinationFile
        this.destinationPath = destinationFile.toPath()
    }

    /**
     * Construct a new ServerPackFile from two [Path]-objects, a source and a destination.
     *
     * @param sourcePath      The source file/directory. Usually a file/directory in a modpack.
     * @param destinationPath The destination file/directory in the server pack.
     * @author Griefed
     */
    constructor(
        sourcePath: Path, destinationPath: Path
    ) {
        this.sourceFile = sourcePath.toFile()
        this.sourcePath = sourcePath
        this.destinationFile = destinationPath.toFile()
        this.destinationPath = destinationPath
    }

    /**
     * Copy this ServerPackFiles source to the destination. Already existing files are replaced. When the
     * source-file is a directory, then the destination-directory is created as an empty directory. Any contents in
     * the source-directory are NOT copied over to the destination-directory.
     * See [copyRecursively][File.copyRecursively] for an example on how to copy entire directories.
     *
     * This method specifically does NOT copy recursively, because we would potentially copy previously EXCLUDED
     * files, too. We do not want that. At all.
     *
     * @throws SecurityException             In the case of the default provider, and a security manager is
     * installed, the [checkRead][java.lang.SecurityManager.checkRead] method is invoked to check read access to the source
     * file, the [checkWrite][SecurityManager.checkWrite] is invoked to check write access to the target file. If
     * a symbolic link is copied the security manager is invoked to check [LinkPermission]`("symbolic")`.
     * @throws UnsupportedOperationException if the array contains a copy option that is not supported.
     * @throws IOException                   if an I/O error occurs
     * @author Griefed
     */
    @Suppress("removal")
    @Throws(SecurityException::class, UnsupportedOperationException::class, IOException::class)
    fun copy(overwrite: Boolean = true) : File {
        try {
            sourceFile.copyTo(destinationFile, overwrite)
            log.trace("Successfully copied ServerPackFile")
            log.trace("    Source: $sourcePath")
            log.trace("    Destination: $destinationPath")
        } catch (ignored: DirectoryNotEmptyException) {
            // If the directory to be copied already exists we're good.
        } catch (skip: FileAlreadyExistsException) {
            log.warn("Skipping copying of $sourcePath because overwriting is disabled.")
        }
        return destinationFile
    }
}