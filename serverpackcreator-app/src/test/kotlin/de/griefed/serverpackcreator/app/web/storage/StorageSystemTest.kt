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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.Optional
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

/**
 * Characterization of [StorageSystem] — the seam the web layer stores through. Pins where an upload
 * lands, what reaches GridFS, and what a delete does and does not remove.
 */
class StorageSystemTest {

    @TempDir
    lateinit var tempDir: Path

    private val objectId = ObjectId("651f3c0e9a1b2c3d4e5f6071")
    private val gridFsTemplate: GridFsTemplate = mockk()
    private val gridFsOperations: GridFsOperations = mockk()

    private fun storageRoot(): Path = tempDir.resolve("modpacks").also { it.createDirectories() }

    private fun storageSystem(root: Path): StorageSystem {
        every { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) } returns objectId
        return StorageSystem(
            FileSystemStorageService(root, MessageDigest.getInstance("SHA-256")),
            gridFsTemplate,
            gridFsOperations
        )
    }

    @Test
    fun landingAnUploadWritesOnlyTheLandingCopy() {
        // Landing is deliberately not committing: nothing durable exists until store() is called, so a
        // caller can validate the archive first and walk away without having written to GridFS.
        val root = storageRoot()
        val upload = MockMultipartFile("file", "All The Mods 9.zip", "application/zip", ByteArray(64) { 3 })

        val landed = storageSystem(root).land(upload).get()

        val names = root.listDirectoryEntries().map { it.fileName.toString() }
        Assertions.assertEquals(1, names.size)
        Assertions.assertTrue(landed.name.endsWith("-orig-All The Mods 9.zip"), landed.name)
        verify(exactly = 0) { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) }
    }

    @Test
    fun committingALandedUploadNamesTheArchiveAfterTheMintedIdAndKeepsTheDisplayName() {
        val root = storageRoot()
        val storage = storageSystem(root)
        val landed = storage.land(
            MockMultipartFile("file", "All The Mods 9.zip", "application/zip", ByteArray(64) { 3 })
        ).get()

        val saved = storage.store(landed).get()

        Assertions.assertEquals("651f3c0e9a1b2c3d4e5f6071.zip", saved.file.fileName.toString())
        Assertions.assertEquals("All The Mods 9.zip", saved.originalName)
    }

    @Test
    fun committingWritesAFullCopyIntoGridFsAsWellAsToDisk() {
        // The object id GridFS mints is what names the file on disk, so the database write is not
        // optional: every committed byte exists twice, which is why delete has to reclaim both.
        val storage = storageSystem(storageRoot())
        val landed = storage.land(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8))).get()

        storage.store(landed)

        verify(exactly = 1) { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) }
    }

    @Test
    fun deletingRemovesBothCopiesOfTheFile() {
        // Every stored file exists twice -- on disk and in GridFS -- so a delete that reclaims one of
        // them leaves the database growing without bound, including for uploads that were rejected.
        val root = storageRoot()
        val storage = storageSystem(root)
        every { gridFsTemplate.delete(any()) } returns Unit
        storage.store(storage.land(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8))).get())

        storage.delete(objectId.toString())

        Assertions.assertFalse(root.resolve("$objectId.zip").toFile().exists())
        verify(exactly = 1) { gridFsTemplate.delete(any()) }
    }

    @Test
    fun loadReturnsTheArchiveWhenItIsOnDisk() {
        val root = storageRoot()
        val storage = storageSystem(root)
        storage.store(storage.land(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8))).get())

        Assertions.assertTrue(storage.load(objectId.toString()).isPresent)
    }

    @Test
    fun anUploadWhoseFilenameCarriesPathSegmentsStaysInsideTheStorageRoot() {
        // getOriginalFilename() is whatever the client put in Content-Disposition; Spring passes it
        // through verbatim, separators included. Nothing here checks it -- the only reason a traversal
        // does not land outside the root today is that "<millis>-orig-" concatenates without a
        // separator, so the first path component is a directory name that does not exist.
        val root = storageRoot()
        val upload = MockMultipartFile("file", "../../../escaped.zip", "application/zip", ByteArray(16) { 5 })

        val landed = storageSystem(root).land(upload)

        Assertions.assertTrue(landed.isPresent, "a hostile filename must not fail the upload outright")
        Assertions.assertTrue(
            landed.get().toPath().toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize()),
            "stored outside the root: ${landed.get()}"
        )
        Assertions.assertFalse(
            tempDir.resolve("escaped.zip").toFile().exists(),
            "a file escaped the storage root"
        )
    }

    @Test
    fun anUploadWhoseFilenameCarriesPathSegmentsKeepsOnlyItsBaseNameForDisplay() {
        val storage = storageSystem(storageRoot())
        val landed = storage.land(
            MockMultipartFile("file", "../../../escaped.zip", "application/zip", ByteArray(16))
        ).get()

        Assertions.assertEquals("escaped.zip", storage.store(landed).get().originalName)
    }

    @Test
    fun anUploadThatCannotBeWrittenIsReportedAsAnEmptyResultRatherThanThrowing() {
        // ModPackController catches StorageException only, so anything else escaping land() reaches the
        // caller as an unhandled 500.
        //
        // The failure is produced by making the storage ROOT a regular file, not by a hostile filename.
        // This guard used to send "sub/dir/pack.zip" and rely on transferTo failing on the unsanitised
        // path -- which it did, until the fix it guards reduced that name to "pack.zip" and the write
        // started succeeding. It then asserted assertDoesNotThrow against a call that simply worked, and
        // said so in a comment. A fixture that stops reproducing its own condition is worse than no
        // guard, because it reads like coverage.
        val root = tempDir.resolve("notADirectory").also { it.toFile().writeText("occupied") }
        val upload = MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(16))

        val landed = Assertions.assertDoesNotThrow<Optional<File>> { storageSystem(root).land(upload) }

        Assertions.assertTrue(landed.isEmpty, "an unwritable root must report empty, not partial success")
    }
}
