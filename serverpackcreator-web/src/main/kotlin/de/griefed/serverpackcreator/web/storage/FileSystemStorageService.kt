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
package de.griefed.serverpackcreator.web.storage

import de.griefed.serverpackcreator.api.utilities.common.size
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.bouncycastle.util.encoders.Hex
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.listDirectoryEntries

class FileSystemStorageService(private val rootLocation: Path, private val messageDigestInstance: MessageDigest) {

    constructor(rootLocation: Path) : this(rootLocation, MessageDigest.getInstance("SHA-256"))

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    @Throws(StorageException::class)
    fun store(file: MultipartFile): Optional<SavedFile> {
        try {
            if (file.isEmpty) {
                throw StorageException("Failed to store empty file.")
            }
            val id = System.currentTimeMillis()
            val destinationFile: Path = rootLocation.resolve("${id}.zip").normalize().toAbsolutePath()
            if (!destinationFile.parent.equals(rootLocation.toAbsolutePath())) {
                // This is a security check
                throw StorageException("Cannot store file outside current directory.")
            }
            file.transferTo(destinationFile)
            log.debug("Stored file to $destinationFile.")
            val sha256 = String(Hex.encode(messageDigestInstance.digest(destinationFile.toFile().readBytes())))
            return Optional.of(
                SavedFile(
                    id,
                    sha256,
                    destinationFile,
                    file.originalFilename ?: file.name, destinationFile.toFile().size().div(1048576.0).toInt()
                )
            )
        } catch (e: IOException) {
            return Optional.empty()
        }
    }

    fun load(id: Long): Optional<File> {
        val file =
            rootLocation.listDirectoryEntries().find { path -> path.toString().contains("$id") }?.normalize()?.toFile()
        return if (file != null && file.exists()) {
            Optional.of(file)
        } else {
            log.warn("Filesystem does not contain a file for $id.")
            Optional.empty()
        }
    }

    fun delete(id: Long) {
        FileSystemUtils.deleteRecursively(rootLocation.resolve("${id}.zip").normalize())
        FileSystemUtils.deleteRecursively(rootLocation.resolve("$id").normalize())
    }

    fun deleteAll() {
        for (path in rootLocation.listDirectoryEntries()) {
            FileSystemUtils.deleteRecursively(path)
        }
    }
}