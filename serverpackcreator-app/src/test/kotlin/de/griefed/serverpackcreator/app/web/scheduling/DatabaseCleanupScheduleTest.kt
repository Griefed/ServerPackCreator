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
package de.griefed.serverpackcreator.app.web.scheduling

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.app.web.assignEntityId
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Characterization of [DatabaseCleanupSchedule]: it removes rows whose file is gone, and every row
 * left in `ERROR`. The opposite direction from [FileCleanupSchedule], and the only deletion mechanism
 * the webservice documents.
 *
 * `cleanDatabase` is private — Spring invokes it reflectively through `@Scheduled`, and so does this.
 */
class DatabaseCleanupScheduleTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackRepository: ModPackRepository = mockk()
    private val modpackService: ModPackService = mockk()
    private val serverPackRepository: ServerPackRepository = mockk()

    private lateinit var modpackRoot: Path

    /** Builds the schedule over fresh directories, with the modpack repository answering [modpacks]. */
    private fun schedule(modpacks: List<ModPack>): DatabaseCleanupSchedule {
        modpackRoot = tempDir.resolve("modpacks").also { it.createDirectories() }
        val apiProperties: ApiProperties = mockk()
        every { apiProperties.modpacksDirectory } returns modpackRoot.toFile()
        every { apiProperties.serverPacksDirectory } returns
                tempDir.resolve("server-packs").also { it.createDirectories() }.toFile()
        every { modpackRepository.findAll() } returns modpacks
        every { serverPackRepository.findAll() } returns emptyList()
        justRun { modpackService.deleteModpack(any()) }
        return DatabaseCleanupSchedule(modpackRepository, modpackService, serverPackRepository, apiProperties)
    }

    /** Runs the private, `@Scheduled` sweep. */
    private fun sweep(schedule: DatabaseCleanupSchedule) {
        DatabaseCleanupSchedule::class.java.getDeclaredMethod("cleanDatabase")
            .apply { isAccessible = true }
            .invoke(schedule)
    }

    /** A persisted-looking [ModPack] with [id], [fileID] and [status]. */
    private fun modPack(id: String, fileID: String, status: ModPackStatus = ModPackStatus.GENERATED): ModPack =
        ModPack().apply {
            assignEntityId(this, id)
            this.fileID = fileID
            this.status = status
        }

    @Test
    fun aModpackWhoseArchiveIsStillOnDiskIsKept() {
        val schedule = schedule(listOf(modPack("packId", "fileId")))
        modpackRoot.resolve("fileId.zip").toFile().writeText("still here")

        sweep(schedule)

        verify(exactly = 0) { modpackService.deleteModpack(any()) }
    }

    @Test
    fun aModpackWhoseArchiveIsGoneIsDeleted() {
        val schedule = schedule(listOf(modPack("packId", "fileId")))

        sweep(schedule)

        verify(exactly = 1) { modpackService.deleteModpack("packId") }
    }

    @Test
    fun everyModpackLeftInErrorIsDeletedEvenWhenItsArchiveIsStillThere() {
        val schedule = schedule(listOf(modPack("packId", "fileId", ModPackStatus.ERROR)))
        modpackRoot.resolve("fileId.zip").toFile().writeText("still here")

        sweep(schedule)

        verify(exactly = 1) { modpackService.deleteModpack("packId") }
    }

    @Test
    fun aModpackStillGeneratingIsKeptWhileItsArchiveExists() {
        // The queue re-submits a TaskDetail per stage; a pack mid-flight must survive the sweep.
        val schedule = schedule(listOf(modPack("packId", "fileId", ModPackStatus.GENERATING)))
        modpackRoot.resolve("fileId.zip").toFile().writeText("still here")

        sweep(schedule)

        verify(exactly = 0) { modpackService.deleteModpack(any()) }
        Assertions.assertTrue(modpackRoot.resolve("fileId.zip").toFile().exists())
    }
}
