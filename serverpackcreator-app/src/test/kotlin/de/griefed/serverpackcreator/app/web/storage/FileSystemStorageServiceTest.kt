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
import io.mockk.every
import io.mockk.mockk
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.data.mongodb.gridfs.GridFsResource
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

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
        // Bytes, not mebibytes: both SPA tables hide the download button when size is 0, so truncating
        // to an Int number of MiB makes every pack under a mebibyte undownloadable. The type must be a
        // Long as well -- the shipped 5000MB upload limit overflows an Int.
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

    /**
     * `ConfigurationHandler.isZip` extracts an archive into a sibling directory named after it, so the
     * storage root holds both `anId.zip` and `anId/`. A `load` that matched on *substring* could return
     * the directory -- which `exists()` reports as present -- and the download route then wrapped a
     * directory in a `FileSystemResource`. This is the case where the archive has already been swept
     * and only the extraction is left, so the wrong answer is the only answer a scan can give.
     */
    @Test
    fun loadNeverReturnsTheExtractedDirectoryInPlaceOfTheArchive() {
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        root.resolve("anId").createDirectories()

        Assertions.assertTrue(service(root).load("anId").isEmpty)
    }

    /** With both present, the archive is the answer -- whatever order the directory happens to list in. */
    @Test
    fun loadPrefersTheArchiveOverTheDirectoryExtractedBesideIt() {
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        val storage = service(root)
        root.resolve("anId").createDirectories()
        storage.store(sourceFile("whatever.zip"), "anId")

        Assertions.assertEquals(root.resolve("anId.zip").toFile(), storage.load("anId").get())
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

    /** A GridFS file and resource pair whose content stream is [content]. */
    private fun gridFsPair(objectId: ObjectId, content: InputStream): Pair<GridFSFile, GridFsResource> {
        val gridFsFile: GridFSFile = mockk()
        val resource: GridFsResource = mockk()
        every { gridFsFile.objectId } returns objectId
        every { resource.inputStream } returns content
        return gridFsFile to resource
    }

    /** Records whether it was closed, so a test can assert the GridFS download stream is released. */
    private class ClosingAwareStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    @Test
    fun recoveringFromGridFsWritesTheWholeArchiveAndClosesTheStream() {
        val objectId = ObjectId()
        val content = ClosingAwareStream(ByteArray(64) { it.toByte() })
        val (gridFsFile, resource) = gridFsPair(objectId, content)
        val storage = service()

        val cached = storage.cache(gridFsFile, resource).get()

        Assertions.assertArrayEquals(ByteArray(64) { it.toByte() }, cached.toFile().readBytes())
        Assertions.assertEquals(cached.toFile(), storage.load(objectId.toString()).get())
        Assertions.assertTrue(content.closed, "the GridFS download stream must be closed")
    }

    /**
     * A recovery that fails mid-copy must leave nothing `load` would serve: a partial `$id.zip` would be taken
     * by its fast path and served, truncated, from then on.
     */
    @Test
    fun aRecoveryThatFailsMidCopyLeavesNothingBehindToServe() {
        val objectId = ObjectId()
        val failing = object : InputStream() {
            private var served = 0
            override fun read(): Int {
                if (served == 32) throw IOException("connection to GridFS lost")
                served++
                return 7
            }
        }
        val (gridFsFile, resource) = gridFsPair(objectId, failing)
        val root = tempDir.resolve("storage").also { it.createDirectories() }
        val storage = service(root)

        Assertions.assertTrue(storage.cache(gridFsFile, resource).isEmpty)
        Assertions.assertTrue(storage.load(objectId.toString()).isEmpty)
        Assertions.assertEquals(emptyList<Path>(), root.listDirectoryEntries())
    }

    /**
     * While one request is still copying an archive out of GridFS, a concurrent request must not find a
     * half-written file under the id.
     */
    @Test
    fun aRecoveryInProgressIsInvisibleToLoad() {
        val objectId = ObjectId()
        val halfWritten = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stalling = object : InputStream() {
            private var served = 0
            override fun read(): Int {
                val single = ByteArray(1)
                return if (read(single, 0, 1) == -1) -1 else single[0].toInt()
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (served == 64) return -1
                if (served == 32) {
                    halfWritten.countDown()
                    release.await(10, TimeUnit.SECONDS)
                }
                val count = minOf(length, 32 - served % 32)
                buffer.fill(7, offset, offset + count)
                served += count
                return count
            }
        }
        val (gridFsFile, resource) = gridFsPair(objectId, stalling)
        val storage = service()
        var recovered: Optional<Path> = Optional.empty()
        val copier = Thread { recovered = storage.cache(gridFsFile, resource) }

        copier.start()
        Assertions.assertTrue(halfWritten.await(10, TimeUnit.SECONDS))
        val seenMidCopy = storage.load(objectId.toString())
        release.countDown()
        copier.join(10_000)

        Assertions.assertTrue(seenMidCopy.isEmpty, "a half-written archive was visible to load: $seenMidCopy")
        Assertions.assertEquals(64L, recovered.get().toFile().length())
    }
}
