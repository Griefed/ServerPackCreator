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
package de.griefed.serverpackcreator.app.web.storage

import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

class StorageSystem(private val fileSystemStorageService: FileSystemStorageService) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }

    constructor(rootLocation: Path, messageDigest: MessageDigest) : this(
        FileSystemStorageService(rootLocation, messageDigest)
    )

    constructor(rootLocation: Path) : this(
        FileSystemStorageService(rootLocation)
    )

    fun store(file: MultipartFile): Optional<SavedFile> {
        return fileSystemStorageService.store(file)
    }

    fun load(id: Long): Optional<File> {
        val fileSys = fileSystemStorageService.load(id)
        if (fileSys.isPresent) {
            return fileSys
        }
        log.error("File with ID $id could not be found.")
        return Optional.empty()
    }

    fun delete(id: Long) {
        fileSystemStorageService.delete(id)
    }

    fun deleteAll() {
        fileSystemStorageService.deleteAll()
    }
}