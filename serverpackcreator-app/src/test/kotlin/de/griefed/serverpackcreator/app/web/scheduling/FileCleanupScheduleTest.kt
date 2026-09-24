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
import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
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
 * Characterization of [FileCleanupSchedule]: it deletes files no row refers to. The direction is the
 * dangerous one, so what it *keeps* matters as much as what it removes.
 *
 * `cleanFiles` is private — Spring invokes it reflectively through `@Scheduled`, and so does this.
 */
class FileCleanupScheduleTest {

    @TempDir
    lateinit var tempDir: Path

    private val modpackRepository: ModPackRepository = mockk()
    private val serverPackRepository: ServerPackRepository = mockk()
    private val modpackService: ModPackService = mockk()
    private val serverPackService: ServerPackService = mockk()

    private lateinit var modpackRoot: Path
    private lateinit var serverPackRoot: Path

    /** Builds the schedule over two fresh directories, with the repositories answering [modpacks]/[serverPacks]. */
    private fun schedule(modpacks: List<ModPack>, serverPacks: List<ServerPack>): FileCleanupSchedule {
        modpackRoot = tempDir.resolve("modpacks").also { it.createDirectories() }
        serverPackRoot = tempDir.resolve("server-packs").also { it.createDirectories() }
        val apiProperties: ApiProperties = mockk()
        every { apiProperties.modpacksDirectory } returns modpackRoot.toFile()
        every { apiProperties.serverPacksDirectory } returns serverPackRoot.toFile()
        every { modpackRepository.findAll() } returns modpacks
        every { serverPackRepository.findAll() } returns serverPacks
        justRun { modpackService.deleteStoredFile(any()) }
        justRun { serverPackService.deleteStoredFile(any()) }
        return FileCleanupSchedule(
            modpackRepository, serverPackRepository, modpackService, serverPackService, apiProperties
        )
    }

    /** Runs the private, `@Scheduled` sweep. */
    private fun sweep(schedule: FileCleanupSchedule) {
        FileCleanupSchedule::class.java.getDeclaredMethod("cleanFiles")
            .apply { isAccessible = true }
            .invoke(schedule)
    }

    /** A [ModPack] carrying [fileID], since the entity's own setter is the only way in. */
    private fun modPack(fileID: String?): ModPack = ModPack().apply { this.fileID = fileID }

    /** A [ServerPack] carrying [fileID]. */
    private fun serverPack(fileID: String?): ServerPack =
        ServerPack(0, null, fileID, "pack.zip", "sha", "modpackId")

    @Test
    fun aFileWhoseIdNoModpackRefersToIsDeleted() {
        val schedule = schedule(modpacks = listOf(modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("orphaned.zip").toFile().writeText("orphan")

        sweep(schedule)

        Assertions.assertTrue(modpackRoot.resolve("keepThisOne.zip").toFile().exists())
        Assertions.assertFalse(modpackRoot.resolve("orphaned.zip").toFile().exists())
    }

    @Test
    fun theExtractedDirectoryBesideAnArchiveIsKeptBecauseItsNameCarriesTheSameId() {
        val schedule = schedule(modpacks = listOf(modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("keepThisOne").toFile().mkdirs()

        sweep(schedule)

        Assertions.assertTrue(modpackRoot.resolve("keepThisOne").toFile().exists())
    }

    @Test
    fun aLandingCopyLeftBehindByACrashIsSweptBecauseItsNameCarriesNoId() {
        // saveUploadedFile now deletes its own "<millis>-orig-<name>" copy in a finally block, so this
        // is no longer the routine path -- it is the backstop for a crash between landing and cleanup.
        val schedule = schedule(modpacks = listOf(modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("1700000000000-orig-All The Mods 9.zip").toFile().writeText("landing copy")

        sweep(schedule)

        Assertions.assertFalse(modpackRoot.resolve("1700000000000-orig-All The Mods 9.zip").toFile().exists())
    }

    @Test
    fun serverPackFilesAreSweptOnTheSameRule() {
        val schedule = schedule(modpacks = emptyList(), serverPacks = listOf(serverPack("keptPack")))
        serverPackRoot.resolve("keptPack.zip").toFile().writeText("kept")
        serverPackRoot.resolve("orphaned.zip").toFile().writeText("orphan")

        sweep(schedule)

        Assertions.assertTrue(serverPackRoot.resolve("keptPack.zip").toFile().exists())
        Assertions.assertFalse(serverPackRoot.resolve("orphaned.zip").toFile().exists())
    }

    @Test
    fun aServerPackRowWithoutAFileIdIsSkippedRatherThanFailingTheSweep() {
        // The server-pack branch filters nulls before dereferencing; the modpack branch above it does not.
        val schedule = schedule(modpacks = emptyList(), serverPacks = listOf(serverPack(null), serverPack("keptPack")))
        serverPackRoot.resolve("keptPack.zip").toFile().writeText("kept")

        sweep(schedule)

        Assertions.assertTrue(serverPackRoot.resolve("keptPack.zip").toFile().exists())
    }

    @Test
    fun anOrphanedArchiveIsDeletedThroughStorageSoItsGridFsTwinGoesWithIt() {
        // Every stored file exists on disk and in GridFS. Unlinking the file reclaims one of the two,
        // so an orphan swept here would leave its database copy behind forever.
        val schedule = schedule(modpacks = listOf(modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("651f3c0e9a1b2c3d4e5f6071.zip").toFile().writeText("orphan")

        sweep(schedule)

        verify(exactly = 1) { modpackService.deleteStoredFile("651f3c0e9a1b2c3d4e5f6071") }
    }

    @Test
    fun aModpackRowWithoutAFileIdDoesNotAbortTheSweep() {
        // The server-pack branch filtered nulls before dereferencing; the modpack branch above it did
        // not, so one row with a null fileID took the whole nightly pass down with an NPE.
        val schedule = schedule(modpacks = listOf(modPack(null), modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("orphaned.zip").toFile().writeText("orphan")

        sweep(schedule)

        Assertions.assertTrue(modpackRoot.resolve("keepThisOne.zip").toFile().exists())
        Assertions.assertFalse(modpackRoot.resolve("orphaned.zip").toFile().exists())
    }

    @Test
    fun aRepositoryThatReturnsNoRowsAtAllDoesNotWipeTheDirectory() {
        // The sweep deletes what no row refers to, so "no rows" means "every file is an orphan". That is
        // correct for a genuinely empty installation and catastrophic for one pointed at the wrong
        // database -- and this project has already shipped a build that silently used Mongo's default
        // `test` database. A destructive nightly job should not be the thing that discovers it.
        //
        // Asserted on the delete calls, not on the files: deleteStoredFile is mocked here, so "the file
        // is still there" would pass even if the sweep had asked for it to be deleted.
        val schedule = schedule(modpacks = emptyList(), serverPacks = emptyList())
        modpackRoot.resolve("651f3c0e9a1b2c3d4e5f6071.zip").toFile().writeText("a real modpack")
        modpackRoot.resolve("1700000000000-orig-pack.zip").toFile().writeText("a landing copy")
        serverPackRoot.resolve("651f3c0e9a1b2c3d4e5f6072.zip").toFile().writeText("a real server pack")

        sweep(schedule)

        verify(exactly = 0) { modpackService.deleteStoredFile(any()) }
        verify(exactly = 0) { serverPackService.deleteStoredFile(any()) }
        Assertions.assertTrue(modpackRoot.resolve("1700000000000-orig-pack.zip").toFile().exists())
        Assertions.assertTrue(serverPackRoot.resolve("651f3c0e9a1b2c3d4e5f6072.zip").toFile().exists())
    }

    @Test
    fun anOrphanWithBothAnArchiveAndAnExtractedDirectoryIsReclaimedOnce() {
        // deleteStored removes the archive, the extracted directory and the GridFS document in one
        // call, so seeing both entries must not repeat it.
        val schedule = schedule(modpacks = listOf(modPack("keepThisOne")), serverPacks = emptyList())
        modpackRoot.resolve("keepThisOne.zip").toFile().writeText("kept")
        modpackRoot.resolve("651f3c0e9a1b2c3d4e5f6071.zip").toFile().writeText("orphan")
        modpackRoot.resolve("651f3c0e9a1b2c3d4e5f6071").toFile().mkdirs()

        sweep(schedule)

        verify(exactly = 1) { modpackService.deleteStoredFile("651f3c0e9a1b2c3d4e5f6071") }
    }
}
