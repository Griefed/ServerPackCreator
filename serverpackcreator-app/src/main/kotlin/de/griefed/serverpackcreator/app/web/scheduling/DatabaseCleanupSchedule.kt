/* Copyright (C) 2024  Griefed
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

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.database.cleanup}")
    private fun cleanDatabase() {
        log.info("Cleaning database...")
        val modpackFiles = modPackRoot.listDirectoryEntries().map { it.toFile() }
        for (modpack in modpackRepository.findAll()) {
            if (modpack.status == ModPackStatus.ERROR) {
                modpackService.deleteModpack(modpack.id!!)
                log.info("Deleted Modpack: ${modpack.id}-${modpack.name}")
            } else if (modpackFiles.find { modpackFile -> modpackFile.name.contains(modpack.fileID!!, ignoreCase = true) } == null) {
                modpackService.deleteModpack(modpack.id!!)
                log.info("Deleted Modpack: ${modpack.id}-${modpack.name}")
            }
        }

        val serverPackFiles = serverPackRoot.listDirectoryEntries().map { it.toFile() }
        for (serverpack in serverPackRepository.findAll()) {
            if (serverPackFiles.find { serverPackFile -> serverPackFile.name.contains(serverpack.fileID!!, ignoreCase = true) } == null) {
                val modpack = modpackService.getByServerPack(serverpack)
                modpack.get().serverPacks.removeIf { pack -> pack.id == serverpack.id }
                modpackService.saveModpack(modpack.get())
                serverPackRepository.delete(serverpack)
                log.info("Deleted Server Pack ${serverpack.id} from modpack ${modpack.get().id}-${modpack.get().name}")
            }
        }
        log.info("Database cleanup completed.")
    }
}