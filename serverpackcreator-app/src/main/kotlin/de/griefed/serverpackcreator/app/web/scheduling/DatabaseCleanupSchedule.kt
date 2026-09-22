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
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.modpack.ModPackStatus
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

/**
 * Removes database rows whose file is gone — the opposite direction from `FileCleanupSchedule`.
 * 
 * Runs on a cron, disabled in tests: a suite running at the scheduled minute against an unreachable database
 * should not get to find out what this does.
 */
@Suppress("unused")
@Service
class DatabaseCleanupSchedule @Autowired constructor(
    private val modpackRepository: ModPackRepository,
    private val modpackService: ModPackService,
    private val serverPackRepository: ServerPackRepository,
    apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val modPackRoot: Path = apiProperties.modpacksDirectory.toPath()
    private val serverPackRoot: Path = apiProperties.serverPacksDirectory.toPath()

    @Scheduled(cron = $$"${de.griefed.serverpackcreator.spring.schedules.database.cleanup}")
    private fun cleanDatabase() {
        log.info("Cleaning database...")
        val modpackFiles = modPackRoot.listDirectoryEntries().map { it.toFile() }
        for (modpack in modpackRepository.findAll()) {
            // A row with no fileID never had an archive, so it is as orphaned as one whose archive is
            // gone. Reading it through a local val rather than `!!` is the point: one such row used to
            // end the whole pass, partway through, after other rows had already been deleted.
            val fileID = modpack.fileID
            val archiveIsGone = fileID == null ||
                    modpackFiles.none { modpackFile -> modpackFile.name.contains(fileID, ignoreCase = true) }
            if (modpack.status == ModPackStatus.ERROR || archiveIsGone) {
                modpackService.deleteModpack(modpack.id!!)
                log.info("Deleted Modpack: ${modpack.id}-${modpack.name}")
            }
        }

        val serverPackFiles = serverPackRoot.listDirectoryEntries().map { it.toFile() }
        for (serverpack in serverPackRepository.findAll()) {
            // A server pack has no fileID until its generation finishes, so skipping those is not just
            // null-safety -- deleting one would remove a pack that is still being built.
            val fileID = serverpack.fileID ?: continue
            if (serverPackFiles.any { serverPackFile -> serverPackFile.name.contains(fileID, ignoreCase = true) }) {
                continue
            }
            val modpack = modpackService.getByServerPack(serverpack)
            if (modpack.isPresent) {
                modpack.get().serverPacks.removeIf { pack -> pack.id == serverpack.id }
                modpackService.saveModpack(modpack.get())
            }
            serverPackRepository.delete(serverpack)
            log.info("Deleted Server Pack ${serverpack.id}, whose archive is gone.")
        }
        log.info("Database cleanup completed.")
    }
}