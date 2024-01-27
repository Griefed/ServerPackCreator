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
package de.griefed.serverpackcreator.web.scheduling

import de.griefed.serverpackcreator.api.ApiProperties
import de.griefed.serverpackcreator.web.modpack.ModpackRepository
import de.griefed.serverpackcreator.web.modpack.ModpackService
import de.griefed.serverpackcreator.web.modpack.ModpackStatus
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@Service
class DatabaseCleanupSchedule @Autowired constructor(
    private val modpackRepository: ModpackRepository,
    private val modpackService: ModpackService,
    apiProperties: ApiProperties
) {
    private val log = cachedLoggerOf(this.javaClass)
    private val modPackRoot: Path = apiProperties.modpacksDirectory.toPath()

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.database.cleanup}")
    private fun cleanDatabase() {
        log.info("Cleaning database...")
        val modpackFiles = modPackRoot.listDirectoryEntries().map { it.toFile() }
        for (modpack in modpackRepository.findAll()) {
            if (modpack.status == ModpackStatus.ERROR) {
                modpackService.deleteModpack(modpack.id)
                log.info("Deleted Modpack: ${modpack.id}-${modpack.name}")
            } else if (modpackFiles.find { modpackFile -> modpackFile.name.contains(modpack.fileID!!.toString(), ignoreCase = true) } == null) {
                modpackService.deleteModpack(modpack.id)
                log.info("Deleted Modpack: ${modpack.id}-${modpack.name}")
            }
        }
        log.info("Database cleanup completed.")
    }
}