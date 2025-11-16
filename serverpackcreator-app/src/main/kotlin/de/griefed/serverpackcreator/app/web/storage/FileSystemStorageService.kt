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
package de.griefed.serverpackcreator.app.web.storage

import com.mongodb.client.gridfs.model.GridFSFile
import de.griefed.serverpackcreator.api.utilities.common.size
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.bouncycastle.util.encoders.Hex
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.listDirectoryEntries

class FileSystemStorageService(val rootLocation: Path, private val messageDigestInstance: MessageDigest) {

    constructor(rootLocation: Path) : this(rootLocation, MessageDigest.getInstance("SHA-256"))

    private val log by lazy { cachedLoggerOf(this.javaClass) }

    private fun determineFilename(filename: String): String {
        return if (
            filename.contains("-orig-") &&
            filename.split("-orig-").size >= 2 &&
            filename.split("-orig-")[1].isNotEmpty()
        ) {
            filename.split("-orig-")[1]
        } else {
            filename
        }
    }

    @Throws(StorageException::class)
    fun store(file: File, objectId: String): Optional<SavedFile> {
        try {
            val id = objectId
            val originalName = determineFilename(file.name)
            val destinationFilePath: Path = rootLocation.resolve("${id}.zip").normalize().toAbsolutePath()
            if (!destinationFilePath.parent.equals(rootLocation.toAbsolutePath())) {
                // This is a security check
                throw StorageException("Cannot store file outside current directory.")
            }
            FileUtils.copyFile(file, destinationFilePath.toFile())
            log.debug("Stored file to $destinationFilePath.")
            val sha256 = String(Hex.encode(messageDigestInstance.digest(destinationFilePath.toFile().readBytes())))
            return Optional.of(
                SavedFile(
                    id = id,
                    sha256 = sha256,
                    file = destinationFilePath,
                    originalName = originalName,
                    size = destinationFilePath.toFile().size().div(1048576.0).toInt()
                )
            )
        } catch (e: IOException) {
            log.error("Error storing file: ", e)
            return Optional.empty()
        }
    }

    @Throws(StorageException::class)
    fun store(file: GridFSFile, resource: GridFsResource): Optional<SavedFile> {
        try {
            val id = file.objectId.toString()
            val destinationFilePath: Path = rootLocation.resolve("${id}.zip").normalize().toAbsolutePath()
            if (!destinationFilePath.parent.equals(rootLocation.toAbsolutePath())) {
                // This is a security check
                throw StorageException("Cannot store file outside current directory.")
            }
            FileUtils.copyToFile(resource.inputStream, destinationFilePath.toFile())
            log.debug("Stored file to $destinationFilePath.")
            val sha256 = String(Hex.encode(messageDigestInstance.digest(destinationFilePath.toFile().readBytes())))
            return Optional.of(
                SavedFile(
                    id,
                    sha256,
                    destinationFilePath,
                    file.filename,
                    destinationFilePath.toFile().size().div(1048576.0).toInt()
                )
            )
        } catch (e: IOException) {
            log.error("Error storing file: ", e)
            return Optional.empty()
        }
    }

    fun load(id: String): Optional<File> {
        val file =
            rootLocation.listDirectoryEntries().find { path -> path.toString().contains(id) }?.normalize()?.toFile()
        return if (file != null && file.exists()) {
            Optional.of(file)
        } else {
            log.warn("Filesystem does not contain a file for $id.")
            Optional.empty()
        }
    }

    fun delete(id: String) {
        FileSystemUtils.deleteRecursively(rootLocation.resolve("${id}.zip").normalize())
        FileSystemUtils.deleteRecursively(rootLocation.resolve(id).normalize())
    }

    fun deleteAll() {
        for (path in rootLocation.listDirectoryEntries()) {
            FileSystemUtils.deleteRecursively(path)
        }
    }
}