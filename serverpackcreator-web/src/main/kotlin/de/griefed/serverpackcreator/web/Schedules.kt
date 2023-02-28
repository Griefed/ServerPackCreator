/* Copyright (C) 2023  Griefed
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
package de.griefed.serverpackcreator.web

import de.griefed.serverpackcreator.api.utilities.common.deleteQuietly
import de.griefed.serverpackcreator.api.versionmeta.VersionMeta
import de.griefed.serverpackcreator.web.serverpack.ServerPackModel
import de.griefed.serverpackcreator.web.serverpack.ServerPackService
import org.apache.logging.log4j.kotlin.cachedLoggerOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.sql.Timestamp
import java.util.*
import javax.xml.parsers.ParserConfigurationException

/**
 * Schedules to cover all kinds of aspects of ServerPackCreator.
 *
 * @author Griefed
 */
@Service
@Suppress("unused")
class Schedules @Autowired constructor(
    private val serverPackService: ServerPackService,
    private val versionMeta: VersionMeta
) {
    private val log = cachedLoggerOf(this.javaClass)

    /**
     * Check the database every
     * `de.griefed.serverpackcreator.spring.schedules.database.cleanup ` for validity. <br></br>
     * Deletes entries from the database which are older than 1 week and have 0 downloads. <br></br>
     * Deletes entries whose status is `Available` but no server pack ZIP-archive can be found.
     * <br></br>
     *
     * @author Griefed
     */
    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.database.cleanup}")
    private fun cleanDatabase() {
        if (serverPackService.getServerPacks().isNotEmpty()) {
            log.info("Cleaning database...")
            for (pack in serverPackService.getServerPacks()) {
                if (Timestamp(Date().time).time - pack.lastModified!!.time
                    >= 604800000
                    && pack.downloads == 0
                ) {
                    deletePack(pack)
                } else if (pack.status.equals("Available") && !File(pack.path!!).isFile) {
                    deletePack(pack)
                } else if (pack.status.equals("Generating")
                    && Timestamp(Date().time).time - pack.lastModified!!.time
                    >= 86400000
                ) {
                    deletePack(pack)
                } else {
                    log.info("No database entries to clean up.")
                }
            }
            log.info("Database cleanup completed.")
        }
    }

    private fun deletePack(pack: ServerPackModel) {
        log.info("Deleting archive ${pack.path}")
        File(pack.path!!).deleteQuietly()
        log.info("Deleting folder ${pack.path!!.replace(" _server_pack -zip", "")}")
        File(pack.path!!.replace("_server_pack-zip", "")).deleteQuietly()
        log.info("Cleaned server pack ${pack.path} from database.")
        serverPackService.deleteServerPack(pack.id)
    }

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.files.cleanup}")
    private fun cleanFiles() {
        if (serverPackService.getServerPacks().isNotEmpty()) {
            log.info("Cleaning files...")
            for (pack in serverPackService.getServerPacks()) {
                if (File(pack.path!!).isFile
                    && File(pack.path!!.replace("_server_pack-zip", "")).isDirectory
                ) {
                    log.info("Deleting folder ${pack.path!!.replace(" _server_pack -zip", "")}")
                    File(pack.path!!.replace("_server_pack-zip", "")).deleteQuietly()
                } else {
                    log.info("No files to clean up.")
                }
            }
            log.info("File cleanup completed.")
        }
    }

    @Scheduled(cron = "\${de.griefed.serverpackcreator.spring.schedules.versions.refresh}")
    private fun refreshVersionLister() {
        try {
            versionMeta.update()
        } catch (ex: IOException) {
            log.error("Could not update VersionMeta.", ex)
        } catch (ex: ParserConfigurationException) {
            log.error("Could not update VersionMeta.", ex)
        } catch (ex: SAXException) {
            log.error("Could not update VersionMeta.", ex)
        }
    }
}