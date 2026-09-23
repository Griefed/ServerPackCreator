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
package de.griefed.serverpackcreator.app.web

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackController
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackController
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import de.griefed.serverpackcreator.app.web.serverpack.runconfiguration.RunConfigurationService
import de.griefed.serverpackcreator.app.web.task.TaskExecutionServiceImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.nio.file.Path
import java.util.Optional

/**
 * Guards that an archive is never carried through the heap on its way in or out.
 *
 * `spring.servlet.multipart.max-file-size` ships at **5000MB**, and a Java array cannot hold more than
 * about 2 GB — so any path that materialises a whole modpack as a `ByteArray` is an OutOfMemoryError
 * on a large pack, and several concurrent medium ones. These call the controllers directly rather than
 * through MockMvc, because the point is what the controller itself touches.
 */
class ArchiveStreamingTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackService: ModPackService = mockk()
    private val serverPackService: ServerPackService = mockk()
    private val runConfigurationService: RunConfigurationService = mockk()
    private val taskExecutionService: TaskExecutionServiceImpl = mockk()

    /** An archive on disk, standing in for a stored modpack or server pack. */
    private fun archive(): File = tempDir.resolve("archive.zip").toFile().apply { writeBytes(ByteArray(64) { 4 }) }

    @Test
    fun downloadingAModpackStreamsTheArchiveInsteadOfCopyingItIntoTheHeap() {
        val controller = ModPackController(modpackService, runConfigurationService, taskExecutionService)
        val modpack = ModPack().apply { assignEntityId(this, "known"); name = "Test Pack" }
        every { modpackService.getModpack("known") } returns Optional.of(modpack)
        every { modpackService.getModPackArchive(modpack) } returns Optional.of(archive())
        every { modpackService.updateDownloadStats(any()) } returns Optional.of(modpack)

        val body = controller.downloadModpack("known").body

        Assertions.assertFalse(body is ByteArrayResource, "the whole archive was read into memory to serve it")
        Assertions.assertInstanceOf(FileSystemResource::class.java, body)
    }

    @Test
    fun downloadingAServerPackStreamsTheArchiveInsteadOfCopyingItIntoTheHeap() {
        val controller = ServerPackController(serverPackService, modpackService)
        val serverPack = ServerPack(0, null, "fileId", "pack", "sha", "modpackId")
            .apply { assignEntityId(this, "known") }
        every { serverPackService.getServerPack("known") } returns Optional.of(serverPack)
        every { serverPackService.getServerPackArchive(serverPack) } returns Optional.of(archive())
        every { serverPackService.updateDownloadStats("known") } returns Optional.of(serverPack)

        val body = controller.downloadServerPack("known").body

        Assertions.assertFalse(body is ByteArrayResource, "the whole archive was read into memory to serve it")
        Assertions.assertInstanceOf(FileSystemResource::class.java, body)
    }

    @Test
    fun rejectingAnEmptyUploadDoesNotReadTheUploadIntoMemory() {
        // `file.size == 0L` already answers "is this empty". Calling getBytes() as well materialises
        // the entire upload, and does so on every non-empty upload because of the || short-circuit.
        val controller = ModPackController(modpackService, runConfigurationService, taskExecutionService)
        val upload = object : MockMultipartFile("file", "pack.zip", "application/zip", ByteArray(8)) {
            override fun getBytes(): ByteArray =
                throw AssertionError("the controller read the whole upload into memory to check it was not empty")
        }

        // Empty version fields, so the guard short-circuits before any service is touched.
        val response = controller.uploadModPack(upload, "", "", "", "", "", "")

        Assertions.assertTrue(response.statusCode.is4xxClientError)
    }
}
