/* Copyright (C) 2026 Griefed
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
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*

/**
 * The storage seam the web layer talks to. Delegates to [FileSystemStorageService] for the bytes while keeping
 * the GridFS collaborators around, which is what lets an installation still holding files in the database be
 * migrated onto the filesystem.
 */
class StorageSystem(
    private val fsStorageService: FileSystemStorageService,
    gridFsTemplate: GridFsTemplate,
    gridFsOperations: GridFsOperations
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val dbStorageService = DatabaseStorageService(gridFsTemplate, gridFsOperations)

    constructor(
        rootLocation: Path,
        messageDigest: MessageDigest,
        gridFsTemplate: GridFsTemplate,
        gridFsOperations: GridFsOperations
    ) : this(FileSystemStorageService(rootLocation, messageDigest), gridFsTemplate, gridFsOperations)

    constructor(
        rootLocation: Path,
        gridFsTemplate: GridFsTemplate,
        gridFsOperations: GridFsOperations
    ) : this(
        FileSystemStorageService(rootLocation),
        gridFsTemplate,
        gridFsOperations
    )

    /** Store an uploaded file, returning what was written — empty when the store failed. */
    fun store(file: MultipartFile): Optional<SavedFile> {
        val uploadName = landingName(file)
        val root = fsStorageService.rootLocation.toAbsolutePath().normalize()
        val destination = root.resolve("${System.currentTimeMillis()}-orig-$uploadName").normalize()
        if (destination.parent != root) {
            // Belt and braces. landingName already reduces the client's string to a base name, so this
            // cannot trigger today; it is here because the thing it guards is an arbitrary path built
            // from a request, and the same check already sits on the internal path in FileSystemStorageService.
            log.error("Refusing to store an upload outside $root.")
            return Optional.empty()
        }
        return try {
            file.transferTo(destination.toFile())
            store(destination.toFile())
        } catch (failure: IOException) {
            // ModPackController catches StorageException only, so anything else escaping here is an
            // unhandled 500 -- and the shipped application.properties sets include-stacktrace=ALWAYS.
            log.error("Could not store the uploaded file $uploadName.", failure)
            Optional.empty()
        } catch (failure: IllegalStateException) {
            // The other half of MultipartFile.transferTo's contract: the part has already been moved.
            log.error("The upload $uploadName was no longer available to store.", failure)
            Optional.empty()
        }
    }

    /**
     * The name an upload is landed under: the base name of what the client sent, never a path.
     *
     * `MultipartFile.getOriginalFilename()` is whatever the caller put in `Content-Disposition`, and
     * Spring hands it over verbatim — separators included. Before this, the only thing keeping a
     * `../../../` out of the resulting path was that `"-orig-"` concatenates without a separator, so
     * the first component became a directory name that happens not to exist. That is an accident of
     * string concatenation, not a check, and it stops holding the moment the prefix changes.
     */
    private fun landingName(file: MultipartFile): String {
        val candidate = file.originalFilename ?: file.name
        val baseName = candidate.replace('\\', '/').substringAfterLast('/').trim()
        return baseName.ifBlank { file.name.ifBlank { "upload" } }
    }

    /** Store a file already on disk, for a generated archive rather than an upload. */
    fun store(file: File): Optional<SavedFile> {
        val id = dbStorageService.store(file)
        return fsStorageService.store(file, id.toString())
    }

    /** The stored file for an id, empty when there is none. */
    fun load(id: String): Optional<File> {
        val fileSys = fsStorageService.load(id)
        if (fileSys.isPresent) {
            return fileSys
        }
        val mongoFile = cacheFile(id)
        if (mongoFile.isPresent) {
            return mongoFile
        }
        log.error("File with ID $id could not be found.")
        return Optional.empty()
    }

    private fun cacheFile(id: String): Optional<File> {
        val mongoPair = dbStorageService.load(id)
        return if (mongoPair.isPresent) {
            val stored = fsStorageService.store(mongoPair.get().first, mongoPair.get().second)
            if (stored.isPresent) {
                Optional.of(stored.get().file.toFile())
            } else {
                Optional.empty()
            }
        } else {
            Optional.empty()
        }
    }

    /** Delete one stored file. */
    fun delete(id: String) {
        fsStorageService.delete(id)
    }

    /** Delete everything stored. For the cleanup schedule, not for a request. */
    @Suppress("unused")
    fun deleteAll() {
        fsStorageService.deleteAll()
    }
}