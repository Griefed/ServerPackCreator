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
package de.griefed.serverpackcreator.app.web.stats

import de.griefed.serverpackcreator.app.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownload
import de.griefed.serverpackcreator.app.web.stats.creation.AmountPerDate
import de.griefed.serverpackcreator.app.web.stats.creation.CreationStatsService
import de.griefed.serverpackcreator.app.web.stats.disk.DiskStatsData
import de.griefed.serverpackcreator.app.web.stats.disk.DiskStatsService
import de.griefed.serverpackcreator.app.web.stats.downloads.DownloadStatsService
import de.griefed.serverpackcreator.app.web.stats.packs.AmountStatsData
import de.griefed.serverpackcreator.app.web.stats.packs.AmountStatsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/stats")
class StatsController @Autowired constructor(
    private val downloadStatsService: DownloadStatsService,
    private val diskStatsService: DiskStatsService,
    private val creationStatsService: CreationStatsService,
    private val amountStatsService: AmountStatsService
) {

    @GetMapping("/downloads/modpacks")
    @ResponseBody
    fun modPackDownloads(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.modPackDownloads()
        )
    }

    @GetMapping("/downloads/serverpacks")
    @ResponseBody
    fun serverPackDownloads(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.serverPackDownloads()
        )
    }

    @GetMapping("/downloads/modpacks/history")
    @ResponseBody
    fun modPackDownloadHistory(): ResponseEntity<List<ModPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.allModPackDownloadsHistory()
        )
    }

    @GetMapping("/downloads/serverpacks/history")
    @ResponseBody
    fun serverPackDownloadHistory(): ResponseEntity<List<ServerPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.allServerPackDownloadsHistory()
        )
    }

    @GetMapping("/downloads/modpacks/{modPackID}")
    @ResponseBody
    fun allDownloadsForModPack(@PathVariable modPackID: Int): ResponseEntity<List<ModPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.downloadHistoryForModPack(modPackID)
        )
    }

    @GetMapping("/downloads/modpacks/{serverPackID}")
    @ResponseBody
    fun allDownloadsForServerPack(@PathVariable serverPackID: Int): ResponseEntity<List<ServerPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.downloadHistoryForServerPack(serverPackID)
        )
    }

    @GetMapping("/disk")
    @ResponseBody
    fun diskStats(): ResponseEntity<List<DiskStatsData>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            diskStatsService.stats
        )
    }

    @GetMapping("/creation/modpacks")
    @ResponseBody
    fun modPacksCreationTimes(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            creationStatsService.getModpackTimeStamps()
        )
    }

    @GetMapping("/creation/serverpacks")
    @ResponseBody
    fun serverPacksCreationTimes(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            creationStatsService.getServerPackTimeStamps()
        )
    }

    @GetMapping("/packs")
    @ResponseBody
    fun amountStats(): ResponseEntity<AmountStatsData> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            amountStatsService.stats
        )
    }
}