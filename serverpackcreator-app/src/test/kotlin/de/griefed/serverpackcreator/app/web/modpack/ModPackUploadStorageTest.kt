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
package de.griefed.serverpackcreator.app.web.modpack

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.api.config.ConfigCheck
import de.griefed.serverpackcreator.api.config.ConfigurationHandler
import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.storage.StorageException
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
import java.util.Optional
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

/**
 * Guards what an upload costs *before* it has been accepted.
 *
 * An upload that fails validation, or that is a duplicate of one already stored, must not leave a
 * durable trace: no GridFS document, no archive under the storage root, and no landing copy waiting
 * for a nightly cron to reclaim it. Storing first and checking afterwards makes a rejected upload
 * cost the same three writes as an accepted one, and the GridFS half of that is never reclaimed at
 * all, because a rejected upload never gets a ModPack row for the cleanup to work back from.
 */
class ModPackUploadStorageTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackRepository: ModPackRepository = mockk()
    private val configurationHandler: ConfigurationHandler = mockk()
    private val modPackDownloadRepository: ModPackDownloadRepository = mockk()
    private val serverPackRepository: ServerPackRepository = mockk()
    private val gridFsTemplate: GridFsTemplate = mockk()
    private val gridFsOperations: GridFsOperations = mockk()

    private lateinit var storageRoot: Path

    /** A service rooted at a fresh storage directory, with GridFS stubbed to mint a fixed id. */
    private fun service(): ModPackService {
        storageRoot = tempDir.resolve("modpacks").also { it.createDirectories() }
        val apiProperties: ApiProperties = mockk()
        every { apiProperties.modpacksDirectory } returns storageRoot.toFile()
        every { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) } returns
                ObjectId("651f3c0e9a1b2c3d4e5f6071")
        return ModPackService(
            modpackRepository, configurationHandler, modPackDownloadRepository,
            serverPackRepository, gridFsTemplate, gridFsOperations, apiProperties
        )
    }

    private fun upload() = MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(64) { 9 })

    /** Everything left under the storage root after the call. */
    private fun leftovers(): List<String> = storageRoot.listDirectoryEntries().map { it.fileName.toString() }

    @Test
    fun anUploadThatFailsValidationIsNeverWrittenToGridFs() {
        val service = service()
        every { configurationHandler.checkZipArchive(any(), any()) } returns
                ConfigCheck().apply { modpackErrors.add("not a valid modpack archive") }

        Assertions.assertThrows(StorageException::class.java) { service.saveUploadedFile(upload()) }

        verify(exactly = 0) { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) }
    }

    @Test
    fun anUploadThatFailsValidationLeavesNothingUnderTheStorageRoot() {
        val service = service()
        every { configurationHandler.checkZipArchive(any(), any()) } returns
                ConfigCheck().apply { modpackErrors.add("not a valid modpack archive") }

        Assertions.assertThrows(StorageException::class.java) { service.saveUploadedFile(upload()) }

        Assertions.assertEquals(
            emptyList<String>(), leftovers(),
            "a rejected upload left files behind, reclaimed only by the 00:30 cron"
        )
    }

    @Test
    fun aDuplicateUploadIsNotStoredASecondTime() {
        val service = service()
        every { configurationHandler.checkZipArchive(any(), any()) } returns ConfigCheck()
        val existing = ModPack().apply { assignEntityId(this, "alreadyHere") }
        every { modpackRepository.findFirstBySha256(any()) } returns Optional.of(existing)

        Assertions.assertThrows(StorageException::class.java) { service.saveUploadedFile(upload()) }

        verify(exactly = 0) { gridFsTemplate.store(any<InputStream>(), any<String>(), any<Any>()) }
        Assertions.assertEquals(emptyList<String>(), leftovers())
    }

    @Test
    fun anAcceptedUploadLeavesExactlyOneArchiveAndNoLandingCopy() {
        val service = service()
        every { configurationHandler.checkZipArchive(any(), any()) } returns ConfigCheck()
        every { modpackRepository.findFirstBySha256(any()) } returns Optional.empty()
        every { modpackRepository.save(any()) } returnsArgument 0

        val stored = service.saveUploadedFile(upload())

        Assertions.assertEquals("651f3c0e9a1b2c3d4e5f6071", stored.fileID)
        Assertions.assertEquals("pack.zip", stored.name)
        Assertions.assertEquals(
            listOf("651f3c0e9a1b2c3d4e5f6071.zip"), leftovers(),
            "an accepted upload should leave its archive and nothing else"
        )
    }
}
