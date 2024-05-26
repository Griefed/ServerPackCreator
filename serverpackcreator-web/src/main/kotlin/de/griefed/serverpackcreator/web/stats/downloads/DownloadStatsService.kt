package de.griefed.serverpackcreator.web.stats.downloads

import de.griefed.serverpackcreator.web.modpack.ModPack
import de.griefed.serverpackcreator.web.modpack.ModPackDownload
import de.griefed.serverpackcreator.web.modpack.ModPackDownloadRepository
import de.griefed.serverpackcreator.web.modpack.ModPackRepository
import de.griefed.serverpackcreator.web.serverpack.ServerPack
import de.griefed.serverpackcreator.web.serverpack.ServerPackDownload
import de.griefed.serverpackcreator.web.serverpack.ServerPackDownloadRepository
import de.griefed.serverpackcreator.web.serverpack.ServerPackRepository
import de.griefed.serverpackcreator.web.stats.creation.AmountPerDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DownloadStatsService @Autowired constructor(
    private val modPackDownloadRepository: ModPackDownloadRepository,
    private val serverPackDownloadRepository: ServerPackDownloadRepository,
    private val modpackRepository: ModPackRepository,
    private val serverPackRepository: ServerPackRepository,
) {

    fun modPackDownloads(): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in modPackDownloadRepository.findAll().filter { it.downloadedAt != null }) {
            dates.add(download.downloadedAt!!.toLocalDateTime())
        }
        return count(dates)
    }

    fun serverPackDownloads(): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (download in serverPackDownloadRepository.findAll().filter { it.downloadedAt != null }) {
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

    fun allModPackDownloadsHistory(): List<ModPackDownload> {
        return modPackDownloadRepository.findAll()
    }

    fun allServerPackDownloadsHistory(): List<ServerPackDownload> {
        return serverPackDownloadRepository.findAll()
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