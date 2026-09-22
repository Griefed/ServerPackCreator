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
import java.io.InputStream
import java.nio.file.Path
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
    fun storingAnUploadKeepsBothTheTemporaryCopyAndTheFinalArchive() {
        val root = storageRoot()
        val upload = MockMultipartFile("file", "All The Mods 9.zip", "application/zip", ByteArray(64) { 3 })

        val saved = storageSystem(root).store(upload).get()

        val names = root.listDirectoryEntries().map { it.fileName.toString() }.sorted()
        // Two files for one upload: the "<millis>-orig-<name>" landing copy is never removed, and is
        // reclaimed only by FileCleanupSchedule at 00:30.
        Assertions.assertEquals(2, names.size)
        Assertions.assertEquals("651f3c0e9a1b2c3d4e5f6071.zip", names.first { !it.contains("-orig-") })
        Assertions.assertTrue(names.any { it.endsWith("-orig-All The Mods 9.zip") })
        Assertions.assertEquals("All The Mods 9.zip", saved.originalName)
    }

    @Test
    fun storingWritesAFullCopyIntoGridFsAsWellAsToDisk() {
        // The object id GridFS mints is what names the file on disk, so the database write is not
        // optional today: every stored byte exists twice.
        storageSystem(storageRoot()).store(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8)))

        verify(exactly = 1) { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) }
    }

    @Test
    fun deletingRemovesTheArchiveFromDiskButLeavesTheGridFsCopy() {
        val root = storageRoot()
        val storage = storageSystem(root)
        storage.store(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8)))

        storage.delete(objectId.toString())

        Assertions.assertFalse(root.resolve("$objectId.zip").toFile().exists())
        // No GridFS delete exists to call: DatabaseStorageService has store and load only.
        verify(exactly = 0) { gridFsOperations.delete(any()) }
    }

    @Test
    fun loadReturnsTheArchiveWhenItIsOnDisk() {
        val root = storageRoot()
        val storage = storageSystem(root)
        storage.store(MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8)))

        Assertions.assertTrue(storage.load(objectId.toString()).isPresent)
    }

}
