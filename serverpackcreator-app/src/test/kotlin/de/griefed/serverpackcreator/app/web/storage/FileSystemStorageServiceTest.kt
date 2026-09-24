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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories

/**
 * Characterization of [FileSystemStorageService]: what it names, what it hashes, what it reports and
 * what it deletes. Written before the storage rework, so the rework can be checked against it rather
 * than against a reading of the code.
 */
class FileSystemStorageServiceTest {

    @TempDir
    lateinit var tempDir: Path

    /** The service under test, rooted at a fresh `storage` directory inside the per-test temp dir. */
    private fun service(root: Path = tempDir.resolve("storage").also { it.createDirectories() }) =
        FileSystemStorageService(root, MessageDigest.getInstance("SHA-256"))

    /** A source file of [bytes] length, named [name], outside the storage root. */
    private fun sourceFile(name: String, bytes: Int = 16): File {
        val source = tempDir.resolve("incoming").also { it.createDirectories() }.resolve(name).toFile()
        source.writeBytes(ByteArray(bytes) { 7 })
        return source
    }

    @Test
    fun storeWritesTheFileUnderTheObjectIdWithAZipExtension() {
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        val saved = service(root).store(sourceFile("whatever.zip"), "651f3c0e9a1b2c3d4e5f6071").get()

        Assertions.assertEquals(root.resolve("651f3c0e9a1b2c3d4e5f6071.zip").toAbsolutePath(), saved.file)
        Assertions.assertTrue(saved.file.toFile().exists())
        Assertions.assertEquals("651f3c0e9a1b2c3d4e5f6071", saved.id)
    }

    @Test
    fun storeReportsTheSha256OfTheStoredBytes() {
        val source = sourceFile("whatever.zip", bytes = 32)
        val expected = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
            .joinToString("") { "%02x".format(it) }

        Assertions.assertEquals(expected, service().store(source, "anId").get().sha256)
    }

    @Test
    fun storeRecoversTheOriginalNameFromTheUploadPrefix() {
        // StorageSystem names an upload "<millis>-orig-<clientFilename>"; determineFilename is how the
        // display name is recovered from it.
        val saved = service().store(sourceFile("1700000000000-orig-All The Mods 9.zip"), "anId").get()

        Assertions.assertEquals("All The Mods 9.zip", saved.originalName)
    }

    @Test
    fun storeKeepsTheFileNameWhenThereIsNoUploadPrefix() {
        Assertions.assertEquals(
            "generated_server_pack.zip",
            service().store(sourceFile("generated_server_pack.zip"), "anId").get().originalName
        )
    }

    @Test
    fun storeReportsSizeInBytesAsItsNameAndDocsSay() {
        // It used to divide by 1048576 and truncate to an Int, so everything under a mebibyte reported
        // 0 -- and both SPA tables hide the download button when size is 0, which made small packs
        // undownloadable. Bytes also need a Long: the shipped 5000MB upload limit overflows an Int.
        val underOneMebibyte = service().store(sourceFile("small.zip", bytes = 1_048_575), "small").get()
        val overTwoMebibytes = service().store(sourceFile("big.zip", bytes = 2_200_000), "big").get()

        Assertions.assertEquals(1_048_575L, underOneMebibyte.size)
        Assertions.assertEquals(2_200_000L, overTwoMebibytes.size)
    }

    @Test
    fun loadFindsAStoredFileByItsId() {
        val storage = service()
        storage.store(sourceFile("whatever.zip"), "651f3c0e9a1b2c3d4e5f6071")

        Assertions.assertTrue(storage.load("651f3c0e9a1b2c3d4e5f6071").isPresent)
    }

    @Test
    fun loadIsEmptyForAnUnknownId() {
        Assertions.assertTrue(service().load("nothingStoredUnderThis").isEmpty)
    }

    @Test
    fun deleteRemovesBothTheArchiveAndTheDirectoryExtractedBesideIt() {
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        val storage = service(root)
        storage.store(sourceFile("whatever.zip"), "anId")
        // ConfigurationHandler.isZip extracts the archive to a sibling directory named after it.
        val extracted = root.resolve("anId").toFile()
        Assertions.assertTrue(extracted.mkdirs())
        File(extracted, "manifest.json").writeText("{}")

        storage.delete("anId")

        Assertions.assertFalse(root.resolve("anId.zip").toFile().exists())
        Assertions.assertFalse(extracted.exists())
    }

    @Test
    fun deleteAllEmptiesTheRootButKeepsIt() {
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        val storage = service(root)
        storage.store(sourceFile("whatever.zip"), "anId")

        storage.deleteAll()

        Assertions.assertTrue(root.toFile().exists())
        Assertions.assertEquals(0, root.toFile().listFiles()!!.size)
    }
}
