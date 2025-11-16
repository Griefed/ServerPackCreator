/* Copyright (C) 2025 Griefed
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
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

@Suppress("unused")
@Service
class FileCleanupSchedule @Autowired constructor(
    private val modpackRepository: ModPackRepository,
    private val serverPackRepository: ServerPackRepository,
    apiProperties: ApiProperties
) {
    private val log by lazy { cachedLoggerOf(this.javaClass) }
    private val modPackRoot: Path = apiProperties.modpacksDirectory.toPath()
    private val serverPackRoot: Path = apiProperties.serverPacksDirectory.toPath()

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.files.cleanup}")
    private fun cleanFiles() {
        log.info("Cleaning files...")
        val modpackFiles = modPackRoot.listDirectoryEntries().map { it.toFile() }
        val modpackFileIDs = modpackRepository.findAll().map { it.fileID!! }
        for (file in modpackFiles) {
            if (!modpackFileIDs.any { fileId -> file.name.contains(fileId, ignoreCase = true) }) {
                file.deleteQuietly()
                log.info("Deleted ${file.absolutePath} as it didn't have a corresponding modpack.")
            }
        }

        val serverPackFiles = serverPackRoot.listDirectoryEntries().map { it.toFile() }
        val serverPackFileIDs = serverPackRepository.findAll().filter { it.fileID != null }.map { it.fileID!! }
        for (file in serverPackFiles) {
            if (!serverPackFileIDs.any { fileId -> file.name.contains(fileId, ignoreCase = true) }) {
                file.deleteQuietly()
                log.info("Deleted ${file.absolutePath} as it didn't have a corresponding modpack.")
            }
        }
        log.info("File cleanup completed.")
    }
}