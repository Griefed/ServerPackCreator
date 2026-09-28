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
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*

/**
 * Read-only statistics for the SPA's dashboard: download counts, download history, creation times, disk usage
 * and the plain counts. Every route delegates to a service; nothing is computed here.
 */
@Suppress("unused")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v2/stats")
class StatsController @Autowired constructor(
    private val downloadStatsService: DownloadStatsService,
    private val diskStatsService: DiskStatsService,
    private val creationStatsService: CreationStatsService,
    private val amountStatsService: AmountStatsService
) {

    /** Modpack downloads per day, whole series. */
    @GetMapping("/downloads/modpacks")
    @ResponseBody
    fun modPackDownloads(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.modPackDownloads()
        )
    }

    /** Modpack downloads per day, one page at a time — for a series long enough that the whole of it is a bad response. */
    @GetMapping("/downloads/modpackspaginated", produces = ["application/json"])
    @ResponseBody
    fun modPackDownloadsPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<AmountPerDate>> {
        val sizedPage = PageRequest.of(page, size)
        val paged = downloadStatsService.modPackDownloads(sizedPage)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            paged
        )
    }

    /** Server-pack downloads per day, whole series. */
    @GetMapping("/downloads/serverpacks")
    @ResponseBody
    fun serverPackDownloads(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.serverPackDownloads()
        )
    }

    /** Server-pack downloads per day, paginated like [modPackDownloadsPaginated]. */
    @GetMapping("/downloads/serverpackspaginated", produces = ["application/json"])
    @ResponseBody
    fun serverPackDownloadsPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<AmountPerDate>> {
        val sizedPage = PageRequest.of(page, size)
        val paged = downloadStatsService.serverPackDownloads(sizedPage)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            paged
        )
    }

    /** Every individual modpack download, whole history. */
    @GetMapping("/downloads/modpacks/history")
    @ResponseBody
    fun modPackDownloadHistory(): ResponseEntity<List<ModPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.allModPackDownloadsHistory()
        )
    }

    /** Every individual modpack download, one page at a time. */
    @GetMapping("/downloads/modpacks/historypaginated", produces = ["application/json"])
    @ResponseBody
    fun modPackDownloadHistoryPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<ModPackDownload>> {
        val sizedPage = PageRequest.of(page, size)
        val paged = downloadStatsService.allModPackDownloadsHistory(sizedPage)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            paged.content
        )
    }

    /** Every individual server-pack download, whole history. */
    @GetMapping("/downloads/serverpacks/history")
    @ResponseBody
    fun serverPackDownloadHistory(): ResponseEntity<List<ServerPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.allServerPackDownloadsHistory()
        )
    }

    /** Every individual server-pack download, one page at a time. */
    @GetMapping("/downloads/serverpacks/historypaginated")
    @ResponseBody
    fun serverPackDownloadHistoryPaginated(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<List<ServerPackDownload>> {
        val sizedPage = PageRequest.of(page, size)
        val paged = downloadStatsService.allServerPackDownloadsHistory(sizedPage)
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            paged.content
        )
    }

    /** The download history of one modpack. */
    @GetMapping("/downloads/modpacks/{modPackID}")
    @ResponseBody
    fun allDownloadsForModPack(@PathVariable modPackID: String): ResponseEntity<List<ModPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.downloadHistoryForModPack(modPackID)
        )
    }

    /** The download history of one server pack. A separate route from the modpack one on purpose — the two paths must not collide. */
    @GetMapping("/downloads/serverpacks/{serverPackID}")
    @ResponseBody
    fun allDownloadsForServerPack(@PathVariable serverPackID: String): ResponseEntity<List<ServerPackDownload>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            downloadStatsService.downloadHistoryForServerPack(serverPackID)
        )
    }

    /** Free, total and SPC-occupied space per relevant directory. */
    @GetMapping("/disk")
    @ResponseBody
    fun diskStats(): ResponseEntity<List<DiskStatsData>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            diskStatsService.stats
        )
    }

    /** How many modpacks were created per day. */
    @GetMapping("/creation/modpacks")
    @ResponseBody
    fun modPacksCreationTimes(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            creationStatsService.getModpackTimeStamps()
        )
    }

    /** How many server packs were created per day. */
    @GetMapping("/creation/serverpacks")
    @ResponseBody
    fun serverPacksCreationTimes(): ResponseEntity<List<AmountPerDate>> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            creationStatsService.getServerPackTimeStamps()
        )
    }

    /** Plain counts — packs, configurations, and the distinct versions and loaders in use. */
    @GetMapping("/packs")
    @ResponseBody
    fun amountStats(): ResponseEntity<AmountStatsData> {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE).body(
            amountStatsService.stats
        )
    }
}