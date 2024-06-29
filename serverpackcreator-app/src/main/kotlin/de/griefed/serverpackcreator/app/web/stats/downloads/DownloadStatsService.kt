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
package de.griefed.serverpackcreator.app.web.stats.downloads

import de.griefed.serverpackcreator.app.web.modpack.ModPack
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.app.web.modpack.ModPackDownloadRepository
import de.griefed.serverpackcreator.app.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPack
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownload
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackDownloadRepository
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.app.web.stats.creation.AmountPerDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DownloadStatsService @Autowired constructor(
    private val modPackDownloadRepository: ModPackDownloadRepository,
    private val serverPackDownloadRepository: ServerPackDownloadRepository,
    private val modpackRepository: ModPackRepository,
    private val serverPackRepository: ServerPackRepository,
) {

    fun modPackDownloads(sort: Sort = Sort.by(Sort.Direction.DESC, "date")): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in modPackDownloadRepository.findAll(sort).filter { it.downloadedAt != null }) {
            dates.add(download.downloadedAt!!.toLocalDateTime())
        }
        return count(dates)
    }

    fun modPackDownloads(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "date")): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in modPackDownloadRepository.findAll(sizedPage.withSort(sort)).filter { it.downloadedAt != null }) {
            dates.add(download.downloadedAt!!.toLocalDateTime())
        }
        return count(dates)
    }

    fun serverPackDownloads(sort: Sort = Sort.by(Sort.Direction.DESC, "date")): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in serverPackDownloadRepository.findAll(sort).filter { it.downloadedAt != null }) {
            dates.add(download.downloadedAt!!.toLocalDateTime())
        }
        return count(dates)
    }

    fun serverPackDownloads(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "date")): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in serverPackDownloadRepository.findAll(sizedPage.withSort(sort)).filter { it.downloadedAt != null }) {
            dates.add(download.downloadedAt!!.toLocalDateTime())
        }
        return count(dates)
    }

    private fun count(dates: List<LocalDateTime>): List<AmountPerDate> {
        val creations = mutableListOf<AmountPerDate>()
        for (date in dates) {
            //skip if the date was already checked
            if (creations.any { it.date == dateToString(date) }) {
                continue
            }
            val dateStr = dateToString(date)
            val count = dates.count { stamp ->
                dateToString(stamp) == dateStr
            }
            creations.add(AmountPerDate(count, dateStr))
        }
        return creations
    }

    private fun dateToString(date: LocalDateTime): String {
        return "${date.year}-${date.monthValue}-${date.dayOfMonth}"
    }

    fun allModPackDownloadsHistory(sort: Sort = Sort.by(Sort.Direction.DESC, "downloadedAt")): List<ModPackDownload> {
        return modPackDownloadRepository.findAll(sort)
    }

    fun allModPackDownloadsHistory(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "downloadedAt")): Page<ModPackDownload> {
        return modPackDownloadRepository.findAll(sizedPage.withSort(sort))
    }

    fun allServerPackDownloadsHistory(sort: Sort = Sort.by(Sort.Direction.DESC, "downloadedAt")): List<ServerPackDownload> {
        return serverPackDownloadRepository.findAll(sort)
    }

    fun allServerPackDownloadsHistory(sizedPage: PageRequest, sort: Sort = Sort.by(Sort.Direction.DESC, "downloadedAt")): Page<ServerPackDownload> {
        return serverPackDownloadRepository.findAll(sizedPage.withSort(sort))
    }

    fun downloadHistoryForModPack(modPackID: Int): List<ModPackDownload> {
        val pack = modpackRepository.findById(modPackID)
        return if (pack.isPresent) {
            downloadHistoryForModPack(pack.get())
        } else {
            listOf()
        }
    }

    fun downloadHistoryForModPack(modPack: ModPack): List<ModPackDownload> {
        return modPackDownloadRepository.findAllByModPack(modPack)
    }

    fun downloadHistoryForServerPack(serverPackID: Int): List<ServerPackDownload> {
        val pack = serverPackRepository.findById(serverPackID)
        return if (pack.isPresent) {
            downloadHistoryForServerPack(pack.get())
        } else {
            listOf()
        }
    }

    fun downloadHistoryForServerPack(serverPack: ServerPack): List<ServerPackDownload> {
        return serverPackDownloadRepository.findAllByServerPack(serverPack)
    }
}