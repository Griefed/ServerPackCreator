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

import com.mongodb.client.gridfs.model.GridFSFile
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

/**
 * Stores uploaded and generated archives on the local filesystem, hashing each on the way in so the caller can
 * recognise a duplicate. The digest is injected rather than created here, because a `MessageDigest` is stateful.
 */
class FileSystemStorageService(
    /** Directory every stored file lands under. Public, because the storage system above reports it. */
    val rootLocation: Path,
    /**
     * The hash every stored file is fingerprinted with. Injected rather than created here because a
     * `MessageDigest` is stateful and not thread-safe, so who owns it has to be a deliberate choice.
     */
    private val messageDigestInstance: MessageDigest
) {

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

    /** Store a local file under the given id, returning what was written — empty when the write failed. */
    @Throws(StorageException::class)
    fun store(file: File, objectId: String): Optional<SavedFile> {
        try {
            val originalName = determineFilename(file.name)
            val destinationFilePath: Path = rootLocation.resolve("${objectId}.zip").normalize().toAbsolutePath()
            if (!destinationFilePath.parent.equals(rootLocation.toAbsolutePath())) {
                // This is a security check
                throw StorageException("Cannot store file outside current directory.")
            }
            FileUtils.copyFile(file, destinationFilePath.toFile())
            log.debug("Stored file to $destinationFilePath.")
            val sha256 = sha256Of(destinationFilePath.toFile())
            return Optional.of(
                SavedFile(
                    id = objectId,
                    sha256 = sha256,
                    file = destinationFilePath,
                    originalName = originalName,
                    size = destinationFilePath.toFile().length()
                )
            )
        } catch (e: IOException) {
            log.error("Error storing file: ", e)
            return Optional.empty()
        }
    }

    /**
     * Copy a file out of GridFS onto the filesystem, for an installation migrating away from database
     * storage, and report where it landed.
     *
     * Reports the path and nothing else because this runs on the download route, inside the request and
     * before any byte reaches the client: hashing the copy here is a second full pass over an archive
     * whose hash the only caller discards.
     */
    @Throws(StorageException::class)
    fun cache(file: GridFSFile, resource: GridFsResource): Optional<Path> {
        try {
            val id = file.objectId.toString()
            val destinationFilePath: Path = rootLocation.resolve("${id}.zip").normalize().toAbsolutePath()
            if (!destinationFilePath.parent.equals(rootLocation.toAbsolutePath())) {
                // This is a security check
                throw StorageException("Cannot store file outside current directory.")
            }
            FileUtils.copyToFile(resource.inputStream, destinationFilePath.toFile())
            log.debug("Stored file to $destinationFilePath.")
            return Optional.of(destinationFilePath)
        } catch (e: IOException) {
            log.error("Error storing file: ", e)
            return Optional.empty()
        }
    }

    /**
     * The SHA-256 of a file, streamed a buffer at a time.
     *
     * Reads rather than slurps: the previous `digest(file.readBytes())` materialised the whole archive
     * as a `ByteArray`, which the shipped 5000 MB multipart limit makes an `OutOfMemoryError` outright —
     * a Java array cannot exceed about 2 GB.
     *
     * Takes a fresh digest per call, deriving only the algorithm from [messageDigestInstance]. A
     * `MessageDigest` is stateful and not thread-safe, uploads run on request threads, and two
     * concurrent hashes sharing one instance interleave into two wrong answers — which for a hash the
     * duplicate-check keys on means a false rejection or a missed duplicate.
     */
    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance(messageDigestInstance.algorithm)
        file.inputStream().buffered().use { source ->
            val buffer = ByteArray(DIGEST_BUFFER_BYTES)
            var read = source.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = source.read(buffer)
            }
        }
        return String(Hex.encode(digest.digest()))
    }

    /**
     * The stored file for an id, empty when there is none.
     *
     * Resolves `$id.zip`, which is the name both [store] overloads write and [delete] removes. The scan
     * this replaced matched any entry whose whole *path* contained the id, so it returned the directory
     * `ConfigurationHandler.isZip` extracts beside the archive as readily as the archive -- `exists()`
     * is true for both, and the download route wrapped whichever came first in a `FileSystemResource`
     * -- and it listed the entire storage root on every download. The fallback keeps files predating
     * that naming readable, matching on the entry's own name and only ever on a regular file.
     */
    fun load(id: String): Optional<File> {
        val archive = rootLocation.resolve("$id.zip").normalize().toFile()
        if (archive.isFile) {
            return Optional.of(archive)
        }
        val legacy = rootLocation.listDirectoryEntries()
            .find { path -> path.fileName.toString().contains(id) && path.toFile().isFile }
            ?.normalize()?.toFile()
        if (legacy != null) {
            return Optional.of(legacy)
        }
        log.warn("Filesystem does not contain a file for $id.")
        return Optional.empty()
    }

    /** Delete one stored file. A file that is already gone is not an error. */
    fun delete(id: String) {
        FileSystemUtils.deleteRecursively(rootLocation.resolve("${id}.zip").normalize())
        FileSystemUtils.deleteRecursively(rootLocation.resolve(id).normalize())
    }

    private companion object {
        /** Read size for [sha256Of]. Large enough to keep syscalls down, small enough to stay off the heap. */
        const val DIGEST_BUFFER_BYTES = 8192
    }

    /** Delete everything under [rootLocation]. Used by the cleanup schedule, not by a request. */
    fun deleteAll() {
        for (path in rootLocation.listDirectoryEntries()) {
            FileSystemUtils.deleteRecursively(path)
        }
    }
}