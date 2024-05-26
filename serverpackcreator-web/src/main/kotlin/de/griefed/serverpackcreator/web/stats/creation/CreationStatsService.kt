package de.griefed.serverpackcreator.web.stats.creation

import de.griefed.serverpackcreator.web.modpack.ModPackService
import de.griefed.serverpackcreator.web.serverpack.ServerPackService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CreationStatsService @Autowired constructor(
    private val serverPackService: ServerPackService,
    private val modpackService: ModPackService
) {

    fun getModpackTimeStamps(): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (pack in modpackService.getModpacks()) {
            dates.add(pack.dateCreated!!.toLocalDateTime())
        }
        return count(dates)
    }

    fun getServerPackTimeStamps(): List<AmountPerDate> {
        val dates = mutableListOf<LocalDateTime>()
        for (pack in serverPackService.getServerPacks()) {
            dates.add(pack.dateCreated!!.toLocalDateTime())
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
}