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
import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

/**
 * Deletes stored files no database row refers to any more, reclaiming the disk an aborted upload left behind.
 * 
 * **The direction is the danger.** It deletes files whose ids are absent from the database, so it must never run
 * against a database it cannot read — which is why the schedules are disabled in the test context rather than
 * left on their crons.
 */
@Suppress("unused")
@Service
class FileCleanupSchedule @Autowired constructor(
    private val modpackRepository: ModPackRepository,
    private val serverPackRepository: ServerPackRepository,
    private val modpackService: ModPackService,
    private val serverPackService: ServerPackService,
    apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val modPackRoot: Path = apiProperties.modpacksDirectory.toPath()
    private val serverPackRoot: Path = apiProperties.serverPacksDirectory.toPath()

    @Scheduled(cron = $$"${de.griefed.serverpackcreator.spring.schedules.files.cleanup}")
    private fun cleanFiles() {
        log.info("Cleaning files...")
        val modpackFileIDs = modpackRepository.findAll().mapNotNull { it.fileID }
        sweep(modPackRoot, modpackFileIDs, "modpack") { id -> modpackService.deleteStoredFile(id) }

        val serverPackFileIDs = serverPackRepository.findAll().mapNotNull { it.fileID }
        sweep(serverPackRoot, serverPackFileIDs, "server pack") { id -> serverPackService.deleteStoredFile(id) }
        log.info("File cleanup completed.")
    }

    /**
     * Delete everything under [root] whose name carries none of [knownFileIDs].
     *
     * A file named after a storage id goes through [deleteStored] rather than being unlinked, because
     * every stored file is written to the filesystem *and* to GridFS, and unlinking reclaims only one
     * of the two. Anything else — a landing copy left by a crash — is simply removed.
     */
    private fun sweep(root: Path, knownFileIDs: List<String>, what: String, deleteStored: (String) -> Unit) {
        for (file in root.listDirectoryEntries().map { it.toFile() }) {
            if (knownFileIDs.any { fileId -> file.name.contains(fileId, ignoreCase = true) }) {
                continue
            }
            val storageId = file.name.removeSuffix(".zip")
            if (storageId.matches(storageIdPattern)) {
                deleteStored(storageId)
            } else {
                file.deleteQuietly()
            }
            log.info("Deleted ${file.absolutePath} as it didn't have a corresponding $what.")
        }
    }

    private companion object {
        /**
         * A GridFS `ObjectId` as it appears in a stored file's name: 24 hexadecimal characters. Only a
         * file named this way has a database twin worth reclaiming.
         */
        val storageIdPattern = "[0-9a-fA-F]{24}".toRegex()
    }
}