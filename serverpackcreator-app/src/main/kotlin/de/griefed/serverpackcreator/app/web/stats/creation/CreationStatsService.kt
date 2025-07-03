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
package de.griefed.serverpackcreator.app.web.stats.creation

import de.griefed.serverpackcreator.app.web.modpack.ModPackService
import de.griefed.serverpackcreator.app.web.serverpack.ServerPackService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.DateFormat
import java.util.Date

@Service
class CreationStatsService @Autowired constructor(
    private val serverPackService: ServerPackService,
    private val modpackService: ModPackService
) {

    fun getModpackTimeStamps(): List<AmountPerDate> {
        val dates = mutableListOf<Date>()
        for (pack in modpackService.getModpacks()) {
            dates.add(pack.dateCreated)
        }
        return count(dates)
    }

    fun getServerPackTimeStamps(): List<AmountPerDate> {
        val dates = mutableListOf<Date>()
        for (pack in serverPackService.getServerPacks()) {
            dates.add(pack.dateCreated)
        }
        return count(dates)
    }

    private fun count(dates: List<Date>): List<AmountPerDate> {
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

    private fun dateToString(date: Date): String {
        return "${DateFormat.getInstance().format(date)}" //"${date.toLocaleString()}-${date.toLocalDate().monthValue}-${date.toLocalDate().dayOfMonth}"
    }
}